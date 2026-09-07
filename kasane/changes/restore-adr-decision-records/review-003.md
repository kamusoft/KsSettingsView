# レビュー結果: restore-adr-decision-records (003 回目)

**日付**: 2026-09-07
**判定**: CHANGES_REQUESTED

## サマリー

review-002 が挙げた 6 件 (Major 1 / Minor 5) は**すべて事実として正しく解消されている**。とくに Major の訂正は、`kasane/changes/archive/2026-08-25-investigate-maui-icon-lease-sharing/` の実在と `maui/ADR-0026` の出典行がそこを指していることを実物で確認でき、deviation.md の記述は正確になった。android/0019 の `現行照合` は製品コード 1 ファイル / opt-in はテスト 2 本を含む、という実際の分布と一致し、`release-pipeline.md` の公開確認の表も `scripts/release/wait-for-registries.sh` の判定 (Maven = pom への HEAD、NuGet = flat container の index を 3 Package ID、SwiftPM = 専用確認なし) と一致する。成果物の本体 (ADR 8 本・新概念・移送元) は round-2 から動いておらず、追記マーカーは decisions/ 96 本を再走査して 0 行だった。

残るのは、round-3 の作業そのものが同じ穴を再現している点である。この round で新規追加された `kasane/lessons/inbox/absence-fallback-masks-failed-search-command.md` が deviation.md に現れず、直前に書き直した `concepts/log.md` の記述も「lessons: 捕捉済み 2 件」のまま実際の 3 件と食い違っている (log.md 19:09:07 → lessons ファイル 19:09:56 の順で書かれ、後から増えた 1 件が反映されていない)。review-002 の Minor 4 / 5 と同じ型が、指摘された 2 ファイルにだけ適用され、ルールとしては効いていない。加えて、新規 lessons のルール文は zsh 固有の挙動を根拠として書いており、bash では根拠が成立しない (危険は残る) ため、ksn-core references/rule-style.md の「どの環境に載っても同じに働くルールだけを書く」に照らして手当てが要る。

## 照合した規約

- `kasane/handbook/index.md` / `cross/index.md` の「適用のきっかけ」で判定 — 本 round の編集は Markdown 4 ファイル (`release-pipeline.md` / `android/0019` / `deviation.md` / `log.md`) と新規 lessons 1 本のみで、コード・テスト・ビルドファイルに触れないため該当する rule は無し。`cross/release-procedure.md` (guide) は deployment ID の二重管理判定の裏取りのため本文まで読んだ
- ksn-core `references/rule-style.md` (ルール文の 6 基準。新規 lessons の評価軸として user から明示指定)
- ksn-core `references/decisions.md` (footer 規約) / `references/concepts.md` (index・log 更新の必須性) / `references/paths.md` / `references/lessons.md` (inbox 形式・match-or-create・昇格トリアージ)
- `kasane/lessons/code-review.md` — 重点観点 L-001 (ミューテーション実測) はコード変更ゼロのため非該当。「指摘しないこと」は昇格済みルール無し
- `kasane/lessons/process.md` L-006 (不在の断定は検索を通してから) / L-009 (合意済みスコープ外のファイルは deviation.md へ 1 行)

## review-002 の指摘 6 件の独立検証

| # | 主張 | 判定 | 検証したこと |
|---|---|---|---|
| 1 | Major: 追跡先 change は archive に実在すると訂正した | **正しい** | `kasane/changes/archive/2026-08-25-investigate-maui-icon-lease-sharing/` が実在 (commit `c7b458d` で追跡下)。`kasane/decisions/maui/0026-*.md:44` の出典行が同 archive の `exploration.md` / `deviation.md` / `review-004.md` を指す。deviation.md:19 の現文は「実在し、残課題 (2) の共有破棄はそこから maui/ADR-0026 へ結実している」で、事実と一致 |
| 2 | Minor: 製品コード 1 ファイル / opt-in はテスト 2 本を含む と書き分けた | **正しい** | `androidx.compose.material3.DatePicker` の import / 呼び出しは `DateCalendarDialog.kt:21,312` の 1 ファイルのみ。`@ExperimentalMaterial3Api` は同ファイル (`:26,72,188`) に加え `DateCalendarDialogTest.kt:8,41` と `DateCalendarRecreationTest.kt:8,44` の 2 本。footer の書き分けと一致 (review-002 が挙げた `DateCalendarRecreationTest.kt:41` は実際は `:44`。訂正後の footer は行番号を書いていないため影響なし) |
| 3 | Minor: deployment ID 項を handbook へのポインタに置き換えた | **正しい** | `release-pipeline.md:103` は「run の artifact で引き継ぐ」+「破棄できる状態とできない状態は [リリース手順] の『失敗したとき』が持つ」の形になり、review-002 が不正確と指摘した状態列挙 (`VALIDATED` / `FAILED` のみを drop 対象、`PUBLISHING` / `PUBLISHED` の欠落) は消えている。残した機構 (artifact で引き継ぐ) は `release.yml:60-61` の `KS_ARTIFACT_DEPLOYMENT_ID` と `:494-498` のコメントに一致し、handbook が持たない「どこに置くか」を述べているため二重管理ではない |
| 4 | Minor: deviation.md に `cross/index.md` と `log.md` を追記した | **正しい** | deviation.md:15 が両ファイルを箇所として名指しし、理由 (ksn-core references/concepts.md が index / log 更新を書き込み経路共通の必須手順としている) も正確 (同 references:10) |
| 5 | Minor: log.md を最終状態へ更新した | **概ね正しい (1 点だけ古い)** | 8 本の `判定` の内訳 (維持 = cross/0018・0020・0023・0028・android/0019、乖離あり = cross/0019・0021、maui/0015) は実物の footer と全件一致。`release-pipeline.md` の要約 (publish 段 9 ステップ・公開確認 3 チャネルの表・保証すること 4 項・してはいけないこと 3 項) も実物と一致。字数 5058 も現物 (総 5947 字 / 空白除き 5030 字) に照らして妥当。ただし lessons の件数だけが古い (下記 Minor 1) |
| 6 | Minor: 公開確認の表を実物に合わせた | **正しい** | `scripts/release/wait-for-registries.sh:47-53` が pom への `curl --head`、`:56-79` が flat container `index.json` の version 一覧を 3 Package ID (`kssettingsview.maui` / `kssettingsview.binding.ios` / `kssettingsview.binding.android`) で判定。SwiftPM が反映待ちの対象外で `smoke-ios` (`release.yml:906-913`、`verify-consumer-ios.yml` の smoke = 配信リポジトリの tag) に委ねられているのも表のとおり。Maven の「Portal に公開済みかを返す API が無いため HEAD で代替」という理由も残っている |

## 指摘事項

### [🟡 Minor] 同じ change で追加した 3 本目の lessons が deviation.md にも log.md にも現れない

**該当箇所**: `deviation.md` (付随修正の節) / `kasane/concepts/log.md:424`

**問題点**: 本 round で `kasane/lessons/inbox/absence-fallback-masks-failed-search-command.md` が新規追加されている (`git ls-files kasane/lessons/inbox/` に無い唯一のファイル)。これは 2 つの記録から漏れている。

1. **L-009 の事後判定が未達**。事後判定は「`git show --stat` に現れるファイルのうち合意済みスコープが名指ししていないものが、すべて deviation.md の行に箇所として現れている」。exploration.md の「実行」節が数える 10 ファイルにこの lessons は含まれず、deviation.md のどの行にも箇所として現れない。review-002 Minor 4 で指摘された型が、名指しされた `cross/index.md` / `log.md` の 2 件だけ埋められ、同じ round で増えた 3 件目に適用されていない。
2. **log.md:424 の記述が古い**。エントリは「lessons: 捕捉済み 2 件 (project-adr-carves-exception-into-harness-rule / implementation-results-accreted-into-adr-consequences)」と書くが、この 2 本は exploration 段階の commit `8266087` で捕捉されたもので、現在の inbox には本 change 由来が 3 本ある。書き込み順 (log.md 19:09:07 → lessons ファイル 19:09:56) からして、log.md を直した後に lessons を足して戻っていない。**長命層に誤った件数が残る**という、この change 自身が是正しようとしている型そのものである。

**推奨修正**: deviation.md の付随修正に 1 行足す (`- [付随修正] kasane/lessons/inbox/absence-fallback-masks-failed-search-command.md: review-002 Major の機序を新規パターンとして捕捉。理由: ...(2026-09-07)`)。あわせて log.md:424 の「捕捉済み 2 件」を 3 件に直し、3 本目の slug と「昇格済み process L-006 のシェル版として起票」を書き添える。

### [🟡 Minor] 新規 lessons のルール文が zsh 固有の挙動を根拠にしており、他シェルで根拠が崩れる

**該当箇所**: `kasane/lessons/inbox/absence-fallback-masks-failed-search-command.md:14-18`

**問題点**: ksn-core `references/rule-style.md` は冒頭で「どの環境に載っても同じに働くルールだけを書く」と定めるが、1 つ目の箇条書きは「複数パターンを 1 コマンドに並べない。**zsh は** glob が 1 つでもマッチしないとコマンド全体を中断するため、後続のパターンは評価されない」と、根拠を zsh の `nomatch` 挙動に置いている。この根拠は bash では成立しない — bash は不一致 glob をリテラルのまま渡すので `ls` は走り、マッチした側を **stdout に出したうえで** 終了ステータス非 0 を返し、`|| echo "(不在)"` が続けて「(不在)」を出す。つまり危険 (フォールバックが不在を主張する) は残るのに、bash 環境のエージェントが読むと「自分には当たらない」と読める。

あわせて機序の記述にもずれがある。実際に `ls -d A* b* 2>/dev/null || echo "(不在)"` を zsh で再現すると、出力は `(eval):1: no matches found: A*` (stderr) と `(不在)` (stdout) の**両方**で、`2>/dev/null` は中断されたコマンド側の redirect なのでシェル自身のエラーを隠さない。ルール文の「対象が無いときと検索自体が失敗したときの両方で**同じ出力を出す**ため、成功と失敗を区別できない」は厳密には成立せず、実際に起きたのは「フォールバックの行だけを読み、別ストリームのエラー行を読み落とした」である。対策の力点 (3 つ目の箇条書きの exit status 確認) は正しいので、機序の記述だけがずれている。

**推奨修正**: 根拠をシェル非依存の形へ書き換える (例:「glob や検索コマンドが失敗したときの挙動はシェルによって違う — zsh はコマンド全体を中断し、bash はマッチした分を出したうえで非 0 を返す。どちらでもフォールバックの『なし』は出るため、フォールバックの行を不在の根拠にしない」)。機序の 1 文も「同じ出力を出す」→「フォールバックの行が同じになり、シェルのエラーは別ストリームに紛れる」に直す。

### [🔵 Suggestion] 新規 lessons の事後判定が、成果物だけからは判定できない

**該当箇所**: `kasane/lessons/inbox/absence-fallback-masks-failed-search-command.md:20`

**問題点**: 事後判定は「不在を断定した箇所に、マッチ 0 件で正常終了する手段の出力か、exit status の確認が伴っている」。rule-style 5 は「守られたかどうかを成果物 (ファイル・報告・diff) から判定できる」ことを求めるが、この形だと判定材料 (コマンドの出力) は会話ログ側にしか無く、実際に事故が起きた成果物 (deviation.md) を見ても守られたかは分からない。今回まさに「deviation.md に不在の断定だけが残り、根拠は残らなかった」ので、判定材料が成果物に落ちる形にすると再発検知が機構になる。

**推奨修正**: 「不在を根拠にした記述には、確認に使ったコマンドとその終了ステータス (またはマッチ 0 件で正常終了する手段の出力) を同じ報告・同じ成果物に添える」の形へ寄せる。

### [🔵 Suggestion] 新規 lessons が rule-style の細目 2 点を外している

**該当箇所**: `kasane/lessons/inbox/absence-fallback-masks-failed-search-command.md:14,16`

**問題点**: 2 点。(a) rule-style 3「禁止を書くときは `<禁止>: <代わりの動き>` の対にする」に対し、1 つ目の箇条書き「複数パターンを 1 コマンドに並べない」は純禁止で、代わりの動きは 2 つ目の箇条書きに離れて置かれている。(b) rule-style 6「強調は承認ゲート・独立レビュー・足場凍結・機密の級に限る」に対し、ルール文冒頭に太字が 2 箇所ある (`検索コマンド自体が成功したことを確かめてから結果を読む` / `検索自体が失敗したとき`)。いずれも該当級ではない。

**推奨修正**: (a) 1 つ目と 2 つ目を 1 項目に畳んで「複数パターンを 1 コマンドに並べず、パターンごとに `find` / `git ls-files` で別々に確認する」の対にする。(b) 太字を落とす。

### [🔵 Suggestion] 昇格先トリアージの見立てがルール文に書かれていない

**該当箇所**: `kasane/lessons/inbox/absence-fallback-masks-failed-search-command.md:14-20`

**問題点**: ksn-core `references/lessons.md` の昇格トリアージは 1 番目に「機械的に検査できる → lint / hook / CI へ (文章ルールにしない。lessons には置かない)」を置く。本パターンの表層 (`|| echo` / `|| true` を伴う不在確認、複数 glob の併記) は Bash の PreToolUse hook で機械検出できる余地があり、判断が要るのは「不在を断定してよいか」の側だけである。同型の先例 `kasane/lessons/inbox/command-substitution-exit-status-discarded-in-condition.md` は「昇格時は lessons ではなく lint へ流す候補」と自分で書いており、本ファイルには対応する見立てが無い。昇格時に再導出する手間になる。

**推奨修正**: 事後判定の後に 1 文足す (「昇格時は、フォールバックを伴う不在確認の検出は hook / lint 側、不在を断定してよいかの判断は process ルール側、と分ける候補」)。なお exploration 論点 4 の「lint / 規約はハーネス本体側」の裁定とは矛盾しない (見立ての記録であって本変更での実装ではない)。

### [🔵 Suggestion] android/0019 の `現行照合` がテストファイルを basename でしか示していない

**該当箇所**: `kasane/decisions/android/0019-datepickercell-calendar-compose-datepicker.md:39`

**問題点**: 製品コードは `android/kssettingsview/src/main/kotlin/.../DateCalendarDialog.kt` とリポジトリ相対で書かれているのに、テスト 2 本は `DateCalendarDialogTest.kt` / `DateCalendarRecreationTest.kt` の basename のみ。ksn-core `references/paths.md` は「コード・テストはリポジトリルートからの相対パス」と定め、ADR の参照行もこの表に従うとしている。`現行照合` は ksn-drift のディープ検証が突き合わせる接地点なので、パスが解決する形のほうがよい (テストは `src/test/` 配下で、製品コードのパスからは導けない)。

**推奨修正**: 「同ディレクトリ (`android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/`) のテスト 2 本」のように、ルート相対で辿れる形にする。

### [🔵 Suggestion] deviation.md:19 が「本変更では直さない所見」ではなくなっている

**該当箇所**: `deviation.md:17-19`

**問題点**: 節の見出しは「記録した所見 (本変更では直さない)」で、他の 3 項 (旧形式 frontmatter・archive 媒体 5 件・構造 lint の既存違反) はいずれも「見つけたが手を付けない課題」である。訂正後の 19 行目は「追跡先は実在し、追跡の経路は ADR-0026 側に残る」という**確認できた事実**で、直す対象ではない。maui/0015 から追記行を落としてよい根拠なので、記録する価値はある。

**推奨修正**: 「決定事項との差」の maui/0015 の項 (deviation.md:8) に根拠として畳むか、「確認した事実」の見出しを別に立てる。

## review-002 の Suggestion 5 件の見送り評価

| Suggestion | 対応 | 見送りの妥当性 |
|---|---|---|
| `USER_MANAGED` がリポジトリのどこにも現れない | 未対応 | **見送りが正しい (指摘の前提が誤り)**。`git grep USER_MANAGED` は 4 件返り、`kasane/roadmaps/package-distribution/phases/phase-8-release-workflow/agenda.md:12` と `kasane/changes/archive/2026-09-04-add-release-workflow/design.md:16` が plugin のログ 1 行 `Uploaded bundle to Central Portal as USER_MANAGED, deployment id: <uuid>` を引用している。この語は plugin が実際に出す文字列で、`release.yml:640` の抽出正規表現が読む行の一部でもあり、宙に浮いていない |
| 「外へ書き込む step」の宣言に run 内の保存が混ざる | 未対応 | 妥当。宣言と 9 項目の齟齬は読めば分かる範囲で、順序の主題 (取り消せる順) は正しく伝わる |
| 段の図が実際の job 依存より強い順序を示す | 未対応 | **取るべき**。`release.yml:388,397,406` の `consumer-*` は `needs: [validate, package-*]` で本体検証に依存せず、合流は `publish` (`:416`) で起きる。図の `本体検証 ∥ 配布物の生成 → 消費者検証 (dry-run)` はこの依存より強い順序を示しており、他 4 件と違って**事実のずれ**である。この文書は「publish の前後にどの段が並ぶか」を冒頭で読者に約束しているので、1 文添えるだけで解消する |
| 「消費者検証 (dry-run)」と workflow 入力 `dry-run` の語の衝突 | 未対応 | 妥当だが価値は高い。同名の別概念が 1 本の workflow に同居している事実は本文から読めないままで、括弧書き 1 つで解消する |
| footer のキー順が揃っていない (`整理`→`出典` が cross/0021:43・0023:52・0028:59 の 3 本) | 未対応 | 妥当。`整理` はこの変更限りのプロジェクト独自キーで、蒸留後に機械で拾う予定も無い |

review-001 からの持ち越し (`関連:` 行、cross/0021 のスクリプト名、rules.md のカテゴリ説明) も未対応で、いずれも見送りは妥当。「初見可読性レビューの証跡」だけは log.md:424 が「初見可読性レビューの指摘 (段の名前が未定義・publish 段の内部順序が本文にない・公開確認が 1 チャネルのみ・宙に浮いた語) を受けて全面的に書き直した」と長命層に残したことで、実質的に応答済みと読める。

## 確認した観点 (指摘なし)

- **足場凍結**: `exploration.md` は 17:44 から未変更。本 round の編集は `release-pipeline.md` / `android/0019` / `deviation.md` / `log.md` / 新規 lessons の 5 ファイルのみ (mtime で確認) で、ADR 7 本と `repository-boundaries.md` は round-2 から動いていない
- **決定内容の不変性**: 8 本の Context / Decision / Alternatives Considered は本 round で 1 行も変わっていない。round-3 の ADR 編集は android/0019 の footer 1 行のみ
- **追記マーカーの残存**: `decisions/` 96 本を再走査 (frontmatter と footer を除き、同一行に日付と「追記」がある行) して 0 件。log.md の「22 行 → 0 行」は正しい
- **lint**: `doc-structure-lint.py` は `release-pipeline.md` / `repository-boundaries.md` に指摘なし (`log.md` / `index.md` はスクリプトの `SKIP_NAMES` で対象外。目次・履歴として設計どおり)。`local-path-lint.py` / `identity-lint.py` は本 round の 5 ファイル全件で指摘なし
- **ビルド・テスト**: コード変更ゼロ (`git status` に `.kt` / `.cs` / `.swift` / ビルドファイルは 1 件も現れない) のため実行対象なし
- **新規 lessons の形式**: frontmatter (scope: process / kind: pain / severity: normal / count: 1 / first-seen / last-seen / evidence) は ksn-core `references/lessons.md` の inbox テンプレートに沿う。`## 関連` の追加と `[[slug]]` 表記はどちらも既存 inbox で確立済みの書き方 (`adjacent-change-state-judged-from-stale-worktree.md` ほか 9 本)。count 1 と match-or-create の判断も妥当 — 参照している 2 パターンとは機序が違う (L-006 = 検索を通していない、stale-worktree = 検索対象が古い、本件 = 検索コマンドが走っていない) ため、count 加算ではなく新規作成が正しい
- **rule-style 1・2・4**: 意味は 1 文で閉じており、発火条件 (「不在を断定するための検索」) は観測可能な事実、事後判定の文が到達状態を与えている。外している基準は上記 Minor / Suggestion に挙げた 3・5・6 と環境非依存の要求のみ
- **`release-pipeline.md` の未変更部分**: 段の役割表・version 注入表・各チャネルの publish 機構・してはいけないこと 3 項は review-002 が実物照合済みで、本 round で触れていない。今回あらためて `release.yml:1-11` の冒頭コメント (段の並びと publish の内部順序) と突き合わせ、齟齬が無いことを確認した

## アクションプラン

1. deviation.md の付随修正に新規 lessons の 1 行を足し、log.md:424 の「捕捉済み 2 件」を 3 件へ直す (Minor・L-009 事後判定 / 長命層の件数誤り)
2. 新規 lessons のルール文から zsh 前提を外し、機序の 1 文 (「同じ出力を出す」) を実際の再現に合わせる (Minor・rule-style の環境非依存)
3. `release-pipeline.md` の段の図に、`consumer-*` が配布物の生成だけを待ち publish が合流点である旨を 1 文添える (review-002 Suggestion のうち唯一の事実ずれ)
4. 新規 lessons の残り 3 点 (事後判定の判定材料・純禁止と太字・昇格トリアージの見立て) と android/0019 の テストパス、deviation.md:19 の置き場所は任意
5. review-002 の他の Suggestion (`USER_MANAGED`・「外へ書き込む step」・`dry-run` の語・footer キー順) は見送りで差し支えない。とくに `USER_MANAGED` は指摘の前提自体が誤りだったため、直さないのが正しい
