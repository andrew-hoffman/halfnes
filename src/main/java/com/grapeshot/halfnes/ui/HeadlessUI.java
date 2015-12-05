package com.grapeshot.halfnes.ui;

import com.grapeshot.halfnes.CPURAM;
import com.grapeshot.halfnes.NES;
import com.grapeshot.halfnes.video.RGBRenderer;
import com.grapeshot.halfnes.video.Renderer;

/**
 * Created by Mitchell on 12/4/2015.
 */
public class HeadlessUI implements GUIInterface {

    private NES nes;
    private Renderer renderer;
    private boolean updateImage;

    public HeadlessUI(String romToLoad) {
        nes = new NES(this);
        this.renderer = new RGBRenderer();
    }

    public void runFrame() {
        nes.frameAdvance();
    }

    public CPURAM getNESCPURAM() {
        return nes.getCPURAM();
    }

    @Override
    public NES getNes() {
        return nes;
    }

    @Override
    public void setNES(NES nes) {
        this.nes = nes;
    }

    @Override
    public void setFrame(int[] frame, int[] bgcolor, boolean dotcrawl) {
        // Null-op
    }

    @Override
    public void messageBox(String message) {
        System.err.println(message); // Shouldn't get any messages except errors
    }

    @Override
    public void run() {
        // Null-op
    }

    @Override
    public void render() {
    }

    @Override
    public void loadROMs(String path) {
    }
}
