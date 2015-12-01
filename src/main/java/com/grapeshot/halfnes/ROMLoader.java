/*
 * HalfNES by Andrew Hoffman
 * Licensed under the GNU GPL Version 3. See LICENSE file
 */
package com.grapeshot.halfnes;

import com.grapeshot.halfnes.mappers.BadMapperException;
import com.grapeshot.halfnes.mappers.Mapper;

public class ROMLoader {
    //this is the oldest code in the project... I'm honestly ashamed
    //at how it's structured but for now it works.
    //TODO: fix this up

    //this SHOULD do just enough to figure out the file type
    //and the correct mapper for it, and no more.
    public String name;
    public int prgsize;
    public int chrsize;
    public Mapper.MirrorType scrolltype;
    public Mapper.TVType tvtype;
    public int mappertype;
    public int submapper;
    public int prgoff, chroff;
    public boolean savesram = false;
    public int[] header;
    private final int[] therom;

    public ROMLoader(String filename) {
        therom = FileUtils.readfromfile(filename);
        name = filename;
    }

    private void ReadHeader(int len) {
        // iNES header is 16 bytes, nsf header is 128,
        //other headers increasingly large
        header = new int[len];
        System.arraycopy(therom, 0, header, 0, len);
    }

    public void parseHeader() throws BadMapperException {
        ReadHeader(16);
        // decode iNES 1.0 headers
        // 1st 4 bytes : $4E $45 $53 $1A
        if (header[0] == 'N' && header[1] == 'E'
                && header[2] == 'S' && header[3] == 0x1A) {
            //a valid iNES file, proceed

            scrolltype = ((header[6] & (utils.BIT3)) != 0)
                    ? Mapper.MirrorType.FOUR_SCREEN_MIRROR
                    : ((header[6] & (utils.BIT0)) != 0)
                            ? Mapper.MirrorType.V_MIRROR
                            : Mapper.MirrorType.H_MIRROR;
            savesram = ((header[6] & (utils.BIT1)) != 0);
            mappertype = (header[6] >> 4);
            //detect NES 2.0 format for the rest of the header
            if (((header[7] >> 2) & 3) == 2) {
                System.err.println("NES 2 format");
                //nes 2
                //mapper buts 4-7 in byte 7
                mappertype += ((header[7] >> 4) << 4);
                //mapper bits 8-12 in byte 8
                mappertype += ((header[8] & 15) << 8);
                //submapper number is the high 4 bits of byte 8
                submapper = (header[8] >> 4);
                //extra prg and chr bits in byte 9
                prgsize = Math.min(therom.length - 16,
                        16384 * (header[4] + ((header[9] & 15) << 8)));
                if (prgsize == 0) {
                    throw new BadMapperException("No PRG ROM size in header");
                }
                chrsize = Math.min(therom.length - 16 - prgsize,
                        8192 * (header[5] + ((header[9] >> 4) << 8)));
                //prg ram size in header byte 10
                //chr ram size byte 11
                //tv type is byte 12
                if ((header[12] & 3) == 1) {
                    //pal mode only rom
                    tvtype = Mapper.TVType.PAL;
                    System.err.println("pal");
                } else {
                    //if ntsc only or works on both we'll use ntsc
                    tvtype = Mapper.TVType.NTSC;
                }
                //byte 13 is Vs. System palettes that i don't deal with yet
                //byte 14 and 15 must be zero

            } else {
                //nes 1 format, with hacks
                prgsize = Math.min(therom.length - 16, 16384 * header[4]);
                if (prgsize == 0) {
                    throw new BadMapperException("No PRG ROM size in header");
                    //someone made this field zero on a 4mb multicart ROM
                    //and someone ELSE made this zero for an 8k PRG dump (no-intro)
                    //so if anyone gets this error make some heuristics to fix it.
                    //basically no multicarts > 2mb in iNES 1.0 format
                }
                chrsize = Math.min(therom.length - 16 - prgsize, 8192 * header[5]);
                if (header[11] + header[12] + header[13] + header[14]
                        + header[15] == 0) {
                    //only consider upper bytes of mapper # if the end bytes are zero
                    mappertype += ((header[7] >> 4) << 4);
                    if (((header[9] & (utils.BIT0)) != 0)) {
                        //detect tv type though it's not really used
                        tvtype = Mapper.TVType.PAL;
                        System.err.println("pal header type 1");
                    } else if ((header[10] & 3) == 2) {
                        tvtype = Mapper.TVType.PAL;
                        System.err.println("pal header type 2");
                    } else {
                        tvtype = Mapper.TVType.NTSC;
                    }
                } else {
                    System.err.println("diskdude (please clean your roms)");
                    tvtype = Mapper.TVType.NTSC;
                }
            }
            // calc offsets; header not incl. here
            prgoff = 0;
            chroff = 0 + prgsize;
        } else if (('N' == header[0]) && ('E' == header[1])
                && ('S' == header[2]) && ('M' == header[3])
                && (0x1a == header[4])) {
            //nsf file
            mappertype = -1;
            //reread header since it's 128 bytes
            ReadHeader(128);
            prgsize = therom.length - 128;
        } else if (header[0] == 'U') {
            throw new BadMapperException("This is a UNIF file with the wrong extension");
        } else {
            throw new BadMapperException("iNES Header Invalid");
        }
    }

    public int[] load(int size, int offset) {
        int[] bindata = new int[size];
        System.arraycopy(therom, offset + header.length, bindata, 0, size);
        return bindata;
    }

    public int romlen() {
        return therom.length - header.length;
    }
}
