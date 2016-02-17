package com.grapeshot.halfnes.nestest;

import com.grapeshot.halfnes.NES;
import com.grapeshot.halfnes.mappers.BadMapperException;
import com.grapeshot.halfnes.ui.ControllerInterface;
import org.testng.annotations.Test;
import static org.junit.Assert.assertEquals;

import static org.mockito.Mockito.mock;

/**
 * Created by KlausH on 28.11.2015.
 */
public class NesTest {

    @Test
    public void nesTest() throws BadMapperException {
        NES nes = new NES(null);
        nes.loadROM("src/test/resources/nestest/nestest.nes", 0xC000);
        nes.setControllers(mock(ControllerInterface.class), mock(ControllerInterface.class));

        //log all instructions executed to compare with real nestest.log
        nes.getCPU().startLog("src/test/resources/nestest/compare.log");
        while (nes.runEmulation) {
            //runs until hits a KIL opcode which is a few instructions after the
            //official log finishes.
            nes.frameAdvance();
        }
        //log should be at least 8992 lines
        //I don't actually compare the logs in this test yet.
        
        //check some bytes of RAM for successful test result values
        System.err.println(nes.getCPURAM().read(0));
        System.err.println(nes.getCPURAM().read(1));
        System.err.println(nes.getCPURAM().read(2));
        System.err.println(nes.getCPURAM().read(3));
        
        assertEquals(nes.getCPURAM().read(0), 0);
        assertEquals(nes.getCPURAM().read(1), 255);
        assertEquals(nes.getCPURAM().read(2), 255);
        assertEquals(nes.getCPURAM().read(3), 255);
    }

}
