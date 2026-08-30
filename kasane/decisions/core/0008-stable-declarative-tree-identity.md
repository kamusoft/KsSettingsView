---
id: 0008
title: 宣言ツリーの安定同一性
status: accepted
date: 2026-05-18
---

## Context

SwiftUI の `body` 再評価や Compose の Recomposition では、宣言ツリーの値型が毎回新しく生成される。生成時のランダム UUID を最終 ID にすると再評価のたびに同一性が変わり、同じ Section / Cell であっても Diff が追加・削除として扱う。

一方、利用者にすべての ID の明示を要求すると宣言 DSL の書き味を損なう。動的コレクションでは項目のキーを引き継ぎ、静的な宣言では再評価をまたいで再現可能な情報から ID を導出する必要がある。

## Decision

宣言ツリーの Section / Cell ID は、動的コレクションのキー、明示 ID、安定した位置情報の順で解決する。

SwiftUI では DSL 専用の `ForEach` に `Identifiable` 版と `id:` KeyPath 版を提供する。Compose では DSL 専用の `forEach` に `key:` lambda 版と `KsIdentifiable` 版を提供する。ルート用と Section 内用をそれぞれ用意し、動的項目のキーを Section / Cell ID に引き継ぐ。

Section ID は次の優先順で解決する。

1. `ForEach` / `forEach` 配下では項目の `id` / `key`
2. 明示された Section ID
3. テキストヘッダがある場合はルート位置とヘッダ文字列のハッシュ
4. それ以外はルート位置

Cell ID は次の優先順で解決する。

1. `ForEach` / `forEach` 配下では項目の `id` / `key`
2. 明示された Cell ID
3. Section ID、Section 内位置、Cell 型のハッシュ

具象 Cell のコンストラクタが持つ既定のランダム ID は、DSL 経路では最終 ID として使わず、上記規則で再束縛する。Store 経路では利用者が明示 ID と既定 ID のどちらを使うか選択できる。

## Alternatives Considered

- すべてランダム UUID で自動採番する案: 宣言ツリーの再評価ごとに ID が変わり、Diff が機能しないため不採用。
- 利用者に常に明示 ID を要求する案: DSL の書き味が悪化し、SwiftUI の宣言的な流儀から離れるため不採用。
- 位置を使わず内容ハッシュだけで ID を作る案: 内容変更が別 Cell と判定され、フォーカスなどの状態が失われるため不採用。
- SwiftUI 標準の View 用 `ForEach` をそのまま受け入れる案: 内部の data / id / content を DSL builder から取り出す公式手段がないため不採用。
- Compose で `KsIdentifiable` または `key:` lambda の一方だけを提供する案: iOS との並列性か Compose 標準の作法のどちらかを損なうため、両方を併存させる。

## Consequences

- 正: 宣言ツリーを再評価しても Section / Cell の同一性が維持され、Diff と Cell の表示状態を安定して引き継げる。
- 正: 静的な宣言では ID の記述を省略でき、動的コレクションでは各 UI フレームワークに馴染むキー指定を利用できる。
- 負: 位置ベースのフォールバックは、明示 ID やコレクションキーなしで要素を挿入・並べ替えする構造には弱く、その場合は利用者が明示 ID を指定する必要がある。
- 負: SwiftUI / Compose の標準 API とは別に、DSL 専用の `ForEach` / `forEach` と ID 再束縛規約を保守する必要がある。

出典: openspec/changes/archive/2026-05-18-add-declarative-dsl/design.md
