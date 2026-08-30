---
scope: spec-review
kind: pain
severity: normal
count: 3
first-seen: 2026-08-02
last-seen: 2026-08-12
evidence:
  - add-maui-custom-cell (2 件同型: maui-cells「共有 Style の適用が例外にならない」の WHEN が MAUI 公開面に存在しない — `Style` は NavigableElement のみで `CellBase : Element` に適用不能。samples-maui「インライン構成の live 更新」の WHEN が同 Requirement のパリティ SHALL — Section ① は native と同一の静的 2 行 — と矛盾し実行不能。いずれも実装・verify フェーズで発覚しオーナー合意の deviation 記録で処理)
  - android-datepicker-spinner-wheel (Scenario「年候補件数が Int 上限を超える指定では提示しない」の GIVEN `minDate = LocalDate.MIN, maxDate = LocalDate.MAX` は年候補 1,999,999,999 件で Int.MAX_VALUE 未満のため到達不能。実装ワーカーが検出しタスク 4.1 を保留)
  - clarify-host-attach-order-contract (Requirement「view load 時の復元」が Store の現在状態に含まれない Root header / footer まで復元対象に含めて成立不能 — `SettingsRootStore.updateAccessory` は root ターゲットで root state を更新せず Diff のみ発行する。相方 spec-review がソース照合で検出 (second-opinion-001 Critical)、Root accessory 除外 + 所有者責務化で補正)
---

## ルール文

spec の Scenario / Requirement が置く前提 (数値境界への到達可能性、復元・取得系なら参照元がその状態を実際に保持していること等) は、入力型の値域やソース照合で成立可能性を検算してから提案に載せる。成立不能なら、その前提の Scenario を書かずに Requirement 本文で扱いを明記する (対象外化・防御的ガードの注記等) か、前提を成立可能な形に引き直す。

## 経緯

- 2026-08-02 android-datepicker-spinner-wheel: 提案レビュー (host + 相方) を通過した spec に到達不能 Scenario が残り、実装フェーズで実装ワーカーが検算して発覚。テスト不能なためタスク 4.1 が保留となり、オーナー判断 (Scenario 修正 or 上限の再定義) が必要になった。
- 2026-08-07 clarify-host-attach-order-contract: 提案段階の iOS Requirement が Root accessory を復元対象に含めたが、Store は Root accessory を現在状態として保持せず復元不能 (加えて E2E タスクの共通シナリオが Root accessory 更新を含み、仕様どおり実装しても E2E 目標を満たせない構造)。相方 spec-review の Critical で発覚し、design Decision 1 (復元対象を「Store が現在状態として保持するもの」に限定 + 所有者責務) で補正。数値検算 (1例目) に限らず「前提の成立可能性のソース照合」に一般化できる2例目。
