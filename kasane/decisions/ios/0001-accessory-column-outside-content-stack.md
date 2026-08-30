---
id: 0001
title: アクセサリは contentStack 外のアクセサリ列に置き垂直 Fill させる
status: accepted
date: 2026-08-01
---

## Context

iOS の Cell 共通行レイアウトはコミット `8fea183` で AiForms 準拠の自前 UIStackView 構造 (stackH / stackV / contentStack / descriptionLabel) へ移行済み。しかしその際の設計判断 (旧 openspec `migrate-cell-base-to-stack-layout` design.md Decision 2) で「すべての trailing control を `contentStack.addArrangedSubview` する」と定めたため、Switch / Checkbox 等のアクセサリがタイトル行に同居し、descriptionLabel (contentStack の兄弟) がセル全幅で折り返してアクセサリの下に回り込む。

AiForms オリジナルは Switch / Checkbox / chevron / checkmark を `UITableViewCell.AccessoryView` / `Accessory` (ContentView の外側) に置いており、ContentView の幅がアクセサリ分だけ縮むことで「アクセサリは垂直 Fill (垂直センター)、description はアクセサリの左までで折り返す」が成立している。`ContentStack` 経由なのは EntryCell の TextField (`_FieldWrapper`) のみ。

Android は `descriptionView.END = accessoryHolder.START` の ConstraintSet で同等の制約を既に実現しており、問題は iOS のみ。

`UICollectionViewListCell` には `UITableViewCell.AccessoryView` に相当する標準 API が `accessories` (`UICellAccessory`) 以外に無いため、自前 Stack 構造の中で等価な領域を作る必要がある。

## Decision

`KsListCellBase` の stackH 直下、stackV の後ろに「アクセサリ列」を設け、Cell 級のアクセサリ (Switch / Checkbox / Radio・SimpleCheck の checkmark / chevron) はそこへ配置する。stackH の `alignment = .center` によりアクセサリはセル全体 (タイトル + description) に対して垂直センターに置かれ、stackV (= descriptionLabel の幅) は自動的にアクセサリ列の左までに制限される。

`applyCellBaseLayout` は 2 系統の受け口を持つ:

- `accessoryView: UIView?` — Cell 級アクセサリ。stackH のアクセサリ列へ (AiForms の `AccessoryView` / `Accessory` 相当)
- `trailingViews: [UIView]` — 行内 trailing。従来どおり contentStack へ (EntryCell の TextField、valueLabel 等。AiForms の `ContentStack.AddArrangedSubview` 相当)

## Alternatives Considered

- `UICellAccessory` (Apple 標準経路) へ戻す案: customView の幅が intrinsic size / 固定 frame に縛られる問題が再発し得るうえ、`8fea183` で廃止した経路の復活となり自前 Stack 構造と title 描画位置が二重定義になるため不採用。
- descriptionLabel に幅制約 (アクセサリの左まで) だけ追加する案: 回り込みは解消するがアクセサリはタイトル行に残り、垂直 Fill (垂直センター) にならない対症療法のため不採用。

## Consequences

- 正: SwitchCell 等の見た目が AiForms オリジナルと等価になる (アクセサリ垂直センター、description はアクセサリ左で折り返し)。
- 正: 「Cell 級アクセサリ = accessoryView / 行内 trailing = trailingViews」の 2 系統に役割が整理され、AiForms の構造 (AccessoryView vs ContentStack) と 1:1 で対応が付く。
- 正: Android の `descriptionView.END = accessoryHolder.START` と意味的に揃い、プラットフォーム間で視覚文法が一致する。
- 負: 旧 openspec design.md Decision 2 (「全 trailing control を contentStack へ」) を部分的に覆すため、Cell renderer 約 10 件と contentStack ベースのテスト assert の追従修正が必要。

出典: kasane/changes/fix-cell-accessory-vertical-fill/exploration.md (2026-08-01 の探索議論)
