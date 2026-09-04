# Verify 001: add-consumer-verification

- 実施日: 2026-09-02
- 対象: `specs/consumer-verification/spec.md` (ADDED 9 Requirement / 18 Scenario)、`specs/verification-ci/spec.md` (ADDED 1 / MODIFIED 3 Requirement、11 Scenario)
- 判定: **INVALID** (Scenario の欠落・乖離は 0 件。未記録の差分 1 件 + 記録粒度の不足 1 件)

> 注: コンテキストパッケージは consumer-verification を「ADDED 8 Requirement」としていたが、spec 本文の Requirement は 9 件ある。本検証は 9 件すべてを対象にした。

## 状態の凡例

| 記号 | 意味 |
|---|---|
| ✅ | 実装が存在し、evidence に実測がある |
| 🔍 | 実装が存在し、静的確認 (生成物・構成の読み取り) のみ。CI 上の実行は未実施 |
| ⏸ | 未実施。spec / proposal / tasks で phase-8 またはオーナー作業へ送ることが明記済み |
| ⚠️ | deviation.md に記録済みの合意差分 |
| ❌ | 欠落・乖離 |

本 change はユニットテストを持たない (消費者検証はスクリプトと CI で担保する構成)。「テスト」列には Scenario を担保する自動検査 (lint の selftest・スクリプトの検査ロジック) と evidence の節番号を書く。

---

## 対応表: consumer-verification

### Requirement: 消費者プロジェクトの構成

| Scenario | 実装 | 検査 / 実測 | 状態 |
|---|---|---|---|
| 本体ソースへの参照を持たない | `verification/ios/Package.swift.template:25-27,33`、`verification/android/settings.gradle.kts:34-50`、`verification/android/app/build.gradle.kts:65`、`verification/maui/VerificationApp.csproj:54` | 本検証で `includeBuild` / `dependencySubstitution` / `ProjectReference` / `.package(path:` をソース横断検索 → コメント中の言及のみで宣言は 0 件。iOS の `path:` は build-consumer.sh が生成するスナップショット参照であり本体ソースではない | ✅ |
| README の最小例がそのままビルド対象になる | `verification/ios/Sources/VerificationApp/SettingsScreen.swift`、`verification/android/app/src/main/kotlin/SettingsScreen.kt`、`verification/maui/SettingsPage.xaml`、`verification/maui/MauiProgram.cs` | `scripts/readme-example-lint.py` (本検証で再実行 exit 0)。3 platform のビルド成功は evidence/consumer-dry-run.txt 節 1-3 / evidence/verification-runs.txt 節 1 | ✅ |

### Requirement: モードと version の指定

| Scenario | 実装 | 検査 / 実測 | 状態 |
|---|---|---|---|
| 引数なしで dry-run が動く | `verification/lib/verification-args.sh:60-118`、既定 version は `verification/android/prepare-feed.sh:22-25` (カタログ由来) / `verification/maui/prepare-feed.sh:19` (`0.0.0-dev`) / iOS は version なし | evidence/verification-runs.txt 節 1 (3 platform とも exit 0)。本検証で Android dry-run を再実行 → exit 0、`jp.kamusoft:kssettingsview:0.1.0-SNAPSHOT` を解決 | ✅ |
| version を与えると全 platform に同じ文字列が流れる | iOS `verification/ios/build-consumer.sh:47-49`、Android `verification/android/app/build.gradle.kts:16-18` + `android/build.gradle.kts:13`、MAUI `verification/maui/VerificationApp.csproj:27,54` | evidence/verification-runs.txt 節 6 (`0.1.0-rc.1` が 3 platform に流れる)。本検証で `./gradlew -Pversion=0.1.0-rc.1 :kssettingsview:properties` → `version: 0.1.0-rc.1` を再現 | ⚠️ 成立に必要だった本体側 2 件の修正は deviation.md に付随修正として記録済み |
| 不正な入力は早期に失敗する | `verification/lib/verification-args.sh:99-114` | 本検証で 3 platform × 2 スクリプト × 3 パターン (mode 不正 / smoke で version 省略 / smoke + `--reference`) を実行 → 18 件すべて exit 1、フィード準備前に終了 | ✅ |

### Requirement: dry-run の参照先

| Scenario | 実装 | 検査 / 実測 | 状態 |
|---|---|---|---|
| 本リポジトリ由来の座標はローカル参照先からのみ取得される | iOS `verification/ios/build-consumer.sh:38-47`、Android `verification/android/settings.gradle.kts:37-47` (`exclusiveContent`)、MAUI `verification/maui/nuget.dry-run.config:18-25` + `verification/maui/build-consumer.sh:54-56` (空の `RestorePackagesPath`) | 取得元検査は `verification/maui/check-dependencies.py:52-60` (`.nupkg.metadata` の source)。evidence/premise-spike.txt 節 2-3、evidence/verification-runs.txt 節 1 | ✅ |
| ローカル参照先に無ければ公開済みの版でも失敗する | 同上 | evidence/verification-runs.txt 節 2 (iOS/Android/MAUI とも exit 1。Android は Central へ問い合わせなし、MAUI は NU1101 でユーザーキャッシュにフォールバックせず)。evidence/premise-spike.txt 節 2 の対照実験 (b) が空の展開先の必要性を裏付け | ✅ |
| 配信先へ副作用を残さない | `.github/workflows/verify-consumer-*.yml:34-35` (`permissions: contents: read`、`secrets:` なし)、`scripts/spm-snapshot/sync-snapshot.sh` は git 書き込みを行わない | evidence/verification-runs.txt 節 1 の副作用比較 (SPM tag 0→0、nuget.org totalHits 0→0、Maven Central numFound 0→0)。Central Portal の deployments は認証が要るため、権限・secrets の不在を根拠とする旨が evidence に明記されている。本検証で `sync-snapshot.sh` に `git push` / `git tag` / `git commit` が無いことを確認 | ✅ |

### Requirement: smoke の参照先

| Scenario | 実装 | 検査 / 実測 | 状態 |
|---|---|---|---|
| 参照先が公開レジストリを指す | iOS `verification/ios/build-consumer.sh:29,49`、Android `verification/android/settings.gradle.kts:40`、MAUI `verification/maui/nuget.smoke.config` | evidence/verification-runs.txt 節 7 (iOS は URL + exact の 1 行のみ、Android は Central だけを検索、MAUI は nuget.org のみ) | ✅ |
| 公開レジストリからの解決 | 同上 | 配布物が未公開のため実証不可。spec 本文・proposal Non-Goals・tasks 5.4 が phase-8 の初回リリースで行うと明記 | ⏸ |

### Requirement: フィード準備と消費者ビルドの分離

| Scenario | 実装 | 検査 / 実測 | 状態 |
|---|---|---|---|
| 外部で準備した配布物を消費者に渡す | `verification/<platform>/prepare-feed.sh` と `verification/<platform>/build-consumer.sh` の 2 段構成。`--reference` 指定時に準備を飛ばす分岐は `verification/ios/build-consumer.sh:37-40`、`verification/android/build-consumer.sh:35-39`、`verification/maui/build-consumer.sh:35-39` | evidence/consumer-dry-run.txt 節 3 (MAUI: `--reference` 指定時に pack 行が出ず restore から始まる)、evidence/verification-runs.txt 節 6 (Android: `--reference` で exit 0)。iOS の `--reference` は節 2 の負ケースで経路のみ確認 | ✅ |

### Requirement: 消費者ビルドの成立条件

| Scenario | 実装 | 検査 / 実測 | 状態 |
|---|---|---|---|
| 3 platform の Release ビルド | iOS `verification/ios/build-consumer.sh:77-83` (`generic/platform=iOS Simulator`、Release)、Android `verification/android/build-consumer.sh:52` (`:app:assembleRelease`、`verification/android/app/build.gradle.kts:48-56` で signingConfig 未割り当て)、MAUI `verification/maui/build-consumer.sh:94-99` (`net10.0-android` / `net10.0-ios` + `iossimulator-*` RID) | evidence/verification-runs.txt 節 1 (3 platform とも成功。MAUI iOS は `Code Signing Key: ""` で署名情報なし)。本検証で Android を再実行 → BUILD SUCCESSFUL | ✅ |

### Requirement: MAUI 消費者の依存検査

| Scenario | 実装 | 検査 / 実測 | 状態 |
|---|---|---|---|
| 依存警告で失敗する | `verification/maui/VerificationApp.csproj:43-45` (`WarningsAsErrors` に NU1605/NU1608/NU1107) | evidence/consumer-dry-run.txt 節 3 (正ケースで 3 コードとも 0 件)。エラー化そのものを踏ませる負ケースの実測は evidence にない (節 3 の負ケース (b) は NU1603 経路で、版一致検査が拾っている) | 🔍 構成で確認 |
| binding の version 不一致を検出する | `verification/maui/check-dependencies.py:75-99` | evidence/verification-runs.txt 節 3 (facade 0.0.0-dev / binding 0.0.1-dev のフィードで exit 1。両者の解決版が出力に出る)。「NU1605/1608/1107 のいずれにも当たらないため検査が無ければ素通りする」ことも記録済み | ✅ |
| ビルド警告は失敗にしない | `WarningsAsErrors` に XA4301 等を含めない (同上) | evidence/verification-runs.txt 節 1 / consumer-dry-run.txt 節 3 (XA4301 × 4 件が出ても「ビルドに成功しました」で終了) | ✅ |

### Requirement: 解決結果の証跡

| Scenario | 実装 | 検査 / 実測 | 状態 |
|---|---|---|---|
| 解決版と取得元が読める | 共通出力 `verification/lib/verification-args.sh:29-47` (`ksv_evidence`、`GITHUB_STEP_SUMMARY` へも書く)。iOS `verification/ios/build-consumer.sh:73-92` (生成 Package.swift / 依存グラフ / Package.resolved)、Android `verification/android/build-consumer.sh:56-68` (`jp.kamusoft` 行)、MAUI `verification/maui/build-consumer.sh:87-92` | evidence/verification-runs.txt 節 1 に 3 platform の出力。iOS dry-run で `Package.resolved` が生成されないことと、その旨を明示出力する分岐 (`build-consumer.sh:85-92`) は evidence/premise-spike.txt 節 1 で裏付け済み。job summary への出力は CI 未実行のため未確認 (tasks 5.6) | ✅ (job summary のみ 🔍) |

### Requirement: README 最小例との一致

| Scenario | 実装 | 検査 / 実測 | 状態 |
|---|---|---|---|
| 例の変更が消費者に追随していなければ失敗する | `scripts/readme-example-lint.py:126-163` | 本検証で `--selftest` 実行 → 11 項目すべて OK (片側変更で exit 1、不一致ファイル名の出力、重複検出、対応先不在の検出)。実 README での片側変更は evidence/verification-runs.txt 節 4 | ✅ |
| 一致していれば通る | 同上 | 本検証で `python3 scripts/readme-example-lint.py` → exit 0「README の最小例 4 件が消費者検証のソースと一致する」 | ✅ |

---

## 対応表: verification-ci

### Requirement (ADDED): 消費者検証 workflow の再利用契約

| Scenario | 実装 | 検査 / 実測 | 状態 |
|---|---|---|---|
| モードと version を与えた呼び出し | `.github/workflows/verify-consumer-{ios,android,maui}.yml:16-32` (`workflow_call`、`mode` 必須 / `version` / `artifact`)、job 名 `verify` (各 `:42-44` 前後)、呼び出し側 `.github/workflows/ci.yml:45-65` | 本検証で 4 workflow を `yaml.safe_load` → `on: workflow_call`、job 名は 3 本とも `verify`、ci.yml の job 名は `ios/android/maui/consumer-ios/consumer-android/consumer-maui/lint` の 7 件、`with: {mode: dry-run}` が inputs に収まることを確認。CI 上の実行は未実施 (tasks 5.6) | 🔍 |
| artifact を与えた呼び出し | 各 workflow の `Download prepared distribution` (`if: inputs.artifact != ''`) と `--reference` への引き渡し (ios `:101-121`、android `:104-124`、maui `:126-146`)。iOS は identity 保持のため展開先末尾を `KsSettingsView-SPM` に固定 | スクリプト側の「準備済み参照先を渡すと準備段を再実行しない」挙動は実測済み (上記「外部で準備した配布物を消費者に渡す」)。CI 上の upload→download 経路は tasks 5.7 が未実施 | ⏸ (CI 経路のみ) |
| 不正な入力で失敗する | 各 workflow の `Validate inputs` ステップ (ios `:53-77`、android `:49-73`、maui `:52-76`)。checkout・download-artifact より前に配置 | ステップ順序と条件を本検証で読み取り確認 (mode 許可値 → smoke の version 必須 → smoke + artifact の禁止)。同じ規則のスクリプト側は 18 件すべて実測で exit 1。CI 上の実行は未実施 | 🔍 |

### Requirement (MODIFIED): CI の起動条件

変更後の全文どおり、`ci.yml` は 7 job 構成になり paths 絞り込みを持たない。旧 4 job 前提の記述は `ci.yml:29-32` のコメントを含め更新済みで、残骸はない。

| Scenario | 実装 | 検査 / 実測 | 状態 |
|---|---|---|---|
| PR で全 job が起動する | `.github/workflows/ci.yml:9-16` (pull_request: develop / main、paths なし)、`:29-65,67-` の 7 job | 構成は本検証で確認。draft PR での起動確認は tasks 5.6 が未実施 | ⏸ |
| main への PR でも起動する | `ci.yml:11-13` に `main` が含まれる | `main` ブランチが未作成のため実証不可 (proposal Non-Goals / tasks 6.1 に明記) | ⏸ |
| develop へのマージ後にも検証される | `ci.yml:14-16` (push: develop) | 構成は確認。実測は未実施 | 🔍 |
| 消費者検証は dry-run で動く | `ci.yml:45-65` (`mode: dry-run`、version / artifact を渡さない)、各消費者 workflow の `permissions: contents: read` と `secrets:` 不在 | 本検証で 3 本とも `permissions.contents == read` のみ・`secrets` キー無しを確認。ローカル参照先からの解決は上記 dry-run の Scenario 群で実測済み | ✅ (構成) / 🔍 (CI 実行) |

### Requirement (MODIFIED): lint の検証

| Scenario | 実装 | 検査 / 実測 | 状態 |
|---|---|---|---|
| 違反の検出 | `.github/workflows/ci.yml:127-134` に `README example lint` を追加 (gitleaks / local-path-lint / identity-lint / comment-policy-lint は既存ステップ) | 本検証で 4 lint をローカル実行 → すべて exit 0。README lint の違反検出は `--selftest` と evidence/verification-runs.txt 節 4 で確認 | ✅ |
| samples 配下の識別子検出 | `kasane/config.yaml:75-77` の `identity.scope` に `verification` を追加 | 本検証で `identity-lint.py` 実行 → exit 0。scope 拡張は config で確認。なお `verification/` は現時点で未追跡のため、identity-lint / comment-policy-lint が実際に走査するのはコミット後 (CI では走査される) | ✅ |

### Requirement (MODIFIED): マージ保護

| Scenario | 実装 | 検査 / 実測 | 状態 |
|---|---|---|---|
| 検査未通過のマージ拒否 | GitHub の branch protection 設定 (リポジトリ外) | tasks 6.1 が未着手。オーナーが行う設定操作で、`main` は未作成のため phase-8 送り | ⏸ |
| 直 push の拒否 | 同上 | 同上 | ⏸ |

---

## 追加検査

### tasks.md の突き合わせ (虚偽チェック)

チェック済みの 1.1〜5.5 は、いずれも実装と evidence の該当節を確認できた。虚偽のチェックは検出されなかった。

- 未チェックのまま残っているのは 5.6 / 5.7 / 6.1 の 3 件で、いずれも CI 実行またはオーナーの GitHub 設定操作を要する。コンテキストパッケージの前提と一致する
- 5.7 は手元側 (`--reference` で準備段を飛ばす) が evidence/consumer-dry-run.txt 節 3 で実施済みだが、CI の `artifact` 入力が未確認のため未チェックのまま。粒度として正しい

### 逆流検査

`git log` / `git diff HEAD` で確認。足場アーティファクト (`proposal.md` / `design.md` / `specs/`) は commit `72df5b3` 以降 1 度も変更されておらず、作業ツリーにも差分がない。逆流なし。

### 未記録の差分

| # | 箇所 | 内容 | 見立て |
|---|---|---|---|
| ❗1 | `kasane/config.yaml:78` | `lint.identity.allow` に `"repo.local"` を追加。tasks 4.3 が指示しているのは `scope` への `verification` 追加だけで、`allow` の拡張は tasks にも deviation.md にも記録がない | **deviation として記録すべき**。本検証で `repo.local` を外して `identity-lint.py` を実行したところ、`kasane/changes/add-consumer-verification/second-opinion-code-001.md:52` の `maven.repo.local` を mDNS ホスト名と誤検出して exit 1 になる。追加は正当かつ必要で、実装の修正は不要。deviation.md に `[付随修正]` 1 行を足せば解消する |
| ❗2 | `android/kssettingsview/build.gradle.kts:172-177` | SNAPSHOT ガードの `GradleException` メッセージを「カタログの kssettingsview キーへ設定」から「`-Pversion=` で注入する」へ改訂。deviation.md の 2 件目は同ファイルの署名条件化と `signAllPublications()` 付近のコメント訂正までを記録しており、このメッセージ改訂は文言に含まれない | **記録粒度の不足**。1 件目の付随修正 (version 注入の受け口) を入れた結果、旧メッセージが実挙動と食い違うようになったための追随であり、独立した判断ではない。既存の deviation 記述にこのメッセージ改訂を含める一文を足せば足りる |

いずれも実装の欠陥ではなく、deviation.md の追記のみで解消する。

### テストの実行

本 change はユニットテストを持たない。代わりに、本検証で以下を実行した (すべて成功)。

- `python3 scripts/readme-example-lint.py` → exit 0
- `python3 scripts/readme-example-lint.py --selftest` → 11 項目 OK / exit 0
- `python3 scripts/local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` → いずれも exit 0
- 消費者検証スクリプトの負ケース 18 通り (3 platform × prepare-feed/build-consumer × mode 不正 / smoke で version 省略 / smoke + `--reference`) → すべて exit 1、フィード準備前に終了
- `verification/android/build-consumer.sh` (引数なし dry-run、`ANDROID_HOME` / `ANDROID_SDK_ROOT` を落とした状態) → exit 0。`android/local.properties` からの SDK 解決が働き、`+--- jp.kamusoft:kssettingsview:0.1.0-SNAPSHOT` を解決して Release ビルド成功
- `android/gradlew -Pversion=0.1.0-rc.1 :kssettingsview:properties` → `version: 0.1.0-rc.1` (注入の受け口が効いている)
- 4 workflow の YAML ロードと job 名 / inputs / permissions / secrets の読み取り

本体の Android ユニットテスト (2700 件) は evidence/verification-runs.txt 節 12 で全件成功が記録されている (本検証では再実行していない)。

publish / upload / GitHub への書き込みは実行していない。

### UI 変更

なし (`ui/` アーティファクトを持たない change)。

---

## 判定

**INVALID** — ただし spec の Requirement / Scenario に対する欠落・乖離は 0 件。

- ❌ (Scenario の欠落・乖離): 0 件
- 未記録の差分: 2 件 (❗1 / ❗2)。いずれも `deviation.md` への追記で解消し、コードの修正を要さない
- 虚偽チェック: なし
- 逆流: なし
- 実行した検査: すべて成功

⏸ の 7 Scenario (smoke の公開レジストリ解決、CI 起動の実証 3 件、artifact の CI 経路、マージ保護 2 件) は spec / proposal / tasks で phase-8 またはオーナー作業へ送ることが明記済みであり、INVALID の根拠にはしていない。

## 気づき (判定外・参考)

- `verification/maui/check-dependencies.py` は `scripts/` 配下の lint 群と違い `--selftest` を持たない。spec の要求ではないが、版一致検査は本 change で唯一「NU1605/1608/1107 が拾わない不一致」を捕まえる部品であり、回帰の検出手段が evidence の手動実行だけになっている
- `verification/` 配下がまだ未追跡のため、`comment-policy-lint` / `identity-lint` のローカル実行はこのディレクトリを走査していない (evidence/consumer-dry-run.txt 節 6 も `--paths` で明示検査した旨を記録している)。コミット後は自動で走査対象に入る
