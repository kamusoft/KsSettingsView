# Proposal: fix-android-accessory-header-refresh

## Why

Android で `updateAccessory(SectionHeader, 既知の sectionID, 別の text)` を呼ぶと、Store の model は更新されるのに実描画の Section header が古いまま残る (Adapter 通知 0 件を実測)。`replaceSection` で section identity が同じまま header text を変えるケース、さらに `replaceSection` / `SettingsRootDiff.Full` で同一 id の Cell 内容が変わるケースも同じ理屈で取りこぼす。原因は、内容更新の部分更新経路 (`submitContentUpdate`) が `CellRow` しか対象にしておらず、Section H/F と full 更新経路の Cell 内容が「素の `submitList` + `areContentsTheSame` 常時 true」で無通知になること。iOS の `updateAccessory` は反映されるため OS 非対称でもある。

## What Changes

[android/ADR-0012](../../decisions/android/0012-full-update-content-sync-diffcallback-and-setrootdirect.md) (accepted) に従い2点を実装する:

1. `CellListItemDiffCallback`: SectionHeader / SectionFooter に限り `areContentsTheSame` を data 等価比較にし、`getChangePayload` で `PAYLOAD_CONTENT` を返して payload 付き rebind に落とす (CellRow の常時 true は維持)
2. `KsSettingsView.setRootDirect`: full 更新を「構造 = DiffUtil / 内容 = commit 後の一括 rebind」で完結させる。構造提出と内容通知を分離し、**`submitList` は内容通知対象が空でも必ず実行**、内容通知は「旧 visible リストと新 visible リストの双方に存在する cell id」だけへ発行する (second-opinion-001 の Critical / Major 指摘による補正。初回 setRoot・空 root への更新はこの規則に自然に包含される)

影響する能力: settings-view-android-ui

## Non-Goals

- iOS 側の header 取りこぼし修正 ([fix-replace-section-header-refresh](../fix-replace-section-header-refresh/exploration.md)、原因が別)
- CellRow への内容比較拡大 (ADR-0012 で却下済み: リスナー等で等価が安定しない)
- `RootHeaderFooterAdapter` の payload なし通知の是正 ([fix-root-accessory-payload-notify](../fix-root-accessory-payload-notify/exploration.md) の責務)
- Root header / footer の更新経路 (`AccessoryTarget.RootHeader` / `RootFooter`) の変更
- add-maui-native-bridge 由来の Bridge 契約テストの強化 (header text 変更ケースの追加) — 本修正の完了後に follow-up として別途判断する (完了判定が曖昧なタスクを本 change に含めない)

## Impact

- 公開 API 変更なし。破壊的変更なし
- `setRootDirect` は setRoot / Full / ReplaceSection / 可視性フォールバックの共通出口のため、回帰確認の重みが本変更の主リスク (M 級とした理由)。payload 付き rebind は既存のちらつき・IME 対策 (android/ADR-0001) と同一機構であり演出面の変化はない
- 蒸留時の申し送り: concept「表示状態同期」(core/architecture/display-state-synchronization.md) の Android 欄と、KsSettingsListAdapter の「areContentsTheSame は常に true」コメントの文言追随が必要 (コメント修正は本変更のタスクに含む。concept は ksn-distill で)
- ADR-0012 は second-opinion-001 の Critical / Major 指摘 (空 root 取りこぼし・二重通知・型切替時の ViewHolder 保証) を受けて本文の補正が必要 (実装前補正。ユーザー承認のうえ適用)

## 未決事項 (Open Question)

なし。`SectionAccessory.View` 同士の内容変更は DiffCallback での **KsAnyView 参照比較**で検出する (ユーザー決定 2026-08-05。core の `View.equals` は変更しない — ADR-0012 補正に記録)

## 級: M

`setRootDirect` (コア経路) と DiffCallback の契約変更を伴うため「迷ったら1段上」で M (ユーザー確定 2026-08-05)。

domain: android
