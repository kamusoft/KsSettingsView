# Verify 001: install-examples-and-release-notes

- 検証日: 2026-09-11
- 対象: 未 commit の作業ツリー全体 (`git diff HEAD` と untracked ファイル)
- デルタスペック: `specs/install-examples/spec.md` (ADDED 2 件) / `specs/release-workflow/spec.md` (MODIFIED 2 件・ADDED 1 件)
- 合意済み差分: `deviation.md` (5 項目)

## 判定

**VALID** (条件付き — 下記「残る実地検証」を蒸留前に閉じること)

❌ は 0 件。全 32 Scenario に対応する実装が存在し、機械で回せる検査はすべて実行して成功した。虚偽チェック・逆流・未記録乖離はいずれも無い。ただし 4 Scenario は実装のみの一致で、tasks.md の実地検証タスク 5 件 (8.3b / 8.3c / 8.4 / 8.5 / 8.6) が未完のまま残る。判断の根拠は「残る実地検証」に書く。

### 状態の凡例

| 記号 | 意味 |
|---|---|
| ✅ | 一致 (実装があり、テストまたは実地確認で裏が取れている) |
| ⚠️ | deviation 記録済みの合意済み差分 |
| ⏳ | 実装は仕様どおりだが、実地での確認が未了 (`tasks.md` に未完タスクとして明示されている。欠落・乖離ではない) |
| ❌ | 欠落・乖離 |

---

## 対応表: install-examples (ADDED)

### Requirement: インストール例の version 表記

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 埋めずに使うと依存解決が失敗する | `README.md:46,58,68` / `README_ja.md:46,58,68` / `skills/en/kssettingsview-ios/SKILL.md:40` / `skills/ja/kssettingsview-ios/SKILL.md:40` / `skills/en/kssettingsview-android/SKILL.md:40` / `skills/ja/kssettingsview-android/SKILL.md:40` / `skills/en/kssettingsview-maui/SKILL.md:37` / `skills/ja/kssettingsview-maui/SKILL.md:37` / `skills/en/kssettingsview-aiforms-migration/SKILL.md:40` / `skills/ja/kssettingsview-aiforms-migration/SKILL.md:40` (14 行すべてが `{version}`) | `scripts/install-example-lint.py` 実行 (「10 ファイルのインストール例 14 行が契約を満たす」) | ⏳ 3 経路での解決失敗の実地確認は tasks 8.6 が未了 |
| 最新版の案内から目的の版に着地できる | `README.md:40` / `README_ja.md:40` / 各 Skill の案内行 (`ios:53` / `android:44` / `maui:41` / `aiforms-migration:44`、英日とも) | 本検証で GitHub API を照会: `releases/latest` が `0.1.0-beta.2` (`draft:false` / `prerelease:false`) に解決する | ✅ |
| リリースしてもインストール例は変わらない | `.github/workflows/release.yml` (README / Skill を書き換える step も version 一致を検査する step も存在しない。旧 `Verify install examples` は削除済み) | 本検証で `.github/workflows/release.yml` 全文を走査し README への参照 0 件を確認 | ⏳ `dry-run` 実行と nupkg 同梱 README の実地確認は tasks 8.4 / 8.5 が未了 |

Requirement 本文の SHALL 条項:

| 条項 | 実装 | 状態 |
|---|---|---|
| version の位置はプレースホルダで、依存解決に成功しない形 | `{version}` (`scripts/install-example-lint.py:47` が正の値を持つ) | ✅ |
| 最新版を確認できる案内を置く | 上記 10 ファイルすべてに `releases/latest` へのリンク | ✅ |
| SwiftPM の依存宣言は `exact:` | `README.md:46` / `README_ja.md:46` / iOS Skill 英日 `:40`。理由の説明は `README.md:49` / `README_ja.md:49` / iOS Skill 英日 `:55` | ✅ |
| 配布物に同梱される README も同じプレースホルダのまま | 同梱されるのはルート `README.md` そのもの (`maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj:31` の `PackageReadmeFile`)。workflow が書き換えないため置換の余地が無い | ⏳ 展開しての確認は tasks 8.5 が未了 |
| リリースは version の一致を検査しない | `.github/workflows/release.yml` の validate から一致検査 step を削除 | ✅ |

### Requirement: インストール例の契約の検査

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 具体 version への逆戻りを検出する | `scripts/install-example-lint.py:186` (`check_files`、行番号付きで報告) / CI 接続は `.github/workflows/ci.yml:179` | `--selftest` の「具体 version の書き戻しで exit 1」「ファイルと行が出力される」「実際の値が出力される」 | ✅ |
| SwiftPM の宣言が `exact:` でなくなったことを検出する | `scripts/install-example-lint.py:63-66,224` (`keyword` を取る正規表現と `exact:` の判定) | `--selftest` の「from: への書き換えで exit 1」「exact: を求める理由が出力される」 | ✅ |
| 検査対象に登録されていない Skill を検出する | `scripts/install-example-lint.py:163` (`check_structure`、対象表 `:78` と実構成 `:127` の突合) | `--selftest` の「対象表に無い Skill で exit 1」「未登録の Skill 名が出力される」「対象表にあるのに実在しない Skill で exit 1」 | ✅ |
| 英日の構成のずれを検出する | `scripts/install-example-lint.py:163,236` (`check_structure` / `check_language_parity`) | `--selftest` の「片言語だけの Skill 追加で exit 1」「片言語だけの宣言追加で exit 1」「英日の本数差が出力される」 | ✅ |

Requirement 本文の SHALL 条項:

| 条項 | 実装 | 状態 |
|---|---|---|
| 検査対象 (ファイル・宣言の種別・本数) を検査する側が明示的に持つ | `scripts/install-example-lint.py:78` の `TARGET_FILES` (10 ファイル × 期待種別)。走査は本数の期待値と突き合わせる (`:186`) | ✅ |
| 対象を持たない走査で代替しない | 検査はコードブロック内 (`:140` `scan`) のうち `TARGET_FILES` に挙げたファイルだけを対象にする。`--selftest` の「コードブロック外の具体 version は拾わない」で範囲も確かめている | ✅ |
| 実構成との突合 (未登録・不在・言語差をいずれも失敗) | `scripts/install-example-lint.py:163` | ✅ |
| 日常の検証 CI で走らせる | `.github/workflows/ci.yml:179` (検査) / `:184` (自己テスト。自己テストの接続は deviation の付随修正として記録済み) | ✅ / ⚠️ |

---

## 対応表: release-workflow (MODIFIED)

### Requirement: GitHub Release の発行

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| beta の版でも最新リリースとして参照できる | `.github/workflows/release.yml:925-929` (`gh release create --title --notes-file --latest`。`--prerelease` の分岐は削除済み) | 静的確認のみ (Release の作成は `dry-run` では起きないため、実地は次回の本番リリース) | ✅ |
| 既に発行済みの Release も最新の選別に乗る | GitHub 上で実施済み (tasks 4.2 / 4.3) | 本検証で API を照会: `0.1.0-beta.1` / `0.1.0-beta.2` とも `prerelease:false` `draft:false`、`releases/latest` は `0.1.0-beta.2` | ✅ |

Requirement 本文の SHALL 条項:

| 条項 | 実装 | 状態 |
|---|---|---|
| publish が全て成功した後に Release を作る | `.github/workflows/release.yml:908` の `Create GitHub Release` は publish job の各公開 step より後、`Push monorepo tag` の直後 | ✅ |
| prerelease の印を付けない | 同 `:925-929` (suffix による `--prerelease` 分岐なし) | ✅ |
| 最新のリリースとして明示的に指定する | 同 `:929` の `--latest` | ✅ |
| alpha / beta / rc を同じ扱いにする | version 文字列による分岐が存在しない | ✅ |

### Requirement: Release ノートの内容

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 利用者向けの変更が種別ごとに並ぶ | `scripts/release/build-release-notes.py:152` (`render_notes`、種別は `:52` の `KINDS` 順) | `--selftest`「種別ごとにまとまり、定義順に並ぶ」「項目の無い種別の見出しは出さない」 | ✅ |
| 利用者向けでない変更は載らない | 同 `:102` (`parse_items` の `- none`) | `--selftest`「`- none` だけの pull request からは項目が載らない」 | ✅ |
| 複数の pull request 分が連結される | 同 `:152` / `:268` (`collect_pulls` の pull request 単位の重複除去) | `--selftest`「複数 pull request 分が連結される」「出所の pull request 番号が付く」「同じ pull request は 1 度だけ数える」「異なる pull request の同じ文面は両方残る」 | ✅ |
| Release を持たない tag は起点にならない | 同 `:211` (`select_base` は Release 一覧を起点候補にする) | `--selftest`「Release を持たない tag は起点にならない」(履歴に `9.9.9` を打った状態で確認) | ✅ |
| 今回の tag がある再実行でも対象が変わらない | 同 `:220` (`tag == version` を除外) | `--selftest`「今回の version の tag は起点にならない」 | ✅ |
| draft の Release は起点にならない | 同 `:220` (`release.get("draft")` を除外) | `--selftest`「draft の Release は起点にならない」 | ✅ |
| 対象 commit の祖先でない Release は起点にならない | 同 `:198,223` (`first_parent_chain` に載る commit だけを候補にする) | `--selftest`「対象 commit の祖先でない Release は起点にならない」(枝に打った `0.9.9` で確認) | ✅ |
| 初回のリリースでは履歴の最初から集める | 同 `:204` (`commits_in_range` の起点なし経路) | `--selftest`「公開済み Release が無ければ起点も無い」「起点が無ければ履歴の最初から集める」。記載不備での停止は解析側のテスト群で確認 | ✅ |
| 開発ブランチ宛ての pull request は対象にならない | 同 `:64,271` (`TARGET_BASE_BRANCH` による base の絞り込み) | `--selftest`「開発ブランチ宛ての pull request は対象にならない」 | ✅ |
| 記載の無い pull request があれば publish の前に止まる | 同 `:85` (`extract_section`) / 位置は `.github/workflows/release.yml:188` (validate job、publish job は `:483` で別 job) / 雛形は `.github/pull_request_template.md:1-3` | `--selftest`「セクションが無いと失敗する」「失敗の原因が pull request 番号で分かる」「pull request テンプレートの未編集状態を受理する」 | ✅ |
| 認識できない記載があれば止まる | 同 `:102` (`parse_items` の閉じた文法) | `--selftest`「不正な種別は失敗する」「認識できない非空行は失敗する」「地の文は失敗する」「空の説明は失敗する」「`- none` と項目の共存は失敗する」「見出しが 2 つあると失敗する」「空のセクションは失敗する」 | ✅ |
| 検査の後に本文が編集されても公開内容が変わらない | `.github/workflows/release.yml:188` (validate で確定) → `:207` (成果物へ) → `:597` (publish で取得) → `:917,925-929` (`--notes-file` で本文に使う。本文の再取得なし) | 静的確認のみ (段をまたぐ受け渡しであり、`--selftest` の範囲外) | ✅ |
| 開発ブランチからのリハーサルでは対象を集めない | `.github/workflows/release.yml:173` (`Decide release notes scope` が条件を 1 度だけ評価) / `:188,207,218` が同じ出力を参照 | 静的確認のみ | ⏳ tasks 8.3b が未了 |
| main からのリハーサルでは実際の経路を通る | 同 `:173` の条件 (`dry-run` かつ `main` 以外のときだけ収集しない) / `:207` の upload まで本番と同じ | 静的確認のみ | ⏳ tasks 8.3c が未了 |
| 初回のリリースでは比較の位置を含めない | `scripts/release/build-release-notes.py:169-174` | `--selftest`「初回のリリースでは比較の位置を含めない」 | ✅ |
| 対象が無いときも本文が決まる | 同 `:161-167` | `--selftest`「対象が 0 件でも本文が決まる」「`- none` だけの pull request からは項目が載らない」 | ✅ |
| ノートの組み立てが単独で検査できる | 同 `:650` (`render` サブコマンド。API も git も使わない) / 手順は `kasane/handbook/cross/release-procedure.md:213-218` | `--selftest` 全 37 件 (本検証で実行、失敗 0 件) | ✅ |

Requirement 本文の SHALL 条項:

| 条項 | 実装 | 状態 |
|---|---|---|
| 収集・検査・整形は publish より前に一度だけ行い、確定結果を渡す | `.github/workflows/release.yml:188,207` (validate) / `:597,917` (publish は成果物のみ使用) | ✅ |
| 検査に失敗したら publish に入る前に止まる | validate job 内の step であり、publish は `needs` で validate の成功を待つ | ✅ |
| 重複の除去は pull request 単位 | `scripts/release/build-release-notes.py:275-277` | ✅ |
| API のページ送りで取りこぼさない | 同 `:252` (`paged`)。自己テストは 1 ページ取得だけを差し替え、辿る処理は本物を通す (`:342`) | ✅ (`--selftest`「ページ送りで全件を取る」「上限ちょうどのページの後も次を要求する」) |
| validate の権限を完全な形で書く / token を明示的に渡す | `.github/workflows/release.yml:77-79` (`contents: read` と `pull-requests: read`) / `:191` (`GH_TOKEN`) | ✅ |

補足 (❌ ではない): 起点選定の祖先判定は、対象 commit からの first-parent 鎖 (`scripts/release/build-release-notes.py:198`) で行う。spec は「`main` の first-parent 上で対象 commit の祖先」と書くが、収集が走るのは本番 (起動 ref が `main` に限定される) と `main` からのリハーサルだけなので、対象 commit は常に `main` 上にあり両者は一致する。

---

## 対応表: release-workflow (ADDED)

### Requirement: リリース手順の定型実行

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 記載の下書きが得られる | `.agents/skills/release/SKILL.md:45-62` (下書きの作り方と、確定をユーザーへ委ねる規定) | 文書の検査 (本検証で全文確認) | ✅ |
| 先行する pull request の変更が下書きに混ざらない | 同 `:47-52` (範囲は `origin/main..origin/develop`、前回リリース以降の全変更にしない旨を明記) / handbook 側にも `kasane/handbook/cross/release-procedure.md:148-153` | 文書の検査 | ✅ |
| 事前確認が実行される | `kasane/handbook/cross/release-procedure.md:131-144` (段 1。`develop` の検証 CI の照会と「失敗しているときは先へ進まない」、docs-refresh の要否) / スキル側は `.agents/skills/release/SKILL.md:31-33` | 文書の検査 | ✅ |
| 各段が順に実行できる | `kasane/handbook/cross/release-procedure.md:127-198` (段 1〜5 と各段の到達状態) / `.agents/skills/release/SKILL.md:31-43` (段ごとの到達状態の報告形式) | 文書の検査 | ✅ |
| 失敗したときに手順書の該当箇所が示される | `.agents/skills/release/SKILL.md:64-68` (該当箇所を引用し、無ければ「該当なし」と明示) / 引用先は `kasane/handbook/cross/release-procedure.md:199-241` (validate 前と publish 途中に分けた記述) | 文書の検査 | ✅ |
| 手順を変えたときに道具を直さずに済む | `.agents/skills/release/SKILL.md:12-24` (手順書を毎回読む。段の順序・節の並び・コマンドを持たない) / handbook 側の所有宣言は `kasane/handbook/cross/release-procedure.md:17-27` | 文書の検査 (スキル本文に節名の列挙・コマンドの写しが無いことを確認。スキルが持つコマンドは下書き生成の `git log` のみ) | ✅ |

Requirement 本文の SHALL 条項:

| 条項 | 実装 | 状態 |
|---|---|---|
| 手順の正は handbook、上から順に読めば 1 回のリリースが完了する単一の入口 | `kasane/handbook/cross/release-procedure.md:17-27` (「この手順書の使い方」) | ✅ |
| 道具が自分で持つのは下書きの手順と判断の境界だけ | `.agents/skills/release/SKILL.md:24-27,45-62,70-77` | ✅ |
| 段は事前確認・PR 作成・起動・見守り・失敗時の案内・公開後の確認 | handbook の段 1〜5 (`:131` `:146` `:164` `:174` `:188`) と失敗時 (`:199`) | ✅ |
| 道具は判断を担わない | `.agents/skills/release/SKILL.md:70-77` と `:79-86` の Guardrails | ✅ |
| symlink の設置 (docs-refresh と同じ形) | `.claude/skills/release` → `../../.agents/skills/release` (既存 `.claude/skills/docs-refresh` と同形。git の無視対象でないことを確認) | ✅ |

---

## 追加検査

### tasks.md

- 虚偽チェック: **無し**。`[x]` が付いた全タスクについて、対応表のとおり実体を確認した。GitHub 側で実施した 4.2 / 4.3 も API の応答で裏が取れている
- 未完のまま残るタスク: 8.3b / 8.3c / 8.4 / 8.5 / 8.6 の 5 件 (いずれも remote への push と `workflow_dispatch` が前提の実地検証)。チェックは付いておらず、記載と実態が一致している

### 逆流検査

足場アーティファクト (`proposal.md` / `design.md` / `specs/install-examples/spec.md` / `specs/release-workflow/spec.md`) は起票時の commit `cfb1d23` 以降 1 度も変更されていない。作業ツリーにも未 commit の差分は無い。`tasks.md` の差分はチェックボックスの状態のみ (本検証で機械的に照合: `- [ ]` → `- [x]` 以外の文字列変化 0 件)。

### 未記録乖離

**0 件**。diff の全ファイルが Requirement / tasks へ対応するか、`deviation.md` に `[付随修正]` として記録済み。

| 差分 | 位置づけ |
|---|---|
| `.github/workflows/ci.yml` の `Install example lint selftest` | ⚠️ 付随修正 (記録済み) |
| `.github/workflows/ci.yml` の `Release notes script selftest` | ⚠️ 付随修正 (記録済み) |
| `scripts/release/check-publish-step-order.py` の自己テスト追随 | ⚠️ 付随修正 (記録済み)。本検証で `--selftest` を実行し 4 ケースとも成功 |
| `kasane/handbook/index.md` の cross 行の要約 | ⚠️ 付随修正 (記録済み) |
| `kasane/concepts/cross/architecture/release-pipeline.md:29,65` の旧記述 | ⚠️ 記録済み (蒸留へ繰り延べ確定)。本検証で当該 2 行が現存することを再確認 |
| `.github/workflows/ci.yml` の Central Portal 自己テストのコメント文言 | 置換機構の撤去 (tasks 3.2) に伴う記述の追随。範囲内と判断 |

### 参照の残骸

`set-readme-version.py` と `.github/release.yml` への参照は、`scripts/` `.github/` `skills/` `AGENTS.md` `kasane/handbook/` のいずれにも残っていない (tasks 3.4 / 9.6 の再確認)。残るのは archive / 他 change / ロードマップ / ADR-0029・0030 の経緯記述と、繰り延べ済みの `kasane/concepts/cross/architecture/release-pipeline.md` のみ。

### UI 変更

無し (`ui/` を持たない change)。

### テストの実行結果

本検証で実行したもの (すべて成功):

| 検査 | 結果 |
|---|---|
| `python3 scripts/install-example-lint.py` | 10 ファイル 14 行が契約を満たす (exit 0) |
| `python3 scripts/install-example-lint.py --selftest` | 18 ケース 失敗 0 件 |
| `python3 scripts/release/build-release-notes.py --selftest` | 37 ケース 失敗 0 件 |
| `python3 scripts/release/check-publish-step-order.py` (+ `--selftest`) | 順序正常 / 4 ケース 失敗 0 件 |
| `python3 scripts/release/check-time-budget.py` (+ `--selftest`) | 余裕 8 分 / 4 ケース 失敗 0 件 |
| `python3 scripts/readme-example-lint.py` | 最小例 4 件が一致 |
| `python3 scripts/local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` | いずれも exit 0 |

`.github/workflows/ci.yml` の lint job に載る検査のうち、ネットワークまたは外部ツール (gitleaks) を要するものは対象外とした。

---

## 残る実地検証 (蒸留前に閉じること)

| tasks | 内容 | 掛かる Scenario |
|---|---|---|
| 8.3b | `main` 以外から `dry-run` を起動し、記載が無いことを理由に失敗しないこと | 開発ブランチからのリハーサルでは対象を集めない |
| 8.3c | `main` から `dry-run` を起動し、収集・検査・受け渡しが実際に走ること | main からのリハーサルでは実際の経路を通る |
| 8.4 | `dry-run` で validate から消費者検証までが通ること | (リリースしてもインストール例は変わらない の前提) |
| 8.5 | `dry-run` の nupkg を展開し、同梱 README がプレースホルダのままであること | リリースしてもインストール例は変わらない |
| 8.6 | プレースホルダのままの例で 3 経路の依存解決が失敗すること | 埋めずに使うと依存解決が失敗する |

**❌ とせず ⏳ とした根拠**:

1. 一致検証が問うのは「約束したものが揃っているか」であり、4 Scenario はいずれも**実装が仕様どおり存在する**。欠けているのは実装ではなく実地の証跡である
2. 未了であることが `tasks.md` に未チェックのまま明示されており、虚偽チェック (このスキルが ❌ とする対象) に当たらない
3. 5 件はいずれも remote への push と `workflow_dispatch` の起動を前提とし、検証ワーカーの制約 (git 操作と workflow 起動の禁止) の外にある。指揮側が後から実施する旨が引き継がれている
4. 静的に確かめられる範囲 — 分岐条件が 1 箇所で評価され 3 step が同じ出力を見ること、publish が `dry-run` では走らないこと (`.github/workflows/release.yml:483`)、release workflow に README を書き換える経路が 1 つも無いこと、同梱 README がルート `README.md` そのものであること — は本検証で確認済み

したがって本検証は **VALID** とする。ただし 8.3b / 8.3c は Scenario がそのまま実行結果を問う形をしており、実行して初めて閉じる。蒸留 (アーカイブ) の前にこの 5 件を完了させることを条件とする。
