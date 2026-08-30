---
id: 0006
title: 構造 Diff と UI Store で更新責務を分離する
status: accepted
date: 2026-05-15
---

## Context

Root 全体を値として作り直して代入する方式は、利用者に変更を全体スナップショットとして表現させ、大量データや高頻度更新で不要な再構築を生む。また、任意 View を含む accessory の変更を新旧 Root の比較から推測する処理は複雑で、更新の意図が曖昧になる。Core は UI 非依存のまま変更を表現し、Native UI はその変更をプラットフォームの差分更新へ適用する境界が必要だった。

## Decision

Core は変更種類を閉じた `SettingsRootDiff` として定義し、Swift の enum / Kotlin の sealed interface による網羅性を持たせる。Diff、accessory の更新対象、Root / Section accessory の統一ラッパは値比較可能とし、Section・Cell の追加、削除、移動、置換などを型付き payload で表現する。Root と Section の accessory 型自体は区別を維持する。

状態の保持と Diff の適用は UI 層の `SettingsRootStore` が担う。Store は SwiftUI / Compose の標準的な observable state を提供し、外部から Root を直接代入させず、明示的な操作を単一 Diff として発行する。Native UI は 1 操作ごとに 1 回 apply し、公開された Root 全代入や新旧 Root からの推測 refresh を更新経路にしない。

対象 ID が存在しない Diff は、開発時には即座に誤りを検出し、本番ではスキップしてログへ記録する。

## Alternatives Considered

- Diff を protocol / interface と外部実装で開放する案は、Native UI と MAUI が全ケースを網羅できなくなるため採用しない。
- Diff 種別と辞書 payload を持つ単一 struct の案は、型安全性が下がり API が冗長になるため採用しない。
- Root、Section、Cell ごとに Diff 階層を分ける案は、Store がフラットな操作列として扱えず複雑になるため採用しない。
- Cell 移動で元 index も指定する案は、Cell ID から一意に解決できる情報を二重指定し、矛盾を生むため採用しない。Section 間移動は削除と追加で表現する。
- Store を Core に置く案は、Combine や coroutines への依存が Core に侵入するため採用しない。別 Store モジュールにする案も、モジュールと import を増やすため採用しない。
- 値型 Root の直接公開や `@Binding` 互換を維持する案は、Store 経由だけで更新する方針に反するため採用しない。
- 複数操作をまとめる batch API は、実装を複雑にし、設定画面では 1 操作 1 apply のオーバーヘッドが無視できるため採用しない。
- 推測 refresh を残す案や Store 内で accessory の差分を自動推測する案は、明示的な Diff と責務が重複するため採用しない。
- 常に例外を送出する案は本番のクラッシュリスクがあり、エラー callback は Store API を複雑にするため採用しない。

## Consequences

正の影響として、Core は UI 非依存の変更表現に集中し、UI 層は状態管理とプラットフォーム固有の適用に集中できる。閉じた Diff により適用側の未対応ケースをコンパイル時に検出でき、値比較可能な Diff はテストや重複検出にも使える。Root 全体の再構築や accessory 更新の推測を避け、更新意図を明示できる。

負の影響として、利用者は Root を直接差し替えず Store API を通じて更新する必要があり、初期 API からの移行が発生する。複数操作は複数回 apply され、単一アニメーションへの batch 化はできない。Diff の payload は既存モデルの等価性契約に依存し、無効な ID への操作は本番では適用されずログの確認が必要になる。

出典:

- `openspec/changes/archive/2026-05-15-add-partial-update-core/design.md` Decision 2–7
- `openspec/changes/archive/2026-05-15-add-partial-update-native/design.md` Decision 1–3, 7–8
