/*
 * HalfNES by Andrew Hoffman
 * Licensed under the GNU GPL Version 3. See LICENSE file
 */
package com.grapeshot.halfnes.video;

import com.grapeshot.halfnes.video.NesColors;
import com.grapeshot.halfnes.video.Renderer;
import java.awt.image.BufferedImage;

/**
 *
 * @author Andrew
 */
public class RGBRenderer extends Renderer {

    public RGBRenderer() {
        frame_width = 256;
        init_images();
    }

    @Override
    public BufferedImage render(int[] nespixels, int[] bgcolors, boolean dotcrawl) {
        //and now replace the nes color numbers with rgb colors (respecting color emph bits)
        for (int i = 0; i < nespixels.length; ++i) {
            nespixels[i] = NesColors.col[(nespixels[i] & 0x1c0) >> 6][nespixels[i] & 0x3f];
        }
        return getBufferedImage(nespixels);
    }
}
