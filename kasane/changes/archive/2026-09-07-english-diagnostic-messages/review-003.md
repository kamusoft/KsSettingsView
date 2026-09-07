# レビュー結果: english-diagnostic-messages (003 回目)

**日付**: 2026-09-07
**判定**: APPROVED

## サマリー

前回の Major (句点条項の軸が実態と合っていない) は解消した。「書き方」節の表はメッセージの形を軸にした 3 行になり、**リポジトリの本体コードにある診断文字列を全件当てて不適合 0 件**を確認した (Kotlin 20 種・Swift 20 種・C# 例外 8 種・MSBuild テキスト 1 件)。前回の Suggestion 2 件 (既定値の二重定義・例文の大小文字) もコードと規約の双方で解消済み。3 platform のテストは全件緑 (iOS 1027 / Android 2854 / MAUI 520、いずれも 0 failures)、lint 4 本も本 change への指摘 0 件。残るのは規約表の分類語の詰めが 1 件だけで、実害はなく確定を妨げない。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| cross/comment-policy.md | 常時 (always) |
| cross/test-execution.md | テストを実行するとき・テスト結果を報告するとき |
| cross/diagnostic-message-language.md | 本変更が新設した規約そのもの (レビュー対象) |
| cross/public-identifiers.md | 新規規約の frontmatter / index 記法の比較対象として参照 (規約自体の適用はなし) |

適用外と判定: cross/sample-parity.md (`samples/` に触れていない)、cross/runtime-behavior-verification.md (実行時挙動の不具合修正ではない)、cross/user-skill-api-listing.md (`skills/` に触れていない)、cross/aiforms-origin-reference.md (未移植機能の実装ではない)、cross/local-development-setup.md · cross/release-procedure.md (guide。今回の作業契機に当たらない)、ios/ · maui/ の各ドメイン規約 (Swift 6 言語モード適合・検証ホスト実行のいずれにも当たらない)。

ksn-core の `references/handbook.md` (規範層の形式・index 規約) と `references/rule-style.md` (ルール記述規約) を新規規約の判定基準に用いた。`kasane/lessons/code-review.md` の重点観点 L-001 (ミューテーションによる検出力の実測) は、文言のみの変更でアサーション検出力が争点にならないため適用しない。「指摘しないこと」は未昇格。

## 前回指摘の解消状況

| review-002 の指摘 | 状態 | 確認内容 |
|---|---|---|
| 🟠 句点条項の軸が「言語」になっている | **解消** | 表がメッセージの形を軸にした 3 行になり、本体コードの診断文字列を全件照合して不適合 0 件 (下の「検証したこと」) |
| 🔵 診断に載せる既定値が定数と二重定義 | **解消** | Kotlin は `LocalTime.MIDNIGHT.toString()` / `EPOCH_DATE.toString()` と実際に返す値から導出、Swift は `defaultTimeText` / `defaultDateText` を新設して parse と diagnose が同じ定数を使う |
| 🔵 規約の例文と iOS 実装で先頭の大小文字が食い違う | **解消** | 例文に出典 (Android の Bridge 診断) を添え、iOS は接頭辞に続けるため小文字始まりになる旨を併記 |
| 🔵 作業ツリーに別 change の差分が同居 | 状況継続 (対処は commit 時) | 作業ツリーには `fix-release-central-validation-wait` の差分が今も同居している。本レビューはその内容を評価対象に含めていない。commit を本 change のファイルへ限定することでのみ解消する性質のため、成果物からは検証できない |

## 検証したこと

- **ビルド / テスト** (cross/test-execution.md の手順で全件実行、絞り込みなし)
  - iOS: `xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,id=<iPhone 17 Pro>'` → バンドル集計行の合算で **1027 tests / 0 failures** (Bridge 166 / Core 88 / SwiftUI 96 / TestSupport 7 / UI 670)、`** TEST SUCCEEDED **`
  - Android: `./gradlew test --rerun-tasks` → `build/test-results/test*UnitTest/TEST-*.xml` 222 ファイルの合算で **2854 tests / 0 failures + 0 errors**、`BUILD SUCCESSFUL`
  - MAUI: `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` → **520 tests / 0 failures**
- **規約表の全件照合** (前回 Major の判定根拠): 本体コードの診断文字列を収集して 3 行の表に当てた。**不適合 0 件**
  - 前回 Major で挙げた 7 箇所はすべて行 2 (複数の文を連ねた診断) に収まる。3 片連結で 2 文になる `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DateSelectionSheet.kt:262-264` と `.../NumberPickerCellViewHolder.kt` の 2 件も、連結後の全文で 2 文構成であることを確認した
  - MSBuild は `maui/KsSettingsView.Maui/buildTransitive/KsSettingsView.Maui.targets:28` の 1 件のみで、2 文構成・各文句点あり → 行 2 に適合
  - C# の例外 message は facade / binding 合わせて 8 種すべて句点で終わる → 行 3 に適合
  - Swift / Kotlin の 1 文の短句 (`init(coder:) is not supported`、`viewType $viewType is not registered in KsCellRegistry` 等) はすべて句点なし → 行 1 に適合
- **英語化漏れ**: 本体ソース (`ios/Sources/` / `android/*/src/main/` / `maui/KsSettingsView.Maui/` / binding 2 本) を日本語文字で走査し、文字列リテラル内の日本語は **0 件**。残るヒットはすべてコメント (`//` · KDoc · XML コメント · `.csproj` コメント)
- **既定値の導出の正しさ**: `LocalTime.MIDNIGHT.toString()` = `"00:00"` は `TIME_FORMAT`、`EPOCH_DATE.toString()` = `"1970-01-01"` は `DATE_FORMAT` と一致する (`LocalTime.toString()` は秒が 0 なら分までしか出さず、`LocalDate.toString()` は 4 桁年で ISO 形式)。診断文と実際の戻り値が構造上ずれない
- **lint**: `local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` いずれも 0 件 (comment-policy は検査対象 784 ファイル)。`doc-structure-lint.py` は `diagnostic-message-language.md` への指摘なし (残る指摘は roadmaps 側の既存債務)
- **規範層の形式**: frontmatter (`kind: rule` / `applies-when` / `title` / `description` / `timestamp`) が ksn-core の形式に適合。`paths` の 5 本と `android/*/src/main/**` がすべて実在ディレクトリに一致し、`../../concepts/core/cells/picker-selection-surface.md` と `comment-policy.md` へのリンク先も実在。index への追加行は「適用のきっかけ」を作業で書く規約に沿う
- **既存の決定との衝突**: `kasane/decisions/` に例外・診断メッセージの言語を定めた ADR はなく、concepts にも本規約と矛盾する記述はない
- **コメント規約**: 新設した Swift の 2 定数の doc コメントは日本語で、作業文書・通番への参照を含まず、`private` 宣言のため公開 doc コメントの制約も掛からない

## 指摘事項

### [🔵 Suggestion] 表の行 1 の括弧書きがログを含まず、本変更自身の Bridge 診断が明示的にはどの行にも入らない

**該当箇所**: `kasane/handbook/cross/diagnostic-message-language.md:47-48`

**問題点**: 行 1 の見出しは「Swift / Kotlin の 1 文で終わる短句 (例外・assert・deprecated 警告)」で、括弧書きにログが入っていない。一方の行 2 は「複数の文を連ねた診断」を条件にする。本変更が書いた Bridge の診断 (`android/.../KsBridgeValueTransport.kt:225-228`、`ios/Sources/KsSettingsViewBridge/KsBridgeValueTransport.swift:197-199`) は **1 文のログ**であり、括弧書きを列挙とみなすと行 1 に入らず、文が 1 つなので行 2 にも入らない。同様に行 2 の見出しの「診断」は、実際に該当する `ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift:155` の `fatalError` を指す語としてはやや狭い。

本文の主語 (「1 文で終わる短句」) を読めば句点なしが正解と分かるため実害はなく、現行コードに違反も生じない。規約が自分の change の成果物を分類しきれていない点だけが残る。

**推奨修正**: 行 1 の括弧書きに「ログ」を足す (「例外・assert・ログ・deprecated 警告」)、または括弧書きを例示と分かる書き方 (「例:」) に改める。行 2 の「診断」は「診断・エラー」等に広げると `fatalError` も自然に収まる。

## アクションプラン

1. **Suggestion**: 表の行 1 の括弧書きにログを含める (規約 1 ファイルの文言修正のみ。コード変更・テスト再実行は不要)
2. commit するファイルを本 change の 9 ファイル (`android/` 2 · `ios/` 4 · `maui/` 1 · `kasane/handbook/cross/` 2 — 新規 `diagnostic-message-language.md` と `index.md`) と change 配下の成果物に限定し、`fix-release-central-validation-wait` の差分と混ぜない
