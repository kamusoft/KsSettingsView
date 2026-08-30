# Exploration: fix-android-accessory-header-refresh

- 起票日: 2026-08-04
- 起票経緯: add-maui-native-bridge 実装中 (グループ4・Android Bridge) にワーカーが発見した既存バグの簡易起票
- 探索日: 2026-08-05 (コードでの裏取り・修正方向の確定)

## 課題

Android で `updateAccessory(SectionHeader, 既知の sectionID, 別の text)` を呼ぶと、Store の model は更新されるが**実描画の Section header が古いまま**になる (Adapter 通知 0 件を実測)。

### コードで裏取りした構図 (2026-08-05)

- `CellListItemDiffCallback.areContentsTheSame` の常時 true は「表示状態同期の三層分離」原則に基づく**意図的な設計** (KsSettingsListAdapter.kt:255)。内容更新は `submitContentUpdate` → `notifyItemChanged(PAYLOAD_CONTENT)` の部分更新経路で反映する建付け
- その部分更新経路が **`CellListItem.CellRow` しか対象にしていない** (KsSettingsListAdapter.kt:78)。Section H/F だけが内容更新経路から漏れているのがバグの本質
- `updateSectionAccessoryAndSubmit` は素の `submitList` のみ (KsSettingsView.kt:681)。`replaceSection` の Full 経路 (`setRootDirect`) も最後は素の `submitList` (KsSettingsView.kt:414) のため、両経路とも header は再 bind されない
- さらに `replaceSection` / `SettingsRootDiff.Full` で**同一 id の Cell の内容**が変わるケースも同じ理屈で取りこぼす (発見時は未起票の潜在バグ。本 change のスコープに含めることで確定)
- concept「表示状態同期」(kasane/concepts/core/architecture/display-state-synchronization.md) の禁止事項は「値等価を**構造 identity** (`areItemsTheSame` / stable ID) に使わない」であり、変更通知の判定 (`areContentsTheSame`) までは縛っていない
- iOS の `updateAccessory` は表示へ反映されるため **OS 非対称** (iOS の取りこぼしは replaceSection 側 → [fix-replace-section-header-refresh](../fix-replace-section-header-refresh/exploration.md) 参照。原因は別)
- add-maui-native-bridge の Bridge 契約テストでは、誤った挙動を固定しないよう footer の追加/解除・header 同一の replaceSection でケースを構成している

## 検討した選択肢 (却下案と理由を含む)

### Section H/F の内容反映

| 案 | 内容 | 評価 |
|---|---|---|
| A: 呼び出し側で明示 notify | `updateSectionAccessoryAndSubmit` と ReplaceSection 経路に Section 版 `submitContentUpdate` を追加 | 却下。経路ごとの発行漏れ (同型バグ) が将来再発し得る |
| **B: DiffCallback で Section H/F のみ内容比較** | `areContentsTheSame` で SectionHeader / SectionFooter は data 等価比較、`getChangePayload` で `PAYLOAD_CONTENT` を返し payload 付き rebind に落とす。CellRow は常時 true を維持 | **採用**。submitList を通る全経路を1箇所で自動カバー。ちらつき対策の既存機構にそのまま乗る |

CellRow へ内容比較を広げない理由: Cell はリスナー等を持ち得て等価比較が安定しない (毎回 false → 全行 rebind 化)。

### Full 経路の Cell 内容反映

| 案 | 内容 | 評価 |
|---|---|---|
| C-1: ReplaceSection 分岐のみ `submitContentUpdate` に切替 | 局所修正 | 却下。`SettingsRootDiff.Full` の同一 id Cell 内容変更が取りこぼされたまま残る |
| **C-2: `setRootDirect` の素の `submitList` を `submitContentUpdate(newList, 新 root の全 cell id)` に置換** | full 更新を「構造 (DiffUtil) + 内容 (commit 後の一括 rebind)」で完結させる | **採用**。ReplaceSection / Full / 可視性フォールバックが1箇所で同時に治る。commit 後に currentList に実在する id のみ notify されるため削除・hidden は自然にスキップ |

案C-2 の注意点: 初回 setRoot では「挿入直後 + notify」の二重 bind が起きるため、旧リストが空なら素の `submitList` に落とすガードを入れる。

## 決定事項

- **案B + 案C-2 の組で修正する** (ユーザー確定 2026-08-05)。Section H/F はどの経路でも DiffCallback が拾い、Cell 内容は full 更新でも `setRootDirect` が拾う
- Cell 内容の取りこぼし (replaceSection / Full) も本 change のスコープに含める
- 修正時のテスト: 「header text を変える updateAccessory / replaceSection」「Cell 内容を変える replaceSection / Full diff」が表示へ反映されることを固定する (iOS 側の修正と合わせて OS 対称性を回復する)
- concept「表示状態同期」の Android 欄と KsSettingsListAdapter のコメント (「areContentsTheSame は常に true」) の文言更新が必要

## ADR 候補

- 作成済み: [android/ADR-0012](../../decisions/android/0012-full-update-content-sync-diffcallback-and-setrootdirect.md) (status: accepted、ユーザー承認 2026-08-05) — 本決定の全体 (案B + 案C-2、却下案含む)

## 未決の論点

- `PAYLOAD_CONTENT` 定数の置き場所: 案Bで DiffCallback からも参照するようになる。[fix-root-accessory-payload-notify](../fix-root-accessory-payload-notify/exploration.md) の小論点「payload 定数の集約」と合わせて実装時に判断
- 初回 setRoot の二重 bind ガードの具体形 (旧リスト空判定の置き場所)

## 関連 change

- [fix-root-accessory-payload-notify](../fix-root-accessory-payload-notify/exploration.md) — 同じ「内容更新は payload 付き通知を正とする」(android/ADR-0001) パターンの横展開。本 change の知見 (payload 定数の共有) が活きる見込み
- [fix-replace-section-header-refresh](../fix-replace-section-header-refresh/exploration.md) — iOS 側の対応バグ (原因は別: diffable data source の supplementary view)

## UI 素材

なし (見た目の新規変更はなし。既存表示の更新反映バグ)

## 変更級の推奨: M (確定)

当初 S 推奨だったが、案C-2 が `setRootDirect` というコア経路 (setRoot / Full / ReplaceSection / 可視性フォールバックの共通出口) に触るため回帰確認の重みが増すこと、DiffCallback の契約変更 (コメント・concept 文書の追随含む) を伴うことから、「迷ったら1段上」で **M 級に確定** (ユーザー承認 2026-08-05)。

## 関連ファイル

- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsListAdapter.kt` (DiffCallback / submitContentUpdate / PAYLOAD_CONTENT)
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt` (setRootDirect: 403 / applyDiff ReplaceSection: 459 / updateSectionAccessoryAndSubmit: 658)
- `kasane/concepts/core/architecture/display-state-synchronization.md` (追随更新が必要)
