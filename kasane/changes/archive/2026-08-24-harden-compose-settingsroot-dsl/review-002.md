# レビュー結果: harden-compose-settingsroot-dsl (002 回目)

**日付**: 2026-08-22
**判定**: APPROVED

## サマリー

前回 (review-001) の Major-1 は閉じた。位置引数テストは「3 つの Boolean のうち 1 つだけ `false`」× 3 パターン × 2 オーバーロードに置き換わり、レビュアー自身のミューテーション実測で **Boolean 3 引数の 3 通りの入れ替えすべてが検出される**こと、かつ **オーバーロード単位で独立に検出される**ことを確認した。Suggestion-1 / Suggestion-2 も対応済み。今回スコープに追加された Minor-1 (既存 flaky) の修正も、`test-execution.md` が定める待機の 3 条件 (実時間 deadline / 実行機会の譲渡 / 超過時の `fail()`) をすべて満たす構造で、固定 sleep によるごまかしは混入していない。

デルタスペックの 2 Requirement / 9 Scenario はすべて実装・テストに対応 (**一致検証: VALID**)。全テスト **2540 tests / 0 failures**、KT-81567 警告 0 件、comment-policy-lint 禁止 0 件。残る指摘は任意対応の Suggestion 1 件のみ。

## 実行した検証 (実測)

すべてレビュアー自身が実行した。実装者の報告は判断材料にしていない。

| 検証 | コマンド | 結果 |
|---|---|---|
| 全テスト (完了判定) | `android/` で `./gradlew test --rerun-tasks` (2 回) | 2 回とも **BUILD SUCCESSFUL** (5m15s / 4m38s、`230 actionable tasks: 230 executed`)。`TEST-*.xml` 集計で **2540 tests / 0 failures / 0 skipped** (前回 2538 から +2 = 位置引数テストが 1 → 2 本 × 2 variant) |
| コンパイルと KT-81567 | `android/` で `./gradlew :ks-settingsview-compose:compileDebugKotlin --rerun-tasks` | BUILD SUCCESSFUL / **25 actionable tasks: 25 executed** (UP-TO-DATE でない)。KT-81567 (DSL marker ... has no effect) **0 件**。出た警告は `ks-settingsview-ui/KsWheelView.kt` の Java deprecated 2 件のみで本変更と無関係 |
| Sample の互換性 | `samples/android/` で `./gradlew :app:compileDebugKotlin --rerun-tasks` | BUILD SUCCESSFUL / 56 tasks executed |
| コメント規約 | `python3 scripts/comment-policy-lint.py` / `--selftest` | 禁止 **0 件** (669 ファイル)。selftest 全件 OK |
| 足場の逆流 | `git diff -- proposal.md specs/ exploration.md` / `git log` | 差分なし。`tasks.md` はチェックボックスのみ |
| 一時ハーネス・残骸 | `grep -rn "println|TODO|FIXME|Log.d|System.out" src/` / `git status -uall` | 該当 0 件。未追跡は `SettingsRootDslMarkerTest.kt` と kasane 証跡 3 件のみ |
| `.gitignore` の実効 | `git check-ignore -v android/.kotlin` | `.gitignore:40:.kotlin/` でヒット。`git status -uall` からも消えている |

### ミューテーション実測 (Major-1 の回帰検出力)

`SettingsRootScope.kt` の **シグネチャの宣言順だけ**を入れ替える (本体は名前付き引数で転写するため挙動は不変、位置の意味だけが変わる)。`./gradlew :ks-settingsview-compose:testDebugUnitTest --rerun-tasks --tests '*SettingsRootBuilderTest*'` を都度実行。

| # | ミューテーション | 結果 |
|---|---|---|
| A | `isVisible` ⇄ `isHeaderVisible` (両オーバーロード) | **FAILED** — 位置引数テスト 2 本 (accessory 版 / 文字列ヘッダ版) が `SettingsRootBuilderTest.kt:252` で失敗。10 tests completed, 2 failed |
| B | `isHeaderVisible` ⇄ `isFooterVisible` (両オーバーロード) | **FAILED** — 同 2 本が失敗 |
| C | `isVisible` ⇄ `isFooterVisible` (両オーバーロード) | **FAILED** — 同 2 本が失敗 |
| D | `isVisible` ⇄ `isHeaderVisible` (**文字列ヘッダ版のみ**) | **FAILED** — 文字列ヘッダ版の 1 本だけが失敗。10 tests completed, 1 failed |

- A / B / C により、Boolean 3 引数の **3 通りの互換 (transposition) すべて**が検出されることを確認した。3 要素の並べ替えは互換の合成で尽くせるため、任意の並び替えが検出される
- D により、検出が**オーバーロード単位で独立**していること (片方だけの退行を他方のテストが肩代わりして隠すことがないこと) を確認した
- 前回 (review-001) の同型ミューテーションは全件緑だった。**Major-1 は閉じている**

**原状復帰**: すべてのミューテーションは backup から復元済み。`SettingsRootScope.kt` の shasum が backup と一致 (`b50890b5…`)、`DSLAccessoryVisibilityRenderingTest.kt` も一致 (`4c4af0b9…`)、`git status` も元の 10 変更 + 4 未追跡に戻っている。復元後に `./gradlew :ks-settingsview-compose:test --rerun-tasks` が BUILD SUCCESSFUL (130 tasks executed) であることも確認した。

## 前回指摘のクローズ確認

| 前回指摘 | 状態 | 根拠 |
|---|---|---|
| 🟠 **Major-1** 位置引数テストが Boolean 3 引数の並び替えを検出できない | ✅ **クローズ** | `SettingsRootBuilderTest.kt:145-201` / `:203-233` が「1 つだけ `false`」× 3 パターン × 2 オーバーロードに置換。共通アサーション `assertPositionalSection` (`:243-256`) が 6 フィールドを個別照合。上表 A〜D のミューテーションで実際に落ちることを実測 |
| 🟡 **Minor-1** 既存 flaky (`DSLAccessoryVisibilityRenderingTest`) | ✅ **クローズ** (deviation でスコープ追加) | 下記「Minor-1 修正の構造評価」参照 |
| 🔵 **Suggestion-1** import の整列崩れ | ✅ **クローズ** | `SettingsRootBuilderTest.kt:3-5` が `core.Section` → `core.SectionAccessory` → `ui.LabelCell` のアルファベット順に是正 |
| 🔵 **Suggestion-2** KDoc の `[section]` リンクがオーバーロード間で曖昧 | ✅ **クローズ** | `SettingsRootScope.kt:41-42` は「文字列ヘッダを取る `section` オーバーロード」、`:76-77` は「`SectionAccessory` を取る `section` オーバーロード」と散文に書き分け。`[section]` リンクは残っていない |

### Minor-1 修正の構造評価

対比実測は取れていない (レビュアー側でも、修正前の版に戻して 12 並列の CPU 負荷下で `DSLAccessoryVisibilityRenderingTest` を回したが、完走した 2 回はいずれも成功で再現しなかった)。したがって**構造面から評価**した。

修正前の失敗 (`expected:<[A, 補足]> but was:<[一般, A, 補足]>`) は、`awaitRows(..., index = 0)` で Store 側だけ待って `assertEquals(rowTexts(0), rowTexts(1))` に進み、**DSL 側 (index 1) を待たずに読んでいた**ことが原因。修正はこの読み取り前の未待機を直接塞いでいる:

- `awaitBothRows(expected)` (`:122-124`) が index 0 / index 1 の**両方**の収束を待つ。比較の前に両側が期待値へ到達していることが保証される
- 対称性テスト 2 本の 4 箇所すべて (`:200` `:206` `:228` `:233`) が `awaitBothRows` に置換され、片側だけ待つ呼び出しは残っていない (`grep` で `index = ` を伴う呼び出しは `awaitBothRows` 内の 2 行のみ)
- 待機条件は**期待値そのもの** (`rowTexts(index) == expected`) であり、固定時間の待機・回数ループ・「とりあえず idle を重ねる」形は導入されていない

`test-execution.md`「非同期反映を待たないアサーション」が要求する 3 条件も `awaitRows` (`:98-114`) がすべて満たす:

1. 上限は**実時間の deadline** — `System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(5_000)` ✅
2. ループ内で**バックグラウンドへ実行機会を譲る** — `Thread.yield()` → `Thread.sleep(1)` に置換 ✅。規約が明示する形 (「`Thread.sleep(1)` 等」) そのもので、姉妹実装 `KsSettingsViewComposeTest.kt:388` とも一致する。**固定 sleep による待機ではない** — 収束すれば即 `return` し、sleep はループ 1 周あたり 1 ms の譲渡にすぎない
3. deadline 超過時は**その時点の実測値を載せて `fail()`** — `"表示行が … へ収束しなかった (index=… の現在の行: …)"` ✅

ごまかし (テスト末尾やアサーション直前への固定 sleep の挿入、timeout の水増し、アサーションの緩和) は混入していない。timeout は 5,000 ms のまま、アサーションはむしろ強化されている (後述)。

## デルタスペック一致検証 (ksn-verify 兼務)

### Requirement: settingsRoot builder の section は Section と同じ属性を受け取る

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| headerHeight を指定して構築する | `SettingsRootScope.kt:53` (引数) / `:66` (転写) | `SettingsRootBuilderTest.kt:59` `headerHeight を指定すると Section へ転写される` | ✅ 一致 |
| Header / Footer の表示トグルを指定して構築する (accessory 版) | `SettingsRootScope.kt:55-56` / `:68-69` | 同 `:73` `Header と Footer の表示トグルを指定しても内容は保持される（accessory 版）` | ✅ 一致 |
| 文字列ヘッダ版でも同じ属性を指定できる | `SettingsRootScope.kt:91-112` (委譲) | 同 `:93` `文字列ヘッダ版でも同じ属性を指定できる` | ✅ 一致 |
| 文字列ヘッダ版で footer を省略すると footer は無い | `SettingsRootScope.kt:104` `footer?.let { SectionAccessory.Text(it) }` | 同 `:117` `文字列ヘッダ版で footer を省略すると footer は無い` | ✅ 一致 |
| 省略時は Section data class の既定値と等価 | `SettingsRootScope.kt:50-57` (既定値。core `Section.kt:44-53` と同一) | 同 `:127` `新引数を省略した section は Section の既定値で構築した Section と等価` | ✅ 一致 |
| 位置引数で規定の並びどおりに呼び出せる | `SettingsRootScope.kt:49-58` / `:91-100` | 同 `:145` `位置引数で規定の並びどおりに呼び出せる（accessory 版）` / `:203` `（文字列ヘッダ版）` | ✅ 一致 (ミューテーション A〜D で検出力を実測。詳細は下記「論点への判定」) |

### Requirement: SettingsRootDsl marker は型にのみ付与できる

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| marker 注釈の許容ターゲットが型に限定されている | `SettingsRootScope.kt:17` `@Target(CLASS, TYPE, TYPEALIAS)` | `SettingsRootDslMarkerTest.kt:17` | ✅ 一致 |
| receiver 型の marker 付与が維持されている | `SettingsRootScope.kt:28` / `:126`、`DSLScope.kt:17` / `:148`、`DSLHandles.kt:17` / `:29` | `SettingsRootDslMarkerTest.kt:31` | ✅ 一致 (`grep -rn "@SettingsRootDsl" src/main` は 6 箇所ちょうど。top-level 関数 29 箇所からは全除去済み) |
| ビルドで DSL marker の無効付与警告が出ない | 29 箇所からの注釈除去 (`DSLHandles.kt` 12 / `BasicCellDsl.kt` 8 / `InputCellDsl.kt` 7 / `CustomCellDsl.kt` 2) | 自動テストなし (Scenario の性質上ビルド自体が検証) | ✅ 一致 (`--rerun-tasks` で 25 tasks executed、KT-81567 警告 0 件を実測) |

### 追加検査

- **tasks.md の虚偽チェック**: なし。全 16 タスクの実体をレビュアー側で追試した (1.3 / 3.8 / 4.1 / 4.2 は上表の実測に対応。1.4 のスコープ制御は review-001 で 2 経路とも実測済みで、当該コードは今回変更されていない)
- **足場の逆流**: なし (`proposal.md` / `specs/spec.md` / `exploration.md` は未変更)
- **未記録乖離**: なし。`.gitignore` への `.kotlin/` 追加と `DSLAccessoryVisibilityRenderingTest.kt` のスコープ追加はいずれも `deviation.md` にオーナー指示として記録済みで、合意済み差分として扱った
- **UI 変更**: 本 change に `ui/` は無く、UI 表示に影響しない (builder の値転写のみ)
- **テスト全件成功**: 2540 tests / 0 failures (2 回とも同数)

**一致検証の判定: VALID**

## 論点への判定 — Scenario「位置引数で規定の並びどおりに呼び出せる」の GIVEN とのズレ

依頼にあった「GIVEN は全 `false` の呼び出しを例示しているが、テストは 1 つだけ `false` × 3 パターン × 2 オーバーロードに置き換わった」件を、verify の観点で判定した。

**結論: Scenario の要求を満たす (✅ 一致)。deviation としての記録も不要。**

根拠:

1. **GIVEN は例示である。** 原文は「`section("s1", …, 40.0, false, false, false) { ... }` **のように**名前を付けずに呼ぶ」と書かれており、「のように」は呼び出し形式 (名前を付けない位置引数呼び出し) を例で示す語。特定の実引数の組を GIVEN が固定しているわけではない。テストは同じ呼び出し形式 (名前なし位置引数、両オーバーロード) を保っている
2. **THEN の規範は括弧内にある。** 「(各値が規定の位置の引数に対応する)」が検証すべき性質で、Requirement 本文の「引数の並びは両オーバーロードとも core `Section` (`header, footer, headerHeight, isVisible, isHeaderVisible, isFooterVisible`) に揃え、末尾ラムダ `block` を最後に置く (SHALL)」がその契約。修正後のテストはこの対応関係を 6 引数すべてについて一意に固定する
3. **検出力は真に強くなった、かつ何も失われていない。** 全 `false` では 3 つの Boolean が相互に区別できず、宣言順の入れ替えを一切検出できなかった (review-001 で実測)。1 つだけ `false` の 3 パターンは各 Boolean の位置を一意に特定する。`header` / `footer` / `headerHeight` は 3 パターンすべてで従来どおり照合されている。6 引数は互いに独立に転写されるだけで結合はないため、「3 つ同時に `false`」という組合せに追加の情報量はない (なお文字列ヘッダ版については `:92` のテストが名前付き引数で 3 つ同時 `false` を照合済み)
4. **1 Scenario に 2 テストが対応する点も問題ない。** Scenario の GIVEN が「accessory 版を…、文字列ヘッダ版を…」と 2 オーバーロードを並べているため、オーバーロードごとにテストを分けるのはむしろ素直な写像。ksn-verify は 1:1 対応を要求していない (Scenario が実装とテストで表現されていることが要件)

したがって「GIVEN の字面とのズレ」は spec の不備でも実装の逸脱でもなく、例示部分に対する強化である。NEEDS_DISCUSSION にはしない。

## 指摘事項

### [🔵 Suggestion] Suggestion-1: marker の許容ターゲット検証が「順序」まで固定している

**該当箇所**: `android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/SettingsRootDslMarkerTest.kt:20-27`

**問題点**: `assertEquals(listOf(CLASS, TYPE, TYPEALIAS), target!!.allowedTargets.toList())` は `List` 比較のため、`@Target` の引数を無害に並べ替えただけでも失敗する。対応する Scenario の THEN は「許容ターゲットは `CLASS` / `TYPE` / `TYPEALIAS` の 3 つだけである」と**集合**を要求しており、順序は契約に含まれない。実害は小さい (並べ替える動機がほぼ無く、失敗しても原因はすぐ分かる) が、契約より強く縛っている分だけ将来の偽陽性の芽になる。

**推奨修正**: `assertEquals(setOf(...), target!!.allowedTargets.toSet())` にする (集合比較なら「3 つだけ」の要求も過不足なく表現できる)。**任意対応** — 本 change を止める理由にはしない。

## 確認したが問題なしと判断した観点

- **前回 APPROVED 相当だった箇所の退行**: なし。`SettingsRootScope.kt` の本体ロジック (両オーバーロードの引数・既定値・転写・委譲) は前回から無変更で、変わったのは KDoc の Suggestion-2 対応 (`:41-42` / `:76-77`) だけ。core `Section.kt:44-53` の既定値 (`headerHeight = -1.0` / 3 つの Boolean = `true`) とも一致し続けている
- **公開 API のソース互換**: `section(...)` の呼び出し箇所を再確認 (`grep -rn "\bsection(" --include='*.kt' samples/android android/`)。`StoreDemoScreen.kt:42`、`KsSettingsViewComposeTest.kt:81` / `:157`、`SettingsRootBuilderTest.kt` の既存呼び出しはすべて名前付き引数。位置引数で `isVisible` を渡している箇所はリポジトリ内に存在しない
- **`DSLScope.Section` との対称性**: `DSLScope.kt:33-42` の引数順 (`header, footer, headerContent, footerContent, headerHeight, isVisible, isHeaderVisible, isFooterVisible, block`) と、今回の `section(...)` の並びは同じ相対順序。proposal が狙った「Section / iOS `ksSection` / `DSLScope.Section` との対称」が保たれている
- **対称性テストの最終 `assertEquals(rowTexts(0), rowTexts(1))`**: `awaitBothRows` が両側を期待値へ収束させた後なので、この 1 行に追加の検出力は無くなった。ただしテストの主旨 (Store 経路と DSL 経路の一致) を明示する記述として残す価値があり、`expected` を両側に固定した分だけ全体としては**前より強い**アサーションになっている。退行ではないため指摘としない
- **`.gitignore` の `.kotlin/`**: 先頭 `/` の無いパターンなので任意階層にマッチし、`android/.kotlin` が実際に無視されている (`git check-ignore` で確認)。Android / Gradle セクションに置かれているが Kotlin ツールチェーン由来のディレクトリはルートにも出得るため、グローバル一致で妥当。ビルド・テストへの影響なし
- **コメント規約**: 今回追加・変更されたコメント (`awaitRows` / `awaitBothRows` / `assertPositionalSection` の KDoc、`SettingsRootDsl` の KDoc) はいずれも change-id・レビュー通番・デルタスペックの裸参照・履歴記述・`SHALL` 等の構文キーワードを含まず、そのファイルだけで読んで意味が通る。lint も禁止 0 件
- **公開 API の KDoc 品質**: 両オーバーロードの `@param` が全引数を網羅し、既定値の意味は core `Section` の KDoc に委ねて重複を避けている (tasks 2.3 の方針どおり)。文字列ヘッダ版の `footer` は `null` の意味と `SectionAccessory.Text` へのラップまで書かれており、`section` オーバーロード間の使い分けも散文で判別できる
- **tasks.md にスコープ追加分のタスク行が無いこと**: `deviation.md` が記録の正しい置き場 (ksn-core: 合意済み乖離は deviation.md に記録) であり、tasks.md の拡張は必須ではない。虚偽チェックにも当たらない
- **一時ハーネス・デバッグコードの残骸**: なし。`println` / `TODO` / `FIXME` / `Log.d` / `System.out` の該当 0 件。review-001 で使われた一時コンパイル用ソースも残っていない (未追跡ファイルは新規テスト 1 本と kasane 証跡のみ)

## 範囲外の申し送り (本 change では対応不要)

`test-execution.md` は「非同期反映を待たないアサーション」の**適用実例**として `ks-settingsview-ui` の `KsSettingsViewTestSupport.awaitConvergence` を挙げているが、当の実装は今も `Thread.yield()` を使っており、規約が退けている形のまま残っている (`KsSettingsViewTestSupport.kt:58` / `:93`、ほかに `CustomCellRecycleTest.kt:389` / `CustomCellBuilderReleaseTest.kt:162`)。本 change の diff 対象外 (別モジュール) なのでここでは指摘としないが、規約と実例が食い違っている状態なので、`Thread.yield()` → `Thread.sleep(1)` の横断置換を別 change として起票することを推奨する。

## アクションプラン

1. 本 change は **APPROVED**。修正必須の指摘はない。蒸留 (ksn-distill) へ進んでよい
2. Suggestion-1 (`allowedTargets` の集合比較化) は任意。対応するなら 1 行の変更で、対応しない判断も妥当
3. 範囲外の申し送り (`ks-settingsview-ui` に残る `Thread.yield()` 待機) を別 change として起票する
