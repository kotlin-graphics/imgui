/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui.internal;

import glm.vec._2.Vec2;

/**
 * Stacked data for BeginGroup()/EndGroup().
 *
 * @author GBarbieri
 */
public class GroupData {

    Vec2 backupCursorPos;
    Vec2 backupCursorMaxPos;
    float backupIndentX;
    float backupCurrentLineHeight;
    float backupCurrentLineTextBaseOffset;
    float backupLogLinePosY;
    boolean advanceCursor;
}
