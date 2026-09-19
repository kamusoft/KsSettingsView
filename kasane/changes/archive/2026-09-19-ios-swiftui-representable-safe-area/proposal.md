# Proposal: ios-swiftui-representable-safe-area

## Why

SwiftUI ラッパ `KsSettingsView` は `UIViewControllerRepresentable` で UIKit の一覧をホストしており、SwiftUI は Representable を既定でセーフエリアの内側に配置する。そのため `NavigationStack` の中身として全画面で使うと一覧がタブバー・ナビバーの後ろまで回り込まず、タブバー手前で背景が切れて下端に帯が出る。iOS 26 ではさらに、大タイトルがスクロールビュー先頭に置かれる仕組み (WWDC25 session 284) のため、スクロールビューがナビバー下まで伸びていないと大タイトルがインラインに畳まれず流れて消える。

依存側リポジトリ KsAppKMP のリファレンスアプリ (iOS 26.4 Simulator) で再現し、利用側に `.ignoresSafeArea()` を 1 つ足すだけで帯・先頭 / 末尾セルの inset・大タイトルの畳み込みの 3 点が解消した。UIKit 側の `KsSettingsViewController` は inset を OS の automatic 調整に任せており正しく扱えている。阻害要因は SwiftUI 側の配置縮小だけで、ライブラリ自身の iOS Sample も全画面で利用側に回避策を書いている。利用者が毎回書く約束を無くすため、ラッパの既定を全面配置に変える (ios/ADR-0006 proposed)。

## What Changes

- **SwiftUI ラッパの既定を全面配置にする**: `KsSettingsView` は Store 方式・DSL 方式のどちらでも、既定で container のセーフエリアを全辺で無視して親の全面に広がる。bar 分の inset は UIKit の automatic な contentInset 調整に任せる。keyboard 領域は無視しない (EntryCell のキーボード回避は現状どおり SwiftUI が担う)
- **opt-out の Root modifier を 1 つ足す**: 部分埋め込みやシートなど全面化を望まない利用者向けに、セーフエリアを尊重する (従来どおり内側に縮む) 側へ戻す Root modifier を `.style(_:)` / `.theme(_:)` と同じ並びに追加する。on / off の切替だけで、辺の指定は持たない
- **iOS Sample の回避策を取り除く**: `samples/ios` の 7 画面にある利用側の `.ignoresSafeArea(.container, edges: .bottom)` を削除し、既定の全面配置で同じ見た目になることを確認する
- 影響能力: settings-view-ios-swiftui (ラッパの配置既定と Root modifier)、samples-ios (回避策の除去)

## Non-Goals

- **UIKit ホスト (`KsSettingsViewController` 直接利用) の変更** — 既にセーフエリアを正しく扱えており、変更の必要がない (別の能力)
- **MAUI への影響対応** — MAUI は UIKit Controller を直接ホストする経路で SwiftUI ラッパを通らない (別の能力、影響なし)
- **無視する辺を選べる modifier** — KsAppKMP の spike で全辺の無視に問題がなく、辺を選びたい具体的な用法が挙がっていない。必要になった時点で足す (ユーザー判断待ち)
- **Representable 内のスクロールビューを `NavigationStack` のバーへ直接紐付ける経路** — SwiftUI に公開 API が無い (ios/ADR-0006 の Revisit When)
- **concepts (`ios/api/ios-swiftui.md`) と `skills/` / README の追従** — concepts は蒸留 (ksn-distill) の責務、`skills/` と README は docs-refresh スキルの責務で、ユーザーの明示依頼により別途行う

## Impact

- **利用者可視の変更 (破壊的)**: 何も書かない `KsSettingsView` の配置が「セーフエリアの内側」から「親の全面」に変わる。全画面で使う利用者は帯が消え iOS 26 の大タイトルが畳まれるようになる (望ましい変化)。部分埋め込み・シートで使う利用者は見た目が変わり得るため、opt-out modifier で戻す必要がある
- **公開 API**: Root modifier を 1 つ追加。既存の init・modifier は変えない
- **既存の決定との関係**: ios/ADR-0006 (proposed) が本提案の決定を記録する。既存の accepted ADR との衝突は無い
- **リスク**: (1) Representable の内側で掛ける `.ignoresSafeArea(.container)` が利用側で掛けた場合と同じ効き目かは実装の最初に spike で確かめる (設計上は同じはず)。(2) キーボード回避は keyboard 領域を残すことで現状維持だが、実機 / Simulator で EntryCell の入力時に確認する。(3) iOS 18 系での見え方も iOS 26 と併せて証跡を取る (handbook `cross/runtime-behavior-verification.md`)。(4) このリポジトリの Sample にはタブバーが無いため、タブバーを伴う構成の確認は SPM 更新後に KsAppKMP 側 (change `ios-large-title-collapse-ios26`) が 7 画面を再確認する。本 change の証跡はホームインジケータ領域に対する回り込みで取る
- **`ui/`**: 見た目の変更を伴うため作る。目指す見た目は KsAppKMP の spike 画像で確定しており、新規モックは作らず、spike 画像を references に複製して brief.md で「承認済みの正」として扱う
- 長命層: `kasane/concepts/ios/api/ios-swiftui.md` の Root modifier 一覧と「保証すること」が変わる (蒸留で追随)

## 級: M

公開 API の既定挙動の変更と Root modifier の追加で、能力は iOS の SwiftUI ラッパ 1 つに閉じる (Sample の追随は付随)。オーナー確定 2026-09-19。

domain: ios
