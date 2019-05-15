package imgui;

import glm_.mat4x4.Mat4;
import glm_.vec3.Vec3;
import org.lwjgl.bgfx.BGFXVertexDecl;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.bgfx.BGFX.*;

public class Cubes extends Demo {

    private static final Object[][] cubeVertices = {
            { -1.0f, 1.0f, 1.0f, 0xff000000 },
            { 1.0f, 1.0f, 1.0f, 0xff0000ff },
            { -1.0f, -1.0f, 1.0f, 0xff00ff00 },
            { 1.0f, -1.0f, 1.0f, 0xff00ffff },
            { -1.0f, 1.0f, -1.0f, 0xffff0000 },
            { 1.0f, 1.0f, -1.0f, 0xffff00ff },
            { -1.0f, -1.0f, -1.0f, 0xffffff00 },
            { 1.0f, -1.0f, -1.0f, 0xffffffff }
    };

    private static final int[] cubeIndices = {
            0, 1, 2, // 0
            1, 3, 2,
            4, 6, 5, // 2
            5, 6, 7,
            0, 2, 4, // 4
            4, 2, 6,
            1, 5, 3, // 6
            5, 7, 3,
            0, 4, 1, // 8
            4, 5, 1,
            2, 3, 6, // 10
            6, 3, 7
    };

    private BGFXVertexDecl decl;
    private ByteBuffer vertices;
    private short vbh;
    private ByteBuffer indices;
    private short ibh;
    private short program;

    private Mat4 view = new Mat4();
    private FloatBuffer viewBuf;
    private Mat4 proj = new Mat4();
    private FloatBuffer projBuf;
    private Mat4 model = new Mat4();
    private FloatBuffer modelBuf;

    public static void main(String[] args) {
        new Cubes().run(args);
    }

    private Cubes() {
        super("01-Cubes");
    }

    @Override
    protected void create() throws IOException {
        decl = BgfxDemoUtil.createVertexDecl(false, true, 0);

        vertices = MemoryUtil.memAlloc(8 * (3 * 4 + 4));

        vbh = BgfxDemoUtil.createVertexBuffer(vertices, decl, cubeVertices);

        indices = MemoryUtil.memAlloc(cubeIndices.length * 2);

        ibh = BgfxDemoUtil.createIndexBuffer(indices, cubeIndices);

        short vs = BgfxDemoUtil.loadShader("vs_cubes");
        short fs = BgfxDemoUtil.loadShader("fs_cubes");

        program = bgfx_create_program(vs, fs, true);

        viewBuf = MemoryUtil.memAllocFloat(16);
        projBuf = MemoryUtil.memAllocFloat(16);
        modelBuf = MemoryUtil.memAllocFloat(16);
    }

    @Override
    protected void frame(float time, float frameTime) {
        bgfx_dbg_text_printf(0, 1, 0x4f, "bgfx/examples/01-cubes");
        bgfx_dbg_text_printf(0, 2, 0x6f, "Description: Rendering simple static mesh.");
        bgfx_dbg_text_printf(0, 3, 0x0f, String.format("Frame: %7.3f[ms]", frameTime));

        BgfxDemoUtil.lookAt(new Vec3(0.0f, 0.0f, 0.0f), new Vec3(0.0f, 0.0f, -35.0f), view);
        BgfxDemoUtil.perspective(60.0f, getWindowWidth(), getWindowHeight(), 0.1f, 100.0f, proj);

        view.to(viewBuf);
        proj.to(projBuf);

        bgfx_set_view_transform(0, viewBuf, projBuf);

        long encoder = bgfx_encoder_begin(false);
        for (int yy = 0; yy < 11; ++yy) {
            for (int xx = 0; xx < 11; ++xx) {
                model
                        .identity()
                        .translateAssign(
                                -15.0f + xx * 3.0f,
                                -15.0f + yy * 3.0f,
                                0.0f)
                        .rotateXYZassign(
                                time + xx * 0.21f,
                                time + yy * 0.37f,
                                0.0f)
                ;

                model.to(modelBuf);

                bgfx_encoder_set_transform(encoder, modelBuf);

                bgfx_encoder_set_vertex_buffer(encoder, 0, vbh, 0, 8, BGFX_INVALID_HANDLE);
                bgfx_encoder_set_index_buffer(encoder, ibh, 0, 36);

                bgfx_encoder_set_state(encoder, BGFX_STATE_DEFAULT, 0);

                bgfx_encoder_submit(encoder, 0, program, 0, false);
            }
        }
        bgfx_encoder_end(encoder);
    }

    @Override
    protected void dispose() {
        MemoryUtil.memFree(viewBuf);
        MemoryUtil.memFree(projBuf);
        MemoryUtil.memFree(modelBuf);

        bgfx_destroy_program(program);

        bgfx_destroy_index_buffer(ibh);
        MemoryUtil.memFree(indices);

        bgfx_destroy_vertex_buffer(vbh);
        MemoryUtil.memFree(vertices);

        decl.free();
    }

}
