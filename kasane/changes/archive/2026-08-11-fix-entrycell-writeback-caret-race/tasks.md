# Tasks: fix-entrycell-writeback-caret-race

## 1. bind ガードの実装 (android/ks-settingsview-ui)

- [x] 1.1 EntryCellViewHolder.bind: 同一 Cell (同じ `cell.id`) への再バインドで入力欄が
      フォーカス中なら setText / setSelection をスキップする。同一性判定は `cell.id` で行い、
      equals / 参照比較を使わない (→ Requirement: フォーカス中の EntryCell 入力欄は値の SSoT)
- [x] 1.2 フォーカス喪失時に最後にバインドされた cell.text と入力欄が食い違っていれば
      再同期する。再同期の setText は TextWatcher を発火させない (→ Requirement: フォーカス喪失時の text 再同期と収束)
- [x] 1.3 別 Cell (異なる `cell.id`) への再バインド・非フォーカス時の内容更新では従来どおり
      text を反映する。`reset()` で同一性判定・再同期の保持状態を破棄する
      (→ Requirement: 非フォーカス時と別 Cell 再バインドの text 反映は維持)
- [x] 1.4 プロパティ反映の優先順位を維持する: 表示系は即時 / 入力系 (keyboardType・
      isPassword・maxLength) は現行の同値ガードのまま変化時のみ / isEnabled=false は編集終了
      として blur 規則に従う (→ Requirement: プロパティ反映の優先順位)

## 2. テスト (Robolectric)

- [x] 2.1 フォーカス中の同一 id・異なる text の再バインドで text / キャレットが変わらない
      テスト (stale bind を明示的に挟む決定論的テスト。→ Scenario: フォーカス中のプログラム的更新は入力欄を上書きしない / 高速連続入力の完全性)
- [x] 2.2 フォーカス喪失で最新 cell.text へ再同期し、onTextChanged が発火しないテスト
      (→ Scenario: 保留されたプログラム的更新の反映)
- [x] 2.3 blur 直前入力の保全テスト: 古い bind 値のまま blur → 静穏化後に直前入力が表示・
      通知経路の双方で保たれる (→ Scenario: フォーカス喪失直前の入力の保全)
- [x] 2.4 同一性判定の判別テスト 3 種: 同一 id・異 text は上書きしない / 異なる id・同 text
      は新 Cell として反映する / reset() 後の再利用で前 Cell の状態を持ち越さない
      (→ Scenario: 別 Cell への再バインド / reset 後の再利用)
- [x] 2.5 非フォーカス時の内容更新反映テスト (→ Scenario: 非フォーカスの入力欄への内容更新)
- [x] 2.6 フォーカス中の placeholder 変更が反映され text が保たれるテスト・isEnabled=false で
      編集終了するテスト (→ Scenario: フォーカス中のプロパティ変更 / フォーカス中の無効化)
- [x] 2.7 既存テストの回帰確認 (InputCellsTest / ContentUpdatePayloadTest ほか ui モジュール全緑)

## 3. 検証ツールの強化

- [x] 3.1 repro-burst-loop.sh を判定強化する: FAIL > 0 または有効試行数不足で非ゼロ終了、
      SKIP を有効試行に数えない、実行結果ログの保存に対応する

## 4. 実機検証 (Pixel 6a — Robolectric 緑は実 IME 挙動を保証しない)

合格条件: 各対象で**有効試行 15 回以上・FAIL 0**。結果ログは change 配下に証跡として保存する
(evidence.md から参照)。

- [x] 4.1 repro-burst-loop.sh による MAUI サンプルのバースト入力試験: 有効 15 試行で欠落・
      並び替え 0 (→ Scenario: 高速連続入力の完全性)
- [x] 4.2 同スクリプトによる native サンプルのバースト入力試験: 有効 15 試行で欠落・
      並び替え 0 (→ Scenario: 高速連続入力の完全性)
- [x] 4.3 連続バースト後の入力継続確認 (修正前に MAUI で入力不能化した同一手順で、バースト後
      も入力が受け付けられること。→ Scenario: バースト入力後の入力継続)
- [x] 4.4 日本語 IME の変換中に書き戻しエコーが届いても未確定文字列が維持されることの確認
      (→ Scenario: 日本語 IME 変換中の内容更新エコー)
- [x] 4.5 通常操作の回帰: 低速入力・日本語 IME の変換確定・BackSpace 連続削除・フォーカス
      移動 (fix-maui-entrycell-focus-loss の再発がないこと)
