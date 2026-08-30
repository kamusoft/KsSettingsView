# レビュー結果: add-maui-accessory-views (003 回目)

**日付**: 2026-08-12
**判定**: APPROVED

## サマリー

前回 (review-002) の Major 1 件・Minor 1 件、および second-opinion-code-002 の Suggestion 1 件は**すべて解消済み**で、修正の方向も正しい。論理所有の確定を `SetAccessoryView` の「多重配置の検査 → 所有の確定 → 実体化 → 配信 → 旧実体の破棄」という一本の順序へ集約し、`Section` 側の `propertyChanged` を変換経路に載っていない Section 専用の冪等な受け皿として残す形は、Root と Section の非対称を解消しつつ Handler 未接続中の継承も維持できている。追加された 11 件のテストは、ミューテーション実測で**実際に回帰を検出する**ことを確認した (下記「確認した観点」)。

cycle 3 の差分に Critical / Major / Minor の問題は見つからなかった。指摘は Suggestion 3 件で、いずれも本 change のスコープ内で直す必要はない (2 件は spec が既存契約へ委譲済みの範囲、1 件はテストの検出力の改善提案)。

## 前回指摘の解消確認

| 前回指摘 | 状態 |
|---|---|
| review-002 Major: Root 方向の多重配置例外が既存 Section accessory の論理親と BindingContext を奪う | **解消**。`SettingsView.cs:61-78` の `propertyChanged` は `_controller.SetRootAccessoryView` を呼ぶだけになり、`SettingsView` 側の `Reassign` 呼び出しは消えた。所有の付け替えは `KsSettingsController.cs:410-435` の `SetAccessoryView` 内で、`EnsureAccessoryViewIsNotPlaced` (`:412`) を通った**後**に 1 箇所だけ行われる。回帰テスト `FailedRootPlacementLeavesTheExistingSectionPlacementIntact` / `FailedSectionPlacementLeavesTheExistingRootPlacementIntact` が論理親・継承 BindingContext・native への輸送の 3 点を固定しており、検査を `Reassign` の後ろへ動かすミューテーションでこの 2 件だけが落ちることを実測した |
| review-002 Minor (優先度: 高): Section accessory の実体化が所有・BindingContext の確定より先に走る | **解消**。Section も controller 経由で `Reassign` → `Materialize` の順になった。`FakeViewLease` に `ParentAtMaterialize` / `BindingContextAtMaterialize` (`FakeViewMaterializer.cs:88-98`) が入り、`ViewIsOwnedBeforeItIsMaterialized` / `ViewIsOwnedBeforeItIsRematerializedForANewHost` が 4 対象すべてで実体化の瞬間の状態を観測する。`Reassign` を `Materialize` の後ろへ動かすミューテーションで前者 4 ケースだけが落ちることを実測した。`Materialize` の `<remarks>` (`:473-477`) も実装と一致している |
| second-opinion-code-002 Suggestion: LeakTests のコメントが fake 実装と一致しない | **解消**。`LeakTests.cs:134-136` が「ここで見るのは platform 実体が回収されることだけで、実体の後片付けに伴う Handler の接続状態は AccessoryViewTests が受け持つ」という現状どおりの記述になった |
| second-opinion-code-002 Minor: 論理所有の寿命変更が deviation.md 未記録 | **解消**。`deviation.md` の 3 件目 (2026-08-12) として記録済み。本レビューでは合意済み差分として扱った |

## 指摘事項

### [🔵 Suggestion] 受け皿経路には多重配置の検査が無いことが、共有ヘルパの remarks から読み取れない

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsAccessoryViewOwnership.cs:14-16` / `maui/KsSettingsView.Maui/Section.cs:40-45`

**問題点**:

`KsAccessoryViewOwnership` の `<remarks>` は付け替えの位置を無条件にこう述べている。

> 付け替えは、多重配置の検査を通った後・platform 実体を作る前に行う。検査より後にすることで、例外になった配置が他所の正しい配置の所有を奪わない。

これが成り立つのは controller 経由の呼び出し (`SetAccessoryView`) だけで、`Section.HeaderViewProperty` / `FooterViewProperty` の受け皿経路は検査を通らない。実測 (一時プローブ、確認後に削除済み):

```
[P1] before  Parent=Section Text=s1-ctx transported=True
[P1] set on unregistered section thrown=none
[P1] after   Parent=s2 Text=s2-ctx        # 論理親と継承 BindingContext が移る
[P1] still transported at s1=True         # s1 の header としては生きたまま
[P1] add s2 thrown=InvalidOperationException  (この時点で初めて例外)
```

つまり、変換経路に載っている Section の header に置かれた View を、**まだ設定ツリーに入れていない Section** の `HeaderView` に設定すると、例外なしに論理親と BindingContext が奪われ、表示中の accessory が別文脈のデータを出す。例外は後で Section を追加した時点までずれ込む。

ただしこれは**既存契約と同型**であり、spec 違反ではない。同じプローブで `CellBase` を比較したところ、まったく同じ形になった。

```
[P2] cell in s1 Title=s1-ctx ctx=s1-ctx
[P2] add same cell to unregistered s2 thrown=none Title=s2-ctx ctx=s2-ctx
[P2] add s2 thrown=InvalidOperationException
```

`specs/maui-core/spec.md` の Requirement「同一 View インスタンスの多重配置は例外になる」は「検出範囲・例外のタイミング・例外後のプロパティ状態は、既存の Section / CellBase 多重配置検出と同一の契約に従う」と明記しているため、この挙動は合意済みの範囲に収まる。

**推奨修正**:
コードの挙動は変えず、`KsAccessoryViewOwnership` の `<remarks>` を「controller 経由で呼ばれるときは検査を通った後になる」と限定し、受け皿経路には検査が無いことを `Section.cs` 側の記述と揃えて書く。あわせて、変換経路に載っていない Section 経由の多重配置は追加時まで検出されない (その間は論理親が移る) ことを、蒸留時に `maui-facade` 系 concepts の既知挙動として拾うことを推奨する。

### [🔵 Suggestion] 多重配置の例外を投げたプロパティは、以後の設定が無言で効かなくなる

**該当箇所**: `maui/KsSettingsView.Maui/SettingsView.cs:61-78` / `maui/KsSettingsView.Maui/Section.cs:46-66` (BindableProperty の `propertyChanged` から例外を送出する構造)

**問題点**:

多重配置の例外は `propertyChanged` コールバック (Root) または `INotifyPropertyChanged` ハンドラ (Section) の中から飛ぶ。MAUI の `BindableObject` は値の格納後・後始末前にこれらを呼ぶため、例外で抜けると当該プロパティのコンテキストが「設定中」のまま残り、**以後そのプロパティへの設定がすべて捨てられる**。実測:

```
[P5] 失敗後に別インスタンス b を RootFooterView へ:  prop=a(古い値のまま) bParent=null calls=0
[P5] 例外を投げていない RootHeaderView は正常:        cParent=SettingsView calls=1
[P5] RootFooterText は正常:                          calls=1
[P6] Section.HeaderView でも同じ (footer / text は正常)
```

利用者から見ると「間違いを直して置き直す」ができず、しかもエラーも出ない (プロパティの getter は拒否された値を返し続ける)。

これも**既存契約と同型**で、spec 違反ではない。本 change 以前からある `SettingsView.Root` (Section の多重配置検出) がまったく同じ挙動を示す。

```
[P7] 重複 Section で例外 → 正常なコレクションへ再設定: calls=0 count=2 (拒否された値のまま)
```

**推奨修正**:
本 change での修正は不要。蒸留時に「多重配置の例外を受けたプロパティは以後設定し直せない (インスタンスを作り直す必要がある)」を既知の制約として concepts / docs 側へ拾うことを推奨する。恒久的に直すなら、例外の送出点を `propertyChanged` の外 (`propertyChanging` や設定前の検査) へ移す設計変更が必要で、`Root` を含む既存の多重配置検出全体に及ぶため、本 change の範囲を超える。

### [🔵 Suggestion] Host 無しの多重配置テストは「既存配置を壊さない」側の検出力を持たない

**該当箇所**: `maui/KsSettingsView.Maui.Tests/AccessoryViewTests.cs:368-381` (`FailedPlacementLeavesTheExistingPlacementIntactWithoutAHost`)

**問題点**:

このテストは `RootHeaderView` に置いた View を `RootFooterView` へ設定して例外を確認するが、**両スロットの所有者が同じ `SettingsView`** であるため、`accessory.Parent` は所有の付け替えが起きても変化しない。実測として、`EnsureAccessoryViewIsNotPlaced` を `Reassign` の後ろへ動かすミューテーションでは `FailedRootPlacement...` / `FailedSectionPlacement...` の 2 件が落ちる一方、このテストは**通過したまま**だった。例外が飛ぶこと自体 (Host 無しでも検出が働くこと) は固定できているが、テスト名が主張する「既存配置を壊さない」部分は検証になっていない。

**推奨修正**:
既存配置側を Section の header にする (`section.HeaderView = accessory` → `view.RootHeaderView = accessory` で例外) と所有者が異なり、`accessory.Parent` が Section のままであることに検出力が生まれる。

## 確認した観点 (指摘に至らなかったもの)

- **ビルド**: `dotnet build maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj` = 3 TFM (net10.0 / net10.0-ios / net10.0-android) すべて成功、警告 0 / エラー 0
- **テスト**: `dotnet test maui/KsSettingsView.Maui.Tests` = **339 tests / 0 failures** (cycle 2 の 328 から +11。内訳は `FailedRootPlacement...` / `FailedSectionPlacement...` / `FailedPlacement...WithoutAHost` 各 1 + `ViewIsOwnedBeforeItIsMaterialized` 4 + `ViewIsOwnedBeforeItIsRematerializedForANewHost` 4 で一致)
- **iOS / Android**: `find -newer review-002.md` で cycle 3 の変更が `maui/` 配下 7 ファイル (SettingsView.cs / Section.cs / KsSettingsController.cs / KsAccessoryViewOwnership.cs / AccessoryViewTests.cs / FakeViewMaterializer.cs / LeakTests.cs) に限られることを確認。native 側は review-002 時点の green (iOS 476 / Android 2280、いずれも failures 0) から無変更のため再実行していない
- **comment-policy lint**: `python3 scripts/comment-policy-lint.py` = 禁止 0 件 (検査対象 573 ファイル)
- **回帰検出力のミューテーション実測** (lessons code-review L-001): (1) `SetAccessoryView` の `Reassign` を `Materialize` の後ろへ移動 → `ViewIsOwnedBeforeItIsMaterialized` の 4 ケースのみ失敗 (335 合格)。(2) `EnsureAccessoryViewIsNotPlaced` を `Reassign` の後ろへ移動 → `FailedRootPlacement...` / `FailedSectionPlacement...` の 2 件のみ失敗 (337 合格)。いずれも `KsSettingsController.cs` を backup から復元し shasum 一致 (`84ec8e43…`) を確認済み
- **受け皿の冪等性**: 変換経路に載った Section では controller の `Reassign` が先に走り、直後の `propertyChanged` の `Reassign` は `oldView.Parent != owner` / `newView.Parent == owner` の両ガードで空振りする。実測で差し替え後の Section の論理子は 1 件のまま、旧 View は `Parent=null` / `BindingContext=null` に戻ることを確認 (二重登録・二重伝播なし)
- **例外時に受け皿が走らないこと**: Section 方向は `INotifyPropertyChanged` ハンドラ内で例外が飛ぶため `propertyChanged` に到達せず、受け皿の `Reassign` は実行されない (`FailedSectionPlacementLeavesTheExistingRootPlacementIntact` が固定)
- **所有と placement の寿命分離**: `ReleaseHost` は `_views` と lease だけを落とし `_accessories` / `_placedViews` を保つ。`ClearRegistrations` は Section 分だけを `RetireSectionAccessoryViews` で退役させ root の placement を残すため、`RebuildRoot` をまたいでも root accessory は `ApplyRootSlot` で復元される。Section 分は `RegisterSection` → `PlaceSectionAccessoryViews` で再配置される
- **`Materialize` 前の `DisposeRetiredViewsOf`**: review-002 で確認済みの「native へ配信を終えてから破棄する」不変条件は、`Reassign` の挿入位置が変わっても崩れていない (`Reassign` は native 配信を伴わない)
- **`SetAccessoryView` の単一経路化による副作用**: root は gateway の有無に関わらず `SetRootAccessoryView` → `SetAccessoryView` を通るため、Handler 未接続中も所有が確定する (`AccessoryInheritsTheBindingContextWithoutAHost` / `FailedPlacementLeavesTheExistingPlacementIntactWithoutAHost` が未接続経路を通っている)
- **足場の非改変**: `kasane/changes/` 配下で追跡対象の変更は tasks.md のチェックボックスのみ (proposal / design / specs 3 本は無改変)。tasks 3.4 の「最後に `AddLogicalChild`」は deviation.md 3 件目 (論理所有の分離) に含まれる合意済み差分であり、チェック済みでも虚偽ではない
- **deviation.md**: 3 件とも合意済み差分として指摘から除外した

## アクションプラン

1. 本 change としては修正不要。Suggestion 3 件はいずれも任意
2. 着手するなら優先順は (a) `KsAccessoryViewOwnership` の `<remarks>` の限定 → (b) `FailedPlacement...WithoutAHost` の所有者を分ける → (c) 蒸留時の既知挙動記録 2 件 (受け皿経路の検出遅れ / 例外後のプロパティ固着)
3. review-001 Suggestion 3 の蒸留時の宿題 (`maui-facade.md` L30 / L74、sample-parity.md、iOS superview 剥がしの ADR 反映先) は引き続き有効
