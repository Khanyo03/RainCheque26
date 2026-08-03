@echo off
cd /d c:\RainCheque\RainCheque26\backend-spring\rain-cheque-backend
call mvnw.cmd test -Dtest=com.raincheque.controller.TimeRangeValidationPropertyTest -DfailIfNoTests=false --no-transfer-progress > c:\RainCheque\pbt-tr-result.txt 2>&1
echo EXIT_CODE=%ERRORLEVEL% >> c:\RainCheque\pbt-tr-result.txt
