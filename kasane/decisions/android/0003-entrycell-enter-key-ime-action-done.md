# ADR-0003: EntryCell の Enter キーは IME_ACTION_DONE で完了扱いにする

- Status: accepted
- Date: 2026-08-01
- Domain: android
- 関連: [ADR-0001](0001-content-update-preserves-viewholder.md) (EntryCell の IME まわりの既存決定)、[ADR-0002](0002-cell-row-width-allocation-linearlayout-weight.md) (EntryCell の行内配置)

## Context

Android サンプルの「入力 Cell 5 種デモ」で、ニックネーム欄 (リスト最後の EntryCell) に入力してキーボードを閉じようと Enter/✓ キーを押した瞬間にクラッシュする不具合が報告された。実機 (Pixel 6a / Android 16 / Gboard) で再現し、以下のスタックトレースを取得した:

```
java.lang.IllegalStateException: focus search returned a view that wasn't able to take focus!
    at android.widget.TextView.onKeyUp(TextView.java:10065)
```

原因は EntryCell の EditText (`EntryCellViewHolder.create`) が `isSingleLine = true` のみで **`imeOptions` 未指定**なこと。この構成では IME の Enter キーが `KEYCODE_ENTER` として届き、TextView が下方向の次フォーカス先へ移動を試みる。EntryCell がリスト内の最後の入力欄の場合、フォーカス探索がフォーカスを受け取れない View に着地して `requestFocus` が失敗し、フレームワークが throw する。

対照実験で確認した再現条件:

- 下に次の EditText がある EntryCell (名前欄など) で Enter → フォーカス移動が成功し生存
- 下に EditText がない最後の EntryCell (ニックネーム欄) で Enter → 即クラッシュ (2 回再現)

IME の未確定文字列 (composition) は無関係。サンプル固有ではなく、「下に EditText がない位置に EntryCell を置いたリスト」なら利用者アプリでも発生するライブラリ不具合である。

## Decision

`EntryCellViewHolder` が生成する EditText に `imeOptions = EditorInfo.IME_ACTION_DONE` を設定する。Enter キーは「完了」(キーボード閉鎖) として扱われ、クラッシュの根本原因であるフォーカス探索自体が発生しなくなる。iOS の `UITextField` + Done ボタンの挙動とも揃う。

**追補 (2026-08-01, オーナー承認)**: `imeOptions` が塞ぐのは IME のソフトキー経路 (`performEditorAction`) のみで、物理キーボード等の生 `KEYCODE_ENTER` は `TextView.onKeyUp` のフォーカス探索分岐に直接落ちて同一クラッシュが残ることが実機 A/B で判明した (修正後ビルド + `adb shell input keyevent 66` で再現)。このため単一行 EntryCell では生 Enter も「完了」として消費し、フォーカス探索を発生させない。複数行指定時の Enter=改行維持の要求は変わらない。

消費の実装手段は `View.OnKeyListener` とする (`OnEditorActionListener` では不可)。実機検証で、IME 表示中は IME が Enter の DOWN を消費して **UP 単独がアプリに届く**配達パターンが確認され、`TextView.onKeyUp` の `OnEditorActionListener` 呼び出しは DOWN 時消費の印 (`enterDown` フラグ) を前提とするため UP 単独では呼ばれない。`View.OnKeyListener` は `dispatchKeyEvent` で `onKeyUp` より先に無条件で呼ばれるため、この配達パターンでも消費できる。

利用者が `keyboardType` に `TYPE_TEXT_FLAG_MULTI_LINE` を指定した複数行入力では、従来どおり Enter で改行できることを実装時に保証する (シナリオテストで確認する)。

## Alternatives Considered

- **B: IME_ACTION_DONE + editor action で `clearFocus` も行う** — iOS の `resignFirstResponder` 相当までフォーカス喪失を揃える案。クラッシュ解消には A で十分であり、フォーカス喪失に伴う挙動確認コストが上乗せになるため今回は採らない (将来 parity 上の要請が出たら再検討)。
- **C: `nextFocusDown` を自身に向けてフォーカス探索を無効化する** — 症状は消えるが Enter が無反応気味になり、挙動として不自然で説明しにくいため却下。

## Consequences

- 最後の EntryCell で Enter を押してもクラッシュしない。Enter=完了でキーボードが閉じる
- フォーカスは EditText に残る (B 案の clearFocus は行わない)
- 単一行 EntryCell の IME に ✓/完了 アクションが表示されるようになる (従来は改行系表示)
- 複数行指定時の Enter=改行は維持する (実装時のテスト対象)

実装を経て判明した帰結 (出典: 実装結果・実機 A/B):
- 単一行 EntryCell では物理キーボードの Enter による次フィールドへのフォーカス送りが無くなる (Enter は「完了」として消費される。Tab による移動は従来どおり)
- 消費は Enter / NumPad Enter の修飾なし押下に限定しており、Shift+Enter 等の修飾付きや他キーは従来処理のまま
- Robolectric は実機の IME 介在 (DOWN 消費・UP 単独配達) を再現しないため、本決定の検証は実機 A/B が必須だった (`runtime-behavior-verification.md` の完了 3 条件で担保)
