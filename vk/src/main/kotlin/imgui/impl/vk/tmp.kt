package imgui.impl.vk

import kool.Ptr
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import vkk.*
import vkk.entities.*
import vkk.extensions.VkColorSpaceKHR
import vkk.identifiers.CommandBuffer
import vkk.identifiers.Device
import vkk.vk10.structs.*

//  glfw -> T : MemoryStack (for VkStack)
// inline fun loop(block: (MemoryStack) -> Unit) = loop({ isOpen }, block)

// enum for all reports flags