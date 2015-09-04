/*
 * HalfNES by Andrew Hoffman
 * Licensed under the GNU GPL Version 3. See LICENSE file
 */
package com.grapeshot.halfnes.mappers;

public class BadMapperException extends Exception {

    public String e;

    public BadMapperException(String e) {
        this.e = e;
    }

    @Override
    public String getMessage() {
        return e;
    }
}
