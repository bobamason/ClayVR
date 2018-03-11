package net.masonapps.clayvr.math;

/**
 * Created by Bob Mason on 10/16/2017.
 */

public class UnitConversion {

    public static float convertToMeters(float value, Unit unit) {
        switch (unit) {
            case meter:
                return value;
            case centimeter:
                return centimeterToMeter(value);
            case millimeter:
                return millimeterToMeter(value);
            case foot:
                return footToMeter(value);
            case inch:
                return inchToMeter(value);
            default:
                throw new IllegalArgumentException("unknown unit");
        }
    }

    public static float convertMeterToUnit(float value, Unit unit) {
        switch (unit) {
            case meter:
                return value;
            case centimeter:
                return meterToCentimeter(value);
            case millimeter:
                return meterToMillimeter(value);
            case foot:
                return meterToFoot(value);
            case inch:
                return meterToInch(value);
            default:
                throw new IllegalArgumentException("unknown unit");
        }
    }

    private static float centimeterToMeter(float value) {
        return value * 0.01f;
    }

    private static float meterToCentimeter(float value) {
        return value * 100f;
    }

    private static float millimeterToMeter(float value) {
        return value * 0.001f;
    }

    private static float meterToMillimeter(float value) {
        return value * 1000f;
    }

    public static float footToMeter(float value) {
        return value * 0.3048f;
    }

    public static float meterToFoot(float value) {
        return value * 3.28084f;
    }

    public static float footToInch(float value) {
        return value * 12f;
    }

    public static float inchToFoot(float value) {
        return value / 12f;
    }

    private static float inchToMeter(float value) {
        return footToMeter(inchToFoot(value));
    }

    private static float meterToInch(float value) {
        return footToInch(meterToFoot(value));
    }

    public static String getUnitString(Unit unit) {
        switch (unit) {
            case millimeter:
                return "mm";
            case centimeter:
                return "cm";
            case meter:
                return "m";
            case inch:
                return "in";
            case foot:
                return "ft";
            default:
                return "";
        }
    }

    public enum Unit {
        meter, centimeter, millimeter, foot, inch
    }
}
