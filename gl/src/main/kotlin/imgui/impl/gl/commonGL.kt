package imgui.impl.gl

import glm_.L
import gln.ShaderType.Companion.FRAGMENT_SHADER
import gln.ShaderType.Companion.VERTEX_SHADER
import gln.buffer.GlBufferDsl
import gln.glf.semantic
import gln.identifiers.GlBuffers
import gln.identifiers.GlProgram
import gln.identifiers.GlShader
import kool.IntBuffer
import kool.adr
import org.lwjgl.opengl.GL15C
import org.lwjgl.opengl.GL30C
import org.lwjgl.system.Platform
import java.nio.ByteBuffer
import java.nio.IntBuffer


/** Store GLSL version string so we can refer to it later in case we recreate shaders.
 * Note: GLSL version is NOT the same as GL version. Leave this to default if unsure. */
var glslVersion = if (Platform.get() == Platform.MACOSX) 150 else 130

/** Extracted at runtime using GL_MAJOR_VERSION, GL_MINOR_VERSION queries (e.g. 320 for GL 3.2) */
var glVersion = 0

val vertexShader_glsl_120: String by lazy {
    """
    #version $glslVersion
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
    #version $glslVersion
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

val vertexShader_glsl_410_core: String by lazy {
    """
    #version $glslVersion
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
    #version $glslVersion
    uniform sampler2D Texture;
    varying vec2 Frag_UV;
    varying vec4 Frag_Color;
    void main() {
        gl_FragColor = Frag_Color * texture2D(Texture, Frag_UV.st);
    }"""
}

val fragmentShader_glsl_130: String by lazy {
    """
    #version $glslVersion
    uniform sampler2D Texture;
    in vec2 Frag_UV;
    in vec4 Frag_Color;
    out vec4 Out_Color;
    void main() {
        Out_Color = Frag_Color * texture(Texture, Frag_UV.st);
    }"""
}

val fragmentShader_glsl_410_core: String by lazy {
    """
    #version $glslVersion
    in vec2 Frag_UV;
    in vec4 Frag_Color;
    uniform sampler2D Texture;
    layout (location = ${semantic.frag.COLOR}) out vec4 Out_Color;
    void main() {
        Out_Color = Frag_Color * texture(Texture, Frag_UV.st);
    }"""
}

fun createProgram(): GlProgram {

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