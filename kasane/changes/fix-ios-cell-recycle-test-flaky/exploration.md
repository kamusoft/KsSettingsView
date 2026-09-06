# Exploration: fix-ios-cell-recycle-test-flaky

## 課題 / 動機

iOS の Bridge テスト `KsBridgeCustomCellTests` の「リサイクルを挟んだ再表示で内容が壊れない」(`ios/Tests/KsSettingsViewBridgeTests/KsBridgeCustomCellTests.swift:380`) が、GitHub Actions の macOS ランナー上で条件待機の deadline 超過により失敗することがある。

発見の文脈: 0.1.0-beta.2 のリリース作業で develop へ push した検証 CI (run 34035445799、job `ios / verify`、2026-09-06) の attempt 1 がこのテストだけで失敗した。失敗したのは `ios / verify` のみで、`android / verify` / `maui / verify` / `lint` は同じ run で成功している。失敗ジョブだけを再実行 (attempt 2) すると成功した。

失敗時のメッセージ:

```
failed - 条件が deadline 内に成立しなかった: 先頭行が画面外へ出て再利用され、内容が表示中の行から外れる / 経過 3.042 秒 (deadline 3.000 秒) / 実測: 先頭行 CustomCellView(12514055206941490091)
XCTAssertNil failed: "<KsSettingsViewUI.CustomCellView: 0x10c861880; baseClass = UICollectionViewListCell; frame = (0 21.6667; 375 52); hidden = YES; layer = <CALayer: 0x10cb50240>>" - 前提: 先頭行が画面外へ出ていない
```

注目点: 超過幅は 42 ミリ秒とごく僅かである一方、失敗時のセルは `hidden = YES` になっており、視覚的には既に画面外へ出ている。それでも `collectionView.cellForItem(at:)` が nil を返していない。テストは `contentOffset` を直接代入してスクロールを起こしており、行の回収が次のレイアウト周回に委ねられている。

先例との関係: 同じ条件待機の仕組み (`KsSettingsViewTestSupport` の `awaitCondition`) は `2026-09-04-fix-ios-memoryleak-test-flaky` で解放テストへ導入されたもので、今回はその仕組みを使う別テストでの再発にあたる。先例は「同じ待機パターンを持つ同型テストをまとめて安定化する」方針を採っており、本件でも横断の観点が要る可能性がある。

リリースへの影響: 検証 CI と release workflow はどちらも test 段の失敗で先へ進まないため、不安定テストはリリースの再実行を 1 回増やす。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

- **未探索 (簡易起票)** — リリース作業中に発見したためスコープ外として起票した。深掘りは本 exploration.md を出発点に通常の探索で行う
- 真因はどちらか: (a) ランナー高負荷で回収が 3 秒に収まらなかっただけ、(b) `contentOffset` の直接代入だけでは回収を促すレイアウト周回が確実に走らず、待機が成立しない経路がある
- `hidden = YES` のセルが `cellForItem(at:)` から返り続ける状態を、テストの前提 (「画面外へ出た」の定義) としてどう扱うか。回収の完了を待つのが妥当か、可視判定に切り替えるべきか
- deadline を伸ばす対処は先例で「時間の長さで誤失敗を減らすだけ」として却下されている。本件でも同じ判断でよいか
- 同じ `awaitCondition` とスクロール操作を組み合わせる他のテストに同型の弱さがないか (横断確認の要否)

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: 未判定

先例 (`2026-09-04-fix-ios-memoryleak-test-flaky`) はテスト 2 ファイルに閉じる修正で S 級だった。本件も製品コードに触れずテスト側で閉じるなら同格と見込めるが、真因が (b) で回収の促し方そのものに手を入れる場合や横断修正が要る場合は範囲が広がるため、探索で真因を切り分けてから判定する。
