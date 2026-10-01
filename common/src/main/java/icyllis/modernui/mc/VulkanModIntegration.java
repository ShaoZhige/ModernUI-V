/*
 * Modern UI - V.
 * Copyright (C) 2019-2024 BloCamLimb. All rights reserved.
 * Modifications Copyright (C) 2026 Shao_Zhige. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.mc;

import icyllis.arc3d.core.RawPtr;
import icyllis.arc3d.engine.Swizzle;
import icyllis.arc3d.vulkan.VKUtil;
import icyllis.arc3d.vulkan.VulkanBackendContext;
import icyllis.arc3d.vulkan.VulkanImage;
import icyllis.arc3d.vulkan.VulkanImageDesc;
import icyllis.arc3d.vulkan.VulkanImageView;
import icyllis.arc3d.vulkan.VulkanMemoryAllocator;
import icyllis.modernui.core.VulkanManager;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.gl.VkGlTexture;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.queue.CommandPool;
import net.vulkanmod.vulkan.queue.GraphicsQueue;
import net.vulkanmod.vulkan.queue.Queue;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkImageBlit;
import org.lwjgl.vulkan.VkImageCopy;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkOffset3D;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures2;

import javax.annotation.Nonnull;
import java.util.Objects;

import org.lwjgl.opengl.GL11;

/**
 * Bridges Arc3D's Vulkan backend with VulkanMod.
 * <p>
 * VulkanMod replaces Minecraft's OpenGL device entirely, so Arc3D cannot create
 * its own {@code VkDevice}; it has to borrow VulkanMod's one. Arc3D images are
 * also not GL textures, they are wrapped into VulkanMod's {@code VkGlTexture}
 * handles so that vanilla/other mods' rendering code can still bind them.
 * <p>
 * Every method here must be called on the render thread, and only when
 * {@link ModernUIMod#isVulkanModLoaded()} is true.
 */
public final class VulkanModIntegration {

    private VulkanModIntegration() {
    }

    /**
     * Builds an Arc3D backend context that shares VulkanMod's device.
     */
    public static VulkanBackendContext wrapContext() {
        VulkanBackendContext backendContext = new VulkanBackendContext();
        VulkanManager vulkanManager = VulkanManager.get();
        backendContext.mInstance = DeviceManager.physicalDevice.getInstance();
        backendContext.mPhysicalDevice = DeviceManager.physicalDevice;
        backendContext.mDevice = DeviceManager.vkDevice;
        Queue.QueueFamilyIndices indices = Queue.getQueueFamilies();
        backendContext.mGraphicsQueueIndex = indices.graphicsFamily;
        vulkanManager.setPhysicalDeviceFeatures2(VkPhysicalDeviceFeatures2.calloc());
        backendContext.mDeviceFeatures2 = vulkanManager.getPhysicalDeviceFeatures2();
        vulkanManager.setMemoryAllocator(
                VulkanMemoryAllocator.make(
                        backendContext.mInstance,
                        backendContext.mPhysicalDevice,
                        backendContext.mDevice,
                        DeviceManager.deviceProperties.apiVersion(),
                        0L
                )
        );
        backendContext.mMemoryAllocator = vulkanManager.getMemoryAllocator();
        backendContext.mQueue = DeviceManager.getGraphicsQueue().vkQueue();
        return backendContext;
    }

    public static int bindArc3DImageToGlTextureId(@RawPtr VulkanImage arc3dVulkanImage) {
        return bindArc3DImageToGlTextureId(arc3dVulkanImage, Swizzle.RGBA);
    }

    /**
     * Wraps an Arc3D Vulkan image into a GL texture id that VulkanMod understands.
     *
     * @param swizzle the swizzle of the image view, A8 masks use {@code ONE,ONE,ONE,R}
     */
    public static int bindArc3DImageToGlTextureId(@RawPtr VulkanImage arc3dVulkanImage,
                                                  short swizzle) {
        VulkanImageDesc desc = arc3dVulkanImage.getVulkanDesc();
        VulkanImageView view = Objects.requireNonNull(
                arc3dVulkanImage.findOrCreateTextureView(swizzle), "texture view");
        net.vulkanmod.vulkan.texture.VulkanImage texture =
                new net.vulkanmod.vulkan.texture.VulkanImage(
                        arc3dVulkanImage.getLabel(),
                        arc3dVulkanImage.vkImage(),
                        desc.mVkFormat,
                        desc.getMipLevelCount(),
                        desc.getWidth(),
                        desc.getHeight(),
                        VKUtil.vkFormatBytesPerBlock(desc.mVkFormat),
                        desc.mImageUsageFlags,
                        view.vkImageView()
                );
        texture.setCurrentLayout(arc3dVulkanImage.getVulkanMutableState().getImageLayout());
        int id = VkGlTexture.genTextureId();
        VkGlTexture.bindIdToImage(id, texture);
        return id;
    }

    public static void syncImageLayoutToVulkanMod(int glTextureId, @RawPtr VulkanImage arc3dVulkanImage) {
        VkGlTexture texture = VkGlTexture.getTexture(glTextureId);
        if (texture != null && texture.getVulkanImage() != null) {
            texture.getVulkanImage().setCurrentLayout(
                    arc3dVulkanImage.getVulkanMutableState().getImageLayout());
        }
    }

    public static void syncImageLayoutFromVulkanMod(int glTextureId, @RawPtr VulkanImage arc3dVulkanImage) {
        VkGlTexture texture = VkGlTexture.getTexture(glTextureId);
        if (texture != null && texture.getVulkanImage() != null) {
            arc3dVulkanImage.getVulkanMutableState().setImageLayout(
                    texture.getVulkanImage().getCurrentLayout());
        }
    }

    public static void addFrameOp(Runnable runnable) {
        MemoryManager.getInstance().addFrameOp(runnable);
    }

    /**
     * VulkanMod compiles shader pipelines lazily, a shader without an initialized
     * pipeline cannot be used and would render nothing.
     */
    public static boolean isShaderUsable(@Nonnull ShaderInstance shader) {
        if (!ModernUIMod.isVulkanModLoaded()) {
            return true;
        }
        return isShaderUsableOnVulkanMod(shader);
    }

    private static boolean isShaderUsableOnVulkanMod(@Nonnull ShaderInstance shader) {
        if (shader instanceof ShaderMixed mixed) {
            return mixed.getPipeline() != null;
        }
        return true;
    }

    public static boolean writePixels(int glTextureId,
                                      int x, int y, int width, int height,
                                      int glFormat, int rowPixels, long pixels) {
        if (VkGlTexture.getTexture(glTextureId) == null) {
            return false;
        }
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, rowPixels);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, glTextureId);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, x, y, width, height,
                glFormat, GL11.GL_UNSIGNED_BYTE, pixels);
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        return true;
    }

    public static boolean copyImage(int srcGlTextureId, int dstGlTextureId, int width, int height) {
        VkGlTexture src = VkGlTexture.getTexture(srcGlTextureId);
        VkGlTexture dst = VkGlTexture.getTexture(dstGlTextureId);
        if (src == null || dst == null) {
            return false;
        }
        net.vulkanmod.vulkan.texture.VulkanImage srcImage = src.getVulkanImage();
        net.vulkanmod.vulkan.texture.VulkanImage dstImage = dst.getVulkanImage();
        if (srcImage == null || dstImage == null) {
            return false;
        }
        GraphicsQueue queue = DeviceManager.getGraphicsQueue();
        CommandPool.CommandBuffer cb = queue.beginCommands();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            srcImage.transitionImageLayout(stack, cb.getHandle(), VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
            dstImage.transitionImageLayout(stack, cb.getHandle(), VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
            VkImageCopy.Buffer region = VkImageCopy.calloc(1, stack);
            region.srcSubresource().aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
                    .mipLevel(0).baseArrayLayer(0).layerCount(1);
            region.dstSubresource().aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
                    .mipLevel(0).baseArrayLayer(0).layerCount(1);
            region.extent().set(width, height, 1);
            VK10.vkCmdCopyImage(
                    cb.getHandle(),
                    srcImage.getId(), VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                    dstImage.getId(), VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    region
            );
            srcImage.transitionImageLayout(stack, cb.getHandle(), VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            dstImage.transitionImageLayout(stack, cb.getHandle(), VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        }
        queue.submitCommands(cb);
        return true;
    }

    public static void generateMipmaps(int glTextureId) {
        VkGlTexture texture = VkGlTexture.getTexture(glTextureId);
        if (texture == null) {
            return;
        }
        net.vulkanmod.vulkan.texture.VulkanImage image = texture.getVulkanImage();
        if (image == null || image.mipLevels <= 1) {
            return;
        }
        recordMipmapChain(image);
    }

    private static void recordMipmapChain(net.vulkanmod.vulkan.texture.VulkanImage image) {
        GraphicsQueue queue = DeviceManager.getGraphicsQueue();
        CommandPool.CommandBuffer cb = queue.beginCommands();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var handle = cb.getHandle();
            image.transitionImageLayout(stack, handle, VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);

            for (int level = 1; level < image.mipLevels; level++) {
                int srcLevel = level - 1;
                VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack)
                        .sType$Default()
                        // VulkanMod 的纹理常驻在 COLOR_ATTACHMENT / GENERAL 布局上，
                        // 屏障的 oldLayout 必须按它实际的布局来写，不是 TRANSFER_*。
                        // VulkanMod keeps its textures in COLOR_ATTACHMENT/GENERAL, so the barrier
                        // must declare that as the old layout rather than a TRANSFER_* layout.
                        .oldLayout(VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                        .newLayout(VK10.VK_IMAGE_LAYOUT_GENERAL)
                        .srcQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                        .dstQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                        .image(image.getId());
                barrier.subresourceRange()
                        .baseMipLevel(srcLevel).levelCount(1)
                        .baseArrayLayer(0).layerCount(VK10.VK_REMAINING_ARRAY_LAYERS)
                        .aspectMask(image.aspect);
                barrier.srcAccessMask(VK10.VK_ACCESS_TRANSFER_WRITE_BIT);
                barrier.dstAccessMask(VK10.VK_ACCESS_TRANSFER_READ_BIT);
                VK10.vkCmdPipelineBarrier(handle,
                        VK10.VK_PIPELINE_STAGE_TRANSFER_BIT,
                        VK10.VK_PIPELINE_STAGE_TRANSFER_BIT,
                        0, null, null, barrier);

                VkImageBlit.Buffer blit = VkImageBlit.calloc(1, stack);
                blit.srcOffsets(0, VkOffset3D.calloc(stack).set(0, 0, 0));
                blit.srcOffsets(1, VkOffset3D.calloc(stack)
                        .set(image.width >> srcLevel, image.height >> srcLevel, 1));
                blit.srcSubresource().aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
                        .mipLevel(srcLevel).baseArrayLayer(0).layerCount(1);
                blit.dstOffsets(0, VkOffset3D.calloc(stack).set(0, 0, 0));
                blit.dstOffsets(1, VkOffset3D.calloc(stack)
                        .set(image.width >> level, image.height >> level, 1));
                blit.dstSubresource().aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
                        .mipLevel(level).baseArrayLayer(0).layerCount(1);
                VK10.vkCmdBlitImage(handle,
                        image.getId(), VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                        image.getId(), VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                        blit, VK10.VK_FILTER_LINEAR);
            }

            VkImageMemoryBarrier.Buffer finalBarrier = VkImageMemoryBarrier.calloc(1, stack)
                    .sType$Default()
                    .oldLayout(VK10.VK_IMAGE_LAYOUT_GENERAL)
                    .newLayout(VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                    .srcQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                    .image(image.getId());
            finalBarrier.subresourceRange()
                    .baseMipLevel(0).levelCount(image.mipLevels - 1)
                    .baseArrayLayer(0).layerCount(VK10.VK_REMAINING_ARRAY_LAYERS)
                    .aspectMask(image.aspect);
            finalBarrier.srcAccessMask(VK10.VK_ACCESS_TRANSFER_WRITE_BIT);
            finalBarrier.dstAccessMask(VK10.VK_ACCESS_SHADER_READ_BIT);
            VK10.vkCmdPipelineBarrier(handle,
                    VK10.VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VK10.VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
                    0, null, null, finalBarrier);
            finalBarrier.oldLayout(VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            finalBarrier.subresourceRange()
                    .baseMipLevel(image.mipLevels - 1).levelCount(1);
            VK10.vkCmdPipelineBarrier(handle,
                    VK10.VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VK10.VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
                    0, null, null, finalBarrier);
            image.setCurrentLayout(VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        }
        queue.submitCommands(cb);
    }

    public static void setTextureFilter(int glTextureId, int glMagFilter, int glMinFilter) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, glTextureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, glMagFilter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, glMinFilter);
    }
}
