# セカンドオピニオン: fix-android-accessory-header-refresh (001 回目)
**相方**: codex / **日付**: 2026-08-05 / **対象**: 提案一式 (proposal.md / specs/settings-view-android-ui/spec.md / tasks.md)
---
## 判定: CHANGES_REQUESTED

Critical 1 / Major 5 / Minor 2 / Suggestion 1。特に `setRootDirect` の計画は、仕様どおり実装すると正当な空 root 更新を取りこぼします。ADR-0012 自体にも同じ問題があるため、実装前に補正判断が必要です。

### Critical — Cell が 0 件の root では `submitList` 自体が呼ばれない

**該当箇所**: `proposal.md:12`、`tasks.md:11-12`、`KsSettingsListAdapter.kt:64-65`、`Section.kt:6-7`

**問題点**: `setRootDirect` から「新 root の全 cell id」を渡す計画ですが、既存の `submitContentUpdate` は `cellIds.isEmpty()` なら `submitList(newList)` より前に return します。したがって、既存表示から以下の正当な root へ更新すると、Adapter は古い表示のままです。

- 空 root
- Cell がなく Section header/footer だけの root
- Cell をすべて削除した root

「旧リストが空なら」という初回ガードでは、新 root 側の Cell が 0 件であるケースを救えません。空 Section は現行モデルで許容されています。

**推奨修正**: 内容通知と list 提出を分離し、対象 ID が空でも `submitList` は必ず実行する設計にしてください。少なくとも「非空 root → 空 root」「Cell あり → header/footer のみ」の Scenario とタスクを追加してください。ADR-0012:25 にも同じ方針が記録されているため、提案だけでなく補正 ADR で扱う必要があります。

### Major — View accessory 同士の内容変更を検出できない

**該当箇所**: `spec.md:5-7`、`proposal.md:11`、`tasks.md:5`、`SectionAccessory.kt:31-46`

**問題点**: Requirement は Section accessory の「内容が変わる更新」全般を保証していますが、採用案の data 等価比較では `SectionAccessory.View` 同士の変更を検出できません。現行の `View.equals` は内部の `KsAnyView` を無視し、すべての `View` インスタンスを同値とします。

そのため Compose 内容や Android View factory を別内容へ差し替えても `areContentsTheSame == true` となり、通知も再 bind も起きません。現在の型切替 Scenario（Text → View）はこの穴を通りません。

**推奨修正**: 次のいずれかを仕様として確定してください。

- View accessory に描画 revision/identity を導入して変更検出する
- View → View 更新を明示通知する専用経路を設ける
- View → View 更新を保証対象外とするなら、Requirement を Text 更新等へ明確に限定する

あわせて Compose → Compose、AndroidView → AndroidView の異なる内容への変更 Scenario を追加してください。

### Major — 新規・再表示 Cell に挿入通知と内容通知が二重発行される

**該当箇所**: `proposal.md:12`、`tasks.md:11`、`ADR-0012:25`、`display-state-synchronization.md:17-21`、`KsSettingsListAdapter.kt:75-80`

**問題点**: 新 root の全 Cell ID を通知対象にすると、同一 ID で残った Cell だけでなく、新規挿入または hidden から復帰した Cell にも commit 後の `notifyItemChanged` が発行されます。これらは DiffUtil による挿入 bind と内容 rebind の二重処理になります。

これは概念文書の「構造」と「同じ ID の内容」を分ける契約、および Requirement の「同一 id の内容変更」という対象範囲と一致しません。初回 root だけをガードしても、後続の Full/ReplaceSection/可視性フォールバックで発生します。

**推奨修正**: 通知対象を原則として「旧 visible Cell ID ∩ 新 visible Cell ID」に限定し、新規・再表示 Cell は構造通知だけで bind してください。「新規 Cell には内容通知を発行しない」「hidden → visible は挿入として扱う」Scenario を追加してください。この点も accepted ADR-0012 の補正対象です。

### Major — Text ↔ View 切替では「同一 ViewHolder」の保証が成立しない

**該当箇所**: `spec.md:24-27`、`ADR-0012:24`、`KsSettingsListAdapter.kt:95-104,132-148`

**問題点**: ADR-0012 は payload rebind を「同一 ViewHolder への再 bind」と説明していますが、Text accessory と View accessory は異なる view type・ViewHolder クラスです。同じ ViewHolder のまま bind すればキャストが成立せず、RecyclerView が型変更を認識した場合は ViewHolder が交換されます。

Scenario は型切替を要求する一方、どの lifecycle を正とするかが proposal/ADR と整合していません。

**推奨修正**: 型切替は同一 ViewHolder 保証の例外とし、次のどちらを契約とするか決めてください。

- 行の stable identity と change 通知は維持するが、ViewHolder 交換を許可する
- view type の変更として構造的な差し替えを許可する

同一 ViewHolder 保証は Text → Text、View → View など view type 不変時に限定してください。

### Major — accessory の追加・削除と「構造通知を使わない」が矛盾する

**該当箇所**: Requirement「Section accessory の内容更新の表示反映」`spec.md:7`、`KsSettingsView.kt:670-681,905-923`

**問題点**: `updateAccessory` は nullable で、header/footer の追加・削除も可能です。しかし Requirement は同 API の更新を「構造変更通知として扱わない」と一括して規定しています。`null → accessory` と `accessory → null` では平坦リスト上の行そのものが挿入・削除されるため、この契約は実現できません。

**推奨修正**: Requirement を次の二種類に分け、header/footer それぞれに Scenario を追加してください。

- 非 null → 非 null: 同一行の内容変更
- null ↔ 非 null: 構造上の挿入・削除

### Major — 現行テストが新契約と直接衝突する

**該当箇所**: `tasks.md:21`、`ListAdapterDiffTest.kt:72-87`

**問題点**: 既存テストは、同一 section ID の header text が異なっても `areContentsTheSame == true` と明示的に固定しています。タスクは「既存回帰の実行」しか記載しておらず、このテストを新契約へ更新する作業がありません。提案どおり実装するとテストは必ず失敗します。

**推奨修正**: タスクへ既存テスト・コメントの契約更新を明記してください。少なくとも以下を分けて固定すべきです。

- CellRow は引き続き常時 true
- Section Text の同一内容は true、異なる内容は false
- Section の内容差では `PAYLOAD_CONTENT`
- View accessory の等価性は上記の設計判断に従う

### Minor — 「root 全体の再設定」に対応する Scenario がない

**該当箇所**: `spec.md:34-56`、`tasks.md:19`

**問題点**: Requirement は `replaceSection`、`SettingsRootDiff.Full`、root 全体の再設定を列挙していますが、Scenario とテストタスクは前二者だけです。既存 root に対する `setRoot` 再呼び出しの受け入れ基準がありません。

**推奨修正**: 同一 Cell ID の内容が異なる root を再設定し、最新内容と payload 通知を確認する Scenario を追加してください。

### Minor — 任意扱いの Bridge テストは完了条件にならない

**該当箇所**: `tasks.md:25`、`proposal.md:14`

**問題点**: 「確認し、可能なら追加する」ではタスク完了を客観的に判定できません。また proposal は影響能力を `settings-view-android-ui` のみとしていますが、タスクは `ks-settingsview-bridge` に及びます。

**推奨修正**: Bridge 契約テストの更新を必須タスクにするか、別 follow-up として明確に分離してください。実施する場合は proposal の Impact/影響能力にも Bridge を記載してください。

### Suggestion — 利用者向け挙動と内部通知契約を分ける

**該当箇所**: `spec.md:7,12,17,36,41`

**問題点**: 「表示が更新される」という外部挙動と、「payload 付き通知」という実装不変条件が同じ Requirement/THEN に混在しています。後者は ADR-0001 に基づく重要な内部契約ですが、仕様変更理由とテスト失敗時の意味が読み取りにくくなっています。

**推奨修正**: 表示反映 Requirement と、Adapter の通知・ViewHolder 再利用 Requirement を分離してください。型切替だけは後者の例外として明記すると、受け入れ基準が明確になります。

静的レビューのみ実施し、ビルド・テスト・ファイル更新は行っていません。


## 突き合わせ結果

ホスト側自己レビュー (2周・チェックリスト通過) との突き合わせ。全指摘を対象コードで検証してから採否を決めた。

| 指摘 | 検証 | 採否 |
|---|---|---|
| Critical: Cell 0 件 root で submitList が呼ばれない | KsSettingsListAdapter.kt:65 の early return を確認。実害シナリオ成立 | **採用** (ホスト側の見逃し)。構造提出と内容通知を分離し submitList は常に実行する設計へ補正。ADR-0012 の補正が必要 → ユーザー提示 |
| Major: View accessory 同士の変更を検出できない | SectionAccessory.kt:39-42 の「クラス一致のみ等価」(旧 openspec Decision 3、意図的) を確認 | **採用**。ただし対処方針 (参照比較で検出 / 保証対象外に限定) は設計判断 → ユーザー提示 |
| Major: 新規・再表示 Cell への二重通知 | submitContentUpdate は currentList 実在 id へ無条件 notify。新規挿入 id も対象になる | **採用**。通知対象を「旧 visible ∩ 新 visible」へ限定。初回ガードはこの規則に包含され不要になる。ADR-0012 の補正対象 |
| Major: Text ↔ View 切替で同一 ViewHolder 保証が不成立 | viewType が変わるため ViewHolder は交換される (RecyclerView 標準動作) | **採用**。同一 ViewHolder 保証は view type 不変時に限る例外を spec に明記 |
| Major: accessory の null ↔ 非 null は構造変更 | updateAccessory は nullable。null 遷移は行の挿入・削除 (flatten で行が消える/現れる) | **採用**。Requirement を「非 null → 非 null = 内容変更」「null ↔ 非 null = 構造変更」に分割 |
| Major: 既存テストが新契約と衝突 | ListAdapterDiffTest.kt:77-88 が「header text 差でも true」を明示固定 | **採用**。既存テスト・コメントの契約更新をタスクに追加 |
| Minor: 「root 全体の再設定」の Scenario 欠落 | 指摘どおり | **採用**。setRoot 再設定の Scenario を追加 |
| Minor: Bridge テストタスクが任意扱い | 指摘どおり (完了判定不能・能力範囲も proposal と不整合) | **採用**。tasks から外し follow-up として proposal に明記 |
| Suggestion: 表示挙動と通知契約の Requirement 分離 | 構成の好みの域 (内容は正しいが M 級の簡潔さを優先) | **降格** (記録のみ。修正サイクルは回さない) |

矛盾 (ホスト側と相方で割れた論点): なし。

追記 (2026-08-05): ユーザー判断の確定 — View accessory の内容変更検出は「DiffCallback での KsAnyView 参照比較」を採用 (core の View.equals は不変)。ADR-0012 本文の補正も承認され適用済み。全採用指摘は提案一式 (proposal / spec / tasks) へ反映完了。
