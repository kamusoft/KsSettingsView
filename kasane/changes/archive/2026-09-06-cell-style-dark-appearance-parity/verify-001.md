# 検証結果: cell-style-dark-appearance-parity (001 回目)

**日付**: 2026-09-06
**判定**: VALID

デルタスペック 3 本の Requirement 6 件 / Scenario 23 件をすべて実装・テストに対応付けた。❌ は 0 件。⚠️ (deviation 記録済み) が 2 件、tasks の手段からの差として記録済みの置き換えが 5 件ある。tasks.md の虚偽チェックなし。足場の書き換えはあるが deviation.md にオーナー裁定つきで記録済み。テストは 3 platform とも再実行して全件成功を確認した。

## テスト実行 (再実行して確認)

| platform | コマンド | 結果 |
|---|---|---|
| Android | `cd android && ./gradlew test --rerun-tasks` | 2850 件 / 失敗 0 (`*/build/test-results/test{Debug,Release}UnitTest/TEST-*.xml` の `tests` / `failures` + `errors` 合計) |
| iOS | `cd ios && xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,name=<機種名>'` | `** TEST SUCCEEDED **` / 失敗 0 (UI バンドル 670 件を含む) |
| MAUI | `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` | 520 件 / 失敗 0 |

いずれも tasks 8.1 の報告値と一致する。

---

## 対応表: settings-view-android-ui

### Requirement: Cell 固有色の未指定表現

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 既定の Cell 固有色は未指定 | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCell.kt:54,59` / `ButtonCell.kt:35` / `SwitchCell.kt:29` / `CheckboxCell.kt:26` / `SimpleCheckCell.kt:30` / `RadioCell.kt:29` / `PickerCell.kt:43` / `NumberPickerCell.kt:38` / `TimePickerCell.kt:38` / `DatePickerCell.kt:47,48` | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/CellSpecificColorTest.kt` (`Cell 固有色 12 本の既定は Unspecified`) | ✅ 一致 |
| 未指定の Cell 固有色は CellStyle と Theme へ継承する | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyle.kt:518` (`toArgbOrElse`) と 7 消費点 (`SwitchCellViewHolder.kt:154` / `CheckboxCellViewHolder.kt:40` / `SimpleCheckCellViewHolder.kt:32` / `RadioCellViewHolder.kt:32` / `EntryCellViewHolder.kt:237` / `DatePickerCellViewHolder.kt:131,224` / `PickerSelectionSheet.kt:134`) | `CellSpecificColorTest.kt` (`未指定の Cell 固有 accent は CellStyle へ継承する` / `…Theme へ継承する` / `SimpleCheckCell と RadioCell の accent も 3 段で解決する`) | ✅ 一致 |
| 明示した Cell 固有色は CellStyle と Theme より優先する | 同上 | `CellSpecificColorTest.kt` (`明示した Cell 固有 accent は CellStyle と Theme より優先する`) | ✅ 一致 |
| DSL の Cell 関数は色引数を省略すると未指定になる | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/compose/BasicCellDsl.kt:270,303,331,360,391,422` / `InputCellDsl.kt:44,52,82,90,126,165,205,244,290,331,374,414,454,456` / `ui/PickerCellItemProjection.kt:40,90,134,173` | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/compose/BasicCellDslTest.kt` (`基本 Cell の DSL は色引数を省略すると未指定になる` / `…明示した色引数をそのまま渡す`)、`InputCellDslTest.kt` (`入力系 Cell の DSL は…` 2 件、`EntryCell DSL placeholderColor 未指定は Unspecified のまま`) | ✅ 一致 |
| EntryCell の placeholder は段ごとに解決する | `ui/EffectiveStyle.kt:333-336` (4 段版) / `ui/EntryCellViewHolder.kt:193-199` | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyleResolutionTest.kt` (Theme フォールバック / 全未指定)、既存 `InputCellsTest.kt:294-417` (Cell 固有 / CellStyle / Theme の 3 段) | ✅ 一致 |
| ButtonCell の title は段ごとに解決する | `ui/EffectiveStyle.kt:434-442` / `:456-462` | `EffectiveStyleResolutionTest.kt` (CellStyle 採用 / Theme 採用 / ライト既定 / ダーク既定 の 4 ケース) | ✅ 一致 |
| DatePickerCell の androidButtonColor は未指定なら accent へ倒れる | `ui/DatePickerCellViewHolder.kt:131` | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DateSelectionSheetTest.kt` (`androidButtonColor と accentColor が未指定なら操作色は Theme の強調色になる`、既存 `androidButtonColor 未指定なら操作色も強調色の段階解決に従う`) | ✅ 一致 |

Requirement 本文の「`null` を未指定として受ける色引数は存在しない (SHALL NOT)」: `android/` 配下の `.kt` に `Color?` の宣言が 0 件であることを確認した。
「描画時に `Unspecified` が ARGB へ変換されることはない (SHALL NOT)」: 素の `toArgb()` が残るのは `ui/EffectiveStyle.kt:116` と `:141` の 2 箇所のみで、いずれも `takeOrElse` の解決チェーンを通った後の値である。
「`Unspecified` 同士の Cell は等価」: `CellSpecificColorTest.kt` の等価性 2 件 (12 フィールド全走査) が固定している。

### Requirement: 明示した Cell 色と外観

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 明示した CellStyle 色は夜間モードへの切替後も変わらない | (実装変更なし。既存の外観再解決経路) | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ThemeAppearanceResolutionTest.kt` (`明示した CellStyle 色は夜間モードへの切替後も変わらない`) | ✅ 一致 |
| 明示した Cell 固有色は夜間モードへの切替後も変わらない | 同上 | `ThemeAppearanceResolutionTest.kt` (`明示した Cell 固有色は夜間モードへの切替後も変わらない`) | ✅ 一致 |
| DSL で外観に応じて選んだ CellStyle 色は再 composition で行に届く | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposable.kt:130` (内容更新は `store.replaceCells` で配る。実装変更なし) | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/compose/DSLCellStyleUpdateTest.kt` | ✅ 一致 (注記あり) |
| Store 経路で style だけ差し替えた Cell は行に反映される | (実装変更なし) | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/StoreCellStyleReplaceTest.kt` | ✅ 一致 (注記あり) |

注記 (❌ ではないが THEN の一部が観測されていない):

- 「DSL で外観に応じて選んだ…」の THEN 前半「差分は内容更新 (`replaceCell`) として発行され」は観測していない。テストが観測するのは行の実描画色 (THEN 後半) だけ。なお Android の DSL は内容変化で `SettingsRootDiff.ReplaceCell` を発行しない設計 (`compose/DSLDiffCalculator.kt:18-20`) で、内容更新は `compose/KsSettingsViewComposable.kt:130` の `store.replaceCells` が担うため、spec が名指しする機構名自体が Android の実態と食い違う。行の色が届くという実質は満たされている
- 「Store 経路で style だけ差し替えた…」の THEN 後半「Section / Cell の identity は維持される」は、モデル (`view.internalRoot()`) の id 一覧で観測しており、行 (ViewHolder) が作り直されないことは観測していない

いずれも review-001.md の Minor 2 として指摘した。

---

## 対応表: settings-view-ios-ui

### Requirement: 明示した Cell 色と外観 (iOS)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| CellStyle の dynamic 色はダーク外観でその色の dark 値になる | (実装変更なし) | `ios/Tests/KsSettingsViewUITests/CellStyleAppearanceTests.swift` (`test_CellStyleのdynamic色はダーク外観でその色のdark値になる`) | ✅ 一致 |
| CellStyle の固定色は外観で変わらない | (実装変更なし) | `CellStyleAppearanceTests.swift` (`test_CellStyleの固定色は外観切替で変わらず未指定の背景だけがdark既定になる`、および `test_表示中に外観を切り替えるとdynamic色はdark値へ描き直される`) | ✅ 一致 |
| Cell 固有の accent に渡した dynamic 色は trait 変更で再解決される | `ios/Sources/KsSettingsViewUI/KsCheckBoxView.swift` (既存の trait 再解決。本 change では doc の是正のみ) | `ios/Tests/KsSettingsViewUITests/BasicCellsTests.swift` (`test_KsCheckBoxView_dynamicなaccent色が外観切替でdark値へ再解決される`) | ✅ 一致 |
| SwiftUI DSL で外観に応じて選んだ CellStyle 色は差分として行に届く | (実装変更なし) | `ios/Tests/KsSettingsViewSwiftUITests/DSLDiffCalculatorTests.swift` (`test_CellStyleの色だけの変更でreplaceCellが発行される` / `test_CellStyleの色が同じなら差分は発行されない`) | ✅ 一致 |

Requirement 本文の「CGColor など外観を持たない表現へ変換して保持する箇所は trait 変更で再解決する (SHALL)」: Cell 固有色を CGColor 化して保持するのは `KsCheckBoxView` の layer 塗り / 枠のみで、上表 3 行目がそれを固定している。

deviation.md の記録どおり、置き場は指定の `EffectiveStyleResolutionTests` / `ThemeDefaultColorAppearanceTests` ではなく新規 `CellStyleAppearanceTests` になっている (実描画 harness が要るため)。

---

## 対応表: maui-bridge

### Requirement: Cell 単位の色プロパティの変更は表示中の Cell に届く

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 表示中の Cell の TitleColor 変更が Cell 置換として配信される | (facade 変更なし) | `maui/KsSettingsView.Maui.Tests/ThemeAndCellStyleTests.cs` (`TitleColorChangeWhileConnectedIsDeliveredAsCellReplacement`) | ✅ 一致 |
| 表示中の Cell 固有色の変更が Cell 置換として配信される | (facade 変更なし) | `ThemeAndCellStyleTests.cs` (`CellSpecificColorChangeWhileConnectedIsDeliveredAsCellReplacement`) | ⚠️ deviation 記録済み (3 件の `ReplaceCell` ではなく `ReplaceCells` 1 バッチ / 更新 3 件として観測。deviation.md「tasks の手段からの差」7.2) |
| 色プロパティを null に戻すと未指定として配信される | (facade 変更なし) | `ThemeAndCellStyleTests.cs` (`ClearedTitleColorIsDeliveredAsUnspecified`) | ✅ 一致 |
| CustomCell のテキスト系 style 変更は内容更新にならない | (facade 変更なし) | `ThemeAndCellStyleTests.cs` (`CustomCellTextStyleChangeIsNotDeliveredButRowStyleIs`) | ✅ 一致 |
| 外観変更を受けて再代入した Cell の色が native まで届く | `maui/tests/KsSettingsView.MauiHost/SettingsPage.xaml:46-49` / `SettingsPage.xaml.cs:36-37,53-55,105-133` | 自動テストなし (両 OS の実物確認)。証跡 `evidence/maui-host-buttoncell-retheme-ios-{light,dark}.png` / `evidence/maui-host-buttoncell-retheme-android-{light,dark}.png`。規約への追記 `kasane/handbook/maui/integration-host-verification.md:116` | ✅ 一致 |

Requirement 本文が改訂されている (`AppThemeBinding` 経路 → `RequestedThemeChanged` 購読 + 再代入)。deviation.md「仕様との乖離 (オーナー指示)」に 2026-09-06 のオーナー裁定つきで記録済み。

証跡の実在と提出コードとの対応 (process L-003) を確認した: 4 枚とも実在し、画像を開いて目視した結果、light は緑・dark はマゼンタで、`SettingsPage.xaml.cs:36-37` の `LightButtonTitle` (`#FF008000`) / `DarkButtonTitle` (`#FFFF00FF`) と一致する。行の見出し「外観追随ボタン」と ValueText「ライトで緑・ダークでマゼンタ」も `SettingsPage.xaml:47-49` と一致する。

### Requirement: Android bridge の Cell 固有色の変換

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Cell DTO の未指定色は Unspecified になる | `android/kssettingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeColor.kt:20` | `android/kssettingsview-bridge/src/test/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeCellConversionTest.kt` (`色項目が全て null の Cell DTO は Native の未指定になる`) | ✅ 一致 |
| Cell DTO の明示 ARGB は保たれる | 同上 | `KsBridgeCellConversionTest.kt` (`Cell DTO の明示 ARGB は同値の色として保たれる`) | ✅ 一致 |

「DTO の wire 形式は本 change 前と同じ (SHALL)」: bridge の 10 Cell DTO と `KsBridgeCellStyle` / `KsBridgeTheme` の公開フィールドは `Int?` のまま。diff に型・項目の変更はない。

### Requirement: CellStyle DTO が輸送する項目

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| MAUI の PlaceholderColor は Cell 固有段として届く | `bridge/KsBridgeEntryCell.kt:66` + `ui/EffectiveStyle.kt:333-336` | `KsBridgeCellConversionTest.kt` (`MAUI の PlaceholderColor は Cell 固有段として Theme より優先する`) | ⚠️ deviation 記録済み (`EffectiveStyle` が本体 internal のため、解決関数の直接呼び出しではなく Host を実描画して入力欄の hint 色を観測する形。deviation.md「tasks の手段からの差」5.2) |

doc コメントの是正 (Requirement 本文が求める記述の実態化): `android/kssettingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeCellStyle.kt:6-19`、`ios/Sources/KsSettingsViewBridge/KsBridgeCellStyle.swift:11-22`、`maui/KsSettingsView.Maui/Internals/KsCellStyleSnapshot.cs:5-14` の 3 面とも「1 対 1」の記述が輸送項目の実態へ置き換わっている。

---

## 追加検査

### tasks.md の完了状況

全 22 タスクがチェック済み。対応表と突き合わせて**虚偽のチェックは無い**。個別確認:

- 0.1 (spike / 承認ゲート): 結果と証跡ファイル名が本文に記録済み。「届かない」判定の後、オーナー裁定 (2026-09-06) で手段を改めて続行と明記されている。証跡 8 ファイルすべて `evidence/` に実在
- 3.2 (MAUI の `AffectsSnapshot` の確認): コード差分は無い = 漏れが無かったという主張。7.1 / 7.2 の新規テスト 4 件が `TitleColor` / `AccentColor` / `PlaceholderColor` / `AndroidButtonColor` / `CustomCell` の配信を通しで観測して緑になっており、主張と整合する
- 8.2 (描画照合): `ui/verification/` に基準 12 枚 + 最終 12 枚が実在。**独立に照合し直した** — 各組をステータスバー帯 (上端 100px) を除いて画素比較したところ、12 枚すべて差分の bounding box が空だった。`ui/brief.md` の照合記録の主張と一致する
- 8.3 (Swift 6 一時設定ビルド): `git diff -- ios/Package.swift` の差分 0 件を確認した (一時設定の残留なし)
- 8.4 (MauiHost の実物確認): 上表のとおり証跡 4 枚が実在し提出コードと対応する

### 逆流検査 (足場アーティファクトの実装期間中の書き換え)

作業ツリーで書き換わっている足場: `proposal.md` / `exploration.md` / `specs/maui-bridge/spec.md` / `tasks.md` / `ui/brief.md`。

- `specs/maui-bridge/spec.md` と `proposal.md`: Requirement / Scenario の手段を `AppThemeBinding` から `RequestedThemeChanged` 購読 + 再代入へ改訂。**deviation.md 冒頭にオーナー裁定 (2026-09-06) つきで記録済み**であり、承認済み提案の修正として扱われている → 未記録の逆流ではない
- `exploration.md`: 未決論点を「解決済み」へ更新 (探索記録の追記であり契約の書き換えではない)
- `tasks.md` / `ui/brief.md`: 結果記入 (想定される更新)

### 未記録乖離

無し。対応表に ❌ は無く、⚠️ 2 件はいずれも deviation.md に記録済み。

### 付随修正 (deviation.md `[付随修正]` 5 件) の同梱条件

Requirement を持たないため対応表の対象外。ksn-core の同梱条件に照らして確認した結果、5 件とも条件内 (本務で触るファイル内、公開 API・データスキーマ・ADR の決定に触れない、局所的、既存テストで担保)。詳細は review-001.md。

### UI 変更の記録

`ui/brief.md` に「mock は作らない (新規デザインなし)。見た目の正は実装着手前の基準画像」と承認の代替が明記され、照合記録に日付・実行面・手順・判定・合意済み妥協 (なし) が揃っている。
