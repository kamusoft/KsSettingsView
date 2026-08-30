---
id: 0029
title: PickerCell の候補は PickerItem 値型列で持ち、object 射影は API の縁のジェネリックで受ける
status: accepted
date: 2026-08-28
---

## Context

`PickerCell` の候補は現在、全プラットフォームで文字列列である (iOS `items: [String]`、Android `items: List<String>`、MAUI `ItemsSource: IList<string>`)。移植元 AiForms.Maui.SettingsView は `ItemsSource: IList` (任意の object) + `DisplayMember` / `SubDisplayMember` (プロパティ名による表示射影) + `SelectedItem: object` / `SelectedItems: IList` を持ち、オーナーレビューで「機能後退。復元の検討が必要」と判定された (kasane/changes/restore-pickercell-object-items/exploration.md)。

この不便は MAUI 移行者に限らない。Native (Swift / Kotlin) の利用者も、Picker のためだけにモデル object から文字列配列を手組みし、選択結果を index から自前で逆引きする必要がある。復元は MAUI facade だけでなく core の公開 API から見直す (探索の会話でのオーナー指示 2026-08-28)。

設計を制約する既存構造:

- iOS の `KsCellRegistry` は `ObjectIdentifier(type(of: cell))` を renderer 解決キーにする (ios/Sources/KsSettingsViewUI/CustomCell.swift の注記)。ジェネリック型 `PickerCell<T>` は T ごとに別の runtime 型になり、任意の T の事前登録が成立しない
- Cell の equality / hash に `items` が含まれ、snapshot 比較・内容更新の差分検出に使われる。Swift の `Any` は Equatable / Sendable に乗らない
- MAUI bridge は「表示整形は上位層が適用済みの文字列を wire に載せる」設計 (ios/Sources/KsSettingsViewBridge/KsBridgePickerCell.swift) で、object のシリアライズ経路は持たない
- AiForms の `SubDisplayMember` (副表示) は native ではなく MAUI 側の選択ページ (`SimpleCheckCell.Description`) が描いていた。KsSettingsView は選択面が core (Android ボトムシート / iOS ページシート) にあるため、副表示は core 選択面への能力追加になる

## Decision

候補のモデル表現と object の受け口を2層に分ける:

1. **core モデルは非ジェネネリックのまま、候補を `PickerItem` 値型 (主表示 `text` + 副表示 `subText`(任意)) の列で持つ**。選択の正は従来どおり index (`selectedIndex` / `selectedIndices`) であり、選択面・bridge・equality は (text, subText) の単型パイプラインで動く。
2. **object の世界は API の縁で受ける**。iOS はジェネリック convenience init、Android はジェネリック factory 関数 (data class の primary constructor は変えない) が `List<T>` + 射影 closure (`displayText: (T) -> String`、`subText: ((T) -> String?)?`) を受け取り、構築時に1回だけ `PickerItem` 列へ射影する。書き戻しは、縁が捕捉した元の `List<T>` を選択 index で逆引きして object callback / Binding / MutableState へ渡す closure を組み立てる。ジェネリック `T` は縁の中で閉じ、モデル型・Registry・描画には現れない。

付随する決定:

- 文字列ケースはジェネリック縁の String 特殊化 (射影省略時は恒等) として提供する。これは互換保証ではなく設計としての簡易形 — ライブラリは未配信で外部利用者がおらず、互換のための旧 API 凍結はしない (提案レビューでのオーナー判断 2026-08-28)。旧 `displayFormatter` は役割を射影に吸収して削除する
- `selectedItem` (object の TwoWay) overload は初期表示の object → index 逆引きに `T: Equatable` (Kotlin は `equals`) を前提とする。逆引き不要な `selectedIndex` + `onItemSelected(T)` 形の overload を併設し、制約を課さない経路も残す
- MAUI facade は `ItemsSource` を object 列に戻し、射影を facade で解決して (text, subText) ペアを wire で運ぶ。戻りは従来どおり index で、facade が保持する元コレクションから `SelectedItem(s)` を復元する。AiForms 互換の `DisplayMember` / `SubDisplayMember` (プロパティ名指定) は C# 側のリフレクション sugar として facade に置ける
- 副表示は `PickerItem.subText` としてモデルに乗り、選択面の2行目表示 (両 OS) と bridge DTO の副表示列追加は本決定の帰結として実装する

## Alternatives Considered

- **`PickerCell<T>` (Cell 型ごとジェネリック)**: 却下。モデルの正を object にする思想としては最も純粋だが、iOS の Registry が metatype 解決である以上、任意の T の事前登録が成立せず型消去の別機構が必要になる。equality にも `T: Equatable` 要求が波及する。利用者から見える書き味は縁ジェネリック案と同等であり、波及コストに見合わない。
- **`items: [Any]` + 射影 closure**: 却下。Swift の `Any` は Equatable / Sendable に乗らず、snapshot 比較と `@Sendable` closure の既存契約に衝突する。型安全も失われる。
- **MAUI facade 層のみの射影 (core は文字列列のまま)**: 却下。MAUI 移行者の障壁は解消するが、Native (Swift / Kotlin) 利用者に文字列配列の手組みと index 逆引きが残り、「Picker のためだけの変換コード」という原問題が core 利用者に残存する (オーナー指摘)。
- **現状維持 (`IList<string>` / `[String]`)**: 却下。オーナーレビューで機能後退と判定済み。

## Consequences

- 正: Native 利用者は `List<T>` + 射影 closure だけで Picker を構成でき、SwiftUI / Compose の流儀 (closure 射影) に揃う。MAUI 利用者は AiForms 相当の object API へ移行できる
- 正: 副表示 (`SubDisplayMember` 相当) が全プラットフォーム共通の `PickerItem.subText` として一度で収束し、wire 形式と選択面契約を二度壊さない
- 正: Registry・equality・「選択の正は index」「モデル値を正規化しない」の既存契約は無変更で成立する
- 負: 選択面の行 UI に2行目の描画能力追加が必要 (iOS は既定 `UITableViewCell` 登録の差し替え、Android は行レイアウト組み替え + `PickerSheetStyle` へのサブ系統追加 + 折り畳み高さ計算への追随)。選択面契約 (kasane/concepts/core/cells/picker-selection-surface.md) の改訂を伴う
- 負: bridge DTO (`KsBridgePickerCell`) と MAUI snapshot に副表示列の追加が必要
- 負: 縁で捕捉した元配列と表示中の選択面の間に更新タイミング差があるため、items 差し替え時の逆引き整合は宣言的再構築 (init が毎回走る) に依存する
- 負: `displayFormatter` の削除と MAUI の型変更により、リポジトリ内の既存呼び出し (samples / tests) と移行 Skill の記述に追随作業が発生する

出典: kasane/changes/restore-pickercell-object-items/exploration.md / 探索の会話中の議論 (2026-08-28) / 移植元 ../AiForms.Maui.SettingsView/SettingsView/Cells/PickerCell.cs・Pages/PickerPage.xaml
