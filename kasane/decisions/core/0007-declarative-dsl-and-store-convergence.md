---
id: 0007
title: 宣言 DSL と Store API を併存させ同じ更新経路へ収束する
status: accepted
date: 2026-05-18
---

## Context

Store API は Root 全体の再構築を避け、大量データや高頻度更新を効率よく扱える一方、静的な設定画面でも Store の事前構築と命令型操作を要求し、SwiftUI / Compose の宣言的な書き方から乖離していた。初期の DSL は簡潔だったが、値型 Root の全再構築と accessory 更新の推測を伴っていた。典型用途の簡潔さを取り戻しつつ、Native UI に確立した Store / Diff 経路を維持する必要があった。

## Decision

一般的な静的・中規模の設定画面には宣言 DSL、高度な大量データ・高頻度更新には Store API を提供し、両方を公開 API として併存させる。

DSL は専用の Native 更新経路を持たない。SwiftUI は View identity に結び付く `@StateObject`、Compose は `remember` で内部 Store と前回の宣言ツリーを保持する。再評価された宣言ツリーと前回ツリーの差から `SettingsRootDiff` 列を算出し、Store を経由して既存の Native `applyDiff` へ順次適用する。これにより DSL と利用者管理の Store は同じ状態管理・差分更新経路へ収束する。

## Alternatives Considered

- DSL だけを公開し Store を内部へ隠す案は、大量データや無限スクロールで命令型更新を選ぶ手段が失われるため採用しない。
- Store だけを公開する案は、典型的な設定画面でも宣言 UI 利用者に不自然で冗長な API を強いるため採用しない。
- Store と DSL を同じ initializer で組み合わせる案は、DSL が初期値なのか継続反映される定義なのか曖昧になるため採用しない。
- DSL 専用に Root setter を復活させる案は、Native UI に二系統の状態管理を持ち込むため採用しない。
- Diff 算出を Native 層へ移し、変更を推測させる案は、廃止した推測 refresh を再導入するため採用しない。
- Store を介さず Controller / View へ Diff を直接渡す案は、observable state による状態管理を共有できないため採用しない。
- DSL の内部状態に値型 Root を持ち、再評価ごとに Native へ全代入する案は、Root 全代入を廃止した性能方針に反するため採用しない。
- DSL 利用者自身に Store の保持を要求する案は、Store API と同じ書き味になり DSL の意味を失うため採用しない。
- Singleton Store は View ごとの状態を分離できず、メモリリークの懸念があるため採用しない。

## Consequences

正の影響として、典型用途では宣言的で簡潔な API を使え、大量データや高頻度更新では Store を直接操作できる。両 API が同じ Store、Diff、Native 適用経路を共有するため、Native UI を変更せずテスト・デバッグ・更新ログを共通化できる。内部 Store は同じ View identity の間、再評価をまたいで状態を維持する。

負の影響として、公開 API の選択肢が増え、用途ごとの使い分けを案内する必要がある。DSL は再評価ごとに宣言ツリー全体を構築して比較するため、データ量の増加に伴い O(N) のコストが増える。また内部 Store の寿命は SwiftUI / Compose の View identity に依存し、親 View の条件分岐などで identity が変わると状態がリセットされる。

出典:

- `openspec/changes/archive/2026-05-18-add-declarative-dsl/design.md` Decision 1–2, 6
