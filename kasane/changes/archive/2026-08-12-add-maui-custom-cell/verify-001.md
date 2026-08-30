# Verify 001: add-maui-custom-cell

**判定**: INVALID (未記録の乖離 1 件)

検証日: 2026-08-12 / 対象: `kasane/changes/add-maui-custom-cell/specs/` の 3 デルタスペック (maui-cells 11 Requirement / 25 Scenario、maui-bridge 4 Requirement / 5 Scenario、samples-maui 2 Requirement / 7 Scenario) — 計 17 Requirement / 37 Scenario。

検証対象の実装は working tree の HEAD (develop) に対する全変更 (追跡 31 ファイル + 未追跡の新規群)。E2E でしか観測できない Scenario は `screenshots/` の最終版証跡 (iOS `ios-final2-*` / `ios-final3-reconnect-*` / `ios-fix5-*`、Android `android-reverify-*` / `android-tapfix-*` / `android-tap-*`) を対応物として扱った。

`deviation.md` 記録済みの 3 件 (①共有 Style Scenario の読み替え ②ReleaseHost 時の空世代再発行 ③iOS 埋め込み形) は合意済み差分として ⚠️ 扱いとし、違反にしていない。

---

## 1. テスト実行結果 (全件実行して確認)

| platform | コマンド | 結果 |
|---|---|---|
| MAUI | `dotnet test KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj -f net10.0` | **400 tests / 0 failures** |
| iOS | `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` | **476 tests / 0 failures** (`** TEST SUCCEEDED **`) |
| Android | `./gradlew test --rerun-tasks` → `build/test-results/test*UnitTest/TEST-*.xml` 集計 | **2320 tests / 0 failures / 0 errors** |

いずれもオーケストレーターの申告値と一致。件数の得方は `concepts/cross/conventions/test-execution.md` に従った (iOS は Simulator 実行、Android は `--rerun-tasks` + XML 集計)。

---

## 2. 対応表: maui-cells (25 Scenario)

### Requirement: CustomCell の配置と Content の表示

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| XAML 直書きの Content が行に表示される | `maui/KsSettingsView.Maui/CustomCell.cs:33` (`[ContentProperty]`) / `:49` (`ContentProperty`) / `:125` (`Content`)、`Internals/KsSettingsController.cs:792` (`PlaceCellContent`) `:375` (`FindCellContentView`)、`Platforms/iOS/KsBridgeGateway.cs` ToDto → `custom.View`、Android 同型 | `CustomCellTests.cs:40`、`CustomCellContentTests.cs:28` `:62` | ✅ |
| 派生サブクラスが同様に描画される | `CustomCell.cs:34` (非 sealed public)、`:203` (`CreateSnapshot` → `KsCustomCellSnapshot`) | `CustomCellTests.cs:79` (`SliderCell : CustomCell`) | ✅ |
| (Requirement 文) Content が null の間は空内容の行 | `CustomCell.cs:49` (既定 null)、gateway は実体なしで `View = null` | `CustomCellTests.cs:68`、iOS `KsBridgeCustomCellTests.swift:211`、Android `KsBridgeCustomCellTest.kt:292` | ✅ |

E2E: `ios-final2-parity-01-top.png` / `android-tapfix-*` (① インライン Section の 2 行が表示)。

### Requirement: 内容変化の live 反映と Content の差し替え

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| バインド値の変更が再設定なしで反映される | `KsSettingsController.cs:846` (`IssueContentToken` は Content 差し替え時のみ)、`CustomCell.cs:218` (`AffectsSnapshot` に Content を含めない) | `CustomCellContentTests.cs:148` `:171` | ✅ |
| 別 View への差し替えで表示が置き換わる | `KsSettingsController.cs:758` (`SetCellContent`) `:1459` (Content 変更通知)、iOS `.id(token)` / Android `key(token)` | `CustomCellContentTests.cs:192` `:213`、iOS `:275`、Android `:359` | ✅ |
| 同一 View の往復差し替えが成立する | `KsSettingsController.cs:763` (先行破棄 → 再実体化) | `CustomCellContentTests.cs:230`、iOS `:299`、Android `:385` | ✅ |
| null への差し替えで空内容になり View は再利用できる | `CustomCell.cs:60` (`KsAccessoryViewOwnership.ReassignIfFree`)、`KsSettingsController.cs:943` (`RetireCellContent`) | `CustomCellContentTests.cs:250` `:734` | ✅ |

E2E: `ios-final2-specific-06-content-swapped-B.png` / `-07-content-null.png` / `-08-content-restored.png`、`android-reverify-specific-02` / `-03`。

### Requirement: 構造的な除去で Content の所有と表示資源は解放される

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Cell 削除後の View 再利用が成立する | `KsSettingsController.cs:1012` / `:1984` (`ContentGuard = null` + `RetireCellContent`)、`:943` | `CustomCellContentTests.cs:277` `:299` `:313` `:328` `:370` `:713` `:734` | ✅ |
| ItemsSource からの除去で行と資源が解放される | 同上 (生成行の退役経路) | `CustomCellContentTests.cs:342` | ✅ |

### Requirement: Content は所有 Cell の BindingContext を継承する

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| ItemTemplate 生成の CustomCell は item を継承する | `KsSettingsController.cs:1971` (`PlaceCellContent` で logical tree 接続) | `CustomCellContentTests.cs:431` `:460` | ✅ |
| BindingContext の変更が Content へ伝播する | 同上 (Element 親子による継承) | `CustomCellContentTests.cs:404` (+ `:390` `:418` 明示 BindingContext 非上書き) | ✅ |

E2E: `ios-final3-reconnect-02-restored.png` / `android-reverify-specific-04` (テンプレート行 A/B/C が独立)。

### Requirement: 行タップは Command / Tapped で通知される

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 行タップで Command が発火する | `CustomCell.cs:187` (`NotifyTapped` — `Tapped` → `Command` 順)、`KsSettingsController.cs:1718` (`CustomCellTapped`)、`Internals/IKsInteractionSink.cs` 追加 | `CustomCellTests.cs:246` `:262`、iOS `:379`、Android `:469` | ✅ |
| content 内の操作はタップを消費し二重発火しない | Android `KsBridgeCellContentView.kt` (`detectTapGestures` — 埋め込みがタッチを引き取った場合はポインタ変化が消費済みで検出が始まらない)、iOS は行の tap 経路のまま | Android `KsBridgeCustomCellTest.kt:544` | ✅ |
| 未設定なら content 内部の操作を妨げない | `CustomCell.cs:167` (`HasTapHandler`)、iOS/Android とも `onTap` を nil/null で構築 | `CustomCellTests.cs:332`、iOS `:95` `:404`、Android `:216` `:489` `:566` | ✅ |
| 表示後の Command 設定が行タップ動作に反映される | `CustomCell.cs:71` / `:103`-`:118` (購読変化の通知)、`:218` (`AffectsSnapshot` に `HasTapHandler`) | `CustomCellTests.cs:342` `:384` `:402`、iOS `:423`、Android `:503` `:583` | ✅ |
| CanExecute=false の間は発火しない | `CustomCell.cs:161` (`IsEffectivelyEnabled`)、`:95` (`KsTapCommand` の `CanExecuteChanged` 追従) | `CustomCellTests.cs:315` `:366` | ✅ |

E2E: `ios-final2-parity-09-rowtap-count2.png` / `-05-pill-tap-no-double-fire.png`、`android-tapfix-01`〜`-04` (行タップ 2 回 → ピルタップで 0 に戻り二重発火なし)。

### Requirement: ShowArrowIndicator で Disclosure Indicator を表示する

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| true で indicator が表示される | `CustomCell.cs:84` / `:209`、`Internals/KsCellSnapshots.cs:39`、iOS `KsBridgeCustomCell.swift` → `CustomCell(showArrow:)`、Android `KsBridgeCustomCell.kt` 同型、`ApiDefinition.cs` に `showArrowIndicator` | `CustomCellTests.cs:92` (Command と独立)、iOS `:76`、Android `:198` | ✅ |

E2E: `ios-final2-parity-08-section4-showarrow-ontap.png` / `android-tapfix-04` — 「詳細設定 (showArrow: true)」と基準行「詳細設定 (CommandCell)」が chevron を同一素材・同一位置で並べて表示し、content が indicator 領域を除いた範囲に収まっていることを目視確認。

### Requirement: IsEnabled / IsVisible の挙動

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 無効時は content 内部の操作も抑止される | `CustomCell.cs:206` (`IsEnabled = IsEffectivelyEnabled` を写す)、抑止自体は native CustomCell 契約 | `CustomCellTests.cs:146` `:300`、Android `KsBridgeCustomCellTest.kt:616` | ✅ |
| IsVisible=false で行が出力されない | `CustomCell.cs:207`、既存 visible projection 経路 | `CustomCellTests.cs:121`、iOS `KsBridgeSectionVisibilityTests.swift` / Android `KsBridgeSectionVisibilityTest.kt` (CustomCell を追加) | ✅ |

E2E: `android-tap-12/13`・`android-tapfix-06` (無効行のスライダーをドラッグしても値不変)、`ios-final2-parity-01-top.png` (「無効」行が無効の視覚状態)。

### Requirement: 継承プロパティのうち不適用のものは silent no-op

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Title を設定しても表示に現れない | `CellBase.cs:449` (`CreateRowStyleSnapshot` — Height / BackgroundColor のみ)、`CustomCell.cs:203` (Title/Description/Hint/Icon を写さない)、`:218` (`AffectsSnapshot` から除外)、`:11`-`:32` (XML doc に不適用一覧) | `CustomCellTests.cs:190` (写しに載らず配信も起きない) | ✅ |
| 共有 Style の適用が例外にならない | 同上 | `CustomCellTests.cs:221` (`SharedStyleValuesTakeEffectOnlyWhereTheyApply`) | ⚠️ deviation #1 記録済み |

### Requirement: 行高さは Content の self-sizing に追従する

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 表示中のサイズ変化に行高さが追従する | probe で「追加通知なしで両 OS 追従」を確定 (`kasane/roadmaps/maui-support/phases/phase-5-custom-cell/artifacts/probe/2026-08-12-cell-content-size-follow.md`)。iOS `KsBridgeCellContentView.swift` の `sizeThatFits` でサイズ中継、Android は `AndroidView` の `requestLayout` 伝播 | iOS `KsBridgeCellContentHostViewTests.swift:260` `:274`、Android `KsBridgeCustomCellTest.kt:404`、MAUI `CustomCellContentTests.cs:171` (計測無効化で送り直さない) | ✅ |

E2E: `ios-fix5-height-01/02/03(.mov)`・`ios-fix5-size-01/02/03(.mov)`、`android-reverify-parity-03/04(.mp4)/05`・`android-reverify-specific-05/06(.mp4)/07` (変化前・遷移中・変化後を保存)。

### Requirement: 同一 View インスタンスの多重配置は例外になる

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 同一インスタンスを2つの CustomCell へ設定すると例外 | `CustomCell.cs:54` (`validateValue` → `ContentGuard.EnsureContentCanBePlaced`、false 返却ではなく `InvalidOperationException` 送出)、`KsSettingsController.cs:381` `:2190` `:2201` `:2213`、バッチ事前検査 `:1418` `:2130` (`EnsureCellContentsAreFree`) | `CustomCellContentTests.cs:500` `:517` `:530` `:542` `:559` `:583` `:624` `:660` `:693` | ✅ |
| null 解除後の再利用は許容される | `CustomCell.cs:60` (`ReassignIfFree`) | `CustomCellContentTests.cs:250` `:734` `:713` | ✅ |

### Requirement: Handler 切断・再接続をまたいで CustomCell は復元される

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 再訪問で CustomCell が復元される | `Handlers/SettingsViewHandler.cs:121` (`ApplyHostViews`)、`SettingsView.cs:829`、`KsSettingsController.cs:284` / `:910` (`ReleaseCellContentViews`) | `CustomCellContentTests.cs:754`、`:780` (ReleaseHost 時の空世代再発行) | ✅ (`:780` は ⚠️ deviation #2 の実装) |
| 切断中の Content 差し替えが再接続後に反映される | `KsSettingsController.cs:810` (`ApplyCellContent` で再接続時に新トークン再発行) | `CustomCellContentTests.cs:796` | ✅ |

E2E: `ios-final3-reconnect-01`〜`-04`、`android-reverify-specific-08/09`、`android-reverify-parity-07`。`ios-final3-reconnect-02-restored.png` に「③ 離脱中に差し替えた Content (1 回目)」と再接続の記録が写っており、切断中差し替えの反映を直接確認できる。

---

## 3. 対応表: maui-bridge (5 Scenario)

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| **KsBridgeCustomCell で輸送** / 構造更新で custom cell が表示される | iOS `ios/Sources/KsSettingsViewBridge/KsBridgeCustomCell.swift` (view / contentToken / showArrowIndicator / hasTapHandler)、Android `KsBridgeCustomCell.kt` 対称、`maui/macios/KsSettingsView.Binding.iOS/ApiDefinition.cs:196`〜、MAUI 側 `Platforms/*/KsBridgeGateway.cs` の `Custom(snapshot)` + `FindCellContentView`、full-bleed は iOS `sizeThatFits` の幅そのまま中継 / Android `Modifier.fillMaxWidth()` | iOS `:128` (setRoot) `:189` (replaceCells) `:211` (view 未指定 = 空内容)、Android `:241` `:272` `:292` | ✅ |
| **トークンでのみ差し替わる** / 同一トークンの再発行では view インスタンスが維持される | iOS builder の `.id(token)`、Android `key(token)`。native content にはトークンのみ格納 (等価性はトークンの値等価) | iOS `:235` (materialize 0 / dispose 0 を計測、同時変更した有効状態の到達で再バインド発生を裏付け)、Android `:317` | ✅ |
| 〃 / トークン変更で view が置き換わる | 同上 | iOS `:275` (materialize 1 / dispose 0)、Android `:359` | ✅ |
| **返す前に親から切り離す** / スクロールによるリサイクルで表示が壊れない | iOS `KsBridgeCellContentView.swift` + `KsBridgeCellContentHostView.swift` (行ごとの入れ物 + 引き取り規則)、Android `KsBridgeCellContentView.kt` の factory 内 `removeView` | iOS `:171` `:321` + `KsBridgeCellContentHostViewTests.swift` 全 14 件、Android `:257` `:423` | ⚠️ deviation #3 記録済み (spec Requirement 自体は充足) |
| **単一 delegate / listener へ通知** / タップ通知が Cell ID 付きで届く | iOS `KsBridgeInteractionDelegate.swift` + `KsBridgeInteractionRelay.swift` に `customCellTapped`、Android `KsBridgeInteractionListener.kt` + `Relay.kt` 同型、`ApiDefinition.cs` に `[Abstract] CustomCellTapped`、MAUI `IKsInteractionSink.CustomCellTapped` → gateway 転送。購読なしは `onTap` を nil/null で構築。書き戻しなし | iOS `:379` `:404` + `KsBridgeInteractionDelegateTests.swift`、Android `:469` `:489` + `KsBridgeInteractionListenerTest.kt`、MAUI `CustomCellTests.cs:276` | ✅ |

E2E (リサイクル): `ios-fix5-scroll-01/02/03` (高速フリック → 遅いドラッグの組を 3 セッション、空行検出 0)、`ios-final2-scroll-s1`〜`s4` (計 22 枚)、`android-reverify-scroll-01/02`。

---

## 4. 対応表: samples-maui (7 Scenario)

### Requirement: パリティ画面 CustomCellDemo を native と同一構成で提供する

| Scenario | 実装 | 証跡 | 状態 |
|---|---|---|---|
| メニューからパリティ画面を開ける | `samples/maui/KsSettingsView.Sample.Maui/SampleScreen.cs:56`-`59` (`SampleScreenCategory.Demo` / 文言「CustomCell デモ」/ 位置は「入力 Cell 5 種デモ」の次 = native と同順)、`Pages/CustomCellDemoPage.xaml` の 5 Section | `android-maui-menu.png` ↔ `android-native-menu.png` (文言・区分・相対順が一致)、`ios-final2-parity-01-top.png` ↔ `ios-final2-native-parity-01-top.png` (Section ヘッダ / フッタ / Cell 文言 / スライダー初期値 70・40・60 / 無効行がすべて一致) | ✅ |
| インライン構成の live 更新が動作する | — (下記 6 節を参照) | `ios-final2-parity-05-pill-tap-no-double-fire.png`、`android-tapfix-03` | ❌ |
| スクロール耐性構成で表示が混線しない | `CustomCellDemoPage.xaml:104`-`120` (`ItemsSource` + `ItemTemplate` の同型 CustomCell)、`ViewModels/CustomCellDemoViewModel.cs:19` (`DummyRowCount = 40`、native の 40 行と一致) | `ios-fix5-scroll-01/02/03`、`ios-final2-scroll-s1`〜`s4`、`android-reverify-scroll-01/02` | ✅ |

構成の内訳 (spec の 5構成に対する充足): ①インライン = `xaml:27`-`48`、②ラップ再利用 = `xaml:51`-`57` (`Views/SampleSliderCell.cs:18` の `SampleSliderCell : CustomCell` — spec が許す「CustomCell 派生」の側)、③挙動プロパティ = `xaml:60`-`71` (動的高さ) と `xaml:74`-`101` (showArrow / onTap) の 2 構成、④スクロール耐性 = `xaml:104`-`120`。Section ヘッダ・フッタ 5 組は native (iOS `CustomCellDemoView.swift` / Android `CustomCellDemoScreen.kt`) と全角括弧・句点・en dash に至るまで一字一句一致。

### Requirement: MAUI 固有の CustomCell デモを別画面で提供する

| Scenario | 実装 | 証跡 | 状態 |
|---|---|---|---|
| 差し替えデモが動作する | `Pages/CustomCellMauiSpecificDemoPage.xaml:20`-`32` + `.xaml.cs:58`-`71` (A ⇔ B トグルと null ⇔ 復元) | `ios-final2-specific-06/07/08`、`android-reverify-specific-02/03` | ✅ |
| ItemTemplate 生成の行が独立して動作する | `xaml:35`-`55` (`ItemsSource` + `DataTemplate`)、`ViewModels/CustomCellMauiSpecificDemoViewModel.cs:36`-`40` `:89`-`121` (行ごとの `Count` / `IncrementCommand`) | `ios-final2-specific-09`、`android-reverify-specific-04`、`ios-final3-reconnect-02-restored.png` | ✅ |
| 再訪問で復元される | `xaml:58`-`67` + `.xaml.cs:73`-`90` (`PopAsync` → Handler 切断確認 → 離脱中に Content 差し替え → `PushAsync` で同一ページインスタンス) | `ios-final3-reconnect-01`〜`-04`、`android-reverify-specific-08/09`、`ios-fix5-specific-04-reconnect-restored.png` | ✅ |
| サイズ変化デモで行高さが追従する | `xaml:70`-`90` (View を置き直さず `GrowingText` を 1 行 ⇔ 3 行に切り替え) | `ios-fix5-size-01/02/03(.mov)`、`ios-final2-specific-02`〜`-05`、`android-reverify-specific-05/06(.mp4)/07` | ✅ |
| (Requirement) ルートメニューの「MAUI 固有」区分に追加 | `SampleScreen.cs:72`-`75` (`SampleScreenCategory.MauiSpecific`、直前が `AccessoryViewsDemoPage` で登録形式も同一) | `android-maui-menu.png` | ✅ |

---

## 5. 追加検査

| 項目 | 結果 |
|---|---|
| tasks.md の全タスク完了 | 1.1–1.2 / 2.1–2.9 / 3.1–3.4 / 4.1–4.4 / 5.1–5.3 / 6.1–6.2 / 7.1–7.4 の全 26 タスクが `[x]`。**虚偽チェックなし** — 対応表と突き合わせて全タスクに実体を確認した。1.2 (再計測通知の追加分岐) は probe 記録の「判断」節で明示的に不採択とされ、native 公開 API 変更が実際に発生していないことを diff で確認 |
| 逆流検査 (足場の書き換え) | **なし**。`proposal.md` / `design.md` / `specs/` は起案コミット `cbaab09` 以降コミットされておらず、working tree でも未変更 (`git status` にクリーン)。変更されているのは進捗記録である `tasks.md` のみ |
| 未記録乖離 | **1 件** (下記 6 節) |
| UI 変更の brief / モック | 本変更に `ui/` アーティファクトはなく、代わりに native パリティ画面を「見た目の正」として E2E 照合 (tasks 7.4) している。パリティ照合の証跡は `ios-final2-native-parity-01`〜`-04` / `android-reverify-native-parity-01/02` と MAUI 側の対応枚で揃っている |
| テスト全件成功 | ✅ (1 節。MAUI 400 / iOS 476 / Android 2320、いずれも 0 failures) |

---

## 6. ❌ の詳細 (未記録の乖離 1 件)

### samples-maui / Scenario「インライン構成の live 更新が動作する」

**事実**: パリティ画面の Section ①「インライン CustomCell」(`samples/maui/KsSettingsView.Sample.Maui/Pages/CustomCellDemoPage.xaml:27`-`48`) は、`SampleAccentRow` + 固定文言のピルの行と、静的 `Label` の行の 2 行だけで構成され、**操作可能な要素も `{Binding}` も 1 つも持たない**。したがって Scenario の WHEN「content 内の操作でバインド値を変更する」を Section ① で文字どおり実行できない。

同 Requirement の parity SHALL に従った結果であり、native も同じ (iOS `CustomCellDemoView.swift:92`-`114` / Android `CustomCellDemoScreen.kt:80`-`105` とも操作要素なしの静的 2 行)。

**THEN 自体は成立している**: XAML に直書き (インライン宣言) された CustomCell のうち、Section ④ の「行タップカウンタ」行 (`xaml:90`-`100`) が、content 内のピルのタップ → `ResetRowTapCountCommand` → `RowTapCountText` → **同じ行のピル表示が即時更新**、という経路をそのまま持つ。E2E 証跡も `ios-final2-parity-05-pill-tap-no-double-fire.png` / `android-tapfix-03-maui-pill-tap-count0-no-double-fire.png` にある。Section ⑤ のダミー行 (`ToggleCommand` → `TagText`) も同型。

**なぜ ❌ か**: 「Scenario の WHEN が文字どおりには成立せず、読み替えで THEN の観測を担保している」という形は、既に deviation.md #1 (共有 Style Scenario) として記録された乖離と**同種**である。同じ基準を当てるなら本件も記録対象だが、deviation.md に記載がない。

**見立て**: **deviation として合意すべき** (実装修正は不適切)。Section ① に操作要素を足すと、同じ Requirement の「native と表示文言・Cell 構成を一致させる」SHALL に正面から反する。妥当な落とし所は、deviation.md へ「Scenario の『インライン構成』を『インライン宣言された CustomCell』と読み替え、Section ④ の行タップカウンタで THEN を担保した。理由: Section ① に操作要素を持たせると parity SHALL に反するため」と 1 行記録すること。合意はユーザーの判断であり、本検証では書き込んでいない。

---

## 7. 判定に影響しない観測事項

- **② の Header / Footer 文言と実装形態の齟齬**: Section ② のヘッダ「再利用（SliderCell ラップ関数）」/ フッタ「SliderCell(label:value:) 関数が CustomCell を返す再利用例。」は native の文言そのままだが、MAUI 実装は関数ではなく派生クラス `SampleSliderCell : CustomCell`。ただしこれは spec が両方を明示的に要求した結果 (「表示文言を一致させる」SHALL と「CustomCell 派生 (または CustomCell を返すヘルパー) で native のラップ関数に対応させる」SHALL) であり、乖離ではない。
- **iOS の MAUI サンプルメニューのスクリーンショットが未取得**: `screenshots/` にあるメニュー証跡は `android-maui-menu.png` / `android-native-menu.png` / `ios-final2-native-menu.png` の 3 枚で、iOS 側の MAUI メニューがない。ただしメニューは platform 非依存の `SampleScreen.cs` 一元定義であり、Android 側で MAUI ↔ native の文言一致を確認済みのため、Scenario の充足には影響しない。
- **content 値型の不在**: native は `SampleSyncState` 等を content として持つが、MAUI facade の `Content` は `View` 型のため、①③④ の subtitle は固定文字列。表示文言は一致しており、parity SHALL の観点で問題はない。

---

## 8. まとめ

- ✅ 一致: 34 Scenario
- ⚠️ deviation 記録済み: 2 Scenario (maui-cells「共有 Style の適用が例外にならない」、maui-bridge「スクロールによるリサイクルで表示が壊れない」。deviation #2 は Scenario ではなく Requirement 内の実装追加として `CustomCellContentTests.cs:780` に対応)
- ❌ 未記録の乖離: 1 Scenario (samples-maui「インライン構成の live 更新が動作する」)

虚偽チェック・逆流・テスト失敗はいずれもなし。❌ 1 件は deviation.md への 1 行追記で解消する性質のもので、コード修正を要する欠落ではない。追記が合意されれば VALID に移る。
