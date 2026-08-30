---
id: 0007
title: Bridge に Host 単独解放 (releaseHost) を追加し Host の寿命を Handler 接続に合わせる
status: accepted
date: 2026-08-06
---

## Context

現行の Bridge 契約 ([ADR-0005](0005-bridge-ownership-model.md)) は「`makeHost*` の再呼び出しは同じ handle を返す / Host だけを解放して作り直す経路は存在しない / 新たな Host が必要なら `dispose()` 後に Bridge ごと再生成」である。しかし MAUI Handler 層の設計では、DisconnectHandler で `dispose()` を呼ぶと再接続 (ページ再表示・Shell 再生成) 時に Store 内容ごと失われる (add-maui-native-bridge review-001 Minor-4 起点の課題)。

MAUI ソース (ローカルクローン) の裏取りで次を確認した:

- ページ pop → 再訪問では `DisconnectHandler` が呼ばれ、platform view は必ず新しい Handler + `CreatePlatformView` で作り直される (`ElementHandler` は disconnect で `PlatformView = null` にリセットする)。同一 platform view インスタンスの再利用はフレームワークの契約に無い
- 既定テンプレートは `ConfigurationChanges` 属性で画面回転等を吸収するため Activity 再生成は既定では起きないが、configChanges 改変・マルチウィンドウでは新 Activity 由来の新しい `MauiContext` が作られるパスが実在する

## Decision

- Bridge の公開 API に `releaseHost()` を追加する。Host のみ解放し、Store (設定ツリーと Theme) は維持する。冪等で、Host 不在時および `dispose()` 後は no-op
- 解放後の `makeHost*` は Store 現在状態から表示復元した**新しい** handle を返す。`makeHost*` の契約は「生きている Host があれば同じ handle を返す」に改める ([ADR-0005](0005-bridge-ownership-model.md) の生成の制約のうち「新たな Host が必要な場合は破棄後に再生成する」をこの決定で置き換える。所有・モジュール分離・破棄の冪等性・スレッド契約は不変)
- MAUI Handler は DisconnectHandler → `releaseHost()`、再接続 (ConnectHandler / `CreatePlatformView`) → `makeHost*`(その時点の Context) と 1:1 に対応させる。Android の Context は Host 生成のたびに再取得する

## Alternatives Considered

- **契約を「Bridge の寿命 = cross-platform control の寿命であり、DisconnectHandler では破棄しない」と固める (コード変更なし)**: MAUI は再接続時に必ず新しい platform view を要求するため、「同じ handle を返す」契約のままでは detach 済みの古い Host を再取り付けするしかなくフレームワークの契約に逆行する。Android では古い Context を保持した Host が生き残り、Activity 差し替えパス (configChanges 改変・マルチウィンドウ) で「Bridge の寿命は Host が保持する Context の寿命を超えてはならない」という既存 lifecycle 契約に違反するため却下

## Consequences

- 正: MAUI Handler の接続/切断サイクルと Bridge の Host 生成/解放が 1:1 対応し、再接続をまたいで Store 内容が保持される
- 正: 切断中は Host 不在となり、更新は Store にだけ適用され再接続時の表示復元で反映される — 非表示中の更新取りこぼしが構造的に消える
- 正: Host 生成ごとに Context を再取得するため、Activity 差し替えパスでも古い Context を握り続けない
- 負: 両 OS の Bridge に公開 API 追加と対称テストが必要になる
- 負: `makeHost*` の「常に同じ handle」という単純さが「生きている Host があれば同じ」という条件付きになる
- 負: Bridge 本体の最終破棄 (`dispose()`) の呼び時機は本 ADR の範囲外であり、利用層 (MAUI facade 等) の設計判断に委ねる

---
出典: kasane/changes/release-host-without-bridge-dispose/exploration.md / kasane/roadmaps/maui-support/phases/phase-2-maui-core/history.md (2026-08-06: Host 単独解放の不在)
