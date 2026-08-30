# セカンドオピニオン: align-sample-parity (002 回目)
**相方**: codex / **日付**: 2026-08-01 / **対象**: 実装 diff (ベース ef821e9 の未コミット変更一式、code-review モード)
**対応するホスト側レビュー**: review-001.md
**注**: 001 は提案段階の spec-review モードで使用済みのため、本ファイルは 002 を採番した
---
# レビュー結果: align-sample-parity

**判定**: CHANGES_REQUESTED  
**指摘件数**: Critical 0 / Major 2 / Minor 1 / Suggestion 0  
**結果ファイル**: なし（依頼どおり応答テキストのみ）

## 指摘事項

### [🟠 Major] `accentColor` の実値がプラットフォーム間で一致していない

**該当箇所**: [UnifyCellCommonFieldsDemoView.swift:71](samples/ios/KsSettingsViewSample/UnifyCellCommonFieldsDemoView.swift:71)、[UnifyCellCommonFieldsDemoScreen.kt:76](samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/UnifyCellCommonFieldsDemoScreen.kt:76)

**問題点**: 共通フィールド統合デモでは、iOS が `systemOrange`、`systemPurple`、`systemTeal`、`systemPink`、`systemGreen`、`systemBlue`、Android がそれぞれ別の固定 RGB 値を渡しています。これらは実際の色値が異なり、iOS 側は appearance によって変化する動的色でもあります。

この画面は `accentColor` 自体の表示確認デモであり、明示的に渡すパラメータの差は、[sample-parity.md:23](kasane/concepts/cross/conventions/sample-parity.md:23) の「各 Cell に渡すパラメータを一致させる」に反します。`deviation.md` にも記録されていません。

**推奨修正**: 両プラットフォームで同じ RGBA の共有パレットを使ってください。プラットフォーム固有の semantic color を意図的に残す場合は、オーナー合意のうえ `deviation.md` に理由と対象色を記録してください。

### [🟠 Major] 必須の最終パリティ検証が未完了

**該当箇所**: [tasks.md:29](kasane/changes/align-sample-parity/tasks.md:29)

**問題点**: 全画面の最終対照確認と deviation の網羅確認（3.1、3.2）が未チェックです。提示されたビルド成功はコンパイル成立を保証しますが、色・パラメータ・視覚差や deviation の漏れまでは検証しません。実際、上記の未記録差分が残っています。

**推奨修正**: 明示パラメータを含む全6対応画面を再照合し、未一致を修正または deviation に記録した後、3.1・3.2を完了させてください。

### [🟡 Minor] StateFlow の収集がライフサイクル非連携

**該当箇所**: [StoreDemoScreen.kt:61](samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/StoreDemoScreen.kt:61)

**問題点**: `store.state.collectAsState()` は画面がバックグラウンドになっても収集を継続します。解決された Compose レビュー規律では `collectAsStateWithLifecycle()` が必須です。この Store はローカルかつ軽量なので実害は限定的ですが、新規ファイルとして残すべきパターンではありません。

**推奨修正**:

```kotlin
val state by store.state.collectAsStateWithLifecycle()
```

必要に応じて互換バージョンの `lifecycle-runtime-compose` 依存を明示してください。

## サマリー

メニュー／タイトルの一元化、入力 Cell の文言・初期値・主要パラメータ、Theme 共有、および記録済み deviation は概ね仕様どおりです。一方、明示色パラメータの未記録差分と最終検証の未完了が、変更目的である厳密なパリティ成立を妨げています。

ビルド・テストは制約に従って再実行せず、提示された成功結果を前提としました。


## 突き合わせ結果

**ホスト側レビュー**: review-001.md (2026-08-01 / 判定 CHANGES_REQUESTED / Critical 0・Major 0・Minor 5・Suggestion 2)

| 相方の指摘 | ホスト側の対応する指摘 | 採否 | 根拠 |
|---|---|---|---|
| Major: `accentColor` の実値が platform 間で不一致 | **なし (見逃し)** | **採用** | 相方のみ + 根拠強。該当箇所と規約条文 (sample-parity.md「各 Cell に渡すパラメータを一致させる」) を特定しており、実在を確認済み (iOS `UIColor.systemOrange` 等 ⇔ Android `Color(0xFFFF9800)` 等)。ホスト側の機械照合は「表示文言」に限定されており色パラメータは対象外だった。2026-08-01 オーナー判断で今回スコープに含め、両 platform を共通固定パレットへ統一 (deviation.md に記録) |
| Major: 最終パリティ検証 (tasks 3.1 / 3.2) が未完了 | Minor: tasks.md グループ 3 が未了 | **確定** | 双方一致。ただし編成上これから消化する残工程であり、実装のやり直しを要する指摘ではない。重要度は高い方 (Major) の扱いとし、verify 前に必ず閉じる |
| Minor: `StoreDemoScreen.kt:61` の `collectAsState` がライフサイクル非連携 | Suggestion: 同箇所を `collectAsStateWithLifecycle()` へ | **確定 (採用)** | 双方一致。重要度は高い方 (Minor) を採る。HEAD 時点の `MainActivity.kt:349` から移設された既存コードで本変更が持ち込んだものではないが、Sample はライブラリ利用例として読まれるため推奨形に是正する |

**採用 3 / 降格 0 / 未解決 0**

相方のみが検出した `accentColor` の不一致は、ホスト側レビューが「表示文言」の軸で機械照合したために構造的に拾えなかった角度であり、クロスモデル並走の収穫として記録する。
