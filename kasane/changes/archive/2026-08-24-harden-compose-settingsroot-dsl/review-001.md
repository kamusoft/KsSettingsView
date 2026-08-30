# レビュー結果: harden-compose-settingsroot-dsl (001 回目)

**日付**: 2026-08-22
**判定**: CHANGES_REQUESTED

## サマリー

実装はデルタスペックの 2 Requirement / 9 Scenario をすべて満たしており、公開 API の引数追加はソース互換 (本体・Sample・テストの既存呼び出しはすべて名前付き引数)、`@Target` 制限とスコープ制御の維持もレビュアー自身の実測で確認できた。コメント規約違反の書き換えも適切で、`comment-policy-lint` は禁止 0 件。

ただし Scenario「位置引数で規定の並びどおりに呼び出せる」に対応するテストが、3 つの Boolean 引数をすべて同じ値 `false` で渡しているため、**引数順の入れ替わりを検出できない**。ミューテーション実測で、`isVisible` と `isHeaderVisible` の宣言順を両オーバーロードで入れ替えても全テストが緑のままであることを確認した (下記 Major-1)。本 change の主目的が公開前の API 形状の固定である以上、この一点は塞いでから完了させたい。

## 実行した検証 (実測)

すべてレビュアー自身が実行した。実装者の報告は入力に含まれていない。

| 検証 | コマンド | 結果 |
|---|---|---|
| コンパイルと KT-81567 警告 | `android/` で `./gradlew :ks-settingsview-compose:compileDebugKotlin --rerun-tasks` | BUILD SUCCESSFUL / **25 actionable tasks: 25 executed** (UP-TO-DATE でない)。KT-81567 警告 **0 件** (出力された警告は `ks-settingsview-ui/KsWheelView.kt` の Java deprecated 2 件のみで本変更と無関係) |
| 全テスト (完了判定) | `android/` で `./gradlew test --rerun-tasks` (計 3 回) | **2538 tests / 0 failures** (2 回目・3 回目とも同数で緑)。1 回目のみ既存 flaky が 1 件失敗 — Minor-1 参照 |
| Sample の互換性 | `samples/android/` で `./gradlew :app:compileDebugKotlin --rerun-tasks` | BUILD SUCCESSFUL / 56 tasks executed (composite build で本体ソースを直接コンパイル。UP-TO-DATE でない) |
| コメント規約 | `python3 scripts/comment-policy-lint.py` / `--selftest` / `--advisory` | 禁止 **0 件** (669 ファイル)、selftest 全件 OK。advisory の要確認 2 件はいずれも本変更が触れていない既存ファイル |
| スコープ制御 (a) | 一時ソースで `settingsRoot { section("a") { section("b") {} } }` をコンパイル | `'fun section(...)' cannot be called in this context with an implicit receiver.` でエラー — 注釈除去後もスコープ制御は維持 |
| スコープ制御 (b) | 一時ソースで `KsSettingsView { Section { Section {} } }` をコンパイル | 同種のエラー (`'fun Section(...)' cannot be called in this context with an implicit receiver.`) |
| `@Target` の強制力 | 一時ソースで top-level 関数に `@SettingsRootDsl` を付与 | `This annotation is not applicable to target 'top level function'. Applicable targets: class, type usage, typealias` でコンパイルエラー |
| marker の残存箇所 | `grep -rn "@SettingsRootDsl" src/main` | 6 箇所のみ (受け手 6 クラス)。top-level 関数 29 箇所からは全除去済み |
| 足場の逆流 | `git status` / `git diff --stat` | `proposal.md` / `specs/` に変更なし。`tasks.md` はチェックボックスのみ |
| deviation | `kasane/changes/harden-compose-settingsroot-dsl/deviation.md` | レビュー中に追加。`.gitignore` への `.kotlin/` 追加をオーナー指示の合意差分として記録済み |

**一時変更の原状復帰**: すべてのミューテーションと一時ソースは復元・削除済み。`SettingsRootScope.kt` の shasum が backup と一致 (`66f2f24d…`)、`git status` も元の 8 変更 + 1 未追跡に戻っていることを確認した。

## デルタスペック一致検証 (ksn-verify 兼務)

### Requirement: settingsRoot builder の section は Section と同じ属性を受け取る

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| headerHeight を指定して構築する | `SettingsRootScope.kt:53` (引数) / `:66` (転写) | `SettingsRootBuilderTest.kt:59` `headerHeight を指定すると Section へ転写される` | ✅ 一致 (ミューテーションで転写を落とすと失敗することを実測) |
| Header / Footer の表示トグルを指定して構築する (accessory 版) | `SettingsRootScope.kt:55-56` / `:68-69` | 同 `:73` `Header と Footer の表示トグルを指定しても内容は保持される（accessory 版）` | ✅ 一致 (実測で検出力あり) |
| 文字列ヘッダ版でも同じ属性を指定できる | `SettingsRootScope.kt:90-108` (委譲) | 同 `:93` `文字列ヘッダ版でも同じ属性を指定できる` | ✅ 一致 (実測で検出力あり) |
| 文字列ヘッダ版で footer を省略すると footer は無い | `SettingsRootScope.kt:103` `footer?.let { SectionAccessory.Text(it) }` | 同 `:117` `文字列ヘッダ版で footer を省略すると footer は無い` | ✅ 一致 |
| 省略時は Section data class の既定値と等価 | `SettingsRootScope.kt:50-57` (既定値) | 同 `:127` `新引数を省略した section は Section の既定値で構築した Section と等価` | ✅ 一致 (既定値を drift させると失敗することを実測) |
| 位置引数で規定の並びどおりに呼び出せる | `SettingsRootScope.kt:49-58` / `:90-98` (並びは core `Section` と同一) | 同 `:145` `位置引数で規定の並びどおりに呼び出せる` | ✅ 一致 (実装・テストとも存在。ただし Boolean 3 引数の位置は固定できていない → Major-1) |

### Requirement: SettingsRootDsl marker は型にのみ付与できる

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| marker 注釈の許容ターゲットが型に限定されている | `SettingsRootScope.kt:17` `@Target(CLASS, TYPE, TYPEALIAS)` | `SettingsRootDslMarkerTest.kt:17` | ✅ 一致 (FUNCTION を足すと失敗することを実測) |
| receiver 型の marker 付与が維持されている | `SettingsRootScope.kt:28` / `:125`、`DSLScope.kt:17` / `:148`、`DSLHandles.kt:17` / `:29` | `SettingsRootDslMarkerTest.kt:31` | ✅ 一致 (`SectionScope` の付与を外すと失敗することを実測) |
| ビルドで DSL marker の無効付与警告が出ない | 29 箇所からの注釈除去 (`DSLHandles.kt` 12 / `BasicCellDsl.kt` 8 / `InputCellDsl.kt` 7 / `CustomCellDsl.kt` 2) | ビルドログで確認 (自動テストなし。Scenario の性質上ビルド自体が検証) | ✅ 一致 (レビュアーが `--rerun-tasks` で実測、警告 0 件) |

### 追加検査

- **tasks.md の虚偽チェック**: なし。全 16 タスクをレビュアー側で追試し、いずれも実体を確認した (1.3 / 1.4 / 3.8 / 4.1 / 4.2 は上表の実測に対応)
- **足場の逆流**: なし (`proposal.md` / `specs/spec.md` は未変更)
- **未記録乖離**: なし。レビュー実施中に `deviation.md` が追加され、`.gitignore` への `.kotlin/` 1 行追加が「オーナー指示による proposal 範囲外の対応」として記録された。記録済みの合意差分であり違反として扱わない (内容も確認済み: Android / Gradle セクションへ Kotlin 2.x のビルドセッション作業ディレクトリを追加するのみで、ビルド・テストへの影響なし)
- **UI 変更**: 本 change に `ui/` は無く、UI 表示にも影響しない (builder の値転写のみ)
- **テスト全件成功**: 2538 tests / 0 failures

**一致検証の判定: VALID**

## 指摘事項

### [🟠 Major] Major-1: 位置引数テストが Boolean 3 引数の並び替えを検出できない

**該当箇所**: `android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/SettingsRootBuilderTest.kt:145-170`

**問題点**:
Scenario「位置引数で規定の並びどおりに呼び出せる」の THEN は「各値が規定の位置の引数に対応する」ことを要求しているが、テストは `isVisible` / `isHeaderVisible` / `isFooterVisible` の 3 つにすべて同じ `false` を渡している。3 つとも同型・同値のため、宣言順がどう入れ替わってもアサーションは通る。

ミューテーション実測で確認した (lessons code-review L-001 の手法):

- 両オーバーロードのシグネチャで `isVisible` と `isHeaderVisible` の宣言順を入れ替える (本体は名前付き引数で転写しているため挙動は不変、位置だけが変わる)
- → `./gradlew :ks-settingsview-compose:testDebugUnitTest --rerun-tasks --tests '*SettingsRootBuilderTest*'` は **BUILD SUCCESSFUL** (全件緑)

対照として、`headerHeight` の転写を落とすミューテーションではこのテストを含む 3 件が落ちるので、`headerHeight` の位置 (型と値が他と異なる) だけは固定できている。つまり固定できていないのは Boolean 3 引数の相対順序に限られる。

本 change は初回リリース前に公開 API の形状を確定させることが目的であり、Boolean 3 引数の順序ずれはコンパイルエラーにならず (全て `Boolean`)、現状どのテストにも捕まらないまま利用者側の位置引数呼び出しの意味を静かに変える。

**推奨修正**:
位置引数テストを、各 Boolean 位置を一意に特定できるパターンに変える。値を 1 組だけ変えても (例: `false, true, false`) 1 番目と 3 番目の入れ替えは検出できないため、**「1 つだけ `false`」の呼び出しを 3 通り**用意するのが確実:

- `section("s1", Text("H"), Text("F"), 40.0, false, true, true)` → `isVisible` だけ `false`
- `section("s2", …, true, false, true)` → `isHeaderVisible` だけ `false`
- `section("s3", …, true, true, false)` → `isFooterVisible` だけ `false`

を accessory 版・文字列ヘッダ版それぞれで呼び、各 Section で 3 フィールドを個別に assert する。修正後に上記の順序入れ替えミューテーションで実際に落ちることを確認してほしい。

### [🟡 Minor] Minor-1: 全件実行 1 回目に既存 flaky テストが 1 件失敗した (本変更に起因しない)

**該当箇所**: `android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/DSLAccessoryVisibilityRenderingTest.kt:186-198` (本 change の diff 対象外)

**問題点**:
`./gradlew test --rerun-tasks` の 1 回目で `Store 経路と DSL 経路で Header トグルの表示結果が一致する` が
`expected:<[A, 補足]> but was:<[一般, A, 補足]>` で失敗した。原因は待機の欠落で、`awaitRows(..., index = 0)` で Store 側の収束だけを待ち、DSL 側 (`index = 1`) は待たずに `assertEquals(rowTexts(0), rowTexts(1))` で比較している。加えて同ファイルの `awaitRows` はループ内で `Thread.yield()` を使っており、`kasane/concepts/cross/conventions/test-execution.md` が「OS へのヒントに留まり CPU 飽和時は譲れる保証がない」として退けている形になっている。

本変更との因果は無いと判断した — 変更したのは `settingsRoot { section(...) }` builder と no-op だった注釈の除去のみで、この失敗テストが通る経路 (`KsSettingsView { Section { … } }` DSL と `SettingsRootStore`) には触れていない。実際、同テストクラス単独では 3 回連続で成功し、全件実行の 2 回目も 2538 tests / 0 failures で緑だった (並列実行時のみ表面化する既知のパターン)。

**推奨修正**:
本 change での修正は求めない (スコープ外)。別 change として起票し、比較前に `awaitRows(listOf("A", "補足"), index = 1)` を入れる (または収束条件つきの待機ヘルパで両 index を待つ) 形にし、あわせて `Thread.yield()` を `Thread.sleep(1)` に置き換えることを推奨する。直近の `fix-compose-dsl-double-update-flaky-test` と同型の残件。

### [🔵 Suggestion] Suggestion-1: 追加した import の位置が既存の整列を崩している

**該当箇所**: `android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/SettingsRootBuilderTest.kt:3-5`

**問題点**: `jp.kamusoft.kssettingsview.ui.LabelCell` の後に `jp.kamusoft.kssettingsview.core.Section` が挿入されており、他がアルファベット順に並んでいる中でこの 2 行だけ逆順になっている (ktlint / detekt は未導入のため機械検出はされない)。

**推奨修正**: `core.Section` / `core.SectionAccessory` を `ui.LabelCell` より前に置く。

### [🔵 Suggestion] Suggestion-2: KDoc の `[section]` リンクがオーバーロード間で曖昧

**該当箇所**: `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/SettingsRootScope.kt:40-41` / `:77`

**問題点**: 「文字列を直接受け取るもう一方の [section] を使う」「accessory 版の [section] と同じ」の `[section]` は同名 2 オーバーロードのどちらを指すか一意でなく、Dokka 導入時にリンクが意図しない側へ解決され得る。読解上の実害は小さい。

**推奨修正**: リンク記法をやめて「文字列ヘッダを取る `section` オーバーロード」のように散文で書き分ける。

## 確認したが問題なしと判断した観点

- **公開 API のソース互換**: `section(...)` の呼び出し箇所は `samples/android/app/.../StoreDemoScreen.kt:42`、`KsSettingsViewComposeTest.kt:80` / `:156`、`SettingsRootBuilderTest.kt` の各所で、すべて名前付き引数。位置引数で `isVisible` を渡している箇所はリポジトリ内に存在しない。文字列ヘッダ版に挿入された `footer: String?` は 3 番目だが、旧 3 番目は `Boolean` だったため万一の位置引数呼び出しは型不一致でコンパイルエラーになり、黙って意味が変わることはない
- **文字列ヘッダ版の委譲**: `footer?.let { SectionAccessory.Text(it) }` で `null` を保持しており、空文字列を `Text("")` として渡す点も core `Section` / `DSLScope.Section` と同じ扱い (core/ADR-0023 の「内容なし = null または空文字列」に一致)
- **オーバーロード解決**: accessory 版 `header: SectionAccessory? = null` と文字列版 `header: String` は型で分離されており、引数追加で新たな曖昧性は生じない
- **コメント規約の書き換え品質** (tasks 2.4): `settingsRoot` KDoc は change-id の裸参照を `core/ADR-0009` 参照 (許容形式) と現在形の説明に置換、`CellHandle.disabled` は「本提案 / 後続提案」を現在の契約の説明に置換、`DSLScope.cell` はデルタスペックの裸参照を削除。いずれも 3 類型の指針どおりで、削除後の KDoc も自然に読める。なお旧 KDoc が参照していた `sectionHeaderText` はリポジトリ内に存在しない識別子で、この書き換えで壊れた参照も解消されている
- **`disabled` の説明**: 「常に no-op」「`isEnabled` で構築する」は concepts `android/api/android-compose.md` の記述と一致
- **注釈除去の副作用**: 除去は 29 箇所ちょうどで、受け手 6 クラスへの付与は維持。スコープ制御が変わらないことを 2 経路のコンパイル実測で確認済み

## アクションプラン

1. **Major-1** — 位置引数テストを「1 つだけ `false`」の 3 パターン × 2 オーバーロードに書き換え、Boolean 3 引数の位置を一意に固定する。書き換え後に順序入れ替えミューテーションで落ちることを実測する
2. **Suggestion-1 / Suggestion-2** — 余力があれば同時に対応 (任意)
3. **Minor-1** — 本 change の外。`DSLAccessoryVisibilityRenderingTest` の待機不足を別 change として起票する
4. 修正後、`android/` 直下で `./gradlew test --rerun-tasks` を再実行し `N tests / M failures` を報告する
