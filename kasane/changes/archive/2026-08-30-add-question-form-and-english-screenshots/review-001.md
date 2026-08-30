# レビュー結果: add-question-form-and-english-screenshots (001 回目)

**日付**: 2026-08-30
**判定**: APPROVED

## サマリー

デルタスペック 2 本の Requirement / Scenario をすべて成果物で確認し、いずれも満たされている。Issue Form 3 本目の新設・CONTRIBUTING の英日ロックステップ・スクリーンショット 4 枚の英語差し替え・docs-refresh の `maui/spike` 言及削除は、いずれも spec と tasks の記述どおりに実装されており、tasks.md の 25 チェックに虚偽は見つからなかった (17 件の受け入れ検査をレビュー側で独立に再実行し、全件同じ結果を得た)。Non-Goal (ルート README 本文 / `samples/` の恒久差分 / `maui/spike/` の削除 / GitHub 設定) への踏み込みもなく、足場アーティファクト (`proposal.md` / `specs/**`) は未改変。指摘は書きぶりの揃えに関する Suggestion 2 件のみで、いずれも実装の適合性を左右しない。

### ビルド・テストの扱い

`git diff HEAD --stat` および未追跡ファイルの全件を確認したところ、変更は Markdown 4 / YAML 1 / PNG 4 のみで、**ソースコード・ビルド構成・テストコードの変更が 1 件もない** (`ios/` `android/` `maui/` `samples/` はすべて無差分)。したがってビルド・テストの結果はこの diff によって変化しえず、実行しても回帰検出力を持たない。代わりに tasks.md 4 章の受け入れ検査 (Requirement 別) をレビュー側で独立に再実行した。結果は下の「確認した観点」に記す。

## 指摘事項

### [🔵 Suggestion] `question.yml` の自由記述欄に記入例 (placeholder) がない

**該当箇所**: `.github/ISSUE_TEMPLATE/question.yml:31-45` (`attempts` / `references_consulted`)

**問題点**: 既存 `bug_report.yml` は `reproduction_steps` に複数行の `placeholder` を置いて、投稿者が何をどの粒度で書けばよいかを例示している。新設した `question.yml` の 2 つの textarea には `placeholder` がない。とくに `references_consulted` (Skill / README の参照箇所) は利用者に馴染みのない項目で、`description` の文だけでは「Skill のページ名を書くのか、URL を貼るのか、見出しを写すのか」が分かれやすい。空欄では送信できない必須項目なので、迷った投稿者が形だけ埋めて実質的な情報が得られない Issue になる余地がある。

なお `feature_request.yml` も `placeholder` を持たないため、**tasks 1.2 が求める「既存 2 本と書きぶりを揃える」という観点では現状で揃っている**。spec の要求 (必須項目・ラベル言語・言語案内・既定ラベル) もすべて満たしており、適合性の問題ではない。

**推奨修正**: 任意。`references_consulted` に `placeholder` を 1 つ足すなら、`e.g. skills/en/kssettingsview-ios/SKILL.md "Cell types", README.md "Getting started"` のような具体例が有効。既存 `feature_request.yml` と揃える方針を優先して現状維持でもよい。

### [🔵 Suggestion] CONTRIBUTING の 3 段落で「いつこのテンプレートを使うか」の案内の有無が揃っていない

**該当箇所**: `.github/CONTRIBUTING.md:18` / `.github/CONTRIBUTING_ja.md:18`

**問題点**: 追加した質問の段落だけが「何を書くか」に加えて「どういうときにこのテンプレートを使うか」(使い方が分からないとき / 不具合か判断できないとき) を持ち、既存のバグ報告・提案の段落は「何を書くか」だけで終わっている。3 段落を並べて読むと、種別間で粒度が一段ずれて見える。

ただし spec Requirement「貢献方針の表明」が要求するのは **英語版と日本語版が同一の種別を同一の粒度で扱うこと**であり、英日は完全に対応している (英 2 文 / 日 2 文、内容も一致) ため spec 違反ではない。またこの案内文は cross/ADR-0024 が phase-2 へ残した宿題 (「仕様か不具合か判断できない利用者の行き先」) に直接答える文であり、質問段落に置く実利がある。

**推奨修正**: 任意。揃えるなら既存 2 段落にも 1 文ずつ振り分け先の案内を足す (例: バグ報告に「再現手順を示せるとき」、提案に「解決したい課題があるとき」) が、英日 2 ファイル両方に手を入れることになるため、本変更の範囲としては現状のままでも妥当。

## 確認した観点 (指摘に至らなかったもの)

### repository-docs / Requirement: Issue テンプレートの必須項目

| Scenario | 検査 | 結果 |
|---|---|---|
| 質問の受け口の存在 | `.github/ISSUE_TEMPLATE/*.yml` から config を除くと `bug_report` / `feature_request` / `question` のちょうど 3 本。3 本とも `yaml.safe_load` で parse 成功し、`name` / `description` / `body` を保持 | ✅ |
| 同上 (ラベル) | `labels` = `['bug']` / `['enhancement']` / `['question']`。3 種とも GitHub 既定ラベル | ✅ |
| 証拠なしの質問の抑止 | `question.yml` の `required: true` = `version` / `platform` / `attempts` / `references_consulted` の 4 件。spec が挙げる「バージョン・platform・試したこと・参照した Skill / README の箇所」と一対一で対応 | ✅ |
| 証拠なしのバグ報告の抑止 (回帰) | `bug_report.yml` の `required: true` = `version` / `platform` / `reproduction_steps` / `actual_behavior` / `expected_behavior` の 5 件。既存契約が維持されている | ✅ |
| exploration への写像 (回帰) | `feature_request.yml` の `required: true` = `problem_to_solve` / `current_impact` / `alternatives_considered` の 3 件で不変 | ✅ |
| テンプレートの迂回不可 | `config.yml` は `{'blank_issues_enabled': False, 'contact_links': []}` で未変更 | ✅ |
| ラベル言語・言語案内 | 3 本ともフィールドラベルは英語。冒頭 `markdown` ブロックに "You may write your answers in English or Japanese." を持ち、`title` 接頭辞も `[Bug]: ` / `[Feature]: ` / `[Question]: ` で揃っている | ✅ |

### repository-docs / Requirement: 貢献方針の表明

- 種別 (バグ報告 / 提案 / 質問) × 言語 (英 / 日) の 6 マスがすべて埋まっており、各マスの内容が英日で対応していることを本文照合で確認した (質問: 英 `.github/CONTRIBUTING.md:18` / 日 `.github/CONTRIBUTING_ja.md:18`)。
- ルート `README.md` / `README_ja.md` は無差分。貢献節 (`README.md:161`) は「用意された Issue テンプレートを使う」としか書いておらずテンプレート本数に依存しないため、3 本化による陳腐化も起きていない (Non-Goal 遵守)。
- リポジトリ全体を `ISSUE_TEMPLATE|bug_report|feature_request|Issue テンプレート|Issue template` で走査し、「2 本」を前提とする現行文書が残っていないことを確認した (`kasane/decisions/cross/0024-*.md` は既に 3 本へ改訂済み。ヒットするのは phase 議論の history / archive 済み change の記録だけで、いずれも当時の記録として正しい)。

### repository-docs / Requirement: スクリーンショットの提示

4 枚すべてを実際に開いて目視した。比較のため `git show HEAD:assets/*.png` で差し替え前の日本語版も同様に目視した。

| Scenario | 検査 | 結果 |
|---|---|---|
| 英日での画像共有 | `README.md:23-24` と `README_ja.md:23-24` が同一の 4 パスを参照。差は alt テキスト / キャプションの言語のみ | ✅ |
| 4 枚の組み合わせの網羅 | iOS × {Modern, Classic} / Android × {Modern, Classic} が 1 枚ずつ。4 枚とも同一のデモ画面・最上部スクロール・プリセット Default。style セグメントの選択状態のみが style 差、解像度・ステータスバー様式のみが platform 差 | ✅ |
| 英語表示 | 画面タイトル `Section decoration (style switch)`、`Decoration preset` / `Default`、Cell (`Airplane Mode` / `Battery`)、header `Appearance` / `Example with a border`、footer 2 本とも英語。`Wi-Fi` / `Bluetooth` / `True Tone` / `demoAP-0a1b2c-5` / `sectionBorderWidth: 2` は brief が言語非依存として据え置くと定めたもの | ✅ |
| 端末固有情報の不在 | iOS は時刻 9:41・Wi-Fi アイコン・充電中を示す電池アイコンのみでキャリア名なし。Android は 9:41 と満充電アイコンのみ (SystemUI demo mode で wifi/mobile/bluetooth/notifications を非表示化)。端末名・通知・実残量の表示なし | ✅ |
| 表示文字列の可読性 | 4 枚とも省略記号・重なり・不自然な折り返しなし。画面タイトルは iOS / Android とも 1 行に収まる (対訳表を `Section decoration demo (style switching)` から短縮した判断は brief に理由付きで記録され、承認記録にも明記されている) | ✅ |
| Sample の無改変 | `git status --short samples/` が空。作業ツリー全体でも `samples/` 配下に追跡・未追跡の差分なし | ✅ |
| brief の撮影条件との一致 | PNG ヘッダから iOS = 1206×2622 / Android = 1080×2400 を読み、brief「撮影の実績」表と一致。`ui/references/` の 4 枚と `assets/` の 4 枚は SHA-256 が全件一致し、承認された候補がそのまま配置されている | ✅ |

補足 2 点 (いずれも実装の不備ではない):

- 4 枚とも画面下端で最後の footer / Cell が viewport 端に達して途切れる。差し替え前の日本語版を同条件で見比べたところ、**同じ位置・同じ量の途切れ**であり、レンダリング上の切れではなくスクロール位置に起因する viewport 端の露出である。Requirement は同時に「4 枚を同一スクロール位置 (最上部) で撮る」ことを課しており、画面より長いコンテンツでは下端の露出は不可避なので、「表示文字列の可読性」は描画の切れ・重なり・折り返しを指すと解した。この点は brief「撮影の実績」末尾にも記録済みで、承認もその状態に対して与えられている。
- `ui/references/` に候補 4 枚が `assets/` と byte 同一で残る。ksn-core の媒体ホワイトリストでは `ui/references/` の書き手は explore / propose とされているが、本 change の brief 冒頭が「HTML モックを作らず撮影候補そのものを承認対象とする」と定め、tasks 2.2 が明示的に `ui/references/` を置き場に指定しているため、実装はアーティファクトの指示どおり。archive 時は `distill.archive-media` の既定で削除される。

### docs-refresh / Requirement: 追従対象の README 群

| Scenario | 検査 | 結果 |
|---|---|---|
| 公開されない資産への言及の不在 | `.agents/skills/docs-refresh/SKILL.md` を `maui/spike` で grep して 0 件 | ✅ |
| 追従対象の列挙 | `skills/.manifest.json` の `readmes` は `skills/README.md` / `skills/README_ja.md` / `README.md` / `README_ja.md` の 4 枚で不変 | ✅ |
| ルート README の言語ペア | `SKILL.md:36` の「ルート `README.md` と `README_ja.md` は同一の委譲単位」の記述、および `--readme-only` の定義 (`SKILL.md:55` / `:122` / `:343`) は無改変 | ✅ |

削除した 1 文が担っていた「`maui/spike/README.md` を追従対象から外す」機能が失われていないかを別途確認した。`SKILL.md` は追従対象を manifest の `readmes` 配列だけで決めており、ツリーを走査して README を発見する手順を持たない (3c の網羅検査は concepts が対象、3d のコード正チェックは取得元パスが列挙式)。よって削除による挙動変化はない。なお同じ段落に残る「platform ディレクトリ (`android/` `maui/`) には README を置かない」は、`maui/spike/README.md` が存在する私有ツリーでは字面上ずれるが、これは削除前から同居していた既存文であり、公開ツリーでは真になる。spec が `maui/spike` への言及の不在を課している以上、この文を注釈で補うことは spec と衝突するため復活を推奨しない。

### 横断

- `python3 scripts/local-path-lint.py` / `python3 scripts/identity-lint.py` をレビュー側で再実行し、いずれも exit 0 (指摘 0 件)。brief に追記された撮影コマンドは iOS の UDID を `<ios-udid>` プレースホルダに置き換えてあり、Android は `emulator-5554` というエミュレータ固有名のみで実機識別子を含まない。
- 足場凍結: `git diff HEAD -- proposal.md specs/` が空。改変されたのは checkbox 状態のみの `tasks.md` と、tasks 2.3 が記録を指示している `ui/brief.md` (対訳表の実績反映・撮影の実績・承認記録の追記) だけで、いずれも規律の範囲内。
- Non-Goal 遵守: ルート README 無差分 / `samples/` 無差分 / `maui/spike/` 無差分 / `.github/ISSUE_TEMPLATE/config.yml` 無差分 (GitHub 設定変更なし)。
- diff 範囲外の混入 (一時スクリプト・作業ファイルの取り残し) はなし。未追跡ファイルは `question.yml` と `ui/references/` の 4 枚だけ。

### lessons

`kasane/lessons/code-review.md` の重点観点 [L-001] (ミューテーションによる検出力実測) は、本変更にテストコードが存在しないため適用対象なし。「指摘しないこと」は現時点で登録なし。

## アクションプラン

1. (任意) `question.yml` の `references_consulted` に記入例 `placeholder` を足すか、既存 2 本との揃えを優先して現状維持かを決める — Suggestion 1
2. (任意) CONTRIBUTING の種別間で「いつ使うか」の案内粒度を揃えるか、質問段落のみ厚い現状を許容するかを決める — Suggestion 2

いずれも APPROVED の判定を変えるものではなく、対応せずに次工程 (蒸留 / 公開ツリー作成) へ進んでよい。
