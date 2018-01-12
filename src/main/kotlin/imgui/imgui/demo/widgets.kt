package imgui.imgui.demo

import gli_.has
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import glm_.vec2.operators.div
import glm_.vec4.Vec4
import imgui.*
import imgui.Context.style
import imgui.ImGui.acceptDragDropPayload
import imgui.ImGui.beginDragDropTarget
import imgui.ImGui.bullet
import imgui.ImGui.bulletText
import imgui.ImGui.button
import imgui.ImGui.checkbox
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
import imgui.ImGui.endDragDropTarget
import imgui.ImGui.fontSize
import imgui.ImGui.image
import imgui.ImGui.imageButton
import imgui.ImGui.indent
import imgui.ImGui.inputFloat
import imgui.ImGui.inputFloat2
import imgui.ImGui.inputFloat3
import imgui.ImGui.inputFloat4
import imgui.ImGui.inputInt
import imgui.ImGui.inputInt2
import imgui.ImGui.inputInt3
import imgui.ImGui.inputInt4
import imgui.ImGui.inputText
import imgui.ImGui.isItemActive
import imgui.ImGui.isItemClicked
import imgui.ImGui.isItemHovered
import imgui.ImGui.isMouseDoubleClicked
import imgui.ImGui.itemRectMax
import imgui.ImGui.itemRectMin
import imgui.ImGui.labelText
import imgui.ImGui.listBox
import imgui.ImGui.mousePos
import imgui.ImGui.newLine
import imgui.ImGui.nextColumn
import imgui.ImGui.openPopup
import imgui.ImGui.plotHistogram
import imgui.ImGui.plotLines
import imgui.ImGui.progressBar
import imgui.ImGui.radioButton
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.separator
import imgui.ImGui.setColorEditOptions
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
import imgui.ImGui.smallButton
import imgui.ImGui.spacing
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
import imgui.functionalProgramming.button
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
import imgui.imgui.imgui_demoDebugInfo.Companion.showHelpMarker
import imgui.or
import kotlin.math.cos
import kotlin.reflect.KMutableProperty0
import imgui.ColorEditFlags as Cef
import imgui.Context as g
import imgui.InputTextFlags as Itf
import imgui.SelectableFlags as Sf
import imgui.TreeNodeFlags as Tnf
import imgui.WindowFlags as Wf

object widgets {

    /* Basic */
    var clicked = 0
    var check = true
    var e = 0
    val arr = floatArrayOf(0.6f, 0.1f, 1f, 0.5f, 0.92f, 0.1f, 0.2f)
    var item = 1
    var item2 = -1
    var str0 = "Hello, world!".toCharArray()
    var i0 = 123
    var f0 = 0.001f
    val vec4a = floatArrayOf(0.1f, 0.2f, 0.3f, 0.44f)
    var i1 = 50
    var i2 = 42
    var f1 = 1f
    var f2 = 0.0067f
    var i3 = 0
    var f3 = 0.123f
    var f4 = 0f
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


    /* Selectables */
    val selected0 = booleanArrayOf(false, true, false, false)
    val selected1 = BooleanArray(3)
    val selected2 = BooleanArray(16)
    val selected3 = booleanArrayOf(true, false, false, false, false, true, false, false, false, false, true, false, false, false, false, true)


    /* Multi-line Text Input */
    val textMultiline = CharArray(1024 * 16).also {
        ("""/*
                The Pentium F00F bug, shorthand for F0 0F C7 C8,
                the hexadecimal encoding of one offending instruction,
                     more formally, the invalid operand with locked CMPXCHG8B
                     instruction bug, is a design flaw in the majority of
                     Intel Pentium, Pentium MMX, and Pentium OverDrive
                     processors (all in the P5 microarchitecture).
                    */

                    label""".trimMargin() +
                "\tlock cmpxchg8b eax\n").toCharArray(it)
    }
    var readOnly = false


    /* Color/Picker Widgets */
    val color = Vec4.fromColor(114, 144, 154, 200)
    var hdr = false
    var alphaPreview = true
    var alphaHalfPreview = false
    var optionsMenu = true
    // Generate a dummy palette
    var savedPaletteInited = false
    var savedPalette = Array(32, { Vec4() })
    var backupColor = Vec4()
    var alpha = true
    var alphaBar = true
    var sidePreview = true
    var refColor = false
    var refColorV = Vec4(1f, 0f, 1f, 0.5f)
    var inputsMode = 2
    var pickerMode = 0


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
    var refreshTime = 0f
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


    /* Vertical Sliders */
    var spacing = 4f
    var intValue = 0
    val values1 = floatArrayOf(0f, 0.6f, 0.35f, 0.9f, 0.7f, 0.2f, 0f)
    val values2 = floatArrayOf(0.2f, 0.8f, 0.4f, 0.25f)


    operator fun invoke() {

        collapsingHeader("Widgets") {

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
                    if (i > 0) sameLine()
                    withId(i) {
                        withStyleColor(
                                Col.Button, Color.hsv(i / 7f, 0.6f, 0.6f),
                                Col.ButtonHovered, Color.hsv(i / 7f, 0.7f, 0.7f),
                                Col.ButtonActive, Color.hsv(i / 7f, 0.8f, 0.8f)) {
                            button("Click")
                        }
                    }
                }

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
                // Combo using values packed in a single constant string (for really quick combo)
                combo("combo", ::item, "aaaa\u0000bbbb\u0000cccc\u0000dddd\u0000eeee\u0000\u0000")
                val items = arrayOf("AAAA", "BBBB", "CCCC", "DDDD", "EEEE", "FFFF", "GGGG", "HHHH", "IIII", "JJJJ", "KKKK", "LLLLLLL", "MMMM", "OOOOOOO", "PPPP", "QQQQQQQQQQ", "RRR", "SSSS")
                // Combo using proper array. You can also pass a callback to retrieve array value, no need to create/copy an array just for that.
                combo("combo scroll", ::item2, items)

                run {
                    inputText("input text", str0, str0.size)
                    sameLine(); showHelpMarker("Hold SHIFT or use mouse to select text.\nCTRL+Left/Right to word jump.\nCTRL+A or double-click to select all.\nCTRL+X,CTRL+C,CTRL+V clipboard.\nCTRL+Z,CTRL+Y undo/redo.\nESCAPE to revert.\n")

                    inputInt("input int", ::i0)
                    sameLine(); showHelpMarker("You can apply arithmetic operators +,*,/ on numerical values.\n  e.g. [ 100 ], input \'*2\', result becomes [ 200 ]\nUse +- to subtract.\n")

                    inputFloat("input float", ::f0, 0.01f, 1f)

                    inputFloat3("input float3", vec4a)
                }
                run {
                    dragInt("drag int", ::i1, 1f)
                    sameLine(); showHelpMarker("Click and drag to edit value.\nHold SHIFT/ALT for faster/slower edit.\nDouble-click or CTRL+click to input value.")

                    dragInt("drag int 0..100", ::i2, 1f, 0, 100, "%.0f%%")

                    dragFloat("drag float", ::f1, 0.005f)
                    dragFloat("drag small float", ::f2, 0.0001f, 0f, 0f, "%.06f ns")
                }
                run {
                    sliderInt("slider int", ::i3, -1, 3)
                    sameLine(); showHelpMarker("CTRL+click to input value.")

                    sliderFloat("slider float", ::f3, 0f, 1f, "ratio = %.3f")
                    sliderFloat("slider log float", ::f4, -10f, 10f, "%.4f", 3f)

                    sliderAngle("slider angle", ::angle)
                }

                colorEdit3("color 1", col1)
                sameLine(); showHelpMarker("Click on the colored square to open a color picker.\nRight-click on the colored square to show options.\nCTRL+click on individual component to input value.\n")

                colorEdit4("color 2", col2)

                val listboxItems = arrayOf("Apple", "Banana", "Cherry", "Kiwi", "Mango", "Orange", "Pineapple", "Strawberry", "Watermelon")
                listBox("listbox\n(single select)", ::listboxItemCurrent, listboxItems, 4)
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

                    showHelpMarker("This is a more standard looking tree with selectable nodes.\nClick to select, CTRL+Click to toggle, click on arrows or double-click to open.")
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
                                    if (selectionMask has (1 shl i)) Tnf.Selected else Tnf.Null
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
                                nodeFlags = nodeFlags or Tnf.Leaf or Tnf.NoTreePushOnOpen // ImGuiTreeNodeFlags_Bullet
                                treeNodeExV(i, nodeFlags, "Selectable Leaf $i")
                                if (isItemClicked()) nodeClicked = i
                            }
                        }
                        if (nodeClicked != -1) {
                            /*  Update selection state. Process outside of tree loop to avoid visual inconsistencies during
                                the clicking-frame.                         */
                            if (IO.keyCtrl)
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
                    textColored(Vec4(1.0f, 0.0f, 1.0f, 1.0f), "Pink")
                    textColored(Vec4(1.0f, 1.0f, 0.0f, 1.0f), "Yellow")
                    textDisabled("Disabled")
                    sameLine(); showHelpMarker("The TextDisabled color is stored in ImGuiStyle.")
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
                        (needs a suitable font, try Arial Unicode or M+ fonts http://mplus-fonts.sourceforge.jp/mplus-outline-fonts/index-en.html)
                        - From C++11 you can use the u8"my text" syntax to encode literal strings as UTF-8
                        - For earlier compiler, you may be able to encode your sources as UTF-8 (e.g. Visual Studio save your file
                            as 'UTF-8 without signature')
                        - HOWEVER, FOR THIS DEMO FILE, BECAUSE WE WANT TO SUPPORT COMPILER, WE ARE *NOT* INCLUDING RAW UTF-8 CHARACTERS
                            IN THIS SOURCE FILE.
                        Instead we are encoding a few string with hexadecimal constants. Don't do this in your application!
                        Note that characters values are preserved even by inputText() if the font cannot be displayed,
                        so you can safely copy & paste garbled characters into another application. */
                    textWrapped("CJK text will only appears if the font was loaded with the appropriate CJK character ranges. Call IO.font.loadFromFileTTF() manually to load extra character ranges.")
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
                val myTexId = IO.fonts.texId
                val myTexSize = Vec2(IO.fonts.texSize)

                text("%.0fx%.0f", myTexSize.x, myTexSize.y)
                val pos = Vec2(cursorScreenPos)
                image(myTexId, myTexSize, Vec2(), Vec2(1), Vec4.fromColor(255, 255, 255, 255), Vec4.fromColor(255, 255, 255, 128))
                if (isItemHovered())
                    withTooltip {
                        val focusSz = 32f
                        val focus = glm.clamp(mousePos - pos - focusSz * 0.5f, Vec2(), myTexSize - focusSz)
                        text("Min: (%.2f, %.2f)", focus.x, focus.y)
                        text("Max: (%.2f, %.2f)", focus.x + focusSz, focus.y + focusSz)
                        val uv0 = focus / myTexSize
                        val uv1 = (focus + focusSz) / myTexSize
                        image(myTexId, Vec2(128), uv0, uv1, Vec4.fromColor(255, 255, 255, 255), Vec4.fromColor(255, 255, 255, 128))
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

            treeNode("Selectables") {
                treeNode("Basic") {
                    selectable("1. I am selectable", selected0, 0)
                    selectable("2. I am selectable", selected0, 1)
                    text("3. I am not selectable")
                    selectable("4. I am selectable", selected0, 2)
                    if (selectable("5. I am double clickable", selected0[3], Sf.AllowDoubleClick.i))
                        if (isMouseDoubleClicked(0)) selected0[3] = !selected0[3]
                }
                treeNode("Rendering more text into the same block") {
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
                    for (i in 0..15)
                        withId(i) {
                            if (selectable("Sailor", selected3, i, 0, Vec2(50))) {
                                val x = i % 4
                                val y = i / 4
                                if (x > 0) selected3[i - 1] = selected3[i - 1] xor true
                                if (x < 3) selected3[i + 1] = selected3[i + 1] xor true
                                if (y > 0) selected3[i - 4] = selected3[i - 4] xor true
                                if (y < 3) selected3[i + 4] = selected3[i + 4] xor true
                            }
                            if ((i % 4) < 3) sameLine()
                        }
                }
            }

            treeNode("Filtered Text Input TODO") {
                //                inputText("default", buf1)
//                inputText("decimal", buf2, Itf.CharsDecimal.i)
//                inputText("hexadecimal", buf3, Itf.CharsHexadecimal or Itf.CharsUppercase)
//                static char buf4[64] = ""; ImGui::InputText("uppercase", buf4, 64, ImGuiInputTextFlags_CharsUppercase);
//                static char buf5[64] = ""; ImGui::InputText("no blank", buf5, 64, ImGuiInputTextFlags_CharsNoBlank);
//                struct TextFilters { static int FilterImGuiLetters(ImGuiTextEditCallbackData* data) { if (data->EventChar < 256 && strchr("imgui", (char)data->EventChar)) return 0; return 1; } };
//                static char buf6[64] = ""; ImGui::InputText("\"imgui\" letters", buf6, 64, ImGuiInputTextFlags_CallbackCharFilter, TextFilters::FilterImGuiLetters);
//
//                ImGui::Text("Password input");
//                static char bufpass[64] = "password123";
//                ImGui::InputText("password", bufpass, 64, ImGuiInputTextFlags_Password | ImGuiInputTextFlags_CharsNoBlank);
//                ImGui::SameLine(); ShowHelpMarker("Display all characters as '*'.\nDisable clipboard cut and copy.\nDisable logging.\n");
//                ImGui::InputText("password (clear)", bufpass, 64, ImGuiInputTextFlags_CharsNoBlank);
            }

            treeNode("Multi-line Text Input TODO") {
                withStyleVar(StyleVar.FramePadding, Vec2()) {
                    checkbox("Read-only", ::readOnly)
                }
                val flags = Itf.AllowTabInput or if (readOnly) Itf.ReadOnly else Itf.Null
//                inputTextMultiline("##source", textMultiline, Vec2(-1f, textLineHeight * 16), flags)
            }

            treeNode("Plots widgets") {

                checkbox("Animate", ::animate)

                val arr = floatArrayOf(0.6f, 0.1f, 1f, 0.5f, 0.92f, 0.1f, 0.2f)
                plotLines("Frame Times", arr)

                /*  Create a dummy array of contiguous float values to plot
                    Tip: If your float aren't contiguous but part of a structure, you can pass a pointer to your first float
                    and the sizeof() of your structure in the Stride parameter.
                 */
                if (!animate || refreshTime == 0f) refreshTime = time
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
                    progress += progressDir * 0.4f * IO.deltaTime
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

                checkbox("With HDR", ::hdr); sameLine(); showHelpMarker("Currently all this does is to lift the 0..1 limits on dragging widgets.")
                checkbox("With Alpha Preview", ::alphaPreview)
                checkbox("With Half Alpha Preview", ::alphaHalfPreview)
                checkbox("With Options Menu", ::optionsMenu); sameLine(); showHelpMarker("Right-click on the individual color widget to show options.")
                var miscFlags = if (hdr) Cef.HDR.i else 0
                if (alphaHalfPreview) miscFlags = miscFlags or Cef.AlphaPreviewHalf
                if (alphaPreview) miscFlags = miscFlags or Cef.AlphaPreview
                if (!optionsMenu) miscFlags = miscFlags or Cef.NoOptions

                text("Color widget:")
                sameLine(); showHelpMarker("Click on the colored square to open a color picker.\nCTRL+click on individual component to input value.\n")
                colorEdit3("MyColor##1", color, miscFlags)

                text("Color widget HSV with Alpha:")
                colorEdit4("MyColor##2", color, Cef.HSV or miscFlags)

                text("Color widget with Float Display:")
                colorEdit4("MyColor##2f", color, Cef.Float or miscFlags)

                text("Color button with Picker:")
                sameLine(); showHelpMarker("With the ImGuiColorEditFlags_NoInputs flag you can hide all the slider/text inputs.\nWith the ImGuiColorEditFlags_NoLabel flag you can pass a non-empty label which will only be used for the tooltip and picker popup.")
                colorEdit4("MyColor##3", color, Cef.NoInputs or Cef.NoLabel or miscFlags)

                text("Color button with Custom Picker Popup:")
                if (!savedPaletteInited)
                    savedPalette.forEachIndexed { n, c ->
                        colorConvertHSVtoRGB(n / 31f, 0.8f, 0.8f, c::x, c::y, c::z)
                        savedPalette[n].w = 1f // Alpha
                    }
                savedPaletteInited = true
                var openPopup = colorButton("MyColor##3b", color, miscFlags)
                sameLine()
                openPopup = openPopup or button("Palette")
                if (openPopup) {
                    openPopup("mypicker")
                    backupColor put color
                }
                popup("mypicker") {
                    // FIXME: Adding a drag and drop example here would be perfect!
                    text("MY CUSTOM COLOR PICKER WITH AN AMAZING PALETTE!")
                    separator()
                    colorPicker4("##picker", color, miscFlags or Cef.NoSidePreview or Cef.NoSmallPreview)
                    sameLine()
                    withGroup {
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

                                if (beginDragDropTarget()) {
                                    acceptDragDropPayload (PAYLOAD_TYPE_COLOR_3F)?.let {
                                        for(i in 0..2) savedPalette [n][i] = (it.data as Vec4)[i]
                                    }
                                    acceptDragDropPayload (PAYLOAD_TYPE_COLOR_4F)?.let {
                                        for(i in 0..3) savedPalette [n][i] = (it.data as Vec4)[i]
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
                combo("Inputs Mode", ::inputsMode, "All Inputs\u0000No Inputs\u0000RGB Input\u0000HSV Input\u0000HEX Input\u0000")
                combo("Picker Mode", ::pickerMode, "Auto/Current\u0000Hue bar + SV rect\u0000Hue wheel + SV triangle\u0000")
                sameLine(); showHelpMarker("User can right-click the picker to change mode.")
                var flags = miscFlags
                if (!alpha) flags = flags or Cef.NoAlpha // This is by default if you call ColorPicker3() instead of ColorPicker4()
                if (alphaBar) flags = flags or Cef.AlphaBar
                if (!sidePreview) flags = flags or Cef.NoSidePreview
                flags = flags or when (pickerMode) {
                    1 -> Cef.PickerHueBar
                    2 -> Cef.PickerHueWheel
                    else -> Cef.Null
                }
                flags = flags or when (inputsMode) {
                    1 -> Cef.NoInputs
                    2 -> Cef.RGB
                    3 -> Cef.HSV
                    4 -> Cef.HEX
                    else -> Cef.Null
                }
                colorPicker4("MyColor##4", color, flags, refColorV.takeIf { refColor })

                text("Programmatically set defaults/options:")
                sameLine(); showHelpMarker("SetColorEditOptions() is designed to allow you to set boot-time default.\nWe don't have Push/Pop functions because you can force options on a per-widget basis if needed, and the user can change non-forced ones with the options menu.\nWe don't have a getter to avoid encouraging you to persistently save values that aren't forward-compatible.")
                button("Uint8 + HSV") { setColorEditOptions(Cef.Uint8 or Cef.HSV) }
                sameLine()
                button("Float + HDR") { setColorEditOptions(Cef.Float or Cef.RGB) }
            }

            treeNode("Range Widgets") {
                dragFloatRange2("range", ::begin, ::end, 0.25f, 0f, 100f, "Min: %.1f %%", "Max: %.1f %%")
                dragIntRange2("range int (no bounds)", ::beginI, ::endI, 5f, 0, 0, "Min: %.0f units", "Max: %.0f units")
            }

            treeNode("Multi-component Widgets") {

                inputFloat2("input float2", vec4f)
                dragFloat2("drag float2", vec4f, 0.01f, 0f, 1f)
                sliderFloat2("slider float2", vec4f, 0f, 1f)
                dragInt2("drag int2", vec4i, 1f, 0, 255)
                inputInt2("input int2", vec4i)
                sliderInt2("slider int2", vec4i, 0, 255)
                spacing()

                inputFloat3("input float3", vec4f)
                dragFloat3("drag float3", vec4f, 0.01f, 0.0f, 1.0f)
                sliderFloat3("slider float3", vec4f, 0.0f, 1.0f)
                dragInt3("drag int3", vec4i, 1f, 0, 255)
                inputInt3("input int3", vec4i)
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

                                    withFloat { f ->
                                        f.set(values1[i])
                                        vSliderFloat("##v", Vec2(18, 160), f, 0f, 1f, "")
                                        values1[i] = f()
                                    }
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