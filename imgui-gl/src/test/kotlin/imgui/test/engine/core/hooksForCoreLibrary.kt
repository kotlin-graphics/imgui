package imgui.test.engine.core

//-------------------------------------------------------------------------
// Hooks for Core Library
//-------------------------------------------------------------------------

void                ImGuiTestEngineHook_PreNewFrame(ImGuiContext* ctx);
void                ImGuiTestEngineHook_PostNewFrame(ImGuiContext* ctx);
void                ImGuiTestEngineHook_ItemAdd(ImGuiContext* ctx, const ImRect& bb, ImGuiID id);
void                ImGuiTestEngineHook_ItemInfo(ImGuiContext* ctx, ImGuiID id, const char* label, ImGuiItemStatusFlags flags);
void                ImGuiTestEngineHook_Log(ImGuiContext* ctx, const char* fmt, ...);
void                ImGuiTestEngineHook_AssertFunc(const char* expr, const char* file, const char* function, int line);
