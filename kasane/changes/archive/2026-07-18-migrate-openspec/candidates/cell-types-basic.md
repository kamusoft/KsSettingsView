# Candidate: cell-types-basic

## 概念候補

### 基本設定行の意味と値契約 (提案カテゴリ: cells/)

基本設定行は、設定画面で頻出する「情報を表示する」「処理や遷移を起動する」「明示的な操作ボタンを置く」「二値を切り替える」「チェック状態を切り替える」「グループから一つを選ぶ」「簡易な選択印を切り替える」という意味を、プラットフォーム間で対応する値として表す。モデルは表示時点の内容と状態を保持し、Native control のライフサイクルや画面全体の状態保持は担わない。

不変条件:

- タイトルを中心に、説明、値、アイコン、ヒントを任意の補助内容として持てる。明示的な操作ボタンだけは説明を持たず、移植元の意味的な例外を維持する。
- 二値・チェック・グループ選択の状態はモデル内容であり、値等価に含める。利用者操作を伝える callback は振る舞いであって内容ではないため、値等価と hash から除外する。
- 利用者操作は callback で外部の状態所有者へ通知する。特にグループ選択は各項目が選択集合を変更せず、候補値と共有された現在値から表示状態を導き、現在値の更新は外部の状態所有者が担う。
- チェックボックスと簡易チェックは同じ二値を扱っても、前者は明示的な checkbox control、後者はリスト項目の選択印という異なる意味を持つ。グループ選択の印も簡易チェックと似るが、単一選択という別の状態契約を持つ。
- 無効状態と非表示状態は独立する。無効状態はモデルを残したまま操作と視覚表現を変え、非表示状態はモデルを残したまま表示射影から外す。
- 既定生成では各インスタンスに新しい識別子を与える。宣言 DSL が再評価をまたぐ安定同一性へ再束縛することと、Store 利用者が識別子を明示することは、基本設定行の生成責務とは分離する。

出典:

- コード: `ios/Sources/KsSettingsViewUI/LabelCell.swift`、`ButtonCell.swift`、`CommandCell.swift`、`SwitchCell.swift`、`CheckboxCell.swift`、`RadioCell.swift`、`SimpleCheckCell.swift`、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/` の対応する基本 Cell モデル
- テスト: `ios/Tests/KsSettingsViewUITests/BasicCellsTests.swift`、`UnifyCellCommonFieldsTests.swift`、`android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/BasicCellsTest.kt`、`UnifyCellCommonFieldsTest.kt`
- spec Purpose: `openspec/specs/cell-types-basic/spec.md` の Purpose、各基本 Cell Requirement、「全 Cell 共通の isEnabled」「全 Cell 共通の isVisible」

### Cell 用画像の値境界 (提案カテゴリ: cells/)

Cell のアイコンは、Core の構造モデルではなく UI 層が所有する判別可能な値として扱う。各プラットフォームで自然な画像入力を直接受け取れる一方、Cell の内容比較と宣言 DSL の再評価に参加できる値同一性を提供する。

不変条件:

- 名前やリソース識別子のような記述的な画像入力は、その記述値で等価性を判定する。
- Native 画像オブジェクトは一般的な内容等価を仮定せず、同じインスタンスを保持する場合だけ等価とする。
- プラットフォーム固有の画像入力は UI 境界内に隔離し、Core に画像フレームワークへの依存を持ち込まない。
- 他プラットフォームとの API 対称性のために受理した表現を解決できない場合は、描画失敗や例外ではなく「アイコンなし」へ安全にフォールバックする。
- 画像がない場合は空のアイコン領域を残さず、主要内容を自然な先頭位置へ詰める。

出典:

- コード: `ios/Sources/KsSettingsViewUI/KsImage.swift`、`CellBaseLayout.swift`、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsImage.kt`、`CellBaseLayout.kt`
- テスト: `ios/Tests/KsSettingsViewUITests/BasicCellsTests.swift` の画像解決テスト、`android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/KsImageTest.kt`、`BasicCellsTest.kt` の画像解決テスト
- spec Purpose: `openspec/specs/cell-types-basic/spec.md` の Purpose、「KsImage 値型」

## ADR 候補

- 操作可能な Cell を不変の値モデルとして保ち、利用者操作は callback で外部へ通知し、callback 自体は値等価と hash から除外する — 出典: `openspec/changes/archive/2026-06-03-add-cell-types-basic/design.md` Decision 1・2、各基本 Cell モデル、両 platform の `BasicCellsTest(s)`、選別基準: 能力・コンポーネント境界を越えて影響する、将来の差分・DSL・公開モデル設計を制約する
- グループ単一選択では各項目が共有された現在値を保持して選択表示を導出し、選択値の更新と他項目への伝播は外部の状態所有者へ委ねる — 出典: `openspec/specs/cell-types-basic/spec.md`「RadioCell」、`openspec/changes/archive/2026-06-03-add-cell-types-basic/design.md` Decision 3、両 platform の `RadioCell` と `BasicCellsTest(s)`、選別基準: Cell と画面状態の責務境界を越えて影響する、将来の状態管理方式を制約する
- 操作ボタンは説明文を公開せず、移植元の意味と API 互換性を優先する — 出典: `openspec/specs/cell-types-basic/spec.md`「全 Cell 共通の description / valueText / icon / hintText フィールド」「ButtonCell」の `MUST NOT` と理由、両 platform の `ButtonCell`、`UnifyCellCommonFieldsTest(s)`、選別基準: 両 platform の公開 API を横断し、将来の追加を明示的に制約する

補足: 画像とスタイル値を UI 層へ置く判断は ADR-0009、共通行のコンポジションは ADR-0011、拡張可能な Cell 抽象と Registry 境界は ADR-0004・ADR-0013 に既に収録されているため、新規 ADR 候補には重ねない。

## drift 所見

- [公開契約・確定] `LabelCell` Requirement はアイコンを「URL または論理名」と説明するが、現行の両 platform の画像値はシンボル名、リソース、Native 画像だけを表し、URL 派生も URL 解決経路も持たない (`openspec/specs/cell-types-basic/spec.md`「LabelCell」 / `ios/Sources/KsSettingsViewUI/KsImage.swift` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsImage.kt`)
- [描画経路・確定] spec は iOS の Checkbox と Radio の trailing 表現を `UICellAccessory.customView` ベースで常設すると記すが、現行実装は system accessory を使わず、共通 content stack の trailing view として常設する。見た目の位置固定という不変条件は維持されているが、規定された構成要素が旧実装のままである (`openspec/specs/cell-types-basic/spec.md`「CheckboxCell」「RadioCell」 / `ios/Sources/KsSettingsViewUI/CheckboxCellView.swift`、`RadioCellView.swift`、`SimpleCheckCellView.swift` / `ios/Tests/KsSettingsViewUITests/BasicCellsTests.swift`)
- [描画経路・確定] spec の共通行 Requirement は Android を `RelativeLayout` ベースと記すが、現行コードとテストは programmatic な `ConstraintLayout` を共通行として使用する (`openspec/specs/cell-types-basic/spec.md`「全 Cell 共通の description / valueText / icon / hintText フィールド」 / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellBaseLayout.kt` / `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/UnifyCellCommonFieldsTest.kt`)
- [共通化境界・確定] spec は基本 Cell 7 種がすべて共通行レイアウト関数を経由し、Cell 固有側に共通フィールドの描画を重複させないとする。Android の操作ボタンは補助内容がない場合に共通関数を呼ばず、タイトルと各領域の可視性を ViewHolder 内で直接反映する分岐を持つ (`openspec/specs/cell-types-basic/spec.md`「共通行レイアウト関数経由での描画（全 7 種 Cell に適用）」 / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/ButtonCellViewHolder.kt` / `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/UnifyCellCommonFieldsTest.kt`)
- [操作通知・確定] spec は Radio の Cell タップで選択値を通知するとし、選択済み項目の例外を定めていない。iOS は選択済みかどうかにかかわらず通知する一方、Android は未選択時だけ即時に印を付けて通知し、選択済み項目の再タップを無通知にする (`openspec/specs/cell-types-basic/spec.md`「RadioCell」 / `ios/Sources/KsSettingsViewUI/RadioCellView.swift` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/RadioCellViewHolder.kt` / `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/BasicCellsTest.kt`)

## 用語

- 基本 Cell: 設定画面で頻出する表示、処理起動、ボタン、二値切替、チェック、単一選択を表す UI 層の具象設定行群。
- 表示 Cell: 情報を提示し、設定値を直接変更する操作を持たない行。
- Command: 行タップを処理または遷移の要求として通知し、進行可能であることを trailing indicator で示す行。
- Button: 行全体を明示的な操作として表す設定行。説明文を持たない。
- Checkbox: 独立した二値を角丸四角の control で表す設定行。
- Radio: 共有された現在値と自身の候補値から、グループ内の単一選択を表す設定行。
- Simple check: 独立した選択状態を簡易な checkmark で表す設定行。
- 共通内容: タイトルに加えて任意に保持できる説明、値、アイコン、ヒント。Cell 固有の操作部品は含まない。
- callback: 利用者操作を外部の状態所有者へ伝える振る舞い。Cell の値等価には含めない。
- 値等価: 同じ識別子と表示内容・状態を持つ Cell を同じ値とみなす契約。構造同期の同一性とは別である。
- KsImage: Cell の画像入力を UI 層内で判別可能に運ぶ値。記述的入力と Native 画像入力で等価性規則が異なる。
- trailing control: 行末に置く、Cell 種別固有の表示または操作部品。
- Registry: Cell モデル型と Native 描画型を結び付ける拡張境界。

## 抽出メモ

- 独立概念は 2 候補とした。共通行の視覚配置は ADR-0011 と両 platform の style 候補、Registry は host 候補と ADR-0004・ADR-0013、安定 ID の再束縛は宣言 DSL 候補と ADR-0008、可視性の射影は状態同期概念と ADR-0010 へ合流するのが自然であり、本 candidate では重複候補にしない。
- Cell 用画像の UI 層所有は ADR-0009 の具体化である。独立 ADR ではなく `cells/` の公開契約として残し、Native 型所有の理由は ADR-0009 へリンクする案を提案する。
- 基本 Cell の列挙や initializer signature はコードから再導出できるため、概念候補本文では用途と責務境界だけを記載した。正確な API 一覧や利用例が必要なら `reference` 型として別管理する余地がある。
- Android の操作直後の control 自己更新と iOS の callback 後再描画には実装差がある。Radio の再タップ通知差は spec と直接照合できたため drift に記録したが、Checkbox / SimpleCheck の即時表示差は spec が通知後の再描画までしか明確に規定しないため、推測で drift 件数へ加えていない。
- `docs/cells.md` と platform guide は利用者向け説明として有用だが、SwiftUI Binding overload や Compose DSL signature は別 capability の公開 API であり、本候補へは取り込まない。
