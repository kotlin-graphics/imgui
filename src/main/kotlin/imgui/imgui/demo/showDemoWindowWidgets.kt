package imgui.imgui.demo

import gli_.has
import glm_.BYTES
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import glm_.vec2.operators.div
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.acceptDragDropPayload
import imgui.ImGui.alignTextToFramePadding
import imgui.ImGui.arrowButton
import imgui.ImGui.beginChild
import imgui.ImGui.beginCombo
import imgui.ImGui.beginDragDropSource
import imgui.ImGui.beginDragDropTarget
import imgui.ImGui.beginPopupContextItem
import imgui.ImGui.begin_
import imgui.ImGui.bullet
import imgui.ImGui.bulletText
import imgui.ImGui.button
import imgui.ImGui.checkbox
import imgui.ImGui.checkboxFlags
import imgui.ImGui.collapsingHeader
import imgui.ImGui.colorButton
import imgui.ImGui.colorConvertHSVtoRGB
import imgui.ImGui.colorEdit3
import imgui.ImGui.colorEdit4
import imgui.ImGui.colorPicker4
import imgui.ImGui.columns
import imgui.ImGui.combo
import imgui.ImGui.cursorPos
import imgui.ImGui.cursorScreenPos
import imgui.ImGui.dragFloat
import imgui.ImGui.dragFloat2
import imgui.ImGui.dragFloat3
import imgui.ImGui.dragFloat4
import imgui.ImGui.dragFloatRange2
import imgui.ImGui.dragInt
import imgui.ImGui.dragInt2
import imgui.ImGui.dragInt3
import imgui.ImGui.dragInt4
import imgui.ImGui.dragIntRange2
import imgui.ImGui.dragVec4
import imgui.ImGui.end
import imgui.ImGui.endChild
import imgui.ImGui.endCombo
import imgui.ImGui.endDragDropSource
import imgui.ImGui.endDragDropTarget
import imgui.ImGui.endPopup
import imgui.ImGui.fontSize
import imgui.ImGui.image
import imgui.ImGui.imageButton
import imgui.ImGui.indent
import imgui.ImGui.inputDouble
import imgui.ImGui.inputFloat
import imgui.ImGui.inputFloat2
import imgui.ImGui.inputFloat3
import imgui.ImGui.inputFloat4
import imgui.ImGui.inputInt
import imgui.ImGui.inputInt2
import imgui.ImGui.inputInt3
import imgui.ImGui.inputInt4
import imgui.ImGui.inputText
import imgui.ImGui.inputTextMultiline
import imgui.ImGui.inputTextWithHint
import imgui.ImGui.io
import imgui.ImGui.isItemActivated
import imgui.ImGui.isItemActive
import imgui.ImGui.isItemClicked
import imgui.ImGui.isItemDeactivated
import imgui.ImGui.isItemDeactivatedAfterEdit
import imgui.ImGui.isItemEdited
import imgui.ImGui.isItemFocused
import imgui.ImGui.isItemHovered
import imgui.ImGui.isItemVisible
import imgui.ImGui.isMouseDoubleClicked
import imgui.ImGui.isWindowFocused
import imgui.ImGui.isWindowHovered
import imgui.ImGui.itemRectMax
import imgui.ImGui.itemRectMin
import imgui.ImGui.itemRectSize
import imgui.ImGui.labelText
import imgui.ImGui.listBox
import imgui.ImGui.menuItem
import imgui.ImGui.newLine
import imgui.ImGui.nextColumn
import imgui.ImGui.openPopup
import imgui.ImGui.plotHistogram
import imgui.ImGui.plotLines
import imgui.ImGui.popButtonRepeat
import imgui.ImGui.popId
import imgui.ImGui.popStyleVar
import imgui.ImGui.progressBar
import imgui.ImGui.pushButtonRepeat
import imgui.ImGui.pushId
import imgui.ImGui.pushStyleVar
import imgui.ImGui.radioButton
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.separator
import imgui.ImGui.setColorEditOptions
import imgui.ImGui.setDragDropPayload
import imgui.ImGui.setItemDefaultFocus
import imgui.ImGui.setTooltip
import imgui.ImGui.sliderAngle
import imgui.ImGui.sliderFloat
import imgui.ImGui.sliderFloat2
import imgui.ImGui.sliderFloat3
import imgui.ImGui.sliderFloat4
import imgui.ImGui.sliderInt
import imgui.ImGui.sliderInt2
import imgui.ImGui.sliderInt3
import imgui.ImGui.sliderInt4
import imgui.ImGui.sliderVec4
import imgui.ImGui.smallButton
import imgui.ImGui.spacing
import imgui.ImGui.style
import imgui.ImGui.text
import imgui.ImGui.textColored
import imgui.ImGui.textDisabled
import imgui.ImGui.textLineHeight
import imgui.ImGui.textWrapped
import imgui.ImGui.time
import imgui.ImGui.treeNodeExV
import imgui.ImGui.treeNodeToLabelSpacing
import imgui.ImGui.treePop
import imgui.ImGui.unindent
import imgui.ImGui.vSliderFloat
import imgui.ImGui.vSliderInt
import imgui.ImGui.windowDrawList
import imgui.functionalProgramming.collapsingHeader
import imgui.functionalProgramming.popup
import imgui.functionalProgramming.smallButton
import imgui.functionalProgramming.treeNode
import imgui.functionalProgramming.withGroup
import imgui.functionalProgramming.withId
import imgui.functionalProgramming.withItemWidth
import imgui.functionalProgramming.withStyleColor
import imgui.functionalProgramming.withStyleVar
import imgui.functionalProgramming.withTextWrapPos
import imgui.functionalProgramming.withTooltip
import imgui.imgui.imgui_demoDebugInformations.Companion.helpMarker
import imgui.or
import kotlin.math.cos
import kotlin.reflect.KMutableProperty0
import imgui.ColorEditFlag as Cef
import imgui.InputTextFlag as Itf
import imgui.SelectableFlag as Sf
import imgui.TreeNodeFlag as Tnf
import imgui.WindowFlag as Wf

object showDemoWindowWidgets {

    /* Basic */
    var counter = 0
    var clicked = 0
    var check = true
    var e = 0
    val arr = floatArrayOf(0.6f, 0.1f, 1f, 0.5f, 0.92f, 0.1f, 0.2f)
    var currentItem0 = 0
    var str0 = "Hello, world!".toCharArray(CharArray(128))
    var str1 = "".toCharArray(CharArray(128))
    var i0 = 123
    var f0 = 0.001f
    var f1 = 1e10f
    var d0 = 999999.00000001
    val vec4a = floatArrayOf(0.1f, 0.2f, 0.3f, 0.44f)
    var i1 = 50
    var i2 = 42
    var f2 = 1f
    var f3 = 0.0067f
    var i3 = 0
    var f4 = 0.123f
    var f5 = 0f
    var angle = 0f
    val col1 = floatArrayOf(1f, 0f, 0.2f)
    val col2 = floatArrayOf(0.4f, 0.7f, 0f, 0.5f)
    var listboxItemCurrent = 1


    /* Trees */
    var alignLabelWithCurrentXposition = false
    /** Dumb representation of what may be user-side selection state. You may carry selection state inside or
     *  outside your objects in whatever format you see fit.    */
    var selectionMask = 1 shl 2


    /* Collapsing Headers */
    var closableGroup = true


    /* Text */
    var wrapWidth = 200f
    val buf = "日本語".toCharArray(CharArray(32)) // "nihongo"


    /* Images */
    var pressedCount = 0


    /* Combo */
    var flags0: ComboFlags = 0
    var currentItem3 = 0
    var currentItem4 = 0
    var currentItem5 = 0


    /* Selectables */
    val selection0 = booleanArrayOf(false, true, false, false, false)
    val selection1 = BooleanArray(5)
    var selected0 = -1
    val selected1 = BooleanArray(3)
    val selected2 = BooleanArray(16)
    val selected3 = booleanArrayOf(
            true, false, false, false,
            false, true, false, false,
            false, false, true, false,
            false, false, false, true)
    val selected4 = booleanArrayOf(true, false, true, false, true, false, true, false, true)


    /* Multi-line Text Input */
    val textMultiline = """
        /*
        The Pentium F00F bug, shorthand for F0 0F C7 C8,
        the hexadecimal encoding of one offending instruction,
        more formally, the invalid operand with locked CMPXCHG8B
        instruction bug, is a design flaw in the majority of
        Intel Pentium, Pentium MMX, and Pentium OverDrive
        processors (all in the P5 microarchitecture).
        */

        label:
            lock cmpxchg8b eax
        """.toCharArray(CharArray(1024 * 16))
    var flags = Itf.AllowTabInput.i


    /* Color/Picker Widgets */
    val color = Vec4.fromColor(114, 144, 154, 200)
    var alphaPreview = true
    var alphaHalfPreview = false
    var dragAndDrop = true
    var optionsMenu = true
    var hdr = false
    // Generate a dummy default palette. The palette will persist and can be edited.
    var savedPaletteInit = true
    var savedPalette = Array(32) { Vec4() }
    var backupColor = Vec4()
    var alpha = true
    var alphaBar = true
    var sidePreview = true
    var refColor = false
    var refColorV = Vec4(1f, 0f, 1f, 0.5f)
    var displayMode = 0
    var pickerMode = 0
    var colorStoredAsHsv = Vec4(0.23f, 1f, 1f, 1f)


    /* Range Widgets */
    var begin = 10f
    var end = 90f
    var beginI = 100
    var endI = 1000


    /* Multi-component Widgets */
    var vec4f = floatArrayOf(0.1f, 0.2f, 0.3f, 0.44f)
    val vec4i = intArrayOf(1, 5, 100, 255)


    /* Plots Widgets */
    var animate = true
    var refreshTime = 0.0
    val values0 = FloatArray(90)
    var valuesOffset = 0
    var phase = 0f
    var funcType = 0
    var displayCount = 70

    object Funcs1 {
        fun sin(i: Int) = kotlin.math.sin(i * 0.1f)
        fun saw(i: Int) = if (i has 1) 1f else -1f
    }

    var progress = 0f
    var progressDir = 1f

    // Drag and Drop
    val col3 = floatArrayOf(1f, 0f, 0.2f)
    val col4 = floatArrayOf(0.4f, 0.7f, 0f, 0.5f)

    enum class Mode { Copy, Move, Swap }

    var mode = Mode.Copy

    val names = arrayOf("Bobby", "Beatrice", "Betty", "Brianna", "Barry", "Bernard", "Bibi", "Blaine", "Bryn")


    /* Vertical Sliders */
    var spacing = 4f
    var intValue = 0
    val values1 = floatArrayOf(0f, 0.6f, 0.35f, 0.9f, 0.7f, 0.2f, 0f)
    val values2 = floatArrayOf(0.2f, 0.8f, 0.4f, 0.25f)

    /* Active, Focused, Hovered & Focused Tests */
    var itemType = 1
    var b0 = false
    val col = Vec4(1f, 0.5, 0f, 1f)
    val str = CharArray(16)
    var currentItem1 = 1
    var embedAllInsideAChildWindow = false
    var testWindow = false

    operator fun invoke() {

        if (!collapsingHeader("Widgets"))
            return

        treeNode("Basic") {
            if (button("Button")) clicked++
            if (clicked has 1) {
                sameLine()
                text("Thanks for clicking me!")
            }
            checkbox("checkbox", ::check)

            radioButton("radio a", ::e, 0); sameLine()
            radioButton("radio b", ::e, 1); sameLine()
            radioButton("radio c", ::e, 2)
            // Color buttons, demonstrate using PushID() to add unique identifier in the ID stack, and changing style.
            for (i in 0..6) {
                if (i > 0)
                    sameLine()
                withId(i) {
                    withStyleColor(
                            Col.Button, Color.hsv(i / 7f, 0.6f, 0.6f),
                            Col.ButtonHovered, Color.hsv(i / 7f, 0.7f, 0.7f),
                            Col.ButtonActive, Color.hsv(i / 7f, 0.8f, 0.8f)) {
                        button("Click")
                    }
                }
            }

            // Use AlignTextToFramePadding() to align text baseline to the baseline of framed elements (otherwise a Text+SameLine+Button sequence will have the text a little too high by default)
            alignTextToFramePadding()
            text("Hold to repeat:")
            sameLine()

            // Arrow buttons with Repeater
            val spacing = style.itemInnerSpacing.x
            pushButtonRepeat(true)
            if (arrowButton("##left", Dir.Left)) counter--
            sameLine(0f, spacing)
            if (arrowButton("##right", Dir.Right)) counter++
            popButtonRepeat()
            sameLine()
            text("$counter")

            text("Hover over me")
            if (isItemHovered()) setTooltip("I am a tooltip")
            sameLine()
            text("- or me")
            if (isItemHovered())
                withTooltip {
                    text("I am a fancy tooltip")
                    plotLines("Curve", arr)
                }
            separator()
            labelText("label", "Value")

            run {
                // Using the _simplified_ one-liner Combo() api here
                // See "Combo" section for examples of how to use the more complete BeginCombo()/EndCombo() api.
                val items = listOf("AAAA", "BBBB", "CCCC", "DDDD", "EEEE", "FFFF", "GGGG", "HHHH", "IIII", "JJJJ", "KKKK", "LLLLLLL", "MMMM", "OOOOOOO")
                combo("combo", ::currentItem0, items)
                sameLine(); helpMarker("Refer to the \"Combo\" section below for an explanation of the full BeginCombo/EndCombo API, and demonstration of various flags.\n")
            }

            run {
                inputText("input text", str0)
                sameLine(); helpMarker("USER:\nHold SHIFT or use mouse to select text.\nCTRL+Left/Right to word jump.\nCTRL+A or double-click to select all.\nCTRL+X,CTRL+C,CTRL+V clipboard.\nCTRL+Z,CTRL+Y undo/redo.\nESCAPE to revert.\n\nPROGRAMMER:\nYou can use the InputTextFlag.CallbackResize facility if you need to wire InputText() to a dynamic string type. See misc/cpp/imgui_stl.h for an example (this is not demonstrated in imgui_demo.cpp).")

                inputTextWithHint("input text (w/ hint)", "enter text here", str1) // TODO check if convert str1 to CharArray

                inputInt("input int", ::i0)
                sameLine(); helpMarker("You can apply arithmetic operators +,*,/ on numerical values.\n  e.g. [ 100 ], input \'*2\', result becomes [ 200 ]\nUse +- to subtract.\n")

                inputFloat("input float", ::f0, 0.01f, 1f, "%.3f")

                inputDouble("input double", ::d0, 0.01, 1.0, "%.8f")

                inputFloat("input scientific", ::f1, 0f, 0f, "%e")
                sameLine(); helpMarker("You can input value using the scientific notation,\n  e.g. \"1e+8\" becomes \"100000000\".\n")

                inputFloat3("input float3", vec4a)
            }
            run {
                dragInt("drag int", ::i1, 1f)
                sameLine(); helpMarker("Click and drag to edit value.\nHold SHIFT/ALT for faster/slower edit.\nDouble-click or CTRL+click to input value.")

                dragInt("drag int 0..100", ::i2, 1f, 0, 100, "%d%%")

                dragFloat("drag float", ::f2, 0.005f)
                dragFloat("drag small float", ::f3, 0.0001f, 0f, 0f, "%.06f ns")
            }
            run {
                sliderInt("slider int", ::i3, -1, 3)
                sameLine(); helpMarker("CTRL+click to input value.")

                sliderFloat("slider float", ::f4, 0f, 1f, "ratio = %.3f")
                sliderFloat("slider float (curve)", ::f5, -10f, 10f, "%.4f", 2f)

                sliderAngle("slider angle", ::angle)
            }

            run {
                colorEdit3("color 1", col1)
                sameLine(); helpMarker("Click on the colored square to open a color picker.\nRight-click on the colored square to show options.\nCTRL+click on individual component to input value.\n")

                colorEdit4("color 2", col2)
            }

            run {
                val listboxItems = arrayOf("Apple", "Banana", "Cherry", "Kiwi", "Mango", "Orange", "Pineapple", "Strawberry", "Watermelon")
                listBox("listbox\n(single select)", ::listboxItemCurrent, listboxItems, 4)
            }
        }

        treeNode("Trees") {
            treeNode("Basic trees") {
                for (i in 0..4) treeNode(i, "Child $i") {
                    text("blah blah")
                    sameLine()
                    smallButton("print") { println("Child $i pressed") }
                }
            }

            treeNode("Advanced, with Selectable nodes") {

                helpMarker("This is a more standard looking tree with selectable nodes.\nClick to select, CTRL+Click to toggle, click on arrows or double-click to open.")
                checkbox("Align label with current X position)", ::alignLabelWithCurrentXposition)
                text("Hello!")
                if (alignLabelWithCurrentXposition) unindent(treeNodeToLabelSpacing)

                /*  Temporary storage of what node we have clicked to process selection at the end of the loop.
                    May be a pointer to your own node type, etc.                     */
                var nodeClicked = -1
                // Increase spacing to differentiate leaves from expanded contents.
                withStyleVar(StyleVar.IndentSpacing, fontSize * 3) {
                    for (i in 0..5) {
                        /*  Disable the default open on single-click behavior and pass in Selected flag according
                            to our selection state.                             */
                        var nodeFlags = Tnf.OpenOnArrow or Tnf.OpenOnDoubleClick or
                                if (selectionMask has (1 shl i)) Tnf.Selected else Tnf.None
                        if (i < 3) {    // Node
                            val nodeOpen = treeNodeExV(i, nodeFlags, "Selectable Node $i")
                            if (isItemClicked()) nodeClicked = i
                            if (nodeOpen) {
                                text("Blah blah\nBlah Blah")
                                treePop()
                            }
                        } else {
                            /*  Leaf: The only reason we have a TreeNode at all is to allow selection of the leaf.
                                Otherwise we can use BulletText() or TreeAdvanceToLabelPos()+Text().                                 */
                            nodeFlags = nodeFlags or Tnf.Leaf or Tnf.NoTreePushOnOpen // or Tnf.Bullet
                            treeNodeExV(i, nodeFlags, "Selectable Leaf $i")
                            if (isItemClicked()) nodeClicked = i
                        }
                    }
                    if (nodeClicked != -1) {
                        /*  Update selection state. Process outside of tree loop to avoid visual inconsistencies during
                            the clicking-frame.                         */
                        if (io.keyCtrl)
                            selectionMask = selectionMask xor (1 shl nodeClicked)   // CTRL+click to toggle
                        /*  Depending on selection behavior you want, this commented bit preserve selection when
                            clicking on item that is part of the selection                         */
                        else //if (!(selectionMask & (1 << nodeClicked)))
                            selectionMask = (1 shl nodeClicked) // Click to single-select
                    }
                }
                if (alignLabelWithCurrentXposition) indent(treeNodeToLabelSpacing)
            }
        }

        treeNode("Collapsing Headers") {
            checkbox("Enable extra group", ::closableGroup)
            collapsingHeader("Header") {
                text("IsItemHovered: ${isItemHovered()}")
                for (i in 0..4) text("Some content $i")
            }
            collapsingHeader("Header with a close button", ::closableGroup) {
                text("IsItemHovered: ${isItemHovered()}")
                for (i in 0..4) text("More content $i")
            }
        }

        treeNode("Bullets") {
            bulletText("Bullet point 1")
            bulletText("Bullet point 2\nOn multiple lines")
            bullet(); text("Bullet point 3 (two calls)")
            bullet(); smallButton("Button")
        }

        treeNode("Text") {
            treeNode("Colored Text") {
                // Using shortcut. You can use PushStyleColor()/PopStyleColor() for more flexibility.
                textColored(Vec4(1f, 0f, 1f, 1f), "Pink")
                textColored(Vec4(1f, 1f, 0f, 1f), "Yellow")
                textDisabled("Disabled")
                sameLine(); helpMarker("The TextDisabled color is stored in ImGuiStyle.")
            }
            treeNode("Word Wrapping") {
                // Using shortcut. You can use PushTextWrapPos()/PopTextWrapPos() for more flexibility.
                textWrapped("This text should automatically wrap on the edge of the window. The current implementation " +
                        "for text wrapping follows simple rules suitable for English and possibly other languages.")
                spacing()

                sliderFloat("Wrap width", ::wrapWidth, -20f, 600f, "%.0f")

                text("Test paragraph 1:")
                val pos = cursorScreenPos
                val a = Vec2(pos.x + wrapWidth, pos.y)
                val b = Vec2(pos.x + wrapWidth + 10, pos.y + textLineHeight)
                windowDrawList.addRectFilled(a, b, COL32(255, 0, 255, 255))
                withTextWrapPos(cursorPos.x + wrapWidth) {
                    text("The lazy dog is a good dog. This paragraph is made to fit within %.0f pixels. Testing a 1 character word. The quick brown fox jumps over the lazy dog.", wrapWidth)
                    windowDrawList.addRect(itemRectMin, itemRectMax, COL32(255, 255, 0, 255))
                }

                text("Test paragraph 2:")
                pos put cursorScreenPos
                a.put(pos.x + wrapWidth, pos.y)
                b.put(pos.x + wrapWidth + 10, pos.y + textLineHeight)
                windowDrawList.addRectFilled(a, b, COL32(255, 0, 255, 255))
                withTextWrapPos(cursorPos.x + wrapWidth) {
                    text("aaaaaaaa bbbbbbbb, c cccccccc,dddddddd. d eeeeeeee   ffffffff. gggggggg!hhhhhhhh")
                    windowDrawList.addRect(itemRectMin, itemRectMax, COL32(255, 255, 0, 255))
                }
            }
            treeNode("JVM UTF-16 Unicode with surrogate characters") {
                /*  UTF-8 test with Japanese characters
                    (Needs a suitable font, try Noto, or Arial Unicode, or M+ fonts. Read misc/fonts/README.txt for details.)
                    - From C++11 you can use the u8"my text" syntax to encode literal strings as UTF-8
                    - For earlier compiler, you may be able to encode your sources as UTF-8 (e.g. Visual Studio save your file
                        as 'UTF-8 without signature')
                    - FOR THIS DEMO FILE ONLY, BECAUSE WE WANT TO SUPPORT OLD COMPILERS, WE ARE *NOT* INCLUDING RAW UTF-8 CHARACTERS IN THIS SOURCE FILE.
                        Instead we are encoding a few strings with hexadecimal constants. Don't do this in your application!
                        Please use u8"text in any language" in your application!
                    Note that characters values are preserved even by inputText() if the font cannot be displayed,
                    so you can safely copy & paste garbled characters into another application. */
                textWrapped("CJK text will only appears if the font was loaded with the appropriate CJK character ranges. Call io.font.AddFontFromFileTTF() manually to load extra character ranges. Read misc/fonts/README.txt for details.")
                // Normally we would use u8"blah blah" with the proper characters directly in the string.
                text("Hiragana: \u304b\u304d\u304f\u3051\u3053 (kakikukeko)")
                text("Kanjis: \u65e5\u672c\u8a9e (nihongo)")
                inputText("UTF-16 input", buf)
            }
        }

        treeNode("Images") {
            textWrapped("Below we are displaying the font texture (which is the only texture we have access to in this demo). Use the 'ImTextureID' type as storage to pass pointers or identifier to your own texture data. Hover the texture for a zoomed view!")
            /*  Here we are grabbing the font texture because that's the only one we have access to inside the demo
                code.
                Remember that textureId is just storage for whatever you want it to be, it is essentially a value
                that will be passed to the render function inside the ImDrawCmd structure.
                If you use one of the default imgui_impl_XXXX.cpp renderer, they all have comments at the top of
                their file to specify what they expect to be stored in textureID.
                (for example, the imgui_impl_glfw_gl3.cpp renderer expect a GLuint OpenGL texture identifier etc.)
                If you decided that textureID = MyEngineTexture*, then you can pass your MyEngineTexture* pointers
                to imgui.image(), and gather width/height through your own functions, etc.
                Using showMetricsWindow() as a "debugger" to inspect the draw data that are being passed to your
                render will help you debug issues if you are confused about this.
                Consider using the lower-level drawList.addImage() API, via imgui.windowDrawList.addImage().    */
            val myTexId = io.fonts.texId
            val myTexSize = Vec2(io.fonts.texSize)

            text("%.0fx%.0f", myTexSize.x, myTexSize.y)
            val pos = Vec2(cursorScreenPos)
            image(myTexId, myTexSize, Vec2(), Vec2(1), Vec4.fromColor(255, 255, 255, 255), Vec4.fromColor(255, 255, 255, 128))
            if (isItemHovered())
                withTooltip {
                    val regionSz = 32f
                    val region = io.mousePos - pos - regionSz * 0.5f
                    region.x = if (region.x < 0f) 0f else if (region.x > myTexSize.x - regionSz) myTexSize.x - regionSz else region.x
                    region.y = if (region.y < 0f) 0f else if (region.y > myTexSize.y - regionSz) myTexSize.y - regionSz else region.y
                    val zoom = 4f
                    text("Min: (%.2f, %.2f)", region.x, region.y)
                    text("Max: (%.2f, %.2f)", region.x + regionSz, region.y + regionSz)
                    val uv0 = Vec2(region.x / myTexSize.x, region.y / myTexSize.y)
                    val uv1 = Vec2((region.x + regionSz) / myTexSize.x, (region.y + regionSz) / myTexSize.y)
                    image(myTexId, Vec2(regionSz * zoom), uv0, uv1, Vec4.fromColor(255, 255, 255, 255), Vec4.fromColor(255, 255, 255, 128))
                }
            textWrapped("And now some textured buttons..")
            for (i in 0..7)
                withId(i) {
                    val framePadding = -1 + i  // -1 = uses default padding
                    if (imageButton(myTexId, Vec2(32, 32), Vec2(), 32 / myTexSize, framePadding, Vec4.fromColor(0, 0, 0, 255)))
                        pressedCount++
                    sameLine()
                }
            newLine()
            text("Pressed $pressedCount times.")
        }

        treeNode("Combo") {
            // Expose flags as checkbox for the demo
            checkboxFlags("ComboFlag.PopupAlignLeft", ::flags0, ComboFlag.PopupAlignLeft.i)
            sameLine(); helpMarker("Only makes a difference if the popup is larger than the combo")
            if (checkboxFlags("ComboFlag.NoArrowButton", ::flags0, ComboFlag.NoArrowButton.i))
                flags0 = flags0 wo ComboFlag.NoPreview     // Clear the other flag, as we cannot combine both
            if (checkboxFlags("ComboFlag.NoPreview", ::flags0, ComboFlag.NoPreview.i))
                flags0 = flags0 wo ComboFlag.NoArrowButton // Clear the other flag, as we cannot combine both

            /*  General BeginCombo() API, you have full control over your selection data and display type.
                (your selection data could be an index, a pointer to the object, an id for the object,
                a flag stored in the object itself, etc.)                 */
            val items = listOf("AAAA", "BBBB", "CCCC", "DDDD", "EEEE", "FFFF", "GGGG", "HHHH", "IIII", "JJJJ", "KKKK", "LLLLLLL", "MMMM", "OOOOOOO")
            if (beginCombo("combo 1", items[0], flags0)) { // The second parameter is the label previewed before opening the combo.
                items.forEachIndexed { i, it ->
                    val isSelected = currentItem3 == i
                    if (selectable(it, isSelected))
                        currentItem3 = i
                    if (isSelected)
                    // Set the initial focus when opening the combo (scrolling + for keyboard navigation support in the upcoming navigation branch)
                        setItemDefaultFocus()
                }
                endCombo()
            }

            // Simplified one-liner Combo() API, using values packed in a single constant string
            combo("combo 2 (one-liner)", ::currentItem4, "aaaa\u0000bbbb\u0000cccc\u0000dddd\u0000eeee\u0000\u0000")

            /*  Simplified one-liner Combo() using an array of const char*
                If the selection isn't within 0..count, Combo won't display a preview                 */
            combo("combo 3 (array)", ::currentItem5, items)

            // Simplified one-liner Combo() using an accessor function TODO
//                struct FuncHolder { static bool ItemGetter(void * data, int idx, const char * * out_str) { *out_str = ((const char * *) data)[idx]; return true; } };
//                static int item_current_4 = 0;
//                ImGui::Combo("combo 4 (function)", & item_current_4, &FuncHolder::ItemGetter, items, IM_ARRAYSIZE(items));
        }

        treeNode("Selectables") {
            /*  Selectable() has 2 overloads:
                - The one taking "bool selected" as a read-only selection information. When Selectable() has been
                    clicked is returns true and you can alter selection state accordingly.
                - The one taking "bool* p_selected" as a read-write selection information (convenient in some cases)
                The earlier is more flexible, as in real application your selection may be stored in
                a different manner (in flags within objects, as an external list, etc). */
            treeNode("Basic") {
                selectable("1. I am selectable", selection0, 0)
                selectable("2. I am selectable", selection0, 1)
                text("3. I am not selectable")
                selectable("4. I am selectable", selection0, 2)
                if (selectable("5. I am double clickable", selection0[3], Sf.AllowDoubleClick.i))
                    if (isMouseDoubleClicked(0)) selection0[3] = !selection0[3]
            }
            treeNode("Selection State: Single Selection") {
                for (n in 0..4)
                    if (selectable("Object $n", selected0 == n))
                        selected0 = n
            }
            treeNode("Selection State: Multiple Selection") {
                helpMarker("Hold CTRL and click to select multiple items.")
                for (n in 0..4)
                    if (selectable("Object $n", selection1[n])) {
                        if (!io.keyCtrl)    // Clear selection when CTRL is not held
                            selection1.fill(false)
                        selection0[n] = selection0[n] xor true
                    }
            }
            treeNode("Rendering more text into the same line") {
                // Using the Selectable() override that takes "bool* p_selected" parameter and toggle your booleans automatically.
                selectable("main.c", selected1, 0); sameLine(300); text(" 2,345 bytes")
                selectable("Hello.cpp", selected1, 1); sameLine(300); text("12,345 bytes")
                selectable("Hello.h", selected1, 2); sameLine(300); text(" 2,345 bytes")
            }
            treeNode("In columns") {
                columns(3, "", false)
                for (i in 0..15) {
                    if (selectable("Item $i", selected2, i)) Unit
                    nextColumn()
                }
                columns(1)
            }
            treeNode("Grid") {
                for (i in 0 until 16)
                    withId(i) {
                        if (selectable("Sailor", selected3, i, 0, Vec2(50))) {
                            // Note: We _unnecessarily_ test for both x/y and i here only to silence some static analyzer. The second part of each test is unnecessary.
                            val x = i % 4
                            val y = i / 4
                            // @formatter:off
                            if (x > 0) selected3[i - 1] = selected3[i - 1] xor true
                            if (x < 3 && i < 15) selected3[i + 1] = selected3[i + 1] xor true
                            if (y > 0 && i > 3) selected3[i - 4] = selected3[i - 4] xor true
                            if (y < 3 && i < 12) selected3[i + 4] = selected3[i + 4] xor true
                            // @formatter:on
                        }
                        if ((i % 4) < 3) sameLine()
                    }
            }
            treeNode("Alignment") {
                helpMarker("Alignment applies when a selectable is larger than its text content.\nBy default, Selectables uses style.SelectableTextAlign but it can be overriden on a per-item basis using PushStyleVar().")
                for (y in 0..2)
                    for (x in 0..2) {
                        val alignment = Vec2(x / 2f, y / 2f)
                        val name = "(%.1f,%.1f)".format(alignment.x, alignment.y)
                        if (x > 0) sameLine()
                        pushStyleVar(StyleVar.SelectableTextAlign, alignment)
                        selectable(name, selected4, 3 * y + x, Sf.None.i, Vec2(80))
                        popStyleVar()
                    }
            }
        }

        treeNode("Text Input") {

            treeNode("Multi-line Text Input") {
                /*  Note: we are using a fixed-sized buffer for simplicity here. See ImGuiInputTextFlags_CallbackResize
                    and the code in misc/cpp/imgui_stdlib.h for how to setup InputText() for dynamically resizing strings.  */
                helpMarker("You can use the InputTextFlag.CallbackResize facility if you need to wire InputTextMultiline() to a dynamic string type. See misc/cpp/imgui_stl.h for an example. (This is not demonstrated in imgui_demo.cpp)") // TODO fix bug, some '?' appear at the end of the line
                checkboxFlags("ImGuiInputTextFlags_ReadOnly", ::flags, Itf.ReadOnly.i)
                checkboxFlags("ImGuiInputTextFlags_AllowTabInput", ::flags, Itf.AllowTabInput.i)
                checkboxFlags("ImGuiInputTextFlags_CtrlEnterForNewLine", ::flags, Itf.CtrlEnterForNewLine.i)
                inputTextMultiline("##source", textMultiline, Vec2(-1f, textLineHeight * 16), flags)
            }

            /*if (ImGui::TreeNode("Filtered Text Input"))
            {
                static char buf1[64] = ""; ImGui::InputText("default", buf1, 64);
                static char buf2[64] = ""; ImGui::InputText("decimal", buf2, 64, ImGuiInputTextFlags_CharsDecimal);
                static char buf3[64] = ""; ImGui::InputText("hexadecimal", buf3, 64, ImGuiInputTextFlags_CharsHexadecimal | ImGuiInputTextFlags_CharsUppercase);
                static char buf4[64] = ""; ImGui::InputText("uppercase", buf4, 64, ImGuiInputTextFlags_CharsUppercase);
                static char buf5[64] = ""; ImGui::InputText("no blank", buf5, 64, ImGuiInputTextFlags_CharsNoBlank);
                struct TextFilters { static int FilterImGuiLetters(ImGuiInputTextCallbackData* data) { if (data->EventChar < 256 && strchr("imgui", (char)data->EventChar)) return 0; return 1; } };
                static char buf6[64] = ""; ImGui::InputText("\"imgui\" letters", buf6, 64, ImGuiInputTextFlags_CallbackCharFilter, TextFilters::FilterImGuiLetters);

                ImGui::Text("Password input");
                static char bufpass[64] = "password123";
                ImGui::InputText("password", bufpass, 64, ImGuiInputTextFlags_Password | ImGuiInputTextFlags_CharsNoBlank);
                ImGui::SameLine(); HelpMarker("Display all characters as '*'.\nDisable clipboard cut and copy.\nDisable logging.\n");
                ImGui::InputTextWithHint("password (w/ hint)", "<password>", bufpass, 64, ImGuiInputTextFlags_Password | ImGuiInputTextFlags_CharsNoBlank);
                ImGui::InputText("password (clear)", bufpass, 64, ImGuiInputTextFlags_CharsNoBlank);
                ImGui::TreePop();
            }

            if (ImGui::TreeNode("Resize Callback"))
            {
                // If you have a custom string type you would typically create a ImGui::InputText() wrapper than takes your type as input.
                // See misc/cpp/imgui_stdlib.h and .cpp for an implementation of this using std::string.
                HelpMarker("Demonstrate using ImGuiInputTextFlags_CallbackResize to wire your resizable string type to InputText().\n\nSee misc/cpp/imgui_stdlib.h for an implementation of this for std::string.");
                struct Funcs
                        {
                            static int MyResizeCallback(ImGuiInputTextCallbackData* data)
                            {
                                if (data->EventFlag == ImGuiInputTextFlags_CallbackResize)
                                {
                                    ImVector<char>* my_str = (ImVector<char>*)data->UserData;
                                    IM_ASSERT(my_str->begin() == data->Buf);
                                    my_str->resize(data->BufSize);  // NB: On resizing calls, generally data->BufSize == data->BufTextLen + 1
                                    data->Buf = my_str->begin();
                                }
                                return 0;
                            }

                            // Tip: Because ImGui:: is a namespace you can add your own function into the namespace from your own source files.
                            static bool MyInputTextMultiline(const char* label, ImVector<char>* my_str, const ImVec2& size = ImVec2(0, 0), ImGuiInputTextFlags flags = 0)
                            {
                                IM_ASSERT((flags & ImGuiInputTextFlags_CallbackResize) == 0);
                                return ImGui::InputTextMultiline(label, my_str->begin(), my_str->size(), size, flags | ImGuiInputTextFlags_CallbackResize, Funcs::MyResizeCallback, (void*)my_str);
                            }
                        };

                // For this demo we are using ImVector as a string container.
                // Note that because we need to store a terminating zero character, our size/capacity are 1 more than usually reported by a typical string class.
                static ImVector<char> my_str;
                if (my_str.empty())
                    my_str.push_back(0);
                Funcs::MyInputTextMultiline("##MyStr", &my_str, ImVec2(-1.0f, ImGui::GetTextLineHeight() * 16));
                ImGui::Text("Data: %p\nSize: %d\nCapacity: %d", my_str.begin(), my_str.size(), my_str.capacity());
                ImGui::TreePop();
            }*/
        }

        treeNode("Plots Widgets") {

            checkbox("Animate", ::animate)

            val arr = floatArrayOf(0.6f, 0.1f, 1f, 0.5f, 0.92f, 0.1f, 0.2f)
            plotLines("Frame Times", arr)

            /*  Create a dummy array of contiguous float values to plot
                Tip: If your float aren't contiguous but part of a structure, you can pass a pointer to your first float
                and the sizeof() of your structure in the Stride parameter.
             */
            if (!animate || refreshTime == 0.0) refreshTime = time
            while (refreshTime < time) { // Create dummy data at fixed 60 hz rate for the demo
                values0[valuesOffset] = cos(phase)
                valuesOffset = (valuesOffset + 1) % values0.size
                phase += 0.1f * valuesOffset
                refreshTime += 1f / 60f
            }
            plotLines("Lines", values0, valuesOffset, "avg 0.0", -1f, 1f, Vec2(0, 80))
            plotHistogram("Histogram", arr, 0, "", 0f, 1f, Vec2(0, 80))

            // Use functions to generate output
            // FIXME: This is rather awkward because current plot API only pass in indices. We probably want an API passing floats and user provide sample rate/count.
            separator()
            withItemWidth(100) { combo("func", ::funcType, "Sin\u0000Saw\u0000") }
            sameLine()
            sliderInt("Sample count", ::displayCount, 1, 400)
            val func = if (funcType == 0) Funcs1::sin else Funcs1::saw
            plotLines("Lines", func, displayCount, 0, "", -1f, 1f, Vec2(0, 80))
            plotHistogram("Histogram", func, displayCount, 0, "", -1f, 1f, Vec2(0, 80))
            separator()

            // Animate a simple progress bar
            if (animate) {
                progress += progressDir * 0.4f * io.deltaTime
                if (progress >= 1.1f) {
                    progress = +1.1f
                    progressDir *= -1f
                }
                if (progress <= -0.1f) {
                    progress = -0.1f
                    progressDir *= -1f
                }
            }
            /*  Typically we would use Vec2(-1f , 0f) to use all available width, or Vec2(width, 0f) for a specified width.
                Vec2() uses itemWidth.  */
            progressBar(progress, Vec2())
            sameLine(0f, style.itemInnerSpacing.x)
            text("Progress Bar")

            val progressSaturated = glm.clamp(progress, 0f, 1f)
            progressBar(progress, Vec2(), "${(progressSaturated * 1753).i}/1753")
        }

        treeNode("Color/Picker Widgets") {

            checkbox("With Alpha Preview", ::alphaPreview)
            checkbox("With Half Alpha Preview", ::alphaHalfPreview)
            checkbox("With Drag and Drop", ::dragAndDrop)
            checkbox("With Options Menu", ::optionsMenu); sameLine(); helpMarker("Right-click on the individual color widget to show options.")
            checkbox("With HDR", ::hdr); sameLine(); helpMarker("Currently all this does is to lift the 0..1 limits on dragging widgets.")
            var miscFlags = if (hdr) Cef.HDR.i else 0
            if (dragAndDrop) miscFlags = miscFlags or Cef.NoDragDrop
            if (alphaHalfPreview) miscFlags = miscFlags or Cef.AlphaPreviewHalf
            else if (alphaPreview) miscFlags = miscFlags or Cef.AlphaPreview
            if (!optionsMenu) miscFlags = miscFlags or Cef.NoOptions

            text("Color widget:")
            sameLine(); helpMarker("Click on the colored square to open a color picker.\nCTRL+click on individual component to input value.\n")
            colorEdit3("MyColor##1", color, miscFlags)

            text("Color widget HSV with Alpha:")
            colorEdit4("MyColor##2", color, Cef.DisplayHSV or miscFlags)

            text("Color widget with Float Display:")
            colorEdit4("MyColor##2f", color, Cef.Float or miscFlags)

            text("Color button with Picker:")
            sameLine(); helpMarker("With the ImGuiColorEditFlags_NoInputs flag you can hide all the slider/text inputs.\nWith the ImGuiColorEditFlags_NoLabel flag you can pass a non-empty label which will only be used for the tooltip and picker popup.")
            colorEdit4("MyColor##3", color, Cef.NoInputs or Cef.NoLabel or miscFlags)

            text("Color button with Custom Picker Popup:")
            if (savedPaletteInit)
                savedPalette.forEachIndexed { n, c ->
                    colorConvertHSVtoRGB(n / 31f, 0.8f, 0.8f, c::x, c::y, c::z)
                    savedPalette[n].w = 1f // Alpha
                }
            savedPaletteInit = false
            var openPopup = colorButton("MyColor##3b", color, miscFlags)
            sameLine()
            openPopup = openPopup or button("Palette")
            if (openPopup) {
                openPopup("mypicker")
                backupColor put color
            }
            popup("mypicker") {
                text("MY CUSTOM COLOR PICKER WITH AN AMAZING PALETTE!")
                separator()
                colorPicker4("##picker", color, miscFlags or Cef.NoSidePreview or Cef.NoSmallPreview)
                sameLine()

                withGroup {
                    // Lock X position
                    text("Current")
                    colorButton("##current", color, Cef.NoPicker or Cef.AlphaPreviewHalf, Vec2(60, 40))
                    text("Previous")
                    if (colorButton("##previous", backupColor, Cef.NoPicker or Cef.AlphaPreviewHalf, Vec2(60, 40)))
                        color put backupColor
                    separator()
                    text("Palette")
                    savedPalette.forEachIndexed { n, c ->
                        withId(n) {
                            if ((n % 8) != 0)
                                sameLine(0f, style.itemSpacing.y)
                            if (colorButton("##palette", c, Cef.NoAlpha or Cef.NoPicker or Cef.NoTooltip, Vec2(20, 20)))
                                color.put(c.x, c.y, c.z, color.w) // Preserve alpha!

                            // Allow user to drop colors into each palette entry
                            // (Note that ColorButton is already a drag source by default, unless using ImGuiColorEditFlags_NoDragDrop)
                            if (beginDragDropTarget()) {
                                acceptDragDropPayload(PAYLOAD_TYPE_COLOR_3F)?.let {
                                    for (i in 0..2) savedPalette[n][i] = it.data!!.getFloat(i)
                                }
                                acceptDragDropPayload(PAYLOAD_TYPE_COLOR_4F)?.let {
                                    for (i in 0..3) savedPalette[n][i] = it.data!!.getFloat(i)
                                }
                                endDragDropTarget()
                            }
                        }
                    }
                }
            }
            text("Color button only:")
            colorButton("MyColor##3c", color, miscFlags, Vec2(80, 80))

            text("Color picker:")
            checkbox("With Alpha", ::alpha)
            checkbox("With Alpha Bar", ::alphaBar)
            checkbox("With Side Preview", ::sidePreview)
            if (sidePreview) {
                sameLine()
                checkbox("With Ref Color", ::refColor)
                if (refColor) {
                    sameLine()
                    colorEdit4("##RefColor", refColorV, Cef.NoInputs or miscFlags)
                }
            }
            combo("Display Mode", ::displayMode, "Auto/Current\u0000None\u0000RGB Only\u0000HSV Only\u0000Hex Only\u0000")
            sameLine(); helpMarker("ColorEdit defaults to displaying RGB inputs if you don't specify a display mode, but the user can change it with a right-click.\n\nColorPicker defaults to displaying RGB+HSV+Hex if you don't specify a display mode.\n\nYou can change the defaults using SetColorEditOptions().")
            combo("Picker Mode", ::pickerMode, "Auto/Current\u0000Hue bar + SV rect\u0000Hue wheel + SV triangle\u0000")
            sameLine(); helpMarker("User can right-click the picker to change mode.")
            var flags = miscFlags
            // @formatter:off
            if (!alpha) flags = flags or Cef.NoAlpha // This is by default if you call ColorPicker3() instead of ColorPicker4()
            if (alphaBar) flags = flags or Cef.AlphaBar
            if (!sidePreview) flags = flags or Cef.NoSidePreview
            if (pickerMode == 1) flags = flags or Cef.PickerHueBar
            if (pickerMode == 2) flags = flags or Cef.PickerHueWheel
            if (displayMode == 1) flags = flags or Cef.NoInputs     // Disable all RGB/HSV/Hex displays
            if (displayMode == 2) flags = flags or Cef.DisplayRGB   // Override display mode
            if (displayMode == 3) flags = flags or Cef.DisplayHSV
            if (displayMode == 4) flags = flags or Cef.DisplayHEX
            // @formatter:on
            colorPicker4("MyColor##4", color, flags, refColorV.takeIf { refColor })

            text("Programmatically set defaults:")
            sameLine(); helpMarker("SetColorEditOptions() is designed to allow you to set boot-time default.\nWe don't have Push/Pop functions because you can force options on a per-widget basis if needed, and the user can change non-forced ones with the options menu.\nWe don't have a getter to avoid encouraging you to persistently save values that aren't forward-compatible.")
            if (button("Default: Uint8 + HSV + Hue Bar"))
                setColorEditOptions(Cef.Uint8 or Cef.DisplayHSV or Cef.PickerHueBar)
            if (button("Default: Float + HDR + Hue Wheel"))
                setColorEditOptions(Cef.Float or Cef.HDR or Cef.PickerHueWheel)

            // HSV encoded support (to avoid RGB<>HSV round trips and singularities when S==0 or V==0)
            spacing()
            text("HSV encoded colors")
            sameLine(); helpMarker("By default, colors are given to ColorEdit and ColorPicker in RGB, but ImGuiColorEditFlags_InputHSV allows you to store colors as HSV and pass them to ColorEdit and ColorPicker as HSV. This comes with the added benefit that you can manipulate hue values with the picker even when saturation or value are zero.")
            text("Color widget with InputHSV:")
            colorEdit4("HSV shown as HSV##1", colorStoredAsHsv, Cef.DisplayRGB or Cef.InputHSV or Cef.Float)
            colorEdit4("HSV shown as RGB##1", colorStoredAsHsv, Cef.DisplayHSV or Cef.InputHSV or Cef.Float)
            dragVec4("Raw HSV values", colorStoredAsHsv, 0.01f, 0f, 1f)
        }

        treeNode("Range Widgets") {
            dragFloatRange2("range", ::begin, ::end, 0.25f, 0f, 100f, "Min: %.1f %%", "Max: %.1f %%")
            dragIntRange2("range int (no bounds)", ::beginI, ::endI, 5f, 0, 0, "Min: %d units", "Max: %d units")
        }

        treeNode("Data Types") {
            // The DragScalar/InputScalar/SliderScalar functions allow various data types: signed/unsigned int/long long and float/double
            // To avoid polluting the public API with all possible combinations, we use the ImGuiDataType enum to pass the type,
            // and passing all arguments by address.
            // This is the reason the test code below creates local variables to hold "zero" "one" etc. for each types.
            // Note that the SliderScalar function has a maximum usable range of half the natural type maximum, hence the /2 below. */
            // In practice, if you frequently use a given type that is not covered by the normal API entry points, you can wrap it
            // yourself inside a 1 line function which can take typed argument as value instead of void*, and then pass their address
            // to the generic function. For example:
            //   bool MySliderU64(const char *label, u64* value, u64 min = 0, u64 max = 0, const char* format = "%lld")
            //   {
            //      return SliderScalar(label, ImGuiDataType_U64, value, &min, &max, format);
            //   }
            // Limits (as helper variables that we can take the address of)
            // Note that the SliderScalar function has a maximum usable range of half the natural type maximum, hence the /2 below.
//                const ImS32   s32_zero = 0,   s32_one = 1,   s32_fifty = 50, s32_min = INT_MIN/2,   s32_max = INT_MAX/2,    s32_hi_a = INT_MAX/2 - 100,    s32_hi_b = INT_MAX/2;
//                const ImU32   u32_zero = 0,   u32_one = 1,   u32_fifty = 50, u32_min = 0,           u32_max = UINT_MAX/2,   u32_hi_a = UINT_MAX/2 - 100,   u32_hi_b = UINT_MAX/2;
//                const ImS64   s64_zero = 0,   s64_one = 1,   s64_fifty = 50, s64_min = LLONG_MIN/2, s64_max = LLONG_MAX/2,  s64_hi_a = LLONG_MAX/2 - 100,  s64_hi_b = LLONG_MAX/2;
//                const ImU64   u64_zero = 0,   u64_one = 1,   u64_fifty = 50, u64_min = 0,           u64_max = ULLONG_MAX/2, u64_hi_a = ULLONG_MAX/2 - 100, u64_hi_b = ULLONG_MAX/2;
//                const float   f32_zero = 0.f, f32_one = 1.f, f32_lo_a = -10000000000.0f, f32_hi_a = +10000000000.0f;
//                const double  f64_zero = 0.,  f64_one = 1.,  f64_lo_a = -1000000000000000.0, f64_hi_a = +1000000000000000.0;
//                +
//                // State
//                static ImS32  s32_v = -1;
//                static ImU32  u32_v = (ImU32)-1;
//                static ImS64  s64_v = -1;
//                static ImU64  u64_v = (ImU64)-1;
//                static float  f32_v = 0.123f;
//                static double f64_v = 90000.01234567890123456789;
//                +
//                const float drag_speed = 0.2f;
//                static bool drag_clamp = false;
//                ImGui::Text("Drags:");
//                ImGui::Checkbox("Clamp integers to 0..50", &drag_clamp); ImGui::SameLine(); ShowHelpMarker("As with every widgets in dear imgui, we never modify values unless there is a user interaction.\nYou can override the clamping limits by using CTRL+Click to input a value.");
//                ImGui::DragScalar("drag s8",        ImGuiDataType_S8,     &s8_v,  drag_speed, drag_clamp ? &s8_zero  : NULL, drag_clamp ? &s8_fifty  : NULL);
//                ImGui::DragScalar("drag u8",        ImGuiDataType_U8,     &u8_v,  drag_speed, drag_clamp ? &u8_zero  : NULL, drag_clamp ? &u8_fifty  : NULL, "%u ms");
//                ImGui::DragScalar("drag s16",       ImGuiDataType_S16,    &s16_v, drag_speed, drag_clamp ? &s16_zero : NULL, drag_clamp ? &s16_fifty : NULL);
//                ImGui::DragScalar("drag u16",       ImGuiDataType_U16,    &u16_v, drag_speed, drag_clamp ? &u16_zero : NULL, drag_clamp ? &u16_fifty : NULL, "%u ms");
//                ImGui::DragScalar("drag s32",       ImGuiDataType_S32,    &s32_v, drag_speed, drag_clamp ? &s32_zero : NULL, drag_clamp ? &s32_fifty : NULL);
//                ImGui::DragScalar("drag u32",       ImGuiDataType_U32,    &u32_v, drag_speed, drag_clamp ? &u32_zero : NULL, drag_clamp ? &u32_fifty : NULL, "%u ms");
//                ImGui::DragScalar("drag s64",       ImGuiDataType_S64,    &s64_v, drag_speed, drag_clamp ? &s64_zero : NULL, drag_clamp ? &s64_fifty : NULL);
//                ImGui::DragScalar("drag u64",       ImGuiDataType_U64,    &u64_v, drag_speed, drag_clamp ? &u64_zero : NULL, drag_clamp ? &u64_fifty : NULL);
//                ImGui::DragScalar("drag float",     ImGuiDataType_Float,  &f32_v, 0.005f,  &f32_zero, &f32_one, "%f", 1.0f);
//                ImGui::DragScalar("drag float ^2",  ImGuiDataType_Float,  &f32_v, 0.005f,  &f32_zero, &f32_one, "%f", 2.0f); ImGui::SameLine(); ShowHelpMarker("You can use the 'power' parameter to increase tweaking precision on one side of the range.");
//                ImGui::DragScalar("drag double",    ImGuiDataType_Double, &f64_v, 0.0005f, &f64_zero, NULL,     "%.10f grams", 1.0f);
//                ImGui::DragScalar("drag double ^2", ImGuiDataType_Double, &f64_v, 0.0005f, &f64_zero, &f64_one, "0 < %.10f < 1", 2.0f);
//                +
//                ImGui::Text("Sliders");
//                ImGui::SliderScalar("slider s8 full",     ImGuiDataType_S8,     &s8_v,  &s8_min,   &s8_max,   "%d");
//                ImGui::SliderScalar("slider u8 full",     ImGuiDataType_U8,     &u8_v,  &u8_min,   &u8_max,   "%u");
//                ImGui::SliderScalar("slider s16 full",    ImGuiDataType_S16,    &s16_v, &s16_min,  &s16_max,  "%d");
//                ImGui::SliderScalar("slider u16 full",    ImGuiDataType_U16,    &u16_v, &u16_min,  &u16_max,  "%u");
//                ImGui::SliderScalar("slider s32 low",     ImGuiDataType_S32,    &s32_v, &s32_zero, &s32_fifty,"%d");
//                ImGui::SliderScalar("slider s32 high",    ImGuiDataType_S32,    &s32_v, &s32_hi_a, &s32_hi_b, "%d");
//                ImGui::SliderScalar("slider s32 full",    ImGuiDataType_S32,    &s32_v, &s32_min,  &s32_max,  "%d");
//                ImGui::SliderScalar("slider u32 low",     ImGuiDataType_U32,    &u32_v, &u32_zero, &u32_fifty,"%u");
//                ImGui::SliderScalar("slider u32 high",    ImGuiDataType_U32,    &u32_v, &u32_hi_a, &u32_hi_b, "%u");
//                ImGui::SliderScalar("slider u32 full",    ImGuiDataType_U32,    &u32_v, &u32_min,  &u32_max,  "%u");
//                ImGui::SliderScalar("slider s64 low",     ImGuiDataType_S64,    &s64_v, &s64_zero, &s64_fifty,"%I64d");
//                ImGui::SliderScalar("slider s64 high",    ImGuiDataType_S64,    &s64_v, &s64_hi_a, &s64_hi_b, "%I64d");
//                ImGui::SliderScalar("slider s64 full",    ImGuiDataType_S64,    &s64_v, &s64_min,  &s64_max,  "%I64d");
//                ImGui::SliderScalar("slider u64 low",     ImGuiDataType_U64,    &u64_v, &u64_zero, &u64_fifty,"%I64u ms");
//                ImGui::SliderScalar("slider u64 high",    ImGuiDataType_U64,    &u64_v, &u64_hi_a, &u64_hi_b, "%I64u ms");
//                ImGui::SliderScalar("slider u64 full",    ImGuiDataType_U64,    &u64_v, &u64_min,  &u64_max,  "%I64u ms");
//                ImGui::SliderScalar("slider float low",   ImGuiDataType_Float,  &f32_v, &f32_zero, &f32_one);
//                ImGui::SliderScalar("slider float low^2", ImGuiDataType_Float,  &f32_v, &f32_zero, &f32_one,  "%.10f", 2.0f);
//                ImGui::SliderScalar("slider float high",  ImGuiDataType_Float,  &f32_v, &f32_lo_a, &f32_hi_a, "%e");
//                ImGui::SliderScalar("slider double low",  ImGuiDataType_Double, &f64_v, &f64_zero, &f64_one,  "%.10f grams", 1.0f);
//                ImGui::SliderScalar("slider double low^2",ImGuiDataType_Double, &f64_v, &f64_zero, &f64_one,  "%.10f", 2.0f);
//                ImGui::SliderScalar("slider double high", ImGuiDataType_Double, &f64_v, &f64_lo_a, &f64_hi_a, "%e grams", 1.0f);
//                +
//                static bool inputs_step = true;
//                ImGui::Text("Inputs");
//                ImGui::Checkbox("Show step buttons", &inputs_step);
//                ImGui::InputScalar("input s8",      ImGuiDataType_S8,     &s8_v,  inputs_step ? &s8_one  : NULL, NULL, "%d");
//                ImGui::InputScalar("input u8",      ImGuiDataType_U8,     &u8_v,  inputs_step ? &u8_one  : NULL, NULL, "%u");
//                ImGui::InputScalar("input s16",     ImGuiDataType_S16,    &s16_v, inputs_step ? &s16_one : NULL, NULL, "%d");
//                ImGui::InputScalar("input u16",     ImGuiDataType_U16,    &u16_v, inputs_step ? &u16_one : NULL, NULL, "%u");
//                ImGui::InputScalar("input s32",     ImGuiDataType_S32,    &s32_v, inputs_step ? &s32_one : NULL, NULL, "%d");
//                ImGui::InputScalar("input s32 hex", ImGuiDataType_S32,    &s32_v, inputs_step ? &s32_one : NULL, NULL, "%08X", ImGuiInputTextFlags_CharsHexadecimal);
//                ImGui::InputScalar("input u32",     ImGuiDataType_U32,    &u32_v, inputs_step ? &u32_one : NULL, NULL, "%u");
//                ImGui::InputScalar("input u32 hex", ImGuiDataType_U32,    &u32_v, inputs_step ? &u32_one : NULL, NULL, "%08X", ImGuiInputTextFlags_CharsHexadecimal);
//                ImGui::InputScalar("input s64",     ImGuiDataType_S64,    &s64_v, inputs_step ? &s64_one : NULL);
//                ImGui::InputScalar("input u64",     ImGuiDataType_U64,    &u64_v, inputs_step ? &u64_one : NULL);
//                ImGui::InputScalar("input float",   ImGuiDataType_Float,  &f32_v, inputs_step ? &f32_one : NULL);
//                ImGui::InputScalar("input double",  ImGuiDataType_Double, &f64_v, inputs_step ? &f64_one : NULL);
        }

        treeNode("Multi-component Widgets") {

            inputFloat2("input float2", vec4f)
            dragFloat2("drag float2", vec4f, 0.01f, 0f, 1f)
            sliderFloat2("slider float2", vec4f, 0f, 1f)
            inputInt2("input int2", vec4i)
            dragInt2("drag int2", vec4i, 1f, 0, 255)
            sliderInt2("slider int2", vec4i, 0, 255)
            spacing()

            inputFloat3("input float3", vec4f)
            dragFloat3("drag float3", vec4f, 0.01f, 0.0f, 1.0f)
            sliderFloat3("slider float3", vec4f, 0.0f, 1.0f)
            inputInt3("input int3", vec4i)
            dragInt3("drag int3", vec4i, 1f, 0, 255)
            sliderInt3("slider int3", vec4i, 0, 255)
            spacing()

            inputFloat4("input float4", vec4f)
            dragFloat4("drag float4", vec4f, 0.01f, 0.0f, 1.0f)
            sliderFloat4("slider float4", vec4f, 0.0f, 1.0f)
            inputInt4("input int4", vec4i)
            dragInt4("drag int4", vec4i, 1f, 0, 255)
            sliderInt4("slider int4", vec4i, 0, 255)
        }

        treeNode("Vertical Sliders") {

            withStyleVar(StyleVar.ItemSpacing, Vec2(spacing)) {

                vSliderInt("##int", Vec2(18, 160), ::intValue, 0, 5)
                sameLine()

                withId("set1") {
                    for (i in 0..6) {
                        if (i > 0) sameLine()
                        withId(i) {
                            withStyleColor(
                                    Col.FrameBg, Color.hsv(i / 7f, 0.5f, 0.5f),
                                    Col.FrameBgHovered, Color.hsv(i / 7f, 0.6f, 0.5f),
                                    Col.FrameBgActive, Color.hsv(i / 7f, 0.7f, 0.5f),
                                    Col.SliderGrab, Color.hsv(i / 7f, 0.9f, 0.9f)) {

                                withFloat(values1, i) { vSliderFloat("##v", Vec2(18, 160), it, 0f, 1f, "") }
                                if (isItemActive || isItemHovered()) setTooltip("%.3f", values1[i])
                            }
                        }
                    }
                }

                sameLine()
                withId("set2") {
                    val rows = 3
                    val smallSliderSize = Vec2(18, (160f - (rows - 1) * spacing) / rows)
                    for (nx in 0..3) {
                        if (nx > 0) sameLine()
                        withGroup {
                            for (ny in 0 until rows) {
                                withId(nx * rows + ny) {
                                    withFloat(values2, nx) { f ->
                                        vSliderFloat("##v", smallSliderSize, f, 0f, 1f, "")
                                    }
                                    if (isItemActive || isItemHovered())
                                        setTooltip("%.3f", values2[nx])
                                }
                            }
                        }
                    }
                }

                sameLine()
                withId("set3") {
                    for (i in 0..3) {
                        if (i > 0) sameLine()
                        withId(i) {
                            withStyleVar(StyleVar.GrabMinSize, 40f) {
                                withFloat(values1, i) {
                                    vSliderFloat("##v", Vec2(40, 160), it, 0f, 1f, "%.2f\nsec")
                                }
                            }
                        }
                    }
                }
            }
        }

        treeNode("Drag and Drop") {
            run {
                /*  ColorEdit widgets automatically act as drag source and drag target.
                    They are using standardized payload strings IMGUI_PAYLOAD_TYPE_COLOR_3F and IMGUI_PAYLOAD_TYPE_COLOR_4F to allow your own widgets
                    to use colors in their drag and drop interaction. Also see the demo in Color Picker -> Palette demo. */
                bulletText("Drag and drop in standard widgets")
                indent()
                colorEdit3("color 1", col3)
                colorEdit4("color 2", col4)
                unindent()
            }

            run {
                bulletText("Drag and drop to copy/swap items")
                indent()
                if (radioButton("Copy", mode == Mode.Copy))
                    mode = Mode.Copy
                sameLine()
                if (radioButton("Move", mode == Mode.Move))
                    mode = Mode.Move
                sameLine()
                if (radioButton("Swap", mode == Mode.Swap))
                    mode = Mode.Swap
                names.forEachIndexed { n, name ->
                    pushId(n)
                    if ((n % 3) != 0) sameLine()
                    button(name, Vec2(60))

                    // Our buttons are both drag sources and drag targets here!
                    if (beginDragDropSource(DragDropFlag.None)) {
                        setDragDropPayload("DND_DEMO_CELL", n, Int.BYTES)        // Set payload to carry the index of our item (could be anything)
                        when (mode) {
                            // Display preview (could be anything, e.g. when dragging an image we could decide to display the filename and a small preview of the image, etc.)
                            Mode.Copy -> text("Copy $name")
                            Mode.Move -> text("Move $name")
                            Mode.Swap -> text("Swap $name")
                        }
                        endDragDropSource()
                    }
                    if (beginDragDropTarget()) {
                        acceptDragDropPayload("DND_DEMO_CELL")?.let { payload ->
                            assert(payload.dataSize == Int.BYTES)
                            val payloadN = payload.data!!.getInt(0)
                            when (mode) {
                                Mode.Copy -> names[n] = names[payloadN]
                                Mode.Move -> {
                                    names[n] = names[payloadN]
                                    names[payloadN] = ""
                                }
                                Mode.Swap -> {
                                    val tmp = names[n]
                                    names[n] = names[payloadN]
                                    names[payloadN] = tmp
                                }
                            }
                        }
                        endDragDropTarget()
                    }
                    popId()
                }
                unindent()
            }
        }

        treeNode("Querying Status (Active/Focused/Hovered etc.)") {
            /*  Display the value of IsItemHovered() and other common item state functions. Note that the flags can be combined.
                (because BulletText is an item itself and that would affect the output of ::isItemHovered
                we pass all state in a single call to simplify the code).   */
            radioButton("Text", ::itemType, 0)
            radioButton("Button", ::itemType, 1)
            radioButton("Checkbox", ::itemType, 2)
            radioButton("SliderFloat", ::itemType, 3)
            radioButton("InputText", ::itemType, 4)
            radioButton("ColorEdit4", ::itemType, 5)
            radioButton("ListBox", ::itemType, 6)
            separator()
            val ret = when (itemType) {
                0 -> false.also { text("ITEM: Text") }   // Testing text items with no identifier/interaction
                1 -> button("ITEM: Button")   // Testing button
                2 -> checkbox("ITEM: Checkbox", ::b0)  // Testing checkbox
                3 -> sliderVec4("ITEM: SliderFloat", col, 0f, 1f)   // Testing basic item
                4 -> inputText("ITEM: InputText", str)  // Testing input text (which handles tabbing)
                5 -> colorEdit4("ITEM: ColorEdit4", col)    // Testing multi-component items (IsItemXXX flags are reported merged)
                6 -> listBox("ITEM: ListBox", ::currentItem1, arrayOf("Apple", "Banana", "Cherry", "Kiwi"))
                else -> false
            }
            bulletText("Return value = $ret\n" +
                    "isItemFocused = $isItemFocused\n" +
                    "isItemHovered() = ${isItemHovered()}\n" +
                    "isItemHovered(AllowWhenBlockedByPopup) = ${isItemHovered(HoveredFlag.AllowWhenBlockedByPopup)}\n" +
                    "isItemHovered(AllowWhenBlockedByActiveItem) = ${isItemHovered(HoveredFlag.AllowWhenBlockedByActiveItem)}\n" +
                    "isItemHovered(AllowWhenOverlapped) = ${isItemHovered(HoveredFlag.AllowWhenOverlapped)}\n" +
                    "isItemHovered(RectOnly) = ${isItemHovered(HoveredFlag.RectOnly)}\n" +
                    "isItemActive = $isItemActive\n" +
                    "isItemEdited = $isItemEdited\n" +
                    "isItemActivated = $isItemActivated\n" +
                    "isItemDeactivated = $isItemDeactivated\n" +
                    "isItemDeactivatedAfterEdit = $isItemDeactivatedAfterEdit\n" +
                    "isItemVisible = $isItemVisible\n" +
                    "GetItemRectMin() = (%.1f, %.1f)\n" +
                    "GetItemRectMax() = (%.1f, %.1f)\n" +
                    "GetItemRectSize() = (%.1f, %.1f)", itemRectMin.x, itemRectMin.y, itemRectMax.x, itemRectMax.y, itemRectSize.x, itemRectSize.y)

            checkbox("Embed everything inside a child window (for additional testing)", ::embedAllInsideAChildWindow)
            if (embedAllInsideAChildWindow)
                beginChild("outer_child", Vec2(0, fontSize * 20), true)

            // Testing IsWindowFocused() function with its various flags. Note that the flags can be combined.
            bulletText(
                    "isWindowFocused() = ${isWindowFocused()}\n" +
                            "isWindowFocused(ChildWindows) = ${isWindowFocused(FocusedFlag.ChildWindows)}\n" +
                            "isWindowFocused(ChildWindows | RootWindow) = ${isWindowFocused(FocusedFlag.ChildWindows or FocusedFlag.RootWindow)}\n" +
                            "isWindowFocused(RootWindow) = ${isWindowFocused(FocusedFlag.RootWindow)}\n" +
                            "isWindowFocused(AnyWindow) = ${isWindowFocused(FocusedFlag.AnyWindow)}\n")

            // Testing IsWindowHovered() function with its various flags. Note that the flags can be combined.
            bulletText(
                    "isWindowHovered() = ${isWindowHovered()}\n" +
                            "isWindowHovered(AllowWhenBlockedByPopup) = ${isWindowHovered(HoveredFlag.AllowWhenBlockedByPopup)}\n" +
                            "isWindowHovered(AllowWhenBlockedByActiveItem) = ${isWindowHovered(HoveredFlag.AllowWhenBlockedByActiveItem)}\n" +
                            "isWindowHovered(ChildWindows) = ${isWindowHovered(HoveredFlag.ChildWindows)}\n" +
                            "isWindowHovered(ChildWindows | RootWindow) = ${isWindowHovered(HoveredFlag.ChildWindows or HoveredFlag.RootWindow)}\n" +
                            "isWindowHovered(RootWindow) = ${isWindowHovered(HoveredFlag.RootWindow)}\n" +
                            "isWindowHovered(AnyWindow) = ${isWindowHovered(HoveredFlag.AnyWindow)}\n")

            beginChild("child", Vec2(0, 50), true)
            text("This is another child window for testing the _ChildWindows flag.")
            endChild()
            if (embedAllInsideAChildWindow)
                endChild()

            /*  Calling IsItemHovered() after begin returns the hovered status of the title bar.
                This is useful in particular if you want to create a context menu (with BeginPopupContextItem)
                associated to the title bar of a window.                 */
            checkbox("Hovered/Active tests after Begin() for title bar testing", ::testWindow)
            if (testWindow) {
                begin_("Title bar Hovered/Active tests", ::testWindow)
                if (beginPopupContextItem()) { // <-- This is using IsItemHovered()
                    if (menuItem("Close")) testWindow = false
                    endPopup()
                }
                text(
                        "IsItemHovered() after begin = ${isItemHovered()} (== is title bar hovered)\n" +
                                "IsItemActive() after begin = $isItemActive (== is window being clicked/moved)")
                end()
            }
        }
    }
}

inline fun <R> withFloat(block: (KMutableProperty0<Float>) -> R): R {
    Ref.fPtr++
    return block(Ref::float).also { Ref.fPtr-- }
}

inline fun <R> withFloat(floats: FloatArray, ptr: Int, block: (KMutableProperty0<Float>) -> R): R {
    Ref.fPtr++
    val f = Ref::float
    f.set(floats[ptr])
    val res = block(f)
    floats[ptr] = f()
    Ref.fPtr--
    return res
}