/*
 * Modern UI.
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

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinConfigPlugin implements IMixinConfigPlugin {

    private boolean mDisableSmoothScrolling;
    private boolean mDisableEnhancedTextField;
    private boolean mVulkanModLoaded;

    @Override
    public void onLoad(String mixinPackage) {
        if (MuiPlatform.get().isClient()) {
            boolean smoothScrollModLoaded = detectSmoothScrollMod();
            ModernUIMod.sSmoothScrollModLoaded = smoothScrollModLoaded;
            String smoothScrolling = ModernUIMod.getBootstrapProperty(
                    ModernUIMod.BOOTSTRAP_DISABLE_SMOOTH_SCROLLING);
            if (smoothScrolling == null) {
                mDisableSmoothScrolling = smoothScrollModLoaded;
                if (smoothScrollModLoaded) {
                    ModernUIMod.LOGGER.info(ModernUIMod.MARKER,
                            "smoothscroll detected, Modern UI smooth scrolling is disabled by default; " +
                                    "set modernui_mc_disableSmoothScrolling=false in bootstrap.properties " +
                                    "to force it back on");
                }
            } else {
                mDisableSmoothScrolling = Boolean.parseBoolean(smoothScrolling);
                if (smoothScrollModLoaded && !mDisableSmoothScrolling) {
                    ModernUIMod.LOGGER.warn(ModernUIMod.MARKER,
                            "smoothscroll detected but Modern UI smooth scrolling is explicitly enabled, " +
                                    "they both write AbstractSelectionList.scrollAmount and vanilla list " +
                                    "scrolling may not work");
                }
            }
            mDisableEnhancedTextField = Boolean.parseBoolean(ModernUIMod.getBootstrapProperty(
                    ModernUIMod.BOOTSTRAP_DISABLE_ENHANCED_TEXT_FIELD));
        }
        mVulkanModLoaded = detectVulkanMod();
        if (mVulkanModLoaded) {
            ModernUIMod.LOGGER.info(ModernUIMod.MARKER,
                    "VulkanMod detected, disabling window GL version promotion");
        }
    }

    /**
     * Mixin loads before mods are constructed, so a mod cannot be queried by class
     * name here, look for one of its resources instead.
     */
    private static boolean detectVulkanMod() {
        try {
            ClassLoader loader = MixinConfigPlugin.class.getClassLoader();
            return loader != null &&
                    loader.getResource("net/vulkanmod/vulkan/Vulkan.class") != null;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean detectSmoothScrollMod() {
        try {
            ClassLoader loader = MixinConfigPlugin.class.getClassLoader();
            if (loader == null) {
                return false;
            }
            return loader.getResource("smsk/smoothscroll/SmoothSc.class") != null ||
                    loader.getResource("smsk/smoothscroll/mixin/EntryListWidgetMixin.class") != null;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mVulkanModLoaded &&
                mixinClassName.equals("icyllis.modernui.mc.mixin.MixinWindowGpuPromotion")) {
            return false;
        }
        if (mDisableSmoothScrolling) {
            if (mixinClassName.equals("icyllis.modernui.mc.mixin.MixinScrollPanel") ||
                    mixinClassName.equals("icyllis.modernui.mc.mixin.MixinSelectionList")) {
                return false;
            }
        }
        if (mDisableEnhancedTextField) {
            if (mixinClassName.equals("icyllis.modernui.mc.mixin.MixinEditBox") ||
                    mixinClassName.equals("icyllis.modernui.mc.mixin.MixinStringSplitter") ||
                    mixinClassName.equals("icyllis.modernui.mc.mixin.MixinTextFieldHelper")) {
                return false;
            }
        }
        return !mixinClassName.endsWith("DBG");
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}
