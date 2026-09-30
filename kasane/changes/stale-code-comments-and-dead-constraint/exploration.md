# Exploration: stale-code-comments-and-dead-constraint

## 課題 / 動機

2026-09-26 の ksn-drift ディープ検証で、概念ドキュメントとコードを照合する途中に、挙動には影響しないコード側の古いコメントと死にコードが 3 件見つかった。drift はコードを修正しないため、ここに積む。

1. **ButtonCell の title 幅についてのコメントが実装と逆**: `ios/Sources/KsSettingsViewUI/ButtonCellView.swift` の `titleAlignment` 反映部のコメントは「`titleLabel` 自体は title 列で残り領域を吸って広がるため、内側で alignment が効く」と書いている。一方で core/ADR-0026 と `ios/Tests/KsSettingsViewUITests/CellRowWidthAllocationTests.swift` (「title はコンテンツ幅を保つ」) は、`valueText` がある行の title はコンテンツ幅になり、余りは value label が吸うと定めている。`ios/Sources/KsSettingsViewUI/CellBaseLayout.swift` の value label のコメントもそちらと一致する。概念ドキュメント `kasane/concepts/core/cells/basic-cells.md` の「alignment が視覚に出るのは `valueText` を持たない行に限る」もテストと一致している
2. **使われていない制約プロパティ**: `ios/Sources/KsSettingsViewUI/KsListCellBase.swift` の `stackHMinHeightConstraint` は宣言だけで、どこからも参照されない。コメントが指す `applyMinHeight(_:)` も存在しない
3. **KsWheelView の KDoc が古い**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsWheelView.kt` の `KsWheelStyle` と `KsWheelView` の KDoc が「将来 DatePicker のホイール版へ再利用する / 展開する前提」と書いている。実際には DatePicker の Spinner 3連ホイール (android/ADR-0009) と TimePicker の時・分ホイール (android/ADR-0018) で既に使われており、同じファイルの別の KDoc は ADR-0009 での利用に触れている。概念ドキュメント `kasane/concepts/core/cells/number-picker-selection-surface.md` は 2026-09-26 に現状へ更新済み

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

## 未決の論点

- 未探索 (簡易起票)
- 2 の制約を削除する前に、同じ意図 (stackH の最小高さ) が別の経路 (`EffectiveStyle` の最低行高・`KsCellViewSupport` の高さ適用) で満たされていることを確認する

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: 未判定 (暫定 S — コメントと未使用コードの整理で、挙動・公開 API は変わらない見込み)
