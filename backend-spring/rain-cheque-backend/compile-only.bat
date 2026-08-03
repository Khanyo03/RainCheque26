@echo off
cd /d c:\RainCheque\RainCheque26\backend-spring\rain-cheque-backend
call mvnw.cmd test-compile --no-transfer-progress > c:\RainCheque\compile-only-out.txt 2>&1
echo EXIT_CODE=%ERRORLEVEL% >> c:\RainCheque\compile-only-out.txt
