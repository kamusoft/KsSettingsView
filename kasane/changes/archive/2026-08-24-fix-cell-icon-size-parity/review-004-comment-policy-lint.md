# レビュー結果: fix-cell-icon-size-parity (004 回目 / comment-policy lint 同梱分)

**日付**: 2026-08-23
**判定**: CHANGES_REQUESTED
**対象**: `scripts/comment_policy_rules.py` / `scripts/comment-policy-lint.py` の検出パターン追加と、それにより検出された既存コメント 3 件の修正のみ (本 change 本体の実装はレビュー対象外)

## サマリー

追加された 3 型のパターンは、いずれも規約 (`comment-policy.md`) が既に禁止している類型に対応しており、過検出方向の設計上の誤りは大きくない。実測でも全走査の禁止件数は 0 件のまま増えず、`--selftest` 全件 OK、hook のラチェット・終了コード・`--summary`・パス指定はすべて期待どおりに動くことを自分で再現して確認した (Android ユニットテストも 930 件成功 / 失敗 0)。

一方で、**追加した検査が拾えていない同型の記述が、修正したコメントの同一ファイル内 (`KsCellRegistryTest.kt:169`) にそのまま残っている**。これは「検査の穴を塞ぐ」という本作業の目的に対する直接の反証であり、かつ同一ファイル内で片方だけ直した不整合になっている。加えて同ファイル L35 の孤立 KDoc が「sealed Cell 派生」と述べており、書き換えた L49 の記述および `Cell.kt` の現行契約と矛盾する。この 2 件は同じファイル・同じ論点の隣接課題であり、本 change 内で閉じるべき。

パターン側では `references?/` の `?` により公開ドキュメントの裸ホストパス (`developer.android.com/reference/...`) が BLOCKING で誤ブロックされることを実測で確認した (規約は恒常的な公開文書の参照を許容している)。

## 確認した観点と実測結果

| 観点 | 実測 |
| --- | --- |
| `--selftest` | 全件 OK / exit 0 (新規 11 ケース含む) |
| 全走査 (`--advisory`) | 禁止 0 件 / 要確認 2 件 (検査対象 670 ファイル)、exit 0。残り 2 件は既存 `だった` パターンの偽陽性 |
| 違反時の終了コード | 一時的に違反行を注入して `exit 1` を確認、`--summary` でも同じ。注入ファイルは shasum 一致で原状復帰済み |
| パス指定 | `android/ks-settingsview-ui` (134 ファイル) / `ios/Sources` (117 ファイル) で正常に絞り込み |
| hook | 新パターン 2 型の新規混入で exit 2、advisory は exit 0、既存行の持ち越し (ラチェット) は exit 0、許容形式 (`cross/ADR-0026`・`ui/CellBaseLayout.kt`) は exit 0 |
| 回帰確認 | 修正 3 件の HEAD 版を新ルールで走査 → 3 件すべて advisory として発火 (旧ルールでは 0 件)。png パス / タスク通番の 2 型も probe で blocking 発火を確認 |
| テスト | `:ks-settingsview-ui:testDebugUnitTest` = 930 件 / 失敗 0 (Gradle は UP-TO-DATE 判定 = 現在のソースに対する結果) |
| 新規退行 | 全走査の禁止件数が 0 → 0 で増えていない。既存 selftest ケースの挙動変化なし |

## 指摘事項

### [🟠 Major] 追加した履歴記述パターンが、修正対象と同型の記述を同一ファイル内で取りこぼしている (かつ未修正のまま残っている)

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistryTest.kt:169`

**問題点**:
L49 は「`add-samples-android` で `Cell` の sealed 制約が外れたことにより…派生できるようになったため」を書き換えたが、同じファイルの L169 に実質同一の文が残っている。

```
 * `add-samples-android` で `Cell` の sealed 制約が外れたため、テストモジュール内でも
 * `Cell` 派生（`DummyOtherCell`）を定義可能になり、本ケースを実テストで検証できる。
```

L49 が検出できたのは `ようになった` に当たったからで、L169 は「制約が**外れた**ため」「定義**可能になり**」という言い換えのため新パターン (c) のどれにも当たらない。実測でも全走査でこの行は無検出。つまり (c) は「〜が外れた」「〜可能になった / 可能になり」型を取りこぼす。**同一ファイルの隣接行に反例が現存する**のは、パターンの絞り込み妥当性に対する具体的な反証である。

なお L169 は変更提案識別子 (`add-samples-android`) の裸参照でもあるが、そちらは既知の第 4 の穴でオーナー判断待ちのため指摘対象にしていない。ここで問題にしているのは**履歴記述としての取りこぼし**と、**同一ファイル内で片方だけ直した不整合**である。

**推奨修正**:
1. `ADVISORY_PATTERNS` に「〜が外れた / 外した」「〜可能になった (になり)」を含む語形を追加し、`--selftest` の `ADVISORY_CASES` に L169 の実文を回帰ケースとして入れる。追加後は全走査で新たな要確認件数が現実的な範囲に収まることを実測して示す。
2. L169 のコメントを、L49 と同じ方針 (現在形の自己完結した説明) に書き換える。

### [🟡 Minor] 修正した L49 と矛盾する孤立 KDoc が同一ファイルに残っている

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistryTest.kt:33-36`

**問題点**:
```
    /**
     * テスト用ダミー Cell（Core モジュール内の sealed Cell 派生のうち本テストで触りたいもの）。
     * `LabelCell` を使用する。
     */
```
これは (a) 直後に宣言を伴わない宙ぶらりんの KDoc であり、(b) `Cell` を sealed と述べていて `Cell.kt:6-10` の現行契約 (`sealed interface` ではなく通常の `interface`、core/ADR-0013) に反し、(c) 13 行下の L49 で新たに書いた「`Cell` は sealed ではないため」と正面から矛盾する。今回の修正はファイル内の記述整合を目的としているのに、その目的が同一ファイル内で達成できていない。

**推奨修正**: 情報価値がないため KDoc ブロックごと削除する (残すなら「本テストは `LabelCell` を既存 Cell 型として使う」等、現行契約と矛盾しない現在形の説明にする)。

### [🟡 Minor] `references?/` の単数形マッチで公開ドキュメントの裸ホストパスが誤ブロックされる

**該当箇所**: `scripts/comment_policy_rules.py:48` (`|references?`)

**問題点**:
規約は「恒常的に到達可能な公開文書」への URL 参照を明示的に許容しているが、`_URL_RE` が取り除くのは `https?://` 付きの形だけである。スキームなしの `developer.android.com/reference/android/view/View` は URL 除去をすり抜け、`reference/` + `android/` にマッチして **BLOCKING (hook exit 2)** になる。実測で確認済み:

```
exit=2  // 仕様は developer.android.com/reference/android/view/View に従う
        - 変更アーティファクト配下のパス参照
```

規約が禁止していない記述を、しかも誤った理由表示でブロックしている。現リポジトリに該当行は 0 件だが、Android / Apple のドキュメント参照は今後書かれうる形である。

**推奨修正**: 変更アーティファクトの実ディレクトリ名は複数形の `references` のみ (`kasane/changes/*/ui/references/`) なので、`references?` から `?` を外して `references` に固定する。検出力は落ちない。

### [🟡 Minor] タスク通番が階層通番 (N.N) 限定で、単層通番を取りこぼす

**該当箇所**: `scripts/comment_policy_rules.py:55`

**問題点**:
`(?:タスク|[Tt]ask|TASK)\s*[0-9]+\.[0-9]+` は `タスク 2.4` を拾うが `タスク 5` / `task 3` を拾わない。両者は同じ「変更アーティファクト内の通番」であり、規約上の扱いに差はない。絞り込みの根拠として「タスク単体は Gradle タスク等でも使う」と説明されているが、この判定は既に `\s*[0-9]+` を要求しているため、語単体の曖昧さは論点にならない。実測すると、単層まで広げたパターン `(?:タスク|[Tt]ask|TASK)\s*[0-9]+` にマッチするコメント行はリポジトリ全体で **0 件** — つまり絞り込みで避けられた偽陽性は実測 0 件、失われた検出力は 1 型ぶんある。既存の `Phase|Round|Decision\s*[0-9]` も単層のまま運用できている。

**推奨修正**: `(?:タスク|[Tt]ask|TASK)\s*[0-9]+(?:\.[0-9]+)*` に広げる。既存 selftest の許容ケース (`assembleDebug タスクを 3 回実行して平均を取る`) は「を」が挟まるため引き続き 0 件で通る。広げるなら `タスク 5` / `task 3` を selftest ケースに追加すること。

### [🔵 Suggestion] ADVISORY_CASES の「許容」ケースが blocking 側の誤検知を素通しする

**該当箇所**: `scripts/comment-policy-lint.py:75-82`, `scripts/comment-policy-lint.py:103`

**問題点**: 判定が `f[0] == "advisory"` の件数だけを見ているため、許容のつもりのケース (期待 0) が **blocking として誤検出された場合でも OK と表示される**。`scan_text` は blocking が当たると advisory を評価しないので、最も知りたい「許容記述を誤ってブロックしていないか」が検証されない構造になっている。

**推奨修正**: 期待 0 のケースは種別を問わず総件数 0 を assert する (`len(scan_text(...)) == 0`)。

### [🔵 Suggestion] 汎用語のディレクトリ名がランタイム用途と衝突しうる

**該当箇所**: `scripts/comment_policy_rules.py:48-49`

**問題点**: 列挙されたディレクトリ名は `kasane/` 配下に実在するもの (`evidence` `screenshots` `artifacts` `candidates` `logs` `spike` を確認) なので根拠はあるが、`logs/` `artifacts/` はランタイム / CI の一般語でもある。実測で `// ログは logs/latest.txt に出力される` が BLOCKING になることを確認した。現リポジトリの該当は 0 件で、`comment-policy:allow` の逃げ道もあるため実害は小さいが、記録として残す。

**推奨修正**: 当面は現状維持で可。誤検知が実際に出たら、これらを advisory 側へ降格するか `ui/` `changes/` 等の親セグメントを要求する形に絞る。

### [🔵 Suggestion] selftest の hook 疎通ラベルが worktree 実行時に「通常」経路を検証していない

**該当箇所**: `scripts/comment-policy-lint.py:114-132`

**問題点**: `where` の判定が `"/worktrees/" in target` なので、worktree 内で実行するとリポジトリルート側のターゲットもパスに `/worktrees/` を含み、両方とも「worktree」と表示される (実際に本レビューの実行結果は全行が worktree 表示)。「通常パスでも発火する」ことを確認する意図が worktree 実行時に失われている。本 change の追加分ではないが、selftest に手を入れる今回のついでに直せる。

**推奨修正**: 判定を「そのケース用に組み立てた 2 つのターゲットのどちらか」で持つ (ループのインデックスやラベルを明示的に渡す) 形にする。

### [🔵 Suggestion] 規約本文に「タスク通番」の明示がない

**該当箇所**: `scripts/comment_policy_rules.py:55` ↔ `kasane/concepts/cross/conventions/comment-policy.md` の「禁止する参照」

**問題点**: 規約の列挙は `Phase / Round / Decision / Critical / Major / Minor / 論点 / 案 番号` で、タスク通番は明示されていない (「レビュー指摘・議論の通番 … 変更提案内の通番であり」の趣旨には収まる)。lint が規約本文より広い状態は、後から「なぜブロックされるのか」を規約から辿れない。本 change では規約本文を変更しない方針のため、蒸留への申し送りとして残す。

**推奨対応**: 蒸留時に `comment-policy.md` の禁止列挙へ「タスク通番 (`タスク 2.4`)」を追記する。

### [🔵 Suggestion] 接頭辞なしの変更アーティファクト文書名に既存の穴が残る

**該当箇所**: `scripts/comment_policy_rules.py:34`

**問題点**: 文書名パターンは `proposal|design|tasks|brief|spec` のみで、`exploration.md` `deviation.md` `agenda.md` `history.md` `roadmap.md` は接頭ディレクトリを伴わない限り無検出 (実測で `// 詳細は exploration.md を参照` が 0 件)。本 change が持ち込んだ穴ではないが、今回の「接頭辞なし形」を塞ぐ趣旨と同じ穴である。

**推奨対応**: 文書名の列挙に `exploration|deviation|agenda|history|roadmap` を加える (別作業でも可)。

## アクションプラン

1. `KsCellRegistryTest.kt:169` のコメントを現在形の自己完結した説明に書き換える (Major)
2. `ADVISORY_PATTERNS` に「〜が外れた」「〜可能になった / になり」型を追加し、L169 の実文を `ADVISORY_CASES` の回帰ケースに入れる。追加後に全走査の要確認件数を再実測する (Major)
3. `KsCellRegistryTest.kt:33-36` の孤立 KDoc を削除する (Minor)
4. `references?` → `references` に固定する (Minor)
5. タスク通番を `[0-9]+(?:\.[0-9]+)*` に広げ、selftest ケースを追加する (Minor)
6. `ADVISORY_CASES` の期待 0 ケースを総件数 0 の assert に変える (Suggestion)
7. 残る Suggestion (汎用語衝突・selftest ラベル・規約本文への明示・文書名の穴) は本 change 内で対応するか蒸留へ申し送るかをオーナー判断
