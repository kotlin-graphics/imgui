package imgui.api

import glm_.i
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.begin
import imgui.ImGui.beginChildFrame
import imgui.ImGui.beginCombo
import imgui.ImGui.beginTooltip
import imgui.ImGui.bulletText
import imgui.ImGui.button
import imgui.ImGui.checkbox
import imgui.ImGui.clearIniSettings
import imgui.ImGui.combo
import imgui.ImGui.debugNodeDrawList
import imgui.ImGui.debugNodeTabBar
import imgui.ImGui.debugNodeWindowSettings
import imgui.ImGui.debugNodeWindowsList
import imgui.ImGui.debugStartItemPicker
import imgui.ImGui.end
import imgui.ImGui.endChildFrame
import imgui.ImGui.endCombo
import imgui.ImGui.endTooltip
import imgui.ImGui.font
import imgui.ImGui.fontSize
import imgui.ImGui.foregroundDrawList
import imgui.ImGui.getForegroundDrawList
import imgui.ImGui.getID
import imgui.ImGui.indent
import imgui.ImGui.inputTextMultiline
import imgui.ImGui.io
import imgui.ImGui.isItemHovered
import imgui.ImGui.logFinish
import imgui.ImGui.logText
import imgui.ImGui.logToClipboard
import imgui.ImGui.popID
import imgui.ImGui.popTextWrapPos
import imgui.ImGui.pushID
import imgui.ImGui.pushTextWrapPos
import imgui.ImGui.sameLine
import imgui.ImGui.saveIniSettingsToDisk
import imgui.ImGui.saveIniSettingsToMemory
import imgui.ImGui.selectable
import imgui.ImGui.separator
import imgui.ImGui.setNextItemWidth
import imgui.ImGui.smallButton
import imgui.ImGui.style
import imgui.ImGui.styleColorsClassic
import imgui.ImGui.styleColorsDark
import imgui.ImGui.styleColorsLight
import imgui.ImGui.text
import imgui.ImGui.textDisabled
import imgui.ImGui.textEx
import imgui.ImGui.textLineHeightWithSpacing
import imgui.ImGui.textUnformatted
import imgui.ImGui.treeNode
import imgui.ImGui.treePop
import imgui.ImGui.unindent
import imgui.classes.Style
import imgui.demo.ExampleApp
import imgui.demo.showExampleApp.StyleEditor
import imgui.dsl.indent
import imgui.dsl.treeNode
import imgui.internal.*
import imgui.internal.classes.*
import kool.BYTES
import kotlin.reflect.KMutableProperty0
import imgui.WindowFlag as Wf

/**
 *  Message to the person tempted to delete this file when integrating ImGui into their code base:
 *  Do NOT remove this file from your project! It is useful reference code that you and other users will want to refer to.
 *  Don't do it! Do NOT remove this file from your project! It is useful reference code that you and other users will want to refer to.
 *  Everything in this file will be stripped out by the linker if you don't call ImGui::ShowDemoWindow().
 *  During development, you can call ImGui::ShowDemoWindow() in your code to learn about various features of ImGui.
 *  Removing this file from your project is hindering your access to documentation, likely leading you to poorer usage of the library.
 *  During development, you can call ImGui::ShowDemoWindow() in your code to learn about various features of ImGui. Have it wired in a debug menu!
 *  Removing this file from your project is hindering access to documentation for everyone in your team, likely leading you to poorer usage of the library.
 *
 *  Note that you can #define IMGUI_DISABLE_DEMO_WINDOWS in imconfig.h for the same effect.
 *  If you want to link core ImGui in your final builds but not those demo windows, #define IMGUI_DISABLE_DEMO_WINDOWS in imconfig.h and those functions will be empty.
 *  In other situation, when you have ImGui available you probably want this to be available for reference and execution.
 *
 *  Thank you,
 *  -Your beloved friend, imgui_demo.cpp (that you won't delete)
 *
 *
 *  Demo, Debug, Information
 */
interface demoDebugInformations {

    //-----------------------------------------------------------------------------
    // [SECTION] About Window / ShowAboutWindow()
    // Access from Dear ImGui Demo -> Tools -> About
    //-----------------------------------------------------------------------------

    /** Create Demo window.
     *  Demonstrate most Dear ImGui features (this is big function!)
     *  You may execute this function to experiment with the UI and understand what it does.
     *  You may then search for keywords in the code when you are interested by a specific feature. */
    fun showDemoWindow(open: BooleanArray) {
        showWindow = open[0]
        showDemoWindow(Companion::showWindow)
        open[0] = showWindow
    }

    /** create Demo window. demonstrate most ImGui features. call this to learn about the library! try to make it always available in your application! */
    fun showDemoWindow(open: KMutableProperty0<Boolean>) {
        // Exceptionally add an extra assert here for people confused about initial Dear ImGui setup
        // Most ImGui functions would normally just crash if the context is missing.
        assert(gImGui != null) { "Missing dear imgui context. Refer to examples app!" }
        ExampleApp(open)
    }

    /** create Metrics/Debugger window. display Dear ImGui internals: windows, draw commands, various internal state, etc. */
    fun showMetricsWindow(open: KMutableProperty0<Boolean>) {

        if (!begin("Dear ImGui Metrics/Debugger", open)) {
            end()
            return
        }

        val cfg = g.debugMetricsConfig

        // Basic info
        text("Dear ImGui $version")
        text("Application average %.3f ms/frame (%.1f FPS)", 1000f / io.framerate, io.framerate)
        text("${io.metricsRenderVertices} vertices, ${io.metricsRenderIndices} indices (${io.metricsRenderIndices / 3} triangles)")
        text("${io.metricsActiveWindows} active windows (${io.metricsRenderWindows} visible)")
        text("${io.metricsAllocs} active allocations")
        //SameLine(); if (SmallButton("GC")) { g.GcCompactAll = true; }

        separator()

        // Debugging enums
        if (cfg.showWindowsRectsType < 0)
            cfg.showWindowsRectsType = WRT.WorkRect.ordinal
        if (cfg.showTablesRectsType < 0)
            cfg.showTablesRectsType = TRT.WorkRect.ordinal

        // Helper functions to display common structures:
        // - NodeDrawList()
        // - NodeColumns()
        // - NodeWindow()
        // - NodeWindows()
        // - NodeTabBar()
        // - NodeStorage()
        // -> Funcs objects

        // Tools
        treeNode("Tools") {

            // The Item Picker tool is super useful to visually select an item and break into the call-stack of where it was submitted.
            if (button("Item Picker..")) debugStartItemPicker()
            sameLine()
            helpMarker("Will call the IM_DEBUG_BREAK() macro to break in debugger.\nWarning: If you don't have a debugger attached, this will probably crash.")

            checkbox("Show windows begin order", ::showWindowsBeginOrder)
            checkbox("Show windows rectangles", ::showWindowsRects)
            sameLine()
            setNextItemWidth(fontSize * 12)
            _i = showWindowsRectType.ordinal
            showWindowsRects = showWindowsRects || combo("##show_windows_rect_type", ::_i, WRT.names, WRT.names.size)
            showWindowsRectType = WRT.values()[_i]
            if (showWindowsRects) g.navWindow?.let { nav ->
                bulletText("'${nav.name}':")
                indent {
                    for (rectN in WRT.values()) {
                        val r = Funcs.getWindowRect(nav, rectN)
                        text("(%6.1f,%6.1f) (%6.1f,%6.1f) Size (%6.1f,%6.1f) ${WRT.names[rectN.ordinal]}",
                             r.min.x, r.min.y, r.max.x, r.max.y, r.width, r.height)
                    }
                }
            }
            checkbox("Show ImDrawCmd mesh when hovering", ::showDrawcmdMesh)
            checkbox("Show ImDrawCmd bounding boxes when hovering", ::showDrawcmdAabb)

            checkbox("Show tables rectangles", cfg::showTablesRects)
            sameLine()
            setNextItemWidth(fontSize * 12)
            cfg.showTablesRects = combo("##show_table_rects_type", cfg::showTablesRectsType, TRT.names, TRT.names.size) or cfg.showTablesRects
            val nav = g.navWindow
            if (cfg.showTablesRects && nav != null)
                for (tableN in 0 until g.tables.size) {
                    val table = g.tables.getByIndex(tableN)
                    if (table.lastFrameActive < g.frameCount - 1 || (table.outerWindow !== nav && table.innerWindow !== nav))
                        continue

                    bulletText("Table 0x%08X (${table.columnsCount} columns, in '${table.outerWindow!!.name}')", table.id)
                    if (isItemHovered())
                        foregroundDrawList.addRect(table.outerRect.min - 1, table.outerRect.max + 1, COL32(255, 255, 0, 255), 0f, 0.inv(), 2f)
                    indent()
                    for (rectN in TRT.values()) {
                        if (rectN >= TRT.ColumnsRect) {
                            if (rectN != TRT.ColumnsRect && rectN != TRT.ColumnsClipRect)
                                continue
                            for (columnN in 0 until table.columnsCount) {
                                val r = Funcs.getTableRect(table, rectN, columnN)
                                val buf = "(%6.1f,%6.1f) (%6.1f,%6.1f) Size (%6.1f,%6.1f) Col $columnN ${rectN.name}"
                                    .format(r.min.x, r.min.y, r.max.x, r.max.y, r.width, r.height)
                                selectable(buf)
                                if (isItemHovered())
                                    foregroundDrawList.addRect(r.min - 1, r.max + 1, COL32(255, 255, 0, 255), 0f, 0.inv(), 2f)
                            }
                        } else {
                            val r = Funcs.getTableRect(table, rectN, -1)
                            val buf = "(%6.1f,%6.1f) (%6.1f,%6.1f) Size (%6.1f,%6.1f) ${rectN.name}".format(
                                r.min.x, r.min.y, r.max.x, r.max.y, r.width, r.height)
                            selectable(buf)
                            if (isItemHovered())
                                foregroundDrawList.addRect(r.min - 1, r.max + 1, COL32(255, 255, 0, 255), 0f, 0.inv(), 2f)
                        }
                    }
                    unindent()
                }
        }

        // Contents
        debugNodeWindowsList(g.windows, "Windows")
        //DebugNodeWindowList(&g.WindowsFocusOrder, "WindowsFocusOrder");
        treeNode("DrawLists", "Active DrawLists (${g.drawDataBuilder.layers[0].size})") {
            for (layer in g.drawDataBuilder.layers[0])
                debugNodeDrawList(null, layer, "DrawList")
        }

        // Details for Popups
        if (treeNode("Popups", "Popups (${g.openPopupStack.size})")) {
            for (popup in g.openPopupStack) {
                val window = popup.window
                val childWindow = if (window != null && window.flags has Wf._ChildWindow) " ChildWindow" else ""
                val childMenu = if (window != null && window.flags has Wf._ChildMenu) " ChildMenu" else ""
                bulletText("PopupID: %08x, Window: '${window?.name}'$childWindow$childMenu", popup.popupId)
            }
            treePop()
        }

        // Details for TabBars
        treeNode("TabBars", "Tab Bars (${g.tabBars.size})") {
            for (n in 0 until g.tabBars.size)
                debugNodeTabBar(g.tabBars[n]!!, "TabBar")

        }

        //        #ifdef IMGUI_HAS_TABLE
        //                if (ImGui::TreeNode("Tables", "Tables (%d)", g.Tables.GetSize()))
        //                {
        //                    for (int n = 0; n < g.Tables.GetSize(); n++)
        //                    Funcs::NodeTable(g.Tables.GetByIndex(n));
        //                    ImGui::TreePop();
        //                }
        //        #endif // #define IMGUI_HAS_TABLE
        //
        // Details for Docking
        //        #ifdef IMGUI_HAS_DOCK
        treeNode("Dock nodes") {

        } //        #endif // #define IMGUI_HAS_DOCK

        // Settings
        treeNode("Settings") {
            if (smallButton("Clear")) clearIniSettings()
            sameLine()
            if (smallButton("Save to memory")) saveIniSettingsToMemory()
            sameLine()
            if (smallButton("Save to disk")) saveIniSettingsToDisk(io.iniFilename)
            sameLine()
            if (io.iniFilename != null) text("\"${io.iniFilename}\"")
            else textUnformatted("<NULL>")
            text("SettingsDirtyTimer %.2f", g.settingsDirtyTimer)
            treeNode("SettingsHandlers", "Settings handlers: (${g.settingsHandlers.size})") {
                g.settingsHandlers.forEach { bulletText(it.typeName) }
            }
            treeNode("SettingsWindows", "Settings packed data: Windows: ${g.settingsWindows.size} bytes") {
                g.settingsWindows.forEach(::debugNodeWindowSettings)
            } //            #ifdef IMGUI_HAS_TABLE
            //            treeNode("SettingsTables", "Settings packed data: Tables: ${g.settingsTables.size} bytes") {
            //                g.settingsTables.forEach(Funcs::nodeTableSettings)
            //            }
            //            #endif

            //            #ifdef IMGUI_HAS_DOCK
            //            #endif

            treeNode("SettingsIniData", "Settings unpacked data (.ini): ${g.settingsIniData.toByteArray().size} bytes") {
                val size = Vec2(-Float.MIN_VALUE, ImGui.textLineHeight * 20)
                inputTextMultiline("##Ini", g.settingsIniData, size, InputTextFlag.ReadOnly.i)
            }
        }

        // Misc Details
        if (treeNode("Internal state")) {

            text("WINDOWING")
            indent {
                text("HoveredWindow: '${g.hoveredWindow?.name}'")
                text("HoveredRootWindow: '${g.hoveredWindow?.name}'")
                text("HoveredWindowUnderMovingWindow: '${g.hoveredWindowUnderMovingWindow?.name}'")/*  Data is "in-flight" so depending on when the Metrics window is called we may see current frame
                    information or not                 */
                text("MovingWindow: '${g.movingWindow?.name ?: "NULL"}'")
            }

            text("ITEMS")
            indent {
                text("ActiveId: 0x%08X/0x%08X (%.2f sec), AllowOverlap: ${g.activeIdAllowOverlap}, Source: ${g.activeIdSource}",
                     g.activeId,
                     g.activeIdPreviousFrame,
                     g.activeIdTimer)
                text("ActiveIdWindow: '${g.activeIdWindow?.name}'")
                text("HoveredId: 0x%08X/0x%08X (%.2f sec), AllowOverlap: ${g.hoveredIdAllowOverlap.i}",
                     g.hoveredId,
                     g.hoveredIdPreviousFrame,
                     g.hoveredIdTimer) // Data is "in-flight" so depending on when the Metrics window is called we may see current frame information or not
                text("DragDrop: ${g.dragDropActive.i}, SourceId = 0x%08X, Payload \"${g.dragDropPayload.dataType}\" (${g.dragDropPayload.dataSize} bytes)",
                     g.dragDropPayload.sourceId)
            }

            text("NAV,FOCUS")
            indent {
                text("NavWindow: '${g.navWindow?.name}'")
                text("NavId: 0x%08X, NavLayer: ${g.navLayer}", g.navId)
                text("NavInputSource: ${g.navInputSource}")
                text("NavActive: ${io.navActive}, NavVisible: ${io.navVisible}")
                text("NavActivateId: 0x%08X, NavInputId: 0x%08X", g.navActivateId, g.navInputId)
                text("NavDisableHighlight: ${g.navDisableHighlight}, NavDisableMouseHover: ${g.navDisableMouseHover}")
                text("NavFocusScopeId = 0x%08X", g.navFocusScopeId)
                text("NavWindowingTarget: '${g.navWindowingTarget?.name}'")
            }

            treePop()
        }

        // Overlay: Display windows Rectangles and Begin Order
        if (showWindowsRects || showWindowsBeginOrder) for (window in g.windows) {
            if (!window.wasActive) continue
            val drawList = getForegroundDrawList(window)
            if (showWindowsRects) {
                val r = Funcs.getWindowRect(window, showWindowsRectType)
                drawList.addRect(r.min, r.max, COL32(255, 0, 128, 255))
            }
            if (showWindowsBeginOrder && window.flags hasnt Wf._ChildWindow) {
                val buf = "${window.beginOrderWithinContext}"
                drawList.addRectFilled(window.pos, window.pos + Vec2(fontSize), COL32(200, 100, 100, 255))
                drawList.addText(window.pos, COL32(255), buf)
            }
        }

        // Overlay: Display Tables Rectangles
        if (cfg.showTablesRects)
            for (tableN in 0 until g.tables.size) {
                val table = g.tables.getByIndex(tableN)
                if (table.lastFrameActive < g.frameCount - 1)
                    continue
                val drawList = getForegroundDrawList(table.outerWindow)
                if (cfg.showTablesRectsType >= TRT.ColumnsRect.ordinal) {
                    for (columnN in 0 until table.columnsCount) {
                        val r = Funcs.getTableRect(table, TRT.values()[cfg.showTablesRectsType], columnN)
                        val col = if (table.hoveredColumnBody == columnN) COL32(255, 255, 128, 255) else COL32(255, 0, 128, 255)
                        val thickness = if (table.hoveredColumnBody == columnN) 3f else 1f
                        drawList.addRect(r.min, r.max, col, 0f, 0.inv(), thickness)
                    }
                } else {
                    val r = Funcs.getTableRect(table, TRT.values()[cfg.showTablesRectsType], -1)
                    drawList.addRect(r.min, r.max, COL32(255, 0, 128, 255))
                }
            }

        //
        //        #ifdef IMGUI_HAS_DOCK
        //        // Overlay: Display Docking info
        //        if (show_docking_nodes && g.IO.KeyCtrl)
        //        {
        //        }
        //        #endif // #define IMGUI_HAS_DOCK

        end()
    }

    /** create About window. display Dear ImGui version, credits and build/system information. */
    fun showAboutWindow(open: KMutableProperty0<Boolean>) {

        if (!begin("About Dear ImGui", open, Wf.AlwaysAutoResize.i)) {
            end()
            return
        }

        // Basic info
        text("Dear ImGui $version")
        separator()
        text("By Omar Cornut and all Dear Imgui contributors.")
        text("Dear ImGui is licensed under the MIT License, see LICENSE for more information.")

        checkbox("Config/Build Information", Companion::showConfigInfo)
        if (showConfigInfo) {

            val copyToClipboard = button("Copy to clipboard")
            val childSize = Vec2(0f, textLineHeightWithSpacing * 18)
            beginChildFrame(getID("cfginfos"), childSize, Wf.NoMove.i)
            if (copyToClipboard) {
                logToClipboard()
                logText("```\n") // Back quotes will make text appears without formatting when pasting on GitHub
            }

            text("Dear ImGui $version ($IMGUI_VERSION_NUM)")
            separator()
            text("sizeof(size_t): ${Int.BYTES}, sizeof(DrawIdx): ${DrawIdx.BYTES}, sizeof(DrawVert): ${DrawVert.SIZE}")
            text("IMGUI_USE_BGRA_PACKED_COLOR: $USE_BGRA_PACKED_COLOR")
            separator()
            text("io.backendPlatformName: ${io.backendPlatformName}")
            text("io.backendRendererName: ${io.backendRendererName}")
            text("io.configFlags: 0x%08X", io.configFlags) // @formatter:off
            if (io.configFlags has ConfigFlag.NavEnableKeyboard) text(" NavEnableKeyboard")
            if (io.configFlags has ConfigFlag.NavEnableGamepad) text(" NavEnableGamepad")
            if (io.configFlags has ConfigFlag.NavEnableSetMousePos) text(" NavEnableSetMousePos")
            if (io.configFlags has ConfigFlag.NavNoCaptureKeyboard) text(" NavNoCaptureKeyboard")
            if (io.configFlags has ConfigFlag.NoMouse) text(" NoMouse")
            if (io.configFlags has ConfigFlag.NoMouseCursorChange) text(" NoMouseCursorChange")
            if (io.mouseDrawCursor) text("io.mouseDrawCursor")
            if (io.configMacOSXBehaviors) text("io.configMacOSXBehaviors")
            if (io.configInputTextCursorBlink) text("io.configInputTextCursorBlink")
            if (io.configWindowsResizeFromEdges) text("io.configWindowsResizeFromEdges")
            if (io.configWindowsMoveFromTitleBarOnly) text("io.configWindowsMoveFromTitleBarOnly")
            if (io.configMemoryCompactTimer >= 0f) text("io.ConfigMemoryCompactTimer = %.1f",
                                                        io.configMemoryCompactTimer)
            text("io.backendFlags: 0x%08X", io.backendFlags)
            if (io.backendFlags has BackendFlag.HasGamepad) text(" HasGamepad")
            if (io.backendFlags has BackendFlag.HasMouseCursors) text(" HasMouseCursors")
            if (io.backendFlags has BackendFlag.HasSetMousePos) text(" HasSetMousePos")
            if (io.backendFlags has BackendFlag.RendererHasVtxOffset) text(" RendererHasVtxOffset") // @formatter:on
            separator()
            text("io.fonts: ${io.fonts.fonts.size} fonts, Flags: 0x%08X, TexSize: ${io.fonts.texSize.x},${io.fonts.texSize.y}",
                 io.fonts.flags)
            text("io.displaySize: ${io.displaySize.x},${io.displaySize.y}")
            text("io.displayFramebufferScale: %.2f,%.2f".format(io.displayFramebufferScale.x,
                                                                io.displayFramebufferScale.y))
            separator()
            text("style.windowPadding: %.2f,%.2f", style.windowPadding.x, style.windowPadding.y)
            text("style.windowBorderSize: %.2f", style.windowBorderSize)
            text("style.framePadding: %.2f,%.2f", style.framePadding.x, style.framePadding.y)
            text("style.frameRounding: %.2f", style.frameRounding)
            text("style.frameBorderSize: %.2f", style.frameBorderSize)
            text("style.itemSpacing: %.2f,%.2f", style.itemSpacing.x, style.itemSpacing.y)
            text("style.itemInnerSpacing: %.2f,%.2f", style.itemInnerSpacing.x, style.itemInnerSpacing.y)

            if (copyToClipboard) {
                logText("\n```\n")
                logFinish()
            }
            endChildFrame()
        }
        end()
    }

    /** add style editor block (not a window). you can pass in a reference ImGuiStyle structure to compare to,
     *  revert to and save to (else it uses the default style)  */
    fun showStyleEditor(ref: Style? = null) = StyleEditor.invoke(ref)

    /** Demo helper function to select among default colors. See showStyleEditor() for more advanced options.
     *  Here we use the simplified Combo() api that packs items into a single literal string.
     *  Useful for quick combo boxes where the choices are known locally.
     *
     *  add style selector block (not a window), essentially a combo listing the default styles. */
    fun showStyleSelector(label: String) =
        if (combo(label, Companion::styleIdx, "Dark\u0000Light\u0000Classic\u0000")) {
            when (styleIdx) {
                0 -> styleColorsDark()
                1 -> styleColorsLight()
                2 -> styleColorsClassic()
            }
            true
        } else false

    /** Demo helper function to select among loaded fonts.
     *  Here we use the regular beginCombo()/endCombo() api which is more the more flexible one.
     *
     *  add font selector block (not a window), essentially a combo listing the loaded fonts. */
    fun showFontSelector(label: String) {
        val fontCurrent = font
        if (beginCombo(label, fontCurrent.debugName)) {
            for (font in io.fonts.fonts) {
                pushID(font)
                if (selectable(font.debugName, font === fontCurrent)) io.fontDefault = font
                popID()
            }
            endCombo()
        }
        sameLine()
        helpMarker("""
            - Load additional fonts with io.Fonts->AddFontFromFileTTF().
            - The font atlas is built when calling io.Fonts->GetTexDataAsXXXX() or io.Fonts->Build().
            - Read FAQ and documentation in misc/fonts/ for more details.
            - If you need to add/remove fonts at runtime (e.g. for DPI change), do it before calling NewFrame().""")
    }

    /** Helper to display basic user controls. */
    fun showUserGuide() {
        bulletText("Double-click on title bar to collapse window.")
        bulletText("""
            Click and drag on lower corner to resize window
            (double-click to auto fit window to its contents).""".trimIndent())
        bulletText("CTRL+Click on a slider or drag box to input value as text.")
        bulletText("TAB/SHIFT+TAB to cycle through keyboard editable fields.")
        if (io.fontAllowUserScaling) bulletText("CTRL+Mouse Wheel to zoom window contents.")
        bulletText("While inputing text:\n")
        indent {
            bulletText("CTRL+Left/Right to word jump.")
            bulletText("CTRL+A or double-click to select all.")
            bulletText("CTRL+X/C/V to use clipboard cut/copy/paste.")
            bulletText("CTRL+Z,CTRL+Y to undo/redo.")
            bulletText("ESCAPE to revert.")
            bulletText("You can apply arithmetic operators +,*,/ on numerical values.\nUse +- to subtract.")
        }
        bulletText("With keyboard navigation enabled:")
        indent {
            bulletText("Arrow keys to navigate.")
            bulletText("Space to activate a widget.")
            bulletText("Return to input text into a widget.")
            bulletText("Escape to deactivate a widget, close popup, exit child window.")
            bulletText("Alt to jump to the menu layer of a window.")
            bulletText("CTRL+Tab to select a window.")
        }
    }

    /** get the compiled version string e.g. "1.80 WIP" (essentially the value for IMGUI_VERSION from the compiled version of imgui.cpp) */
    val version: String
        get() = IMGUI_VERSION

    companion object {

        // Debugging enums

        /** Windows Rect Type */
        enum class WRT {
            OuterRect, OuterRectClipped, InnerRect, InnerClipRect, WorkRect, Content, ContentIdeal, ContentRegionRect;

            companion object {
                val names = values().map { it.name }
            }
        }

        /** Tables Rect Type */
        enum class TRT {
            OuterRect, InnerRect, WorkRect, HostClipRect, InnerClipRect, BackgroundClipRect, ColumnsRect, ColumnsWorkRect,
            ColumnsClipRect, ColumnsContentHeadersUsed, ColumnsContentHeadersIdeal, ColumnsContentFrozen, ColumnsContentUnfrozen;

            companion object {
                val names = WRT.values().map { it.name }
            }
        }

        var showWindowsRects = false
        var showWindowsRectType = WRT.InnerClipRect
        var showWindowsBeginOrder = false
        var showTablesRects = false
        var showTablesRectType = TRT.WorkRect
        var showDrawcmdDetails = true
        var showDrawcmdMesh = true
        var showDrawcmdAabb = true

        var showWindow = false

        var showConfigInfo = false


        // Helper functions to display common structures:
        // - NodeDrawList()
        // - NodeColumns()
        // - NodeWindow()
        // - NodeWindows()
        // - NodeTabBar()
        // - NodeStorage()
        object Funcs {

            fun getTableRect(table: Table, rectType: TRT, n: Int): Rect = when (rectType) {
                TRT.OuterRect -> table.outerRect
                TRT.InnerRect -> table.innerRect
                TRT.WorkRect -> table.workRect
                TRT.HostClipRect -> table.hostClipRect
                TRT.InnerClipRect -> table.innerClipRect
                TRT.BackgroundClipRect -> table.bgClipRect
                TRT.ColumnsRect -> table.columns[n].let { c -> Rect(c.minX, table.innerClipRect.min.y, c.maxX, table.innerClipRect.min.y + table.lastOuterHeight) }
                TRT.ColumnsWorkRect -> table.columns[n].let { c -> Rect(c.workMinX, table.workRect.min.y, c.workMaxX, table.workRect.max.y) }
                TRT.ColumnsClipRect -> table.columns[n].clipRect
                TRT.ColumnsContentHeadersUsed -> table.columns[n].let { c -> Rect(c.workMinX, table.innerClipRect.min.y, c.contentMaxXHeadersUsed, table.innerClipRect.min.y + table.lastFirstRowHeight) } // Note: y1/y2 not always accurate
                TRT.ColumnsContentHeadersIdeal -> table.columns[n].let { c -> Rect(c.workMinX, table.innerClipRect.min.y, c.contentMaxXHeadersIdeal, table.innerClipRect.min.y + table.lastFirstRowHeight) }
                TRT.ColumnsContentFrozen -> table.columns[n].let { c -> Rect(c.workMinX, table.innerClipRect.min.y, c.contentMaxXFrozen, table.innerClipRect.min.y + table.lastFirstRowHeight) }
                TRT.ColumnsContentUnfrozen -> table.columns[n].let { c -> Rect(c.workMinX, table.innerClipRect.min.y + table.lastFirstRowHeight, c.contentMaxXUnfrozen, table.innerClipRect.max.y) }
            }

            fun getWindowRect(window: Window, rectType: WRT): Rect = when (rectType) {
                WRT.OuterRect -> window.rect()
                WRT.OuterRectClipped -> window.outerRectClipped
                WRT.InnerRect -> window.innerRect
                WRT.InnerClipRect -> window.innerClipRect
                WRT.WorkRect -> window.workRect
                WRT.Content -> {
                    val min = window.innerRect.min - window.scroll + window.windowPadding
                    Rect(min, min + window.contentSize)
                }
                WRT.ContentIdeal -> {
                    val min = window.innerRect.min - window.scroll + window.windowPadding
                    Rect(min, min + window.contentSize)
                }
                WRT.ContentRegionRect -> window.contentRegionRect
            }
        }

        val selected = BooleanArray(4 + 3 + 16 + 16) { it == 1 || it == 23 + 0 || it == 23 + 5 || it == 23 + 10 || it == 23 + 15 }

        var styleIdx = -1

        /** Helper to display a little (?) mark which shows a tooltip when hovered.
         *  In your own code you may want to display an actual icon if you are using a merged icon fonts (see docs/FONTS.txt)    */
        fun helpMarker(desc: String) {
            textDisabled("(?)")
            if (isItemHovered()) {
                beginTooltip()
                pushTextWrapPos(fontSize * 35f)
                textEx(desc)
                popTextWrapPos()
                endTooltip()
            }
        }
    }
}