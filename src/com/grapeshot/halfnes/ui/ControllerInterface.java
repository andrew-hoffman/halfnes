/*
 * HalfNES by Andrew Hoffman
 * Licensed under the GNU GPL Version 3. See LICENSE file
 */
package com.grapeshot.halfnes.ui;

/**
 *
 * @author Andrew
 */
public interface ControllerInterface {

    public void strobe();

    public void output(final boolean state);
    
    public int peekOutput();

    public int getbyte();
}
