# Tasks: rollout-user-skills

## 1. 準備 (生成の前提)

- [x] 1.1 `docs/legacy-aiforms-reference.md` を `kasane/concepts/cross/conventions/aiforms-spec-summary.md` へ移送する — frontmatter (type: reference・凍結注記)、本文のローカル絶対パス・`file://` リンクの無害化 (リモート URL / `../<リポジトリ名>/` 形式へ変換、local-path lint 通過)、aiforms-origin-reference.md との相互リンク (docs/ 直リンクの差し替え)、cross index.md / log.md 登載 (→ Requirement: 移植元仕様要約の concepts 移送)

## 2. skills/ 8 部の生成 (Skill 単位 fan-out、各ワーカーが en/ja ペアを同一文脈で同時生成)

各ワーカーへの委譲プロンプトに内容規約 (→ Requirement: 生成の内容規約 ①〜⑤) と設計原則 (→ Requirement: Skill 内容の設計原則、frontmatter の標準準拠、翻訳ロックステップ) を明記する。源泉は concepts (日本語正本)、旧 docs はコード例の素材としてのみ使用。各ワーカーは生成した各ファイルごと (言語抜き相対パス単位) の源泉 concepts マップを報告する。

- [x] 2.1 `kssettingsview-ios` en/ja 生成 (SKILL.md + references 4 本) (→ Requirement: Skill 一式の構成)
- [x] 2.2 `kssettingsview-android` en/ja 生成 (SKILL.md + references 4 本) (→ Requirement: Skill 一式の構成)
- [x] 2.3 `kssettingsview-maui` en/ja 生成 (SKILL.md + references 4 本。旧 docs に原型なし・新規書き起こし) (→ Requirement: Skill 一式の構成)
- [x] 2.4 `kssettingsview-aiforms-migration` en/ja 生成 (SKILL.md + references/api-mapping.md。新規書き起こし。源泉に aiforms-spec-summary.md を含む) (→ Requirement: Skill 一式の構成)
- [x] 2.5 `skills/.manifest.json` v3 の草案作成 — 各ワーカー報告のファイル別マップから targets を構成し、残る concept (集合は index / log / rules 除外) を excluded (空でない理由つき) へ配置。readmes に索引 2 枚 + ルート README を含む README 群を列挙。網羅検査 (4.1 ①) に使用し、最終書き出しは 7.1 で行う (→ Requirement: manifest 初期版)

## 3. 索引と導線

- [x] 3.1 `skills/README.md` + `skills/README_ja.md` の作成 (3 要素のみ) (→ Requirement: 索引 README)
- [x] 3.2 ルート README への導線追記 (主な特徴直後の 1 文 + モノレポ構成表の `skills/` 行。他の節は触らない) (→ Requirement: ルート README への導線)

## 4. 機械検査 (テスト相当 — Scenario が指標)

- [x] 4.1 完了検査一式の実行: ①concepts 網羅 (manifest 草案)、②en/ja 節構成一致、③コードブロック byte 一致、④frontmatter、⑤旧名残 grep、⑥内部リンク解決、⑦identity-lint、⑧配信識別子ゆれ grep、⑨ローカル絶対パス lint、⑩ツール最低バージョン一致 (ビルドファイル ↔ SKILL.md 導入節)。失敗分を修正して再実行 (→ Requirement: 完了検査一式、翻訳ロックステップ、manifest 初期版、frontmatter の標準準拠)

## 5. レビュー

- [x] 5.1 独立レビュー (ksn-review、Skill 単位): 源泉 concepts との内容整合 (targets の各 concept の内容が担当 Skill に反映されているかを 1 件ずつ確認) / 設計原則の遵守 / en/ja の等価性 / API 署名・コード例のコード・テストとの突合 / ja description の英語キーワード適切性 (→ Requirement: Skill 内容の設計原則、生成の内容規約)
- [x] 5.2 初見レビュー: concepts もコードも読んでいない新鮮なエージェントに Skill 本文だけを渡し、「この文書だけで利用目的を達成できるか / 宙に浮いた参照・新造語がないか」を報告させる
- [x] 5.3 指摘の反映と機械検査の再実行 (4.1 の再通過)
- [x] 5.4 オーナー目視検収 (少なくとも ja 4 部の通し読み) — 指摘 12 件はドキュメント反映 (サイクル 3) + 実装課題 3 件の簡易起票で処理、反映は review-002 追補 2 で APPROVED 維持

## 6. docs/ の廃止と残記述整理 (検収通過後)

- [x] 6.1 concepts の docs/ 前提記述の差し替え: comment-policy 対象外リスト (skills/ の別規約注記を含む) / test-execution の README・docs 節 (→ Requirement: docs/ の廃止と残記述整理)
- [x] 6.2 README 群の docs/ リンクの解消: ルート README (7 箇所) / `android/README.md` / `samples/ios/README.md` / `samples/android/README.md` の docs/ 参照を対応する Skill・skills 索引へ差し替え、または記述ごと除去 (→ Requirement: ルート README への導線)
- [x] 6.3 `trash docs/` (.manifest.json 含むディレクトリごと) と `kasane/config.yaml` の整理 (lint.exclude の docs/ 除外 + コメント、identity.scope の docs) (→ Requirement: docs/ の廃止と残記述整理)
- [x] 6.4 残存検査: docs/ の不在と、凍結資料を除く docs/ 参照 0 件の grep 確認 (→ Requirement: docs/ の廃止と残記述整理)

## 7. manifest 確定と最終検査 (docs/ 廃止後)

- [x] 7.1 `skills/.manifest.json` の最終書き出し — concept ハッシュを最終状態 (グループ 6 完了後) で再計算し、スキーマ規範検証と作業ツリーとのキー・ハッシュ一致確認 (→ Requirement: manifest 初期版)
- [x] 7.2 完了検査一式の最終実行 (4.1 の全 10 検査 + リポジトリ全体の identity / local-path lint) (→ Requirement: 完了検査一式)
