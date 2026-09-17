# レビュー結果: android-accessory-view-late-insert-animation (001 回目)

**日付**: 2026-09-17
**判定**: CHANGES_REQUESTED

## サマリー

デルタスペック 4 本 (android-host / ios-host / maui-core / maui-cells) の Requirement と Scenario は、いずれも実装とテストに対応が付いている。Android の同期の受け口 (Store → Host、弱参照・Host ごと 1 件・Root 対象は受け口だけが反映) は core/ADR-0033 の決定どおりに実装され、二重適用の防止も Adapter の通知回数で固定されている。MAUI 側の実体化の前倒しも maui/ADR-0027 の順序 (口の差し込み → 実体化 → 配信データ → Host 生成 → Root 適用) に一致し、3 platform のテストは全件通る。

一方で、本 change の設計自体が「iOS では実環境でしか分からない」とした退行リスク (自己計測 wrapper の幅未確定時の計測機会増) の確認が未了で、tasks 6.3 は不具合が出たら完了を止める条件付きゲートになっている。手元で実機録画が取れないことは確認の断念理由にならない — handbook の「実行時挙動の検証規約」は実環境に Simulator を含めており、本 change でも 6.4 の iOS 側は Simulator で確認済みなので、同じ手段で 6.3 を満たせる。これを最優先の指摘とし、あわせてコメント・doc の追随漏れ (4.8 / 2.6 / 3.5 の網羅漏れ) と、接続失敗経路で実体化の口が残る新しい穴を指摘する。

## 実行結果

| platform | コマンド | 結果 |
|---|---|---|
| Android | `./gradlew test --rerun-tasks` (`android/`) | 2934 tests / 0 failures / 0 errors (debug + release、`build/test-results/test*UnitTest/TEST-*.xml` 集計) |
| iOS | `xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` (`ios/`) | 1050 tests / 0 failures (Bridge 166 / Core 88 / SwiftUI 98 / TestSupport 7 / UI 691。バンドル集計行の合算) |
| MAUI | `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` | 572 tests / 0 failures / 0 skipped |
| lint | `local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` | いずれも禁止 0 件 (comment-policy の要確認 167 件はすべて既存分。後述 Minor-2 は規約本文側の判定) |

`ios/Package.swift` に Swift 6 言語モードの一時設定は残っていない (tasks 3.4 の到達状態を満たす)。tasks.md の差分はチェックボックスのみで、足場アーティファクトの書き換えは無い。

## 照合した規約

- `cross/comment-policy.md` (always)
- `cross/test-execution.md` (テスト実行・結果報告)
- `cross/runtime-behavior-verification.md` (実行時挙動が絡む不具合の完了判定)
- `cross/diagnostic-message-language.md` (本体コードへの診断ログ追加 — `SettingsRootStore` の `Log.e`)
- `ios/swift6-language-mode-check.md` (`ios/Sources/` を触る変更の完了判定)
- `maui/integration-host-verification.md` (facade 層に触れる変更の end-to-end 疎通)

参照した決定: core/ADR-0033 (proposed)・maui/ADR-0027 (proposed)・core/ADR-0005・core/ADR-0019・core/ADR-0020・core/ADR-0023・maui/ADR-0016。proposed の 2 件は決定ではないため、これを根拠にした指摘は出していない (いずれも実装は記載どおり)。

## 指摘事項

### [🟠 Major] iOS の遷移直後フレームの確認は Simulator で満たせる — 実機録画が取れないことは断念の理由にならない

**該当箇所**: `kasane/changes/android-accessory-view-late-insert-animation/tasks.md` 1.3 / 6.3、`evidence/README.md`「1.3 (iOS 実機) について」

**問題点**: 本 change は iOS の実体化タイミングを前倒しするため、design.md の Risks が「自己計測 wrapper が幅未確定のまま測られる機会が増え、最初のフレームで高さがぶれるかは実機でしか分からない」とし、tasks 6.3 は退行が出た場合に完了を止めることを条件として書いている。現状この確認は未了で、evidence には iOS の遷移直後フレームが 1 枚も無い。`cross/runtime-behavior-verification.md` は「実環境とは実機・エミュレータ/Simulator と実物の依存先の組み合わせ」と定義しており、Simulator は実環境として認められている。本 change でも tasks 6.4 の iOS 側は Simulator で確認済み (`evidence/README.md`「6.4 Native Sample の退行確認」) で、Simulator の画面録画は `xcrun simctl io <udid> recordVideo` で取得できる。したがって「iOS 実機の画面録画手段が無い」ことは、このゲートを空けたまま完了にする根拠にならない。

加えて、`evidence/README.md` は 1.3 の詳細を「実装報告に記す」として外部へ預けている。証跡は change と一緒にアーカイブされ、実装報告は残らないため、この記述だけでは後から未了の理由と再開条件を追えない。

**推奨修正**: iOS Simulator で 1.1 / 1.2 と同じ操作・同じ t=0 の決め方で修正前ビルドと修正後ビルドの連続フレームを撮り、`evidence/` へ 1.3 / 6.3 の証跡として残す (折り返す Label を持つ FooterView・CustomCell の Content を含む)。実機でしか判定できない項目が残ると判断する場合は、その項目と理由を `evidence/README.md` に自己完結する形で書き、オーナーへの申し送りとして明示する。Simulator で高さの変動が観測された場合は tasks 6.3 の指示どおり完了を止めてオーナーへ報告する。

### [🟡 Minor] 輸送 DTO と実体化の前後関係を語るコメントが、新しい順序と食い違う

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:2123` (`RegisterSection`)、同 `:2159` (`RegisterCell`)

**問題点**: どちらも「輸送 DTO は実体化前に組み立てられるため、accessory の View はここで実体化して送り直す」と書いているが、設定ツリー全体の配信では `MaterializeTreeViews` が配信データを組む前に実体化を済ませるようになったため、この前提は成り立たない。さらに `PlaceAccessoryView` / `PlaceCellContent` は同じ View が既に置かれていれば実体を確かめるだけで**送り直さない**ので、後半も現在の挙動と合わない (送り直しが起きるのは Section / Cell を実行時に挿入した経路だけ)。tasks 4.8 が求める「新しい順序へのコメント追随」の取りこぼしにあたり、`cross/comment-policy.md` の「現在の仕様を現在形で書く」にも反する。

**推奨修正**: 2 箇所のコメントを、現在の役割 (実行時の挿入では輸送 DTO の後に実体化して送り、既に実体がある場合は確かめるだけで送り直さない) を現在形で述べる形に書き直す。

### [🟡 Minor] 新しい Host 保証の doc コメントが Bridge の一部 API にしか入っていない

**該当箇所**: `android/kssettingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsSettingsBridge.kt:361`-`363` (`updateAccessoryView` の doc)、`ios/Sources/KsSettingsViewBridge/KsSettingsBridge.swift:257`-`259` (`updateAccessory` の doc) および同 `:297`-`299` (`updateAccessoryView` の doc)

**問題点**: Android の `updateAccessory` (text 版) には「生成済みの Host に対しては、view 階層への取り付け前に渡した root 対象の値も失われず、最初の表示に含まれる」が追記されたが、View 版には入っていない。iOS では `makeHostViewController` / `releaseHost` にだけ追記され、`updateAccessory` / `updateAccessoryView` は「引き継ぐ場合は呼び出し側が値を保持して再適用する」だけのままである。本 change が解消した症状の主役は View 版であり、利用者が最初に読むのはこれらの更新 API の doc であるため、保証が最も要る場所に書かれていない。tasks 2.6 / 3.5 の網羅漏れ。

**推奨修正**: Android の `updateAccessoryView` と iOS の `updateAccessory` / `updateAccessoryView` にも、取り付け (view load) 前に渡した root 対象の値が失われない旨を、Android の text 版と同じ表現で加える。公開 doc コメントなので ADR ID は入れない。

### [🟡 Minor] 空になった `GatewayScope.Attach()` が、意味のない呼び出しを 39 箇所に残している

**該当箇所**: `maui/KsSettingsView.Maui.Tests/Fakes/GatewayScope.cs:99`-`102`

**問題点**: `Attach()` の本体はコメント 1 行だけの空実装になり、テスト側には 39 箇所の呼び出しが残っている。呼び出し側からは「取り付けを再現している」ように読めるが実際には何も起きないため、後から読む人が前後関係を誤読する。取り付けの通知で何も起きないことは `TheAttachNotificationDoesNotRedeliverViewAccessories` / `TheAttachNotificationDoesNotRedeliverContent` が Handler の実経路 (`OnHostAttached`) で固定済みであり、fake 側に空の操作を残す必要はない。

**推奨修正**: `Attach()` と 39 箇所の呼び出しを削除する (取り付けの検証は Handler 経路のテストに集約する)。まとめて消すのが本 change の範囲を超えると判断する場合は、残す理由を含めてオーナーへ諮る。

### [🟡 Minor] 反映失敗の切り分け (spec の SHALL NOT) に対応するテストが無い

**該当箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt:354`-`364` (`deliverRootAccessory` の `catch` は 360 行目)、`specs/android-host/spec.md`「ある Host での反映の失敗は `updateAccessory` の呼び出し元へ伝わらず、他の Host の受け取りと Store の通知の発行を妨げない」

**問題点**: この規範文には Scenario が無く、tasks 2.1 の列挙にも含まれていないため、`catch (error: RuntimeException)` の経路を通すテストが 1 件も無い。結果として「失敗を握り潰して他へ配り続ける」という、利用者から見える契約が回帰検出の外にある。握り潰しの実装は入れ違えても他のテストが落ちないため、テストが無いこと自体が主要なリスクになる。

**推奨修正**: 1 件目の Host の反映で例外が起きても、`updateAccessory` の呼び出しが例外を投げず、2 件目の Host が値を受け取り、`diffs` への emit も起きることを確認するテストを追加する (受け口は internal なので、反映で例外を投げる状況を作れる Host か、受け口の呼び出しを差し替えられる形が要る)。テストが書けない形だと判断する場合は、spec の規範文の扱いをオーナーへ諮る。

### [🟡 Minor] 接続に失敗した後も実体化の口が残る

**該当箇所**: `maui/KsSettingsView.Maui/SettingsView.cs:983` (`Controller.AttachViews(views)` の位置)、`maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:219`-`228` (`Disconnect`)

**問題点**: 実体化の口の差し込みが `Connect` より前へ移った結果、初回接続で `Connect` が例外を投げた場合 (重複した Section / Cell を含むツリーなど、利用者の入力で到達し得る) に `Disconnect()` が走っても `_views` が残り、gateway が無いまま実体化だけができる状態になる。この状態で View を置くと platform の実体 (Handler) が Host 不在のまま作られ、その lease は次の接続でも `Materialize` の「実体があるなら作らない」判定に引っかかって作り直されないため、前の世代の実体が新しい Host へ輸送され得る。従来は口の差し込みが `Connect` の後だったため、この窓は無かった。

**推奨修正**: `Disconnect()` で `_views` も手放す (画像の解決口と同じ扱いにする)。または `AttachViews` の呼び出しを接続が成立した後に寄せ、失敗時に口が残らないようにする。

### [🔵 Suggestion] Store が Host の具象型に依存している

**該当箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt:97`-`98`・`335`・`342`

**問題点**: 受け口の登録が `WeakReference<KsSettingsView>` と `registerRootAccessoryReceiver(host: KsSettingsView)` の形で、状態管理の Store から表示の具象クラスへ直接の依存が入っている。design.md が求めるのは「結び付いている Host へ同期に知らせる受け口 (モジュール内部の口)」であり、具象型の指定ではない。モジュール内部の `interface` を挟めば、Store 単体で配送・弱参照・登録重複のテストが書ける (上の Minor の失敗切り分けテストもこの形なら素直に書ける)。

**推奨修正**: 必須ではない。上のテスト追加とあわせて内部 interface 化を検討する。

## アクションプラン

1. Major: iOS Simulator で 1.3 / 6.3 の連続フレーム証跡を取り、`evidence/` へ残す。退行が出たら完了を止めてオーナーへ報告する。実機でしか判定できない残件があるなら `evidence/README.md` に自己完結する形で書く
2. Minor-6: `Disconnect()` で実体化の口を手放す (接続失敗時に口が残る窓を塞ぐ)
3. Minor-2 / Minor-3: `KsSettingsController` の 2 箇所のコメントと、両 OS の Bridge の更新 API の doc を新しい保証・新しい順序へそろえる
4. Minor-5: 反映失敗の切り分けのテストを追加する (書けない形ならオーナーへ相談)
5. Minor-4: `GatewayScope.Attach()` と 39 箇所の呼び出しを削除する
6. Suggestion: 受け口の内部 interface 化は 4 とあわせて検討する
