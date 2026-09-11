# レビュー結果: backport-release-workflow-hardening (003 回目)

**日付**: 2026-09-11
**判定**: APPROVED

## サマリー

前回 (002) の Major 1 件・Minor 2 件はすべて閉じている。切り詰めの撤回で `request_max_time` は定数を返すだけになり、歯止めは「期限を過ぎた照会を送らない」(`poll_once`) と「sleep 後が期限を過ぎるなら巡回に入らない」(ループ末尾) の 2 箇所へ移った。3 種のミューテーション実測で、この 2 箇所と応答上限の固定はいずれも自己テストが検出することを確かめた (下表)。**今回の修正が新しい Critical / Major を持ち込んでいる形跡は無い。**

残るのは Minor 1 件 — 1 巡目の途中で期限が切れた場合に、一度も照会していない対象が初期値のまま「未反映」として出力される。既定値では到達不能 (1 巡は最悪 480 秒、上限は 2700 秒) で、spec の分類語彙にも「未照会」は無いため**許容範囲と判断する**が、直すなら 2 行で済むので所見として残す。

## 照合した規約

`kasane/handbook/index.md` から cross の index を開き、担当範囲 (`.github/workflows/` の 2 本、`scripts/release/` の bash / python、自己テストの実行と件数報告) に当たる文書を本文までロードした。

- `kasane/handbook/cross/comment-policy.md` (always) — 新規コメントが多いため全節を照合。許容参照 (リポジトリ内のコードファイル名)・禁止参照 (作業文書パス・変更識別子・ローカル通番)・禁止類型 (履歴記述・デルタスペック構文キーワード) を個別に確認
- `kasane/handbook/cross/test-execution.md` (テスト実行・テスト結果の報告) — 自己テストと検査の実行、件数の併記。3 platform の本体コードに変更が無いため iOS / Android / MAUI のテストスイートは対象外と判定した
- `kasane/handbook/cross/release-procedure.md` (release workflow を触る作業) — publish の待ちと再実行の手順、失敗時の引き継ぎの記述との整合
- 適用外と判定: `runtime-behavior-verification.md` / `sample-parity.md` / `public-identifiers.md` / `diagnostic-message-language.md` / `aiforms-origin-reference.md` / `user-skill-api-listing.md` / `local-development-setup.md`
- `kasane/lessons/code-review.md` の重点観点 [L-001] を適用し、修正の検出力をミューテーション実測で確かめた
- `kasane/decisions/cross/index.md` の accepted ADR に抵触なし。`kasane/config.yaml` の `skills.code-review` は空、ドメイン cross に `domain-skills` のオーバーレイ無し

## 実行結果

リポジトリルートで実行 (lint job と同じ内容)。

| 実行 | 結果 |
|---|---|
| `scripts/release/central-portal.sh --selftest` | 失敗なし (52 件 OK) |
| `scripts/release/wait-for-registries.sh --selftest` | 失敗なし (53 件 OK) |
| `scripts/release/deployment-handover.sh --selftest` | 失敗なし (22 件 OK) |
| `python3 scripts/release/check-time-budget.py` | 合計 217 分 / 上限 240 分 / 余裕 23 分 (exit 0) |
| `python3 scripts/release/check-publish-step-order.py` | 順序は正しい (exit 0) |
| `python3 scripts/release/check-publish-step-order.py --selftest` | 失敗なし (7 件 OK) |
| `python3 scripts/local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` | 禁止 0 件 (comment-policy は `--advisory` でも変更ファイルに指摘なし) |
| 両 workflow の YAML 解析 | 正常 (jobs を読める) |

## 前回指摘の閉じ確認

ミューテーションはすべて backup からの復元を shasum で確認したうえで行い、作業ツリーは原状のまま。

| 前回 (002) の指摘 | 現在 | 検出力の実測 |
|---|---|---|
| (Major) 期限到達後の最後の 1 巡が、未反映を「通信そのものの失敗」へ書き換える | 切り詰めを撤回し `request_max_time` は定数を返す (`scripts/release/wait-for-registries.sh:104`)。期限後は巡回に入らない (`:402`)、巡回内でも期限を過ぎたら送らない (`:339`) | 変異 A (`poll_once` の break 除去) → NG 3 件。変異 B (末尾の判定を `SECONDS >= deadline` へ戻す) → NG 1 件。変異 C (`request_max_time` を残り時間で切り詰める形へ戻す) → NG 2 件 |
| (Minor / SO-002) 残り 7 秒の自己テストが秒境界で不安定 | `can_start_probe <残り秒数>` (純粋関数) と `remaining_seconds()` (時刻の計算) に分離 (`:114-126`)。検査は固定入力 7 件 + 未設定 1 件 | 秒境界に依存する検査は残っていない (`grep` で確認)。`can_start_probe` を `-ge 0` に変えれば「残り 0 秒なら照会を始めない」が落ちる形 |
| (Minor) `central-portal.sh` の自己テスト上限に関するコメントが実態と食い違う | `scripts/release/central-portal.sh:363-368` の安全網の説明と、`:538-540` の「この自己テストで効いているのは冒頭の 5 秒であって既定値ではない」の注記 | — (コメントのみ) |

### 特に見た経路

- **時間の上界**: 照会は `remaining_seconds > 0` のときだけ始まり、1 件ごとに再判定する。したがって期限を跨げる照会は常に 1 件までで、上界は `上限 2700 秒 + 応答上限 120 秒 = 2820 秒 (47.0 分)`。job の `timeout-minutes: 60` (`.github/workflows/release.yml:921`) に対して 13 分の余裕。実装者の算出は正しい
- **歯止めの片肺経路**: ループ末尾を通過した時点で `SECONDS + interval < deadline` が成立し、`interval >= 0` なので sleep 明けも `SECONDS < deadline` が保たれる。つまり**どの巡回も先頭の未反映対象は必ず照会される**ので、「巡回に入ったが 1 件も照会しない」空回りは (sleep が要求より延びた場合を除いて) 起きない。逆に `poll_once` が break した時点でその対象は未反映なので `has_unreflected` が真になり、必ず末尾の判定に到達する。片方だけが働いて素通りする経路は見つからなかった
- **成功経路**: `poll_once` → `has_unreflected` が偽 → `return 0`。末尾の打ち切り判定より前にあり、期限直前に全件反映済みになっても成功する (自己テスト「全件が反映済みになれば成功する」で担保)
- **待機の外から 1 回だけ照会する経路** (`POLL_DEADLINE_SET=0`): `remaining_seconds` が常に 1 を返して照会を許す。自己テストで担保 (下の Suggestion 2 も参照)
- **自己テスト 53 件の空回り**: 常に通るだけの検査は見つからなかった。`all_calls_use_full_max_time` は記録が空だと真になるが、直前の「4 対象を 1 巡だけ照会する」で件数を固定しているため空にならない。「期限を過ぎていても応答上限は変わらない」は変異 C を捕まえられず (残りが負のとき切り詰め側も 120 を返すため)、同趣旨の「期限が目前でも〜」が捕まえた。2 件のうち片方は検出力が弱いが、無意味ではない

## 指摘事項

### [🟡 Minor] 一度も照会していない対象が「未反映」として出力される

**該当箇所**: `scripts/release/wait-for-registries.sh:316` (初期値 `STATE_PENDING`)、`scripts/release/wait-for-registries.sh:339-343` (期限超過時の break)、`scripts/release/wait-for-registries.sh:699-700` (同趣旨の自己テスト)

**問題点**: `reset_targets` は 4 対象すべてを `STATE_PENDING` (=「未反映」) で初期化する。`poll_once` が期限超過で break すると、その先の対象は照会されないまま保持している分類が出力される。1 巡目の途中で期限が切れた場合、その対象は**一度も照会していないのに「未反映」**として失敗出力に並び、「照会できたうえで未反映」と区別できない。実測 (上限 2 秒 / 1 件目の照会に 3 秒かかる状況を作って再現):

```
巡回 1 の分類:
  Maven Central jp.kamusoft:kssettingsview: 未反映     ← 実際に 404 を得た
  nuget.org/kssettingsview.maui: 未反映                 ← 一度も照会していない
  nuget.org/kssettingsview.binding.ios: 未反映          ← 同上
  nuget.org/kssettingsview.binding.android: 未反映      ← 同上
```

既定値 (上限 2700 秒、照会 1 件あたり最悪 120 秒 × 4 対象 = 480 秒) では 1 巡目が期限内に必ず終わるため、本番で到達する経路ではない。2 巡目以降で break しても、その対象は 1 巡目で得た分類を保持しているので出力は嘘にならない。到達するのは `KSR_POLL_TIMEOUT_SECONDS` を 360 秒未満へ上書きしたときだけ。

**spec への照らし**: Requirement「公開レジストリへの反映待ち」が区別を求めているのは「照会できたうえで未反映」と「照会そのものが行えなかったこと (= 判定不能の 3 種別)」であり、「まだ照会していない」という状態は spec の語彙に無い。また「出力に示す分類は、その時点で各対象が保持している分類とする」という条項に照らせば現在の挙動は文面どおりでもある。**したがって許容範囲と判断し、本件を CHANGES_REQUESTED の理由にしない。**

**推奨修正** (任意): 初期分類を `STATE_UNPROBED` (`state_label` で「未照会」) にして `reset_targets` の初期値だけ差し替える。2 行で、出力が嘘をつく余地が消える。あわせて自己テストの検査名「未反映の対象は未反映のまま残る」(`:700`) を実態に合わせる — 現在この名前は、一度も照会していない対象を「未反映」と呼ぶことを検査として固定してしまっている。

### [🔵 Suggestion] 期限の値が 2 つの名前で持たれている

**該当箇所**: `scripts/release/wait-for-registries.sh:380-382`

**問題点**: `POLL_DEADLINE` (グローバル、`remaining_seconds` が読む) と `deadline` (ローカル、ループ末尾の判定が読む) が同じ値の写しになっている。歯止め 2 箇所が別々の名前を経由して同じ値を見ているため、片方だけを書き換える改修で 2 つの歯止めがずれうる。

**推奨修正**: ローカル `deadline` を廃し、末尾の判定も `POLL_DEADLINE` を直接見る。

### [🔵 Suggestion] 期限未設定の分岐に本番の呼び出し元が無い

**該当箇所**: `scripts/release/wait-for-registries.sh:108-120`

**問題点**: `POLL_DEADLINE_SET=0` の分岐 (待機の外から 1 回だけ照会する使い方への配慮) は、`main` が `wait_for_registries` か `selftest` しか呼ばないため本番では通らない。自己テストからしか到達しない防御コードで、前回から持ち越しの `deployment-handover.sh writeback` の `keep` 到達性と同じ型の所見。実害は無いので、オーナー報告のときに同じ項目としてまとめて扱えばよい。

### [🔵 Suggestion] 新規 python 検査 2 本に実行権限が無い

**該当箇所**: `scripts/release/check-time-budget.py:1`、`scripts/release/check-publish-step-order.py:1`

**問題点**: どちらも `#!/usr/bin/env python3` を持つが mode は 644。兄弟の `scripts/release/set-readme-version.py` は 755 で、shebang と実行権限が揃っている。CI は `python3 <path>` で呼ぶので動作には影響しないが、同じディレクトリ内で shebang の扱いが割れる。

**推奨修正**: `chmod +x` して揃えるか、shebang を落とす。

### [🔵 Suggestion] 時間予算の検査だけ自己テストが無い

**該当箇所**: `scripts/release/check-time-budget.py`、`.github/workflows/ci.yml:196-207`

**問題点**: ci.yml のコメントは順序検査に自己テストを付けた理由を「崩した入力を通す形に退化しても平時は緑のままになる」と説明しているが、同じ性質は時間予算の検査にもある。定数を読めなくなる退化は fail-closed (`single_value` がエラーにする) で防げているものの、`rows` から 1 項を落とすような退化は合計を静かに小さくするだけで検査は通り続ける。本レビューでは 3 通りのミューテーション (公開待ちの上限 90→120 分 / 応答上限 300→900 秒 / `timeout-minutes` 240→120) で現時点の検出力を実測し、いずれも失敗することを確認済み。

**推奨修正**: 順序検査と同じ形で、既知の内訳に対する期待値を持つ自己テストを足す。優先度は低い。

## アクションプラン

1. (任意・低優先) Minor 1 件 — 初期分類を「未照会」にするかをオーナーが決める。しないなら現状のまま完了してよい
2. (任意) Suggestion 4 件 — いずれも実害なし。1 と 4 は着手が安い
3. **本レビューとしては修正サイクルを回す必要は無い。** `tasks.md` 5.2 (dry-run) の扱いはオーナー裁定に委ねる
