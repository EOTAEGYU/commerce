아래 순서를 자동으로 실행하라. 사용자에게 추가 입력을 요청하지 말고 전체 흐름을 완료하라.

1. `./gradlew test` 를 실행하라.

2. 테스트가 하나라도 실패하면:
   - 실패한 테스트 목록을 출력하라.
   - 이후 단계를 실행하지 말고 즉시 중단하라.

3. 테스트가 전체 통과하면:
   - `git diff HEAD` 와 `git status` 로 변경 내용을 분석하라.
   - Conventional Commits 규칙 (feat / fix / refactor / test / chore) 에 맞는 커밋 메시지를 자동 생성하라.
   - `git add -A` → `git commit` → `git push -u origin HEAD` 를 순서대로 실행하라.

4. push 완료 후 현재 브랜치가 `feature/` 로 시작하면 PR을 생성하고 merge한다:
   ```bash
   gh pr create --base dev \
     --title "{커밋 메시지 제목}" \
     --body "$(cat <<'EOF'
   ## Summary
   {변경 내용 2-3줄 요약}

   ## Test plan
   - [x] ./gradlew test 전체 통과

   🤖 Generated with [Claude Code](https://claude.com/claude-code)
   EOF
   )"
   gh pr merge --merge --delete-branch
   ```

5. 완료 후 아래 항목을 요약하여 출력하라:
   - 통과한 테스트 수
   - 자동 생성된 커밋 메시지
   - 커밋 해시 (short)
   - PR URL (생성된 경우)
   - merge 결과
