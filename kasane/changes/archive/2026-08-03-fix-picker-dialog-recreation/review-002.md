# レビュー結果: fix-picker-dialog-recreation (002 回目)

**日付**: 2026-08-03
**判定**: APPROVED

## サマリー

1 周目に確定した 4 件 (🟠1・🟡2・🔵1) はすべて解決している。焦点だった Major — 復元走査が「表示中の生きたダイアログ」まで処理対象に含めていた問題 — は、`PickerRestoreRegistry.claim()` を**表示側でも取る**方式で塞がれており、ダイアログを立てる経路が `TimePickerCellViewHolder.kt:98` と `MaterialDatePickerPresenter.kt:93` の 2 箇所しかないこと (grep で網羅確認) から、世代を進める再表示 (「今日」による作り直し) を含めて漏れがない。復元経路は Activity 再生成後に別インスタンスとして戻るため claim されず、静的読解・テスト・変異注入の 3 方向で非破壊を確認した。

one-shot ラッチの 2 状態化にも走査漏れ・二重走査の穴は見つからなかった。ビルド・テストとも green (866 件 / 失敗 0)。新規指摘は 🔵 2 件のみで、いずれも実装の欠陥ではなく将来の被覆と記録に関するもの。

## 実施した検証

- `./gradlew testDebugUnitTest --rerun-tasks` (ANDROID_HOME 環境変数指定) → BUILD SUCCESSFUL / **tests 866, failures 0, errors 0, skipped 0** (1 周目 861 → +5。`PickerDialogRecreationTest` 24→28、`PickerRestoreRegistryTest` 3→4)
- **変異注入によるテストの検出力確認** (実施後、実装は元のバイト列へ復元済み。`shasum` で backup と同一性確認、全件再実行して green を再確認)
  - 表示側の `PickerRestoreRegistry.claim(picker)` を 2 箇所とも除去 → 新設 3 件が **すべて FAILED**
    - 「表示中のダイアログは後から attach したインスタンスに閉じられない」(:459 — 生きたダイアログが閉じられる)
    - 「表示中のダイアログは入れ替わったインスタンスの Cell へ束ね直されない」(:491 — **別 root の Cell へ確定値が書き込まれる**。1 周目 Major の経路 2 が実在したことの裏取りでもある)
    - 「作り直しを経た表示中のダイアログも後から attach したインスタンスに触られない」(:522)
  - `runRestoreScan()` 冒頭の `isRestoreScanScheduled = false` を除去 (= 1 周目の単一ラッチ挙動へ差し戻し) → 「予約分を detach 中に消化しても再 attach で復元される」のみ FAILED (他 27 件は green。修正の意図をピンポイントに固定している)
- 走査対象の網羅確認: `grep -rn "\.show("` で main ソースのピッカー DialogFragment 表示経路は上記 2 箇所のみ。ボトムシート系 3 箇所 (`DatePickerCellViewHolder:124` / `NumberPickerCellViewHolder:68` / `PickerCellViewHolder:75`) は `BottomSheetDialog` で Non-Goals 対象
- 復元経路の非破壊確認 (静的): `claimedFragments` は `Collections.newSetFromMap(WeakHashMap())` で `Fragment` の同一性 (`Object.equals`) を鍵にする。Activity 再生成で saved state から戻る Fragment は別インスタンスのため claim 済みにならず、走査は従来どおり成立する。テスト側も 24 件の復元系がすべて green
- ラッチ 2 状態化の網羅読解: 「予約中に detach → 空振り消化 → 再 attach」で再予約されること、「予約中に detach→再 attach」で二重 post されないこと (`isRestoreScanScheduled` ガード)、走査せず終わったとき (`!isAttachedToHostWindow` / `findFragmentManager() == null`) に完了ラッチが立たないことを確認
- 状態非依存契約の確認: `TimePickerColorizer.reattach` / `DatePickerColorizer.reattach` は「先にフック登録 → View があればその場で適用」の順で、View 未生成なら早期 return してフックに委ねる。`DatePickerTodayShortcut.install` は既存ボタン検出で冪等 (`DatePickerTodayShortcut.kt:91`) のため、即時適用とフック発火が重なっても二重注入しない
- 公開 API 不変: 追加シンボル (`PickerDialogTag` / `PickerDialogKind` / `PickerRestoreRegistry` / `MaterialDatePickerPresenter` / `resolve*DialogColors` / `notifyTimePickerSelection` / `restoreTodayProvider`) はすべて `internal`。公開シグネチャの追加・変更・削除なし
- Non-Goals 非侵食: 変更 5 ファイル + 新規 6 ファイルのみ。ボトムシート系・iOS/MAUI に変更なし
- コメント規約 (`concepts/cross/conventions/comment-policy.md`) の機械照合: 新規・変更コメントに禁止参照 (change-id 裸参照 / `kasane/changes` パス / Phase・Decision 通番 / `MUST` `SHALL` 等) の混入なし。ADR 参照は許容形式 `android/ADR-0011`
- 足場の逆流検査: `git diff HEAD -- kasane/changes/fix-picker-dialog-recreation/` は `tasks.md` のチェックボックスのみ。`proposal.md` / `specs/` / `ui/` および `review-001.md` / `verify-001.md` / `second-opinion-002.md` は無変更

## 1 周目指摘の解決状況

| # | 指摘 | 対処 | 判定 |
|---|---|---|---|
| 1 | 🟠 復元走査が生きた Fragment を対象に含める | `TimePickerCellViewHolder.kt:102` / `MaterialDatePickerPresenter.kt:97` で表示直後に `claim()`。走査側は `KsSettingsView.kt:686` で claim 失敗を skip | ✅ 解決 (変異注入で 3 件が検出) |
| 2 | 🟡 one-shot ラッチが予約時点で立つ | `isRestoreScanScheduled` (予約) と `isRestoreScanCompleted` (完了) に分離。`KsSettingsView.kt:655-660` / `671-677` | ✅ 解決 (変異注入で 1 件が検出) |
| 3 | 🟡 `findFragmentManager` の KDoc 分離 | 新規 2 関数を `TimePickerCellViewHolder.kt:169-197` に置き、`findFragmentManager` の KDoc (:199-209) は宣言直前へ復帰 | ✅ 解決 |
| 4 | 🔵 detach 時の `unregister` を固定するテスト | `PickerRestoreRegistryTest`「KsSettingsView の attach と detach が登録に反映される」を追加 (実 View の addView / removeView で往復を固定) | ✅ 解決 |

「今日」による作り直しの 2 経路 — 生きたダイアログからの作り直し (`MaterialDatePickerPresenter.kt:80` の再帰) と、復元済みダイアログからの作り直し (`KsSettingsView.kt:759`) — はいずれも `MaterialDatePickerPresenter.show()` を通り、その末尾で新しい picker を claim する。したがって世代が進んだダイアログも表示直後から走査対象外になる。

## 指摘事項

### [🔵 Suggestion] 復元済みダイアログからの「今日」作り直し経路にテストがない

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:755-768` (`restorePicker` 内 `onRebuildRequired` クロージャ)

**問題点**:

「今日」操作は日グリッドを駆動できない状態 (テキスト入力モード表示中) では作り直しへ倒れる。既存テストは
「生きたダイアログを作り直す」(`作り直しを経た表示中のダイアログも後から attach したインスタンスに触られない`) と
「作り直し済みのダイアログを復元する」(`今日ボタンの作り直しを経たダイアログも復元される`) を覆っているが、
**「復元したダイアログを作り直す」** — テキスト入力モードのまま回転し、復元後に「今日」を押す経路 — は踏んでいない。
このクロージャは復元文脈で `generation + 1` の tag を発行する唯一の場所であり、`fragmentManager` / `cell` / `colors` /
`restoreTodayProvider` を復元時のものに差し替えて持ち回る独自コードでもある。読解上は正しいが、実行される回帰網が無い。

**推奨修正**: `再生成後の日付ダイアログに今日ボタンが再提示され機能する` と同型のテストに、復元後に
`MaterialIds.HEADER_TOGGLE` を押してから「今日」を押す変種を 1 件足す (世代が 2 になること・確定で
`onValueChanged` が今日の日付で 1 回発火することを固定すれば足りる)。

### [🔵 Suggestion / 蒸留フェーズ向け] ADR-0011 に「表示側 claim」が反映されていない

**該当箇所**: `kasane/decisions/android/0011-picker-dialog-rotation-restore-container-driven.md` Decision 4

現行の ADR は claim を「複数インスタンスが走査しても 1 つの Fragment への処理は一度だけ」という**走査どうしの排他**としてのみ記述している。
本周の修正で claim には「表示した側が生きたダイアログを走査対象から外す」という第 2 の役割が加わり、
これは position ベース tag を却下した failure mode (別 Cell への値書き込み) を走査対象の選別側からも塞ぐ、
設計上の要点になっている (`PickerRestoreRegistry.kt:52-65` の KDoc には記述済み)。
ADR は本レビューの修正対象ではないため指摘に留めるが、蒸留時に Decision 4 への追記候補として拾うのが筋。

## アクションプラン

いずれも任意。APPROVED の妨げにはならない。

1. **Suggestion**: 復元済みダイアログからの「今日」作り直し (`KsSettingsView.kt:755-768`) の回帰テストを 1 件追加
2. **Suggestion (蒸留時)**: ADR-0011 Decision 4 に「表示側 claim による生きたダイアログの除外」を追記

## 確認したが指摘に至らなかった観点

- **`claim` 済み集合の肥大化 / リーク**: `WeakHashMap` ベースで、dismiss 後に Fragment が到達不能になれば `add` 時の expunge で回収される。`attachedViews` も key (FragmentManager) / value 内 (KsSettingsView) の双方が弱参照で、value→key の強参照経路は無い
- **`claim` を表示直後に置くタイミング**: `picker.show()` と `claim()` は同一の同期ブロック内にあり、間に posted runnable は割り込まない
- **`isRestoreScanCompleted` をループ前に立てること**: 走査中の例外で残タスクが取り残されるが、その例外はいずれにせよ posted runnable から伝播してクラッシュするため、追加の防御に意味がない
- **`findRestoreTarget` が `isVisible = false` の Cell も母数に含める**: `verify-001.md` が spec の「現 root」の文言どおりの解釈として既に整理済み。今周も挙動は不変
- **1 周目に「修正しない」と裁定された 2 件** (横向き通常表示の参照ショット / `DatePickerColorizer.kt:488` の既存コメント) は再指摘していない
