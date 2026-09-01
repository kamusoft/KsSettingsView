# 一致検証: add-spm-distribution (001 回目)

**日付**: 2026-09-01
**対象**: `specs/spm-distribution/spec.md` (ADDED Requirements 5 本 / Scenario 12 件)
**判定**: **VALID**

## サマリー

デルタスペックの 5 Requirement・12 Scenario すべてに実装と検証手段の対応が付いた。❌ (未記録の欠落・乖離) は 0 件。tasks.md の 12 タスクはすべて対応表と整合し、虚偽チェックはない。足場 (proposal / design / specs) は提案コミット `e1cb0b1` 以降 1 度も書き換えられておらず、逆流もない。テストは本検証で再実行して全件成功を確認した。

deviation.md の実測所見 2 件・付随修正 5 件は、いずれも Requirement を持たない範囲か合意済みの受容であり、対応表の ❌ には結び付かない。

## 実行した検証

| 検証 | コマンド | 結果 |
|---|---|---|
| package 定義 | `swift package --package-path ios dump-package` | products は `KsSettingsView` 1 件のみ (targets: Core / UI / SwiftUI)。platforms は iOS 16.0 / macOS 13.0 |
| scheme 一覧 | `cd ios && xcodebuild -list` | scheme は `KsSettingsView` 1 本 |
| 同期スクリプトのテスト | `scripts/spm-snapshot/sync-snapshot-test.sh` | 41 アサーション全成功 (exit 0) |
| 配信リポジトリ設定 | `gh api repos/kamusoft/KsSettingsView-SPM` (読み取りのみ) | visibility=public / default_branch=main / issues・wiki・projects・discussions すべて false / description・homepage は monorepo を指す |
| 配信リポジトリの tag | `gh api .../tags --jq length` | `0` |
| 配信リポジトリの workflow | `gh api .../actions/workflows --jq .total_count` | `0` |
| branch protection | `gh api .../branches/main/protection` | `Branch not protected` (404) |
| 配信リポジトリのルート内容 | `gh api .../contents` | `LICENSE` / `Package.swift` / `README.md` / `Sources` / `Tests` の 5 点のみ |
| スナップショットの同一性 | `gh api .../contents/Package.swift --jq .sha` と `git hash-object ios/Package.swift` | 双方 `ab81999a…` で一致 |
| iOS 全件テスト・Sample / binding ビルド | review-002.md:18 / review-003.md:16 / review-001.md:19-20 の実測を引用 | 全件テスト `** TEST SUCCEEDED **` 1000 tests / 0 failures、Sample `** BUILD SUCCEEDED **`、binding `** ARCHIVE SUCCEEDED **` + xcframework 2 スライス生成 |

## 対応表

### Requirement: umbrella product の一本化

| Scenario | 実装 | テスト・検証 | 状態 |
|---|---|---|---|
| package 定義の確認 | `ios/Package.swift:30-37` (product は umbrella 1 本)、`ios/Package.swift:44-104` (targets 無変更)、`ios/Package.swift:11-20` (platforms 無変更) | 本検証の `swift package --package-path ios dump-package`。products は `KsSettingsView` 1 件で targets は Core / UI / SwiftUI。`git diff ios/Package.swift` 上、変更は products 節とヘッダコメントのみで targets / platforms の行は 1 つも動いていない | ✅ 一致 |
| 既存 target のビルド・テスト維持 | 同上 | `.github/workflows/verify-ios.yml:88-93` と同じ経路 (`xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,...'`) を review-002 / 003 で実測: `** TEST SUCCEEDED **`、5 バンドル合計 1000 tests / 0 failures (≥ 1 件) (review-002.md:18 / review-003.md:16) | ✅ 一致 |

Requirement 本文の「`KsSettingsViewBridge` target は product として公開しない」も、dump-package の products が 1 件のみであることで確認 (`ios/Package.swift:38-41` に理由コメント)。

### Requirement: monorepo 内消費者の umbrella product への追随

| Scenario | 実装 | テスト・検証 | 状態 |
|---|---|---|---|
| iOS Sample のビルド | `samples/ios/KsSettingsViewSample.xcodeproj/project.pbxproj` — `XCSwiftPackageProductDependency` / `packageProductDependencies` / Frameworks build phase の 3 箇所が `KsSettingsView` 1 本へ集約 (旧 3 productRef を削除) | review-001.md:19 で `xcodebuild build -scheme KsSettingsViewSample -destination 'platform=iOS Simulator,...'` → `** BUILD SUCCEEDED **`。以降の周回で当該 pbxproj は変更されていない | ✅ 一致 |
| binding framework のビルド | `ios/binding/KsSettingsViewBridge.xcodeproj/project.pbxproj` — 同じ 3 箇所が `KsSettingsView` 1 本へ集約 | review-001.md:20 で `./ios/binding/build-xcframework.sh` → `** ARCHIVE SUCCEEDED **` / xcframework 生成 (ios-arm64 + ios-arm64_x86_64-simulator)。以降の周回で当該 pbxproj は変更されていない | ✅ 一致 |

### Requirement: スナップショット同期スクリプト

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| ホワイトリスト 5 点の配置 | `scripts/spm-snapshot/sync-snapshot.sh:149-153` | `scripts/spm-snapshot/sync-snapshot-test.sh:117-136` (直下 5 点のみ・種別・内容一致 8 アサーション) | ✅ 一致 |
| 列挙外ファイルの混入防止 | `scripts/spm-snapshot/sync-snapshot.sh:141-147` (`.git` 以外を `find -maxdepth 1` で除去) | `scripts/spm-snapshot/sync-snapshot-test.sh:140-152` (残骸・隠しファイル・入れ子の残骸を投入して除去を確認) | ✅ 一致 |
| 冪等性 | 同上 (除去 → 再配置の順序) | `scripts/spm-snapshot/sync-snapshot-test.sh:156-166` (2 回目の一覧・全ファイル内容ハッシュが 1 回目と一致) | ✅ 一致 |
| 同期先の誤指定の拒否 | 検証 2: `sync-snapshot.sh:93-107` / 検証 3: `sync-snapshot.sh:109-125` / 検証 4: `sync-snapshot.sh:127-131`, `sync-snapshot.sh:53-57` | `scripts/spm-snapshot/sync-snapshot-test.sh:170-235` — 非 git / git サブディレクトリ / 別 origin / 配信リポジトリ名を含む詐称 URL / monorepo 自身 / monorepo の祖先 の 6 ケース + 祖先判定ロジック単体 5 ケース。いずれも非ゼロ終了かつ同期先無変更 | ✅ 一致 |
| コピー元不足時の無変更 | `scripts/spm-snapshot/sync-snapshot.sh:85-91` (破壊的操作の手前に配置) | `scripts/spm-snapshot/sync-snapshot-test.sh:239-248` (偽 monorepo から `ios/Tests` を欠落させて実行 → 非ゼロ終了・同期先無変更) | ✅ 一致 |
| git 非操作 | `scripts/spm-snapshot/sync-snapshot.sh` 全体に `git commit` / `tag` / `push` / `remote set-url` が存在しない (`git` 呼び出しは `rev-parse --show-toplevel` / `remote get-url` / `status --porcelain` の読み取り 3 種のみ) | `scripts/spm-snapshot/sync-snapshot-test.sh:252-274` (HEAD・commit 数・tag・index・remote URL の不変と `.git` 保持、変更が未コミット working tree に残ることを確認) | ✅ 一致 |

Requirement 本文の「ネットワーク操作を行わない」もスクリプトに fetch / push / curl 等の呼び出しがないことで確認。CI 常設化は `.github/workflows/ci.yml:106-109` (deviation.md の付随修正として記録済み)。

### Requirement: 配信リポジトリの初期状態

| Scenario | 実装 (外部状態) | 検証 | 状態 |
|---|---|---|---|
| 初期設定の機械検証 | 配信リポジトリ `kamusoft/KsSettingsView-SPM` | `evidence/spm-repo-settings.md` + 本検証の `gh api` 再照会。public / `main` / issues・wiki・projects・discussions すべて無効 / description・homepage は monorepo / workflow 0 件 / branch protection なし | ✅ 一致 |
| 誘導 README の内容 | `scripts/spm-snapshot/README.template.md` (同期で `README.md` として配置) | 本検証で `gh api .../contents/README.md` を実読。monorepo URL への誘導、インストール手順は monorepo README 参照、Issue 窓口は monorepo issues への誘導が含まれる。テンプレートと配信リポジトリの内容も一致 | ✅ 一致 |

### Requirement: 配信リポジトリの https 解決

| Scenario | 実装 (外部状態) | 検証 | 状態 |
|---|---|---|---|
| 実リモートからの依存解決とビルド | 配信リポジトリへ push 済みのスナップショット (`Package.swift` の blob sha が monorepo の `ios/Package.swift` と一致) | `evidence/spm-https-resolution.md` — `Package.resolved` に `https://github.com/kamusoft/KsSettingsView-SPM` / `0.1.0-alpha.1` が pin され、`xcodebuild build -scheme SpmConsumer -destination 'platform=iOS Simulator,...'` が `** BUILD SUCCEEDED **` | ✅ 一致 (下記「観察」参照) |
| 検証用 tag の後始末 | 同上 | `evidence/spm-https-resolution.md` の `git ls-remote --tags origin` を `wc -l` に通した結果が 0、および本検証の `gh api .../tags --jq length` = `0` | ✅ 一致 |

## 追加検査

| 検査 | 結果 |
|---|---|
| tasks.md 全タスク完了 | 12 タスクすべて `[x]`。対応表と突き合わせて未実装のチェック済み (虚偽) は **0 件** |
| 逆流検査 (足場凍結) | `proposal.md` / `design.md` / `specs/spm-distribution/spec.md` は `e1cb0b1` (提案化コミット) が唯一の履歴で、working tree にも変更なし。**逆流なし** |
| 未記録乖離 | 対応表に ❌ なし。working tree の変更ファイル (`ios/Package.swift` / 2 つの pbxproj / `scripts/spm-snapshot/` / `.github/workflows/ci.yml` / `.github/workflows/verify-ios.yml` / `ios/binding/build-xcframework.sh` / `maui/.../KsSettingsView.Binding.iOS.csproj` / `kasane/handbook/cross/public-identifiers.md` / `kasane/handbook/cross/test-execution.md`) を全件走査し、Scenario に対応しないものはすべて deviation.md の `[付随修正]` 5 行に記録済み。**未記録の乖離 0 件** |
| 付随修正の照合 | 5 行すべて実物と一致 (verify-ios.yml の scheme 名、handbook 2 本の scheme 識別子と件数の数え方、ci.yml の lint step 追加、csproj の `_XcbInputs` に SwiftUI 追加、Package.swift / build-xcframework.sh のヘッダコメント) |
| 実測所見の照合 | SwiftUI シンボル同梱 (staticlib の dead-strip 不発) は deviation.md にオーナー受容として記録済み。spec の受け入れ条件 (ビルド成功・xcframework 生成完了) は満たしており、Scenario 違反にはならない。`rm -rf` 採用も記録済み |
| UI 変更 | 該当なし (`ui/` アーティファクトを持たない変更) |
| テスト全件成功 | 本検証で `scripts/spm-snapshot/sync-snapshot-test.sh` を再実行し 41 アサーション全成功。iOS の全件テストは review-002 / 003 の実測 (1000 tests / 0 failures) を引用 (以降 `ios/Sources/` `ios/Tests/` は未変更) |

## 観察 (判定に影響しない申し送り)

- **https 解決 Scenario の証跡の粒度**: `evidence/spm-https-resolution.md` は `Package.resolved` と `** BUILD SUCCEEDED **` を残しており、THEN (依存解決とビルドの成功) は証跡で裏付けられている。一方 WHEN の「3 module それぞれの公開型を最低 1 つ参照するコード」については消費者側ソースが残っておらず、記述のみが根拠になる。消費者プロジェクトは spec が「リポジトリ外」と定め、検証用 tag も spec の要求どおり削除済みのため再現は不可能。次回同種の検証では消費者側の `Package.swift` と参照コードの抜粋を evidence に含めると、証跡だけで完結する。
- **配信リポジトリの内容は spec の Scenario 対象外**: 「初期状態」Requirement はリポジトリ設定と README のみを縛るため、`Sources/` / `Tests/` の中身の同一性は契約外。本検証では参考として `Package.swift` の blob sha 一致まで確認した。
