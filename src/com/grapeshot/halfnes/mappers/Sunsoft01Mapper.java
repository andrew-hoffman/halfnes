package com.grapeshot.halfnes.mappers;
//HalfNES, Copyright Andrew Hoffman, October 2010

import com.grapeshot.halfnes.*;

public class Sunsoft01Mapper extends Mapper {

    private int lowBank = 0;
    private int highBank = 0;

    public void loadrom() throws BadMapperException {
        //needs to be in every mapper. Fill with initial cfg
        super.loadrom();
        for (int i = 0; i < 32; ++i) {
            prg_map[i] = (1024 * i) & (prgsize - 1);
        }
        for (int i = 0; i < 8; ++i) {
            chr_map[i] = (1024 * i) & (chrsize - 1);
        }
    }

    @Override
    public final void cartWrite(int addr, int data) {
        if (addr >= 0x6000 && addr < 0x8000) {
            lowBank = data & 7;
            highBank = (data >> 4) & 7;

            //remap CHR bank 0
            for (int i = 0; i < 4; ++i) {
                chr_map[i] = (1024 * (i + lowBank * 4)) % chrsize;
            }
            //remap CHR bank 1
            for (int i = 0; i < 4; ++i) {
                chr_map[4 + i] = (1024 * (i + highBank * 4)) % chrsize;
            }
        }
    }
}
