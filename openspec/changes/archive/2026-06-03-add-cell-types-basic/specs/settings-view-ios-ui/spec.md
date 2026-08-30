## REMOVED Requirements

### Requirement: PoC Cell の存在

**Reason**: 本変更提案で具象 `LabelCell`（および基本 Cell 6 種）が公開されたため、`PoCLabelCell` は不要となり削除する。

**Migration**: 旧 PoC Cell を利用していたコード（テスト・サンプル）は `LabelCell` に置き換える。`PoCLabelCell(title: ...)` → `LabelCell(title: ...)` への直接置換で動作する。

#### Scenario: 削除確認

- **GIVEN** 本変更提案アーカイブ後の `KsSettingsViewUI` モジュール
- **WHEN** ソースを `PoCLabelCell` および `PoCLabelCellView` で検索する
- **THEN** 該当する型定義は存在せず、登録コードも残っていない
