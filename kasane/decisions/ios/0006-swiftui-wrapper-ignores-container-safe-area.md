---
id: 0006
title: SwiftUI ラッパは既定で container のセーフエリアを無視して全面に広がり、inset は UIKit に任せる
status: accepted
date: 2026-09-19
---

## Context

SwiftUI ラッパ `KsSettingsView` は `UIViewControllerRepresentable` で UIKit の `KsSettingsViewController` をホストする。SwiftUI は Representable を既定でセーフエリアの内側に配置するため、`NavigationStack` の中身として全画面で使うと一覧がタブバー・ナビバーの後ろまで回り込まず、タブバー手前で背景が切れて下端に帯が出る。iOS 26 ではさらに、大タイトルがコンテンツのスクロールビュー先頭に置かれる仕組みになった (WWDC25 session 284) ため、スクロールビューがナビバー下まで伸びていないと大タイトルがインラインに畳まれず流れて消える。

依存側リポジトリ KsAppKMP のリファレンスアプリ (iOS 26.4 Simulator) で 2026-09-19 に再現し、利用側で `.ignoresSafeArea()` を 1 つ足すだけで帯の消失・先頭 / 末尾セルの正しい inset・大タイトルの畳み込みの 3 点が解消した (上端の位置は変化なし)。UIKit 側の `KsSettingsViewController` は `contentInsetAdjustmentBehavior` を上書きせず既定の automatic のままで、セーフエリアを自前で正しく扱えている。阻害要因は SwiftUI 側の配置縮小だけである。

Apple の筋は、スクロールコンテナを実際にバーに覆われる位置まで広げ、inset は `UIScrollView.contentInsetAdjustmentBehavior = .automatic` に任せることである (`UIView.safeAreaInsets` の文書、WWDC25 284)。Representable 内の UIScrollView を SwiftUI の `NavigationStack` のバーに紐付ける公開 API は一次情報の範囲では見つかっておらず、SwiftUI 側の公式レバーは `.ignoresSafeArea()` のみ。

ライブラリ自身の iOS サンプル (`samples/ios`) も全画面で利用側に `.ignoresSafeArea(.container, edges: .bottom)` を書いており、既定のままでは利用者が毎回書く約束になっている。

前提:
- `KsSettingsViewController` はキーボード出現時の inset 調整を持たない。EntryCell 入力時の回避は、Representable がセーフエリア (keyboard 領域) の内側に縮むことで SwiftUI が担っている
- SwiftUI に Representable 内のスクロールビューをバーへ紐付ける公開 API が無い

## Decision

`KsSettingsView` (SwiftUI ラッパ) は既定で `.container` のセーフエリアを全辺で無視し、全面に広がる。bar 分の inset は UIKit の automatic な contentInset 調整に任せる。無視する領域は `.container` に限定し、keyboard 領域は残す (キーボード回避を壊さないため)。部分埋め込みやシートなど全面化を望まない利用者向けに、セーフエリアを尊重する側へ戻す Root modifier (`respectsSafeArea(_:)`、on / off の切替のみで辺は選べない) を設ける。

## Alternatives Considered

- **今のまま + opt-in modifier**: 既存利用者への影響はないが、毎回 1 行足す約束が残り、書き忘れで帯と大タイトル不全が再発する。`List` を置いたときの期待と揃わない。却下
- **今のまま (利用側で `.ignoresSafeArea()` を書く)**: KsAppKMP 側の探索で却下済み。利用者が毎回書く約束になり、ライブラリ自身のサンプルですら全画面で書いている。却下
- **keyboard 領域も含めて全領域を無視する**: Controller がキーボード回避を持たないため EntryCell 入力時に入力欄がキーボードに隠れる。却下

## Consequences

- 正: 利用者は何も書かずに `List` と同じ全面配置になり、iOS 26 の大タイトルの畳み込みが既定で成立する
- 正: キーボード回避の経路は現状 (SwiftUI 任せ) のまま変わらない
- 負: 部分埋め込み・シートで使っている既存利用者は見た目が変わり得る (opt-out で戻せるが、更新時の確認は要る)
- 負: 全面化の判断が UIKit の automatic inset に依存するため、`contentInsetAdjustmentBehavior` を変える将来の変更はこの決定と併せて見直す必要がある
- 負: keyboard 領域を無視しないことは自動テストで固定できず (Simulator でキーボードを出す必要がある)、実行時証跡に頼る

## Revisit When

- SwiftUI 側に Representable 内のスクロールビューを `NavigationStack` のバーへ紐付ける公開 API が出たとき
- `KsSettingsViewController` が自前のキーボード回避を持つようになったとき (keyboard 領域の扱いを再検討)
- `contentInsetAdjustmentBehavior` を automatic 以外にする必要が出たとき

---
出典: kasane/changes/archive/2026-09-19-ios-swiftui-representable-safe-area/exploration.md (検討した選択肢・決定事項) / kasane/changes/archive/2026-09-19-ios-swiftui-representable-safe-area/proposal.md / ../KsAppKMP/kasane/changes/ios-large-title-collapse-ios26/exploration.md (spike と証跡)
現行照合: 2026-09-19 確認。`ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift` (body で Store / DSL 両方式に `.ignoresSafeArea(.container, edges:)` を適用、`respectsSafeArea(_:)` で辺を空にして戻す。ADR-0006 参照コメントあり)、`ios/Tests/KsSettingsViewSwiftUITests/RootSafeAreaLayoutTests.swift` (配置の実測)。判定: 維持
