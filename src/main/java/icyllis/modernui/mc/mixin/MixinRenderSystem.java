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

package icyllis.modernui.mc.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.mc.ModernUIMod;
import icyllis.modernui.mc.fabric.MuiFabricBootstrap;
import net.minecraft.util.TimeSource;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.Configuration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(RenderSystem.class)
public class MixinRenderSystem {

    @Inject(method = "initBackendSystem", at = @At("HEAD"), remap = false)
    private static void onInitBackendSystem(CallbackInfoReturnable<TimeSource.NanoTimeSource> ci) {
        String name = Configuration.OPENGL_LIBRARY_NAME.get();
        if (name != null) {
            // non-system library should load before window creation
            ModernUIMod.LOGGER.info(ModernUIMod.MARKER, "OpenGL library: {}", name);
            Objects.requireNonNull(GL.getFunctionProvider(), "Implicit OpenGL loading is required");
        }
    }

    /**
     * 设备初始化的首选触发点。
     * <p>
     * ⚠ VulkanMod 的 {@code net.vulkanmod.mixin.render.RenderSystemMixin} 把
     * {@code RenderSystem.initRenderer} 整个 {@code @Overwrite} 掉了，而本 mixin
     * 的 priority 是 1（比 VulkanMod 的默认 1000 低，即**先**应用），
     * 所以这个注入有被 VulkanMod 的覆写静默丢弃的可能。
     * 因此真正干活的是 {@link MuiFabricBootstrap#initialize()}（幂等），
     * 同时 {@code MixinMinecraftEXT} 在 {@code Minecraft.<init>} 结束时再调一次兜底。
     */
    @Inject(method = "initRenderer", at = @At("TAIL"), remap = false)
    private static void onInitRenderer(int debugLevel, boolean debugSync, CallbackInfo ci) {
        MuiFabricBootstrap.initialize();
    }

    /**
     * @author BloCamLimb
     * @reason Disable runtime checks
     */
    @Overwrite(remap = false)
    public static void assertOnRenderThread() {
    }
}
