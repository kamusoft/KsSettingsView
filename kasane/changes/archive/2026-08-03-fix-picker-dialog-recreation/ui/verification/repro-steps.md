# 再現手順: fix-picker-dialog-recreation (修正前)

tasks.md 5.1 の実施記録。`concepts/cross/conventions/runtime-behavior-verification.md` に基づく
「修正前に実環境で症状を再現する」フェーズ。5.2 (修正後の解消確認) は本書と**同一手順**をなぞること。

## 1. 環境

| 項目 | 値 |
| --- | --- |
| 対象 commit | `d53ac69d1a26dde746d431a49fd9756de043b5c3` (ライブラリ・サンプルとも未修正) |
| ワークツリー | `.claude/worktrees/fix-picker-dialog-recreation-0606d3` |
| 実行端末 | Android Emulator (AVD 名 `Pixel_6`) / serial `emulator-5554` |
| OS | Android 12 (API 31) — `ro.build.version.release=12` / `ro.build.version.sdk=31` |
| 画面 | 1080x2400 / density 420dpi |
| Android SDK | `~/Library/Developer/Xamarin/android-sdk-macosx/` |
| ホスト | macOS 26.5.1 (arm64) |

実機は使用していない (エミュレータのみ)。同一 Activity 内の KsSettingsView は 1 インスタンス。

### エミュレータ起動時の注意

既定の GPU モード (`hw.gpu.mode=auto`) では `emulator` プロセスが起動直後に
`detected a hanging thread 'QEMU2 main loop'` を出して終了コード 139 で落ちる (2回連続で再現)。
**`-gpu swiftshader_indirect` を明示すると安定して起動する。**

```sh
~/Library/Developer/Xamarin/android-sdk-macosx/emulator/emulator \
  -avd Pixel_6 -no-snapshot-load -gpu swiftshader_indirect -no-boot-anim
```

boot 完了は `adb -s emulator-5554 shell getprop sys.boot_completed` が `1` になるまで待つ。

## 2. ビルドと install

`samples/android` は `local.properties` を持たないため、`ANDROID_HOME` を明示しないと
`SDK location not found` でビルドが落ちる。環境変数で渡す (ファイルは追加しない)。

```sh
cd <worktree>/samples/android
ANDROID_HOME=$HOME/Library/Developer/Xamarin/android-sdk-macosx ./gradlew :app:assembleDebug

adb -s emulator-5554 install -r \
  <worktree>/samples/android/app/build/outputs/apk/debug/app-debug.apk
```

生成 APK: `samples/android/app/build/outputs/apk/debug/app-debug.apk`
package: `jp.kamusoft.kssettingsview.samples.android`

## 3. 画面遷移

1. アプリを起動する (`adb -s emulator-5554 shell monkey -p jp.kamusoft.kssettingsview.samples.android -c android.intent.category.LAUNCHER 1`)
2. ルートメニューの **「入力 Cell 5 種デモ」** をタップする
3. リストを下方向にスクロールし、`TimePickerCell` の **「アラーム」** と
   `DatePickerCell（カレンダー）` の **「予約日」** を表示させる

対象 Cell (`InputCellsDemoScreen.kt`):

- 「アラーム」= `TimePickerCell`、初期値 07:30
- 「予約日」= `DatePickerCell(uiStyle = DatePickerUIStyle.Material, todayText = "今日")`、初期値 2026/06/01
- KsSettingsView には `theme = SampleTheme.maui` が適用されている
  (accent = amber `#FFBF00` / 背景 = クリーム `#F2EFE6`)。**回転前のダイアログはこの配色になる**
- 画面上部の「最後のイベント: ...」は `onValueChanged` の発火有無をそのまま映す。
  未発火なら `(none)` のまま

> 注: サンプルの Cell はいずれも `id` を明示していない (既定のランダム id)。
> また状態は `remember` (非 saveable) のため、回転で Cell 値は初期値に戻る。

## 4. 回転のさせ方

自動回転が ON のままだと `user_rotation` が無視されるため、**必ず先に自動回転を切る**。
(初回に `accelerometer_rotation 0` を入れたが boot 直後だったため反映されず、`1` に戻っていた。
 実行直前に投げ直して値を `settings get` で確認すること)

```sh
# 縦 → 横 (Activity 再生成)
adb -s emulator-5554 shell settings put system accelerometer_rotation 0
adb -s emulator-5554 shell settings put system user_rotation 1

# 縦に戻す
adb -s emulator-5554 shell settings put system user_rotation 0
```

反映確認: `adb -s emulator-5554 shell dumpsys window | grep -m1 "mRotation="` が
`mRotation=ROTATION_90` になること。

検証後は `accelerometer_rotation 1` に戻す。

## 5. 症状の再現手順と観測結果

### 5-A. TimePickerCell「アラーム」

手順:

1. 「アラーム」をタップして `MaterialTimePicker` を開く (キーボード入力モードで開く)
2. `adb ... shell input text "09"` → `adb ... shell input text "45"` で **09:45** を入力する
3. 回転前のスクリーンショットを撮る → `before-timepicker-dialog-normal.png`
4. 横向きに回転させて Activity を再生成する
5. 回転後のスクリーンショットを撮る → `before-timepicker-dialog-after-rotate.png`
6. ダイアログの **OK** をタップする
7. リストをスクロールして「アラーム」行と「最後のイベント」を撮る → `before-timepicker-ok-no-effect.png`

観測された事実:

- **症状1 (OK が効かない): 再現した。** 回転後のダイアログは 09:45 を保持したまま表示されているが、
  OK をタップしてもダイアログが閉じるだけで「アラーム」は **07:30** のまま、
  「最後のイベント」も **(none)** のまま。`onValueChanged` は発火していない
  (`before-timepicker-ok-no-effect.png`)
- **症状2 (配色が既定に戻る): 再現した。** 回転前はクリーム背景・amber の選択枠・amber の
  Cancel/OK (`before-timepicker-dialog-normal.png`)。回転後は淡紫背景・紫の選択枠・
  マゼンタの Cancel/OK という Material 既定配色に変わる (`before-timepicker-dialog-after-rotate.png`)
- 回転後もダイアログ自体は表示され続ける (Material の saved state で 09:45 も保持される) ため、
  見た目上は操作可能なのに確定できない「ゾンビダイアログ」になっている

### 5-B. 対照実験 (回転なし) — TimePickerCell

回転を挟まなければ同じ操作で OK が効くことを確認するための A/B。

手順: 縦向きのまま「アラーム」を開く → 09:45 を入力 → **回転させずに** OK をタップ。

観測された事実:

- **OK は正常に発火した。**「最後のイベント: アラーム → 09:45」が表示され、
  「アラーム」行も **09:45** に更新される (`before-timepicker-ok-control-no-rotate.png`)
- したがって 5-A の未発火は「回転 (Activity 再生成) によってのみ起きる」ことが示された

### 5-C. DatePickerCell (Material)「予約日」

手順:

1. 「予約日」をタップして `MaterialDatePicker` を開く
2. カレンダーの **15 日**をタップして選択を 2026/06/15 に変える
3. 回転前のスクリーンショットを撮る → `before-datepicker-material-normal.png`
4. 横向きに回転させて Activity を再生成する
5. 回転後のスクリーンショットを撮る → `before-datepicker-material-after-rotate.png`
6. ダイアログの **OK** をタップする
7. リストをスクロールして「予約日」行と「最後のイベント」を撮る → `before-datepicker-ok-no-effect.png`

観測された事実:

- **症状1 (OK が効かない): 再現した。** 回転後のダイアログは Jun 15, 2026 を保持しているが、
  OK をタップしても「予約日」は **2026/06/01** のまま、「最後のイベント」も **(none)** のまま
  (`before-datepicker-ok-no-effect.png`)
- **症状2 (配色が既定に戻る): 再現した。** 回転前はクリーム背景・amber の選択日丸・
  amber の Cancel/OK/「今日」。回転後は淡紫背景・紫の選択日丸・マゼンタの Cancel/OK に変わる
  (`before-datepicker-material-normal.png` vs `before-datepicker-material-after-rotate.png`)
- **症状3 (「今日」ボタンが消える): 再現した。** 回転前はダイアログ左下に amber の
  **「今日」** ボタンがある。回転後は同じ位置に鉛筆 (テキスト入力切替) アイコンだけが残り、
  「今日」ボタンが消えている (同上2枚の比較)

## 6. 証跡ファイル

すべて `kasane/changes/fix-picker-dialog-recreation/ui/verification/` 配下。

| ファイル | 内容 |
| --- | --- |
| `before-timepicker-dialog-normal.png` | TimePicker 回転前 (09:45 入力済み / amber 配色) |
| `before-timepicker-dialog-after-rotate.png` | TimePicker 回転後 (Material 既定配色に退行) |
| `before-timepicker-ok-no-effect.png` | 回転後 OK 後 — アラーム 07:30 / 最後のイベント (none) |
| `before-timepicker-ok-control-no-rotate.png` | 対照: 回転なしなら OK が発火する (アラーム 09:45) |
| `before-datepicker-material-normal.png` | DatePicker(Material) 回転前 (Jun 15 選択 / amber 配色 / 「今日」あり) |
| `before-datepicker-material-after-rotate.png` | DatePicker(Material) 回転後 (既定配色 / 「今日」消失) |
| `before-datepicker-ok-no-effect.png` | 回転後 OK 後 — 予約日 2026/06/01 / 最後のイベント (none) |

## 7. 5.2 (修正後) で確認すべきこと

本書の 5-A / 5-C を同一手順でなぞり、`after-` 接頭辞で証跡を残す。

- サンプルの Cell は **id 未指定 (既定のランダム id)** かつ状態が `remember` (非 saveable) のため、
  デルタスペックの「既定のランダム id では dismiss になる」Scenario に該当する可能性が高い。
  修正後は**完全復元ではなく dismiss** が期待挙動になりうる — その場合の受け入れ基準は
  「回転後にダイアログが閉じ、いかなる Cell にも誤発火がないこと」
- 完全復元 (OK 発火 / 配色維持 / 「今日」維持) を実環境で確認するには、安定 id を持つ Cell が
  必要になる。サンプルに該当 Cell がなければ、5.2 の担当者はどう確認するかを
  orchestrator に確認すること (本フェーズではサンプルを変更していない)

---

# 解消確認: fix-picker-dialog-recreation (修正後)

tasks.md 5.2 の実施記録。上記 5-A / 5-C を**同一手順**でなぞった結果。

## 8. 環境 (5.1 との同一性)

| 項目 | 5.1 (修正前) | 5.2 (修正後) | 差異 |
| --- | --- | --- | --- |
| 実行端末 | Emulator `Pixel_6` / `emulator-5554` | 同左 (同一の起動中インスタンス) | なし |
| OS / 画面 | Android 12 (API 31) / 1080x2400 / 420dpi | 同左 | なし |
| ホスト・SDK | macOS 26.5.1 / Xamarin android-sdk-macosx | 同左 | なし |
| ビルド対象 | 未修正コード (commit `d53ac69`) | **修正入りワークツリー** (グループ1〜4 適用済み) | 意図した差異 |
| 実機 | 未使用 | 未使用 | なし |

修正が APK に載っていることは、`app-debug.apk` の `classes3.dex` に `PickerDialogTag` /
`PickerRestoreRegistry` の文字列が含まれることで確認した (修正で追加された新規クラス)。

ビルド・install・回転操作の手順は 2 章 / 4 章と同一。**自動回転の落とし穴も再発した** —
APK 再インストールを挟んだ後に `accelerometer_rotation` が `1` に戻っており、
`user_rotation` を書いても回転しなかった。実行直前に `settings get` で確認する運用が必須。

## 9. A. 3症状の解消

### 9-A. TimePickerCell「アラーム」(5-A と同一手順)

観測された事実:

- **症状1 (OK が効かない): 解消した。** 回転後のダイアログで OK をタップすると
  「最後のイベント: アラーム → 09:45」が表示され、「アラーム」行も **09:45** に更新された
  (`after-timepicker-ok-applied.png`)。修正前は 07:30 / (none) のままだった
- **症状2 (配色が既定に戻る): 解消した。** 回転後もクリーム背景・amber の選択枠・
  amber の Cancel/OK を維持している (`after-timepicker-dialog-after-rotate.png`)。
  修正前の同構図 (`before-timepicker-dialog-after-rotate.png`) は淡紫背景・紫枠・
  マゼンタ Cancel/OK で、2枚を並べると差は明確

### 9-B. DatePickerCell (Material)「予約日」(5-C と同一手順)

観測された事実:

- **症状1 (OK が効かない): 解消した。** 回転後のダイアログで OK をタップすると
  「最後のイベント: 予約日 → 2026/06/15」が表示され、「予約日」行も **2026/06/15** に
  更新された (`after-datepicker-ok-applied.png`)
- **症状2 (配色が既定に戻る): 解消した。** 回転後もクリーム背景・amber の選択日丸・
  amber の Cancel/OK/「今日」 (`after-datepicker-material-after-rotate.png`)
- **症状3 (「今日」ボタンが消える): 解消した。** 回転後もダイアログ左下に amber の
  **「今日」** が表示される (同上)。押すと表示月が **August 2026**、選択日が **3** に移動した
  (今日 = 2026-08-03)。このとき「最後のイベント」は **(none)** のままで、
  「今日」操作自体は `onValueChanged` を発火していない (`after-datepicker-today-works.png`)

### 9-C. 作り直し世代のダイアログ (追加観測)

「今日」で作り直された世代のダイアログを、さらに回転させた場合も確認した。

- 回転後も amber 配色・「今日」ボタン・選択日 (Aug 3) を保ったまま復元された
  (`after-datepicker-recreated-generation-restored.png`)
- その状態で OK をタップすると「最後のイベント: 予約日 → 2026/08/03」が発火し、
  「予約日」行も 2026/08/03 に更新された (`after-datepicker-generation-ok-applied.png`)

## 10. B. キャンセル/dismiss で発火しないこと

- **TimePicker / Cancel ボタン:** 回転後のダイアログ (09:45 表示) で Cancel をタップ。
  「アラーム」は **07:30** (再生成後の初期値)、「最後のイベント」は **(none)** のまま。
  他の Cell (予約日 2026/06/01 等) にも変化なし (`after-timepicker-cancel-no-effect.png`)
- **DatePicker / 戻るキーによる dismiss:** 回転後のダイアログ (Jun 20 選択) で
  `KEYCODE_BACK`。「予約日」は **2026/06/01**、「最後のイベント」は **(none)** のまま
  (`after-datepicker-dismiss-no-effect.png`)

誤発火は観測されなかった。

> 注: 回転で Activity が再生成されるとサンプルの `remember` 状態は初期値に戻るため、
> キャンセル検証の基準値は「再生成後の初期値」になる。誤発火があれば
> 「最後のイベント」に値が入るので、未発火との識別はできている。

## 11. C. dismiss フォールバックの実環境観測

**実施した。** サンプルは Compose DSL 経路で `DeclarativeDSLIdentity` が決定的な安定 id を
振るため、無改変では完全復元になり dismiss を踏まない (9 章の結果がその裏付けでもある)。
そこで **サンプルアプリに一時的な変更**を入れて観測した。

一時変更の内容 (`samples/android/.../InputCellsDemoScreen.kt`):

- `val unstableAlarmId = remember { UUID.randomUUID().toString() }` を追加し、
  TimePickerCell「アラーム」に `.cellID(unstableAlarmId)` を付けた。
  `remember` は saveable ではないため、Activity 再生成のたびに新しい UUID になる
  = **再生成をまたいで Cell の id が変わる**

観測された事実:

- ダイアログを開いて 09:45 を入力 → 回転 → **ダイアログは閉じられた**。
  「アラーム」は 07:30、「予約日」は 2026/06/01、「最後のイベント」は **(none)** のままで、
  いかなる Cell にも誤発火はなかった (`after-timepicker-unstable-id-dismissed.png`)

**一時変更は検証後に元へ戻し、`git status --porcelain samples/` が空であることを確認済み。**
戻した状態で再ビルド・再 install し、エミュレータには無改変のサンプルが入っている。
回転設定も `accelerometer_rotation=1` / `user_rotation=0` (検証前の状態) に戻した。

## 12. 証跡ファイル (5.2)

| after ファイル | 対になる before | 内容 |
| --- | --- | --- |
| `after-timepicker-dialog-normal.png` | `before-timepicker-dialog-normal.png` | TimePicker 回転前 (09:45 / amber 配色) |
| `after-timepicker-dialog-after-rotate.png` | `before-timepicker-dialog-after-rotate.png` | 回転後も amber 配色を維持 (before は Material 既定に退行) |
| `after-timepicker-ok-applied.png` | `before-timepicker-ok-no-effect.png` | 回転後 OK — アラーム 09:45 / 最後のイベント発火 (before は 07:30 / (none)) |
| `after-timepicker-cancel-no-effect.png` | (対応なし / B の新規) | 回転後 Cancel — 値もイベントも変化なし |
| `after-datepicker-material-normal.png` | `before-datepicker-material-normal.png` | DatePicker(Material) 回転前 (Jun 15 / amber / 「今日」あり) |
| `after-datepicker-material-after-rotate.png` | `before-datepicker-material-after-rotate.png` | 回転後も amber 配色・「今日」を維持 (before は既定配色・「今日」消失) |
| `after-datepicker-ok-applied.png` | `before-datepicker-ok-no-effect.png` | 回転後 OK — 予約日 2026/06/15 / 最後のイベント発火 |
| `after-datepicker-today-works.png` | (対応なし / A.3 の機能確認) | 回転後の「今日」押下で August 2026 / 3 へ移動。イベントは (none) のまま |
| `after-datepicker-recreated-generation-restored.png` | (対応なし / 追加観測) | 作り直し世代のダイアログも回転後に復元される |
| `after-datepicker-generation-ok-applied.png` | (対応なし / 追加観測) | 世代付きダイアログの OK が 2026/08/03 で発火 |
| `after-datepicker-dismiss-no-effect.png` | (対応なし / B の新規) | 回転後 戻るキーで dismiss — 値もイベントも変化なし |
| `after-timepicker-unstable-id-dismissed.png` | (対応なし / C の新規) | 不安定 id の Cell では回転後にダイアログが閉じ、誤発火なし |

なお `before-timepicker-ok-control-no-rotate.png` は 5.1 の対照実験 (回転なし) であり、
5.2 に対応する after はない。

## 13. 未解消・新規に気づいた事象

- 修正前に再現していた3症状は、いずれも解消を確認した。**未解消の症状はない**
- 検証中に新たな不具合は観測されなかった
- 運用上の注意 (不具合ではない): APK の再インストールを挟むと
  `accelerometer_rotation` が `1` に戻っていることがある。回転操作の直前に
  `settings get system accelerometer_rotation` で確認しないと、回転せず
  「再生成が起きていないのに起きたつもり」で誤判定しうる

---

# 再確認: fix-picker-dialog-recreation (レビュー修正後)

上の 5.2 (9〜13 章) の後に独立レビュー (review-002 / second-opinion-002) の Major 指摘対応で
**プロダクションコードが変わった**ため、同一手順をもう一度なぞって撮り直した記録。

## 14. なぜ撮り直したか

5.2 の撮影後に入ったプロダクションコードの変更:

- ピッカーダイアログを `show()` した直後に `PickerRestoreRegistry.claim(picker)` を呼ぶようになった
  (`TimePickerCellViewHolder.showTimePicker` / `MaterialDatePickerPresenter.show`)。
  自分で立てた**生きた**ダイアログを、後から attach した別インスタンスの復元走査の対象から外す
- 復元走査の one-shot ラッチが「予約」(`isRestoreScanScheduled`) と
  「完了」(`isRestoreScanCompleted`) の 2 状態に分離された

`claim()` は**回転と無関係な通常の `show()` 経路にも入った**ため、9〜11 章の結果は現在のコードに
対応しない。この change の出発点が「ユニットテスト green + レビュー APPROVED でも実機では
直っていなかった」であることから、コードが動いた以上もう一度実環境で見る、という判断。

既存の `after-*.png` は上記変更**前**の撮影なので、同名で上書き差し替えした
(`before-*.png` は修正前の証跡なので一切触れていない)。

## 15. 環境 (5.1 / 5.2 との同一性)

| 項目 | 5.1 / 5.2 | 今回 | 差異 |
| --- | --- | --- | --- |
| 実行端末 | Emulator `Pixel_6` / `emulator-5554` | 同左 (同一の起動中インスタンス) | なし |
| OS | Android 12 (API 31) | 同左 (`ro.build.version.sdk=31`) | なし |
| 画面 | 1080x2400 / 420dpi | 同左 | なし |
| ホスト・SDK | macOS / Xamarin android-sdk-macosx | 同左 | なし |
| ビルド手順 | 2 章と同一 | 同左 (`ANDROID_HOME` を環境変数で明示) | なし |
| 実機 | 未使用 | 未使用 | なし |
| ビルド対象 | 5.2 時点の修正コード | **レビュー修正適用後のコード** | 意図した差異 |

実機 (`0B261JEC216142` / `<android-device-serial>`) は接続されていたが、比較の一貫性のため使用していない。

修正が APK に載っていることは、`app-debug.apk` の `classes3.dex` にのみ
`PickerRestoreRegistry` の文字列が含まれる (他の 7 つの dex には 0 件) ことで確認した。

**自動回転の落とし穴は今回も踏んだ。** APK 再インストール後に `accelerometer_rotation` が `1` に
戻っており、1 回目の `user_rotation 1` は無視されて `mRotation=ROTATION_0` のままだった
(= Activity 再生成が起きていない)。以降は回転操作のたびに `settings get` で `0` を確認してから
`user_rotation` を書き、`dumpsys window` の `mRotation` で反映を確認する運用にした。

## 16. A. 復元経路 (最重要)

### 16-A. TimePickerCell「アラーム」(5-A と同一手順)

観測された事実:

- 回転前: クリーム背景・amber の選択枠・amber の Cancel/OK で 09:45 を入力
  (`after-timepicker-dialog-normal.png`)
- 回転後 (`mRotation=ROTATION_90` を確認済み): **配色は維持されている。**
  クリーム背景・amber の選択枠・amber の Cancel/OK のまま、09:45 も保持
  (`after-timepicker-dialog-after-rotate.png`)
- その状態で OK をタップ: 「最後のイベント: アラーム → 09:45」が表示され、
  「アラーム」行も **09:45** に更新された (`after-timepicker-ok-applied.png`)

**退行なし。**

### 16-B. DatePickerCell (Material)「予約日」(5-C と同一手順)

観測された事実:

- 回転前: クリーム背景・amber の選択日丸 (Jun 15)・amber の「今日」/Cancel/OK
  (`after-datepicker-material-normal.png`)
- 回転後: **配色も「今日」ボタンも維持されている。** クリーム背景・amber の Jun 15 丸・
  左下に鉛筆アイコン + amber の**「今日」**・amber の Cancel/OK
  (`after-datepicker-material-after-rotate.png`)
- その状態で OK をタップ: 「最後のイベント: 予約日 → 2026/06/15」が発火し、
  「予約日」行も **2026/06/15** に更新された (`after-datepicker-ok-applied.png`)
- 復元されたダイアログの**「今日」を押すと**、表示月が **August 2026**、選択日が **3** に移った
  (今日 = 2026-08-03)。このとき「最後のイベント」は **(none)** のままで、
  「今日」操作自体は `onValueChanged` を発火していない (`after-datepicker-today-works.png`)

**退行なし。**

### 16-C. 作り直し世代のダイアログを、さらに回転させる

「今日」で作り直された世代 (generation=1) のダイアログを、さらに回転させた場合。
この世代は復元走査の中から `MaterialDatePickerPresenter.show(..., generation + 1)` で
表示されるため、**新しい `claim()` 呼び出しを通る経路**にあたる。

観測された事実:

- 回転後もクリーム背景・amber の Aug 3 丸・「今日」ボタンを保ったまま復元された
  (`after-datepicker-recreated-generation-restored.png`)
- その状態で OK をタップすると「最後のイベント: 予約日 → 2026/08/03」が発火し、
  「予約日」行も **2026/08/03** に更新された (`after-datepicker-generation-ok-applied.png`)

**退行なし。**「表示側の claim」が復元済みダイアログの再走査を妨げていないことが確認できた。

## 17. B. 通常表示 (回転なし) の退行確認

`claim()` は回転と無関係な通常の `show()` 経路にも入ったため、今回新たに確認した項目。
いずれも回転を一切挟んでいない。

観測された事実:

- **OK:** 「アラーム」を開いて 09:45 を入力し、**回転させずに** OK をタップ。
  「最後のイベント: アラーム → 09:45」が表示され、「アラーム」行も **09:45** に更新された
  (`after-timepicker-no-rotate-ok-applied.png`)
- **キャンセル:** 続けて同じ Cell を開き **06:15** を入力してから Cancel をタップ。
  「アラーム」は直前の **09:45** のまま、「最後のイベント」も **アラーム → 09:45** のままで、
  06:15 は一切反映されていない (`after-timepicker-no-rotate-cancel-no-effect.png`)。
  06:15 を入れてからキャンセルしているので、誤発火があれば値が 06:15 に変わって識別できる

**通常表示に退行なし。**

## 18. C. 回転後のキャンセル / dismiss で発火しないこと

観測された事実:

- **TimePicker / Cancel ボタン:** 回転後のダイアログ (09:45 表示・amber 配色) で Cancel をタップ。
  「アラーム」は **07:30**、「予約日」は **2026/06/01**、「最後のイベント」は **(none)** のまま
  (`after-timepicker-cancel-no-effect.png`)
- **DatePicker / 戻るキーによる dismiss:** 回転後のダイアログ (Jun 20 選択) で `KEYCODE_BACK`。
  「予約日」は **2026/06/01**、「アラーム」は **07:30**、「最後のイベント」は **(none)** のまま
  (`after-datepicker-dismiss-no-effect.png`)

誤発火は観測されなかった。

> 注 (11 章と同じ): 回転で Activity が再生成されるとサンプルの `remember` 状態は初期値に戻るため、
> キャンセル検証の基準値は「再生成後の初期値」になる。

## 19. 差し替え・追加したスクリーンショット

差し替え (同名で上書き。11 件):

| ファイル | 対応する確認項目 |
| --- | --- |
| `after-timepicker-dialog-normal.png` | A / 回転前 (09:45 / amber) |
| `after-timepicker-dialog-after-rotate.png` | A / 回転後も amber 配色を維持 |
| `after-timepicker-ok-applied.png` | A / 回転後 OK でアラーム 09:45・イベント発火 |
| `after-timepicker-cancel-no-effect.png` | C / 回転後 Cancel で無変化 |
| `after-datepicker-material-normal.png` | A / 回転前 (Jun 15 / amber / 「今日」あり) |
| `after-datepicker-material-after-rotate.png` | A / 回転後も amber 配色・「今日」を維持 |
| `after-datepicker-ok-applied.png` | A / 回転後 OK で予約日 2026/06/15・イベント発火 |
| `after-datepicker-today-works.png` | A / 回転後の「今日」押下で August 2026 / 3 へ移動 |
| `after-datepicker-recreated-generation-restored.png` | A / 作り直し世代も回転後に復元される |
| `after-datepicker-generation-ok-applied.png` | A / 世代付きダイアログの OK が 2026/08/03 で発火 |
| `after-datepicker-dismiss-no-effect.png` | C / 回転後 戻るキーで無変化 |

追加 (B の新規カット。2 件):

| ファイル | 内容 |
| --- | --- |
| `after-timepicker-no-rotate-ok-applied.png` | 回転なしで OK — アラーム 09:45・イベント発火 |
| `after-timepicker-no-rotate-cancel-no-effect.png` | 回転なしで 06:15 入力後キャンセル — 09:45 のまま |

**差し替えなかった after ファイル (1 件):**

- `after-timepicker-unstable-id-dismissed.png` — 11 章の不安定 id 観測。撮り直しには
  サンプルアプリの一時改変が必要だが、今回のタスクでは `samples/android` の変更が禁止され、
  かつ不安定 id の観測は不要と指示されている。**したがってこのファイルだけは
  レビュー修正前 (11 章時点) の撮影のままである。**
  現在のコードで dismiss フォールバックが同じく機能するかは、今回**未観測**

`before-*.png` は 1 件も変更・削除していない。

## 20. 未解消・新規に気づいた事象

- レビュー修正後も、A (復元経路) / B (通常表示) / C (キャンセル・dismiss) のいずれにも
  **退行は観測されなかった**。修正前に再現していた 3 症状も再発していない
- 検証中に新たな不具合は観測されなかった
- 15 章のとおり、自動回転が `1` に戻る落とし穴は今回も発生した。
  **回転操作の直前に `settings get system accelerometer_rotation` を確認し、
  回転後に `dumpsys window` の `mRotation` で反映を確認する**手順を守らないと、
  「再生成が起きていないのに起きたつもり」で誤判定する
- 検証後、エミュレータの回転設定は `accelerometer_rotation=1` / `user_rotation=0`
  (検証前の状態) に戻した。`mRotation=ROTATION_0` を確認済み
