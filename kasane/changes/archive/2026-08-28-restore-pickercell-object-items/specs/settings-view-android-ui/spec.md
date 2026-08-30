# settings-view-android-ui — デルタスペック (restore-pickercell-object-items)

## ADDED Requirements

### Requirement: 選択面候補行の副表示 (Android)

選択面 (ボトムシート) は `subText` を持つ候補を、主表示に加えて副表示を添えて表示する SHALL。副表示の文字スタイルは呼び出し元 Cell の description 系統の実効値 (`CellStyle` → `Theme`) を、選択面の他の内容と同じ提示経路の中で1回だけ解決して継承する SHALL。`subText` の無い候補行は従来どおり主表示のみで表示する SHALL。折り畳み高さの契約 (コンテンツ高・画面約半分を上限に内部スクロール) と初期スクロール (選択中の項目が見える状態で開く) は、副表示を持つリストでも維持する SHALL。各候補行のアクセシビリティ公開 (TalkBack) には副表示も含める SHALL。

#### Scenario: subText 付き候補の表示
- **GIVEN** `subText` を持つ候補を含む PickerCell
- **WHEN** 行タップで選択面を開く
- **THEN** その候補行は主表示と副表示の両方を表示し、副表示は description 系統の実効値で描画される

#### Scenario: 混在リスト
- **GIVEN** `subText` の有る候補と無い候補が混在する PickerCell
- **WHEN** 選択面を開く
- **THEN** `subText` の有る行だけが副表示を持ち、無い行は従来の表示のまま

#### Scenario: 折り畳み高さの契約維持
- **GIVEN** 画面半分を超える件数の `subText` 付き候補
- **WHEN** 選択面を開く
- **THEN** シートは画面約半分を上限とした高さで開き、候補リストは内部スクロールする

#### Scenario: 副表示混在時の初期スクロール
- **GIVEN** 副表示の有る候補と無い候補が混在し、選択中の index が初期表示範囲より後方にある PickerCell
- **WHEN** 選択面を開く
- **THEN** 選択中の項目が見える状態で開く

#### Scenario: TalkBack への公開
- **GIVEN** `subText` を持つ候補行
- **WHEN** アクセシビリティ機構が行を読み上げる
- **THEN** 主表示・副表示・選択状態が公開されている
