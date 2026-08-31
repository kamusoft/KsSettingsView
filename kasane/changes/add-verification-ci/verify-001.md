# Verify 001: add-verification-ci

- 検証日: 2026-08-31
- 対象: ブランチ `feature/add-verification-ci` (`0bc699b..72b731b`)
- 正: `kasane/changes/add-verification-ci/specs/verification-ci/spec.md` (Requirement 8 / Scenario 13)
- 合意済み差分: `kasane/changes/add-verification-ci/deviation.md` (4 件)

**判定: VALID (実装範囲)**

実装に落ちる 11 Scenario はすべて ✅ 一致または ⚠️ deviation 記録済み。未記録の欠落・乖離は 0 件。
残る 2 Scenario (Requirement「マージ保護」) と Scenario「develop へのマージ後にも検証される」の実行確認は、
コンテキストパッケージの指示どおり **GitHub 設定・運用操作として未実施 (⏳)** と扱う。アーカイブ可否は
tasks 4.6 / 5.1 / 5.2 / 5.3 の完了を前提とする。

> 補足: 受領したコンテキストパッケージは「Scenario 11 個」としていたが、spec 本文を数えると 13 個
> (マージ保護の 2 個を含む) だった。本表は spec 本文の 13 個をすべて行にしている。

---

## 対応表

状態の凡例: ✅ 一致 / ⚠️ deviation 記録済み / ⏳ 実装は充足・運用操作が未実施 / ❌ 欠落・乖離

### Requirement: CI の起動条件

| Scenario | 実装 | 検証 | 状態 |
|---|---|---|---|
| PR で全 job が起動する | `.github/workflows/ci.yml:9-12` (pull_request: develop/main、paths フィルタなし)、`ci.yml:31-44` (ios / android / maui / lint の 4 job) | run `33354483531` で 4 job すべて起動・成功 (tasks 4.1) | ✅ |
| main への PR でも起動する | `.github/workflows/ci.yml:12` (`branches:` に `main`) | workflow 定義での確認 (tasks 4.1 が指定する確認方法) | ✅ |
| develop へのマージ後にも検証される | `.github/workflows/ci.yml:13-15` (push: develop)、`ci.yml:23-25` (concurrency group を push では `github.sha` 単位にし、`cancel-in-progress` を pull_request のときだけ true にする) | 実行確認は tasks 4.6 が未チェック | ⏳ 実装は充足 / 実行未確認 |

- Requirement 本文の「paths による絞り込みは行わず」は `ci.yml` に `paths` / `paths-ignore` が一切ないことで満たす (`ci.yml:8-15`)。
- `ci.yml:23-25` の concurrency は最終 commit `72b731b` で `github.ref` → `github.sha` に変更されており、**実測済みの run `33354483531` (commit `9642652`) はこの変更を通っていない**。Scenario の充足は定義の読解による。

### Requirement: platform workflow の再利用契約

| Scenario | 実装 | 検証 | 状態 |
|---|---|---|---|
| 別 workflow からの呼び出し | `.github/workflows/verify-ios.yml:10-11` / `verify-android.yml:7-8` / `verify-maui.yml:8-9` (いずれも `on: workflow_call:`、入力なし)、呼び出し側は `ci.yml:33,37,41` の `uses: ./.github/workflows/...` | 3 本とも `workflow_call` 経由で run `33354483531` にて実行・成功。入力・条件分岐を持たないため呼び出し元による内容差は生じない | ✅ |

### Requirement: iOS の検証

| Scenario | 実装 | 検証 | 状態 |
|---|---|---|---|
| Simulator 全件実行 | `.github/workflows/verify-ios.yml:86-93` (`xcodebuild test -scheme KsSettingsView-Package -destination "platform=iOS Simulator,id=..."`、`set -o pipefail` で `tee` 後も失敗が伝播)、destination 解決は `verify-ios.yml:50-84`、実行件数の確認は `verify-ios.yml:95-134` | run `33354483531` の ios job で `Executed 642 tests, with 0 failures`、`KsSettingsViewUITests.xctest` (UIKit ガード内) の実行を確認 (tasks 4.2)。負ケース (実行 0 件 / 件数抽出失敗) はステップ単体で非 0 終了を確認 | ✅ |

- 「`swift test` を成否判定に用いてはならない」: `verify-ios.yml` に `swift test` の呼び出しはない (`swift --version` のみ、`verify-ios.yml:48`)。
- 全テストターゲットの網羅: `ios/Package.swift` の testTarget 4 本 (Core / UI / SwiftUI / Bridge) を集約 scheme `KsSettingsView-Package` (`xcodebuild -list` で存在確認) が包含する。
- ⚠️ 実行 0 件・件数抽出失敗を job の失敗とする点は spec の要求を超える追加であり、deviation「iOS にも実行 0 件のゲートを設ける」に記録済み。

### Requirement: Android の検証と実行件数の担保

| Scenario | 実装 | 検証 | 状態 |
|---|---|---|---|
| 全件実行と件数表示 | `.github/workflows/verify-android.yml:51-53` (`./gradlew --no-daemon --console=plain test`)、期待集合の導出 `verify-android.yml:70-111`、summary 出力 `verify-android.yml:139-156` | run `33354483531` の summary に `合計: 2700 件 (期待 8 組)`、annotation に導出 module 4 件 (`:ks-settingsview-bridge, :ks-settingsview-compose, :ks-settingsview-core, :ks-settingsview-ui`)。`android/settings.gradle.kts` の include 4 件 × debug/release = 8 組と一致 (tasks 4.3) | ✅ |
| 0 件実行の検出 | `verify-android.yml:117-137` (module×variant ごとに結果 XML の欠落 → `failures` 追加、`tests` 合計 0 → `failures` 追加)、`verify-android.yml:158-161` (`failures` があれば `::error::` 出力のうえ exit 1) | 負ケース 2 種 (結果ディレクトリ 1 組欠落 / 1 組で `tests` 合計 0) をステップ単体で実行し、いずれも非 0 終了を確認 (tasks 4.3) | ✅ |

- 「期待する集合を Gradle 構成から導出」: `verify-android.yml:72-87` が `android/settings.gradle.kts` をコメント除去のうえ `include(...)` / `include "..."` 両記法で走査。`verify-android.yml:92-109` が `android/` 直下の `build.gradle.kts` 実体と突き合わせて導出の縮みを検出する。
- 「存在する XML だけを数えると見逃す」への対応: `verify-android.yml:121-125` が XML 不在そのものを失敗にしている。
- 復元されたビルド成果物を実行済みとみなす経路がないこと: キャッシュ対象は `~/.gradle/caches/modules-2` と `~/.gradle/wrapper` のみで `build/` を含まない (`verify-android.yml:38-41`)。

### Requirement: MAUI の検証

| Scenario | 実装 | 検証 | 状態 |
|---|---|---|---|
| facade テストと配線のコンパイル検証 | テスト実行 `.github/workflows/verify-maui.yml:76-81` (TRX 出力)、件数検査と summary `verify-maui.yml:83-124` (`total == 0` で exit 1)、binding 2 本のビルド `verify-maui.yml:126-130`、facade の platform TFM (`net10.0-ios` / `net10.0-android`) ビルド `verify-maui.yml:132-136` | run `33354483531` で `Passed! - Failed: 0, Passed: 516, Total: 516`、summary に `合計: 516 件 (成功 516 / 失敗 0)`、4 ビルドすべて成功 (tasks 4.4)。負ケース (TRX 0 件) はステップ単体で非 0 終了を確認 | ✅ |

- 「検証ホストの実行 (E2E) は行わない」: `verify-maui.yml` に Simulator / Emulator 起動もホストアプリ実行もない。

### Requirement: lint の検証

| Scenario | 実装 | 検証 | 状態 |
|---|---|---|---|
| 違反の検出 | gitleaks `.github/workflows/ci.yml:61-95` (CLI を版 + SHA-256 固定で導入、`git archive HEAD` の追跡内容を走査、走査対象が空でないことの検査を含む)、`ci.yml:97-98` local-path-lint、`ci.yml:100-101` identity-lint、`ci.yml:103-104` comment-policy-lint | 4 検査それぞれの負ケースで非 0 終了と違反箇所の出力を確認 (tasks 4.5)。本検証でも 3 スクリプトを手元実行し全件 exit 0 | ⚠️ deviation 記録済み |
| samples 配下の識別子検出 | `kasane/config.yaml:70-73` (`lint.identity.scope` に `samples` を追加)、実行は `ci.yml:100-101` | samples 配下に識別子を置いた負ケースで identity-lint が検出・fail することを確認 (tasks 4.5) | ✅ |

- ⚠️ gitleaks を action ではなく CLI で実行する点、および走査対象を `git archive HEAD` に限る点は deviation「gitleaks を action ではなく CLI で実行する」に記録済み。spec Requirement は「secret scan (gitleaks)」であって実行形態を規定していないため、要求は満たされている。
- `scripts/identity-lint.py:88-90` (`HOST_LOCAL` の否定先読み追加) と `scripts/identity-lint.py:127-136` (`GREP_PATTERN` から GNU 拡張 `\b` を除去) は deviation `[付随修正]` に記録済み。Requirement を持たないため対応表本体の行にはしない。手元実行で検出 0 件・exit 0 を再確認した。

### Requirement: ツールチェーンの再現性

| Scenario | 実装 | 検証 | 状態 |
|---|---|---|---|
| 版の変更が diff に現れる | ランナーイメージ: `verify-ios.yml:24` / `verify-maui.yml:25` (`macos-26`)、`verify-android.yml:18` / `ci.yml:47` (`ubuntu-24.04`)。Xcode: `verify-ios.yml:19` と `verify-maui.yml:16` (`KS_XCODE_VERSION: "26.5"`)、選択は `verify-ios.yml:33-42` / `verify-maui.yml:34-43` (メジャー.マイナー一致のうち最新パッチ、不在なら fail)。JDK: `verify-android.yml:28-30` / `verify-maui.yml:48-50` (temurin / 17)。.NET: `verify-maui.yml:53-55` (`global-json-file: global.json`) と `global.json` (`sdk.version: 10.0.300` / `workloadVersion: 10.0.300.3`) | run `33354483531` の ios / maui とも `DEVELOPER_DIR=/Applications/Xcode_26.5.0.app/Contents/Developer`。`latest` 系ラベルは 4 job のいずれにも存在しない | ✅ |

- 「Xcode の選択は iOS を扱う全 job (ios / maui) に適用する」: 両 workflow が同一の選択ロジックを持つ。
- 固定境界を越える版の固定 (gitleaks 8.30.1 + SHA-256、`ci.yml:54-55`、外部 action の commit SHA 固定 `ci.yml:59` / `verify-*.yml`) は spec の要求を満たしたうえでの上乗せで、乖離ではない (tasks 3.3)。

### Requirement: マージ保護

| Scenario | 実装 | 検証 | 状態 |
|---|---|---|---|
| 検査未通過のマージ拒否 | status check context を安定させる job 名の固定 `.github/workflows/ci.yml:28-44` (`ios / verify` / `android / verify` / `maui / verify` / `lint`) のみ。branch protection 自体は GitHub 設定操作 | tasks 5.1 / 5.2 / 5.3 が未チェック。context 名 4 つが実測 run で確定していることまでを確認 | ⏳ 未実施 (運用操作) |
| 直 push の拒否 | 同上 | tasks 5.1 / 5.3 が未チェック | ⏳ 未実施 (運用操作) |

- ⚠️ 適用範囲を `develop` のみとし `main` を phase-8 へ申し送る点は deviation「マージ保護の適用範囲を develop のみとする」に記録済み (`main` ブランチが存在しないためオーナー裁定)。
- リポジトリ側の実装物としては「安定した status check context の提供」までが本 change の担当で、それは充足している。要求本体 (取り込みを PR 経由に強制する) の充足は GitHub 設定の完了待ち。

---

## 追加検査

### tasks.md の整合

- 未チェック: 4.6 / 5.1 / 5.2 / 5.3 の 4 件。いずれも GitHub 設定・マージ後の実行確認という運用操作で、実装コードを伴わない。対応表の ⏳ と一致する。
- チェック済み 12 件はすべて対応表の実装箇所または実測事実で裏が取れる。**未実装なのにチェック済みの虚偽は検出されなかった。**
  - 1.1 → `kasane/config.yaml:70-73`
  - 2.1 → `.github/workflows/verify-ios.yml`
  - 2.2 / 2.3 → `.github/workflows/verify-android.yml`
  - 2.4 / 2.5 → `.github/workflows/verify-maui.yml`
  - 3.1 / 3.2 / 3.3 → `.github/workflows/ci.yml` および各 `verify-*.yml` の `permissions:` (`verify-ios.yml:13-14` / `verify-android.yml:10-11` / `verify-maui.yml:11-12`)
  - 4.1〜4.5 → run `33354483531` の実測と負ケースのステップ単体実行

### 逆流検査

`git log 0bc699b..HEAD -- proposal.md specs/` の結果は空。実装期間中に足場アーティファクト (`proposal.md` / `specs/verification-ci/spec.md`) は一切書き換えられていない。`tasks.md` の差分はチェックボックスの `[ ]` → `[x]` のみで、タスク本文の書き換えはない。**逆流なし。**

### 未記録乖離

対応表に ❌ は 0 件。diff に現れる変更で Scenario に対応しないものは `scripts/identity-lint.py` の 2 箇所のみで、deviation の `[付随修正]` に記録済み。**未記録乖離なし。**

### テストの実行確認

- 手元実行 (本検証で実施): `python3 scripts/local-path-lint.py` / `scripts/identity-lint.py` / `scripts/comment-policy-lint.py` はいずれも exit 0 (comment-policy-lint は検査対象 685 ファイル / 禁止 0 件)。
- 3 platform のテスト: run `33354483531` (commit `9642652`) の実測に依拠する — iOS 642 件 / Android 2700 件 / MAUI 516 件、すべて失敗 0。
- **限定事項**: HEAD (`72b731b`) は当該 run より後の commit で、`ci.yml` の concurrency group (`github.ref` → `github.sha`)、および ios / android / maui のコメント文言を変更している。ロジックを変える差分は concurrency の 1 行のみで、job 内のテスト実行経路には触れていないが、HEAD そのものでの CI 実行は未実施。

---

## 残タスク (アーカイブ前提)

判定 VALID は実装範囲に対するもので、次の運用操作が完了するまで Requirement「マージ保護」と Scenario
「develop へのマージ後にも検証される」の実行確認は開いたままになる。

1. tasks 5.1 / 5.2 — `develop` の branch protection に 4 context (`ios / verify` / `android / verify` / `maui / verify` / `lint`) を必須 status check として設定し、`gh api` で再取得して検査する (`main` は deviation により phase-8 へ申し送り)
2. tasks 5.3 — 検査未通過 PR のマージブロックと直 push 拒否の確認
3. tasks 4.6 — マージ後の push トリガーで `develop` 上の実行が走ることの確認 (HEAD の concurrency 変更が実際に効くことの確認も兼ねる)
