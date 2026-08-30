# Candidate: monorepo-foundation

## 概念候補

### クロスプラットフォーム・リポジトリ境界 (提案カテゴリ: architecture/)

KsSettingsView は、複数プラットフォームで対応する能力を同じ変更単位として扱えるよう、製品コード、サンプル、設計知識を単一リポジトリに集約する。一方、ビルドと依存解決はプラットフォーム固有のツールチェインに委ね、各プラットフォームを独立したビルドルートとして保つ。

この境界においてリポジトリルートが担うのは、横断変更の調整、製品識別、文書への入口である。全プラットフォームを一つのビルドグラフへ統合する責務は持たず、各ビルドルートが自身のモジュール構成、依存方向、テスト入口を所有する。サンプルはライブラリ本体の配布物ではなく利用側として分離し、プラットフォーム標準のローカル参照機構を通じて本体を検証できる。

不変条件は、横断的な製品変更を単一リポジトリ内で同期できることと、あるプラットフォームのビルド成立が別プラットフォームのツールチェインを要求しないことである。この capability はランタイムの公開 API を提供せず、外部に約束する面はリポジトリの責務分割、独立したビルド入口、サンプルから本体への検証経路である。

出典: `README.md`、`ios/Package.swift`、`android/settings.gradle.kts`、`android/ks-settingsview-core/build.gradle.kts`、`android/ks-settingsview-ui/build.gradle.kts`、`android/ks-settingsview-compose/build.gradle.kts`、`maui/KsSettingsView.slnx`、`samples/ios/KsSettingsViewSample.xcodeproj/project.pbxproj`、`samples/android/settings.gradle.kts`、`openspec/specs/monorepo-foundation/spec.md` Purpose、`docs/overview.md`

### 公開識別子の名前空間 (提案カテゴリ: conventions/)

公開識別子は、所有するドメインと製品名を基準に、エコシステムごとの慣例へ写像する。Apple と Android では所有ドメインを逆順にした lowercase の名前空間を共有し、.NET では同じ製品境界を PascalCase の名前空間で表す。Maven の公開座標は所有ドメインに対応する groupId を基点とする。

識別子の表記を全エコシステムで文字列として統一することは目的ではない。所有主体と製品境界を一貫させながら、各配布基盤で衝突せず、後続モジュールが同じ規則から識別子を導けることが不変条件である。具体的には Apple / Android の接頭辞を `jp.kamusoft.kssettingsview.*`、Maven Central の groupId を `jp.kamusoft`、.NET の接頭辞を `KsSettingsView.*` とする。

出典: `samples/ios/KsSettingsViewSample.xcodeproj/project.pbxproj`、`android/ks-settingsview-*/build.gradle.kts`、`samples/android/app/build.gradle.kts`、`openspec/specs/monorepo-foundation/spec.md`「命名規約とパッケージ ID」、`docs/overview.md`、`kasane/decisions/0002-public-identifier-namespace.md`

## ADR 候補

- 新規候補なし。モノレポと独立ビルドルートの判断は ADR-0001、公開識別子の判断は ADR-0002 として accepted 済み。

## drift 所見

- spec は命名規約の参照先を `docs/conventions.md` とするが、このファイルは存在せず、現行の命名規約は `docs/overview.md`「命名規約・パッケージ ID」に集約されている (`openspec/specs/monorepo-foundation/spec.md`「命名規約のドキュメント化」 / `docs/overview.md`)。
- spec は最低ツールチェインの参照先を `docs/development.md` とするが、このファイルは存在せず、現行の最低要件とビルド入口は `docs/overview.md` に集約されている (`openspec/specs/monorepo-foundation/spec.md`「開発環境ドキュメントの存在」 / `docs/overview.md`)。
- spec・文書・ADR-0002 は Maven Central の groupId を `jp.kamusoft` とする一方、現行の3つの Android ライブラリビルドと Sample の依存座標は `jp.kamusoft.kssettingsview` を使用している。公開用 publication 設定はまだ存在しないため、意図した公開座標と現行 Gradle group のどちらを正とするかは未確定 (`openspec/specs/monorepo-foundation/spec.md`「パッケージ ID の規約」 / `docs/overview.md` / `kasane/decisions/0002-public-identifier-namespace.md` / `android/ks-settingsview-core/build.gradle.kts` / `android/ks-settingsview-ui/build.gradle.kts` / `android/ks-settingsview-compose/build.gradle.kts` / `samples/android/app/build.gradle.kts`)。
- `README.md` と `docs/README.md` は OpenSpec specs を仕様の SSoT と説明しているが、現行の Kasane 運用ではコードとテストが現仕様の SSoT であり、OpenSpec 資産は移行後に歴史資料として凍結する方針である (`README.md` / `docs/README.md` / `AGENTS.md` / `kasane/changes/migrate-openspec/proposal.md`)。

## 用語

- モノレポ: 複数プラットフォームの製品コード、サンプル、設計知識を、横断変更を同期できる一つのリポジトリで管理する方式。
- ビルドルート: プラットフォーム固有のビルド入口、モジュール依存、テスト入口を所有し、他プラットフォームから独立して開ける単位。
- ローカルソース参照: Sample を利用側として分離したまま、同一リポジトリ内のライブラリ本体をソースとして組み込む開発時の参照方式。iOS では Local Swift Package、Android では composite build を用いる。
- composite build: Android の Sample build とライブラリ build を独立したルートとして保ちながら、Gradle がローカルのモジュール座標を本体プロジェクトへ置換する構成。
- 公開識別子: バンドル ID、Android package / namespace、Maven groupId、.NET namespace など、所有主体と製品境界を外部へ示す名前。

## 抽出メモ

独立概念は「クロスプラットフォーム・リポジトリ境界」と「公開識別子の名前空間」の2件を提案する。前者は後続の platform capability と Sample capability の共通土台であり、後者は conventions として分離すると、将来の MAUI / KMP 追加時にも参照しやすい。

具体的なディレクトリ一覧、モジュール名、ツールチェインの現在値はコードとビルド設定から再導出でき、更新頻度も高いため概念候補には含めない。最低バージョンを明示して利用者の環境選定を可能にする責務だけを、統合時に残すか検討する余地がある。

この capability 専用の自動テストは存在しない。現行契約は各ビルドマニフェスト、iOS Sample の Local Swift Package 参照、Android Sample の composite build 設定により確認した。MAUI は空のソリューション入口だけがあり、実装モジュールと .NET 名前空間はまだコードから検証できない。
