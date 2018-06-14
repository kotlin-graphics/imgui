package imgui.imgui

import gli_.has
import glm_.f
import glm_.func.cos
import glm_.func.sin
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.F32_TO_INT8_UNBOUND
import imgui.ImGui.acceptDragDropPayload
import imgui.ImGui.beginDragDropSource
import imgui.ImGui.beginDragDropTarget
import imgui.ImGui.beginGroup
import imgui.ImGui.beginPopup
import imgui.ImGui.buttonBehavior
import imgui.ImGui.calcItemWidth
import imgui.ImGui.calcTextSize
import imgui.ImGui.colorConvertHSVtoRGB
import imgui.ImGui.colorConvertRGBtoHSV
import imgui.ImGui.colorEditOptionsPopup
import imgui.ImGui.colorTooltip
import imgui.ImGui.currentWindow
import imgui.ImGui.cursorScreenPos
import imgui.ImGui.dragInt
import imgui.ImGui.dragScalar
import imgui.ImGui.endDragDropSource
import imgui.ImGui.endDragDropTarget
import imgui.ImGui.endGroup
import imgui.ImGui.endPopup
import imgui.ImGui.findRenderedTextEnd
import imgui.ImGui.fontTexUvWhitePixel
import imgui.ImGui.frameHeight
import imgui.ImGui.getColorU32
import imgui.ImGui.hsvToRGB
import imgui.ImGui.inputText
import imgui.ImGui.invisibleButton
import imgui.ImGui.io
import imgui.ImGui.isItemActive
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.openPopup
import imgui.ImGui.openPopupOnItemClick
import imgui.ImGui.popId
import imgui.ImGui.popItemFlag
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushId
import imgui.ImGui.pushItemFlag
import imgui.ImGui.pushItemWidth
import imgui.ImGui.renderColorRectWithAlphaCheckerboard
import imgui.ImGui.renderFrameBorder
import imgui.ImGui.renderNavHighlight
import imgui.ImGui.rgbToHSV
import imgui.ImGui.sameLine
import imgui.ImGui.separator
import imgui.ImGui.setDragDropPayload
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.shadeVertsLinearColorGradientKeepAlpha
import imgui.ImGui.style
import imgui.ImGui.text
import imgui.ImGui.textUnformatted
import imgui.ImGui.u32
import imgui.imgui.imgui_widgetsText.Companion.colorPickerOptionsPopup
import imgui.imgui.imgui_widgetsText.Companion.renderArrowsForVerticalBar
import imgui.internal.*
import imgui.ColorEditFlag as Cef
import imgui.InputTextFlag as Itf
import imgui.WindowFlag as Wf
import imgui.internal.DrawCornerFlag as Dcf

/** Widgets: Color Editor/Picker (tip: the ColorEdit* functions have a little colored preview square that can be
 *  left-clicked to open a picker, and right-clicked to open an option menu.)
 *  Note that a 'float v[X]' function argument is the same as 'float* v', the array syntax is just a way to document
 *  the number of elements that are expected to be accessible. You can the pass the address of a first float element
 *  out of a contiguous structure, e.g. &myvector.x   */
interface imgui_widgetsColorEditorPicker {

    /** 3-4 components color edition. Click on colored squared to open a color picker, right-click for options.
     *  Hint: 'float col[3]' function argument is same as 'float* col'.
     *  You can pass address of first element out of a contiguous set, e.g. &myvector.x */
    fun colorEdit3(label: String, col: Vec4, flags: ColorEditFlags = 0): Boolean {
        val floats = col to FloatArray(4)
        return colorEdit4(label, floats, flags or Cef.NoAlpha).also {
            col put floats
        }
    }

    fun colorEdit3(label: String, col: FloatArray, flags: ColorEditFlags = 0) = colorEdit4(label, col, flags or Cef.NoAlpha)

    /** Edit colors components (each component in 0.0f..1.0f range).
     *  See enum ImGuiColorEditFlags_ for available options. e.g. Only access 3 floats if ColorEditFlags.NoAlpha flag is set.
     *  With typical options: Left-click on colored square to open color picker. Right-click to open option menu.
     *  CTRL-Click over input fields to edit them and TAB to go to next item.   */
    fun colorEdit4(label: String, col: Vec4, flags: ColorEditFlags = 0): Boolean {
        val floats = col to FloatArray(4)
        return colorEdit4(label, floats, flags).also {
            col put floats
        }
    }

    fun colorEdit4(label: String, col: FloatArray, flags: ColorEditFlags = 0): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val squareSz = frameHeight
        val wExtra = if (flags has Cef.NoSmallPreview) 0f else squareSz + style.itemInnerSpacing.x
        val wItemsAll = calcItemWidth() - wExtra
        val labelDisplayEnd = findRenderedTextEnd(label)

        val alpha = flags hasnt Cef.NoAlpha
        val hdr = flags has Cef.HDR
        val components = if (alpha) 4 else 3
        val flagsUntouched = flags

        beginGroup()
        pushId(label)

        var flags = flags

        // If we're not showing any slider there's no point in doing any HSV conversions
        if (flags has Cef.NoInputs) flags = (flags wo Cef._InputsMask) or Cef.RGB or Cef.NoOptions

        // Context menu: display and modify options (before defaults are applied)
        if (flags hasnt Cef.NoOptions) colorEditOptionsPopup(col, flags)

        // Read stored options
        if (flags hasnt Cef._InputsMask) flags = flags or (g.colorEditOptions and Cef._InputsMask)
        if (flags hasnt Cef._DataTypeMask) flags = flags or (g.colorEditOptions and Cef._DataTypeMask)
        if (flags hasnt Cef._PickerMask) flags = flags or (g.colorEditOptions and Cef._PickerMask)
        flags = flags or (g.colorEditOptions wo (Cef._InputsMask or Cef._DataTypeMask or Cef._PickerMask))

        // Convert to the formats we need
        val f = floatArrayOf(col[0], col[1], col[2], if (alpha) col[3] else 1f)
        if (flags has Cef.HSV) f.rgbToHSV()

        val i = IntArray(4, { F32_TO_INT8_UNBOUND(f[it]) })

        var valueChanged = false
        var valueChangedAsFloat = false

        if (flags has (Cef.RGB or Cef.HSV) && flags hasnt Cef.NoInputs) {

            // RGB/HSV 0..255 Sliders
            val wItemOne = glm.max(1f, ((wItemsAll - style.itemInnerSpacing.x * (components - 1)) / components).i.f)
            val wItemLast = glm.max(1f, (wItemsAll - (wItemOne + style.itemInnerSpacing.x) * (components - 1)).i.f)

            val hidePrefix = wItemOne <= calcTextSize(if (flags has Cef.Float) "M:0.000" else "M:000").x
            val ids = arrayOf("##X", "##Y", "##Z", "##W")
            val fmtTableInt = arrayOf(
                    arrayOf("%3d", "%3d", "%3d", "%3d"),             // Short display
                    arrayOf("R:%3d", "G:%3d", "B:%3d", "A:%3d"),     // Long display for RGBA
                    arrayOf("H:%3d", "S:%3d", "V:%3d", "A:%3d"))     // Long display for HSVA
            val fmtTableFloat = arrayOf(
                    arrayOf("%.3f", "%.3f", "%.3f", "%.3f"),            // Short display
                    arrayOf("R:%.3f", "G:%.3f", "B:%.3f", "A:%.3f"),    // Long display for RGBA
                    arrayOf("H:%.3f", "S:%.3f", "V:%.3f", "A:%.3f"))    // Long display for HSVA
            val fmtIdx = if (hidePrefix) 0 else if (flags has Cef.HSV) 2 else 1

            pushItemWidth(wItemOne)
            for (n in 0 until components) {
                if (n > 0) sameLine(0f, style.itemInnerSpacing.x)
                if (n + 1 == components) pushItemWidth(wItemLast)
                valueChanged = when {
                    flags has Cef.Float -> {
                        // operands inverted to have dragScalar always executed, no matter valueChanged
                        valueChangedAsFloat = dragScalar(ids[n], f, n, 1f / 255f, 0f, if (hdr) 0f else 1f, fmtTableFloat[fmtIdx][n]) || valueChanged
                        valueChangedAsFloat
                    }
                    else -> dragInt(ids[n], i, n, 1f, 0, if (hdr) 0 else 255, fmtTableInt[fmtIdx][n]) || valueChanged
                }
                if (flags hasnt Cef.NoOptions) openPopupOnItemClick("context")
            }
            popItemWidth()
            popItemWidth()

        } else if (flags has Cef.HEX && flags hasnt Cef.NoInputs) {
            // RGB Hexadecimal Input
            val text = if (alpha) "#%02X%02X%02X%02X".format(style.locale, glm.clamp(i[0], 0, 255), glm.clamp(i[1], 0, 255), glm.clamp(i[2], 0, 255), glm.clamp(i[3], 0, 255))
            else "#%02X%02X%02X".format(style.locale, glm.clamp(i[0], 0, 255), glm.clamp(i[1], 0, 255), glm.clamp(i[2], 0, 255))
            val buf = text.toCharArray(CharArray(64))
            pushItemWidth(wItemsAll)
            if (inputText("##Text", buf, Itf.CharsHexadecimal or Itf.CharsUppercase)) {
                valueChanged = true
                var p = 0
                while (buf[p] == '#' || buf[p].isBlankA) p++
                i.fill(0)
                String(buf, p, buf.strlen - p).scanHex(i, if (alpha) 4 else 3, 2)   // Treat at unsigned (%X is unsigned)
            }
            if (flags hasnt Cef.NoOptions) openPopupOnItemClick("context")
            popItemWidth()
        }

        var pickerActiveWindow: Window? = null
        if (flags hasnt Cef.NoSmallPreview) {
            if (flags hasnt Cef.NoInputs) sameLine(0f, style.itemInnerSpacing.x)
            val colVec4 = Vec4(col[0], col[1], col[2], if (alpha) col[3] else 1f)
            if (colorButton("##ColorButton", colVec4, flags))
                if (flags hasnt Cef.NoPicker) { // Store current color and open a picker
                    g.colorPickerRef put colVec4
                    openPopup("picker")
                    setNextWindowPos(window.dc.lastItemRect.bl + Vec2(-1, style.itemSpacing.y))
                }
            if (flags hasnt Cef.NoOptions) openPopupOnItemClick("context")

            if (beginPopup("picker")) {
                pickerActiveWindow = g.currentWindow
                if (0 != labelDisplayEnd) {
                    textUnformatted(label, labelDisplayEnd)
                    separator()
                }
                val pickerFlagsToForward = Cef._DataTypeMask or Cef._PickerMask or Cef.HDR or Cef.NoAlpha or Cef.AlphaBar
                val pickerFlags = (flagsUntouched and pickerFlagsToForward) or Cef._InputsMask or Cef.NoLabel or Cef.AlphaPreviewHalf
                pushItemWidth(squareSz * 12f)   // Use 256 + bar sizes?
                val p = g.colorPickerRef to FloatArray(4)
                valueChanged = colorPicker4("##picker", col, pickerFlags, p) or valueChanged
                g.colorPickerRef put p
                popItemWidth()
                endPopup()
            }
        }

        if (0 != labelDisplayEnd && flags hasnt Cef.NoLabel) { // TODO check first comparison
            sameLine(0f, style.itemInnerSpacing.x)
            textUnformatted(label, labelDisplayEnd)
        }

        // Convert back
        if (pickerActiveWindow == null) {
            if (!valueChangedAsFloat) for (n in 0..3) f[n] = i[n] / 255f
            if (flags has Cef.HSV) f.hsvToRGB()
            if (valueChanged) {
                col[0] = f[0]
                col[1] = f[1]
                col[2] = f[2]
                if (alpha) col[3] = f[3]
            }
        }
        popId()
        endGroup()

        // Drag and Drop Target

        // NB: The flag test is merely an optional micro-optimization, BeginDragDropTarget() does the same test.
        if (window.dc.lastItemStatusFlags has ItemStatusFlag.HoveredRect && beginDragDropTarget()) {
            acceptDragDropPayload(PAYLOAD_TYPE_COLOR_3F)?.let {
                for (j in 0..2) col[j] = (it.data as Vec4)[j]
                valueChanged = true
            }
            acceptDragDropPayload(PAYLOAD_TYPE_COLOR_4F)?.let {
                for (j in 0..components) col[j] = (it.data as Vec4)[j]
                valueChanged = true
            }
            endDragDropTarget()
        }

        // When picker is being actively used, use its active id so IsItemActive() will function on ColorEdit4().
        if (pickerActiveWindow != null && g.activeId != 0 && g.activeIdWindow === pickerActiveWindow)
            window.dc.lastItemId = g.activeId

        return valueChanged
    }

    fun colorEditVec4(label: String, col: Vec4, flags: ColorEditFlags = 0): Boolean {
        val col4 = floatArrayOf(col.x, col.y, col.z, col.w)
        val valueChanged = colorEdit4(label, col4, flags)
        col.x = col4[0]
        col.y = col4[1]
        col.z = col4[2]
        col.w = col4[3]
        return valueChanged
    }

    fun colorPicker3(label: String, col: FloatArray, flags: ColorEditFlags = 0): Boolean {
        val col4 = floatArrayOf(*col, 1f)
        if (!colorPicker4(label, col4, flags or Cef.NoAlpha)) return false
        col[0] = col4[0]; col[1] = col4[1]; col[2] = col4[2]
        return true
    }

    /** ColorPicker
     *  Note: only access 3 floats if ImGuiColorEditFlags_NoAlpha flag is set.
     *  FIXME: we adjust the big color square height based on item width, which may cause a flickering feedback loop
     *  (if automatic height makes a vertical scrollbar appears, affecting automatic width..)   */
    fun colorPicker4(label: String, col: Vec4, flags: ColorEditFlags = 0, refCol: Vec4? = null): Boolean {
        val floats = col to FloatArray(4)
        val res = if (refCol == null) colorPicker4(label, floats, flags, refCol)
        else {
            val floats2 = refCol to FloatArray(4)
            val res = colorPicker4(label, floats, flags, floats2)
            refCol put floats2
            res
        }
        col put floats
        return res
    }

    fun colorPicker4(label: String, col: FloatArray, flags: ColorEditFlags = 0, refCol: FloatArray? = null): Boolean {

        val window = currentWindow
        val drawList = window.drawList

        pushId(label)
        beginGroup()

        var flags = flags
        if (flags hasnt Cef.NoSidePreview)
            flags = flags or Cef.NoSmallPreview

        // Context menu: display and store options.
        if (flags hasnt Cef.NoOptions)
            colorPickerOptionsPopup(flags, col)

        // Read stored options
        if (flags hasnt Cef._PickerMask)
            flags = flags or ((if (g.colorEditOptions has Cef._PickerMask) g.colorEditOptions else Cef._OptionsDefault.i) and Cef._PickerMask)
        assert((flags and Cef._PickerMask).isPowerOfTwo) { "Check that only 1 is selected" }
        if (flags hasnt Cef.NoOptions)
            flags = flags or (g.colorEditOptions and Cef.AlphaBar)

        // Setup
        val components = if (flags has Cef.NoAlpha) 3 else 4
        val alphaBar = flags has Cef.AlphaBar && flags hasnt Cef.NoAlpha
        val pickerPos = Vec2(window.dc.cursorPos)
        val squareSz = frameHeight
        val barsWidth = squareSz     // Arbitrary smallish width of Hue/Alpha picking bars
        // Saturation/Value picking box
        val svPickerSize = glm.max(barsWidth * 1, calcItemWidth() - (if (alphaBar) 2 else 1) * (barsWidth + style.itemInnerSpacing.x))
        val bar0PosX = pickerPos.x + svPickerSize + style.itemInnerSpacing.x
        val bar1PosX = bar0PosX + barsWidth + style.itemInnerSpacing.x
        val barsTrianglesHalfSz = (barsWidth * 0.2f).i.f

        val backupInitialCol = FloatArray(4, { col[it] })

        val wheelThickness = svPickerSize * 0.08f
        val wheelROuter = svPickerSize * 0.50f
        val wheelRInner = wheelROuter - wheelThickness
        val wheelCenter = Vec2(pickerPos.x + (svPickerSize + barsWidth) * 0.5f, pickerPos.y + svPickerSize * 0.5f)

        // Note: the triangle is displayed rotated with trianglePa pointing to Hue, but most coordinates stays unrotated for logic.
        val triangleR = wheelRInner - (svPickerSize * 0.027f).i
        val trianglePa = Vec2(triangleR, 0f)   // Hue point.
        val trianglePb = Vec2(triangleR * -0.5f, triangleR * -0.866025f) // Black point.
        val trianglePc = Vec2(triangleR * -0.5f, triangleR * +0.866025f) // White point.

        var (h, s, v) = colorConvertRGBtoHSV(col)

        var valueChanged = false
        var valueChangedH = false
        var valueChangedSv = false

        pushItemFlag(ItemFlag.NoNav.i, true)
        if (flags has Cef.PickerHueWheel) {
            // Hue wheel + SV triangle logic
            invisibleButton("hsv", Vec2(svPickerSize + style.itemInnerSpacing.x + barsWidth, svPickerSize))
            if (isItemActive) {
                val initialOff = io.mouseClickedPos[0] - wheelCenter
                val currentOff = io.mousePos - wheelCenter
                val initialDist2 = initialOff.lengthSqr
                if (initialDist2 >= (wheelRInner - 1) * (wheelRInner - 1) && initialDist2 <= (wheelROuter + 1) * (wheelROuter + 1)) {
                    // Interactive with Hue wheel
                    h = glm.atan(currentOff.y, currentOff.x) / glm.PIf * 0.5f
                    if (h < 0f)
                        h += 1f
                    valueChanged = true
                    valueChangedH = true
                }
                val cosHueAngle = glm.cos(-h * 2f * glm.PIf)
                val sinHueAngle = glm.sin(-h * 2f * glm.PIf)
                if (triangleContainsPoint(trianglePa, trianglePb, trianglePc, initialOff.rotateAssign(cosHueAngle, sinHueAngle))) { // TODO check
                    // Interacting with SV triangle
                    val currentOffUnrotated = currentOff.rotateAssign(cosHueAngle, sinHueAngle)
                    if (!triangleContainsPoint(trianglePa, trianglePb, trianglePc, currentOffUnrotated))
                        currentOffUnrotated put triangleClosestPoint(trianglePa, trianglePb, trianglePc, currentOffUnrotated)
                    val (uu, vv, ww) = triangleBarycentricCoords(trianglePa, trianglePb, trianglePc, currentOffUnrotated)
                    v = glm.clamp(1f - vv, 0.0001f, 1f)
                    s = glm.clamp(uu / v, 0.0001f, 1f)
                    valueChangedSv = true
                    valueChanged = true
                }
            }
            if (flags hasnt Cef.NoOptions) openPopupOnItemClick("context")

        } else if (flags has Cef.PickerHueBar) {
            // SV rectangle logic
            invisibleButton("sv", Vec2(svPickerSize))
            if (isItemActive) {
                s = saturate((io.mousePos.x - pickerPos.x) / (svPickerSize - 1))
                v = 1f - saturate((io.mousePos.y - pickerPos.y) / (svPickerSize - 1))
                valueChangedSv = true
                valueChanged = true
            }
            if (flags hasnt Cef.NoOptions) openPopupOnItemClick("context")
            // Hue bar logic
            cursorScreenPos = Vec2(bar0PosX, pickerPos.y)
            invisibleButton("hue", Vec2(barsWidth, svPickerSize))
            if (isItemActive) {
                h = saturate((io.mousePos.y - pickerPos.y) / (svPickerSize - 1))
                valueChangedH = true
                valueChanged = true
            }
        }

        // Alpha bar logic
        if (alphaBar) {
            cursorScreenPos = Vec2(bar1PosX, pickerPos.y)
            invisibleButton("alpha", Vec2(barsWidth, svPickerSize))
            if (isItemActive) {
                col[3] = 1f - saturate((io.mousePos.y - pickerPos.y) / (svPickerSize - 1))
                valueChanged = true
            }
        }
        popItemFlag() // ItemFlag.NoNav

        if (flags hasnt Cef.NoSidePreview) {
            sameLine(0f, style.itemInnerSpacing.x)
            beginGroup()
        }

        if (flags hasnt Cef.NoLabel) {
            val labelDisplayEnd = findRenderedTextEnd(label)
            if (0 != labelDisplayEnd) {
                if (flags has Cef.NoSidePreview)
                    sameLine(0f, style.itemInnerSpacing.x)
                textUnformatted(label, labelDisplayEnd)
            }
        }
        if (flags hasnt Cef.NoSidePreview) {
            pushItemFlag(ItemFlag.NoNavDefaultFocus.i, true)
            val colV4 = Vec4(col[0], col[1], col[2], if (flags has Cef.NoAlpha) 1f else col[3])
            if (flags has Cef.NoLabel)
                text("Current")
            val f = flags and (Cef.HDR or Cef.AlphaPreview or Cef.AlphaPreviewHalf or Cef.NoTooltip)
            colorButton("##current", colV4, f, Vec2(squareSz * 3, squareSz * 2))
            refCol?.let {
                text("Original")
                val refColV4 = Vec4(it[0], it[1], it[2], if (flags has Cef.NoAlpha) 1f else it[3])
                if (colorButton("##original", refColV4, f, Vec2(squareSz * 3, squareSz * 2))) {
                    for (i in 0 until components) col[i] = it[i]
                    valueChanged = true
                }
            }
            popItemFlag()
            endGroup()
        }

        // Convert back color to RGB
        if (valueChangedH || valueChangedSv)
            colorConvertHSVtoRGB(if (h >= 1f) h - 10 * 1e-6f else h, if (s > 0f) s else 10 * 1e-6f, if (v > 0f) v else 1e-6f, col)

        // R,G,B and H,S,V slider color editor
        if (flags hasnt Cef.NoInputs) {
            pushItemWidth((if (alphaBar) bar1PosX else bar0PosX) + barsWidth - pickerPos.x)
            val subFlagsToForward = Cef._DataTypeMask or Cef.HDR or Cef.NoAlpha or Cef.NoOptions or Cef.NoSmallPreview or
                    Cef.AlphaPreview or Cef.AlphaPreviewHalf
            val subFlags = (flags and subFlagsToForward) or Cef.NoPicker
            valueChanged = when {
                flags has Cef.RGB || flags hasnt Cef._InputsMask -> colorEdit4("##rgb", col, subFlags or Cef.RGB)
                flags has Cef.HSV || flags hasnt Cef._InputsMask -> colorEdit4("##hsv", col, subFlags or Cef.HSV)
                flags has Cef.HEX || flags hasnt Cef._InputsMask -> colorEdit4("##hex", col, subFlags or Cef.HEX)
                else -> false
            } or valueChanged
            popItemWidth()
        }

        // Try to cancel hue wrap (after ColorEdit), if any
        if (valueChanged) {
            val (newH, newS, newV) = colorConvertRGBtoHSV(col)
            if (newH <= 0 && h > 0) {
                if (newV <= 0 && v != newV)
                    colorConvertHSVtoRGB(h, s, if (newV <= 0) v * 0.5f else newV, col)
                else if (newS <= 0)
                    colorConvertHSVtoRGB(h, if (newS <= 0) s * 0.5f else newS, newV, col)
            }
        }

        val hueColorF = Vec4(1)
        colorConvertHSVtoRGB(h, 1f, 1f).apply { hueColorF.x = this[0]; hueColorF.y = this[1]; hueColorF.z = this[2] }
        val hueColor32 = hueColorF.u32
        val col32NoAlpha = Vec4(col[0], col[1], col[2], 1f).u32

        val hueColors = arrayOf(COL32(255, 0, 0, 255), COL32(255, 255, 0, 255), COL32(0, 255, 0, 255),
                COL32(0, 255, 255, 255), COL32(0, 0, 255, 255), COL32(255, 0, 255, 255), COL32(255, 0, 0, 255))
        val svCursorPos = Vec2()

        if (flags has Cef.PickerHueWheel) {
            // Render Hue Wheel
            val aeps = 1.5f / wheelROuter   // Half a pixel arc length in radians (2pi cancels out).
            val segmentPerArc = glm.max(4, (wheelROuter / 12).i)
            for (n in 0..5) {
                val a0 = n / 6f * 2f * glm.PIf - aeps
                val a1 = (n + 1f) / 6f * 2f * glm.PIf + aeps
                val vertStartIdx = drawList.vtxBuffer.size
                drawList.pathArcTo(wheelCenter, (wheelRInner + wheelROuter) * 0.5f, a0, a1, segmentPerArc)
                drawList.pathStroke(COL32_WHITE, false, wheelThickness)
                val vertEndIdx = drawList.vtxBuffer.size

                // Paint colors over existing vertices
                val gradientP0 = Vec2(wheelCenter.x + a0.cos * wheelRInner, wheelCenter.y + a0.sin * wheelRInner)
                val gradientP1 = Vec2(wheelCenter.x + a1.cos * wheelRInner, wheelCenter.y + a1.sin * wheelRInner)
                shadeVertsLinearColorGradientKeepAlpha(drawList.vtxBuffer, vertStartIdx, vertEndIdx, gradientP0, gradientP1, hueColors[n], hueColors[n + 1])
            }

            // Render Cursor + preview on Hue Wheel
            val cosHueAngle = glm.cos(h * 2f * glm.PIf)
            val sinHueAngle = glm.sin(h * 2f * glm.PIf)
            val hueCursorPos = Vec2(wheelCenter.x + cosHueAngle * (wheelRInner + wheelROuter) * 0.5f,
                    wheelCenter.y + sinHueAngle * (wheelRInner + wheelROuter) * 0.5f)
            val hueCursorRad = wheelThickness * if (valueChangedH) 0.65f else 0.55f
            val hueCursorSegments = glm.clamp((hueCursorRad / 1.4f).i, 9, 32)
            drawList.addCircleFilled(hueCursorPos, hueCursorRad, hueColor32, hueCursorSegments)
            drawList.addCircle(hueCursorPos, hueCursorRad + 1, COL32(128, 128, 128, 255), hueCursorSegments)
            drawList.addCircle(hueCursorPos, hueCursorRad, COL32_WHITE, hueCursorSegments)

            // Render SV triangle (rotated according to hue)
            val tra = wheelCenter + trianglePa.rotate(cosHueAngle, sinHueAngle)
            val trb = wheelCenter + trianglePb.rotate(cosHueAngle, sinHueAngle)
            val trc = wheelCenter + trianglePc.rotate(cosHueAngle, sinHueAngle)
            val uvWhite = fontTexUvWhitePixel
            drawList.primReserve(6, 6)
            drawList.primVtx(tra, uvWhite, hueColor32)
            drawList.primVtx(trb, uvWhite, hueColor32)
            drawList.primVtx(trc, uvWhite, COL32_WHITE)
            drawList.primVtx(tra, uvWhite, COL32_BLACK_TRANS)
            drawList.primVtx(trb, uvWhite, COL32_BLACK)
            drawList.primVtx(trc, uvWhite, COL32_BLACK_TRANS)
            drawList.addTriangle(tra, trb, trc, COL32(128, 128, 128, 255), 1.5f)
            svCursorPos put trc.lerp(tra, saturate(s)).lerp(trb, saturate(1 - v))
        } else if (flags has Cef.PickerHueBar) {
            // Render SV Square
            drawList.addRectFilledMultiColor(pickerPos, pickerPos + svPickerSize, COL32_WHITE, hueColor32, hueColor32, COL32_WHITE)
            drawList.addRectFilledMultiColor(pickerPos, pickerPos + svPickerSize, COL32_BLACK_TRANS, COL32_BLACK_TRANS, COL32_BLACK, COL32_BLACK)
            renderFrameBorder(pickerPos, pickerPos + svPickerSize, 0f)
            // Sneakily prevent the circle to stick out too much
            svCursorPos.x = glm.clamp((pickerPos.x + saturate(s) * svPickerSize + 0.5f).i.f, pickerPos.x + 2, pickerPos.x + svPickerSize - 2)
            svCursorPos.y = glm.clamp((pickerPos.y + saturate(1 - v) * svPickerSize + 0.5f).i.f, pickerPos.y + 2, pickerPos.y + svPickerSize - 2)

            // Render Hue Bar
            for (i in 0..5) {
                val a = Vec2(bar0PosX, pickerPos.y + i * (svPickerSize / 6))
                val c = Vec2(bar0PosX + barsWidth, pickerPos.y + (i + 1) * (svPickerSize / 6))
                drawList.addRectFilledMultiColor(a, c, hueColors[i], hueColors[i], hueColors[i + 1], hueColors[i + 1])
            }
            val bar0LineY = (pickerPos.y + h * svPickerSize + 0.5f).i.f
            renderFrameBorder(Vec2(bar0PosX, pickerPos.y), Vec2(bar0PosX + barsWidth, pickerPos.y + svPickerSize), 0f)
            renderArrowsForVerticalBar(drawList, Vec2(bar0PosX - 1, bar0LineY), Vec2(barsTrianglesHalfSz + 1, barsTrianglesHalfSz), barsWidth + 2f)
        }

        // Render cursor/preview circle (clamp S/V within 0..1 range because floating points colors may lead HSV values to be out of range)
        val svCursorRad = if (valueChangedSv) 10f else 6f
        drawList.addCircleFilled(svCursorPos, svCursorRad, col32NoAlpha, 12)
        drawList.addCircle(svCursorPos, svCursorRad + 1, COL32(128, 128, 128, 255), 12)
        drawList.addCircle(svCursorPos, svCursorRad, COL32_WHITE, 12)

        // Render alpha bar
        if (alphaBar) {
            val alpha = saturate(col[3])
            val bar1Bb = Rect(bar1PosX, pickerPos.y, bar1PosX + barsWidth, pickerPos.y + svPickerSize)
            renderColorRectWithAlphaCheckerboard(bar1Bb.min, bar1Bb.max, COL32(0, 0, 0, 0), bar1Bb.width / 2f, Vec2())
            drawList.addRectFilledMultiColor(bar1Bb.min, bar1Bb.max, col32NoAlpha, col32NoAlpha, col32NoAlpha wo COL32_A_MASK, col32NoAlpha wo COL32_A_MASK)
            val bar1LineY = (pickerPos.y + (1f - alpha) * svPickerSize + 0.5f)
            renderFrameBorder(bar1Bb.min, bar1Bb.max, 0f)
            renderArrowsForVerticalBar(drawList, Vec2(bar1PosX - 1, bar1LineY), Vec2(barsTrianglesHalfSz + 1, barsTrianglesHalfSz), barsWidth + 2f)
        }

        endGroup()
        popId()

        var compare = true
        repeat(components) { if (backupInitialCol[it] != col[it]) compare = false }

        return valueChanged && !compare
    }

    /**  A little colored square. Return true when clicked.
     *  FIXME: May want to display/ignore the alpha component in the color display? Yet show it in the tooltip.
     *  'desc_id' is not called 'label' because we don't display it next to the button, but only in the tooltip.    */
    fun colorButton(descId: String, col: Vec4, flags: ColorEditFlags = 0, size: Vec2 = Vec2()): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getId(descId)
        val defaultSize = frameHeight
        if (size.x == 0f)
            size.x = defaultSize
        if (size.y == 0f)
            size.y = defaultSize
        val bb = Rect(window.dc.cursorPos, window.dc.cursorPos + size)
        itemSize(bb, if (size.y >= defaultSize) style.framePadding.y else 0f)
        if (!itemAdd(bb, id)) return false

        var (pressed, hovered, held) = buttonBehavior(bb, id)

        var flags = flags
        if (flags has Cef.NoAlpha)
            flags = flags and (Cef.AlphaPreview or Cef.AlphaPreviewHalf).inv()

        val colWithoutAlpha = Vec4(col.x, col.y, col.z, 1f)
        val gridStep = glm.min(size.x, size.y) / 2.99f
        val rounding = glm.min(style.frameRounding, gridStep * 0.5f)
        val bbInner = Rect(bb)
        /*  The border (using Col.FrameBg) tends to look off when color is near-opaque and rounding is enabled.
            This offset seemed like a good middle ground to reduce those artifacts.  */
        val off = -0.75f
        bbInner expand off
        if (flags has Cef.AlphaPreviewHalf && col.w < 1f) {
            val midX = ((bbInner.min.x + bbInner.max.x) * 0.5f + 0.5f).i.f
            renderColorRectWithAlphaCheckerboard(Vec2(bbInner.min.x + gridStep, bbInner.min.y), bbInner.max, getColorU32(col),
                    gridStep, Vec2(-gridStep + off, off), rounding, Dcf.TopRight or Dcf.BotRight)
            window.drawList.addRectFilled(bbInner.min, Vec2(midX, bbInner.max.y), getColorU32(colWithoutAlpha), rounding,
                    Dcf.TopLeft or Dcf.BotLeft)
        } else {
            /*  Because getColorU32() multiplies by the global style alpha and we don't want to display a checkerboard 
                if the source code had no alpha */
            val colSource = if (flags has Cef.AlphaPreview) col else colWithoutAlpha
            if (colSource.w < 1f)
                renderColorRectWithAlphaCheckerboard(bbInner.min, bbInner.max, colSource.u32, gridStep, Vec2(off), rounding)
            else
                window.drawList.addRectFilled(bbInner.min, bbInner.max, getColorU32(colSource), rounding, Dcf.All.i)
        }
        renderNavHighlight(bb, id)
        if (style.frameBorderSize > 0f)
            renderFrameBorder(bb.min, bb.max, rounding)
        else
            window.drawList.addRect(bb.min, bb.max, Col.FrameBg.u32, rounding)  // Color button are often in need of some sort of border

        // Drag and Drop Source
        if (g.activeId == id && beginDragDropSource()) { // NB: The ActiveId test is merely an optional micro-optimization

            if (flags has Cef.NoAlpha)
                setDragDropPayload(PAYLOAD_TYPE_COLOR_3F, col, Vec3.size, Cond.Once)
            else
                setDragDropPayload(PAYLOAD_TYPE_COLOR_4F, col, Vec4.size, Cond.Once)
            colorButton(descId, col, flags)
            sameLine()
            textUnformatted("Color")
            endDragDropSource()
            hovered = false
        }
        // Tooltip
        if (flags hasnt Cef.NoTooltip && hovered) {
            val pF = floatArrayOf(col.x, col.y, col.z, col.w)
            colorTooltip(descId, pF, flags and (Cef.NoAlpha or Cef.AlphaPreview or Cef.AlphaPreviewHalf))
            col.put(pF)
        }

        return pressed
    }

    /** initialize current options (generally on application startup) if you want to select a default format, picker
     *  type, etc. User will be able to change many settings, unless you pass the _NoOptions flag to your calls.    */
    fun setColorEditOptions(flags: ColorEditFlags) {
        var flags = flags
        if (flags hasnt Cef._InputsMask)
            flags = flags or (Cef._OptionsDefault and Cef._InputsMask)
        if (flags hasnt Cef._DataTypeMask)
            flags = flags or (Cef._OptionsDefault and Cef._DataTypeMask)
        if (flags hasnt Cef._PickerMask)
            flags = flags or (Cef._OptionsDefault and Cef._PickerMask)
        assert((flags and Cef._InputsMask).isPowerOfTwo) { "Check only 1 option is selected" }
        assert((flags and Cef._DataTypeMask).isPowerOfTwo) { "Check only 1 option is selected" }
        assert((flags and Cef._PickerMask).isPowerOfTwo) { "Check only 1 option is selected" }
        g.colorEditOptions = flags
    }
}