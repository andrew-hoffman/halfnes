/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.grapeshot.halfnes;

import javax.swing.filechooser.FileFilter;
import java.io.FilenameFilter;
import java.io.File;

/**
 *
 * @author Andrew
 */
public class NESFileFilter implements FilenameFilter {

//    public boolean accept(File f) {
//        if (f.isDirectory()) {
//            return true;
//        }
//
//        String extension = utils.getExtension(f);
//        if (extension != null) {
//            if (extension.equalsIgnoreCase(".nes")
//                    || extension.equalsIgnoreCase(".fds")
//                    || extension.equalsIgnoreCase(".nsf")) {
//                return true;
//            } else {
//                return false;
//            }
//        }
//        return false;
//    }
    public String getDescription() {
        return ".NES, .FDS, .NSF, .ZIP";
    }

    public boolean accept(File dir, String name) {
        if (name.endsWith(".nes")
                || name.endsWith(".fds")
                || name.endsWith(".nsf")
                || name.endsWith(".zip")) {
            return true;
        } else {
            return false;
        }
    }
}
