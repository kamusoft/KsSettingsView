# Tasks: release-host-without-bridge-dispose

## 1. UI モジュール (購読解除 API の additive 追加)

- [x] 1.1 iOS `KsSettingsViewController` に Store 購読解除 API (disconnectStore 相当) を additive に追加する (既存公開 API の変更なし。Store 対称化 = maui/ADR-0002 と同じ additive 前例) (→ Requirement: Host の単独解放 / Scenario: 解放後の Store 更新は旧 handle に反映されない)
- [x] 1.2 Android `KsSettingsView` に `unbind()` 相当を additive に追加する (→ 同上)

## 2. iOS Bridge

- [x] 2.1 `KsSettingsBridge.releaseHost()` を実装する — 旧 Host の購読解除 (1.1 の API 利用)・`hostController` 解放・Store 維持・冪等・dispose 後 no-op (→ Scenario: 解放後の再生成は Store 現在状態を復元する / 解放後の Store 更新は旧 handle に反映されない / releaseHost は冪等で Store を維持する / dispose 後の releaseHost は no-op)
- [x] 2.2 `makeHostViewController()` を解放後の再生成に対応させる (→ Requirement: Native Host の生成と接続 / Scenario: 生きている Host がある間は同じ handle を返す)
- [x] 2.3 iOS テスト追加 (KsSettingsViewBridgeTests): 上記 Scenario 群 + 「解放後、Bridge は旧 Host への参照を保持しない」(weak 参照 + autoreleasepool で回収検証)

## 3. Android Bridge

- [x] 3.1 `KsSettingsBridge.releaseHost()` を実装する — `unbind()` 呼び出し・`hostView` 解放・Store 維持・冪等・dispose 後 no-op (→ iOS 2.1 と対称の Scenario 群)
- [x] 3.2 `makeHostView(context)` を解放後の再生成に対応させる (→ Scenario: Android は解放後に別の Context で再生成できる)
- [x] 3.3 Android テスト追加 (KsBridgeLifecycleTest / KsBridgeHostTest 拡張): iOS と対称の Scenario 群 + 「解放後、Bridge は旧 Host への参照を保持しない」(`WeakReference` + `System.gc()` ループで Host と Context の回収を検証)

## 4. Binding / E2E

- [x] 4.1 Binding (net10.0-ios / net10.0-android) から `releaseHost()` が呼び出せることを確認する (必要なら binding 定義を追加) (→ Requirement: .NET binding からの呼び出し / Scenario: C# からの参照とビルド)
- [x] 4.2 maui/tests の検証ホストに「解放 → Store 更新 → 再生成 → 復元表示」の E2E シナリオを追加する。合否基準: 再生成後の設定 list に更新後の Cell 内容が表示されること (スクリーンショットを証跡として残す) (→ Scenario: C# からの解放と再生成 / 解放中の更新は再生成時に反映される)

## 実装前ゲート

- [x] maui/ADR-0007 の accepted 化 (2026-08-07 オーナー承認済み)。実装コードへの ADR 参照コメントを付与してよい
