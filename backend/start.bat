@echo off
chcp 65001 >nul
echo ======================================
echo   企业广告系统 - 后端启动脚本
echo ======================================
echo.

:: 设置环境变量
set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot
set MAVEN_HOME=D:\dev\apache-maven-3.9.6
set PATH=%MAVEN_HOME%\bin;%JAVA_HOME%\bin;%PATH%

:: 切换到脚本目录
cd /d "%~dp0"

echo [1/2] 正在编译项目...
call mvn clean compile -DskipTests
if errorlevel 1 (
    echo ❌ 编译失败，请检查错误信息
    pause
    exit /b 1
)

echo.
echo [2/2] 正在启动 Spring Boot...
echo.
echo 启动后访问：
echo   - API文档: http://localhost:8080/api/doc.html
echo   - Swagger:  http://localhost:8080/api/swagger-ui/index.html
echo.
echo 按 Ctrl+C 停止服务
echo.

call mvn spring-boot:run -DskipTests

pause
