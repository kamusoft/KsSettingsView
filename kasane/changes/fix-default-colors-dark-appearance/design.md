# Design: fix-default-colors-dark-appearance

## Context

Theme の既定色が (iOS の title / description を除き) ライト前提の固定値で、ダーク外観で Theme を渡さないと判読できない。根本原因は Theme の色が外観を持たない静的な値しか表せないこと (Compose `Color` は不変値)。設計の正は [core/ADR-0030](../../decisions/core/0030-theme-dark-appearance-library-owned-light-dark-defaults.md) (proposed) で、本 design はそれを実装単位の決定に落とす。

現行の構造 (コードで確認):

- iOS: `Theme` の既定色は `public static let` の固定 RGB (`ios/Sources/KsSettingsViewUI/Theme.swift`)。`.label` / `.secondaryLabel` だけ dynamic。描画は `UIColor` をそのまま `backgroundColor` / `textColor` に渡すため dynamic 色なら自動追随する。例外は Modern の Section 装飾の枠線で、`layer.borderColor` に CGColor を置く (`SectionBoxDecorationView.swift`)。`Theme` の `==` は各色の `isEqual` で判定する
- Android: `Theme` / `CellStyle` は Compose `Color` を持つ data class。既定色は `Theme.DEFAULT_*` の public 定数、上書き可能な色は `Color? = null` (`Theme.kt` / `CellStyle.kt`)。title / valueText だけ「未指定なら描画時に Context の `textColorPrimary` から解決」(`EffectiveStyle.kt` の `resolveDefaultTitleColor`)。Theme の消費者は adapter 経由の `EffectiveStyle.from(context, theme, cellStyle)`、`RecyclerView` の背景、Classic / Modern の ItemDecoration、Section / Root Header・Footer の ViewHolder、選択面 (`PickerSelectionSheet`)、カレンダー dialog の配色 (`resolveDatePickerDialogColors`) に散っている。`KsSettingsView` は利用者から受けた Theme を `themeBacking` と `internalTheme` の 2 つのフィールドで持つ。同梱テーマ付き Context (`ksThemedContext()`) は夜間モードをキーにキャッシュされ、Activity の Configuration が変われば新しいラッパを返す
- MAUI: facade は未指定を null で bridge DTO に載せ、Android bridge の `KsBridgeTheme.resolve()` が `?: Theme().x` で既定を埋める。iOS bridge も同型
- .NET MAUI のテンプレート既定 `MainActivity` は `ConfigChanges.UiMode` を宣言するため、夜間モード変更で Activity は再生成されず View が生き残る (`samples/maui/.../Platforms/Android/MainActivity.cs`)

## Goals / Non-Goals

Goals:
- Theme を渡さない・一部だけ上書きした利用者が、3 platform ともダーク外観で判読できる
- 既存利用者のライト時の見た目を変えない
- Android の公開面を Compose の慣習 (`Color.Unspecified`・`Defaults` factory・light / dark 2 セット) に揃える
- Android で View が生き残る構成 (MAUI 既定テンプレート) でも表示中の外観変更に未指定の既定が追随する

Non-Goals: proposal.md の Non-Goals のとおり (ホスト `MaterialTheme` 追随、明示指定色の表示中追随、Theme 構造の見直し、色以外の nullable、skills / README、MAUI サンプルの再生成回避策の撤去)。

## Decisions

### Decision 1: 既定色の所有と dark 値の出所

**採用案:** ライブラリが light / dark の 2 セットの既定色を所有する。light は現行の固定値そのまま、dark は iOS の dark システムパレットを不透明に近似した値を採り、3 platform で同じ生値を置く。値の正は `ui/mock/approved.png` の色ロール対応表 (承認モック)。iOS の title / description の既定はシステム色 (`.label` / `.secondaryLabel`) のまま残し、Android は同じロールに近似値を置く (ライトの platform 差は現状どおり許容、handbook cross/sample-parity.md の「本体既定値の platform 差」)。

**理由:** 現行の light 既定値が iOS の light パレットの近似なので、同じ出所で対にすると light / dark が同じ文法になる。OS 標準色なので「ライブラリ既定色を中立に保つ」(style-resolution.md) にも合う。

**代替案:**
- **A: 独自のダーク配色を新規デザイン** — light は iOS 風のまま dark だけ独自になり、特定の趣味の配色になりやすい。却下
- **B: Material3 の dark baseline** — light と dark で文法が変わる。却下
- **C: iOS はセマンティック色に全面置換** — ライト時の見た目が変わる (separator が半透明、Header 文字が `.secondaryLabel` 相当)。却下 (ADR-0030)

### Decision 2: 未指定の色だけを描画時に解決し、明示指定は変えない

**採用案:** 「未指定」を表す印を Theme に持たせ、描画側が現在の外観の既定へ解決する。明示指定された色は外観で変えない。旧既定値との値比較で未指定を推測しない。iOS の印は `UIColor` 自体が両外観を持つため既定値 (dynamic) そのもの、Android の印は `Color.Unspecified` (Decision 4)。

**理由:** Compose の慣習 (`Color.Unspecified` + `takeOrElse` の遅延解決) と一致し、利用者が accent だけ上書きした Theme でも残りが外観に追随する。明示指定を変えないのは、利用者が意図して渡した色をライブラリが勝手に変換しないため (並走調査で両者一致)。

**代替案:**
- **A: light / dark の Theme の対を持つ新型** — 利用者が減らせるのは「どちらを渡すか」の 1 行だけ。受け口が二重になり Compose に無い発想。却下 (ADR-0030)
- **B: 外観を受け取って Theme を返すコールバック** — 同上、XAML と相性が悪い。却下
- **C: 既定値との値比較で「未指定だったはず」と推測** — 利用者が既定と同じ値を明示した場合と区別できない。却下

### Decision 3: iOS は既定色の `public static let` を dynamic な `UIColor` にする

**採用案:** `Theme.defaultSeparatorColor` / `defaultBackgroundColor` / `defaultSelectedColor` / `defaultAccentColor` / `defaultHeaderBackgroundColor` / `defaultFooterBackgroundColor` / `defaultHeaderTextColor` / `defaultFooterTextColor` / `defaultDisabledTextColor` / `defaultButtonTitleColor` と、`init` の既定引数 `cellBackgroundColor: UIColor = .white` を、`UIColor(dynamicProvider:)` で light = 現行値 / dark = Decision 1 の値の対にする (`cellBackgroundColor` は新設の `defaultCellBackgroundColor` を既定引数にする)。`defaultButtonTitleColor` は `.systemBlue` (既に dynamic) を維持する。`defaultCellTitleColor` (`.label`) / `defaultCellDescriptionColor` (`.secondaryLabel`) は不変。dark の生値は `Theme` の internal な定数として持ち、テストが 3 面同値の比較に使う。公開型・シグネチャ・public static の可視性は変えない。

```swift
public static let defaultBackgroundColor = UIColor { trait in
    trait.userInterfaceStyle == .dark ? Theme.darkBackgroundColorValue : UIColor(red: 1.0, green: 1.0, blue: 1.0, alpha: 1.0)
}
```

**理由:** `UIColor` が両外観を持つため、既定値を差し替えるだけで描画側 (`backgroundColor` / `textColor`) は自動追随する。static は単一インスタンスなので `Theme` の `==` (`isEqual`) は既定同士で真のまま。Android 相当の Defaults factory は不要 (`Theme()` と同じ中身になるだけ)。

**代替案:**
- **A: iOS にも `Defaults` 相当を新設し static を internal 化** — UIKit に無い概念で、中身が `Theme()` と同じ。却下 (探索 論点 6)
- **B: セマンティック色への置換** — Decision 1 の C と同じ。却下

### Decision 4: Android の未指定の印は `Color.Unspecified`、Theme と CellStyle の色を全部揃える

**採用案:** `Theme` の色フィールドは全部 `Color` 型にし、既定値を `Color.Unspecified` にする (`separatorColor` / `backgroundColor` / `cellBackgroundColor` / `selectedColor` / `cellAccentColor` / `disabledTextColor` / `headerTextColor` / `headerBackgroundColor` / `footerTextColor` / `footerBackgroundColor` は固定値既定から `Unspecified` 既定へ、`cellTitleColor` / `cellValueTextColor` / `cellDescriptionColor` / `cellHintTextColor` / `cellPlaceholderColor` / `sectionBorderColor` は `Color? = null` から `Color = Color.Unspecified` へ)。`CellStyle` の色フィールド (`titleColor` / `descriptionColor` / `valueTextColor` / `hintTextColor` / `backgroundColor` / `accentColor` / `placeholderColor`) も同じ。解決は `takeOrElse` で `CellStyle` → 解決済み Theme の順。`EffectiveStyle.titleColorIsExplicit` は解決点の内部でしか使われていないため廃止する。色以外の nullable (`TextStyle?` / `Dp?` / `PaddingValues?`) は触らない。

```kotlin
public data class Theme(
    val separatorColor: Color = Color.Unspecified,
    val backgroundColor: Color = Color.Unspecified,
    // ...
    val cellTitleColor: Color = Color.Unspecified,
)
// 解決 (CellStyle → Theme。Theme は Decision 6 で解決済み)
val titleColor = cellStyle.titleColor.takeOrElse { theme.cellTitleColor }
```

**理由:** `Color` は inline value class で nullable はボクシングを起こし、公式の sentinel は `Unspecified` (androidx material3 `Text.kt` と同型)。1 つの data class に未指定の表現を 2 流儀混在させない。ライブラリは未配信で型変更の代償が最小。

**代替案:**
- **A: `Color?` (null = 未指定)** — 非慣習、ボクシング。却下 (並走調査)
- **B: 今回追随させる色だけ `Unspecified`、既存の `Color?` は据え置き** — 2 流儀混在。却下
- **C: 色以外の nullable も `Unspecified` 化 (`Dp.Unspecified` 等)** — 色の課題と無関係で Compose 自身も `TextStyle` は既定値式で扱う。今回は触らない

### Decision 5: Android の公開面は `KsSettingsViewDefaults` の factory、`DEFAULT_*` の色定数は internal

公開 object 名は `KsSettingsViewDefaults` で確定する (Compose の `ButtonDefaults` の命名に倣う。相方レビュー second-opinion-spec-001 の指摘で仮称を解消)。

**採用案:** `jp.kamusoft.kssettingsview.ui.KsSettingsViewDefaults` (object) を新設し、`lightTheme()` / `darkTheme()` (外観で決まる既定を持つ色フィールドを明示した `Theme`。valueText / hintText / placeholder / `sectionBorderColor` はフォールバック契約を保つため `Unspecified` のまま)、`theme(darkTheme: Boolean)` (どちらかを返す)、`@Composable theme()` (`isSystemInDarkTheme()` で選ぶ) を公開する。light / dark の生値は internal なパレット (用途名のロールで持つ) に置き、factory はそれを写す。`Theme.DEFAULT_SEPARATOR_COLOR` 等の色定数は internal 化する (`DEFAULT_CELL_ICON_SIZE_DP_VALUE` / `DEFAULT_CELL_ICON_RADIUS_DP_VALUE` は色ではないため据え置き)。「既定へ戻す」は `Color.Unspecified` を渡す、「既定値を基準に派生値を作る」は factory の値から派生する。Compose 入口 (DSL 方式) の `theme` の既定値式は `KsSettingsViewDefaults.theme()` にする。Store 方式の入口は Theme を Store が運ぶため変えない (`SettingsRootStore(initialTheme = Theme())` の `Theme()` は全未指定で View 側が解決する)。

```kotlin
public object KsSettingsViewDefaults {
    public fun lightTheme(): Theme = Theme(separatorColor = Palette.Light.separator, /* ... */)
    public fun darkTheme(): Theme = Theme(separatorColor = Palette.Dark.separator, /* ... */)
    public fun theme(darkTheme: Boolean): Theme = if (darkTheme) darkTheme() else lightTheme()
    @Composable public fun theme(): Theme = theme(darkTheme = isSystemInDarkTheme())
}

@Composable
public fun KsSettingsView(
    modifier: Modifier = Modifier,
    style: KsSettingsViewStyle = KsSettingsViewStyle.Classic,
    theme: Theme = KsSettingsViewDefaults.theme(),
    /* ... */
)
```

**理由:** Compose Component API Guidelines の「既定値は `ComponentDefaults` に集約し、`@Composable` factory で既定値式からテーマを読む」に一致。プリミティブ (生値) は internal、公開はロール名だけという慣習に揃う。

**代替案:**
- **A: `values-night` の色リソースを正にする** — 公開面が Android リソース依存になり、3 面同値の検証にリソース解決が要る。却下
- **B: 同梱 Material3 テーマの attr から解決** — ライト時の見た目が変わる。却下
- **C: `DEFAULT_*` を public のまま light 値として残す** — dark 値の定数も並べる必要があり、公開面に生値が増える。既定値の参照は factory に一本化する。却下

### Decision 6: Android は Theme を 1 箇所で外観解決してから全消費者へ渡す

**採用案:** `KsSettingsView` が利用者から受けた Theme (`themeBacking`) と、現在の外観で未指定を埋めた解決済み Theme (`internalTheme`) を分けて持つ。解決は internal な 1 関数 (`Theme.resolvedFor(nightMode: Boolean)` 相当) が行い、未指定フィールドのうち既定セットが値を持つものを `KsSettingsViewDefaults.theme(darkTheme)` の対応値で埋める (明示指定は素通し。valueText / hintText / placeholder / `sectionBorderColor` は `Unspecified` のまま残し、`EffectiveStyle` の既存フォールバック (valueText → title、hint → accent、placeholder → ホスト既定) と装飾の透明解決に委ねる)。adapter・`EffectiveStyle`・ItemDecoration・Header / Footer の ViewHolder・選択面・カレンダー dialog の配色は解決済み Theme だけを読む。夜間の判定源は `ksThemedContext()` と同じ (Activity / Application の Configuration の uiMode)。Cell title の既定は解決済み Theme の値になるため、`resolveDefaultTitleColor` (`textColorPrimary` の動的解決) は削除する。ButtonCell の title 既定 (4 段解決の最終段。Theme にフィールドが無い) は、`EffectiveStyle` の解決に外観 (`darkTheme: Boolean`) を渡し、internal パレットの ButtonCell ロールから選ぶ (Compose 用の Context 不要な解決関数も同じ引数を取る)。accent に流用しない (accent だけ変えた利用者の ButtonCell まで変わり、独立した 4 段契約を壊すため)。

構造更新経路 (`setRootDirect` を通る Full diff・Section 置換・可視性変更・Store の再同期) は現行では引数の Theme を `themeBacking` にも書き戻している。解決済み Theme を渡す以上、この書き戻しをやめ、`themeBacking` は利用者の Theme が入る経路 (`theme` setter・Store の `theme` 配信・`bind` 時の初期同期) でしか更新しない。`internalTheme` は常に `themeBacking` から導出する。

**理由:** Theme の消費者が 6 箇所に散っており、各所で `takeOrElse` を書くと解決漏れ (Unspecified の ARGB 変換は透明黒になる) が起きる。解決点を 1 つにすると uiMode 変更時の再解決 (Decision 7) も 1 箇所で済む。

**代替案:**
- **A: 各消費者が描画時に `takeOrElse` で解決** — 6 箇所に同じ分岐が散り、漏れが透明色として現れる。却下
- **B: Store 側で解決済み Theme を配信** — Store は Context を持たず外観を知らない。却下

### Decision 7: Android の View は uiMode の Configuration 変更で未指定を再解決して再適用する

**採用案:** `KsSettingsView.onConfigurationChanged(newConfig)` で夜間モード (`uiMode and UI_MODE_NIGHT_MASK`) の変化を検出し、変わっていれば `themeBacking` から再解決した Theme を `internalTheme` に置き直し、既存の Theme 更新経路 (`applyThemeInternal` 相当: RecyclerView 背景・adapter の Theme 差し替え・payload 付き再 bind・ItemDecoration の再構築) を通す。利用者の Theme (`themeBacking`) は変わらないため `theme` setter の同値スキップは通らず、再適用は内部経路で直接呼ぶ。表示中の選択面・カレンダーの選択面は開いた時点の解決済み値で描かれたまま (再配色も作り直しもしない) で、閉じて開き直したときから新しい外観の値を使う。夜間モードの切替中に選択面が開いているのは稀で、再配色・作り直しは表示状態の復元 (android/ADR-0021) と絡んで複雑になるため。Window へ attach された時点の外観と解決時の外観が違い得るため、attach 時にも夜間モードを照合して必要なら再解決する。

**理由:** MAUI テンプレート既定の Activity は uiMode を自前処理して再生成しないため、View が生き残ったまま外観だけ変わる。既存の Theme 更新経路は行・canvas・Section 装飾の再評価を持つのでそれを流用する。

**代替案:**
- **A: Activity の再生成に任せる** — MAUI 既定テンプレートで表示中に追随しない。却下 (探索 論点 7)
- **B: 消費者側で描画のたびに外観を見る** — Decision 6 の A と同じ散らばり。却下

### Decision 8: iOS の Section 装飾の枠線は trait 変更で CGColor を再解決する

**採用案:** `SectionBoxDecorationView` が最後に適用した枠線色 (`UIColor`) を保持し、`registerForTraitChanges([UITraitUserInterfaceStyle.self])` (iOS 16 は `traitCollectionDidChange` フォールバック) で `layer.borderColor` を再解決する。`KsCheckBoxView` の既存パターンに揃える。

**理由:** CGColor は解決済みの値で外観を持たない。既定の枠線は透明で実害はないが、利用者が dynamic な `sectionBorderColor` を渡したとき枠線だけ古い外観で残るのを防ぐ。

**代替案:**
- **A: 対応しない (既定は透明のため)** — 利用者の dynamic 色で枠線だけ取り残される。却下

### Decision 9: MAUI は facade・wire 形式を変えず、Android bridge の変換だけ追随する

**採用案:** facade (`KsThemeSnapshot` / `SettingsView` の `Color?` プロパティ) と bridge DTO (`Int?` の ARGB) は不変。Android bridge の `KsBridgeTheme.resolve()` / `KsBridgeCellStyle` は `KsBridgeColor.color(x) ?: Color.Unspecified` に単純化する (`Theme()` の既定を埋める処理を撤去)。iOS bridge は `?? base.x` のままでよい (base の static が dynamic になるだけ)。`AppThemeBinding` が facade の色プロパティ経由で Theme 再適用まで届くかは実装の先頭で spike として実物確認する。

**理由:** 未指定を null で運び native の既定に任せる現行契約が、そのまま「native の既定が外観に追随する」契約になる。

**代替案:**
- **A: facade に light / dark のプロパティ組を新設** — Decision 2 の A と同じ。却下

## Risks / Trade-offs

- Android の構造更新経路が解決済み Theme を利用者側へ書き戻す現行実装 (Decision 6 で契約化し、回帰 Scenario で固定)
- Android の解決漏れ: `Unspecified` を `toArgb()` すると透明黒になる。Decision 6 で解決点を 1 つにし、`EffectiveStyle` と各消費者が解決済み Theme 以外を受け取らない型 / 経路にする (テストで全消費者の入力が specified であることを検査する)
- iOS の等価性: `UIColor(dynamicProvider:)` の `isEqual` は同一インスタンスでのみ真。既定は static の単一インスタンスなので `Theme() == Theme()` は保たれるが、テストで固定 RGB と `isEqual` 比較している箇所は dark / light trait での `resolvedColor` 比較に書き換える
- iOS の `EffectiveStyle` が保持する `UIColor` は dynamic のまま渡るため、描画時解決になる。`cgColor` を取る箇所は Decision 8 の枠線以外にない (`KsCheckBoxView` は対応済み) ことを実装で再確認する
- Android の `titleColorIsExplicit` 廃止で、title 既定の解決が Context 依存から Theme 依存に変わる。同梱テーマの `textColorPrimary` と 2 セットの値 (黒 / 白) は実質同じだが、ライトで完全一致することをテストで固定する
- MAUI `AppThemeBinding` の経路が届かない場合: facade のプロパティ変更 → snapshot → bridge → native `applyTheme` の経路のどこで止まるかで対処が変わるため、spike で確認してから tasks を進める (届かなければ探索へ戻す)
- 破壊的変更: リポジトリ内の `null` 渡し (samples の `SampleTheme`)、`?:` 補完 (bridge)、`DEFAULT_*` 参照 (tests) の追随が要る。skills/ の例は docs-refresh 送り (Non-Goal)

## Migration Plan

1. spike: MAUI `AppThemeBinding` → native Theme 再適用の経路を実物で確認する (Android / iOS 各 1 面)。届かなければ探索へ戻す
2. Android 本体: `Theme` / `CellStyle` の型変更 (コンパイルエラー駆動で消費者を洗い出す) → internal パレットと `KsSettingsViewDefaults` → 解決点 (`resolvedFor`) と `KsSettingsView` の `internalTheme` 経路 → `EffectiveStyle` の `takeOrElse` 化と `resolveDefaultTitleColor` 削除 → 消費者 (decorations / H・F holder / 選択面 / dialog) を解決済み Theme に統一 → `onConfigurationChanged` / attach 時の再解決 → Compose 入口の既定値式 → bridge の変換 → tests
3. iOS 本体: 既定色の dynamic 化と dark 定数 → `SectionBoxDecorationView` の trait 再解決 → tests (dark trait 解決での 3 面同値比較を含む)
4. samples: Android `SampleTheme` の `null` → `Unspecified`。3 面とも文言・構成は変えない
5. 視覚照合: Theme を渡さない画面 (Store / DSL / 共通フィールド統合 / isVisible / Section 装飾デモ) を 4 実行面のダークで撮影し、承認モックと照合。MAUI Android は「システム」選択で端末の夜間を切り替えて表示中の追随も撮る
6. 蒸留: concepts の改訂 (exploration.md 論点 8 の範囲)、ADR-0030 の accepted 昇格

## Open Questions

- なし (accent の dark 値は承認モック案A で iOS の dark systemBlue 側に確定。公開 object 名は Decision 5 で確定)

## ADR 候補

- Decision 1 / 2 / 3 / 4 / 5 / 7 は [core/ADR-0030](../../decisions/core/0030-theme-dark-appearance-library-owned-light-dark-defaults.md) (proposed) が既に扱う。蒸留時に accepted へ昇格する (実装で覆る知見が出たら書き換える)
- Decision 6 (単一解決点)・8 (枠線の再解決)・9 (bridge の変換) は実装の内部構造で、コード + テストに任せる。新規 ADR なし
