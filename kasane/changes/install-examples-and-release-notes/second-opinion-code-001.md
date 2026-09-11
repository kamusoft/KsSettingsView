# セカンドオピニオン: install-examples-and-release-notes (code-001)

**相方**: codex / **label**: so-code-install-examples-and-release-notes / **日付**: 2026-09-11 / **対象**: 未 commit の作業ツリー全体 (`git diff HEAD` と untracked ファイル)

---

# レビュー結果: install-examples-and-release-notes

**日付**: 2026-09-11  
**判定**: **CHANGES_REQUESTED**

## サマリー

Release ノートの生成経路とインストール例 lint は概ね仕様どおりです。ただし、ページネーションの selftest が本番実装を通っておらず、将来の退行で Release ノートが静かに欠落し得る Major が1件あります。

提示済みのテスト結果を前提とした静的レビューであり、未実施と明示された tasks 8.3b / 8.3c / 8.4 / 8.5 / 8.6 は指摘に数えていません。`deviation.md` の4件も合意済み差分として除外しました。

## 照合した規約

- `comment-policy.md`（always）
- `test-execution.md`（テスト結果の報告）
- `user-skill-api-listing.md`（`skills/` の変更）
- `install-examples.md`（README・Skill のインストール例）
- `release-procedure.md`（release workflow・リハーサル・再実行）
- 関連 accepted ADR: cross/ADR-0018、0019、0020、0022
- proposed ADR-0029、0030は変更の設計背景としてのみ参照
- `kasane/config.yaml` の cross domain では追加の `code-review` / `domain` / `impl` スキル指定なし

## 指摘事項

### [🟠 Major] ページネーションの selftest が本番実装を検査していない

**該当箇所**: `scripts/release/build-release-notes.py:342`、`scripts/release/build-release-notes.py:578`

**問題点**: `selftest_paging()` は本番の `GitHubApi.paged()` を呼ばず、テスト専用の `PagingApi.paged()` にページ送り処理を再実装しています。そのため、本番側を「1ページ目だけ返す」実装へ退行させても selftest は成功します。これは tasks 5.3e の「GitHub API のページ送りを自己テストで検査する」を満たさず、100件を超えた場合に不完全な Release ノートが成功扱いで公開される回帰を検出できません。

**推奨修正**: `GitHubApi` の `_get()` だけを差し替えたテスト用 subclass、または取得関数の注入を使い、実際の `GitHubApi.paged()` を2ページ以上通してください。本番のループを1ページだけに変えた場合に selftest が落ちる構造にします。

### [🟡 Minor] 再実行手順が「同じ commitの既存 tag」と「別 commit の衝突」を区別していない

**該当箇所**: `kasane/handbook/cross/release-procedure.md:208`

**問題点**: 「既に tag がある version は出し直せない」と一律に記載されていますが、workflow は同じ `GITHUB_SHA` を指す tag を再実行として受理します。Release 作成だけが失敗した場合など、同じ version で続行すべき経路との区別が読み手に伝わりません。

**推奨修正**: 「別 commit を指す同名 tag は再利用不可。同じ起動 commit を指す場合は再実行として続行する」と明記してください。

### [🟡 Minor] `## Changes` がそのまま Release 本文になるという説明が実装と異なる

**該当箇所**: `kasane/handbook/cross/release-procedure.md:160`

**問題点**: 実際には項目を種別別に再編し、日本語見出し・PR番号・比較リンクを追加します。「セクションがそのまま本文になる」という説明は、記載順や書式が保存されるとの誤解を招きます。

**推奨修正**: 「このセクションの項目が Release 本文の材料になり、workflow が種別別に整形する」など、変換があることを明記してください。

### [🟡 Minor] tasks 9.6が完了扱いだが、現行conceptに削除済みscriptへの参照が残る

**該当箇所**: `tasks.md:87`、`kasane/concepts/cross/architecture/release-pipeline.md:65`

**問題点**: tasks 9.6は「conceptsを含め参照が残っていない」として完了していますが、conceptは現在形で `set-readme-version.py` による置換とvalidate検査を説明しています。一方、同じtasksの蒸留申し送りでは、このconceptの更新を後続作業として明記しています。

**推奨修正**: 蒸留で更新する方針なら9.6の対象からconceptsを外して記述とチェック状態を整合させるか、9.6を完了扱いにするならconceptを現在の実装へ追随させてください。

## アクションプラン

1. `GitHubApi.paged()` 本体を通るページネーションselftestへ修正する。
2. handbookの再実行条件とRelease本文生成の説明を実装に合わせる。
3. tasks 9.6とconcept更新のタイミングを整合させる。
4. 修正後、既存selftestとページネーションの退行確認を再実施する。
5. その後、予定どおりtasks 8.3b / 8.3c / 8.4 / 8.5 / 8.6をremote上で完了させる。

重要度別件数: **Critical 0 / Major 1 / Minor 3 / Suggestion 0**

なお、GitHub公式仕様上、`gh release create --latest` は有効であり、default branch上のcommitに対する関連PR APIもmerged PRを返すため、これらは指摘対象にしていません。[GitHub CLI](https://cli.github.com/manual/gh_release_create)、[GitHub REST API](https://docs.github.com/en/rest/commits/commits#list-pull-requests-associated-with-a-commit)


---

## 突き合わせ結果

ホスト側レビュー ([review-001.md](review-001.md)) と突き合わせた結果:

- **確定** (双方一致): ページ送りの自己テストが実装を通っていない (Major)。ホスト側は `GitHubApi.paged` を 1 ページ打ち切りに退行させても自己テスト 35 件が緑のままであることを実測で確認している
- **確定** (双方一致): `kasane/concepts/cross/architecture/release-pipeline.md` に削除済みスクリプトの記述が残る (Minor)。対応は tasks.md の「蒸留への申し送り」に従い蒸留へ繰り延べ、9.6 の走査結果を [deviation.md](deviation.md) に記録した
- **採用** (相方のみ・根拠強): handbook `cross/release-procedure.md` の再実行条件が、同じ起動 commit を指す tag の再実行と、別 commit を指す同名 tag の衝突を区別していない (Minor)
- **採用** (相方のみ・根拠強): 同 handbook の「`## Changes` がそのまま Release 本文になる」という説明が、種別別の再編・見出し・比較リンクの付与を行う実装と食い違う (Minor)
- **降格**: なし
- **未解決**: なし

相方が触れなかったホスト側の Major 1 件 (`Upload release notes` の `overwrite: true` 欠落) と Suggestion 2 件は、ホスト側判定のまま同じ修正サイクルに含める。

件数: 確定 2 / 採用 2 / 降格 0 / 未解決 0
