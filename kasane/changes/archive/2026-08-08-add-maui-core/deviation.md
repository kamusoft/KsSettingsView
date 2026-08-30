# Deviation: add-maui-core

spec からの乖離、および spec が沈黙する領域で確定した利用者可視の実装判断の記録 (lessons/process L-001)。いずれもオーケストレーター判断でオーナーへ報告済み (2026-08-08)。

- **CellBase / Section の基底型**: spec/design 未指定 → `Element` 派生とした (BindableObject 直下だと XAML 親からの BindingContext 伝播が効かないため。Element は BindableObject の派生であり proposal の「BindableObject 階層」と矛盾しない) (2026-08-08)
- **Title の null 解決**: spec 未定義 → `Title` の既定は `string.Empty`、null 代入時は空文字として輸送・表示する (Bridge DTO が非 null title を要求するため) (2026-08-08)
- **XAML 直置き要素への BindingContext 伝播**: spec は BindingContext 伝播に沈黙 → XAML で使えるという変更ゴールに直結する利用者可視挙動のため追加する。方式は `Element.Parent` 配線**ではなく** `BindableObject.SetInheritedBindingContext` による階層明示配布 — `Parent` は子→親の強参照になり spec の SHALL「外部保持があっても facade は回収される」を破り、かつ親 BindingContext の後続変更は logical children 経由でしか伝播せず狙いも達成できないため。ItemsSource 生成物の明示 BindingContext は上書きしない。トレードオフ: Section / Cell は logical tree に載らず `x:Reference` / `DynamicResource` は届かない (本変更のスコープ外) (2026-08-08 実装中に方式確定)
- **Cells の Reset の配信形**: spec は「`Reset` は Root 全体の再構築」とのみ記述 → Section 配下の `Cells.Clear()` (Reset) も文言どおり `setRoot` 全再構築で配信する (`replaceSection` で Section 内に閉じる案は採らず) (2026-08-08)
- **重複配置例外の後状態**: spec は「表示は変化しない」のみ要求 → `InvalidOperationException` 送出後もモデル側コレクションには要素が残る (`ObservableCollection` は追加後にイベントを出すため巻き戻し不可) (2026-08-08)
- **DataTemplateSelector 非対応**: spec 未記述 → `ItemTemplate` に `DataTemplateSelector` を渡す利用は本変更では非対応 (対応は後続フェーズの判断) (2026-08-08)
- **iOS の親 ViewController 解決方式**: design Decision 4 は「`MauiContext` から親 ViewController を解決」と記述 → 実装は VirtualView の祖先 Page の handler (`IPlatformViewHandler.ViewController`) から解決し、不能時のみ responder chain 探索 (Host 自身の VC は除外) へフォールバック。理由: MauiContext からは window root VC しか取れず Navigation 配下の Page VC を指せないため、実装側が containment の意図 (直近の親 Page への embed) に正しい (2026-08-08 review-002 Minor 1 の記録要請による)
