///*
// * Copyright LWJGL. All rights reserved.
// * License terms: https://www.lwjgl.org/license
// */
//
//import org.lwjgl.assimp.*;
//import org.lwjgl.demo.opengl.util.DemoUtils;
//import org.lwjgl.system.MemoryStack;
//import org.lwjgl.vulkan.*;
//
//import java.io.IOException;
//import java.nio.ByteBuffer;
//import java.nio.LongBuffer;
//
//import static org.lwjgl.BufferUtils.createByteBuffer;
//import static org.lwjgl.assimp.Assimp.*;
//import static org.lwjgl.demo.opengl.util.DemoUtils.*;
//import static org.lwjgl.demo.vulkan.VKFactory.*;
//import static org.lwjgl.system.MemoryStack.stackPush;
//import static org.lwjgl.system.MemoryUtil.NULL;
//import static org.lwjgl.util.shaderc.Shaderc.*;
//import static org.lwjgl.vulkan.EXTDebugReport.VK_ERROR_VALIDATION_FAILED_EXT;
//import static org.lwjgl.vulkan.KHRDisplaySwapchain.VK_ERROR_INCOMPATIBLE_DISPLAY_KHR;
//import static org.lwjgl.vulkan.KHRSurface.VK_ERROR_NATIVE_WINDOW_IN_USE_KHR;
//import static org.lwjgl.vulkan.KHRSurface.VK_ERROR_SURFACE_LOST_KHR;
//import static org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR;
//import static org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR;
//import static org.lwjgl.vulkan.NVRayTracing.*;
//import static org.lwjgl.vulkan.VK10.*;
//
//public class VKUtil {
//
//    public static final int VK_FLAGS_NONE = 0;
//
//    private static int vulkanStageToShadercKind(int stage) {
//        switch (stage) {
//            case VK_SHADER_STAGE_VERTEX_BIT:
//                return shaderc_vertex_shader;
//            case VK_SHADER_STAGE_FRAGMENT_BIT:
//                return shaderc_fragment_shader;
//            case VK_SHADER_STAGE_RAYGEN_BIT_NV:
//                return shaderc_raygen_shader;
//            case VK_SHADER_STAGE_CLOSEST_HIT_BIT_NV:
//                return shaderc_closesthit_shader;
//            case VK_SHADER_STAGE_MISS_BIT_NV:
//                return shaderc_miss_shader;
//            case VK_SHADER_STAGE_ANY_HIT_BIT_NV:
//                return shaderc_anyhit_shader;
//            default:
//                throw new IllegalArgumentException("Stage: " + stage);
//        }
//    }
//
//    public static ByteBuffer glslToSpirv(String classPath, int vulkanStage) throws IOException {
//        ByteBuffer src = DemoUtils.ioResourceToByteBuffer(classPath, 1024);
//        long compiler = shaderc_compiler_initialize();
//        long res;
//        try (MemoryStack stack = MemoryStack.stackPush()) {
//            res = shaderc_compile_into_spv(compiler, src, vulkanStageToShadercKind(vulkanStage),
//                    stack.UTF8(classPath), stack.UTF8("main"), 0L);
//            if (res == 0L)
//                throw new AssertionError("Internal error during compilation!");
//        }
//        if (shaderc_result_get_compilation_status(res) != shaderc_compilation_status_success) {
//            throw new AssertionError("Shader compilation failed: " + shaderc_result_get_error_message(res));
//        }
//        int size = (int) shaderc_result_get_length(res);
//        ByteBuffer resultBytes = createByteBuffer(size);
//        resultBytes.put(shaderc_result_get_bytes(res));
//        resultBytes.flip();
//        shaderc_compiler_release(res);
//        shaderc_compiler_release(compiler);
//        return resultBytes;
//    }
//
//    public static void _CHECK_(int ret, String msg) {
//        if (ret != VK_SUCCESS)
//            throw new AssertionError(msg + ": " + translateVulkanResult(ret));
//    }
//
//
//    public static void transitionImageLayout(VkCommandBuffer cmdbuffer, long image,
//                                             int oldImageLayout, int newImageLayout, int srcStageMask, int dstStageMask) {
//        transitionImageLayout(cmdbuffer, image, oldImageLayout, newImageLayout, srcStageMask, dstStageMask, 1);
//    }
//    public static void transitionImageLayout(VkCommandBuffer cmdbuffer, long image,
//                                             int oldImageLayout, int newImageLayout, int srcStageMask, int dstStageMask, int layerCount) {
//        try (MemoryStack stack = stackPush()) {
//            VkImageMemoryBarrier.Buffer imageMemoryBarrier = VkImageMemoryBarrier(stack)
//                    .oldLayout(oldImageLayout)
//                    .newLayout(newImageLayout)
//                    .image(image)
//                    .subresourceRange(r -> {
//                        r.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
//                                .layerCount(layerCount)
//                                .levelCount(1);
//                    });
//            vkCmdPipelineBarrier(cmdbuffer, srcStageMask, dstStageMask,0,
//                    null,null, imageMemoryBarrier);
//        }
//    }
//
//    public static void loadShader(VkPipelineShaderStageCreateInfo info, MemoryStack stack, VkDevice device,
//                                  String classPath, int stage) throws IOException {
//        ByteBuffer shaderCode = glslToSpirv(classPath, stage);
//        VkShaderModuleCreateInfo moduleCreateInfo = VkShaderModuleCreateInfo(stack)
//                .pNext(NULL)
//                .pCode(shaderCode)
//                .flags(0);
//        LongBuffer pShaderModule = stack.mallocLong(1);
//        _CHECK_(vkCreateShaderModule(device, moduleCreateInfo, null, pShaderModule),
//                "Failed to create shader module");
//        info.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
//                .stage(stage)
//                .module(pShaderModule.get(0)).pName(stack.UTF8("main"));
//    }
//
//    /**
//     * Translates a Vulkan {@code VkResult} value to a String describing the result.
//     *
//     * @param result
//     *            the {@code VkResult} value
//     *
//     * @return the result description
//     */
//    public static String translateVulkanResult(int result) {
//        switch (result) {
//            // Success codes
//            case VK_SUCCESS:
//                return "Command successfully completed.";
//            case VK_NOT_READY:
//                return "A fence or query has not yet completed.";
//            case VK_TIMEOUT:
//                return "A wait operation has not completed in the specified time.";
//            case VK_EVENT_SET:
//                return "An event is signaled.";
//            case VK_EVENT_RESET:
//                return "An event is unsignaled.";
//            case VK_INCOMPLETE:
//                return "A return array was too small for the result.";
//            case VK_SUBOPTIMAL_KHR:
//                return "A swapchain no longer matches the surface properties exactly, but can still be used to present to the surface successfully.";
//
//            // Error codes
//            case VK_ERROR_OUT_OF_HOST_MEMORY:
//                return "A host memory allocation has failed.";
//            case VK_ERROR_OUT_OF_DEVICE_MEMORY:
//                return "A device memory allocation has failed.";
//            case VK_ERROR_INITIALIZATION_FAILED:
//                return "Initialization of an object could not be completed for implementation-specific reasons.";
//            case VK_ERROR_DEVICE_LOST:
//                return "The logical or physical device has been lost.";
//            case VK_ERROR_MEMORY_MAP_FAILED:
//                return "Mapping of a memory object has failed.";
//            case VK_ERROR_LAYER_NOT_PRESENT:
//                return "A requested layer is not present or could not be loaded.";
//            case VK_ERROR_EXTENSION_NOT_PRESENT:
//                return "A requested extension is not supported.";
//            case VK_ERROR_FEATURE_NOT_PRESENT:
//                return "A requested feature is not supported.";
//            case VK_ERROR_INCOMPATIBLE_DRIVER:
//                return "The requested version of Vulkan is not supported by the driver or is otherwise incompatible for implementation-specific reasons.";
//            case VK_ERROR_TOO_MANY_OBJECTS:
//                return "Too many objects of the type have already been created.";
//            case VK_ERROR_FORMAT_NOT_SUPPORTED:
//                return "A requested format is not supported on this device.";
//            case VK_ERROR_SURFACE_LOST_KHR:
//                return "A surface is no longer available.";
//            case VK_ERROR_NATIVE_WINDOW_IN_USE_KHR:
//                return "The requested window is already connected to a VkSurfaceKHR, or to some other non-Vulkan API.";
//            case VK_ERROR_OUT_OF_DATE_KHR:
//                return "A surface has changed in such a way that it is no longer compatible with the swapchain, and further presentation requests using the "
//                        + "swapchain will fail. Applications must query the new surface properties and recreate their swapchain if they wish to continue" + "presenting to the surface.";
//            case VK_ERROR_INCOMPATIBLE_DISPLAY_KHR:
//                return "The display used by a swapchain does not use the same presentable image layout, or is incompatible in a way that prevents sharing an" + " image.";
//            case VK_ERROR_VALIDATION_FAILED_EXT:
//                return "A validation layer found an error.";
//            default:
//                return String.format("%s [%d]", "Unknown", Integer.valueOf(result));
//        }
//    }
//
//    public static String format_to_string(int format) {
//        switch (format) {
//            case 0: return "UNDEFINED";
//            case 1: return "R4G4_UNORM_PACK8";
//            case 2: return "R4G4B4A4_UNORM_PACK16";
//            case 3: return "B4G4R4A4_UNORM_PACK16";
//            case 4: return "R5G6B5_UNORM_PACK16";
//            case 5: return "B5G6R5_UNORM_PACK16";
//            case 6: return "R5G5B5A1_UNORM_PACK16";
//            case 7: return "B5G5R5A1_UNORM_PACK16";
//            case 8: return "A1R5G5B5_UNORM_PACK16";
//            case 9: return "R8_UNORM";
//            case 10: return "R8_SNORM";
//            case 11: return "R8_USCALED";
//            case 12: return "R8_SSCALED";
//            case 13: return "R8_UINT";
//            case 14: return "R8_SINT";
//            case 15: return "R8_SRGB";
//            case 16: return "R8G8_UNORM";
//            case 17: return "R8G8_SNORM";
//            case 18: return "R8G8_USCALED";
//            case 19: return "R8G8_SSCALED";
//            case 20: return "R8G8_UINT";
//            case 21: return "R8G8_SINT";
//            case 22: return "R8G8_SRGB";
//            case 23: return "R8G8B8_UNORM";
//            case 24: return "R8G8B8_SNORM";
//            case 25: return "R8G8B8_USCALED";
//            case 26: return "R8G8B8_SSCALED";
//            case 27: return "R8G8B8_UINT";
//            case 28: return "R8G8B8_SINT";
//            case 29: return "R8G8B8_SRGB";
//            case 30: return "B8G8R8_UNORM";
//            case 31: return "B8G8R8_SNORM";
//            case 32: return "B8G8R8_USCALED";
//            case 33: return "B8G8R8_SSCALED";
//            case 34: return "B8G8R8_UINT";
//            case 35: return "B8G8R8_SINT";
//            case 36: return "B8G8R8_SRGB";
//            case 37: return "R8G8B8A8_UNORM";
//            case 38: return "R8G8B8A8_SNORM";
//            case 39: return "R8G8B8A8_USCALED";
//            case 40: return "R8G8B8A8_SSCALED";
//            case 41: return "R8G8B8A8_UINT";
//            case 42: return "R8G8B8A8_SINT";
//            case 43: return "R8G8B8A8_SRGB";
//            case 44: return "B8G8R8A8_UNORM";
//            case 45: return "B8G8R8A8_SNORM";
//            case 46: return "B8G8R8A8_USCALED";
//            case 47: return "B8G8R8A8_SSCALED";
//            case 48: return "B8G8R8A8_UINT";
//            case 49: return "B8G8R8A8_SINT";
//            case 50: return "B8G8R8A8_SRGB";
//            case 51: return "A8B8G8R8_UNORM_PACK32";
//            case 52: return "A8B8G8R8_SNORM_PACK32";
//            case 53: return "A8B8G8R8_USCALED_PACK32";
//            case 54: return "A8B8G8R8_SSCALED_PACK32";
//            case 55: return "A8B8G8R8_UINT_PACK32";
//            case 56: return "A8B8G8R8_SINT_PACK32";
//            case 57: return "A8B8G8R8_SRGB_PACK32";
//            case 58: return "A2R10G10B10_UNORM_PACK32";
//            case 59: return "A2R10G10B10_SNORM_PACK32";
//            case 60: return "A2R10G10B10_USCALED_PACK32";
//            case 61: return "A2R10G10B10_SSCALED_PACK32";
//            case 62: return "A2R10G10B10_UINT_PACK32";
//            case 63: return "A2R10G10B10_SINT_PACK32";
//            case 64: return "A2B10G10R10_UNORM_PACK32";
//            case 65: return "A2B10G10R10_SNORM_PACK32";
//            case 66: return "A2B10G10R10_USCALED_PACK32";
//            case 67: return "A2B10G10R10_SSCALED_PACK32";
//            case 68: return "A2B10G10R10_UINT_PACK32";
//            case 69: return "A2B10G10R10_SINT_PACK32";
//            case 70: return "R16_UNORM";
//            case 71: return "R16_SNORM";
//            case 72: return "R16_USCALED";
//            case 73: return "R16_SSCALED";
//            case 74: return "R16_UINT";
//            case 75: return "R16_SINT";
//            case 76: return "R16_SFLOAT";
//            case 77: return "R16G16_UNORM";
//            case 78: return "R16G16_SNORM";
//            case 79: return "R16G16_USCALED";
//            case 80: return "R16G16_SSCALED";
//            case 81: return "R16G16_UINT";
//            case 82: return "R16G16_SINT";
//            case 83: return "R16G16_SFLOAT";
//            case 84: return "R16G16B16_UNORM";
//            case 85: return "R16G16B16_SNORM";
//            case 86: return "R16G16B16_USCALED";
//            case 87: return "R16G16B16_SSCALED";
//            case 88: return "R16G16B16_UINT";
//            case 89: return "R16G16B16_SINT";
//            case 90: return "R16G16B16_SFLOAT";
//            case 91: return "R16G16B16A16_UNORM";
//            case 92: return "R16G16B16A16_SNORM";
//            case 93: return "R16G16B16A16_USCALED";
//            case 94: return "R16G16B16A16_SSCALED";
//            case 95: return "R16G16B16A16_UINT";
//            case 96: return "R16G16B16A16_SINT";
//            case 97: return "R16G16B16A16_SFLOAT";
//            case 98: return "R32_UINT";
//            case 99: return "R32_SINT";
//            case 100: return "R32_SFLOAT";
//            case 101: return "R32G32_UINT";
//            case 102: return "R32G32_SINT";
//            case 103: return "R32G32_SFLOAT";
//            case 104: return "R32G32B32_UINT";
//            case 105: return "R32G32B32_SINT";
//            case 106: return "R32G32B32_SFLOAT";
//            case 107: return "R32G32B32A32_UINT";
//            case 108: return "R32G32B32A32_SINT";
//            case 109: return "R32G32B32A32_SFLOAT";
//            case 110: return "R64_UINT";
//            case 111: return "R64_SINT";
//            case 112: return "R64_SFLOAT";
//            case 113: return "R64G64_UINT";
//            case 114: return "R64G64_SINT";
//            case 115: return "R64G64_SFLOAT";
//            case 116: return "R64G64B64_UINT";
//            case 117: return "R64G64B64_SINT";
//            case 118: return "R64G64B64_SFLOAT";
//            case 119: return "R64G64B64A64_UINT";
//            case 120: return "R64G64B64A64_SINT";
//            case 121: return "R64G64B64A64_SFLOAT";
//            case 122: return "B10G11R11_UFLOAT_PACK32";
//            case 123: return "E5B9G9R9_UFLOAT_PACK32";
//            case 124: return "D16_UNORM";
//            case 125: return "X8_D24_UNORM_PACK32";
//            case 126: return "D32_SFLOAT";
//            case 127: return "S8_UINT";
//            case 128: return "D16_UNORM_S8_UINT";
//            case 129: return "D24_UNORM_S8_UINT";
//            case 130: return "D32_SFLOAT_S8_UINT";
//            case 131: return "BC1_RGB_UNORM_BLOCK";
//            case 132: return "BC1_RGB_SRGB_BLOCK";
//            case 133: return "BC1_RGBA_UNORM_BLOCK";
//            case 134: return "BC1_RGBA_SRGB_BLOCK";
//            case 135: return "BC2_UNORM_BLOCK";
//            case 136: return "BC2_SRGB_BLOCK";
//            case 137: return "BC3_UNORM_BLOCK";
//            case 138: return "BC3_SRGB_BLOCK";
//            case 139: return "BC4_UNORM_BLOCK";
//            case 140: return "BC4_SNORM_BLOCK";
//            case 141: return "BC5_UNORM_BLOCK";
//            case 142: return "BC5_SNORM_BLOCK";
//            case 143: return "BC6H_UFLOAT_BLOCK";
//            case 144: return "BC6H_SFLOAT_BLOCK";
//            case 145: return "BC7_UNORM_BLOCK";
//            case 146: return "BC7_SRGB_BLOCK";
//            case 147: return "ETC2_R8G8B8_UNORM_BLOCK";
//            case 148: return "ETC2_R8G8B8_SRGB_BLOCK";
//            case 149: return "ETC2_R8G8B8A1_UNORM_BLOCK";
//            case 150: return "ETC2_R8G8B8A1_SRGB_BLOCK";
//            case 151: return "ETC2_R8G8B8A8_UNORM_BLOCK";
//            case 152: return "ETC2_R8G8B8A8_SRGB_BLOCK";
//            case 153: return "EAC_R11_UNORM_BLOCK";
//            case 154: return "EAC_R11_SNORM_BLOCK";
//            case 155: return "EAC_R11G11_UNORM_BLOCK";
//            case 156: return "EAC_R11G11_SNORM_BLOCK";
//            case 157: return "ASTC_4x4_UNORM_BLOCK";
//            case 158: return "ASTC_4x4_SRGB_BLOCK";
//            case 159: return "ASTC_5x4_UNORM_BLOCK";
//            case 160: return "ASTC_5x4_SRGB_BLOCK";
//            case 161: return "ASTC_5x5_UNORM_BLOCK";
//            case 162: return "ASTC_5x5_SRGB_BLOCK";
//            case 163: return "ASTC_6x5_UNORM_BLOCK";
//            case 164: return "ASTC_6x5_SRGB_BLOCK";
//            case 165: return "ASTC_6x6_UNORM_BLOCK";
//            case 166: return "ASTC_6x6_SRGB_BLOCK";
//            case 167: return "ASTC_8x5_UNORM_BLOCK";
//            case 168: return "ASTC_8x5_SRGB_BLOCK";
//            case 169: return "ASTC_8x6_UNORM_BLOCK";
//            case 170: return "ASTC_8x6_SRGB_BLOCK";
//            case 171: return "ASTC_8x8_UNORM_BLOCK";
//            case 172: return "ASTC_8x8_SRGB_BLOCK";
//            case 173: return "ASTC_10x5_UNORM_BLOCK";
//            case 174: return "ASTC_10x5_SRGB_BLOCK";
//            case 175: return "ASTC_10x6_UNORM_BLOCK";
//            case 176: return "ASTC_10x6_SRGB_BLOCK";
//            case 177: return "ASTC_10x8_UNORM_BLOCK";
//            case 178: return "ASTC_10x8_SRGB_BLOCK";
//            case 179: return "ASTC_10x10_UNORM_BLOCK";
//            case 180: return "ASTC_10x10_SRGB_BLOCK";
//            case 181: return "ASTC_12x10_UNORM_BLOCK";
//            case 182: return "ASTC_12x10_SRGB_BLOCK";
//            case 183: return "ASTC_12x12_UNORM_BLOCK";
//            case 184: return "ASTC_12x12_SRGB_BLOCK";
//            case 1000156000: return "G8B8G8R8_422_UNORM";
//            case 1000156001: return "B8G8R8G8_422_UNORM";
//            case 1000156002: return "G8_B8_R8_3PLANE_420_UNORM";
//            case 1000156003: return "G8_B8R8_2PLANE_420_UNORM";
//            case 1000156004: return "G8_B8_R8_3PLANE_422_UNORM";
//            case 1000156005: return "G8_B8R8_2PLANE_422_UNORM";
//            case 1000156006: return "G8_B8_R8_3PLANE_444_UNORM";
//            case 1000156007: return "R10X6_UNORM_PACK16";
//            case 1000156008: return "R10X6G10X6_UNORM_2PACK16";
//            case 1000156009: return "R10X6G10X6B10X6A10X6_UNORM_4PACK16";
//            case 1000156010: return "G10X6B10X6G10X6R10X6_422_UNORM_4PACK16";
//            case 1000156011: return "B10X6G10X6R10X6G10X6_422_UNORM_4PACK16";
//            case 1000156012: return "G10X6_B10X6_R10X6_3PLANE_420_UNORM_3PACK16";
//            case 1000156013: return "G10X6_B10X6R10X6_2PLANE_420_UNORM_3PACK16";
//            case 1000156014: return "G10X6_B10X6_R10X6_3PLANE_422_UNORM_3PACK16";
//            case 1000156015: return "G10X6_B10X6R10X6_2PLANE_422_UNORM_3PACK16";
//            case 1000156016: return "G10X6_B10X6_R10X6_3PLANE_444_UNORM_3PACK16";
//            case 1000156017: return "R12X4_UNORM_PACK16";
//            case 1000156018: return "R12X4G12X4_UNORM_2PACK16";
//            case 1000156019: return "R12X4G12X4B12X4A12X4_UNORM_4PACK16";
//            case 1000156020: return "G12X4B12X4G12X4R12X4_422_UNORM_4PACK16";
//            case 1000156021: return "B12X4G12X4R12X4G12X4_422_UNORM_4PACK16";
//            case 1000156022: return "G12X4_B12X4_R12X4_3PLANE_420_UNORM_3PACK16";
//            case 1000156023: return "G12X4_B12X4R12X4_2PLANE_420_UNORM_3PACK16";
//            case 1000156024: return "G12X4_B12X4_R12X4_3PLANE_422_UNORM_3PACK16";
//            case 1000156025: return "G12X4_B12X4R12X4_2PLANE_422_UNORM_3PACK16";
//            case 1000156026: return "G12X4_B12X4_R12X4_3PLANE_444_UNORM_3PACK16";
//            case 1000156027: return "G16B16G16R16_422_UNORM";
//            case 1000156028: return "B16G16R16G16_422_UNORM";
//            case 1000156029: return "G16_B16_R16_3PLANE_420_UNORM";
//            case 1000156030: return "G16_B16R16_2PLANE_420_UNORM";
//            case 1000156031: return "G16_B16_R16_3PLANE_422_UNORM";
//            case 1000156032: return "G16_B16R16_2PLANE_422_UNORM";
//            case 1000156033: return "G16_B16_R16_3PLANE_444_UNORM";
//            case 1000054000: return "PVRTC1_2BPP_UNORM_BLOCK_IMG";
//            case 1000054001: return "PVRTC1_4BPP_UNORM_BLOCK_IMG";
//            case 1000054002: return "PVRTC2_2BPP_UNORM_BLOCK_IMG";
//            case 1000054003: return "PVRTC2_4BPP_UNORM_BLOCK_IMG";
//            case 1000054004: return "PVRTC1_2BPP_SRGB_BLOCK_IMG";
//            case 1000054005: return "PVRTC1_4BPP_SRGB_BLOCK_IMG";
//            case 1000054006: return "PVRTC2_2BPP_SRGB_BLOCK_IMG";
//            case 1000054007: return "PVRTC2_4BPP_SRGB_BLOCK_IMG";
//            default: return "";
//        }
//    }
//}
