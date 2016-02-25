/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.grapeshot.halfnes.video;

/**
 *
 * @author Andrew
 */
public class NesColors {

    private NesColors() {}
    
    private final static double att = 0.7;
    public final static int[][] col = GetNESColors();
    public final static byte[][][] colbytes = NESColorsToBytes(col);

    private static int[][] GetNESColors() {
        //just or's all the colors with opaque alpha and does the color emphasis calcs
        //This set of colors matches current version of ntsc filter output
        int[] colorarray = {
            0x606060, 0x09268e, 0x1a11bd, 0x3409b6, 0x5e0982, 0x790939, 0x6f0c09, 0x511f09,
            0x293709, 0x0d4809, 0x094e09, 0x094b17, 0x093a5a, 0x000000, 0x000000, 0x000000,
            0xb1b1b1, 0x1658f7, 0x4433ff, 0x7d20ff, 0xb515d8, 0xcb1d73, 0xc62922, 0x954f09,
            0x5f7209, 0x28ac09, 0x099c09, 0x099032, 0x0976a2, 0x090909, 0x000000, 0x000000,
            0xffffff, 0x5dadff, 0x9d84ff, 0xd76aff, 0xff5dff, 0xff63c6, 0xff8150, 0xffa50d,
            0xccc409, 0x74f009, 0x54fc1c, 0x33f881, 0x3fd4ff, 0x494949, 0x000000, 0x000000,
            0xffffff, 0xc8eaff, 0xe1d8ff, 0xffccff, 0xffc6ff, 0xffcbfb, 0xffd7c2, 0xffe999,
            0xf0f986, 0xd6ff90, 0xbdffaf, 0xb3ffd7, 0xb3ffff, 0xbcbcbc, 0x000000, 0x000000};
        //the older rgb palette I was using, may make this switchable.
//        int[] rgbarray = {0x757575, 0x271B8F, 0x0000AB,
//            0x47009F, 0x8F0077, 0xAB0013, 0xA70000, 0x7F0B00, 0x432F00,
//            0x004700, 0x005100, 0x003F17, 0x1B3F5F, 0x000000, 0x000000,
//            0x000000, 0xBCBCBC, 0x0073EF, 0x233BEF, 0x8300F3, 0xBF00BF,
//            0xE7005B, 0xDB2B00, 0xCB4F0F, 0x8B7300, 0x009700, 0x00AB00,
//            0x00933B, 0x00838B, 0x000000, 0x000000, 0x000000, 0xFFFFFF,
//            0x3FBFFF, 0x5F97FF, 0xA78BFD, 0xF77BFF, 0xFF77B7, 0xFF7763,
//            0xFF9B3B, 0xF3BF3F, 0x83D313, 0x4FDF4B, 0x58F898, 0x00EBDB,
//            0x444444, 0x000000, 0x000000, 0xFFFFFF, 0xABE7FF, 0xC7D7FF,
//            0xD7CBFF, 0xFFC7FF, 0xFFC7DB, 0xFFBFB3, 0xFFDBAB, 0xFFE7A3,
//            0xE3FFA3, 0xABF3BF, 0xB3FFCF, 0x9FFFF3, 0xaaaaaa, 0x000000,
//            0x000000};
        for (int i = 0; i < colorarray.length; ++i) {
            colorarray[i] |= 0xff000000;
        }
        int[][] colors = new int[8][colorarray.length];
        for (int j = 0; j < colorarray.length; ++j) {
            int col = colorarray[j];
            int r = r(col);
            int b = b(col);
            int g = g(col);
            colors[0][j] = col;
            //emphasize red
            colors[1][j] = compose_col(r, g * att, b * att);
            //emphasize green
            colors[2][j] = compose_col(r * att, g, b * att);
            //emphasize yellow
            colors[3][j] = compose_col(r, g, b * att);
            //emphasize blue
            colors[4][j] = compose_col(r * att, g * att, b);
            //emphasize purple
            colors[5][j] = compose_col(r, g * att, b);
            //emphasize cyan?
            colors[6][j] = compose_col(r * att, g, b);
            //de-emph all 3 colors
            colors[7][j] = compose_col(r * att, g * att, b * att);

        }
        return colors;
    }


    private static byte[][][] NESColorsToBytes(int[][] col) {
        byte[][][] colbytes = new byte[col.length][][];
        for (int i=0; i<col.length; i++) {
            int[] col2 = col[i];
            byte[][] colbytes2 = colbytes[i] = new byte[col2.length][3];
            for (int j=0; j<col2.length; j++) {
                colbytes2[j][0] = (byte) b(col2[j]);
                colbytes2[j][1] = (byte) g(col2[j]);
                colbytes2[j][2] = (byte) r(col2[j]);
            }
        }
        return colbytes;
    }
    
    private static int r(int col) {
        return (col >> 16) & 0xff;
    }

    private static int g(int col) {
        return (col >> 8) & 0xff;
    }

    private static int b(int col) {
        return col & 0xff;
    }

    private static int compose_col(double r, double g, double b) {
        return (((int) r & 0xff) << 16) + (((int) g & 0xff) << 8) + ((int) b & 0xff) + 0xff000000;
    }
}
