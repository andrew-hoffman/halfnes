/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.grapeshot.halfnes.audio;

import com.grapeshot.halfnes.ui.DebugUI;
import static com.grapeshot.halfnes.utils.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 *
 * @author Andrew
 */
public class VRC7SoundChip implements ExpansionSoundChip {

    //Emulates the YM2413 sound chip, pretty much only found in Lagrange Point
    //sound test in lagrange point: hold A and B on controller 2 and reset.
    //this is the cut-down version from the vrc7. Only 6 channels, no percussion. 
    private static enum adsr {

        CUTOFF, ATTACK, DECAY, SUSTAIN, SUSTRELEASE, RELEASE;
    }
    private final adsr[] modenv_state = new adsr[6], carenv_state = new adsr[6];
    private final int[] vol = new int[6], freq = new int[6],
            octave = new int[6], instrument = new int[6],
            mod = new int[6],
            oldmodout = new int[6], out = new int[6];
    private final boolean[] key = new boolean[6], sust = new boolean[6];
    private int fmctr = 0, amctr = 0; //free running counter for indices
    private final double[] phase = new double[6], modenv_vol = new double[6], carenv_vol = new double[6];
    private final int[] usertone = new int[8];
    private final int[][] instdata = { //instrument parameters
        usertone, //user tone register
        //i'm surprised no one's bothered to decap it and take a look
        //here's the latest one from rainwarrior aug.2012
        {0x03, 0x21, 0x05, 0x06, 0xB8, 0x82, 0x42, 0x27},
        {0x13, 0x41, 0x13, 0x0D, 0xD8, 0xD6, 0x23, 0x12},
        {0x31, 0x11, 0x08, 0x08, 0xFA, 0x9A, 0x22, 0x02},
        {0x31, 0x61, 0x18, 0x07, 0x78, 0x64, 0x30, 0x27},
        {0x22, 0x21, 0x1E, 0x06, 0xF0, 0x76, 0x08, 0x28},
        {0x02, 0x01, 0x06, 0x00, 0xF0, 0xF2, 0x03, 0xF5},
        {0x21, 0x61, 0x1D, 0x07, 0x82, 0x81, 0x16, 0x07},
        {0x23, 0x21, 0x1A, 0x17, 0xCF, 0x72, 0x25, 0x17},
        {0x15, 0x11, 0x25, 0x00, 0x4F, 0x71, 0x00, 0x11},
        {0x85, 0x01, 0x12, 0x0F, 0x99, 0xA2, 0x40, 0x02},
        {0x07, 0xC1, 0x69, 0x07, 0xF3, 0xF5, 0xA7, 0x12},
        {0x71, 0x23, 0x0D, 0x06, 0x66, 0x75, 0x23, 0x16},
        {0x01, 0x02, 0xD3, 0x05, 0xA3, 0x92, 0xF7, 0x52},
        {0x61, 0x63, 0x0C, 0x00, 0x94, 0xAF, 0x34, 0x06},
        {0x21, 0x62, 0x0D, 0x00, 0xB1, 0xA0, 0x54, 0x17}
    };
    //TODO: make instrument data a properly parsed static enum of some kind, that will clean up 90% of this bit shifting crap!
    private final static int[] logsin = genlogsintbl(), exp = genexptbl(),
            am = genamtbl();
    private final static double[] multbl = {0.5, 1, 2, 3, 4, 5,
        6, 7, 8, 9, 10, 10, 12, 12, 15, 15}, vib = genvibtbl();
    private final static int[] keyscaletbl = {0, 1536, 2048, 2368, 2560,
        2752, 2880, 3008, 3072, 3200, 3264, 3328, 3392, 3456, 3520, 3584
    };

    public VRC7SoundChip() {
////        some debug code to make a scope view:
//        DebugUI d = new DebugUI(512,480);
//        BufferedImage b = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
//        d.run();
////        adsr[] est = {adsr.ATTACK};
////        double[] evl = {511};
//
//
//        for (int i = 0; i < 1024; ++i) {
////            setenvelope(instdata[2], est, evl, 0, false);
////            setenvelope(instdata[2], est, evl, 0, false);
////            setenvelope(instdata[2], est, evl, 0, false);
////            setenvelope(instdata[2], est, evl, 0, false);
//            double j = tri(i/50.);
//            //int k = (int) evl[0];
//            b.setRGB(i / 4, clamp((int)((-j)*90) + 128), 0xFF0000);
//           // b.setRGB(i / 4, clamp((k) / 4), 0x00FFFF);
//            //b.setRGB(i / 4, clamp(-i + 128), 0x00FF00);
//            b.setRGB(i / 4, clamp(128), 0xffffff);
//        }
//        d.setFrame(b);
        Arrays.fill(modenv_state, adsr.CUTOFF);
        Arrays.fill(carenv_state, adsr.CUTOFF);
        Arrays.fill(modenv_vol, 511);
        Arrays.fill(carenv_vol, 511);
    }

    public static int clamp(final int a) {
        return (a != (a & 0xff)) ? ((a < 0) ? 0 : 255) : a;
    }

    private static double[] genvibtbl() {
        //vibrato wavetable. Yes this is a waste of memory. sue me.
        //from looking at genplus gx code that vibrato depth is supposed
        //to vary per octave but exactly how is complex.

        double l = 1789773 / 6.;
        double f = 6.4;
        int depth = 10; //blatant guess
        double[] tbl = new double[(int) Math.ceil(l / f)];
        for (int x = 0; x < tbl.length; ++x) {
            tbl[x] = (depth * tri(2 * Math.PI * f * x / l));
        }
        return tbl;
    }

    private static int[] genamtbl() {
        double l = 1789773 / 6.;
        double f = 3.7;
        int depth = 128;
        int[] tbl = new int[(int) Math.ceil(l / f)];//one full cycle of wave
        for (int x = 0; x < tbl.length; ++x) {
            tbl[x] = (int) (depth * tri(2 * Math.PI * f * x / l) + depth);
            //should be a triangle wave?
        }
        return tbl;
    }

    private static double tri(double x) {
        //triangle wave function.
        x %= 2 * Math.PI;
        if (x < (Math.PI / 2)) {
            return x / (Math.PI);
        } else if (x < (3 * Math.PI) / 2) {
            return 1 - (x / (Math.PI));
        } else {
            return x / (Math.PI) - 2;
        }
    }

    private static int[] genlogsintbl() {
        int[] tbl = new int[256];
        for (int i = 0; i < tbl.length; ++i) {
            //y = round(-log(sin((x+0.5)*pi/256/2))/log(2)*256)
            //see https://docs.google.com/Doc?id=dd8kqn9f_13cqjkf4gp for info
            tbl[i] = (int) Math.round(-Math.log(Math.sin((i + 0.5) * Math.PI / 256 / 2)) / Math.log(2) * 256);
        }
        return tbl;
    }

    private static int[] genexptbl() {
        int[] tbl = new int[256];
        for (int i = 0; i < tbl.length; ++i) {
            //y = round((power(2, x/256)-1)*1024)
            tbl[i] = (int) Math.round((Math.pow(2, i / 256.) - 1) * 1024.);
        }
        return tbl;
    }

    @Override
    public final void write(int register, int data) {
        switch (register) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
                //parameters for instrument 0 (user settable instrument)
                usertone[register & 7] = data;
                break;
            case 0x10:
            case 0x11:
            case 0x12:
            case 0x13:
            case 0x14:
            case 0x15: //frequency registers for ch. 0-5
                int n = register - 0x10;
                freq[n] = (freq[n] & 0xf00) | data;
                break;
            case 0x20:
            case 0x21:
            case 0x22:
            case 0x23:
            case 0x24:
            case 0x25: // ???tooof
//f: Upper bit of frequency
//o: Octave Select 
//t: Channel keying on/off (key on = note starts, key off: note decays).
//?: bit 5 is sustain, 6 and 7 unused
                int m = register - 0x20;
                octave[m] = (data >> 1) & 7;
                freq[m] = (freq[m] & 0xff) | ((data & 1) << 8);
                if (getbit(data, 4) && !key[m]) {
                    //when note is keyed on
                    carenv_state[m] = adsr.CUTOFF;
                    modenv_state[m] = adsr.CUTOFF;
                    // printarray(key);
                }
                key[m] = getbit(data, 4);
                sust[m] = getbit(data, 5);
                break;
            case 0x30:
            case 0x31:
            case 0x32:
            case 0x33:
            case 0x34:
            case 0x35: //top 4 bits instrument number, bottom 4 volume
                int j = register - 0x30;
                vol[j] = data & 0xf;
                //System.err.println(j + " " + hex(data));
                instrument[j] = (data >> 4) & 0xf;
                break;
            default:
            //System.err.println(hex(register) + " doesn't exist " + hex(data));
        }
    }
    int ch = 0;

    @Override
    public final void clock(final int cycle) {
        //chip runs at 3.58 mhz, but this operates at 1.789
        //because i do the modulator and carrier in a single cycle
        //as opposed to doing them alternate cycles like the real one
        //actual chip on the nes runs at 3.6 mhz with a separate clock
        //code is running at a higher sample rate than the real chip

        for (int i = 0; i < cycle; ++i) {
            ch = (ch + 1) % (6 * 6);
            if (ch < 6) {
                operate();
            }
        }
    }

    private void operate() {
        fmctr = (fmctr + 1) % vib.length;
        amctr = (amctr + 1) % am.length;
        phase[ch] += (1 / (256. * 2.)) * (freq[ch] << (octave[ch]));
        //Tuned this with audacity so it's definitely ok this time.
        phase[ch] %= 1024;
        int[] inst = instdata[instrument[ch]];
        //envelopes
        //TODO: rewrite the whole envelope code
        //to match rate chip is actually running envelope updates at
        for (int fi = 0; fi < 6; ++fi) {
            setenvelope(inst, modenv_state, modenv_vol, ch, false);
            setenvelope(inst, carenv_state, carenv_vol, ch, true);
        }
        //key scaling
        int keyscale = keyscaletbl[freq[ch] >> 5] - 512 * (7 - octave[ch]);
        if (keyscale < 0) {
            keyscale = 0;
        }
        int modks = inst[2] >> 6;
        modks = (modks == 0) ? 0 : (keyscale >> (3 - modks));
        int carks = (inst[3] >> 6);
        carks = (carks == 0) ? 0 : (keyscale >> (3 - carks));
        int fb = (~inst[3] & 7);
        //now the operator cells
        //invaluable info: http://gendev.spritesmind.net/forum/viewtopic.php?t=386
        //http://www.smspower.org/maxim/Documents/YM2413ApplicationManual
        //http://forums.nesdev.com/viewtopic.php?f=3&t=9102
        final double modVibrato = getbit(inst[0], 6) ? vib[fmctr] * (1 << octave[ch]) : 0;
        final double modFreqMultiplier = multbl[inst[0] & 0xf];
        final int modFeedback = (fb == 7) ? 0 : (mod[ch] + oldmodout[ch]) >> (2 + fb);
        //no i don't know why it adds the last 2 old outputs but MAME
        //does it that way and the feedback doesn't sound right w/o it
        final int mod_f = modFeedback + (int) (modVibrato + modFreqMultiplier * phase[ch]);
        //each of these values is an attenuation value
        final int modVol = (inst[2] & 0x3f) * 32;//modulator vol
        final int modEnvelope = ((int) modenv_vol[ch]) << 2;
        final int modAM = getbit(inst[0], 7) ? am[amctr] : 0;
        final boolean modRectify = getbit(inst[3], 3);
        //calculate modulator operator value
        mod[ch] = operator(mod_f, (int) (modVol + modEnvelope + modks + modAM), modRectify) << 2;
        oldmodout[ch] = mod[ch];
        //now repeat most of that for the carrier
        final double carVibrato = getbit(inst[1], 6) ? vib[fmctr] * (freq[ch] << octave[ch]) / 512. : 0;
        final double carFreqMultiplier = multbl[inst[1] & 0xf];
        final int carFeedback = (mod[ch] + oldmodout[ch]) >> 1; //inaccurately named
        final int car_f = carFeedback + (int) (carVibrato + carFreqMultiplier * phase[ch]);
        final int carVol = vol[ch] * 128; //4 bits for carrier vol not 6
        final int carEnvelope = ((int) carenv_vol[ch]) << 2;
        final int carAM = getbit(inst[1], 7) ? am[amctr] : 0;
        final boolean carRectify = getbit(inst[3], 4);
        out[ch] = operator(car_f, (int) (carVol + carEnvelope + carks + carAM), carRectify) << 2;
        outputSample();
    }

    private int operator(final int phase, final int gain, final boolean rectify) {
        return exp((logsin(phase, rectify) + gain));
    }

    private int exp(int val) {
        //perform e^x function on 13 bit fp output value using the hardware table on the chip
        //value should never be negative; if it is, find out why.
//        if (val < 0) {
//            val = 0;
//            System.err.println("why");
//           
//        }
        //values saturate instead of rolling over in the actual hardware
        if (val > (1 << 13) - 1) {
            val = (1 << 13) - 1;
        }
        //val &= (1 << 12); //wrong, but it makes all the waves into square waves! woo!
        int mantissa = exp[(-val & 0xff)];
        int exponent = (-val) >> 8;
//        int a = (int) Math.scalb(mantissa + 1024, exponent) * s; //correct but slow
        int b = ((((mantissa + 1024) >> (-exponent)))) * s; //not correct for negative #s
        return b;
    }
    private int s; // ugly hackish sign flag

    private int logsin(final int x, final boolean rectify) {
        //s stores sign of the output, in actual hw the sign bypasses everything else and
        //goes directly to the dac.
        switch ((x >> 8) & 3) {
            case 0:
                s = 1;
                return logsin[(x & 0xff)];
            case 1:
                s = 1;
                return logsin[255 - (x & 0xff)];
            case 2:
                s = rectify ? 0 : -1;
                return logsin[(x & 0xff)];
            case 3:
            default:
                s = rectify ? 0 : -1;
                return logsin[255 - (x & 0xff)];
        }
    }
    int lpaccum = 0;

    private void outputSample() {
        int sample = (out[0] + out[1] + out[2] + out[3] + out[4] + out[5]) * 3;
        sample += lpaccum;
        lpaccum -= sample >> 2;
    }

    @Override
    public final int getval() {
        return lpaccum;
    }
    final private static int ZEROVOL = 511;
    final private static int MAXVOL = 0;

    private void setenvelope(final int[] instrument, final adsr[] state, final double[] vol, final int ch, final boolean isCarrier) {
        final boolean keyscaleRate = getbit(instrument[(isCarrier ? 1 : 0)], 4);
        final int ksrShift = keyscaleRate ? octave[ch] << 1 : octave[ch] >> 1;
        //^ the key scaling bit (java should really have unions, this is such a mess)
        /*TODO: fix all of this. Most of these constants were calculated from a 
         badly translated YM2413 technical manual and are objectively wrong.
        
         The key scaling stuff is similarly just a best guess.
        
         Of course the real hardware isn't using floating point here either.
         */

        //from docs on the OPL3: envelope starts at 511 and counts down to zero (no attenuation)
        switch (state[ch]) {
            default:
            case CUTOFF:
                if (vol[ch] < ZEROVOL) {
                    vol[ch] += 2; //the programmer's manual suggests that sound has to
                    //decay back to zero volume when keyed on, but other references don't say this
                } else {
                    vol[ch] = ZEROVOL;
                    if (key[ch]) {
                        state[ch] = adsr.ATTACK;
                        phase[ch] = 0;
                        //reset phase to avoid popping? can't tell if the chip does this.
                        //i think it doesn't, but it does sound better if I do.
                    }
                }
                break;
            case ATTACK:
                if (vol[ch] > MAXVOL + 0.01) {
                    //((vol[ch] + 17) / 272)
                    //or
                    // (1 + (((int)vol[ch]) >> 4) )
                    vol[ch] -= ((vol[ch] + 17) / 272) * attack_tbl[
                            (instrument[(isCarrier ? 5 : 4)] >> 4) * 4
                            + ksrShift];
                } else {
                    state[ch] = adsr.DECAY;
                }
                if (!key[ch]) {
                    state[ch] = adsr.RELEASE;
                }
                break;
            case DECAY:
                if (vol[ch] < ((instrument[(isCarrier ? 7 : 6)] >> 4)) * 32) {
                    //not entirely sure whether higher value = more attenuation 
                    //or more volume here. docs are unclear.
                    //opl3 site suggests it's more volume.
                    vol[ch] += decay_tbl[
                            (instrument[(isCarrier ? 5 : 4)] & 0xf) * 4
                            + ksrShift];
                } else {
                    state[ch] = adsr.RELEASE;
                }
                if (!key[ch]) {
                    state[ch] = adsr.RELEASE;
                }
                break;
            case RELEASE:
                //release at std rate if key is off
                if (!key[ch] && vol[ch] < ZEROVOL) {
                    if (sust[ch]) {
                        vol[ch] += 0.001;
                    } else {
                        vol[ch] += .005;
                    }
                } else if (vol[ch] < ZEROVOL) {
                    if (getbit(instrument[isCarrier ? 1 : 0], 5)) {
                        //sustain on, don't decay until keyed
                        if (!key[ch]) {
                            state[ch] = adsr.SUSTRELEASE;
                        }
                    } else {
                        //decay immediately
                        vol[ch] += decay_tbl[(instrument[(isCarrier ? 7 : 6)] & 0xf) * 4
                                + ksrShift];
                    }

                }
                break;
            case SUSTRELEASE:
                if (vol[ch] < ZEROVOL) {
                    if (sust[ch]) {
                        vol[ch] += 0.0001;
                    } else {
                        vol[ch] += decay_tbl[(instrument[(isCarrier ? 7 : 6)] & 0xf) * 4
                                + ksrShift];
                    }
                }
                break;
        }
        if (vol[ch] < MAXVOL) {
            vol[ch] = MAXVOL;
        }
        if (vol[ch] > ZEROVOL) {
            vol[ch] = ZEROVOL;
        }
    }
    private final static double[] attack_tbl = {0, 0, 0, 0,
        0.00147964, 0.001827788, 0.002219467, 0.002589363,
        0.00295653, 0.003280789, 0.004438896, 0.005178727,
        0.005918528, 0.007147843, 0.009131117, 0.010357663,
        0.011837056, 0.014622722, 0.017718715, 0.020728745,
        0.023675206, 0.029243774, 0.035121416, 0.041430652,
        0.046655732, 0.058487549, 0.071032186, 0.082847896,
        0.094709582, 0.121904762, 0.142064373, 0.165695793,
        0.189349112, 0.234003656, 0.284128746, 0.331606218,
        0.378698225, 0.468007313, 0.567627494, 0.663212435,
        0.775757576, 0.934306569, 1.137777778, 1.32642487,
        1.514792899, 1.868613139, 2.265486726, 2.639175258,
        3.047619048, 3.657142857, 4.266666667, 4.740740741,
        5.12, 6.095238095, 7.529411765, 8.533333333,
        9.142857143, 11.63636364, 14.22222222, 18.28571429,
        511, 511, 511, 511,
        511, 511, 511, 511,
        511, 511, 511, 511,
        511, 511, 511, 511,
        511, 511, 511, 511,
        511, 511, 511, 511,};
    private final static double[] decay_tbl = {0, 0, 0, 0,
        0.000122332, 0.000152316, 0.000175261, 0.000211944,
        0.000244665, 0.000304632, 0.000365559, 0.000425651,
        0.00048933, 0.000609264, 0.000731117, 0.000851302,
        0.000978661, 0.001173833, 0.00146223, 0.001702603,
        0.001957321, 0.002437051, 0.002924478, 0.003405206,
        0.003914672, 0.004874148, 0.005848888, 0.006808873,
        0.007829225, 0.009748296, 0.011698044, 0.013620644,
        0.01565845, 0.01949585, 0.023396088, 0.027242737,
        0.031318816, 0.038994669, 0.046792177, 0.054479677,
        0.063888196, 0.07992507, 0.093567251, 0.108982546,
        0.125244618, 0.156002438, 0.188235294, 0.21787234,
        0.250489237, 0.31181486, 0.374269006, 0.436115843,
        0.500978474, 0.624390244, 0.748538012, 0.870748299,
        1.003921569, 1.248780488, 1.497076023, 1.741496599,
        2.015748031, 2.015748031, 2.015748031, 2.015748031,
        2.015748031, 2.015748031, 2.015748031, 2.015748031,
        //last lines duplicated to account for key scaling
        2.015748031, 2.015748031, 2.015748031, 2.015748031,
        2.015748031, 2.015748031, 2.015748031, 2.015748031,
        2.015748031, 2.015748031, 2.015748031, 2.015748031,
        2.015748031, 2.015748031, 2.015748031, 2.015748031};
}
//these 2 tables calculated from excel based on the envelope table
//in the programming guide.
