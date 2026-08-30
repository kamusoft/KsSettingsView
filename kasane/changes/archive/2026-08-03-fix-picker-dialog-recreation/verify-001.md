# 一致検証結果: fix-picker-dialog-recreation (001 回目)

**日付**: 2026-08-03
**判定**: VALID

対象デルタスペック: `kasane/changes/fix-picker-dialog-recreation/specs/settings-view-android-ui/spec.md`
`deviation.md`: なし (合意済み乖離なし)

テスト実行: `cd android && ANDROID_HOME=$HOME/Library/Developer/Xamarin/android-sdk-macosx ./gradlew testDebugUnitTest --rerun-tasks` → BUILD SUCCESSFUL / **tests 861, failures 0, errors 0, skipped 0**。うち `PickerDialogRecreationTest` 24 件・`PickerDialogTagTest` 10 件・`PickerRestoreRegistryTest` 3 件。

---

## Requirement 1: 再生成後のピッカーダイアログの完全復元 (ADDED)

### 本文条項の対応

| 条項 | 実装 | テスト | 状態 |
|---|---|---|---|
| 「attach 済み」∧「root 反映済み」で復元走査を駆動 | `KsSettingsView.kt:236-238` / `364-365` / `642` (`scheduleRestoreScanIfReady`) | `PickerDialogRecreationTest`「attach 前に root が反映される順序でも復元される」/「attach 後に root が反映される順序でも復元される」 | ✅ 一致 |
| 適格条件: 同一 `id`・同型・現 root にちょうど1つ | `KsSettingsView.kt:682-691` (`findRestoreTarget`、`take(2)` + `singleOrNull`) / `693-696` (`matchesPickerKind`) | 「該当 id が現 root に無ければ閉じられ誤発火しない」/「uiStyle が変更されていたら閉じられる」/「同一 id の候補が複数なら閉じられる」 | ✅ 一致 |
| `isEnabled` / `onValueChanged == null` / `minDate`・`maxDate` 変化は適格性に影響しない | `findRestoreTarget` は `id` と型のみを見る (構成値を参照しない) | 「`onValueChanged` が null で `isEnabled` が false でも適格として復元される」 | ✅ 一致 (`minDate` / `maxDate` の変化は実装上参照経路が存在せず構造的に保証) |
| 復元ダイアログは表示時の構成と選択状態を維持し、Cell の構成値を流し込まない | `KsSettingsView.kt:706-` (`restorePicker` はリスナー・配色・「今日」のみ付け直し、`setSelection` 等を呼ばない) | 「復元したダイアログは Cell の構成値変化を取り込まない」 | ✅ 一致 |
| 対応付けは `id` で行い、区切り文字を含む id でも成立 | `PickerDialogTag.kt:42-50` (encode) / `68-78` (decode。可変長 `cellId` を最終フィールドに置く固定書式) | `PickerDialogTagTest` 全 10 件 (ドット・`|`・空文字・Unicode・`.r1` 風末尾・多世代・他所 tag の除外) | ✅ 一致 |
| 世代を経たダイアログも復元対象 | `MaterialDatePickerPresenter.kt:67-71` (tag へ generation 埋め込み) / `KsSettingsView.kt` `restorePicker` 内の作り直しで `tag.generation + 1` | 「今日ボタンの作り直しを経たダイアログも復元される」 | ✅ 一致 |

### Scenario 対応

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| TimePicker の値確定が回復する | `KsSettingsView.kt:706-` (`restorePicker` の TimePicker 分岐) → `TimePickerCellViewHolder.kt:202` (`notifyTimePickerSelection`) | `PickerDialogRecreationTest`「再生成後の時刻ダイアログは確定で1回だけ通知する」 | ✅ 一致 |
| DatePicker の値確定が回復する | `restorePicker` の DatePicker 分岐 → `MaterialDatePickerPresenter.kt:123` (`notifySelection`) | 「再生成後の日付ダイアログは確定で1回だけ通知する」 | ✅ 一致 |
| キャンセルでは発火しない | 確定リスナーのみを再登録 (negative 経路には何も張らない) | 「再生成後の時刻ダイアログはキャンセルで通知しない」/「…を閉じるだけでは通知しない」/「再生成後の日付ダイアログはキャンセルで通知しない」 | ✅ 一致 (dismiss 経路も time で被覆) |
| 配色が再適用される | `TimePickerColorizer.kt:160` / `DatePickerColorizer.kt:185` (`reattach` = フック登録 + View 生成済みなら即時適用) | 「再生成後の時刻ダイアログにテーマ配色が再適用される」/「再生成後の日付ダイアログにテーマ配色が再適用される」(文字色・ボタン色・window 背景の `MaterialShapeDrawable.fillColor` まで検証) | ✅ 一致 |
| 「今日」操作が再提示される | `restorePicker` 内 `MaterialDatePickerPresenter.buildTodayShortcut(...)` → `DatePickerColorizer.reattach` の `viewHook?.onViewCreated(root)` | 「再生成後の日付ダイアログに今日ボタンが再提示され機能する」(ラベル・表示月・選択日・`onValueChanged` 未発火)、「再提示された今日操作も範囲外の今日には反応しない」、「再生成後の今日ボタンのラベルはアクセシビリティにも公開される」 | ✅ 一致 (本文が挙げる観察可能な挙動 4 点すべてに対応テストあり) |
| 作り直し世代のダイアログも復元される | 同上 (世代付き tag も `decode` 可能で走査対象) | 「今日ボタンの作り直しを経たダイアログも復元される」(世代 1 であることを assert した上で復元と確定発火を検証) | ✅ 一致 |
| id に区切り文字を含む Cell でも対応付けが成立する | `PickerDialogTag.decode` の `split(SEPARATOR, limit = 4)` | `PickerDialogTagTest`「ドットを含む id でも往復する」/「フィールド区切りを含む id でも往復する」+ `PickerDialogRecreationTest` の全ケースが `"settings.wake-up.time"` / `"settings.birthday.date"` を使用 | ✅ 一致 |

---

## Requirement 2: 対応付け不能時の dismiss フォールバック (ADDED)

### 本文条項の対応

| 条項 | 実装 | テスト | 状態 |
|---|---|---|---|
| 適格な Cell がなければ閉じる | `KsSettingsView.kt:656-` (`runRestoreScan` の `if (!restored) fragment.dismiss()`) | 「該当 id が現 root に無ければ閉じられ誤発火しない」 | ✅ 一致 |
| 同型でない / 候補複数も「存在しない」扱い | `matchesPickerKind` / `singleOrNull` | 「uiStyle が変更されていたら閉じられる」/「同一 id の候補が複数なら閉じられる」 | ✅ 一致 |
| いかなる Cell の `onValueChanged` も発火してはならない | 不成立時はリスナーを一切登録しない | 上記 2 件 + 「復元されない場合でも他の Cell へ値が書き込まれない」(3 Cell を並べて全件未発火を検証) | ✅ 一致 |
| 複数インスタンス時は一律 dismiss | `KsSettingsView.kt:656-` の `hasUniqueOwner = PickerRestoreRegistry.attachedViewCount(fm) <= 1` / `PickerRestoreRegistry.kt:33-49` | 「複数の KsSettingsView が attach されていると閉じられ二重発火しない」+ `PickerRestoreRegistryTest`「attach 中のインスタンス数を FragmentManager 単位で数える」「別の FragmentManager のインスタンスは数に混ざらない」 | ✅ 一致 |
| 1 つの復元 Fragment への処理は一度だけ | `PickerRestoreRegistry.claim()` (`PickerRestoreRegistry.kt:57`、弱参照集合) / 走査ループの `if (!claim(fragment)) continue` | `PickerRestoreRegistryTest`「復元 Fragment の処理権は一度しか渡らない」 | ✅ 一致 |

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
| 両条件を最初に満たした時点で実行 (one-shot ラッチ) | `KsSettingsView.kt:642-650` (`isAttachedToHostWindow` ∧ `isRootApplied` ∧ `!isRestoreScanScheduled` → `post { runRestoreScan() }`) | 下記 3 Scenario | ✅ 一致 |
| root 反映と attach のどちらが先でも成立 | `onAttachedToWindow` (236-238) と `setRootDirect` 経路 (364-365) の双方から同じ関数を呼ぶ | 「attach 前に root が反映される順序でも復元される」/「attach 後に root が反映される順序でも復元される」(後者は root 未反映の時点でダイアログが未処理であることも assert) | ✅ 一致 |
| 走査が繰り返されてもリスナー・配色・「今日」が重複しない | `isRestoreScanScheduled` ラッチ + `claim` の二重防御 + `DatePickerTodayShortcut.install` の既存ボタン検出 (`DatePickerTodayShortcut.kt:91`) | 「復元走査は同じダイアログを二度処理しない」 | ✅ 一致 |
| 確定操作での発火は常に1回 | 同上 | 「再生成後の時刻ダイアログは確定で1回だけ通知する」/「再生成後の日付ダイアログは確定で1回だけ通知する」(いずれも `assertEquals(listOf(expected), notified)` で件数まで固定) | ✅ 一致 |

**駆動タイミングの解釈について**: spec は「最初に満たした時点で実行される」と書き、実装は同時点で `post` により**次のメッセージへ予約**する。これは「同一トラバーサルでまとめて起きる複数インスタンスの attach を数え切ってから判定する」ために必要な遅延であり (変異注入で確認: `post` を同期呼び出しに変えると複数インスタンス Scenario が破れる)、Scenario の GIVEN/WHEN/THEN はすべて満たされているため一致と判定した。

**適格条件「現 root にちょうど1つ」の数え方**: 実装は `internalRoot.sections.flatMap { it.cells }` を対象とし、`isVisible = false` の Section / Cell も母数に含める。spec は可視性に言及しておらず「現 root」の文言どおりの解釈であり、かつ隠れた同一 id が存在する場合は dismiss (安全側) へ倒れるため、一致と判定した。

---

## 追加検査

| 項目 | 結果 |
|---|---|
| tasks.md 全 20 タスクの完了 | ✅ 全 `[x]`。**虚偽チェックなし** — 上表のとおり全タスクに対応する実装・テストを確認。グループ 5 (実環境検証) も `ui/verification/repro-steps.md` + before 7 枚 / after 11 枚の証跡で裏付けあり |
| 逆流検査 (足場アーティファクトの書き換え) | ✅ なし。`git diff HEAD` で `kasane/changes/` 配下の変更は `tasks.md` のチェックボックスのみ。`proposal.md` / `specs/` / `ui/brief.md` は無変更 |
| 未記録乖離 | ✅ なし (❌ が 0 件のため) |
| UI 変更の記録 | ✅ `ui/brief.md` に mock 免除の理由と視覚の正 (通常表示のダイアログ) が記録されている。合意済み妥協の記録は不要 (該当なし) |
| テスト全件成功 | ✅ 861 件成功 / 失敗 0 (実行して確認) |
| Non-Goals の遵守 | ✅ ボトムシート系 (PickerCell / NumberPickerCell / DatePickerCell(Spinner)) に変更なし。公開 API の追加・変更・削除なし (新規シンボルはすべて `internal`) |

---

## 判定

**VALID** — デルタスペックの全 Requirement / Scenario が実装とテストに対応し、欠落・乖離・虚偽チェック・逆流はない。

なお本検証は「約束したものが揃っているか」のみを見る。実装品質の観点では、走査対象の選別が「復元された Fragment」に限定されていないことによる Major 級の指摘が別途ある (`review-001.md`)。その指摘はデルタスペックの GIVEN (Activity 再生成後) の外側で顕在化する事象であり、Scenario 対応表の判定には影響しない。
