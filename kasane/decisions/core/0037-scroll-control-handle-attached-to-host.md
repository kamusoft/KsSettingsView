---
id: 0037
title: スクロール制御は KsCollectionView と同形の命令ハンドルを Host につないで提供し、Store を経由しない
status: accepted
date: 2026-09-29
---

## Context

「特定の Section / Cell へ送る」「先頭・末尾へ送る」のような命令的なスクロールは、宣言的なデータの流れ (Store の状態 → Host の表示) の外にある。KsSettingsView には、両 OS ともスクロール位置を動かす公開 API が無い。移植元の AiForms が持つのは `ScrollToTop` / `ScrollToBottom` の bool プロパティ (true を代入すると発火し、ライブラリが false に戻す) だけである。

同系統のライブラリ KsCollectionView は、命令ハンドル `KsScrollController` を実装済みである (`../KsCollectionView/kasane/decisions/core/0007-scroll-controller.md`)。オーナーは、KsSettingsView のスクロール制御をこれに合わせるよう指示した。

Store の契約は状態保持と変更意図の通知であり、Native list や visible projection を担わない (kasane/concepts/core/architecture/store-and-update-streams.md)。一方、KsSettingsView の Store 方式では ViewModel が Store を既に所有しているため、命令を Store に乗せる道もあった。Android の Host は一覧への反映を非同期に行う。

前提: Store が表示位置・Native list を担わない契約が保たれていること。スクロール制御の形を KsCollectionView にそろえる方針であること。

## Decision

スクロール制御は、KsCollectionView と同形の命令ハンドルで提供する。両 OS 同名の plain オブジェクトで、ID で指す命令と先頭・末尾への命令を持つ。未接続・存在しない ID への命令は no-op とし、命令は保留中のデータ反映の後に実行する。

ハンドルは表示側 (UIKit Host / Android View Host) につなぐ。SwiftUI / Compose は修飾子・引数でハンドルを受けて Host へ渡す。命令は Store の公開操作・通知には足さない。

maui/ADR-0001 の「Bridge の公開 API を Store の公開操作へ変換して既存の収束経路に乗せる」は、状態の更新の経路についての決定である。状態ではない命令はその対象外とし、本決定と衝突しない。

含むもの: iOS / Android の Native と宣言 UI、および MAUI が命令を出すときに経由する共通の契約。含まないもの: 対象の種類と各層での対象の指し方、型の公開形 (設計。出典の agenda の決定事項)、MAUI の公開形 (maui/ADR-0029)、Host の作り直しをまたぐスクロール位置の保持。

## Alternatives Considered

- スクロール先を状態 (プロパティ) で渡す (AiForms の bool プロパティを含む): 一度きりの命令を状態で表すと、実行後に誰がいつ戻すかの約束が利用者に漏れるため却下 (KsCollectionView の同 ADR と同じ理由)。
- ハンドルを Store につなぐ (命令を Store の一過性通知に乗せる): Store 方式の ViewModel は Store だけで命令できる。しかし Store が表示の命令を運ぶよう契約が広がり、同じ Store を購読する全画面が動く。DSL 方式では Store が内部所有なので、結局ハンドルからの橋渡しが要る。これらの理由で却下。

## Consequences

- 正: KsCollectionView と同じ語彙・契約になり、両ライブラリの利用者が同じ書き方で命令できる。
- 正: Store の契約 (表示位置を担わない) を変えずに済む。
- 正: 同じ Store を複数の画面に出しても、命令が届くのはハンドルをつないだ画面だけになる。
- 負: Store 方式の ViewModel は、Store とは別にハンドルを持つ必要がある。
- 負: 「データ反映後に実行」は Store の通知順だけでは保証されないため、Host 側に反映の完了を待つ仕組みが要る。
- 負: Store を通らない表示側の経路が 1 本増え、状態の更新とは別に保守する必要がある。

## Revisit When

- 前提 (Context) が崩れたとき

---
出典: kasane/roadmaps/maui-support/phases/phase-8-scroll-control/history.md (2026-09-29: スクロール制御の API の形 / 命令ハンドルの接続先) / ../KsCollectionView/kasane/decisions/core/0007-scroll-controller.md
関連: maui/ADR-0029 (MAUI での公開形)
