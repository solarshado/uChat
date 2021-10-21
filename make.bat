@echo off
del *.class
javac App.java || (pause && exit)
jar -cvfe uChat.jar App *.class

pause