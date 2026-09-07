package com.tidal.builtin.client;

final class RadialMath {
    private RadialMath() {}

    static float tau(float angle) {
        float twoPi = (float) (Math.PI * 2.0);
        angle %= twoPi;
        if (angle < 0.0f) {
            angle += twoPi;
        }
        return angle;
    }

    static float wrappedLength(float start, float end) {
        float len = tau(end) - tau(start);
        if (len < 0.0f) {
            len += (float) (Math.PI * 2.0);
        }
        return len;
    }

    static boolean inArc(float angle, float start, float end) {
        angle = tau(angle);
        start = tau(start);
        end = tau(end);
        if (start <= end) {
            return angle >= start && angle <= end;
        }
        return angle >= start || angle <= end;
    }
}
