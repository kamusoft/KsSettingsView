# セカンドオピニオン: android-accessory-view-late-insert-animation (code-005)
**相方**: codex / **label**: so-code-android-accessory-view-late-insert-animation / **日付**: 2026-09-18 / **対象**: 修正サイクル 4 後の作業ツリーの未コミット差分のうち android/・ios/・maui/ (ベース HEAD)
---
## 判定: CHANGES_REQUESTED

- Critical: 0件
- Major: 2件
- Minor: 0件
- Suggestion: 0件

### Major 1: native への書き戻し失敗後も、参照中の wrapper を破棄している

該当箇所:

- `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:780`
- `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1178`
- 関連規約: `kasane/decisions/maui/0016-mauiview-materialization-self-measuring-wrapper.md:24`

問題点:

`ReleaseAccessoryViews` は書き戻し例外を収集した後、その書き戻しに失敗した slot の lease も `:785` で破棄します。同様に `ReleaseCellContentViews` も、内容なし世代の配信に失敗しても `:1180` で全 lease を破棄します。

その時点では native Host の解放はまだ `ReleaseHost:405` に到達しておらず、native が旧 wrapper を保持したまま `DisconnectHandlers` される可能性があります。これは ADR-0016 の「native への配信 → 旧 wrapper 破棄」という順序に反します。

今回追加されたテストは lease の `Dispose` 失敗を扱っていますが、`UpdateAccessory` / `ReplaceCell(s)` の失敗は再現していません。

推奨修正:

- 書き戻し・内容なし配信に失敗した lease は直ちに破棄せず、native Host の解放成功後まで延期してください。
- native Host の解放にも失敗した場合は、native が参照している可能性がある lease を安全に再試行できる場所へ保持してください。
- gateway の書き戻し・一括配信を失敗させ、失敗対象の lease が native Host 解放前に破棄されないことを検証するテストを追加してください。

### Major 2: 通常切断では親子関係の解消失敗によって Host 解放が中断する

該当箇所:

- `maui/KsSettingsView.Maui/Handlers/SettingsViewHandler.cs:111`

問題点:

`DisconnectHandler` は次の処理を逐次実行しており、`Containment.Remove()` が例外を投げると `Containment = null`、`view.ReleaseHost()`、`base.DisconnectHandler()`へ到達しません。

ロールバック側は同じ `Remove()` の失敗を独立した後片付け単位として扱っていますが、通常切断側には同じ保証がありません。`RecordingHostContainment.FailsToRemove` で失敗可能性もモデル化されています。

また、`view.ReleaseHost()` が集約例外を返す場合も、内部の Host 解放自体は済んでいても `base.DisconnectHandler()` が飛ばされます。

推奨修正:

- 通常切断も、親子関係の解消、`Containment` のクリア、`ReleaseHost`、基底 Handler の切断を独立して試行し、最後に例外を集約してください。
- `FailsToRemove = true` の通常切断テストを追加し、少なくとも `ReleaseHostCount == 1`、`Containment == null`、後続の Handler 解放まで到達することを確認してください。

## 前回指摘の解消確認

前回の「lease の破棄失敗によって native Host の解放まで到達しない」という直接の問題は解消されています。`ReleaseHost` は4単位を独立して試行し、追加テストもロールバック時・通常切断時の破棄失敗後に `DetachInteractions` と `ReleaseHost` へ到達することを確認しています。

例外がない正常系についても、Section accessory は書き戻し後、CustomCell は内容なし世代の配信後に lease を破棄しており、ADR-0016 の退役順序を維持しています。上記2件はいずれも例外発生時に残る問題です。

Android・iOSは前回から変更なしで、新規指摘はありません。テスト・ビルド・lint結果は提示された客観的事実として確認し、こちらでは実行していません。


## 突き合わせ結果

突き合わせ相手: review-005.md (ホスト側、APPROVED / Minor 1・Suggestion 3)

- **確定** (双方が同じ箇所を指す): 切断時の後片付けのうち、accessory の書き戻しと Cell の内容の後片付け (`ReleaseAccessoryViews` の書き戻しループ・`ReleaseCellContentViews`) — 相方は「書き戻し・内容なし配信に失敗した lease まで native Host の解放前に破棄し、maui/ADR-0016 の順序に反する」(Major)、ホスト側は「この部分の全件試行はテストで固定されておらず、元へ戻しても全件通る」(Minor)。同じ箇所の別の側面で、矛盾はしない
- **確定** (双方が同じ箇所を指す): 通常切断の `DisconnectHandler` が逐次のままで、親子関係の解消の失敗で Host の解放へ進まない — 相方 Major / ホスト側 Suggestion (本 change の diff 外、以前からの処理)
- **降格**: なし
- **未解決**: 「失敗時の後片付け」の指摘が 4 周連続で残り、修正のたびに 1 段深い異常系が出ている (収束していない)。本 change でどこまで塞ぐかをオーナー判断へ上げる
