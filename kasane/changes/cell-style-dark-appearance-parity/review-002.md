# レビュー結果: cell-style-dark-appearance-parity (002 回目)

**日付**: 2026-09-06
**判定**: CHANGES_REQUESTED

## サマリー

修正サイクル 1 で閉じるはずだった 4 件のうち 3 件 (待機の条件ベース化・Checkbox / Entry の消費点テスト・handbook の完了条件) は閉じている。`DSLCellStyleUpdateTest.awaitTitleColor` は handbook `cross/test-execution.md` の 3 条件 (述語 + 実時間 deadline + 実行機会の譲渡 + 超過時の実測値付き fail) をすべて満たし、Checkbox の `buttonTintList` と Entry の `highlightColor` の 2 本はミューテーション probe で検出力を実測した (消費点を素の `toArgb()` へ戻すと **この 2 本だけが落ちる**)。一方、指摘 3 (行が作り直されていないことの観測) に足された `assertSame` は、**名指しされた退行を検出しない**ことを probe で実証した — style だけの更新が remove + insert へ退行しても両テストは緑のまま通る。合格を主張する assert が実際には何も保証していないため、これを Major として差し戻す。ViewHolder 2 ファイルの再構成は兄弟 4 ファイルと同じ変換パターンのみで、欠落も余分も無い。Android 2854 件 / 失敗 0、MAUI 520 件 / 失敗 0 を独立に再実行して確認した。

## 実行したテスト

| platform | コマンド | 結果 |
|---|---|---|
| Android | `cd android && ./gradlew test --rerun-tasks` | 2854 件 / 失敗 0 / エラー 0 (`*/build/test-results/test{Debug,Release}UnitTest/TEST-*.xml` の `tests` / `failures` / `errors` 合計) |
| MAUI | `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` | 520 件 / 失敗 0 |
| iOS | 未実行 | 修正サイクル 1 で `ios/` 配下のファイルは 1 件も変更されていない (作業ツリーの更新時刻で確認: 最終更新は review-001 より前)。verify-001 の 1027 件 / 失敗 0 が有効 |

## 照合した規約

| 文書 | 適用のきっかけ | 判定 |
|---|---|---|
| `kasane/handbook/cross/comment-policy.md` (always) | 常時 | 適合。`python3 scripts/comment-policy-lint.py --advisory` は禁止 0 件 / 要確認 164 件で、本サイクルが触った 5 ファイル (新規テスト 3 本・ViewHolder 2 本) はいずれも要確認にも挙がらない。新規テストのコメントに change 名・レビュー通番・タスク番号の裸参照は無い |
| `kasane/handbook/cross/test-execution.md` | テスト実行・結果報告 / 収束を待つアサーション | 「収束を待つアサーション」節は適合 (下記「確認した観点」参照)。件数の報告は tasks 8.1 が修正サイクル後の値に追随しておらず Minor 2 |
| `kasane/handbook/maui/integration-host-verification.md` | `maui/` の facade に触れ end-to-end 疎通を確認するとき | 適合。「期待される表示」への追記 (`:116`) に加え、完了条件へ「表示したまま外観を切り替えると外観追随ボタンの title 色が両 OS で切り替わる」(`:125`) が入り、timestamp も更新されている。指摘 4 は閉じた |
| `kasane/lessons/code-review.md` L-001 | 回帰検出力が争点になったとき | 適用した。Major 1 と「確認した観点」の判断はいずれも静的読解ではなく probe の実測に基づく |
| `kotlin-impl-skill` (config `domain-skills.android.code-review`) | Android ドメインの実装レビュー | 本サイクルの差分に言語レベルの所見なし (拡張関数 1 本への集約・null 安全化はすでに review-001 で確認済み) |

## 指摘事項

### [🟠 Major] ViewHolder 同一性の assert は「行が作り直された」退行を検出しない (probe で実証)

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/compose/DSLCellStyleUpdateTest.kt:73-77` / `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/StoreCellStyleReplaceTest.kt:93-97`

**問題点**: 両テストに足された `assertSame(holderBefore, labelRowHolder())` は、失敗メッセージで「style だけの更新で行の ViewHolder が作り直されている（identity が維持されていない）」と主張するが、その退行が起きても落ちない。

ミューテーション probe で実測した。`ui/KsSettingsListAdapter.kt:326` の `areItemsTheSame` の CellRow 分岐を `oldItem.cell.id == newItem.cell.id` から `oldItem.cell == newItem.cell` へ差し替えると、style だけの更新は DiffUtil から見て別アイテムになり、行は remove + insert として作り直される。この状態で `DSLCellStyleUpdateTest` と `StoreCellStyleReplaceTest` を実行すると **2 件とも成功する** (`BUILD SUCCESSFUL`、結果 XML で 2 クラス各 1 件・失敗 0 を確認)。差し替えた 1 行は backup からの復元後に shasum 一致と `git status` の差分 0 で原状復帰を確認済み。

緑のまま通る理由は 2 段ある。いずれも実装側の正しい設計であり、テストの観測点の選択が誤っている:

- remove + insert では旧 ViewHolder が `RecycledViewPool` へ戻り、同じ viewType の insert がそこから同一インスタンスを引き当てる
- `ui/KsSettingsView.kt:89` が `supportsChangeAnimations = false` を設定しているため、payload の有無に関わらず `SimpleItemAnimator.canReuseUpdatedViewHolder` は true を返す。payload 無しの `notifyItemChanged` へ退行しても ViewHolder は再利用される

つまりこの観測点は、内容更新経路が現実的にどう壊れても同じ結果を返す。review-001 Minor 2 が塞ごうとした穴は空いたままで、さらに「identity を守っている」と読める assert とコメント (`StoreCellStyleReplaceTest.kt:91-92` の「行の identity が保たれたことは、置換の前後で同じ ViewHolder が使われていることで見る」) が残るぶん、次に読む人を誤らせる。review-001 が採用条件として付けた「推奨形が Robolectric で成立するかは採用前に実測で確かめること」が実行されていない。

**推奨修正**: 観測点を ViewHolder のインスタンスから **adapter の通知列**へ移す。更新の直前に `view.internalMainListAdapter()` へ `RecyclerView.AdapterDataObserver` を登録し、更新後に「`onItemRangeChanged(position, count, payload)` が `KsSettingsView.PAYLOAD_CONTENT` 付きで発行され、`onItemRangeRemoved` / `onItemRangeInserted` / `onChanged` は 1 件も発行されない」ことを assert する。

この形の検出力は probe で確認済み (一時テストを置いて実測し、確認後に削除):

| 実装 | 観測された通知列 |
|---|---|
| 現行 | `[changed(0,1,payload=ks-content)]` |
| remove + insert へ退行させた版 | `[removed(0,1), inserted(0,1), changed(0,1,payload=ks-content)]` |

あわせてこの観測点は、spec の THEN 前半「差分は内容更新として発行され」(verify-001 が「観測していない」と注記した部分) を初めて実測で埋める。DSL 経路も同じ adapter を通るので、両テストに同じ形を置ける。

オーナーが本サイクルでこれ以上コストをかけない判断をする場合は、**誤った保証を残すより 2 つの `assertSame` とその説明コメントを削除して穴を明示的に開けたままにするほうが良い** (未観測であることは verify-001 の注記が既に記録している)。

### [🟡 Minor] Store 側の待機述語が「行が無い瞬間」に例外で中断する形になっている

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/StoreCellStyleReplaceTest.kt:78-81` (述語と `extraDiagnostics` の両方)

**問題点**: 述語 `{ titleColorOfRow(view) == after.toArgb() }` と診断 `{ "現在の title 色: ${titleColorOfRow(view)}" }` はどちらも `labelRowHolder(view)` の `requireNotNull` (`:131`) を通る。行が未生成・一時的に外れている瞬間に呼ばれると `IllegalArgumentException` が投げられ、`awaitConvergence` のループは待たずに中断する。診断側で投げた場合は handbook が求める「その時点の実測値を載せた fail」ではなく、無関係な例外でテストが落ちる。

現行の内容更新経路では payload 付き変更通知だけが飛ぶので child が外れる瞬間は無く、実害は今のところ無い。ただし同じ change 内の姉妹テスト `DSLCellStyleUpdateTest.awaitTitleColor` (`:95`) は同じ状況を `labelRowHolder()?.views?.titleView?.currentTextColor` と null 安全に書いており、流儀が割れている。Major 1 の推奨修正で観測点を足すときに同じ形を踏まないよう、あわせて直すのが安い。

**推奨修正**: `labelRowHolder(view)` を nullable を返す形 (`…singleOrNull()`) に変え、述語と診断は null を「まだ収束していない」として扱う。最終アサーション側だけが `requireNotNull` で落とす。

### [🟡 Minor] tasks 8.1 の Android 件数が修正サイクル後の実測に追随していない

**該当箇所**: `tasks.md` の 8.1 (Android 分)

**問題点**: 8.1 は「本体 1252 件 × 2 + bridge 173 件 × 2 = 計 2850 件」と記録しているが、修正サイクル 1 で `CellSpecificColorTest` に 2 件が加わり、現在の実測は 2854 件である (本レビューで `--rerun-tasks` により再現)。handbook `cross/test-execution.md` は「実行件数を確認するところまでが検証」と定め、報告に件数の併記を求めている。change の記録が最終状態と食い違うと、蒸留や後日の棚卸しで「どの状態で緑だったか」を追えない。

**推奨修正**: 8.1 の Android 行に修正サイクル後の再実行結果 (本体 1254 件 × 2 + bridge 173 件 × 2 = 2854 件 / 失敗 0) を追記する。MAUI は本レビューで 520 件 / 失敗 0 を再確認済み。iOS は本サイクルで `ios/` を触っていないため再実行不要。

## 確認した観点 (指摘に至らなかったもの)

- **指摘 1 (`AsyncListDiffer` の待機) は閉じた**: `DSLCellStyleUpdateTest.awaitTitleColor` (`:90-107`) は handbook の 3 条件をすべて満たす — 述語は「対象行が存在し期待色になった」で行の生成前も安全に false になり、上限は `System.nanoTime()` の実時間 deadline、超過時は現在の色と行一覧を載せて `fail()`、ループ内で `shadowOf(Looper.getMainLooper()).idle()` と `composeRule.waitForIdle()` を回したうえで `Thread.sleep(1)` で実行機会を譲っている (`Thread.yield()` ではない)。`StoreCellStyleReplaceTest` は既存の `KsSettingsViewTestSupport.awaitConvergence` (`:41-63`) を使っており、こちらも 3 条件を満たす (述語の書き方だけが Minor 1)
- **指摘 2 (Checkbox / Entry の消費点) は閉じた、かつ検出力を実測した**: `CellSpecificColorTest.kt:295-330` の 2 本は解決結果ではなく消費点 (`MaterialCheckBox.buttonTintList` / `EditText.highlightColor`) を実描画で読み、Cell 固有 → Theme の 2 ケースと「透明な黒でないこと」を固定している。`CheckboxCellViewHolder.kt:40` と `EntryCellViewHolder.kt:237` を素の `toArgb()` へ差し替えた probe では `CellSpecificColorTest` 10 件のうち **この 2 本だけ**が落ちた (前提アサーションは通過)。probe の 2 ファイルは backup からの復元後に shasum 一致と `git diff --stat` の一致で原状復帰を確認済み。caret tint を `highlightColor` で代替した点は deviation.md 記載の合意済み手段差
- **指摘 4 (handbook の完了条件) は閉じた**: `kasane/handbook/maui/integration-host-verification.md:125` に完了条件が 1 行追加され、本文 `:116` の期待値 (緑 `#FF008000` / マゼンタ `#FFFF00FF`) と対応している。完了条件だけを満たして外観追随を未確認で通す経路は塞がった
- **ViewHolder 2 ファイルの再構成 (指揮側からの追加確認事項)**: `git diff HEAD` は `CheckboxCellViewHolder.kt` が「`toArgb` import 1 行の削除 + `cell.accentColor?.toArgb() ?: effective.accentColor` → `cell.accentColor.toArgbOrElse(effective.accentColor)` 1 行」(+1/-2)、`EntryCellViewHolder.kt` が「同じ変換 1 行」(+1/-1) のみ。兄弟 4 ファイル (`SwitchCellViewHolder` / `RadioCellViewHolder` / `SimpleCheckCellViewHolder` / `DatePickerCellViewHolder`) と同一の変換パターンで、それ以外の差分 (欠落・余分) は無い。`EntryCellViewHolder` で import を残しているのは `:198` の placeholder 経路 (`takeIf { it.isSpecified }?.toArgb()`) が使うためで正しく、`DatePickerCellViewHolder` も同様に残している。deviation.md の「4 ViewHolder の未使用 `toArgb` import 削除」は Switch / Radio / SimpleCheck / Checkbox の 4 本と一致する。両ファイルに ADR 参照除去の作業は無く、deviation.md の付随修正リスト (Cell ファイル側) とも矛盾しない
- **消費点の網羅**: `toArgbOrElse` の呼び出しは 8 箇所 (`SwitchCellViewHolder:154` / `CheckboxCellViewHolder:40` / `SimpleCheckCellViewHolder:32` / `RadioCellViewHolder:32` / `EntryCellViewHolder:237` / `DatePickerCellViewHolder:131,224` / `PickerSelectionSheet:134`) で tasks 1.4 の列挙と一致する。`android/` に `?.toArgb() ?:` 型の読み出しと `Color?` の宣言はいずれも 0 件のまま
- **修正サイクルの影響範囲**: 作業ツリーの更新時刻から、review-001 以降に触れたのは新規テスト 3 本・handbook 1 本・ViewHolder 2 本・deviation.md / tasks.md のみ。`ios/` `maui/` のソースと足場 (specs / proposal) は本サイクルで動いていない
- **review-001 Minor 3 / Suggestion の申し送り**: `tasks.md:79-80` に ADR-0030 Decision 3 の MAUI 節の限定と、Android の内容更新経路を `replaceCells` として書く旨が追記済み。相方指摘の exploration スコープ注記 (`exploration.md:80`) と別 change の evidence 参照のリポジトリ相対化 (`kasane/changes/maui-appthemebinding-coverage/exploration.md:18`) も反映済み
- **新規テストのコメント規約**: 3 本とも change 名・レビュー通番・タスク番号・デルタスペック構文キーワードの持ち込みが無く、観測点を選んだ理由がファイル単独で読める形で書かれている

## アクションプラン

1. **Major 1** — `DSLCellStyleUpdateTest` / `StoreCellStyleReplaceTest` の観測点を adapter の通知列へ移す (推奨形の検出力は本レビューで実測済み)。コストを抑えるなら、代わりに 2 つの `assertSame` と該当コメントを削除して穴を明示のまま残す
2. **Minor 1** — Store 側の待機述語・診断を null 安全にする (Major 1 の修正と同じファイルなので同時に)
3. **Minor 2** — tasks 8.1 の Android 件数を 2854 件へ追随させる
4. 再修正後は `cd android && ./gradlew test --rerun-tasks` を回して件数を確認する (iOS / MAUI は本サイクルで触っていないため再実行不要)
