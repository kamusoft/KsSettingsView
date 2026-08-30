# 一致検証: fix-android-accessory-header-refresh (001 回目)

**日付**: 2026-08-05
**判定**: VALID

デルタスペック `specs/settings-view-android-ui/spec.md` の ADDED Requirement 3 件・normative bullet 6 件・Scenario 16 件をすべて突き合わせた。❌ (未記録の欠落・乖離) は 0 件。tasks.md の虚偽チェックなし、足場の逆流なし、テスト全件成功。

MODIFIED / REMOVED Requirement は存在しない (ADDED のみ) ため、旧挙動の残骸検査は既存テストの契約更新分のみを対象とした。

---

## 対応表

### Requirement: Section accessory の内容更新の表示反映

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| bullet 1: view type 不変時は行 identity を維持した payload 付き変更通知で反映し、構造変更通知としない。View accessory 同士は保持 View インスタンスの差し替えを内容変更とみなす | `KsSettingsListAdapter.kt:284-304` (areContentsTheSame) / `:319-327` (getChangePayload) / `:339-350` (isSameAccessoryContent — Text は値等価、View は `===` 参照比較) | `ListAdapterDiffTest.kt:83` `:121` `:143` `:162` / `FullUpdateContentSyncTest.kt:93` `:124` `:226` | ✅ 一致 |
| bullet 2: 型切替 (Text ↔ View) は行の stable identity を維持したまま表示を新 accessory にする。ViewHolder 差し替えは許容 | `KsSettingsListAdapter.kt:133-` (getItemId は `sectionId` + 役割のみで accessory 型に非依存) / `:119-131` (getItemViewType が accessory 型で分岐) | `ListAdapterDiffTest.kt:184` / `FullUpdateContentSyncTest.kt:191` (view type が `VIEW_TYPE_SECTION_HEADER_VIEW` へ切替、changed 通知は position 0 のみ) | ✅ 一致 |
| bullet 3: accessory 内容が変わらない場合は当該行へ変更通知を発行しない | `KsSettingsListAdapter.kt:293-300` (areContentsTheSame が true) | `ListAdapterDiffTest.kt:104` / `FullUpdateContentSyncTest.kt:261` | ✅ 一致 |
| Scenario: updateAccessory による header text 変更が表示へ反映される | 同上 + `KsSettingsView.kt:695` (updateSectionAccessoryAndSubmit → submitList) | `FullUpdateContentSyncTest.kt:93` | ✅ 一致 |
| Scenario: updateAccessory による footer text 変更が表示へ反映される | 同上 | `FullUpdateContentSyncTest.kt:124` | ✅ 一致 |
| Scenario: replaceSection による header text 変更が表示へ反映される | 同上 + `KsSettingsView.kt:473-488` (ReplaceSection → setRootDirect) | `FullUpdateContentSyncTest.kt:155` | ✅ 一致 |
| Scenario: accessory の型の切替も表示へ反映される | bullet 2 と同じ | `FullUpdateContentSyncTest.kt:191` | ✅ 一致 |
| Scenario: View accessory の差し替えが表示へ反映される | `KsSettingsListAdapter.kt:345-347` (`oldAccessory.view === newAccessory.view`) | `FullUpdateContentSyncTest.kt:226` | ✅ 一致 |
| Scenario: 内容が同一なら Section H/F へ変更通知を発行しない | bullet 3 と同じ | `FullUpdateContentSyncTest.kt:261` | ✅ 一致 |

### Requirement: Section accessory の追加と削除は構造変更として反映する

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| null → 非 null は挿入、非 null → null は削除として反映する (内容変更として扱わない) | `KsSettingsView.kt:914-` (flatten — accessory が null の Section は H/F 行そのものを生成しない) + `:695` (submitList → DiffUtil が挿入・削除として検出) | `FullUpdateContentSyncTest.kt:297` `:319` | ✅ 一致 |
| Scenario: header の追加が行の挿入として反映される | 同上 | `FullUpdateContentSyncTest.kt:297` (`recorder.inserted` 非空) | ✅ 一致 |
| Scenario: footer の解除が行の削除として反映される | 同上 | `FullUpdateContentSyncTest.kt:319` (`recorder.removed` 非空) | ✅ 一致 |

### Requirement: full 更新経路での同一 id の Cell 内容反映

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 本文: 旧新双方に存在する同一 id の Cell の内容変化を payload 付き変更通知で反映する | `KsSettingsView.kt:425-428` (setRootDirect が提出前の `currentList` から算出) / `:961-` (contentChangedCellIds) / `KsSettingsListAdapter.kt:93-117` (submitFullUpdate → submitListAndNotifyContent) | `FullUpdateContentSyncTest.kt:348` `:372` `:399` `:640` | ⚠️ deviation 記録済み |
| bullet 1: 新規挿入・hidden 復帰の Cell は構造変更として表示し、内容通知を重ねない | `KsSettingsView.kt:966-972` (旧リストに存在しない id は `?: continue` で除外) | `FullUpdateContentSyncTest.kt:478` `:547` | ✅ 一致 |
| bullet 2: 削除・hidden 化の Cell へは内容通知を発行しない | `KsSettingsView.kt:969-` (新リストのみを走査するため構造的に除外) | `FullUpdateContentSyncTest.kt:514` | ✅ 一致 |
| bullet 3: 内容通知対象が空でも構造の反映は必ず実行する | `KsSettingsListAdapter.kt:93-95` (submitFullUpdate に早期 return なし) / `:107-117` (submitList を先に発行し、空判定は commit callback の内側) | `FullUpdateContentSyncTest.kt:422` `:442` `:614` | ✅ 一致 |
| Scenario: replaceSection で同一 id の Cell 内容変更が表示へ反映される | 上記本文 + `KsSettingsView.kt:473-488` | `FullUpdateContentSyncTest.kt:348` | ✅ 一致 |
| Scenario: Full diff で同一 id の Cell 内容変更が表示へ反映される | 上記本文 + `KsSettingsView.kt:440-442` | `FullUpdateContentSyncTest.kt:372` | ✅ 一致 |
| Scenario: root の再設定でも同一 id の Cell 内容変更が反映される | 上記本文 (setRootDirect 直接呼び出し) | `FullUpdateContentSyncTest.kt:399` | ✅ 一致 |
| Scenario: 空 root への更新で表示が空になる | bullet 3 と同じ | `FullUpdateContentSyncTest.kt:422` (itemCount 2 → 0) | ✅ 一致 |
| Scenario: Section header / footer のみの root への更新が反映される | bullet 3 と同じ | `FullUpdateContentSyncTest.kt:442` (CellRow 残存なし・footer 表示) | ✅ 一致 |
| Scenario: 新規に挿入される Cell へは内容通知を重ねない | bullet 1 と同じ | `FullUpdateContentSyncTest.kt:478` (changed は position 0 のみ) | ✅ 一致 |
| Scenario: 削除された Cell へは内容通知を発行しない | bullet 2 と同じ | `FullUpdateContentSyncTest.kt:514` | ✅ 一致 |
| Scenario: 初回の root 反映では内容変更通知を発行しない | `KsSettingsView.kt:965` (`oldList.isEmpty()` で空返し) | `FullUpdateContentSyncTest.kt:614` (inserted のみ・changed 空) | ✅ 一致 |

#### ⚠️ の内訳 (deviation.md 記録済み)

Requirement 本文の「内容が変わった場合、その内容を反映する」に対し、実装は ADR-0012 Decision 2 の「旧∩新 visible の cell id だけへ発行」を**通知対象の上限**と解釈し、さらに「Cell の値 (`equals`) が変化したもの」へ絞っている (`KsSettingsView.kt:970`)。

- Requirement の SHALL (「内容が変わった場合に反映」「新規・削除へ通知しない」) には適合しており、**過少通知は生じない**
- 例外は `CustomCell` の `builder` / `onTap` だけを差し替えた場合で、これは core/ADR-0014 の既存の利用者契約どおり (本実装で新たに開いた穴ではない)
- `deviation.md` に理由 (破壊的変更の回避 / 既存契約 2 件の維持) とユーザー承認 (2026-08-05) が記録済み。ADR-0012 にも解釈の注記が追記されている
- 当該挙動は `FullUpdateContentSyncTest.kt:591` (`内容が変わらない Cell へは内容通知を発行しない`) と `:640` (contentChangedCellIds 直接テスト) で固定されている

合意済み差分として扱い、乖離としてはカウントしない。

---

## 追加検査

### tasks.md の突き合わせ

14 タスク中 14 件がチェック済み、未チェック 0 件。虚偽チェックなし。

| タスク | 実体 |
|---|---|
| 1.1 areContentsTheSame の Section 限定内容比較 | `KsSettingsListAdapter.kt:284-304` `:339-350` (Text 値等価 / View 参照比較 / CellRow は常時 true / core の `View.equals` 未変更) |
| 1.2 getChangePayload で PAYLOAD_CONTENT | `KsSettingsListAdapter.kt:319-327` (payload 定数は既存の `KsSettingsListAdapter.PAYLOAD_CONTENT` を参照。置き場所の移動なし) |
| 1.3 「areContentsTheSame は常に true」前提のコメント更新 | `KsSettingsListAdapter.kt:57-58` (submitContentUpdate) `:251-260` (DiffCallback クラス doc) `:286-295` (分岐内) |
| 2.1 構造提出と内容通知の分離 | `KsSettingsView.kt:425-428` + `KsSettingsListAdapter.kt:93-117` |
| 2.2 空 root / H/F のみ root / 初回 setRoot の確認 | `FullUpdateContentSyncTest.kt:422` `:442` `:614` |
| 3.1〜3.7 Scenario 対応テスト | 対応表のとおり全 Scenario にテストが存在 |
| 3.8 既存テストの契約更新 | `ListAdapterDiffTest.kt:83`(旧 `SectionHeader Text の内容差分は areContents に反映されない` を新契約へ書き換え) `:45`(CellRow は常時 true + payload null を追加固定) |
| 3.9 既存回帰の実行 | 1964 tests / 0 failures |

### 逆流検査 (足場アーティファクトの実装中書き換え)

| ファイル | mtime | 判定 |
|---|---|---|
| `specs/settings-view-android-ui/spec.md` | 11:19:35 | 実装開始 (11:28) 前。書き換えなし |
| `proposal.md` | 11:19:47 | 同上 |
| `tasks.md` | 11:42:08 | 進捗チェックのみ (足場の内容変更ではない) |
| `deviation.md` | 12:02:14 | 実装後の乖離記録 — 正しい運用 |
| 実装ファイル群 | 11:28〜12:16 | — |

逆流なし。レビュー修正ラウンド (12:15〜12:16) でも spec / proposal は無変更。

### 旧挙動の残骸

`areContents に反映されない` の全文検索結果 0 件。新契約と矛盾するテストの残存なし。

### テスト実行

`cd android && ./gradlew test --rerun-tasks` → BUILD SUCCESSFUL / **1964 tests / 0 failures / 0 errors / 0 skipped** (各モジュールの `build/test-results/*/TEST-*.xml` の `tests` / `failures` 属性を集計。`cross/conventions/test-execution.md` の件数確認規約に従う)。

### UI アーティファクト

本 change は `ui/` を持たない。表示の同期経路の修正であり見た目の仕様変更を伴わないため、モック承認ゲートの対象外。brief.md の不在は正しい。

---

## 未記録乖離

なし。

## 判定理由

全 Requirement / Scenario が「✅ 一致」または「⚠️ deviation 記録済み」。tasks.md に虚偽チェックなし、足場の逆流なし、テスト全件成功のため **VALID**。

なお実装スコープ外として送られた既存挙動 (`Section.headerHeight` のみを変える full 更新が再 bind されない) は、本デルタスペックの Requirement が accessory 内容に限定されているためこの検証の対象外。`kasane/changes/fix-android-header-height-refresh/` に follow-up として起票済み。
