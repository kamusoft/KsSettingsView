# 一致検証結果: fix-picker-dialog-recreation (002 回目)

**日付**: 2026-08-03
**判定**: VALID

対象デルタスペック: `kasane/changes/fix-picker-dialog-recreation/specs/settings-view-android-ui/spec.md`
`deviation.md`: なし (合意済み乖離なし)

テスト実行: `cd android && ANDROID_HOME=$HOME/Library/Developer/Xamarin/android-sdk-macosx ./gradlew testDebugUnitTest --rerun-tasks`
→ BUILD SUCCESSFUL / **tests 866, failures 0, errors 0, skipped 0**
(内訳: `:ks-settingsview-ui` 712 / `:ks-settingsview-compose` 80 / `:ks-settingsview-core` 74。
本変更の新規は `PickerDialogRecreationTest` 28 件・`PickerDialogTagTest` 10 件・`PickerRestoreRegistryTest` 4 件)

**001 回目からの差分**: 1 周目レビュー指摘への修正 4 件が入り、テストは 861 → 866 (+5)。
デルタスペック自体は無変更のため、Requirement / Scenario の構成も変わっていない。行番号と、
修正で新たに追加された固定点を反映して対応表を取り直した。

---

## Requirement 1: 再生成後のピッカーダイアログの完全復元 (ADDED)

### 本文条項の対応

| 条項 | 実装 | テスト | 状態 |
|---|---|---|---|
| 「attach 済み」∧「root 反映済み」で復元走査を駆動 | `KsSettingsView.kt:247-249` (`onAttachedToWindow`) / `375-376` (`setRootDirect`) / `655-660` (`scheduleRestoreScanIfReady`) | `PickerDialogRecreationTest`「attach 前に root が反映される順序でも復元される」/「attach 後に root が反映される順序でも復元される」 | ✅ 一致 |
| 適格条件: 同一 `id`・同型・現 root にちょうど1つ | `KsSettingsView.kt:701-709` (`findRestoreTarget`、`take(2)` + `singleOrNull`) / `712-715` (`matchesPickerKind`) | 「該当 id が現 root に無ければ閉じられ誤発火しない」/「uiStyle が変更されていたら閉じられる」/「同一 id の候補が複数なら閉じられる」 | ✅ 一致 |
| `isEnabled` / `onValueChanged == null` / `minDate`・`maxDate` 変化は適格性に影響しない | `findRestoreTarget` は `id` と型のみを見る (構成値を参照しない) | 「`onValueChanged` が null で `isEnabled` が false でも適格として復元される」 | ✅ 一致 (`minDate` / `maxDate` の変化は実装上参照経路が存在せず構造的に保証) |
| 復元ダイアログは表示時の構成と選択状態を維持し、Cell の構成値を流し込まない | `KsSettingsView.kt:725-775` (`restorePicker` はリスナー・配色・「今日」のみ付け直し、`setSelection` 等を呼ばない) | 「復元したダイアログは Cell の構成値変化を取り込まない」 | ✅ 一致 |
| 対応付けは `id` で行い、区切り文字を含む id でも成立 | `PickerDialogTag.kt:42-50` (encode) / `68-78` (decode。可変長 `cellId` を最終フィールドに置く固定書式) | `PickerDialogTagTest` 全 10 件 (ドット・`|`・空文字・Unicode・`.r1` 風末尾・多世代・他所 tag の除外) | ✅ 一致 |
| 世代を経たダイアログも復元対象 | `MaterialDatePickerPresenter.kt:67-71` (tag へ generation 埋め込み) / 生きたダイアログの作り直し `MaterialDatePickerPresenter.kt:80-88` / 復元済みダイアログの作り直し `KsSettingsView.kt:759-767` (いずれも `generation + 1`) | 「今日ボタンの作り直しを経たダイアログも復元される」(世代 1 を assert した上で復元と確定発火を検証) | ✅ 一致 |

### Scenario 対応

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| TimePicker の値確定が回復する | `KsSettingsView.kt:732-740` (`restorePicker` TimePicker 分岐) → `TimePickerCellViewHolder.kt:195` (`notifyTimePickerSelection`) | 「再生成後の時刻ダイアログは確定で1回だけ通知する」 | ✅ 一致 |
| DatePicker の値確定が回復する | `KsSettingsView.kt:742-771` (DatePicker 分岐) → `MaterialDatePickerPresenter.kt:127` (`notifySelection`) | 「再生成後の日付ダイアログは確定で1回だけ通知する」 | ✅ 一致 |
| キャンセルでは発火しない | 確定リスナーのみを再登録 (negative 経路には何も張らない) | 「再生成後の時刻ダイアログはキャンセルで通知しない」/「…を閉じるだけでは通知しない」/「再生成後の日付ダイアログはキャンセルで通知しない」 | ✅ 一致 (dismiss 経路も time で被覆) |
| 配色が再適用される | `TimePickerColorizer.kt:160` / `DatePickerColorizer.kt:185` (`reattach` = フック登録 + View 生成済みなら即時適用) | 「再生成後の時刻ダイアログにテーマ配色が再適用される」/「再生成後の日付ダイアログにテーマ配色が再適用される」(文字色・ボタン色・window 背景の `MaterialShapeDrawable.fillColor` まで検証) | ✅ 一致 |
| 「今日」操作が再提示される | `KsSettingsView.kt:751-768` (`buildTodayShortcut`) → `DatePickerColorizer.reattach` の `viewHook?.onViewCreated(root)` | 「再生成後の日付ダイアログに今日ボタンが再提示され機能する」(ラベル・表示月・選択日・`onValueChanged` 未発火)、「再提示された今日操作も範囲外の今日には反応しない」、「再生成後の今日ボタンのラベルはアクセシビリティにも公開される」 | ✅ 一致 (本文が挙げる観察可能な挙動 4 点すべてに対応テストあり) |
| 作り直し世代のダイアログも復元される | 世代付き tag も `PickerDialogTag.decode` 可能で走査対象 | 「今日ボタンの作り直しを経たダイアログも復元される」 | ✅ 一致 |
| id に区切り文字を含む Cell でも対応付けが成立する | `PickerDialogTag.decode` の `split(SEPARATOR, limit = 4)` | `PickerDialogTagTest`「ドットを含む id でも往復する」/「フィールド区切りを含む id でも往復する」+ `PickerDialogRecreationTest` の全ケースが `"settings.wake-up.time"` / `"settings.birthday.date"` を使用 | ✅ 一致 |

---

## Requirement 2: 対応付け不能時の dismiss フォールバック (ADDED)

### 本文条項の対応

| 条項 | 実装 | テスト | 状態 |
|---|---|---|---|
| 適格な Cell がなければ閉じる | `KsSettingsView.kt:688-690` (`if (!restored) fragment.dismiss()`) | 「該当 id が現 root に無ければ閉じられ誤発火しない」 | ✅ 一致 |
| 同型でない / 候補複数も「存在しない」扱い | `matchesPickerKind` (`KsSettingsView.kt:712-715`) / `singleOrNull` (`:708`) | 「uiStyle が変更されていたら閉じられる」/「同一 id の候補が複数なら閉じられる」 | ✅ 一致 |
| いかなる Cell の `onValueChanged` も発火してはならない | 不成立時はリスナーを一切登録しない | 上記 2 件 + 「復元されない場合でも他の Cell へ値が書き込まれない」(3 Cell を並べて全件未発火を検証) | ✅ 一致 |
| 別の Cell への値の書き込みは対応付けの成否によらず発生しない | `findRestoreTarget` が一意な Cell を返した場合のみ `restorePicker` がリスナーを張る。加えて `PickerRestoreRegistry.claim` を表示側でも取る (`TimePickerCellViewHolder.kt:102` / `MaterialDatePickerPresenter.kt:97`) ことで、生きたダイアログが別インスタンスの root へ束ね直されない | 「復元されない場合でも他の Cell へ値が書き込まれない」+ 新設「表示中のダイアログは入れ替わったインスタンスの Cell へ束ね直されない」/「表示中のダイアログは後から attach したインスタンスに閉じられない」/「作り直しを経た表示中のダイアログも後から attach したインスタンスに触られない」 | ✅ 一致 (002 で固定点が 3 件増加) |
| 複数インスタンス時は一律 dismiss | `KsSettingsView.kt:680` (`hasUniqueOwner = attachedViewCount(fm) <= 1`) / `PickerRestoreRegistry.kt:34-50` | 「複数の KsSettingsView が attach されていると閉じられ二重発火しない」+ `PickerRestoreRegistryTest`「attach 中のインスタンス数を FragmentManager 単位で数える」「別の FragmentManager のインスタンスは数に混ざらない」「KsSettingsView の attach と detach が登録に反映される」 | ✅ 一致 |
| 1 つの復元 Fragment への処理は一度だけ | `PickerRestoreRegistry.kt:66` (`claim`、弱参照集合) / `KsSettingsView.kt:686` (`if (!claim(fragment)) continue`) | `PickerRestoreRegistryTest`「復元 Fragment の処理権は一度しか渡らない」+「複数の KsSettingsView が attach されていると閉じられ二重発火しない」 | ✅ 一致 |

### Scenario 対応

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 該当 id が現 root にないダイアログは閉じられる | `runRestoreScan` / `findRestoreTarget` | 「該当 id が現 root に無ければ閉じられ誤発火しない」 | ✅ 一致 |
| 既定のランダム id では dismiss になる | 同上 | 「既定のランダム id では再構築で一致せず閉じられる」(前提として `before.id != after.id` を assert) | ✅ 一致 |
| uiStyle が変更されていたら閉じられる | `matchesPickerKind` (`Date` は `uiStyle == Material` を要求) | 「uiStyle が変更されていたら閉じられる」 | ✅ 一致 |
| 同一 id の候補が複数なら閉じられる | `findRestoreTarget` の `take(2)` + `singleOrNull` | 「同一 id の候補が複数なら閉じられる」(2 Section に同一 id を配置) | ✅ 一致 |
| 複数の KsSettingsView が存在すると閉じられ、二重発火しない | `hasUniqueOwner` 判定 + `claim` | 「複数の KsSettingsView が attach されていると閉じられ二重発火しない」(2 インスタンス構成であることを assert した上で、ダイアログ消滅と両 Cell 未発火を検証) | ✅ 一致 |

---

## Requirement 3: 復元走査の駆動条件 (ADDED)

| 条項 / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 両条件を最初に満たした時点で実行 (one-shot ラッチ) | `KsSettingsView.kt:655-660` (予約: `isRestoreScanScheduled`) / `671-677` (実行と完了ラッチ: `isRestoreScanCompleted`) | 下記 4 Scenario | ✅ 一致 |
| root 反映と attach のどちらが先でも成立 | `onAttachedToWindow` (`:247-249`) と `setRootDirect` (`:375-376`) の双方から同じ関数を呼ぶ | 「attach 前に root が反映される順序でも復元される」/「attach 後に root が反映される順序でも復元される」(後者は root 未反映の時点でダイアログが未処理であることも assert) | ✅ 一致 |
| 走査が繰り返されてもリスナー・配色・「今日」が重複しない | 完了ラッチ + `claim` の二重防御 + `DatePickerTodayShortcut.install` の既存ボタン検出 (`DatePickerTodayShortcut.kt:91`) | 「復元走査は同じダイアログを二度処理しない」 | ✅ 一致 |
| 確定操作での発火は常に1回 | 同上 | 「再生成後の時刻ダイアログは確定で1回だけ通知する」/「再生成後の日付ダイアログは確定で1回だけ通知する」(いずれも `assertEquals(listOf(expected), notified)` で件数まで固定) | ✅ 一致 |
| (002 で追加) 予約分を空振りで消化しても駆動条件は失効しない | `runRestoreScan` 冒頭で予約ラッチを下ろし (`:673`)、走査に至らなかった経路 (`:675` / `:676`) では完了ラッチを立てない | 新設「予約分を detach 中に消化しても再 attach で復元される」 | ✅ 一致 |

**駆動タイミングの解釈について** (001 から継続): spec は「最初に満たした時点で実行される」と書き、実装は同時点で `post` により**次のメッセージへ予約**する。これは「同一トラバーサルでまとめて起きる複数インスタンスの attach を数え切ってから判定する」ために必要な遅延であり、Scenario の GIVEN/WHEN/THEN はすべて満たされているため一致と判定した。002 で予約と完了が別状態になったことで、「予約分が空振りしても次の attach で駆動条件が再成立する」という spec の趣旨 (どちらの順序でも必ず一度は走る) により忠実になっている。

**適格条件「現 root にちょうど1つ」の数え方** (001 から継続・実装変更なし): `internalRoot.sections.flatMap { it.cells }` を対象とし、`isVisible = false` の Section / Cell も母数に含める。spec は可視性に言及しておらず「現 root」の文言どおりの解釈であり、かつ隠れた同一 id が存在する場合は dismiss (安全側) へ倒れるため、一致と判定した。

---

## 追加検査

| 項目 | 結果 |
|---|---|
| tasks.md 全 20 タスクの完了 | ✅ 全 `[x]`。**虚偽チェックなし** — 上表のとおり全タスクに対応する実装・テストを確認 |
| 逆流検査 (足場アーティファクトの書き換え) | ✅ なし。`git diff HEAD -- kasane/changes/fix-picker-dialog-recreation/` は `tasks.md` のチェックボックス 19 行のみ。`proposal.md` / `specs/` / `ui/brief.md` は無変更。既存の `review-001.md` / `verify-001.md` / `second-opinion-002.md` も無変更 (untracked のまま内容不変) |
| 未記録乖離 | ✅ なし (❌ が 0 件のため) |
| UI 変更の記録 | ✅ `ui/brief.md` に mock 免除の理由と視覚の正 (通常表示のダイアログ) が記録されている。`ui/verification/` に `repro-steps.md` + before 7 枚 / after 12 枚 |
| 実環境検証証跡の有効性 (002 での再取得要否) | ✅ 再取得不要と判断。002 の修正 3 点はいずれも「Activity 再生成時の単独インスタンス経路」の観察可能な挙動を変えない (claim 追加は複数インスタンス / インスタンス入れ替え時のみ分岐が変わる。ラッチ 2 状態化は detach を挟む場合のみ。KDoc / テスト追加は挙動不変)。かつ 002 が塞いだ failure mode は Robolectric で再現可能な範囲であり、`runtime-behavior-verification.md` の「ユニットテストで症状を再現できない不具合」の適用範囲外 |
| テスト全件成功 | ✅ 866 件成功 / 失敗 0 (実行して確認) |
| Non-Goals の遵守 | ✅ ボトムシート系 (`PickerCellViewHolder:75` / `NumberPickerCellViewHolder:68` / `DatePickerCellViewHolder:124` の `BottomSheetDialog`) に変更なし。公開 API の追加・変更・削除なし (新規シンボルはすべて `internal`) |

---

## 判定

**VALID** — デルタスペックの全 Requirement / Scenario が実装とテストに対応し、欠落・乖離・虚偽チェック・逆流はない。

001 回目から Requirement / Scenario の対応状況に変化はなく、修正 4 件はいずれも既存の対応を壊さずに固定点を 5 件増やしている。品質観点の残指摘は `review-002.md` (🔵 2 件のみ、APPROVED) を参照。
