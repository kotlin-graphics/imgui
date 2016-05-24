/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.opengl.GLWindow;
import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER_BINDING;
import static com.jogamp.opengl.GL.GL_TEXTURE_BINDING_2D;
import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;
import static com.jogamp.opengl.GL2ES3.GL_VERTEX_ARRAY_BINDING;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import java.nio.IntBuffer;

/**
 *
 * @author GBarbieri
 */
public class ImpNewt implements MouseListener, KeyListener {

    private static final String SHADERS_ROOT = "test/imgui/shaders/", SHADERS_SRC = "shader";

    // Data
    private static GLWindow window = null;
    private static long time = 0;
    private static boolean[] mousePressed = new boolean[3];
    private static float mouseWheel;
    private static int programName;

    private interface Attribute {

        public static final int TEX = 0;
        public static final int PROJ = 1;
        public static final int MAX = 2;
    }

    private static int[] location = new int[Attribute.MAX];

    public static boolean init(GLWindow window) {

        ImpNewt.window = window;

        imgui.IO io = ImGui.getIO();

        // Keyboard mapping. ImGui will use those indices to peek into the io.KeyDown[] array.
        io.keyMap[Key.Tab] = KeyEvent.VK_TAB;
        io.keyMap[Key.LeftArrow] = KeyEvent.VK_LEFT;
        io.keyMap[Key.RightArrow] = KeyEvent.VK_RIGHT;
        io.keyMap[Key.UpArrow] = KeyEvent.VK_UP;
        io.keyMap[Key.DownArrow] = KeyEvent.VK_DOWN;
        io.keyMap[Key.PageUp] = KeyEvent.VK_PAGE_UP;
        io.keyMap[Key.PageDown] = KeyEvent.VK_PAGE_DOWN;
        io.keyMap[Key.Home] = KeyEvent.VK_HOME;
        io.keyMap[Key.End] = KeyEvent.VK_END;
        io.keyMap[Key.Delete] = KeyEvent.VK_DELETE;
        io.keyMap[Key.Backspace] = KeyEvent.VK_BACK_SPACE;
        io.keyMap[Key.Enter] = KeyEvent.VK_ENTER;
        io.keyMap[Key.Escape] = KeyEvent.VK_ESCAPE;
        io.keyMap[Key.A] = KeyEvent.VK_A;
        io.keyMap[Key.C] = KeyEvent.VK_C;
        io.keyMap[Key.V] = KeyEvent.VK_V;
        io.keyMap[Key.X] = KeyEvent.VK_X;
        io.keyMap[Key.Y] = KeyEvent.VK_Y;
        io.keyMap[Key.Z] = KeyEvent.VK_Z;
    }

    private static IntBuffer fontTexture = GLBuffers.newDirectIntBuffer(1);

    public static void newFrame(GL3 gl3) {

        if (fontTexture.get(0) == 0) {
            createDeviceObjects(gl3);
        }

        IO io = ImGui.getIO();

        // Setup display size (every frame to accommodate for window resizing)
        io.displaySize.set(window.getWidth(), window.getHeight());

        // Setup time step
        long currentTime = System.nanoTime();
        io.deltaTime = (float) (time > 0.0 ? ((double) (currentTime - time) / 1_000_000_000) : 1.0f / 60.0f);
        time = currentTime;
    }

    private static void createDeviceObjects(GL3 gl3) {

        // Backup GL state
        IntBuffer lastTexture = GLBuffers.newDirectIntBuffer(1), lastArrayBuffer = GLBuffers.newDirectIntBuffer(1),
                lastVertexArray = GLBuffers.newDirectIntBuffer(1);
        gl3.glGetIntegerv(GL_TEXTURE_BINDING_2D, lastTexture);
        gl3.glGetIntegerv(GL_ARRAY_BUFFER_BINDING, lastArrayBuffer);
        gl3.glGetIntegerv(GL_VERTEX_ARRAY_BINDING, lastVertexArray);

    }

    private static void initProgram(GL3 gl3) {

        ShaderCode vertShader = ShaderCode.create(gl3, GL_VERTEX_SHADER, ImpNewt.class, SHADERS_ROOT, null, SHADERS_SRC,
                "vert", null, true);
        ShaderCode fragShader = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, ImpNewt.class, SHADERS_ROOT, null, SHADERS_SRC,
                "frag", null, true);

        ShaderProgram shaderProgram = new ShaderProgram();
        shaderProgram.add(vertShader);
        shaderProgram.add(fragShader);

        shaderProgram.init(gl3);

        programName = shaderProgram.program();

        shaderProgram.link(gl3, System.out);

        vertShader.destroy(gl3);
        fragShader.destroy(gl3);

        location[Attribute.TEX] = gl3.glGetUniformLocation(programName, "texture_");
        location[Attribute.PROJ] = gl3.glGetUniformLocation(programName, "proj");

        gl3.glUseProgram(program);
        {
            /**
             * We bind the uniform texture0UL to the Texture Image Units zero
             * or, in other words, Semantic.Uniform.TEXTURE0.
             */
            gl3.glUniform1i(texture0UL, Semantic.Sampler.DIFFUSE);
        }
        gl3.glUseProgram(0);
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() >= MouseEvent.BUTTON1 && e.getButton() <= MouseEvent.BUTTON3) {
            mousePressed[e.getButton() - 1] = true;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {
        mouseWheel += e.getRotation()[1];
    }

    @Override
    public void keyPressed(KeyEvent e) {

        imgui.IO io = ImGui.getIO();

        io.keysDown[e.getKeyCode()] = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {

        imgui.IO io = ImGui.getIO();

        io.keysDown[e.getKeyCode()] = false;
    }
}
