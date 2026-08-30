---
id: 0013
title: 値型中心のモデルで拡張可能な Cell 抽象を使う
status: accepted
date: 2026-07-18
supersedes: 0003
---

## Context

ADR-0003 は値型中心の Core モデルと薄い Cell 抽象を採用した一方、Swift の異種 Cell 集合には型消去ラッパ、Kotlin の Cell 抽象には `sealed interface` を使うとした。

現行実装では、Swift はプロトコル型の異種集合を直接保持し、Kotlin は外部モジュールから独自 Cell を実装できる通常の interface を使用している。Kotlin の sealed 制約は、Sample や利用側モジュールで独自 Cell を定義する拡張点と両立しない。

値型中心と薄い共通契約は維持しつつ、各言語の現行機能と外部拡張性に合わせて Cell 表現を改める必要がある。

## Decision

Core モデルは引き続き値型を中心とし、Swift では `struct`、Kotlin では `data class` を具象モデルの基本とする。

Cell の共通抽象は識別子と値等価に必要な最小契約に絞り、UIスタイルを要求しない。

- Swift は型消去ラッパを設けず、プロトコル型の異種集合を直接保持する。値等価は各具象 Cell の契約へ委譲する。
- Kotlin は通常の interface とし、ライブラリ外のモジュールから独自 Cell を実装できるようにする。
- Kotlin の網羅的な型分岐より、Registryを介した外部拡張性を優先する。

## Alternatives Considered

- Swift に専用の型消去ラッパを維持する案。現行言語機能ではプロトコル型の異種集合を直接保持でき、追加ラッパを維持する必要がないため採用しない。
- Kotlin を `sealed interface` に戻す案。コンパイル時の網羅性は得られるが、別モジュールから独自 Cell を実装できなくなるため採用しない。
- Cell を抽象クラスにする案。Swift の値型と両立せず、Kotlinでも継承を要求して外部拡張の自由度を下げるため採用しない。
- UIスタイルをCoreのCell共通契約へ戻す案。UI層へ分離したスタイル責務をCoreへ再び混入させるため採用しない。

## Consequences

- Swiftでは追加の型消去型なしで異種Cellを保持できる。
- KotlinではSampleや利用者モジュールが独自Cellを定義できる。
- CoreのCell抽象は薄く保たれ、具象Cellとスタイルの責務をUI層へ置ける。
- Kotlinではsealed型の網羅性検査を利用できないため、未登録の独自CellはRegistry境界で検出する必要がある。
- Swiftのプロトコル型集合は値等価の自動合成ができないため、集合を保持する値型が各要素の値等価へ明示的に委譲する必要がある。

出典: `ios/Sources/KsSettingsViewCore/KsCell.swift`

出典: `ios/Sources/KsSettingsViewCore/Section.swift`

出典: `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/Cell.kt`

出典: `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/Section.kt`

出典: `openspec/specs/settings-view-core/spec.md`「Cell 抽象」「Section ドメインモデル」

出典: `kasane/decisions/0003-value-oriented-core-model.md`

出典: 2026-07-18 ユーザー判断「0013は承認」
