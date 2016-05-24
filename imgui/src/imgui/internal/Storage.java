/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui.internal;

import java.util.Hashtable;

/**
 * Helper: Simple Key->value storage
 * - Store collapse state for a tree (Int 0/1)
 * - Store color edit options (Int using values in ImGuiColorEditMode enum)
 * - Custom user storage for temporary values
 * Typically you don't have to worry about this since a storage is held within each Window
 * Declare your own storage if:
 * - You want to manipulate the open/close state of a particular sub-tree in your interface (tree node uses Int 0/1 to store
 * their state)
 * - You want to store custom debug data easily without adding or editing structures in your code
 * Types are NOT stored, so it is up to you to make sure your Key don't collide with different types.
 *
 * @author GBarbieri
 */
public class Storage {

    Hashtable<Integer, Float> data;
}
