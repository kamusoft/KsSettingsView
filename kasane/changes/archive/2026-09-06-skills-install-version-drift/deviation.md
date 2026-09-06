# deviation: skills-install-version-drift

- [付随修正] `kasane/handbook/cross/release-procedure.md` の「リリース PR」手順 2 と直後の注意書き: script の対象を Skill 8 枚へ広げた結果「README 2 枚」「README の不一致」の記述が実態より狭くなったため、同じ手順内の 2 箇所の文言を「README 2 枚と利用者向け Skill 8 枚 (4 本 × 2 言語)」「インストール例の不一致」に改めた。実装ワーカーのスコープ外報告を受け、本 change が直接原因で同ファイル内 2 行で閉じるためサイクル内で修正 (2026-09-06)
- [付随修正] `.github/workflows/release.yml` の validate step 名 (`Verify README install examples` → `Verify install examples (README + Skills)`) と `.github/workflows/ci.yml` の selftest step 名・コメントを README 限定でない表現に改めた。script の対象拡張で呼び出し元の表現だけが取り残されたため (review-001 Minor 1)。job 名は不変で branch protection の必須 check には影響しない (2026-09-06)
