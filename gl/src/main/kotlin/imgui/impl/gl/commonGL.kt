package imgui.impl.gl

import gln.ShaderType.Companion.FRAGMENT_SHADER
import gln.ShaderType.Companion.VERTEX_SHADER
import gln.glf.semantic
import gln.identifiers.GlProgram
import gln.identifiers.GlShader
import imgui.internal.DrawData
import kool.IntBuffer
import org.lwjgl.opengl.GL
import org.lwjgl.system.Platform


// OpenGL Data

//----------------------------------------
// OpenGL    GLSL      GLSL
// version   version   string
//----------------------------------------
//  2.0       110       "#version 110"
//  2.1       120       "#version 120"
//  3.0       130       "#version 130"
//  3.1       140       "#version 140"
//  3.2       150       "#version 150"
//  3.3       330       "#version 330 core"
//  4.0       400       "#version 400 core"
//  4.1       410       "#version 410 core"
//  4.2       420       "#version 410 core"
//  4.3       430       "#version 430 core"
//  ES 2.0    100       "#version 100"      = WebGL 1.0
//  ES 3.0    300       "#version 300 es"   = WebGL 2.0
//----------------------------------------

/** Store GLSL version string so we can refer to it later in case we recreate shaders.
 * Note: GLSL version is NOT the same as GL version. Leave this to default if unsure. */
var glslVersionString = ""

/** Extracted at runtime using GL_MAJOR_VERSION, GL_MINOR_VERSION queries (e.g. 320 for GL 3.2) */
var glVersion = 0

val vertexShader_glsl_120: String by lazy {
    """
    $glslVersionString
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
    }"""
}

val vertexShader_glsl_130: String by lazy {
    """
    $glslVersionString
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
    }"""
}

//const GLchar* vertex_shader_glsl_300_es =
//"precision mediump float;\n"
//"layout (location = 0) in vec2 Position;\n"
//"layout (location = 1) in vec2 UV;\n"
//"layout (location = 2) in vec4 Color;\n"
//"uniform mat4 ProjMtx;\n"
//"out vec2 Frag_UV;\n"
//"out vec4 Frag_Color;\n"
//"void main()\n"
//"{\n"
//"    Frag_UV = UV;\n"
//"    Frag_Color = Color;\n"
//"    gl_Position = ProjMtx * vec4(Position.xy,0,1);\n"
//"}\n";

val vertexShader_glsl_410_core: String by lazy {
    """
    $glslVersionString
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
    }"""
}

val fragmentShader_glsl_120: String by lazy {
    """
    $glslVersionString
    uniform sampler2D Texture;
    varying vec2 Frag_UV;
    varying vec4 Frag_Color;
    void main() {
        gl_FragColor = Frag_Color * texture2D(Texture, Frag_UV.st);
    }"""
}

val fragmentShader_glsl_130: String by lazy {
    """
    $glslVersionString
    uniform sampler2D Texture;
    in vec2 Frag_UV;
    in vec4 Frag_Color;
    out vec4 Out_Color;
    void main() {
        Out_Color = Frag_Color * texture(Texture, Frag_UV.st);
    }"""
}

//const GLchar* fragment_shader_glsl_300_es =
//"precision mediump float;\n"
//"uniform sampler2D Texture;\n"
//"in vec2 Frag_UV;\n"
//"in vec4 Frag_Color;\n"
//"layout (location = 0) out vec4 Out_Color;\n"
//"void main()\n"
//"{\n"
//"    Out_Color = Frag_Color * texture(Texture, Frag_UV.st);\n"
//"}\n";

val fragmentShader_glsl_410_core: String by lazy {
    """
    $glslVersionString
    in vec2 Frag_UV;
    in vec4 Frag_Color;
    uniform sampler2D Texture;
    layout (location = ${semantic.frag.COLOR}) out vec4 Out_Color;
    void main() {
        Out_Color = Frag_Color * texture(Texture, Frag_UV.st);
    }"""
}

fun createProgram(glslVersion: Int): GlProgram {

    // Select shaders matching our GLSL versions
    val vertexShader: String
    val fragmentShader: String
    when {
        glslVersion < 130 -> {
            vertexShader = vertexShader_glsl_120
            fragmentShader = fragmentShader_glsl_120
        }
        glslVersion >= 410 -> {
            vertexShader = vertexShader_glsl_410_core
            fragmentShader = fragmentShader_glsl_410_core
        }
        else -> {
            vertexShader = vertexShader_glsl_130
            fragmentShader = fragmentShader_glsl_130
        }
    }

    // Create shaders
    val vertHandle = GlShader.createFromSource(VERTEX_SHADER, vertexShader)
    val fragHandle = GlShader.createFromSource(FRAGMENT_SHADER, fragmentShader)

    return GlProgram.create().apply {
        attach(vertHandle)
        attach(fragHandle)
        if (glslVersion < 410) {
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


enum class Buffer {
    Vertex, Element;

    companion object {
        val MAX = values().size
    }
}

val bufferName = IntBuffer<Buffer>()

/*  JVM differs. We are not yet doing this because no user case. If ever needed: https://github.com/ocornut/imgui/compare/a1a36e762eac707cc3f9c81ec5af7150f6620c4c...d7f97922b883aec0c873e0e405c46b154d382120

    ~Recreate the VAO every time (This is to easily allow multiple GL contexts. VAO are not shared among GL contexts,
    and we don't track creation/deletion of windows so we don't have an obvious key to use to cache them.)~     */
val vaoName = IntBuffer(1)

val fontTexture = IntBuffer(1)



var IMPL_OPENGL_ES2 = false
var IMPL_OPENGL_ES3 = false

var MAY_HAVE_VTX_OFFSET = lazy { IMPL_OPENGL_ES2 || IMPL_OPENGL_ES3 || glVersion >= 320 }.value

var CLIP_ORIGIN = false

var POLYGON_MODE = true
var SAMPLER_BINDING = lazy { glVersion >= 330 }.value
var UNPACK_ROW_LENGTH = true



//class ImplBestGL: GLInterface {
//    private val internalImpl: GLInterface
//
//    init {
//        val caps = GL.getCapabilities()
//        internalImpl = when {
//            caps.OpenGL32 -> {
//                glslVersionString = 150
//                ImplGL3()
//            }
//            caps.OpenGL30 && Platform.get() != Platform.MACOSX -> {
//                glslVersionString = 130
//                ImplGL3()
//            }
//            caps.OpenGL20 -> {
//                glslVersionString = 110
//                if (Platform.get() == Platform.MACOSX) ImplGL2_mac() else ImplGL2()
//            }
//            else -> throw RuntimeException("OpenGL 2 is not present on this system!")
//        }
//    }
//
//    override fun shutdown() = internalImpl.shutdown()
//    override fun newFrame() = internalImpl.newFrame()
//    override fun renderDrawData(drawData: DrawData) = internalImpl.renderDrawData(drawData)
//    override fun createFontsTexture() = internalImpl.createFontsTexture()
//    override fun destroyFontsTexture() = internalImpl.destroyFontsTexture()
//    override fun createDeviceObjects() = internalImpl.createDeviceObjects()
//    override fun destroyDeviceObjects() = internalImpl.destroyDeviceObjects()
//}