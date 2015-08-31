/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.grapeshot.halfnes.video;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 *
 * @author Andrew
 */
public abstract class Renderer {

    public abstract BufferedImage render(int[] nespixels, int[] bgcolors, boolean dotcrawl);

    public static BufferedImage getImageFromArray(final int[] bitmap, final int offset, final int width, final int height) {
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        DataBufferInt dataBuffer = new DataBufferInt(bitmap, width * height, offset);
        int[] bandmasks = new int[3];
        bandmasks[0] = 0x00ff0000;       // Red
        bandmasks[1] = 0x0000ff00;       // Green
        bandmasks[2] = 0x000000ff;       // Blue
        final ColorModel colorModel = new DirectColorModel(24, bandmasks[0], bandmasks[1], bandmasks[2], 0);
        final WritableRaster raster = Raster.createPackedRaster(dataBuffer, width, height, width, bandmasks, null);
        final BufferedImage bufferedImage = new BufferedImage(colorModel, raster, false, null);
        return bufferedImage;
    }
}
