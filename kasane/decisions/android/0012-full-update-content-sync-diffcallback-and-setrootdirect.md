---
id: 0012
title: submitList 経路の内容取りこぼしは DiffCallback の Section H/F 内容比較と setRootDirect の一括 rebind で解消する
status: accepted
date: 2026-08-05
---

## Context

Android で `updateAccessory(SectionHeader, 既知の sectionID, 別の text)` を呼ぶと、Store の model は更新されるが実描画の Section header が古いまま残る (Adapter 通知 0 件を実測。add-maui-native-bridge グループ4 実装中に発見)。

構図は次のとおり (2026-08-05 コード裏取り):

- `CellListItemDiffCallback.areContentsTheSame` の常時 true は「表示状態同期の三層分離」原則に基づく意図的な設計で、内容更新は `submitContentUpdate` → `notifyItemChanged(PAYLOAD_CONTENT)` の部分更新経路で反映する建付け。
- しかしその部分更新経路は `CellListItem.CellRow` しか対象にしておらず、**Section H/F だけが内容更新経路から漏れている**。
- `updateSectionAccessoryAndSubmit` も `replaceSection` の Full 経路 (`setRootDirect`) も最後は素の `submitList` に落ちるため、section identity が同じままの header/footer text 変更はどの経路でも再 bind されない。
- さらに `replaceSection` / `SettingsRootDiff.Full` で同一 id の Cell の内容が変わるケースも同じ理屈で取りこぼす (潜在バグとして同時発見)。
- concept「表示状態同期」の禁止事項は「値等価を構造 identity (`areItemsTheSame` / stable ID) に使わない」であり、変更通知の判定 (`areContentsTheSame`) までは縛っていない。

## Decision

submitList を通る全経路で内容の取りこぼしをなくすため、次の2点を採用する:

1. **Section H/F の内容検出は DiffCallback で行う**: `areContentsTheSame` を SectionHeader / SectionFooter に限り内容比較とし (Text accessory は data 等価、View accessory は `KsAnyView` の**参照比較** — core の `View.equals` (クラス一致のみ等価、旧 openspec Decision 3) は変更しない)、`getChangePayload` で `PAYLOAD_CONTENT` を返して payload 付き rebind (android/ADR-0001 の機構) に落とす。**CellRow の常時 true は維持する**。payload rebind による同一 ViewHolder 保証は **view type 不変時に限る** — Text ↔ View の型切替では行の stable identity を維持したまま ViewHolder の交換を許容する。
2. **full 更新は `setRootDirect` で内容 rebind まで完結させる**: 構造提出と内容通知を分離し、**`submitList` は内容通知対象が空でも必ず実行する** (空 root・Section H/F のみの root への更新を取りこぼさない)。内容通知は**旧 visible リストと新 visible リストの双方に存在する cell id** だけへ発行する。新規挿入・hidden 復帰の Cell は構造通知のみで bind し内容通知を重ねない。削除・hidden 化はもとより対象外で、初回 setRoot は通知対象が空になるため自然に素の構造反映となる。

## Alternatives Considered

- **呼び出し側で明示 notify (updateSectionAccessoryAndSubmit と ReplaceSection 経路に Section 版 submitContentUpdate を追加)**: 却下。既存の「明示発行」スタイルには一致するが、経路ごとに通知の発行漏れという同型バグが将来再発し得る。DiffCallback 側で拾えば submitList を通る全経路を1箇所で自動カバーできる。
- **CellRow へも内容比較を拡大**: 却下。Cell はリスナー等を持ち得て等価比較が安定せず (毎回 false)、全行 rebind 化して常時 true 設計の意図 (ちらつき回避) を壊す。
- **ReplaceSection 分岐のみ submitContentUpdate に切替 (setRootDirect は触らない)**: 却下。影響範囲は最小だが、`SettingsRootDiff.Full` で同一 id の Cell 内容が変わるケースの取りこぼしが残る。setRootDirect を直せば ReplaceSection / Full / 可視性フォールバックが1箇所で同時に治る。

## Consequences

- 正: header/footer text を変える `updateAccessory` / `replaceSection`、Cell 内容を変える `replaceSection` / `Full` diff がすべて表示へ反映される。iOS 側の修正 (fix-replace-section-header-refresh) と合わせて OS 対称性が回復する。
- 正: payload 付き rebind に落ちるため、ちらつき・IME 切断対策 (android/ADR-0001) と矛盾しない。
- 負: `setRootDirect` はコア経路のため回帰確認の重みが増す (本 change を M 級とした理由)。
- 追随: KsSettingsListAdapter のコメント (「areContentsTheSame は常に true」) と concept「表示状態同期」の Android 欄の文言更新が必要。
- 関連: DiffCallback が `PAYLOAD_CONTENT` を参照するようになるため、payload 定数の置き場所の集約 (fix-root-accessory-payload-notify の小論点) の重みが増す。

出典: fix-android-accessory-header-refresh の探索 (exploration.md 2026-08-05 / ユーザー確定 2026-08-05)
補正: 2026-08-05 — second-opinion-001 (codex spec-review) の Critical / Major 指摘に基づき Decision 1 (View accessory の参照比較・型切替時の ViewHolder 交換許容) と Decision 2 (submitList 常時実行・通知対象を旧∩新 visible に限定。初回ガードは本規則に包含され廃止) を補正 (ユーザー承認 2026-08-05)
注記: 2026-08-05 — Decision 2 の「旧∩新の cell id だけへ発行する」は**通知対象の上限**を定める句であり、実装は旧∩新のうち値 (equals) が変化した Cell のみへ通知してよい (変化していない Cell への通知は要求しない)。再 attach 時の差分通知ゼロ等の既存契約と両立させるための解釈で、本リポジトリの Cell は equals をクロージャ除外・表示フィールド全比較で手実装しているため値等価で内容変化を取りこぼさない (ユーザー承認 2026-08-05)
