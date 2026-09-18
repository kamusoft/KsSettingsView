# レビュー結果: android-accessory-view-late-insert-animation (006 回目)

**日付**: 2026-09-18
**判定**: APPROVED

## サマリー

オーナー裁定で範囲を絞った確認レビュー。前回 (review-005 / second-opinion-code-005 の Major 1) が指した 1 点 —「退役の知らせに失敗した実体を Native Host の解放より前に破棄してしまう」— は解消している。書き戻し・内容なし配信に失敗した実体だけが `deferred` へ預けられ、`_gateway.ReleaseHost()` の後に破棄される形になり、**新規テスト 2 件はミューテーション実測で検出力を確認した** (修正を元へ戻すと、破棄の時機を見ている争点のアサーションだけが落ち、前提の破棄件数は通る)。正常系の順序と既存テストは壊れておらず、MAUI **589 件全成功** (前回 587 → +2)・全 TFM ビルド 0 警告 0 エラー・標準 lint 禁止 0 件。この変更自体が持ち込んだ新しい欠陥は見つからなかった。

指摘は 0 件。対象外と裁定された領域で気づいた 2 点は下の「起票先への申し送り」に分けて書く。

**この APPROVED は変更の完了を意味しない** — tasks 6.3 (iOS 実機・修正後) と 6.5 が開いたままである。

## レビュー対象

オーナー裁定 (deviation.md の「レビューの収束」の行) に従い、前回以降に動いた次の範囲だけを対象にした。

- `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs` の `ReleaseHost` / `ReleaseAccessoryViews` / `ReleaseCellContentViews`
- `maui/KsSettingsView.Maui.Tests/AccessoryViewTests.cs:1120` (新規 1 件) / `maui/KsSettingsView.Maui.Tests/CustomCellContentTests.cs:477` (新規 1 件)
- `maui/KsSettingsView.Maui.Tests/Fakes/FakeSettingsGateway.cs` の失敗切り替え (`UpdateAccessoryFails` / `ReplaceCellFails`)
- `deviation.md` の追記 (27 / 28 / 29)

範囲の裏取り: 前回レビュー出力より後に更新されたファイルはこの 5 本だけで、Android / iOS の `*.kt` / `*.swift` は 1 件も更新されていない (前回の出力時刻を基準にした更新時刻の比較)。指示どおり Android / iOS のテストは再実行していない。足場アーティファクト (proposal / design / specs / tasks) の書き換えは無い — `tasks.md` も前回から動いていない。

対象外 (別 change `kasane/changes/maui-teardown-failure-safety` へ起票済み): 通常切断の `DisconnectHandler` の逐次実行、Native Host の解放も失敗した場合の実体の保持先、手書き try の重複、`DisposeRetiredViews` の平坦化、その他のより深い多重障害。これらは指摘に数えていない。

## 実行結果

| 対象 | コマンド | 結果 |
|---|---|---|
| MAUI テスト | `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` | **589 tests / 0 失敗 / 0 スキップ** |
| MAUI ビルド (全 TFM) | `dotnet build maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj` | net10.0 / net10.0-android / net10.0-ios とも **0 警告 0 エラー** |
| lint | `scripts/local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` | 禁止 **0 件** (comment-policy は 808 ファイル検査) |

前回 587 → **589 (+2)**。増分は本サイクルの新規テスト 2 件と一致する。`doc-structure-lint.py` は本サイクルが `kasane/concepts` / `kasane/handbook` / `kasane/roadmaps` を触っていないため対象外。

## 検出力の実測 (lessons code-review L-001)

制約によりプロジェクトのコードは触れないため、`maui/` ツリーを scratchpad へ複製し (`bin` / `obj` 除外)、**複製側だけにミューテーションを入れて実測した**。複製の基準実行は 589 件成功で本体と一致し、実測後に複製を原状復帰して本体との shasum 一致を確認している (本体ファイルの更新時刻も変わっていない)。

| ミューテーション (複製側) | 結果 |
|---|---|
| `ReleaseAccessoryViews`: 書き戻し失敗時に `deferred` ではなく即時破棄へ戻す | **1 件失敗**。`AccessoryWhoseWriteBackFailedIsDisposedAfterTheNativeHostIsReleased` が、破棄の瞬間の Host 解放回数 期待 1 / 実際 0 で落ちる (破棄件数のアサーションは通る) |
| `ReleaseCellContentViews`: 配信失敗でも全リースを即時破棄する | **1 件失敗**。`ContentWhoseReleaseDeliveryFailedIsDisposedAfterTheNativeHostIsReleased` が同じ形で落ちる |
| `ReleaseAccessoryViews`: 最初の書き戻し失敗で投げ直す (全件試行をやめる) | **1 件失敗**。集約例外の件数 2 → 1、2 件目と root の実体が破棄されないことで落ちる |
| `ReleaseCellContentViews`: 配信成功側のリース破棄ループから `try` を外す | **全 589 件成功 (検出できない)** — 下の申し送り 1 |

落ちたものはいずれも前提アサーション (破棄件数・例外の型) ではなく争点のアサーション (破棄の時機・全件試行) で、トートロジーではない。テストは `FakeViewLease.OnDispose` の中で `GatewayCall.ReleaseHost` の記録件数を読む形で時機を観測しており、`Dispose` の内側から読むため順序の証人として正しく機能している。

## 照合した規約

- `cross/comment-policy.md` (always) — 本サイクルの新規コメント (`ReleaseHost` / `ReleaseAccessoryViews` / `ReleaseCellContentViews` の remarks と `deferred` の param、`FakeSettingsGateway` の 2 プロパティ、新規テスト 2 件の summary / remarks) を節ごとに照合。許容参照 (`maui/ADR-0016` の ID 形式)・禁止参照 (作業文書のパス・change 名・ローカル通番・行番号)・禁止する記述類型 (履歴記述・過去仕様の説明・デルタスペック構文キーワード) のいずれにも違反なし。コメントは単独で読んで意味が通る (「知らせが届いていない実体は native がまだ子として抱えている可能性がある」まで自己完結して書かれている)
- `cross/comment-policy.md` の「公開メンバーの doc コメント」 — `comment-policy-lint.py --advisory` の要確認は `KsSettingsController.cs` の 5 件で、本サイクルが増やしたのは `:397` (`ReleaseHost` の remarks 中の `maui/ADR-0016`) 1 件。`KsSettingsController` は `internal sealed class` (`:24`) で公開メンバーではなく、検査の可視性推定の限界による報告。規約違反ではない
- `cross/test-execution.md` (テストを実行・報告するとき) — MAUI は末尾集計行を報告。新規 2 件に固定時間の待機は無く、非同期の収束を待つ要素も無い (同期経路)。Android / iOS は差分不変を確認したうえでの再実行省略 (指示による)
- `cross/runtime-behavior-verification.md` / `cross/sample-parity.md` / `cross/diagnostic-message-language.md` — 本サイクルで触れた範囲に該当なし (新規の例外文字列はテスト用 fake の中のみで、英語・現在形)
- `maui/integration-host-verification.md` — C# → Native の疎通の形は変わっておらず、既存の証跡で足りる
- `maui/ADR-0016` (accepted) — 「Store 更新 → native への配信 → 旧 wrapper 破棄」の順序と、ADR 本文の「Handler 切断時: Section 系は `updateAccessory` の書き戻しで stale closure を除去する / **Root 系は Store に無いため書き戻し不要**」の箇条を、実装の 3 メソッドと突き合わせた。下の「欠陥が見つからなかった範囲」に照合の中身を書く
- `core/ADR-0033` / `maui/ADR-0027` はいずれも `proposed`。これらを根拠にした指摘は出していない

ロードした code-review スキル: kotlin-impl-skill (android ドメイン)。本サイクルの差分は MAUI (C#) のみのため適用箇所なし。

## 前回指摘の解消状況

| 出所 | 指摘 | 状況 |
|---|---|---|
| second-opinion-code-005 Major 1 | 書き戻し・内容なし配信に失敗した実体を、Native Host の解放より前に破棄している (maui/ADR-0016 の順序違反) | **解消**。`ReleaseAccessoryViews` は書き戻しが失敗した位置の実体だけを `deferred` へ預け、`ReleaseCellContentViews` は配信が失敗したときに退役させた実体をすべて預ける。`ReleaseHost` が `_gateway?.ReleaseHost()` の後に `deferred` を破棄する。ミューテーション実測で検出力を確認 |
| second-opinion-code-005 Major 1 の推奨 3 点目 | gateway の書き戻し・一括配信を失敗させるテストを足す | **解消**。`FakeSettingsGateway.UpdateAccessoryFails` / `ReplaceCellFails` と新規テスト 2 件。単発 (`ReplaceCell`) と一括 (`ReplaceCells`) の両方に切り替えが効く |
| review-005 Minor のうち accessory の書き戻し側 | 書き戻しの全件試行がテストで固定されていない | **解消**。新規テストが Section 2 つの書き戻しを両方失敗させ、集約例外 2 件・2 件目の実体の破棄・root の実体の破棄まで見ている。最初の失敗で投げ直すミューテーションで検出を確認 |
| review-005 Minor のうち `ReleaseCellContentViews` の破棄側 | リース破棄の全件試行がテストで固定されていない | **未解消**。配信失敗の経路は固定されたが、配信成功後の破棄ループの `try` を外すミューテーションは今も検出されない。オーナー裁定により本 change の対象外のため指摘に数えず、下の申し送り 1 とする |
| second-opinion-code-005 Major 2 / review-005 Suggestion 1 | 通常切断の `DisconnectHandler` の逐次実行 | **対象外** (`kasane/changes/maui-teardown-failure-safety` へ起票済み) |
| review-005 Suggestion 2 / 3 | 手書き try の `TryCleanUp` への寄せ / 蒸留時の ADR 書き足し | **対象外** (前者は別 change、後者は蒸留時の判断) |

## 指摘事項

なし (Critical 0 / Major 0 / Minor 0 / Suggestion 0)。

## 今回あらためて見て、欠陥が見つからなかった範囲

- **正常系の順序**: 失敗が無いとき、`ReleaseAccessoryViews` は「全リースを外す → 全件の書き戻し → 全件の破棄」、`ReleaseCellContentViews` は「全リースを外す → 内容なし世代を 1 回で配信 → 全件の破棄」で、`deferred` は空のまま。`ReleaseHost` の 4 単位の実行順 (accessory → Cell の内容 → 通知の解除 → Native Host) も前回から変わらない。maui/ADR-0016 の退役順序を語る既存テスト (`SectionAccessoryIsWrittenBackAsTextWhenTheHostIsReleased` / `PreviousViewIsDisposedAfterTheRebuiltRootIsDelivered` / `PreviousContentIsDisposedAfterTheRebuiltRootIsDelivered` 等) は全件通っている
- **root の実体を解放より前に破棄することの妥当性**: 新規テストは `releasesWhenRootDisposed` が 0 (= Host 解放より前の破棄) を期待として固定している。これは maui/ADR-0016 が「Root 系は Store に無いため書き戻し不要」と明記した区別と一致する。Section の accessory は Store に残る stale closure が破棄済み実体を指し続けるのが危険の実体であり、Store に載らない root にはその窓が無い。過剰な一般化 (root まで遅延させる) を避けた判断として正しい
- **知らせが届かなかった実体を native が使い続ける窓**: 書き戻しに失敗した Section の実体は `deferred` に入るため、後続の `ReleaseCellContentViews` の配信で native が再バインドしても、まだ生きている実体を掴む。破棄は Host 解放の後になる。これがこの修正の狙いどおり成立している
- **例外の入れ子と件数**: `ReleaseAccessoryViews` / `ReleaseCellContentViews` が投げる `AggregateException` は `TryCleanUp` が 1 件として集め、`ReleaseHost` の `Flatten()` で平坦化される。新規テストが `InnerExceptions` の件数 (2 / 1) を直接見ており、入れ子で件数が崩れないことが固定されている
- **`deferred` の取りこぼし**: `ReleaseAccessoryViews` / `ReleaseCellContentViews` の呼び出しはいずれも `ReleaseHost` の 1 箇所のみ。`deferred` は 4 単位の後に必ず走る破棄ループを通り、どの経路でも破棄されずに捨てられることはない
- **二重破棄**: `TakeLease` が置き場所からリースを外してから預けるため、`deferred` の実体が再び `_accessories` / `_cellContents` から拾われる経路は無い。`disposable` と `deferred` はループの各回でどちらか一方にしか入らない
- **`_gateway` が無い切断**: `WriteBackAccessoryText` は `_gateway is not null` を確かめてから呼ぶため、gateway 不在では書き戻しが「失敗」にならず、実体は従来どおり解放より前に破棄される (知らせるべき相手がいない)。`TryResolveSectionId` が解決できない Section も同様で、native が id を知らない = 配信されていない位置のため取り違えにならない
- **再接続後の整合**: 書き戻しが失敗した Section は View プロパティを保ったままなので、再接続時の配信で新しい実体が載り、stale closure は上書きされる
- **fake の素性**: `FakeViewLease.Dispose` は `DisposeCount++` → Handler 切断 → `OnDispose` の順で、観測処理が破棄の内側で走る。`UpdateAccessoryFails` / `ReplaceCellFails` はいずれも既定 false で、既存 588 件への影響が無いことは全件成功で裏付けられている。`ReplaceCellFails` が単発と一括の両方を塞ぐため、内容の配信が 1 件のときも複数件のときも同じ経路を踏める
- **deviation の網羅 (lessons process L-009)**: 本サイクルで動いた合意済みスコープ外のファイルは `KsSettingsController.cs` (設計変更) と `FakeSettingsGateway.cs` (付随修正) で、どちらも deviation.md の 29 / 28 に箇所として現れている。新規テスト 2 件は 29 の設計に対する回帰テストで、足場の逸脱ではない
- **足場の不可侵**: specs / proposal / design / tasks のいずれも本サイクルで書き換えられていない

## 起票先への申し送り (指摘ではない)

いずれも `kasane/changes/maui-teardown-failure-safety` の「残っている指摘」へ足す候補。オーナー裁定により本 change では扱わない。

1. **`ReleaseCellContentViews` の破棄ループの全件試行が、今もテストで固定されていない** (`maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1229`)。配信が成功した後にリースの破棄が失敗する経路で、`TryCleanUp` を素の `Dispose()` へ戻しても 589 件すべて通る (上の実測 4 行目)。review-005 の Minor のうち、本サイクルで解消しなかった半分にあたる。同ファイルの accessory 側 (`:814`) は既存テストで固定されている
2. **`ReleaseCellContentViews` のリースを外すループの途中で例外が出ると、取り出し済みの実体が取りこぼされる** (`maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1196`-`1206`)。`IssueContentToken` が投げた場合、そこまでに `TakeLease` した実体は `retired` にあるだけで `deferred` にも破棄ループにも届かない。`ReleaseAccessoryViews` の取り出しループ (`:777`-`783`) も同じ形。現実性は低いが、起票先が掲げる「後片付け全体の方針」の対象に入る

## 完了条件の未充足 (コードの指摘ではない)

`kasane/lessons/process.md` の L-003 に従い、実機・Simulator での視覚確認と証跡を変更の完了条件として扱う。**この APPROVED は変更の完了を意味しない。**

- tasks 6.3 のうち **iOS 実機 (iPhone 11) の修正後の観測が未了** (指示により `evidence/` は今回の対象外。別ワーカーが追加中と申告されている)。合否条件は「view accessory と CustomCell の Content が後着していないこと」「1.3 の実機で測った折り返す FooterView の高さのブレが増えていないこと」で、抵触した場合は tasks 6.3 のとおり完了を止める分岐が残る
- tasks 6.5 (`../ColorAnalyzer` での確認) はリリース後の確認項目としてオーナーへ申し送る方針と申告されている
- 実機の証跡が揃った時点で、6.3 の観測結果を対象に含めた確認が別途必要になる

## アクションプラン

1. iOS 実機 (修正後) の証跡の追加を待ち、tasks 6.3 の合否条件に抵触していないかを確認する。抵触した場合は完了を止める
2. 上の申し送り 2 件を `kasane/changes/maui-teardown-failure-safety` の「残っている指摘」へ足す
3. 蒸留時: maui/ADR-0027 に退役順序の例外を、maui/ADR-0016 に「知らせが失敗した実体は Host 解放の後に破棄する」旨を、core/ADR-0033 にキャンセルの合図の例外を書き足すかを判断する
