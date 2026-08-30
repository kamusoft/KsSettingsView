# レビュー結果: restore-pickercell-object-items (001 回目)

**日付**: 2026-08-28
**判定**: NEEDS_DISCUSSION

## サマリー

ビルド・テストは 3 プラットフォームすべて green (iOS 973 / Android 2676 / MAUI 491、いずれも失敗 0)。デルタスペックの Requirement / Scenario は概ね過不足なく実装とテストへ落ちており、`PickerItem` 値型・ジェネリック縁・per-item DTO・MAUI リフレクション射影という ADR-0029 / design の骨格も忠実に守られている。視覚証跡も approved.png と実装の対応が取れている。

一方で、本 change が新設した MAUI の `SelectedItems` (TwoWay) に、**binding の適用順によって ViewModel 側の初期選択を黙って破壊する**経路があることを実測で確認した。デルタスペックの字面には反していない (spec が「`ItemsSource` 未設定は未選択へ揃える」と定めているため) が、AiForms 移行者の既存コードをそのまま動かすという本 change の目的に照らすと設計判断を要するため、NEEDS_DISCUSSION とする。あわせて、deviation に記録された付随修正 1 件が回帰テストで担保されていないこと (ミューテーション実測で確認) と、実態と食い違う doc コメント 1 件を挙げる。

## 確認した観点と結果

| 観点 | 結果 |
|---|---|
| ビルド / テスト (iOS Simulator 全件) | `xcodebuild test -scheme KsSettingsView-Package` → 973 tests / 0 failures |
| ビルド / テスト (Android 全件) | `./gradlew test --rerun-tasks` → 2676 tests (debug+release) / 0 failures / 0 errors |
| ビルド / テスト (MAUI) | `dotnet test KsSettingsView.Maui.Tests` → 491 / 0 failures |
| platform TFM のビルド (net10.0 では未コンパイルの gateway / binding) | `dotnet build -f net10.0-android` / `-f net10.0-ios` いずれも 0 エラー |
| iOS binding の実体整合 | 生成済み `KsSettingsViewBridge-Swift.h` に `SWIFT_CLASS_NAMED("KsBridgePickerItem")` と `initWithText:subText:` / `text` / `subText` を確認。`maui/macios/KsSettingsView.Binding.iOS/ApiDefinition.cs` の Export と一致 |
| lint | comment-policy 0 件 (`--selftest` 全 OK)、local-path-lint / identity-lint いずれも exit 0 |
| tasks.md の虚偽チェック | 該当なし (8.1 の視覚照合を含め、対応する実装・テスト・証跡を確認) |
| 足場アーティファクトの書き換え | spec / proposal / design に変更なし。`ui/brief.md` の追記は照合結果の記録で、規約どおり |
| 視覚証跡と提出コードの対応 | `ui/verification/` の 8 点すべて実在。iOS / Android とも「副表示あり行は 2 行 / なし行は 1 行 / 長い副表示は 1 行末尾省略 / 選択印は accent 色」が approved.png と一致し、`PickerListItemCell` (numberOfLines=1 + byTruncatingTail) / `PickerSelectionSheet` (isSingleLine + TruncateAt.END) の実装と対応が取れている |
| 付随修正の同梱条件 (ksn-core) | doc コメント 2 件・CellShapeTests の表 1 行・Android 呼び出し形の機械的追随はいずれも条件内。`KsSimpleCheckView` の 1 件のみテスト担保が欠けている (下記 Major-2) |

## 指摘事項

### [🟠 Major] MAUI `SelectedItems` は binding 適用順によって ViewModel 側の初期選択を黙って破棄する

**該当箇所**: `maui/KsSettingsView.Maui/PickerCell.cs` の `SyncIndicesFromSelectedItems` / `ResolveSelectedIndices` / `IndexOfItem` (および同型の `SyncIndexFromSelectedItem`)

**問題点**:

`SelectedItems` の setter は `_items` (= `ItemsSource` 設定時の写し) が空のとき、逆引きに失敗した要素をすべて捨て、`SelectedIndices` を空にしたうえで **正から再導出した空リストを `SelectedItems` へ書き戻す**。`SelectedItems` は TwoWay 既定なので、この書き戻しは ViewModel まで到達する。

`ItemsSource` より先に `SelectedItems` の binding が適用されると (XAML の属性宣言順に依存する)、ViewModel が持っていた初期選択がその時点で消え、後から `ItemsSource` が届いても復元されない。scratch harness (`Microsoft.Maui.Controls` 10.0.70 + 本 change の `KsSettingsView.Maui`) で `SetBinding` の登録順だけを入れ替えて実測した結果:

```
[1] ItemsSource → SelectedItem(s) の順   (= sample の宣言順)
  multi.SelectedIndices = [0,2]      vm.SelectedMembers = [佐藤 花子,高橋 次郎]
  single.SelectedIndex  = 0          vm.Assignee        = 佐藤 花子

[2] SelectedItem(s) → ItemsSource の順
  multi.SelectedIndices = []         vm.SelectedMembers = []        ← VM へ空が書き戻された
  single.SelectedIndex  = null       vm.Assignee        = null      ← VM へ null が書き戻された
```

`SelectedItem` 側の同じ挙動は本 change 以前からのもので (`SettingSelectedItemWithoutItemsSourceLeavesUnselected` が既存)、**`SelectedItems` は本 change の新規 API** としてこの性質を引き継いでいる。`samples/maui/.../Pages/InputCellsDemoPage.xaml` は `ItemsSource` を先に書いているため成立しているが、成立が属性順という暗黙の前提に依存している。

`maui-cells` spec の「見つからない要素・`ItemsSource` 未設定は未選択へ揃える SHALL」の字面には適合しており、**実装の逸脱ではなく spec が想定していなかった経路**と判断する。ただし本 change の目的 (design Decision 6: AiForms 移行者の既存コードがそのまま動く形を正にする) に照らすと、公開 TwoWay API がユーザーデータを黙って失う経路は放置しにくい。

**推奨修正** (いずれも設計判断が必要なため、オーナー/orchestrator の裁定を求める):

- **A: 現状維持 + 明示** — spec どおりとして受け入れ、`SelectedItem` / `SelectedItems` は `ItemsSource` より後に宣言する必要がある旨を facade の公開契約 (concepts の maui-facade) と移行 Skill へ書く。実装変更なし。テストとしては「`ItemsSource` 未設定時の `SelectedItems` 設定は空へ揃う」を明示的に固定する
- **B: `ItemsSource` 未設定時は書き戻さない** — `_items.Count == 0` のとき `SelectedIndices` の導出と `SelectedItems` の再導出をスキップし、設定値を保留する。`ItemsSource` 到着時 (`OnItemsSourceChanged`) に保留値から逆引きし直す。順序非依存になるが、「正は index」の契約に保留状態という第 3 の状態が入る
- **C: 逆引き不能時は index だけ空にし、`SelectedItems` の公開値は設定値のまま残す** — VM への破壊的書き戻しだけを止める。ただし spec の「公開値は正からの再導出で確定する」に反するため spec 改訂が要る

いずれを採る場合も、選んだ挙動を Scenario として固定するテスト (binding 経由でなくとも、`ItemsSource` 未設定での `SelectedItems` 設定 → `ItemsSource` 設定の順での期待値) を足すこと。

### [🟠 Major] 付随修正 `KsSimpleCheckView.onDraw` に回帰検出力のあるテストが無い

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSimpleCheckView.kt:85-87`

**問題点**:

deviation.md に付随修正として記録されている `canvas.width/height` → `width/height` の修正が、どのテストにも守られていない。ミューテーション実測で確認した (lessons L-001):

- `val w = width.toFloat()` / `val h = height.toFloat()` を元の `canvas.width` / `canvas.height` へ戻す
- `./gradlew :ks-settingsview-ui:testDebugUnitTest` → **BUILD SUCCESSFUL (1 件も落ちない)**
- backup との `shasum` 一致で原状復帰を確認済み (`87edc157cfc6f5ebe7247947a4015268fcda8383`)

ksn-core の付随修正の同梱条件④は「既存テストの通過と、必要なら 1 件のテスト追加で担保できる」であり、担保されていない修正の同梱はスコープの膨張にあたる。加えてこの View は `PickerSelectionSheet` だけでなく `RadioCell` / `SimpleCheckCell` の accessory でも使われる共有 View で、退行してもテストは緑のまま、症状 (チェックマークの消失) は View 階層をソフトウェア Canvas へ一括描画する経路でしか現れない — 気づけない類の退行である。

**推奨修正**: View 自身より大きい `Canvas` (例: `Bitmap` 128×128 に対し View は 30dp) へ `draw(canvas)` し、描かれた線分の座標が View の寸法比 (22%/52% → 38%/68% 等) から導かれる位置に来ること (= Canvas 寸法基準になっていないこと) を検証するテストを 1 件足す。Robolectric の `ShadowCanvas` で描画イベントを取れない場合は、`Bitmap` の該当画素が着色されていることの検証でもよい。

### [🟡 Minor] `PickerSelectionSheet.bindRow(row, position)` の doc コメントが実態と食い違う

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:617-623`

**問題点**: 「行の再利用 (`RecyclerView` の recycle 経路) と同じ状態遷移を、シートの実表示を伴わずに検証するための経路」と書かれているが、この関数は `ItemsAdapter.onBindViewHolder` が呼ぶ**本番の bind 実装そのもの**であり、検証用の経路ではない (検証用フックは同ファイルの `internal fun bindRow(index: Int)` の側で、「MARK: - 検証用フック」節に置かれている)。このファイルだけを読む人に用途を誤って伝える。comment-policy の「そのファイルだけを読んでいる人にとって意味が通ること」に照らして書き換えたい。

**推奨修正**: 本番の bind 実装としての説明 (候補の主表示 / 副表示の反映と可視性、contentDescription の組み立て、選択印の反映) に書き直す。テスト用に `internal` へ広げた事実を残したいなら、検証用フック側の doc に書く。

### [🔵 Suggestion] iOS の「元コレクションの変更を観測しない」テストは実装に依らず必ず通る

**該当箇所**: `ios/Tests/KsSettingsViewUITests/PickerCellItemsTests.swift:103-119`

**問題点**: `var source = plans` は Swift の値型 Array のコピーであり、`source[1] = ...` は縁が捕捉した配列に届きようがない。`PickerCell+ItemProjection.swift` の `let elements = items` を消しても (消せないが) このテストは通る。cell-types-input spec の Scenario「元コレクションの変更を観測しない」は Swift では言語のセマンティクスが保証しており、テストとしての回帰検出力は無い。

Android 側 (`PickerCellItemsTest` / `PickerCellObjectBindingTest` の同名テスト) は `MutableList` を共有するため意味があり、そちらで Scenario は担保されている。

**推奨修正**: 必須ではない。残すなら「Swift では値型セマンティクスにより構造的に保証される」旨をコメントで明示し、確認しているのは `onItemSelected` が確定 index の要素を渡すことである、と読める形にすると誤解が減る。

### [🔵 Suggestion] samples-maui の Requirement に実行証跡が無い

**該当箇所**: `kasane/changes/restore-pickercell-object-items/specs/samples-maui/spec.md` / `ui/verification/`

**問題点**: `ui/verification/` の 8 点は iOS / Android の native sample と選択面単体で、MAUI sample の証跡は無い。samples-maui の Scenario は「確定すると行の値表示と ViewModel 側の選択項目が更新される」という実行時の要求で、MAUI だけが facade 射影 → snapshot → per-item DTO → 2 種の binding という本 change で最も広く変わった経路を通る。

ただし本レビューでは代替として (a) facade 層のユニットテスト (`PickerItemProjectionTests` / `PickerSelectedItemTests` の Fakes gateway 経由の書き戻し)、(b) `net10.0-android` / `net10.0-ios` TFM のビルド成功、(c) 生成済み `KsSettingsViewBridge-Swift.h` の Export と `ApiDefinition.cs` の一致、(d) Android AAR からの binding 生成成功 (`KsBridgePickerItem` の C# 呼び出しがコンパイル通過) を確認しており、輸送面の整合は取れている。残る未確認は MAUI 実アプリでの表示・確定の 1 往復のみ。

**推奨修正**: mock 照合の対象ではないので必須ではないが、Major-1 の裁定でどの案を採るにせよ MAUI sample の実行確認は同時に済ませ、スクリーンショット 1 枚を `ui/verification/` (または `evidence/`) へ残しておくと、binding 越しの実挙動と Major-1 の実害度の両方が同時に片付く。

## アクションプラン

1. **Major-1 (MAUI `SelectedItems` の順序依存)** — A / B / C のいずれを採るかオーナー判断を仰ぐ。採用案に対応する Scenario とテストを追加する (spec 改訂を伴う案なら足場の spec 更新も含む)
2. **Major-2 (`KsSimpleCheckView` の回帰テスト)** — テストを 1 件追加する。追加できない技術的理由があるなら、その理由を deviation.md へ書いたうえで同梱の是非をオーナーに諮る
3. **Minor-1 (doc コメント)** — `bindRow(row, position)` の doc を本番実装の説明へ書き直す
4. **Suggestion 2 件** — Major-1 の対応と同じ周回で、MAUI sample の実行証跡と iOS テストのコメント補足をあわせて片付けると効率がよい

## 再レビュー時の確認点

- Major-1 の採用案が実装・テスト・(必要なら) spec に一貫して反映されているか
- Major-2 のテストが実際に検出力を持つか (`canvas.width` へ戻したときに落ちるか) をミューテーションで確認する
- 3 プラットフォームの全件テストが引き続き green か (件数併記)
