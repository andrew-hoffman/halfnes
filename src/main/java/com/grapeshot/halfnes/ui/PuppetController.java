package com.grapeshot.halfnes.ui;

import static com.grapeshot.halfnes.utils.BIT0;
import static com.grapeshot.halfnes.utils.BIT1;
import static com.grapeshot.halfnes.utils.BIT2;
import static com.grapeshot.halfnes.utils.BIT3;
import static com.grapeshot.halfnes.utils.BIT4;
import static com.grapeshot.halfnes.utils.BIT5;
import static com.grapeshot.halfnes.utils.BIT6;
import static com.grapeshot.halfnes.utils.BIT7;

/**
 * Created by skaggsm on 12/5/15.
 */
public class PuppetController implements ControllerInterface {
    private int latchbyte = 0, controllerbyte = 0, outbyte = 0;

    @Override
    public void strobe() {
        //shifts a byte out
        outbyte = latchbyte & 1;
        latchbyte = ((latchbyte >> 1) | 0x100);
    }

    @Override
    public void output(boolean state) {
        latchbyte = controllerbyte;
    }

    @Override
    public int peekOutput() {
        return latchbyte;
    }

    @Override
    public int getbyte() {
        return outbyte;
    }

    public void resetButtons() {
        controllerbyte = 0;
    }

    public void releaseButton(Button button) {
        switch (button) {
            case UP:
                controllerbyte &= ~BIT4;
                break;
            case DOWN:
                controllerbyte &= ~BIT5;
                break;
            case LEFT:
                controllerbyte &= ~BIT6;
                break;
            case RIGHT:
                controllerbyte &= ~BIT7;
                break;
            case A:
                controllerbyte &= ~BIT0;
                break;
            case B:
                controllerbyte &= ~BIT1;
                break;
            case SELECT:
                controllerbyte &= ~BIT2;
                break;
            case START:
                controllerbyte &= ~BIT3;
                break;
        }
    }

    public void pressButton(Button button) {
        switch (button) {
            case UP:
                controllerbyte |= BIT4;
                break;
            case DOWN:
                controllerbyte |= BIT5;
                break;
            case LEFT:
                controllerbyte |= BIT6;
                break;
            case RIGHT:
                controllerbyte |= BIT7;
                break;
            case A:
                controllerbyte |= BIT0;
                break;
            case B:
                controllerbyte |= BIT1;
                break;
            case SELECT:
                controllerbyte |= BIT2;
                break;
            case START:
                controllerbyte |= BIT3;
                break;
        }
    }

    public enum Button {
        UP, DOWN, LEFT, RIGHT, A, B, SELECT, START
    }
}
