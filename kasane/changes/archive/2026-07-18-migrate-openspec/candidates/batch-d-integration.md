# バッチD 統合案 — 基本 Cell

統合日: 2026-07-18
レビュー結果: 2026-07-18 承認

対象:

- `cell-types-basic`
- 補助資料: `docs/cells.md`、`docs/styling-and-theming.md`、`docs/platform-guide-ios.md`、`docs/platform-guide-android.md`

抽出結果:

- 概念候補: 2件
- ADR候補: 3件
- drift所見: 5件

## 統合後の concepts 案

### `cells/basic-cell-semantics.md`

- 基本 Cell は、表示、処理・遷移の要求、明示的操作、二値切替、チェック、単一選択という設定行の意味をプラットフォーム間で対応させる。
- Cell は表示時点の内容と状態を持つ不変の値モデルであり、利用者操作は callback で外部の状態所有者へ通知する。
- callback は振る舞いであるため値等価と hash から除外し、二値・チェック・選択状態は内容であるため値等価へ含める。
- Radio は候補値と共有された現在値から選択表示を導出し、選択値の更新と他項目への伝播を外部へ委ねる。
- Checkbox、Radio、Simple check は印が似ていても、独立二値、グループ単一選択、簡易な独立選択という異なる意味を持つ。
- Button は移植元との意味互換を保つため description を公開しない。これは全 Cell 共通フィールドに対する公開 API 上の例外である。
- 無効と非表示は独立する。無効は操作・視覚状態、非表示は visible projection の責務として扱う。
- 既定 ID の生成、宣言 DSL の安定 ID 再束縛、Store 利用者の明示 ID は別責務とする。

関連: ADR-0010、ADR-0013、`architecture/declarative-ui-bridge.md`、`styling/cell-visual-states.md`。

### `cells/cell-image-boundary.md`

- Cell の icon は Core ではなく UI 層が所有する判別可能な値として扱う。
- 名前・リソース識別子は記述値で等価とし、Native 画像オブジェクトは参照同一性で等価とする。
- 解決できないプラットフォーム対称用の表現は、例外や描画失敗ではなく「icon なし」へ安全にフォールバックする。
- icon がない、または解決できない場合は空領域を残さない。
- URL は現行の画像値契約に含めない。将来必要なら、解決・取得・キャッシュ・失敗時表示を含む別の能力として設計する。

関連: ADR-0009、`styling/cell-row-layout.md`。

## 既存 concepts / ADR への合流

- 共通行の視覚配置と任意要素の領域除去は `styling/cell-row-layout.md`、共通行のコンポジション判断は ADR-0011 に確定済み。
- 無効状態の操作抑止と表示優先順位は `styling/cell-visual-states.md` に確定済み。
- 可視性を内容更新から分ける原則は ADR-0010 と `architecture/display-state-synchronization.md` に確定済み。
- 宣言 UI の callback から外部状態へ戻す経路は `architecture/declarative-ui-bridge.md` に確定済み。
- 既定 ID と宣言 DSL の再束縛は ADR-0008 と `architecture/declarative-tree-identity.md` に確定済み。
- Cell 抽象を値型中心に保つ判断は ADR-0013、画像を UI 層へ置く判断は ADR-0009 に確定済み。

## 見送る独立 concepts

- 7 種類の initializer signature、既定値、具象 Renderer 名はコードから再導出できるため長命概念へ移さない。
- Switch、Checkbox、Radio などを 1 ファイルずつ分割せず、意味と状態所有の共通契約へ統合する。
- 共通行レイアウト、Registry、宣言 DSL の Binding overload は既存概念または別 capability と重なるため本バッチでは作らない。

## ADR 統合判断

抽出された3候補はいずれも、新規 ADR ではなく既存決定の具体化として扱う。

1. 不変の Cell 値モデルは ADR-0013、内容を値等価に含めながら構造同一性と分離する判断は ADR-0010 に含まれる。callback を等価・hash から除く規則は、この二つを基本 Cell の操作へ適用した公開契約として `basic-cell-semantics.md` に残す。
2. Radio の外部状態所有は、値モデルと callback bridge を単一選択へ適用した Cell 固有契約であり、独立 ADR を必要とする横断的な選択肢ではない。
3. Button が description を持たない規則は移植元との局所的な公開 API 互換であり、ADR ではなく基本 Cell の例外契約として残す。

したがって新規 ADR は起票しない。

## drift 所見の統合

| ID | 所見 | 主な根拠 | 推奨する扱い |
|---|---|---|---|
| D-1 | spec の Label icon は「URL または論理名」と記すが、両 platform の現行画像値に URL 派生・解決経路はない | spec ↔ code/test | 現行コードと ADR-0009 を正とし、URL を長命契約へ含めない。必要なら取得・キャッシュを含む別 change で設計する |
| D-2 | spec は iOS の Checkbox / Radio の trailing 表現を `UICellAccessory.customView` と記すが、現行は共通 content stack の trailing view を使う | spec ↔ code/test / ADR-0011 | 現行の stack 構成を正とする。長命概念には「trailing に常設する」という視覚不変条件だけを残す |
| D-3 | spec は Android 共通行を `RelativeLayout` と記すが、現行は programmatic `ConstraintLayout` を使う | spec ↔ code/test / ADR-0011 | ADR-0011 と現行コードを正とし、旧構成要素名を concepts へ移さない |
| D-4 | Android Button は補助内容がない分岐で共通行関数を通らず、共通フィールドの可視性と title を直接反映する | code ↔ spec / ADR-0011 | accepted ADR との実装 drift。見た目を維持しつつ共通反映経路へ統合する後続 change の不具合候補とする |
| D-5 | Radio の選択済み項目を再タップした場合、iOS は callback を通知するが Android は通知しない | iOS ↔ Android ↔ spec | spec と iOS の「タップを選択要求として通知」を基準に、Android の通知契約を揃える後続 change の不具合候補とする |

## 推奨レビュー結果

1. 新規 2 conceptsを承認する。
2. 新規ADRは起票せず、ADR-0008 / 0009 / 0010 / 0011 / 0013を維持する。
3. D-1〜D-3は推奨どおり現行コードとaccepted ADRを正とする。
4. D-4、D-5は実装不具合候補として移行範囲外に残し、後続Kasane changeへ送る。
