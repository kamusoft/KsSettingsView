# Exploration: bridge-dto-doc-audit

## 課題 / 動機

cell-style-dark-appearance-parity (2026-09-06) の実装・レビューで見つかった、bridge DTO 群の doc コメントの既存債務 2 点。

1. bridge DTO 全体 (`android/kssettingsview-bridge/.../KsBridgeCell.kt` / `KsBridgeTheme.kt` / `KsBridgeCellStyle.kt` ほか、および `ios/Sources/KsSettingsViewBridge/` の対応ファイル) の公開 doc コメントに `maui/ADR-0004` 参照が残り、`comment-policy-lint.py --advisory` の要確認に出る。両 platform に一様な債務で、cell-style-dark-appearance-parity では触ったファイル (`KsBridgeCellStyle.*`) でも「2 platform で揃った状態」を優先して据え置いた
2. `ios/Sources/KsSettingsViewBridge/KsBridgeTheme.swift` (と Android の対応 DTO) の「項目は Native の `Theme` の公開項目と 1 対 1 で対応する」という記述。同 change で `KsBridgeCellStyle` の「1 対 1」が実態 (accent / placeholder の CellStyle 段は MAUI から設定されない) と違うと判明したため、Theme DTO 側も同じ検証が要る

規約: `kasane/handbook/cross/comment-policy.md`。関連 concepts: `kasane/concepts/maui/api/native-bridge.md`。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

- 未探索 (簡易起票)
- 輸送 DTO の doc を comment-policy の「公開メンバーの doc コメント」と見なすか (bridge モジュールは MAUI binding からしか使われない internal な公開面)
- Theme DTO の輸送項目と native `Theme` の差分の実態 (要実測: 項目を 1 つずつ突き合わせる)

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: 未判定
