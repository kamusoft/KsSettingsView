---
id: 0011
title: Cell の輸送は per-type 展開とし、facade 派生・Snapshot・Bridge DTO を Cell 種ごとに 1:1 で増やす
status: accepted
date: 2026-08-10
---

## Context

maui-support / phase-4-basic-input-cells の議論。phase-2 (add-maui-core) で確立した facade の Cell パターンは「`CellBase` 派生 + `CreateSnapshot()` / `AffectsSnapshot()` → `KsSettingsController` の dirty-set → `KsBridgeGateway.ToDto()` で Bridge DTO 化」だが、実装は LabelCell のみで、`ToDto()` は戻り値が `KsBridgeLabelCell` に固定されており Cell 種の判別機構が存在しない。Bridge DTO も両OSとも LabelCell 用のみ。

phase-4 で残り11種 (基本6 + 入力5) を追加するにあたり、輸送形を「Cell 種ごとの個別 DTO」にするか「全プロパティを nullable で持つ単一 wide DTO + cellType 判別」にするかを決める必要があった。前提として、Native (iOS / Android) の Store・UI 層には 11 Cell 種 + Custom が個別型として既に実装済みである。

## Decision

- facade 派生クラス (AiForms 互換命名、[ADR-0008](0008-aiforms-compatible-api-surface-policy.md) の A 分類)・Snapshot record・Bridge DTO を **Cell 種ごとに 1:1 で追加する (per-type 展開)**。
- `KsBridgeGateway.ToDto()` は facade Cell 型による**型スイッチ**で対応する Bridge DTO を生成する。cellType の enum / 文字列判別は導入しない。
- 異種 DTO の混載 (Section への収容・置換 API の受け渡し) のため、両OSに**共通基底 DTO 型 `KsBridgeCell`** (iOS: `@objc` class、Android: abstract class) を置き、全 Cell DTO をその派生とする。`KsBridgeSection.cells` / `addCell` / `KsBridgeCellUpdate` / `KsBridgeRootBuilder` / C# gateway のシグネチャは基底型で受け、ID 採番・共通フィールド (cellID) は基底へ引き上げる。

## Alternatives Considered

- **単一 wide DTO + cellType 判別**: interop 面は1本で済むが、全プロパティが nullable になり実行時検証頼みになる。C# 側と Native 側の両方で cellType の分解スイッチが必要 (判別の二重化)。[ADR-0002](0002-bridge-api-per-store-operation.md) が union DTO を却下して Store 操作 1:1 を採った経緯とも不整合のため却下。

## Consequences

- 正: Native Store の個別 Cell 型と DTO が 1:1 で素直に対応し、変換層が最薄のまま保たれる ([ADR-0001](0001-maui-bridge-dsl-variant-internal-store.md) / ADR-0002 の路線と整合)。
- 正: コンパイル時の型安全が保たれ、`ConversionPathTests` へ Cell 種ごとの変換テストを足す既存テスト構造と噛み合う。
- 正: phase-5 (CustomCell) 以降も派生一式の追加で自然に伸びる。
- 負: DTO ボイラープレートが 11種 × 両OS で増える。ただし機械的な写しでありテストに守られる。interop DTO は非公開の輸送表現 ([ADR-0004](0004-maui-idiomatic-types-for-styling.md)) のため利用者には露出しない。
- 負: 基底型化により Bridge interop のシグネチャは source-breaking (`KsBridgeSection.cells` 等が `KsBridgeLabelCell` → `KsBridgeCell`)。Bridge / Binding assembly は未配布かつ facade 経由のみが公開契約 (Binding 型の直接使用は禁止事項) のため、利用者影響なしと評価して許容した (出典: 実装結果)。

---
出典: 2026-08-10 ksn-agenda (maui-support / phase-4-basic-input-cells) での議論 (案A 採用はオーナー判断) / add-maui-basic-input-cells design.md Decision 1 (共通基底 DTO)
