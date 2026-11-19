@echo off
rem Compile and run (Windows)
if not exist out mkdir out
javac -d out -sourcepath src src\hospital\Main.java
if %errorlevel%==0 java -cp out hospital.Main
pause
