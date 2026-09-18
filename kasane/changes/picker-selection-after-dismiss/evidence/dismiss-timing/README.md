# dismiss 実時間の計測 (提案段階の切り分け)

目的: 「iOS の選択面の dismiss が UIKit 既定より遅く見える」観測 (exploration.md) がライブラリ側の原因かを、提案を確定する前に切り分ける。

## 計測方法

- ライブラリの確定経路 (`ios/Sources/KsSettingsViewUI/PickerListViewController.swift` の単一選択 `didSelectRowAt`) に一時的な計測ログを入れる (commit しない。計測後に `git checkout` で戻す):
  - `t0` = 候補タップ直後 (確定 callback `onSingleDone` を呼ぶ直前)
  - `t1` = 確定 callback から戻った時点
  - `t2` = `dismiss(animated: true, completion:)` の completion が呼ばれた時点
  - `callbackMs = t1 - t0` (確定 callback の処理時間。MAUI 経由では bridge → facade の書き戻し → Store 反映まで含む)、`dismissMs = t2 - t1` (dismiss アニメーションの実時間)
  - 時計は `CACurrentMediaTime()`、出力は `NSLog` (Simulator は `xcrun simctl spawn <udid> log show --predicate 'eventMessage CONTAINS "KSN_TIMING"'` で回収)
- 操作: 「入力 Cell 5 種デモ」の PickerCell 行をタップ → 候補を 1 つタップ。同じ操作を連続で繰り返す
- 比較軸: (1) Native 直接 (`samples/ios`) vs MAUI 経由 (`samples/maui`、同じ native をリンク)、(2) 候補数 (都道府県 47 件 / テーマ 3 件)

## 環境

- 2026-09-18、iOS Simulator iPhone 17 Pro (iOS 26.0)、Debug 構成、macOS ホスト
- 実機 (iPhone 11 / iOS 18 系) での計測は本 README の末尾「実機」節を参照 (未実施なら未実施と記す)

## 結果: Native 直接 (samples/ios)

| 試行 | 候補数 | callbackMs | dismissMs |
|---|---|---|---|
| 1 | 47 | 0 | 546 |
| 2 | 47 | 0 | 546 |
| 3 | 47 | 0 | 544 |
| 4 | 47 | 0 | 543 |
| 5 | 3 | 0 | 583 |
| 6 | 3 | 0 | 546 |

- dismiss は候補数に依らず約 545ms (中央値 546ms、最大 583ms)。ページシートの標準 dismiss の尺に収まる
- 確定 callback の処理時間は 0ms (Native 直接では Binding の setter だけ)

## 結果: MAUI 経由 (samples/maui、同じ Simulator・同じ native)

binding / facade / Sample の iOS 出力を捨てて再ビルドし、実行ファイルに計測ログの文字列が含まれることを `strings` で確認してから計測した (最初のビルドは古い native をリンクしていた。handbook `cross/local-development-setup.md` の注意どおり)。

| 試行 | 候補数 | callbackMs | dismissMs |
|---|---|---|---|
| 1 | 47 | 3 | 533 |
| 2 | 47 | 0 | 530 |
| 3 | 47 | 0 | 536 |
| 4 | 47 | 1 | 532 |
| 5 | 47 | 0 | 540 |
| 6 | 3 | 7 | 529 |

- dismiss の中央値 533ms (最大 540ms)。Native 直接 (中央値 546ms) との差は 13ms で、判定基準 (100ms 以内) に収まる
- 確定 callback の処理 (bridge → facade の書き戻し → Store `replaceCell` → セクション reload) は最大 7ms。dismiss アニメーションを詰まらせる大きさではない

## 判定

- **ライブラリ側に dismiss を遅らせる要因は無い。** iOS のページシート dismiss は UIKit 既定で約 530〜585ms かかる (exploration.md が想定した 0.3〜0.5 秒より長い)。利用側の固定待ち 500ms で足りず 625ms で通った観測は、この尺そのもので説明できる
- 探索で立てた仮説 (値の書き戻しによるセクション reload がメインスレッドを詰まらせる) は棄却 (callbackMs ≤ 7ms)
- 「500ms 版で体感ラグが残る」観測の残りは、本ライブラリの外 (利用側のダイアログ実装・ホストのメインスレッド) の領分で、本 change では追わない

## 実機

未実施 (2026-09-18 時点)。dismiss の尺は UIKit のアニメーションで決まり端末クラスへの依存は小さい見込みだが、実装フェーズで新しい閉じ切り callback を使い iPhone 11 実機で 1 系列を取り、この README に追記する (tasks.md 6.x)。
