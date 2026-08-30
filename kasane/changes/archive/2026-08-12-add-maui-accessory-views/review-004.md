# レビュー結果: add-maui-accessory-views (004 回目)

**日付**: 2026-08-12
**判定**: APPROVED

## サマリー

cycle 4 の差分 (`KsAccessoryViewOwnership.ReassignIfFree` の新設・`Section` の受け皿の切り替え・テスト 5 件) は、second-opinion-code-003 の Major (未接続 Section の受け皿が多重配置の検査より先に既存配置の論理親と継承 BindingContext を奪う) を deviation.md 5 件目の裁定どおりに解消している。ガードの条件は裁定の文言 (`newView.Parent` が null または当該 Section 自身のときのみ確定) と厳密に一致し、正規の再利用 (null 戻し後の引き取り) は壊れていないことをミューテーション実測 2 種と挙動プローブ 2 件で確認した。

cycle 4 差分に Critical / Major / Minor は見つからなかった。指摘は Suggestion 1 件 (コメントが述べる「例外が出る時点」が 1 経路で実際とずれる) のみで、本 change のスコープ内で直す必要はない。

## 前回指摘の解消確認

| 前回指摘 | 状態 |
|---|---|
| second-opinion-code-003 Major: 未接続 Section の fallback が、追加時の多重配置検査より前に既存配置を奪う | **解消**。`Section.cs:52` / `:64` の `propertyChanged` が `KsAccessoryViewOwnership.ReassignIfFree` (`KsAccessoryViewOwnership.cs:68-77`) を呼ぶようになり、`newView.Parent` が他所を指すときは引き取らず、外す方の後始末 (`Detach`) だけを行う。回帰テストは `AddingASectionThatReusesAPlacedViewThrows` (オブジェクト初期化子経路) と `GeneratedSectionThatReusesAPlacedViewThrowsWithoutStealingIt` (ItemsSource / DataTemplate 経路) の 2 件で、論理親・継承 BindingContext・lease の同一性・native への配信の 4 点を固定している |
| review-003 Suggestion 1: 受け皿経路に検査が無いことが共有ヘルパの remarks から読み取れない | **解消**。`KsAccessoryViewOwnership.cs:14-27` が入口を 2 つに分けて明記し (`Reassign` = 検査を通った呼び出し元用 / `ReassignIfFree` = 検査を行う相手がいない呼び出し元用)、`Section.cs:40-45` の remarks も受け皿の役割と引き取らない条件を述べている |
| review-003 Suggestion 3: Host 無しの多重配置テストが「既存配置を壊さない」側の検出力を持たない | **解消**。`AccessoryViewTests.cs:413` の `FailedPlacementLeavesTheExistingPlacementIntactWithoutAHost` は既存配置を Section の header 側に置き、失敗させるのは `RootHeaderView` への設定に変わった。所有者が別であるため `accessory.Parent` が `Section` のままであることに検出力がある |
| review-003 Suggestion 2 (例外後のプロパティ固着) | 蒸留での既知挙動記録として据え置き済み。本レビューでは扱わない |

## 指摘事項

### [🔵 Suggestion] 引き取りを断った配置の例外が出る時点が、1 経路でコメントの記述とずれる

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsAccessoryViewOwnership.cs:62-63` / `maui/KsSettingsView.Maui/Section.cs:44-45`

**問題点**:

両方の remarks が、引き取らなかった配置の落とし所をこう述べている。

> 引き取らなかった配置は表示にも論理ツリーにも現れず、所有者が設定ツリーへ入る時点で多重配置の例外になる。 (`KsAccessoryViewOwnership.cs:62-63`)
> この Section を設定ツリーへ入れる時点で例外になる (`Section.cs:45`)

これは「まだ `SettingsView.Root` に入っていない Section」には正確だが、**設定ツリーには入っているが Native Host がまだ無い Section** — XAML でページを組み立ててから Handler が付くまでの間 — では成り立たない。この状態の Section は `_sectionEntries` に載っておらず `HandleSectionPropertyChanged` (`KsSettingsController.cs:1030-1033`) が早期 return するため、受け皿だけが走る。実測 (一時プローブ、確認後に削除済み):

```
at set:     thrown=none                     parent=SettingsView   # 引き取らず、既存配置は無傷
at connect: thrown=InvalidOperationException parent=SettingsView   # 例外は Host 接続時まで遅れる
```

つまりこの経路では、既に設定ツリーに入っている Section であっても例外は「設定ツリーへ入る時点」ではなく **Native Host が接続される時点** (`RegisterSection` → `PlaceSectionAccessoryViews` → `EnsureAccessoryViewIsNotPlaced`) までずれ込む。壊れてはいけない既存配置が無傷である点は満たされているため、実害はコメントの精度に限られる。

**推奨修正**:
「所有者が設定ツリーへ入る時点で」を「所有者が変換経路に載る時点 (設定ツリーへの追加、または Native Host の接続) で」のように、両方の合流点を指す表現へ広げる。本 change での修正は必須ではなく、蒸留時に concepts 側の記述と合わせて整えるのでもよい。

## 確認した観点 (指摘に至らなかったもの)

- **ビルド**: `dotnet build maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj` = 3 TFM (net10.0 / net10.0-ios / net10.0-android) すべて成功、警告 0 / エラー 0
- **テスト**: `dotnet test maui/KsSettingsView.Maui.Tests` = **342 tests / 0 failures** (cycle 3 の 339 から +3。新規 `GeneratedSectionThatReusesAPlacedViewThrowsWithoutStealingIt` / `UnregisteredSectionClaimsAViewThatWasReleased` / `DuplicatePlacementIsDetectedWithoutAGateway` と一致)
- **差分範囲**: `review-003.md` より新しい実装ファイルは `Section.cs` / `KsAccessoryViewOwnership.cs` / `AccessoryViewTests.cs` の 3 件のみ (`KsSettingsController.cs` は mtime のみ更新で内容は無変更 — shasum `84ec8e43…` 一致)。iOS / Android / samples に cycle 4 の変更は無く、native テストは再実行していない
- **足場の凍結**: `proposal.md` / `design.md` / `specs/*/spec.md` / `tasks.md` は cycle 4 で書き換えられていない (mtime が review-003 より前)
- **deviation との一致**: 実装のガード条件 (`newView.Parent` が null または owner 自身のときだけ確定) は deviation.md 5 件目の裁定文と厳密に一致する。裁定に含まれるテスト強化 3 点 (例外後の既存配置無傷アサート / ItemsSource 経路 / review-003 Suggestion 3 の検出力補強) もすべて入っている
- **回帰検出力のミューテーション実測** (lessons code-review L-001):
  - (1) `Section.cs` の `ReassignIfFree` を `Reassign` に戻す (ガード撤去) → `AddingASectionThatReusesAPlacedViewThrows` / `GeneratedSectionThatReusesAPlacedViewThrowsWithoutStealingIt` の **2 件だけが失敗** (340 合格)。修正が守っている性質をテストが実際に検出できている
  - (2) ガードを過剰に広げる (`newView is not null` なら常に引き取らない) → `UnregisteredSectionClaimsAViewThatWasReleased` / `AccessoryInheritsTheBindingContextWithoutAHost` の **2 件だけが失敗** (340 合格)。ガードが正規の引き取りまで潰した場合も検出できる (指示事項 (b) の確認)
  - いずれも編集した `Section.cs` / `KsAccessoryViewOwnership.cs` を復元し shasum 一致 (`eb55e8ed…` / `8f087625…`) を確認済み
- **正規経路が壊れていないこと (挙動プローブ、確認後に削除済み)**:
  - 通常 Layout (Grid) の子を未接続 Section の `HeaderView` に置く → 生成時は引き取らない (`parent=Grid`) が、`Root.Add` 時に controller が例外なく引き取り、論理親・継承 BindingContext・native への輸送がすべて成立する (`parent=Section` / `text=view-ctx` / `transported=True`)。多重配置ではない View の最終状態は cycle 3 と変わらず、継承の確定が設定ツリー参加まで遅れるだけ
  - 引き取りを断った後に元の置き場所を null に戻してから `Root.Add` → 正常に引き取られる (`parent=Section` / `transported=True`)
- **connected な Section での順序**: 変換経路に載っている Section では controller の `SetAccessoryView` (検査 → 所有確定) が先に走り、`propertyChanged` の受け皿は空振りする。ミューテーション (1) でガードを外しても `FailedSectionPlacementLeavesTheExistingRootPlacementIntact` が通過したままだったことが、例外が受け皿到達より前に飛んでいることの実測になっている (`Section.cs:40-45` の remarks の記述と一致)
- **ガード条件の冗長性**: `!ReferenceEquals(holder, owner)` を外しても `Reassign` 側の早期 return が同じ結果を返すため、この節は挙動としては冗長。ただし「自分が既に持っている View は奪取に当たらない」という意図の明示として読めるため、削減は推奨しない
- **`ReassignIfFree` の後始末**: 引き取りを断つ枝でも `Detach(owner, oldView)` を通るため、置き換え元の View が所有者に残り続けることはない。断った後に `null` を設定した場合も `Detach` は `oldView.Parent != owner` で空振りし、他所の配置を巻き込まない
- **comment-policy lint**: `python3 scripts/comment-policy-lint.py` = 禁止 0 件 (検査対象 573 ファイル)。cycle 4 で追加された remarks に禁止参照・履歴記述・spec キーワードの混入はない
- **独立性**: レビュー中に `second-opinion-code-004.md` が生成されたが、判断の独立を保つため参照していない

## アクションプラン

1. 本 change としては修正不要。Suggestion 1 件は任意
2. 着手するなら、`KsAccessoryViewOwnership` / `Section` の remarks で「例外になる時点」を設定ツリー参加と Native Host 接続の両方を含む表現へ広げる
3. 蒸留時の宿題は review-001 Suggestion 3 / review-003 Suggestion 2 から引き続き有効 (`maui-facade.md` L30 / L74、sample-parity.md、iOS superview 剥がしの ADR 反映先、例外後のプロパティ固着の既知挙動記録)。これに「受け皿経路では多重配置の検出が変換経路への合流まで遅れる」という契約を加えると concepts 側が揃う
