---
type: concept
title: Store の状態と更新通知
description: 復元可能な現在状態と一過性の更新通知を分離する SettingsRootStore の共通契約
tags: [architecture, store, state, updates]
timestamp: 2026-07-19
---

この文書は、iOS / Android の `SettingsRootStore` が現在状態と変更通知をどう分けるかを説明する。読むと、購読開始前後の復元、構造 Diff、内容更新、Theme 更新の責務が分かる。

## 目的

Store は、hidden 要素を含む現在の `SettingsRoot` と現在の `Theme` を保持し、その後に起きた変更意図を Host へ通知する。通知を受け取る前の状態も、Store の現在値から復元できる。

| 種類 | iOS | Android |
|---|---|---|
| Root の現在値 | `root` | `state: StateFlow<SettingsRoot>` |
| Theme の現在値 | `theme` | `theme: StateFlow<Theme>` |
| 構造変更 | Combine publisher の `SettingsRootDiff` | module-internal `SharedFlow<SettingsRootDiff>` |
| 複数 Cell の内容更新 | 個々の `replaceCell` | `replaceCells` の内容更新バッチ |

## 更新契約

Section / Cell の公開操作は Store の現在状態を先に更新し、対応する構造 Diff または内容更新を通知する。insert / move の index は visible projection ではなく、hidden 要素を含む model 配列上の位置である。

同じ ID の Cell 内容更新は ID を変えない。ID 自体を変更する場合は、同一行の内容更新ではなく remove + insert などの構造変更で表す。

Theme は設定ツリーの構造ではない。`applyTheme` は Theme の現在値と専用通知だけを更新し、`SettingsRootDiff` を発行しない。同値 Theme は不要な再通知を起こさない。

## 責務境界

Store が担うのは状態保持と変更意図の通知である。Native snapshot / list、Renderer、animation、visible projection、宣言ツリー同士の比較は担わない。

SwiftUI / Compose の DSL 方式も内部に同じ Store を持つ。DSL の差分計算結果は Store の公開操作へ変換され、利用者所有 Store と同じ Host 更新経路へ流れる。

## 保証すること

- 購読開始時点の Root と Theme を Store の現在値から復元できる。
- 成功した構造操作では、更新後の状態と通知が同じ変更を表す。
- 同じ ID の内容更新で ID を変更しない。
- Theme 更新を構造 Diff として通知しない。
- Store は hidden 要素を完全な model に保持する。

## してはいけないこと

- Store の内部 mutable state を利用者側から直接更新しない。
- 部分操作の index を visible projection 上の位置として渡さない。
- Store から Native list、Renderer、ViewHolder を直接操作しない。
- Theme を `SettingsRoot` または `SettingsRootDiff` へ戻さない。

## 関連

- [Native Host の責務境界](native-host-boundary.md)
- [表示状態同期](display-state-synchronization.md)
- [SettingsRootDiff による構造変更](../core-model/structural-changes.md)
- [スタイルの所有と実効値解決](../styling/style-resolution.md)
