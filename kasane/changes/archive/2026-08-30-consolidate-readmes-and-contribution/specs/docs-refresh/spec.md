# docs-refresh

## MODIFIED Requirements

### Requirement: コード正の機械チェック

concepts ハッシュ差分とは独立に、コードを正とする突合をメインコンテキストで行う SHALL: **ツール最低バージョン** (ビルドファイルの取得元 ↔ ルート README 群の対応プラットフォーム表、および該当記載を持つ場合は各 `SKILL.md` の導入節)。差分があれば該当 README / Skill ファイルを要追従リストに追加する SHALL。ただし `--readme-only` 実行時に Skill ファイル側の差分は要追従リストへ載せず報告のみとし、次回の通常実行で処理される旨を完了サマリに添える SHALL。

従前の①モジュール一覧と②Sample デモ画面一覧の突合は行わない SHALL。①の突合先だったルート README のモジュール表・`android/README.md`・`maui/README.md` と、②の突合先だった `samples/*/README.md` がいずれも存在しなくなるため。

#### Scenario: ツール最低バージョンの変更

- **GIVEN** ビルドファイル上のツール最低バージョンが README 群の対応プラットフォーム表と食い違う
- **WHEN** 差分検出を実行する
- **THEN** ルート README 群 (および該当記載を持つ `SKILL.md`) が要追従リストに載る

#### Scenario: Sample デモ画面の追加

- **GIVEN** Sample の実ソースに新しいデモ画面が追加された
- **WHEN** 差分検出を実行する
- **THEN** デモ画面一覧の突合は行われず、要追従リストに何も追加されない

#### Scenario: 旧指示の残存がないこと

- **GIVEN** 本変更を適用した `.agents/skills/docs-refresh/SKILL.md`
- **WHEN** 追従対象の表・差分検出の手順・実行例・README 委譲プロンプト・整合性チェック・完了サマリの全体を読む
- **THEN** platform / Sample README への言及と、ルート README のモジュール表を確認せよという指示が残っていない

#### Scenario: モジュール構成の変更

- **GIVEN** `android/settings.gradle.kts` の `include` が増減した
- **WHEN** 差分検出を実行する
- **THEN** モジュール一覧の突合は行われず、要追従リストに何も追加されない

## ADDED Requirements

### Requirement: 追従対象の README 群

追従対象の README は manifest の `readmes` 配列を正とし、その内容は `skills/README.md`・`skills/README_ja.md`・ルート `README.md`・ルート `README_ja.md` の 4 枚とする SHALL。`maui/spike/README.md` は完了済み検証の記録であり追従対象に含めない SHALL。

#### Scenario: 追従対象の列挙

- **GIVEN** 本変更を適用した `skills/.manifest.json`
- **WHEN** `readmes` 配列を読む
- **THEN** 上記 4 枚だけが列挙されている

#### Scenario: ルート README の言語ペア

- **GIVEN** `--readme-only` を指定した実行
- **WHEN** ルート README が要追従になる
- **THEN** `README.md` と `README_ja.md` が同一の委譲単位として扱われ、両方が同時に更新される
