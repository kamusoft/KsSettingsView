# レビュー結果: add-release-workflow (003 回目)

**日付**: 2026-09-03
**判定**: APPROVED

## サマリー

review-002 の Major (validate の配信リポジトリ tag 検査の握り潰し) と Minor (tag 作成直前の再検査の握り潰し) は、どちらも代入形 + `match` / `absent` / `*` の 3 分岐に直っており、合成した配信リポジトリで step の `run` をそのまま流して**内容の異なる同名 tag に対して exit 1 になること**を実測した。失敗理由も `fatal: tag ... already exists` ではなく意図した `::error::` に戻っている。second-opinion-code-002 の 2 件 (`continue-on-error` の撤去、tag 作成直前検査の常時実行と remote 正の判定) も、`download-artifact` v6 の実装を確認したうえで意図どおり閉じていると判断した。Suggestion 2 件 (ログからの ID 拾い直し、evidence 8 節の注記) も反映済み。

残る指摘は、拾い直した deployment ID が artifact へ保存されないまま「引き継ぐ」と書かれている 1 点 (Minor) と、抽出正規表現の二重化 (Suggestion) のみで、いずれもデルタスペックの Scenario の常経路には影響しない。Critical / Major なしのため APPROVED とする。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — `comment-policy-lint.py` が禁止 0 件 (検査対象 728 ファイル)。修正サイクル 2 で書き足されたコメント (`release.yml` の `Download previous deployment id` / `Recover deployment id from upload log` / `Push distribution repository tag`、`check-distribution-tag.sh` の冒頭 20-24 行) を本文でも目視し、change 名・Decision 番号・作業文書パスの参照が無いことを確認
- `kasane/handbook/cross/test-execution.md` (テストを実行・報告するとき) — 実行件数を下に併記
- `kasane/handbook/cross/release-procedure.md` (本 change が追加。index の「適用のきっかけ」と本文の整合を再確認。下の Minor は本文 156 行に掛かる)
- `kasane/handbook/cross/local-development-setup.md` — 修正サイクル 2 は `verification/maui/build-consumer.sh` に触れておらず、手元実行の手順に変化なし
- `kasane/handbook/cross/public-identifiers.md` — 配布座標に変更なし
- `kasane/handbook/maui/index.md` の 2 文書は今回の diff (workflow / script / 証跡) に当たらないため参照のみ
- lessons: `code-review.md` (L-001 — 静的読解で止めず合成リポジトリでの実測に置き換えた)、`process.md` (L-005 / L-007)

## 実行した検証

- `dotnet test maui/KsSettingsView.Maui.Tests -c Release`: **合格 516 / 失敗 0 / スキップ 0 / 合計 516**
- `check-distribution-tag.sh` を合成した配信リポジトリ (bare origin + 作業コピー) の 4 ケースで直接実行:

      ケース                          出力      exit
      tag 無し                        absent    0
      同一内容の tag                  match     0
      別内容の tag                    (無し)    1
      remote から削除済み・手元に残存  absent    0  ← 手元の tag を消し、後続の `git tag` が通る

- PyYAML で `release.yml` から step の `run` を取り出し、同じ合成リポジトリに対して `bash` で実行:

      validate `Verify distribution repository tag` (別内容の tag)     → ::error:: を出して exit 1
      publish  `Push distribution repository tag`   (別内容の tag)     → ::error:: を出して exit 1
      publish  `Push distribution repository tag`   (同一内容 = match) → skip して exit 0

  review-002 で握り潰しを実測した 2 箇所が、いずれも errexit に載ったことを確認した
- `Recover deployment id from upload log` の `run` を取り出し、合成ログ 3 種で実測: ログ無し → 出力なし / ID あり → `::warning::` と `deployment-id=` の出力 / ID 無し → 出力なし。いずれも exit 0 (job の失敗状態を戻さない)
- `actions/download-artifact` v6.0.0 (pin されている `018cc2cf`) の実装を確認: `pattern` 経路は一致 0 件でもエラーにならず正常終了し、`merge-multiple: true` は artifact 名のサブディレクトリを作らず `path` 直下へ展開する。したがって `${RUNNER_TEMP}/deployment/deployment-id.txt` の読み出し位置と合っており、通信・展開エラーだけが step を落とす形になっている
- `release.yml` の全 27 個の `run` ブロックを `bash -n`、`scripts/release/*.sh` 5 本と `verification/maui/build-consumer.sh` も `bash -n`
- `scripts/release/central-portal.sh --selftest` (失敗なし) / `python3 scripts/release/set-readme-version.py --selftest` (失敗なし)
- `comment-policy-lint.py` / `local-path-lint.py` / `identity-lint.py` / `doc-structure-lint.py`: 本 change の新規・変更ファイルに指摘なし
- `id: distribution-tag` の撤去に伴う参照漏れが無いこと (`grep` で `distribution-tag` の残存参照 0 件)、および `if [ "$(...)" ]` / `case "$(...)"` 形の握り潰しが workflow と scripts に残っていないことを確認 (唯一の一致は `build-consumer.sh:64` の `case "$(uname -m)"` で、失敗し得ないコマンド)
- step の `if` と暗黙 `success()` の対応を再確認: `Upload deployment id` / `Release Maven Central deployment` は status 関数を含まないため暗黙 `success()` が掛かり maven 失敗時に skip、`Recover` / `Drop` / `Clear` は `failure()` を含むため失敗後に評価される
- iOS / Android の本体テストは未実行 (製品コードに差分なし。review-001 / 002 と同じ扱い)

## 指摘事項

### [🟡 Minor] ログから拾い直した deployment ID は artifact へ保存されず、「引き継ぐ」という記述だけが残る

**該当箇所**: `.github/workflows/release.yml:833-844` / `kasane/handbook/cross/release-procedure.md:156`

**問題点**: `Recover deployment id from upload log` が拾い直した ID は `Summarize` と `Drop pending deployment` には渡るが、`central-deployment-id` artifact には書き戻されない (artifact を作る `Upload deployment id` の条件は `steps.maven.outputs.deployment-id != ''` のままで、拾い直し経路では常に false)。

`Drop pending deployment` は削除できない状態のとき

```bash
echo "deployment は残っている (${state})。ID の引き継ぎはそのままにする"
```

と出し、手順書 156 行も「削除できない状態 (公開処理が始まっている) のときは何もせず理由が出て ID もそのまま残るので (次の attempt がその状態を見て続きを行く)」と書いているが、拾い直し経路では**引き継ぐ artifact がそもそも存在しない**。upload が Portal に受理された直後の deployment は PENDING / VALIDATING であることが多く、`central-portal.sh drop` はこの 2 状態で何もしない (selftest の該当チェックで確認済み) ため、拾い直しが起きたときはむしろこの分岐に入りやすい。

その場合の次の attempt は ID 無しで始まり、`published` が未公開を返すので再 upload に進み、同じ座標 + version の保留 deployment が 2 つできる。取り消せない被害は出ず (release されるのは片方だけ)、ID は前 attempt の `::warning::` と job summary に残るが、後始末は手作業になる。

デルタスペック (release-workflow) の「deployment ID は同じ実行の再実行から参照できる形で保存する SHALL」は、通常経路 (maven step が成功して ID を出力する) では満たされている。本指摘は spec が明示していない縁の経路と、そこに掛かる記述の不一致についてのもの。

**推奨修正**: `Recover` step で `${RUNNER_TEMP}/deployment/deployment-id.txt` にも書き出し、`if: failure() && steps.recover-deployment.outputs.deployment-id != ''` の `upload-artifact` step (`overwrite: true`) を `Drop pending deployment` より前に足す。この形なら `Clear stored deployment id` が後から空で上書きするので、drop できた場合の整合も崩れない。artifact への保存を行わない判断を採るなら、代わりに `release.yml:843` の文言と手順書 156 行を「拾い直した ID は次の attempt へは渡らないので、前 attempt のログから控えて手で drop する」に直す。

### [🔵 Suggestion] deployment ID の抽出正規表現が 2 箇所に複製され、片方が黙って効かなくなり得る

**該当箇所**: `.github/workflows/release.yml:646` / `.github/workflows/release.yml:683`

**問題点**: `grep -oiE 'deployment id: [0-9a-f-]{36}' ... | tail -n 1 | awk '{print $3}'` が upload step と拾い直し step に同じ形で 2 回書かれている。plugin のログ文言が変わったとき、upload 側は `exit 1` で必ず気づけるのに対し、拾い直し側は「ログに deployment ID は無い」と出して exit 0 で通るため、片方だけ直した状態が発覚しない。step をまたぐので shell 関数では括れないが、抽出だけを `scripts/release/` の小さな script (または `central-portal.sh` のサブコマンド) に出せば 1 箇所になり、`--selftest` にも載せられる。

**推奨修正**: 抽出を script 1 本に寄せ、両 step から呼ぶ。据え置く場合は、拾い直し側のコメントに「upload step と同じ規則。片方だけ変えないこと」を明記する。

## 確認して問題がなかった観点

- **review-002 Major の解消 (validate)**: `release.yml:140` が代入形になり、`match` / `absent` / `*` の 3 分岐を持つ。合成リポジトリでの実測で step が exit 1 になり、デルタスペックの Scenario「配信リポジトリの同名 tag は publish の前に内容で判定する」の THEN (配布物の生成・publish に入る前に失敗する) が成立する位置に戻った
- **review-002 Minor の解消 (tag 作成直前)**: `release.yml:755-763` が同じ形になり、`match` は skip、`absent` だけが `git tag` へ進み、それ以外は理由を出して失敗する。`Push monorepo tag` (768-782) の扱いとも揃った
- **second-opinion Major 1 の解消**: `Download previous deployment id` から `continue-on-error` が消え、`pattern:` + `merge-multiple: true` に置き換わっている。artifact 名 `central-deployment-id` に glob メタ文字は無く完全一致になる。v6.0.0 の実装で「一致 0 件は正常終了」「取得失敗は throw」「merge-multiple は path 直下へ展開」を確認したので、artifact 不在だけが正常扱いになり、読み出し位置も一致している
- **second-opinion Major 2 の解消**: `Push distribution repository tag` の step-level `if` が撤去され常時実行になった。`check-distribution-tag.sh` は `git ls-remote --tags origin` で remote を正として有無を判定し (49 行)、remote から消えた手元の tag を `git tag -d` で落とす (54-57 行) ため、prune 漏れによる `git tag` の重複失敗も起きない。合成リポジトリの「remote 削除済み・手元に残存」ケースで、prune 後に `git tag` が通ることを実測した。`git tag -d` の対象は配信リポジトリの作業コピー (`-C "${WORK}"`) に限られ、validate は使い捨ての clone、publish は自分が作る tag しか持たないため、呼び出し側の期待を壊さない
- **review-002 Suggestion の反映**: `Recover deployment id from upload log` が入り、3 経路とも実測どおり。`Summarize` (804) と `Drop pending deployment` (824 / 828) の `a || b` は空文字が falsy なので意図どおりフォールバックし、skip された step の output が null でも比較は成立する。`evidence/premise-spike-pack.txt` の 8 節は見出しに「※ 9 節で解消済み」が付き、本文でも 9 節を指しているため、証跡だけを読んでも結論が 1 つに読める
- **証跡の追随**: `evidence/release-workflow-static.txt` の secrets 行番号 13 件が修正後の `release.yml` と一致しており (503 / 556-562 / 707 / 740-741 / 826-827)、サイクル 2 の後に採り直されている。`evidence/scripts-unit.txt` の「保留 deployment の ID をログから拾い直す経路」の 3 ケースは手元で再現でき、記述と一致する
- **tasks.md の誠実さ**: 新たに `[x]` になったものはいずれも実物がある。GitHub Actions 上の実行が要るもの (1.1 / 1.3 / 1.4 / 5.1〜5.3 / 5.6 / 5.7 / 6.x / 7.x) は `[ ]` のまま残っており、静的部分の証跡がある 5.3 も未チェックのままで、虚偽のチェックは無い。1.2 の本文 (`TargetsForTfmSpecificContentInPackage` の末尾) と実装 (`AfterTargets="_IncludeAarInNuGetPackage"`) の差は deviation.md の 1 項目目に記録済み
- **足場の不変**: proposal / design / specs 4 本 / tasks の本文に実装由来の書き換えは無く (tasks は `[ ]` → `[x]` のみ)、deviation.md の 5 項目はいずれも合意済みの差分として指摘対象外
- **秘密情報の扱い**: `secrets.` の参照 13 行がすべて publish job の行範囲、`secrets:` ブロックと `secrets: inherit` は定義として存在せず (一致するのはコメント 1 行のみ)、publish 以外の job は `contents: read` を継承する
- **`Directory.Build.targets` / `build-consumer.sh` / `ci.yml` / `.github/release.yml`**: サイクル 2 で変更されておらず、review-002 の確認から差分なし

## アクションプラン

1. Minor: 拾い直した ID の artifact 保存 (upload step 1 つの追加) か、`release.yml:843` と `release-procedure.md:156` の文言修正のどちらかで、記述と実装のずれを閉じる。初回リリース (tasks 7.3) より前に済ませておくと、本番 run で拾い直しに当たったときの手順が確定する
2. Suggestion: deployment ID の抽出を 1 箇所に寄せる (据え置くならコメントで対を明示する)

いずれも判定を CHANGES_REQUESTED にする性質のものではない。

---

## 修正確認 (Minor)

**日付**: 2026-09-03 / **対象**: `.github/workflows/release.yml:690-703` / **結果**: **解消**

### (1) 推奨修正の意図どおりか

意図どおり。`Recover deployment id from upload log` が output に加えて `${RUNNER_TEMP}/deployment/deployment-id.txt` へ書き (690-692)、直後の `Store recovered deployment id` (696-703) が `if: failure() && steps.recover-deployment.outputs.deployment-id != ''` で `overwrite: true` の upload-artifact を回す。review-003 の推奨修正の 1 つ目 (artifact への保存) と同じ形で、通常経路の `Upload deployment id` は `steps.maven.outputs.deployment-id != ''` (status 関数を含まないため暗黙 `success()` が掛かる) のままなので、両者は排他になり同じ artifact 名を同時に取り合わない。

ファイルの内容は拾い直し側が `printf '%s\n'`、通常経路が `printf '%s'` と改行の有無が違うが、読み出し側 (`release.yml:569-571`) が `tr -d '[:space:]'` を通すため差は消える。

### (2) `failure()` 下の step の並びと artifact の最終内容

publish job の step 順は 12 `Recover` → 13 `Store recovered` → 14 `Upload deployment id` → … → 22 `Drop pending` → 23 `Clear stored` で、保存 (13) が後始末 (22) より前にあり、drop できたときの上書き (23) が最後に来る。`Recover` と `Drop` の `run` を取り出し、`central-portal.sh` をモックへ差し替えて 3 経路を流した結果:

    経路                          Store が走るか  drop の cleared  artifact の最終内容
    drop 成功 (VALIDATED)         走る            true             "\n" (Clear が置換)
    drop 不可 (PENDING/VALIDATING) 走る            false            "<id>\n" (Store のまま)
    ID 無し (ログ無し / ID 無し)   走らない        (Drop 自体 skip) 書かない

3 経路とも整合する。とくに 2 行目が今回の穴で、次の attempt は artifact から ID を読み、`release.yml:573-604` の検証待ち → 状態分岐へ入るため、保留 deployment を二重に作らずに済む。1 行目は削除済みの ID が残らないこと、3 行目は `Drop pending deployment` の `if` が両 output とも空で false になるため `Clear` も走らず、前 attempt の artifact をいたずらに壊さないことを確認した。

`Store` の `if-no-files-found: error` は、output を書いた `Recover` が必ず同じ step 内でファイルも書くため、通常は発火しない。ファイル書き込みだけが失敗した場合は `Store` が明示的に落ちる (job は既に失敗している) ので、黙って引き継ぎを失う経路にはならない。

### (3) 手順書 `release-procedure.md:156` の記述

主張の本体は正しくなった。「削除できない状態のときは何もせず理由が出て ID もそのまま残る (次の attempt がその状態を見て続きを行う)」が、拾い直し経路でも成立するようになったため、記述の修正は不要。

1 点だけ用語のずれが残る: 括弧書きの「(公開処理が始まっている)」は PUBLISHING だけを指しているが、`central-portal.sh drop` は PENDING / VALIDATING でも削除しない (契約コメント 23-26 行と selftest の該当チェック)。拾い直しが起きるのは upload 直後なので、実際に当たりやすいのは PUBLISHING ではなく PENDING / VALIDATING の方になる。読み手が「公開処理は始まっていないのに消えない」と迷い得るので、括弧を「(検証中か公開処理中)」程度に広げると手順書と script の契約が揃う。判定を左右する不備ではないため、次に同じ節へ触れるときで足りる。

### 再検査

- PyYAML で `release.yml` を読み直し、`run` 27 ブロックの `bash -n` に失敗なし
- publish job の step 一覧を定義から生成し、上記の並びを確認
- `Recover` / `Drop` の `run` をモックで 3 経路実行 (上表)
