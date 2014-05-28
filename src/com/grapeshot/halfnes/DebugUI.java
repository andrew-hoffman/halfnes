package com.grapeshot.halfnes;
//HalfNES, Copyright Andrew Hoffman, October 2010

import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

public class DebugUI extends JFrame {
    // StrokeInformer aStrokeInformer = new StrokeInformer();

    private ShowFrame fbuf;
    private int xsize, ysize;
    private Repainter painter = new Repainter();

    public DebugUI(int height, int width) {
        this.xsize = height;
        this.ysize = width;
        fbuf = new ShowFrame();
        fbuf.setIgnoreRepaint(true);
    }

    public void run() {
        this.setTitle("HalfNES  Debug " + NES.VERSION);
        this.setResizable(false);
        this.setDefaultCloseOperation(HIDE_ON_CLOSE);
        this.setContentPane(fbuf);
        this.pack();
        this.setVisible(true);
    }

    public void messageBox(String s) {
        JOptionPane.showMessageDialog(fbuf, s);
    }

    public void setFrame(BufferedImage b) {
        fbuf.nextFrame = b;

        java.awt.EventQueue.invokeLater(painter);
        //do the actual screen update on the event thread, basically all this does is blit the new frame
    }

    public class Repainter implements Runnable {

        public void run() {
            fbuf.repaint();
        }
    }

    public class ShowFrame extends javax.swing.JPanel {

        public BufferedImage nextFrame;

        /**
         *
         */
        public ShowFrame() {
            this.setBounds(0, 0, xsize, ysize);
            this.setPreferredSize(new Dimension(xsize, ysize));
        }

        @Override
        public void paint(final Graphics g) {
            g.drawImage(nextFrame, 0, 0, xsize, ysize, null);
        }
    }
}
