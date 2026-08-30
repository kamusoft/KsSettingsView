# セカンドオピニオン: align-view-accessory-header-height (code-001)
**相方**: codex / **日付**: 2026-08-11 / **対象**: 作業ツリー未コミット変更 (Android 実装+テスト / iOS テスト)
---
# レビュー結果

**判定: APPROVED**

Critical 0 / Major 0 / Minor 1 / Suggestion 0

## 指摘事項

### [🟡 Minor] `PAYLOAD_CONTENT` の KDoc が実装と矛盾している

**該当箇所**: [android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsListAdapter.kt:240](<android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsListAdapter.kt:240>)

**問題点**: 「3 引数版 `onBindViewHolder` は実装していない」と記載されていますが、同ファイルの204行目で実装されています。実際には、高さ専用 payload 以外を `super` に委譲し、最終的に2引数版の full bindへ流しています。将来の保守時に payload 処理を誤解させます。

**推奨修正**: 「3引数版は高さ専用 payload のみ個別処理し、それ以外は `super` 経由で2引数版へ委譲する」という現在の実装に合わせて KDoc を更新してください。

## サマリー

Android の固定高さ解決、ViewHolder 再利用時の `WRAP_CONTENT` 復帰、高さ専用 payload による hosted view 維持はデルタスペックを満たしています。動的変更、Footer 非適用、Text 回帰、iOS 対称性までテストされ、タスクの完了チェックにも虚偽は見当たりません。

提示された Android 2214件・iOS 457件の全成功結果、および3枚の Android 視覚証跡も確認しました。上記はコメントのみの軽微な問題であり、動作上の変更要求はありません。指定に従いレビュー結果ファイルは作成していません。



## 突き合わせ結果 (ksn-orchestrator, 2026-08-11)

| 指摘 | 出典 | 採否 |
|---|---|---|
| [Major] 固定高さ時に hosted view が領域を埋めず iOS と非対称 (SectionAccessoryViewHolders.kt:394-401) | ホストのみ | **採用** (相方は見逃し。画素解析による実証あり → NEEDS_DISCUSSION としてオーナー裁定へ) |
| [Minor] PAYLOAD_CONTENT の KDoc「3 引数版は未実装」が実装と矛盾 (KsSettingsListAdapter.kt:240-241) | 双方一致 | **確定** (Minor) |
| [Suggestion] applySectionHeaderHeight の internal 可視性 | ホストのみ | 確定 (Suggestion のまま) |
| [Suggestion] layoutParams == null 分岐が到達不能 | ホストのみ | 確定 (Suggestion のまま) |
| [Suggestion] rebound スクリーンショットが states と byte 同一 | ホストのみ | 確定 (Suggestion のまま) |

- 相方判定 APPROVED / ホスト判定 NEEDS_DISCUSSION → lessons/process.md L-002 のとおり相方 APPROVED を「問題なし」の証明とは扱わず、ホスト側 Major を基準に判定処理する
