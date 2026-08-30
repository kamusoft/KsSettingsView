# Tasks: fix-ios-separator-color-not-applied

## 1. 再現の確立

- [x] 1.1 `separatorConfiguration(for:base:)` の返す `UIListSeparatorConfiguration` の `color` が、Theme でセパレータ色を指定しても既定値のままであることをテストレベルで実測する (→ proposal「Why」)
- [x] 1.2 観測手段の確立: **`separatorConfiguration` の返り値の `color` 自体**を観測すること。`currentTheme` の値やモーダル側 (`PickerListViewController`) の反映は代理値であり通過してしまう

## 2. 実装

- [x] 2.1 `separatorConfiguration(for:base:)` で `config.color` に現在の Theme のセパレータ色を設定する (確定済み設計 — 探索議論 2026-08-04)。可視性・インセット規則には触れない
- [x] 2.2 公開 API は変更しない

## 3. テスト

- [x] 3.1 退行テスト: セパレータ色付き Theme で初期化したとき `separatorConfiguration` の返す `color` が Theme のセパレータ色になること
- [x] 3.2 退行テスト: 実行時の Theme 変更 (`applyTheme`) 後に `separatorConfiguration` の返す `color` が新 Theme に追従すること (`reconfigureItems` による separator 再評価の UIKit 依存を固定する)
- [x] 3.3 既存テストが引き続き green であること。とくに `PickerSelectionScreenTests` の separatorColor 系 (モーダル側の既存契約) と、罫線インセット規則を固定しているテスト
- [x] 3.4 追加テストが対象経路を実際に踏んでいることを変異注入で確認する (修正を外すとテストが落ちること。確認後は原状復帰)
- [x] 3.5 iOS テストスイート全体の回帰確認 (実行件数まで確認する)

## 4. シミュレータでの視覚確認

- [x] 4.1 **iPhone 17 / iOS 26.5 シミュレータ** (ユーザー指定) で `samples/ios` の「基本 Cell 7 種デモ」を表示し、セパレータが `SampleTheme` のセパレータ色 (`#E6DAB9` 系) で描画されることをスクリーンショットのピクセル実測で確認する。比較の基準は `ui/references/ios-basic-cells-separator-gray.png` (修正前の実機観測)

---

## 補足

- テスト実行の規約は `kasane/concepts/cross/conventions/test-execution.md` に従う
- S 級のためデルタスペックはなく、verify は非適用。**独立文脈でのレビューは必須**
- 本 change は Android 側 `fix-decoration-theme-not-applied-on-initial-bind` の実機 A/B 確認中に発見された iOS 固有の欠落。機構が異なる (iOS は消費コード自体が無い) ため独立 change とした
- `kasane/concepts/core/styling/list-appearance.md:34` の「iOS は適用しない」記述の追随更新は蒸留 (ksn-distill) で行う。実装フェーズでは concepts に触れない
