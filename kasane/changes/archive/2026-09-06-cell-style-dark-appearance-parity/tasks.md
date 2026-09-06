# Tasks: cell-style-dark-appearance-parity

挙動の正は `specs/`。設計判断は [exploration.md](exploration.md) の決定事項 (論点 1 = (a)、論点 2 = A) と [proposal.md](proposal.md)。UI の新規デザインは無く mock は持たない。`ui/brief.md` は「見た目は本 change 前と同じ」を非回帰の基準として記録し、`ui/verification/` に基準画像と最終画像を置く (8.2)。

## 0. spike (先頭で実施。崩れたら探索へ戻す)
- [x] 0.1 MAUI の Cell 単位プロパティに `AppThemeBinding` を使ったとき、外観切替で facade → snapshot → bridge → native の Cell 置換まで届くかを iOS / Android 各 1 面で実物確認し、結果を本 tasks の注記に残す (前回 change の 0.1 と同じ流儀: Sample の基本 Cell デモの ButtonCell `TitleColor` に一時的に `AppThemeBinding` を付け、表示したまま外観を切り替える。Android は `MainActivity` の `Recreate()` を一時的に外して同一 Activity / 同一 View のまま届くことも確認する。一時改変は確認後に戻し `git diff -- samples/maui` が空であることを確認する。証跡は `evidence/` に、識別子・端末名を含めない形で)。**届かなければ以降のタスクに進まず探索へ戻す** (承認ゲート) (→ Requirement: Cell 単位の色プロパティの変更は表示中の Cell に届く / Scenario: Cell プロパティの AppThemeBinding の値が native まで届く)

  **結果: 届かない (MAUI iOS / MAUI Android とも)。承認ゲート不通過。**

  破綻しているのは facade より手前 — MAUI の `AppThemeBinding` が Cell のプロパティで**再評価されない**。外観が変わっても C# 上の `ButtonCell.TitleColor` の値そのものが light 値のまま動かないため、snapshot 以降の経路は起動すらしない。一方、同じ Cell のプロパティへ**直接代入**した色は、同じ画面・同じ Activity のまま native の行まで届いて描き替わる。

  - 確認方法: MAUI Sample の基本 Cell デモ (`samples/maui/KsSettingsView.Sample.Maui/Pages/BasicCellsDemoPage.xaml`) の ButtonCell「ログアウト」の `TitleColor` に `{AppThemeBinding Light=#FF008000 (緑), Dark=#FFFF00FF (マゼンタ)}` を一時付与し、code-behind の `LogoutButton.TitleColor = …` を一時的に外して binding だけが値を決める状態にした。同じページに 3 つの対照を置いた: (1) 同じ `AppThemeBinding` を付けた通常の MAUI `Label` (視覚ツリー上)、(2) code で作り視覚ツリーに載せない `Label` に `SetAppTheme` した色、(3) 外観変更の 2.5 秒後に Cell へ直接代入する色。各値と `Application.RequestedTheme`・`ButtonCell.Parent` をログに出した
  - MAUI iOS (Simulator / iOS 26): 表示したまま Simulator の外観をダークにすると `RequestedTheme` は `Dark` になり、対照 (1) の Label は緑 → マゼンタへ再評価された。ButtonCell の title は**緑のまま**。対照 (2) の非アタッチ Label も**緑のまま**。その後の直接代入では title がマゼンタへ描き替わった
  - MAUI Android (Emulator / API 35): `MainActivity.OnConfigurationChanged()` の `Recreate()` を一時的に外したビルドで、表示したまま端末の夜間モードを on にした。結果は iOS と同一 (対照 (1) は追随、ButtonCell の title は緑のまま、直接代入では描き替わる)。同一 Activity / 同一 View であることは `adb shell dumpsys activity top` の identityHashCode が切替の前後で一致することで確認した (Activity `329c446` / KsSettingsView `5085ecb` / RecyclerView `b01585`)
  - 原因の見立て: `ButtonCell.Parent` は両 OS とも `null` で、facade は Section / Cell を MAUI の element ツリーへ繋いでいない (`maui/KsSettingsView.Maui/Section.cs` / `SettingsView.cs` に親子付けのコードが無い)。対照 (2) の「視覚ツリーに載せない素の `Label` も再評価されない」という実測から、再評価されない条件は Cell という型ではなく**アプリの element ツリーに繋がっていないこと**だと分かる。MAUI 本体の `AppThemeBinding` 実装そのものは読んでいないため、ここは実測から導いた見立てであって内部実装の確認ではない
  - 証跡: `evidence/maui-ios-basic-cells-buttoncell-appthemebinding-light.png` / `evidence/maui-ios-basic-cells-buttoncell-appthemebinding-dark.png` / `evidence/maui-ios-basic-cells-buttoncell-direct-assignment-dark.png`、`evidence/maui-android-basic-cells-buttoncell-appthemebinding-light.png` / `evidence/maui-android-basic-cells-buttoncell-appthemebinding-dark.png` / `evidence/maui-android-basic-cells-buttoncell-direct-assignment-dark.png`、`evidence/maui-android-basic-cells-instance-identity.txt`、`evidence/maui-spike-appthemebinding-log.txt`。dark の画像はいずれも画面を表示したまま外観を切り替えた後で、同じ画面の Label (「最後にタップ: (none)」) がマゼンタ = dark 値に変わっている一方、ButtonCell の「ログアウト」は緑 = light 値のまま
  - 一時改変 (`AppThemeBinding` の付与・code-behind の無効化・計測ログ・対照 Label・`Recreate()` の無効化) は確認後に戻し、`git diff -- samples/maui` が空であることを確認したうえで、改変前のビルドを Simulator / Emulator に入れ直して表示を目視した
  - **裁定 (2026-09-06、オーナー)**: MAUI の手段を「外観変更を購読して Cell の色プロパティへ再代入」に改めて本 change を続行する (spec / proposal / 3.3 / 8.4 を修正済み)。`AppThemeBinding` を Cell で効かせる改修は `maui-appthemebinding-coverage` で別途探索。以下は裁定前の記録
  - **以降のタスクへは進んでいない** (承認ゲート不通過)。spec の Scenario「Cell プロパティの AppThemeBinding の値が native まで届く」と Requirement の「両外観で異なる色を使いたい利用者の手段は Cell の色プロパティに `AppThemeBinding` を書くこと」は現状の実装では成立しないため、探索へ戻す判断がいる

## 1. Android 本体: Cell 固有色の型
- [x] 1.1 Cell の data class の色引数 12 本 (`EntryCell.placeholderColor` / `accentColor`、`ButtonCell.titleColor`、`SwitchCell` / `CheckboxCell` / `SimpleCheckCell` / `RadioCell` / `PickerCell` / `NumberPickerCell` / `TimePickerCell` / `DatePickerCell` の `accentColor`、`DatePickerCell.androidButtonColor`) を `Color = Color.Unspecified` にする。対象 10 Cell すべてが手書きの `hashCode` (`accentColor?.hashCode() ?: 0` 等) と一部は手書き `equals` を持つため、10 Cell 全部を追随させる (→ Requirement: Cell 固有色の未指定表現)
- [x] 1.2 Compose DSL の Cell 関数 (`compose/BasicCellDsl.kt` の buttonCell / switch / checkbox / simpleCheck / radio 系、`compose/InputCellDsl.kt` の entry / picker / numberPicker / timePicker / datePicker 系) と `PickerCellItemProjection` の同名引数を同じ型・既定値にする (→ Requirement: Cell 固有色の未指定表現 / Scenario: DSL の Cell 関数は色引数を省略すると未指定になる)
- [x] 1.3 `EffectiveStyle` の Cell 固有段を受ける解決関数 (`effectivePlaceholderColor(entryPlaceholderColor, …)` / `effectiveButtonTitleColor` / `effectiveButtonTitleColorArgb`) の引数を `Color` にし、`?.let` を `takeOrElse` に置き換える。解決順は変えない (→ Requirement: Cell 固有色の未指定表現 / Scenario: EntryCell の placeholder は段ごとに解決する, ButtonCell の title は段ごとに解決する)
- [x] 1.4 ViewHolder の `cell.accentColor?.toArgb() ?: effective.accentColor` 型の読み出し (`SwitchCellViewHolder` / `CheckboxCellViewHolder` / `SimpleCheckCellViewHolder` / `RadioCellViewHolder` / `EntryCellViewHolder` / `DatePickerCellViewHolder` の accent と `androidButtonColor`) を `Unspecified` が ARGB へ落ちない形 (`takeIf { isSpecified }?.toArgb() ?:` または `takeOrElse` 後に変換) にする。Picker 系の選択面 (`PickerSelectionSheet` / projection 経由) と TimePicker / NumberPicker の accent 消費者も同じく確認する (→ Requirement: Cell 固有色の未指定表現 / Scenario: 未指定の Cell 固有色は CellStyle と Theme へ継承する, DatePickerCell の androidButtonColor は未指定なら accent へ倒れる)
- [x] 1.5 触るファイル (`ButtonCell.kt` / `RadioCell.kt` ほか 1.1 で編集する Cell ファイル) の公開 doc コメントに残る ADR 参照を除去し現在形の説明に置き換える (handbook cross/comment-policy.md。前回 change の据え置き分のうち本 change が触る範囲) (付随修正。対応 Requirement なし)

## 2. Android bridge
- [x] 2.1 `KsBridgeColor.color(argb)` を未指定で `Color.Unspecified` を返す形にし (または各 Cell DTO の変換で `?: Color.Unspecified`)、`KsBridgeTheme` / `KsBridgeCellStyle` の private な `color()` と Cell DTO 10 種の変換を 1 流儀に揃える (→ Requirement: Android bridge の Cell 固有色の変換)
- [x] 2.2 `KsBridgeCellStyle.kt` の doc コメント「native の CellStyle の公開項目と 1 対 1」を輸送項目の実態 (accent / placeholder の CellStyle 段は MAUI から設定されず、placeholder は Cell 固有段で輸送) に書き換える (→ Requirement: CellStyle DTO が輸送する項目)

## 3. iOS bridge / MAUI facade の記述
- [x] 3.1 `ios/Sources/KsSettingsViewBridge/KsBridgeCellStyle.swift` と `maui/KsSettingsView.Maui/Internals/KsCellStyleSnapshot.cs` の doc コメントを 2.2 と同じ実態へ書き換える。コードの変更なし (→ Requirement: CellStyle DTO が輸送する項目)
- [x] 3.2 MAUI の各 Cell の `AffectsSnapshot` が Cell 固有色 (`AccentColor` / `PlaceholderColor` / `AndroidButtonColor`) を含むこと、`CustomCell` がテキスト系 style を含まないことをコードで確認し、漏れがあれば追加する (→ Requirement: Cell 単位の色プロパティの変更は表示中の Cell に届く / Scenario: 表示中の Cell 固有色の変更が Cell 置換として配信される, CustomCell のテキスト系 style 変更は内容更新にならない)
- [x] 3.3 `KsSettingsView.MauiHost` に、アプリの外観変更 (`Application.RequestedThemeChanged`) を購読して ButtonCell の `TitleColor` へ外観に応じた値を再代入する行を既定シナリオに追加し、0.1 の「直接代入は届く」確認を実装後に反復できる回帰資産にする (購読はページ表示中だけ持つ。handbook maui/integration-host-verification.md の「期待される表示」に追記) (→ Scenario: 外観変更を受けて再代入した Cell の色が native まで届く)

## 4. tests: Android 本体
- [x] 4.1 Cell 固有色の既定が `Unspecified` であること、DSL 関数の省略が `Unspecified` になること、10 Cell の `equals` / `hashCode` が未指定同士で等価・明示色の差で非等価になることのテスト (表形式で 12 フィールドを網羅。`BasicCellsTest` / `InputCellsTest` / DSL テストに追加) (→ Scenario: 既定の Cell 固有色は未指定, DSL の Cell 関数は色引数を省略すると未指定になる)
- [x] 4.2 `EffectiveStyle` の解決テスト: accent の 3 段優先 (Cell 固有 > CellStyle > Theme)、placeholder と ButtonCell title の段ごと (Cell 固有 / CellStyle / Theme / 最終既定の各 1 ケース)、`androidButtonColor` の accent へのフォールバック。いずれも `Unspecified` が ARGB へ落ちないことを含む (→ Scenario: 未指定の Cell 固有色は CellStyle と Theme へ継承する, 明示した Cell 固有色は CellStyle と Theme より優先する, EntryCell の placeholder は段ごとに解決する, ButtonCell の title は段ごとに解決する, DatePickerCell の androidButtonColor は未指定なら accent へ倒れる)
- [x] 4.3 `ThemeAppearanceResolutionTest` に、明示した `CellStyle` 色と明示した Cell 固有 accent が夜間モードへの切替後も変わらず、同じ行の未指定色だけが dark 既定へ変わるテストを追加する (→ Scenario: 明示した CellStyle 色は夜間モードへの切替後も変わらない, 明示した Cell 固有色は夜間モードへの切替後も変わらない)
- [x] 4.4 Compose DSL で `CellStyle` だけを変えた再 composition が `replaceCell` として発行され行の title 色が変わること、Store 経路の `replaceCell` で style だけ差し替えた Cell が行に反映され identity が保たれることのテスト (→ Scenario: DSL で外観に応じて選んだ CellStyle 色は再 composition で行に届く, Store 経路で style だけ差し替えた Cell は行に反映される)
- [x] 4.5 既存テストの追随: `DateCalendarDialogTest` の `accentColor = null` など `null` を渡している箇所と、`Color?` 前提の比較を `Unspecified` に書き換える (→ Requirement: Cell 固有色の未指定表現)

## 5. tests: Android bridge
- [x] 5.1 `KsBridgeCellConversionTest` に、Cell DTO の色項目 null → `Color.Unspecified`、明示 ARGB → 同値の `Color` のテストを追加する (EntryCell / DatePickerCell / SwitchCell) (→ Scenario: Cell DTO の未指定色は Unspecified になる, Cell DTO の明示 ARGB は保たれる)
- [x] 5.2 MAUI の `PlaceholderColor` (Cell 固有段) が Theme の `CellPlaceholderColor` より優先して解決されることを、Android bridge の変換 + `EffectiveStyle` の解決で固定する (→ Scenario: MAUI の PlaceholderColor は Cell 固有段として届く)

## 6. tests: iOS
- [x] 6.1 `EffectiveStyleResolutionTests` または `ThemeDefaultColorAppearanceTests` に、`CellStyle.titleColor` に dynamic な `UIColor` を渡したとき dark trait でその色の dark 値へ解決され、固定色は変わらないテストを追加する (→ Scenario: CellStyle の dynamic 色はダーク外観でその色の dark 値になる, CellStyle の固定色は外観で変わらない)
- [x] 6.2 `KsCheckBoxView` の accent (CGColor 化する layer の塗りと枠) が trait 変更で dynamic 色の dark 値へ再解決されるテストを追加する (`BasicCellsTests` の既存の Checkbox テストの流儀に合わせる) (→ Scenario: Cell 固有の accent に渡した dynamic 色は trait 変更で再解決される)
- [x] 6.3 SwiftUI DSL で `CellStyle` の色だけを変えた再評価が内容更新 (reconfigure) として発行されるテストを、`DSLDiffCalculator` の既存テストに追加する (→ Scenario: SwiftUI DSL で外観に応じて選んだ CellStyle 色は差分として行に届く)

## 7. tests: MAUI (net10 ユニットテスト、fake gateway)
- [x] 7.1 表示中の LabelCell の `TitleColor` 設定 / `null` 戻しが、その Cell だけの `ReplaceCell` として配信され、style の title 色が設定値の ARGB / 未指定になるテスト (`BatchDeliveryTests` または `ThemeAndCellStyleTests` の流儀) (→ Scenario: 表示中の Cell の TitleColor 変更が Cell 置換として配信される, 色プロパティを null に戻すと未指定として配信される)
- [x] 7.2 表示中の SwitchCell の `AccentColor`、EntryCell の `PlaceholderColor`、DatePickerCell の `AndroidButtonColor` の設定が Cell 置換として配信され、DTO の Cell 固有色が設定値の ARGB になるテスト。CustomCell の `TitleColor` 設定は置換を配信せず `BackgroundColor` は配信することも固定する (→ Scenario: 表示中の Cell 固有色の変更が Cell 置換として配信される, CustomCell のテキスト系 style 変更は内容更新にならない)

## 8. 実行と確認
- [x] 8.1 Android 本体 / bridge、iOS、MAUI (net10 ユニットテスト) のテストを handbook cross/test-execution.md に従って実行し、結果を報告する

  Android 分は完了 (2026-09-06)。`cd android && ./gradlew test --rerun-tasks` で debug / release 両 variant を全件実行し、本体 1254 件 × 2 + bridge 173 件 × 2 = 計 2854 件 (修正サイクル 1 で +4: 新規 2 本 × 2 variant) / 失敗 0 件。件数は `build/test-results/test{Debug,Release}UnitTest/TEST-*.xml` の `tests` / `failures` 属性の合計。修正サイクル 2 (テストの観測点差し替え) の後に再実行し、同じ 2854 件 / 失敗 0 件を確認済み (テストの増減は無い)。

  iOS 分は完了 (2026-09-06)。`cd ios && xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,name=<機種名>'` で全件実行し、バンドル集計行の合計で Bridge 166 + Core 88 + SwiftUI 96 + TestSupport 7 + UI 670 = 計 1027 件 / 失敗 0 件。

  MAUI 分は完了 (2026-09-06)。`dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` で 520 件 / 失敗 0 件 (7.1 / 7.2 で 4 件追加)。
- [x] 8.2 Android Native Sample の基本 Cell デモ・共通フィールド統合デモ・入力 Cell デモ・日付選択デモを Emulator のライト / ダークで表示し、本 change 前 (実装着手前に同じ画面を撮った基準画像) と描画が変わらないこと (Cell 固有色の型変更による透明黒の混入がないこと) を照合する。基準と最終画像は `ui/verification/` に、照合結果は `ui/brief.md` に記録する (識別子・端末名を含めない)
- [x] 8.3 `ios/Sources/` に触れる (3.1 の doc コメント) ため、handbook ios/swift6-language-mode-check.md の手順で Swift 6 言語モードの一時設定ビルドを行い error 0 件と `ios/Package.swift` の差分 0 件を確認する

  結果 (2026-09-06): `ios/Package.swift` に `swiftLanguageVersions: [.version("6")]` を一時追加し `xcodebuild build -scheme KsSettingsView -destination 'platform=iOS Simulator,name=<機種名>'` を実行。BUILD SUCCEEDED / error 0 件。一時設定を戻して `git diff -- ios/Package.swift` が差分 0 件であることを確認した。
- [x] 8.4 MAUI: `KsSettingsView.MauiHost` の既定シナリオ (3.3 を含む) を iOS / Android で起動し、画面を表示したまま外観を切り替えると ButtonCell の title が再代入した値で描き直されることを確認する。証跡 (light / dark) は `evidence/` に (handbook maui/integration-host-verification.md)

  結果: 両 OS とも成立 (2026-09-06)。設定画面を表示したまま外観を切り替えると、「外観追随ボタン」の title が同じ画面・同じ行のまま緑 (`#FF008000`) → マゼンタ (`#FFFF00FF`) へ描き直された。証跡は `evidence/maui-host-buttoncell-retheme-{ios,android}-{light,dark}.png`。iOS は Simulator (`xcrun simctl ui <udid> appearance dark`)、Android は Emulator (`adb -s <serial> shell cmd uimode night yes`。配備は `dotnet build … -t:Run -p:AdbTarget="-s <serial>"` で Emulator に限定した)。

  **証跡に写る別件 (本 change 起因ではない)**: MAUI iOS の dark 側の画像では、「外観追随ボタン」以外の Cell のテキストが読めなくなっている。行の背景がライトのまま (白) で、未指定のテキスト色だけが dark 既定 (白系) へ解決されるため。本 change の変更を外した HEAD のビルドでも同じ表示になることを確認済みで、本 change 起因ではない。Android 側では起きない (背景・テキストとも dark で描かれる)。原因の特定はしていない。

## 蒸留への申し送り (実装タスクではない)
- concepts: `core/styling/style-resolution.md` (未指定表現を Cell 固有色まで正確に、明示色の追随手段 4 経路、`KsCheckBoxView` の CGColor 再解決)、`android/api/android-native-host.md` / `android-compose.md` (Cell 固有色の型、事前定義した色の持ち方、Store 経路の `replaceCell` 例)、`ios/api/ios-native-host.md` (CellStyle の dynamic 色の例)、`maui/api/maui-styling.md` (Cell プロパティの外観追随は `RequestedThemeChanged` 購読 + 再代入。`AppThemeBinding` は Cell では再評価されない)、`maui/api/native-bridge.md` (CellStyle DTO の輸送項目)
- ADR-0030 Decision 3 に CellStyle / Cell 固有色への適用を一文追記し、Context の前提「ライブラリは未配信」と Revisit When の該当項を「`0.1.0-beta.1` 配信済み。beta 期間中は破壊的変更を受容 (2026-09-06 オーナー裁定)」へ訂正する (amend 相当の小改訂)
- 次の prerelease のリリースノートに Android の Cell 固有色の型変更を breaking change として記載する (release-procedure の担当範囲)
- (review-001 Minor 3) ADR-0030 Decision 3 は「MAUI は `AppThemeBinding`」と書いている。Cell 段への適用を追記するとき、MAUI 節は「`SettingsView` の Theme プロパティには `AppThemeBinding`、Cell の色プロパティは `RequestedThemeChanged` 購読 + 再代入 (Cell は element ツリー外で binding が再評価されない)」と限定して書く。`maui/api/maui-styling.md` も同様
- (review-001 Suggestion) settings-view-android-ui spec の「内容更新 (`replaceCell`)」は Android Compose DSL では `store.replaceCells` (`KsSettingsViewComposable.kt`) が担う。concepts へ写すときは実態どおり書く
- skills/ の追従は docs-refresh でユーザーの明示依頼により別途
