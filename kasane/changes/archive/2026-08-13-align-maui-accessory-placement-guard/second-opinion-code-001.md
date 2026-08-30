# セカンドオピニオン: align-maui-accessory-placement-guard (code-001)
**相方**: codex / **日付**: 2026-08-13 / **対象**: HEAD (3cb2f92) に対する working tree diff (maui/ 配下)
---
# レビュー結果

**日付**: 2026-08-13  
**判定**: **APPROVED**

## サマリー

仕様・tasks・合意済み deviation と実装差分を突き合わせました。値確定前ガード、弱参照による guard 配線、バッチ事前検査、失敗時の状態維持はいずれも整合しており、Critical / Major / Minor の指摘はありません。

提示済みの検証結果（413件成功、3 TFM ビルド成功、lint違反なし）を前提とし、再実行はしていません。

## 指摘事項

### [🔵 Suggestion] accessory と CustomCell.Content のバッチ内交差を直接テストする

**該当箇所**: [AccessoryViewTests.cs:575](maui/KsSettingsView.Maui.Tests/AccessoryViewTests.cs:575)、[KsSettingsController.cs:2103](maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:2103)

**問題点**: 実装は同一 `seen` 集合で Section accessory と `CustomCell.Content` の交差を正しく検査しています。一方、追加テストは accessory 同士のバッチ重複と、プロパティ差し替え時の Content 交差を扱っていますが、バッチ内の accessory–Content 交差を直接固定していません。

**推奨修正**: 同じ View を `Section.HeaderView` と配下 `CustomCell.Content` に持つ Section の一括追加テストを追加し、例外・gateway呼び出しなし・対応表未登録を確認すると、仕様の明示的な一節に対する回帰耐性が上がります。現実装の承認を妨げるものではありません。

## 件数

- Critical: 0
- Major: 0
- Minor: 0
- Suggestion: 1

**最終判定: APPROVED**


## 突き合わせ結果

ホスト側 review-001.md (APPROVED / Minor 2・Suggestion 2) との突き合わせ:

| 指摘 | 出典 | 採否 | 対応 |
|---|---|---|---|
| 実装の正しさ・状態保全・弱参照化の妥当性 | 双方一致 | **確定 (APPROVED)** | — |
| バッチ内 accessory–Content 交差の直接テスト | 相方のみ (Suggestion) | **採用** | `AddingASectionWhoseAccessoryAndCellContentShareAViewThrowsBeforeAnyInsert` を追加 |
| guard 解除 2 行の回帰検出力なし | ホストのみ (Minor) | 採用 | `ASectionRemovedFromTheRootStopsConsultingThePlacement` / `ASectionDroppedByARootRebuildStopsConsultingThePlacement` を追加 (解除 2 箇所を各々固定) |
| バッチ内数えあげが現状二重検査である旨のコメント欠如 | ホストのみ (Minor) | 採用 | `EnsureSectionsAreNotPlaced` のコメントに現在形で追記 |
| cref の非修飾統一 / 再収束テストの注記 | ホストのみ (Suggestion) | 採用 | 反映済み |

対応後テスト: 416 件 / 失敗 0 (レビュー時点 413 件 +3)。comment-policy lint 禁止 0 件。降格・未解決: なし
