# Proposal: add-maui-native-bridge

## Why

KsSettingsView を .NET MAUI から利用可能にする maui-support ロードマップの第1フェーズ。C# から Native (iOS Swift / Android Kotlin) の SettingsView を呼ぶための interop 境界が存在しないため、薄い Bridge 層と Binding csproj を新設し、LabelCell 1種で Bridge→Store→Host の経路を縦に疎通させる。

## What Changes

- **iOS**: `@objc public` な Bridge 層 (別ライブラリ) を新設。内部所有 `SettingsRootStore` を持ち、公開 API を Store 公開操作へ変換する ([maui/ADR-0001](../../decisions/maui/0001-maui-bridge-dsl-variant-internal-store.md))。あわせて iOS `SettingsRootStore` に `replaceCells` (複数 Cell 内容更新バッチ) を追加し Android と契約を対称化する ([maui/ADR-0002](../../decisions/maui/0002-bridge-api-per-store-operation.md))
- **Android**: JVM 互換の Bridge 層 (別モジュール) を新設。同じく内部所有 Store 経由
- **MAUI**: XcodeProject / AndroidGradleProject 形式の Binding csproj を新設 (net10.0-ios / net10.0-android)。Binding は**輸送層** — MAUI 慣例型の公開 facade は phase-2 の責務 ([maui/ADR-0004](../../decisions/maui/0004-maui-idiomatic-types-for-styling.md))。先頭タスクは binding toolchain の net10.0 疎通 spike
- Bridge 公開 API は Store 操作 1:1 の12メソッド + `addLabelCell` を含む Builder (ID は Bridge 採番) + **内部 Store 接続済み Native Host の生成・公開と破棄 API** ([maui/ADR-0002](../../decisions/maui/0002-bridge-api-per-store-operation.md) / [maui/ADR-0005](../../decisions/maui/0005-bridge-ownership-model.md))。Theme は primitive の輸送 DTO で受ける
- ユーザー操作通知は単一 delegate/listener 方式を [maui/ADR-0003](../../decisions/maui/0003-single-interaction-delegate.md) で確定済み。ただし LabelCell は表示専用のため**実装は phase-4** (最初の対話型 Cell 導入時)
- 影響 capability: **maui-bridge (新設)**、**ios-store (replaceCells 追加)**

## Non-Goals

- LabelCell 以外の Cell の Bridge API・インターフェース定義 (各 Cell フェーズで additive に追加)
- MAUI 本体 (BindableObject / Handler 階層) と MAUI 慣例型の公開 facade — phase-2
- interaction delegate の実装 (方式は ADR-0003 で確定済み、実装は phase-4)
- 任意 View を内包する accessory の輸送 (phase-1 の accessory 輸送は text ベースに限定)
- `updateCellValue` 直行パス・debounce (作らないことを決定済み — phase-1 agenda 論点4)
- Store handle の C# 公開 (Store 方式) — 将来拡張 (ADR-0001)
- NuGet パッケージング

## Impact

- 破壊的変更なし (iOS `replaceCells` は additive な公開 API 追加)
- 新規モジュール追加が主で、既存コードへの変更は iOS Store のみ
- リスク: binding toolchain の net10.0 対応 (先頭 spike で早期検出し、問題があれば phase agenda に差し戻す)

## 級: L

複数能力横断 (maui-bridge 新設 + iOS Store 公開 API 変更) かつ interop アーキテクチャの新設のため

domain: cross
roadmap: maui-support/phase-1-native-bridge
