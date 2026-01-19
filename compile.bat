@echo off
echo Compiling Java files...
if not exist bin mkdir bin
javac -d bin src\*.java
if %errorlevel% == 0 (
    echo Compilation successful!
    echo Run with: java -cp bin MultiSizeExperimentRunner
) else (
    echo Compilation failed!
)
pause

