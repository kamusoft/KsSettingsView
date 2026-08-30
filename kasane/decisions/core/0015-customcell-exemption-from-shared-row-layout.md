---
id: 0015
title: CustomCell は共通行レイアウト統一の適用除外とする
status: superseded
date: 2026-08-03
---

## Context

ADR-0011 は継承ではなくコンポジションで**全 Cell** を共通行レイアウト関数と accessory slot により構成すると決定し、Android については「RecyclerView 内で `ComposeView.setContent` は使わない」と明記した。

一方、`add-cell-types-custom` の CustomCell（ADR-0014）は full-bleed の任意宣言 UI をセル化するもので、共通行構造（title / description / valueText / icon / hintText / accessory slot）を持たず、描画は `UIHostingConfiguration`（iOS）/ `ComposeView.setContent`（Android）に依存する。実装側には CustomCell を名指しで見越した `ComposeCellViewHolder`（`DisposeOnDetachedFromWindow` 強制の基底クラス）が既に存在するが、決定層では ADR-0011 との矛盾が未解決だった（second-opinion-001 指摘 #2）。

## Decision

- CustomCell（および将来の full-bleed 宣言 UI ホスティング型 Cell）は、ADR-0011 の「共通行レイアウトによる構成」「RecyclerView 内 `ComposeView.setContent` 不使用」の**適用除外**とする。
- ADR-0011 の適用範囲は「共通行構造を持つ Cell（標準の基本・入力 Cell 群および同型の利用者定義 Cell）」と整理する。標準 Cell 群に対する ADR-0011 の決定は引き続き有効。
- 適用除外の Cell は宣言 UI ホスティングの lifecycle 管理を必須とする（Android: `ComposeCellViewHolder` 経由の `DisposeOnDetachedFromWindow`、iOS: `UIHostingConfiguration` の構成差し替え）。
- Disclosure Indicator 等、既存 Cell と見た目を揃える装飾は hosted 宣言 UI 内で同一アセット・同一寸法定数を共有して合成する。

## Alternatives Considered

- **CustomCell も共通行（中央スロット差し替え）で構成する案** — 探索（論点2a）で不採用が確定済み。`UseFullSize` 等の条件付きプロパティ群が復活し API が肥大するため採用しない。
- **ADR-0011 を supersede して全面改訂する案** — 標準 Cell 群に対する ADR-0011 の決定は現在も有効であり、必要なのは適用範囲の整理だけ。全面改訂は過剰なため採用しない。

## Consequences

- 標準 Cell は従来どおり ADR-0011 に従い、full-bleed 系だけが宣言 UI ホスティング経路を使う二本立てが明文化される。
- RecyclerView 内 ComposeView 使用の性能特性（初期化・再構成コスト。ADR-0011 が Compose slot API 案を却下した理由）は、CustomCell のスクロール耐性デモ（40 行ダミー）を受け入れ検証として確認する。

出典: `kasane/decisions/core/0011-composed-shared-cell-row-layout.md`

出典: `kasane/decisions/core/0014-customcell-content-value-with-builder.md`

出典: `kasane/changes/add-cell-types-custom/second-opinion-001.md` 指摘 #2

出典: 2026-08-03 ユーザー判断「例外 ADR を新規起票」
