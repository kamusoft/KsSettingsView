# Exploration: ios-appearance-test-observe-rendered-values

## 課題 / 動機

iOS の外観切替テストが、行の背景を「保持している dynamic `UIColor` を検証側で `resolvedColor(with:)` した値」で見ている。この観測は UIKit が実際に背景を貼り直したかを示さず、描画が古いままでも緑になる (検出力が無い)。

maui-appearance-change-tracking (archive 2026-09-17) の調査で見つかり、同 change の tasks 3.1〜3.7 として計画されたが、不具合の前提が切り分けゲートで反証されて change ごと探索へ差し戻されたため未着手のまま残った。他の残スコープ (Cell の `AppThemeBinding`・Header / Footer 背景の Theme 適用・`AppThemeBinding` の回帰資産) は別 change が引き取り済みで、この項目だけ受け皿が無かった (2026-09-17 の蒸留でオーナー裁定により簡易起票)。

### 分かっている事実

- 該当箇所: `ios/Tests/KsSettingsViewUITests/ThemeDarkAppearanceRenderingTests.swift` の `appliedCellBackgroundColor` (`backgroundConfiguration` の色を `resolvedColor(with: cell.traitCollection)` で解決) と、`CellStyleAppearanceTests.swift` の同型の観測
- 同じテストファイルの list 下地は、描画画像の画素で観測済み。流儀を揃える先がある
- 表示中の外観切替の追随そのものは、iOS Native 単体・MauiHost iOS (iOS 26.0 / 18.6) とも実測で成立している (archive 側 tasks.md の 0.1〜0.3 の切り分け結果)。直すのは製品の挙動ではなくテストの観測点
- 元の計画と Scenario は `kasane/changes/archive/2026-09-17-maui-appearance-change-tracking/` の design.md Decision 3、tasks.md グループ 3、`specs/settings-view-ios-ui/spec.md`

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

## 未決の論点

- 未探索 (簡易起票)
- 観測手段: 行の描画画像の背景領域の画素を読むか、行の背景 view の layer に載っている色を読むか
- 範囲: 観測点の置き換え (旧 3.1 / 3.2) だけに留めるか、旧 3.3 (CustomCell の行)・3.4 (明示した固定色)・3.6 (スクロール位置と可視行を保った往復) のテスト追加まで含めるか。旧 3.7 (Header / Footer 背景) は scroll-indicator-visible-not-applied のテストと重ならないかを先に確認する
- 検出力の確認方法: 製品側に「外す修正」が無いので、背景の再適用を一時的に止める改変で新しい観測が赤になることを確かめられるか

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: 未判定 (暫定 S / ios)

暫定の根拠: iOS のテストコードだけを触り、製品コード・公開 API・他 platform に波及しない。
