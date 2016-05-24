/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui.internal;

import glm.vec._2.Vec2;

/**
 * Data saved in imgui.ini file.
 * 
 * @author GBarbieri
 */
public class IniData {
    
    String name;
    
    int id;
    
    Vec2 pos;
    
    Vec2 size;
    
    boolean collapsed;
}
