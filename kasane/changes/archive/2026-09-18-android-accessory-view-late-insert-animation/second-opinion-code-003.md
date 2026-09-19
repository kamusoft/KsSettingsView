# セカンドオピニオン: android-accessory-view-late-insert-animation (code-003)
**相方**: codex / **label**: so-code-android-accessory-view-late-insert-animation / **日付**: 2026-09-17 / **対象**: 修正サイクル 2 後の作業ツリーの未コミット差分のうち android/・ios/・maui/ (ベース HEAD)
---
## 判定: CHANGES_REQUESTED

Critical 0 / Major 1 / Minor 1 / Suggestion 0

### [🟠 Major] ロールバック範囲が Native Host 生成完了まで届いていない

**該当箇所**: `maui/KsSettingsView.Maui/Handlers/SettingsViewHandler.cs:65`、`maui/KsSettingsView.Maui/Platforms/Android/SettingsViewHandler.cs:23`、`maui/KsSettingsView.Maui/Platforms/iOS/SettingsViewHandler.cs:21`

**問題点**: 前回指摘した gateway factory 失敗、途中実体化失敗、`SetRoot` 失敗は修正されています。しかし、`Controller.Connect` 成功後の次の処理にはロールバックがありません。

- `Controller.AttachInteractions()` / `AttachImages()` の失敗
- native `MakeHost()` の例外・null
- `ApplyRootAccessories()` での Root View 実体化・配信失敗
- `Containment.AddToParent()` の失敗

特に `ApplyRootAccessories()` は Host 生成後に Root View を実体化します。例えば Header の実体化後に Footer の実体化が失敗すると、`CreatePlatformView()` が例外で終わる一方、Controller は接続済みのままで gateway、native Host、旧 materializer の lease を保持します。

再試行時は既存 gateway の経路へ入り、`placement.Lease != null` によって新しい MauiContext で再実体化されません。Android/iOS の Bridge も既に作った native Host を返すため、失敗した Handler 世代の Context・wrapper が再利用され得ます。前回指摘した「materializer の差し込みから Native Host 生成完了までを一つの失敗可能な処理として扱う」という境界は、まだ完全には閉じていません。

**推奨修正**: 共通の `CreatePlatformView()` で `CreateHost()`、Root accessory 適用、containment 登録を `try/catch` し、失敗時に containment の解除と `ReleaseHost()` 相当の Host 世代ロールバックを行ってから再送出してください。Root Footer の実体化を意図的に失敗させ、先に作られた Header leaseとnative Hostが解放され、新しい seam で再試行できるテストを追加すると固定できます。

### [🟡 Minor] 接続失敗時の一括破棄が最初の Dispose 例外で中断する

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:249`

**問題点**: `DisposePlacedViews()` は先に全 placement から lease を取り外した後、順番に `Dispose()` します。最初の `Dispose()` が例外を投げると、残りの lease は既に placement から外れているうえ、ローカルリストとともに失われ、破棄されません。続く `DisposeRetired()` も呼ばれません。

同クラスの `DisposeRetiredViews()` は「1件が失敗しても全件を試行してから集約する」実装になっており、新しい失敗時後片付けだけがその耐障害性を持っていません。

**推奨修正**: placed lease も既存の退役キューへ移して共通の集約破棄を使うか、全件の破棄を試行して例外を集約してください。2件中1件目の破棄を失敗させ、2件目も破棄されるテストを追加してください。

## 前回指摘の確認

- Android の公開 API 露出: 解消。`KsSettingsView` 自身は receiver interface を実装せず、private receiver オブジェクトへ移っています。
- gateway factory 失敗時の seam 残留: 解消。
- 実体化途中／`SetRoot` 失敗時の lease 持ち越し: 解消。未登録 placement を直接走査し、再接続で新しい seam が使われるテストもあります。
- `CancellationException` の握り潰し: 解消。
- オーナー追認済みの MAUI Root 再構築設計: ADR-0016および仕様との整合性に問題ありません。
- Android・iOS の残りの対象差分には新規指摘はありません。

提示された Android 2942件、MAUI 579件、iOS 1050件、lint 0件を前提にした静的レビューです。`samples/` と `evidence/` は指定どおり対象外としました。


## 突き合わせ結果

突き合わせ相手: review-003.md (ホスト側、APPROVED / Suggestion 2)

- **確定** (双方一致): なし
- **採用候補** (相方のみ・根拠強): Native Host の生成完了までの失敗 (Host 生成・Root の header / footer の実体化と配信・親子関係の登録) にロールバックが無い — Major。Root の実体化を Host 生成の処理の中へ移したのは本 change (tasks 4.6) であり、失敗点を本 change が増やしている。レビューのループ上限 (3 周) に達したため、本 change で塞ぐかはオーナー判断へ上げる
- **採用** (相方のみ・根拠強): 接続失敗時の一括破棄が最初の破棄の例外で中断し、残りの lease が破棄されない — Minor。本 change が足した処理の中で数行で閉じる
- **降格**: なし
- **未解決**: なし (ホスト側は同箇所に欠陥なしと評価したが、相方の指摘と矛盾する主張はしていない)
