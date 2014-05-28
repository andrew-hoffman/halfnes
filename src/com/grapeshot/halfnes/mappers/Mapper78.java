/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.grapeshot.halfnes.mappers;

import com.grapeshot.halfnes.*;

/**
 *
 * @author Andrew
 */
public class Mapper78 extends Mapper {

    @Override
    public void loadrom() throws BadMapperException {
        //needs to be in every mapper. Fill with initial cfg
        super.loadrom();
        for (int i = 1; i <= 32; ++i) {
            prg_map[32 - i] = prgsize - (1024 * i);
        }
        for (int i = 1; i <= 8; ++i) {
            chr_map[8 - i] = chrsize - (1024 * i);
        }
    }

    @Override
    public final void cartWrite(final int addr, final int data) {
        //System.out.println(data);
        if (addr < 0x8000 || addr > 0xffff) {
            super.cartWrite(addr, data);
            return;
        }
        int prgselect = data & 7;
        int chrselect = (data >> 4) & 0xf;
        if (crc == 0x42392440) //Cosmo Carrier
        {
            setmirroring(utils.getbit(data, 3) ? MirrorType.SS_MIRROR1 : MirrorType.SS_MIRROR0);
        } else //Holy Diver
        {
            setmirroring(utils.getbit(data, 3) ? MirrorType.V_MIRROR : MirrorType.H_MIRROR);
        }

        //remap CHR bank
        for (int i = 0; i < 8; ++i) {
            chr_map[i] = (1024 * (i + 8 * chrselect)) & (chrsize - 1);
        }
        //remap PRG bank
        for (int i = 0; i < 16; ++i) {
            prg_map[i] = (1024 * (i + 16 * prgselect)) & (prgsize - 1);
        }
    }
}
