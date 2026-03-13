---
name: commit
description: Conventional Commits 규칙에 따라 커밋 생성
disable-model-invocation: true
allowed-tools: Bash, Read, Grep
---

# Conventional Commits 커밋 스킬

변경 사항을 분석하고 Conventional Commits 규칙에 맞는 커밋을 생성한다.

## 절차

1. `git status`로 변경된 파일 확인 (untracked 포함, -uall 플래그 사용 금지)
2. `git diff`와 `git diff --cached`로 변경 내용 파악
3. `git log --oneline -5`로 최근 커밋 스타일 참고
4. 변경 사항을 분석하여 아래 규칙에 맞는 커밋 메시지 작성
5. 관련 파일만 `git add`로 스테이징 (`git add -A` 사용 금지)
6. 커밋 메시지를 사용자에게 보여주고 확인 후 커밋

## 커밋 메시지 형식

```
<type>: <subject>
```

### type (필수)

| type | 용도 |
|------|------|
| feat | 새로운 기능 추가 |
| fix | 버그 수정 |
| refactor | 리팩토링 (기능 변경 없음) |
| doc | 문서 추가/수정 |
| test | 테스트 추가/수정 |
| chore | 빌드, 설정 등 기타 변경 |

### subject (필수)

- 한글 또는 영어로 작성
- 50자 이내
- 마침표 없이 끝냄
- 변경의 "무엇을" 했는지 명확히 기술

### 예시

```
feat: relay-worker Consumer 재처리 정책 및 외부 API 신뢰성 구현
refactor: 코드 리뷰 피드백 반영 - 아키텍처 개선 및 설정 외부화
fix: Circuit Breaker 저트래픽 환경 복귀 지연 해결
doc: readme 추가
test: RetryClassifier 단위 테스트 추가
chore: Gradle 의존성 업데이트
```

## 주의사항

- `.env`, `credentials`, 시크릿 파일은 절대 커밋하지 않는다
- 커밋 전 반드시 사용자 확인을 받는다
- `--no-verify`, `--amend` 플래그는 사용자가 명시적으로 요청한 경우에만 사용한다
- 커밋 메시지는 반드시 HEREDOC 방식으로 전달한다:

```bash
git commit -m "$(cat <<'EOF'
type: subject

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```
