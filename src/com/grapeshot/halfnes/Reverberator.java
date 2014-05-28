/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.grapeshot.halfnes;

import com.grapeshot.halfnes.audio.CircularBuffer;

/**
 *
 * @author Andrew
 */
public class Reverberator implements AudioOutInterface {

    AudioOutInterface iface;
    CircularBuffer cb;
    double echo, lp_coef, hp_coef;

    public Reverberator(AudioOutInterface i, int length, double echo_gain, double lp_coef, double hp_coef) {
        this.echo = echo_gain;
        this.lp_coef = lp_coef;
        this.hp_coef = hp_coef;
        iface = i;
        cb = new CircularBuffer(length);
    }
    int lpaccum = 0;

    private int lowpass_filter(int sample) {
        sample += lpaccum;
        lpaccum -= sample * lp_coef;
        return lpaccum;
    }
    private int dckiller = 0;

    private int highpass_filter(int sample) {
        //for killing the dc in the signal
        sample += dckiller;
        dckiller -= sample * hp_coef;//the actual high pass part
        dckiller += (sample > 0 ? -1 : 1);//guarantees the signal decays to exactly zero
        return sample;
    }

    @Override
    public void outputSample(int sample) {
        sample -= cb.read() * echo;
        if (sample < -32768) {
            sample = -32768;
            //System.err.println("clip");
        }
        if (sample > 32767) {
            sample = 32767;
            //System.err.println("clop");
        }
        cb.write(lowpass_filter(highpass_filter(sample)));
        if (!(iface == null)) {
            iface.outputSample(sample);
        }
    }

    @Override
    public void flushFrame(boolean waitIfBufferFull) {
        if (!(iface == null)) {
            iface.flushFrame(waitIfBufferFull);
        }

    }

    @Override
    public void pause() {
        if (!(iface == null)) {
            iface.pause();
        }
    }

    @Override
    public void resume() {
        if (!(iface == null)) {
            iface.resume();
        }
    }

    @Override
    public void destroy() {
        if (!(iface == null)) {
            iface.destroy();
        }
    }

    @Override
    public boolean bufferHasLessThan(int samples) {
        if (!(iface == null)) {
            return iface.bufferHasLessThan(samples);
        } else {
            return false;
        }
    }
}
