# Verify 001: consolidate-readmes-and-contribution

- 対象: `specs/repository-docs/spec.md` (ADDED 9 Requirement / 25 Scenario)、`specs/docs-refresh/spec.md` (MODIFIED 1 + ADDED 1 Requirement / 6 Scenario)
- 級: L / domain: cross
- 検証日: 2026-08-29
- 判定: **VALID**

本変更はドキュメント再編でありユニットテストの対象がない。「テスト」列には実際に実行したコマンド / 現物確認の内容と結果を書く。

---

## 1. repository-docs

### Requirement: README の所在

| Scenario | 実装 | 確認方法と結果 | 状態 |
|---|---|---|---|
| platform / Sample README の不在 | `android/README.md` `maui/README.md` `samples/{ios,android,maui}/README.md` を削除 (git status `D` 5 件) | `for d in android maui samples/ios samples/android samples/maui; do [ -e "$d/README.md" ]; done` → 5 件とも absent | ✅ 一致 |
| 公開ドキュメント面の README 集合 | ルート `README.md` / `README_ja.md`、`skills/README.md` / `skills/README_ja.md`、`maui/spike/README.md` | `find . -maxdepth 1 -name 'README*.md'` → 2 件、`find skills android ios maui samples -name 'README*.md'` → `maui/spike/README.md` / `skills/README.md` / `skills/README_ja.md` の 3 件。合計 5 枚ちょうど | ✅ 一致 |
| 現行文書からの参照の解消 | ルート README 2 枚 / `skills/` / `.github/` / `.agents/skills/docs-refresh/SKILL.md` | `grep -rnE '\]\([^)]*(android/README\|maui/README\|samples/[a-z]+/README)'` → 0 件 (Markdown リンクなし)。素の言及は `.agents/skills/docs-refresh/SKILL.md:182` の 1 件のみでリンクではない (deviation 記録あり)。リポジトリ全体でも残存は `.claude/worktrees/` 配下のみ (spec が対象外と明示) | ✅ 一致 |

### Requirement: ルート README の節構成

| Scenario | 実装 | 確認方法と結果 | 状態 |
|---|---|---|---|
| (要件本文) 節の順序 | `README.md:7-172` / `README_ja.md:7-172` | `grep -n '^#'` で両者を照合。概要と主な特徴 → スクリーンショット → 対応プラットフォーム → インストール → 最小コード例 → Skills → リポジトリ構成 → 貢献 → ライセンス (サードパーティ通知は `###`) の順で一致 | ✅ 一致 |
| 開発者向け手順の不在 | `README.md` 全文 | 全文精読。`ANDROID_HOME` / `local.properties` / `DEVELOPER_DIR` / `gradlew` / `swift build` / 検証ホスト起動 / モジュール一覧のいずれも存在しない。「リポジトリ構成」節 (`README.md:145-156`) はディレクトリ表 + `AGENTS.md` / `kasane/concepts/index.md` リンクのみ | ✅ 一致 |
| 導入手順の委譲 | `README.md:38` / `README_ja.md:38` | インストール節は「依存宣言と prerelease 指定だけ」を明示し `skills/README.md` (ja は `skills/README_ja.md`) へ委譲。IDE 操作・module 説明・要件表は不在 | ✅ 一致 |
| 最小コード例の platform 対応 | `README.md:74-135` / `README_ja.md:74-135` | Python でコードブロックを抽出し `skills/{en,ja}/kssettingsview-{ios,android,maui}/SKILL.md` の「Minimal working example」/「最小動作コード」直後のブロックと文字列比較 → en 3 件・ja 3 件すべて `True`。AiForms Skill は該当見出しを持たず (`## Minimal migration`)、README にも 4 例目がないため比較対象外 | ✅ 一致 |
| prerelease の取得方法 | `README.md:48` (SwiftPM) / `:58` (Maven) / `:68` (NuGet) | 現物確認。SwiftPM = semver tag を `from:` / `exact:` で指定、Maven = `X.Y.Z-{alpha\|beta\|rc}.N`、NuGet = `Version` に `X.Y.Z-beta.N` + `--prerelease`。3 ecosystem すべて記載あり | ✅ 一致 |
| サードパーティ通知の所在と誤読防止 | `README.md:168-172` / `README_ja.md:168-172` | ライセンス節配下の `### Third-party notices` に Material Symbols (Apache 2.0) を記載。末尾に「applies only to icons used by the sample applications; it does not describe a dependency of the KsSettingsView library itself」(ja 同旨) の否定文あり | ✅ 一致 |

### Requirement: スクリーンショットの提示

| Scenario | 実装 | 確認方法と結果 | 状態 |
|---|---|---|---|
| 英日での画像共有 | `README.md:21-24` / `README_ja.md:21-24` | 両者とも `assets/ios-modern.png` `assets/ios-classic.png` `assets/android-modern.png` `assets/android-classic.png` を参照。差は alt テキストのみ (`iOS Modern style` ↔ `iOS Modernスタイル`) | ✅ 一致 |
| 4 枚の組み合わせの網羅 | `assets/*.png` 4 枚 | 4 枚を実際に閲覧。iOS×{Modern,Classic}・Android×{Modern,Classic} が 1 枚ずつ。4 枚とも画面は「Section 装飾デモ (style 切替)」、スクロール位置は最上部、装飾プリセット「既定」で一致。差は platform と style セグメントの選択状態のみ | ✅ 一致 |
| 端末固有情報の不在 | `assets/*.png` 4 枚のステータスバー | 4 枚を実際に閲覧。iOS = 時刻 9:41 / Wi-Fi / 充電アイコン、キャリア名なし。Android = 時刻 9:41 / 電池アイコンのみ、キャリア名・残量数値なし。実機時刻・残量表示ともになし。撮影条件 (`simctl status_bar override` / SystemUI demo mode、実機除外) は `ui/brief.md` の「撮影条件」に記録あり | ✅ 一致 |
| (承認ゲート) | `ui/brief.md` 「承認」節 | 承認日 2026-08-29 / オーナー承認済み (「そのまま採用」)、採用候補 → 配置先の対応表あり。`assets/*.png` と `ui/references/*.png` は `cmp` で 4 件ともバイト一致 | ✅ 一致 |
| (配置) 主な特徴の直後 | `README.md:19` | スクリーンショット節が「概要と主な特徴」の直後、「対応プラットフォーム」の前。MAUI は画像を置かず `README.md:26` / `README_ja.md:26` の 1 文で補足 | ✅ 一致 |

### Requirement: 配信準備中の状態表記

| Scenario | 実装 | 確認方法と結果 | 状態 |
|---|---|---|---|
| 解除箇所の単一性 | `README.md:1` / `README_ja.md:1` | `grep -nEi 'prepar\|not (yet )?(publish\|available\|released)\|unpublish\|coming soon\|準備中\|未配信\|未公開\|公開前'` → 各ファイル 1 行目のみヒット。インストール節 (36-68 行) には 0 件 | ✅ 一致 |
| 配布座標の書き方 | `README.md:42-68` | インストール節を `grep -Ei 'source\|local\|clone\|submodule\|ProjectReference\|path'` → 0 件。SwiftPM / Maven / NuGet いずれも公開レジストリ前提の座標のみで、代替手順を持たない | ✅ 一致 |
| (API 安定性の区別) | `README.md:17` / `README_ja.md:17` | 「The public API may introduce breaking changes while the project remains on `0.x` versions.」を概要節に常設記述として配置。1 行目の状態表記とは別箇所 | ✅ 一致 |

### Requirement: 英日 README の翻訳ロックステップ

| Scenario | 実装 | 確認方法と結果 | 状態 |
|---|---|---|---|
| 節構成の一致 | `README.md` / `README_ja.md` | `grep -n '^#'` の出力を照合。見出しの行番号・階層 (`#` / `##` / `###`) ・並びが 17 個すべて完全一致 | ✅ 一致 |
| (同時コミット) | 両ファイルとも未 commit の作業ツリー上に揃って存在 | git status: `README.md` = M、`README_ja.md` = ?? (新規)。片方だけの状態は生じていない | ✅ 一致 |

### Requirement: 開発者向け知識の所在

| Scenario | 実装 | 確認方法と結果 | 状態 |
|---|---|---|---|
| 知識の正の逆転の解消 | `kasane/concepts/maui/api/native-bridge.md` / `kasane/concepts/maui/architecture/binding-build-integration.md` (新規) | `grep -n 'README' kasane/concepts/maui/api/native-bridge.md` → 0 件 (旧版は `git show HEAD:` で 89・91 行目に「正は `maui/README.md`」が 2 箇所)。binding 構成は native-bridge.md「binding 構成の要点」、生成経路・既知の制約・SDK 更新時の再検証箇所は binding-build-integration.md (`## Native artifact の生成` / `## 既知の制約` / `## SDK 更新時に再検証する箇所`) にある。`kasane/concepts/` 全体で README を知識の正とする参照は残存 0 件 | ✅ 一致 |
| 移送した手順の到達可能性 | `kasane/concepts/cross/conventions/local-development-setup.md`(新規):34-62 | 現物確認。`## Android SDK ロケーション` 配下に `### ANDROID_HOME を使う` と `### local.properties を使う` があり、`samples/android/local.properties` と `android/local.properties` の 2 つが必要である旨 (composite build のため Android Studio は included build 側を生成しない旨を含む) が読める | ✅ 一致 |
| Sample の実行手順の到達可能性 | 同上:96-224 | 現物確認。`## Sample を実行する` に iOS Native / Android Native / MAUI iOS / MAUI Android、`## 本体 source へステップインする` に iOS / Android / MAUI の 3 platform。`## デモ画面一覧はどこを見るか` で 3 platform の `SampleScreen` 実ソースを正と明記 (一覧の転記なし)。MAUI のステップインは移送元不在のため新規記述 (deviation 記録あり)、保証範囲を facade の C# source までと限定 | ⚠️ deviation 記録済み |
| 検証ホストの起動手順の到達可能性 | `kasane/concepts/maui/conventions/integration-host-verification.md`(新規) | 現物確認。`## Xcode の選択` (26-29 行) で `DEVELOPER_DIR` の明示を指示、`## IntegrationHost` 配下に `### iOS` / `### Android` の起動手順と `### 期待される表示`、`## MauiHost` 配下に iOS / Android の手順、`## 完了条件` あり | ✅ 一致 |

補: 移送先 3 本は `kasane/concepts/{cross,maui}/index.md` に 1 行ずつ登録済み、`kasane/concepts/log.md` に created 3 件 + updated 1 件を記録済み (tasks 1.8)。

### Requirement: 貢献方針の表明

| Scenario | 実装 | 確認方法と結果 | 状態 |
|---|---|---|---|
| 投稿前の到達経路 | `.github/CONTRIBUTING.md`(新規) | GitHub が contributing guidelines として認識する `.github/` 直下に配置されていることをファイル存在で確認。本文 `## Contribution policy` に「we do not accept pull requests from external contributors」と Issue で受ける旨、`## How to open an issue` に Issue テンプレート選択画面への導線あり | ✅ 一致 |
| README だけを読む人への到達 | `README.md:158-162` / `README_ja.md:158-162` | 現物確認。PR 非受付・GitHub Issues で受ける旨・テンプレート使用の依頼の 3 点が貢献節にある | ✅ 一致 |
| (日本語版の存在) | `.github/CONTRIBUTING_ja.md`(新規) | 英日とも方針の理由 (Kasane ワークフロー内で文脈・設計判断・検証根拠を保つ) と Issue の書き方 (バグ / 提案それぞれの必要項目) を記載。相互リンク `CONTRIBUTING.md` ↔ `CONTRIBUTING_ja.md` はリンク解決検査でともに解決 | ✅ 一致 |

### Requirement: Issue テンプレートの必須項目

| Scenario | 実装 | 確認方法と結果 | 状態 |
|---|---|---|---|
| 証拠なしのバグ報告の抑止 | `.github/ISSUE_TEMPLATE/bug_report.yml`(新規) | `yaml.safe_load` で parse OK。field 一覧 = `version`(input) / `platform`(dropdown) / `reproduction_steps`(textarea) / `actual_behavior`(textarea) / `expected_behavior`(textarea)、**5 件すべて `validations.required: true`**。再現手順を空にすると送信不可 | ✅ 一致 |
| テンプレートの迂回不可 | `.github/ISSUE_TEMPLATE/config.yml`(新規) | `yaml.safe_load` → `{'blank_issues_enabled': False, 'contact_links': []}`。空 Issue の選択肢が出ない | ✅ 一致 |
| exploration への写像 | `.github/ISSUE_TEMPLATE/feature_request.yml`(新規) | parse OK。field = `problem_to_solve` / `current_impact` / `alternatives_considered` の 3 件、すべて `required: true`。ラベルは英語 (`Problem to solve` / `Current impact` / `Alternatives considered`)。`kasane/changes/archive/*/exploration.md` の実物節名は `## 課題 / 動機` と `## 検討した選択肢` で、Problem to solve → 課題 / 動機、Alternatives considered → 検討した選択肢 に一対一で写る。description にも「becomes the problem and motivation for exploration」「becomes the alternatives considered during exploration」と明記 | ✅ 一致 |
| (英日どちらでも可の案内) | 両 yml の先頭 `markdown` ブロック | 「You may write your answers in English or Japanese.」を両フォームに配置 | ✅ 一致 |

### Requirement: 配布座標の文書間の一致

| Scenario | 実装 | 確認方法と結果 | 状態 |
|---|---|---|---|
| 仮名の解消 | `skills/{en,ja}/kssettingsview-ios/SKILL.md:29,39,45` | `grep -rn 'KsSettingsView-SPM'` → 本文 URL・`.package(url:)`・`.product(name:..., package:)` の 3 箇所×2 言語すべて `KsSettingsView-SPM`。`grep -rn 'KsSettingsView-Swift'` を `kasane/` `openspec/` `.git` 除外で実行 → **0 件** (残存は change 成果物とロードマップ内の記述のみで公開面ではない) | ✅ 一致 |
| README と Skills の一致 | `README.md:44,54,64` ↔ `skills/{en,ja}/*/SKILL.md` | grep で照合。SwiftPM = `https://github.com/kamusoft/KsSettingsView-SPM` + `from: "0.1.0"` (README ↔ ios SKILL 一致)、Maven = `jp.kamusoft:kssettingsview:0.1.0` (README ↔ android SKILL 一致)、NuGet = `KsSettingsView.Maui` `Version="0.1.0"` (README ↔ maui SKILL / aiforms SKILL 一致)。3 platform とも同一値 | ✅ 一致 |

---

## 2. docs-refresh

### MODIFIED Requirement: コード正の機械チェック

| Scenario | 実装 | 確認方法と結果 | 状態 |
|---|---|---|---|
| ツール最低バージョンの変更 | `.agents/skills/docs-refresh/SKILL.md:174-180` (Step 3d) | `git diff` で確認。3d が「3 種」→「**1 種**」になり、表は 1 行 (ツール最低バージョン) のみ。突合先は「ルート README 群の対応プラットフォーム表・開発環境要件、および該当記載を持つ場合は各 `SKILL.md` の導入節」。「差分があった項目は該当 README / Skill ファイルを要追従リストへ追加する」の本文は維持、`--readme-only` 時の Skill 側報告のみ扱いも維持 | ✅ 一致 |
| Sample デモ画面の追加 | 同上:182 | 旧②行 (突合先 `samples/*/README.md`) が表から削除され、代わりに引用注記「従前の…『Sample デモ画面一覧』の突合は**行わない**…Sample の実ソースにデモ画面が増減しても、この手順は要追従リストに何も追加しない」 | ✅ 一致 |
| モジュール構成の変更 | 同上:182 | 旧①行 (取得元に `android/settings.gradle.kts` の `include`) が表から削除され、同注記が「モジュール一覧…の突合は行わない」を宣言 | ✅ 一致 |
| 旧指示の残存がないこと | `.agents/skills/docs-refresh/SKILL.md` 全体 | `grep -nE 'android/README\|maui/README\|samples/[^ ]*README\|モジュール表\|モジュール一覧\|デモ画面一覧'` → 3 箇所ヒット。(a) L36 = 追従対象表直後の範囲注記 (「platform / Sample ディレクトリには README を置かない」「`maui/spike/README.md` は含めない」)、(b) L182 = 廃止理由の注記、(c) L366 = 委譲プロンプトの再導入禁止指示。いずれも動作指示ではなく、廃止の根拠 / 再導入の禁止 / 対象範囲の否定的定義。Step 4 実行例・整合性チェック 6-⑦の注記・完了サマリからは `git diff` のとおり除去済み | ⚠️ deviation 記録済み |

> ⚠️ の補足: deviation.md「docs-refresh SKILL.md に残した意図的な言及」は (b)(c) の 2 箇所を列挙し、「旧指示は動作指示を指し、廃止の根拠と再導入の禁止はこれに当たらない」と解釈を記録している。(a) は deviation の列挙に含まれていないが、同じ解釈 (対象範囲の否定的定義であり動作指示ではない) の適用先であり、かつ ADDED Requirement「追従対象の README 群」が `maui/spike/README.md` の除外を SHALL で要求するため記述自体が必要。記録済み解釈の射程内と判定し ❌ にはしない。

### ADDED Requirement: 追従対象の README 群

| Scenario | 実装 | 確認方法と結果 | 状態 |
|---|---|---|---|
| 追従対象の列挙 | `skills/.manifest.json` の `readmes` | `python3 -c "json.load(...)['readmes']"` → `["skills/README.md", "skills/README_ja.md", "README.md", "README_ja.md"]` の 4 枚ちょうど。旧 5 件 (`android/` `maui/` `samples/*`) は `git diff` のとおり削除、`README_ja.md` を追加。`maui/spike/README.md` は含まれない | ✅ 一致 |
| ルート README の言語ペア | `.agents/skills/docs-refresh/SKILL.md:36` / `:355` | L36「ルート `README.md` と `README_ja.md` は**同一の委譲単位**として扱い、要追従になったときは常に両方を同時に更新する」。5b テンプレートの対象ファイル欄も「en/ja ペアがある場合は両方を同一構成で同時に更新すること」「`<stem>.md` と `<stem>_ja.md` は同一ディレクトリのペアとして扱う」。完了サマリ例も「README 2 = 1 ペア」に更新済み | ✅ 一致 |

---

## 3. 追加検査

| 検査 | 結果 |
|---|---|
| tasks.md 全タスク完了 | 8 グループ 42 タスクすべて `[x]`。`git diff` の差分は 42 挿入 / 42 削除ですべてチェックボックス行のみ (タスクの追加・削除・書き換えなし) |
| 虚偽チェックの有無 | 対応表と突き合わせ、**虚偽なし**。抜き取り検証した例: 1.12 (`public-identifiers.md` の記述制限 2 項目削除 → `git diff` で実削除を確認)、4.1 (旧 README 5 枚 → git status `D` 5 件)、6.4 (manifest 4 枚 → json 実値)、7.1 (`KsSettingsView-SPM` 3 箇所 → grep)、8.4 (lint 実行 → 下記) |
| 逆流検査 (足場凍結) | `git status kasane/changes/consolidate-readmes-and-contribution/` → `proposal.md` / `design.md` / `specs/repository-docs/spec.md` / `specs/docs-refresh/spec.md` はいずれも提案 commit (a15b907) から**未変更**。変更されているのは `tasks.md` (チェックのみ) と `ui/brief.md` (承認記録の追記) だけ。逆流なし |
| 未記録乖離 | **なし**。❌ 0 件。⚠️ 2 件はいずれも deviation.md に記録あり |
| 付随修正 | deviation.md の `[付随修正]` 2 件 (`performance-verification.md` の参照差し替え 2 箇所、`public-identifiers.md` の H1 追加) を `git diff` で実在確認。いずれも Requirement を持たないため対応表の対象外。diff 中に Scenario にも `[付随修正]` にも対応しない変更は見当たらない |
| UI 変更 (brief 承認記録) | `ui/brief.md`「承認」節に承認日・オーナー承認・採用対応表・撮影条件・申し送り (Sample UI が日本語固定のため英語 README にも日本語 UI 画像が載る) を記録済み |
| lint | `python3 scripts/local-path-lint.py` → exit 0。未追跡の新規ファイル 10 本 (`README_ja.md` / `.github/**` / 新規 concept 3 本) を `--paths` で明示指定して再実行 → exit 0。`python3 scripts/identity-lint.py` → exit 0 |
| 内部リンク解決 | ルート README 2 枚・CONTRIBUTING 2 枚・新規 concept 3 本・`native-bridge.md` の全 Markdown リンクを走査 → **broken 0 件** |
| テスト | 本変更にユニットテストの対象はない。deviation 記録の根拠となる MAUI facade テストのみ再実行: `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` → **失敗 0 / 合格 516 / 合計 516 (336 ms)**。`cross/conventions/test-execution.md` の MAUI 節が記す「516 件 / 0 失敗」と一致 |
| 相方セカンドオピニオン | deviation.md に未実施 (利用枠枯渇) と申し送りが記録済み。verify の判定対象外 |

---

## 判定

**VALID** — 全 31 Scenario (repository-docs 25 / docs-refresh 6) が「✅ 一致」または「⚠️ deviation 記録済み」。❌ 0 件、虚偽チェックなし、足場への逆流なし、lint / リンク解決 / 再実行テストすべて成功。
