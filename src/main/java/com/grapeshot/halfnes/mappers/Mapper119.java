/*
 * HalfNES by Andrew Hoffman
 * Licensed under the GNU GPL Version 3. See LICENSE file
 */
package com.grapeshot.halfnes.mappers;

import com.grapeshot.halfnes.utils;

/**
 *
 * @author Andrew
 */
//TQROM mapper
//mmc3 derivative with chr ram and rom
public class Mapper119 extends MMC3Mapper {

    int[] chrRam = new int[8192];

    @Override
    public int ppuRead(final int addr) {
        if (addr < 0x2000) {
            checkA12(addr);
            return (chr_map[addr >> 10] > 65535)
                    ? chrRam[(chr_map[addr >> 10] + (addr & 1023)) & 8191]
                    : chr[(chr_map[addr >> 10] & 65535) + (addr & 1023)];
        } else {
            return super.ppuRead(addr);
        }
    }

    @Override
    public void ppuWrite(final int addr, final int data) {
        if (addr < 0x2000) {
            checkA12(addr);
            if (chr_map[addr >> 10] > 63) {
                chrRam[(chr_map[addr >> 10] + (addr & 1023)) & 8191] = data;
            }
        } else {
            super.ppuWrite(addr, data);
        }
    }

    @Override
    public void loadrom() throws BadMapperException {
        //needs to be in every mapper. Fill with initial cfg
        super.loadrom();
        //on startup:
        for (int i = 0; i < 8; ++i) {
            prg_map[i] = (1024 * i);
            prg_map[i + 8] = (1024 * i);
            //yes this actually matters; MMC3 does NOT start up in a random state
            //(at least Smash TV and TMNT3 expect certain banks w/o even setting up mapper)
        }
        for (int i = 1; i <= 32; ++i) {
            prg_map[32 - i] = prgsize - (1024 * i);
        }

        for (int i = 0; i < 8; ++i) {
            chr_map[i] = 0;
        }
        setbank6();
        //cpuram.setPrgRAMEnable(false);
    }

    protected void setppubank(int banksize, int bankpos, int banknum) {
        for (int i = 0; i < banksize; ++i) {
            chr_map[i + bankpos] = (1024 * ((banknum) + i));
        }
    }
}
