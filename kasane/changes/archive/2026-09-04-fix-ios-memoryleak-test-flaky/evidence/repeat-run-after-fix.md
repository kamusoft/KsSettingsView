# 修正後の反復テスト

2026-09-04 に iPhone 17 Pro / iOS 26.5 Simulator で、scheme `KsSettingsView` の対象2クラスを `-test-iterations 10` で連続実行した。`swift test` は使用していない。

| テストクラス | 1反復の件数 | 反復数 | 合計実行件数 | failures |
|---|---:|---:|---:|---:|
| `KsBridgeHostReleaseTests` | 7 | 10 | 70 | 0 |
| `MemoryLeakTests` | 2 | 10 | 20 | 0 |

`xcodebuild` の最終判定は `TEST SUCCEEDED` だった。
