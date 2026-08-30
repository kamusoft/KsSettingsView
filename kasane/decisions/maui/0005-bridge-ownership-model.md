---
id: 0005
title: Bridge は Store と Native Host を所有し、独立モジュールとして生成・破棄を担う
status: accepted
date: 2026-08-04
---

## Context

add-maui-native-bridge のスペックレビュー (second-opinion-001 / 002) で、Bridge の内部所有 Store ([ADR-0001](0001-maui-bridge-dsl-variant-internal-store.md)) と既存 Native Host (iOS `KsSettingsViewController(store:)` / Android `KsSettingsView.bind(store)`) の接続経路が仕様から欠落していることが Critical として指摘された。Store を Bridge 内部に隠す以上、Host が要求する Store を外部から渡す手段がなく、Host の生成・所有・破棄の責務を決める必要がある。この判断は phase-2 の MAUI Handler・将来の複数 Host 対応・モジュール依存関係を制約する長期のアーキテクチャ判断である。

## Decision

- **所有**: Bridge は内部 Store に接続済みの Native Host handle (iOS: view controller、Android: view) を生成・公開する API を持つ。Host の寿命は Bridge に従う。
- **モジュール分離**: Native Bridge は各ビルドルート内の独立モジュール (iOS: `ios/` 配下の Bridge ライブラリ、Android: `android/` 配下の新規 Gradle module) とし、既存公開 API (`KsSettingsViewUI` / `ks-settingsview-ui`) に interop 都合の型を混入させない。Binding csproj は `maui/` 配下。
- **生成の制約**: Bridge は同時に1つの Host をサポートする。新たな Host が必要な場合は破棄後に再生成する。Android の `Context` は Host 生成 API の引数で受け取り、Bridge は保持しない。
- **破棄**: Bridge は明示的な破棄 API を持つ。破棄は冪等で、破棄後の操作 API 呼び出しは no-op、破棄後に Host が更新されることはない。
- **スレッド**: Bridge の全 API は各 platform の UI スレッドから呼び出す (呼び出し側契約)。Bridge 自身は marshal しない。

## Alternatives Considered

- **Native 側 factory が Bridge と Host を同時生成**: C# から見た所有・破棄の関係が複雑になるため却下。
- **Host が Bridge を受け取って内部 Store に接続**: 既存 Host の公開 API 変更が必要で、所有が逆転するため却下。
- **既存 UI モジュールへ interop API を直接追加**: interop 都合の型が既存公開 API に混入するため却下。
- **Bridge が全操作を UI スレッドへ marshal**: 薄い Bridge の原則に反し二重 dispatch の原因になる。phase-2 の MAUI Handler は UI スレッドで動作するため呼び出し側契約で足りる、として却下。

## Consequences

- 正: C# 側の所有モデルが「Bridge 1個を持ち、そこから Host を得る」だけになり単純。phase-2 の MAUI Handler (platform view を要求) に素直に接続できる。
- 正: 破棄の冪等性と破棄後 no-op により、MAUI 側の DisconnectHandler 経路から安全に呼べる。
- 負: 複数 Host (同一 Store の多画面表示) は将来要件になった時点で本 ADR の改訂が必要。
- 負: UI スレッド契約は Bridge では強制されないため、違反時の挙動は保証されない (呼び出し側の責務)。

---
出典: 2026-08-04 ksn-propose (add-maui-native-bridge) のセカンドオピニオン 001/002 とオーナー裁定
