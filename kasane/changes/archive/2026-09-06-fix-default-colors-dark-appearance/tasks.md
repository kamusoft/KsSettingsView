# Tasks: fix-default-colors-dark-appearance

見た目の正は `ui/mock/approved.png` (色ロール対応表 = dark セットの生値の正)。挙動の正は `specs/`。設計は `design.md` (Decision 1〜9)。

## 0. spike (先頭で実施。崩れたら探索へ戻す)
- [x] 0.1 MAUI の `AppThemeBinding` を SettingsView の色プロパティに使ったとき、外観切替で facade → snapshot → bridge → native `applyTheme` まで届くかを iOS / Android 各 1 面で実物確認し、結果を本 tasks の注記に残す。**届かなければ以降のタスクに進まず探索へ戻す** (承認ゲート) (→ Requirement: 未指定色の外観既定への解決 / Scenario: AppThemeBinding の値が native まで届く)

  **結果: 届く (MAUI iOS / MAUI Android とも)。承認ゲート通過。**

  - 確認方法: MAUI Sample の isVisible デモ画面 (`samples/maui/KsSettingsView.Sample.Maui/Pages/VisibilityDemoPage.xaml`、色を一切指定していない画面) の `<ks:SettingsView>` に、`CellBackgroundColor` と `HeaderTextColor` の 2 属性を `{AppThemeBinding Light=..., Dark=...}` で一時的に付け、light 値と dark 値に明確に判別できる色 (淡いピンク / 濃紺) を置いた。ログ出力の追加は不要だった (画面の見た目で判定できたため)
  - MAUI Android (Emulator `ksn_custcell_api35` / Android 15): ライトで light 値が Cell 背景と Section Header 文字に描かれることを確認。**画面を表示したまま**端末の夜間モードを on にすると、その場で dark 値へ描き替わった ~~(Activity 再生成なし。`ConfigChanges.UiMode` 宣言により View が生き残る構成)~~ — **訂正 (2026-09-05)**: 初回 spike 時の Sample は `MainActivity.OnConfigurationChanged()` で `Recreate()` を呼んでいたため、この観測は「同一 View への表示中の追随」を証明していなかった。下の「0.1 の再実施」を正とする
  - MAUI iOS (iPhone 17 Pro Simulator / iOS 26.0): 同じ画面でライトの light 値を確認。**画面を表示したまま** Simulator の外観をダークにすると、その場で dark 値へ描き替わった
  - よって facade の `Color?` プロパティ変更 → `ApplyTheme()` → snapshot (`KsThemeSnapshot`) → bridge DTO → native `applyTheme` の経路は、両 OS とも外観切替をきっかけに最後まで通っている
  - 一時改変は確認後に戻し、`git status` で `samples/` に差分が残っていないことを確認済み。両 OS の Simulator / Emulator には改変前のビルドを入れ直した
  - 証跡: `evidence/maui-ios-visibility-appthemebinding-light.png` / `evidence/maui-ios-visibility-appthemebinding-dark.png` / `evidence/maui-android-visibility-appthemebinding-light.png` / `evidence/maui-android-visibility-appthemebinding-dark.png` (dark はいずれも画面を表示したまま外観を切り替えた後)

  **0.1 の再実施 (2026-09-05、MAUI Android のみ)**

  独立レビューの指摘 (`second-opinion-code-001.md` [Major] 2 件目) を受け、`samples/maui/KsSettingsView.Sample.Maui/Platforms/Android/MainActivity.cs` の `OnConfigurationChanged()` の `Recreate()` を一時的にコメントアウトしたビルドで、MAUI Android の spike をやり直した。結果は**届く**で変わらず、承認ゲートの判定は維持する。MAUI iOS は Activity 再生成の概念が無く、初回 spike の結果がそのまま有効なので再実施していない。

  - (a) 色プロパティ未設定の画面「共通フィールド統合デモ」を表示した状態で端末の夜間モードを on にすると、**同一 Activity・同一 View のまま** dark の既定色へ描き替わった (list 下地 #FFFFFF → #000000、Cell 背景 #FFFFFF → #1C1C1E)
  - (b) 同一インスタンスの根拠: `adb shell dumpsys activity top` の `Local Activity <identityHashCode>` と View 階層の `KsSettingsView{...}` / `RecyclerView{...}` が切替の前後で一致した (Activity `329c446` / KsSettingsView `5085ecb` / RecyclerView `f916cca`)。同じ dump に写るランチャの Activity は同じ切替で別値に変わっており、識別子が再生成で変わること自体は確認できている
  - (c) 明示値も届く: 「isVisible デモ」の `CellBackgroundColor` / `HeaderTextColor` に `{AppThemeBinding}` を一時付与し、表示中の切替で Cell 背景 #FFD9E6 → #102A54、Section Header 文字 #C2185B → #7FD4FF と、light 値から dark 値へその場で描き替わった。この画面でも Activity `329c446` / KsSettingsView `3b4e2a…` / RecyclerView `2a9f242` は切替の前後で同一で、同画面の未指定色 (list 下地) も #FFFFFF → #000000 へ追随した
  - よって facade の `Color?` プロパティ変更 → `ApplyTheme()` → snapshot → bridge DTO → native `applyTheme` の経路は、**Activity を再生成しないホストでも**外観切替をきっかけに最後まで通る
  - 一時改変 (`Recreate()` のコメントアウトと `AppThemeBinding` の付与) は確認後に戻し、`git diff -- samples/maui` が空であることを確認したうえで改変前のビルドを Emulator に入れ直した
  - 証跡: `evidence/maui-android-norecreate-unify-common-fields-light.png` / `evidence/maui-android-norecreate-unify-common-fields-dark.png` (a) / `evidence/maui-android-norecreate-instance-identity.txt` (b) / `evidence/maui-android-norecreate-visibility-appthemebinding-light.png` / `evidence/maui-android-norecreate-visibility-appthemebinding-dark.png` (c)
  - なお現行 Sample の `Recreate()` 自体は据え置き (proposal Non-Goals)。本 spike が示すのは「ライブラリ側の経路は再生成なしでも通る」ことであって、Sample が再生成をやめるべきという結論ではない

## 1. Android 本体: 型と既定セット
- [x] 1.1 `Theme` の色フィールドを全部 `Color` 型・既定 `Color.Unspecified` にする (固定値既定 10 個と `Color?` 6 個。色以外は据え置き) (→ Requirement: 色の未指定表現)
- [x] 1.2 `CellStyle` の色フィールド 7 個を `Color` 型・既定 `Color.Unspecified` にする (→ Requirement: 色の未指定表現)
- [x] 1.3 internal なパレット (light / dark の用途名ロール) を新設し、値は approved.png の対応表に一致させる。`DEFAULT_*` の色定数は internal 化 (icon の定数は据え置き) (→ Requirement: ライブラリ既定色の light / dark セット)
- [x] 1.4 `KsSettingsViewDefaults` に `lightTheme()` / `darkTheme()` / `theme(darkTheme:)` / `@Composable theme()` を実装する (→ Requirement: ライブラリ既定色の light / dark セット)
- [x] 1.5 `Theme` の Section 装飾 4 属性の `sectionBorderColor` を `Unspecified` 契約に合わせ、透明解決を維持する (→ MODIFIED Requirement: Theme の Section 装飾4属性)

## 2. Android 本体: 解決点と消費者
- [x] 2.1 未指定を現在の外観の既定セットで埋める internal な解決関数 (`Theme.resolvedFor(nightMode)` 相当) を実装し、夜間判定は `ksThemedContext()` と同じ Configuration の uiMode を源にする (→ Requirement: 未指定色の外観解決)
- [x] 2.2 `KsSettingsView` が利用者の Theme (`themeBacking`) と解決済み Theme (`internalTheme`) を分けて持ち、adapter・`RecyclerView` 背景・Classic / Modern の ItemDecoration・Section / Root Header・Footer の ViewHolder・`PickerSelectionSheet`・`resolveDatePickerDialogColors` が解決済み Theme だけを読むようにする。`setRootDirect` / `theme` setter / Store 経路の 3 経路とも解決を通す。構造更新経路 (`applyDiff` の Full diff・Section 置換・可視性変更・Store 再同期) が `themeBacking` を書き戻さないよう `setRootDirect` の Theme 取り込みを利用者 Theme の経路から分ける (→ Requirement: 未指定色の外観解決 / Scenario: 構造更新を挟んでも未指定色は追随し続ける)
- [x] 2.3 `EffectiveStyle.from` を `takeOrElse` (CellStyle → 解決済み Theme) に書き換え、`resolveDefaultTitleColor` と `titleColorIsExplicit` を削除する。valueText / hint / placeholder の既存フォールバック順は維持する。ButtonCell の title 既定 (`effectiveButtonTitleColor` / `effectiveButtonTitleColorArgb` の最終段) は外観 (`darkTheme`) を受けて internal パレットの ButtonCell ロールから選ぶ (→ Requirement: 色の未指定表現 / 未指定色の外観解決 / Scenario: ButtonCell の title 既定は外観で選ばれ accent と独立)
- [x] 2.4 `theme` プロパティと Store の `theme` が利用者の Theme (解決前) を返すことを保つ (→ Scenario: 利用者が読む Theme は解決前のまま)

## 3. Android 本体: 外観変更の再解決と Compose 入口
- [x] 3.1 `KsSettingsView.onConfigurationChanged` で夜間モードの変化を検出し、`themeBacking` から再解決して既存の Theme 更新経路 (背景・adapter の Theme 差し替え・payload 再 bind・ItemDecoration 再構築) で再適用する。夜間モード以外の変更では再適用しない (→ Requirement: 夜間モード変更時の未指定色の再解決)
- [x] 3.2 window への attach 時に解決時の夜間モードと照合し、異なれば再解決する (→ Scenario: 表示中に夜間モードへ切り替わる / attach)
- [x] 3.3 DSL 方式 Composable の `theme` 既定値式を `KsSettingsViewDefaults.theme()` にする (→ Requirement: Compose 入口の既定 Theme は外観で選ばれる)

## 4. Android bridge
- [x] 4.1 `KsBridgeTheme.resolve()` / `KsBridgeCellStyle` の変換を `KsBridgeColor.color(x) ?: Color.Unspecified` に単純化し、`Theme()` の既定を埋める処理を撤去する (→ Requirement: 未指定色の外観既定への解決 (maui-bridge))

## 5. iOS 本体
- [x] 5.1 既定色の `public static let` 10 個と `init` の `cellBackgroundColor` 既定引数 (新設 `defaultCellBackgroundColor`) を `UIColor(dynamicProvider:)` の light = 現行値 / dark = approved.png の値の対にする。dark の生値は internal 定数として持つ。可視性・名前・型は不変 (→ Requirement: 既定色の外観追随 (iOS))
- [x] 5.2 `SectionBoxDecorationView` が最後の枠線色を保持し、`registerForTraitChanges` (iOS 16 は `traitCollectionDidChange`) で `layer.borderColor` を再解決する (→ Requirement: Section 装飾の枠線色の外観再解決)
- [x] 5.3 `cgColor` を取る箇所が枠線と `KsCheckBoxView` 以外に無いことを再確認し、あれば同じ形で再解決を入れる (→ design.md Risks)
- [x] 5.4 handbook ios/swift6-language-mode-check.md の手順で Swift 6 言語モード適合を確認する

## 6. samples
- [x] 6.1 Android `SampleTheme` の `null` 渡し (`sectionBorderColor` 等) を `Color.Unspecified` に追随させ、light / dark プリセットの値は変えない (→ Requirement: Sample Theme の未指定色は Unspecified で表す)
- [x] 6.2 3 面の Theme を渡さない画面にダーク用の色を補っていないことを確認する (文言・構成の変更なし) (→ Requirement: Theme を渡さない画面のライブラリ既定色の外観追随 (iOS / Android / MAUI))

## 7. tests
- [x] 7.1 Android: `Theme()` の全色が `Unspecified`、`lightTheme()` が現行既定と同値、`darkTheme()` が approved.png の対応表と同値で valueText / hintText / placeholder / `sectionBorderColor` だけ `Unspecified`、title を明示した Theme の valueText が title へフォールバックする、`theme(darkTheme:)` の切替 (→ Requirement: 色の未指定表現 / ライブラリ既定色の light / dark セット)
- [x] 7.2 Android: 夜間 / 非夜間の Configuration で `KsSettingsView` を表示し、行の実効 style・背景・separator・Header / Footer・選択面・カレンダー dialog の配色が該当セットの値になる。一部上書きの残りが追随し、明示指定は変わらない。ライトの title 実効値が本 change 前と一致する (→ Requirement: 未指定色の外観解決)
- [x] 7.3 Android: `onConfigurationChanged` で夜間モードが変わると再適用され identity が維持される。向きだけの変更では再適用しない。attach 時の照合。一部上書きの Theme で Full diff・Section 置換・可視性変更を適用した後の夜間切替でも未指定色が追随する (回帰)。detach / attach の復元経路でも同じ。表示中の選択面は閉じて開き直すと新しい外観になる (→ Requirement: 夜間モード変更時の未指定色の再解決)
- [x] 7.4 Android: 実値変換点 (`toArgb()` 等) へ到達する時点で `Unspecified` が残らないことの検査。sentinel を理解するフォールバック経路 (valueText → title、hint → accent、placeholder → ホスト既定、`sectionBorderColor` → 透明) は例外として列挙し、その経路では `Unspecified` が意図どおり後段へ落ちることを検査する (→ design.md Risks)
- [x] 7.4a Android: 夜間 / 非夜間で title 未指定の ButtonCell の title が該当セットの ButtonCell 既定になり、accent だけ明示しても変わらず、Compose 経路 (Context 不要の解決) と View 経路 (ARGB) が同値 (→ Scenario: ButtonCell の title 既定は外観で選ばれ accent と独立)
- [x] 7.5 Android: Section 装飾の `sectionBorderColor = Unspecified` が透明として描かれる (→ MODIFIED Requirement: Theme の Section 装飾4属性)
- [x] 7.6 Android bridge: 全 null の Theme DTO が全 `Unspecified` の Theme に変換される。CellStyle DTO は全 null と一部明示 ARGB の両方で、未指定だけ `Unspecified`・明示 ARGB は保持される (→ Requirement: 未指定色の外観既定への解決)
- [x] 7.7 iOS: 既定色を light / dark trait で解決した生値が approved.png の対応表と一致する。既定 Theme 同士と既定値を明示した Theme の等価性。既存の `isEqual` 比較テスト (`ThemeRenameTests` / `SectionAccessoryRenderingTests`) を trait 解決比較に書き換える (→ Requirement: 既定色の外観追随 (iOS))
- [x] 7.8 iOS: ダーク trait の window で Theme 未指定の list を表示し背景・separator・Header / Footer が dark 値になる。表示中の trait 変更で描き直される。明示指定は変わらない (→ Requirement: 既定色の外観追随 (iOS))
- [x] 7.9 iOS: dynamic な `sectionBorderColor` の枠線が trait 変更で再解決される (→ Requirement: Section 装飾の枠線色の外観再解決)
- [x] 7.10 既存テストの追随 (Android `DEFAULT_*` 参照・`null` 渡し、iOS の固定 RGB 比較) と全件実行 (handbook cross/test-execution.md の規律で件数を報告)

## 8. 視覚照合 (ksn-ui)
- [x] 8.1 iOS Native: Theme を渡さない画面 (Store / DSL / 共通フィールド統合 / isVisible / Section 装飾デモ) をダークで撮影し approved.png と照合。references/ の症状画像と同じ画面が判読できることを確認 (→ Requirement: 既定色の外観追随 (iOS) / samples-ios)
- [x] 8.2 Android Native: 同上をエミュレータの夜間モードで撮影し照合。選択面とカレンダーの選択面も撮る (→ Requirement: 未指定色の外観解決 / samples-android)
- [x] 8.3 MAUI iOS / MAUI Android: 色プロパティを設定しない画面 (共通フィールド統合 / isVisible / Section 装飾デモ) をダークで撮影し照合 (MAUI に Store / DSL 画面は無い)。MAUI Android は「システム」選択で表示中に端末の夜間を切り替え、表示中の追随も撮る (→ Requirement: 未指定色の外観既定への解決 / 夜間モード変更時の未指定色の再解決 / samples-maui)
- [x] 8.4 ライトの非回帰: 実装着手前に基準コミット (本 change の分岐元 `develop` の SHA を記録) で同じ Simulator / Emulator・OS・画面状態の Theme を渡さない画面をライトで撮影しておき、実装後に同条件で A/B 撮影して比較する (ステータスバーを除く)。画素一致が取れない環境では、light セットの値テスト (7.1 / 7.7) と目視のレイアウト非回帰に分けて記録する (→ Scenario: ライト外観の既定色は変わらない / ライトの既定は本 change 前と同じ)
- [x] 8.5 撮影画像を `ui/verification/` に置き、brief.md に照合記録を書く (個人要素なしの確認を含む)

## 9. 申し送り (蒸留)
- [x] 9.1 concepts の改訂範囲 (exploration.md 論点 8) と ADR-0030 の accepted 昇格を ksn-distill へ申し送る。`skills/` の追従は docs-refresh の明示依頼で別途
