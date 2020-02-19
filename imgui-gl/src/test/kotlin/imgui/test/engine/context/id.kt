package imgui.test.engine.context

import glm_.f
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.end
import imgui.ImGui.endChild
import imgui.ImGui.endGroup
import imgui.ImGui.endTabBar
import imgui.ImGui.findWindowByID
import imgui.ImGui.focusWindow
import imgui.ImGui.isItemActivated
import imgui.ImGui.isItemActive
import imgui.ImGui.isItemClicked
import imgui.ImGui.isItemDeactivated
import imgui.ImGui.isItemDeactivatedAfterEdit
import imgui.ImGui.isItemEdited
import imgui.ImGui.isItemFocused
import imgui.ImGui.isItemHovered
import imgui.ImGui.isItemVisible
import imgui.ImGui.popID
import imgui.ImGui.treePop
import imgui.classes.Context
import imgui.internal.*
import imgui.internal.classes.Rect
import imgui.internal.classes.Window
import imgui.test.IMGUI_HAS_TABLE
import imgui.test.engine.TestEngine
import imgui.test.engine.core.*
import io.kotlintest.shouldBe
import imgui.WindowFlag as Wf

// [JVM]
fun TestContext.getID(ref: ID): ID = getID(TestRef(ref))

fun TestContext.getID(ref: TestRef): ID = when (ref.id) {
    0 -> hashDecoratedPath(ref.path, refID)
    else -> ref.id
}

// [JVM]
fun TestContext.getID(ref: String, seedRef: TestRef): ID = getID(TestRef(path = ref), seedRef)
// [JVM]
fun TestContext.getID(ref: String, seedRef: ID): ID = getID(TestRef(path = ref), TestRef(seedRef))

fun TestContext.getID(ref: TestRef, seedRef: TestRef): ID = when (ref.id) {
    0 -> hashDecoratedPath(ref.path, getID(seedRef))
    else -> ref.id // FIXME: What if seed_ref != 0
}

fun TestContext.getIDByInt(n: Int): ID = hash(n, getID(refID))

fun TestContext.getIDByInt(n: Int, seedRef: TestRef): ID = hash(n, getID(refID))

//    TODO
//    fun TestContext.getIDByPtr (p: Any): ID
//    ImGuiID GetIDByPtr (void * p, ImGuiTestRef seed_ref)