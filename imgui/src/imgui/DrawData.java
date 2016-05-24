/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui;

import java.util.ArrayList;

/**
 * All draw data to render an ImGui frame.
 *
 * @author GBarbieri
 */
public class DrawData {

    /**
     * Only valid after Render() is called and before the next NewFrame() is called.
     */
    boolean valid = false;

    ArrayList<DrawList> cmdLists=null;

    int cmdListsCount=0;

    /**
     * For convenience, sum of all cmd_lists vtx_buffer.Size.
     */
    int totalVtxCount=0;

    /**
     * For convenience, sum of all cmd_lists idx_buffer.Size.
     */
    int totalIdxCount=0;
}
