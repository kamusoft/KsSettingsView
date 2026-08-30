---
id: 0021
title: MAUI CustomCell の公開面は CellBase 継承 + Content で構成し、継承で来る不適用プロパティは silent no-op で露出のまま維持する
status: accepted
date: 2026-08-12
---

## Context

core の CustomCell 契約 ([custom-cell.md](../../concepts/core/cells/custom-cell.md)) では、行は full-bleed で共通行レイアウトのスロット (title / description / icon) を持たず (core/ADR-0015 の適用除外)、テキスト系 CellStyle 項目は content に効かない。効く style は行レベル (背景色・cellHeight) のみ、挙動プロパティは `onTap` / `showArrow` (既定 false) / `isEnabled` / `isVisible`。

一方 facade の `CellBase` は Title / Description / IconSource / テキスト系 style などの共通スロットプロパティを持ち、Section への配置や snapshot 変換の共通機構は CellBase を前提にしている。原典 AiForms の `CustomCell` は `CommandCell` 継承 + `[ContentProperty("Content")]` で、`ShowArrowIndicator` / `IsSelectable` / `IsMeasureOnce` / `UseFullSize` / `LongCommand` を持つ。

## Decision

- `CustomCell : CellBase` とし、`[ContentProperty(nameof(Content))]` の **`Content : View`** を持つ (XAML 直書き対応、AiForms 同形)
- 挙動プロパティは **`Command` / `CommandParameter` / `Tapped`** (core の `onTap` 対応。命名は facade の CommandCell と統一) と **`ShowArrowIndicator`** (既定 false。AiForms 命名踏襲。CommandCell の `HideArrow` と向きが逆なのは、core 契約の既定値 — CommandCell は矢印あり・CustomCell はなし — が逆であることによる意図的非対称)
- `IsEnabled` / `IsVisible` / `BackgroundColor` / `Height` は CellBase 継承をそのまま使う (core 契約と一致)
- 継承で露出する不適用プロパティ (Title / Description / IconSource / テキスト系 style) は**隠蔽せず露出のまま silent no-op** とする — snapshot 変換が単に読まない。不適用一覧は XML doc コメントと利用者ドキュメントに明記する
- AiForms の `IsSelectable` / `IsMeasureOnce` / `UseFullSize` / `LongCommand` は**非提供** — 現行コア契約に対応概念がない ([ADR-0008](0008-aiforms-compatible-api-surface-policy.md))。`IsMeasureOnce` (計測キャッシュ) は「用途固有ポリシーを wrapper 本体に持ち込まない」([ADR-0016](0016-mauiview-materialization-self-measuring-wrapper.md)) とも衝突する

## Alternatives Considered

- **専用の細い基底を新設して不適用プロパティを継承させない**: Section への配置や snapshot 共通機構が CellBase 前提のため公開済み API の再編が必要で波及が大きい。却下
- **CommandCell 継承 (AiForms 同形)**: `ValueText` まで露出し、`HideArrow` の既定値 (矢印あり) が core の CustomCell 既定 (矢印なし) と衝突する。却下
- **`new` 隠蔽 (+ `[EditorBrowsable(Never)]` / `[Obsolete]`) で不適用プロパティを消す**: XAML の設定は基底の BindableProperty への `SetValue` にコンパイルされ、Binding は BindableProperty を直接指し、基底型アクセスは静的束縛のため、いずれも隠蔽を素通りする。「見かけ上消えているのに設定できて効かない」中途半端な可視性はかえって混乱を招く。却下
- **不適用プロパティの設定時に例外を送出**: CellBase を対象にした共有 Style を全 Cell へ当てる正当な使い方を壊す。却下

## Consequences

- 正: 既存公開面に波及なし。Section 配置・snapshot 変換・共有 Style の既存機構にそのまま乗る
- 正: `[ContentProperty]` 同形により AiForms からの移行が自然。CustomCell 派生サブクラスによる再利用形も XAML から使える
- 正: silent no-op は共有 Style と共存する (設定は許容し、描画にだけ効かない)
- 負: 効かないプロパティが IntelliSense に見え続ける。不適用一覧のドキュメント維持が公開面の責務になる
- この決定は、全スロットを使わない Cell を今後追加する場合の扱いの前例になる
- 補足 (出典: 実装結果): MAUI の `Style` 適用口は `NavigableElement` のみで `CellBase : Element` に共有 Style は適用できないと実装時に判明した (deviation 記録済み)。「例外送出は共有 Style を壊す」の却下理由は実機では発生しないが、同一のスタイル指定値を複数 Cell へ当てる形は成立し、silent no-op はそれと共存する — 却下判断自体は維持

---
出典: kasane/roadmaps/maui-support/phases/phase-5-custom-cell/history.md (2026-08-12: DataTemplate との対応関係と CustomCell の API 表現)
