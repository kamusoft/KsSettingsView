---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-08-22
last-seen: 2026-08-22
evidence:
  - fix-ios-entrycell-writeback-race (Android の書き戻しレース修正を「同型の構造が iOS にもある (コードで確認済み)」として M 級提案化し、デルタスペック 5 Requirement + ios/ADR-0004 + 相方 spec-review まで作成。実装前の再現ゲート (tasks グループ 1) で Simulator 3 条件 + 実機 pixie4 の計 8 セットを実測して FAIL 0、修正前ビルドで欠陥を 1 件も再現できずゲート不成立)
---

## ルール文

先行 platform の欠陥修正を「同型」として別 platform へ移植する提案では、往復経路の存在ではなく**欠陥が成立する機構** — 「古い値が render / bind に届く実体」— を両 platform のソースで 1 対 1 に突き合わせ、移植先にも実在することを根拠付きで proposal に書く。突き合わせるのは値の**保持方式**である: 配信側が更新時点のスナップショットを保持する実装 (Android の adapter / AsyncListDiffer 等) では古い値が届き得るが、配信側が描画時点で最新の索引を引く実装 (iOS の `cellProvider` が `cellIndex` をライブ参照する等) では同じ往復があっても古い値は届かない。「同型の往復がある」「同じ callback → コミット → 再 render の形をしている」はこの根拠にならない。突き合わせで実在を確定できない場合は、デルタスペック・ADR の作成前に実測 spike を**提案化の前提条件**として置く (spike をタスク先頭に置くだけでは、反証時に提案一式が無駄になる)。

## 経緯

- 2026-08-22 fix-ios-entrycell-writeback-race: exploration / proposal / ios-ADR-0004 はいずれも「打鍵 → `onTextChanged` → 呼び出し側のコミット → `reconfigureItems` による同一 Native cell の再 render」という**往復の存在**を根拠に、android/ADR-0014 と同型の窓が iOS にもあると机上で確定した。実装ゲート (tasks グループ 1) の実測は、Simulator (mobilecli `io text` 約 33ms/文字 / WDA `frequency` 1000・3000 の 1〜3ms/文字) と実機 pixie4 の計 8 セット・有効 165 試行で **FAIL 0**。加えて一時ログで「`editingChanged` が 4〜10 回連続した後に届く `render` の `cell.text` は必ず最新値」であることを観測した。

  原因は配信側の保持方式の差だった。iOS の `KsSettingsViewController.cellProvider` は render 時点で `self.cellIndex[itemID]` を**ライブに引く**設計で、`applyReplaceCell` は `dataSource.apply` より前に同期で `cellIndex[cellID] = new` を更新する。よって `apply` が遅延しても「更新時点で握った古い Cell」で render される経路が存在しない。Android の RecyclerView adapter が submitList 時点のリストを保持するのとは非対称であり、この差は提案時に両者の配信コードを並べていれば読めた。

  ゲートが実装より前にあったため (process L-004) コード修正の手戻りは 0 だが、デルタスペック 5 Requirement・ADR・相方 spec-review 1 往復は前提が反証された状態で作られた。**spike の位置を「実装の前」から「提案化の前」へ引き上げる**のが、この型の再発時に効く差分。

  (2026-08-22 時点でオーナーの処遇判断は未確定 — 探索差し戻し / 予防的実装 / 対応不要アーカイブのいずれか。MAUI 経路 (`ScheduleFlush` の dispatcher post が**古い値を積んだまま**配信し得る) は proposal の Non-Goals により未計測で、そこに窓が残る可能性は否定されていない。判断確定後に本記録の射程を距離を置いて見直すこと)
