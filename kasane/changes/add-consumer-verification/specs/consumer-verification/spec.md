# consumer-verification デルタスペック

## ADDED Requirements

### Requirement: 消費者プロジェクトの構成
リポジトリは、配布物を利用者と同じ経路で参照する消費者プロジェクトを `verification/` 配下に platform ごと (iOS / Android / MAUI) に持つ SHALL。iOS は SwiftPM パッケージ、Android は application module、MAUI は MAUI アプリであり、いずれも本体のソースを参照せず、パッケージ (SwiftPM product / Maven artifact / NuGet パッケージ) だけを参照する。各消費者は、ルート README の当該 platform の最小コード例 (iOS / Android / MAUI XAML / MAUI C# の 4 コードブロック) をソースとして含む。

#### Scenario: 本体ソースへの参照を持たない
- **GIVEN** `verification/` 配下のいずれかの消費者プロジェクト
- **WHEN** その依存宣言を確認する
- **THEN** 本体 (`ios/` / `android/` / `maui/`) へのローカルソース参照 (Local Swift Package・composite build・ProjectReference) は存在せず、公開座標 (product `KsSettingsView` / `jp.kamusoft:kssettingsview` / `KsSettingsView.Maui`) への参照だけがある

#### Scenario: README の最小例がそのままビルド対象になる
- **GIVEN** ルート README の 4 コードブロック
- **WHEN** 各消費者プロジェクトをビルドする
- **THEN** 最小例のコードがコンパイル対象に含まれ、例が壊れていればビルドが失敗する

### Requirement: モードと version の指定
各消費者の検証は、モード (`dry-run` / `smoke`) と version を外部から受け取って動作する SHALL。`dry-run` で version の指定がなければ platform ごとの開発用 version (Android は `0.1.0-SNAPSHOT`、MAUI は `0.0.0-dev`、iOS はスナップショット参照のため version を持たない) で動作する。`smoke` では version が必須である。許可値以外のモード、および `smoke` で version が無い場合は、フィード準備や依存解決に入る前に失敗する。消費者のソース (README 最小例を含む) はモードによって変わらない。

#### Scenario: 引数なしで dry-run が動く
- **GIVEN** 手元の作業環境で本体がビルドできる状態
- **WHEN** 引数を与えずに消費者検証を実行する
- **THEN** dry-run として、platform ごとの開発用 version の配布物をローカルの参照先から解決してビルドする

#### Scenario: version を与えると全 platform に同じ文字列が流れる
- **GIVEN** version `X.Y.Z-rc.1` を指定した実行
- **WHEN** 各 platform の消費者が依存を解決する
- **THEN** iOS の `exact:` (smoke 時)、Android の依存座標、MAUI の `PackageReference` のいずれも同じ `X.Y.Z-rc.1` を用いる

#### Scenario: 不正な入力は早期に失敗する
- **GIVEN** モードに `dry-run` / `smoke` 以外の値を与えた、または `smoke` で version を省いた実行
- **WHEN** 消費者検証を起動する
- **THEN** フィード準備・依存解決を行わずに失敗として報告され、理由が出力で分かる

### Requirement: dry-run の参照先
`dry-run` では、本リポジトリ由来の配布物をローカルの参照先だけから解決する SHALL。参照先は iOS がスナップショット (配信リポジトリのルートと同じファイル配置で、identity が `KsSettingsView-SPM` になるディレクトリ) への `path:` 参照、Android が mavenLocal、MAUI がローカルフォルダフィードである。ローカル参照先は本リポジトリ由来の座標 (group `jp.kamusoft` / `KsSettingsView.*`) について排他的であり、そこに無ければ公開レジストリやユーザー環境のキャッシュへフォールバックせず失敗する。MAUI では実行ごとに空のパッケージ展開先を使い、既存のユーザーキャッシュを参照しない。本リポジトリ由来以外の依存 (AndroidX・MAUI 本体等) は通常の公開リポジトリから取得してよい。dry-run は配信リポジトリ・Maven Central・NuGet.org に対して書き込み (tag・upload・push) を行わない。

#### Scenario: 本リポジトリ由来の座標はローカル参照先からのみ取得される
- **GIVEN** dry-run の実行
- **WHEN** 依存解決が完了する
- **THEN** `KsSettingsView` product / `jp.kamusoft:kssettingsview` / `KsSettingsView.*` パッケージの取得元 (パッケージ単位の記録) はすべてローカル参照先であり、公開レジストリからは取得されていない

#### Scenario: ローカル参照先に無ければ公開済みの版でも失敗する
- **GIVEN** ローカル参照先に指定 version の配布物が置かれておらず、同じ version が公開レジストリまたはユーザー環境のキャッシュに存在する状態
- **WHEN** dry-run を実行する
- **THEN** 依存解決は失敗として報告され、公開レジストリやキャッシュの同名パッケージへフォールバックしない

#### Scenario: 配信先へ副作用を残さない
- **GIVEN** dry-run の実行
- **WHEN** 実行が完了する
- **THEN** 配信リポジトリの tag 一覧・Maven Central の deployment・NuGet.org のパッケージ一覧は実行前と同一であり、dry-run の実行経路は配信先への書き込み権限・認証情報を持たない

### Requirement: smoke の参照先
`smoke` では、本リポジトリ由来の配布物を公開レジストリ (配信リポジトリ `KsSettingsView-SPM` の tag、Maven Central、NuGet.org) から解決する SHALL。消費者側の座標・identity の文字列は dry-run と同一である。公開レジストリから当該 version を解決してビルドが成功することの実証は、配布物が公開される初回リリース (release workflow) で行う。

#### Scenario: 参照先が公開レジストリを指す
- **GIVEN** smoke で version を指定した実行
- **WHEN** 各消費者の依存解決の設定が生成される
- **THEN** iOS は `https://github.com/kamusoft/KsSettingsView-SPM` + 指定 version、Android は Maven Central、MAUI は nuget.org を指し、ローカル参照先を含まない

#### Scenario: 公開レジストリからの解決
- **GIVEN** 指定 version の配布物が 3 つの公開レジストリに存在する状態
- **WHEN** smoke を実行する
- **THEN** 3 platform の消費者が公開レジストリから当該 version を解決し、ビルドが成功する

### Requirement: フィード準備と消費者ビルドの分離
消費者検証の実行手段は「フィード準備」(スナップショット配置 / mavenLocal への発行 / NuGet の pack) と「消費者ビルド」を分けて呼び出せる SHALL。消費者ビルドは、外部で準備済みの配布物 (手元のディレクトリ、または CI の artifact として渡された release workflow の publish 段の成果物) を与えられて動作でき、その場合フィード準備を再実行しない。

#### Scenario: 外部で準備した配布物を消費者に渡す
- **GIVEN** 別の工程で pack / 発行 / 配置された配布物
- **WHEN** フィード準備を行わず、その配布物の場所 (または artifact) を指定して消費者ビルドだけを実行する
- **THEN** 消費者はその配布物を解決してビルドし、フィード準備を再実行しない

### Requirement: 消費者ビルドの成立条件
各消費者は Release 構成でビルドが成功する SHALL。対象は iOS (iOS Simulator 向け)、Android (application の release variant)、MAUI (`net10.0-android` の Release と、`net10.0-ios` の Simulator RID を明示した Release)。署名情報 (provisioning・証明書) を要求せず、Simulator / Emulator / 実機での起動、`dotnet publish`、実機向け署名は検証範囲に含めない。

#### Scenario: 3 platform の Release ビルド
- **GIVEN** dry-run または smoke で依存解決が完了した消費者
- **WHEN** Release 構成でビルドする
- **THEN** 3 platform すべてで署名情報なしにビルドが成功し、いずれかの失敗は検証全体の失敗として報告される

### Requirement: MAUI 消費者の依存検査
MAUI 消費者の restore は、ダウングレード (NU1605)・依存版範囲外 (NU1608)・版競合 (NU1107) の警告を失敗として扱う SHALL。あわせて、解決された `KsSettingsView.Binding.iOS` / `KsSettingsView.Binding.Android` の version が facade `KsSettingsView.Maui` の version と一致することを検査し、不一致は失敗とする SHALL。ビルド警告全般 (XA4301 等) は失敗にしない。AndroidX 等の推移依存の解決版は検査しない。

#### Scenario: 依存警告で失敗する
- **GIVEN** 依存の版競合またはダウングレードが起きる構成
- **WHEN** MAUI 消費者を restore する
- **THEN** restore は失敗として報告される

#### Scenario: binding の version 不一致を検出する
- **GIVEN** 参照先に facade と異なる version の binding しか存在しない状態
- **WHEN** MAUI 消費者の依存検査が走る
- **THEN** 検査は失敗として報告され、facade と binding それぞれの解決版が出力で確認できる

#### Scenario: ビルド警告は失敗にしない
- **GIVEN** restore に警告がなく、ビルドで XA4301 のような警告だけが出る状態
- **WHEN** MAUI 消費者を Release ビルドする
- **THEN** ビルドは成功として報告される

### Requirement: 解決結果の証跡
各消費者の検証は、本リポジトリ由来の配布物について解決版と取得元 (ローカル参照先か公開レジストリか) を実行ログまたは出力ファイルとして残す SHALL。iOS は dry-run では生成したマニフェストと依存グラフの表示 (path 参照には version が無いことを含む)、smoke では `Package.resolved` の URL・revision・version。Android は依存ツリーの `jp.kamusoft` 行。MAUI はパッケージ単位の解決版と取得元。CI では job summary で確認できる。

#### Scenario: 解決版と取得元が読める
- **GIVEN** 消費者検証の実行
- **WHEN** 実行が完了する
- **THEN** 3 platform について、本リポジトリ由来の配布物の解決版 (iOS dry-run は path と identity) と取得元が出力から特定できる

### Requirement: README 最小例との一致
ルート README (英語) の 4 コードブロック (iOS / Android / MAUI XAML / MAUI C#) と、`verification/` 配下の対応する 4 ソースファイルは完全一致する SHALL。一致は lint で検査し、不一致は失敗とする。`README_ja` は対象外 (英日同期は docs-refresh の責務)。

#### Scenario: 例の変更が消費者に追随していなければ失敗する
- **GIVEN** README の最小例と消費者のソースのどちらか一方だけが変更された状態
- **WHEN** lint を実行する
- **THEN** 不一致のコードブロックとファイルが出力され、lint は失敗として報告される

#### Scenario: 一致していれば通る
- **GIVEN** 4 コードブロックと 4 ファイルが一致している状態
- **WHEN** lint を実行する
- **THEN** lint は成功として報告される
