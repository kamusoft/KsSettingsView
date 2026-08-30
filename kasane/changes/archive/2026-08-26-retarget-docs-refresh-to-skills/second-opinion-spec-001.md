# セカンドオピニオン: retarget-docs-refresh-to-skills (spec-001)

**相方**: codex / **label**: so-spec-retarget-docs-refresh / **日付**: 2026-08-25 / **対象**: 提案一式 (proposal.md / specs/docs-refresh/spec.md / tasks.md、参照: .agents/skills/docs-refresh/SKILL.md・kasane/decisions/cross/0022-user-docs-as-agent-skills.md)

---

# レビュー結果: retarget-docs-refresh-to-skills

**日付**: 2026-08-25
**判定**: **NEEDS_DISCUSSION**

## サマリー

方向性は ADR-0022 と概ね整合していますが、manifest の状態遷移と初期生成の責務に、実装前に解消すべき矛盾があります。特に `--readme-only` による未反映差分の消失と、docs-refresh が初期生成を拒否する一方で phase-12 が docs-refresh に初期生成させる計画になっている点は、このままでは運用を詰まらせます。

指摘件数: Critical 0 / Major 8 / Minor 1 / Suggestion 0

## 指摘事項

### [🟠 Major] `--readme-only` が未反映の concept 差分を消費し得る

**該当箇所**: `specs/docs-refresh/spec.md:119`、`specs/docs-refresh/spec.md:135`
**問題点**: `--readme-only` は Skill 本体を更新しない一方、成功後は manifest 全体を現在の concepts ハッシュで書き直すよう読めます。その場合、Skill に未反映の concept 変更がスナップショット上は処理済みとなり、次回の通常実行で再検出できません。部分承認を許す場合も、同じ concept が複数 target に対応すると同様の問題が起きます。
**推奨修正**: `--readme-only` では concepts / targets / excluded のスナップショットを更新しない、と明記してください。部分承認を許可するかも決定し、許可するなら未承認 target の差分を失わない状態管理を定義してください。対応 Scenario とテストを追加してください。

### [🟠 Major] 初期生成の責務が phase-12 と正面衝突している

**該当箇所**: `proposal.md:12`、`proposal.md:23`、`specs/docs-refresh/spec.md:25`、`kasane/roadmaps/package-distribution/phases/phase-12-skills-rollout/agenda.md:3`、`kasane/roadmaps/package-distribution/phases/phase-12-skills-rollout/agenda.md:7`
**問題点**: 本提案は manifest 不在時の初期生成を明確に拒否しますが、phase-12 agenda は「改修した docs-refresh」「docs-refresh --all」で初回生成すると規定しています。両方を同時には満たせません。
**推奨修正**: 初期生成主体を一つに確定してください。本提案を維持するなら、phase-12 は承認済み change の実装として skills/ と manifest を直接初期生成し、その後だけ docs-refresh を使用する、と agenda を更新してください。

### [🟠 Major] 新しい AGENTS 規約が初期生成を禁止する閉路を作る

**該当箇所**: `proposal.md:17`、`tasks.md:19`
**問題点**: 「skills/ と README 群の書き換えは docs-refresh 経由のみ」という規約は、docs-refresh 自身が manifest 不在時に停止する契約と組み合わせると、phase-12 が初期生成する正規経路まで禁止すると解釈されます。初期作成を「書き換え」に含めないという暗黙前提にも依存しています。
**推奨修正**: 規約を「継続的な追従更新は docs-refresh 経由のみ。初期生成・構成変更は承認済み Kasane change で行う」のようにし、phase-12 の例外を明文化してください。

### [🟠 Major] manifest v3 の妥当性を判定できる規範スキーマがない

**該当箇所**: `specs/docs-refresh/spec.md:9`、`specs/docs-refresh/spec.md:25`、`specs/docs-refresh/spec.md:135`
**問題点**: `targets` のキーの基準ディレクトリ、concept パスの基準、`excluded` の理由表現、`readmes` の型、必須更新メタデータが決まっていません。また version だけ 3 で、必須キー欠落・型不正・不正パスを含む manifest の扱いもありません。phase-12 と docs-refresh が別々に実装するため、JSON 例を実装者判断に任せると相互運用できない危険があります。
**推奨修正**: spec に規範的な v3 スキーマ例と不変条件を置き、構造不正も「破損」として書き込み前に停止させてください。言語抜き target の具体例、削除 concept の target 整理、存在しない target/source の扱いも Scenario 化してください。

### [🟠 Major] concept 変更から README への追従経路が失われている

**該当箇所**: `specs/docs-refresh/spec.md:9`、`specs/docs-refresh/spec.md:35`、`specs/docs-refresh/spec.md:119`
**問題点**: `targets` は「Skill ファイル → concepts」だけで、`readmes` は列挙にすぎません。通常の concept 差分から README を逆引きする規則がないため、README に残る公開識別子や利用案内などの concept 由来記述を追従対象にできません。現行スキルが持つ concept → README 対応が消えることへの代替が定義されていません。
**推奨修正**: README はコード正チェックだけで維持すると明示して内容を限定するか、README にも concept 依存を記録できるスキーマへ変更してください。`readmes` に含める具体的な README 群と追加・削除方法も定義してください。

### [🟠 Major] 完動コードを生成するのに実装コードとの照合が要求されていない

**該当箇所**: `specs/docs-refresh/spec.md:69`、`.agents/skills/docs-refresh/SKILL.md:185`、`kasane/decisions/cross/0022-user-docs-as-agent-skills.md:22`
**問題点**: ADR は「完動コード」のレシピを要求し、現行スキルは API 署名とコード例を実装コードで最終確認させています。しかし新しい生成プロンプトの必須制約から、この確認が落ちています。3種の機械チェックはモジュール・Sample・バージョンだけで、API 例の正しさを保証しません。
**推奨修正**: 委譲プロンプトに「API 署名とコード例は実装コード・テストで確認する」「concepts と実装が矛盾したら drift として報告し独断で正本を変えない」を追加してください。可能なコード例には構文・コンパイル確認方法も定めてください。

### [🟠 Major] 維持対象の機械チェック表が現行コードと既に食い違っている

**該当箇所**: `tasks.md:9`、`.agents/skills/docs-refresh/SKILL.md:131`
**問題点**: task は現在の取得元テーブルを維持するとしていますが、現行スキルは AGP を各 module の `id(...) version` から取得すると記載しています。実際の正は `android/gradle/libs.versions.toml:10` の version catalog です。また phase-9 の要件にある Kotlin/JDK、MAUI Sample の画面列挙元も新 spec では決まっていません。
**推奨修正**: 現行表の維持ではなく、現在のリポジトリに基づく「項目・取得元・抽出方法・突合先」の規範表へ更新してください。iOS / Android / MAUI の Sample すべてについて列挙元を明示してください。

### [🟠 Major] 公開対象 skills/ が identity/secret lint の検査外になる

**該当箇所**: `tasks.md:20`、`kasane/config.yaml:56`
**問題点**: `identity.scope` は現在 `[kasane, openspec, docs]` で、公開される `skills/` を含みません。本提案は config の context だけを変更対象としているため、docs/ の後継成果物が個体・個人・秘密情報の検査から漏れます。spec のローカル絶対パス検査だけでは代替できません。
**推奨修正**: phase-11 で `skills` を identity scope に追加し、`docs` は削除される phase-12 まで残してください。整合性チェックにも identity-lint の成功を含めるのが安全です。

### [🟡 Minor] 検証タスクが主要 Requirement を直接検証していない

**該当箇所**: `tasks.md:24`
**問題点**: ダミー入力で検証するのは節構成・コードブロック・frontmatter の3種だけです。逆引き、網羅検査、manifest 構造不正、承認前無変更、各実行フラグ、旧名残、リンク、絶対パス、識別子、コード正チェックには正例・負例がありません。また `--all --readme-only` の同時指定時の扱いも未定義です。
**推奨修正**: Requirement / Scenario と検証項目の対応表を tasks に追加し、各埋め込みスクリプトを一時 fixture で正例・負例とも確認してください。フラグ同時指定は拒否または優先順位を明記してください。

## アクションプラン

1. `--readme-only` と部分承認時の manifest 更新規則を確定する。
2. phase-12 の初期生成主体と AGENTS の例外規定を整合させる。
3. manifest v3 の規範スキーマ、README 依存、削除 concept の扱いを確定する。
4. コード例照合規則と機械チェック取得元を現行コードに合わせる。
5. identity lint と Scenario ベースの検証タスクを追加する。

指定どおり静的レビューのみ実施し、ビルド・テスト・ファイル書き込みは行っていません。

---

## 突き合わせ結果 (ホスト側判定: 2026-08-25)

ホスト側自己レビュー (2 周、指摘なし) との突き合わせ。全件が相方のみの指摘のため、根拠の強さで採否を判定した。

| # | 指摘 | 採否 | 根拠 |
|---|---|---|---|
| 1 | --readme-only が concept 差分を消費 | **採用** | 実害シナリオ具体的。manifest 更新規則を「--readme-only では concepts スナップショット非更新」「未処理 concept は旧ハッシュ保持」として spec に反映 |
| 2 | 初期生成の責務が phase-12 と衝突 | **採用** | phase-12 agenda の現物で確認 (「docs-refresh --all で初回生成」の記述あり)。phase-11 決定 (初期生成は change が担う) が後発のユーザー確定事項のため、phase-12 agenda 側を申し送りで更新 |
| 3 | AGENTS 規約の閉路 | **採用** | 論理的に正しい。「継続的な追従更新は docs-refresh 経由のみ。初期生成・構成変更は承認済み change で行う」へ文言修正 |
| 4 | manifest v3 規範スキーマ欠如 | **採用** | phase-12 と本 change が別実装のため相互運用リスクは実在。spec に規範スキーマと不変条件・構造不正 = 破損扱いを追加 |
| 5 | concept → README 追従経路の喪失 | **一部採用 (明確化のみ)** | readmes を「列挙のみの粗い管理」とするのは phase-10 の確定決定 (ADR-0022 出典) であり、スキーマ変更は却下済み案の再提案にあたる。spec に「README は concepts 逆引きの対象外、コード正チェックと --all / --readme-only で維持」と限定を明文化 |
| 6 | コード例の実装照合の欠落 | **採用** | ADR-0022 の「完動コード」要求と現行スキルの既存制約の両方に接地。内容規約に照合と drift 報告を追加 |
| 7 | 機械チェック取得元の陳腐化 | **採用** | android/gradle/libs.versions.toml の現物で確認 (agp = "8.13.2")。tasks を「取得元を現行リポジトリの正に更新」へ修正 |
| 8 | identity lint scope に skills 未登載 | **採用** | config.yaml の現物で確認 (scope: [kasane, openspec, docs])。phase-11 で skills を追加、整合性チェックにも identity-lint を組み込み |
| 9 | 検証タスクの網羅不足 (Minor) | **採用** | tasks の検証を fixture 正例・負例へ拡張、フラグ同時指定はエラーと定義 |

未解決: なし (NEEDS_DISCUSSION の主因 #1・#2 はいずれも採用・反映で解消)。
