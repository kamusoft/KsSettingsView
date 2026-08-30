## Context

`add-monorepo-foundation` で `samples/ios/` ディレクトリと placeholder の `README.md` のみが配置された状態で、`add-settings-view-ios-ui` で実装した KsSettingsView (Core / UI / SwiftUI) を実機・シミュレータで目視確認できる Sample アプリ本体が存在しない。本提案では、Sample アプリの土台（Xcode プロジェクト + SwiftUI App エントリ + KsSettingsView ローカル参照 + Sample 専用 `SampleLabelCell` の最小デモ）を独立 capability として確立する。具象 Cell（Label / Switch / Command 等）のデモページは後続の `add-cell-types-*` 群が「ページ追加」として担当するため、本提案ではあえて `SampleLabelCell`（`KsCell` 準拠 / id・style・title のみの最小型）のみの構成にとどめる。なお、`add-settings-view-ios-ui` で配置された `PoCLabelCell` は `KsSettingsViewUI` モジュール内 `internal` のため Sample アプリから直接利用できないので、Sample アプリ側で同等の最小 Cell を独自定義する。

iOS 16+ / Xcode 16+ / Swift 6 が前提で、これは monorepo-foundation 規約に準拠する。Bundle Identifier プレフィックスは `jp.kamusoft.kssettingsview.samples.ios` とし、KsSettingsView 本体（`jp.kamusoft.kssettingsview.*`）と区別する。

## Goals / Non-Goals

**Goals:**
- `samples/ios/` 配下に Xcode プロジェクト形式の SwiftUI Sample アプリを配置
- `KsSettingsViewCore` / `KsSettingsViewUI` / `KsSettingsViewSwiftUI` を **Local Swift Package** として参照
- Sample 専用の Cell 型 `SampleLabelCell`（`KsCell` 準拠）と Renderer `SampleLabelCellView`（`UICollectionViewListCell` + `KsCellRenderer` 実装）を Sample アプリ内に定義し、`KsCellRegistry.shared.register(...)` で登録
- `SampleLabelCell` を含む `SettingsRoot { Section { ... } }` の 1 ページを `ContentView` から表示し、シミュレータ起動時に「タイトル付き 1 行のセル」が複数行描画される
- `samples/ios/README.md` を実 Sample のクイックスタート README に置き換え（Xcode で開く手順、シミュレータでの起動手順）
- 「placeholder のまま実 Sample が配置される」という monorepo-foundation review-result_002.md で言及された懸念を解消
- 後続の `add-cell-types-basic` / `add-cell-types-input` / `add-cell-types-custom` が「ページ追加」のみで Sample を拡張可能な構造（後続の追加が容易な「メニュー画面 → 各デモページ」の素地は本提案では用意せず、後続提案の判断に委ねる）

**Non-Goals:**
- 具象 Cell（Label / Switch / Command / Entry / Picker / Custom 等）のデモページ追加 → `add-cell-types-*` 群
- CI 連携（GitHub Actions などで Sample を build / boot する仕組み）
- スナップショットテスト / UI テスト
- Theme 切替 UI / Style（`.classic` / `.modern`）切替 UI
- iPad 専用レイアウト最適化（iPhone での動作確認を優先）
- App Store 配布のためのアイコン / Launch Screen 整備（標準テンプレートで足りる範囲とする）

## Decisions

### Decision 1: Xcode プロジェクト形式 + Local Swift Package 参照

**選択**: `samples/ios/KsSettingsViewSample.xcodeproj` を作成し、リポジトリルートの `ios/Package.swift` を **Local Swift Package** として `Frameworks, Libraries, and Embedded Content` に追加する。Sample から `import KsSettingsViewCore` / `import KsSettingsViewUI` / `import KsSettingsViewSwiftUI` ができるようにする。

**理由**:
- Local Package 参照ならパッケージのソース変更が即時 Sample に反映され、開発サイクルが短い
- 公開リポジトリ URL や Tag に依存しないため、開発中の `develop` ブランチでも常に最新ライブラリを試せる
- `add-settings-view-ios-ui` で `ios/Package.swift` に `KsSettingsViewUI` / `KsSettingsViewSwiftUI` ターゲットが追加済みのため、追加実装不要

**代替案**:
- リモート Swift Package 参照（`.package(url: ..., from: ...)`）: タグ管理が必要で開発中は不向き。本リポジトリはモノレポであり Local 参照が合理的。
- パッケージなしで直接ソースをコピー: 二重管理になりメンテ困難。却下。
- Swift Package 形式の Executable Target（Xcode プロジェクトを使わない）: 純粋 SwiftUI App は Xcode プロジェクトのほうが標準的で、シミュレータ起動が `xcodebuild` / Xcode IDE の両方から自然。Executable Target は CLI に向くが iOS App には不向き。

### Decision 2: Swift Package Manager 経由のみで参照（CocoaPods / Carthage を使わない）

**選択**: Sample の依存関係は Swift Package Manager のみで完結させる。

**理由**:
- monorepo-foundation の規約で iOS は Swift Package Manager を採用済み
- `Podfile` / `Cartfile` を増やすとメンテ対象が増える

**代替案**:
- CocoaPods 併用: 依存追加時に `pod install` が必要となりオンボーディングが重くなる。却下。

### Decision 3: アプリ構造は SwiftUI App ライフサイクル + `@State` で `SettingsRoot` を保持

**選択**: `@main struct KsSettingsViewSampleApp: App` をエントリポイントとし、`ContentView` 内で `@State private var root: SettingsRoot = ...` を保持し、`KsSettingsView(root: $root)` をボディとして表示する最小構成にする。

**理由**:
- iOS 16+ なら SwiftUI App ライフサイクルが標準推奨
- `UIApplicationDelegate` ベースの追加コードを不要にし、コードの読みやすさを優先
- `KsSettingsView` の公開 API は `init(root: Binding<SettingsRoot>, style: KsSettingsViewStyle = .classic)` であり、`Binding<SettingsRoot>` を要求する。Sample では `@State` 保持の `root` プロパティを `$root` で渡すことで、最小コードかつ将来的な動的更新（Style 切替・Theme 切替等）にも拡張可能な状態にしておく

**代替案**:
- UIKit `UIApplicationDelegate` + `SceneDelegate` ベース: 現状必要な機能（プッシュ通知 / Background mode 等）はないため不要。
- `Binding.constant(...)` で静的値を渡す: 最初は動かせるが、後続提案で動的更新を入れたい場合に書き換えコストが発生する。`@State` のほうが将来拡張性が高く、本サンプル目的にも適合する。

### Decision 4: Sample 専用 Cell `SampleLabelCell` の独自定義

**選択**: 既存の `PoCLabelCell` / `PoCLabelCellView`（`KsSettingsViewUI` モジュール内 `internal`）は Sample から直接利用できないため、Sample アプリ内に以下を独自定義する：

- `struct SampleLabelCell: KsCell`（`id: UUID` / `style: CellStyle` / `title: String` のみ）
- `final class SampleLabelCellView: UICollectionViewListCell, KsCellRenderer`（`UIListContentConfiguration.cell()` の `text` に `title` をセットして描画）

これらは Sample アプリのモジュール内（`samples/ios/KsSettingsViewSample/...`）に配置し、Sample アプリ起動時（`KsSettingsViewSampleApp.init` または `ContentView.init`）に `KsCellRegistry.shared.register(cellType: SampleLabelCell.self, rendererType: SampleLabelCellView.self)` を呼んで登録する。

**理由**:
- `PoCLabelCell` / `PoCLabelCellView` は `add-settings-view-ios-ui` で `internal` として配置されている（`KsSettingsViewUI` モジュール外からは参照不可）。Sample から `import KsSettingsViewUI` しても `PoCLabelCell` 型名解決はできない
- `KsCell` プロトコルおよび `KsCellRenderer` プロトコル、`KsCellRegistry` は `public` で公開されているため、Sample 側で外部から準拠する Cell 型を新規定義する難易度は低い
- これにより `add-settings-view-ios-ui` の archive 後の改変（`PoCLabelCell` を `public` 化する等）を行わずに Sample が成立する。`add-settings-view-ios-ui` の完了条件「PoCLabelCell を含む SettingsRoot を SwiftUI Sample で表示...」は本提案完了後に「Sample 専用 `SampleLabelCell` を使った同等の検証」として吸収する（文言修正は別タスクで行う）

**代替案**:
- 案 A: `PoCLabelCell` を `public` に昇格させる。
  - 却下理由: `add-settings-view-ios-ui` の design.md でも `PoCLabelCell` は「内部 PoC で、後続具象 Cell 追加時に削除する」位置付け。`public` API として公開すると意図に反するうえ、archive 済み提案の振る舞いに影響する範囲が広がる。
- 案 B: `@testable import KsSettingsViewUI` で参照する。
  - 却下理由: `@testable` は `Test` ターゲット向けであり、リリースビルドの App ターゲットでは機能しない。
- 案 C: Sample アプリを `KsSettingsViewUI` モジュール内に同梱する。
  - 却下理由: モジュール責務（Library / Sample）の境界を壊す。テスト性・保守性が低下する。

### Decision 5: 表示する SettingsRoot の内容

**選択**: 1 セクション・3 行程度の `SampleLabelCell` を含む `SettingsRoot` を `ContentView` 内で構築する。Section の `header` には `SectionAccessory.text("PoC Section")` を、`footer` には `SectionAccessory.text("This is a footer")` を設定する。Root H/F は本提案では設定しない（後続提案の判断に委ねる）。

**理由**:
- 動作確認には複数行があるとレイアウト確認がしやすい
- Section H/F の `text` 形式は目視確認のため最小限設定する。`view` 形式（KsAnyView）は後続提案で扱う
- Root H/F は後続の add-cell-types-* または別途 Sample 拡張提案で扱う

**代替案**:
- 1 行のみの最小構成: 動作確認としては 1 行でも足りるが、複数行のほうがレイアウト確認がしやすい。
- Style 切替 UI を含める: Sample 土台の責務を超えるため、後続提案に委ねる。

### Decision 6: README の構成

**選択**: `samples/ios/README.md` を以下のセクション構成で書き換える：
1. 概要（このサンプルアプリが何を示すか）
2. 必要環境（Xcode 16+ / iOS 16+ シミュレータ）
3. 開き方（`open samples/ios/KsSettingsViewSample.xcodeproj`）
4. 実行（Xcode から ⌘R、または `xcodebuild ... -destination 'platform=iOS Simulator,...'`）
5. ディレクトリ構成（簡易ツリー）
6. 関連リンク（KsSettingsView Core / UI / SwiftUI README、`add-settings-view-ios-ui` 提案へのリンク）

**理由**:
- monorepo-foundation review-result_002.md で「placeholder のまま実 Sample が配置される」リスクが指摘されており、明確に置き換える
- オンボーディング時間の短縮（`open` → ⌘R）

**代替案**:
- README なし: クイックスタート不能で却下。
- 詳細チュートリアル化: 本提案のスコープ外。後続で `docs/` に整備する。

### Decision 7: Bundle Identifier と表示名

**選択**:
- Bundle Identifier: `jp.kamusoft.kssettingsview.samples.ios`
- Display Name: `KsSettingsView Sample`
- Deployment Target: iOS 16.0
- Swift Language Version: 6

**理由**:
- monorepo-foundation のパッケージ ID プレフィックス規約 `jp.kamusoft.kssettingsview.*` に準拠
- iOS 16 は KsSettingsView 本体と一致させ、`UIHostingConfiguration`（iOS 16+）が利用可能であることを保証

**代替案**:
- Deployment Target を iOS 14: KsSettingsView 本体（iOS 16+）と一致しないため、依存解決で失敗する。却下。

### Decision 8: テストターゲットは置かない

**選択**: 本提案では Sample 専用のテストターゲット（XCTest など）は配置しない。

**理由**:
- KsSettingsView 本体のテストは `ios/Tests/` に既に存在する
- Sample の責務は「目視確認可能な最小アプリ」であり、自動テストは Non-Goals

**代替案**:
- UI Test を追加: スナップショットテストや UI Test は後続提案（CI 整備時）で扱うべき。本提案のスコープ外。

## Risks / Trade-offs

- **Risk**: Local Package 参照のパス（`ios/Package.swift` への相対パス）が壊れる可能性
  - **緩和策**: `samples/ios/KsSettingsViewSample.xcodeproj` 内の `Package.swift` 参照を相対パス `../../ios` で固定。README にも明記する
- **Risk**: Xcode プロジェクト（`.xcodeproj`）のバイナリ的な差分が PR レビューで読みにくい
  - **緩和策**: 初回コミット以降は最小限の変更にとどめる。後続提案で大きく構成を変える際は design.md に明記する
- **Risk**: Sample アプリのアイコン / Launch Screen 等の細部が未整備
  - **緩和策**: 標準テンプレート相当の placeholder で足りる。本提案では App Store 配布を想定しないため許容
- **Risk**: 後続の `add-cell-types-*` 提案が「ページ追加」する際に、本 Sample の構造を変更する必要が出る可能性
  - **緩和策**: 本提案では `ContentView` で直接 `KsSettingsView` を表示するシンプルな構造とし、後続提案の design.md でナビゲーション構造（メニュー → 各デモページ）を再設計する余地を残す
- **Trade-off**: SwiftUI App ライフサイクルを採用したことで `AppDelegate` を持たず、将来 UIKit 機能（プッシュ通知等）を追加するときには追加作業が必要
  - **緩和策**: 必要になった時点で `@UIApplicationDelegateAdaptor` で接続できるため、現時点では問題なし
