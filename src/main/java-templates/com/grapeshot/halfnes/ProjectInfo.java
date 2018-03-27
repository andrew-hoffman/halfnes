package com.grapeshot.halfnes;

/**
 * Specifies the version and url as defined in the POM
 *
 */
public interface ProjectInfo {
    String VERSION = "${project.version}";
    String URL = "${project.url}";
}
