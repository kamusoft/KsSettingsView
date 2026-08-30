# レビュー結果: add-maui-accessory-views (002 回目)

**日付**: 2026-08-12
**判定**: CHANGES_REQUESTED

## サマリー

前回サイクルの指摘は**5 件すべて解消済み**で、修正の質も高い。特に相方 Major 2 件への対処 — 論理所有を `KsAccessoryViewOwnership` として platform lease から分離したこと、`Materialize` の直前に `DisposeRetiredViewsOf` を挟んで同一 View の Handler 巻き添え切断を防いだこと — はどちらも構造として正しく、fake の Handler 1:1 共有化によって回帰テストで固定されている。全 platform green (iOS 476 / Android 2280 / MAUI 328、いずれも failures 0)、comment-policy lint 0 件。

一方、修正で入った新しい構造に **2 件の問題**を見つけた。いずれも実測で確認済み。(1) 依頼の重点確認 (多重配置例外時の論理親) は Section 方向では**問題なし**と確定したが、**Root 方向では実際に起きる** — 例外後に、罪のない Section accessory の論理親と BindingContext が SettingsView に奪われ、復旧不能な状態になる。(2) Section accessory の実体化が論理所有・BindingContext の確定より**先に**走っており、design.md Decision 1 が明示する生成順序 (tasks 3.4) と食い違ううえ、`Materialize` の doc comment がその逆を主張している。Root 側は正しい順序で動いているため、同じ契約が対象によって非対称になっている。

## 指摘事項

### [🟠 Major] Root 方向の多重配置例外が、既存の有効な Section accessory の論理親と BindingContext を奪う

**該当箇所**: `maui/KsSettingsView.Maui/SettingsView.cs:798-802` (`OnRootAccessoryViewChanged`) / `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:406-428` (`SetAccessoryView`) / `maui/KsSettingsView.Maui/Internals/KsAccessoryViewOwnership.cs:25-40`

**問題点**:

`OnRootAccessoryViewChanged` は `KsAccessoryViewOwnership.Reassign` を**先に**呼び、その後で `_controller.SetRootAccessoryView` を呼ぶ。多重配置の検出は controller 側 (`EnsureAccessoryViewIsNotPlaced`) にあるため、例外が飛ぶ時点では既に論理親の付け替えが完了している。

実測 (一時プローブテストで確認、確認後に削除済み。手順は「確認した観点」に記載):

```
[root] before Parent=Section        Text=section-ctx
[root] thrown=InvalidOperationException: The same View instance cannot be placed more than once.
[root] after  Parent=SettingsView   Text=root-ctx
[root] view logical=1  section logical=1
[root] recov (RootHeaderView=null 後) Parent=SettingsView Text=root-ctx
[root] recov after section ctx change Text=section-ctx-2
[root] section accessory still transported? True
```

Section の header に正しく置かれ、native へも輸送済みの View に対し、別の場所 (`RootHeaderView`) へ誤って設定しただけで:

- `Parent` が `Section` → `SettingsView` へ移り、**両方の論理子リストに同時に載る** (view logical=1 / section logical=1)
- 継承 BindingContext が Section のもの (`section-ctx`) から SettingsView のもの (`root-ctx`) へ**すり替わる**。この Section accessory は表示され続け、native への輸送も生きたままなので、**Section header が誤った文脈のデータを表示する**
- 例外を握って `RootHeaderView = null` で後始末しても `Parent` は `SettingsView` のまま戻らず、復旧できない

これは spec が「例外後のプロパティ状態」として既存契約へ委ねている範囲を越えている。壊れるのは失敗した配置ではなく、**何も間違っていない側の既存配置**であり、`specs/maui-core/spec.md` の Requirement「accessory View は所有者の BindingContext を継承する」(Section accessory は所有 Section から継承する SHALL) が例外後に破れる。しかも例外を捕捉すればそれ以上のエラーは出ない (silent)。

なお Section 方向 (Section の header に置いた後に同じ View を Root へ、ではなくその逆) は**問題ない**。MAUI は `INotifyPropertyChanged.PropertyChanged` を BindableProperty の `propertyChanged` コールバックより**先**に発火させる (実測: `[order] INotify:Probe -> propertyChanged`) ため、Section 側では controller が先に例外を投げ、`Reassign` は走らない。つまり本件は Root と Section で経路が非対称であることに起因する。

**推奨修正**:
`OnRootAccessoryViewChanged` で `Reassign` より前に多重配置を判定する (controller に検査だけを行う入口を設け、`_placedViews` を引いて重複なら先に送出する)。副作用のない検査を先に置けば、例外後の論理ツリーは無傷のまま「プロパティだけが新しい値になっている」既存契約と一致する。あわせて Section 方向と Root 方向で例外時の状態が揃うことを固定するテストを足す (現在の `PlacingTheSameViewTwiceThrows` / `AddingASectionThatReusesAPlacedViewThrows` はいずれも Section 方向のみで、例外後の状態を見ていない)。

### [🟡 Minor (優先度: 高)] Section accessory の実体化が論理所有・BindingContext の確定より先に走る

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:459-480` (`Materialize` とその `<remarks>`) / 同 `:1032-1041` (`HandleSectionPropertyChanged` の View 分岐) / `maui/KsSettingsView.Maui/Section.cs:45,56` (`propertyChanged` の `Reassign`)

**問題点**:

`Materialize` の doc comment は次を保証すると述べている。

> 論理ツリーへの接続と BindingContext の継承は所有者が受け持つため、実体化の時点では既に確定している (`KsAccessoryViewOwnership`)。

Section accessory では成り立っていない。`Section.HeaderView` の `Reassign` は BindableProperty の `propertyChanged` にあり、controller は `INotifyPropertyChanged` 経由で通知を受けるため、**controller の `Materialize` が先、`Reassign` が後**になる (前掲の発火順の実測より)。

実測 (同じ一時プローブ。`IKsViewMaterializer` を差し替えて実体化の瞬間の状態を記録):

```
[mat] -- set RootHeaderView --
[mat] Materialize view=Label Parent=SettingsView BindingContext=Owner Text=ctx
[mat] -- set Section.HeaderView --
[mat] Materialize view=Label Parent=null        BindingContext=null   Text=null
[mat]    after: Parent=Section Text=ctx
```

Root は正しい順序、Section は `Parent=null` / `BindingContext=null` のまま `Materialize` に入る。実 platform の `KsViewMaterializer` は `Materialize` の中で `view.ToPlatform(context)` (= `ToHandler`) を呼ぶ (`Platforms/iOS/KsAccessoryHostView.cs:41` / `Platforms/Android/KsAccessoryHostView.cs:39`) ため、**Handler が未バインド・未伝播の状態で生成される**。これは design.md Decision 1 が採用案として明記する生成骨格

> `PropagatePropertyChanged` → BindingContext を Handler 生成より先に設定 → `ToHandler` → detach → attach → 最後に `AddLogicalChild`

および tasks 3.4 (チェック済み) と食い違う。deviation.md にも記録がない。

発生条件は「接続・取り付け済みの状態で `Section.HeaderView` / `FooterView` を実行時に設定する」経路に限られる (XAML 構築時や `RegisterSection` 経由の `PlaceSectionAccessoryViews` では、View は既に所有者へ結び付いている)。実害としては Handler 生成時に既定値でマッパが走り直後に再マップされる分の無駄と、`PropagatePropertyChanged` 前に生成されることによる FlowDirection・親スコープのリソース/暗黙 Style の遅れが想定される。バインド解決後に `MeasureInvalidated` が発火して測り直しへ届くため高さは収束するが、「所有が確定してから実体化する」という Decision 1 の前提そのものが崩れている点が問題で、現行テストは fake seam が実体化時の状態を観測しないため検出できない。

**推奨修正**:
Section でも controller が `Materialize` する前に所有が確定するようにする (例: `Reassign` を `propertyChanging` 側へ移す、あるいは controller の `SetAccessoryView` から所有確定を呼び出す形にして Root と経路を揃える。ただし未接続時は controller を通らないため、所有確定が controller 依存にならない構造は維持する必要がある)。あわせて、テスト用 materializer が実体化の瞬間に `Parent` / `BindingContext` を観測できるようにして 4 対象で固定する。順序を変えられない事情があるなら、`Materialize` の `<remarks>` を実際の保証内容へ書き直したうえで deviation.md に Decision 1 からの逸脱として記録する (この場合は NEEDS_DISCUSSION 相当の裁定事項)。

## 前回指摘の解消確認

| 前回指摘 | 状態 |
|---|---|
| review-001 Minor 1: アサーション皆無のプローブテスト | **解消**。`AccessoryViewSwapProbeTest.kt:199-205` が `assertThrows(IllegalStateException::class.java)` で例外型を固定し、テスト名も事実を述べる形になった |
| review-001 Minor 2: `LeakTests` の `Handler` トートロジー | **解消**。当該アサーションは削除され、コメントが「退役の対象は platform 実体であり、Handler 切断の確認は platform 実装側の担当で net10.0 の検証範囲外」と実際の内容に書き直された |
| review-001 Suggestion 1: プローブテストの `println` 9 箇所 | **解消**。両ファイルとも `println` は 0 件 |
| review-001 Suggestion 2: `AttachViews` の `<remarks>` | **解消**。`KsSettingsController.cs:234-240` が「口が差し込まれるのは Host を作った後であり、それ以前に置かれた View は `ApplyAccessories` でまとめて実体化する」という実装どおりの記述になった |
| second-opinion Major 1: Root 再構築時に旧 lease が新 wrapper の Handler を切断 | **解消**。`Materialize` (`:476`) が `DisposeRetiredViewsOf(placement.View)` で同一 View の退役実体を先に破棄する。回帰テスト `RebuildingTheRootKeepsTheNewAccessoryHandlerConnected` / `ReattachingTheHostKeepsTheNewAccessoryHandlerConnected` が追加され、`FakeViewMaterializer` が View ごとの Handler 1:1 共有と切断を再現するようになったため検出力がある (共有を外すと落ちる構造) |
| second-opinion Major 2: Handler 未接続中の論理ツリー・BindingContext 継承 | **解消**。`KsAccessoryViewOwnership` として所有者側へ分離され、`ReleaseHost` は platform lease だけを破棄する。`AccessoryInheritsTheBindingContextWithoutAHost` / `BindingContextChangeReachesTheAccessoryWhileTheHostIsReleased` / `AccessoryStaysInTheLogicalTreeWhileTheHostIsReleased` / `ClearingTheViewRemovesItFromTheLogicalTree` が追加されている。ただし分離の結果として上記 Major / Minor の順序問題が生じている |

review-001 Suggestion 3 (蒸留への申し送り) は本 change の修正対象外であり、そのまま蒸留時の宿題として残る。

## 確認した観点 (指摘に至らなかったもの)

- **ビルド・テスト**: iOS `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` = 476 tests / 0 failures、Android `./gradlew test --rerun-tasks` = 2280 tests / 0 failures (XML 集計)、MAUI `dotnet test` = 328 tests / 0 failures。`python3 scripts/comment-policy-lint.py` = 禁止 0 件 (検査対象 573 ファイル)
- **実測に使った一時プローブ**: `maui/KsSettingsView.Maui.Tests/ZzProbeTests.cs` を新規作成して 4 件のプローブ (多重配置の Section 方向 / Root 方向、`propertyChanged` と `INotifyPropertyChanged` の発火順、実体化時の `Parent` / `BindingContext` 記録) を実行し、確認後に削除済み。実装コードには一切手を入れていない。`git status` で残存ファイルなしを確認済み
- **足場の非改変**: `kasane/changes/` 配下で追跡対象の変更は tasks.md のチェックボックスのみ (proposal / design / specs 3 本は無改変)。deviation.md の 2 件は合意済み差分として指摘から除外した
- **`DisposeRetiredViewsOf` の安全性**: 「native への配信を終えてから破棄する」不変条件を崩していないことを全経路で追跡した。`_retiredViews` へ積まれるのは `RetireAccessoryView` 経由のみで、その後 `SetRoot` / `RemoveSection` / `ReplaceSection` のいずれかが native へ届いた後にしか `Materialize` は走らない。実際に同一 View の退役実体が見つかるのは Root 再構築で同じ Section が残る経路だけである
- **`SetAccessoryView` の局所 `retired` との二重管理**: 局所変数の退役リースは `_retiredViews` に載らないため `DisposeRetiredViewsOf` の対象外だが、同一インスタンスの再設定は BindableProperty の同値 no-op で到達しないため衝突しない
- **退役順序 (design Decision 5)**: 「Store 更新 → native 配信 → 旧実体破棄」は `SetAccessoryView` / `RetireAccessoryView` + `DisposeRetired` / `ReleaseAccessoryViews` の全経路で維持されている。`PreviousViewIsDisposedAfterTheNewOneIsDelivered` / `...AfterTheClearIsDelivered` が破棄時点の配信件数を観測しており検出力がある
- **リークテストの追加分**: 差し替え / null 化 / Section 削除 / Root 再構築 / Host 解放 / Root accessory の 6 経路で `GcProbe.AssertCollected` が張られ、`FakeViewMaterializer.Forget` で観測側の強参照を切っている。tasks 6.2 の範囲を満たす
- **多重配置検出の 3 経路**: `_placedViews` による slot 単位の検出、Root accessory を先に数える `EnsureTreeHasNoDuplicates`、追加経路の `EnsureAccessoryViewIsFree` は揃っており、null 解除後の再利用も許容される。指摘 Major は検出漏れではなく検出**後**の状態の話である
- **native 側の追加 API**: iOS `invalidateAccessoryMeasurement` は `UICollectionViewLayoutInvalidationContext.invalidateSupplementaryElements` で対象限定、Android は container 配下の hosted view へ `requestLayout` を出す形で、いずれも対象不在時は no-op。Store 側は一過性通知 (replay 0 / PassthroughSubject) で復元可能状態を汚していない。Bridge の `KsBridgeAccessoryView` は両 OS とも返却前 detach を実装しており、deviation.md の合意どおり
- **sample-parity**: `MauiSpecific` 区分の新設は sample-parity.md の例外の適用拡張として design Decision 6 に明記済み (蒸留時に規約へ明文化する申し送りあり)。既存デモ項目の文言・構成は無改変

## アクションプラン

1. `OnRootAccessoryViewChanged` で多重配置の検査を `Reassign` より前に行い、例外後に他の有効な配置の論理親・BindingContext が壊れないようにする。Root / Section 双方向で例外後の状態を固定するテストを追加する (優先度: 高)
2. Section accessory の実体化を論理所有・BindingContext 確定の**後**に回す (または順序を変えない裁定を取り、`Materialize` の `<remarks>` の修正と deviation.md への記録を行う)。実体化時の `Parent` / `BindingContext` を観測できるテストを追加する (優先度: 高)
3. 蒸留時の宿題 (review-001 Suggestion 3 の 4 点: `maui-facade.md` L30 / L74、sample-parity.md、iOS superview 剥がしの ADR 反映先) は引き続き有効 (本 change の修正対象外)
