## Why

`AiForms.Maui.NativeCollectionView` の `HeaderView` / `FooterView` 相当の「リスト全体のヘッダ／フッタ」機能を `KsSettingsView` にも提供したい。MAUI 利用者が `<settings:KsSettingsView.HeaderView>...</>` のように XAML から任意 `View` を差し込めることが要件である。あわせて、現行の `SectionAccessory.custom(AnyCell)` / `.Custom(Cell)` は「Cell 概念」を Header/Footer 装飾領域に流用しており、概念上の混入になっている。Header/Footer は本来 Cell（タップ・選択・編集する行）ではないため、この機会に Cell 概念を Accessory から排除して整理する。

## What Changes

- **BREAKING**: `settings-view-core` の `SectionAccessory` から `custom(AnyCell)` / `Custom(Cell)` ケースを削除し、`view(KsAnyView)` / `View(KsAnyView)` ケースに置き換える。装飾領域は Cell ではなく「任意 View」を保持する型として再定義する。
- **NEW**: `settings-view-core` に `KsAnyView` 型消去ラッパを追加する。iOS では `SwiftUI View` / `UIView` の二択 backing、Android では `@Composable` / `Android View` の二択 backing を保持する。`Hashable` / `equals` 契約は持たない（差分検出のキーには参加しない）。
- **NEW**: `settings-view-core` の `SettingsRoot` に `header: RootAccessory?` / `footer: RootAccessory?` を追加する。`RootAccessory` は `text(String)` / `view(KsAnyView)` の sum type。`SectionAccessory` と shape は同じだが、将来の挙動分岐（ピン留め、テーマ継承ルール等）に備え別型として導入する。
- **NEW**: `settings-view-ios-ui` の `KsSettingsViewController` に Root H/F の描画を追加する。`UICollectionViewCompositionalLayout` の `boundarySupplementaryItem`（global、`elementKind: "ks-root-header" / "ks-root-footer"`）を採用し、`pinToVisibleBounds = false`（一緒にスクロールする AiForms 互換挙動）をデフォルトとする。Section H/F の `.view(KsAnyView)` ケースも本提案で本実装する（`UIHostingConfiguration` ベース）。
- **NEW**: `settings-view-android-ui` の `KsSettingsView` に Root H/F の描画を追加する。`ConcatAdapter(headerAdapter, mainAdapter, footerAdapter)` の構成で、`headerAdapter` / `footerAdapter` は `ItemCount = 0/1` 切り替え式の独立 adapter として実装する。Section H/F の `.View(KsAnyView)` ケースも本提案で本実装する（`ComposeView` ベース）。
- **NEW**: `maui-bindings` の `KsSettingsView` に `HeaderView` / `FooterView` の `BindableProperty`（`View?`）を追加する。MAUI `View` → ネイティブ `KsAnyView` への変換は `MauiView.ToPlatform()` を経由する UIKit / Android View backing として実装する。
- **MODIFIED**: `cell-types-custom` から「Section ヘッダ／フッタの任意 View 描画」スコープを削除し、`CustomCell`（Cell 本体）のみに集中させる。proposal / design / tasks / 関連 delta spec から H/F 関連記述を除去する。
- **既存提案の更新**: `add-settings-view-ios-ui` / `add-settings-view-android-ui` の delta spec / design / tasks における `SectionAccessory.custom(AnyCell)` 参照を `.view(KsAnyView)` に書き換える。Phase 1 のプレースホルダ規定はそのまま `.view` ケースに適用する形で維持する。

## Capabilities

### New Capabilities
（なし）

### Modified Capabilities
- `settings-view-core`: `SectionAccessory` の `.custom(AnyCell)` ケースを `.view(KsAnyView)` に変更（破壊的）。`KsAnyView` 型を新設。`SettingsRoot` に `header` / `footer`（`RootAccessory?`）と `RootAccessory` 型を追加。`AnyCell` 要件を削除。

> 注: `settings-view-ios-ui` / `settings-view-android-ui` / `maui-bindings` / `cell-types-custom` は現在 in-progress の変更提案であり `openspec/specs/` に未登録のため、本提案では delta spec を作らず、当該提案ファイル群（proposal.md / design.md / tasks.md / 各 delta spec）の直接書き換えで反映する。詳細は Impact / Migration Plan を参照。

## Impact

- 影響範囲：
  - iOS: `KsSettingsViewCore`（型追加・変更）、`KsSettingsViewUI`（Root H/F 描画追加、Section H/F 本実装化）
  - Android: `ks-settingsview-core`（型追加・変更）、`ks-settingsview-ui`（Root H/F 描画追加、Section H/F 本実装化）
  - MAUI: `KsSettingsView` バインディング Handler（HeaderView/FooterView Mapper 追加）
  - 既存変更提案: `add-settings-view-ios-ui` / `add-settings-view-android-ui` / `add-cell-types-custom` の各アーティファクト修正
- 依存関係：
  - 前提: `settings-view-core`（archive 済）、`settings-view-ios-ui` / `settings-view-android-ui`（in-progress）、`maui-bindings`（in-progress）
  - 並列: `add-cell-types-basic`、`add-cell-types-input`（影響なし）
  - 影響: `add-cell-types-custom`（H/F スコープ削除、Cell 本体のみに集中）
- 破壊的変更のリスク:
  - `SectionAccessory.custom(AnyCell)` を直接参照しているコードは `.view(KsAnyView)` への書き換えが必要。ただし本機能は archive 済 core spec の段階でも実装途中であり、現実装コードへの影響は限定的と見込む。
  - 既存の in-progress 変更提案（ios-ui / android-ui / cell-types-custom）は本提案により仕様変更されるため、マージ順序の調整が必要。

## Risks

- **破壊的変更（Cell 概念排除）の波及**: `add-cell-types-custom` が `SectionAccessory.custom(AnyCell)` 経由で H/F 描画を計画していたため、本提案により当該提案の scope を縮める必要がある。`add-cell-types-custom` の作業を一時停止し、本提案を先にマージする運用が必要。
- **`KsAnyView` の差分検出非対応**: `KsAnyView` は `Hashable` / `equals` を持たないため、SettingsRoot の差分検出には参加しない。中身の更新は描画レイヤ（`UIHostingConfiguration` の再構成 / `ComposeView.setContent` の再呼び出し）に依存する。これは AiForms.Maui.NativeCollectionView の `HeaderView` / `FooterView` と同じ運用方針であり、リスクは低いが ドキュメントで明示する必要がある。
- **MAUI `View` → ネイティブ変換**: `MauiView.ToPlatform()` の挙動（HotReload、BindingContext 伝播）を Handler 内で正しく扱う必要がある。Handler 実装時に検証ポイントを設ける。
