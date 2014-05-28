/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.grapeshot.halfnes.audio;

import com.grapeshot.halfnes.utils;

/**
 *
 * @author Andrew
 */
public class NoiseTimer extends Timer {

    private int divider = 0;
    private int[] values = genvalues(1, 1);
    private int prevduty = 1;
    private final static int periodadd = 0;

    public NoiseTimer() {
        period = 0;
    }

    @Override
    public void setduty(int duty) {
        if (duty != prevduty) {
            values = genvalues(duty, values[position]);
            position = 0;
        }
        prevduty = duty;
    }

    @Override
    public final void clock() {
        --divider;
        while (divider <= 0) {
            divider += period + periodadd;
            position = ++position % values.length;
        }
    }

    @Override
    public final int getval() {
        return (values[position] & 1);
    }

    @Override
    public final void reset() {
        position = 0;
    }

    @Override
    public final void clock(final int cycles) {
        divider -= cycles;
        while (divider <= 0) {
            divider += period + periodadd;
            position = ++position % values.length;
        }
    }

    @Override
    public final void setperiod(final int newperiod) {
        period = newperiod;
    }

    public static int[] genvalues(int whichbit, int seed) {
        int[] tehsuck = new int[(whichbit == 1) ? 32767 : 93];
        for (int i = 0; i < tehsuck.length; ++i) {
            seed = (seed >> 1)
                    | ((utils.getbit(seed, whichbit)
                    ^ utils.getbit(seed, 0))
                    ? 16384 : 0);
            tehsuck[i] = seed;
        }
        return tehsuck;

    }

    @Override
    public void setduty(int[] duty) {
        throw new UnsupportedOperationException("Not supported on noise channel.");
    }
}
