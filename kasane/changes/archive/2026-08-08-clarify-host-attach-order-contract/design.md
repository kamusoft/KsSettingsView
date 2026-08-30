# Design: clarify-host-attach-order-contract

## Context

iOS Host は viewDidLoad 前に届いた非 full Diff を破棄し、viewDidLoad で Store を再 pull しないため、「Host 生成 → 構造操作 → 取り付け」の順序で更新を取りこぼす (検証ホストで実測、コード調査で実装ギャップと確定)。Android は `onAttachedToWindow` の `resyncFromStore` で Store 現在状態を pull するため同順序でも安全で、両 OS の観察可能挙動が非対称。MAUI Handler はマッパー適用が view 階層への追加より必ず前に完結するため、この隙間を構造的に踏む。core/ADR-0019 (proposed) が方向 (Host は view load / attach 時に Store 現在状態から復元) を定めており、本 design はその適用範囲と境界条件を確定する。

## Goals / Non-Goals

- Goal: 取り付け順序によらず表示が Store 現在状態へ収束する契約を両 OS で成立させ、テストで固定する
- Non-Goal: view load / attach 前 Diff のイベントとしての保全 (収束のみ保証) / Store のデータ構造変更 / MAUI Handler の実装

## Decisions

### Decision 1: 復元対象は「Store が現在状態として保持するもの」に限定し、Root accessory を除外する

**採用案:** 復元保証の対象 = 設定ツリーの構造・Cell 内容・Section accessory・theme (いずれも Store の現在状態から pull 可能)。Root header / footer は Store の現在状態に含まれない (UI 層プロパティ、core/ADR-0005 の責務分離) ため復元対象外とし、**所有者 (呼び出し側 — MAUI facade やアプリ) が view load / attach 後に適用する責務**と明記する。
**理由:** `SettingsRootStore.updateAccessory` は root ターゲットで root state を更新せず Diff のみ発行する実装 (コメントで意図明言)。phase-2 の MAUI facade は `RootHeaderText` / `RootFooterText` を自分で保持するため再適用可能で、所有者責務モデルが成立する。
**代替案:**
- **A: Store が Root accessory も現在状態として保持する** — core/ADR-0005 (Root 装飾は View の責務) の supersede が必要になり、Store・両 OS Host・Bridge に波及する大きな変更。本変更の目的 (順序契約の隙間を塞ぐ) に対して過大なため却下

### Decision 2: Store 接続中の Theme は Store を正とする

**採用案:** Store 接続中は Store の theme が正。view load 時の復元は Store の theme を適用し、view load 前に公開 API `applyTheme` で直接適用された Theme は保持されない (Store 接続中の直接適用の併用は非保証と契約化)。
**理由:** Store = SSoT の原則と一貫し、復元の意味論が「Store 現在状態への収束」の一文で閉じる。
**代替案:**
- **A: 最後の直接適用を維持する (pull から theme を除外)** — 「どちらが勝つか」の状態管理が Host に増え、復元保証に theme だけ穴が残るため却下

### Decision 3: 収束の観測境界

**採用案:** iOS は **viewDidLoad 完了時点** (同期。復元は viewDidLoad 内で完結する)。Android は **attach 後、メインスレッドのキューが空になった時点** (theme の `StateFlow.collect` 開始と `submitList` が非同期のため eventual。テストは Robolectric 等でメインループを flush して判定する)。
**理由:** 現行実装の同期特性の違いをそのまま契約化でき、Android の実装変更 (同期化) を不要にする。
**代替案:**
- **A: 両 OS とも attach/load 完了時点の同期収束を要求** — Android の theme 反映経路と RecyclerView の非同期 submit を同期化する実装変更が必要になり、「Android はコード変更なし」の利点を失うため却下

### Decision 4: iOS の契約トリガーは viewDidLoad (window attach と区別する)

**採用案:** iOS の復元トリガーは viewDidLoad。view load は `loadViewIfNeeded()` や `.view` 参照でも発生し、window への attach とは独立のイベントとして扱う。テストの WHEN は「Store 更新後に `loadViewIfNeeded()` を呼ぶ」で統一する。
**理由:** UIKit の実際のライフサイクルに一致し、テストが検証すべきイベントが一意になる。
**代替案:**
- **A: window attach (didMoveToWindow) をトリガーにする** — 既存の viewDidLoad ベースの構築フローと二重の復元点ができ、Android との概念対応 (attach) の見かけの対称より実装の単純さを優先して却下

## Risks / Trade-offs

- iOS の公開挙動が2点変わる: 取りこぼし → 復元 (改善方向) / Store 接続中の直接 `applyTheme` の上書き (非互換の可能性があるが、Store 接続と直接適用の併用は元々未定義挙動)
- Root accessory の除外は「復元対象の非対称」を契約に残す — 所有者責務の明文化とテストで補う

## Migration Plan

単一 change で実施。iOS 改修 → 両 OS 回帰テスト → 検証ホストの回避策撤去の順。

## Open Questions

なし

## ADR 候補

- Decision 1〜3 は core/ADR-0019 (proposed) の適用範囲の確定として **ADR-0019 本文へ反映済み** (新規 ADR は不要)。Decision 4 は実装詳細のためコード+テストに任せる
