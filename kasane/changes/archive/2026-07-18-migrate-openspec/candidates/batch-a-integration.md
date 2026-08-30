# バッチA 統合結果 — 基盤と Core

統合日: 2026-07-17
レビュー確定日: 2026-07-18

対象:

- `monorepo-foundation`
- `settings-view-core`
- 補助資料: `docs/overview.md`、`docs/core-model.md`、`docs/architecture.md`、`docs/README.md`

## 統合後の concepts 案

### `architecture/repository-boundaries.md`

- 単一リポジトリは横断変更、製品識別、知識への入口をまとめる。
- iOS、Android、MAUI は独立したビルドルートとして、自身の依存方向とテスト入口を所有する。
- Sample は配布物ではなく利用側として分離し、プラットフォーム標準のローカル参照で本体を検証する。
- 現在のディレクトリ一覧、ツールバージョン、コマンド一覧は記載しない。

### `conventions/public-identifiers.md`

- 所有ドメインと製品名から、各エコシステムの慣例に合わせて公開識別子を導く。
- Apple / Android、Maven、.NET で文字列表現を同一化するのではなく、所有主体と製品境界を一貫させる。
- Maven groupId の drift が未解決のため、値の確定はユーザー判断後に行う。

### `core-model/settings-tree.md`

- Core は描画を行わない論理モデルとして、Root、Section、Cell、装飾の語彙を提供する。
- UIスタイル、具象Cell、状態保持と適用はUI層の責務とする。
- Root / Section / Cell の順序、空状態、装飾とCellの区別、任意Viewの値等価に関する不変条件をまとめる。
- 「プラットフォーム型を一切含まない」という実装と矛盾する表現は採用しない。

### `architecture/display-state-synchronization.md`

- 値等価、構造同一性、内容再構成、可視性 projection を別の契約として整理する。
- 構造は識別子、内容は同一行の再構成、可視性は model と visible projection の再構築で扱う。
- 具体的なクラス名、メソッド名、Diffケース一覧は記載しない。

### `core-model/structural-changes.md`

- Core の変更値は状態を保持せず、更新意図だけを UI 層へ渡す。
- Section / Cell の構造操作、同一Cellの内容再構成、装飾更新の意味論を整理する。
- Theme 更新は構造変更に含めない。
- 現在の enum / sealed class のケース列挙やシグネチャはコードから再導出できるため記載しない。

## ADR 統合判断

新規ADR候補はない。抽出した判断はADR-0001、0002、0003、0005、0006、0010に対応済み。

ただしADR-0003のプラットフォーム別Cell表現は現行コード・specと矛盾する。accepted ADRは変更せず、コード・テスト・現行specを正とする場合は、次の内容で新ADRを作りADR-0003をsupersedeする必要がある。

- 値型中心・薄いCell抽象という方針は維持する。
- Swiftは型消去ラッパを使わず、異種Cellを existential collection として保持する。
- Kotlinは外部モジュールから独自Cellを実装できる通常interfaceとする。
- 網羅的な型分岐より拡張可能性を優先する。

## drift 所見の統合

| ID | 所見 | 根拠 | 推奨する扱い |
|---|---|---|---|
| A-1 | Coreをプラットフォーム型非依存とする説明に対し、任意View payloadはSwiftUI/UIKit/Android View/Composeへ直接依存する | code ↔ spec/docs | コードを正とし、「描画責務を持たない論理モデル」へ説明を弱める |
| A-2 | docsとADR-0003はSwift型消去ラッパを採用するが、現行コード・specは existential collection を使う | code/spec ↔ docs/ADR | ADR-0003を新ADRでsupersedeし、docsは後続で修正 |
| A-3 | ADR-0003はKotlin `sealed interface` を採用するが、現行コード・specは外部実装可能な通常interfaceを要求する | code/spec ↔ ADR | A-2と同じ新ADRでsupersede |
| A-4 | Kotlinのタイトル配置列挙値がdocs/specではlowercase、コード・テストではuppercase | code/test ↔ spec/docs | コードを正とし、conceptsにはケース名を転記しない。docsは後続で修正 |
| A-5 | specが参照する `docs/conventions.md` は存在せず、内容は `docs/overview.md` にある | spec ↔ docs | 凍結specは修正せず、conceptsの新しい入口で置換 |
| A-6 | specが参照する `docs/development.md` は存在せず、内容は `docs/overview.md` にある | spec ↔ docs | 凍結specは修正せず、conceptsには変動しやすいツール値を移さない |
| A-7 | ADR/spec/docsのMaven groupIdは `jp.kamusoft`、現行Gradle groupは `jp.kamusoft.kssettingsview` | code ↔ ADR/spec/docs | 公開用設定が未実装のため、正とする値をユーザー判断 |
| A-8 | READMEとdocs/READMEはOpenSpec specsを仕様SSoTとするが、現運用はコードとテストがSSoT | docs ↔ AGENTS/Kasane | Kasane移行完了時にREADME/docsの案内を更新 |

## 推奨レビュー結果

1. 上記5concepts案を承認する。
2. A-1、A-4〜A-6、A-8は推奨扱いで確定する。
3. A-2 / A-3はコード・specを正としてADR-0003をsupersedeする。
4. A-7はADR-0002どおり公開Maven groupIdを `jp.kamusoft` とし、現行Gradle groupを実装driftとして後続対応する。

## レビュー結果

- 5 conceptsを承認。
- A-7は一度「現行Gradleを優先」と判断したが、Maven座標の役割を再調査した結果、その判断を撤回した。
- Maven `groupId` は組織・project groupを表す `jp.kamusoft`、`artifactId` は個別成果物を表す `ks-settingsview-*` とする。
- ADR-0012案は却下し、ADR-0002を維持する。現行Gradleの `group = "jp.kamusoft.kssettingsview"` は実装driftとして後続対応する。
- ADR-0013をacceptedとし、ADR-0003をsupersededとする。

確認根拠:

- Maven公式「[Naming convention of Maven coordinates](https://maven.apache.org/guides/mini/guide-naming-conventions.html)」: `groupId` はproject group、`artifactId` はartifact名として定義される。
- Maven公式「[POM Reference](https://maven.apache.org/pom.html#Maven_Coordinates)」: `groupId` は一般に組織またはprojectで一意、`artifactId` は一般にprojectが知られる名前として説明される。
- Maven Central公式「[Namespaces vs groupIds](https://central.sonatype.org/faq/namespaces-vs-groupids/)」: namespaceはgroupIdの接頭辞であり、groupIdは関連成果物を整理する単位として説明される。
