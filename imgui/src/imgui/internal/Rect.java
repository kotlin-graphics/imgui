/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui.internal;

import glm.vec._2.Vec2;
import glm.vec._4.Vec4;

/**
 *
 * @author GBarbieri
 */
public class Rect {

    /**
     * Upper-left.
     */
    Vec2 min;
    /**
     * Lower-right.
     */
    Vec2 max; 

    public Rect() {
        min = new Vec2(Float.MAX_VALUE);
        max = new Vec2(-Float.MAX_VALUE);
    }

    public Rect(Vec2 min, Vec2 max) {
        this.min = min;
        this.max = max;
    }

    public Rect(Vec4 v) {
        min = new Vec2(v.x, v.y);
        max = new Vec2(v.z, v.w);
    }

    public Rect(float x1, float y1, float x2, float y2) {
        min = new Vec2(x1, y1);
        max = new Vec2(x2, y2);
    }

    public Vec2 getCenter() {
        return min.add_(max).mul(0.5f);
    }

    public Vec2 getSize() {
        return max.sub_(min);
    }

    public float getWidth() {
        return max.x - min.x;
    }

    public float getHeight() {
        return max.y - min.y;
    }
    
    /**
     * @return Top-left
     */
    public Vec2 getTL() {
        return min;
    }
}
