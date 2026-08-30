# Delta: settings-view-ios-ui (fix-ios-entrycell-writeback-race)

EntryCell の内容更新反映の契約変更。書き戻しラウンドトリップ (打鍵 → `onTextChanged` 通知 → 内容更新に
よる同一 Native cell の再 render) が入力より遅れて到着しても、入力中の `UITextField` を巻き戻さないことを
契約にする。Android 版 (`settings-view-android-ui`、android/ADR-0014) と対称の契約であり、設計判断は
ios/ADR-0004。本変更で規定するのは EntryCell の text 反映・入力継続性の挙動のみ。他 Cell・他プロパティの
既存挙動 (コードとテストが正) は変更しない。

用語:
- 「同一 Cell」とは**同じ安定 `cell.id`** を持つ Cell を指す (Host の item identity と同一)。`EntryCell` の
  `Equatable` は text を含むため、同一性判定に equals / 参照比較を用いてはならない
- 「再 render」とは、同じ `EntryCellView` インスタンスに対して `render(cell:theme:)` が再び呼ばれることを
  指す (Host の内容更新経路 `replaceCell` / `replaceCells` による同一 Native cell の reconfigure)。本契約の
  保証対象はこの経路に限る。Native cell が交換される経路 (`replaceCell` での具象型変更・`replaceSection`・
  `replaceAll` 等の reload) では入力欄自体が作り直されて first responder を失うため、本契約の対象外とする
- 「フォーカス中」とは、EntryCell の `UITextField` が first responder である状態を指す

## ADDED Requirements

### Requirement: フォーカス中の EntryCell 入力欄は値の SSoT

同一 Cell (同じ `cell.id`) の再 render において、EntryCell の入力欄 (`UITextField`) がフォーカス中の間は、
入力欄の text およびキャレット位置を差し替えてはならない (SHALL NOT)。入力中の値の正は入力欄自身であり、
書き戻し経路から返ってくる値 (遅延したエコーを含む) で上書きしない。

#### Scenario: 高速連続入力の完全性

- **GIVEN** フォーカス済みでキャレットが末尾にある EntryCell の入力欄
- **WHEN** 書き戻しの往復より速い間隔で複数文字を連続入力する
- **THEN** すべての文字が入力順どおり反映され、欠落・並び替え・キャレット移動が起きない

#### Scenario: フォーカス中のプログラム的更新は入力欄を上書きしない

- **GIVEN** フォーカス済みの EntryCell の入力欄
- **WHEN** プログラムから同一 Cell の text を別の値に変更し、内容更新の再 render が届く
- **THEN** 入力欄の表示 text とキャレット位置は変わらない (text 以外の変更は「プロパティ反映の優先順位」に
  従い反映されてよい)

### Requirement: フォーカス喪失時の text 再同期と収束

EntryCell の入力欄がフォーカスを失ったとき、最後に render された Cell の text と入力欄の text が食い違って
いれば、入力欄を Cell の text へ再同期しなければならない (SHALL)。この再同期は `onTextChanged` 通知を
発火させてはならない (SHALL NOT) — 再同期値は書き戻し経路へ逆流させない。フォーカス喪失時点で書き戻しの
往復が未完了の入力が存在する場合、静穏化後 (未完了の往復がすべて配信された後) の表示とアプリ状態の双方から
当該入力が失われてはならない (SHALL NOT)。

#### Scenario: 保留されたプログラム的更新の反映

- **GIVEN** フォーカス中にプログラム的な text 変更が保留された EntryCell
- **WHEN** 入力欄がフォーカスを失う
- **THEN** 入力欄は最後に render された Cell の text を表示し、この再同期による `onTextChanged` 通知は
  発火しない

#### Scenario: フォーカス喪失直前の入力の保全

- **GIVEN** 直前の入力の書き戻し往復が未完了 (最後に render された text が入力欄より古い) の EntryCell
- **WHEN** 入力欄がフォーカスを失い、その後未完了の往復が配信されて静穏化する
- **THEN** 表示とアプリ状態の双方が直前の入力を含む最終値に収束し、古い値がアプリ状態へ書き戻されない

### Requirement: 非フォーカス時と別 Cell 再 render の text 反映は維持

フォーカスを持たない入力欄への内容更新は従来どおり text を反映しなければならない (SHALL)。また
`EntryCellView` が別 Cell (異なる `cell.id`) を render される場合は、フォーカス状態や text の一致・不一致に
よらず新しい Cell の text を表示しなければならない (SHALL)。`prepareForReuse` された `EntryCellView` は、
前の Cell に関する同一性判定・再同期の保持状態を持ち越してはならない (SHALL NOT)。

#### Scenario: 非フォーカスの入力欄への内容更新

- **GIVEN** フォーカスを持たない EntryCell の入力欄
- **WHEN** プログラムから Cell の text を変更し、内容更新の再 render が届く
- **THEN** 入力欄は新しい text を表示する

#### Scenario: 別 Cell の render

- **GIVEN** ある EntryCell A を表示中の `EntryCellView`
- **WHEN** 異なる `cell.id` の EntryCell B を render される (text が偶然同じ場合を含む)
- **THEN** 入力欄は B の text を表示し、以後の入力は B の `onTextChanged` へ届く (A の callback には届かない)

#### Scenario: 別 Cell の render 後は新しい Cell が同一性の基準になる

- **GIVEN** フォーカス中の EntryCell A を表示中の `EntryCellView` に、A と同じ text を持つ異なる `cell.id` の
  EntryCell B が render された状態
- **WHEN** 続けて B と同じ `cell.id` で異なる text を持つ Cell が再 render される
- **THEN** フォーカス中ガードが B を同一 Cell として扱い、入力欄の text とキャレットは変わらない

#### Scenario: prepareForReuse 後の再利用

- **GIVEN** `prepareForReuse` された `EntryCellView`
- **WHEN** 新しい EntryCell を render される
- **THEN** 前の Cell の保持状態 (同一性判定・保留中の再同期) の影響を受けず、新しい Cell の text を表示する

### Requirement: プロパティ反映の優先順位

フォーカス中の同一 Cell 再 render における text 以外のプロパティ反映は、次の優先順位に従わなければならない
(SHALL):

1. 表示系プロパティ (placeholder・配色・タイトル等) は従来どおり即時反映する
2. 入力系プロパティ (keyboardType・isPassword・maxLength) は従来どおり反映する。変化時の反映は意図的な
   仕様変更であり、「入力継続性 (IME)」の明示的な例外として IME の未確定文字列の確定を伴ってよい
   (text とキャレットは差し替えない)
3. `isEnabled` の false への変化は編集の意図的な終了であり、フォーカス喪失を経て「フォーカス喪失時の text
   再同期と収束」の規則に従う

#### Scenario: フォーカス中のプロパティ変更

- **GIVEN** フォーカス済みの EntryCell の入力欄
- **WHEN** プログラムから Cell の placeholder を変更し、内容更新の再 render が届く
- **THEN** placeholder の変更は反映され、入力欄の text とキャレットは変わらない

#### Scenario: フォーカス中の無効化

- **GIVEN** フォーカス済みで書き戻し往復が未完了の入力を持つ EntryCell
- **WHEN** プログラムから `isEnabled` を false に変更し、内容更新の再 render が届く
- **THEN** 入力欄は first responder でなくなり、静穏化後に表示とアプリ状態から直前の入力が失われない

### Requirement: 入力継続性 (IME)

同一 Cell の内容更新による再 render (遅延したエコーを含む) の後も、入力欄は入力を受け付け続けなければ
ならない (SHALL)。また同一 Cell の再 render だけを原因として、IME の未確定文字列 (markedText) を確定または
破棄してはならない (SHALL NOT)。入力系プロパティ (keyboardType・isPassword・maxLength) の値の変化を伴う
再 render は「プロパティ反映の優先順位」2. の例外であり、本 Requirement の対象外とする。

#### Scenario: バースト入力後の入力継続

- **GIVEN** 書き戻しの往復より速い連続入力を複数回行った EntryCell の入力欄
- **WHEN** 続けて文字を入力する
- **THEN** 入力は引き続き受け付けられ、反映される

#### Scenario: 日本語 IME 変換中の内容更新エコー

- **GIVEN** 日本語 IME で未確定文字列を変換操作中の EntryCell の入力欄
- **WHEN** 先行入力の書き戻しエコーによる同一 Cell の再 render が届く
- **THEN** 未確定文字列と変換操作は維持される (確定・破棄されない)
