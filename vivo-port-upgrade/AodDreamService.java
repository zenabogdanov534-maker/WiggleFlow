package io.oneplus.vivothemes;

import android.service.dreams.DreamService;

public class AodDreamService extends DreamService {
    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(false);
        setContentView(new AodStudioActivity.AodCanvasView(this));
    }
}
