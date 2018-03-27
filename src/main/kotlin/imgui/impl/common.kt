package imgui.impl

import glm_.BYTES
import glm_.mat4x4.Mat4
import uno.buffer.bufferBig
import uno.buffer.intBufferBig
import uno.buffer.intBufferOf

val vertexShader
    get() = """

        #version $glslVersion

        uniform mat4 mat;

        in vec2 Position;
        in vec2 UV;
        in vec4 Color;

        out vec2 uv;
        out vec4 color;

        void main()
        {
            uv = UV;
            color = Color;
            gl_Position = mat * vec4(Position.xy, 0, 1);
        }
        """

val fragmentShader
    get() = """

        #version $glslVersion

        uniform sampler2D Texture;

        in vec2 uv;
        in vec4 color;

        out vec4 outColor;

        void main()
        {
            outColor = color * texture(Texture, uv);
        }
        """

var glslVersion = 150


val mouseJustPressed = BooleanArray(3)

enum class Buffer { Vertex, Element;

    companion object {
        val MAX = values().size
    }
}

val bufferName = intBufferBig<Buffer>()

/*  JVM. We are not yet doing this because no user case. If ever needed: https://github.com/ocornut/imgui/compare/a1a36e762eac707cc3f9c81ec5af7150f6620c4c...d7f97922b883aec0c873e0e405c46b154d382120

        ~Recreate the VAO every time (This is to easily allow multiple GL contexts. VAO are not shared among GL contexts, and we don't track
        creation/deletion of windows so we don't have an obvious key to use to cache them.)~     */
val vaoName = intBufferBig(1)

val fontTexture = intBufferOf(-1)

val mat = Mat4()


var vtxSize = 1 shl 5 // 32768
var idxSize = 1 shl 6 // 65536
var vtxBuffer = bufferBig(vtxSize)
var idxBuffer = intBufferBig(idxSize / Int.BYTES)