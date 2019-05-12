package imgui.impl

import com.jogamp.opengl.GL2ES3
import com.jogamp.opengl.GL3
import glm_.BYTES
import glm_.L
import glm_.mat4x4.Mat4
import gln.GlBufferEnum
import gln.ShaderType.Companion.FRAGMENT_SHADER
import gln.ShaderType.Companion.VERTEX_SHADER
import gln.buffer.GlBufferDsl
import gln.glf.semantic
import gln.objects.GlBuffers
import gln.objects.GlProgram
import gln.objects.GlShader
import gln.objects.GlTexture
import gln.vertexArray.GlVertexArray
import imgui.MouseCursor
import kool.ByteBuffer
import kool.IntBuffer
import kool.adr
import kool.lib.toByteArray
import kool.use
import org.lwjgl.opengl.GL15C
import org.lwjgl.opengl.GL30C
import org.lwjgl.system.Platform
import java.nio.ByteBuffer
import java.nio.IntBuffer
import kotlin.reflect.KMutableProperty0


/** Store GLSL version string so we can refer to it later in case we recreate shaders.
 * Note: GLSL version is NOT the same as GL version. Leave this to default if unsure. */
var glslVersion = if (Platform.get() == Platform.MACOSX) 150 else 130

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


enum class Buffer : GlBufferEnum {
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

val mat = Mat4()

var vtxSize = 1 shl 5 // 32768
var idxSize = 1 shl 6 // 65536
var vtxBuffer = ByteBuffer(vtxSize)
var idxBuffer = IntBuffer(idxSize / Int.BYTES)


class JoglProgram(gl: GL3, vert: String, frag: String) {

    val name = gl.glCreateProgram()

    init {
        with(gl) {

            val v = shaderFromSource(vert, GL2ES3.GL_VERTEX_SHADER)
            val f = shaderFromSource(frag, GL2ES3.GL_FRAGMENT_SHADER)

            glAttachShader(name, v)
            glAttachShader(name, f)

            glBindAttribLocation(name, semantic.attr.POSITION, "Position")
            glBindAttribLocation(name, semantic.attr.TEX_COORD, "UV")
            glBindAttribLocation(name, semantic.attr.COLOR, "Color")
            glBindFragDataLocation(name, semantic.frag.COLOR, "outColor")

            glLinkProgram(name)

            IntBuffer(1).use { i ->
                glGetProgramiv(name, GL2ES3.GL_LINK_STATUS, i)
                if (i[0] == GL2ES3.GL_FALSE) {
                    ByteBuffer(100).use {
                        glGetProgramInfoLog(name, 100, i, it)
                        throw Error(String(it.toByteArray()))
                    }
                }
            }
            glDetachShader(name, v)
            glDetachShader(name, f)
            glDeleteShader(v)
            glDeleteShader(f)

            glUseProgram(name)
            glUniform1i(glGetUniformLocation(name, "Texture"), semantic.sampler.DIFFUSE)
            glUseProgram(0)
        }
    }

    val mat = gl.glGetUniformLocation(name, "mat")

    fun GL3.shaderFromSource(source: String, type: Int): Int = IntBuffer(1).use {
        val shader = glCreateShader(type)
        glShaderSource(shader, 1, arrayOf(source), null)

        glCompileShader(shader)

        glGetShaderiv(shader, GL2ES3.GL_COMPILE_STATUS, it)
        if (it[0] == GL2ES3.GL_FALSE)
            throw Error()

        shader
    }
}


fun GlTexture.Companion.gen(texture: KMutableProperty0<GlTexture>): GlTexture {
    texture.set(gen())
    return texture()
}

fun GlVertexArray.Companion.gen(vao: KMutableProperty0<GlVertexArray>): GlVertexArray {
    vao.set(gen())
    return vao()
}

fun GlBuffers.delete() = GL30C.glDeleteBuffers(names)
fun GlBufferDsl.subData(offset: Int, size: Int, data: ByteBuffer) = GL15C.nglBufferSubData(target.i, offset.L, size.L, data.adr)
fun GlBufferDsl.subData(offset: Int, size: Int, data: IntBuffer) = GL15C.nglBufferSubData(target.i, offset.L, size.L, data.adr)


var clientApi = GlfwClientApi.Unknown
var time = 0.0
val mouseJustPressed = BooleanArray(5)
val mouseCursors = LongArray(MouseCursor.COUNT)

enum class GlfwClientApi { Unknown, OpenGL, Vulkan }
