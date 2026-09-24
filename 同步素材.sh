#!/bin/bash
# ============================================================
#  从现有 JS 版「重工业」同步资源到 Java 工程
#  用法:
#     bash 同步素材.sh                 # 默认从「素材版」（内容最全）同步
#     bash 同步素材.sh <源模组目录>     # 指定源目录
#
#  只复制资源，不动 src/ 与 build 配置；重复执行是安全的（覆盖式）。
# ============================================================
set -u

JAVA_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC="${1:-/storage/emulated/0/文件/像素工厂/Heavy industry/素材/Heavy industry}"

if [ ! -f "$SRC/mod.hjson" ]; then
  echo "❌ 源目录里没有 mod.hjson: $SRC"
  echo "   请传入正确的源模组目录，例如："
  echo "   bash 同步素材.sh \"/storage/emulated/0/文件/像素工厂/Heavy industry/素材/Heavy industry\""
  exit 1
fi

echo "源  : $SRC"
echo "目标: $JAVA_DIR/assets"
echo

# assets/ 下要同步的子目录（对应 jar 根目录）
DIRS="content bundles sprites sprites-override sounds maps scripts"

for d in $DIRS; do
  if [ -d "$SRC/$d" ]; then
    mkdir -p "$JAVA_DIR/assets/$d"
    # 用 cp -r 的「源目录/.」写法，把内容复制进去而不是复制目录本身
    cp -rf "$SRC/$d/." "$JAVA_DIR/assets/$d/" 2>/dev/null
    n=$(find "$JAVA_DIR/assets/$d" -type f ! -name '.gitkeep' | wc -l)
    printf "  %-18s -> %s 个文件\n" "$d" "$n"
  else
    printf "  %-18s -> (源目录没有，跳过)\n" "$d"
  fi
done

# 顶层散件
for f in icon.png; do
  [ -f "$SRC/$f" ] && cp -f "$SRC/$f" "$JAVA_DIR/$f" && echo "  $f -> 已复制"
done

echo
echo "✅ 同步完成。下一步："
echo "   1) git add -A && git commit -m \"同步资源\" && git push    # 触发云编译"
echo "   2) 或在 Termux 里执行: bash /storage/emulated/0/MindustryBuild/build-local.sh \"$JAVA_DIR\""
