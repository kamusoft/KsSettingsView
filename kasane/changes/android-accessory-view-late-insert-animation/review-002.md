# レビュー結果: android-accessory-view-late-insert-animation (002 回目)

**日付**: 2026-09-17
**判定**: CHANGES_REQUESTED

## サマリー

前回 (review-001) の指摘 6 件と相方レビュー (second-opinion-code-001) の Major 2 件は、iOS の遷移直後フレーム (tasks 1.3 / 6.3、オーナー合意で Simulator 録画へ方針変更済み・撮影進行中) を除いていずれも手当てされている。退役順序と配送中の rebind 競合はどちらも実装が直り、破棄の時点と配送のすれ違いを観測する回帰テストが付いた。3 platform のテストは全件通り、標準 lint も 0 件。

一方で、修正の過程で新しい問題が 1 件入った。受け口を内部 interface へ切り出した結果、`KsSettingsView` の**公開 API 面に配送用のメソッドが増えている** — Kotlin の `internal interface` のメンバは JVM で名前修飾されず、public クラスが実装した override は Kotlin からも Java からも呼べる。design.md は受け口を「モジュール内部の口」と定め、Migration Plan は「公開 API の変更は無い」と書いているため、意図に反した契約の拡大にあたる。これを Major とする。あわせて、前回 Minor-6 (接続失敗時に実体化の口が残る) の残りと、その手当てに付いたテストが当該経路を覆えていないことを指摘する。

## 実行結果

| platform | コマンド | 結果 |
|---|---|---|
| Android | `./gradlew test --rerun-tasks` (`android/`) | 2940 tests / 0 failures / 0 errors (debug + release、`build/test-results/test*UnitTest/TEST-*.xml` 集計) |
| iOS | `xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,name=iPhone 17 Pro Max'` (`ios/`) | 1050 tests / 0 failures (Bridge 166 / Core 88 / SwiftUI 98 / TestSupport 7 / UI 691。バンドル集計行の合算)。`** TEST SUCCEEDED **` |
| MAUI | `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` | 576 tests / 0 failures / 0 skipped |
| lint | `local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` | いずれも禁止 0 件 (comment-policy は 807 ファイル検査) |

tasks.md の差分はチェックボックスのみで、足場アーティファクトの書き換えは無い。未チェックのまま残っているのは 1.3 / 6.3 / 6.5 で、いずれも申告された実態と一致する。

## 照合した規約

- `cross/comment-policy.md` (always)
- `cross/test-execution.md` (テスト実行・結果報告)
- `cross/runtime-behavior-verification.md` (実行時挙動が絡む不具合の完了判定)
- `cross/diagnostic-message-language.md` (`SettingsRootStore` の `Log.e` — 英語・文脈先頭・句点なしで既存の流儀に適合)
- `ios/swift6-language-mode-check.md` (`ios/Sources/` を触る変更の完了判定 — `ios/Package.swift` に一時設定は無い)
- `maui/integration-host-verification.md` (facade 層に触れる変更の end-to-end 疎通 — `evidence/` に 6 枚)

参照した決定: core/ADR-0033 (proposed)・maui/ADR-0027 (proposed)・core/ADR-0005・core/ADR-0006・core/ADR-0019・core/ADR-0020・core/ADR-0023・maui/ADR-0016。proposed の 2 件を根拠にした指摘は出していない。

ロードした code-review スキル: kotlin-impl-skill (android ドメイン)。lessons の `code-review.md` 重点観点 (ミューテーションによる検出力の実測) は、本レビューでは制約によりコードへ一時変更を入れられないため、代わりにコンパイル済みクラスの検査 (`javap`) と失敗経路の到達可能性の追跡で裏を取った。

## 合意済み差分の評価 (依頼分)

### 「表示中の Root の作り直しでは、後片付け待ちの実体を持つ View を事前実体化の対象から外す」

**評価: 妥当。** 以下の 3 点で確認した。

1. **accepted ADR との関係**: maui/ADR-0016 は「旧 wrapper の退役は全経路 (…Root 再構築…) で『Store 更新 → native への配信 → 旧 wrapper 破棄』の順序を守る」「同一 View を再実体化する際は包み直しの前に退役待ち実体を破棄する」を決めている。事前実体化は包み直しであり、配信より前に置くと旧実体の破棄が配信を追い越す。除外はこの accepted 決定を守るために必要な措置であり、逸脱ではない。
2. **proposed ADR との関係**: maui/ADR-0027 が前倒しを求めるのは「Host を作る前に実体化して Store へ届ける」場面であり、表示中の設定ツリーの作り直しは Host 生成を伴わない。除外の対象になるのは `ClearRegistrations()` が退役させた実体を持つ View だけで、この集合は初回接続・再接続では空 (初回は lease 未生成、再接続は `ReleaseHost` が即時破棄する) のため、ADR-0027 が指す場面の挙動は変わらない。
3. **デルタスペックとの関係**: `specs/maui-core/spec.md` の Requirement は「Native Host の最初の表示」を対象とし、「表示中の SettingsView に対して実行時に View を配置・差し替え・解除した場合の反映は…本 Requirement の対象外」と明記している。除外が効くのは表示中の作り直しだけなので、どの Scenario も弱まらない。`specs/maui-cells/spec.md` も同様 (初回・再接続の 3 Scenario はいずれも除外が効かない経路)。

回帰の固定も足りている — `AccessoryViewTests.PreviousViewIsDisposedAfterTheRebuiltRootIsDelivered` と `CustomCellContentTests.PreviousContentIsDisposedAfterTheRebuiltRootIsDelivered` が、破棄の瞬間に `SetRoot` が記録済みであることを数で見ており、破棄が配信より前へ戻れば落ちる。

所見 (指摘ではない): この判断は「合意済み設計からの変更」というより、maui/ADR-0016 の退役順序を守るために maui/ADR-0027 の適用範囲が Host 生成時に限られることの帰結にあたる。ADR-0027 の本文は表示中の作り直しについて沈黙しているので、確定 (accepted) 時に一行足すかどうかはオーナー・propose 側の判断材料になる。

### その他の deviation

- 付随修正 4 件 (`AccessoryTests` の口の改名追随 / `ImplicitStyleTests`・`CustomCellTests` の旧契約を語る記述 / `GatewayScope.Attach` と 39 箇所の削除) は、いずれも本務で触るファイル内・3 ファイル相当の局所変更で、既存テストの通過で担保されている。同梱条件に収まる。
- Android の受け口を内部 `RootAccessoryReceiver` へ切り出し配送元を載せた判断は、配送中の rebind / unbind との競合を塞ぐために必要で、design.md Decision 3 の「モジュール内部の口」の範囲に収まる。ただし実装の形に問題があり、下の Major で指摘する。
- `Disconnect` で口を手放す判断も妥当だが、後片付けが不完全 (下の Minor)。

## 指摘事項

### [🟠 Major] 受け口の internal interface 化で、`KsSettingsView` の公開 API にメソッドが増えている

**該当箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:80` (`RootAccessoryReceiver` の実装宣言)、同 `:648` (`override fun receiveRootAccessoryUpdate`)、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/RootAccessoryReceiver.kt:14`

**問題点**: Kotlin の `internal` はトップレベル宣言では JVM の public として出力され、**メンバ関数の名前修飾は行われない**。`internal interface` のメンバを public クラスが override すると、その override は Kotlin のメタデータ上も public として記録される。ビルド成果物で確認した:

```
public final class jp.kamusoft.kssettingsview.ui.KsSettingsView extends android.widget.FrameLayout
        implements jp.kamusoft.kssettingsview.ui.RootAccessoryReceiver {
  public void receiveRootAccessoryUpdate(SettingsRootStore, SettingsRootDiff$UpdateAccessory);
```

同じ成果物で `SettingsRootStore` 側の口は正しく隠れている (`registerRootAccessoryReceiver$jp_kamusoft_kssettingsview_release` と名前修飾されている) ため、露出しているのは Host 側だけである。引数の型 (`SettingsRootStore` / `SettingsRootDiff.UpdateAccessory`) も public なので、モジュール外の Kotlin からも Java からも `view.receiveRootAccessoryUpdate(store, diff)` が呼べる。

これは 3 点で問題になる。

- design.md Decision 3 と core/ADR-0033 は受け口を「モジュール内部の口」と定めている。公開されると、内部の配送路が利用者の契約として固定される
- design.md Migration Plan は「公開 API の変更は無く、利用者の移行は不要」と書いているが、実際には公開クラスにメンバが 1 件増えている (HEAD の `KsSettingsView` には同名のメンバが無い)。配布物の API 面の変化は後から取り下げにくい
- 利用者がこれを呼ぶと、現在結び付いている Store と一致しないかぎり**黙って何も起きない**。公開された入口としては挙動が説明できない

**推奨修正**: `KsSettingsView` 自身に実装させず、受け口を Host が private に持つ小さな実装オブジェクトへ移し、そこから Host の名前修飾される内部メンバへ委譲する (Store が弱参照するのはその実装オブジェクト、Host はそれを強参照、実装オブジェクトは Host を強参照 — 外部から Host への参照が切れれば両方まとめて回収されるため、`RootAccessoryReceiverReleaseTest` の保証は変わらない)。あるいは受け口の形を戻し、Store が `KsSettingsView` の internal メンバを直接呼ぶ形にする (前回の Suggestion は必須ではないと明記しており、公開面を広げてまで採る必要は無い)。いずれの形でも、`javap` で `KsSettingsView` に未修飾の公開メンバが増えていないことを確認してほしい。

### [🟡 Minor] 接続の失敗が実体化より後で起きると、作られた実体が後片付けされないまま次の接続へ残る

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:223`-`237` (`Disconnect`)、同 `:1087` (`RebuildRoot` の `MaterializeTreeViews`)、`maui/KsSettingsView.Maui.Tests/HandlerTests.cs:97` (`AFailedInitialConnectReleasesTheMaterializationSeam`)

**問題点**: `Disconnect()` の後片付けは `ClearRegistrations()` に依存しており、それが退役させるのは `_sectionEntries` / `_cellEntries` に載っている Section・Cell の分だけである。`MaterializeTreeViews` は**登録より前**に実体を作るため、`MaterializeTreeViews` 自体 (利用者の View の Handler 生成が失敗する場合) や `_gateway.SetRoot` が例外を投げると、その時点までに作られた lease は `_accessories` / `_cellContents` の `Lease` に残ったまま `Disconnect()` を通り抜ける。`DisposeRetired()` も対象を `_retiredViews` から取るので届かない。

結果として (a) 実体と Handler が切られないまま残り、Android では Context を掴み続ける、(b) 次の接続で `Materialize` が「実体があるなら作らない」判定に入るため、前の世代の実体がそのまま新しい Host へ配信される。これは `Disconnect` に書かれた「その実体は次の接続でも作り直されないまま新しい Host へ渡ってしまう」という、この修正が塞いだはずの窓そのものである。`ReleaseHost()` 側は `_accessories` / `_cellContents` を直接走査する (`ReleaseAccessoryViews` / `ReleaseCellContentViews`) ため同じ穴が無く、両者の後片付けの土台が揃っていない。

あわせて、この手当てに付いた `AFailedInitialConnectReleasesTheMaterializationSeam` は上の経路を覆えていない。テストが作る失敗 (同一 View の多重配置) は `RebuildRoot` の先頭にある `EnsureTreeHasNoDuplicates` が木全体を先に走査して弾くため、`MaterializeTreeViews` へ到達する前に例外になる。そのため `Assert.That(views.Leases, Is.Empty)` は「実体化の口が残っていない」ことは示すが、「失敗までに作られた実体が片付いている」ことは示さない。

**推奨修正**: `Disconnect()` の後片付けを `ReleaseHost()` と同じ土台にする (置き場所の対応表を直接走査して lease を破棄する)。テストは、実体化の途中で失敗する状況 — 例えば 2 件目以降の `Materialize` で例外を投げる fake materializer — を作り、失敗した接続の後に残った lease が破棄済みであること、その後の再接続で実体が作り直されることまで見る形にしてほしい。

### [🔵 Suggestion] 配送の握り潰しが `CancellationException` まで飲み込む

**該当箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt:361`-`362`

**問題点**: `catch (error: RuntimeException)` は `CancellationException` (JVM では `RuntimeException` の派生) も捕まえる。`updateAccessory` は公開 API で、利用者のコルーチンの中から呼ばれ得るため、受け口の中で協調的キャンセルが起きた場合に構造化並行性の合図が消える。現在の受け口の実装 (値を控えて `Handler` へ送るだけ) は中断点を持たないので実害は無く、登録できるのもモジュール内部の実装だけなので、優先度は低い。

**推奨修正**: 必須ではない。`catch (error: CancellationException) { throw error }` を先に置くか、捕捉対象を明示的な例外型へ絞る。

## 前回指摘の解消状況

| 出所 | 指摘 | 状況 |
|---|---|---|
| review-001 Major | iOS の遷移直後フレーム (tasks 1.3 / 6.3) | **未解消 (合意済み・進行中)**。オーナー合意で Simulator 録画を先に取る方針へ変更し `deviation.md` に記録済み。`evidence/README.md` の「1.3 (iOS 実機) について」は今も詳細を実装報告へ預けているので、Simulator の証跡を足すときに自己完結する記述へ直してほしい |
| review-001 Minor-2 | `RegisterSection` / `RegisterCell` のコメントが新しい順序と食い違う | **解消**。実行時の挿入経路の役割を現在形で述べる形に書き直されている (`KsSettingsController.cs:2171`・`:2208`) |
| review-001 Minor-3 | Host 保証の doc が Bridge の一部 API にしか無い | **解消**。Android の `updateAccessoryView`、iOS の `updateAccessory` / `updateAccessoryView` に取り付け前の保証が入った |
| review-001 Minor-4 | 空になった `GatewayScope.Attach()` と 39 箇所の呼び出し | **解消**。削除され、`ConnectFacade` / `CreateHost` に役割が割り直された。deviation に記録あり |
| review-001 Minor-5 | 反映失敗の切り分けに対応するテストが無い | **解消**。`RootAccessoryDeliveryFailureTest` が、失敗する受け口を先に並べた状態で後続 Host の受け取りと `diffs` の発行 1 回を固定している |
| review-001 Minor-6 | 接続失敗後も実体化の口が残る | **部分解消**。`Disconnect` で `_views` / `_images` を手放すようになったが、失敗までに作られた実体の後片付けが残っている (上の Minor) |
| review-001 Suggestion | Store が Host の具象型に依存 | **解消**。内部 interface `RootAccessoryReceiver` へ切り出された。ただし公開面の副作用あり (上の Major) |
| second-opinion Major-1 | Root 再構築で旧 lease の破棄が配信より先 | **解消**。事前実体化から後片付け待ちを除外し、破棄の時点を見るテスト 2 件が付いた |
| second-opinion Major-2 | 配送中の rebind / unbind と競合すると旧 Store の値を受理 | **解消**。知らせに配送元を載せ、受理判定と binding の差し替えを同じロックで扱う形になった。`RootAccessoryRebindRaceTest` が barrier で配送を止めて競合を再現している |
| second-opinion Major-3 | iOS 実機の修正前後確認が未完了 | review-001 Major と同一。上と同じ扱い |

## アクションプラン

1. Major: `KsSettingsView` の公開 API 面から配送用メソッドを外す (受け口を private な実装オブジェクトへ移すか、受け口の形を戻す)。直したら `javap` で公開メンバが増えていないことを確認する
2. Minor: `Disconnect()` の後片付けを `ReleaseHost()` と同じ土台にし、実体化の途中で失敗する状況を作るテストへ差し替える
3. 進行中: iOS Simulator の 1.3 / 6.3 証跡を `evidence/` へ足し、`evidence/README.md` の 1.3 の記述を自己完結する形へ直す
4. Suggestion: `CancellationException` の再スロー (任意)
