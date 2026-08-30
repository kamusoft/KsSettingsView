# Proposal: restore-pickercell-object-items

## Why

PickerCell の候補が全プラットフォームで文字列列 (`[String]` / `List<String>` / `IList<string>`) に後退しており、移植元 AiForms の `ItemsSource: IList` (任意の object) + `DisplayMember` / `SubDisplayMember` / `SelectedItem(s)` を使う利用者は、Picker のためだけに文字列モデルへの変換と選択結果の逆引きを自前で書く必要がある。オーナー判定は「機能後退。復元の検討が必要」。この不便は MAUI 移行者に限らず、Native (Swift / Kotlin) 利用者にも同じ変換コードを強いている。

方針は探索で確定済み ([core/ADR-0029](../../decisions/core/0029-pickercell-item-model-with-generic-edge-projection.md)): core の候補を `PickerItem` 値型 (主表示 `text` + 副表示 `subText`) の列にし、object 射影は API の縁 (iOS ジェネリック init / Android ジェネリック factory / MAUI facade) で受ける。選択の正は index のまま。

## What Changes

- **core モデル (iOS / Android)**: `PickerCell.items` を `PickerItem` 列へ変更。`List<T>` + 射影 closure (`displayText` / `subText`) を受けるジェネリック縁 API と、object 書き戻し (`selectedItem` TwoWay / `onItemSelected`) を追加。文字列ケースはジェネリック縁の String 特殊化 (射影省略時は恒等) として提供する — 互換保証ではなく設計としての簡易形。旧 `displayFormatter` は削除 (役割は射影が吸収)
- **選択面 (iOS / Android)**: 候補行に副表示 (2行目) の描画能力を追加。選択面契約 (picker-selection-surface) の改訂を伴う
- **bridge (iOS / Android)**: `KsBridgePickerCell` DTO と輸送に副表示列を追加
- **MAUI facade**: `ItemsSource` を object 列 (`IList`) に戻し、`DisplayMember` / `SubDisplayMember` (リフレクション sugar) と `SelectedItem: object` / `SelectedItems: IList` の TwoWay を復元。射影は facade で解決し (text, subText) を輸送
- **samples (iOS / Android / MAUI)**: object 候補 + 副表示のデモを追加
- 影響 capability: `cell-types-input` / `settings-view-ios-ui` / `settings-view-android-ui` / `maui-bridge` / `maui-cells` / `samples-ios` / `samples-android` / `samples-maui`

## Non-Goals

- **AiForms の `SelectedItemsOrderKey` / `UseNaturalSort` (複数選択サマリのソート指定)**: 選択サマリの並び規則という別の能力で、現行契約 (index 昇順連結) からの変更要否はオーナー判定が出ていない。必要なら別途起票
- **AiForms の `UsePickToClose` (上限到達で自動 close)**: 選択面の確定操作の意味論に関わる別の挙動追加。現行契約 (確定操作でのみ close) を変える判断は今回の復元と独立
- **NumberPicker / TimePicker / DatePicker 等、他 Cell への object 対応の波及**: 候補選択の構造を持つのは PickerCell のみ
- **利用者定義の選択面 UI カスタマイズ**: 副表示は既存選択面の能力追加に留める

## Impact

- **互換制約なし**: ライブラリは未配信 (初回リリースは package-distribution ロードマップのこれから、配布は ProjectReference のみ) のため、守るべき外部利用者はいない。API は互換を気にせず再設計し、リポジトリ内の呼び出し (samples / tests) はこの change で追随させる。旧形との差分は実装後に移行 Skill (api-mapping) へ docs-refresh で反映
- **API 変更の内訳**: MAUI は `ItemsSource` が `IList<string>` → object 列、`SelectedItem` が `string?` → `object?`。iOS / Android は文字列ケースの呼び出し形は String 特殊化で今と同じに保たれるが、`displayFormatter` は削除される
- **wire 形式変更**: bridge DTO に副表示列が増える (bridge は同一リポジトリ内結合のため互換性負債にはならない)
- リスク: 選択面の行高変更に伴う Android の折り畳み高さ計算・初期スクロール位置の追随。ジェネリック縁と既存 overload の解決衝突 (Swift の overload 解決・Kotlin の型推論)

## 級: L

公開 API 変更が 3 プラットフォーム + bridge + 選択面 UI (両 OS) + wire 形式に及び、UI あり・設計判断 (縁 API の形・副表示の描画) を伴うため。

domain: cross
