# レビュー結果: fix-entrycell-ime-composition (001 回目)

**日付**: 2026-08-01
**判定**: APPROVED (再確認により更新。初回判定は CHANGES_REQUESTED)

> 初回判定は CHANGES_REQUESTED。Minor 2 件の修正を受けた再確認で APPROVED に更新した。経緯は末尾の「再確認 (2026-08-01)」を参照。以下の初回指摘は証跡としてそのまま残す。

## サマリー

合意済みスコープ (A 案: `EntryCellViewHolder.bind()` の IME 破壊系プロパティへの差分ガード) は正しく実装されており、`inputType` / `hint` / `filters` の 3 ガードはいずれも「View の現在状態と比較する」形で、正当な更新を落とさない収束的な作りになっている。Android 全モジュールのユニットテストは 516 件全 pass (core 74 / ui 366 / compose 76、失敗・エラー・skip なし)、新規 5 件も pass を確認した。

修正を求めるのは 2 点のみ: 触れた 2 ファイルに残るソースコメント規約違反 (各 1 行) と、placeholder テストがガード撤去を検出できない (実効性がない) 点。実装ロジックそのものへの Critical / Major 指摘はない。

## 指摘事項

### [🟡 Minor] 触れた 2 ファイルに `openspec/` パス参照のコメントが残っている

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCellViewHolder.kt:32`、`android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/InputCellsTest.kt:35`

**問題点**: 両ファイルのクラス KDoc に `仕様: openspec/changes/add-cell-types-input/specs/cell-types-input/spec.md` が残っている。`concepts/cross/conventions/comment-policy.md` の「禁止する参照 — アーカイブ文書のパス」に該当する。同規約は適用契機として「既存コメントに触れる実装をするとき・コードレビューのとき」を挙げており、本 change は両ファイルを変更している (`EntryCellViewHolder.kt` については、違反行が変更対象である `bind` を説明する KDoc そのもの)。直前の change で同型の違反がレビュー Major となり、オーナーが「触れたファイルの部分だけ直す」と裁定した経緯もある。

**推奨修正**: comment-policy の「1. 定型句型」に従い、`仕様: ...` の 1 行を削除する。上部の説明文は参照句がなくても自然に読めるため、整形は不要。新規に追加されたコメント (`EntryCellViewHolder.kt:79-81`, `95-97`, `134-135` および試験側 KDoc) は規約準拠であり、こちらの修正は不要。

### [🟡 Minor] placeholder のテストがガード撤去を検出できない

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/InputCellsTest.kt:244-254`

**問題点**: `EntryCell 同値 Cell の再 bind でも placeholder は維持される` は bind 後の `hint` の**値**しか見ていないため、`EntryCellViewHolder.kt:82-84` のガードを撤去しても pass する (ガードなしでも hint の値は同じになる)。本 change が守ろうとしているのは「setter を呼ばないこと」であり、値の一致ではない。`inputType` (呼び出し回数カウント) と `filters` (`assertSame` による同一配列参照) の 2 テストは撤去を正しく検出できるので、hint だけが回帰を素通りさせる。

**推奨修正**: `InputTypeCountingEditText` と同じ手法で `setHint` の呼び出し回数を数える (同一 fake に `override fun setHint(hint: CharSequence?)` を足して `createEntryCellViewHolder` 経由で注入する) か、値検証テストは残しつつ「同値再 bind で setHint が呼ばれない」ケースを 1 件追加する。

### [🔵 Suggestion] filters ガードは「先頭要素が LengthFilter」以外の状態を復旧できない

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCellViewHolder.kt:136-143`

**問題点**: `(editText.filters.firstOrNull() as? InputFilter.LengthFilter)?.max` は、先頭が LengthFilter 以外 (または空) のとき一律 `null` を返す。`cell.maxLength == null` かつ EditText に別種のフィルタが載っている状態では `null == null` で差し替えがスキップされ、そのフィルタが残り続ける。現状は EditText を `create()` 内で生成し filters を書くのが本 ViewHolder だけ (`reset()` の `arrayOf()` を含む) なので**到達しない**が、不変条件がコメントの宣言だけに依存している。

**推奨修正**: 現状のままでも実害はない。将来の耐性を上げるなら、比較条件を「`filters.size <= 1` かつ先頭が期待どおり」に広げ、崩れていたら差し替える形にすると不変条件が自己検査になる。

### [🔵 Suggestion] テストヘルパーが `EntryCellViewHolder.create()` から静かに乖離し得る

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/InputCellsTest.kt:260-265`

**問題点**: `createEntryCellViewHolder` は `create()` の構築手順 (`buildCellBaseViews` → `isSingleLine` → `accessoryHolder.addView`) を複製しており、`minWidth` など一部が欠けている。`create()` 側の初期設定が変わっても、このテストだけが古い前提のまま緑になる。特に `isSingleLine` は `inputType` の内部状態 (MULTI_LINE フラグ) に影響するため、ガードの検証結果に効き得る。

**推奨修正**: 本 change では現状でよい。継続的に fake EditText を注入していくなら、`create()` に EditText を生成するラムダ (既定は現在の実装) を渡せるようにして、テストは初期設定を共有する形が安全。

### [🔵 Suggestion] IME 実挙動の確認結果が残っていない

**該当箇所**: 変更全体

**問題点**: Robolectric で検証できるのは「setter が呼ばれないこと」までで、報告された症状 (「あ」が即時確定される) が消えたことは検証されていない。exploration.md もこの限界を認めている。S 級で ADR も残らないため、アーカイブ後にこの change の妥当性を裏づける記録がテストだけになる。

**推奨修正**: サンプルアプリの「入力 Cell 5 種デモ」で、TwoWay 経路・callback 経路の両方で日本語変換候補が維持されることをエミュレータ/実機で目視確認し、結果 (確認した経路・OS バージョン) を完了報告に 1 行残す。

## 確認した観点 (指摘に至らなかったもの)

- **ガードが効きすぎないか**: 3 ガードとも「View の現在状態 vs 目標値」の比較であり、Cell 側のキャッシュを持たない。ViewHolder が `reset()` を経ずに別 Cell へ再 bind される経路でも、View 状態が正になるため必ず収束する。
- **`reset()` との干渉**: `reset()` は `KsSettingsListAdapter.kt:159-167` の `onViewRecycled` からのみ呼ばれ、`submitContentUpdate` → `notifyItemChanged` のキーストローク経路には乗らない。よって毎打鍵の再 bind でガード前提が壊されることはない。
- **`isPassword` の扱い**: `PasswordTransformationMethod` の適用は `TextView.setInputType` の内部で `targetInputType` のみから決まるため、同値スキップで transformation が実態とずれることはない。`keyboardType` に PASSWORD を直接含める経路と `isPassword = true` の経路が同じ合成値になるケースでも整合する。
- **`inputType` 比較の前提**: `setInputType` は与えた値をそのまま保持する (single-line 状態に合わせた MULTI_LINE フラグの再調整を含めても、`create()` の `isSingleLine = true` 前提で round-trip する) ため、`editText.inputType` との等値比較が成立する。既存の `keyboardType` / `isPassword` 反映テストが通っていることでも裏づけられている。
- **他の未ガード代入**: `gravity` / `setTextColor` / `highlightColor` は IME の再接続を伴わない。`isEnabled` / `isFocusable` は同値時に View 側で早期 return するため、追加ガードは不要。
- **新規コメントの規約適合**: 追加された 4 箇所のコメントに変更提案パス参照・spec 裸参照・`MUST` 等の混入はない。
- **テスト実行**: `./gradlew :ks-settingsview-{core,ui,compose}:testDebugUnitTest` を実行し、XML レポートで件数を確認 (core 74 / ui 366 / compose 76、failures 0 / errors 0 / skipped 0)。`InputCellsTest` は 41 件全 pass。

## アクションプラン

1. `EntryCellViewHolder.kt:32` と `InputCellsTest.kt:35` の `仕様: openspec/...` 行を削除する (Minor 1)
2. placeholder の同値再 bind テストを「setHint が呼ばれない」検証に強化する、または同等のテストを 1 件追加する (Minor 2)
3. サンプルアプリで IME 変換の維持を目視確認し、結果を完了報告に残す (Suggestion。実施可否はオーナー判断)
4. filters ガードの不変条件強化・テストヘルパーの共通化は本 change では見送ってよい (Suggestion)

---

## 再確認 (2026-08-01)

**判定**: APPROVED

Minor 2 件の修正を確認した。Suggestion 3 件は今回未対応 (任意のまま) で、いずれも実害がないため判定には影響しない。

### Minor 1 (openspec 仕様参照の削除) — 解消

- `EntryCellViewHolder.kt`: クラス KDoc から `仕様: openspec/...` の 2 行と直前の空行が削除され、末尾は箇条書きで自然に閉じている。comment-policy の「1. 定型句型」の指示どおりで、残った説明文の可読性も損なわれていない。
- `InputCellsTest.kt`: 同様に 1 行と直前の空行を削除。
- 両ファイルに対する禁止パターンの grep (`openspec/` / `Phase [0-9]` / `Decision [0-9]` / `MUST` / `SHOULD` / `spec.md` / `tasks.md`) はヒット 0。

### Minor 2 (placeholder テストの実効化) — 解消

`InputCellsTest.kt` のテストは `EntryCell 同値 Cell の再 bind では hint が差し替えられない` に改名され、初回 bind 後の `editText.hint` を捕捉 → `buildString` で作った同値・別インスタンスの文字列で再 bind → `assertSame` で同一性を検証する形になった。`filters` テストと同じ「参照が変わらない = setter 未呼び出し」の検証軸に揃っている。

`setHint` が final でオーバーライドできないため代替手段を採った、という判断も妥当。**この検出手段が実際に効くかを実測で確認した**: 一時プローブテスト (`EditText` 単体) を Robolectric 上で走らせ、以下 2 点を確認した (確認後、当該一時ファイルは削除済み。作業ツリーには残っていない)。

1. `getHint()` は代入されたインスタンスをそのまま返す (コピーを返さない) — ガードありで `assertSame` が通る理由が「毎回同じ値だから」ではなく「同じインスタンスだから」であることの裏づけ
2. 同値だが別インスタンスの文字列を代入すると、保持インスタンスは新しい方に**入れ替わる** — ガード撤去時に `assertSame` が確実に失敗する

すなわち改修後のテストは、初回指摘の「ガードを外しても pass する」問題を解消している。`buildString` による生成は定数畳み込みされないため、別インスタンスであることも保証される。

### テスト実行 (再確認時)

`./gradlew :ks-settingsview-{core,ui,compose}:testDebugUnitTest` を再実行。XML レポートで件数を確認:

- ks-settingsview-core: tests=74 failures=0 errors=0 skipped=0
- ks-settingsview-ui: tests=366 failures=0 errors=0 skipped=0
- ks-settingsview-compose: tests=76 failures=0 errors=0 skipped=0

初回確認時と同数 (テストは改名のみで増減なし)、全 516 件 pass。

### 残る任意事項

Suggestion 3 件 (filters ガードの不変条件強化 / テストヘルパーと `create()` の共通化 / IME 実挙動の目視確認記録) は未対応。前 2 件は本 change で見送って問題ない。3 件目の実機・エミュレータでの確認は、本 change の効果そのものが自動テストでは検証できない領域に属するため、アーカイブ前にオーナー判断で実施を検討することを推奨する (レビュー判定を保留する理由にはしない)。
