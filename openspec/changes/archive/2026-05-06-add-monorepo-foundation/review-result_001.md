# レビュー結果 - add-monorepo-foundation

**レビュー日時**: 2026年05月06日
**レビュワー**: sdd-reviewer
**変更提案ID**: add-monorepo-foundation

## サマリー

KsSettingsView モノレポの土台（ディレクトリ構成・各プラットフォームのビルド入口・ドキュメント・命名規約）が proposal.md / design.md / spec.md / tasks.md に沿って実装されている。

ビルド入口コマンドは 3 プラットフォームすべて成功:
- `swift package describe` (ios/) → 成功 (Tools 5.10, iOS 16.0)
- `./gradlew tasks` (android/) → BUILD SUCCESSFUL (Gradle 8.10.2, root 'ks-settingsview')
- `dotnet sln KsSettingsView.slnx list` (maui/) → 成功 ("No projects found")

ドキュメント類（README.md、LICENSE、docs/development.md、docs/conventions.md）は spec の要求するセクション・ツールチェイン情報・命名規約をすべて含んでいる。

一方、`samples/{ios,android,maui}/` は空ディレクトリのまま placeholder ファイル（`.gitkeep` 等）が無く、Git でクローンした際にディレクトリが消失する。これは spec.md の「Sample のサブディレクトリ構成」Scenario（`THEN samples/ios/、samples/android/、samples/maui/ の 3 つのサブディレクトリが存在する` をクローン直後に満たす必要がある）に違反する可能性が高く、修正が必要。

**判定**: `CHANGES_REQUESTED`

## 指摘事項

### 🟠 Major

#### [Major] samples/ 配下の空サブディレクトリが Git で追跡されない

**該当箇所**:
- `samples/ios/`
- `samples/android/`
- `samples/maui/`

**問題点**:
spec.md の Requirement「モノレポのディレクトリ構成」配下 Scenario「Sample のサブディレクトリ構成」では、`GIVEN samples/ ディレクトリ` / `WHEN その配下を確認する` / `THEN samples/ios/、samples/android/、samples/maui/ の 3 つのサブディレクトリが存在する` と定義されている。同 Requirement 配下のもう一つの Scenario「トップレベルディレクトリの存在」も `GIVEN リポジトリのクローン直後` を前提としている。

しかし、現状 3 サブディレクトリは完全に空であり、`.gitkeep` 等の placeholder ファイルが存在しない。Git は空ディレクトリを追跡しないため、このリポジトリを `git init` してコミットし他者がクローンした際、`samples/ios/`・`samples/android/`・`samples/maui/` は存在しなくなる。結果として「クローン直後」に Scenario が失敗する。

また tasks.md 1.2 「`samples/` 配下に `ios/`、`android/`、`maui/` の 3 サブディレクトリを作成」も完了扱いになっているが、Git 追跡されない以上「作成された」とは言えない。

**推奨修正**:
各サブディレクトリに最小限の placeholder を配置する。例えば以下のいずれか。

1. `.gitkeep` ファイル（最小、慣例的）:
   - `samples/ios/.gitkeep`
   - `samples/android/.gitkeep`
   - `samples/maui/.gitkeep`

2. `README.md`（後続 Sample 追加までのメモを兼ねられる）:
   - `samples/ios/README.md` などに「後続変更提案で追加予定」と一行記述

design.md / proposal.md の意図（後続 capability ですぐ各 Sample を配置できる土台）を踏まえると、`README.md` 形式の方が将来意図を残せて望ましい。

### 🟡 Minor

#### [Minor] `.openspec.yaml` の `pluginManagement` に `gradlePluginPortal()` が追加されているがタスクには記載なし

**該当箇所**: `android/settings.gradle.kts:6-12`

**問題点**:
tasks.md 3.1 では `pluginManagement` の repositories を `google()` / `mavenCentral()` 指定 と書かれているが、実装では `gradlePluginPortal()` も追加されている。仕様の意図に反するわけではなく後続 AGP/Kotlin プラグイン解決に必要となる可能性が高い妥当な拡張だが、tasks.md の記述と差分があるため小さな乖離として記録する。

**推奨修正**:
- このまま維持で問題なし。ただし、後続変更提案で `pluginManagement` の整備を行う際に再検討する旨をコミットメッセージや PR 説明に残しておくと良い。
- 厳密に tasks.md に合わせたい場合は `gradlePluginPortal()` を削除するが、AGP プラグイン解決時に困るため非推奨。

### 🔵 Suggestion

#### [Suggestion] `maui/KsSettingsView.slnx` の最小化と将来の Visual Studio 互換性

**該当箇所**: `maui/KsSettingsView.slnx`

**問題点**:
現状内容は `<Solution>\n</Solution>` の 2 行のみで `dotnet sln list` は通るが、Visual Studio 2022 / Rider が `slnx` を開く際に XML 宣言や namespace を期待する場合がある。.NET 9 SDK の `dotnet sln` は許容するが、IDE 依存の最小要件は揺れる可能性がある。

**推奨修正**:
- 当面は変更不要。ただし将来 `Visual Studio で開けない` 等の報告があれば、`<Solution xmlns="http://schemas.microsoft.com/dotnet/solution/v1">` などの拡張を検討する。
- 仕様 (`MAUI のソリューション入口` Scenario) は `存在` のみを要求しており、現状の実装で要件は満たしている。

#### [Suggestion] `.gitignore` に `.gradle/` が含まれているが、`android/.gradle/` が既に作業ツリーに残っている

**該当箇所**: `android/.gradle/`（作業ディレクトリ）

**問題点**:
`./gradlew tasks` の検証を実行した結果、`android/.gradle/` キャッシュが生成されている。`.gitignore` で除外されているためコミット汚染の懸念は無いが、レビュー時のディスク容量や `find` 系コマンド実行時のノイズになり得る。

**推奨修正**:
- 検証完了後、`trash android/.gradle` で削除すると後続作業者がクリーンな状態で確認できる。
- 必須ではない。

## アクションプラン

優先度順:

1. (Major) `samples/{ios,android,maui}/` に `.gitkeep` または `README.md` を配置し、Git 追跡を保証する。
2. (Minor) `pluginManagement` に追加した `gradlePluginPortal()` の意図をコミットメッセージで補足する（または該当箇所にコメント）。
3. (Suggestion) `android/.gradle/` キャッシュを削除（任意）。
4. (Suggestion) `slnx` の IDE 互換性は当面様子見、問題があれば XML 宣言追加。

## 判定結果

**ステータス**: `CHANGES_REQUESTED`

**理由**:
- Major 指摘 1 件 (`samples/` の空ディレクトリ未追跡) が spec.md の Scenario「Sample のサブディレクトリ構成」を「クローン直後」前提では満たさなくなるリスクがあるため、修正を要求する。
- 残りは Minor / Suggestion で必須対応ではない。
- ビルド入口の動作・ドキュメント整備・命名規約・最低ツールチェイン明記は全て要求を満たしており、Major 1 件の修正後は速やかに APPROVED に移行可能。
