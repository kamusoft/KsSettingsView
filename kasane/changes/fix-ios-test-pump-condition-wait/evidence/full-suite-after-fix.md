# 修正後の全件 Simulator 実行

置換完了後、およびレビュー指摘の修正後の全件実行の記録。実行日: 2026-09-01。
下表の「実行件数 / 失敗 / 所要」はレビュー指摘の修正を反映した後の最終実行の値である。

```
cd ios
xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17'
```

結果: `** TEST SUCCEEDED **`

## テストバンドル別の実行件数

| バンドル | 実行件数 | 失敗 | 所要 | 修正前 (CI 実測) |
|---|---:|---:|---:|---:|
| `KsSettingsViewBridgeTests` | 166 | 0 | 7.034 秒 | 166 / 0 (34.564 秒) |
| `KsSettingsViewCoreTests` | 88 | 0 | 0.132 秒 | 88 / 0 (0.819 秒) |
| `KsSettingsViewSwiftUITests` | 94 | 0 | 0.518 秒 | 94 / 0 (4.173 秒) |
| `KsSettingsViewTestSupportTests` | 7 | 0 | 0.876 秒 | (本 change で新設) |
| `KsSettingsViewUITests` | 642 | 0 | 8.601 秒 | 642 / 0 (36.241 秒) |
| **合計** | **997** | **0** | | 990 (新設分を除く) |

修正前の件数は evidence/ci-flaky-before-fix.md と同じ CI run (attempt 2) の実測値。**テストの件数はすべて維持**され、新設した待機ヘルパのテスト 7 件が加わっている。

## 実行時間の変化

固定時間待機を条件ベース待機へ置き換えたことで、条件が成立した時点で即座に抜けるようになり、待ち時間そのものが縮んでいる (修正前の値は CI ランナー、修正後は手元の Simulator であり実行環境が異なるため厳密な比較ではないが、`KsSettingsViewUITests` の 36.241 秒 → 8.601 秒のような差は待機の削減による)。

置換前は呼び出し 206 箇所が最低 0.05 秒 (明示指定は最大 1.0 秒) を必ず消費していた。

## 位置づけ

- **完了判定に使うのはこの絞り込みなしの全件実行**である (`kasane/handbook/cross/test-execution.md`)。`swift test` は UI 系テストが `#if canImport(UIKit)` で除外され「1 件も実行されない」状態が成功として返るため使わない
- flaky が観測されたテストの反復実行は evidence/repeat-run-after-fix.md
- 初期反映の述語の検出力は evidence/initial-render-predicate-detection.md

## develop マージ後の再実行 (2026-09-01)

実装完了後、develop (7 コミット先行。うち `fix(ios): 行タップ通知と押下フィードバックを Swift 6 の actor 分離に適合させる` が `ios/` に影響) をマージしたうえで再実行した。

| バンドル | 実行件数 | 失敗 | 本 change 単独時 |
|---|---:|---:|---:|
| `KsSettingsViewBridgeTests` | 166 | 0 | 166 |
| `KsSettingsViewCoreTests` | 88 | 0 | 88 |
| `KsSettingsViewSwiftUITests` | 94 | 0 | 94 |
| `KsSettingsViewTestSupportTests` | 7 | 0 | 7 |
| `KsSettingsViewUITests` | **645** | 0 | 642 |
| **合計** | **1000** | **0** | 997 |

`** TEST SUCCEEDED **`。UITests の +3 件は develop 側が追加した `KsCellViewSupportTests` / `KsSettingsViewControllerTests` によるもので、マージによる欠落や競合は無い。

ログ中の `error:` 18 件はいずれも `CHHapticPattern` のセレクタ名 (`patternForKey:error:`) を含む情報ログで、Simulator に触覚デバイスが無いことによる。テストの失敗ではない。

なお develop 由来の新規テストは独自の private 待機ヘルパを持つ (待機規約には準拠。共有ターゲットへ寄せる件は change `share-wait-helpers-in-ios-cellviewsupport-tests` として起票済み)。
