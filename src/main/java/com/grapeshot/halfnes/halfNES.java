/*
 * HalfNES by Andrew Hoffman
 * Licensed under the GNU GPL Version 3. See LICENSE file
 */
package com.grapeshot.halfnes;

import com.grapeshot.halfnes.ui.SwingUI;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import javax.swing.*;

public class halfNES {



    public static void main(String[] args) throws IOException {
        JInputHelper.setupJInputNatives();
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set system look and feel. Meh.");
        }
        new SwingUI(args);
    }

}
