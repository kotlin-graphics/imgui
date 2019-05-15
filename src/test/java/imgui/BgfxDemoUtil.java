package imgui;

import glm_.detail.GlmDepthClipSpace;
import glm_.glm;
import glm_.mat4x4.Mat4;
import glm_.vec3.Vec3;
import org.lwjgl.bgfx.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;

import static org.lwjgl.bgfx.BGFX.*;
import static glm_.detail.SetupKt.setGLM_DEPTH_CLIP_SPACE;
import static org.lwjgl.system.APIUtil.apiLog;
import static org.lwjgl.system.MemoryUtil.*;

@SuppressWarnings("StaticNonFinalField")
final class BgfxDemoUtil {

    private static int renderer = -1;

    private static BGFXReleaseFunctionCallback releaseMemoryCb = BGFXReleaseFunctionCallback.create((_ptr, _userData) -> nmemFree(_ptr));

    private BgfxDemoUtil() {

    }

    static void configure(int renderer) {
        BgfxDemoUtil.renderer = renderer;
        if (bgfx_get_caps().homogeneousDepth())
            setGLM_DEPTH_CLIP_SPACE(GlmDepthClipSpace.NEGATIVE_ONE_TO_ONE);
        else
            setGLM_DEPTH_CLIP_SPACE(GlmDepthClipSpace.ZERO_TO_ONE);
    }

    static void dispose() {
        releaseMemoryCb.free();
    }

    static BGFXVertexDecl createVertexDecl(boolean withNormals, boolean withColor, int numUVs) {

        BGFXVertexDecl decl = BGFXVertexDecl.calloc();

        bgfx_vertex_decl_begin(decl, renderer);

        bgfx_vertex_decl_add(decl,
                BGFX_ATTRIB_POSITION,
                3,
                BGFX_ATTRIB_TYPE_FLOAT,
                false,
                false);

        if (withNormals) {
            bgfx_vertex_decl_add(decl,
                    BGFX_ATTRIB_NORMAL,
                    3,
                    BGFX_ATTRIB_TYPE_FLOAT,
                    false,
                    false);
        }

        if (withColor) {
            bgfx_vertex_decl_add(decl,
                    BGFX_ATTRIB_COLOR0,
                    4,
                    BGFX_ATTRIB_TYPE_UINT8,
                    true,
                    false);
        }

        if (numUVs > 0) {
            bgfx_vertex_decl_add(decl,
                    BGFX_ATTRIB_TEXCOORD0,
                    2,
                    BGFX_ATTRIB_TYPE_FLOAT,
                    false,
                    false);
        }

        bgfx_vertex_decl_end(decl);

        return decl;
    }

    static short createVertexBuffer(ByteBuffer buffer, BGFXVertexDecl decl, Object[][] vertices) {

        for (Object[] vtx : vertices) {
            for (Object attr : vtx) {
                if (attr instanceof Float) {
                    buffer.putFloat((float) attr);
                } else if (attr instanceof Integer) {
                    buffer.putInt((int) attr);
                } else {
                    throw new RuntimeException("Invalid parameter type");
                }
            }
        }

        if (buffer.remaining() != 0) {
            throw new RuntimeException("ByteBuffer size and number of arguments do not match");
        }

        buffer.flip();

        return createVertexBuffer(buffer, decl);
    }

    static short createVertexBuffer(ByteBuffer buffer, BGFXVertexDecl decl) {

        BGFXMemory vbhMem = bgfx_make_ref(buffer);

        return bgfx_create_vertex_buffer(vbhMem, decl, BGFX_BUFFER_NONE);
    }

    static short createIndexBuffer(ByteBuffer buffer, int[] indices) {

        for (int idx : indices) {
            buffer.putShort((short) idx);
        }

        if (buffer.remaining() != 0) {
            throw new RuntimeException("ByteBuffer size and number of arguments do not match");
        }

        buffer.flip();

        BGFXMemory ibhMem = bgfx_make_ref(buffer);

        return bgfx_create_index_buffer(ibhMem, BGFX_BUFFER_NONE);
    }

    private static ByteBuffer loadResource(String resourcePath, String name) throws IOException {

        URL url = BgfxDemoUtil.class.getResource(resourcePath + name);

        if (url == null) {
            throw new IOException("Resource not found: " + resourcePath + "/" + name);
        }

        int resourceSize = url.openConnection().getContentLength();

        apiLog("bgfx: loading resource '" + url.getFile() + "' (" + resourceSize + " bytes)");

        ByteBuffer resource = memAlloc(resourceSize);

        try (BufferedInputStream bis = new BufferedInputStream(url.openStream())) {
            int b;
            do {
                b = bis.read();
                if (b != -1) {
                    resource.put((byte) b);
                }
            } while (b != -1);
        }

        resource.flip();

        return resource;
    }

    static short loadShader(String name) throws IOException {

        String resourcePath = "/bgfx/shaders/";

        switch (renderer) {

            case BGFX_RENDERER_TYPE_DIRECT3D11:
            case BGFX_RENDERER_TYPE_DIRECT3D12:
                resourcePath += "dx11/";
                break;

            case BGFX_RENDERER_TYPE_DIRECT3D9:
                resourcePath += "dx9/";
                break;

            case BGFX_RENDERER_TYPE_OPENGL:
                resourcePath += "glsl/";
                break;

            case BGFX_RENDERER_TYPE_METAL:
                resourcePath += "metal/";
                break;

            default:
                throw new IOException("No demo shaders supported for " + bgfx_get_renderer_name(renderer) + " renderer");
        }

        ByteBuffer shaderCode = loadResource(resourcePath, name + ".bin");

        return bgfx_create_shader(bgfx_make_ref_release(shaderCode, releaseMemoryCb, NULL));
    }

    static short loadShader(char[] shaderCodeGLSL, char[] shaderCodeD3D9, char[] shaderCodeD3D11, char[] shaderCodeMtl) throws IOException {
        char[] sc;

        switch (renderer) {

            case BGFX_RENDERER_TYPE_DIRECT3D11:
            case BGFX_RENDERER_TYPE_DIRECT3D12:
                sc = shaderCodeD3D11;
                break;

            case BGFX_RENDERER_TYPE_DIRECT3D9:
                sc = shaderCodeD3D9;
                break;

            case BGFX_RENDERER_TYPE_OPENGL:
                sc = shaderCodeGLSL;
                break;

            case BGFX_RENDERER_TYPE_METAL:
                sc = shaderCodeMtl;
                break;

            default:
                throw new IOException("No demo shaders supported for " + bgfx_get_renderer_name(renderer) + " renderer");
        }

        ByteBuffer shaderCode = memAlloc(sc.length);

        for (char c : sc) {
            shaderCode.put((byte) c);
        }

        shaderCode.flip();

        return bgfx_create_shader(bgfx_make_ref_release(shaderCode, releaseMemoryCb, NULL));
    }

    static short loadTexture(String fileName) throws IOException {

        ByteBuffer textureData = loadResource("/org/lwjgl/demo/bgfx/textures/", fileName);

        BGFXMemory textureMemory = bgfx_make_ref_release(textureData, releaseMemoryCb, NULL);

        return bgfx_create_texture(textureMemory, BGFX_TEXTURE_NONE, 0, null);
    }

    static void reportSupportedRenderers() {
        int[] rendererTypes = new int[BGFX_RENDERER_TYPE_COUNT];
        int count = bgfx_get_supported_renderers(rendererTypes);

        apiLog("bgfx: renderers supported");

        for (int i = 0; i < count; i++) {
            apiLog("    " + bgfx_get_renderer_name(rendererTypes[i]));
        }
    }

    static void lookAt(Vec3 at, Vec3 eye, Mat4 dest) {
        glm.INSTANCE.lookAtLh(dest, eye, at, new Vec3(0, 1, 0));
    }

    static void perspective(float fov, int width, int height, float near, float far, Mat4 dest) {
        float fovRadians = fov * (float) Math.PI / 180.0f;
        float aspect = width / (float) height;
        glm.INSTANCE.perspectiveLh(dest, fovRadians, aspect, near, far);
    }

    static void ortho(float left, float right, float bottom, float top, float zNear, float zFar, Mat4 dest) {
        glm.INSTANCE.orthoLh(dest, left, right, bottom, top, zNear, zFar);
    }

}
