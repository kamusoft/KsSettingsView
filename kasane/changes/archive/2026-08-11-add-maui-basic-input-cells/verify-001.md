# Verify 001: add-maui-basic-input-cells

- 検証日: 2026-08-11
- 対象: working tree (HEAD `9c2a4c0` との全差分 + untracked 新規ファイル)
- デルタスペック: maui-cells (11 Req / 20 Scenario) / maui-bridge (5 Req / 9 Scenario) / samples-maui (1 ADDED Req + 1 REMOVED Req / 2 Scenario)
- 判定: **VALID** (但し書き 1件 — tasks 7.2 未実施)

---

## 判定サマリ

| 項目 | 結果 |
|---|---|
| Requirement / Scenario の一致 | 17 Requirement / 31 Scenario すべて ✅ または ⚠️ (deviation 記録済み)、❌ ゼロ |
| tasks.md の虚偽チェック | なし (7.2 のみ未チェックで、実際に未実施 — 表記と実態が一致) |
| 足場アーティファクトの逆流 | なし (proposal / design / specs は `9c2a4c0` から無変更) |
| 未記録の乖離 | なし |
| テスト全件成功 | MAUI 241 / iOS 711 / Android 2164 = **3116 件すべて成功** |
| ビルドゲート | 両OS Binding csproj・MAUI サンプルアプリともビルド成功 |

---

## 1. maui-cells 対応表

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| **基本 Cell 6種の公開** | `CommandCell.cs:16-123` / `ButtonCell.cs:19-130` / `SwitchCell.cs:8-67` / `CheckboxCell.cs:8-67` / `RadioCell.cs:15-106` / `SimpleCheckCell.cs:11-70` | `CellShapeTests.BasicCellsExposeTheirOwnState` / `.BasicCellsExposeValueText` | ✅ |
| └ ButtonCell が Description を輸送・表示しない | `ButtonCell.cs:95` (`private new string? Description`)、snapshot 生成で `ButtonCell.cs:117` `Description = null` | `CellShapeTests` (形状) | ✅ |
| Scenario: XAML 直置きと表示反映 | 同上 | tasks 7.1 実機目視 (完了) | ✅ (native 描画のため実機確認が証跡) |
| Scenario: プロパティ変更の反映 | `SwitchCell.cs:64-66` (`AffectsSnapshot`) → `KsSettingsController.cs:788-798, 918-992` (バッチ配信) | `ThemeAndCellStyleTests.CellStyleChangeIsPublished` ほか同経路テスト + tasks 7.1 | ✅ |
| **入力 Cell 5種の公開** | `EntryCell.cs:14-138` / `PickerCell.cs:18-287` / `NumberPickerCell.cs:8-143` / `TimePickerCell.cs:12-103` / `DatePickerCell.cs:14-182` | `CellShapeTests.InputCellsExposeTheirOwnState` (`:142-185`) | ✅ |
| └ EntryCell が `ValueText` 以外の入力値プロパティを持たない | `EntryCell.cs` の BindableProperty は `ValueText`/`Placeholder`/`Keyboard`/`IsPassword`/`TextAlignment`/`MaxLength` + `AccentColor` (スタイル) のみ | `CellShapeTests.InputCellsExposeTheirOwnState:142-150` | ✅ |
| └ Number/Time/DatePicker の状態フィールド | `NumberPickerCell.cs:17-59` / `TimePickerCell.cs:23-42` / `DatePickerCell.cs:17-65` (spec 列挙と過不足なし) | 同上 `:163-184` | ✅ |
| Scenario: 入力 Cell の表示反映 | 同上 | tasks 7.1 実機目視 | ✅ |
| Scenario: 選択面の挙動は native 契約に従う | facade は `IKsInteractionSink` 経由でのみ値変更を受ける (`KsSettingsController.cs:1069-1140`)。非確定 dismiss では native が sink を呼ばない | native 選択面契約 (既存) + tasks 7.1 | ✅ (透過契約。facade 側に介入コードが存在しないことが構造的保証) |
| **タップ通知 (CommandCell / ButtonCell)** | 実効有効: `CommandCell.cs:92` / `ButtonCell.cs:98` (`IsEnabled && _tapCommand.CanExecute(CommandParameter)`)、通知順: `CommandCell.cs:96-105` / `ButtonCell.cs:102-111` (Tapped → Execute)、`KsTapCommand.cs` (CanExecuteChanged 購読) | `TapNotificationTests` 全体 | ✅ |
| └ Tapped を CellBase に公開しない | `Tapped` の宣言は `CommandCell.cs:57` と `ButtonCell.cs:60` のみ。`CellBase.cs` に `Tapped` 出現なし | — | ✅ |
| Scenario: タップで Command 実行 | `CommandCell.cs:96-105` | `TapNotificationTests.CommandCellTapRaisesTappedThenExecutesCommand:16-28` | ✅ |
| Scenario: CanExecute = false で無効化 | `ButtonCell.cs:98` | `TapNotificationTests.CellWithUnexecutableCommandIsDisabledAndIgnoresTap:76-89` | ✅ |
| Scenario: CanExecuteChanged で復帰 | `KsTapCommand.cs` → `ButtonCell.cs:56` | `TapNotificationTests.CanExecuteChangedRestoresEffectiveEnabledState:93-111` | ✅ |
| Scenario: Command 差し替え後は旧 Command の通知を無視 | `KsWeakCommandSubscription.cs`、`CommandCell.cs:33-49` | `TapNotificationTests.ReplacedCommandIsFullyDetached:115-136` | ✅ |
| **双方向バインドの書き戻し** | `KsSettingsController.cs:1014-1140` (10 経路)、`Write<TCell,TValue>`:1176-1189 (間引きなし)、radio group fan-out:1043-1058 | `NativeValueWritebackTests` 全体 | ✅ |
| └ 10プロパティの既定 TwoWay | 個別確認済 (下表) | `CellShapeTests.UserEditablePropertiesDefaultToTwoWayBinding:188-200` / `.OtherPropertiesDefaultToOneWayBinding:204-227` | ✅ |
| Scenario: スイッチ操作が ViewModel へ届く | `KsSettingsController.cs:1014` | `NativeValueWritebackTests.WritebackReachesTwoWayBoundViewModel:252-262` | ✅ |
| Scenario: radio 選択がグループ全体へ反映される | `KsSettingsController.cs:1043-1058` | `NativeValueWritebackTests.RadioSelectionIsAppliedToWholeGroup:157-179` | ✅ |
| Scenario: 再訪問で操作結果が復元される | Store コミット + 再接続経路 | `NativeValueWritebackTests.InteractionsFollowNativeHostLifecycle:281-296`、`InteractionLifetimeTests.ReconnectRestoresInteractions` | ✅ |
| **エコー抑止 (入口同値チェック)** | `KsSettingsController.cs:1169-1189` (`EqualityComparer<TValue>.Default.Equals` で早期 return)、複数選択は `KsWireValues.IndicesEqual` (:1085-1098) | `NativeValueWritebackTests.SameValueNotificationIsIgnored:199-212` / `.WritebackRoundTripConverges:235-248` / `.PickerMultiSelectionIgnoresOrderAndDuplicateOnlyNotifications:216-231` | ✅ |
| Scenario: 同値通知は無視される | 同上 | `SameValueNotificationIsIgnored:199-212` | ✅ |
| **PickerCell の SelectedItem 相互導出** | `PickerCell.cs:218-235` (`SyncSelectedItemFromIndex`) / `:242-259` (`SyncIndexFromSelectedItem`) / `:261-286` (解決) | `PickerSelectedItemTests` 全体 | ✅ |
| Scenario: SelectedItem 設定が index に反映される | `PickerCell.cs:242-259` | `PickerSelectedItemTests.SettingSelectedItemResolvesIndex:13-21` | ✅ |
| Scenario: ItemsSource 未設定時は null | `PickerCell.cs:261-272` | `PickerSelectedItemTests.SelectedItemIsNullWithoutItemsSource:48-54` | ✅ |
| └ 選択確定で index と item の両方更新 | `PickerCell.cs:51-52` (propertyChanged コールバック) | `PickerSelectedItemTests.UserSelectionUpdatesBothIndexAndItem:105-116` | ✅ |
| **DatePickerUIStyle の統一 enum** | `DatePickerCell.cs:68-72,139-143,171`、`KsWireValues.UIStyle`、native 変換は iOS `KsBridgeValueTransport.swift:102-109` / Android `KsBridgeValueTransport.kt:140-144` | `ConversionPathTests.EnumsAreCarriedAsSharedOrdinals:740-754`、iOS `test_uiStyle序数がDatePickerUIStyleへ変換される`、Android `` `uiStyle 序数 1 は Spinner になる` `` | ✅ |
| Scenario: Wheels 指定の両OS 適用 | 上記 ordinal 1 → iOS `.wheels` / Android `Spinner` | 上記 native 変換テスト + tasks 7.1 実機目視 | ✅ |
| **platform 固有プロパティの無視** | iOS gateway は `AndroidButtonColor` を DTO へ写さない (`Platforms/iOS/KsBridgeGateway.cs:350-363` に該当行なし)。iOS Bridge DTO `KsBridgeDatePickerCell.swift` にフィールド自体が存在しない。Android のみ `Platforms/Android/KsBridgeGateway.cs:361` → `KsBridgeDatePickerCell.kt:55` | `CellShapeTests.ColorIsCarriedAsPackedArgb:302-309` | ✅ (iOS 側は値が境界を越える経路が存在しない構造的保証。テスト不要) |
| Scenario: iOS での AndroidButtonColor | 同上 | 同上 | ✅ |
| **IconSource の実体化と反映** | `KsSettingsController.cs:811-829` (`ResolveIcon`) / `:837-852` (`CompleteIcon`、世代番号 latest-wins) / `:861-893` (`StoreIcon`) / `:192-203` (`AttachImages` で接続後再解決) | `IconSourceTests` 全体 | ✅ |
| Scenario: 接続前設定の反映 | `KsSettingsController.cs:192-203, 822-825` | `IconSourceTests.IconSetBeforeConnectIsResolvedOnConnect:19-34` | ✅ |
| Scenario: null 化で icon なし | `KsSettingsController.cs:816-820` | `IconSourceTests.ClearingIconRemovesIt:75-91` | ✅ |
| Scenario: 解決競合は最後の設定が勝つ | `KsSettingsController.cs:837-852` (世代番号) | `IconSourceTests.LatestIconSourceWinsRegardlessOfCompletionOrder:95-111` | ✅ |
| └ 解決失敗時は icon なし | `CompleteIcon` が `lease: null` を透過 | `IconSourceTests.FailedResolutionFallsBackToNoIcon:115-127` | ✅ |
| **Theme 系プロパティの公開と適用** | `SettingsView.cs:89-377` (BindableProperty) / `:449-620+` (アクセサ)、`KsSettingsController.cs:218-226` (`SetTheme`)。native `KsBridgeTheme` の全フィールドに対応 (フォントは Family/Size/Attributes へ分割公開) | `ThemeAndCellStyleTests` 全体、`CellShapeTests.ThemePropertiesDefaultToUnspecified:389-410` (閉じた集合を列挙) | ✅ |
| └ native に対応概念のない項目を公開しない | MAUI 公開面 ↔ iOS/Android `KsBridgeTheme` フィールド集合を突き合わせ、余剰なし (`rowHeight` / `backgroundColor` も対応済み) | 同上 | ✅ |
| Scenario: Theme 色の適用 | `KsSettingsController.cs:218-226` | `ThemeAndCellStyleTests.ThemeSetBeforeConnectIsAppliedOnConnect:19-32` + tasks 7.1 | ✅ |
| Scenario: 表示中の Theme 変更 | 同上 | `ThemeAndCellStyleTests.ThemeChangeWhileConnectedIsApplied:36-46` | ✅ |
| **Cell 単位スタイルの公開と適用** | `CellBase.cs:61-388` (スタイルプロパティ) / `:443-468` (`CreateStyleSnapshot`)、`AccentColor` は対話・選択系 9 Cell のみ | `ThemeAndCellStyleTests.AccentColorIsCarriedByInteractiveCells:164-194` / `.AccentColorChangeIsPublished:198-209`、`CellShapeTests.AccentColorIsExposedByInteractiveCellsOnly:366-381` | ✅ |
| Scenario: Cell の AccentColor 上書き | facade は Cell 値と Theme 値を独立に輸送。実効値解決 (Cell → CellStyle → Theme) は native 既存契約 | 上記 facade 輸送テスト + native 既存解決テスト + tasks 7.1 | ✅ (解決順は native 側の既存契約でありデルタ対象外) |
| **Section.IsVisible** | `Section.cs:37-42,127-131` (既定 true)、`KsSettingsController.cs:657-661` (dirty-tracking) / `:682-720` (`ReplaceSectionKeepingCellIds`) | `SectionVisibilityTests` 全体 | ✅ |
| Scenario: 非表示と復帰 | 同上 | `SectionVisibilityTests.VisibilityChangeIsDeliveredAsSingleReplaceSection:31-45` / `.VisibilityChangeKeepsCellIds:49-70` | ✅ |
| Scenario: 非表示中の内容変更が復帰後に反映される | `KsSettingsController.cs:682-720` | `SectionVisibilityTests.ContentChangedWhileHiddenAppearsOnRestore:94-113` | ✅ |
| Scenario: 切替後も双方向バインドが機能する | cellId 温存により sink 経路が維持 | `SectionVisibilityTests.WritebackKeepsWorkingAcrossVisibilityToggle:74-90` | ✅ |
| **DataTemplateSelector の解決** | `KsItemsSourceBinder.cs` (SettingsView 直下 / Section 配下の両階層で `SelectTemplate(item, container)`) | `DataTemplateSelectorTests` 全体 | ✅ |
| Scenario: Section 生成の出し分け | 同上 | `DataTemplateSelectorTests.SectionsAreGeneratedFromSelectedTemplate:51-61` / `.SectionSelectorReceivesSettingsViewAsContainer:65-73` | ✅ |
| Scenario: item ごとのテンプレート出し分け | 同上 | `DataTemplateSelectorTests.CellsAreGeneratedFromSelectedTemplate:20-34` / `.CellSelectorReceivesOwningSectionAsContainer:38-47` | ✅ |
| └ 例外契約の同一性 | 同上 | `MissingTemplateFails` / `WrongElementTypeFails` / `NestedSelectorFailsInSection` / `NestedSelectorFailsUnderSettingsView` (`:109-160`) | ✅ |

### 10プロパティの既定 TwoWay (個別確認)

`defaultBindingMode: BindingMode.TwoWay` を 10件すべてで直接確認した。例外なし。

| プロパティ | 宣言 |
|---|---|
| `SwitchCell.On` | `SwitchCell.cs:12-17` ✅ |
| `CheckboxCell.Checked` | `CheckboxCell.cs:12-17` ✅ |
| `SimpleCheckCell.Checked` | `SimpleCheckCell.cs:15-20` ✅ |
| `RadioCell.SelectedValue` | `RadioCell.cs:33-38` ✅ |
| `EntryCell.ValueText` | `EntryCell.cs:18-23` ✅ |
| `PickerCell.SelectedIndex` | `PickerCell.cs:45-52` ✅ |
| `PickerCell.SelectedIndices` | `PickerCell.cs:56-61` ✅ |
| `NumberPickerCell.Number` | `NumberPickerCell.cs:40-45` ✅ |
| `TimePickerCell.Time` | `TimePickerCell.cs:23-28` ✅ |
| `DatePickerCell.Date` | `DatePickerCell.cs:25-30` ✅ |

---

## 2. maui-bridge 対応表

| Requirement / Scenario | 実装 (iOS / Android) | テスト | 状態 |
|---|---|---|---|
| **per-type Cell DTO の輸送** | 11種 + Label が両OSに存在し、すべて共通基底 `KsBridgeCell` (iOS `KsBridgeCell.swift:30` / Android `KsBridgeCell.kt:32`) の派生。統合 DTO なし | iOS `KsBridgeCellConversionTests` 11種 / Android `KsBridgeCellConversionTest` 11種 | ✅ |
| └ 基底型での混載 | `KsBridgeSection.cells: [KsBridgeCell]` (iOS `:38` / Android `:38`)、`KsBridgeCellUpdate.cell` (iOS `:22` / Android `:16`)、`KsBridgeRootBuilder.addCell` (iOS `:53` / Android `:50`) | 下記混載テスト | ✅ |
| Scenario: 各 Cell 種の変換 | 同上 | iOS `test_〜DTOが〜へ変換される` ×11 / Android 同名 ×11、描画確認 iOS `test_混載したCellが実描画される:379` / Android `:513` | ✅ |
| Scenario: 異種 Cell の混載 | 同上 | iOS `test_異種CellをsetRootで混載できる:344` / `test_異種CellをreplaceCellsで同一バッチ更新できる:359`、Android `:470` / `:490` | ✅ |
| **単一 interaction delegate / listener** | iOS `KsBridgeInteractionDelegate.swift:22-103` / Android `KsBridgeInteractionListener.kt:14-109`。12メソッド (tap 2・二値 3・radio・entry・picker 単一/複数・number/time/date) が**両OSで同一集合・同一命名** | iOS `KsBridgeInteractionDelegateTests` / Android `KsBridgeInteractionListenerTest` | ✅ |
| └ native UI スレッド上で同期呼び出し | 両OS の relay は `delegate?.method(...)` / `listener?.method(...)` を直接呼び、dispatch queue を介さない (`KsBridgeInteractionRelay.swift` / `.kt`) | 同上 | ✅ |
| └ 未設定・解除後は安全に破棄 | 両OS とも nullable 保持 + `?.` 呼び出し、`dispose()` で null 化 (Android `KsSettingsBridge.kt:130-135`) | iOS `test_delegate未設定の操作は破棄される:171` / `test_delegate解除後〜:180` / `test_dispose後〜:194`、Android `:200` / `:210` / `:226` | ✅ |
| └ facade の回収を妨げない | iOS: `weak var delegate` (`KsBridgeInteractionRelay.swift:20`) + binding `ArgumentSemantic.Weak` (`ApiDefinition.cs:1006-1007`)。Android: 強保持 + 明示解除 (`KsBridgeGateway.cs:163-167` `DetachInteractions`) だが、Cell に注入される閉包が relay と cellId しか捕捉せず facade を root 化しない | iOS `test_delegateは弱参照で保持される:208-218`、facade 側 `InteractionLifetimeTests.ViewIsCollectedWhileInteractionsAreAttached` (SettingsView / gateway 双方の `WeakReference` 回収を固定) | ✅ (下記「所見1」参照) |
| Scenario: スイッチ操作の通知 | 上記 relay | iOS `test_タップと二値変更が対応するメソッドで通知される:84` / Android `:92` | ✅ |
| Scenario: 未設定時は破棄 | 上記 | 上記 | ✅ |
| **値の輸送表現** | 両OS `KsBridgeValueTransport`。時刻 "HH:mm" / 日付 "yyyy-MM-dd" / 二値 Bool / 数値・index Int / keyboard enum Int | iOS `KsBridgeValueTransportTests` / Android `KsBridgeValueTransportTest` | ✅ |
| └ 解釈不能な時刻→00:00 / 日付→1970-01-01、例外なし | iOS `KsBridgeValueTransport.swift:33-51`、Android `KsBridgeValueTransport.kt:68-81`(+`:194`, `:197-207` で `DateTimeParseException` 捕捉)。**両OSで同一結果** | 下記 Scenario テスト | ✅ |
| └ 複数選択の昇順・重複除去 (双方向) | 受信: iOS `:142-144` / Android `:182`。通知: iOS `:148-150` (relay `:63-68`) / Android `:189` (relay `:60-66`) | iOS `test_複数選択indexは順序と重複を問わず同じ集合になる` / `test_通知方向の複数選択indexは昇順になる` (`:140-166`)、Android 同名 (`:182-219`) | ✅ |
| └ 範囲外 index は正規化せず透過 | 両OS ともクランプ処理なし | iOS `test_範囲外の選択indexは正規化せず透過する:168-181` / Android `:222-238` | ✅ |
| Scenario: 不正な日付文字列の無害化 | 上記 | iOS `test_解釈できない日付を持つDTOも他フィールドを反映して構築される:68-78` / `test_解釈できない日付をinsertCellとreplaceCellへ渡しても同じ既定値になる:80-97`、Android `:77-89` / `:92-105` | ✅ (setRoot / insertCell / replaceCell の3経路を網羅) |
| Scenario: 複数選択の順序違いは同値 | 上記 | iOS `test_順序違いの複数選択DTOは同一のNative値になる:149-166` / Android `:198-219` | ✅ |
| Scenario: 日付の往復 | 上記 | iOS `test_時刻と日付の変更がISO文字列で通知される:152-167` / Android `:176-193` | ✅ |
| **DatePickerUIStyle の enum 輸送** | iOS `KsBridgeValueTransport.swift:102-109` (0→`.calendar` / 1→`.wheels` / nil→native 既定)、Android `KsBridgeValueTransport.kt:140-144` (0→`Material` / 1→`Spinner` / else→null)、消費は iOS `KsBridgeDatePickerCell.swift:73` / Android `:71` | 下記 | ✅ |
| Scenario: 未指定は native 既定 | 同上 | iOS `test_DatePickerCellDTOのuiStyle未指定はNative既定になる:237` / Android `` `〜uiStyle 未指定は Native 既定になる` ``:350-358 | ✅ |
| **KsBridgeSection の isVisible 輸送** | iOS `KsBridgeSection.swift:32` / Android `KsBridgeSection.kt:30` (ともに既定 true) | iOS `test_Section_DTOのisVisible既定はtrue:41` / Android `:58` | ✅ |
| Scenario: 非表示 Section の輸送 | 同上 | iOS `test_非表示SectionをsetRootで輸送すると表示から除外される:47` / Android `:64` | ✅ |
| **replaceSection の cellId 温存** | `adoptCellID` (iOS `KsBridgeCell.swift:112-117` / Android `KsBridgeCell.kt:63-67`、canonical UUID 検証付き)、呼び出しは両OS gateway `:192-197` | 下記 | ✅ |
| Scenario: isVisible 差し替え後の通知 ID | 同上 | iOS `test_ID引き継ぎ済みDTOでのreplaceSectionは通知IDを温存する:100-120` / `test_isVisible差し替え後も温存したcellIDで通知される:146-170`、Android `:124-145` / `:174-196` | ✅ |
| Scenario: 別インスタンスへの置換は新規扱い | 同上 (引き継ぎなしで再採番) | iOS `test_ID引き継ぎのないreplaceSectionはcellIDを再採番する:124-143` / Android `:148-169` | ✅ |

---

## 3. samples-maui 対応表

| Requirement / Scenario | 実装 | 検証 | 状態 |
|---|---|---|---|
| **デモページ4画面の追加** | `Pages/BasicCellsDemoPage.xaml(.cs)` / `InputCellsDemoPage` / `UnifyCellCommonFieldsDemoPage` / `VisibilityDemoPage` + 対応 ViewModel 4本、`SampleScreen.cs` (文言一元定義) | 下記 | ✅ |
| └ メニュー文言・画面タイトルの一致 | `SampleScreen.cs:44-60` ↔ `samples/ios/.../SampleScreen.swift` ↔ `samples/android/.../SampleScreen.kt` | 4項目とも文字列完全一致 (「基本 Cell 7 種デモ」「入力 Cell 5 種デモ」「共通フィールド統合デモ」「isVisible デモ（条件付き非表示）」)。3 platform とも title を一元定義から画面タイトルへ供給 | ✅ |
| └ Section / Cell 構成・表示文言・デモデータの一致 | 各 Page.xaml + ViewModel | 4ページ全てについて iOS 側の日本語リテラル (header / footer / Cell タイトル / 選択肢 / 初期値) を MAUI 側 (xaml + xaml.cs + ViewModel) と機械照合し、**欠落ゼロ**。デモデータも一致 (例: `Tanaka Taro` / `tanaka.taro@example.com` / `090-0000-0000` / `secret123` / `yyyy/MM/dd` / 選択肢「ライト・ダーク・自動」「SMS・プッシュ・アプリ内」) | ✅ |
| └ platform 固有 semantic color 不使用・共通色定義と同一値 | `SampleTheme.cs:20-66` | iOS `SampleTheme.swift` / Android `SampleTheme.kt` と**全色リテラル一致** (`#F2EFE6` / `#E6DAB9` / `#50FFBF00` / `#FFBF00` / `#CC9900` / `#999999` / `#555555` / `#FF8D28` / `#CB30E0` / `#00C3D0` / `#FF2D55` / `#34C759` / `#0088FF`)。iOS 側も `UIColor.systemXxx` ではなく数値リテラルで定義済み | ✅ |
| Scenario: 画面集合の一致 | 同上 | MAUI 非保有は Store / DSL デモ (spec の明示例外) と CustomCell デモ (phase-5 追随の片側先行として spec が許容) のみ。それ以外の欠落なし | ✅ |
| Scenario: デモ操作の動作 | 各 ViewModel の TwoWay バインド | tasks 7.1 実機目視 (完了) | ✅ (但し 7.2 の IME 確認は未実施 — 後述) |
| └ アイコン配色の不一致 | — | deviation.md #3 に記録済み (iOS は SF Symbols 青ティント、MAUI は Material Symbols 由来 SVG 黒。サイズ感一致で許容) | ⚠️ deviation 記録済み |
| └ 「ニックネーム (callback)」の経路差 | — | deviation.md #1 に記録済み (MAUI facade は EntryCell の値変更 callback を公開しないため5セルとも TwoWay binding。footer 文言は3 platform 一致を優先し据え置き) | ⚠️ deviation 記録済み |
| **REMOVED: LabelCell 検証ページ** | `Pages/LabelCellVerificationPage.xaml` / `.xaml.cs` / `ViewModels/LabelCellVerificationViewModel.cs` の3ファイルが削除済み (`git status` の `D`) | リポジトリ全文検索で `LabelCellVerification` の残骸ヒット **0件** (kasane/ 記録・openspec/ 凍結資料・ビルド生成物を除く)。`SampleScreen.All` にも `Verification` 区分の項目なし | ✅ |

---

## 4. 追加検査

### 4.1 tasks.md の虚偽チェック

全 41 タスクのうち **40 件がチェック済み、1 件 (7.2) が未チェック**。対応表と突き合わせた結果、チェック済みタスクはすべて実体を伴っており、**未実装なのにチェック済みの虚偽は検出されなかった**。

- 2.x / 3.x (両OS Bridge): 11種 DTO・delegate/listener・値変換・isVisible・cellId 温存とも実装・テストを確認
- 4.x / 5.x (facade + テスト): 13実装タスク・10テストタスクとも対応する実体を確認
- 6.x (サンプル): 4ページ追加 + LabelCell 検証ページ削除を確認
- 8.1 (テスト全件): 本検証で再実行し全緑を確認 (下記 4.4)
- 8.2 (ビルド): 本検証で再実行し成功を確認 (下記 4.5)
- 9.x (引き継ぎ評価): review 由来の評価タスク。成果物は review-001/002 に反映済み

**7.2 (EntryCell の日本語 IME 実機確認) は未チェックのままで、実態 (オーナー実機待ち) と表記が一致している。** 虚偽ではない。

### 4.2 足場アーティファクトの逆流検査

`git diff HEAD -- kasane/changes/add-maui-basic-input-cells/` の結果、**変更されたのは `tasks.md` のみ** (チェックボックス更新 52行)。`proposal.md` / `design.md` / `specs/**` は提案作成コミット `9c2a4c0` から一切書き換えられていない。**逆流なし**。

### 4.3 未記録乖離

deviation.md の 4件 (要求は5件だが、ファイルには4項目が記載 — 下記「所見2」) はいずれもコード上の実体を確認した:

| deviation | コード上の実体 | 確認 |
|---|---|---|
| #1 ニックネーム (callback) デモの経路差 | MAUI `EntryCell` に値変更 callback / event の公開なし | ✅ |
| #2 Android EntryCell パスワードマスク修正 | `EntryCellViewHolder.kt` に `passwordInputType()` 追加 (variation を消してから書き込む方式)。`InputCellsTest.kt` にテスト追加 | ✅ |
| #3 サンプルのアイコン配色 (iOS) | サンプル資産の差。spec の配色一致 SHALL に対する明示的許容 | ✅ |
| #4 Section の header 高さ | facade `Section.cs:44-46,134-137` + iOS `KsBridgeSection.swift:35,86` + Android `KsBridgeSection.kt:33,65`。`SectionHeaderHeightTests.cs` / iOS `KsBridgeSectionHeaderHeightTests.swift` / Android `KsBridgeSectionHeaderHeightTest.kt` | ✅ |

**deviation.md に記録のない乖離は検出されなかった。**

### 4.4 テスト全件実行

| 対象 | コマンド | 結果 |
|---|---|---|
| MAUI facade | `dotnet test maui/KsSettingsView.Maui.Tests -f net10.0` | **241 件成功 / 0 失敗 / 0 スキップ** |
| iOS | `xcodebuild test -scheme KsSettingsView-Package` (Xcode 26.1.1) | **711 件成功 / 0 失敗** (Bridge 101 / Core 83 / SwiftUI 76 / UI 451)、`** TEST SUCCEEDED **` |
| Android | `./gradlew test --rerun-tasks` | **2164 件成功 / 0 失敗 / 0 エラー / 0 スキップ**、`BUILD SUCCESSFUL` |

合計 **3116 件すべて成功**。コンテキストパッケージ記載の期待件数と完全一致。

### 4.5 ビルドゲート (tasks 8.2)

| 対象 | 結果 |
|---|---|
| `maui/macios/KsSettingsView.Binding.iOS` | ビルド成功 (0 警告 / 0 エラー) |
| `maui/android/KsSettingsView.Binding.Android` | ビルド成功 (0 警告 / 0 エラー) |
| `samples/maui/KsSettingsView.Sample.Maui` | ビルド成功 (0 警告 / 0 エラー) ※ `DEVELOPER_DIR=/Applications/Xcode-26.1.1.app/...` 必須 |

### 4.6 UI 変更のモック承認

本変更は `ui/` アーティファクトを持たない (facade API 追加とサンプル画面追加であり、新規モック設計を伴わない)。サンプル画面の「見た目の正」は iOS / Android の既存サンプルであり、その一致は tasks 7.1 の実機目視で確認済み。

---

## 5. 所見 (判定に影響しない記録)

**所見1: delegate / listener の保持強度が両OSで非対称。**
iOS は真の弱参照 (`weak var delegate` + binding の `ArgumentSemantic.Weak`) で回収非阻害を保証し、専用テスト (`test_delegateは弱参照で保持される`) がある。Android は強保持 + 明示解除で、非保持を直接検証する Bridge テストがない。ただし spec の SHALL NOT は「保持が facade の回収を妨げないこと」という結果要件であり、Android でも Cell 閉包が relay と cellId しか捕捉しないため facade は root 化されず、facade 側 `InteractionLifetimeTests.ViewIsCollectedWhileInteractionsAreAttached` が SettingsView / gateway 双方の回収を `WeakReference` で固定している。要件は充足しており **❌ ではない**。この非対称は review-001 (`:299`) で既に確認・許容済み。

**所見2: deviation.md の項目数。**
コンテキストパッケージは「deviation.md (5件)」としているが、ファイルには **4項目**が記載されている。いずれも実体を確認済みで未記録乖離はないため判定には影響しないが、件数認識のずれとして記録する。

---

## 6. 判定

**VALID**

17 Requirement / 31 Scenario のすべてが「✅ 一致」または「⚠️ deviation 記録済み」で、❌ はゼロ。tasks.md に虚偽チェックなし、足場アーティファクトの逆流なし、未記録の乖離なし、テスト 3116 件全緑、ビルドゲート全通過。

**但し書き:** tasks 7.2 (EntryCell の連続入力 — 日本語 IME・カーソル位置の実機確認) のみ未実施で、オーナー実機待ちの既知保留。「双方向バインドの書き戻し」Requirement の自動テストによる検証は完了しているが、IME 合成中のカーソル挙動という実機でしか観測できない側面は未確認のまま残る。アーカイブ判断の際はこの1点を明示的に引き受けるか、実機確認後に消化するかをオーナーが選ぶこと。
