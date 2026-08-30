## 依存関係

- 前提:
  - `add-monorepo-foundation`（archive 済）: `samples/ios/README.md` placeholder が存在
  - `add-settings-view-core`（archive 済）: `KsCell` プロトコル / `SettingsRoot` / `Section` / `CellStyle` / `Theme` 等のモデルを使用
  - `refactor-accessory-and-root-hf`（archive 済）: `KsAnyView` / `SectionAccessory` を使用
  - `add-settings-view-ios-ui`（実装完了・APPROVED・VALID 済）: `KsSettingsViewUI` の公開 API（`KsSettingsViewController` / `KsSettingsViewStyle` / `KsCellRenderer` / `KsCellRegistry`）と `KsSettingsViewSwiftUI` の `KsSettingsView` ラッパ・DSL を使用
- 後続:
  - `add-cell-types-basic` / `add-cell-types-input` / `add-cell-types-custom`: 本 Sample にページ追加する

## 1. Xcode プロジェクト作成

- [x] 1.1 `samples/ios/` 配下に Xcode プロジェクト `KsSettingsViewSample.xcodeproj` を作成（テンプレート: iOS App, Interface: SwiftUI, Language: Swift）
- [x] 1.2 Bundle Identifier を `jp.kamusoft.kssettingsview.samples.ios` に設定
- [x] 1.3 Display Name を `KsSettingsView Sample` に設定
- [x] 1.4 Deployment Target を iOS 16.0 に設定
- [x] 1.5 Swift Language Version を 6 に設定
- [x] 1.6 不要な自動生成ファイル（テストターゲット等）を整理する

## 2. KsSettingsView パッケージ参照

- [x] 2.1 リポジトリルート相対の `ios/Package.swift` を Local Swift Package として `KsSettingsViewSample.xcodeproj` に追加
- [x] 2.2 ターゲットの `Frameworks, Libraries, and Embedded Content` に `KsSettingsViewCore` / `KsSettingsViewUI` / `KsSettingsViewSwiftUI` をリンク
- [x] 2.3 `import KsSettingsViewCore` / `import KsSettingsViewUI` / `import KsSettingsViewSwiftUI` がコンパイル可能であることを確認

## 3. SwiftUI App エントリポイント

- [x] 3.1 `@main struct KsSettingsViewSampleApp: App` を実装し、`WindowGroup { ContentView() }` を返す
- [x] 3.2 不要な AppDelegate を削除（SwiftUI App ライフサイクルのみで構成）

## 4. Sample 専用 Cell の定義と登録

- [x] 4.1 Sample アプリ内に `struct SampleLabelCell: KsCell` を定義（プロパティ: `id: UUID` / `style: CellStyle` / `title: String`、初期化子で `id = UUID()` / `style = CellStyle()` を既定値とする）
- [x] 4.2 Sample アプリ内に `final class SampleLabelCellView: UICollectionViewListCell, KsCellRenderer` を定義し、`render(cell:theme:)` で `cell as? SampleLabelCell` の `title` を `defaultContentConfiguration().text` に設定する
- [x] 4.3 `SampleLabelCellView.prepareForReuse()` で `contentConfiguration = nil` / `backgroundConfiguration = nil` を実行（再利用時の状態残り防止）
- [x] 4.4 `KsSettingsViewSampleApp.init()` または `ContentView.init()` で `KsCellRegistry.shared.register(cellType: SampleLabelCell.self, rendererType: SampleLabelCellView.self)` を呼び出して登録

## 5. ContentView デモ画面

- [x] 5.1 `ContentView` を `View` プロトコル準拠で実装
- [x] 5.2 `ContentView` 内に `@State private var root: SettingsRoot = ...` を宣言し、初期値を `SettingsRootBuilder` / `SectionBuilder` の DSL（`SettingsRoot { Section { ... } }` 形式）で構築
- [x] 5.3 `Section` の `header` に `SectionAccessory.text("PoC Section")` を設定
- [x] 5.4 `Section` の `footer` に `SectionAccessory.text("This is a footer")` を設定
- [x] 5.5 Section 内に `SampleLabelCell` を 3 行（id・title が異なる）配置
- [x] 5.6 `body` から `KsSettingsView(root: $root)` を返し、画面いっぱいに表示

## 6. README 整備

- [x] 6.1 `samples/ios/README.md` の placeholder を削除
- [x] 6.2 「概要」セクションを記載（このサンプルアプリが何を示すか）
- [x] 6.3 「必要環境」セクションを記載（Xcode 16+ / iOS 16+ シミュレータ）
- [x] 6.4 「開き方」セクションを記載（`open samples/ios/KsSettingsViewSample.xcodeproj` 等）
- [x] 6.5 「実行手順」セクションを記載（Xcode から `⌘R`、または `xcodebuild` コマンド例）
- [x] 6.6 「ディレクトリ構成」セクションを記載（簡易ツリー）
- [x] 6.7 「関連リンク」セクションを記載（`add-settings-view-ios-ui` 提案 / `docs/ios-ui.md` などへのリンク）
- [x] 6.8 「本体ライブラリのデバッグ」セクションを記載：本 Sample は `ios/Package.swift` を Local Swift Package として参照するため、本体ソース（`KsSettingsViewCore` / `KsSettingsViewUI` / `KsSettingsViewSwiftUI`）にブレークポイントを置いてステップイン可能。本体テストを主軸に走らせる場合は `ios/Package.swift` を直接 Xcode で開く運用と、Sample で動作確認しながら本体を編集する場合は本 Sample プロジェクトを開く運用、両者を使い分ける旨を明記

## 7. 動作確認

- [x] 7.1 Xcode から iOS 16+ シミュレータ（例: iPhone 15 / iPhone 16 等、利用可能なもの）でビルドが成功することを確認
- [x] 7.2 Xcode から Run（⌘R）して、シミュレータで Sample アプリが起動することを確認
- [x] 7.3 起動直後の画面に `SampleLabelCell` の `title` が複数行（3 行）描画されることを目視確認
- [x] 7.4 Section ヘッダ "PoC Section" と Section フッタ "This is a footer" が描画されることを目視確認
- [x] 7.5 `xcodebuild -project samples/ios/KsSettingsViewSample.xcodeproj -scheme KsSettingsViewSample -destination 'platform=iOS Simulator,name=<利用可能な iOS 16+ シミュレータ名>' build` が成功することを確認

## 完了条件

- すべてのタスクのチェックボックスが完了している
- `samples-ios` capability の全 Scenario が満たされている
- `samples/ios/KsSettingsViewSample.xcodeproj` を Xcode 16+ で開き、iOS 16+ シミュレータで Run すると、`SampleLabelCell` を含む 1 セクションのデモ画面が描画される
- `samples/ios/README.md` が実 Sample 用のクイックスタートに置き換わっている
- `xcodebuild` のコマンドラインビルドも成功する
