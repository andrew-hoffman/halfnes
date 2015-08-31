package com.grapeshot.halfnes;

//HalfNES, Copyright Andrew Hoffman, October 2010
import com.grapeshot.halfnes.ui.GUIImpl;
import java.io.*;
import javax.swing.*;

public class halfNES {

    private static final long serialVersionUID = -7269569171056445433L;

    public static void main(String[] args) throws IOException {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            System.err.println("Could not set system look and feel. Meh: " + e);
        }
        NES nes = new NES(new GUIImpl());
        if (args == null || args.length < 1 || args[0] == null) {
            nes.run();
        } else {
            nes.run(args[0]);
        }

    }
}
