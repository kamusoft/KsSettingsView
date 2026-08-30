## Why

`add-monorepo-foundation` で `samples/ios/` ディレクトリと placeholder の `README.md` のみが配置された状態であり、`add-settings-view-ios-ui` で実装された KsSettingsView (Core / UI / SwiftUI) を実機・シミュレータで目視確認できる Sample アプリ本体が存在しない。後続の `add-cell-types-basic` / `add-cell-types-input` / `add-cell-types-custom` には「`samples/ios/` SwiftUI Sample に各 Cell 表示例を追加する」というタスクが含まれているが、Sample アプリの土台がいつ作られるかを定義した変更提案が存在せず、依存順序が宙に浮いている。Sample アプリ土台を独立した capability として切り出すことで、責務と依存関係を明確化する。

## What Changes

- 新ディレクトリ `samples/ios/` 配下に Xcode プロジェクト形式の SwiftUI Sample アプリ（最小構成）を作成
  - アプリ名: `KsSettingsViewSample`（仮）
  - 言語: Swift 6（Swift Concurrency 有効）
  - 最小 OS: iOS 16
  - Bundle Identifier プレフィックス: `jp.kamusoft.kssettingsview.samples.ios`
- Swift Package Manager 経由で `KsSettingsViewCore` / `KsSettingsViewUI` / `KsSettingsViewSwiftUI` をローカル参照
  - 参照方式: 親リポジトリの `ios/Package.swift` を Local Package として組み込む
- Sample アプリ内で独自の最小 Cell `SampleLabelCell`（`KsCell` 準拠 / id・title のみ）と Renderer `SampleLabelCellView`（`UICollectionViewListCell` + `KsCellRenderer` 実装）を定義し、`KsCellRegistry.shared.register(...)` で登録
  - 既存の `PoCLabelCell` / `PoCLabelCellView` は `KsSettingsViewUI` モジュール内 `internal` のため Sample から直接利用できない。Sample 専用 Cell を別途定義して PoC 相当の表示を実現する
- 1 ページのデモ画面を `ContentView` から表示
  - SwiftUI 側で `@State private var root: SettingsRoot = ...` を保持し、`KsSettingsView(root: $root)` をラッパとして使用（`KsSettingsView` の API は `Binding<SettingsRoot>` を要求する）
  - `SettingsRoot { Section { ... } }` の DSL 形式で 1 セクション・複数行の `SampleLabelCell` を表示
- `samples/ios/README.md`（現状 placeholder）を実 Sample のクイックスタート README に置き換え
  - Xcode で開く手順、シミュレータでの起動手順、依存関係の説明を含む
- 「含まないこと」（後続提案で対応）：
  - 各種 Cell（Label / Switch / Command / Entry / Picker / Custom 等）のデモページ追加 → `add-cell-types-*` 群
  - CI 連携 / スナップショットテスト
  - Theme 切替 UI / Style 切替 UI（必要なら後続）

## Capabilities

### New Capabilities
- `samples-ios`: `samples/ios/` 配下に配置される iOS Native (SwiftUI) Sample アプリの構造・依存・起動可能性に関する振る舞いを規定する

### Modified Capabilities
（なし。本提案は純粋な追加であり、`monorepo-foundation` spec の placeholder Scenario は引き続き有効。後続の別変更提案で `add-settings-view-ios-ui` の完了条件と Cell 系提案の Sample 関連タスクを本 capability に整合させる）

## Impact

- 影響範囲: `samples/ios/` 配下の新規 Xcode プロジェクト・SwiftUI ソース・README
- 依存:
  - `add-monorepo-foundation`（archive 済）: `samples/ios/` ディレクトリと placeholder README が存在する前提
  - `add-settings-view-core`（archive 済）: `SettingsRoot` / `Section` 等のモデルを使用
  - `refactor-accessory-and-root-hf`（archive 済）: `KsAnyView` / `RootAccessory` / `SectionAccessory` を使用
  - `add-settings-view-ios-ui`（実装完了・APPROVED・VALID 済）: `KsSettingsViewUI` / `KsSettingsViewSwiftUI` の公開 API（`KsSettingsView` / `KsCell` / `KsCellRenderer` / `KsCellRegistry` 等）を使用
- 後続が依存:
  - `add-cell-types-basic` / `add-cell-types-input` / `add-cell-types-custom`: 本 Sample にページ追加する
  - `add-settings-view-ios-ui` 側の完了条件「PoCLabelCell を含む SettingsRoot を SwiftUI Sample で表示すると 1 行のセルがテキスト付きで描画される」は、本提案完了後に Sample 専用 `SampleLabelCell` を使った検証へ置き換える形で吸収される（既存提案側の文言修正は本提案完了後に別途行う）
- リスク: 低。Xcode プロジェクト構成（`.xcodeproj` / `.xcworkspace`）は標準テンプレートに準拠し、KsSettingsView 本体への影響はない
