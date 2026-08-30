---
id: 0024
title: SectionMargin は Thickness で公開し Left / Right を leading / trailing として解釈する
status: accepted
date: 2026-08-20
---

## Context

maui-support / phase-11-modern-style の議論。Native の Theme 4属性のうち `sectionMargin` は leading / trailing の論理方向基準で契約されており (iOS `NSDirectionalEdgeInsets` / Android `PaddingValues(start, end)`、RTL の左右解決は各 platform の機構が担う)、MAUI facade でこれをどう公開するかを決める必要があった。MAUI の慣例型 `Thickness` は Left / Right の物理座標基準で、MAUI 自身も FlowDirection による Thickness の自動反転は行わない — ここに方向意味論の断絶がある。現行の MAUI Theme 相当 (フラットな BindableProperty 群) に矩形 inset 型の前例は無く、完全新規の型写像になる。

## Decision

- `SectionMargin` は **`Thickness?`** の BindableProperty として公開する (null = platform 既定へ委譲。maui/[ADR-0004](0004-maui-idiomatic-types-for-styling.md) の nullable 委譲パターン)。
- **`Thickness.Left` / `Right` は leading / trailing (論理方向) として解釈**し、Native の directional 型へ位置のまま写す。RTL 時の左右反転は Native の解決機構 (iOS: UIKit の `NSDirectionalEdgeInsets` 解決 / Android: `SectionBoxMetrics.resolve` の `LayoutDirection` 引数) に委ね、MAUI 層は `FlowDirection` を監視も変換もしない。
- MAUI 標準の Thickness 意味論 (物理座標) と異なるため、このプロパティに限り論理方向である旨を facade 契約とドキュメントに明記する。
- 残り3属性は `SectionCornerRadius: double?` / `SectionBorderWidth: double?` / `SectionBorderColor: Color?` の nullable 公開 (既存 snapshot / 輸送パターンの踏襲で、本 ADR の主題ではない)。

## Alternatives Considered

- **独自 `DirectionalThickness` 型 (Start / Top / End / Bottom) の新設**: 意味論が型に現れて誤解の余地が無いが、新型 + XAML TypeConverter の自作が必要で、「公開 API は MAUI 慣例型」(ADR-0004) から外れるため不採用。
- **`Thickness` を物理座標として扱い、MAUI 側で FlowDirection を見て start / end へ変換**: `Left` = 常に物理左で直感的だが、FlowDirection 変更の監視と Theme 再送機構を facade に持ち込むことになり、implement-modern-style からの申し送り「MAUI は値の伝搬のみで新たな視覚契約を作らない」に反するため不採用。

## Consequences

- 正: XAML 組込の TypeConverter で `SectionMargin="16,12"` 等がそのまま書け、RTL 対応は Native の既存契約に自動追従する。facade は値の伝搬のみで完結する。
- 正: 利用者が明示した値は各 platform の単位でそのまま適用される (list-appearance 契約の「既定値を platform 間で同じ生値に統一しない」とも整合)。
- 負: `Thickness.Left` の字面と実際の方向が RTL 環境でズレる。ドキュメント明記で吸収するが、物理座標を期待する利用者には非直感的。
- 負: 将来 MAUI 本体が flow-relative な Thickness 解釈を導入した場合、本プロパティの独自解釈との整合を再確認する必要がある。
- 負: 「facade は検証せず素通しし正規化は Native の描画時のみ」の契約を成立させるには、輸送層にも検証しない型が要る — Android は Compose 標準の `PaddingValues(...)` ファクトリが構築時に全成分 0 以上を要求するため、非検証実装 `KsBridgeSectionMargin` を Bridge に置いて生値を描画時正規化まで運ぶ。(出典: 実装結果 — review-001 Critical の解消形)

---
出典: 2026-08-20 ksn-agenda (maui-support / phase-11-modern-style) での議論 (案 A の採用はオーナー判断)
