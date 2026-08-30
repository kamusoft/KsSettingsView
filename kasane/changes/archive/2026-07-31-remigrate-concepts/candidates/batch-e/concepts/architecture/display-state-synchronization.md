---
type: concept
title: 表示状態同期
description: 構造・同一 ID の内容・可視性・Theme を異なる更新経路へ分ける共通原則
tags: [architecture, state, diff, visibility]
timestamp: 2026-07-19
---

この文書は、設定ツリーの変更を Native 表示へ同期する4つの経路を説明する。読むと、値等価と構造 identity を分ける理由、同じ ID の内容更新、visible projection、Theme の扱いが分かる。

ここで Cell model は、Core の Cell 抽象へ準拠し、UI 層が定義する具象の値を指す。`CellStyle` は Core 抽象の要件ではないが、それを持つ具象 Cell model では内容比較の対象になる。platform の再利用行は Native cell / ViewHolder と呼ぶ。3層の関係は [Native Host の責務境界](native-host-boundary.md#cell-の3層) を参照する。

## 同期経路

モデルの値等価と、表示構造を追跡する identity は別の契約である。値等価は通常の値比較とテストに使い、構造判定には Section / Cell ID だけを使う。

| 変化 | 共通の扱い | iOS | Android |
|---|---|---|---|
| 構造 | 追加・削除・移動・ID 変更 | 表示 ID 列を持つ snapshot を更新 | `submitList` へ新しい平坦 list を渡す |
| 内容 | 同じ ID の Cell model 値変更 | item を保ったまま reconfigure | `submitList` の反映完了後に ViewHolder へ最新 model を再適用 |
| 可視性 | hidden を含む model から projection を再構築 | full snapshot | full list |
| Theme | 構造から独立した表示状態 | canvas と表示中 Cell を再評価。表示済み Header / Footer の即時再評価は保証しない | 行・背景・Section 装飾を再評価 |

`Cell` の `Equatable` / `equals` は内容を含む通常の値等価として維持する。構造同期だけが内容を無視して ID を比較する。

## model と visible projection

model は hidden Section / Cell を含む完全な `SettingsRoot`、visible projection は表示対象だけを元の順序と ID のまま取り出した派生状態である。hidden 対象への更新は model に保持され、再表示時に更新済みの内容が現れる。

可視性の変化を通常の内容更新へ押し込むと、表示行の追加・削除を見落とす。そのため DSL の事前差分確認と Host の防御処理は、可視性差を検出したら full 更新へ切り替える。

## 内容更新

同じ ID の `replaceCell` は同一行の内容再構成を意味する。ID が変わる差し替えは remove + insert などの構造変更で表す。

Android の `replaceCells` は RadioCell など連動する複数 Cell を一回の状態更新へまとめ、一回の `submitList` 完了後に対象行を再 bind する。iOS は Diffable Data Source の item identity を維持して各対象を reconfigure する。

## 保証すること

- 内容を含む値等価を構造 identity の判定に使わない。
- 同じ ID の Cell 内容更新で行 identity を維持する。
- 可視性の変化では完全な model から visible projection を再構築する。
- Theme 更新で Section / Cell の ID と構造を変更しない。
- hidden 対象への操作を model に保持する。

## してはいけないこと

- Cell の title、選択値、CellStyle を snapshot / stable item ID に含めない。
- ID が変わる差し替えを同一行の内容更新として扱わない。
- 可視性変更と通常の内容更新を同じ経路へ流さない。
- Theme を構造・内容・可視性のいずれかへ擬装しない。

## 用語

| 用語 | 意味 |
|---|---|
| model | hidden 要素を含む設定ツリーの完全な状態 |
| visible projection | model から表示対象だけを取り出した派生状態 |
| snapshot | iOS Diffable Data Source が現在の Section / Cell ID 列を保持する表示構造 |
| reconfigure / rebind | 同じ ID の Native 行を破棄せず、最新 Cell model と Theme を再適用すること |
| full 更新 | 現在 model 全体から visible projection と Native 表示構造を作り直す更新 |

## 関連

- [Store の状態と更新通知](store-and-update-streams.md)
- [宣言ツリーの安定 identity](declarative-tree-identity.md)
- [Native Host の責務境界](native-host-boundary.md)
- [SettingsRootDiff による構造変更](../core-model/structural-changes.md)
