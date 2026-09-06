# Tasks: cell-style-dark-appearance-parity

挙動の正は `specs/`。設計判断は [exploration.md](exploration.md) の決定事項 (論点 1 = (a)、論点 2 = A) と [proposal.md](proposal.md)。UI の新規デザインは無く mock は持たない。`ui/brief.md` は「見た目は本 change 前と同じ」を非回帰の基準として記録し、`ui/verification/` に基準画像と最終画像を置く (8.2)。

## 0. spike (先頭で実施。崩れたら探索へ戻す)
- [ ] 0.1 MAUI の Cell 単位プロパティに `AppThemeBinding` を使ったとき、外観切替で facade → snapshot → bridge → native の Cell 置換まで届くかを iOS / Android 各 1 面で実物確認し、結果を本 tasks の注記に残す (前回 change の 0.1 と同じ流儀: Sample の基本 Cell デモの ButtonCell `TitleColor` に一時的に `AppThemeBinding` を付け、表示したまま外観を切り替える。Android は `MainActivity` の `Recreate()` を一時的に外して同一 Activity / 同一 View のまま届くことも確認する。一時改変は確認後に戻し `git diff -- samples/maui` が空であることを確認する。証跡は `evidence/` に、識別子・端末名を含めない形で)。**届かなければ以降のタスクに進まず探索へ戻す** (承認ゲート) (→ Requirement: Cell 単位の色プロパティの変更は表示中の Cell に届く / Scenario: Cell プロパティの AppThemeBinding の値が native まで届く)

## 1. Android 本体: Cell 固有色の型
- [ ] 1.1 Cell の data class の色引数 12 本 (`EntryCell.placeholderColor` / `accentColor`、`ButtonCell.titleColor`、`SwitchCell` / `CheckboxCell` / `SimpleCheckCell` / `RadioCell` / `PickerCell` / `NumberPickerCell` / `TimePickerCell` / `DatePickerCell` の `accentColor`、`DatePickerCell.androidButtonColor`) を `Color = Color.Unspecified` にする。対象 10 Cell すべてが手書きの `hashCode` (`accentColor?.hashCode() ?: 0` 等) と一部は手書き `equals` を持つため、10 Cell 全部を追随させる (→ Requirement: Cell 固有色の未指定表現)
- [ ] 1.2 Compose DSL の Cell 関数 (`compose/BasicCellDsl.kt` の buttonCell / switch / checkbox / simpleCheck / radio 系、`compose/InputCellDsl.kt` の entry / picker / numberPicker / timePicker / datePicker 系) と `PickerCellItemProjection` の同名引数を同じ型・既定値にする (→ Requirement: Cell 固有色の未指定表現 / Scenario: DSL の Cell 関数は色引数を省略すると未指定になる)
- [ ] 1.3 `EffectiveStyle` の Cell 固有段を受ける解決関数 (`effectivePlaceholderColor(entryPlaceholderColor, …)` / `effectiveButtonTitleColor` / `effectiveButtonTitleColorArgb`) の引数を `Color` にし、`?.let` を `takeOrElse` に置き換える。解決順は変えない (→ Requirement: Cell 固有色の未指定表現 / Scenario: EntryCell の placeholder は段ごとに解決する, ButtonCell の title は段ごとに解決する)
- [ ] 1.4 ViewHolder の `cell.accentColor?.toArgb() ?: effective.accentColor` 型の読み出し (`SwitchCellViewHolder` / `CheckboxCellViewHolder` / `SimpleCheckCellViewHolder` / `RadioCellViewHolder` / `EntryCellViewHolder` / `DatePickerCellViewHolder` の accent と `androidButtonColor`) を `Unspecified` が ARGB へ落ちない形 (`takeIf { isSpecified }?.toArgb() ?:` または `takeOrElse` 後に変換) にする。Picker 系の選択面 (`PickerSelectionSheet` / projection 経由) と TimePicker / NumberPicker の accent 消費者も同じく確認する (→ Requirement: Cell 固有色の未指定表現 / Scenario: 未指定の Cell 固有色は CellStyle と Theme へ継承する, DatePickerCell の androidButtonColor は未指定なら accent へ倒れる)
- [ ] 1.5 触るファイル (`ButtonCell.kt` / `RadioCell.kt` ほか 1.1 で編集する Cell ファイル) の公開 doc コメントに残る ADR 参照を除去し現在形の説明に置き換える (handbook cross/comment-policy.md。前回 change の据え置き分のうち本 change が触る範囲) (付随修正。対応 Requirement なし)

## 2. Android bridge
- [ ] 2.1 `KsBridgeColor.color(argb)` を未指定で `Color.Unspecified` を返す形にし (または各 Cell DTO の変換で `?: Color.Unspecified`)、`KsBridgeTheme` / `KsBridgeCellStyle` の private な `color()` と Cell DTO 10 種の変換を 1 流儀に揃える (→ Requirement: Android bridge の Cell 固有色の変換)
- [ ] 2.2 `KsBridgeCellStyle.kt` の doc コメント「native の CellStyle の公開項目と 1 対 1」を輸送項目の実態 (accent / placeholder の CellStyle 段は MAUI から設定されず、placeholder は Cell 固有段で輸送) に書き換える (→ Requirement: CellStyle DTO が輸送する項目)

## 3. iOS bridge / MAUI facade の記述
- [ ] 3.1 `ios/Sources/KsSettingsViewBridge/KsBridgeCellStyle.swift` と `maui/KsSettingsView.Maui/Internals/KsCellStyleSnapshot.cs` の doc コメントを 2.2 と同じ実態へ書き換える。コードの変更なし (→ Requirement: CellStyle DTO が輸送する項目)
- [ ] 3.2 MAUI の各 Cell の `AffectsSnapshot` が Cell 固有色 (`AccentColor` / `PlaceholderColor` / `AndroidButtonColor`) を含むこと、`CustomCell` がテキスト系 style を含まないことをコードで確認し、漏れがあれば追加する (→ Requirement: Cell 単位の色プロパティの変更は表示中の Cell に届く / Scenario: 表示中の Cell 固有色の変更が Cell 置換として配信される, CustomCell のテキスト系 style 変更は内容更新にならない)
- [ ] 3.3 `KsSettingsView.MauiHost` に、Cell の `TitleColor` に `AppThemeBinding` を付けた行を含む既定シナリオを追加し、0.1 の確認を実装後に反復できる回帰資産にする (handbook maui/integration-host-verification.md) (→ Scenario: Cell プロパティの AppThemeBinding の値が native まで届く)

## 4. tests: Android 本体
- [ ] 4.1 Cell 固有色の既定が `Unspecified` であること、DSL 関数の省略が `Unspecified` になること、10 Cell の `equals` / `hashCode` が未指定同士で等価・明示色の差で非等価になることのテスト (表形式で 12 フィールドを網羅。`BasicCellsTest` / `InputCellsTest` / DSL テストに追加) (→ Scenario: 既定の Cell 固有色は未指定, DSL の Cell 関数は色引数を省略すると未指定になる)
- [ ] 4.2 `EffectiveStyle` の解決テスト: accent の 3 段優先 (Cell 固有 > CellStyle > Theme)、placeholder と ButtonCell title の段ごと (Cell 固有 / CellStyle / Theme / 最終既定の各 1 ケース)、`androidButtonColor` の accent へのフォールバック。いずれも `Unspecified` が ARGB へ落ちないことを含む (→ Scenario: 未指定の Cell 固有色は CellStyle と Theme へ継承する, 明示した Cell 固有色は CellStyle と Theme より優先する, EntryCell の placeholder は段ごとに解決する, ButtonCell の title は段ごとに解決する, DatePickerCell の androidButtonColor は未指定なら accent へ倒れる)
- [ ] 4.3 `ThemeAppearanceResolutionTest` に、明示した `CellStyle` 色と明示した Cell 固有 accent が夜間モードへの切替後も変わらず、同じ行の未指定色だけが dark 既定へ変わるテストを追加する (→ Scenario: 明示した CellStyle 色は夜間モードへの切替後も変わらない, 明示した Cell 固有色は夜間モードへの切替後も変わらない)
- [ ] 4.4 Compose DSL で `CellStyle` だけを変えた再 composition が `replaceCell` として発行され行の title 色が変わること、Store 経路の `replaceCell` で style だけ差し替えた Cell が行に反映され identity が保たれることのテスト (→ Scenario: DSL で外観に応じて選んだ CellStyle 色は再 composition で行に届く, Store 経路で style だけ差し替えた Cell は行に反映される)
- [ ] 4.5 既存テストの追随: `DateCalendarDialogTest` の `accentColor = null` など `null` を渡している箇所と、`Color?` 前提の比較を `Unspecified` に書き換える (→ Requirement: Cell 固有色の未指定表現)

## 5. tests: Android bridge
- [ ] 5.1 `KsBridgeCellConversionTest` に、Cell DTO の色項目 null → `Color.Unspecified`、明示 ARGB → 同値の `Color` のテストを追加する (EntryCell / DatePickerCell / SwitchCell) (→ Scenario: Cell DTO の未指定色は Unspecified になる, Cell DTO の明示 ARGB は保たれる)
- [ ] 5.2 MAUI の `PlaceholderColor` (Cell 固有段) が Theme の `CellPlaceholderColor` より優先して解決されることを、Android bridge の変換 + `EffectiveStyle` の解決で固定する (→ Scenario: MAUI の PlaceholderColor は Cell 固有段として届く)

## 6. tests: iOS
- [ ] 6.1 `EffectiveStyleResolutionTests` または `ThemeDefaultColorAppearanceTests` に、`CellStyle.titleColor` に dynamic な `UIColor` を渡したとき dark trait でその色の dark 値へ解決され、固定色は変わらないテストを追加する (→ Scenario: CellStyle の dynamic 色はダーク外観でその色の dark 値になる, CellStyle の固定色は外観で変わらない)
- [ ] 6.2 `KsCheckBoxView` の accent (CGColor 化する layer の塗りと枠) が trait 変更で dynamic 色の dark 値へ再解決されるテストを追加する (`BasicCellsTests` の既存の Checkbox テストの流儀に合わせる) (→ Scenario: Cell 固有の accent に渡した dynamic 色は trait 変更で再解決される)
- [ ] 6.3 SwiftUI DSL で `CellStyle` の色だけを変えた再評価が内容更新 (reconfigure) として発行されるテストを、`DSLDiffCalculator` の既存テストに追加する (→ Scenario: SwiftUI DSL で外観に応じて選んだ CellStyle 色は差分として行に届く)

## 7. tests: MAUI (net10 ユニットテスト、fake gateway)
- [ ] 7.1 表示中の LabelCell の `TitleColor` 設定 / `null` 戻しが、その Cell だけの `ReplaceCell` として配信され、style の title 色が設定値の ARGB / 未指定になるテスト (`BatchDeliveryTests` または `ThemeAndCellStyleTests` の流儀) (→ Scenario: 表示中の Cell の TitleColor 変更が Cell 置換として配信される, 色プロパティを null に戻すと未指定として配信される)
- [ ] 7.2 表示中の SwitchCell の `AccentColor`、EntryCell の `PlaceholderColor`、DatePickerCell の `AndroidButtonColor` の設定が Cell 置換として配信され、DTO の Cell 固有色が設定値の ARGB になるテスト。CustomCell の `TitleColor` 設定は置換を配信せず `BackgroundColor` は配信することも固定する (→ Scenario: 表示中の Cell 固有色の変更が Cell 置換として配信される, CustomCell のテキスト系 style 変更は内容更新にならない)

## 8. 実行と確認
- [ ] 8.1 Android 本体 / bridge、iOS、MAUI (net10 ユニットテスト) のテストを handbook cross/test-execution.md に従って実行し、結果を報告する
- [ ] 8.2 Android Native Sample の基本 Cell デモ・共通フィールド統合デモ・入力 Cell デモ・日付選択デモを Emulator のライト / ダークで表示し、本 change 前 (実装着手前に同じ画面を撮った基準画像) と描画が変わらないこと (Cell 固有色の型変更による透明黒の混入がないこと) を照合する。基準と最終画像は `ui/verification/` に、照合結果は `ui/brief.md` に記録する (識別子・端末名を含めない)
- [ ] 8.3 `ios/Sources/` に触れる (3.1 の doc コメント) ため、handbook ios/swift6-language-mode-check.md の手順で Swift 6 言語モードの一時設定ビルドを行い error 0 件と `ios/Package.swift` の差分 0 件を確認する
- [ ] 8.4 MAUI: `KsSettingsView.MauiHost` の既定シナリオ (3.3 を含む) を iOS / Android で起動し、Cell の `AppThemeBinding` が外観切替で描き直されることを確認する (handbook maui/integration-host-verification.md)

## 蒸留への申し送り (実装タスクではない)
- concepts: `core/styling/style-resolution.md` (未指定表現を Cell 固有色まで正確に、明示色の追随手段 4 経路、`KsCheckBoxView` の CGColor 再解決)、`android/api/android-native-host.md` / `android-compose.md` (Cell 固有色の型、事前定義した色の持ち方、Store 経路の `replaceCell` 例)、`ios/api/ios-native-host.md` (CellStyle の dynamic 色の例)、`maui/api/maui-styling.md` (Cell プロパティの `AppThemeBinding`)、`maui/api/native-bridge.md` (CellStyle DTO の輸送項目)
- ADR-0030 Decision 3 に CellStyle / Cell 固有色への適用を一文追記し、Context の前提「ライブラリは未配信」と Revisit When の該当項を「`0.1.0-beta.1` 配信済み。beta 期間中は破壊的変更を受容 (2026-09-06 オーナー裁定)」へ訂正する (amend 相当の小改訂)
- 次の prerelease のリリースノートに Android の Cell 固有色の型変更を breaking change として記載する (release-procedure の担当範囲)
- skills/ の追従は docs-refresh でユーザーの明示依頼により別途
