/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.grapeshot.halfnes.audio;

/**
 *
 * @author Andrew
 */
public class SquareTimer extends Timer {

    protected int[] values;
    final private int periodadd;
    private int divider = 0;

    public final void clock() {
        if (period + periodadd <= 0) {
            return;
        }
        --divider;
        while (divider <= 0) {
            divider += period + periodadd;
            position = ++position % values.length;
        }
    }

    public final void clock(final int cycles) {
        if (period < 8) {
            return;
        }
        divider -= cycles;
        while (divider <= 0) {
            divider += period + periodadd;
            position = ++position % values.length;
        }
    }

    public SquareTimer(final int ctrlen, final int periodadd) {
        this.periodadd = periodadd;
        values = new int[ctrlen];
        period = 0;
        position = 0;
        setduty(ctrlen / 2);
    }

    public SquareTimer(final int ctrlen) {
        this.periodadd = 0;
        values = new int[ctrlen];
        period = 0;
        position = 0;
        setduty(ctrlen / 2);
    }

    public final void reset() {
        position = 0;
    }

    public final void setduty(final int duty) {
        for (int i = 0; i < values.length; ++i) {
            values[i] = (i < duty) ? 1 : 0;
        }
    }

    public final void setduty(int[] dutyarray) {
        values = dutyarray;
    }

    public final int getval() {
        return values[position];
    }

    public final void setperiod(final int newperiod) {
        period = newperiod;
    }
}
