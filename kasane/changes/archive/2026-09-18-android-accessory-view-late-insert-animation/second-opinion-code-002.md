# セカンドオピニオン: android-accessory-view-late-insert-animation (code-002)
**相方**: codex / **label**: so-code-android-accessory-view-late-insert-animation / **日付**: 2026-09-17 / **対象**: 修正サイクル 1 後の作業ツリーの未コミット差分 (ベース HEAD)
---
## 判定: CHANGES_REQUESTED

Critical 0 / Major 1 / Minor 0 / Suggestion 0

### Major

- 該当箇所: `maui/KsSettingsView.Maui/SettingsView.cs:983`、`maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:225`、`maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1087`
- 問題点: Host 生成前へ移した View 実体化の失敗ロールバックが不完全です。

  具体的には次の経路があります。

  1. `ConnectGateway` は `AttachViews` の後に `gatewayFactory()` を呼ぶため、factory が例外を投げると `KsSettingsController.Connect` の catch / `Disconnect` を通りません。`_views` が残ったままなので、未接続中のプロパティ変更で lease を生成でき、次の接続ではその lease が既に存在するため、新しい materializer で作り直されません。
  2. `MaterializeTreeViews` は `SetRoot` と `RegisterSection` より前に placement と lease を生成します。その後の materialize または `SetRoot` が失敗すると `Disconnect` は呼ばれますが、`ClearRegistrations` が処理するのは登録済み Section / Cell だけです。まだ対応表へ登録されていない `_accessories` / `_cellContents` の lease は残ります。次回接続では `placement.Lease != null` により再実体化されず、失敗した Host 世代の wrapper が新しい Host へ渡され得ます。

  `_views = null` だけでは「失敗した Host 世代の派生物を次へ持ち越さない」という maui/ADR-0016 の寿命契約を満たしません。MauiContext の保持、古い Handler の再利用、wrapper のリークにつながります。

- 推奨修正: materializer の差し込みから Native Host 生成完了までを一つの失敗可能な処理として扱い、失敗時に未登録を含む全 placement の lease を破棄して `Lease = null` に戻してください。少なくとも `gatewayFactory()` は `AttachViews` より先に行うか、外側の catch から共通の abort 処理を呼ぶ必要があります。次をテストすると回帰を固定できます。

  - gateway factory が失敗した後に View を配置し、別 materializer で再接続するケース
  - 1件目の実体化後に materializer または `SetRoot` が失敗し、再接続するケース
  - 失敗世代の lease が破棄され、新しい materializer の platform view が輸送されること

## 前回指摘の解消確認

- Android の旧 Store 配送競合: 解消しています。配送元 Store を伴う内部 receiver、binding 切替と pending 値を同じロックで管理する実装、および配送途中の rebind / unbind テストにより、以前の Store の値が反映される競合は閉じられています。
- MAUI の Root 再構築時の早すぎる旧 lease 破棄: 解消しています。後片付け待ちの同一 View は事前実体化から除外され、`SetRoot` 後に旧 lease を破棄して再実体化する順序になっています。
- iOS の遷移直後フレーム証跡: 指示どおり既知の未完了として扱い、新規指摘にはしていません。

## 未裁定の MAUI 設計変更について

deviation.md の「後片付け待ち View を事前実体化から外し、SetRoot 後に実体化・配信する」判断は妥当で、オーナーによる採用を推奨します。

- maui-core / maui-cells の新しい保証対象は初回表示と Handler 再接続です。表示中の Root 再構築は前者では明示的に対象外で、後者も既存の実行時反映契約を維持する記述です。
- maui/ADR-0027 の事前実体化要求も、初回接続と Host 再接続を対象としています。
- 一方、accepted の maui/ADR-0016 は「native への配信後に旧 wrapper を破棄」「同一 View の再実体化前に旧 lease を破棄」を要求します。現在の二段階処理は、この両方を守るために必要です。
- 初回接続・再接続では後片付け待ち lease が存在しないため、新仕様の「Host 生成時点の状態へ含める」経路には影響しません。

申告されたテスト・lint 結果は前提として確認しました。今回は制約どおり実行していません。


## 突き合わせ結果

突き合わせ相手: review-002.md (ホスト側、CHANGES_REQUESTED)

- **確定** (双方一致): 接続が実体化より後で失敗したとき、未登録の配置の lease が後片付けされず次の接続へ残る (gateway 生成の失敗経路を含む) — 相方 Major / ホスト Minor。高い方の Major として扱う
- **採用** (ホストのみ・根拠強): 受け口の internal interface 化で `KsSettingsView` の公開 API に `receiveRootAccessoryUpdate` が未修飾で露出する — Major (本ファイルの対象外だが判定処理では同格)
- **降格**: なし
- **未解決**: なし。未裁定だった「表示中の Root 作り直しでは後片付け待ちの View を事前実体化から外す」判断は、両レビューとも maui/ADR-0016 の退役順序を守るために必要で妥当と評価 (矛盾なし)
