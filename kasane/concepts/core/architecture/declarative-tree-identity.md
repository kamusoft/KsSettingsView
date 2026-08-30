---
type: concept
title: 宣言ツリーの安定 identity
description: 宣言 UI の再評価をまたいで Section と Cell を継続追跡する識別契約
tags: [architecture, identity, declarative-ui, diff]
timestamp: 2026-07-19
---

この文書は、SwiftUI / Compose の DSL が再評価ごとに生成する Section / Cell を同じ要素として追跡する契約を説明する。読むと、動的 key、明示 ID、位置 fallback の安全な使い分けと、内容を identity に含めない理由が分かる。

## identity の選び方

| 構造 | 使用する identity | 例 |
|---|---|---|
| 動的 collection | DSL `ForEach` / `forEach` の item key | 利用者データの `id` |
| 静的だが意味上の名前が必要 | `sectionID` / `cellID` の明示 hint | `"app-version"` |
| 追加・削除・並べ替えのない静的構造 | 親 ID・位置・型などから導く fallback | 固定された設定項目列 |

同じ要素に動的 collection key と明示 ID を併用しない。どちらか一方だけを identity の入力にする。accepted [ADR-0008](../../../decisions/core/0008-stable-declarative-tree-identity.md) と現行 iOS / Android 実装で併用時の優先順位が一致しないため、優先順位へ依存しないことが現在安全な利用契約である。

位置 fallback は挿入・削除・並べ替えで意味が変わるため、動的構造に使わない。一つの collection item から同じ階層へ複数 Section / Cell を返すと同じ key hint が付き得るため、一 item は一要素へ対応させる。

たとえば ID `42` の item を先頭から末尾へ並べ替えても、collection key `42` を使えば同じ要素として追跡できる。その要素へさらに `.cellID("manual")` / `cellID("manual")` を指定してはいけない。どちらが勝つかではなく、key だけを使う。

## resolved ID

identity の入力値と最終 ID は同じものではない。SwiftUI は hint から決定的な UUID、Compose は hint から決定的な String ID を解決する。明示 hint の文字列表現と最終 ID の一致へ依存しない。

hint は型も identity の一部として扱う。整数 `1` と文字列 `"1"` は表示上の文字が同じでも別の hint であり、同一要素として扱わない。

title、選択値、CellStyle などの内容を identity に含めない。同じ ID で内容が変わった Cell は同じ行の reconfigure / rebind へ流れる。

## 利用者定義 Cell

DSL が最終 ID を Cell 値へ再束縛するには、iOS は `DSLReidentifiable`、Android は `DSLReidentifiableCell` への準拠が必要である。非準拠 Cell は例外にせず元の ID を維持するため、利用者自身が再評価間の安定 ID を保証する。

## 保証すること

- 同じ identity hint から再評価をまたいで同じ ID を解決する。
- ID 入力の型を区別し、異なる型の同じ文字列表現を同一視しない。
- Cell の内容変更で Section / Cell identity を変えない。
- 動的 collection の追加・削除・並べ替えで既存 item の ID を維持する。

## してはいけないこと

- 動的構造を位置 fallback だけで追跡しない。
- 同じ要素で collection key と明示 ID を併用しない。
- 一つの collection item から同階層へ複数要素を返さない。
- 明示 ID の入力値と resolved ID が同じ文字列だと仮定しない。
- title、値、style を identity に含めない。

## 用語

| 用語 | 意味 |
|---|---|
| identity hint | collection key、明示 ID、静的位置など、最終 ID を決める入力 |
| resolved tree | hint を Section / Cell の最終 ID へ反映済みの宣言ツリー |
| 位置 fallback | 明示 key がない静的要素を親 ID・位置・型などから追跡する代替規則 |

## 関連

- [宣言 UI と Native Host の Bridge](declarative-ui-bridge.md)
- [表示状態同期](display-state-synchronization.md)
- [iOS SwiftUI Bridge](../../ios/api/ios-swiftui.md)
- [Android Compose Bridge](../../android/api/android-compose.md)
