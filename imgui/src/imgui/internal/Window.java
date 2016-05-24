/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui.internal;

import com.jogamp.opengl.math.geom.AABBox;
import glm.vec._2.Vec2;
import imgui.DrawList;
import imgui.SetCond;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Windows data.
 *
 * @author GBarbieri
 */
public class Window {

    String name;

    /**
     * == ImHash(Name).
     */
    int id = idGenerator.incrementAndGet();

    /**
     * See enum WindowFlags.
     */
    int flags = 0;

    /**
     * Order within immediate parent window, if we are a child window. Otherwise 0.
     */
    int indexWithinParent = 0;

    Vec2 posFloat = new Vec2(0.0f);

    /**
     * Position rounded-up to nearest pixel.
     */
    Vec2 pos = new Vec2(0.0f);

    /**
     * Current size (==SizeFull or collapsed title bar size).
     */
    Vec2 size = new Vec2(0.0f);

    /**
     * Size when non collapsed.
     */
    Vec2 sizeFull = new Vec2(0.0f);

    /**
     * Size of contents (== extents reach of the drawing cursor) from previous frame.
     */
    Vec2 sizeContents = new Vec2(0.0f);

    /**
     * Size of contents explicitly set by the user via SetNextWindowContentSize().
     */
    Vec2 sizeContentsExplicit = new Vec2(0.0f);

    /**
     * Window padding at the time of begin. We need to lock it, in particular manipulation of the ShowBorder would have an
     * effect.
     */
    Vec2 windowPadding = new Vec2(0.0f);

    /**
     * == window->GetID("#MOVE").
     * // TODO = GetID("#MOVE");
     */
    int moveId;

    Vec2 scroll = new Vec2(0.0f);

    /**
     * Target scroll position. stored as cursor position with scrolling canceled out, so the highest point is always 0.0f.
     * (FLT_MAX for no change)
     */
    Vec2 scrollTarget = new Vec2(Float.MAX_VALUE);

    /**
     * 0.0f = scroll so that target position is at top, 0.5f = scroll so that target position is centered.
     */
    Vec2 scrollTargetCenterRatio = new Vec2(0.5f);

    boolean scrollbarX = false, scrollbarY = false;

    Vec2 scrollbarSizes = new Vec2(0.0f);

    float borderSize = 0.0f;

    /**
     * Set to true on Begin().
     */
    boolean active = false;

    boolean wasActive = false;

    /**
     * Set to true when any widget access the current window.
     */
    boolean accessed = false;

    /**
     * Set when collapsing window to become only title-bar.
     */
    boolean collapsed = false;

    /**
     * == Visible && !Collapsed
     */
    boolean skipItems = false;

    /**
     * Number of Begin() during the current frame (generally 0 or 1, 1+ if appending via multiple Begin/End pairs).
     */
    int beginCount = 0;

    /**
     * ID in the popup stack when this window is used as a popup/menu (because we use generic Name/ID for recycling).
     */
    int popupId = 0;

    int autoFitFramesX = -1, autoFitFramesY = -1;

    boolean autoFitOnlyGrows = false;

    int autoPosLastDirection = -1;

    int hiddenFrames = 0;

    /**
     * bit ImGuiSetCond_*** specify if SetWindowPos() call will succeed with this particular flag.
     */
    int setWindowPosAllowFlags = SetCond.Always | SetCond.Once | SetCond.FirstUseEver | SetCond.Appearing;

    /**
     * bit ImGuiSetCond_*** specify if SetWindowSize() call will succeed with this particular flag.
     */
    int setWindowSizeAllowFlags = SetCond.Always | SetCond.Once | SetCond.FirstUseEver | SetCond.Appearing;

    /**
     * bit ImGuiSetCond_*** specify if SetWindowCollapsed() call will succeed with this particular flag.
     */
    int setWindowCollapsedAllowFlags = SetCond.Always | SetCond.Once | SetCond.FirstUseEver | SetCond.Appearing;

    boolean setWindowPosCenterWanted = false;

    /**
     * Temporary per-window data, reset at the beginning of the frame.
     */
    DrawContext dc;

    /**
     * ID stack. ID are hashes seeded with the value at the top of the stack.
     */
    ArrayList<Integer> idStack;

    /**
     * = DrawList->clip_rect_stack.back(). Scissoring / clipping rectangle. x1, y1, x2, y2.
     */
    Rect clipRect;

    /**
     * = WindowRect just after setup in Begin(). == window->Rect() for root window.
     */
    Rect windowRectClipped;

    int lastFrameActive = -1;

    float itemWidthDefault = 0.0f;

    /**
     * Simplified columns storage for menu items.
     */
    SimpleColumns menuColumns;

    Storage stateStorage;

    /**
     * Scale multiplier per-window.
     */
    float fontWindowScale = 1.0f;

    DrawList drawList;

    /**
     * If we are a child window, this is pointing to the first non-child parent window. Else point to ourself.
     */
    Window rootWindow = null;

    /**
     * If we are a child window, this is pointing to the first non-child non-popup parent window. Else point to ourself.
     */
    Window rootNonPopupWindow = null;

    /**
     * If we are a child window, this is pointing to our parent window. Else point to NULL.
     */
    Window parentWindow = null;

    /**
     * Focus.
     */
    /**
     * Start at -1 and increase as assigned via FocusItemRegister().
     */
    int focusIdxAllCounter = -1;

    /**
     * (same, but only count widgets which you can Tab through).
     */
    int focusIdxTabCounter = -1;

    /**
     * Item being requested for focus.
     */
    int focusIdxAllRequestCurrent = Integer.MAX_VALUE;

    /**
     * Tab-able item being requested for focus.
     */
    int focusIdxTabRequestCurrent = Integer.MAX_VALUE;

    /**
     * Item being requested for focus, for next update (relies on layout to be stable between the frame pressing TAB and
     * the next frame).
     */
    int focusIdxAllRequestNext = Integer.MAX_VALUE;

    int focusIdxTabRequestNext = Integer.MAX_VALUE;

    public Window(String name) {
        this.name = name;
        drawList = new DrawList();
        drawList.setOwnerName(name);
    }

    private static AtomicInteger idGenerator = new AtomicInteger();
}
