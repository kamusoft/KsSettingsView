---
id: 0005
title: Root と Section の装飾責務を分離する
status: accepted
date: 2026-05-15
---

## Context

初期設計では Section の Header・Footer に Cell 抽象を流用し、Root の Header・Footer も `SettingsRoot` の値として保持していた。しかし Header・Footer は選択・編集される行ではなく装飾領域であり、任意 View は意味のある値等価比較もできない。さらに Root 装飾はリスト全体の View に属し、Section 装飾とは UI 上の役割と描画レイヤが異なる。

## Decision

Section の Header・Footer は Cell ではなく、文字列または型消去した任意 View を持つ `SectionAccessory` として表現する。任意 View は Cell 抽象から分離し、値等価による差分検出には参加させない。内容更新は各描画レイヤの再構成に委ねる。

Root の Header・Footer は `SettingsRoot` のデータから分離し、Native View 側のプロパティとして扱う。SwiftUI では View modifier、Compose では slot 引数から設定する。Root 用と Section 用の accessory 型は、将来の役割差を型で保つため区別を維持する。

## Alternatives Considered

- Section 装飾に Cell 抽象を流用し続ける案は、装飾領域へ Cell 概念を混入させるため採用しない。
- Section 装飾を任意 Cell のみにする案は、一般的な文字列 Header を簡潔に表す手段を失うため採用しない。
- Root と Section の accessory を単一型へ統合する案は、ピン留め、テーマ継承、Safe Area、描画レイヤなど将来の挙動差を型で表せなくなるため採用しない。
- 任意 View の identity を利用者に指定させる案は、API の簡潔さを著しく損なうため採用しない。クロージャの参照同一性や型情報による比較も、内容の同一性を正しく表せないため採用しない。
- Root Header・Footer を `SettingsRoot` に残す案は、データと View の責務が混在し、Core の等価性契約と Diff API を複雑にするため採用しない。
- Root Header・Footer を Store のプロパティにする案は、Store の責務を肥大させ、宣言 UI の自然な指定方法から外れるため採用しない。

## Consequences

正の影響として、Cell、Section 装飾、Root 装飾の責務境界が明確になる。Core の Root モデルと等価性契約が単純になり、SwiftUI・Compose・MAUI は各 UI の慣用的な方法で Root 装飾を指定できる。Root と Section の型を分けることで、将来それぞれに異なる振る舞いを追加できる。

負の影響として、shape が似た Root 用と Section 用の型を別々に維持する必要がある。任意 View は値等価で変更を検出できないため、更新を描画レイヤまたは明示的な更新経路へ委ねる必要がある。また、初期の `SettingsRoot.header/footer` を利用するコードは移行が必要になる。

出典:

- `openspec/changes/archive/2026-05-09-refactor-accessory-and-root-hf/design.md` Decision 1–3
- `openspec/changes/archive/2026-05-15-add-partial-update-core/design.md` Decision 1, 4
- `openspec/changes/archive/2026-05-15-add-partial-update-native/design.md` Decision 5–6
