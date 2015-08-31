package com.grapeshot.halfnes.ui;

import static com.grapeshot.halfnes.utils.*;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

/**
 * @author Stephen Chin <steveonjava@gmail.com>
 */
class GpioControllerImpl {
  
  private static final Pin[] BUTTON_PINS = {
    RaspiPin.GPIO_02, // GPIO 27
    RaspiPin.GPIO_03, // GPIO 22
    RaspiPin.GPIO_04, // GPIO 23
    RaspiPin.GPIO_05, // GPIO 24
    RaspiPin.GPIO_06, // GPIO 25
    RaspiPin.GPIO_25, // GPIO 26
  };
  
  private static final int[] BUTTON_BIT = {
    BIT1, // B
    BIT0, // A
    BIT6, // left
    BIT5, // down
    BIT7, // right
    BIT4, // up
  };
  
  private static final int SELECT = BIT2;
  private static final int MAGIC_SELECT = BIT6 | BIT7;
  private static final int START = BIT3;
  private static final int MAGIC_START = BIT4 | BIT5;

  private final GpioController gpio;
  private int gpiobyte = 0;
  private final GpioPinDigitalInput[] buttons = new GpioPinDigitalInput[BUTTON_PINS.length];

  public GpioControllerImpl() {
    gpio = GpioFactory.getInstance();
    for (int i = 0; i < BUTTON_PINS.length; i++) {
      final int bx = i;
      buttons[bx] = gpio.provisionDigitalInputPin(BUTTON_PINS[bx], PinPullResistance.PULL_UP);
      buttons[bx].setDebounce(20);
      buttons[bx].addListener((GpioPinListenerDigital) event -> {
        if (event.getState().isLow()) { // press
          gpiobyte |= BUTTON_BIT[bx];
          if ((gpiobyte & MAGIC_START) == MAGIC_START) {
            gpiobyte &= ~MAGIC_START;
            gpiobyte |= START;
          }
          if ((gpiobyte & MAGIC_SELECT) == MAGIC_SELECT) {
            gpiobyte &= ~MAGIC_SELECT;
            gpiobyte |= SELECT;
          }
        } else { // release
          if ((gpiobyte & START) != 0 && (BUTTON_BIT[bx] & MAGIC_START) != 0) {
            gpiobyte &= ~START;
            gpiobyte |= MAGIC_START;
          }
          if ((gpiobyte & SELECT) != 0 && (BUTTON_BIT[bx] & MAGIC_SELECT) != 0) {
            gpiobyte &= ~SELECT;
            gpiobyte |= MAGIC_SELECT;
          }
          gpiobyte &= ~BUTTON_BIT[bx];
        }
      });
    }
  }

  int getByte() {
    return gpiobyte;
  }
  
}
