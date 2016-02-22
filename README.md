[DOWNLOAD HERE](https://github.com/andrew-hoffman/halfnes/releases)

halfnes
=======

An accurate NES/Famicom emulator

[![Join the chat at https://gitter.im/andrew-hoffman/halfnes](https://badges.gitter.im/Join%20Chat.svg)]
(https://gitter.im/andrew-hoffman/halfnes?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Current Features
----------------

- Joystick support through both DirectInput and xInput (thanks Zlika) 
- Cross-Platform
- Supports Mapper 0, 1, 2, 3, 4, 5, 7, 9, 10, 11, 15, 19, 21, 22, 23, 24, 25, 26,
 31, 33, 34, 38, 41, 48, 58, 60, 61, 62, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73,
 75, 76, 78, 79, 86, 87, 88, 89, 92, 93, 94, 97, 107, 112, 113, 118, 119, 140,
 152, 154, 180, 182, 185, 200, 201, 203, 206, 212, 213, 214, 225, 226, 229, 231,
 240, 241, 242, 244, 246, 255
- Accurate sound core
- Fast display code
- Battery save support (No savestates! Come on. You can live without them.)
- Remappable controls
- Full screen mode 
- NTSC filter
- NSF player

Running HalfNES
---------------

Download the latest version from https://github.com/andrew-hoffman/halfnes/releases .
There are two versions of HalfNES included in this package: a Windows
executable and a JAR file for other platforms.
Use whichever one works best on your platform, but you will need
Java 8 or newer installed no matter what file is to be used.
Linux users will need to set execute permissions on the JAR.

# Default Controls (See Preferences dialog to remap them)
Controller 1:
- D-Pad: Arrow Keys
- B Button: Z
- A Button: X
- Select: Right Shift
- Start: Enter 

# Controller 2:
- D-Pad: WASD
- B Button: F
- A Button: G
- Select: R
- Start: T 

The keys mapped to the A and B buttons are used to change tracks in the NSF player.

#Note on joystick support

The first detected gamepad will be used as Controller 1, and the second 
will be Controller 2. Currently the buttons used are not configurable. 
(the controller needs to be plugged in before a game is loaded in order to be detected.)

#Compatibility

At this point in 
development, almost all US released games will start, but certain games 
still have graphics corruption or freezing problems. Please report any 
issues you encounter with the emulator or with games on the Github Issues page 
(https://github.com/andrew-hoffman/halfnes/issues). 
PAL games are now supported as well but are likely to have more issues.
Please change the system type to PAL in preferences to run these. 

Building instructions
---------------------

The project requires JInput library to build.  
The project comes with a Maven build script that will automatically download
that and package the natives as a library. To use it you will need to 
install Maven, change to the project directory and run

    mvn install

and that should produce an exe and a JAR with all the natives in the
/target/ directory under the project root. 

Do NOT ask me where to find ROM files of commercial games. Some public 
domain homebrew ROMs are available at www.pdroms.de for testing 
purposes. 

A 2 ghz Athlon 64 or better is currently required to run all games full 
speed. (The NTSC filter requires MUCH more processing power, however.)
Saved games are placed in the folder that the ROM file is in for 
now. 

If you are having problems getting the emulator to run, make sure to 
update your Java Runtime to the latest version. Go to 
http://java.com/en/download/manual.jsp and get the correct version for 
your OS. 

Special Thanks to the NESDev wiki and forum community for the invaluable 
NES hardware reference that made this project possible. 
