@echo off
echo ========================================
echo KEYSTORE OLUSTURMA
echo ========================================
echo.
echo Bu script Android app signing icin keystore olusturur.
echo.

set JAVA_HOME=C:\SoundSTBoost\jdk-11.0.25+9

"%JAVA_HOME%\bin\keytool.exe" -genkey -v -keystore soundst-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias soundst-key

echo.
echo ========================================
echo Keystore olusturuldu: soundst-release-key.jks
echo ========================================
echo.
echo ONEMLI: Bu dosyayi ve sifrelerini guvenli bir yerde sakla!
echo Kaybedersen uygulamayi guncelleyemezsin!
echo.
pause
