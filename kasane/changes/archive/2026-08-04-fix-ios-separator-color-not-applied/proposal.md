# Proposal: fix-ios-separator-color-not-applied

## Why

iOS の主リスト画面 (`KsSettingsViewController`) は **`Theme` のセパレータ色を一切消費していない**。セパレータは常に UIKit 既定のグレー (実機実測 `#C6C6C8`) で描画され、Theme でセパレータ色を指定しても反映されない。

機構:

1. 主リストのセパレータは `itemSeparatorHandler` → `separatorConfiguration(for:base:)` で構成される
2. `separatorConfiguration` は `topSeparatorVisibility` / `bottomSeparatorVisibility` / `*SeparatorInsets` のみ設定し、`UIListSeparatorConfiguration.color` を設定していない
3. 初期 bind に限らず、実行時の Theme 変更 (`applyTheme`) でも反映されない — **常に未反映**
4. 一方、ピッカー選択のモーダル画面 (`PickerListViewController`) は `tableView.separatorColor` に反映済みで、**同じ iOS 内で非対称**が存在する

該当箇所 (行番号は 2026-08-04 時点):

- `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:614-651` — `separatorConfiguration(for:base:)` (色未設定の欠落点)
- `KsSettingsViewController.swift:408-410` — `itemSeparatorHandler` (呼び出し元)
- `KsSettingsViewController.swift:297-309` — `applyTheme` (実行時変更もセパレータ色を扱わない)
- `ios/Sources/KsSettingsViewUI/PickerListViewController.swift:96` — モーダル側は反映済み (非対称の証跡)

**再現は確認済み**: サンプル「基本 Cell 7 種デモ」(`samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift:175` が `SampleTheme.maui` 相当の Theme を指定) の実機スクリーンショットで、セパレータが期待の `#E6DAB9` ではなく `#C6C6C8` (実測 RGB 198,198,200) のままであることをピクセル実測で確認。証跡: `ui/references/ios-basic-cells-separator-gray.png`

発見経緯: Android 側の双子 change `fix-decoration-theme-not-applied-on-initial-bind` の実機 A/B 確認中に、ユーザーが iOS サンプルの同一画面で同症状を発見した。Android は「初期 bind 時の適用漏れ」だったが、iOS は「消費コード自体の欠落」であり機構が異なるため独立した変更とする。

なお `kasane/concepts/core/styling/list-appearance.md:34` はこの挙動を「iOS の現行描画は `Theme.separatorColor` を separator へ適用しない」と既知の制限として記述している。本変更によりこの記述は事実でなくなるため、蒸留 (ksn-distill) で concepts を追随更新する。

## What Changes

対象能力: **settings-view-ios-ui** (UIKit Native Host)

- `separatorConfiguration(for:base:)` で `UIListSeparatorConfiguration.color` に現在の Theme のセパレータ色を設定する
- 初期表示と実行時 Theme 変更 (`applyTheme` / Store 経由) の**両方**でセパレータ色が Theme に追従すること。`applyTheme` の `reconfigureItems` で separator 構成が再評価されるかは UIKit の挙動依存のため、両経路を退行テストで固定する
- 可視性・インセット規則 (罫線インセット規則) には触れない
- 退行テストを追加する

## Non-Goals

- 公開 API の変更
- separator の可視性・インセット規則の変更
- `PickerListViewController` (モーダル側) の変更 — 既に反映済み
- Android / MAUI (iOS UIKit Native Host 固有)
- concepts の直接編集 (蒸留の責務)

## Impact

- 破壊的変更なし。挙動変更は「Theme のセパレータ色が主リストの separator に反映される」のみ
- 微細な既定挙動の変化: 既定 Theme では separator が「UIKit 任せの `#C6C6C8`」から「Theme 既定値 `#C8C7CC` (`Theme.swift:235-236` の `defaultSeparatorColor`)」に変わる。差は 2/255 程度で目視不可。文書化された既定値が正になる方向であり許容する (探索議論で合意済み)
- リスク: `reconfigureItems` による separator 再評価が UIKit のバージョン挙動に依存する可能性 → 実行時変更の退行テストで担保する

## 級: S (確定済み)

バグ修正 / 単一能力内 / 公開 API 変更なし / 修正箇所は一点・可逆。ユーザー確定済み (2026-08-04 探索議論)。

デルタスペックは S 級のため作成しない (verify は非適用)。受け入れ基準は tasks.md が持つ。独立文脈でのレビューは S 級でも必須。

domain: ios
