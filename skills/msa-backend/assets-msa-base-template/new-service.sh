#!/usr/bin/env bash
#
# service-template 을 복사해 새 Java 마이크로서비스를 생성한다.
#
# 사용법:
#   ./new-service.sh <service-name> <base-package> [port]
#
# 예:
#   ./new-service.sh order-service com.myapp.order 8090
#
# 하는 일:
#   - service-template/ -> <service-name>/ 복사
#   - 패키지 com.example.msa.template -> <base-package> 로 이동/치환
#   - 메인 클래스 ServiceTemplateApplication -> <Pascal>Application
#   - settings.gradle / spring.application.name / 기본 포트 치환
#   - Kafka consumer group / 토픽 등 문자열의 service-template -> <service-name>
#
# 도메인(Sample 엔티티 등)은 그대로 복사되므로, 이후 IDE 에서 실제 도메인으로 바꾼다.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEMPLATE="$SCRIPT_DIR/service-template"

usage() {
  echo "사용법: $0 <service-name> <base-package> [port]" >&2
  echo "예:    $0 order-service com.myapp.order 8090" >&2
  exit 1
}

[ $# -ge 2 ] || usage

NAME="$1"                 # 예: order-service
PACKAGE="$2"              # 예: com.myapp.order
PORT="${3:-8081}"

DEST="$SCRIPT_DIR/$NAME"

if [ ! -d "$TEMPLATE" ]; then
  echo "service-template 을 찾을 수 없습니다: $TEMPLATE" >&2
  exit 1
fi
if [ -e "$DEST" ]; then
  echo "이미 존재합니다: $DEST" >&2
  exit 1
fi

# service-name -> PascalCase (order-service -> OrderService)
PASCAL="$(echo "$NAME" | awk -F'[-_]' '{for(i=1;i<=NF;i++){printf "%s%s", toupper(substr($i,1,1)), substr($i,2)}}')"
PKG_PATH="$(echo "$PACKAGE" | tr '.' '/')"

echo "▶ '$NAME' 생성 (package=$PACKAGE, class=${PASCAL}Application, port=$PORT)"

# 1) 복사 (빌드 산출물 제외)
cp -r "$TEMPLATE" "$DEST"
rm -rf "$DEST/build" "$DEST/.gradle"

# 2) 패키지 디렉터리 이동 (main + test)
for base in "$DEST/src/main/java" "$DEST/src/test/java"; do
  OLD="$base/com/example/msa/template"
  NEW="$base/$PKG_PATH"
  if [ -d "$OLD" ]; then
    mkdir -p "$NEW"
    cp -r "$OLD/." "$NEW/"
    # 이전 패키지 루트 정리
    rm -rf "$base/com/example/msa/template"
    # 빈 상위 디렉터리 정리 (실패해도 무시)
    find "$base/com/example/msa" -type d -empty -delete 2>/dev/null || true
    find "$base/com/example" -type d -empty -delete 2>/dev/null || true
    find "$base/com" -type d -empty -delete 2>/dev/null || true
  fi
done

# 3) 텍스트 치환 (긴 문자열부터)
#    - 패키지 경로
#    - 메인 클래스 이름
#    - 서비스 이름 (rootProject / app name / group-id / 토픽 접두 등)
grep -rIl --exclude-dir=gradle . "$DEST" 2>/dev/null | while read -r f; do
  sed -i \
    -e "s/com\.example\.msa\.template/$PACKAGE/g" \
    -e "s/ServiceTemplateApplication/${PASCAL}Application/g" \
    -e "s/service-template/$NAME/g" \
    "$f"
done

# 4) 메인/테스트 클래스 파일명 변경 (public 클래스명 = 파일명 이어야 함)
find "$DEST" -name 'ServiceTemplateApplication.java' -exec bash -c \
  'mv "$1" "$(dirname "$1")/'"${PASCAL}"'Application.java"' _ {} \;
find "$DEST" -name 'ServiceTemplateApplicationTests.java' -exec bash -c \
  'mv "$1" "$(dirname "$1")/'"${PASCAL}"'ApplicationTests.java"' _ {} \;

# 5) 기본 포트 치환 (application.yml 의 SERVER_PORT 기본값)
sed -i "s/\${SERVER_PORT:8081}/\${SERVER_PORT:$PORT}/" "$DEST/src/main/resources/application.yml"

echo "✔ 완료: $NAME/"
echo "  - 빌드/테스트: (cd $NAME && ./gradlew test)"
echo "  - 다음 단계: entity/Sample.java 등 도메인을 실제 도메인으로 교체하고,"
echo "    docker-compose.yml 에 서비스 블록을 추가하세요."
