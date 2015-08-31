/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.grapeshot.halfnes;

import com.grapeshot.halfnes.cheats.Patch;
import com.grapeshot.halfnes.mappers.Mapper;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashMap;
import sun.misc.Unsafe;

/**
 *
 * @author Andrew Hoffman
 *
 *
 */
public final class CPURAM {

    static final Unsafe unsafe;
    static final int INTEGER_ARRAY_BASE_OFFSET;

    static {
        unsafe = (Unsafe) AccessController.doPrivileged(
            new PrivilegedAction<Object>() {
              @Override
              public Object run() {
                try {
                  Field f = Unsafe.class.getDeclaredField("theUnsafe");
                  f.setAccessible(true);
                  return f.get(null);
                } catch (Exception e) {
                  throw new Error();
                }
              }
            });

        INTEGER_ARRAY_BASE_OFFSET = unsafe.arrayBaseOffset(int[].class);
        if (unsafe.arrayIndexScale(int[].class) != 4) {
          throw new AssertionError();
        }
    }

    private boolean hasprgram = true;
    final int[] wram = new int[2048];
    Mapper mapper;
    public APU apu;
    PPU ppu; //need these to call their write handlers from here.
    private HashMap<Integer, Patch> patches = null;

    public CPURAM(final Mapper mappy) {
        mapper = mappy;
        // init memory
        Arrays.fill(wram, 0xff);
    }

//    public final int read(final int addr) {
//        if (patches != null) {
//            int retval = _read(addr);
//            Patch p = patches.get(addr);
//            if (p != null) {
//                if (p.getAddress() == addr && p.matchesData(retval)) {
//                    return p.getData();
//                }
//            }
//            return retval;
//        } else {
//            return _read(addr);
//        }
//    }

    public final int read(final int addr) {
        if (addr > 0x4018) {
            return mapper.cartRead(addr);
        } else if (addr <= 0x1fff) {
            return wram[addr & 0x7FF];
//            return unsafe.getInt(wram, INTEGER_ARRAY_BASE_OFFSET + (addr & 0x7FF) * 4);
        } else if (addr <= 0x3fff) {
            // 8 byte ppu regs; mirrored lots
            return ppu.read(addr & 7);
        } else if (0x4000 <= addr && addr <= 0x4018) {
            return apu.read(addr - 0x4000);
        } else {
            return addr >> 8; //open bus
        }
    }

    public final void write(final int addr, final int data) {
//        if((data & 0xff) != data){
//            System.err.println("DANGER WILL ROBINSON");
//        }
        if (addr > 0x4018) {
            mapper.cartWrite(addr, data);
        } else if (addr <= 0x1fff) {
            wram[addr & 0x7FF] = data;
        } else if (addr <= 0x3fff) {
            // 8 byte ppu regs; mirrored lots
            ppu.write(addr & 7, data);
        } else if (0x4000 <= addr && addr <= 0x4018) {
            apu.write(addr - 0x4000, data);
        }
    }

    public void setPrgRAMEnable(boolean b) {
        hasprgram = b;
    }

    public void setAPU(APU apu) {
        this.apu = apu;
    }

    public void setPPU(PPU ppu) {
        this.ppu = ppu;
    }

    public void setPatches(HashMap<Integer, Patch> p) {
        this.patches = p;
    }
}
