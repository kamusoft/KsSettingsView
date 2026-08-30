# Tasks: retarget-docs-refresh-to-skills

## 1. スキル本文の改修 (.agents/skills/docs-refresh/SKILL.md)

- [x] 1.1 frontmatter (description) と冒頭の目的・対象定義を skills/ + README 群へ書き換える。自動発動禁止の文言を維持する (→ Requirement: 起動規律の維持)
- [x] 1.2 差分検出手順を manifest v3 (`skills/.manifest.json`: concepts ハッシュ + targets + excluded + readmes) ベースへ改修し、スキル本文内の固定影響マップ (旧 Step 2d の表) を撤去する (→ Requirement: manifest v3 に基づく差分検出)
- [x] 1.3 manifest 不在・parse エラー・version ≠ 3 のときの停止と初期生成案内を記述する。旧フルリフレッシュフォールバックを削除する (→ Requirement: manifest 不在・非対応時の停止)
- [x] 1.4 concepts 網羅検査 (未参照かつ未除外の検出 → 配置判断のユーザー提示) を差分更新フローに組み込む (→ Requirement: concepts 網羅検査)
- [x] 1.5 コード正の機械チェック 3 種を「項目・取得元・抽出方法・突合先」の規範表として現行リポジトリの正に合わせて書き直す — AGP / Kotlin は `android/gradle/libs.versions.toml` (version catalog)、Gradle は `android/gradle/wrapper/gradle-wrapper.properties`、minSdk / compileSdk は `android/ks-settingsview-ui/build.gradle.kts`、Swift tools / iOS Deployment Target は `ios/Package.swift`、.NET TFM は `maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj`。Sample デモ一覧の列挙元は iOS / Android / MAUI の 3 platform とも明示する。突合先は README 群 (+記載がある場合の SKILL.md 導入節) (→ Requirement: コード正の機械チェック)
- [x] 1.6 更新方針の提示・承認ゲートを skills/ 対象の提示例つきで書き直す (→ Requirement: 更新方針の承認ゲート)
- [x] 1.7 委譲手順を Skill 単位 + en/ja ペア同時生成へ改修し、プロンプトテンプレート (内容規約 ①〜⑥ を含む。⑥ = API 署名・コード例の実装コード照合と drift 報告) を書き下ろす。README 委譲の単位と最大 3 並列も明記する (→ Requirement: Skill 単位の委譲と en/ja ペア生成 / 生成プロンプトの内容規約)
- [x] 1.8 整合性チェック 8 種の手順と検査スクリプト断片 (節構成一致・コードブロック byte 一致・frontmatter・旧名残 grep・リンク解決・絶対パス・識別子表記ゆれ) を書き下ろす (→ Requirement: 整合性チェック一式)
- [x] 1.9 `--all` / `--readme-only` の定義を再定義後の意味で書き直す。`--readme-only` の manifest 非更新 (concepts / targets / excluded を触らない) と `--all` 同時指定エラーを含める (→ Requirement: 実行フラグ)
- [x] 1.10 manifest v3 の書き出し手順とスキーマ例 (JSON、デルタスペックの規範スキーマと一致させる) を記述する。「最後に書く」規律と、未処理 concept の旧ハッシュ保持を明記する (→ Requirement: manifest の更新 / manifest v3 スキーマの規範)
- [x] 1.11 Guardrails を刷新する: 旧 docs/ 前提の項 (legacy 据え置き・MAUI ガイド未設置等) を整理し、メイン concepts 非読・承認ゲート・自動発動禁止・trash 使用を引き継ぐ (→ Requirement: 起動規律の維持 / 更新方針の承認ゲート)

## 2. 規約記述の更新

- [x] 2.1 AGENTS.md (CLAUDE.md はその写し) の docs-refresh / docs/ 関連 2 記述を「skills/ と README 群の継続的な追従更新は docs-refresh 経由のみ (初期生成・構成の見直しは承認済み change の実装として行う)」へ書き換え、docs/ 言及を除去する (凍結注記は置かない)
- [x] 2.2 kasane/config.yaml の context にある docs/ 記述を skills/ ベースへ書き換える (lint.exclude の docs/ は phase-12 の宿題として触らない)
- [x] 2.3 kasane/config.yaml の `lint.identity.scope` に `skills` を追加する (`docs` は phase-12 で削除されるまで残す) (→ Requirement: 整合性チェック一式)

## 3. 検証 (テスト相当)

- [x] 3.1 改修後スキル本文と phase-11 agenda の決定事項 6 件を 1 件ずつ照合し、反映漏れがないことを確認する
- [x] 3.2 スキルに埋め込む検査スクリプト断片 (bash / python) を実際に実行して構文と検出ロジックを確認する — 全検査 (節構成一致・コードブロック byte 一致・frontmatter・旧名残 grep・リンク解決・identity-lint・識別子表記ゆれ) を一時 fixture の正例・負例の両方で試す (→ Requirement: 整合性チェック一式)
- [x] 3.3 manifest v3 スキーマ例の JSON 妥当性 (parse 可能・必須キー・不変条件の整合) を確認し、デルタスペックの規範スキーマと一致することを照合する (→ Requirement: manifest v3 スキーマの規範 / manifest の更新)
- [x] 3.4 manifest 不在ケースの案内文言が「初期生成は変更フローで」へ誘導することを本文照読で確認する (→ Requirement: manifest 不在・非対応時の停止)
- [x] 3.5 実行フラグの定義 (`--all` / `--readme-only` / 同時指定エラー) と manifest 更新規則 (`--readme-only` 非更新・未処理 concept の旧ハッシュ保持) が spec の Scenario と一致することを本文照読で確認する (→ Requirement: 実行フラグ / manifest の更新)
