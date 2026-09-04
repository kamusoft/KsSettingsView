# 解放検出力の確認

## 修正前の観測

- GitHub Actions run 33836940680 の attempt 1 では `ios / verify` が失敗し、同一 commit の attempt 2 では成功した。
- attempt 1 の XCTest 詳細ログは現在取得できない。そのため、当時の失敗が expectation timeout と待機後の weak 参照非 `nil` のどちらだったかは断定しない。

## 検出力確認

2026-09-04 に iPhone 17 Pro / iOS 26.5 Simulator で実施した。最終実装の weak 参照とは別に一時的な強参照を各テストへ残し、対象ケースだけを実行した。一時変更は確認後に除去し、最終差分には含めていない。

| 対象 | 観測結果 | 判定 |
|---|---|---|
| `KsSettingsViewUITests.MemoryLeakTests.test_KsSettingsViewControllerはスコープを抜けるとdeinitされる` | 約 3 秒の deadline 超過。実測 `weakController=non-nil`。1件実行、2 failures | 期待どおり検出 |
| `KsSettingsViewBridgeTests.KsBridgeHostReleaseTests.test_解放後に旧Hostへの参照を保持しない` | 約 3 秒の deadline 超過。実測 `weakHost=non-nil`。1件実行、2 failures | 期待どおり検出 |

各ケースの2 failuresは、条件待機が発火した failure と、直後の既存 `XCTAssertNil` が検出した failure である。条件待機自身が deadline と weak 参照の実測値を報告できることを確認した。
