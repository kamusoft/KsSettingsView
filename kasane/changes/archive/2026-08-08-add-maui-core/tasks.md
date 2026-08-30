# Tasks: add-maui-core

## 1. プロジェクト足場

- [x] 1.1 `maui/KsSettingsView.Maui/` csproj 新設 (TargetFrameworks `net10.0;net10.0-ios;net10.0-android`、platform TFM のみ Binding csproj 参照) (→ Decision 1)
- [x] 1.2 `maui/KsSettingsView.Maui.Tests/` csproj 新設 (net10.0、dotnet-test-skill の規約に従う) (→ Decision 1)
- [x] 1.3 `maui/KsSettingsView.slnx` へ 2 プロジェクト追加

## 2. gateway 抽象

- [x] 2.1 internal gateway インターフェース定義 (Store 操作 1:1 + `updateAccessory` + lifecycle。`setTheme` は非ゴールのため含めない) と dispatcher seam (→ Decision 1, 2)
- [x] 2.2 iOS gateway 実装 (facade 型 → `KsBridge*` DTO 変換、`makeHostViewController` / `releaseHost`) (→ Decision 1)
- [x] 2.3 Android gateway 実装 (同上、`makeHostView(context)`) (→ Decision 1)
- [x] 2.4 テスト用 fake gateway (呼び出し記録 + ID 採番) と fake dispatcher (→ Decision 1, 2)

## 3. BindableObject 階層

- [x] 3.1 `CellBase` (Title / Description / HintText / IsEnabled / IsVisible) (→ Requirement: CellBase / LabelCell の公開プロパティ)
- [x] 3.2 `LabelCell` (ValueText) (→ Requirement: CellBase / LabelCell の公開プロパティ)
- [x] 3.3 `Section` (HeaderText / FooterText、`[ContentProperty(nameof(Cells))]`、`Cells: IList<CellBase>` 既定 `ObservableCollection<CellBase>`) (→ Requirement: 公開コンテナ形状 / Section header / footer テキスト)
- [x] 3.4 `SettingsRoot` 型と `SettingsView` (View 派生、`[ContentProperty(nameof(Root))]`、`Root: IList<Section>` 既定 `SettingsRoot`、RootHeaderText / RootFooterText) (→ Requirement: 公開コンテナ形状 / Root header / footer テキスト)

## 4. 変換経路 (facade 内部)

- [x] 4.1 双方向対応表 (sectionId ↔ Section / cellId ↔ CellBase、gateway 返却 ID のみ登録、setRoot / Reset で全再構築、重複配置の検出と `InvalidOperationException`) (→ Decision 3、Requirement: 同一インスタンスの重複配置の禁止)
- [x] 4.2 コレクション購読管理 (weak proxy 購読、INotifyCollectionChanged 判定、静的コレクションは初回 setRoot のみ、Root / Cells 差し替え時の再購読 + 再構築。UI スレッド契約を公開 doc コメントに明記) (→ Decision 4、Requirement: 公開コンテナ形状 / 静的コレクションの描画 / UI スレッド契約)
- [x] 4.3 構造イベントの即時 1:1 変換 (Add / Remove / Move / Replace、Reset は setRoot 再構築。除去・置換された要素の購読を同期解除し対応表から除去) (→ Requirement: 構造変更の反映 / 削除済み要素からの通知遮断)
- [x] 4.4 dirty set + dispatcher flush (可視性変更 Cell は各1件 `replaceCell`、残りは 1件 `replaceCell` / 複数 `replaceCells`。対応表に無い Cell は skip、エコー抑止フックの口) (→ Decision 2、Requirement: Cell 内容更新のバッチ配信)
- [x] 4.5 Root / Section accessory 変更の `updateAccessory` 変換 (表示中の設定・変更・null クリア。発行前に対応表の生存ガード必須) (→ Requirement: Root header / footer テキスト / Section header / footer テキスト / 削除済み要素からの通知遮断)

## 5. ItemsSource / ItemTemplate の器

- [x] 5.1 Section 配下の Cell 生成 (CreateContent + BindingContext、TemplateStartIndex、provenance 追跡、状態遷移表どおりの挙動: Template 後付け / 表示中変更 / null 化 / Move ミラー / Reset の手動分温存 / 型不正の例外) (→ Decision 3、Requirement: ItemsSource / ItemTemplate による生成)
- [x] 5.2 SettingsView 直下の Section 生成 (同一パターン) (→ Requirement: ItemsSource / ItemTemplate による生成)

## 6. Handler

- [x] 6.1 `SettingsViewHandler` (CreatePlatformView → makeHost*、DisconnectHandler → releaseHost + Handler 帰属購読の解除) (→ Decision 4、Requirement: Handler 接続時の表示反映 / 切断と再接続の復元)
- [x] 6.2 iOS の ViewController containment (親 VC 解決 → AddChild → DidMove、切断時 WillMove(null) → RemoveFromParent → releaseHost → 参照破棄) (→ Decision 4、Requirement: 切断と再接続の復元 Scenario: iOS の親子関係)
- [x] 6.3 root accessory の attach 後再適用 (両 OS 同一経路、Android は attach 検知必須) (→ Requirement: 切断と再接続の復元 Scenario: root accessory の再適用)
- [x] 6.4 `AddKsSettingsView()` (SettingsViewHandler 1件のみ登録) (→ Requirement: Handler 登録)

## 7. テスト

- [x] 7.1 変換経路ユニットテスト (構造 1:1 / Reset 再構築 / 静的コレクション非反映 / Root・Cells 差し替え / 対応表整合 / 重複配置の例外) (→ Requirement: 公開コンテナ形状 / 構造変更の反映 / 静的コレクションの描画 / 同一インスタンスの重複配置の禁止)
- [x] 7.2 バッチ配信ユニットテスト (複数変更 1 バッチ / 単発 replaceCell / 可視性変更の単発分離 / 保留中削除の安全 / flush 境界) (→ Requirement: Cell 内容更新のバッチ配信)
- [x] 7.3 通知遮断ユニットテスト (削除済み Section の HeaderText 変更が配信も例外も起こさない / 削除済み Cell の変更) (→ Requirement: 削除済み要素からの通知遮断)
- [x] 7.4 accessory ユニットテスト (Root / Section の設定・変更・null クリア・再接続後の再適用) (→ Requirement: Root header / footer テキスト / Section header / footer テキスト)
- [x] 7.5 ItemsSource ユニットテスト (生成 / BindingContext / Template 後付け / 表示中の Template・StartIndex 変更 / ミラー (Add・Remove・Replace・Move) / Reset・null 化の手動分温存 (生成区間への手動挿入含む) / 型不正の例外) (→ Requirement: ItemsSource / ItemTemplate による生成)
- [x] 7.6 リークテスト基盤 (WeakReference + GC ヘルパ) と回収テスト (切断後の Handler・platform view 回収 / 外部保持ありでの facade・gateway 回収) (→ Requirement: 切断後の資源回収、Decision 5)
- [x] 7.7 検証ホスト (maui/tests) での end-to-end 疎通確認 — LabelCell 表示・内容更新・再訪問復元 (iOS は親子関係の成立含む) を両 OS で確認。Host 解放の目視確認は best effort (→ Requirement: Handler 接続時の表示反映 / 切断と再接続の復元)
