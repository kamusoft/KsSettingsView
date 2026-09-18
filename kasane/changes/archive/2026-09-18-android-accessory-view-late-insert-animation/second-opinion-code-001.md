# セカンドオピニオン: android-accessory-view-late-insert-animation (code-001)
**相方**: codex / **label**: so-code-android-accessory-view-late-insert-animation / **日付**: 2026-09-17 / **対象**: 作業ツリーの未コミット差分 (android/・ios/・maui/ の製品コードとテスト、ベース HEAD)
---
# レビュー結果: android-accessory-view-late-insert-animation

**日付**: 2026-09-17  
**判定**: **CHANGES_REQUESTED**

件数: Critical 0 / Major 3 / Minor 0 / Suggestion 0

## 指摘事項

### [🟠 Major] Root 再構築時に、Native が参照中の旧 View lease を配信前に破棄する

**該当箇所**: [maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1079]、[同:581]

**問題点**: `RebuildRoot()` は次の順で処理します。

1. `ClearRegistrations()` で現在表示中の lease を退役待ちへ移す
2. `MaterializeTreeViews()` で同じ View を再実体化する
3. `gateway.SetRoot()` で新しい設定ツリーを Native へ配信する

同じ View を再利用する場合、2 の `Materialize()` / `MaterializeContent()` が `DisposeRetiredViewsOf()` を呼び、旧 lease を3の配信より前に破棄します。この時点では Native Host がまだ旧 wrapper を保持しています。

これは accepted の maui/ADR-0016 が要求する「Native への配信後に旧 wrapper を破棄する」という退役順序に反します。瞬間的な空表示や切断済み Handler の参照を生じ得るほか、`SetRoot()` が失敗するとNative側に破棄済み wrapperが残ります。

既存テストは再構築後の新 Handler が接続済みであることだけを確認しており、破棄時点で `SetRoot` が完了済みかは検査していません。

**推奨修正**: 初回 Host 用の事前実体化と、表示中の Root 再構築を分離してください。表示中の再構築では、同一 View の lease を対応付け直して維持するか、Native の旧参照を外す配信を完了してから破棄・再実体化してください。`FakeViewLease.OnDispose` から `GatewayCall.SetRoot` が既に記録済みであることを検査する回帰テストも追加してください。

### [🟠 Major] 別 Store への rebind / unbind と配送中の更新が競合すると、旧 Store の値を受理する

**該当箇所**: [android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:631]、[SettingsRootStore.kt:354]

**問題点**: Store からのコールバックには配送元 Store や登録世代が含まれず、`receiveRootAccessoryUpdate()` は届いた更新を無条件に受理します。

例えば以下の競合が成立します。

1. 別スレッドの Store A が弱参照から Host を取得する
2. メインスレッドで Host を Store B へ rebindし、Aの登録解除と pending破棄を行う
3. Store A の配送が再開し、Host がAの値を現在の更新として受理する

`unbind()` に対しても同じ競合があります。これは「別 Store へ bind 後は以前の Store の値を反映しない」「unbind 後は反映しない」と、任意スレッドからの更新を組み合わせた場合に違反します。追加テストは rebind 完了後に旧 Store を呼ぶ逐次ケースだけで、配送途中の競合を覆っていません。

**推奨修正**: 登録ごとの世代トークン、または配送元 Store をコールバックに含め、Host 側で現在の binding と一致する更新だけを受理してください。binding の差し替え・pending 更新・受理判定を同じロックまたは同等の同期境界で扱い、Store A の配送を barrier で停止中に rebind / unbind してから再開する競合テストを追加してください。

### [🟠 Major] iOS 実機の修正前後確認が未完了で、実行時挙動の完了条件を満たしていない

**該当箇所**: [tasks.md:11]、[tasks.md:49]

**問題点**: tasks 1.3 と 6.3 が未完了です。今回の変更は、最初の描画フレーム、後着、wrapper の初回計測、高さ変動という実行時タイミングそのものを変更します。Simulator のテストや IntegrationHost の静止画は、iOS 実機での最初のフレームと高さ安定性を保証しません。

これは `runtime-behavior-verification.md` の「修正前の実環境観測」「同一手順による修正後確認」「証跡」の完了ゲートに該当します。

**推奨修正**: 段取り中のオーナー録画で1.3と6.3を完了し、同じ操作・フレーム基準で比較できる証跡を保存してください。それまではコード修正後もchange完了・APPROVEDにはできません。

## 照合結果

- `deviation.md` の2件は合意済み付随修正として扱い、違反にはしていません。
- tasks 6.5 は但し書きどおり、公開パッケージへのリリース後確認として申し送れば問題ありません。
- 提示されたテスト結果（Android 2934件、iOS 1050件、MAUI 572件、すべて失敗0）、lint 0件、Swift 6 error 0件を前提にしました。依頼どおり再実行していません。
- Androidの修正前後証跡は説明と整合し、目視した画像に公開上問題となる個人情報は見当たりませんでした。

## アクションプラン

1. MAUI のRoot再構築におけるlease退役順序を修正し、破棄時点を検査するテストを追加する。
2. Android のreceiver登録にStore識別または世代トークンを導入し、rebind / unbind競合テストを追加する。
3. iOS実機の1.3・6.3を完了して証跡を残す。
4. 修正後に独立再レビューを実施する。


## 突き合わせ結果

突き合わせ相手: review-001.md (ホスト側、CHANGES_REQUESTED)

- **確定** (双方一致): iOS の遷移直後フレーム確認 (tasks 1.3 / 6.3) が未了 — Major。ホスト側は `cross/runtime-behavior-verification.md` が実環境に Simulator を含める点を併せて指摘。tasks は実機を名指ししているため、Simulator で代替するかはオーナー判断
- **採用** (相方のみ・根拠強): MAUI の Root 再構築で旧 View lease の破棄が Native への配信より先になる (maui/ADR-0016 の退役順序) — Major
- **採用** (相方のみ・根拠強): Android で別 Store への rebind / unbind と配送中の更新が競合すると旧 Store の値を受理する — Major
- **降格**: なし
- **未解決**: なし (両レビューに矛盾する指摘は無い)
