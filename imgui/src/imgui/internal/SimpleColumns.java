/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui.internal;

/**
 * Simple column measurement currently used for MenuItem() only. This is very short-sighted/throw-away code and NOT a
 * generic helper.
 *
 * @author GBarbieri
 */
public class SimpleColumns {

    int count = 0;
    
    float spacing = 0.0f;
    
    float width = 0.0f, nextWidth = 0.0f;
    
    float[] pos = new float[8], nextWidths = new float[8];

    public void update(int count, float spacing, boolean clear) {

        assert (this.count <= pos.length);
        this.count = count;
        width = 0.0f;
        nextWidth = 0.0f;
        this.spacing = spacing;
        if (clear) {
            for (int i = 0; i < nextWidths.length; i++) {
                nextWidths[i] = 0;
            }
        }
        for (int i = 0; i < this.count; i++) {
            if (i > 0 && nextWidths[i] > 0.0f) {
                width += this.spacing;
            }
            pos[i] = (int) width;
            width += nextWidths[i];
            nextWidths[i] = 0.0f;
        }
    }

    public float declColumns(float w0, float w1, float w2) {

        nextWidth = 0.0f;
        nextWidths[0] = Math.max(nextWidths[0], w0);
        nextWidths[1] = Math.max(nextWidths[1], w1);
        nextWidths[2] = Math.max(nextWidths[2], w2);
        for (int i = 0; i < 3; i++) {
            nextWidth += nextWidths[i] + ((i > 0 && nextWidths[i] > 0.0f) ? spacing : 0.0f);
        }
        return Math.max(width, nextWidth);
    }

    public float calcExtraSpace(float avail_w) {

        return Math.max(0.0f, avail_w - width);
    }
}
