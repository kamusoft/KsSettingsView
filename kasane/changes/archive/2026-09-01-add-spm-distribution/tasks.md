# Tasks: add-spm-distribution

## 1. Package.swift の umbrella 化

- [x] 1.1 `ios/Package.swift` の products を umbrella `KsSettingsView` 1 本 (3 target 束ね) に置き換える (→ Requirement: umbrella product の一本化)
- [x] 1.2 `swift package dump-package` で product 定義と target 構成・platforms の不変を確認し、iOS Simulator destination で package の全テスト (verify-ios.yml と同じ経路) が 1 件以上実行され成功することを確認する (→ Scenario: package 定義の確認 / 既存 target のビルド・テスト維持)

## 2. monorepo 内消費者の追随

- [x] 2.1 `samples/ios/KsSettingsViewSample.xcodeproj` の productRef を umbrella 1 本へ差し替え、iOS Simulator 向けビルドの成功を確認する (→ Scenario: iOS Sample のビルド)
- [x] 2.2 `ios/binding/KsSettingsViewBridge.xcodeproj` の productRef を umbrella 1 本へ差し替え、framework ビルドと xcframework 生成工程の成功を確認する (→ Scenario: binding framework のビルド)

## 3. スナップショット同期スクリプト

- [x] 3.1 `scripts/spm-snapshot/` に同期スクリプトを実装する — 破壊的操作前の全件検証 (コピー元 5 点 / git top-level / origin remote / monorepo 拒否)、`.git/` 以外の除去、ホワイトリスト 5 点配置、git 非操作、冪等 (design.md Decision 1) (→ Requirement: スナップショット同期スクリプト)
- [x] 3.2 誘導 README テンプレート (monorepo・Issue 窓口への誘導のみ) を `scripts/spm-snapshot/` に置く (→ Scenario: 誘導 README の内容)
- [x] 3.3 スクリプトのテストを作成する — 一時ディレクトリの git リポジトリを作業コピーに見立て、6 Scenario (5 点配置 / 列挙外除去 / 冪等性 / 誤指定拒否 / コピー元不足時の無変更 / git 非操作) を検証する (→ Requirement: スナップショット同期スクリプト)

## 4. 配信リポジトリの作成と解決確認

- [x] 4.1 `KsSettingsView-SPM` を初期設定一式で作成し、`gh api` / `gh repo view` で設定 (public / `main` / Issues・Wiki・Projects・Discussions 無効 / workflow・branch protection なし / description・Website) を機械検証する (design.md Decision 3) (→ Requirement: 配信リポジトリの初期状態)
- [x] 4.2 同期スクリプトの成果物を手動で commit・push し、検証用 prerelease tag (`X.Y.Z-alpha.N`) を打つ (→ Scenario: 実リモートからの依存解決とビルド)
- [x] 4.3 一時消費者プロジェクト (リポジトリ外) から https URL + tag の exact 指定で依存解決し、3 module の公開型を各 1 つ以上参照するコードを iOS Simulator 向けにビルドして成功を確認する (→ Scenario: 実リモートからの依存解決とビルド)
- [x] 4.4 検証用 prerelease tag を削除し、リモートに tag が残っていないことを確認する。検証の証跡 (解決ログ) は change 側に保存する (design.md Decision 2) (→ Scenario: 検証用 tag の後始末)

## 5. ドキュメント追随

- [x] 5.1 `kasane/handbook/cross/public-identifiers.md` に SwiftPM product 行の更新 (3 本 → umbrella 1 本)・Package URL・配信リポジトリ名を追記する (phase-9 申し送りを出典とする承認済み規範改訂)
