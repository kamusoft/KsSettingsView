# dry-run 検証の証跡 (tasks 8.3b / 8.3c / 8.4 / 8.5 / 8.6)

## 実行

| 項目 | 値 |
|---|---|
| 実行 | https://github.com/kamusoft/KsSettingsView/actions/runs/34604848409 |
| 日付 | 2026-09-11 |
| 対象 SHA | `6cd911e63ebc6140ec78fe964b21146b344df3d8` |
| ブランチ | `develop` |
| version | `0.1.0-beta.3` (monorepo・SwiftPM 配信リポジトリとも未使用) |
| dry-run | true |

先行 change ([backport-release-workflow-hardening](../../backport-release-workflow-hardening/evidence/dry-run-verification.md)) のリハーサルでは、validate の `Verify install examples` が README 2 枚と Skill 8 枚の version 一致を要求するため検証専用ブランチが必要だった。本 change でその検査 step を撤去したので、`develop` の HEAD から直接起動できる。検証のためだけの差分は含まれていない。

## 結果 (tasks 8.4)

| job | 結果 |
|---|---|
| validate | success |
| ios / verify | success |
| android / verify | success |
| maui / verify | success |
| package-ios | success |
| package-android | success |
| package-maui | success |
| consumer-ios / verify | success |
| consumer-android / verify | success |
| consumer-maui / verify | success |
| publish | skipped |
| wait-for-registries | skipped |
| smoke-ios / smoke-android / smoke-maui | skipped |

validate から消費者検証 3 経路までが通り、publish 以降はリハーサルとして実行されていない。

## 開発ブランチからのリハーサルでは対象を集めない (tasks 8.3b)

validate job の step:

| step | 結果 |
|---|---|
| Decide release notes scope | success |
| Build release notes | skipped |
| Upload release notes | skipped |
| Skip release notes | success |

`develop` (= `main` 以外) からの起動では収集の判定が `false` に倒れ、`## Changes` の記載が無いことを理由に失敗していない。3 つの step が同じ判定結果を見ている (条件式の一本化) ことも、skip と success の組み合わせから確認できる。

## リリースしてもインストール例は変わらない (tasks 8.5)

`release-maui-nupkg` artifact の `KsSettingsView.Maui.0.1.0-beta.3.nupkg` を展開し、同梱の `README.md` を検査した。

- SwiftPM: `.package(url: "https://github.com/kamusoft/KsSettingsView-SPM", exact: "{version}")`
- Maven: `implementation("jp.kamusoft:kssettingsview:{version}")`
- NuGet: `<PackageReference Include="KsSettingsView.Maui" Version="{version}" />`

3 経路ともプレースホルダのまま。具体 version (`0.1.0`) の混入は 0 件。リポジトリの `README.md` と `diff` が一致し、リリース処理が同梱 README を書き換えないことを確認した。

## 埋めずに使うと依存解決が失敗する (tasks 8.6)

同梱 README のインストール例をそのまま写した消費者プロジェクトを一時領域に作り、3 経路で解決を試みた。

| 経路 | 実行 | 結果 |
|---|---|---|
| SwiftPM | `swift package resolve` | `error: invalid manifest (evaluation failed)` / `Invalid semantic version string '{version}'` |
| NuGet | `dotnet restore` | `error : '{version}' は有効なバージョン文字列ではありません。` |
| Maven | `gradlew dependencies --configuration compileClasspath` | `jp.kamusoft:kssettingsview:{version} FAILED` |

3 経路とも解決に失敗する。プレースホルダを埋めずに使った利用者は、ビルドの最初の段で気づく。

## main からのリハーサルでは実際の経路を通る (tasks 8.3c)

`main` に本変更が入るまで実施できないため、リリース pull request のマージ後に別途起動した。

| 項目 | 値 |
|---|---|
| 実行 | https://github.com/kamusoft/KsSettingsView/actions/runs/34611883314 |
| 日付 | 2026-09-11 |
| 対象 SHA | `9c97e13` (pull request #12 のマージ commit) |
| ブランチ | `main` |
| version | `0.1.0-beta.3` (monorepo・SwiftPM 配信リポジトリとも未使用) |
| dry-run | true |

### validate の step

8.3b (`develop` から) と判定が反転し、`main` では収集・検査・整形・受け渡しが実際に走る。

| step | 8.3b (`develop` から) | 8.3c (`main` から) |
|---|---|---|
| Decide release notes scope | success | success |
| Build release notes | skipped | **success** |
| Upload release notes | skipped | **success** |
| Skip release notes | success | **skipped** |

判定のログ: `Release ノートの収集: true (dry-run: true / 起動 ref: refs/heads/main)`

### 対象の決定

`起点: 0.1.0-beta.2 / 対象 commit: 9c97e13fde5ee7c9e88fc9c08e64c66d74e4cda0 / 対象 pull request: [12]`

- 起点は `main` の first-parent 上で対象 commit の祖先となる、draft でない公開済み Release のうちもっとも近いもの (`0.1.0-beta.2`) に解決された
- 対象は base が `main` の pull request に絞られ、#12 の 1 件になった

### 成果物への受け渡し

`release-notes` artifact (2 ファイル・620 bytes、Artifact ID 10268737480) に `notes.md` と `meta.json` が保存された。

```json
{
  "version": "0.1.0-beta.3",
  "base-tag": "0.1.0-beta.2",
  "commit": "9c97e13fde5ee7c9e88fc9c08e64c66d74e4cda0",
  "pulls": [12]
}
```

```markdown
## What's Changed

### Bug Fixes
- Exception messages, diagnostic logs, and deprecation warnings are now written in English (#12)

### Documentation
- Installation examples in the READMEs and Agent Skills no longer pin a version; the latest release page shows the version to use (#12)

**Full Changelog**: https://github.com/kamusoft/KsSettingsView/compare/0.1.0-beta.2...0.1.0-beta.3
```

成果物の `notes.md` は、同じ pull request 本文を `render` に与えた手元の出力と `diff` で一致した。

### Release を作らないこと

| job | 結果 |
|---|---|
| validate / package-ios / package-android / package-maui | success |
| ios / android / maui (各 verify) | success |
| consumer-ios / consumer-android / consumer-maui (各 verify) | success |
| publish / wait-for-registries / smoke-ios / smoke-android / smoke-maui | skipped |

run 全体は success。実行の前後で配信先は変わっていない。

- GitHub Release は `0.1.0-beta.2` が最新のまま (`0.1.0-beta.3` は作られていない)
- monorepo の tag は `0.1.0-beta.1` / `0.1.0-beta.2` の 2 本のまま
- SwiftPM 配信リポジトリの tag も同じ 2 本のまま
