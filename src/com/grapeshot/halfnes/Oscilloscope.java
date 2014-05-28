/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.grapeshot.halfnes;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 *
 * @author Andrew
 */
public class Oscilloscope implements AudioOutInterface {

    final private static int width = 400, length = 640;
    final private static int scf = 65536 / width / 2;
    DebugUI d;
    BufferedImage b;
    Graphics2D g;
    AudioOutInterface iface;
    int[] buffer = new int[length];
    int buf_ptr = 0;
    int prevsample = 0;

    public Oscilloscope(AudioOutInterface i) {
        this.iface = i;
        d = new DebugUI(length, width);
        b = new BufferedImage(length, width, BufferedImage.TYPE_INT_ARGB_PRE);
        g = b.createGraphics();
        g.setBackground(Color.black);
        g.setColor(Color.green);
        d.pack();
        d.run();
    }

    public Oscilloscope() {
        this.iface = null;
        d = new DebugUI(length, width);
        b = new BufferedImage(length, width, BufferedImage.TYPE_INT_ARGB_PRE);
        g = b.createGraphics();
        g.setBackground(Color.black);
        g.setColor(Color.red);
        d.pack();
        d.run();
    }

    @Override
    public void outputSample(int sample) {
        if (buf_ptr > 0
                || (prevsample <= 0 && sample >= 0)) {
            //start cap @ zero crossing
            if (buf_ptr < buffer.length) {
                buffer[buf_ptr++] = sample;
            }
        }
        prevsample = sample;
        if (!(iface == null)) {
            iface.outputSample(sample);
        }
    }

    @Override
    public void flushFrame(boolean waitIfBufferFull) {
        if (!(iface == null)) {
            iface.flushFrame(waitIfBufferFull);
        }
        g.clearRect(0, 0, length, width);
        for (int i = 1; i < buf_ptr; ++i) {

            g.drawLine(i - 1, (buffer[i - 1] / scf) + width / 2, i, (buffer[i] / scf) + width / 2);
        }
        g.drawLine(0, width / 2, length, width / 2);
        d.setFrame(b);
        buf_ptr = 0;

    }

    @Override
    public void pause() {
        if (!(iface == null)) {
            iface.pause();
        }
    }

    @Override
    public void resume() {
        if (!(iface == null)) {
            iface.resume();
        }
    }

    @Override
    public void destroy() {

        d.setVisible(false);
        d.dispose();
        if (!(iface == null)) {
            iface.destroy();
        }
    }

    @Override
    public boolean bufferHasLessThan(int samples) {
        if (!(iface == null)) {
            return iface.bufferHasLessThan(samples);
        } else {
            return false;
        }
    }
}
