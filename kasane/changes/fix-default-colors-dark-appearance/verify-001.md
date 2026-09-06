# Verify 001: fix-default-colors-dark-appearance

- 判定: **VALID**
- 検証日: 2026-09-06
- 対象: 作業ツリーの未コミット差分全体 (`git diff HEAD` + 未追跡ファイル)
- デルタスペック: `specs/` 6 capability / 全 45 Scenario
- 合意済み差分の出典: `deviation.md` (乖離 7 項目 + 付随修正 5 項目)

## 1. 対応表

凡例: ✅ 一致 / ⚠️ deviation 記録済み (合意済みの差分) / ❌ 欠落・乖離

### settings-view-android-ui

#### ADDED Requirement: 色の未指定表現

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 既定の Theme はすべて未指定 | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt:88`-`121` (色 16 個すべて `Color = Color.Unspecified`) | `KsSettingsViewDefaultsTest`「引数なしの Theme は全ての色フィールドが未指定」/ `ThemeTest`「デフォルトコンストラクタは中立的な既定値を持つ」「cellTitleColor は既定で未指定」 | ✅ |
| CellStyle の未指定は Theme へ継承する | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellStyle.kt:33`-`51` (色 7 個を `Color.Unspecified` 既定へ)、`EffectiveStyle.kt:227` の `takeOrElse` 連鎖 | `EffectiveStyleResolutionTest`「effectiveTitleColor は Theme フォールバック」ほか 各ロールの CellStyle→Theme 継承 12 件 / `CellStyleTest`「デフォルトコンストラクタは全フィールド未指定」 | ✅ |
| 既定と同じ生値の明示は既定と等価でない | `Theme.kt:88` (data class 等価性) + `KsSettingsViewDefaults.kt:96` `resolvedFor` が明示値を素通し | `KsSettingsViewDefaultsTest`「既定と同じ生値を明示した Theme は既定 Theme と等価でなく外観で変わらない」 | ✅ |
| Unspecified を明示的に渡して既定へ戻す | `KsSettingsViewDefaults.kt:96` `resolvedFor` (`takeOrElse`) | `KsSettingsViewDefaultsTest`「Unspecified を明示的に渡すと既定へ戻る」 | ✅ |

#### ADDED Requirement: ライブラリ既定色の light / dark セット

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| light セットは現行の既定色と同値 | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsThemePalette.kt:19` (`Light`) / `KsSettingsViewDefaults.kt:35` (`lightTheme()`) | `KsSettingsViewDefaultsTest`「lightTheme はライト外観の既定色を持つ」 | ✅ |
| dark セットは承認モックの対応表と同値 | `KsThemePalette.kt:62` (`Dark`) / `KsSettingsViewDefaults.kt:49` (`darkTheme()`) | `KsSettingsViewDefaultsTest`「darkTheme は色ロール対応表の dark 列と同値」「どちらのセットもフォールバック契約の色は未指定のまま」 | ⚠️ 対応表 10 ロールは一致。title / description は factory が埋めない (deviation 案 B'。テスト「どちらのセットもタイトルと説明文を埋めず後段の解決へ委ねる」が明示的に検証) |
| 外観で factory の結果が切り替わる | `KsSettingsViewDefaults.kt:67` (`theme(darkTheme:)`) / `:71` (`@Composable theme()`) | `KsSettingsViewDefaultsTest`「theme は外観で light dark を選ぶ」「light と dark は同じロールで異なる値を持つ」 | ✅ |
| (規範) `Theme` companion の色定数を公開しない | 色定数は削除、生値は `KsThemePalette` (internal) へ。icon の 2 定数は `Theme.kt:126`-`129` に据え置き | リポジトリ全走査で `DEFAULT_*_COLOR` / `SYSTEM_BLUE_ARGB` の残存なし | ⚠️ design は「internal 化」、実装は「削除」(deviation 記録済み。spec の SHALL NOT は満たす) |

#### ADDED Requirement: 未指定色の外観解決

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Theme を渡さず夜間モードで表示する | `KsSettingsViewDefaults.kt:96` `resolvedFor` + `ui/KsSettingsView.kt:269`/`583`/`783` (解決点 3 経路) | `ThemeAppearanceResolutionTest`「Theme を渡さない View は夜間モードでダークの既定色で描画される」 | ✅ |
| ライトの既定は本 change 前と同じ | 同上 | `ThemeAppearanceResolutionTest`「Theme を渡さない View はライトの既定色で描画される」/ `EffectiveStyleResolutionTest`「effectiveTitleColor はライトの既定へフォールバック」 | ⚠️ list 下地・Cell 背景・separator・選択色・accent・disabled・Header / Footer は本 change 前と同値。Cell title / valueText のみ #1D1B20 → #000000 (deviation 記録済みのオーナー承認済み変化) |
| 一部だけ上書きした Theme の残りが追随する | `KsSettingsViewDefaults.kt:96` (`takeOrElse` で明示値を素通し) | `ThemeAppearanceResolutionTest`「一部だけ上書きした Theme は残りが夜間の既定へ追随する」 | ✅ |
| 選択面とカレンダーの選択面も解決済み値で描かれる | `ui/KsSettingsView.kt:1008` (`EffectiveStyle.from(internalTheme, …, resolvedDarkTheme)`)、`PickerDialogColors.kt`、`DatePickerCellViewHolder.kt:35` | `ThemeAppearanceResolutionTest`「選択面の配色は夜間の既定セットになる」「カレンダーの選択面の配色は夜間の既定セットになる」 | ✅ |
| title を明示した Theme の valueText は title へフォールバックする | `ui/EffectiveStyle.kt:274` (`valueText → theme.cellValueTextColor → theme.cellTitleColor → 外観既定`) | `KsSettingsViewDefaultsTest`「title を明示した Theme の valueText は title へフォールバックする」「全段未指定の valueText は外観のタイトル既定へ落ちる」 | ✅ |
| ButtonCell の title 既定は外観で選ばれ accent と独立 | `ui/EffectiveStyle.kt:435`/`460` (4 段解決の最終段が `KsThemePalette.buttonTitle(darkTheme)`)、`ui/ButtonCellViewHolder.kt:89`-`99` | `ThemeAppearanceResolutionTest`「ButtonCell のタイトル既定はライトで accent を明示しても変わらない」「同・夜間」「Compose 経路と View 経路で一致する」/ `DSLDefaultThemeAppearanceTest` 5 件 / `BasicCellsTest` | ✅ |
| 利用者が読む Theme は解決前のまま | `ui/KsSettingsView.kt:233` (`get() = themeBacking`)、`:565` `setRootStructureOnly` (構造更新が `themeBacking` を書き戻さない) | `ThemeAppearanceResolutionTest`「利用者が読む Theme は解決前のまま」 | ✅ |
| (規範) 描画時に `Unspecified` が ARGB へ変換されない | `KsSettingsViewDefaults.kt:96`、`SectionBoxMetrics.kt:125` (`takeOrElse`) | `ThemeAppearanceResolutionTest`「解決済み Theme は実値変換される色をすべて指定済みで持つ」「実効スタイルの全色は Unspecified の ARGB 変換にならない」「placeholder は解決後も未指定でホスト既定へ委ねる」 | ✅ |

#### ADDED Requirement: 夜間モード変更時の未指定色の再解決

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 表示中に夜間モードへ切り替わる | `ui/KsSettingsView.kt:319` `onConfigurationChanged` → `applyThemeInternal(themeBacking.resolvedFor(darkTheme))` | `ThemeAppearanceResolutionTest`「表示中に夜間モードへ切り替わると未指定色が追随し identity は保たれる」 | ✅ (視覚証跡: `ui/brief.md`「再撮影 (2026-09-06)」/ `ui/verification/android-native-unify-common-fields-live-*.png`) |
| 明示指定色は切替後も変わらない | 同上 (`resolvedFor` が明示値を素通し) | `ThemeAppearanceResolutionTest`「明示指定色は夜間モードへの切替後も変わらない」 | ✅ |
| 構造更新を挟んでも未指定色は追随し続ける | `ui/KsSettingsView.kt:565`/`575` (`setRootStructureOnly` / `applyRootDirect(userTheme = null)`)、`applyDiff` の Full・Section 置換・可視性切替・`resyncFromStore` の 4 呼出点 | `ThemeAppearanceResolutionTest`「構造更新を挟んでも未指定色は夜間へ追随する」 | ✅ |
| 表示中の選択面は次回表示から追随する | `ui/KsThemedContext.kt:34`/`104` (生成時の夜間モードと現在値を照合して同梱テーマ付き Context を作り直す) | `ThemeAppearanceResolutionTest`「選択面は開き直すと新しい外観の配色になる」「開いたままの選択面は外観が切り替わっても再配色されない」「選択面を開き直すとテーマ属性も夜間側で解決される」「行の Context から得る同梱テーマ付き Context は外観の切替後に作り直される」 | ✅ (視覚証跡: `ui/verification/android-native-picker-sheet-reopen-*.png` / `android-native-datepicker-calendar-reopen-*.png`) |
| 夜間モード以外の Configuration 変更では再適用しない | `ui/KsSettingsView.kt:319`-`324` (`if (darkTheme == resolvedDarkTheme) return`) | `ThemeAppearanceResolutionTest`「夜間モードが変わらない構成変更では Theme を再適用しない」 | ✅ |
| (規範) attach 時に解決時と外観が異なれば再解決する | `ui/KsSettingsView.kt:307`/`793` (`reresolveThemeIfAppearanceChanged`) | `ThemeAppearanceResolutionTest`「attach 時に解決時と外観が違えば再解決する」 | ✅ |
| (再適用の随伴) 行のテーマ属性由来の色も新しい外観で引き直す | `ui/EntryCellViewHolder.kt:366` `syncThemeHintTextColors`、`ui/SwitchCellViewHolder.kt:164` (`ksThemedContext()` から attr を引く) | `ThemeAppearanceResolutionTest`「placeholder 未指定の行の hint 色は夜間モードへの切替後に引き直される」「Switch のオフ色は夜間モードへの切替後に引き直される」 | ✅ (「表示中の行 … を再適用する」の実現に必要な随伴実装。視覚証跡は `ui/brief.md`「再撮影 (2026-09-06)」の 2 表) |

#### ADDED Requirement: Compose 入口の既定 Theme は外観で選ばれる

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 夜間モードの composition では dark セットが既定になる | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposable.kt:68` (`theme: Theme = KsSettingsViewDefaults.theme()`) | `DSLDefaultThemeAppearanceTest`「theme を渡さない DSL は夜間モードでダークの既定セットを Store へ流す」「同・ライト」 | ✅ |

#### MODIFIED Requirement: Theme の Section 装飾4属性

変更点は `sectionBorderColor` の `Color?` → `Color` (未指定表現の `Unspecified` 化) のみ。他 3 属性・余白幾何・clamp の契約は本 change 前と同一で、既存テストがそのまま指標。

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 未指定の Theme で Modern を表示する | `ui/SectionBoxMetrics.kt:125` (`takeOrElse { DEFAULT_BORDER_COLOR }`) | `ModernSectionDecorationTest`「ボーダー未指定なら描画しない」/ `InitialThemeDecorationTest` | ✅ |
| 指定値が箱の描画へ反映される | `ui/SectionBoxMetrics.kt` | `ModernSectionDecorationTest`「箱の左右は sectionMargin の水平成分だけ内側に入る」「ボーダーは separator と Cell 背景より後に描かれ最前面に来る」 | ✅ |
| 実行時の Theme 変更が装飾へ反映される | `ui/KsSettingsView.kt` `applyThemeInternal` (ItemDecoration 再構築) | `ModernSectionDecorationTest`「実行時の Theme 変更で角丸が再解決される」 | ✅ |
| sectionMargin は Header / Footer を含む Section 単位を包む | `ui/SectionBoxMetrics.kt` | `ModernSectionDecorationTest`「sectionMargin は Header の上と Footer の下に入り Header と箱の間には入らない」 | ✅ |
| 負の成分は 0 として扱う | `ui/SectionBoxMetrics.kt` | `ModernSectionDecorationTest`「負の寸法を持つ Theme でも 0 として描画され例外を出さない」「非有限の寸法…」 | ✅ |
| Unspecified の枠線色は透明として描かれる | `ui/SectionBoxMetrics.kt:125` | `ThemeAppearanceResolutionTest`「未指定の枠線色は解決後も透明として描かれる」/ `ThemeTest`「Section 装飾 4 属性の既定はすべて未指定」 | ✅ |

### settings-view-ios-ui

#### ADDED Requirement: 既定色の外観追随 (iOS)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Theme を渡さずダーク外観で表示する | `ios/Sources/KsSettingsViewUI/Theme.swift:262`-`317` (public static 既定色 10 個を `UIColor(dynamicProvider:)` 化)、`:361`-`379` (dark 生値) | `ThemeDarkAppearanceRenderingTests`「Themeを渡さないlistはダークでdarkセットの色で描かれる」 | ✅ |
| ライト外観の既定色は変わらない | 同上 (light 側は本 change 前の literal を維持) | `ThemeDefaultColorAppearanceTests`「既定色のライト解決値が固定値と一致する」 | ✅ |
| 一部だけ上書きした Theme の残りが追随する | `Theme.swift:146`-`151` (`init` 既定引数が dynamic 定数を参照) | `ThemeDarkAppearanceRenderingTests`「表示中に外観をダークへ切り替えると既定色が描き直される」/ `ThemeDefaultColorAppearanceTests`「利用者のdynamicな色はその色自身のdark値になる」 | ✅ |
| 明示指定した色は外観で変わらない | ライブラリは利用者の色を置き換えない (解決関数を持たない) | `ThemeDarkAppearanceRenderingTests`「明示指定した固定色は外観切替で変わらない」 | ✅ |
| 利用者の dynamic な色はその色自身の dark 値になる | 同上 | `ThemeDefaultColorAppearanceTests`「利用者のdynamicな色はその色自身のdark値になる」 | ✅ |
| 表示中の外観切替に既定色が追随する | `UIColor` の trait 解決 (UIKit 標準経路) | `ThemeDarkAppearanceRenderingTests`「表示中に外観をダークへ切り替えると既定色が描き直される」 | ✅ |
| 既定色の生値が承認モックの対応表と一致する | `Theme.swift:351`-`379` | `ThemeDefaultColorAppearanceTests`「既定色のダーク解決値がdarkセットの値と一致する」「darkセットの生値が3platform共通の値である」「Cellのtitleとdescriptionの既定はシステム色のまま」 | ⚠️ ButtonCell title は `.systemBlue` 維持、light 列 hex は現行 float 値との丸め差 — いずれも deviation 記録済み。dark 10 ロールは対応表 hex と厳密一致 |
| 既定 Theme 同士の等価性 | `Theme.swift:146` (`init` 既定引数が同一の static インスタンスを参照) | `ThemeDefaultColorAppearanceTests`「既定Theme同士と既定定数を明示したThemeは等価」/ `ThemeRenameTests`「backgroundColor_既定値はdefaultBackgroundColor」 | ✅ |
| 固定色で明示した Theme は既定と等価でない | 同上 | `ThemeDefaultColorAppearanceTests`「固定色で明示したThemeは既定と等価でなくダークでも変わらない」 | ✅ |

`init` の `cellBackgroundColor` 既定引数が `.white` (リテラル) から新設 public 定数 `Theme.defaultCellBackgroundColor` (`Theme.swift:306`) に変わった点は tasks 5.1 が明示する実装手段であり、既存の public 定数の型・名前・可視性は不変。

#### ADDED Requirement: Section 装飾の枠線色の外観再解決

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| dynamic な枠線色が外観切替に追随する | `ios/Sources/KsSettingsViewUI/SectionBoxDecorationView.swift:19`/`31`/`65`/`71` (解決前 `UIColor` を保持し `registerForTraitChanges` / iOS 16 は `traitCollectionDidChange` で `cgColor` を再解決) | `ThemeDarkAppearanceRenderingTests`「dynamicなsectionBorderColorが外観切替で再解決される」 | ✅ |

tasks 5.3 (`cgColor` を取る箇所の再確認) は枠線と `KsCheckBoxView` 以外に該当なしを確認済み、5.4 (Swift 6 言語モード) はビルド成功で担保。

### maui-bridge

#### ADDED Requirement: 未指定色の外観既定への解決

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| MAUI で色を設定しない SettingsView をダーク外観で表示する | `android/kssettingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeTheme.kt:193` (未指定を `Color.Unspecified` へ)、iOS 側は `Theme.swift` の dynamic 既定 | 視覚照合: `ui/brief.md`「一致した点 (ダーク)」の MAUI iOS / MAUI Android 列 (`ui/verification/maui-ios-*-dark.png` / `maui-android-*-dark.png`) | ✅ |
| 設定した色は外観で変わらない | 同上 (明示 ARGB は素通し) | `KsBridgeThemeTest`「setTheme の一部明示は明示分だけ保たれ残りは未指定になる」+ 視覚照合 `ui/brief.md`「再実証 1.」の表 | ✅ |
| AppThemeBinding の値が native まで届く | facade → snapshot → bridge DTO → native `applyTheme` の既存経路 | 実物確認 (tasks 0.1 の spike と「0.1 の再実施」)。証跡: `evidence/maui-ios-visibility-appthemebinding-{light,dark}.png` / `evidence/maui-android-norecreate-visibility-appthemebinding-{light,dark}.png` / `evidence/maui-android-norecreate-instance-identity.txt` | ✅ 承認ゲート通過。初回 spike の「Activity 再生成なし」の主張は訂正済みで、`Recreate()` を外したビルドでの再実施が同一 Activity / 同一 View (identity hash 一致) を実証している |
| Android bridge は未指定を Unspecified として渡す | `KsBridgeTheme.kt:193` | `KsBridgeThemeTest`「setTheme の未指定項目は Theme 側の未指定になる」「Section 装飾の未指定項目は…」「cellPlaceholderColor 未指定は…」 | ✅ |
| Android bridge の CellStyle 変換は明示 ARGB を保ち未指定だけ Unspecified にする | `android/kssettingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeCellStyle.kt:81` | `KsBridgeCellConversionTest`「色が全て null の style は全ての色が未指定の CellStyle になる」「一部だけ明示した style は明示分だけ保たれ残りは未指定になる」「style 輸送値の全 13 項目が対応する CellStyle 項目へ写される」 | ✅ |

DTO の wire 形式 (項目・型・null の意味) は `KsBridgeTheme` / `KsBridgeCellStyle` の public プロパティに変更なしで維持。

### samples-android

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| Theme を渡さない画面をダークで開く | Sample 側の変更なし (ダーク用の色を補っていない。`git status` 上 `samples/android` の差分は `SampleTheme.kt` の 1 ファイルのみ) | 視覚照合: `ui/verification/android-native-{store,dsl,unify-common-fields,visibility,section-decoration}-dark.png` と `ui/brief.md`「再照合」の実測表 (7 ロールすべて approved.png dark 列と一致) | ✅ |
| Section 装飾デモの既定の枠線色 | `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/SampleTheme.kt:253` (`sectionBorderColor: Color = Color.Unspecified`) | 本体側 `ThemeAppearanceResolutionTest`「未指定の枠線色は解決後も透明として描かれる」+ 視覚照合 `android-native-section-decoration-dark.png` | ✅ |

light / dark プリセットの生値は本 change 前と同じ (diff は doc コメント 1 行と既定引数の型のみ)。

### samples-ios

| Scenario | 実装 | 証跡 | 状態 |
|---|---|---|---|
| Theme を渡さない画面をダークで開く | Sample 側の変更なし (`git status` に `samples/ios` の差分なし) | `ui/verification/ios-native-{store,dsl,unify-common-fields,visibility,section-decoration}-dark.png`、`ui/brief.md`「一致した点 (ダーク)」iOS Native 列 | ✅ |

### samples-maui

| Scenario | 実装 | 証跡 | 状態 |
|---|---|---|---|
| Theme を渡さない画面をダークで開く | Sample 側の変更なし (`git status` に `samples/maui` の差分なし。spike の一時改変は復元済み) | `ui/verification/maui-ios-*-dark.png` / `maui-android-*-dark.png`、`ui/brief.md`「一致した点 (ダーク)」MAUI 2 列 | ✅ |

## 2. 追加検査

### tasks.md の完了状況

- 0.1〜8.5 の 34 タスクすべてチェック済み。未チェックは 9.1 (蒸留への申し送り) のみで、これは実装フェーズ完了後に ksn-distill が実行するタスクのため未完了で正しい
- **虚偽チェックなし**: 上の対応表で全タスクの成果物 (実装・テスト・証跡ファイル) の実在を確認した。特に検証したもの:
  - 1.3 の「`DEFAULT_*` の色定数は internal 化」は実装では削除 → deviation 記録済み (spec の SHALL NOT は満たす)
  - 5.3 / 5.4 / 6.2 のような「確認する」タスクは、それぞれ `cgColor` 参照箇所の走査結果・ビルド成功・`git status` の差分不在で裏付けられる
  - 8.1〜8.5 の視覚照合は `ui/verification/` に 44 ファイル、`evidence/` に 9 ファイルが実在し、`ui/brief.md` に照合記録・再照合・再実証・再撮影の 4 節が対応する

### 逆流検査 (足場の凍結)

- `git log -- specs/ proposal.md design.md` は起票コミット (`ff64fc6`) 1 件のみ。実装期間中の書き換えなし
- `git diff HEAD` の change 配下の差分は `tasks.md` (spike 結果の注記・チェック) と `ui/brief.md` (照合記録) のみで、いずれも実装中に育てる文書。`deviation.md` は新規追加
- **逆流なし**

### 未記録乖離

❌ はゼロ。deviation.md に記録のない乖離は検出しなかった。

### 付随修正の突き合わせ

deviation.md「付随修正」5 件はいずれも diff 中に実在を確認した (`SYSTEM_BLUE_ARGB` 削除 / `SectionBoxMetrics` の `takeOrElse` / `EntryCellViewHolder` の `isSpecified` / Android の旧契約 doc コメント・テスト名の掃き取り / iOS `KsSettingsViewController.swift:1164` のコメント)。

一方、次の 2 箇所は Scenario にも付随修正の記載にも直接対応しない編集で、**記録の追加を提案する** (挙動への影響はなく、いずれも本 change が正当に書き換えている doc ブロック内の隣接行):

- `ios/Sources/KsSettingsViewUI/Theme.swift:22` — public doc コメントから `（core/ADR-0009）` を削除
- `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellStyle.kt:17`-`18` — public doc コメントから `core/ADR-0009` 参照を外し自己完結の説明へ書き換え

いずれも handbook cross/comment-policy.md の「公開メンバーの doc コメントには ADR ID を含む内部用語を使わない」に沿う方向の修正であり、規範への違反ではない。**見立て**: 実装を戻す必要はなく、deviation.md の付随修正へ 1 行追記するのが妥当 (同種の掃き取りは既に 2 件記録されており、記録の粒度を揃えるだけ)。なお同じ規約に触れる `ui/RadioCell.kt:17` の `（core/ADR-0009）` は本 change の担当範囲外の既存記述として残っており、掃き取りが網羅的でない点も申し送りに値する。

### テスト実行 (handbook cross/test-execution.md)

いずれも本検証で実際に実行した。

| platform | コマンド | 結果 |
|---|---|---|
| Android | `./gradlew test --rerun-tasks` (`124 actionable tasks: 124 executed`) | **2810 件 / 失敗 0** (kssettingsview debug 1235・release 1235、kssettingsview-bridge debug 170・release 170)。件数は `build/test-results/test{Debug,Release}UnitTest/TEST-*.xml` の `tests` / `failures` + `errors` を合算 |
| iOS | `xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,…'` | **1021 件 / 失敗 0** (`** TEST SUCCEEDED **`)。バンドル集計行の合算: UITests 666 / Bridge 166 / SwiftUI 94 / Core 88 / TestSupport 7 |

初回の Android 実行では `:kssettingsview-bridge:testDebugUnitTest` のテストワーカーが `java.io.EOFException` / `NoSuchFileException` で異常終了したが、原因は実行機のメモリ逼迫と `kssettingsview-bridge/build/` の中間生成物の破損 (実行環境側の問題)。build ディレクトリを捨てて再実行したところ全件成功し、その後の `--rerun-tasks` 全件実行でも再現しない。**本 change の差分に起因する失敗ではない。**

MAUI facade テストは本 change の差分に含まれないため未実行 (`maui/` に差分なし)。

### UI 変更の承認モックと合意済み妥協

- `ui/brief.md`「承認モック」節に `mock/plan-a.html` 採用・2026-09-05 オーナー承認・`approved.png` 内の色ロール対応表が生値の正であることが記録済み
- 承認モックの dark 対応表 11 ロールと `KsThemePalette.Dark` / `Theme.swift` の dark 生値を突き合わせ、全ロール一致を確認した (ButtonCell title の iOS `.systemBlue` は対応表自身の注記どおり)
- 合意済み妥協の記録: iOS の `.secondaryLabel` / `.systemBlue` 合成値、Android ライトの title #1D1B20 → #000000、初回照合時の ButtonCell 乖離とその解消がすべて `ui/brief.md` と `deviation.md` に残っている
- 個人要素の確認は撮影 4 回分すべてについて記録あり。`evidence/maui-android-norecreate-instance-identity.txt` は `scripts/log-sanitize.py` を通して置換対象なしを確認済みと記録

## 3. 判定

**VALID**

全 45 Scenario が「✅ 一致」または「⚠️ deviation 記録済み」で、❌ はゼロ。tasks.md の虚偽チェックなし、足場の逆流なし、Android 2810 件 / iOS 1021 件とも失敗 0。

判定に影響しない申し送り 1 件: 上の「付随修正の突き合わせ」に挙げた public doc コメントの ADR 参照除去 2 箇所を deviation.md の付随修正へ追記すること (挙動への影響なし)。
