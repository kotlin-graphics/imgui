package imgui.demo

import gli_.has
import glm_.*
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.acceptDragDropPayload
import imgui.ImGui.alignTextToFramePadding
import imgui.ImGui.arrowButton
import imgui.ImGui.begin
import imgui.ImGui.beginChild
import imgui.ImGui.beginCombo
import imgui.ImGui.beginDragDropSource
import imgui.ImGui.beginDragDropTarget
import imgui.ImGui.beginPopupContextItem
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
import imgui.ImGui.dragScalar
import imgui.ImGui.dragVec4
import imgui.ImGui.end
import imgui.ImGui.endChild
import imgui.ImGui.endCombo
import imgui.ImGui.endDragDropSource
import imgui.ImGui.endDragDropTarget
import imgui.ImGui.endPopup
import imgui.ImGui.fontSize
import imgui.ImGui.getMouseDragDelta
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
import imgui.ImGui.inputScalar
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
import imgui.ImGui.isItemToggledOpen
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
import imgui.ImGui.popID
import imgui.ImGui.popStyleVar
import imgui.ImGui.progressBar
import imgui.ImGui.pushButtonRepeat
import imgui.ImGui.pushID
import imgui.ImGui.pushStyleVar
import imgui.ImGui.radioButton
import imgui.ImGui.resetMouseDragDelta
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.separator
import imgui.ImGui.setColorEditOptions
import imgui.ImGui.setDragDropPayload
import imgui.ImGui.setItemDefaultFocus
import imgui.ImGui.setNextItemOpen
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
import imgui.ImGui.sliderScalar
import imgui.ImGui.smallButton
import imgui.ImGui.spacing
import imgui.ImGui.style
import imgui.ImGui.text
import imgui.ImGui.textColored
import imgui.ImGui.textDisabled
import imgui.ImGui.textLineHeight
import imgui.ImGui.textWrapped
import imgui.ImGui.time
import imgui.ImGui.treeNode
import imgui.ImGui.treeNodeEx
import imgui.ImGui.treeNodeToLabelSpacing
import imgui.ImGui.treePop
import imgui.ImGui.unindent
import imgui.ImGui.vSliderFloat
import imgui.ImGui.vSliderInt
import imgui.ImGui.windowDrawList
import imgui.api.demoDebugInformations.Companion.helpMarker
import imgui.classes.Color
import imgui.classes.InputTextCallbackData
import imgui.dsl.collapsingHeader
import imgui.dsl.group
import imgui.dsl.popup
import imgui.dsl.radioButton
import imgui.dsl.smallButton
import imgui.dsl.tooltip
import imgui.dsl.treeNode
import imgui.dsl.withButtonRepeat
import imgui.dsl.withId
import imgui.dsl.withItemWidth
import imgui.dsl.withStyleColor
import imgui.dsl.withStyleVar
import imgui.dsl.withTextWrapPos
import imgui.internal.sections.ItemFlags
import imgui.or
import kool.BYTES
import unsigned.Ubyte
import unsigned.Uint
import unsigned.Ulong
import unsigned.Ushort
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.reflect.KMutableProperty0
import imgui.ColorEditFlag as Cef
import imgui.InputTextFlag as Itf
import imgui.SelectableFlag as Sf
import imgui.TreeNodeFlag as Tnf

object ShowDemoWindowWidgets {

    /* Basic */
    var counter = 0
    var clicked = 0
    var check = true
    var e = 0
    val arr = floatArrayOf(0.6f, 0.1f, 1f, 0.5f, 0.92f, 0.1f, 0.2f)
    var currentItem0 = 0
    var str0 = "Hello, world!".toByteArray(128)
    var str1 = ByteArray(128)
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

    enum class Element { Fire, Earth, Air, Water }

    var elem = Element.Fire.ordinal
    val col1 = floatArrayOf(1f, 0f, 0.2f)
    val col2 = floatArrayOf(0.4f, 0.7f, 0f, 0.5f)
    var itemCurrent = 1


    /* Trees */
    var baseFlags: TreeNodeFlags = Tnf.OpenOnArrow or Tnf.OpenOnDoubleClick or Tnf.SpanAvailWidth
    var alignLabelWithCurrentXposition = false
    var testDragAndDrop = false
    var selectionMask = 1 shl 2


    /* Collapsing Headers */
    var closableGroup = true


    /* Text */
    var wrapWidth = 200f
    val buf = "日本語".toByteArray(32) // "nihongo"


    /* Images */
    var pressedCount = 0


    /* Combo */
    var flags0: ComboFlags = 0
    var itemCurrentIdx = 0
    var currentItem1 = 0
    var currentItem2 = 0
    var currentItem3 = 0

    object Funcs0 {
        val itemGetter: (Array<String>, Int, KMutableProperty0<String>) -> Boolean = { items, n, pStr ->
            pStr.set(items[n])
            true
        }
    }


    /* Selectables */
    val selection0 = booleanArrayOf(false, true, false, false, false)
    val selection1 = BooleanArray(5)
    var selected0 = -1
    val selected1 = BooleanArray(3)
    val selected2 = BooleanArray(16)
    val selected3 = arrayOf(
            intArrayOf(1, 0, 0, 0),
            intArrayOf(0, 1, 0, 0),
            intArrayOf(0, 0, 1, 0),
            intArrayOf(0, 0, 0, 1))
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
        ${'\t'}lock cmpxchg8b eax
        
        """.trimIndent().toByteArray(1024 * 16)
    var flags1 = Itf.AllowTabInput.i

    val bufs = Array(6) { ByteArray(64) }

    object TextFilters {
        // Return 0 (pass) if the character is 'i' or 'm' or 'g' or 'u' or 'i'
        val filterImGuiLetters: InputTextCallback = { data: InputTextCallbackData ->
            when {
                data.eventChar.i < 256 && data.eventChar in "imgui" -> false
                else -> true
            }
        }
    }

    val password = "password123".toByteArray(64)

    object Funcs1 {
        val myCallback: InputTextCallback = { data: InputTextCallbackData ->
            when (data.eventFlag) {
                Itf.CallbackCompletion.i -> data.insertChars(data.cursorPos, "..")
                Itf.CallbackHistory.i ->
                    if (data.eventKey == Key.UpArrow) {
                        data.deleteChars(0, data.bufTextLen)
                        data.insertChars(0, "Pressed Up!")
                        data.selectAll()
                    } else if (data.eventKey == Key.DownArrow) {
                        data.deleteChars(0, data.bufTextLen)
                        data.insertChars(0, "Pressed Down!")
                        data.selectAll()
                    }
                Itf.CallbackEdit.i -> {
                    // Toggle casing of first character
                    val c = data.buf[0].c
                    if (c in 'a'..'z' || c in 'A'..'Z')
                        data.buf[0] = data.buf[0] xor 32
                    data.bufDirty = true

                    // Increment a counter
                    var counter by (data.userData as KMutableProperty0<Int>)
                    counter = counter + 1 // cant ++ because of bug
                }
            }
            false
        }
    }

    var buf1 = ByteArray(64)
    var buf2 = ByteArray(64)
    var buf3 = ByteArray(64)
    var editCount = 0

    /* Color/Picker Widgets */
    val color = Vec4.fromColor(114, 144, 154, 200)
    var noBorder = false
    var alphaPreview = true
    var alphaHalfPreview = false
    var dragAndDrop = true
    var optionsMenu = true
    var hdr = false

    // Generate a default palette. The palette will persist and can be edited.
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
    var colorHsv = Vec4(0.23f, 1f, 1f, 1f)  // Stored as HSV!
    var flags2: SliderFlags = SliderFlag.None.i
    var dragF = 0.5f
    var dragI = 50
    var sliderF = 0.5f
    var sliderI = 50


    /* Range Widgets */

    var begin = 10f
    var end = 90f
    var beginI = 100
    var endI = 1000


    /* Data Types */
    // State
    var s8_v = 127.b
    var u8_v = Ubyte(255)
    var s16_v = 32767.s
    var u16_v = Ushort(65535)
    var s32_v = -1
    var u32_v = Uint(-1)
    var s64_v = -1L
    var u64_v = Ulong(-1)
    var f32_v = 0.123f
    var f64_v = 90000.01234567890123456789
    var dragClamp = false
    var inputsStep = true


    /* Multi-component Widgets */
    var vec4f = floatArrayOf(0.1f, 0.2f, 0.3f, 0.44f)
    val vec4i = intArrayOf(1, 5, 100, 255)

    /* Text Input */
    object Funcs2 {
        val MyResizeCallback: InputTextCallback = { data ->
            if (data.eventFlag == Itf.CallbackResize.i) {
                val myString = data.userData as ByteArray
                assert(myString.contentEquals(data.buf))
                data.userData = ByteArray(data.bufSize)  // NB: On resizing calls, generally data->BufSize == data->BufTextLen + 1
                data.buf = myString
            }
            false
        }

        // Note: Because ImGui:: is a namespace you would typically add your own function into the namespace.
        // For example, you code may declare a function 'ImGui::InputText(const char* label, MyString* my_str)'
        val MyInputTextMultiline: (label: String, myStr: ByteArray, size: Vec2, flags: ItemFlags) -> Boolean =
                { label, myStr, size, flags ->
                    assert(flags hasnt Itf.CallbackResize)
                    inputTextMultiline(label, String(myStr), size, flags or Itf.CallbackResize, MyResizeCallback, myStr)
                }
    }

    var myStr = ByteArray(0)

    /* Plots Widgets */
    var animate = true
    var refreshTime = 0.0
    val values0 = FloatArray(90)
    var valuesOffset = 0
    var phase = 0f
    var funcType = 0
    var displayCount = 70

    object Funcs3 {
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

    val names = arrayOf(
            "Bobby", "Beatrice", "Betty",
            "Brianna", "Barry", "Bernard",
            "Bibi", "Blaine", "Bryn")
    val itemNames = arrayOf("Item One", "Item Two", "Item Three", "Item Four", "Item Five")


    /* Vertical Sliders */
    var spacing = 4f
    var intValue = 0
    val values1 = floatArrayOf(0f, 0.6f, 0.35f, 0.9f, 0.7f, 0.2f, 0f)
    val values2 = floatArrayOf(0.2f, 0.8f, 0.4f, 0.25f)

    /* Active, Focused, Hovered & Focused Tests */
    var itemType = 1
    var b0 = false
    val col = floatArrayOf(1f, 0.5f, 0f, 1f)
    val str = ByteArray(16)
    var currentItem4 = 1
    var embedAllInsideAChildWindow = false
    var unusedStr = "This widget is only here to be able to tab-out of the widgets above."
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

            // Use AlignTextToFramePadding() to align text baseline to the baseline of framed widgets elements
            // (otherwise a Text+SameLine+Button sequence will have the text a little too high by default!)
            // See 'Demo->Layout->Text Baseline Alignment' for details.
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
                tooltip {
                    text("I am a fancy tooltip")
                    plotLines("Curve", arr)
                }
            separator()
            labelText("label", "Value")

            run {
                // Using the _simplified_ one-liner Combo() api here
                // See "Combo" section for examples of how to use the more complete BeginCombo()/EndCombo() api.
                val items = listOf("AAAA", "BBBB", "CCCC", "DDDD", "EEEE", "FFFF", "GGGG", "HHHH", "IIIIIII", "JJJJ", "KKKKKKK")
                combo("combo", ::currentItem0, items)
                sameLine(); helpMarker(
                    "Refer to the \"Combo\" section below for an explanation of the full BeginCombo/EndCombo API, " +
                            "and demonstration of various flags.\n")
            }

            run {
                // To wire InputText() with std::string or any other custom string type,
                // see the "Text Input > Resize Callback" section of this demo, and the misc/cpp/imgui_stdlib.h file.
                inputText("input text", str0)
                sameLine(); helpMarker("""
                    USER:
                    Hold SHIFT or use mouse to select text.
                    CTRL+Left/Right to word jump.
                    CTRL+A or double-click to select all.
                    CTRL+X,CTRL+C,CTRL+V clipboard.
                    CTRL+Z,CTRL+Y undo/redo.
                    ESCAPE to revert.
                    
                    PROGRAMMER:
                    You can use the ImGuiInputTextFlags_CallbackResize facility if you need to wire InputText() 
                    to a dynamic string type. See misc/cpp/imgui_stdlib.h for an example (this is not demonstrated 
                    in imgui_demo.cpp).""".trimIndent())

                inputTextWithHint("input text (w/ hint)", "enter text here", str1)

                inputInt("input int", ::i0)
                sameLine(); helpMarker("""
                    You can apply arithmetic operators +,*,/ on numerical values.
                      e.g. [ 100 ], input \'*2\', result becomes [ 200 ]
                    Use +- to subtract.""".trimIndent())

                inputFloat("input float", ::f0, 0.01f, 1f, "%.3f")

                inputDouble("input double", ::d0, 0.01, 1.0, "%.8f")

                inputFloat("input scientific", ::f1, 0f, 0f, "%e")
                sameLine(); helpMarker("""
                    You can input value using the scientific notation,
                      e.g. \"1e+8\" becomes \"100000000\".""".trimIndent())

                inputFloat3("input float3", vec4a)
            }
            run {
                dragInt("drag int", ::i1, 1f)
                sameLine(); helpMarker("""
                    Click and drag to edit value.
                    Hold SHIFT/ALT for faster/slower edit.
                    Double-click or CTRL+click to input value.""".trimIndent())

                dragInt("drag int 0..100", ::i2, 1f, 0, 100, "%d%%", SliderFlag.AlwaysClamp.i)

                dragFloat("drag float", ::f2, 0.005f)
                dragFloat("drag small float", ::f3, 0.0001f, 0f, 0f, "%.06f ns")
            }
            run {
                sliderInt("slider int", ::i3, -1, 3)
                sameLine(); helpMarker("CTRL+click to input value.")

                sliderFloat("slider float", ::f4, 0f, 1f, "ratio = %.3f")
//                TODO
//                sliderFloat("slider float (curve)", ::f5, -10f, 10f, "%.4f", 2f)

                sliderAngle("slider angle", ::angle)

                // Using the format string to display a name instead of an integer.
                // Here we completely omit '%d' from the format string, so it'll only display a name.
                // This technique can also be used with DragInt().
                val elemName = Element.values().getOrNull(elem)?.name ?: "Unknown"
                sliderInt("slider enum", ::elem, 0, Element.values().lastIndex, elemName)
                sameLine(); helpMarker("Using the format string parameter to display a name instead of the underlying integer.")
            }

            run {
                colorEdit3("color 1", col1)
                sameLine(); helpMarker("""
                    Click on the colored square to open a color picker.
                    Click and hold to use drag and drop.
                    Right-click on the colored square to show options.
                    CTRL+click on individual component to input value.""".trimIndent())

                colorEdit4("color 2", col2)
            }

            run { // List box
                val items = arrayOf("Apple", "Banana", "Cherry", "Kiwi", "Mango", "Orange", "Pineapple", "Strawberry", "Watermelon")
                listBox("listbox\n(single select)", ::itemCurrent, items, 4)
            }
        }

        treeNode("Trees") {
            treeNode("Basic trees") {
                for (i in 0..4) {
                    // Use SetNextItemOpen() so set the default state of a node to be open. We could
                    // also use TreeNodeEx() with the ImGuiTreeNodeFlags_DefaultOpen flag to achieve the same thing!
                    if (i == 0)
                        setNextItemOpen(true, Cond.Once)

                    treeNode(i.L, "Child $i") {
                        text("blah blah")
                        sameLine()
                        smallButton("button") { }
                    }
                }
            }

            treeNode("Advanced, with Selectable nodes") {

                helpMarker("""
                    This is a more typical looking tree with selectable nodes.
                    Click to select, CTRL+Click to toggle, click on arrows or double-click to open.""".trimIndent())
                checkboxFlags("ImGuiTreeNodeFlags_OpenOnArrow", ::baseFlags, Tnf.OpenOnArrow.i)
                checkboxFlags("ImGuiTreeNodeFlags_OpenOnDoubleClick", ::baseFlags, Tnf.OpenOnDoubleClick.i)
                checkboxFlags("ImGuiTreeNodeFlags_SpanAvailWidth", ::baseFlags, Tnf.SpanAvailWidth.i); sameLine(); helpMarker("Extend hit area to all available width instead of allowing more items to be laid out after the node.")
                checkboxFlags("ImGuiTreeNodeFlags_SpanFullWidth", ::baseFlags, Tnf.SpanFullWidth.i)
                checkbox("Align label with current X position", ::alignLabelWithCurrentXposition)
                checkbox("Test tree node as drag source", ::testDragAndDrop)
                text("Hello!")
                if (alignLabelWithCurrentXposition) unindent(treeNodeToLabelSpacing)

                // 'selection_mask' is dumb representation of what may be user-side selection state.
                //  You may retain selection state inside or outside your objects in whatever format you see fit.
                // 'node_clicked' is temporary storage of what node we have clicked to process selection at the end
                /// of the loop. May be a pointer to your own node type, etc.
                var nodeClicked = -1
                for (i in 0..5) {
                    // Disable the default "open on single-click behavior" + set Selected flag according to our selection.
                    var nodeFlags = baseFlags
                    val isSelected = selectionMask has (1 shl i)
                    if (isSelected)
                        nodeFlags = nodeFlags or Tnf.Selected
                    if (i < 3) {
                        // Items 0..2 are Tree Node
                        val nodeOpen = treeNodeEx(i.L, nodeFlags, "Selectable Node $i")
                        if (isItemClicked()) nodeClicked = i
                        if (testDragAndDrop && beginDragDropSource()) {
                            setDragDropPayload("_TREENODE", null)
                            text("This is a drag and drop source")
                            endDragDropSource()
                        }
                        if (nodeOpen) {
                            bulletText("Blah blah\nBlah Blah")
                            treePop()
                        }
                    } else {
                        // Items 3..5 are Tree Leaves
                        // The only reason we use TreeNode at all is to allow selection of the leaf. Otherwise we can
                        // use BulletText() or advance the cursor by GetTreeNodeToLabelSpacing() and call Text().
                        nodeFlags = nodeFlags or Tnf.Leaf or Tnf.NoTreePushOnOpen // or Tnf.Bullet
                        treeNodeEx(i.L, nodeFlags, "Selectable Leaf $i")
                        if (isItemClicked()) nodeClicked = i
                        if (testDragAndDrop && beginDragDropSource()) {
                            setDragDropPayload("_TREENODE", null)
                            text("This is a drag and drop source")
                            endDragDropSource()
                        }
                    }
                }
                if (nodeClicked != -1) {
                    // Update selection state
                    // (process outside of tree loop to avoid visual inconsistencies during the clicking frame)
                    if (io.keyCtrl)
                        selectionMask = selectionMask xor (1 shl nodeClicked)   // CTRL+click to toggle
                    else //if (!(selectionMask & (1 << nodeClicked))) // Depending on selection behavior you want, may want to preserve selection when clicking on item that is part of the selection
                        selectionMask = (1 shl nodeClicked) // Click to single-select
                }
                if (alignLabelWithCurrentXposition) indent(treeNodeToLabelSpacing)
            }
        }

        treeNode("Collapsing Headers") {
            checkbox("Show 2nd header", ::closableGroup)
            collapsingHeader("Header", Tnf.None.i) {
                text("IsItemHovered: ${isItemHovered()}")
                for (i in 0..4) text("Some content $i")
            }
            collapsingHeader("Header with a close button", ::closableGroup) {
                text("IsItemHovered: ${isItemHovered()}")
                for (i in 0..4)
                    text("More content $i")
            }
            /*
            if (ImGui::CollapsingHeader("Header with a bullet", ImGuiTreeNodeFlags_Bullet))
                ImGui::Text("IsItemHovered: %d", ImGui::IsItemHovered());
            */
        }

        treeNode("Bullets") {
            bulletText("Bullet point 1")
            bulletText("Bullet point 2\nOn multiple lines")
            treeNode("Tree node") { bulletText("Another bullet point") }
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
                textWrapped(
                        "This text should automatically wrap on the edge of the window. The current implementation " +
                                "for text wrapping follows simple rules suitable for English and possibly other languages.")
                spacing()

                sliderFloat("Wrap width", ::wrapWidth, -20f, 600f, "%.0f")

                val drawList = windowDrawList
                for (n in 0..1) {
                    text("Test paragraph $n:")
                    val pos = cursorScreenPos
                    val markerMin = Vec2(pos.x + wrapWidth, pos.y)
                    val markerMax = Vec2(pos.x + wrapWidth + 10, pos.y + textLineHeight)
                    withTextWrapPos(cursorPos.x + wrapWidth) {
                        if (n == 0)
                            text("The lazy dog is a good dog. This paragraph should fit within %.0f pixels. Testing a 1 character word. The quick brown fox jumps over the lazy dog.", wrapWidth)
                        if (n == 1)
                            text("aaaaaaaa bbbbbbbb, c cccccccc,dddddddd. d eeeeeeee   ffffffff. gggggggg!hhhhhhhh")

                        // Draw actual text bounding box, following by marker of our expected limit (should not overlap!)
                        drawList.addRect(itemRectMin, itemRectMax, COL32(255, 255, 0, 255))
                        drawList.addRectFilled(markerMin, markerMax, COL32(255, 0, 255, 255))
                    }
                }
            }
            treeNode("UTF-8 text") {
                // UTF-8 test with Japanese characters
                // (Needs a suitable font? Try "Google Noto" or "Arial Unicode". See docs/FONTS.txt for details.)
                // - From C++11 you can use the u8"my text" syntax to encode literal strings as UTF-8
                // - For earlier compiler, you may be able to encode your sources as UTF-8 (e.g. in Visual Studio, you
                //   can save your source files as 'UTF-8 without signature').
                // - FOR THIS DEMO FILE ONLY, BECAUSE WE WANT TO SUPPORT OLD COMPILERS, WE ARE *NOT* INCLUDING RAW UTF-8
                //   CHARACTERS IN THIS SOURCE FILE. Instead we are encoding a few strings with hexadecimal constants.
                //   Don't do this in your application! Please use u8"text in any language" in your application!
                // Note that characters values are preserved even by InputText() if the font cannot be displayed,
                // so you can safely copy & paste garbled characters into another application.
                textWrapped(
                        "CJK text will only appears if the font was loaded with the appropriate CJK character ranges." +
                                "Call io.Font->AddFontFromFileTTF() manually to load extra character ranges." +
                                "Read docs/FONTS.txt for details.")
                text("Hiragana: \u304b\u304d\u304f\u3051\u3053 (kakikukeko)") // Normally we would use u8"blah blah" with the proper characters directly in the string.
                text("Kanjis: \u65e5\u672c\u8a9e (nihongo)")
                inputText("UTF-8 input", buf)
            }
        }

        treeNode("Images") {
            textWrapped(
                    "Below we are displaying the font texture (which is the only texture we have access to in this demo). " +
                            "Use the 'ImTextureID' type as storage to pass pointers or identifier to your own texture data. " +
                            "Hover the texture for a zoomed view!")
            // Below we are displaying the font texture because it is the only texture we have access to inside the demo!
            // Remember that ImTextureID is just storage for whatever you want it to be. It is essentially a value that
            // will be passed to the rendering back-end via the ImDrawCmd structure.
            // If you use one of the default imgui_impl_XXXX.cpp rendering back-end, they all have comments at the top
            // of their respective source file to specify what they expect to be stored in ImTextureID, for example:
            // - The imgui_impl_dx11.cpp renderer expect a 'ID3D11ShaderResourceView*' pointer
            // - The imgui_impl_opengl3.cpp renderer expect a GLuint OpenGL texture identifier, etc.
            // More:
            // - If you decided that ImTextureID = MyEngineTexture*, then you can pass your MyEngineTexture* pointers
            //   to ImGui::Image(), and gather width/height through your own functions, etc.
            // - You can use ShowMetricsWindow() to inspect the draw data that are being passed to your renderer,
            //   it will help you debug issues if you are confused about it.
            // - Consider using the lower-level ImDrawList::AddImage() API, via ImGui::GetWindowDrawList()->AddImage().
            // - Read https://github.com/ocornut/imgui/blob/master/docs/FAQ.md
            // - Read https://github.com/ocornut/imgui/wiki/Image-Loading-and-Displaying-Examples
            val myTexId = io.fonts.texID
            val myTexSize = Vec2(io.fonts.texSize)

            run {
                text("%.0fx%.0f", myTexSize.x, myTexSize.y)
                val pos = cursorScreenPos
                val uvMin = Vec2(0f)                 // Top-left
                val uvMax = Vec2(1f)                 // Lower-right
                val tintCol = Vec4(1f)   // No tint
                val borderCol = Vec4(1f, 1f, 1f, 0.5f) // 50% opaque white
                image(myTexId, Vec2(myTexSize.x, myTexSize.y), uvMin, uvMax, tintCol, borderCol)
                if (isItemHovered())
                    tooltip {
                        val regionSz = 32f
                        var regionX = io.mousePos.x - pos.x - regionSz * 0.5f
                        var regionY = io.mousePos.y - pos.y - regionSz * 0.5f
                        val zoom = 4f
                        if (regionX < 0f)
                            regionX = 0f
                        else if (regionX > myTexSize.x - regionSz)
                            regionX = myTexSize.x - regionSz
                        if (regionY < 0f)
                            regionY = 0f
                        else if (regionY > myTexSize.y - regionSz)
                            regionY = myTexSize.y - regionSz
                        text("Min: (%.2f, %.2f)", regionX, regionY)
                        text("Max: (%.2f, %.2f)", regionX + regionSz, regionY + regionSz)
                        val uv0 = Vec2(regionX / myTexSize.x, regionY / myTexSize.y)
                        val uv1 = Vec2((regionX + regionSz) / myTexSize.x, (regionY + regionSz) / myTexSize.y)
                        image(myTexId, Vec2(regionSz * zoom, regionSz * zoom), uv0, uv1, tintCol, borderCol)
                    }
            }
            textWrapped("And now some textured buttons..")
            for (i in 0..7) {
                withId(i) {
                    val framePadding = -1 + i                             // -1 == uses default padding (style.FramePadding)
                    val size = Vec2(32)                     // Size of the image we want to make visible
                    val uv0 = Vec2()                        // UV coordinates for lower-left
                    val uv1 = Vec2(32f / myTexSize.x, 32 / myTexSize.y)   // UV coordinates for (32,32) in our texture
                    val bgCol = Vec4(0f, 0f, 0f, 1f)         // Black background
                    val tintCol = Vec4(1f, 1f, 1f, 1f)       // No tint
                    if (imageButton(myTexId, size, uv0, uv1, framePadding, bgCol, tintCol))
                        pressedCount++
                }
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

            // Using the generic BeginCombo() API, you have full control over how to display the combo contents.
            // (your selection data could be an index, a pointer to the object, an id for the object, a flag intrusively
            // stored in the object itself, etc.)
            val items = listOf("AAAA", "BBBB", "CCCC", "DDDD", "EEEE", "FFFF", "GGGG", "HHHH", "IIII", "JJJJ", "KKKK", "LLLLLLL", "MMMM", "OOOOOOO")
            // Here our selection data is an index.
            if (beginCombo("combo 1", items[0], flags0)) { // Label to preview before opening the combo (technically could be anything)(
                items.forEachIndexed { n, it ->
                    val isSelected = itemCurrentIdx == n
                    if (selectable(it, isSelected))
                        itemCurrentIdx = n

                    // Set the initial focus when opening the combo (scrolling + keyboard navigation focus)
                    if (isSelected)
                        setItemDefaultFocus()
                }
                endCombo()
            }
            // Simplified one-liner Combo() API, using values packed in a single constant string
            combo("combo 2 (one-liner)", ::currentItem1, "aaaa\u0000bbbb\u0000cccc\u0000dddd\u0000eeee\u0000\u0000")

            /*  Simplified one-liner Combo() using an array of const char*
                If the selection isn't within 0..count, Combo won't display a preview                 */
            combo("combo 3 (array)", ::currentItem2, items)

            // Simplified one-liner Combo() using an accessor function
            combo("combo 4 (function)", ::currentItem3, Funcs0.itemGetter, items.toTypedArray())
        }

        treeNode("Selectables") {
            // Selectable() has 2 overloads:
            // - The one taking "bool selected" as a read-only selection information.
            //   When Selectable() has been clicked it returns true and you can alter selection state accordingly.
            // - The one taking "bool* p_selected" as a read-write selection information (convenient in some cases)
            // The earlier is more flexible, as in real application your selection may be stored in many different ways
            // and not necessarily inside a bool value (e.g. in flags within objects, as an external list, etc).
            treeNode("Basic") {
                selectable("1. I am selectable", selection0, 0)
                selectable("2. I am selectable", selection0, 1)
                text("3. I am not selectable")
                selectable("4. I am selectable", selection0, 2)
                if (selectable("5. I am double clickable", selection0[3], Sf.AllowDoubleClick.i))
                    if (isMouseDoubleClicked(MouseButton.Left)) selection0[3] = !selection0[3]
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
                        selection1[n] = selection1[n] xor true
                    }
            }
            treeNode("Rendering more text into the same line") {
                // Using the Selectable() override that takes "bool* p_selected" parameter,
                // this function toggle your bool value automatically.
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

                // Add in a bit of silly fun...
                val time = ImGui.time
                val winningState = selected3.all { it.all { it == 1 } } // If all cells are selected...
                if (winningState)
                    pushStyleVar(StyleVar.SelectableTextAlign, Vec2(0.5f + 0.5f * cos(time * 2f), 0.5f + 0.5f * sin(time * 3f)))

                for (y in 0..3)
                    for (x in 0..3) {
                        if (x > 0)
                            sameLine()
                        pushID(y * 4 + x)
                        if (selectable("Sailor", selected3[y][x] != 0, 0, Vec2(50))) {
                            // Toggle clicked cell + toggle neighbors
                            selected3[y][x] = selected3[y][x] xor 1
                            if (x > 0) selected3[y][x - 1] = selected3[y][x - 1] xor 1
                            if (x < 3) selected3[y][x + 1] = selected3[y][x + 1] xor 1
                            if (y > 0) selected3[y - 1][x] = selected3[y - 1][x] xor 1
                            if (y < 3) selected3[y + 1][x] = selected3[y + 1][x] xor 1
                        }
                        popID()
                    }

                if (winningState)
                    popStyleVar()
            }
            treeNode("Alignment") {
                helpMarker(
                        """
                    By default, Selectables uses style.SelectableTextAlign but it can be overridden on a per-item 
                    " +
                                "basis using PushStyleVar(). You'll probably want to always keep your default situation to 
                    " +
                                "left-align otherwise it becomes difficult to layout multiple items on a same line""".trimIndent())
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

        // To wire InputText() with std::string or any other custom string type,
        // see the "Text Input > Resize Callback" section of this demo, and the misc/cpp/imgui_stdlib.h file.
        treeNode("Text Input") {

            treeNode("Multi-line Text Input") {
                /*  Note: we are using a fixed-sized buffer for simplicity here. See ImGuiInputTextFlags_CallbackResize
                    and the code in misc/cpp/imgui_stdlib.h for how to setup InputText() for dynamically resizing strings.  */
                helpMarker("You can use the InputTextFlag.CallbackResize facility if you need to wire InputTextMultiline() to a dynamic string type. See misc/cpp/imgui_stl.h for an example. (This is not demonstrated in imgui_demo.cpp because we don't want to include <string> in here)") // TODO fix bug, some '?' appear at the end of the line
                checkboxFlags("ImGuiInputTextFlags_ReadOnly", ::flags1, Itf.ReadOnly.i)
                checkboxFlags("ImGuiInputTextFlags_AllowTabInput", ::flags1, Itf.AllowTabInput.i)
                checkboxFlags("ImGuiInputTextFlags_CtrlEnterForNewLine", ::flags1, Itf.CtrlEnterForNewLine.i)
                inputTextMultiline("##source", textMultiline, Vec2(-Float.MIN_VALUE, textLineHeight * 16), flags1)
            }

            treeNode("Filtered Text Input") {
                inputText("default", bufs[0])
                inputText("decimal", bufs[1], Itf.CharsDecimal.i)
                inputText("hexadecimal", bufs[2], Itf.CharsHexadecimal or Itf.CharsUppercase)
                inputText("uppercase", bufs[3], Itf.CharsUppercase.i)
                inputText("no blank", bufs[4], Itf.CharsNoBlank.i)
                inputText("\"imgui\" letters", bufs[5], Itf.CallbackCharFilter.i, TextFilters.filterImGuiLetters)
            }
            treeNode("Password Input") {
                inputText("password", password, Itf.Password.i)
                sameLine(); helpMarker("Display all characters as '*'.\nDisable clipboard cut and copy.\nDisable logging.")
                inputTextWithHint("password (w/ hint)", "<password>", password, Itf.Password.i)
                inputText("password (clear)", password)
            }

            treeNode("Completion, History, Edit Callbacks") {
                inputText("Completion", buf1, Itf.CallbackCompletion.i, Funcs1.myCallback)
                sameLine(); helpMarker("Here we append \"..\" each time Tab is pressed. See 'Examples>Console' for a more meaningful demonstration of using this callback.")

                inputText("History", buf2, Itf.CallbackHistory.i, Funcs1.myCallback)
                sameLine(); helpMarker("Here we replace and select text each time Up/Down are pressed. See 'Examples>Console' for a more meaningful demonstration of using this callback.")

                inputText("Edit", buf3, Itf.CallbackEdit.i, Funcs1.myCallback, ::editCount)
                sameLine(); helpMarker("Here we toggle the casing of the first character on every edits + count edits.")
                sameLine(); text("($editCount)")
            }

            treeNode("Resize Callback") {
                // To wire InputText() with std::string or any other custom string type,
                // you can use the ImGuiInputTextFlags_CallbackResize flag + create a custom ImGui::InputText() wrapper
                // using your preferred type. See misc/cpp/imgui_stdlib.h for an implementation of this using std::string.
                helpMarker("""
                    Using ImGuiInputTextFlags_CallbackResize to wire your custom string type to InputText().
                    
                    See misc/cpp/imgui_stdlib.h for an implementation of this for std::string.""".trimIndent())

                // For this demo we are using ImVector as a string container.
                // Note that because we need to store a terminating zero character, our size/capacity are 1 more
                // than usually reported by a typical string class.
                if (myStr.isEmpty())
                    myStr = ByteArray(1)
                Funcs2.MyInputTextMultiline("##MyStr", myStr, Vec2(-Float.MIN_VALUE, textLineHeight * 16), 0)
                text("Data: ${myStr.hashCode()}\nSize: ${myStr.strlen()}\nCapacity: ${myStr.size}")
            }
        }

        // Plot/Graph widgets are currently fairly limited.
        // Tip: If your float aren't contiguous but part of a structure, you can pass a pointer to your first float
        // and the sizeof() of your structure in the "stride" parameter.
        treeNode("Plots Widgets") {

            checkbox("Animate", ::animate)

            val arr = floatArrayOf(0.6f, 0.1f, 1f, 0.5f, 0.92f, 0.1f, 0.2f)
            plotLines("Frame Times", arr)

            // Fill an array of contiguous float values to plot
            // Tip: If your float aren't contiguous but part of a structure, you can pass a pointer to your first float
            // and the sizeof() of your structure in the "stride" parameter.
            if (!animate || refreshTime == 0.0) refreshTime = time
            while (refreshTime < time) { // Create data at fixed 60 Hz rate for the demo
                values0[valuesOffset] = cos(phase)
                valuesOffset = (valuesOffset + 1) % values0.size
                phase += 0.1f * valuesOffset
                refreshTime += 1f / 60f
            }
            // Plots can display overlay texts
            // (in this example, we will display an average value)
            run {
                val overlay = "avg ${values0.average()}"
                plotLines("Lines", values0, valuesOffset, overlay, -1f, 1f, Vec2(0f, 80f))
            }
            plotHistogram("Histogram", arr, 0, "", 0f, 1f, Vec2(0f, 80f))

            // Use functions to generate output
            // FIXME: This is rather awkward because current plot API only pass in indices.
            // We probably want an API passing floats and user provide sample rate/count.
            separator()
            withItemWidth(100) { combo("func", ::funcType, "Sin\u0000Saw\u0000") }
            sameLine()
            sliderInt("Sample count", ::displayCount, 1, 400)
            val func = if (funcType == 0) Funcs3::sin else Funcs3::saw
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
            /*  Typically we would use Vec2(-1f , 0f) or ImVec2(-FLT_MIN,0.0f) to use all available width,
                or Vec2(width, 0f) for a specified width. Vec2(0f, 0f) uses ItemWidth. */
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
            var miscFlags = if (hdr) Cef.HDR.i else Cef.None.i
            if (!dragAndDrop) miscFlags = miscFlags or Cef.NoDragDrop
            if (alphaHalfPreview) miscFlags = miscFlags or Cef.AlphaPreviewHalf
            else if (alphaPreview) miscFlags = miscFlags or Cef.AlphaPreview
            if (!optionsMenu) miscFlags = miscFlags or Cef.NoOptions

            text("Color widget:")
            sameLine(); helpMarker("""
                Click on the colored square to open a color picker.                
                CTRL+click on individual component to input value.
                """.trimIndent())
            colorEdit3("MyColor##1", color, miscFlags)

            text("Color widget HSV with Alpha:")
            colorEdit4("MyColor##2", color, Cef.DisplayHSV or miscFlags)

            text("Color widget with Float Display:")
            colorEdit4("MyColor##2f", color, Cef.Float or miscFlags)

            text("Color button with Picker:")
            sameLine(); helpMarker(
                "With the ImGuiColorEditFlags_NoInputs flag you can hide all the slider/text inputs.\n" +
                        "With the ImGuiColorEditFlags_NoLabel flag you can pass a non-empty label which will only " +
                        "be used for the tooltip and picker popup.")
            colorEdit4("MyColor##3", color, Cef.NoInputs or Cef.NoLabel or miscFlags)

            text("Color button with Custom Picker Popup:")
            if (savedPaletteInit)
                savedPalette.forEachIndexed { n, c ->
                    colorConvertHSVtoRGB(n / 31f, 0.8f, 0.8f, c::x, c::y, c::z)
                    savedPalette[n].w = 1f // Alpha
                }
            savedPaletteInit = false
            var openPopup = colorButton("MyColor##3b", color, miscFlags)
            sameLine(0f, style.itemInnerSpacing.x)
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

                group {
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

                            val paletteButtonFlags = Cef.NoAlpha or Cef.NoPicker or Cef.NoTooltip
                            if (colorButton("##palette", c, paletteButtonFlags, Vec2(20)))
                                color.put(c.x, c.y, c.z, color.w) // Preserve alpha!

                            // Allow user to drop colors into each palette entry. Note that ColorButton() is already a
                            // drag source by default, unless specifying the ImGuiColorEditFlags_NoDragDrop flag.
                            if (beginDragDropTarget()) {
                                acceptDragDropPayload(PAYLOAD_TYPE_COLOR_3F)?.let {
                                    for (i in 0..2) savedPalette[n][i] = (it.data!! as Vec3).array[i]
                                }
                                acceptDragDropPayload(PAYLOAD_TYPE_COLOR_4F)?.let {
                                    for (i in 0..3) savedPalette[n][i] = (it.data!! as Vec4).array[i]
                                }
                                endDragDropTarget()
                            }
                        }
                    }
                }
            }
            text("Color button only:")
            checkbox("ImGuiColorEditFlags_NoBorder", ::noBorder)
            colorButton("MyColor##3c", color, miscFlags or if (noBorder) Cef.NoBorder else Cef.None, Vec2(80))

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
            sameLine(); helpMarker(
                "ColorEdit defaults to displaying RGB inputs if you don't specify a display mode, " +
                        "but the user can change it with a right-click.\n\nColorPicker defaults to displaying RGB+HSV+Hex " +
                        "if you don't specify a display mode.\n\nYou can change the defaults using SetColorEditOptions().")
            combo("Picker Mode", ::pickerMode, "Auto/Current\u0000Hue bar + SV rect\u0000Hue wheel + SV triangle\u0000")
            sameLine(); helpMarker("User can right-click the picker to change mode.")
            var flags = miscFlags
            if (!alpha) flags = flags or Cef.NoAlpha // This is by default if you call ColorPicker3() instead of ColorPicker4()
            if (alphaBar) flags = flags or Cef.AlphaBar
            if (!sidePreview) flags = flags or Cef.NoSidePreview
            if (pickerMode == 1) flags = flags or Cef.PickerHueBar
            if (pickerMode == 2) flags = flags or Cef.PickerHueWheel
            if (displayMode == 1) flags = flags or Cef.NoInputs     // Disable all RGB/HSV/Hex displays
            if (displayMode == 2) flags = flags or Cef.DisplayRGB   // Override display mode
            if (displayMode == 3) flags = flags or Cef.DisplayHSV
            if (displayMode == 4) flags = flags or Cef.DisplayHEX
            colorPicker4("MyColor##4", color, flags, refColorV.takeIf { refColor })

            text("Set defaults in code:")
            sameLine(); helpMarker(
                "SetColorEditOptions() is designed to allow you to set boot-time default.\n" +
                        "We don't have Push/Pop functions because you can force options on a per-widget basis if needed," +
                        "and the user can change non-forced ones with the options menu.\nWe don't have a getter to avoid" +
                        "encouraging you to persistently save values that aren't forward-compatible.")
            if (button("Default: Uint8 + HSV + Hue Bar"))
                setColorEditOptions(Cef.Uint8 or Cef.DisplayHSV or Cef.PickerHueBar)
            if (button("Default: Float + HDR + Hue Wheel"))
                setColorEditOptions(Cef.Float or Cef.HDR or Cef.PickerHueWheel)

            // HSV encoded support (to avoid RGB<>HSV round trips and singularities when S==0 or V==0)
            spacing()
            text("HSV encoded colors")
            sameLine(); helpMarker(
                "By default, colors are given to ColorEdit and ColorPicker in RGB, but ImGuiColorEditFlags_InputHSV" +
                        "allows you to store colors as HSV and pass them to ColorEdit and ColorPicker as HSV. This comes with the" +
                        "added benefit that you can manipulate hue values with the picker even when saturation or value are zero.")
            text("Color widget with InputHSV:")
            colorEdit4("HSV shown as RGB##1", colorHsv, Cef.DisplayRGB or Cef.InputHSV or Cef.Float)
            colorEdit4("HSV shown as HSV##1", colorHsv, Cef.DisplayHSV or Cef.InputHSV or Cef.Float)
            dragVec4("Raw HSV values", colorHsv, 0.01f, 0f, 1f)
        }

        treeNode("Drag/Slider Flags") {
            // Demonstrate using advanced flags for DragXXX and SliderXXX functions. Note that the flags are the same!
            checkboxFlags("ImGuiSliderFlags_AlwaysClamp", ::flags2, SliderFlag.AlwaysClamp.i)
            sameLine(); helpMarker("Always clamp value to min/max bounds (if any) when input manually with CTRL+Click.")
            checkboxFlags("ImGuiSliderFlags_Logarithmic", ::flags2, SliderFlag.Logarithmic.i)
            sameLine(); helpMarker("Enable logarithmic editing (more precision for small values).")
            checkboxFlags("ImGuiSliderFlags_NoRoundToFormat", ::flags2, SliderFlag.NoRoundToFormat.i)
            sameLine(); helpMarker("Disable rounding underlying value to match precision of the format string (e.g. %.3f values are rounded to those 3 digits).")
            checkboxFlags("ImGuiSliderFlags_NoInput", ::flags2, SliderFlag.NoInput.i)
            sameLine(); helpMarker("Disable CTRL+Click or Enter key allowing to input text directly into the widget.")

            // Drags
            text("Underlying float value: %f", dragF)
            dragFloat("DragFloat (0 -> 1)", ::dragF, 0.005f, 0f, 1f, "%.3f", flags2)
            dragFloat("DragFloat (0 -> +inf)", ::dragF, 0.005f, 0f, Float.MAX_VALUE, "%.3f", flags2)
            dragFloat("DragFloat (-inf -> 1)", ::dragF, 0.005f, -Float.MAX_VALUE, 1f, "%.3f", flags2)
            dragFloat("DragFloat (-inf -> +inf)", ::dragF, 0.005f, -Float.MAX_VALUE, +Float.MAX_VALUE, "%.3f", flags2)
            dragInt("DragInt (0 -> 100)", ::dragI, 0.5f, 0, 100, "%d", flags2)

            // Sliders
            text("Underlying float value: %f", sliderF)
            sliderFloat("SliderFloat (0 -> 1)", ::sliderF, 0f, 1f, "%.3f", flags2)
            sliderInt("SliderInt (0 -> 100)", ::sliderI, 0, 100, "%d", flags2)
        }

        treeNode("Range Widgets") {
            dragFloatRange2("range float", ::begin, ::end, 0.25f, 0f, 100f, "Min: %.1f %%", "Max: %.1f %%", SliderFlag.AlwaysClamp.i)
            dragIntRange2("range int", ::beginI, ::endI, 5f, 0, 1000, "Min: %d units", "Max: %d units")
            dragIntRange2("range int (no bounds)", ::beginI, ::endI, 5f, 0, 0, "Min: %d units", "Max: %d units")
        }

        treeNode("Data Types") {
            // DragScalar/InputScalar/SliderScalar functions allow various data types
            // - signed/unsigned
            // - 8/16/32/64-bits
            // - integer/float/double
            // To avoid polluting the public API with all possible combinations, we use the ImGuiDataType enum
            // to pass the type, and passing all arguments by pointer.
            // This is the reason the test code below creates local variables to hold "zero" "one" etc. for each types.
            // In practice, if you frequently use a given type that is not covered by the normal API entry points,
            // you can wrap it yourself inside a 1 line function which can take typed argument as value instead of void*,
            // and then pass their address to the generic function. For example:
            //   bool MySliderU64(const char *label, u64* value, u64 min = 0, u64 max = 0, const char* format = "%lld")
            //   {
            //      return SliderScalar(label, ImGuiDataType_U64, value, &min, &max, format);
            //   }

            // Setup limits (as helper variables so we can take their address, as explained above)
            // Note: SliderScalar() functions have a maximum usable range of half the natural type maximum, hence the /2.

            // @formatter:off
            val s8_zero: Byte = 0.b
            val s8_one: Byte = 1.b
            val s8_fifty: Byte = 50.b
            val s8_min: Byte = (-128).b
            val s8_max: Byte = 127.b
            val u8_zero: Ubyte = Ubyte(0)
            val u8_one: Ubyte = Ubyte(1)
            val u8_fifty: Ubyte = Ubyte(50)
            val u8_min: Ubyte = Ubyte(0)
            val u8_max: Ubyte = Ubyte(255)
            val s16_zero: Short = 0.s
            val s16_one: Short = 1.s
            val s16_fifty: Short = 50.s
            val s16_min: Short = (-32768).s
            val s16_max: Short = 32767.s
            val u16_zero: Ushort = Ushort(0)
            val u16_one = Ushort(1)
            val u16_fifty: Ushort = Ushort(50)
            val u16_min: Ushort = Ushort(0)
            val u16_max: Ushort = Ushort(65535)
            val s32_zero: Int = 0
            val s32_one: Int = 1
            val s32_fifty: Int = 50
            val s32_min: Int = Int.MIN_VALUE / 2
            val s32_max: Int = Int.MAX_VALUE / 2
            val s32_hi_a = Int.MAX_VALUE / 2 - 100
            val s32_hi_b = Int.MAX_VALUE / 2
            val u32_zero: Uint = Uint(0)
            val u32_one: Uint = Uint(1)
            val u32_fifty: Uint = Uint(50)
            val u32_min: Uint = Uint(0)
            val u32_max: Uint = Uint.MAX / 2
            val u32_hi_a = Uint.MAX / 2 - 100
            val u32_hi_b: Uint = Uint.MAX / 2
            val s64_zero: Long = 0L
            val s64_one: Long = 1L
            val s64_fifty: Long = 50L
            val s64_min: Long = Long.MIN_VALUE / 2
            val s64_max: Long = Long.MAX_VALUE / 2
            val s64_hi_a: Long = Long.MAX_VALUE / 2 - 100
            val s64_hi_b: Long = Long.MAX_VALUE / 2
            val u64_zero: Ulong = Ulong(0)
            val u64_one: Ulong = Ulong(1)
            val u64_fifty: Ulong = Ulong(50)
            val u64_min: Ulong = Ulong(0)
            val u64_max: Ulong = Ulong.MAX / 2
            val u64_hi_a: Ulong = Ulong.MAX / 2 - 100
            val u64_hi_b: Ulong = Ulong.MAX / 2
            val f32_zero: Float = 0f
            val f32_one: Float = 1f
            val f32_lo_a: Float = -10_000_000_000f
            val f32_hi_a: Float = +10_000_000_000f
            val f64_zero: Double = 0.0
            val f64_one: Double = 1.0
            val f64_lo_a: Double = -1_000_000_000_000_000.0
            val f64_hi_a: Double = +1_000_000_000_000_000.0

            val dragSpeed = 0.2f
            text("Drags:")
            checkbox("Clamp integers to 0..50", ::dragClamp)
            sameLine(); helpMarker(
                """As with every widgets in dear imgui, we never modify values unless there is a user interaction.
                You can override the clamping limits by using CTRL+Click to input a value.""".trimIndent())
            dragScalar("drag s8",         DataType.Byte,   ::s8_v,  dragSpeed, s8_zero.takeIf { dragClamp },  s8_fifty.takeIf { dragClamp })
            dragScalar("drag u8",         DataType.Ubyte,  ::u8_v,  dragSpeed, u8_zero.takeIf { dragClamp },  u8_fifty.takeIf { dragClamp }, "%d ms")
            dragScalar("drag s16",        DataType.Short,  ::s16_v, dragSpeed, s16_zero.takeIf { dragClamp }, s16_fifty.takeIf { dragClamp })
            dragScalar("drag u16",        DataType.Ushort, ::u16_v, dragSpeed, u16_zero.takeIf { dragClamp }, u16_fifty.takeIf { dragClamp }, "%d ms")
            dragScalar("drag s32",        DataType.Int,    ::s32_v, dragSpeed, s32_zero.takeIf { dragClamp }, s32_fifty.takeIf { dragClamp })
            dragScalar("drag u32",        DataType.Uint,   ::u32_v, dragSpeed, u32_zero.takeIf { dragClamp }, u32_fifty.takeIf { dragClamp }, "%d ms")
            dragScalar("drag s64",        DataType.Long,   ::s64_v, dragSpeed, s64_zero.takeIf { dragClamp }, s64_fifty.takeIf { dragClamp })
            dragScalar("drag u64",        DataType.Ulong,  ::u64_v, dragSpeed, u64_zero.takeIf { dragClamp }, u64_fifty.takeIf { dragClamp })
            dragScalar("drag float",      DataType.Float,  ::f32_v, 0.005f, f32_zero, f32_one, "%f")
            dragScalar("drag float log",  DataType.Float,  ::f32_v, 0.005f, f32_zero, f32_one, "%f", SliderFlag.Logarithmic.i)
            dragScalar("drag double",     DataType.Double, ::f64_v, 0.0005f, f64_zero, null, "%.10f grams")
            dragScalar("drag double log", DataType.Double, ::f64_v, 0.0005f, f64_zero, f64_one, "0 < %.10f < 1", SliderFlag.Logarithmic.i)

            text("Sliders")
            sliderScalar("slider s8 full",        DataType.Byte,   ::s8_v,  s8_min,   s8_max,    "%d")
            sliderScalar("slider u8 full",        DataType.Ubyte,  ::u8_v,  u8_min,   u8_max,    "%d")
            sliderScalar("slider s16 full",       DataType.Short,  ::s16_v, s16_min,  s16_max,   "%d")
            sliderScalar("slider u16 full",       DataType.Ushort, ::u16_v, u16_min,  u16_max,   "%d")
            sliderScalar("slider s32 low",        DataType.Int,    ::s32_v, s32_zero, s32_fifty, "%d")
            sliderScalar("slider s32 high",       DataType.Int,    ::s32_v, s32_hi_a, s32_hi_b,  "%d")
            sliderScalar("slider s32 full",       DataType.Int,    ::s32_v, s32_min,  s32_max,   "%d")
            sliderScalar("slider u32 low",        DataType.Uint,   ::u32_v, u32_zero, u32_fifty, "%d")
            sliderScalar("slider u32 high",       DataType.Uint,   ::u32_v, u32_hi_a, u32_hi_b,  "%d")
            sliderScalar("slider u32 full",       DataType.Uint,   ::u32_v, u32_min,  u32_max,   "%d")
            sliderScalar("slider s64 low",        DataType.Long,   ::s64_v, s64_zero, s64_fifty, "%d")
            sliderScalar("slider s64 high",       DataType.Long,   ::s64_v, s64_hi_a, s64_hi_b,  "%d")
            sliderScalar("slider s64 full",       DataType.Long,   ::s64_v, s64_min,  s64_max,   "%d")
            sliderScalar("slider u64 low",        DataType.Ulong,  ::u64_v, u64_zero, u64_fifty, "%d ms")
            sliderScalar("slider u64 high",       DataType.Ulong,  ::u64_v, u64_hi_a, u64_hi_b,  "%d ms")
            sliderScalar("slider u64 full",       DataType.Ulong,  ::u64_v, u64_min,  u64_max,   "%d ms")
            sliderScalar("slider float low",      DataType.Float,  ::f32_v, f32_zero, f32_one)
            sliderScalar("slider float low log",  DataType.Float,  ::f32_v, f32_zero, f32_one,   "%.10f", SliderFlag.Logarithmic.i)
            sliderScalar("slider float high",     DataType.Float,  ::f32_v, f32_lo_a, f32_hi_a,  "%e")
            sliderScalar("slider double low",     DataType.Double, ::f64_v, f64_zero, f64_one,   "%.10f grams")
            sliderScalar("slider double low log", DataType.Double, ::f64_v, f64_zero, f64_one,   "%.10f", SliderFlag.Logarithmic.i)
            sliderScalar("slider double high",    DataType.Double, ::f64_v, f64_lo_a, f64_hi_a,  "%e grams")

            text("Sliders (reverse)")
            sliderScalar("slider s8 reverse",  DataType.Byte,  ::s8_v,  s8_max,    s8_min,   "%d")
            sliderScalar("slider u8 reverse",  DataType.Ubyte, ::u8_v,  u8_max,    u8_min,   "%d") // [JVM] %u -> %d
            sliderScalar("slider s32 reverse", DataType.Int,   ::s32_v, s32_fifty, s32_zero, "%d")
            sliderScalar("slider u32 reverse", DataType.Uint,  ::u32_v, u32_fifty, u32_zero, "%s") // [JVM] %u -> %d
            sliderScalar("slider s64 reverse", DataType.Long,  ::s64_v, s64_fifty, s64_zero, "%d") // [JVM] %I64d -> %d
            sliderScalar("slider u64 reverse", DataType.Ulong, ::u64_v, u64_fifty, u64_zero, "%d ms") // [JVM] %I64u -> %d

            text("Inputs")
            checkbox("Show step buttons", ::inputsStep)
            inputScalar("input s8",      DataType.Byte,   ::s8_v,  s8_one.takeIf { inputsStep },  null, "%d")
            inputScalar("input u8",      DataType.Ubyte,  ::u8_v,  u8_one.takeIf { inputsStep },  null, "%d")
            inputScalar("input s16",     DataType.Short,  ::s16_v, s16_one.takeIf { inputsStep }, null, "%d")
            inputScalar("input u16",     DataType.Ushort, ::u16_v, u16_one.takeIf { inputsStep }, null, "%d")
            inputScalar("input s32",     DataType.Int,    ::s32_v, s32_one.takeIf { inputsStep }, null, "%d")
            inputScalar("input s32 hex", DataType.Int,    ::s32_v, s32_one.takeIf { inputsStep }, null, "%08X", Itf.CharsHexadecimal.i)
            inputScalar("input u32",     DataType.Uint,   ::u32_v, u32_one.takeIf { inputsStep }, null, "%d")
            inputScalar("input u32 hex", DataType.Uint,   ::u32_v, u32_one.takeIf { inputsStep }, null, "%08X", Itf.CharsHexadecimal.i)
            inputScalar("input s64",     DataType.Long,   ::s64_v, s64_one.takeIf { inputsStep })
            inputScalar("input u64",     DataType.Ulong,  ::u64_v, u64_one.takeIf { inputsStep })
            inputScalar("input float",   DataType.Float,  ::f32_v, f32_one.takeIf { inputsStep })
            inputScalar("input double",  DataType.Double, ::f64_v, f64_one.takeIf { inputsStep })
            // @formatter:on
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
                    val smallSliderSize = Vec2(18, floor((160f - (rows - 1) * spacing) / rows))
                    for (nx in 0..3) {
                        if (nx > 0) sameLine()
                        group {
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

            treeNode("Drag and drop in standard widgets") {
                // ColorEdit widgets automatically act as drag source and drag target.
                // They are using standardized payload strings IMGUI_PAYLOAD_TYPE_COLOR_3F and IMGUI_PAYLOAD_TYPE_COLOR_4F
                // to allow your own widgets to use colors in their drag and drop interaction.
                // Also see 'Demo->Widgets->Color/Picker Widgets->Palette' demo.
                helpMarker("You can drag from the colored squares.")
                colorEdit3("color 1", col3)
                colorEdit4("color 2", col4)
            }

            treeNode("Drag and drop to copy/swap items") {

                radioButton("Copy", mode == Mode.Copy) { mode = Mode.Copy }; sameLine()
                radioButton("Move", mode == Mode.Move) { mode = Mode.Move }; sameLine()
                radioButton("Swap", mode == Mode.Swap) { mode = Mode.Swap }
                names.forEachIndexed { n, name ->
                    pushID(n)
                    if ((n % 3) != 0) sameLine()
                    button(name, Vec2(60))

                    // Our buttons are both drag sources and drag targets here!
                    if (beginDragDropSource(DragDropFlag.None)) {
                        // Set payload to carry the index of our item (could be anything)
                        setDragDropPayload("DND_DEMO_CELL", n)

                        // Display preview (could be anything, e.g. when dragging an image we could decide to display
                        // the filename and a small preview of the image, etc.)
                        when (mode) {
                            Mode.Copy -> text("Copy $name")
                            Mode.Move -> text("Move $name")
                            Mode.Swap -> text("Swap $name")
                        }
                        endDragDropSource()
                    }
                    if (beginDragDropTarget()) {
                        acceptDragDropPayload("DND_DEMO_CELL")?.let { payload ->
//                            assert(payload.dataSize == Int.BYTES) [JVM] we don't use this field
                            val payloadN = payload.data!! as Int
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
                    popID()
                }
            }

            treeNode("Drag to reorder items (simple)") {
                // Simple reordering
                helpMarker(
                        "We don't use the drag and drop api at all here! " +
                                "Instead we query when the item is held but not hovered, and order items accordingly.")
                itemNames.forEachIndexed { n, item ->
                    selectable(item)

                    if (isItemActive && !isItemHovered()) {
                        val nNext = n + if (getMouseDragDelta(MouseButton.Left).y < 0f) -1 else 1
                        if (nNext in itemNames.indices) {
                            itemNames[n] = itemNames[nNext]
                            itemNames[nNext] = item
                            resetMouseDragDelta()
                        }
                    }
                }
            }
        }

        treeNode("Querying Status (Active/Focused/Hovered etc.)") {
            // Select an item type
            val itemNames = arrayOf(
                    "Text", "Button", "Button (w/ repeat)", "Checkbox", "SliderFloat", "InputText", "InputFloat",
                    "InputFloat3", "ColorEdit4", "MenuItem", "TreeNode", "TreeNode (w/ double-click)", "ListBox")
            combo("Item Type", ::itemType, itemNames, itemNames.size)
            sameLine()
            helpMarker("Testing how various types of items are interacting with the IsItemXXX functions.")

            // Submit selected item item so we can query their status in the code following it.
            val ret = when (itemType) {
                0 -> false.also { text("ITEM: Text") }   // Testing text items with no identifier/interaction
                1 -> button("ITEM: Button")   // Testing button
                2 -> withButtonRepeat(true) { button("ITEM: Button") } // Testing button (with repeater)
                3 -> checkbox("ITEM: Checkbox", ::b0) // Testing checkbox
                4 -> sliderFloat("ITEM: SliderFloat", col, 0, 0f, 1f)   // Testing basic item
                5 -> inputText("ITEM: InputText", str) // Testing input text (which handles tabbing)
                6 -> inputFloat("ITEM: InputFloat", col, 1f)  // Testing +/- buttons on scalar input
                7 -> inputFloat3("ITEM: InputFloat3", col)  // Testing multi-component items (IsItemXXX flags are reported merged)
                8 -> colorEdit4("ITEM: ColorEdit4", col)    // Testing multi-component items (IsItemXXX flags are reported merged)
                9 -> menuItem("ITEM: MenuItem") // Testing menu item (they use ImGuiButtonFlags_PressedOnRelease button policy)
                10 -> treeNode("ITEM: TreeNode").also { if (it) treePop() } // Testing tree node
                11 -> treeNodeEx("ITEM: TreeNode w/ ImGuiTreeNodeFlags_OpenOnDoubleClick", Tnf.OpenOnDoubleClick or Tnf.NoTreePushOnOpen)   // Testing tree node with ImGuiButtonFlags_PressedOnDoubleClick button policy.
                12 -> listBox("ITEM: ListBox", ::currentItem4, arrayOf("Apple", "Banana", "Cherry", "Kiwi"))
                else -> false
            }

            // Display the values of IsItemHovered() and other common item state functions.
            // Note that the ImGuiHoveredFlags_XXX flags can be combined.
            // Because BulletText is an item itself and that would affect the output of IsItemXXX functions,
            // we query every state in a single call to avoid storing them and to simplify the code.
            if (DEBUG)
                bulletText("""
                    Return value = $ret
                    isItemFocused = ${isItemFocused.i}
                    isItemHovered() = ${isItemHovered().i}
                    isItemHovered(AllowWhenBlockedByPopup) = ${isItemHovered(HoveredFlag.AllowWhenBlockedByPopup).i}
                    isItemHovered(AllowWhenBlockedByActiveItem) = ${isItemHovered(HoveredFlag.AllowWhenBlockedByActiveItem).i}
                    isItemHovered(AllowWhenOverlapped) = ${isItemHovered(HoveredFlag.AllowWhenOverlapped).i}
                    isItemHovered(RectOnly) = ${isItemHovered(HoveredFlag.RectOnly).i}
                    isItemActive = ${isItemActive.i}
                    isItemEdited = ${isItemEdited.i}
                    isItemActivated = ${isItemActivated.i}
                    isItemDeactivated = ${isItemDeactivated.i}
                    isItemDeactivatedAfterEdit = ${isItemDeactivatedAfterEdit.i}
                    isItemVisible = ${isItemVisible.i}
                    isItemClicked = ${isItemClicked().i}
                    IsItemToggledOpen = ${isItemToggledOpen.i}
                    GetItemRectMin() = (%.1f, %.1f)
                    GetItemRectMax() = (%.1f, %.1f)
                    GetItemRectSize() = (%.1f, %.1f)
                    """.trimIndent(), itemRectMin.x, itemRectMin.y, itemRectMax.x, itemRectMax.y, itemRectSize.x, itemRectSize.y)
            else
                bulletText("""
                    Return value = $ret
                    isItemFocused = $isItemFocused
                    isItemHovered() = ${isItemHovered().i}
                    isItemHovered(AllowWhenBlockedByPopup) = ${isItemHovered(HoveredFlag.AllowWhenBlockedByPopup)}
                    isItemHovered(AllowWhenBlockedByActiveItem) = ${isItemHovered(HoveredFlag.AllowWhenBlockedByActiveItem)}
                    isItemHovered(AllowWhenOverlapped) = ${isItemHovered(HoveredFlag.AllowWhenOverlapped)}
                    isItemHovered(RectOnly) = ${isItemHovered(HoveredFlag.RectOnly)}
                    isItemActive = $isItemActive
                    isItemEdited = $isItemEdited
                    isItemActivated = $isItemActivated
                    isItemDeactivated = $isItemDeactivated
                    isItemDeactivatedAfterEdit = $isItemDeactivatedAfterEdit
                    isItemVisible = $isItemVisible
                    isItemClicked = ${isItemClicked()}
                    IsItemToggledOpen = $isItemToggledOpen
                    GetItemRectMin() = (%.1f, %.1f)
                    GetItemRectMax() = (%.1f, %.1f)
                    GetItemRectSize() = (%.1f, %.1f)
                    """.trimIndent(), itemRectMin.x, itemRectMin.y, itemRectMax.x, itemRectMax.y, itemRectSize.x, itemRectSize.y)

            checkbox("Embed everything inside a child window (for additional testing)", ::embedAllInsideAChildWindow)
            if (embedAllInsideAChildWindow)
                beginChild("outer_child", Vec2(0, fontSize * 20f), true)

            // Testing IsWindowFocused() function with its various flags.
            // Note that the ImGuiFocusedFlags_XXX flags can be combined.
            bulletText(
                    "isWindowFocused() = ${isWindowFocused()}\n" +
                            "isWindowFocused(ChildWindows) = ${isWindowFocused(FocusedFlag.ChildWindows)}\n" +
                            "isWindowFocused(ChildWindows | RootWindow) = ${isWindowFocused(FocusedFlag.ChildWindows or FocusedFlag.RootWindow)}\n" +
                            "isWindowFocused(RootWindow) = ${isWindowFocused(FocusedFlag.RootWindow)}\n" +
                            "isWindowFocused(AnyWindow) = ${isWindowFocused(FocusedFlag.AnyWindow)}\n")

            // Testing IsWindowHovered() function with its various flags.
            // Note that the ImGuiHoveredFlags_XXX flags can be combined.
            bulletText(
                    "isWindowHovered() = ${isWindowHovered()}\n" +
                            "isWindowHovered(AllowWhenBlockedByPopup) = ${isWindowHovered(HoveredFlag.AllowWhenBlockedByPopup)}\n" +
                            "isWindowHovered(AllowWhenBlockedByActiveItem) = ${isWindowHovered(HoveredFlag.AllowWhenBlockedByActiveItem)}\n" +
                            "isWindowHovered(ChildWindows) = ${isWindowHovered(HoveredFlag.ChildWindows)}\n" +
                            "isWindowHovered(ChildWindows | RootWindow) = ${isWindowHovered(HoveredFlag.ChildWindows or HoveredFlag.RootWindow)}\n" +
                            "isWindowHovered(ChildWindows | AllowWhenBlockedByPopup) = ${isWindowHovered(HoveredFlag.ChildWindows or HoveredFlag.AllowWhenBlockedByPopup)}\n" +
                            "isWindowHovered(RootWindow) = ${isWindowHovered(HoveredFlag.RootWindow)}\n" +
                            "isWindowHovered(AnyWindow) = ${isWindowHovered(HoveredFlag.AnyWindow)}\n")

            beginChild("child", Vec2(0, 50), true)
            text("This is another child window for testing the _ChildWindows flag.")
            endChild()
            if (embedAllInsideAChildWindow)
                endChild()

            inputText("unused", unusedStr, Itf.ReadOnly.i)

            // Calling IsItemHovered() after begin returns the hovered status of the title bar.
            // This is useful in particular if you want to create a context menu associated to the title bar of a window.
            checkbox("Hovered/Active tests after Begin() for title bar testing", ::testWindow)
            if (testWindow) {
                begin("Title bar Hovered/Active tests", ::testWindow)
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