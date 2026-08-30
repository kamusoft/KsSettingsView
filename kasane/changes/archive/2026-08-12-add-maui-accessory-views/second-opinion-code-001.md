# セカンドオピニオン: add-maui-accessory-views (code-001)
**相方**: codex / **日付**: 2026-08-12 / **対象**: 未コミット working tree 全体 (native 再計算口 + Bridge 新 API + MAUI facade view accessory 機構 + サンプル + テスト)
---

# レビュー結果: add-maui-accessory-views（001 回目）

**日付**: 2026-08-12
**判定**: **CHANGES_REQUESTED**

指定どおり静的レビューのみ実施し、提示された全テスト成功結果を前提としました。合意済みの `deviation.md` 2 件は指摘から除外しています。ファイルは作成していません。

## 指摘事項

### 🟠 Major: Root 再構築時に新しい wrapper の Handler を旧 lease が切断する

**該当箇所**: maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:683、同:688、同:694、maui/KsSettingsView.Maui/Platforms/iOS/KsViewMaterializer.cs:40、maui/KsSettingsView.Maui/Platforms/Android/KsViewMaterializer.cs:37

**問題点**: 同じ `Section` を含む新しい Root コレクションを設定すると、旧 lease は退役待ちへ積まれたまま、同一 accessory `View` が先に再 materialize されます。MAUI の Handler は `VisualElement` と 1:1 なので、新 wrapper の `ToPlatform` は旧 Handler／platform view を再利用します。その後 `DisposeRetired()` が旧 lease を破棄し、`view.DisconnectHandlers()` により、新 wrapper が使用中の Handlerまで切断します。

この経路は maui/KsSettingsView.Maui.Tests/AccessoryViewTests.cs:448 で実行されていますが、fake materializer は各 lease に独立した目印を作るだけで、Handler 1:1 の共有を再現しないため検出できません。再構築後に表示が残っていても、バインディング更新・計測・入力処理が切れた platform view になる可能性があります。

**推奨修正**: 同一 `View` を再 materialize する前に旧 lease の Handler を確実に切断するか、Root 再構築後も残る同一 `Section` の lease を維持してください。少なくとも実プラットフォーム相当の fake を使い、「旧 lease の破棄後も新 lease の Handler が接続済み」であることを検証する回帰テストが必要です。

### 🟠 Major: Handler 未接続中は accessory View が論理ツリーにも BindingContext にも接続されない

**該当箇所**: maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:469、同:486、同:534

**問題点**: `AddLogicalChild` と継承 `BindingContext` の設定が `Materialize()` 内にあり、materializer がない場合は何も行われません。そのため、典型的な Handler 生成前の XAML 構築時に accessoryを設定しても、その View は所有者の `BindingContext` を継承しません。また `ReleaseHost()` は `Detach()` 経由で論理ツリーからも外すため、切断中の所有者の `BindingContext` 変更は accessoryへ即時伝播しません。

これは delta spec の「accessory View は論理ツリーに接続され、所有者の BindingContext を継承する」という無条件の SHALL 契約に反します。現在のテストは接続済み状態の初期伝播と再接続後の最終状態しか確認していません。

**推奨修正**: 論理所有と platform lease の寿命を分離してください。accessoryプロパティへの配置時に、Handler の有無にかかわらず所有者との論理関係と継承 Context を確立し、Host 解放時は platform lease／Handlerだけを破棄します。論理ツリーから外すのは、プロパティの解除・差し替え・所有 Section の削除時に限定します。Handler 未生成時、および Host 解放中の BindingContext 変更を確認するテストも追加してください。

## アクションプラン

1. Root 再構築時の旧 lease と同一 `VisualElement` 再 materialization の順序を修正する。
2. accessory の論理所有を Host 世代から分離する。
3. 実 Handler の共有・切断を模した回帰テストと、Handler 未接続中の BindingContext テストを追加する。

**件数**: Critical 0 / Major 2 / Minor 0 / Suggestion 0

---

## 突き合わせ結果 (ksn-orchestrator、2026-08-12)

ホスト側 review-001.md (Minor 2 / Suggestion 3) との突き合わせ。双方一致の指摘はなし、矛盾もなし。

| 指摘 | 採否 | 根拠 |
|---|---|---|
| Major 1: Root 再構築時に旧 lease の `DisconnectHandlers` が再 materialize 済み同一 View の新 Handler を切断する | **採用** | 該当箇所特定 (KsSettingsController.cs:683-694 / 両 KsViewMaterializer) + Handler 1:1 再利用による実害シナリオが具体的。fake materializer が Handler 共有を再現しないため既存テストで検出不能という指摘も妥当 |
| Major 2: Handler 未接続中 (XAML 構築時・Host 解放中) は accessory View が論理ツリー・BindingContext 継承に接続されない | **採用** | specs/maui-core/spec.md L53 の SHALL は無条件 (Handler 接続の限定句なし) であることを文言確認。`Materialize()` 内に `AddLogicalChild` / 継承設定が同居しているという構造指摘も該当箇所と一致 |

採用 2 件はホスト側指摘と同格として修正サイクル (cycle 2) へ。降格・未解決なし。
