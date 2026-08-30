# Candidate: settings-view-android-style

## 概念候補

### Android スタイルの所有境界 (提案カテゴリ: styling/)

設定画面の見た目を表す値は Android の UI 層が所有し、プラットフォーム非依存の設定モデルへ混在させない。画面全体の既定と単一 Cell の上書きを分け、利用者は Android の色・文字・寸法型を変換なしで渡せる。

責務境界:

- Core は設定内容と構造を表し、Theme、CellStyle、画像表現、実効スタイルを持たない。
- Theme は画面全体と Section 装飾の既定、CellStyle は単一 Cell の上書きだけを担う。
- 論理値から Android View が消費する色・書体・寸法への変換と描画反映は UI 層内で完結する。

不変条件:

- 全体背景と Cell 背景は独立した値であり、一方の変更から他方を推論しない。
- CellStyle の未指定値は「値なし」ではなく、上位の既定を継承する意思を表す。
- Theme の値等価性は、同じ見た目指定の不要な再適用を抑止するために利用できる。

出典: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt`、`CellStyle.kt`、`EffectiveStyle.kt`、`android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ThemeTest.kt`、`CellStyleTest.kt`、`docs/architecture.md` §1・§3、`openspec/specs/settings-view-android-style/spec.md` Purpose / 「Theme 型 (UI 層)」/「CellStyle 型 (UI 層)」

### 段階的な実効スタイル解決 (提案カテゴリ: styling/)

Cell 描画に使う最終値を、局所指定、画面全体の既定、プラットフォーム既定の順に解決する。利用者は必要な差分だけを Cell に指定でき、未指定の属性は画面全体の一貫性を保つ。

不変条件:

- 通常属性の優先順位は「Cell 個別 → Theme → platform default」である。
- Cell 種別が独自の意味属性を公開する場合、その属性は通常の Cell 個別指定より前に置ける。Button の意味色はこの特例に当たる。
- 独立した文字サイズ指定が正値なら、継承した文字スタイルの size だけを置換し、他の文字属性は維持する。
- 無効状態のテキスト色は、通常の段階解決後に状態表現として置換される。
- 背景、選択時フィードバック、アクセント、文字、行高さは同じ解決結果から各描画器へ供給し、Cell 種別ごとに別の継承規則を作らない。

出典: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyle.kt`、`CellBaseLayout.kt`、`ButtonCellViewHolder.kt`、`android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyleResolutionTest.kt`、`EffectiveStyleTest.kt`、`BasicCellsTest.kt`、`openspec/specs/settings-view-android-style/spec.md`「EffectiveStyle の解決順序」

### Theme の独立更新経路 (提案カテゴリ: architecture/)

Theme は設定ツリーの構造や内容ではなく、同じ表示要素に再適用できる独立した表示状態として扱う。Theme の変更で Section や Cell の同一性を変えず、現在表示されている描画器、画面背景、Section 装飾を新しい値で再評価する。

不変条件:

- Theme 更新は構造 Diff を発行しない。
- Theme の現在値は永続スナップショットとして保持し、画面はバインド開始時にも取得できる。
- 同値の Theme は再適用しない。
- Theme 変更は既存の表示項目を再利用した再 bind と装飾の再描画で反映し、表示項目を構造上の別物として扱わない。

出典: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt`、`KsSettingsView.kt`、`android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStoreTest.kt` の applyTheme テスト、`ApplyDiffTest.kt` の Theme 更新テスト、`docs/styling-and-theming.md` §12、`docs/architecture.md` §5

### Android Cell 行の寸法契約 (提案カテゴリ: styling/)

Android の Cell 行は、内容に応じて伸びられる自動高さを既定としつつ、タップ対象と補助情報の視認性に必要な最低高さを保証する。利用者が固定高さを選んだ場合だけ、内容量にかかわらず指定高さへ揃える。

不変条件:

- 個別 Cell の正の高さ指定、画面全体の正の高さ指定、Android 既定の順に基準高さを選ぶ。
- 最終的な基準高さは Android の最低行高さ 60dp を下回らない。
- 可変高さモードでは 60dp 以上を下限とし、長い内容が下限を超えて自然に伸びることを妨げない。
- 固定高さモードでは実効高さを固定値として適用し、可変高さ用の minimum height を残さない。
- Android の最低行高さは iOS の値と機械的に共通化せず、各プラットフォームの互換性と慣習を保つ。

出典: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyle.kt`、`CellBaseLayout.kt`、`MinHeightConstraintLayout.kt`、`android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyleTest.kt`、`MinHeightConstraintLayoutTest.kt`、`UnifyCellCommonFieldsTest.kt` の最低高さ回帰テスト、`openspec/specs/settings-view-android-style/spec.md`「行高さ (RowHeight / HasUnevenRows) の適用」、`docs/styling-and-theming.md` §6

### Android 設定リストの視覚モードと共通行 (提案カテゴリ: platforms/)

同じ設定ツリーと描画器を保ったまま、フラットな互換表示と角丸グルーピング表示を選択できる。Cell の主要情報と補助情報は共通の行構造で配置し、Cell 種別固有の trailing control だけを差し替える。

責務境界:

- 視覚モードは Section の外観と区切り方を変更するが、設定モデル、安定 ID、Cell 描画器の登録を変更しない。
- 共通行はタイトル、説明、値、アイコン、ヒント、trailing slot の配置と共通スタイル反映を担う。trailing control の内容と操作は Cell 種別側が担う。
- Section / Root の文字アクセサリは Theme の Header / Footer 属性を使い、Header は後続 Cell 側、Footer は先行 Cell 側へ寄せて配置する。

不変条件:

- 視覚モードの切替は既存の Section 装飾を一つだけ入れ替え、同じリスト内容を再利用する。
- 任意の共通フィールドが未指定なら対応する表示領域を占有しない。
- 共通行は Cell 種別によらず同じ左右端と縦方向の基準を提供し、trailing control の右端を揃える。
- 無効状態は行全体の透明度変更ではなく、共通テキストの無効色と内部 control の標準 disabled 表現で示す。

出典: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsViewStyle.kt`、`KsSettingsView.kt`、`ClassicSectionDecoration.kt`、`ModernSectionDecoration.kt`、`CellBaseLayout.kt`、`SectionAccessoryViewHolders.kt`、`android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsViewStyleTest.kt`、`UnifyCellCommonFieldsTest.kt`、`SectionAccessoryRenderingTest.kt`、`openspec/specs/settings-view-android-style/spec.md` Purpose / 「スタイル切替」/「Section H/F の描画」

## ADR 候補

- 新規候補なし。スタイルを UI 層に隔離し Native 型で表現する決定、Theme を構造 Diff から分離する決定、実効値の段階解決は ADR-0009 に統合済み。全 Cell の共通行をコンポジションで統一し Android 外枠を View ベースに保つ決定は ADR-0011 に統合済み。行高さ、罫線、余白、具体色は既存トリアージで局所的な視覚契約として ADR 対象外とされている。

## drift 所見

- spec の Purpose は Theme / CellStyle の変換を `settings-view-android-theme-bridge` へ分離し、この capability は結果を消費するだけとする一方、同じ spec の後半に Theme / CellStyle / EffectiveStyle / KsImage の型と変換規則を再収録している。コード上もこれらは同じ UI モジュールにあり、capability 間の論理境界が spec 内で重複している (`openspec/specs/settings-view-android-style/spec.md` Purpose / 「Theme 型 (UI 層)」以降、`docs/architecture.md` §7 / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt`、`CellStyle.kt`、`EffectiveStyle.kt`)。
- spec は icon の実効サイズと角丸半径を Cell 描画で使用する契約を置くが、現行の共通行は icon を常に 24dp で構築し、実効サイズ・角丸半径のアクセサを bind から呼ばない。テストもアクセサの戻り値までで e2e 描画を検証していない (`openspec/specs/settings-view-android-style/spec.md`「CellStyle 型 (UI 層)」/「EffectiveStyle の解決順序」 / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyle.kt`、`CellBaseLayout.kt` / `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyleResolutionTest.kt`)。
- spec の fontFamily e2e Scenario は指定した FontFamily が描画まで保持されるとするが、現行の Android View 変換は fontFamily を参照せず、システム既定 Typeface に weight だけを合成する (`openspec/specs/settings-view-android-style/spec.md`「fontFamily 反映の e2e」 / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyle.kt`)。
- spec は論理アクセサの未指定タイトル色を Material / TextView の platform default とするが、現行アクセサは固定の黒を返し、Context を受ける View 変換経路だけが `textColorPrimary` を解決する。現行テストはアクセサについて固定の黒を正としている (`openspec/specs/settings-view-android-style/spec.md`「通常 Cell の解決順序 (既定フォールバック)」 / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyle.kt` / `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyleResolutionTest.kt`、`EffectiveStyleTest.kt`)。
- Theme の `scrollIndicatorVisible` は公開値として保持・既定値だけがテストされるが、RecyclerView のスクロールバー表示へ適用するコードが存在しない (`openspec/specs/settings-view-android-style/spec.md`「Theme 型 (UI 層)」 / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt`、`KsSettingsView.kt` / `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ThemeTest.kt`)。
- 「基本 Cell 共通の垂直パディング」Requirement はコンテナを LinearLayout、最低高さを旧 44dp と記述するが、現行実装と同じ spec の行高さ Requirement は共通 ConstraintLayout と最終下限 60dp を正としている (`openspec/specs/settings-view-android-style/spec.md`「基本 Cell 共通の垂直パディング」/「行高さ (RowHeight / HasUnevenRows) の適用」 / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellBaseLayout.kt`、`EffectiveStyle.kt`、`MinHeightConstraintLayout.kt`)。
- Section の Compose backing 更新 Scenario は `ComposeView.setContent` を再呼び出すと記述するが、現行実装とテストは ComposeView と Composition を再利用し、bind 時には内部 MutableState の content だけを差し替える (`openspec/specs/settings-view-android-style/spec.md`「View 形式ヘッダの中身更新」 / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SectionAccessoryViewHolders.kt` / `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/SectionAccessoryRenderingTest.kt`)。

## 用語

- Theme: 設定画面全体、Section 装飾、全 Cell の既定となる UI 層のスタイル値。
- CellStyle: 単一 Cell だけで Theme を部分上書きする nullable なスタイル値。
- 実効スタイル: Cell 個別値、Theme、platform default を解決し、描画時に使える確定値へ変換した結果。
- platform default: CellStyle と Theme の双方が未指定のとき、Android のテーマまたは UI 層の既定から得る値。
- 明示指定: CellStyle または Theme に利用者由来の値があり、platform default ではない状態。
- 可変高さモード: 基準高さを最低値として保証しつつ、内容が必要とする高さまで行を伸ばすモード。
- 固定高さモード: 内容量にかかわらず実効高さを行の高さとして使うモード。
- 共通行: Cell 間で共有する主要情報・補助情報・trailing slot の配置契約。
- trailing slot: Switch、Checkbox、矢印など Cell 種別固有の control を置く行末領域。
- Classic: Section をフラットな罫線で区切る Android の視覚モード。
- Modern: Section を外側余白と角丸背景でグループ化する Android の視覚モード。

## 抽出メモ

- 「Android スタイルの所有境界」「段階的な実効スタイル解決」「Theme の独立更新経路」は `settings-view-android-theme-bridge` と `settings-view-android-host` の候補に重なる。統合時は ADR-0009 を中心に共通原則を一つへまとめ、Android 固有の View 変換・再 bind だけを platforms/ 側へ残すのが自然。
- 「Android 設定リストの視覚モードと共通行」のうち共通行部分は ADR-0011 と `cell-types-basic` 候補へ合流し得る。Classic / Modern と Section 装飾は Android 固有なので platforms/ または styling/ の Android 節として残す余地がある。
- 60dp の最低行高さはコード・テスト・spec の主要 Requirement が一致し、現在の視覚契約として確度が高い。ただし具体値は design token に近いため、独立した architecture 概念ではなく styling/ に置く。
- アイコン寸法・角丸、fontFamily、スクロールインジケータは実装と spec が一致しないため、どちらを正にするか決めず概念候補の不変条件から除外した。
- Section / Root H/F、Switch / Checkbox の見た目、Classic / Modern、Theme Bridge が一つの旧 capability に同居しており粒度が大きい。統合時は「スタイル解決」「共通行」「Section 装飾」「Cell 固有 control 調整」の境界で分けると長命知識を保ちやすい。
