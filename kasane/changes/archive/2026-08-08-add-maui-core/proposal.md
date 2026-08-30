# Proposal: add-maui-core

## Why

phase-1 (add-maui-native-bridge) で C# → Bridge → Store → Native Host の経路は整備され、検証ホストで end-to-end 疎通済み。しかし MAUI アプリ利用者が XAML/C# から使える公開面はまだ存在しない。本変更は `KsSettingsView.Maui` 本体 — SettingsView / Section / CellBase の BindableObject 階層と SettingsViewHandler — を整備し、LabelCell で XAML → Bridge → Native の経路を動作証明する。ここで確立する変換経路・lifecycle・対応表のパターンが後続の全 Cell フェーズ (4〜6) の土台になる。

## What Changes

- 新規 `maui/KsSettingsView.Maui/` csproj (net10.0-ios / net10.0-android、Binding csproj 参照、slnx へ追加) と対応するテストプロジェクト
- **BindableObject 階層**: `SettingsView` (View 派生、`Root` コンテナ = AiForms 同形) / `Section` / `CellBase` / `LabelCell`。公開面は maui/ADR-0008 の3原則 (A 分類は AiForms 命名踏襲 / B 分類は提供しない / Font 系は分割公開 + facade 合成) に従う
- **Root header / footer**: `RootHeaderText` / `RootFooterText` (string) → `updateAccessory` (root)。再適用は Handler 取り付け後に行う (core/ADR-0019、Android の attach 前 Diff 消失対策)。`RootHeaderView` / `RootFooterView` は phase-6 の予約名であり、本変更では公開 API を追加しない
- **Section header / footer**: `HeaderText` / `FooterText` (対称対、AiForms の `Title` は踏襲しない) → `updateAccessory` (section)
- **コレクション購読 → Diff 変換経路** (決定済み二層方式): `Root` / `Cells` は `IList<T>` 公開 (XAML content property、AiForms 同形)、実体が `INotifyCollectionChanged` なら購読して構造イベントを Bridge 構造操作へ即時 1:1 変換 (`Reset` のみ `setRoot` 再構築)。内容更新は dirty set + `Dispatcher.Dispatch` で同一 UI サイクル末尾に1回フラッシュ (`replaceCell` / `replaceCells`)。cellId ↔ CellBase の双方向対応表は SettingsView (facade) が一元管理。エコー抑止フックを差せる口のみ確保 (実装は phase-4)
- **ItemsSource / ItemTemplate の器**: SettingsView 直下の Section 生成 + Section 配下の Cell 生成 (非仮想化の AiForms 方式)。テンプレ生成物は上記変換経路に乗せる
- **Handler 基盤**: `SettingsViewHandler` 1件のみ (`CreatePlatformView` で `makeHost*` / `DisconnectHandler` で `releaseHost()` + 購読解除の 1:1 対応)。`AddKsSettingsView()` はこの 1 Handler 登録のまま将来も増えない
- **リークテスト基盤**: 切断後に Handler / platform view / Host native 実体が回収されることを `WeakReference` + GC ループで検証する基盤を設置 (後続フェーズが再利用)
- **LabelCell 疎通**: 検証ホスト (maui/tests) での end-to-end 表示確認

影響 capability: `maui-core` (新規)

## Non-Goals

- Theme 系 BindableProperty (SettingsView の styling 公開面) と `setTheme` 変換経路 — 後続フェーズ (phase-4 着手時に置き場を確定)
- `Section.IsVisible` (ADR-0008 の A 分類だが Bridge interop が未輸送) — Bridge 輸送拡張と併せて後続フェーズで扱う (phase-4 agenda へ論点として引き継ぎ)
- ユーザー操作通知 (delegate/listener) とエコー抑止の実装 — 最初の対話型 Cell を通す phase-4 の責務 (agenda 決定事項)
- `RootHeaderView` / `RootFooterView` / Section の View 版 accessory の輸送・実体化 — phase-6 の責務 (名前予約のみ)
- サンプルアプリ — phase-3 の責務
- DataTemplate 仮想化・D&D・Scroll 制御等の B 分類 — 強化フェーズ (7〜10) で Native から再設計
- NuGet パッケージング (ProjectReference での動作まで)

## Impact

- 純粋な追加 (新規 csproj 2つ + slnx)。Native / Bridge / Binding 側の変更なし — 前提だった Bridge 側整備 (releaseHost / attach 順序契約) は先行変更で完了済み
- 破壊的変更なし。既存の検証ホストは維持 (LabelCell 疎通確認に利用)
- リスク: MAUI Handler の lifecycle (切断・再接続・リーク) が本丸。releaseHost / 復元契約 (ADR-0007 / ADR-0019) で Bridge 側の保証は確立済みのため、リスクは MAUI 層の購読解除漏れに集中 — リークテスト基盤で検証する

## 級: L

公開 API 面の新設 (覆すコスト高) + MAUI ↔ Bridge の境界をまたぐ + 後続全 Cell フェーズの実装パターンを制約するため。

domain: maui
roadmap: maui-support/phase-2-maui-core
