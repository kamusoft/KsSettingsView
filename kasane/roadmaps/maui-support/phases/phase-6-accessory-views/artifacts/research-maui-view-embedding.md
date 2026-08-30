# 調査: MAUI 本体の VisualElement → native 埋め込みの公式作法

調査日: 2026-08-11 / 対象: dotnet/maui のローカルクローン (main / f27ca83a47)
調査者: ksn-scout (論点①の裏取り)

## 1. ToPlatform の契約

`ToPlatform(IElement, IMauiContext)` は内部で `ToHandler(context)` を呼ぶ薄いラッパー。Handler がなければ生成、あれば再利用。ただし既存 Handler の `MauiContext` が渡された context と異なれば捨てて作り直す。同一 context ならべき等 (同じ platform view が返る)。戻り値は `ContainerView ?? PlatformView` — WrapperView が挟まっている場合は必ずそちらが返るので、親付けは戻り値をそのまま使う。

- `src/Core/src/Platform/ElementExtensions.cs:125-136` (ToPlatform)、`:56-102` (ToHandler。67-68 で context 不一致時に破棄、94-99 で SetMauiContext → view.Handler 代入 → SetVirtualView)、`:104-123`

## 2. サイズ計測の公式経路

親レイアウト外での計測の正は `IView.Measure(widthConstraint, heightConstraint)` → `IView.Arrange(Rect)`。platform 側から呼ぶときはこれを native の計測フックに橋渡しする。

- **iOS**: 橋渡し口は `MauiView` (public abstract, `ICrossPlatformLayoutBacking`)。`SizeThatFits` → `CrossPlatformMeasure` (結果キャッシュ)、`LayoutSubviews` → `CrossPlatformArrange`。任意 IView を包む既製品 `GeneralWrapperView` は **internal** — `MauiView` + `ICrossPlatformLayout` の自前サブクラスが実質公式ルート。
  - `src/Core/src/Platform/iOS/MauiView.cs:653-685, 687-741, 603-645`、`src/Core/src/Platform/iOS/GeneralWrapperView.cs:1-71`
  - 注意: `LayoutSubviews` は「Superview が cross-platform layout backing なら measure 済みとみなしスキップ」する分岐あり (`MauiView.cs:723-740`)。
- **Android**: `IPlatformViewHandler.MeasureVirtualView(widthSpec, heightSpec)` / `LayoutVirtualView(l,t,r,b)` (internal 拡張) が MeasureSpec ↔ dp 変換込みで `IView.Measure/Arrange` を呼ぶ。`ItemContentView` (**public** ViewGroup) が `OnMeasure`/`OnLayout` から呼ぶ公開された模範 (`RealizeContent`/`Recycle` は internal なので同型自作)。
  - `src/Core/src/Handlers/ViewHandlerExtensions.Android.cs:18-77`、`src/Controls/src/Core/Handlers/Items/Android/ItemContentView.cs:82-206`

## 3. 本体内の模範実装

### (a) CollectionView ItemTemplate

- **iOS**: `TemplatedCell.Bind()` — テンプレート変更時のみ `CreateContent()` → BindingContext を先に設定 → `TemplateHelpers.GetHandler` → `SetRenderer` (旧 subview 全削除 → 新 platform view 追加 → `MarkAsCrossPlatformLayoutBacking()`) → 最後に `AddLogicalChild`。同一テンプレートなら BindingContext 差し替えのみ。計測は `PreferredLayoutAttributesFittingAttributes` で `Measure()` → `preferredAttributes.Frame` 書き換え。
  - `src/Controls/src/Core/Handlers/Items/iOS/TemplatedCell.cs:174-225, 233-246, 94-116, 118-147`、`iOS/TemplateHelpers.cs:12-26`
- **Android**: `TemplatedItemViewHolder.Bind()` → BindingContext → `PropagatePropertyChanged` → `ItemContentView.RealizeContent` (Handler 取得 → `ContainerView ?? PlatformView` → `RemoveFromParent()` してから `AddView` → `MeasureInvalidated` 購読) → `AddLogicalChild`。
  - `Android/TemplatedItemViewHolder.cs:45-88`、`ItemContentView.cs:40-53`

### (b) CollectionView Header/Footer — 今回のケースに最も近い

- **iOS**: supplementary view 機構を使わず `CollectionView.AddSubview()` + Tag 識別 + frame 手動計算。`StructuredItemsViewController.UpdateSubview()` が事実上のレシピ:

```csharp
uiView?.RemoveFromSuperview();
if (formsElement != null) ItemsView.RemoveLogicalChild(formsElement);
UpdateView(view, viewTemplate, ref uiView, ref formsElement);   // 実体化
if (uiView != null) { uiView.Tag = viewTag; CollectionView.AddSubview(uiView); }
if (formsElement != null) ItemsView.AddLogicalChild(formsElement);
RemeasureLayout(formsElement, uiView);
```

  - 実体化は `TemplateHelpers.RealizeView`: **テンプレート無しで生の View インスタンスが渡された場合は `GeneralWrapperView` で包む** (「measure/arrange できるように包む必要がある」とコメント明記)。
  - 計測は `ItemsViewController.RemeasureLayout`: 縦なら `Measure(CollectionView.Frame.Width, ∞)` → wrapper の Frame を直接書き換え。
  - `iOS/StructuredItemsViewController.cs:105-128, 78-84`、`iOS/ItemsViewController.cs:675-700, 745-766`、`iOS/TemplateHelpers.cs:28-64`
  - グループヘッダーは別経路 (`VerticalSupplementaryView : TemplatedCell` としてセルと同じ仕組み — `iOS/VerticalSupplementaryView.cs:19-32`)。
- **Android**: `StructuredItemsViewAdapter.CreateHeaderFooterViewHolder` — 生の View インスタンスなら `SimpleViewHolder.FromFormsView` → `ItemContentView.RealizeContent` → `AddLogicalChild`。
  - `Android/Adapters/StructuredItemsViewAdapter.cs:143-166, 108-112`、`Android/SimpleViewHolder.cs:57-62`

## 4. リサイクルと再親付け

同じ VisualElement の platform view を別セルへ移し替える方式ではなく、ViewHolder ごとにテンプレートから新規生成し、リサイクル時は BindingContext だけ差し替えるのが基本。再親付けするときは必ず「旧親から detach → 新親へ attach」。

単一インスタンスを複数箇所に置けない根拠 (3層):

1. platform view の superview/parent は一意 (両 OS とも再親付け前に必ず detach している)。
2. `Element.Handler` は 1 VirtualView : 1 Handler。二重設定は `InvalidOperationException("Handler is already being set elsewhere")` (`src/Controls/src/Core/Element/Element.cs:1082`)。
3. 論理ツリーの Parent も単一。重複設定は警告ログ (`Element.cs:416-419`)。

## 5. BindingContext / Parent の公式作法 (順序が決まっている)

1. `template.CreateContent()` (または既存インスタンス)
2. `PropertyPropagationExtensions.PropagatePropertyChanged(null, view, container)`
3. **BindingContext を Handler 生成より先に**設定
4. `ToHandler` / `GetHandler` で実体化 → native へ attach
5. **最後に** `container.AddLogicalChild(view)` — 先にやると一時的に親の BindingContext を継承して無駄なバインディングエラーが出る (iOS 側コメント明記)
6. 破棄時は `RemoveLogicalChild` + `BindingContext = null`

Parent 直代入ではなく `AddLogicalChild` / `RemoveLogicalChild` を使う (`Element.cs:215-226, 236-250`)。

## 6. 寿命管理

- 正は `IView.DisconnectHandlers()` (public 拡張) — ツリーを平坦化して全 `Handler?.DisconnectHandler()`。`HandlerDisconnectPolicy.Manual` の枝は打ち切り (呼び出し側管理の契約)。
  - `src/Core/src/ViewExtensions.cs:31-67`
- `GeneralWrapperView.Disconnect()` が `childView.DisconnectHandlers()` を呼ぶのが埋め込みケースの模範。
- CollectionView header/footer は view null 化時に `uiView?.Dispose(); formsElement?.Handler?.DisconnectHandler();` (`iOS/ItemsViewController.cs:757`)。
- MAUI 本体は platform view → virtual view の参照をほぼ全て `WeakReference` にしている (`MauiView._crossPlatformLayoutReference` 等)。

## 7. MeasureInvalidated / 内容変化の検知

内容変化 → `VisualElement.MeasureInvalidated` イベント → native 側で再計測要求、が公式経路。

- **Android**: `ItemContentView.RealizeContent` で購読し `PlatformInterop.RequestLayoutIfNeeded(this)` → RecyclerView の `OnMeasure` 再実行 (`ItemContentView.cs:49-52, 208-218`)。
- **iOS (header/footer)**: `OnFormsElementMeasureInvalidated` → `RemeasureLayout`。`StructuredItemsViewController` は override で `UpdateHeaderFooterPosition()` も呼ぶ (`iOS/ItemsViewController.cs:732-743`、`iOS/StructuredItemsViewController.cs:219-223`)。
- **iOS (セル)**: `IPlatformMeasureInvalidationController.InvalidateMeasure` でフラグを立て、次の `PreferredLayoutAttributesFittingAttributes` で反映 (`iOS/TemplatedCell.cs:94-116, 316-333`)。
- `src/Controls/src/Core/VisualElement/VisualElement.cs:1330, 1480-1535`

## 設計への含意 (要点)

- iOS は `MauiView` + `ICrossPlatformLayout` の自前 wrapper (GeneralWrapperView 相当) が公式ルート。Android は `ItemContentView` 同型の自作 ViewGroup。
- 手順の骨格は両 OS 共通: `PropagatePropertyChanged` → BindingContext → `ToHandler/ToPlatform` → detach → attach → `AddLogicalChild` → `MeasureInvalidated` 購読 → 破棄時に購読解除 + `RemoveLogicalChild` + `DisconnectHandlers`。
- 単一 VisualElement を複数箇所へは置けない (Handler 1:1 + platform 親一意)。
