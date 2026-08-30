# セカンドオピニオン: add-maui-accessory-views (code-003)
**相方**: codex / **日付**: 2026-08-12 / **対象**: cycle 3 差分 (論理所有確定の SetAccessoryView 集約 + Section propertyChanged 受け皿の冪等化)
---

# Cycle 3 再レビュー結果

**判定: CHANGES_REQUESTED**  
**内訳: Critical 0 / Major 1 / Minor 0 / Suggestion 0**

過去のMajor 2件は引き続き解消しています。ただし、未接続Sectionを後から追加する経路で、cycle 3の「多重配置検査前に既存配置を奪わない」という保証が成立しないケースを確認しました。

## 過去指摘の判定

### 1. Root再構築時のHandler切断 — 引き続き解消

[KsSettingsController.cs:487](maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:487) の `DisposeRetiredViewsOf()` が、同一Viewの再実体化前に旧leaseを破棄する構造は維持されています。

cycle 3の論理所有権移動はこの退役順序に干渉しておらず、Root再構築後の新Handlerを後続破棄が切断する問題は再発していません。

### 2. Handler未接続中の論理ツリー・BindingContext未接続 — 引き続き解消

- Rootは[SettingsView.cs:67](maui/KsSettingsView.Maui/SettingsView.cs:67)から常設controllerへ渡され、Hostなしでも論理所有が確定します。
- 未接続Sectionは[Section.cs:51](maui/KsSettingsView.Maui/Section.cs:51)のfallbackで論理所有を確定します。
- Host解放ではplatform leaseのみ破棄され、論理子は維持されます。
- [AccessoryViewTests.cs:393](maui/KsSettingsView.Maui.Tests/AccessoryViewTests.cs:393)以降のテストも、初回・再接続時とも実体化前にParentとBindingContextが確定していることを直接観測しています。

## 新規指摘

### [🟠 Major] 未接続Sectionのfallbackが、追加時の多重配置検査より前に既存配置を奪う

**該当箇所**: [Section.cs:51](maui/KsSettingsView.Maui/Section.cs:51)、[KsAccessoryViewOwnership.cs:38](maui/KsSettingsView.Maui/Internals/KsAccessoryViewOwnership.cs:38)、[KsSettingsController.cs:816](maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:816)、[AccessoryViewTests.cs:313](maui/KsSettingsView.Maui.Tests/AccessoryViewTests.cs:313)

**問題点**: 次の既存テスト経路では、controllerの多重配置検査より前にSection側fallbackが動きます。

```csharp
view.RootHeaderView = accessory;

view.Root.Add(new Section
{
    HeaderView = accessory,
});
```

`new Section { HeaderView = accessory }` の評価時点ではSectionがまだcontrollerへ接続されていないため、propertyChanged callbackが直接`Reassign()`を呼びます。`accessory.Parent`は既存のSettingsViewなので冪等条件に該当せず、別SectionのBindingContextが設定された後、そのSectionへ`AddLogicalChild`されます。

MAUIの`AddLogicalChild`は子の`SetParent(this)`を呼ぶため、既存Parentから新しいOwnerへ再parentします。[MAUI Element実装](https://github.com/dotnet/maui/blob/43db9d77f2ff59999dea36ab8befb8541e919013/src/Controls/src/Core/Element/Element.cs)

その後に`Root.Add`が[KsSettingsController.cs:818](maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:818)で重複を検出しますが、既存Root accessoryのParent／継承BindingContextは既に変更済みです。ItemsSourceのDataTemplateが同じaccessoryインスタンスを再利用した場合も同じ経路になります。

現在の`AddingASectionThatReusesAPlacedViewThrows`は例外だけを確認するため、この破壊を検出できません。Section初期化中に例外になっても`Assert.Throws`は成功する点にも注意が必要です。

**推奨修正**:

- 未接続Sectionのfallbackでは、`newView.Parent`がnullまたは当該Section自身の場合だけ所有を確定し、別Ownerを持つViewにはBindingContext伝播・`AddLogicalChild`を行わない。
- SectionがSettingsViewへ登録される際は、現在どおりcontrollerの多重配置検査後に`SetAccessoryView()`で所有を確定する。
- `AddingASectionThatReusesAPlacedViewThrows`へ、例外後も既存Root配置のParent、BindingContext、native輸送が変わらないアサーションを追加する。可能ならItemsSource/DataTemplate経路も1件追加する。

前回のMinorだった論理所有寿命の乖離は`deviation.md`へ記録済みで、LeakTestsのコメントも現行fakeの責務に合わせて修正されています。提示されたMAUI 339件／0 failuresを前提とし、こちらではテスト・git操作・ファイル書き込みを行っていません。

---

## 突き合わせ結果 (ksn-orchestrator、2026-08-12)

ホスト側 review-003.md は APPROVED (Suggestion 3)、相方は CHANGES_REQUESTED (Major 1) で判定が割れた。ただし**事実認識は一致** (ホスト Suggestion 1 と相方 Major は同一現象 — 未接続 Section の fallback が多重配置検査を通らず既存配置の論理親・BindingContext を奪う)。割れたのは severity と spec 解釈 (ホスト: CellBase 既存契約と同型で spec 委譲範囲内 / 相方: オブジェクト初期化子の自然な経路で既存配置が壊れるため要修正)。事実で割れていないため相方への根拠再提示は行わず、max-review-cycles (3) 到達と合わせてオーナーに裁定を仰いだ。

**オーナー裁定 (2026-08-12): cycle 4 で修正する** — 相方の Major を採用 (fallback に Parent ガードを追加)。テスト強化 (例外後の既存配置無傷アサート・ItemsSource 経路・review-003 Suggestion 3 の検出力補強) を同梱し、修正後に独立確認を 1 回追加実施する。
