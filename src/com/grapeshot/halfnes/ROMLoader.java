package com.grapeshot.halfnes;
//HalfNES, Copyright Andrew Hoffman, October 2010

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
    public int mappertype;
    public int prgoff, chroff;
    public boolean savesram = false;
    public int[] header;
    private int[] therom;

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
            prgsize = 16384 * header[4];
            if (prgsize == 0) {
                throw new BadMapperException("No PRG ROM size in header");
                //someone made this field zero on a 4mb multicart ROM
                //and someone ELSE made this zero for an 8k PRG dump (no-intro)
                //so if anyone gets this error make some heuristics to fix it.
                //basically multicarts over 2mb won't ever work without a DB
            }
            chrsize = 8192 * header[5];
            scrolltype = utils.getbit(header[6], 3)
                    ? Mapper.MirrorType.FOUR_SCREEN_MIRROR
                    : utils.getbit(header[6], 0)
                    ? Mapper.MirrorType.V_MIRROR
                    : Mapper.MirrorType.H_MIRROR;
            savesram = utils.getbit(header[6], 1);
            mappertype = (header[6] >> 4);
            if (header[11] + header[12] + header[13] + header[14]
                    + header[15] == 0) {// fix for DiskDude
                mappertype += ((header[7] >> 4) << 4);
            }

            // calc offsets; header not incl. here
            prgoff = 0;
            chroff = 0 + prgsize;
        } else if (header[0] == 'N' && header[1] == 'E'
                && header[2] == 'S' && header[3] == 'M'
                && header[4] == 0x1a) {
            //nsf file
            mappertype = -1;
            //reread header since it's 128 bytes
            ReadHeader(128);
            prgsize = Math.min(32768, therom.length - 128);
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
