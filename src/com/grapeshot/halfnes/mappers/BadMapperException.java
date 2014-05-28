package com.grapeshot.halfnes.mappers;
//HalfNES, Copyright Andrew Hoffman, October 2010

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Andrew
 */
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
