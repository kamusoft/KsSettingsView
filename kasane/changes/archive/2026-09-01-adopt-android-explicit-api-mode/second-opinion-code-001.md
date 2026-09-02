# セカンドオピニオン: adopt-android-explicit-api-mode (code-001)

**相方**: claude / **label**: so-code-adopt-android-explicit-api-mode / **日付**: 2026-09-01 / **対象**: `android/kssettingsview/**` と `kasane/changes/adopt-android-explicit-api-mode/` の実装差分

---

規律・規約・アーティファクト・差分・証跡をすべて照合しました。以下がレビュー結果です（制約に従いファイル書き込み・ビルド実行はしていません）。

---

# レビュー結果: adopt-android-explicit-api-mode (独立レビュー / 静的)

**日付**: 2026-09-01
**判定**: **CHANGES_REQUESTED**（Critical 0 / Major 0 / Minor 4 / Suggestion 3）

## サマリー

デルタスペックの 4 Requirement・8 Scenario はいずれも実装と証跡で追跡でき、Strict の対象範囲・指定 3 API だけの internal 化・公開 ABI の維持は独立に再現確認できました。特に「Strict が実際に効いていること」の陽性対照（明示前に debug/release とも 204 件の診断で BUILD FAILED）と、release AAR の全 class `javap -public` 差分（270 class / 2,849 行が前後一致、3 API の name mangling のみ）は、この種の変更で最も抜けやすい点を正面から押さえており質が高い。

一方で、公開面の縮小に伴う**後始末が完了していません**。`SettingsRootStore.preview` はリポジトリ全体で参照ゼロの死蔵コードになり、それを保持するためだけの `public companion object` が公開型として ABI に残っています。また `KsCellRegistry` の公開 doc コメントの「可視性」節が、本 change がまさに internal 化した 2 メンバーに言及しないまま取り残されています。いずれも実装側で解消可能で、変更の目的（公開境界を宣言上明確にする）に直結するため、アーカイブ前の修正を求めます。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md`（always）
- `kasane/handbook/cross/public-identifiers.md`（`**/build.gradle.kts` を触るため。namespace / artifactId / 配布座標に変更はなく適合）
- `kasane/handbook/cross/test-execution.md`（テスト結果の報告。実行件数併記あり: 本体 1,183×2 / Bridge 167×2 = 2,700 件、failures 0）
- `kasane/decisions/android/0022-explicit-api-strict-for-public-library.md`（Decision と実装が一致）
- `kasane/lessons/code-review.md` L-001（ミューテーション実測）— 本レビューは静的指定のため未適用。ただし本 change の陽性対照証跡が実質的に同型の検出力証明になっている

## 仕様対応の確認結果（すべて充足）

| Requirement / Scenario | 実装 | 証跡 |
|---|---|---|
| 明示 API 境界 / 明示不足を拒否 | `build.gradle.kts:84` `explicitApi()` | `evidence/explicit-api-positive-control.txt`（debug/release とも 204 件・BUILD FAILED） |
| 同 / 両 variant でコンパイル成功 | 201 宣言への `public` 明示 | compileDebug/ReleaseKotlin 成功 |
| 同 / 対象外へ波及しない | Bridge の `build.gradle.kts` 未変更、KGP は test compilation を除外 | test 2,700 件成功・Bridge 両 variant 成功 |
| 公開 API 差分の限定 | — | `evidence/release-aar-abi-diff.txt`（270 class / 2,849 行一致） |
| Registry 公開面（正 / 負） | `viewTypeOf` / `isRegistered` → `internal` | `evidence/external-visibility-probe.txt` |
| Store 生成境界（正 / 負） | `preview` → `internal`、テストを通常コンストラクタへ | 同上 + `SettingsRootStoreTest.kt:409-418` |

独立確認したこと:
- main source 全体で `internal` キーワードが増えたのは `KsCellRegistry.kt`(+2) と `SettingsRootStore.kt`(+1) の **3 件のみ**。過剰降格なし
- 追加された可視性修飾は 201（public）+ 3（internal）で、陽性対照の診断 204 件と整合
- `tasks.md` の差分は `- [ ]` → `- [x]` のみ。足場アーティファクトの書き換えなし

---

## 指摘事項

### 🟡 Minor 1 — `SettingsRootStore.preview` が参照ゼロの死蔵コードになり、空の `public companion object` が公開型として残る

**該当箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt:339-350`

**問題点**: 本 change 前の `preview` の唯一の呼び出し元は `SettingsRootStoreTest` の 1 件でしたが、task 3.2 でその呼び出しが通常コンストラクタへ書き換えられた結果、`preview` は main / test / samples / bridge / maui / skills のどこからも参照されなくなりました（`grep -rn 'SettingsRootStore.preview\|\.preview('` で 0 件）。KDoc は「Preview / Test 用ファクトリ」と称しますが、その Preview も Test も存在しません。

さらに副作用として、`companion object` はこの死蔵メンバー 1 個のみを持ちながら `public` として残り、`evidence/release-aar-abi-diff.txt` が示すとおり `public final class jp.kamusoft.kssettingsview.ui.SettingsRootStore$Companion` は依然 Java/Kotlin 利用者から見える公開型です。「意図しない公開面を絞る」という本 change の目的に照らすと、公開メンバーがゼロの公開ネスト型だけが残る形は目的の取りこぼしです。

**実害シナリオ**: 初回リリース版の AAR に、誰も呼ばない internal factory と、公開メンバーを 1 つも持たない公開型 `SettingsRootStore.Companion` が同梱される。後任が `preview` を「Preview 用に用意された経路」と誤認して復活・public 化を検討する、あるいは Explicit API 導入後も公開面に無意味な型が残っていることを理由に再度の棚卸し change が必要になる。

**推奨修正**: 次のいずれか。
1. `preview` を削除し、`companion object` ごと除去する（デルタスペックの「Preview / Test 用 factory を外部利用者へ公開しない」は削除でも充足。ただし `evidence/external-visibility-probe.txt` の負 probe が対象を失うため、証跡の但し書き更新が必要）
2. `preview` を残す方針なら、それを実際に使うテストを 1 件置き、KDoc の「Preview / Test 用」という説明を実在の利用に裏付けさせる

なお `companion object` を `internal` にする案は、Requirement「公開 API 差分の限定」（3 宣言以外の公開宣言を維持する）に抵触し得るため、1 を採る場合も含めて実装単独で決めず、判断をオーナーへ上げてください。

---

### 🟡 Minor 2 — `KsCellRegistry` の公開 doc コメント「# 可視性」節が、本 change の internal 化を反映していない

**該当箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistry.kt:22-27`

**問題点**: この節は「何が public で何が internal か」を明示的に説明する箇所で、現状 `createViewHolder` のみを internal として列挙しています。本 change で `viewTypeOf` と `isRegistered` も internal になったのに追記されておらず、節の記述が実態を網羅しなくなりました。**この staleness は本 diff が作り出したもの**です。

加えて `comment-policy.md`「公開メンバーの doc コメント」は、公開 doc に機能と契約だけを書き、内部詳細（どのメンバーが internal か、同モジュール内の `KsSettingsListAdapter` から呼ばれる、等）は非公開側コメントに置くよう定めています。現状の節はこの点でも規約から外れています。

**実害シナリオ**: 利用者が生成ドキュメント / IDE の KDoc から「internal なのは `createViewHolder` だけ」と読み、`viewTypeOf` を公開 API と誤認して呼び出そうとしてコンパイルエラーに遭う。エラーの理由がドキュメントと矛盾するため原因追跡に時間を要する。

**推奨修正**: 「# 可視性」節を、利用者向けの契約（独自 Cell 型を `register` に渡して描画対象に加えられること）だけの記述に整理し、どのメンバーが internal かという内部詳細の列挙は本文から外すか、非公開の実装側コメントへ移す。列挙を残す方針なら `viewTypeOf` / `isRegistered` を必ず加える。

---

### 🟡 Minor 3 — `isRegistered` の KDoc「（テスト・診断用）」が実態と食い違う

**該当箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistry.kt:203-206`

**問題点**: `isRegistered` は production コードの `KsSettingsView.kt:242 / 248 / 252` で、基本 Cell・入力 Cell・CustomCell の自動登録をスキップするか判定する本番経路として使われています。「テスト・診断用」という説明は事実に反します。しかもこの KDoc の記述は、`exploration.md`「Registry の公開面」で降格判断の根拠として明示的に引用されたものです（「`isRegistered` は KDoc がテスト・診断用と明記するため、公開契約から外す」）。

降格の結論自体は妥当です（本番利用はすべて同一 module 内なので internal で成立します）が、根拠に使った記述が不正確なまま残るのは、次に同じ判断をする人を誤らせます。

**実害シナリオ**: 将来 `KsSettingsView` の自動登録処理をリファクタする担当者が、KDoc を信じて `isRegistered` を「テスト専用ヘルパ」と判断し削除・改名し、自動登録のオプトアウト挙動（利用者が事前に別 factory を登録した場合に上書きしない契約）を壊す。

**推奨修正**: KDoc を実態に合わせる。例:「指定 Cell 型が登録済みかを返す。自動登録処理が既登録をスキップする判定と、テスト・診断に使う。」

---

### 🟡 Minor 4 — primary constructor property への `public` 付与が不統一で、どちらが規約か読み取れない

**該当箇所**: `core/SettingsRootDiff.kt:18-85`、`core/AccessoryTarget.kt:23-32`、`core/KsAnyView.kt:29,38`、`ui/CustomCell.kt:52-57`、`ui/PickerItem.kt:13` ⇔ `ui/Theme.kt:83-117`、`core/Section.kt:44-`、`ui/CellStyle.kt:36-49`、`ui/EntryCell.kt:44-58`

**問題点**: 同じ「public な型の primary constructor property」が、本 diff の中で 2 通りに書かれています。

- 付与あり: `public data class SectionHeader(public val sectionId: String)`、`public class PickerItem(public val text: String, ...)`、`public class CustomCell<Content : Any>(public val content: Content, ...)`
- 付与なし: `public data class Theme(val separatorColor: Color = ..., ...)`、`public data class Section(val id: String, ...)`、`public data class CellStyle(val titleColor: Color? = null, ...)`

両方が Explicit API Strict で診断ゼロのままコンパイルできている以上、少なくとも一方は冗長です。本 change の成果物は「公開面を宣言として明示したソース」そのものなので、その表記が 2 通りあると、規約が何なのかがコードから読み取れません。

**実害シナリオ**: 新しい公開 data class を足す担当者が `Theme` を手本にすれば `public` を書かず、`AccessoryTarget` を手本にすれば書く。以後 diff に一貫性のない修飾が混ざり続け、「Explicit API のために必要な修飾」と「単なる冗長修飾」をレビューで区別できなくなる（本 change が獲得した「公開面差分のレビュー可能性」を目減りさせる）。

**推奨修正**: どちらかに統一する。コンパイラが要求しない箇所には付けない（ktlint の `no-redundant-visibility-modifier` 相当）方針が差分を最小化でき、統一後は `kasane/handbook/android/` に「primary constructor property には明示不要」の 1 行規約として残すと、次に公開型を足すときに迷いが出ません。ABI・実行時挙動には影響しないため、優先度は本指摘群の中で最も低くて構いません。

---

### 🔵 Suggestion 5 — ABI 差分証跡の限界（`javap -public` は class 単位の internal 化を検出できない）を明記する

**該当箇所**: `kasane/changes/adopt-android-explicit-api-mode/evidence/release-aar-abi-diff.txt:13`

Kotlin の `internal` は関数・プロパティ accessor には name mangling が入るため `javap -public` の差分に現れますが、**`internal class` / `internal object` には mangling が入らず、JVM 上は public class のまま出力されます**。つまりこの手法は「型ごと誤って internal 化した」ケースを構造的に検出できません。証跡の「その他の公開宣言の増減: 0 件」という断定は、この盲点の分だけ実際の保証範囲より強い表現になっています。

本 change では、私が `internal` キーワード数を変更前後で全ファイル比較した結果、増加は関数 3 件のみで型の降格はゼロと確認できたため実害はありません。証跡に「本手法は member 単位の可視性変化を捉える。型単位の降格は diff 上 `internal` 追加が 3 件（すべて関数）であることで別途担保した」と一文足すと、後から証跡だけを読む人が保証範囲を誤読しません。

---

### 🔵 Suggestion 6 — 公開メンバー `Theme.DEFAULT_BACKGROUND_COLOR` の doc コメントに change 識別子と履歴記述が残る

**該当箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt:147-148`

「旧名 `DEFAULT_VIEW_BACKGROUND_COLOR` から `port-theme-and-cellstyle-missing-fields` change で本名にリネームされた」は、`comment-policy.md` の**禁止する参照**（変更識別子の裸参照）と**禁止する記述類型**（進捗ログ・履歴記述）の双方に該当し、さらに公開 doc コメントに内部用語を持ち込んでいます。既存記述で本 diff が新たに壊したものではありませんが、直下の `public val DEFAULT_BACKGROUND_COLOR` は本 change が公開宣言として明示した行であり、公開面の棚卸しをしている今が直す機会です（書き換え類型 3「履歴記述型」＝現在の仕様の説明に書き換えるか削除）。「SettingsView 全体の既定背景色（白系）。互換シムは提供しない。」程度に整理すれば足ります。

---

### 🔵 Suggestion 7 — `tasks.md 1.4` の keep / demote 全走査一覧が成果物から追えない

**該当箇所**: `kasane/changes/adopt-android-explicit-api-mode/tasks.md:8`

1.4 は「明示した宣言を全走査し、keep / demote を判定した一覧を**実装報告へ残す**」を完了条件としていますが、実装報告は change 配下に残らないため、アーカイブ後は判定過程が追跡不能になります。降格 3 件の妥当性自体は exploration と ABI 差分で追えるので判定には影響しませんが、同種の棚卸しを他 platform（`ios-effectivestyle-visibility` 等）で行う際の参照資産としては失われます。`evidence/` に keep / demote 一覧を 1 ファイル残すか、蒸留時に concepts へ要約を移すことを検討してください。

---

## アクションプラン（優先度順）

1. **Minor 1**: `preview` の死蔵を解消する。削除（＋`companion object` の扱いはオーナー判断を仰ぐ）か、実利用テストの追加か、方針を決めてから着手する
2. **Minor 2 / Minor 3**: `KsCellRegistry` の class KDoc「# 可視性」節と `isRegistered` の KDoc を実態に合わせる。公開 doc からは内部詳細を落とす
3. **Suggestion 5**: ABI 差分証跡に手法の保証範囲を 1 文追記する（証跡がアーカイブされる前に）
4. **Minor 4**: primary constructor property の `public` 表記を統一し、方針を handbook へ 1 行残す
5. **Suggestion 6 / 7**: `Theme.kt` の公開 doc から履歴記述を除去、keep / demote 一覧の保存先を決める

Critical / Major は 0 件で、仕様充足・堅牢性・テスト・ABI 維持の観点では問題を検出していません。判定を CHANGES_REQUESTED としたのは、Minor 1 と Minor 2 が**本 change 自身が作り出した後始末漏れ**であり、いずれもアーカイブ後には気づきにくくなる（死蔵コードは警告が出ず、公開 doc の齟齬は利用者側で初めて表面化する）ためです。上記 1・2 を解消すれば APPROVED 相当と判断します。

## 突き合わせ結果

- 確定: 0 件。ホスト側 `review-001.md` は指摘なしで APPROVED。
- 採用: 4 件。
  - Minor 2: `KsCellRegistry` の公開 KDoc から内部メンバー列挙を外し、利用者向け契約だけに整理する。
  - Minor 3: `isRegistered` の KDoc を、本番の自動登録判定にも使う実態へ合わせる。
  - Suggestion 5: ABI 証跡へ `javap -public` の保証範囲と、型の誤降格を別途確認した根拠を追記する。
  - Suggestion 6: `Theme.DEFAULT_BACKGROUND_COLOR` の公開 KDoc から変更識別子と履歴記述を除去する。
- 降格: 3 件。
  - Minor 1: `preview` の削除と `companion object` の除去は、合意済み仕様の「3 宣言を internal 化する」を越えて公開型まで削除し、公開 ABI 差分を指定3宣言に限定する Requirement と衝突する。参照ゼロの internal helper は外部契約の欠陥ではないため、本 change では維持する。
  - Minor 4: primary constructor property の修飾差はコンパイラが許容する表記上の差で、ABI・挙動・既存規約への違反が示されていない。新規 handbook 規約の追加も本 change の範囲外なので修正サイクルを回さない。
  - Suggestion 7: task 1.4 は keep / demote 一覧を実装報告へ残す要件であり、change 配下の恒久成果物化は要求していない。降格3件の判断は exploration / proposal / spec / ABI 証跡から追跡できるため追加しない。
- 未解決: 0 件。
