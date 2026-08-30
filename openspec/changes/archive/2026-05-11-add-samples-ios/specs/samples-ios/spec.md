## ADDED Requirements

### Requirement: iOS Sample アプリの存在

`samples/ios/` 配下に SwiftUI ベースの Sample アプリが Xcode プロジェクト形式で存在しなければならない (SHALL)。Sample アプリは `KsSettingsViewCore` / `KsSettingsViewUI` / `KsSettingsViewSwiftUI` を依存し、Xcode（16+）から開いて iOS シミュレータ（iOS 16+）で起動可能でなければならない (MUST)。

#### Scenario: Xcode プロジェクトの存在

- **GIVEN** リポジトリのクローン直後
- **WHEN** `samples/ios/` 配下を確認する
- **THEN** `KsSettingsViewSample.xcodeproj`（または同等の Xcode プロジェクト）と SwiftUI App エントリポイント（`@main struct ... : App`）を含む Swift ソースファイルが存在する

#### Scenario: KsSettingsView パッケージへの依存

- **GIVEN** `samples/ios/KsSettingsViewSample.xcodeproj` を Xcode で開く
- **WHEN** プロジェクト設定の `Frameworks, Libraries, and Embedded Content` を確認する
- **THEN** リポジトリルート相対の `ios/Package.swift` が Local Swift Package として参照され、`KsSettingsViewCore` / `KsSettingsViewUI` / `KsSettingsViewSwiftUI` の 3 ターゲットがリンクされている

#### Scenario: シミュレータでの起動

- **GIVEN** Xcode でプロジェクトを開いた状態
- **WHEN** iPhone シミュレータをターゲットに `⌘R`（Run）を実行する
- **THEN** ビルドが成功し、シミュレータ上で Sample アプリが起動する

### Requirement: Sample 専用 Cell の定義と登録

Sample アプリは、`KsSettingsViewUI` モジュール内 `internal` の `PoCLabelCell` に依存せず、Sample アプリ内に独自の `SampleLabelCell`（`KsCell` 準拠 / `id` / `style` / `title` の最小プロパティを持つ）と `SampleLabelCellView`（`UICollectionViewListCell` + `KsCellRenderer` 実装）を定義しなければならない (SHALL)。アプリ起動時に `KsCellRegistry.shared.register(cellType: SampleLabelCell.self, rendererType: SampleLabelCellView.self)` を呼び出して登録しなければならない (MUST)。

#### Scenario: SampleLabelCell の存在

- **GIVEN** Sample アプリのソースコード
- **WHEN** Cell モデル定義を確認する
- **THEN** `KsCell` プロトコルに準拠した `SampleLabelCell` 型が Sample アプリ内に定義されている

#### Scenario: SampleLabelCellView の存在

- **GIVEN** Sample アプリのソースコード
- **WHEN** Renderer 定義を確認する
- **THEN** `UICollectionViewListCell` を継承し `KsCellRenderer` プロトコルに準拠した `SampleLabelCellView` 型が Sample アプリ内に定義されている

#### Scenario: KsCellRegistry への登録

- **GIVEN** Sample アプリの起動シーケンス
- **WHEN** `ContentView` が表示される前
- **THEN** `KsCellRegistry.shared` に `SampleLabelCell.self` → `SampleLabelCellView.self` のマッピングが登録されている

### Requirement: SampleLabelCell を含むデモ画面

Sample アプリの起動直後の画面は、`KsSettingsView`（`KsSettingsViewSwiftUI`）を使い、`SampleLabelCell` を 1 セクション・複数行含む `SettingsRoot` を描画しなければならない (SHALL)。

#### Scenario: 起動時の画面表示

- **GIVEN** Sample アプリがシミュレータで起動した直後
- **WHEN** 画面のコンテンツを確認する
- **THEN** `KsSettingsView` が画面いっぱいに表示され、`SampleLabelCell` の `title` を含む 1 行のセルが複数行（2 行以上）描画される

#### Scenario: Section ヘッダ・フッタの描画

- **GIVEN** Sample アプリがシミュレータで起動した直後
- **WHEN** 描画されたセクションを確認する
- **THEN** `SectionAccessory.text(...)` 形式のヘッダおよびフッタが、対応する文字列でセクション境界に表示される

#### Scenario: SwiftUI DSL の使用

- **GIVEN** Sample のソースコードを参照する
- **WHEN** `ContentView` の本文を確認する
- **THEN** `ContentView` 内に `@State private var root: SettingsRoot = ...` が宣言されており、`KsSettingsView(root: $root)` を `body` から返している。`root` の初期値は `SettingsRootBuilder` / `SectionBuilder` の DSL（`SettingsRoot { Section { ... } }` 形式）で構築されている

### Requirement: README の整備

`samples/ios/README.md` は、`add-monorepo-foundation` で配置された placeholder から、実 Sample アプリのクイックスタート README に置き換えられていなければならない (SHALL)。

#### Scenario: クイックスタートの記載

- **GIVEN** `samples/ios/README.md` を開く
- **WHEN** その内容を確認する
- **THEN** 「概要」「必要環境（Xcode 16+ / iOS 16+ シミュレータ）」「開き方（Xcode でプロジェクトを開く手順）」「実行手順（Run / `xcodebuild`）」「ディレクトリ構成」「関連リンク」のいずれにも該当する記載が含まれている

#### Scenario: placeholder からの置き換え

- **GIVEN** `samples/ios/README.md`
- **WHEN** その内容を確認する
- **THEN** 「後続変更提案で追加予定」等の placeholder 文言は残っておらず、実 Sample 用のクイックスタートに更新されている

#### Scenario: 本体ライブラリのデバッグ手順の記載

- **GIVEN** `samples/ios/README.md`
- **WHEN** その内容を確認する
- **THEN** 「本体ライブラリのデバッグ」セクションが存在し、本 Sample が Local Swift Package 参照によって本体ソース（`KsSettingsViewCore` / `KsSettingsViewUI` / `KsSettingsViewSwiftUI`）にブレークポイントを置いてステップインできる旨と、本体テストを主軸に走らせる場合は `ios/Package.swift` を直接 Xcode で開く運用が併記されている

### Requirement: アプリのメタデータ

Sample アプリは、Bundle Identifier プレフィックスとして `jp.kamusoft.kssettingsview.samples.ios` を使用しなければならない (SHALL)。Deployment Target は iOS 16.0 以上、Swift 言語バージョンは 6 でなければならない (MUST)。

#### Scenario: Bundle Identifier の確認

- **GIVEN** Sample アプリのビルド設定
- **WHEN** `PRODUCT_BUNDLE_IDENTIFIER` を確認する
- **THEN** `jp.kamusoft.kssettingsview.samples.ios` で始まる識別子が設定されている

#### Scenario: Deployment Target の確認

- **GIVEN** Sample アプリのビルド設定
- **WHEN** `IPHONEOS_DEPLOYMENT_TARGET` を確認する
- **THEN** `16.0` 以上の値が設定されている
