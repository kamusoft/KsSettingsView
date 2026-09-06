# レビュー結果: fix-default-colors-dark-appearance (004 回目)

**日付**: 2026-09-06
**判定**: APPROVED

## サマリー

範囲限定 (3 周目の Minor / Suggestion と相方 Suggestion への直接修正) の再確認。3 件とも指摘どおりに解消しており、識別子の改名は挙動を変えていない (呼び出し順・リセット対象・早期 return の条件は 3 周目に確認した形のまま、名前とコメントだけが変わっている)。旧識別子の残存参照も本体・テスト・サンプルに無い。

ただし 3 周目の Minor が指摘した誤り (placeholder の解決元を「ホストテーマ」と説明する) は、指摘に挙がっていた `EntryCellViewHolder.kt` の中でしか直っていない。同じ主張が本 change で書き足した / 書き直した 2 箇所と公開 API の doc コメント 1 箇所に残っており、いま同一リポジトリ内で `EntryCellViewHolder.kt` (同梱テーマ) と `EffectiveStyle.kt` (ホストテーマ) が同じフォールバックを別々に説明する状態になっている。挙動には影響しないため優先度は低い Minor とし、判定は APPROVED を維持する。

### 実行した検証

- Android: `ANDROID_HOME=<SDK> ./gradlew test --rerun-tasks` (`android/`) — 結果 XML の集計で `kssettingsview` は debug 1235 / release 1235 の **2470 tests / 0 failures / 0 errors**、`kssettingsview-bridge` は release 170 / 0。**`:kssettingsview-bridge:testDebugUnitTest` だけがテストの失敗ではなく Gradle 側の実行基盤エラーで落ちた** (3 回試行して `java.io.EOFException` / `NoSuchFileException: .../test-results/testDebugUnitTest/binary/in-progress-results-generic.bin` / 「テストを 1 件も発見できない」の 3 通り。いずれも結果バイナリの欠落で、アサーション失敗は 1 件も記録されていない)。同タスクを 2 回に分けて `--tests` で全 15 クラスを回すと **96 + 74 = 170 tests / 0 failures** で通る。したがって Android 全体は **2810 tests / 0 failures** (指揮側が 3 周目に報告した件数と一致) で、落ちているのはテストではなく実行基盤と判断した。詳細と所見は下の [🔵 Suggestion] に書く
- 範囲該当クラスの単独実行: `:kssettingsview:testDebugUnitTest --tests` で `ThemeAppearanceResolutionTest` 26 / `InputCellsTest` 74 / `HostThemeIndependenceTest` 10 / `EffectiveStyleResolutionTest` 47 = **157 tests / 0 failures**
- lint: `scripts/comment-policy-lint.py` 禁止 0 件 (検査対象 778 ファイル)、`--selftest` 全件 OK、`scripts/local-path-lint.py` 0 件
- 旧識別子の走査: `hostHintTextColors` / `currentHostHintTextColors` / `hostHintTextColorsSource` / `hostDefault` をリポジトリ全体の `*.kt` `*.swift` `*.cs` `*.xml` `*.md` に対して検索。本体コードの残存参照は 0 件 (ヒットするのは本 change と過去 change のレビュー文書、および本 change が触っていない既存テストのローカル変数)
- 足場凍結: `specs/` / `proposal.md` / `design.md` / `exploration.md` / `ui/mock/` に diff なし。`tasks.md` の未チェックは 9.1 のみで 3 周目から増減なし
- 解決元の裏取り: Material Components 1.12.0 の AAR から `values.xml` を取り出し、`Base.V14.Theme.Material3.Light` が `android:textColorHint` に `@color/m3_hint_foreground`、`Base.V14.Theme.Material3.Dark` が `@color/m3_dark_hint_foreground` を設定していることを実測で確認した。同梱テーマ `Theme.KsSettingsView.Internal` はその DayNight 派生であり、`ContextThemeWrapper` は `applyStyle(resId, force = true)` で被せるため、ホスト側の同名属性値は必ず覆われる。あわせて `KsThemedContext.kt` の `themeBaseContext()` が中間のホスト製ラッパを素通りして Activity / Application まで降りることも確認した。つまり「解決元は同梱テーマであってホストテーマではない」は成立する

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| cross/comment-policy.md | 常時 (always) |
| cross/test-execution.md | テスト実行・テスト結果の報告 |
| cross/local-development-setup.md | Android SDK の解決先を確かめてテストを実行する |
| lessons/code-review.md (L-001) | 昇格済みルール |

4 周目は範囲限定 (コメントの記述・識別子の改名・変更文書内の参照訂正) のため、UI 実装・iOS・sample-parity・実行時挙動の各規約は担当範囲に当たらず本文まで読んでいない (3 周目で照合済み)。

## 3 周目指摘の解消状況

| 出所 | 指摘 | 判定 | 根拠 |
|---|---|---|---|
| review-003 [Minor] | placeholder の解決元を「ホストテーマ」と説明するコメント | **部分解消** | `EntryCellViewHolder.kt` 内は全面的に解消。同ファイルに残る「ホスト」は `:74`「ホストアプリの XML テーマは参照しない」と `:359`「Activity を再生成せずに外観が変わるホスト」の 2 箇所だけで、どちらもホストアプリを指す正しい用法。ホスト前提を述べていた段落 (旧 `:76`) は「同梱テーマが `android:textColorHint` を持つことを前提にする」へ置き換わっている。ただし他ファイルの同じ主張が残る (下の [🟡 Minor]) |
| review-003 [Suggestion] | getter 名の関数が適用済み記録までリセットする | **解消** | `currentHostHintTextColors()` → `syncThemeHintTextColors()` へ改名。KDoc にも「名前どおり「同期」であり、単なる getter ではない」と明記。フィールドも `themeHintTextColors` / `themeHintTextColorsSource`、`applyPlaceholderColor` のローカルも `themeDefault` へ揃った |
| second-opinion code-003 [Suggestion] | `ui/brief.md` の「再実証 1.」→「2.」 | **解消** | `ui/brief.md:196` は「上の「再実証」2.」と「「再実証」2. の 1〜5 と同じ」の 2 箇所とも 2. を指す。節見出しは 1. が MAUI Android、2. が Android Native で、live 切替の証跡 (`android-native-input-cells-live-*.png`) は 2. の本文で言及されているファイルであり、参照先は正しい |

### 改名が挙動を変えていないことの確認

`syncThemeHintTextColors()` は 3 周目に確認した `currentHostHintTextColors()` と同じ 6 行 — 現在の `ksThemedContext()` を引き、保持している解決元 Context と同一なら保持値を返し、違えば解決元を更新して `hintTextColorsFromTheme()` で引き直し、`appliedPlaceholderColor` / `placeholderColorApplied` を落として返す。呼び出し箇所も `applyPlaceholderColor` の冒頭 (差分判定の前) と `unbind` の 2 箇所のままで増減が無い。`hintTextColorsFromTheme()` の実装 (`obtainStyledAttributes` → `getColorStateList` → `recycle`) にも変更が無い。範囲該当クラスの 157 件が通ることと合わせ、改名以外の作用は無いと判断した。

## 指摘事項

### [🟡 Minor] 「解決元はホストテーマ」の説明が他 3 箇所に残り、リポジトリ内で説明が割れている

**該当箇所**:

- `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt:86` (公開 `Theme.cellPlaceholderColor` の `@property` doc)
- `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyle.kt:314-316`
- `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsViewDefaults.kt:88`
- (影響は小さい) `ui/brief.md:198`、`ui/brief.md:206`

**問題点**:

3 周目の Minor は「解決元は同梱テーマであってホストテーマではない」という誤りへの指摘で、`EntryCellViewHolder.kt` の該当行を列挙していた。修正はその列挙どおりに同ファイル内だけへ入り、同じ主張をしている他ファイルが残った。

- `Theme.kt:86` — 「`Color.Unspecified` は未指定 → プラットフォーム既定（ホストテーマの hint 色）にフォールバックし」。**公開 API の doc コメント**であり、利用者はこれを読むと「アプリ側テーマの `android:textColorHint` が使われる」と理解する。実際には同梱テーマが `force = true` で被さるため、ホストが `android:textColorHint` を明示していてもライブラリの placeholder には出ない (android/ADR-0020 の視覚隔離)
- `EffectiveStyle.kt:314-316` — 「描画側はホストテーマの hint 色（`android:textColorHint` の `ColorStateList`）をそのまま使う」。この段落は本 change で書き直された行であり、`Unspecified` 化に合わせて文面を更新する際に誤った出所の記述が引き継がれている。同じファイルの `:454-455` は「4 段目はホストのテーマを参照せず、ライブラリが所有する外観別の既定値である」と隔離を明記しており、同一ファイル内でも語り口が揃っていない
- `KsSettingsViewDefaults.kt:88` — 「`cellPlaceholderColor` — ホストテーマの hint 色（platform 既定）」。本 change で新規追加したファイルの記述。なお同ファイルの公開 doc (`:29`) は「入力欄の placeholder 文字色 — プラットフォームの既定」と書いており、こちらは問題ない
- `ui/brief.md:198`「「現在の外観のホストテーマが解決する値」が契約」、`:206` の期待欄「ホストテーマ light の hint 色」— アーカイブされる作業文書のため実害は小さいが、蒸留で concepts へ書き起こす際の元資料になる

comment-policy の「現在の仕様を現在形で書く」に照らすと、現在の仕様と食い違う説明が残っている状態である。実害は 3 周目の指摘と同じ — この説明を信じた後続の変更が解決元を `ksHostContext()` 側へ寄せると視覚隔離が壊れる — に加え、公開 doc の分は利用者に対して事実と異なる契約を示している。挙動には影響しないため優先度は低い。

**推奨修正**: 3 箇所の「ホストテーマの hint 色」を「同梱テーマ（`Theme.Material3.DayNight` 派生）が解決する hint 色」に相当する表現へ揃える。`Theme.kt:86` は公開 doc なので内部用語 (ADR ID 等) を持ち込まず、「ライブラリが用意するテーマの既定 hint 色にフォールバックし、ライブラリ独自の既定色は持ち込まない」程度の言い方で足りる。`ui/brief.md` の 2 行は蒸留前に直しておくと concepts への転記時に誤りが伝播しない。

### [🔵 Suggestion] `kssettingsview-bridge` のテストタスクが単体では完走せず、テスト実行の信頼性を下げている

**該当箇所**: `android/kssettingsview-bridge/build.gradle.kts` (テストタスクの JVM 設定が無い) / 比較対象は `android/kssettingsview/build.gradle.kts:76` (`maxHeapSize = "2g"`)

**問題点**: 本レビューで `./gradlew test --rerun-tasks` を回したところ、`:kssettingsview-bridge:testDebugUnitTest` が 3 回連続で失敗した。3 回とも失敗の形が違い (`java.io.EOFException` / 結果バイナリ `in-progress-results-generic.bin` の `NoSuchFileException` / 「テストを 1 件も発見できない」)、いずれも**テストのアサーション失敗は 1 件も記録されていない**。同じ 15 クラス 170 件を `--tests` で 2 回に分けると 96 + 74 = 170 件すべて成功し、同一ソースの release variant (170 件) も 1 タスクで完走する。

Gradle のログでは、このモジュールのテストワーカーが既定の `-Xmx512m` で起動している (`kssettingsview` 側は `maxHeapSize = "2g"` を明示しているが、bridge 側には指定が無い)。Robolectric を 15 クラス分ロードすると限界付近になり、ワーカーが結果バイナリを書き切れずに落ちている、という説明と観測が整合する。分割すると通ること、release では通ること、同じコマンドが指揮側の環境では今日成功していること (3 周目報告の 2810 / 0) から、**本 change の変更 (4 周目はコメントと同一ファイル内の改名のみ、bridge モジュールには一切触れていない) に起因する失敗ではない**と判断し、判定には反映していない。ただし本 change は bridge のテストを 2 ファイルに追加しており (`KsBridgeCellConversionTest` / `KsBridgeThemeTest`)、限界付近の押し上げに寄与している可能性はある。

**推奨修正**: 本 change では扱わず、別の変更として `kssettingsview-bridge` のテストタスクにも明示のヒープ設定を入れることを検討する (`kssettingsview` と同じ形)。恒常的に再現するなら「テストが 0 件でも BUILD SUCCESSFUL になり得る」のと同種の、静かに検証が空振りする経路になる。

## 確認して問題が無かった観点

- **コメントと実装の整合 (`EntryCellViewHolder.kt`)**: `themeHintTextColors` の KDoc が言う「初期値は入力欄を生成した Context の解決結果」は、`create()` が `EditText(views.root.context)` で入力欄を作るため `editText.context === views.root.context` となり成立する。`themeHintTextColorsSource` の初期値もこの Context で、`views.root.context.ksThemedContext()` は同じ夜間モードなら自身を返すので、初回 bind は早期 return で引き直しが起きない (`InputCellsTest` の `assertSame(…, vh.editText.hintTextColors)` 系 3 本が通ることが実測の裏付け)
- **comment-policy の禁止類型**: 追加・書き換えされたコメントに作業文書のパス・変更識別子・通番・履歴記述・デルタスペック構文キーワードは無い。参照は `[KsThemedContext]` `[ksThemedContext]` `[syncThemeHintTextColors]` などのコード識別子のみで許容範囲。`//` 行コメント内での `[識別子]` 表記は本リポジトリの既存スタイルと一致する (`CellBaseLayout.kt:165` 等)
- **公開 doc への内部用語の混入**: 本周で触れた `EntryCellViewHolder` は `internal`。`KsSettingsViewDefaults` の公開 doc に内部用語は無い
- **改名の巻き添え**: `syncThemeHintTextColors` / `themeHintTextColors` / `themeHintTextColorsSource` / `themeDefault` はいずれも `private`・ローカルで、モジュール外や bridge / MAUI 側へ露出していない。公開 API 面の diff は無い
- **`unbind` の二重リセット**: `syncThemeHintTextColors()` が内部で `appliedPlaceholderColor` / `placeholderColorApplied` を落とし、直後に `unbind` が同じ 2 つを再度落とす。冗長だが害は無く、`unbind` 側だけを読んでも初期化の意図が読める形になっている
- **足場と tasks.md**: 凍結対象に diff なし、虚偽チェックなし (未チェックは 9.1 の申し送りのみ)
- **`ui/brief.md` の参照訂正**: 節番号・節見出し・言及ファイルの三者が一致することを本文で確認した。他の「再実証」参照 (`:38` `:43` `:45` `:47` `:75`) にも番号のずれは無い

## アクションプラン

1. (任意・蒸留前が望ましい) `Theme.kt:86` / `EffectiveStyle.kt:314-316` / `KsSettingsViewDefaults.kt:88` の「ホストテーマの hint 色」を同梱テーマ由来の表現へ揃える。公開 doc (`Theme.kt`) は内部用語を使わない言い方にする
2. (任意) `ui/brief.md:198` / `:206` の同じ語も直し、蒸留で concepts へ誤った解決元が転記されないようにする
3. (別変更) `kssettingsview-bridge` のテストタスクのヒープ設定を検討する
