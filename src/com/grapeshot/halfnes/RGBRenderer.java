/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.grapeshot.halfnes;

import java.awt.image.BufferedImage;

/**
 *
 * @author Andrew
 */
public class RGBRenderer extends Renderer {

    @Override
    public BufferedImage render(int[] nespixels, int[] bgcolors, boolean dotcrawl) {
        //and now replace the nes color numbers with rgb colors (respecting color emph bits)
        for (int i = 0; i < nespixels.length; ++i) {
            nespixels[i] = NesColors.col[(nespixels[i] & 0x1c0) >> 6][nespixels[i] & 0x3f];
        }
        return getImageFromArray(nespixels, 256 * 8, 256, 224);
    }
}
