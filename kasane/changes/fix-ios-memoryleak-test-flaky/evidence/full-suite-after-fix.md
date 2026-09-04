# 修正後の iOS 全件テスト

2026-09-04 に iPhone 17 Pro / iOS 26.5 Simulator で、scheme `KsSettingsView` を絞り込みなしで実行した。`swift test` は使用していない。

| テストバンドル | 実行件数 | failures |
|---|---:|---:|
| `KsSettingsViewBridgeTests.xctest` | 166 | 0 |
| `KsSettingsViewCoreTests.xctest` | 88 | 0 |
| `KsSettingsViewSwiftUITests.xctest` | 94 | 0 |
| `KsSettingsViewTestSupportTests.xctest` | 7 | 0 |
| `KsSettingsViewUITests.xctest` | 645 | 0 |
| **合計** | **1000** | **0** |

`xcodebuild` の最終判定は `TEST SUCCEEDED` だった。
