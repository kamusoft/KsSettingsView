# Proposal: add-consumer-verification

## Why

3 platform のパッケージング (SwiftPM 配信リポジトリ / Maven 発行構成 / NuGet pack) は整ったが、「配布物を利用者と同じ経路で解決してビルドできるか」の確認は、各フェーズで一時プロジェクトを手で作って行った証跡が残るだけで、リポジトリ内に再実行できる形では存在しない。ルート README の最小コード例も `skills/` との文字列一致でしか担保されておらず、実際にビルドが通るかは未検証である。release workflow (phase-8) は publish 前の dry-run と publish 後の smoke を必要とし、その器がこの変更である。

設計判断はフェーズ議論で決着済み ([agenda](../../roadmaps/package-distribution/phases/phase-7-consumer-verification/agenda.md) の決定事項 7 件)。本提案はそれをアーティファクトに落とす。

## What Changes

- **`verification/` の新設**: 配布物を参照する消費者プロジェクトを platform ごとに置く
  - `verification/ios/`: SwiftPM パッケージ (`Package.swift` はテンプレートから生成、`platforms: iOS 16`、1 target)。README の iOS 最小例をソースとして同梱
  - `verification/android/`: `com.android.application` の app module 1 つ。AGP / Kotlin / Compose BOM の版は `android/gradle/libs.versions.toml` を共有する (Sample と同じ)。README の Android 最小例を同梱
  - `verification/maui/`: `dotnet new maui` 相当のアプリ (TFM は `net10.0-android;net10.0-ios`、`MauiVersion` 10.0.70、最低 OS 版 Android 29 / iOS 16.0)。README の MAUI 最小例 (XAML + `MauiProgram`) を同梱
  - application ID は既存規則から導出する: `jp.kamusoft.kssettingsview.verification.android` / `.maui`
- **参照先とモードの切り替え**: 各消費者はモード (`dry-run` / `smoke`) と version の 2 引数を受け取る
  - iOS: dry-run はスナップショット (`scripts/spm-snapshot/sync-snapshot.sh` の出力を一時ディレクトリ `KsSettingsView-SPM` に配置) への `path:` 参照、smoke は `https://github.com/kamusoft/KsSettingsView-SPM` + `exact:`
  - Android: Gradle プロパティで version と参照先を切り替え。`jp.kamusoft` はモードに応じて mavenLocal / mavenCentral のどちらか 1 つに `exclusiveContent` で排他的に割り当て、無ければ他のリポジトリを検索せず失敗する
  - MAUI: MSBuild プロパティで version、`nuget.config` 2 枚 (dry-run はローカルフォルダフィード、smoke は nuget.org。いずれも packageSourceMapping で `KsSettingsView.*` の取得元を 1 つに固定し、他は nuget.org) を `-p:RestoreConfigFile=` で選ぶ。mapping は global packages folder に既にあるパッケージには働かないため、実行ごとに空の `RestorePackagesPath` を使い、取得元はパッケージ単位の `.nupkg.metadata` で検査する
  - dry-run で version 未指定なら platform ごとの開発用 version、smoke は version 必須。許可値以外のモードは早期に失敗する
- **実行スクリプト**: `verification/<platform>/` に「フィード準備」(スナップショット配置 / `publishToMavenLocal` / `pack`) と「消費者ビルド」の 2 段のスクリプト。release では準備段を publish job の成果物 (CI artifact として受け渡す) で置き換えられる
- **検査**: 消費者ビルドは Release 構成 (MAUI iOS は Simulator RID を明示し署名情報を要求しない)。MAUI は restore 警告 (NU1605 / NU1608 / NU1107) をエラー扱いにし、binding 2 件の解決版が facade と一致することを検査する。解決結果の証跡は Android が依存ツリー、iOS が dry-run では依存グラフ表示 (path 参照は `Package.resolved` に残らない)・smoke では `Package.resolved`、MAUI が解決版と取得元
- **README 一致 lint**: `scripts/readme-example-lint.py` (仮) が README.md の最小例 4 コードブロック (iOS / Android / MAUI XAML / MAUI C#) と `verification/` の対応 4 ファイルの完全一致を検査し、CI の lint job に加える
- **CI**: platform 別の再利用可能 workflow 3 本 `verify-consumer-{ios,android,maui}.yml` (`workflow_call`、入力 `mode` / `version` (任意) / `artifact` (任意。publish 段の成果物を受け取る)) を新設し (cross/ADR-0025 の形に揃える)、`ci.yml` から `mode=dry-run` で毎回呼ぶ (status check 名 `consumer-<platform> / verify`)。release workflow からの呼び出し (dry-run / smoke) は phase-8
- **branch protection (GitHub 設定操作)**: `develop` の必須 status check に消費者検証 3 job を追加する (`main` は未作成のため phase-8 の作成時に設定)

影響する能力: consumer-verification (新設)、verification-ci (job 構成と lint の拡張)

## Non-Goals

- **release workflow からの呼び出し (publish 前 dry-run / publish 後 smoke の job 化) と smoke 正ケース (公開レジストリからの解決成功) の実証** — phase-8 の守備範囲。配布物が未公開のため本変更で実証できるのは smoke の参照先設定の生成まで
- **`main` の branch protection** — `main` が未作成 (phase-8 の作成時に 7 job で設定する申し送り済み)
- **Simulator / Emulator での起動、`dotnet publish`、実機** — agenda 決定 (検証範囲)。実行時挙動は Sample と手元手順が担う (cross/ADR-0026)
- **AndroidX 解決版の期待値照合** — agenda 決定 (CPM 更新との二重管理になる。競合の不在は NU1107 / NU1608 で担保)
- **XA4301 (Android Release ビルドの警告 4 件) の解消と NU1507 の恒久対処** — phase-8 の申し送り。本変更ではビルド警告をエラーにしない
- **`verification/` の Sample パリティ規約・ADR-0016 への言及追加** — agenda 決定 (規約は改訂せず、役割は蒸留時に concepts へ記述)
- **`README_ja` の最小例との一致検査** — 英日同期は docs-refresh の責務
- **API 版付き TFM の解決要件の README / skills への反映** — 実測は本変更のタスクに含めるが、文書反映は docs-refresh 依頼 (agenda の TODO)

## Impact

- 破壊的変更なし。ライブラリのコード・テスト・pack 構成には触れない
- CI の所要時間が増える (macOS 2 job + ubuntu 1 job、並列。フィード準備込みで各 5〜10 分見込み)。必須 status check が 4 → 7 job になる
- README の最小例がそのままビルド対象になるため、例が壊れていれば本変更の実装中に露見する。その場合は README を同じ変更で修正する (README は cross/ADR-0023 の 4 枚の 1 つで、例の修正は docs-refresh 依頼にも含める)
- リスク: iOS の `path:` 参照で package identity がディレクトリ名から決まる前提、MAUI の packageSourceMapping + 空の packages path、Android の `exclusiveContent` は机上確定のため、tasks の冒頭で実測する (lessons process L-004)
- セカンドオピニオン (second-opinion-spec-001.md) の指摘を反映済み: Gradle の content filter は排他でないため `exclusiveContent` に変更、NuGet の global cache 迂回対策、artifact 受け渡し契約、platform 別 workflow、iOS 証跡、Simulator RID、`main` と smoke 正ケースの phase-8 送り

## 級: L

当初は M (phase-3 の検証 CI と同じ構図) として起票したが、セカンドオピニオン (second-opinion-spec-001.md) で参照先の排他性・キャッシュ隔離・workflow 間の成果物受け渡しなど設計判断が複数出たため、2 能力 (consumer-verification / verification-ci) にまたがる点と合わせて L に改めた (2026-09-02 オーナー判断)。設計判断と却下した代替案は design.md に集約する。

domain: cross
roadmap: package-distribution/phase-7-consumer-verification
