/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui.internal;

/**
 * Internal state of the currently focused/edited text input box.
 *
 * @author GBarbieri
 */
public class TextEditState {

    /**
     * Widget id owning the text state.
     */
    int id;
    
    /**
     * Edit buffer, we need to persist but can't guarantee the persistence of the user-provided buffer. So we copy into own
     * buffer.
     */
    short[] text;
    
    /**
     * Backup of end-user buffer at the time of focus (in UTF-8, unaltered).
     */
    byte[] initialText;

    byte[] tempTextBuffer;

    /**
     * We need to maintain our buffer length in both UTF-8 and wchar format.
     */
    int curLenA, curLenW;
    
    /**
     * End-user buffer size.
     */
    int bufSizeA;

    float scrollX;

    STB_TexteditState stbState;

    float cursorAnim;

    boolean cursorFollow;

    boolean selectedAllMouseLock;

    // TODO, check arrays, maybe need arraylist
    // ImGuiTextEditState()                            { memset(this, 0, sizeof(*this)); }
    /**
     * After a user-input the cursor stays on for a while without blinking.
     */
    public void cursorAnimReset() {
        cursorAnim = -0.30f;
    }

    public void cursorClamp() {
        stbState.cursor = Math.min(stbState.cursor, curLenW);
        stbState.selectStart = Math.min(stbState.selectStart, curLenW);
        stbState.selectEnd = Math.min(stbState.selectEnd, curLenW);
    }

    public boolean hasSelection() {
        return stbState.selectStart != stbState.selectEnd;
    }

    public void ClearSelection() {
        stbState.selectStart = stbState.selectEnd = stbState.cursor;
    }

    public void SelectAll() {
        stbState.selectStart = 0;
        stbState.selectEnd = curLenW;
        stbState.cursor = stbState.selectEnd;
        stbState.hasPreferredX = 0;
    }

    // TODO
    public void OnKeyPressed(int key){
        
    }
}
