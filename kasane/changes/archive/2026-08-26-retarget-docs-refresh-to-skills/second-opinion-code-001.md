# セカンドオピニオン: retarget-docs-refresh-to-skills (code-001)

**相方**: codex / **label**: so-code-retarget-docs-refresh-to-skills / **日付**: 2026-08-26 / **対象**: 作業ツリーの未コミット変更一式 (.agents/skills/docs-refresh/SKILL.md 全面改稿・AGENTS.md・kasane/config.yaml・tasks.md のチェック状態)

---

# レビュー結果: retarget-docs-refresh-to-skills

**日付**: 2026-08-26
**判定**: CHANGES_REQUESTED

## サマリー

変更範囲は proposal / Non-Goals と概ね一致し、AGENTS.md・config・manifest v3 の規範記述も妥当です。一方、主要フローに実行不能または spec 違反となる分岐が3件あります。

指摘件数: Critical 0 / Major 3 / Minor 2 / Suggestion 0

## 指摘事項

### [🟠 Major] 未確定 manifest を検査できず、新規・削除 concept の処理が完了しない

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:139`, `.agents/skills/docs-refresh/SKILL.md:143`, `.agents/skills/docs-refresh/SKILL.md:283`, `.agents/skills/docs-refresh/SKILL.md:294`, `.agents/skills/docs-refresh/SKILL.md:486`

**問題点**: 新規 concept の配置や削除 concept の整理を承認しても、Step 6 の網羅検査はディスク上の旧 `skills/.manifest.json` を読み直します。manifest は Step 7 まで書かない規律なので、新規 concept は引き続き `UNCOVERED`、削除 concept は `DELETED` となります。manifest を早期更新すると「最後に書く」契約に反し、更新しなければ検査を通過できません。

**推奨修正**: 承認内容を反映した「候補 manifest」を一時ファイルまたはメモリ上に構築し、Step 6 の全検査を候補 manifest に対して実行してください。検査通過後にだけ候補を `skills/.manifest.json` へ反映する手順を明記してください。

### [🟠 Major] `--readme-only` が Skill 本体を検出・修正対象にできてしまう

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:117`, `.agents/skills/docs-refresh/SKILL.md:169`, `.agents/skills/docs-refresh/SKILL.md:175`, `.agents/skills/docs-refresh/SKILL.md:283`
**対応仕様**: `specs/docs-refresh/spec.md:143`

**問題点**: Step 3d は `--readme-only` でも実行され、ツールバージョン差分から各 `SKILL.md` を要追従リストへ追加できます。またStep 6の対象リストは常にmanifestの全 `targets` を含み、Skill側の失敗を再修正対象にします。「候補はreadmesに限定し、skills/本体を検出・更新しない」というScenarioに反します。`tasks.md:29` の検証完了チェックも現状では成立していません。

**推奨修正**: モード別に対象を分離し、`--readme-only` ではStep 3dのSkill突合先を除外してください。Step 6もREADMEだけを再修正対象とし、Skill固有検査は対象外（N/A）とするなど、8検査の扱いを明示してください。

### [🟠 Major] サブエージェント不在時のフォールバックが2つのSHALLに違反する

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:47`, `.agents/skills/docs-refresh/SKILL.md:221`
**対応仕様**: `specs/docs-refresh/spec.md:17`, `specs/docs-refresh/spec.md:77`

**問題点**: サブエージェント機構がない場合にメインが順次処理する分岐では、メインコンテキストがconcepts本文を読む必要があります。これは「メインはconcepts本文を読まない」「更新はサブエージェントへ委譲する」という両方の契約に反します。

**推奨修正**: サブエージェントを利用できない場合は、承認・書き込み前に実行不能として停止してください。別の隔離コンテキスト機構を正式に採用する場合は、その具体的な手順を記載してください。

### [🟡 Minor] README委譲に適用できるプロンプトがない

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:218`, `.agents/skills/docs-refresh/SKILL.md:223`

**問題点**: README単位の委譲は宣言されていますが、直後のテンプレートはSkill名・Skillディレクトリ・関連conceptsを必須とするSkill専用です。READMEタスクにはそのまま適用できず、特に`--readme-only`の手順が自己完結していません。

**推奨修正**: README専用テンプレートを追加し、対象ペア、コード正の取得元、検出差分、README種別ごとの確認事項を定義してください。

### [🟡 Minor] `docs/` 参照検査に検出漏れがある

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:426`

**問題点**: 正規表現は `docs/foo.md` だけを想定しており、`docs/` という裸の参照、`docs/guides/foo.md`、複数ドットを含むファイル名などを検出しません。「`docs/` 参照新設を検出する」という仕様を完全には満たしません。

**推奨修正**: Markdownリンク先を解析して正規化後の先頭セグメントが`docs`かを判定し、併せて裸の`docs/`記述も検査してください。正負fixtureにも裸参照・ネスト参照を追加してください。

## アクションプラン

1. 候補manifestを用いた検査フローを定義する。
2. `--readme-only`の対象範囲をREADMEに限定する。
3. サブエージェント不在時は安全に停止する。
4. README専用プロンプトと`docs/`検査を補強する。
5. 関連するtasksの完了状態を再確認する。

**判定: CHANGES_REQUESTED**

---

## 突き合わせ結果

ホスト側 [review-001.md](review-001.md) との突き合わせ (2026-08-26、オーケストレーター実施):

| 相方の指摘 | ホスト側の対応指摘 | 採否 | 根拠 |
|---|---|---|---|
| Major 1: 網羅検査が旧 manifest を読み偽陽性 | Major 1 (同内容・同一箇所) | **確定** (Major) | 双方一致。修正方向も一致 (承認済み配置判断を反映した予定 targets/excluded を検査入力にする) |
| Major 2: `--readme-only` が Skill 本体を対象化 | Major 2 (同内容。spec の 2 SHALL の交差衝突と特定) | **確定** (Major) | 双方一致。解の選択 (報告のみ / 例外許可) は spec が解いていないためユーザー判断へ |
| Major 3: サブエージェント不在時フォールバックが SHALL 2 件に違反 | 対応なし (ホスト見逃し) | **採用** (Major) | 相方のみだが根拠強 — SKILL.md:47 / :221 の該当記述の実在と、spec「メインは concepts 本文を読まない」「Skill 単位で委譲する SHALL」への違反をオーケストレーターが実地確認 |
| Minor 1: README 委譲に適用できるプロンプトがない | 対応なし | **採用** (Minor) | 該当箇所特定済み。README 単位委譲の宣言 (SKILL.md:219) に対しテンプレートが Skill 専用で、`--readme-only` の手順が自己完結しない実害あり |
| Minor 2: `docs/` 参照検査の検出漏れ | Suggestion 7 (ネストパスの取りこぼし。相方は裸参照・複数ドットも指摘) | **確定** (Minor — 相方の高い方を採る) | 双方一致。修正は相方の指摘範囲 (裸 `docs/` 参照・ネスト・複数ドット) を含める |

採用 2 / 確定 3 / 降格 0 / 未解決 0。採用分はホスト側指摘と同格として修正サイクル 1 周目に含める。
