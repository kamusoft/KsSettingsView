---
id: 0003
title: Modern は insetGrouped を廃し自前の Section 装飾で実現する
status: accepted
date: 2026-08-20
---

## Context

`KsSettingsViewStyle.Modern` の現行 iOS 実装は、list appearance を `.insetGrouped` へ切り替えるだけで、余白・角丸は UIKit 任せである。Modern の完全実装にあたり、Section Margin (上下左右の余白)・Section Border Radius (角丸半径)・Section Border Width (ボーダー幅)・Section Border Color (ボーダー色) の4属性を利用者が制御できることが要件となった。

`.insetGrouped` はこれら4属性の制御 API を持たない。margin の一部を除き変更手段がなく、border に至っては iOS 標準の設定画面にも存在しない概念である。

一方 Android の Modern は `ModernSectionDecoration` による自前 Canvas 描画が既にあり (寸法はハードコード)、自前描画側に土台がある。iOS の現行 layout は `UICollectionViewCompositionalLayout` + `NSCollectionLayoutSection.list(using:)` の sectionProvider 構成で、Classic では `.plain` が既定で行う header の pin 固定を手動で外すなど、list 構成をカスタマイズする下地がある。

## Decision

iOS の Modern は `.insetGrouped` を廃し、現行の compositional layout の延長で自前の Section 装飾 (余白は section の contentInsets、角丸・ボーダーは装飾描画) として実現する。4属性の制御可能性を OS ネイティブ外観への自動追従より優先する。

これに伴い、Modern の既定の見た目 (余白・角丸の既定値) はライブラリが所有し、iOS の設定画面を模した値を自分で決めて維持する。

## Alternatives Considered

- **`.insetGrouped` 維持**: OS の外観進化 (iOS 26 の Liquid Glass 世代の大きな角丸など) に自動追従でき実装コストも最小だが、4属性の制御 API が存在せず要件を満たせない。ユーザー判断も「iOS に自動で追随してしまう弊害の方が大きい」であり、追従はむしろ非目標。不採用
- **未指定なら `.insetGrouped`・属性指定があれば自前装飾へ切替**: 未指定時のみ OS 追従を残せるが、Modern の描画経路が2本になり、実装・テスト・視覚保証の維持コストが倍増する。不採用

## Consequences

- 正: 4属性を利用者へ公開できる。装飾の描画根拠がコードに実在し、視覚仕様をライブラリが完全に説明できる
- 正: Android (自前 Canvas 描画) とアプローチが対称になり、共通契約 (4属性) を両 platform で同じ意味論で提供できる
- 負: OS が Modern 系外観を進化させても自動では追従しない。追従したい場合は既定値の改訂として自分で行う
- 負: 既定値 (余白・角丸) の選定と維持の責任をライブラリが負う

出典: kasane/changes/implement-modern-style/exploration.md (2026-08-19〜20 の探索議論)
