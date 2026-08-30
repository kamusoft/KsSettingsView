# Delta Spec: maui-bridge (harden-update-accessory-unknown-id)

## MODIFIED Requirements

### Requirement: Store 操作 1:1 の更新 API

Bridge は Store 公開操作と 1:1 対応する更新 API (`insertSection` / `removeSection` / `moveSection` / `replaceSection` / `insertCell` / `removeCell` / `moveCell` / `replaceCell` / `updateAccessory` / `replaceCells`) を提供しなければならない (SHALL)。各操作は内部所有 Store の対応する公開操作へ素通しされ、Store の現行契約 — hidden 要素を含む model 配列上の index、同じ ID の内容更新は identity を維持、Cell / Section 操作および `updateAccessory` の section 系 target における未知 ID の no-op (core/ADR-0020) — がそのまま適用される。未知 sectionID での `updateAccessory` は状態・表示・通知のいずれにも影響しない (Root 系 target は `sectionID` 引数を参照しないため未知 ID 判定の対象外)。

phase-1 の `updateAccessory` および Builder の Section header / footer が輸送する accessory は text (および clear = null) に限定する。`updateAccessory` は target (root header / root footer / 指定 Section の header / footer) と text または clear を受け取り、clear 後は accessory が指定されていない場合と同じ表示になる。

#### Scenario: Cell の構造操作が表示へ反映される
- **GIVEN** `setRoot` 済みの Bridge
- **WHEN** `insertCell` で新しい LabelCell を挿入し、`removeCell` で既存 Cell を削除する
- **THEN** Native の設定 list に挿入と削除が反映される

#### Scenario: Section の構造操作が表示へ反映される
- **GIVEN** 複数 Section を持つ root で `setRoot` 済みの Bridge
- **WHEN** `insertSection` で Section を挿入し、`moveSection` で Section の順序を入れ替え、`removeSection` で Section を削除する
- **THEN** Native の設定 list に挿入・並べ替え・削除が反映される

#### Scenario: replaceCell は行の identity を維持する
- **GIVEN** `setRoot` 済みの Bridge
- **WHEN** 表示中の LabelCell と同じ cellID で内容の異なる LabelCell を `replaceCell` に渡す
- **THEN** 同じ行の表示内容が更新され、行の削除+挿入 (構造変更) としては扱われない

#### Scenario: replaceCells は1バッチで反映される
- **GIVEN** `setRoot` 済みで複数の LabelCell を表示中の Bridge
- **WHEN** `replaceCells` に複数の (cellID, 新 Cell) を渡す
- **THEN** 対象行の表示内容が1回のバッチ内容更新として反映される

#### Scenario: 未知 sectionID の updateAccessory は no-op
- **GIVEN** `setRoot` 済みで Host を表示中の Bridge
- **WHEN** Bridge が採番していない canonical UUID 文字列を sectionID として、section header target と section footer target のそれぞれで `updateAccessory` を呼ぶ
- **THEN** いずれもエラーやクラッシュは発生せず、状態と表示は変化しない (iOS / Android で同じ結果)

#### Scenario: 全12操作が契約どおりに反映される
- **GIVEN** `setRoot` 済みの Bridge
- **WHEN** 12操作それぞれを代表的な引数で呼ぶ
- **THEN** 各操作後に観察可能な結果 (Host の表示内容と通知) が、対応する Store 操作の契約 (index 解釈・identity 維持・no-op 条件を含む) と一致する (操作ごとに検証する)
