# cell-types-input — デルタスペック (restore-pickercell-object-items)

## MODIFIED Requirements

### Requirement: PickerCell の候補モデル

PickerCell は候補を `PickerItem` (主表示 `text` + 任意の副表示 `subText`) の列として保持する SHALL。候補モデルと選択の正 (単一 `selectedIndex` / 複数 `selectedIndices`) は iOS / Android で同型である SHALL。候補は次のいずれの経路でも構成できる SHALL:

- **生の経路**: `PickerItem` 列を直接渡す
- **ジェネリック縁**: 任意の要素列と射影 (`displayText`、任意の `subText`) を渡す。射影は構築時に1回だけ適用され、要素型は縁の外 (モデル・描画・equality) に現れない
- **String 特殊化**: 文字列列を射影の指定なしで渡せ、恒等射影 (文字列がそのまま主表示・副表示なし) として扱われる

`subText` が空文字列の候補は副表示なしとして扱う SHALL (縁で「なし」へ正規化する)。ジェネリック縁は構築時に元要素列をコピーして捕捉し、object callback の逆引きはこの捕捉列を参照する SHALL — 呼び出し側が元コレクションを後から変更しても、届く要素は構築時点の列のものである。

#### Scenario: 生の PickerItem 列を渡す
- **GIVEN** `subText` を含む `PickerItem` 列
- **WHEN** PickerCell を構築する
- **THEN** その列がそのまま候補になる

#### Scenario: ジェネリック縁の射影
- **GIVEN** 任意の型の要素列と `displayText` / `subText` 射影
- **WHEN** PickerCell を構築する
- **THEN** 各要素が射影の適用結果 (`text` / `subText`) の候補になる

#### Scenario: String 特殊化
- **GIVEN** 文字列列 (射影の指定なし)
- **WHEN** PickerCell を構築する
- **THEN** 各文字列が主表示 (副表示なし) の候補になる

#### Scenario: 空文字列の subText は副表示なし
- **GIVEN** `subText` 射影が空文字列を返す要素
- **WHEN** PickerCell を構築する
- **THEN** その候補は副表示なしとして扱われる

#### Scenario: 元コレクションの変更を観測しない
- **GIVEN** 可変なコレクションからジェネリック縁で構築した PickerCell
- **WHEN** 構築後にそのコレクションを変更してから選択を確定する
- **THEN** object callback には構築時点の列の要素が届く

### Requirement: PickerCell の value 自動表示

`valueText` 未指定のとき、単一選択は選択中候補の `text` を、複数選択は選択中候補の `text` を index 昇順に `, ` で連結した文字列を表示する SHALL。`subText` は自動表示に含めない SHALL。範囲外 index は自動表示から除外する SHALL。

#### Scenario: 複数選択の自動表示は主表示のみ
- **GIVEN** `subText` 付き候補と複数の選択 index
- **WHEN** `valueText` 未指定で行を表示する
- **THEN** 選択中候補の `text` だけが index 昇順に `, ` 連結される

#### Scenario: 範囲外 index の除外
- **GIVEN** 候補数を超える index を含む `selectedIndices`
- **WHEN** `valueText` 未指定で行を表示する
- **THEN** 有効な index の候補だけが表示に使われる

## ADDED Requirements

### Requirement: 単一選択の object 書き戻し

ジェネリック縁は、選択確定時に選択された元要素を受け取る callback (`onItemSelected`) を任意で受け付ける SHALL。また元要素の TwoWay binding (`selectedItem`) の overload を提供する SHALL。TwoWay overload は構築時に要素を候補列から同値で逆引きして選択 index を導出し、同値の重複要素は最初の index に解決する SHALL。候補列に無い要素は未選択として扱う SHALL。TwoWay overload は選択確定時に対応する元要素へ更新される SHALL。iOS のジェネリック縁は要素型に `Sendable` を要求し、TwoWay overload はさらに同値比較 (`Equatable`) を要求する (Kotlin に対応する制約はない)。

#### Scenario: 確定で元要素が届く
- **GIVEN** ジェネリック縁で構築した単一選択 PickerCell と `onItemSelected`
- **WHEN** 選択面で候補を確定する
- **THEN** `onItemSelected` にその index の元要素が1回渡される

#### Scenario: selectedItem の初期逆引き
- **GIVEN** 候補列に含まれる要素を `selectedItem` に指定
- **WHEN** PickerCell を構築する
- **THEN** 選択 index がその要素の位置に解決される

#### Scenario: 同値重複は最初の index
- **GIVEN** 同値の要素が候補列の2箇所にある状態で、その要素を `selectedItem` に指定
- **WHEN** PickerCell を構築する
- **THEN** 選択 index は最初の出現位置に解決される

#### Scenario: 候補に無い要素は未選択
- **GIVEN** 候補列に含まれない要素を `selectedItem` に指定
- **WHEN** PickerCell を構築する
- **THEN** 未選択 (選択 index なし) になる

#### Scenario: selectedItem TwoWay の書き戻し
- **GIVEN** `selectedItem` TwoWay (iOS `Binding<T?>` / Android `MutableState<T?>`) で構築した PickerCell
- **WHEN** 選択面で候補を確定する
- **THEN** binding / state が対応する元要素へ1回更新される

### Requirement: 複数選択の object 受け取り

ジェネリック縁は、複数選択の確定時に選択された元要素の列を index 昇順で受け取る callback (`onItemsSelected`) を任意で受け付ける SHALL。範囲外 index に対応する要素は列に含めない SHALL (index 集合の callback / TwoWay には従来どおり保持される)。元要素の集合の TwoWay binding は提供しない — 複数選択の状態の正は `selectedIndices` である。

#### Scenario: 確定で元要素列が index 昇順で届く
- **GIVEN** ジェネリック縁で構築した複数選択 PickerCell と `onItemsSelected`
- **WHEN** 選択面で複数の候補を確定する
- **THEN** `onItemsSelected` に選択中の元要素が index 昇順の列で1回渡される

#### Scenario: 範囲外 index は元要素列から除外
- **GIVEN** 候補数を超える index を含む `selectedIndices` で構築した PickerCell
- **WHEN** 選択面でそのまま確定する
- **THEN** index 集合の callback は範囲外 index を保持し、`onItemsSelected` の列は有効な index の要素だけを含む

## REMOVED Requirements

### Requirement: PickerCell の項目表示フォーマッタ (displayFormatter)

**Reason**: 項目の表示整形はジェネリック縁の射影 (`displayText`) が吸収した。ライブラリは未配信で互換制約がなく、二重の表示整形 API を残さない (core/ADR-0029)。
