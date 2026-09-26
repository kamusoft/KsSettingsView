---
id: 0036
title: 宣言ツリーで明示 ID と collection key が同じ要素に付いたときは明示 ID を採用する
status: accepted
date: 2026-09-26
amends: 0008
---

## Context

core/ADR-0008 は宣言ツリーの Section / Cell ID の解決順を「`ForEach` / `forEach` 配下の項目の `id` / `key` → 明示 ID → 位置 fallback」と定めた。この順序では、同じ要素に collection key と明示 ID (`.sectionID` / `.cellID`、Compose の `sectionID` / `cellID`) の両方が付いた場合、collection key が勝つ。

iOS / Android の DSL 実装は、この併用時の順序を逆に解決している。iOS は hint の記録時に既存の明示 ID を collection key で上書きせず、記録順によらず明示 ID が勝つ。Android は `forEach` が hint を持たない要素にだけ collection key を付けるため、`content` 内で付いた明示 ID が残り、`forEach` の後から付けた明示 ID も collection key を上書きする。両 platform の実装は互いに一致しており、ADR-0008 の順序と一致していないのは併用時の 1 点だけである。

概念ドキュメント (kasane/concepts/core/architecture/declarative-tree-identity.md) は、この不一致を理由に「同じ要素に collection key と明示 ID を併用しない」を利用契約として案内している。

前提: 併用は推奨される書き方ではなく、利用者は通常どちらか一方だけを identity の入力にする。

## Decision

同じ Section / Cell に collection key と明示 ID の両方が付いた場合は、明示 ID を採用する。明示 ID 同士が重なった場合は後から付いたものを採用する。

ADR-0008 の Decision のうち、Section ID と Cell ID の優先順の 1 位と 2 位の関係だけを置き換える。collection key も明示 ID もない要素の位置 fallback、DSL 専用 `ForEach` / `forEach` の提供、DSL 経路での ID 再束縛は ADR-0008 のまま生かす。

## Alternatives Considered

- 実装を ADR-0008 の順序 (collection key 優先) へ戻す案: iOS / Android の両実装を変えることになり、併用している既存コードでは resolved ID が変わって、差分が同一要素の更新ではなく削除と追加として扱われる。併用は推奨されない書き方であり、順序を戻す利得がこの影響に見合わないため不採用。
- 併用をエラーとして検出する案: DSL の評価中に検出の段を新設する必要があり、既存の併用コードを実行時に壊す。優先順位を決めれば足りる問題に対して重いため不採用。

## Consequences

- 正: 決定記録と iOS / Android の実装が一致し、併用時の resolved ID を ADR から読み取れる。
- 正: 明示 ID は利用者が意図して書いた値であり、collection key より強い意図として扱われる。
- 負: `ForEach` / `forEach` の content 内で項目ごとに変わらない明示 ID を付けると、全項目が同じ ID に解決され、collection key による追跡が効かなくなる。
- 負: 併用を禁止する仕組みは持たないため、「併用しない」の案内は引き続き文書に頼る。

## Revisit When

- DSL に identity hint の検証段 (重複 ID の検出など) を設けるとき
- 前提 (Context) が崩れたとき

---
出典: kasane/concepts/log.md (2026-09-26 drift のディープ検証) / kasane/concepts/core/architecture/declarative-tree-identity.md (identity の選び方)
現行照合: 2026-09-26 確認。ios/Sources/KsSettingsViewSwiftUI/DSLHintRegistry.swift (`shouldOverride` で `(.explicit, .forEach)` を上書きしない) と android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DSLScope.kt (`forEach` は `identityHint == null` の要素にだけ key hint を付ける) で明示 ID 優先を確認。判定: 維持
