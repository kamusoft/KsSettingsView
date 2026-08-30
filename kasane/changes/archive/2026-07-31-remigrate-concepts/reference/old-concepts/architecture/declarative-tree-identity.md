---
type: concept
title: 宣言ツリーの安定同一性
description: 宣言UIの再評価をまたいでSectionとCellを継続追跡するための識別原則
tags: [architecture, identity, declarative-ui, diff]
timestamp: 2026-07-18
---

## 目的

宣言UIでは評価のたびにSectionとCellの値が再生成される。安定同一性は、一時的なインスタンスではなく意味のある識別情報から、画面上の同じ要素を継続して追跡するための契約である。

## 識別情報

動的コレクションでは要素の安定key、利用者が意味を与える場合は明示IDを使う。どちらもない静的構造に限り、親要素と構造位置から決定的なfallbackを導出できる。

位置fallbackは挿入や並べ替えで意味が変わるため、動的構造の同一性保証には使わない。

## 不変条件

- 意味的に同じ識別情報は、再評価をまたいで同じIDへ解決される。
- 表示内容、現在値、Style modifierの変更は同一性を変えない。
- 内容変更を別要素として扱わず、同一要素の再構成へ流す。
- ID再割り当て契約へ参加しない独自Cellでは、利用者が安定IDを保証する。
- 同じ設定ツリー内のSectionとCellのIDは一意である。

同一性は[表示状態同期](architecture/display-state-synchronization.md)における構造判定の入力となり、値等価とは別の契約である。

