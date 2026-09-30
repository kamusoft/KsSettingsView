# セカンドオピニオン: add-scroll-control (code-002)
**相方**: codex / **label**: so-code-add-scroll-control (turn 2) / **日付**: 2026-09-30 / **対象**: 修正サイクル 1 後の作業ツリー (コミット 38a6c74 からの差分)
---
**判定: CHANGES_REQUESTED**（新規 Major 1件）

前回の指摘は両方とも**解消**しています。今回の修正箇所を静的に確認した結果、Android の非メインスレッド配送に新たな指摘があります。

## 前回の指摘

1. **対象のない命令が先行アニメーションを止める — 解消。** iOS は行き先を求めた後、Android は対象の計画を解決した後に停止する形へ変わっています。両 OS に、無効な命令の後も先行アニメーションが末尾へ到着するテストがあります。

2. **SwiftUI DSL で旧ハンドルの保留命令が残る — 解消。** `ios/Sources/KsSettingsViewSwiftUI/DSLScrollCommandResolver.swift:52` は差し替え時に、引き直し待ちと Host に渡した未実行命令を破棄します。差し替え・切断の両段階を追加テストが確認しています。

## 新規指摘

### 🟠 Major: Android で `post` 待ちの命令が別の Host に届く

**該当箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsScrollController.kt:71`

**問題点**: `strictMode = false` で Host A に接続中、別スレッドから命令を出すとメインスレッドへ `post` されます。その実行前に A を切断して Host B を接続した場合、ラムダは実行時の `receiver` を読み直すため、A に向けて出した命令が B に届きます。A の切断時に待ち行列を消しても、この命令はまだそこに入っていません。接続変更時に旧接続の未実行命令を捨てるという `deviation.md` の扱いから漏れます。

**推奨修正**: `post` 時に接続の世代、または `WeakReference` オブジェクトの同一性を控え、実行時に同じ接続が続いている場合だけ配送してください。A から B への切り替えを `post` の前後に挟む回帰テストを追加してください。

ホスト側の iOS 1184件、Android 3142件、MAUI 624件の成功結果と Swift 6・xcframework の結果を前提にしています。こちらではビルド・テストを再実行せず、ファイルも変更していません。

## 突き合わせ結果

ホスト側: review-002.md (APPROVED、Minor 1)

| 指摘 | 出典 | 採否 | 根拠 |
|---|---|---|---|
| 前回 Major 1・Major 2 の解消 | 双方一致 | 確定 (解消) | 双方とも解消と判定 |
| Android で `post` 待ちの命令が別の Host に届く (strictMode = false の非メインスレッドからの命令) | 相方のみ (Major) | 採用 | 該当箇所と実害シナリオが特定されており、deviation の「接続変更時に未実行の命令を捨てる」扱いから漏れる。strictMode = false はリリースビルドで利用者が選ぶ設定 |
| DSL の受け口の「引き直し前の命令を捨てる」処理を単独で守るテストが無い (Android は経路を通らない、iOS は順序依存) | ホストのみ (Minor) | 確定 | ホスト側判定どおり |

未解決: なし
