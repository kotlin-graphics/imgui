package imgui.impl

import com.jogamp.opengl.GL2ES3
import com.jogamp.opengl.GL3
import glm_.BYTES
import glm_.L
import glm_.mat4x4.Mat4
import gln.GlBufferEnum
import gln.glf.semantic
import gln.objects.GlTexture
import kool.ByteBuffer
import kool.IntBuffer
import kool.adr
import kool.lib.toByteArray
import kool.use
import org.lwjgl.system.Platform
import java.nio.ByteBuffer
import java.nio.IntBuffer
import kotlin.reflect.KMutableProperty0


var USE_GL_ES3 = false

/** Store GLSL version string so we can refer to it later in case we recreate shaders.
 * Note: GLSL version is NOT the same as GL version. Leave this to default if unsure. */
var glslVersion = if (Platform.get() == Platform.MACOSX) 150 else 130

val vertexShaderInWord: String
    get() = if(glslVersion > 120) "in" else "attribute"

val vertexShaderOutWord: String
    get() = if(glslVersion > 120) "out" else "varying"

val fragmentShaderInWord: String
    get() = if(glslVersion > 120) "in" else "varying"

val vertexShader: String
    get() = """

        #version ${if (USE_GL_ES3) "300 es" else "$glslVersion"}

        uniform mat4 mat;

        $vertexShaderInWord vec2 Position;
        $vertexShaderInWord vec2 UV;
        $vertexShaderInWord vec4 Color;

        $vertexShaderOutWord vec2 uv;
        $vertexShaderOutWord vec4 color;

        void main()
        {
            uv = UV;
            color = Color;
            gl_Position = mat * vec4(Position.xy, 0, 1);
        }
        """

val fragmentShader: String
    get() = """

        #version ${if (USE_GL_ES3) "300 es" else "$glslVersion"}

        uniform sampler2D Texture;

        $fragmentShaderInWord vec2 uv;
        $fragmentShaderInWord vec4 color;

        ${if(glslVersion > 120) "out vec4 outColor;" else ""}

        void main()
        {
            ${if(glslVersion > 120) "outColor" else "gl_FragColor"} = color * texture${if(glslVersion <= 120) "2D" else ""}(Texture, uv);
        }
        """


val mouseJustPressed = BooleanArray(5)

enum class Buffer : GlBufferEnum { Vertex, Element;

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