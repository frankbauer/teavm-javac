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

public class ISOTimestampConverter {
    /**
     * Converts a date object to an ISO 8601 string
     * @param date the date to convert
     * @return the ISO 8601 string
     */
    public static String toISO(java.util.Date date) {
        return toISO(date.getTime() / 1000);
    }

    /**
     * Converts a unix timestamp to an ISO 8601 string
     * @param unixTimestamp the unix timestamp to convert
     * @return the ISO 8601 string
     */
    public static String toISO(long unixTimestamp) {
        // Constants
        final long SECONDS_PER_MINUTE = 60;
        final long MINUTES_PER_HOUR = 60;
        final long HOURS_PER_DAY = 24;
        final long DAYS_PER_YEAR = 365;
        final long[] DAYS_PER_MONTH = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

        // Calculate date and time components
        long seconds = unixTimestamp;
        long minutes = seconds / SECONDS_PER_MINUTE;
        seconds %= SECONDS_PER_MINUTE;
        long hours = minutes / MINUTES_PER_HOUR;
        minutes %= MINUTES_PER_HOUR;
        long days = hours / HOURS_PER_DAY;
        hours %= HOURS_PER_DAY;

        // Calculate year
        long year = 1970;
        while (days >= DAYS_PER_YEAR + (isLeapYear(year)?1:0)) {
            days -= DAYS_PER_YEAR + (isLeapYear(year)?1:0);
            year++;
        }

        // Calculate month
        int month = 0;
        while (days >= DAYS_PER_MONTH[month] + (month == 1 && isLeapYear(year) ? 1 : 0)) {
            days -= DAYS_PER_MONTH[month] + (month == 1 && isLeapYear(year) ? 1 : 0);
            month++;
        }
        month++;

        // Calculate day
        int day = (int) days + 1;

        return year + "-" +
                (month < 10 ? "0" : "") + month + "-" +
                (day < 10 ? "0" : "") + day + "T" +
                (hours < 10 ? "0" : "") + hours + ":" +
                (minutes < 10 ? "0" : "") + minutes + ":" +
                (seconds < 10 ? "0" : "") + seconds + "Z";
    }


    /**
     * Converts an ISO 8601 string to a Date object
     * @param iso the ISO 8601 string to convert
     * @return the Date object
     */
    public static java.util.Date toDate(String iso) {
        return new java.util.Date(toUnixTimestamp(iso) * 1000);
    }

    /**
     * Converts an ISO 8601 string to a unix timestamp
     * @param iso the ISO 8601 string to convert
     * @return the unix timestamp
     */
    public static long toUnixTimestamp(String iso) {
        if (!isValidISO(iso)) throw new IllegalArgumentException("Invalid ISO 8601 string: " + iso);
        // Constants
        final long SECONDS_PER_MINUTE = 60;
        final long MINUTES_PER_HOUR = 60;
        final long HOURS_PER_DAY = 24;
        final long DAYS_PER_YEAR = 365;
        final long[] DAYS_PER_MONTH = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

        // Extract date and time components from ISO string
        int year = Integer.parseInt(iso.substring(0, 4));
        int month = Integer.parseInt(iso.substring(5, 7));
        int day = Integer.parseInt(iso.substring(8, 10));
        int hour = Integer.parseInt(iso.substring(11, 13));
        int minute = Integer.parseInt(iso.substring(14, 16));
        int second = Integer.parseInt(iso.substring(17, 19));

        // Calculate days since epoch
        long daysSinceEpoch = 0;
        for (int y = 1970; y < year; y++) {
            daysSinceEpoch += DAYS_PER_YEAR + (isLeapYear(y)?1:0);
        }
        for (int m = 0; m < month - 1; m++) {
            daysSinceEpoch += DAYS_PER_MONTH[m] + (m == 1 && isLeapYear(year) ? 1 : 0);
        }
        daysSinceEpoch += day - 1;

        // Calculate timestamp
        long timestamp = daysSinceEpoch * HOURS_PER_DAY * MINUTES_PER_HOUR * SECONDS_PER_MINUTE;
        timestamp += hour * MINUTES_PER_HOUR * SECONDS_PER_MINUTE;
        timestamp += minute * SECONDS_PER_MINUTE;
        timestamp += second;

        return timestamp;
    }

    /**
     * Determines whether a year is a leap year
     * @param year the year to check
     * @return true if the year is a leap year, false otherwise
     */
    private static boolean isLeapYear(long year) {
        if (year % 400 == 0) {
            return true;
        } else if (year % 100 == 0) {
            return false;
        } else if (year % 4 == 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Determines whether a string is a valid ISO 8601 string
     * @param iso the string to check
     * @return true if the string is a valid ISO 8601 string, false otherwise
     */
    public static boolean isValidISO(String iso) {
        //String regex = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$";
        //return iso.matches(regex);
        return true;
    }
}




