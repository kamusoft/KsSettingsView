# 検証結果: cell-style-dark-appearance-parity (003 回目)

**日付**: 2026-09-06
**判定**: VALID

修正サイクル 2 の差分更新。[verify-001.md](verify-001.md) の対応表を基準とし、[verify-002.md](verify-002.md) の更新を引き継いだうえで、本サイクルで対応が動いた Scenario だけを更新する。ここに挙げない Requirement / Scenario は先行 verify の判定 (✅ 一致 / ⚠️ deviation 記録済み) がそのまま有効 — 該当する実装ファイルが本サイクルで 1 件も変更されていないことを作業ツリーの更新時刻で確認した (`ios/` / `maui/` および `android/**/src/main/` は review-002 より前の時刻のまま)。

❌ は 0 件。新規の未記録乖離なし。足場 (specs / proposal / ui) は本サイクルで動いていない。

## テスト実行 (再実行して確認)

| platform | コマンド | 結果 |
|---|---|---|
| Android | `cd android && ./gradlew test --rerun-tasks` | 2854 件 / 失敗 0 / エラー 0 (`*/build/test-results/test{Debug,Release}UnitTest/TEST-*.xml` の `tests` / `failures` / `errors` 属性の合計、結果 XML 222 本)。verify-002 と同値 (本サイクルはテストの観測点を差し替えただけで件数の増減は無い) |
| iOS | 未実行 (本サイクルで `ios/` の変更なし) | verify-001 の 1027 件 / 失敗 0 が有効 |
| MAUI | 未実行 (本サイクルで `maui/` の変更なし) | verify-002 の 520 件 / 失敗 0 が有効 |

---

## 更新した対応行

### settings-view-android-ui / Requirement: 明示した Cell 色と外観

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| DSL で外観に応じて選んだ CellStyle 色は再 composition で行に届く | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposable.kt:130` (内容更新は `store.replaceCells` で配る。実装変更なし) | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/compose/DSLCellStyleUpdateTest.kt:78-86` (通知列の観測へ差し替え) | ✅ 一致 (verify-002 の注記は解消) |
| Store 経路で style だけ差し替えた Cell は行に反映される | (実装変更なし) | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/StoreCellStyleReplaceTest.kt:95-105` (通知列の観測へ差し替え) | ✅ 一致 (verify-002 の注記は解消) |

観測の器は `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsViewTestSupport.kt:181-213` の `ChangeRecordingObserver` を拡張したもの (通知種別を発行順に記録し、内容更新以外を `structuralNotifications` として取り出す)。

**verify-001 / verify-002 が残していた 2 つの注記は、本サイクルで解消した**:

- 「DSL で外観に応じて選んだ…」の THEN 前半「差分は内容更新 (`replaceCell`) として発行され」は、verify-001 / verify-002 の時点では未観測だった。本サイクルで `observer.payloads` に `KsSettingsView.PAYLOAD_CONTENT` が含まれることの assert が入り、**THEN 前半が実測で観測された**。なお Android の DSL は `SettingsRootDiff.ReplaceCell` を発行せず内容更新は `store.replaceCells` が担うため、spec が名指しする機構名と実態の食い違いは残る (verify-001 と同じ。`tasks.md` の蒸留への申し送りに記録済み)
- 「Store 経路で style だけ差し替えた…」の THEN 後半「Section / Cell の identity は維持される」について、verify-002 は `assertSame` による ViewHolder 同一性の観測が退行を検出しないと記録した。本サイクルで観測点が Adapter の通知列へ移り、**行が作り直される退行を実際に検出する**ことをミューテーション probe で確認した (`ui/KsSettingsListAdapter.kt:326` の `areItemsTheSame` を全等価比較へ退行させると、両テストが `[removed, inserted]` を検出して落ちる)。加えて payload 落ちの退行でも両テストが落ちる。実測は [review-003.md](review-003.md) に記録

---

## 追加検査 (差分)

### tasks.md

全 22 タスクがチェック済みのまま。**虚偽のチェックは無い**。verify-002 が挙げた 8.1 の件数の食い違いは解消しており、Android 分は「本体 1254 件 × 2 + bridge 173 件 × 2 = 計 2854 件 / 失敗 0 件」と、修正サイクル 2 後の再実行でも同値であることが併記されている。本検証の実測値と一致する。

### 逆流検査

本サイクルで書き換わった足場は無い。作業ツリーの更新時刻から、review-002 (2026-09-06 19:54) 以降に変更されたのは以下のみ:

- テスト 3 本 (`ui/KsSettingsViewTestSupport.kt` / `compose/DSLCellStyleUpdateTest.kt` / `ui/StoreCellStyleReplaceTest.kt`)
- 記録 2 本 (`deviation.md` / `tasks.md`)

`specs/` / `proposal.md` / `ui/brief.md` はいずれも 2026-09-06 18:10 以前のままで動いていない。実装コード (`src/main/`) も本サイクルでは変更されていない。

### 未記録乖離

無し。本サイクルの手段変更 (観測点を ViewHolder の同一性から Adapter の通知列へ移したこと) は `deviation.md` の「tasks の手段からの差」に「3.1 / 3.2 (レビュー修正)」として、理由と probe の裏付けまで含めて記録済み。記載内容は本検証で確認した実測と一致する。

### 付随修正

本サイクルで新たに加わった付随修正は無い。verify-001 で確認した 5 件、verify-002 の確認内容に変化なし。
