package com.grapeshot.halfnes;

import com.grapeshot.halfnes.ui.HeadlessUI;
import com.grapeshot.halfnes.ui.PuppetController;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author Mitchell Skaggs
 */
public class HeadlessNES {
    
    private HeadlessNES() {}
    
    public static final int scale = 4;
    public static void main(String[] args) {
        BufferedImage bufferedImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB_PRE);
        HeadlessUI ui = new HeadlessUI("src/test/resources/nestest/nestest.nes", true);

        for (int i = 0; i < 100; i++) {
            ui.runFrame();
        }
        ui.getController1().pressButton(PuppetController.Button.START);
        ui.runFrame();
        ui.getController1().releaseButton(PuppetController.Button.START);
        for (int i = 0; i < 5; i++) {
            ui.runFrame();
        }

        BufferedImage image = ui.getLastFrame();

        JFrame frame = new JFrame("Display") {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                g.drawImage(image, 0, 0, image.getWidth() * scale, image.getHeight() * scale, this);
            }
        };
        frame.setSize(256 * scale, 224 * scale);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }
}
