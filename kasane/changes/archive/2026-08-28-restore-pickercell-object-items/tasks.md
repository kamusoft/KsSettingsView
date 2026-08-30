# Tasks: restore-pickercell-object-items

## 1. core モデル (iOS)

- [x] 1.1 `PickerItem` 公開値型 (text + subText、Equatable/Hashable/Sendable) を追加 (→ Requirement: PickerCell の候補モデル)
- [x] 1.2 `PickerCell.items` を `[PickerItem]` へ変更し、equality / hash / snapshot 比較を追随 (→ Requirement: PickerCell の候補モデル)
- [x] 1.3 ジェネリック縁 init 群 (displayText / subText 射影、selectedIndex + onItemSelected、selectedItem TwoWay [T: Equatable]、selectedIndices + onItemsSelected) と String 特殊化を追加 (→ Requirements: PickerCell の候補モデル / 単一選択の object 書き戻し / 複数選択の object 受け取り)
- [x] 1.4 `displayFormatter` を削除し、`effectiveValueText()` を PickerItem.text ベースへ変更 (→ Requirements: PickerCell の value 自動表示 / REMOVED: displayFormatter)
- [x] 1.5 iOS core モデルのテスト (射影・空文字 subText 正規化・逆引き・同値重複・候補外要素・TwoWay 書き戻し・元コレクション非観測・範囲外 index・value 自動表示) と、design の公開シグネチャ一覧の全呼び出し形のコンパイル成立の固定 (→ 各 Scenario)

## 2. core モデル (Android)

- [x] 2.1 `PickerItem` 公開値型を追加 (→ Requirement: PickerCell の候補モデル)
- [x] 2.2 `PickerCell.items` を `List<PickerItem>` へ変更し、equals / hashCode を追随 (→ Requirement: PickerCell の候補モデル)
- [x] 2.3 ジェネリック factory 群 (callback 経路は ks-settingsview-ui、MutableState 経路は ks-settingsview-compose — design の配置一覧どおり) と String 特殊化を追加 (→ Requirements: 同上)
- [x] 2.4 `displayFormatter` を削除し、`autoValueText()` を PickerItem.text ベースへ変更 (→ Requirements: PickerCell の value 自動表示 / REMOVED: displayFormatter)
- [x] 2.5 Android core モデルのテスト (1.5 と同観点、コンパイル成立の固定含む) (→ 各 Scenario)

## 3. 選択面 (iOS)

- [x] 3.1 `PickerListViewController` の候補行を副表示対応にする (subText 非 nil 行のみ副表示、description 系統の実効値継承) (→ Requirement: 選択面候補行の副表示 (iOS))
- [x] 3.2 VoiceOver 公開に副表示を含める (→ Scenario: VoiceOver への公開)
- [x] 3.3 初期スクロール契約の確認と追随 (→ Requirement: 選択面候補行の副表示 (iOS))
- [x] 3.4 iOS 選択面のテスト (副表示の描画・混在・初期スクロール・a11y) (→ 各 Scenario)

## 4. 選択面 (Android)

- [x] 4.1 `PickerSelectionSheet` の行レイアウトを副表示対応にし、`PickerSheetStyle` に description 系統のサブ項目を追加 (→ Requirement: 選択面候補行の副表示 (Android))
- [x] 4.2 折り畳み高さ計算・初期スクロールを可変行高へ追随 (→ Scenario: 折り畳み高さの契約維持)
- [x] 4.3 TalkBack 公開に副表示を含める (→ Scenario: TalkBack への公開)
- [x] 4.4 Android 選択面のテスト (副表示の描画・混在・折り畳み高さ・初期スクロール・a11y) (→ 各 Scenario)

## 5. bridge (iOS / Android)

- [x] 5.1 候補輸送を per-item DTO (text + nullable subText、design Decision 7) へ変更し、snapshot (`KsPickerCellSnapshot`) と gateway 変換を追随 (→ Requirement: PickerCell の輸送)
- [x] 5.2 bridge 経路のテスト (副表示付き候補の輸送・index 通知の維持) (→ 各 Scenario)

## 6. MAUI facade

- [x] 6.1 `ItemsSource` を `IList` へ変更し、`DisplayMember` / `SubDisplayMember` のリフレクション射影 (public instance プロパティ限定・型別キャッシュ・PropertyInfo ベース・ToString() フォールバック・null 要素拒否・snapshot 確定 — design Decision 6/8) を実装 (→ Requirement: PickerCell の候補と表示射影)
- [x] 6.2 `SelectedItem` (object) の相互導出を拡張し、`SelectedItems` (IList、SelectedIndices との相互導出・index 昇順・最初一致 + 再導出規則・null/空正規形) を追加 (→ Requirement: PickerCell の選択項目の相互導出)
- [x] 6.3 `DisplayFormatter` を削除し、snapshot 生成を (text, subText) ペアへ変更 (→ Requirements: PickerCell の候補と表示射影 / REMOVED: DisplayFormatter)
- [x] 6.4 MAUI facade のテスト (射影・フォールバック・相互導出・TwoWay 書き戻し) (→ 各 Scenario)

## 7. samples

- [x] 7.1 iOS sample に object 候補 + 副表示のデモを追加 (→ Requirement: PickerCell の object 候補デモ (iOS))
- [x] 7.2 Android sample に同デモを追加 (→ Requirement: PickerCell の object 候補デモ (Android))
- [x] 7.3 MAUI sample に DisplayMember / SubDisplayMember + SelectedItem(s) バインドのデモを追加 (→ Requirement: PickerCell の object 候補デモ (MAUI))

## 8. 視覚照合

- [x] 8.1 iOS / Android の選択面スクリーンショットを mock (approved.png) と照合し、乖離を潰す (→ ui/brief.md)
