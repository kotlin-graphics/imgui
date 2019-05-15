package imgui;

import org.lwjgl.bgfx.*;
import org.lwjgl.glfw.*;
import org.lwjgl.system.*;
import org.lwjgl.system.libc.LibCStdio;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.lwjgl.bgfx.BGFX.*;
import static org.lwjgl.bgfx.BGFXPlatform.bgfx_set_platform_data;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.APIUtil.apiLog;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.system.Pointer.*;
import static org.lwjgl.system.libc.LibCString.nmemmove;

/**
 * Abstract base class for all bgfx examples.
 */
abstract class Demo {

    private String title;
    private int format;

    protected int renderer = BGFX_RENDERER_TYPE_COUNT;
    private short pciId = BGFX_PCI_ID_NONE;
    private int width = 1280;
    private int height = 720;
    private int debug = BGFX_DEBUG_TEXT;
    private int reset = BGFX_RESET_VSYNC;

    private boolean useCallbacks;
    private boolean useCustomAllocator;

    protected Demo(String title) {
        this.title = title;
    }

    protected abstract void create() throws IOException;

    protected abstract void frame(float time, float frameTime);

    protected abstract void dispose();

    protected void run(String[] args) {

        parseArgs(args);

        try (MemoryStack stack = MemoryStack.stackPush()) {

            /* Initialize GLFW, create window, pass window handle to bgfx platform data */

            if (!glfwInit()) {
                throw new RuntimeException("Error initializing GLFW");
            }

            // the client (renderer) API is managed by bgfx
            glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);

            long window = glfwCreateWindow(width, height, title, 0, 0);

            if (window == 0) {
                throw new RuntimeException("Error creating GLFW window");
            }

            glfwSetFramebufferSizeCallback(window, this::resize);

            glfwSetKeyCallback(window, (windowHnd, key, scancode, action, mods) -> {
                if (action != GLFW_RELEASE) {
                    return;
                }

                switch (key) {
                    case GLFW_KEY_ESCAPE:
                        glfwSetWindowShouldClose(windowHnd, true);
                        break;
                }
            });

            BGFXPlatformData platformData = BGFXPlatformData.callocStack(stack);

            switch (Platform.get()) {
                case LINUX:
                    platformData.ndt(GLFWNativeX11.glfwGetX11Display());
                    platformData.nwh(GLFWNativeX11.glfwGetX11Window(window));
                    break;
                case MACOSX:
                    platformData.ndt(NULL);
                    platformData.nwh(GLFWNativeCocoa.glfwGetCocoaWindow(window));
                    break;
                case WINDOWS:
                    platformData.ndt(NULL);
                    platformData.nwh(GLFWNativeWin32.glfwGetWin32Window(window));
                    break;
            }

            platformData.context(NULL);
            platformData.backBuffer(NULL);
            platformData.backBufferDS(NULL);

            bgfx_set_platform_data(platformData);

            BgfxDemoUtil.reportSupportedRenderers();

            /* Initialize bgfx */

            BGFXInit init = BGFXInit.mallocStack(stack);
            bgfx_init_ctor(init);
            init
                    .type(renderer)
                    .vendorId(pciId)
                    .deviceId((short)0)
                    .callback(useCallbacks ? createCallbacks(stack) : null)
                    .allocator(useCustomAllocator ? createAllocator(stack) : null)
                    .resolution(it -> it
                            .width(width)
                            .height(height)
                            .reset(reset));

            if (!bgfx_init(init)) {
                throw new RuntimeException("Error initializing bgfx renderer");
            }

            format = init.resolution().format();

            if (renderer == BGFX_RENDERER_TYPE_COUNT) {
                renderer = bgfx_get_renderer_type();
            }

            String rendererName = bgfx_get_renderer_name(renderer);
            if ("NULL".equals(rendererName)) {
                throw new RuntimeException("Error identifying bgfx renderer");
            }

            apiLog("bgfx: using renderer '" + rendererName + "'");

            BgfxDemoUtil.configure(renderer);

            bgfx_set_debug(debug);

            bgfx_set_view_clear(0,
                    BGFX_CLEAR_COLOR | BGFX_CLEAR_DEPTH,
                    0x303030ff,
                    1.0f,
                    0);

            /* Initialize demo specific code */

            try {
                create();
            } catch (IOException e) {
                throw new RuntimeException("Error initializing bgfx demo", e);
            }

            /* Application loop */

            long lastTime;
            long startTime = lastTime = glfwGetTimerValue();

            while (!glfwWindowShouldClose(window)) {

                glfwPollEvents();

                long now = glfwGetTimerValue();
                long frameTime = now - lastTime;
                lastTime = now;

                double freq = glfwGetTimerFrequency();
                double toMs = 1000.0 / freq;

                double time = (now - startTime) / freq;

                bgfx_set_view_rect(0, 0, 0, width, height);

                bgfx_dbg_text_clear(0, false);

                frame((float) time, (float) (frameTime * toMs));

                bgfx_frame(false);
            }

            /* Shutdown */

            dispose();

            bgfx_shutdown();

            if (useCallbacks) {
                freeCallbacks(init);
            }
            if (useCustomAllocator) {
                freeAllocator(init);
            }
            BgfxDemoUtil.dispose();

            glfwFreeCallbacks(window);
            glfwDestroyWindow(window);
            glfwTerminate();
        }
    }

    private void parseArgs(String[] args) {
        if (hasArg(args, "gl")) {
            renderer = BGFX_RENDERER_TYPE_OPENGL;
        } else if (hasArg(args, "vk")) {
            renderer = BGFX_RENDERER_TYPE_VULKAN;
        } else if (hasArg(args, "noop")) {
            renderer = BGFX_RENDERER_TYPE_NOOP;
        } else if (Platform.get() == Platform.WINDOWS) {
            if (hasArg(args, "d3d9")) {
                renderer = BGFX_RENDERER_TYPE_DIRECT3D9;
            } else if (hasArg(args, "d3d11")) {
                renderer = BGFX_RENDERER_TYPE_DIRECT3D11;
            } else if (hasArg(args, "d3d12")) {
                renderer = BGFX_RENDERER_TYPE_DIRECT3D12;
            }
        } else if (Platform.get() == Platform.MACOSX) {
            if (hasArg(args, "mtl")) {
                renderer = BGFX_RENDERER_TYPE_METAL;
            }
        }

        if (hasArg(args, "amd")) {
            pciId = BGFX_PCI_ID_AMD;
        } else if (hasArg(args, "nvidia")) {
            pciId = BGFX_PCI_ID_NVIDIA;
        } else if (hasArg(args, "intel")) {
            pciId = BGFX_PCI_ID_INTEL;
        } else if (hasArg(args, "sw")) {
            pciId = BGFX_PCI_ID_SOFTWARE_RASTERIZER;
        }

        useCallbacks = hasArg(args, "cb");
        useCustomAllocator = hasArg(args, "alloc");
    }

    private static boolean hasArg(String[] args, String arg) {
        String expandArg = "--" + arg;
        for (String a : args) {
            if (expandArg.equalsIgnoreCase(a)) {
                return true;
            }
        }
        return false;
    }

    private void resize(long window, int width, int height) {
        this.width = width;
        this.height = height;
        bgfx_reset(width, height, reset, format);
    }

    protected int getWindowWidth() {
        return width;
    }

    protected int getWindowHeight() {
        return height;
    }

    private static BGFXCallbackInterface createCallbacks(MemoryStack stack) {
        return BGFXCallbackInterface.callocStack(stack)
                .vtbl(BGFXCallbackVtbl.callocStack(stack)
                        .fatal((_this, _filePath, _line, _code, _str) -> {
                            if (_code == BGFX_FATAL_DEBUG_CHECK) {
                                System.out.println("BREAK"); // set debugger breakpoint
                            } else {
                                throw new RuntimeException("Fatal error " + _code + ": " + memASCII(_str));
                            }
                        })
                        .trace_vargs((_this, _filePath, _line, _format, _argList) -> {
                            try (MemoryStack frame = MemoryStack.stackPush()) {
                                String filePath = (_filePath != NULL) ? memUTF8(_filePath) : "[n/a]";

                                ByteBuffer buffer = frame.malloc(128); // arbitary size to store formatted message
                                int length = LibCStdio.nvsnprintf(memAddress(buffer), buffer.remaining(), _format, _argList);

                                if (length > 0) {
                                    String message = memASCII(buffer, length - 1); // bgfx log messages are terminated with the newline character
                                    apiLog("bgfx: [" + filePath + " (" + _line + ")] - " + message);
                                } else {
                                    apiLog("bgfx: [" + filePath + " (" + _line + ")] - error: unable to format output: " + memASCII(_format));
                                }
                            }
                        })
                        .profiler_begin((_this, _name, _abgr, _filePath, _line) -> {

                        })
                        .profiler_begin_literal((_this, _name, _abgr, _filePath, _line) -> {

                        })
                        .profiler_end(_this -> {

                        })
                        .cache_read_size((_this, _id) -> 0)
                        .cache_read((_this, _id, _data, _size) -> false)
                        .cache_write((_this, _id, _data, _size) -> {

                        })
                        .screen_shot((_this, _filePath, _width, _height, _pitch, _data, _size, _yflip) -> {

                        })
                        .capture_begin((_this, _width, _height, _pitch, _format, _yflip) -> {

                        })
                        .capture_end(_this -> {

                        })
                        .capture_frame((_this, _data, _size) -> {

                        })
                );
    }

    private static void freeCallbacks(BGFXInit init) {
        long base = init.callback().vtbl().address();
        for (int i = BGFXCallbackVtbl.FATAL; i < BGFXCallbackVtbl.SIZEOF; i += POINTER_SIZE) {
            Callback.free(memGetAddress(base + i));
        }
    }

    private static long NATURAL_ALIGNMENT = 8L;
    private static BGFXAllocatorInterface createAllocator(MemoryStack stack) {
        return BGFXAllocatorInterface.callocStack(stack)
                .vtbl(BGFXAllocatorVtbl.callocStack(stack)
                        .realloc((_this, _ptr, _size, _align, _file, _line) -> {
                            long ptr;
                            if (_size == 0) {
                                if (_ptr != NULL) {
                                    if (_align <= NATURAL_ALIGNMENT) {
                                        nmemFree(_ptr);
                                    } else {
                                        alignedFree(_ptr);
                                    }
                                    apiLog("bgfx: freed memory at address " + Long.toHexString(_ptr));
                                }
                                ptr = NULL;
                            } else if (_ptr == NULL) {
                                ptr = _align <= NATURAL_ALIGNMENT
                                        ? nmemAlloc(_size)
                                        : alignedAlloc(_size, _align);
                                apiLog("bgfx: allocated " + _size + " [" + _align + "] bytes at address " + Long.toHexString(ptr));
                            } else {
                                ptr = _align <= NATURAL_ALIGNMENT
                                        ? nmemRealloc(_ptr, _size)
                                        : alignedRealloc(_ptr, _size, _align);
                                apiLog("bgfx: reallocated address " + Long.toHexString(_ptr) + " with " + _size + " [" + _align + "] bytes at address " + Long.toHexString(ptr));
                            }
                            return ptr;
                        }));
    }

    private static long align(long pointer, long alignment) {
        return (pointer + (alignment - 1)) & -alignment;
    }

    private static long alignedAlloc(long size, long alignment) {
        long ptr     = nmemAlloc(size + alignment);
        long aligned = align(ptr + Integer.BYTES, alignment);

        memPutInt(aligned - Integer.BYTES, (int)(aligned - ptr));
        return aligned;
    }

    private static void alignedFree(long pointer) {
        nmemFree(pointer - memGetInt(pointer - Integer.BYTES));
    }

    private static long alignedRealloc(long pointer, long size, long alignment) {
        int offset = memGetInt(pointer - Integer.BYTES);

        long ptr     = nmemRealloc(pointer - offset, size + alignment);
        long aligned = align(ptr + Integer.BYTES, alignment);
        if (aligned == pointer) {
            return pointer;
        }

        nmemmove(aligned, ptr + offset, size);
        memPutInt(aligned - Integer.BYTES, (int)(aligned - ptr));
        return aligned;
    }

    private static void freeAllocator(BGFXInit init) {
        BGFXAllocatorVtbl vtbl = init.allocator().vtbl();
        vtbl.realloc().free();
    }

}
