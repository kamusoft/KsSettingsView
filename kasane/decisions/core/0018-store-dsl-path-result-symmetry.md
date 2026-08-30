---
id: 0018
title: Store と DSL の更新経路間で観測結果の対称性を対称テストで保証する
status: accepted
date: 2026-08-05
---

## Context

ADR-0007 は宣言 DSL と Store API を併存させ、`SettingsRootDiff → Store → Native Host` の同じ適用経路へ収束させた。しかしこの収束が保証するのは適用側だけであり、DSL 経路にのみ存在する検出層 — 宣言ツリーの再評価差分を `SettingsRootDiff` 列へ翻訳する `DSLDiffCalculator` — が「モデルのどのプロパティ変化を diff に翻訳できるか」を律する決まりはなかった。

このため `Section.headerHeight` のみの変更が、Store / View API 経由では表示へ反映される (fix-android-header-height-refresh で修正・実機検証済み) のに、宣言的 DSL 経由では両 platform とも diff 0 件となり表示が更新されない欠落が発生した (fix-dsl-header-height-diff)。この失敗は「diff 0 件 → 何も起きない」という無音の形をとり、エラーにもならないため、Store 側を修正して実機検証を通しても DSL 側の欠落には気づけなかった。人の注意だけでは同じすり抜け方で再発する。

## Decision

- Store の公開操作で表示へ反映される観測可能な変化は、宣言的 DSL 経由で同じ変化を与えたときも同じ表示結果へ到達しなければならない。反映しない場合は「明示的に非対応」として文書化する (無音の取りこぼしを契約違反とする)。
- この対称性は観測結果に対する契約であり、検出層の実装形 (早期 return の条件・発行する diff の種別) は platform ごとに異なってよい。実装は縛らず、結果の等価だけを保証する。
- 動的反映の対象プロパティを追加・修正するときは、Store 経由と DSL 経由の両方の反映テストを併せて追加することを義務とする (対称テスト)。DSL 側は既存の検出層テスト (Android: `DSLDiffCalculatorTest.kt` / iOS: `DSLDiffCalculatorTests.swift`) に、Store 側は各経路の既存テストに追う。

## Alternatives Considered

- 文書のみ (ADR + concepts の「保証すること」への追記) で守る案は、無音の失敗に対して人の注意頼みとなり、今回と同じすり抜け方をするため採用しない。
- レビュー観点 (lessons / ksn-review の観点リスト) への追加のみで守る案は、レビュアー依存で強制力が不足するため採用しない。
- platform 間で検出層の実装形まで統一する案は、早期 return の設計が platform ごとに異なる現状 (Android は `sameStructure` の構造比較、iOS は値等価) を作り直すコストに対し、保証したい観測結果の等価には寄与しないため採用しない。

## Consequences

正の影響として、経路間の無音の取りこぼしが対称テストの欠けとして目に見えるようになり、片経路だけの修正が混入しにくくなる。検出層の実装は platform ごとに自由なままで、既存テスト基盤に追う形のため新しい仕組みは不要。fix-dsl-header-height-diff の headerHeight 対称テストが最初の適用例になる。

負の影響として、動的反映プロパティごとにテストが2経路 × 2 platform 分増える。また「観測可能な変化」に該当するかの判断は実装者に委ねられるため、判断に迷うプロパティは提案・レビューで明示する運用が要る。

出典:

- 本探索の会話 (2026-08-05、/ksn-explore)
- `kasane/changes/fix-dsl-header-height-diff/exploration.md` (構図のコード裏取り・経路の切り分け)
