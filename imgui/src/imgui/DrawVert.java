/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui;

import glm.vec._2.Vec2;
import glm.vec._4.ub.Vec4ub;

/**
 * Vertex Layout.
 *
 * @author GBarbieri
 */
public class DrawVert {

    static final int SIZE = 2 * Vec2.SIZE + Vec4ub.SIZE;
    static final int OFFSET_POSITION = 0;
    static final int OFFSET_UV = Vec2.SIZE;
    static final int OFFSET_COLOR = 2 * Vec2.SIZE;

    Vec2 pos;

    Vec2 uv;

    Vec4ub col;
}
