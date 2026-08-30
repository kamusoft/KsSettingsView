## 参考実装

本変更提案の実装に着手する**前に必ず**以下を確認すること。

- [`docs/legacy-aiforms-reference.md`](../../../docs/legacy-aiforms-reference.md) — 移植元の仕様要約
  - **必読セクション**: §3（CustomCell のプロパティ表）、§11（旧版との差分）、§9（NativeCollectionView パターン）
- 原典コード：
  - [`../AiForms.Maui.SettingsView/SettingsView/Cells/CustomCell.cs`](file://../AiForms.Maui.SettingsView/SettingsView/Cells/CustomCell.cs) — `Content: View`、`IsSelectable`、`IsMeasureOnce`、`UseFullSize`、`LongCommand`/`LongCommandParameter` の 7 つのオプション
  - [`../AiForms.Maui.SettingsView/SettingsView/Handlers/CustomCell/`](file://../AiForms.Maui.SettingsView/SettingsView/Handlers/CustomCell/) — DataTemplate / View → Native View 変換のロジック

**重要**: 旧版 `CustomCell` は MAUI `Microsoft.Maui.Controls.View` を `Content` として受け取り Native に変換する設計。KsSettingsView の **Native Core** ではプラットフォーム非依存のため、Native 側 CustomCell は `Content: ジェネリック型` を取り、利用者が事前登録した「Content → SwiftUI View / UIView / Composable / Android View 生成関数」で描画する設計に置き換える。MAUI 側の `Content: DataTemplate` は `add-maui-cells` で Bridge 境界で実現する（本変更提案では扱わない）。

`IsSelectable` / `IsMeasureOnce` / `UseFullSize` / `LongCommand` 等の旧オプションは Phase 1 では採用しない（必要に応じて後続変更提案で個別追加する）。

**スコープ縮小**: 旧計画では Section H/F の任意 View 描画も本変更提案でまとめて実装する予定だったが、`refactor-accessory-and-root-hf` 提案で装飾領域の Cell 概念排除と `KsAnyView` 型消去ラッパの導入が決定し、Section H/F・Root H/F の描画は `settings-view-ios-ui` / `settings-view-android-ui` 側で本実装される方針に変更された。本変更提案は `CustomCell`（Cell 本体、`Section.cells` に格納される行）のみに集中する。

## Context

CustomCell は SettingsView の最も柔軟な拡張ポイントで、利用者は自前の SwiftUI / UIKit / Compose / Android View をセル化して任意の表示・操作を実現できる。一方、`UIHostingController` や `ComposeView` のライフサイクル管理は誤りやすく、リークやクラッシュにつながりやすい。本変更提案は最低 iOS 16 を前提とするため、SwiftUI 統合には Apple 推奨の `UIHostingConfiguration`（iOS 16+）をデフォルトで採用し、`UIHostingController` 手動親付けに伴うライフサイクル不整合のリスクを排除する。Android 側は最低 API 29 で `ComposeView + DisposeOnDetachedFromWindow` 戦略を採用する。

## Goals / Non-Goals

**Goals:**
- 任意 SwiftUI View / UIView / Composable / Android View をセル化できる API
- ジェネリック Content 型による型安全な登録と描画
- iOS では `UIHostingConfiguration`（iOS 16+）をデフォルトで採用し、SwiftUI 統合のライフサイクル管理を OS に委譲
- iOS / Android 双方でメモリリークしない実装
- iOS / Android Sample に CustomCell の利用例を追加

**Non-Goals:**
- カスタムセル内のジェスチャ・ドラッグ＆ドロップ統合は本変更提案では扱わない
- 動的レイアウト変更時のアニメーション最適化は本変更提案では扱わない
- MAUI 側 CustomCell（DataTemplate）、および `samples/maui/` への CustomCell ページ追加は `add-maui-cells` で対応

## Decisions

### Decision 1: Content 型は Hashable & Identifiable（iOS）/ Any with id（Android）

**選択**: iOS では `CustomCell<Content: Hashable & Identifiable>`、Android では `CustomCell<Content : Any>` で `id: String` を Cell 自身に持たせる。

**理由**:
- iOS は DiffableDataSource が Content の Hashable を要求する
- Android は `data class` の equals が機能すれば DiffUtil で十分

**代替案**:
- Content を `AnyHashable` で抽象化：型安全性が落ちる。

### Decision 2: SwiftUI 統合は UIHostingConfiguration をデフォルト採用

**選択**: SwiftUI ベースの CustomCell は、`UICollectionViewCell.contentConfiguration` に `UIHostingConfiguration { ... }`（iOS 16+）を設定する方式を標準とする。`UIHostingController` を ViewHolder 内に手動で埋め込む実装は採用しない。

**理由**:
- 最低 iOS 16 のため `UIHostingConfiguration` がフル活用できる
- HostingController の `addChild` / `didMove` の親付け管理を OS が自動で行うため、ライフサイクル不整合・リークの実装ミスが構造的に起きない
- セル再利用時は `contentConfiguration` の差し替えだけで済み、内部 SwiftUI ビューツリーの再生成コストを Apple のフレームワークが最適化する
- レイアウト境界の Auto Layout 統合が `UIHostingController` 直埋めより安定

**代替案**:
- `UIHostingController` を ViewHolder に手動埋め込み：iOS 15 互換が必要な場合の旧来手法。本プロジェクトは iOS 16 最低のため不要。
- `_UIHostingView`（プライベート API）：App Store 審査リジェクトリスク。

### Decision 3: UIView ベース CustomCell の併設

**選択**: SwiftUI 統合とは別に、UIView を直接渡す `registerUIViewCustomCell` も提供し、`UIViewCustomCellView` という別 ViewHolder クラスで描画する。

**理由**:
- 既存 UIKit コードベースをそのままセル化したい利用者向け
- Storyboard / xib ベースの UIView を直接埋め込めれば段階移行しやすい
- 実装は `contentView.addSubview(...)` の単純パターンで完結し、`UIHostingConfiguration` ベースとライフサイクル衝突しない

**代替案**:
- UIView を `UIViewRepresentable` でラップして SwiftUI 経由：余計な変換層が増えるだけで利点なし。

### Decision 4: ComposeView 戦略の強制

**選択**: `ComposeCustomCellViewHolder` 抽象クラスを `ks-settingsview-ui` モジュールに置き、`init` 時に `composeView.setViewCompositionStrategy(DisposeOnDetachedFromWindow)` を必ず設定する。

**理由**:
- `add-settings-view-android-ui` で要件化済み
- 漏れを防ぐため抽象クラスで強制

**代替案**:
- ドキュメント注意のみ：実装者依存でリーク発生リスク。

### Decision 5: 登録 API の具体形

**選択**:
- iOS：
  ```swift
  KsCellRegistry.shared.registerSwiftUICustomCell(contentType: ProfileData.self) { profile in
      MyCustomView(profile: profile)
  }
  KsCellRegistry.shared.registerUIViewCustomCell(contentType: ProfileData.self) { profile in
      MyUIViewSubclass(profile: profile) // 戻り値は UIView
  }
  ```
- Android：
  ```kotlin
  KsCellRegistry.registerComposeCustomCell(ProfileData::class) { profile ->
      MyCustomComposable(profile)
  }
  KsCellRegistry.registerViewCustomCell(ProfileData::class) { context, profile ->
      MyAndroidView(context).apply { bind(profile) }
  }
  ```

**理由**:
- Content 型ごとに 1 登録で済む
- SwiftUI / UIView、Compose / View の差は API 名で明示

### Decision 5b: Section H/F・Root H/F の任意 View 描画は本提案のスコープ外（廃止）

旧版では本決定で「`SectionAccessory.custom(AnyCell)` の描画機構を CustomCell の `UIHostingConfiguration` / `ComposeView` 機構に合流させる」としていたが、`refactor-accessory-and-root-hf` 提案で装飾領域から Cell 概念を排除する方針が確定した結果、Section H/F・Root H/F の任意 View 描画は `KsAnyView` 型消去ラッパを介して `settings-view-ios-ui` / `settings-view-android-ui` 側で本実装される。本変更提案は `CustomCell`（Cell 本体、`Section.cells` 行）のみを対象とし、装飾領域の描画には踏み込まない。

### Decision 6: AnyView ベースの便利 init

**選択**: SwiftUI 利用者向けに `CustomCell.swiftUI(id:style:_ content: () -> some View)` のヘルパを提供し、内部で `AnyView` ラップ + 登録不要な「インライン CustomCell」を実現するかは Open Question として残す。本変更提案では Content 型ベースの登録 API のみとする。

**理由**:
- 型安全性とパフォーマンス（AnyView は再描画を阻害）のバランス
- インライン形式は将来の改善余地として残す（必要が確認できれば後続変更提案で追加）

## Risks / Trade-offs

- **リスク**: ジェネリック型登録 API が利用者にとって難解
  - **緩和策**: ドキュメントで複数の使用例を提示。Sample で実例を最低 2 種（プロフィールカード、グラフ表示）作成。
- **リスク**: HostingController の親付け／解除タイミング誤りでメモリリークやクラッシュ
  - **緩和策**: ユニットテストでリーク検出を CI に組み込み（XCTest + WeakReference）。
- **リスク**: Compose の Recomposition が頻繁に発生してパフォーマンス低下
  - **緩和策**: Content 型を `data class` 化し equals 安定性を確保。`remember(content) { ... }` パターンを Sample で示す。
- **トレードオフ**: 最低 iOS 16 とすることで iOS 15 デバイスを切り捨てる
  - **緩和策**: 旧 `AiForms.Maui.SettingsView` は iOS 13 以降で別途存続。新規ユーザーは現代的なシェア（2026 年時点で iOS 16 以上が大多数）を前提に KsSettingsView を採用する。`docs/development.md` および README で対応 OS を明示。

## Open Questions

- インライン CustomCell（登録不要、Section 内で直接 SwiftUI View / Composable を書ける API）を提供するか？ → 本変更提案ではスコープ外。利用実績を見て、必要があれば後続変更提案で追加する。
