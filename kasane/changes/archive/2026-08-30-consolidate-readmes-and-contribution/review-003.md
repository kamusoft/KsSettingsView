# レビュー結果: consolidate-readmes-and-contribution (003 回目)

**日付**: 2026-08-29
**判定**: APPROVED

## サマリー

`review-002.md` が挙げた 7 件 (Critical 1 / Minor 2 / Suggestion 3 / 申し送り実体化 1) は **7 件すべて現物で解消を確認**した。焦点だった `README_ja.md:162` は `.github/CONTRIBUTING_ja.md` を指すようになり、`.github/CONTRIBUTING.md` ⇔ `CONTRIBUTING_ja.md` の相互リンクも解決する。`test-execution.md` の MAUI 節移動はソート済み全行の突き合わせで**欠落・重複ゼロ**を確認した — 旧版から消えた行は意図的に書き換えた 4 行だけで、それ以外はすべて保存されている。

`deviation.md` には ADR-0006 の参照切れ (蒸留への supersede 判断送り) と、見送り 3 件 (英語 README のスクショ言語 / Issue の質問窓口 / `user-skill-api-listing.md` の manifest 配置判断) が実体として記録された。#15 は phase-2 の「Discussions OFF 確定済み」との衝突まで書かれており、`publish-procedure.md:35` を実際に開いて記述の正しさを確認した。

1・2 周目で解消済みとされた範囲は非破壊。修正が持ち込んだ新規の劣化は、`deviation.md` のパス表記 1 箇所 (🔵) のみで、内容の誤りではない。

## (a) review-002 の指摘 7 件の解消可否

| # | review-002 の指摘 | 判定 | 根拠 |
|---|---|---|---|
| 1 | 🔴 `README_ja.md` の貢献導線 | **解消** | `README_ja.md:162` = `[貢献ガイドライン](.github/CONTRIBUTING_ja.md)`。英語側 `README.md:162` は `.github/CONTRIBUTING.md` のままで、言語別リンクの対称性が成立。`.github/CONTRIBUTING_ja.md:3` → `CONTRIBUTING.md` / `.github/CONTRIBUTING.md:3` → `CONTRIBUTING_ja.md` の相互リンクも実在・解決 |
| 2 | 🟡 maui/ADR-0006 の参照切れが deviation 未記録 | **解消** | `deviation.md:73-76`「2026-08-29 accepted ADR からの参照切れ (蒸留への申し送り)」。ADR の該当行 (`kasane/decisions/maui/0006-android-binding-gradlew-exec.md:23`) を開いて引用の逐語一致を確認。移送先 (`binding-build-integration.md` の「SDK 更新時に再検証する箇所」節) と、表 → ADR の導線 (同節冒頭 + 「関連」の 1 行) も現物で確認。**supersede の要否は蒸留 (ksn-distill) へ送る**と明記されている |
| 3 | 🟡 `local-development-setup.md` の冒頭宣言 | **解消** | `:11` が「…Sample を開いて実行し、**本体モジュールをビルド / lint し**、本体 source へデバッガでステップインするまでの手順をまとめる」へ。frontmatter `description` (:3)・`cross/index.md:17` の 1 行説明と主題 3 つが揃った (入口 3 箇所の追随完了) |
| 4 | 🔵 `test-execution.md` の節順と列挙順 | **解消** | 本文は `## iOS` (:17) → `## Android` (:64) → `## MAUI` (:98) の順。`:15`「iOS / Android / MAUI の 3 platform を記載する」と frontmatter `description` の列挙順に一致。`## MAUI` 見出しの出現回数は 1 (重複なし)。review-002 が推した「MAUI 節を末尾へ」の側を採っており、最長の Android 節が中央に来る形も解消済み |
| 5 | 🔵 `log.md` の `created:` 3 行 | **解消** | 3 行すべての末尾付近に「初見可読性レビュー実施済み (独立文脈、Major 4 件を反映)。」を追記。`git diff` 上、`log.md` の変更は依然として**末尾への純粋な追記のみ** (既存行の移動・改変なし) で append-only を維持 |
| 6 | 🔵 `user-skill-api-listing.md` が Step 3c に残る件 | **解消 (申し送りとして)** | `deviation.md:84` の第 3 項が「先行 change 由来で本変更の責ではないが、`excluded` へ 3 本を足した結果 Step 3c の失敗要因はこの 1 本だけになった。docs-refresh SKILL.md は『配置判断はスキルが独断で決めない』と定めるため実装側では追加せず、次回 docs-refresh 通常実行時にオーナー判断が要る 1 件として申し送る」と記録。SKILL.md:142-172 の Step 3c スクリプトを実行 → `UNCOVERED: cross/conventions/user-skill-api-listing.md` の 1 本のみで、本変更由来の 3 本は消えている (`DELETED` も 0) |
| (c) | 申し送り 2 件の実体化 | **解消** | `deviation.md:80-84`「2026-08-29 本変更で見送った改善 (phase-2 への申し送り)」に 2 件 + 上記 #6 を記録。**#14** は相方案 (b)「英語 README のキャプションに `screenshots from the Japanese-locale sample app` を添える」を採らなかった理由まで記載。**#15** は「`contact_links` は本変更の成果物内のフィールドであり『リポジトリ設定だから範囲外』ではない」と review-002 の指摘どおりに理由づけを訂正したうえで、`publish-procedure.md` が **Discussions OFF を確定済み**であり窓口を置くならこの決定の見直しが要る点まで書いている |

**解消 7 / 未解消 0。**

#15 の申し送り内容の事実確認: `kasane/roadmaps/package-distribution/phases/phase-2-public-readiness/artifacts/publish-procedure.md:35` に `Discussions OFF` が実在し、`agenda.md:26` の public 化手順 (5) も同じ。deviation の主張は正確。

## (b) 修正で触った箇所の新規劣化

### `test-execution.md` の MAUI 節移動 — 欠落・重複なし

移動の検査は目視ではなく機械的に行った。`git show HEAD:` の旧版と現行版の**全行をソートして差分**を取り、失われた行が意図的な書き換え 4 行に限られることを確認した。

失われた行 (すべて意図的):

- frontmatter の `description` / `tags` / `timestamp` の旧 3 行
- `現時点では iOS と Android を記載する。MAUI (...) は、実際に実行して確かめた時点で追記する` (:15 の旧文)
- `### README の \`swift test\` との関係` の見出しと本文 1 行 (削除済み README を前提にしていた記述。1 周目の Major 修正)

追加された行はすべて MAUI 節 (見出し 3・コードブロック 1・箇条書き 3・段落 1) と上記の置換文。**旧版の他の行はすべて現行版に保存されている**。節の前後関係も `## iOS` → `## Android` → `## MAUI` → `## 関連` で壊れていない。

MAUI 節の記述内容も実行で裏取りした:

```
成功!   -失敗:     0、合格:   516、スキップ:     0、合計:   516、期間: 331 ms
```

`:106` の「2026-08-29 実測: 516 件 / 0 失敗、約 0.3 秒」と `:107` が示す出力形式 (`失敗: N、合格: M、スキップ: K、合計: T`) がいずれも実出力と一致する。ビルドも成功 (ソース・ビルドファイルへの diff はゼロなので当然だが、ゲートとして実行した)。

節移動に伴う参照切れも無い — `local-development-setup.md:211` は「[テスト実行規約](test-execution.md) の **MAUI 節**が正」と節名で指しており行番号に依存しない。`:158` の「テストの実行方法と完了判定は…が正であり、本節はビルドのみを扱う」も維持され、`local-development-setup.md` に `dotnet test` / `swift test` / `gradlew test` は **1 件も残っていない** (重複なし)。

### `README_ja.md` / `local-development-setup.md` / `log.md` / `deviation.md`

- `README_ja.md`: 変更は :162 の 1 行のみ。見出し 17 個の行番号が英語版と完全一致したまま (下記 (c))、コードブロック 6 個も英日で逐語一致を維持
- `local-development-setup.md`: :11 の 1 句追加のみ。文が長くなりすぎておらず、既存の 2 文目 (Android SDK / Xcode) との重複もない
- `log.md`: 追記の位置・形式ともに先行事例と整合。ただし先行 `user-skill-api-listing.md` 行が「(2026-08-29 オーナー合意、初見可読性レビュー実施済み)」と括弧内にまとめるのに対し、今回は独立文として置く形。記録としては十分で、指摘には至らない
- `deviation.md`: 追記 2 節。事実誤りなし (上記 ADR-0006:23・publish-procedure.md:35 を現物照合)。パス表記に 1 件の 🔵 (下記)

### 指摘

#### [🔵 Suggestion] `deviation.md` の申し送りに 1 箇所だけリポジトリ相対でないパスがある

**該当箇所**: `deviation.md:83`

**問題点**: 申し送り #15 の末尾が

> `roadmaps/package-distribution/phases/phase-2-public-readiness/artifacts/publish-procedure.md` が **Discussions OFF を確定済み**であり

と書いており、先頭の `kasane/` が欠けている。ksn-core `references/paths.md` は「コード・テスト・`kasane/` の他の層 → リポジトリルートからの相対パス」と定めており、同じ `deviation.md` 内の他の参照 (`:32` `kasane/concepts/maui/conventions/performance-verification.md`、`:74` `kasane/decisions/maui/0006-...:23`、`:75` `kasane/concepts/maui/architecture/binding-build-integration.md`) はすべて `kasane/` 付きで書かれている。この 1 行だけが例外。

実害: 小。ただし本項目は **archive 後に phase-2 の議論から参照されることを目的に書かれた申し送り**であり、辿る側が最初に叩くパスが解決しないのは目的に直接効く。

**推奨修正**: 先頭に `kasane/` を付ける。

## (c) 1・2 周目で解消済みとされた範囲の非破壊

すべて再実測した。破壊なし。

| 検査 | 方法 | 結果 |
|---|---|---|
| 英日 README の見出し階層一致 | `grep -n '^#\{1,4\} '` を両ファイルで取り並置 + レベル列の `diff` | 見出し 17 個、**行番号まで完全一致**、レベル列も一致 (`# / ## ×7 / ### ×3 / ## ×4 / ### ×1` … 完全同型) |
| 最小コード例 3 platform × en/ja | README の code fence 6 個を抽出し、`skills/{en,ja}/kssettingsview-{ios,android,maui}/SKILL.md` の fence 集合と照合 | 6 組すべて **exact 一致**。README 英日の同ブロック同士も逐語一致 |
| 内部リンクの解決 | `kasane/concepts/**` `kasane/decisions/**` `skills/**` `.github/**` ルート README 2 枚 `AGENTS.md` `CLAUDE.md` docs-refresh SKILL.md の計 **187 ファイル**の相対リンクを解決 | 破断 **0**。唯一の非解決 `.agents/skills/docs-refresh/SKILL.md:670 -> {target}` は埋め込み Python スクリプト内の f-string 変数 (HEAD 時点にも同一で存在) であり、Markdown リンクではない |
| README 集合 5 枚 | 公開ドキュメント面の `README*.md` 列挙 | ルート 2 (`README.md` / `README_ja.md`)・`skills/` 2・`maui/spike/` 1 の **5 枚ちょうど**。他は `kasane/changes/archive/**/verification|evidence/README.md` (対象外) |
| 削除済み README への live 参照 | `android/README.md` `maui/README.md` `samples/*/README.md` を全 md/json/yml で grep | live 文書での言及は `.agents/skills/docs-refresh/SKILL.md:182` の廃止理由注記のみ (deviation 記録済みの意図的残置)。他はロードマップの歴史記録 (本変更で未改変) と change 成果物 |
| lint 2 本 | `python3 scripts/local-path-lint.py` / `scripts/identity-lint.py` | ともに **exit 0** |
| 足場 (proposal / design / specs) の無改変 | `git diff HEAD --stat` を 3 パスへ限定 | **diff なし** (凍結維持)。`tasks.md` の変更はチェックボックスと 8.4 の記述是正のみで、ksn-core の凍結対象外 |
| 配布座標 | `KsSettingsView-Swift` を公開面で grep | 公開面 **0 件**。残存はロードマップの歴史記録と change 成果物のみ |
| Issue Forms | YAML パース + `required` 抽出 | `bug_report.yml` = 必須 5 (`version` / `platform` / `reproduction_steps` / `actual_behavior` / `expected_behavior`)、`feature_request.yml` = 必須 3 (`problem_to_solve` / `current_impact` / `alternatives_considered`)、`config.yml` = `blank_issues_enabled: false` |
| スクリーンショット参照 | 英日 README の Screenshots 節を並置 | 4 枚とも**同一パス** (`assets/{ios,android}-{modern,classic}.png`) を参照し、alt テキストのみ言語別。MAUI の補足文も両言語に存在 |
| manifest | `readmes` の内容 | 4 枚 (`skills/README.md` / `skills/README_ja.md` / `README.md` / `README_ja.md`) — tasks 6.4 の要求どおり |
| ビルド・テスト | `dotnet test maui/KsSettingsView.Maui.Tests/...` | ビルド成功 / **失敗 0・合格 516・スキップ 0** |

### 観測 (指摘ではない)

`kasane/roadmaps/package-distribution/phases/phase-9-docs/agenda.md` の TODO 2 件 (`:161` native-bridge.md の参照解消 / `:166` iOS 配布座標の確定値追従) が未チェックのまま残っているが、いずれも本変更のタスク 1.3 / 7.1 で完了済みの作業である。ロードマップ artifacts の追随は蒸留 (ksn-distill) の Step で行われるものであり、本変更の diff 範囲外。**蒸留時に閉じる項目として記録しておく。**

`cross/index.md:11-17` の conventions 節は項目間に空行が混じった loose list になっているが、これは本変更以前から (`aiforms-origin-reference` / `aiforms-spec-summary` / `comment-policy` の追記時に) 存在する形で、新規 1 行はアルファベット順の正しい位置に挿入されている。本変更の劣化ではない。

## アクションプラン

1. **🔵** — `deviation.md:83` のパスに `kasane/` を付ける (1 語)。判定を保留する性質のものではないので、蒸留のついでで構わない
2. **蒸留 (ksn-distill) への引き継ぎ**:
   - maui/ADR-0006 の supersede 要否の判断 (`deviation.md:73-76`)
   - `local-development-setup.md` の分割提案 (初見可読性レビュー Suggestion s6。`deviation.md:70` が申し送り済み)
   - phase-9-docs agenda の TODO 2 件 (`:161` / `:166`) の完了マーク
   - phase-2 への申し送り 2 件 (`deviation.md:82-83`) の受け皿への転記

## 確認した観点 (指摘に至らなかったもの)

- **仕様充足**: `verify-001.md` が VALID 判定を出した範囲を再検査に含めた (README 集合・翻訳ロックステップ・最小コード例・配布座標・Issue Forms・内部リンク・lint)。今回の修正で崩れたものはない
- **tasks.md の虚偽チェック**: 全 33 項目が `[x]`。抜き取りで 1.3 (`native-bridge.md` の「正は `maui/README.md`」2 箇所解消)・4.1 (旧 README 5 枚の削除 = `git status` の `D` 5 件)・6.4 (manifest `readmes` 4 枚)・7.1 (`KsSettingsView-SPM` 3 箇所 × 2 言語)・8.1 (README 5 枚)・8.4 (lint exit 0)・8.7 (Issue Forms 必須項目) を現物で確認 — 虚偽なし
- **deviation 記録済みの乖離**: 移送対応表の包括解釈・MAUI ステップイン手順の新規記述・グループ 4/6 の実施主体・docs-refresh SKILL.md の意図的言及 3 箇所・相方セカンドオピニオン未実施・ビルドコマンドの主題別配分・初見可読性レビューの反映 — いずれも合意済み差分として違反扱いしていない
- **付随修正の同梱条件**: `performance-verification.md` の参照差し替え 2 箇所と `public-identifiers.md` の H1 追加は、本務 (README 廃止に伴う知識の正の移動) が直接の原因。ksn-core の同梱条件内で、1・2 周目の判定を維持
- **`log.md` の append-only**: diff が末尾追記のみであることを再確認 (既存 328 行に手が入っていない)
- **docs-refresh Step 3c / `--readme-only`**: 3c は残 1 本 (申し送り済み)。`--readme-only` が取る `readmes` は 4 枚で SKILL.md の定義と一致
