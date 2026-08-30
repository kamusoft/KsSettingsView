# Exploration: fix-ios-full-content-refresh

- 起票日: 2026-08-05
- 起票経緯: [fix-dsl-header-height-diff](../fix-dsl-header-height-diff/exploration.md) の探索で concepts「full 更新のコストモデル」を概念化する際のコード裏取り中に発見。実装未着手・議論未実施の簡易起票
- 2026-08-06: 実挙動検証を実施し**バグ確定** (下記「実挙動検証」節)

## 実挙動検証 (2026-08-06 — バグ確定)

repro テスト `ios/Tests/KsSettingsViewUITests/_ReproFullContentRefreshTests.swift` (使い捨て。実装フェーズで正式テスト化するか破棄) で確認:

- window に実 hosting した `KsSettingsViewController` に対し、同一 Section ID・同一 Cell ID のままタイトルだけ変えた root を `SettingsRootStore.replaceAll` (`.full` 発行) で適用
- 結果: 表示は `["A", "B", "C"]` のまま stale (期待 `["A2", "B", "C2"]`)。**表示中の同一 ID セルの内容変化は `.full` 更新で表示に反映されない**
- 実行環境: iPhone 17 シミュレータ、`xcodebuild test -scheme KsSettingsView-Package -only-testing:KsSettingsViewUITests/_ReproFullContentRefreshTests`

コード側の裏付け (2026-08-06 再確認):

- `applyFullSnapshot` は snapshot を ID のみで構築し、`reloadSections` は header / footer 差分 (+ forceReload) の Section だけ。内容差分セルへの `reconfigureItems` は存在しない
- 部分更新経路 (`replaceCells`) には `snapshot.reconfigureItems(targets)` があり、full 経路だけ内容再適用の出口がない — Android の `android/ADR-0012` 以前と同型の非対称
- concepts「内容更新」節の「full 更新でも取りこぼさない」の根拠記述は Android の `setRootDirect` のみで、iOS 側の機構は元々書かれていなかった (主張が iOS では裏付けを欠いていた)

## 課題 (確定)

concepts の [表示状態同期](../../concepts/core/architecture/display-state-synchronization.md)「内容更新」節は「full 更新でも同一 ID の Cell の内容変化は取りこぼさない」と主張するが、この iOS 側の機構がコードから確認できない:

- `applyFullSnapshot` ([KsSettingsViewController.swift:1135](../../../ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift)) は snapshot を ID のみで構築し、`reloadSections` の対象は header / footer が変わった Section (+ forceReload 指定) だけ。同一 ID で内容が変わった Cell を reconfigure する処理が存在しない
- `KsCellID` は ID のみで等価判定 ([KsCellID.swift](../../../ios/Sources/KsSettingsViewCore/KsCellID.swift) — 内容ハッシュは refactor-display-state-sync で意図的に排除) のため、内容変化は snapshot 差分にも現れない
- Android は full 更新の共通出口 `setRootDirect` が payload 付き内容通知を一括発行して補う (`android/ADR-0012`・実機実証済み) が、iOS に対応物が見当たらない

したがって iOS では `.full` 適用後、**表示中の同一 ID セルの内容が stale のまま残る** (スクロールで画面外→内に入れば最新化される)。上記の実挙動検証で確定済み。

## 想定される影響経路

- Store / View API 経由: `setRoot` 等の full 反映で、同一 ID のまま内容が変わった表示中セルが更新されない可能性
- DSL 経由: 可視性 preflight は `[.full]` のみを返すため、**同一再評価内で「可視性 + 内容」の同時変更**があると内容側が落ちる可能性。fix-dsl-header-height-diff で追加予定の headerHeight preflight も同型 (「固定高さ + 内容」の同時変更)

## 修正方針 (検証済み・議論中)

- ~~テスト / シミュレータで実挙動を検証する~~ → 2026-08-06 実施、バグ確定
- 修正方向: `applyFullSnapshot` に旧・新 visible projection の内容差分セルへの `reconfigureItems` を追加する (Android `setRootDirect` / `android/ADR-0012` の対称形)
- 対象選定は Android と同じ規律に揃える: 旧・新双方の visible projection に存在し値が変わった Cell のみ。新規挿入・削除・hidden には重ねない。`reloadSections` 対象 Section との重複処理も要検討

## 関連

- 出典: fix-dsl-header-height-diff の探索 (2026-08-05) — concepts「full 更新のコストモデル」概念化のコード裏取り
- 関連 concepts: [display-state-synchronization.md](../../concepts/core/architecture/display-state-synchronization.md) 「内容更新」節の主張の検証でもある
- 関連 change: [fix-dsl-header-height-diff](../fix-dsl-header-height-diff/exploration.md) — headerHeight preflight のデルタスペックは「同時変更シナリオ」の扱いを本疑いと整合させる
