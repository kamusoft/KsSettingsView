---
scope: process
kind: success
severity: normal
count: 1
first-seen: 2026-08-05
last-seen: 2026-08-05
evidence:
  - add-maui-native-bridge (「SDK 制約」主張の裏取りに ksn-dual-research を投入し、iOS の主張を実験で反証・Android の主張を実測確認。半分誤りの制約主張が deviation として固定される前に切り分けられた)
---

## ルール文

「SDK / ツールチェーン制約でできない」という実装側の主張が spec 逸脱 (deviation) の根拠になる場合、主張のまま deviation を承認せず、実測実験 (規模が大きければ ksn-dual-research の並走調査) で裏取りしてから確定する。制約主張は実装失敗の自己弁護と区別がつかず、実測でしか切り分けられない。

## 経緯

- 2026-08-05 add-maui-native-bridge: Binding csproj の Exec 方式逸脱の理由「SDK 制約」をオーナーが疑義とし、ksn-dual-research (相方 codex 2ラウンド + ホスト側実験、参考プロジェクト AdMobMediation.Maui) を指示。iOS は「SDK が署名設定を渡せないため framework が install されない」という因果が実験で不成立と判明し標準 XcodeProject 方式へ復帰、Android は init script の buildDirectory 束ねによるビルド不能を実測確認して deviation 承認。主張の半分が誤りだったことを本実装確定前に切り分けられた。
