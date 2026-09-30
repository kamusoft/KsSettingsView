# Exploration: ios-picker-surface-contract-gaps

## 課題 / 動機

2026-09-26 の ksn-drift ディープ検証 (対象: `kasane/concepts/core/cells/number-picker-selection-surface.md`) で、iOS の選択面が概念ドキュメントの共通契約を満たしていない箇所が 3 件見つかった。Android 側は同じ契約をすべて満たしている。オーナー裁定により、文書をプラットフォーム差として書き直すのではなく、コード側の修正として扱う。

1. **タイトルの fallback がない**: 契約は「`pickerTitle` があればそれ、なければ `title`」。iOS は `EmbeddedPickerToolbar.build(title: cell.pickerTitle, ...)` に `pickerTitle` だけを渡し、nil ならタイトルを出さない (`ios/Sources/KsSettingsViewUI/EmbeddedPickerToolbar.swift` の中央タイトル組み立て)。同じ作りが次の 3 箇所にあり、time / date の選択面の概念ドキュメントも同じ契約を書いている
   - `ios/Sources/KsSettingsViewUI/NumberPickerCellView.swift` (`rebuildToolbar`)
   - `ios/Sources/KsSettingsViewUI/TimePickerCellView.swift`
   - `ios/Sources/KsSettingsViewUI/DatePickerCellView.swift`
2. **ツールバーの accent が 3 段解決になっていない**: 契約は「`NumberPickerCell.accentColor` → `CellStyle.accentColor` → `Theme.cellAccentColor`」。iOS は `accentColor: cell.accentColor` だけを渡していて、nil ならシステム既定の tint になる (`NumberPickerCellView.swift` の `rebuildToolbar`)。なお `kasane/concepts/core/cells/input-cells.md` は「iOS の `accentColor` は埋め込み picker の `tintColor` と入力ツールバーに適用」とだけ書いていて、今の実装と矛盾しない。Time / Date のツールバーが同じ作りかは未確認
3. **初期選択の範囲外処理**: 契約は「`value` が候補に含まれない場合は先頭候補」。iOS は `value` を `min` / `max` に clamp してから候補を探すため、`value > max` で `max` が step の格子上にあると末尾候補が選ばれる (`NumberPickerCellView.swift` の候補組み立て)

発見の文脈: drift の調査ワーカーの報告を、メインが上記 3 箇所のコードで再確認した。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

- 3 件ともコード側の修正として扱う (2026-09-26 オーナー裁定。文書をプラットフォーム差として書き直す案は採らない)

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

## 未決の論点

- 未探索 (簡易起票)
- 2 の accent: 3 段解決を契約とするなら、`input-cells.md` の iOS の記述も揃える必要がある。Time / Date のツールバーも同じ解決にするか
- 1 のタイトル: AiForms 移植時からの挙動かどうか (`kasane/handbook/cross/aiforms-origin-reference.md` に従い、移植元の同機能を確認する)
- 調査ワーカーの推測 (未検証): ドラッグで dismiss した後、再描画を経ずに開き直すと、前回回した位置が残る可能性がある (`pickerView` の選択行と `preSelectedIndex` がリセットされないため)。残るなら「開いた時点で `value` の候補が選択中」の契約に反する
- 範囲外だが近い論点: iOS は候補を配列に展開して `v += safeStep` を overflow 検査なしで回すため、`max` が `Int` 上限の近くだとトラップや巨大配列が起こりうる (契約外の防御挙動。文書は「iOS に上限判定はない」とだけ書いている)

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: 未判定 (暫定 S — iOS の単一能力内のバグ修正で、公開 API は変わらない見込み)
