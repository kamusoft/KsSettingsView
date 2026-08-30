# Tasks: fix-cell-icon-size-parity

## 1. Android (共通行の icon 枠と角丸)

- [x] 1.1 `EffectiveStyle.kt`: data class に解決済み icon size / radius を追加し、`from()` で `effectiveIconSize` / `effectiveIconRadius` を呼んで埋める。両解決関数に有効値判定を足す — size は正の有限値のみ、radius は 0 以上の有限値のみ有効。無効値は未指定として次の段へ (→ Requirement: Cell icon の正方形枠への実効 icon size の反映 / Cell icon の正方形枠に対する角丸)
- [x] 1.2 `CellBaseLayout.kt` `applyCellBaseLayout`: icon 表示時に `iconView` の LayoutParams を解決済み icon size の正方形へ更新し、解決済み radius が正なら `clipToOutline = true` + roundRect の `ViewOutlineProvider`、角丸なしなら `clipToOutline = false` に戻す (再 bind で前回の状態を残さない)。`scaleType` は `FIT_CENTER` を明示する。`buildCellBaseViews` の固定値は初期値として残してよいが SoT は `EffectiveStyle` 側 (→ Requirement: 両方)
- [x] 1.3 `EffectiveStyleResolutionTest.kt`: 無効値 (0 / 負 / NaN / ∞) の size・radius が次の段へ解決されることをテストする (→ Scenario: 無効な icon size は未指定として次の段へ解決する / 無効な radius は未指定として次の段へ解決する)
- [x] 1.4 Robolectric テスト: Theme 指定 / CellStyle 優先 / 未指定既定 / 非正方形 drawable / icon なし Cell の 5 ケースで `iconView` の LayoutParams・`scaleType == FIT_CENTER`・visibility を検証する。未指定ケースは `Theme.DEFAULT_CELL_ICON_SIZE_DP_VALUE` / `DEFAULT_CELL_ICON_RADIUS_DP_VALUE` が iOS の `defaultCellIconSize` / `defaultCellIconRadius` と同じ生値 (24 / 0) であることもアサートする (→ Scenario: Theme.cellIconSize が icon 枠に反映される / CellStyle.iconSize は Theme より優先される / 未指定なら iOS と同じ生値の既定枠になる / 非正方形画像でも枠は正方形のまま / icon のない Cell の配置は変わらない)
- [x] 1.5 Robolectric テスト: `view.theme = ...` で Theme を切り替えた後、表示中の行の `iconView` が新サイズになることを検証する (→ Scenario: Theme 変更で表示中の行の枠が更新される)
- [x] 1.6 Robolectric テスト: radius 指定 / CellStyle 優先 / 未指定 / 非正方形 drawable の 4 ケースで `clipToOutline` と outline (roundRect の半径・矩形が枠に等しい) を検証し、同一 ViewHolder で 正値 → 別の正値 → 0 と再 bind して outline が追随・解除されることを検証する (→ Scenario: Theme.cellIconRadius で枠が角丸に clip される / CellStyle.iconRadius は Theme より優先される / 角丸未指定なら clip しない / 角丸は枠に対してかかり画像の描画矩形には追従しない / 再 bind で radius の変更と解除が反映される)
- [x] 1.7 `CellRowWidthAllocationTest.kt` に追加: 大きめの icon size + 長い title で行幅を狭くしても `iconView` の実寸が解決値のまま title が省略されることを検証する (→ Scenario: 狭幅でも icon 枠は縮まない)
- [x] 1.8 `CellBaseLayout.kt`: 既定の主行配分を title `wrap_content` (singleLine + ellipsize END 維持) / valueText `0dp + weight 1 + gravity END` へ入れ替える。`applyCellBaseLayout` で valueText が GONE のとき title を `0dp + weight 1` に、VISIBLE のとき `wrap_content` に切り替える (再 bind で追随)。EntryCell 固有の weight 付け替えが既定と同型になるなら整理する。title の `paddingEnd` クリアランスは維持 (→ Requirement: 主行の幅配分は title を守り valueText を省略する)
- [x] 1.9 `CellRowWidthAllocationTest.kt`: 既存 2 本 (`valueText はコンテンツ幅で全文表示され title は残り幅で末尾省略される` / `主行幅を超える valueText は末尾省略され行からはみ出さない`) の期待値を新契約へ反転し、valueText 有無の再 bind 切り替え・行内 trailing なしの全幅・ButtonCell 中央揃え・EntryCell の退行なしを検証する (→ Scenario: 長い valueText は省略され title は全文残る / 主行幅を超える title は上限で省略され valueText は残り幅になる / 行内 trailing がない Cell では title が主行の全幅を使う / 同じ行で valueText の有無が切り替わっても配分が追随する / EntryCell では title がコンテンツ幅を維持し入力フィールドが縮む)

## 2. iOS (icon 枠の制約と主行の幅配分)

- [x] 2.1 `KsListCellBase.swift`: `iconWidthConstraint` / `iconHeightConstraint` を `.required` にし、icon の表示/非表示を 1 箇所 (例: `setIconVisible(_:size:)`) に集約して、非表示時に両制約を `isActive = false`、表示時に `true` + constant 更新を行う。`CellBaseLayout.swift` の icon 解決部と `prepareForReuse` をその経路に乗せる。`iconImageView` の hugging / CCR は両軸とも下げる。階層コメント (14 行目付近) の優先度記述を実値に直す (→ Requirement: Cell icon 枠の寸法が画像の intrinsic size に依存しない)
- [x] 2.2 `EffectiveStyle.swift`: `effectiveIconSize` / `effectiveIconRadius` に有効値判定を足す (size は正の有限値、radius は 0 以上の有限値。無効値は次の段へ)。`EffectiveStyleResolutionTests.swift` に無効値ケースを追加する (→ Scenario: 無効な icon size は未指定として次の段へ解決する / 無効な radius は未指定として次の段へ解決する)
- [x] 2.3 (削除: iOS の主行優先度は現行のまま ADR-0026 の契約を満たすため変更しない。テストは 2.7)
- [x] 2.4 XCTest (レイアウト実寸): intrinsic 幅の異なる SF Symbols を並べた複数 Cell で `iconImageView` の実寸が全行で解決済み icon size に等しく、`titleLabel` の leading が一致することを検証する。枠より幅も高さも大きい `UIImage` でも枠が解決値であることを検証する (→ Scenario: intrinsic 幅が異なる SF Symbols でも icon 列幅が揃う / 枠より大きい intrinsic size の画像でも枠は解決済みサイズのまま)
- [x] 2.5 XCTest: CellStyle 優先 / `KsSettingsViewController.applyTheme(_:)` 経由の Theme 変更後に表示中 Cell の枠が更新されること / icon なし Cell で両制約が `isActive == false` かつ hidden で title が通常位置 / icon なし → icon ありの再 bind (および `prepareForReuse` 後の再 bind) で制約が `isActive == true` に戻り枠が解決値になることを検証する (→ Scenario: CellStyle.iconSize は Theme より優先される / Theme 変更で表示中の行の枠が更新される / icon のない Cell では枠の制約が無効化される / icon なし → icon ありの再 bind で枠が戻る)
- [x] 2.6 XCTest: 非正方形 `UIImage` + 正の radius で `iconImageView.layer.cornerRadius` が解決済み radius・`clipsToBounds` が有効・`contentMode` が aspect fit であること、radius 未指定で cornerRadius が 0 であることを検証する (→ Scenario: 角丸は枠に対してかかり画像の描画矩形には追従しない / 角丸未指定なら clip しない)
- [x] 2.7 XCTest (幅配分の契約固定、Android の `CellRowWidthAllocationTest` と対になる): 短い title + 主行幅超えの valueText → title 全文・valueText 省略・icon とアクセサリの幅不変・はみ出しなし / 主行幅超えの title + 短い valueText → title 上限で省略・valueText 残り幅 / valueText なし → title 全幅 / EntryCell の長い title → title コンテンツ幅・フィールド残り幅 / 大きめ icon + 長い title + 狭幅 → icon 枠不変・title 省略 (→ Scenario: 長い valueText は省略され title は全文残る / 主行幅を超える title は上限で省略され valueText は残り幅になる / 行内 trailing がない Cell では title が主行の全幅を使う / EntryCell では title がコンテンツ幅を維持し入力フィールドが縮む / 狭幅でも icon 枠は縮まない)
- [x] 2.8 テスト実行時のコンソールに "Unable to simultaneously satisfy constraints" が出ないことを確認し、実行ログを `ui/verification/ios-test-constraints.log` に保存する (再現可能な証跡として残す)

## 3. 視覚照合 (process L-003 / runtime-behavior-verification)

- [x] 3.1 Android 実機またはエミュレータ: Section 装飾デモ (Modern・既定) で修正前 (`ui/references/android-modern-standard-before.png`) と修正後を撮り、icon 領域の寸法・角丸が `ui/mock/approved.png` (iOS) と揃うことを照合する。証跡を `ui/verification/android-modern-standard-after.png` へ保存する
- [x] 3.2 iOS シミュレータ: 共通フィールドデモ (SF Symbols) で修正前後を撮り、title の開始位置の基準線を注釈した比較画像を作って行間で揃うことを照合する (`ui/verification/ios-common-fields-before.png` / `-after.png` / `-compare-annotated.png`)。あわせて Section 装飾デモが `approved.png` から変化していない (退行なし) ことも撮る (`ios-modern-standard-after.png`)
- [x] 3.3 両 OS: 長い valueText (SSID 相当) を持つ行と Dynamic Type / フォントスケール最大での表示を撮り、title が残って valueText が省略され、icon・アクセサリが潰れないことを両 OS で並べて照合する (`ui/verification/android-overflow-long-value.png` / `ios-overflow-long-value.png`)。Android は修正前も同条件で撮り A/B にする (修正前は title が潰れる)
- [x] 3.4 `ui/brief.md` の「視覚照合の結果」に照合日・結果・合意済み妥協を記入する

## 4. 仕上げ

- [x] 4.1 触ったファイルに `python3 scripts/comment-policy-lint.py <path>` を通し禁止件数 0 を確認する (impl L-001)。Android の outline は `core/ADR-0025` を参照する。iOS の優先度・制約 activate のコメントは ADR を引かず、そのファイルだけで意味が通る理由 (UIStackView の非表示制約との衝突回避、主行の幅配分) を現在形で書く
- [x] 4.2 蒸留への申し送りを確認する: `concepts/core/styling/style-resolution.md` の但し書き削除と ADR-0025・無効値の扱いの追記、`concepts/core/styling/cell-row-layout.md` の「主行の幅配分」を ADR-0026 の契約へ書き直す (実装フェーズでは concepts を触らない)
