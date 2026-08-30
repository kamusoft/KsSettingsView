# レビュー結果: unify-inline-looper-idle-calls (002 回目)

**日付**: 2026-08-09
**判定**: APPROVED

## サマリー

review-001 の Major 指摘 (`ClassicSectionDecorationTest.kt` に残存していた完全修飾形のインライン idle) は解消している。置換は共有ヘルパへ正しく解決され、随伴するコメント改訂もスコープ外の余計な変更ではなく、同一編集の整合を取る必要な後始末と判断した。

網羅性は自分で式の形に依存しない 3 系統の検索を独立に実施して確認した。`.idle()` (任意レシーバ・任意修飾)、`Looper` の全出現、および Robolectric の looper 操作 API 群 (`idleMainLooper` / `shadowMainLooper` / `runToEndOfTasks` / `runOneTask` / `runUiThreadTasks` / `flushForegroundThreadScheduler` / `flushBackgroundThreadScheduler` / `ShadowLooper`) の 3 通りがいずれも同じ結論に収束しており、**形の違いで漏れる経路は残っていない**。

テストは全 4 モジュール・両 variant で 2024 件成功 (0 failures / 0 errors / 0 skipped)、コメント規約 lint も 0 件。指摘事項なし。

## 前回指摘の解消確認

### [🟠 Major → 解消] 同一パッケージに完全修飾形のインライン idle が 1 箇所残存している

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ClassicSectionDecorationTest.kt:187-190`

**確認内容**:

```diff
-        // submitList は AsyncListDiffer の挙動で非同期になる場合があるため、テストでは内部 list を
-        // 同期反映する getCurrentList ベースの API として submitList を呼び idleMainLooper で flush する。
+        // submitList は AsyncListDiffer の挙動で非同期になる場合があるため、submitList を呼んだ後に
+        // メインループを流し切って内部リストへ反映させる。
         mainAdapter.submitList(items)
-        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
+        idle()
```

**置換の正しさ**: 共有ヘルパ `idle()` の本体は `shadowOf(Looper.getMainLooper()).idle()` そのものであるため、この置換は**構成上定義的に等価**で、周囲の非同期セマンティクスに依存せず挙動が変わらない。名前解決も確認済み — 当該ファイルにローカルな `idle` 定義はなく、パッケージ内の `fun idle` 定義は `KsSettingsViewTestSupport.kt:20` の 1 件のみなので、`internal` トップレベル関数へ一意に解決される。完全修飾形だったため import の追加・削除が発生していない点も指摘どおり。

**コメント改訂の妥当性**: **スコープ外の余計な変更ではなく、整合として妥当**と判断する。理由は 3 点ある。

1. 旧コメントは `idleMainLooper` という、**このコードが呼んでいない API 名**を機構の説明として書いていた (実際に呼んでいたのは `shadowOf(...).idle()`)。`idleMainLooper` は Robolectric に実在する別 API であり、読み手を誤った API へ誘導する
2. 旧コメントは「内部 list を**同期反映する** getCurrentList ベースの API として submitList を呼び」と書いており、同じ文の冒頭の「**非同期になる場合がある**ため」と自己矛盾していた
3. 置換によって、旧コメントが名指しする機構がコード上から消える。放置すれば「除去済みの機構を説明するコメントが新しい呼び出しの直上に残る」状態を**この変更自身が作り出す**ことになる

comment-policy 規約は、コード識別子への参照を許容する根拠を「grep で到達でき、消えれば同一コミット内で壊れに気づける」ことに置いており、また書き換え類型 3 (履歴記述型) で「現在の仕様の説明に書き換える」ことを求めている。今回の改訂はまさにこの類型に当たり、置換と不可分な後始末である。改訂後の文は自己完結し、禁止参照・禁止類型のいずれにも触れていない。

## 独立に実施した網羅性の全数確認

オーケストレーター側の確認とは別に、`android/ks-settingsview-ui/src/test/` 全体に対して自分で 3 系統の検索を行った。前回の見落としが「検索式の形への依存」に起因していたため、**異なる原理で漏れを拾う検索を重ねて交差検証する**方針を取った。

### [1] 任意レシーバ・任意修飾の `.idle()`

```
grep -rn --include='*.kt' "\.idle()" .
```

→ ヒットは `KsSettingsViewTestSupport.kt:21` (共有ヘルパ本体) の **1 件のみ**。

この式はレシーバ式の形をまったく仮定しないため、完全修飾・エイリアス import・変数経由 (`val s = shadowOf(...); s.idle()`)・`Shadows.shadowOf(...)` のクラス修飾など、`.idle()` で終わるあらゆる呼び方を拾う。前回の見落とし (完全修飾形) はこの検索で確実に捕捉される形になっている。

### [2] `Looper` という語の全出現

```
grep -rn --include='*.kt' "Looper" .
```

→ 全 22 件の内訳は、`idleFor` 呼び出し 14 件 / `import android.os.Looper` 5 件 / 共有ヘルパの実装 1 行 / 共有ヘルパの KDoc 2 行。**待機目的の未集約コードは 1 件もない**。

`.idle()` 以外のメソッド名で main looper を回す経路 (メソッド名が想定外でも `Looper` は必ず現れる) を拾うための検索。

### [3] Robolectric の looper 操作 API 群

```
grep -rnE --include='*.kt' "idleMainLooper|shadowMainLooper|runToEndOfTasks|runOneTask|runUiThreadTasks|flushForegroundThreadScheduler|flushBackgroundThreadScheduler|ShadowLooper|idleFor" .
```

→ ヒットは `idleFor` の **14 件のみ**。`ShadowLooper` の静的 API (`idleMainLooper()` / `shadowMainLooper()`) や legacy scheduler API を使った「`Looper` の語も `.idle()` も現れない」抜け道は存在しない。

検索 [1] は「呼び出し形式」、[2] は「対象オブジェクト」、[3] は「API 名の語彙」という互いに独立した軸で網を張っており、3 つが同一の結論に収束したことをもって網羅性の確認とした。

### `idleFor(Duration)` の無変更確認

`PickerSelectionSheetTest` 2 / `KsWheelViewTest` 6 / `DateSelectionSheetTest` 4 / `NumberSelectionSheetTest` 2 = **14 箇所**。HEAD 版の実測値 14 と一致し、対象外扱いが守られている。`DateSelectionSheetTest` / `NumberSelectionSheetTest` が `import android.os.Looper` を保持しているのも `idleFor` 用途として正しい (この 2 ファイルはそもそも本変更の対象外)。

## その他の確認 (指摘なし)

**変更範囲の限定**
- 前回レビュー以降に新規変更されたのは `ClassicSectionDecorationTest.kt` の 1 ファイルのみ (差分 6 行 = コード 1 行 + コメント 2 行の置換)
- 対象 7 ファイルの差分行数は review-001 時点と完全に一致しており、**再修正のついでに入った意図しない編集はない**
- production コード・bridge・Gradle への変更は引き続きなし。テストコードのみ

**テスト実行**
- `cd android && ./gradlew test --rerun-tasks` → **exit code 0**
- test-results XML 148 件がすべて再生成されていることを生成時刻で確認 (古い結果の混入なし)。`<failure` / `<error` タグを含む XML は 0 件
- モジュール別内訳: `ks-settingsview-ui` 1604 / `ks-settingsview-compose` 196 / `ks-settingsview-core` 148 / `ks-settingsview-bridge` 76 = **合計 2024 tests / 0 failures / 0 errors / 0 skipped**
- オーケストレーター側は `:ks-settingsview-ui:test` のみの実行だったため、**全モジュール実行で compose / bridge / core への波及がないこと**を独立に確認した (テスト実行規約の「完了判定には絞り込みなしの全件実行を使う」に従う)

**コメント規約**
- `python3 scripts/comment-policy-lint.py` → 合計 0 ファイル / 禁止 0 件 (検査対象 478 ファイル)

**review-001 で確認済みの観点 (再確認して変化なし)**
- インライン idle 26 箇所の置換の等価性・過剰置換の不在
- `awaitDifferCommit` 集約の等価性 (ミューテーションプローブによる遅延評価の実測証明を含む)
- import 除去 5 ファイル / 保持 2 ファイルの判断
- `committedSummary` のローカル維持と CellRow 要約差分の保存

## 積み残し (本変更では対応不要)

以下は review-001 の Suggestion で、オーケストレーターの方針 (前者は後続タスクとして起票、後者は対応せず) に異論はない。

- **`layoutAndDraw` の重複と exploration の除外理由の誤り**: `DatePickerTodayShortcutTest.kt:503-514` と `DatePickerDialogIntegrationTest.kt:99-110` に完全一致で重複。exploration.md の除外理由「単一ファイル内でしか使われず重複していないため」が事実と合っていない点は、後続タスクの起票時に前提として引き継ぐことを推奨する
- **`awaitDifferCommit` の名前付き引数化**: 22 箇所の機械的置換になり、動作・可読性への実害はないため見送りで妥当

## アクションプラン

なし。本変更は蒸留 (ksn-distill) へ進んでよい状態にある。
