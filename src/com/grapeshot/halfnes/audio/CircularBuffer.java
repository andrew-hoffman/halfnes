/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.grapeshot.halfnes.audio;

/**
 *
 * @author Andrew
 */
public class CircularBuffer {

    int[] buffer;
    int bufsize;
    int write_ptr;
    int read_ptr;

    public CircularBuffer(int size) {
        //gives you a circular FIFO of indicated size.
        //size rounded up to nearest power of two. Limited to 2^26 b/c that's 256mb
        for (int i = 0; i < 26; ++i) {
            int q = (int) Math.pow(2, i);
            if (q > (size)) {
                buffer = new int[q];
                bufsize = q;
                break;
            }
        }
        if (buffer == null) {
            throw new UnsupportedOperationException("Buffer too large");
        }
        write_ptr = size - 2;
        read_ptr = 0;
    }

    public void write(final int data) {
        buffer[write_ptr] = data;
        ++write_ptr;
        write_ptr &= (bufsize - 1);
    }

    public int read() {
        final int retval = buffer[read_ptr];
        ++read_ptr;
        read_ptr &= (bufsize - 1);
        return retval;
    }

    public void advanceRead(int amt) {
        read_ptr = (read_ptr + amt) & (bufsize - 1);
    }

    public void advanceWrite(int amt) {
        write_ptr = (write_ptr + amt) & (bufsize - 1);
    }

    public int peek() {
        return buffer[read_ptr];
    }
}
