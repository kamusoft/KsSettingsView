---
id: 0019
title: Host は view load / attach 時に Store 現在状態から表示を復元する
status: accepted
date: 2026-08-06
---

## Context

maui-bridge の検証ホストで「Host 生成 → 構造操作 → view 階層へ取り付け」の順序では iOS が view load 前の構造 Diff を取りこぼすことが実測された (clarify-host-attach-order-contract)。コード調査で次が確定した:

- iOS `KsSettingsViewController` は Store 購読を init 時に開始するが、viewDidLoad 前に届いた Diff は `.full` 以外を内部 root にすら反映せず破棄し、viewDidLoad では Store を再 pull せず init 時キャプチャの内部 root からスナップショットを構築する — Store の「現在値から復元できる」保証 (pull 型) を Host が使っていない実装ギャップ
- Android `KsSettingsView` は attach 前は Diff の collect を開始せず、`onAttachedToWindow` の `resyncFromStore` が Store 現在状態を pull して全復元する — 同じ順序でも取りこぼしは起きない
- MAUI の Handler はプロパティマッパー適用が親 view への追加より必ず前に完結する順序で動くため (MAUI ソースで確認)、MAUI Handler 層はこの隙間を構造的に踏む

## Decision

- 契約として「**Host は view load (iOS: viewDidLoad) / window attach (Android: onAttachedToWindow) の時点で Store の現在状態を pull して表示を復元する**」を両 OS の Host 保証に加える。これにより Host 生成・構造操作・取り付けの順序によらず、最終的な表示は Store の現在状態に収束する
- 復元の対象は **Store が現在状態として保持するもの** (設定ツリーの構造・Cell 内容・Section accessory・theme) に限定する。Root の header / footer は Store の現在状態に含まれない (UI 層プロパティ — ADR-0005 の責務分離) ため復元対象外とし、所有者 (呼び出し側) が view load / attach 後に適用する責務とする
- Store 接続中の theme は Store を正とする。iOS の直接適用 API (`applyTheme`) を Store 接続中に併用した場合の結果は保証しない (view load 時に Store theme で上書きされ得る)
- iOS は `KsSettingsViewController.viewDidLoad` を「接続中 Store の現在状態を pull して構築する」形に改修する (Android の `resyncFromStore` パターンへの対称化)
- Android は現行実装が既にこの契約を満たすため変更しない。両 OS 対称の回帰テストで契約を固定する

## Alternatives Considered

- **「Host は view load 前の構造 Diff を保証しない。取り付け前の操作は setRoot のみ保証」と契約に明文化するだけに留める (コード変更なし)**: MAUI Handler はマッパー適用が必ず取り付け前に完結するため、Handler 側に Loaded イベントまで操作を遅延・フラッシュする機構を常設する必要が生じ、恒久コストを利用側へ転嫁する。また Android は既にこの順序で安全なため「両 OS とも保証しない」という契約は実態と乖離し、Bridge/Host 直接利用者にも罠が残るため却下

## Consequences

- 正: MAUI Handler 層が取り付け順序を意識しない素直なマッパー実装で済む
- 正: 復元保証が両 OS で対称になり、maui/ADR-0007 の「makeHost* は Store 現在状態から復元した handle を返す」と同じ復元意味論で一貫する
- 正: Bridge / Host を直接利用するアプリからも取りこぼしの罠が消える
- 負: iOS Host の観察可能挙動が変わる (実装変更と両 OS 対称の回帰テストが必要)
- 負: view load / attach 前に届いた Diff はイベントとしては失われたまま (保証するのは最終状態への収束のみ)。attach 前の中間状態に依存する利用は今後も保証されない

---
出典: kasane/changes/archive/2026-08-08-clarify-host-attach-order-contract/exploration.md / kasane/roadmaps/maui-support/phases/phase-2-maui-core/artifacts/2026-08-06-attach-order-scout-findings.md / kasane/roadmaps/maui-support/phases/phase-2-maui-core/history.md (2026-08-06: Host 取り付け順序の契約の隙間)
