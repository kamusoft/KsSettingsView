---
scope: code-review
kind: pain
severity: normal
count: 1
first-seen: 2026-08-12
last-seen: 2026-08-12
evidence:
  - add-maui-accessory-views (未接続 Section の fallback が既存 Root accessory の論理親・BindingContext を例外なしに奪う件。ホスト review-003 は「spec が多重配置契約を既存 Section/CellBase 検出へ委譲しており CellBase も同型挙動」を根拠に Suggestion と判定、相方 second-opinion-code-003 は Major と判定して割れた。オーナー裁定は修正側 (cycle 4 で ReassignIfFree ガード追加))
---

## ルール文

「既存契約と同型」「spec が既存検出契約へ委譲済み」であることを根拠に、**無言で他所の正しい配置・状態を壊す**挙動の指摘を Suggestion へ降格しない。例外や表示で利用者が気づける壊れ方と、例外前に黙って状態が壊れて後から気づけない壊れ方は別の重さであり、後者は既存挙動とのパリティが取れていても Major 側で提示してオーナー裁定を仰ぐ。

## 経緯

- 2026-08-12 add-maui-accessory-views: `new Section { HeaderView = 既配置View }` のオブジェクト初期化子経路で、未接続 Section の propertyChanged 受け皿が多重配置検査を通らずに既存 Root 配置の論理親と継承 BindingContext を奪い、`Root.Add` の例外時には破壊済みという現象。ホストと相方が同一事実を確認した上で severity 判定が割れ (Suggestion vs Major)、max-review-cycles 到達と合わせてオーナーへ提示した結果、裁定は「修正する」(既配置 View は fallback で引き取らないガード + 例外時の既存配置無傷を固定するテスト)。spec 委譲・既存契約同型は「spec 違反ではない」ことの根拠にはなるが「修正不要」の根拠にはならなかった。
