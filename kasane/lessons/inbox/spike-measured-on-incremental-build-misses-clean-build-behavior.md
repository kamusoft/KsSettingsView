---
scope: process
kind: pain
severity: normal
count: 1
first-seen: 2026-09-04
last-seen: 2026-09-04
evidence:
  - add-release-workflow (自 assembly 用 aar の除外。「SDK は無条件で aar を生成して nupkg に詰める」という前提を手元の増分ビルドだけで確定し、設計・スパイク・実装まで通した。CI のクリーン checkout では aar 自体が生成されず pack が「aar が見つからない」で失敗し、クリーン clone で再現した)
---

## ルール文 (候補)

外部 SDK・ビルドツールの挙動を前提として確定するスパイク (机上確定の裏取り) は、**クリーンな状態 (クリーン clone またはクリーンビルド) で採取する**。増分ビルドだけで観測した生成物・警告・入力の有無を前提に書かない。増分ビルドとクリーンビルドで結果が違い得る対象 (生成される中間成果物、SDK が入力から組み立てる同梱物、キャッシュ済みモジュールの診断) を扱うときは、両方で採取して差があれば前提に「増分ビルドでのみ現れる」と明記する。

事後判定: スパイクの証跡に採取条件 (クリーン / 増分) が書かれており、前提の記述が両方の結果と矛盾しない。

## 関連

[[warning-count-taken-from-incremental-build]] (コンパイラ診断の再表示) と [[ios-incremental-build-runs-stale-binary]] (配備の stale binary) と根は同じ (増分ビルドの観測を実測の根拠に使うと実態を外す)。本パターンは**スパイクで確定する前提そのもの**が増分ビルド由来だった場合を扱う。昇格時は 3 つの統合 (「増分ビルドの観測を根拠にしない」の一般化) を検討する。

## 経緯

- 2026-09-04 add-release-workflow: フェーズ議論の調査 (scout) と設計、tasks 冒頭のスパイク (pack 拡張点での aar 除外) はいずれも手元の増分ビルドで「SDK が生成する aar には推移依存の `.so` 4 ABI 分が入る」を観測し、その存在を前提に「aar が無ければ SDK 構成の変化とみなして pack を失敗させる」検査まで実装した。マージ後の PR CI (クリーン checkout) で消費者検証 MAUI の pack が「aar が見つからない」で失敗し、クリーン clone で再現。SDK の `_CreateAar` は入力が空だと target ごと skip され、推移依存の `.so` は増分ビルドでのみ入力に現れる。過去の XA4301 の観測 (add-consumer-verification の evidence) もすべて増分ビルド由来だった。修正は「aar が存在するときだけ検査・除外し、無ければ何もしない」(deviation.md に記録)。
