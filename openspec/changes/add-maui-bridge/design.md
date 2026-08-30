## 参考実装

本変更提案は元 `add-maui-bindings` から「Native Bridge + Binding csproj」部分を切り出したものであり、設計判断の多くは元提案の Decision 群を継承する。実装着手前に以下を熟読すること。

- [`openspec/changes/add-maui-cells/design.md`](../add-maui-cells/design.md)（元 `add-maui-bindings` の design.md。Decision 1〜8 の経緯）
- [`docs/legacy-aiforms-reference.md`](../../../docs/legacy-aiforms-reference.md) — 移植元の仕様要約
- CommunityToolkit [Maui.NativeLibraryInterop](https://github.com/CommunityToolkit/Maui.NativeLibraryInterop) のドキュメント・サンプル
- `.claude/skills/maui-native-binding-skill/` — XcodeProject / AndroidGradleProject 形式の取り込み手順

## Context

Native iOS / Android の SettingsView 基盤と Cell 群（`KsSettingsViewCore` / `KsSettingsViewUI` / `ks-settingsview-core` / `ks-settingsview-ui`）は既に archive 済の変更提案で実装されている。本提案ではこれらを .NET MAUI から呼び出すための薄い Bridge レイヤと、それを取り込む MAUI バインディングプロジェクトを整備する。

元 `add-maui-bindings` 1 提案で「Bridge + Binding + MAUI 本体 + 全 Cell Handler + Sample + Snapshot」までを抱える設計だったが、(1) レビュー粒度が大きすぎる、(2) Bridge API 確定前に MAUI 本体タスクを並走させると手戻りが発生する、(3) Decision 7 で言及されていた Phase A/B/C 分割を変更提案単位で実体化したい、という観点から、本提案では Phase A 相当の「Bridge + Binding」のみを担当する。

`KsSettingsView.Maui`（MAUI 本体）と全 Cell Handler 群は本提案 archive 後にそれぞれ `add-maui-core` / `add-maui-cells` が担当する。Phase A 完了時点では C# テストコードから直接 Bridge を呼んで動作確認できる状態までを保証する。

## Goals / Non-Goals

**Goals:**

- Swift / Kotlin の Native Bridge ライブラリ（`xcframework` / `aar`）の新設
- MAUI バインディングプロジェクト 2 つ（`KsSettingsView.Bindings.iOS.csproj` / `KsSettingsView.Bindings.Android.csproj`）の新設
- Builder + `setRoot` 方式の Bridge 公開 API 設計（元 `add-maui-bindings` Decision 2 を継承）
- `KsCellInteractionDelegate` / `KsCellInteractionListener` による単一 delegate / listener API（元 Decision 3 を継承）
- Root H/F 用 API（`KsAnyView` 経由、元 Decision 6d を継承）
- 高頻度更新パス用 `updateCellValue` API（元 Decision 6b の例外パス）
- Bridge ユニットテストで Builder / `setRoot` / DiffUtil 差分更新を検証
- `LabelCell` 1 種類の `addLabelCell(...)` のみを公開（最小動作確認用）

**Non-Goals:**

- `MAUI BindableObject` Cell 階層、`SettingsViewHandler`、`CellBaseHandler<TCell, TNativeCell>` の実装 → `add-maui-core` および `add-maui-cells`
- `LabelCell` 以外の Cell 用 `addXxxCell(...)` の追加（13 種類） → `add-maui-cells`
- Snapshot テスト基盤 → `add-maui-cells`
- 移行ガイド `docs/migration-from-aiforms.md` → `add-maui-cells`
- MAUI Sample アプリ（`samples/maui/`） → `add-samples-maui` および `add-maui-cells`
- NuGet 配信整備 → Phase 3 以降

## Decisions

### Decision 1: Native Bridge は別ライブラリ

**選択**: `KsSettingsViewBridge.framework` / `.aar` を `KsSettingsViewUI` / `ks-settingsview-ui` とは別の独立ライブラリとして配置する。

**理由**:

- Bridge には `@objc public` / `@JvmStatic` などの「ObjC/Java 向け装飾」が必要で、Pure Swift / Pure Kotlin の API と分離した方が API サーフェスがクリーン
- Native 直接利用者（SwiftUI / Compose ユーザー）に Bridge は不要
- xcframework / aar の生成タイミングを Bridge 側で独立管理できる

**代替案**:

- `KsSettingsViewUI` 内に `@objc` API を併設：API が肥大、Native ユーザーに Bridge 用の DTO が見えてしまう。**不採用**

### Decision 2: Bridge の API は Builder + setRoot + applyDiff（二段構え）

**選択**: C# から扱える ObjC / Java 互換の Builder API を採用し、Builder で構築した `SettingsRoot` 値型を `Bridge.setRoot(root)` で Native UI 層へ全体差し替えする経路と、`Bridge.applyDiff(diff)` で部分更新する経路の二段構えとする。Theme は `SettingsRoot` ドメインモデルから完全分離されており（`purify-core-extract-style-to-ui-layer` の方針追随）、`Bridge.setTheme(theme)` 独立 API として扱う。Bridge 公開 API は以下：

- `KsSettingsViewBridge.makeBuilder()`：Builder インスタンス生成
- `builder.beginSection(header:footer:)` / `endSection()`
- `builder.addLabelCell(...)`（本提案では `LabelCell` 1 種類のみ公開、他 Cell は後続提案で追加）
- `builder.build() -> KsSettingsRootDTO`
- **Builder には `setTheme` を含めない**（Theme は `SettingsRoot` から分離。Bridge Controller / View 側の独立 API で扱う）
- `KsSettingsViewBridge.makeController(delegate:)` / `makeView(context:listener:)`
- `controller.setRoot(root)` / `view.setRoot(root)`: 初期化・全体差し替え
- `controller.applyDiff(diff)` / `view.applyDiff(diff)`: 部分更新（`KsSettingsRootDiffDTO` を受け取る、全 10 ケース。`UpdateTheme` は含まない）
- `controller.setTheme(theme)` / `view.setTheme(theme)`: Theme 適用の独立 API（`KsThemeDTO` を受け取り、Bridge 内部で Native `Theme` 値に変換した上で `SettingsRootStore.applyTheme(_:)` を呼ぶ。Diff Publisher は不発行）
- `controller.setStyle(style)` / `view.setStyle(style)`
- `controller.setRootHeader(view:)` / `controller.setRootFooter(view:)` / `view.setRootHeader(view:)` / `view.setRootFooter(view:)`: Root H/F の View 設定（`KsAnyViewDTO` を受け取る）
- `controller.updateCellValue(cellId:value:)`: 高頻度更新パス

`KsSettingsRootDiffDTO` は `add-partial-update-core` の `SettingsRootDiff` を ObjC / Java 互換の class 階層に変換した DTO とする。Swift では `@objc class KsSettingsRootDiffDTO : NSObject` 抽象クラス + 各ケース用サブクラス（`KsSettingsRootDiffFullDTO`、`KsSettingsRootDiffInsertCellDTO` など）、Kotlin では `sealed class KsSettingsRootDiffDTO` + 各ケース用 `class` で表現する。**`KsSettingsRootDiffUpdateThemeDTO` は導入しない**（Native `SettingsRootDiff.updateTheme` が削除されたため）。

`KsThemeDTO` は Native UI 層 `Theme` の payload を ObjC / Java 互換で表現するクラスとする。Bridge 内部の変換ロジックで **MAUI `Microsoft.Maui.Graphics.Color` → Native (`UIColor` / Compose `Color`) を 1 段で直接変換**する。中間表現として `KsColorDTO` / `KsFontDTO` のような独自 Color / Font DTO 型は導入しない（`KsColor` 自体が `purify-core-extract-style-to-ui-layer` で削除されたため）。

**理由**:

- ObjC / Java は Swift `struct` や Kotlin `sealed class` を直接扱えないため、Builder API で C# 側から属性付きメソッド連鎖で Cell ツリーを宣言的に構築する
- `add-partial-update-native` で導入される `SettingsRootStore` / `applyDiff` API を MAUI Handler から呼び出せるようにする責務を Bridge が担う
- `add-maui-core` 側 `SettingsViewHandler` が `ObservableCollection.CollectionChanged` を購読し、`NotifyCollectionChangedAction` を `KsSettingsRootDiffDTO` に変換して `Bridge.applyDiff` を呼ぶ設計に対応する（`AiForms.Maui.NativeCollectionView` 流儀）
- `setRoot` は初期化・リセット時の経路として残し、部分更新は `applyDiff` で表現することで、二段構えの API を実現

**代替案**:

- `setRoot` のみ提供（細粒度 API なし）：`add-partial-update-native` で導入される部分更新の効果を MAUI 側に届けられず、AiForms 系の利点が失われる。**不採用**
- `applyDiff` のみ提供、`setRoot` も `Diff.full` で表現：API の用途分離が不明確、初期化と部分更新の区別がつきにくい。**不採用**
- `NSDictionary` / Java `Map` で DTO を渡す：型安全性が下がる、デバッグ困難。**不採用**

### Decision 3: ユーザー操作通知は単一 delegate / listener

**選択**: 1 つの `KsCellInteractionDelegate`（iOS）／ `KsCellInteractionListener`（Android）に全 Cell の操作通知を集約する。Cell 種別はメソッド名で識別（`didChangeBoolValue`、`didChangeTextValue` 等）。

**理由**:

- ObjC / Java は protocol / interface 1 つにまとめると C# 実装が単一クラスで済む
- C# Handler 側で Cell ID から具体 Cell インスタンスをルックアップして適切な BindableProperty に SetValue する集約処理が書ける

**代替案**:

- Cell 種別ごとに個別 protocol / interface：型安全性は高いが C# Handler が複雑化。**不採用**

### Decision 4: 本提案では LabelCell のみ Bridge API 公開

**選択**: 本提案完了時点で Bridge が公開する Cell 追加 API は `builder.addLabelCell(...)` のみとする。他 Cell 用 `addCommandCell` / `addSwitchCell` 等 13 種類は `add-maui-cells` で順次追加する。

**理由**:

- Bridge レイヤの「形」が確立すれば、Cell 追加は機械的なパターン適用となり並列実装が可能
- `LabelCell` は最も単純な Cell（双方向バインド不要、操作 delegate 不要）なので Bridge ↔ Native の経路を最小コストで検証できる
- `add-maui-core` は本提案の `addLabelCell` 1 個があれば `SettingsViewHandler` の `BuildAndSetRoot()` / `ApplyDiff()` 経路を完全に検証できる
- `add-maui-cells` で他 Cell を追加する際、本提案の Bridge プロジェクトに `addXxxCell` を**追加していく**形になる（Bridge プロジェクト自体は本提案で完成、API 表面のみ後続提案で拡張）

**代替案**:

- 本提案で 14 Cell 全ての `addXxxCell(...)` を Bridge API として揃える：Bridge 単体テストが肥大、Cell 種別ごとのバリエーション（双方向 / 入力系 / Custom）を検証するためのテストコードも本提案に集中。**不採用**

### Decision 5: Bridge API 表面の後続提案による拡張

**選択**: 本提案で確立した Bridge プロジェクト（Swift `KsSettingsViewBridge`、Kotlin `KsSettingsViewBridge`、`KsSettingsView.Bindings.iOS/Android.csproj`）に対して、`add-maui-cells` は新規 `addXxxCell(...)` メソッドや新規 delegate / listener メソッドを**追加していく**形で利用する。本提案が Bridge プロジェクト自体の存在・基本構造を保証し、Cell 種別ごとの追加は後続提案の責務に委ねる。

**理由**:

- Bridge プロジェクトの新設・取り込み作業は本提案で 1 度だけ実施すれば後続提案では「メソッド追加」のみで済む
- `add-maui-cells` 側で Bridge を新設し直す必要がなく、責務境界が明確
- ObjC / Java 互換の API 設計パターン（Builder メソッドの引数型、delegate メソッド命名規則）は本提案で確立される

**代替案**:

- Bridge プロジェクトの構造定義のみを本提案で行い、`addLabelCell` も `add-maui-cells` で初めて実装する：本提案で動作確認可能な Cell が 0 個になり、Bridge 単体テストが書けない。**不採用**

### Decision 6: Binding csproj 形式の選択

**選択**:

- iOS: `XcodeProject` 形式（`<XcodeProject Include="../../ios/...">` で `KsSettingsViewBridge` の Xcode プロジェクトを参照、自動で xcframework ビルド → `objective-sharpie` で `ApiDefinitions.cs` 自動生成）
- Android: `AndroidGradleProject` 形式（`<AndroidGradleProject Include="../../android/...">` で Gradle サブプロジェクトを参照、自動で aar ビルド → Xamarin Android Bindings の標準フローで Java バインディング自動生成）

**理由**:

- CommunityToolkit `Maui.NativeLibraryInterop` が推奨するモダンな形式
- Native ソース変更が `dotnet build` 経由で自動的に MAUI 側へ伝播するため、ビルド時の手動作業が不要
- `.skill/maui-native-binding-skill/` のガイドが直接適用可能

**代替案**:

- 事前ビルド済の xcframework / aar をリポジトリにコミット：バイナリ管理になり、Native 側変更時の更新漏れリスク。**不採用**

## Risks / Trade-offs

- **リスク**: MAUI 9 + Native Library Interop の組み合わせはまだ実例が少ない、xcframework / aar の取り込みで CI 環境差異が発生
  - **緩和策**: `maui-native-binding-skill` のガイドに準拠、ローカル macOS / Linux / Windows 各環境で初回ビルドを確認、CI 設定は `add-maui-core` 完了後の Sample 動作確認時に整備
- **リスク**: Swift `@objc` で表現できない型（Swift `struct`、ジェネリクス）を C# 側から扱えない
  - **緩和策**: Bridge 層で `@objc class`（reference type）と `NSObject` 互換 DTO で囲み、Pure Swift API は Bridge 内部に閉じる
- **リスク**: `objective-sharpie` 自動生成の `ApiDefinitions.cs` が手動修正を必要とする
  - **緩和策**: 生成後の手動修正パッチを `maui/KsSettingsView.Bindings.iOS/Patches/` 配下に `.diff` ファイルでバージョン管理、`docs/maui-bindings.md` に手順記載
- **リスク**: 本提案で公開 Bridge API が `LabelCell` のみなので、`add-maui-cells` 着手時に API 設計の見落としが発覚する可能性
  - **緩和策**: 本提案の `KsCellInteractionDelegate` / `KsCellInteractionListener` は 14 Cell 分のメソッド宣言を**インターフェース定義としては全部入れておく**（`add-maui-cells` 側で実体実装、本提案ではプロトコル定義だけ存在し空実装でビルドは通る）。Builder の `addXxxCell` 引数設計は元 `add-maui-bindings` tasks.md §1.3 / §2.4 を参照して 14 Cell 分の API シグネチャを `design.md` のリファレンス節に記載しておく

## Migration Plan

本提案は新規追加のため移行手順は不要。ただし以下の archive 順序を守る：

1. 本提案（`add-maui-bridge`）archive
2. `add-maui-core` archive
3. `add-samples-maui` archive
4. `add-maui-cells` archive（Bridge API に他 Cell 用 `addXxxCell(...)` を追加 + MAUI 側 Cell Handler 実装 + Sample 拡張）

## Open Questions

- `objective-sharpie` バージョンと .NET 9 推奨 `LibraryImport` の混在パターンは？ → `maui-native-binding-skill` の最新版を参照しつつ、Phase A 早期で確認。
- ~~`KsSettingsRootDTO` の命名（iOS Native 側 `SettingsRoot` 値型と MAUI 側 `SettingsRoot : BindableObject`（`add-maui-core` で導入）の名称衝突回避）：本提案では Native 側 DTO を `KsSettingsRootDTO` として確定し、MAUI 側の `BindableObject` 派生は `add-maui-core` で別名（候補：`SettingsRootDefinition`）にする方針。~~ **解決済**: `add-partial-update-core` で `SettingsRoot.header/footer` が削除されたため、MAUI 側で別途「XAML ルート用 BindableObject」型を導入する必要性が低下した。`add-maui-core` Decision 5 により `SettingsRootDefinition` の導入は撤回され、`SettingsView : View` 自身が `Sections` / `HeaderView` / `FooterView` を直接保持する設計に変更されている。命名衝突は「Native は `KsSettingsRootDTO`、MAUI は `SettingsView`」で十分回避される。
