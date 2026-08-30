# セカンドオピニオン: rollout-user-skills (spec-001)
**相方**: codex / **label**: so-spec-rollout-user-skills / **日付**: 2026-08-26 / **対象**: 提案一式 (proposal.md / specs/user-skills/spec.md / tasks.md)
---
# レビュー結果: rollout-user-skills

**日付**: 2026-08-26  
**判定**: **NEEDS_DISCUSSION**

## サマリー

受け入れ条件同士に両立不能な矛盾があり、現状のままでは全 Scenario を満たす実装を作れません。また、移送対象にローカル絶対パスが残ること、manifest の初期状態・生成順序・内容網羅性が規定不足であることから、初回 docs-refresh が誤検出するリスクがあります。

**指摘件数**: Critical 2 / Major 5 / Minor 1 / Suggestion 0  
指定どおり、ファイル作成およびビルド・テスト実行は行っていません。

## 指摘事項

### [🔴 Critical] ルート README の変更制限と `docs/` 参照ゼロ条件が両立しない

**該当箇所**: `specs/user-skills/spec.md:85`、`specs/user-skills/spec.md:105`、`README.md:34`

**問題点**:  
前者はルート README の差分を「導線1文と構成表1行の追記のみ」に限定しています。一方、後者は凍結資料を除く全ファイルから `docs/` 参照をなくすよう要求しています。

現行 README には `docs/` 構成表、ドキュメント節、知識の正本説明、ビルド要件、Android 注意事項など多数の参照があります（`README.md:34,60-64,68,74,95`）。加えて `android/README.md`、`samples/ios/README.md`、`samples/android/README.md` にも `docs/` リンクが残っています。現在の tasks はこれらの更新を割り当てていないため、Task 6.3 は必ず失敗するか、README 変更範囲の Requirement に違反します。

**推奨修正**:  
以下のどちらを採るか明示的に決定してください。

1. 本 change で README 群の `docs/` リンクを skills または適切な現存先へ置換し、ルート README の「2箇所のみ」を撤回する。
2. `docs/` 廃止を phase-9 後へ延期する。

前者を採る場合は proposal の Impact、manifest の `readmes`、tasks に `android/README.md`、`maui/README.md`、`samples/*/README.md` を明記し、変更範囲 Scenario を更新する必要があります。

### [🔴 Critical] 移送対象にローカル絶対パスと `file://` リンクが残る

**該当箇所**: `tasks.md:5`、`docs/legacy-aiforms-reference.md:11`、`docs/legacy-aiforms-reference.md:56`

**問題点**:  
移送タスクは frontmatter・凍結注記・相互リンクの追加しか要求していませんが、対象文書には `/Volumes/<VOLUME>/...` 形式のローカル絶対パスと `file:///Volumes/...` 形式のリンク（いずれも実名入り。本引用ではプレースホルダ化済み）が複数残っています（ほかに 226、272、324、335、356、377 行）。

現在は `docs/` が lint 除外ですが、`kasane/concepts/` へ移した時点で `local-path-lint.py` の検査対象になります。さらに公開リポジトリへローカル環境情報と解決不能なリンクを持ち込むことになります。Task 4.1 は `identity-lint` しか明記しておらず、独立した `local-path-lint` も漏れています。

**推奨修正**:  
移送要件に次を追加してください。

- ローカル絶対パスと `file://` URL をすべて除去する。
- 参照先は公開 URL、`../<repo>/...` 形式、または環境非依存の論理パスへ変換する。
- 移送後のファイルを含めて `scripts/local-path-lint.py` を実行する。
- `config.yaml` 変更後、リポジトリ全体の local-path/identity lint を再実行する。

### [🟠 Major] manifest が扱う「concept」の集合と初期ハッシュ完全性が未定義

**該当箇所**: `specs/user-skills/spec.md:59`、`tasks.md:15`

**問題点**:  
「全 concept ファイル」とありますが、`docs-refresh` の実態は `index.md`、`log.md`、`rules.md` を対象外にしています（`.agents/skills/docs-refresh/SKILL.md:69,130`）。どちらを manifest の `concepts`、`targets`、`excluded` に含めるべきかが提案から判定できません。

また v3 スキーマは `concepts` を必須 object とするだけで、初期版が対象 concept 全件の正しい SHA-256 を持つことを要求していません。空または欠落したハッシュでも現在の Scenario を通り得て、最初の docs-refresh ですべてが「新規 concept」と誤検出されます。

**推奨修正**:  
対象集合を次のように明文化し、Scenario を追加してください。

- `kasane/concepts/**/*.md` から basename が `index.md`、`log.md`、`rules.md` のものを除く。
- `concepts` のキー集合は上記集合と完全一致する。
- 各値は初期生成完了時点の SHA-256 と一致する。
- `targets ∪ excluded` も同じ集合を過不足なく覆う。
- `excluded` の理由は空文字列不可とする。

### [🟠 Major] manifest はファイル単位なのに、ワーカー報告は Skill 単位になっている

**該当箇所**: `specs/user-skills/spec.md:39`、`specs/user-skills/spec.md:59`

**問題点**:  
manifest v3 の `targets` は「Skillファイル → 源泉 concepts」の対応です。しかしワーカーへ要求しているのは「担当 Skill の源泉 concepts の列挙」であり、`SKILL.md`、`references/cells.md`、`references/updates.md` 等へのファイル別帰属が得られません。

実装者が全 concept を全ファイルへ付ければスキーマと網羅検査は通りますが、逆引き更新が過剰になり、manifest の目的を損ないます。

**推奨修正**:  
ワーカー成果を `言語抜き相対ファイルパス -> concepts[]` のマップとして規定してください。各生成ファイルが `targets` にちょうど1キーとして存在し、報告内容と manifest が一致する Scenario も追加してください。

### [🟠 Major] manifest 書き出し後に concept を変更するため、完成時点でハッシュが古くなる

**該当箇所**: `tasks.md:15`、`tasks.md:35`

**問題点**:  
Task 2.5 で manifest 初期版を書いた後、Task 6.1 で `comment-policy.md` と `test-execution.md` を変更します。したがって Task 2.5 で記録した `concepts` ハッシュは完成時点のリポジトリと一致しません。

Task 5.3 の機械検査再実行も Task 6 より前であり、最終変更後に manifest 網羅・内部リンク・lint 等を再検査するタスクがありません。

**推奨修正**:  
最終状態を作った後に manifest を確定する順序へ変更してください。最低でも Task 6 後に以下を追加すべきです。

- 全 concept ハッシュの再計算
- manifest の最終書き出し
- 8検査、内部リンク、local-path/identity lint の再実行
- manifest と作業ツリーのハッシュ一致確認

### [🟠 Major] 内容の網羅性を判定できる受け入れ基準がない

**該当箇所**: `specs/user-skills/spec.md:27`、`specs/user-skills/spec.md:57`

**問題点**:  
仕様が定めるのは能力マップやレシピの「形」と concept の割り当てだけです。manifest に concept を列挙しても、その内容が実際に Skill へ反映されたことは保証されません。極端には、ほぼ空のレシピに全 concept を `targets` として割り当てても機械検査を通せます。

特に AiForms 移行 Skill は API 対応表と非互換点の具体的な範囲が定義されていません。phase-9 agenda でも範囲が未決事項として残っています（`kasane/roadmaps/package-distribution/phases/phase-9-docs/agenda.md:16`）。

**推奨修正**:  
Skill ごとに最低限含む能力・レシピのチェックリストをデルタスペックへ追加してください。少なくとも以下が必要です。

- 各 platform Skill の対象 API・利用経路・更新・styling・CustomCell の必須項目
- AiForms 移行 Skill の対象旧 API、対応先、非互換点、代替手段
- 各必須項目と源泉 concept／コード位置の追跡表
- 「各必須項目が能力マップまたはレシピに現れる」Scenario

### [🟠 Major] 最低バージョンと「完動コード」の正しさを検証するタスクがない

**該当箇所**: `specs/user-skills/spec.md:29`、`specs/user-skills/spec.md:115`、`tasks.md:24`

**問題点**:  
各 Skill に最低バージョンと完動コードを要求していますが、完了検査は構造・翻訳一致・lint 等だけです。phase-11 にあったコード正チェック（モジュール、Sample、ツール最低バージョン）は8検査とは別の Requirement であり、本提案の「同等の8検査」には継承されていません。

そのため、古い AGP/Gradle/Kotlin 値やコンパイル不能なコードでも、en/ja で同じなら通過できます。実際、現行 README の Android バージョンは実装と既に食い違っています。

**推奨修正**:  
以下を独立した完了条件と tasks に追加してください。

- build file から取得した最低バージョンとの一致検査
- API 名・署名のコード／テストとの突合
- コード例を既存のコンパイル済み Sample から引用するか、抽出してコンパイルできる検証方法
- drift が見つかった場合の記録先と、完了を継続できる条件

### [🟡 Minor] frontmatter の必須性と検査条件が曖昧

**該当箇所**: `specs/user-skills/spec.md:19`

**問題点**:  
`license`、`metadata`、`metadata.source` が必須か任意か明確ではありません。また「ja版 description に英語キーワードを併記」は、何を英語キーワードと認めるか判定不能です。Scenario も `metadata.source` の存在・値を検証していません。

**推奨修正**:  
各フィールドの required/optional を明示し、`metadata.source` の許容値または形式を定義してください。ja description は、Skill ごとに含める具体的な英語トリガー語を列挙するか、機械検査ではなくレビュー項目であることを明記してください。

## アクションプラン

1. ルート README の変更範囲と `docs/` 廃止時期を決定する。
2. legacy 資料のパス無害化を移送要件へ追加する。
3. manifest の concept 集合・完全ハッシュ・ファイル別 targets を定義する。
4. Task 6 後に manifest 確定と全検査再実行を置く。
5. Skill ごとの内容網羅表、最低バージョン、コード例の検証基準を追加する。
6. frontmatter の必須・任意条件を明確化した後、proposal / spec / tasks の対応を再レビューする。



## 突き合わせ結果 (2026-08-26)

ホスト側自己レビュー (2 周、指摘なし) との突き合わせ。裏取りは実物 (README 群 grep / legacy 本文 / docs-refresh SKILL / 自アーティファクト) で実施。

- **Critical「README 変更制限と docs/ 参照ゼロの両立不能」**: 根拠強 (README 群に docs/ 参照が計 15 箇所実在)。**採用** — ホスト側の見逃し。修正方向 (README 群のリンク差し替えを本 change に含める) は agenda 決定「追記 2 箇所のみ」の変更を伴うため**オーナー承認 (2026-08-26)**: 案 A (README 群の docs/ リンク差し替え・除去を本 change に含め、ルート README の変更範囲を「導線追記 + docs/ 参照の解消」に緩和) を採用
- **Critical「移送対象のローカル絶対パス・file://」**: 根拠強 (legacy 本文に 10 箇所実在、移送先は lint 検査対象)。**採用** — 移送要件にパス無害化と local-path lint を追加
- **Major「concept 集合と初期ハッシュ完全性が未定義」**: 根拠強 (docs-refresh は index/log/rules を対象外)。**採用** — 集合定義・キー完全一致・SHA-256 一致・excluded 理由非空を spec に明文化
- **Major「targets ファイル単位 vs ワーカー報告 Skill 単位」**: 根拠強 (v3 規範との不整合)。**採用** — 報告をファイル別マップに規定
- **Major「manifest 書き出し後の concepts 変更でハッシュ陳腐化」**: 根拠強 (Task 2.5 → 6.1 の順序欠陥)。**採用** — manifest 確定と最終検査を Task 6 の後へ移動
- **Major「内容網羅の受け入れ基準がない」**: **部分採用** — 移行 Skill は spec-summary の旧公開 API 網羅を spec 化。platform Skill の API 別チェックリスト新設は不採用 (concept 単位網羅 + レビューでの反映確認が ADR-0022 の設計。代わりに独立レビューへ「targets の各 concept の内容反映確認」を明記)
- **Major「最低バージョン・完動コードの検証タスクがない」**: **部分採用** — ツール最低バージョン一致検査を完了検査に追加、API 署名突合を独立レビュー観点に明記。コード例の抽出コンパイル検証は不採用 (コード例は既存テスト・Sample 実コードを素材とする規約 + レビューで担保。スニペット単体のビルド基盤構築は本 change の範囲を超える)
- **Minor「frontmatter 必須性・英語キーワード判定が曖昧」**: **採用** (軽量) — 全フィールド必須を明示、英語キーワード併記は機械検査対象外のレビュー項目と明記
