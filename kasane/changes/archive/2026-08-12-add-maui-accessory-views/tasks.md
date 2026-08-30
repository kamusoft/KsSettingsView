# Tasks: add-maui-accessory-views

## 1. 先行検証 (実装冒頭 — design.md の Risks 2件の確定)

- [x] 1.1 iOS: accessory 内の view の制約変化が UICollectionView self-sizing の行高さ再計算まで届くかを最小構成で検証する。届かない場合は native 側の再計算の口の要否と形を確定し、オーナーに提示する (→ Requirement: View の差し替えと内容変化が表示に反映される / Scenario: サイズが変わる内容変化に領域高さが追従する)
- [x] 1.2 両 OS: native の `updateAccessory` で view accessory を差し替えたとき、旧 view が表示から正しく剥がれることを検証する。問題があれば detach 対策 (Decision 2) の範囲で吸収する (→ Requirement: View の差し替えと内容変化が表示に反映される / Scenario: 新しいインスタンスへの差し替え)

## 2. Bridge (iOS / Android / Binding)

- [x] 2.1 iOS `KsSettingsBridge`: `updateAccessoryView(target, sectionID, view: UIView?)` を追加。定数返し closure (`KsAnyView.uiKit { view }`、返却前に superview から detach) で Store の accessory 更新経路へ (→ Requirement: updateAccessoryView で view accessory を更新できる)
- [x] 2.2 Android `KsSettingsBridge`: `updateAccessoryView(target, sectionID, view: View?)` を追加。`KsAnyView.AndroidView { _ -> view の親からの detach 付き }` で同経路へ (→ 同上)
- [x] 2.3 `KsBridgeSection` (両 OS) に `headerView` / `footerView` を追加し、DTO → native Section 変換で view accessory を構築する (text と両指定時は view 優先) (→ Requirement: KsBridgeSection は headerView / footerView を輸送する)
- [x] 2.4 Binding (iOS ApiDefinition / Android 生成確認): 新 API と DTO フィールドを C# へ露出する
- [x] 2.5 Bridge ユニットテスト (両 OS): 設定 / null クリア / 未知 sectionID no-op / setRoot・replaceSection 経由の輸送 / 再バインド安全性 (→ maui-bridge の全 Scenario)

## 3. facade — 実体化機構 (共有部)

- [x] 3.1 materializer seam (`IKsViewMaterializer` 相当) を `IKsImageResolver` と並ぶ per-TFM seam として定義し、`SettingsViewHandler` の `ConnectGateway` で注入する
- [x] 3.2 iOS wrapper: `MauiView` + `ICrossPlatformLayout` の自前サブクラスを実装 (計測・arrange・`MeasureInvalidated` 中継・`DisconnectHandlers` 破棄を自蔵)
- [x] 3.3 Android wrapper: `ItemContentView` 同型の ViewGroup を実装 (`OnMeasure` → `IView.Measure`、`MeasureInvalidated` → requestLayout、破棄)
- [x] 3.4 生成骨格の共通処理: `PropagatePropertyChanged` → BindingContext 先行設定 → `ToHandler` → detach → attach → `AddLogicalChild`、破棄時の逆手順 (→ Requirement: accessory View は SettingsView の BindingContext を継承する)

## 4. facade — 公開 API と更新経路

- [x] 4.1 `RootHeaderView` / `RootFooterView` / `Section.HeaderView` / `FooterView` (BindableProperty, `View?`) を追加 (→ Requirement: Root / Section の Header・Footer に任意 View を設定できる)
- [x] 4.2 View 優先の競合解決: view 非 null の間 text を輸送せず、null 戻しで text へフォールバック再発行 (→ Requirement: text と view の併存は View 優先で解決される)
- [x] 4.3 差し替え検知 → 再実体化 → `updateAccessoryView` 明示経路での再発行。値比較経路 (`replaceSection`) に view 変更を流さないことをコードで保証 (→ Requirement: View の差し替えと内容変化が表示に反映される)
- [x] 4.4 同一インスタンス多重配置の検出と `InvalidOperationException` (既存の facade 制約と同じ機構) (→ Requirement: 同一 View インスタンスの多重配置は例外になる)
- [x] 4.5 Host 世代管理: Handler 切断時に wrapper 破棄 + Section 系 stale closure の Store 書き戻し除去、`OnHostAttached` で再実体化 + Root / Section の全 view accessory 再発行 (`ApplyRootAccessory` の拡張) (→ Requirement: Handler 切断・再接続をまたいで view accessory は保持される)
- [x] 4.6 accessory slot の所有状態機械 (design Decision 5): 差し替え / null 化 / Section 削除・置換 / Root 再構築の各遷移で「Store 更新 → native 配信 → 旧 wrapper 破棄」の退役順序を実装する (icon の retired lease パターン踏襲) (→ Requirement: View の差し替えと内容変化が表示に反映される / Root・Section の Header・Footer に任意 View を設定できる)

## 5. サンプル

- [x] 5.1 一覧ページに「MAUI 固有」区分の Section を追加 (`SampleScreen` 相当の一元定義に従う) (→ Requirement: AccessoryViewsDemoPage を MAUI 固有区分に追加する)
- [x] 5.2 `AccessoryViewsDemoPage` を7項目構成で追加。項目 (7) の復元確認は**同一 SettingsView インスタンス**で Handler 切断・再接続が起きる手順 (子ページ push → pop、または保持インスタンスへの再遷移) にする — 毎回 `CreatePage()` される現行メニュー経由の再選択では検証にならない (→ Requirement: AccessoryViewsDemoPage は本 change の公開挙動を確認できる / Scenario: 復元確認は同一インスタンスで行われる)

## 6. テスト

- [x] 6.1 net10.0 ユニットテスト (fake gateway + fake materializer): **Root / Section × Header / Footer の4対象マトリクス**で設定 / クリア / View 優先 / フォールバック / 差し替え再発行 / 切断中変更の再接続反映。BindingContext は初期伝播 (Root → SettingsView / Section → 所有 Section / ItemsSource 生成 Section → item) と変更追従・明示値の非上書き。多重配置は例外 + null 解除後再利用の許容 (→ maui-core の全 Scenario)
- [x] 6.2 リークテスト (phase-2 設置の WeakReference + GC 基盤): Host 切断に加え、**差し替え / null 化 / Section 削除 / Root 再構築**の各経路で旧 wrapper・Handler・accessory View の platform 実体が回収されること (→ design Decision 5 の状態機械)
- [x] 6.3 E2E (maui/tests/KsSettingsView.MauiHost 両 OS): 4対象の表示 / 内容変化の live 反映 / サイズ追従 / HeaderHeight 固定 clip / **同一 facade インスタンスでの切断・再接続**による復元 (→ maui-core の表示系 Scenario、実機確認)

## 7. 検証

- [x] 7.1 両 OS のテストスイート・E2E が green であること
- [x] 7.2 サンプル7項目の目視確認結果 (スクリーンショット) を記録する
