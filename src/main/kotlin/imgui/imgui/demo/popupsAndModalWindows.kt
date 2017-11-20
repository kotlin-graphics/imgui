package imgui.imgui.demo

import glm_.glm
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.button
import imgui.ImGui.checkbox
import imgui.ImGui.closeCurrentPopup
import imgui.ImGui.dragFloat
import imgui.ImGui.inputText
import imgui.ImGui.isItemHovered
import imgui.ImGui.menuItem
import imgui.ImGui.openPopup
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.separator
import imgui.ImGui.setTooltip
import imgui.ImGui.text
import imgui.ImGui.textWrapped
import imgui.functionalProgramming.button
import imgui.functionalProgramming.collapsingHeader
import imgui.functionalProgramming.menu
import imgui.functionalProgramming.popup
import imgui.functionalProgramming.popupContextItem
import imgui.functionalProgramming.popupModal
import imgui.functionalProgramming.treeNode
import imgui.functionalProgramming.withId
import imgui.functionalProgramming.withItemWidth
import imgui.functionalProgramming.withStyleVar
import imgui.imgui.imgui_demoDebugInfo.Companion.showExampleMenuFile
import kotlin.reflect.KMutableProperty0
import imgui.ColorEditFlags as Cef
import imgui.Context as g
import imgui.InputTextFlags as Itf
import imgui.SelectableFlags as Sf
import imgui.TreeNodeFlags as Tnf
import imgui.WindowFlags as Wf

object popupsAndModalWindows {

    /* Popus */
    var selectedFish = -1
    val toggles = booleanArrayOf(true, false, false, false, false)

    /* Context Menu */
    var value = 0.5f
    var name = "Label1"

    var dontAskMeNextTime = false

    operator fun invoke() {

        collapsingHeader("Popups & Modal windows") {

            treeNode("Popups") {

                textWrapped("When a popup is active, it inhibits interacting with windows that are behind the popup. Clicking outside the popup closes it.")

                val names = arrayOf("Bream", "Haddock", "Mackerel", "Pollock", "Tilefish")

                /*  Simple selection popup
                    (If you want to show the current selection inside the Button itself, you may want to build a string
                    using the "###" operator to preserve a constant ID with a variable label)                 */
                if (button("Select..")) openPopup("select")
                sameLine()
                text(names.getOrElse(selectedFish, { "<None>" }))
                popup("select") {
                    text("Aquarium")
                    separator()
                    names.forEachIndexed { i, n -> if (selectable(n)) selectedFish = i }
                }

                // Showing a menu with toggles
                if (button("Toggle..")) openPopup("toggle")
                popup("toggle") {
                    names.forEachIndexed { i, n -> withBool(toggles, i) { b -> menuItem(n, "", b) } }

                    menu("Sub-menu") { menuItem("Click me") }

                    separator()
                    text("Tooltip here")
                    if (isItemHovered()) setTooltip("I am a tooltip over a popup")

                    if (button("Stacked Popup")) openPopup("another popup")
                    popup("another popup") {
                        names.forEachIndexed { i, n -> withBool(toggles, i) { b -> menuItem(n, "", b) } }
                        menu("Sub-menu") { menuItem("Click me") }
                    }
                }

                if (button("Popup Menu..")) openPopup("FilePopup")
                popup("FilePopup") { showExampleMenuFile() }
            }

            treeNode("Context menus") {

                // BeginPopupContextItem() is a helper to provide common/simple popup behavior of essentially doing:
                //    if (IsItemHovered() && IsMouseClicked(0))
                //       OpenPopup(id);
                //    return BeginPopup(id);
                // For more advanced uses you may want to replicate and cuztomize this code. This the comments inside BeginPopupContextItem() implementation.
                text("Value = %.3f (<-- right-click here)", value)
                popupContextItem("item context menu") {
                    if (selectable("Set to zero")) value = 0f
                    if (selectable("Set to PI")) value = glm.PIf
                    withItemWidth(-1) {
                        dragFloat("##Value", ::value, 0.1f, 0f, 0f)
                    }
                }

                val text = "Button: $name###Button" // ### operator override id ignoring the preceding label
                button(text)
                // When used after an item that has an ID (here the Button), we can skip providing an ID to BeginPopupContextItem().
                popupContextItem {
                    text("Edit name")
                    inputText("##edit", name.toCharArray())
                    if (button("Close")) closeCurrentPopup()
                }
                sameLine(); text("(<-- right-click here)")
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

                    withStyleVar(StyleVar.FramePadding, Vec2()) { checkbox("Don't ask me next time", ::dontAskMeNextTime) }

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

            treeNode("Menus inside a regular window") {
                textWrapped("Below we are testing adding menu items to a regular window. It's rather unusual but should work!")
                separator()
                /*  NB: As a quirk in this very specific example, we want to differentiate the parent of this menu from
                    the parent of the various popup menus above.
                    To do so we are encloding the items in a pushID()/popID() block to make them two different menusets.
                    If we don't, opening any popup above and hovering our menu here would open it. This is because once
                    a menu is active, we allow to switch to a sibling menu by just hovering on it, which is the desired
                    behavior for regular menus. */
                withId("foo") {
                    menuItem("Menu item", "CTRL+M")
                    menu("Menu inside a regular window") { showExampleMenuFile() }
                }
                separator()
            }
        }
    }
}


private inline fun <R> withBool(bools: BooleanArray, index: Int, block: (KMutableProperty0<Boolean>) -> R): R {
    Ref.bPtr++
    val b = Ref::bool
    b.set(bools[index])
    val res = block(b)
    bools[index] = b()
    Ref.bPtr--
    return res
}