package imgui.internal.api

import imgui.classes.Context
import imgui.internal.classes.DockContext

// Docking
// (some functions are only declared in imgui.cpp, see Docking section)
interface docking {

    fun dockContextInitialize(ctx: Context) {
        val g = ctx
        assert(g.dockContext == null)
        g.dockContext = DockContext()

        // Add .ini handle for persistent docking data
//        ImGuiSettingsHandler ini_handler
//        ini_handler.TypeName = "Docking"
//        ini_handler.TypeHash = ImHashStr("Docking")
//        ini_handler.ReadOpenFn = DockSettingsHandler_ReadOpen
//        ini_handler.ReadLineFn = DockSettingsHandler_ReadLine
//        ini_handler.WriteAllFn = DockSettingsHandler_WriteAll
//        g.SettingsHandlers.push_back(ini_handler)
    }

    fun dockContextShutdown(ctx: Context) {
        val g = ctx
        val dc = ctx.dockContext!!
        dc.nodes.data.values.filter { it }
        for (int n = 0; n < dc->Nodes.Data.Size; n++)
        if (ImGuiDockNode* node = (ImGuiDockNode*)dc->Nodes.Data[n].val_p)
        IM_DELETE(node)
        IM_DELETE(g.DockContext)
        g.DockContext = NULL
    }

    IMGUI_API void          DockContextShutdown(ImGuiContext* ctx)
    IMGUI_API void          DockContextOnLoadSettings(ImGuiContext* ctx)
    IMGUI_API void          DockContextRebuildNodes(ImGuiContext* ctx)
    IMGUI_API void          DockContextUpdateUndocking(ImGuiContext* ctx)
    IMGUI_API void          DockContextUpdateDocking(ImGuiContext* ctx)
    IMGUI_API ImGuiID       DockContextGenNodeID(ImGuiContext* ctx)
    IMGUI_API void          DockContextQueueDock(ImGuiContext* ctx, ImGuiWindow* target, ImGuiDockNode* target_node, ImGuiWindow* payload, ImGuiDir split_dir, float split_ratio, bool split_outer)
    IMGUI_API void          DockContextQueueUndockWindow(ImGuiContext* ctx, ImGuiWindow* window)
    IMGUI_API void          DockContextQueueUndockNode(ImGuiContext* ctx, ImGuiDockNode* node)
    IMGUI_API bool          DockContextCalcDropPosForDocking(ImGuiWindow* target, ImGuiDockNode* target_node, ImGuiWindow* payload, ImGuiDir split_dir, bool split_outer, ImVec2* out_pos)
    inline ImGuiDockNode*   DockNodeGetRootNode(ImGuiDockNode* node) { while (node->ParentNode) node = node->ParentNode; return node; }
    inline ImGuiDockNode*   GetWindowDockNode() { ImGuiContext& g = *GImGui; return g.CurrentWindow->DockNode; }
    IMGUI_API bool          GetWindowAlwaysWantOwnTabBar(ImGuiWindow* window)
    IMGUI_API void          BeginDocked(ImGuiWindow* window, bool* p_open)
    IMGUI_API void          BeginDockableDragDropSource(ImGuiWindow* window)
    IMGUI_API void          BeginDockableDragDropTarget(ImGuiWindow* window)
    IMGUI_API void          SetWindowDock(ImGuiWindow* window, ImGuiID dock_id, ImGuiCond cond)
}