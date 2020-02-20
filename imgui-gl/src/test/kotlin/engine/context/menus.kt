package engine.context

import engine.core.TestItemInfo
import engine.core.TestRef

fun TestContext.menuAction(action: TestAction, ref: TestRef) {

    if (isError) return

//    IMGUI_TEST_CONTEXT_REGISTER_DEPTH(this)
    logDebug("MenuAction '${ref.path}' %08X", ref.id)

    assert(ref.path != null)

    TODO()
//    int depth = 0
//    const char* path = ref.Path
//    const char* path_end = path + strlen(path)
//    Str128 buf
//    while (path < path_end)
//    {
//        const char* p = ImStrchrRange(path, path_end, '/')
//        if (p == NULL)
//            p = path_end
//        const bool is_target_item = (p == path_end)
//        if (depth == 0)
//        {
//            // Click menu in menu bar
//            IM_ASSERT(RefStr[0] != 0) // Unsupported: window needs to be in Ref
//            buf.setf("##menubar/%.*s", (int)(p - path), path)
//        }
//        else
//        {
//            // Click sub menu in its own window
//            buf.setf("/##Menu_%02d/%.*s", depth - 1, (int)(p - path), path)
//        }
//
//        // We cannot move diagonally to a menu item because depending on the angle and other items we cross on our path we could close our target menu.
//        // First move horizontally into the menu, then vertically!
//        if (depth > 0)
//        {
//            ImGuiTestItemInfo* item = ItemLocate(buf.c_str())
//            IM_CHECK_SILENT(item != NULL)
//            item->RefCount++
//            if (depth > 1 && (Inputs->MousePosValue.x <= item->RectFull.Min.x || Inputs->MousePosValue.x >= item->RectFull.Max.x))
//            MouseMoveToPos(ImVec2(item->RectFull.GetCenter().x, Inputs->MousePosValue.y))
//            if (depth > 0 && (Inputs->MousePosValue.y <= item->RectFull.Min.y || Inputs->MousePosValue.y >= item->RectFull.Max.y))
//            MouseMoveToPos(ImVec2(Inputs->MousePosValue.x, item->RectFull.GetCenter().y))
//            item->RefCount--
//        }
//
//        if (is_target_item)
//        {
//            // Final item
//            ItemAction(action, buf.c_str())
//        }
//        else
//        {
//            // Then aim at the menu item
//            ItemAction(ImGuiTestAction_Click, buf.c_str())
//        }
//
//        path = p + 1
//        depth++
//    }
}

fun TestContext.menuActionAll(action: TestAction, refParent: TestRef) {

    val items = ArrayList<TestItemInfo>()
    menuAction(TestAction.Open, refParent)
    gatherItems(items, focusWindowRef, 1)
    for (item in items) {
        menuAction(TestAction.Open, refParent) // We assume that every interaction will close the menu again
        itemAction(action, item.id)
    }
}

infix fun TestContext.menuClick(ref: TestRef) = menuAction(TestAction.Click, ref)

infix fun TestContext.menuCheck(ref: TestRef) = menuAction(TestAction.Check, ref)

infix fun TestContext.menuUncheck(ref: TestRef) = menuAction(TestAction.Uncheck, ref)

infix fun TestContext.menuCheckAll(refParent: TestRef) = menuActionAll(TestAction.Check, refParent)

infix fun TestContext.menuUncheckAll(refParent: TestRef) = menuActionAll(TestAction.Uncheck, refParent)