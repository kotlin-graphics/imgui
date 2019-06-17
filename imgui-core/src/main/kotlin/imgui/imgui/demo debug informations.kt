package imgui.imgui

import glm_.BYTES
import glm_.f
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.begin
import imgui.ImGui.beginChildFrame
import imgui.ImGui.beginCombo
import imgui.ImGui.beginTooltip
import imgui.ImGui.bulletText
import imgui.ImGui.button
import imgui.ImGui.checkbox
import imgui.ImGui.combo
import imgui.ImGui.combo1
import imgui.ImGui.cursorScreenPos
import imgui.ImGui.dummy
import imgui.ImGui.end
import imgui.ImGui.endChildFrame
import imgui.ImGui.endCombo
import imgui.ImGui.endTooltip
import imgui.ImGui.font
import imgui.ImGui.fontSize
import imgui.ImGui.frameCount
import imgui.ImGui.getId
import imgui.ImGui.inputFloat
import imgui.ImGui.io
import imgui.ImGui.isItemHovered
import imgui.ImGui.logFinish
import imgui.ImGui.logToClipboard
import imgui.ImGui.menuItem
import imgui.ImGui.popId
import imgui.ImGui.popTextWrapPos
import imgui.ImGui.pushId
import imgui.ImGui.pushTextWrapPos
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.separator
import imgui.ImGui.setNextItemWidth
import imgui.ImGui.sliderFloat
import imgui.ImGui.smallButton
import imgui.ImGui.style
import imgui.ImGui.styleColorsClassic
import imgui.ImGui.styleColorsDark
import imgui.ImGui.styleColorsLight
import imgui.ImGui.text
import imgui.ImGui.textColored
import imgui.ImGui.textDisabled
import imgui.ImGui.textEx
import imgui.ImGui.textLineHeight
import imgui.ImGui.textLineHeightWithSpacing
import imgui.ImGui.treeNode
import imgui.ImGui.treePop
import imgui.ImGui.windowDrawList
import imgui.dsl.child
import imgui.dsl.indent
import imgui.dsl.menu
import imgui.imgui.demo.ExampleApp
import imgui.imgui.demo.showExampleApp.StyleEditor
import imgui.imgui.imgui_colums.Companion.offsetNormToPixels
import imgui.imgui.imgui_miscellaneousUtilities.Companion.getForegroundDrawList
import imgui.internal.*
import kool.lim
import java.nio.ByteBuffer
import java.util.*
import kotlin.reflect.KMutableProperty0
import imgui.ColorEditFlag as Cef
import imgui.InputTextFlag as Itf
import imgui.SelectableFlag as Sf
import imgui.TreeNodeFlag as Tnf
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
interface imgui_demoDebugInformations {

    /** Create Demo window.
     *  Demonstrate most Dear ImGui features (this is big function!)
     *  You may execute this function to experiment with the UI and understand what it does.
     *  You may then search for keywords in the code when you are interested by a specific feature. */
    fun showDemoWindow(open: BooleanArray) {
        showWindow = open[0]
        showDemoWindow(Companion::showWindow)
        open[0] = showWindow
    }

    fun showDemoWindow(open: KMutableProperty0<Boolean>) {
        assert(gImGui != null) { "Missing dear imgui context. Refer to examples app!" } // Exceptionally add an extra assert here for people confused with initial dear imgui setup
        ExampleApp(open)
    }

    //-----------------------------------------------------------------------------
    // [SECTION] About Window / ShowAboutWindow()
    // Access from ImGui Demo -> Help -> About
    //-----------------------------------------------------------------------------

    /** create About window. display Dear ImGui version, credits and build/system information. */
    fun showAboutWindow(open: KMutableProperty0<Boolean>) {

        if (!begin("About Dear ImGui", open, Wf.AlwaysAutoResize.i)) {
            end()
            return
        }
        text("Dear ImGui $version")
        separator()
        text("By Omar Cornut and all dear imgui contributors.")
        text("Dear ImGui is licensed under the MIT License, see LICENSE for more information.")

        checkbox("Config/Build Information", ::showConfigInfo)
        if (showConfigInfo) {

            val copyToClipboard = button("Copy to clipboard")
            beginChildFrame(getId("cfginfos"), Vec2(0, textLineHeightWithSpacing * 18), Wf.NoMove.i)
            if (copyToClipboard)
                logToClipboard()

            text("Dear ImGui $version ($IMGUI_VERSION_NUM)")
            separator()
            text("sizeof(size_t): ${Int.BYTES}, sizeof(DrawIdx): ${DrawIdx.BYTES}, sizeof(DrawVert): ${DrawVert.size}")
            text("IMGUI_USE_BGRA_PACKED_COLOR: $USE_BGRA_PACKED_COLOR")
            separator()
            text("io.backendPlatformName: ${io.backendPlatformName}")
            text("io.backendRendererName: ${io.backendRendererName}")
            text("io.configFlags: 0x%08X", io.configFlags)
            // @formatter:off
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
            text("io.backendFlags: 0x%08X", io.backendFlags)
            if (io.backendFlags has BackendFlag.HasGamepad) text(" HasGamepad")
            if (io.backendFlags has BackendFlag.HasMouseCursors) text(" HasMouseCursors")
            if (io.backendFlags has BackendFlag.HasSetMousePos) text(" HasSetMousePos")
            if (io.backendFlags has BackendFlag.RendererHasVtxOffset)           text(" RendererHasVtxOffset")
            // @formatter:on
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

            if (copyToClipboard)
                logFinish()
            endChildFrame()
        }
        end()
    }

    /** Create Metrics/Debug window. display Dear ImGui internals: draw commands (with individual draw calls and vertices),
     *  window list, basic internal state, etc.    */
    fun showMetricsWindow(open: KMutableProperty0<Boolean>) {

        if (!begin("Dear ImGui Metrics", open)) {
            end()
            return
        }

        text("Dear ImGui $version")
        text("Application average %.3f ms/frame (%.1f FPS)", 1000f / io.framerate, io.framerate)
        text("${io.metricsRenderVertices} vertices, ${io.metricsRenderIndices} indices (${io.metricsRenderIndices / 3} triangles)")
        text("${io.metricsActiveWindows} active windows (${io.metricsRenderWindows} visible)")
        text("${io.metricsAllocs} active allocations")
        separator()

        Funcs.nodeWindows(g.windows, "Windows")
        if (treeNode("DrawList", "Active DrawLists (${g.drawDataBuilder.layers[0].size})")) {
            g.drawDataBuilder.layers.forEach { layer -> layer.forEach { Funcs.nodeDrawList(null, it, "DrawList") } }
            treePop()
        }

        if (treeNode("Popups", "Popups (${g.openPopupStack.size})")) {
            for (popup in g.openPopupStack) {
                val window = popup.window
                val childWindow = if (window != null && window.flags has Wf.ChildWindow) " ChildWindow" else ""
                val childMenu = if (window != null && window.flags has Wf.ChildMenu) " ChildMenu" else ""
                bulletText("PopupID: %08x, Window: '${window?.name}'$childWindow$childMenu", popup.popupId)
            }
            treePop()
        }

        if (treeNode("TabBars", "Tab Bars (${g.tabBars.size})")) {
            for (n in 0 until g.tabBars.size)
                Funcs.nodeTabBar(g.tabBars[n]!!)
            treePop()
        }

        if (treeNode("Internal state")) {
            text("HoveredWindow: '${g.hoveredWindow?.name}'")
            text("HoveredRootWindow: '${g.hoveredWindow?.name}'")
            /*  Data is "in-flight" so depending on when the Metrics window is called we may see current frame
                information or not                 */
            text("HoveredId: 0x%08X/0x%08X (%.2f sec), AllowOverlap: ${g.hoveredIdAllowOverlap}", g.hoveredId, g.hoveredIdPreviousFrame, g.hoveredIdTimer)
            text("ActiveId: 0x%08X/0x%08X (%.2f sec), AllowOverlap: ${g.activeIdAllowOverlap}, Source: ${g.activeIdSource}", g.activeId, g.activeIdPreviousFrame, g.activeIdTimer)
            text("ActiveIdWindow: '${g.activeIdWindow?.name}'")
            text("MovingWindow: '${g.movingWindow?.name}'")
            text("NavWindow: '${g.navWindow?.name}'")
            text("NavId: 0x%08X, NavLayer: ${g.navLayer}", g.navId)
            text("NavInputSource: ${g.navInputSource}")
            text("NavActive: ${io.navActive}, NavVisible: ${io.navVisible}")
            text("NavActivateId: 0x%08X, NavInputId: 0x%08X", g.navActivateId, g.navInputId)
            text("NavDisableHighlight: ${g.navDisableHighlight}, NavDisableMouseHover: ${g.navDisableMouseHover}")
            text("NavWindowingTarget: '${g.navWindowingTarget?.name}'")
            text("DragDrop: ${g.dragDropActive}, SourceId = 0x%08X, Payload \"${g.dragDropPayload.dataTypeS}\" " +
                    "(${g.dragDropPayload.dataSize} bytes)", g.dragDropPayload.sourceId)
            treePop()
        }

        if (treeNode("Tools")) {
            checkbox("Show windows begin order", ::showWindowsBeginOrder)
            checkbox("Show windows rectangles", ::showWindowsRects)
            sameLine()
            setNextItemWidth(fontSize * 12)
            showWindowsRects = showWindowsRects || combo1("##rects_type", ::showWindowsRectType, "OuterRect\u0000OuterRectClipped\u0000InnerVisibleRect\u0000InnerWorkRect\u0000InnerWorkRectClipped\u0000ContentsRegionRect\u0000")
            if (showWindowsRects)
                g.navWindow?.let { nav ->
                    val r = Funcs.getRect(nav, showWindowsRectType)
                    bulletText("'${nav.name}': (%.1f,%.1f) (%.1f,%.1f) Size (%.1f,%.1f)", r.min.x, r.min.y, r.max.x, r.max.y, r.width, r.height)
                }
            checkbox("Show clipping rectangle when hovering ImDrawCmd node", ::showDrawcmdClipRects)
            treePop()
        }

        if (showWindowsRects || showWindowsBeginOrder)
            for (window in g.windows) {
                if (!window.wasActive)
                    continue
                val drawList = getForegroundDrawList(window)
                if (showWindowsRects) {
                    val r = Funcs.getRect(window, showWindowsRectType)
                    drawList.addRect(r.min, r.max, COL32(255, 0, 128, 255))
                }
                if (showWindowsBeginOrder && window.flags hasnt Wf.ChildWindow) {
                    val buf = "${window.beginOrderWithinContext}".toCharArray(CharArray(32))
                    drawList.addRectFilled(window.pos, window.pos + Vec2(fontSize), COL32(200, 100, 100, 255))
                    drawList.addText(window.pos, COL32(255, 255, 255, 255), buf)
                }
            }
    }

    /** add style editor block (not a window). you can pass in a reference ImGuiStyle structure to compare to,
     *  revert to and save to (else it uses the default style)  */
    fun showStyleEditor(ref: Style? = null) = StyleEditor.invoke(ref)

    /** Demo helper function to select among default colors. See showStyleEditor() for more advanced options.
     *  Here we use the simplified combo() api that packs items into a single literal string. Useful for quick combo
     *  boxes where the choices are known locally.
     *
     *  add style selector block (not a window), essentially a combo listing the default styles. */
    fun showStyleSelector(label: String) =
            if (combo(label, ::styleIdx, "Classic\u0000Dark\u0000Light\u0000")) {
                when (styleIdx) {
                    0 -> styleColorsClassic()
                    1 -> styleColorsDark()
                    2 -> styleColorsLight()
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
                pushId(font)
                if (selectable(font.debugName, font === fontCurrent))
                    io.fontDefault = font
                popId()
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
        bulletText("Click and drag on lower right corner to resize window\n(double-click to auto fit window to its contents).")
        bulletText("Click and drag on any empty space to move window.")
        bulletText("TAB/SHIFT+TAB to cycle through keyboard editable fields.")
        bulletText("CTRL+Click on a slider or drag box to input value as text.")
        if (io.fontAllowUserScaling)
            bulletText("CTRL+Mouse Wheel to zoom window contents.")
        bulletText("Mouse Wheel to scroll.")
        bulletText("While editing text:\n")
        indent {
            bulletText("Hold SHIFT or use mouse to select text.")
            bulletText("CTRL+Left/Right to word jump.")
            bulletText("CTRL+A or double-click to select all.")
            bulletText("CTRL+X,CTRL+C,CTRL+V to use clipboard.")
            bulletText("CTRL+Z,CTRL+Y to undo/redo.")
            bulletText("ESCAPE to revert.")
            bulletText("You can apply arithmetic operators +,*,/ on numerical values.\nUse +- to subtract.")
        }
    }

    /** get the compiled version string e.g. "1.23" (essentially the compiled value for IMGUI_VERSION) */
    val version: String
        get() = IMGUI_VERSION

    companion object {

        enum class RT { OuterRect, OuterRectClipped, InnerVisibleRect, InnerWorkRect, InnerWorkRectClipped, ContentsRegionRect, ContentsFullRect }

        var showWindowsBeginOrder = false
        var showWindowsRects = false
        var showWindowsRectType = RT.InnerWorkRect
        var showDrawcmdClipRects = true

        var showWindow = false

        /** Helper to display a little (?) mark which shows a tooltip when hovered.
         *  In your own code you may want to display an actual icon if you are using a merged icon fonts (see misc/fonts/README.txt)    */
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

        var enabled = true
        var float = 0.5f
        var combo = 0
        var check = true

        var showConfigInfo = false

        /** Note that shortcuts are currently provided for display only (future version will add flags to BeginMenu to process shortcuts) */
        fun showExampleMenuFile() {
            menuItem("(dummy menu)", "", false, false)
            menuItem("New")
            menuItem("Open", "Ctrl+O")
            menu("Open Recent") {
                menuItem("fish_hat.c")
                menuItem("fish_hat.inl")
                menuItem("fish_hat.h")
                menu("More..") {
                    menuItem("Hello")
                    menuItem("Sailor")
                    menu("Recurse..") { showExampleMenuFile() }
                }
            }
            menuItem("Save", "Ctrl+S")
            menuItem("Save As..")
            separator()
            menu("Options") {
                menuItem("Enabled", "", Companion::enabled)
                child("child", Vec2(0, 60), true) {
                    for (i in 0 until 10) text("Scrolling Text %d", i)
                }
                sliderFloat("Value", Companion::float, 0f, 1f)
                inputFloat("Input", Companion::float, 0.1f)
                combo("Combo", Companion::combo, "Yes\u0000No\u0000Maybe\u0000\u0000")
                checkbox("Check", Companion::check)
            }
            menu("Colors") {
                val sz = textLineHeight
                for (col in Col.values()) {
                    val name = col.name
                    val p = Vec2(cursorScreenPos)
                    windowDrawList.addRectFilled(p, Vec2(p.x + sz, p.y + sz), col.u32)
                    dummy(Vec2(sz))
                    sameLine()
                    menuItem(name)
                }

            }
            menu("Disabled", false) { assert(false) { "Disabled" } }
            menuItem("Checked", selected = true)
            menuItem("Quit", "Alt+F4")
        }


        object Funcs {

            fun getRect(window: Window, rectType: RT): Rect = when (rectType) {
                RT.OuterRect -> window.rect()
                RT.OuterRectClipped -> window.outerRectClipped
                RT.InnerVisibleRect -> window.innerVisibleRect
                RT.InnerWorkRect -> window.innerWorkRect
                RT.InnerWorkRectClipped -> window.innerWorkRectClipped
                RT.ContentsRegionRect -> window.contentsRegionRect
                else -> error("invalid")
            }

            fun nodeDrawList(window: Window?, drawList: DrawList, label: String) {

                val nodeOpen = treeNode(drawList, "$label: '${drawList._ownerName}' ${drawList.vtxBuffer.lim} vtx, " +
                        "${drawList.idxBuffer.lim} indices, ${drawList.cmdBuffer.size} cmds")
                if (drawList === windowDrawList) {
                    sameLine()
                    // Can't display stats for active draw list! (we don't have the data double-buffered)
                    textColored(Vec4(1f, 0.4f, 0.4f, 1f), "CURRENTLY APPENDING")
                    if (nodeOpen) treePop()
                    return
                }
                if (!nodeOpen)
                    return

                val fgDrawList = getForegroundDrawList(window)   // Render additional visuals into the top-most draw list
                window?.let {
                    if (isItemHovered())
                        fgDrawList.addRect(window.pos, window.pos + window.size, COL32(255, 255, 0, 255))
                }

                var elemOffset = 0
                for (i in drawList.cmdBuffer.indices) {
                    val cmd = drawList.cmdBuffer[i]
                    if (cmd.userCallback == null && cmd.elemCount == 0) continue
                    if (cmd.userCallback != null) {
                        bulletText("Callback %s, UserData %s", cmd.userCallback!!.toString(), String((cmd.userCallbackData
                                ?: ByteBuffer.wrap(byteArrayOf())).array()))
                        continue
                    }
                    val idxBuffer = drawList.idxBuffer.takeIf { it.hasRemaining() }
                    val mode = if (idxBuffer != null) "indexed" else "non-indexed"
                    val buf = CharArray(300)
                    "Draw %4d triangles, tex 0x%p, clip_rect (%4.0f,%4.0f)-(%4.0f,%4.0f)".format(
                            cmd.elemCount/3, cmd.textureId, cmd.clipRect.x, cmd.clipRect.y, cmd.clipRect.z, cmd.clipRect.w).toCharArray(buf)
                    val cmdNodeOpen = treeNode(cmd.hashCode() - drawList.cmdBuffer.hashCode(), "%s", buf)
                    if (showDrawcmdClipRects && fgDrawList != null && isItemHovered()) {
                        val clipRect = Rect(cmd.clipRect)
                        val vtxsRect = Rect()
                        for (e in elemOffset until elemOffset + cmd.elemCount)
                            vtxsRect.add(Vec2(drawList.vtxBuffer.data, idxBuffer?.get(e) ?: e))
                        clipRect.floor(); fgDrawList.addRect(clipRect.min, clipRect.max, COL32(255, 255, 0, 255))
                        vtxsRect.floor(); fgDrawList.addRect(vtxsRect.min, vtxsRect.max, COL32(255, 0, 255, 255))
                    }
                    if (!cmdNodeOpen) continue
                    // Display individual triangles/vertices. Hover on to get the corresponding triangle highlighted.
                    text("ElemCount: ${cmd.elemCount}, ElemCount/3: ${cmd.elemCount / 3}, VtxOffset: +${cmd.vtxOffset}, IdxOffset: +${cmd.idxOffset}")
                    // Manually coarse clip our print out of individual vertices to save CPU, only items that may be visible.
                    val clipper = ListClipper(cmd.elemCount / 3)
                    while (clipper.step()) {
                        var idxI = elemOffset + clipper.display.start * 3
                        for (prim in clipper.display.start until clipper.display.last) {
                            var bufP = 0
                            val trianglesPos = arrayListOf(Vec2(), Vec2(), Vec2())
                            for (n in 0 until 3) {
                                val vtxI = idxBuffer?.get(idxI) ?: idxI
                                val pos = Vec2(drawList.vtxBuffer.data, vtxI * DrawVert.size)
                                val uv = Vec2(drawList.vtxBuffer.data, vtxI * DrawVert.size + Vec2.size)
                                val col = drawList.vtxBuffer.data.getInt(vtxI * DrawVert.size + Vec2.size * 2)
                                trianglesPos[n] = pos
                                val name = if (n == 0) "elem" else "    "
                                val string = "$name %04d: pos (%8.2f,%8.2f), uv (%.6f,%.6f), col %08X\n".format(style.locale,
                                        idxI, pos.x, pos.y, uv.x, uv.y, col)
                                string.toCharArray(buf, bufP)
                                bufP += string.length
                                idxI++
                            }
                            selectable(buf.joinToString("", limit = bufP, truncated = ""), false)
                            if (fgDrawList != null && isItemHovered()) {
                                val backupFlags = fgDrawList.flags
                                // Disable AA on triangle outlines at is more readable for very large and thin triangles.
                                fgDrawList.flags = fgDrawList.flags and DrawListFlag.AntiAliasedLines.i.inv()
                                fgDrawList.addPolyline(trianglesPos, COL32(255, 255, 0, 255), true, 1f)
                                fgDrawList.flags = backupFlags
                            }

                        }
                    }
                    treePop()
                    elemOffset += cmd.elemCount
                }
                treePop()
            }

            fun nodeColumns(columns: Columns) {
                if (!treeNode(columns.id, "Columns Id: 0x%08X, Count: ${columns.count}, Flags: 0x%04X", columns.id, columns.flags))
                    return
                bulletText("Width: %.1f (MinX: %.1f, MaxX: %.1f)", columns.offMaxX - columns.offMinX, columns.offMinX, columns.offMaxX)
                columns.columns.forEachIndexed { i, c ->
                    bulletText("Column %02d: OffsetNorm %.3f (= %.1f px)", i, c.offsetNorm, offsetNormToPixels(columns, c.offsetNorm))
                }
                treePop()
            }

            fun nodeWindows(windows: ArrayList<Window>, label: String) {
                if (!treeNode(label, "$label (${windows.size})")) return
                for (i in 0 until windows.size)
                    nodeWindow(windows[i], "Window")
                treePop()
            }

            fun nodeWindow(window: Window, label: String) {
                if (treeNode(window, "$label '${window.name}', ${window.active || window.wasActive} @ 0x%p", window.hashCode()))
                    return
                val flags = window.flags
                nodeDrawList(window, window.drawList, "DrawList")
                bulletText("Pos: (%.1f,%.1f), Size: (%.1f,%.1f), SizeContents (%.1f,%.1f)", window.pos.x.f, window.pos.y.f,
                        window.size.x, window.size.y, window.sizeContents.x, window.sizeContents.y)
                val builder = StringBuilder()
                if (flags has Wf.ChildWindow) builder += "Child "
                if (flags has Wf.Tooltip) builder += "Tooltip "
                if (flags has Wf.Popup) builder += "Popup "
                if (flags has Wf.Modal) builder += "Modal "
                if (flags has Wf.ChildMenu) builder += "ChildMenu "
                if (flags has Wf.NoSavedSettings) builder += "NoSavedSettings "
                if (flags has Wf.NoMouseInputs) builder += "NoMouseInputs"
                if (flags has Wf.NoNavInputs) builder += "NoNavInputs"
                if (flags has Wf.AlwaysAutoResize) builder += "AlwaysAutoResize"
                bulletText("Flags: 0x%08X ($builder..)", flags)
                bulletText("Scroll: (%.2f/%.2f,%.2f/%.2f)", window.scroll.x, window.scrollMaxX, window.scroll.y, window.scrollMaxY)
                val order = if (window.active || window.wasActive) window.beginOrderWithinContext else -1
                bulletText("Active: ${window.active}/${window.wasActive}, WriteAccessed: ${window.writeAccessed} BeginOrderWithinContext: $order")
                bulletText("Appearing: ${window.appearing}, Hidden: ${window.hidden} (CanSkip ${window.hiddenFramesCanSkipItems} Cannot ${window.hiddenFramesCannotSkipItems}), SkipItems: ${window.skipItems}")
                bulletText("NavLastIds: 0x%08X,0x%08X, NavLayerActiveMask: %X", window.navLastIds[0], window.navLastIds[1], window.dc.navLayerActiveMask)
                bulletText("NavLastChildNavWindow: ${window.navLastChildNavWindow?.name}")
                if (!window.navRectRel[0].isInverted)
                    bulletText("NavRectRel[0]: (%.1f,%.1f)(%.1f,%.1f)", window.navRectRel[0].min.x, window.navRectRel[0].min.y, window.navRectRel[0].max.x, window.navRectRel[0].max.y)
                else
                    bulletText("NavRectRel[0]: <None>")
                if (window.rootWindow !== window) nodeWindow(window.rootWindow!!, "RootWindow")
                window.parentWindow?.let { nodeWindow(it, "ParentWindow") }
                if (window.dc.childWindows.isNotEmpty()) nodeWindows(window.dc.childWindows, "ChildWindows")
                if (window.columnsStorage.isNotEmpty() && treeNode("Columns", "Columns sets (${window.columnsStorage.size})")) {
                    window.columnsStorage.forEach(::nodeColumns)
                    treePop()
                }
                bulletText("Storage: %d bytes", window.stateStorage.data.size * Int.BYTES * 2)
                treePop()
            }

            fun nodeTabBar(tabBar: TabBar) {
                // Standalone tab bars (not associated to docking/windows functionality) currently hold no discernible strings.
                val string = "TabBar (${tabBar.tabs.size} tabs)${if (tabBar.prevFrameVisible < frameCount - 2) " *Inactive*" else ""}"
                if (treeNode(tabBar, string)) {
                    for (tabN in tabBar.tabs.indices) {
                        val tab = tabBar.tabs[tabN]
                        pushId(tab)
                        if (smallButton("<"))
                            tabBar.queueChangeTabOrder(tab, -1)
                        sameLine(0, 2)
                        if (smallButton(">")) {
                            tabBar.queueChangeTabOrder(tab, +1)
                            sameLine()
                            text("%02d${if (tab.id == tabBar.selectedTabId) '*' else ' '} Tab 0x%08X", tabN, tab.id)
                            popId()
                        }
                        treePop()
                    }
                }
            }
        }

        val selected = BooleanArray(4 + 3 + 16 + 16) { it == 1 || it == 23 + 0 || it == 23 + 5 || it == 23 + 10 || it == 23 + 15 }

        var styleIdx = -1
    }
}