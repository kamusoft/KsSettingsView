---
id: 0010
title: 表示状態同期を構造・内容・可視性に分離
status: accepted
date: 2026-06-14
---

## Context

Cell の内容プロパティまで構造 Diff の比較に使うと、チェックやスイッチの状態変更が Cell の差し替えとして扱われ、ViewHolder / CellView の再 bind や再生成によるちらつきが発生した。内容を値等価から除外する暫定対処では、プロパティごとの不整合が残り、値型としての契約も歪む。

その後、model 内に Section / Cell を保持したまま非表示にする機能が加わった。可視性変化は、ID 集合を扱う構造同期にも、同一 Cell を再構成する内容更新にも収まらないため、当初の二層分離を三層へ改訂する必要が生じた。

## Decision

表示状態同期を次の三経路に分離する。

1. 構造同期: Section / Cell の追加、削除、移動、ID 変更を ID 同一性だけで判定する。内容を含む値等価は構造 Diff に使わない。
2. 内容更新: 同じ ID の Cell は破棄せず、Android では ViewHolder の部分更新、iOS では `reconfigureItems` によって内容を反映する。`replaceCell` は同一 ID の内容再構成を意味し、ID が変わる差し替えは remove + insert などの構造差分で表す。
3. 可視性変化: hidden を含む model と visible のみの projection を分けて管理し、可視性変化では Full 経路で visible projection を再構築する。通常の Replace や内容更新には可視性差分を流さない。

Cell の `equals` / `hashCode` / `Hashable` は内容を含む通常の値等価として維持する。可視性の切り替えを Replace で表してはならず、UI 層は Replace で可視性変化を受けた場合に Full 経路へ防御的にフォールバックする。

部分 Diff の index は hidden を含む model 配列上の位置とし、UI 層が visible projection 上の位置へ変換する。hidden 対象への操作では model を更新し、表示上の操作は行わない。

## Alternatives Considered

- 内容を含む値等価で Diff し、アニメーションだけ無効化する案: Cell の再生成自体が残り、ちらつきの原因を除去できないため不採用。
- 内部状態だけを値等価から除外する案: title など他の内容変更では Replace が続き、値型の契約も一貫しないため不採用。
- 可視性変化を Replace に乗せる案: reconfigure では projection からの出現・消失を表現できず、内容更新と構造同期の境界を壊すため不採用。
- 可視性専用の Diff ケースを追加する案: API 表面を増やさなくても Full 経路で意図を表現できるため現時点では不採用。
- 可視性変化を細粒度の insert / remove に翻訳する案: 実装コストとリスクに対して得られる効果が限定的なため不採用。
- index を visible projection 基準にする案: hidden の有無で同じ index の意味が変わるため不採用。

## Consequences

- 正: 内容変更で Cell を破棄せず、チェック系操作などのちらつきを抑えられる。
- 正: 値型として自然な等価性を保ちながら、構造 Diff の責務を ID 同一性に限定できる。
- 正: 非表示要素を model に保持したまま、visible projection を一貫して再構築できる。
- 負: UI 層は hidden を含む model と visible projection の二つを整合させ、model index を表示 index に変換する責務を持つ。
- 負: 可視性変化は Full 経路を使うため、高頻度または非常に大きなツリーでは細粒度更新よりコストが高くなる。
- 負: 内容更新の見た目は即時反映中心となり、差し替えに伴うアニメーションは失われるか変化する。

出典: openspec/changes/archive/2026-06-03-refactor-display-state-sync/design.md
出典: openspec/changes/archive/2026-06-14-add-visibility-flags-section-and-cell/design.md
