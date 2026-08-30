# レビュー結果: add-maui-core (001 回目)

**日付**: 2026-08-08
**判定**: APPROVED

## サマリー

デルタスペック 14 Requirement / 24 Scenario をすべて実装で満たしており、変換経路・対応表・購読管理・バッチ配信・ItemsSource 器・Handler lifecycle のいずれにも仕様違反は見つからなかった。`dotnet test` は 101 件全成功 (net10.0)、comment-policy lint は 0 件、既存資産 (`ios/` / `android/` / `maui/macios/` / `maui/android/`) への変更もない。特に「対応表に載せる ID は gateway が返した ID のみ」という interop 契約は Bridge 側 Kotlin 実装 (`KsBridgeSection.makeSection` / `KsBridgeRootBuilder.makeRoot`) まで遡って照合し、`insertSection` / `replaceSection` 経路で DTO 側 cellID がそのまま Store identity になることを確認したうえで正しいと判定した。

指摘は Critical / Major なし、Minor 3 件・Suggestion 4 件。いずれも本変更のゴールを損なわず、後続フェーズまたは蒸留で処理できる。

## 確認した観点 (指摘なし)

- **仕様充足**: spec の全 Requirement を実装と照合。tasks.md 7.1〜7.6 のチェックはいずれも対応するテストが実在 (虚偽チェックなし)。足場アーティファクト (proposal / design / specs) は実装中に書き換えられていない (`kasane/roadmaps/` の 3 ファイル差分はフェーズ進捗の記録であり spec ではない)
- **deviation.md との整合**: 記録済み 6 件 (Element 派生 / Title の null 解決 / SetInheritedBindingContext / Cells Reset の setRoot 配信 / 重複配置例外の後状態 / DataTemplateSelector 非対応) はすべて実装と一致。未記録の無断逸脱は検出せず
- **削除済み要素からの通知遮断**: `KsSettingsController.HandleSectionPropertyChanged` が `_sectionEntries` の生存を必ずガードしてから `UpdateAccessory` を発行しており、`updateAccessory` が「未知 ID no-op 契約の対象外」であるという Bridge 契約 (concepts/maui/api/native-bridge.md) を正しく守っている。購読解除・対応表除去・dirty set からの除去が `UnregisterCell` / `UnregisterSection` に集約されている点も良い
- **リーク**: `KsWeakCollectionSubscription` / `KsWeakPropertySubscription` はいずれも通知元→中継が強・中継→観測者が弱で、observer 不在時にその場で購読を外す。`Section` / `CellBase` を `Element` 派生にしつつ `Parent` を配線しない判断と併せて、「外部保持があっても facade は回収される」SHALL が成立している (LeakTests で実測)
- **バッチ境界**: `_flushScheduled` の立て方と `Flush()` 冒頭での snapshot により、「最初の変更で予約された flush までが 1 バッチ」という dispatcher 実装非依存の定義が守られている。可視性変更の単発分離が内容バッチより先に発行される順序も実装・テストとも正しい
- **命名・スタイル**: csharp-impl-skill の規約 (Allman / `_camelCase` / 明示的な型 / primary constructor / 単一行 if の禁止) に一貫して従っている。public API の XML doc も全面的に付与されている
- **コメント規約**: `scripts/comment-policy-lint.py` で 429 ファイル / 禁止 0 件。change ID・Phase 番号・spec パスの混入なし。`(core/ADR-0019)` 参照は許容形式
- **手動確認した挙動 (テスト外)**: 一時的なプローブテストで以下 3 点が仕様どおりに動くことを実測し、確認後に削除した — ①`ItemTemplate` が既配置 Cell を返すと `InvalidOperationException`、②`Cells` 差し替え後に旧コレクションへの操作が配信されない、③`Root = null` が例外にならず空の `setRoot` になる

## 指摘事項

### [🟡 Minor] `IKsSettingsGateway` が `IDisposable` を宣言しているが、誰も `Dispose()` を呼ばない

**該当箇所**: `maui/KsSettingsView.Maui/Internals/IKsSettingsGateway.cs:16`、`maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:38`、`maui/KsSettingsView.Maui/Platforms/iOS/KsBridgeGateway.cs:106` / `Platforms/Android/KsBridgeGateway.cs:108`

**問題点**: `KsBridgeGateway.Dispose()` は `_bridge.DisposeBridge()` を呼ぶ実装を持つが、`KsSettingsController` は `_gateway` を保持したまま一度も `Dispose()` を呼ばない。`SettingsView` にも破棄口がないため、Bridge (と内部所有 Store) の解放は managed peer の GC 任せになる。`releaseHost()` が Host と `Context` を確実に解放するため実害は Store 分の遅延解放にとどまるが、インターフェースが宣言している破棄契約が誰にも履行されない状態は、後続フェーズで「Dispose を呼んでいるつもり」の誤読を招く。テストも `IsDisposed` が false のままであることしか確認していない (`HandlerTests.cs:58`)。

**推奨修正**: 次のいずれか。(a) 本変更の範囲では破棄契約を持たないことを明示するため `IKsSettingsGateway` から `IDisposable` を外し、`Dispose` は各 gateway 実装の固有メソッドに留める。(b) `IDisposable` を残すなら、破棄を誰がいつ行うか (facade の破棄口の有無を含む) を後続フェーズの論点として phase-4 agenda へ引き継ぐ。いずれにせよ「宣言されているが呼ばれない」状態のまま放置しない。

### [🟡 Minor] 新規プロジェクトの追加で NU1608 (AndroidX Lifecycle のバージョン制約外) が復元時に発生する

**該当箇所**: `maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj:12,30`

**問題点**: `dotnet restore` / `dotnet test` が毎回 NU1608 を 2 件出す。`Xamarin.AndroidX.Lifecycle.LiveData 2.9.2.1` (MAUI 側が要求) が `Lifecycle.LiveData.Core(.Ktx) >= 2.9.2.1 && < 2.9.3` を求めるのに対し、Binding 側の `Xamarin.AndroidX.Lifecycle.Runtime.Ktx 2.11.0.1` 経由で 2.11.0.1 が解決される。Binding csproj 単体の復元では NU1608 は 0 件であり、**本変更で `KsSettingsView.Maui` が両者を同一グラフに載せたことで新たに顕在化した警告**である。Android 実機 E2E は通っているため実害は観測されていないが、警告付きのビルドが常態化すると新規の警告を見落とす。

**推奨修正**: `KsSettingsView.Maui` (または Binding 側) で AndroidX Lifecycle 系のバージョンを揃えるか、揃えられない理由と安全性の根拠を `maui/README.md` の「既知の制約」へ記録して意図的な許容であることを示す (README の書き換えは `docs-refresh` 経由)。

### [🟡 Minor] spec の 2 つの SHALL にテストがない (挙動自体は正しいことを実測で確認済み)

**該当箇所**: `maui/KsSettingsView.Maui.Tests/ConversionPathTests.cs:312`、`maui/KsSettingsView.Maui.Tests/ItemsSourceTests.cs`

**問題点**: 次の 2 つは spec の明示的な SHALL だが、テストスイートに対応するケースがない。

- 「`Root` / `Cells` の差し替え時は、**旧コレクションの購読を解除し**」 — `Root` の差し替えは `ReplacingRootRebuildsAndDropsOldCollection` で旧コレクション無反映まで確認しているのに対し、`Cells` の差し替え (`ReplacingCellsIssuesReplaceSectionAndKeepsSectionId`) は新コレクションが観測されることしか確認していない
- 「ItemsSource のテンプレートが既配置のインスタンスを返した場合も同様とする (SHALL)」 (同一インスタンスの重複配置の禁止) — 手動追加のケース (`AddingCellPlacedElsewhereThrows...`) のみで、テンプレート経由のケースがない

いずれも一時プローブで正しく動くことを実測したため機能欠陥ではないが、`KsSettingsController.SubscribeCells` の解除漏れや `KsItemsSourceBinder.Create` の経路変更を回帰で検出できない。

**推奨修正**: `ConversionPathTests` へ「`Cells` 差し替え後に旧コレクションへ Add しても gateway 呼び出しが発生しない」、`ItemsSourceTests` へ「既配置 Cell を返す `DataTemplate` を設定すると `InvalidOperationException`」の 2 ケースを追加する。

### [🔵 Suggestion] `SettingsView.Root` 差し替え時の処理順が `Section.Cells` 差し替え時と非対称

**該当箇所**: `maui/KsSettingsView.Maui/SettingsView.cs:31-37`

**問題点**: `RootProperty` の propertyChanged は `_controller.SetRootCollection` → `_sectionBinder.OnTargetChanged` → `_sectionContextBinder.OnTargetChanged` の順で走る。一方 `Section.CellsProperty` (`Section.cs:47-52`) は binder が先に走り、その結果を含んだ状態を controller が `ReplaceSection` 1 回で送る。このため Root 側では ItemsSource 生成の Section が `setRoot` に含まれず N 件の `insertSection` として後追いで飛び、手動配置 Section の `{Binding}` 由来 accessory も一度未解決の値で送られてから `updateAccessory` で訂正される (同一 UI サイクル内なので表示上は問題ない)。

**推奨修正**: `RootProperty` でも binder を先に走らせ、controller の `SetRootCollection` を最後にすると `Section.Cells` 側と対称になり、native への往復も 1 回の `setRoot` に収まる。挙動が変わる変更なので本変更で無理に入れず、後続フェーズの改善候補として扱ってよい。

### [🔵 Suggestion] 重複配置例外がテンプレ生成経路で起きたときの `_generated` と生成先の不整合

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsItemsSourceBinder.cs:111-126`

**問題点**: `Generate()` のループ途中で `target.Insert` が重複配置の `InvalidOperationException` を投げると、`ObservableCollection` は要素を追加してからイベントを出すため生成先には残るが `_generated.Add` は実行されない。以後 `RemoveGenerated()` はその要素を取り除けず、テンプレ生成分として管理されないまま生成先に残り続ける。deviation.md 5 番目 (「例外送出後もモデル側コレクションには要素が残る」) と同じ機構による帰結であり spec も例外送出のみを要求しているが、`ItemsSource` 経路では「テンプレ生成分だけを除去する」provenance 追跡の前提が崩れる点が手動配置ケースより影響が広い。

**推奨修正**: `target.Insert` の前に `_generated.Add` する (順序入れ替え) か、`Create` 直後に既配置チェックを行って生成先へ触れる前に落とす。後続フェーズの検討で足りる。

### [🔵 Suggestion] null 要素の扱いが経路によって非対称

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:786-804` (`Items<T>`)、同 `706-753` (`EnsureTreeHasNoDuplicates` / `EnsureCellsHaveNoDuplicates`)

**問題点**: `Cells.Add(null)` は `Items<CellBase>` が非 `CellBase` を黙って落とすため無視されるが、同じ null を含んだまま Reset や接続が走ると `InvalidOperationException("A null cell cannot be placed...")` が飛ぶ。同じ入力に対して発生タイミング次第で無視と例外に分かれる。spec は null に沈黙しているため違反ではない。

**推奨修正**: 構造イベント経路でも null を検出して同じ例外にする (早期に失敗させる) か、両経路とも無視に統一する。決めた側を doc コメントに残すとよい。

### [🔵 Suggestion] 蒸留・ドキュメント側への申し送り 2 件

**該当箇所**: `kasane/concepts/cross/conventions/test-execution.md:15`、`maui/README.md`

**問題点**: 実装側の欠陥ではないが、本変更で確定した情報が長命層・利用者ドキュメントに届いていない。

- `test-execution.md` は「MAUI (`maui/` ビルドルート) は、実際に実行して確かめた時点で追記する」と明記しているが、本変更で `dotnet test KsSettingsView.Maui.Tests/... -f net10.0` (101 件) と 3 TFM ビルド (iOS は `DEVELOPER_DIR` 指定が必要) が確立した。追記の契機に達している
- `maui/README.md` の「構成」「検証ホストの使い方」は Binding と旧検証ホストのみを扱っており、`KsSettingsView.Maui` 本体・ユニットテスト・`tests/KsSettingsView.MauiHost` に触れていない

**推奨修正**: 前者は ksn-distill の concepts 追随で、後者は `docs-refresh` スキル経由で処理する (CLAUDE.md により README 群の直接編集は禁止のため、実装ワーカーが触らなかったのは正しい)。

## アクションプラン

1. **本変更内で対応推奨 (任意)**: Minor 3 の 2 ケース追加 (テスト追加のみで挙動は変わらない)
2. **本変更内で判断が必要**: Minor 1 (`IDisposable` の宣言を外すか、破棄の担当を phase-4 agenda へ引き継ぐか) — オーナー判断
3. **本変更内または直後**: Minor 2 (NU1608) — バージョンを揃えるか、許容理由を README の既知の制約へ回す
4. **蒸留時**: Suggestion 4 の 2 件 (test-execution.md への MAUI 節追加、`docs-refresh` の起票)
5. **後続フェーズ候補**: Suggestion 1・2・3 (処理順の対称化、テンプレ生成経路の例外時整合、null の扱い統一)
