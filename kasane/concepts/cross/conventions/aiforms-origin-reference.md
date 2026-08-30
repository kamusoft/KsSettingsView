---
type: reference
title: 移植元 AiForms リポジトリの参照
description: KsSettingsView の移植元 (AiForms.Maui.SettingsView / NativeCollectionView) の在り処と、移植前の仕様・不具合調査で参照するルール。移植完了までの時限規約
tags: [aiforms, origin, porting, reference]
timestamp: 2026-07-31
---

# 移植元 AiForms リポジトリの参照

この文書は、KsSettingsView の移植元である AiForms 系リポジトリの在り処と、どんなときにそれを参照すべきかのルールを定める。読むと、未移植機能の実装や不具合調査で「移植前の正」をどこで確認すればよいかが分かる。**移植が完全に終わるまでの時限規約**である。

## 成り立ちと目的

KsSettingsView は、.NET MAUI 用ライブラリ **AiForms.Maui.SettingsView** を Native ベースへ移植・リファインしたものであり、移植はまだ途中である ([cross/ADR-0017](../../../decisions/cross/0017-port-aiforms-to-native.md))。Native (iOS / Android) が主、MAUI が副という位置付けになる (この主従は参照の仕方には影響しない — どの platform の実装でも移植元の参照ルールは同じ)。

移植前の仕様・挙動の正は移植元コードにしか存在しない。移植元を参照せずに実装・調査すると、仕様の取り違え (移植時の退行を「仕様」と誤認する、意図された挙動を「バグ」と誤認する) が起こる。

## 参照先

| リポジトリ | ローカルパス | リモート | 役割 |
|---|---|---|---|
| AiForms.Maui.SettingsView | `../AiForms.Maui.SettingsView` | `github.com/muak/AiForms.Maui.SettingsView` | API・振る舞いの**主参照元**。upstream で不具合修正が継続中 |
| AiForms.Maui.NativeCollectionView | `../AiForms.Maui.NativeCollectionView` | なし (**ローカルのみ**) | Native UI 層 (UICollectionView / RecyclerView) の実装パターン参照元 |

補助資料: [aiforms-spec-summary.md](aiforms-spec-summary.md) — 移植元仕様の要約。凍結された歴史資料であり古い記述があり得るため、最終的な正は移植元コードで確認する。

## 参照するルール

- **未移植機能を実装するとき**: 移植元の該当実装を読み、仕様・挙動を確認してから設計する
- **不具合・挙動差を調査するとき**: 移植元の同機能の挙動と突き合わせ、意図された仕様なのか移植時の退行なのかを判別する
- SettingsView は upstream で修正が続いているため、不具合調査では移植元の最新コミットに同種の修正が入っていないかも確認する

## 時限性

移植が完全に終わった時点でこの規約は役目を終える。終了時はこの概念を削除し (成り立ちの記録は cross/ADR-0017 が持ち続ける)、[cross の index.md](../index.md) と [concepts の log.md](../../log.md) に廃止を記録する。`timestamp` は移植状況と参照先の在り処を確認した日。高腐食 (移植の進行とともに古くなりやすい) だが高価値の概念として、ksn-drift (ドキュメントとコードの乖離検出) の重点棚卸し対象とする。

## してはいけないこと

- 移植元ソースを KsSettingsView 内へコピー配置しない (2026-07-31 検討の上で不採用。upstream の更新が続いておりスナップショットはすぐ腐る。GitHub リモートから再取得できるため二重管理する価値がない)
- 移植元との互換 shim (AiForms API をそのまま受け付ける互換層) を提供しない。KsSettingsView は仕様と実装パターンだけを継承する独立ブランドであり、互換層を持つと AiForms の API 制約に引きずられてリファイン (設計の仕切り直し) の目的が損なわれる
- NativeCollectionView をあると思い込まない — リモートを持たないローカル専用リポジトリのため、消失すると参照不能になる。在り処やバックアップ状況が変わったら本概念を更新する

## 関連

- [cross/ADR-0017](../../../decisions/cross/0017-port-aiforms-to-native.md) — Native ベースへ移植・リファインする決定 (成り立ちの恒久記録)
- [リポジトリとビルドの責務境界](../architecture/repository-boundaries.md) — 移植先である本リポジトリの構成
