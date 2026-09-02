# セカンドオピニオン: add-maui-nuget-distribution (spec-001)
**相方**: codex / **label**: so-spec-add-maui-nuget-distribution / **日付**: 2026-09-02 / **対象**: kasane/changes/add-maui-nuget-distribution/ の proposal.md / design.md / specs/maui-nuget-distribution/spec.md / tasks.md (提案一式)
---
# 独立提案レビュー: add-maui-nuget-distribution

静的レビューのみ実施し、ビルド・テスト・ファイル変更は行っていません。

指摘件数: Critical 0 / Major 4 / Minor 3

## 指摘事項

### [🟠 Major] 同梱 README が改名後に動作しない利用例を公開する

**該当箇所**: `proposal.md:26`、`tasks.md:21`、`specs/maui-nuget-distribution/spec.md:91`、`README.md:126`、`README_ja.md:126`

**問題点**: facade の namespace を `KsSettingsView` へ変更する一方、package README として同梱する現在の README は `clr-namespace:KsSettingsView.Maui;assembly=KsSettingsView.Maui` を案内しています。proposal はこの文面の追随を change 完了後の docs-refresh へ送っていますが、tasks 3.1 は追随前の README をパッケージへ同梱します。

その結果、本変更が生成する nupkg 自体が、コピーするとコンパイルできない最小例を配布します。README Scenario は画像 URL しか検査せず、公開 namespace の Requirement と矛盾した内容を検出できません。

**推奨修正**: 実装前にライフサイクルを決定してください。

- docs-refresh の成果を pack 完了条件へ組み込み、改名後の README を pack する
- または本変更では package README の同梱を行わず、docs-refresh 完了後の後続変更へ移す

いずれの場合も、同梱 README の XAML 例をそのまま使用した消費者ビルド Scenario を追加してください。

---

### [🟠 Major] `Directory.Build.props` では TFM 依存の最低 OS 値を安全に導出できるとは限らない

**該当箇所**: `design.md:73`、`design.md:75`、`tasks.md:28`、`maui/macios/KsSettingsView.Binding.iOS/KsSettingsView.Binding.iOS.csproj:11`、`maui/tests/KsSettingsView.IntegrationHost.Android/KsSettingsView.IntegrationHost.Android.csproj:9`

**問題点**: design/tasks は、`maui/Directory.Build.props` から共通 props を import し、各プロジェクトの `SupportedOSPlatformVersion` を platform TFM に応じて導出するとしています。しかし `Directory.Build.props` はプロジェクト本体より早く import され、後で定義されるプロパティはそこで参照できません。特に単一 TFM の binding / IntegrationHost は `TargetFramework` を csproj 本体で初めて定義するため、自然な TFM 条件実装では条件が空振りします。[MSBuild の公式 import-order 説明](https://learn.microsoft.com/en-us/visualstudio/msbuild/customize-by-directory?view=visualstudio)

また、版比較方法も未決定です。MSBuild は通常の比較演算子より `VersionLessThan` 等を推奨しています。[MSBuild property functions](https://learn.microsoft.com/en-us/visualstudio/msbuild/property-functions?view=visualstudio)

**推奨修正**:

- 同梱 `.props` は要件定数だけを宣言する
- `SupportedOSPlatformVersion` の代入は、各 csproj の現在の TFM 条件を残して値だけ共通定数参照にするか、TFM が確定した後に評価される配置をdesignで明示する
- Android/iOS、単一/複数 TFM ごとの評価値を `-getProperty` または preprocess 出力で確認する Scenario を追加する
- 比較は `VersionLessThan` を使うことまで design で確定する

---

### [🟠 Major] platform 外ではガードが動かないことを検証できない

**該当箇所**: `specs/maui-nuget-distribution/spec.md:71`、`specs/maui-nuget-distribution/spec.md:73`、`tasks.md:27`

**問題点**: Requirement は「platform TFM のビルドでのみ」ガードすると規定していますが、Scenario は Android/iOS の成功・失敗だけです。facade は `net10.0` も公開しており、複数 TFM の outer build も存在します。

`buildTransitive/` 直下の資産は推移的な全消費者へ import されるため、Condition の誤りによって `net10.0`、outer build、間接参照するライブラリを壊しても現在の受け入れテストでは検出できません。[NuGet の buildTransitive 仕様](https://learn.microsoft.com/en-us/nuget/concepts/msbuild-props-and-targets)

**推奨修正**: 次を独立 Scenario として追加してください。

- `net10.0` 消費者ではガードが診断を出さない
- 複数 TFM プロジェクトの outer build ではガードが動かない
- platform の inner build でのみ、対象 OS の値を比較する
- facade を間接参照する project でも意図した範囲だけに適用される

---

### [🟠 Major] README 画像の「表示できる」が prefix 検査に縮退している

**該当箇所**: `design.md:122`、`design.md:124`、`specs/maui-nuget-distribution/spec.md:93`、`specs/maui-nuget-distribution/spec.md:99`、`tasks.md:33`

**問題点**: Requirement は GitHub と nuget.org の双方で表示できることを求めていますが、Scenario は URL が所定 prefix で始まることしか確認しません。存在しない branch、誤ったファイル名、HTML 応答でも通ります。

さらに、使用する branch 名が Open Question のままであり、public リポジトリが未作成なら現在の時点では到達性を判定できません。`raw.githubusercontent.com` 自体は nuget.org の許可ドメインですが、実 URL の有効性は別途検証が必要です。[NuGet package README 仕様](https://learn.microsoft.com/en-us/nuget/nuget-org/package-readme-on-nuget-org)

**推奨修正**: branch/ref の方針を提案承認前に確定するか、public 化後へ要件を延期してください。受け入れ基準には各画像 URL の取得成功・画像 Content-Type と、NuGet Upload Preview での描画確認を含めてください。

---

### [🟡 Minor] `PackageReadmeFile` の配置が ADR-0025 と不一致

**該当箇所**: `design.md:44`、`tasks.md:21`、`kasane/decisions/maui/0025-nuget-three-package-root-namespace.md:34`

**問題点**: ADR-0025 は `PackageReadmeFile` を `maui/Directory.Build.props` に集約すると決定していますが、design は facade csproj に置くとしています。機能上は facade 固有設定が自然ですが、ADR を実装すると称する提案として差分が説明されていません。

**推奨修正**: props に条件付きで置くか、facade 固有設定として csproj に置く判断を明示し、蒸留時に ADR-0025 の該当記述を日付付きで訂正するタスクを追加してください。

---

### [🟡 Minor] binding 2 パッケージの共通メタデータ検証がデルタスペック上不足している

**該当箇所**: `specs/maui-nuget-distribution/spec.md:23`、`specs/maui-nuget-distribution/spec.md:25`、`specs/maui-nuget-distribution/spec.md:53`

**問題点**: Requirement は `maui/` 配下の全プロジェクトが共通メタデータを継承するとしていますが、メタデータ Scenario は facade の nuspec しか検査しません。binding Scenario は依存・内容・Description だけで、license、repository、icon、version、snupkg の欠落を検出できません。

**推奨修正**: 3パッケージすべてについて version、authors、license、repository、icon、snupkg を検査し、readme は facade のみに存在することを明文化してください。

---

### [🟡 Minor] 一時消費者検証の SDK とパッケージ取得元が固定されていない

**該当箇所**: `design.md:10`、`specs/maui-nuget-distribution/spec.md:63`、`tasks.md:35`

**問題点**: design の実測は SDK 10.0.300、リポジトリの `global.json` も 10.0.300 を指定していますが、リポジトリ外へ一時プロジェクトを作るとこの pin が適用されません。API 版付き TFM と workload 挙動が SDK に依存するため、別 SDK でも同じ検証として扱われる余地があります。また、ローカルフィードで生成した同一パッケージを本当に取得したことの判定方法もありません。

**推奨修正**: SDK/workload 10.0.300.3 の適用方法、使用した Package ID/version/source、隔離した restore packages path を evidence に記録するタスクを追加してください。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md`（always）
- `kasane/handbook/cross/public-identifiers.md`
- `kasane/handbook/cross/test-execution.md`
- `kasane/handbook/maui/integration-host-verification.md`
- 指定された maui/cross ADR と `binding-build-integration.md`
- 現行の MAUI csproj、namespace 宣言、README 利用例

## 総合判定

**NEEDS_DISCUSSION**

README と docs-refresh の完了順序、最低 OS 値を設定する MSBuild 評価位置、public 化前に検証不能な画像 URL の扱いは、実装者の判断だけでは解消できない仕様・設計上の未決事項です。これらを proposal/design/spec/tasks で確定してから実装へ進む必要があります。


## 突き合わせ結果 (2026-09-02)

ホスト側の自己レビュー (2 周、指摘は agenda 決定「slnx は Sample を含めたまま」の反映漏れ 1 件のみ) との突き合わせ。相方のみの指摘 7 件はすべて該当箇所と実害シナリオが具体的で根拠が強い。

| 指摘 | 採否 | 反映 |
|---|---|---|
| Major 1: 同梱 README の XAML 例が改名後に動かない | 採用 (オーナー判断: 追随を本変更に含める) | tasks 5.2 / 6.2b、spec「README の例による消費者ビルド」、proposal の What Changes / Non-Goals |
| Major 2: `Directory.Build.props` で TFM 依存の最低 OS 値を導出できない、比較方法未決 | 採用 | design Decision 5 (csproj の TFM 条件を残し定数参照、`VersionLessThan`、`-getProperty` で確認、代替案 D)、tasks 4.1 / 4.2 |
| Major 3: platform 外 (net10.0 / outer build / 間接参照) でガードが無効なことを検証できない | 採用 | design Decision 5 の Condition、spec「非 platform TFM と outer build ではガードが動かない」、tasks 6.4 |
| Major 4: README 画像の検証が prefix 検査に縮退、ブランチ未確定 | 採用 (public 化済み・既定ブランチ `develop` を確認し確定) | design Decision 6 (代替案 C 追加、Open Questions 解消)、spec の THEN に取得成功と Content-Type、tasks 5.1 |
| Minor 1: `PackageReadmeFile` の置き場が ADR-0025 の記述と不一致 | 採用 | design Decision 6 に判断を明記、ADR 候補節に ADR-0025 への注記を追加 |
| Minor 2: binding 2 件の共通メタデータ検証が不足 | 採用 | spec「nuspec のメタデータと Version の既定・注入」を 3 パッケージに拡張、tasks 3.3 |
| Minor 3: 一時消費者検証の SDK と取得元が固定されていない | 採用 | spec「消費者からの導入」に SDK 固定・隔離 packages path・取得元記録、tasks 6.1 |

未解決: なし。相方の総合判定 NEEDS_DISCUSSION の 3 論点 (README と docs-refresh の順序 / MSBuild 評価位置 / 画像 URL) はいずれも上記で確定した。
