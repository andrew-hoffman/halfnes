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

    private static final String[] NATIVE_LIBRARIES = new String[]{
            // Windows
            "jinput-dx8.dll",
            "jinput-dx8_64.dll",
            "jinput-raw.dll",
            "jinput-raw_64.dll",
            "jinput-wintab.dll",
            "jinput-wintab.dll",
            // Linux
            "libjinput-linux.so",
            "libjinput-linux64.so",
            // OsX (Mac)
            "libjinput-osx.jnilib",
    };

    public static void main(String[] args) throws IOException {
        setupJInputNatives();
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set system look and feel. Meh.");
        }
        new SwingUI(args);
    }

    private static void setupJInputNatives() {
        try {
            File nativesDirectory = createTempDirectory();
            unpackNativeLibraries(nativesDirectory);
            setLibraryPath(nativesDirectory);
        } catch (Exception exception) {
            throw new RuntimeException("Unable to setup native libraries.");
        }
    }

    private static void unpackNativeLibraries(File nativesDirectory) throws IOException {
        for (String nativeLibrary : NATIVE_LIBRARIES) {
            unpackNativeLibrary(nativesDirectory, nativeLibrary);
        }
    }

    private static void unpackNativeLibrary(File nativesDirectory, String nativeLibrary) throws IOException {
        InputStream nativeLibraryInputStream = ClassLoader.getSystemResourceAsStream(nativeLibrary);
        File nativeLibraryTempFile = new File(nativesDirectory, nativeLibrary);
        nativeLibraryTempFile.deleteOnExit();
        BufferedOutputStream nativeLibraryTempFileOutputStream = new BufferedOutputStream(new FileOutputStream(nativeLibraryTempFile));
        byte[] buffer = new byte[4096];
        while (nativeLibraryInputStream.read(buffer) != -1) {
            nativeLibraryTempFileOutputStream.write(buffer);
        }
        nativeLibraryTempFileOutputStream.close();
    }

    private static File createTempDirectory() throws IOException {
        File nativeDirectory = Files.createTempDirectory("halfNES-natives").toFile();
        nativeDirectory.deleteOnExit();
        return nativeDirectory;
    }

    private static void setLibraryPath(final File nativesDirectory) throws NoSuchFieldException, IllegalAccessException {
        System.setProperty("java.library.path", nativesDirectory.getAbsolutePath());
        final Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
        fieldSysPath.setAccessible(true);
        fieldSysPath.set(null, null);
    }

}
