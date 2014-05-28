package com.grapeshot.halfnes.audio;
//HalfNES, Copyright Andrew Hoffman, October 2010

/**
 *
 * @author Andrew
 */
public abstract class Timer {

    protected int period;
    protected int position;

    public final int getperiod() {
        return period;
    }

    public abstract void setperiod(final int newperiod);

    public abstract void setduty(int duty);

    public abstract void setduty(int[] duty);

    public abstract void reset();

    public abstract void clock();

    public abstract void clock(final int cycles);

    public abstract int getval();
}
