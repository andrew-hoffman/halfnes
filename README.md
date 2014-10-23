halfnes
=======

An accurate NES/Famicom emulator

Current Features
----------------

- Joystick support through both DirectInput and xInput (thanks Zlika) 
- Cross-Platform
- Supports Mapper 0, 1, 2, 3, 4, 5, 7, 9, 10, 11, 15, 19, 21, 22, 23, 24, 25, 26,
 33, 34, 38, 41, 48, 58, 60, 61, 62, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 75,
 76, 78, 79, 86, 87, 88, 89, 92, 93, 94, 97, 107, 112, 113, 118, 119, 140, 152,
 154, 180, 182, 185, 200, 201, 203, 206, 212, 213, 214, 225, 226, 229, 231,
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
There are three versions of HalfNES included in this package: a Windows
executable, a Mac .app package, and a JAR file for other platforms.
Use whichever one works best on your platform, but you will need
Java installed no matter what file is to be used.
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
issues you encounter with the emulator or with games on the Google Code 
page (http://code.google.com/p/halfnes/). 

Building instructions
---------------------

The project requires two libraries to build: JInput (gamepad support) and The Happy Java Library 1.3 (parallel for loops). 
Sorry there's no decent build script included with this package, at this point you might as well just create a new Netbeans project, import all the src files, and add those two libraries in the project properties.

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
