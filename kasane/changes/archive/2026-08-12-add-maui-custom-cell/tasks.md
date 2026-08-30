# Tasks: add-maui-custom-cell

## 1. probe (design Decision 5 の分岐確定)

- [x] 1.1 サイズ変化 probe: wrapper の計測無効化だけで両 OS の行高さが追従するかを最小構成で実測し、結果を artifacts/probe に記録する (→ Requirement: 行高さは Content の self-sizing に追従する)
- [x] 1.2 probe の結果、不足する OS があれば一過性の再計測通知 (accessory の invalidateAccessoryMeasurement と同型の cell 対象版) を native 側へ追加する分岐を採る — proposal Non-Goals の事前許容に従い、採った場合は deviation.md に記録する

## 2. facade (maui-cells)

- [x] 2.1 `CustomCell : CellBase` を追加 — `[ContentProperty]` の `Content : View?`、`Command` / `CommandParameter` / `Tapped`、`ShowArrowIndicator` (→ Requirement: CustomCell の配置と Content の表示 / ShowArrowIndicator で Disclosure Indicator を表示する)
- [x] 2.2 `KsCustomCellSnapshot` を per-type 展開へ追加 (共通フィールド + ContentToken + ShowArrowIndicator + タップ購読有無。View は含めない) (→ Requirement: 埋め込み platform view はトークンの変更でのみ差し替わる)
- [x] 2.3 Content の論理所有 (`KsAccessoryViewOwnership` 再利用) — logical tree 接続・BindingContext 継承・多重配置検出 (→ Requirement: Content は所有 Cell の BindingContext を継承する / 同一 View インスタンスの多重配置は例外になる)
- [x] 2.4 controller の cell lease 所有と世代トークン発行 — 設定・差し替え・往復・null 遷移・退役順序 (Store 更新 → 配信 → 破棄、同一 View 包み直し前の先行破棄) (→ Requirement: 内容変化の live 反映と Content の差し替え)
- [x] 2.5 Handler 切断・再接続の復元 — 再実体化 + 新トークン再発行 (→ Requirement: Handler 切断・再接続をまたいで CustomCell は復元される)
- [x] 2.8 構造的な除去の解放経路 — Cell 削除 / Section Reset・削除 / ItemsSource 除去 / Root 再構築で lease・所有表・計測購読・多重配置表を解放し、除去後の View 再利用をテストする (→ Requirement: 構造的な除去で Content の所有と表示資源は解放される)
- [x] 2.9 gateway の輸送 seam 追加 — cell content lease の platform 実体を DTO へ載せる口、fake gateway / fake materializer の更新、既存 accessory 挙動の回帰テスト (→ Requirement: KsBridgeCustomCell で platform view と content トークンを輸送する)
- [x] 2.6 不適用プロパティの silent no-op (変換で読まない) + XML doc への不適用一覧明記 (→ Requirement: 継承プロパティのうち不適用のものは silent no-op)
- [x] 2.7 net10.0 ユニットテスト (fake seam / fake gateway、Handler 1:1 模擬) — 上記全 Requirement の Scenario を指標に (→ Requirement: maui-cells 全件)

## 3. iOS bridge

- [x] 3.1 `KsBridgeCustomCell` DTO 追加 (view / contentToken / showArrowIndicator / タップ購読有無) (→ Requirement: KsBridgeCustomCell で platform view と content トークンを輸送する)
- [x] 3.2 native CustomCell 構築 — content = トークン、builder = detach してから返す定数返し representable (wrapper のサイズ中継含む) (→ Requirement: platform view は返す前に既存の親から切り離される)
- [x] 3.3 interaction delegate へ custom cell タップ通知を追加 (購読なしは onTap nil 構築) (→ Requirement: 行タップは単一 delegate / listener へ通知される)
- [x] 3.4 iOS bridge テスト — view インスタンス安定性を materialize / detach / dispose の回数で正負両方向計測 (同一トークン再配信で 0 回 / トークン変更で 1 回)、detach・リサイクル (→ Requirement: maui-bridge 全件)

## 4. Android bridge

- [x] 4.1 `KsBridgeCustomCell` DTO 追加 (iOS と対称) (→ Requirement: KsBridgeCustomCell で platform view と content トークンを輸送する)
- [x] 4.2 native CustomCell 構築 — content = トークン、builder = AndroidView (factory 内 detach 必須) (→ Requirement: platform view は返す前に既存の親から切り離される)
- [x] 4.3 interaction listener へ custom cell タップ通知を追加 (→ Requirement: 行タップは単一 delegate / listener へ通知される)
- [x] 4.4 Android bridge テスト — view インスタンス安定性を materialize / detach / dispose の回数で正負両方向計測、detach・リサイクル (→ Requirement: maui-bridge 全件)

## 5. facade 経路の接続と対称テスト

- [x] 5.1 IKsInteractionSink へ `CustomCellTapped(cellId)` を追加し Command / Tapped へ配線 — 実効有効状態 (CanExecute 連動) と発火順 (Tapped → Command) を既存 CommandCell と同一にし、購読の動的変更 (Command 設定/解除・Tapped の最初/最後の購読) の再配信をテストする (→ Requirement: 行タップは Command / Tapped で通知される)
- [x] 5.2 IsEnabled / IsVisible / BackgroundColor / Height の輸送を既存経路で確認するテスト (→ Requirement: IsEnabled / IsVisible の挙動)
- [x] 5.3 ItemTemplate 生成の CustomCell が独立に実体化されるテスト (→ Requirement: Content は所有 Cell の BindingContext を継承する)

## 6. サンプル (samples-maui)

- [x] 6.1 パリティ画面 `CustomCellDemoPage` を native の CustomCellDemo と同一構成・文言 (5構成) で追加し、メニューのパリティ区分に登録する (→ Requirement: パリティ画面 CustomCellDemo を native と同一構成で提供する)
- [x] 6.2 MAUI 固有デモページを追加 (差し替え・null 遷移 / ItemTemplate 独立動作 / 再接続復元 / サイズ変化追従) し、メニューの「MAUI 固有」区分に登録する (→ Requirement: MAUI 固有の CustomCell デモを別画面で提供する)

## 7. E2E 視覚検証 (lessons L-003: 証跡必須)

- [x] 7.1 両 OS 実機 / Simulator でパリティ画面 (5構成) と MAUI 固有ページのスクリーンショットを撮り change 配下 screenshots/ に保存する — サイズ変化は変化前後 + 遷移中を残す (→ Requirement: samples-maui 全件 / 行高さは Content の self-sizing に追従する)
- [x] 7.2 スクロールリサイクル検証 — パリティ画面のスクロール耐性構成を末尾まで往復させ、表示混線がないことをスクショで記録 (→ Requirement: platform view は返す前に既存の親から切り離される / スクロール耐性構成で表示が混線しない)
- [x] 7.3 再接続検証 — ページ離脱→再訪問の復元をスクショで記録 (→ Requirement: Handler 切断・再接続をまたいで CustomCell は復元される)
- [x] 7.4 パリティ照合 — MAUI のパリティ画面と native 両 OS の同画面を突き合わせ、文言・構成の一致をスクショで確認する (→ Requirement: パリティ画面 CustomCellDemo を native と同一構成で提供する)
