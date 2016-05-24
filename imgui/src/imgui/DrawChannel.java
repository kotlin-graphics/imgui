/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui;

import java.util.ArrayList;

/**
 * Draw channels are used by the Columns API to "split" the render list into different channels while building, so items of 
 * each column can be batched together.
 * 
 * You can also use them to simulate drawing layers and submit primitives in a different order than how they will be 
 * rendered.
 *
 * @author GBarbieri
 */
public class DrawChannel {

    ArrayList<DrawCmd> cmdBuffer;
    
    ArrayList<Short> idxBuffer;
}
