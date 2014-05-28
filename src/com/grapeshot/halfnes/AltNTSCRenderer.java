/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.grapeshot.halfnes;

import java.awt.image.BufferedImage;
import com.grapeshot.halfnes.utils;

/**
 *
 * @author Andrew
 */
//Direct port of Bisqwit's code on the wiki. (probably just as slow, we'll see.)
//yep, it's WORSE
//the expensive part is the Math function calls of course
public class AltNTSCRenderer extends Renderer {

    public AltNTSCRenderer() {
//        for (int i = 0; i < 12; ++i) {
//            System.err.println(inColorPhase( 3, i));
//        };
    }
    private final int[] frame = new int[width * 240];
    private int frame_ptr = 0;
    private int frame_ctr;
    private int phase;
    private final static double attenuation = 0.746;
    private final static double[] levels = {
        -0.117f, 0.000f, 0.308f, 0.715f,
        0.397f, 0.681f, 1.0f, 1.0f
    //0x00    0x10    0x20    0x30
    };
    private final static int SAMPLESPERPIXEL = 8;

    @Override
    public BufferedImage render(int[] nespixels, int[] bgcolors, boolean dotcrawl) {
        ++frame_ctr;
        if ((frame_ctr & 1) == 0) {
            phase = 0;
        } else {
            phase = 6;
        }
        for (int i = 0; i < 240; ++i) {
            double phi = (phase + 3.9 - 0.3) % 12;
            for (int j = 0; j < 256; ++j) {
                ntsc_render(nespixels[i * 256 + j]);
            }
            ntsc_decode(phi);
            ntsc_buf_ptr = 0;
        }
        frame_ptr = 0;
        return getImageFromArray(frame, width * 8, width, 224);
    }

    private void ntsc_render(int pixel) {
        int color = pixel & 0xf;
        int level = (pixel >> 4) & 3;
        int emphasis = (pixel >> 6);
        if (color > 13) {
            level = 1;
        }
        double low = levels[level];
        double high = levels[4 + level];
        if (color == 0) {
            low = high;
        } else if (color > 12) {
            high = low;
        }
        for (int i = 0; i < SAMPLESPERPIXEL; ++i, ++phase) {
            double signal = inColorPhase(color, phase) ? high : low;
            if (emphasis != 0) {
                if ((utils.getbit(emphasis, 0) && inColorPhase(0, phase))
                        || (utils.getbit(emphasis, 1) && inColorPhase(4, phase))
                        || (utils.getbit(emphasis, 2) && inColorPhase(8, phase))) {
                    signal *= attenuation;
                }
            }
            signal_levels[ntsc_buf_ptr++] = signal;
        }
    }

    private static boolean inColorPhase(final int color, final int phase) {
        return (color + phase) % 12 < 6;
    }
    private double[] signal_levels = new double[256 * SAMPLESPERPIXEL];
    private int ntsc_buf_ptr = 0;
    private final static int width = 604;

    private void ntsc_decode(final double phase) {
        for (int x = 0; x < width; ++x) {
            int center = x * (256 * SAMPLESPERPIXEL) / width + 0;
            int begin = center - 6;
            if (begin < 0) {
                begin = 0;
            }
            int end = center + 6;
            if (end > 256 * SAMPLESPERPIXEL) {
                end = (256 * SAMPLESPERPIXEL);
            }
            double y = 0, i = 0, q = 0;
            for (int p = begin; p < end; ++p) {
                double level = signal_levels[p] / 12.;
                y += level;
                i += level * Math.cos(Math.PI * (phase + p) / 6.);
                q += level * Math.sin(Math.PI * (phase + p) / 6.);
            }
            render_pixel(y, i, q);
        }
    }

    private void render_pixel(final double y, final double i, final double q) {

        final int rgb = 0xff000000
                | 0x10000 * clamp(255.95 * gammafix(y + 0.946882f * i + 0.623557f * q))
                + 0x00100 * clamp(255.95 * gammafix(y + -0.274788f * i + -0.635691f * q))
                + 0x00001 * clamp(255.95 * gammafix(y + -1.108545f * i + 1.709007f * q));
        frame[frame_ptr++] = rgb;
    }

    public static int clamp(final double a) {
        return (int) ((a < 0) ? 0 : ((a > 255) ? 255 : a));
    }

    public static double gammafix(double luma) {
        final float gamma = 2.0f; // Assumed display gamma
        return luma <= 0.f ? 0.f : Math.pow(luma, 2.2f / gamma);
    }
}
