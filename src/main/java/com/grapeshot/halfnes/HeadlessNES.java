package com.grapeshot.halfnes;

import com.grapeshot.halfnes.ui.HeadlessUI;
import com.grapeshot.halfnes.ui.PuppetController;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;

/**
 * Created by skaggsm on 12/7/15.
 */
public class HeadlessNES {
    public static void main(String[] args) {
        BufferedImage bufferedImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB_PRE);
        HeadlessUI ui = new HeadlessUI("/Volumes/Data/Users/skaggsm/Desktop/nestest.nes", true);

        for (int i = 0; i < 100; i++) {
            ui.runFrame();
        }
        ui.getController1().pressButton(PuppetController.Button.START);
        for (int i = 0; i < 10; i++) {
            ui.runFrame();
        }
        ui.getController1().releaseButton(PuppetController.Button.START);
        for (int i = 0; i < 1; i++) {
            ui.runFrame();
        }

        BufferedImage image = ui.getLastFrame();

        JFrame frame = new JFrame("Display") {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                g.drawImage(image, 0, 0, image.getWidth() * 2, image.getHeight() * 2, this);
            }
        };
        frame.setSize(500, 500);
        frame.setVisible(true);
    }
}
