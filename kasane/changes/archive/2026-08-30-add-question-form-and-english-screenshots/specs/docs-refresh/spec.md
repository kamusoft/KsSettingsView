# docs-refresh — デルタスペック

## MODIFIED Requirements

### Requirement: 追従対象の README 群

追従対象の README は manifest の `readmes` 配列を正とし、その内容は `skills/README.md`・`skills/README_ja.md`・ルート `README.md`・ルート `README_ja.md` の 4 枚とする SHALL。公開リポジトリに含めない資産 (`maui/spike/`) への言及を `SKILL.md` に残さない SHALL。

#### Scenario: 追従対象の列挙

- **GIVEN** `skills/.manifest.json`
- **WHEN** `readmes` 配列を読む
- **THEN** 上記 4 枚だけが列挙されている

#### Scenario: ルート README の言語ペア

- **GIVEN** `--readme-only` を指定した実行
- **WHEN** ルート README が要追従になる
- **THEN** `README.md` と `README_ja.md` が同一の委譲単位として扱われ、両方が同時に更新される

#### Scenario: 公開されない資産への言及の不在

- **GIVEN** docs-refresh の `SKILL.md`
- **WHEN** `maui/spike` を検索する
- **THEN** 一致が 0 件である
