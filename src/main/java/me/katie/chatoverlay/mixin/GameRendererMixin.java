package me.katie.chatoverlay.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Redirect(
            method = "render",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;",
                    opcode = Opcodes.GETFIELD
            )
    )
    private Screen ecw_redirectScreenDuringRendering(Minecraft instance) {
        Screen screen = instance.screen;

        if (screen instanceof ChatScreen) {
            return null;
        }

        return screen;
    }
}
