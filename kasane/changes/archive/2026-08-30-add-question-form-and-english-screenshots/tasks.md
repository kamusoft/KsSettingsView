# Tasks: add-question-form-and-english-screenshots

## 1. Issue の質問窓口

- [x] 1.1 `.github/ISSUE_TEMPLATE/question.yml` を新設する。必須項目はバージョン / platform / 試したこと / 参照した Skill・README の箇所。ラベルは英語、本文は英語・日本語どちらでもよい旨を案内し、`labels: [question]` を付ける (→ Requirement: Issue テンプレートの必須項目)
- [x] 1.2 既存 2 本 (`bug_report.yml` / `feature_request.yml`) と項目の書きぶり・言語案内の文面を揃える (→ Requirement: Issue テンプレートの必須項目)
- [x] 1.3 `.github/CONTRIBUTING.md` の「How to open an issue」に質問テンプレートの段落を追加する (→ Requirement: 貢献方針の表明)
- [x] 1.4 `.github/CONTRIBUTING_ja.md` に 1.3 と同一粒度の段落を追加する (英日ロックステップ) (→ Requirement: 貢献方針の表明)

## 2. 英語スクリーンショット

- [x] 2.1 iOS / Android の対象画面 (`SectionDecorationDemoView.swift` / `SectionDecorationDemoScreen.kt` と各 DemoControls・Preset・SampleScreen) を [ui/brief.md](ui/brief.md) の対訳表どおり一時的に英訳する (→ Requirement: スクリーンショットの提示)
- [x] 2.2 brief の撮影の統制に従い 4 枚を撮影し、`ui/references/` へ候補として置く (→ Requirement: スクリーンショットの提示)
- [x] 2.3 候補をオーナーへ提示して承認を得て、`ui/brief.md` の「承認モック」に記録する (→ Requirement: スクリーンショットの提示)
- [x] 2.4 承認された 4 枚で `assets/{ios,android}-{modern,classic}.png` を差し替える (→ Scenario: 4 枚の組み合わせの網羅 / 英語表示)
- [x] 2.5 **2.1 の一時英訳を revert し、`git diff samples/` が空であることを確認する** (→ Scenario: Sample の無改変)

## 3. docs-refresh の追従対象定義

- [x] 3.1 `.agents/skills/docs-refresh/SKILL.md` から `maui/spike` への言及を**削除する** (公開リポジトリに含めないため。追従対象が 4 枚である旨の記述自体は残す) (→ Requirement: 追従対象の README 群)

## 4. 検証

本変更は docs / 設定 / 画像のみでプロダクションコードを含まないため、単体テストの追加対象がない。代わりに Requirement ごとの受け入れ検査を実施する。

### Requirement: Issue テンプレートの必須項目

- [x] 4.1 `.github/ISSUE_TEMPLATE/` の `*.yml` (config.yml を除く) が**ちょうど 3 本**であることを確認する (→ Scenario: 質問の受け口の存在)
- [x] 4.2 3 本を同一の検査で走査し、各ファイルが YAML として解釈でき、`name` / `description` / `body` を備え、`labels` が `bug` / `enhancement` / `question` に対応していることを確認する (→ Scenario: 質問の受け口の存在)
- [x] 4.3 各フォームの必須項目が `validations.required: true` を持つことを確認する — 質問: バージョン / platform / 試したこと / 参照箇所、**バグ報告: バージョン / platform / 再現手順 / 実際の挙動 / 期待した挙動 (回帰確認)**、**提案: 解決したい課題 / 現状どう困っているか / 考えた選択肢 (回帰確認)** (→ Scenario: 証拠なしの質問の抑止 / 証拠なしのバグ報告の抑止)
- [x] 4.4 3 本ともラベルが英語で、本文を英語・日本語どちらで書いてもよい旨の案内を持つことを確認する
- [x] 4.5 `.github/ISSUE_TEMPLATE/config.yml` が `blank_issues_enabled: false` のままであることを確認する (→ Scenario: テンプレートの迂回不可)

### Requirement: 貢献方針の表明

- [x] 4.6 `CONTRIBUTING.md` と `CONTRIBUTING_ja.md` について「種別 (バグ報告 / 提案 / 質問) × 言語 (英 / 日)」の対応表を作り、6 マスすべてが埋まっていることを確認する (→ Scenario: テンプレート種別の網羅)
- [x] 4.7 ルート README 英日の貢献節が未変更であることを確認する (本変更の Non-Goal)

### Requirement: スクリーンショットの提示

- [x] 4.8 差し替え後の 4 枚が [ui/brief.md](ui/brief.md) の撮影条件表 (端末 / 向き / 初期スクロール位置 / 装飾プリセット / ステータスバー) をすべて満たすことを確認する (→ Scenario: 4 枚の組み合わせの網羅)
- [x] 4.9 4 枚を目視し、画面内の表示文字列がすべて英語であることを確認する (→ Scenario: 英語表示)
- [x] 4.10 4 枚を目視し、実機の時刻・実際のバッテリー残量・キャリア名・端末名・通知が写っていないことを確認する (固定デモ表示は可) (→ Scenario: 端末固有情報の不在)
- [x] 4.11 4 枚を目視し、画面タイトル・Section header / footer・Cell の表示文字列に切れ・重なり・不自然な折り返しがないことを確認する (→ Scenario: 表示文字列の可読性)
- [x] 4.12 ルート `README.md` と `README_ja.md` が同一の画像パスを参照したままであることを確認する (→ Scenario: 英日での画像共有)
- [x] 4.13 `git diff samples/` が空であることを確認する (→ Scenario: Sample の無改変)

### Requirement: 追従対象の README 群

- [x] 4.14 `.agents/skills/docs-refresh/SKILL.md` を `maui/spike` で検索し、一致が 0 件であることを確認する (→ Scenario: 公開されない資産への言及の不在)
- [x] 4.15 `skills/.manifest.json` の `readmes` が 4 枚のままであることを確認する (→ Scenario: 追従対象の列挙)
- [x] 4.16 `SKILL.md` のルート README 言語ペアの扱い (`--readme-only` で英日が同一の委譲単位) の記述が変わっていないことを確認する (→ Scenario: ルート README の言語ペア)

### 横断

- [x] 4.17 `scripts/local-path-lint.py` と `scripts/identity-lint.py` を実行し 0 件を確認する
