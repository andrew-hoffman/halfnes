package com.grapeshot.halfnes.nestest;

import com.grapeshot.halfnes.NES;
import com.grapeshot.halfnes.mappers.BadMapperException;
import com.grapeshot.halfnes.ui.ControllerInterface;
import org.testng.annotations.Test;

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
        while (nes.runEmulation) {
            nes.frameAdvance();
        }
    }

}
