/*
 * HalfNES by Andrew Hoffman
 * Licensed under the GNU GPL Version 3. See LICENSE file
 */
package com.grapeshot.halfnes;

import java.util.prefs.Preferences;

/**
 *
 * @author Andrew
 */
public class PrefsSingleton {

    private static Preferences instance = null;

    protected PrefsSingleton() {
        // Exists only to defeat instantiation.
    }

    public synchronized static Preferences get() {
        if (instance == null) {
            instance = Preferences.userNodeForPackage(com.grapeshot.halfnes.NES.class);
        }
        return instance;
    }
}
