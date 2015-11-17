/*
 * HalfNES by Andrew Hoffman
 * Licensed under the GNU GPL Version 3. See LICENSE file
 */
package com.grapeshot.halfnes.ui;

import com.grapeshot.halfnes.NES;

/**
 *
 * @author Andrew
 */
public interface GUIInterface extends Runnable {

    public NES getNes();

    public void setNES(NES nes);

    public void setFrame(int[] frame, int[] bgcolor, boolean dotcrawl);
    //Frame is now a 256x240 array with NES color numbers from 0-3F
    //plus the state of the 3 color emphasis bits in bits 7,8,9

    public void messageBox(String message);

    @Override
    public void run();

    public void render();

    public void loadROMs(String path);
}
