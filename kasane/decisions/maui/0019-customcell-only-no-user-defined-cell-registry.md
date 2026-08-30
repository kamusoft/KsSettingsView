---
id: 0019
title: MAUI のカスタムセルは CustomCell (content + builder) までを提供し、利用者定義 Cell (Registry 公開) は提供しない
status: accepted
date: 2026-08-12
---

## Context

現行コア契約のカスタムセルは3層で構成される ([custom-cell.md](../../concepts/core/cells/custom-cell.md)): ① CustomCell のインライン利用、② 固定 builder + content 型を与えた CustomCell を返す再利用形 (①があれば登録不要で手に入る)、③ 自前 Cell 型 + Renderer + `register` による利用者定義 Cell ([cell-renderer-registry.md](../../concepts/core/architecture/cell-renderer-registry.md))。

MAUI facade の Cell 輸送は per-type 展開 (facade 派生・Snapshot・Bridge DTO を種ごとに 1:1 — [ADR-0011](0011-per-type-cell-dto-transport.md)) であり、輸送に乗る Cell 型の集合はライブラリが定義する閉じた集合である。

## Decision

- MAUI 公開面はカスタムセル3層のうち **①② 相当 (CustomCell とその再利用形) までを提供**する
- **③ 利用者定義 Cell は MAUI では提供しない**。`KsCellRegistry` (iOS / Android の登録機構) を MAUI 公開 API に露出しない。Registry は native 側利用者向けの拡張境界のまま維持する
- ②は追加機構なしで成立する — C# のファクトリメソッド形 (`static CustomCell SliderCell(...)`) または CustomCell 派生サブクラス形のどちらでもよい。等価性は型ではなくプロパティ値 (content 等) で決まり、facade → DTO の型スイッチは派生型を CustomCell として受ける

## Alternatives Considered

- **③まで提供 (Registry を MAUI 公開)**: 利用者定義 Cell 型は per-type 輸送 (ADR-0011) に乗らず、native 側に対応する model 型も Renderer も存在しない。描画には利用者自身が Swift/Kotlin で Renderer を書き binding 拡張まで用意する必要があり、「C# だけで完結して使う」という MAUI 利用者像から外れる。輸送を union 的な汎用 DTO で逃がす案は ADR-0002 の union 却下経緯と衝突する。却下
- **CustomCell 自体も非提供 (カスタムセル全部を先送り)**: MAUI 版 CustomCell の content は任意 MauiView であり、「プリセット外 UI を1行差し込む」需要はライブラリ利用の主要ユースケースに含まれるため却下

## Consequences

- 正: MAUI の公開面と輸送契約が閉じた Cell 型集合のまま保たれ、Bridge / Binding の複雑化を避けられる
- 正: ①の content が任意 MauiView のため、プリセット外 UI の差し込み需要は①②で実用上満たせる
- 正: ③の後付け提供は公開面の追加であり可逆 (将来必要になった時点で再検討できる)
- 負: MAUI 利用者は独自の型 identity・スタイル解決への参加・共通行レイアウトへの参加を持つ一級市民 Cell を作れない (CustomCell は full-bleed のみ)
- 負: native (SwiftUI / Compose) 側と提供層が非対称になり、ドキュメントで差を明示する必要がある

---
出典: kasane/roadmaps/maui-support/phases/phase-5-custom-cell/history.md (2026-08-12: カスタムセル3層のうち MAUI でどの層まで提供するか)
