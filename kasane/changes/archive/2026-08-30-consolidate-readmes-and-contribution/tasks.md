# Tasks: consolidate-readmes-and-contribution

本変更はドキュメント再編でありコードの振る舞いを変えないため、ユニットテストの対象がない。検証はグループ 8 のチェックで行う (デルタスペックの Scenario がその指標)。

## 1. 移送 (旧 README の廃止より先に行う)

- [x] 1.1 廃止対象 5 枚の全節を A/B/C/D 分類と突き合わせ、`design.md` の移送対応表 (節 → 移送先ファイル) に照らして漏れがないことを確認する (→ Requirement: 開発者向け知識の所在)
- [x] 1.2 MAUI binding の内部割り込み知識・既知の制約・SDK 更新時の再検証箇所を `design.md` の移送対応表が定める `kasane/concepts/maui/` 配下のファイルへ移す (→ Requirement: 開発者向け知識の所在)
- [x] 1.3 `kasane/concepts/maui/api/native-bridge.md` の「正は `maui/README.md`」参照 2 箇所を解消する (→ Requirement: 開発者向け知識の所在)
- [x] 1.4 環境セットアップ手順 (`ANDROID_HOME` / 2 つの `local.properties` / `DEVELOPER_DIR`) を `design.md` の移送対応表が定める `kasane/concepts/cross/conventions/` 配下のファイルへ移す (→ Requirement: 開発者向け知識の所在)
- [x] 1.5 実機目視確認チェックリスト (`samples/ios/README.md`) を `design.md` の移送対応表が定める `kasane/concepts/cross/conventions/` 配下のファイルへ移す (→ Requirement: 開発者向け知識の所在)
- [x] 1.6 検証ホストの起動手順と期待される表示を `design.md` の移送対応表が定める `kasane/concepts/maui/` 配下のファイルへ移す (→ Requirement: 開発者向け知識の所在)
- [x] 1.7 Sample の実行手順・本体ライブラリへのステップイン手順を `kasane/concepts/` へ移す。デモ画面一覧は移送せず、`SampleScreen` の実ソースを正とする旨を記す (→ Requirement: 開発者向け知識の所在)
- [x] 1.8 移送先 concepts の index と timestamp を更新する (→ Requirement: 開発者向け知識の所在)
- [x] 1.11 A 分類 (他所に既にある内容) が実在することを 1 件ずつ確認してから破棄する (→ Requirement: 開発者向け知識の所在)
- [x] 1.12 `kasane/concepts/cross/conventions/public-identifiers.md` の「してはいけないこと」から未公開を理由とした記述制限 2 項目 (開発用 GAV を公開済み座標と説明しない / 実装のない MAUI product ID を現在利用可能と列挙しない) を削除し、timestamp を更新する (→ Requirement: 配信準備中の状態表記 / 配布座標の一貫性)
- [x] 1.13 1.12 の変更が、`public-identifiers.md` を源泉とする Skill (`kssettingsview-aiforms-migration` / `kssettingsview-android`) の記述に影響しないことを確認する (→ Requirement: 配布座標の文書間の一致)

## 2. スクリーンショット

- [x] 2.1 iOS シミュレータで Modern / Classic を撮影し `ui/references/` に候補を置く (→ Requirement: スクリーンショットの提示)
- [x] 2.2 Android エミュレータで Modern / Classic を撮影し `ui/references/` に候補を置く (→ Requirement: スクリーンショットの提示)
- [x] 2.3 候補をユーザーに提示して採用を選び、`ui/brief.md` の承認欄に記録する (→ Requirement: スクリーンショットの提示)
- [x] 2.4 採用画像をルートの `assets/` へ配置する (→ Requirement: スクリーンショットの提示)

## 3. ルート README (英日)

- [x] 3.1 英語 `README.md` を作成する (→ Requirement: ルート README の節構成 / 配信準備中の状態表記 / スクリーンショットの提示 / 貢献方針の表明)
- [x] 3.2 日本語 `README_ja.md` を作成する (→ Requirement: 英日 README の翻訳ロックステップ、および 3.1 と同じ Requirement 群)
- [x] 3.3 インストール節に 3 platform の座標を書く (SwiftPM は `https://github.com/kamusoft/KsSettingsView-SPM`、Maven は `jp.kamusoft:kssettingsview`、NuGet は `KsSettingsView.Maui`) 、および prerelease の取得方法を ecosystem ごとに書く (→ Requirement: 配信準備中の状態表記 / 配布座標の文書間の一致 / ルート README の節構成)
- [x] 3.4 最小コード例を 3 platform ごとに 1 例置き、対応する platform Skill の最小動作コードブロックと一致させる (AiForms 移行 Skill は対象外) (→ Requirement: ルート README の節構成)
- [x] 3.5 サードパーティ通知をライセンス節に入れ、サンプルアプリで使用しているアイコン由来である旨を明記する (→ Requirement: ルート README の節構成)
- [x] 3.6 リポジトリ構成節にディレクトリ表 (`assets/` を含む) と `AGENTS.md` / `kasane/concepts/` へのリンクを書く (→ Requirement: ルート README の節構成)
- [x] 3.7 冒頭に「配信準備中」バナーを 1 行置き、API 安定性の記述は常設として別に書く (→ Requirement: 配信準備中の状態表記)

## 4. 旧 README の廃止

- [x] 4.1 `android/README.md`・`maui/README.md`・`samples/{ios,android,maui}/README.md` を `trash` で削除する (→ Requirement: README の所在)
- [x] 4.2 リポジトリ内 Markdown から廃止した README への参照を解消する (旧ルート README の 3 リンク、`samples/maui/README.md` の相互リンク等) (→ Requirement: README の所在)

## 5. `.github/` 一式

- [x] 5.1 `.github/ISSUE_TEMPLATE/bug_report.yml` を作成する (バージョン / platform / 再現手順 / 実際の挙動 / 期待した挙動 を必須) (→ Requirement: Issue テンプレートの必須項目)
- [x] 5.2 `.github/ISSUE_TEMPLATE/feature_request.yml` を作成する (解決したい課題 / 現状どう困っているか / 考えた選択肢 を必須) (→ Requirement: Issue テンプレートの必須項目)
- [x] 5.3 `.github/ISSUE_TEMPLATE/config.yml` で blank issue を無効化し、テンプレートを迂回できないようにする (→ Requirement: Issue テンプレートの必須項目)
- [x] 5.4 `.github/CONTRIBUTING.md` (英語) を作成する (→ Requirement: 貢献方針の表明)
- [x] 5.5 `.github/CONTRIBUTING_ja.md` (日本語) を作成し、`CONTRIBUTING.md` から相互リンクする (→ Requirement: 貢献方針の表明)

## 6. docs-refresh の対象定義変更

- [x] 6.1 `.agents/skills/docs-refresh/SKILL.md` の追従対象の表を 4 枚へ改める (→ Requirement: 追従対象の README 群)
- [x] 6.2 コード正の機械チェックをツール最低バージョン 1 種へ改める (→ Requirement: コード正の機械チェック)
- [x] 6.3 platform / Sample README への言及とモジュール表確認の指示を全箇所から除去する — 追従対象の表 / Step 3d の突合表 / Step 4 の実行例 / **README 委譲プロンプト (5b) の「README 種別ごとの確認事項」** / 整合性チェック / 完了サマリ (→ Requirement: コード正の機械チェック / 旧指示の残存がないこと)
- [x] 6.4 `skills/.manifest.json` の `readmes` を 4 枚へ更新する (→ Requirement: 追従対象の README 群)

## 7. `skills/` の配布座標修正

- [x] 7.1 `skills/{en,ja}/kssettingsview-ios/SKILL.md` の 3 箇所 (本文 URL・`.package(url:)`・`.product(package:)`) を `KsSettingsView-SPM` へ更新する (→ Requirement: 配布座標の文書間の一致)

## 8. 検証

- [x] 8.1 公開ドキュメント面 (ルート直下と `skills/` `android/` `ios/` `maui/` `samples/` 配下) から `README*.md` を列挙し、5 枚 (ルート 2・`skills/` 索引 2・`maui/spike/` 1) だけであることを確認する。`kasane/` `openspec/` `.claude/` は対象外 (→ Requirement: README の所在)
- [x] 8.2 英日ルート README の見出し階層と並びが一致することを確認する (→ Requirement: 英日 README の翻訳ロックステップ)
- [x] 8.3 現行の公開文書 (ルート README 2 枚・`skills/`・`.github/`・`.agents/skills/docs-refresh/SKILL.md`) から廃止対象への Markdown リンクが残っていないこと、および内部リンクがすべて解決することを確認する (→ Requirement: README の所在)
- [x] 8.4 `scripts/local-path-lint.py` と `scripts/identity-lint.py` を通す。`identity-lint.py` は `lint.identity.scope` により README / `.github/` を検査しないため、この 2 つの証跡になるのは `local-path-lint.py` の側だけ (→ Requirement: ルート README の節構成)
- [x] 8.5 スクリーンショットの受け入れ検査: 4 枚が platform × style の 4 通りを 1 枚ずつ満たすこと、英日 README が同一パスを参照しキャプションのみ言語別であること、同一デモ画面・同一スクロール位置であること、MAUI の補足文があること、ステータスバーに端末を特定できる表示がないことを確認する (→ Requirement: スクリーンショットの提示)
- [x] 8.6 docs-refresh を `--readme-only` で実行し、停止せず 4 枚を対象に取ることを確認する (→ Requirement: 追従対象の README 群)
- [x] 8.7 Issue Forms の静的検査: YAML がスキーマとして妥当であること、バグ 5 項目 / 提案 3 項目がすべて存在し `required: true` であること、`config.yml` が `blank_issues_enabled: false` であること、日英どちらでも投稿してよい案内があること、英日 CONTRIBUTING の相互リンクが解決すること、CONTRIBUTING に PR 非受付の理由と Issue の書き方があること (→ Requirement: Issue テンプレートの必須項目 / 貢献方針の表明)
- [x] 8.8 ルート README と `skills/` の配布座標が文書間で一致し、`KsSettingsView-Swift` が残っていないことを grep で確認する (値の確定は配信フェーズの責務のため、ADR との突合は行わない) (→ Requirement: 配布座標の文書間の一致)
