/*
 * HalfNES by Andrew Hoffman
 * Licensed under the GNU GPL Version 3. See LICENSE file
 */
package com.grapeshot.halfnes.video;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;

/**
 *
 * @author Andrew
 */
public abstract class Renderer {

    int frame_width;
    /*
    there's stuff involving this variable that's much uglier
    than it needs to be because of me not really remembering
    how abstract classes work
     */
    int clip = 8;
    int height = 240 - 2 * clip;
    BufferedImage[] imgs = {null, null, null, null};
    int imgctr = 0;

    protected final void init_images() {
        for (int i = 0; i < imgs.length; ++i) {
            imgs[i] = new BufferedImage(frame_width, height, BufferedImage.TYPE_INT_ARGB_PRE);
        }
    }

    public abstract BufferedImage render(int[] nespixels, int[] bgcolors, boolean dotcrawl);

    public void setClip(int i) {
        //how many lines to clip from top + bottom
        clip = i;
        height = 240 - 2 * clip;
    }

    public BufferedImage getBufferedImage(int[] frame) {
        final BufferedImage image = imgs[++imgctr % imgs.length];
        final WritableRaster raster = image.getRaster();
        final int[] pixels = ((DataBufferInt) raster.getDataBuffer()).getData();
        System.arraycopy(frame, frame_width * clip, pixels, 0, frame_width * height);
        return image;
    }

}
