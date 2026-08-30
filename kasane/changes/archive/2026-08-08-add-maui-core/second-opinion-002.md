# セカンドオピニオン: add-maui-core (code-review、ホスト側 review-001 対応)
**相方**: codex / **日付**: 2026-08-08 / **対象**: 実装一式 (maui/KsSettingsView.Maui + Tests + MauiHost + slnx)
**注**: second-opinion-001.md は spec-review の証跡。本ファイルが code-review 1周目 (review-001.md 対応) の証跡
---
# レビュー結果: add-maui-core

**日付:** 2026-08-08  
**判定:** **CHANGES_REQUESTED**

## サマリー

Critical 0件、Major 2件、Minor 1件です。

提示されたビルド・101件のテスト・両OS E2E成功は確認済みの前提として扱いました。指摘はいずれも現行検証で未カバーの経路です。`deviation.md` に記録済みの差分は指摘対象から除外しています。

また、`maui/macios/`、`maui/android/`、`ios/`、`android/` の既存ネイティブ／Binding資産には変更がないことを確認しました。

### [🟠 Major] iOSのViewController containment順序が設計とUIKit契約に反する

**該当箇所:**  
[maui/KsSettingsView.Maui/Platforms/iOS/SettingsViewHandler.cs:30](maui/KsSettingsView.Maui/Platforms/iOS/SettingsViewHandler.cs:30)  
[maui/KsSettingsView.Maui/Platforms/iOS/SettingsViewHandler.cs:46](maui/KsSettingsView.Maui/Platforms/iOS/SettingsViewHandler.cs:46)  
[maui/KsSettingsView.Maui/Handlers/SettingsViewHandler.cs:101](maui/KsSettingsView.Maui/Handlers/SettingsViewHandler.cs:101)  
[kasane/changes/add-maui-core/design.md:93](kasane/changes/add-maui-core/design.md:93)

**問題点:**  
設計では次の順序が明記されています。

`AddChildViewController` → `controller.View`をplatform viewとして追加 → `DidMoveToParentViewController`

実装では先に`controller.View`を返し、MAUIによるview階層への追加と`Loaded`発火を待ってから`AddChildViewController`と`DidMove...`を実行しています。つまり、子ViewControllerの登録より先にviewが階層へ入ります。

E2Eで最終的な親子関係が成立しても、appearance transitionやライフサイクル通知がUIKitの想定順序で伝播する保証がありません。

**推奨修正:**  
`MauiContext`から親ViewControllerを解決し、platform viewを返す前に`AddChildViewController`を実行してください。view階層への追加完了後にだけ`DidMoveToParentViewController`を呼びます。接続・切断・再接続について、呼び出し順序を記録するplatformテストも追加してください。

### [🟠 Major] 複数項目のItemsSource Moveで生成物が1件しか移動しない

**該当箇所:**  
[maui/KsSettingsView.Maui/Internals/KsItemsSourceBinder.cs:221](maui/KsSettingsView.Maui/Internals/KsItemsSourceBinder.cs:221)  
[maui/KsSettingsView.Maui/Internals/KsItemsSourceBinder.cs:230](maui/KsSettingsView.Maui/Internals/KsItemsSourceBinder.cs:230)  
[kasane/changes/add-maui-core/design.md:70](kasane/changes/add-maui-core/design.md:70)  
[maui/KsSettingsView.Maui.Tests/ItemsSourceTests.cs:185](maui/KsSettingsView.Maui.Tests/ItemsSourceTests.cs:185)

**問題点:**  
設計は「複数項目のイベントも項目ごとに適用」と規定していますが、`MirrorMove`は`OldStartingIndex`位置の1要素だけを移動し、`OldItems.Count`を参照していません。

複数項目を含む`NotifyCollectionChangedAction.Move`を発行するItemsSourceでは、ItemsSourceの並びと`_generated`／生成先コレクションが不一致になります。既存テストは`ObservableCollection.Move`による単一項目移動だけなので、この不具合を検出できません。

同様に直接的な範囲Moveに対して、Rootは1回だけ`MoveSection`を呼び、Cellsは各要素を同じ`newIndex`へ移動しているため、順序を保持できません。

**推奨修正:**  
移動対象をブロックとして取り出し、前方・後方のどちらでも元の要素順を維持する範囲Move処理にしてください。少なくとも以下のテストが必要です。

- SettingsViewとSectionのItemsSource
- 前方／後方への複数項目Move
- 生成区間内に手動要素がある場合
- 生成コレクション、対応表、Gateway上の順序がすべて一致すること

### [🟡 Minor] 初回接続時の検証例外がSettingsViewを不完全な「接続済み」状態にする

**該当箇所:**  
[maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:90](maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:90)  
[maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:194](maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:194)  
[maui/KsSettingsView.Maui/SettingsView.cs:182](maui/KsSettingsView.Maui/SettingsView.cs:182)

**問題点:**  
`Connect`は`_gateway`と`_dispatcher`を設定してからツリーの重複検証を行います。初期ツリーが不正で`RebuildRoot`が例外になると、Gatewayは保持されたまま、対応表とコレクション購読は構築されません。

その後モデルを修正して再接続しても、`ConnectGateway`は既存Gatewayを返すだけです。したがって、空または未構築のStoreを使い続け、以後の構造変更も追跡されない状態になります。失敗したGatewayの破棄も行われません。

**推奨修正:**  
接続処理をトランザクション化してください。初期構築に失敗した場合は、登録・購読を解除し、`_gateway`と`_dispatcher`を未接続状態へ戻し、所有権に従って失敗したGatewayを破棄します。「接続失敗 → ツリー修正 → 再接続」で正常に構築・購読されるテストも追加してください。

## アクションプラン

1. iOS containmentの接続順序を修正する。
2. 複数項目Moveを順序保持する範囲操作として実装し、テストを追加する。
3. 初回接続失敗時のロールバックと再接続テストを追加する。
4. 既存のビルド・ユニットテスト・両OS E2Eを再実行する。

## 突き合わせ結果 (vs review-001.md: APPROVED / Minor 3 / Suggestion 4)

両者の指摘に重複なし。相方のみの指摘3件を根拠検証:

| 指摘 | 採否 | 判定根拠 |
|---|---|---|
| Major: iOS containment 順序が design と UIKit 契約に反する | **採用** | design.md Decision 4 の明記順序 (AddChild → view 追加 → DidMove) と実装の乖離は事実。UIKit の containment 規約 (子 VC 登録前の view 追加) にも反する |
| Major: 複数項目 Move で生成物が1件しか移動しない | **採用** | design「複数項目のイベントも項目ごとに適用」と MirrorMove 実装 (OldItems.Count 不参照) の乖離はコードで確認可能。ObservableCollection は単一 Move しか発行しないため既存テストでは検出不能という指摘も正しい |
| Minor: 初回接続の検証例外で不完全な接続済み状態になる | **採用** | gateway 保持 + 購読未構築のまま再接続不能になる実害シナリオが具体的 (重複検出は接続時に発火し得る) |

- 確定 (双方一致): 0件 / 採用 (相方のみ・根拠強): 3件 / 降格: 0件 / 未解決: 0件
- 総合判定: 採用 Major 2件により **CHANGES_REQUESTED** として修正サイクルへ (ホスト側 Minor 1 (IDisposable 未履行)・Minor 3 (SHALL テスト欠落) も同サイクルで修正。Minor 2 (NU1608) はパッケージングへ申し送り)
