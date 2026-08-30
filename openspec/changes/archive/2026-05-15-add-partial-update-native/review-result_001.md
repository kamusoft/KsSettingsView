# レビュー結果 - add-partial-update-native

**レビュー日時**: 2026年05月14日
**レビュワー**: sdd-reviewer
**変更提案ID**: add-partial-update-native

## サマリー

`add-partial-update-native` は iOS / Android Native UI 層に `SettingsRootStore` ベースの部分更新 API（`applyDiff(_:)`）を導入する大規模な API 刷新提案である。本レビューでは proposal/design/spec の 4 capability 分のドキュメントと、iOS（`SettingsRootStore` / `KsSettingsViewController` / SwiftUI ラッパ）、Android（`SettingsRootStore` / `KsSettingsView` / Compose ラッパ）、両 Sample アプリ、それらのテストを精読した。

### ビルド・テスト結果

- iOS: `swift build` 成功 / `swift test` 成功（78 件、failures=0）
- Android: `./gradlew :ks-settingsview-ui:test :ks-settingsview-compose:test :ks-settingsview-core:test` 成功（テストスイート合計 130+ 件、すべて failures=0 errors=0）
- 主要新規テスト: iOS `SettingsRootStoreTests` / `ApplyDiffTests` / `MemoryLeakTests`、Android `SettingsRootStoreTest`（14 件） / `ApplyDiffTest`（15 件） / `MemoryLeakTest`（2 件）。tasks.md 列挙の全 11 Diff ケースを iOS / Android で網羅。

### 全体所感

- 提案ドキュメント（proposal / design / specs）と実装の対応は概ね正確。タスク完了状況も実装と一致する。
- BREAKING CHANGE（`controller.root` / `view.root` setter 撤廃）は spec.md の MODIFIED 文と一致。
- iOS / Android で API の対称性（メソッド名・Diff ケース一覧）が維持されており、`tasks.md` 13.1 / 13.2 の整合性確認も妥当。
- 一方で、**Store 側のフォールトトレラント挙動と applyDiff のエラーハンドリングが二重発火する設計上の不整合**、**Compose ラッパの `bind(store)` を `AndroidView.factory` で呼ぶことによる Diff 購読の機能停止リスク**、**`notifyDataSetChanged` の濫用による DiffUtil 経路の崩壊リスク**など、複数の Major 級懸念がある。

**最終判定: ❌ CHANGES_REQUESTED**

主要因は Major 指摘 2 件と Minor の累積。Critical 指摘は無し（テスト全通過・ビルド通過）。Sample アプリの目視確認（tasks 13.5 / 13.6）が Headless 環境のため未完了であることも残課題。

---

## 指摘事項

### 🟠 Major: Store の「存在しない ID」操作時に state 変更なしで Diff が発行され、applyDiff 側で `reportMissingID` が誤発火する

**該当箇所**:
- `ios/Sources/KsSettingsViewUI/SettingsRootStore.swift:86-97`（`removeSection`）, `122-131`（`replaceSection`）, `141-159`（`insertCell`）, `164-186`（`removeCell` 経路）, `193-212`（`replaceCell`）, `219-240`（`moveCell`）
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt:78-87`（`removeSection`）, `103-112`（`replaceSection`）, `117-131`（`insertCell`）, `134-149`（`removeCell`）, `152-167`（`replaceCell`）, `170-187`（`moveCell`）

**問題点**:
これらのメソッドは「対象 ID が見つからない場合は state を更新しないが Diff だけ発行する」実装になっている（iOS 例: `removeSection`）。

```swift
public func removeSection(sectionID: UUID) {
    var sections = root.sections
    guard let index = sections.firstIndex(where: { $0.id == sectionID }) else {
        // state を変えずに Diff のみ発行
        diffSubject.send(.removeSection(sectionID: sectionID))
        return
    }
    ...
}
```

Controller / View は Store の Diff Publisher / Flow を購読しており、この空振り Diff を受けると `applyRemoveSection` / `applyRemoveCell` 内の `reportMissingID` / `reportMissingId` が必ず走る。design.md Decision 7 では「DEBUG ビルドでは `assertionFailure` / `error(...)` で即座にクラッシュ」と明記されており、**DEBUG ビルドで Store 経由の no-op 操作を行うと利用者コードが正しいのに UI 層側でクラッシュする**。

設計判断 7 のエラーハンドリングは `applyDiff` を直接呼ぶ使い方（テスト / 低レベル利用）を想定した契約と読めるが、Store 経由でも結果として同じパスが発火するため、Store API の「safe by default」性が失われている。実際 iOS Sample の `removeLastCell()` は「Cell が存在するときのみ削除」というガードで運用しているが、利用者がこのガードを書かない実装パターンを取った場合に DEBUG 落ちする。

**推奨修正**:
以下のいずれかを採用：

1. **Store 側で見つからなければ Diff を発行しない（推奨）**:
   ```swift
   public func removeSection(sectionID: UUID) {
       var sections = root.sections
       guard let index = sections.firstIndex(where: { $0.id == sectionID }) else { return }
       sections.remove(at: index)
       root = SettingsRoot(sections: sections, theme: root.theme)
       diffSubject.send(.removeSection(sectionID: sectionID))
   }
   ```
   - Store と state の整合性を必ず保つ。
   - applyDiff 側のエラーハンドリングは「applyDiff を直接呼ぶ低レベル利用」のセーフネットとして残す。

2. **applyDiff 側で Store 発の Diff と直接呼び出しを区別する API を新設**（複雑化するため非推奨）。

採用案 1 の場合、`ApplyDiffTests`（iOS）/`ApplyDiffTest`（Android）にある「存在しない ID」テストは applyDiff 直接呼び出しの形なので変更不要。`SettingsRootStoreTests` / `SettingsRootStoreTest` には「存在しない ID を渡しても Diff が発行されない」テストを追加する。

---

### 🟠 Major: Compose ラッパの `AndroidView.factory` で `bind(store)` を呼ぶ実装は、`findViewTreeLifecycleOwner()` が `null` を返すケースで Diff 購読が確立されない

**該当箇所**: `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposable.kt:54-70`、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:163-189`

**問題点**:
`KsSettingsView.bind(store)` は `findViewTreeLifecycleOwner()` を呼び、`null` の場合は `Log.w` を出すだけで `storeCollectJob` を張らない。Compose の `AndroidView.factory` は View をインスタンス化する時点で呼ばれるが、その時点で View はまだ Window に attach されていないため、`findViewTreeLifecycleOwner()` は `null` を返す可能性が高い（ComposeView は Window attach 後に Lifecycle owner を伝播する）。

実装側コメント（`KsSettingsView.kt:179-188`）でも「LifecycleOwner が見つからない場合は collect は次回 attach 時には張られない」と明記されており、その場合「利用者は本メソッドを attach 後または LifecycleOwner が利用可能な状況で呼ぶ必要がある」とある。しかし Compose ラッパの実装はその制約を満たしていない。

`spec.md`（settings-view-android-ui）の「Compose からの利用」Scenario は明確に **`AndroidView.factory` で `view.bind(store)` が呼ばれて Store の Diff Flow が購読される** ことを要求している。実装で購読が確立されなければ、これは spec 違反のリスクが高い（Robolectric テストでは collect が実際に張られていないことが `KsSettingsViewComposeTest` のコメントから読み取れる）。

仮に実機 Compose では Window attach 直前に factory が動き、attach の瞬間に `findViewTreeLifecycleOwner()` が値を返す（厳密には setContent 配下では `LocalLifecycleOwner` を経由して伝播されており、`ViewTreeLifecycleOwner.set` が直近の View へ事前に設定される）として動作するかもしれないが、Compose のバージョンや内部実装次第で挙動が変わる。少なくとも Robolectric テスト上で「実プロダクトでは AndroidView 内で bind(store) を呼ぶ」というコメントを書いた上でテストでは bind 経路を回避している事実は、設計上の脆弱性を示唆している。

**推奨修正**:

- Compose ラッパ側で `LocalLifecycleOwner.current` を取得し、`DisposableEffect`（または `LaunchedEffect`）で `store.diffs.collect { view.applyDiff(it) }` を直接購読する。`view.bind(store)` は初期 state 反映だけを行うようにシグネチャを変える（あるいは LifecycleOwner を引数で受ける `bind(store, lifecycleOwner)` を追加）。

  ```kotlin
  @Composable
  fun KsSettingsView(store: SettingsRootStore, ...) {
      val lifecycleOwner = LocalLifecycleOwner.current
      var viewRef by remember { mutableStateOf<KsSettingsViewLayout?>(null) }
      AndroidView(
          factory = { ctx ->
              KsSettingsViewLayout(ctx).apply {
                  this.style = style
                  setRootDirect(store.state.value)
              }.also { viewRef = it }
          },
          update = { v -> /* style / header / footer 反映 */ }
      )
      LaunchedEffect(store, viewRef) {
          val v = viewRef ?: return@LaunchedEffect
          store.diffs.collect { v.applyDiff(it) }
      }
  }
  ```

- もしくは `KsSettingsView` View 側で `onAttachedToWindow` をオーバーライドし、「pending Store があれば attach 時に bind し直す」自動リトライ機構を実装する。

修正後は `KsSettingsViewComposeTest` で「Compose で `store.insertCell(...)` を呼び、内部 `mainListAdapter.itemCount` が更新される」フローを検証するテストを追加すべき。

---

### 🟡 Minor: `applyUpdateTheme`（Android）が `notifyDataSetChanged()` を 3 adapter 分呼び、DiffUtil 経路を無効化している

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:336-351`

**問題点**:
`UpdateTheme` Diff の適用で `mainListAdapter.notifyDataSetChanged()` / `headerAdapter.notifyDataSetChanged()` / `footerAdapter.notifyDataSetChanged()` を呼んでいる。これは以下の問題がある：

1. `notifyDataSetChanged()` は Android Lint 警告（`NotifyDataSetChanged`）を出すケースが多く、`@SuppressLint` 無しでコンパイル通過しているのは Lint 設定の都合の可能性が高い（`ks-settingsview-ui` モジュールの Lint 設定確認推奨）。
2. spec.md「Theme 更新」Scenario の MUST 文は「すべての可視 Cell の bind が新 Theme で再呼び出しされる」のみであり手段は限定されないが、DiffUtil の利点（最小範囲再描画 / アニメーション維持）を捨てている。
3. ListAdapter の `submitList(new list)` で list 参照を差し替えれば DiffUtil が走り、各 ViewHolder の bind が呼び出される。Theme が `Cell` モデルに含まれない設計のため、リスト参照を変えるだけでは DiffUtil は no-op になる可能性があるが、`KsSettingsListAdapter` 側に `theme` フィールドを持たせて `payload` 付きの `notifyItemRangeChanged(0, itemCount, "theme")` を発行する設計のほうが Material 推奨パターンに沿う。

**推奨修正**:

- `KsSettingsListAdapter` に `theme` を持たせ、theme 変更時は `notifyItemRangeChanged(0, itemCount, payload = THEME_CHANGED_PAYLOAD)` を発行。`onBindViewHolder(holder, position, payloads)` で `THEME_CHANGED_PAYLOAD` を受けた場合のみ theme 関連プロパティを再適用、それ以外は通常通り。
- 同様に `RootHeaderFooterAdapter` も `notifyItemChanged(0, payload = THEME_CHANGED_PAYLOAD)` で対応。
- 現状の実装が許容されるなら、せめて `@SuppressLint("NotifyDataSetChanged")` で意図を明示し、コメントに「Theme は DiffUtil が拾えないため全件 reload」と説明を残す。

---

### 🟡 Minor: iOS `applyDiff(.full(_))` の経路で `applyFullSnapshot` が `animated: true` で apply するが、結果としてアニメーションは無いまま全件入れ替えに近い動作になる

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:514-515`, `559-585`

**問題点**:
`applyDiff(.full(newRoot))` は `applyFullSnapshot(root: newRoot, animated: true)` を呼ぶ。`SettingsRootStore.replaceAll(_:)` 経由でこれが呼ばれた場合、内部 `sectionIndex` / `cellIndex` を完全に作り直して新スナップショットを apply する。これ自体は spec の「Theme 更新」「`.full` ケース」要件を満たすが、新 root と旧 root で同一 `Section.id` / `KsCellID` の Cell があった場合に reloadItems が走らないため、Cell の中身変化（例: タイトル文字列）が UI に反映されない可能性がある（snapshot 上は同じ identifier のため DiffableDataSource は更新を検出しない）。

`updateTheme` ケースで `reloadItems(snapshot.itemIdentifiers)` を入れているのに対し、`applyFullSnapshot` ではこれが無いのは整合性の点で気になる。

**推奨修正**:
`applyFullSnapshot` で apply 後に必要に応じて `reloadItems(snapshot.itemIdentifiers)` も実行する。あるいは `replaceAll` のユースケースは「全体差し替え」なので使用頻度が低く、利用者は `replaceCell` を併用すべきというルールをドキュメント化する。

---

### 🟡 Minor: iOS `rootHeader` / `rootFooter` setter で `rebuildLayout()` 呼び出しが頻発し、`applyFullSnapshot(animated: false)` が無アニメーションで毎回走る

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:62-87`, `280-285`

**問題点**:
`rootHeader` / `rootFooter` が `nil` ⇔ 非 `nil` の切り替えで `rebuildLayout()` を呼ぶ実装になっている。`rebuildLayout` は `setCollectionViewLayout(newLayout, animated: false)` の後で `applyFullSnapshot(root, animated: false)` を実行する。これにより：

- Section H/F や Cell が瞬間的に reload される（無アニメーション）
- スクロール位置がリセットされる可能性
- 大量データ時のパフォーマンス劣化

SwiftUI の `.header(...)` modifier が `nil` ⇔ `.text("...")` を頻繁に切り替えるパターン（例: 状態に応じて表示）で UX 劣化が懸念される。

**推奨修正**:
- `rootHeader` / `rootFooter` setter で `nil` ⇔ 非 `nil` の切り替え時は、現在の snapshot を保持して boundary supplementary だけを reconfigure する API（`UICollectionViewCompositionalLayout.configuration` を差し替え）を検討。難しい場合はドキュメントに「Root H/F の頻繁な切り替えはコストが高い」と注記。

---

### 🟡 Minor: Sample アプリ（iOS / Android）で `KsCellRegistry.register` が呼ばれていない or 自動登録に依存しすぎている

**該当箇所**:
- iOS: `samples/ios/KsSettingsViewSample/ContentView.swift` 全体（SampleLabelCell の register が `SampleLabelCellPreviewRegistration` 経由のみ）
- Android: `samples/android/app/src/main/kotlin/.../MainActivity.kt:52-59`

**問題点**:
- iOS Sample では `SampleLabelCell` の Renderer 登録が `#Preview` ブロックの `SampleLabelCellPreviewRegistration.registerOnce` 経由でしかない。実機起動時にこの static evaluation が呼ばれない可能性があり、起動時の Cell 描画が placeholder に落ちるリスクがある。Sample 起動時に `KsCellRegistry.shared.register(...)` を確実に呼ぶ初期化点（`@main App` の `init()` など）が必要。
- Android Sample は `MainActivity.onCreate` で `KsCellRegistry.register` を呼んでおり問題なし。iOS のみ非対称。

**推奨修正**:
iOS Sample の `KsSettingsViewSampleApp.swift`（または相当）に以下を追加：

```swift
@main
struct KsSettingsViewSampleApp: App {
    init() {
        KsCellRegistry.shared.register(
            cellType: SampleLabelCell.self,
            rendererType: SampleLabelCellView.self
        )
    }
    var body: some Scene { WindowGroup { ContentView() } }
}
```

（実機目視確認 tasks.md 13.5 が未完了なため、本問題は実機テストで検出される可能性大）

---

### 🟡 Minor: SwiftUI ラッパの `applyUpdate` が `store` 自体の差し替えに対応していない

**該当箇所**: `ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift:94-107`

**問題点**:
`KsSettingsView` 構造体は `let store: SettingsRootStore` を持ち、`makeUIViewController` で `KsSettingsViewController(store: store, ...)` を作る。`updateUIViewController` 内では `style` / `rootHeader` / `rootFooter` の更新のみで、`store` 自体が別インスタンスに差し替わったケースを検出していない。

SwiftUI 利用パターンとして `@StateObject` で Store を保持する場合は store の同一性が保たれるため通常問題ないが、`@ObservedObject` でリビルドのたびに別 Store を渡してしまうケースで Controller 側の `storeSubscription` が旧 Store を購読し続けるバグが発生し得る。

**推奨修正**:
- Controller に「Store 再接続」API（例: `internal func reconnectStore(_ store: SettingsRootStore)`）を追加し、`applyUpdate` 内で `if controller の現 Store != view.store` のときに再接続する。ただし Controller 側に Store の参照を保持する必要があり、循環参照防止の検討が必要。
- もしくはドキュメントで「`KsSettingsView` は `store` の差し替えをサポートしない。`@StateObject` での保持を推奨」と明記。

---

### 🟡 Minor: メモリリークテストが「Store の Diff Publisher 購読の解除」を直接検証していない

**該当箇所**:
- `ios/Tests/KsSettingsViewUITests/MemoryLeakTests.swift:34-58`
- `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/MemoryLeakTest.kt:38-57`

**問題点**:
spec.md「Store 購読の解除」Scenario は「Store の Diff Publisher 購読は解除され、Controller への参照が残らない（Store が長命であっても Controller がリークしない）」を要求している。

- iOS のテスト `test_Store経由でもControllerがdeinitされStore購読が解除される` は Controller の弱参照解放のみ検証し、「Controller deinit 後に `store.insertCell(...)` を呼んでもクラッシュしない」までは確認している（良い）。ただし `storeSubscription` の Cancellable が確実に cancel されたかの直接確認はない（Combine の Subject に subscriber が残らないことの検証）。
- Android の `Store 経由で setRootDirect しても detach 後 adapter が null になる` テストは `bind(store)` 経由でなく `setRootDirect` を使うため、`storeCollectJob` の cancel 検証になっていない（コメントで Robolectric 制約を認めている）。

**推奨修正**:
- iOS: `store.diffSubject` の subscriber 数を直接確認できれば理想だが、Combine では困難。代わりに「Controller deinit 後、Store のメソッドを N 回呼んだ後の Controller への参照は依然 nil」を確認する追加テストを書く。
- Android: `bind(store)` を呼んだ後の `storeCollectJob` を internal 公開し（テストフレンドリーアクセサ）、`onDetachedFromWindow` 後に `isCancelled == true` を直接検証するテストを追加する。LifecycleOwner が必要なら `androidx.lifecycle.testing` の `TestLifecycleOwner` を使う。

---

### 🟡 Minor: `tasks.md` 13.5 / 13.6（Sample 実機目視確認）が未完了のまま放置されており、archive 前のリスクとして残る

**該当箇所**: `openspec/changes/add-partial-update-native/tasks.md:174-175`

**問題点**:
`tasks.md` の完了条件は「全タスクのチェックボックスが完了している」となっており、13.5 / 13.6 が未チェックの状態。注釈で「Headless 環境では未実施」と書かれているが、archive 時には実機 / シミュレータ / エミュレータでの目視確認結果が必要。

**推奨修正**:
- 実機目視確認を実施し、両方のチェックを付ける。
- もしくはタスクを明示的に「以降のレビューフェーズで実施」とラベリングして archive 条件から外す変更提案ドキュメント修正を行う（ただしこれは spec 変更扱いとなるためレビュアー権限外）。
- レビュアーとしては「実機検証なしの archive を非推奨」とする。

---

### 🔵 Suggestion: iOS `KsSettingsViewController.deinit` の整理

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:162-173`

**問題点（提案レベル）**:
`deinit` で `dataSource = nil` を代入しているが、`UICollectionViewDiffableDataSource` は `collectionView.dataSource` の retain を持つため、`cv.dataSource = nil` を先に行わないと dataSource を nil 代入してもメモリ解放されないケースがある。現在の実装順序は良好だが、`dataSource = nil` のコメントを加えると意図が明確になる。

**推奨修正**:
コメント追加のみ。

```swift
deinit {
    storeSubscription?.cancel()
    storeSubscription = nil
    if let cv = self.collectionView {
        // collectionView が dataSource を retain しているため、先に解除
        cv.dataSource = nil
        cv.delegate = nil
    }
    // DataSource は内部で collectionView を強参照する可能性があるため明示解放
    self.dataSource = nil
    self.sectionIndex.removeAll()
    self.cellIndex.removeAll()
}
```

---

### 🔵 Suggestion: Android `KsSettingsView` の `concatAdapter` 構成の `SHARED_STABLE_IDS` 設定と stable ID 衝突回避の検証強化

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:76-81`

**問題点（提案レベル）**:
spec.md「ID 衝突回避」Scenario は「`headerAdapter.getItemId(0) = 1L`、`footerAdapter.getItemId(0) = 2L`、`mainListAdapter` の各 ID は 1L / 2L と衝突しない値域」を要求している。`RootHeaderFooterAdapter` 側の `getItemId` 実装と `KsSettingsListAdapter` 側の stable ID 計算ロジックが衝突しないことを直接検証するテストが見当たらない（`RootHeaderFooterAdapterTest` は本提案前から存在する既存テストで対応済みかもしれないが、本提案では `concatAdapter` の `SHARED_STABLE_IDS` 設定が新しい）。

**推奨修正**:
`KsSettingsViewTest` に「`headerAdapter.view = Text("X")` + `mainListAdapter.submitList([...])` 状態で `concatAdapter.getItemId(0)` / `concatAdapter.getItemId(1)` / ... が全 unique」を検証するテストを追加。

---

## アクションプラン

優先度順：

1. **🟠 Major 修正必須**: Store の「存在しない ID」操作で空振り Diff を発行する挙動を見直す（iOS / Android 両方の `removeSection` / `replaceSection` / `insertCell` / `removeCell` / `replaceCell` / `moveCell`）。`SettingsRootStoreTests` / `SettingsRootStoreTest` に「存在しない ID で Diff が発行されない」テストを追加。
2. **🟠 Major 修正必須**: Compose ラッパで `bind(store)` を `AndroidView.factory` で呼ぶ実装を見直す。`LocalLifecycleOwner.current` + `LaunchedEffect` で diff 購読を Compose 側ライフサイクルに乗せる、もしくは `KsSettingsView` View 側で `onAttachedToWindow` 時に再 bind するフォールバックを実装。動作確認用テストを追加。
3. **🟡 Minor 修正推奨**: `applyUpdateTheme` の `notifyDataSetChanged` を payload 付き `notifyItemRangeChanged` に置き換え、または `@SuppressLint` + コメントで意図を明示。
4. **🟡 Minor 修正推奨**: `applyFullSnapshot` での同一 identifier Cell の reload 戦略を明文化、または `reloadItems` を追加。
5. **🟡 Minor 修正推奨**: `rootHeader` / `rootFooter` setter の `rebuildLayout` 多発によるスクロール位置リセット問題のドキュメント化、または boundary のみ差し替える API 改善。
6. **🟡 Minor 修正推奨**: iOS Sample の `App.init()` で `KsCellRegistry.shared.register` を確実に呼ぶ。
7. **🟡 Minor 修正推奨**: SwiftUI ラッパの Store 差し替え非対応をドキュメント化、または `reconnectStore` API 追加。
8. **🟡 Minor 修正推奨**: メモリリークテストに Diff Publisher / SharedFlow 購読解除の直接検証を追加。
9. **🟡 Minor**: tasks.md 13.5 / 13.6 の実機目視確認を実施し、未確認のまま archive しない運用を徹底。
10. **🔵 Suggestion**: deinit のコメント補強、ConcatAdapter の ID 衝突回避テスト追加。

---

## 判定結果

**ステータス**: `CHANGES_REQUESTED`

- Major 指摘 2 件（Store の空振り Diff、Compose ラッパの bind タイミング）は仕様準拠性および利用者体験に直接影響するため、修正後の再レビュー必須。
- ビルド・テストは全通過しているが、テスト自体が両 Major ケースをカバーしていないことに留意（テストの網羅性不足）。
- 実機目視確認（tasks 13.5 / 13.6）が未完了のため、archive 前に必ず実施すること。
