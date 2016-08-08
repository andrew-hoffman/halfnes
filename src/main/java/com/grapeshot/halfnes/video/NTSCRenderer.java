/*
 * HalfNES by Andrew Hoffman
 * Licensed under the GNU GPL Version 3. See LICENSE file
 */
package com.grapeshot.halfnes.video;

import java.awt.image.*;
import java.util.*;

import java.util.zip.CRC32;

/**
 *
 * @author Andrew
 */
public class NTSCRenderer extends Renderer {

    private final static List<Integer> lines;
    static {
        lines = new ArrayList<>();
        for (int line = 0; line < 240; ++line) {
            lines.add(line);
        }
    }

    //hm, if I downsampled these perfectly to 4Fsc i could get rid of matrix decode
    //and the sine tables altogether...
    private final static int[][] colorphases = { //int for alignment reasons
        {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},//0x00
        {0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1},//0x01
        {0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0},//0x02
        {0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0},//0x03
        {0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0},//0x04
        {0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0},//0x05
        {0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0},//0x06
        {1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0},//0x07
        {1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1},//0x08
        {1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1},//0x09
        {1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1},//0x0A
        {1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1},//0x0B
        {1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1},//0x0C
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},//0x0D
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},//0x0E
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}};//0x0F
    //i would like to replace these tables with logic but it's a tricky shape
    //for a Karnaugh map
    private final static float[][][] lumas = genlumas();
    private final static int[][] coloremph = {
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1},//X
        {0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0},//Y
        {1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1},//XY
        {1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1},//Z
        {1, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 1},//XZ
        {1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1},//YZ
        {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}};//XYZ
    //private final static float sync = -0.359f;
    private int frames = 0;
    private final float[] i_filter = new float[12], q_filter = new float[12];
    private final static int[] colortbl = genColorCorrectTbl();

    public NTSCRenderer() {
        frame_width = 704 * 3;
        init_images();
        int hue = -512;
        double col_adjust = 1.2 / .707;
        for (int j = 0; j < 12; ++j) {
            float angle = (float) (Math.PI * ((hue + (j << 8)) / (12 * 128.0) - 33.0 / 180));
            i_filter[j] = (float) (-col_adjust * Math.cos(angle));
            q_filter[j] = (float) (col_adjust * Math.sin(angle));
        }
    }

    public static int[] genColorCorrectTbl() {
        int[] corr = new int[256];
        //float gamma = 1.2;
        float brightness = 20;
        float contrast = 1;
        for (int i = 0; i < 256; ++i) {
            float br = (i * contrast - (128 * contrast) + 128 + brightness) / 255.f;
            corr[i] = clamp((int) (255 * Math.pow(br, 1.3)));
            //convert tv gamma image (~2.2-2.5) to computer gamma (~1.8)
        }
        return corr;
    }

    public static float[][][] genlumas() {
        float[][] lumas = {
            {-0.117f, 0.000f, 0.308f, 0.715f}, //low phase
            //0x00    0x10    0x20    0x30
            {0.397f, 0.681f, 1.0f, 1.0f} //high phase
        };
        float[][][] premultlumas = new float[lumas.length][lumas[0].length][2];
        for (int i = 0; i < lumas.length; ++i) {
            for (int j = 0; j < lumas[i].length; ++j) {
                premultlumas[i][j][0] = lumas[i][j];
                premultlumas[i][j][1] = lumas[i][j] * 0.735f;
            }
        }
        return premultlumas;
    }

    public final float[] ntsc_encode(final int[] nescolors, final int offset, final int scanline, final int bgcolor) {
        //part one of the process. creates a 2728 pxl array of floats representing
        //ntsc version of scanline passed to it. Meant to be called 240x a frame

        //todo:
        //-make this encode an entire frame at a time
        //-reduce # of array lookups (precalc. what is necessary)
        int i, col = bgcolor & 0xf, lum = (bgcolor >> 4) & 3, emphasis = (bgcolor >> 6);
        //luminance portion of nes color is bits 4-6, chrominance part is bits 1-3
        //they are both used as the index into various tables
        //the chroma generator chops between 2 different voltages from luma table 
        //at a constant rate but shifted phase.

        //sync and front porch are not actually used by decoder so not implemented here
        //dot 0-200:sync
        //dot 200-232:black
        //dot 232-352:colorburst
        //dot 352-400:black  
        //dot 520-2568:picture
        //dot 400-520 and 2568-2656: background color
        //dot 2656-2720:black
        //but then i'm going to chop off before dot 240 and after 2656 b/c it's not used
        //so after this comment, add 240 to any num. in this for dot #
        final float[] sample = new float[2728 - 240];
        for (i = 400 - 240; i < 520 - 240; ++i) { //bg color at beginning
            final int phase = (i + offset) % 12;
            final int hue = colorphases[col][phase];
            sample[i] = lumas[hue][lum][coloremph[emphasis][phase]];
        }
        for (i = 2568 - 240; i < 2656 - 240; ++i) { //bg color at end of line
            final int phase = (i + offset) % 12;
            final int hue = colorphases[col][phase];
            sample[i] = lumas[hue][lum][coloremph[emphasis][phase]];
        }
        for (i = 520 - 240; i < 2568 - 240; ++i) { //picture
            if ((i & 7) == 0) {
                col = nescolors[(((i - (520 - 240)) >> 3))];
                if ((col & 0xf) > 0xd) {
                    col = 0x0f;
                }
                lum = (col >> 4) & 3;
                emphasis = (col >> 6);
                col &= 0xf;
            }
            final int phase = (i + offset) % 12;
            final int hue = colorphases[col][phase];
            sample[i] = lumas[hue][lum][coloremph[emphasis][phase]];
        }
        sample[2728 - 241] = offset; //hack to not have to deal with a tuple
        return sample;
    }
    public final static float chroma_filterfreq = 3579000.f, pixel_rate = 42950000.f;
    private final static int[] cbstphase = {240 - 240, 0, 250 - 240, 0, 248 - 240, 0, 246 - 240, 0, 244 - 240, 0, 242 - 240, 0};
    //starting point for color burst (depends on offset of previous line, even values not used in a progressive signal)

    public final int[] ntsc_decode(final float[] ntsc, final int offset) {
        final float[] chroma = new float[2656 - 240];
        final float[] luma = new float[2656 - 240];
        final float[] eye = new float[2656 - 240];
        final float[] queue = new float[2656 - 240];
        final int[] line = new int[frame_w];

        //decodes one scan line of ntsc video and outputs as rgb packed in int
        //uses the cheap TV method, which is filtering the chroma from the luma w/o
        //combing or buffering previous lines
        box_filter(ntsc, luma, chroma, 12);

        for (int cbst = cbstphase[offset], j = 0; cbst < 2656 - 240 - 50; ++cbst, ++j, j %= 12) {
            //matrix decode the color difference signals;
            eye[cbst] = i_filter[j] * chroma[cbst + 12];
            queue[cbst] = q_filter[j] * chroma[cbst + 12]; //comment out for teal and orange filter
        }

        lowpass_filter(eye, 0.06f);
        lowpass_filter(queue, 0.05f);

        for (int i = 0, x = 492 - 240; i < frame_w; ++i, ++x) {
            line[i] = compose_col(
                    ((luma[x] <= 0) ? 0 : colortbl[clamp((int) (iqm[0][0] * luma[x] + iqm[0][1] * eye[x] + iqm[0][2] * queue[x]))]),
                    ((luma[x] <= 0) ? 0 : colortbl[clamp((int) (iqm[1][0] * luma[x] + iqm[1][1] * eye[x] + iqm[1][2] * queue[x]))]),
                    ((luma[x] <= 0) ? 0 : colortbl[clamp((int) (iqm[2][0] * luma[x] + iqm[2][1] * eye[x] + iqm[2][2] * queue[x]))]));
        }
        return line;
    }

    public static void box_filter(final float[] in, final float[] lpout, final float[] hpout, final int order) {
        float accum = 0;
        for (int i = 12; i < 2656 - 240; ++i) {
            accum += in[i] - in[i - order];
            lpout[i] = accum / order;
            hpout[i] = in[i] - lpout[i];
        }
    }

    public static void lowpass_filter(final float[] arr, final float order) {
        float b = 0;
        for (int i = 0; i < 2656 - 240; ++i) {
            arr[i] -= b;
            b += arr[i] * order;
            arr[i] = b;
        }
    }

    private static int compose_col(int r, int g, int b) {
        return (r << 16) | (g << 8) | (b) | 0xff000000;
    }
    private final static int[][] iqm = {{255, -244, 158}, {255, 69, -165}, {255, 282, 434}};

    public static int clamp(final int a) {
        return (a != (a & 0xff)) ? ((a < 0) ? 0 : 255) : a;
    }
    public final static int frame_w = 704 * 3;
    int[] frame = new int[frame_w * 240];
//    Kernel kernel = new Kernel(3, 3,
//            new float[]{-.0625f, .125f, -.0625f,
//                .125f, .75f, .125f,
//                -.0625f, .125f, -.0625f});
//    BufferedImageOp op = new ConvolveOp(kernel);

    @Override
    public BufferedImage render(final int[] nespixels, final int[] bgcolors, final boolean dotcrawl) {
        // multithreaded filter
        lines.parallelStream().forEach(line -> cacheRender(nespixels, line, bgcolors, dotcrawl));

        BufferedImage i = getBufferedImage(frame);
        ++frames;
        //i = op.filter(i, null); //sharpen
        return i;
    }
    //ConcurrentHashMap cache = new ConcurrentHashMap<Long, int[]>(600);
    Map<Long, int[]> cache = Collections.synchronizedMap(new WeakHashMap<Long, int[]>(600));
    //weak hash map allows things in it to be garbage collected

    private void cacheRender(final int[] nespixels, final int line, final int[] bgcolors, final boolean dotcrawl) {

        //first of all, increment scanline numbers and get the offset for this line.
        int offset = ((frames & 1) == 0 && dotcrawl) ? 0 : 6;
        offset = (4 * line + offset) % 12; //3 line dot crawl
        final int[] inpixels = new int[256];
        System.arraycopy(nespixels, line << 8, inpixels, 0, 256);
        final long crc = crc32(inpixels, offset, bgcolors[line]);
//        //you'd think crc32 would have too many collisions but i haven't seen a one
        int[] outpixels;
        outpixels = (int[]) cache.get(crc);
        if (outpixels == null) { //not in cache
            //could do with hints from the PPU here: if the entire screen is
            //scrolling horizontally, the cache will be useless.
            outpixels = ntsc_decode(ntsc_encode(inpixels, offset, line, bgcolors[line]), offset);
           cache.put(crc, outpixels);
        }

        System.arraycopy(outpixels, 0, frame, line * frame_w, frame_w);
    }

    public static long crc32(int[] array, int offset, int bgcolor) {
        CRC32 c = new CRC32();
        for (int i : array) {
            c.update(i);
        }
        //it's not immediately obvious, but this ONLY sets the CRC based on the
        //blue channel of the output. Still works well though.
        //You get some interesting compressiony effects if you only take the CRC
        //of every 20th pixel to see if your line is the same.
        //especially in sidescrolling things.
        c.update(offset);
        c.update(bgcolor);
        return c.getValue();
    }
}
