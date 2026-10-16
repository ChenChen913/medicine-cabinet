@echo off
chcp 65001 >nul
echo ========================================
echo 家庭智慧药箱管理系统 - 数据库初始化
echo ========================================
echo.

echo 请确保MySQL服务已启动！
echo.

set /p MYSQL_USER="请输入MySQL用户名 (默认: root): "
if "%MYSQL_USER%"=="" set MYSQL_USER=root

set /p MYSQL_PASSWORD="请输入MySQL密码: "

echo.
echo [1/2] 创建数据库...
mysql -u %MYSQL_USER% -p%MYSQL_PASSWORD% -e "CREATE DATABASE IF NOT EXISTS medicine_cabinet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
if %errorlevel% neq 0 (
    echo 错误: 数据库创建失败
    echo 请检查:
    echo 1. MySQL服务是否启动
    echo 2. 用户名和密码是否正确
    pause
    exit /b 1
)
echo 数据库创建成功！
echo.

echo [2/2] 执行建表脚本...
mysql -u %MYSQL_USER% -p%MYSQL_PASSWORD% medicine_cabinet < src\main\resources\sql\schema.sql
if %errorlevel% neq 0 (
    echo 错误: 建表脚本执行失败
    pause
    exit /b 1
)
echo 建表脚本执行成功！
echo.

echo ========================================
echo 数据库初始化完成！
echo ========================================
echo.
echo 数据库名称: medicine_cabinet
echo 表数量: 5个
echo - medicines (药品表)
echo - categories (类别表)
echo - medicine_categories (关联表)
echo - usage_logs (用药记录表)
echo - shopping_list (购物清单表)
echo.

echo 下一步:
echo 1. 修改 src\main\resources\application.yml 中的数据库配置
echo 2. 运行 start-backend.bat 启动后端服务
echo.

pause
