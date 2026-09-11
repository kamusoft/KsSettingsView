# dry-run 検証の証跡 (tasks 8.3b / 8.4 / 8.5 / 8.6)

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

## 未了

- tasks 8.3c (`main` からのリハーサルで収集・検査・受け渡しが実際に走ること) は、`main` に本変更が入っていないと旧 workflow が走るため、リリース pull request のマージ後に実施する
