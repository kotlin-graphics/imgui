package imgui.demo

import gli_.has
import glm_.L
import glm_.f
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import glm_.wo
import imgui.*
import imgui.ImGui.alignTextToFramePadding
import imgui.ImGui.begin
import imgui.ImGui.beginChild
import imgui.ImGui.beginMenu
import imgui.ImGui.beginMenuBar
import imgui.ImGui.beginTabBar
import imgui.ImGui.beginTabItem
import imgui.ImGui.bulletText
import imgui.ImGui.button
import imgui.ImGui.checkbox
import imgui.ImGui.checkboxFlags
import imgui.ImGui.closeCurrentPopup
import imgui.ImGui.collapsingHeader
import imgui.ImGui.columns
import imgui.ImGui.combo
import imgui.ImGui.contentRegionAvail
import imgui.ImGui.cursorScreenPos
import imgui.ImGui.cursorStartPos
import imgui.ImGui.dragFloat
import imgui.ImGui.dragInt
import imgui.ImGui.dragVec2
import imgui.ImGui.dummy
import imgui.ImGui.end
import imgui.ImGui.endChild
import imgui.ImGui.endMenu
import imgui.ImGui.endMenuBar
import imgui.ImGui.endTabBar
import imgui.ImGui.endTabItem
import imgui.ImGui.getColumnWidth
import imgui.ImGui.getID
import imgui.ImGui.invisibleButton
import imgui.ImGui.io
import imgui.ImGui.isItemActive
import imgui.ImGui.isItemHovered
import imgui.ImGui.itemRectMax
import imgui.ImGui.itemRectSize
import imgui.ImGui.listBox
import imgui.ImGui.listBoxFooter
import imgui.ImGui.listBoxHeader
import imgui.ImGui.nextColumn
import imgui.ImGui.openPopup
import imgui.ImGui.plotHistogram
import imgui.ImGui.popID
import imgui.ImGui.popItemWidth
import imgui.ImGui.popStyleColor
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushID
import imgui.ImGui.pushItemWidth
import imgui.ImGui.pushStyleColor
import imgui.ImGui.pushStyleVar
import imgui.ImGui.sameLine
import imgui.ImGui.scrollMaxX
import imgui.ImGui.scrollMaxY
import imgui.ImGui.scrollX
import imgui.ImGui.scrollY
import imgui.ImGui.selectable
import imgui.ImGui.separator
import imgui.ImGui.setNextItemWidth
import imgui.ImGui.setNextWindowContentSize
import imgui.ImGui.setScrollFromPosX
import imgui.ImGui.setScrollFromPosY
import imgui.ImGui.setScrollHereX
import imgui.ImGui.setScrollHereY
import imgui.ImGui.setTabItemClosed
import imgui.ImGui.setTooltip
import imgui.ImGui.sliderFloat
import imgui.ImGui.sliderInt
import imgui.ImGui.smallButton
import imgui.ImGui.spacing
import imgui.ImGui.style
import imgui.ImGui.tabItemButton
import imgui.ImGui.text
import imgui.ImGui.textColored
import imgui.ImGui.textLineHeight
import imgui.ImGui.textUnformatted
import imgui.ImGui.textWrapped
import imgui.ImGui.treeNode
import imgui.ImGui.treePop
import imgui.ImGui.windowContentRegionMax
import imgui.ImGui.windowContentRegionWidth
import imgui.ImGui.windowDrawList
import imgui.ImGui.windowPos
import imgui.ImGui.windowWidth
import imgui.api.demoDebugInformations.Companion.helpMarker
import imgui.classes.Color
import imgui.demo.showExampleApp.MenuFile
import imgui.dsl.child
import imgui.dsl.group
import imgui.dsl.indent
import imgui.dsl.menuBar
import imgui.dsl.popup
import imgui.dsl.popupContextItem
import imgui.dsl.tabBar
import imgui.dsl.treeNode
import imgui.dsl.withClipRect
import imgui.dsl.withId
import imgui.dsl.withItemWidth
import imgui.dsl.withStyleColor
import imgui.dsl.withStyleVar
import kotlin.math.sin
import imgui.WindowFlag as Wf

object ShowDemoWindowLayout {

    /* Child regions */
    var disableMouseWheel = false
    var disableMenu = false
    var offsetX = 0


    /* Widgets Width */
    var f = 0f


    /* Basic Horizontal Layout */
    var c1 = false
    var c2 = false
    var c3 = false
    var c4 = false
    var f0 = 1f
    var f1 = 2f
    var f2 = 3f
    var item = -1
    val selection = intArrayOf(0, 1, 2, 3)


    /* Tabs */
    var tabBarFlags0: TabBarFlags = TabBarFlag.Reorderable.i
    val names0 = arrayOf("Artichoke", "Beetroot", "Celery", "Daikon")
    val opened = BooleanArray(4) { true } // Persistent user state

    /* TabItem Button */
    var nextTabId = 0
    val activeTabs = ArrayList<Int>()
    var tabBarFlags1: TabBarFlags = TabBarFlag.AutoSelectNewTabs or TabBarFlag.Reorderable or TabBarFlag.FittingPolicyResizeDown
    var showLeadingButton = true
    var showTrailingButton = true
    var enablePosition = false


    /** Text Baseline Alignment */
    var spacing = style.itemInnerSpacing.x


    /* Scrolling */
    var enableTrack = true
    var enableExtraDecorations = false
    var trackItem = 50
    val names2 = arrayOf("Left", "25%%", "Center", "75%%", "Right")
    var scrollToOffPx = 0f
    var scrollToPosPx = 200f


    /* Horizontal Scrolling */
    var lines = 7
    var showHorizontalContentsSizeDemoWindow = false
    var showHscrollbar = true
    var showButton = true
    var showTreeNodes = true
    var showTextWrapped = false
    var open = true
    var showColumns = true
    var showTabBar = true
    var showChild = false
    var explicitContentSize = false
    var contentsSizeX = 300f


    /* Clipping */
    val size = Vec2(100f)
    val offset = Vec2(30)

    operator fun invoke() {

        if (!collapsingHeader("Layout & Scrolling"))
            return

        treeNode("Child Windows") {

            helpMarker("Use child windows to begin into a self-contained independent scrolling/clipping regions within a host window.")
            checkbox("Disable Mouse Wheel", ::disableMouseWheel)
            checkbox("Disable Menu", ::disableMenu)

            // Child 1: no border, enable horizontal scrollbar
            run {
                var windowFlags = Wf.HorizontalScrollbar.i
                if (disableMouseWheel)
                    windowFlags = windowFlags or Wf.NoScrollWithMouse
                child("ChildL", Vec2(windowContentRegionWidth * 0.5f, 260), false, windowFlags) {
                    for (i in 0..99)
                        text("%04d: scrollable region", i)
                }
            }
            sameLine()

            // Child 2: rounded border
            run {
                var windowFlags = Wf.None.i
                if (disableMouseWheel)
                    windowFlags = windowFlags or Wf.NoScrollWithMouse
                if (!disableMenu)
                    windowFlags = windowFlags or Wf.MenuBar
                withStyleVar(StyleVar.ChildRounding, 5f) {
                    child("ChildR", Vec2(0, 260), true, windowFlags) {
                        if (!disableMenu && beginMenuBar()) {
                            if (beginMenu("Menu")) {
                                MenuFile()
                                endMenu()
                            }
                            endMenuBar()
                        }
                        columns(2)
                        for (i in 0..99) {
                            val text = "%03d".format(style.locale, i)
                            button(text, Vec2(-Float.MIN_VALUE, 0f))
                            nextColumn()
                        }
                    }
                }
            }

            separator()

            // Demonstrate a few extra things
            // - Changing ImGuiCol_ChildBg (which is transparent black in default styles)
            // - Using SetCursorPos() to position child window (the child window is an item from the POV of parent window)
            //   You can also call SetNextWindowPos() to position the child window. The parent window will effectively
            //   layout from this position.
            // - Using ImGui::GetItemRectMin/Max() to query the "item" state (because the child window is an item from
            //   the POV of the parent window). See 'Demo->Querying Status (Active/Focused/Hovered etc.)' for details.
            run {
                setNextItemWidth(100f)
                dragInt("Offset X", ::offsetX, 1f, -1000, 1000)

                ImGui.cursorPosX += offsetX
                withStyleColor(Col.ChildBg, COL32(255, 0, 0, 100)) {
                    beginChild("Red", Vec2(200, 100), true, Wf.None.i)
                    for (n in 0..49)
                        text("Some test $n")
                    endChild()
                }
                val childIsHovered = ImGui.isItemHovered()
                val childRectMin = ImGui.itemRectMin
                val childRectMax = ImGui.itemRectMax
                text("Hovered: ${childIsHovered.i}")
                text("Rect of child window is: (%.0f,%.0f) (%.0f,%.0f)", childRectMin.x, childRectMin.y, childRectMax.x, childRectMax.y)
            }
        }

        treeNode("Widgets Width") {

            // Use SetNextItemWidth() to set the width of a single upcoming item.
            // Use PushItemWidth()/PopItemWidth() to set the width of a group of items.
            // In real code use you'll probably want to choose width values that are proportional to your font size
            // e.g. Using '20.0f * GetFontSize()' as width instead of '200.0f', etc.
            text("SetNextItemWidth/PushItemWidth(100)")
            sameLine(); helpMarker("Fixed width.")
            setNextItemWidth(100f)
            dragFloat("float##1", ::f)

            text("SetNextItemWidth/PushItemWidth(GetWindowWidth() * 0.5f)")
            sameLine(); helpMarker("Half of window width.")
            setNextItemWidth(windowWidth * 0.5f)
            dragFloat("float##2", ::f)

            text("SetNextItemWidth/PushItemWidth(GetContentRegionAvail().x * 0.5f)")
            sameLine(); helpMarker("Half of available width.\n(~ right-cursor_pos)\n(works within a column set)")
            setNextItemWidth(contentRegionAvail.x * 0.5f)
            dragFloat("float##3", ::f)

            text("SetNextItemWidth/PushItemWidth(-100)")
            sameLine(); helpMarker("Align to right edge minus 100")
            setNextItemWidth(-100f)
            dragFloat("float##4", ::f)

            // Demonstrate using PushItemWidth to surround three items.
            // Calling SetNextItemWidth() before each of them would have the same effect.
            text("SetNextItemWidth/PushItemWidth(-1)")
            sameLine(); helpMarker("Align to right edge")
            pushItemWidth(-1)

            dragFloat("##float5a", ::f)
            dragFloat("##float5b", ::f)
            dragFloat("##float5c", ::f)
            popItemWidth()
        }

        treeNode("Basic Horizontal SimpleLayout") {

            textWrapped("(Use SameLine() to keep adding items to the right of the preceding item)")

            // Text
            text("Two items: Hello"); sameLine()
            textColored(Vec4(1, 1, 0, 1), "Sailor")

            // Adjust spacing
            text("More spacing: Hello"); sameLine(0, 20)
            textColored(Vec4(1, 1, 0, 1), "Sailor")

            // Button
            alignTextToFramePadding()
            text("Normal buttons"); sameLine()
            button("Banana"); sameLine()
            button("Apple"); sameLine()
            button("Corniflower")

            // Button
            text("Small buttons"); sameLine()
            smallButton("Like this one"); sameLine()
            text("can fit within a text block.")

            // Aligned to arbitrary position. Easy/cheap column.
            text("Aligned")
            sameLine(150); text("x=150")
            sameLine(300); text("x=300")
            text("Aligned")
            sameLine(150); smallButton("x=150")
            sameLine(300); smallButton("x=300")

            // Checkbox
            checkbox("My", ::c1); sameLine()
            checkbox("Tailor", ::c2); sameLine()
            checkbox("Is", ::c3); sameLine()
            checkbox("Rich", ::c4)

            // Various
            val items = arrayOf("AAAA", "BBBB", "CCCC", "DDDD")
            withItemWidth(80f) {
                combo("Combo", ::item, items); sameLine()
                sliderFloat("X", ::f0, 0f, 5f); sameLine()
                sliderFloat("Y", ::f1, 0f, 5f); sameLine()
                sliderFloat("Z", ::f2, 0f, 5f)
            }

            withItemWidth(80f) {
                text("Lists:")
                for (i in 0..3) {
                    if (i > 0) sameLine()
                    withId(i) {
                        withInt(selection, i) {
                            listBox("", it, items)
                        }
                    }
                    //if (IsItemHovered()) SetTooltip("ListBox %d hovered", i);
                }
            }

            // Dummy
            val buttonSz = Vec2(40)
            button("A", buttonSz); sameLine()
            dummy(buttonSz); sameLine()
            button("B", buttonSz)

            // Manually wrapping
            // (we should eventually provide this as an automatic layout feature, but for now you can do it manually)
            text("Manually wrapping:")
            val buttonsCount = 20
            val windowVisibleX2 = windowPos.x + windowContentRegionMax.x
            for (n in 0 until buttonsCount) {
                pushID(n)
                button("Box", buttonSz)
                val lastButtonX2 = itemRectMax.x
                val nextButtonX2 = lastButtonX2 + style.itemSpacing.x + buttonSz.x // Expected position if next button was on same line
                if (n + 1 < buttonsCount && nextButtonX2 < windowVisibleX2)
                    sameLine()
                popID()
            }
        }

        treeNode("Tabs") {

            treeNode("Basic") {
                val tabBarFlags: TabBarFlags = TabBarFlag.None.i
                if (beginTabBar("MyTabBar", tabBarFlags)) {
                    if (beginTabItem("Avocado")) {
                        text("This is the Avocado tab!\nblah blah blah blah blah")
                        endTabItem()
                    }
                    if (beginTabItem("Broccoli")) {
                        text("This is the Broccoli tab!\nblah blah blah blah blah")
                        endTabItem()
                    }
                    if (beginTabItem("Cucumber")) {
                        text("This is the Cucumber tab!\nblah blah blah blah blah")
                        endTabItem()
                    }
                    endTabBar()
                }
                separator()
            }

            treeNode("Advanced & Close Button") {
                // Expose a couple of the available flags. In most cases you may just call BeginTabBar() with no flags (0).
                checkboxFlags("ImGuiTabBarFlags_Reorderable", ::tabBarFlags0, TabBarFlag.Reorderable.i)
                checkboxFlags("ImGuiTabBarFlags_AutoSelectNewTabs", ::tabBarFlags0, TabBarFlag.AutoSelectNewTabs.i)
                checkboxFlags("ImGuiTabBarFlags_TabListPopupButton", ::tabBarFlags0, TabBarFlag.TabListPopupButton.i)
                checkboxFlags("ImGuiTabBarFlags_NoCloseWithMiddleMouseButton", ::tabBarFlags0, TabBarFlag.NoCloseWithMiddleMouseButton.i)
                if (tabBarFlags0 hasnt TabBarFlag.FittingPolicyMask_)
                    tabBarFlags0 = tabBarFlags0 or TabBarFlag.FittingPolicyDefault_
                if (checkboxFlags("ImGuiTabBarFlags_FittingPolicyResizeDown", ::tabBarFlags0, TabBarFlag.FittingPolicyResizeDown.i))
                    tabBarFlags0 = tabBarFlags0 wo (TabBarFlag.FittingPolicyMask_ xor TabBarFlag.FittingPolicyResizeDown)
                if (checkboxFlags("ImGuiTabBarFlags_FittingPolicyScroll", ::tabBarFlags0, TabBarFlag.FittingPolicyScroll.i))
                    tabBarFlags0 = tabBarFlags0 wo (TabBarFlag.FittingPolicyMask_ xor TabBarFlag.FittingPolicyScroll)

                // Tab Bar
                for (n in opened.indices) {
                    if (n > 0) sameLine()
                    checkbox(names0[n], opened, n)
                }

                // Passing a bool* to BeginTabItem() is similar to passing one to Begin():
                // the underlying bool will be set to false when the tab is closed.
                if (beginTabBar("MyTabBar", tabBarFlags0)) {
                    for (n in opened.indices)
                        if (opened[n] && beginTabItem(names0[n], opened, n, TabItemFlag.None.i)) {
                            text("This is the ${names0[n]} tab!")
                            if (n has 1)
                                text("I am an odd tab.")
                            endTabItem()
                        }
                    endTabBar()
                }
                separator()
            }

            treeNode("TabItemButton & Leading/Trailing flags") {

                if (nextTabId == 0) // Initialize with some default tabs
                    for (i in 0..2)
                        activeTabs += nextTabId++

                // TabItemButton() and Leading/Trailing flags are distinct features which we will demo together.
                // (It is possible to submit regular tabs with Leading/Trailing flags, or TabItemButton tabs without Leading/Trailing flags...
                // but they tend to make more sense together)
                checkbox("Show Leading TabItemButton()", ::showLeadingButton)
                checkbox("Show Trailing TabItemButton()", ::showTrailingButton)

                // Expose some other flags which are useful to showcase how they interact with Leading/Trailing tabs
                checkboxFlags("ImGuiTabBarFlags_TabListPopupButton", ::tabBarFlags1, TabBarFlag.TabListPopupButton.i)
                if (checkboxFlags("ImGuiTabBarFlags_FittingPolicyResizeDown", ::tabBarFlags1, TabBarFlag.FittingPolicyResizeDown.i))
                    tabBarFlags1 = tabBarFlags1 wo (TabBarFlag.FittingPolicyMask_ xor TabBarFlag.FittingPolicyResizeDown)
                if (checkboxFlags("ImGuiTabBarFlags_FittingPolicyScroll", ::tabBarFlags1, TabBarFlag.FittingPolicyScroll.i))
                    tabBarFlags1 = tabBarFlags1 wo (TabBarFlag.FittingPolicyMask_ xor TabBarFlag.FittingPolicyScroll.i)

                tabBar("MyTabBar", tabBarFlags1) {
                    // Demo a Leading TabItemButton(): click the "?" button to open a menu
                    if (showLeadingButton)
                        if(tabItemButton("?", TabItemFlag.Leading or TabItemFlag.NoTooltip))
                            openPopup("MyHelpMenu")
                    popup("MyHelpMenu") {
                        selectable("Hello!")
                    }

                    // Demo Trailing Tabs: click the "+" button to add a new tab (in your app you may want to use a font icon instead of the "+")
                    // Note that we submit it before the regular tabs, but because of the ImGuiTabItemFlags_Trailing flag it will always appear at the end.
                    if (showTrailingButton)
                        if(tabItemButton("+", TabItemFlag.Trailing or TabItemFlag.NoTooltip))
                            activeTabs += nextTabId++ // Add new tab

                    // Submit our regular tabs
                    var n = 0
                    while (n < activeTabs.size) {

                        var open = true
                        _b = open
                        val name = "%04d".format(activeTabs[n])
                        if (beginTabItem(name, ::_b, TabItemFlag.None.i)) {
                            open = _b
                            text("This is the $name tab!")
                            endTabItem()
                        }

                        if (!open)
                            activeTabs.removeAt(n)
                        else
                           n++
                    }
                }
                separator()
            }
        }

        treeNode("Groups") {

            helpMarker(
                    "BeginGroup() basically locks the horizontal position for new line. " +
                            "EndGroup() bundles the whole group so that you can use \"item\" functions such as " +
                            "IsItemHovered()/IsItemActive() or SameLine() etc. on the whole group.")
            group {
                group {
                    button("AAA")
                    sameLine()
                    button("BBB")
                    sameLine()
                    group {
                        button("CCC")
                        button("DDD")
                    }
                    sameLine()
                    button("EEE")
                }
                if (isItemHovered()) setTooltip("First group hovered")

                // Capture the group size and create widgets using the same size
                val size = Vec2(itemRectSize)
                val values = floatArrayOf(0.5f, 0.2f, 0.8f, 0.6f, 0.25f)
                plotHistogram("##values", values, 0, "", 0f, 1f, size)

                button("ACTION", Vec2((size.x - style.itemSpacing.x) * 0.5f, size.y))
                sameLine()
                button("REACTION", Vec2((size.x - style.itemSpacing.x) * 0.5f, size.y))
            }
            sameLine()

            button("LEVERAGE\nBUZZWORD", size)
            sameLine()

            if (listBoxHeader("List", size)) {
                selectable("Selected", true)
                selectable("Not Selected", false)
                listBoxFooter()
            }
        }

        treeNode("Text Baseline Alignment") {

            run {
                bulletText("Text baseline:")
                sameLine(); helpMarker(
                    "This is testing the vertical alignment that gets applied on text to keep it aligned with widgets. " +
                            "Lines only composed of text or \"small\" widgets use less vertical space than lines with framed widgets.")
                indent {

                    text("KO Blahblah"); sameLine()
                    button("Some framed item"); sameLine()
                    helpMarker("Baseline of button will look misaligned with text..")

                    // If your line starts with text, call AlignTextToFramePadding() to align text to upcoming widgets.
                    // (because we don't know what's coming after the Text() statement, we need to move the text baseline
                    // down by FramePadding.y ahead of time)
                    alignTextToFramePadding()
                    text("OK Blahblah"); sameLine()
                    button("Some framed item"); sameLine()
                    helpMarker("We call AlignTextToFramePadding() to vertically align the text baseline by +FramePadding.y")

                    // SmallButton() uses the same vertical padding as Text
                    button("TEST##1"); sameLine()
                    text("TEST"); sameLine()
                    smallButton("TEST##2")

                    // If your line starts with text, call AlignTextToFramePadding() to align text to upcoming widgets.
                    alignTextToFramePadding()
                    text("Text aligned to framed item"); sameLine()
                    button("Item##1"); sameLine()
                    text("Item"); sameLine()
                    smallButton("Item##2"); sameLine()
                    button("Item##3")
                }
            }

            spacing()

            run {
                bulletText("Multi-line text:")
                indent {
                    text("One\nTwo\nThree"); sameLine()
                    text("Hello\nWorld"); sameLine()
                    text("Banana")

                    text("Banana"); sameLine()
                    text("Hello\nWorld"); sameLine()
                    text("One\nTwo\nThree")

                    button("HOP##1"); sameLine()
                    text("Banana"); sameLine()
                    text("Hello\nWorld"); sameLine()
                    text("Banana")

                    button("HOP##2"); sameLine()
                    text("Hello\nWorld"); sameLine()
                    text("Banana")
                }
            }

            spacing()

            run {
                bulletText("Misc items:")
                indent {

                    // SmallButton() sets FramePadding to zero. Text baseline is aligned to match baseline of previous Button.
                    button("80x80", Vec2(80))
                    sameLine()
                    button("50x50", Vec2(50))
                    sameLine()
                    button("Button()")
                    sameLine()
                    smallButton("SmallButton()")

                    // Tree
                    button("Button##1")
                    sameLine(0f, spacing)
                    treeNode("Node##1") {
                        // Placeholder tree data
                        for (i in 0..5)
                            bulletText("Item $i..")
                    }

                    // Vertically align text node a bit lower so it'll be vertically centered with upcoming widget.
                    // Otherwise you can use SmallButton() (smaller fit).
                    alignTextToFramePadding()

                    // Common mistake to avoid: if we want to SameLine after TreeNode we need to do it before we add
                    // other contents below the node.
                    val nodeOpen = treeNode("Node##2")
                    sameLine(0f, spacing); button("Button##2")
                    if (nodeOpen) {
                        // Placeholder tree data
                        for (i in 0..5)
                            bulletText("Item $i..")
                        treePop()
                    }

                    // Bullet
                    button("Button##3")
                    sameLine(0f, spacing)
                    bulletText("Bullet text")

                    alignTextToFramePadding()
                    bulletText("Node")
                    sameLine(0f, spacing); button("Button##4")
                }
            }
        }

        treeNode("Scrolling") {

            // Vertical scroll functions
            helpMarker("Use SetScrollHereY() or SetScrollFromPosY() to scroll to a given vertical position.")

            checkbox("Decoration", ::enableExtraDecorations)

            checkbox("Track", ::enableTrack)
            pushItemWidth(100)
            sameLine(140); enableTrack = dragInt("##item", ::trackItem, 0.25f, 0, 99, "Item = %d") or enableTrack

            var scrollToOff = button("Scroll Offset")
            sameLine(140); scrollToOff = dragFloat("##off", ::scrollToOffPx, 1f, 0f, Float.MAX_VALUE, "+%.0f px") or scrollToOff

            var scrollToPos = button("Scroll To Pos")
            sameLine(140); scrollToPos = dragFloat("##pos", ::scrollToPosPx, 1f, -10f, Float.MAX_VALUE, "X/Y = %.0f px") or scrollToPos

            popItemWidth()
            if (scrollToOff || scrollToPos)
                enableTrack = false

            var childW = (contentRegionAvail.x - 4 * style.itemSpacing.x) / 5
            if (childW < 1f)
                childW = 1f
            pushID("##VerticalScrolling")
            for (i in 0..4) {
                if (i > 0) sameLine()
                group {
                    val names = arrayOf("Top", "25%%", "Center", "75%%", "Bottom") // double quote for ::format escaping
                    textUnformatted(names[i])

                    val childFlags = if (enableExtraDecorations) Wf.MenuBar else Wf.None
                    val childId = getID(i.L)
                    val childIsVisible = beginChild(childId, Vec2(childW, 200f), true, childFlags.i)
                    menuBar { textUnformatted("abc") }
                    if (scrollToOff)
                        scrollY = scrollToOffPx
                    if (scrollToPos)
                        setScrollFromPosY(cursorStartPos.y + scrollToPosPx, i * 0.25f)
                    // Avoid calling SetScrollHereY when running with culled items
                    if (childIsVisible)
                        for (item in 0..99)
                            if (enableTrack && item == trackItem) {
                                textColored(Vec4(1, 1, 0, 1), "Item %d", item)
                                setScrollHereY(i * 0.25f) // 0.0f:top, 0.5f:center, 1.0f:bottom
                            } else
                                text("Item $item")
                    val scrollY = scrollY
                    val scrollMaxY = scrollMaxY
                    endChild()
                    text("%.0f/%.0f", scrollY, scrollMaxY)
                }
            }
            popID()

            // Horizontal scroll functions
            spacing()
            helpMarker(
                    "Use SetScrollHereX() or SetScrollFromPosX() to scroll to a given horizontal position.\n\n" +
                            "Because the clipping rectangle of most window hides half worth of WindowPadding on the " +
                            "left/right, using SetScrollFromPosX(+1) will usually result in clipped text whereas the " +
                            "equivalent SetScrollFromPosY(+1) wouldn't.")
            pushID("##HorizontalScrolling")
            for (i in 0..4) {
                val childHeight = textLineHeight + style.scrollbarSize + style.windowPadding.y * 2f
                val childFlags = Wf.HorizontalScrollbar or if (enableExtraDecorations) Wf.AlwaysVerticalScrollbar else Wf.None
                val childId = getID(i.L)
                val childIsVisible = beginChild(childId, Vec2(-100f, childHeight), true, childFlags)
                if (scrollToOff)
                    scrollX = scrollToOffPx
                if (scrollToPos)
                    setScrollFromPosX(cursorStartPos.x + scrollToPosPx, i * 0.25f)
                if (childIsVisible) // Avoid calling SetScrollHereY when running with culled items
                    for (item in 0..99) {
                        if (enableTrack && item == trackItem) {
                            textColored(Vec4(1, 1, 0, 1), "Item $item")
                            setScrollHereX(i * 0.25f) // 0.0f:left, 0.5f:center, 1.0f:right
                        } else
                            text("Item $item")
                        sameLine()
                    }
                endChild()
                sameLine()
                text("${names2[i]}\n%.0f/%.0f", scrollX, scrollMaxX)
                spacing()
            }
            popID()

            // Miscellaneous Horizontal Scrolling Demo

            helpMarker(
                    "Horizontal scrolling for a window is enabled via the ImGuiWindowFlags_HorizontalScrollbar flag.\n\n" +
                            "You may want to also explicitly specify content width by using SetNextWindowContentWidth() before Begin().")
            sliderInt("Lines", ::lines, 1, 15)
            pushStyleVar(StyleVar.FrameRounding, 3f)
            pushStyleVar(StyleVar.FramePadding, Vec2(2f, 1f))
            val scrollingChildSize = Vec2(0f, ImGui.frameHeightWithSpacing * 7 + 30)
            beginChild("scrolling", scrollingChildSize, true, Wf.HorizontalScrollbar.i)
            for (line in 0 until lines) {
                // Display random stuff. For the sake of this trivial demo we are using basic Button() + SameLine()
                // If you want to create your own time line for a real application you may be better off manipulating
                // the cursor position yourself, aka using SetCursorPos/SetCursorScreenPos to position the widgets
                // yourself. You may also want to use the lower-level ImDrawList API.
                val numButtons = 10 + (line * if (line has 1) 9 else 3)
                for (n in 0 until numButtons) {
                    if (n > 0) sameLine()
                    pushID(n + line * 1000)
                    val label = if (n % 15 == 0) "FizzBuzz" else if (n % 3 == 0) "Fizz" else if (n % 5 == 0) "Buzz" else "$n"
                    val hue = n * 0.05f
                    pushStyleColor(Col.Button, Color.hsv(hue, 0.6f, 0.6f))
                    pushStyleColor(Col.ButtonHovered, Color.hsv(hue, 0.7f, 0.7f))
                    pushStyleColor(Col.ButtonActive, Color.hsv(hue, 0.8f, 0.8f))
                    button(label, Vec2(40f + sin((line + n).f) * 20f, 0f))
                    popStyleColor(3)
                    popID()
                }
            }
            val _scrollX = scrollX
            val scrollMaxX = scrollMaxX
            endChild()
            popStyleVar(2)
            var scrollXDelta = 0f
            smallButton("<<")
            if (isItemActive)
                scrollXDelta = -io.deltaTime * 1000f
            sameLine()
            text("Scroll from code"); sameLine()
            smallButton(">>")
            if (isItemActive)
                scrollXDelta = io.deltaTime * 1000f
            sameLine()
            text("%.0f/%.0f", _scrollX, scrollMaxX)
            if (scrollXDelta != 0f) {
                // Demonstrate a trick: you can use Begin to set yourself in the context of another window
                // (here we are already out of your child window)
                beginChild("scrolling")
                scrollX += scrollXDelta
                endChild()
            }
            spacing()

            checkbox("Show Horizontal contents size demo window", ::showHorizontalContentsSizeDemoWindow)

            if (showHorizontalContentsSizeDemoWindow) {
                if (explicitContentSize)
                    setNextWindowContentSize(Vec2(contentsSizeX, 0f))
                begin("Horizontal contents size demo window", ::showHorizontalContentsSizeDemoWindow, if (showHscrollbar) Wf.HorizontalScrollbar.i else Wf.None.i)
                pushStyleVar(StyleVar.ItemSpacing, Vec2(2, 0))
                pushStyleVar(StyleVar.FramePadding, Vec2(2, 0))
                helpMarker("Test of different widgets react and impact the work rectangle growing when horizontal scrolling is enabled.\n\nUse 'Metrics->Tools->Show windows rectangles' to visualize rectangles.")
                checkbox("H-scrollbar", ::showHscrollbar)
                checkbox("Button", ::showButton)            // Will grow contents size (unless explicitly overwritten)
                checkbox("Tree nodes", ::showTreeNodes)    // Will grow contents size and display highlight over full width
                checkbox("Text wrapped", ::showTextWrapped)     // Will grow and use contents size
                checkbox("Columns", ::showColumns)          // Will use contents size
                checkbox("Tab bar", ::showTabBar)          // Will use contents size
                checkbox("Child", ::showChild)              // Will grow and use contents size
                checkbox("Explicit content size", ::explicitContentSize)
                text("Scroll %.1f/%.1f %.1f/%.1f", scrollX, scrollMaxX, scrollY, scrollMaxY)
                if (explicitContentSize) {
                    sameLine()
                    setNextItemWidth(100f)
                    dragFloat("##csx", ::contentsSizeX)
                    val p = cursorScreenPos
                    windowDrawList.addRectFilled(p, Vec2(p.x + 10, p.y + 10), COL32_WHITE)
                    windowDrawList.addRectFilled(Vec2(p.x + contentsSizeX - 10, p.y), Vec2(p.x + contentsSizeX, p.y + 10), COL32_WHITE)
                    dummy(Vec2(0, 10))
                }
                popStyleVar(2)
                separator()
                if (showButton)
                    button("this is a 300-wide button", Vec2(300, 0))
                if (showTreeNodes) {
                    open = true
                    treeNode("this is a tree node") {
                        treeNode("another one of those tree node...") {
                            text("Some tree contents")
                        }
                    }
                    collapsingHeader("CollapsingHeader", ::open)
                }
                if (showTextWrapped)
                    textWrapped("This text should automatically wrap on the edge of the work rectangle.")
                if (showColumns) {
                    columns(4)
                    for (n in 0..3) {
                        text("Width %.2f", getColumnWidth())
                        nextColumn()
                    }
                    columns(1)
                }
                if (showTabBar && beginTabBar("Hello")) {
                    if (beginTabItem("OneOneOne"))
                        endTabItem()
                    if (beginTabItem("TwoTwoTwo"))
                        endTabItem()
                    if (beginTabItem("ThreeThreeThree"))
                        endTabItem()
                    if (beginTabItem("FourFourFour"))
                        endTabItem()
                    endTabBar()
                }
                if (showChild) {
                    beginChild("child", Vec2(), true)
                    endChild()
                }
                end()
            }
        }

        treeNode("Clipping") {
            dragVec2("size", size, 0.5f, 1f, 200f, "%.0f")
            textWrapped("(Click and drag to scroll)")

            for (n in 0..2) {
                if (n > 0)
                    sameLine()
                pushID(n)
                group { // Lock X position

                    invisibleButton("##empty", size)
                    if (ImGui.isItemActive && ImGui.isMouseDragging(MouseButton.Left))
                        offset += io.mouseDelta
                    val p0 = Vec2(ImGui.itemRectMin)
                    val p1 = Vec2(ImGui.itemRectMax)
                    val textStr = "Line 1 hello\nLine 2 clip me!"
                    val textPos = p0 + offset
                    val drawList = ImGui.windowDrawList

                    when (n) {
                        0 -> {
                            helpMarker("""
                                Using ImGui::PushClipRect():
                                Will alter ImGui hit-testing logic + ImDrawList rendering.
                                (use this if you want your clipping rectangle to affect interactions)""".trimIndent())
                            withClipRect(p0, p1, true) {
                                drawList.addRectFilled(p0, p1, COL32(90, 90, 120, 255))
                                drawList.addText(textPos, COL32_WHITE, textStr)
                            }
                        }
                        1 -> {
                            helpMarker("""
                                Using ImDrawList::PushClipRect():
                                Will alter ImDrawList rendering only.
                                (use this as a shortcut if you are only using ImDrawList calls)""".trimIndent())
                            drawList.withClipRect(p0, p1, true) {
                                addRectFilled(p0, p1, COL32(90, 90, 120, 255))
                                addText(textPos, COL32_WHITE, textStr)
                            }
                        }
                        2 -> {
                            helpMarker("""
                                Using ImDrawList::AddText() with a fine ClipRect:
                                Will alter only this specific ImDrawList::AddText() rendering.
                                (this is often used internally to avoid altering the clipping rectangle and minimize draw calls)""".trimIndent())
                            val clipRect = Vec4(p0, p1) // AddText() takes a ImVec4* here so let's convert.
                            drawList.addRectFilled(p0, p1, COL32(90, 90, 120, 255))
                            drawList.addText(ImGui.font, ImGui.fontSize, textPos, COL32_WHITE, textStr, 0f, clipRect)
                        }
                    }
                }
                popID()
            }
        }
    }
}