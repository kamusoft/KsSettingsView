# レビュー結果: scroll-indicator-visible-not-applied (003 回目)

**日付**: 2026-09-17
**判定**: APPROVED

## サマリー

review-002 の Major (設定リストの Context 配線に回帰検出力が無い) と、second-opinion-code-002 の「突き合わせ結果」が採否の正とした 7 件は、すべて解消している。中核の Major については**ミューテーションで解消を実測**した — 配線を戻すと 4 件が落ち、さらに新設ガードを外しても新しいテスト 1 件は独立に落ちる (probe A / H)。新規に入った 3 件のテスト (Context 自体の観測・表示中の Store 更新・選択面の開き直し) にも実測で検出力があった (probe H / I / 既存 probe の再確認)。

ビルドとテストは 3 platform とも全件成功。新規の指摘は Minor 1 件 (コメント配置) と Suggestion 2 件のみで、いずれも実装の正しさには関わらない。

### 実行したビルドとテスト

| platform | コマンド | 結果 |
|---|---|---|
| iOS | `xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,id=<機種 UDID>'` (`ios/` で実行) | `** TEST SUCCEEDED **` / **1046 tests・0 failures** (バンドル集計行の合計: Bridge 166 + Core 88 + SwiftUI 98 + TestSupport 7 + UI 687) |
| Android | `./gradlew :kssettingsview:testDebugUnitTest :kssettingsview:testReleaseUnitTest :kssettingsview-bridge:testDebugUnitTest :kssettingsview-bridge:testReleaseUnitTest --rerun-tasks` (`android/` で実行) | `BUILD SUCCESSFUL` / **2902 tests・0 failures** (4 タスクの `TEST-*.xml` の `tests` / `failures` + `errors` 合計: 1278 + 1278 + 173 + 173) |
| MAUI | `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` | **564 合格・0 失敗** |
| lint | `scripts/comment-policy-lint.py --advisory` / `local-path-lint.py` / `identity-lint.py` / `doc-structure-lint.py` | 禁止 **0 件** (検査対象 802 ファイル) / 検出なし / 検出なし / 本 change 配下の指摘なし |

Android は `./gradlew test --rerun-tasks` の一括実行が 3 回続けて**実行環境側の失敗**で落ちた (Kotlin daemon の異常終了 / Robolectric の `ClassNotFoundException: <テストクラス自身>` / `java.io.EOFException` / 直前に Gradle が作った成果物の `No such file or directory`)。いずれも `kasane/handbook/cross/test-execution.md` が「テストの失敗ではなく実行環境の問題」と記述する型で、失敗箇所は実行ごとに別だった。モジュール / variant 単位に分けて直列 (`--no-parallel --max-workers=1`) で回すと 4 タスクとも全件 green になり、件数も一括実行時の集計 (2902) と一致した。**コード起因の失敗は 1 件も無い。**

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 公開 doc コメントの ADR 参照は `ios/Sources/KsSettingsViewUI/Theme.swift` から外れており、リポジトリ全体でも本 change 由来の `core/ADR-0032` 参照は source / test とも 0 件。機械検査の禁止 0 件。`--advisory` の要確認で本 change の触ったファイルに乗るものは、すべて HEAD 時点からの既存債務 (件数を増やしていない)。ただしコメントの**配置**に 1 件抵触 (下記 Minor)
- `kasane/handbook/cross/test-execution.md` (テスト実行・結果報告) — 件数を併記。新規・変更テストの待機は Android が `awaitConvergence` (実時間 deadline + `Thread.sleep(1)` + 超過時に実測値付き `fail`)、iOS が `awaitCondition` / `awaitInitialRender` で、固定秒数待機の持ち込みなし。review-002 で指摘のあった `HostWrappedContextUserContentTest` の `repeat(2) { idle() }` は、観測対象そのもの (3 つの利用者コンテンツの生成) を条件にした `awaitConvergence` へ置き換わっている。絞り込みを使うテスト (`HeaderFooterBackgroundDefaultTest` / `SheetScrollbarTest`) は「前提:」アサーションで対象が空でないことを確かめている
- `kasane/handbook/cross/runtime-behavior-verification.md` (実行時挙動が絡む修正の完了判定) — 争点だった「Activity を再生成しない夜間モード変更でのつまみの追従」が Robolectric 代替から**実環境の証跡**へ置き換わった。`ConfigChanges.UiMode` を宣言したホスト (MAUI の Android サンプル) を使い、`adb shell cmd uimode night yes` の前後で同一画面を撮り比べ、`dumpsys` の `topResumedActivity` が同じ `ActivityRecord` を指すことで同一 Activity であることも示している。Android の Modern / 未指定の証跡も追加済み
- `kasane/handbook/cross/sample-parity.md` — samples は 3 platform とも Header / Footer 背景を明示しており、既定を透明にしても画面は変わらない (tasks 4.4)
- `kasane/handbook/ios/swift6-language-mode-check.md` (`ios/Sources/**` を触る変更の完了判定) — 今サイクルの修正で再度 `ios/Sources/` を触ったことに対し、3 回目の実施記録 (error 0 件・`ios/Package.swift` の shasum 一致) が `evidence/README.md` にある
- `kasane/decisions/android/0020-*` (accepted) — ライブラリ所有 UI は同梱テーマ、利用者所有コンテンツはホストのテーマ。`ksHostContext()` / `ksUserContentContext()` / `ksScrollIndicatorContext()` の呼び出し箇所を全数確認し、取り違えは無い。`bindKsAnyView` が今も `container.context.ksHostContext()` を使う点も、container が `ksUserContentContext()` で作られているため結果は同じで、その等価性は `assertSame` で固定されている
- `kasane/decisions/core/0030-*` (accepted) — Decision 1 の既定値のうち Header / Footer 背景だけを置き換える変更であり、accepted な決定との衝突が残る。改訂 ADR (core/0032) は `proposed` のため、これを根拠に判定は下していない (所見に記載)
- `kasane/lessons/code-review.md` — 重点観点 L-001 (アサーションの検出力) を適用し、ミューテーション 4 本を実測した。使った一時変更はすべて backup からの書き戻しと shasum 一致で原状復帰済み。「指摘しないこと」は昇格済みルールなし
- `proposal.md` の Impact に記録された「`ui/` (brief・モック) を作らない」はオーナー合意済みの逸脱として扱い、違反として数えていない

## 前回指摘の解消確認

second-opinion-code-002 の「突き合わせ結果」の 7 件と review-002 の指摘は、すべて解消している。

| # | 指摘 | 状態 | 確認の根拠 |
|---|---|---|---|
| 1 | 設定リストの Context 配線に回帰検出力が無い | **解消** | ミューテーション実測 (下記) |
| 2 | tasks 5.2 が完了済みだが evidence に未実施の確認が残る | **解消** | 夜間モードは実環境で取り直し、Android の Modern 未指定も追加。残る iOS の目視は 5.2b として未了のまま分割され、`evidence/README.md` の「取れていない確認」と一致 |
| 3 | 公開 doc コメント / テストコメントの proposed ADR 参照 | **解消** | `core/ADR-0032` は source / test とも 0 件。lint の禁止 0 件 |
| 4 | Android の Store 後続更新の Scenario がテストされていない | **解消** | `ScrollIndicatorVisibleTest.kt:166` を追加。probe I で検出力あり |
| 5 | `HostWrappedContextUserContentTest` が固定回数の `idle` で待つ | **解消** | `awaitConvergence` + 観測対象そのものの条件へ置換 (`HostWrappedContextUserContentTest.kt:109`) |
| 6 | `KsThemePalette` の KDoc が変更後の契約と矛盾 | **解消** | `KsThemePalette.kt:9` を現在の契約の記述へ書き換え、透明が `Unspecified` と別物である点も明記 |
| 7 | 未参照の internal 定数 / 選択面を開き直す SHALL のテスト欠落 | **解消** | `lightFooterBackgroundColorValue` は削除。開き直しのケースは `SheetScrollbarTest.kt:145` と `ScrollIndicatorVisibleTests.swift:215` の両 platform に追加 |

### ミューテーション実測 (今サイクル)

`android/` で `./gradlew :kssettingsview:testDebugUnitTest --tests` を 5 クラスに絞って実行。いずれも実施後に backup から書き戻し、shasum 一致で原状復帰を確認済み。

| probe | 入れた変更 | 結果 |
|---|---|---|
| A | `KsSettingsView.kt:89` を `RecyclerView(context)` に戻す (配線を外す) | **4 件 FAILED** — `内部 RecyclerView の Context がスクロールバー用の style を運んでいる` / `内部 RecyclerView はスクロールバーの描画状態を持つ` / `外観の切り替えでスクロールバーの thumb が解決し直される` / `ホストが被せた ContextThemeWrapper でもスクロールバーの描画状態は同梱テーマから届く` |
| G | `applyScrollbarThumbForAppearance` の新ガード (`verticalScrollbarThumbDrawable == null` の早期 return) だけを外す | 0 件 FAILED |
| H | probe A + probe G (配線もガードも外す) | **1 件 FAILED** — `内部 RecyclerView の Context がスクロールバー用の style を運んでいる` |
| I | `applyThemeInternal` から `applyScrollIndicatorVisible(theme)` を外す | **6 件 FAILED** — うち `表示中の Store の Theme 更新で縦スクロールバーが無効になる` (新規) と `DSL の再評価で渡した false が縦スクロールバーへ届く` を含む。最初から `false` の Store を bind するだけの既存テストは落ちない |

A と H の差が、**review-002 の Major が二重に塞がれている**ことの証明になっている。H で残る 1 件 (`ScrollIndicatorVisibleTest.kt:127`) は、設定リストの Context からもう 1 つ `RecyclerView` を作って描画状態を見る形で、実行時の thumb 代入から完全に独立した観測点である。A で追加の 3 件が落ちるのは新ガードの効果で、これは「配線が壊れたときに実行時の代入がスクロールバーを黙って蘇らせる」という review-002 が副次的に挙げた冗長性そのものを取り除いている。ガードには独立した防御上の根拠 (`View` の thumb setter が ScrollabilityCache ごと作ってしまうため、`android:scrollbars` の届いていない View に代入しない) もコメントに書かれており、テストを通すためだけの分岐ではない。

I は、新設の Store 後続更新テストが、既存の「最初から `false` の Store を bind する」テストが通らない経路 (`applyThemeInternal`) を実際に踏んでいることを示している。

## 指摘事項

### [🟡 Minor] ライト外観の定数が「ダーク外観の既定色の生値」の節に置かれている

**該当箇所**: `ios/Sources/KsSettingsViewUI/Theme.swift:377` (節の見出しは `:351`)

**問題点**:
本 change で新設した `lightHeaderBackgroundColorValue` が、`// MARK: - ダーク外観の既定色の生値（core/ADR-0030。Android の KsThemePalette.Dark と同じ値を置く）` の配下に置かれている。節の見出しが、そこに含まれるメンバーに対して事実でなくなっている。`kasane/handbook/cross/comment-policy.md` は区切りコメント (`// MARK:` 等) もコメントとして扱い、「そのファイルだけを読んでいる人にとって意味が通ること」を最低条件としているが、この節を読むと「ライトの Header 背景」が**ダークの生値**であり **Android の `KsThemePalette.Dark` と同じ値**である、と読めてしまう。

second-opinion-code-002 で採用した #6 (`KsThemePalette` の KDoc が変更後の契約と矛盾) と同じ型の、記述が事実でなくなる問題である。実装の正しさには影響しない。

ついでに `Theme.swift:381` の `darkFooterBackgroundColorValue` の doc は「ダークの Footer 背景。」のままで、隣の 2 つ (`:376` `:378`) が透明であることを明記しているのと揃っていない。

**推奨修正**:
節の見出しを両外観を含む言い方へ広げる (例: 「外観別の既定色の生値」) か、ライトの定数だけを別の節へ分ける。見出しの `Android の KsThemePalette.Dark と同じ値を置く` の部分も、ライトの定数を含めるならダークの並びに掛かる説明だと分かる形にする。`darkFooterBackgroundColorValue` の doc も「ダークの Header と同じく透明」の形へ揃えると、3 つが同じ粒度になる。

### [🔵 Suggestion] 新規テストに不要な `!!` があり、コンパイラ警告を 1 件増やしている

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/HostWrappedContextUserContentTest.kt:219` / `:235`

**問題点**:
`composeView` は `:201` で `ComposeView?` として受けた後 `:202` で `assertNotNull` しているが、Kotlin の smart cast は JUnit のアサーションでは効かないため `:219` の `composeView!!` が要る。一方 `:235` の `composeView!!` は `:219` の `!!` によってすでに非 null 化された後なので不要で、ビルドが `Unnecessary non-null assertion (!!) on a non-null receiver of type 'ComposeView'` を出している。本 change が新しく足した警告である。

同じファイルの `sectionCtx` / `rootCtx` は `requireNotNull(...) { "..." }` で受けており、`composeView` だけ流儀が違う。

**推奨修正**:
`val composeView = requireNotNull(composeViewIn(view)) { "CustomCell の行が生成されていない" }` の形に揃え、`assertNotNull` と 2 つの `!!` を落とす。周囲の 2 つと同じ書き方になり、警告も消える。

### [🔵 Suggestion] `findKsSettingsViewHost()` の doc が用途の片方しか語っていない

**該当箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:33-37`

**問題点**:
doc は「行の ViewHolder から、表示中の選択面を預ける先を解決するために使う」と用途を 1 つに限定して書いているが、本 change で `ksUserContentContext()` (利用者所有コンテンツへ渡す Context の解決) という別用途が加わった。読んだ人が「選択面のための関数」と受け取ると、Context 解決側の制約 (親を辿れないと手元の Context にフォールバックする) が視界から外れる。

**推奨修正**:
用途の列挙を外して「何を返すか」だけを書くか、2 つ目の用途を足す。末尾の「`KsSettingsView` の外で単体の行を組み立てた場合は `null` になり、そのときは表示継続の対象にならない」も、選択面固有の帰結なので、返り値の説明と呼び出し側の帰結に分けると両用途に通る。

## 所見 (指摘ではない)

- **新ガードは probe G で単独では落ちるテストが無い**が、これは正常系で no-op になる防御なので当然であり、検出力は probe A との組み合わせで示される。ガード単体を固定するテストを足す価値は薄い
- ガードの式は `verticalScrollbarThumbDrawable == null` だが、コメントは「描画状態が無い」と書いている。ScrollabilityCache は作られるがテーマが `android:scrollbarThumbVertical` を配らない構成では両者がずれ、その場合は夜間モードの追従を黙って行わない。同梱テーマ (Material3 DayNight 派生) は thumb を配るため実害は想定しにくく、`minSdk = 29` なので getter / setter の API レベルも満たしている
- `applyScrollbarThumbForAppearance` の外観判定が `context.isKsDarkAppearance()`、`onConfigurationChanged` が `newConfig.isKsDarkAppearance()` である点は review-002 と同じ所見。ホストが `createConfigurationContext` で別 Configuration を持つ Context を渡した場合に食い違い得るが、通常のホストでは実害は想定しにくい
- `ksScrollIndicatorContext()` は呼び出しのたびに新しい `ContextThemeWrapper` を作るが、呼び出しは設定リストの構築 (1 回) と選択面の生成 (提示ごと) だけで、ラップ元は従来どおりキャッシュ済みの同梱テーマ付き Context なので、テーマを保持する Context の数は増えていない
- 選択面の候補行 (`createRowViews`) は `listView.context` ではなく BottomSheetDialog の `context` から組み立てているため、スクロールバー用のオーバーレイが行の中へ波及する経路は無い
- core/ADR-0032 は依然 `proposed` のまま。accepted な core/ADR-0030 Decision 1 の既定値を実装が置き換えている状態は残っている。アーカイブ (蒸留) の前に 0032 を accepted へ上げ、core/ADR-0030 の index 行にも 0031 と同じ形で「一部改訂: 0032」の後方参照を入れておくと、後から読む人が現行の既定値を追える (`kasane/decisions/` は本レビューの対象外のため所見にとどめる)
- tasks の 5.2b (iOS のスクロールインジケータの実機目視) は未了のまま残っているが、`tasks.md` の項目分割と `evidence/README.md` の「取れていない確認」の双方で開示されており、状態を読み違える余地は無い。iOS 側は実 window に載せたテスト (`showsVerticalScrollIndicator` の観測) で担保されている
- MAUI は facade / bridge の実装が無変更のまま、未指定・指定・表示中の変更 (スクロールバーと背景色の 2 系統) を 4 件のテストが押さえている。`GatewayScope.Connect` の後に `Reset()` を挟む形なので、初期適用と変更適用が混ざらない

## アクションプラン

APPROVED のため必須の対応は無い。着手するなら次の順。

1. [Minor] `ios/Sources/KsSettingsViewUI/Theme.swift:351` の MARK を両外観を含む見出しへ広げる (`:381` の doc も隣 2 つへ揃える)
2. [Suggestion] `HostWrappedContextUserContentTest` の `composeView` を `requireNotNull` へ揃え、コンパイラ警告を落とす
3. [Suggestion] `findKsSettingsViewHost()` の doc を 2 つの用途に通る書き方へ直す

## 追補: 残指摘の解消確認 (2026-09-17)

上記 3 件の修正を静的に確認した (依頼によりビルド・テストの再実行はしていない。ホスト側で comment-policy lint の禁止 0 件・`HostWrappedContextUserContentTest` の実行成功・iOS の実機向けビルド成功を確認済みとの報告を受けている)。**3 件とも解消**しており、判定は APPROVED のまま変わらない。

| # | 指摘 | 状態 | 確認内容 |
|---|---|---|---|
| 1 | 🟡 ライト外観の定数が「ダーク外観の既定色の生値」の節に置かれている | **解消** | `ios/Sources/KsSettingsViewUI/Theme.swift:351` の MARK が `既定色の生値（core/ADR-0030。ダーク外観の値と、両外観で透明な Header / Footer 背景。Android の KsThemePalette と同じ値を置く）` へ。ライトの定数を含むことと、`KsThemePalette` との対応がダーク側に掛かることの両方が読み取れる |
| 2 | 🔵 新規テストに不要な `!!` | **解消** | `HostWrappedContextUserContentTest.kt:201` が `requireNotNull(composeViewIn(view)) { "CustomCell の行が生成されていない" }` へ。以降の 2 か所は `composeView.context` になり、ファイル内に `!!` は 0 件 |
| 3 | 🔵 `findKsSettingsViewHost()` の doc が用途の片方しか語っていない | **解消** | `KsSettingsView.kt:32-40` が「表示中の選択面を預ける先」と「利用者所有コンテンツへ渡す Context」の 2 用途を並べ、`null` のときの帰結も両方 (表示継続の対象外 / 手元の Context から解決) を書いている |

補足:

- 指摘 1 の推奨修正に「ついでに」として添えた `darkFooterBackgroundColorValue` の doc (`ダークの Footer 背景。`) は文面としては据え置きだが、新しい節の見出しが「両外観で透明な Header / Footer 背景」を名指ししているため、節を読めば透明であることは追える。**残指摘としては扱わない**
- 指摘 2 の修正で `assertNotNull` の呼び出しが 1 か所減ったが、同ファイル内の別テストでまだ使われており、未使用 import にはなっていない。`assertEquals` / `assertNotEquals` / `assertSame` も同様
- 3 ファイルとも、修正は指摘した箇所に閉じている。`KsSettingsView.kt` は前回レビュー時点の内容との差分が doc コメント 1 ブロックのみであることを diff で確認した。`Theme.swift` は MARK の 1 行のみが前回レビュー時点から変わっている。実装の挙動に触れる変更は入っていない
- ホスト側で確認済みの項目に加えて `scripts/comment-policy-lint.py --summary` を再実行し、**禁止 0 件 (検査対象 802 ファイル)** を再確認した。`local-path-lint.py` / `identity-lint.py` も検出なし
