package imgui.unittests

import glm_.vec3.Vec3
import glm_.vec4.Vec4
import gln.checkError
import gln.glGetVec2
import gln.glGetVec4
import gln.glGetVec4i
import imgui.Context
import imgui.DEBUG
import imgui.ImGui
import imgui.impl.LwjglGlfw
import io.kotlintest.specs.AbstractAnnotationSpec
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL46.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.Platform
import uno.glfw.GlfwWindow
import uno.glfw.VSync
import uno.glfw.glfw
import java.util.logging.LogManager
import org.lwjgl.opengl.GLDebugMessageCallback
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL43




class GLDebugCallback_43 : GLDebugMessageCallback() {

    override fun invoke(source: Int, type: Int, id: Int, severity: Int, len: Int, msg: Long, udata: Long) {
        if (id == 131185 || id == 131218)
        // || id == 131186
            return
        val _source: String
        val _type: String
        val _severity: String
        when (source) {
            GL_DEBUG_SOURCE_API -> _source = "API"

            GL_DEBUG_SOURCE_WINDOW_SYSTEM -> _source = "WINDOW SYSTEM"

            GL_DEBUG_SOURCE_SHADER_COMPILER -> _source = "SHADER COMPILER"

            GL_DEBUG_SOURCE_THIRD_PARTY -> _source = "THIRD PARTY"

            GL_DEBUG_SOURCE_APPLICATION -> _source = "APPLICATION"

            GL_DEBUG_SOURCE_OTHER -> _source = "UNKNOWN"
            else -> _source = "UNKNOWN"
        }

        when (type) {
            GL_DEBUG_TYPE_ERROR -> _type = "ERROR"

            GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR -> _type = "DEPRECATED BEHAVIOR"

            GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR -> _type = "UDEFINED BEHAVIOR"

            GL_DEBUG_TYPE_PORTABILITY -> _type = "PORTABILITY"

            GL_DEBUG_TYPE_PERFORMANCE -> _type = "PERFORMANCE"

            GL_DEBUG_TYPE_OTHER -> _type = "OTHER"

            GL_DEBUG_TYPE_MARKER -> _type = "MARKER"

            else -> _type = "UNKNOWN"
        }

        when (severity) {
            GL_DEBUG_SEVERITY_HIGH -> _severity = "HIGH"

            GL_DEBUG_SEVERITY_MEDIUM -> _severity = "MEDIUM"

            GL_DEBUG_SEVERITY_LOW -> _severity = "LOW"

            GL_DEBUG_SEVERITY_NOTIFICATION -> _severity = "NOTIFICATION"

            else -> _severity = "UNKNOWN"
        }

        println(logger == null)

        logger.severe("ID $id:$_type of $_severity severity, raised from $_source: ${MemoryUtil.memASCII(msg)}")
        logger.info(Thread.currentThread().stackTrace[5].toString())
        logger.info(Thread.currentThread().stackTrace[6].toString())
        logger.info(Thread.currentThread().stackTrace[7].toString())
    }

    companion object {
        private val logger = LogManager.getLogManager().getLogger("")
    }
}

class RendererTests {

    lateinit var window: GlfwWindow
    lateinit var ctx: Context


    var f = 0f
    var showAnotherWindow = false
    var showDemo = true
    var counter = 0

    lateinit var lwjglGlfw: LwjglGlfw

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            RendererTests().testUntouchedState()
        }
    }

    private lateinit var gldc: GLDebugCallback_43

    @AbstractAnnotationSpec.Test
    fun testUntouchedState() {

        glfw.init(if (Platform.get() == Platform.MACOSX) "3.2" else "3.0")

        window = GlfwWindow(1280, 720, "Dear ImGui Lwjgl OpenGL3 example").apply {
            init()
        }

        glfw.swapInterval = VSync.ON   // Enable vsync

        // Setup ImGui binding
//         glslVersion = 330 // set here your desidered glsl version

        // Setup Dear ImGui context
        ctx = Context()
        //io.configFlags = io.configFlags or ConfigFlag.NavEnableKeyboard  // Enable Keyboard Controls
        //io.configFlags = io.configFlags or ConfigFlag.NavEnableGamepad   // Enable Gamepad Controls

        // Setup Dear ImGui style
        ImGui.styleColorsDark()
//        ImGui.styleColorsClassic()

        // Setup Platform/Renderer bindings
        lwjglGlfw = LwjglGlfw(window, true, LwjglGlfw.GlfwClientApi.OpenGL)

        GL11.glEnable(GL43.GL_DEBUG_OUTPUT)
        GL11.glEnable(GL43.GL_DEBUG_OUTPUT_SYNCHRONOUS)

        gldc = GLDebugCallback_43()
        GL43.glDebugMessageCallback(gldc, 1)
        GL43.glDebugMessageControl(GL11.GL_DONT_CARE, GL11.GL_DONT_CARE, GL11.GL_DONT_CARE, BufferUtils.createIntBuffer(0),
                true)

        val initialGL_CURRENT_COLORglGetFloatv = glGetVec4(GL_CURRENT_COLOR)
        val initialGL_CURRENT_INDEXglGetFloatv = glGetFloat(GL_CURRENT_INDEX)
        val initialGL_CURRENT_TEXTURE_COORDSglGetFloatv = glGetVec4(GL_CURRENT_TEXTURE_COORDS)
        val initialGL_CURRENT_NORMALglGetFloatv = glGetVec3(GL_CURRENT_NORMAL)
        val initialGL_CURRENT_RASTER_POSITIONglGetFloatv = glGetVec4(GL_CURRENT_RASTER_POSITION)
        val initialGL_CURRENT_RASTER_DISTANCEglGetFloatv = glGetFloat(GL_CURRENT_RASTER_DISTANCE)
        val initialGL_CURRENT_RASTER_COLORglGetFloatv = glGetVec4(GL_CURRENT_RASTER_COLOR)
        val initialGL_CURRENT_RASTER_INDEXglGetFloatv = glGetFloat(GL_CURRENT_RASTER_INDEX)
        val initialGL_CURRENT_RASTER_TEXTURE_COORDSglGetFloatv = glGetVec4(GL_CURRENT_RASTER_TEXTURE_COORDS)
        val initialGL_CURRENT_RASTER_POSITION_VALIDglGetBooleanv = glGetBoolean(GL_CURRENT_RASTER_POSITION_VALID)
        val initialGL_EDGE_FLAGglGetBooleanv = glGetBoolean(GL_EDGE_FLAG)
        val initialGL_MODELVIEW_MATRIXglGetFloatv = MemoryUtil.memAllocFloat(16)
        glGetFloatv(GL_MODELVIEW_MATRIX, initialGL_MODELVIEW_MATRIXglGetFloatv)
        val initialGL_PROJECTION_MATRIXglGetFloatv = MemoryUtil.memAllocFloat(16)
        glGetFloatv(GL_PROJECTION_MATRIX, initialGL_PROJECTION_MATRIXglGetFloatv)
        val initialGL_TEXTURE_MATRIXglGetFloatv = MemoryUtil.memAllocFloat(16)
        glGetFloatv(GL_TEXTURE_MATRIX, initialGL_TEXTURE_MATRIXglGetFloatv)
        val initialGL_VIEWPORTglGetIntegerv = glGetInteger(GL_VIEWPORT)
        val initialGL_DEPTH_RANGEglGetFloatv = glGetVec2(GL_DEPTH_RANGE)
        val initialGL_MODELVIEW_STACK_DEPTHglGetIntegerv = glGetInteger(GL_MODELVIEW_STACK_DEPTH)
        val initialGL_PROJECTION_STACK_DEPTHglGetIntegerv = glGetInteger(GL_PROJECTION_STACK_DEPTH)
        val initialGL_TEXTURE_STACK_DEPTHglGetIntegerv = glGetInteger(GL_TEXTURE_STACK_DEPTH)
        val initialGL_MATRIX_MODEglGetIntegerv = glGetInteger(GL_MATRIX_MODE)
        val initialGL_NORMALIZEboolglIsEnabled = glIsEnabled(GL_NORMALIZE)
        val initialGL_CLIP_PLANE0glGetClipPlane = MemoryUtil.memAllocDouble(4)
        glGetClipPlane(GL_CLIP_PLANE0, initialGL_CLIP_PLANE0glGetClipPlane)
        val initialGL_CLIP_PLANE1glGetClipPlane = MemoryUtil.memAllocDouble(4)
        glGetClipPlane(GL_CLIP_PLANE1, initialGL_CLIP_PLANE1glGetClipPlane)
        val initialGL_CLIP_PLANE0boolglIsEnabled = glIsEnabled(GL_CLIP_PLANE0)
        val initialGL_CLIP_PLANE1boolglIsEnabled = glIsEnabled(GL_CLIP_PLANE1)
        val initialGL_FOG_COLORglGetFloatv = glGetVec4(GL_FOG_COLOR)
        val initialGL_FOG_INDEXglGetFloatv = glGetFloat(GL_FOG_INDEX)
        val initialGL_FOG_DENSITYglGetFloatv = glGetFloat(GL_FOG_DENSITY)
        val initialGL_FOG_STARTglGetFloatv = glGetFloat(GL_FOG_START)
        val initialGL_FOG_ENDglGetFloatv = glGetFloat(GL_FOG_END)
        val initialGL_FOG_MODEglGetIntegerv = glGetInteger(GL_FOG_MODE)
        val initialGL_FOGboolglIsEnabled = glIsEnabled(GL_FOG)
        val initialGL_SHADE_MODELglGetIntegerv = glGetInteger(GL_SHADE_MODEL)
        val initialGL_LIGHTINGboolglIsEnabled = glIsEnabled(GL_LIGHTING)
        val initialGL_COLOR_MATERIALboolglIsEnabled = glIsEnabled(GL_COLOR_MATERIAL)
        val initialGL_COLOR_MATERIAL_PARAMETERglGetIntegerv = glGetInteger(GL_COLOR_MATERIAL_PARAMETER)
        val initialGL_COLOR_MATERIAL_FACEglGetIntegerv = glGetInteger(GL_COLOR_MATERIAL_FACE)
        val initialGL_AMBIENTglGetMaterialfv = MemoryUtil.memAllocFloat(GL_AMBIENT)
        glGetMaterialfv(GL_FRONT, GL_AMBIENT, initialGL_AMBIENTglGetMaterialfv)
        val initialGL_DIFFUSEglGetMaterialfv = MemoryUtil.memAllocFloat(GL_DIFFUSE)
        glGetMaterialfv(GL_FRONT, GL_DIFFUSE, initialGL_DIFFUSEglGetMaterialfv)
        val initialGL_SPECULARglGetMaterialfv = MemoryUtil.memAllocFloat(GL_SPECULAR)
        glGetMaterialfv(GL_FRONT, GL_SPECULAR, initialGL_SPECULARglGetMaterialfv)
        val initialGL_EMISSIONglGetMaterialfv = MemoryUtil.memAllocFloat(GL_EMISSION)
        glGetMaterialfv(GL_FRONT, GL_EMISSION, initialGL_EMISSIONglGetMaterialfv)
        val initialGL_SHININESSglGetMaterialfv = glGetMaterialf(GL_SHININESS)
        val initialGL_LIGHT_MODEL_AMBIENTglGetFloatv = glGetVec4(GL_LIGHT_MODEL_AMBIENT)
        val initialGL_LIGHT_MODEL_LOCAL_VIEWERglGetBooleanv = glGetBoolean(GL_LIGHT_MODEL_LOCAL_VIEWER)
        val initialGL_LIGHT_MODEL_TWO_SIDEglGetBooleanv = glGetBoolean(GL_LIGHT_MODEL_TWO_SIDE)
        val initialGL_AMBIENTglGetLightfv = MemoryUtil.memAllocFloat(GL_AMBIENT)
        glGetLightfv(GL_LIGHT0, GL_AMBIENT, initialGL_AMBIENTglGetLightfv)
        val initialGL_DIFFUSEglGetLightfv = glGetLightf(GL_LIGHT0, GL_DIFFUSE)
        val initialGL_SPECULARglGetLightfv = glGetLightf(GL_LIGHT0, GL_SPECULAR)
        val initialGL_POSITIONglGetLightfv = MemoryUtil.memAllocFloat(GL_POSITION)
        glGetLightfv(GL_LIGHT0, GL_POSITION, initialGL_POSITIONglGetLightfv)
        val initialGL_CONSTANT_ATTENUATIONglGetLightfv = glGetLightf(GL_LIGHT0, GL_CONSTANT_ATTENUATION)
        val initialGL_LINEAR_ATTENUATIONglGetLightfv = glGetLightf(GL_LIGHT0, GL_LINEAR_ATTENUATION)
        val initialGL_QUADRATIC_ATTENUATIONglGetLightfv = glGetLightf(GL_LIGHT0, GL_QUADRATIC_ATTENUATION)
        val initialGL_SPOT_DIRECTIONglGetLightfv = MemoryUtil.memAllocFloat(GL_SPOT_DIRECTION)
        glGetLightfv(GL_LIGHT0, GL_SPOT_DIRECTION, initialGL_SPOT_DIRECTIONglGetLightfv)
        val initialGL_SPOT_EXPONENTglGetLightfv = glGetLightf(GL_LIGHT0, GL_SPOT_EXPONENT)
        val initialGL_SPOT_CUTOFFglGetLightfv = glGetLightf(GL_LIGHT0, GL_SPOT_CUTOFF)
        val initialGL_LIGHT0boolglIsEnabled = glIsEnabled(GL_LIGHT0)
        val initialGL_LIGHT1boolglIsEnabled = glIsEnabled(GL_LIGHT1)
        val initialGL_POINT_SIZEglGetFloatv = glGetFloat(GL_POINT_SIZE)
        val initialGL_POINT_SMOOTHboolglIsEnabled = glIsEnabled(GL_POINT_SMOOTH)
        val initialGL_LINE_WIDTHglGetFloatv = glGetFloat(GL_LINE_WIDTH)
        val initialGL_LINE_SMOOTHboolglIsEnabled = glIsEnabled(GL_LINE_SMOOTH)
        val initialGL_LINE_STIPPLE_PATTERNglGetIntegerv = glGetInteger(GL_LINE_STIPPLE_PATTERN)
        val initialGL_LINE_STIPPLE_REPEATglGetIntegerv = glGetInteger(GL_LINE_STIPPLE_REPEAT)
        val initialGL_LINE_STIPPLEboolglIsEnabled = glIsEnabled(GL_LINE_STIPPLE)
        val initialGL_CULL_FACEboolglIsEnabled = glIsEnabled(GL_CULL_FACE)
        val initialGL_CULL_FACE_MODEglGetIntegerv = glGetInteger(GL_CULL_FACE_MODE)
        val initialGL_FRONT_FACEglGetIntegerv = glGetInteger(GL_FRONT_FACE)
        val initialGL_POLYGON_SMOOTHboolglIsEnabled = glIsEnabled(GL_POLYGON_SMOOTH)
        val initialGL_POLYGON_MODEglGetIntegerv = glGetInteger(GL_POLYGON_MODE)
        val initialGL_POLYGON_STIPPLEboolglIsEnabled = glIsEnabled(GL_POLYGON_STIPPLE)
        val initialGL_TEXTURE_1DboolglIsEnabled = glIsEnabled(GL_TEXTURE_1D)
        val initialGL_TEXTURE_2DboolglIsEnabled = glIsEnabled(GL_TEXTURE_2D)

        val initialGL_TEXTURE_WIDTHglGetTexLevelParameter = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH)
        val initialGL_TEXTURE_HEIGHTglGetTexLevelParameter = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT)
        val initialGL_TEXTURE_BORDERglGetTexLevelParameter = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_BORDER)
        val initialGL_TEXTURE_COMPONENTSglGetTexLevelParameter = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_COMPONENTS)
        val initialGL_TEXTURE_GEN_SboolglIsEnabled = glIsEnabled(GL_TEXTURE_GEN_S)
        val initialGL_SCISSOR_TESTboolglIsEnabled = glIsEnabled(GL_SCISSOR_TEST)
        val initialGL_SCISSOR_BOXglGetIntegerv = glGetInteger(GL_SCISSOR_BOX)
        val initialGL_STENCIL_TESTboolglIsEnabled = glIsEnabled(GL_STENCIL_TEST)
        val initialGL_STENCIL_FUNCglGetIntegerv = glGetInteger(GL_STENCIL_FUNC)
        val initialGL_STENCIL_VALUE_MASKglGetIntegerv = glGetInteger(GL_STENCIL_VALUE_MASK)
        val initialGL_STENCIL_REFglGetIntegerv = glGetInteger(GL_STENCIL_REF)
        val initialGL_STENCIL_FAILglGetIntegerv = glGetInteger(GL_STENCIL_FAIL)
        val initialGL_STENCIL_PASS_DEPTH_FAIglGetIntegerv = glGetInteger(GL_STENCIL_PASS_DEPTH_FAIL)
        val initialGL_STENCIL_PASS_DEPTH_PASglGetIntegerv = glGetInteger(GL_STENCIL_PASS_DEPTH_PASS)
        val initialGL_ALPHA_TESTboolglIsEnabled = glIsEnabled(GL_ALPHA_TEST)
        val initialGL_ALPHA_TEST_FUNCglGetIntegerv = glGetInteger(GL_ALPHA_TEST_FUNC)
        val initialGL_ALPHA_TEST_REFglGetIntegerv = glGetInteger(GL_ALPHA_TEST_REF)
        val initialGL_DEPTH_TESTboolglIsEnabled = glIsEnabled(GL_DEPTH_TEST)
        val initialGL_DEPTH_FUNCglGetIntegerv = glGetInteger(GL_DEPTH_FUNC)
        val initialGL_BLENDboolglIsEnabled = glIsEnabled(GL_BLEND)
        val initialGL_BLEND_SRCglGetIntegerv = glGetInteger(GL_BLEND_SRC)
        val initialGL_BLEND_DSTglGetIntegerv = glGetInteger(GL_BLEND_DST)
        val initialGL_LOGIC_OPboolglIsEnabled = glIsEnabled(GL_LOGIC_OP)
        val initialGL_LOGIC_OP_MODEglGetIntegerv = glGetInteger(GL_LOGIC_OP_MODE)
        val initialGL_DITHERboolglIsEnabled = glIsEnabled(GL_DITHER)
        val initialGL_DRAW_BUFFERglGetIntegerv = glGetInteger(GL_DRAW_BUFFER)
        val initialGL_INDEX_WRITEMASKglGetIntegerv = glGetInteger(GL_INDEX_WRITEMASK)
        val initialGL_COLOR_WRITEMASKglGetBooleanv = glGetBoolean(GL_COLOR_WRITEMASK)
        val initialGL_DEPTH_WRITEMASKglGetBooleanv = glGetBoolean(GL_DEPTH_WRITEMASK)
        val initialGL_STENCIL_WRITEMASKglGetIntegerv = glGetInteger(GL_STENCIL_WRITEMASK)
        val initialGL_COLOR_CLEAR_VALUEglGetFloatv = glGetVec4(GL_COLOR_CLEAR_VALUE)
        val initialGL_INDEX_CLEAR_VALUEglGetFloatv = glGetFloat(GL_INDEX_CLEAR_VALUE)
        val initialGL_DEPTH_CLEAR_VALUEglGetIntegerv = glGetInteger(GL_DEPTH_CLEAR_VALUE)
        val initialGL_STENCIL_CLEAR_VALUEglGetIntegerv = glGetInteger(GL_STENCIL_CLEAR_VALUE)
        val initialGL_ACCUM_CLEAR_VALUEglGetFloatv = glGetFloat(GL_ACCUM_CLEAR_VALUE)
        val initialGL_UNPACK_SWAP_BYTESglGetBooleanv = glGetBoolean(GL_UNPACK_SWAP_BYTES)
        val initialGL_UNPACK_LSB_FIRSTglGetBooleanv = glGetBoolean(GL_UNPACK_LSB_FIRST)
        val initialGL_UNPACK_ROW_LENGTHglGetIntegerv = glGetInteger(GL_UNPACK_ROW_LENGTH)
        val initialGL_UNPACK_SKIP_ROWSglGetIntegerv = glGetInteger(GL_UNPACK_SKIP_ROWS)
        val initialGL_UNPACK_SKIP_PIXELSglGetIntegerv = glGetInteger(GL_UNPACK_SKIP_PIXELS)
        val initialGL_UNPACK_ALIGNMENTglGetIntegerv = glGetInteger(GL_UNPACK_ALIGNMENT)
        val initialGL_PACK_SWAP_BYTESglGetBooleanv = glGetBoolean(GL_PACK_SWAP_BYTES)
        val initialGL_PACK_LSB_FIRSTglGetBooleanv = glGetBoolean(GL_PACK_LSB_FIRST)
        val initialGL_PACK_ROW_LENGTHglGetIntegerv = glGetInteger(GL_PACK_ROW_LENGTH)
        val initialGL_PACK_SKIP_ROWSglGetIntegerv = glGetInteger(GL_PACK_SKIP_ROWS)
        val initialGL_PACK_SKIP_PIXELSglGetIntegerv = glGetInteger(GL_PACK_SKIP_PIXELS)
        val initialGL_PACK_ALIGNMENTglGetIntegerv = glGetInteger(GL_PACK_ALIGNMENT)
        val initialGL_MAP_COLORglGetBooleanv = glGetBoolean(GL_MAP_COLOR)
        val initialGL_MAP_STENCILglGetBooleanv = glGetBoolean(GL_MAP_STENCIL)
        val initialGL_INDEX_SHIFTglGetIntegerv = glGetInteger(GL_INDEX_SHIFT)
        val initialGL_INDEX_OFFSETglGetIntegerv = glGetInteger(GL_INDEX_OFFSET)
        val initialGL_ZOOM_XglGetFloatv = glGetFloat(GL_ZOOM_X)
        val initialGL_ZOOM_YglGetFloatv = glGetFloat(GL_ZOOM_Y)
        val initialGL_READ_BUFFERglGetIntegerv = glGetInteger(GL_READ_BUFFER)
        val initialGL_PERSPECTIVE_CORRECTION_HINTglGetIntegerv = glGetInteger(GL_PERSPECTIVE_CORRECTION_HINT)
        val initialGL_POINT_SMOOTH_HINTglGetIntegerv = glGetInteger(GL_POINT_SMOOTH_HINT)
        val initialGL_LINE_SMOOTH_HINTglGetIntegerv = glGetInteger(GL_LINE_SMOOTH_HINT)
        val initialGL_POLYGON_SMOOTH_HINTglGetIntegerv = glGetInteger(GL_POLYGON_SMOOTH_HINT)
        val initialGL_FOG_HINTglGetIntegerv = glGetInteger(GL_FOG_HINT)
        val initialGL_MAX_LIGHTSglGetIntegerv = glGetInteger(GL_MAX_LIGHTS)
        val initialGL_MAX_CLIP_PLANESglGetIntegerv = glGetInteger(GL_MAX_CLIP_PLANES)
        val initialGL_MAX_MODELVIEW_STACK_DEPTHglGetIntegerv = glGetInteger(GL_MAX_MODELVIEW_STACK_DEPTH)
        val initialGL_MAX_PROJECTION_STACK_DEPTHglGetIntegerv = glGetInteger(GL_MAX_PROJECTION_STACK_DEPTH)
        val initialGL_MAX_TEXTURE_STACK_DEPTHglGetIntegerv = glGetInteger(GL_MAX_TEXTURE_STACK_DEPTH)
        val initialGL_SUBPIXEL_BITSglGetIntegerv = glGetInteger(GL_SUBPIXEL_BITS)
        val initialGL_MAX_TEXTURE_SIZEglGetIntegerv = glGetInteger(GL_MAX_TEXTURE_SIZE)
        val initialGL_MAX_PIXEL_MAP_TABLEglGetIntegerv = glGetInteger(GL_MAX_PIXEL_MAP_TABLE)
        val initialGL_MAX_NAME_STACK_DEPTHglGetIntegerv = glGetInteger(GL_MAX_NAME_STACK_DEPTH)
        val initialGL_MAX_LIST_NESTINGglGetIntegerv = glGetInteger(GL_MAX_LIST_NESTING)
        val initialGL_MAX_EVAL_ORDERglGetIntegerv = glGetInteger(GL_MAX_EVAL_ORDER)
        val initialGL_MAX_VIEWPORT_DIMSglGetIntegerv = glGetInteger(GL_MAX_VIEWPORT_DIMS)
        val initialGL_MAX_ATTRIB_STACK_DEPTHglGetIntegerv = glGetInteger(GL_MAX_ATTRIB_STACK_DEPTH)
        val initialGL_AUX_BUFFERSglGetBooleanv = glGetBoolean(GL_AUX_BUFFERS)
        val initialGL_RGBA_MODEglGetBooleanv = glGetBoolean(GL_RGBA_MODE)
        val initialGL_INDEX_MODEglGetBooleanv = glGetBoolean(GL_INDEX_MODE)
        val initialGL_DOUBLEBUFFERglGetBooleanv = glGetBoolean(GL_DOUBLEBUFFER)
        val initialGL_STEREOglGetFloatv = glGetFloat(GL_STEREO)
        val initialGL_POINT_SIZE_RANGEglGetFloatv = glGetVec2(GL_POINT_SIZE_RANGE)
        val initialGL_POINT_SIZE_GRANULARITYglGetFloatv = glGetFloat(GL_POINT_SIZE_GRANULARITY)
        val initialGL_LINE_WIDTH_RANGEglGetFloatv = glGetVec2(GL_LINE_WIDTH_RANGE)
        val initialGL_LINE_WIDTH_GRANULARITYglGetFloatv = glGetFloat(GL_LINE_WIDTH_GRANULARITY)
        val initialActiveTexture = glGetInteger(GL_ACTIVE_TEXTURE)
        val initialProgram = glGetInteger(GL_CURRENT_PROGRAM)
        val initialTexture = glGetInteger(GL_TEXTURE_BINDING_2D)
        val initialSampler = glGetInteger(GL_SAMPLER_BINDING) 
        val initialArrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING)
        val initialVertexArray = glGetInteger(GL_VERTEX_ARRAY_BINDING)
        val initialPolygonMode = glGetInteger(GL_POLYGON_MODE)
        val initialViewport = glGetVec4i(GL_VIEWPORT)
        val initialScissorBox = glGetVec4i(GL_SCISSOR_BOX)
        val initialBlendSrcRgb = glGetInteger(GL_BLEND_SRC_RGB)
        val initialBlendDstRgb = glGetInteger(GL_BLEND_DST_RGB)
        val initialBlendSrcAlpha = glGetInteger(GL_BLEND_SRC_ALPHA)
        val initialBlendDstAlpha = glGetInteger(GL_BLEND_DST_ALPHA)
        val initialBlendEquationRgb = glGetInteger(GL_BLEND_EQUATION_RGB)
        val initialBlendEquationAlpha = glGetInteger(GL_BLEND_EQUATION_ALPHA)
        val initialEnableBlend = glIsEnabled(GL_BLEND)
        val initialEnableCullFace = glIsEnabled(GL_CULL_FACE)
        val initialEnableDepthTest = glIsEnabled(GL_DEPTH_TEST)
        val initialEnableScissorTest = glIsEnabled(GL_SCISSOR_TEST)



        stackPush().use {
            mainLoop(it)
            mainLoop(it)
            mainLoop(it)
        }






        val finalGL_CURRENT_COLORglGetFloatv = glGetVec4(GL_CURRENT_COLOR)
        if(!initialGL_CURRENT_COLORglGetFloatv.equals(finalGL_CURRENT_COLORglGetFloatv)) throw RuntimeException()
        val finalGL_CURRENT_INDEXglGetFloatv = glGetFloat(GL_CURRENT_INDEX)
        if(!initialGL_CURRENT_INDEXglGetFloatv.equals(finalGL_CURRENT_INDEXglGetFloatv)) throw RuntimeException()
        val finalGL_CURRENT_TEXTURE_COORDSglGetFloatv = glGetVec4(GL_CURRENT_TEXTURE_COORDS)
        if(!initialGL_CURRENT_TEXTURE_COORDSglGetFloatv.equals(finalGL_CURRENT_TEXTURE_COORDSglGetFloatv)) throw RuntimeException()
        val finalGL_CURRENT_NORMALglGetFloatv = glGetVec3(GL_CURRENT_NORMAL)
        if(!initialGL_CURRENT_NORMALglGetFloatv.equals(finalGL_CURRENT_NORMALglGetFloatv)) throw RuntimeException()
        val finalGL_CURRENT_RASTER_POSITIONglGetFloatv = glGetVec4(GL_CURRENT_RASTER_POSITION)
        if(!initialGL_CURRENT_RASTER_POSITIONglGetFloatv.equals(finalGL_CURRENT_RASTER_POSITIONglGetFloatv)) throw RuntimeException()
        val finalGL_CURRENT_RASTER_DISTANCEglGetFloatv = glGetFloat(GL_CURRENT_RASTER_DISTANCE)
        if(!initialGL_CURRENT_RASTER_DISTANCEglGetFloatv.equals(finalGL_CURRENT_RASTER_DISTANCEglGetFloatv)) throw RuntimeException()
        val finalGL_CURRENT_RASTER_COLORglGetFloatv = glGetVec4(GL_CURRENT_RASTER_COLOR)
        if(!initialGL_CURRENT_RASTER_COLORglGetFloatv.equals(finalGL_CURRENT_RASTER_COLORglGetFloatv)) throw RuntimeException()
        val finalGL_CURRENT_RASTER_INDEXglGetFloatv = glGetFloat(GL_CURRENT_RASTER_INDEX)
        if(!initialGL_CURRENT_RASTER_INDEXglGetFloatv.equals(finalGL_CURRENT_RASTER_INDEXglGetFloatv)) throw RuntimeException()
        val finalGL_CURRENT_RASTER_TEXTURE_COORDSglGetFloatv = glGetVec4(GL_CURRENT_RASTER_TEXTURE_COORDS)
        if(!initialGL_CURRENT_RASTER_TEXTURE_COORDSglGetFloatv.equals(finalGL_CURRENT_RASTER_TEXTURE_COORDSglGetFloatv)) throw RuntimeException()
        val finalGL_CURRENT_RASTER_POSITION_VALIDglGetBooleanv = glGetBoolean(GL_CURRENT_RASTER_POSITION_VALID)
        if(!initialGL_CURRENT_RASTER_POSITION_VALIDglGetBooleanv.equals(finalGL_CURRENT_RASTER_POSITION_VALIDglGetBooleanv)) throw RuntimeException()
        val finalGL_EDGE_FLAGglGetBooleanv = glGetBoolean(GL_EDGE_FLAG)
        if(!initialGL_EDGE_FLAGglGetBooleanv.equals(finalGL_EDGE_FLAGglGetBooleanv)) throw RuntimeException()
        val finalGL_MODELVIEW_MATRIXglGetFloatv = MemoryUtil.memAllocFloat(16)
        glGetFloatv(GL_MODELVIEW_MATRIX, finalGL_MODELVIEW_MATRIXglGetFloatv)
        if(!initialGL_MODELVIEW_MATRIXglGetFloatv.equals(finalGL_MODELVIEW_MATRIXglGetFloatv)) throw RuntimeException()
        val finalGL_PROJECTION_MATRIXglGetFloatv = MemoryUtil.memAllocFloat(16)
        glGetFloatv(GL_PROJECTION_MATRIX, finalGL_PROJECTION_MATRIXglGetFloatv)
        if(!initialGL_PROJECTION_MATRIXglGetFloatv.equals(finalGL_PROJECTION_MATRIXglGetFloatv)) throw RuntimeException()
        val finalGL_TEXTURE_MATRIXglGetFloatv = MemoryUtil.memAllocFloat(16)
        glGetFloatv(GL_TEXTURE_MATRIX, finalGL_TEXTURE_MATRIXglGetFloatv)
        if(!initialGL_TEXTURE_MATRIXglGetFloatv.equals(finalGL_TEXTURE_MATRIXglGetFloatv)) throw RuntimeException()
        val finalGL_VIEWPORTglGetIntegerv = glGetInteger(GL_VIEWPORT)
        if(!initialGL_VIEWPORTglGetIntegerv.equals(finalGL_VIEWPORTglGetIntegerv)) throw RuntimeException()
        val finalGL_DEPTH_RANGEglGetFloatv = glGetVec2(GL_DEPTH_RANGE)
        if(!initialGL_DEPTH_RANGEglGetFloatv.equals(finalGL_DEPTH_RANGEglGetFloatv)) throw RuntimeException()
        val finalGL_MODELVIEW_STACK_DEPTHglGetIntegerv = glGetInteger(GL_MODELVIEW_STACK_DEPTH)
        if(!initialGL_MODELVIEW_STACK_DEPTHglGetIntegerv.equals(finalGL_MODELVIEW_STACK_DEPTHglGetIntegerv)) throw RuntimeException()
        val finalGL_PROJECTION_STACK_DEPTHglGetIntegerv = glGetInteger(GL_PROJECTION_STACK_DEPTH)
        if(!initialGL_PROJECTION_STACK_DEPTHglGetIntegerv.equals(finalGL_PROJECTION_STACK_DEPTHglGetIntegerv)) throw RuntimeException()
        val finalGL_TEXTURE_STACK_DEPTHglGetIntegerv = glGetInteger(GL_TEXTURE_STACK_DEPTH)
        if(!initialGL_TEXTURE_STACK_DEPTHglGetIntegerv.equals(finalGL_TEXTURE_STACK_DEPTHglGetIntegerv)) throw RuntimeException()
        val finalGL_MATRIX_MODEglGetIntegerv = glGetInteger(GL_MATRIX_MODE)
        if(!initialGL_MATRIX_MODEglGetIntegerv.equals(finalGL_MATRIX_MODEglGetIntegerv)) throw RuntimeException()
        val finalGL_NORMALIZEboolglIsEnabled = glIsEnabled(GL_NORMALIZE)
        if(!initialGL_NORMALIZEboolglIsEnabled.equals(finalGL_NORMALIZEboolglIsEnabled)) throw RuntimeException()
        val finalGL_FOG_COLORglGetFloatv = glGetVec4(GL_FOG_COLOR)
        if(!initialGL_FOG_COLORglGetFloatv.equals(finalGL_FOG_COLORglGetFloatv)) throw RuntimeException()
        val finalGL_FOG_INDEXglGetFloatv = glGetFloat(GL_FOG_INDEX)
        if(!initialGL_FOG_INDEXglGetFloatv.equals(finalGL_FOG_INDEXglGetFloatv)) throw RuntimeException()
        val finalGL_FOG_DENSITYglGetFloatv = glGetFloat(GL_FOG_DENSITY)
        if(!initialGL_FOG_DENSITYglGetFloatv.equals(finalGL_FOG_DENSITYglGetFloatv)) throw RuntimeException()
        val finalGL_FOG_STARTglGetFloatv = glGetFloat(GL_FOG_START)
        if(!initialGL_FOG_STARTglGetFloatv.equals(finalGL_FOG_STARTglGetFloatv)) throw RuntimeException()
        val finalGL_FOG_ENDglGetFloatv = glGetFloat(GL_FOG_END)
        if(!initialGL_FOG_ENDglGetFloatv.equals(finalGL_FOG_ENDglGetFloatv)) throw RuntimeException()
        val finalGL_FOG_MODEglGetIntegerv = glGetInteger(GL_FOG_MODE)
        if(!initialGL_FOG_MODEglGetIntegerv.equals(finalGL_FOG_MODEglGetIntegerv)) throw RuntimeException()
        val finalGL_FOGboolglIsEnabled = glIsEnabled(GL_FOG)
        if(!initialGL_FOGboolglIsEnabled.equals(finalGL_FOGboolglIsEnabled)) throw RuntimeException()
        val finalGL_SHADE_MODELglGetIntegerv = glGetInteger(GL_SHADE_MODEL)
        if(!initialGL_SHADE_MODELglGetIntegerv.equals(finalGL_SHADE_MODELglGetIntegerv)) throw RuntimeException()
        val finalGL_LIGHTINGboolglIsEnabled = glIsEnabled(GL_LIGHTING)
        if(!initialGL_LIGHTINGboolglIsEnabled.equals(finalGL_LIGHTINGboolglIsEnabled)) throw RuntimeException()
        val finalGL_COLOR_MATERIALboolglIsEnabled = glIsEnabled(GL_COLOR_MATERIAL)
        if(!initialGL_COLOR_MATERIALboolglIsEnabled.equals(finalGL_COLOR_MATERIALboolglIsEnabled)) throw RuntimeException()
        val finalGL_COLOR_MATERIAL_PARAMETERglGetIntegerv = glGetInteger(GL_COLOR_MATERIAL_PARAMETER)
        if(!initialGL_COLOR_MATERIAL_PARAMETERglGetIntegerv.equals(finalGL_COLOR_MATERIAL_PARAMETERglGetIntegerv)) throw RuntimeException()
        val finalGL_COLOR_MATERIAL_FACEglGetIntegerv = glGetInteger(GL_COLOR_MATERIAL_FACE)
        if(!initialGL_COLOR_MATERIAL_FACEglGetIntegerv.equals(finalGL_COLOR_MATERIAL_FACEglGetIntegerv)) throw RuntimeException()
        val finalGL_AMBIENTglGetMaterialfv = MemoryUtil.memAllocFloat(GL_AMBIENT)
        glGetMaterialfv(GL_FRONT, GL_AMBIENT, finalGL_AMBIENTglGetMaterialfv)
        if(!initialGL_AMBIENTglGetMaterialfv.equals(finalGL_AMBIENTglGetMaterialfv)) throw RuntimeException()
        val finalGL_DIFFUSEglGetMaterialfv = MemoryUtil.memAllocFloat(GL_DIFFUSE)
        glGetMaterialfv(GL_FRONT, GL_DIFFUSE, finalGL_DIFFUSEglGetMaterialfv)
        if(!initialGL_DIFFUSEglGetMaterialfv.equals(finalGL_DIFFUSEglGetMaterialfv)) throw RuntimeException()
        val finalGL_SPECULARglGetMaterialfv = MemoryUtil.memAllocFloat(GL_SPECULAR)
        glGetMaterialfv(GL_FRONT, GL_SPECULAR, finalGL_SPECULARglGetMaterialfv)
        if(!initialGL_SPECULARglGetMaterialfv.equals(finalGL_SPECULARglGetMaterialfv)) throw RuntimeException()
        val finalGL_EMISSIONglGetMaterialfv = MemoryUtil.memAllocFloat(GL_EMISSION)
        glGetMaterialfv(GL_FRONT, GL_EMISSION, finalGL_EMISSIONglGetMaterialfv)
        if(!initialGL_EMISSIONglGetMaterialfv.equals(finalGL_EMISSIONglGetMaterialfv)) throw RuntimeException()
        val finalGL_SHININESSglGetMaterialfv = glGetMaterialf(GL_SHININESS)
        if(!initialGL_SHININESSglGetMaterialfv.equals(finalGL_SHININESSglGetMaterialfv)) throw RuntimeException()
        val finalGL_LIGHT_MODEL_AMBIENTglGetFloatv = glGetVec4(GL_LIGHT_MODEL_AMBIENT)
        if(!initialGL_LIGHT_MODEL_AMBIENTglGetFloatv.equals(finalGL_LIGHT_MODEL_AMBIENTglGetFloatv)) throw RuntimeException()
        val finalGL_LIGHT_MODEL_LOCAL_VIEWERglGetBooleanv = glGetBoolean(GL_LIGHT_MODEL_LOCAL_VIEWER)
        if(!initialGL_LIGHT_MODEL_LOCAL_VIEWERglGetBooleanv.equals(finalGL_LIGHT_MODEL_LOCAL_VIEWERglGetBooleanv)) throw RuntimeException()
        val finalGL_LIGHT_MODEL_TWO_SIDEglGetBooleanv = glGetBoolean(GL_LIGHT_MODEL_TWO_SIDE)
        if(!initialGL_LIGHT_MODEL_TWO_SIDEglGetBooleanv.equals(finalGL_LIGHT_MODEL_TWO_SIDEglGetBooleanv)) throw RuntimeException()
        val finalGL_AMBIENTglGetLightfv = MemoryUtil.memAllocFloat(GL_AMBIENT)
        glGetLightfv(GL_LIGHT0, GL_AMBIENT, finalGL_AMBIENTglGetLightfv)
        if(!initialGL_AMBIENTglGetLightfv.equals(finalGL_AMBIENTglGetLightfv)) throw RuntimeException()
        val finalGL_DIFFUSEglGetLightfv = glGetLightf(GL_LIGHT0, GL_DIFFUSE)
        if(!initialGL_DIFFUSEglGetLightfv.equals(finalGL_DIFFUSEglGetLightfv)) throw RuntimeException()
        val finalGL_SPECULARglGetLightfv = glGetLightf(GL_LIGHT0, GL_SPECULAR)
        if(!initialGL_SPECULARglGetLightfv.equals(finalGL_SPECULARglGetLightfv)) throw RuntimeException()
        val finalGL_POSITIONglGetLightfv = MemoryUtil.memAllocFloat(GL_POSITION)
        glGetLightfv(GL_LIGHT0, GL_POSITION, finalGL_POSITIONglGetLightfv)
        if(!initialGL_POSITIONglGetLightfv.equals(finalGL_POSITIONglGetLightfv)) throw RuntimeException()
        val finalGL_CONSTANT_ATTENUATIONglGetLightfv = glGetLightf(GL_LIGHT0, GL_CONSTANT_ATTENUATION)
        if(!initialGL_CONSTANT_ATTENUATIONglGetLightfv.equals(finalGL_CONSTANT_ATTENUATIONglGetLightfv)) throw RuntimeException()
        val finalGL_LINEAR_ATTENUATIONglGetLightfv = glGetLightf(GL_LIGHT0, GL_LINEAR_ATTENUATION)
        if(!initialGL_LINEAR_ATTENUATIONglGetLightfv.equals(finalGL_LINEAR_ATTENUATIONglGetLightfv)) throw RuntimeException()
        val finalGL_QUADRATIC_ATTENUATIONglGetLightfv = glGetLightf(GL_LIGHT0, GL_QUADRATIC_ATTENUATION)
        if(!initialGL_QUADRATIC_ATTENUATIONglGetLightfv.equals(finalGL_QUADRATIC_ATTENUATIONglGetLightfv)) throw RuntimeException()
        val finalGL_SPOT_DIRECTIONglGetLightfv = MemoryUtil.memAllocFloat(GL_SPOT_DIRECTION)
        glGetLightfv(GL_LIGHT0, GL_SPOT_DIRECTION, finalGL_SPOT_DIRECTIONglGetLightfv)
        if(!initialGL_SPOT_DIRECTIONglGetLightfv.equals(finalGL_SPOT_DIRECTIONglGetLightfv)) throw RuntimeException()
        val finalGL_SPOT_EXPONENTglGetLightfv = glGetLightf(GL_LIGHT0, GL_SPOT_EXPONENT)
        if(!initialGL_SPOT_EXPONENTglGetLightfv.equals(finalGL_SPOT_EXPONENTglGetLightfv)) throw RuntimeException()
        val finalGL_SPOT_CUTOFFglGetLightfv = glGetLightf(GL_LIGHT0, GL_SPOT_CUTOFF)
        if(!initialGL_SPOT_CUTOFFglGetLightfv.equals(finalGL_SPOT_CUTOFFglGetLightfv)) throw RuntimeException()
        val finalGL_POINT_SIZEglGetFloatv = glGetFloat(GL_POINT_SIZE)
        if(!initialGL_POINT_SIZEglGetFloatv.equals(finalGL_POINT_SIZEglGetFloatv)) throw RuntimeException()
        val finalGL_POINT_SMOOTHboolglIsEnabled = glIsEnabled(GL_POINT_SMOOTH)
        if(!initialGL_POINT_SMOOTHboolglIsEnabled.equals(finalGL_POINT_SMOOTHboolglIsEnabled)) throw RuntimeException()
        val finalGL_LINE_WIDTHglGetFloatv = glGetFloat(GL_LINE_WIDTH)
        if(!initialGL_LINE_WIDTHglGetFloatv.equals(finalGL_LINE_WIDTHglGetFloatv)) throw RuntimeException()
        val finalGL_LINE_SMOOTHboolglIsEnabled = glIsEnabled(GL_LINE_SMOOTH)
        if(!initialGL_LINE_SMOOTHboolglIsEnabled.equals(finalGL_LINE_SMOOTHboolglIsEnabled)) throw RuntimeException()
        val finalGL_LINE_STIPPLE_PATTERNglGetIntegerv = glGetInteger(GL_LINE_STIPPLE_PATTERN)
        if(!initialGL_LINE_STIPPLE_PATTERNglGetIntegerv.equals(finalGL_LINE_STIPPLE_PATTERNglGetIntegerv)) throw RuntimeException()
        val finalGL_LINE_STIPPLE_REPEATglGetIntegerv = glGetInteger(GL_LINE_STIPPLE_REPEAT)
        if(!initialGL_LINE_STIPPLE_REPEATglGetIntegerv.equals(finalGL_LINE_STIPPLE_REPEATglGetIntegerv)) throw RuntimeException()
        val finalGL_LINE_STIPPLEboolglIsEnabled = glIsEnabled(GL_LINE_STIPPLE)
        if(!initialGL_LINE_STIPPLEboolglIsEnabled.equals(finalGL_LINE_STIPPLEboolglIsEnabled)) throw RuntimeException()
        val finalGL_CULL_FACEboolglIsEnabled = glIsEnabled(GL_CULL_FACE)
        if(!initialGL_CULL_FACEboolglIsEnabled.equals(finalGL_CULL_FACEboolglIsEnabled)) throw RuntimeException()
        val finalGL_CULL_FACE_MODEglGetIntegerv = glGetInteger(GL_CULL_FACE_MODE)
        if(!initialGL_CULL_FACE_MODEglGetIntegerv.equals(finalGL_CULL_FACE_MODEglGetIntegerv)) throw RuntimeException()
        val finalGL_FRONT_FACEglGetIntegerv = glGetInteger(GL_FRONT_FACE)
        if(!initialGL_FRONT_FACEglGetIntegerv.equals(finalGL_FRONT_FACEglGetIntegerv)) throw RuntimeException()
        val finalGL_POLYGON_SMOOTHboolglIsEnabled = glIsEnabled(GL_POLYGON_SMOOTH)
        if(!initialGL_POLYGON_SMOOTHboolglIsEnabled.equals(finalGL_POLYGON_SMOOTHboolglIsEnabled)) throw RuntimeException()
        val finalGL_POLYGON_MODEglGetIntegerv = glGetInteger(GL_POLYGON_MODE)
        if(!initialGL_POLYGON_MODEglGetIntegerv.equals(finalGL_POLYGON_MODEglGetIntegerv)) throw RuntimeException()
        val finalGL_POLYGON_STIPPLEboolglIsEnabled = glIsEnabled(GL_POLYGON_STIPPLE)
        if(!initialGL_POLYGON_STIPPLEboolglIsEnabled.equals(finalGL_POLYGON_STIPPLEboolglIsEnabled)) throw RuntimeException()
        val finalGL_TEXTURE_WIDTHglGetTexLevelParameter = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH)
        if(!initialGL_TEXTURE_WIDTHglGetTexLevelParameter.equals(finalGL_TEXTURE_WIDTHglGetTexLevelParameter)) throw RuntimeException()
        val finalGL_TEXTURE_HEIGHTglGetTexLevelParameter = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT)
        if(!initialGL_TEXTURE_HEIGHTglGetTexLevelParameter.equals(finalGL_TEXTURE_HEIGHTglGetTexLevelParameter)) throw RuntimeException()
        val finalGL_TEXTURE_BORDERglGetTexLevelParameter = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_BORDER)
        if(!initialGL_TEXTURE_BORDERglGetTexLevelParameter.equals(finalGL_TEXTURE_BORDERglGetTexLevelParameter)) throw RuntimeException()
        val finalGL_TEXTURE_COMPONENTSglGetTexLevelParameter = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_COMPONENTS)
        if(!initialGL_TEXTURE_COMPONENTSglGetTexLevelParameter.equals(finalGL_TEXTURE_COMPONENTSglGetTexLevelParameter)) throw RuntimeException()
        val finalGL_TEXTURE_GEN_SboolglIsEnabled = glIsEnabled(GL_TEXTURE_GEN_S)
        if(!initialGL_TEXTURE_GEN_SboolglIsEnabled.equals(finalGL_TEXTURE_GEN_SboolglIsEnabled)) throw RuntimeException()
        val finalGL_SCISSOR_TESTboolglIsEnabled = glIsEnabled(GL_SCISSOR_TEST)
        if(!initialGL_SCISSOR_TESTboolglIsEnabled.equals(finalGL_SCISSOR_TESTboolglIsEnabled)) throw RuntimeException()
        val finalGL_SCISSOR_BOXglGetIntegerv = glGetInteger(GL_SCISSOR_BOX)
        if(!initialGL_SCISSOR_BOXglGetIntegerv.equals(finalGL_SCISSOR_BOXglGetIntegerv)) throw RuntimeException()
        val finalGL_STENCIL_TESTboolglIsEnabled = glIsEnabled(GL_STENCIL_TEST)
        if(!initialGL_STENCIL_TESTboolglIsEnabled.equals(finalGL_STENCIL_TESTboolglIsEnabled)) throw RuntimeException()
        val finalGL_STENCIL_FUNCglGetIntegerv = glGetInteger(GL_STENCIL_FUNC)
        if(!initialGL_STENCIL_FUNCglGetIntegerv.equals(finalGL_STENCIL_FUNCglGetIntegerv)) throw RuntimeException()
        val finalGL_STENCIL_VALUE_MASKglGetIntegerv = glGetInteger(GL_STENCIL_VALUE_MASK)
        if(!initialGL_STENCIL_VALUE_MASKglGetIntegerv.equals(finalGL_STENCIL_VALUE_MASKglGetIntegerv)) throw RuntimeException()
        val finalGL_STENCIL_REFglGetIntegerv = glGetInteger(GL_STENCIL_REF)
        if(!initialGL_STENCIL_REFglGetIntegerv.equals(finalGL_STENCIL_REFglGetIntegerv)) throw RuntimeException()
        val finalGL_STENCIL_FAILglGetIntegerv = glGetInteger(GL_STENCIL_FAIL)
        if(!initialGL_STENCIL_FAILglGetIntegerv.equals(finalGL_STENCIL_FAILglGetIntegerv)) throw RuntimeException()
        val finalGL_STENCIL_PASS_DEPTH_FAIglGetIntegerv = glGetInteger(GL_STENCIL_PASS_DEPTH_FAIL)
        if(!initialGL_STENCIL_PASS_DEPTH_FAIglGetIntegerv.equals(finalGL_STENCIL_PASS_DEPTH_FAIglGetIntegerv)) throw RuntimeException()
        val finalGL_STENCIL_PASS_DEPTH_PASglGetIntegerv = glGetInteger(GL_STENCIL_PASS_DEPTH_PASS)
        if(!initialGL_STENCIL_PASS_DEPTH_PASglGetIntegerv.equals(finalGL_STENCIL_PASS_DEPTH_PASglGetIntegerv)) throw RuntimeException()
        val finalGL_ALPHA_TESTboolglIsEnabled = glIsEnabled(GL_ALPHA_TEST)
        if(!initialGL_ALPHA_TESTboolglIsEnabled.equals(finalGL_ALPHA_TESTboolglIsEnabled)) throw RuntimeException()
        val finalGL_ALPHA_TEST_FUNCglGetIntegerv = glGetInteger(GL_ALPHA_TEST_FUNC)
        if(!initialGL_ALPHA_TEST_FUNCglGetIntegerv.equals(finalGL_ALPHA_TEST_FUNCglGetIntegerv)) throw RuntimeException()
        val finalGL_ALPHA_TEST_REFglGetIntegerv = glGetInteger(GL_ALPHA_TEST_REF)
        if(!initialGL_ALPHA_TEST_REFglGetIntegerv.equals(finalGL_ALPHA_TEST_REFglGetIntegerv)) throw RuntimeException()
        val finalGL_DEPTH_TESTboolglIsEnabled = glIsEnabled(GL_DEPTH_TEST)
        if(!initialGL_DEPTH_TESTboolglIsEnabled.equals(finalGL_DEPTH_TESTboolglIsEnabled)) throw RuntimeException()
        val finalGL_DEPTH_FUNCglGetIntegerv = glGetInteger(GL_DEPTH_FUNC)
        if(!initialGL_DEPTH_FUNCglGetIntegerv.equals(finalGL_DEPTH_FUNCglGetIntegerv)) throw RuntimeException()
        val finalGL_BLENDboolglIsEnabled = glIsEnabled(GL_BLEND)
        if(!initialGL_BLENDboolglIsEnabled.equals(finalGL_BLENDboolglIsEnabled)) throw RuntimeException()
        val finalGL_BLEND_SRCglGetIntegerv = glGetInteger(GL_BLEND_SRC)
        if(!initialGL_BLEND_SRCglGetIntegerv.equals(finalGL_BLEND_SRCglGetIntegerv)) throw RuntimeException()
        val finalGL_BLEND_DSTglGetIntegerv = glGetInteger(GL_BLEND_DST)
        if(!initialGL_BLEND_DSTglGetIntegerv.equals(finalGL_BLEND_DSTglGetIntegerv)) throw RuntimeException()
        val finalGL_LOGIC_OPboolglIsEnabled = glIsEnabled(GL_LOGIC_OP)
        if(!initialGL_LOGIC_OPboolglIsEnabled.equals(finalGL_LOGIC_OPboolglIsEnabled)) throw RuntimeException()
        val finalGL_LOGIC_OP_MODEglGetIntegerv = glGetInteger(GL_LOGIC_OP_MODE)
        if(!initialGL_LOGIC_OP_MODEglGetIntegerv.equals(finalGL_LOGIC_OP_MODEglGetIntegerv)) throw RuntimeException()
        val finalGL_DITHERboolglIsEnabled = glIsEnabled(GL_DITHER)
        if(!initialGL_DITHERboolglIsEnabled.equals(finalGL_DITHERboolglIsEnabled)) throw RuntimeException()
        val finalGL_DRAW_BUFFERglGetIntegerv = glGetInteger(GL_DRAW_BUFFER)
        if(!initialGL_DRAW_BUFFERglGetIntegerv.equals(finalGL_DRAW_BUFFERglGetIntegerv)) throw RuntimeException()
        val finalGL_INDEX_WRITEMASKglGetIntegerv = glGetInteger(GL_INDEX_WRITEMASK)
        if(!initialGL_INDEX_WRITEMASKglGetIntegerv.equals(finalGL_INDEX_WRITEMASKglGetIntegerv)) throw RuntimeException()
        val finalGL_COLOR_WRITEMASKglGetBooleanv = glGetBoolean(GL_COLOR_WRITEMASK)
        if(!initialGL_COLOR_WRITEMASKglGetBooleanv.equals(finalGL_COLOR_WRITEMASKglGetBooleanv)) throw RuntimeException()
        val finalGL_DEPTH_WRITEMASKglGetBooleanv = glGetBoolean(GL_DEPTH_WRITEMASK)
        if(!initialGL_DEPTH_WRITEMASKglGetBooleanv.equals(finalGL_DEPTH_WRITEMASKglGetBooleanv)) throw RuntimeException()
        val finalGL_STENCIL_WRITEMASKglGetIntegerv = glGetInteger(GL_STENCIL_WRITEMASK)
        if(!initialGL_STENCIL_WRITEMASKglGetIntegerv.equals(finalGL_STENCIL_WRITEMASKglGetIntegerv)) throw RuntimeException()
        val finalGL_COLOR_CLEAR_VALUEglGetFloatv = glGetVec4(GL_COLOR_CLEAR_VALUE)
        if(!initialGL_COLOR_CLEAR_VALUEglGetFloatv.equals(finalGL_COLOR_CLEAR_VALUEglGetFloatv)) throw RuntimeException()
        val finalGL_INDEX_CLEAR_VALUEglGetFloatv = glGetFloat(GL_INDEX_CLEAR_VALUE)
        if(!initialGL_INDEX_CLEAR_VALUEglGetFloatv.equals(finalGL_INDEX_CLEAR_VALUEglGetFloatv)) throw RuntimeException()
        val finalGL_DEPTH_CLEAR_VALUEglGetIntegerv = glGetInteger(GL_DEPTH_CLEAR_VALUE)
        if(!initialGL_DEPTH_CLEAR_VALUEglGetIntegerv.equals(finalGL_DEPTH_CLEAR_VALUEglGetIntegerv)) throw RuntimeException()
        val finalGL_STENCIL_CLEAR_VALUEglGetIntegerv = glGetInteger(GL_STENCIL_CLEAR_VALUE)
        if(!initialGL_STENCIL_CLEAR_VALUEglGetIntegerv.equals(finalGL_STENCIL_CLEAR_VALUEglGetIntegerv)) throw RuntimeException()
        val finalGL_ACCUM_CLEAR_VALUEglGetFloatv = glGetFloat(GL_ACCUM_CLEAR_VALUE)
        if(!initialGL_ACCUM_CLEAR_VALUEglGetFloatv.equals(finalGL_ACCUM_CLEAR_VALUEglGetFloatv)) throw RuntimeException()
        val finalGL_UNPACK_SWAP_BYTESglGetBooleanv = glGetBoolean(GL_UNPACK_SWAP_BYTES)
        if(!initialGL_UNPACK_SWAP_BYTESglGetBooleanv.equals(finalGL_UNPACK_SWAP_BYTESglGetBooleanv)) throw RuntimeException()
        val finalGL_UNPACK_LSB_FIRSTglGetBooleanv = glGetBoolean(GL_UNPACK_LSB_FIRST)
        if(!initialGL_UNPACK_LSB_FIRSTglGetBooleanv.equals(finalGL_UNPACK_LSB_FIRSTglGetBooleanv)) throw RuntimeException()
        val finalGL_UNPACK_ROW_LENGTHglGetIntegerv = glGetInteger(GL_UNPACK_ROW_LENGTH)
        if(!initialGL_UNPACK_ROW_LENGTHglGetIntegerv.equals(finalGL_UNPACK_ROW_LENGTHglGetIntegerv)) throw RuntimeException()
        val finalGL_UNPACK_SKIP_ROWSglGetIntegerv = glGetInteger(GL_UNPACK_SKIP_ROWS)
        if(!initialGL_UNPACK_SKIP_ROWSglGetIntegerv.equals(finalGL_UNPACK_SKIP_ROWSglGetIntegerv)) throw RuntimeException()
        val finalGL_UNPACK_SKIP_PIXELSglGetIntegerv = glGetInteger(GL_UNPACK_SKIP_PIXELS)
        if(!initialGL_UNPACK_SKIP_PIXELSglGetIntegerv.equals(finalGL_UNPACK_SKIP_PIXELSglGetIntegerv)) throw RuntimeException()
        val finalGL_UNPACK_ALIGNMENTglGetIntegerv = glGetInteger(GL_UNPACK_ALIGNMENT)
        if(!initialGL_UNPACK_ALIGNMENTglGetIntegerv.equals(finalGL_UNPACK_ALIGNMENTglGetIntegerv)) throw RuntimeException()
        val finalGL_PACK_SWAP_BYTESglGetBooleanv = glGetBoolean(GL_PACK_SWAP_BYTES)
        if(!initialGL_PACK_SWAP_BYTESglGetBooleanv.equals(finalGL_PACK_SWAP_BYTESglGetBooleanv)) throw RuntimeException()
        val finalGL_PACK_LSB_FIRSTglGetBooleanv = glGetBoolean(GL_PACK_LSB_FIRST)
        if(!initialGL_PACK_LSB_FIRSTglGetBooleanv.equals(finalGL_PACK_LSB_FIRSTglGetBooleanv)) throw RuntimeException()
        val finalGL_PACK_ROW_LENGTHglGetIntegerv = glGetInteger(GL_PACK_ROW_LENGTH)
        if(!initialGL_PACK_ROW_LENGTHglGetIntegerv.equals(finalGL_PACK_ROW_LENGTHglGetIntegerv)) throw RuntimeException()
        val finalGL_PACK_SKIP_ROWSglGetIntegerv = glGetInteger(GL_PACK_SKIP_ROWS)
        if(!initialGL_PACK_SKIP_ROWSglGetIntegerv.equals(finalGL_PACK_SKIP_ROWSglGetIntegerv)) throw RuntimeException()
        val finalGL_PACK_SKIP_PIXELSglGetIntegerv = glGetInteger(GL_PACK_SKIP_PIXELS)
        if(!initialGL_PACK_SKIP_PIXELSglGetIntegerv.equals(finalGL_PACK_SKIP_PIXELSglGetIntegerv)) throw RuntimeException()
        val finalGL_PACK_ALIGNMENTglGetIntegerv = glGetInteger(GL_PACK_ALIGNMENT)
        if(!initialGL_PACK_ALIGNMENTglGetIntegerv.equals(finalGL_PACK_ALIGNMENTglGetIntegerv)) throw RuntimeException()
        val finalGL_MAP_COLORglGetBooleanv = glGetBoolean(GL_MAP_COLOR)
        if(!initialGL_MAP_COLORglGetBooleanv.equals(finalGL_MAP_COLORglGetBooleanv)) throw RuntimeException()
        val finalGL_MAP_STENCILglGetBooleanv = glGetBoolean(GL_MAP_STENCIL)
        if(!initialGL_MAP_STENCILglGetBooleanv.equals(finalGL_MAP_STENCILglGetBooleanv)) throw RuntimeException()
        val finalGL_INDEX_SHIFTglGetIntegerv = glGetInteger(GL_INDEX_SHIFT)
        if(!initialGL_INDEX_SHIFTglGetIntegerv.equals(finalGL_INDEX_SHIFTglGetIntegerv)) throw RuntimeException()
        val finalGL_INDEX_OFFSETglGetIntegerv = glGetInteger(GL_INDEX_OFFSET)
        if(!initialGL_INDEX_OFFSETglGetIntegerv.equals(finalGL_INDEX_OFFSETglGetIntegerv)) throw RuntimeException()
        val finalGL_ZOOM_XglGetFloatv = glGetFloat(GL_ZOOM_X)
        if(!initialGL_ZOOM_XglGetFloatv.equals(finalGL_ZOOM_XglGetFloatv)) throw RuntimeException()
        val finalGL_ZOOM_YglGetFloatv = glGetFloat(GL_ZOOM_Y)
        if(!initialGL_ZOOM_YglGetFloatv.equals(finalGL_ZOOM_YglGetFloatv)) throw RuntimeException()
        val finalGL_READ_BUFFERglGetIntegerv = glGetInteger(GL_READ_BUFFER)
        if(!initialGL_READ_BUFFERglGetIntegerv.equals(finalGL_READ_BUFFERglGetIntegerv)) throw RuntimeException()
        val finalGL_PERSPECTIVE_CORRECTION_HINTglGetIntegerv = glGetInteger(GL_PERSPECTIVE_CORRECTION_HINT)
        if(!initialGL_PERSPECTIVE_CORRECTION_HINTglGetIntegerv.equals(finalGL_PERSPECTIVE_CORRECTION_HINTglGetIntegerv)) throw RuntimeException()
        val finalGL_POINT_SMOOTH_HINTglGetIntegerv = glGetInteger(GL_POINT_SMOOTH_HINT)
        if(!initialGL_POINT_SMOOTH_HINTglGetIntegerv.equals(finalGL_POINT_SMOOTH_HINTglGetIntegerv)) throw RuntimeException()
        val finalGL_LINE_SMOOTH_HINTglGetIntegerv = glGetInteger(GL_LINE_SMOOTH_HINT)
        if(!initialGL_LINE_SMOOTH_HINTglGetIntegerv.equals(finalGL_LINE_SMOOTH_HINTglGetIntegerv)) throw RuntimeException()
        val finalGL_POLYGON_SMOOTH_HINTglGetIntegerv = glGetInteger(GL_POLYGON_SMOOTH_HINT)
        if(!initialGL_POLYGON_SMOOTH_HINTglGetIntegerv.equals(finalGL_POLYGON_SMOOTH_HINTglGetIntegerv)) throw RuntimeException()
        val finalGL_FOG_HINTglGetIntegerv = glGetInteger(GL_FOG_HINT)
        if(!initialGL_FOG_HINTglGetIntegerv.equals(finalGL_FOG_HINTglGetIntegerv)) throw RuntimeException()
        val finalGL_MAX_LIGHTSglGetIntegerv = glGetInteger(GL_MAX_LIGHTS)
        if(!initialGL_MAX_LIGHTSglGetIntegerv.equals(finalGL_MAX_LIGHTSglGetIntegerv)) throw RuntimeException()
        val finalGL_MAX_CLIP_PLANESglGetIntegerv = glGetInteger(GL_MAX_CLIP_PLANES)
        if(!initialGL_MAX_CLIP_PLANESglGetIntegerv.equals(finalGL_MAX_CLIP_PLANESglGetIntegerv)) throw RuntimeException()
        val finalGL_MAX_MODELVIEW_STACK_DEPTHglGetIntegerv = glGetInteger(GL_MAX_MODELVIEW_STACK_DEPTH)
        if(!initialGL_MAX_MODELVIEW_STACK_DEPTHglGetIntegerv.equals(finalGL_MAX_MODELVIEW_STACK_DEPTHglGetIntegerv)) throw RuntimeException()
        val finalGL_MAX_PROJECTION_STACK_DEPTHglGetIntegerv = glGetInteger(GL_MAX_PROJECTION_STACK_DEPTH)
        if(!initialGL_MAX_PROJECTION_STACK_DEPTHglGetIntegerv.equals(finalGL_MAX_PROJECTION_STACK_DEPTHglGetIntegerv)) throw RuntimeException()
        val finalGL_MAX_TEXTURE_STACK_DEPTHglGetIntegerv = glGetInteger(GL_MAX_TEXTURE_STACK_DEPTH)
        if(!initialGL_MAX_TEXTURE_STACK_DEPTHglGetIntegerv.equals(finalGL_MAX_TEXTURE_STACK_DEPTHglGetIntegerv)) throw RuntimeException()
        val finalGL_SUBPIXEL_BITSglGetIntegerv = glGetInteger(GL_SUBPIXEL_BITS)
        if(!initialGL_SUBPIXEL_BITSglGetIntegerv.equals(finalGL_SUBPIXEL_BITSglGetIntegerv)) throw RuntimeException()
        val finalGL_MAX_TEXTURE_SIZEglGetIntegerv = glGetInteger(GL_MAX_TEXTURE_SIZE)
        if(!initialGL_MAX_TEXTURE_SIZEglGetIntegerv.equals(finalGL_MAX_TEXTURE_SIZEglGetIntegerv)) throw RuntimeException()
        val finalGL_MAX_PIXEL_MAP_TABLEglGetIntegerv = glGetInteger(GL_MAX_PIXEL_MAP_TABLE)
        if(!initialGL_MAX_PIXEL_MAP_TABLEglGetIntegerv.equals(finalGL_MAX_PIXEL_MAP_TABLEglGetIntegerv)) throw RuntimeException()
        val finalGL_MAX_NAME_STACK_DEPTHglGetIntegerv = glGetInteger(GL_MAX_NAME_STACK_DEPTH)
        if(!initialGL_MAX_NAME_STACK_DEPTHglGetIntegerv.equals(finalGL_MAX_NAME_STACK_DEPTHglGetIntegerv)) throw RuntimeException()
        val finalGL_MAX_LIST_NESTINGglGetIntegerv = glGetInteger(GL_MAX_LIST_NESTING)
        if(!initialGL_MAX_LIST_NESTINGglGetIntegerv.equals(finalGL_MAX_LIST_NESTINGglGetIntegerv)) throw RuntimeException()
        val finalGL_MAX_EVAL_ORDERglGetIntegerv = glGetInteger(GL_MAX_EVAL_ORDER)
        if(!initialGL_MAX_EVAL_ORDERglGetIntegerv.equals(finalGL_MAX_EVAL_ORDERglGetIntegerv)) throw RuntimeException()
        val finalGL_MAX_VIEWPORT_DIMSglGetIntegerv = glGetInteger(GL_MAX_VIEWPORT_DIMS)
        if(!initialGL_MAX_VIEWPORT_DIMSglGetIntegerv.equals(finalGL_MAX_VIEWPORT_DIMSglGetIntegerv)) throw RuntimeException()
        val finalGL_MAX_ATTRIB_STACK_DEPTHglGetIntegerv = glGetInteger(GL_MAX_ATTRIB_STACK_DEPTH)
        if(!initialGL_MAX_ATTRIB_STACK_DEPTHglGetIntegerv.equals(finalGL_MAX_ATTRIB_STACK_DEPTHglGetIntegerv)) throw RuntimeException()
        val finalGL_AUX_BUFFERSglGetBooleanv = glGetBoolean(GL_AUX_BUFFERS)
        if(!initialGL_AUX_BUFFERSglGetBooleanv.equals(finalGL_AUX_BUFFERSglGetBooleanv)) throw RuntimeException()
        val finalGL_RGBA_MODEglGetBooleanv = glGetBoolean(GL_RGBA_MODE)
        if(!initialGL_RGBA_MODEglGetBooleanv.equals(finalGL_RGBA_MODEglGetBooleanv)) throw RuntimeException()
        val finalGL_INDEX_MODEglGetBooleanv = glGetBoolean(GL_INDEX_MODE)
        if(!initialGL_INDEX_MODEglGetBooleanv.equals(finalGL_INDEX_MODEglGetBooleanv)) throw RuntimeException()
        val finalGL_DOUBLEBUFFERglGetBooleanv = glGetBoolean(GL_DOUBLEBUFFER)
        if(!initialGL_DOUBLEBUFFERglGetBooleanv.equals(finalGL_DOUBLEBUFFERglGetBooleanv)) throw RuntimeException()
        val finalGL_STEREOglGetFloatv = glGetFloat(GL_STEREO)
        if(!initialGL_STEREOglGetFloatv.equals(finalGL_STEREOglGetFloatv)) throw RuntimeException()
        val finalGL_POINT_SIZE_RANGEglGetFloatv = glGetVec2(GL_POINT_SIZE_RANGE)
        if(!initialGL_POINT_SIZE_RANGEglGetFloatv.equals(finalGL_POINT_SIZE_RANGEglGetFloatv)) throw RuntimeException()
        val finalGL_POINT_SIZE_GRANULARITYglGetFloatv = glGetFloat(GL_POINT_SIZE_GRANULARITY)
        if(!initialGL_POINT_SIZE_GRANULARITYglGetFloatv.equals(finalGL_POINT_SIZE_GRANULARITYglGetFloatv)) throw RuntimeException()
        val finalGL_LINE_WIDTH_RANGEglGetFloatv = glGetVec2(GL_LINE_WIDTH_RANGE)
        if(!initialGL_LINE_WIDTH_RANGEglGetFloatv.equals(finalGL_LINE_WIDTH_RANGEglGetFloatv)) throw RuntimeException()
        val finalGL_LINE_WIDTH_GRANULARITYglGetFloatv = glGetFloat(GL_LINE_WIDTH_GRANULARITY)
        if(!initialGL_LINE_WIDTH_GRANULARITYglGetFloatv.equals(finalGL_LINE_WIDTH_GRANULARITYglGetFloatv)) throw RuntimeException()
        val finalActiveTexture = glGetInteger(GL_ACTIVE_TEXTURE)
        if(!initialActiveTexture.equals(finalActiveTexture)) throw RuntimeException()
        val finalProgram = glGetInteger(GL_CURRENT_PROGRAM)
        if(!initialProgram.equals(finalProgram)) throw RuntimeException()
        val finalTexture = glGetInteger(GL_TEXTURE_BINDING_2D)
        if(!initialTexture.equals(finalTexture)) throw RuntimeException()
        val finalSampler = glGetInteger(GL_SAMPLER_BINDING)
        if(!initialSampler.equals(finalSampler)) throw RuntimeException()
        val finalArrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING)
        if(!initialArrayBuffer.equals(finalArrayBuffer)) throw RuntimeException()
        val finalVertexArray = glGetInteger(GL_VERTEX_ARRAY_BINDING)
        if(!initialVertexArray.equals(finalVertexArray)) throw RuntimeException()
        val finalPolygonMode = glGetInteger(GL_POLYGON_MODE)
        if(!initialPolygonMode.equals(finalPolygonMode)) throw RuntimeException()
        val finalViewport = glGetVec4i(GL_VIEWPORT)
        if(!initialViewport.equals(finalViewport)) throw RuntimeException()
        val finalScissorBox = glGetVec4i(GL_SCISSOR_BOX)
        if(!initialScissorBox.equals(finalScissorBox)) throw RuntimeException()
        val finalBlendSrcRgb = glGetInteger(GL_BLEND_SRC_RGB)
        if(!initialBlendSrcRgb.equals(finalBlendSrcRgb)) throw RuntimeException()
        val finalBlendDstRgb = glGetInteger(GL_BLEND_DST_RGB)
        if(!initialBlendDstRgb.equals(finalBlendDstRgb)) throw RuntimeException()
        val finalBlendSrcAlpha = glGetInteger(GL_BLEND_SRC_ALPHA)
        if(!initialBlendSrcAlpha.equals(finalBlendSrcAlpha)) throw RuntimeException()
        val finalBlendDstAlpha = glGetInteger(GL_BLEND_DST_ALPHA)
        if(!initialBlendDstAlpha.equals(finalBlendDstAlpha)) throw RuntimeException()
        val finalBlendEquationRgb = glGetInteger(GL_BLEND_EQUATION_RGB)
        if(!initialBlendEquationRgb.equals(finalBlendEquationRgb)) throw RuntimeException()
        val finalBlendEquationAlpha = glGetInteger(GL_BLEND_EQUATION_ALPHA)
        if(!initialBlendEquationAlpha.equals(finalBlendEquationAlpha)) throw RuntimeException()
        val finalEnableBlend = glIsEnabled(GL_BLEND)
        if(!initialEnableBlend.equals(finalEnableBlend)) throw RuntimeException()
        val finalEnableCullFace = glIsEnabled(GL_CULL_FACE)
        if(!initialEnableCullFace.equals(finalEnableCullFace)) throw RuntimeException()
        val finalEnableDepthTest = glIsEnabled(GL_DEPTH_TEST)
        if(!initialEnableDepthTest.equals(finalEnableDepthTest)) throw RuntimeException()
        val finalEnableScissorTest = glIsEnabled(GL_SCISSOR_TEST)
        if(!initialEnableScissorTest.equals(finalEnableScissorTest)) throw RuntimeException()


        lwjglGlfw.shutdown()
        ctx.destroy()

        window.destroy()
        glfw.terminate()
    }

    private fun glGetMaterialf(gL_SHININESS: Int): Float {
        stackPush().use {
            val s = it.mallocFloat(1)
            glGetMaterialfv(GL_FRONT, gL_SHININESS, s)
            return s[0]
        }
    }

    private fun glGetVec3(pname: Int): Vec3 {
        stackPush().use {
            val fb = it.mallocFloat(3)
            glGetFloatv(pname, fb)
            return Vec3(fb[0], fb[1], fb[2])
        }
    }

    fun mainLoop(stack: MemoryStack) {

        // Start the Dear ImGui frame
        lwjglGlfw.newFrame()

        ImGui.run {

            newFrame()

            // 1. Show the big demo window (Most of the sample code is in ImGui::ShowDemoWindow()! You can browse its code to learn more about Dear ImGui!).
            if (showDemo)
                showDemoWindow(::showDemo)

            // 2. Show a simple window that we create ourselves. We use a Begin/End pair to created a named window.
            run {
                begin("Hello, world!")                          // Create a window called "Hello, world!" and append into it.

                text("This is some useful text.")                // Display some text (you can use a format strings too)
                checkbox("Demo Window", ::showDemo)             // Edit bools storing our window open/close state
                checkbox("Another Window", ::showAnotherWindow)

                sliderFloat("float", ::f, 0f, 1f)   // Edit 1 float using a slider from 0.0f to 1.0f

                if (button("Button"))                           // Buttons return true when clicked (most widgets return true when edited/activated)
                    counter++

                /*  Or you can take advantage of functional programming and pass directly a lambda as last parameter:
                    button("Button") { counter++ }                */

                sameLine()
                text("counter = $counter")

                text("Application average %.3f ms/frame (%.1f FPS)", 1_000f / io.framerate, io.framerate)

                end()

                // 3. Show another simple window.
                if (showAnotherWindow) {
                    // Pass a pointer to our bool variable (the window will have a closing button that will clear the bool when clicked)
                    begin_("Another Window", ::showAnotherWindow)
                    text("Hello from another window!")
                    if (button("Close Me"))
                        showAnotherWindow = false
                    end()
                }
            }
        }

        // Rendering
        ImGui.render()
        gln.glViewport(window.framebufferSize)
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT)

        lwjglGlfw.renderDrawData(ImGui.drawData!!)

        if(DEBUG)
            checkError("mainLoop")
    }
}