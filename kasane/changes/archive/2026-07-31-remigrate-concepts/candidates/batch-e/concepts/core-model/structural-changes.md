---
type: reference
title: SettingsRootDiff による構造変更
description: 設定ツリーの全体・Section・Cell・Accessory 更新を表す公開 Diff 契約
tags: [core, diff, public-api]
timestamp: 2026-07-19
---

この文書は、`SettingsRootDiff` が設定ツリーの変更意図をどう表すかを説明する。読むと、Cell 内容更新と identity 変更の違い、移動可能な範囲、UI 層・Store が担う責務が分かる。

## 目的

`SettingsRootDiff` は `SettingsRoot` を自ら変更するオブジェクトではない。UI 層または Store に「設定ツリーのどこへ、どの種類の変更を適用するか」を渡す値である。

Swift は `Hashable` な enum、Kotlin は sealed interface と data class で同じ変更語彙を公開する。

## 責務境界

| 分類 | Swift / Kotlin | 意味 |
|---|---|---|
| 全体 | `full` / `Full` | `SettingsRoot` 全体を差し替える |
| Section | `insertSection` / `InsertSection` | index に Section を追加する |
| Section | `removeSection` / `RemoveSection` | ID で Section を削除する |
| Section | `moveSection` / `MoveSection` | Section の順序を変更する |
| Section | `replaceSection` / `ReplaceSection` | ID で Section 全体を置換する |
| Cell | `insertCell` / `InsertCell` | Section の index に Cell を追加する |
| Cell | `removeCell` / `RemoveCell` | ID で Cell を削除する |
| Cell | `replaceCell` / `ReplaceCell` | 同一 ID の Cell 内容を reconfigure する |
| Cell | `moveCell` / `MoveCell` | 同一 Section 内で Cell の順序を変更する |
| Accessory | `updateAccessory` / `UpdateAccessory` | Root / Section Header・Footer を追加・更新・削除する |

Section 操作は ID または root の Section index、Cell 操作は Section ID・Cell ID・Section 内 index を payload に持つ。Core は変更意図だけを表し、対象の探索、model の保持、visible projection、アニメーションは UI 層 / Store が担う。

`moveSection(from, to)` / `MoveSection(from, to)` の両 index は、hidden Section を含む `SettingsRoot.sections` を基準にする。`moveCell(cellID, to)` / `MoveCell(cellId, toIndex)` は Cell ID から現在の所属 Section を解決し、hidden Cell を含むその `Section.cells` 内で移動する。いずれも対象をいったん配列から除いた後の挿入 index として `to` を解釈する。

`full` / `Full` は `SettingsRoot.sections` を置き換えるが、UI 層が別に保持する Root Header / Footer と Theme は更新しない。

## identity と内容更新

`replaceCell` / `ReplaceCell` は、同一 ID の表示内容を更新し、同じ表示行を reconfigure する契約である。対象 ID と新しい Cell の ID が一致することは呼び出し側が保証する。

iOS は Cell 対象を UUID だけで等価になる `KsCellID` で表し、Android は `Cell.id` の `String` を直接使う。Bridge はこの型差を明示的に変換する必要がある。

Cell の ID を変える差し替えは `removeCell` + `insertCell`、Section 間の Cell 移動も削除 + 追加で表す。`moveCell` は同一 Section 内だけに使う。

呼び出し側は、対象 ID が現在の model に一意に存在し、`moveSection.from` が有効であることを保証する。追加先・移動先 index の範囲外値、対象不在、重複 ID に対する error / clamp / no-op は platform 実装ごとに異なり、`SettingsRootDiff` の公開契約では保証しない。

## Accessory 更新

`AccessoryTarget` は `rootHeader` / `RootHeader`、`rootFooter` / `RootFooter`、Section ID を持つ `sectionHeader` / `SectionHeader`、`sectionFooter` / `SectionFooter` の4位置を識別する。Root target には `SettingsAccessory.root` / `Root`、Section target には `SettingsAccessory.section` / `Section` を渡す。payload の `nil` / `null` は対象位置の削除を意味する。

target と payload 種別の一致、および Section target の ID が存在することは呼び出し側の事前条件である。`full` / `Full` では Root Accessory は変わらず、Root target の `updateAccessory` / `UpdateAccessory` を適用したときだけ UI 層の保持値が変わる。

## 保証すること

- ID が同じ Cell の内容更新を、行 identity の変更と区別して表せる。
- Cell ID の変更と Section 間移動を、削除と追加の組み合わせで表せる。
- `updateAccessory(..., nil)` / `UpdateAccessory(..., null)` は指定位置の Accessory 削除を表す。
- Theme 更新は `SettingsRootDiff` に含まれず、UI 層の独立経路で扱う。

## してはいけないこと

- ID が異なる新 Cell を `replaceCell` へ渡して identity 変更を表してはならない。
- Section 間移動を `moveCell` だけで表してはならない。
- Theme / style の変更を構造 Diff へ混ぜてはならない。
- `SettingsAccessory` を通常の Root / Section Accessory API の代替として使ってはならない。
- 対象不在や範囲外 index の platform 別 fallback を、Core が保証する挙動として依存してはならない。

## 利用例

```swift
let updated = CustomCell(id: oldCell.id, title: "通知（更新）")
let diff: SettingsRootDiff = .replaceCell(
    cellID: KsCellID(cell: oldCell),
    new: updated
)
```

```kotlin
val updated = oldCell.copy(title = "通知（更新）")
val diff: SettingsRootDiff = SettingsRootDiff.ReplaceCell(
    cellId = oldCell.id,
    newCell = updated,
)
```

## 関連

- [SettingsRoot・Section・Cell の設定ツリー](settings-tree.md)
