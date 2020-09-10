/*
 * Copyright 2014-2015 Daniel Collin. All rights reserved.
 * License: https://github.com/bkaradzic/bgfx#license-bsd-2-clause
 */

package imgui.impl.bgfx

import glm_.b
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec4.Vec4ub
import imgui.ImGui
import imgui.ImGui.io
import imgui.ImGui.style
import imgui.ImGui.styleColorsDark
import imgui.ImGui.styleColorsLight
import imgui.classes.Context
import imgui.font.Font
import imgui.font.FontConfig
import imgui.font.glyphRanges
import imgui.internal.DrawData
import kool.set
import org.lwjgl.bgfx.BGFX
import org.lwjgl.bgfx.BGFX.*
import org.lwjgl.bgfx.BGFXReleaseFunctionCallback
import org.lwjgl.bgfx.BGFXVertexLayout
import org.lwjgl.system.APIUtil
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.system.MemoryUtil.nmemFree
import java.io.BufferedInputStream
import java.io.IOException
import java.net.URL
import java.nio.ByteBuffer


//class FontRangeMerge {
//    const void* data
//    size_t      size
//    ImWchar     ranges[3]
//}
//
//static FontRangeMerge s_fontRangeMerge[] =
//{
//    { s_iconsKenneyTtf, sizeof(s_iconsKenneyTtf),      { ICON_MIN_KI, ICON_MAX_KI, 0 } },
//    { s_iconsFontAwesomeTtf, sizeof(s_iconsFontAwesomeTtf), { ICON_MIN_FA, ICON_MAX_FA, 0 } },
//}

enum class FontType { Regular, Mono }

lateinit var ctx: OcornutImguiContext

class OcornutImguiContext (val fontSize: Float) {

    val imguiCtx = Context()

    val layout = BGFXVertexLayout.calloc()
    val program: Short

    val imageProgram: Short

    val texture: Short
    val tex: Short
    val imageLodEnabled: Short

    val font: Array<Font>
    var last = System.currentTimeMillis()
    var lastScroll = 0
    val viewId = 255

    val releaseMemoryCb = BGFXReleaseFunctionCallback.create { ptr, _ -> nmemFree(ptr) }

    init {

        io.displaySize = Vec2i(1280, 720)
        io.deltaTime = 1f / 60f
        io.iniFilename = null

        setupStyle(true)

        /*#if USE_ENTRY
        io.KeyMap[ImGuiKey_Tab] = (int) entry ::Key::Tab
        io.KeyMap[ImGuiKey_LeftArrow] = (int) entry ::Key::Left
        io.KeyMap[ImGuiKey_RightArrow] = (int) entry ::Key::Right
        io.KeyMap[ImGuiKey_UpArrow] = (int) entry ::Key::Up
        io.KeyMap[ImGuiKey_DownArrow] = (int) entry ::Key::Down
        io.KeyMap[ImGuiKey_PageUp] = (int) entry ::Key::PageUp
        io.KeyMap[ImGuiKey_PageDown] = (int) entry ::Key::PageDown
        io.KeyMap[ImGuiKey_Home] = (int) entry ::Key::Home
        io.KeyMap[ImGuiKey_End] = (int) entry ::Key::End
        io.KeyMap[ImGuiKey_Insert] = (int) entry ::Key::Insert
        io.KeyMap[ImGuiKey_Delete] = (int) entry ::Key::Delete
        io.KeyMap[ImGuiKey_Backspace] = (int) entry ::Key::Backspace
        io.KeyMap[ImGuiKey_Space] = (int) entry ::Key::Space
        io.KeyMap[ImGuiKey_Enter] = (int) entry ::Key::Return
        io.KeyMap[ImGuiKey_Escape] = (int) entry ::Key::Esc
        io.KeyMap[ImGuiKey_A] = (int) entry ::Key::KeyA
        io.KeyMap[ImGuiKey_C] = (int) entry ::Key::KeyC
        io.KeyMap[ImGuiKey_V] = (int) entry ::Key::KeyV
        io.KeyMap[ImGuiKey_X] = (int) entry ::Key::KeyX
        io.KeyMap[ImGuiKey_Y] = (int) entry ::Key::KeyY
        io.KeyMap[ImGuiKey_Z] = (int) entry ::Key::KeyZ

        io.ConfigFlags | = 0
        | ImGuiConfigFlags_NavEnableGamepad
        | ImGuiConfigFlags_NavEnableKeyboard

        io.NavInputs[ImGuiNavInput_Activate] = (int) entry ::Key::GamepadA
        io.NavInputs[ImGuiNavInput_Cancel] = (int) entry ::Key::GamepadB
//		io.NavInputs[ImGuiNavInput_Input]       = (int)entry::Key::;
//		io.NavInputs[ImGuiNavInput_Menu]        = (int)entry::Key::;
        io.NavInputs[ImGuiNavInput_DpadLeft] = (int) entry ::Key::GamepadLeft
        io.NavInputs[ImGuiNavInput_DpadRight] = (int) entry ::Key::GamepadRight
        io.NavInputs[ImGuiNavInput_DpadUp] = (int) entry ::Key::GamepadUp
        io.NavInputs[ImGuiNavInput_DpadDown] = (int) entry ::Key::GamepadDown
//		io.NavInputs[ImGuiNavInput_LStickLeft]  = (int)entry::Key::;
//		io.NavInputs[ImGuiNavInput_LStickRight] = (int)entry::Key::;
//		io.NavInputs[ImGuiNavInput_LStickUp]    = (int)entry::Key::;
//		io.NavInputs[ImGuiNavInput_LStickDown]  = (int)entry::Key::;
//		io.NavInputs[ImGuiNavInput_FocusPrev]   = (int)entry::Key::;
//		io.NavInputs[ImGuiNavInput_FocusNext]   = (int)entry::Key::;
//		io.NavInputs[ImGuiNavInput_TweakSlow]   = (int)entry::Key::;
//		io.NavInputs[ImGuiNavInput_TweakFast]   = (int)entry::Key::;
        #endif // USE_ENTRY*/

        val stack = MemoryStack.stackPush()

        val type = bgfx_get_renderer_type()

        fun loadShader(shaders: Map<String, ByteArray>): Short {
            val renderer = when (type) {
                BGFX_RENDERER_TYPE_OPENGL -> "glsl"
                BGFX_RENDERER_TYPE_VULKAN -> "spv"
                BGFX_RENDERER_TYPE_DIRECT3D9 -> "dx9"
                BGFX_RENDERER_TYPE_DIRECT3D11 -> "dx11"
                BGFX_RENDERER_TYPE_METAL -> "mtl"
                else -> error("")
            }
            val shader = shaders[renderer]!!
            val bytes = stack.malloc(shader.size)
            for (i in shader.indices)
                bytes[i] = shader[i]
            return bgfx_create_shader(bgfx_make_ref_release(bytes, releaseMemoryCb, NULL)!!)
        }

        program = bgfx_create_program(loadShader(vs_ocornut_imgui), loadShader(fs_ocornut_imgui), true)

        imageLodEnabled = bgfx_create_uniform("u_imageLodEnabled", BGFX_UNIFORM_TYPE_VEC4, 1)
        imageProgram = bgfx_create_program(loadShader(vs_imgui_image), loadShader(fs_imgui_image), true)

        bgfx_vertex_layout_begin(layout, type)
        bgfx_vertex_layout_add(layout, BGFX_ATTRIB_POSITION, Vec2.length, BGFX_ATTRIB_TYPE_FLOAT, false, false)
        bgfx_vertex_layout_add(layout, BGFX_ATTRIB_TEXCOORD0, Vec2.length, BGFX_ATTRIB_TYPE_FLOAT, false, false)
        bgfx_vertex_layout_add(layout, BGFX_ATTRIB_COLOR0, Vec4ub.length, BGFX_ATTRIB_TYPE_UINT8, false, false)
        bgfx_vertex_layout_end(layout)

        tex = bgfx_create_uniform("s_tex", BGFX_UNIFORM_TYPE_SAMPLER, 1)

//        uint8_t * data
//        int32_t width
//                int32_t height;

        val config = FontConfig().apply {
            fontDataOwnedByAtlas = false
            mergeMode = false
//			    mergeGlyphCenterV = true
        }

        val ranges = glyphRanges.cyrillic
        font = arrayOf(
                io.fonts.addFontFromFileTTF("Roboto-Regular.ttf", fontSize, config, ranges)!!,
                io.fonts.addFontFromFileTTF("RobotoMono-Regular.ttf", fontSize - 3f, config, ranges)!!)

        config.mergeMode = true
        config.dstFont = font[FontType.Regular.ordinal]

//        for (uint32_t ii = 0; ii < BX_COUNTOF(s_fontRangeMerge); ++ii)
//        {
//            const FontRangeMerge & frm = s_fontRangeMerge [ii]
//
//            io.fonts.addFontFromMemoryTTF((void *) frm . data
//                    , (int) frm . size
//                    , _fontSize - 3.0f
//                    , & config
//            , frm.ranges
//            )
//        }

        val (data, size, _) = io.fonts.getTexDataAsRGBA32()

        val textureMemory = bgfx_make_ref_release(data, releaseMemoryCb, NULL)
        texture = bgfx_create_texture_2d(size.x, size.y, false, 1,
                BGFX_TEXTURE_FORMAT_BGRA8, 0, textureMemory)

//        ImGui::InitDockContext()

        stack.pop()
    }

    fun setupStyle(dark: Boolean) {
        // Doug Binks' darl color scheme
        // https://gist.github.com/dougbinks/8089b4bbaccaaf6fa204236978d165a9
        if (dark) styleColorsDark()
        else styleColorsLight()

        style.frameRounding = 4f
        style.windowBorderSize = 0f
    }

    fun destroy()    {
        imguiCtx.destroy()

        bgfx_destroy_uniform(tex)
        bgfx_destroy_texture(texture)

        bgfx_destroy_uniform(imageLodEnabled)
        bgfx_destroy_program(imageProgram)
        bgfx_destroy_program(program)
    }


    /** OcornutImguiContext */
//    val Ctx = object {
//
//        fun render(drawData: DrawData) {
//
//            val (width, height) = io.displaySize
//
//            bgfx_set_view_name(viewId, "ImGui")
//            bgfx_set_view_mode(viewId, BGFX_VIEW_MODE_SEQUENTIAL)
//
//            val caps = BGFX.bgfx_get_caps()
//            run {
//                float ortho [16]
//                bx::mtxOrtho(ortho, 0.0f, width, height, 0.0f, 0.0f, 1000.0f, 0.0f, caps->homogeneousDepth)
//                bgfx::setViewTransform(m_viewId, NULL, ortho)
//                bgfx::setViewRect(m_viewId, 0, 0, uint16_t(width), uint16_t(height))
//            }
//
//            // Render command lists
//            for (int32_t ii = 0, num = _drawData->CmdListsCount; ii < num; ++ii)
//            {
//                bgfx::TransientVertexBuffer tvb
//                        bgfx::TransientIndexBuffer tib
//
//                        const ImDrawList * drawList = _drawData->CmdLists[ii]
//                uint32_t numVertices =(uint32_t) drawList->VtxBuffer.size()
//                uint32_t numIndices =(uint32_t) drawList->IdxBuffer.size()
//
//                if (!checkAvailTransientBuffers(numVertices, m_layout, numIndices)) {
//                    // not enough space in transient buffer just quit drawing the rest...
//                    break
//                }
//
//                bgfx::allocTransientVertexBuffer(& tvb, numVertices, m_layout)
//                bgfx::allocTransientIndexBuffer(& tib, numIndices)
//
//                ImDrawVert * verts = (ImDrawVert *) tvb . data
//                        bx::memCopy(verts, drawList->VtxBuffer.begin(), numVertices * sizeof(ImDrawVert))
//
//                ImDrawIdx * indices = (ImDrawIdx *) tib . data
//                        bx::memCopy(indices, drawList->IdxBuffer.begin(), numIndices * sizeof(ImDrawIdx))
//
//                uint32_t offset = 0
//                for (const ImDrawCmd* cmd = drawList->CmdBuffer.begin(), *cmdEnd = drawList->CmdBuffer.end(); cmd != cmdEnd; ++cmd)
//                {
//                    if (cmd->UserCallback)
//                    { cmd ->
//                        UserCallback(drawList, cmd)
//                    }
//                    else if (0 != cmd->ElemCount)
//                    {
//                        uint64_t state = 0
//                        | BGFX_STATE_WRITE_RGB
//                        | BGFX_STATE_WRITE_A
//                        | BGFX_STATE_MSAA
//
//                        bgfx::TextureHandle th = m_texture
//                                bgfx::ProgramHandle program = m_program
//
//                                if (NULL != cmd->TextureId)
//                        {
//                            union { ImTextureID ptr; struct { bgfx::TextureHandle handle; uint8_t flags; uint8_t mip; } s; } texture = { cmd -> TextureId }
//                            state | = 0 != (IMGUI_FLAGS_ALPHA_BLEND & texture.s.flags)
//                            ? BGFX_STATE_BLEND_FUNC(BGFX_STATE_BLEND_SRC_ALPHA, BGFX_STATE_BLEND_INV_SRC_ALPHA)
//                            : BGFX_STATE_NONE
//                            th = texture.s.handle
//                            if (0 != texture.s.mip) {
//                                const float lodEnabled[4] = { float(texture.s.mip), 1.0f, 0.0f, 0.0f }
//                                bgfx::setUniform(u_imageLodEnabled, lodEnabled)
//                                program = m_imageProgram
//                            }
//                        }
//                        else
//                        {
//                            state | = BGFX_STATE_BLEND_FUNC(BGFX_STATE_BLEND_SRC_ALPHA, BGFX_STATE_BLEND_INV_SRC_ALPHA)
//                        }
//
//                        const uint16_t xx = uint16_t(bx::max(cmd->ClipRect.x, 0.0f))
//                        const uint16_t yy = uint16_t(bx::max(cmd->ClipRect.y, 0.0f))
//                        bgfx::setScissor(xx, yy
//                                , uint16_t(bx::min(cmd->ClipRect.z, 65535.0f)-xx)
//                        , uint16_t(bx::min(cmd->ClipRect.w, 65535.0f)-yy)
//                        )
//
//                        bgfx::setState(state)
//                        bgfx::setTexture(0, s_tex, th)
//                        bgfx::setVertexBuffer(0, & tvb, 0, numVertices)
//                        bgfx::setIndexBuffer(& tib, offset, cmd->ElemCount)
//                        bgfx::submit(m_viewId, program)
//                    }
//
//                    offset += cmd->ElemCount
//                }
//            }
//        }
//    }
//
//    void beginFrame(
//    int32_t _mx
//    , int32_t _my
//    , uint8_t _button
//    , int32_t _scroll
//    , int _width
//    , int _height
//    , int _inputChar
//    , bgfx::ViewId _viewId
//    )
//    {
//        m_viewId = _viewId
//
//        ImGuiIO& io = ImGui::GetIO()
//        if (_inputChar >= 0) {
//            io.AddInputCharacter(_inputChar)
//        }
//
//        io.DisplaySize = ImVec2((float) _width, (float) _height)
//
//        const int64_t now = bx::getHPCounter()
//        const int64_t frameTime = now - m_last
//        m_last = now
//        const double freq = double(bx::getHPFrequency())
//        io.DeltaTime = float(frameTime / freq)
//
//        io.MousePos = ImVec2((float) _mx, (float) _my)
//        io.MouseDown[0] = 0 != (_button & IMGUI_MBUT_LEFT)
//        io.MouseDown[1] = 0 != (_button & IMGUI_MBUT_RIGHT)
//        io.MouseDown[2] = 0 != (_button & IMGUI_MBUT_MIDDLE)
//        io.MouseWheel = (float)(_scroll - m_lastScroll)
//        m_lastScroll = _scroll
//
//        #if USE_ENTRY
//        uint8_t modifiers = inputGetModifiersState ()
//        io.KeyShift = 0 != (modifiers & (entry::Modifier::LeftShift | entry::Modifier::RightShift))
//        io.KeyCtrl = 0 != (modifiers & (entry::Modifier::LeftCtrl  | entry::Modifier::RightCtrl))
//        io.KeyAlt = 0 != (modifiers & (entry::Modifier::LeftAlt   | entry::Modifier::RightAlt))
//        for (int32_t ii = 0; ii < (int32_t) entry ::Key::Count; ++ii)
//        {
//            io.KeysDown[ii] = inputGetKeyState(entry::Key::Enum(ii))
//        }
//        #endif // USE_ENTRY
//
//        ImGui::NewFrame()
//
//        ImGuizmo::BeginFrame()
//    }
//
//    void endFrame()
//    {
//        ImGui::Render()
//        render(ImGui::GetDrawData())
//    }
}

//static OcornutImguiContext s_ctx
//
//static void * memAlloc (size_t _size, void* _userData)
//{
//    BX_UNUSED(_userData)
//    return BX_ALLOC(s_ctx.m_allocator, _size)
//}
//
//static void memFree(void * _ptr, void * _userData)
//{
//    BX_UNUSED(_userData)
//    BX_FREE(s_ctx.m_allocator, _ptr)
//}
//
//void imguiCreate (float _fontSize, bx::AllocatorI* _allocator)
//{
//    s_ctx.create(_fontSize, _allocator)
//}
//
//void imguiDestroy ()
//{
//    s_ctx.destroy()
//}
//
//void imguiBeginFrame (int32_t _mx, int32_t _my, uint8_t _button, int32_t _scroll, uint16_t _width, uint16_t _height, int _inputChar, bgfx::ViewId _viewId)
//{
//    s_ctx.beginFrame(_mx, _my, _button, _scroll, _width, _height, _inputChar, _viewId)
//}
//
//void imguiEndFrame ()
//{
//    s_ctx.endFrame()
//}
//
//namespace ImGui
//{
//    void PushFont (Font::Enum _font)
//    {
//        PushFont(s_ctx.m_font[_font])
//    }
//}