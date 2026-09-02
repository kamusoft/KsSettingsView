# セカンドオピニオン: add-spm-distribution (code-002)
**相方**: codex / **label**: so-code-add-spm-distribution / **日付**: 2026-09-01 / **対象**: 修正サイクル 1 回目で触れた 6 ファイル (scripts/spm-snapshot/ 3 点、.github/workflows/ci.yml、ios/binding/build-xcframework.sh、kasane/handbook/cross/test-execution.md)
---
# 再レビュー結果: add-spm-distribution

**判定**: **APPROVED**

## 前回指摘の解消状況

| 前回指摘 | 判定 | 根拠 |
|---|---|---|
| Major: READMEが誘導専用ではない | 解消 | `scripts/spm-snapshot/README.template.md:10` はインストール手順をmonorepoへ誘導し、版番号・依存宣言を持たない |
| Minor: filesystem rootを拒否できない | 解消 | `scripts/spm-snapshot/sync-snapshot.sh:50` の共通判定が `/` を祖先として扱い、`:129` から実運用でも使用される。`:225` 以降に境界テストあり |
| Minor: Sources・Tests・LICENSEの内容未検証 | 解消 | `scripts/spm-snapshot/sync-snapshot-test.sh:127` 以降でLICENSE、Sources、Testsの内容とツリー構造を比較している |

41アサーションの成功に加え、検証4ガードとパス正規化に対するミューテーションで各2ケースが失敗することも確認済みであり、`kasane/lessons/code-review.md` の回帰検出力の観点を満たしています。

## 新規変更の確認

- `.github/workflows/ci.yml:108` のテスト追加は既存job名やplatform workflow構成を変えず、cross/ADR-0025のstatus check契約を維持しています。
- `ios/binding/build-xcframework.sh` の変更はコメントのみで、現在の静的リンク構成と整合しています。
- `kasane/handbook/cross/test-execution.md:46` は複数テストバンドルの件数報告を明確化しており、テスト結果報告規約を改善しています。
- 新たなCritical / Major / Minorはありません。

## 指摘事項

### [🔵 Suggestion] ハッシュコマンドの抽象化を1箇所だけ通っていない

**該当箇所**: `scripts/spm-snapshot/sync-snapshot-test.sh:258`

**問題点**: macOS/Linux向けに`${HASH_TOOL}`を選択していますが、index比較だけ`shasum`を直接使用しています。CIはUbuntu 24.04で実行されるため、選択済みコマンドへ統一すると環境依存を完全に閉じられます。

**推奨修正**: `shasum`を`"${HASH_TOOL}"`へ置き換えてください。現状の承認を妨げる問題ではありません。

指摘件数: Critical 0 / Major 0 / Minor 0 / Suggestion 1

指定どおり、ファイルへの書き込みとテストの再実行は行っていません。
