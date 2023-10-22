package me.katie.chatoverlay.mixin;

import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.ScreenManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import me.katie.chatoverlay.render.WindowCreateHacks;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = Window.class, remap = false)
public class WindowMixin {
    @ModifyArg(
            method = "<init>",
            index = 4,
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J",
                    remap = false
            )
    )
    private long ecw_injectWindowSharing(long original) {
        if (WindowCreateHacks.parentWindow != 0) {
            return WindowCreateHacks.parentWindow;
        }

        return original;
    }

    @Inject(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/glfw/GLFW;glfwDefaultWindowHints()V",
                    shift = At.Shift.AFTER
            )
    )
    private void ecw_injectWindowHints(WindowEventHandler windowEventHandler, ScreenManager screenManager, DisplayData displayData, String string, String string2, CallbackInfo ci) {
        for (Map.Entry<Integer, Integer> entry: WindowCreateHacks.windowHints.entrySet()) {
            GLFW.glfwWindowHint(entry.getKey(), entry.getValue());
        }
    }
}
