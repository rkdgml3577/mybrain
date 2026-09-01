#!/usr/bin/env bash
#
# mybrain/skills/ 를 정본으로 두고, 스킬을 원하는 곳에 배포(복사)한다.
#
# 사용법:
#   ./install.sh                      # 사용 가능한 스킬 목록
#   ./install.sh <skill>              # 글로벌(~/.claude/skills)에 설치
#   ./install.sh <skill> --global     # 위와 동일
#   ./install.sh <skill> --project <프로젝트경로>   # 그 프로젝트의 .claude/skills 에 설치
#   ./install.sh --all                # 모든 스킬을 글로벌에 설치
#
# 항상 mybrain 쪽이 원본이다. 스킬을 고칠 때는 여기(skills/<name>/)를 고치고
# 이 스크립트로 다시 배포한다. 대상 폴더는 덮어쓴다.
set -euo pipefail

SRC_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

list_skills() {
  find "$SRC_DIR" -mindepth 1 -maxdepth 1 -type d -exec test -f '{}/SKILL.md' ';' -print \
    | xargs -n1 basename 2>/dev/null | sort
}

usage() {
  echo "사용법:"
  echo "  ./install.sh                          스킬 목록"
  echo "  ./install.sh <skill> [--global]       글로벌(~/.claude/skills)에 설치"
  echo "  ./install.sh <skill> --project <dir>  프로젝트(.claude/skills)에 설치"
  echo "  ./install.sh --all                    모든 스킬을 글로벌에 설치"
  echo ""
  echo "사용 가능한 스킬:"
  list_skills | sed 's/^/  - /'
}

install_one() {
  local skill="$1" dest_root="$2"
  local src="$SRC_DIR/$skill"
  if [ ! -f "$src/SKILL.md" ]; then
    echo "✗ '$skill' 은(는) 스킬이 아닙니다 (SKILL.md 없음)" >&2
    return 1
  fi
  mkdir -p "$dest_root"
  rm -rf "${dest_root:?}/$skill"
  cp -r "$src" "$dest_root/$skill"
  # 실행 스크립트 권한 보존
  find "$dest_root/$skill" -name '*.sh' -exec chmod +x {} + 2>/dev/null || true
  echo "✔ $skill → $dest_root/$skill"
}

[ $# -ge 1 ] || { usage; exit 0; }

# 대상 위치 결정
DEST_ROOT="$HOME/.claude/skills"
SKILL=""
ALL=false

while [ $# -gt 0 ]; do
  case "$1" in
    --all) ALL=true; shift ;;
    --global) DEST_ROOT="$HOME/.claude/skills"; shift ;;
    --project)
      [ $# -ge 2 ] || { echo "--project 뒤에 프로젝트 경로가 필요합니다" >&2; exit 1; }
      DEST_ROOT="$2/.claude/skills"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    -*) echo "알 수 없는 옵션: $1" >&2; exit 1 ;;
    *) SKILL="$1"; shift ;;
  esac
done

if [ "$ALL" = true ]; then
  while read -r s; do install_one "$s" "$DEST_ROOT"; done < <(list_skills)
  exit 0
fi

[ -n "$SKILL" ] || { usage; exit 0; }
install_one "$SKILL" "$DEST_ROOT"
