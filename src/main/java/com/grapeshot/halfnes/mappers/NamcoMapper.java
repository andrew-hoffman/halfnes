/*
 * HalfNES by Andrew Hoffman
 * Licensed under the GNU GPL Version 3. See LICENSE file
 */
package com.grapeshot.halfnes.mappers;

import com.grapeshot.halfnes.audio.*;
import com.grapeshot.halfnes.utils;
import java.util.Arrays;

/**
 *
 * @author Andrew
 */
public class NamcoMapper extends Mapper {

    private int soundAddr = 0;
    private boolean autoincrement = false, irqenable = false,
            interrupted = false, chrramenable0 = false, chrramenable1 = false;
    Namco163SoundChip sound = new Namco163SoundChip();
    private boolean hasInitSound = false;
    private int irqcounter = 0x3fff;
    private int[] chrbanks = new int[8], chr_ram = new int[16384];

    @Override
    public void loadrom() throws BadMapperException {
        super.loadrom();
        // needs to be in every mapper. Fill with initial cfg
        for (int i = 1; i <= 32; ++i) {
            //map last banks in to start off
            prg_map[32 - i] = prgsize - (1024 * i);
        }
        for (int i = 0; i < 8; ++i) {
            chr_map[i] = (1024 * i) & (chrsize - 1);
        }
    }

    @Override
    public int cartRead(final int addr) {
        if (addr >= 0x4800 && addr < 0x5000) {
            //read sound ram
            if (!hasInitSound) {
                cpuram.apu.addExpnSound((ExpansionSoundChip) sound);
                hasInitSound = true;
            }
            int retval = sound.read(soundAddr);
            if (autoincrement) {
                soundAddr = ++soundAddr & 0x7f;
            }
            return retval;
        } else if (addr < 0x5800) {
            irqack();
            return irqcounter & 0xff;
        } else if (addr < 0x6000) {
            //read high bits of irq ctr and enable bit and ack irqs
            irqack();
            return ((irqcounter >> 8) & 0x7f) | (irqenable ? 0x80 : 0);
        } else if (addr >= 0x8000) {
            return prg[prg_map[((addr & 0x7fff)) >> 10] + (addr & 1023)];
        } else if (addr >= 0x6000 && hasprgram) {
            return prgram[addr & 0x1fff];
        }
        return addr >> 8; //open bus
    }

    public final void cartWrite(final int addr, final int data) {
        if (addr < 0x4800 || ((addr >= 0x6000) && (addr < 0x8000)) || addr > 0xffff) {
            //need to add WRAM protection here
            super.cartWrite(addr, data);
            return;
        } else if (addr <= 0x4fff) {
            //write to sound chip
            if (!hasInitSound) {
                cpuram.apu.addExpnSound((ExpansionSoundChip) sound);
                hasInitSound = true;
            }
            sound.write(soundAddr, data);
            if (autoincrement) {
                soundAddr = ++soundAddr & 0x7f;
            }
        } else if (addr <= 0x57ff) {
            //irq counter low bits
            irqcounter &= 0x7f00;
            irqcounter |= data;
            irqack();
        } else if (addr <= 0x5fff) {
            //irq counter high 7 bits           
            irqcounter &= 0xff;
            irqcounter |= ((data & 0x7f) << 8);
            irqenable = ((data & (utils.BIT7)) != 0);
            irqack();
            //and bit 7 is irq enable
        } else if (addr <= 0xbfff) {
            //select chr pages
            int bank = (addr >> 11) & 7;
            setppubank(1, bank, data);
            chrbanks[bank] = data;
            //note: pages E0-FF are chr ram if a bit is set
        } else if (addr <= 0xc7ff) {
            //nametable select (can map chr rom in: how?)
            //on namco 175 this is PRG RAM enable instead?
            if (data < 0xe0) {
                //use chr rom as the nametable
                //i hope it doesnt try to write while it's chr rom
                nt0 = Arrays.copyOfRange(chr, (data * 1024), (data + 1) * 1024);
            } else {
                nt0 = (((data & (utils.BIT0)) != 0) ? pput1 : pput0);
            }
        } else if (addr <= 0xc8ff) {
            //nametable select 2
            if (data < 0xe0) {
                nt1 = Arrays.copyOfRange(chr, (data * 1024), (data + 1) * 1024);
            } else {
                nt1 = (((data & (utils.BIT0)) != 0) ? pput1 : pput0);
            }
        } else if (addr <= 0xd7ff) {
            //nametable select 3
            if (data < 0xe0) {
                nt2 = Arrays.copyOfRange(chr, (data * 1024), (data + 1) * 1024);
            } else {
                nt2 = (((data & (utils.BIT0)) != 0) ? pput1 : pput0);
            }
        } else if (addr <= 0xdfff) {
            //nametable select 4
            if (data < 0xe0) {
                nt3 = Arrays.copyOfRange(chr, (data * 1024), (data + 1) * 1024);
            } else {
                nt3 = (((data & (utils.BIT0)) != 0) ? pput1 : pput0);
            }
        } else if (addr <= 0xe7ff) {
            //prg select 1 (1st 6 bits) and mirroring
            for (int i = 0; i < 8; ++i) {
                prg_map[i] = (1024 * (i + 8 * (data & 63))) % prgsize;
            }
        } else if (addr <= 0xefff) {
            //prg select 2 (1st 6 bits) and CHR RAM enable
            for (int i = 0; i < 8; ++i) {
                prg_map[i + 8] = (1024 * (i + 8 * (data & 63))) % prgsize;
            }
            chrramenable0 = !((data & (utils.BIT6)) != 0);
            chrramenable1 = !((data & (utils.BIT7)) != 0);
        } else if (addr <= 0xf7ff) {
            for (int i = 0; i < 8; ++i) {
                prg_map[i + 16] = (1024 * (i + 8 * (data & 63))) % prgsize;
            }
            //prg select 3 (1st 6 bits)
        } else if (addr <= 0xffff) {
            //write protect for prg ram on namco 163
            //also sound address port (7 bits)
            autoincrement = ((data & (utils.BIT7)) != 0);
            soundAddr = data & 0x7f;
        }
    }

    private void irqack() {
        if (interrupted) {
            --cpu.interrupt;
            interrupted = false;
        }

    }

    @Override
    public void cpucycle(int cycles) {
        irqcounter += cycles;
        if (irqcounter > 0x7fff) {
            irqcounter = 0x7fff;
        }
        if (irqcounter == 0x7fff && irqenable && !interrupted) {
            ++cpu.interrupt;
            interrupted = true;
        }
    }

    private void setppubank(final int banksize, final int bankpos, final int banknum) {
//        System.err.println(banksize + ", " + bankpos + ", "+ banknum);
        for (int i = 0; i < banksize; ++i) {
            chr_map[i + bankpos] = (1024 * ((banknum) + i)) & (chrsize - 1);
        }
//        utils.printarray(chr_map);
    }

    @Override
    public int ppuRead(int addr) {
        //i can't find any games that use this additional chr ram
        //so who knows if this works?
        if (addr < 0x1000) {
            if (chrramenable0 && chrbanks[addr >> 10] > 0xe0) {
                return chr_ram[chr_map[addr >> 10] + (addr & 1023)];
            } else {
                return chr[chr_map[addr >> 10] + (addr & 1023)];
            }
        } else if (addr < 0x2000) {
            if (chrramenable1 && chrbanks[addr >> 10] > 0xe0) {
                return chr_ram[chr_map[addr >> 10] - (0xe0 << 10) + (addr & 1023)];
            } else {
                return chr[chr_map[addr >> 10] + (addr & 1023)];
            }
        } else {
            return super.ppuRead(addr);
        }
    }

    @Override
    public void ppuWrite(int addr, final int data) {
        addr &= 0x3fff;
        if (addr < 0x1000) {
            if (chrramenable0 && chrbanks[addr >> 10] > 0xe0) {
                chr_ram[chr_map[addr >> 10] - (0xe0 << 10) + (addr & 1023)] = data;
            } else {
                chr[chr_map[addr >> 10] + (addr & 1023)] = data;
            }
        } else if (addr < 0x2000) {
            if (chrramenable1 && chrbanks[addr >> 10] > 0xe0) {
                chr_ram[chr_map[addr >> 10] - (0xe0 << 10) + (addr & 1023)] = data;
            } else {
                chr[chr_map[addr >> 10] + (addr & 1023)] = data;
            }
        } else {
            super.ppuWrite(addr, data);
        }
    }
}