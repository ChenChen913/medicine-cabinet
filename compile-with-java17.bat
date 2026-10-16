@echo off
echo ========================================
echo 设置 Java 17 环境并编译项目
echo ========================================

REM 设置 Java 17 环境变量
set JAVA_HOME=D:\java\jdk17
set PATH=%JAVA_HOME%\bin;%PATH%

echo.
echo 当前 Java 版本:
java -version

echo.
echo ========================================
echo 开始编译项目...
echo ========================================
mvn clean compile

echo.
echo ========================================
echo 编译完成！
echo ========================================
pause
