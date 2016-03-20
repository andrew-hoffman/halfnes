/*
 * HalfNES by Andrew Hoffman
 * Licensed under the GNU GPL Version 3. See LICENSE file
 */
package com.grapeshot.halfnes.mappers;

/**
 * Speed optimization for NROM games: copy everything to linear mapping and
 * don't use the bankswitching capability at all
 *
 * thanks to Stephen Chin - steveonjava@gmail.com
 */
public class NromMapper extends Mapper {

    @Override
    public void loadrom() throws BadMapperException {
        super.loadrom();
        //copy the whole rom around so we need to do less math

        int[] shiftedprg = new int[65536];
        System.arraycopy(prg, 0, shiftedprg, 0x8000, prg.length);
        if (prgsize <= 16384) {
            //double up the rom if 16k
            System.arraycopy(prg, 0, shiftedprg, 0xc000, prg.length);
        }
        prg = shiftedprg;
    }

    @Override
    public int cartRead(final int addr) {
        if (addr >= 0x8000) {
            return prg[addr];
        } else if (addr >= 0x6000 && hasprgram) {
            return prgram[addr & 0x1fff];
        }
        return addr >> 8; //open bus
    }

    @Override
    public int ppuRead(int addr) {
        if (addr < 0x2000) {
            //math is hard let's go shopping
            return chr[addr];
        } else {
            switch (addr & 0xc00) {
                case 0:
                    return nt0[addr & 0x3ff];
                case 0x400:
                    return nt1[addr & 0x3ff];
                case 0x800:
                    return nt2[addr & 0x3ff];
                case 0xc00:
                default:
                    if (addr >= 0x3f00) {
                        addr &= 0x1f;
                        if (addr >= 0x10 && ((addr & 3) == 0)) {
                            addr -= 0x10;
                        }
                        return ppu.pal[addr];
                    } else {
                        return nt3[addr & 0x3ff];
                    }
            }
        }
    }
}
