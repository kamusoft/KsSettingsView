# セカンドオピニオン: backport-release-workflow-hardening (code-002)

**相方**: codex / **label**: so-code-backport-release-workflow-hardening / **日付**: 2026-09-11

**対象**: 修正サイクル 1 後の作業ツリー (未コミット)。code-001 の指摘 3 件に対する修正の確認と、新たな欠陥の有無

---

# 追補レビュー結果

**判定**: **CHANGES_REQUESTED**  
**件数**: Critical 0 / Major 0 / Minor 1 / Suggestion 0

前回の3件はすべて閉じています。ただし、追加された期限計算の自己テストにCIを不安定化させる新しいMinorが1件あります。

## 前回指摘の確認

| 前回指摘 | 結果 | 根拠 |
|---|---|---|
| 60分のjob timeoutが先に発火する | CLOSED | `request_max_time` が期限未設定を区別し、残り時間に応じて120秒から最小1秒まで切り詰めています。巡回間隔を含めても既定45分からの超過は小さく、60分の外側上限より先に最終分類を出力できます。 |
| 不正なNuGet versions要素の誤分類 | CLOSED | `scripts/release/wait-for-registries.sh:224` で全要素が文字列か検証し、null・数値・objectの自己テストも追加されています。 |
| downloadとinspectの順序未検査 | CLOSED | `scripts/release/check-publish-step-order.py:113` で逆順を明示的に拒否し、実物成功・順序破壊・step欠落を含む自己テストがCIへ接続されています。 |

期限未設定の単発照会は `POLL_DEADLINE_SET=0` により通常の120秒になります。残り0秒・期限超過後は1秒へ制限され、`curl --max-time 0` の無制限動作も避けられています。

また、自己テストの明示的な5秒上限は、上限超過テストで指定する0秒に上書きされるため、検査を骨抜きにはしていません。モック台本の枯渇も別途検出されており、順序検査の正例・負例にも常に通るだけの空回りは確認できませんでした。

## 指摘事項

### [🟡 Minor] 残り7秒の自己テストが秒境界で不安定になる

**該当箇所**: `scripts/release/wait-for-registries.sh:516`、`.github/workflows/ci.yml:187`

**問題点**: `POLL_DEADLINE=$(( SECONDS + 7 ))` の設定後、別のコマンド置換内で `SECONDS` を再取得し、結果が厳密に`7`であることを要求しています。代入とコマンド置換の間で秒境界をまたぐと、正しい実装でも残り時間は`6`になり、lint jobが間欠的に失敗します。高負荷のrunnerほど発生しやすい競合です。

**推奨修正**: 切り詰め計算を「残り秒数を引数で受ける純粋関数」へ分け、固定入力`7`に対して`7`を検査してください。`request_max_time`は`deadline - SECONDS`を計算してその関数へ渡すだけにすれば、境界計算と切り詰め規則を決定的に検査できます。

## 確認結果

`bash -n`、Pythonソースのコンパイル、`git diff --check`は通過しました。自己テスト本体は、ファイル書き込みを行わないという条件に合わせて再実行していません。レビュー結果ファイルも変更していません。


