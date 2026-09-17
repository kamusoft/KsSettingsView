# Exploration: ios-root-accessory-unexpected-accent-background

## 課題 / 動機

(2026-09-18、change `android-accessory-view-late-insert-animation` の実機・Simulator 確認中に見つかったスコープ外の発見より簡易起票。出典: `kasane/changes/android-accessory-view-late-insert-animation/evidence/README.md`)

**症状** (ワーカーの観察。iOS Simulator iPhone 17 / iOS 26.1、および iOS Native Sample):

- MAUI Sample の `AccessoryViewsDemoPage` で、① Root Header View のすぐ下に、XAML に無い鮮やかな青 (アクセント色に見える) の帯が描かれる。証跡: `kasane/changes/android-accessory-view-late-insert-animation/evidence/after-ios-sim-accessory-views-settled-no-scroll.png` ほか
- iOS Native Sample の DSL 方式デモで、root footer の背景がアクセント色 (青) で塗られ、caption の文字が読みにくい。証跡: 同 `evidence/native-sample-ios-dsl.png`
- どちらも上記 change の修正前ビルド (HEAD) でも同じに出る (前者は画素単位で同一)。同 change による退行ではない
- 2 つの現象が同じ原因かは未確認 (「同系に見える」という見立てのみ)

**利用者への影響**: 機能面の問題は無い。利用者が指定していない色が root の header / footer の領域に出るため、見た目が崩れ、文字が読みにくくなる場合がある。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

## 未決の論点

- **未探索 (簡易起票)** — 観察の記録だけで、原因の調査・議論はしていない。コードは読んでいない
- 青い帯が何の領域か (root accessory の背景・supplementary view の既定背景・tint の継承・Sample 側の色指定のいずれか) は未確認
- Android で同じ見え方になるか、実機 (iPhone 11) でも出るかは未確認
- ライブラリ本体の課題か、Sample の色指定の課題かの切り分けが要る
