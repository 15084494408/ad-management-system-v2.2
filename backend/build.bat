@echo off
chcp 65001 >nul
echo ======================================
echo   企业广告系统 - 打包脚本
echo ======================================
echo.

set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot
set MAVEN_HOME=D:\dev\apache-maven-3.9.6
set PATH=%MAVEN_HOME%\bin;%JAVA_HOME%\bin;%PATH%

cd /d "%~dp0"

echo 正在打包（跳过测试）...
call mvn clean package -DskipTests

if errorlevel 1 (
    echo ❌ 打包失败
    pause
    exit /b 1
)

echo.
echo ✅ 打包完成！
echo JAR 文件位于: target\ad-system-1.0.0.jar
echo.
echo 启动命令:
echo   java -jar target\ad-system-1.0.0.jar
echo.
pause
