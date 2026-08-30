---
type: concept
title: 宣言UIとNative HostのBridge
description: SwiftUI・Composeの宣言状態をStoreとNative Hostの共通更新経路へ接続する境界
tags: [architecture, declarative-ui, swiftui, compose]
timestamp: 2026-07-18
---

## 二つの利用方式

一般的な設定画面には宣言ツリー方式を提供し、大量データや高頻度の命令型操作には利用者所有Store方式を提供する。両方式は別の描画基盤を持たず、同じStore、変更通知、Native Hostの経路へ収束する。

## 宣言ツリー方式

宣言UIのidentityが続く間、Bridgeは内部Storeと前回の宣言状態を保持する。再評価された宣言状態は、前回状態との差を通じて既存の更新経路へ反映する。

宣言評価中にNative表示を直接変更せず、各フレームワークのNative更新境界から反映する。これにより宣言評価の純粋性とNative側のライフサイクルを分離する。

## 状態の接続

宣言UIが所有する可変状態は、評価時点の値として不変なCellへ写す。ユーザー操作はCellが保持するコールバックから宣言UI側の状態へ戻す。Cell自身は宣言フレームワークの可変状態オブジェクトを状態として所有しない。

状態値の変化は内容更新であり、[宣言ツリーの同一性](architecture/declarative-tree-identity.md)を変えない。入力頻度の制御やNative Viewの即時更新は、このBridgeの長命な契約には含めない。

## 責務境界

- Bridgeは宣言状態と値・イベントの相互変換を担う。
- Nativeリスト、Cell描画、Style解決、visible projectionは下位Host層が担う。
- Root Header / Footer、Theme、描画Styleは画面側の指定として扱い、CoreのRootへ混在させない。

