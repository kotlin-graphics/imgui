package engine.context

import engine.KeyModFlag
import engine.core.*
import imgui.ID
import imgui.Key
import imgui.MouseButton
import imgui.internal.InputSource
import imgui.internal.has
import imgui.internal.hasnt
import imgui.internal.ItemStatusFlag as Isf

// [JVM]
fun TestContext.itemAction(action: TestAction, ref: String, actionArg: Int? = null) = itemAction(action, TestRef(path = ref), actionArg)

// [JVM]
fun TestContext.itemAction(action: TestAction, ref: ID, actionArg: Int? = null) = itemAction(action, TestRef(ref), actionArg)

fun TestContext.itemAction(action_: TestAction, ref: TestRef, actionArg: Int? = null) {

    var action = action_
    if (isError) return

    REGISTER_DEPTH {

        // [DEBUG] Breakpoint
        //if (ref.ID == 0x0d4af068)
        //    printf("");

        val item = itemLocate(ref) ?: return
        val desc = TestRefDesc(ref, item)

        logDebug("Item${action.name} $desc${if (inputMode == InputSource.Mouse) "" else " (w/ Nav)"}")

        // Automatically uncollapse by default
        item.window?.let {
            if (opFlags hasnt TestOpFlag.NoAutoUncollapse)
                windowAutoUncollapse(it)
        }

        if (action == TestAction.Click || action == TestAction.DoubleClick)
            if (inputMode == InputSource.Mouse) {
                val mouseButton = actionArg ?: 0
                assert(mouseButton >= 0 && mouseButton < MouseButton.COUNT)
                mouseMove(ref)
                if (!engineIO!!.configRunFast)
                    sleep(0.05f)
                if (action == TestAction.DoubleClick)
                    mouseDoubleClick(mouseButton)
                else
                    mouseClick(mouseButton)
                return
            } else action = TestAction.NavActivate

        if (action == TestAction.NavActivate) {
            assert(actionArg == null) // Unused
            navMoveTo(ref)
            navActivate()
            if (action == TestAction.DoubleClick)
                assert(false)
            return
        }

        if (action == TestAction.Input) {
            assert(actionArg == null) // Unused
            if (inputMode == InputSource.Mouse) {
                mouseMove(ref)
                keyDownMap(Key.Count, KeyModFlag.Ctrl.i)
                mouseClick(0)
                keyUpMap(Key.Count, KeyModFlag.Ctrl.i)
            } else {
                navMoveTo(ref)
                navInput()
            }
            return
        }

        if (action == TestAction.Open) {
            assert(actionArg == null) // Unused
            if (item.statusFlags hasnt Isf.Opened) {
                item.refCount++
                mouseMove(ref)

                // Some item may open just by hovering, give them that chance
                if (item.statusFlags hasnt Isf.Opened) {
                    itemClick(ref)
                    if (item.statusFlags hasnt Isf.Opened) {
                        itemDoubleClick(ref) // Attempt a double-click // FIXME-TESTS: let's not start doing those fuzzy things..
                        if (item.statusFlags hasnt Isf.Opened)
                            ERRORF_NOHDR("Unable to Open item: ${TestRefDesc(ref, item)}")
                    }
                }
                item.refCount--
                yield()
            }
            return
        }

        if (action == TestAction.Close) {
            assert(actionArg == null) // Unused
            if (item.statusFlags has Isf.Opened) {
                item.refCount++
                itemClick(ref)
                if (item.statusFlags has Isf.Opened) {
                    itemDoubleClick(ref) // Attempt a double-click // FIXME-TESTS: let's not start doing those fuzzy things..
                    if (item.statusFlags has Isf.Opened)
                        ERRORF_NOHDR("Unable to Close item: ${TestRefDesc(ref, item)}")
                }
                item.refCount--
                yield()
            }
            return
        }

        if (action == TestAction.Check) {
            assert(actionArg == null) // Unused
            if (item.statusFlags has Isf.Checkable && item.statusFlags hasnt Isf.Checked) {
                itemClick(ref)
                yield()
            }
            itemVerifyCheckedIfAlive(ref, true) // We can't just IM_ASSERT(ItemIsChecked()) because the item may disappear and never update its StatusFlags any more!
            return
        }

        if (action == TestAction.Uncheck) {
            assert(actionArg == null) // Unused
            if (item.statusFlags has Isf.Checkable && item.statusFlags has Isf.Checked) {
                itemClick(ref)
                yield()
            }
            itemVerifyCheckedIfAlive(ref, false) // We can't just IM_ASSERT(ItemIsChecked()) because the item may disappear and never update its StatusFlags any more!
            return
        }
        assert(false)
    }
}

// [JVM]
fun TestContext.itemClick(ref: String, button: Int = 0) = itemAction(TestAction.Click, TestRef(path = ref), button)

// [JVM]
fun TestContext.itemClick(ref: ID, button: Int = 0) = itemAction(TestAction.Click, TestRef(ref), button)
fun TestContext.itemClick(ref: TestRef, button: Int = 0) = itemAction(TestAction.Click, ref, button)

// [JVM]
infix fun TestContext.itemDoubleClick(ref: String) = itemAction(TestAction.DoubleClick, TestRef(path = ref))

infix fun TestContext.itemDoubleClick(ref: TestRef) = itemAction(TestAction.DoubleClick, ref)

infix fun TestContext.itemCheck(ref: TestRef) = itemAction(TestAction.Check, ref)

infix fun TestContext.itemUncheck(ref: TestRef) = itemAction(TestAction.Uncheck, ref)

// [JVM]
infix fun TestContext.itemOpen(ref: String) = itemAction(TestAction.Open, TestRef(path = ref))
infix fun TestContext.itemOpen(ref: TestRef) = itemAction(TestAction.Open, ref)

// [JVM]
infix fun TestContext.itemClose(ref: ID) = itemAction(TestAction.Close, TestRef(ref))
infix fun TestContext.itemClose(ref: TestRef) = itemAction(TestAction.Close, ref)

// [JVM]
infix fun TestContext.itemInput(ref: String) = itemAction(TestAction.Input, TestRef(path = ref))

infix fun TestContext.itemInput(ref: TestRef) = itemAction(TestAction.Input, ref)

infix fun TestContext.itemNavActivate(ref: TestRef) = itemAction(TestAction.NavActivate, ref)

fun TestContext.itemActionAll(action: TestAction, refParent: TestRef, maxDepth: Int = 99, maxPasses: Int = 99) {

    assert(maxDepth > 0 && maxPasses > 0)

    var actionedTotal = 0
    for (pass in 0 until maxPasses) {

        val items = TestItemList()
        gatherItems(items, refParent, maxDepth)

        // Find deep most items
        val highestDepth = when (action) {
            TestAction.Close -> items.list.filter { it.statusFlags has Isf.Openable && it.statusFlags has Isf.Opened }
                    .map { it.depth }.max() ?: -1
            else -> -1
        }

        val actionedTotalAtBeginningOfPass = actionedTotal

        // Process top-to-bottom in most cases
        var scanStart = 0
        var scanEnd = items.size
        var scanDir = +1
        if (action == TestAction.Close) {
            // Close bottom-to-top because
            // 1) it is more likely to handle same-depth parent/child relationship better (e.g. CollapsingHeader)
            // 2) it gives a nicer sense of symmetry with the corresponding open operation.
            scanStart = items.lastIndex
            scanEnd = -1
            scanDir = -1
        }

        var n = scanStart
        while (n != scanEnd) {
            if (isError) break

            val info = items[n]
            when (action) {
                TestAction.Click -> {
                    itemAction(action, info.id)
                    actionedTotal++
                }
                TestAction.Check ->
                    if (info.statusFlags has Isf.Checkable && info.statusFlags hasnt Isf.Checked) {
                        itemAction(action, info.id)
                        actionedTotal++
                    }
                TestAction.Uncheck ->
                    if (info.statusFlags has Isf.Checkable && info.statusFlags has Isf.Checked) {
                        itemAction(action, info.id)
                        actionedTotal++
                    }
                TestAction.Open ->
                    if (info.statusFlags has Isf.Openable && info.statusFlags hasnt Isf.Opened) {
                        itemAction(action, info.id)
                        actionedTotal++
                    }
                TestAction.Close ->
                    if (info.depth == highestDepth && info.statusFlags has Isf.Openable && info.statusFlags has Isf.Opened) {
                        itemClose(info.id)
                        actionedTotal++
                    }
                else -> assert(false)
            }
            n += scanDir
        }

        if (isError) break

        if (actionedTotalAtBeginningOfPass == actionedTotal) break
    }
    logDebug("$action $actionedTotal items in total!")
}

fun TestContext.itemOpenAll(refParent: TestRef, depth: Int = 99, passes: Int = 99) = itemActionAll(TestAction.Open, refParent, depth, passes)

fun TestContext.itemCloseAll(refParent: TestRef, depth: Int = 99, passes: Int = 99) = itemActionAll(TestAction.Close, refParent, depth, passes)

fun TestContext.itemHold(ref: TestRef, time: Float) {

    if (isError) return

    REGISTER_DEPTH {
        logDebug("ItemHold '${ref.path}' %08X", ref.id)

        mouseMove(ref)

        yield()
        inputs!!.mouseButtonsValue = 1 shl 0
        sleep(time)
        inputs!!.mouseButtonsValue = 0
        yield()
    }
}

fun TestContext.itemHoldForFrames(ref: TestRef, frames: Int) {

    if (isError) return

    REGISTER_DEPTH {
        logDebug("ItemHoldForFrames '${ref.path}' %08X", ref.id)

        mouseMove(ref)
        yield()
        inputs!!.mouseButtonsValue = 1 shl 0
        yieldFrames(frames)
        inputs!!.mouseButtonsValue = 0
        yield()
    }
}

fun TestContext.itemDragAndDrop(refSrc: TestRef, refDst: TestRef) {

    if (isError) return

    REGISTER_DEPTH {
        val itemSrc = itemLocate(refSrc)
        val itemDst = itemLocate(refDst)
        val descSrc = TestRefDesc(refSrc, itemSrc)
        val descDst = TestRefDesc(refDst, itemDst)
        logDebug("ItemDragAndDrop $descSrc to $descDst")

        mouseMove(refSrc, TestOpFlag.NoCheckHoveredId.i)
        sleepShort()
        mouseDown(0)

        // Enforce lifting drag threshold even if both item are exactly at the same location.
        mouseLiftDragThreshold()

        mouseMove(refDst, TestOpFlag.NoCheckHoveredId.i)
        sleepShort()
        mouseUp(0)
    }
}

fun TestContext.itemVerifyCheckedIfAlive(ref: TestRef, checked: Boolean) {

    yield()
    itemLocate(ref, TestOpFlag.NoError.i)?.let {
        if (it.timestampMain + 1 >= engine!!.frameCount &&
                it.timestampStatus == it.timestampMain &&
                it.statusFlags has Isf.Checked != checked)
            CHECK((it.statusFlags has Isf.Checked) == checked)
    }
}