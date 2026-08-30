# Candidate: settings-view-ios-theme-bridge

## 概念候補

### iOS Native スタイル階層 (提案カテゴリ: styling/)

iOS のスタイル境界は、画面全体の既定、単一 Cell の部分上書き、プラットフォーム既定を階層的に合成し、描画側が欠損値を扱わずに済む実効値を提供する。色・フォント・寸法・画像は iOS の Native 型で表し、Core の構造モデルと Native 表現の間に独自の論理色・論理フォント変換を置かない。

画面全体の既定は Cell 間で共有する視覚方針を表し、Cell 個別スタイルの未指定値だけを補完する。Cell 固有の明示値を持つ項目は、個別スタイルよりさらに優先できる。どの段にも指定がない項目は、iOS の慣習に沿う既定値へ必ず解決される。画面背景と Cell 背景は異なる表示領域の値であり、相互の代用にはしない。

この境界が担うのはスタイル値の所有と優先順位の解決である。設定ツリーの構造、宣言ツリーの差分算出、Native リストへの構造変更適用は担わない。

出典: `ios/Sources/KsSettingsViewUI/Theme.swift`、`ios/Sources/KsSettingsViewUI/CellStyle.swift`、`ios/Sources/KsSettingsViewUI/EffectiveStyle.swift`、`ios/Tests/KsSettingsViewUITests/EffectiveStyleTests.swift`、`ios/Tests/KsSettingsViewUITests/EffectiveStyleResolutionTests.swift`、`ios/Tests/KsSettingsViewUITests/ThemeRenameTests.swift`、`openspec/specs/settings-view-ios-theme-bridge/spec.md`「Theme / CellStyle の UIKit 変換」、`docs/styling-and-theming.md` §1〜§4、`docs/architecture.md` §3、`kasane/decisions/0009-ui-layer-native-styling.md`

### iOS Theme 更新境界 (提案カテゴリ: platforms/)

Theme は設定ツリーの構造ではなく、画面側が所有する独立した表示状態である。宣言方式と外部 Store 方式のどちらから指定しても、UI 層の Theme 状態へ収束し、現在の画面背景と Cell 描画を再評価する。

Theme の変更は Section / Cell の識別子、集合、順序を変えず、構造変更値にも含めない。同値の再適用は通知を発生させない。これにより Theme の切替は構造同期と独立し、表示要素の同一性を維持したまま反映される。

この境界が担うのは Theme の保持・通知・再描画への接続である。個々のスタイル値の優先順位は Native スタイル階層、構造変更は Store とホスト、宣言ツリーの比較は SwiftUI ラッパが担う。

出典: `ios/Sources/KsSettingsViewUI/SettingsRootStore.swift`、`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift`、`ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift`、`ios/Tests/KsSettingsViewUITests/SettingsRootStoreTests.swift`、`ios/Tests/KsSettingsViewUITests/ApplyDiffTests.swift`、`ios/Tests/KsSettingsViewSwiftUITests/DSLDiffCalculatorTests.swift`、`docs/styling-and-theming.md` §12、`docs/architecture.md` §3・§5、`kasane/decisions/0009-ui-layer-native-styling.md`

### iOS Cell の視覚状態優先順位 (提案カテゴリ: styling/)

iOS の Cell 描画は、通常・押下または選択・無効の状態をスタイル値の上に重ねる。操作可能な Cell が押下または選択されている間だけ選択色を使い、状態解除時はその Cell の実効背景色へ戻す。無効状態は選択状態より優先し、操作と Cell 固有アクションを抑止する。

無効状態の表現は Cell 全体の透明度を下げず、テキストの意味色を無効時の色へ置換し、内包する Native コントロールを無効化する。これにより、背景・画像・レイアウトまで一律に薄めることなく、操作不能であることを一貫して示す。

Cell 固有の基準色を持つ種類では、その値を個別スタイル、Theme、プラットフォーム既定より先に解決し、無効状態では最終的に無効時の色を優先する。

出典: `ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift`、`ios/Sources/KsSettingsViewUI/CellBaseLayout.swift`、`ios/Sources/KsSettingsViewUI/ButtonCellView.swift`、`ios/Sources/KsSettingsViewUI/EffectiveStyle.swift`、`ios/Tests/KsSettingsViewUITests/BasicCellsTests.swift`、`ios/Tests/KsSettingsViewUITests/UnifyCellCommonFieldsTests.swift`、`ios/Tests/KsSettingsViewUITests/EffectiveStyleResolutionTests.swift`、`openspec/specs/settings-view-ios-theme-bridge/spec.md`「タッチフィードバック」「isEnabled 描画の反映」「ButtonCell の baseColor 解決順序」、`docs/styling-and-theming.md` §9〜§11

## ADR 候補

- 新規候補なし。スタイル型を UI 層へ置いて Native 型で表現する判断、実効スタイルの優先順位、Theme を構造モデルと構造 Diff から分離する判断は `kasane/decisions/0009-ui-layer-native-styling.md` に accepted として記録済み。共通 Cell 描画への集約も `kasane/decisions/0011-composed-shared-cell-row-layout.md` に記録済みである。

## drift 所見

- Purpose は Core が持つ論理スタイル値を UIKit 値へ変換する「テーマ変換ブリッジ層」を本 capability の責務としているが、現行 Core に Theme・CellStyle・KsImage・KsColor は存在せず、UI 層の Theme / CellStyle が UIKit 型を直接保持する。変換ユーティリティも存在せず、現行の責務は実効スタイル合成と視覚状態の適用である。Requirements 内の「変換ユーティリティは存在してはならない」という記述および現行 docs とも Purpose が自己矛盾している (`openspec/specs/settings-view-ios-theme-bridge/spec.md` Purpose・「Theme / CellStyle の UIKit 変換」・「KsImage.uiImage 派生の解決」 / `ios/Sources/KsSettingsViewUI/Theme.swift` / `ios/Sources/KsSettingsViewUI/CellStyle.swift` / `ios/Sources/KsSettingsViewUI/KsImage.swift` / `docs/architecture.md` §1・§3)。
- Requirement と Scenario は削除済みの Theme 名 `viewBackgroundColor`・`titleColor`・`titleFont`・`descriptionColor` を使用している。現行公開面はそれぞれ `backgroundColor`・`cellTitleColor`・`cellTitleFont`・`cellDescriptionColor` であり、旧名の互換 API は意図的に提供されていないため、spec の例はコンパイルしない (`openspec/specs/settings-view-ios-theme-bridge/spec.md`「Theme / CellStyle の UIKit 変換」「ButtonCell の baseColor 解決順序」 / `ios/Sources/KsSettingsViewUI/Theme.swift` / `ios/Tests/KsSettingsViewUITests/ThemeRenameTests.swift` / `docs/styling-and-theming.md` §2・§4)。

## 用語

- Theme: 画面全体で共有する iOS Native の視覚既定と表示方針を保持する、構造モデルから独立した表示状態。
- CellStyle: 単一 Cell が Theme の一部だけを上書きするための任意指定集合。未指定は「Theme から継承する」を意味する。
- 実効スタイル: Cell 固有値、CellStyle、Theme、iOS 既定を優先順位に従って合成した、描画時に欠損のない値集合。
- プラットフォーム既定: 利用者も Theme も値を指定しない場合に採用する、iOS の意味論と慣習に沿った最終 fallback。
- 明示由来: 実効値がプラットフォーム既定ではなく、CellStyle または Theme の指定から解決された状態。
- Theme 更新: 設定ツリーの構造を変更せず、現在の表示に新しい Theme を再適用する更新。
- 選択フィードバック: 操作可能な Cell の押下・選択中だけ背景を選択色へ切り替え、解除時に実効背景へ戻す視覚応答。
- 無効時の意味色置換: Cell 全体の透明度を変えず、操作不能時のテキスト色を専用色へ置換する表現規則。

## 抽出メモ

- 独立概念は「iOS Native スタイル階層」「iOS Theme 更新境界」「iOS Cell の視覚状態優先順位」の3件を提案する。
- 「iOS Native スタイル階層」は Android 側にも同形の上位原則があり、ADR-0009 の現在形でもある。統合時は `styling/` にプラットフォーム共通の優先順位を一度だけ置き、UIKit の fallback と Native 型だけを iOS 補足へ分ける案が適切である。
- 「iOS Theme 更新境界」は `settings-view-ios-host` の候補「iOS Native 設定画面ホスト境界」「iOS の observable Store 境界」および `settings-view-ios-swiftui` の候補「宣言ツリーの更新分類」と重なる。独立ファイル化より、構造更新から Theme を分離する共通不変条件へ合流する余地が大きい。
- 「iOS Cell の視覚状態優先順位」は基本 Cell の共通描画契約と重なる。Cell 種別ごとのプロパティ列挙は concepts に残さず、通常・選択・無効の優先順位だけを `styling/` または `cells/` の共通契約へ統合するのがよい。
- KsImage の各派生から Native 画像を得る処理は現行コードとテストで確認できるが、独立した長命概念にする粒度ではない。Native 型を UI 層へ隔離する ADR-0009 と Cell のアイコン契約へ合流する候補とする。
- Theme modifier の初期適用と更新経路はコードで確認できる一方、SwiftUI test target には Theme modifier をホスティングして初期値・再更新を検証するテストがない。また Controller の Theme テストは snapshot の項目数が不変であることまでで、実際の可視 Cell・Header / Footer・レイアウトの再描画は検証していない。これは spec との矛盾とは断定せず、統合・後続 drift 検証時の低信頼箇所として残す。
