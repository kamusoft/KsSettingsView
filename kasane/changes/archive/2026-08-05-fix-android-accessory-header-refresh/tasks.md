# Tasks: fix-android-accessory-header-refresh

## 1. DiffCallback: Section H/F の内容検出 (ADR-0012 決定1)

- [x] 1.1 `CellListItemDiffCallback.areContentsTheSame` を SectionHeader / SectionFooter に限り内容比較にする (Text は data 等価、View は KsAnyView の参照比較。core の View.equals は変更しない。CellRow は常時 true を維持) (→ Requirement: Section accessory の内容更新の表示反映)
- [x] 1.2 `getChangePayload` を実装し、Section H/F の内容変化時に `PAYLOAD_CONTENT` を返す (payload 定数の置き場所は fix-root-accessory-payload-notify の集約論点を踏まえて判断) (→ Requirement: Section accessory の内容更新の表示反映)
- [x] 1.3 `KsSettingsListAdapter` / `CellListItemDiffCallback` / `submitContentUpdate` の「areContentsTheSame は常に true」前提のコメントを新契約へ更新する

## 2. setRootDirect: full 更新の内容 rebind 完結 (ADR-0012 決定2・補正後)

- [x] 2.1 full 更新の構造提出と内容通知を分離する: `submitList` は**内容通知対象が空でも必ず実行**し、内容通知は「旧 visible リストと新 visible リストの双方に存在する cell id」だけへ発行する (→ Requirement: full 更新経路での同一 id の Cell 内容反映)
- [x] 2.2 空 root への更新・Section H/F のみの root への更新・初回 setRoot が正しく反映されることを実装中に確認する (→ Scenario: 空 root への更新で表示が空になる / Section header / footer のみの root への更新が反映される / 初回の root 反映では内容変更通知を発行しない)

## 3. テスト (Scenario 対応)

- [x] 3.1 updateAccessory の header / footer text 変更が表示・payload 付き通知に反映されるテスト (→ Scenario: updateAccessory による header text 変更 / footer text 変更)
- [x] 3.2 replaceSection の header text 変更・accessory 型切替・View accessory 差し替えが反映されるテスト (→ Scenario: replaceSection による header text 変更 / accessory の型の切替 / View accessory の差し替え)
- [x] 3.3 内容同一の Section H/F へ変更通知が出ないテスト (→ Scenario: 内容が同一なら Section H/F へ変更通知を発行しない)
- [x] 3.4 accessory の null ↔ 非 null が挿入・削除として反映されるテスト (→ Requirement: Section accessory の追加と削除は構造変更として反映する)
- [x] 3.5 replaceSection / Full diff / root 再設定の同一 id Cell 内容変更が反映されるテスト (→ Scenario: replaceSection で同一 id の Cell 内容変更 / Full diff で同一 id の Cell 内容変更 / root の再設定でも同一 id の Cell 内容変更が反映される)
- [x] 3.6 空 root・Section H/F のみ root への更新が反映されるテスト (→ Scenario: 空 root への更新で表示が空になる / Section header / footer のみの root への更新が反映される)
- [x] 3.7 新規 Cell・削除 Cell へ内容通知が出ない・初回 root 反映で内容通知が出ないテスト (→ Scenario: 新規に挿入される Cell へは内容通知を重ねない / 削除された Cell へは内容通知を発行しない / 初回の root 反映では内容変更通知を発行しない)
- [x] 3.8 既存テストの契約更新: `ListAdapterDiffTest` の「SectionHeader Text の内容差分は areContents に反映されない」を新契約 (Section Text の内容差は false + PAYLOAD_CONTENT、CellRow は常時 true) へ書き換える (→ Requirement: Section accessory の内容更新の表示反映)
- [x] 3.9 既存回帰の実行: ListAdapterDiffTest / ContentUpdatePayloadTest / BasicCellsTest ほか android ユニットテスト一式
