# セカンドオピニオン: android-accessory-view-late-insert-animation (code-004)
**相方**: codex / **label**: so-code-android-accessory-view-late-insert-animation / **日付**: 2026-09-18 / **対象**: 修正サイクル 3 後の作業ツリーの未コミット差分のうち android/・ios/・maui/ (ベース HEAD)
---
## 判定: CHANGES_REQUESTED

Critical 0 / Major 1 / Minor 0 / Suggestion 0

### [🟠 Major] `ReleaseHost` 内の失敗で native Host の解放まで到達しない

**該当箇所**: `maui/KsSettingsView.Maui/Handlers/SettingsViewHandler.cs:148`、`maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:392`、同 `:744`、同 `:1123`

**問題点**: Host 生成区間を `try/catch` で囲み、containment の解消後も `ReleaseHost()` を試みる修正自体は妥当です。しかし、`RollbackHost()` は `VirtualView.ReleaseHost()` 全体を一つの処理として捕捉しており、`ReleaseHost()` 内部は次の処理を逐次実行するだけです。

1. accessory lease の書き戻し・破棄
2. CustomCell content lease の書き戻し・破棄
3. interaction の解除
4. native Host の解放

`ReleaseAccessoryViews()` または `ReleaseCellContentViews()` の更新・`Dispose()` が一つでも例外を投げると、後続の処理は実行されません。例えば Root Header の lease 破棄が失敗すると、Root FooterやCell contentの lease、interaction、native Hostが残ります。失敗した世代を次の生成へ持ち越すという、今回塞ぐべき状態がまだ発生します。

追加された `AFailedRollbackKeepsTheOriginalHostFailure` は containment の `Remove()` だけを失敗させており、その後の `ReleaseHost()` が内部で失敗する経路は検出できません。これは deviation.md に記録された「後片付けの一つが失敗しても残りを試みる」という合意済み設計にも反します。

**推奨修正**: `KsSettingsController.ReleaseHost()` 自体を全件試行型にしてください。accessory、Cell content、interaction、native Host の各後片付けを独立して試み、最後に例外を集約する必要があります。`ReleaseAccessoryViews()`／`ReleaseCellContentViews()` も、全leaseを取り出した後は一件の更新・破棄失敗で残りを飛ばさない構造にしてください。

Root accessoryの最初のlease破棄を失敗させたrollbackテストを追加し、少なくとも次を確認するのが適切です。

- 後続leaseも破棄される
- interactionが解除される
- `gateway.ReleaseHost()` が呼ばれる
- 集約例外の先頭に元のHost生成失敗が残る

## 前回指摘の確認

- 接続失敗時のplaced／retired leaseの全件破棄: **解消**。両方を試行して例外を集約し、残りのleaseも破棄するテストがあります。
- gateway生成・途中実体化・`SetRoot`失敗時のロールバック: **解消**。
- Host生成・Root accessory適用・containment登録を一つの失敗単位にする修正: **基本経路は解消**。ただし上記のとおり、Host世代の後片付け自体が失敗する経路が残っています。
- Android・iOSの変更済み差分: 今回のサイクルによる新規問題は確認できませんでした。

提示されたMAUI 584件、Android 2942件、iOS 1050件、全TFMビルドおよびlintの結果を前提とした静的レビューです。`samples/` と `evidence/` は対象外としました。

