# 修正後の反復実行 (flaky が観測されたテスト)

CI で間欠失敗していた `KsBridgeCustomCellTests` (evidence/ci-flaky-before-fix.md) を、置換後に Simulator で 10 回連続実行した記録。

実行コマンド (10 回反復):

```
cd ios
xcodebuild test -scheme KsSettingsView-Package \
  -destination 'platform=iOS Simulator,name=iPhone 17' \
  -only-testing:KsSettingsViewBridgeTests/KsBridgeCustomCellTests
```

## 結果: 10 回すべて成功

- `** TEST SUCCEEDED **` × 10
- 各回とも `Executed 15 tests, with 0 failures (0 unexpected)`
- 所要時間: 0.250〜0.283 秒 (テスト実行部分。全 10 回の範囲)

## 修正前との対比

| | 修正前 (CI attempt 1・失敗時) | 修正前 (CI attempt 2・成功時) | 修正後 (手元 10 回) |
|---|---|---|---|
| `test_リサイクルを挟んだ再表示で内容が壊れない` | failed (15.437 秒) | passed (1.135 秒) | passed (10/10) |
| スイート全体 (15 件) | 21.631 秒 / 1 failure | — | 0.250〜0.283 秒 / 0 failures |

固定時間待機を条件ベース待機へ置き換えたことで、**収束した時点で即座に抜ける**ようになり、待ち時間そのものが消えている。

## 位置づけ

反復実行は補助確認であり、**決定的な回帰検証は `KsSettingsViewTestSupportTests` の「遅延して成立する述語が deadline 内で成功する」テスト**が担う (tasks 1.5)。反復回数を増やしても「たまたま通った」を排除しきれないため、待機そのものの正しさはヘルパのテストで固定している。
