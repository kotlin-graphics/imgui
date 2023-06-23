package imgui.impl.gl

import glm_.*
import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec4.Vec4ub
import gln.*
import gln.glf.semantic
import gln.identifiers.GlBuffers
import gln.identifiers.GlProgram
import gln.identifiers.GlShader
import gln.identifiers.GlVertexArray
import gln.uniform.glUniform
import gln.vertexArray.glVertexAttribPointer
import imgui.*
import imgui.ImGui.io
import imgui.L
import imgui.internal.DrawData
import imgui.internal.DrawIdx
import imgui.internal.DrawVert
import kool.*
import org.lwjgl.opengl.GL30C.*
import org.lwjgl.opengl.GL31C.GL_PRIMITIVE_RESTART
import org.lwjgl.opengl.GL32C.*
import org.lwjgl.opengl.GL33C
import org.lwjgl.opengl.GL33C.glBindSampler
import org.lwjgl.opengl.GL45C.GL_CLIP_ORIGIN
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.system.Platform


class ImplGL3 : GLInterface {

    /** ~ImGui_ImplOpenGL3_Init */
    init {
        assert(io.backendRendererUserData == null) { "Already initialized a renderer backend!" }

        // Setup backend capabilities flags
        io.backendRendererUserData = data
        io.backendRendererName = "imgui_impl_opengl3"

        // Query for GL version (e.g. 320 for GL 3.2)
        var major = glGetInteger(GL_MAJOR_VERSION)
        var minor = glGetInteger(GL_MINOR_VERSION)
        if (major == 0 && minor == 0)
        // Query GL_VERSION in desktop GL 2.x, the string will start with "<major>.<minor>"
            glGetString(GL_VERSION)?.let { glVersion ->
                major = glVersion.substringBefore('.').i
                minor = glVersion.substringAfter('.').i
            }
        data.glVersion = when {
            !OPENGL_ES2 -> major * 100 + minor * 10
            else -> 200 // GLES 2
        }

        if (IMGUI_IMPL_OPENGL_DEBUG)
            print("GL_MAJOR_VERSION = $major\nGL_MINOR_VERSION = $minor\nGL_VENDOR = '${glGetString(GL_VENDOR)}'\nGL_RENDERER = '${glGetString(GL_RENDERER)}'") // [DEBUG]

        // Detect extensions we support
        data.hasClipOrigin = data.glVersion >= 450
        if (OPENGL_MAY_HAVE_EXTENSIONS) {
            val numExtensions = glGetInteger(GL_NUM_EXTENSIONS)
            for (i in 0 until numExtensions) {
                val extension = glGetStringi(GL_EXTENSIONS, i)
                if (extension == "GL_ARB_clip_control")
                    data.hasClipOrigin = true
            }
        }

        if (OPENGL_MAY_HAVE_VTX_OFFSET)
            if (data.glVersion >= 320)
                io.backendFlags = io.backendFlags or BackendFlag.RendererHasVtxOffset  // We can honor the ImDrawCmd::VtxOffset field, allowing for large meshes.
    }

    override fun shutdown() {
        destroyDeviceObjects()
        io.backendRendererName = null
        io.backendRendererUserData = null
        io.backendFlags -= BackendFlag.RendererHasVtxOffset
    }

    override fun newFrame() {
        if (data.shaderHandle.isInvalid)
            createDeviceObjects()
    }

    fun setupRenderState(drawData: DrawData, fbWidth: Int, fbHeight: Int) {

        // Setup render state: alpha-blending enabled, no face culling, no depth testing, scissor enabled
        glEnable(GL_BLEND)
        glBlendEquation(GL_FUNC_ADD)
        glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA)
        glDisable(GL_CULL_FACE)
        glDisable(GL_DEPTH_TEST)
        glDisable(GL_STENCIL_TEST)
        glEnable(GL_SCISSOR_TEST)
        if (OPENGL_MAY_HAVE_PRIMITIVE_RESTART && data.glVersion >= 310)
            glDisable(GL_PRIMITIVE_RESTART)
        if (IMPL_HAS_POLYGON_MODE)
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)

        // Support for GL 4.5 rarely used glClipControl(GL_UPPER_LEFT)
        val clipOriginLowerLeft = when {
            CLIP_ORIGIN && data.hasClipOrigin -> glGetInteger(GL_CLIP_ORIGIN) != GL_UPPER_LEFT
            else -> true
        }

        // Setup viewport, orthographic projection matrix
        // Our visible imgui space lies from draw_data->DisplayPos (top left) to draw_data->DisplayPos+data_data->DisplaySize (bottom right).
        // DisplayPos is (0,0) for single viewport apps.
        glViewport(0, 0, fbWidth, fbHeight); glCall("glViewport")
        val L = drawData.displayPos.x
        val R = drawData.displayPos.x + drawData.displaySize.x
        var T = drawData.displayPos.y
        var B = drawData.displayPos.y + drawData.displaySize.y
        if (CLIP_ORIGIN)
            if (!clipOriginLowerLeft) {
                val tmp = T; T = B; B = tmp; } // Swap top and bottom if origin is upper left
        val orthoProjection = glm.ortho(L, R, B, T, mat)
        glUseProgram(data.shaderHandle.name)
        glUniform(data.attribLocationProjMtx, orthoProjection)
        if (OPENGL_MAY_HAVE_BIND_SAMPLER)
            if (data.glVersion > 330/* || bd->GlProfileIsES3*/)
                glBindSampler(0, 0) // We use combined texture/sampler state. Applications using GL 3.3 and GL ES 3.0 may set that otherwise.

        data.vao.bind()

        // Bind vertex/index buffers and setup attributes for ImDrawVert
        glBindBuffer(GL_ARRAY_BUFFER, data.buffers[Buffer.Vertex].name); glCall("glBindBuffer")
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, data.buffers[Buffer.Element].name); glCall("glBindBuffer")
        glEnableVertexAttribArray(semantic.attr.POSITION); glCall("glEnableVertexAttribArray")
        glEnableVertexAttribArray(semantic.attr.TEX_COORD); glCall("glEnableVertexAttribArray")
        glEnableVertexAttribArray(semantic.attr.COLOR); glCall("glEnableVertexAttribArray")
        glVertexAttribPointer(semantic.attr.POSITION, Vec2.length, GL_FLOAT, false, DrawVert.SIZE, 0); glCall("glVertexAttribPointer")
        glVertexAttribPointer(semantic.attr.TEX_COORD, Vec2.length, GL_FLOAT, false, DrawVert.SIZE, Vec2.size); glCall("glVertexAttribPointer")
        glVertexAttribPointer(semantic.attr.COLOR, Vec4ub.length, GL_UNSIGNED_BYTE, true, DrawVert.SIZE, Vec2.size * 2); glCall("glVertexAttribPointer")
    }

    /** OpenGL3 Render function.
     *  Note that this implementation is little overcomplicated because we are saving/setting up/restoring every OpenGL state explicitly.
     *  This is in order to be able to run within an OpenGL engine that doesn't do so.   */
    override fun renderDrawData(drawData: DrawData) {

        // Avoid rendering when minimized, scale coordinates for retina displays (screen coordinates != framebuffer coordinates)
        val fbWidth = (drawData.displaySize.x * drawData.framebufferScale.x).i
        val fbHeight = (drawData.displaySize.y * drawData.framebufferScale.y).i
        if (fbWidth == 0 || fbHeight == 0) return

        // Backup GL state
        val lastActiveTexture = glGetInteger(GL_ACTIVE_TEXTURE)
        glActiveTexture(GL_TEXTURE0 + semantic.sampler.DIFFUSE)
        val lastProgram = glGetInteger(GL_CURRENT_PROGRAM)
        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D)
        val lastSampler = when {
            !OPENGL_MAY_HAVE_BIND_SAMPLER && data.glVersion > 330 /* || bd->GlProfileIsES3*/ -> glGetInteger(GL33C.GL_SAMPLER_BINDING)
            else -> 0
        }
        val lastArrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING)
        val lastVertexArray = glGetInteger(GL_VERTEX_ARRAY_BINDING)
        val lastElementBuffer = glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING)
        val lastPolygonMode = if (IMPL_HAS_POLYGON_MODE) glGetVec2i(GL_POLYGON_MODE) else Vec2i(-1)
        val lastViewport = glGetVec4i(GL_VIEWPORT)
        val lastScissorBox = glGetVec4i(GL_SCISSOR_BOX)
        val lastBlendSrcRgb = glGetInteger(GL_BLEND_SRC_RGB)
        val lastBlendDstRgb = glGetInteger(GL_BLEND_DST_RGB)
        val lastBlendSrcAlpha = glGetInteger(GL_BLEND_SRC_ALPHA)
        val lastBlendDstAlpha = glGetInteger(GL_BLEND_DST_ALPHA)
        val lastBlendEquationRgb = glGetInteger(GL_BLEND_EQUATION_RGB)
        val lastBlendEquationAlpha = glGetInteger(GL_BLEND_EQUATION_ALPHA)
        val lastEnableBlend = glIsEnabled(GL_BLEND)
        val lastEnableCullFace = glIsEnabled(GL_CULL_FACE)
        val lastEnableDepthTest = glIsEnabled(GL_DEPTH_TEST)
        val lastEnableStencilTest = glIsEnabled(GL_STENCIL_TEST)
        val lastEnableScissorTest = glIsEnabled(GL_SCISSOR_TEST)
        val lastEnablePrimitiveRestart = OPENGL_MAY_HAVE_PRIMITIVE_RESTART && data.glVersion >= 310 && glIsEnabled(GL_PRIMITIVE_RESTART)

        // Setup desired GL state
        setupRenderState(drawData, fbWidth, fbHeight)

        // Will project scissor/clipping rectangles into framebuffer space
        val clipOff = drawData.displayPos     // (0,0) unless using multi-viewports
        val clipScale = drawData.framebufferScale   // (1,1) unless using retina display which are often (2,2)

        // Render command lists
        for (cmdList in drawData.cmdLists) {

            // Upload vertex/index buffers
            // - OpenGL drivers are in a very sorry state nowadays....
            //   During 2021 we attempted to switch from glBufferData() to orphaning+glBufferSubData() following reports
            //   of leaks on Intel GPU when using multi-viewports on Windows.
            // - After this we kept hearing of various display corruptions issues. We started disabling on non-Intel GPU, but issues still got reported on Intel.
            // - We are now back to using exclusively glBufferData(). So bd->UseBufferSubData IS ALWAYS FALSE in this code.
            //   We are keeping the old code path for a while in case people finding new issues may want to test the bd->UseBufferSubData path.
            // - See https://github.com/ocornut/imgui/issues/4468 and please report any corruption issues.
            val vtxBufferSize = cmdList.vtxBuffer.lim.L
            val idxBufferSize = cmdList.idxBuffer.lim.L * DrawIdx.BYTES

            if (data.useBufferSubData) {
                if (data.vertexBufferSize < vtxBufferSize) {
                    data.vertexBufferSize = vtxBufferSize
                    nglBufferData(GL_ARRAY_BUFFER, data.vertexBufferSize, NULL, GL_STREAM_DRAW); glCall("glBufferData")
                }
                if (data.indexBufferSize < idxBufferSize) {
                    data.indexBufferSize = idxBufferSize
                    nglBufferData(GL_ELEMENT_ARRAY_BUFFER, data.indexBufferSize, NULL, GL_STREAM_DRAW); glCall("glBufferData")
                }
                nglBufferSubData(GL_ARRAY_BUFFER, 0, vtxBufferSize, cmdList.vtxBuffer.adr.L); glCall("glBufferSubData")
                nglBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, idxBufferSize, cmdList.idxBuffer.adr.L); glCall("glBufferSubData")
            } else {
                nglBufferData(GL_ARRAY_BUFFER, cmdList.vtxBuffer.data.lim.L, cmdList.vtxBuffer.data.adr.L, GL_STREAM_DRAW); glCall("glBufferData")
                nglBufferData(GL_ELEMENT_ARRAY_BUFFER, cmdList.idxBuffer.lim * DrawIdx.BYTES.L, cmdList.idxBuffer.adr.L, GL_STREAM_DRAW); glCall("glBufferData")
            }

            for (cmd in cmdList.cmdBuffer) {

                val userCB = cmd.userCallback
                if (userCB != null) {
                    // User callback, registered via ImDrawList::AddCallback()
                    // (ImDrawCallback_ResetRenderState is a special callback value used by the user to request the renderer to reset render state.)
                    if (cmd.resetRenderState)
                        setupRenderState(drawData, fbWidth, fbHeight)
                    else
                        userCB(cmdList, cmd)
                } else {
                    // Project scissor/clipping rectangles into framebuffer space
                    val clipMin = Vec2((cmd.clipRect.x - clipOff.x) * clipScale.x, (cmd.clipRect.y - clipOff.y) * clipScale.y)
                    val clipMax = Vec2((cmd.clipRect.z - clipOff.x) * clipScale.x, (cmd.clipRect.w - clipOff.y) * clipScale.y)
                    if (clipMax.x <= clipMin.x || clipMax.y <= clipMin.y)
                        continue

                    // Apply scissor/clipping rectangle (Y is inverted in OpenGL)
                    glScissor(clipMin.x.i, (fbHeight.f - clipMax.y).i, (clipMax.x - clipMin.x).i, (clipMax.y - clipMin.y).i); glCall("glScissor")

                    // Bind texture, Draw
                    glBindTexture(GL_TEXTURE_2D, cmd.texID!!)
                    if (OPENGL_MAY_HAVE_VTX_OFFSET && data.glVersion >= 320) {
                        glDrawElementsBaseVertex(GL_TRIANGLES, cmd.elemCount, GL_UNSIGNED_INT, cmd.idxOffset.L * DrawIdx.BYTES, cmd.vtxOffset); glCall("glDrawElementsBaseVertex")
                    } else {
                        glDrawElements(GL_TRIANGLES, cmd.elemCount, GL_UNSIGNED_INT, cmd.idxOffset.L * DrawIdx.BYTES); glCall("glDrawElements")
                    }
                }
            }
        }

        // Restore modified GL state
        if (lastProgram == 0 || glIsProgram(lastProgram)) glUseProgram(lastProgram)
        glBindTexture(GL_TEXTURE_2D, lastTexture)
        if (OPENGL_MAY_HAVE_BIND_SAMPLER)
            if (data.glVersion > 330 /*|| bd->GlProfileIsES3*/)
                glBindSampler(0, lastSampler)
        glActiveTexture(lastActiveTexture)
        glBindVertexArray(lastVertexArray)
        glBindBuffer(GL_ARRAY_BUFFER, lastArrayBuffer)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, lastElementBuffer)
        glBlendEquationSeparate(lastBlendEquationRgb, lastBlendEquationAlpha)
        glBlendFuncSeparate(lastBlendSrcRgb, lastBlendDstRgb, lastBlendSrcAlpha, lastBlendDstAlpha)
        if (lastEnableBlend) glEnable(GL_BLEND) else glDisable(GL_BLEND)
        if (lastEnableCullFace) glEnable(GL_CULL_FACE) else glDisable(GL_CULL_FACE)
        if (lastEnableDepthTest) glEnable(GL_DEPTH_TEST) else glDisable(GL_DEPTH_TEST)
        if (lastEnableStencilTest) glEnable(GL_STENCIL_TEST) else glDisable(GL_STENCIL_TEST)
        if (lastEnableScissorTest) glEnable(GL_SCISSOR_TEST) else glDisable(GL_SCISSOR_TEST)
        if (OPENGL_MAY_HAVE_PRIMITIVE_RESTART && data.glVersion >= 310)
            when {
                lastEnablePrimitiveRestart -> glEnable(GL_PRIMITIVE_RESTART)
                else -> glDisable(GL_PRIMITIVE_RESTART)
            }
        if (IMPL_HAS_POLYGON_MODE)
        // Desktop OpenGL 3.0 and OpenGL 3.1 had separate polygon draw modes for front-facing and back-facing faces of polygons
            if (data.glVersion <= 310 || data.glProfileIsCompat) {
                glPolygonMode(GL_FRONT, lastPolygonMode[0])
                glPolygonMode(GL_BACK, lastPolygonMode[1])
            } else
                glPolygonMode(GL_FRONT_AND_BACK, lastPolygonMode[0])
        glViewport(lastViewport)
        glScissor(lastScissorBox)
    }

    /** Build texture atlas */
    override fun createFontsTexture(): Boolean {

        /*  Load as RGBA 32-bit (75% of the memory is wasted, but default font is so small) because it is more likely
            to be compatible with user's existing shaders. If your ImTextureId represent a higher-level concept than
            just a GL texture id, consider calling GetTexDataAsAlpha8() instead to save on GPU memory.  */
        val (pixels, size) = io.fonts.getTexDataAsRGBA32()

        // Upload texture to graphics system
        // (Bilinear sampling is required by default. Set 'io.Fonts->Flags |= ImFontAtlasFlags_NoBakedLines' or 'style.AntiAliasedLinesUseTex = false' to allow point/nearest sampling)
        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D); glCall("glGetInteger")
        glGenTextures(data.fontTexture); glCall("glGenTextures")
        glBindTexture(GL_TEXTURE_2D, data.fontTexture[0]); glCall("glBindTexture")
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR); glCall("glTexParameteri")
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR); glCall("glTexParameteri")
        if (UNPACK_ROW_LENGTH) // Not on WebGL/ES
            glPixelStorei(GL_UNPACK_ROW_LENGTH, 0); glCall("glPixelStorei")
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, size.x, size.y, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels); glCall("glTexImage2D")

        // Store our identifier
        io.fonts.texID = data.fontTexture[0]

        // Restore state
        glBindTexture(GL_TEXTURE_2D, lastTexture); glCall("glBindTexture")

        return when {
            DEBUG -> checkError("mainLoop")
            else -> true
        }
    }

    override fun destroyFontsTexture() {
        if (data.fontTexture[0] != 0) {
            glDeleteTextures(data.fontTexture)
            io.fonts.texID = 0
            data.fontTexture[0] = 0
        }
    }

    override fun createDeviceObjects(): Boolean {

        // Backup GL state [JVM] we have to save also program since we do the uniform mat and texture setup once here
        val lastProgram = glGetInteger(GL_CURRENT_PROGRAM)
        val lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D)
        val lastArrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING)
        val lastElementBuffer = glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING)

        data.shaderHandle = createProgram()
        data.shaderHandle.use {
            data.attribLocationProjMtx = "ProjMtx".uniform
            "Texture".unit = semantic.sampler.DIFFUSE
        }

        // Create buffers
        data.buffers.gen()

        createFontsTexture()

        data.vao = GlVertexArray.gen(); glCall("glGenVertexArrays")

        // Restore modified GL state
        // This "glIsProgram()" check is required because if the program is "pending deletion" at the time of binding backup, it will have been deleted by now and will cause an OpenGL error. See #6220.
        if (glIsProgram(lastProgram)) glUseProgram(lastProgram)
        glBindTexture(GL_TEXTURE_2D, lastTexture)
        glBindBuffer(GL_ARRAY_BUFFER, lastArrayBuffer)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, lastElementBuffer)

        return when {
            DEBUG -> checkError("mainLoop")
            else -> true
        }
    }

    override fun destroyDeviceObjects() {

        data.vao.delete(); glCall("glDeleteVertexArrays")
        data.buffers.delete()

        if (data.shaderHandle.isValid)
            data.shaderHandle.delete()

        destroyFontsTexture()
    }

    companion object {

        enum class Buffer {
            Vertex, Element;

            companion object {
                val MAX = values().size
            }
        }

        var OPENGL_ES2 = false
        var OPENGL_ES3 = false

        // Desktop GL 3.2+ has glDrawElementsBaseVertex() which GL ES and WebGL don't have.
        val OPENGL_MAY_HAVE_VTX_OFFSET by lazy { !OPENGL_ES2 && !OPENGL_ES3 && data.glVersion >= 320 }

        // Desktop GL 3.3+ and GL ES 3.0+ have glBindSampler()
        val OPENGL_MAY_HAVE_BIND_SAMPLER by lazy { !OPENGL_ES2 && !OPENGL_ES3 && data.glVersion >= 330 }

        // Desktop GL 3.1+ has GL_PRIMITIVE_RESTART state
        val OPENGL_MAY_HAVE_PRIMITIVE_RESTART by lazy { OPENGL_ES2 && !OPENGL_ES3 && data.glVersion >= 310 }

        // Desktop GL use extension detection
        val OPENGL_MAY_HAVE_EXTENSIONS = true

        var CLIP_ORIGIN = false && Platform.get() != Platform.MACOSX

        // Desktop GL 2.0+ has glPolygonMode() which GL ES and WebGL don't have.
        var IMPL_HAS_POLYGON_MODE = true
        var UNPACK_ROW_LENGTH = true
        var SINGLE_GL_CONTEXT = true

        // [Debugging]
        var IMGUI_IMPL_OPENGL_DEBUG = false

        fun glCall(call: String) {
            if (!IMGUI_IMPL_OPENGL_DEBUG)
                return
            val glErr = glGetError()
            if (glErr != 0)
                System.err.println("GL error 0x%x returned from '$call'.".format(glErr))
        }

        val mat = Mat4()

        val vertexShader_glsl_120: String by lazy {
            """
                #version ${data.glslVersion}
                uniform mat4 ProjMtx;
                attribute vec2 Position;
                attribute vec2 UV;
                attribute vec4 Color;
                varying vec2 Frag_UV;
                varying vec4 Frag_Color;
                void main() {
                    Frag_UV = UV;
                    Frag_Color = Color;
                    gl_Position = ProjMtx * vec4(Position.xy,0,1);
                }""".trimIndent()
        }

        val vertexShader_glsl_130: String by lazy {
            """
                #version ${data.glslVersion}
                uniform mat4 ProjMtx;
                in vec2 Position;
                in vec2 UV;
                in vec4 Color;
                out vec2 Frag_UV;
                out vec4 Frag_Color;
                void main() {
                    Frag_UV = UV;
                    Frag_Color = Color;
                    gl_Position = ProjMtx * vec4(Position.xy,0,1);
                }""".trimIndent()
        }

        val vertexShader_glsl_410_core: String by lazy {
            """
                #version ${data.glslVersion}
                layout (location = ${semantic.attr.POSITION}) in vec2 Position;
                layout (location = ${semantic.attr.TEX_COORD}) in vec2 UV;
                layout (location = ${semantic.attr.COLOR}) in vec4 Color;
                uniform mat4 ProjMtx;
                out vec2 Frag_UV;
                out vec4 Frag_Color;
                void main() {
                    Frag_UV = UV;
                    Frag_Color = Color;
                    gl_Position = ProjMtx * vec4(Position.xy,0,1);
                }""".trimIndent()
        }

        val fragmentShader_glsl_120: String by lazy {
            """
                #version ${data.glslVersion}
                uniform sampler2D Texture;
                varying vec2 Frag_UV;
                varying vec4 Frag_Color;
                void main() {
                    gl_FragColor = Frag_Color * texture2D(Texture, Frag_UV.st);
                }""".trimIndent()
        }

        val fragmentShader_glsl_130: String by lazy {
            """
                #version ${data.glslVersion}
                uniform sampler2D Texture;
                in vec2 Frag_UV;
                in vec4 Frag_Color;
                out vec4 Out_Color;
                void main() {
                    Out_Color = Frag_Color * texture(Texture, Frag_UV.st);
                }""".trimIndent()
        }

        val fragmentShader_glsl_410_core: String by lazy {
            """
                #version ${data.glslVersion}
                in vec2 Frag_UV;
                in vec4 Frag_Color;
                uniform sampler2D Texture;
                layout (location = ${semantic.frag.COLOR}) out vec4 Out_Color;
                void main() {
                    Out_Color = Frag_Color * texture(Texture, Frag_UV.st);
                }""".trimIndent()
        }

        fun createProgram(): GlProgram {

            val vertexShader: String
            val fragmentShader: String
            when {
                data.glslVersion < 130 -> {
                    vertexShader = vertexShader_glsl_120
                    fragmentShader = fragmentShader_glsl_120
                }

                data.glslVersion >= 410 -> {
                    vertexShader = vertexShader_glsl_410_core
                    fragmentShader = fragmentShader_glsl_410_core
                }

                else -> {
                    vertexShader = vertexShader_glsl_130
                    fragmentShader = fragmentShader_glsl_130
                }
            }

            val vertHandle = GlShader.createFromSource(ShaderType.VERTEX_SHADER, vertexShader)
            val fragHandle = GlShader.createFromSource(ShaderType.FRAGMENT_SHADER, fragmentShader)

            return GlProgram.create().apply {
                attach(vertHandle)
                attach(fragHandle)
                if (data.glslVersion < 410) {
                    bindAttribLocation(semantic.attr.POSITION, "Position")
                    bindAttribLocation(semantic.attr.TEX_COORD, "UV")
                    bindAttribLocation(semantic.attr.COLOR, "Color")
                    bindFragDataLocation(semantic.frag.COLOR, "Out_Color")
                }
                link()
                detach(vertHandle)
                detach(fragHandle)
                vertHandle.delete()
                fragHandle.delete()
            }
        }

        val data = Data()

        class Data {

            /** Extracted at runtime using GL_MAJOR_VERSION, GL_MINOR_VERSION queries (e.g. 320 for GL 3.2) */
            var glVersion = 0

            /** Specified by user or detected based on compile time GL settings. */
            var glslVersion = if (Platform.get() == Platform.MACOSX) 150 else 130

            /** [JVM] both lazy for the same reason as `useBufferSubData` */
            val glProfileIsCompat: Boolean by lazy { glProfileMask has GL_CONTEXT_COMPATIBILITY_PROFILE_BIT }
            val glProfileMask: Int by lazy { glGetInteger(GL_CONTEXT_PROFILE_MASK) }
            val fontTexture = IntBuffer(1)
            var shaderHandle = GlProgram(0)

            var attribLocationProjMtx = -1

            val buffers = GlBuffers<Buffer>()
            var vao = GlVertexArray()

            var vertexBufferSize = 0L
            var indexBufferSize = 0L

            var hasClipOrigin = false

            // Query vendor to enable glBufferSubData kludge
            // [JVM] we have to go `lazy` because at the initialization there are still no GL caps and `glGetString` will fail
            val useBufferSubData by lazy { Platform.get() == Platform.WINDOWS && glGetString(GL_VENDOR)?.startsWith("Intel") ?: false }// #if !defined(IMGUI_IMPL_OPENGL_ES2)
        }

        // b7686a88e950f95250c1e88e301bef1ebca22523
        //        // OpenGL vertex attribute state
        //        #ifndef IMGUI_IMPL_OPENGL_USE_VERTEX_ARRAY
        //        struct ImGui_ImplOpenGL3_VtxAttribState
        //        {
        //            GLint   Enabled;
        //            GLint   Size;
        //            GLint   Type;
        //            GLint   Normalized;
        //            GLint   Stride;
        //            GLvoid* Ptr;
        //        };
        //
        //        static void ImGui_ImplOpenGL3_BackupVertexAttribState(GLint index, ImGui_ImplOpenGL3_VtxAttribState* state)
        //        {
        //            glGetVertexAttribiv(index, GL_VERTEX_ATTRIB_ARRAY_ENABLED, &state->Enabled);
        //            glGetVertexAttribiv(index, GL_VERTEX_ATTRIB_ARRAY_SIZE, &state->Size);
        //            glGetVertexAttribiv(index, GL_VERTEX_ATTRIB_ARRAY_TYPE, &state->Type);
        //            glGetVertexAttribiv(index, GL_VERTEX_ATTRIB_ARRAY_NORMALIZED, &state->Normalized);
        //            glGetVertexAttribiv(index, GL_VERTEX_ATTRIB_ARRAY_STRIDE, &state->Stride);
        //            glGetVertexAttribPointerv(index, GL_VERTEX_ATTRIB_ARRAY_POINTER, &state->Ptr);
        //        }
        //
        //        static void ImGui_ImplOpenGL3_RestoreVertexAttribState(GLint index, ImGui_ImplOpenGL3_VtxAttribState* state)
        //        {
        //            glVertexAttribPointer(index, state->Size, state->Type, state->Normalized, state->Stride, state->Ptr);
        //            if (state->Enabled) glEnableVertexAttribArray(index); else glDisableVertexAttribArray(index);
        //        }
        //        #endif
    }

    /*private fun debugSave(fbWidth: Int, fbHeight: Int) {
        if (g.frameCount % 60 == 0) {

            glReadBuffer(GL11C.GL_BACK)
            // no more alignment problems by texture updating
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1)  // upload
            glPixelStorei(GL_PACK_ALIGNMENT, 1)  // download

            val colorBufferImg = BufferedImage(fbWidth, fbHeight, BufferedImage.TYPE_INT_ARGB)
            val graphicsColor = colorBufferImg.graphics

            val buffer = ByteBuffer(fbWidth * fbHeight * 4)
            glReadPixels(0, 0, fbWidth, fbHeight, GL_RGBA, GL_UNSIGNED_BYTE, buffer)

            var i = 0
            for (h in 0 until fbHeight) {
                for (w in 0 until fbWidth) {

                    val iR = buffer[i + 0].toUInt()
                    val iG = buffer[i + 1].toUInt()
                    val iB = buffer[i + 2].toUInt()
                    val iA = buffer[i + 3].toInt() and 0xff

                    graphicsColor.color = Color(iR, iG, iB, iA)
                    graphicsColor.fillRect(w, fbHeight - h - 1, 1, 1) // height - h is for flipping the image
                    i += 4
                }
            }

            val imgNameColor = "whate_(${System.currentTimeMillis()}).png"
            ImageIO.write(colorBufferImg, "png", File("""C:\Users\gbarbieri\Pictures\$imgNameColor"""))
            graphicsColor.dispose()
            buffer.free()
        }
    }*/
}