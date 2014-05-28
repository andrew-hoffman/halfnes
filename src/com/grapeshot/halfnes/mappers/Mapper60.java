package com.grapeshot.halfnes.mappers;

import com.grapeshot.halfnes.*;

public class Mapper60 extends Mapper {

    int reg = 0;

    @Override
    public void loadrom() throws BadMapperException {
        //needs to be in every mapper. Fill with initial cfg
        super.loadrom();
        //remap CHR bank
        for (int i = 0; i < 8; ++i) {
            chr_map[i] = (1024 * i) & (chrsize - 1);
        }
        //remap PRG bank
        for (int i = 0; i < 16; ++i) {
            prg_map[i] = (1024 * i) & (prgsize - 1);
        }
        for (int i = 0; i < 16; ++i) {
            prg_map[i + 16] = (1024 * i) & (prgsize - 1);
        }
    }

    @Override
    public void reset() {
        reg = (reg + 1) & 3;

        //remap CHR bank
        for (int i = 0; i < 8; ++i) {
            chr_map[i] = (1024 * (i + 8 * reg)) & (chrsize - 1);
        }
        //remap PRG bank
        for (int i = 0; i < 16; ++i) {
            prg_map[i] = (1024 * (i + 16 * reg)) & (prgsize - 1);
        }
        for (int i = 0; i < 16; ++i) {
            prg_map[i + 16] = (1024 * (i + 16 * reg)) & (prgsize - 1);
        }
    }
}
