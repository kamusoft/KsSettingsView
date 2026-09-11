# レビュー結果: backport-release-workflow-hardening (004 回目)

**日付**: 2026-09-11
**判定**: APPROVED

## サマリー

前回 (003 回目) 以降の変更は `scripts/release/wait-for-registries.sh` 1 本のみで、未照会 (`unprobed`) の分類を導入して「照会できたうえで未反映」と「一度も照会していない」を出力で分けたもの。実コード (偽 `curl` を PATH に置く方法) で成功・途中回復・上限超過・待機外の単発照会の 4 経路をなぞり、いずれも壊れていないことを確認した。追加された自己テストは変異を入れると実際に落ちる (空回りなし) ことを 4 種の変異で実測した。Critical / Major に相当する実害は見つからなかった。

これは最終確認のレビューであり、新規 Suggestion の掘り起こしは行っていない。review-003 と second-opinion-code-003 の Suggestion 群が未修正であることは判定の材料にしていない (オーナー報告事項として扱う方針を前提とした)。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 新旧コメントに作業文書のパス・変更識別子・通番・デルタスペック構文キーワードの混入がないことを確認。`comment-policy-lint.py` も 0 件
- `kasane/handbook/cross/test-execution.md` (適用のきっかけ: テスト実行・テスト結果の報告) — 実行件数を併記する要求に従い、以下に件数を明記する
- `kasane/lessons/code-review.md` の重点観点 [L-001] (ミューテーションでアサーションの検出力を実測する) を今回の主な手法として適用した

## 実行結果

lint job が回す検査を手元で全件実行し、すべて成功 (終了コード 0)。

| 検査 | 結果 |
|---|---|
| `scripts/release/wait-for-registries.sh --selftest` | 61 件 OK / 0 件 NG |
| `scripts/release/central-portal.sh --selftest` | 52 件 OK / 0 件 NG |
| `scripts/release/deployment-handover.sh --selftest` | 22 件 OK / 0 件 NG |
| `python3 scripts/release/set-readme-version.py --selftest` | 33 件 OK / 0 件 NG |
| `python3 scripts/release/check-time-budget.py` | 合計 217 分 / 上限 240 分 (余裕 23 分) |
| `python3 scripts/release/check-publish-step-order.py` | 順序正常 |
| `python3 scripts/release/check-publish-step-order.py --selftest` | 7 件 OK / 0 件 NG |
| `python3 scripts/comment-policy-lint.py` | 禁止 0 件 (検査対象 787 ファイル) |
| `python3 scripts/local-path-lint.py` / `identity-lint.py` / `readme-example-lint.py` | 検出なし |

待機スクリプトの自己テストは 5 回連続で 61 OK / 0 NG。時計に依存する検査 (`MOCK_DELAY` と `POLL_DEADLINE` の組み合わせ) が間欠的に揺れないことを確認した。

## 修正が目的を達しているか

`unprobed` は `reset_targets` の初期値としてだけ入り、`poll_once` (反映済みのみ読み飛ばす) / `state_label` (「未照会」) / `has_unreflected` (反映済み以外はすべて未完了) の 3 箇所で正しく扱われる。デルタスペック「公開レジストリへの反映待ち」の SHALL「照会できたうえで未反映であることと、照会そのものが行えなかったこと」の区別は、未反映 (`pending`) / 判定不能 3 種 / 未照会 (`unprobed`) の 4 系統として出力に現れる。

未照会を判定不能の 3 種別 (通信そのものの失敗 / 応答が成功を示さない / 応答を解釈できない) のいずれかへ寄せると、照会していない対象に失敗の種別が付いてしまい spec の種別区別と食い違う。独立した分類にした判断は spec に適合している。

実コードでの確認 (偽 `curl` を PATH に置き、期限を跨ぐ応答遅延を与える):

```
巡回 1 の分類:
  Maven Central jp.kamusoft:kssettingsview: 反映済み
  nuget.org/kssettingsview.maui: 未照会
  nuget.org/kssettingsview.binding.ios: 未照会
  nuget.org/kssettingsview.binding.android: 未照会
対象ごとの最後の分類:
  (同上)
::error::反映を待ちきれませんでした (上限 3 秒、巡回 1 回)
```

## 別経路を壊していないか

偽 `curl` を PATH に置いて実コードを走らせ、4 経路すべてをなぞった。

| 経路 | 与えた応答 | 結果 |
|---|---|---|
| 成功 (全件 1 巡目で反映済み) | 4 対象すべて 200 / version あり | 巡回 1 で 4 件反映済み → 終了コード 0 |
| 途中で反映 (2 巡目で全件) | 1 巡目に 404 / 503 / 通信失敗を混在、2 巡目は全件成功 | 巡回 2 で成功。反映済みになった対象 (maui) の照会回数は 1 回のまま = 再照会なし |
| 失敗 (全対象を照会したうえで上限超過) | 404 / version なし / 404 / 503 を反復 | 3 巡して終了コード 1。最後の分類に 4 対象すべてが出て、未照会は 1 件も混じらない |
| 待機外の単発照会 (`POLL_DEADLINE_SET=0`) | 200 / 200 / 404 / 通信失敗 | `remaining_seconds` が 1 を返し、`poll_once` が 4 対象すべてを照会。`has_unreflected` は未完了を返す |

待機外の単発照会は、`main` の末尾行を落として関数を source する形で実行した (本番の呼び出し元がまだ無いため。呼び出し元不在そのものは review-003 の Suggestion で既知)。初期値を `unprobed` にしたことが `POLL_DEADLINE_SET=0` の経路を止めないことを確認した狙い。

`has_unreflected` は反映済み以外をすべて未完了として扱うため、未照会のまま `return 0` (成功終了) する経路は存在しない。後述の変異 3 で実測した。

呼び出し側への影響も見た。`.github/workflows/release.yml` の `wait-for-registries` job はスクリプトの終了コードだけを見ており、出力文字列を解析する後段は無い (`smoke-ios` / `smoke-android` / `smoke-maui` が `needs` で繋がるのみ)。分類語彙が 1 つ増えても下流は壊れない。

## 自己テストの検出力 (ミューテーション実測)

作業ツリーは触らず、scratchpad の複製に変異を入れて自己テストを走らせた。

| 変異 | 落ちた検査 |
|---|---|
| 1. `reset_targets` の初期値を `STATE_UNPROBED` → `STATE_PENDING` に戻す | NG 6 件 (「1 度も照会していない対象は未照会のまま残る」「照会していない対象 1〜3 は未照会のまま」「未照会は出力でも未反映と区別できる」「照会していない対象を未反映として出さない」) |
| 2. `state_label` から未照会の分岐を落とす | NG 1 件 (「未照会は出力でも未反映と区別できる」— 「不明な分類 (unprobed)」に化けて検出される) |
| 3. `has_unreflected` が未照会を完了扱いにする | NG 1 件 (「未照会が残っていれば待機は続く」) |
| 4. `poll_once` の期限による `break` を外す | NG 9 件 (期限後の巡回・巡回途中の期限・未照会の保持がまとめて崩れる) |

変異 1 の 6 件は実装者の報告と一致する。追加された検査はいずれも実装を壊すと落ちる (空回りしていない)。

## 足場の凍結

前回レビュー (`review-003.md` 18:47) 以降に更新されたのは `scripts/release/wait-for-registries.sh` (18:51) のみ。`proposal.md` / `specs/release-workflow/spec.md` / `exploration.md` は HEAD からの差分が無く、実装中の書き換え (逆流) は無い。

## 指摘事項

Critical / Major に相当する実害は無し。

この回では新規の Suggestion を挙げていない。review-003 の Suggestion 4 件と second-opinion-code-003 の Suggestion 1 件 (反映待ち job の予算の機械検査 / `writeback` の `keep`・`store` の到達性 / 期限値の 2 名持ち / `POLL_DEADLINE_SET=0` の呼び出し元不在 / python 2 本の実行権限 / 時間予算の検査の自己テスト) はいずれも今回の修正で悪化しておらず、オーナー報告事項として据え置く方針を尊重した。

## アクションプラン

1. なし (実装側の対応は不要)
2. `tasks.md` 5.2 (`dry-run` での release 起動) の扱いをオーナーに諮る — 現状の validate がどの version でも落ちるため未実施。この change の判定材料にはしていない

## 確認した観点 (指摘に至らなかったもの)

- 分類の定数が増えたことによる他分岐への波及 — `STATE_*` の全参照箇所 (`poll_once` / `state_label` / `has_unreflected` / `reset_targets`) を列挙して確認
- コメントの自己完結性 — 未照会の説明・`has_unreflected` の返り値の記述・冒頭の分類一覧はいずれも当該ファイルだけで意味が通る。`has_unreflected` の返り値の記述が逆だった点は今回の修正で訂正されている
- 自己テストの時刻依存 — 「巡回の途中で期限に達する」検査は `POLL_DEADLINE = SECONDS + 1` と 1 秒の応答遅延の組み合わせで、1 件目は必ず照会・2 件目以降は必ず打ち切りになる。境界が揺れないことを 5 回の反復実行で確認
- 出力を解析する下流の有無 — `release.yml` は終了コードのみを見る
