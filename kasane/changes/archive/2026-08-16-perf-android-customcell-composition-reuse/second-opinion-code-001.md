# セカンドオピニオン: perf-android-customcell-composition-reuse (code-001)

**相方**: codex / **日付**: 2026-08-16 / **対象**: 作業ツリー未コミット diff (ComposeCellViewHolder / CustomCellViewHolder / テスト新設 5 + 追随 1)

---

## サマリー

仕様と実装の対応に Critical / Major はありません。`ReusableContentHost`、Cell ID による状態隔離、pool-aware な破棄戦略、Bridge の再親付けはデルタスペックに沿っています。提示済みテスト結果は 1171 件 × debug/release、失敗・エラー 0 として確認しました。

## 指摘事項

### [🟡 Minor] GC テストが ViewHolder の builder 参照切断を検出できない

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/CustomCellBuilderReleaseTest.kt:64`

**問題点**: `recycledViewPool.clear()` 後に ViewHolder への強参照が残りません。そのため、`reset()` から `contentState.value = EMPTY_CELL_CONTENT` を削除しても、ViewHolder 全体が回収可能になりテストは成功し得ます。「ViewHolder が content state 経由で builder を保持しない」という要求の回帰検出力がありません。

**推奨修正**: 対象 ViewHolder をテスト終了まで強参照し、Composition 破棄後も holder 自体は生存した状態で参照対象が回収されることを確認してください。JIT の生存期間短縮を避けるため、判定後に `Reference.reachabilityFence(holder)` を置く方法が適切です。あわせて builder 切断行を除くミューテーションで失敗を確認してください。

### [🟡 Minor] FrameDriver が未収束を黙って受け入れる

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ComposeFrameDriver.kt:62`、`android/ks-settingsview-bridge/src/test/kotlin/jp/kamusoft/kssettingsview/bridge/ComposeFrameDriver.kt:62`

**問題点**: `MAX_FRAMES` 到達時も正常終了するため、recomposition が収束していない状態で後続アサーションへ進みます。特に「dispose されていない」「古い表示がない」といった否定形アサーションが、単なる未反映で成功する可能性があります。

**推奨修正**: ループ終了後に `check(!recomposer.hasPendingWork)` などで未収束を明示的に失敗させてください。

## 件数

- Critical: 0
- Major: 0
- Minor: 2
- Suggestion: 0

なお、未実施の実機高速フリック検証は今回の静的レビュー判定外ですが、実行時挙動検証規約上、change の完了・蒸留前には証跡が必要です。

**判定: APPROVED**

(session: <session-id> / raw: ~/.kasane/counterpart-bridge/responses/so-code-perf-android-customcell-composition-reuse-1.md)

---

## 突き合わせ結果 (vs review-001.md)

| 相方の指摘 | 採否 | 根拠 |
|---|---|---|
| GC テストが ViewHolder の builder 参照切断を検出できない (Minor) | **採用** | 相方のみ + 根拠強 (holder 無参照で判定が素通りする機構を特定、ミューテーションでの実害シナリオあり)。ホスト側の見逃しとして修正サイクルへ |
| FrameDriver が未収束を黙って受け入れる (Minor) | **確定** | ホスト側 review-001.md の Suggestion「frame() の打ち切り握り潰し」と一致。重要度は高い方 (Minor) を採る。修正サイクルへ |

- 判定の突き合わせ: 相方 APPROVED / ホスト CHANGES_REQUESTED。ホストの Major (実機検証 3.1 未実施) は相方も判定外の付記で「蒸留前に証跡必要」と同方向の言及をしており矛盾なし。修正サイクルはホスト判定に従う
- 採用 2 / 降格 0 / 未解決 0

