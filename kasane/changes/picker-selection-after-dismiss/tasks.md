# Tasks: picker-selection-after-dismiss

## 1. iOS UI (ios/Sources/KsSettingsViewUI)
- [ ] 1.1 `PickerCell` に `onSelectionCompleted` / `onMultiSelectionCompleted` を追加する。プロパティ宣言・internal 全部入り init・公開 init 4 本 (単一 Store / 単一 DSL / 複数 Store / 複数 DSL)・object 射影の init 群 (`PickerCell+ItemProjection.swift`、String 特殊化を含む全 init) の省略可能引数・`with*` 系再構築の引き継ぎ (→ Requirement: PickerCell 単一選択の閉じ切り通知 (iOS) / PickerCell 複数選択の閉じ切り通知 (iOS) / PickerCell の閉じ切り callback はすべての構築経路と再構築で保持される (iOS))
- [ ] 1.2 `PickerListViewController` に `onSingleCompleted` / `onMultiCompleted` を追加し、`dismissModal(completion:)` を確定経路だけ completion 付きで呼ぶ。キャンセル経路は completion を渡さない。`PickerCellView` で Cell の新 callback に結線する (→ Requirement: PickerCell 単一選択の閉じ切り通知 (iOS) / PickerCell 複数選択の閉じ切り通知 (iOS)) — design Decision 3
- [ ] 1.3 `DatePickerCell` に `onValueCompleted` を追加する (値 init と Binding init の両方、`withDSLID` / `withStyle` / `withIcon` の転送) (→ Requirement: DatePickerCell の閉じ切り通知 (iOS))
- [ ] 1.4 `DatePickerCalendarSheetController` に `onDoneCompleted` を追加し、Done の dismiss completion で確定日付を渡して呼ぶ。`DatePickerCellView` で `onValueCompleted` に結線する (→ Requirement: DatePickerCell の閉じ切り通知 (iOS)) — design Decision 4
- [ ] 1.5 `DatePickerCellView.handleWheelsDone()` でキーボード非表示完了通知の一回限りの購読を張り、通知で `onValueCompleted` を呼ぶ。`resignFirstResponder()` が false のときはその場で呼ぶ (→ Requirement: DatePickerCell の閉じ切り通知 (iOS) / Scenario: ホイールの Done) — design Decision 4
- [ ] 1.6 テスト: `PickerSelectionScreenTests` に単一 / 複数の「値 callback → 閉じ切り callback の順序と同値」「Cancel / 対話的 dismiss で不発火」「トグルで不発火」を追加。`InputCellsTests` に DatePicker カレンダー / ホイールの順序と不発火、Binding 経路・object 射影 init・`with*` 再構築での保持 (Picker / DatePicker とも) を追加 (→ 上記全 Requirement の各 Scenario)。ホイールのキーボード非表示完了通知はテストホストで自然に届かない場合があるため、テストから通知を投稿して発火を検証してよい。待機は handbook `cross/test-execution.md` の条件ベース待機で書く

## 2. Android UI (android/kssettingsview)
- [ ] 2.1 `PickerCell` data class に `onSelectionCompleted` / `onMultiSelectionCompleted`、`DatePickerCell` に `onValueCompleted` を追加する。object 射影の factory 群 (`PickerCellItemProjection.kt`) と Compose DSL の overload (`compose/InputCellDsl.kt` の picker 3 本・datePicker 1 本) にも省略可能引数として通す (`copy` は自動で引き継ぐ) (→ Requirement: PickerCell 単一選択の閉じ切り通知 (Android) / PickerCell 複数選択の閉じ切り通知 (Android) / DatePickerCell の閉じ切り通知 (Android))
- [ ] 2.2 `PickerSelectionSheet` / `DateSelectionSheet` / `DateCalendarDialog` に確定値の控えを持たせ、確定経路で値 callback → 控え → `dismiss()` の順にする。キャンセル経路は控えを立てない (→ 同上) — design Decision 5
- [ ] 2.3 `PickerCellViewHolder` / `DatePickerCellViewHolder` の `showAnchoredTo(anchor, onDismissed)` の `onDismissed` で、既存の参照解放に加えて控えを読み、立っていれば Cell の completed callback を呼ぶ。`setOnDismissListener` の所有は `HostAnchoredDialog` に残す (→ 同上 / Requirement: 閉じ切り通知を足しても選択面の再提示と参照解放は従来どおり働く (Android))
- [ ] 2.4 テスト: `PickerSelectionSheetTest` / `DateSelectionSheetTest` / `DateCalendarDialogTest` に順序・同値・非確定 4 経路の不発火・トグル不発火・再提示後の再発火を追加。`PickerCellItemsTest` / `compose/PickerCellObjectBindingTest` に射影 factory・Compose DSL 経路で callback が通ることを追加 (→ 上記全 Requirement の各 Scenario)

## 3. Bridge (iOS)
- [ ] 3.1 `KsBridgeInteractionDelegate` に `pickerCellSelectionCompleted(cellID:index:)` / `pickerCellMultiSelectionCompleted(cellID:indices:)` を必須メンバとして追加し、`KsBridgeInteractionRelay` で既存と同じ正規化で転送、`KsBridgePickerCell.makeCell` で結線する。`KsBridgeDatePickerCell` は `onValueCompleted` を結線しない (→ Requirement: PickerCell の閉じ切り通知を interaction 経路で中継する / DatePickerCell の閉じ切り通知は interaction 経路に含めない) — design Decision 6
- [ ] 3.2 `maui/macios/KsSettingsView.Binding.iOS/ApiDefinition.cs` の `KsBridgeInteractionDelegate` に 2 メソッドを追加する (→ 同上)
- [ ] 3.3 テスト: `KsBridgeInteractionDelegateTests` に単一 / 複数の中継 (順序・正規化)・破棄後の不到達・DatePicker の追加通知なしを追加。fake delegate 3 本 (`KsBridgeInteractionDelegateTests` / `KsBridgeCustomCellTests` / `KsBridgeSectionVisibilityTests`) に新メンバを実装 (→ 同上の各 Scenario)

## 4. Bridge (Android)
- [ ] 4.1 `KsBridgeInteractionListener` に同名 2 メソッドを追加し、`KsBridgeInteractionRelay` で `Set<Int>` → 昇順 `IntArray` 正規化して転送、`KsBridgePickerCell` で結線する。`KsBridgeDatePickerCell` は結線しない (→ Requirement: PickerCell の閉じ切り通知を interaction 経路で中継する / DatePickerCell の閉じ切り通知は interaction 経路に含めない)
- [ ] 4.2 Android binding (`maui/android/KsSettingsView.Binding.Android`) を再生成し、`Metadata.xml` の追記なしで 2 メソッドが C# に露出することをビルドで確認する (露出しなければ `Metadata.xml` を調整し deviation に記録) (→ 同上)
- [ ] 4.3 テスト: `KsBridgeInteractionListenerTest` に中継・正規化・破棄後不到達・DatePicker 追加通知なしを追加。fake listener 3 本 (`KsBridgeInteractionListenerTest` / `KsBridgeCustomCellTest` / `KsBridgeSectionVisibilityTest`) に override を追加 (→ 同上の各 Scenario)

## 5. MAUI (maui/KsSettingsView.Maui)
- [ ] 5.1 `IKsInteractionSink` に `PickerCellSelectionCompleted(string, int)` / `PickerCellMultiSelectionCompleted(string, IReadOnlyList<int>)` を追加し、両 OS の `KsBridgeGateway` から転送する (→ Requirement: PickerCell の SelectedCommand は選択面が閉じ切った後に実行する)
- [ ] 5.2 `KsSettingsController`: completed メソッドで `PickerCell.NotifySelectionCompleted(mode)` を呼び、`PickerCellSelectionChanged / MultiSelectionChanged` からは呼び出しを外して書き戻しだけにする。cellId が PickerCell でなければ無視 (→ 同上) — design Decision 7
- [ ] 5.3 テスト: `PickerSelectedCommandTests` を「確定通知ではコマンド未実行 / 閉じ切り通知で 1 回実行 / 引数は届いた通知の種類で決まる / 未設定なら無反応 / 他 Cell の cellId は無視」に書き換え、既存契約のケース (OneWay・既定 null / 同値再確定でも実行 / 直接 setter では不発火 / `CanExecute` false でも `Execute`) は閉じ切り通知起点に置き換えて維持する。`InteractionLifetimeTests` に Handler 切断後の閉じ切り通知不実行を追加。`NativeValueWritebackTests` が変わらず通ることを確認 (→ 同上の各 Scenario)。fake sink を実装しているテストに新メンバを追加

## 6. dismiss 実時間の実機確認 (design Decision 8)
- [ ] 6.1 iPhone 11 実機 (iOS 18 系 / Debug) の `samples/ios` で、新しい `onSelectionChanged` → `onSelectionCompleted` の時刻差を一時コード (commit しない) で取り、都道府県 (47 件) を連続 5 回計測して `evidence/dismiss-timing/README.md` の「実機」節に追記する。判定: 中央値が Simulator の計測 (546ms) と 100ms 以内なら一致。超えていれば記録のみ (修正は別 change)

## 7. 完了判定
- [ ] 7.1 テスト実行: iOS (`cd ios && xcodebuild test -scheme KsSettingsView ...`)、Android (Robolectric)、MAUI (`KsSettingsView.Maui.Tests`) を handbook `cross/test-execution.md` の手順で実行し結果を報告する
- [ ] 7.2 Swift 6 言語モードの一時設定ビルドで error 0 件を確認し、`ios/Package.swift` の差分が無いことを確認する (handbook `ios/swift6-language-mode-check.md`)
- [ ] 7.3 統合ホストで C# → Native → 閉じ切り通知 → `SelectedCommand` の end-to-end を両 OS で確認する (handbook `maui/integration-host-verification.md`)。iOS 実機で ColorAnalyzer 相当の導線 (確定後にモーダルを提示) が固定待ちなしで動くことを証跡に残す
- [ ] 7.4 コメント規約 (handbook `cross/comment-policy.md`) の lint を通す
