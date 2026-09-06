# セカンドオピニオン: cell-style-dark-appearance-parity (spec-001)
**相方**: codex / **label**: so-spec-cell-style-dark-appearance-parity / **日付**: 2026-09-06 / **対象**: proposal.md / specs/ (settings-view-android-ui, settings-view-ios-ui, maui-bridge) / tasks.md (入力素材: exploration.md)
---
# レビュー結果: cell-style-dark-appearance-parity

**日付**: 2026-09-06  
**判定**: **NEEDS_DISCUSSION**

## サマリー

Critical 0件、Major 3件、Minor 5件です。

特に、Android API が既に Maven Central へ公開済みなのに「未配信」を破壊的変更の根拠としている点は、実装前に方針決定が必要です。また、MAUI の Requirement は現行契約上表示に影響しないプロパティまで一律に対象化しています。

本レビューは依頼どおり静的レビューのみで、ビルド・テストは実行していません。レビュー結果ファイルも作成していません。

## 照合した規約

- `cross/comment-policy.md`（always）
- `cross/test-execution.md`（テスト計画）
- `cross/sample-parity.md`（Sample を用いた確認）
- `cross/local-development-setup.md`（ダーク外観・MAUI Sample 確認）
- `ios/swift6-language-mode-check.md`（`ios/Sources/` の変更）
- `maui/integration-host-verification.md`（facade → native の end-to-end 確認）

## 指摘事項

### [🟠 Major] 公開済み Android API を「未配信」とする破壊的変更の前提が失効している

**該当箇所**: `proposal.md:33`、`proposal.md:42`、`README.md:56`、`kasane/concepts/android/architecture/build-toolchain.md:57`、`kasane/decisions/core/0030-theme-dark-appearance-library-owned-light-dark-defaults.md:96`

**問題点**: proposal は「ライブラリは未配信で外部利用者なし」を理由に `Color?` → `Color` の破壊的変更を受容しています。しかし、現行 README は `0.1.0-beta.1` を案内しており、concepts も Maven Central からの公開後 smoke を実証済みとしています。

この変更は `null` を渡すコードだけでなく、旧 constructor / `copy` をコンパイル済みの Android 利用者にバイナリ非互換を生じさせます。ADR-0030 の Revisit When にある「配信後の互換問題」の条件にも到達しています。現状の M 級根拠も、この失効した前提に依存しています。

**推奨修正**: 次のどちらを採るかオーナー判断を得てください。

- 互換経路を設計し、旧 API から段階的に移行する。
- prerelease 間の breaking change として明示的に受容し、影響範囲、移行方法、リリース方針、旧版 consumer を使った互換性検査を proposal/design/tasks に加える。

この判断を含めるなら、複数 capability 横断でもあるため L 級へ上げ、`design.md` で互換案と却下理由を残すのが妥当です。

### [🟠 Major] MAUI の色プロパティ契約が適用不能な Cell／platform まで含んでいる

**該当箇所**: `specs/maui-bridge/spec.md:3`、`specs/maui-bridge/spec.md:5`、`maui/KsSettingsView.Maui/CustomCell.cs:211`、`maui/KsSettingsView.Maui/CustomCell.cs:231`、`maui/KsSettingsView.Maui/DatePickerCell.cs:145`、`kasane/decisions/maui/0004-maui-idiomatic-types-for-styling.md:19`

**問題点**: Requirement は `CellBase` 派生 Cell の列挙色プロパティを変更すると、表示中の行が「新しい色で描き直される」と一律に規定しています。しかし現仕様では:

- `CustomCell` は共通テキストスロットを持たず、`TitleColor` 等は snapshot に載せず更新対象にもしていません。
- `AndroidButtonColor` は Android 固有で、iOS では表示・挙動に影響しません。
- 表示していないスロットの色は、当然ながら観察可能な描画変化を持ちません。

したがって、この SHALL は現行契約と両立せず、すべての列挙プロパティについて成立させる実装もできません。

**推奨修正**: Requirement を「対象 Cell が実際に描画する色プロパティ」に限定してください。併せて以下を明記してください。

- `AndroidButtonColor` は Android のみで有効。
- `CustomCell` のテキスト系 style は従来どおり silent no-op。
- 「内容更新が配信されること」と「画面上の色が変わること」を分離する。
- platform 固有値と CustomCell の no-op を負の Scenario で固定する。

### [🟠 Major] 10個の data class の手書き `hashCode` 追随が tasks から漏れている

**該当箇所**: `proposal.md:34`、`tasks.md:9`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/ButtonCell.kt:59`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCell.kt:89`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SwitchCell.kt:58`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CheckboxCell.kt:50`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SimpleCheckCell.kt:54`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/RadioCell.kt:57`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerCell.kt:94`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/NumberPickerCell.kt:74`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerCell.kt:65`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerCell.kt:78`

**問題点**: proposal/tasks は `DatePickerCell` だけを手書き `equals` / `hashCode` の追随対象として挙げています。実際には対象10 Cellすべてが手書き `hashCode` を持ち、現在は `(accentColor?.hashCode() ?: 0)` 等の nullable 前提です。記載どおりに型だけ変えると9 Cellでもコンパイルエラーになります。

**推奨修正**: task 1.1 を、対象10 Cellすべての `hashCode` 追随として明記してください。等価性テストも代表1件だけでなく、12フィールドが `Unspecified` を含めて `equals` / `hashCode` 契約を満たすことを表形式テスト等で固定してください。

### [🟡 Minor] Cell 単位 AppThemeBinding の回帰検証が一時的な Sample spike にしか残らない

**該当箇所**: `tasks.md:5`、`tasks.md:6`、`tasks.md:39`、`tasks.md:44`、`kasane/handbook/maui/integration-host-verification.md:18`、`kasane/handbook/maui/integration-host-verification.md:118`

**問題点**: task 0.1 は正しい実物確認ですが、一時的な Sample 改変を元に戻すため、変更後に同じ契約を反復確認できません。net10 テストは fake gateway までで、platform gateway・binding・native 描画を実行しません。

**推奨修正**: `MauiHost` に Cell の `AppThemeBinding` を確認できる恒久シナリオを追加し、iOS / Android の両方で完了条件に含めてください。最低でも、実装後に spike を再実施するタスクと IntegrationHost / MauiHost の既定シナリオ実行を追加してください。

### [🟡 Minor] `ios/Sources/` 変更に必要な Swift 6 言語モード確認がない

**該当箇所**: `tasks.md:19`、`tasks.md:20`、`tasks.md:43`、`kasane/handbook/ios/swift6-language-mode-check.md:14`

**問題点**: `KsBridgeCellStyle.swift` を変更しますが、完了タスクは通常の iOS テストだけです。handbook は `ios/Sources/` を触るすべての変更に、一時的な Swift 6 言語モードでのビルドを要求しています。

**推奨修正**: Swift 6 一時設定ビルド、error 0件、`ios/Package.swift` の差分0件を確認するタスクを追加してください。

### [🟡 Minor] UI 変更なのに `ui/` と見た目の基準がない

**該当箇所**: `tasks.md:3`、`tasks.md:45`、`proposal.md:37`、`proposal.md:38`

**問題点**: ViewHolder と選択面の色解決を変更し、`Unspecified.toArgb()` による透明黒混入を主要リスクとしているため、UI に触れない変更とは扱えません。task 8.2 の目視確認はありますが、「本 change 前と同じ」の比較基準と照合結果を保持する `ui/brief.md` / `ui/verification/` がありません。

**推奨修正**: 新規デザインがない旨を `ui/brief.md` に記録し、直前 change の承認済み配色または実装前スクリーンショットを非回帰基準として参照してください。最終画像と照合結果は `ui/verification/` に残してください。

### [🟡 Minor] 「4段解決」という Scenario が中間段を検証できない

**該当箇所**: `specs/settings-view-android-ui/spec.md:29`、`specs/settings-view-android-ui/spec.md:34`、`tasks.md:25`

**問題点**: EntryCell の Scenario は「全段指定」と「全段未指定」、ButtonCell は Cell 固有値と CellStyle までしか扱いません。CellStyle 未指定時の Theme 採用や、Theme 未指定時の最終既定が欠けているため、CellStyle と Theme の順序を入れ替えた実装でも Scenario の一部を通過できます。

**推奨修正**: Cell固有／CellStyle／Theme／最終既定を各1ケースずつ観測する表形式テスト、または段ごとの Scenario を追加してください。

### [🟡 Minor] DTO の doc コメント文言が挙動スペックに混入している

**該当箇所**: `specs/maui-bridge/spec.md:41`、`specs/maui-bridge/spec.md:43`

**問題点**: DTO が輸送する値は挙動契約ですが、「doc コメントで『1対1』と述べない」はソース記述上の作業で、利用者が観察できる挙動ではありません。デルタスペックの Scenario でも判定できません。

**推奨修正**: DTO の輸送範囲だけを Requirement に残し、doc コメント訂正は `tasks.md` と comment-policy の照合項目へ移してください。

## アクションプラン

1. 公開済み Android API の互換性方針を決定し、変更級を再判定する。
2. MAUI Requirement を Cell・property・platform の適用範囲で限定する。
3. 全対象 Cell の `hashCode` 追随を tasks に追加する。
4. MAUI end-to-end、Swift 6、UI 非回帰の完了ゲートを補う。
5. Android の4段解決 Scenarioを強化し、docコメント作業をデルタスペックから分離する。

## 突き合わせ結果 (ホスト側の自己レビュー 2 周との照合、2026-09-06)

ホスト側の自己レビューは指摘 0 で通過していた (検査軸: Scenario 前提の成立可能性 / 条項ペアの矛盾 / tasks が参照するファイルの実在)。相方の指摘はすべてホスト側にない新規指摘のため「相方のみ」として根拠で判定した。

| # | 指摘 | 判定 | 根拠・対処 |
|---|---|---|---|
| Major 1 | 公開済み Android API を「未配信」とする前提の失効 | **採用 (オーナー裁定 2026-09-06: 案 A = beta 期間中の破壊的変更として受容、互換経路なし、M 級維持)** | 事実確認: tag `0.1.0-beta.1` は 2026-09-04 に配信済み (README のインストール例・`concepts/android/architecture/build-toolchain.md` の公開後 smoke の記述)。ADR-0030 (2026-09-05 accepted) の「未配信」前提はその時点で既に古く、前回 change の Theme / CellStyle の型変更も beta.1 配信後に行われている。Revisit When「配信後に互換問題」に該当。proposal Impact に配信状況・互換方針・移行手順を明記し、ADR-0030 の前提訂正を蒸留へ申し送った |
| Major 2 | MAUI Requirement が描画に影響しない Cell / platform まで一律に SHALL | **採用** | `CustomCell.AffectsSnapshot` はテキスト系 style を含まない (`maui/KsSettingsView.Maui/CustomCell.cs:231`)、`AndroidButtonColor` は iOS で無効。Requirement を「snapshot に写す項目は届く」「描画に使う項目だけ描き直す」に分離し、CustomCell の no-op を負の Scenario で固定 (specs/maui-bridge/spec.md、tasks 3.2 / 7.2) |
| Major 3 | 10 Cell の手書き `hashCode` 追随が tasks から漏れ | **採用** | grep で 10 Cell 全部に `accentColor?.hashCode() ?: 0` 等を確認。tasks 1.1 を 10 Cell 全部に、4.1 を 12 フィールドの表形式等価性テストに改訂 |
| Minor 1 | Cell の `AppThemeBinding` の回帰検証が一時 spike にしか残らない | **採用** | handbook maui/integration-host-verification.md は MauiHost を回帰資産と定める。tasks 3.3 (MauiHost に恒久シナリオ) と 8.4 (実行) を追加 |
| Minor 2 | `ios/Sources/` 変更の Swift 6 言語モード確認が無い | **採用** | handbook ios/swift6-language-mode-check.md は `ios/Sources/**` の全変更に適用。doc コメントのみの変更でも規約どおり tasks 8.3 を追加 |
| Minor 3 | UI に触れるのに `ui/` と見た目の基準が無い | **採用** | 新規デザインは無いが非回帰の基準が要る。`ui/brief.md` (基準 = 実装前の同画面スクリーンショット) と `ui/verification/` を追加し、tasks 8.2 を照合形式に改訂 |
| Minor 4 | 「4 段解決」Scenario が中間段を検証できない | **採用** | placeholder / ButtonCell title の Scenario を段ごと (a)〜(d) の 4 ケースに改訂、tasks 4.2 を追随 |
| Minor 5 | doc コメント文言が挙動スペックに混入 | **採用** | 「1 対 1 と述べない (SHALL)」を Requirement から外し、輸送範囲だけを残した。doc コメント訂正は tasks 2.2 / 3.1 に残る |

採用 8 / 降格 0 / 未解決 0。
