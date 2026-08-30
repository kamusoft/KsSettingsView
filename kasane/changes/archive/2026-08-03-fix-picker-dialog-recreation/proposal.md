# Proposal: fix-picker-dialog-recreation

## Why

MaterialTimePicker / MaterialDatePicker (DialogFragment) の表示中に Activity が再生成 (画面回転等) されると、ダイアログは FragmentManager の saved state から復元されるが、コードで登録した状態 (OK リスナー・着色フック・「今日」ボタン注入) は失われる。結果、「表示されているのに OK を押しても値が確定せず、配色も Material 既定に戻ったゾンビダイアログ」が残る。timepickercell-color-adjust のレビューで確認され、独立変更として切り出されたもの (経緯と決定は android/ADR-0011)。

## What Changes

対象能力: **settings-view-android-ui** (Android View Host)

1. **コンテナ駆動の復元**: KsSettingsView が「Window に attach 済み」かつ「root 反映済み」の両条件が揃った最初の時点で FragmentManager を一度だけ走査し (one-shot ラッチ)、復元済みピッカーを検出して「OK リスナー再登録 + 即時着色 + (DatePicker なら)「今日」ボタン再注入」を行う
2. **Fragment tag の cell.id 化**: TimePickerCell / DatePickerCell (Material) の Fragment tag を `bindingAdapterPosition` ベースから cell.id ベースへ変更 (世代サフィックス `.r<n>` は維持)。現 root に対応 Cell が見つからなければ dismiss にフォールバック
3. **即時適用経路の追加**: TimePickerColorizer / DatePickerColorizer / DatePickerTodayShortcut に、フック待ち (`FragmentLifecycleCallbacks`) とは別の「生成済み View への即時適用」経路を追加

## Non-Goals

- ボトムシート系 (PickerCell / NumberPickerCell / DatePickerCell (Spinner)) — 素の BottomSheetDialog で Activity と共に消滅する現行挙動を維持 (ゾンビ化しないため対象外)
- iOS / MAUI — Android 固有の FragmentManager 問題
- 公開 API の変更 — なし (tag・復元ロジックは internal)。ただし利用者向けに「回転復元には安定 id を推奨」の案内をドキュメント方針として申し送る (docs 書き換え自体は docs-refresh の責務で本変更のスコープ外)
- ダイアログ内の選択中間状態 (回転前に動かした針・選択中の日付等) の保存 — Material 側の saved state に委ねる (本変更で独自の状態保存は追加しない)

## Impact

- 破壊的変更なし。挙動変更は「回転後のゾンビダイアログが、完全復元 (安定 id あり) または dismiss (対応付け不能) になる」のみで、どちらも現状 (ゾンビ) より安全側
- 完全復元が効く条件は「再生成の前後で同じ id であること」。DSL 経路 (`withDSLId` の安定 ID) と明示 id 指定で成立するほか、既定のランダム id でも同一の root / Store インスタンスを再生成後も保持するアプリでは一致する。再生成時に Cell を再構築して id が変わる経路では dismiss フォールバックになる
- リスク: 復元 Fragment の内部状態 (`MaterialTimePicker.hour` 等) への依存は公開 API の範囲内。即時着色は既存 Colorizer の走査ロジックを再利用するため、material 内部依存の増分は限定的
- 復元機構は FragmentManager の saved state 駆動のため、機構上はプロセス再生成でも同経路で動くことが期待されるが、本変更が保証・検証するのは Activity 再生成 (画面回転) のみ (プロセス再生成は非保証)
- 既知の制約: 同一 Activity (= 同一 supportFragmentManager) 内に複数の KsSettingsView が attach されている場合、所有者を一意に特定できないため完全復元は行わず一律 dismiss とする。完全復元が効くのは単独インスタンス構成 (通常の利用形態) のみ。二重発火・誤発火は構造的に発生しない (second-opinion-001 Critical への対処)

## UI アーティファクト

新規 mock は作成しない (新規 UI 要素はなく、視覚基準は既存の通常表示)。免除理由と視覚基準は `ui/brief.md` に記録し、実行時検証規約に基づく修正前後のスクリーンショット証跡は `ui/verification/` に保存する。

## 級: M

複数ファイル横断 (両 ViewHolder・両 Colorizer・TodayShortcut・KsSettingsView) + 新設計 (即時適用経路・one-shot ラッチ) を含むが、公開 API 変更なし・単一 platform のため。

domain: android
