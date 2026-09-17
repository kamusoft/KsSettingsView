# Verify 001: scroll-indicator-visible-not-applied

**判定**: VALID

**検証日**: 2026-09-17
**対象**: デルタスペック 3 件 (settings-view-ios-ui / settings-view-android-ui / maui-bridge) の全 Requirement / 全 Scenario と、Requirement 本文の SHALL / SHALL NOT
**対象の実装**: develop の作業ツリーの未コミット差分 (`git diff HEAD` と未追跡ファイル。`android/` ・ `ios/` ・ `maui/` 配下)
**対象外**: `kasane/changes/` 配下の他の change ディレクトリ

`deviation.md` は存在しない。`proposal.md` の Impact に記録された「`ui/` (brief・モック) は作らない」はオーナー合意済みの逸脱として扱い、違反として数えていない。`tasks.md` 5.2b (iOS のスクロールインジケータの実機目視) はオーナー確認待ちの未了として扱い、それ単独を INVALID の理由にしていない。

凡例: ✅ 一致 / ⚠️ deviation 記録済み・要注記 / ❌ 欠落・乖離

---

## 1. settings-view-ios-ui

### Requirement: スクロールバー表示の Theme 反映 (iOS)

実装:

- `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:425` (`makeCollectionView` 相当の初期化時反映)、`:492-495` (`applyScrollIndicatorVisibility(theme:)`)、`:385` (`applyTheme(_:)` からの呼び出し)、`:451` (初期構築の確定後の再反映)
- `ios/Sources/KsSettingsViewUI/PickerListViewController.swift:80` (開いた時点の値を控える)、`:102` (`tableView.showsVerticalScrollIndicator`)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 既定の Theme ではスクロールバーを表示する | `KsSettingsViewController.swift:425` | `ScrollIndicatorVisibleTests.test_既定Themeでは縦スクロールインジケータが表示される` | ✅ |
| false を指定した Theme ではスクロールバーを表示しない | 同上 | `ScrollIndicatorVisibleTests.test_scrollIndicatorVisibleがfalseの初期Themeで縦インジケータが消える` | ✅ |
| 表示中の Theme 差し替えに追従する | `KsSettingsViewController.swift:385, 492-495` | `ScrollIndicatorVisibleTests.test_applyThemeで縦インジケータの表示が追従する` (false → true の往復まで踏む) | ✅ |
| Store 経由と DSL 経由でも同じ結果になる | 同上 | `ScrollIndicatorVisibleTests.test_Store経由のTheme更新で縦インジケータの表示が追従する` / `DSLScrollIndicatorVisibleTests.test_DSL再評価で渡したfalseが縦インジケータへ届く`・`test_DSLに最初から渡したfalseが縦インジケータへ届く` | ✅ |
| スクロールバーの表示だけを変えても行とスクロール位置は維持される | `KsSettingsViewController.swift:492-495` (layout・snapshot に触れない) | `ScrollIndicatorVisibleTests.test_スクロールバーの表示だけを変えても行とスクロール位置は維持される` | ✅ |
| PickerCell の候補リストも表示設定に従う | `PickerListViewController.swift:80, 102` | `ScrollIndicatorVisibleTests.test_PickerCellの候補リストも表示設定に従う` | ✅ |

Scenario の中身の確認:

- 「行とスクロール位置は維持される」は GIVEN (40 行 + 途中までスクロール)・WHEN (`scrollIndicatorVisible` だけが異なる Theme を `applyTheme`)・THEN (content offset の一致、可視 Section / Cell ID 列の一致、可視 cell の View インスタンスの `===` 一致) を 3 点とも踏んでいる。自己サイズ行の offset 補正が落ち着くまで待ってから基準値を控えており、Theme と無関係なずれを掴まない形になっている
- 「PickerCell の候補リスト」は GIVEN の 2 つの Theme をそれぞれ `PickerCellView.render` → `_makeListViewControllerForTesting()` の提示経路と同じ seam で組み立て、THEN の有効・無効を両方観測している

Requirement 本文の SHALL / SHALL NOT:

| 契約 | 実装 | テスト | 状態 |
|---|---|---|---|
| 反映は最初の表示時と表示中の差し替え時の両方 (Host 直接適用・Store・DSL のいずれの経路でも) | `KsSettingsViewController.swift:385, 425, 451` | 上表の 4 件 | ✅ |
| 表示中の選択面は追従しなくてよく、次に開いたときから新しい値に従う | `PickerListViewController.swift:80` (`let` で控える) | `ScrollIndicatorVisibleTests.test_Themeを差し替えて開き直した候補リストは新しい値に従う` | ✅ |
| 回転ホイールの選択面 (数値・日付・時刻) にスクロールバーを表示しない (SHALL NOT) | `UIPickerView` / `UIDatePicker` を使っており、本 change はそこに触れていない (`showsVerticalScrollIndicator` の代入は Controller と PickerList の 2 箇所のみ) | 専用テストなし | ✅ (注1) |
| 横スクロールバーは対象にしない (SHALL NOT) | `showsHorizontalScrollIndicator` への代入は `ios/Sources/` に 0 件 | 専用テストなし | ✅ (注1) |

### Requirement: Header / Footer 背景色の Theme 反映 (iOS)

実装:

- `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1258` / `:2267` (`backgroundColor` の引数追加)、`:1190` (Section accessory)、`:1223` (Root accessory)、`:2146` / `:2224` (再適用経路)、`:2281` (`UIBackgroundConfiguration.clear()` を土台に、text 形式のときだけ色を置き、View 形式では `nil` にして再利用で残さない)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 指定した背景色が text の Header / Footer に描画される | `:1190, 1223, 2281` | `AccessoryBackgroundColorTests.test_SectionHeaderとFooterにそれぞれの背景色が反映される` + `test_RootHeaderとFooterにそれぞれの背景色が反映される` | ✅ |
| 表示中の Theme 差し替えに追従する | `:2146, 2224, 2281` | `AccessoryBackgroundColorTests.test_applyThemeでSectionHeaderとFooterの背景色が追従する` (identity の維持まで assert) + `test_applyThemeでRootHeaderとFooterの背景色が追従する` | ✅ |
| View 形式の Header には背景色を適用しない | `:2281` (`accessoryText != nil` のときだけ塗る) | `AccessoryBackgroundColorTests.test_View形式のSectionHeaderには背景色を塗らない` | ✅ |
| 利用者の dynamic な背景色は外観切替に追従する | `:2281` (dynamic な `UIColor` をそのまま置く) | `AccessoryBackgroundColorTests.test_利用者のdynamicな背景色は外観切替に追従する` | ✅ |
| text 形式から View 形式へ差し替えた領域に背景色を残さない | `:2281` (毎回 `backgroundConfiguration` を置き直す) | `AccessoryBackgroundColorTests.test_text形式からView形式へ差し替えた領域に背景色を残さない` | ✅ |

Scenario の中身の確認:

- 「指定した背景色が…」の Scenario は GIVEN で Section と Root の両方を要求している。テストは Section 用と Root 用に分かれており、2 件で GIVEN / THEN を覆っている
- 「表示中の Theme 差し替え」の THEN の後半 (Section / Cell の identity 維持) は、差し替え前後の `snapshot().sectionIdentifiers` / `itemIdentifiers` の一致で踏んでいる
- 「利用者の dynamic な背景色…」は WHEN (window の `overrideUserInterfaceStyle` をダークへ) の後、trait の伝播を条件ベース待機で待ってから、Header が置かれている trait で解決した値を見ており、切替前の値を掴まない形になっている

Requirement 本文の SHALL NOT:

| 契約 | 実装 | テスト | 状態 |
|---|---|---|---|
| View 形式の Header / Footer にライブラリの背景色を適用しない | `:2281` | `test_View形式のSectionHeaderには背景色を塗らない` | ✅ |
| text 形式から View 形式へ差し替えた領域に以前の背景色を残さない | `:2281` | `test_text形式からView形式へ差し替えた領域に背景色を残さない` | ✅ |

### Requirement: Header / Footer 背景色の既定は透明 (iOS)

実装:

- `ios/Sources/KsSettingsViewUI/Theme.swift:284-290` (`defaultHeaderBackgroundColor` は両外観の値を持つ `UIColor` の形を保ったまま、`defaultFooterBackgroundColor` はその別名)
- `ios/Sources/KsSettingsViewUI/Theme.swift:377, 379, 381` (`lightHeaderBackgroundColorValue` / `darkHeaderBackgroundColorValue` = `UIColor.clear`、Footer は Header の別名)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 何も指定しない Header / Footer には list 下地が見える | `Theme.swift:284-290` + `KsSettingsViewController.swift:2281` | `AccessoryBackgroundColorTests.test_何も指定しないHeaderFooterにはlist下地が見える` (light / dark の両 trait で alpha = 0 を確認し、list 下地が明示色のままであることも assert) | ✅ |
| 既定値の定数は両外観で透明に解決される | `Theme.swift:284-290, 377, 379` | `AccessoryBackgroundColorTests.test_既定値の定数は両外観で透明に解決される` / `ThemeDefaultColorAppearanceTests.test_既定色のライト解決値が固定値と一致する`・`test_既定色のダーク解決値がdarkセットの値と一致する` | ✅ |
| 既定 Theme 同士の等価性は保たれる | `Theme.swift:157, 162` (既定引数が定数そのもの) | `ThemeDefaultColorAppearanceTests.test_既定Theme同士と既定定数を明示したThemeは等価` | ✅ |
| 固定の透明色を明示した Theme は既定と等価でない | `Theme.swift:284-290` (dynamic な形を保つ) | `AccessoryBackgroundColorTests.test_固定の透明色を明示したThemeは既定と等価でない` (等価性と、どちらでも list 下地が見えることの両方を踏む) | ✅ |

Requirement 本文の SHALL:

| 契約 | 実装 | テスト | 状態 |
|---|---|---|---|
| 公開定数は型・名前・可視性と「両外観の値を持つ `UIColor`」の形を保つ | `Theme.swift:284-290` (`public static let`、`UIColor { trait in ... }` のまま値だけ変更) | `test_既定値の定数は両外観で透明に解決される` + 等価性の 2 件 | ✅ |
| この既定は Android の既定と同じである | `Theme.swift:377, 379` = `UIColor.clear` / `KsThemePalette.kt:43, 49, 86, 92` = `Color.Transparent` | `ThemeDefaultColorAppearanceTests.test_darkセットの生値が3platform共通の値である` と `KsSettingsViewDefaultsTest.どちらのセットも Header と Footer の背景に透明を持ち Unspecified ではない` の双方が新しい値へ追随済み | ✅ (注2) |

---

## 2. settings-view-android-ui

### Requirement: スクロールバー表示の Theme 反映 (Android)

実装:

- `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:89` (内部 `RecyclerView` を `ksScrollIndicatorContext()` から生成)、`:314-316` (`applyScrollIndicatorVisible`)、`:306` / `:697` / `:924` (3 つの反映経路からの呼び出し)、`:334-` (`applyScrollbarThumbForAppearance`)
- `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsThemedContext.kt:140-141` (`ksScrollIndicatorContext()`)
- `android/kssettingsview/src/main/res/values/themes.xml:25-42` (`ThemeOverlay.KsSettingsView.ScrollIndicator` → `recyclerViewStyle` → `android:scrollbars="vertical"`)
- `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:208, 263, 347` / `PickerCellViewHolder.kt:70` (候補リスト)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 既定の Theme ではスクロールバーが描画できる状態になる | `KsSettingsView.kt:89` + `themes.xml:25-42` | `ScrollIndicatorVisibleTest.内部 RecyclerView はスクロールバーの描画状態を持つ` / `内部 RecyclerView の Context がスクロールバー用の style を運んでいる` / `既定 Theme の適用で縦スクロールバーが有効になる` | ✅ |
| false を指定した Theme ではスクロールバーを表示しない | `KsSettingsView.kt:314-315` | `ScrollIndicatorVisibleTest.scrollIndicatorVisible が false の Store を bind すると縦スクロールバーが無効になる` | ✅ |
| 表示中の Theme 差し替えに追従する | `KsSettingsView.kt:697` | `ScrollIndicatorVisibleTest.theme の差し替えに縦スクロールバーの有効無効が追従する` (false → true の往復まで踏む) | ✅ |
| Store 経由と DSL 経由でも同じ結果になる | `KsSettingsView.kt:306, 697` | `ScrollIndicatorVisibleTest.表示中の Store の Theme 更新で縦スクロールバーが無効になる` / `DSLScrollIndicatorVisibleTest.DSL の再評価で渡した false が縦スクロールバーへ届く`・`DSL に最初から渡した false が縦スクロールバーへ届く` | ✅ |
| 構造更新と同時に適用される Theme も反映される | `KsSettingsView.kt:924` (`setRootDirect` 経路) | `ScrollIndicatorVisibleTest.構造更新と同時に適用される Theme も反映される` | ✅ |
| スクロールバーの表示だけを変えても行とスクロール位置は維持される | `KsSettingsView.kt:314-316` | `ScrollIndicatorVisibleTest.スクロールバーの表示だけを変えても行とスクロール位置は維持される` | ✅ |
| 夜間モードの変更にスクロールバーのつまみが追従する | `KsSettingsView.kt:334-` | `ScrollIndicatorVisibleTest.外観の切り替えでスクロールバーの thumb が解決し直される` | ✅ |
| PickerCell の候補リストも表示設定に従う | `PickerSelectionSheet.kt:263, 347` / `PickerCellViewHolder.kt:70` | `SheetScrollbarTest.既定の Theme では候補リストにスクロールバーが出る` / `false を指定した Theme では候補リストのスクロールバーが無効になる` | ✅ |
| 回転ホイールにはスクロールバーを出さない | `PickerSelectionSheet.kt:263` (ラップを候補リストの生成だけに限定) | `SheetScrollbarTest.回転ホイールの可動部にはスクロールバーの描画状態が無い` / `同梱テーマから作った RecyclerView にスクロールバーの描画状態が無い` | ✅ |

Scenario の中身の確認:

- 「既定の Theme では…」の THEN は「表示が有効」と「描画に必要な状態が初期化されている」の 2 つを要求している。前者は `isVerticalScrollBarEnabled`、後者は `verticalScrollbarThumbDrawable` の非 null で踏んでいる。さらに `内部 RecyclerView の Context がスクロールバー用の style を運んでいる` が、実行時の thumb 代入では満たせない観測点 (同じ Context から新しく作った `RecyclerView` の描画状態) を見ており、Context 配線そのものを固定している
- 「行とスクロール位置は維持される」は GIVEN (40 行 + `scrollToPositionWithOffset(6, -24)`)・WHEN (`theme` 代入)・THEN (行の View インスタンスの `assertSame`、Section / Cell の ID 列、`findFirstVisibleItemPosition` とその `top`) を踏んでいる
- 「夜間モードの変更に…」は GIVEN (`notnight` qualifier で表示)・WHEN (`RuntimeEnvironment.setQualifiers("+night")` + `dispatchConfigurationChanged`。Activity は再生成しない)・THEN (同梱テーマを夜間で解決した thumb の `constantState` と一致) を踏み、さらにライトとダークの thumb が別物であることを前提として assert しており、トートロジーになっていない
- 「回転ホイールには…」の THEN は「描画に必要な状態が初期化されていない」を `verticalScrollbarThumbDrawable == null` で踏んでいる

Requirement 本文の SHALL / SHALL NOT:

| 契約 | 実装 | テスト | 状態 |
|---|---|---|---|
| 既定では**スクロール操作中に実際に描画される** | `KsSettingsView.kt:89` + `themes.xml:25-42` | 描画状態の初期化まではテストで担保。実描画は `evidence/android-scrollbar-visible.png` (低速ドラッグ中の撮影) | ✅ |
| つまみの外観は Activity を再生成しない夜間モード変更に追従する | `KsSettingsView.kt:334-` | `外観の切り替えでスクロールバーの thumb が解決し直される` + `evidence/android-night-mode-thumb-before.png` / `-after.png` | ✅ |
| overscroll 効果の色は追従を保証しない | `KsSettingsView.kt:86-87` のコードコメントに明記 (tasks 1.5 の要求) | — (非保証のため対象外) | ✅ |
| 表示中の選択面は追従しなくてよく、次に開いたときから新しい値に従う | `PickerCellViewHolder.kt:70` (bind 時の theme から渡す) | `SheetScrollbarTest.Theme を差し替えて開き直した候補リストは新しい値に従う` | ✅ |
| 有効化を設定リストと候補リストに限定し、他のライブラリ所有リストに出さない (SHALL NOT) | `themes.xml:25-30` (オーバーレイを生成箇所にだけ重ねる) | `SheetScrollbarTest` の回転ホイール 2 件 | ✅ |
| 利用者所有コンテンツの中のリストの見た目を変えない (SHALL NOT) | `KsThemedContext.kt:168-169` (利用者コンテンツはホスト Context のまま) | `HostWrappedContextUserContentTest.ホストが被せた ContextThemeWrapper のテーマ属性が利用者所有コンテンツから解決できる` | ✅ (注3) |
| 横スクロールバーは対象にしない (SHALL NOT) | `themes.xml:41` は `vertical` のみ、コードは `isVerticalScrollBarEnabled` のみ | 専用テストなし | ✅ (注1) |

### Requirement: 利用者所有コンテンツの Context は設定リストの生成方法に依らない

実装:

- `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsThemedContext.kt:168-169` (`View.ksUserContentContext()` が `KsSettingsView` 自身が受け取った Context まで親を辿る)
- `KsCellRegistryCustomCell.kt:29` / `SectionAccessoryViewHolders.kt:199, 274` (3 つの生成箇所)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| ホストが被せたテーマの属性が利用者所有コンテンツから解決できる | `KsThemedContext.kt:168-169` + 上記 3 箇所 | `HostWrappedContextUserContentTest.ホストが被せた ContextThemeWrapper のテーマ属性が利用者所有コンテンツから解決できる` | ✅ |
| その構成でも設定リストのスクロールバーは描画できる | `KsSettingsView.kt:89` | `HostWrappedContextUserContentTest.ホストが被せた ContextThemeWrapper でもスクロールバーの描画状態は同梱テーマから届く` | ✅ |

Scenario の中身の確認: GIVEN の 3 つ (CustomCell・View 形式の Section Header・View 形式の Root Header) をすべて生成し、THEN の「渡された Context がホストの Context と同じインスタンス」を `assertSame` で、「テーマ属性がホスト側の値に解決される」を `colorPrimary` の比較で、それぞれ 3 つとも踏んでいる。Activity とホストラッパで `colorPrimary` が異なることを前提として assert しており、取り違えを検出できる形になっている。

### Requirement: Header / Footer 背景色の既定は透明 (Android)

実装: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsThemePalette.kt:43, 49` (light)、`:86, 92` (dark)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 既定セットの Header / Footer 背景色は透明 | `KsThemePalette.kt:43, 49, 86, 92` | `KsSettingsViewDefaultsTest.どちらのセットも Header と Footer の背景に透明を持ち Unspecified ではない` (+ `lightTheme はライト外観の既定色を持つ` / `darkTheme は色ロール対応表の dark 列と同値`) | ✅ |
| 何も指定しない Header / Footer には list 下地が見える | 同上 (未指定色の解決の仕組みは変えない) | `HeaderFooterBackgroundDefaultTest.何も指定しない Header と Footer には list 下地が見える` (`notnight`) / `夜間モードでも何も指定しない Header と Footer には list 下地が見える` (`night`) | ✅ |
| 明示指定した背景色はそのまま描画される | 同上 | `HeaderFooterBackgroundDefaultTest.明示指定した Header と Footer の背景色はそのまま描画される` | ✅ |

Scenario の中身の確認: 「何も指定しない…」は WHEN が夜間モードの有無の両方を要求しており、テストも `@Config(qualifiers = ...)` で 2 件に分けている。THEN は実際に生成された Header / Footer の `TextView` の `ColorDrawable` が透明であることと、list 下地が明示色のままであることの両方で踏んでいる。

Requirement 本文の SHALL: 「この既定は iOS の既定と同じである」→ 注2 と同じ。

---

## 3. maui-bridge

### Requirement: スクロールバー表示と Header / Footer 背景色は native の契約に追随する

実装: **無変更** (`maui/KsSettingsView.Maui/SettingsView.cs:1029` ほか既存の wire 経路のまま)。本 Requirement は既存の契約の明記であり、diff に facade / bridge の実装変更は無い。

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 未指定の値は未指定のまま native へ渡る | 無変更 | `ThemeAndCellStyleTests.UnsetScrollIndicatorAndHeaderFooterBackgroundsCarryNothing` | ✅ |
| 指定した値はそのまま native へ渡る | 無変更 | `ThemeAndCellStyleTests.SetScrollIndicatorAndHeaderFooterBackgroundsCarryTheirValues` | ✅ |
| 表示中のプロパティ変更が native へ届く | 無変更 | `ThemeAndCellStyleTests.ScrollIndicatorVisibleChangeWhileConnectedIsApplied` + `HeaderFooterBackgroundColorChangeWhileConnectedIsApplied` | ✅ |
| MAUI の実行面で native と同じ見た目になる | native 側の契約に追随 | `evidence/maui-android-default-transparent-and-scrollbar.png` / `evidence/maui-ios-default-transparent.png` と `evidence/README.md` の「MAUI サンプルの実行面」 | ✅ (注4) |

Scenario の中身の確認: 「表示中のプロパティ変更」は `GatewayScope.Connect(view).Reset()` で初期適用を切り離してから変更を撃ち、`Single<SetTheme>` で 1 回だけ届くことを見ており、初期適用と混ざらない形になっている。

Requirement 本文の SHALL / SHALL NOT:

| 契約 | 実装 | テスト | 状態 |
|---|---|---|---|
| facade は未指定の値を自前の既定値で埋めない (SHALL NOT) | 無変更 | `UnsetScrollIndicatorAndHeaderFooterBackgroundsCarryNothing` (3 つとも `null`) | ✅ |
| facade の公開面と bridge の wire 形式は変えない (SHALL) | `maui/` 配下の差分はテストファイル 1 本のみ (`ThemeAndCellStyleTests.cs`) | — | ✅ |

---

## 追加検査

### tasks.md との突き合わせ (虚偽チェック)

| タスク | 対応表との一致 | 状態 |
|---|---|---|
| 1.1 / 1.2 (iOS スクロールバー + 対称テスト + 行・offset 維持) | 実装・テストとも存在 | ✅ |
| 1.3 / 1.4 (Android 反映 + 波及の限定 + 検出力) | 実装・テストとも存在。review-002 Major の「反映処理を消すと落ちる」観測点は `内部 RecyclerView の Context がスクロールバー用の style を運んでいる` の追加で成立 | ✅ |
| 1.5 (夜間モードのつまみ追従 + overscroll 非保証のコメント) | `KsSettingsView.kt:334-` + `:86-87` のコメント | ✅ |
| 1.6 (両 OS の候補リスト、Android はホイールと分けたテスト) | `SheetScrollbarTest` / `ScrollIndicatorVisibleTests` | ✅ |
| 2.1 (利用者所有コンテンツの Context の回帰テスト) | `HostWrappedContextUserContentTest` | ✅ |
| 3.1 / 3.2 (iOS Header / Footer 背景 + 全 Scenario) | `AccessoryBackgroundColorTests` | ✅ |
| 4.1 (iOS 公開定数を形のまま透明に + 既存テストの追随) | `Theme.swift:284-290, 377, 379` / `ThemeDefaultColorAppearanceTests` 更新済み | ✅ |
| 4.2 (Android 既定パレット + 既存テストの追随) | `KsThemePalette.kt` / `KsSettingsViewDefaultsTest` 更新済み | ✅ |
| 4.3 (platform 間の定数比較テストの追随) | iOS `test_darkセットの生値が3platform共通の値である` と Android `KsSettingsViewDefaultsTest` の両側が新しい値へ追随済み | ✅ (注2) |
| 4.4 (samples の確認) | `samples/ios/KsSettingsViewSample/SampleTheme.swift` / `samples/android/.../SampleTheme.kt` / `samples/maui/KsSettingsView.Sample.Maui/SampleStyles.xaml` はいずれも Header / Footer 背景を明示済みで、変更不要。`evidence/README.md` の記述と一致 | ✅ |
| 4.5 (MAUI の facade / bridge テスト) | `ThemeAndCellStyleTests` に 4 件追加、実装は無変更 | ✅ |
| 5.1 (全テスト実行 + Swift 6 言語モードの記録) | `evidence/README.md` に 3 回分の実施記録 (error 0 件・`Package.swift` の shasum 一致) | ✅ |
| 5.2 (実機・エミュレータ確認) | 5.2b のみ `[ ]`。`evidence/README.md` の「取れていない確認」と一致 | ⚠️ オーナー確認待ち |

**未実装なのにチェック済みのタスクは無い。** review-002 Minor で指摘されていた tasks 5.2 と `evidence/README.md` の食い違いは、5.2 を項目分割して 5.2b だけ未了に戻す形で解消されている。

### 逆流検査 (足場の凍結)

change 一式が未コミットのため git の履歴では追えないので、ファイルの更新時刻で確認した。

- `specs/*/spec.md` と `proposal.md` はいずれも 2026-09-17 15:41:31 で同一
- 実装・テストの最終更新はいずれもそれより後 (`ios/Tests/...` 17:07、`maui/...Tests.cs` 17:07、`android/.../ScrollIndicatorVisibleTest.kt` 17:08、`android/.../KsSettingsView.kt` 17:20)

**デルタスペック確定後に足場を書き換えた形跡は無い。** なお `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift` の最終更新 (14:25) が spec の確定より前だが、これは「S 級として着手済み → M 級へ引き上げて提案を後付けした」経緯によるもので、`proposal.md` の Impact 「実装の現況」に記録されている。逆流ではない。

### 未記録乖離

**無し。** diff に含まれる変更はすべて、いずれかの Requirement / Scenario に対応している。`[付随修正]` に相当する、Requirement を持たない変更も見当たらない。

`kasane/decisions/core/0032-header-footer-background-default-transparent.md` と `kasane/decisions/core/index.md` は `proposal.md` の Why / Impact で起票が宣言されているアーティファクト側の変更であり、実装乖離ではない。

### テストの実行結果

| platform | コマンド | 結果 |
|---|---|---|
| iOS | `xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,...'` | **1046 件 / 0 失敗** (Bridge 166・Core 88・SwiftUI 98・TestSupport 7・UI 687。`** TEST SUCCEEDED **`) |
| Android | `./gradlew test` (モジュール別に再実行して結果 XML を集計) | **2902 件 / 0 失敗 / 0 error** (本体 1278 + bridge 173 を debug / release の 2 variant) |
| MAUI | `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` | **564 件 / 0 失敗 / 0 スキップ** |

本 change で追加・更新されたテストがいずれも実行されて成功していることを個別に確認した (iOS: `ScrollIndicatorVisibleTests` / `AccessoryBackgroundColorTests` / `DSLScrollIndicatorVisibleTests` / `ThemeDefaultColorAppearanceTests`、Android: `ScrollIndicatorVisibleTest` 9 件・`SheetScrollbarTest` 5 件・`HeaderFooterBackgroundDefaultTest` 3 件・`HostWrappedContextUserContentTest` 2 件・`DSLScrollIndicatorVisibleTest` 2 件・`KsSettingsViewDefaultsTest` 16 件、MAUI: `ThemeAndCellStyleTests` の 4 件)。

**実行環境に関する注記**: Android は最初の 3 回の全件実行が基盤エラー (Robolectric の `SandboxClassLoader` による `ClassNotFoundException`、`in-progress-results-generic.bin` の欠落、Gradle daemon の停止) で落ちた。`kasane/handbook/cross/test-execution.md` が「テストの失敗ではなく実行環境の問題」として挙げている形であり、同時に走っていた別ビルドを止めてモジュール別に実行し直したところ全件成功した。**テストの中身に起因する失敗ではない。**

---

## 注記

- **注1 (専用テストの無い SHALL NOT)**: 「iOS の回転ホイールにスクロールバーを表示しない」「横スクロールバーは対象にしない」は、実装が当該 API に一切触れていないことで成立しており (iOS の回転ホイールは `UIPickerView` / `UIDatePicker`、横インジケータへの代入は 0 件、Android の style は `vertical` のみ)、対応する Scenario も置かれていない。一致検証としては ✅ とするが、「実装がそこに触れていないこと」を固定するテストは無いため、将来の変更で静かに破れ得る点は記録しておく (Android の回転ホイールには `SheetScrollbarTest` の固定がある)。
- **注2 (platform 間の既定値の一致)**: 「この既定は相手 platform の既定と同じである」という SHALL は、両 platform のテストがそれぞれ自分側の期待値を literal で固定する形で担保されており、実行時に相手の値を読む仕組みは無い。これは本 change が持ち込んだ構造ではなく、`kasane/concepts/core/styling/style-resolution.md` が「3 面同値であることはテストが定数比較で固定する」と定めている既存の形であり、その両側が新しい値 (透明) へ追随している。
- **注3 (利用者所有コンテンツの「見た目を変えない」)**: この SHALL NOT を直接 (利用者 View の中のリストの描画状態で) 観測するテストは無い。ただし利用者所有コンテンツへ渡す Context がホストの Context そのものであること (`assertSame`) が固定されており、スクロールバー用のオーバーレイはその Context に乗らないため、契約は Context の同一性を通じて担保されている。
- **注4 (実行面の証跡)**: maui-bridge の Scenario「MAUI の実行面で native と同じ見た目になる」は、性質上テストではなく `evidence/` の実行面の証跡が対応物である。両 platform の静止画と、撮影のための一時変更を作業ツリーに残していない旨の記録がある。ただし iOS 側のスクロールバーそのものは静止画に写らないため、この Scenario の THEN のうち「iOS で縦スクロールバーが表示される」だけは `showsVerticalScrollIndicator` の観測テストと tasks 5.2b (オーナーの実機目視待ち) に委ねられている。
- **参考 (長命層の追随)**: `kasane/concepts/core/styling/style-resolution.md` の色ロール対応表は Header / Footer 背景を旧既定のまま記載している。これは `proposal.md` の Impact に「長命層: ... style-resolution.md (既定色の表) ほかの記述が変わる (蒸留で追随)」と記録済みであり、未記録乖離ではない。蒸留時の作業として残る。
