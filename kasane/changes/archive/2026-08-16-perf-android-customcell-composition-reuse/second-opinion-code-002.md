# セカンドオピニオン: perf-android-customcell-composition-reuse (code-002)

**相方**: codex / **日付**: 2026-08-16 / **対象**: 修正サイクル後の作業ツリー未コミット diff 全体 (クラッシュ修正 + レビュー指摘対応込み)

---

**判定**: APPROVED
**指摘件数**: Critical 0 / Major 0 / Minor 0 / Suggestion 1

## サマリー

Composition のプール生存、Cell 単位の状態隔離、reset 時の参照切断、非活性ノードの measure 回避、Bridge の platform view 保全は、デルタスペックおよび android/ADR-0015 と整合しています。提示された全件テスト・実機再検証・ミューテーション結果も踏まえ、承認を妨げる問題は見つかりませんでした。

## 指摘事項

### [🔵 Suggestion] 非活性期間の高さ確保を回帰テストで固定する

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/CustomCellPooledRebindMeasureTest.kt:105`

**問題点**: 同期 measure が例外なく完了することは検証されていますが、非活性分岐が新しい Cell の高さを確保することは検証されていません。実装が高さ 0 や古い Cell の高さを返しても、現在のテストは次フレーム後の表示だけを確認するため通過します。

**推奨修正**: A/B で異なる固定高を設定し、再 bind 直後・フレーム送出前の `measuredHeight` が B の高さになっていることを追加で検証してください。これは非ブロッキングのテスト強化です。

## アクションプラン

上記テスト強化は任意です。現状のままマージ可能と判断します。

(session: <session-id> / raw: ~/.kasane/counterpart-bridge/responses/so-code2-perf-android-customcell-composition-reuse-1.md)

---

## 突き合わせ結果 (vs review-002.md)

| 相方の指摘 | 採否 | 根拠 |
|---|---|---|
| 非活性期間の高さ確保を回帰テストで固定する (Suggestion) | **確定** | review-002.md の Minor (高さ確保が `isFixedHeight` を見ない穴 + KDoc 過強表現) と同じ穴を突いており実質一致。第3周の修正サイクルでテスト強化として適用 |

- 判定の突き合わせ: 相方 APPROVED / ホスト CHANGES_REQUESTED。ホストの Major 2 件はいずれも証跡・決定層の追随 (verification-mutation.md が修正前ビルドの記録 / measure policy と 1 フレーム遅延が ADR に未記録) であり、相方の静的レビュー範囲外。矛盾なし
- 確定 1 / 採用 0 / 降格 0 / 未解決 0

