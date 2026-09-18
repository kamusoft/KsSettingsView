# レビュー結果: android-accessory-view-late-insert-animation (005 回目)

**日付**: 2026-09-18
**判定**: APPROVED

## サマリー

修正サイクル 4 で入った「Host 世代の後片付けの全件試行化」は、狙った穴を塞いでいる。review-004 の Minor (`ReleaseHost` の中だけが最初の破棄の失敗で中断する) と Suggestion 2 (`Disconnect` の外側の組み合わせにテストが無い) はどちらも解消し、新規 3 件のテストはいずれも**ミューテーション実測で検出力を確認した**（修正を元へ戻すと、争点のアサーションだけが落ちる）。既存の正常系 (切断時の書き戻し → 破棄の順序、退役順序 maui/ADR-0016) は壊れておらず、MAUI 587 件全成功・全 TFM ビルド 0 警告 0 エラー・標準 lint 禁止 0 件。

同じミューテーション実測で、**新しい耐障害化のうち 2 箇所 (Cell の内容の実体の後片付け・accessory のテキスト書き戻し) には回帰テストが無い**ことも分かった (元へ戻しても 587 件すべて通る)。同じサイクルで入った accessory リース破棄の側は 2 件で固定されているため、片側だけ無防備になっている — 下の Minor がこれで、優先度は低い。

前回対象に入れた範囲 (Android / iOS / evidence / samples) のうち、今回新しく入った `before-ios-device-*` の 14 枚と evidence/README.md の実機の節も確認した。Android / iOS のコードは review-004 時点から差分が動いていないことを確認したうえで、再実行はしていない。

**この APPROVED は変更の完了を意味しない** — tasks 6.3 の実機 (修正後) と 6.5 が開いている (下の「完了条件の未充足」)。

## レビュー対象

- 作業ツリーの未コミット差分すべて (ベース HEAD = develop)。追跡済み 31 ファイル + 未追跡 (Kotlin 5・`Fakes/UnsubscribableCellCollection.cs`・`evidence/`・`deviation.md`・レビュー証跡)
- **前回 (review-004) 以降に動いたのは 6 ファイル**: `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs` / `maui/KsSettingsView.Maui.Tests/HandlerTests.cs` / `maui/KsSettingsView.Maui.Tests/Fakes/UnsubscribableCellCollection.cs` (新規) / `kasane/handbook/cross/local-development-setup.md` / `tasks.md` / `deviation.md`。あわせて `evidence/` に `before-ios-device-*` 14 枚と README の実機の節、`kasane/changes/ios-customcell-initial-height-grow-animation/exploration.md` の追記、`kasane/lessons/inbox/device-redeployed-without-clean-build-then-handed-to-owner.md` が加わった
- Android / iOS は `git diff --stat HEAD -- android ios` が 7 ファイル / +805 -44 で、対象ファイルの最終更新がいずれも review-004 の出力時刻より前 (最新で 09-17 23:17)。review-004 時点と同一と判断し、指示どおり再実行はしていない
- 足場アーティファクト (proposal / design / specs) の書き換えは無い。`tasks.md` の差分はチェックボックスのみで、1.3 が `[x]` へ変わり 6.3 / 6.5 が開いたまま残る — 申告された実態と一致する

## 実行結果

| 対象 | コマンド | 結果 |
|---|---|---|
| MAUI テスト | `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` | **587 tests / 0 失敗 / 0 スキップ** |
| MAUI ビルド (全 TFM) | `dotnet build maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj` | net10.0 / net10.0-android / net10.0-ios とも **0 警告 0 エラー** |
| lint | `scripts/local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` | 禁止 **0 件** (comment-policy は 808 ファイル検査) |
| lint | `scripts/doc-structure-lint.py` | 編集した `kasane/handbook/cross/local-development-setup.md` に指摘なし (既存の他文書の指摘のみ) |

MAUI は前回 584 → **587 (+3)**。増分は本サイクルの新規テスト 3 件と一致する。Android (2942) / iOS (1050) は差分不変のため再実行なし。

`comment-policy-lint.py --advisory` の要確認は review-004 と同じ 5 件 (`KsSettingsController.cs:374` / `:426` / `:445` / `:465`、`Platforms/Android/SettingsViewHandler.cs:45`)。前 4 件は `internal sealed class` のメンバで実際には公開 API ではなく (検査の可視性推定の限界)、最後の 1 件は本 change 以前からある行。本サイクルが新しく持ち込んだものは無い。

## 検出力の実測 (lessons code-review L-001)

制約によりプロジェクトのコードは触れないため、`maui/` ツリーを scratchpad へ複製し (`bin` / `obj` 除外)、**複製側だけにミューテーションを入れて実測した**。複製の基準実行は 587 件成功で本体と一致し、実測後に複製を原状復帰して本体との shasum 一致を確認している (本体ファイルの更新時刻も変わっていない)。

| ミューテーション (複製側) | 結果 |
|---|---|
| `ReleaseHost` を逐次実行へ戻す | **2 件失敗**。`AFailedAccessoryDisposeDuringRollbackStillReleasesTheHost` / `AFailedAccessoryDisposeDuringDisconnectStillReleasesTheHost` が `DetachInteractionsCount` 期待 1 / 実際 0 で落ちる |
| `ReleaseAccessoryViews` の破棄ループから `try` を外す | **2 件失敗**。同じ 2 件が `Leases[1].IsDisposed` 期待 True / 実際 False で落ちる |
| `Disconnect` を逐次実行へ戻す | **1 件失敗**。`AFailedPlacedDisposeDuringConnectFailureStillDisposesTheRetiredViews` が落ちる |
| `DisposePlacedViews` の破棄ループから `try` を外す | **1 件失敗**。前サイクルの `AFailedDisposeDuringConnectFailureStillDisposesTheRemainingViews` が落ちる |
| `ReleaseCellContentViews` を元の逐次実行へ戻す | **全 587 件成功 (検出できない)** |
| accessory のテキスト書き戻しループから `try` を外す | **全 587 件成功 (検出できない)** |

落ちたものはいずれも前提アサーションではなく争点のアサーションで、トートロジーではない。最後の 2 行が下の Minor の根拠である。

## 照合した規約

- `cross/comment-policy.md` (always) — 本サイクルの新規コメント (`TryCleanUp` / `DisposePlacedViews` / `ReleaseHost` / `ReleaseAccessoryViews` / `ReleaseCellContentViews` の remarks、`UnsubscribableCellCollection` の doc、新規テスト 3 件の doc) を節ごとに照合。許容参照・禁止参照 (作業文書のパス・change 名・ローカル通番)・禁止する記述類型 (履歴記述・デルタスペック構文キーワード)・公開メンバーの doc コメントのいずれにも違反なし。新規コメントに ADR ID は含まれない
- `cross/test-execution.md` (テストを実行・報告するとき) — MAUI は末尾集計行を報告。新規 3 件に固定時間の待機は無い。Android / iOS は差分不変を確認したうえでの再実行省略 (指示による)
- `cross/local-development-setup.md` (編集対象そのもの) — `kind: guide` の文書で、追記は iOS 節 (`:144`) と対称な位置・粒度・語り口。`timestamp` を 2026-09-18 へ更新済み。`applies-when.tasks` の「MAUI Sample への native 変更の配備確認」が既に該当するため追加は不要。本文が挙げる `maui/android/KsSettingsView.Binding.Android` は実在する。doc-structure lint の指摘なし
- `cross/runtime-behavior-verification.md` / `cross/sample-parity.md` / `cross/diagnostic-message-language.md` — 本サイクルで新たに触れた範囲に該当なし (新規の例外文字列はテスト用 fake の中のみで、いずれも英語・現在形)
- `maui/integration-host-verification.md` — facade の後片付け経路に触れたが、C# → Native の疎通の形は変わっておらず、既存の証跡 4 枚 + MauiHost 2 枚で足りる
- `maui/ADR-0016` (accepted) — 「Store 更新 → native への配信 → 旧 wrapper 破棄」の退役順序は、`ReleaseAccessoryViews` (書き戻し → 破棄) と `ReleaseCellContentViews` (配信 → 破棄) の成功路で保たれている。ADR-0016 の「再接続時は Host 取り付け後に再実体化・再発行する」箇条は `maui/ADR-0027` が `amends: 0016` を宣言して置き換える形になっており、突き合わせ漏れは無い
- `core/ADR-0033` / `maui/ADR-0027` はいずれも `proposed`。これらを根拠にした指摘は出していない

ロードした code-review スキル: kotlin-impl-skill (android ドメイン)。本サイクルの差分は MAUI (C#) のみのため適用箇所なし。

## 前回指摘の解消状況

| 出所 | 指摘 | 状況 |
|---|---|---|
| review-004 Minor | `ReleaseHost` の中だけが最初の破棄の失敗で中断し、Native Host が解放されないまま残る | **解消**。`ReleaseHost` が 4 単位を独立に試み、`ReleaseAccessoryViews` / `ReleaseCellContentViews` の内側も全件試行になった。ミューテーション実測で回帰が固定されていることを確認 |
| second-opinion-code-004 Major | 同上 (相方の同一指摘) | **解消**。相方が求めた確認項目 4 つ (後続リースの破棄・interaction の解除・`gateway.ReleaseHost()` の到達・集約例外の先頭が元の失敗) は `HandlerTests.cs:418` がすべて見ている |
| review-004 Suggestion 2 | `Disconnect()` の外側の組み合わせ (置き場所側が失敗しても退役キュー側を試みる) にテストが無い | **解消**。`HandlerTests.cs:494` が `UnsubscribableCellCollection` で「登録済み = 後片付け待ち」と「未登録 = 置き場所に残る」を同時に作り、置き場所側の破棄を失敗させている。ミューテーションで検出力を確認 |
| review-004 Suggestion 3 / review-003 Suggestion 2 | 蒸留時に maui/ADR-0027 へ退役順序の例外を、core/ADR-0033 へキャンセルの合図の例外を書き足すか判断する | **未対応 (蒸留時の項目)**。実装側の対応は不要 |
| review-003 Suggestion 1 | 公開 API 面の機械検査 (binary-compatibility-validator) が無い | **未対応 (任意)**。判断はオーナー・propose 側 |
| review-001 Major / second-opinion-code-001 Major-3 | iOS の遷移直後フレーム (tasks 1.3 / 6.3) | **1.3 は解消**。実機 (iPhone 11) 修正前の 14 枚と README の実機の節が加わり、tasks 1.3 が `[x]` になった。6.3 の実機 (修正後) は未了 |

## 指摘事項

### [🟡 Minor] 同じサイクルで入れた耐障害化のうち、Cell の内容の後片付けと accessory の書き戻しだけ回帰テストが無い

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1176`-`1188` (`ReleaseCellContentViews`)、同 `:780`-`783` (`ReleaseAccessoryViews` の書き戻しループ)

**問題点**: 上の実測のとおり、この 2 箇所を修正前の逐次実行へ戻しても 587 件すべて通る。deviation.md が本サイクルの設計変更として掲げた「各単位の内側も、置き場所からリースを外し切った後は 1 件の書き戻し・破棄の失敗で残りを飛ばさない」のうち、**固定されているのは accessory のリース破棄だけ**で、同じ文が語る残り 2 箇所は無防備である。`ReleaseHost` が 4 単位を独立に試みる形は覆われているため Native Host の解放までは守られるが、`DeliverCellContents` が投げた場合に内容のリースが破棄されない・1 件目の内容リースの破棄が投げた場合に 2 件目が破棄されない・1 件目の書き戻しが投げた場合に 2 件目の書き戻しとリース破棄が飛ぶ、という単位内部の退行は誰も検出しない。次に誰かがこの `try` を「冗長だ」と外しても、テストは緑のまま通る。

優先度を低く見るのは、`IKsViewLease.Dispose` と `UpdateAccessory` / `UpdateCellContent` が実際に投げる現実性が低く、`ReleaseHost` 側の全件試行で最悪でも Native Host は解放されるためである。

**推奨修正**: `AFailedAccessoryDisposeDuringRollbackStillReleasesTheHost` (`maui/KsSettingsView.Maui.Tests/HandlerTests.cs:418`) と同じ足場で 1〜2 件足す。`FakeViewMaterializer.Materialized` で CustomCell の内容の 1 件目の実体に破棄失敗を仕込み、2 件目の内容リースが破棄されることと `ReleaseHostCount` が 1 になることを見れば `ReleaseCellContentViews` 側が埋まる。書き戻し側は `FakeSettingsGateway` に `UpdateAccessory` を失敗させる切り替えを足し (`SetRootFails` と同型)、2 つ目の Section の書き戻しとリース破棄まで到達することを見る。

### [🔵 Suggestion] 切断の経路は逐次のままで、ロールバックが「切断時と同じ手順」と述べる相手より脆い

**該当箇所**: `maui/KsSettingsView.Maui/Handlers/SettingsViewHandler.cs:106`-`116` (`DisconnectHandler`)、同 `:127`-`157` (`RollbackHost`)

**問題点**: `RollbackHost` の doc は「手放す手順は切断時の後片付けと同じ」と述べ、実際には `Containment?.Remove()` と `VirtualView.ReleaseHost()` を個別に試みる。一方 `DisconnectHandler` は HEAD と同一で、`Containment?.Remove()` → `view.ReleaseHost()` → `base.DisconnectHandler(platformView)` が素直に並んでいる。`Containment.Remove()` が投げれば `ReleaseHost()` に到達せず、本 change が生成側で塞いだ「世代を手放せないまま残る」状態が切断側で起きる。`ReleaseHost()` が投げれば `base.DisconnectHandler` が走らない — 新規テスト `AFailedAccessoryDisposeDuringDisconnectStillReleasesTheHost` (`HandlerTests.cs:457`) が `view.Handler = null` は投げるものとして固定したので、この分岐が到達し得ることは実証済みになった。

本 change の diff はこのメソッドを 1 行も動かしておらず、`IKsHostContainment.Remove` が投げる現実性も低いため、指摘ではなく所見として残す。

**推奨修正**: 必須ではない。塞ぐなら `RollbackHost` と同じ形 (2 つの後片付けを個別に試み、失敗を集約してから `base.DisconnectHandler` へ進む) に揃えるのが自然で、数行で閉じる。

### [🔵 Suggestion] `TryCleanUp` と同じイディオムの手書きが同一ファイルに残っている

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1945`-`1982` (`DisposeRetired`)、同 `:922`-`951` (`DisposeRetiredViews`)

**問題点**: 本サイクルで `TryCleanUp` が同じファイルに入ったが、先行する 2 メソッドは `try` / `catch` / `failures ??= []` / `failures.Add` を手で並べたままである。同じ意図の書き方が 1 ファイルに 2 通りあり、後から読む人がどちらが正か迷う。動作は同じで、どちらも本サイクルより前から在る。

**推奨修正**: 必須ではない。`TryCleanUp(DisposeRetiredViews, ref failures)` / `TryCleanUp(entry.Lease.Dispose, ref failures)` / `TryCleanUp(lease.Dispose, ref failures)` への置き換えで、行数はむしろ減る。

### [🔵 Suggestion] 蒸留時に持ち越す項目 (review-004 から引き継ぎ)

**該当箇所**: `kasane/decisions/maui/0027-*.md`、`kasane/decisions/core/0033-*.md`

**問題点**: review-004 / review-003 が挙げた 2 件は未対応のままで、いずれも実装側の対応は不要。(1) maui/ADR-0027 を accepted にする時点で、「表示中の Root の作り直しで同じ View が残る場合は、退役順序を優先して配信の後に実体化する」旨を足すかを判断する。(2) core/ADR-0033 にキャンセルの合図を投げ直す例外を書き足すかを判断する。

**推奨修正**: 蒸留時に判断する。足場の書き換えはレビューの権限外のため所見として残す。

## 証跡の確認 (lessons process L-003)

- `evidence/` の PNG は **83 枚**で、README が本文で参照するファイル名と機械的に突き合わせて**過不足ゼロ** (参照だけあって不在なし・実在するが未参照なし)
- 今回加わった実機の節 (`evidence/README.md` の「1.3 (iOS 実機) 修正前の観測」) について、`before-ios-device-accessory-views-t0000ms.png` と `-t0100ms.png` を実際に開いて記述と突き合わせた。t=0 はデモ画面が右端に約 45px だけ出たフレームで、その細い帯に色付きの accessory 領域が並んでおり「view accessory は最初のフレームから在る」と一致する。t=+100ms は ① Root Header View・② Section Header / Footer View・③ 折り返す Label の Footer View (3 行)・④ Header View・⑤ 差し替え前の Header View がすべて在り、③ が 3 行で確定している。README の記述と齟齬なし
- 実機の収録が Simulator と別画面 (CustomCell の MAUI 固有デモ) になった件・6.3 で同じ位置を測る申し送りは、README と deviation.md の 22 / 23 に書かれており自己完結している
- 実機フレームに個体・個人を特定する表示は無く、identity-lint も 0 件

## 今回あらためて見て、欠陥が見つからなかった範囲

- **正常系の順序**: `ReleaseHost` の 4 単位の実行順 (accessory → Cell の内容 → 通知の解除 → Native Host)、`ReleaseAccessoryViews` の「書き戻し → 破棄」、`ReleaseCellContentViews` の「配信 → 破棄」、`Disconnect` の「置き場所 → 後片付け待ち」は、いずれも修正前と同じ。退役順序 (maui/ADR-0016) を語る既存テストが全件通っている
- **書き戻しの前倒しが副作用を持たないこと**: `ReleaseAccessoryViews` は全リースを先に外してから書き戻すようになったが、`AccessoryTextOf` は `_rootHeaderText` / `Section.HeaderText` / `FooterText` を、`TryResolveSectionId` は `_sectionEntries` を読むだけで、`TakeLease` が触る `ViewPlacement.Lease` に依存しない。ループ変数 `slot` のクロージャ捕捉も C# の反復ごと捕捉で正しい
- **例外の入れ子**: `ReleaseAccessoryViews` / `ReleaseCellContentViews` が投げる `AggregateException` は `ReleaseHost` の `Flatten()` で平坦化され、`RollbackHost` はさらに元の失敗を先頭に置いて平坦化する。`HandlerTests.cs:418` が「先頭が元の Host 生成の失敗」を固定しており、入れ子で順序が崩れない
- **二重破棄**: `TakeLease` が置き場所からリースを外してから破棄するため、破棄が失敗したリースが再び拾われる経路は無い
- **失敗後のやり直し**: `Disconnect` は `_views` / `_images` / `_gateway` を破棄より前に落とすため、`DisposePlacedViews` が投げても未接続状態への復帰は完了する。やり直しが新しい口で作り直すことは `ReconnectingAfterAFailedConnectMaterializesTheViewsWithTheNewSeam` が固定している
- **`UnsubscribableCellCollection` の素性**: 2 つ目の購読だけを拒む形で、controller が Cells を購読しなくなれば `Assert.Throws<AggregateException>` が落ちる。実装が変わったときに無言で空回りするテストにはならない
- **`TryCleanUp` が例外を広く飲むこと**: MAUI facade の後片付け経路には協調的キャンセルが無く (Android 側で `CancellationException` を投げ直しているのとは事情が違う)、飲んだ失敗は必ず集約されて投げ直されるため、握り潰しにはなっていない
- **deviation の網羅 (lessons process L-009)**: 本サイクルで動いた合意済みスコープ外のファイル (`local-development-setup.md` / `KsSettingsController.cs` の設計変更 / `UnsubscribableCellCollection.cs`) はすべて deviation.md の 24 / 25 / 26 に箇所として現れている。`ios-customcell-initial-height-grow-animation/exploration.md` への追記は tasks 6.3 が名指しした先で、観測そのものは deviation 23 に記録がある
- **handbook 追記の妥当性**: 症状 (「Root の header / footer だけが表示されない」) が本 change の Android 側の保証と整合し、対処 (`bin` / `obj` を捨ててフルビルド → 配備 → 対象画面を 1 度開く) が iOS 節と同型。`kasane/lessons/inbox/device-redeployed-without-clean-build-then-handed-to-owner.md` も、直接証拠が取れていないことを含めて正直に書かれている

## 完了条件の未充足 (コードの指摘ではない)

`kasane/lessons/process.md` の L-003 に従い、実機・Simulator での視覚確認と証跡を変更の完了条件として扱う。**この APPROVED は変更の完了を意味しない。**

- tasks 6.3 のうち **iOS 実機 (iPhone 11) の修正後の観測が未了**。修正前 (1.3) は Simulator・実機とも揃った。6.3 の合否条件は「view accessory と CustomCell の Content が後着していないこと」「1.3 の実機で測った ③ の高さのブレ (最初の 3 フレームだけ 57px 低い) が増えていないこと」で、抵触した場合は tasks 6.3 のとおり完了を止める分岐が残る
- tasks 6.5 (`../ColorAnalyzer` での確認) はリリース後の確認項目としてオーナーへ申し送る方針と申告されている
- 実機の証跡が揃った時点で、6.3 の観測結果を対象に含めた確認が別途必要になる

## アクションプラン

1. (任意・優先度低) `ReleaseCellContentViews` と accessory の書き戻しの耐障害化に回帰テストを 1〜2 件足す。既存の `AFailedAccessoryDisposeDuringRollbackStillReleasesTheHost` と同じ足場で書ける
2. iOS 実機 (修正後) の証跡の追加を待ち、tasks 6.3 の合否条件に抵触していないかを確認する。抵触した場合は完了を止める
3. (任意) `DisconnectHandler` を `RollbackHost` と同じ全件試行の形に揃える
4. (任意) `DisposeRetired` / `DisposeRetiredViews` の手書きを `TryCleanUp` へ寄せる
5. 蒸留時: maui/ADR-0027 に退役順序の例外を、core/ADR-0033 にキャンセルの合図の例外を書き足すかを判断する
6. (任意・別途) 公開 API 面の機械検査 (binary-compatibility-validator) の導入可否を判断する
