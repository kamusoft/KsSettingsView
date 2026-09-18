# レビュー結果: android-accessory-view-late-insert-animation (004 回目)

**日付**: 2026-09-18
**判定**: APPROVED

## サマリー

修正サイクル 3 で入った 2 件 (相方レビュー second-opinion-code-003 の Major「Native Host の生成完了までの失敗にロールバックが無い」と Minor「接続失敗時の一括破棄が最初の例外で中断する」) は、いずれも塞がれている。`CreatePlatformView` が Host 生成・Root の適用・親子関係の登録を 1 つの失敗単位として囲い、失敗時は親子関係の解消と Host 世代の解放を両方試みる形になった。後片付け自体の失敗は元の失敗を先頭に置いた集約例外になり、この経路ごとに回帰テストが 5 件付いている。

今回はじめて対象に入れた `evidence/` と `samples/maui` も確認した。`evidence/README.md` が参照する PNG 41 枚はすべて実在し、逆に参照の無い PNG も無い。修正前後の Android フレーム 2 枚を実際に開いて内容を突き合わせ、README の記述と一致することを確かめた。review-002 のアクション 3 (1.3 の記述の自己完結) も解消している。

3 platform のテストは全件成功 (Android 2942 / iOS 1050 (5 バンドル合算) / MAUI 584)、標準 lint は禁止 0 件。コード側の指摘は Minor 1 件 (優先度低) と Suggestion 2 件で、いずれも本サイクルの修正が新たに壊したものではない。

ただし**変更の完了条件は未充足のまま**である — tasks 1.3 / 6.3 のうち iOS 実機 (iPhone 11) の観測と 6.5 が開いており、これは申告どおり既知の未完了として扱った (下の「完了条件の未充足」)。

## レビュー対象

- 作業ツリーの未コミット差分すべて (ベース HEAD = develop)。追跡済み 29 ファイル + 未追跡の Kotlin 5 ファイル + `evidence/` (README.md と PNG 41 枚) + `deviation.md`
- 前回 (review-003) 以降に動いたのは 6 ファイル: `maui/KsSettingsView.Maui/Handlers/SettingsViewHandler.cs` / `Handlers/SettingsViewHandler.Standard.cs` / `Internals/KsSettingsController.cs`、`maui/KsSettingsView.Maui.Tests/HandlerTests.cs` / `Fakes/RecordingHostContainment.cs` / `Fakes/FakeViewMaterializer.cs`。それ以外は review-003 時点と同一であることを確認したうえで、Android / iOS / MAUI の差分全体を読み直した
- `tasks.md` の差分はチェックボックスのみ。足場アーティファクト (proposal / design / specs) の書き換えは無い。未チェックで残るのは 1.3 / 6.3 / 6.5 で、申告された実態と一致する

## 実行結果

| platform | コマンド | 結果 |
|---|---|---|
| Android | `./gradlew test --rerun-tasks` (`android/`) | 2942 tests / 0 failures / 0 errors / 0 skipped (`build/test-results/test*UnitTest/TEST-*.xml` 240 ファイルの集計)。`BUILD SUCCESSFUL` |
| iOS | `xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` (`ios/`) | **合計 1050 tests / 0 failures**。`** TEST SUCCEEDED **` (exit 0) |
| MAUI | `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` | 584 tests / 0 failures / 0 skipped |
| lint | `scripts/local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` | いずれも禁止 0 件 (comment-policy は 807 ファイル検査) |

iOS はバンドル集計行の合算である (前回のレビューは UI バンドル 1 本分しか報告していなかった):

| テストバンドル | 件数 |
|---|---|
| `KsSettingsViewBridgeTests.xctest` | 166 |
| `KsSettingsViewCoreTests.xctest` | 88 |
| `KsSettingsViewSwiftUITests.xctest` | 98 |
| `KsSettingsViewTestSupportTests.xctest` | 7 |
| `KsSettingsViewUITests.xctest` | 691 |
| **合計** | **1050** |

前回比で MAUI が +5 (579 → 584)。いずれも本サイクルの修正に対応する新規テストである。Android / iOS は増減なし。

`comment-policy-lint.py --advisory` の要確認は、今回の差分が新たに持ち込んだ公開 doc コメント内の ADR 参照を含まない。報告される `KsSettingsController.cs:376` / `:416` / `:435` / `:455` は `internal sealed class` のメンバであり実際には公開 API ではない (検査の可視性推定の限界)。`SettingsView.cs` の 3 件と `Platforms/Android/SettingsViewHandler.cs:45` は本 change 以前からある行である。

## 照合した規約

- `cross/comment-policy.md` (always) — 新規コメントについて許容参照・禁止参照・禁止する記述類型・公開 doc コメントの各節を照合。ADR ID を置いたのは非公開の実装側コメント (`SettingsViewHandler.cs:73`-`75` の行コメント) と internal 型の doc に限られ、違反なし
- `cross/test-execution.md` (テストを実行・報告するとき) — iOS はバンドル集計行だけを合算、Android は `--rerun-tasks` と XML 集計、MAUI は末尾集計行。新規 Android テストの待機はいずれも条件ベース (`awaitConvergence` / `awaitDifferCommit` / deadline 付きの `awaitCollected`) で、固定時間の収束待ちは無い
- `cross/runtime-behavior-verification.md` (実行時挙動が絡む不具合の完了判定) — 下の「証跡の確認」「完了条件の未充足」に関係する
- `cross/sample-parity.md` (`samples/` のデモ画面・文言を変更するとき) — `AccessoryViewsDemoPage` は handbook が名指しで「MAUI のみの画面」と定めた例外に当たるため、③ の追加に他 platform への追随義務は生じない。番号の繰り下げはページ内で閉じており、`.xaml` と `.xaml.cs` の文言も揃っている
- `cross/diagnostic-message-language.md` (開発者向け文字列の追加) — 新規の `InvalidOperationException` 3 件はいずれも英語・現在形で、既存の流儀に適合
- `ios/swift6-language-mode-check.md` (`ios/Sources/` を触る変更の完了判定) — `ios/Package.swift` に一時設定は無く、テストは警告なく成功
- `maui/integration-host-verification.md` (facade 層に触れる変更の end-to-end 疎通) — 証跡 4 枚 + MauiHost 2 枚が `evidence/` にあり、README が規約の表どおりの内容を記述している

参照した決定: core/ADR-0033 (proposed)・maui/ADR-0027 (proposed)・core/ADR-0005・core/ADR-0019・core/ADR-0023・maui/ADR-0016・maui/ADR-0020。proposed の 2 件を根拠にした指摘は出していない。

ロードした code-review スキル: kotlin-impl-skill (android ドメイン)。`kasane/lessons/code-review.md` の重点観点 (ミューテーションによる検出力の実測) は、制約によりコードへ一時変更を入れられないため実施していない。代わりに新規テスト 5 件それぞれについて「この修正を戻したときにどのアサーションが落ちるか」を静的に追い、いずれもトートロジーでないことを確認した (下記)。

## 前回指摘の解消状況

| 出所 | 指摘 | 状況 |
|---|---|---|
| second-opinion-code-003 Major | Native Host の生成完了までの失敗 (Host 生成・Root の実体化と配信・親子関係の登録) にロールバックが無い | **解消**。下に検証内容 |
| second-opinion-code-003 Minor | 接続失敗時の一括破棄が最初の `Dispose` 例外で中断する | **解消**。下に検証内容 |
| review-002 アクション 3 | `evidence/README.md` の 1.3 の記述を自己完結する形へ直す | **解消**。下の「証跡の確認」 |
| review-003 Suggestion 1 | 公開 API 面の機械検査 (binary-compatibility-validator) が無い | **未対応 (任意)**。現在の形は正しいままで、機械検査の導入可否は引き続きオーナー・propose 側の判断 |
| review-003 Suggestion 2 | 蒸留時に core/ADR-0033 へキャンセルの合図の例外を書き足すか判断する | **未対応 (蒸留時の項目)**。実装側の対応は不要 |
| review-001 Major / second-opinion-code-001 Major-3 | iOS の遷移直後フレーム (tasks 1.3 / 6.3) | **一部解消**。Simulator の修正前後は `evidence/` に揃い、6.3 の合否条件も Simulator 分は判定済み。実機 (iPhone 11) 分は未完了 (合意済み・進行中) |

### ロールバックの検証 (Major)

失敗単位は `maui/KsSettingsView.Maui/Handlers/SettingsViewHandler.cs:68`-`86` で閉じている。`CreateHost()` (platform 実装では `ConnectGateway` → `MakeHost` まで含む)・`VirtualView.ApplyRootAccessories()`・`Containment?.AddToParent()` が 1 つの `try` に入り、どこで落ちても `RollbackHost` (同 `:133`-`156`) を通る。手放す手順は切断時 (`DisconnectHandler`) と同じ「親子関係の解消 → `Containment = null` → `VirtualView.ReleaseHost()`」で、2 つの後片付けは個別に `try` で囲まれているため、片方が投げてももう片方は試みられる。

相方が挙げた 4 つの失敗点の到達性を追った。`Controller.AttachInteractions()` / `AttachImages()` と `MakeHost()` の例外・null は `ConnectGateway` の内側または直後、すなわち platform の `CreateHost()` の中にあり、`ApplyRootAccessories()` と `AddToParent()` は明示的に `try` の中にある。囲えていない失敗点は残っていない。

再試行が旧世代を使い回す経路も塞がれている。`ReleaseHost()` が `_views = null` と置き場所のリース退役を行うため、やり直しは `ConnectGateway` の再接続分岐へ入って新しい実体化の口で作り直される。テストがこれを経路ごとに固定している:

- `HandlerTests.cs:272`-`302` (`AFailedRootAccessoryApplicationReleasesTheHostGeneration`) — Root footer の実体化で失敗させ、先に作った header のリースが破棄されること・`ReleaseHostCount` が 1 になること・やり直すと新しい口で両方が実体化されて配信に載ることまで見ている
- `HandlerTests.cs:306`-`322` (`AFailedHostCreationReleasesTheHostGeneration`) — Host 生成そのものの失敗。`Containment.Steps` が `["Remove"]` になる
- `HandlerTests.cs:326`-`344` (`AFailedContainmentRegistrationReleasesTheHostGeneration`) — 登録の失敗。`Steps` が `["AddToParent", "Remove"]` になる
- `HandlerTests.cs:352` (`AFailedRollbackKeepsTheOriginalHostFailure`) — 後片付け自体が失敗しても、集約例外の先頭が元の失敗であり、かつ `ReleaseHostCount` が 1 になる (親子関係の解消の失敗で Host の解放が飛ばされない)

検出力の確認 (静的): ロールバックを外せば 1 件目の `IsDisposed` が false、2・3 件目の `Steps` からは `"Remove"` が消え、4 件目は `ReleaseHostCount` が 0 になる。どれも前提アサーションではなく争点のアサーションが落ちる形である。

Host 生成の失敗を踏ませるための `internal bool FailsToCreateHost` (`maui/KsSettingsView.Maui/Handlers/SettingsViewHandler.Standard.cs:22`) は、Native Host を持たない TFM では生成が失敗し得ないことへの手当てで、deviation.md に記録がある。`internal` で既定 false、同梱条件 (本務で触るファイル・公開 API に触れない・局所的・テストで担保・ユーザーの選択を要しない) をすべて満たしており、`SettingsRootStore.internalRootAccessoryReceiverCount()` と同じ既存の流儀に揃っている。

### 一括破棄の耐障害化の検証 (Minor)

`KsSettingsController.DisposePlacedViews()` (`maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:296`-`336`) は、置き場所からリースを外し切った後、全件の `Dispose()` を試みてから失敗を集約するようになった。`Disconnect()` (同 `:248`-`285`) も `DisposePlacedViews` と `DisposeRetired` を個別に `try` で囲い、両方を試みてから集約する。既存の `DisposeRetiredViews` の流儀と揃った。

`HandlerTests.cs:378` (`AFailedDisposeDuringConnectFailureStillDisposesTheRemainingViews`) が、1 件目の破棄だけを失敗させて 2 件目が破棄されること・集約例外が接続の失敗と破棄の失敗の両方を含むことを固定している。ループが 1 件目で中断すれば `Leases[1].IsDisposed` が false になるため、アサーションに検出力がある。

## 証跡の確認 (process L-003)

- `evidence/README.md` が本文で参照する PNG は 41 種で、**すべて `evidence/` に実在する**。逆に README から参照されていない PNG も無い (機械的に突き合わせた)
- 修正前後の対応が最も重要な 2 枚を実際に開いて README の記述と突き合わせた。`before-android-accessory-views-t0000ms.png` は先頭行が LabelCell「Header View のバインド」で view accessory がどれも無く、④ の Section は text フォールバック「④ Header Text（View を外すと現れる）」を表示している。`after-android-accessory-views-t0000ms.png` は同じ t=0 で ① Root Header View・② Section Header / Footer View・③ 折り返す Label の Footer View (3 行)・④ Header View (text ではなく View)・⑤ 差し替え前の Header View がすべて在り、先頭行が ① になっている。README の記述と一致し、提出コードが狙った変化 (実体化と配信の前倒し) と対応している
- `after-android-*` の一部フレームがバイト単位で同一 (t0100〜t0500 の 5 枚、customcell の 4 枚) だが、これは「変化なし」という記述と整合する。画面が静止している区間の切り出しとして妥当で、証跡の水増しには当たらない
- **review-002 のアクション 3 は解消している** — 1.3 の節は自前の「環境」「手順」表と観測結果を持ち、`before-ios-sim-*` の各フレームの観測内容が表になっている。外部の文書へ詳細を預けている箇所は無い。6.3 の節も同じ形で、合否条件 (「後着が無いこと」「1.3 に無かった高さの変動が出ていないこと」) に対する判定文がある
- Sample の追加項目 (③) については、修正前のビルドにも同じ Sample 追加分だけを当てて撮ったこと・CustomCell の画面は ③ の影響を受けないため修正前フレームを流用したことが README に明記されている。比較の前提として妥当

`before-ios-device-*` に当たるファイルは現時点で `evidence/` に存在せず、README にも実機の節は無い。申告どおり進行中として扱った。

## 指摘事項

### [🟡 Minor] Host 世代を手放す経路のうち `ReleaseHost()` の中だけが、最初の破棄の失敗で中断する

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:392`-`401` (`ReleaseHost`)、同 `:744`-`770` (`ReleaseAccessoryViews`)、同 `:1123`-`1145` (`ReleaseCellContentViews`)

**問題点**: 新しいロールバック (`SettingsViewHandler.cs:150` の `VirtualView.ReleaseHost()`) は、Native Host の解放をこの処理に委ねている。ところが `ReleaseAccessoryViews` と `ReleaseCellContentViews` のリース破棄ループには `try` が無く、1 件でも投げると `ReleaseHost()` はそこで抜ける。すると後続の `ReleaseCellContentViews()` と `_gateway.ReleaseHost()` が実行されず、**Native Host そのものが解放されないまま**残る。deviation.md が本サイクルの設計変更として掲げた「後片付けの 1 つが失敗しても残りを試み」「Native Host を世代ごと解放してから失敗を伝える」は、`RollbackHost` の 2 単位のあいだでは成立しているが、`ReleaseHost()` の内側では成立していない。今回 `DisposePlacedViews` で同じ形の中断を塞いだのと対になる場所であり、片方だけ耐障害性が無い状態になっている。

なお `ReleaseAccessoryViews` / `ReleaseCellContentViews` 自体は本 change 以前からある処理で、通常の切断 (`DisconnectHandler`) でも同じ形である。優先度を低く見るのは、`IKsViewLease.Dispose` が実際に投げる現実性が低いためで、形としての不整合は残る。

**推奨修正**: 両メソッドの破棄ループを `DisposePlacedViews` (`:317`-`335`) と同じ「全件試行 → 失敗を集約」の形にする。数行で閉じる。回帰は 2 件のうち 1 件目の破棄を失敗させ、2 件目も破棄されることと `_gateway.ReleaseHost()` まで到達することを見るテストで固定できる。

### [🔵 Suggestion] `Disconnect()` の 2 段の集約のうち、片方の組み合わせにテストが無い

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:259`-`284`、`maui/KsSettingsView.Maui.Tests/HandlerTests.cs:378`

**問題点**: `Disconnect()` は `DisposePlacedViews` と `DisposeRetired` を別々に試みて集約する形になったが、テストが覆っているのは `DisposePlacedViews` の内側 (1 件目の破棄が失敗しても 2 件目が破棄される) だけである。「`DisposePlacedViews` が投げても `DisposeRetired` は試みられる」という外側の組み合わせは固定されていない。`Disconnect()` の前段で `ClearRegistrations()` が走るため、接続失敗の時点で `_retiredViews` が空でないことは普通に起こり、到達しない組み合わせではない。

**推奨修正**: 必須ではない。`AFailedDisposeDuringConnectFailureStillDisposesTheRemainingViews` と同じ足場で、置き場所側の破棄を失敗させたうえで退役キュー側のリースも破棄されることを見るテストを 1 件足せば埋まる。

### [🔵 Suggestion] 蒸留時に、実体化の前倒しに伴う退役順序の例外を長命層へ写す

**該当箇所**: `kasane/changes/android-accessory-view-late-insert-animation/specs/maui-core/spec.md` (Requirement「配置済みの view accessory は Native Host の最初の表示に含まれる」)、`deviation.md` の「表示中の Root の作り直しでは、後片付け待ちの実体を持つ View を事前実体化の対象から外す」

**問題点**: デルタスペックは「Section の view accessory は Native Host を生成する時点で native の設定ツリーの現在状態に含まれている」と無条件に書いているが、実装は後片付け待ちの実体を持つ View だけを事前実体化から外し、配信の後に実体化して送る (maui/ADR-0016 の退役順序を守るため。deviation 記録済み・オーナー追認済みの合意差分であり、指摘ではない)。この文言はそのまま蒸留で長命層へ写ると、実装と食い違う記述が残る。

**推奨修正**: 実装側の対応は不要。maui/ADR-0027 を accepted にする時点で、「表示中の Root の作り直しで同じ View が残る場合は、退役順序を優先して配信の後に実体化する」旨の一行を足すかどうかを判断してほしい (足場の書き換えはレビューの権限外のため、所見として残す)。

## 今回あらためて見て、欠陥が見つからなかった範囲

- **ロールバックの冪等性**: `CreateHost()` の中 (`Controller.Connect`) が失敗した場合、`Connect` の `DisconnectAfterFailure` が先に走って `_gateway` が null になった後で `RollbackHost` が `ReleaseHost()` を呼ぶ。リースは既に外れており、`_gateway?.ReleaseHost()` は no-op になるため、二重の後片付けで壊れる箇所は無い
- **`Containment.Remove()` の安全性**: `ApplyRootAccessories()` で失敗したときは `AddToParent()` を通っていないが、iOS の `KsHostContainment.Remove()` は `WillMoveToParentViewController(nil)` / `RemoveFromSuperview()` / `RemoveFromParentViewController()` のいずれも未登録で安全に no-op になる
- **`Loaded` 購読の漏れ**: `CreatePlatformView` が投げると `ConnectHandler` は呼ばれないため、`VirtualView.Loaded` の購読は残らない
- **`ConnectGateway` の再接続分岐で `ApplyStoreViews()` が投げた場合**: 例外は `CreateHost()` を抜けて `CreatePlatformView` の `catch` に入り、同じロールバックを通る。gateway は接続済みのまま残るが、これは再接続として扱う設計どおりで、やり直しは新しい実体化の口で再接続分岐へ入る
- **例外型の契約**: 重複配置の検出 (`EnsureTreeHasNoDuplicates`) は `MaterializeTreeViews` より前にあるため、利用者に見える `InvalidOperationException` は従来どおりの型で出る (対応する既存テストが通過)。`AggregateException` になるのは後片付けまで失敗した場合に限られる
- **実行時の `Root` 差し替えで `MaterializeTreeViews` が失敗した場合**: 登録が消えたまま一部のリースが残るが、次の `RebuildRoot` は同じ View の置き場所を見つけて既存リースを使い回すため漏れにはならない。この失敗形は実体化が `RegisterSection` の中にあった修正前から同じで、本 change が新しく開けた穴ではない
- **Android 側**: `bind` / `unbind` と受け口の登録、`rootAccessoryLock` の内側で binding の差し替えと控えの破棄を同時に行う形、`diffs` 購読での Root 対象の読み飛ばし、`CancellationException` の投げ直しは review-003 時点から変更が無く、再読でも欠陥は見つからなかった
- **iOS 側**: `applyBeforeViewLoad` が Root 対象だけをプロパティへ控え、`collectionView` 未生成の間は didSet が何もしないため view load を誘発しない点も変更なし

## 完了条件の未充足 (コードの指摘ではない)

`kasane/lessons/process.md` の L-003 に従い、実機・Simulator での視覚確認と証跡は変更の完了条件として扱う。**この APPROVED は変更の完了を意味しない。**

- tasks 1.3 / 6.3 のうち **iOS 実機 (iPhone 11) の観測が未完了**。Simulator 分は修正前後とも揃い、6.3 の合否条件 (後着なし・折り返す FooterView の高さのブレなし) も Simulator では満たしている。実機の修正前フレーム (`before-ios-device-*`) の追加は進行中、修正後の撮影はこれから
- tasks 6.5 (`../ColorAnalyzer` での確認) はリリース後の確認項目としてオーナーへ申し送る方針と申告されている
- `kasane/changes/ios-customcell-initial-height-grow-animation` への追記材料は、README のとおり Simulator 環境からは得られていない。実機の観測で得られた場合はあちらへ回す

実機の証跡が揃った時点で、6.3 の合否条件に抵触していないかの確認が別途必要になる。抵触した場合は tasks 6.3 のとおり完了を止める分岐が残っている。

## アクションプラン

1. (任意・優先度低) `ReleaseAccessoryViews` / `ReleaseCellContentViews` の破棄ループを `DisposePlacedViews` と同じ「全件試行 → 集約」の形に揃える。数行で閉じ、回帰テスト 1 件で固定できる
2. (任意) `Disconnect()` の外側の組み合わせ (置き場所側の破棄が失敗しても退役キュー側を試みる) のテストを 1 件足す
3. iOS 実機の証跡の追加を待ち、tasks 1.3 / 6.3 の観測結果を対象に含めて確認する。6.3 の合否条件に抵触した場合は完了を止める
4. 蒸留時: maui/ADR-0027 に退役順序の例外を、core/ADR-0033 にキャンセルの合図の例外 (review-003 の所見) を書き足すかを判断する
5. (任意・別途) 公開 API 面の機械検査 (binary-compatibility-validator) の導入可否を判断する
