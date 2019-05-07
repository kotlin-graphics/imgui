package imgui.imgui.demo.showExampleApp

import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.begin
import imgui.ImGui.beginMenu
import imgui.ImGui.beginMenuBar
import imgui.ImGui.beginPopupContextItem
import imgui.ImGui.beginPopupModal
import imgui.ImGui.beginTabBar
import imgui.ImGui.beginTabItem
import imgui.ImGui.begin_
import imgui.ImGui.button
import imgui.ImGui.checkbox
import imgui.ImGui.closeCurrentPopup
import imgui.ImGui.colorEdit3
import imgui.ImGui.end
import imgui.ImGui.endMenu
import imgui.ImGui.endMenuBar
import imgui.ImGui.endPopup
import imgui.ImGui.endTabBar
import imgui.ImGui.endTabItem
import imgui.ImGui.isPopupOpen
import imgui.ImGui.listBoxFooter
import imgui.ImGui.listBoxHeader
import imgui.ImGui.menuItem
import imgui.ImGui.openPopup
import imgui.ImGui.popId
import imgui.ImGui.popStyleColor
import imgui.ImGui.pushId
import imgui.ImGui.pushItemWidth
import imgui.ImGui.pushStyleColor
import imgui.ImGui.sameLine
import imgui.ImGui.separator
import imgui.ImGui.setTabItemClosed
import imgui.ImGui.text
import imgui.ImGui.textWrapped
import kotlin.reflect.KMutableProperty0

//-----------------------------------------------------------------------------
// [SECTION] Example App: Documents Handling / ShowExampleAppDocuments()
//-----------------------------------------------------------------------------

// Simplified structure to mimic a Document model
class MyDocument(
        /** Document title */
        val name: String,
        /** Set when the document is open (in this demo, we keep an array of all available documents to simplify the demo) */
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

    // Display dummy contents for the Document
    fun displayContents() {
        pushId(this)
        text("Document \"$name\"")
        pushStyleColor(Col.Text, color)
        textWrapped("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.")
        popStyleColor()
        if (button("Modify", Vec2(100, 0)))
            dirty = true
        sameLine()
        if (button("Save", Vec2(100, 0)))
            doSave()
        colorEdit3("color", color)  // Useful to test drag and drop and hold-dragged-to-open-tab behavior.
        popId()
    }

    // Display context menu for the Document
    fun displayContextMenu() {
        if (!beginPopupContextItem())
            return

        if (menuItem("Save $name", "CTRL+S", false, open))
            doSave()
        if (menuItem("Close", "CTRL+W", false, open))
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

    /** [Optional] Notify the system of Tabs/Windows closure that happened outside the regular tab interface.
     *  If a tab has been closed programmatically (aka closed from another source such as the Checkbox() in the demo, as opposed
     *  to clicking on the regular tab closing button) and stops being submitted, it will take a frame for the tab bar to notice its absence.
     *  During this frame there will be a gap in the tab bar, and if the tab that has disappeared was the selected one, the tab bar
     *  will report no selected tab during the frame. This will effectively give the impression of a flicker for one frame.
     *  We call SetTabItemClosed() to manually notify the Tab Bar or Docking system of removed tabs to avoid this glitch.
     *  Note that this completely optional, and only affect tab bars with the ImGuiTabBarFlags_Reorderable flag. */
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

    operator fun invoke(pOpen: KMutableProperty0<Boolean>) {

        if (!begin("Example: Documents", pOpen, WindowFlag.MenuBar.i)) {
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
                if (menuItem("Exit", "Alt+F4")) {
                }
                endMenu()
            }
            endMenuBar()
        }

        // [Debug] List documents with one checkbox for each
        for (docN in documents.indices) {
            val doc = documents[docN]
            if (docN > 0) sameLine()
            pushId(doc)
            if (checkbox(doc.name, doc::open))
                if (!doc.open)
                    doc.doForceClose()
            popId()
        }

        separator()

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
                if (beginPopupModal("Save?")) {
                    text("Save change to the following items?")
                    pushItemWidth(-1f)
                    if (listBoxHeader("##", closeQueueUnsavedDocuments, 6)) {
                        closeQueue.forEach { if (it.dirty) text(it.name) }
                        listBoxFooter()
                    }

                    if (button("Yes", Vec2(80, 0))) {
                        closeQueue.forEach {
                            if (it.dirty)
                                it.doSave()
                            it.doForceClose()
                        }
                        closeQueue.clear()
                        closeCurrentPopup()
                    }
                    sameLine()
                    if (button("No", Vec2(80, 0))) {
                        closeQueue.forEach { it.doForceClose() }
                        closeQueue.clear()
                        closeCurrentPopup()
                    }
                    sameLine()
                    if (button("Cancel", Vec2(80, 0))) {
                        closeQueue.clear()
                        closeCurrentPopup()
                    }
                    endPopup()
                }
            }
        }
    }
}