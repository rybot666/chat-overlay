package me.katie.chatoverlay.render;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.platform.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL30;

import java.util.Locale;
import java.util.OptionalInt;

import static net.minecraft.client.Minecraft.ON_OSX;

public class ExternalChatRenderer {
    public final Window window;
    private MainTarget renderTarget;
    private boolean visible = false;
    private int windowFrameBufferId = -1;

    public ExternalChatRenderer(ScreenManager screenManager) {
        DisplayData displayData = new DisplayData(
                640,
                480,
                OptionalInt.empty(),
                OptionalInt.empty(),
                false
        );

        // This is somewhat of a hack to force sharing of objects between windows. We set the context back because
        // Mojang overwrites it and doesn't set it back.
        long originalContext = GLFW.glfwGetCurrentContext();
        Window parent = Minecraft.getInstance().getWindow();

        WindowCreateHacks.setWindowHint(GLFW.GLFW_FLOATING, 1);
        WindowCreateHacks.setWindowHint(GLFW.GLFW_MOUSE_PASSTHROUGH, 1);
        WindowCreateHacks.setWindowHint(GLFW.GLFW_TRANSPARENT_FRAMEBUFFER, 1);
        WindowCreateHacks.setWindowHint(GLFW.GLFW_DECORATED, 0);
        WindowCreateHacks.setWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, 0);
        WindowCreateHacks.setWindowHint(GLFW.GLFW_RESIZABLE, 0);
        this.window = WindowCreateHacks.createParentedWindow(parent, new NopWindowEventHandler(), screenManager, displayData, null, "Chat Window");

        GLFW.glfwMakeContextCurrent(originalContext);
    }

    public void setupDefaultState() {
        this.window.setErrorSection("Startup");
        long originalContext = GLFW.glfwGetCurrentContext();

        try {
            GLFW.glfwMakeContextCurrent(this.window.getWindow());
            RenderSystem.setupDefaultState(0, 0, this.window.getWidth(), this.window.getHeight());

            this.window.setErrorSection("Post startup");
        } finally {
            GLFW.glfwMakeContextCurrent(originalContext);
        }
    }

    public void createBuffers() {
        // Create a render target and associated framebuffer.
        this.renderTarget = new MainTarget(this.window.getWidth(), this.window.getHeight());
        this.resizeAndMoveToFit();

        // Set initial GUI scale.
        int scale = this.window.calculateScale(Minecraft.getInstance().options.guiScale().get(), Minecraft.getInstance().isEnforceUnicode());
        this.window.setGuiScale(scale);
    }

    private void destroyFrameBuffer() {
        if (this.windowFrameBufferId == -1) {
            return;
        }

        long originalContext = GLFW.glfwGetCurrentContext();

        try {
            GLFW.glfwMakeContextCurrent(this.window.getWindow());
            GlStateManager._glDeleteFramebuffers(this.windowFrameBufferId);
            this.windowFrameBufferId = -1;
        } finally {
            GLFW.glfwMakeContextCurrent(originalContext);
        }
    }

    private void createWindowFramebuffer() {
        long originalContext = GLFW.glfwGetCurrentContext();

        try {
            GLFW.glfwMakeContextCurrent(this.window.getWindow());

            this.windowFrameBufferId = GlStateManager.glGenFramebuffers();
            GlStateManager._glBindFramebuffer(36160, this.windowFrameBufferId);

            GlStateManager._bindTexture(this.renderTarget.getColorTextureId());
            GlStateManager._texParameter(3553, 10241, 9728);
            GlStateManager._texParameter(3553, 10240, 9728);
            GlStateManager._texParameter(3553, 10242, 33071);
            GlStateManager._texParameter(3553, 10243, 33071);
            GlStateManager._glFramebufferTexture2D(36160, 36064, 3553, this.renderTarget.getColorTextureId(), 0);

            GlStateManager._bindTexture(this.renderTarget.getDepthTextureId());
            GlStateManager._texParameter(3553, 34892, 0);
            GlStateManager._texParameter(3553, 10241, 9728);
            GlStateManager._texParameter(3553, 10240, 9728);
            GlStateManager._texParameter(3553, 10242, 33071);
            GlStateManager._texParameter(3553, 10243, 33071);
            GlStateManager._glFramebufferTexture2D(36160, 36096, 3553, this.renderTarget.getDepthTextureId(), 0);

            GlStateManager._bindTexture(0);

            int code = GlStateManager.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
            if (code != GL30.GL_FRAMEBUFFER_COMPLETE) {
                throw new IllegalStateException("Failed to create window owned framebuffer: " + code);
            }

            GlStateManager._glBindFramebuffer(36160, 0);
        } finally {
            GLFW.glfwMakeContextCurrent(originalContext);
        }
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void render() {
        this.window.setErrorSection("Render");

        ProfilerFiller profiler = Minecraft.getInstance().getProfiler();
        Window mcWindow = Minecraft.getInstance().getWindow();
        profiler.popPush("external_chat_window");

        profiler.push("resizeAndMove");
        this.resizeAndMoveToFit();

        profiler.popPush("render");
        this.renderTarget.setClearColor(0f, 0f, 0f, 0f);
        this.renderTarget.clear(ON_OSX);

        if (this.visible &&
                !Minecraft.getInstance().noRender &&
                !Minecraft.getInstance().options.hideGui &&
                (Minecraft.getInstance().screen == null || Minecraft.getInstance().screen instanceof ChatScreen) &&
                Minecraft.getInstance().getOverlay() == null
        ) {
            this.renderTarget.bindWrite(true);

            // Setup RenderSystem state for GUI rendering.
            FogRenderer.setupNoFog();
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();

            RenderSystem.viewport(0, 0, mcWindow.getWidth(), mcWindow.getHeight());
            RenderSystem.clear(256, ON_OSX);
            RenderSystem.setProjectionMatrix(
                    new Matrix4f().setOrtho(
                            0.0f,
                            (float) ((double) mcWindow.getWidth() / mcWindow.getGuiScale()),
                            (float) ((double) mcWindow.getHeight() / mcWindow.getGuiScale()),
                            0.0f,
                            1000.0f,
                            21000.0f
                    ),
                    VertexSorting.ORTHOGRAPHIC_Z
            );

            PoseStack poseStack = RenderSystem.getModelViewStack();
            poseStack.pushPose();
            poseStack.setIdentity();
            poseStack.translate(0.0f, 0.0f, -11000.0f);
            RenderSystem.applyModelViewMatrix();

            Lighting.setupFor3DItems();

            GuiGraphics guiGraphics = new GuiGraphics(Minecraft.getInstance(), Minecraft.getInstance().renderBuffers().bufferSource());
            int mouseX = Mth.floor(Minecraft.getInstance().mouseHandler.xpos() * (double) mcWindow.getGuiScaledWidth() / (double) mcWindow.getScreenWidth());
            int mouseY = Mth.floor(Minecraft.getInstance().mouseHandler.ypos() * (double) mcWindow.getGuiScaledHeight() / (double) mcWindow.getScreenHeight());

            if (Minecraft.getInstance().level != null) {
                // Draw chat overlay.
                profiler.push("drawChat");
                Minecraft.getInstance().gui.getChat().render(guiGraphics, Minecraft.getInstance().gui.getGuiTicks(), mouseX, mouseY);
                profiler.pop();
            }

            RenderSystem.clear(GL30.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

            if (Minecraft.getInstance().screen instanceof ChatScreen screen) {
                // Draw chat screen.
                try {
                    screen.renderWithTooltip(guiGraphics, mouseX, mouseY, Minecraft.getInstance().getDeltaFrameTime());
                } catch (Throwable throwable) {
                    CrashReport report = CrashReport.forThrowable(throwable, "Rendering screen");
                    CrashReportCategory category = report.addCategory("Screen render details");
                    category.setDetail("Screen name", () -> screen.getClass().getCanonicalName());
                    category.setDetail("Mouse location", () -> String.format(Locale.ROOT, "Scaled: (%d, %d). Absolute: (%f, %f)", mouseX, mouseY, Minecraft.getInstance().mouseHandler.xpos(), Minecraft.getInstance().mouseHandler.ypos()));
                    category.setDetail("Screen size", () -> String.format(Locale.ROOT, "Scaled: (%d, %d). Absolute: (%d, %d). Scale factor of %f", mcWindow.getGuiScaledWidth(), mcWindow.getGuiScaledHeight(), mcWindow.getWidth(), mcWindow.getHeight(), mcWindow.getGuiScale()));

                    throw new ReportedException(report);
                }

                try {
                    screen.handleDelayedNarration();
                } catch (Throwable throwable) {
                    CrashReport report = CrashReport.forThrowable(throwable, "Narrating screen");
                    CrashReportCategory category = report.addCategory("Screen details");
                    category.setDetail("Screen name", () -> screen.getClass().getCanonicalName());

                    throw new ReportedException(report);
                }
            }

            // Flush graphics and reset state.
            guiGraphics.flush();
            poseStack.popPose();
            RenderSystem.applyModelViewMatrix();
            this.renderTarget.unbindWrite();
        }

        // Swap OpenGL contexts and blit the main target texture to the screen.
        profiler.popPush("blit");
        long originalContext = GLFW.glfwGetCurrentContext();

        try {
            GLFW.glfwMakeContextCurrent(this.window.getWindow());

            RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, ON_OSX);

            // Bind the window framebuffer as the read buffer and then blit to back buffer, then swap buffers.
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, this.windowFrameBufferId);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);

            GlStateManager._glBlitFrameBuffer(
                    0, 0, this.renderTarget.width, this.renderTarget.height,
                    0, 0, this.window.getWidth(), this.window.getHeight(),
                    GL30.GL_COLOR_BUFFER_BIT, GL30.GL_NEAREST
            );

            profiler.popPush("updateDisplay");

            // Can't poll events here because it causes race conditions if we process main window events, because MC
            // doesn't swap contexts.
            RenderSystem.replayQueue();
            Tesselator.getInstance().getBuilder().clear();
            GLFW.glfwSwapBuffers(this.window.getWindow());

            profiler.pop();
        } finally {
            GLFW.glfwMakeContextCurrent(originalContext);
        }

        this.window.setErrorSection("Post Render");

        // no profiler.pop() because the code we're injecting into uses `popPush`
    }

    public void resizeAndMoveToFit() {
        Window mcWindow = Minecraft.getInstance().getWindow();

        if (mcWindow.getWidth() != this.window.getWidth() || mcWindow.getHeight() != this.window.getHeight()) {
            this.window.setWidth(mcWindow.getWidth());
            this.window.setHeight(mcWindow.getHeight());
            GLFW.glfwSetWindowSize(this.window.getWindow(), this.window.getWidth(), this.window.getHeight());

            int scale = this.window.calculateScale(Minecraft.getInstance().options.guiScale().get(), Minecraft.getInstance().isEnforceUnicode());
            this.window.setGuiScale(scale);

            if (this.renderTarget != null) {
                this.destroyFrameBuffer();
                this.renderTarget.resize(this.window.getWidth(), this.window.getHeight(), ON_OSX);
                this.createWindowFramebuffer();
            }
        }

        // Move to fit MC window position.
        int[] xs = new int[1];
        int[] ys = new int[1];
        GLFW.glfwGetWindowPos(mcWindow.getWindow(), xs, ys);
        GLFW.glfwSetWindowPos(this.window.getWindow(), xs[0], ys[0]);
    }

    private static class NopWindowEventHandler implements WindowEventHandler {
        @Override
        public void setWindowActive(boolean active) {
            if (active) {
                GLFW.glfwFocusWindow(Minecraft.getInstance().getWindow().getWindow());
            }
        }

        @Override
        public void resizeDisplay() {

        }

        @Override
        public void cursorEntered() {

        }
    }
}
