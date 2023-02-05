package imgui.demo.showExampleApp

import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.begin
import imgui.ImGui.beginChildFrame
import imgui.ImGui.beginMenu
import imgui.ImGui.beginMenuBar
import imgui.ImGui.beginPopupContextItem
import imgui.ImGui.beginPopupModal
import imgui.ImGui.beginTabBar
import imgui.ImGui.beginTabItem
import imgui.ImGui.button
import imgui.ImGui.checkbox
import imgui.ImGui.closeCurrentPopup
import imgui.ImGui.colorEdit3
import imgui.ImGui.end
import imgui.ImGui.endChildFrame
import imgui.ImGui.endMenu
import imgui.ImGui.endMenuBar
import imgui.ImGui.endPopup
import imgui.ImGui.endTabBar
import imgui.ImGui.endTabItem
import imgui.ImGui.fontSize
import imgui.ImGui.getID
import imgui.ImGui.isPopupOpen
import imgui.ImGui.menuItem
import imgui.ImGui.openPopup
import imgui.ImGui.popID
import imgui.ImGui.popStyleColor
import imgui.ImGui.pushID
import imgui.ImGui.pushStyleColor
import imgui.ImGui.sameLine
import imgui.ImGui.separator
import imgui.ImGui.setTabItemClosed
import imgui.ImGui.shortcut
import imgui.ImGui.text
import imgui.ImGui.textLineHeightWithSpacing
import imgui.ImGui.textWrapped
import kotlin.reflect.KMutableProperty0

//-----------------------------------------------------------------------------
// [SECTION] Example App: Documents Handling / ShowExampleAppDocuments()
//-----------------------------------------------------------------------------

// Simplified structure to mimic a Document model
class MyDocument(
        /** Document title */
        val name: String,
        /** Set when open (we keep an array of all available documents to simplify demo code!) */
        var open: Boolean = true,
        /** An arbitrary variable associated to the document */
        val color: Vec4 = Vec4(1f)
) {

    /** Copy of Open from last update. */
    var openPrev = open
    /** Set when the document has been modified */
    var dirty = false
    /** Set when the document */
    var wantClose = false

    fun doOpen() {
        open = true
    }

    fun doQueueClose() {
        wantClose = true
    }

    fun doForceClose() {
        open = false
        dirty = false
    }

    fun doSave() {
        dirty = false
    }

    // Display placeholder contents for the Document
    fun displayContents() {
        pushID(this)
        text("Document \"$name\"")
        pushStyleColor(Col.Text, color)
        textWrapped("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.")
        popStyleColor()
        if (button("Modify (Ctrl+M)") || shortcut(Key.Mod_Shortcut or Key.M))
            dirty = true
        sameLine()
        if (button("Save (Ctrl+S)") || shortcut(Key.Mod_Shortcut or Key.S))
            doSave()
        sameLine()
        if (button("Close (Ctrl+W)") || shortcut(Key.Mod_Shortcut or Key.W))
            doQueueClose()
        colorEdit3("color", color)  // Useful to test drag and drop and hold-dragged-to-open-tab behavior.
        popID()
    }

    // Display context menu for the Document
    fun displayContextMenu() {
        if (!beginPopupContextItem())
            return

        if (menuItem("Save $name", "Ctrl+S", false, open))
            doSave()
        if (menuItem("Close", "Ctrl+W", false, open))
            doQueueClose()
        endPopup()
    }
}

object Documents {

    val documents = arrayOf(
            MyDocument("Lettuce", true, Vec4(0.4f, 0.8f, 0.4f, 1.0f)),
            MyDocument("Eggplant", true, Vec4(0.8f, 0.5f, 1.0f, 1.0f)),
            MyDocument("Carrot", true, Vec4(1.0f, 0.8f, 0.5f, 1.0f)),
            MyDocument("Tomato", false, Vec4(1.0f, 0.3f, 0.4f, 1.0f)),
            MyDocument("A Rather Long Title", false),
            MyDocument("Some Document", false)
    )

    // [Optional] Notify the system of Tabs/Windows closure that happened outside the regular tab interface.
    // If a tab has been closed programmatically (aka closed from another source such as the Checkbox() in the demo,
    // as opposed to clicking on the regular tab closing button) and stops being submitted, it will take a frame for
    // the tab bar to notice its absence. During this frame there will be a gap in the tab bar, and if the tab that has
    // disappeared was the selected one, the tab bar will report no selected tab during the frame. This will effectively
    // give the impression of a flicker for one frame.
    // We call SetTabItemClosed() to manually notify the Tab Bar or Docking system of removed tabs to avoid this glitch.
    // Note that this completely optional, and only affect tab bars with the ImGuiTabBarFlags_Reorderable flag.
    fun notifyOfDocumentsClosedElsewhere() {
        for (doc in documents) {
            if (!doc.open && doc.openPrev)
                setTabItemClosed(doc.name)
            doc.openPrev = doc.open
        }
    }

    // Options
    var optReorderable = true
    var optFittingFlags: TabBarFlags = TabBarFlag.FittingPolicyDefault_.i

    val closeQueue = ArrayList<MyDocument>()

    operator fun invoke(pOpen: KMutableProperty0<Boolean>?) {

        val windowContentsVisible = begin("Example: Documents", pOpen, WindowFlag.MenuBar.i)
        if (!windowContentsVisible) {
            end()
            return
        }

        // Menu
        if (beginMenuBar()) {

            if (beginMenu("File")) {

                val openCount = documents.count { it.open }

                if (beginMenu("Open", openCount < documents.size)) {
                    for (doc in documents) {
                        if (!doc.open)
                            if (menuItem(doc.name))
                                doc.doOpen()
                    }
                    endMenu()
                }
                if (menuItem("Close All Documents", "", false, openCount > 0))
                    for (doc in documents)
                        doc.doQueueClose()
                if (menuItem("Exit") && pOpen != null)
                    pOpen.set(false)
                endMenu()
            }
            endMenuBar()
        }

        // [Debug] List documents with one checkbox for each
        for (docN in documents.indices) {
            val doc = documents[docN]
            if (docN > 0) sameLine()
            pushID(doc)
            if (checkbox(doc.name, doc::open))
                if (!doc.open)
                    doc.doForceClose()
            popID()
        }

        separator()

        // About the ImGuiWindowFlags_UnsavedDocument / ImGuiTabItemFlags_UnsavedDocument flags.
        // They have multiple effects:
        // - Display a dot next to the title.
        // - Tab is selected when clicking the X close button.
        // - Closure is not assumed (will wait for user to stop submitting the tab).
        //   Otherwise closure is assumed when pressing the X, so if you keep submitting the tab may reappear at end of tab bar.
        //   We need to assume closure by default otherwise waiting for "lack of submission" on the next frame would leave an empty
        //   hole for one-frame, both in the tab-bar and in tab-contents when closing a tab/window.
        //   The rarely used SetTabItemClosed() function is a way to notify of programmatic closure to avoid the one-frame hole.

        // Submit Tab Bar and Tabs
        run {
            val tabBarFlags: TabBarFlags = optFittingFlags or if (optReorderable) TabBarFlag.Reorderable else TabBarFlag.None
            if (beginTabBar("##tabs", tabBarFlags)) {
                if (optReorderable)
                    notifyOfDocumentsClosedElsewhere()

                // [DEBUG] Stress tests
                //if ((ImGui::GetFrameCount() % 30) == 0) docs[1].Open ^= 1;            // [DEBUG] Automatically show/hide a tab. Test various interactions e.g. dragging with this on.
                //if (ImGui::GetIO().KeyCtrl) ImGui::SetTabItemSelected(docs[1].Name);  // [DEBUG] Test SetTabItemSelected(), probably not very useful as-is anyway..

                // Submit Tabs
                for (doc in documents) {

                    if (!doc.open) continue

                    val tabFlags: TabItemFlags = if (doc.dirty) TabItemFlag.UnsavedDocument.i else TabItemFlag.None.i
                    val visible = beginTabItem(doc.name, doc::open, tabFlags)

                    // Cancel attempt to close when unsaved add to save queue so we can display a popup.
                    if (!doc.open && doc.dirty) {
                        doc.open = true
                        doc.doQueueClose()
                    }

                    doc.displayContextMenu()
                    if (visible) {
                        doc.displayContents()
                        endTabItem()
                    }
                }
                endTabBar()
            }
        }

        // Update closing queue
        if (closeQueue.isEmpty()) {
            // Close queue is locked once we started a popup
            for (doc in documents)
                if (doc.wantClose) {
                    doc.wantClose = false
                    closeQueue += doc
                }
        }

        // Display closing confirmation UI
        if (closeQueue.isNotEmpty()) {
            val closeQueueUnsavedDocuments = closeQueue.count { it.dirty }

            if (closeQueueUnsavedDocuments == 0) {
                // Close documents when all are unsaved
                closeQueue.forEach { it.doForceClose() }
                closeQueue.clear()
            } else {

                if (!isPopupOpen("Save?"))
                    openPopup("Save?")
                if (beginPopupModal("Save?", null, WindowFlag.AlwaysAutoResize.i)) {
                    text("Save change to the following items?")
                    val itemHeight = textLineHeightWithSpacing
                    if (beginChildFrame(getID("frame"), Vec2(-Float.MIN_VALUE, 6.25f * itemHeight))) {
                        closeQueue.forEach { if (it.dirty) text(it.name) }
                        endChildFrame()
                    }

                    val buttonSize = Vec2(fontSize * 7f, 0f)
                    if (button("Yes", buttonSize)) {
                        closeQueue.forEach {
                            if (it.dirty)
                                it.doSave()
                            it.doForceClose()
                        }
                        closeQueue.clear()
                        closeCurrentPopup()
                    }
                    sameLine()
                    if (button("No", buttonSize)) {
                        closeQueue.forEach { it.doForceClose() }
                        closeQueue.clear()
                        closeCurrentPopup()
                    }
                    sameLine()
                    if (button("Cancel", buttonSize)) {
                        closeQueue.clear()
                        closeCurrentPopup()
                    }
                    endPopup()
                }
            }
        }
        end()
    }
}