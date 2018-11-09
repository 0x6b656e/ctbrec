REM This script is just a wrapper to call the actual powershell script.
REM But you do something complete different here, too.
REM
REM If you want to use powershell, make sure, that your system allows the execution of powershell scripts:
REM 1. Open cmd.exe as administrator (Click on start, type cmd.exe, right-click on it and select "Run as administrator")
REM 2. Execute powershell
REM 3. Execute Set-ExecutionPolicy Unrestricted

@echo off	

set directory=%1
set file=%2
set model=%3
set site=%4
set unixtime=%5

powershell -F C:\Users\henrik\Desktop\ctbrec\pp.ps1 -dir "%directory%" -file "%file%" -model "%model%" -site "%site%" -time "%unixtime%"