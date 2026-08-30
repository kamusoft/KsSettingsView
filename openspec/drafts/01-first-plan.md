# KsSettingsView リニューアル 具体化計画

## Context

`AiForms.Maui.SettingsView` は MAUI 専用の設定画面ライブラリで、iOS/Android の OS 設定アプリ風 UI を簡単に構築できる。だが UI が古びてきており、また MAUI 専用なので Native / KMP 利用者には届かない。本計画はこれを **KsSettingsView** としてリニューアルし、Native (iOS/Android) を一次ソースとした上で MAUI / KMP がその薄いラッパとして共存できるマルチプラットフォーム基盤を作る。フェーズ1ではクラシック UI（現行の見た目）を踏襲しつつ、内部実装を `UICollectionView + DiffableDataSource` / `RecyclerView + ListAdapter + DiffUtil` ベースに刷新し、`AiForms.Maui.NativeCollectionView` で得られた高パフォーマンス知見を吸収する。モダン UI とカスタムセルの SwiftUI/Compose 直差し込みは後期フェーズで追加する。

## 確定要件（ユーザー回答済み）

| 項目 | 決定 |
|------|------|
| UI フェーズ | クラシック先行、モダンは Phase 6 |
| Android UI 実装 | RecyclerView + ListAdapter + DiffUtil |
| iOS UI 実装 | UICollectionView + UICollectionViewDiffableDataSource |
| Phase 1 の公開 API | モデル + View + SwiftUI/Compose **DSL も含める** |
| KMP 対応 | 薄いラッパー（cinterop / aar 依存）のみ初期提供 |
| MAUI バインディング | CommunityToolkit Native Library Interop |
| Native 配信 | iOS: SwiftPM (xcframework) / Android: Maven Central (aar) |
| 最低 OS | iOS 15.0 / Android API 29 (Android 10) |
| リポジトリ構成 | モノレポ |
| Sample | iOS Native / Android Native / MAUI の 3 つ |
| Cell 範囲 | AiForms フルセット（15 種）を初期で移植 |
| テスト | Native 単体（XCTest / JUnit）+ MAUI Snapshot |
| 永続化層 | スコープ外（別ライブラリ） |
| 旧版互換性 | 互換 shim なし、breaking change として独立ブランド |
| OpenSpec 運用 | すぐ `changes/` に提案を作る |

## 推奨アーキテクチャ

```
KsSettingsView/
├── ios/                                  # SwiftPM (xcframework 配信)
│   ├── KsSettingsViewCore/               # モデル層 (struct + protocol)
│   ├── KsSettingsViewUI/                 # UICollectionView + DiffableDataSource
│   └── KsSettingsViewSwiftUI/            # UIViewControllerRepresentable + DSL
├── android/                              # Gradle (aar / Maven Central)
│   ├── ks-settingsview-core/             # sealed class モデル
│   ├── ks-settingsview-ui/               # RecyclerView + ListAdapter
│   └── ks-settingsview-compose/          # AndroidView ラッパー + DSL
├── maui/                                 # NuGet
│   ├── KsSettingsView.Bindings.iOS/      # XcodeProject (xcframework バインド)
│   ├── KsSettingsView.Bindings.Android/  # AndroidGradleProject (aar バインド)
│   └── KsSettingsView.Maui/              # Handler + BindableObject 層
├── kmp/
│   └── ks-settingsview-kmp/              # expect/actual + cinterop + aar
├── samples/
│   ├── ios/         (SwiftUI App)
│   ├── android/     (Compose App)
│   └── maui/        (XAML + MVVM)
├── openspec/
└── docs/
```

レイヤ責務：
- **Core (Native)**: 純粋データモデル（Swift `struct` / Kotlin `data class`）。プラットフォーム UI 型に非依存。`Theme` / `CellStyle` / `SettingsRoot` / `Section` / `Cell` 群。
- **UI (Native)**: `UICollectionView` / `RecyclerView` を内包する `KsSettingsViewController` / `KsSettingsView`（FrameLayout）。再利用ロジック・差分更新を担う。
- **DSL (Native)**: SwiftUI 用 `UIViewControllerRepresentable` ラッパと、Compose 用 `@Composable fun KsSettingsView(...)`。Phase 1 で同梱。
- **MAUI Bindings**: Swift/Kotlin 側に `KsSettingsViewBridge`（ObjC 公開 / `@JvmStatic` ファサード）を置き、C# は Bridge 経由でモデルと UIViewController/View を生成。**Cell ごとの Handler は現行 AiForms 同様に維持**（後述の「Cell バインディング戦略」参照）。
- **KMP**: `expect/actual` で iOS/Android の Native ファサードを統合した薄い API のみ。

## iOS Native 公開 API（Phase 1）

```swift
// Core
public struct SettingsRoot: Hashable { public var sections: [Section]; public var theme: Theme }
public struct Section: Hashable, Identifiable { public var id: UUID; public var header: String?; public var footer: String?; public var cells: [AnyCell] }
public protocol KsCell: Hashable { var id: UUID { get }; var style: CellStyle { get } }
public struct AnyCell: Hashable { ... } // 型消去

// 15 種の Cell（struct）: LabelCell, CommandCell, ButtonCell, SwitchCell, CheckboxCell,
// RadioCell, SimpleCheckCell, EntryCell, PickerCell, TextPickerCell, NumberPickerCell,
// TimePickerCell, DatePickerCell, CustomCell, （CellBase は protocol 化）

// UIKit
public final class KsSettingsViewController: UIViewController {
    public var root: SettingsRoot { didSet { applySnapshot() } }
}

// SwiftUI ラッパ
public struct KsSettingsView: UIViewControllerRepresentable {
    @Binding var root: SettingsRoot
}

// SwiftUI DSL（Phase 1 で同梱）
public struct KsSettingsViewBuilder {
    @resultBuilder public struct SectionBuilder { ... }
}
// 利用例:
// KsSettingsView(root: $root) {
//     Section("一般") { LabelCell(...); SwitchCell(...) }
// }
```

最低 iOS 15。レイアウトは `UICollectionLayoutListConfiguration`（iOS 14+）+ `estimatedHeight` で自動高さ計算コストを抑制。

ユーザー操作通知（双方向バインド用）として、Bridge 層は以下の delegate プロトコルを `@objc public` で公開：

```swift
@objc public protocol KsCellInteractionDelegate {
    func cell(id: UUID, didChangeBoolValue value: Bool)   // Switch / Checkbox
    func cell(id: UUID, didChangeTextValue value: String) // Entry
    func cell(id: UUID, didChangeIntValue value: Int)     // NumberPicker
    func cell(id: UUID, didChangeDateValue value: Date)   // Date/TimePicker
    func cell(id: UUID, didSelectIndex index: Int)        // Picker / Radio
    func cellDidTap(id: UUID)                             // Command / Button
}
```

C# Cell Handler はこの delegate を実装し、`cell.SetValue(SwitchCell.OnProperty, value)` のように `BindingMode.TwoWay` を成立させる。

## Android Native 公開 API（Phase 1）

```kotlin
// Core
sealed class Cell { abstract val id: String; abstract val style: CellStyle }
data class LabelCell(...) : Cell()
// ... 15 種

data class Section(val header: String?, val footer: String?, val cells: List<Cell>)
data class SettingsRoot(val sections: List<Section>, val theme: Theme)

// View
class KsSettingsView(ctx: Context, attrs: AttributeSet?) : FrameLayout(ctx, attrs) {
    var root: SettingsRoot = SettingsRoot()  // setter で adapter.submitList
}

// Compose ラッパ + DSL（Phase 1 で同梱）
@Composable fun KsSettingsView(root: SettingsRoot, onChange: (SettingsRoot)->Unit)

// DSL 例:
// KsSettingsView(theme = ...) {
//     section("一般") { switchCell(...); labelCell(...) }
// }
```

最低 API 29 (Android 10)。`ListAdapter` + `DiffUtil.ItemCallback`、`ConcatAdapter` でヘッダ/フッタ拡張、`ComposeView` は `setViewCompositionStrategy(DisposeOnDetachedFromWindow)` 強制。

ユーザー操作通知（双方向バインド用）として、Bridge 層は以下の Java-friendly listener interface を公開：

```kotlin
interface KsCellInteractionListener {
    fun onBoolValueChanged(id: String, value: Boolean)
    fun onTextValueChanged(id: String, value: String)
    fun onIntValueChanged(id: String, value: Int)
    fun onDateValueChanged(id: String, epochMillis: Long)  // C#からは DateTime に変換
    fun onIndexSelected(id: String, index: Int)
    fun onCellTapped(id: String)
}
```

## MAUI バインディング戦略

CommunityToolkit の **Native Library Interop** パターン。詳細は [Microsoft Learn: Native Library Interop](https://learn.microsoft.com/en-us/dotnet/communitytoolkit/maui/native-library-interop/get-started) と [Maui.NativeLibraryInterop リポジトリ](https://github.com/CommunityToolkit/Maui.NativeLibraryInterop) のサンプルを踏襲。

1. **iOS**: Swift 側に `@objc public class KsSettingsViewBridge` を実装し、`SettingsRoot` を C# 構築可能な ObjC 互換型（`NSDictionary`/`NSArray` ベースの DTO）から組み立てて `UIViewController` を返す。`xcframework` 化して `KsSettingsView.Bindings.iOS.csproj`（XcodeProject 形式）から `objective-sharpie` で `ApiDefinitions.cs` を生成。
2. **Android**: Kotlin の `data class` は C# から扱いづらいため、Kotlin で `KsSettingsViewBridge.kt` の Java-friendly ファサード（`@JvmStatic` ファクトリ、`@JvmField` 公開、Builder パターン）を別途用意。`aar` を `KsSettingsView.Bindings.Android.csproj`（AndroidGradleProject 形式）から取り込む。
3. **MAUI Handler 階層**: 現行 AiForms と同じ階層を踏襲。
   - `SettingsViewHandler` — ルート View の Handler（`KsSettingsViewController` / `KsSettingsView` を生成し、Bridge を保持）
   - `CellBaseHandler<TVirtualCell, TNativeCell>` — Cell 共通 Handler。`BasePropertyMapper` で Title/Description/Icon/HintText/IsEnabled/BackgroundColor 等を Native セルへ反映
   - 各 Cell の Handler（`SwitchCellHandler`, `EntryCellHandler` ...） — `PropertyMapper` に Cell 固有プロパティ（`On`, `ValueText` 等）を登録し、Native セルプロパティへ橋渡し
   - 双方向バインド：Native セルからのユーザー操作（Switch トグル、Entry 入力等）は Bridge の delegate/listener で C# Cell に書き戻す（`SetValue` で `BindingMode.TwoWay` を満たす）
4. **Cell バインディング戦略**:
   - C# 側は **`BindableObject` を継承する Cell（POCO ではなく現行と同じ BindableProperty）** を維持。XAML での `On="{Binding Foo}"` の `BindingMode.TwoWay` を従来どおり保証する
   - Cell の各 BindableProperty は対応する Cell Handler の `PropertyMapper` 経由で Native セルへ反映
   - Native 側ユーザー操作 → C# Cell プロパティへの戻りは、Native Bridge が公開する delegate（iOS: `@objc protocol KsSwitchCellDelegate`、Android: `interface KsSwitchCellListener`）を Cell Handler が実装し、`SwitchCell.On = newValue` のように書き戻す
   - 単方向更新 (`OneWay`) の Cell（Label/Command 等）は delegate なし
5. **メモリ管理**: `DisconnectHandler` で Native 参照を必ず解放。テスト基盤に `WeakReference` リーク検出を組み込み、AiForms 時代の `HandlerCleanUpHelper` と同等の保険を CI で機械的にチェック。

## Cell 階層設計

| 観点 | iOS | Android |
|------|-----|---------|
| モデル | `protocol KsCell: Hashable` + `struct LabelCell: KsCell` | `sealed class Cell` + `data class LabelCell(...) : Cell()` |
| ViewHolder | `protocol CellRenderer { func render(_ cell: AnyCell) }` を `UICollectionViewCell` サブクラスが実装 | `abstract class CellViewHolder<T : Cell>(view: View) { abstract fun bind(cell: T) }` |
| 差分 | DiffableDataSource が自動 (Hashable) | DiffUtil.ItemCallback が equals 比較 (data class 自動) |
| カスタム化 | `CustomCell(content: AnyView)`（SwiftUI を `UIHostingController` で埋め込み、iOS 16+ 機能は条件分岐） | `CustomCell(content: @Composable () -> Unit)`（`ComposeView` で埋め込み） |

スタイル二重定義回避：`Theme`/`CellStyle` を Native Core にだけ定義し、MAUI 側は `Color` / `FontFamily` を Bridge 境界で Native 表現に変換するヘルパを 1 箇所に集約する。

## 開発フェーズ計画

| Phase | 内容 | 並列可否 |
|------|------|---------|
| 1. Native 基盤 | iOS/Android の Core モデル、UI 層、SwiftUI/Compose DSL、Theme、3 種 Cell（Label/Switch/Command）で PoC、CI（XCTest/JUnit）、Sample アプリ起動可能まで | iOS と Android は並列 |
| 2. Cell 15 種移植 | 残り 12 種を Phase 1 のパターンに沿って実装。Picker 系は最後に。スナップショット差分を AiForms 版と比較し回帰検出 | iOS/Android 並列 |
| 3. 配信整備 | xcframework / aar ビルド、SwiftPM `Package.swift`、Maven Central 公開（Sonatype OSSRH 申請含む）、SemVer・CHANGELOG 運用、署名 | Phase 2 と一部並列可 |
| 4. MAUI バインディング | XcodeProject/AndroidGradleProject 設定、Bridge 実装、`SettingsView` Handler、MAUI Sample、Snapshot テスト、移行ガイド | Phase 3 完了後 |
| 5. KMP ラッパー | cinterop（iOS）+ aar 依存（Android）で `expect/actual` の薄いファサード、KMP Sample 1 本 | Phase 4 と並列可 |
| 6. モダン UI + カスタムセル拡張 | モダンテーマセット、SwiftUI Cell / Composable Cell の本格対応、UIHostingConfiguration（iOS 16+）活用、ドキュメント整備 | 独立 |

依存：Phase 1 → 2 → (3 ‖ 4) → 5。Phase 6 は独立で着手可。

## OpenSpec 変更提案の初期セット

`openspec/config.yaml` の規約（日本語本文、英語キーワード、SHALL/MUST、`### Requirement:` / `#### Scenario:` 形式）に従う。Phase 1 着手前に以下の changes を順に作成：

1. `add-settings-view-core` — capability `settings-view-core`（Native 共通モデル / Theme / SettingsRoot / Section）
2. `add-settings-view-ios-ui` — capability `settings-view-ios-ui`（UICollectionView + DiffableDataSource、KsSettingsViewController、SwiftUI ラッパ）
3. `add-settings-view-android-ui` — capability `settings-view-android-ui`（RecyclerView + ListAdapter、KsSettingsView FrameLayout、Compose ラッパ）
4. `add-cell-types-basic` — capability `cell-types-basic`（Label/Command/Button/Switch/Checkbox/SimpleCheck/Radio）
5. `add-cell-types-input` — capability `cell-types-input`（Entry/Picker/TextPicker/NumberPicker/Time/Date）
6. `add-cell-types-custom` — capability `cell-types-custom`（CustomCell + SwiftUI/Compose 埋め込み）
7. `add-native-binding-maui` — capability `native-binding-maui`（Native Library Interop ブリッジ + `KsSettingsViewBridge` の公開 API）
8. `add-maui-settings-view` — capability `maui-settings-view`（`SettingsView` BindableObject + ルート Handler）
9. `add-maui-cell-handlers` — capability `maui-cell-handlers`（15 種 Cell の `BindableProperty` 仕様 + 各 Cell Handler の PropertyMapper / 双方向バインド delegate）
10. `add-kmp-wrapper` — capability `kmp-wrapper`
11. `add-samples` — capability `samples`（iOS/Android/MAUI 3 つ）

各提案には `proposal.md`・`tasks.md`・必要に応じ `design.md` と `specs/<capability>/spec.md`（ADDED Requirements）を含める。

## 重要なリスクと緩和策

1. **MAUI 9 Handler メモリリーク** — `DisconnectHandler` 必須化。`WeakReference` リーク検出テストを CI に組み込み。既存 [`AiForms.Maui.NativeCollectionView/Platforms/iOS/`](../AiForms.Maui.NativeCollectionView/) の Dispose パターンを参考にする。
2. **iOS UICollectionView 自動高さ計測コスト** — `UICollectionLayoutListConfiguration` + `estimatedHeight`、Picker 系のみ固定高さフォールバックを許可。
3. **Compose ↔ View 相互運用** — `ComposeView.setViewCompositionStrategy(DisposeOnDetachedFromWindow)` を CustomCell 内で強制。lifecycleOwner 不一致を防ぐ ViewHolder ベースクラスを用意。
4. **Maven Central 配信ハードル** — Sonatype OSSRH 申請・GPG 署名で 2〜3 週間の初期コスト。Phase 3 を独立タスクとして先行着手し、社内利用は GitHub Packages で繋ぐ。
5. **同一 API サーフェス維持** — Swift/Kotlin/C# のネーミング差分が運用で乖離しがち。最低限「契約テスト」（公開 API 名・必須プロパティ一覧の YAML を 3 言語で生成し diff チェック）を Phase 3 で導入。
6. **iOS 15 最低保証下での SwiftUI 統合** — `UIHostingConfiguration` は iOS 16+。Phase 1 では `UIHostingController` を ViewHolder に手動で埋める実装にし、Phase 6 で iOS 16+ 機能のオプトイン強化。

## 参照する既存コード

> **重要**: 本計画と各変更提案で参照すべき仕様要約は [`docs/legacy-aiforms-reference.md`](../../docs/legacy-aiforms-reference.md) に集約済み。各変更提案の `design.md` 冒頭の「## 参考実装」セクションから本ドキュメントを参照する運用とし、Cell の BindingMode・デフォルト値・全体プロパティを変更提案間で**一貫して**踏襲する。

- [../AiForms.Maui.NativeCollectionView/AiForms.Maui.NativeCollectionView/Platforms/iOS/](AiForms.Maui.NativeCollectionView の iOS 実装) — UICollectionViewDiffableDataSource、Cell 再利用、PrepareForReuse、CancellationTokenSource パターン
- [../AiForms.Maui.NativeCollectionView/AiForms.Maui.NativeCollectionView/Platforms/Android/](AiForms.Maui.NativeCollectionView の Android 実装) — ListAdapter + DiffUtil + ConcatAdapter
- [../AiForms.Maui.SettingsView/SettingsView/Cells/](Cells/) 全 15 種 — Cell 振る舞い・Bindable プロパティ仕様の参考
- [../AiForms.Maui.SettingsView/SettingsView/Handlers/](Handlers/) — 各 Cell Handler の振る舞い（移植時の比較リファレンス）
- [../AiForms.Maui.SettingsView/SettingsView/SettingsView.DefineProperites.cs](SettingsView.DefineProperites.cs) — 40+ Bindable プロパティ仕様
- [openspec/config.yaml](openspec/config.yaml) — OpenSpec 規約

## 検証手順（end-to-end）

1. **Native PoC（Phase 1 完了時）**
   - `cd ios && swift build && swift test` で Core/UI のユニットテストが通る
   - `cd android && ./gradlew test` で JUnit が通る
   - `samples/ios/` を Xcode で開き iPhone 15 シミュレータで起動、Sample 設定画面が AiForms 版と同等に動く
   - `samples/android/` を Android Studio で起動、Pixel 8 エミュレータで動作確認
2. **配信検証（Phase 3）**
   - `swift package generate-xcframework` で xcframework 生成 → サンプル外プロジェクトに `.package(url: "...", from: "0.1.0")` で取り込み起動
   - Maven Central へのスナップショット公開を社内テスト Gradle プロジェクトから取得して起動
3. **MAUI 検証（Phase 4）**
   - `samples/maui/` を `dotnet build -t:Run -f net9.0-ios` および `-f net9.0-android` で実機/シミュレータ起動
   - Snapshot テストで AiForms 版とのピクセル差分が許容内
4. **メモリリーク検証**
   - iOS: Instruments の Allocations で SettingsView ページを 10 回 push/pop し、`KsSettingsViewController` が解放されることを確認
   - Android: LeakCanary をデバッグ時に有効化、同様に 10 回開閉
5. **OpenSpec 整合**
   - 各 change の `tasks.md` チェックボックスがすべて埋まっており、`openspec/specs/` に対応する capability spec が存在し、`### Requirement:` 配下のシナリオが実装で観察可能なこと

## 次のアクション（このプランの実行に入った後）

1. `openspec/changes/add-settings-view-core/` を作成し、proposal.md / design.md / specs/settings-view-core/spec.md / tasks.md を起こす（OpenSpec 規約準拠）
2. モノレポルートの基本ディレクトリ（`ios/`, `android/`, `maui/`, `kmp/`, `samples/`, `docs/`）と `README.md` 雛形を整備
3. iOS / Android の最小 PoC（Core モデル + 1 Cell の表示）を並行で着手

## 作成済み変更提案と実装順（2026-05-06 時点）

本計画は当初 11 個の変更提案として粒度設計したが、レビューと段階的実装容易性の観点から **8 個に再編成**して `openspec/changes/` に作成済み。KMP・配信整備（Phase 3）・モダン UI（Phase 6）は本フェーズの変更提案からは除外し、Phase 4 完了後に別途追加提案を起こす方針。

### 実装順（依存と並列可否）

| # | 変更提案 ID | capability | 依存（先行） | タスク数 |
|---|---|---|---|---|
| 1 | `add-monorepo-foundation` | `monorepo-foundation` | なし | 19 |
| 2 | `add-settings-view-core` | `settings-view-core` | #1 | 34 |
| 3 | `add-settings-view-ios-ui` | `settings-view-ios-ui` | #1, #2 | 30 |
| 4 | `add-settings-view-android-ui` | `settings-view-android-ui` | #1, #2 | 41 |
| 5 | `add-cell-types-basic` | `cell-types-basic` | #1, #2, #3, #4 | 55 |
| 6 | `add-cell-types-input` | `cell-types-input` | #1, #2, #3, #4 | 48 |
| 7 | `add-cell-types-custom` | `cell-types-custom` | #1, #2, #3, #4 | 29 |
| 8 | `add-maui-bindings` | `maui-bindings` | #1〜#7 すべて | 78 |

### 推奨実行ステージ

```
Stage A:  #1 (monorepo-foundation)
              ↓
Stage B:  #2 (settings-view-core)
              ↓
Stage C:  #3 ‖ #4               ← iOS と Android を並列開発
       (ios-ui)  (android-ui)
              ↓
Stage D:  #5 ‖ #6 ‖ #7          ← Cell 群を並列開発（Core 共通型追加は順次）
      (basic) (input) (custom)
              ↓
Stage E:  #8 (maui-bindings)    ← 最大規模、Native が完成してから着手
```

### 各変更提案のスコープ

1. **`add-monorepo-foundation`** — `ios/` `android/` `maui/` `samples/` `docs/` のディレクトリ構成、各プラットフォームのビルド入口（`Package.swift`、`settings.gradle.kts`、`KsSettingsView.slnx`）、命名規約（パッケージ ID プレフィックス `jp.kamusoft.kssettingsview.*`、Maven Central groupId `jp.kamusoft`）、最低ツールチェイン（Xcode 16+/iOS 16/AGP 8.7+/JDK 17/minSdk 29/.NET 9）
2. **`add-settings-view-core`** — Native 共通モデル（`SettingsRoot` / `Section` / `KsCell` 抽象 / `Theme` / `CellStyle` / `KsColor` / `KsFont`）を iOS Swift と Android Kotlin で論理同型に定義。プラットフォーム UI 型非依存。XCTest / JUnit 整備
3. **`add-settings-view-ios-ui`** — `KsSettingsViewController` + `UICollectionView` + `UICollectionViewDiffableDataSource` + `UICollectionLayoutListConfiguration`、`KsCellRegistry`、`KsCellRenderer` 抽象、SwiftUI ラッパ + `@resultBuilder` DSL、PoC `PoCLabelCell`
4. **`add-settings-view-android-ui`** — `KsSettingsView` (FrameLayout) + `RecyclerView` + `ListAdapter` + `DiffUtil`、`CellViewHolder<T>` 抽象、`KsCellRegistry`、`ComposeCellViewHolder` 基盤、Compose ラッパ + DSL、PoC `PocLabelCell`
5. **`add-cell-types-basic`** — 7 種の基本 Cell（Label / Command / Button / Switch / Checkbox / Radio / SimpleCheck）+ `KsImage` 値型、`registerBasicCells()` API。完了時に PoC Cell を REMOVED
6. **`add-cell-types-input`** — 6 種の入力系 Cell（Entry / Picker / TextPicker / NumberPicker / TimePicker / DatePicker）+ `KsKeyboardType` / `KsTime` / `KsDate` 補助型、モーダル UX（iOS は UITableViewController/UIDatePicker、Android は AlertDialog/MaterialPicker）
7. **`add-cell-types-custom`** — `CustomCell<Content>` ジェネリック型、iOS は `UIHostingConfiguration`（iOS 16+）デフォルト採用、Android は `ComposeView` + `DisposeOnDetachedFromWindow`、SwiftUI/UIView/Compose/Android View の 4 系統登録 API
8. **`add-maui-bindings`** — Native Bridge（Swift/Kotlin の `KsSettingsViewBridge` + `KsCellInteractionDelegate`/`Listener`）、MAUI バインディングプロジェクト（XcodeProject / AndroidGradleProject 形式）、`KsSettingsView.Maui` コアライブラリ（`SettingsView` + 14 種 Cell の `BindableObject` + Handler 階層）、`AddKsSettingsView()` 拡張、リーク検出、MAUI Sample、ゴールデンイメージ方式 Snapshot テスト、移行ガイド

### 本フェーズで除外したもの（次フェーズで追加提案）

- **KMP ラッパー**：cinterop（iOS）+ aar 依存（Android）の薄いファサード
- **配信整備**：xcframework 自動ビルド、SwiftPM 公開、Maven Central（DNS TXT 検証）、NuGet パッケージング、署名運用
- **モダン UI / カスタムセル拡張**：モダンテーマセット、SwiftUI / Compose インライン Cell DSL、ドラッグ＆ドロップ並べ替え

### 実装着手コマンド例

```bash
# Stage A
openspec apply add-monorepo-foundation
# Stage B
openspec apply add-settings-view-core
# Stage C（並列可、別ブランチで作業）
openspec apply add-settings-view-ios-ui
openspec apply add-settings-view-android-ui
# Stage D（並列可。ただし Core への型追加は順次マージ）
openspec apply add-cell-types-basic
openspec apply add-cell-types-input
openspec apply add-cell-types-custom
# Stage E
openspec apply add-maui-bindings
```
