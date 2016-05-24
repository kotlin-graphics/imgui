/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui;

/**
 * Flags for ImGui::TreeNode*(), ImGui::CollapsingHeader*().
 * 
 * @author GBarbieri
 */
public interface TreeNodeFlags {

    /**
     * Draw as selected.
     */
    public static final int Selected = 1 << 0;

    /**
     * Full colored frame (e.g. for CollapsingHeader).
     */
    public static final int Framed = 1 << 1;

    /**
     * Hit testing to allow subsequent widgets to overlap this one.
     */
    public static final int AllowOverlapMode = 1 << 2;

    /**
     * Don't do a TreePush() when open (e.g. for CollapsingHeader) = no extra indent nor pushing on ID stack.
     */
    public static final int NoTreePushOnOpen = 1 << 3;

    /**
     * Don't automatically and temporarily open node when Logging is active (by default logging will automatically open tree
     * nodes).
     */
    public static final int NoAutoOpenOnLog = 1 << 4;

    /**
     * Default node to be open.
     */
    public static final int DefaultOpen = 1 << 5;

    /**
     * Need double-click to open node.
     */
    public static final int OpenOnDoubleClick = 1 << 6;

    /**
     * Only open when clicking on the arrow part. If public static final int OpenOnDoubleClick is also set, single-click
     * arrow or double-click all box to open.
     */
    public static final int OpenOnArrow = 1 << 7;

    /**
     * No collapsing, no arrow (use as a convenience for leaf nodes).
     */
    public static final int AlwaysOpen = 1 << 8;
    //public static final int UnindentArrow      = 1 << 9,   // FIXME: TODO: Unindent tree so that Label is aligned to current X position
    //public static final int SpanAllAvailWidth  = 1 << 10,  // FIXME: TODO: Extend hit box horizontally even if not framed
    //public static final int NoScrollOnOpen     = 1 << 11,  // FIXME: TODO: Automatically scroll on TreePop() if node got just open and contents is not visible

    public static final int CollapsingHeader = Framed | NoAutoOpenOnLog;
}
