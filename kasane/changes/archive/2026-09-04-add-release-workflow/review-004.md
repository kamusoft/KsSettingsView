# レビュー結果: add-release-workflow (004 回目)

**日付**: 2026-09-04
**判定**: CHANGES_REQUESTED

## サマリー

validate の step 入れ替え (「Verify distribution repository tag」を「Verify README install examples」の前へ) 自体は妥当で、依存関係の破壊も YAML / bash の構文問題も無く、tasks 4.1 が定める順序に実装が揃った。証跡 8〜10 節の記述も GitHub 上の実測 (run 33832694159 / 33832493118、Environment・secrets・deploy key・配信リポジトリの tag と commit) と突き合わせてすべて一致し、虚偽は見つからなかった。

一方で、証跡 9 節が Central Portal の deployment ID を生値のまま載せているため、必須 status check である CI の lint job が本 branch の HEAD (b71016e) で **失敗している** (run 33832827993 / step 7 `Identity lint`)。この状態では develop → main のリリース PR がマージできないため CHANGES_REQUESTED とする。修正は sanitize 1 回で閉じる。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 移動した step のコメントは移動していないため新規のコメント追加は無し。既存コメント (「不可逆な操作に入る前にここで見つける」) は移動後も内容が成立している
- `kasane/handbook/cross/release-procedure.md` (適用のきっかけ: リリースを行うとき・release workflow の secrets / Environment を設定するとき) — 6.2 / 6.3 の証跡が手順書の記載と対応するかの確認に使用
- ksn-core `references/evidence.md` (証跡を残すとき) — 生ログの sanitize 規律。**指摘 1 の根拠**
- `kasane/lessons/code-review.md` の重点観点 L-001 (ミューテーション実測) は本 diff (workflow の step 順のみ) に該当なし。`kasane/lessons/process.md` L-007 (成果物が実態を反映しているか) を tasks のチェックの検証軸に採用
- `kasane/config.yaml` の `skills.code-review` / `domain-skills.*.code-review` — 本 diff はどの platform ドメインにも属さない CI 定義のため、ドメインスキルの適用対象外

## 検証したこと (実測)

| 観点 | 手段 | 結果 |
|---|---|---|
| YAML の妥当性 | `yaml.safe_load` で全体を parse し validate job の step 列を出力 | OK。step は Validate inputs → Checkout → Verify monorepo tag → Verify distribution repository tag → Verify README install examples → Export artifact names |
| bash の構文 | 全 job の `run` ブロック 27 本を `bash -n` | 構文エラー 0 件 |
| step 間の依存 | 移動した 2 step の env / outputs / cwd / 生成物を照合 | 依存なし。どちらも `KS_VERSION` のみを step env で受け、`id` を持たず、workspace の cwd 前提のみ。`Export artifact names` が読む `steps.names` 系の入力にも影響しない |
| 作業ツリーの副作用 | `scripts/release/check-distribution-tag.sh` と `scripts/spm-snapshot/sync-snapshot.sh` を読解 | 破壊的操作 (`rm -rf` / `git add -A` / `git fetch`) はすべて `${RUNNER_TEMP}/spm-validate` の作業コピーに閉じており、monorepo の workspace は read-only。後段の README 検査が読む `README.md` / `README_ja.md` は影響を受けない |
| Scenario「配信リポジトリの同名 tag は publish の前に内容で判定する」 | 実行順と job 依存 | 引き続き充足。検査は validate job 内にあり、`package-*` / `publish` はすべて `needs: validate`。むしろ入れ替えにより、README 不一致と同時発生したケースでも検査に到達するようになった |
| Scenario「README の version が一致しなければ失敗する」 | 実行順と run 33832694159 の出力 | 引き続き充足。step 6 が失敗し 8 件の不一致 (ファイル + 行番号) を `::error::` で出力している |
| 証跡 10 節と実測の一致 | `gh run view 33832694159` / `33832493118` | 完全一致。33832694159 は 4 success `Verify monorepo tag` / 5 success `Verify distribution repository tag` / 6 failure `Verify README install examples`、入力は version 9.9.8・dry-run true・ref `refs/heads/chore/release-prep`。step 5 の出力は「配信リポジトリの tag 9.9.8 は今回のスナップショットと同一」→「publish の tag 作成は skip される」、step 6 は「README.md:44: ... version が 9.9.8 でない (実際は 0.1.0)」。旧順序の 33832493118 も 5 が README 失敗・6 が配信リポジトリ tag skipped で、証跡の経緯記述どおり |
| 証跡 8 節と実測の一致 | `gh secret list --env release` / `gh api .../environments/release` / `.../deployment-branch-policies` / `gh api .../KsSettingsView-SPM/keys` / `gh secret list` (repo) | 完全一致。secrets 7 件は列挙どおり、branch policy は `main` の custom policy、deploy key は `read_only=false`、リポジトリ単位の secrets は 0 件 |
| 証跡 10 節の配信リポジトリの前提 | `gh api .../KsSettingsView-SPM/commits` | 一致。commit は `8661e38 Initial snapshot from kamusoft/KsSettingsView` の 1 件のみ |
| tasks のチェックの誠実さ | 6.2 / 6.3 / 1.1 を証跡と実測で照合 | 6.2 / 6.3 は充足。1.1 は 1 節が未実測のまま `[x]` (指摘 2) |
| lint | `scripts/{local-path,identity,comment-policy,doc-structure}-lint.py` | identity-lint が exit 1 (指摘 1)。他は本 change 由来の違反なし (doc-structure の警告は roadmaps 側で既存) |
| CI | `gh run view 33832827993` (HEAD b71016e の pull_request 実行) | lint job **failure** (step 7 `Identity lint`)。以降の step は skipped |

## 指摘事項

### [🔴 Critical] 証跡の deployment ID が sanitize されておらず、必須 status check の lint job が落ちている

**該当箇所**: `evidence/github-actions-runs.txt:87,93,95,96,101`

**問題点**: 9 節に Central Portal の deployment ID (`<DEPLOYMENT_ID>`) が生値で 5 箇所入っている。`scripts/identity-lint.py` の `uuid` 検出に当たり、手元で exit 1 になる (`.claude/settings.json:29` の hook にも同じ検査が登録されている)。これは机上の懸念ではなく実害が出ている — CI の lint job は `ci.yml:148-149` で `python3 scripts/identity-lint.py` を必須ステップとして実行しており、本 branch の HEAD (b71016e) に対する run 33832827993 の lint job が step 7 `Identity lint` で failure、後続 6 step が skipped になっている。`lint` は main の branch protection の必須 7 check の 1 つなので、この状態のままではリリース PR (develop → main、tasks 7.2) をマージできない。ksn-core `references/evidence.md` の「生ログは `scripts/log-sanitize.py` を通してから抜粋する」規律にも反する (9 節の追記だけがこの手順を経ていない — 1〜7 節および同ディレクトリの他の証跡は `<TEMP>` 等のプレースホルダに置換済み)。

**推奨修正**: `python3 scripts/log-sanitize.py kasane/changes/add-release-workflow/evidence/github-actions-runs.txt` を通して 5 箇所を `<uuid>` へ置換し、`python3 scripts/identity-lint.py` が exit 0 になることを確認する。9 節の主張 (同一の ID が upload → status → drop → status を貫くこと) はプレースホルダが全箇所同じ語になるので保たれる。念のため 9 節の地の文に「同一の deployment に対する一連の操作である」旨を 1 語添えると、置換後も読み手が取り違えない。`lint.identity.allow` への追加は、drop 済みの一過性 ID を恒久的な許可値として残すことになるので採らないこと。

### [🟡 Minor] tasks 1.1 の 1 節が未実測のまま `[x]` になっている

**該当箇所**: `tasks.md:5` / `evidence/github-actions-runs.txt:97-99`

**問題点**: tasks 1.1 は 5 つの確認事項を持ち、うち「PUBLISHING / PUBLISHED の deployment に `DELETE` が拒否されることも確認する」だけが実接続で未確認である。証跡 9 節はこの点を正直に書いている (「実 deployment を publish しないと踏めないため実接続では未確認。script 側は状態を照会して VALIDATED / FAILED 以外では DELETE を送らない — selftest で担保」) が、tasks.md 側は注記なしの `[x]` で、deviation.md にも項目が無い。tasks.md だけを見た読み手 (アーカイブ後の参照者・次のレビュアー・相方) は 5 節すべてが実測済みと読む。process L-007 が繰り返し指している「成果物と実態のずれ」がここに残っている。実害の面では、`central-portal.sh` が VALIDATED / FAILED 以外へ DELETE を送らないことで spec「PUBLISHING / PUBLISHED の deployment は drop しない」は script 側で閉じているため、リスクは限定的。

**推奨修正**: deviation.md に 1 行足す (例: 「tasks 1.1 (PUBLISHING / PUBLISHED への DELETE 拒否): 実 deployment を publish せずには踏めないため実接続では未確認。script 側で VALIDATED / FAILED 以外へ DELETE を送らないことを selftest で担保し、この 1 節のみ未実測のまま 1.1 を完了とした」)。あるいは tasks 1.1 の行末に同趣旨の注記を添える。どちらでも足りるが、証跡ファイルの中だけに閉じている状態は解消すること。

### [🔵 Suggestion] 証跡 6 節の「未実施」が 8 節で覆っていることが 6 節から辿れない

**該当箇所**: `evidence/github-actions-runs.txt:61`

**問題点**: 6 節末尾の「secrets 7 件の登録はオーナー作業 (未実施)」は 8 節で解消済みだが、6 節を読んだ時点では最新状態が分からない。節が日付順に積まれる形式なので致命的ではないものの、アーカイブ後にこのファイルだけを参照する読み手には誤読の余地が残る。

**推奨修正**: 6 節の当該行に「→ 8 節で登録済み」を添える。

### [🔵 Suggestion] 配信リポジトリの一時 tag `9.9.8` がレビュー時点で残っている

**該当箇所**: `evidence/github-actions-runs.txt:120`

**問題点**: `gh api repos/kamusoft/KsSettingsView-SPM/tags` で確認したところ、10 節の実測で使った一時 tag `9.9.8` が現存する。削除は tasks 5.1 の「tag は削除」に含まれており 5.1 は未チェックなので追跡自体は生きているが、配信リポジトリが利用者から解決される段になると、SwiftPM の `from:` 系要件がこの tag を最新版として選ぶ。README のインストール例が `exact:` へ揃うのは tasks 7.2 の後なので、初回リリースまでに確実に消えている必要がある。

**推奨修正**: tasks 7.2 / 7.3 に着手する前に削除を済ませる (オーナー作業)。tasks 5.1 の該当箇所に「配信リポジトリの一時 tag 9.9.8 を含む」と明記しておくと取りこぼしにくい。

## 判定の理由

CI の必須 check が現に落ちているため、指摘 1 は blocking。ksn-review の「ビルドが通らない・テストが失敗している場合はそれだけで CHANGES_REQUESTED」に該当する。step 入れ替えそのものと 8〜10 節の事実関係には問題が無く、修正は sanitize 1 回と注記 2〜3 行で閉じるので、再レビューは軽い。

## アクションプラン

1. `scripts/log-sanitize.py` で証跡 9 節の deployment ID を置換し、`identity-lint.py` が通ることと CI の lint job が緑になることを確認する (指摘 1、blocking)
2. tasks 1.1 の未実測 1 節を deviation.md か tasks の注記として残す (指摘 2)
3. 証跡 6 節に 8 節への参照を足す (指摘 3)
4. 初回リリース着手前に配信リポジトリの一時 tag `9.9.8` を削除する (指摘 4、tasks 5.1 の範囲)
