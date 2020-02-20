package engine.context

fun TestContext.yield() = engine!!.yeld
void        YieldFrames(int count);
void        YieldUntil(int frame_count);
void        Sleep(float time);
void        SleepNoSkip(float time, float frame_time_step);
void        SleepShort();