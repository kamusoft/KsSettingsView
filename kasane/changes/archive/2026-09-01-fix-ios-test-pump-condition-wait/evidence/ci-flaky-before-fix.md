# 修正前の flaky 証跡 (CI)

対象: GitHub Actions `CI` workflow の run `33356260591` (event: pull_request、headSha `49cfce8`、workflow 名 `CI`)。
**同一 commit の再実行 (attempt 1 → attempt 2) で結果が反転**しており、テストの待ち方に依存した間欠失敗であることが記録されている。

- attempt 1: job `ios / verify` が **failure**
- attempt 2: job `ios / verify` が **success** (コードの変更なし)

## 失敗した箇所

attempt 1 の失敗行 (sanitize 済み抜粋):

```
/Users/<USER>/work/KsSettingsView/KsSettingsView/ios/Tests/KsSettingsViewBridgeTests/KsBridgeCustomCellTests.swift:358: error: -[KsSettingsViewBridgeTests.KsBridgeCustomCellTests test_リサイクルを挟んだ再表示で内容が壊れない] : XCTAssertNil failed: "<KsSettingsViewUI.CustomCellView: 0x10c105180; baseClass = UICollectionViewListCell; frame = (0 21.6667; 375 52); hidden = YES; layer = <CALayer: 0x10ab63e70>>" - 前提: 先頭行が画面外へ出ていない
```

`contentOffset` を動かした後に先頭行が画面外へ出て再利用されていることを前提として確認する assert が、**再利用が完了しないうちに評価された**。分類台帳 [triage.md](../triage.md) の A 分類 `KsBridgeCustomCellTests.swift:356,357` (固定時間待機) がこの assert の直前にある。

## 同一テストの実行時間の対比

| | attempt 1 (失敗) | attempt 2 (成功) |
|---|---|---|
| `test_リサイクルを挟んだ再表示で内容が壊れない` | **failed (15.437 seconds)** | passed (1.135 seconds) |

## テストバンドル別の実行件数

| バンドル | attempt 1 | attempt 2 |
|---|---|---|
| `KsSettingsViewBridgeTests` | `Executed 166 tests, with 1 failure` (51.063 秒) | `Executed 166 tests, with 0 failures` (34.564 秒) |
| `KsSettingsViewUITests` | `Executed 642 tests, with 0 failures` (91.515 秒) | `Executed 642 tests, with 0 failures` (36.241 秒) |

UITests バンドルは**同じ 642 件が attempt 1 では約 2.5 倍の時間**を要している。attempt 1 のランナーが混雑しており、固定時間待機が待ち足りなくなる条件が揃っていたことを示す (待機の秒数は実行機の速度に関係なく一定であるため、混雑時ほど収束前に assert へ進む)。

## 全文ログ

判定に要る行のみを上に抜粋した。job ログの全文は evidence に置かない (`~/.kasane/` 配下・作業用スクラッチに一時保管)。再取得は `gh api /repos/kamusoft/KsSettingsView/actions/jobs/<job-id>/logs` で行える (attempt 1 の `ios / verify` job id: `99378912620`、attempt 2: `99380470976`)。
