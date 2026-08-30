# Delta Spec: user-skills (skills/ 一式の初回生成と docs/ 廃止)

対象能力: user-skills — 利用者向けドキュメントの提供形態。本デルタは `skills/` 一式 (8 部 + 索引 + manifest) の初回生成と、旧 `docs/` の廃止後の状態を契約として定義する。manifest v3 のスキーマ規範は phase-11 のデルタスペック ([specs/docs-refresh/spec.md](../../../archive/2026-08-26-retarget-docs-refresh-to-skills/specs/docs-refresh/spec.md) の「manifest v3 スキーマの規範」) が正であり、本スペックはそれに準拠する側の契約を書く。

## ADDED Requirements

### Requirement: Skill 一式の構成

`skills/` は `skills/{en,ja}/<name>/` の 2 言語トップ分離で、各言語配下に自己完結した Skill ディレクトリ 4 本を持つ SHALL。name は `kssettingsview-{ios,android,maui,aiforms-migration}` で en/ja 同名 SHALL。各 Skill は `SKILL.md` を持ち、platform Skill (ios / android / maui) は `references/` に cells / updates / styling / custom-cells の 4 本、移行 Skill (aiforms-migration) は api-mapping の 1 本を持つ SHALL。これ以外のファイルを Skill ディレクトリに置かない SHALL NOT。

#### Scenario: ディレクトリ構成の検査

- **GIVEN** 生成完了後の `skills/`
- **WHEN** ディレクトリ構成を検査する
- **THEN** en/ja 各配下に上記 4 Skill が同一相対パス構成で存在し、規定外のファイルが存在しない

### Requirement: frontmatter の標準準拠

各 `SKILL.md` の frontmatter は `name` / `description` / `license` / `metadata` (language / source) の 4 フィールドすべてを持ち、これ以外のフィールドを使わない SHALL NOT (実行系固有の拡張フィールド禁止)。`metadata.language` は配置パスの言語 (en / ja) と一致し、`metadata.source` は生成元 (本リポジトリ) を指す値を持つ SHALL。ja 版 `description` は日本語本文に英語キーワードを併記する SHALL (キーワード選定の適切さは機械検査ではなく独立レビューの確認項目)。

#### Scenario: frontmatter の検査

- **GIVEN** 生成された 8 部の SKILL.md
- **WHEN** frontmatter を検査する
- **THEN** 全部が規定 4 フィールドのみで構成され、en/ja で `name` が同一、`metadata.language` がパスと一致し、`metadata.source` が存在し、ja 版 description に英語表記のキーワードが含まれる

### Requirement: Skill 内容の設計原則

各 `SKILL.md` は発火情報 (description)・できること一覧 (能力マップ表)・導入 (コピー手順への参照と最低バージョン)・最小動作コード・`references/` への振り分けで構成する SHALL。`references/` の各ファイルは「やりたいこと」の自然言語見出し + 1〜2 行のリード文 + 完動コードのレシピ形式で構成する SHALL。アーキテクチャ解説など利用に直結しない読み物を含めない SHALL NOT。旧 `docs/` の構成・章立てを引き継がない SHALL NOT (コード例の素材としてのみ使う)。AiForms 移行 Skill の `references/api-mapping.md` は、`cross/conventions/aiforms-spec-summary.md` に記載された旧公開 API を網羅する対応表 (対応先 API、または非互換の別と代替手段) を持つ SHALL。

#### Scenario: 能力マップとレシピ形式の検査

- **GIVEN** 生成された Skill 一式
- **WHEN** 内容構成をレビューする
- **THEN** 各 SKILL.md に能力マップ表と最小動作コードがあり、references の各節が「やりたいこと見出し + リード文 + コード」の形になっている

#### Scenario: 移行対応表の網羅

- **GIVEN** 生成された api-mapping.md と移送済み aiforms-spec-summary.md
- **WHEN** 旧公開 API の網羅を突き合わせる
- **THEN** spec-summary に記載された旧公開 API がすべて対応表に現れる (対応先、または非互換 + 代替手段の別つき)

### Requirement: 生成の内容規約

生成 (fan-out の委譲プロンプト) は次の制約を明記し、生成物はこれに従う SHALL: ①コード例は原則コメントを書かず、説明はレシピの見出しとリード文が担う。やむを得ない最小限のコメントは英語で統一する。②ローカル絶対パスを書かない。③旧 `docs/` および `openspec/` への参照を新設しない。④API 署名とコード例は concepts の記載に加えて実装コード・テストで最終確認する (利用者がコピーして動くこと)。concepts と実装の矛盾を見つけたら drift 所見として報告し、独断でどちらも書き換えない SHALL NOT。⑤各ワーカーは生成した各ファイルごと (言語抜きの Skill 相対パス単位) の源泉 concepts のマップ (manifest `targets` の材料) を成果物と併せて報告する SHALL。

#### Scenario: コード例のコメント規約

- **GIVEN** 生成された全コードブロック
- **WHEN** コメントを検査する
- **THEN** コメントは原則存在せず、存在する場合は英語である

### Requirement: 翻訳ロックステップ

en/ja の対応する Skill ファイルは同一構成 (見出し階層の並び・コードブロックの数と順序) である SHALL。対応するコードブロックの内容は byte 一致する SHALL。片言語のみの生成・更新は発生しない SHALL NOT。

#### Scenario: ロックステップの機械検査

- **GIVEN** 生成された en/ja の全ファイルペア
- **WHEN** 構成一致とコードブロック一致を機械検査する
- **THEN** 全ペアで見出し階層の並びが一致し、コードブロックが数・順序・内容 (byte) で一致する

### Requirement: manifest 初期版

`skills/.manifest.json` を phase-11 スペックの「manifest v3 スキーマの規範」に従って書き出す SHALL。manifest が扱う concept の集合は `kasane/concepts/` 配下の `*.md` から `index.md` / `log.md` / `rules.md` を除いたものとする SHALL (docs-refresh の追従対象と同一)。`concepts` のキー集合はこの集合と過不足なく一致し、各値は本 change の全タスク完了時点のファイル内容の SHA-256 と一致する SHALL。`targets` はワーカー報告のファイル別マップから構成し、生成された全 Skill ファイルが言語抜き相対パスでちょうど 1 キーとして存在する SHALL。AiForms 移行 Skill の源泉に `cross/conventions/aiforms-spec-summary.md` を含む SHALL。concept 集合の全要素は `targets` のいずれかの値に現れるか、`excluded` に空でない理由文字列つきで列挙されるかのどちらかである SHALL (網羅不変条件 — 満たされない場合、初回の docs-refresh 実行が網羅検査で停止する)。`readmes` には索引 2 枚 (`skills/README.md` / `skills/README_ja.md`) とルート `README.md` を含む、利用者向け README 群 (platform / samples の README を含む) を列挙する SHALL。

#### Scenario: 網羅不変条件の検査

- **GIVEN** 書き出された manifest と `kasane/concepts/` の全 concept ファイル
- **WHEN** 網羅検査 (targets のどの値にも現れず excluded にもない concept の検出) を実行する
- **THEN** 該当する concept が 0 件である

#### Scenario: スキーマ準拠の検査

- **GIVEN** 書き出された manifest
- **WHEN** phase-11 スペックのスキーマ規範 (必須キー・不変条件①②③) で検証する
- **THEN** すべて満たされる

#### Scenario: ハッシュの最終状態一致

- **GIVEN** 全タスク完了後の作業ツリーと書き出された manifest
- **WHEN** concept 集合 (index / log / rules 除外) の SHA-256 を再計算して `concepts` と突き合わせる
- **THEN** キー集合・ハッシュ値ともに過不足なく一致する

### Requirement: 索引 README

`skills/README.md` (英語) と `skills/README_ja.md` (日本語) は、① Skill 一覧表 (name / 対象 / 1 行説明 / en・ja リンク)、②コピー手順 (`.agents/skills/` を共通コピー先の第一候補、Claude Code は `.claude/skills/`)、③利用者は片言語のみコピーする前提の明記、の 3 要素のみで構成する SHALL。

#### Scenario: 索引の構成検査

- **GIVEN** 生成された索引 2 枚
- **WHEN** 内容を検査する
- **THEN** 3 要素がすべて存在し、それ以外の節が存在しない

### Requirement: ルート README への導線

ルート `README.md` に、skills/ への導線 1 文 (索引 `skills/README.md` へのリンクを含む) とモノレポ構成表の `skills/` 行を追記する SHALL。README 群 (ルート / `android/README.md` / `samples/ios/README.md` / `samples/android/README.md`) の既存の docs/ 参照は、対応する Skill・skills 索引への差し替え、または参照先の消滅に伴う記述の除去で解消する SHALL。本 change の README 群への変更は導線追記と docs/ 参照の解消に限る SHALL (本文の大幅改訂・英語化は phase-9 の責務)。

#### Scenario: 導線の追記と変更範囲

- **GIVEN** 変更後の README 群
- **WHEN** 変更前との diff を確認する
- **THEN** 差分は導線 1 文・構成表 1 行の追記と、docs/ 参照の差し替え・除去に限られる

### Requirement: 移植元仕様要約の concepts 移送

`docs/legacy-aiforms-reference.md` は `kasane/concepts/cross/conventions/aiforms-spec-summary.md` へ移送される SHALL。frontmatter は `type: reference` とし、凍結された歴史資料であり最終的な正は移植元コードである旨を注記する SHALL。本文中のローカル絶対パスと `file://` リンクはすべて除去し、リモート URL または `../<リポジトリ名>/` 形式の相対参照へ変換する SHALL (ksn-core paths.md 規約。移送先は local-path lint の検査対象)。[aiforms-origin-reference.md](../../../../concepts/cross/conventions/aiforms-origin-reference.md) と相互リンクし (同ファイルの docs/ 直リンクは新パスへ差し替え)、cross の index.md と concepts の log.md に登載する SHALL。

#### Scenario: 移送後の参照整合

- **GIVEN** 移送完了後のリポジトリ
- **WHEN** aiforms-spec-summary.md とその参照元を検査する
- **THEN** 新パスにファイルが存在し、aiforms-origin-reference との相互リンクが解決し、cross index に登載され、docs/ 側にファイルが残っておらず、本文にローカル絶対パス・`file://` リンクが 0 件である

### Requirement: docs/ の廃止と残記述整理

`docs/` はディレクトリごと (`.manifest.json` 含む) 廃止される SHALL。`kasane/config.yaml` は `lint.exclude` の docs/ 除外 (説明コメント含む) と `identity.scope` の `docs` を持たない SHALL NOT。concepts の docs/ 前提記述 2 箇所 (comment-policy の対象外リスト / test-execution の README・docs 節) は skills/ 体制の記述へ差し替えられる SHALL (comment-policy には skills/ のコード例が「原則コメントレス・最小限は英語」の別規約である旨を含める)。凍結資料 (`openspec/`、`kasane/changes/archive/`、ロードマップの過去記録) を除き、リポジトリ内に `docs/` 配下への参照が残らない SHALL NOT。

#### Scenario: 廃止後の残存検査

- **GIVEN** 廃止タスク完了後のリポジトリ
- **WHEN** `docs/` の存在と、凍結資料を除く全ファイルの `docs/` 参照を grep する
- **THEN** `docs/` ディレクトリは存在せず、参照は 0 件である

### Requirement: 完了検査一式

初期生成の完了条件として、phase-11 スペックの「整合性チェック一式」8 検査と同等の検査 (①concepts 網羅、②en/ja 節構成一致、③コードブロック byte 一致、④frontmatter、⑤旧名残 grep (廃止 API・docs/ 参照新設・openspec 参照)、⑥内部リンク解決、⑦identity-lint (skills/ を検査範囲に含む)、⑧配信識別子の表記ゆれ grep) に加え、⑨ローカル絶対パス lint (移送済み concepts と config 整理後の全検査範囲)、⑩ツール最低バージョン一致 (ビルドファイルの取得値 ↔ 各 SKILL.md 導入節の記載) をすべて通過する SHALL。

#### Scenario: 完了検査の通過

- **GIVEN** 生成・レビュー反映後の skills/ 一式と manifest
- **WHEN** 8 検査を実行する
- **THEN** すべて通過する (失敗があれば該当生成物を修正して再検査する)
