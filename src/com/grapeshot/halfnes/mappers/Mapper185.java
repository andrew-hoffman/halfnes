package com.grapeshot.halfnes.mappers;
//HalfNES, Copyright Andrew Hoffman, October 2010

import com.grapeshot.halfnes.*;

//CNROM with copy protection
public class Mapper185 extends Mapper {

    boolean chr_enabled = true;

    @Override
    public void loadrom() throws BadMapperException {
        //needs to be in every mapper. Fill with initial cfg
        super.loadrom();
        for (int i = 0; i < 16; ++i) {
            prg_map[i] = (1024 * i) & (prgsize - 1);
        }
        for (int i = 0; i < 8; ++i) {
            chr_map[i] = (1024 * i) & (chrsize - 1);
        }
    }

    @Override
    public int ppuRead(int addr) {
        if (!chr_enabled) {
            chr_enabled = true;
            return 0x12;
        }
        if (addr < 0x2000) {
            return chr[chr_map[addr >> 10] + (addr & 1023)];
        } else {
            return super.ppuRead(addr);
        }
    }

    @Override
    public final void cartWrite(final int addr, final int data) {
        if (addr < 0x8000 || addr > 0xffff) {
            super.cartWrite(addr, data);
            return;
        }
        //remap CHR bank
        for (int i = 0; i < 8; ++i) {
            chr_map[i] = (1024 * (i + 8 * (data & 3))) & (chrsize - 1);
            //copy protection
            chr_enabled = ((chr_map[i] & 0xF) > 0 && (chr_map[i] != 0x13));
        }
    }
}
