package me.katie.chatoverlay.render;

import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.ScreenManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class WindowCreateHacks {
    private WindowCreateHacks() {
        throw new UnsupportedOperationException("Cannot instantiate WindowCreateHacks");
    }

    public static long parentWindow = 0;
    public static Map<Integer, Integer> windowHints = new HashMap<>();

    public static Window createParentedWindow(Window parent, WindowEventHandler windowEventHandler, ScreenManager screenManager, DisplayData displayData, @Nullable String string, String title) {
        parentWindow = parent.getWindow();
        Window window = new Window(windowEventHandler, screenManager, displayData, string, title);
        parentWindow = 0;
        windowHints.clear();

        return window;
    }

    public static void setWindowHint(int hint, int value) {
        windowHints.put(hint, value);
    }
}
