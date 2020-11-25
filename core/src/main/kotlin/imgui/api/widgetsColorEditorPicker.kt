package imgui.api

import gli_.has
import glm_.func.cos
import glm_.func.sin
import glm_.glm
import glm_.i
import glm_.max
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import glm_.wo
import imgui.*
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
import imgui.ImGui.colorPickerOptionsPopup
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
import imgui.ImGui.markItemEdited
import imgui.ImGui.openPopup
import imgui.ImGui.openPopupOnItemClick
import imgui.ImGui.popID
import imgui.ImGui.popItemFlag
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushID
import imgui.ImGui.pushItemFlag
import imgui.ImGui.pushItemWidth
import imgui.ImGui.renderColorRectWithAlphaCheckerboard
import imgui.ImGui.renderFrameBorder
import imgui.ImGui.renderNavHighlight
import imgui.ImGui.rgbToHSV
import imgui.ImGui.sameLine
import imgui.ImGui.setDragDropPayload
import imgui.ImGui.setNextItemWidth
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.spacing
import imgui.ImGui.style
import imgui.ImGui.text
import imgui.ImGui.textEx
import imgui.classes.DrawList
import imgui.internal.*
import imgui.internal.classes.Rect
import imgui.internal.classes.Window
import imgui.internal.sections.ItemFlag
import imgui.internal.sections.ItemStatusFlag
import imgui.internal.sections.has
import imgui.ColorEditFlag as Cef
import imgui.InputTextFlag as Itf
import imgui.internal.sections.DrawCornerFlag as Dcf


/** Widgets: Color Editor/Picker (tip: the ColorEdit* functions have a little colored preview square that can be
 *  left-clicked to open a picker, and right-clicked to open an option menu.)
 *  - Note that in C++ a 'float v[X]' function argument is the _same_ as 'float* v', the array syntax is just a way to
 *      document the number of elements that are expected to be accessible.
 *  - You can pass the address of a first float element out of a contiguous structure, e.g. &myvector.x  */
interface widgetsColorEditorPicker {

    /** 3-4 components color edition. Click on colored squared to open a color picker, right-click for options.
     *  Hint: 'float col[3]' function argument is same as 'float* col'.
     *  You can pass address of first element out of a contiguous set, e.g. &myvector.x */
    fun colorEdit3(label: String, col: Vec4, flags: ColorEditFlags = 0): Boolean =
            colorEdit4(label, col to _fa, flags or Cef.NoAlpha)
                    .also { col put _fa }

    fun colorEdit3(label: String, col: FloatArray, flags: ColorEditFlags = 0): Boolean =
            colorEdit4(label, col, flags or Cef.NoAlpha)

    /** Edit colors components (each component in 0.0f..1.0f range).
     *  See enum ImGuiColorEditFlags_ for available options. e.g. Only access 3 floats if ColorEditFlags.NoAlpha flag is set.
     *  With typical options: Left-click on colored square to open color picker. Right-click to open option menu.
     *  CTRL-Click over input fields to edit them and TAB to go to next item.   */
    fun colorEdit4(label: String, col: Vec4, flags: ColorEditFlags = 0): Boolean =
            colorEdit4(label, col to _fa, flags).also { col put _fa }

    fun colorEdit4(label: String, col: FloatArray, flags_: ColorEditFlags = 0): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val squareSz = frameHeight
        val wFull = calcItemWidth()
        val wButton = if (flags_ has Cef.NoSmallPreview) 0f else squareSz + style.itemInnerSpacing.x
        val wInputs = wFull - wButton
        val labelDisplayEnd = findRenderedTextEnd(label)
        g.nextItemData.clearFlags()

        beginGroup()
        pushID(label)

        var flags = flags_

        // If we're not showing any slider there's no point in doing any HSV conversions
        val flagsUntouched = flags
        if (flags has Cef.NoInputs) flags = (flags wo Cef._DisplayMask) or Cef.DisplayRGB or Cef.NoOptions

        // Context menu: display and modify options (before defaults are applied)
        if (flags hasnt Cef.NoOptions) colorEditOptionsPopup(col, flags)

        // Read stored options
        if (flags hasnt Cef._DisplayMask) flags = flags or (g.colorEditOptions and Cef._DisplayMask)
        if (flags hasnt Cef._DataTypeMask) flags = flags or (g.colorEditOptions and Cef._DataTypeMask)
        if (flags hasnt Cef._PickerMask) flags = flags or (g.colorEditOptions and Cef._PickerMask)
        if (flags hasnt Cef._InputMask)
            flags = flags or (g.colorEditOptions and Cef._InputMask)
        flags = flags or (g.colorEditOptions wo (Cef._DisplayMask or Cef._DataTypeMask or Cef._PickerMask or Cef._InputMask))
        assert((flags and Cef._DisplayMask).isPowerOfTwo) { "Check that only 1 is selected" }
        assert((flags and Cef._InputMask).isPowerOfTwo) { "Check that only 1 is selected" }

        val alpha = flags hasnt Cef.NoAlpha
        val hdr = flags has Cef.HDR
        val components = if (alpha) 4 else 3

        // Convert to the formats we need
        val f = floatArrayOf(col[0], col[1], col[2], if (alpha) col[3] else 1f)
        if (flags has Cef.InputHSV && flags has Cef.DisplayRGB)
            f.hsvToRGB()
        else if (flags has Cef.InputRGB && flags has Cef.DisplayHSV) {
            // Hue is lost when converting from greyscale rgb (saturation=0). Restore it.
            f.rgbToHSV()
            if (g.colorEditLastColor[0] == col[0] && g.colorEditLastColor[1] == col[1] && g.colorEditLastColor[2] == col[2]) {
                if (f[1] == 0f)
                    f[0] = g.colorEditLastHue
                if (f[2] == 0f)
                    f[1] = g.colorEditLastSat
            }
        }

        val i = IntArray(4) { F32_TO_INT8_UNBOUND(f[it]) }

        var valueChanged = false
        val valueChangedAsFloat = false

        val pos = Vec2(window.dc.cursorPos)
        val inputsOffsetX = if (style.colorButtonPosition == Dir.Left) wButton else 0f
        window.dc.cursorPos.x = pos.x + inputsOffsetX

        if (flags has (Cef.DisplayRGB or Cef.DisplayHSV) && flags hasnt Cef.NoInputs) {

            // RGB/HSV 0..255 Sliders
            val wItemOne = 1f max floor((wInputs - style.itemInnerSpacing.x * (components - 1)) / components)
            val wItemLast = 1f max floor(wInputs - (wItemOne + style.itemInnerSpacing.x) * (components - 1))

            val hidePrefix = wItemOne <= calcTextSize(if (flags has Cef.Float) "M:0.000" else "M:000").x
            val fmtIdx = if (hidePrefix) 0 else if (flags has Cef.DisplayHSV) 2 else 1

            repeat (components) { n ->
                if (n > 0)
                    sameLine(0f, style.itemInnerSpacing.x)
                setNextItemWidth(if (n + 1 < components) wItemOne else wItemLast)

                // Disable Hue edit when Saturation is zero
                // FIXME: When ImGuiColorEditFlags_HDR flag is passed HS values snap in weird ways when SV values go below 0.
                valueChanged = when {
                    flags has Cef.Float -> // operands inverted to have dragScalar always executed, no matter valueChanged
                        dragScalar(ids[n], f, n, 1f / 255f, 0f, if (hdr) 0f else 1f, fmtTableFloat[fmtIdx][n]) || valueChanged // ~ valueChangedAsFloat
                    else -> dragInt(ids[n], i, n, 1f, 0, if (hdr) 0 else 255, fmtTableInt[fmtIdx][n]) || valueChanged
                }
                if (flags hasnt Cef.NoOptions) openPopupOnItemClick("context")
            }

        } else if (flags has Cef.DisplayHEX && flags hasnt Cef.NoInputs) {
            // RGB Hexadecimal Input
            val buf = when {
                alpha -> "#%02X%02X%02X%02X".format(style.locale, glm.clamp(i[0], 0, 255), glm.clamp(i[1], 0, 255), glm.clamp(i[2], 0, 255), glm.clamp(i[3], 0, 255))
                else -> "#%02X%02X%02X".format(style.locale, glm.clamp(i[0], 0, 255), glm.clamp(i[1], 0, 255), glm.clamp(i[2], 0, 255))
            }
            setNextItemWidth(wInputs)
            if (inputText("##Text", buf.toByteArray(64), Itf.CharsHexadecimal or Itf.CharsUppercase)) {
                valueChanged = true
                var p = 0
                while (buf[p] == '#' || buf[p].isBlankA) p++
                i.fill(0)
                buf.substring(p).scanHex(i, if (alpha) 4 else 3, 2)   // Treat at unsigned (%X is unsigned)
            }
            if (flags hasnt Cef.NoOptions)
                openPopupOnItemClick("context")
        }

        var pickerActiveWindow: Window? = null
        if (flags hasnt Cef.NoSmallPreview) {
            val buttonOffsetX = when {
                flags has Cef.NoInputs || style.colorButtonPosition == Dir.Left -> 0f
                else -> wInputs + style.itemInnerSpacing.x
            }
            window.dc.cursorPos.put(pos.x + buttonOffsetX, pos.y)

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
                    textEx(label, labelDisplayEnd)
                    spacing()
                }
                val pickerFlagsToForward = Cef._DataTypeMask or Cef._PickerMask or Cef._InputMask or Cef.HDR or Cef.NoAlpha or Cef.AlphaBar
                val pickerFlags = (flagsUntouched and pickerFlagsToForward) or Cef._DisplayMask or Cef._DisplayMask or Cef.NoLabel or Cef.AlphaPreviewHalf
                setNextItemWidth(squareSz * 12f)   // Use 256 + bar sizes?
                val p = g.colorPickerRef to FloatArray(4)
                valueChanged = colorPicker4("##picker", col, pickerFlags, p) or valueChanged
                g.colorPickerRef put p
                endPopup()
            }
        }

        if (0 != labelDisplayEnd && flags hasnt Cef.NoLabel) { // TODO check first comparison
            val textOffsetX = if (flags has Cef.NoInputs) wButton else wFull + style.itemInnerSpacing.x
            window.dc.cursorPos.put(pos.x + textOffsetX, pos.y + style.framePadding.y)
            textEx(label, labelDisplayEnd)
        }

        // Convert back
        if (valueChanged && pickerActiveWindow == null) {
            if (!valueChangedAsFloat) for (n in 0..3) f[n] = i[n] / 255f
            if (flags has Cef.DisplayHSV && flags has Cef.InputRGB) {
                g.colorEditLastHue = f[0]
                g.colorEditLastSat = f[1]
                f.hsvToRGB()
                g.colorEditLastColor[0] = f[0]
                g.colorEditLastColor[1] = f[1]
                g.colorEditLastColor[2] = f[2]
            }
            if (flags has Cef.DisplayRGB && flags has Cef.InputHSV)
                f.rgbToHSV()
            col[0] = f[0]
            col[1] = f[1]
            col[2] = f[2]
            if (alpha) col[3] = f[3]
        }
        popID()
        endGroup()

        // Drag and Drop Target

        // NB: The flag test is merely an optional micro-optimization, BeginDragDropTarget() does the same test.
        if (window.dc.lastItemStatusFlags has ItemStatusFlag.HoveredRect && beginDragDropTarget()) {
            var acceptedDragDrop = false
            acceptDragDropPayload(PAYLOAD_TYPE_COLOR_3F)?.let {
                val data = it.data!! as Vec4
                for (j in 0..2)  // Preserve alpha if any
                    col[j] = data.array[j]
                acceptedDragDrop = true
                valueChanged = true
            }
            acceptDragDropPayload(PAYLOAD_TYPE_COLOR_4F)?.let {
                val floats = (it.data!! as Vec4).array
                for (j in 0 until components)
                    col[j] = floats[j]
                acceptedDragDrop = true
                valueChanged = true
            }

            // Drag-drop payloads are always RGB
            if (acceptedDragDrop && flags has Cef.InputHSV)
                col.rgbToHSV()
            endDragDropTarget()
        }

        // When picker is being actively used, use its active id so IsItemActive() will function on ColorEdit4().
        if (pickerActiveWindow != null && g.activeId != 0 && g.activeIdWindow === pickerActiveWindow)
            window.dc.lastItemId = g.activeId

        if (valueChanged)
            markItemEdited(window.dc.lastItemId)

        return valueChanged
    }

    fun String.scanHex(ints: IntArray, count: Int = ints.size, precision: Int) {
        var c = 0
        for (i in 0 until count) {
            val end = glm.min((i + 1) * precision, length)
            ints[i] = if (c > end) 0 else with(substring(c, end)) { if (isEmpty()) 0 else toInt(16) }
            c += precision
        }
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
     *  (In C++ the 'float col[4]' notation for a function argument is equivalent to 'float* col', we only specify a size to facilitate understanding of the code.)
     *  FIXME: we adjust the big color square height based on item width, which may cause a flickering feedback loop
     *  (if automatic height makes a vertical scrollbar appears, affecting automatic width..)
     *  FIXME: this is trying to be aware of style.Alpha but not fully correct. Also, the color wheel will have overlapping glitches with (style.Alpha < 1.0)   */
    fun colorPicker4(label: String, col: Vec4, flags: ColorEditFlags = 0, refCol: Vec4? = null): Boolean =
            colorPicker4(label, col to _fa, flags, refCol?.to(_fa2))
                    .also { col put _fa; refCol?.put(_fa2) }

    /** ColorPicker
     *  Note: only access 3 floats if ImGuiColorEditFlags_NoAlpha flag is set.
     *  (In C++ the 'float col[4]' notation for a function argument is equivalent to 'float* col', we only specify a size to facilitate understanding of the code.)
     *  FIXME: we adjust the big color square height based on item width, which may cause a flickering feedback loop
     *  (if automatic height makes a vertical scrollbar appears, affecting automatic width..)
     *  FIXME: this is trying to be aware of style.Alpha but not fully correct. Also, the color wheel will have overlapping glitches with (style.Alpha < 1.0)   */
    fun colorPicker4(label: String, col: FloatArray, flags_: ColorEditFlags = 0, refCol: FloatArray? = null): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val drawList = window.drawList

        val width = calcItemWidth()
        g.nextItemData.clearFlags()

        pushID(label)
        beginGroup()

        var flags = flags_
        if (flags hasnt Cef.NoSidePreview)
            flags = flags or Cef.NoSmallPreview

        // Context menu: display and store options.
        if (flags hasnt Cef.NoOptions)
            colorPickerOptionsPopup(col, flags)

        // Read stored options
        if (flags hasnt Cef._PickerMask)
            flags = flags or ((if (g.colorEditOptions has Cef._PickerMask) g.colorEditOptions else Cef._OptionsDefault.i) and Cef._PickerMask)
        if (flags hasnt Cef._InputMask)
            flags = flags or ((if (g.colorEditOptions has Cef._InputMask) g.colorEditOptions else Cef._OptionsDefault.i) and Cef._InputMask)
        assert((flags and Cef._PickerMask).isPowerOfTwo) { "Check that only 1 is selected" }
        assert((flags and Cef._InputMask).isPowerOfTwo);  // Check that only 1 is selected
        if (flags hasnt Cef.NoOptions)
            flags = flags or (g.colorEditOptions and Cef.AlphaBar)

        // Setup
        val components = if (flags has Cef.NoAlpha) 3 else 4
        val alphaBar = flags has Cef.AlphaBar && flags hasnt Cef.NoAlpha
        val pickerPos = Vec2(window.dc.cursorPos)
        val squareSz = frameHeight
        val barsWidth = squareSz     // Arbitrary smallish width of Hue/Alpha picking bars
        // Saturation/Value picking box
        val svPickerSize = glm.max(barsWidth * 1, width - (if (alphaBar) 2 else 1) * (barsWidth + style.itemInnerSpacing.x))
        val bar0PosX = pickerPos.x + svPickerSize + style.itemInnerSpacing.x
        val bar1PosX = bar0PosX + barsWidth + style.itemInnerSpacing.x
        val barsTrianglesHalfSz = floor(barsWidth * 0.2f)

        val backupInitialCol = FloatArray(4) { col.getOrElse(it) { 0f } }

        val wheelThickness = svPickerSize * 0.08f
        val wheelROuter = svPickerSize * 0.50f
        val wheelRInner = wheelROuter - wheelThickness
        val wheelCenter = Vec2(pickerPos.x + (svPickerSize + barsWidth) * 0.5f, pickerPos.y + svPickerSize * 0.5f)

        // Note: the triangle is displayed rotated with trianglePa pointing to Hue, but most coordinates stays unrotated for logic.
        val triangleR = wheelRInner - (svPickerSize * 0.027f).i
        val trianglePa = Vec2(triangleR, 0f)   // Hue point.
        val trianglePb = Vec2(triangleR * -0.5f, triangleR * -0.866025f) // Black point.
        val trianglePc = Vec2(triangleR * -0.5f, triangleR * +0.866025f) // White point.

        val hsv = FloatArray(3) { col[it] }
        val rgb = FloatArray(3) { col[it] }
        if (flags has Cef.InputRGB) {
            // Hue is lost when converting from greyscale rgb (saturation=0). Restore it.
            colorConvertRGBtoHSV(rgb, hsv)
            if (g.colorEditLastColor[0] == col[0] && g.colorEditLastColor[1] == col[1] && g.colorEditLastColor[2] == col[2]) {
                if (hsv[1] == 0f)
                    hsv[0] = g.colorEditLastHue
                if (hsv[2] == 0f)
                    hsv[1] = g.colorEditLastSat
            }
        } else if (flags has Cef.InputHSV)
            colorConvertHSVtoRGB(hsv, rgb)
        var (H, S, V) = hsv
        var (R, G, B) = rgb // turn to capital as cpp to avoid clashing with ImGui `g`

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
                    H = glm.atan(currentOff.y, currentOff.x) / glm.PIf * 0.5f
                    if (H < 0f)
                        H += 1f
                    valueChanged = true
                    valueChangedH = true
                }
                val cosHueAngle = glm.cos(-H * 2f * glm.PIf)
                val sinHueAngle = glm.sin(-H * 2f * glm.PIf)
                if (triangleContainsPoint(trianglePa, trianglePb, trianglePc, initialOff.rotate(cosHueAngle, sinHueAngle))) {
                    // Interacting with SV triangle
                    val currentOffUnrotated = currentOff.rotate(cosHueAngle, sinHueAngle)
                    if (!triangleContainsPoint(trianglePa, trianglePb, trianglePc, currentOffUnrotated))
                        currentOffUnrotated put triangleClosestPoint(trianglePa, trianglePb, trianglePc, currentOffUnrotated)
                    val (uu, vv, _) = triangleBarycentricCoords(trianglePa, trianglePb, trianglePc, currentOffUnrotated)
                    V = glm.clamp(1f - vv, 0.0001f, 1f)
                    S = glm.clamp(uu / V, 0.0001f, 1f)
                    valueChangedSv = true
                    valueChanged = true
                }
            }
            if (flags hasnt Cef.NoOptions) openPopupOnItemClick("context")

        } else if (flags has Cef.PickerHueBar) {
            // SV rectangle logic
            invisibleButton("sv", Vec2(svPickerSize))
            if (isItemActive) {
                S = saturate((io.mousePos.x - pickerPos.x) / (svPickerSize - 1))
                V = 1f - saturate((io.mousePos.y - pickerPos.y) / (svPickerSize - 1))
                valueChangedSv = true
                valueChanged = true
            }
            if (flags hasnt Cef.NoOptions) openPopupOnItemClick("context")
            // Hue bar logic
            cursorScreenPos = Vec2(bar0PosX, pickerPos.y)
            invisibleButton("hue", Vec2(barsWidth, svPickerSize))
            if (isItemActive) {
                H = saturate((io.mousePos.y - pickerPos.y) / (svPickerSize - 1))
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
                textEx(label, labelDisplayEnd)
            }
        }
        if (flags hasnt Cef.NoSidePreview) {
            pushItemFlag(ItemFlag.NoNavDefaultFocus.i, true)
            val colV4 = Vec4(col[0], col[1], col[2], if (flags has Cef.NoAlpha) 1f else col[3])
            if (flags has Cef.NoLabel)
                text("Current")

            val subFlagsToForward = Cef._InputMask or Cef.HDR or Cef.AlphaPreview or Cef.AlphaPreviewHalf or Cef.NoTooltip
            colorButton("##current", colV4, flags and subFlagsToForward, Vec2(squareSz * 3, squareSz * 2))
            refCol?.let {
                text("Original")
                val refColV4 = Vec4(it[0], it[1], it[2], if (flags has Cef.NoAlpha) 1f else it[3])
                if (colorButton("##original", refColV4, flags and subFlagsToForward, Vec2(squareSz * 3, squareSz * 2))) {
                    for (i in 0 until components) col[i] = it[i]
                    valueChanged = true
                }
            }
            popItemFlag()
            endGroup()
        }

        // Convert back color to RGB
        if (valueChangedH || valueChangedSv)
            if (flags has Cef.InputRGB) {
                colorConvertHSVtoRGB(if (H >= 1f) H - 10 * 1e-6f else H, if (S > 0f) S else 10 * 1e-6f, if (V > 0f) V else 1e-6f, col)
                g.colorEditLastHue = H
                g.colorEditLastSat = S
                g.colorEditLastColor[0] = col[0]
                g.colorEditLastColor[1] = col[1]
                g.colorEditLastColor[2] = col[2]
            } else if (flags has Cef.InputHSV) {
                col[0] = H
                col[1] = S
                col[2] = V
            }

        // R,G,B and H,S,V slider color editor
        var valueChangedFixHueWrap = false
        if (flags hasnt Cef.NoInputs) {
            pushItemWidth((if (alphaBar) bar1PosX else bar0PosX) + barsWidth - pickerPos.x)
            val subFlagsToForward = Cef._DataTypeMask or Cef._InputMask or Cef.HDR or Cef.NoAlpha or Cef.NoOptions or Cef.NoSmallPreview or
                    Cef.AlphaPreview or Cef.AlphaPreviewHalf
            val subFlags = (flags and subFlagsToForward) or Cef.NoPicker
            if (flags has Cef.DisplayRGB || flags hasnt Cef._DisplayMask)
                if (colorEdit4("##rgb", col, subFlags or Cef.DisplayRGB)) {
                    // FIXME: Hackily differentiating using the DragInt (ActiveId != 0 && !ActiveIdAllowOverlap) vs. using the InputText or DropTarget.
                    // For the later we don't want to run the hue-wrap canceling code. If you are well versed in HSV picker please provide your input! (See #2050)
                    valueChangedFixHueWrap = g.activeId != 0 && !g.activeIdAllowOverlap
                    valueChanged = true
                }
            if (flags has Cef.DisplayHSV || flags hasnt Cef._DisplayMask)
                valueChanged = colorEdit4("##hsv", col, subFlags or Cef.DisplayHSV) || valueChanged
            if (flags has Cef.DisplayHEX || flags hasnt Cef._DisplayMask)
                valueChanged = colorEdit4("##hex", col, subFlags or Cef.DisplayHEX) || valueChanged
            popItemWidth()
        }

        // Try to cancel hue wrap (after ColorEdit4 call), if any
        if (valueChangedFixHueWrap && flags has Cef.InputRGB) {
            val (newH, newS, newV) = colorConvertRGBtoHSV(col)
            if (newH <= 0 && H > 0) {
                if (newV <= 0 && V != newV)
                    colorConvertHSVtoRGB(H, S, if (newV <= 0) V * 0.5f else newV, col)
                else if (newS <= 0)
                    colorConvertHSVtoRGB(H, if (newS <= 0) S * 0.5f else newS, newV, col)
            }
        }

        if (valueChanged) {
            if (flags has Cef.InputRGB) {
                R = col[0]
                G = col[1]
                B = col[2]
                colorConvertRGBtoHSV(R, G, B).let {
                    H = it[0]
                    S = it[1]
                    V = it[2]
                }
                if (g.colorEditLastColor[0] == col[0] && g.colorEditLastColor[1] == col[1] && g.colorEditLastColor[2] == col[2]) { // Fix local Hue as display below will use it immediately.
                    if (S == 0f)
                        H = g.colorEditLastHue
                    if (V == 0f)
                        S = g.colorEditLastSat
                }
            } else if (flags has Cef.InputHSV) {
                H = col[0]
                S = col[1]
                V = col[2]
                colorConvertHSVtoRGB(H, S, V).let {
                    R = it[0]
                    G = it[1]
                    B = it[2]
                }
            }
        }

        val styleAlpha8 = F32_TO_INT8_SAT(style.alpha)
        val colBlack = COL32(0, 0, 0, styleAlpha8)
        val colWhite = COL32(255, 255, 255, styleAlpha8)
        val colMidgrey = COL32(128, 128, 128, styleAlpha8)
        val colHues = arrayOf(COL32(255, 0, 0, styleAlpha8), COL32(255, 255, 0, styleAlpha8), COL32(0, 255, 0, styleAlpha8), COL32(0, 255, 255, styleAlpha8), COL32(0, 0, 255, styleAlpha8), COL32(255, 0, 255, styleAlpha8), COL32(255, 0, 0, styleAlpha8))

        val hueColorF = Vec4(1f, 1f, 1f, style.alpha); colorConvertHSVtoRGB(H, 1f, 1f, hueColorF::x, hueColorF::y, hueColorF::z)
        val hueColor32 = hueColorF.u32
        val userCol32StripedOfAlpha = Vec4(R, G, B, style.alpha).u32 // Important: this is still including the main rendering/style alpha!!

        val svCursorPos = Vec2()

        if (flags has Cef.PickerHueWheel) {
            // Render Hue Wheel
            val aeps = 0.5f / wheelROuter   // Half a pixel arc length in radians (2pi cancels out).
            val segmentPerArc = glm.max(4, (wheelROuter / 12).i)
            for (n in 0..5) {
                val a0 = n / 6f * 2f * glm.PIf - aeps
                val a1 = (n + 1f) / 6f * 2f * glm.PIf + aeps
                val vertStartIdx = drawList.vtxBuffer.size
                drawList.pathArcTo(wheelCenter, (wheelRInner + wheelROuter) * 0.5f, a0, a1, segmentPerArc)
                drawList.pathStroke(colWhite, false, wheelThickness)
                val vertEndIdx = drawList.vtxBuffer.size

                // Paint colors over existing vertices
                val gradientP0 = Vec2(wheelCenter.x + a0.cos * wheelRInner, wheelCenter.y + a0.sin * wheelRInner)
                val gradientP1 = Vec2(wheelCenter.x + a1.cos * wheelRInner, wheelCenter.y + a1.sin * wheelRInner)
                drawList.shadeVertsLinearColorGradientKeepAlpha(vertStartIdx, vertEndIdx, gradientP0, gradientP1, colHues[n], colHues[n + 1])
            }

            // Render Cursor + preview on Hue Wheel
            val cosHueAngle = glm.cos(H * 2f * glm.PIf)
            val sinHueAngle = glm.sin(H * 2f * glm.PIf)
            val hueCursorPos = Vec2(wheelCenter.x + cosHueAngle * (wheelRInner + wheelROuter) * 0.5f,
                    wheelCenter.y + sinHueAngle * (wheelRInner + wheelROuter) * 0.5f)
            val hueCursorRad = wheelThickness * if (valueChangedH) 0.65f else 0.55f
            val hueCursorSegments = glm.clamp((hueCursorRad / 1.4f).i, 9, 32)
            drawList.addCircleFilled(hueCursorPos, hueCursorRad, hueColor32, hueCursorSegments)
            drawList.addCircle(hueCursorPos, hueCursorRad + 1, colMidgrey, hueCursorSegments)
            drawList.addCircle(hueCursorPos, hueCursorRad, colWhite, hueCursorSegments)

            // Render SV triangle (rotated according to hue)
            val tra = wheelCenter + trianglePa.rotate(cosHueAngle, sinHueAngle)
            val trb = wheelCenter + trianglePb.rotate(cosHueAngle, sinHueAngle)
            val trc = wheelCenter + trianglePc.rotate(cosHueAngle, sinHueAngle)
            val uvWhite = fontTexUvWhitePixel
            drawList.primReserve(6, 6)
            drawList.primVtx(tra, uvWhite, hueColor32)
            drawList.primVtx(trb, uvWhite, hueColor32)
            drawList.primVtx(trc, uvWhite, colWhite)
            drawList.primVtx(tra, uvWhite, 0)
            drawList.primVtx(trb, uvWhite, colBlack)
            drawList.primVtx(trc, uvWhite, 0)
            drawList.addTriangle(tra, trb, trc, colMidgrey, 1.5f)
            svCursorPos put trc.lerp(tra, saturate(S)).lerp(trb, saturate(1 - V))
        } else if (flags has Cef.PickerHueBar) {
            // Render SV Square
            drawList.addRectFilledMultiColor(pickerPos, pickerPos + svPickerSize, colWhite, hueColor32, hueColor32, colWhite)
            drawList.addRectFilledMultiColor(pickerPos, pickerPos + svPickerSize, 0, 0, colBlack, colBlack)
            renderFrameBorder(pickerPos, pickerPos + svPickerSize, 0f)
            // Sneakily prevent the circle to stick out too much
            svCursorPos.x = glm.clamp(floor(pickerPos.x + saturate(S) * svPickerSize + 0.5f), pickerPos.x + 2, pickerPos.x + svPickerSize - 2)
            svCursorPos.y = glm.clamp(floor(pickerPos.y + saturate(1 - V) * svPickerSize + 0.5f), pickerPos.y + 2, pickerPos.y + svPickerSize - 2)

            // Render Hue Bar
            for (i in 0..5) {
                val a = Vec2(bar0PosX, pickerPos.y + i * (svPickerSize / 6))
                val c = Vec2(bar0PosX + barsWidth, pickerPos.y + (i + 1) * (svPickerSize / 6))
                drawList.addRectFilledMultiColor(a, c, colHues[i], colHues[i], colHues[i + 1], colHues[i + 1])
            }
            val bar0LineY = round(pickerPos.y + H * svPickerSize)
            renderFrameBorder(Vec2(bar0PosX, pickerPos.y), Vec2(bar0PosX + barsWidth, pickerPos.y + svPickerSize), 0f)
            drawList.renderArrowsForVerticalBar(Vec2(bar0PosX - 1, bar0LineY), Vec2(barsTrianglesHalfSz + 1, barsTrianglesHalfSz), barsWidth + 2f, style.alpha)
        }

        // Render cursor/preview circle (clamp S/V within 0..1 range because floating points colors may lead HSV values to be out of range)
        val svCursorRad = if (valueChangedSv) 10f else 6f
        drawList.addCircleFilled(svCursorPos, svCursorRad, userCol32StripedOfAlpha, 12)
        drawList.addCircle(svCursorPos, svCursorRad + 1, colMidgrey, 12)
        drawList.addCircle(svCursorPos, svCursorRad, colWhite, 12)

        // Render alpha bar
        if (alphaBar) {
            val alpha = saturate(col[3])
            val bar1Bb = Rect(bar1PosX, pickerPos.y, bar1PosX + barsWidth, pickerPos.y + svPickerSize)
            renderColorRectWithAlphaCheckerboard(drawList, bar1Bb.min, bar1Bb.max, 0, bar1Bb.width / 2f, Vec2())
            drawList.addRectFilledMultiColor(bar1Bb.min, bar1Bb.max, userCol32StripedOfAlpha, userCol32StripedOfAlpha, userCol32StripedOfAlpha wo COL32_A_MASK, userCol32StripedOfAlpha wo COL32_A_MASK)
            val bar1LineY = round(pickerPos.y + (1f - alpha) * svPickerSize)
            renderFrameBorder(bar1Bb.min, bar1Bb.max, 0f)
            drawList.renderArrowsForVerticalBar(Vec2(bar1PosX - 1, bar1LineY), Vec2(barsTrianglesHalfSz + 1, barsTrianglesHalfSz), barsWidth + 2f, style.alpha)
        }

        endGroup()

        var compare = true
        repeat(components) { if (backupInitialCol[it] != col[it]) compare = false }
        if (valueChanged && compare)
            valueChanged = false
        if (valueChanged)
            markItemEdited(window.dc.lastItemId)

        popID()

        return valueChanged
    }

    /**  A little colored square. Return true when clicked.
     *  FIXME: May want to display/ignore the alpha component in the color display? Yet show it in the tooltip.
     *  'desc_id' is not called 'label' because we don't display it next to the button, but only in the tooltip.
     *  Note that 'col' may be encoded in HSV if ImGuiColorEditFlags_InputHSV is set.   */
    fun colorButton(descId: String, col: Vec4, flags_: ColorEditFlags = 0, size: Vec2 = Vec2()): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getID(descId)
        val defaultSize = frameHeight
        if (size.x == 0f)
            size.x = defaultSize
        if (size.y == 0f)
            size.y = defaultSize
        val bb = Rect(window.dc.cursorPos, window.dc.cursorPos + size)
        itemSize(bb, if (size.y >= defaultSize) style.framePadding.y else 0f)
        if (!itemAdd(bb, id)) return false

        val (pressed, hovered, _) = buttonBehavior(bb, id)

        var flags = flags_
        if (flags has Cef.NoAlpha)
            flags = flags and (Cef.AlphaPreview or Cef.AlphaPreviewHalf).inv()

        val colRgb = Vec4(col)
        if (flags has Cef.InputHSV)
            colorConvertHSVtoRGB(colRgb)

        val colRgbWithoutAlpha = Vec4(colRgb.x, colRgb.y, colRgb.z, 1f)
        val gridStep = glm.min(size.x, size.y) / 2.99f
        val rounding = glm.min(style.frameRounding, gridStep * 0.5f)
        val bbInner = Rect(bb)
        var off = 0f
        if (flags hasnt Cef.NoBorder) {
            off = -0.75f // The border (using Col_FrameBg) tends to look off when color is near-opaque and rounding is enabled. This offset seemed like a good middle ground to reduce those artifacts.
            bbInner expand off
        }
        if (flags has Cef.AlphaPreviewHalf && colRgb.w < 1f) {
            val midX = round((bbInner.min.x + bbInner.max.x) * 0.5f)
            renderColorRectWithAlphaCheckerboard(window.drawList, Vec2(bbInner.min.x + gridStep, bbInner.min.y), bbInner.max,
                    getColorU32(colRgb), gridStep, Vec2(-gridStep + off, off), rounding, Dcf.TopRight or Dcf.BotRight)
            window.drawList.addRectFilled(bbInner.min, Vec2(midX, bbInner.max.y), getColorU32(colRgbWithoutAlpha), rounding,
                    Dcf.TopLeft or Dcf.BotLeft)
        } else {
            /*  Because getColorU32() multiplies by the global style alpha and we don't want to display a checkerboard 
                if the source code had no alpha */
            val colSource = if (flags has Cef.AlphaPreview) colRgb else colRgbWithoutAlpha
            if (colSource.w < 1f)
                renderColorRectWithAlphaCheckerboard(window.drawList, bbInner.min, bbInner.max, colSource.u32, gridStep, Vec2(off), rounding)
            else
                window.drawList.addRectFilled(bbInner.min, bbInner.max, getColorU32(colSource), rounding, Dcf.All.i)
        }
        renderNavHighlight(bb, id)
        // Color button are often in need of some sort of border
        if (flags hasnt Cef.NoBorder)
            if (g.style.frameBorderSize > 0f)
                renderFrameBorder(bb.min, bb.max, rounding)
            else
                window.drawList.addRect(bb.min, bb.max, Col.FrameBg.u32, rounding)

        /*  Drag and Drop Source
            NB: The ActiveId test is merely an optional micro-optimization, BeginDragDropSource() does the same test.         */
        if (g.activeId == id && flags hasnt Cef.NoDragDrop && beginDragDropSource()) {

            if (flags has Cef.NoAlpha)
                setDragDropPayload(PAYLOAD_TYPE_COLOR_3F, colRgb, Cond.Once)
            else
                setDragDropPayload(PAYLOAD_TYPE_COLOR_4F, colRgb, Cond.Once)
            colorButton(descId, col, flags)
            sameLine()
            textEx("Color")
            endDragDropSource()
        }
        // Tooltip
        if (flags hasnt Cef.NoTooltip && hovered) {
            val pF = floatArrayOf(col.x, col.y, col.z, col.w)
            colorTooltip(descId, pF, flags and (Cef._InputMask or Cef.NoAlpha or Cef.AlphaPreview or Cef.AlphaPreviewHalf))
            col.put(pF)
        }

        return pressed
    }

    /** initialize current options (generally on application startup) if you want to select a default format, picker
     *  type, etc. User will be able to change many settings, unless you pass the _NoOptions flag to your calls.    */
    fun setColorEditOptions(flags_: ColorEditFlags) {
        var flags = flags_
        if (flags hasnt Cef._DisplayMask)
            flags = flags or (Cef._OptionsDefault and Cef._DisplayMask)
        if (flags hasnt Cef._DataTypeMask)
            flags = flags or (Cef._OptionsDefault and Cef._DataTypeMask)
        if (flags hasnt Cef._PickerMask)
            flags = flags or (Cef._OptionsDefault and Cef._PickerMask)
        if (flags hasnt Cef._InputMask)
            flags = flags or (Cef._OptionsDefault and Cef._InputMask)
        assert((flags and Cef._DisplayMask).isPowerOfTwo) { "Check only 1 option is selected" }
        assert((flags and Cef._DataTypeMask).isPowerOfTwo) { "Check only 1 option is selected" }
        assert((flags and Cef._PickerMask).isPowerOfTwo) { "Check only 1 option is selected" }
        assert((flags and Cef._InputMask).isPowerOfTwo) { "Check only 1 option is selected" }
        g.colorEditOptions = flags
    }

    companion object {
        val ids = arrayOf("##X", "##Y", "##Z", "##W")
        val fmtTableInt = arrayOf(
                arrayOf("%3d", "%3d", "%3d", "%3d"),             // Short display
                arrayOf("R:%3d", "G:%3d", "B:%3d", "A:%3d"),     // Long display for RGBA
                arrayOf("H:%3d", "S:%3d", "V:%3d", "A:%3d"))     // Long display for HSVA
        val fmtTableFloat = arrayOf(
                arrayOf("%.3f", "%.3f", "%.3f", "%.3f"),            // Short display
                arrayOf("R:%.3f", "G:%.3f", "B:%.3f", "A:%.3f"),    // Long display for RGBA
                arrayOf("H:%.3f", "S:%.3f", "V:%.3f", "A:%.3f"))    // Long display for HSVA

        fun DrawList.renderArrowsForVerticalBar(pos: Vec2, halfSz: Vec2, barW: Float, alpha: Float) {
            val alpha8 = F32_TO_INT8_SAT(alpha)
            // @formatter:off
            renderArrowPointingAt(Vec2(pos.x + halfSz.x + 1, pos.y), Vec2(halfSz.x + 2, halfSz.y + 1), Dir.Right, COL32(0, 0, 0, alpha8))
            renderArrowPointingAt(Vec2(pos.x + halfSz.x, pos.y), halfSz, Dir.Right, COL32(255, 255, 255, alpha8))
            renderArrowPointingAt(Vec2(pos.x + barW - halfSz.x - 1, pos.y), Vec2(halfSz.x + 2, halfSz.y + 1), Dir.Left, COL32(0, 0, 0, alpha8))
            renderArrowPointingAt(Vec2(pos.x + barW - halfSz.x, pos.y), halfSz, Dir.Left, COL32(255, 255, 255, alpha8))
            // @formatter:on
        }
    }
}