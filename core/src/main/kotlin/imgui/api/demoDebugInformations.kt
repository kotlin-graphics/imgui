package imgui.api

import glm_.*
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.begin
import imgui.ImGui.beginChild
import imgui.ImGui.beginChildFrame
import imgui.ImGui.beginCombo
import imgui.ImGui.beginTable
import imgui.ImGui.beginTooltip
import imgui.ImGui.bulletText
import imgui.ImGui.button
import imgui.ImGui.calcTextSize
import imgui.ImGui.checkbox
import imgui.ImGui.checkboxFlags
import imgui.ImGui.clearIniSettings
import imgui.ImGui.clipboardText
import imgui.ImGui.combo
import imgui.ImGui.data
import imgui.ImGui.debugLocateItemOnHover
import imgui.ImGui.debugNodeDrawList
import imgui.ImGui.debugNodeInputTextState
import imgui.ImGui.debugNodeTabBar
import imgui.ImGui.debugNodeTable
import imgui.ImGui.debugNodeTableSettings
import imgui.ImGui.debugNodeViewport
import imgui.ImGui.debugNodeWindowSettings
import imgui.ImGui.debugNodeWindowsList
import imgui.ImGui.debugNodeWindowsListByBeginStackParent
import imgui.ImGui.debugRenderKeyboardPreview
import imgui.ImGui.debugStartItemPicker
import imgui.ImGui.debugTextEncoding
import imgui.ImGui.end
import imgui.ImGui.endChild
import imgui.ImGui.endChildFrame
import imgui.ImGui.endCombo
import imgui.ImGui.endTable
import imgui.ImGui.endTooltip
import imgui.ImGui.findWindowByID
import imgui.ImGui.font
import imgui.ImGui.fontSize
import imgui.ImGui.foregroundDrawList
import imgui.ImGui.getForegroundDrawList
import imgui.ImGui.getID
import imgui.ImGui.getInstanceData
import imgui.ImGui.getKeyChordName
import imgui.ImGui.getOwnerData
import imgui.ImGui.indent
import imgui.ImGui.inputText
import imgui.ImGui.inputTextMultiline
import imgui.ImGui.io
import imgui.ImGui.isClicked
import imgui.ImGui.isDown
import imgui.ImGui.isItemHovered
import imgui.ImGui.isMouseHoveringRect
import imgui.ImGui.isPressed
import imgui.ImGui.isReleased
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
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.setScrollHereY
import imgui.ImGui.showFontAtlas
import imgui.ImGui.smallButton
import imgui.ImGui.style
import imgui.ImGui.styleColorsClassic
import imgui.ImGui.styleColorsDark
import imgui.ImGui.styleColorsLight
import imgui.ImGui.tableHeadersRow
import imgui.ImGui.tableNextColumn
import imgui.ImGui.tableSetBgColor
import imgui.ImGui.tableSetupColumn
import imgui.ImGui.text
import imgui.ImGui.textColored
import imgui.ImGui.textDisabled
import imgui.ImGui.textEx
import imgui.ImGui.textLineHeightWithSpacing
import imgui.ImGui.textUnformatted
import imgui.ImGui.treeNode
import imgui.ImGui.treeNodeToLabelSpacing
import imgui.ImGui.treePop
import imgui.ImGui.unindent
import imgui.classes.ListClipper
import imgui.classes.Style
import imgui.demo.DemoWindow
import imgui.demo.showExampleApp.StyleEditor
import imgui.dsl.indent
import imgui.dsl.listBox
import imgui.dsl.treeNode
import imgui.internal.DrawIdx
import imgui.internal.DrawVert
import imgui.internal.api.debugTools.Companion.metricsHelpMarker
import imgui.internal.classes.*
import imgui.internal.formatString
import imgui.internal.sections.KeyOwner_None
import imgui.internal.sections.NextWindowDataFlag
import imgui.internal.sections.testEngine_FindItemDebugLabel
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
        // Most functions would normally just crash if the context is missing.
        DemoWindow(open)
    }

    /** create Metrics/Debugger window. display Dear ImGui internals: windows, draw commands, various internal state, etc. */
    fun showMetricsWindow(open: KMutableProperty0<Boolean>) {

        val cfg = g.debugMetricsConfig
        if (cfg.showDebugLog)
            showDebugLogWindow(cfg::showDebugLog)
        if (cfg.showStackTool)
            showStackToolWindow(cfg::showStackTool)

        if (!begin("Dear ImGui Metrics/Debugger", open) || ImGui.currentWindow.beginCount > 1) {
            end()
            return
        }

        // Basic info
        text("Dear ImGui $IMGUI_VERSION ($IMGUI_VERSION_NUM)")
        text("Application average %.3f ms/frame (%.1f FPS)", 1000f / io.framerate, io.framerate)
        text("${io.metricsRenderVertices} vertices, ${io.metricsRenderIndices} indices (${io.metricsRenderIndices / 3} triangles)")
        text("${io.metricsRenderWindows} visible windows, ${io.metricsActiveAllocations} active allocations")
        text("If your company uses this, please consider sponsoring the project!")
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

            val showEncodingViewer = treeNode("UTF-8 Encoding viewer")
            sameLine()
            metricsHelpMarker("You can also call ImGui::DebugTextEncoding() from your code with a given string to test that your UTF-8 encoding settings are correct.")
            if (showEncodingViewer) {
                setNextItemWidth(-Float.MIN_VALUE)
                inputText("##Text", buf)
                if (buf.isNotEmpty())
                    debugTextEncoding(buf)
                treePop()
            }

            // The Item Picker tool is super useful to visually select an item and break into the call-stack of where it was submitted.
            if (checkbox("Show Item Picker", g::debugItemPickerActive) && g.debugItemPickerActive)
                debugStartItemPicker()
            sameLine()
            metricsHelpMarker("Will call the IM_DEBUG_BREAK() macro to break in debugger.\nWarning: If you don't have a debugger attached, this will probably crash.")

            // Stack Tool is your best friend!
            checkbox("Show Debug Log", cfg::showDebugLog)
            sameLine()
            metricsHelpMarker("You can also call ImGui::ShowDebugLogWindow() from your code.")

            // Stack Tool is your best friend!
            checkbox("Show Stack Tool", cfg::showStackTool)
            sameLine()
            helpMarker("You can also call ImGui::ShowStackToolWindow() from your code.")

            checkbox("Show windows begin order", ::showWindowsBeginOrder)
            checkbox("Show windows rectangles", ::showWindowsRects)
            sameLine()
            setNextItemWidth(fontSize * 12)
            val ordinalRef = showWindowsRectType.ordinal.mutableReference
            val ordinal by ordinalRef
            showWindowsRects = showWindowsRects || combo("##show_windows_rect_type", ordinalRef, WRT.names, WRT.names.size)
            showWindowsRectType = WRT.values()[ordinal]
            if (showWindowsRects) g.navWindow?.let { nav ->
                bulletText("'${nav.name}':")
                indent {
                    for (rectN in WRT.values()) {
                        val r = Funcs.getWindowRect(nav, rectN)
                        text("(%6.1f,%6.1f) (%6.1f,%6.1f) Size (%6.1f,%6.1f) ${WRT.names[rectN.ordinal]}", r.min.x, r.min.y, r.max.x, r.max.y, r.width, r.height)
                    }
                }
            }

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
                        foregroundDrawList.addRect(table.outerRect.min - 1, table.outerRect.max + 1, COL32(255, 255, 0, 255), thickness = 2f)
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
                                    foregroundDrawList.addRect(r.min - 1, r.max + 1, COL32(255, 255, 0, 255), thickness = 2f)
                            }
                        } else {
                            val r = Funcs.getTableRect(table, rectN, -1)
                            val buf = "(%6.1f,%6.1f) (%6.1f,%6.1f) Size (%6.1f,%6.1f) ${rectN.name}".format(
                                    r.min.x, r.min.y, r.max.x, r.max.y, r.width, r.height)
                            selectable(buf)
                            if (isItemHovered())
                                foregroundDrawList.addRect(r.min - 1, r.max + 1, COL32(255, 255, 0, 255), thickness = 2f)
                        }
                    }
                    unindent()
                }
        }

        // Windows
        treeNode("Windows", "Windows (${g.windows.size})") {
            //SetNextItemOpen(true, ImGuiCond_Once);
            debugNodeWindowsList(g.windows, "By display order")
            debugNodeWindowsList(g.windowsFocusOrder, "By focus order (root windows)")
            treeNode("By submission order (begin stack)") {
                // Here we display windows in their submitted order/hierarchy, however note that the Begin stack doesn't constitute a Parent<>Child relationship!
                val tempBuffer = g.windowsTempSortBuffer
                tempBuffer.clear()
                for (i in g.windows.indices)
                    if (g.windows[i].lastFrameActive + 1 >= g.frameCount)
                        tempBuffer += g.windows[i]
                tempBuffer.sortBy(Window::beginOrderWithinContext)
                debugNodeWindowsListByBeginStackParent(tempBuffer, null)
            }
        }

        // DrawLists
        val drawlistCount = g.viewports.sumOf { it.drawDataBuilder.drawListCount }
        treeNode("DrawLists", "Active DrawLists ($drawlistCount)") {
            checkbox("Show ImDrawCmd mesh when hovering", cfg::showDrawCmdMesh)
            checkbox("Show ImDrawCmd bounding boxes when hovering", cfg::showDrawCmdBoundingBoxes)
            for (viewport in g.viewports)
                for (layer in viewport.drawDataBuilder.layers)
                    for (drawListIdx in layer.indices)
                        debugNodeDrawList(null, layer[drawListIdx], "DrawList")
        }

        // Viewports
        treeNode("Viewports", "Viewports (${g.viewports.size})") {
            indent(treeNodeToLabelSpacing) {
                renderViewportsThumbnails()
            }
            for (viewport in g.viewports)
                debugNodeViewport(viewport)
        }

        // Details for Popups
        if (treeNode("Popups", "Popups (${g.openPopupStack.size})")) {
            for (popupData in g.openPopupStack) {
                // As it's difficult to interact with tree nodes while popups are open, we display everything inline.
                val window = popupData.window
                val windowName = window?.name ?: "NULL"
                val childWindow = if (window != null && window.flags has Wf._ChildWindow) "Child;" else ""
                val childMenu = if (window != null && window.flags has Wf._ChildMenu) "Menu;" else ""
                val backupName = popupData.backupNavWindow?.name ?: "NULL"
                val parentName = window?.parentWindow?.name ?: "NULL"
                bulletText("PopupID: %08x, Window: '$windowName' ($childWindow$childMenu), BackupNavWindow '$backupName', ParentWindow '$parentName'", popupData.popupId)
            }
            treePop()
        }

        // Details for TabBars
        treeNode("TabBars", "Tab Bars (${g.tabBars.size})") {
            for (n in 0 until g.tabBars.size)
                debugNodeTabBar(g.tabBars[n]!!, "TabBar")

        }

        treeNode("Tables", "Tables (${g.tables.size})") {
            for (n in 0 until g.tables.size)
                debugNodeTable(g.tables.getByIndex(n))
        }

        // Details for Fonts
        val atlas = g.io.fonts
        treeNode("Fonts", "Fonts (${atlas.fonts.size})") {
            showFontAtlas(atlas)
        }

        // Details for InputText
        treeNode("InputText") {
            debugNodeInputTextState(g.inputTextState)
        }


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
            checkbox("io.ConfigDebugIniSettings", io::configDebugIniSettings)
            text("SettingsDirtyTimer %.2f", g.settingsDirtyTimer)
            treeNode("SettingsHandlers", "Settings handlers: (${g.settingsHandlers.size})") {
                g.settingsHandlers.forEach { bulletText("\"${it.typeName}\"") }
            }
            treeNode("SettingsWindows", "Settings packed data: Windows: ${g.settingsWindows.size} bytes") {
                g.settingsWindows.forEach(::debugNodeWindowSettings)
            }

            treeNode("SettingsTables", "Settings packed data: Tables: ${g.settingsTables.size} bytes") {
                g.settingsTables.forEach(::debugNodeTableSettings)
            }

            //            #ifdef IMGUI_HAS_DOCK
            //            #endif

            treeNode("SettingsIniData", "Settings unpacked data (.ini): ${g.settingsIniData.toByteArray().size} bytes") {
                val size = Vec2(-Float.MIN_VALUE, ImGui.textLineHeight * 20)
                inputTextMultiline("##Ini", g.settingsIniData, size, InputTextFlag.ReadOnly)
            }
        }

        treeNode("Inputs") {
            text("KEYBOARD/GAMEPAD/MOUSE KEYS")
            // We iterate both legacy native range and named ImGuiKey ranges, which is a little odd but this allows displaying the data for old/new backends.
            // User code should never have to go through such hoops! You can generally iterate between ImGuiKey_NamedKey_BEGIN and ImGuiKey_NamedKey_END.
            indent {
                //                #ifdef IMGUI_DISABLE_OBSOLETE_KEYIO
                //                    struct funcs { static bool IsLegacyNativeDupe(ImGuiKey) { return false; } };
                //                #else
                //                struct funcs { static bool IsLegacyNativeDupe(ImGuiKey key) { return key < 512 && GetIO().KeyMap[key] != -1; } }; // Hide Native<>ImGuiKey duplicates when both exists in the array
                //Text("Legacy raw:");      for (ImGuiKey key = ImGuiKey_KeysData_OFFSET; key < ImGuiKey_COUNT; key++) { if (io.KeysDown[key]) { SameLine(); Text("\"%s\" %d", GetKeyName(key), key); } }
                //                #endif
                text("Keys down:")
                for (key in Key.Data) {
                    if (!key.isDown) continue
                    sameLine(); text('"' + key.name + '"'); sameLine(); text("(%.02f)", key.data.downDuration)
                }
                text("Keys pressed:")
                for (key in Key.Data) {
                    if (!key.isPressed) continue
                    sameLine(); text('"' + key.name + '"')
                }
                text("Keys released:")
                for (key in Key.Data) {
                    if (!key.isReleased) continue
                    sameLine(); text('"' + key.name + '"')
                }
                text("Keys mods: ${if (io.keyCtrl) "CTRL " else ""}${if (io.keyShift) "SHIFT " else ""}${if (io.keyAlt) "ALT " else ""}${if (io.keySuper) "SUPER " else ""}")
                text("Chars queue:")
                for (c in io.inputQueueCharacters) {
                    sameLine(); text("'${if (c > ' ' && c <= 255) c else '?'}\' (0x%04X)", c)
                } // FIXME: We should convert 'c' to UTF-8 here but the functions are not public.
                debugRenderKeyboardPreview(ImGui.windowDrawList)
            }

            text("MOUSE STATE")
            indent {
                if (ImGui.isMousePosValid())
                    text("Mouse pos: (%g, %g)", io.mousePos.x, io.mousePos.y)
                else
                    text("Mouse pos: <INVALID>")
                text("Mouse delta: (%g, %g)", io.mouseDelta.x, io.mouseDelta.y)
                val count = io.mouseDown.size
                text("Mouse down:"); for (i in 0 until count) if (MouseButton.of(i).isDown) {; sameLine(); text("b$i (%.02f secs)", io.mouseDownDuration[i]); }
                text("Mouse clicked:"); for (i in 0 until count) if (MouseButton.of(i).isClicked) {; sameLine(); text("b$i (${io.mouseClickedCount[i]})"); }
                text("Mouse released:"); for (i in 0 until count) if (MouseButton.of(i).isReleased) {; sameLine(); text("b$i"); }
                text("Mouse wheel: %.1f", io.mouseWheel)
                text("mouseStationaryTimer: %.2f", g.mouseStationaryTimer)
                text("Mouse source: " + io.mouseSource)
                text("Pen Pressure: %.1f", io.penPressure) // Note: currently unused
            }

            text("MOUSE WHEELING")
            indent {
                text("WheelingWindow: '${g.wheelingWindow?.name ?: "NULL"}'")
                text("WheelingWindowReleaseTimer: %.2f", g.wheelingWindowReleaseTimer)
                val axis = if (g.wheelingAxisAvg.x > g.wheelingAxisAvg.y) "X" else if (g.wheelingAxisAvg.x < g.wheelingAxisAvg.y) "Y" else "<none>"
                text("WheelingAxisAvg[] = { %.3f, %.3f }, Main Axis: %s", g.wheelingAxisAvg.x, g.wheelingAxisAvg.y)
            }

            text("KEY OWNERS")
            indent {
                listBox("##owners", Vec2(-Float.MIN_VALUE, textLineHeightWithSpacing * 6)) {
                    for (key in Key.Named) {
                        val ownerData = key.getOwnerData(g)
                        if (ownerData.ownerCurr == KeyOwner_None)
                            continue
                        text("$key: 0x%08X${if (ownerData.lockUntilRelease) " LockUntilRelease" else if (ownerData.lockThisFrame) " LockThisFrame" else ""}", ownerData.ownerCurr)
                        debugLocateItemOnHover(ownerData.ownerCurr)
                    }
                }
            }

            text("SHORTCUT ROUTING")
            indent {
                listBox("##routes", Vec2(-Float.MIN_VALUE, textLineHeightWithSpacing * 8)) {
                    for (key in Key.Named) {
                        val rt = g.keysRoutingTable
                        var idx = rt.index[key]
                        while (idx != -1) {
                            val routingData = rt.entries[idx]
                            val keyChordName = getKeyChordName(key or routingData.mods)
                            text("$keyChordName: 0x%08X", routingData.routingCurr)
                            debugLocateItemOnHover(routingData.routingCurr)
                            idx = routingData.nextEntryIndex
                        }
                    }
                }
                text("(ActiveIdUsing: AllKeyboardKeys: %d, NavDirMask: 0x%X)", g.activeIdUsingAllKeyboardKeys, g.activeIdUsingNavDirMask)
            }
        }

        if (treeNode("Internal state")) {

            // [JVM] redundant
            //            const char* input_source_names[] = { "None", "Mouse", "Keyboard", "Gamepad", "Clipboard" }; IM_ASSERT(IM_ARRAYSIZE(input_source_names) == ImGuiInputSource_COUNT);

            text("WINDOWING")
            indent {
                text("HoveredWindow: '${g.hoveredWindow?.name}'")
                text("HoveredWindow->Root: '${g.hoveredWindow?.rootWindow!!.name}'")
                text("HoveredWindowUnderMovingWindow: '${g.hoveredWindowUnderMovingWindow?.name}'")/*  Data is "in-flight" so depending on when the Metrics window is called we may see current frame
                    information or not                 */
                text("MovingWindow: '${g.movingWindow?.name ?: "NULL"}'")
            }

            text("ITEMS")
            indent {
                text("ActiveId: 0x%08X/0x%08X (%.2f sec), AllowOverlap: ${g.activeIdAllowOverlap}, Source: ${g.activeIdSource}", g.activeId, g.activeIdPreviousFrame, g.activeIdTimer)
                debugLocateItemOnHover(g.activeId)
                text("ActiveIdWindow: '${g.activeIdWindow?.name}'")

                text("ActiveIdUsing: AllKeyboardKeys: ${g.activeIdUsingAllKeyboardKeys} NavDirMask: %X", g.activeIdUsingNavDirMask)
                text("HoveredId: 0x%08X (%.2f sec), AllowOverlap: ${g.hoveredIdAllowOverlap.i}", g.hoveredIdPreviousFrame, g.hoveredIdTimer) // Not displaying g.HoveredId as it is update mid-frame
                text("HoverItemDelayId: 0x%08X, Timer: %.2f, ClearTimer: %.2f", g.hoverItemDelayId, g.hoverItemDelayTimer, g.hoverItemDelayClearTimer)
                text("DragDrop: ${g.dragDropActive.i}, SourceId = 0x%08X, Payload \"${g.dragDropPayload.dataType}\" (${g.dragDropPayload.dataSize} bytes)", g.dragDropPayload.sourceId)
                debugLocateItemOnHover(g.dragDropPayload.sourceId)
            }

            text("NAV,FOCUS")
            indent {
                text("NavWindow: '${g.navWindow?.name}'")
                text("NavId: 0x%08X, NavLayer: ${g.navLayer}", g.navId)
                debugLocateItemOnHover(g.navId)
                text("NavInputSource: ${g.navInputSource}")
                text("NavActive: ${io.navActive}, NavVisible: ${io.navVisible}")
                text("NavActivateId/DownId/PressedId: %08X/%08X/%08X/%08X", g.navActivateId, g.navActivateDownId, g.navActivatePressedId)
                text("NavActivateFlags: %04X", g.navActivateFlags)
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
                        drawList.addRect(r.min, r.max, col, thickness = thickness)
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

    /** create Debug Log window. display a simplified log of important dear imgui events. */
    fun showDebugLogWindow(pOpen: KMutableProperty0<Boolean>? = null) {
        if (g.nextWindowData.flags hasnt NextWindowDataFlag.HasSize)
            setNextWindowSize(Vec2(0f, ImGui.fontSize * 12f), Cond.FirstUseEver)
        if (!begin("Dear ImGui Debug Log", pOpen) || ImGui.currentWindow.beginCount > 1) {
            end()
            return
        }

        checkboxFlags("All", g::debugLogFlags, DebugLogFlag.EventMask)
        sameLine(); checkboxFlags("ActiveId", g::debugLogFlags, DebugLogFlag.EventActiveId)
        sameLine(); checkboxFlags("Focus", g::debugLogFlags, DebugLogFlag.EventFocus)
        sameLine(); checkboxFlags("Popup", g::debugLogFlags, DebugLogFlag.EventPopup)
        sameLine(); checkboxFlags("Nav", g::debugLogFlags, DebugLogFlag.EventNav)
        sameLine(); checkboxFlags("Clipper", g::debugLogFlags, DebugLogFlag.EventClipper)
        //SameLine(); CheckboxFlags("Selection", &g.DebugLogFlags, ImGuiDebugLogFlags_EventSelection);
        sameLine(); checkboxFlags("IO", g::debugLogFlags, DebugLogFlag.EventIO)

        if (smallButton("Clear")) {
            g.debugLogBuf.clear()
            g.debugLogIndex.clear()
        }
        sameLine()
        if (smallButton("Copy"))
            clipboardText = g.debugLogBuf.toString()
        beginChild("##log", Vec2(), true, Wf.AlwaysVerticalScrollbar or Wf.AlwaysHorizontalScrollbar)

        val clipper = ListClipper()
        clipper.begin(g.debugLogIndex.size)
        while (clipper.step())
            for (lineNo in clipper.displayStart until clipper.displayEnd) {
                val lineBegin = g.debugLogIndex getLineBegin lineNo
                val lineEnd = g.debugLogIndex getLineEnd lineNo
                textUnformatted(g.debugLogBuf.toString(), lineEnd)
                val textRect = g.lastItemData.rect
                if (isItemHovered()) {
                    var p = lineBegin
                    while (p <= lineEnd - 10) {
                        val buf = g.debugLogBuf.toString()
                        val id: ID = buf.drop(2).parseInt()
                        if (buf[p] != '0' || (buf[p + 1] != 'x' && buf[p + 1] != 'X')/* || sscanf(p + 2, "%X", & id) != 1*/)
                            continue
                        val p0 = calcTextSize(buf.drop(lineBegin).take(p - lineBegin))
                        val p1 = calcTextSize(buf.drop(p).take(10))
                        g.lastItemData.rect.put(textRect.min + Vec2(p0.x, 0f), textRect.min + Vec2(p0.x + p1.x, p1.y))
                        if (isMouseHoveringRect(g.lastItemData.rect.min, g.lastItemData.rect.max, true))
                            debugLocateItemOnHover(id)
                        p += 10
                    }
                }
            }
        if (ImGui.scrollY >= ImGui.scrollMaxY)
            setScrollHereY(1f)
        endChild()

        end()
        clipper.end()
    }

    /** create Stack Tool window. hover items with mouse to query information about the source of their unique ID.
     *
     *  Stack Tool: Display UI     */
    fun showStackToolWindow(pOpen: KMutableProperty0<Boolean>? = null) {

        if (g.nextWindowData.flags hasnt NextWindowDataFlag.HasSize)
            setNextWindowSize(Vec2(0f, fontSize * 8f), Cond.FirstUseEver)
        if (!begin("Dear ImGui Stack Tool", pOpen) || ImGui.currentWindow.beginCount > 1) {
            end()
            return
        }

        // Display hovered/active status
        val tool = g.debugStackTool
        val hoveredId = g.hoveredIdPreviousFrame
        val activeId = g.activeId
        if (IMGUI_ENABLE_TEST_ENGINE)
            text("HoveredId: 0x%08X (\"${if (hoveredId != 0) testEngine_FindItemDebugLabel(g, hoveredId) else ""}\"), ActiveId:  0x%08X (\"${if (activeId != 0) testEngine_FindItemDebugLabel(g, activeId) else ""}\")", hoveredId, activeId)
        else
            text("HoveredId: 0x%08X, ActiveId:  0x%08X", hoveredId, activeId)

        sameLine()
        metricsHelpMarker("Hover an item with the mouse to display elements of the ID Stack leading to the item's final ID.\nEach level of the stack correspond to a PushID() call.\nAll levels of the stack are hashed together to make the final ID of a widget (ID displayed at the bottom level of the stack).\nRead FAQ entry about the ID stack for details.")

        // CTRL+C to copy path
        val timeSinceCopy = g.time.f - tool.copyToClipboardLastTime
        checkbox("Ctrl+C: copy path to clipboard", tool::copyToClipboardOnCtrlC)
        sameLine()
        textColored(if (timeSinceCopy >= 0f && timeSinceCopy < 0.75f && glm.mod(timeSinceCopy, 0.25f) < 0.25f * 0.5f) Vec4(1f, 1f, 0.3f, 1f) else Vec4(), "*COPIED*")
        if (tool.copyToClipboardOnCtrlC && Key.Mod_Ctrl.isDown && Key.C.isPressed) {
            tool.copyToClipboardLastTime = g.time.f
            var p = 0 //g.tempBuffer
            val pEnd = g.tempBuffer.size
            var stackN = 0
            while (stackN < tool.results.size && p + 3 < pEnd) {
                g.tempBuffer[p++] = '/'.code.toByte()
                val levelDesc = ByteArray(256)
                stackToolFormatLevelInfo(tool, stackN, false, levelDesc)
                var n = 0
                while (levelDesc[n] != 0.b && p + 2 < pEnd) {
                    if (levelDesc[n] == '/'.code.b)
                        g.tempBuffer[p++] = '\\'.code.b
                    g.tempBuffer[p++] = levelDesc[n]
                    n++
                }
                stackN++
            }
            g.tempBuffer[p] = 0.b
            clipboardText = g.tempBuffer.cStr
        }

        // Display decorated stack
        tool.lastActiveFrame = g.frameCount
        if (tool.results.isNotEmpty() && beginTable("##table", 3, TableFlag.Borders)) {

            val idWidth = calcTextSize("0xDDDDDDDD").x
            tableSetupColumn("Seed", TableColumnFlag.WidthFixed, idWidth)
            tableSetupColumn("PushID", TableColumnFlag.WidthStretch)
            tableSetupColumn("Result", TableColumnFlag.WidthFixed, idWidth)
            tableHeadersRow()
            for (n in tool.results.indices) {
                val info = tool.results[n]
                tableNextColumn()
                text("0x%08X", if (n > 0) tool.results[n - 1].id else 0)
                tableNextColumn()

                stackToolFormatLevelInfo(tool, n, true, g.tempBuffer)
                textUnformatted(g.tempBuffer.cStr)
                tableNextColumn()
                text("0x%08X", info.id)
                if (n == tool.results.lastIndex)
                    tableSetBgColor(TableBgTarget.CellBg, Col.Header.u32)
            }
            endTable()
        }
        end()
    }

    /** create About window. display Dear ImGui version, credits and build/system information. */
    object ShowAboutWindow {
        var showConfigInfo = false
        operator fun invoke(open: KMutableProperty0<Boolean>) {

            if (!begin("About Dear ImGui", open, Wf.AlwaysAutoResize)) {
                end()
                return
            }

            // Basic info
            text("Dear ImGui ${ImGui.version}")
            separator()
            text("By Omar Cornut and all Dear Imgui contributors.")
            text("Dear ImGui is licensed under the MIT License, see LICENSE for more information.")

            checkbox("Config/Build Information", ::showConfigInfo)
            if (showConfigInfo) {

                val copyToClipboard = button("Copy to clipboard")
                val childSize = Vec2(0f, textLineHeightWithSpacing * 18)
                beginChildFrame(getID("cfginfos"), childSize, Wf.NoMove)
                if (copyToClipboard) {
                    logToClipboard()
                    logText("```\n") // Back quotes will make text appears without formatting when pasting on GitHub
                }

                text("Dear ImGui ${ImGui.version} ($IMGUI_VERSION_NUM)")
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
                if (io.configMemoryCompactTimer >= 0f) text("io.ConfigMemoryCompactTimer = %.1f", io.configMemoryCompactTimer)
                text("io.backendFlags: 0x%08X", io.backendFlags)
                if (io.backendFlags has BackendFlag.HasGamepad) text(" HasGamepad")
                if (io.backendFlags has BackendFlag.HasMouseCursors) text(" HasMouseCursors")
                if (io.backendFlags has BackendFlag.HasSetMousePos) text(" HasSetMousePos")
                if (io.backendFlags has BackendFlag.RendererHasVtxOffset) text(" RendererHasVtxOffset") // @formatter:on
                separator()
                text("io.fonts: ${io.fonts.fonts.size} fonts, Flags: 0x%08X, TexSize: ${io.fonts.texSize.x},${io.fonts.texSize.y}", io.fonts.flags)
                text("io.displaySize: ${io.displaySize.x},${io.displaySize.y}")
                text("io.displayFramebufferScale: %.2f,%.2f".format(io.displayFramebufferScale.x, io.displayFramebufferScale.y))
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
    }

    /** add style editor block (not a window). you can pass in a reference ImGuiStyle structure to compare to,
     *  revert to and save to (else it uses the default style)  */
    fun showStyleEditor(ref: Style? = null) = StyleEditor.invoke(ref)

    object ShowStyleSelector {
        var styleIdx = -1

        /** Demo helper function to select among default colors. See showStyleEditor() for more advanced options.
         *  Here we use the simplified Combo() api that packs items into a single literal string.
         *  Useful for quick combo boxes where the choices are known locally.
         *
         *  add style selector block (not a window), essentially a combo listing the default styles. */
        operator fun invoke(label: String) =
                if (combo(label, ::styleIdx, "Dark\u0000Light\u0000Classic\u0000")) {
                    when (styleIdx) {
                        0 -> styleColorsDark()
                        1 -> styleColorsLight()
                        2 -> styleColorsClassic()
                    }
                    true
                } else false
    }

    /** Demo helper function to select among loaded fonts.
     *  Here we use the regular BeginCombo()/EndCombo() api which is the more flexible one.
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

    /** Helper to display basic user controls.
     *
     *  add basic help/info block (not a window): how to manipulate ImGui as an end-user (mouse/keyboard controls). */
    fun showUserGuide() {
        bulletText("Double-click on title bar to collapse window.")
        bulletText("""
            Click and drag on lower corner to resize window
            (double-click to auto fit window to its contents).""".trimIndent())
        bulletText("CTRL+Click on a slider or drag box to input value as text.")
        bulletText("TAB/SHIFT+TAB to cycle through keyboard editable fields.")
        bulletText("CTRL+Tab to select a window.")
        if (io.fontAllowUserScaling)
            bulletText("CTRL+Mouse Wheel to zoom window contents.")
        bulletText("While inputing text:\n")
        indent {
            bulletText("CTRL+Left/Right to word jump.")
            bulletText("CTRL+A or double-click to select all.")
            bulletText("CTRL+X/C/V to use clipboard cut/copy/paste.")
            bulletText("CTRL+Z,CTRL+Y to undo/redo.")
            bulletText("ESCAPE to revert.")
        }
        bulletText("With keyboard navigation enabled:")
        indent {
            bulletText("Arrow keys to navigate.")
            bulletText("Space to activate a widget.")
            bulletText("Return to input text into a widget.")
            bulletText("Escape to deactivate a widget, close popup, exit child window.")
            bulletText("Alt to jump to the menu layer of a window.")
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


        // Helper functions to display common structures:
        // - NodeDrawList()
        // - NodeColumns()
        // - NodeWindow()
        // - NodeWindows()
        // - NodeTabBar()
        // - NodeStorage()
        object Funcs {

            fun getTableRect(table: Table, rectType: TRT, n: Int): Rect {
                val tableInstance = table getInstanceData table.instanceCurrent // Always using last submitted instance
                return when (rectType) {
                    TRT.OuterRect -> table.outerRect
                    TRT.InnerRect -> table.innerRect
                    TRT.WorkRect -> table.workRect
                    TRT.HostClipRect -> table.hostClipRect
                    TRT.InnerClipRect -> table.innerClipRect
                    TRT.BackgroundClipRect -> table.bgClipRect
                    TRT.ColumnsRect -> table.columns[n].let { c -> Rect(c.minX, table.innerClipRect.min.y, c.maxX, table.innerClipRect.min.y + tableInstance.lastOuterHeight) }
                    TRT.ColumnsWorkRect -> table.columns[n].let { c -> Rect(c.workMinX, table.workRect.min.y, c.workMaxX, table.workRect.max.y) }
                    TRT.ColumnsClipRect -> table.columns[n].clipRect
                    TRT.ColumnsContentHeadersUsed -> table.columns[n].let { c -> Rect(c.workMinX, table.innerClipRect.min.y, c.contentMaxXHeadersUsed, table.innerClipRect.min.y + tableInstance.lastFirstRowHeight) } // Note: y1/y2 not always accurate
                    TRT.ColumnsContentHeadersIdeal -> table.columns[n].let { c -> Rect(c.workMinX, table.innerClipRect.min.y, c.contentMaxXHeadersIdeal, table.innerClipRect.min.y + tableInstance.lastFirstRowHeight) }
                    TRT.ColumnsContentFrozen -> table.columns[n].let { c -> Rect(c.workMinX, table.innerClipRect.min.y, c.contentMaxXFrozen, table.innerClipRect.min.y + tableInstance.lastFrozenHeight) }
                    TRT.ColumnsContentUnfrozen -> table.columns[n].let { c -> Rect(c.workMinX, table.innerClipRect.min.y + tableInstance.lastFrozenHeight, c.contentMaxXUnfrozen, table.innerClipRect.max.y) }
                }
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


        /** Helper to display a little (?) mark which shows a tooltip when hovered.
         *  In your own code you may want to display an actual icon if you are using a merged icon fonts (see docs/FONTS.txt)    */
        fun helpMarker(desc: String) {
            textDisabled("(?)")
            if (isItemHovered(HoveredFlag.DelayShort) && beginTooltip()) {
                pushTextWrapPos(fontSize * 35f)
                textEx(desc)
                popTextWrapPos()
                endTooltip()
            }
        }

        fun renderViewportsThumbnails() {
            val window = g.currentWindow!!

            // We don't display full monitor bounds (we could, but it often looks awkward), instead we display just enough to cover all of our viewports.
            val SCALE = 1f / 8f
            val bbFull = Rect(Float.MAX_VALUE, -Float.MAX_VALUE)
            for (viewport in g.viewports)
                bbFull add viewport.mainRect
            val p = window.dc.cursorPos // careful, class instance
            val off = p - bbFull.min * SCALE
            for (viewport in g.viewports) {
                val viewportDrawBb = Rect(off + (viewport.pos) * SCALE, off + (viewport.pos + viewport.size) * SCALE)
                ImGui.debugRenderViewportThumbnail(window.drawList, viewport, viewportDrawBb)
            }
            ImGui.dummy(bbFull.size * SCALE)
        }

        fun stackToolFormatLevelInfo(tool: StackTool, n: Int, formatForUi: Boolean, buf: ByteArray): Int {
            val info = tool.results[n]
            val desc = info.desc.toByteArray()
            val window = if (desc[0] == 0.b && n == 0) findWindowByID(info.id) else null
            if (window != null)                                                                 // Source: window name (because the root ID don't call GetID() and so doesn't get hooked)
                return formatString(buf, if (formatForUi) "\"%s\" [window]" else "%s", window.name)
            if (info.querySuccess)                                                     // Source: GetID() hooks (prioritize over ItemInfo() because we frequently use patterns like: PushID(str), Button("") where they both have same id)
                return formatString(buf, if (formatForUi && info.dataType == DataType._String) "\"%s\"" else "%s", info.desc)
            if (tool.stackLevel < tool.results.size)                                  // Only start using fallback below when all queries are done, so during queries we don't flickering ??? markers.
                return 0.also { buf[0] = 0 }
            if (IMGUI_ENABLE_TEST_ENGINE)
                testEngine_FindItemDebugLabel(gImGui, info.id)?.let { label ->     // Source: ImGuiTestEngine's ItemInfo()
                    formatString(buf, if (formatForUi) "??? \"%s\"" else "%s", label)
                }
            return formatString(buf, "???")
        }

        val buf = ""
    }
}