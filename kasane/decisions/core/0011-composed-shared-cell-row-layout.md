---
id: 0011
title: Cell 共通行レイアウトをコンポジションで統一
status: accepted
date: 2026-06-13
---

## Context

Cell ごとに共通フィールドの対応状況が異なり、title、description、valueText、icon、hintText、isEnabled などの配置とスタイル反映が個別実装に分散していた。各 Cell が同じ行構造を重複実装すると、フィールド追加や描画規約の変更のたびに複数箇所の同期が必要になる。

iOS は `UICollectionViewListCell` と `UIListContentConfiguration`、Android は RecyclerView の ViewHolder と View ベースの既存実装を持つ。Cell 種別ごとの差は trailing accessory にあり、共通部分は行レイアウトとして合成できる。

## Decision

継承階層ではなく、UI 層内部の共通行レイアウト関数と accessory slot のコンポジションで全 Cell を構成する。

iOS は UIKit の `applyCellBaseLayout` に共通フィールドと実効スタイル、accessories を渡す。SwiftUI 版の行レイアウトは併設しない。Android は programmatic な `ConstraintLayout` の View 群を ViewHolder ごとに保持し、View ベースの `applyCellBaseLayout` で共通フィールドを反映する。RecyclerView 内で `ComposeView.setContent` は使わない。

各 Cell View / ViewHolder は共通関数を呼び、Switch、Checkbox、checkmark、chevron など Cell 固有の trailing control だけを accessory slot に組み込む。共通関数は各 UI モジュールの `internal` 実装とする。

valueText は subtitle 構成時に本体寄りの trailing accessory として扱う。hintText は accessory に含めず、両プラットフォームとも本体行とは別に Cell 右上へ float 配置する。

## Alternatives Considered

- 共通基底クラスとテンプレートメソッドを使う案: iOS の継承階層が深くなり、既存のサイズ計算経路や accessory の再利用管理が複雑になるため不採用。
- マクロで Cell ごとのレイアウトを生成する案: Swift マクロのデバッグコストが高く、Android に同等の仕組みがないため不採用。
- UIKit と SwiftUI の行レイアウトを併設する案: 二重実装となり、Hosting とサイズ計算の検証まで必要になるため不採用。
- Android を Compose の slot API で実装する案: RecyclerView での初期化・再構成コスト、将来の MAUI 接続時の再実装、既存 View 実装との乖離を避けるため不採用。
- Android を XML レイアウトにする案: Theme / CellStyle による動的な値の反映は programmatic 構築の方が直接的なため不採用。
- 共通行レイアウト関数を public API にする案: 現時点では UI 層の実装詳細であり、カスタム Cell 向け公開は別途検討するため不採用。

## Consequences

- 正: 共通フィールドの配置、スタイル解決、enabled 状態の反映を一か所で保守でき、全 Cell の表示を揃えられる。
- 正: Cell 固有実装は accessory に集中し、新しい共通フィールドを Cell ごとに重複実装する必要が減る。
- 正: Android は既存の RecyclerView / View ベースを維持し、ViewHolder 構築後の bind を軽量に保てる。
- 負: iOS と Android で共通関数の実装自体は別々に保守する必要がある。
- 負: hintText の位置は従来の accessory 横並びから右上 float に変わり、既存画面には視覚的な差分が生じる。
- 負: Android の programmatic ConstraintLayout は ViewHolder 初期化時に制約を構築するコストを持ち、共通レイアウト関数は internal のため外部カスタム Cell から直接再利用できない。

出典: openspec/changes/archive/2026-06-13-unify-cell-common-fields-via-shared-row-layout/design.md
