package com.grapeshot.halfnes;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Created by KlausH on 29.11.2015.
 */
public enum JInputHelper {

    ;

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
        "libjinput-osx.jnilib",};

    public static void setupJInput() {
        try {
            File nativesDirectory = createTempDirectory();
            unpackNativeLibraries(nativesDirectory);
            setLibraryPath(nativesDirectory);
            fixInputPluginForWindows8();
        } catch (Exception exception) {
            throw new RuntimeException("Unable to setup native libraries.", exception);
        }
    }

    private static void unpackNativeLibraries(File nativesDirectory) throws IOException {
        for (String nativeLibrary : NATIVE_LIBRARIES) {
            unpackNativeLibrary(nativesDirectory, nativeLibrary);
        }
    }

    private static void unpackNativeLibrary(File nativesDirectory, String nativeLibrary) throws IOException {
        try (InputStream nativeLibraryInputStream = ClassLoader.getSystemResourceAsStream(nativeLibrary)) {
            File nativeLibraryTempFile = new File(nativesDirectory, nativeLibrary);
            nativeLibraryTempFile.deleteOnExit();
            if (!nativeLibraryTempFile.exists()) {
                try (BufferedOutputStream nativeLibraryTempFileOutputStream 
                        = new BufferedOutputStream(new FileOutputStream(nativeLibraryTempFile))) {
                    byte[] buffer = new byte[4096];
                    int length;
                    while ((length = nativeLibraryInputStream.read(buffer)) > 0) {
                        nativeLibraryTempFileOutputStream.write(buffer, 0, length);
                    }
                }
            }
        }
    }

    private static File createTempDirectory() throws IOException {

        String tmpdir = System.getProperty("java.io.tmpdir") + "/halfnes-" + NES.VERSION;
        System.err.println(tmpdir);
        File f = new File(tmpdir);
        if (!f.exists()) {
            f.mkdir();
        }

        //File nativeDirectory = Files.createTempDirectory("halfNES-natives").toFile();
        f.deleteOnExit();
        return f;
    }

    private static void setLibraryPath(final File nativesDirectory) throws NoSuchFieldException, IllegalAccessException {
        System.setProperty("java.library.path", nativesDirectory.getAbsolutePath());
        final Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
        fieldSysPath.setAccessible(true);
        fieldSysPath.set(null, null);
    }

    private static void fixInputPluginForWindows8() {
        AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
            String os = System.getProperty("os.name", "").trim();
            if (os.startsWith("Windows")) {
                if (os.startsWith("Windows 8")) {
                    // disable default plugin lookup
                    System.setProperty("jinput.useDefaultPlugin", "false");
                    // set to same as windows 7
                    System.setProperty("net.java.games.input.plugins", "net.java.games.input.DirectAndRawInputEnvironmentPlugin");
                }
                if (os.startsWith("Windows 10") || isWindows10()) {
                    // disable default plugin lookup
                    System.setProperty("jinput.useDefaultPlugin", "false");
                    // set fallback to AWT plugin
                    System.setProperty("net.java.games.input.plugins", "net.java.games.input.DirectAndRawInputEnvironmentPlugin");
                }
            }
            return null;
        });
    }

    private static boolean isWindows10() {
        try {
            Process process = Runtime.getRuntime().exec("cmd.exe /c ver");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            bufferedReader.readLine();
            String line = bufferedReader.readLine();
            process.waitFor();
            return line.contains("10");
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

}
