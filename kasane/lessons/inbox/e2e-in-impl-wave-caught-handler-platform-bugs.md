---
scope: impl
kind: success
severity: normal
count: 2
first-seen: 2026-08-08
last-seen: 2026-08-12
evidence:
  - add-maui-core (ユニットテスト 101件全 green の状態から、実装ウェーブ内の両 OS E2E 疎通で実バグ2件を発見・修正 — ①iOS: responder chain 起点の親 VC 解決が Host 自身の ViewController を返しクラッシュ ②Android: PlatformArrange が measure しないため Host 内部の RecyclerView が AT_MOST measure の結果のまま画面幅の約39%でしか描画されない。いずれも fake gateway ベースのユニットテストでは原理的に検出不能)
  - add-maui-custom-cell (Robolectric / net10.0 / iOS Simulator ユニット全 green の状態から、実装ウェーブ内 E2E で実欠陥2件を発見 — ①Android: CustomCell の Content が実機エミュレータで一切描画されない (Robolectric では緑) ②iOS: 高速スクロール往復後に一部行の Content が非決定的に欠落。いずれもユニットテスト非到達)
---

## ルール文

MAUI Handler / platform view 層 (containment・measure/arrange・attach 順序) に触れる実装タスクは、ユニットテストが全 green でも実バグが残る前提で、実装ウェーブの完了条件に実機/シミュレータでの E2E 疎通確認を含める。fake gateway / net10.0 テストは変換経路の契約は固定できるが、UIKit / Android View の実挙動 (responder chain・measure 契約) には到達できない。

## 経緯

- 2026-08-08 add-maui-core: グループ6 (Handler) の実装ウェーブに E2E 疎通を含めたことで、ユニットテスト非到達の実バグ2件 (iOS 親 VC 解決・Android measure) を同ウェーブ内で発見・修正できた。レビュー・verify に持ち越さず、修正コストが最小の時点で潰せた成功例。
