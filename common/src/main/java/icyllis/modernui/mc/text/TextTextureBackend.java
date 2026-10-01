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

package icyllis.modernui.mc.text;

import icyllis.arc3d.core.ColorInfo;
import icyllis.arc3d.engine.Image;
import icyllis.arc3d.engine.Swizzle;
import icyllis.arc3d.opengl.GLDevice;
import icyllis.arc3d.opengl.GLTexture;
import icyllis.arc3d.vulkan.VulkanImage;
import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.core.Core;
import icyllis.modernui.mc.ModernUIMod;
import icyllis.modernui.mc.VulkanModIntegration;
import org.lwjgl.opengl.GL11;

import java.util.IdentityHashMap;

/**
 * Textures of the text engine: on OpenGL they are just GL textures, but on
 * VulkanMod they are Arc3D Vulkan images that must be wrapped into GL texture
 * ids first. This class hides that difference from the text engine.
 */
@RenderThread
public final class TextTextureBackend {

    /**
     * Arc3D image to GL texture id, only used on VulkanMod.
     */
    private static final IdentityHashMap<Image, Integer> sVulkanIds = new IdentityHashMap<>();

    /**
     * ONE,ONE,ONE,R — A8 masks are un-premultiplied.
     */
    private static final short SWIZZLE_A8 = Swizzle.make(5, 5, 5, 0);

    private TextTextureBackend() {
    }

    public static int getId(Image image, boolean alpha8) {
        if (image == null) {
            return 0;
        }
        if (image instanceof GLTexture texture) {
            return texture.getHandle();
        }
        if (!(image instanceof VulkanImage vulkanImage) || !ModernUIMod.isVulkanModLoaded()) {
            return 0;
        }
        Integer cached = sVulkanIds.get(image);
        if (cached != null) {
            return cached;
        }
        int id = VulkanModIntegration.bindArc3DImageToGlTextureId(
                vulkanImage,
                alpha8 ? SWIZZLE_A8 : Swizzle.RGBA
        );
        sVulkanIds.put(image, id);
        return id;
    }

    public static boolean writePixels(Image image,
                                      int x, int y, int width, int height,
                                      int colorType, int rowBytes, long pixels) {
        if (Core.requireImmediateContext().getDevice() instanceof GLDevice glDevice) {
            return glDevice.writePixels(image, x, y, width, height,
                    colorType, colorType, rowBytes, pixels);
        }
        boolean alpha8 = colorType == ColorInfo.CT_ALPHA_8;
        int id = getId(image, alpha8);
        if (id == 0) {
            return false;
        }
        int bytesPerPixel = ColorInfo.bytesPerPixel(colorType);
        int rowPixels = bytesPerPixel > 0 ? rowBytes / bytesPerPixel : width;
        return VulkanModIntegration.writePixels(id, x, y, width, height,
                alpha8 ? GL11.GL_RED : GL11.GL_RGBA, rowPixels, pixels);
    }

    public static boolean copyImage(Image src, Image dst, int width, int height, boolean alpha8) {
        if (Core.requireImmediateContext().getDevice() instanceof GLDevice glDevice) {
            return glDevice.copyImage(src, 0, 0, dst, 0, 0, width, height);
        }
        int srcId = getId(src, alpha8);
        int dstId = getId(dst, alpha8);
        if (srcId == 0 || dstId == 0) {
            return false;
        }
        return VulkanModIntegration.copyImage(srcId, dstId, width, height);
    }

    public static void generateMipmaps(Image image, boolean alpha8) {
        if (Core.requireImmediateContext().getDevice() instanceof GLDevice glDevice) {
            glDevice.generateMipmaps(image);
            return;
        }
        int id = getId(image, alpha8);
        if (id != 0) {
            VulkanModIntegration.generateMipmaps(id);
        }
    }

    public static void setFilter(Image image, boolean alpha8, boolean linear, boolean linearSampling) {
        int id = getId(image, alpha8);
        if (id == 0) {
            return;
        }
        boolean glBackend = Core.requireImmediateContext().getDevice() instanceof GLDevice;
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
        if (glBackend) {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER,
                    linear ? GL11.GL_LINEAR_MIPMAP_LINEAR : GL11.GL_NEAREST);
        } else {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER,
                    linearSampling ? GL11.GL_LINEAR : GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER,
                    linearSampling ? GL11.GL_LINEAR_MIPMAP_LINEAR : GL11.GL_NEAREST);
        }
    }

    /**
     * The backing Arc3D image is gone, drop the cached GL texture id.
     */
    public static void onTextureReplaced(Image oldTexture) {
        if (oldTexture != null) {
            sVulkanIds.remove(oldTexture);
        }
    }
}
