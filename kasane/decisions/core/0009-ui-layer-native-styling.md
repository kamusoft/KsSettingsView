---
id: 0009
title: スタイルを UI 層に隔離し Native 型で表現
status: accepted
date: 2026-06-08
---

## Context

Core は構造モデルに加えて Theme、CellStyle、色、フォント、画像のスタイル型も保持していた。しかし iOS と Android の Core はコードを共有しておらず、画像型はすでに `UIImage` / `Drawable` を直接保持していたため、「Core はプラットフォーム非依存」という契約は実態と一致していなかった。

独自の色・フォント表現は描画前に必ず Native 型へ変換され、利用者にも冗長な値の構築を要求していた。さらに Theme を構造モデルや構造 Diff に含めると、データ構造と描画スタイルの責務が混在する。

## Decision

Core は SettingsRoot、Section、Cell の同一性など構造的なドメインモデルに純化する。Theme、CellStyle、KsImage は UI 層に配置し、独自の `KsColor` / `KsFont` / `KsFontWeight` は廃止する。

色とフォントは、iOS では `UIColor` / `UIFont`、Android では Compose `Color` / `TextStyle` を直接使う。iOS の色は `UIColor`、Android の色は Compose `Color` の各一系統に統一する。

Theme は SettingsRoot から分離して View 側の引数または modifier で渡す。Theme 更新は構造 Diff から外し、UI 層の独立した更新経路で扱う。Cell の Core 抽象は style を要求せず、必要な具象 Cell が UI 層で CellStyle を保持する。

実効スタイルは UI 層で `CellStyle.X → Theme.cellX → プラットフォーム既定` の順に解決する。Cell 固有値を持つ例外では、その値を CellStyle より先に解決する。

## Alternatives Considered

- 独自の色・フォント型を UI 層へ移して残す案: 利用者が引き続き独自型を構築する必要があり、API 体験が改善しないため不採用。
- Theme / CellStyle を Core に残してフィールドだけ Native 型にする案: Core の責務がさらに曖昧になり、プラットフォーム依存を Core に固定するため不採用。
- Native 型を入口だけで受け、内部では独自型を保持する案: 読み出し時には独自型が露出し、中間変換も残るため不採用。
- Theme や Cell の色で複数の Native 色型を受け付ける案: API と fallback の組み合わせが膨張するため、各プラットフォームで一つの色型に統一する。
- Cell ごとに実効スタイル解決を記述する案: 解決ロジックとテストが分散するため不採用。
- Theme 自身に CellStyle との合成処理を持たせる案: Theme が CellStyle に依存して責務分離が崩れるため不採用。

## Consequences

- 正: Core の責務が構造モデルに限定され、スタイル変更と構造変更の境界が明確になる。
- 正: 利用者は慣れた Native の色・フォント API を直接使え、不要な変換層がなくなる。
- 正: CellStyle、Theme、プラットフォーム既定の優先順位が UI 層の一か所に集約される。
- 負: iOS と Android のスタイル API は Native 型の違いを持ち、完全に同じシグネチャにはならない。
- 負: Core、UI、SwiftUI / Compose、Sample、Bridge にまたがる破壊的な移行が必要になり、既存利用者は型と Theme の受け渡しを書き換える必要がある。
- 負: クロスプラットフォーム層は、Native 型との変換を Bridge 側の責務として持つ必要がある。

出典: openspec/changes/archive/2026-06-08-purify-core-extract-style-to-ui-layer/design.md
出典: openspec/changes/archive/2026-06-08-port-theme-and-cellstyle-missing-fields/design.md
