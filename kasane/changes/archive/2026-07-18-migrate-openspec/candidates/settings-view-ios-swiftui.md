# Candidate: settings-view-ios-swiftui

## 概念候補

### SwiftUI 宣言的ラッパ境界 (提案カテゴリ: platforms/)

iOS の既存ホストを SwiftUI から利用できるようにし、ネイティブ描画を重複実装せず SwiftUI 流の宣言記法とライフサイクルへ接続する境界である。

- 一般用途向けの宣言ツリー方式と、大量・高頻度・命令型操作向けの外部 Store 方式を同じ入口から提供する。
- 宣言ツリー方式は View identity が続く間、前回の宣言状態と内部 Store を保持する。外部 Store 方式は呼び出し側が所有する状態をそのまま参照する。
- SwiftUI の `body` 評価では状態変更を起こさず、ネイティブホストの更新ライフサイクルで宣言状態を反映する。
- Root の Header / Footer、描画スタイル、Theme は View 側の指定として扱い、Core の Root モデルへ混在させない。
- コレクション描画、Cell renderer、スタイル解決、Theme のプラットフォーム変換は下位 UI ホスト層の責務であり、この境界は担わない。

出典: `ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift`、`ios/Tests/KsSettingsViewSwiftUITests/KsSettingsViewRepresentableTests.swift`、`ios/Tests/KsSettingsViewSwiftUITests/KsSettingsViewMakeUIViewControllerTests.swift`、`openspec/specs/settings-view-ios-swiftui/spec.md` Purpose、`docs/architecture.md` §1・§4

### 宣言ツリーの安定同一性 (提案カテゴリ: architecture/)

宣言 UI の再評価で Section / Cell の値が作り直されても、画面上の同じ要素を同じものとして追跡するための契約である。

- 一時的なインスタンス ID ではなく、コレクション要素のキー、利用者が与える明示 ID、または構造位置から決定的な ID を導出する。
- 動的な追加・削除・並べ替えが起きる構造では、コレクション要素のキーまたは明示 ID が必要である。位置依存の fallback は静的構造向けであり、位置変化時の同一性を保証しない。
- 同じ意味のヒントは再評価やプロセスごとのランダムシードに左右されず、同じ ID に解決される。
- Cell が ID の再割り当て契約に参加できない場合、安定同一性の確保は利用者側の責務となる。
- 表示属性を変更する modifier は同一性を変えない。

出典: `ios/Sources/KsSettingsViewSwiftUI/DeclarativeDSLIdentity.swift`、`ios/Sources/KsSettingsViewSwiftUI/DSLNodes.swift`、`ios/Tests/KsSettingsViewSwiftUITests/DeclarativeDSLIdentityTests.swift`、`ios/Tests/KsSettingsViewSwiftUITests/KsSettingsViewDSLIntegrationTests.swift`、`openspec/specs/settings-view-ios-swiftui/spec.md` Purpose・「Section / Cell の同一性判定戦略」

### 宣言ツリーの更新分類 (提案カテゴリ: architecture/)

宣言ツリーの変化を、構造・内容・可視性・Theme の異なる更新経路へ分け、不要な再生成と表示のちらつきを避ける契約である。

- 構造変化は ID の集合と順序で判定し、追加・削除・移動として扱う。
- 同一 ID の内容変化は構造を変えず、既存要素の再構成として扱う。連続する内容更新でも識別子は変化しない。
- 可視性変化は通常の内容更新に混ぜず、hidden を含む新しい全モデルから描画用 projection を再構成する。
- Theme は構造差分に含めず、独立した Theme 更新経路へ流す。
- 型消去された任意 View の内部は値比較の対象にせず、同じアクセサリ種別同士の内容差を構造差分として扱わない。

出典: `ios/Sources/KsSettingsViewSwiftUI/DSLDiffCalculator.swift`、`ios/Tests/KsSettingsViewSwiftUITests/DSLDiffCalculatorTests.swift`、`ios/Tests/KsSettingsViewSwiftUITests/DSLVisibilityPreflightTests.swift`、`openspec/specs/settings-view-ios-swiftui/spec.md`「DSL → SettingsRootDiff 算出ロジック」、`docs/architecture.md` §2・§5

## ADR 候補

- SwiftUI 側で独自 renderer を持たず、既存の iOS ホストをアダプタ経由で再利用する — 出典: `openspec/specs/settings-view-ios-swiftui/spec.md` Purpose・「SwiftUI ラッパ KsSettingsView」、選別基準: 覆すコスト高 / コンポーネント境界を越える / 将来を制約する
- 宣言 UI の `body` は副作用なしとし、宣言ツリー評価・差分適用・Store 更新はネイティブホスト更新ライフサイクルで行う — 出典: `ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift`、選別基準: コンポーネント境界を越える / 将来を制約する
- Cell の同一性再割り当て契約は Core、スタイル変更契約は UI 層に置き、UI 層から SwiftUI 層への逆依存を作らない — 出典: `openspec/specs/settings-view-ios-swiftui/spec.md`「SwiftUI DSL」の protocol 配置要件、`ios/Sources/KsSettingsViewSwiftUI/DSLNodes.swift`、選別基準: コンポーネント境界を越える / 将来を制約する
- Theme を Root モデルと構造差分から分離し、Store の独立した Theme 更新経路で反映する — 出典: `openspec/specs/settings-view-ios-swiftui/spec.md`「SwiftUI ラッパ KsSettingsView」「DSL → SettingsRootDiff 算出ロジック」、選別基準: コンポーネント境界を越える / 将来を制約する

## drift 所見

- spec は公開 View 自体が `UIViewControllerRepresentable` に準拠すると記述するが、現行実装の公開 View は通常の SwiftUI `View` であり、内部の Store 用・DSL 用バックエンドがそれぞれ `UIViewControllerRepresentable` を担う。補助資料にも旧構成の記述が残る (`openspec/specs/settings-view-ios-swiftui/spec.md`「SwiftUI ラッパ KsSettingsView」 / `ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift` / `docs/architecture.md` §6)
- spec は DSL init の builder と戻り値を旧来の Section 配列としているが、現行実装は同一性ヒントを保持する中間ノード配列を受ける専用 builder を公開入口に使う (`openspec/specs/settings-view-ios-swiftui/spec.md`「SwiftUI ラッパ KsSettingsView」「SwiftUI DSL」 / `ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift` / `ios/Sources/KsSettingsViewSwiftUI/KsSettingsViewBuilder.swift`)
- spec は独自 `ForEach` の content クロージャに result builder 属性を付けないと明記するが、現行の4オーバーロードはいずれもルート用または Section 内用 builder を付けている (`openspec/specs/settings-view-ios-swiftui/spec.md`「SwiftUI DSL」 / `ios/Sources/KsSettingsViewSwiftUI/ForEachDSL.swift`)
- spec と利用ガイドは ForEach 由来 ID を明示 ID より優先すると記述する一方、現行レジストリは明示 ID が記録済みなら後から付く ForEach ヒントで上書きしない。Purpose の要約は逆に「明示 ID → ForEach → fallback」となっており、spec 内でも優先順位が一貫していない (`openspec/specs/settings-view-ios-swiftui/spec.md` Purpose・「Section / Cell の同一性判定戦略」 / `ios/Sources/KsSettingsViewSwiftUI/DSLHintRegistry.swift` / `docs/platform-guide-ios.md` §6)
- spec は Cell modifier 群をイミュータブルな copy として規定するが、無効化 modifier は現行実装ではフラグを保持せず同じ値を返す no-op である (`openspec/specs/settings-view-ios-swiftui/spec.md`「SwiftUI DSL」 / `ios/Sources/KsSettingsViewSwiftUI/CellModifiers.swift`)
- Section の Header / Footer modifier は現行実装で Section を再構築する際に可視性を引き継がないため、非表示 Section へ適用すると既定の表示状態へ戻る。spec は Section ヘルパの可視性をモデルへ反映することと modifier の copy セマンティクスを要求している (`openspec/specs/settings-view-ios-swiftui/spec.md`「SwiftUI DSL」「SwiftUI DSL における isVisible 引数」 / `ios/Sources/KsSettingsViewSwiftUI/SectionModifiers.swift`)
- spec 後半の共通行レイアウト要件は、Purpose が下位へ分離するとした UICollectionView 描画・Cell renderer・スタイル解決をこの capability に再び含めている。実装の所在も `KsSettingsViewUI` であり、SwiftUI ラッパの責務境界外である (`openspec/specs/settings-view-ios-swiftui/spec.md` Purpose・「共通行レイアウト関数 applyCellBaseLayout」 / `ios/Sources/KsSettingsViewSwiftUI/` / `docs/architecture.md` §1・§3)

## 用語

- 宣言ツリー: SwiftUI の評価ごとに Section と Cell の宣言から構築される、画面状態の値表現。
- 安定同一性: 宣言値が再生成されても、同じ意味の要素へ同じ ID を与える性質。
- 同一性ヒント: コレクション要素のキー、明示 ID、ヘッダ文字列、構造位置など、安定 ID の導出に用いる入力。
- 位置 fallback: 明示的な意味 ID がない場合に、親 ID・並び位置・要素型などの構造情報から ID を導出する方式。
- 構造更新: Section / Cell の ID 集合または順序の変化を反映する更新。
- 内容更新: 同一 ID の要素について表示内容だけを再構成する更新。
- 可視性 preflight: 通常の差分算出前に可視性の変化を検出し、全モデル更新へ切り替える判定。
- Bookkeeper: DSL 方式で内部 Store と前回の解決済み宣言ツリーを View identity の間保持する役割。

## 抽出メモ

- 「宣言ツリーの更新分類」は iOS 固有ではなく `settings-view-android-compose` と共有する上位原則を多く含むため、統合時は `architecture/` の表示状態同期概念への合流候補とする。ただし同一 ID の内容更新経路はプラットフォームごとに異なる。
- 「宣言ツリーの安定同一性」も Android 側の key 戦略と対称であり、共通概念を中心に、SwiftUI 固有のヒント解決を `platforms/` から参照する構成が考えられる。
- Binding Cell の利用意図は spec と補助資料にあるが、今回のコード領域では Binding 自体の所有・書き戻し・debounce の契約を直接検証できない。独立概念にはせず、Cell capability または iOS host 側の抽出結果との統合材料とする。
- `applyCellBaseLayout` 一式はこの candidate へ概念化せず、iOS UI host / Cell layout 側 candidate への移管候補とする。
- ID ヒントの優先順位はコード・spec・Purpose・利用ガイドが不一致であり、統合時に正を決める必要がある。ここでは優先順位を概念候補の不変条件に含めていない。
