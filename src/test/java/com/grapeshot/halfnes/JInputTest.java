package com.grapeshot.halfnes;

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import org.testng.annotations.Test;

import java.util.Arrays;

/**
 * Created by KlausH on 29.11.2015.
 */
public class JInputTest {

    static {
        JInputHelper.setupJInput();
    }

    @Test
    public void testJInput() {
        ControllerEnvironment controllerEnvironment = ControllerEnvironment.getDefaultEnvironment();
        Controller[] controllers = controllerEnvironment.getControllers();
        System.out.println(String.format("%s controllers found.", controllers.length));
        Arrays.asList(controllers).forEach(controller -> {
            System.out.println(String.format("  %s (%s)", controller, controller.getType()));
        });
    }

}