# レビュー結果: unify-inline-looper-idle-calls (001 回目)

**日付**: 2026-08-09
**判定**: CHANGES_REQUESTED

## サマリー

インライン `shadowOf(Looper.getMainLooper()).idle()` の共有 `idle()` への置換は、探索が数えた 26 箇所すべてで正しく等価に行われ、過剰置換も import の取りこぼしもない。`awaitDifferCommit` の共有ヘルパ集約も待機ロジック・失敗メッセージともに集約前と同一で、遅延評価ラムダ化による評価タイミングの変化がないことはミューテーションプローブで実測確認した。テストは全 2024 件成功、コメント規約 lint も 0 件。

ただし**同一パッケージ・同一ディレクトリの `ClassicSectionDecorationTest.kt:190` に、完全修飾形で書かれたインライン idle が 1 箇所残っている**。探索の 26 という実測値がリテラル文字列検索に依存していたため取りこぼされたもので、本変更の動機 (「共有ヘルパの存在を知らない書き手が素の Looper 待機を書く」再重複の芽を摘む) に対して穴が残る。これは探索自身が「前変更が名前違いの `awaitDifferCommit` を取りこぼした」と診断した検出失敗の型と同一であり、同じ理由で見落とされている。

## 指摘事項

### [🟠 Major] 同一パッケージに完全修飾形のインライン idle が 1 箇所残存している

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ClassicSectionDecorationTest.kt:190`

**問題点**:

当該行は完全修飾形で書かれているため、探索が使った検索式にヒットしていない。

```kotlin
org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
```

このファイルは `package jp.kamusoft.kssettingsview.ui` に属し、置換対象 7 ファイルと同一ディレクトリにある。共有ヘルパ `idle()` は同パッケージの `internal` トップレベル関数なので、そのまま呼べる状態にある。つまり技術的な除外理由はなく、**単に検索式に引っかからなかったために残った**。

`kasane/changes/unify-inline-looper-idle-calls/exploration.md` の「決定事項」は案B (インライン `idle()` 置換 + `awaitDifferCommit` 集約) の採用であり、除外が明示されているのは `idleFor(Duration)` の 14 箇所とファイルローカルの複合ヘルパのみ。本箇所はどちらにも該当せず、合意済みスコープ内の取りこぼしである。

残したままだと、本変更の唯一の成果 (パッケージ内でメインループ待機の書き方を 1 つに揃え、共有ヘルパの存在を発見可能にする) に穴が残り、次にこのファイルを触る書き手が素の Looper 待機を正しい書き方だと受け取る。

なお `ks-settingsview-compose` (2 箇所) と `ks-settingsview-bridge` (2 箇所) にも同式は残るが、これらは別モジュール・別パッケージで `internal` の共有ヘルパに到達できないため、対象外として正しい。

**推奨修正**:

当該 1 行を `idle()` に置換する。完全修飾形のため import の追加・削除は不要 (このファイルは `Looper` / `shadowOf` を import していない)。

あわせて、以後の同種作業では検索式を完全修飾形・エイリアス import を拾える形 (例: `getMainLooper()).idle()` のような後方部分一致) に広げることを推奨する。今回の見落としは、前変更の `awaitDifferCommit` 見落としと同じ「検索式の形に依存した網羅性判定」に起因している。

### [🔵 Suggestion] `layoutAndDraw` が 2 ファイルに完全一致で重複しており、除外の根拠が事実と合っていない

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerTodayShortcutTest.kt:503-514` および `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerDialogIntegrationTest.kt:99-110`

**問題点**:

exploration.md の「未決の論点」は、ファイルローカルの複合ヘルパを対象外とする理由を「単一ファイル内でしか使われず**重複していない**ため」としている。しかし `layoutAndDraw` は上記 2 ファイルに 12 行が完全一致で重複しており、この前提が成り立っていない (`PickerDialogRecreationTest` の `settle` 内にも同じ処理が展開されている)。

これは `awaitDifferCommit` と同型の重複 — 名前も本体も同一で、共有ヘルパ化の要件を満たしている。除外の判断自体は合意済みなので本変更での対応は求めないが、**判断の根拠が事実誤認である**点は次の変更のために記録しておく価値がある。

**推奨修正**:

本変更では対応不要。後続の変更で `layoutAndDraw` の共有ヘルパ化を検討するか、除外を維持するなら exploration の理由を実態に合った記述 (例: 「レイアウト回数などの調整余地が呼び出し側にあるため意図的に残す」) に改めることを推奨する。ただし足場アーティファクトは実装中に書き換えない規律のため、記述の是正は蒸留時か次変更の探索で扱う。

### [🔵 Suggestion] `awaitDifferCommit` の呼び出しが先頭ラムダ + 末尾ラムダの読みにくい形になっている

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/FullUpdateContentSyncTest.kt` ほか計 22 箇所

**問題点**:

```kotlin
awaitDifferCommit({ committedSummary(adapter) }) { recorder.changed.isNotEmpty() }
```

丸括弧内の無名ラムダと末尾ラムダが並ぶ形は Kotlin では読み手が引数の役割を推測しにくい。姉妹関数の `awaitConvergence` が診断用ラムダを `extraDiagnostics` という名前付き省略可能引数として第 3 位に置いているのに対し、こちらは無名の第 1 引数になっており、2 つの待機ヘルパで診断ラムダの渡し方が揃っていない。

**推奨修正**:

名前付き引数にすると役割が呼び出し側で自明になる (シグネチャ変更は不要)。

```kotlin
awaitDifferCommit(committedSummary = { committedSummary(adapter) }) { recorder.changed.isNotEmpty() }
```

22 箇所の機械的置換になるため、優先度は低い。現状のままでも動作・可読性に実害はない。

## 確認した観点 (指摘なし)

**置換の等価性と網羅性**
- HEAD 版の各ファイルのインライン idle 件数を実測: `PickerDialogRecreationTest` 11 / `ContentUpdatePayloadTest` 4 / `DatePickerDialogIntegrationTest` 4 / `DatePickerTodayShortcutTest` 3 / `FullUpdateContentSyncTest` 2 / `KsWheelViewTest` 1 / `PickerSelectionSheetTest` 1 = 計 26。作業ツリーでは 7 ファイルすべてが 0 件になっており、**26/26 が置換済み・過剰置換なし**
- `ks-settingsview-ui` の test パッケージに残る同式は、共有ヘルパ本体の 1 定義と上記 Major 指摘の 1 箇所のみ

**`awaitDifferCommit` 集約の等価性**
- 集約前の 2 定義 (`ContentUpdatePayloadTest` / `FullUpdateContentSyncTest`) を diff から復元して比較。待機ループ (deadline → while → idle → condition → `Thread.yield()`)、既定 `timeoutMillis = 5_000`、失敗メッセージ文字列がいずれも共有版と完全一致。差分は doc コメントの文言のみ (「後続の負のアサーション」→「後続のアサーション」) で、共有版は両者を包含する記述になっている
- **遅延評価ラムダの評価タイミングを実測検証** (lessons/code-review L-001 のミューテーションプローブ): `ContentUpdatePayloadTest` の待機条件を `{ false }` / `timeoutMillis = 300` に一時改変して失敗経路を発火させたところ、失敗メッセージは `差分コミットの待機条件が 300 ms 以内に成立しなかった (コミット済みリスト: [cell(text=あ)])` を出力した。**更新後**の状態 (`text=あ`) が載っている点が決定的で、`committedSummary` が呼び出し時点ではなく失敗時点で評価されていることの証明になる (先行評価なら呼び出し時点の `[cell(text=)]` が出る)。集約前も `fail(...)` の中で `committedSummary(adapter)` を呼んでいたため、**評価タイミングは完全に等価**であり、待機ループ内での余計な評価コストも発生しない
- プローブ後の原状復帰は shasum 一致で確認済み (`da97d61cb3c80e0cb5b0e8e3696e654cb4ed7f1b`)

**import 除去の正しさ**
- 除去した 5 ファイル (`PickerDialogRecreationTest` / `ContentUpdatePayloadTest` / `DatePickerDialogIntegrationTest` / `DatePickerTodayShortcutTest` / `FullUpdateContentSyncTest`) は `Looper` / `shadowOf` / `Assert.fail` / `TimeUnit` の参照が本文に **0 件**。除去は過不足なし
- 残した 2 ファイルの判断は正しい。`KsWheelViewTest` は `shadowOf(Looper.getMainLooper()).idleFor(...)` が 6 箇所実在。`PickerSelectionSheetTest` は `idleFor` 2 箇所に加え `shadowOf(Dialog)` (`:165`) と `shadowOf(View)` (`:667`) の別用途があり、`shadowOf` の import は二重に必要
- `Assert.fail` / `TimeUnit` は集約で削除された `awaitDifferCommit` 専用だったため、併せて除去したのは妥当

**スコープ遵守**
- `idleFor(Duration)` は HEAD・作業ツリーとも 14 箇所で無変更
- `committedSummary` は両ファイルに `private fun` としてローカル維持され、CellRow の要約差分 (`EntryCell.text` / `LabelCell.title`) も意図どおり保存されている
- `git status` 上、production コード・bridge・Gradle への変更は一切なし。テストコードのみ
- ファイルローカルの複合ヘルパ (`settle` 等) は内部のインライン idle 置換のみで、構造は無変更

**テスト実行**
- `cd android && ./gradlew test --rerun-tasks` → BUILD SUCCESSFUL (228 tasks executed)
- `build/test-results/` の XML 148 件を集計: **2024 tests / 0 failures / 0 errors / 0 skipped**
- `--rerun-tasks` により全 variant が実際に再実行されていることを確認 (UP-TO-DATE による 0 件実行ではない)

**コメント規約**
- `python3 scripts/comment-policy-lint.py` → 合計 0 ファイル / 禁止 0 件 (検査対象 478 ファイル)
- 共有ヘルパの `awaitDifferCommit` doc コメントは、`awaitConvergence` との使い分け (View 基点 / Adapter 基点) と `committedSummary` を呼び出し側から受ける理由を自己完結で説明できており、外部文書 ID への依存もない

**設計品質**
- 命名・抽象化レベルは既存の共有ヘルパ (`idle` / `awaitConvergence` / `committedTexts` / `visibleRowTexts`) と一貫している
- ファイル冒頭コメントが待機系 3 関数 (`idle` / `awaitConvergence` / `awaitDifferCommit`) を列挙する形へ更新されており、追加関数が地図から漏れていない
- 関連 ADR なし (exploration の判断どおり、テスト専用・可逆・境界を越えないため ksn-core の ADR 選別基準に該当しない)

## アクションプラン

1. **[Major]** `ClassicSectionDecorationTest.kt:190` の完全修飾インライン idle を `idle()` に置換する (import 変更不要)
2. 置換後に `grep -rn --include='*.kt' "getMainLooper()).idle()" android` で `ks-settingsview-ui` 側の残存が共有ヘルパの 1 定義のみになることを確認し、`./gradlew test --rerun-tasks` を再実行する
3. **[Suggestion]** `layoutAndDraw` の重複と `awaitDifferCommit` の呼び出し形式は本変更では対応不要。後続変更または蒸留時の検討材料として記録する
