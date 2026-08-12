@echo off
echo Running Checkpoint 1 verification...
echo.

echo 1. Compile Debug
call ./gradlew.bat :app:compileDebugKotlin --console=plain > .bob/checkpoint1-compile.log 2>&1
echo Compile Exit: %ERRORLEVEL%

echo.
echo 2. Run Unit Tests (inc. Guard Tests)
call ./gradlew.bat :app:testDebugUnitTest --console=plain > .bob/checkpoint1-test.log 2>&1
echo Test Exit: %ERRORLEVEL%

echo.
echo 3. Assemble APK
call ./gradlew.bat :app:assembleDebug --console=plain > .bob/checkpoint1-assemble.log 2>&1
echo Assemble Exit: %ERRORLEVEL%

echo.
echo Done. You can check the logs in .bob/
