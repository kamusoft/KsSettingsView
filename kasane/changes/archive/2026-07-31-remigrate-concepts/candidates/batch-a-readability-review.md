# Batch A 統合案 初見可読性レビュー

## 総合判定

**修正要求（必須 6 件、推奨 5 件、問題なし 4 件）**

各型の用途・主要な保証・禁止は概ね読める。一方、文書横断で Root Accessory、Diff 適用、独自 Cell 描画、状態連携の説明がつながらず、現状は「概要は説明できるが、責務境界と利用契約を他者へ誤解なく説明するには不足」と判定する。

## 必須

### 1. 相対リンクを実配置に合わせる

対象: 5文書すべて — 本文中の参照と「関連」

現在のリンクはリンク元から見た階層が合わず、文書間を辿れない。`core-model` から `cells` は `../cells/...`、`cells` から `core-model` は `../core-model/...`、同一ディレクトリ内はファイル名だけにする。

### 2. Root Accessory の所有先を明記する

対象:

- `core-model/settings-tree.md` — 「責務境界」「公開 API」
- `core-model/structural-changes.md` — 「Accessory 更新」

`SettingsRoot` は Root Header / Footer を持たない一方、`RootAccessory` とその更新 Diff が存在する。誰が Root Accessory を保持し、`full` と `updateAccessory` がそれぞれどこまで更新するかを明記する。

### 3. `visible projection` を定義する

対象:

- `core-model/settings-tree.md` — 「責務境界」「保証すること」
- `cells/basic-cells.md` — 「保証すること」
- `cells/input-cells.md` — 「目的」

重要な保証に使われる語だが定義がない。何から生成され、誰が保持し、非表示の Section / Cell と元の順序・identity をどう扱う派生データかを最初の出現で説明する。

### 4. 独自 Cell の描画経路を補う

対象:

- `core-model/settings-tree.md` — 「保証すること」「利用例」
- `cells/basic-cells.md` / `cells/input-cells.md` — 「保証すること」

独自 Cell を Root に入れる例はあるが、UI 層がそれを描画する方法がない。標準 Cell の Registry 登録との違い、独自 renderer / 登録の要否、未登録時の挙動を示す。

### 5. Diff の適用前提を具体化する

対象: `core-model/structural-changes.md` — 「責務境界」「identity と内容更新」「Accessory 更新」

操作名は分かるが、Section の指定方法、move の index 基準、対象不在・範囲外・重複 ID の扱いが分からない。各 case の主要 payload、呼び出し側の事前条件、適用側に求める失敗時挙動を示す。`AccessoryTarget` についても全 target、必要 ID、許可される Root / Section payload の組み合わせを明記する。

### 6. 状態連携と Picker 選択 API の用語を解決する

対象:

- `cells/basic-cells.md` — 「目的」「状態所有と callback」「利用例」
- `cells/input-cells.md` — 「目的」「公開 API」「選択と表示値」「してはいけないこと」

「安定 ID の再束縛」「Store 経路」「TwoWay 経路」が未定義であり、基本 Cell の Android 例は callback の説明に対して `MutableState` を直接渡している。経路ごとの ID と状態更新の流れを定義する。また、`PickerCell.selectionMode` は禁止事項で突然現れるため、有効値と `selectedIndex` / `selectedIndices` / callback の対応を説明する。

## 推奨

### 1. 推奨読書順を入口に置く

対象: `core-model/settings-tree.md` — 冒頭または「関連」

設定ツリー、Cell 群、画像、構造変更を読む順序と、独立して読める文書を案内する。

### 2. 値域外挙動を補う

対象:

- `core-model/settings-tree.md` — 「公開 API」
- `cells/input-cells.md` — 「保証すること」

`headerHeight` の `0` / `-1` 未満、`min > max`、範囲外 NumberPicker 値、負の `maxLength` などを、fallback または呼び出し側の事前条件として示す。

### 3. `style` / `CellStyle` の最小限の意味を示す

対象: `core-model/settings-tree.md`、`cells/basic-cells.md`、`cells/input-cells.md` — style に言及する箇所

UI 層の責務であることは分かるが、Cell が持つ `style` が何を選ぶ値なのかは分からない。一文で意味を示すか、詳細は本バッチ外と明記する。

### 4. platform 限定保証を対で示す

対象: `cells/basic-cells.md` — 「保証すること」

選択済み `RadioCell` 再タップ時の Android だけの記述は、iOS の挙動が異なるのか未規定なのか判別できない。差または未規定を明記する。

### 5. Cell 共通契約の正本を決める

対象: `cells/basic-cells.md` / `cells/input-cells.md` — 「目的」「保証すること」

外部状態所有、`isEnabled`、`isVisible`、style、Registry 登録が重複する。共通契約の正本を一方へ寄せ、他方は差分だけにすると読み順と保守性が明確になる。

## 問題なし

### 1. Core と UI の大枠の責務境界

対象: `core-model/settings-tree.md` — 「目的」「責務境界」

Core は内容と順序、UI は描画・Theme・style・具象 Cell とする大枠は明確である。

### 2. Cell の外部状態所有

対象: `cells/basic-cells.md` — 「状態所有と callback」

表示値と callback を分離し、Cell 自身が永続状態を所有しないことは理解しやすい。

### 3. identity 変更と内容更新の区別

対象: `core-model/structural-changes.md` — 「identity と内容更新」

同一 ID の reconfigure、ID 変更時の remove + insert、同一 Section 内だけの move という区別は具体的である。

### 4. `KsImage` の責務と fallback

対象: `cells/ks-image.md` — 全体

UI 層の値であること、解決不能時は icon なしにすること、Android `SystemName` の扱いが一貫している。

## 再レビュー

### 判定

**残存修正あり（必須 2 件、推奨 1 件）**

リンクは concepts ルート相対規約に従う前提として判定対象外とした。前回の必須2〜4、および推奨1・3〜5は初見可読性の観点で解消している。必須5・6と推奨2には、次の残存がある。

### 必須 1. move 系 Diff の payload を操作単位で特定する

対象: `core-model/structural-changes.md` — 「責務境界」「identity と内容更新」

Section 操作は ID または index、Cell 操作は Section ID・Cell ID・index を持つという総論は追加されたが、`moveSection` / `moveCell` が具体的に `ID + 移動先 index` なのか `移動元 index + 移動先 index` なのかは一意に読めない。「移動元 index が有効」という事前条件も、どの case のどの payload を指すか曖昧である。move 2種だけでも payload と index の基準を明記すれば解消する。

### 必須 2. TwoWay 共通契約の適用範囲を限定する

対象: `cells/basic-cells.md` — 「Cell 共通契約」「状態所有と callback」「してはいけないこと」

「宣言 DSL の TwoWay 経路では Binding / MutableState を使う」という全 Cell 共通に見える説明と、「iOS の基本 Cell に Binding initializer はない」、さらに「Android の `SwitchCell` DSL だけは MutableState overload を持つ」という説明が並び、どの Cell / platform に TwoWay 経路があるか初見では確定できない。共通契約を「その overload を持つ Cell に限る」と限定し、基本 Cell と入力 Cell の適用範囲を明示すれば解消する。

### 推奨 1. `step <= 0` の fallback と事前条件の関係を整理する

対象: `cells/input-cells.md` — 「保証すること」

同じ節で「`step <= 0` は 1 へ fallback する」と保証しつつ、「呼び出し側は `step > 0` を指定する」としているため、fallback が依存可能な公開保証なのか、防御的実装だが利用禁止なのかが曖昧である。どちらかに位置付けを揃えるとよい。

### 解消確認

- 前回必須2: Root Accessory の保持者と `full` の範囲が明記され、解消。
- 前回必須3: model と visible projection の定義、順序・ID・hidden 要素の扱いが明記され、解消。
- 前回必須4: 独自 Cell の登録責務と未登録時挙動が両 platform で明記され、解消。
- 前回必須5: 対象不在・範囲外・重複 ID の非保証は解消。move payload の特定だけが残存。
- 前回必須6: ID 再束縛、Store / DSL、Picker mode と state / callback の対応は解消。TwoWay の適用範囲だけが残存。
- 前回推奨1・3〜5: 読書順、style の意味、Radio 再通知差、共通契約の正本化が明記され、解消。
- 前回推奨2: `headerHeight` と入力値域の大半は解消。`step` の位置付けだけが残存。

## 最終確認

### 判定

**残存修正あり（必須 0 件、推奨 1 件）**

リンクは規約上の理由により対象外とした。前回残存した3点の意味上の曖昧さはすべて解消しており、初見読者が責務と利用条件を理解するうえでの必須修正はない。

### 解消確認

- `moveSection(from, to)` と `moveCell(cellID, to)` の payload、hidden 要素を含む基準配列、削除後の挿入 index という解釈が明記され、解消。
- TwoWay は overload がある Cell だけの経路であり、基本7種の platform 別適用範囲と入力5種の適用範囲が明記され、解消。
- `step <= 0` は 1 への fallback を公開保証とする記述に揃い、`step > 0` の事前条件との競合は解消。

### 推奨 1. `step <= 0` の保証を一度にまとめる

対象: `cells/input-cells.md` — 「保証すること」

`NumberPickerCell.step <= 0` を1へ fallback する保証が同じ節に2回記載されている。内容の矛盾はないが、重複を1つにまとめると読みやすい。

## 完了確認

### 判定

**PASS（残存 0 件）**

リンクは規約上の理由により対象外とした。`NumberPickerCell.step <= 0` の保証重複は解消されている。前回までに確認した責務境界、保証、禁止、代表的利用法、用語、文書間の役割と読書順にも再発・新規の初見可読性問題はない。
