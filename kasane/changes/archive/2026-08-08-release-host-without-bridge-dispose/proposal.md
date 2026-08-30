# Proposal: release-host-without-bridge-dispose

## Why

現行の Bridge 契約 (maui/ADR-0005) は「`makeHost*` の再呼び出しは同じ handle を返す / Host だけを解放する経路は存在しない」であり、新たな Host が必要なら `dispose()` で Bridge ごと破棄するしかない。しかし MAUI Handler は再接続 (ページ再訪・Activity 差し替え) のたびに新しい platform view を要求するため (MAUI ソースで裏取り済み)、DisconnectHandler で `dispose()` を呼ぶと Store 内容ごと失われ、再接続パターンが塞がっている。maui/ADR-0007 (proposed) で `releaseHost()` の追加を決定した。

## What Changes

- iOS / Android の Bridge に `releaseHost()` を追加する — Host のみ解放し、Store (設定ツリーと Theme) は維持する。冪等で、Host 不在時および `dispose()` 後は no-op
- 解放時に旧 handle の Store 購読を解除して無効化する (解放後の Store 更新は旧 handle に反映されない)。このため UI モジュールに購読解除 API (iOS: disconnectStore 相当 / Android: unbind 相当) を additive に追加する
- `makeHost*` の契約を「生きている Host があれば同じ handle を返し、解放後の呼び出しは Store 現在状態から表示復元した新しい handle を返す」に改める
- 両 OS 対称のテストを追加する (解放 → 再生成 → 状態復元、冪等性、dispose との相互作用)
- 影響 capability: maui-bridge

## Non-Goals

- MAUI facade / SettingsViewHandler の実装 (phase-2 本体の責務。DisconnectHandler → releaseHost の配線もそちら)
- Bridge 最終破棄 (`dispose()`) の呼び時機の設計 (phase-2 で「明示 dispose なし」決定済み、facade 側の話)
- 複数 Host の同時サポート (ADR-0005 のまま同時1個)

## Impact

- Bridge 公開 API の追加 (additive)。既存の Store 操作 12 メソッドの挙動は不変。`makeHost*` は「解放後の再呼び出し」という従来存在しなかった状態でのみ契約が変わる (既存利用者への破壊的変更なし)
- UI モジュール (`KsSettingsViewUI` / `ks-settingsview-ui`) への購読解除 API の追加は additive (既存公開 API の変更なし。iOS Store への replaceCells 対称化と同じ前例)
- 現行の Bridge 利用者は maui/tests の検証ホストのみ
- Android は解放後の再生成で新しい `Context` を受け取れるようになり、既存の「Bridge の寿命は Host の Context の寿命を超えてはならない」制約が「Host 解放後は Context 参照なし」に緩和される

## 級: M

1能力 (maui-bridge) 内の公開 API 追加 + 両 OS 対称テスト。

domain: maui
