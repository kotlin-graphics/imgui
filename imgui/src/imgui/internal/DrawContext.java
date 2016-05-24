/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui.internal;

import glm.vec._2.Vec2;
import imgui.ColorEditMode;
import java.util.ArrayList;

/**
 * Transient per-window data, reset at the beginning of the frame
 * TODO FIXME: That's theory, in practice the delimitation between ImGuiWindow and ImGuiDrawContext is quite tenuous and
 * could be reconsidered.
 *
 * @author GBarbieri
 */
public class DrawContext {

    Vec2 cursorPos = new Vec2(0.0f);

    Vec2 cursorPosPrevLine = new Vec2(0.0f);

    Vec2 cursorStartPos = new Vec2(0.0f);

    /**
     * Implicitly calculate the size of our contents, always extending. Saved into window->SizeContents at the end of the
     * frame.
     */
    Vec2 cursorMaxPos = new Vec2(0.0f);

    float currentLineHeight = 0.0f;

    float currentLineTextBaseOffset = 0.0f;

    float prevLineHeight = 0.0f;

    float prevLineTextBaseOffset = 0.0f;

    float logLinePosY = -1.0f;

    int treeDepth = 0;

    int lastItemID = 0;

    Rect lastItemRect = new Rect(0.0f);

    /**
     * Item rectangle is hovered, and its window is currently interactable with (not blocked by a popup preventing access
     * to the window).
     */
    boolean lastItemHoveredAndUsable = false;

    /**
     * Item rectangle is hovered, but its window may or not be currently interactable with (might be blocked by a popup
     * preventing access to the window).
     */
    boolean lastItemHoveredRect = false;

    boolean menuBarAppending = false;

    float menuBarOffsetX = 0.0f;

    ArrayList<Window> childWindows;

    Storage stateStorage = null;

    int layoutType = LayoutType.Vertical;

    /**
     * We store the current settings outside of the vectors to increase memory locality (reduce cache misses). The vectors
     * are rarely modified. Also it allows us to not heap allocate for short-lived windows which are not using those
     * settings.
     */
    /**
     * == ItemWidthStack.back(). 0.0: default, >0.0: width in pixels, <0.0: align xx pixels to the right of window.
     */
    float itemWidth = 0.0f;

    /**
     * == TextWrapPosStack.back() [empty == -1.0f].
     */
    float textWrapPos = -1.0f;

    /**
     * == AllowKeyboardFocusStack.back() [empty == true].
     */
    boolean allowKeyboardFocus = true;

    /**
     * == ButtonRepeatStack.back() [empty == false].
     */
    boolean buttonRepeat = false;

    float itemWidthStack;

    ArrayList<Float> textWrapPosStack;

    ArrayList<Boolean> allowKeyboardFocusStack;

    ArrayList<Boolean> buttonRepeatStack;

    ArrayList<GroupData> groupStack;

    int colorEditMode = ColorEditMode.RGB;

    /**
     * Store size of various stacks for asserting.
     */
    int[] stackSizesBackup = new int[6];

    /**
     * Indentation / start position from left of window (increased by TreePush/TreePop, etc.).
     */
    float indentX = 0.0f;

    /**
     * Offset to the current column (if ColumnsCurrent > 0). FIXME: This and the above should be a stack to allow use
     * cases like Tree->Column->Tree. Need revamp columns API.
     */
    float columnsOffsetX = 0.0f;

    int columnsCurrent = 0;

    int columnsCount = 1;

    float columnsMinX = 0.0f;

    float columnsMaxX = 0.0f;

    float columnsStartPosY = 0.0f;

    float columnsCellMinY = 0.0f;

    float columnsCellMaxY = 0.0f;

    boolean columnsShowBorders = true;

    int columnsSetID = 0;

    ArrayList<ColumnData> columnsData;
}
