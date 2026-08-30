# Tasks: add-cell-types-custom

## 1. iOS 実装

- [x] 1.1 `CustomCell` struct（型消去内蔵・ジェネリック init 2形・手動 equality）を `KsSettingsViewUI` に追加 (→ Requirement: CustomCell の定義と等価性 / 静的コンテンツの省略形)
- [x] 1.2 `CustomCellView`（`UIHostingConfiguration` ベースの Renderer。chevron 合成・tapHandler・`prepareForReuse` 解放・`isEnabled=false` 時の content への `.disabled(true)` 適用と opacity 0.38 の淡色化）を追加 (→ Requirement: content 駆動の描画と再利用 / 行タップ / Disclosure Indicator の表示)
- [x] 1.3 `registerCustomCell()` + `autoRegisterCustomCell` フラグ（basic / input と同列の自動登録機構）を追加 (→ Requirement: 事前登録なしの描画)
- [x] 1.4 行レベル style 適用（背景色・cellHeight。`EffectiveStyle` 経由）(→ Requirement: スタイルの適用範囲)
- [x] 1.5 `VisibilityAware` 準拠 (→ Requirement: 可視性フィルタへの参加)
- [x] 1.6 iOS ユニットテスト（equality 3 Scenario・静的省略形・可視性・登録解決・render 出力・再利用リセット・style 適用範囲・SectionBuilder 直書き配置・同値 content の no-rebind・無効時の content 操作抑止・タップ競合）。任意 content の観測は accessibilityIdentifier 付き probe content で行う (→ 上記各 Requirement + DSL による配置の Scenario)

## 2. Android 実装

- [x] 2.1 `CustomCell<Content : Any>` class（手動 equals / hashCode・`VisibilityAware` 準拠）を `ks-settingsview-ui` に追加 (→ Requirement: CustomCell の定義と等価性 / 静的コンテンツの省略形 / 可視性フィルタへの参加)
- [x] 2.2 `CustomCellViewHolder`（`ComposeCellViewHolder` 継承。消去済みエントリポイント `composeContent` の呼び出し・`setContent` 合成・click listener 上書き・`reset` 解放・`isEnabled=false` 時の入力遮断）を追加 (→ Requirement: content 駆動の描画と再利用 / 行タップ / Disclosure Indicator の表示)
- [x] 2.3 予約 viewType 定数を追加し、`KsSettingsView` 初期化時の sentinel 方式自動登録（LabelCell / EntryCell と同列）へ接続 (→ Requirement: 事前登録なしの描画)
- [x] 2.4 行レベル style 適用（背景色・cellHeight）(→ Requirement: スタイルの適用範囲)
- [x] 2.5 `DSLSectionScope.CustomCell(...)` 拡張関数（content あり / なしの 2 形、CellHandle 戻り値）を `ks-settingsview-compose` に追加 (→ Requirement: DSL による配置)
- [x] 2.6 Android ユニットテスト（equality 3 Scenario・静的省略形・可視性・登録解決・bind 出力・再利用リセット・DSL 配置 + modifier チェーン・同値 content の no-rebind・無効時の content 操作抑止・タップ競合）。任意 content の観測は testTag 付き probe content で行い、Compose UI test 依存が必要なら `ks-settingsview-ui` の build.gradle へ追加する (→ 上記各 Requirement の Scenario)

## 3. Sample デモ

- [x] 3.1 iOS `CustomCellDemoView` を追加（SampleTheme 適用。①インライン ②SliderCell ラップ関数 ③動的高さ ④showArrow + onTap ⑤スクロール耐性ダミー 40 行の 5 構成）。SampleScreen のメニュー導線（enum case / title / destination）も追加し、Android とタイトルを一致させる (→ Requirement: DSL による配置 / 高さの自動追従 / 行タップ / Disclosure Indicator の表示 / content 駆動の描画と再利用)
- [x] 3.2 Android `CustomCellDemoScreen` を追加（同上 + メニュー導線追加）(→ 同上)
- [x] 3.3 SliderCell ラップ関数例を両 Sample に実装（②の実体。CustomCell を返す関数として定義）(→ Requirement: CustomCell の定義と等価性)

## 4. 検証

- [x] 4.1 mock（承認済み案）との視覚照合。chevron の見た目・位置が既存 Cell と一致することを含めて確認し、`ui/verification/` に証跡を保存 (→ Requirement: Disclosure Indicator の表示)
      — 2026-08-04（修正サイクル 2）: 無効時の淡色化を iOS 側にも適用し、Sample の Slider のアクセント色を
        両プラットフォームで揃えた。iOS 26.5 シミュレータ (iPhone 17) と Android 実機 (Pixel 6a / Android 16、
        Pixel 4a / Android 13) の左右比較を `ui/verification/compare-01` / `compare-02` に保存した。
- [x] 4.2 動的高さの実機/エミュレータ挙動確認（展開/折りたたみで行高さと後続行が追従すること。受け入れ条件）(→ Requirement: 高さの自動追従)
      — Android 実機 (Pixel 6a / Android 16、Pixel 4a / Android 13) と iOS 26.5 シミュレータ (iPhone 17) で確認済み。
        iOS 16 下限 (pixie4) はオーナー目視で確認済み (2026-08-03)。
        同時に指摘された展開アニメーションのブレは修正し、修正前後のフレーム列を `ui/verification/` に保存した (2026-08-04)。
        Android は展開/折りたたみが 1 フレームで確定し中間状態を持たないことを 60fps 録画のフレーム列で確認済み (2026-08-04)。
- [x] 4.3 デルタスペック全 Scenario とテストの対応を確認（ksn-verify の事前セルフチェック）
      — 2026-08-04: ksn-verify を独立実行し **VALID**（`verify-001.md`）。10 Requirement / 21 Scenario 全件で
        iOS / Android 双方の実装とテストの対応が取れ、欠落・未記録の乖離はゼロ。spec 本文との差分 5 件は
        すべて `deviation.md` 記録済み。足場（specs / proposal / design）は提案作成コミット以降未変更で逆流なし。
