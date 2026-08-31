# Tasks: add-verification-ci

## 1. リポジトリ側の設定変更

- [x] 1.1 `kasane/config.yaml` の `lint.identity.scope` に `samples` を追加する (→ Requirement: lint の検証)

## 2. platform 別 reusable workflow

- [x] 2.1 `.github/workflows/` に ios 検証 workflow を作成する — `workflow_call` 対応、`macos-26`、Xcode 26.5 を変数 + `DEVELOPER_DIR` で明示選択 (イメージ内の正確な指定名はマニフェストで確認)、`xcodebuild test -scheme KsSettingsView-Package` を Simulator destination で実行 (→ Requirement: iOS の検証 / platform workflow の再利用契約 / ツールチェーンの再現性)
- [x] 2.2 android 検証 workflow を作成する — `workflow_call` 対応、ubuntu、`setup-java` Temurin 17、Gradle 依存のみキャッシュ (`build/` は対象外)、`./gradlew test` 実行 (→ Requirement: Android の検証と実行件数の担保 / platform workflow の再利用契約)
- [x] 2.3 android の実行件数検査ステップを作成する — 期待する module×variant 集合を `android/settings.gradle.kts` の include (現行 4 module × debug/release = 8 組) から導出し、各組について「テスト結果 XML が存在しない」「`tests` 属性合計が 0」のいずれでも fail、合計件数を job summary へ出力 (→ Requirement: Android の検証と実行件数の担保)
- [x] 2.4 maui 検証 workflow を作成する — `workflow_call` 対応、`macos-26`、ios job と同じ Xcode 26.5 選択 (`DEVELOPER_DIR`)、`setup-dotnet` (global.json 準拠。workload set は既存の `workloadVersion: 10.0.300.3` で固定済み) + workload install、`setup-java` 17 を `JavaSdkDirectory` に接続、NuGet キャッシュ、`dotnet test` (facade) + platform TFM / binding 2 本のビルド (→ Requirement: MAUI の検証 / platform workflow の再利用契約 / ツールチェーンの再現性)
- [x] 2.5 maui の実行件数検査を組み込む — `dotnet test` の結果 (TRX またはコンソールの `合計` / `Total`) から実行件数を取得し、合計 0 件で fail、件数を job summary へ出力 (→ Requirement: MAUI の検証)

## 3. CI 入口と lint

- [x] 3.1 CI 入口 workflow を作成する — pull_request (develop / main) + push (develop) トリガーで 3 platform workflow を `uses:` で呼び、lint job を並置する。job 名 (status check context) は branch protection 登録用に安定した名前で固定する (→ Requirement: CI の起動条件 / マージ保護)
- [x] 3.2 lint job を実装する — gitleaks (action)、`scripts/local-path-lint.py`、`scripts/identity-lint.py`、`scripts/comment-policy-lint.py` を実行 (→ Requirement: lint の検証)
- [x] 3.3 全 workflow に `permissions: contents: read` を明示し、外部 action の参照は commit SHA で固定する (→ Requirement: ツールチェーンの再現性)

## 4. 検証 (Scenario の実機確認)

- [ ] 4.1 draft PR (base: develop) を作成し、4 job がすべて起動・成功することを確認する。各 job の所要時間を記録する。main を base とするトリガー設定は workflow 定義の branches 指定で確認する (→ Scenario: PR で全 job が起動する / main への PR でも起動する)
- [ ] 4.2 iOS job のログで Simulator 実行と実行件数 (`Executed N tests`) を確認する (→ Scenario: Simulator 全件実行)
- [ ] 4.3 android job の summary に合計件数が表示されることを確認する。負ケースとして、期待 8 組のうち 1 組の結果ディレクトリ全体が欠けた状態と `tests` 合計 0 の状態の両方で fail することをステップ単体で確認する (→ Scenario: 全件実行と件数表示 / 0 件実行の検出)
- [ ] 4.4 maui job の summary に実行件数が表示され、0 件で fail することをステップ単体で確認する (→ Scenario: facade テストと配線のコンパイル検証)
- [x] 4.5 lint の負ケースを 4 検査それぞれで確認する — gitleaks は実在しない検証用ダミー文字列 (実秘密は commit しない)、local-path-lint はローカル絶対パス、identity-lint は samples 配下の識別子、comment-policy-lint は規約違反コメント。それぞれ fail し違反箇所が出力されること (→ Scenario: 違反の検出 / samples 配下の識別子検出)
- [ ] 4.6 マージ後の push トリガーで develop 上の実行が走ることを確認する (→ Scenario: develop へのマージ後にも検証される)

## 5. branch protection (GitHub 設定操作)

- [ ] 5.1 `develop` / `main` の branch protection に、3.1 で固定した 4 つの status check context を必須として追加し、PR 必須化を設定する (admin バイパス許容、force-push 禁止・削除禁止は維持)。設定は `gh api` で行い、実行したコマンドを evidence に記録する (→ Requirement: マージ保護)
- [ ] 5.2 設定後に両ブランチの保護設定を `gh api` で再取得し、必須 check 4 つ・PR 必須・force-push 禁止・削除禁止が入っていることを検査する (→ Requirement: マージ保護)
- [ ] 5.3 検査未通過 PR のマージがブロックされることと、直 push が拒否されること (または保護設定の検査で同等確認) を確認する (→ Scenario: 検査未通過のマージ拒否 / 直 push の拒否)

## 備考

- workflow の実装は github-workflow-skill を参照する
- `global.json` の SDK / workload set 固定 (10.0.300 / 10.0.300.3) は既存 — 本変更では触らない (前提条件)
- E2E (検証ホスト実行) は対象外 (proposal Non-Goals)
- 所要時間の実測値が許容できない場合の最適化は別 change (proposal Non-Goals)
