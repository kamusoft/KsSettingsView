---
id: 0003
title: 値型中心の Core モデルと薄い Cell 抽象
status: superseded
date: 2026-05-07
---

## Context

KsSettingsView は iOS と Android で論理的に同型の Core モデルを持つ一方、Swift と Kotlin では型システムとコレクションの扱いが異なる。Core モデルには、テストや一般的な比較で利用できる同一性・値等価の契約が必要である。

また、Cell ごとに異なるフィールドを値型で表現しつつ、異種 Cell のコレクションを安全に扱う必要がある。後続のスタイル責務の UI 層への移動により、Core の Cell 抽象から UI 固有のスタイル契約を除外する必要も生じた。

## Decision

Core モデルは値型を中心とし、Swift では `struct`、Kotlin では `data class` を一貫して採用する。

Cell の共通抽象は、Swift では protocol、Kotlin では sealed interface とする。Swift の異種 Cell コレクションには、Cell protocol を保持する型消去ラッパを使用する。Kotlin では sealed interface をそのまま使用し、型消去ラッパは設けない。

Core の Cell 抽象は同一性に必要な最小契約に絞り、スタイルプロパティを要求しない。スタイルが必要な具象 Cell は、UI 層で個別にそのプロパティを持つ。

## Alternatives Considered

- Core モデルを `class` / `open class` などの参照型にする案。継承による拡張は容易だが、`Hashable` / `equals` の手動実装が必要となり、変更検知の信頼性が落ちるため採用しない。
- Cell 抽象に抽象クラスを使う案。Swift の `struct` と相性が悪く、Kotlin でも sealed interface を使う方が `when` の網羅性を強制できるため採用しない。
- Swift で `[any KsCell]` を直接使う案。異種コレクションでは既存の `Hashable` 実装との相性が悪く、ハッシュが不安定になりやすいため採用しない。
- スタイル型をパラメーターにして Cell 抽象をジェネリック化する案。Swift の existential と Kotlin の interface としての扱いが複雑になり、既存コードへの影響も大きいため採用しない。
- Cell 抽象に弱く型付けしたスタイル値を残す案。型安全性が低下し、UI 層で型キャストが多発するため採用しない。

## Consequences

- Swift では `Hashable`、Kotlin では `equals` / `hashCode` の値型による実装を、テストや一般的な比較に利用できる。
- イミュータブルなスナップショットとして状態を扱えるため、MAUI からの状態更新を含む差分処理を単純化できる。
- Kotlin の sealed interface により、UI 層の `when` で Cell 種別の網羅性を検査できる。
- Core の Cell 抽象からスタイル責務が外れ、カスタム Cell 実装者は不要なスタイルプロパティを実装せずに済む。
- Swift の型消去では具象型へのキャストが必要になり、その処理を UI 側の型別分岐に集約する必要がある。
- 値型のため、深い Cell ツリーの一部を変更した場合もスナップショット全体が再生成される。
- iOS と Android は論理的な概念を揃えても、型消去の有無など言語仕様に合わせた差異を持つ。

出典: openspec/changes/archive/2026-05-07-add-settings-view-core/design.md

出典: openspec/changes/archive/2026-06-08-purify-core-extract-style-to-ui-layer/design.md
