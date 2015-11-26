copy .\dist\halfnes.jar halfnes-temp.jar

del HalfNES.jar
del HalfNES.exe
del HalfNES.app

java -jar .\proguard4.7\lib\proguard.jar @halfnes_autojar.pro

rem Reminder that the version of jarsplice used here is patched to take the right file extension for an OSX library (original programmer didn't look it up apparently)
java -jar jarsplice-0.40-CLI-CUSTPATCH.jar -fat HalfNES.jar -win HalfNES.exe -jars halfnes-temp.jar;.\lib\jinput.jar;.\lib\org.happy.library-1.3.jar;.\lib\libs\concurrent.jar;  -nats .\lib\libjinput-linux64.so;.\lib\libjinput-linux.so;.\lib\libjinput-osx.jnilib;.\lib\jinput-wintab.dll;.\lib\jinput-raw_64.dll;.\lib\jinput-raw.dll;.\lib\jinput-dx8_64.dll;.\lib\jinput-dx8.dll; -main com.grapeshot.halfnes.halfNES

echo Next time you make a release this should create the archive too.
pause

rem \lib\org.happy.library-1.3.jar;.\lib\libs\commons-collections-3.2.1.jar;.\lib\libs\guava-12.0.jar;.\lib\libs\commons-io-2.3.jar;.\lib\libs\junit-4.10.jar;.\lib\libs\poi-3.8-20120326.jar;.\lib\libs\concurrent.jar;.\lib\libs\xstream-1.3.1.jar;

