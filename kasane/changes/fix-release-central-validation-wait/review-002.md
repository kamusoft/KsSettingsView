# レビュー結果: fix-release-central-validation-wait (002 回目)

**日付**: 2026-09-07
**判定**: APPROVED

## サマリー

前回 (001) の Minor 2 件・Suggestion 3 件はいずれも解消されている。検証待ちは独立 step `Wait for Central Portal validation` として `Upload deployment id` の後・`Login to NuGet.org` の前に置かれ、回収不能な cancel 窓は元の長さ (upload 受理〜ID 保存の数秒) に戻った。`if` の条件・後始末との配線・自己テストの検出力を実測で確認し、step 分離で新たに開いた穴は見つからなかった。

自己テストは 42 → 49 チェックに増え全件成功。comment-policy lint / doc-structure lint / local-path lint / identity lint はいずれも本変更の 3 ファイルに指摘なし。指摘は Critical / Major / Minor なし、Suggestion 1 件のみ (自己テストのチェック名に関する後読みやすさの話で、回帰検出力の穴ではない)。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 今回追加・改稿されたコメント (workflow 3 箇所・script は前回から変更なし) を節ごとに照合。許容する外部参照 / 禁止する参照 (作業文書パス・変更識別子の裸参照・ローカル通番) / 禁止する記述類型 (進捗ログ・履歴記述・過去仕様の説明・デルタスペック構文キーワード) のいずれにも該当なし。公開 doc コメントは対象外 (workflow / shell script)
- `kasane/handbook/cross/release-procedure.md` (リリースを行うとき / 再実行するとき) — 本変更が更新する当事者の文書として、記述と実装の一致を照合
- `kasane/handbook/cross/test-execution.md` (テスト実行・報告) — 本変更のテストは bash の自己テストのみで platform テストの節は非該当。実行件数の併記の規律に従い件数を報告する
- `kasane/lessons/code-review.md` — 重点観点 L-001 (アサーションの検出力はミューテーションで実測する) を適用。今回削除されたチェックの穴埋めが効いているかを実測で確認した (下記)

## 実行した検証

| 検証 | 結果 |
|---|---|
| `bash scripts/release/central-portal.sh --selftest` | 「失敗なし」(exit 0)。49 チェック / 0 失敗。HEAD 時点は 42 チェックなので本変更で +7 (すべて `[wait-validated]`) |
| `python3 scripts/comment-policy-lint.py` | 禁止 0 件 (検査対象 784 ファイル)。`--advisory` でも本変更の 3 ファイルに報告なし |
| `python3 scripts/doc-structure-lint.py` | `release-procedure.md` に指摘なし (exit 1 は roadmaps 配下の既存違反によるもので、本変更とは無関係) |
| `python3 scripts/local-path-lint.py` / `identity-lint.py` | いずれも指摘なし |
| workflow の YAML パース + 全 run ブロックの `bash -n` | パース成功、構文エラー 0 件。step 並びは `Upload deployment id` → `Wait for Central Portal validation` → `Login to NuGet.org` → `Push packages to NuGet.org` → `Release Maven Central deployment` |
| ミューテーション実測 2 件 | 下記「前回指摘への対応」に記載。実施後は backup との shasum 一致 (`d6cb38ab…`) と自己テスト再実行で原状復帰を確認済み |

## 前回指摘への対応の確認

### Minor 1 (cancel / timeout で保留 deployment の手がかりが残らない) — 解消

fresh upload 経路の待ちが `Publish Android to Central Portal` step の外へ出て、独立 step `Wait for Central Portal validation` (`.github/workflows/release.yml:715`) になった。step 並びは上表のとおりで、`Upload deployment id` (`:699`) が artifact へ ID を保存した後に待つ。これで最大 30 分の待ちは「ID が durable になった後」に移り、cancel / job timeout で `if: failure()` の後始末が走らなくても、次の attempt が artifact から ID を引き継げる。回収不能な窓は upload 受理〜`Upload deployment id` 完了の元の長さ (数秒) に戻っている。

引き継ぎ経路の待ち (maven step 内) も同様に安全で、そこで cancel されても ID は前の attempt が保存済みの artifact に残る。

### Minor 2 (90 分の予算に待ちの合計が収まらない) — 解消

`timeout-minutes` が 120 になり (`:434`)、根拠が直上のコメントに書かれた。最悪経路を数え直すと「引き継いだ deployment の決着 30 → FAILED で drop して再 upload した deployment の決着 30 → `wait-published` 30」= 90 分 + 本体の作業 11 分 = 約 101 分で、120 分に収まる。4 本目の待ちが並ぶ経路は無い (引き継ぎが `PUBLISHING` に落ちた場合は `wait-published` で `release-needed=false` になり、新 step も `Release` step も skip される)。コメントの記述はこの数え方と一致する。

### Suggestion 1・2 (トートロジーの自己テスト / `arrange` の共有) — 解消

「待機中の進捗は標準出力に出ない」チェックが削除され、これに伴って `arrange` の共有も解消した。残った「上限を過ぎれば失敗する」は自前の `arrange` を持つ。

守りたい性質 (標準出力の純度) の検出力が落ちていないことをミューテーションで実測した。

- `cmd_wait_validated` の進捗行から `>&2` を外す → `[wait-validated]` の 4 件が NG (「PENDING / VALIDATING を経て VALIDATED を返す」「FAILED / PUBLISHING / PUBLISHED は失敗にせず状態として返す」)。削除されたチェックは元々このミューテーションで OK のままだったので、検出力は前回の状態から減っていない
- 上限判定 (`if [ "${SECONDS}" -ge "${deadline}" ]` のブロック) を削除 → 「上限を過ぎれば失敗する」が NG

### Suggestion 3 (handbook の FAILED 行) — 解消

`kasane/handbook/cross/release-procedure.md:154` は「upload からやり直す (NuGet は未 push なので、原因を直せば同じ version で埋め直せる)」になり、表の列 (再実行で起きること) の意味に揃った。直後の段落との重複も無くなっている。

## 確認した観点 (問題なしと判断したもの)

- **`release-needed` と新 step の `if` の整合**: `release_needed=true` になるのは (a) 引き継いだ deployment が VALIDATED、(b) fresh upload に成功、の 2 経路だけで、どちらも `deployment_id` は非空 (b は空なら `exit 1`)。よって新 step が走るとき `steps.maven.outputs.deployment-id` は必ず埋まっており、`Upload deployment id` (`if: … deployment-id != ''`) も必ず先に走っている。`release-needed=false` の 3 経路 (`PUBLISHING` は maven step 内で `wait-published` 済み / `PUBLISHED` / Maven Central に公開済みで upload skip) はいずれも決着済みなので、待つ必要が無い
- **失敗経路の後始末の配線**: 新 step が落ちたとき maven step の `deployment-id` は非空なので、`Recover deployment id from upload log` (`if: failure() && steps.maven.outputs.deployment-id == ''`) は正しく skip され、`Drop pending deployment` (`if: failure() && (steps.maven.outputs.deployment-id || steps.recover-deployment.outputs.deployment-id) != ''`) が maven 側の ID を拾って走る。上限超過なら状態は VALIDATING で `cmd_drop` が何も送らず ID が残り、FAILED なら DELETE されて `status` が NOT_FOUND を返し `cleared=true` で ID が空に置き換わる — handbook の新しい 2 行と一致する
- **新 step の失敗の作り方**: `set -euo pipefail` の下でコマンド置換に `wait-validated` を受けているので、上限超過 (`fail` が `::error::` を出して exit 1) はそのまま step の失敗になる。VALIDATED 以外の決着は明示の `::error::` + `exit 1` で止まる。いずれも NuGet push の前
- **引き継ぎ VALIDATED での二重照会**: maven step 内と新 step で `wait-validated` を 1 回ずつ呼ぶため状態照会が 2 回になるが、いずれも副作用の無い GET 相当の照会で、待ちも発生しない (どちらも初回照会で即座に返る)。新 step のコメントの記述と一致する
- **handbook「起動」段落と実装**: 「Maven の upload の直後に Central Portal の検証の決着を待ち (上限 30 分)、検証を通らなければ NuGet へ push する前に止まる」は step 分離後も成立する。upload と待ちの間にあるのは ID を artifact へ保存する step だけで、NuGet push はその後。「上限 30 分」は `KSR_POLL_TIMEOUT_SECONDS` の既定 1800 秒と一致し、workflow は上書きしていない。この段落は通常の初回リリースを説明する文脈なので、再実行で待ちが 2 本並び得ることは「失敗したとき」の表が受け持つ形で矛盾しない
- **サブコマンドの配線**: usage・ヘッダの契約・`main` の受理リスト・認証情報の要求 (`published` 以外) ・内側の dispatch のすべてに `wait-validated` が入っている。新 step にも `MAVEN_CENTRAL_USERNAME` / `MAVEN_CENTRAL_PASSWORD` が渡っている
- **足場アーティファクト**: `exploration.md` は前回レビュー時点から変更されていない (今回のサイクルで書き換えられていない)。実装が待ちを独立 step にしたのは決定事項「upload の直後、NuGet push より前で待つ」を満たしたままの配置変更であり、「変更級の推奨」節の見積り (upload step に待ちを追加) は決定事項ではないので逸脱にあたらない。handbook についても決定事項は「待ちの位置が変わることに合わせて直す」であり、upload 行を書き換える代わりに新しい失敗位置の 2 行を足した形は意図を満たしている
- **待ちの位置変更の副作用**: NuGet push 以降で失敗すると、後始末は VALIDATED になった deployment を drop するため、次の attempt は upload からやり直しになる (本変更前は失敗時点でまだ VALIDATING のことが多く、ID を引き継げた)。再実行が gradle の作り直しと再 upload のぶん重くなるが、孤児 deployment は残らず同じ version を埋め直せるので、正しさの問題ではない。handbook の upload 行が説明する「検証済みなら upload せず release へ」の経路も、上限超過で VALIDATING が残る新しい行から従来どおり到達できる

## 指摘事項

### [🔵 Suggestion] 標準出力の純度を名前で示すチェックが自己テストから消えた

**該当箇所**: `scripts/release/central-portal.sh:22-27` (ヘッダの契約) / `scripts/release/central-portal.sh:434-456` (`[wait-validated]` の自己テスト)

**問題点**: 「待機中の進捗行は標準エラーへ出す」はヘッダの契約として明記された性質だが、それを検査していると名前で分かるチェックが無くなった。実測のとおり性質自体は 4 件のチェックが守っており回帰検出力の穴ではないが、将来この性質を壊した人が見るのは「VALIDATED を返す」「FAILED は状態として返す」といった、出力先とは無関係に読める名前のチェックが 4 件まとめて落ちる状況になる。原因に辿り着くのに一段余計にかかる。

**推奨修正**: 前回の推奨のうち「上限を過ぎない設定で 1 回以上ポーリングさせ、標準出力が決着状態の 1 行だけであることを確かめる」形を 1 件足す (`arrange 'VALIDATING' 'VALIDATED'` を `KSR_POLL_INTERVAL_SECONDS=0` で回し、標準出力の行数が 1 であることを見る)。数行で済み、名前が契約を説明する。急ぐ話ではないので、見送っても本変更を止める理由にはならない。

## アクションプラン

1. (任意) Suggestion — 標準出力の純度を名前で示す自己テストを 1 件足す。見送り可

Critical / Major / Minor はなく、前回指摘はすべて解消済み。この状態で完了として差し支えない。
