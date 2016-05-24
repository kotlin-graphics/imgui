/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui.internal;

import glm.vec._2.Vec2;
import glm.vec._4.Vec4;

/**
 * 2D axis aligned bounding-box.
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

    public Rect(float f) {
        this(f, f, f, f);
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

    /**
     * @return Top-right
     */
    public Vec2 getTR() {
        return new Vec2(max.x, min.y);
    }

    /**
     * @return Bottom-left
     */
    public Vec2 getBL() {
        return new Vec2(min.x, max.y);
    }

    /**
     * @return Bottom-right
     */
    public Vec2 getBR() {
        return max;
    }

    public boolean contains(Vec2 p) {
        return p.x >= min.x && p.y >= min.y && p.x < max.x && p.y < max.y;
    }

    public boolean contains(Rect r) {
        return r.min.x >= min.x && r.min.y >= min.y && r.max.x < max.x && r.max.y < max.y;
    }

    public boolean overlaps(Rect r) {
        return r.min.y < max.y && r.max.y > min.y && r.min.x < max.x && r.max.x > min.x;
    }

    public void add(Vec2 rhs) {
        if (min.x > rhs.x) {
            min.x = rhs.x;
        }
        if (min.y > rhs.y) {
            min.y = rhs.y;
        }
        if (max.x < rhs.x) {
            max.x = rhs.x;
        }
        if (max.y < rhs.y) {
            max.y = rhs.y;
        }
    }

    public void add(Rect rhs) {
        if (min.x > rhs.min.x) {
            min.x = rhs.min.x;
        }
        if (min.y > rhs.min.y) {
            min.y = rhs.min.y;
        }
        if (max.x < rhs.max.x) {
            max.x = rhs.max.x;
        }
        if (max.y < rhs.max.y) {
            max.y = rhs.max.y;
        }
    }

    public void expand(float amount) {
        min.x -= amount;
        min.y -= amount;
        max.x += amount;
        max.y += amount;
    }

    public void expand(Vec2 amount) {
        min.x -= amount.x;
        min.y -= amount.y;
        max.x += amount.x;
        max.y += amount.y;
    }

    public void reduce(Vec2 amount) {
        min.x += amount.x;
        min.y += amount.y;
        max.x -= amount.x;
        max.y -= amount.y;
    }

    public void clip(Rect clip) {
        if (min.x < clip.min.x) {
            min.x = clip.min.x;
        }
        if (min.y < clip.min.y) {
            min.y = clip.min.y;
        }
        if (max.x > clip.max.x) {
            max.x = clip.max.x;
        }
        if (max.y > clip.max.y) {
            max.y = clip.max.y;
        }
    }

    public void floor() {
        min.x = (int) min.x;
        min.y = (int) min.y;
        max.x = (int) max.x;
        max.y = (int) max.y;
    }

    public Vec2 getClosestPoint(Vec2 p, boolean onEdge) {
        if (!onEdge && contains(p)) {
            return p;
        }
        if (p.x > max.x) {
            p.x = max.x;
        } else if (p.x < min.x) {
            p.x = min.x;
        }
        if (p.y > max.y) {
            p.y = max.y;
        } else if (p.y < min.y) {
            p.y = min.y;
        }
        return p;
    }
}
