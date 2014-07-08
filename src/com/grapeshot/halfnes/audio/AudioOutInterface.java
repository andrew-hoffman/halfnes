/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
