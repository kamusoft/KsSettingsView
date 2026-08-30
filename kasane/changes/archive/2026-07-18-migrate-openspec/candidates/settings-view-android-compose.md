# Candidate: settings-view-android-compose

## 概念候補

### Android Compose 宣言的ラッパ境界 (提案カテゴリ: platforms/)

Android の既存 Native 設定画面ホストを Compose から利用できるようにし、Native 描画を重複実装せず、Compose のライフサイクルと宣言記法へ接続する境界である。

- 一般用途向けの宣言ツリー方式と、大量・高頻度・命令型操作向けの外部 Store 方式を併存させる。
- 宣言ツリー方式は Compose 上の identity が続く間、内部 Store と前回の宣言状態を保持する。外部 Store 方式では状態の所有権を呼び出し側に残す。
- 宣言ツリーは Recomposition ごとに再評価されるが、描画への反映は Compose と Native View の更新境界から行う。
- Root の Header / Footer、描画スタイル、Theme は画面側の指定として扱い、Core の Root モデルへ混在させない。
- Native リスト、Cell renderer、スタイル解決、表示 projection は下位 UI ホスト層の責務であり、この境界は担わない。

出典: `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposable.kt`、`android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposeTest.kt`、`openspec/specs/settings-view-android-compose/spec.md` Purpose、`docs/architecture.md` §1・§4、`docs/platform-guide-android.md` §2

### 宣言ツリーの安定同一性 (提案カテゴリ: architecture/)

宣言 UI の再評価で Section / Cell の値が作り直されても、画面上の同じ要素を継続して追跡するための契約である。

- 一時インスタンスの識別子ではなく、コレクション要素のキー、利用者が与える明示 ID、または構造位置から決定的な ID を導出する。
- 動的な追加・削除・並べ替えが起きる構造では、コレクション要素のキーまたは明示 ID が必要である。位置依存の fallback は静的構造向けであり、位置変化時の同一性を保証しない。
- 意味的に同じ安定キーは再評価をまたいで同じ ID へ解決され、型の異なる同形のプリミティブ値は区別される。
- 表示属性や現在値の変化は同一性を変えない。
- Cell が ID の再割り当て契約に参加できない場合、安定同一性の確保は利用者側の責務となる。

出典: `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DeclarativeDSLIdentity.kt`、`android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DSLNodes.kt`、`android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/DeclarativeDSLIdentityTest.kt`、`android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/DSLIntegrationTest.kt`、`android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/DSLHandleTest.kt`、`openspec/specs/settings-view-android-compose/spec.md` Purpose・「Section / Cell の同一性判定戦略（Compose）」、`docs/platform-guide-android.md` §5・§6

### 宣言ツリーの更新分類 (提案カテゴリ: architecture/)

宣言ツリーの変化を、構造・内容・可視性・Theme の異なる更新経路へ分け、不要な再生成や表示のちらつきを避ける契約である。

- 構造変化は ID の集合と順序だけで判定し、追加・削除・移動として扱う。
- 同一 ID の内容変化は構造変更へ混ぜず、既存の表示要素を対象とする部分更新として扱う。
- 可視性変化は通常の内容更新に混ぜず、非表示要素を含む新しい全モデルから描画用 projection を再構成する。
- Theme は構造差分に含めず、独立した Theme 更新経路へ流す。同値の Theme は再適用しない。
- 型消去された任意 View の内部は値比較の対象にせず、同じアクセサリ種別同士の内容差を構造差分として扱わない。

出典: `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DSLDiffCalculator.kt`、`android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposable.kt`、`android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/DSLDiffCalculatorTest.kt`、`android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/DSLVisibilityPreflightTest.kt`、`android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposeTest.kt`、`openspec/specs/settings-view-android-compose/spec.md`「DSL → SettingsRootDiff 算出ロジック（Compose）」、`docs/architecture.md` §2・§5

### Compose State と Cell 値の接続境界 (提案カテゴリ: platforms/)

Compose が所有する可変状態を、Native ホストが扱う不変な Cell 値とイベントへ接続する双方向境界である。

- 宣言評価時には State の現在値を Cell の値へ写し、ユーザー操作は Cell が保持するコールバックから State へ書き戻す。Cell 自体は Compose State オブジェクトを状態として所有しない。
- State の変更は Recomposition と新しい値スナップショットを生むが、同じ画面要素の ID は維持され、内容更新経路で反映される。
- State 接続は Cell の同一性採番と独立しており、現在値や State インスタンスの置換によって ID を変えない。
- この境界が担うのは State と値・イベントの相互変換までであり、入力頻度の制御や Native View の即時更新は下位 UI ホスト層の責務である。

出典: `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/BasicCellDsl.kt`、`android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/InputCellDsl.kt`、`android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/BasicCellDslTest.kt`、`android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/InputCellDslTest.kt`、`openspec/specs/settings-view-android-compose/spec.md` Purpose・「DSL での Bindingセル規約（Compose）」

## ADR 候補

- 新規候補なし。Native View の再利用は ADR-0004、DSL と Store の収束は ADR-0007、安定同一性は ADR-0008、Theme の UI 層分離は ADR-0009、更新分類は ADR-0010 として accepted 済み。

## drift 所見

- spec の Purpose は ID ヒントの優先順位を「明示 ID → forEach key → fallback」と要約する一方、Requirements と利用ガイドは「forEach key → 明示 ID → fallback」と記述する。現行実装は宣言内ですでに付いた明示 ID を forEach ヒントで上書きしないため、明示 ID が優先される (`openspec/specs/settings-view-android-compose/spec.md` Purpose・「Section / Cell の同一性判定戦略（Compose）」 / `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DSLScope.kt` / `docs/architecture.md` §6 / `docs/platform-guide-android.md` §6)。
- spec の DSL 方式公開シグネチャは宣言内容の receiver を旧来の値構築用 scope としているが、現行公開 API は同一性ヒントを保持する宣言ツリー専用 scope を受け取る (`openspec/specs/settings-view-android-compose/spec.md`「Compose ラッパ KsSettingsView」 / `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposable.kt`)。
- spec は同一性再割り当て契約とスタイル変更契約をともに Core へ置くよう要求するが、現行コードでは前者だけが Core、後者は UI 層にある。Theme / CellStyle を UI 層へ隔離した現在の依存境界とは、現行配置の方が整合する (`openspec/specs/settings-view-android-compose/spec.md`「Compose DSL」 / `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/DSLCellIdentity.kt` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DSLStyleModifiable.kt`)。
- spec と利用ガイドは無効化 modifier を Cell modifier 群の一つとして示すが、現行の値型版・Handle 版はいずれもフラグを保持せず同じ値を返す no-op であり、Cell の有効状態を変更しない (`openspec/specs/settings-view-android-compose/spec.md`「Compose DSL」 / `docs/platform-guide-android.md` §9 / `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/CellModifiers.kt` / `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DSLHandles.kt`)。
- spec は Binding Cell が Compose State オブジェクトを内部保持すると記述するが、現行 DSL は評価時の値を Cell へ写し、書き戻しコールバックだけが State を捕捉する。また spec は Binding Cell の内部 ID に UUID を使うことを禁止する一方、具象 Cell は UUID 既定値を持ち、DSL 解決時に安定 ID へ再割り当てする。これは同じ spec 内の「具象 Cell の id デフォルト値規約」とも矛盾する (`openspec/specs/settings-view-android-compose/spec.md`「Compose DSL」「DSL での Bindingセル規約（Compose）」 / `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/BasicCellDsl.kt` / `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/InputCellDsl.kt` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SwitchCell.kt` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCell.kt`)。
- spec の State 変更シナリオは同一 ID の内容変化で ReplaceCell Diff を発行すると記述するが、同じ spec の差分要件と現行実装は構造 Diff を空にし、内容更新を専用バッチ経路へ流す (`openspec/specs/settings-view-android-compose/spec.md`「Compose ラッパ KsSettingsView」「DSL → SettingsRootDiff 算出ロジック（Compose）」「DSL での Bindingセル規約（Compose）」 / `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DSLDiffCalculator.kt` / `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposable.kt` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt`)。
- spec は高頻度入力を 200ms debounce 後の専用値更新 API へ流すとするが、その API と debounce 実装は現行 Android コードに存在しない。Compose 層で確認できる契約は値の snapshot とコールバックによる State 書き戻しまでである (`openspec/specs/settings-view-android-compose/spec.md`「DSL での Bindingセル規約（Compose）」 / `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/InputCellDsl.kt` / `android/ks-settingsview-ui/src/main/kotlin/`)。
- 利用ガイドは基本 Cell の Switch / Checkbox / Radio / SimpleCheck が `MutableState<T>` を受けるよう説明するが、現行の基本 Cell DSL で State オブジェクトを直接受ける overload は Switch のみであり、残りは値とコールバックを受ける (`docs/platform-guide-android.md` §4 / `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/BasicCellDsl.kt`)。
- spec 後半の共通行レイアウト要件は、Purpose が下位へ分離するとした RecyclerView 描画・Cell renderer・レイアウト構築をこの capability に再び含めている。実装の所在も UI ホスト層であり、Compose ラッパの責務境界外である (`openspec/specs/settings-view-android-compose/spec.md` Purpose・「共通行レイアウト関数 applyCellBaseLayout（View ベース）」 / `android/ks-settingsview-compose/src/main/kotlin/` / `android/ks-settingsview-ui/src/main/kotlin/` / `docs/architecture.md` §1・§3)。

## 用語

- 宣言ツリー: Compose の評価ごとに Section と Cell の宣言から構築される、画面状態の値表現。
- 安定同一性: 宣言値が再生成されても、同じ意味の要素へ同じ ID を与える性質。
- 同一性ヒント: コレクション要素のキー、明示 ID、ヘッダ文字列、構造位置など、安定 ID の導出に使う入力。
- 位置 fallback: 明示的な意味 ID がない場合に、親 ID・並び位置・要素型などの構造情報から ID を導出する方式。
- 構造更新: Section / Cell の ID 集合または順序の変化を反映する更新。
- 内容更新: 同一 ID の要素について表示内容だけを更新すること。
- 可視性 preflight: 通常の差分算出前に可視性の変化を検出し、全モデル更新へ切り替える判定。
- State snapshot: 宣言評価時点の Compose State の値を、不変な Cell 値へ写したもの。
- Bookkeeper: 宣言ツリー方式で内部 Store、前回の解決済み宣言ツリー、前回の Theme を Compose identity の間保持する役割。

## 抽出メモ

- 「宣言ツリーの更新分類」は `settings-view-ios-swiftui` と共有する上位原則であり、統合時は `architecture/` の表示状態同期概念へ合流する候補とする。ただし同一 ID の内容更新は Android では専用バッチ経路、iOS では再構成用 Diff と、具体経路が異なる。
- 「宣言ツリーの安定同一性」も iOS と対称であり、共通概念を中心に、Compose の key lambda と State 再評価に関する差分を `platforms/` から参照する構成が考えられる。
- 「Compose State と Cell 値の接続境界」は入力 Cell の概念候補とも重なる。統合時は State の所有と書き戻しだけを Compose 側に残し、入力頻度制御や Native イベント処理は Cell / Android host 側へ分けるのが適切である。
- forEach のキーは現在、親 Section や展開位置で名前空間化されず直接 ID ヒントになる。同じキーを複数の展開領域で再利用した場合や、1 item から複数要素を生成した場合の一意性契約はコード・テスト・spec から確定できないため、統合時の要確認事項とする。
- `applyCellBaseLayout` 一式はこの candidate へ概念化せず、Android UI host / Cell layout 側 candidate への移管候補とする。
- ID ヒントの優先順位はコード・spec・Purpose・利用ガイドが一致していないため、統合時に正を確定する必要がある。ここでは優先順位を概念候補の不変条件に含めていない。
