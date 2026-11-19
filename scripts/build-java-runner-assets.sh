#!/bin/sh
# 게시글 Java 코드 실행(CheerpJ)용 자산 생성 → <출력폴더>/
#   javac17.jar        JDK 17 javac (jdk.compiler + java.compiler 클래스, 약 3.5MB)
#   jdk17/lib/modules  java.base 만 담은 jlink 이미지 (javac --system 용, 약 12MB)
#   jdk17/lib/jrt-fs.jar
#
# 사용
#   Docker 빌드:  Dockerfile.web 이 eclipse-temurin:17-jdk 안에서 호출
#   로컬 개발:    docker run --rm -v "$PWD/scripts:/s:ro" -v "$PWD/frontend/public/cheerpj:/out" \
#                   eclipse-temurin:17-jdk sh /s/build-java-runner-assets.sh /out
set -eu

OUT="${1:?usage: build-java-runner-assets.sh <output-dir>}"
J="${JAVA_HOME:-/opt/java/openjdk}"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

for m in jdk.compiler java.compiler; do
  "$J/bin/jmod" extract --dir "$TMP/$m" "$J/jmods/$m.jmod"
  # 모듈이 아니라 classpath 로 올리므로 module-info 제거
  rm -f "$TMP/$m/classes/module-info.class"
done

mkdir -p "$OUT/jdk17/lib"
"$J/bin/jar" --create --file "$OUT/javac17.jar" \
  -C "$TMP/jdk.compiler/classes" . \
  -C "$TMP/java.compiler/classes" .

"$J/bin/jlink" --add-modules java.base --output "$TMP/img" \
  --strip-debug --no-header-files --no-man-pages --compress=2
cp "$TMP/img/lib/modules" "$TMP/img/lib/jrt-fs.jar" "$OUT/jdk17/lib/"

ls -la "$OUT" "$OUT/jdk17/lib"
