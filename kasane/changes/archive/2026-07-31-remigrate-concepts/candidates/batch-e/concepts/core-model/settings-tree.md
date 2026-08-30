---
type: reference
title: SettingsRoot・Section・Cell の設定ツリー
description: Core が公開する設定ツリー、Cell 抽象、Header・Footer 用 Accessory の責務と利用方法
tags: [core, model, public-api]
timestamp: 2026-07-19
---

この文書は、`KsSettingsViewCore`（iOS）と `ks-settingsview-core`（Android）が公開する設定画面モデルを説明する。読むと、`SettingsRoot → Section → Cell` の責務、Accessory の使い分け、両プラットフォームの型差が分かる。

最初に本書を読み、次に利用する Cell の種類に応じて [基本 Cell](../cells/basic-cells.md) または [入力 Cell](../cells/input-cells.md) を読む。部分更新を発行する場合は [SettingsRootDiff による構造変更](structural-changes.md)、icon を扱う場合は [KsImage](../cells/ks-image.md) を参照する。

## 目的

Core は設定画面の内容と順序を値として表し、UI 層へ渡す。描画、Theme、CellStyle、具象 Cell、状態の保持と更新適用は Core の責務ではない。`CellStyle` は具象 Cell が持つ行単位の視覚上書きであり、UI 層が Theme と合成する。詳細は [スタイルの所有と実効値解決](../styling/style-resolution.md) を参照する。

`KsAnyView` は SwiftUI / UIKit / Compose / Android View を受け取るため、Core はプラットフォーム型を完全には排除していない。境界は「描画と style の責務を持たない」ことである。

## 責務境界

| 公開型 | 責務 |
|---|---|
| `SettingsRoot` | 順序付きの `sections` だけを保持する。Theme と Root Header / Footer は持たない |
| `Section` | ID、Section Header / Footer、順序付き Cell、`headerHeight`、`isVisible` を保持する |
| `KsCell` / `Cell` | 全 Cell の最小共通契約。iOS は `UUID` ID と値比較、Android は `String` ID を要求する |
| `SectionAccessory` | Section Header / Footer の `text` または任意 View |
| `RootAccessory` | Root Header / Footer の `text` または任意 View。`SectionAccessory` とは別型 |
| `KsAnyView` | Accessory に任意の Native / declarative View を渡す型消去ラッパ |

具象 Cell と `KsImage` は UI 層に属する。標準 Cell は [基本 Cell](../cells/basic-cells.md) と [入力 Cell](../cells/input-cells.md) を参照する。

## 公開 API

iOS の `Section.id` は `UUID()` が既定で、`cells` は `[any KsCell]` である。Android の `Section.id` は呼び出し側が `String` を指定し、`cells` は `List<Cell>` である。

`Section.headerHeight` は `-1` が自動高さ、正値が固定高さを表す。`Section.isVisible` は既定 `true` で、`false` でも Section は `SettingsRoot.sections` の元の位置に保持される。

`RootAccessory` と `SectionAccessory` はどちらも text と view を扱うが、Root と Section を型で取り違えないため分かれている。Root Header / Footer の現在値は `SettingsRoot` ではなく、iOS の `KsSettingsViewController.rootHeader` / `rootFooter`、Android の `KsSettingsView.rootHeader` / `rootFooter` が保持する。`SettingsAccessory` はこの2型の代替ではなく、`SettingsRootDiff.updateAccessory` の payload 専用である。

`headerHeight` の公開契約で意味が定まる値は `-1` と正値だけである。`0` や `-1` 未満の挙動は保証しないため、呼び出し側は指定しない。

任意 View の Accessory は、iOS では `KsAnyView.swiftUI` / `.uiKit`、Android では `KsAnyView.Compose` / `.AndroidView` で構築する。

```swift
let header = SectionAccessory.view(
    .swiftUI { Text("詳細設定") }
)
```

```kotlin
val header = SectionAccessory.View(
    KsAnyView.Compose { Text("詳細設定") },
)
```

この値を `Section.header` / `footer` または `RootAccessory` の view case へ渡す。任意 View の内容は Accessory の値等価へ参加しないため、closure 内の変化だけを model 差分として検出できるとは限らない。

## model と visible projection

model は hidden な Section / Cell も含む `SettingsRoot` の完全な状態である。visible projection は、UI 層が model から `isVisible = true` の Section / Cell だけを元の順序と ID のまま取り出した描画用の派生データである。UI 層は両方を区別して保持し、非表示要素を model から削除しない。

## 独自 Cell の描画

独自 Cell を `Section.cells` に格納できることと、UI が描画できることは別契約である。利用者は Cell 値に加えて、iOS では `KsCellRenderer` に準拠する `UICollectionViewCell` を `KsCellRegistry.register` へ、Android では `CellViewHolder` factory と衝突しない `viewType` を `KsCellRegistry.register` へ登録する。

未登録時、iOS は assertion を報告して placeholder Cell を表示する。Android は `KsCellRegistry.strictMode = true` なら `IllegalStateException`、`false` なら高さ0の placeholder を使う。標準 Cell は各一括登録 API を利用する。

## 保証すること

- `SettingsRoot.sections` と `Section.cells` は順序を保ち、空配列も受け付ける。
- `headerHeight = -1` かつ header が未設定または空 text のとき、UI 層は空の Section Header 領域を作らない。
- `Section.isVisible = false` の Section は model に保持され、visible projection から Header / Footer / Cell ごと除外される。
- `SettingsRoot` と `Section` は値比較でき、`Section` の比較には ID、Accessory、Cell、`headerHeight`、`isVisible` が参加する。
- `RootAccessory.text` / `SectionAccessory.text` は文字列内容で比較する。view 同士は `KsAnyView` の中身を比較せず、同じ case なら等価とする。
- Android の `Cell` は通常の `interface` であり、利用側モジュールから独自 Cell を実装できる。
- 独自 Cell は対応 renderer / ViewHolder を Registry へ登録した場合に描画できる。

## してはいけないこと

- Theme、CellStyle、`KsImage`、具象 Cell を Core モデルの責務として扱ってはならない。
- Cell を Root / Section Header・Footer の内容として扱ってはならない。
- `KsAnyView` の内容変更を `SettingsRoot` / `Section` の値比較だけで検出できると仮定してはならない。
- Cell / Section の ID 重複を Core が検査すると仮定してはならない。一意性は呼び出し側が保証する。

## 利用例

```swift
import KsSettingsViewCore

struct CustomCell: KsCell {
    let id: UUID
    let title: String
}

let root = SettingsRoot(sections: [
    Section(
        header: .text("一般"),
        cells: [CustomCell(id: UUID(), title: "通知")]
    )
])
```

```kotlin
import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot

data class CustomCell(override val id: String, val title: String) : Cell

val root = SettingsRoot(
    sections = listOf(
        Section(
            id = "general",
            header = SectionAccessory.Text("一般"),
            cells = listOf(CustomCell("notifications", "通知")),
        ),
    ),
)
```

## 関連

- [SettingsRootDiff による構造変更](structural-changes.md)
- [基本 Cell](../cells/basic-cells.md)
- [入力 Cell](../cells/input-cells.md)
- [KsImage](../cells/ks-image.md)
