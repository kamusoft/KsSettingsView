# セカンドオピニオン: add-scroll-control (code-001)
**相方**: codex / **label**: so-code-add-scroll-control / **日付**: 2026-09-30 / **対象**: コミット 38a6c74 からの作業ツリーの差分 (ios/Sources・ios/Tests・android/kssettingsview・android/kssettingsview-bridge・maui/・samples/)
---
**判定: CHANGES_REQUESTED**（Major 2件）

`ksn-review` の観点で変更アーティファクト、`deviation.md`、実装差分を静的に確認しました。以下は記録済みの乖離や既知の未了事項には含まれません。

### 🟠 Major 1: 対象のない命令が先行アニメーションを止める

**該当箇所**: iOS の命令実行 `ios/Sources/KsSettingsViewUI/KsSettingsViewController+ScrollControl.swift:184`、Android の命令実行 `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsViewScrollControl.kt:151`

**問題点**: 両実装とも、対象を解決する前に先行スクロールを停止します。例えば `scrollToEnd(animated: true)` の直後に存在しない Cell ID への命令を出すと、後者は対象がないにもかかわらず前者のアニメーションを止めます。対象がない命令は何もしない、というデルタスペックの契約に反します。

**推奨修正**: 対象と行き先を解決してから先行アニメーションを止めてください。アニメーション中に無効な命令が続くケースを回帰テストに加えてください。

### 🟠 Major 2: SwiftUI DSL で切断前の保留命令が新しい接続状態へ流れる

**該当箇所**: `ios/Sources/KsSettingsViewSwiftUI/DSLScrollCommandResolver.swift:52`

**問題点**: `connect(_:)` は旧ハンドルを切り離しますが、`pending` を消しません。旧ハンドルの命令を受けた後、`resolvePending()` より先に修飾子のハンドルが差し替わる、または外れると、その命令が現在の Host に配送され得ます。接続を外した命令が後から画面を動かす経路です。

**推奨修正**: 接続先の変更時に旧接続の保留命令を破棄し、解決待ちの間にハンドルを差し替えるケースをテストしてください。

照合した規約は `comment-policy`、`test-execution`、`runtime-behavior-verification`、`sample-parity`、`diagnostic-message-language`、iOS の Swift 6 確認、MAUI の IntegrationHost 確認です。ビルド・テストは依頼どおり再実行せず、提示された成功結果を前提にしました。ファイルへの書き込みはしていません。

## 突き合わせ結果

ホスト側: review-001.md (CHANGES_REQUESTED、Minor 1 / Suggestion 3)

| 指摘 | 出典 | 採否 | 根拠 |
|---|---|---|---|
| 対象のない命令が先行アニメーションを止める (iOS / Android) | 相方のみ (Major 1) | 採用 | 該当箇所と実害シナリオが特定されており、spec「存在しない ID への命令は何もしない」に反する |
| SwiftUI DSL で切断前の保留命令が新しい接続状態へ流れる | 相方のみ (Major 2) | 採用 | 該当箇所が特定され、deviation の「差し替えで未実行の命令を捨てる」扱い (Android は実装済み) と食い違う |
| iOS のテストに Section の center / end・高い対象の start・Section 見出しの控えと戻し・画面外の間の命令の 4 点が無い | ホストのみ (Minor) | 確定 | ホスト側判定どおり |
| Android `KsScrollController.receiver` の非メインスレッドからの読み取りに可視性の保証が無い | ホストのみ (Suggestion) | 採用 | 注釈 1 つで閉じる |
| Android `onLayout` ごとの既定 id Host の走査、Bridge `onPreDraw` の毎フレームの一時リスト | ホストのみ (Suggestion) | 降格 | 性能の好みの域で実害の計測が無い。修正サイクルは回さない |

未解決: なし
