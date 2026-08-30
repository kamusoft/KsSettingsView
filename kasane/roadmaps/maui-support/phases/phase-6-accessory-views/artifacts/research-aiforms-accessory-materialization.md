# 調査: AiForms 原典の HeaderView / FooterView 実体化機構

調査日: 2026-08-11 / 対象: ../AiForms.Maui.SettingsView (ローカルクローン)
調査者: ksn-scout (論点①の裏取り)

## 前提: 原典のアーキテクチャ (KsSettingsView との差)

- 原典は MAUI handler 方式 (`ViewHandler<SettingsView, AiTableView/AiRecyclerView>`)。ただし内部は Xamarin renderer 時代の設計をほぼそのまま移植しており、iOS は `UITableView` + `UITableViewSource`、Android は `RecyclerView` + `Adapter` を自前で駆動する (MAUI の layout パイプラインには乗せない)。
- HeaderView/FooterView は Section 単位のみ (`Section.HeaderView` / `FooterView`)。Root 全体の Header/Footer View は存在しない (`SettingsView/SettingsRoot.cs:10`)。
- VisualElement の実体化は「MAUI の親子ツリーに繋がない裏技」で行う: `Parent` を `Application.Current.MainPage` に付け替え、`FindMauiContext()` で context を横取りする。

## 1. 実体化経路

**iOS**: `CustomHeaderFooterView.UpdateCell()` が入口。既に Handler があればそれを流用し、無ければ `_virtualCell.ToHandler(findMauiContext)`。IMauiContext は `View.FindMauiContext()` (自分→親チェーンで最初に Handler.MauiContext を持つ要素、最後は `Application.Current`) から取得。

- `SettingsView/Native/iOS/CustomHeaderFooterView.cs:158-237` (UpdateCell)、`:285-304` (`GetNewHandler`→`ToHandler`)、`:75` (`_mauiContext => _virtualCell.FindMauiContext()`)
- `SettingsView/Extensions/ViewExtension.cs:8-20` (FindMauiContext 実装)
- 呼び出し元: `SettingsView/Native/iOS/SettingsTableSource.cs:288-294` ← `GetViewForHeader` `:175-180` / `GetViewForFooter` `:254-257`
- reuse 登録: `SettingsView/Native/iOS/AiTableView.cs:46-49` (`RegisterClassForHeaderFooterViewReuse`)

**Android**: `HeaderFooterContainer` (FrameLayout) が入口。`CreateNewHandler` で `_contentView.ToPlatform(view.FindMauiContext())` → `AddView`。

- `SettingsView/Native/Android/HeaderFooterContainer.cs:178-188` (`CreateNewHandler`, `ToPlatform`)、`:111-167` (`UpdateCell`)
- 呼び出し元: `SettingsView/Native/Android/SettingsViewRecyclerAdapter.cs:448-453` (`BindCustomHeaderFooterView`) ← `OnBindViewHolder` `:233-238`
- ViewHolder 生成: 同 `:176-183`。ViewType 判定は `ModelProxy.cs:136,158` (`section.HeaderView == null ? TextHeader : CustomHeader`)

## 2. サイズ計測

**iOS**: `GetHeightForHeader/Footer` は HeaderView がある場合 `UITableView.AutomaticDimension` を返すだけ (`SettingsTableSource.cs:152-154, 231-234`)。実際の高さは C# 側で `VisualElement.Measure(tableView.Frame.Width, ∞)` を呼び、その結果を NSLayoutConstraint (HeightAnchor, Priority 999) として platform view に貼ることで AutoLayout に伝える。幅は `HorizontalOptions == Fill` なら tableView 幅で上書き。

```csharp
var result = _virtualCell.Measure(tableView.Frame.Width, double.PositiveInfinity);
var finalH = (float)result.Height;
_heightConstraint = handler.PlatformView.HeightAnchor.ConstraintEqualTo(finalH);
_heightConstraint.Priority = 999f;
_heightConstraint.Active = true;
```

`SettingsView/Native/iOS/CustomHeaderFooterView.cs:239-260`。上下左右は ContentView へ 4 辺 anchor で固定 (`:306-317`)。

**Android**: `HeaderFooterContainer.OnMeasure` をオーバーライドし、幅は MeasureSpec から、高さは `_viewHandler.VirtualView.Measure(dpWidth, ∞)` の結果を px 変換して `SetMeasuredDimension`。`OnLayout` は `_viewHandler.LayoutVirtualView(l,t,r,b)`。ViewHolder 側は `LayoutParams(-1, -2)` = wrap_content。
`SettingsView/Native/Android/HeaderFooterContainer.cs:62-91`、`ViewHolders.cs:81-96`

## 3. BindingContext / Parent 伝播

- BindingContext: `Section.OnBindingContextChanged()` で `HeaderView.BindingContext = BindingContext` を直代入 (`SettingsView/Section.cs:31-42`)。`SetInheritedBindingContext` ではない (Cell は `SectionBase.cs:153-176` で inherited 版を使う)。
- Parent: `SettingsView.OnCollectionChanged` / `OnModelChanged` で `section.HeaderView.Parent = this` (`SettingsView/SettingsView.cs:147-154`, `:196-204`)。
- ただし Android は実体化直前に Parent を上書きする: `view.Parent = Application.Current.MainPage;` (`HeaderFooterContainer.cs:116`、CustomCell 側も `FormsViewContainer.cs:145`)。GestureRecognizer バグ (dotnet/maui#17948) の回避策。iOS 側は同じ行がコメントアウト済み。

## 4. 更新セマンティクス

- プロパティ差し替え: `SettingsView.SectionPropertyChanged` を handler/adapter が購読。iOS は `HeaderViewProperty` 変更で `PlatformView.UpdateSectionNoAnimation(section)` (`Handlers/SettingsViewHandler.iOS.cs:132-146`)。Android は `NotifyItemChanged(index)` (`SettingsViewRecyclerAdapter.cs:71-90`, `:117-127`)。どちらもセクション再バインド経由で `UpdateCell` が再走する。
- 内容変化 (内部): iOS のみ能動対応。`UpdateCell` の最後で `_virtualCell.MeasureInvalidated` と全 descendant の MeasureInvalidated / Layout.SizeChanged を購読し (`CustomHeaderFooterView.cs:262-272`、descendant 列挙はリフレクションで `Element.Descendants()` — `CustomCellContent.cs:18,332-335`)、発火時に 100ms デバウンス → `ForceLayout()` で再 Measure・HeightConstraint 貼り直し → `tableView.BeginUpdates()/EndUpdates()` (`:319-376`)。
- Android には対応する再計測経路が実質無い: `MeasureInvalidated` を購読しているがハンドラは空 (`HeaderFooterContainer.cs:190-193`)。`OnMeasure` が毎回 Measure を呼ぶ設計なので、RecyclerView の再レイアウトが走れば追従する形。

## 5. CustomCell との共有度

コード共有はゼロ (コピー分岐)。ロジックはほぼ同型だが別クラス。

| | header/footer | custom cell |
|---|---|---|
| iOS | `CustomHeaderFooterView` | `CustomCellContent` (`Native/iOS/Cells/CustomCellContent.cs`) |
| Android | `HeaderFooterContainer` | `FormsViewContainer` (`Native/Android/FormsViewContainer.cs`) |

差分: CustomCell 側は `IsMeasureOnce` 計測キャッシュ、`ShowArrowIndicator`/`UseFullSize` 幅補正、`IsForceLayout` フラグを持つ。header/footer 側には無い。

リサイクル時の platform view 扱い (両者同アルゴリズム):

1. 新 View が既に Handler を持つ → その Handler を流用し、`RemoveFromSuperview()` / `RemoveFromParent()` で前の親から剥がして再親付け。
2. 保持中 Handler の型 == `GetHandlerType(view.GetType())` → `handler.SetVirtualView(newView)` で差し替え再利用。
3. 型が違う → `DisposeHandlersAndChildren()` / `DisconnectHandler()` して新規生成。

`CustomHeaderFooterView.cs:194-237` / `HeaderFooterContainer.cs:125-167`。TODO コメントが明言する通り、Content 部分は仮想化されておらず Cell 数ぶん native view が生存する。

## 6. 寿命管理

- 自動切断: `ShouldAutoDisconnect` 有効時、親 Page の `Unloaded` で `DisconnectHandler()` (`Extensions/HandlerCleanUpHelper.cs:8-35`)。
- iOS: `CustomHeaderFooterView.Dispose` で購読解除 + `DisposeModalAndChildHandlers()`。注目: `UpdateCell` 内の旧 Cell 側では Handler を意図的に切らない — `// Do not disconnect the Handler here as it may not redraw.` (`:171-172`)。再描画不良を優先してリークを許容。
- Android: `AiRecyclerView.Dispose` で全 Section の HeaderView/FooterView に `DisposeChildView` (`DisconnectHandler()` → `RemoveFromParent()` + `Dispose()`) (`Native/Android/AiRecyclerView.cs:109-123`, `:172-183`)。

## 設計への含意

- iOS = AutoLayout height constraint + AutomaticDimension、Android = カスタム FrameLayout の OnMeasure で VisualElement.Measure、という 2 系統の非対称実装。共通ヘルパー無し。
- 動的内容変化への追従は iOS のみ (MeasureInvalidated + デバウンス + BeginUpdates/EndUpdates) で、最も再現の手間がかかる箇所。
- `Parent = Application.Current.MainPage` とリフレクション `Descendants()` は MAUI 内部依存で壊れうる (原典自身が TODO 明記)。踏襲すべきでない負債。
