package com.grapeshot.halfnes.mappers;
//HalfNES, Copyright Andrew Hoffman, October 2010
import com.grapeshot.halfnes.*;

/**
 *
 * @author Andrew
 */
public class BnromMapper extends Mapper {

    @Override
    public void loadrom() throws BadMapperException {
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
        if (addr < 0x8000 || addr > 0xffff) {
            super.cartWrite(addr, data);
            return;
        }
        //remap all 32k of PRG to 32 x bank #
        int bankstart = 32 * (data & 15);
        for (int i = 0; i < 32; ++i) {
            prg_map[i] = (1024 * (i + bankstart)) & (prgsize - 1);
        }
    }
}
