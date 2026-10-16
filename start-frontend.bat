@echo off
chcp 65001 >nul
echo ========================================
echo 家庭智慧药箱管理系统 - 前端启动脚本
echo ========================================
echo.

echo [1/3] 检查Node.js...
node -v
if %errorlevel% neq 0 (
    echo 错误: 未找到Node.js，请先安装Node.js 18+
    pause
    exit /b 1
)
echo.

echo [2/3] 进入前端目录...
cd frontend
if %errorlevel% neq 0 (
    echo 错误: frontend目录不存在
    pause
    exit /b 1
)
echo.

echo [3/3] 检查依赖...
if not exist "node_modules" (
    echo 首次运行，正在安装依赖...
    echo 这可能需要几分钟时间...
    npm install
    if %errorlevel% neq 0 (
        echo 错误: 依赖安装失败
        echo 请尝试: npm config set registry https://registry.npmmirror.com
        pause
        exit /b 1
    )
)
echo.

echo ========================================
echo 前端服务启动中...
echo 访问地址: http://localhost:3000
echo 按 Ctrl+C 停止服务
echo ========================================
echo.
echo 注意: 请确保已根据 FRONTEND_IMPLEMENTATION.md 创建所有必需文件
echo.

npm run dev
