---
type: concept
title: Cell Renderer Registry
description: Cellモデル型とNative描画型を分離し、利用者定義Cellを追加する拡張境界
tags: [architecture, cell, renderer, extensibility]
timestamp: 2026-07-18
---

## 目的

Renderer Registryは、Cellモデル型とNative描画型の対応をHost本体の型分岐から分離する。ライブラリ提供Cellと利用者定義Cellを同じ解決経路で扱い、Cell追加時に描画基盤を変更しないための拡張境界である。

## 登録と解決

Registryはプロセス共通の既定登録を提供すると同時に、テストや隔離された構成へ独立した登録集合を注入できる。モデル型の対応から描画型を解決し、現在のThemeとCell値をNative表示へ反映する。

登録識別子の名前空間はHostが予約する領域と衝突させない。

## 診断と耐障害性

未登録Cellは厳格な診断モードで早期に検出する。耐障害性を優先するモードでは、画面全体を停止させず診断可能な代替表示へ退避する。

## 再利用時の不変条件

- 前のモデルのテキスト、画像、サブビュー、イベントハンドラを残さない。
- 非同期処理や購読を表示の再利用・破棄に合わせて解放する。
- 宣言UIを内包する描画型は、その表示寿命に合わせて宣言UIの構成を破棄する。
- Cell型の追加が[Native Host](architecture/native-host-boundary.md)本体の型分岐追加を要求しない。

