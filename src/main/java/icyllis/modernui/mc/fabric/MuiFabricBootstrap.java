/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.modernui.mc.fabric;

import icyllis.arc3d.engine.ContextOptions;
import icyllis.modernui.core.Core;
import icyllis.modernui.mc.ModernUIClient;
import icyllis.modernui.mc.ModernUIMod;
import icyllis.modernui.mc.VulkanModIntegration;

/**
 * Modern UI 的设备初始化，独立成一个类的原因有两条：
 * <ol>
 *     <li>要在原始 OpenGL 与 VulkanMod 两条路径之间分流；</li>
 *     <li>初始化可能有两个触发点（见 {@link #initialize()}），需要幂等。</li>
 * </ol>
 */
public final class MuiFabricBootstrap {

    private static boolean sInitialized;

    private MuiFabricBootstrap() {
    }

    /**
     * 初始化 Arc3D 设备与 UI 管理器，**只会真正执行一次**。
     * <p>
     * 有两个调用点，因为 VulkanMod 的
     * {@code net.vulkanmod.mixin.render.RenderSystemMixin} 把
     * {@code RenderSystem.initRenderer} 整个 {@code @Overwrite} 掉了：
     * <ul>
     *     <li>{@code MixinRenderSystem#onInitRenderer} —— 正常路径，
     *         {@code initRenderer} 的 TAIL；</li>
     *     <li>{@code MixinMinecraftEXT#onMinecraftInitDone} —— 兜底，
     *         {@code Minecraft.<init>} 的 RETURN（原版在 {@code <init>} 里
     *         调用 {@code RenderSystem.initRenderer}，所以这个点必然更晚，
     *         并且 VulkanMod 只在这里挂了普通 {@code @Inject}，没有覆盖）。</li>
     * </ul>
     * Mixin 的 {@code @Overwrite} 会丢掉先前注入进该方法的代码，
     * 所以上面第一个调用点有被静默丢弃的可能 —— 这正是需要兜底的原因。
     */
    public static void initialize() {
        synchronized (MuiFabricBootstrap.class) {
            if (sInitialized) {
                return;
            }
            // 先置位再干活：Core.initialize() 本身不是为重复调用设计的，
            // 一旦开始就不允许第二个调用点再进来。
            sInitialized = true;
        }

        Core.initialize();

        ContextOptions options = new ContextOptions();
        String value = ModernUIClient.getBootstrapProperty(ModernUIClient.BOOTSTRAP_USE_STAGING_BUFFERS_IN_OPENGL);
        if (value != null) {
            options.mUseStagingBuffers = Boolean.parseBoolean(value);
        }
        value = ModernUIClient.getBootstrapProperty(ModernUIClient.BOOTSTRAP_ALLOW_SPIRV_IN_OPENGL);
        if (value != null) {
            options.mAllowGLSPIRV = Boolean.parseBoolean(value);
        }
        options.mDriverBugWorkarounds = ModernUIClient.getGpuDriverBugWorkarounds();

        if (ModernUIMod.isVulkanModLoaded()) {
            // VulkanMod 已经把 OpenGL 整个拿掉了，只能借它自己的 VkDevice。
            ModernUIMod.LOGGER.info(ModernUIMod.MARKER,
                    "Creating Arc3D device on VulkanMod's Vulkan device");
            var context = VulkanModIntegration.wrapContext();
            if (!Core.initVulkan(context, options)) {
                // 这里不能像 OpenGL 那样退回原生渲染：Modern UI 的界面全靠 Arc3D 画，
                // 没有设备就什么都画不出来。直接抛，让崩溃报告一眼看出原因。
                throw new IllegalStateException(
                        "Failed to create Arc3D Vulkan device on VulkanMod's device. " +
                                "This is a bug of the VulkanMod compatibility port.");
            }
        } else {
            if (!Core.initOpenGL(options)) {
                Core.glShowCapsErrorDialog();
            }
        }

        UIManagerFabric.initialize();
    }
}
