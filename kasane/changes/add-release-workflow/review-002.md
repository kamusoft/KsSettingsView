# レビュー結果: add-release-workflow (002 回目)

**日付**: 2026-09-03
**判定**: CHANGES_REQUESTED

## サマリー

review-001 の Critical (`maven-metadata*.xml` の比較除外) と Major (drop 済み deployment ID の引き継ぎ) はいずれも塞がっている。Major の実装は推奨と異なり「drop 後に `status` が `NOT_FOUND` のときだけ空 ID を保存し直す」形だが、`central-portal.sh status` が 404 を `NOT_FOUND` として返せるようになったことで、引き継いだ ID が削除済みでも publish 側 (`release.yml:602-607`) が捨てて再 upload へ落ちる二重の受け皿になっており、Scenario 3 つ (失敗時に保留 deployment が残らない / 部分 publish を同じ version で埋める / release の応答が失われても再実行で整合する) をすべて満たす。Minor / Suggestion の 4 件も対応済み。

一方で、修正で新設された `check-distribution-tag.sh` の呼び出し 2 箇所が、シェルの仕様上コマンド置換の終了ステータスを捨てる位置に置かれている。とくに validate 側は**内容の異なる同名 tag があっても job が成功する**ため、デルタスペックの Scenario「配信リポジトリの同名 tag は publish の前に内容で判定する」が成立しない (手元で再現済み)。publish 側の再検査が不可逆な公開の前で止めるので取り消せない被害は出ないが、spec が求める「配布物の生成・publish に入る前に失敗する」位置ではなくなっている。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 新規の shell / workflow / MSBuild コメントに作業文書パス・`Decision N`・change 名・デルタスペック用語の参照なし (機械検査 0 件 / 検査 728 ファイル。本文からも目視で確認)
- `kasane/handbook/cross/test-execution.md` (テストを実行・報告するとき) — MAUI facade の実行件数を併記
- `kasane/handbook/cross/release-procedure.md` (本 change が追加。index の「適用のきっかけ」と本文の整合を確認)
- `kasane/handbook/cross/public-identifiers.md` (配布座標を扱うため) — 変更なし、review-001 の確認から差分なし
- `kasane/handbook/cross/local-development-setup.md` — `verification/maui/build-consumer.sh` の変更が手元実行の手順を変えていないことを確認
- `kasane/handbook/maui/index.md` の 2 文書は今回の diff (workflow / script / 手順書) に当たらないため参照のみ
- lessons: `code-review.md` (L-001)、`process.md` (L-002 / L-006 / L-007)

## 実行した検証

- `dotnet test maui/KsSettingsView.Maui.Tests -c Release`: **合格 516 / 失敗 0 / スキップ 0 / 合計 516**
- `scripts/release/central-portal.sh --selftest`: 42 チェックすべて OK (`NOT_FOUND` 関連の 6 チェックを含む)
- `python3 scripts/release/set-readme-version.py --selftest`: 失敗なし
- `scripts/comment-policy-lint.py` / `local-path-lint.py` / `identity-lint.py` / `doc-structure-lint.py`: 新規・変更ファイルに指摘なし (doc-structure の指摘はいずれも本 change 外の既存 roadmap 文書)
- `bash -n`: `scripts/release/*.sh` 5 本と `verification/maui/build-consumer.sh`
- PyYAML で `release.yml` / `ci.yml` / `.github/release.yml` / `spike-release-premise.yml` を構文解析
- review-001 の Critical の再現ケース (`maven-metadata-local.xml` の `<lastUpdated>` だけが違う 2 ツリー) を `compare-maven-artifacts.sh` に与え、**一致と判定されること**を確認
- 合成した配信リポジトリ (bare origin + 内容の異なる `1.0.0` tag + 作業コピー) に対して `check-distribution-tag.sh` を直接実行 (exit 1) し、続けて `release.yml:134-142` / `538-543` / `722-733` と同型の 3 スニペットを `bash` で実行して終了ステータスを実測 (下の指摘に結果を載せる)
- `secrets.` の参照 13 行がすべて publish job の行範囲に収まること、`secrets: inherit` がコメント以外に無いことを再確認
- iOS / Android の本体テストは未実行 (製品コードに差分なし。review-001 と同じ扱い)

## 指摘事項

### [🟠 Major] validate の配信リポジトリ tag 検査が失敗を握り潰し、内容の異なる同名 tag があっても通過する

**該当箇所**: `.github/workflows/release.yml:138`

**問題点**:

```bash
case "$(scripts/release/check-distribution-tag.sh "${work}" "${KS_VERSION}")" in
  match)
    echo "publish の tag 作成は skip される"
    ;;
esac
```

`case` の対象語の中のコマンド置換は、そのコマンドが 0 以外で終了しても `set -e` の対象にならない (`case` 文自身の終了ステータスは、一致した分岐で最後に実行したコマンド、どこにも一致しなければ 0)。`check-distribution-tag.sh` は内容が異なるとき `::error::` を出して exit 1 するが、その値は捨てられ、step は成功する (`::error::` は注釈であって失敗にはならない)。

合成した配信リポジトリ (origin の tag `1.0.0` が別内容、作業コピーがスナップショット相当) で実測した:

```
=== 直接実行 ===
::error::配信リポジトリの tag 1.0.0 が今回のスナップショットと異なる内容を指している
直接 exit=1

=== validate step (release.yml 134-142 と同型) を bash -e で実行 ===
::error::配信リポジトリの tag 1.0.0 が今回のスナップショットと異なる内容を指している
step の exit=0
```

結果として、デルタスペック (release-workflow) の Scenario「配信リポジトリの同名 tag は publish の前に内容で判定する」の THEN「**配布物の生成・publish に入る前に失敗する**」が成立しない。実行は test / package / dry-run (60 分規模) を丸ごと回し、publish job の `Push snapshot commit` で配信リポジトリへ commit を 1 本押し込んだうえで、`Re-check distribution repository tag` (`release.yml:538`、代入形なので errexit が効く。実測 exit 1) で初めて止まる。不可逆な公開 (Maven upload / NuGet push) の前ではあるので取り消せない被害は出ないが、tasks 4.1 が validate に置くと書いた検査そのものが働いていない。

同じ `case` は `absent` にも `*` にも分岐を持たないため、想定外の出力も黙って通る。

**推奨修正**: `release.yml:538` と同じ代入形にして終了ステータスを errexit に載せる。あわせて `match` / `absent` 以外を明示的に失敗させる (例: `result="$(scripts/release/check-distribution-tag.sh ...)"` の後に `case "${result}" in match) ... ;; absent) ... ;; *) echo "::error::..."; exit 1 ;; esac`)。

### [🟡 Minor] tag 作成直前の再検査も失敗を握り潰し、失敗理由が git の重複エラーにすり替わる

**該当箇所**: `.github/workflows/release.yml:727`

**問題点**:

```bash
if [ "$(scripts/release/check-distribution-tag.sh "${work}" "${KS_VERSION}")" = "match" ]; then
```

`if` の条件部ではコマンド置換の失敗が握り潰される (条件部は errexit の対象外)。内容の異なる同名 tag が publish の後半で現れた場合、script は exit 1 するが、出力が空なので `match` ではないと判定され、`git -C "${work}" tag` へ進む。script が内部で `git fetch --tags --force origin` を済ませているため tag はローカルに存在し、実測では次のようになる:

```
=== Push distribution repository tag step (722-733) ===
::error::配信リポジトリの tag 1.0.0 が今回のスナップショットと異なる内容を指している
fatal: tag '1.0.0' already exists
exit=128
```

step は失敗するので安全側だが、これは Maven Central の release を終えた**取り消せない位置**であり、そこで人が最初に見る行が `fatal: tag ... already exists` になる。判定の意味 (「内容が違う tag が別の手で作られた」) が失われ、`Push monorepo tag` 側の同種の検査 (`release.yml:738-746`、明示的に理由を出して exit 1) と扱いが揃っていない。

**推奨修正**: Major と同じく代入形に直し、`match` / `absent` で分岐させる (`absent` のときだけ `git tag` へ進む)。

### [🔵 Suggestion] upload 成功後・ID 保存前に step が落ちると、保留 deployment を後始末する手がかりが残らない

**該当箇所**: `.github/workflows/release.yml:633-644` / `788-790` / `767-784`

**問題点**: `publishToMavenCentral` は `2>&1 | tee "${log}"` で実行され、`set -o pipefail` 下でこのパイプラインが失敗すると step はその場で終わる。bundle が Portal に受理された後に gradle 側が失敗した場合、deployment は作られているのにログからの ID 抽出 (`release.yml:640`) に到達せず、`steps.maven.outputs.deployment-id` が空のままになる。`Drop pending deployment` は `steps.maven.outputs.deployment-id != ''` を条件にしているため走らず、`Summarize` の「Central deployment」欄も `なし` になる。

窓は狭く (upload はこの gradle 実行の最後尾)、リリース手順書が Portal の deployment 一覧からの手動確認と `central-portal.sh drop` を案内しているので運用では回収できる。ただし ID を持たない状態からの回収になるため、手順書の想定より手数が増える。

**推奨修正**: 失敗経路 (`if: failure()`) で `${RUNNER_TEMP}/central-upload.log` が存在すれば同じ正規表現で ID を拾い直し、拾えたら `Summarize` と `Drop pending deployment` の入力に載せる。あるいは手順書の「失敗したとき」に、ID が出ていない場合の入口 (Portal の一覧から当該 version の deployment を探す) を 1 行足す。

### [🔵 Suggestion] evidence の 8 節が「未実施 / 未検証」のまま残り、9 節および deviation と矛盾する

**該当箇所**: `evidence/premise-spike-pack.txt:181-188`

**問題点**: 8 節の見出しは「未実施 — maui/nuget.config (tasks 3.1)」で、本文も「Scenario『複数ソース環境でも nuget.org だけから取得する』は未検証」と書いている。追記された 9 節 (191-197 行) が同じ Scenario の充足を示し、`deviation.md` の `[付随修正]` も充足済みと記録しているため、証跡だけを読む人には結論が 2 つ並んで見える。証跡は蒸留後もアーカイブに残り、後続の判断材料になる。

**推奨修正**: 8 節の見出しと結論の行に、9 節で解消済みである旨の 1 行を足す (時系列の記録として本文は残してよい)。

## 確認して問題がなかった観点

- **review-001 Critical の解消**: `compare-maven-artifacts.sh:70` の `! -name 'maven-metadata*.xml'` と、除外理由を書いた冒頭コメント (21-23 行)。再現ケースで一致判定になることを実測。除外は spec が挙げる比較対象 (pom / module / aar / sources jar / javadoc jar) を狭めていない。`check-signatures.sh:43-44` の対象も `*.aar` / `*.pom` / `*.jar` / `*.module` に限られており、`maven-metadata-local.xml` に `.asc` を要求しない
- **review-001 Major の解消**: `central-portal.sh` が 404 を `NOT_FOUND` として返し (`178-199`)、`release`・`wait-published`・`drop` の 3 サブコマンドがそれぞれ適切に扱う (`--selftest` の該当 6 チェック)。`release.yml:788-810` が drop 後に `status` を取り直し、`NOT_FOUND` のときだけ空 ID を保存し直す。`Clear stored deployment id` (`814-821`) の `overwrite: true` で artifact が置き換わる。削除できない状態 (PUBLISHING / PENDING / VALIDATING) では ID を残す判断は、次の attempt の `release.yml:570-612` が「検証中なら決着を待つ / PUBLISHING なら公開を待つ / PUBLISHED なら skip」と分岐する形と噛み合っている。仮に artifact に古い ID が残っても `NOT_FOUND` 分岐 (`602-607`) が捨てて再 upload へ落ちるため、Scenario 3 つのいずれの経路でも停止しない
- **second-opinion 採用 2 件**: 配信リポジトリ tag の publish 直前再検査は `Push snapshot commit` の直後・Maven upload の前 (`release.yml:534-543`) に入り、相方の推奨位置と一致 (上の Major / Minor は位置ではなく終了ステータスの扱いの問題)。fork 対策は `ci.yml` の `github.event.pull_request.head.repo.full_name` と `GITHUB_REPOSITORY` の比較で入っており、head 制限 step が `Checkout` より前に置かれているため PR のコードを実行する前に判定される
- **review-001 の残りの Minor / Suggestion**: 手順書 `release-procedure.md:146` の `Re-run all jobs` の理由が artifact の上書き可否と所要時間に書き直され、package 段 3 つの `upload-artifact` に `overwrite: true` が入っている (`release.yml:207` / `272` / `368`) ため記述と実装が一致。snupkg は `evidence/premise-spike-pack.txt` の 10 節で 3 件の生成と pdb の同梱を実測。`release.yml:324-326` の pack 順序コメントは `ProjectReference` を認めたうえで「push の順序と揃える」に直っている。`spike-release-premise.yml` の削除は `deviation.md` の最終項に完了条件として記録
- **deviation の環境記述**: `deviation.md` の `[付随修正]` が「2 ソース環境で NU1507 が 0 件になった」に直り、`evidence/premise-spike-pack.txt` の 9 節が裏づけている
- **AGENTS.md の例外文**: `from:` → `exact:` の正規化を明示したため、script の実装と例外の文言が一致 (second-opinion の Major を降格した理由と整合)
- **`central-portal.sh` の契約変更の波及**: `status` を呼ぶ箇所は `release.yml:575` (代入形、errexit が効く) と `release.yml:801` の 2 箇所のみで、どちらも `NOT_FOUND` を明示的に扱う。手順書 158-162 行が案内する手動操作も新しい契約と矛盾しない
- **`if:` 式と step output の参照**: `steps.maven.outputs.release-needed` / `steps.distribution-tag.outputs.exists` / `steps.drop-deployment.outputs.cleared` はいずれも同じ job 内の先行 step の `id` を指し、値の書き込み (`$GITHUB_OUTPUT`) と読み出しの綴りが一致。`if: failure() && ...` は先行 step の失敗後も評価される
- **`build-consumer.sh` の XA4301 検出**: `dotnet build | tee` が `pipefail` 下にあるためビルド失敗が握り潰されず、`grep -F ... || true` はパイプライン全体に掛かるので未検出でも `set -e` に触れない。`work` は 31-33 行で定義済み。正ケース (検出なし) / 負ケース (4 件検出して exit 1) の両方が `evidence/premise-spike-pack.txt` の 6 節に残っている
- **`ci.yml` の head 制限**: `on.pull_request.branches` に `main` が含まれ、`base_ref == 'main'` 以外 (push / develop 向け PR) では step ごと skip される。ブランチ名は環境変数経由でシェルへ渡している
- **秘密情報の扱い**: `secrets.` の 13 参照がすべて publish job の行範囲、`secrets: inherit` はコメント中の 1 件のみ、deploy key はディスクに置かず ssh-agent に載せ、known_hosts は固定値。手順書も `SIGNING_KEY` を標準入力へ流し鍵ファイルを `trash` で消す形

## アクションプラン

1. Major: `release.yml:138` を代入形に直し、`match` / `absent` 以外を失敗させる (validate が spec の位置で止まるようにする)
2. Minor: `release.yml:727` を同じ形に直し、tag 作成直前の失敗理由が git のエラーにすり替わらないようにする
3. Suggestion: upload 後に ID を失った場合の後始末の入口 (ログからの再抽出、または手順書の 1 行) を用意する
4. Suggestion: `evidence/premise-spike-pack.txt` の 8 節に、9 節で解消済みである旨を足す
