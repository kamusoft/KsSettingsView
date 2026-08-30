# Delta Spec: docs-refresh (skills/ 追従への改修)

対象能力: docs-refresh — 利用者向けドキュメントを kasane/concepts/ とコード・テストへ追従させるメンテナンススキル。本デルタは追従対象を `docs/` から `skills/` + README 群へ移した改修後の契約を定義する (旧 `docs/` 向けの挙動はすべて置き換え)。

## ADDED Requirements

### Requirement: manifest v3 に基づく差分検出

スキルは `skills/.manifest.json` (version 3) を差分検出の正とし、concepts ハッシュの比較で変更・新規・削除を分類し、`targets` (Skill ファイル → 源泉 concepts、言語抜きパス) の逆引きで更新対象を特定する SHALL。特定された更新対象は常に en/ja の言語ペアとして扱われる SHALL。README 群は concepts ハッシュ逆引きの対象外である (manifest の `readmes` は更新対象の列挙のみ) — README の concept 由来記述は、コード正の機械チェックと `--all` / `--readme-only` 実行時の見直しで維持する。

#### Scenario: concept 変更の逆引き

- **GIVEN** version 3 の manifest と、ある concept をハッシュ変化ありと判定した差分検出
- **WHEN** 更新対象を特定する
- **THEN** その concept を源泉として `targets` に列挙された Skill ファイルが、en/ja 両パスのペアとして要追従リストに載る

#### Scenario: メインコンテキストは concepts 本文を読まない

- **GIVEN** 差分検出から更新方針の提示まで
- **WHEN** メインオーケストレーターが判断する
- **THEN** 判断はハッシュ計算・manifest 参照・コード正の機械チェックのみで行われ、concepts 本文の読み込みはサブエージェントに委譲される

### Requirement: manifest v3 スキーマの規範

`skills/.manifest.json` は次の構造に従う SHALL。phase の初期生成と docs-refresh の書き出しの両方がこの規範に従う (別実装間の相互運用の正):

```json
{
  "version": 3,
  "generatedAt": "<ISO 8601 タイムスタンプ>",
  "concepts": { "<concepts ルート相対パス>": "<sha256>" },
  "targets": { "<言語抜きの Skill 相対パス (例: kssettingsview-ios/references/cells.md)>": ["<concepts ルート相対パス>", "..."] },
  "excluded": { "<concepts ルート相対パス>": "<除外理由 (文字列)>" },
  "readmes": ["<リポジトリ相対パス>", "..."],
  "lastUpdatedFiles": ["<このリフレッシュで更新したファイルのリポジトリ相対パス>"]
}
```

不変条件: ①`targets` のキーは `skills/en/` と `skills/ja/` の双方に同一相対パスで実在するファイルを指す、②`targets` の値と `excluded` のキーは concepts に実在するパスを指す、③`version` / `concepts` / `targets` / `excluded` / `readmes` は必須キー。

#### Scenario: 削除された concept の整理

- **GIVEN** `targets` または `excluded` に列挙された concept ファイルが削除されている
- **WHEN** 差分検出と更新フローを実行する
- **THEN** 削除済みとして分類され、影響する Skill ファイルの扱い (該当記述の除去) がユーザーに提示され、承認後の manifest 書き出しで該当パスが `targets` / `excluded` から取り除かれる

### Requirement: manifest 不在・非対応時の停止

manifest が存在しない・JSON として壊れている・version が 3 でない・スキーマ規範の必須キー欠落や型不正がある場合、スキルは skills/ の再生成にフォールバックしない SHALL NOT。停止し、初期生成 (変更フローによる skills/ 一式 + manifest 初期版の作成) または manifest の修復が必要である旨を案内する SHALL。

#### Scenario: manifest 不在での起動

- **GIVEN** `skills/.manifest.json` が存在しない (または parse エラー / version ≠ 3 / 必須キー欠落・型不正)
- **WHEN** docs-refresh を起動する
- **THEN** skills/ と README 群は一切書き換えられず、初期生成または修復が必要である旨の案内のみが表示されて終了する

### Requirement: concepts 網羅検査

差分更新フローは「`targets` のどの Skill ファイルにも現れず、`excluded` にも列挙されていない concept」を機械検出し、存在すれば検査失敗として報告する SHALL。該当 concept の配置判断 (どの Skill に載せるか / 理由つきで除外するか) はユーザーに提示し、スキルが独断で決めない SHALL NOT。

#### Scenario: 未参照・未除外 concept の検出

- **GIVEN** targets にも excluded にも現れない concept ファイル (新規追加を含む)
- **WHEN** 差分更新フローを実行する
- **THEN** 網羅検査が失敗として報告され、配置判断の選択肢がユーザーに提示される (manifest への反映は判断確定後)

### Requirement: 更新方針の承認ゲート

スキルは要追従リストを要約してユーザーに提示し、承認を得るまで skills/ と README 群を書き換えない SHALL NOT。承認なく中止した場合、manifest は更新されない SHALL NOT。

#### Scenario: 承認前の無変更

- **GIVEN** 差分検出で得られた要追従リストの提示
- **WHEN** ユーザーが承認せずに中止する
- **THEN** skills/・README 群・manifest のいずれも変更されていない

### Requirement: Skill 単位の委譲と en/ja ペア生成

ドキュメント更新は Skill 単位でサブエージェントへ委譲する SHALL — 1 つの Skill の更新対象ファイル群 (両言語) を 1 サブエージェントが処理し、en/ja ペアを同一文脈で同時生成する。README 群は対象 README ごと (en/ja ペアがあればペア) に委譲する SHALL。並列実行は最大 3 並列のバッチとする SHALL。

#### Scenario: 同一 Skill 内の複数ファイル更新

- **GIVEN** ある Skill の references 1 本と SKILL.md (能力マップ) の両方が要追従
- **WHEN** サブエージェントへ委譲する
- **THEN** その Skill の対象ファイル群 (en/ja 両版) が 1 つのサブエージェントにまとめて渡され、能力マップとレシピの整合が同一文脈で保たれる

#### Scenario: 言語ペアの同時更新

- **GIVEN** 更新対象の Skill ファイル
- **WHEN** サブエージェントが生成する
- **THEN** en 版と ja 版が同一構成 (見出し階層・コードブロックの数と順序) で同時に更新され、片言語のみの更新は発生しない

### Requirement: 生成プロンプトの内容規約

委譲プロンプトは生成物の内容規約を制約として明記する SHALL: ①能力マップ + レシピ形式 + 段階開示 (cross/ADR-0022 の内容設計)、②frontmatter は Agent Skills 標準 6 フィールド内・en/ja 同名、③コード例は原則コメントを書かない (説明はレシピの見出しとリード文が担う)。やむを得ない最小限のコメントは英語で統一する、④ローカル絶対パスを書かない、⑤旧 docs/ ファイルおよび openspec への参照を新設しない、⑥API 署名とコード例は concepts の記載に加えて実装コード・テストで最終確認する (利用者がコピーして動くこと)。concepts と実装が矛盾する場合は drift 所見としてユーザーへ報告し、独断でどちらの正本も書き換えない。

#### Scenario: プロンプトの制約明記

- **GIVEN** Skill 更新の委譲プロンプト
- **WHEN** その内容を検査する
- **THEN** 上記 ①〜⑥ の制約がすべて明記されている

### Requirement: コード正の機械チェック

concepts ハッシュ差分とは独立に、コードを正とする 3 種の突合をメインコンテキストで行う SHALL: ①モジュール一覧 (ビルド構成 ↔ ルート README 群のモジュール表)、②Sample デモ画面一覧 (Sample 実ソース ↔ `samples/*/README.md`)、③ツール最低バージョン (ビルドファイルの取得元 ↔ ルート README 群の対応表、および記載がある場合は各 SKILL.md 導入節)。差分があれば該当 README / Skill ファイルを要追従リストに追加する SHALL。

#### Scenario: Sample デモ画面の追加

- **GIVEN** Sample の実ソースに新しいデモ画面が追加され、`samples/<platform>/README.md` の一覧に載っていない
- **WHEN** 差分検出を実行する
- **THEN** 該当 README が要追従リストに載る

#### Scenario: ツール最低バージョンの変更

- **GIVEN** ビルドファイル上のツール最低バージョンが README 群の対応表と食い違う
- **WHEN** 差分検出を実行する
- **THEN** ルート README 群 (および該当記載を持つ SKILL.md) が要追従リストに載る

### Requirement: 整合性チェック一式

全更新の完了後、次の 8 検査を実行し、失敗した生成物を再修正対象に追加する SHALL: ①concepts 網羅検査、②en/ja 節構成一致 (見出し階層の並び)、③コードブロック一致 (数・順序・内容の byte 一致)、④frontmatter 検査 (標準 6 フィールド内・en/ja 同名・`metadata.language` とパスの一致)、⑤旧名残 grep (廃止 API・`docs/` 参照新設・openspec 参照)、⑥内部リンク解決、⑦identity-lint (ローカル絶対パスを含む、個体・個人・秘密を特定する値の検査。lint の検査範囲に skills/ を含める)、⑧配信識別子の表記ゆれ grep。

#### Scenario: en/ja 構成乖離の検出

- **GIVEN** en/ja ペアで見出し階層、またはコードブロックの数・順序・内容が一致しない生成物
- **WHEN** 整合性チェックを実行する
- **THEN** 該当ペアが失敗として報告され、再修正対象に追加される

#### Scenario: 混入の検出

- **GIVEN** 生成物にローカル絶対パス・廃止 API・`docs/` への新設参照のいずれかが含まれる
- **WHEN** 整合性チェックを実行する
- **THEN** 該当ファイルが失敗として報告され、再修正対象に追加される

#### Scenario: frontmatter 違反の検出

- **GIVEN** 標準 6 フィールド外のフィールドを持つ、または en/ja で `name` が異なる SKILL.md
- **WHEN** 整合性チェックを実行する
- **THEN** 該当 Skill が失敗として報告され、再修正対象に追加される

### Requirement: 実行フラグ

`--all` は差分検出をスキップし、manifest の `targets` に列挙された全 Skill ファイルと `readmes` を要追従とする全再生成として動作する SHALL (manifest 前提は維持し、不在なら停止する)。`--readme-only` は manifest の `readmes` に列挙された README 群のみを追従対象とし、skills/ 本体をスキップする SHALL。`--readme-only` の実行では manifest の `concepts` / `targets` / `excluded` を更新しない SHALL NOT (Skill 本体に未反映の concept 差分を消費しないため)。`--all` と `--readme-only` の同時指定はエラーとして停止する SHALL。

#### Scenario: --all の全再生成

- **GIVEN** version 3 の manifest
- **WHEN** `--all` で起動する
- **THEN** targets の全 Skill ファイル (en/ja ペア) と readmes が要追従リストに載り、承認ゲート以降は通常フローと同一に進む

#### Scenario: --readme-only の軽量チェック

- **GIVEN** version 3 の manifest
- **WHEN** `--readme-only` で起動する
- **THEN** 要追従の候補は readmes 列挙分に限定され、skills/ 本体は検出・更新の対象にならない

#### Scenario: --readme-only は concept 差分を消費しない

- **GIVEN** Skill 本体に未反映の concept 変更がある状態
- **WHEN** `--readme-only` を実行して完了する
- **THEN** manifest の `concepts` / `targets` / `excluded` は更新されず、次回の通常実行で同じ concept 変更が再検出される

### Requirement: manifest の更新

manifest は全更新と整合性チェックの通過後、最後にスキーマ規範に従って全体を書き直す SHALL。このとき concepts ハッシュを現在値へ更新するのは「この実行で対象の全 Skill ファイルが更新され、検査を通過した concept」に限る SHALL — 未処理 (未承認・部分承認で対象外・検査失敗) の concept は旧ハッシュを保持し、次回実行で再検出できる SHALL。途中中断時には更新されず、次回実行が同じ差分を再検出できる SHALL。

#### Scenario: 中断時の再検出可能性

- **GIVEN** 更新フローが整合性チェック前に中断した状態
- **WHEN** 次回 docs-refresh を実行する
- **THEN** manifest が旧状態のまま残っているため、同じ concepts 差分が再検出される

#### Scenario: 部分承認時の再検出可能性

- **GIVEN** 要追従リストの一部の項目だけをユーザーが承認して完了した実行
- **WHEN** 次回 docs-refresh を実行する
- **THEN** 未承認項目の源泉 concept は旧ハッシュのまま残っているため、同じ差分が再検出される

### Requirement: 起動規律の維持

スキルは自発的に自動発動しない SHALL NOT。起動はユーザーの明示依頼のみとし、その旨を description と Guardrails の両方に保持する SHALL。

#### Scenario: concepts 更新後の非発動

- **GIVEN** concepts が更新された直後のセッション
- **WHEN** ユーザーが docs-refresh を明示的に依頼していない
- **THEN** スキルは発動せず、skills/ と README 群は変更されない
