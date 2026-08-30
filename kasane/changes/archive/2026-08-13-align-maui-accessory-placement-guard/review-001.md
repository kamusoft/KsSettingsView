# レビュー結果: align-maui-accessory-placement-guard (001 回目)

**日付**: 2026-08-13
**判定**: APPROVED

## サマリー

デルタスペックの全 Scenario が実装とテストで満たされている。値確定前ガード (`validateValue` → `IKsAccessoryViewGuard` → `EnsureAccessoryViewIsNotPlaced`) は Section 4 対象・Root 2 対象すべてで有効に働いており、失敗時に公開値・論理所有・lease・native 配信のいずれも動かないことをミューテーション実測で確認した (ガード配線を外すと該当テストだけが赤になる)。ビルド成功・maui テスト 413 件 / 失敗 0 (自分で実行)。

指摘は 2 件とも「挙動は正しいが、その挙動を守っているコードに回帰検出力が無い」型で、いずれも Critical / Major ではない。deviation.md 記載の 2 件 (AccessoryGuard の弱参照化・CustomCell.ContentGuard の既存リーク) は合意済み差分として扱い、弱参照化の理由が実測で裏付けられることのみ確認した (下記「確認した観点」)。

## 指摘事項

### [🟡 Minor] 取り外した Section の guard 解除に回帰検出力が無い

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1022` (`ClearRegistrations`) および `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1961` (`UnregisterSection`) の `section.AccessoryGuard = null;`

**問題点**: この 2 行はデルタスペックの「設定ツリーに未参加の所有者の accessory プロパティには検査を行う相手がいないため、既配置の View を設定しても既存配置を奪わず、その所有者が変換経路に加わった時点で例外になる」を直に支えている。しかし現行テストはこの解除を 1 件も固定していない。

実測 (ミューテーションプローブ):

- 上記 2 行を両方削除しても既存 413 件は全通過する
- 一方で挙動は確実に変わる。「Root から取り外した Section の `HeaderView` に、その SettingsView の `RootHeaderView` に置かれている View を設定する」を確かめる一時テストを足すと、解除ありでは `DoesNotThrow`、解除なしでは即 `InvalidOperationException` になる (一時テストは検証後に削除済み)

つまり将来この解除が落ちても、公開挙動が spec の原則から外れたまま緑になる。

**推奨修正**: `AccessoryViewTests` へ 1 本追加する。「Root から取り外した Section の `HeaderView` へ既配置 View を設定しても例外にならず、その Section を `Root` へ戻した時点で例外になる」を固定すれば、解除の有無と持ち越しの意味論が同時に押さえられる。

### [🟡 Minor] バッチ内 accessory 数えあげ (tasks 2.1) が現状到達不能で、対応テストが新規コードを固定していない

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:2105-2120` (`EnsureSectionsAreNotPlaced` の `HashSet<View> seen` と `AddSeenView` 2 箇所、および `EnsureCellsAreNotPlaced(cells, seen)` への seen 引き回し)

**問題点**: `EnsureSectionsAreNotPlaced` の呼び出し口は `AddSections` / `ReplaceSections` の 2 箇所だけで、どちらも直前に同じリストで `EnsureTreeHasNoDuplicates` を呼んでいる。そちらが root accessory を種として header / footer / `CustomCell.Content` を同一集合で数えているため、新規追加分の `AddSeenView` は現時点で 1 度も発火し得ない。

実測: `EnsureSectionsAreNotPlaced` から `AddSeenView` 2 行を削除し、`EnsureCellsAreNotPlaced` へ毎回新しい集合を渡す形に戻しても、413 件全通過する。つまり tasks 3.3 のテスト (`AddingSectionsThatShareAnAccessoryViewThrowsBeforeAnyInsert`) が守っているのは既存の `EnsureTreeHasNoDuplicates` 側であり、tasks 2.1 で足したコードではない。

proposal が「検査の置き場の対称化 (将来の呼び出し口で穴が開かないようにする)」を目的として明記しているため仕様違反ではない。ただし現状のコメント (「1 件目を native と対応表へ入れた後に 2 件目で例外にすると、途中まで進んだ配置が残る」) は、この数えあげが `EnsureTreeHasNoDuplicates` と二重であり今は保険として置かれている、という肝心の事実に触れていない。ファイル単独で読むと「ここが唯一の防波堤」と誤読する。

**推奨修正**: どちらかを行う。

- コメントに「この数えあげは呼び出し口が現在必ず通る `EnsureTreeHasNoDuplicates` と重複しており、`EnsureSectionsAreNotPlaced` を単独で呼ぶ経路が増えたときの受け皿である」旨を足す (最小)
- または `EnsureSectionsAreNotPlaced` に相当する経路を直接叩く単体テストを 1 本足して、削除したら赤になる状態にする

### [🔵 Suggestion] doc コメントの型名修飾が SettingsView 側と揃っていない

**該当箇所**: `maui/KsSettingsView.Maui/Section.cs:49` の `<see cref="System.InvalidOperationException"/>`

**問題点**: 同じ趣旨の説明が `SettingsView.cs:66` では `<see cref="InvalidOperationException"/>` と非修飾で書かれている。本変更で `Section.cs` に `using System;` を足したため、修飾は不要になった。

**推奨修正**: `<see cref="InvalidOperationException"/>` に揃える。

### [🔵 Suggestion] 再収束テストの一部アサーションが再構築の前から成立している

**該当箇所**: `maui/KsSettingsView.Maui.Tests/AccessoryViewTests.cs:700` (`AFailedAddBatchIsRecoveredByRebuildingTheRoot` の `Assert.That(shared.Parent, Is.SameAs(first))`)

**問題点**: `shared` の論理親は、失敗したバッチの前に `Section first = new() { HeaderView = shared }` を組み立てた時点で既に `first` になっている (`ReassignIfFree` が空き View を引き取るため)。再構築の成否と無関係に真であり、このアサーション単独では再収束を証明しない。

検出力は同テスト内の `FindSectionId(first) is not null` と `AccessoryViewOf(first, SectionHeader)` が担っているため実害はない。冗長なアサーションだと分かる形にするか、そのままでも可。

**推奨修正**: 残すなら「再構築後も論理親が保たれている」ことの確認である旨をコメントで明示する。

## 確認した観点 (指摘なし)

- **ビルドとテスト**: `dotnet test KsSettingsView.Maui.Tests -f net10.0` で 413 件 / 失敗 0。ベースライン 400 件に対し +13 件・退行ゼロ (tasks 4.1 の申告と一致)
- **ガード配線の検出力** (lessons L-001 の実測): `RegisterSection` の `section.AccessoryGuard = this;` を外すと `AFailedViewReplacementKeepsTheCurrentView(SectionHeader / SectionFooter)` の 2 件が赤。`RootHeaderViewProperty` の `validateValue` を外すと `AFailedViewReplacementKeepsTheCurrentView(RootHeader)` と `AFailedRootViewReplacementWithACellContentKeepsTheCurrentView` の 2 件が赤。4 対象すべてで検査が実際に効いていることを確認した
- **弱参照化 (deviation 1)**: `Section.AccessoryGuard` を強参照の自動プロパティへ戻すと `ViewIsCollectedWhileInteractionsAreAttached` / `FacadeAndGatewayAreCollectedWhileExternalHoldsModel` の 2 件が赤になり、deviation の理由 (強参照だと Section → controller → SettingsView が繋がる) が実測で裏付けられた。model → controller を弱参照にする既存規律 (`KsWeakPropertySubscription`) とも一貫している
- **成功経路の非退行**: 同一 slot への再設定は `EnsureAccessoryViewIsNotPlaced` の `placed != slot` で素通りし、null 解除後の再利用・同一 Section を含む Root 再構築も緑 (`RebuildingWithTheSameSectionKeepsItsAccessory`)
- **検査と確定の順序**: `validateValue` → `propertyChanged` (`ReassignIfFree`) → 変換経路の `SetAccessoryView` の間で `_placedViews` は動かないため、ガードを通った設定が後段で例外になる残存経路は無い。`SetAccessoryView` 側の再検査も残っており二重の防波堤になっている
- **失敗時の gateway 無干渉**: 新規 4 テストと既存 `AddingASectionThatReusesAPlacedViewThrows` (本変更で `UpdateAccessoryView` 限定から `Gateway.Calls` 全体へ強化) が「例外後に gateway 呼び出し 0 件」を固定している。アサーションの強化方向であり緩和はない
- **足場の非改変**: working tree の変更は `maui/` 配下 4 ファイル + 新規 2 ファイルのみ。spec / proposal に相当する差分は無い
- **コメント規約**: `python3 scripts/comment-policy-lint.py --summary` で禁止参照 0 件。未追跡の新規 2 ファイル (`IKsAccessoryViewGuard.cs` / `RangeReplaceCollection.cs`) は lint の走査対象外のため目視確認したが、変更 ID・レビュー通番・spec キーワードの混入は無い
- **テスト補助の一貫性**: `RangeReplaceCollection<T>` は既存 `RangeAddCollection<T>` と同じ書き方 (`Items` 直接操作 + 単発 `OnCollectionChanged`) で揃っている
- **CustomCell.ContentGuard の強参照リーク (deviation 2)**: `CustomCell.cs:183` が自動プロパティのままであることを確認。本変更のスコープ外という記録どおり手が入っておらず、Non-Goals とも整合する。別変更での対応を前提とする

## アクションプラン

1. (任意・推奨) Minor 1: 取り外した Section の持ち越し挙動を固定するテストを `AccessoryViewTests` に 1 本追加する
2. (任意) Minor 2: `EnsureSectionsAreNotPlaced` の数えあげが `EnsureTreeHasNoDuplicates` と二重の保険であることをコメントで明示する
3. (任意) Suggestion 2 件: doc の型名修飾を揃える / 再収束テストの冗長アサーションに注記する

いずれも実装の正しさには影響しないため、蒸留へ進めてよい。1 は次の maui 変更で拾う形でも構わない。
