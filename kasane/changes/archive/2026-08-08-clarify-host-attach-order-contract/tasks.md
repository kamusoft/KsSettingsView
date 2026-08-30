# Tasks: clarify-host-attach-order-contract

## 1. iOS

- [x] 1.1 `KsSettingsViewController.viewDidLoad` を接続中 Store の現在状態 (root / theme) を pull して構築する形に改修する — Android の `resyncFromStore` パターンへの対称化 (→ Requirement: view load 時の Store 現在状態からの復元 / Decision 1・2)
- [x] 1.2 Store 非接続 init (root 直接指定) のフォールバックが従来どおり動くことを確認する (→ Scenario: Store 非接続 init は従来どおり init 時の root で表示する)
- [x] 1.3 iOS テスト追加 — WHEN は `loadViewIfNeeded()` で view load を誘発する (Decision 4):
  - Scenario: view load 前の構造操作が load 時に反映される
  - Scenario: view load 前の Cell 内容更新が load 時に反映される (`replaceCell` / `replaceCells` の両経路)
  - Scenario: view load 前の Section accessory / theme 変更が load 時に反映される
  - Scenario: Store 接続中の直接 applyTheme は view load 時に Store theme で上書きされる
  - Scenario: Root accessory は復元対象外で、所有者の再適用により反映される

## 2. Android

- [x] 2.1 回帰テスト追加 (コード変更なしの契約固定) — 判定はメインループを flush (Robolectric shadow looper 等) してから行う (Decision 3):
  - Scenario: attach 前の更新が attach 後に反映される (構造・`replaceCells`・theme)
  - Scenario: detach 中の更新が再 attach で反映される

## 3. 検証ホスト (E2E)

- [x] 3.1 maui/tests 検証ホストの iOS 回避策 (「取り付け → `LoadViewIfNeeded()` → 操作」) を外し、自然な順序 (Host 生成 → 操作 → 取り付け) に戻す。Root header / footer の更新は取り付け後の再適用に移す (Decision 1 の所有者責務)。合否基準: 起動後の設定 list に事前操作後の Section / Cell 列と theme が表示され、再適用後に Root header / footer が表示されること。スクリーンショットを証跡として残す

## 実装前ゲート

- [x] core/ADR-0019 の accepted 化 (2026-08-07 オーナー承認済み)。実装コードへの ADR 参照コメントを付与してよい
