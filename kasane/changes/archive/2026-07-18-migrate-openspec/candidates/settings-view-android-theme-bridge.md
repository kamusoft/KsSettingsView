# Candidate: settings-view-android-theme-bridge

## 概念候補

### Android Native スタイル階層 (提案カテゴリ: styling/)

Android のスタイル境界は、画面全体の既定、単一 Cell の部分上書き、Android の既定を階層的に合成し、描画側が未指定値を個別に扱わずに済む実効値を提供する。色・文字・寸法・画像は Android UI の Native 型で表し、Core の構造モデルとの間に独自の論理色・論理フォント変換を置かない。

画面全体の既定は Cell 間で共有する視覚方針を表し、Cell 個別スタイルの未指定値だけを補完する。Cell 種別が固有の意味色を持つ場合は、その値を個別スタイルより先に解決できる。どの段にも指定がない項目は Android のテーマまたは UI 層の既定へ必ず解決される。画面背景と Cell 背景は異なる表示領域の値であり、相互の代用にはしない。

この境界が担うのはスタイル値の所有、優先順位の解決、Android View が消費する値への橋渡しである。設定ツリーの構造、宣言ツリーの差分算出、リストへの構造変更適用は担わない。

出典: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt`、`CellStyle.kt`、`EffectiveStyle.kt`、`android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ThemeTest.kt`、`CellStyleTest.kt`、`EffectiveStyleResolutionTest.kt`、`EffectiveStyleTest.kt`、`openspec/specs/settings-view-android-theme-bridge/spec.md`「Theme / CellStyle の Android 変換」、`docs/styling-and-theming.md` §1〜§4、`docs/architecture.md` §3、`kasane/decisions/0009-ui-layer-native-styling.md`

### Android Theme 更新境界 (提案カテゴリ: platforms/)

Theme は設定ツリーの構造ではなく、画面側が所有する独立した表示状態である。宣言方式と外部 Store 方式のどちらから指定しても UI 層の Theme 状態へ収束し、現在の画面背景、Cell 描画、Section 装飾を再評価する。

Theme の変更は Section / Cell の識別子、集合、順序を変えず、構造変更値にも含めない。同値の再適用は通知を発生させない。これにより Theme の切替は構造同期と独立し、既存の表示要素を維持したまま反映される。

この境界が担うのは Theme の保持・通知・再描画への接続である。個々のスタイル値の優先順位は Native スタイル階層、構造変更は Store とホスト、宣言ツリーの比較は Compose ラッパが担う。

出典: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt`、`KsSettingsView.kt`、`android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposable.kt`、`android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStoreTest.kt`、`ApplyDiffTest.kt`、`android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposeTest.kt`、`SettingsRootBuilderTest.kt`、`docs/styling-and-theming.md` §12、`docs/architecture.md` §3・§5、`kasane/decisions/0009-ui-layer-native-styling.md`

### Android Cell の視覚状態優先順位 (提案カテゴリ: styling/)

Android の Cell 描画は、通常・押下・無効の状態をスタイル値の上に重ねる。操作可能な Cell の押下中は選択色をフィードバックとして使い、通常時はその Cell の実効背景色を保つ。無効状態は押下状態より優先し、Cell の操作と内包するコントロールの操作を抑止する。

無効状態の表現は Cell 全体の透明度を下げず、テキストの意味色を無効時の色へ置換し、内包する Android コントロールを無効化する。これにより、背景・画像・レイアウトまで一律に薄めることなく、操作不能であることを一貫して示す。

Cell 固有の基準色を持つ種類では、その値を個別スタイル、Theme、Android の慣習色より先に解決し、無効状態では最終的に無効時の色を優先する。

出典: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellBaseLayout.kt`、`ButtonCellViewHolder.kt`、`EffectiveStyle.kt`、各 Cell ViewHolder、`android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/BasicCellsTest.kt`、`UnifyCellCommonFieldsTest.kt`、`EffectiveStyleResolutionTest.kt`、`openspec/specs/settings-view-android-theme-bridge/spec.md`「タッチフィードバック」「isEnabled 描画の反映」「ButtonCell の baseColor 解決順序」、`docs/styling-and-theming.md` §9〜§11、`kasane/decisions/0011-composed-shared-cell-row-layout.md`

## ADR 候補

- 新規候補なし。スタイル型を UI 層へ置いて Native 型で表現する判断、実効スタイルの優先順位、Theme を構造モデルと構造 Diff から分離する判断は `kasane/decisions/0009-ui-layer-native-styling.md` に accepted として記録済みである。共通の Cell 描画と無効状態の反映を UI 層内部へ集約する判断も `kasane/decisions/0011-composed-shared-cell-row-layout.md` に記録済みである。Android で解決できない画像表現を非表示へフォールバックする規則は局所的かつ可逆で、ADR 選別3基準を満たさない。

## drift 所見

- Purpose は Core が `Theme` / `CellStyle` / `KsColor` / `KsImage` を持ち、それらを Android 値へ変換するブリッジを本 capability の責務としている。しかし現行 Core にこれらのスタイル型はなく、UI 層の `Theme` / `CellStyle` / `KsImage` が Compose `Color` / `TextStyle` / `Dp` や Android `Drawable` を直接保持する。独自色・フォント変換も存在せず、現行の責務は実効スタイル合成と Android View への反映である。Requirement 内の「変換ユーティリティは存在してはならない」という記述および現行 docs とも Purpose が自己矛盾している (`openspec/specs/settings-view-android-theme-bridge/spec.md` Purpose・「Theme / CellStyle の Android 変換」・「KsImage 派生のアイコン解決」 / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt` / `CellStyle.kt` / `KsImage.kt` / `docs/architecture.md` §1・§3)。
- Requirement と Scenario は削除済みの Theme 名 `viewBackgroundColor`・`titleColor`・`titleFont`・`descriptionColor` を使用している。現行公開面はそれぞれ `backgroundColor`・`cellTitleColor`・`cellTitleFont`・`cellDescriptionColor` であり、旧名の互換 API は提供されていないため、spec の例はコンパイルしない (`openspec/specs/settings-view-android-theme-bridge/spec.md`「Theme / CellStyle の Android 変換」「ButtonCell の baseColor 解決順序」 / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt` / `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ThemeRenameTest.kt` / `docs/styling-and-theming.md` §2・§4)。
- spec は個別または Theme 由来のアイコン寸法・角丸半径を実効スタイルとして補完する契約を置く。現行コードには論理値を解決するアクセサとその単体テストがあるが、共通行の画像領域は固定寸法で構築され、解決結果を View の寸法・角丸描画へ適用していない。したがって値の保持と論理解決までは実装済みだが、描画までの橋渡しは成立していない (`openspec/specs/settings-view-android-theme-bridge/spec.md`「Theme / CellStyle の Android 変換」 / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyle.kt` / `CellBaseLayout.kt` / `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyleResolutionTest.kt`)。

## 用語

- Theme: 設定画面全体で共有する Android Native の視覚既定と表示方針を保持する、構造モデルから独立した表示状態。
- CellStyle: 単一 Cell が Theme の一部だけを上書きするための任意指定集合。未指定は「Theme から継承する」を意味する。
- 実効スタイル: Cell 固有値、CellStyle、Theme、Android の既定を優先順位に従って合成し、描画時に欠損なく使える値へ橋渡しした結果。
- Android の既定: 利用者も Theme も値を指定しない場合に採用する、現在の Android テーマまたは UI 層の慣習値。
- 明示由来: 実効値が Android の既定ではなく、CellStyle または Theme の指定から解決された状態。
- Theme 更新: 設定ツリーの構造を変更せず、現在の表示に新しい Theme を再適用する更新。
- 選択フィードバック: 操作可能な Cell の押下中だけ選択色を重ね、通常時には実効背景を保つ視覚応答。
- 無効時の意味色置換: Cell 全体の透明度を変えず、操作不能時のテキスト色を専用色へ置換する表現規則。
- Native 型直保持: 独自の中間スタイル型を挟まず、利用プラットフォームの色・文字・寸法・画像型を UI 層が直接保持する方針。

## 抽出メモ

- 独立概念は「Android Native スタイル階層」「Android Theme 更新境界」「Android Cell の視覚状態優先順位」の3件を提案する。
- 「Android Native スタイル階層」は iOS 側にも同形の上位原則があり、ADR-0009 の現在形でもある。統合時は `styling/` にプラットフォーム共通の優先順位を一度だけ置き、Android テーマからの fallback と View 値への変換だけを Android 補足へ分ける案が適切である。
- 「Android Theme 更新境界」は `settings-view-android-host`、`settings-view-android-compose`、`settings-view-android-style` の候補と重なる。独立ファイル化より、構造更新から Theme を分離する共通不変条件へ合流する余地が大きい。
- 「Android Cell の視覚状態優先順位」は基本 Cell の共通描画契約と重なる。Cell 種別ごとの処理列挙は concepts に残さず、通常・押下・無効の優先順位だけを `styling/` または `cells/` の共通契約へ統合するのがよい。
- 画像表現の派生ごとの解決処理はコード・テスト・spec が一致するが、独立した長命概念にする粒度ではない。Native 型を UI 層へ隔離する ADR-0009 と Cell のアイコン契約へ合流する候補とする。アイコン寸法・角丸は描画への適用が確認できないため概念の不変条件には含めず、drift 所見に限定した。
- Theme の初期適用・変更・同値抑制はコードとテストで確認できる。一方、Theme 変更時に payload 再 bind された可視 Cell、Root / Section Header・Footer、装飾の全てが新値で描画されたことを一つの経路で確認するテストはなく、統合・後続 drift 検証時の低信頼箇所として残す。
