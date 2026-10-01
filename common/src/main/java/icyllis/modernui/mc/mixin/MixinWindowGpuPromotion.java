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

package icyllis.modernui.mc.mixin;

import com.mojang.blaze3d.platform.Window;
import icyllis.modernui.mc.ModernUIClient;
import icyllis.modernui.mc.ModernUIMod;
import icyllis.modernui.mc.MuiModApi;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.system.Platform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Promotes the OpenGL context to the highest supported 4.x core profile.
 * <p>
 * This used to live in {@link MixinWindow}, it is split out so that
 * {@link icyllis.modernui.mc.MixinConfigPlugin} can skip it when VulkanMod is
 * loaded — VulkanMod has no OpenGL context to promote.
 */
@Mixin(Window.class)
public abstract class MixinWindowGpuPromotion {

    @Redirect(method = "<init>",
            at = @At(value = "INVOKE",
                    target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V",
                    ordinal = 5),
            remap = false
    )
    private void onInit(int x, int y) {
        if (MuiModApi.get().isGLVersionPromoted()) {
            return;
        }
        if (Platform.get() == Platform.MACOSX ||
                Boolean.parseBoolean(ModernUIClient.getBootstrapProperty(
                        ModernUIClient.BOOTSTRAP_SKIP_GL_VERSION_PROMOTION))) {
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
            if (Platform.get() == Platform.MACOSX) {
                GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
                GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 1);
            } else {
                GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
                GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
            }
        } else {
            GLFWErrorCallback callback = GLFW.glfwSetErrorCallback(null);
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
            long window = 0;
            try {
                for (int minor = 6; minor >= 0; minor--) {
                    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
                    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, minor);
                    ModernUIMod.LOGGER.debug(ModernUIMod.MARKER, "Trying OpenGL 4.{}", minor);
                    window = GLFW.glfwCreateWindow(640, 480, "System Testing", 0, 0);
                    if (window != 0) {
                        ModernUIMod.LOGGER.info(ModernUIMod.MARKER, "Promoted to OpenGL 4.{} Core Profile",
                                minor);
                        return;
                    }
                }
                GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
                GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
            } catch (Throwable e) {
                GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
                GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2);
                ModernUIMod.LOGGER.warn(ModernUIMod.MARKER, "Fallback to OpenGL 3.2 Core Profile", e);
            } finally {
                if (window != 0) {
                    GLFW.glfwDestroyWindow(window);
                }
                GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_TRUE);
                GLFW.glfwSetErrorCallback(callback);
            }
        }
    }
}
