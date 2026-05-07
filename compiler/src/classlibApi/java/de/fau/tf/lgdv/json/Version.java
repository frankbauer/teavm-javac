/*
 *  Copyright 2026 frank bauer.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package de.fau.tf.lgdv.json;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Semantic Version value object with optional pre-release comment.
 */
public class Version implements JsonObjectable, Comparable<Version> {
    private static final Pattern SEMVER_PATTERN = Pattern.compile(
            "^(\\d+)\\.(\\d+)\\.(\\d+)(?:-([0-9A-Za-z.-]+))?$");

    public static final Version ZERO = new Version(0, 0, 0, null);

    public final int major;
    public final int minor;
    public final int patch;
    public final String comment;

    public Version(String value) {
        if (value == null) {
            this.major = 0;
            this.minor = 0;
            this.patch = 0;
            this.comment = null;
            return;
        }

        String trimmed = value.trim();
        Matcher matcher = SEMVER_PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            this.major = 0;
            this.minor = 0;
            this.patch = 0;
            this.comment = null;
            return;
        }

        this.major = Integer.parseInt(matcher.group(1));
        this.minor = Integer.parseInt(matcher.group(2));
        this.patch = Integer.parseInt(matcher.group(3));
        this.comment = normalizeComment(matcher.group(4));
    }

    public Version(int major, int minor, int patch, String comment) {
        this.major = Math.max(0, major);
        this.minor = Math.max(0, minor);
        this.patch = Math.max(0, patch);
        this.comment = normalizeComment(comment);
    }

    public Version(JsonObject o) {
        this(resolveMajor(o), resolveMinor(o), resolvePatch(o), resolveComment(o));
    }

    public Version(JsonArray a) {
        this.major = a != null ? Math.max(0, a.getInt(0, 0)) : 0;
        this.minor = a != null ? Math.max(0, a.getInt(1, 0)) : 0;
        this.patch = a != null ? Math.max(0, a.getInt(2, 0)) : 0;
        this.comment = a != null && a.size() > 3 ? normalizeComment(a.get(3).getString(null)) : null;
    }

    public static Version fromJsonElement(JsonElement el) {
        if (el == null || el.isNull()) {
            return Version.ZERO;
        } else if (el.isObject()) {
            return new Version(el.getObject());
        } else if (el.isArray()) {
            return new Version(el.getArray());
        } else if (el.isString()) {
            return new Version(el.getString());
        } else {
            return Version.ZERO;
        }
    }

    private static String normalizeComment(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static int resolveMajor(JsonObject o) {
        if (o == null) {
            return 0;
        }
        int major = Math.max(0, o.getInt("major", 0));
        if (major != 0 || o.has("major")) {
            return major;
        }
        Version parsed = parseNestedVersion(o);
        return parsed != null ? parsed.major : 0;
    }

    private static int resolveMinor(JsonObject o) {
        if (o == null) {
            return 0;
        }
        int minor = Math.max(0, o.getInt("minor", 0));
        if (minor != 0 || o.has("minor")) {
            return minor;
        }
        Version parsed = parseNestedVersion(o);
        return parsed != null ? parsed.minor : 0;
    }

    private static int resolvePatch(JsonObject o) {
        if (o == null) {
            return 0;
        }
        int patch = Math.max(0, o.getInt("patch", 0));
        if (patch != 0 || o.has("patch")) {
            return patch;
        }
        Version parsed = parseNestedVersion(o);
        return parsed != null ? parsed.patch : 0;
    }

    private static String resolveComment(JsonObject o) {
        if (o == null) {
            return null;
        }
        String comment = normalizeComment(o.getString("comment", null));
        if (comment != null) {
            return comment;
        }
        comment = normalizeComment(o.getString("preRelease", null));
        if (comment != null) {
            return comment;
        }
        Version parsed = parseNestedVersion(o);
        return parsed != null ? parsed.comment : null;
    }

    private static Version parseNestedVersion(JsonObject o) {
        if (o == null) {
            return null;
        }
        String input = normalizeComment(o.getString("version", null));
        if (input == null) {
            return null;
        }
        return new Version(input);
    }

    public boolean isHigherThan(Version other) {
        return compareTo(other) > 0;
    }

    public boolean isLowerThan(Version other) {
        return compareTo(other) < 0;
    }

    public boolean isEqualTo(Version other) {
        return compareTo(other) == 0;
    }

    public boolean isAtLeast(Version other) {
        return compareTo(other) >= 0;
    }

    public boolean isAtMost(Version other) {
        return compareTo(other) <= 0;
    }

    @Override
    public int compareTo(Version other) {
        if (other == null) {
            return 1;
        }

        int cmp = Integer.compare(major, other.major);
        if (cmp != 0) {
            return cmp;
        }

        cmp = Integer.compare(minor, other.minor);
        if (cmp != 0) {
            return cmp;
        }

        cmp = Integer.compare(patch, other.patch);
        if (cmp != 0) {
            return cmp;
        }

        return compareComments(comment, other.comment);
    }

    private static int compareComments(String left, String right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }

        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");
        int length = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < length; ++i) {
            if (i >= leftParts.length) {
                return -1;
            }
            if (i >= rightParts.length) {
                return 1;
            }

            String leftPart = leftParts[i];
            String rightPart = rightParts[i];

            boolean leftNumeric = isNumeric(leftPart);
            boolean rightNumeric = isNumeric(rightPart);
            if (leftNumeric && rightNumeric) {
                int cmp = Integer.compare(Integer.parseInt(leftPart), Integer.parseInt(rightPart));
                if (cmp != 0) {
                    return cmp;
                }
            } else if (leftNumeric != rightNumeric) {
                return leftNumeric ? -1 : 1;
            } else {
                int cmp = leftPart.compareTo(rightPart);
                if (cmp != 0) {
                    return cmp;
                }
            }
        }

        return 0;
    }

    private static boolean isNumeric(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); ++i) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public JsonElement toJsonElement() {
        JsonObject obj = new JsonObject()
                .put("major", major)
                .put("minor", minor)
                .put("patch", patch);
        if (comment == null) {
            obj.putNull("comment");
        } else {
            obj.put("comment", comment);
        }
        return obj.toJsonElement();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(major).append('.').append(minor).append('.').append(patch);
        if (comment != null) {
            sb.append('-').append(comment);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Version)) {
            return false;
        }
        Version version = (Version) o;
        return major == version.major
                && minor == version.minor
                && patch == version.patch
                && Objects.equals(comment, version.comment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch, comment);
    }
}
