---
type: concept
title: Native設定画面ホストの責務境界
description: Coreの設定状態をNativeリストへ接続し、宣言UIから再利用する共通Host境界
tags: [architecture, native-ui, host, lifecycle]
timestamp: 2026-07-18
---

## 目的

Native設定画面ホストは、Coreが表す完全な設定状態と変更意図を、各プラットフォームのリスト表示へ接続する。Native UIから直接利用できると同時に、宣言UIや外部バインディングから再利用される描画基盤となる。

## 責務境界

Hostは、非表示要素を含む完全モデル、表示対象だけのprojection、Native表示構造、更新購読のライフサイクルを整合させる。空の設定状態も有効な入力として扱う。

宣言ツリーから変更列を算出する責務、Coreモデルの定義、ThemeとCellStyleの値解決はHostの外側に置く。RootのHeader / FooterとThemeは画面側の状態であり、CoreのRootモデルへ混在させない。

iOSのsnapshotとAndroidの平坦リストは異なる表現だが、どちらも[表示状態同期](architecture/display-state-synchronization.md)の共通原則をNative表示へ適用する。

## 不変条件

- Native描画を宣言UIごとに重複実装せず、同じHostを再利用する。
- 完全モデルとvisible projectionを混同しない。
- 表示構造の同一性には安定した識別子を使う。
- 購読は画面のライフサイクルに結び付け、長命な状態保持者から画面を延命しない。
- Cell種別の追加でHost本体へ型分岐を増やさず、[Renderer Registry](architecture/cell-renderer-registry.md)へ委譲する。

