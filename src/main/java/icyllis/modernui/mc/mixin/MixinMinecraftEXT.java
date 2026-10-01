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

import icyllis.modernui.mc.fabric.MuiFabricBootstrap;
import icyllis.modernui.mc.fabric.ModernUIFabricClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinMinecraftEXT {

    /**
     * 设备初始化的兜底触发点。
     * <p>
     * 原版在 {@code Minecraft.<init>} 内部调用 {@code RenderSystem.initRenderer}，
     * 所以构造器的 RETURN 一定更晚；而 VulkanMod 只在 {@code <init>} 的 RETURN 挂了
     * 一个普通 {@code @Inject}（{@code forceGraphicsMode}），没有 {@code @Overwrite}，
     * 不会把这里丢掉。
     * <p>
     * 为什么需要它：VulkanMod 覆写了整个 {@code RenderSystem.initRenderer}，
     * 可能导致 {@code MixinRenderSystem#onInitRenderer} 里的初始化被丢弃。
     * {@link MuiFabricBootstrap#initialize()} 是幂等的，两边都调也不会重复初始化。
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onMinecraftInitDone(GameConfig config, CallbackInfo ci) {
        MuiFabricBootstrap.initialize();
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;render(Lnet/minecraft/client/DeltaTracker;Z)V",
            shift = At.Shift.BEFORE))
    private void onStartRenderTick(boolean hasMemory, CallbackInfo ci) {
        ModernUIFabricClient.START_RENDER_TICK.invoker().run();
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;render(Lnet/minecraft/client/DeltaTracker;Z)V",
            shift = At.Shift.AFTER))
    private void onEndRenderTick(boolean hasMemory, CallbackInfo ci) {
        ModernUIFabricClient.END_RENDER_TICK.invoker().run();
    }
}
