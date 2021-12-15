package de.georgrichter.vibrationdemoapp.util;

import android.graphics.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;
import java.util.function.Function;

public class InterpolationUtils {
    public static class Vector2{
        public float x;
        public float y;

        public Vector2() {this(0, 0);}

        public Vector2(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    public static final Function<Float, Float> INTP_LINEAR = x -> x;

    public static final Function<Float, Float> INTP_LOGISTIC
            = x -> (float)(1 / (1 + Math.exp(-10 * (x - 0.5))));

    public static Function<Float, Float> FromPoints(Vector2... points){
        return FromPoints(Arrays.asList(points), INTP_LINEAR);
    }

    public static Function<Float, Float> FromPoints(Function<Float, Float> intpFunc, Vector2... points){
        return FromPoints(Arrays.asList(points), intpFunc);
    }

    public static Function<Float, Float> FromPoints(List<Vector2> points, Function<Float, Float> intpFunc){
        List<Vector2> copy = new ArrayList<>(points);
        copy.sort(Comparator.comparingDouble(c -> c.x));
        return x -> {
            for (int i = 0; i < copy.size(); i++) {
                Vector2 p = copy.get(i);
                if(i == copy.size() - 1) return p.y;
                Vector2 next = points.get(i + 1);
                if(x >= p.x && x < next.x){
                    float t = (x - p.x) / (next.x - p.x);
                    t = intpFunc.apply(t);
                    float r = lerp(t, p.y, next.y);
                    //System.out.println("x=" + x + " p.y= " + p.y + " next.y=" + next.y + " t=" + t + " r=" + r);
                    return r;
                }
            }
            return copy.get(0).y;
        };
    }

    public static float lerp(float t, float from, float to){
        return from + (to - from) * t;
    }
}
