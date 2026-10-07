# レビュー結果: android-compose-scroll-restore-with-multiple-hosts (001 回目)

**日付**: 2026-10-07
**判定**: APPROVED

## サマリー

合意スコープ (exploration.md の決定事項、論点 1・2) と deviation.md に記録済みの差分のとおりに実装されている。数える範囲の変更は `KsSettingsView.kt` の判定 1 箇所に閉じ、公開 API を変えていない。Android の全件テストは 3186 件 / 失敗 0 件で、判定を変更前へ戻す改変で新しいテスト 6 件が落ちることを実測した (検出力あり)。Critical / Major は無く、優先度の低い Minor 3 件と Suggestion 1 件を残す。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always)
- `kasane/handbook/cross/test-execution.md` (テストを実行するとき・結果を報告するとき)
- `kasane/handbook/cross/runtime-behavior-verification.md` (スクロール制御の位置の控えと戻しを変更して完了を判定するとき)
- `kasane/handbook/cross/sample-parity.md` (`samples/` を変更するとき)
- `kasane/lessons/code-review.md` (L-001)・`kasane/lessons/process.md` (L-003・L-009 を重点に照合)
- 決定: android/ADR-0021 (accepted)、android/ADR-0023 (proposed — 判定の根拠には使っていない)、cross/ADR-0016、cross/ADR-0027

ロードしたスキル: ksn-review / kotlin-impl-skill / jetpack-compose-impl-skill / csharp-impl-skill / maui-skill

## 確認した観点

**実行した検証**

- Android 全件: `./gradlew test --rerun-tasks` (JDK 21) → BUILD SUCCESSFUL。`kssettingsview` 1400 件 × 2 variant、`kssettingsview-bridge` 193 件 × 2 variant、合計 3186 tests / 0 failures / 0 errors / 0 skipped。対象 3 クラスは `ComposeScrollRecreationTest` 6 件、`ScrollRecreationTest` 6 件、`DateCalendarRecreationTest` 19 件。既知の間欠 (`ComposeScrollControlTest` の待機切れ) はこの実行では出なかった
- 改変による検出力の実測 (L-001。リポジトリの外に置いた写しで行い、作業ツリーは触っていない。写しの復元は SHA-1 の一致で確かめた)

  | 改変 | 落ちたテスト |
  |---|---|
  | A: 判定を変更前 (階層全体で数える) へ戻す | 6 件 — Compose の「1 画面に複数」「進んで戻る」、カレンダーの「入れ物を分けたホスト」、`ScrollRecreationTest` の新しい 3 件 |
  | B: 入れ子の境目の下を数えない処理だけを外す | 1 件 — 「直置きと入れ物を分けた親の下」 |
  | C: 範囲の根を自身からではなく親からたどる | 0 件 (指摘 2) |
  | D: テストの `reissueLayoutRequestsMadeDuringLayout` の呼び出しを外す | 1 件 — 「進んで戻る」(指摘 1) |

- `samples/android` の `:app:compileDebugKotlin` は通る。`samples/maui` のビルドと MAUI のテストは実行していない (本体・facade に差分が無く、サンプルの動作は証跡の静止画で確かめた)
- lint: `comment-policy-lint.py` 禁止 0 件、`local-path-lint.py`・`identity-lint.py` とも検出なし

**仕様充足 (合意スコープ)**

- 論点 1: 範囲の見分け (`KsSettingsView.kt:1405`)・数え方 (`:1420`)・位置とカレンダー選択面の両方に同じ判定 — 充足。決定事項との差 3 点 (外れた後は親が無い / 自身が根 / 入れ子の境目の下は数えない) は deviation.md に記録済み
- 論点 2 のテスト 4 つ: 期待値の反転・居合わせる進む戻る・入れ物を分けた親・同じ入れ物の 2 つは据え置き (`ScrollRecreationTest`・`DateCalendarRecreationTest` の既存 2 本は無変更で残っている) — 充足。追加分は deviation.md に記録済み
- 論点 2 の端末: 場面 1・2・4 の修正前後の A/B (`evidence/device-scroll-restore-ab.txt` と静止画 6 点)。場面 2・4 が「先頭に戻る → 保たれる」に変わっている。静止画の上部バーの操作 2 つは提出コードと対応する
- 論点 2 の MAUI Android: `evidence/maui-android-scroll-restore-ab.txt` と静止画 10 点。修正前後で観測に差が無いという結果と、確かめていない点が deviation.md と証跡の両方に書かれている
- 足場の書き換え: exploration.md は未追跡で履歴が無く、実装中の書き換えの有無は機械的には確かめられない。決定事項と実装の差は deviation.md 側に記録されており、逆流修正の形跡は読み取れなかった
- 合意スコープが名指ししていないファイルへの変更 (L-009): 実装の diff 6 ファイルはすべて決定事項か deviation.md の行に対応する
- 長命層: 実装の diff に `kasane/concepts/`・`kasane/handbook/` の書き換えは無い。追随は「蒸留時に反映」の行に送られている

**テスト**

- 待機は実時間の deadline・`Thread.sleep(1)`・超過時の実測値付き `fail()` の 3 条件を満たす。条件は自分のコードが決める値 (Section の数・処理済み件数・行の位置) で、絞り込みの結果が空のまま通る形は無い (`layoutShowing(...)!!` と `cellRow(...) ?: throw`)
- カレンダーを提示する新しいテストは、同じクラスの既存のヘルパー (`launch`・`openDialog`・`recreate`) だけで組まれており、提示後の待ち方を新しく持ち込んでいない

**設計品質**

- android/ADR-0021 の縮退条件の範囲を変える実装で、accepted の決定と字面では食い違う。これは合意スコープ (論点 1) と proposed の android/ADR-0023 が改訂として扱っており、違反として扱わない。ADR-0021 の他の決定 (既定 ID の自前付与・保存と閉じの分離) は維持されている
- オーバーエンジニアリング: 追加は private 関数 1 つと引数 1 つで、新しい抽象は無い
- コメント: 変更したコメントは現在形で、ADR ID 以外の作業文書への参照は無い。ライブラリの公開 doc コメントへの内部用語の追加は無い (`onSaveInstanceState` は final クラスの protected で、ADR 参照は変更前からある)
- 性能: レイアウトごとの走査は、変更前 (階層全体) と同じか狭い範囲になる
- サンプル: Android は Material 3 の `TopAppBar` の `actions` と `contentDescription` 付きの `IconButton`。MAUI の `ToolbarItem` は `Text` が読み上げに使われ、アイコンの白は `App.cs` の `BarTextColor` と合う。上部バーに置く判断は deviation.md に記録済み (sample-parity の「OS 標準のナビゲーション chrome」の扱い)。iOS の MAUI で未確認であることは既知の事項として受け取っている

## 指摘事項

### [🟡 Minor] 1. 進んで戻るテストの補助 (レイアウト要求の出し直し) が、端末でも同じ状態にならないことを裏づけていない

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/compose/ComposeScrollRecreationTest.kt:177`、`:189`
**問題点**: 補助を外すと (改変 D)、戻った画面は復元を 1 件処理した後、一覧のレイアウト要求が残ったまま先頭の行を表示して止まる (`processed=1 layoutRequested=true rows=[0@22.., ...]`)。これはこの change が直そうとしている症状と同じ見え方である。コメントは「window のレイアウトの外では、`ViewRootImpl` の出し直しが働かない」ことを Robolectric 固有の事情として説明するが、同じファイルの `drawWindow` のコメントは「`AndroidView` の中の View のレイアウトは Compose がフレームの描画の中で反映する」と書いており、端末でも後から足した画面の最初のレイアウトが window のレイアウトの外で走る経路があり得る。その経路で要求が残るかどうかは、テストが補うため検出できない。端末の証跡 (場面 2・4 で位置が保たれた) は最終の見え方を 1 構成で確かめたもので、要求が残らないことの直接の観測ではない。
**推奨修正**: 補助が端末の挙動と等価である根拠を 1 つ残す。たとえば端末で、戻った直後に外からのレイアウトのきっかけ無しで復元が完了することを観測して証跡に足すか、根拠が取れないなら「端末では未確認」とコメントに書き、リリース後に起票元のアプリで確かめる項目 (場面 3 と同じ扱い) に加える。判定を変える指摘ではない — 端末の A/B で場面 2・4 の解消は確認されている。

### [🟡 Minor] 2. 「自身が保存の入れ物の根」のテストが、その判定を外しても落ちない

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ScrollRecreationTest.kt:193`、対象の実装は `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:1405`
**問題点**: 範囲の根を親からたどる形へ改変しても (改変 C)、3 クラス 31 件がすべて通る。View Host を 2 つとも `HostItself` にした構成では、親からたどると根が階層全体になり、入れ子の境目を数えない処理で 2 つとも 0 と数えられて「複数ではない」になるため、結果が同じになる。deviation.md の 2 行目 (自身に設定が付いていれば自身を根にする) を固定するテストとして足したものだが、その差を見分けていない。差が出るのは、自身が根の View Host 1 つと、直置きの既定 id の View Host 2 つが同じ window にある構成である (自身が根の側は保存され、直置きの 2 つは保存されない)。
**推奨修正**: `saveSeparations = listOf(HostItself, null, null)` の形を 1 件足し、1 つめだけがその位置へ戻り、残り 2 つは復元の要求が積まれないことを確かめる。足した後に改変 C で落ちることを実測する。

### [🟡 Minor] 3. Android の端末で、Activity の作り直しを修正後のビルドで観測していない

**該当箇所**: `evidence/device-scroll-restore-ab.txt`
**問題点**: `kasane/handbook/cross/runtime-behavior-verification.md` の「Host の作り直し後の位置」は、Android について Navigation Compose の行き来と、`NavHost` 配下での Activity の作り直し (夜間モードの切り替え) の 2 つの起こし方を挙げる。証跡は前者 (場面 1・2・4) だけで、後者は MAUI Android にしか無い。合意スコープ (論点 2) が端末で確かめる範囲を場面 1・2・4 と決めているため逸脱ではないが、保存の要否の判定を変えた change で、Compose の端末での作り直しが未観測のまま残る。Robolectric の作り直しのテストは通っている。
**推奨修正**: 場面 2 の続き (行き先の画面を表示中) で夜間モードを切り替えてから戻る 1 場面を足すか、確かめていない点として deviation.md に 1 行残す。

### [🔵 Suggestion] 4. 見張りのテストが集めた観測の一部を使っていない

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/compose/ComposeScrollRecreationTest.kt:453`、`:483`
**問題点**: `SaveProbeView` は外れた後の保存の時点の親の並び (`savesAfterDetach`) と `ancestorCount` を控えるが、アサーションに使うのは外れる時点の `nearestSaveBoundary` だけで、`savesAfterDetach` は待機条件で「空でない」ことだけを見ている。deviation.md の 1 行目の事実 (保存を求められた時点で親が 1 つも無い) はテストに現れない。
**推奨修正**: 実装がその事実に頼らない (親が残っていても正しく数える) なら `ancestorCount` を削る。事実の変化を知りたいなら `savesAfterDetach` の `ancestorCount` を前提として確かめる。どちらでもよい。

## 所見 (指摘ではない)

- コードとテストのコメントが参照する android/ADR-0023 は proposed である。蒸留で accepted にならなかった場合は、コメントの参照も合わせて直す必要がある
- 起票元の表の 3 つめの場面 (タブ) と、iOS の MAUI でのサンプルの表示は未確認のまま残る (既知の事項として受け取っている)

## アクションプラン

1. 指摘 2: `HostItself` と直置き 2 つの構成のテストを 1 件足す (数行で閉じる)
2. 指摘 1: 補助の根拠を端末の観測で残すか、未確認として記録し、リリース後の確認項目に加える
3. 指摘 3: 作り直しの 1 場面を足すか、未確認として deviation.md に残す
4. 指摘 4: 任意
