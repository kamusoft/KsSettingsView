# Candidate: settings-view-ios-host

## 概念候補

### iOS Native 設定画面ホスト境界 (提案カテゴリ: platforms/)

iOS の設定画面ホストは、Core が表す設定ツリーと変更意図を、Native のリスト表示へ接続する境界である。UIKit から直接利用できると同時に、SwiftUI や将来の MAUI バインディングから再利用される描画基盤となる。空の設定ツリーも有効な入力として扱い、画面の生成時点から表示可能な空状態を成立させる。

ホストが担うのは、現在のモデル状態の保持、表示対象への射影、Native リストの構造同期、Cell 描画型の解決、画面ライフサイクルに結び付く購読と参照の管理である。設定ツリーの語彙と変更値の定義は Core、宣言ツリーから変更列を算出する責務は SwiftUI ラッパ、Theme と CellStyle の値解決はスタイル境界が担う。Root の Header / Footer と Theme は設定ツリーに含めず、画面側の状態として構造変更とは別経路で反映する。

不変条件は、設定ツリーを外部から直接代入して表示状態を推測更新しないこと、モデル変更は Store または明示的な変更値を通すこと、Theme 更新を構造変更として扱わないこと、長命な Store が画面の寿命を延ばさないことである。

出典: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift`、`ios/Sources/KsSettingsViewUI/SettingsRootStore.swift`、`ios/Tests/KsSettingsViewUITests/KsSettingsViewControllerTests.swift`、`ios/Tests/KsSettingsViewUITests/ApplyDiffTests.swift`、`ios/Tests/KsSettingsViewUITests/MemoryLeakTests.swift`、`openspec/specs/settings-view-ios-host/spec.md` Purpose・「KsSettingsViewController の公開 API」・「メモリリーク防止」、`docs/architecture.md` §1・§4・§6、`docs/platform-guide-ios.md` §11・§12、`kasane/decisions/0004-native-view-rendering-foundation.md`、`kasane/decisions/0006-structural-diff-ui-store-boundary.md`

### iOS のモデル・表示投影・Native snapshot の整合 (提案カテゴリ: architecture/)

iOS ホストは、非表示要素を含む完全なモデル、表示対象だけから成る visible projection、Native リストが保持する snapshot を区別して管理する。完全なモデルは更新対象の探索と位置の意味を保持し、visible projection は index path を使うすべての描画判断の基準となり、snapshot は表示構造と順序だけを表す。

Section と Cell の追加・削除・移動を追跡する同一性には安定した識別子だけを使う。同じ識別子の内容変更では snapshot の集合と順序を変えず、表示中の行を再構成する。可視性の変更は内容変更ではなく projection 上の追加・削除であるため、完全なモデルから表示構造を再構築する。Section 全体の置換も、装飾・高さ・可視性・Cell 集合の複数軸を含み得るため、細粒度の推測差分へ分解しない。

部分変更が持つ位置は、常に非表示要素を含むモデル上の位置を意味する。ホストはその位置を visible projection 上の位置へ変換する。非表示の対象に対する操作はモデルだけを更新し、snapshot 上では正常な no-op とする。これにより、非表示要素は内容と相対位置を失わず、再表示時に更新済みの状態で復帰する。

出典: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift`、`ios/Tests/KsSettingsViewUITests/ApplyDiffTests.swift`、`ios/Tests/KsSettingsViewUITests/DiffableDataSourceTests.swift`、`ios/Tests/KsSettingsViewUITests/VisibilityProjectionTests.swift`、`openspec/specs/settings-view-ios-host/spec.md`「DiffableDataSource」・「visible projection の二重管理」・「部分 Diff の index 規約と hidden 対象の no-op」・「ReplaceCell / ReplaceSection の可視性切替防御」、`docs/architecture.md` §2・§5、`docs/platform-guide-ios.md` §7、`kasane/decisions/0010-three-way-display-state-synchronization.md`

### iOS の observable Store 境界 (提案カテゴリ: platforms/)

iOS の Store は、設定ツリーの現在値を外部へ観測可能にしつつ、変更を明示的な操作へ限定する状態境界である。成功した構造操作は、まず保持状態へ反映され、その操作に対応する変更値を発行する。利用者は Root 全体の差し替えと細粒度操作を用途に応じて選べるが、保持状態へ直接代入することはできない。

Store が担うのは状態保持と変更意図の発行であり、Native snapshot の操作、描画、アニメーション、無効な変更値を直接受け取ったホスト側の診断は担わない。Theme は設定ツリーの構造ではないため独立した観測状態として保持し、構造変更値を発行せずに通知する。同値の Theme 適用は不要な通知を生じさせない。

不変条件は、保持状態と発行した変更値が同じ操作を表すこと、同一識別子への連続した内容更新が識別子のドリフトを起こさないこと、Store が画面より長命でも購読を介した循環参照を作らないことである。

出典: `ios/Sources/KsSettingsViewUI/SettingsRootStore.swift`、`ios/Tests/KsSettingsViewUITests/SettingsRootStoreTests.swift`、`ios/Tests/KsSettingsViewUITests/MemoryLeakTests.swift`、`openspec/specs/settings-view-ios-host/spec.md` Purpose・「SettingsRootStore（iOS）」、`docs/architecture.md` §4・§5、`docs/platform-guide-ios.md` §11、`kasane/decisions/0006-structural-diff-ui-store-boundary.md`、`kasane/decisions/0007-declarative-dsl-and-store-convergence.md`

### iOS Cell 描画の拡張境界 (提案カテゴリ: platforms/)

iOS の Cell 描画は、設定モデルの具象 Cell 型と Native Cell 描画型を登録によって結び付ける。これにより、ホストは具象 Cell の全種類を列挙せず、ライブラリ提供 Cell と利用者定義 Cell を同じ解決経路で扱える。プロセス共通の既定登録を利用できる一方、テストや隔離された構成では独立した登録集合を注入できる。

描画型は、任意の Cell モデルと現在の Theme を受け取って Native Cell を構成する責務を持つ。再利用時には以前のテキスト、画像、サブビュー、イベントハンドラを残してはならない。未登録の Cell は開発時に登録漏れとして顕在化させ、本番では表示全体をクラッシュさせず診断可能な代替表示へ退避する。

不変条件は、登録と解決を並行アクセスから保護すること、モデル型の完全一致で描画型を解決すること、Cell の追加がホスト本体の型分岐追加を要求しないこと、再利用された表示に前のモデルの状態を漏らさないことである。

出典: `ios/Sources/KsSettingsViewUI/KsCellRegistry.swift`、`ios/Sources/KsSettingsViewUI/KsCellRenderer.swift`、`ios/Sources/KsSettingsViewUI/KsListCellBase.swift`、`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift`、`ios/Tests/KsSettingsViewUITests/KsCellRegistryTests.swift`、`ios/Tests/KsSettingsViewUITests/BasicCellsTests.swift`、`ios/Tests/KsSettingsViewUITests/InputCellsTests.swift`、`openspec/specs/settings-view-ios-host/spec.md`「Cell レジストリ」・「KsCellRenderer プロトコル」、`docs/platform-guide-ios.md` §10、`kasane/decisions/0004-native-view-rendering-foundation.md`

## ADR 候補

- 差分アニメーション中は Native リストの layout を同期差し替えせず、最新の visible projection を動的に参照する section provider と、snapshot 適用完了後の layout 無効化によって supplementary 構成を追従させる — 出典: `openspec/specs/settings-view-ios-host/spec.md`「partial Section / UpdateAccessory の supplementary 追従」、`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift`、選別基準: 将来の iOS レイアウト更新方式を制約する。同期差し替えへ戻すと差分アニメーションとの競合による全 Cell バウンドや描画乱れが再発するため、覆すコストも高い。
- その他の新規候補なし。Native リスト基盤と Cell Registry は ADR-0004、Store と明示 Diff の更新境界は ADR-0006、Theme の UI 層分離は ADR-0009、構造・内容・可視性の分離は ADR-0010 として accepted 済み。

## drift 所見

- 画面の Native リストがまだ構築されていない時点で部分 Diff を受け取ると、全体差し替え以外は内部モデルへ適用されず失われる。たとえば Store 接続後から画面ロード前までの間に Section / Cell を追加しても、Store の保持状態は更新される一方、ホストは初期化時に取得した古い Root のまま初回 snapshot を構築する。これは「Store または明示 Diff 経由で内部状態を更新する」「Store の操作を購読して表示へ反映する」という spec の契約を満たさない (`openspec/specs/settings-view-ios-host/spec.md`「KsSettingsViewController の公開 API」・「SettingsRootStore（iOS）」 / `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift` の `applyDiff(_:)`・`updateInternalRoot(for:)`)。

## 用語

- iOS Native 設定画面ホスト: Core の設定ツリーと変更意図を UIKit のリスト表示へ接続し、宣言 UI やバインディングから再利用される画面境界。
- model: 非表示の Section / Cell も含み、更新対象と位置の意味を保持する完全な設定状態。
- visible projection: model から表示対象だけを順序を保って射影した、index path ベースの描画判断に使う派生状態。
- Native snapshot: 表示中の Section / Cell の識別子と順序だけを表し、Native リストへ構造変更を適用する値。
- 内容再構成: 同じ識別子の Cell を構造上は維持したまま、最新モデルと Theme で表示内容を更新すること。
- observable Store: 現在状態を観測可能にし、明示操作による状態更新と対応する変更値の発行を統合する境界。
- Cell Registry: 具象 Cell モデル型から Native 描画型を解決する、利用者拡張可能な登録集合。
- Renderer: Cell モデルと Theme を Native Cell の表示へ反映し、再利用時の状態分離を担う描画契約。

## 抽出メモ

独立概念は「iOS Native 設定画面ホスト境界」「iOS のモデル・表示投影・Native snapshot の整合」「iOS の observable Store 境界」「iOS Cell 描画の拡張境界」の4件を提案する。

「iOS のモデル・表示投影・Native snapshot の整合」は settings-view-core 側の候補「値等価・構造同一性・表示更新の分離」と強く重なる。統合時は共通原則を architecture/ に一度だけ置き、本候補からは iOS がその原則を Native snapshot へ適用するプラットフォーム補足だけを残すのが適切である。「iOS の observable Store 境界」も ADR-0006 / ADR-0007 の現在形であり、Android Store 候補と比較して共通責務を architecture/ へ昇格する余地がある。

レイアウト、罫線、Section Header / Footer の寸法や Theme の具体的な合成規則は隣接する settings-view-ios-style / settings-view-ios-theme-bridge の統合対象とし、本候補には含めない。個々の変更ケース、公開メソッド、登録 API のシグネチャ、Native クラス名の列挙もコードから再導出できるため概念本文には残さない。

UIKit からの直接利用と SwiftUI ラッパからの再利用はコードとテストで確認できた。MAUI バインディングからの直接利用は Purpose と docs に意図として記載されているが、現行の MAUI 実装・テストからは確認できないため、概念候補では将来の利用境界としてのみ扱い、動作保証には含めていない。
