# core ADR 一覧

| ID | タイトル | status | 概要 |
|---:|---|---|---|
| [0003](0003-value-oriented-core-model.md) | 値型中心の Core モデルと薄い Cell 抽象 | superseded | ADR-0013により、Cell表現を現行言語機能と外部拡張性へ合わせて置換。 |
| [0004](0004-native-view-rendering-foundation.md) | Native View を描画基盤として宣言 UI から再利用する | accepted | Native のリスト描画基盤を SwiftUI・Compose・MAUI から共有する。 |
| [0005](0005-root-section-accessory-boundary.md) | Root と Section の装飾責務を分離する | accepted | Section 装飾をモデル、Root 装飾を View の責務として分ける。 |
| [0006](0006-structural-diff-ui-store-boundary.md) | 構造 Diff と UI Store で更新責務を分離する | accepted | Core の閉じた Diff と UI 層の Store を更新境界とする。 |
| [0007](0007-declarative-dsl-and-store-convergence.md) | 宣言 DSL と Store API を併存させ同じ更新経路へ収束する | accepted | DSL と Store を用途別に公開し、内部では同じ Diff 適用経路へ流す。 |
| [0008](0008-stable-declarative-tree-identity.md) | 宣言ツリーの安定同一性 | accepted | 動的キー・明示 ID・安定位置から再評価をまたぐ同一性を解決する。 |
| [0009](0009-ui-layer-native-styling.md) | スタイルを UI 層に隔離し Native 型で表現 | accepted | Theme と CellStyle を UI 層へ置き、各プラットフォームの Native 型を使う。 |
| [0010](0010-three-way-display-state-synchronization.md) | 表示状態同期を構造・内容・可視性に分離 | accepted | 構造同期、内容再構成、visible projection 再構築を別経路にする。 |
| [0011](0011-composed-shared-cell-row-layout.md) | Cell 共通行レイアウトをコンポジションで統一 | accepted | 共通行レイアウト関数と accessory slot で Cell の重複を減らす。 |
| [0013](0013-extensible-cell-abstractions.md) | 値型中心のモデルで拡張可能な Cell 抽象を使う | accepted | Swiftのexistential collectionとKotlinの通常interfaceにより、薄いCell契約と外部拡張性を両立する。 |
| [0014](0014-customcell-content-value-with-builder.md) | CustomCell は content 値 + builder クロージャで表現し等価性は content のみに置く | accepted | CustomCell を content 値 + ビルダで表現し、差分検出は content と表示スカラーのみ（関数値除外）。Registry 拡張と KsAnyView 直持ちは不採用。 |
| [0015](0015-customcell-exemption-from-shared-row-layout.md) | CustomCell は共通行レイアウト統一の適用除外とする | superseded | [0022](0022-customcell-lifecycle-delegated-to-platform-adr.md) が supersede。適用除外の決定は 0022 が引き継ぎ、Android lifecycle 機構は android/ADR-0015 へ委譲。 |
| [0016](0016-customcell-type-erasure-vs-generic-representation.md) | CustomCell の内部表現は iOS が型消去内蔵の非ジェネリック struct、Android がジェネリック class | accepted | Registry 解決キーの差 (metatype vs KClass) を吸収し、事前登録なしを単一 register のまま成立させる。 |
| [0017](0017-customcell-disabled-suppression-over-a11y-symmetry.md) | CustomCell の無効化は操作抑止を優先し、Android の読み上げ喪失による非対称を受け入れる | accepted | Compose に「操作のみ無効化」機構がなく二者択一のため、誤操作防止を優先して clearAndSetSemantics で遮断。 |
| [0018](0018-store-dsl-path-result-symmetry.md) | Store と DSL の更新経路間で観測結果の対称性を対称テストで保証する | accepted | Store 経由で反映される変化は DSL 経由でも同じ表示結果に到達すること。実装は縛らず、対称テストの追加を義務化。 |
| [0019](0019-host-restores-from-store-on-attach.md) | Host は view load / attach 時に Store 現在状態から表示を復元する | accepted | 取り付け順序によらず表示が Store 現在状態へ収束する保証。iOS を Android の resync パターンへ対称化。 |
| [0020](0020-store-emits-no-diff-without-state-update.md) | updateAccessory の未知 sectionID は Store で no-op とし、state 更新が成立しない構造 Diff は発行しない | accepted | Store state と発行 Diff の一致保証へ統一。Host の missing ID 検出は内部整合性チェックとして温存。 |
| [0021](0021-header-height-applies-regardless-of-accessory-kind.md) | Section Header の固定高さは accessory 種別に依らず適用する (OS 対称) | accepted | headerHeight の解決を text / view 非依存で統一 (正値固定 clip > Theme > 自動)。固定時は hosted view が領域を占有。原典非準拠を両 OS で受け入れ Android を iOS へ対称化。 |
| [0022](0022-customcell-lifecycle-delegated-to-platform-adr.md) | CustomCell の適用除外を再定義し Android の lifecycle 機構を platform ADR へ委譲 | accepted | ADR-0015 を supersede。適用除外の決定を本文へ引き継ぎ、Android の宣言 UI lifecycle 機構を android/ADR-0015 へ委譲 (iOS は当面 core 規定のまま)。 |
| [0023](0023-accessory-visibility-and-composition.md) | Section Header / Footer の表示は可視トグルと内容有無の AND で判定する | accepted | 可視トグル (bool・既定 true) を追加し「トグル && 内容あり」で判定。内容不在 = nil または空 text を両 OS 対称化、view accessory は常に内容あり、Theme.headerHeight は存在を作らない。 |
| [0024](0024-modern-default-corner-radius-unified.md) | Modern の既定角丸は両 platform で同じ生値 26 に統一する | accepted | Android 既定 12dp→26dp (iOS 26pt は無変更) でクロスプラットフォーム利用者の混乱を解消。margin の既定は引き続き platform 所有 (この判断は [0027](0027-section-margin-defaults-unified-across-style-and-platform.md) が置き換え)。中間値・全既定統一・サンプルのみ調整は却下。 |
| [0025](0025-cell-icon-radius-applies-to-square-frame.md) | Cell icon の角丸は aspect fit の正方形枠に対して適用する | accepted | icon は正方形枠に aspect fit、角丸は枠に対して適用 (描画矩形への追従・aspect fill は却下)。非正方形では角丸が効かないことを契約として明記し、Android を iOS へ対称化。 |
| [0026](0026-main-row-protects-title-truncates-value.md) | 主行の幅配分は title を守り valueText を省略する (両 platform) | accepted | 移植元で逆だった iOS / Android の配分を iOS 側 (title 残り・valueText 省略) へ統一。Android の既定配分を入れ替え、android/ADR-0002 の配分項目を置き換える。iOS を Android へ揃える案・platform 差の容認は却下。 |
| [0027](0027-section-margin-defaults-unified-across-style-and-platform.md) | Section margin の既定は style 間・platform 間とも同値に統一する | accepted | 既定 sectionMargin を両 platform・Classic/Modern とも top 22 / bottom 0 / 水平 16 へ統一 (Classic 実効は上下のみ)。Section H/F の text 間隔 4pt/dp・Root は 0。ADR-0024 の margin 据え置き判断を置き換え。 |
| [0028](0028-timepickercell-is24hour-sole-hour-cycle-source.md) | TimePickerCell の時制は is24Hour を唯一の決定源とし format は表示専用とする | accepted | is24Hour (Bool・既定 true=24h) を3面共通で新設し選択面の時制を一本化。Android の format `a` 判定は撤去、iOS の端末設定依存は廃止。format/フラグの食い違いは利用者責任 (破壊的変更として受容)。 |
| [0029](0029-pickercell-item-model-with-generic-edge-projection.md) | PickerCell の候補は PickerItem 値型列で持ち、object 射影は API の縁のジェネリックで受ける | accepted | 候補を text/subText の PickerItem 列にし、List<T> + 射影 closure をジェネリック init / factory の縁で受けて object 書き戻しも縁で解決。ジェネリック Cell 型・[Any]・facade 限定射影は却下。 |

番号は旧フラット時代の採番を温存 (採番規則は [../index.md](../index.md) を参照)。
