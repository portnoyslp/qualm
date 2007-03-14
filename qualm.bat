@ECHO OFF

SET CP=qualm.jar

REM Find any getopt JAR files and add them to the class path.
FOR %%f IN (*-getopt*.jar) DO SET CP=%CP%;%%f

java -cp %CP% qualm.Qualm %1 %2 %3 %4 %5 %6 %7 %8 %9
