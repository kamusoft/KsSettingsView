# レビュー結果: consolidate-robolectric-wait-helpers (002 回目)

**日付**: 2026-08-09
**判定**: APPROVED

## サマリー

review-001 の Minor (待機失敗時の診断劣化) と Suggestion (KDoc リンク記法) はいずれも解消している。修正は指摘の趣旨どおり「判別力の回復」と「埋もれの解消」を両立させており、実測でも既定メッセージは **1250 文字 → 73 文字**に縮み、`コミット済みリスト` と `内部 root` の 2 軸が両方載る形になった。Theme の全 dump は、待機条件に Theme を含む唯一の箇所 (`AttachOrderRestoreTest` の取り付け後収束) だけが `extraDiagnostics` で受け取る。

Android 全件テストは `./gradlew test --rerun-tasks` で 2024 tests / 0 failures / 0 errors / 0 skipped、`comment-policy-lint.py` は禁止 0 件。新規の指摘は Critical / Major / Minor いずれもなし。残る Suggestion 2 件は本変更のスコープ外 (フォローアップとコミット手順) で、実装の修正を要求するものではない。

## review-001 指摘の解消確認

### [🟡 Minor] 待機失敗時の診断劣化 → **解消**

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsViewTestSupport.kt:39-60`

推奨修正の両案 (内部 root の常時併記 / `extraDiagnostics` 引数) が組み合わせて適用されている。一時プローブでタイムアウト経路を実際に走らせた実測値:

| 経路 | 実測メッセージ | 長さ |
|---|---|---|
| 既定 | `収束の待機条件が 200 ms 以内に成立しなかった (コミット済みリスト: [見出し, A, B, 脚注] / 内部 root: [A, B])` | 73 文字 |
| `extraDiagnostics` あり | 上記 + ` / Theme: Theme(separatorColor=…)` | 1250 文字 |
| bind 前 | `… (コミット済みリスト: [] / 内部 root: [])` | 例外なし |

review-001 で問題にした 2 点はどちらも解消している。

- **判別力**: `内部 root` (Store → Host の到達) と `コミット済みリスト` (Adapter のコミット) が全テストで併記されるようになり、不達がどちらの段で止まったかを切り分けられる。プローブで detach 中に Store を更新した状態を作って確認したところ、2 軸が別々の値として観測できることを実測
- **埋もれ**: 既定メッセージから Theme 全 dump が外れ、判別に効く情報だけが残った。約 17 分の 1 の長さになっている

`extraDiagnostics` の割り当ても妥当。`awaitConvergence` の呼び出しは 17 箇所あり、待機条件に `view.internalTheme()` を含むのは `AttachOrderRestoreTest.kt:161-163` の 1 箇所だけで、そこだけが `extraDiagnostics` を渡している。他の 16 箇所の条件は `committedTexts` / `currentList` / `observer.events` のいずれかで Theme を参照しない。

### [🔵 Suggestion] ファイル冒頭コメントの KDoc リンク記法 → **解消**

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsViewTestSupport.kt:15-16`

`[idle]` 等の角括弧が外れ、素の識別子名になった。ブロックコメント (`/* */`) の選択は維持されており、`/** */` にして直後の `idle()` の doc comment に吸われる副作用も起きていない。

## 再確認依頼への回答

### StoreUnbindTest / UnknownSectionAccessoryHostTest から Theme が消えた点

**診断劣化にならない。** オーケストレーターの判断に同意する。根拠は 2 点。

- 両テストの `awaitConvergence` 条件 (`StoreUnbindTest.kt:90,95,122,142,154` / `UnknownSectionAccessoryHostTest.kt:96,117`) はいずれも `committedTexts(view) == listOf(...)` のみで、Theme を参照しない。時間切れの原因が Theme 側にあることはあり得ないため、Theme は判別に寄与しないノイズだった
- `UnknownSectionAccessoryHostTest` の集約前の失敗メッセージには元々 Theme が入っておらず (`コミット済みリスト` のみ)、今回の形は「元の情報 + 内部 root」で純増。`StoreUnbindTest` は Theme を失った代わりに `内部 root` を得ており、当該テストの争点 (unbind 後に Store 更新が Host へ届かないこと) にはこちらが直接効く

なお `StoreUnbindTest.kt:109` の `assertEquals("解除後の Theme 更新も届かない", Theme(), view.internalTheme())` は `awaitConvergence` を経由しない通常のアサーションであり、失敗時は JUnit が expected / actual を出す。Theme の検証力自体は失われていない。

### 修正 diff が Minor の趣旨を満たしているか

満たしている (上表の実測を参照)。加えて副次的な改善が 2 点ある。

- `cellTitles` は `AttachOrderRestoreTest.kt` と `AdapterReattachTest.kt` にバイト一致で重複していたが、共有化により重複が消えた。review-001 で「集約の副次的な整理にもなる」と書いた点がそのまま実現している
- `extraDiagnostics` はラムダで受け取るため、成功時には `Theme.toString()` が評価されない。収束が成立する通常経路にコストを持ち込まない形になっている

### タイムアウト経路の実測 (任意項目)

実施した。上表のとおり 4 ケース (既定 / extraDiagnostics あり / bind 前 / 2 軸の切り分け) をプローブで測定し、すべて期待どおり。

特に **bind 前でも診断が例外を投げない**ことを確認した意味は大きい。`cellTitles` は `view.internalRoot()` を常時呼ぶようになったため、未初期化なら診断自体が例外で落ちて本来の時間切れ原因を隠す危険があった。実装は `KsSettingsView.kt:98` で `private var internalRoot: SettingsRoot = SettingsRoot()` と非 null 初期化されており、bind 前は `内部 root: []` を報告して正常に `AssertionError` を投げる。

## 指摘事項

Critical / Major / Minor なし。

### [🔵 Suggestion] 2 つの診断リストは対象範囲が異なる

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsViewTestSupport.kt:53-54`

**問題点**: `committedTexts` は Section header / footer を含み、`cellTitles` は Cell の title だけを拾う。収束済みでも `コミット済みリスト: [見出し, A, B, 脚注] / 内部 root: [A, B]` のように対象範囲の異なる 2 つの列が並ぶため、header / footer を持つ root では一見「不一致」に読める。ヘルパ 2 つの KDoc (`:63-69` と `:80`) を読めば対象範囲の違いは分かるので実害は小さい。

**推奨修正**: 対応するなら、失敗メッセージのラベルを対象範囲が伝わる語 (例: `内部 root の Cell:`) にする程度で足りる。現状のままでも許容できる。

### [🔵 Suggestion] コミット対象の切り出し (review-001 から継続、範囲が拡大)

**該当箇所**: `docs/architecture.md`、`docs/cells.md`、`docs/core-model.md`、`docs/platform-guide-android.md`、`docs/platform-guide-ios.md`、`docs/styling-and-theming.md`、`.agents/skills/docs-refresh/SKILL.md`

**問題点**: review-001 時点では 2 ファイルだった並行 docs-refresh 作業が 7 ファイルへ拡大している。いずれも本変更とは無関係のためレビュー対象外 (未レビュー) とした。`git add -A` 相当でコミットすると本変更に混入する。

**推奨修正**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/` 配下の 7 ファイルのみをパス指定で staging する。

## 確認した観点

**修正による等価性の維持**

- `awaitConvergence` のシグネチャ変更 (`extraDiagnostics` を `timeoutMillis` と `condition` の間に挿入) は既存呼び出しを壊さない。17 箇所すべてが `condition` を trailing lambda で渡しており、位置引数で `condition` を渡している箇所はない。`extraDiagnostics: (() -> String)?` と `condition: () -> Boolean` は戻り値型が異なるため、取り違えは型エラーになり黙って通ることもない
- `cellTitles` の共有化 (`private` → `internal`) で挙動は不変 (実装はバイト一致の移動)。ローカル定義の残存なし
- 待機ループ本体 (`idle()` → `condition()` → 期限判定 → `Thread.yield()`) は 001 レビュー時から無変更。待機挙動そのものは今回も変わっていない
- 削除に伴う import の取り残しなし。`AttachOrderRestoreTest` の `ViewGroup` / `TextView` は `collectTextViews` (`rowTextColor` 用、`collectTexts` とは別用途) が今も使用中

**テスト実行** (cross/conventions/test-execution.md 準拠)

- `cd android && ./gradlew test --rerun-tasks` → BUILD SUCCESSFUL、**2024 tests / 0 failures / 0 errors / 0 skipped** (`*/build/test-results/*/TEST-*.xml` 集計、4 モジュール × debug / release)
- オーケストレーター報告の 1604 件は `:ks-settingsview-ui:test` 単体の件数。件数は 001 レビュー時の全件と一致しており、テストの増減はない

**コメント規約**

- `python3 scripts/comment-policy-lint.py` → 検査対象 478 ファイル / 禁止 0 件
- 追記された KDoc (`:35-37`) は変更提案 ID・フェーズ番号・アーカイブ文書パス・デルタスペック構文キーワードを含まず、`extraDiagnostics` を使う条件がファイル単独で読み取れる

**レビュー手法の原状復帰**

- 一時プローブ (`ZzReviewProbeTest.kt`) は `trash` で削除済み。レビュー対象 7 ファイルは baseline との `shasum -c` 全件 OK

## アクションプラン

1. コミット時に `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/` 配下の 7 ファイルのみをパス指定で staging する (docs 側 7 ファイルの混入回避)
2. (任意) 失敗メッセージの `内部 root:` ラベルを対象範囲が伝わる語にする
3. 後続変更の候補 (review-001 から継続、本変更のスコープ外): 同パッケージのインライン `shadowOf(Looper.getMainLooper()).idle()` 約 30 箇所を共有 `idle()` へ寄せる
