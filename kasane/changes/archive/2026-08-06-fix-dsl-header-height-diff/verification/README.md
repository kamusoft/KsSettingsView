# 実行時挙動の検証証跡: fix-dsl-header-height-diff

規約: [concepts/cross/conventions/runtime-behavior-verification.md](../../../concepts/cross/conventions/runtime-behavior-verification.md)

platform ごとに節を分ける。連番 PNG は `01`〜`05` が Android、`06`〜`11` が iOS。

# Android / Compose DSL

- 実施日: 2026-08-05
- 実機: Pixel 6a / Android 16
- アプリ: `samples/android` (composite build で本体ソースを直接参照するため、本体の修正有無がそのままビルドへ反映される)
- 対象経路: **Compose DSL** (`KsSettingsView { Section(headerHeight = ...) { ... } }` の再評価)

## 再現手順

`DSLDemoScreen` に検証用の一時ボタン「H高さ」と一時状態 `headerHeight` を追加し、
「静的 Section」の `Section(headerHeight = ...)` へ渡して `-1.0` (自動) ↔ `160.0` (固定) を
トグルさせた。header text は据え置き。

1. アプリ起動 → 「DSL 方式デモ」を開く
2. 「H高さ」をタップ (headerHeight: -1.0 → 160.0)
3. 「静的 Section」ヘッダー領域の高さを観察する
4. もう一度タップして自動高さへ戻ることを観察する

高さと内容の同時変更 (05) では、同じ一時状態から `固定 Cell A` の title も
「固定 Cell A（内容更新）」へ切り替え、1 回の再評価で両方が変わる形にした。

検証用ボタン・一時状態は証跡取得後に削除した (Sample は全 platform で同一画面構成を保つ規約のため)。

## A/B の結果

| 証跡 | ビルド | 操作 | 結果 |
|---|---|---|---|
| `01-before-fix-initial.png` | 修正なし | 画面表示直後 | ヘッダーは自動高さ |
| `02-before-fix-after-tap-unchanged.png` | 修正なし | 「H高さ」タップ後 | **高さが変わらない (症状の再現)** |
| `03-after-fix-after-tap-expanded.png` | 修正あり | 「H高さ」タップ後 | **160dp へ拡大 (解消)** |
| `04-after-fix-toggle-back.png` | 修正あり | もう一度タップ | 自動高さへ復帰 (往復も成立) |
| `05-after-fix-height-and-content.png` | 修正あり | 高さ + Cell 内容を同時に変更 | **ヘッダー 160dp と `固定 Cell A（内容更新）` の両方が反映** |

「修正なし」ビルドは `DSLDiffCalculator.containsHeaderHeightChange` の先頭で `false` を返す
一時改変で作成した (preflight が発火せず、修正前と同じく `compute` が `sameStructure` の
早期 return で空リストを返す状態)。同一の操作・同一のタップ座標で 03 では高さが変わるため、
02 の「変わらない」はタップ取りこぼしではなく症状そのものである。

## 観測できたこと

- Compose DSL の再評価で `headerHeight` だけを変えたとき、修正前は diff が 1 件も出ず表示が
  更新されない。修正後は preflight が `Full` を発行し、Store 経由と同じ表示結果 (固定高さの反映)
  に到達する
- 固定 → 自動 (`160.0` → `-1.0`) の逆方向も反映される (04)
- `Full` の適用は Cell 内容の反映を内包するため、高さと内容を同時に変えても両方が 1 回の更新で
  表示へ届く (05)。`contentUpdates` を併用する必要はない

# iOS / SwiftUI DSL

- 実施日: 2026-08-05
- シミュレータ: iPhone 17 Pro / iOS 26.1
- アプリ: `samples/ios` (ローカル Swift Package を直接参照するため、本体の修正有無がそのままビルドへ反映される)
- 対象経路: **SwiftUI DSL** (`KsSettingsView { Section(headerHeight:) { ... } }` の再評価) と
  **Store `replaceSection`**

## 再現手順

`DSLDemoView` に検証用の一時ボタン「H高さ」「H高さ+内容」と一時状態 `headerHeight` /
`contentUpdated` を追加し、「静的 Section」の `Section(headerHeight:)` へ渡して
`-1` (自動) ↔ `160` (固定) をトグルさせた。header text は据え置き。
「H高さ+内容」は同じ 1 回の再評価で `固定 Cell A` の title も
「固定 Cell A（内容更新）」へ切り替える。

`StoreDemoView` にも一時ボタン「H高さ」を追加し、`PoC Section` の headerHeight だけを
変えた Section を `store.replaceSection(sectionID:new:)` へ渡す形にした。

1. アプリ起動 → 「DSL 方式デモ」を開く
2. 「H高さ」をタップ (headerHeight: -1 → 160)
3. 「静的 Section」ヘッダー領域の高さを観察する
4. もう一度タップして自動高さへ戻ることを観察する
5. 「H高さ+内容」をタップし、高さと Cell の title が同時に変わることを観察する
6. 戻って「Store 方式デモ」を開き、「H高さ」をタップして `PoC Section` のヘッダー高さを観察する

検証用ボタン・一時状態は証跡取得後に削除した (Sample は全 platform で同一画面構成を保つ規約のため)。

## A/B の結果

| 証跡 | ビルド | 経路 | 操作 | 結果 |
|---|---|---|---|---|
| `06-ios-before-fix-initial.png` | 修正なし | DSL | 画面表示直後 | ヘッダーは自動高さ |
| `07-ios-before-fix-after-tap-unchanged.png` | 修正なし | DSL | 「H高さ」タップ後 | **高さが変わらない (症状の再現)** |
| `08-ios-after-fix-after-tap-expanded.png` | 修正あり | DSL | 「H高さ」タップ後 | **160pt へ拡大 (解消)** |
| `09-ios-after-fix-toggle-back.png` | 修正あり | DSL | もう一度タップ | 自動高さへ復帰 (往復も成立) |
| `10-ios-after-fix-height-and-content.png` | 修正あり | DSL | 「H高さ+内容」タップ | **ヘッダー 160pt と `固定 Cell A（内容更新）` の両方が反映** |
| `11-ios-after-fix-store-replacesection.png` | 修正あり | Store | 「H高さ」タップ | **`PoC Section` のヘッダーが 160pt へ拡大** |

「修正なし」ビルドは `DSLDiffCalculator.containsHeaderHeightChange` の先頭で `false` を返す
一時改変で作成した (preflight が発火せず、`compute` が headerHeight を扱わない状態)。
同一の操作・同一のタップ座標で 08 では高さが変わるため、07 の「変わらない」はタップ取りこぼしでは
なく症状そのものである。

## 観測できたこと

- SwiftUI DSL の再評価で `headerHeight` だけを変えたとき、修正前は表示が更新されない。修正後は
  preflight が `.full` を発行し、Store 経由と同じ表示結果 (固定高さの反映) に到達する
- 固定 → 自動 (`160` → `-1`) の逆方向も反映される (09)
- iOS は `.full` の適用だけでは同一 ID Cell を再構成しないため、高さと内容の同時変更では
  `.full` に続けて当該 Cell の `.replaceCell` を発行する。この 2 件の diff 列で両方が
  1 回の更新で表示へ届く (10)。Android の `Full` のみで足りる挙動とは実装形が異なるが、
  観測結果は同じ (core/ADR-0018)
- Store `replaceSection` 経由の headerHeight 変更も表示中 header の高さへ反映される (11)。
  自動テストは `SectionAccessoryRenderingTests` が `SettingsRootStore` に接続した controller を
  window に載せ、`store.replaceSection` / `store.replaceAll` の 2 経路について layout attributes と
  表示中 supplementary の実 frame 高さを観測している。さらに `.full` → `.replaceCell` の順で
  適用したときの表示結果 (header の実高さ + 表示中 Cell の title) も自動テストで観測している
