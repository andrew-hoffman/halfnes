package com.grapeshot.halfnes.mappers;

public class Mapper86 extends Mapper {

    @Override
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
    public final void cartWrite(final int addr, final int data) {
        if (addr >= 0x6000 && addr <= 0x6FFF) {
            int prgselect = (data >> 4) & 3;
            int chrselect = (data & 3) | ((data & 0x40) >> 4);

            //remap CHR bank
            for (int i = 0; i < 8; ++i) {
                chr_map[i] = (1024 * (i + 8 * chrselect)) & (chrsize - 1);
            }
            //remap PRG bank
            for (int i = 0; i < 32; ++i) {
                prg_map[i] = (1024 * (i + 32 * prgselect)) & (prgsize - 1);
            }
        } else if (addr >= 0x7000 && addr <= 0x7FFF) {
            if ((data & 0x30) != 0x20) {
                return;
            }
            //TODO: add sound control by using external sound files
            switch (data & 0x1F) {
                case 0:
                    System.out.println("\"Strike!\"");
                    break;
                case 1:
                    System.out.println("\"Ball!\"");
                    break;
                case 2:
                    System.out.println("\"Time!\"");
                    break;
                case 3:
                    System.out.println("\"Out!\"");
                    break;
                case 4:
                    System.out.println("\"Safe!\"");
                    break;
                case 5:
                    System.out.println("\"Foul!\"");
                    break;
                case 6:
                    System.out.println("\"Fair!\"");
                    break;
                case 7:
                    System.out.println("\"You're out!\"");
                    break;
                case 8:
                    System.out.println("\"Play ball!\"");
                    break;
                case 9:
                    System.out.println("\"Ball 4!\"");
                    break;
                case 10:
                    System.out.println("\"Home run!\"");
                    break;
                case 11:
                    System.out.println("\"New pitcher\"");
                    break;
                case 12:
                    System.out.println("\"Ouch!\"");
                    break;
                case 13:
                    System.out.println("\"Dummy!\"");
                    break;
                case 14:
                    System.out.println("*crack*");
                    break;
                case 15:
                    System.out.println("*cheer*");
                    break;
            }
        }
    }
}
