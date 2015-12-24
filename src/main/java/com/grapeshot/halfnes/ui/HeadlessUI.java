package com.grapeshot.halfnes.ui;

import com.grapeshot.halfnes.CPURAM;
import com.grapeshot.halfnes.NES;
import com.grapeshot.halfnes.video.RGBRenderer;
import com.grapeshot.halfnes.video.Renderer;

import java.awt.image.BufferedImage;

/**
 * @author Mitchell Skaggs
 */
public class HeadlessUI implements GUIInterface {

    private NES nes;
    private Renderer renderer;
    private boolean renderFrames;
    private BufferedImage lastFrame = null;
    private boolean updateImage;
    private PuppetController controller1, controller2;

    public HeadlessUI(String romToLoad, boolean renderFrames) {
        nes = new NES(this);
        this.loadROM(romToLoad);
        this.renderer = new RGBRenderer();
        this.controller1 = new PuppetController();
        this.controller2 = new PuppetController();
        nes.setControllers(this.controller1, this.controller2);
        this.renderFrames = renderFrames;
    }

    public void loadROM(String romToLoad) {
        this.nes.loadROM(romToLoad);
    }

    public BufferedImage getLastFrame() {
        return lastFrame;
    }

    public PuppetController getController1() {
        return controller1;
    }

    public PuppetController getController2() {
        return controller2;
    }

    public synchronized void runFrame() {
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
        if (renderFrames) {
            this.lastFrame = renderer.render(frame, bgcolor, dotcrawl);
        }
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
