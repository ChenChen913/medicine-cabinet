@echo off
echo ========================================
echo 设置 Java 17 环境并启动项目
echo ========================================

REM 设置 Java 17 环境变量
set JAVA_HOME=D:\java\jdk17
set PATH=%JAVA_HOME%\bin;%PATH%

echo.
echo 当前 Java 版本:
java -version

echo.
echo ========================================
echo 启动 Spring Boot 应用...
echo ========================================
mvn spring-boot:run
