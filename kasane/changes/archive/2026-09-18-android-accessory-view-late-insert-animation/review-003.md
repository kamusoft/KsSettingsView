# レビュー結果: android-accessory-view-late-insert-animation (003 回目)

**日付**: 2026-09-17
**判定**: APPROVED

## サマリー

前回 (review-002) の Major 1 件・Minor 1 件・Suggestion 1 件は、いずれも手当てされている。公開 API 面への配送メソッドの露出はビルド成果物で消えていることを確認し、失敗した接続が実体を持ち越す窓は `ReleaseHost` と同じ土台の後片付けで塞がれ、その経路を実際に通る回帰テストが 4 件付いた。相方レビュー (second-opinion-code-002) が Major としていた gateway 生成の失敗経路も同じ修正に含まれている。

修正で新たに入った欠陥は見つからなかった。3 platform のテストは全件成功 (Android 2942 / iOS 691 (UI バンドル) / MAUI 579)、標準 lint は禁止 0 件。コード側の指摘は Suggestion 2 件のみで、いずれも任意。

ただし**変更の完了条件は未充足**である — tasks 1.3 / 6.3 (iOS の遷移直後フレームの観測) が開いたままで、これはオーナー合意済みの進行中項目として本レビューの対象外に置かれている (下の「完了条件の未充足」を参照)。本判定はレビュー対象 (`android/` `ios/` `maui/` の製品コードとテスト) に対するものである。

## レビュー対象と除外

- 対象: 作業ツリーの未コミット差分のうち `android/` `ios/` `maui/` の製品コードとテスト (ベース HEAD = develop)。25 ファイル変更 + 未追跡の Kotlin 5 ファイル
- 除外 (依頼による): `samples/` と `evidence/` — 別ワーカーが FooterView の追加と修正前後の撮り直しを並行作業中で、途中の状態。テスト・ビルドでも `samples/maui` には触れていない
- `tasks.md` の差分はチェックボックスのみ。足場アーティファクトの書き換えは無い。未チェックで残るのは 1.3 / 6.3 / 6.5 で、申告された実態と一致する

## 実行結果

| platform | コマンド | 結果 |
|---|---|---|
| Android | `./gradlew test --rerun-tasks` (`android/`) | 2942 tests / 0 failures / 0 errors / 0 skipped (`build/test-results/test*UnitTest/TEST-*.xml` 240 ファイルの集計)。`BUILD SUCCESSFUL` |
| iOS | `xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,name=iPhone 17 Pro Max'` (`ios/`) | `KsSettingsViewUITests.xctest` 691 tests / 0 failures。`** TEST SUCCEEDED **` (exit 0) |
| MAUI | `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` | 579 tests / 0 failures / 0 skipped |
| lint | `scripts/local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` | いずれも禁止 0 件 (comment-policy は 807 ファイル検査) |

前回比で Android +2 / MAUI +3 の増加があり、いずれも今回の修正に対応する新規テストである。

comment-policy の `--advisory` は要確認 169 件を報告するが、これはリポジトリ全体 (130 ファイル) に及ぶ既存の傾向であり、今回の差分が新しく持ち込んだ公開 doc コメント内の ADR 参照は無い (追加行に ADR ID を含む公開 doc は `KsSettingsController` の `ApplyRootAccessories` / `ApplyStoreViews` だけで、当該クラスは `internal sealed class` のため実際には公開 API ではない — 検査の可視性推定の限界による報告)。

## 照合した規約

- `cross/comment-policy.md` (always) — 公開 doc コメントの内部用語、禁止参照 (作業文書のパス・変更識別子・レビュー通番)、履歴記述の各節を新規コメントについて照合。違反なし
- `cross/test-execution.md` (テストを実行・報告するとき)
- `cross/runtime-behavior-verification.md` (実行時挙動が絡む不具合の完了判定) — 下の「完了条件の未充足」に関係する
- `cross/diagnostic-message-language.md` (`SettingsRootStore` の `Log.e` — 英語・文脈先頭・句点なしで既存の流儀に適合)
- `ios/swift6-language-mode-check.md` (`ios/Sources/` を触る変更の完了判定 — `ios/Package.swift` に一時設定は無く、テストは警告なく成功)
- `maui/integration-host-verification.md` (facade 層に触れる変更の end-to-end 疎通 — 証跡は `evidence/` にあり、本レビューでは対象外として実在のみ確認)

参照した決定: core/ADR-0033 (proposed)・maui/ADR-0027 (proposed)・core/ADR-0005・core/ADR-0006・core/ADR-0019・core/ADR-0020・core/ADR-0023・maui/ADR-0016。proposed の 2 件を根拠にした指摘は出していない。

ロードした code-review スキル: kotlin-impl-skill (android ドメイン)。`kasane/lessons/code-review.md` の重点観点 (ミューテーションによる検出力の実測) は、本レビューでも制約によりコードへ一時変更を入れられないため、代わりにビルド成果物の検査 (`javap -v` によるフラグ確認) と、テストが通る失敗経路の到達可能性の追跡で裏を取った。

## 前回指摘の解消状況

| 出所 | 指摘 | 状況 |
|---|---|---|
| review-002 Major | 受け口の internal interface 化で `KsSettingsView` の公開 API に `receiveRootAccessoryUpdate` が露出 | **解消**。下に検証内容 |
| review-002 Minor | 接続が実体化より後で失敗すると、作られた実体が後片付けされないまま次の接続へ残る / その手当てのテストが当該経路を覆えていない | **解消**。下に検証内容 |
| review-002 Suggestion | 配送の握り潰しが `CancellationException` まで飲み込む | **解消**。`SettingsRootStore.kt:345`-`346` で先に捕捉して投げ直す。`RootAccessoryDeliveryFailureTest.kt:174` が呼び出し元への伝播を固定し、同 `:134` が通常の失敗は伝わらないことを引き続き固定している |
| second-opinion-code-002 Major | gateway 生成の失敗経路 (`ConnectGateway` が `AttachViews` の後に factory を呼ぶ) と、未登録の配置の lease が残る経路 | **解消**。下に検証内容 |
| review-001 Major / second-opinion-code-001 Major-3 | iOS の遷移直後フレーム (tasks 1.3 / 6.3) | **未解消 (合意済み・進行中)**。本レビューの対象外。下の「完了条件の未充足」 |
| review-002 アクション 3 | `evidence/README.md` の 1.3 の記述を自己完結する形へ直す | **本レビューの対象外** (`evidence/` は並行作業中)。次のレビューで確認が要る |

### 公開 API 面の検証

`KsSettingsView` はもう `RootAccessoryReceiver` を実装しておらず、受け口は Host が private に持つ匿名オブジェクトへ移っている (`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:68`-`75`)。release ビルドの `KsSettingsView.class` を `javap` で確認した:

- クラス宣言に `implements ... RootAccessoryReceiver` は無い
- 未修飾の `receiveRootAccessoryUpdate` は存在しない
- 配送の入口は `private final void onRootAccessoryUpdate(...)` と、コンパイラ生成の `access$onRootAccessoryUpdate` のみ。後者は `flags: (0x1019) ... ACC_SYNTHETIC` で、Kotlin のメタデータにも現れない標準的な合成アクセサ

寿命の保証も変わっていない — Store は受け口オブジェクトを弱参照し、受け口オブジェクトは外側の Host を強参照、Host は受け口オブジェクトを強参照するため、外部からの参照が切れれば 2 つまとめて回収される。`RootAccessoryReceiverReleaseTest` は Host 自身の弱参照で回収を見ており、この形でも意味を保つ (通過を確認)。

### 失敗した接続の後片付けの検証

3 点が揃っている。

1. `maui/KsSettingsView.Maui/SettingsView.cs:985`-`997` で `gatewayFactory()` が `AttachViews` より前に移った。factory が投げれば `_views` は差し込まれないままで終わる
2. `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:227`-`240` の `Disconnect` が `_imageGeneration++` / `_images = null` / `_views = null` を行い、`DisposePlacedViews()` (同 `:248`-`276`) で `_accessories` / `_cellContents` の対応表を直接走査して lease を破棄する。登録をたどる `ClearRegistrations` では届かない、未登録の配置がこれで拾える。形は既存の `ReleaseAccessoryViews` (同 `:682`-) と揃っており、`_measureDirtySlots.Clear()` を含めて同じ土台になっている
3. テスト 4 件が経路ごとに分かれている (`maui/KsSettingsView.Maui.Tests/HandlerTests.cs:99`/`:121`/`:145`/`:172`)。実体化の途中で失敗する状況は `FakeViewMaterializer.SucceedsUpTo` で作られており、`EnsureTreeHasNoDuplicates` に先回りされない (2 つの Section が別々の `Label` を持つため重複検査は通過し、`MaterializeTreeViews` へ到達して 2 件目で失敗する)。前回指摘のとおり「失敗までに作られた実体が破棄済みであること」と「再接続で新しい口により作り直され配信データに載ること」まで見ている

検出力も確認した — `_views = null` を戻せば `AFailedInitialConnectReleasesTheMaterializationSeam` の `Leases` 件数が 2 になり、`DisposePlacedViews()` を外せば同テストの `IsDisposed` が false になる (失敗時点で Section はまだ登録されていないため `ClearRegistrations` では拾えない)。

## 今回の変更で新たに見た範囲

前回から動いた箇所を中心に、次を追った。いずれも欠陥は見つからなかった。

- **`bind` / `unbind` と受け口の登録の整合**: 同一 Store への再 bind (購読が生きている場合の早期 return / 取り外しで Job が止まっている場合の通常経路) の双方で、登録は 1 件のまま、控えた値も落ちない (`switchRootAccessorySource` が同一配送元では何もしないため)。別 Store への bind では、登録解除 → 配送元の差し替えと控えの破棄 → 新しい登録の順序が保たれている
- **`diffs` の読み飛ばしと配送路の単一性**: `store.diffs` の購読者はリポジトリ内で `KsSettingsView.kt:616` の 1 箇所のみで、Root 対象はそこで読み飛ばされる。二重適用の経路は無い
- **MAUI の配信順序**: `ConnectGateway` の再接続分岐は `AttachViews` → `ApplyStoreViews` を Native Host 生成 (`MakeHost`) より前に通し、Root は `CreatePlatformView` が `CreateHost()` の直後・`Containment.AddToParent()` より前に適用する。Section / Cell は Store の現在状態として復元され、Root は Host のプロパティとして届く、という design.md の二本立てがそのまま実装に出ている
- **`PlaceAccessoryView` / `PlaceCellContent` の「同じ View なら置き直さず配信もしない」分岐**: 到達し得る経路を洗った。(a) 初回構築では `MaterializeTreeViews` が先に置いて `SetRoot` が実体を運ぶため再配信は不要、(b) 実行時の Section 挿入・再挿入では `RetireSectionAccessoryViews` が対応表の項目を消しているため必ず `SetAccessoryView` の全経路を通る、(c) 後片付け待ちの実体を持つ View は `MaterializeTreeViews` で除外されるため対応表に載らず、同じく全経路を通る。配信が落ちる組み合わせは作れなかった
- **maui/ADR-0016 の退役順序**: `PreviousViewIsDisposedAfterTheRebuiltRootIsDelivered` / `CustomCellContentTests` の対応テストが引き続き破棄の時点を数で見ており、配信より前へ戻れば落ちる

## 指摘事項

### [🔵 Suggestion] 公開 API 面に配送用メソッドが増えないことを機械で守る手段が無い

**該当箇所**: `android/kssettingsview/build.gradle.kts:86` (`explicitApi()`)、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/RootAccessoryReceiver.kt:14`

**問題点**: 今回の Major は「Kotlin の `internal interface` のメンバを public クラスが override すると公開面に出る」という形で入り、`explicitApi()` では捕まらなかった (override は明示可視性の要求の対象外)。現在の形は正しいが、同じ誤りが再発しても検知する仕組みが無く、確認は人手の `javap` に依存している。

**推奨修正**: 必須ではない。binary-compatibility-validator (`apiDump` / `apiCheck`) を `android/kssettingsview` に入れると、公開面の変化が `api/*.api` の差分として毎回見える形になる。ただし導入は本変更のスコープ外の足場作業であり、判断はオーナー・propose 側に委ねる。

### [🔵 Suggestion] 蒸留時に、キャンセルの合図を投げ直す扱いを長命層の文言へ反映する

**該当箇所**: `specs/android-host/spec.md` (Requirement「購読できていない間に渡された Root の header / footer の保持」)、`kasane/decisions/core/0033-*.md:32`

**問題点**: デルタスペックと core/ADR-0033 (proposed) はどちらも「ある Host での反映の失敗は `updateAccessory` の呼び出し元へ伝わらない」と無条件に書いているが、実装はコルーチンのキャンセルの合図だけを例外として投げ直す (deviation.md 記録済み・合意済みの差分)。これ自体は妥当な判断であり、指摘ではない。ただしこの文言は蒸留で長命層へ写るため、そのまま写すと実装と食い違う記述が残る。

**推奨修正**: 実装側の対応は不要。ADR-0033 を accepted にする時点で、「失敗は伝えない。ただし協調的キャンセルの合図は握り潰さず投げ直す」旨の一行を足すかどうかを判断してほしい (足場の書き換えはレビューの権限外のため、ここでは所見として残す)。

## 完了条件の未充足 (コードの指摘ではない)

`kasane/lessons/process.md` の L-003 に従い、実機・Simulator での視覚確認と証跡は変更の完了条件として扱う。現状は次のとおりで、**このレビューの APPROVED は変更の完了を意味しない**。

- tasks 1.3 / 6.3 (iOS の遷移直後フレームの修正前後の観測) が未完了。オーナー合意で iOS Simulator の録画を先に取り、実機 (iPhone 11) の観測はオーナーの画面収録で後から追加する方針へ変更済み (deviation.md 記録あり)。証跡の追加作業は本レビューと並行して進行中で、`evidence/` は本レビューの対象から外している
- 6.3 の合否条件 (「view accessory の後着が無いこと」および「1.3 の修正前には無かった高さの変動が出ていないこと」) の判定はまだ行われていない。ここで変動が出た場合は完了を止めてオーナーへ報告する、と tasks に書かれた分岐が残っている
- tasks 6.5 (`../ColorAnalyzer` での確認) はリリース後の確認項目としてオーナーへ申し送る方針と申告されている
- `evidence/README.md` の 1.3 の記述を自己完結する形へ直す件 (review-002 のアクション 3) も未確認

証跡が揃った時点で、`evidence/` と `samples/` を対象に含めた確認が別途必要になる。

## アクションプラン

1. (コード側) 対応の必要な指摘は無い
2. 証跡の追加作業の完了を待ち、tasks 1.3 / 6.3 の観測結果と `evidence/` の内容を対象に含めて確認する。6.3 の合否条件に抵触した場合は完了を止める
3. 蒸留時: core/ADR-0033 の「失敗を呼び出し元へ伝えない」の文言にキャンセルの合図の例外を足すかを判断する
4. (任意・別途) 公開 API 面の機械検査 (binary-compatibility-validator) の導入可否を判断する
