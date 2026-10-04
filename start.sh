#!/usr/bin/env bash
# ============================================================
# 在线课程证书签发与姓名更正系统 —— 一键启动
# 用法: ./start.sh        （首次运行会下载依赖，耗时较长）
# 启动后访问 http://localhost:8080
# ============================================================
set -euo pipefail
cd "$(dirname "$0")"

# ---- 1. 定位 JDK 与 Maven（优先系统环境，缺失时使用本地工具链） ----
if [ -z "${JAVA_HOME:-}" ] && [ -x "$HOME/tools/jdk/bin/java" ]; then
  export JAVA_HOME="$HOME/tools/jdk"
fi
if [ -n "${JAVA_HOME:-}" ]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi
if ! command -v mvn >/dev/null 2>&1 && [ -x "$HOME/tools/apache-maven-3.9.9/bin/mvn" ]; then
  export PATH="$HOME/tools/apache-maven-3.9.9/bin:$PATH"
fi
command -v java >/dev/null 2>&1 || { echo "未找到 Java，请先安装 JDK 17+"; exit 1; }
command -v mvn  >/dev/null 2>&1 || { echo "未找到 Maven"; exit 1; }
command -v node >/dev/null 2>&1 || { echo "未找到 Node.js"; exit 1; }

# ---- 2. 构建前端并放入后端静态资源目录（前后端同源部署） ----
echo "==> [1/3] 构建前端 (Svelte + Vite)"
(cd frontend && npm install --no-audit --no-fund --loglevel=error && npm run build)
rm -rf backend/src/main/resources/META-INF/resources
mkdir -p backend/src/main/resources/META-INF/resources
cp -r frontend/dist/* backend/src/main/resources/META-INF/resources/

# ---- 3. 打包后端并启动 ----
echo "==> [2/3] 打包后端 (Quarkus + Hibernate + SQLite)"
(cd backend && mvn -B -q -DskipTests package)

echo "==> [3/3] 启动服务"
echo "------------------------------------------------------------"
echo "  系统入口:   http://localhost:8080"
echo "  证书核验:   http://localhost:8080/#/verify"
echo "  数据库文件: backend/cert-platform.db (SQLite)"
echo "  停止: Ctrl+C"
echo "------------------------------------------------------------"
cd backend && exec java -jar target/quarkus-app/quarkus-run.jar
