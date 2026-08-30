# レビュー結果: fix-entrycell-enter-focus-crash (001 回目)

**日付**: 2026-08-01
**判定**: APPROVED

## サマリー

ADR-0003 (追補含む) の決定 — 単一行 EntryCell では `imeOptions = IME_ACTION_DONE` で IME ソフトキー経路を、`View.OnKeyListener` で生 `KEYCODE_ENTER` 経路をそれぞれ塞ぎ、複数行では Enter=改行を維持する — が過不足なく実装されている。ViewHolder 再利用経路 (bind 側で常に付け替え / reset 側で解除) も両方から塞がれており、リスナーの持ち越しはシナリオテストで固定されている。ビルド成功・`:ks-settingsview-ui:testDebugUnitTest` 全 green (InputCellsTest 76 件、新規 7 件を含め failures 0)、実機証跡も修正前の FATAL 再現ログと修正後 3 経路の生存ログが揃っており、実行時挙動の検証規約の完了 3 条件を満たす。

Critical / Major はなし。以下は優先度の低い Minor 2 件と Suggestion 2 件。

## 指摘事項

### [🟡 Minor] 生 Enter 経路の「キーボードを閉じる」半分がユニットテストで固定されていない

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCellViewHolder.kt:64-66`

**問題点**: ADR-0003 の Decision は Enter を「完了 (キーボード閉鎖)」として扱うことを求めており、生 Enter 経路ではその閉鎖を `hideSoftInput(v)` が担っている。しかし新規テストが検証しているのは「フォーカスが移らない」「テキストが変わらない」だけで、IME を閉じたかどうかを見ているテストがない。`hideSoftInput(v)` の 1 行を削除しても `:ks-settingsview-ui:testDebugUnitTest` は全 green のままであり、退行を検出できない (IME ソフトキー経路の閉鎖はフレームワークの `IME_ACTION_DONE` 既定処理が担うため、この行はもっぱら生 Enter 経路の挙動を決めている)。

**推奨修正**: `単一行 EntryCell は生の Enter キーでフォーカス探索を起こさない` に、Robolectric の `ShadowInputMethodManager` (`shadowOf(imm).isSoftInputVisible` 等) で Enter 押下後にソフト入力が閉じ要求されたことを確認するアサーションを追加する。テストファイルは既に `shadowOf` を使っているため追加コストは小さい。

### [🟡 Minor] 複数行 + メールアドレス variation の組み合わせに同種のフォーカス探索経路が残る可能性

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCellViewHolder.kt:132-135`

**問題点**: 複数行のときリスナーを外す設計は、「複数行では `TextView` が Enter でフォーカス探索をしない」という前提に依存している。素の複数行 (`TYPE_CLASS_TEXT or TYPE_TEXT_FLAG_MULTI_LINE`) についてはテスト `複数行 EntryCell では Enter が改行として扱われる` が実 `TextView` 上で改行挿入とフォーカス非移動を実証しており前提は成立している。一方 `TextView` の内部判定はメールアドレス variation を例外扱いしており (`TYPE_TEXT_VARIATION_EMAIL_ADDRESS` / `TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS` は複数行でもフォーカス送りの対象になる)、`keyboardType` は任意の `InputType` 定数を受ける公開 API のため `TYPE_CLASS_TEXT or TYPE_TEXT_FLAG_MULTI_LINE or TYPE_TEXT_VARIATION_EMAIL_ADDRESS` を指定した末尾 EntryCell では本 change が塞いだはずのクラッシュが残り得る。

なお本指摘のうち「メール variation が例外扱いされる」部分はフレームワーク内部実装の読解に基づく仮説で、本レビューでは実機・Robolectric いずれでも裏取りできていない (Robolectric の android-all jar は instrumented 版でメソッド本体を確認できず、SDK ソースも未取得)。組み合わせ自体が実用上まれなため優先度は低い。

**推奨修正**: いずれか一方で足りる。(a) 該当の組み合わせを 1 ケース足したテストを書き、フォーカスが移らないことを実 `TextView` 上で確認する (green ならこの指摘は無効と確定でき、以後の前提も固定される)。(b) 赤なら `isMultiLine` の判定から メール variation を除外し、その場合はリスナーを付けたままにする。

### [🔵 Suggestion] KDoc が明言する「修飾キー付き Enter」「NUMPAD_ENTER」の扱いにテストがない

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCellViewHolder.kt:57`, `61-62`

**問題点**: KDoc は「Enter 以外のキーと修飾キー付きの Enter は `false` を返して通常処理へ渡す」と契約を明言し、`KEYCODE_NUMPAD_ENTER` も消費対象に含めている。テストは `KEYCODE_A` が素通りすることのみを確認しており、修飾キー付き Enter (Shift+Enter 等) の素通りと NUMPAD_ENTER の消費は固定されていない。`hasNoModifiers()` の条件はフレームワークのフォーカス探索分岐と一致させた妥当な設計だが、条件を後から緩めても気づけない。

**推奨修正**: `dispatchEnterKey` と同じ形で、`KeyEvent(0, 0, ACTION_UP, KEYCODE_ENTER, 0, KeyEvent.META_SHIFT_ON)` が消費されないこと・`KEYCODE_NUMPAD_ENTER` が消費されることを 1 ケースずつ足す。

### [🔵 Suggestion] EntryCell の Enter 挙動は公開契約の変更であり、蒸留時の concepts 追随を検討すべき

**該当箇所**: `kasane/concepts/core/cells/input-cells.md`

**問題点**: 本修正で Android の `EntryCell` は「Enter で次の入力欄へフォーカスが移る」から「Enter は完了 (キーボード閉鎖) でフォーカスは留まる」へ、利用者から観測できる挙動が変わった (ADR-0003 の Consequences に記載のとおり意図的な変更)。`input-cells.md` の「保証すること」節は `maxLength` / `isPassword` / `isEnabled` の挙動を公開契約として持っており、Enter の扱いも同格の知識になり得る。実装レビューの対象外だが、蒸留フェーズで拾い漏らすと利用者向けの説明が古いまま残る。

**推奨修正**: ksn-distill で `input-cells.md` のプラットフォーム差または保証すること節への追記要否を判断する (ADR-0003 で足りると判断するなら追記不要)。

## 確認した観点

- **仕様充足**: ADR-0003 Decision の 3 要素 (`IME_ACTION_DONE` の設定 / 生 Enter を `View.OnKeyListener` で消費 / 複数行の Enter=改行維持) がすべて実装され、それぞれにテストが対応する。`OnEditorActionListener` ではなく `OnKeyListener` を使うという ADR 追補の指定にも従っている。deviation.md 記載のスコープ拡張以外に無断の逸脱はなし
- **実行時挙動の検証規約**: 修正前の FATAL EXCEPTION 2 経路分 (`before-crash-logcat.txt`)、中間版で生 Enter が残存クラッシュした A/B 証跡 (`interim-*`)、最終版 3 経路の生存ログとスクリーンショットが `evidence/` に揃う。最終スクリーンショットで composing 中の文字列が確定され callback が発火した状態 (「最後のイベント: ニックネーム (callback) → kamu」) まで確認でき、Enter 消費が IME の確定を妨げていないことが読み取れる
- **ViewHolder 再利用 (bind / reset)**: `bind` が毎回 `setOnKeyListener` を単一行/複数行で必ず付け替えるため、ADR-0001 の payload 更新経路 (reset を経ずに再 bind される) でも持ち越しが起きない。`reset` 側の解除と合わせて二重に塞がれており、`EntryCell を単一行と複数行で使い回しても Enter の扱いが混ざらない` が単一行→複数行→単一行の往復を固定している。`imeOptions` が reset の `inputType` 再代入で失われないことも独立したテストで固定済み
- **既存挙動への副作用**: `EditText` 生成箇所は本 ViewHolder の 1 箇所のみで、他 Cell への波及はなし。`:ks-settingsview-ui:testDebugUnitTest` の全テストクラスが failures 0 / errors 0 で、既存の IME 保護テスト (同値再 bind の inputType / hint / filters 非差し替え) も含めて退行なし。Enter でフォーカスが次欄へ移らなくなる点は ADR-0003 Consequences に記載済みの意図的変更
- **コメント規約**: 新規コメントの外部参照は `android/ADR-0003` 形式のみで、変更提案 ID・spec 裸参照・`MUST` 系キーワードの混入なし。触れたファイルに残っていた履歴記述 (「旧実装の…は撤去した」) と change-id 裸参照 (`fix-android-cell-width-allocation`) が同時に解消されている
- **足場の凍結**: exploration.md・ADR-0003 に実装都合の書き換えはなく、スコープ拡張は deviation.md と ADR 追補で記録されている

## アクションプラン

いずれも本サイクルでの必須修正ではない。着手するなら以下の順を推奨する。

1. Minor 2 の (a) — テストを 1 ケース足して仮説を確定させる (赤なら (b) の実装修正へ、green ならこの指摘はクローズ)
2. Minor 1 — 生 Enter 経路のキーボード閉鎖アサーション追加
3. Suggestion 3 — 修飾キー付き Enter / NUMPAD_ENTER のテスト追加
4. Suggestion 4 — 蒸留フェーズへの申し送り
