/*
 * HalfNES by Andrew Hoffman
 * Licensed under the GNU GPL Version 3. See LICENSE file
 */
package com.grapeshot.halfnes.ui;

import java.util.HashMap;

import static com.grapeshot.halfnes.utils.BIT0;
import static com.grapeshot.halfnes.utils.BIT1;
import static com.grapeshot.halfnes.utils.BIT2;
import static com.grapeshot.halfnes.utils.BIT3;
import static com.grapeshot.halfnes.utils.BIT4;
import static com.grapeshot.halfnes.utils.BIT5;
import static com.grapeshot.halfnes.utils.BIT6;
import static com.grapeshot.halfnes.utils.BIT7;

/**
 * @author Andrew
 */
public class DummyController implements ControllerInterface {

    //i wrote this to test a bug in the menu of one game.
    //if using this again, maybe a parser and some RLE would be appropriate?
    //or just make it load FCEUX movie files?
    int outbyte = 0;
    int latchbyte = 0;
    char[] input = ("000000000000000000000000000000000000000000000000000000000000"
            + "000000000000000000000000000000000000000000000000000000000000000000"
            + "000000000000000000000000000000000000000000000000000000000000000000"
            + "000000000000000000000000000000000000000000000000000000000000000000"
            + "000000000000000000000000000000000000000000000000000000000000000000"
            + "000000000000000000000000000000000000000000000000000000000000000000"
            + "000000000000000000000000000000000000000000000000000000000000000000"
            + "000000000000000000000000000000000000000000000000000000000000000000"
            + "00000000000000000000000000000SSSSSSSSSSSSSS00000000000000000000000"
            + "00000000000000000000000000000AAAAAAAAAAAAAA00000000000000000000000"
            + "00000000000000000000000000000AAAAAAAAAAAAAA00000000000000000000000"
            + "00000000000000000000000000000AAAAAAAAAAAAAA00000000000000000000000"
            + "00000000000000000000000000000AAAAAAAAAAAAAA00000000000000000000000"
            + "00000000000000000000000000000AAAAAAAAAAAAAA00000000000000000000000"
            + "00000000000000000000000000000AAAAAAAAAAAAAA00000000000000000000000"
            + "00000000000000000000000000000AAAAAAAAAAAAAA00000000000000000000000"
            + "000000000000000000000000000000000000000000000000000000000000000000"
            + "000000000000000000000000000000000000000000000000000000000000000000"
            + "000000000000000000000000000000000000000000000000000000000000000000"
            + "000000000000000000000000000000000000000000000000000000000000000000"
            + "000000000000000000000000000000000000000000000000000000000000000000"
            + "000000000000000000000000000000000000000000000000000000000000000000"
            + "000000000000000000000000000000000000000000000000000000000000000000"
            + "000000000000000000000000000000000000000000000000000000000000000000"
            + "000000000000000000000000000000000000000000000000000000000000000000"
            + "000000000000000000000000000000000000000000000000000000000000000000"
            + "00000000000000000000000000000SSSSSSSSSSSSSS00000000000000000000000").toCharArray();
    HashMap<Character, Integer> m = new HashMap<>();
    int frame = 0;

    public DummyController(int controllernum) {
        m.put('0', 0x00); // Null
        m.put('U', BIT4); // Up
        m.put('D', BIT5); // Down
        m.put('L', BIT6); // Left
        m.put('R', BIT7); // Right
        m.put('A', BIT0); // A
        m.put('B', BIT1); // B
        m.put('s', BIT2); // Select
        m.put('S', BIT3); // Start
    }

    @Override
    public void strobe() {
        //shifts a byte out
        outbyte = latchbyte & 1;
        latchbyte = ((latchbyte >> 1) | 0x100);
    }

    @Override
    public void output(final boolean state) {
        if (frame < input.length) {
            latchbyte = m.get(input[frame]);
        } else {
            latchbyte = 0;
        }
        ++frame;
    }

    @Override
    public int peekOutput() {
        return latchbyte;
    }

    @Override
    public int getbyte() {
        return outbyte;
    }
}
