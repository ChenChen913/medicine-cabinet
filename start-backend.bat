@echo off
chcp 65001 >nul
echo ========================================
echo 家庭智慧药箱管理系统 - 后端启动脚本
echo ========================================
echo.

echo [1/4] 检查Java版本...
java -version
if %errorlevel% neq 0 (
    echo 错误: 未找到Java，请先安装Java 17
    pause
    exit /b 1
)
echo.

echo [2/4] 检查Maven...
mvn -version
if %errorlevel% neq 0 (
    echo 错误: 未找到Maven，请先安装Maven
    pause
    exit /b 1
)
echo.

echo [3/4] 编译项目...
mvn clean compile -DskipTests
if %errorlevel% neq 0 (
    echo 错误: 编译失败，请检查错误信息
    echo.
    echo 常见问题:
    echo 1. Java版本不是17 - 请升级Java
    echo 2. 依赖下载失败 - 请检查网络连接
    echo.
    pause
    exit /b 1
)
echo.

echo [4/4] 启动后端服务...
echo.
echo ========================================
echo 后端服务启动中...
echo 访问地址: http://localhost:8080/api
echo 按 Ctrl+C 停止服务
echo ========================================
echo.

mvn spring-boot:run
