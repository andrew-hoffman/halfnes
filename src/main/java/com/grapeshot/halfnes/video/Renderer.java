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
    
    int width = 256;
    int clip = 8;
    
    public abstract BufferedImage render(int[] nespixels, int[] bgcolors, boolean dotcrawl);
    
    public void setClip(int i){
        //how many lines to clip from top + bottom
        clip = i;
    }

    public static BufferedImage getImageFromArray(final int[] bitmap, final int offset, final int width, final int height) {
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
        final WritableRaster raster = image.getRaster();
        final int[] pixels = ((DataBufferInt) raster.getDataBuffer()).getData();
        System.arraycopy(bitmap, offset, pixels, 0, width * height);
        return image;
    }
}
