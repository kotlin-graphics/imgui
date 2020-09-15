package imgui.impl.glfw

import glm_.and

infix fun Long.wo(long: Long) = and(long.inv())
infix fun Long.wo(int: Int) = and(int.inv())