# セカンドオピニオン: add-release-workflow (code-002)
**相方**: codex / **label**: so-code-add-release-workflow (turn 2) / **日付**: 2026-09-03 / **対象**: 修正サイクル 1 後の作業ツリー (develop 4c04878 からの未コミット差分)
---
# レビュー結果: add-release-workflow 再確認

**判定: CHANGES_REQUESTED**

指摘件数: Critical 0 / Major 2 / Minor 0 / Suggestion 0

## 指摘事項

### [🟠 Major] deployment ID artifact の取得障害を「ID なし」として扱う

**該当箇所**: `.github/workflows/release.yml:480`

**問題点**:  
`Download previous deployment id` 全体に `continue-on-error: true` が指定されています。固定されている `download-artifact` は、指定名の artifact が無い場合だけでなく、API・通信・展開エラーでも step を失敗させますが、現在はすべて握り潰されます。[固定版 action の実装](https://github.com/actions/download-artifact/blob/018cc2cf5baa6db3ef3c5f8a56943fffe632ef53/src/download-artifact.ts)

前 attempt に VALIDATED / PUBLISHING の deployment がある状態で一時的な取得障害が起きると、`deployment_id=""` のまま公開済み HEAD 検査へ進み、未公開なら新しい deployment を upload します。結果として保留 deployment の重複や、追跡不能な deployment が残り得ます。

**推奨修正**:  
「artifact が存在しない」と「取得失敗」を分離してください。例えば、完全一致する `pattern:` で取得して artifact 0 件だけを正常扱いにし、`continue-on-error` を外す方法があります。あるいは API で存在確認し、存在時の download エラーは publish を停止してください。

### [🟠 Major] 事前検査で tag が存在すると、作成直前の再検査が実行されない

**該当箇所**: `.github/workflows/release.yml:719` / `scripts/release/check-distribution-tag.sh:45`

**問題点**:  
不可逆な公開前の検査で `match` だった場合、`if: steps.distribution-tag.outputs.exists != 'true'` により `Push distribution repository tag` step 全体が skip されます。したがって、その後に同名 tag が削除・移動されても検出せず、distribution tag が無い、または異なる状態で monorepo tag と Release を作成できます。

また、step を常時実行するだけでは削除を検出できません。現在の `git fetch --tags --force` は、remote から削除されたローカル tag を prune しないためです。[git-fetch の公式仕様](https://git-scm.com/docs/git-fetch)

**推奨修正**:  
step-level の `if` を外して常に最終確認を行い、検査前に remote を正として tag を更新してください。例えば `git fetch --prune --prune-tags --tags --force origin`、または `git ls-remote --tags origin "refs/tags/${VERSION}"` で存在を直接確認し、`match` なら skip、`absent` なら作成、内容違いなら失敗とします。

## 修正済みと確認した点

- HTTP 404 は `NOT_FOUND` となり、再 upload 分岐へ戻れる
- drop 後に実在を再照会し、削除済みの場合だけ ID artifact を空内容で上書きする
- `maven-metadata*.xml` は Android 成果物比較から除外されている
- CI の head 検査は branch 名に加えて repository identity も確認する
- `AGENTS.md` の README 機械置換例外と `exact:` への変更が整合している
- `release-procedure.md` の drop／再実行手順は現在の実装と一致する
- 指定された `rm` と `pull_request_target` / Ruleset の論点は判定対象から除外した

ユーザー指定に従い、ビルド・テスト実行およびレビュー結果ファイルの書き込みは行っていません。


## 突き合わせ結果 (review-002 との照合、2026-09-03)

| 相方の指摘 | 採否 | 根拠 |
|---|---|---|
| deployment ID artifact の取得障害を「ID なし」として扱う (Major) | **採用** (Minor 相当・修正サイクル 2 で対応) | 相方のみだが箇所特定・実害 (保留 deployment の重複) が具体。`continue-on-error` を外し「artifact 不在」だけを正常扱いにする数行の修正で閉じる |
| 事前検査で tag が存在すると作成直前の再検査が実行されない / fetch が削除を prune しない (Major) | **採用** (Minor 相当・修正サイクル 2 で対応) | 相方のみ。発生には同一 job 内の数分の窓で人が tag を削除する必要があり実害の確率は低いが、review-002 の Minor (同じ step の握り潰し) と同じ箇所の作り直しで一緒に閉じる |

review-002 (ホスト) の Major「validate の配信リポジトリ tag 検査が失敗を握り潰す」は相方の指摘には無かった (ホストのみ)。採用 2 / 降格 0 / 未解決 0
