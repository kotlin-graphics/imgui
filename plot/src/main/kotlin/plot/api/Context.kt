package plot.api

//-----------------------------------------------------------------------------
// [SECTION] Contexts
//-----------------------------------------------------------------------------

// [JVM] all -> PlotContext class

// Creates a new ImPlot context. Call this after ImGui::CreateContext.
//IMPLOT_API ImPlotContext* CreateContext();
//// Destroys an ImPlot context. Call this before ImGui::DestroyContext. nullptr = destroy current context.
//IMPLOT_API void DestroyContext(ImPlotContext* ctx = nullptr);
//// Returns the current ImPlot context. nullptr if no context has ben set.
//IMPLOT_API ImPlotContext* GetCurrentContext();
//// Sets the current ImPlot context.
//IMPLOT_API void SetCurrentContext(ImPlotContext* ctx);
//
//// Sets the current **ImGui** context. This is ONLY necessary if you are compiling
//// ImPlot as a DLL (not recommended) separate from your ImGui compilation. It
//// sets the global variable GImGui, which is not shared across DLL boundaries.
//// See GImGui documentation in imgui.cpp for more details.
//IMPLOT_API void SetImGuiContext(ImGuiContext* ctx);
