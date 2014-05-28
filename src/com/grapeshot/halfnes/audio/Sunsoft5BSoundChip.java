/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.grapeshot.halfnes.audio;

import com.grapeshot.halfnes.utils;

import javax.sound.sampled.*;

/**
 *
 * @author Andrew
 */
public class Sunsoft5BSoundChip implements ExpansionSoundChip {
    //not complete... missing volume envelopes and noise channel at the moment.
    //sound test for Gimmick - Hold Select, push Start on title screen

    private final Timer[] timers = {new SquareTimer(32), new SquareTimer(32), new SquareTimer(32)};
    private boolean[] enable = {false, false, false};
    private boolean[] useenvelope = {false, false, false};
    private int[] volume = {0, 0, 0};
    int enval = 0;
    private int[] volumetbl = getvoltbl();

    @Override
    public final void write(final int register, final int data) {
        //System.err.println(register + " " + data);
        switch (register) {
            case 0:
                timers[0].setperiod((timers[0].getperiod() & 0xf00) + data);
                break;
            case 1:
                timers[0].setperiod((timers[0].getperiod() & 0xff) + ((data & 0xf) << 8));
                break;
            case 2:
                timers[1].setperiod((timers[1].getperiod() & 0xf00) + data);
                break;
            case 3:
                timers[1].setperiod((timers[1].getperiod() & 0xff) + ((data & 0xf) << 8));
                break;
            case 4:
                timers[2].setperiod((timers[2].getperiod() & 0xf00) + data);
                break;
            case 5:
                timers[2].setperiod((timers[2].getperiod() & 0xff) + ((data & 0xf) << 8));
                break;
            case 7:
                for (int i = 0; i < 3; ++i) {
                    enable[i] = !(utils.getbit(data, i));
                }
                break;
            case 8:
                volume[0] = data & 0xf;
                useenvelope[0] = utils.getbit(data, 4);
                break;
            case 9:
                volume[1] = data & 0xf;
                useenvelope[1] = utils.getbit(data, 4);
                break;
            case 10:
                volume[2] = data & 0xf;
                useenvelope[2] = utils.getbit(data, 4);
                break;
            case 13:
                enval = 15;
                break;
            default:
            //System.err.println("Unrecognized register write to " + register + " ," + data);
        }
    }

    @Override
    public final void clock(final int cycle) {
        clockenvelope(cycle);
        timers[0].clock(cycle);
        timers[1].clock(cycle);
        timers[2].clock(cycle);
    }

    public final int getval() {
        final int mixvol = (enable[0] ? ((useenvelope[0] ? enval : volumetbl[volume[0]]) * timers[0].getval()) : 0)
                + (enable[1] ? ((useenvelope[1] ? enval : volumetbl[volume[1]]) * timers[1].getval()) : 0)
                + (enable[2] ? ((useenvelope[2] ? enval : volumetbl[volume[2]]) * timers[2].getval()) : 0);
        return mixvol;
    }

    public static int[] getvoltbl() {
        //the AY-3-8910 volume levels are NOT linear, but logarithmic.
        int[] vols = new int[16];
        for (int i = 0; i < 16; ++i) {
            vols[i] = (int) (90 * Math.pow(1.4, i));
            //matches YMZ284 datasheet info
        }
        //utils.printarray(vols);
        return vols;
    }

    private void clockenvelope(final int cycles) {
        enval = 0; //gimmick only uses the envelope to mute a channel.
    }
}
