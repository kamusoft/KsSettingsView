# remigrate-concepts 進捗

状態: `未着手 → 抽出済 → 統合済 → 可読性レビュー済 → 確定`
再開時はこのファイルが SSoT。バッチ内の全 capability が「抽出済」になったら統合へ進む。

## Batch A: Core (→ core-model/ + cells/)

- [x] settings-view-core — 状態: 確定
- [x] cell-types-basic — 状態: 確定
  - 注意: 入力系 Cell (add-cell-types-input) は spec 未整備だが実装済み。コードから拾うこと
- [x] Batch A 統合
- [x] Batch A 初見可読性レビュー (抽出・統合と別文脈で)
- [x] Batch A オーナーレビュー → 確定 (index.md / log.md 更新) — 状態: 確定

## Batch B: iOS (→ platforms/ + architecture/ styling/ への合流材料)

- [x] settings-view-ios-host — 状態: 確定
- [x] settings-view-ios-swiftui — 状態: 確定
- [x] settings-view-ios-style — 状態: 確定
- [x] settings-view-ios-theme-bridge — 状態: 確定
- [x] Batch B 統合
- [x] Batch B 初見可読性レビュー
- [x] Batch B オーナーレビュー → 確定 — 状態: 確定

## Batch C: Android (→ platforms/ + architecture/ styling/ への合流材料)

- [x] settings-view-android-host — 状態: 確定
- [x] settings-view-android-compose — 状態: 確定
- [x] settings-view-android-style — 状態: 確定
- [x] settings-view-android-theme-bridge — 状態: 確定
- [x] Batch C 統合
- [x] Batch C 初見可読性レビュー
- [x] Batch C オーナーレビュー → 確定 — 状態: 確定

## Batch D: 横断統合 (→ architecture/ conventions/)

B と C の両方が確定してから着手 (プラットフォーム横断概念の統合に両方の材料が必要)。

- [x] monorepo-foundation — 状態: 確定
- [x] samples-ios — 状態: 確定
- [x] samples-android — 状態: 確定
- [x] 横断概念の統合 (iOS/Android にまたがる共通原則をここで統合する)
- [x] Batch D 統合
- [x] Batch D 初見可読性レビュー
- [x] Batch D オーナーレビュー → 確定 — 状態: 確定

## Batch E: docs/ 残差スイープと仕上げ

- 注意: Batch D 確定時の全体リンク検査で、Batch A の `core-model/` / `cells/` に concepts root 基準の旧相対リンク24件を検出。全体初見可読性レビュー前に実配置基準へ修正すること
- [x] docs/ 8 文書 (README / overview / architecture / core-model / cells / styling-and-theming / platform-guide-ios / platform-guide-android) を走査し、concepts 未回収の知識を候補化 — 状態: 統合済
- [x] 残差の統合・確定 — 状態: 確定（オーナー承認）
- [x] 前回 log.md の未解消 drift・実装不具合候補・deferred 事項の引き継ぎ一覧を所見として提示
- [x] index.md 最終整備 (再移行中の注記を除去)・log.md 記録 — 状態: 確定
- [x] 全体の初見可読性レビュー (総仕上げ) — 状態: PASS
- [x] 完了報告 (確定 concepts 一覧 / 見送った候補と理由 / drift 所見 / docs/ スタブ化を別変更として提案) — 状態: 確定

## 完了条件

- 全バッチ「確定」+ Batch E 完了
- concepts/index.md に「再移行中」注記が残っていない
- archive への移動はオーナー承認後 (archive = 蒸留済みの完了印)
