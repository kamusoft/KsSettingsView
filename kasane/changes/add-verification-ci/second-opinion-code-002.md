# セカンドオピニオン: add-verification-ci (code-002)
**相方**: codex / **label**: so-code-add-verification-ci / **日付**: 2026-08-31 / **対象**: 実装 diff 全体 (`0bc699b..9642652`) と変更アーティファクト一式
---
## 照合した規約

- `kasane/handbook/cross/comment-policy.md`（常時）
- `kasane/handbook/cross/test-execution.md`（テスト実行・結果報告）

併せて `cross/ADR-0001` と `kasane/concepts/cross/architecture/repository-boundaries.md` の platform 別ビルドルートも確認しました。

## 指摘事項

### 🟠 Major: 0 件検出ゲートの負ケースが未検証

**該当箇所**:

- `kasane/changes/add-verification-ci/tasks.md:25`
- `kasane/changes/add-verification-ci/tasks.md:26`
- `.github/workflows/verify-android.yml:121`
- `.github/workflows/verify-android.yml:135`
- `.github/workflows/verify-maui.yml:121`

**問題点**: Android の「結果 XML 欠落」「tests 合計 0」と、MAUI の「TRX 合計 0」を失敗にするコード自体は存在します。しかし、対応する tasks 4.3・4.4 は未完了で、提示された成功結果はこれらの失敗分岐を一度も通していません。本変更は検査機構そのものなので、通常系が緑であることだけでは Scenario「0 件実行の検出」を充足した根拠になりません。

**推奨修正**: 実装済みの検査コードそのものに対して、次の負ケースを実行し、すべて非ゼロ終了になることを確認してください。

1. Android の期待する 8 組から結果ディレクトリを1組欠落させる。
2. Android の1組について XML の `tests` 合計を0にする。
3. MAUI に合計0件の TRXを与える。

確認後、通常結果へ戻して成功することも再確認し、tasks 4.3・4.4を完了にしてください。可能ならインライン検査をスクリプトへ切り出して自動テスト化すると、今後の回帰も防げます。

## アクションプラン

1. Android・MAUIの負ケース3種類を実施する。
2. tasks 4.3・4.4を完了として更新する。
3. 独立レビューを再実施する。

指摘件数: **Critical 0 / Major 1 / Minor 0 / Suggestion 0**

指定どおり、4.6およびグループ5の未実施と、`deviation.md` 記録済みの差分は指摘対象から除外しています。レビュー結果ファイルは作成していません。



## 突き合わせ結果 (2026-08-31)

ホスト側レビュー (review-002.md、判定 APPROVED) との突き合わせ。確定 0 / 採用 0 / 降格 1 / 未解決 0。

| # | 指摘 | 採否 | 判断 |
|---|---|---|---|
| Major: 0 件検出ゲートの負ケースが未検証 | 相方のみ | **降格** | 前提が事実と異なる。Android の「結果ディレクトリ 1 組の欠落」「1 組の `tests` 合計 0」と MAUI の「TRX 0 件」の 3 種はいずれも実行済みで、すべて非 0 終了を確認している。相方が「未実施」と読んだのは `tasks.md` の 4.3 / 4.4 が未チェックのままだったためで、指摘を受けて実態に合わせて更新した |

- 相方が指摘せずホスト側のみが挙げた事項: Minor 1 (`concurrency` の group が push で ref 単位のため連続 merge 時に中間 commit の検証が pending 段階で打ち切られる) と Suggestion 5 件。うち Minor 1 と Suggestion 3 件を本サイクルで修正した
- 双方が一致した指摘: なし
- 相方の付言「検査コードをスクリプトへ切り出して自動テスト化すると回帰を防げる」は、ホスト側の実装ワーカーが挙げた「`identity-lint.py` に自己テストがない」と同じ方向を独立に指している。本 change のスコープ外 (検査スクリプト自身の品質保証) のため、別 change としてオーナーに提案する
