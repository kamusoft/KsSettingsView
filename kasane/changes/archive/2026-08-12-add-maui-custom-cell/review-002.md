# レビュー結果: add-maui-custom-cell (002 回目)

**日付**: 2026-08-12
**判定**: APPROVED

## サマリー

review-001 と second-opinion-code-001 の突き合わせで確定・採用された 5 件 (ホスト Major 1 / 相方 Major 2 / 相方 Major 3 / 相方 Minor 1 / 相方 Minor 2) はいずれも解消を確認した。とくにホスト Major 1 の証跡取得は、MAUI Android で content 上の行タップが不発になる**実欠陥の発見と修正**へつながっており、修正には実タッチ配送を通す Robolectric テスト 5 件と欠陥時・修正後の実機証跡が付いている (L-003 が想定した「実機確認が別の実欠陥の発見につながる」型そのもの)。3 プラットフォームのビルド・テストは全件成功、comment-policy lint も 0 件。

修正が新たな問題を持ち込んでいないかを、指摘対象になった 3 つの機構 (validateValue guard / バッチ事前検査 / Android の detectTapGestures 常設) について実測で確かめた。**validateValue から例外を投げる形は、MAUI の「false を返す」規約から外れるが、この用途では妥当**と判断する (根拠は下記「確認した観点」)。新規指摘は Minor 1 件・Suggestion 1 件で、いずれも本変更の可否を左右しない。

## 前回指摘の解消確認

| 前回指摘 | 状態 | 確認内容 |
|---|---|---|
| ホスト Major 1 (Android タップ操作証跡なし) | **解消** | `screenshots/android-tap-01〜13` に欠陥の記録 (MAUI で 2 タップしてもカウンタ 0 回 / native は同操作で 2 回) と、`android-tapfix-01〜09` に修正後の証跡。`android-tapfix-02` = content 上タップでカウンタ「2 回」、`-03` = ピルタップでカウンタ「0 回」(二重発火なし)、`-07` = タップ購読なしの行でスライダーが 40 → 81 へ動く。ユニットテストも `performClick()` ではなく実 `MotionEvent` を `itemView.dispatchTouchEvent` へ流す形 (`KsBridgeCustomCellTest.kt` の `touchContent`) へ変わり、消費関係が測定対象に入った |
| 相方 Major 2 / ホスト Minor 1 (バッチの View 重複が native 更新後) | **解消** | `KsSettingsController.cs:2100` `EnsureCellsAreNotPlaced` にバッチ内 `HashSet<View>`、`:1418` `RebuildSectionCells` で `EnsureCellContentsAreFree` を登録解除・gateway 呼び出しより前へ前倒し。`AddingCellsThatShareAContentViewThrowsBeforeAnyInsert` / `ResettingTheCellsWithADuplicateContentThrowsWithoutTouchingTheCurrentCells` が「gateway 呼び出し 0 件・対応表未汚染・既存行の ID/lease/論理親が維持」まで確認している。`ReplaceCells` (`:1233`) と `ReplaceSections` (`:1128`) も同じ事前検査を通る |
| 相方 Major 3 (Content 差し替え失敗後の状態分離) | **解消** | `CustomCell.ContentProperty` の `validateValue` + `IKsCellContentGuard` により値確定前に弾く。`AFailedContentReplacementKeepsTheCurrentContent` / `...WithAnAccessoryViewKeepsTheCurrentContent` が公開値・論理親・世代・lease・引き当ての全維持を確認。ロールバック不成立を回避する判断も妥当 (下記参照) |
| 相方 Minor 1 (バッチ配信テストの payload) | **解消** | `GatewayCall.CellUpdate` が呼び出し時点の `Snapshot` と `ContentView` を持つ不変記録になり、`AttachDeliversEveryContentInASingleBatch` が各件の `ContentToken` と実体を、`HostReleaseDeliversEveryContentInASingleBatch` が「実体なし・別世代」を件ごとに検証している |
| 相方 Minor 2 (iOS 再接続証跡) | **解消** | `ios-final3-reconnect-01〜04` + `ios-final3-specific-05-content-swapped-B` (20:52〜20:53)。`-03-record` に「1 回目の再接続後／離脱中の Handler: 切断／離脱中に Content も差し替え」の記録が写っている |

降格済みの相方 Major 1 (delegate 必須メソッド) と対処不要裁定の Suggestion 2 件は本レビューの対象外とした。deviation.md の 3 件も合意済みとして扱った。

## 確認した観点と実測結果

- **ビルド**: `dotnet build KsSettingsView.Maui` を `-f net10.0` / `net10.0-ios` / `net10.0-android` の 3 TFM で実行、いずれも 0 警告 0 エラー
- **テスト**: MAUI `dotnet test` = **400 tests / 0 failures** (前回 395 → +5)、iOS `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` = **476 tests / 0 failures**、Android `./gradlew test --rerun-tasks` = **2320 tests / 0 failures** (前回 2310 → +10 = 新規 5 件 × debug/release。`build/test-results/*/TEST-*.xml` 集計)
- **規約**: `python3 scripts/comment-policy-lint.py --summary` = 禁止 0 件 / 596 ファイル
- **足場凍結**: `git status` で足場の変更は tasks.md のチェックのみ。specs/ ・design.md ・proposal.md は無改変
- **証跡と提出コードの対応 (L-003(4))**: 今サイクルのコード更新時刻は C# 側が 20:40〜20:47 (`IKsCellContentGuard.cs` / `CustomCell.cs` / `KsSettingsController.cs`)、Android bridge 側が 21:06〜21:21 (`KsBridgeCustomCell.kt` / `build.gradle.kts` / `KsBridgeCellContentView.kt`)。証跡はそれぞれ後の時刻に撮られている — `ios-final3-*` (20:52〜20:53) は C# 変更後、`android-tapfix-*` (21:28〜21:31) は Android bridge 変更後。前サイクルの `ios-final2-*` (19:43〜20:09) は今回の C# 変更より前だが、今回の変更 (値確定前検査・バッチ事前検査・検査順序の前倒し) はいずれも多重配置の**例外経路**にしか効かず正常系の描画を変えないため、表示系の証跡としてなお有効

### validateValue から例外を投げる形の妥当性 (依頼された批判的評価)

**結論: この用途では妥当。** ただし MAUI の文書化された契約から外れている点は事実なので、意図の明示を Suggestion として出す。

一時プローブ (レビュー後に削除、`shasum` で原状復帰確認済み) で 3 経路を実測した:

| 経路 | 結果 |
|---|---|
| Binding 経由 (`SetBinding(ContentProperty, ...)` の元 VM 値を重複 View に変更) | `InvalidOperationException` が VM の setter まで伝播し、`target.Content` は null のまま、置かれている側の `Parent` も維持 — **guard は binding 経路でも働く** |
| `SetValue` 直接呼び出し | 同上、`InvalidOperationException` |
| 例外の後に同じ Cell へ有効な View を設定 | **正常に確定** (`Content`・論理親・platform 実体の引き当てすべて成立) — 例外で `BindableObject` が半端な状態 (再入 set の遅延キューが drain されないまま等) に固まることはない |

妥当と判断した根拠:

1. MAUI は `validateValue` を**値の確定 (`context.Value` の書き込み) より前**に呼ぶため、例外時点で公開値も論理所有も未変更。実装者が挙げた「ロールバックが成立しない」問題を構造的に回避できる唯一の seam である
2. `false` を返す形では MAUI が `ArgumentException` を投げるため、spec が要求する `InvalidOperationException` を満たせない。規約どおりに戻り値で拒否する選択肢は spec と両立しない
3. 例外の後も Cell が使い続けられることを実測済み (上表 3 行目)。副作用は検査のみで、`validateValue` に期待される「副作用を持たない」性質は保たれている
4. 依存している順序 (validate → 確定 → propertyChanged) は `AFailedContentReplacementKeepsTheCurrentContent` が回帰として固定している

### Android の detectTapGestures 常設が既存挙動へ与える影響

- **内容の中の操作を妨げない**: `pointerInteropFilter` が Initial パスで埋め込み View へ配送し、View が引き取ると変更が消費済みになるため `awaitFirstDown(requireUnconsumed = true)` が始動しない。`内容がタッチを引き取ると行タップは通知されない` / `タップ購読なしの行でも内容の中の操作は妨げられない` がこれを固定し、実機証跡 `android-tapfix-07` (購読なしの行でスライダーが 40 → 81) と `-05` (購読なしの行で content 内の展開が動く) が裏付けている
- **二重発火なし**: 行の `OnClickListener` は `AndroidComposeView.dispatchTouchEvent` が true を返す限り呼ばれないため、content 上のタップで両経路が同時に走ることはない。テストは 1 タッチにつき通知 1 件 (`listOf(dto.cellID)`) を検証している
- **無効行**: `android-tapfix-06` (無効スライダーがドラッグで動かない) とテスト `無効な行では内容の上のタッチで行タップが通知されない`
- **「購読の有無によらず常設する」理由の実証**: コメント (`KsBridgeCellContentView.kt:47-48`) が主張する「購読の切り替えで modifier の構成が変わると埋め込みが作り直される」を検証するため、`pointerInput` を `currentRowTap != null` の条件付きに変えるミューテーションを一時適用したところ、`タップ購読の有無は再発行で切り替わる` が `KsBridgeCustomCellTest.kt:521` (`購読の切り替えで埋め込みは作り直されない`) で失敗した。**コメントの理由は正しく、このテストは実際の検出力を持つ** (原状復帰は `shasum` 一致と再テスト green で確認)
- **Compose 依存の追加**: bridge モジュールが `compose = true` + `foundation` / `ui` / `runtime` を取り込む。利用者の依存フットプリントは増えるが、`AndroidView` による埋め込みは proposal / design で織り込み済みの方式であり、native UI 側 (`ks-settingsview-ui`) の CustomCell 経路には影響しない

### guard の着脱

`RegisterCell` (`:1973`) で装着、`UnregisterCell` (`:1986`) と `ClearRegistrations` (`:1014`) で解除。`_cellEntries` から Cell が外れる経路は `UnregisterCell` と `_cellEntries.Clear()` の 2 つだけで、`RemoveCells` / `ReplaceCells` / `RebuildSectionCells` / `ReplaceSectionKeepingCellIds` / `UnregisterSection` / `RebuildRoot` / `Disconnect` はいずれもそのどちらかを通る — **着脱漏れは見つからなかった**。`ReleaseHost` は登録を保つ設計であり、Handler 切断中も多重配置検査が効き続ける点も意図どおり。`ACellDroppedByARootRebuildStopsConsultingThePlacement` が解除側を固定している。

## 指摘事項

### [🟡 Minor] 値確定前の検査が Content 側だけで、accessory 側は公開値と表示が食い違ったまま残る

**該当箇所**: `maui/KsSettingsView.Maui/Section.cs:48-56` (`HeaderViewProperty` / `FooterViewProperty`) / `maui/KsSettingsView.Maui/SettingsView.cs:62-79` (`RootHeaderViewProperty` / `RootFooterViewProperty`) / `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:456` (`SetAccessoryView`)

**問題点**:
`CustomCell.Content` に accessory の View を入れる方向は guard で値確定前に弾かれるようになった (`AFailedContentReplacementWithAnAccessoryViewKeepsTheCurrentContent` が維持を確認済み) が、**逆方向 — accessory に CustomCell の Content を入れる — には guard がなく、公開値だけが確定してしまう**。一時プローブで実測した結果:

- `section.HeaderView = <CustomCell の Content として置かれている View>` → `InvalidOperationException` は送出されるが、`section.HeaderView` の値は**拒否された View に変わっている**。表示・lease・論理所有は旧 header のまま (`accessoryView = set` / `header.Parent = Section` / `content.Parent = cell`)
- `view.RootHeaderView = <同上>` も同じ結果 (`RootHeaderView` が拒否された View、`header.Parent = SettingsView`)

つまり「公開プロパティが指す View」と「実際に表示されている View」が食い違った状態で残る。相方 Major 3 が Content 側について指摘した状態分離の、方向違いの残りである。論理所有は失われないため Major 3 より軽く、同じ値を設定し直せば復帰できるが、対称であるべき 2 操作が非対称になっている。

なお accessory 同士の重複 (`section.HeaderView = <別 Section の HeaderView>`) は本変更以前から同じ形であり、本変更が新規に持ち込んだのは「accessory ↔ Content の交差」という新しい発火経路のみ。spec は例外送出しか要求していないため spec 違反ではない。

**推奨修正**:
`IKsCellContentGuard` と同型の口 (または同一インターフェースの拡張) を `Section` / `SettingsView` の accessory プロパティにも差し込み、`HeaderViewProperty` / `FooterViewProperty` / `RootHeaderViewProperty` / `RootFooterViewProperty` の `validateValue` で `EnsureAccessoryViewIsNotPlaced` を先に通す。テストは `AFailedContentReplacementWithAnAccessoryViewKeepsTheCurrentContent` の鏡像として、accessory へ Content の View を設定して例外になった後も `HeaderView` の値・表示・lease が変わらないことを確認する形。accessory 同士の重複も同じ手当で揃う。

本変更のスコープ (maui-cells の CustomCell 追加) の外側に踏み出す修正になるため、別 change として起こす選択も妥当と考える。

### [🔵 Suggestion] validateValue から例外を投げる選択の理由がコード上に残っていない

**該当箇所**: `maui/KsSettingsView.Maui/CustomCell.cs:44-51` (`ContentProperty` の `validateValue`) / `maui/KsSettingsView.Maui/Internals/IKsCellContentGuard.cs`

**問題点**:
`validateValue` は MAUI では「妥当なら true / 妥当でなければ false を返す」デリゲートであり、false を返した場合は MAUI 自身が `ArgumentException` を投げる。ここでは常に true を返し、代わりに `EnsureContentCanBePlaced` から `InvalidOperationException` を投げている — 規約から外れた使い方である。

`ContentProperty` の XML doc は「なぜ値の確定より前に検査するのか」は説明しているが、**なぜ戻り値ではなく例外なのか** (= spec が `InvalidOperationException` を要求しており `false` では `ArgumentException` になってしまう) と、**MAUI が validate を値の確定より前に呼ぶ順序に依存していること**は書かれていない。この 2 点を知らずに読むと「規約違反の書き方」に見え、後から `return false` へ「直される」余地が残る。

**推奨修正**:
`ContentProperty` の `<remarks>` に 1〜2 文を足す — (1) 戻り値で拒否すると `ArgumentException` になり公開契約の `InvalidOperationException` と食い違うため例外を送出していること、(2) 依存しているのは「validate → 値の確定 → propertyChanged」の順序であり、それは `AFailedContentReplacementKeepsTheCurrentContent` が回帰として固定していること。コード変更は不要。

## アクションプラン

1. **[Minor]** accessory 側にも値確定前の検査を入れる (別 change として起こす選択も可)。本変更のマージを止める理由にはしない
2. **[Suggestion]** `ContentProperty` の `<remarks>` に validateValue を選んだ理由と依存している順序を 1〜2 文追記する

いずれも Critical / Major ではなく、本変更はこのまま次工程 (verify / 蒸留) へ進めてよい。
