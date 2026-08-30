# Exploration: fix-entrycell-enter-focus-crash

## 課題 / 動機

Android サンプル「入力 Cell 5 種デモ」で、ニックネーム欄 (リスト最後の EntryCell) に入力してキーボードを閉じた瞬間にクラッシュする。実機 (Pixel 6a / Android 16 / Gboard) で再現し、スタックトレースを取得済み:

```
java.lang.IllegalStateException: focus search returned a view that wasn't able to take focus!
    at android.widget.TextView.onKeyUp(TextView.java:10065)
```

- 原因: `EntryCellViewHolder.create` の EditText が `isSingleLine = true` のみで `imeOptions` 未指定。IME の Enter が `KEYCODE_ENTER` として届き、TextView の下方向フォーカス探索が「フォーカスを受け取れない View」に着地して throw
- 再現条件: **下に次の EditText がない EntryCell** で Enter 押下 (対照実験: 名前欄=生存 / ニックネーム欄=即クラッシュ、2 回再現)
- 「キーボードを閉じた瞬間」という報告は Gboard の Enter/✓ 押下 = KEYCODE_ENTER 送出の瞬間だった。IME composition (未確定文字列) は無関係
- サンプル固有ではなく**ライブラリ不具合** (`android/ks-settingsview-ui` の EntryCellViewHolder)

## 検討した選択肢 (却下案と理由を含む)

- **A (採用): EditText に `imeOptions = IME_ACTION_DONE` を設定** — Enter=完了でキーボード閉鎖。フォーカス探索自体が発生しなくなり根本解決。iOS の UITextField + Done と parity が揃う
- **B (却下): A + editor action で `clearFocus`** — resignFirstResponder 相当まで揃うが、クラッシュ解消には A で十分。フォーカス喪失に伴う挙動確認コストが上乗せ
- **C (却下): `nextFocusDown` を自身に向ける** — 症状は消えるが Enter が無反応気味で不自然

## 決定事項

- A 案で修正する (ユーザー確定、2026-08-01)
- 複数行 (`TYPE_TEXT_FLAG_MULTI_LINE`) 指定時は従来どおり Enter で改行できることをテストで保証する

## ADR 候補 (作成済み: android/ADR-0003 (proposed) / 未起票: なし)

## 未決の論点

- 複数行 inputType + IME_ACTION_DONE の組み合わせで IME が改行を出すかは実装時に実機確認が必要 (必要なら単一行時のみ DONE を設定する条件分岐にする)

## UI 素材 (ui/references/ の一覧と注釈)

なし (UI の見た目変更を伴わない)

## 変更級の推奨: S (理由)

ライブラリ 1 ファイル (`EntryCellViewHolder.kt`) + テスト追加のみ。公開 API 変更なし、可逆。デルタスペック不要、ADR-0003 が判断の記録を担う。

## 検証メモ (再現手順)

1. サンプル APK を実機へインストールし「入力 Cell 5 種デモ」を開く
2. ニックネーム (callback) 欄をタップしてフォーカス
3. `adb shell input keyevent 66` (Enter) → 修正前はクラッシュ、修正後はキーボードが閉じて生存すること
4. 対照: 名前欄で Enter → フォーカスがメール欄へ移動する従来挙動が壊れていないこと (A 案では Enter=完了に変わるため、期待挙動は「キーボード閉鎖」に更新される点に注意)
