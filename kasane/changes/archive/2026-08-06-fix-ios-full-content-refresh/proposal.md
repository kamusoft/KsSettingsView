# Proposal: fix-ios-full-content-refresh

## Why

iOS の full 更新 (`applyFullSnapshot`) は snapshot を ID のみで構築し、`reloadSections` の対象も header / footer 差分の Section に限られるため、**同一 ID のまま内容が変わった表示中セルが stale のまま残る** (exploration.md 2026-08-06 の repro テストでバグ確定)。concepts「[表示状態同期](../../concepts/core/architecture/display-state-synchronization.md)」の保証「full 更新でも同一 ID の Cell の内容変化は取りこぼさない」が iOS では破られており、Android が [android/ADR-0012](../../decisions/android/0012-full-update-content-sync-diffcallback-and-setrootdirect.md) で解消した非対称 (full 経路だけ内容再適用の出口がない) と同型の欠落である。

## What Changes

`applyFullSnapshot` に、Android `setRootDirect` の対称形として**内容差分セルの一括 reconfigure** を追加する:

1. 旧・新 visible projection の**双方に存在し、値 (`==`) が変化した**同一 ID の Cell を検出し、snapshot 適用時に `reconfigureItems` で内容を再適用する。対象選定は純粋 helper として分離し、返却 ID 集合を単体テスト可能にする (second-opinion-001 M3)
2. 対象選定は ADR-0012 の規律に揃える: 新規挿入・削除・hidden の Cell には内容再適用を重ねない。対象が空でも構造反映は必ず実行する (初回適用は自然に対象空)。`reloadSections` で再構成される Section の Cell は reload 側が内容ごと再構成するため reconfigure 対象から除外する (この場合の行 identity 維持は保証しない — 既存契約どおり。second-opinion-001 M1)
3. 同一 ID のまま具象型が変わる Cell (`KsCellID` は UUID のみで等価判定のため同一 item と扱われる) は reconfigure では Native cell を維持できないため、cell 交換 (`reloadItems`) で内容を反映する (second-opinion-001 M2)
4. DSL の headerHeight preflight から `.replaceCell` 続発を廃止し、可視性 preflight と同じ `.full(newRoot)` のみの発行に統一する。続発は full 更新が内容反映を内包していなかった時期の暫定措置であり、残すと同一 Cell への内容再適用が二重に発火する (second-opinion-001 M4、ユーザー裁定 2026-08-06)。実装済み fix-dsl-header-height-diff 側の spec 改訂は不要 (Kasane では実装完了後の spec は足場として役目を終えている)
5. repro テスト (`_ReproFullContentRefreshTests.swift`) を正式テスト化し、デルタスペックの Scenario に対応させる

影響する能力: settings-view-ios-ui

## Non-Goals

- `.view` 形式 Section accessory の中身差し替えの full 更新反映 (`SectionAccessory` の `.view` 常時等価による既知の制約。`updateAccessory` 経由が正 — concepts 記載済みの現行契約を維持)
- DSL の可視性 preflight・通常差分経路の変更 (headerHeight preflight の続発廃止以外は触らない)
- Android 側の変更 (ADR-0012 で解消済み)

## Impact

- 公開 API 変更なし・可逆 (`applyFullSnapshot` 内部への処理追加のみ)
- full 更新の計算コストに旧∩新 visible の値比較が加わるが、全行照合が前提の経路であり同オーダー。reconfigure は内容が変わった行に限られ、行 identity は維持される (破棄なし)
- `applyFullSnapshot` は `.full` / `replaceAll` / `replaceSection` / DSL preflight の合流点のため回帰確認の重みがある (M 級の理由の一つ。ADR-0012 が Android で同じ理由を挙げた)
- 蒸留時の申し送り: concepts「表示状態同期」の「内容更新」節に iOS 側機構 (reconfigure による一括内容再適用) の追記が必要。ADR は新規に起こさず android/ADR-0012 の対称移植として扱う (蒸留時に再判断可)

## 級: M

コード差分は小さいが、concepts の保証に関わる契約挙動であり、対象選定のエッジケース (旧∩新限定・reloadSections との重複) を仕様で縛る価値が高いため。

domain: ios
