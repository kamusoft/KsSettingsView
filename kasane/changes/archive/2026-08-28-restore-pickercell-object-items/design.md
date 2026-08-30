# Design: restore-pickercell-object-items

## Context

方針は [core/ADR-0029](../../decisions/core/0029-pickercell-item-model-with-generic-edge-projection.md) で確定済み: core の候補を `PickerItem` 値型 (text + subText) の列にし、object 射影は API の縁 (iOS ジェネリック init / Android ジェネリック factory / MAUI facade) で受ける。選択の正は index のまま、Registry・equality・bridge は単型で動く。文字列ケースは String 特殊化の簡易形、旧 `displayFormatter` は削除 (未配信のため互換制約なし)。

本書は ADR-0029 の枠内で残る設計判断 — 縁 API の overload 構成・`PickerItem` の形・選択面の副表示・MAUI 射影 API — を確定する。

## Goals / Non-Goals

proposal.md の What Changes / Non-Goals を正とする。要旨: object 候補 + 表示射影 + object 書き戻し + 副表示を 3 プラットフォームで復元する。サマリの並び規則 (`SelectedItemsOrderKey` 等)・`UsePickToClose`・他 Cell への波及は対象外。

## Decisions

### Decision 1: 単一選択の縁は index 経路と object 経路を併設する

**採用案:** ジェネリック縁 (iOS init / Android factory) に2系統の overload を置く。

- **index 経路**: `selectedIndex: Binding<Int?>` / `MutableState<Int?>` (現行形) + 任意の `onItemSelected: (T) -> Void`。型制約なし
- **object 経路**: `selectedItem: Binding<T?>` / `MutableState<T?>`。初期表示の object → index 逆引きに `T: Equatable` (Swift) を要求する (Kotlin は全型が `equals` を持つため制約は現れない)。同値の重複項目は最初の index に解決する
- **String 特殊化**: `T == String` の制約付き overload で `displayText` を省略可 (既定 = 恒等射影)。現行の文字列呼び出しと同じ書き味を保つ
- Store 経路 (非 Binding) は現行どおり index callback (`onSelectionChanged(Int)`) を正とし、縁の overload はそれを包んで組み立てる

**型制約 (iOS)**: 既存の callback (`onSelectionChanged` 等) は `@Sendable` であり、縁の closure は元要素列を捕捉する。Swift 6 の厳格並行性の下で成立させるため、iOS のジェネリック縁は **`T: Sendable`** を、object TwoWay 経路は **`T: Equatable & Sendable`** を要求する。`@unchecked Sendable` による隠蔽は解決策にしない。Kotlin に対応する制約はない。

**公開シグネチャと配置** (実装はこの一覧を正とし、各呼び出し形のコンパイル成立をテストで固定する):

| プラットフォーム / module | 形 |
|---|---|
| iOS (`KsSettingsViewUI`) | 生: `init(items: [PickerItem], selectedIndex:/selectedIndices:)` (Store callback / Binding の両経路) |
| 〃 | ジェネリック Store: `init<T: Sendable>(items: [T], displayText:, subText:, selectedIndex: Int?, onSelectionChanged:, onItemSelected:)` (複数選択は `selectedIndices` + `onMultiSelectionChanged` + `onItemsSelected`) |
| 〃 | ジェネリック DSL: `init<T: Sendable>(items:, displayText:, subText:, selectedIndex: Binding<Int?>, onItemSelected:)` / `init<T: Equatable & Sendable>(items:, ..., selectedItem: Binding<T?>)` / 複数選択 `selectedIndices: Binding<Set<Int>>` + `onItemsSelected` |
| 〃 | String 特殊化: 上記の `T == String` 制約 overload (`displayText` 省略可) |
| Android (`ks-settingsview-ui`) | 生: data class constructor (`items: List<PickerItem>`)。ジェネリック factory 関数 (callback 経路: `selectedIndex: Int?` + `onSelectionChanged` + `onItemSelected` / `selectedIndices` + `onMultiSelectionChanged` + `onItemsSelected`) と String 特殊化 |
| Android (`ks-settingsview-compose`) | `MutableState` 拡張の overload: `selectedIndex: MutableState<Int?>` + `onItemSelected` / `selectedItem: MutableState<T?>` / `selectedIndices: MutableState<Set<Int>>` + `onItemsSelected` (既存の InputCellDsl の分担どおり state 経路は compose module に置く) |

**実行順**: index の書き戻し (binding / state / index callback) が先、object callback (`onItemSelected` / `onItemsSelected`) が後。いずれも確定操作につき1回。

**理由:** object 経路だけだと Equatable を全利用者に強制し、「選択の正は index」の既存契約 (Store 経路・範囲外 index の非正規化) とも非対称になる。index 経路だけだと object 書き戻しの復元という change の目的を満たさない。

**代替案:**
- **A: object 経路のみ** — 却下。`T: Equatable` の強制と、Store 経路 (index が正) との非対称。範囲外 index を保持する既存契約が object 表現で表せない
- **B: index 経路のみ (+ 利用者が自前で逆引き)** — 却下。逆引きの自前実装を強いる現状の不便が残り、復元にならない

### Decision 2: 複数選択の object 受け渡しは callback のみ (object 集合の TwoWay は持たない)

**採用案:** 複数選択は現行の `selectedIndices` TwoWay (`Set<Int>`) を維持し、縁に任意の `onItemsSelected: ([T]) -> Void` (index 昇順) を追加する。object 集合の TwoWay binding (`Binding<Set<T>>` 等) は提供しない。

**理由:** object 集合の TwoWay は `T: Hashable` の強制と、同値重複項目の index 曖昧性 (集合⇄index 集合の往復で情報が落ちる) を持ち込む。確定 callback で選択項目列を受け取れれば実用は足り、必要になれば後から非破壊で追加できる。MAUI の `SelectedItems` TwoWay は facade が `SelectedIndices` からの相互導出で実現し、core には要求しない。

**代替案:**
- **A: `Binding<Set<T>>` を併設** — 却下。`T: Hashable` 強制と重複項目の曖昧性。AiForms の `SelectedItems` も実態はページ離脱時の一括反映で、リアルタイム双方向ではない
- **B: `Binding<[T]>` (順序付き) を併設** — 却下。並びの意味論 (タップ順か index 順か) の規定が必要になり、サマリの並び規則を Non-Goal にした本 change のスコープを超える

### Decision 3: `PickerItem` は公開値型 (text + subText) とし、生の経路も公開する

**採用案:** `PickerItem(text: String, subText: String? = nil)` を Equatable / Hashable (/ Swift は Sendable) な公開値型として追加する。`items: [PickerItem]` を直接渡す生の init / factory も公開し、ジェネリック縁はその糖衣とする。

**理由:** 射影を通さず表示ペアを直接組みたいケース (定数リスト・ローカライズ済み文字列) に自然な最小経路を残す。公開値型にしておけば将来の表示拡張の置き場にもなる。

**代替案:**
- **A: タプル / Pair で表す** — 却下。MAUI・bridge との対応点と将来の拡張点がなく、equality の意図も表現しにくい
- **B: 内部型に隠して縁のみ公開** — 却下。生の経路を塞ぐ理由がない。隠すと MAUI facade からの組み立てにも内部 API が要る

### Decision 4: 副表示は選択面の候補行の2行目として描画し、description 系統の実効値を継承する

**採用案:** `subText` が非 nil の候補行は主表示の下に副表示を持つ2行構成で描画する (iOS `PickerListViewController` / Android `PickerSelectionSheet` の両方)。副表示の文字色・フォントは Cell の **description 系統の実効値** (`CellStyle.descriptionColor / descriptionFont` → `Theme.cellDescriptionColor / cellDescriptionFont`) を、選択面の他の内容と同じく提示経路で1回だけ解決して継承する。行高・Android の折り畳み高さ計算・初期スクロールは2行行高に追随する。全項目が subText なしの選択面は現行と同じ1行構成のまま。

**理由:** 選択面の内容が呼び出し元 Cell の実効 style を継承する既存契約 (picker-selection-surface) の延長に乗る。description 系統は「主文の補足」という意味論が副表示と一致し、両 OS にトークンが実在する (Theme.swift / Theme.kt で確認済み)。

**代替案:**
- **A: 副表示を主表示に連結して1行で表示 (`text — sub`)** — 却下。長い副表示で主表示が読めなくなり、表示能力の復元にならない
- **B: 副表示専用のスタイルトークンを新設** — 却下。トークン追加は Theme / CellStyle / bridge / facade の全層に波及する割に、description 系統との違いを利用者が使い分ける実益がない

### Decision 5: 行の value 自動表示は主表示のみで組み立てる (subText を含めない)

**採用案:** `valueText` 未指定時の自動表示 (単一 = 選択項目、複数 = `, ` 連結) は `PickerItem.text` のみを使う。

**理由:** AiForms も行の value 表示は `DisplayMember` のみで組み立てていた。副表示まで連結すると値表示の役割 (現在値の要約) を超えて行が溢れる。

**代替案:**
- **A: `text (subText)` 形式で連結** — 却下。複数選択の連結でただちに溢れ、要約にならない

### Decision 6: MAUI の射影は `DisplayMember` / `SubDisplayMember` (リフレクション) を正とし、`DisplayFormatter` は削除する

**採用案:** `ItemsSource` を `IList` (object 列) に戻し、射影は AiForms 同型のリフレクション sugar で解決する:

- `DisplayMember` / `SubDisplayMember` (`string?`): 要素の実行時型から単一プロパティ名を解決し、getter delegate を型別にキャッシュする (ドット区切りパス式は対象外)。未指定・未解決時は `ToString()` (副表示は null)
- **リフレクションの意味論** (全入力に対して定義する):
  - 解決対象は **public instance の引数なし readable プロパティ**のみ。該当が無ければ「未解決」として `ToString()` フォールバック
  - プロパティ値が string 以外なら `ToString()` で文字列化。値が null なら主表示は空文字列、副表示は「なし」
  - getter が送出した例外は握りつぶさず伝播する (利用者コードのバグを隠さない)
  - `ItemsSource` の **null 要素は非対応** — 設定時に検出して `ArgumentException` (許すと `SelectedItem == null` が「未選択」と「null 要素の選択」を区別できなくなる)
  - getter の実装は `PropertyInfo` ベース (AOT 環境で動的コード生成に依存しない)。文字列名でしか参照されないプロパティの trimming 保全は**利用者契約**とする — 未保全時は「未解決」経路 (`ToString()` フォールバック) に落ちるため観測可能
- facade が射影を適用して (text, subText) を snapshot / bridge へ輸送する (core の縁 API は MAUI 経路では使わない — 射影済みペアを運ぶだけ)
- `SelectedItem` (`object?`) / `SelectedItems` (`IList?`) を TwoWay で復元する。正は従来どおり `SelectedIndex` / `SelectedIndices` で、相互導出は現行 `SelectedItem` (string) と同じ番人パターンを拡張する。`SelectedItems` は index 昇順
- **逆引きの規則**: 要素 → index は値等価で**最初に一致した index** に解決する (単一・複数とも。core 縁の Decision 1 と同じ規則)。`SelectedItems` に同値要素が複数あっても index 集合上は1つに揃い、直後に正 (`SelectedIndices`) から再導出された `SelectedItems` が公開値になる — 「設定した列がそのまま返る」保証はしない
- **null / 空の正規形**: `SelectedItems = null` と空リストはいずれも「選択なし」(`SelectedIndices` 空) へ揃える。導出方向は、有効な選択が無ければ空リスト (範囲外 index は `SelectedIndices` に保持されたまま導出から除外 — 現行契約の踏襲)
- `DisplayFormatter` (`Func<string, string>`) は削除する

**理由:** MAUI facade の公開面は AiForms 互換が方針 (maui/ADR-0008)。移行者の既存コードがそのまま動く形 (`DisplayMember="Name"`) を正にする。`DisplayFormatter` は AiForms に無い Ks 独自追加で、object 射影の導入で役割が消える。

**代替案:**
- **A: `Func<object, string>` の型付き射影プロパティを併設** — 却下。2射影系統の優先順位規則が要る公開面の重複。必要になれば後から非破壊で追加できる
- **B: ドット区切りパス式のサポート** — 却下。AiForms にも無く、移行に不要。リフレクション解決の複雑化に見合わない
- **C: `SelectionMode` 既定を AiForms の Multiple に合わせ直す** — 却下 (現行 Single 既定を踏襲)。既定値の変更は本 change の復元目的と独立の判断で、現行の移行 Skill も Single 既定で記述済み

### Decision 7: bridge の候補輸送は per-item DTO とし、空文字列の subText は「副表示なし」へ正規化する

**採用案:** bridge DTO は候補1件を1オブジェクト (主表示 `text` + nullable `subText`) で運ぶ per-item 型 (`KsBridgePickerItem` 相当) の列とする。Objective-C / .NET binding では nullable string プロパティとして表現でき、平行配列の件数不一致問題が構造的に存在しない。`subText` の**空文字列は nil / null と同義** — `PickerItem` の縁 (core init / factory / facade 射影) で「なし」へ正規化し、選択面の「subText を持つ行だけ副表示」の判定が sentinel に汚染されないようにする。

**理由:** 平行配列 (`texts` + `subTexts`) は件数不一致・null 要素の binding 表現という2つの不変条件を輸送のたびに守る必要がある。per-item DTO なら不変条件が型で閉じる。

**代替案:**
- **A: 平行配列 + 空文字 sentinel** — 却下。「非 nil なら副表示行」の UI 契約が sentinel の解釈規則に依存し、件数不一致の防御も別途要る
- **B: 平行配列 + nullable 要素** — 却下。Objective-C の `[String?]` は表現できず、iOS 側だけ別構造になる

### Decision 8: 縁と facade は元要素列を設定時に snapshot し、表示と逆引きは同一 snapshot を参照する

**採用案:** ジェネリック縁 (iOS / Android) は構築時に元要素列をコピーして捕捉し、facade は `ItemsSource` 設定時に「元 object の snapshot」と「射影済み (text, subText) の snapshot」を同時に確定する。以後の差し替えまで、表示・逆引き・object callback はこの同一 snapshot を参照する — 元コレクションを in-place 変更しても、確定 callback / `SelectedItem(s)` が「画面に見えていた候補と別の object」を返すことはない。

**理由:** Kotlin `List<T>` / C# `IList` は外部から同じ実体を変更できる。表示は古い射影のまま逆引きだけ現在のリストを参照すると、画面で選んだ項目と返る object がずれる (現行 MAUI 実装は選択解決時に生の `ItemsSource` を参照しており、この経路が残る)。snapshot 統一は「差し替えで反映 (in-place 変更は観測しない)」の既存契約とも一貫し、無関係な snapshot 更新で getter を再評価しない性能意図も担保する。

**代替案:**
- **A: 逆引き時に現在の `ItemsSource` を参照 (現行同型)** — 却下。in-place 変更で表示と callback の対象がずれる
- **B: in-place 変更の観測 (INotifyCollectionChanged 購読)** — 却下。「差し替えで反映」の既存契約の変更であり、本 change の復元目的を超える

## Risks / Trade-offs

- **overload 解決の曖昧性**: String 特殊化とジェネリック縁・生の `[PickerItem]` 経路が並ぶため、Swift の overload 解決 / Kotlin の型推論で意図しない解決が起こり得る。実装時に呼び出し形ごとの解決をテストで固定する
- **Android の折り畳み高さ**: 2行行高で `PickerSelectionSheet` の折り畳み高さ計算・初期スクロールの前提が変わる。subText 混在リスト (一部の行だけ2行) の高さ見積もりに注意
- **equality の変更**: `items` の型変更は snapshot 比較・内容更新の差分検知に直結する。`PickerItem` の Equatable 実装を経て既存の更新経路テストで回帰を確認する
- **リフレクション射影の性能**: 型別キャッシュで緩和。射影は ItemsSource 差し替え時の1回のみ (`PropertyInfo` ベースの呼び出しコストは候補件数に対して線形で、選択リストの規模では問題にならない想定)
- **AOT / trimming**: `PropertyInfo` ベースで AOT の動的コード生成問題は回避するが、利用者モデルのプロパティが trimming で落ちると `ToString()` フォールバックに退化する (クラッシュはしない)。保全は利用者契約とし、Release 構成での実モデル射影の統合検証は初回リリース前の検証 (package-distribution ロードマップの verification) に委ねる

## Migration Plan

未配信のため外部移行はない。リポジトリ内:

1. core (iOS / Android) のモデル・縁 API・選択面 → bridge DTO → MAUI facade → samples の順に追随
2. リポジトリ内の既存呼び出し (samples / tests) の `displayFormatter` 利用を射影へ置換
3. 実装完了後、移行 Skill (api-mapping の PickerCell 行・`SubDisplayMember`「提供しない」の記述) と README 群の追随は docs-refresh で行う (ユーザーの明示依頼)

## Open Questions

なし

## ADR 候補

なし (中核判断は core/ADR-0029 として起票済み。Decision 1〜6 は ADR-0029 の枠内の API 詳細で、蒸留では concepts — input-cells / picker-selection-surface / maui-facade — の改訂で足りる)
