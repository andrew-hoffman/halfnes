/*
 * HalfNES by Andrew Hoffman
 * Licensed under the GNU GPL Version 3. See LICENSE file
 */
package com.grapeshot.halfnes.ui;

/**
 *
 * @author Andrew
 */
public interface FrameLimiterInterface {

    public void sleep();

    public void sleepFixed();
    
    public void setInterval(long ns);
}
