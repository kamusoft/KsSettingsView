# Tasks: add-maui-modern-style

## 1. facade 公開 API (maui-core)

- [x] 1.1 enum `SettingsViewStyle { Classic, Modern }` を新設する (→ Requirement: ListStyle の公開)
- [x] 1.2 `SettingsView` に BindableProperty `ListStyle` (非 nullable・既定 `Classic`) を追加し、変更時に gateway の style 設定操作を呼ぶ (→ Requirement: ListStyle の公開)
- [x] 1.3 4属性の BindableProperty (`SectionMargin: Thickness?` / `SectionCornerRadius: double?` / `SectionBorderWidth: double?` / `SectionBorderColor: Color?`) を追加し、既存の `propertyChanged → ApplyTheme()` 経路に乗せる。validateValue / coerce は付けない (→ Requirement: Theme の Section 装飾4属性の公開)
- [x] 1.4 `SectionMargin` の XML doc に論理方向解釈 (Left/Right = leading/trailing、RTL は native 解決) と「Classic では上下成分のみ適用・左右無視 (全幅契約)」を明記する (→ Requirement: SectionMargin の論理方向解釈)

## 2. 輸送層 (maui-bridge)

- [x] 2.1 `KsThemeSnapshot` に7フィールドを追加し、`CreateThemeSnapshot()` で `Thickness?` を論理4成分へ all-or-none で展開する (`KsWireValues` に必要な変換を追加) (→ Requirement: Theme DTO の Section 装飾4属性輸送)
- [x] 2.2 `IKsSettingsGateway.SetStyle` を追加し、`KsSettingsController` が `_style` を保持して gateway の初回接続時に配信・`ListStyle` 変更時に送信する (ADR-0002 の Store 操作 1:1 の枠外である旨をコメントで注記) (→ Requirement: style の設定操作の輸送 / style の Host 再生成をまたぐ保持)
- [x] 2.3 iOS: `KsBridgeTheme.swift` に7フィールド追加 + `resolve()` で `NSDirectionalEdgeInsets` を組み立て (部分 null は margin 全体未指定)。`KsSettingsBridge` に `setStyle:` (序数 int、定義域外は Classic 正規化) を新設し、style を Host 外のフィールドで保持して `makeHostViewController()` 生成時に適用・生きた Host には即時適用。`ApiDefinition.cs` を追随 (→ Requirement: Theme DTO の Section 装飾4属性輸送 / style の設定操作の輸送 / style の Host 再生成をまたぐ保持)
- [x] 2.4 Android: `KsBridgeTheme.kt` に7フィールド追加 + `resolve()` で `PaddingValues(start, end)` を組み立て (部分 null は margin 全体未指定)。`KsSettingsBridge` に `setStyle` (序数 int、定義域外は Classic 正規化) を新設し、style を Host 外のフィールドで保持して `makeHostView()` 生成時に適用・生きた Host には即時適用。binding (Metadata.xml) を確認・追随 (→ Requirement: Theme DTO の Section 装飾4属性輸送 / style の設定操作の輸送 / style の Host 再生成をまたぐ保持)
- [x] 2.5 両 OS の `KsBridgeGateway.cs` で snapshot → DTO 写像と `SetStyle` 呼び出しを実装する (→ Requirement: Theme DTO の Section 装飾4属性輸送 / style の設定操作の輸送)

- [x] 2.6 native 堅牢化: 両 OS の Section 装飾値の描画時正規化を「非有限 (NaN・±∞) → 0」へ拡張する (iOS/Android の SectionBoxMetrics 相当) (→ Requirement: Section 装飾値の非有限数正規化)

## 3. テスト (net10.0 ユニットテスト + native 単体)

- [x] 3.1 facade: ListStyle の gateway 伝搬・既定 Classic・実行時切替のテスト (fake gateway) (→ Scenario: 切替が gateway へ伝わる / 既定値では現行挙動と一致する)
- [x] 3.2 facade: 4属性の Theme 伝搬・null 素通し・範囲外値の無例外素通し・論理方向写像 (Left→leading / Right→trailing)・Classic でも全成分伝搬のテスト (→ Requirement: Theme の Section 装飾4属性の公開 / SectionMargin の論理方向解釈)
- [x] 3.3 facade: gateway 初回接続時の style 配信テスト (fake gateway) (→ Scenario: gateway 初回接続時に style が配信される)
- [x] 3.4 native bridge: 両 OS の `KsBridgeTheme.resolve()` の4成分組み立て・null 未指定写像・部分 null の margin 全体未指定・borderColor 変換・序数→style 対応 (定義域外の Classic 正規化含む) のテスト (→ Requirement: Theme DTO の Section 装飾4属性輸送 / style の設定操作の輸送)
- [x] 3.5 native bridge: style lifecycle テスト — Host 生成前の `setStyle` が生成時に適用される・`releaseHost()` 後の再生成で style が維持される (両 OS) (→ Requirement: style の Host 再生成をまたぐ保持)
- [x] 3.6 native ui: 非有限 (NaN・±∞) の装飾値が例外なく 0 として描画されるテスト (両 OS)。facade 側は 3.2 に NaN / ±Infinity の素通しケースを含める (→ Requirement: Section 装飾値の非有限数正規化 / Scenario: 非有限数も例外を投げず素通しする)

## 4. サンプル (samples-maui)

- [x] 4.1 samples-ios / samples-android の SectionDecorationDemo を読み、文言・Section 構成・preset 内容の対応表を作る (sample-parity の正の確認) (→ Requirement: SectionDecoration デモページ)
- [x] 4.2 デモページ (style 切替 + preset 切替) を追加し MenuPage へ登録する。新しい色既定は足さない (→ Requirement: SectionDecoration デモページ)

## 5. 視覚照合

- [x] 5.1 iOS / Android それぞれで MAUI サンプルのデモページを起動し、native サンプル (SectionDecorationDemo) と OS × style × preset の組ごとに突き合わせる (mock は無し — 正は native 実装)。確認項目: 初期 style が native と一致 (Modern) / 切替後の style 反映 / Header・Footer が箱外 / 単一 Cell Section の separator なし / 中間 separator の左右対称 / margin・radius・border preset の反映 / Classic で水平 margin が効かないこと。比較スクリーンショットを `screenshots/` に保存する (→ Requirement: SectionDecoration デモページ)
