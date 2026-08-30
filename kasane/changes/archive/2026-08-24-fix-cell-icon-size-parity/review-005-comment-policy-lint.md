# レビュー結果: fix-cell-icon-size-parity (005 回目 / comment-policy lint 同梱分)

**日付**: 2026-08-23
**判定**: APPROVED

**対象**: review-004 の指摘に対する修正のみ。`scripts/comment_policy_rules.py` / `scripts/comment-policy-lint.py` / `android/.../KsCellRegistryTest.kt`。本 change 本体の実装 (review-001〜003 / verify-001 で判定済み) はレビュー対象外。

## サマリー

review-004 の Major 1 件・Minor 3 件はすべて解消している。報告を鵜呑みにせず、各修正について**逆ミューテーション** (修正前のパターンに戻して当該入力が誤ブロック / 取りこぼしになることを再現) と**リポジトリ全走査での偽陽性計測**を自分で実施し、修正が実在の欠陥を実際に塞いでいること、かつ新たな偽陽性を持ち込んでいないことを確認した。Suggestion 3 件のうち 2 件 (ADVISORY_CASES の欠陥・selftest の経路ラベル) も実効性を確認済み。

指示外の自己判断だった文書名列挙の拡張 (`exploration|deviation|agenda|history|roadmap`) は、review-004 が同じ穴として明示的に指摘していた項目であり、方向・粒度とも妥当。実測でも偽陽性 0 件。

新規の指摘は Suggestion 3 件のみで、いずれも本 change 内での対応を要求しない。

## 確認した観点と実測結果 (すべて自分で再現)

| 観点 | 実測 |
| --- | --- |
| `--selftest` | 全 30 チェック OK / exit 0 (検出ロジック 21・履歴記述 10・hook 疎通 5) |
| 全走査 (`--advisory`) | 禁止 0 件 / 要確認 2 件 (検査対象 670 ファイル)。要確認 2 件は既存 `だった` パターン由来で本 change の増分ではない |
| hook 終了コード | 独自に 17 ケースを流して全件期待どおり。公開ドキュメント URL 6 形式 = exit 0、禁止 6 形式 = exit 2、許容・advisory 5 形式 = exit 0 |
| コンパイル | `KsCellRegistryTest.kt` の変更はコメント修正と孤立 KDoc の削除のみで挙動を変えないため、確認すべきはコンパイル成立。`:ks-settingsview-ui` のテストコンパイルは成功 (参考として `testDebugUnitTest` の実行結果も 930 件 / 失敗 0 / エラー 0 / skip 0、`KsCellRegistryTest` は 10 件全通過) |
| 新規退行 | 全走査の禁止件数 0 → 0。既存 selftest ケースの挙動変化なし |
| 制約遵守 | `comment-policy.md` は未変更 (`git diff HEAD` 空)。`kasane/` 配下の変更は本 change 既存の `tasks.md` / `ui/brief.md` のみ |

### 個別の解消確認 (逆ミューテーションによる証明)

| review-004 の指摘 | 検証方法 | 結果 |
| --- | --- | --- |
| Major: 履歴記述の取りこぼし | selftest ケース `制約が外れた` / `可能になり` の発火、および全走査での偽陽性計測 | **解消**。両ケースとも advisory として発火。素朴パターン `外れた｜外した` は全走査 44 件ヒットするのに対し、絞り込み後の `(?:制約｜条件｜指定｜属性)が外れた` は 0 件。素朴な `可能に` は 11 件 (すべて `可能にする` / `可能になる` の正当用法) に対し `可能に(?:なった｜なり)` は 0 件。偽陽性ゼロで目的の語形だけを拾えている |
| Major: `KsCellRegistryTest.kt:169` のコメント | `Cell.kt` / `KsCellRegistry.kt` と突き合わせ | **解消**。後述の「契約との一致」参照 |
| Minor: `references?` の誤ブロック | `references?` に戻して再現 → 現行と比較 | **解消**。旧パターンでは `developer.android.com/reference/android/view/View` が `変更アーティファクト配下のパス参照` として BLOCK。現行は pass。hook 実行でも exit 0 |
| Minor: 孤立 KDoc (L33-36) | 現物確認 | **解消**。ブロックごと削除済み |
| Minor: タスク通番の単層取りこぼし | 階層限定パターンに戻して比較 | **解消**。`タスク 5` / `task 3` は旧 miss → 現行 HIT。許容ケース `assembleDebug タスクを 3 回実行して平均を取る` は旧・現行とも miss を維持。全走査でのマッチは 0 件で偽陽性なし |
| Suggestion: `ADVISORY_CASES` の欠陥 | blocking のみ該当する入力を構成して判定式を比較 | **解消**。`// 派生できるようになった。詳細は exploration.md` は blocking 1 件のみを返す。旧判定 (advisory 件数) = 0 で「期待 0 → OK」と誤表示、現行判定 (総件数) = 1 で NG として検出できる |
| Suggestion: selftest の hook 経路ラベル | worktree 内で `--selftest` を実行 | **解消**。`通常` と `worktree` が別々に表示される (旧実装では両方 `worktree` になっていた) |
| 自己判断: 文書名列挙の拡張 | 旧列挙と比較 + 全走査 | **妥当**。`exploration/deviation/agenda/history/roadmap` の 5 語すべて旧 miss → 現行 HIT。全走査でのマッチ 0 件。review-004 の Suggestion が挙げた語と完全一致で、独自解釈による拡大はない |

### 既存パターンが緩められていないか

変更方向はほぼすべて拡張・固定であり、緩和は `references?` → `references` の 1 箇所のみ (Suggestion 1 で後述)。`ようになった` の否定先読み `(?!場合|ら|とき|時)` は review-004 時点で既に存在していた (review-004 の全走査が要確認 2 件であり、先読みがなければ `TimePickerColorizer.kt:71` の `ようになった場合` が 3 件目として計上されるはず)。今回の変更は `|なり` の追加のみで、緩和ではない。

### コメントと現行契約の一致

`KsCellRegistryTest.kt` の書き換え後の記述は現行契約と一致する。

- 「`Cell` は sealed ではない」 — `Cell.kt` は `interface Cell` として定義され、KDoc も「`sealed interface` ではなく通常の `interface` として定義し…（core/ADR-0013）」と述べている。一致
- 「重複検出（`cellClassByViewType[viewType]` の逆引きチェック）」 — `KsCellRegistry.kt:113` に `cellClassByViewType` が実在し、`:133-135` で `cellClassByViewType[viewType]` を引いて別 `cellClass` なら `IllegalArgumentException` を投げる。テストが検証しているパスと説明が一致
- 変更提案識別子 `add-samples-android` の裸参照は同ファイルから消えている (grep で 0 件)

## 指摘事項

### [🔵 Suggestion] スキームなしの公開ドキュメントパスが照合対象に残っており、`references` 固定は対症療法にとどまる

**該当箇所**: `scripts/comment_policy_rules.py:52-58` (成果物パス参照)、`scripts/comment_policy_rules.py:95` (`_URL_RE`)

**問題点**:
Minor 3 の根本原因は「`_URL_RE` が `https?://` 付きの形しか除去しない」ことであり、今回の修正はその症状のうち `reference/` の 1 件だけを潰している。根本が残るため 2 点の残渣がある。

1. **わずかだが実在の検出力を失っている**。review-004 の推奨文は「変更アーティファクトの実ディレクトリ名は複数形の `references` のみ…検出力は落ちない」としていたが、これは事実ではない。単数形の成果物ディレクトリが実在する:

   ```
   kasane/changes/archive/2026-07-31-remigrate-concepts/reference/old-concepts/
   ```

   実測で `// 旧概念は reference/old-concepts/foo.md` は現行パターンで無検出 (旧パターンでは blocking)。`kasane/` 接頭辞付きの形は第 1 パターンが拾うため実害は小さいが、**推奨文の前提が誤っていた**ことは記録しておく (蒸留でこの前提が持ち越されないように)。

2. **同型の誤ブロックが別の入口に残っている**。実測で `// github.com/foo/bar/blob/main/roadmap.md` が `変更アーティファクト文書への参照` として BLOCKING になる。これはスキームなしの公開文書パスであり、Minor 3 が問題にしたのと同じ類型である。

**推奨対応**: 本 change 内での対応は不要。将来的には `_URL_RE` をスキームなしのホストパス (`[\w-]+(?:\.[\w-]+)+/\S+` 相当) まで広げて照合前に除去する形にすれば、`references?` を戻したうえで両方の残渣が同時に解ける。`logs/` `artifacts/` の一般語衝突 (review-004 の Suggestion、オーナー判断で現状維持) とも同じ根を持つため、まとめて扱うのが妥当。

### [🔵 Suggestion] 新規追加した 2 パターンだけ、兄弟パターンが持つ否定先読みを欠いている

**該当箇所**: `scripts/comment_policy_rules.py:80-81`

**問題点**:
同じ「過去に存在した制限が解けた」型を扱う 3 パターンのうち、`ように(?:なった(?!場合|ら|とき|時)|なり)` だけが「将来の仮定・実行時の条件」を除外する否定先読みを持ち、今回追加した 2 つは持たない。結果、先読みが除外しようとしたのと同じ用法が要確認として出る。実測:

```
advisory  // 到達可能になった場合に備えて保険をかける
advisory  // スクロールが可能になったら通知する
advisory  // 制約が外れたときに再計算する
clean     // 選択で駆動するようになった場合に備えた保険   ← 先読みを持つ兄弟は正しく除外
```

いずれも履歴記述ではなく、実行時の条件・将来の仮定を述べる正当な記述である。現リポジトリの該当は 0 件、かつ advisory なので書き込みは止まらないが、次にこの語形が書かれた時点でノイズになる。パターン群の中で守りの強さが揃っていないこと自体が、後から読んだときに意図を取り違える原因になる。

**推奨対応**: 本 change 内での対応は不要。`(?:制約|条件|指定|属性)が外れた(?!場合|とき|時|ら)` / `可能に(?:なった(?!場合|とき|時|ら)|なり)` のように、兄弟と同じ先読みを揃える。

### [🔵 Suggestion] selftest の回帰ケースを持たない advisory パターンが残る

**該当箇所**: `scripts/comment-policy-lint.py:83-94` (`ADVISORY_CASES`) ↔ `scripts/comment_policy_rules.py:66-86`

**問題点**:
`ADVISORY_PATTERNS` 8 件のうち、`旧(?:実装|方式|…)`、`全面刷新|から移植|…|だった`、`(?:で|に)はなくなった` の 3 件に対応する `ADVISORY_CASES` がない。selftest は「検査が壊れて無音になっていないか」を確認する仕掛け (規約 `comment-policy.md` の機械検査節) なので、ケースを持たないパターンは静かに腐りうる。うち `(?:で|に)はなくなった` は前回追加分でありながらケースが用意されていない。

**推奨対応**: 本 change 内での対応は不要。次に selftest に触れる際、パターンとケースが 1 対 1 で対応することを埋め合わせる。

## アクションプラン

本 change 内で必要な対応はない。Suggestion 3 件は蒸留への申し送り候補:

1. `_URL_RE` のスキームなしホストパス対応 (Suggestion 1) — `logs/` `artifacts/` の一般語衝突とまとめて扱う
2. advisory パターン間の否定先読みの統一 (Suggestion 2)
3. `ADVISORY_PATTERNS` と `ADVISORY_CASES` の 1 対 1 対応 (Suggestion 3)
4. review-004 から引き継ぐ申し送り: 規約本文 (`comment-policy.md`) の「禁止する参照」へタスク通番と接頭辞なしパス形式を追記する
