---
scope: impl
kind: pain
severity: normal
count: 1
first-seen: 2026-08-20
last-seen: 2026-08-20
evidence:
  - add-maui-modern-style (Android Bridge の resolveSectionMargin() が Compose 標準の PaddingValues(...) ファクトリで組み立てており、構築時の「0 以上」事前条件が負値・NaN を IllegalArgumentException で拒否 — 「facade は検証せず素通しし正規化は Native の描画時のみ」の契約が委譲先へ届く前に破綻。facade テストは fake gateway、UI 層テストは RawPaddingValues で、境界の実経路が両側からテストダブルに挟まれて未検証だったため全 2518 件の緑を通過。review-001 Critical + 相方 Major の双方一致で検出し、非検証実装 KsBridgeSectionMargin へ差し替えて解消)
---

## ルール文

「検証せず素通しする」契約の経路を実装するときは、経路上の中間層が使う標準型・ファクトリの構築時事前条件 (非負要求・NaN 拒否等) を確認し、境界の実経路を通しで検証するテスト (範囲外・非有限値を実際の輸送 API へ渡すケース) を置く。経路の両端をテストダブル (fake gateway / テスト専用型) で挟むと境界そのものが未検証になる。

## 経緯

- 2026-08-20 add-maui-modern-style: 素通し契約の Requirement は facade (fake gateway) と UI 層 (RawPaddingValues) の単体テストでは全て緑だったが、実経路の Bridge 中間層が検証付きファクトリを使っており、公開 API のセッターから到達できるクラッシュが残っていた。しかも同リポジトリのテストヘルパ KDoc がこのファクトリの事前条件を明記済みだった (既知情報の見落とし)。修正後は両 OS 対称の通し回帰テスト (①生値が Theme へ届く ②描画まで通して 0 へ正規化) が入った。
