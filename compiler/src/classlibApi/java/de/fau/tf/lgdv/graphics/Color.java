package de.fau.tf.lgdv.graphics;

import de.fau.tf.lgdv.json.*;

public class Color implements JsonObjectable {
    public static final Color Transparent = new Color(0, 0, 0, 0);
    public static final Color Black = new Color(0, 0, 0);
    public static final Color White = new Color(255, 255, 255);

  public static record RGB(double r, double g, double b, double a) implements JsonObjectable {
    public RGB {
      a = clamp01(a);
    }

    public RGB(JsonObject obj) {
      this(
          obj.getDouble("r", 0) / 0xff,
          obj.getDouble("g", 0) / 0xff,
          obj.getDouble("b", 0) / 0xff,
          obj.getDouble("a", 1.0)
      );
    }

    public RGB(JsonArray arr) {
      this(
          arr.getDouble(0, 0) / 0xff,
          arr.getDouble(1, 0) / 0xff,
          arr.getDouble(2, 0) / 0xff,
          arr.getDouble(3, 1.0)
      );
    }

    @Override
    public JsonElement toJsonElement() {
      return new JsonArray()
          .push(toByte(r))
          .push(toByte(g))
          .push(toByte(b))
          .push(a)
          .toJsonElement();
    }

    public static RGB fromJsonElement(JsonElement el) {
      if (el == null || el.isNull()) {
        return null;
      }
      if (el.isArray()) {
        JsonArray arr = el.getArray();
        return arr != null ? new RGB(arr) : null;
      }
      if (el.isObject() && el.getObject().has("r") && el.getObject().has("g") && el.getObject().has("b")) {
        JsonObject obj = el.getObject();
        return obj != null ? new RGB(obj) : null;
      }
      return null;
    }
  }

  public static record HSL(double h, double s, double l, double a) implements JsonObjectable {
    public HSL {
      h = normalizeHue(h);
      s = clamp01(s);
      l = clamp01(l);
      a = clamp01(a);
    }

    public HSL(JsonObject obj) {
      this(
          obj.getDouble("h", 0),
          obj.getDouble("s", 0),
          obj.getDouble("l", 0),
          obj.getDouble("a", 1.0)
      );
    }

    public HSL(JsonArray arr) {
      this(
          arr.getDouble(0, 0),
          arr.getDouble(1, 0),
          arr.getDouble(2, 0),
          arr.getDouble(3, 1.0)
      );
    }

    @Override
    public JsonElement toJsonElement() {
      return new JsonObject()
          .put("h", h)
          .put("s", s)
          .put("l", l)
          .put("a", a)
          .toJsonElement();
    }

    public static HSL fromJsonElement(JsonElement el) {
      if (el == null || el.isNull()) {
        return null;
      }
      if (el.isArray()) {
        JsonArray arr = el.getArray();
        return arr != null ? new HSL(arr) : null;
      }
      if (el.isObject()) {
        JsonObject obj = el.getObject();
        if (obj == null || !obj.has("h") || !obj.has("s") || !obj.has("l")) {
          return null;
        }
        return new HSL(obj);
      }
      return null;
    }
  }

  public static record HSV(double h, double s, double v, double a) implements JsonObjectable {
    public HSV {
      h = normalizeHue(h);
      s = clamp01(s);
      v = clamp01(v);
      a = clamp01(a);
    }

    public HSV(JsonObject obj) {
      this(
          obj.getDouble("h", 0),
          obj.getDouble("s", 0),
          obj.getDouble("v", 0),
          obj.getDouble("a", 1.0)
      );
    }

    public HSV(JsonArray arr) {
      this(
          arr.getDouble(0, 0),
          arr.getDouble(1, 0),
          arr.getDouble(2, 0),
          arr.getDouble(3, 1.0)
      );
    }

    @Override
    public JsonElement toJsonElement() {
      return new JsonObject()
          .put("h", h)
          .put("s", s)
          .put("v", v)
          .put("a", a)
          .toJsonElement();
    }

    public static HSV fromJsonElement(JsonElement el) {
      if (el == null || el.isNull()) {
        return null;
      }
      if (el.isArray()) {
        JsonArray arr = el.getArray();
        return arr != null ? new HSV(arr) : null;
      }
      if (el.isObject()) {
        JsonObject obj = el.getObject();
        if (obj == null || !obj.has("h") || !obj.has("s") || !obj.has("v")) {
          return null;
        }
        return new HSV(obj);
      }
      return null;
    }
  }

  public enum InputFormat {
    RGB,
    HSL,
    HSV
  }

  public final double r, g, b;
  public final double a;

  private RGB rgb;
  private HSL hsl;
  private HSV hsv;
  private final InputFormat inputFormat;

    public Color(double r, double g, double b) {
        this(r, g, b, 1.0);
    }

    public Color(double r, double g, double b, double a) {
      this(new RGB(r, g, b, a));
    }

    public Color(RGB rgb) {
      RGB normalized = new RGB(rgb.r(), rgb.g(), rgb.b(), rgb.a());
      this.rgb = normalized;
      this.r = normalized.r();
      this.g = normalized.g();
      this.b = normalized.b();
      this.a = normalized.a();
      this.inputFormat = InputFormat.RGB;
    }

    public Color(HSL hsl) {
      HSL normalized = new HSL(hsl.h(), hsl.s(), hsl.l(), hsl.a());
      RGB initialRgb = hslToRgb(normalized);
      this.hsl = normalized;
      this.r = initialRgb.r();
      this.g = initialRgb.g();
      this.b = initialRgb.b();
      this.a = initialRgb.a();
      this.inputFormat = InputFormat.HSL;
    }

    public Color(HSV hsv) {
      HSV normalized = new HSV(hsv.h(), hsv.s(), hsv.v(), hsv.a());
      RGB initialRgb = hsvToRgb(normalized);
      this.hsv = normalized;
      this.r = initialRgb.r();
      this.g = initialRgb.g();
      this.b = initialRgb.b();
      this.a = initialRgb.a();
      this.inputFormat = InputFormat.HSV;
    }

    public RGB rgb() {
      if (rgb == null) {
        rgb = hsl != null ? hslToRgb(hsl) : hsvToRgb(hsv);
      }
      return rgb;
    }

    public HSL hsl() {
      if (hsl == null) {
        hsl = rgbToHsl(rgb());
      }
      return hsl;
    }

    public HSV hsv() {
      if (hsv == null) {
        hsv = rgbToHsv(rgb());
      }
      return hsv;
    }

    public InputFormat inputFormat() {
      return inputFormat;
    }

    public double r() {
      return rgb().r();
    }

    public double g() {
      return rgb().g();
    }

    public double b() {
      return rgb().b();
    }

    public double h() {
      return hsl().h();
    }

    public double s() {
      return inputFormat == InputFormat.HSV ? hsv().s() : hsl().s();
    }

    public double l() {
      return hsl().l();
    }

    public double v() {
      return hsv().v();
    }

    public String toRgbaString() {
      RGB c = rgb();
      return "rgba(" + toByte(c.r()) + "," + toByte(c.g()) + "," + toByte(c.b()) + "," + c.a() + ")";
    }

    @Override
    public JsonElement toJsonElement() {
      switch (inputFormat) {
        case HSL -> {
          return hsl().toJsonElement();
        }
        case HSV -> {
          return hsv().toJsonElement();
        }
        case RGB -> {
          return rgb().toJsonElement();
        }
        default -> throw new IllegalStateException("Unsupported input format: " + inputFormat);
      }
    }

    public static Color fromJsonElement(JsonElement el) {
        if (el == null || el.isNull()) {
            return Color.Black;
        } else if (el.isObject()) {
        HSL hsl = HSL.fromJsonElement(el);
        if (hsl != null) {
          return new Color(hsl);
        }

        HSV hsv = HSV.fromJsonElement(el);
        if (hsv != null) {
          return new Color(hsv);
        }

        RGB rgb = RGB.fromJsonElement(el);
        if (rgb != null) {
          return new Color(rgb);
        }

        return Color.Black;
        } else if (el.isArray()) {
        RGB rgb = RGB.fromJsonElement(el);
        return rgb != null ? new Color(rgb) : Color.Black;
        } else {
            return Color.Black;
        }
    }

    private static int toByte(double value) {
      return (int) (clamp01(value) * 0xff);
    }

    private static double clamp01(double value) {
      return value < 0 ? 0 : (value > 1 ? 1 : value);
    }

    private static double normalizeHue(double hue) {
      double normalized = hue % 360.0;
      return normalized < 0 ? normalized + 360.0 : normalized;
    }

    private static HSL rgbToHsl(RGB rgb) {
      double r = clamp01(rgb.r());
      double g = clamp01(rgb.g());
      double b = clamp01(rgb.b());
      double max = Math.max(r, Math.max(g, b));
      double min = Math.min(r, Math.min(g, b));
      double delta = max - min;
      double l = (max + min) / 2.0;
      double h = hueFromRgb(r, g, b, max, delta);
      double s = delta == 0 ? 0 : delta / (1.0 - Math.abs(2.0 * l - 1.0));
      return new HSL(h, s, l, rgb.a());
    }

    private static HSV rgbToHsv(RGB rgb) {
      double r = clamp01(rgb.r());
      double g = clamp01(rgb.g());
      double b = clamp01(rgb.b());
      double max = Math.max(r, Math.max(g, b));
      double min = Math.min(r, Math.min(g, b));
      double delta = max - min;
      double h = hueFromRgb(r, g, b, max, delta);
      double s = max == 0 ? 0 : delta / max;
      double v = max;
      return new HSV(h, s, v, rgb.a());
    }

    private static RGB hslToRgb(HSL hsl) {
      double h = normalizeHue(hsl.h());
      double s = clamp01(hsl.s());
      double l = clamp01(hsl.l());
      double c = (1.0 - Math.abs(2.0 * l - 1.0)) * s;
      double hPrime = h / 60.0;
      double x = c * (1.0 - Math.abs(hPrime % 2.0 - 1.0));
      double m = l - c / 2.0;
      double[] rgbPrime = rgbPrimeFromHue(hPrime, c, x);
      return new RGB(rgbPrime[0] + m, rgbPrime[1] + m, rgbPrime[2] + m, hsl.a());
    }

    private static RGB hsvToRgb(HSV hsv) {
      double h = normalizeHue(hsv.h());
      double s = clamp01(hsv.s());
      double v = clamp01(hsv.v());
      double c = v * s;
      double hPrime = h / 60.0;
      double x = c * (1.0 - Math.abs(hPrime % 2.0 - 1.0));
      double m = v - c;
      double[] rgbPrime = rgbPrimeFromHue(hPrime, c, x);
      return new RGB(rgbPrime[0] + m, rgbPrime[1] + m, rgbPrime[2] + m, hsv.a());
    }

    private static double[] rgbPrimeFromHue(double hPrime, double c, double x) {
      if (hPrime < 1) {
        return new double[] { c, x, 0 };
      } else if (hPrime < 2) {
        return new double[] { x, c, 0 };
      } else if (hPrime < 3) {
        return new double[] { 0, c, x };
      } else if (hPrime < 4) {
        return new double[] { 0, x, c };
      } else if (hPrime < 5) {
        return new double[] { x, 0, c };
      } else {
        return new double[] { c, 0, x };
      }
    }

    private static double hueFromRgb(double r, double g, double b, double max, double delta) {
      if (delta == 0) {
        return 0;
      }
      double hPrime;
      if (max == r) {
        hPrime = ((g - b) / delta) % 6.0;
      } else if (max == g) {
        hPrime = ((b - r) / delta) + 2.0;
      } else {
        hPrime = ((r - g) / delta) + 4.0;
      }
      return normalizeHue(60.0 * hPrime);
    }
}