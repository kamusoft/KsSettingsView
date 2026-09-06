# 検証結果: cell-style-dark-appearance-parity (002 回目)

**日付**: 2026-09-06
**判定**: VALID

修正サイクル 1 の差分更新。[verify-001.md](verify-001.md) の対応表を基準とし、本サイクルで対応が動いた Scenario だけを更新する。ここに挙げない Requirement / Scenario は verify-001 の判定 (✅ 一致 / ⚠️ deviation 記録済み) がそのまま有効 — 該当する実装ファイルが本サイクルで 1 件も変更されていないことを作業ツリーの更新時刻で確認した (`ios/` の最終更新は review-001 より前、`maui/` のソースも同様)。

❌ は 0 件。新規の未記録乖離なし。足場 (specs / proposal) は本サイクルで動いていない。

## テスト実行 (再実行して確認)

| platform | コマンド | 結果 |
|---|---|---|
| Android | `cd android && ./gradlew test --rerun-tasks` | 2854 件 / 失敗 0 / エラー 0 (`*/build/test-results/test{Debug,Release}UnitTest/TEST-*.xml` の `tests` / `failures` / `errors` 合計)。verify-001 の 2850 件に対し、`CellSpecificColorTest` の新規 2 件 × 2 variant = 4 件の増 |
| MAUI | `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` | 520 件 / 失敗 0 (verify-001 と同値) |
| iOS | 未実行 (本サイクルで `ios/` の変更なし) | verify-001 の 1027 件 / 失敗 0 が有効 |

---

## 更新した対応行

### settings-view-android-ui / Requirement: Cell 固有色の未指定表現

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 未指定の Cell 固有色は CellStyle と Theme へ継承する | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyle.kt:518` (`toArgbOrElse`) と 8 消費点 (`SwitchCellViewHolder.kt:154` / `CheckboxCellViewHolder.kt:40` / `SimpleCheckCellViewHolder.kt:32` / `RadioCellViewHolder.kt:32` / `EntryCellViewHolder.kt:237` / `DatePickerCellViewHolder.kt:131,224` / `PickerSelectionSheet.kt:134`) | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/CellSpecificColorTest.kt` (既存 3 件に加え、消費点を直接観測する `CheckboxCell の accent は buttonTint として出る` / `EntryCell の accent は入力欄のハイライト色として出る` の 2 件を追加) | ✅ 一致 (verify-001 から補強) |

verify-001 では消費点 8 箇所のうち Checkbox の `buttonTintList` と Entry の `highlightColor` を観測するテストが無かった。本サイクルの追加で 8 消費点すべてが実描画のテストに載った。検出力はミューテーション probe で実測済み (この 2 消費点を素の `toArgb()` へ戻すと `CellSpecificColorTest` の当該 2 件だけが落ちる。詳細は [review-002.md](review-002.md))。

### settings-view-android-ui / Requirement: 明示した Cell 色と外観

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| DSL で外観に応じて選んだ CellStyle 色は再 composition で行に届く | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposable.kt:130` (内容更新は `store.replaceCells` で配る。実装変更なし) | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/compose/DSLCellStyleUpdateTest.kt` (待機を条件ベース化。ViewHolder 同一性の assert を追加) | ✅ 一致 (注記あり — 更新) |
| Store 経路で style だけ差し替えた Cell は行に反映される | (実装変更なし) | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/StoreCellStyleReplaceTest.kt` (ViewHolder 同一性の assert を追加) | ✅ 一致 (注記あり — 更新) |

注記 (❌ ではないが THEN の一部が依然として観測されていない):

- 「DSL で外観に応じて選んだ…」の THEN 前半「差分は内容更新 (`replaceCell`) として発行され」は、本サイクル後も観測されていない。テストが観測するのは行の実描画色 (THEN 後半) のみ。なお Android の DSL は内容変化で `SettingsRootDiff.ReplaceCell` を発行しない設計 (`compose/DSLDiffCalculator.kt:18-20`) で、内容更新は `compose/KsSettingsViewComposable.kt:130` の `store.replaceCells` が担うため、spec が名指しする機構名自体が Android の実態と食い違う (verify-001 と同じ)
- 「Store 経路で style だけ差し替えた…」の THEN 後半「Section / Cell の identity は維持される」について、本サイクルで追加された `assertSame` による ViewHolder 同一性の観測は、**行が作り直される退行を検出しない**ことが実測で判明した。`ui/KsSettingsListAdapter.kt:326` の `areItemsTheSame` を全等価比較へ差し替えて style 更新を remove + insert へ退行させても、両テストは緑のまま通る (`RecycledViewPool` からの同一インスタンス再利用と、`ui/KsSettingsView.kt:89` の `supportsChangeAnimations = false` による)。したがって「行 (ViewHolder) が作り直されないこと」は verify-001 時点と同じく未観測のままである

いずれも review-002.md の Major 1 として指摘した (adapter の通知列を観測する形へ移す推奨。その形の検出力は probe で確認済み)。実装そのものは両 Scenario の THEN を満たしており、不足しているのはテストの観測点であるため ❌ とはしない。

### maui-bridge / Requirement: Cell 単位の色プロパティの変更は表示中の Cell に届く

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 外観変更を受けて再代入した Cell の色が native まで届く | `maui/tests/KsSettingsView.MauiHost/SettingsPage.xaml:46-49` / `SettingsPage.xaml.cs:36-37,53-55,105-133` (本サイクルで変更なし) | 自動テストなし (両 OS の実物確認)。証跡 `evidence/maui-host-buttoncell-retheme-{ios,android}-{light,dark}.png`。規約は `kasane/handbook/maui/integration-host-verification.md:116` (期待される表示) に加え `:125` (完了条件) へ追記済み | ✅ 一致 (verify-001 から補強) |

verify-001 時点では期待される表示への追記のみで、完了条件は従来の内容更新・再訪問だけだった。本サイクルで完了条件へ「MauiHost の設定画面を表示したまま OS の外観を切り替えると、『外観追随ボタン』の title 色が両 OS で切り替わる」が入り、handbook の timestamp も更新されている。

---

## 追加検査 (差分)

### tasks.md

全 22 タスクがチェック済みのまま。**虚偽のチェックは無い**。ただし 8.1 の Android 分の記録が「本体 1252 件 × 2 + bridge 173 件 × 2 = 計 2850 件」のままで、修正サイクル後の実測 2854 件に追随していない (`cross/test-execution.md` は件数の確認までを検証とする)。実施していないタスクをチェックしたものではないため ❌ ではないが、記録と最終状態の食い違いとして review-002.md の Minor 2 に挙げた。

蒸留への申し送りには review-001 の Minor 3 (ADR-0030 Decision 3 の MAUI 節の限定) と Suggestion (`replaceCell` → `replaceCells`) が追記済み (`tasks.md:79-80`)。

### 逆流検査

本サイクルで書き換わった足場は無い。作業ツリーの更新時刻から、review-001 以降に変更されたのは以下のみ:

- テスト 3 本 (`compose/DSLCellStyleUpdateTest.kt` / `ui/StoreCellStyleReplaceTest.kt` / `ui/CellSpecificColorTest.kt`)
- 実装 2 本 (`ui/CheckboxCellViewHolder.kt` / `ui/EntryCellViewHolder.kt`)
- 規範 1 本 (`kasane/handbook/maui/integration-host-verification.md`)
- 記録 2 本 (`deviation.md` / `tasks.md`)

`specs/` / `proposal.md` / `ui/brief.md` は動いていない。

### 実装 2 本の再構成の妥当性

`CheckboxCellViewHolder.kt` / `EntryCellViewHolder.kt` の `git diff HEAD` は、兄弟 4 ViewHolder と同一の変換パターン (`cell.accentColor?.toArgb() ?: effective.accentColor` → `cell.accentColor.toArgbOrElse(effective.accentColor)`、Checkbox はあわせて未使用となった `toArgb` import の削除) のみで、欠落も余分も無い。`EntryCellViewHolder` が `toArgb` import を残しているのは `:198` の placeholder 経路が使うためで、deviation.md の付随修正「4 ViewHolder の未使用 `toArgb` import 削除」(Switch / Radio / SimpleCheck / Checkbox) とも一致する。

### 未記録乖離

無し。deviation.md には本サイクルの手段差 (Checkbox / Entry の消費点テストの置き場を `CellSpecificColorTest` にしたこと、caret tint が読み戻せないため `highlightColor` を観測点にしたこと) が「4.1 (レビュー修正)」として追記済み。

### 付随修正

本サイクルで新たに加わった付随修正は無い。verify-001 で確認した 5 件に変化なし。
