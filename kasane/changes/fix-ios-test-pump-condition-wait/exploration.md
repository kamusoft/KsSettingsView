# Exploration: fix-ios-test-pump-condition-wait

add-verification-ci の実装中 (2026-08-31)、CI 上で flaky が実際に顕在化したため簡易起票。

## 課題 / 動機

iOS テストの待機ヘルパ `KsBridgeTestHost.pump(_:seconds:)` が固定時間待機であり、`kasane/handbook/cross/test-execution.md` の待機規約 (実時間 deadline で区切る・収束条件で判定する・超過時は実測値付きで fail) を満たしていない。

```swift
// ios/Tests/KsSettingsViewBridgeTests/KsBridgeTestHost.swift:50
static func pump(_ attachment: Attachment, seconds: TimeInterval = 0.05) {
    let view = attachment.collectionView
    view.setNeedsLayout()
    view.layoutIfNeeded()
    RunLoop.current.run(until: Date().addingTimeInterval(seconds))
    view.setNeedsLayout()
    view.layoutIfNeeded()
}
```

指定秒数を待つだけで収束条件を見ないため、実行機が混んでいると収束前に assert へ進む。

### 実際に観測された flaky (2026-08-31)

`ios/Tests/KsSettingsViewBridgeTests/KsBridgeCustomCellTests.swift:322` `test_リサイクルを挟んだ再表示で内容が壊れない`

```swift
collectionView.contentOffset = CGPoint(x: 0, y: maxOffset)
KsBridgeTestHost.pump(attachment, seconds: 0.3)
KsBridgeTestHost.pump(attachment, seconds: 0.3)
XCTAssertNil(collectionView.cellForItem(at: IndexPath(item: 0, section: 0)), "前提: 先頭行が画面外へ出ていない")
```

固定 0.3 秒 × 2 の後に「先頭行が画面外へ出た」ことを assert する。GitHub Actions の `macos-26` ランナー上で、**同一 commit に対する 1 回目の実行が失敗し、2 回目は成功した** (run 33356260591、iOS job)。失敗時も 642 件のテスト自体は `0 failures` でスイート集計を通過しており、xcodebuild の最終判定だけが failing を報告している。

## 現状確認 (2026-08-31 時点、実測)

- `pump(` の使用は `ios/Tests/` 配下で 221 箇所・30 ファイル。`KsSettingsViewUITests` / `KsSettingsViewSwiftUITests` / `KsSettingsViewBridgeTests` の 3 ターゲットすべてに広がっている
- **221 箇所すべてが問題とは限らない** (未確認)。`pump` には「レイアウトを走らせる」用途と「非同期の収束を待つ」用途が混在しており、後者だけが条件ベースへの作り替えを要すると見込まれる。仕分けが探索の主題になる

## 関連

- [[fix-customcell-test-pump-condition-wait]] — Android の同型問題 (`android/ks-settingsview-ui` のテスト 2 箇所で、反復回数区切りのループ内 `Thread.yield()`)。課題の型と根拠となる規約は同一で、プラットフォームと規模だけが異なる。実装時は待機規約の解釈を揃える
- 発見の文脈: `kasane/changes/add-verification-ci` (検証 CI の構築)。CI を必須 status check にしたため、この flaky は今後 PR のマージを不定期にブロックする

## 未決の論点

- `pump` の 221 箇所のうち、収束待ちとして使われている箇所の特定と仕分け
- 条件ベースの待機ヘルパをどの形で提供するか (既存 `pump` を条件付きオーバーロードで置き換えるか、別ヘルパを足して段階移行するか)
- Android 側と共通の待機規約をどこまで揃えるか (handbook の記述を platform 共通の指針として補強するか)

## 変更級の推奨: 探索で確定 (仕分けの結果次第で S〜M)

プロダクトコード・公開 API の変更は伴わない見込み。ただし対象箇所が多く、待機ヘルパの設計判断を含むため、Android 版 (S 見込み) より重くなる可能性がある。
