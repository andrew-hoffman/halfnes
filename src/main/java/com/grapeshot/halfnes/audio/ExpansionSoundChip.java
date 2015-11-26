/*
 * HalfNES by Andrew Hoffman
 * Licensed under the GNU GPL Version 3. See LICENSE file
 */
package com.grapeshot.halfnes.audio;

/**
 *
 * @author Andrew
 */
public interface ExpansionSoundChip {

    public void clock(final int cycles);

    public void write(int register, int data);

    public int getval();
}
