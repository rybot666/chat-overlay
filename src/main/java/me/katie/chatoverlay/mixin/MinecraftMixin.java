package me.katie.chatoverlay.mixin;

import com.mojang.blaze3d.platform.ScreenManager;
import me.katie.chatoverlay.render.ExternalChatRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.renderer.VirtualScreen;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow @Final private VirtualScreen virtualScreen;
    @Unique private ExternalChatRenderer ecw_renderer;

    @Inject(
            method = "<init>",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/Minecraft;window:Lcom/mojang/blaze3d/platform/Window;",
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.AFTER
            )
    )
    private void ecw_createWindow(GameConfig gameConfig, CallbackInfo ci) {
        ScreenManager screenManager = ((VirtualScreenAccessor) (Object) this.virtualScreen).getScreenManager();
        this.ecw_renderer = new ExternalChatRenderer(screenManager);
    }

    @Inject(
            method = "<init>",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/Minecraft;mainRenderTarget:Lcom/mojang/blaze3d/pipeline/RenderTarget;",
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.AFTER
            )
    )
    private void ecw_createRenderTarget(GameConfig gameConfig, CallbackInfo ci) {
        this.ecw_renderer.createBuffers();
    }

    @Inject(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;setupDefaultState(IIII)V",
                    shift = At.Shift.AFTER,
                    remap = false
            )
    )
    private void ecw_setupDefaultState(GameConfig gameConfig, CallbackInfo ci) {
        this.ecw_renderer.setupDefaultState();
    }

    @Inject(
            method = "runTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/Window;updateDisplay()V",
                    shift = At.Shift.AFTER
            )
    )
    private void ecw_render(boolean bl, CallbackInfo ci) {
        this.ecw_renderer.render();
    }

    @Inject(
            method = "setWindowActive",
            at = @At("TAIL")
    )
    private void ecw_setWindowActive(boolean active, CallbackInfo ci) {
        if (this.ecw_renderer != null) {
            this.ecw_renderer.setVisible(active);
        }
    }
}
