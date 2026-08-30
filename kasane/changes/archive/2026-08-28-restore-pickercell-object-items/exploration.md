# Exploration: restore-pickercell-object-items

起票日: 2026-08-26 / 起票元: rollout-user-skills のオーナー検収 (移行対応表レビュー中に発覚) / 探索: 2026-08-28

## 課題 / 動機

PickerCell の候補が AiForms の `ItemsSource: IList` (任意の object + `DisplayMember` / `SubDisplayMember` / `SelectedItem(s)`) から文字列列へ後退している。オーナー判定: **機能後退。復元の検討が必要**。

探索で範囲を拡張: この不便は MAUI 移行者に限らず、**Native (Swift / Kotlin) 利用者も Picker のためだけに文字列配列を手組みし index を逆引きしている**。復元は MAUI facade だけでなく core の公開 API から見直す (オーナー指示)。

## 調査結果 (要点)

- core は iOS `items: [String]` / Android `items: List<String>`、選択の正は index。equality/hash に items が入る
- iOS の `KsCellRegistry` は `ObjectIdentifier(type(of: cell))` で renderer 解決 → ジェネリック Cell 型は事前登録不能 (ios/Sources/KsSettingsViewUI/CustomCell.swift 注記)
- MAUI 経路は「射影済み文字列を wire に載せる」設計。選択結果は index だけが facade に戻る
- 選択面の行 UI に2行目 (副表示) の器は両 OS とも無い。AiForms の副表示は native ではなく MAUI 側選択ページ (`SimpleCheckCell.Description`) が描いていた
- AiForms の `DisplayMember` 解決はリフレクション (プロパティ getter の Expression キャッシュ、単一プロパティ名のみ)。書き戻しはページ離脱時に `SelectedItems` 一括反映

## 検討した選択肢 (却下案と理由を含む)

採用: **案3「`PickerItem` 値型 + API の縁のジェネリック」** — 詳細は [core/ADR-0029](../../decisions/core/0029-pickercell-item-model-with-generic-edge-projection.md)

却下 (理由は ADR-0029 の Alternatives Considered を正とする):

- `PickerCell<T>` (Cell 型ごとジェネリック) — iOS Registry / equality への波及大、書き味は同等
- `items: [Any]` + 射影 closure — `Any` が Equatable / Sendable に乗らず既存契約と衝突
- MAUI facade 層のみの射影 — Native 利用者の不便が残存 (オーナー指摘により却下)
- 副表示の見送り (object 候補のみ復元) — 副表示を後送りすると wire 形式と選択面契約を二度壊す。副表示は `PickerItem.subText` として同時復元する

## 決定事項

- core の候補モデルを `PickerItem` (text + subText) 列に変更し、`List<T>` + 射影 closure をジェネリック init (iOS) / factory 関数 (Android) の縁で受ける。object 書き戻しは縁が捕捉した元配列の index 逆引きで行う (ADR-0029)
- 選択の正は index のまま。文字列ケースは String 特殊化 (射影省略時は恒等) の簡易形として提供 — 未配信で外部利用者がいないため互換目的の旧 API 凍結はしない。旧 `displayFormatter` は削除 (提案レビューでのオーナー判断 2026-08-28)
- 副表示 (SubDisplayMember 相当) は同一 change で復元する: 選択面の2行目 (両 OS) + bridge DTO の副表示列
- MAUI facade は object `ItemsSource` + 射影を復元し、`DisplayMember` / `SubDisplayMember` はリフレクション sugar として facade に置く

## ADR 候補

- 作成済み: [core/ADR-0029](../../decisions/core/0029-pickercell-item-model-with-generic-edge-projection.md) (status: accepted、2026-08-28 オーナー承認)

## 未決の論点 (提案フェーズで詰める)

- `selectedItem` (object TwoWay) overload の形: `T: Equatable` 前提の逆引き経路と、制約なしの `selectedIndex` + `onItemSelected(T)` 経路の揃え方
- 複数選択の object 受け渡し形 (`selectedItems` の型: 順序付き List か Set か、AiForms の `SelectedItemsOrderKey` / `UseNaturalSort` 相当を持つか)
- MAUI `DisplayMember` sugar の仕様範囲 (AiForms 同様に単一プロパティ名のみか、`Func<object, string>` 射影も併設するか)
- 選択面2行目の詳細 (サブ文字のスタイル系統・行高・Android 折り畳み高さ計算への追随・アクセシビリティ公開)
- 行の value 表示 (`valueText` 自動生成) が副表示を含むか (AiForms は主表示のみ連結)

## UI 素材

なし (選択面2行目のモックは ksn-propose の ui/ 工程で作成)

## 変更級の推奨: L

- 触る能力: core モデル (iOS/Android)・選択面 UI (両 OS)・bridge DTO (両 OS)・MAUI facade・概念文書 (input-cells / picker-selection-surface / maui-facade)・移行 Skill (実装後に docs-refresh)
- 公開 API 変更あり (3プラットフォーム)、UI あり (選択面2行目)、wire 形式変更あり
