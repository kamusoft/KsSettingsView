# spm-distribution (delta)

## ADDED Requirements

### Requirement: umbrella product の一本化

`ios/Package.swift` は product として `KsSettingsView` 1 本のみを公開し、`KsSettingsViewCore` / `KsSettingsViewUI` / `KsSettingsViewSwiftUI` の 3 target を束ねる (SHALL)。target 構成 (名前・依存・path)・module 名・platforms 指定は変更しない (SHALL)。`KsSettingsViewBridge` target は引き続き product として公開しない (SHALL)。

#### Scenario: package 定義の確認
- **GIVEN** `ios/` の Swift package
- **WHEN** `swift package dump-package` で package 定義を読む
- **THEN** products は `KsSettingsView` 1 件のみで、その targets に `KsSettingsViewCore` / `KsSettingsViewUI` / `KsSettingsViewSwiftUI` の 3 つが含まれ、targets の名前・依存・path と platforms 指定は変更前の定義と一致する

#### Scenario: 既存 target のビルド・テスト維持
- **GIVEN** product 一本化後の `ios/Package.swift`
- **WHEN** iOS Simulator destination で package の全テストを実行する (検証 CI `verify-ios.yml` と同じ経路)
- **THEN** ビルドとテストが成功し、実行されたテスト件数は 1 件以上である

### Requirement: monorepo 内消費者の umbrella product への追随

`samples/ios/KsSettingsViewSample.xcodeproj` と `ios/binding/KsSettingsViewBridge.xcodeproj` は、削除された旧 3 product ではなく umbrella product `KsSettingsView` をリンクしてビルドできる (SHALL)。

#### Scenario: iOS Sample のビルド
- **GIVEN** umbrella product 1 本をリンクする productRef へ差し替えた Sample プロジェクト
- **WHEN** Sample を iOS Simulator 向けにビルドする
- **THEN** ビルドが成功する

#### Scenario: binding framework のビルド
- **GIVEN** umbrella product 1 本をリンクする productRef へ差し替えた binding プロジェクト
- **WHEN** `KsSettingsViewBridge` framework をビルドする
- **THEN** ビルドが成功し、xcframework 生成の工程が従来どおり完了する

### Requirement: スナップショット同期スクリプト

`scripts/spm-snapshot/` の同期スクリプトは、引数で渡されたチェックアウト済み配信リポジトリ作業コピーに対し、`.git/` 以外の既存内容を除去した上で、ホワイトリスト 5 点 — `ios/Package.swift` (ルートへ)・`ios/Sources/`・`ios/Tests/`・monorepo ルート `LICENSE` のコピー・誘導 README — のみを配置する (SHALL)。実行は冪等である (SHALL)。

破壊的操作の前に次を全件検証し、1 つでも失敗したら同期先を一切変更せず非ゼロ終了する (SHALL):

1. コピー元 5 点がすべて存在すること
2. 同期先が canonical path 化の上で git top-level ディレクトリであること
3. 同期先の `origin` remote URL が配信リポジトリ `KsSettingsView-SPM` を指すこと
4. 同期先が monorepo 自身またはその祖先ディレクトリでないこと

スクリプトは git 操作 (commit / tag / push) を行わず、git metadata (HEAD / index / refs / remote 設定) を変更せず、ネットワーク操作を行わない (SHALL)。

#### Scenario: ホワイトリスト 5 点の配置
- **GIVEN** 検証を満たす空の作業コピー (`.git/` のみ)
- **WHEN** 同期スクリプトを実行する
- **THEN** 作業コピー直下に `Package.swift` / `Sources/` / `Tests/` / `LICENSE` / `README.md` の 5 点だけが存在する

#### Scenario: 列挙外ファイルの混入防止
- **GIVEN** 作業コピーに前回スナップショットの残骸や無関係なファイルが存在する
- **WHEN** 同期スクリプトを実行する
- **THEN** `.git/` 以外の列挙外ファイルはすべて除去され、ホワイトリスト 5 点だけが残る

#### Scenario: 冪等性
- **GIVEN** 同期スクリプトを一度実行した作業コピー
- **WHEN** 同じ入力でもう一度実行する
- **THEN** 作業コピーの内容は 1 回目の実行結果と同一である

#### Scenario: 同期先の誤指定の拒否
- **GIVEN** 同期先として git top-level でないディレクトリ、`origin` が配信リポジトリを指さない git リポジトリ、または monorepo 自身のいずれかを渡す
- **WHEN** 同期スクリプトを実行する
- **THEN** スクリプトは非ゼロ終了し、同期先の内容は一切変更されない

#### Scenario: コピー元不足時の無変更
- **GIVEN** コピー元 5 点のいずれかが存在しない状態
- **WHEN** 同期スクリプトを実行する
- **THEN** スクリプトは非ゼロ終了し、同期先の内容は一切変更されない

#### Scenario: git 非操作
- **GIVEN** コミット履歴を持つ作業コピー
- **WHEN** 同期スクリプトを実行する
- **THEN** `.git/` は保持され、新しい commit / tag は作られず、HEAD・index・remote 設定は実行前と同一である (変更は未コミットの working tree として残る)

### Requirement: 配信リポジトリの初期状態

配信リポジトリ `KsSettingsView-SPM` は次の初期状態を持つ (SHALL): public / default branch `main` / Issues・Wiki・Projects・Discussions 無効 / GitHub Actions workflow と branch protection なし / description と Website は monorepo を指す。ルートの README は monorepo (ソース・Issue 窓口) への誘導のみを内容とする (SHALL)。

#### Scenario: 初期設定の機械検証
- **GIVEN** 作成済みの配信リポジトリ
- **WHEN** `gh api` / `gh repo view` でリポジトリ設定を照会する
- **THEN** visibility は public、default branch は `main`、Issues・Wiki・Projects・Discussions は無効、description / Website は monorepo を指す

#### Scenario: 誘導 README の内容
- **GIVEN** 初回 push 後の配信リポジトリ
- **WHEN** ルートの README を読む
- **THEN** monorepo の URL への誘導が含まれ、インストール手順・Issue 窓口は monorepo 側を参照するよう案内されている

### Requirement: 配信リポジトリの https 解決

配信リポジトリ `KsSettingsView-SPM` に push されたスナップショットは、消費者プロジェクトから `https://github.com/kamusoft/KsSettingsView-SPM` の URL と semver tag 指定で SwiftPM 解決でき、umbrella product `KsSettingsView` をリンクして各 module (`KsSettingsViewCore` / `KsSettingsViewUI` / `KsSettingsViewSwiftUI`) を import できる (SHALL)。検証に用いる prerelease tag (`X.Y.Z-alpha.N` 形式) は検証完了後 (失敗時の後始末を含む) に削除し、配信リポジトリに tag を残さない (SHALL)。

#### Scenario: 実リモートからの依存解決とビルド
- **GIVEN** スナップショットと prerelease tag (`X.Y.Z-alpha.N` 形式) が push された配信リポジトリ
- **WHEN** 一時消費者プロジェクトが https URL + その tag を exact 指定で依存に追加し、3 module それぞれの公開型を最低 1 つ参照するコードを iOS Simulator 向けにビルドする
- **THEN** 依存解決とビルドが成功する

#### Scenario: 検証用 tag の後始末
- **GIVEN** https 解決の検証が完了した配信リポジトリ
- **WHEN** リモートの tag 一覧を照会する
- **THEN** tag は存在しない (検証用 prerelease tag は削除済み)
