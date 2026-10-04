#!/usr/bin/env bash
# 一条命令启动:构建前端 -> 打包后端 -> 启动服务(含演示数据)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ---- 定位 JDK 与 Maven(优先使用仓库同级 tools 目录,其次系统 PATH)----
if [ -z "${JAVA_HOME:-}" ] && [ -x "$ROOT/../tools/jdk17/bin/java" ]; then
  export JAVA_HOME="$ROOT/../tools/jdk17"
fi
if [ -z "${JAVA_HOME:-}" ] && [ -x "/home/node/tools/jdk17/bin/java" ]; then
  export JAVA_HOME="/home/node/tools/jdk17"
fi
if [ -n "${JAVA_HOME:-}" ]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi
if [ -x "/home/node/tools/maven/bin/mvn" ]; then
  export PATH="/home/node/tools/maven/bin:$PATH"
fi

command -v java >/dev/null || { echo "错误: 未找到 Java,请安装 JDK 17+ 或设置 JAVA_HOME"; exit 1; }
command -v mvn  >/dev/null || { echo "错误: 未找到 Maven,请安装 Maven 3.9+"; exit 1; }
command -v npm  >/dev/null || { echo "错误: 未找到 npm,请安装 Node.js 18+"; exit 1; }

echo "==> [1/4] 构建前端 (Svelte + Vite)"
cd "$ROOT/frontend"
[ -d node_modules ] || npm install --no-audit --no-fund
npm run build

echo "==> [2/4] 拷贝前端产物到后端静态资源目录"
STATIC="$ROOT/backend/src/main/resources/META-INF/resources"
rm -rf "$STATIC"
mkdir -p "$STATIC"
cp -r "$ROOT/frontend/dist/." "$STATIC/"

echo "==> [3/4] 打包后端 (Quarkus + SQLite)"
cd "$ROOT/backend"
mkdir -p data
mvn -q -B package -DskipTests

echo "==> [4/4] 启动服务  http://localhost:8080"
echo "    演示账号数据已内置:张三(可签发)/ 李四(学时不足)/ 王五(需补考)"
exec java -jar "$ROOT/backend/target/quarkus-app/quarkus-run.jar"
