/*
 * HalfNES by Andrew Hoffman
 * Licensed under the GNU GPL Version 3. See LICENSE file
 */
package com.grapeshot.halfnes.audio;

/**
 *
 * @author Andrew
 */
public interface AudioOutInterface {

    public void outputSample(int sample);

    public void flushFrame(boolean waitIfBufferFull);

    public void pause();

    public void resume();

    public void destroy();

    public boolean bufferHasLessThan(int samples);
}
