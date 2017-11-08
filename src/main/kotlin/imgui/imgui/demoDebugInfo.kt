package imgui.imgui

import gli_.has
import glm_.*
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec2.operators.div
import glm_.vec4.Vec4
import imgui.*
import imgui.Context.overlayDrawList
import imgui.Context.style
import imgui.ImGui._begin
import imgui.ImGui.alignTextToFramePadding
import imgui.ImGui.begin
import imgui.ImGui.beginChild
import imgui.ImGui.beginGroup
import imgui.ImGui.beginMainMenuBar
import imgui.ImGui.beginMenu
import imgui.ImGui.beginMenuBar
import imgui.ImGui.beginTooltip
import imgui.ImGui.bullet
import imgui.ImGui.bulletText
import imgui.ImGui.button
import imgui.ImGui.checkbox
import imgui.ImGui.closeCurrentPopup
import imgui.ImGui.colorEditVec4
import imgui.ImGui.columns
import imgui.ImGui.combo
import imgui.ImGui.cursorScreenPos
import imgui.ImGui.dragFloat
import imgui.ImGui.dummy
import imgui.ImGui.end
import imgui.ImGui.endChild
import imgui.ImGui.endGroup
import imgui.ImGui.endMainMenuBar
import imgui.ImGui.endMenu
import imgui.ImGui.endMenuBar
import imgui.ImGui.endTooltip
import imgui.ImGui.fontSize
import imgui.ImGui.image
import imgui.ImGui.imageButton
import imgui.ImGui.indent
import imgui.ImGui.inputFloat
import imgui.ImGui.isItemClicked
import imgui.ImGui.isItemHovered
import imgui.ImGui.isMouseDoubleClicked
import imgui.ImGui.isMouseHoveringRect
import imgui.ImGui.itemsLineHeightWithSpacing
import imgui.ImGui.logFinish
import imgui.ImGui.logToClipboard
import imgui.ImGui.menuItem
import imgui.ImGui.mousePos
import imgui.ImGui.newLine
import imgui.ImGui.nextColumn
import imgui.ImGui.openPopup
import imgui.ImGui.popFont
import imgui.ImGui.popId
import imgui.ImGui.popItemWidth
import imgui.ImGui.popStyleColor
import imgui.ImGui.popStyleVar
import imgui.ImGui.popTextWrapPos
import imgui.ImGui.pushFont
import imgui.ImGui.pushId
import imgui.ImGui.pushItemWidth
import imgui.ImGui.pushStyleColor
import imgui.ImGui.pushStyleVar
import imgui.ImGui.pushTextWrapPos
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.separator
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.setNextWindowSizeConstraints
import imgui.ImGui.setScrollHere
import imgui.ImGui.setWindowFontScale
import imgui.ImGui.setWindowSize
import imgui.ImGui.sliderFloat
import imgui.ImGui.sliderFloatVec2
import imgui.ImGui.sliderInt
import imgui.ImGui.smallButton
import imgui.ImGui.spacing
import imgui.ImGui.text
import imgui.ImGui.textColored
import imgui.ImGui.textDisabled
import imgui.ImGui.textUnformatted
import imgui.ImGui.textWrapped
import imgui.ImGui.treeNode
import imgui.ImGui.treeNodeExV
import imgui.ImGui.treeNodeToLabelSpacing
import imgui.ImGui.treePop
import imgui.ImGui.unindent
import imgui.ImGui.version
import imgui.ImGui.windowDrawList
import imgui.ImGui.windowWidth
import imgui.functionalProgramming.button
import imgui.functionalProgramming.collapsingHeader
import imgui.functionalProgramming.menu
import imgui.functionalProgramming.menuBar
import imgui.functionalProgramming.popupModal
import imgui.functionalProgramming.smallButton
import imgui.functionalProgramming.tooltip
import imgui.functionalProgramming.treeNode
import imgui.functionalProgramming.window
import imgui.functionalProgramming.withId
import imgui.functionalProgramming.withItemWidth
import imgui.functionalProgramming.withStyleVar
import imgui.functionalProgramming.withWindow
import imgui.internal.Rect
import imgui.internal.Window
import imgui.or
import java.util.*
import kotlin.reflect.KMutableProperty0
import imgui.ColorEditFlags as Cef
import imgui.Context as g
import imgui.InputTextFlags as Itf
import imgui.SelectableFlags as Sf
import imgui.TreeNodeFlags as Tnf
import imgui.WindowFlags as Wf

private var b = false

/**
 *  Message to the person tempted to delete this file when integrating ImGui into their code base:
 *  Do NOT remove this file from your project! It is useful reference code that you and other users will want to refer to.
 *  Don't do it! Do NOT remove this file from your project! It is useful reference code that you and other users will want to refer to.
 *  Everything in this file will be stripped out by the linker if you don't call ImGui::ShowTestWindow().
 *  During development, you can call ImGui::ShowTestWindow() in your code to learn about various features of ImGui.
 *  Removing this file from your project is hindering your access to documentation, likely leading you to poorer usage of the library.
 *  During development, you can call ImGui::ShowTestWindow() in your code to learn about various features of ImGui. Have it wired in a debug menu!
 *  Removing this file from your project is hindering access to documentation for everyone in your team, likely leading you to poorer usage of the library.
 *
 *  Note that you can #define IMGUI_DISABLE_TEST_WINDOWS in imconfig.h for the same effect.
 *  If you want to link core ImGui in your public builds but not those test windows, #define IMGUI_DISABLE_TEST_WINDOWS in imconfig.h and those functions will be empty.
 *  For any other case, if you have ImGui available you probably want this to be available for reference and execution.
 *
 *  Thank you,
 *  -Your beloved friend, imgui_demo.cpp (that you won't delete)
 */
interface imgui_demoDebugInfo {
    /** Create demo/test window.
     *  Demonstrate most ImGui features (big function!)
     *  Call this to learn about the library! try to make it always available in your application!   */
    fun showTestWindow(open: BooleanArray) {
        //val b = open[0]
        b = open[0]
        showTestWindow(::b)
        open[0] = b
    }

    fun showTestWindow(open: KMutableProperty0<Boolean>) {

        if (showApp.mainMenuBar) showExampleAppMainMenuBar()
        if (showApp.console) showExampleAppConsole(showApp::console)
        if (showApp.log) showExampleAppLog(showApp::log)
        if (showApp.layout) showExampleAppLayout(showApp::layout)
        if (showApp.propertyEditor) showExampleAppPropertyEditor(showApp::propertyEditor)
        if (showApp.longText) showExampleAppLongText(showApp::longText)
        if (showApp.autoResize) showExampleAppAutoResize(showApp::autoResize)
        if (showApp.constrainedResize) showExampleAppConstrainedResize(showApp::constrainedResize)
        if (showApp.fixedOverlay) showExampleAppFixedOverlay(showApp::fixedOverlay)
        if (showApp.manipulatingWindowTitle) showExampleAppManipulatingWindowTitle(showApp::manipulatingWindowTitle)
        if (showApp.customRendering[0]) showExampleAppCustomRendering(showApp.customRendering)
        if (showApp.metrics[0]) ImGui.showMetricsWindow(showApp.metrics)
        if (showApp.styleEditor)
            window("Style Editor", showApp::styleEditor) { showStyleEditor() }

        if (showApp.about[0])
            withWindow("About ImGui", showApp.about, Wf.AlwaysAutoResize.i) {
                text("JVM ImGui, $version")
                separator()
                text("Original by Omar Cornut, ported by Giuseppe Barbieri and all github contributors.")
                text("ImGui is licensed under the MIT License, see LICENSE for more information.")
            }

        // Demonstrate the various window flags. Typically you would just use the default.
        var windowFlags = 0
        if (noTitlebar[0]) windowFlags = windowFlags or Wf.NoTitleBar
        if (!noBorder[0]) windowFlags = windowFlags or Wf.ShowBorders
        if (noResize[0]) windowFlags = windowFlags or Wf.NoResize
        if (noMove[0]) windowFlags = windowFlags or Wf.NoMove
        if (noScrollbar[0]) windowFlags = windowFlags or Wf.NoScrollbar
        if (noCollapse[0]) windowFlags = windowFlags or Wf.NoCollapse
        if (!noMenu[0]) windowFlags = windowFlags or Wf.MenuBar
        setNextWindowSize(Vec2(550, 680), Cond.FirstUseEver)
        if (!_begin("ImGui Demo", open, windowFlags)) {
            end()   // Early out if the window is collapsed, as an optimization.
            return
        }

        //pushItemWidth(getWindowWidth() * 0.65f);    // 2/3 of the space for widget and 1/3 for labels
        pushItemWidth(-140f) // Right align, keep 140 pixels for labels

        text("dear imgui says hello. ($version)")

        // Menu
        menuBar {
            if (beginMenu("Menu")) {
                showExampleMenuFile()
                endMenu()
            }
            menu("Examples") {
                menuItem("Main menu bar", "", showApp.mainMenuBar)
                menuItem("Console", "", showApp::console)
                menuItem("Log", "", showApp::log)
                menuItem("Simple layout", "", showApp::layout)
                menuItem("Property editor", "", showApp::propertyEditor)
                menuItem("Long text display", "", showApp::longText)
                menuItem("Auto-resizing window", "", showApp::autoResize)
                menuItem("Constrained-resizing window", "", showApp::constrainedResize)
                menuItem("Simple overlay", "", showApp::fixedOverlay)
                menuItem("Manipulating window title", "", showApp.manipulatingWindowTitle)
                menuItem("Custom rendering", "", showApp.customRendering)
            }
            menu("Help") {
                menuItem("Metrics", "", showApp.metrics)
                menuItem("Style Editor", "", showApp::styleEditor)
                menuItem("About ImGui", "", showApp.about)
            }
        }

        spacing()
        collapsingHeader("Help") {
            textWrapped("This window is being created by the ShowTestWindow() function. Please refer to the code " +
                    "for programming reference.\n\nUser Guide:")
            showUserGuide()
        }

        collapsingHeader("Window options") {

            checkbox("No titlebar", noTitlebar); sameLine(150f)
            checkbox("No border", noBorder); sameLine(300f)
            checkbox("No resize", noResize)
            checkbox("No move", noMove); sameLine(150f)
            checkbox("No scrollbar", noScrollbar); sameLine(300f)
            checkbox("No collapse", noCollapse)
            checkbox("No menu", noMenu)

            treeNode("Style") { showStyleEditor() }

            treeNode("Logging") {
                textWrapped("The logging API redirects all text output so you can easily capture the content of a " +
                        "window or a block. Tree nodes can be automatically expanded. You can also call LogText() to " +
                        "output directly to the log without a visual output.")
//                logButtons() TODO
            }
        }

        collapsingHeader("Widgets") {
            //            if (ImGui::TreeNode("Basic"))
//                {
//                        static int clicked = 0;
//                        if (ImGui::Button("Button"))
//                                clicked++;
//                        if (clicked & 1)
//                        {
//                                ImGui::SameLine();
//                                ImGui::Text("Thanks for clicking me!");
//                            }
//                    +
//                        static bool check = true;
//                        ImGui::Checkbox("checkbox", &check);
//                    +
//                        static int e = 0;
//                        ImGui::RadioButton("radio a", &e, 0); ImGui::SameLine();
//                        ImGui::RadioButton("radio b", &e, 1); ImGui::SameLine();
//                        ImGui::RadioButton("radio c", &e, 2);
//                    +
//                        // Color buttons, demonstrate using PushID() to add unique identifier in the ID stack, and changing style.
//                        for (int i = 0; i < 7; i++)
//                        {
//                                if (i > 0) ImGui::SameLine();
//                                ImGui::PushID(i);
//                                ImGui::PushStyleColor(ImGuiCol_Button, (ImVec4)ImColor::HSV(i/7.0f, 0.6f, 0.6f));
//                                ImGui::PushStyleColor(ImGuiCol_ButtonHovered, (ImVec4)ImColor::HSV(i/7.0f, 0.7f, 0.7f));
//                                ImGui::PushStyleColor(ImGuiCol_ButtonActive, (ImVec4)ImColor::HSV(i/7.0f, 0.8f, 0.8f));
//                                ImGui::Button("Click");
//                                ImGui::PopStyleColor(3);
//                                ImGui::PopID();
//                            }
//                    +
//                        ImGui::Text("Hover over me");
//                        if (ImGui::IsItemHovered())
//                                ImGui::SetTooltip("I am a tooltip");
//                    +
//                        ImGui::SameLine();
//                        ImGui::Text("- or me");
//                        if (ImGui::IsItemHovered())
//                            {
//                                    ImGui::BeginTooltip();
//                                    ImGui::Text("I am a fancy tooltip");
//                                    static float arr[] = { 0.6f, 0.1f, 1.0f, 0.5f, 0.92f, 0.1f, 0.2f };
//                                    ImGui::PlotLines("Curve", arr, IM_ARRAYSIZE(arr));
//                                    ImGui::EndTooltip();
//                                }
//                    +
//                        // Testing IMGUI_ONCE_UPON_A_FRAME macro
//                        //for (int i = 0; i < 5; i++)
//                        //{
//                        //  IMGUI_ONCE_UPON_A_FRAME
//                        //  {
//                        //      ImGui::Text("This will be displayed only once.");
//                        //  }
//                        //}
//                    +
//                        ImGui::Separator();
//                    +
//                        ImGui::LabelText("label", "Value");
//                    +
//                        static int item = 1;
//                        ImGui::Combo("combo", &item, "aaaa\0bbbb\0cccc\0dddd\0eeee\0\0");   // Combo using values packed in a single constant string (for really quick combo)
//                    +
//                        const char* items[] = { "AAAA", "BBBB", "CCCC", "DDDD", "EEEE", "FFFF", "GGGG", "HHHH", "IIII", "JJJJ", "KKKK" };
//                        static int item2 = -1;
//                        ImGui::Combo("combo scroll", &item2, items, IM_ARRAYSIZE(items));   // Combo using proper array. You can also pass a callback to retrieve array value, no need to create/copy an array just for that.
//                    +
//                        {
//                                static char str0[128] = "Hello, world!";
//                                static int i0=123;
//                                static float f0=0.001f;
//                                ImGui::InputText("input text", str0, IM_ARRAYSIZE(str0));
//                                ImGui::SameLine(); ShowHelpMarker("Hold SHIFT or use mouse to select text.\n" "CTRL+Left/Right to word jump.\n" "CTRL+A or double-click to select all.\n" "CTRL+X,CTRL+C,CTRL+V clipboard.\n" "CTRL+Z,CTRL+Y undo/redo.\n" "ESCAPE to revert.\n");
//                        +
//                                ImGui::InputInt("input int", &i0);
//                                ImGui::SameLine(); ShowHelpMarker("You can apply arithmetic operators +,*,/ on numerical values.\n  e.g. [ 100 ], input \'*2\', result becomes [ 200 ]\nUse +- to subtract.\n");
//                        +
//                                ImGui::InputFloat("input float", &f0, 0.01f, 1.0f);
//                        +
//                                static float vec4a[4] = { 0.10f, 0.20f, 0.30f, 0.44f };
//                                ImGui::InputFloat3("input float3", vec4a);
//                            }
//                    +
//                        {
//                                static int i1=50, i2=42;
//                                ImGui::DragInt("drag int", &i1, 1);
//                                ImGui::SameLine(); ShowHelpMarker("Click and drag to edit value.\nHold SHIFT/ALT for faster/slower edit.\nDouble-click or CTRL+click to input value.");
//                        +
//                                ImGui::DragInt("drag int 0..100", &i2, 1, 0, 100, "%.0f%%");
//                        +
//                                static float f1=1.00f, f2=0.0067f;
//                                ImGui::DragFloat("drag float", &f1, 0.005f);
//                                ImGui::DragFloat("drag small float", &f2, 0.0001f, 0.0f, 0.0f, "%.06f ns");
//                            }
//                    +
//                        {
//                                static int i1=0;
//                                ImGui::SliderInt("slider int", &i1, -1, 3);
//                                ImGui::SameLine(); ShowHelpMarker("CTRL+click to input value.");
//                        +
//                                static float f1=0.123f, f2=0.0f;
//                                ImGui::SliderFloat("slider float", &f1, 0.0f, 1.0f, "ratio = %.3f");
//                                ImGui::SliderFloat("slider log float", &f2, -10.0f, 10.0f, "%.4f", 3.0f);
//                                static float angle = 0.0f;
//                                ImGui::SliderAngle("slider angle", &angle);
//                            }
//                    +
//                        static float col1[3] = { 1.0f,0.0f,0.2f };
//                        static float col2[4] = { 0.4f,0.7f,0.0f,0.5f };
//                        ImGui::ColorEdit3("color 1", col1);
//                        ImGui::SameLine(); ShowHelpMarker("Click on the colored square to open a color picker.\nRight-click on the colored square to show options.\nCTRL+click on individual component to input value.\n");
//                    +
//                        ImGui::ColorEdit4("color 2", col2);
//                    +
//                        const char* listbox_items[] = { "Apple", "Banana", "Cherry", "Kiwi", "Mango", "Orange", "Pineapple", "Strawberry", "Watermelon" };
//                        static int listbox_item_current = 1;
//                        ImGui::ListBox("listbox\n(single select)", &listbox_item_current, listbox_items, IM_ARRAYSIZE(listbox_items), 4);
//                    +
//                        //static int listbox_item_current2 = 2;
//                        //ImGui::PushItemWidth(-1);
//                        //ImGui::ListBox("##listbox2", &listbox_item_current2, listbox_items, IM_ARRAYSIZE(listbox_items), 4);
//                        //ImGui::PopItemWidth();
//                    +
//                        ImGui::TreePop();
//                    }

            treeNode("Trees") {

                treeNode("Basic trees") {

                    for (i in 0..4)
                        treeNode(i, "Child $i") {
                            text("blah blah")
                            sameLine()
                            smallButton("print") { println("Child $i pressed") }
                        }
                }

                treeNode("Advanced, with Selectable nodes") {

                    showHelpMarker("""|This is a more standard looking tree with selectable nodes.
                        |Click to select, CTRL+Click to toggle, click on arrows or double-click to open.""".trimMargin())
                    checkbox("Align label with current X position)", alignLabelWithCurrentXposition)
                    text("Hello!")
                    if (alignLabelWithCurrentXposition[0]) unindent(treeNodeToLabelSpacing)

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
                    if (alignLabelWithCurrentXposition[0]) indent(treeNodeToLabelSpacing)
                }
            }

            treeNode("Collapsing Headers") {
                checkbox("Enable extra group", closableGroup)
                collapsingHeader("Header") {
                    text("IsItemHovered: ${isItemHovered()}")
                    for (i in 0..4) text("Some content $i")
                }
                collapsingHeader("Header with a close button", closableGroup) {
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

//                    static float wrap_width = 200.0f
//                    ImGui::SliderFloat("Wrap width", & wrap_width, -20, 600, "%.0f")
//                    +
//                    ImGui::Text("Test paragraph 1:")
//                    ImVec2 pos = ImGui ::GetCursorScreenPos()
//                    ImGui::GetWindowDrawList()->AddRectFilled(ImVec2(pos.x+wrap_width, pos.y), ImVec2(pos.x+wrap_width+10, pos.y+ImGui::GetTextLineHeight()), IM_COL32(255, 0, 255, 255))
//                    ImGui::PushTextWrapPos(ImGui::GetCursorPos().x + wrap_width)
//                    ImGui::Text("The lazy dog is a good dog. This paragraph is made to fit within %.0f pixels. Testing a 1 character word. The quick brown fox jumps over the lazy dog.", wrap_width)
//                    ImGui::GetWindowDrawList()->AddRect(ImGui::GetItemRectMin(), ImGui::GetItemRectMax(), IM_COL32(255, 255, 0, 255))
//                    ImGui::PopTextWrapPos()
//                    +
//                    ImGui::Text("Test paragraph 2:")
//                    pos = ImGui::GetCursorScreenPos()
//                    ImGui::GetWindowDrawList()->AddRectFilled(ImVec2(pos.x+wrap_width, pos.y), ImVec2(pos.x+wrap_width+10, pos.y+ImGui::GetTextLineHeight()), IM_COL32(255, 0, 255, 255))
//                    ImGui::PushTextWrapPos(ImGui::GetCursorPos().x + wrap_width)
//                    ImGui::Text("aaaaaaaa bbbbbbbb, c cccccccc,dddddddd. d eeeeeeee   ffffffff. gggggggg!hhhhhhhh")
//                    ImGui::GetWindowDrawList()->AddRect(ImGui::GetItemRectMin(), ImGui::GetItemRectMax(), IM_COL32(255, 255, 0, 255))
//                    ImGui::PopTextWrapPos()
                }
            }

            treeNode("UTF-8 Text TODO") {
                /*  UTF-8 test with Japanese characters
                    (needs a suitable font, try Arial Unicode or M+ fonts
                    http://mplus-fonts.sourceforge.jp/mplus-outline-fonts/index-en.html)
                    Most compiler appears to support UTF-8 in source code (with Visual Studio you need to save your file as
                    'UTF-8 without signature')
                    However for the sake for maximum portability here we are *not* including raw UTF-8 character in this source file,
                    instead we encode the string with hexadecimal constants.
                    In your own application be reasonable and use UTF-8 in source or retrieve the data from file system!
                    Note that characters values are preserved even if the font cannot be displayed, so you can safely copy & paste
                    garbled characters into another application.    */
                textWrapped("CJK text will only appears if the font was loaded with the appropriate CJK character ranges. " +
                        "Call io.Font->LoadFromFileTTF() manually to load extra character ranges.")
//    TODO            text("Hiragana: \u00e3\u0081\u008b\u00e3\u0081\u008d\u00e3\u0081\u008f\u00e3\u0081\u0091\u00e3\u0081\u0093 (kakikukeko)")
//                text("Kanjis: \u00e6\u0097\u00a5\u00e6\u009c\u00ac\u00e8\u00aa\u009e (nihongo)")
//                inputText("UTF-8 input", buf, buf.size)
            }

            treeNode("Images") {
                textWrapped("Below we are displaying the font texture (which is the only texture we have access to in this " +
                        "demo). Use the 'ImTextureID' type as storage to pass pointers or identifier to your own texture data. Hover " +
                        "the texture for a zoomed view!")
                val texScreenPos = Vec2(cursorScreenPos)
                val texSize = Vec2(IO.fonts.texSize)
                val texId = IO.fonts.texId
                text("%.0fx%.0f", texSize.x, texSize.y)
                image(texId, texSize, Vec2(), Vec2(1), Vec4.fromColor(255, 255, 255, 255), Vec4.fromColor(255, 255, 255, 128))
                if (isItemHovered())
                    tooltip {
                        val focusSz = 32f
                        val focus = glm.clamp(mousePos - texScreenPos - focusSz * 0.5f, Vec2(), texSize - focusSz)
                        text("Min: (%.2f, %.2f)", focus.x, focus.y)
                        text("Max: (%.2f, %.2f)", focus.x + focusSz, focus.y + focusSz)
                        val uv0 = focus / texSize
                        val uv1 = (focus + focusSz) / texSize
                        image(texId, Vec2(128), uv0, uv1, Vec4.fromColor(255, 255, 255, 255), Vec4.fromColor(255, 255, 255, 128))
                    }
                textWrapped("And now some textured buttons..")
                for (i in 0..7)
                    withId(i) {
                        val framePadding = -1 + i  // -1 = uses default padding
                        if (imageButton(texId, Vec2(32, 32), Vec2(), 32 / texSize, framePadding, Vec4.fromColor(0, 0, 0, 255)))
                            pressedCount++
                        sameLine()
                    }
                newLine()
                text("Pressed $pressedCount times.")
            }

            var offset = 0
            treeNode("Selectables") {
                treeNode("Basic") {
                    selectable("1. I am selectable", selected, 0)
                    selectable("2. I am selectable", selected, 1)
                    text("3. I am not selectable")
                    selectable("4. I am selectable", selected, 2)
                    if (selectable("5. I am double clickable", selected[3], Sf.AllowDoubleClick.i))
                        if (isMouseDoubleClicked(0))
                            selected[3] = !selected[3]
                }
                offset += 4
                treeNode("Rendering more text into the same block") {
                    selectable("main.c", selected, offset + 0); sameLine(300f); text(" 2,345 bytes")
                    selectable("Hello.cpp", selected, offset + 1); sameLine(300f); text("12,345 bytes")
                    selectable("Hello.h", selected, offset + 2); sameLine(300f); text(" 2,345 bytes")
                }
                offset += 3
                treeNode("In columns") {
                    columns(3, null, false)
                    for (i in 0..15) {
                        if (selectable("Item $i", selected, offset + i)) Unit
                        nextColumn()
                    }
                    columns(1)
                }
                offset += 16
                treeNode("Grid") {
                    for (i in 0..15)
                        withId(offset + i) {
                            if (selectable("Sailor", selected, offset + i, 0, Vec2(50))) {
                                val x = i % 4
                                val y = i / 4
                                when {
                                    x > 0 -> selected[offset + i - 1] = selected[offset + i - 1] xor true
                                    x < 3 -> selected[offset + i + 1] = selected[offset + i + 1] xor true
                                    y > 0 -> selected[offset + i - 4] = selected[offset + i + 1] xor true
                                    y < 3 -> selected[offset + i + 4] = selected[offset + i + 1] xor true
                                }
                            }
                            if ((i % 4) < 3) sameLine()
                        }
                }
            }

            treeNode("Filtered Text Input TODO") {
                //                static char buf1[64] = ""; ImGui::InputText("default", buf1, 64);
//                static char buf2[64] = ""; ImGui::InputText("decimal", buf2, 64, ImGuiInputTextFlags_CharsDecimal);
//                static char buf3[64] = ""; ImGui::InputText("hexadecimal", buf3, 64, ImGuiInputTextFlags_CharsHexadecimal | ImGuiInputTextFlags_CharsUppercase);
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
                    checkbox("Read-only", readOnly)
                }
                val flags = Itf.AllowTabInput or if (readOnly[0]) Itf.ReadOnly else Itf.Null
//                inputTextMultiline("##source", textMultiline, Vec2(-1f, textLineHeight * 16), flags)
            }
//
//            if (ImGui::TreeNode("Plots widgets"))
//            {
//                static bool animate = true;
//                ImGui::Checkbox("Animate", &animate);
//
//                static float arr[] = { 0.6f, 0.1f, 1.0f, 0.5f, 0.92f, 0.1f, 0.2f };
//                ImGui::PlotLines("Frame Times", arr, IM_ARRAYSIZE(arr));
//
//                // Create a dummy array of contiguous float values to plot
//                // Tip: If your float aren't contiguous but part of a structure, you can pass a pointer to your first float and the sizeof() of your structure in the Stride parameter.
//                static float values[90] = { 0 };
//                static int values_offset = 0;
//                static float refresh_time = 0.0f;
//                if (!animate || refresh_time == 0.0f)
//                        refresh_time = ImGui::GetTime();
//                while (refresh_time < ImGui::GetTime()) // Create dummy data at fixed 60 hz rate for the demo
//                    {
//                            static float phase = 0.0f;
//                            values[values_offset] = cosf(phase);
//                            values_offset = (values_offset+1) % IM_ARRAYSIZE(values);
//                            phase += 0.10f*values_offset;
//                            refresh_time += 1.0f/60.0f;
//                        }
//                ImGui::PlotLines("Lines", values, IM_ARRAYSIZE(values), values_offset, "avg 0.0", -1.0f, 1.0f, ImVec2(0,80));
//                ImGui::PlotHistogram("Histogram", arr, IM_ARRAYSIZE(arr), 0, NULL, 0.0f, 1.0f, ImVec2(0,80));
//
//                // Use functions to generate output
//                // FIXME: This is rather awkward because current plot API only pass in indices. We probably want an API passing floats and user provide sample rate/count.
//                struct Funcs
//                        {
//                                static float Sin(void*, int i) { return sinf(i * 0.1f); }
//                                static float Saw(void*, int i) { return (i & 1) ? 1.0f : -1.0f; }
//                            };
//                static int func_type = 0, display_count = 70;
//                ImGui::Separator();
//                ImGui::PushItemWidth(100); ImGui::Combo("func", &func_type, "Sin\0Saw\0"); ImGui::PopItemWidth();
//                ImGui::SameLine();
//                ImGui::SliderInt("Sample count", &display_count, 1, 400);
//                float (*func)(void*, int) = (func_type == 0) ? Funcs::Sin : Funcs::Saw;
//                ImGui::PlotLines("Lines", func, NULL, display_count, 0, NULL, -1.0f, 1.0f, ImVec2(0,80));
//                ImGui::PlotHistogram("Histogram", func, NULL, display_count, 0, NULL, -1.0f, 1.0f, ImVec2(0,80));
//                ImGui::Separator();
//
//                // Animate a simple progress bar
//                static float progress = 0.0f, progress_dir = 1.0f;
//                if (animate)
//                    {
//                            progress += progress_dir * 0.4f * ImGui::GetIO().DeltaTime;
//                            if (progress >= +1.1f) { progress = +1.1f; progress_dir *= -1.0f; }
//                            if (progress <= -0.1f) { progress = -0.1f; progress_dir *= -1.0f; }
//                        }
//
//                // Typically we would use ImVec2(-1.0f,0.0f) to use all available width, or ImVec2(width,0.0f) for a specified width. ImVec2(0.0f,0.0f) uses ItemWidth.
//                ImGui::ProgressBar(progress, ImVec2(0.0f,0.0f));
//                ImGui::SameLine(0.0f, ImGui::GetStyle().ItemInnerSpacing.x);
//                ImGui::Text("Progress Bar");
//
//                float progress_saturated = (progress < 0.0f) ? 0.0f : (progress > 1.0f) ? 1.0f : progress;
//                char buf[32];
//                sprintf(buf, "%d/%d", (int)(progress_saturated*1753), 1753);
//                ImGui::ProgressBar(progress, ImVec2(0.f,0.f), buf);
//                ImGui::TreePop();
//            }
//
//            if (ImGui::TreeNode("Color/Picker Widgets"))
//            {
//                static ImVec4 color = ImColor(114, 144, 154, 200);
//
//                static bool hdr = false;
//                static bool alpha_preview = true;
//                static bool alpha_half_preview = false;
//                static bool options_menu = true;
//                ImGui::Checkbox("With HDR", &hdr); ImGui::SameLine(); ShowHelpMarker("Currently all this does is to lift the 0..1 limits on dragging widgets.");
//                ImGui::Checkbox("With Alpha Preview", &alpha_preview);
//                ImGui::Checkbox("With Half Alpha Preview", &alpha_half_preview);
//                ImGui::Checkbox("With Options Menu", &options_menu); ImGui::SameLine(); ShowHelpMarker("Right-click on the individual color widget to show options.");
//                int misc_flags = (hdr ? ImGuiColorEditFlags_HDR : 0) | (alpha_half_preview ? ImGuiColorEditFlags_AlphaPreviewHalf : (alpha_preview ? ImGuiColorEditFlags_AlphaPreview : 0)) | (options_menu ? 0 : ImGuiColorEditFlags_NoOptions);
//
//                ImGui::Text("Color widget:");
//                ImGui::SameLine(); ShowHelpMarker("Click on the colored square to open a color picker.\nCTRL+click on individual component to input value.\n");
//                ImGui::ColorEdit3("MyColor##1", (float*)&color, misc_flags);
//
//                ImGui::Text("Color widget HSV with Alpha:");
//                ImGui::ColorEdit4("MyColor##2", (float*)&color, ImGuiColorEditFlags_HSV | misc_flags);
//
//                ImGui::Text("Color widget with Float Display:");
//                ImGui::ColorEdit4("MyColor##2f", (float*)&color, ImGuiColorEditFlags_Float | misc_flags);
//
//                ImGui::Text("Color button with Picker:");
//                ImGui::SameLine(); ShowHelpMarker("With the ImGuiColorEditFlags_NoInputs flag you can hide all the slider/text inputs.\nWith the ImGuiColorEditFlags_NoLabel flag you can pass a non-empty label which will only be used for the tooltip and picker popup.");
//                ImGui::ColorEdit4("MyColor##3", (float*)&color, ImGuiColorEditFlags_NoInputs | ImGuiColorEditFlags_NoLabel | misc_flags);
//
//                ImGui::Text("Color button with Custom Picker Popup:");
//                static bool saved_palette_inited = false;
//                static ImVec4 saved_palette[32];
//                static ImVec4 backup_color;
//                if (!saved_palette_inited)
//                        for (int n = 0; n < IM_ARRAYSIZE(saved_palette); n++)
//                        ImGui::ColorConvertHSVtoRGB(n / 31.0f, 0.8f, 0.8f, saved_palette[n].x, saved_palette[n].y, saved_palette[n].z);
//                bool open_popup = ImGui::ColorButton("MyColor##3b", color, misc_flags);
//                ImGui::SameLine();
//                open_popup |= ImGui::Button("Palette");
//                if (open_popup)
//                    {
//                            ImGui::OpenPopup("mypicker");
//                            backup_color = color;
//                        }
//                if (ImGui::BeginPopup("mypicker"))
//                    {
//                            // FIXME: Adding a drag and drop example here would be perfect!
//                            ImGui::Text("MY CUSTOM COLOR PICKER WITH AN AMAZING PALETTE!");
//                            ImGui::Separator();
//                            ImGui::ColorPicker4("##picker", (float*)&color, misc_flags | ImGuiColorEditFlags_NoSidePreview | ImGuiColorEditFlags_NoSmallPreview);
//                            ImGui::SameLine();
//                            ImGui::BeginGroup();
//                            ImGui::Text("Current");
//                            ImGui::ColorButton("##current", color, ImGuiColorEditFlags_NoPicker | ImGuiColorEditFlags_AlphaPreviewHalf, ImVec2(60,40));
//                            ImGui::Text("Previous");
//                            if (ImGui::ColorButton("##previous", backup_color, ImGuiColorEditFlags_NoPicker | ImGuiColorEditFlags_AlphaPreviewHalf, ImVec2(60,40)))
//                                color = backup_color;
//                            ImGui::Separator();
//                            ImGui::Text("Palette");
//                            for (int n = 0; n < IM_ARRAYSIZE(saved_palette); n++)
//                            {
//                                    ImGui::PushID(n);
//                                    if ((n % 8) != 0)
//                                            ImGui::SameLine(0.0f, ImGui::GetStyle().ItemSpacing.y);
//                                    if (ImGui::ColorButton("##palette", saved_palette[n], ImGuiColorEditFlags_NoPicker | ImGuiColorEditFlags_NoTooltip, ImVec2(20,20)))
//                                        color = ImVec4(saved_palette[n].x, saved_palette[n].y, saved_palette[n].z, color.w); // Preserve alpha!
//                                    ImGui::PopID();
//                                }
//                            ImGui::EndGroup();
//                            ImGui::EndPopup();
//                        }
//
//                        ImGui::Text("Color button only:");
//                        ImGui::ColorButton("MyColor##3c", *(ImVec4*)&color, misc_flags, ImVec2(80,80));
//                        +
//                        ImGui::Text("Color picker:");
//                        static bool alpha = true;
//                        static bool alpha_bar = true;
//                        static bool side_preview = true;
//                        static bool ref_color = false;
//                        static ImVec4 ref_color_v(1.0f,0.0f,1.0f,0.5f);
//                        static int inputs_mode = 2;
//                        static int picker_mode = 0;
//                        ImGui::Checkbox("With Alpha", &alpha);
//                        ImGui::Checkbox("With Alpha Bar", &alpha_bar);
//                        ImGui::Checkbox("With Side Preview", &side_preview);
//                        if (side_preview)
//                            {
//                                    ImGui::SameLine();
//                                    ImGui::Checkbox("With Ref Color", &ref_color);
//                                    if (ref_color)
//                                        {
//                                                ImGui::SameLine();
//                                                ImGui::ColorEdit4("##RefColor", &ref_color_v.x, ImGuiColorEditFlags_NoInputs | misc_flags);
//                                            }
//                                }
//                        ImGui::Combo("Inputs Mode", &inputs_mode, "All Inputs\0No Inputs\0RGB Input\0HSV Input\0HEX Input\0");
//                        ImGui::Combo("Picker Mode", &picker_mode, "Auto/Current\0Hue bar + SV rect\0Hue wheel + SV triangle\0");
//                        ImGui::SameLine(); ShowHelpMarker("User can right-click the picker to change mode.");
//                        ImGuiColorEditFlags flags = misc_flags;
//                        if (!alpha) flags |= ImGuiColorEditFlags_NoAlpha; // This is by default if you call ColorPicker3() instead of ColorPicker4()
//                        if (alpha_bar) flags |= ImGuiColorEditFlags_AlphaBar;
//                        if (!side_preview) flags |= ImGuiColorEditFlags_NoSidePreview;
//                        if (picker_mode == 1) flags |= ImGuiColorEditFlags_PickerHueBar;
//                        if (picker_mode == 2) flags |= ImGuiColorEditFlags_PickerHueWheel;
//                        if (inputs_mode == 1) flags |= ImGuiColorEditFlags_NoInputs;
//                        if (inputs_mode == 2) flags |= ImGuiColorEditFlags_RGB;
//                        if (inputs_mode == 3) flags |= ImGuiColorEditFlags_HSV;
//                        if (inputs_mode == 4) flags |= ImGuiColorEditFlags_HEX;
//                        ImGui::ColorPicker4("MyColor##4", (float*)&color, flags, ref_color ? &ref_color_v.x : NULL);
//                        +
//                        ImGui::Text("Programmatically set defaults/options:");
//                        ImGui::SameLine(); ShowHelpMarker("SetColorEditOptions() is designed to allow you to set boot-time default.\nWe don't have Push/Pop functions because you can force options on a per-widget basis if needed, and the user can change non-forced ones with the options menu.\nWe don't have a getter to avoid encouraging you to persistently save values that aren't forward-compatible.");
//                        if (ImGui::Button("Uint8 + HSV"))
//                                ImGui::SetColorEditOptions(ImGuiColorEditFlags_Uint8 | ImGuiColorEditFlags_HSV);
//                        ImGui::SameLine();
//                        if (ImGui::Button("Float + HDR"))
//                                ImGui::SetColorEditOptions(ImGuiColorEditFlags_Float | ImGuiColorEditFlags_RGB);
//
//                        ImGui::TreePop();
//                    }
//
//            if (ImGui::TreeNode("Range Widgets"))
//            {
//                static float begin = 10, end = 90;
//                static int begin_i = 100, end_i = 1000;
//                ImGui::DragFloatRange2("range", &begin, &end, 0.25f, 0.0f, 100.0f, "Min: %.1f %%", "Max: %.1f %%");
//                ImGui::DragIntRange2("range int (no bounds)", &begin_i, &end_i, 5, 0, 0, "Min: %.0f units", "Max: %.0f units");
//
//                ImGui::TreePop();
//            }
//
//            if (ImGui::TreeNode("Multi-component Widgets"))
//            {
//                static float vec4f[4] = { 0.10f, 0.20f, 0.30f, 0.44f };
//                static int vec4i[4] = { 1, 5, 100, 255 };
//
//                ImGui::InputFloat2("input float2", vec4f);
//                ImGui::DragFloat2("drag float2", vec4f, 0.01f, 0.0f, 1.0f);
//                ImGui::SliderFloat2("slider float2", vec4f, 0.0f, 1.0f);
//                ImGui::DragInt2("drag int2", vec4i, 1, 0, 255);
//                ImGui::InputInt2("input int2", vec4i);
//                ImGui::SliderInt2("slider int2", vec4i, 0, 255);
//                ImGui::Spacing();
//
//                ImGui::InputFloat3("input float3", vec4f);
//                ImGui::DragFloat3("drag float3", vec4f, 0.01f, 0.0f, 1.0f);
//                ImGui::SliderFloat3("slider float3", vec4f, 0.0f, 1.0f);
//                ImGui::DragInt3("drag int3", vec4i, 1, 0, 255);
//                ImGui::InputInt3("input int3", vec4i);
//                ImGui::SliderInt3("slider int3", vec4i, 0, 255);
//                ImGui::Spacing();
//
//                ImGui::InputFloat4("input float4", vec4f);
//                ImGui::DragFloat4("drag float4", vec4f, 0.01f, 0.0f, 1.0f);
//                ImGui::SliderFloat4("slider float4", vec4f, 0.0f, 1.0f);
//                ImGui::InputInt4("input int4", vec4i);
//                ImGui::DragInt4("drag int4", vec4i, 1, 0, 255);
//                ImGui::SliderInt4("slider int4", vec4i, 0, 255);
//
//                ImGui::TreePop();
//            }
//
//            if (ImGui::TreeNode("Vertical Sliders"))
//            {
//                const float spacing = 4;
//                ImGui::PushStyleVar(ImGuiStyleVar_ItemSpacing, ImVec2(spacing, spacing));
//
//                static int int_value = 0;
//                ImGui::VSliderInt("##int", ImVec2(18,160), &int_value, 0, 5);
//                ImGui::SameLine();
//
//                static float values[7] = { 0.0f, 0.60f, 0.35f, 0.9f, 0.70f, 0.20f, 0.0f };
//                ImGui::PushID("set1");
//                for (int i = 0; i < 7; i++)
//                {
//                    if (i > 0) ImGui::SameLine();
//                    ImGui::PushID(i);
//                    ImGui::PushStyleColor(ImGuiCol_FrameBg, (ImVec4)ImColor::HSV(i/7.0f, 0.5f, 0.5f));
//                    ImGui::PushStyleColor(ImGuiCol_FrameBgHovered, (ImVec4)ImColor::HSV(i/7.0f, 0.6f, 0.5f));
//                    ImGui::PushStyleColor(ImGuiCol_FrameBgActive, (ImVec4)ImColor::HSV(i/7.0f, 0.7f, 0.5f));
//                    ImGui::PushStyleColor(ImGuiCol_SliderGrab, (ImVec4)ImColor::HSV(i/7.0f, 0.9f, 0.9f));
//                    ImGui::VSliderFloat("##v", ImVec2(18,160), &values[i], 0.0f, 1.0f, "");
//                    if (ImGui::IsItemActive() || ImGui::IsItemHovered())
//                        ImGui::SetTooltip("%.3f", values[i]);
//                    ImGui::PopStyleColor(4);
//                    ImGui::PopID();
//                }
//                ImGui::PopID();
//
//                ImGui::SameLine();
//                ImGui::PushID("set2");
//                static float values2[4] = { 0.20f, 0.80f, 0.40f, 0.25f };
//                const int rows = 3;
//                const ImVec2 small_slider_size(18, (160.0f-(rows-1)*spacing)/rows);
//                for (int nx = 0; nx < 4; nx++)
//                {
//                    if (nx > 0) ImGui::SameLine();
//                    ImGui::BeginGroup();
//                    for (int ny = 0; ny < rows; ny++)
//                    {
//                        ImGui::PushID(nx*rows+ny);
//                        ImGui::VSliderFloat("##v", small_slider_size, &values2[nx], 0.0f, 1.0f, "");
//                        if (ImGui::IsItemActive() || ImGui::IsItemHovered())
//                            ImGui::SetTooltip("%.3f", values2[nx]);
//                        ImGui::PopID();
//                    }
//                    ImGui::EndGroup();
//                }
//                ImGui::PopID();
//
//                ImGui::SameLine();
//                ImGui::PushID("set3");
//                for (int i = 0; i < 4; i++)
//                {
//                    if (i > 0) ImGui::SameLine();
//                    ImGui::PushID(i);
//                    ImGui::PushStyleVar(ImGuiStyleVar_GrabMinSize, 40);
//                    ImGui::VSliderFloat("##v", ImVec2(40,160), &values[i], 0.0f, 1.0f, "%.2f\nsec");
//                    ImGui::PopStyleVar();
//                    ImGui::PopID();
//                }
//                ImGui::PopID();
//                ImGui::PopStyleVar();
//
//                ImGui::TreePop();
//            }
        }

        collapsingHeader("Layout") {

            //            if (ImGui::TreeNode("Child regions"))
//            {
//                ImGui::Text("Without border");
//                static int line = 50;
//                bool goto_line = ImGui::Button("Goto");
//                ImGui::SameLine();
//                ImGui::PushItemWidth(100);
//                goto_line |= ImGui::InputInt("##Line", &line, 0, 0, ImGuiInputTextFlags_EnterReturnsTrue);
//                ImGui::PopItemWidth();
//                ImGui::BeginChild("Sub1", ImVec2(ImGui::GetWindowContentRegionWidth() * 0.5f,300), false, ImGuiWindowFlags_HorizontalScrollbar);
//                for (int i = 0; i < 100; i++)
//                {
//                    ImGui::Text("%04d: scrollable region", i);
//                    if (goto_line && line == i)
//                        ImGui::SetScrollHere();
//                }
//                if (goto_line && line >= 100)
//                    ImGui::SetScrollHere();
//                ImGui::EndChild();
//
//                ImGui::SameLine();
//
//                ImGui::PushStyleVar(ImGuiStyleVar_ChildWindowRounding, 5.0f);
//                ImGui::BeginChild("Sub2", ImVec2(0,300), true);
//                ImGui::Text("With border");
//                ImGui::Columns(2);
//                for (int i = 0; i < 100; i++)
//                {
//                    if (i == 50)
//                        ImGui::NextColumn();
//                    char buf[32];
//                    sprintf(buf, "%08x", i*5731);
//                    ImGui::Button(buf, ImVec2(-1.0f, 0.0f));
//                }
//                ImGui::EndChild();
//                ImGui::PopStyleVar();
//
//                ImGui::TreePop();
//            }
//
//            if (ImGui::TreeNode("Widgets Width"))
//            {
//                static float f = 0.0f;
//                ImGui::Text("PushItemWidth(100)");
//                ImGui::SameLine(); ShowHelpMarker("Fixed width.");
//                ImGui::PushItemWidth(100);
//                ImGui::DragFloat("float##1", &f);
//                ImGui::PopItemWidth();
//
//                ImGui::Text("PushItemWidth(GetWindowWidth() * 0.5f)");
//                ImGui::SameLine(); ShowHelpMarker("Half of window width.");
//                ImGui::PushItemWidth(ImGui::GetWindowWidth() * 0.5f);
//                ImGui::DragFloat("float##2", &f);
//                ImGui::PopItemWidth();
//
//                ImGui::Text("PushItemWidth(GetContentRegionAvailWidth() * 0.5f)");
//                ImGui::SameLine(); ShowHelpMarker("Half of available width.\n(~ right-cursor_pos)\n(works within a column set)");
//                ImGui::PushItemWidth(ImGui::GetContentRegionAvailWidth() * 0.5f);
//                ImGui::DragFloat("float##3", &f);
//                ImGui::PopItemWidth();
//
//                ImGui::Text("PushItemWidth(-100)");
//                ImGui::SameLine(); ShowHelpMarker("Align to right edge minus 100");
//                ImGui::PushItemWidth(-100);
//                ImGui::DragFloat("float##4", &f);
//                ImGui::PopItemWidth();
//
//                ImGui::Text("PushItemWidth(-1)");
//                ImGui::SameLine(); ShowHelpMarker("Align to right edge");
//                ImGui::PushItemWidth(-1);
//                ImGui::DragFloat("float##5", &f);
//                ImGui::PopItemWidth();
//
//                ImGui::TreePop();
//            }
//
//            if (ImGui::TreeNode("Basic Horizontal Layout"))
//            {
//                ImGui::TextWrapped("(Use ImGui::SameLine() to keep adding items to the right of the preceding item)");
//
//                // Text
//                ImGui::Text("Two items: Hello"); ImGui::SameLine();
//                ImGui::TextColored(ImVec4(1,1,0,1), "Sailor");
//
//                // Adjust spacing
//                ImGui::Text("More spacing: Hello"); ImGui::SameLine(0, 20);
//                ImGui::TextColored(ImVec4(1,1,0,1), "Sailor");
//
//                // Button
//                ImGui::AlignFirstTextHeightToWidgets();
//                ImGui::Text("Normal buttons"); ImGui::SameLine();
//                ImGui::Button("Banana"); ImGui::SameLine();
//                ImGui::Button("Apple"); ImGui::SameLine();
//                ImGui::Button("Corniflower");
//
//                // Button
//                ImGui::Text("Small buttons"); ImGui::SameLine();
//                ImGui::SmallButton("Like this one"); ImGui::SameLine();
//                ImGui::Text("can fit within a text block.");
//
//                // Aligned to arbitrary position. Easy/cheap column.
//                ImGui::Text("Aligned");
//                ImGui::SameLine(150); ImGui::Text("x=150");
//                ImGui::SameLine(300); ImGui::Text("x=300");
//                ImGui::Text("Aligned");
//                ImGui::SameLine(150); ImGui::SmallButton("x=150");
//                ImGui::SameLine(300); ImGui::SmallButton("x=300");
//
//                // Checkbox
//                static bool c1=false,c2=false,c3=false,c4=false;
//                ImGui::Checkbox("My", &c1); ImGui::SameLine();
//                ImGui::Checkbox("Tailor", &c2); ImGui::SameLine();
//                ImGui::Checkbox("Is", &c3); ImGui::SameLine();
//                ImGui::Checkbox("Rich", &c4);
//
//                // Various
//                static float f0=1.0f, f1=2.0f, f2=3.0f;
//                ImGui::PushItemWidth(80);
//                const char* items[] = { "AAAA", "BBBB", "CCCC", "DDDD" };
//                static int item = -1;
//                ImGui::Combo("Combo", &item, items, IM_ARRAYSIZE(items)); ImGui::SameLine();
//                ImGui::SliderFloat("X", &f0, 0.0f,5.0f); ImGui::SameLine();
//                ImGui::SliderFloat("Y", &f1, 0.0f,5.0f); ImGui::SameLine();
//                ImGui::SliderFloat("Z", &f2, 0.0f,5.0f);
//                ImGui::PopItemWidth();
//
//                ImGui::PushItemWidth(80);
//                ImGui::Text("Lists:");
//                static int selection[4] = { 0, 1, 2, 3 };
//                for (int i = 0; i < 4; i++)
//                {
//                    if (i > 0) ImGui::SameLine();
//                    ImGui::PushID(i);
//                    ImGui::ListBox("", &selection[i], items, IM_ARRAYSIZE(items));
//                    ImGui::PopID();
//                    //if (ImGui::IsItemHovered()) ImGui::SetTooltip("ListBox %d hovered", i);
//                }
//                ImGui::PopItemWidth();
//
//                // Dummy
//                ImVec2 sz(30,30);
//                ImGui::Button("A", sz); ImGui::SameLine();
//                ImGui::Dummy(sz); ImGui::SameLine();
//                ImGui::Button("B", sz);
//
//                ImGui::TreePop();
//            }
//
//            if (ImGui::TreeNode("Groups"))
//            {
//                ImGui::TextWrapped("(Using ImGui::BeginGroup()/EndGroup() to layout items. BeginGroup() basically locks the horizontal position. EndGroup() bundles the whole group so that you can use functions such as IsItemHovered() on it.)");
//                ImGui::BeginGroup();
//                {
//                    ImGui::BeginGroup();
//                    ImGui::Button("AAA");
//                    ImGui::SameLine();
//                    ImGui::Button("BBB");
//                    ImGui::SameLine();
//                    ImGui::BeginGroup();
//                    ImGui::Button("CCC");
//                    ImGui::Button("DDD");
//                    ImGui::EndGroup();
//                    ImGui::SameLine();
//                    ImGui::Button("EEE");
//                    ImGui::EndGroup();
//                    if (ImGui::IsItemHovered())
//                        ImGui::SetTooltip("First group hovered");
//                }
//                // Capture the group size and create widgets using the same size
//                ImVec2 size = ImGui::GetItemRectSize();
//                const float values[5] = { 0.5f, 0.20f, 0.80f, 0.60f, 0.25f };
//                ImGui::PlotHistogram("##values", values, IM_ARRAYSIZE(values), 0, NULL, 0.0f, 1.0f, size);
//
//                ImGui::Button("ACTION", ImVec2((size.x - ImGui::GetStyle().ItemSpacing.x)*0.5f,size.y));
//                ImGui::SameLine();
//                ImGui::Button("REACTION", ImVec2((size.x - ImGui::GetStyle().ItemSpacing.x)*0.5f,size.y));
//                ImGui::EndGroup();
//                ImGui::SameLine();
//
//                ImGui::Button("LEVERAGE\nBUZZWORD", size);
//                ImGui::SameLine();
//
//                ImGui::ListBoxHeader("List", size);
//                ImGui::Selectable("Selected", true);
//                ImGui::Selectable("Not Selected", false);
//                ImGui::ListBoxFooter();
//
//                ImGui::TreePop();
//            }
//
//            if (ImGui::TreeNode("Text Baseline Alignment"))
//            {
//                ImGui::TextWrapped("(This is testing the vertical alignment that occurs on text to keep it at the same baseline as widgets. Lines only composed of text or \"small\" widgets fit in less vertical spaces than lines with normal widgets)");
//
//                ImGui::Text("One\nTwo\nThree"); ImGui::SameLine();
//                ImGui::Text("Hello\nWorld"); ImGui::SameLine();
//                ImGui::Text("Banana");
//
//                ImGui::Text("Banana"); ImGui::SameLine();
//                ImGui::Text("Hello\nWorld"); ImGui::SameLine();
//                ImGui::Text("One\nTwo\nThree");
//
//                ImGui::Button("HOP##1"); ImGui::SameLine();
//                ImGui::Text("Banana"); ImGui::SameLine();
//                ImGui::Text("Hello\nWorld"); ImGui::SameLine();
//                ImGui::Text("Banana");
//
//                ImGui::Button("HOP##2"); ImGui::SameLine();
//                ImGui::Text("Hello\nWorld"); ImGui::SameLine();
//                ImGui::Text("Banana");
//
//                ImGui::Button("TEST##1"); ImGui::SameLine();
//                ImGui::Text("TEST"); ImGui::SameLine();
//                ImGui::SmallButton("TEST##2");
//
//                ImGui::AlignFirstTextHeightToWidgets(); // If your line starts with text, call this to align it to upcoming widgets.
//                ImGui::Text("Text aligned to Widget"); ImGui::SameLine();
//                ImGui::Button("Widget##1"); ImGui::SameLine();
//                ImGui::Text("Widget"); ImGui::SameLine();
//                ImGui::SmallButton("Widget##2"); ImGui::SameLine();
//                ImGui::Button("Widget##3");
//
//                // Tree
//                const float spacing = ImGui::GetStyle().ItemInnerSpacing.x;
//                ImGui::Button("Button##1");
//                ImGui::SameLine(0.0f, spacing);
//                if (ImGui::TreeNode("Node##1")) { for (int i = 0; i < 6; i++) ImGui::BulletText("Item %d..", i); ImGui::TreePop(); }    // Dummy tree data
//
//                ImGui::AlignFirstTextHeightToWidgets();         // Vertically align text node a bit lower so it'll be vertically centered with upcoming widget. Otherwise you can use SmallButton (smaller fit).
//                bool node_open = ImGui::TreeNode("Node##2");  // Common mistake to avoid: if we want to SameLine after TreeNode we need to do it before we add child content.
//                ImGui::SameLine(0.0f, spacing); ImGui::Button("Button##2");
//                if (node_open) { for (int i = 0; i < 6; i++) ImGui::BulletText("Item %d..", i); ImGui::TreePop(); }   // Dummy tree data
//
//                // Bullet
//                ImGui::Button("Button##3");
//                ImGui::SameLine(0.0f, spacing);
//                ImGui::BulletText("Bullet text");
//
//                ImGui::AlignFirstTextHeightToWidgets();
//                ImGui::BulletText("Node");
//                ImGui::SameLine(0.0f, spacing); ImGui::Button("Button##4");
//
//                ImGui::TreePop();
//            }
//
//            if (ImGui::TreeNode("Scrolling"))
//            {
//                ImGui::TextWrapped("(Use SetScrollHere() or SetScrollFromPosY() to scroll to a given position.)");
//                static bool track = true;
//                static int track_line = 50, scroll_to_px = 200;
//                ImGui::Checkbox("Track", &track);
//                ImGui::PushItemWidth(100);
//                ImGui::SameLine(130); track |= ImGui::DragInt("##line", &track_line, 0.25f, 0, 99, "Line = %.0f");
//                bool scroll_to = ImGui::Button("Scroll To Pos");
//                ImGui::SameLine(130); scroll_to |= ImGui::DragInt("##pos_y", &scroll_to_px, 1.00f, 0, 9999, "Y = %.0f px");
//                ImGui::PopItemWidth();
//                if (scroll_to) track = false;
//
//                for (int i = 0; i < 5; i++)
//                {
//                    if (i > 0) ImGui::SameLine();
//                    ImGui::BeginGroup();
//                    ImGui::Text("%s", i == 0 ? "Top" : i == 1 ? "25%" : i == 2 ? "Center" : i == 3 ? "75%" : "Bottom");
//                    ImGui::BeginChild(ImGui::GetID((void*)(intptr_t)i), ImVec2(ImGui::GetWindowWidth() * 0.17f, 200.0f), true);
//                    if (scroll_to)
//                        ImGui::SetScrollFromPosY(ImGui::GetCursorStartPos().y + scroll_to_px, i * 0.25f);
//                    for (int line = 0; line < 100; line++)
//                    {
//                        if (track && line == track_line)
//                        {
//                            ImGui::TextColored(ImColor(255,255,0), "Line %d", line);
//                            ImGui::SetScrollHere(i * 0.25f); // 0.0f:top, 0.5f:center, 1.0f:bottom
//                        }
//                        else
//                        {
//                            ImGui::Text("Line %d", line);
//                        }
//                    }
//                    float scroll_y = ImGui::GetScrollY(), scroll_max_y = ImGui::GetScrollMaxY();
//                    ImGui::EndChild();
//                    ImGui::Text("%.0f/%0.f", scroll_y, scroll_max_y);
//                    ImGui::EndGroup();
//                }
//                ImGui::TreePop();
//            }
//
//            if (ImGui::TreeNode("Horizontal Scrolling"))
//            {
//                ImGui::Bullet(); ImGui::TextWrapped("Horizontal scrolling for a window has to be enabled explicitly via the ImGuiWindowFlags_HorizontalScrollbar flag.");
//                ImGui::Bullet(); ImGui::TextWrapped("You may want to explicitly specify content width by calling SetNextWindowContentWidth() before Begin().");
//                static int lines = 7;
//                ImGui::SliderInt("Lines", &lines, 1, 15);
//                ImGui::PushStyleVar(ImGuiStyleVar_FrameRounding, 3.0f);
//                ImGui::PushStyleVar(ImGuiStyleVar_FramePadding, ImVec2(2.0f, 1.0f));
//                ImGui::BeginChild("scrolling", ImVec2(0, ImGui::GetItemsLineHeightWithSpacing()*7 + 30), true, ImGuiWindowFlags_HorizontalScrollbar);
//                for (int line = 0; line < lines; line++)
//                {
//                    // Display random stuff (for the sake of this trivial demo we are using basic Button+SameLine. If you want to create your own time line for a real application you may be better off
//                    // manipulating the cursor position yourself, aka using SetCursorPos/SetCursorScreenPos to position the widgets yourself. You may also want to use the lower-level ImDrawList API)
//                    int num_buttons = 10 + ((line & 1) ? line * 9 : line * 3);
//                    for (int n = 0; n < num_buttons; n++)
//                    {
//                        if (n > 0) ImGui::SameLine();
//                        ImGui::PushID(n + line * 1000);
//                        char num_buf[16];
//                        const char* label = (!(n%15)) ? "FizzBuzz" : (!(n%3)) ? "Fizz" : (!(n%5)) ? "Buzz" : (sprintf(num_buf, "%d", n), num_buf);
//                        float hue = n*0.05f;
//                        ImGui::PushStyleColor(ImGuiCol_Button, (ImVec4)ImColor::HSV(hue, 0.6f, 0.6f));
//                        ImGui::PushStyleColor(ImGuiCol_ButtonHovered, (ImVec4)ImColor::HSV(hue, 0.7f, 0.7f));
//                        ImGui::PushStyleColor(ImGuiCol_ButtonActive, (ImVec4)ImColor::HSV(hue, 0.8f, 0.8f));
//                        ImGui::Button(label, ImVec2(40.0f + sinf((float)(line + n)) * 20.0f, 0.0f));
//                        ImGui::PopStyleColor(3);
//                        ImGui::PopID();
//                    }
//                }
//                float scroll_x = ImGui::GetScrollX(), scroll_max_x = ImGui::GetScrollMaxX();
//                ImGui::EndChild();
//                ImGui::PopStyleVar(2);
//                float scroll_x_delta = 0.0f;
//                ImGui::SmallButton("<<"); if (ImGui::IsItemActive()) scroll_x_delta = -ImGui::GetIO().DeltaTime * 1000.0f; ImGui::SameLine();
//                ImGui::Text("Scroll from code"); ImGui::SameLine();
//                ImGui::SmallButton(">>"); if (ImGui::IsItemActive()) scroll_x_delta = +ImGui::GetIO().DeltaTime * 1000.0f; ImGui::SameLine();
//                ImGui::Text("%.0f/%.0f", scroll_x, scroll_max_x);
//                if (scroll_x_delta != 0.0f)
//                {
//                    ImGui::BeginChild("scrolling"); // Demonstrate a trick: you can use Begin to set yourself in the context of another window (here we are already out of your child window)
//                    ImGui::SetScrollX(ImGui::GetScrollX() + scroll_x_delta);
//                    ImGui::End();
//                }
//                ImGui::TreePop();
//            }
//
//            if (ImGui::TreeNode("Clipping"))
//            {
//                static ImVec2 size(100, 100), offset(50, 20);
//                ImGui::TextWrapped("On a per-widget basis we are occasionally clipping text CPU-side if it won't fit in its frame. Otherwise we are doing coarser clipping + passing a scissor rectangle to the renderer. The system is designed to try minimizing both execution and CPU/GPU rendering cost.");
//                ImGui::DragFloat2("size", (float*)&size, 0.5f, 0.0f, 200.0f, "%.0f");
//                ImGui::TextWrapped("(Click and drag)");
//                ImVec2 pos = ImGui::GetCursorScreenPos();
//                ImVec4 clip_rect(pos.x, pos.y, pos.x+size.x, pos.y+size.y);
//                ImGui::InvisibleButton("##dummy", size);
//                if (ImGui::IsItemActive() && ImGui::IsMouseDragging()) { offset.x += ImGui::GetIO().MouseDelta.x; offset.y += ImGui::GetIO().MouseDelta.y; }
//                ImGui::GetWindowDrawList()->AddRectFilled(pos, ImVec2(pos.x+size.x,pos.y+size.y), ImColor(90,90,120,255));
//                ImGui::GetWindowDrawList()->AddText(ImGui::GetFont(), ImGui::GetFontSize()*2.0f, ImVec2(pos.x+offset.x,pos.y+offset.y), ImColor(255,255,255,255), "Line 1 hello\nLine 2 clip me!", NULL, 0.0f, &clip_rect);
//                ImGui::TreePop();
//            }
        }

        collapsingHeader("Popups & Modal windows") {

            treeNode("Popups") {

                //                ImGui::TextWrapped("When a popup is active, it inhibits interacting with windows that are behind the popup. Clicking outside the popup closes it.");
//
//                static int selected_fish = -1;
//                const char* names[] = { "Bream", "Haddock", "Mackerel", "Pollock", "Tilefish" };
//                static bool toggles[] = { true, false, false, false, false };
//
//                // Simple selection popup
//                // (If you want to show the current selection inside the Button itself, you may want to build a string using the "###" operator to preserve a constant ID with a variable label)
//                if (ImGui::Button("Select.."))
//                    ImGui::OpenPopup("select");
//                ImGui::SameLine();
//                ImGui::Text(selected_fish == -1 ? "<None>" : names[selected_fish]);
//                if (ImGui::BeginPopup("select"))
//                {
//                    ImGui::Text("Aquarium");
//                    ImGui::Separator();
//                    for (int i = 0; i < IM_ARRAYSIZE(names); i++)
//                    if (ImGui::Selectable(names[i]))
//                        selected_fish = i;
//                    ImGui::EndPopup();
//                }
//
//                // Showing a menu with toggles
//                if (ImGui::Button("Toggle.."))
//                    ImGui::OpenPopup("toggle");
//                if (ImGui::BeginPopup("toggle"))
//                {
//                    for (int i = 0; i < IM_ARRAYSIZE(names); i++)
//                    ImGui::MenuItem(names[i], "", &toggles[i]);
//                    if (ImGui::BeginMenu("Sub-menu"))
//                    {
//                        ImGui::MenuItem("Click me");
//                        ImGui::EndMenu();
//                    }
//
//                    ImGui::Separator();
//                    ImGui::Text("Tooltip here");
//                    if (ImGui::IsItemHovered())
//                        ImGui::SetTooltip("I am a tooltip over a popup");
//
//                    if (ImGui::Button("Stacked Popup"))
//                        ImGui::OpenPopup("another popup");
//                    if (ImGui::BeginPopup("another popup"))
//                    {
//                        for (int i = 0; i < IM_ARRAYSIZE(names); i++)
//                        ImGui::MenuItem(names[i], "", &toggles[i]);
//                        if (ImGui::BeginMenu("Sub-menu"))
//                        {
//                            ImGui::MenuItem("Click me");
//                            ImGui::EndMenu();
//                        }
//                        ImGui::EndPopup();
//                    }
//                    ImGui::EndPopup();
//                }
//
//                if (ImGui::Button("Popup Menu.."))
//                    ImGui::OpenPopup("FilePopup");
//                if (ImGui::BeginPopup("FilePopup"))
//                {
//                    ShowExampleMenuFile();
//                    ImGui::EndPopup();
//                }
            }

            treeNode("Context menus") {

                // BeginPopupContextItem() is a helper to provide common/simple popup behavior of essentially doing:
                //    if (IsItemHovered() && IsMouseClicked(0))
                //       OpenPopup(id);
                //    return BeginPopup(id);
                // For more advanced uses you may want to replicate and cuztomize this code. This the comments inside BeginPopupContextItem() implementation.
                //                static float value = 0.5f;
//                ImGui::Text("Value = %.3f (<-- right-click here)", value);
//                if (ImGui::BeginPopupContextItem("item context menu"))
//                {
//                    if (ImGui::Selectable("Set to zero")) value = 0.0f;
//                    if (ImGui::Selectable("Set to PI")) value = 3.1415f;
//                    ImGui::PushItemWidth(-1);
//                    ImGui::DragFloat("##Value", &value, 0.1f, 0.0f, 0.0f);
//                    ImGui::PopItemWidth();
//                    ImGui::EndPopup();
//                }
//
//                static char name[32] = "Label1";
//                char buf[64]; sprintf(buf, "Button: %s###Button", name); // ### operator override ID ignoring the preceding label
//                ImGui::Button(buf);
//                if (ImGui::BeginPopupContextItem()) // When used after an item that has an ID (here the Button), we can skip providing an ID to BeginPopupContextItem().
//                {
//                    ImGui::Text("Edit name");
//                    ImGui::InputText("##edit", name, IM_ARRAYSIZE(name));
//                    if (ImGui::Button("Close"))
//                        ImGui::CloseCurrentPopup();
//                    ImGui::EndPopup();
//                }
//                ImGui::SameLine(); ImGui::Text("(<-- right-click here)");
            }

            treeNode("Modals") {

                textWrapped("Modal windows are like popups but the user cannot close them by clicking outside the window.")

                if (button("Delete..")) {
                    openPopup("Delete?")
                }
                popupModal("Delete?", null, Wf.AlwaysAutoResize.i) {

                    text("All those beautiful files will be deleted.\nThis operation cannot be undone!\n\n")
                    separator()

                    //static int dummy_i = 0;
                    //ImGui::Combo("Combo", &dummy_i, "Delete\0Delete harder\0");

                    withStyleVar(StyleVar.FramePadding, Vec2()) { checkbox("Don't ask me next time", dontAskMeNextTime) }

                    button("OK", Vec2(120, 0)) { closeCurrentPopup() }
                    sameLine()
                    button("Cancel", Vec2(120, 0)) { closeCurrentPopup() }
                }

                button("Stacked modals..") { openPopup("Stacked 1") }
                popupModal("Stacked 1") {

                    text("Hello from Stacked The First\nUsing style.Colors[ImGuiCol_ModalWindowDarkening] for darkening.")
//                    static int item = 1; TODO
//                    ImGui::Combo("Combo", &item, "aaaa\0bbbb\0cccc\0dddd\0eeee\0\0");
//                    static float color[4] = { 0.4f,0.7f,0.0f,0.5f };
//                    ImGui::ColorEdit4("color", color);  // This is to test behavior of stacked regular popups over a modal

                    button("Add another modal..") { openPopup("Stacked 2") }
                    popupModal("Stacked 2") {
                        text("Hello from Stacked The Second")
                        button("Close") { closeCurrentPopup() }
                    }
                    button("Close") { closeCurrentPopup() }
                }
            }
//            if (ImGui::TreeNode("Menus inside a regular window"))
//                +        {
//                    +            ImGui::TextWrapped("Below we are testing adding menu items to a regular window. It's rather unusual but should work!");
//                    +            ImGui::Separator();
//                    +            // NB: As a quirk in this very specific example, we want to differentiate the parent of this menu from the parent of the various popup menus above.
//                    +            // To do so we are encloding the items in a PushID()/PopID() block to make them two different menusets. If we don't, opening any popup above and hovering our menu here
//                    +            // would open it. This is because once a menu is active, we allow to switch to a sibling menu by just hovering on it, which is the desired behavior for regular menus.
//                    +            ImGui::PushID("foo");
//                    +            ImGui::MenuItem("Menu item", "CTRL+M");
//                    +            if (ImGui::BeginMenu("Menu inside a regular window"))
//                        +            {
//                            +                ShowExampleMenuFile();
//                            +                ImGui::EndMenu();
//                            +            }
//                    +            ImGui::PopID();
//                    +            ImGui::Separator();
//                    +            ImGui::TreePop();
//                    +        }
        }

//        if (ImGui::CollapsingHeader("Columns"))
//        {
//            ImGui::PushID("Columns");
//
//            // Basic columns
//            if (ImGui::TreeNode("Basic"))
//            {
//                ImGui::Text("Without border:");
//                ImGui::Columns(3, "mycolumns3", false);  // 3-ways, no border
//                ImGui::Separator();
//                for (int n = 0; n < 14; n++)
//                {
//                    char label[32];
//                    sprintf(label, "Item %d", n);
//                    if (ImGui::Selectable(label)) {}
//                    //if (ImGui::Button(label, ImVec2(-1,0))) {}
//                    ImGui::NextColumn();
//                }
//                ImGui::Columns(1);
//                ImGui::Separator();
//
//                ImGui::Text("With border:");
//                ImGui::Columns(4, "mycolumns"); // 4-ways, with border
//                ImGui::Separator();
//                ImGui::Text("ID"); ImGui::NextColumn();
//                ImGui::Text("Name"); ImGui::NextColumn();
//                ImGui::Text("Path"); ImGui::NextColumn();
//                ImGui::Text("Hovered"); ImGui::NextColumn();
//                ImGui::Separator();
//                const char* names[3] = { "One", "Two", "Three" };
//                const char* paths[3] = { "/path/one", "/path/two", "/path/three" };
//                static int selected = -1;
//                for (int i = 0; i < 3; i++)
//                {
//                    char label[32];
//                    sprintf(label, "%04d", i);
//                    if (ImGui::Selectable(label, selected == i, ImGuiSelectableFlags_SpanAllColumns))
//                        selected = i;
//                    bool hovered = ImGui::IsItemHovered();
//                    ImGui::NextColumn();
//                    ImGui::Text(names[i]); ImGui::NextColumn();
//                    ImGui::Text(paths[i]); ImGui::NextColumn();
//                    ImGui::Text("%d", hovered); ImGui::NextColumn();
//                }
//                ImGui::Columns(1);
//                ImGui::Separator();
//                ImGui::TreePop();
//            }
//
//            // Create multiple items in a same cell before switching to next column
//            if (ImGui::TreeNode("Mixed items"))
//            {
//                ImGui::Columns(3, "mixed");
//                ImGui::Separator();
//
//                ImGui::Text("Hello");
//                ImGui::Button("Banana");
//                ImGui::NextColumn();
//
//                ImGui::Text("ImGui");
//                ImGui::Button("Apple");
//                static float foo = 1.0f;
//                ImGui::InputFloat("red", &foo, 0.05f, 0, 3);
//                ImGui::Text("An extra line here.");
//                ImGui::NextColumn();
//
//                ImGui::Text("Sailor");
//                ImGui::Button("Corniflower");
//                static float bar = 1.0f;
//                ImGui::InputFloat("blue", &bar, 0.05f, 0, 3);
//                ImGui::NextColumn();
//
//                if (ImGui::CollapsingHeader("Category A")) { ImGui::Text("Blah blah blah"); } ImGui::NextColumn();
//                if (ImGui::CollapsingHeader("Category B")) { ImGui::Text("Blah blah blah"); } ImGui::NextColumn();
//                if (ImGui::CollapsingHeader("Category C")) { ImGui::Text("Blah blah blah"); } ImGui::NextColumn();
//                ImGui::Columns(1);
//                ImGui::Separator();
//                ImGui::TreePop();
//            }
//
//            // Word wrapping
//            if (ImGui::TreeNode("Word-wrapping"))
//            {
//                ImGui::Columns(2, "word-wrapping");
//                ImGui::Separator();
//                ImGui::TextWrapped("The quick brown fox jumps over the lazy dog.");
//                ImGui::TextWrapped("Hello Left");
//                ImGui::NextColumn();
//                ImGui::TextWrapped("The quick brown fox jumps over the lazy dog.");
//                ImGui::TextWrapped("Hello Right");
//                ImGui::Columns(1);
//                ImGui::Separator();
//                ImGui::TreePop();
//            }
//
//            if (ImGui::TreeNode("Borders"))
//            {
//                // NB: Future columns API should allow automatic horizontal borders.
//                static bool h_borders = true;
//                static bool v_borders = true;
//                ImGui::Checkbox("horizontal", &h_borders);
//                ImGui::SameLine();
//                ImGui::Checkbox("vertical", &v_borders);
//                ImGui::Columns(4, NULL, v_borders);
//                for (int i = 0; i < 4*3; i++)
//                {
//                    if (h_borders && ImGui::GetColumnIndex() == 0)
//                        ImGui::Separator();
//                    ImGui::Text("%c%c%c", 'a'+i, 'a'+i, 'a'+i);
//                    ImGui::Text("Width %.2f\nOffset %.2f", ImGui::GetColumnWidth(), ImGui::GetColumnOffset());
//                    ImGui::NextColumn();
//                }
//                ImGui::Columns(1);
//                if (h_borders) ImGui::Separator();
//        ImGui::TreePop();
//        +        }
//    +
//    +        // Scrolling columns
//    +        /*
//+        if (ImGui::TreeNode("Vertical Scrolling"))
//+        {
//+            ImGui::BeginChild("##header", ImVec2(0, ImGui::GetTextLineHeightWithSpacing()+ImGui::GetStyle().ItemSpacing.y));
//+            ImGui::Columns(3);
//+            ImGui::Text("ID"); ImGui::NextColumn();
//+            ImGui::Text("Name"); ImGui::NextColumn();
//+            ImGui::Text("Path"); ImGui::NextColumn();
//+            ImGui::Columns(1);
//+            ImGui::Separator();
//+            ImGui::EndChild();
//+            ImGui::BeginChild("##scrollingregion", ImVec2(0, 60));
//+            ImGui::Columns(3);
//+            for (int i = 0; i < 10; i++)
//+            {
//+                ImGui::Text("%04d", i); ImGui::NextColumn();
//+                ImGui::Text("Foobar"); ImGui::NextColumn();
//+                ImGui::Text("/path/foobar/%04d/", i); ImGui::NextColumn();
//+            }
//+            ImGui::Columns(1);
//+            ImGui::EndChild();
//+            ImGui::TreePop();
//+        }
//+        */
//    +
//    +        if (ImGui::TreeNode("Horizontal Scrolling"))
//    +        {
//        +            ImGui::SetNextWindowContentWidth(1500);
//        +            ImGui::BeginChild("##scrollingregion", ImVec2(0, 120), false, ImGuiWindowFlags_HorizontalScrollbar);
//        +            ImGui::Columns(10);
//        +            for (int i = 0; i < 20; i++)
//        +                for (int j = 0; j < 10; j++)
//        +                {
//            +                    ImGui::Text("Line %d Column %d...", i, j);
//            +                    ImGui::NextColumn();
//            +                }
//        +            ImGui::Columns(1);
//        +            ImGui::EndChild();
//                ImGui::TreePop();
//            }
//
//        bool node_open = ImGui::TreeNode("Tree within single cell");
//        ImGui::SameLine(); ShowHelpMarker("NB: Tree node must be poped before ending the cell. There's no storage of state per-cell.");
//
//        if (node_open)
//        {
//            ImGui::Columns(2, "tree items");
//            ImGui::Separator();
//            if (ImGui::TreeNode("Hello")) { ImGui::BulletText("Sailor"); ImGui::TreePop(); } ImGui::NextColumn();
//            if (ImGui::TreeNode("Bonjour")) { ImGui::BulletText("Marin"); ImGui::TreePop(); } ImGui::NextColumn();
//            ImGui::Columns(1);
//            ImGui::Separator();
//            ImGui::TreePop();
//        }
//        ImGui::PopID();
//    }
//
//    if (ImGui::CollapsingHeader("Filtering"))
//    {
//        static ImGuiTextFilter filter;
//        ImGui::Text("Filter usage:\n"
//                "  \"\"         display all lines\n"
//        "  \"xxx\"      display lines containing \"xxx\"\n"
//        "  \"xxx,yyy\"  display lines containing \"xxx\" or \"yyy\"\n"
//        "  \"-xxx\"     hide lines containing \"xxx\"");
//        filter.Draw();
//        const char* lines[] = { "aaa1.c", "bbb1.c", "ccc1.c", "aaa2.cpp", "bbb2.cpp", "ccc2.cpp", "abc.h", "hello, world" };
//        for (int i = 0; i < IM_ARRAYSIZE(lines); i++)
//        if (filter.PassFilter(lines[i]))
//            ImGui::BulletText("%s", lines[i]);
//    }
//        if (ImGui::CollapsingHeader("Inputs & Focus"))
//        {
//            +        ImGuiIO& io = ImGui::GetIO();
//            +        ImGui::Checkbox("io.MouseDrawCursor", &io.MouseDrawCursor);
//            +        ImGui::SameLine(); ShowHelpMarker("Request ImGui to render a mouse cursor for you in software. Note that a mouse cursor rendered via regular GPU rendering will feel more laggy than hardware cursor, but will be more in sync with your other visuals.");
//            +
//            +        ImGui::Text("WantCaptureMouse: %d", io.WantCaptureMouse);
//            +        ImGui::Text("WantCaptureKeyboard: %d", io.WantCaptureKeyboard);
//            +        ImGui::Text("WantTextInput: %d", io.WantTextInput);
//                    ImGui::Text("WantMoveMouse: %d", io.WantMoveMouse);
//            +
//            +        if (ImGui::TreeNode("Keyboard & Mouse State"))
//                +        {
//                    +            ImGui::Text("Mouse pos: (%g, %g)", io.MousePos.x, io.MousePos.y);
//                    +            ImGui::Text("Mouse down:");     for (int i = 0; i < IM_ARRAYSIZE(io.MouseDown); i++) if (io.MouseDownDuration[i] >= 0.0f)   { ImGui::SameLine(); ImGui::Text("b%d (%.02f secs)", i, io.MouseDownDuration[i]); }
//                    +            ImGui::Text("Mouse clicked:");  for (int i = 0; i < IM_ARRAYSIZE(io.MouseDown); i++) if (ImGui::IsMouseClicked(i))          { ImGui::SameLine(); ImGui::Text("b%d", i); }
//                    +            ImGui::Text("Mouse dbl-clicked:"); for (int i = 0; i < IM_ARRAYSIZE(io.MouseDown); i++) if (ImGui::IsMouseDoubleClicked(i)) { ImGui::SameLine(); ImGui::Text("b%d", i); }
//                    +            ImGui::Text("Mouse released:"); for (int i = 0; i < IM_ARRAYSIZE(io.MouseDown); i++) if (ImGui::IsMouseReleased(i))         { ImGui::SameLine(); ImGui::Text("b%d", i); }
//                    +            ImGui::Text("Mouse wheel: %.1f", io.MouseWheel);
//                    +
//                    +            ImGui::Text("Keys down:");      for (int i = 0; i < IM_ARRAYSIZE(io.KeysDown); i++) if (io.KeysDownDuration[i] >= 0.0f)     { ImGui::SameLine(); ImGui::Text("%d (%.02f secs)", i, io.KeysDownDuration[i]); }
//                    +            ImGui::Text("Keys pressed:");   for (int i = 0; i < IM_ARRAYSIZE(io.KeysDown); i++) if (ImGui::IsKeyPressed(i))             { ImGui::SameLine(); ImGui::Text("%d", i); }
//                    +            ImGui::Text("Keys release:");   for (int i = 0; i < IM_ARRAYSIZE(io.KeysDown); i++) if (ImGui::IsKeyReleased(i))            { ImGui::SameLine(); ImGui::Text("%d", i); }
//                    +            ImGui::Text("Keys mods: %s%s%s%s", io.KeyCtrl ? "CTRL " : "", io.KeyShift ? "SHIFT " : "", io.KeyAlt ? "ALT " : "", io.KeySuper ? "SUPER " : "");
//                    +
//                    +
//                    +            ImGui::Button("Hovering me sets the\nkeyboard capture flag");
//                    +            if (ImGui::IsItemHovered())
//                        +                ImGui::CaptureKeyboardFromApp(true);
//                    +            ImGui::SameLine();
//                    +            ImGui::Button("Holding me clears the\nthe keyboard capture flag");
//                    +            if (ImGui::IsItemActive())
//                        +                ImGui::CaptureKeyboardFromApp(false);
//                    +
//                    +            ImGui::TreePop();
//                    +        }
//            +
//            if (ImGui::TreeNode("Tabbing"))
//            {
//                ImGui::Text("Use TAB/SHIFT+TAB to cycle through keyboard editable fields.");
//                static char buf[32] = "dummy";
//                ImGui::InputText("1", buf, IM_ARRAYSIZE(buf));
//                ImGui::InputText("2", buf, IM_ARRAYSIZE(buf));
//                ImGui::InputText("3", buf, IM_ARRAYSIZE(buf));
//                ImGui::PushAllowKeyboardFocus(false);
//                ImGui::InputText("4 (tab skip)", buf, IM_ARRAYSIZE(buf));
//                //ImGui::SameLine(); ShowHelperMarker("Use ImGui::PushAllowKeyboardFocus(bool)\nto disable tabbing through certain widgets.");
//                ImGui::PopAllowKeyboardFocus();
//                ImGui::InputText("5", buf, IM_ARRAYSIZE(buf));
//                ImGui::TreePop();
//            }
//
//            if (ImGui::TreeNode("Focus from code"))
//            {
//                bool focus_1 = ImGui::Button("Focus on 1"); ImGui::SameLine();
//                bool focus_2 = ImGui::Button("Focus on 2"); ImGui::SameLine();
//                bool focus_3 = ImGui::Button("Focus on 3");
//                int has_focus = 0;
//                static char buf[128] = "click on a button to set focus";
//
//                if (focus_1) ImGui::SetKeyboardFocusHere();
//                ImGui::InputText("1", buf, IM_ARRAYSIZE(buf));
//                if (ImGui::IsItemActive()) has_focus = 1;
//
//                if (focus_2) ImGui::SetKeyboardFocusHere();
//                ImGui::InputText("2", buf, IM_ARRAYSIZE(buf));
//                if (ImGui::IsItemActive()) has_focus = 2;
//
//                ImGui::PushAllowKeyboardFocus(false);
//                if (focus_3) ImGui::SetKeyboardFocusHere();
//                ImGui::InputText("3 (tab skip)", buf, IM_ARRAYSIZE(buf));
//                if (ImGui::IsItemActive()) has_focus = 3;
//                ImGui::PopAllowKeyboardFocus();
//                if (has_focus)
//                    ImGui::Text("Item with focus: %d", has_focus);
//                else
//                    ImGui::Text("Item with focus: <none>");
//                ImGui::TextWrapped("Cursor & selection are preserved when refocusing last used item in code.");
//                ImGui::TreePop();
//            }
//
//        if (ImGui::TreeNode("Hovering"))
//            +        {
//                +            // Testing IsWindowHovered() function
//                +            ImGui::BulletText(
//                        +                "IsWindowHovered() = %d\n"
//                                +                "IsWindowHovered(_AllowWhenBlockedByPopup) = %d\n"
//                                +                "IsWindowHovered(_AllowWhenBlockedByActiveItem) = %d\n",
//                        +                ImGui::IsWindowHovered(),
//                        +                ImGui::IsWindowHovered(ImGuiHoveredFlags_AllowWhenBlockedByPopup),
//                        +                ImGui::IsWindowHovered(ImGuiHoveredFlags_AllowWhenBlockedByActiveItem));
//                +
//                +            // Testing IsItemHovered() function (because BulletText is an item itself and that would affect the output of IsItemHovered, we pass all lines in a single items to shorten the code)
//                +            ImGui::Button("ITEM");
//                +            ImGui::BulletText(
//                        +                "IsItemHovered() = %d\n"
//                                +                "IsItemHovered(_AllowWhenBlockedByPopup) = %d\n"
//                                +                "IsItemHovered(_AllowWhenBlockedByActiveItem) = %d\n"
//                                +                "IsItemHovered(_AllowWhenOverlapped) = %d\n"
//                                +                "IsItemhovered(_RectOnly) = %d\n",
//                        +                ImGui::IsItemHovered(),
//                        +                ImGui::IsItemHovered(ImGuiHoveredFlags_AllowWhenBlockedByPopup),
//                        +                ImGui::IsItemHovered(ImGuiHoveredFlags_AllowWhenBlockedByActiveItem),
//                        +                ImGui::IsItemHovered(ImGuiHoveredFlags_AllowWhenOverlapped),
//                        +                ImGui::IsItemHovered(ImGuiHoveredFlags_RectOnly));
//                +
//                +            ImGui::TreePop();
//                +        }
//
//            if (ImGui::TreeNode("Dragging"))
//            {
//                ImGui::TextWrapped("You can use ImGui::GetMouseDragDelta(0) to query for the dragged amount on any widget.");
//                ImGui::Button("Drag Me");
//                if (ImGui::IsItemActive())
//                {
//                    // Draw a line between the button and the mouse cursor
//                    ImDrawList* draw_list = ImGui::GetWindowDrawList();
//                    draw_list->PushClipRectFullScreen();
//        draw_list->AddLine(ImGui::CalcItemRectClosestPoint(io.MousePos, true, -2.0f), io.MousePos, ImColor(ImGui::GetStyle().Colors[ImGuiCol_Button]), 4.0f);
//        draw_list->PopClipRect();
//        ImVec2 value_raw = ImGui::GetMouseDragDelta(0, 0.0f);
//        ImVec2 value_with_lock_threshold = ImGui::GetMouseDragDelta(0);
//        ImVec2 mouse_delta = io.MouseDelta;
//        ImGui::SameLine(); ImGui::Text("Raw (%.1f, %.1f), WithLockThresold (%.1f, %.1f), MouseDelta (%.1f, %.1f)", value_raw.x, value_raw.y, value_with_lock_threshold.x, value_with_lock_threshold.y, mouse_delta.x, mouse_delta.y);
//    }
//    ImGui::TreePop();
//}
//        if (ImGui::TreeNode("Mouse cursors"))
//        {
//        ImGui::Text("Hover to see mouse cursors:");
//        +            ImGui::SameLine(); ShowHelpMarker("Your application can render a different mouse cursor based on what ImGui::GetMouseCursor() returns. If software cursor rendering (io.MouseDrawCursor) is set ImGui will draw the right cursor for you, otherwise your backend needs to handle it.");
//                     for (int i = 0; i < ImGuiMouseCursor_Count_; i++)
//        {
//            char label[32];
//            sprintf(label, "Mouse cursor %d", i);
//            ImGui::Bullet(); ImGui::Selectable(label, false);
//            if (ImGui::IsItemHovered())
//                ImGui::SetMouseCursor(i);
//        }
//        ImGui::TreePop();
//    }
//}
        end()
    }

    /** create metrics window. display ImGui internals: browse window list, draw commands, individual vertices, basic
     *  internal state, etc.    */
    fun showMetricsWindow(pOpen: BooleanArray) {

        if (begin("ImGui Metrics", pOpen)) {
            text("ImGui $version")
            text("Application average %.3f ms/frame (%.1f FPS)", 1000f / IO.framerate, IO.framerate)
            text("%d vertices, %d indices (%d triangles)", IO.metricsRenderVertices, IO.metricsRenderIndices, IO.metricsRenderIndices / 3)
            text("%d allocations", IO.metricsAllocs)
            checkbox("Show clipping rectangles when hovering an ImDrawCmd", showClipRects)
            separator()

            Funcs.nodeWindows(g.windows, "Windows")
            if (treeNode("DrawList", "Active DrawLists (${g.renderDrawLists[0].size})")) {
                g.renderDrawLists.forEach { layer -> layer.forEach { Funcs.nodeDrawList(it, "DrawList") } }
                for (i in g.renderDrawLists[0])
                    Funcs.nodeDrawList(i, "DrawList")
                treePop()
            }
            if (treeNode("Popups", "Open Popups Stack (${g.openPopupStack.size})")) {
                for (popup in g.openPopupStack) {
                    val window = popup.window
                    val childWindow = if (window != null && window.flags has Wf.ChildWindow) " ChildWindow" else ""
                    val childMenu = if (window != null && window.flags has Wf.ChildMenu) " ChildMenu" else ""
                    bulletText("PopupID: %08x, Window: '${window?.name}'$childWindow$childMenu", popup.popupId)
                }
                treePop()
            }
            if (treeNode("Basic state")) {
                text("HoveredWindow: '${g.hoveredWindow?.name}'")
                text("HoveredRootWindow: '${g.hoveredWindow?.name}'")
                /*  Data is "in-flight" so depending on when the Metrics window is called we may see current frame
                    information or not                 */
                text("HoveredId: 0x%08X/0x%08X", g.hoveredId, g.hoveredIdPreviousFrame)
                text("ActiveId: 0x%08X/0x%08X", g.activeId, g.activeIdPreviousFrame)
                text("ActiveIdWindow: '${g.activeIdWindow?.name}'")
                text("NavWindow: '${g.navWindow?.name}'")
                treePop()
            }
        }
        end()
    }

    object Funcs {

        fun nodeDrawList(drawList: DrawList, label: String) {

            val nodeOpen = treeNode(drawList, "$label: '${drawList._ownerName}' ${drawList.vtxBuffer.size} vtx, " +
                    "${drawList.idxBuffer.size} indices, ${drawList.cmdBuffer.size} cmds")
            if (drawList === windowDrawList) {
                sameLine()
                // Can't display stats for active draw list! (we don't have the data double-buffered)
                textColored(Vec4.fromColor(255, 100, 100), "CURRENTLY APPENDING")
                if (nodeOpen) treePop()
                return
            }
            if (!nodeOpen)
                return

            val overlayDrawList = g.overlayDrawList   // Render additional visuals into the top-most draw list
            overlayDrawList.pushClipRectFullScreen()
            var elemOffset = 0
            for (i in drawList.cmdBuffer.indices) {
                val cmd = drawList.cmdBuffer[i]
                if (cmd.userCallback == null && cmd.elemCount == 0) continue
                if (cmd.userCallback != null) {
                    TODO()
//                        ImGui::BulletText("Callback %p, user_data %p", pcmd->UserCallback, pcmd->UserCallbackData)
//                        continue
                }
                val idxBuffer = drawList.idxBuffer.takeIf { it.isNotEmpty() }
                val mode = if (drawList.idxBuffer.isNotEmpty()) "indexed" else "non-indexed"
                val cmdNodeOpen = treeNode(i, "Draw %-4d $mode vtx, tex = ${cmd.textureId}, clip_rect = (%.0f,%.0f)..(%.0f,%.0f)",
                        cmd.elemCount, cmd.clipRect.x, cmd.clipRect.y, cmd.clipRect.z, cmd.clipRect.w)
                if (showClipRects[0] && isItemHovered()) {
                    val clipRect = Rect(cmd.clipRect)
                    val vtxsRect = Rect()
                    for (e in elemOffset until elemOffset + cmd.elemCount)
                        vtxsRect.add(drawList.vtxBuffer[idxBuffer?.get(e) ?: e].pos)
                    clipRect.floor(); overlayDrawList.addRect(clipRect.min, clipRect.max, COL32(255, 255, 0, 255))
                    vtxsRect.floor(); overlayDrawList.addRect(vtxsRect.min, vtxsRect.max, COL32(255, 0, 255, 255))
                }
                if (!cmdNodeOpen) continue
                // Manually coarse clip our print out of individual vertices to save CPU, only items that may be visible.
                val clipper = ListClipper(cmd.elemCount / 3)
                while (clipper.step()) {
                    var vtxI = elemOffset + clipper.display.start * 3
                    for (prim in clipper.display.start until clipper.display.last) {
                        val buf = CharArray(300)
                        var bufP = 0
                        val trianglesPos = arrayListOf(Vec2(), Vec2(), Vec2())
                        for (n in 0 until 3) {
                            val v = drawList.vtxBuffer[idxBuffer?.get(vtxI) ?: vtxI]
                            trianglesPos[n] = v.pos
                            val name = if (n == 0) "vtx" else "   "
                            val string = "$name %04d { pos = (%8.2f,%8.2f), uv = (%.6f,%.6f), col = %08X }\n".format(style.locale,
                                    vtxI, v.pos.x, v.pos.y, v.uv.x, v.uv.y, v.col)
                            string.toCharArray(buf, bufP)
                            bufP += string.length
                            vtxI++
                        }
                        selectable(buf.joinToString("", limit = bufP, truncated = ""), false)
                        if (isItemHovered())
                        // Add triangle without AA, more readable for large-thin triangle
                            overlayDrawList.addPolyline(trianglesPos, COL32(255, 255, 0, 255), true, 1f, false)
                    }
                }
                treePop()
                elemOffset += cmd.elemCount
            }
            overlayDrawList.popClipRect()
            treePop()
        }

        fun nodeWindows(windows: ArrayList<Window>, label: String) {
            if (!treeNode(label, "$label (${windows.size})")) return
            for (i in 0 until windows.size)
                nodeWindow(windows[i], "Window")
            treePop()
        }

        fun nodeWindow(window: Window, label: String) {
            val active = if (window.active or window.wasActive) "active" else "inactive"
            if (!treeNode(window, "$label '${window.name}', $active @ 0x%X", System.identityHashCode(window)))
                return
            nodeDrawList(window.drawList, "DrawList")
            bulletText("Pos: (%.1f,%.1f), Size: (%.1f,%.1f), SizeContents (%.1f,%.1f)", window.pos.x, window.pos.y, window.size.x,
                    window.size.y, window.sizeContents.x, window.sizeContents.y)
            if (isItemHovered())
                overlayDrawList.addRect(Vec2(window.pos), Vec2(window.pos + window.size), COL32(255, 255, 0, 255))
            bulletText("Scroll: (%.2f,%.2f)", window.scroll.x, window.scroll.y)
            bulletText("Active: ${window.active}, Accessed: ${window.accessed}")
            if (window.rootWindow !== window) nodeWindow(window.rootWindow, "RootWindow")
            if (window.dc.childWindows.isNotEmpty()) nodeWindows(window.dc.childWindows, "ChildWindows")
            bulletText("Storage: %d bytes", window.stateStorage.data.size * Int.BYTES * 2)
            treePop()
        }

        fun showDummyObject(prefix: String, uid: Int) {
//            println("showDummyObject $prefix _$uid")
            //  Use object uid as identifier. Most commonly you could also use the object pointer as a base ID.
            pushId(uid)
            /*  Text and Tree nodes are less high than regular widgets, here we add vertical spacing to make the tree
                lines equal high.             */
            alignTextToFramePadding()
            val nodeOpen = treeNode("Object", "${prefix}_$uid")
            nextColumn()
            alignTextToFramePadding()
            text("my sailor is rich")
            nextColumn()
            if (nodeOpen) {
                for (i in 0..7) {
                    pushId(i) // Use field index as identifier.
                    if (i < 2)
                        showDummyObject("Child", 424242)
                    else {
                        alignTextToFramePadding()
                        // Here we use a Selectable (instead of Text) to highlight on hover
                        //Text("Field_%d", i);
                        bullet()
                        selectable("Field_$i")
                        nextColumn()
                        pushItemWidth(-1f)
                        if (i >= 5)
                            inputFloat("##value", dummyMembers, i, 1f)
                        else
                            dragFloat("##value", dummyMembers, i, 0.01f)
                        popItemWidth()
                        nextColumn()
                    }
                    popId()
                }
                treePop()
            }
            popId()
        }

        val dummyMembers = floatArrayOf(0f, 0f, 1f, 3.1416f, 100f, 999f, 0f, 0f, 0f)
    }

    fun showStyleEditor(ref: Style? = null) {

        /*  You can pass in a reference ImGuiStyle structure to compare to, revert to and save to
            (else it compares to the default style)         */
        val defaultStyle = Style()  // Default style
        button("Revert Style") {
            style = ref ?: defaultStyle
        }

        ref?.let {
            sameLine()
            button("Save Style") {
                TODO()//*ref = style
            }
        }

        pushItemWidth(windowWidth * 0.55f)

        treeNode("Rendering") {
            checkbox("Anti-aliased lines", bool.apply { set(0, style.antiAliasedLines) })
            style.antiAliasedLines = bool[0]
            checkbox("Anti-aliased shapes", bool.apply { this[0] = style.antiAliasedShapes })
            style.antiAliasedShapes = bool[0]
            pushItemWidth(100f)
            dragFloat("Curve Tessellation Tolerance", pF.apply { this[1] = style.curveTessellationTol }, 1,
                    0.02f, 0.1f, Float.MAX_VALUE, "", 2f)
            style.curveTessellationTol = pF[1]
            if (style.curveTessellationTol < 0f) style.curveTessellationTol = 0.1f
            /*  Not exposing zero here so user doesn't "lose" the UI (zero alpha clips all widgets).
                But application code could have a toggle to switch between zero and non-zero.             */
            dragFloat("Global Alpha", pF.apply { this[2] = style.alpha }, 2, 0.005f, 0.2f, 1f, "%.2f")
            style.alpha = pF[2]
            popItemWidth()
        }

        treeNode("Settings") {
            sliderFloatVec2("WindowPadding", style.windowPadding, 0f, 20f, "%.0f")
            sliderFloat("WindowRounding", pF.apply { this[3] = style.windowRounding }, 3, 0f, 16f, "%.0f")
            style.windowRounding = pF[3]
            sliderFloat("ChildWindowRounding", pF.apply { this[4] = style.childWindowRounding }, 4, 0f, 16f, "%.0f")
            style.childWindowRounding = pF[4]
            sliderFloatVec2("FramePadding", style.framePadding, 0f, 20f, "%.0f")
            sliderFloat("FrameRounding", pF.apply { this[5] = style.frameRounding }, 5, 0f, 16f, "%.0f")
            style.frameRounding = pF[5]
            sliderFloatVec2("ItemSpacing", style.itemSpacing, 0f, 20f, "%.0f")
            sliderFloatVec2("ItemInnerSpacing", style.itemInnerSpacing, 0f, 20f, "%.0f")
            sliderFloatVec2("TouchExtraPadding", style.touchExtraPadding, 0f, 10f, "%.0f")
            sliderFloat("IndentSpacing", pF.apply { this[6] = style.indentSpacing }, 6, 0f, 30f, "%.0f")
            style.indentSpacing = pF[6]
            sliderFloat("ScrollbarSize", pF.apply { this[7] = style.scrollbarSize }, 7, 1f, 20f, "%.0f")
            style.scrollbarSize = pF[7]
            sliderFloat("ScrollbarRounding", pF.apply { this[8] = style.scrollbarRounding }, 8, 0.0f, 16.0f, "%.0f")
            style.scrollbarRounding = pF[8]
            sliderFloat("GrabMinSize", pF.apply { this[9] = style.grabMinSize }, 9, 1f, 20f, "%.0f")
            style.grabMinSize = pF[9]
            sliderFloat("GrabRounding", pF.apply { this[10] = style.grabRounding }, 10, 0f, 16f, "%.0f")
            style.grabRounding = pF[10]
            text("Alignment")
            sliderFloatVec2("WindowTitleAlign", style.windowTitleAlign, 0f, 1f, "%.2f")
            sliderFloatVec2("ButtonTextAlign", style.buttonTextAlign, 0f, 1f, "%.2f")
            sameLine()
            showHelpMarker("Alignment applies when a button is larger than its text content.")
        }

        treeNode("Colors") {

            button("Copy Colors") {
                if (outputDest[0] == 0)
                    logToClipboard()
                else
                    TODO() //logToTTY()
                //ImGui::LogText("ImVec4* colors = ImGui::GetStyle().Colors;" IM_NEWLINE); TODO
                for (i in Col.values()) {
                    val col = style.colors[i]
                    val name = i.name
                    if (!outputOnlyModified[0] || col != (ref?.colors?.get(i) ?: defaultStyle.colors[i]))
                        TODO()//logText("colors[ImGuiCol_%s]%*s= ImVec4(%.2ff, %.2ff, %.2ff, %.2ff);" IM_NEWLINE, name, 23 - (int)strlen(name), "", col.x, col.y, col.z, col.w);
                }
                logFinish()
            }
            sameLine()
            withItemWidth(120f) { combo("##output_type", outputDest, "To Clipboard\u0000To TTY\u0000") }
            sameLine()
            checkbox("Only Modified Fields", outputOnlyModified)

            text("Tip: Left-click on colored square to open color picker,\nRight-click to open edit options menu.")

//            static ImGuiColorEditFlags alpha_flags = 0; TODO (also 10 loc below)
//            ImGui::RadioButton("Opaque", &alpha_flags, 0); ImGui::SameLine();
//            ImGui::RadioButton("Alpha", &alpha_flags, ImGuiColorEditFlags_AlphaPreview); ImGui::SameLine();
//
//            radioButton("Both", alphaFlags, ColorEditFlags.AlphaPreviewHalf)

            beginChild("#colors", Vec2(0, 300), true, Wf.AlwaysVerticalScrollbar.i)
            pushItemWidth(-160f)
            for (i in 0 until Col.COUNT.i) {
                val name = Col.values()[i].name
                if (!filter.passFilter(name)) // TODO fix bug
                    continue
                withId(i) {
                    colorEditVec4(name, style.colors[i], Cef.AlphaBar.i) // | alpha_flags); TODO
                    if (style.colors[i] != (ref?.colors?.get(i) ?: defaultStyle.colors[i])) {
                        sameLine()
                        button("Revert") { style.colors[i] put (ref?.colors?.get(i) ?: defaultStyle.colors[i]) }
                        ref?.let {
                            sameLine()
                            button("Save") { it.colors[i] = style.colors[i] }
                        }
                    }
                }
            }
            popItemWidth()
            endChild()
        }

        val fontsOpened = treeNode("Fonts", "Fonts (${IO.fonts.fonts.size})")
        sameLine(); showHelpMarker("Tip: Load fonts with IO.fonts.addFontFromFileTTF()\nbefore calling IO.fonts.getTex* functions.")
        if (fontsOpened) {
            val atlas = IO.fonts
            treeNode("Atlas texture", "Atlas texture (${atlas.texSize.x}x${atlas.texSize.y} pixels)") {
                image(atlas.texId, Vec2(atlas.texSize), Vec2(), Vec2(1), Vec4.fromColor(255, 255, 255, 255),
                        Vec4.fromColor(255, 255, 255, 128))
            }
            pushItemWidth(100f)
            for (i in 0 until atlas.fonts.size) {

                val font = atlas.fonts[i]
                val name = font.configData.getOrNull(0)?.name ?: ""
                val fontDetailsOpened = bulletText("Font $i: '$name', %.2f px, ${font.glyphs.size} glyphs", font.fontSize)
                sameLine(); smallButton("Set as default") { IO.fontDefault = font }
                if (fontsOpened) {
                    pushFont(font)
                    text("The quick brown fox jumps over the lazy dog")
                    popFont()
                    val scale = floatArrayOf(font.scale)
                    // Scale only this font
                    dragFloat("Font scale", scale, 0.005f, 0.3f, 2f, "%.1f")
                    font.scale = scale[0]
                    sameLine()
                    showHelpMarker("""
                        |Note than the default embedded font is NOT meant to be scaled.
                        |
                        |Font are currently rendered into bitmaps at a given size at the time of building the atlas. You may oversample them to get some flexibility with scaling. You can also render at multiple sizes and select which one to use at runtime.
                        |
                        |(Glimmer of hope: the atlas system should hopefully be rewritten in the future to make scaling more natural and automatic.)""".trimMargin())
                    text("Ascent: ${font.ascent}, Descent: ${font.descent}, Height: ${font.ascent - font.descent}")
                    text("Fallback character: '${font.fallbackChar}' (${font.fallbackChar.i})")
                    val side = glm.sqrt(font.metricsTotalSurface.f).i
                    text("Texture surface: ${font.metricsTotalSurface} pixels (approx) ~ ${side}x$side")
                    for (cfgI in font.configData.indices) {
                        val cfg = font.configData[cfgI]
                        bulletText("Input $cfgI: '${cfg.name}', Oversample: ${cfg.oversample}, PixelSnapH: ${cfg.pixelSnapH}")
                    }
                    treeNode("Glyphs", "Glyphs (${font.glyphs.size})") {
                        // Display all glyphs of the fonts in separate pages of 256 characters
                        // Forcefully/dodgily make FindGlyph() return NULL on fallback, which isn't the default behavior.
                        val glyphFallback = font.fallbackGlyph
                        font.fallbackGlyph = null
                        for (base in 0 until 0x10000 step 256) {
                            val count = (0 until 256).sumBy { if (font.findGlyph((base + it).c) != null) 1 else 0 }
                            val s = if (count > 1) "glyphs" else "glyph"
                            if (count > 0 && treeNode(base, "U+%04X..U+%04X ($count $s)", base, base + 255)) {
                                val cellSpacing = style.itemSpacing.y
                                val cellSize = Vec2(font.fontSize)
                                val basePos = Vec2(cursorScreenPos)
                                val drawList = windowDrawList
                                for (n in 0 until 256) {
                                    val cellP1 = Vec2(basePos.x + (n % 16) * (cellSize.x + cellSpacing),
                                            basePos.y + (n / 16) * (cellSize.y + cellSpacing))
                                    val cellP2 = Vec2(cellP1.x + cellSize.x, cellP1.y + cellSize.y)
                                    val glyph = font.findGlyph((base + n).c)
                                    drawList.addRect(cellP1, cellP2, COL32(255, 255, 255, if (glyph != null) 100 else 50))
                                    /*  We use ImFont::RenderChar as a shortcut because we don't have UTF-8 conversion
                                        functions available to generate a string.                                     */
                                    font.renderChar(drawList, cellSize.x, cellP1, Col.Text.u32, (base + n).c)
                                    if (glyph != null && isMouseHoveringRect(cellP1, cellP2))
                                        tooltip {
                                            text("Codepoint: U+%04X", base + n)
                                            separator()
                                            text("AdvanceX+1: %.1f", glyph.advanceX)
                                            text("Pos: (%.2f,%.2f)->(%.2f,%.2f)", glyph.x0, glyph.y0, glyph.x1, glyph.y1)
                                            text("UV: (%.3f,%.3f)->(%.3f,%.3f)", glyph.u0, glyph.v0, glyph.u1, glyph.v1)
                                        }
                                }
                                dummy(Vec2((cellSize.x + cellSpacing) * 16, (cellSize.y + cellSpacing) * 16))
                                treePop()
                            }
                        }
                        font.fallbackGlyph = glyphFallback
                    }
                }
            }
            val pF = floatArrayOf(windowScale)
            dragFloat("this window scale", pF, 0.005f, 0.3f, 2f, "%.1f")    // scale only this window
            windowScale = pF[0]
            pF[0] = IO.fontGlobalScale
            dragFloat("global scale", pF, 0.005f, 0.3f, 2f, "%.1f") // scale everything
            IO.fontGlobalScale = pF[0]
            popItemWidth()
            setWindowFontScale(windowScale)
        }
        popItemWidth()
    }

    fun showUserGuide() {
        bulletText("Double-click on title bar to collapse window.")
        bulletText("Click and drag on lower right corner to resize window.")
        bulletText("Click and drag on any empty space to move window.")
        bulletText("Mouse Wheel to scroll.")
        if (IO.fontAllowUserScaling)
            bulletText("CTRL+Mouse Wheel to zoom window contents.")
        bulletText("TAB/SHIFT+TAB to cycle through keyboard editable fields.")
        bulletText("CTRL+Click on a slider or drag box to input text.")
        bulletText(
                "While editing text:\n" +
                        "- Hold SHIFT or use mouse to select text\n" +
                        "- CTRL+Left/Right to word jump\n" +
                        "- CTRL+A or double-click to select all\n" +
                        "- CTRL+X,CTRL+C,CTRL+V clipboard\n" +
                        "- CTRL+Z,CTRL+Y undo/redo\n" +
                        "- ESCAPE to revert\n" +
                        "- You can apply arithmetic operators +,*,/ on numerical values.\n" +
                        "  Use +- to subtract.\n")
    }

    companion object {

        fun showHelpMarker(desc: String) {
            textDisabled("(?)")
            if (isItemHovered()) {
                beginTooltip()
                pushTextWrapPos(450f)
                textUnformatted(desc)
                popTextWrapPos()
                endTooltip()
            }
        }

        /** Demonstrate creating a fullscreen menu bar and populating it.   */
        fun showExampleAppMainMenuBar() {

            if (beginMainMenuBar()) {
                if (beginMenu("File")) {
                    showExampleMenuFile()
                    endMenu()
                }
                if (beginMenu("Edit")) {
                    if (menuItem("Undo", "CTRL+Z")) {
                    }
                    if (menuItem("Redo", "CTRL+Y", false, false)) { // Disabled item
                    }
                    separator()
                    if (menuItem("Cut", "CTRL+X")) {
                    }
                    if (menuItem("Copy", "CTRL+C")) {
                    }
                    if (menuItem("Paste", "CTRL+V")) {
                    }
                    endMenu()
                }
                endMainMenuBar()
            }
        }

        fun showExampleMenuFile() {

            menuItem("(dummy menu)", "", false, false)
            if (menuItem("New")) Unit
            if (menuItem("Open", "Ctrl+O")) Unit
            if (beginMenu("Open Recent")) {

                menuItem("fish_hat.c")
                menuItem("fish_hat.inl")
                menuItem("fish_hat.h")
                if (beginMenu("More..")) {

                    menuItem("Hello")
                    menuItem("Sailor")
                    if (beginMenu("Recurse..")) {

                        showExampleMenuFile()
                        endMenu()
                    }
                    endMenu()
                }
                endMenu()
            }
            if (menuItem("Save", "Ctrl+S")) {
            }
            if (menuItem("Save As..")) {
            }
            separator()
            if (beginMenu("Options")) {
                menuItem("Enabled", "", enabled)
                beginChild("child", Vec2(0, 60), true)
                for (i in 0 until 10)
                    text("Scrolling Text %d", i)
                endChild()
                sliderFloat("Value", pF, 0f, 1f)
                inputFloat("Input", pF, 0.1f, 0f, 2)
                combo("Combo", n, "Yes\u0000No\u0000Maybe\u0000\u0000")
                checkbox("Check", checkbox)
                endMenu()
            }
            if (beginMenu("Colors")) {
                for (col in Col.values())
                    menuItem(col.toString())
                endMenu()
            }
            if (beginMenu("Disabled", false)) // Disabled
                assert(false)
            if (menuItem("Checked", selected = true)) {
            }
            if (menuItem("Quit", "Alt+F4")) {
            }
        }

        var enabled = booleanArrayOf(true)
        /**
         * [0] = sliderFloat for showExampleMenuFile()
         * [1] = "Curve Tessellation Tolerance" for showStyleEditor()
         * [2] = "Global Alpha" for showStyleEditor()
         * [3] = "style.windowRounding" for showStyleEditor()
         * [4] = "style.childWindowRounding" for showStyleEditor()
         * [5] = "style.frameRounding" for showStyleEditor()
         * [6] = "style.indentSpacing" for showStyleEditor()
         * [7] = "style.scrollbarSize" for showStyleEditor()
         * [8] = "style.scrollbarRounding" for showStyleEditor()
         * [9] = "style.grabMinSize" for showStyleEditor()
         * [10] = "style.grabRounding" for showStyleEditor()
         */
        val pF = FloatArray(11, { if (it == 0) 0.5f else 0f })
        val n = intArrayOf(0)
        val bool = BooleanArray(1)

        /** Demonstrate creating a window which gets auto-resized according to its content. */
        fun showExampleAppAutoResize(open: KMutableProperty0<Boolean>) {

            if (!_begin("Example: Auto-resizing window", open, Wf.AlwaysAutoResize.i)) {
                end()
                return
            }

            text("Window will resize every-frame to the size of its content.\nNote that you probably don't want to " +
                    "query the window size to\noutput your content because that would create a feedback loop.")
            sliderInt("Number of lines", lines, 1, 20)
            for (i in 0 until lines[0])
                text(" ".repeat(i * 4) + "This is line $i") // Pad with space to extend size horizontally
            end()
        }

        val lines = intArrayOf(10)

        /** Demonstrate creating a window with custom resize constraints.   */
        fun showExampleAppConstrainedResize(open: KMutableProperty0<Boolean>) {

            when (type[0]) {
                0 -> setNextWindowSizeConstraints(Vec2(-1, 0), Vec2(-1, Float.MAX_VALUE))      // Vertical only
                1 -> setNextWindowSizeConstraints(Vec2(0, -1), Vec2(Float.MAX_VALUE, -1))      // Horizontal only
                2 -> setNextWindowSizeConstraints(Vec2(100), Vec2(Float.MAX_VALUE)) // Width > 100, Height > 100
                3 -> setNextWindowSizeConstraints(Vec2(300, 0), Vec2(400, Float.MAX_VALUE))     // Width 300-400
                4 -> setNextWindowSizeConstraints(Vec2(), Vec2(Float.MAX_VALUE), CustomConstraints.square)          // Always Square
                5 -> setNextWindowSizeConstraints(Vec2(), Vec2(Float.MAX_VALUE), CustomConstraints.step, 100)// Fixed Step
            }

            window("Example: Constrained Resize", open) {
                val desc = listOf("Resize vertical only", "Resize horizontal only", "Width > 100, Height > 100",
                        "Width 300-400", "Custom: Always Square", "Custom: Fixed Steps (100)")
                combo("Constraint", type, desc)
                button("200x200") { setWindowSize(Vec2(200)) }; sameLine()
                button("500x500") { setWindowSize(Vec2(500)) }; sameLine()
                button("800x200") { setWindowSize(Vec2(800, 200)) }
                for (i in 0 until 10)
                    text("Hello, sailor! Making this line long enough for the example.")
            }
        }

        /** Helper functions to demonstrate programmatic constraints    */
        object CustomConstraints {
            val square: SizeConstraintCallback = { _: Any?, _: Vec2i, _: Vec2, desiredSize: Vec2 ->
                desiredSize put glm.max(desiredSize.x, desiredSize.y)
            }
            val step: SizeConstraintCallback = { userData: Any?, _: Vec2i, _: Vec2, desiredSize: Vec2 ->
                val step = (userData as Int).f
                desiredSize.x = (desiredSize.x / step + 0.5f).i * step
                desiredSize.y = (desiredSize.y / step + 0.5f).i * step
            }
        }

        var type = intArrayOf(0)

        /** Demonstrate creating a simple static window with no decoration + a context-menu to choose which corner
         *  of the screen to use */
        fun showExampleAppFixedOverlay(open: KMutableProperty0<Boolean>) {

            val DISTANCE = 10f
            val windowPos = Vec2(if (corner has 1) IO.displaySize.x - DISTANCE else DISTANCE,
                    if (corner has 2) IO.displaySize.y - DISTANCE else DISTANCE)
            val windowPosPivot = Vec2(if (corner has 1) 1f else 0f, if (corner has 2) 1f else 0f)
            setNextWindowPos(windowPos, Cond.Always, windowPosPivot)
            pushStyleColor(Col.WindowBg, Vec4(0f, 0f, 0f, 0.3f))  // Transparent background
            if (_begin("Example: Fixed Overlay", open, Wf.NoTitleBar or Wf.NoResize or Wf.AlwaysAutoResize or Wf.NoMove or Wf.NoSavedSettings)) {
                text("Simple overlay\nin the corner of the screen.\n(right-click to change position)")
                separator()
                text("Mouse Position: (%.1f,%.1f)".format(IO.mousePos.x, IO.mousePos.y))
//                if(beginPopupContextWindow()) { TODO
//                    if (ImGui::MenuItem("Top-left", NULL, corner == 0)) corner = 0
//                    if (ImGui::MenuItem("Top-right", NULL, corner == 1)) corner = 1
//                    if (ImGui::MenuItem("Bottom-left", NULL, corner == 2)) corner = 2
//                    if (ImGui::MenuItem("Bottom-right", NULL, corner == 3)) corner = 3
//                    ImGui::EndPopup()
//                }
                end()
            }
            popStyleColor()
        }

        var corner = 0

        /** Demonstrate using "##" and "###" in identifiers to manipulate ID generation.
         *  Read section "How can I have multiple widgets with the same label? Can I have widget without a label? (Yes).
         *  A primer on the purpose of labels/IDs." about ID.   */
        fun showExampleAppManipulatingWindowTitle(open: KMutableProperty0<Boolean>) {

            /*  By default, Windows are uniquely identified by their title.
                You can use the "##" and "###" markers to manipulate the display/ID.
             */
//TODO
            // Using "##" to display same title but have unique identifier.
//            setNextWindowPos(Vec2(100), Cond.FirstUseEver)
//            window("Same title as another window##1") {
//                text("This is window 1.\nMy title is the same as window 2, but my identifier is unique.")
//            }
//
//            setNextWindowPos(Vec2(100, 200), Cond.FirstUseEver)
//            window("Same title as another window##2") {
//                text("This is window 2.\nMy title is the same as window 1, but my identifier is unique.")
//            }
//
//            // Using "###" to display a changing title but keep a static identifier "AnimatedTitle"
//            val title = "Animated title ${"|/-\\"[(time / 0.25f).i and 3]} ${glm_.detail.Random.int}###AnimatedTitle"
//            setNextWindowPos(Vec2(100, 300), Cond.FirstUseEver)
//            window(title) { text("This window has a changing title.") }
        }

        /** Demonstrate using the low-level ImDrawList to draw custom shapes.   */
        fun showExampleAppCustomRendering(pOpen: BooleanArray) {

            setNextWindowSize(Vec2(350, 560), Cond.FirstUseEver)
            if (!begin("Example: Custom rendering", pOpen)) {
                end()
                return
            }

            text("TODO")

            /*  Tip: If you do a lot of custom rendering, you probably want to use your own geometrical types and
                benefit of overloaded operators, etc.
                Define IM_VEC2_CLASS_EXTRA in imconfig.h to create implicit conversions between your types and
                ImVec2/ImVec4.
                ImGui defines overloaded operators but they are internal to imgui.cpp and not exposed outside
                (to avoid messing with your types)
                In this example we are not using the maths operators!   */
//            ImDrawList* draw_list = ImGui::GetWindowDrawList();
//
//            // Primitives
//            ImGui::Text("Primitives");
//            static float sz = 36.0f;
//            static ImVec4 col = ImVec4(1.0f,1.0f,0.4f,1.0f);
//            ImGui::DragFloat("Size", &sz, 0.2f, 2.0f, 72.0f, "%.0f");
//            ImGui::ColorEdit3("Color", &col.x);
//            {
//                const ImVec2 p = ImGui::GetCursorScreenPos();
//                const ImU32 col32 = ImColor(col);
//                float x = p.x + 4.0f, y = p.y + 4.0f, spacing = 8.0f;
//                for (int n = 0; n < 2; n++)
//                {
//                    float thickness = (n == 0) ? 1.0f : 4.0f;
//                    draw_list->AddCircle(ImVec2(x+sz*0.5f, y+sz*0.5f), sz*0.5f, col32, 20, thickness); x += sz+spacing;
//                    draw_list->AddRect(ImVec2(x, y), ImVec2(x+sz, y+sz), col32, 0.0f, ~0, thickness); x += sz+spacing;
//                    draw_list->AddRect(ImVec2(x, y), ImVec2(x+sz, y+sz), col32, 10.0f, ~0, thickness); x += sz+spacing;
//                    draw_list->AddTriangle(ImVec2(x+sz*0.5f, y), ImVec2(x+sz,y+sz-0.5f), ImVec2(x,y+sz-0.5f), col32, thickness); x += sz+spacing;
//                    draw_list->AddLine(ImVec2(x, y), ImVec2(x+sz, y   ), col32, thickness); x += sz+spacing;
//                    draw_list->AddLine(ImVec2(x, y), ImVec2(x+sz, y+sz), col32, thickness); x += sz+spacing;
//                    draw_list->AddLine(ImVec2(x, y), ImVec2(x,    y+sz), col32, thickness); x += spacing;
//                    draw_list->AddBezierCurve(ImVec2(x, y), ImVec2(x+sz*1.3f,y+sz*0.3f), ImVec2(x+sz-sz*1.3f,y+sz-sz*0.3f), ImVec2(x+sz, y+sz), col32, thickness);
//                    x = p.x + 4;
//                    y += sz+spacing;
//                }
//                draw_list->AddCircleFilled(ImVec2(x+sz*0.5f, y+sz*0.5f), sz*0.5f, col32, 32); x += sz+spacing;
//                draw_list->AddRectFilled(ImVec2(x, y), ImVec2(x+sz, y+sz), col32); x += sz+spacing;
//                draw_list->AddRectFilled(ImVec2(x, y), ImVec2(x+sz, y+sz), col32, 10.0f); x += sz+spacing;
//                draw_list->AddTriangleFilled(ImVec2(x+sz*0.5f, y), ImVec2(x+sz,y+sz-0.5f), ImVec2(x,y+sz-0.5f), col32); x += sz+spacing;
//                draw_list->AddRectFilledMultiColor(ImVec2(x, y), ImVec2(x+sz, y+sz), ImColor(0,0,0), ImColor(255,0,0), ImColor(255,255,0), ImColor(0,255,0));
//                ImGui::Dummy(ImVec2((sz+spacing)*8, (sz+spacing)*3));
//            }
//            ImGui::Separator();
//            {
//                static ImVector<ImVec2> points;
//                static bool adding_line = false;
//                ImGui::Text("Canvas example");
//                if (ImGui::Button("Clear")) points.clear();
//                if (points.Size >= 2) { ImGui::SameLine(); if (ImGui::Button("Undo")) { points.pop_back(); points.pop_back(); } }
//                ImGui::Text("Left-click and drag to add lines,\nRight-click to undo");
//
//                // Here we are using InvisibleButton() as a convenience to 1) advance the cursor and 2) allows us to use IsItemHovered()
//                // However you can draw directly and poll mouse/keyboard by yourself. You can manipulate the cursor using GetCursorPos() and SetCursorPos().
//                // If you only use the ImDrawList API, you can notify the owner window of its extends by using SetCursorPos(max).
//                ImVec2 canvas_pos = ImGui::GetCursorScreenPos();            // ImDrawList API uses screen coordinates!
//                ImVec2 canvas_size = ImGui::GetContentRegionAvail();        // Resize canvas to what's available
//                if (canvas_size.x < 50.0f) canvas_size.x = 50.0f;
//                if (canvas_size.y < 50.0f) canvas_size.y = 50.0f;
//                draw_list->AddRectFilledMultiColor(canvas_pos, ImVec2(canvas_pos.x + canvas_size.x, canvas_pos.y + canvas_size.y), ImColor(50,50,50), ImColor(50,50,60), ImColor(60,60,70), ImColor(50,50,60));
//                draw_list->AddRect(canvas_pos, ImVec2(canvas_pos.x + canvas_size.x, canvas_pos.y + canvas_size.y), ImColor(255,255,255));
//
//                bool adding_preview = false;
//                ImGui::InvisibleButton("canvas", canvas_size);
//                ImVec2 mouse_pos_in_canvas = ImVec2(ImGui::GetIO().MousePos.x - canvas_pos.x, ImGui::GetIO().MousePos.y - canvas_pos.y);
//                if (adding_line)
//                {
//                    adding_preview = true;
//                    points.push_back(mouse_pos_in_canvas);
//                    if (!ImGui::GetIO().MouseDown[0])
//                        adding_line = adding_preview = false;
//                }
//                if (ImGui::IsItemHovered())
//                {
//                    if (!adding_line && ImGui::IsMouseClicked(0))
//                    {
//                        points.push_back(mouse_pos_in_canvas);
//                        adding_line = true;
//                    }
//                    if (ImGui::IsMouseClicked(1) && !points.empty())
//                    {
//                        adding_line = adding_preview = false;
//                        points.pop_back();
//                        points.pop_back();
//                    }
//                }
//                draw_list->PushClipRect(canvas_pos, ImVec2(canvas_pos.x+canvas_size.x, canvas_pos.y+canvas_size.y));      // clip lines within the canvas (if we resize it, etc.)
//                for (int i = 0; i < points.Size - 1; i += 2)
//                draw_list->AddLine(ImVec2(canvas_pos.x + points[i].x, canvas_pos.y + points[i].y), ImVec2(canvas_pos.x + points[i+1].x, canvas_pos.y + points[i+1].y), IM_COL32(255,255,0,255), 2.0f);
//                draw_list->PopClipRect();
//                if (adding_preview)
//                    points.pop_back();
//            }
            end()
        }

        fun showExampleAppConsole(open: KMutableProperty0<Boolean>) = console.draw("Example: Console", open)

        val console = ExampleAppConsole()

        /** Demonstrate creating a simple log window with basic filtering.  */
        fun showExampleAppLog(open: KMutableProperty0<Boolean>) {

            // Demo: add random items (unless Ctrl is held)
            val time = ImGui.time
            if (time - lastTime >= 0.2f && !IO.keyCtrl) {
                val s = randomWords[rand % randomWords.size]
                val t = "%.1f".format(style.locale, time)
                log.addLog("[$s] Hello, time is $t, rand() $rand\n")
                lastTime = time
            }
            log.draw("Example: Log (Filter not yet implemented)", open)
        }

        val log = ExampleAppLog()
        var lastTime = -1f
        val randomWords = arrayOf("system", "info", "warning", "error", "fatal", "notice", "log")
        val random = Random()
        val rand get() = glm.abs(random.nextInt() / 100_000)

        /** Demonstrate create a window with multiple child windows.    */
        fun showExampleAppLayout(open: KMutableProperty0<Boolean>) {

            setNextWindowSize(Vec2(500, 440), Cond.FirstUseEver)
            if (_begin("Example: Layout", open, Wf.MenuBar.i)) {
                if (beginMenuBar()) {
                    if (beginMenu("File")) {
                        if (menuItem("Close")) open.set(false)
                        endMenu()
                    }
                    endMenuBar()
                }

                // left
                beginChild("left pane", Vec2(150, 0), true)
                repeat(100) {
                    if (selectable("MyObject $it", selectedChild == it))
                        selectedChild = it
                }
                endChild()
                sameLine()

                // right
                beginGroup()
                beginChild("item view", Vec2(0, -itemsLineHeightWithSpacing)) // Leave room for 1 line below us
                text("MyObject: $selectedChild")
                separator()
                textWrapped("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor " +
                        "incididunt ut labore et dolore magna aliqua. ")
                endChild()
                beginChild("buttons")
                if (button("Revert")) Unit
                sameLine()
                if (button("Save")) Unit
                endChild()
                endGroup()
            }
            end()
        }

        var selectedChild = 0

        /** Demonstrate create a simple property editor.    */
        fun showExampleAppPropertyEditor(open: KMutableProperty0<Boolean>) {

            setNextWindowSize(Vec2(430, 450), Cond.FirstUseEver)
            if (!_begin("Example: Property editor", open)) {
                end()
                return
            }

            showHelpMarker("This example shows how you may implement a property editor using two columns.\n" +
                    "All objects/fields data are dummies here.\n" +
                    "Remember that in many simple cases, you can use ImGui::SameLine(xxx) to position\n" +
                    "your cursor horizontally instead of using the Columns() API.")

            pushStyleVar(StyleVar.FramePadding, Vec2(2))
            columns(2)
            separator()


            // Iterate dummy objects with dummy members (all the same data)
            for (objI in 0..2)
                Funcs.showDummyObject("Object", objI)

            columns(1)
            separator()
            popStyleVar()
            end()
        }

        /** Demonstrate/test rendering huge amount of text, and the incidence of clipping.  */
        fun showExampleAppLongText(open: KMutableProperty0<Boolean>) {

            setNextWindowSize(Vec2(520, 600), Cond.FirstUseEver)
            if (!_begin("Example: Long text display, TODO", open)) {
                end()
                return
            }

//            static int test_type = 0;
//            static ImGuiTextBuffer log;
//            static int lines = 0;
//            ImGui::Text("Printing unusually long amount of text.");
//            ImGui::Combo("Test type", &test_type, "Single call to TextUnformatted()\0Multiple calls to Text(), clipped manually\0Multiple calls to Text(), not clipped\0");
//            ImGui::Text("Buffer contents: %d lines, %d bytes", lines, log.size());
//            if (ImGui::Button("Clear")) { log.clear(); lines = 0; }
//            ImGui::SameLine();
//            if (ImGui::Button("Add 1000 lines"))
//            {
//                for (int i = 0; i < 1000; i++)
//                log.append("%i The quick brown fox jumps over the lazy dog\n", lines+i);
//                lines += 1000;
//            }
//            ImGui::BeginChild("Log");
//            switch (test_type)
//            {
//                case 0:
//                // Single call to TextUnformatted() with a big buffer
//                ImGui::TextUnformatted(log.begin(), log.end());
//                break;
//                case 1:
//                {
//                    // Multiple calls to Text(), manually coarsely clipped - demonstrate how to use the ImGuiListClipper helper.
//                    ImGui::PushStyleVar(ImGuiStyleVar_ItemSpacing, ImVec2(0,0));
//                    ImGuiListClipper clipper(lines);
//                    while (clipper.Step())
//                        for (int i = clipper.DisplayStart; i < clipper.DisplayEnd; i++)
//                    ImGui::Text("%i The quick brown fox jumps over the lazy dog", i);
//                    ImGui::PopStyleVar();
//                    break;
//                }
//                case 2:
//                // Multiple calls to Text(), not clipped (slow)
//                ImGui::PushStyleVar(ImGuiStyleVar_ItemSpacing, ImVec2(0,0));
//                for (int i = 0; i < lines; i++)
//                ImGui::Text("%i The quick brown fox jumps over the lazy dog", i);
//                ImGui::PopStyleVar();
//                break;
//            }
//            ImGui::EndChild();
            end()
        }

        object showApp {
            // Examples apps
            var mainMenuBar = false
            var console = false
            var log = false
            var layout = false
            var propertyEditor = false
            var longText = false
            var autoResize = false
            var constrainedResize = false
            var fixedOverlay = false
            var manipulatingWindowTitle = false
            var customRendering = booleanArrayOf(false)
            var styleEditor = false

            var metrics = booleanArrayOf(false)
            var about = booleanArrayOf(false)
        }

        var noTitlebar = booleanArrayOf(false)
        var noBorder = booleanArrayOf(true)
        var noResize = booleanArrayOf(false)
        var noMove = booleanArrayOf(false)
        var noScrollbar = booleanArrayOf(false)
        var noCollapse = booleanArrayOf(false)
        var noMenu = booleanArrayOf(false)


        val showClipRects = booleanArrayOf(true)

        val listboxItemCurrent = intArrayOf(1)

        val dontAskMeNextTime = booleanArrayOf(false)

        val outputDest = intArrayOf(0)
        val outputOnlyModified = booleanArrayOf(false)

        var colorFlags = intArrayOf(Cef.RGB.i)

        val filter = TextFilter()

        var windowScale = 1f

        val alignLabelWithCurrentXposition = booleanArrayOf(false)

        /** Dumb representation of what may be user-side selection state. You may carry selection state inside or
         *  outside your objects in whatever format you see fit.    */
        var selectionMask = 1 shl 2

        val closableGroup = booleanArrayOf(true)

        val wrapWidth = floatArrayOf(200f)

        val buf = CharArray(32).apply { "\u00e6\u0097\u00a5\u00e6\u009c\u00ac\u00e8\u00aa\u009e".toCharArray(this) }

        var pressedCount = 0

        val selected = BooleanArray(4 + 3 + 16 + 16, { it == 1 || it == 23 + 0 || it == 23 + 5 || it == 23 + 10 || it == 23 + 15 })

        val readOnly = booleanArrayOf(false)

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

        val checkbox = booleanArrayOf(true)
    }

    /** Demonstrating creating a simple console window, with scrolling, filtering, completion and history.
     *  For the console example, here we are using a more C++ like approach of declaring a class to hold the data and
     *  the functions.  */
    class ExampleAppConsole {
        //        char                  InputBuf[256];
//        ImVector<char*>       Items;
//        bool                  ScrollToBottom;
//        ImVector<char*>       History;
//        int                   HistoryPos;    // -1: new line, 0..History.Size-1 browsing history.
//        ImVector<const char*> Commands;
//
//        ExampleAppConsole()
//        {
//            ClearLog();
//            memset(InputBuf, 0, sizeof(InputBuf));
//            HistoryPos = -1;
//            Commands.push_back("HELP");
//            Commands.push_back("HISTORY");
//            Commands.push_back("CLEAR");
//            Commands.push_back("CLASSIFY");  // "classify" is here to provide an example of "C"+[tab] completing to "CL" and displaying matches.
//            AddLog("Welcome to ImGui!");
//        }
//        ~ExampleAppConsole()
//    {
//        ClearLog();
//        for (int i = 0; i < History.Size; i++)
//        free(History[i]);
//    }
//
//        // Portable helpers
//        static int   Stricmp(const char* str1, const char* str2)         { int d; while ((d = toupper(*str2) - toupper(*str1)) == 0 && *str1) { str1++; str2++; } return d; }
//        static int   Strnicmp(const char* str1, const char* str2, int n) { int d = 0; while (n > 0 && (d = toupper(*str2) - toupper(*str1)) == 0 && *str1) { str1++; str2++; n--; } return d; }
//        static char* Strdup(const char *str)                             { size_t len = strlen(str) + 1; void* buff = malloc(len); return (char*)memcpy(buff, (const void*)str, len); }
//
//        void    ClearLog()
//        {
//            for (int i = 0; i < Items.Size; i++)
//            free(Items[i]);
//            Items.clear();
//            ScrollToBottom = true;
//        }
//
//        void    AddLog(const char* fmt, ...) IM_PRINTFARGS(2)
//        {
//            char buf[1024];
//            va_list args;
//            va_start(args, fmt);
//            vsnprintf(buf, IM_ARRAYSIZE(buf), fmt, args);
//            buf[IM_ARRAYSIZE(buf)-1] = 0;
//            va_end(args);
//            Items.push_back(Strdup(buf));
//            ScrollToBottom = true;
//        }
//
        fun draw(title: String, open: KMutableProperty0<Boolean>) {

            setNextWindowSize(Vec2(520, 600), Cond.FirstUseEver)
            if (!_begin(title, open)) {
                end()
                return
            }

            textWrapped("This example is not yet implemented, you are welcome to contribute")
//            textWrapped("This example implements a console with basic coloring, completion and history. A more elaborate implementation may want to store entries along with extra data such as timestamp, emitter, etc.");
//            ImGui::TextWrapped("Enter 'HELP' for help, press TAB to use text completion.");
//
//            // TODO: display items starting from the bottom
//
//            if (ImGui::SmallButton("Add Dummy Text")) { AddLog("%d some text", Items.Size); AddLog("some more text"); AddLog("display very important message here!"); } ImGui::SameLine();
//            if (ImGui::SmallButton("Add Dummy Error")) { AddLog("[error] something went wrong"); } ImGui::SameLine();
//            if (ImGui::SmallButton("Clear")) { ClearLog(); } ImGui::SameLine();
//            bool copy_to_clipboard = ImGui::SmallButton("Copy"); ImGui::SameLine();
//            if (ImGui::SmallButton("Scroll to bottom")) ScrollToBottom = true;
//            //static float t = 0.0f; if (ImGui::GetTime() - t > 0.02f) { t = ImGui::GetTime(); AddLog("Spam %f", t); }
//
//            ImGui::Separator();
//
//            ImGui::PushStyleVar(ImGuiStyleVar_FramePadding, ImVec2(0,0));
//            static ImGuiTextFilter filter;
//            filter.Draw("Filter (\"incl,-excl\") (\"error\")", 180);
//            ImGui::PopStyleVar();
//            ImGui::Separator();
//
//            ImGui::BeginChild("ScrollingRegion", ImVec2(0,-ImGui::GetItemsLineHeightWithSpacing()), false, ImGuiWindowFlags_HorizontalScrollbar);
//            if (ImGui::BeginPopupContextWindow())
//            {
//                if (ImGui::Selectable("Clear")) ClearLog();
//                ImGui::EndPopup();
//            }
//
//            // Display every line as a separate entry so we can change their color or add custom widgets. If you only want raw text you can use ImGui::TextUnformatted(log.begin(), log.end());
//            // NB- if you have thousands of entries this approach may be too inefficient and may require user-side clipping to only process visible items.
//            // You can seek and display only the lines that are visible using the ImGuiListClipper helper, if your elements are evenly spaced and you have cheap random access to the elements.
//            // To use the clipper we could replace the 'for (int i = 0; i < Items.Size; i++)' loop with:
//            //     ImGuiListClipper clipper(Items.Size);
//            //     while (clipper.Step())
//            //         for (int i = clipper.DisplayStart; i < clipper.DisplayEnd; i++)
//            // However take note that you can not use this code as is if a filter is active because it breaks the 'cheap random-access' property. We would need random-access on the post-filtered list.
//            // A typical application wanting coarse clipping and filtering may want to pre-compute an array of indices that passed the filtering test, recomputing this array when user changes the filter,
//            // and appending newly elements as they are inserted. This is left as a task to the user until we can manage to improve this example code!
//            // If your items are of variable size you may want to implement code similar to what ImGuiListClipper does. Or split your data into fixed height items to allow random-seeking into your list.
//            ImGui::PushStyleVar(ImGuiStyleVar_ItemSpacing, ImVec2(4,1)); // Tighten spacing
//            if (copy_to_clipboard)
//                ImGui::LogToClipboard();
//            for (int i = 0; i < Items.Size; i++)
//            {
//                const char* item = Items[i];
//                if (!filter.PassFilter(item))
//                    continue;
//                ImVec4 col = ImVec4(1.0f,1.0f,1.0f,1.0f); // A better implementation may store a type per-item. For the sample let's just parse the text.
//                if (strstr(item, "[error]")) col = ImColor(1.0f,0.4f,0.4f,1.0f);
//                else if (strncmp(item, "# ", 2) == 0) col = ImColor(1.0f,0.78f,0.58f,1.0f);
//                ImGui::PushStyleColor(ImGuiCol_Text, col);
//                ImGui::TextUnformatted(item);
//                ImGui::PopStyleColor();
//            }
//            if (copy_to_clipboard)
//                ImGui::LogFinish();
//            if (ScrollToBottom)
//                ImGui::SetScrollHere();
//            ScrollToBottom = false;
//            ImGui::PopStyleVar();
//            ImGui::EndChild();
//            ImGui::Separator();
//
//            // Command-line
//            if (ImGui::InputText("Input", InputBuf, IM_ARRAYSIZE(InputBuf), ImGuiInputTextFlags_EnterReturnsTrue|ImGuiInputTextFlags_CallbackCompletion|ImGuiInputTextFlags_CallbackHistory, &TextEditCallbackStub, (void*)this))
//            {
//                char* input_end = InputBuf+strlen(InputBuf);
//                while (input_end > InputBuf && input_end[-1] == ' ') { input_end--; } *input_end = 0;
//                if (InputBuf[0])
//                    ExecCommand(InputBuf);
//                strcpy(InputBuf, "");
//            }
//
//            // Demonstrate keeping auto focus on the input box
//            if (ImGui::IsItemHovered() || (ImGui::IsRootWindowOrAnyChildFocused() && !ImGui::IsAnyItemActive() && !ImGui::IsMouseClicked(0)))
//                ImGui::SetKeyboardFocusHere(-1); // Auto focus previous widget
//
//            ImGui::End();
        }
//
//        void    ExecCommand(const char* command_line)
//        {
//            AddLog("# %s\n", command_line);
//
//            // Insert into history. First find match and delete it so it can be pushed to the back. This isn't trying to be smart or optimal.
//            HistoryPos = -1;
//            for (int i = History.Size-1; i >= 0; i--)
//            if (Stricmp(History[i], command_line) == 0)
//            {
//                free(History[i]);
//                History.erase(History.begin() + i);
//                break;
//            }
//            History.push_back(Strdup(command_line));
//
//            // Process command
//            if (Stricmp(command_line, "CLEAR") == 0)
//            {
//                ClearLog();
//            }
//            else if (Stricmp(command_line, "HELP") == 0)
//            {
//                AddLog("Commands:");
//                for (int i = 0; i < Commands.Size; i++)
//                AddLog("- %s", Commands[i]);
//            }
//            else if (Stricmp(command_line, "HISTORY") == 0)
//            {
//                int first = History.Size - 10;
//                for (int i = first > 0 ? first : 0; i < History.Size; i++)
//                AddLog("%3d: %s\n", i, History[i]);
//            }
//            else
//            {
//                AddLog("Unknown command: '%s'\n", command_line);
//            }
//        }
//
//        static int TextEditCallbackStub(ImGuiTextEditCallbackData* data) // In C++11 you are better off using lambdas for this sort of forwarding callbacks
//        {
//            ExampleAppConsole* console = (ExampleAppConsole*)data->UserData;
//            return console->TextEditCallback(data);
//        }
//
//        int     TextEditCallback(ImGuiTextEditCallbackData* data)
//        {
//            //AddLog("cursor: %d, selection: %d-%d", data->CursorPos, data->SelectionStart, data->SelectionEnd);
//            switch (data->EventFlag)
//            {
//                case ImGuiInputTextFlags_CallbackCompletion:
//                {
//                    // Example of TEXT COMPLETION
//
//                    // Locate beginning of current word
//                    const char* word_end = data->Buf + data->CursorPos;
//                    const char* word_start = word_end;
//                    while (word_start > data->Buf)
//                    {
//                        const char c = word_start[-1];
//                        if (c == ' ' || c == '\t' || c == ',' || c == ';')
//                            break;
//                        word_start--;
//                    }
//
//                    // Build a list of candidates
//                    ImVector<const char*> candidates;
//                    for (int i = 0; i < Commands.Size; i++)
//                    if (Strnicmp(Commands[i], word_start, (int)(word_end-word_start)) == 0)
//                        candidates.push_back(Commands[i]);
//
//                    if (candidates.Size == 0)
//                    {
//                        // No match
//                        AddLog("No match for \"%.*s\"!\n", (int)(word_end-word_start), word_start);
//                    }
//                    else if (candidates.Size == 1)
//                        {
//                            // Single match. Delete the beginning of the word and replace it entirely so we've got nice casing
//                            data->DeleteChars((int)(word_start-data->Buf), (int)(word_end-word_start));
//                            data->InsertChars(data->CursorPos, candidates[0]);
//                            data->InsertChars(data->CursorPos, " ");
//                        }
//                    else
//                    {
//                        // Multiple matches. Complete as much as we can, so inputing "C" will complete to "CL" and display "CLEAR" and "CLASSIFY"
//                        int match_len = (int)(word_end - word_start);
//                        for (;;)
//                        {
//                            int c = 0;
//                            bool all_candidates_matches = true;
//                            for (int i = 0; i < candidates.Size && all_candidates_matches; i++)
//                            if (i == 0)
//                                c = toupper(candidates[i][match_len]);
//                            else if (c == 0 || c != toupper(candidates[i][match_len]))
//                                all_candidates_matches = false;
//                            if (!all_candidates_matches)
//                                break;
//                            match_len++;
//                        }
//
//                        if (match_len > 0)
//                            {
//                                data->DeleteChars((int)(word_start - data->Buf), (int)(word_end-word_start));
//                                data->InsertChars(data->CursorPos, candidates[0], candidates[0] + match_len);
//                            }
//
//                        // List matches
//                        AddLog("Possible matches:\n");
//                        for (int i = 0; i < candidates.Size; i++)
//                        AddLog("- %s\n", candidates[i]);
//                    }
//
//                    break;
//                }
//                case ImGuiInputTextFlags_CallbackHistory:
//                {
//                    // Example of HISTORY
//                    const int prev_history_pos = HistoryPos;
//                    if (data->EventKey == ImGuiKey_UpArrow)
//                    {
//                        if (HistoryPos == -1)
//                            HistoryPos = History.Size - 1;
//                        else if (HistoryPos > 0)
//                            HistoryPos--;
//                    }
//                    else if (data->EventKey == ImGuiKey_DownArrow)
//                    {
//                        if (HistoryPos != -1)
//                            if (++HistoryPos >= History.Size)
//                                HistoryPos = -1;
//                    }
//
//                    // A better implementation would preserve the data on the current input line along with cursor position.
//                    if (prev_history_pos != HistoryPos)
//                        {
//                            data->CursorPos = data->SelectionStart = data->SelectionEnd = data->BufTextLen = (int)snprintf(data->Buf, (size_t)data->BufSize, "%s", (HistoryPos >= 0) ? History[HistoryPos] : "");
//                            data->BufDirty = true;
//                        }
//                }
//            }
//            return 0;
//        }
    }

    /** Usage:
     *      static ExampleAppLog my_log;
     *      my_log.AddLog("Hello %d world\n", 123);
     *      my_log.Draw("title");   */
    class ExampleAppLog {

        val buf = StringBuilder()
        val filter = TextFilter()// TODO
        //        ImVector<int>       LineOffsets;        // Index to lines offset
        var scrollToBottom = false

        fun addLog(fmt: String) {
            buf.append(fmt)
            scrollToBottom = true
        }

        fun clear() = buf.setLength(0)

        fun draw(title: String, open: KMutableProperty0<Boolean>? = null) {

            setNextWindowSize(Vec2(500, 400), Cond.FirstUseEver)
            _begin(title, open)
            if (button("Clear")) clear()
            sameLine()
            val copy = button("Copy")
            sameLine()
            filter.draw("Filter", -100f)
            separator()
            beginChild("scrolling", Vec2(0, 0), false, Wf.HorizontalScrollbar.i)
            if (copy) logToClipboard()

//      TODO      if (Filter.IsActive())
//            {
//                const char* buf_begin = Buf.begin()
//                const char* line = buf_begin
//                for (int line_no = 0; line != NULL; line_no++)
//                {
//                    const char* line_end = (line_no < LineOffsets.Size) ? buf_begin + LineOffsets[line_no] : NULL
//                    if (Filter.PassFilter(line, line_end))
//                        ImGui::TextUnformatted(line, line_end)
//                    line = line_end && line_end[1] ? line_end + 1 : NULL
//                }
//            }
//            else
            textUnformatted(buf.toString())

            if (scrollToBottom) setScrollHere(1f)
            scrollToBottom = false
            endChild()
            end()
        }
    }
}