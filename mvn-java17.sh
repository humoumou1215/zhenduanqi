#!/bin/bash
# 使用Java 17运行Maven命令
# 用法: ./mvn-java17.sh test
#       ./mvn-java17.sh clean compile
#       ./mvn-java17.sh package

export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH=$JAVA_HOME/bin:$PATH

echo "🔧 使用 Java $(java -version 2>&1 | head -1)"
echo "📦 Maven 命令: mvn $@"
echo ""

mvn "$@"
