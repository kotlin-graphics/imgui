/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui;

import com.jogamp.opengl.util.GLBuffers;
import java.nio.IntBuffer;

/**
 *
 * @author GBarbieri
 */
public class Imp_newt {
    
    private static IntBuffer fontTexture = GLBuffers.newDirectIntBuffer(1);
    
    public static void newFrame() {
        
        if(fontTexture.get(0) == 0) {
            
        }
    }
    
    private static void createDeviceObjects() {
        
        // Build texture atlas
        IO io = 
    }
}
