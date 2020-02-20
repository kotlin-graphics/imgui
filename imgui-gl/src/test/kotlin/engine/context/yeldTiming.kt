package engine.context

fun TestContext.yield() = engine!!.yeld

infix fun TestContext.yieldFrames(count_: Int) {
    var count = count_
    while (count > 0) {
        engine!!.yield()
        count--
    }
}

infix fun TestContext.yieldUntil(frameCount: Int) {
    while (this.frameCount < frameCount)
        engine!!.yield()
}

infix fun TestContext.sleep(time_: Float) {

    if (isError) return

    var time = time_

    if (engineIO!!.configRunFast)
    //ImGuiTestEngine_AddExtraTime(Engine, time); // We could add time, for now we have no use for it...
        engine!!.yield()
    else
        while (time > 0f && !abort) {
            engine!!.yield()
            time -= uiContext!!.io.deltaTime
        }
}

// Sleep for a given clock time from the point of view of the imgui context, without affecting wall clock time of the running application.
fun TestContext.sleepNoSkip(time_: Float, frameTimeStep: Float) {

    if (isError)        return

    var time = time_

    while (time > 0f && !abort)    {
        engine!! setDeltaTime frameTimeStep
        engine!!.yield()
        time -= uiContext!!.io.deltaTime
    }
}

fun TestContext.sleepShort() = sleep(0.3f)