# レビュー結果: restore-adr-decision-records (004 回目)

**日付**: 2026-09-07
**判定**: CHANGES_REQUESTED

**スコープを絞った確認レビュー**。全体の再点検は行っていない。見た範囲は review-003 の指摘への対応 3 件 (Minor 2 件 / Suggestion 1 件) と、review-003 が「前提が誤り」として見送った `USER_MANAGED` の 1 件のみ。review-002 への対応 6 件は review-003 が独立検証済みのため再検証していない。ADR 8 本・新概念の本体・移送元は本 round の編集対象外であることだけ mtime で確認した。

## サマリー

3 件の対応はいずれも**方向として正しく、成果物は前 round より正確になっている**。とくに段の図は `.github/workflows/release.yml` の `needs` と厳密に一致する形に書き直され、review-002 から 2 round 残っていた唯一の事実ずれが解消した。

ただし 3 件のうち 2 件は**同じ round の中で再び記録の追随が漏れている**。lessons の 3 本目を deviation.md に足したが、同じ change のコミットが追加した lessons 2 本は依然として現れず、deviation.md から読める捕捉件数 (1 件) と log.md が書く件数 (3 件) が食い違う。図を書き直して `release-pipeline.md` が 247 字増えたのに、直前の行で log.md が書く字数 (5058 字) は据え置かれている。さらに書き直したルール文が `grep -rl` を「マッチ 0 件でも正常終了する手段」に数えているが、`grep` は 0 件で exit 1 を返すため、この例示は事実として誤りである。いずれも 1 行で閉じる修正で、設計判断は要らない。

## 照合した規約

- `kasane/handbook/index.md` — 本 round の編集は `kasane/` 配下の Markdown 4 ファイルのみで、コード・テスト・ビルドファイル・利用者向け Skill に触れないため、cross / ios / maui のいずれの「適用のきっかけ」にも当たらない (`always` 指定の文書も無し)。該当 rule なし
- ksn-core `references/rule-style.md` (6 基準 + 環境非依存の要求。指摘 2 の評価軸として user から明示指定)
- ksn-core `references/lessons.md` (inbox 形式・昇格トリアージ)、`references/paths.md`
- `kasane/lessons/process.md` L-009 (合意済みスコープ外のファイルは deviation.md へ 1 行。事後判定を実際に当てた)
- `kasane/lessons/code-review.md` — 重点観点 L-001 (ミューテーション実測) はコード変更ゼロのため非該当。「指摘しないこと」は昇格済みルール無し

## 本 round の編集範囲 (確認)

review-003 の出力時刻 (19:20:38) 以降に mtime が動いたのは主張どおりの 4 ファイルだけだった。

| ファイル | mtime | 主張との一致 |
|---|---|---|
| `changes/restore-adr-decision-records/deviation.md` | 19:21:50 | 一致 (指摘 1 への対応) |
| `kasane/concepts/log.md` | 19:21:51 | 一致 (指摘 1 への対応) |
| `kasane/concepts/cross/architecture/release-pipeline.md` | 19:21:51 | 一致 (指摘 3 への対応) |
| `kasane/lessons/inbox/absence-fallback-masks-failed-search-command.md` | 19:22:08 | 一致 (指摘 2 への対応) |

足場 (`exploration.md` 17:44:46) は凍結を維持。ADR 8 本 (最終 19:08:42) と `repository-boundaries.md` (18:39:52)・`cross/index.md` (18:56:53) は本 round で動いていない。`local-path-lint.py` / `identity-lint.py` は本 round の 4 ファイル全件で指摘なし、`doc-structure-lint.py` は `release-pipeline.md` に違反 0 (log.md の「構造 lint 違反 0」と一致)。コード変更ゼロのためビルド・テストは実行対象なし。

## 対応 1: lessons 3 本目の記録 — 事実としては正しいが、L-009 の事後判定は依然として未達

deviation.md の付随修正に 1 行が足され、log.md の記述も「捕捉 3 件」に直っている。どちらも**書かれている内容自体は正しい** — inbox の 3 本は実在し、slug も一致する。しかし L-009 の事後判定を機械的に当てると通らない (下記 Minor 1)。

## 対応 2: ルール文の書き直し — rule-style の 6 基準に照らした評価

| 基準 | 評価 | 根拠 |
|---|---|---|
| 環境非依存 (冒頭の要求) | **満たす** | zsh の `nomatch` を根拠に置く形が消え、「不一致 glob の扱いはシェルによって違い (コマンドごと中断する / リテラルのまま渡す)、同じコマンドが環境をまたいで同じに働かない」に置き換わった。これは zsh / bash 双方で成立し、どちらの環境で読んでも「自分には当たらない」と読めない |
| review-003 が指摘した「両方で同じ出力を出す」の不正確さ | **解消している** | 現文は「フォールバックは…両方で発火し、**成果物に残る**のは同じ「なし」だけなので、後から区別できない」。主語が端末出力から成果物 (報告・記録) へ移り、bash で部分マッチ分が stdout に出る事実と衝突しなくなった。rule-style 5 が言う成果物の語法とも整合する |
| 1. 意味が閉じている | 満たす | 2 段落とも他文書を読まずに解釈できる。`## 関連` の L-006 参照は補足であってルールの解釈に必須ではない |
| 2. 発火条件が観測可能 | 満たす | 「「〜は存在しない」を報告・記録に書くときは」は観測可能な事実 |
| 3. 禁止は代わりの動きと対 | 概ね満たす | review-003 が純禁止と指摘した「複数パターンを 1 コマンドに並べない」は「1 コマンドに並べず、パターンごとに実行する」の対に畳まれた。「フォールバック形は使わない」の代わりの動きは第 1 段落が持つ |
| 4. 到達状態で書く | 満たす | 事後判定の 1 文が到達状態を与える |
| 5. 事後判定可能 | **解消している** | 「不在を断定した報告・記録に、使った検索手段のコマンドか、その 0 件出力が併記されている」— 判定材料が会話ログから成果物へ移り、review-003 の Suggestion が解けた |
| 6. 強調は落とせないものだけ | **満たしていない** | 下記 Suggestion 1 |

加えて、書き直しで**新しい事実誤りが 1 件入った** (下記 Minor 3)。

## 対応 3: 段の図 — `needs` と一致し、説明文とも矛盾しない

`.github/workflows/release.yml` の実際の依存は次のとおり。

| job | `needs` | 行 |
|---|---|---|
| `ios` / `android` / `maui` (本体検証) | `validate` | `:170,175,180` |
| `package-ios` / `package-android` / `package-maui` | `validate` | `:188,224,289` |
| `consumer-ios` / `consumer-android` / `consumer-maui` | `[validate, package-<platform>]` | `:388,397,406` |
| `publish` | `[validate, ios, android, maui, consumer-ios, consumer-android, consumer-maui]` | `:416` |
| `wait-for-registries` | `publish` (`:889`) / `smoke-*` | `wait-for-registries` (`:908,917,926`) |

書き直された図 (`release-pipeline.md:19-23`) は `validate` から 2 本の枝が独立に伸び、`publish` で合流する形になっており、この依存と一致する。`consumer-*` が本体検証を待たないという review-002 由来の事実ずれは解消した。図の直後の 1 文 (`:25`「本体検証と「配布物の生成 → 消費者検証 (dry-run)」は互いに依存せず並列に走り、publish は両方の成功を待つ」) も図と `needs` の双方に一致する。`:37`「publish より前の段がすべて成功したときにだけ配信先へ書き込む」も `publish` の `needs` が先行 7 job を全部挙げている事実どおり。

素の Markdown での可読性は問題ない — 図は ``` フェンスの中にあり、整形済みテキストとして表示される。ライブラリも画像も要らない。ただし枠線の桁が 1 つずれている (下記 Suggestion 2)。

なお `consumer-ios` が待つのは `package-ios` だけで `package-android` は待たない (platform ごとに独立) が、この文書は段の粒度で書くと冒頭で宣言しており、段の表も段単位なので、図が 1 レーンに畳んでいることは事実のずれではない。

## 指摘事項

### [🟡 Minor] L-009 の事後判定が依然として通らない — 同じ change が追加した lessons 2 本が deviation.md に無い

**該当箇所**: `deviation.md` (付随修正の節) / `kasane/concepts/log.md:424`

**問題点**: L-009 の事後判定は「`git show --stat` に現れるファイルのうち合意済みスコープが名指ししていないものが、すべて deviation.md の行に箇所として現れている」。これを本 change の全コミットに当てると、まだ通らない。

本 change のコミットは `8266087` (「ADR の変更ログ化の是正を探索する」) の 1 本で、その `--stat` は 3 ファイル — `exploration.md` と `kasane/lessons/inbox/project-adr-carves-exception-into-harness-rule.md` と `kasane/lessons/inbox/implementation-results-accreted-into-adr-consequences.md` である。`exploration.md` の「実行」節は触る対象を 10 ファイル (ADR 8 本 + `release-pipeline.md` + `repository-boundaries.md`) と数え、lessons ファイルを 1 本も名指ししていない。にもかかわらず deviation.md が箇所として挙げる lessons は `absence-fallback-masks-failed-search-command.md` の 1 本だけで、コミット済みの 2 本は現れない。

つまり本 round の対応は、review-003 が名指しした 3 本目にだけ適用され、同じ性質の 2 本には及んでいない。これは review-003 自身が「指摘された 2 ファイルにだけ適用され、ルールとしては効いていない」と診断した型の 3 度目である。

実害は件数の食い違いとして現れる: deviation.md だけを読むと本 change の lessons 捕捉は 1 件に見えるが、log.md:424 は「捕捉 3 件」と書く。蒸留時にどちらを正とするかが決まらない。

なお読み方は 2 通りあり、どちらを採っても現状は揃っていない。

- **ファイル名で読む** (`exploration.md` が名指しした 10 ファイル以外はすべて記録対象): 3 本とも deviation.md に要る。現状は 1 本
- **範囲で読む** (決定事項の「再発防止はプロジェクト側では lessons のみ」が `lessons/` を範囲として名指ししている): 3 本とも不要。現状は 1 本だけ余分

**推奨修正**: どちらかに揃える。前者なら既存の 1 行を 3 本まとめた形に書き換える (例: `- [付随修正] kasane/lessons/inbox/ の 3 本 (project-adr-carves-exception-into-harness-rule / implementation-results-accreted-into-adr-consequences / absence-fallback-masks-failed-search-command): 本変更の作業中に踏んだ観測を捕捉。…`)。後者なら 1 行を落とし、「決定事項が lessons/ を範囲として挙げているため個別記録は不要」と 1 行残す。前者を推す — log.md が既に 3 本を列挙しており、揃えるコストが低い。

### [🟡 Minor] `release-pipeline.md` を書き直したのに log.md の字数が追随していない

**該当箇所**: `kasane/concepts/log.md:424`

**問題点**: log.md の concept-work エントリは新概念を「5058 字、構造 lint 違反 0」と書く。本 round で段の図と説明文を書き直した結果、`release-pipeline.md` の現在の字数は **5305 字**で、247 字 (約 5%) ずれている。

字数の数え方はこのプロジェクトの既存記述から一意に決まる。同じエントリが `repository-boundaries.md` を「9256 → 8910 字」と書いており、現物の**空白を除いた全文字数**が 8910 でぴったり一致する (`doc-structure-lint.py --stats` が出す「散文」は同ファイルで 7947 で、こちらではない)。同じ metric を `release-pipeline.md` に当てると 5305。

review-003 の時点では現物は 5030 字前後で、5058 は妥当と判定されていた。本 round の書き直しで増えた分が log.md へ返っていない。指摘 1 と同じ「編集した長命層の記述が、直前に書いた log へ戻らない」型であり、この change が是正しようとしている「長命層に古い事実が残る」そのものである。

**推奨修正**: `5058 字` を `5305 字` に直す。あわせて、この round で図を書き直したこと (段の依存関係を `needs` に合わせた) をエントリに 1 句足すと、初見可読性レビュー由来の書き直しと区別がつく。

### [🟡 Minor] 書き直したルール文が `grep -rl` を「マッチ 0 件でも正常終了する手段」に数えている

**該当箇所**: `kasane/lessons/inbox/absence-fallback-masks-failed-search-command.md` (ルール文 第 1 段落)

**問題点**: 現文は「`find <root> -name '<pattern>'` / `git ls-files '<pattern>'` / `grep -rl` は**いずれもこの性質を持ち**、0 件であることが出力の空で分かる」と書く。「この性質」は直前の「マッチ 0 件でも正常終了する手段」を指す。しかし `grep` はマッチ 0 件のとき **exit 1** を返す (実測: `grep -rl '<存在しない語>' kasane/` → exit 1。同条件で `find` は exit 0、`git ls-files` は exit 0)。3 つのうち `grep -rl` だけがこの性質を持たない。

これは書き直しで入った新しい事実誤りで、しかもルールの中心に据えた性質そのものについての誤りである。害は 2 通りに出る。

1. このルールの眼目は「0 件」と「コマンドが失敗した」を終了ステータスで区別することなのに、その例示に区別できない前提のコマンドが混ざる。`grep` は 0 件を 1、エラーを 2 で返すので区別自体は可能だが、「正常終了する」という言明のままでは `set -e` 下や `&&` 連結で 0 件が失敗として扱われ、逆向きの事故を生む
2. inbox のルール文は「昇格時にそのまま転記できる」形で書く規約 (ksn-core `references/lessons.md`) なので、この誤りは昇格時に `lessons/process.md` へそのまま入る

**推奨修正**: `grep -rl` を例示から外すか、性質の違いを明示する (例: 「`find <root> -name '<pattern>'` と `git ls-files '<pattern>'` は 0 件でも exit 0 で空の出力を返す。`grep -rl` は 0 件で exit 1 を返すため、1 (0 件) と 2 (エラー) を区別したうえで使う」)。

### [🔵 Suggestion] rule-style 6 (強調) が未対応で、太字は 2 箇所から 3 箇所に増えている

**該当箇所**: `kasane/lessons/inbox/absence-fallback-masks-failed-search-command.md` (ルール文・`## 関連`)

**問題点**: rule-style 6 は「太字や「必ず」「禁止」は承認ゲート・独立レビュー・足場凍結・機密の級に限る」と定める。書き直し後のルール文の太字は `マッチ 0 件でも正常終了する手段` / `対象が無いとき` / `検索自体が失敗したとき` の 3 箇所で、`## 関連` にも `シェル版` の 1 箇所がある。いずれも該当級ではない。review-003 は書き直し前を 2 箇所として Suggestion に挙げており、対応の結果として 1 箇所増えた形になっている。review-003 のアクションプランでこの項は「任意」に置かれていたため未対応自体は方針どおりだが、増えている点は意図した結果ではないと思われる。

**推奨修正**: 4 箇所とも太字を落とす。強調を使わなくても、第 1 段落の「マッチ 0 件でも正常終了する手段」は文の主語の位置にあるため読み落とされない。

### [🔵 Suggestion] 段の図の右下の角が 1 桁ずれている

**該当箇所**: `kasane/concepts/cross/architecture/release-pipeline.md:20-22`

**問題点**: 等幅 (罫線素片を 1 桁幅) で描画したときの桁位置は、左の角が 3 行とも 11 桁目で揃う一方、右は `┐` が 50 桁目、`├` が 50 桁目、`┘` が **51 桁目**になる。下段だけ 1 桁はみ出す。読解の妨げにはならない (枝の対応関係は読み取れる) が、直すのは末尾の `─` を 1 つ落とすだけで済む。

なお全角の日本語と罫線素片を混ぜている以上、罫線素片を 2 桁幅で描くフォント環境では枠の整列はどのみち保てない。整列を厳密に要求するなら罫線をやめて `validate → (本体検証 ∥ 配布物の生成 → 消費者検証 (dry-run)) → publish → …` のような 1 行表記にする手もあるが、現在の図のほうが「2 本の枝が独立している」ことは伝わるので、書き換えは薦めない。

**推奨修正**: `:22` 末尾の `─` を 1 つ削る。

## `USER_MANAGED` の見送りの妥当性

**見送りは妥当**。理由は 2 つあり、どちらか一方でも成り立つ。

1. review-002 の指摘の前提が事実として誤り。`git grep USER_MANAGED` は 4 件返る — `kasane/roadmaps/package-distribution/phases/phase-8-release-workflow/agenda.md:12`、`kasane/changes/archive/2026-09-04-add-release-workflow/design.md:16`、同 `evidence/github-actions-runs.txt:87,165`。いずれも plugin が実際に出すログ 1 行 `Uploaded bundle to Central Portal as USER_MANAGED, deployment id: <uuid>` を引用している。review-003 の結論のとおり
2. より強い理由として、`release-pipeline.md:79` はこの語を単独で放り出しておらず、`自動 release なし (\`USER_MANAGED\`)` と**同じ括弧の中で言い換えている**。読み手は他の文書を引かずに意味を取れるので、rule-style 1 に相当する「意味が閉じている」条件を文自体が満たしている

補足として、1 の 4 件はいずれも `roadmaps/` と `changes/archive/` にあり、長命層 (concepts / handbook / decisions) には無い。もし将来この語の由来を長命層で辿れるようにしたくなったら、`release-pipeline.md:79` に「plugin のログに出る語」と 1 句足すのが最小の手当てになる — ただし現状は 2 の理由で不要であり、本 change で直す必要はない。

## 確認した観点 (指摘なし)

- **足場凍結**: `exploration.md` は 17:44:46 から未変更
- **決定内容の不変性**: ADR 8 本は本 round で 1 バイトも動いていない (最終 mtime 19:08:42 < review-003 の 19:20:38)
- **図と本文の整合**: `release-pipeline.md` の段の表 (`:27-35`)・publish 段の 9 ステップ (`:43-51`)・`:37` の 1 文は、書き直した図および `release.yml` の `needs` と矛盾しない
- **lessons の形式**: frontmatter (scope / kind / severity / count 1 / first-seen / last-seen / evidence)・`## 関連`・`## 経緯` は ksn-core `references/lessons.md` の inbox テンプレートに沿ったまま。count 1 と match-or-create の判断は review-003 が検証済みで、本 round で変わっていない
- **lint**: `local-path-lint.py` / `identity-lint.py` は本 round の 4 ファイル全件で指摘なし。`doc-structure-lint.py` は `release-pipeline.md` に違反 0 (`log.md` は `SKIP_NAMES` で対象外)
- **ビルド・テスト**: `git status` に `.kt` / `.swift` / `.cs` / ビルドファイルは 1 件も現れず、実行対象なし
- **昇格トリアージの見立て** (review-003 の Suggestion): ルール文に追記されていない。review-003 のアクションプランで「任意」に置かれた項であり、未対応で差し支えない

## アクションプラン

1. ルール文の `grep -rl` の例示を直す (Minor 3・唯一の事実誤り。昇格時にそのまま転記される場所なので優先)
2. deviation.md の lessons の行を 3 本まとめた形に揃える (Minor 1・L-009 事後判定)
3. log.md:424 の `5058 字` を `5305 字` に直す (Minor 2)
4. ルール文の太字 4 箇所と図の `:22` 末尾の `─` は任意 (Suggestion 2 件)
5. `USER_MANAGED` は直さないのが正しい。review-002 由来の他の Suggestion は本レビューの対象外 (review-003 の評価のまま)
