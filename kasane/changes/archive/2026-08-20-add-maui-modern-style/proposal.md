# Proposal: add-maui-modern-style

## Why

Native (iOS / Android) の `KsSettingsViewStyle.Modern` 完全実装 (implement-modern-style) が完了したが、MAUI facade には style の公開 API も Theme の Section 装飾4属性も存在せず、Bridge にも style を渡す口が無い (両 OS とも Classic 固定で生成)。MAUI 利用者が Modern style と Section 箱の装飾制御を使えるようにする。公開 API の形・型写像・Bridge 経路は phase-11 の議論で確定済み (maui/ADR-0023・ADR-0024 proposed)。

## What Changes

- **maui-core**: 統一 enum `SettingsViewStyle { Classic, Modern }` を新設し、`SettingsView` に `ListStyle` (非 nullable・既定 `Classic`) と Theme 4属性の BindableProperty (`SectionMargin: Thickness?` / `SectionCornerRadius: double?` / `SectionBorderWidth: double?` / `SectionBorderColor: Color?`) を追加する。4属性は null = platform 既定へ委譲の完全素通し (既定値定数・バリデーションを持たない)。`SectionMargin` の Left / Right は leading / trailing (論理方向) として解釈する (ADR-0024)。Classic での上下のみ適用は doc-only で明記する。
- **maui-bridge**: `KsThemeSnapshot` / `KsBridgeTheme` (両 OS) に7フィールド追加 (margin はフラット論理4成分・all-or-none)、Bridge の `resolve()` が directional 型を組み立てる。style 用に `IKsSettingsGateway.SetStyle` + Bridge `setStyle:` を新設 (enum 序数 int 輸送・Native 可変プロパティを叩く・Handler 再接続時再送)。iOS ApiDefinition / Android binding を追随させる。
- **samples-maui**: Native の `SectionDecorationDemo` 一式を正として、sample-parity (cross/ADR-0016) で Classic / Modern 切替 + 4属性 preset controls のページを追加し `MenuPage` に登録する (初期 style は native と同じ Modern)。
- **settings-view-ios-ui / settings-view-android-ui**: Section 装飾値の描画時正規化を「非有限 (NaN・±∞) → 0」へ拡張する (両 OS)。素通しされた非有限数が Android の `roundToInt()` で例外に到達する既存の穴 (native 利用者にも影響) を塞ぐ堅牢化で、視覚契約は変えない (相方スペックレビュー Major 3 由来)。

## Non-Goals

- Native 側の視覚契約の変更・新設 (正は implement-modern-style の成果と concepts styling/list-appearance。MAUI は値の伝搬のみ)
- 新たな色既定の導入 (箱の視認性はアプリの Theme 指定に依存する — implement-modern-style からの申し送り)
- MAUI 側での既定値の保持・負値正規化・radius clamp (Native の描画時正規化に委譲)
- `FlowDirection` の監視・物理⇔論理変換 (RTL の左右解決は Native の機構に委譲)
- ui/ (brief / mock) の作成 — 見た目の正は implement-modern-style で承認済みの Native 実装と native サンプル (SectionDecorationDemo) にあり、MAUI 側で新たにデザインする視覚要素が無いため省略する (方向確認で合意済み)。実装時の視覚照合は native サンプルとの比較タスクで担保する

## Impact

- 破壊的変更なし (追加のみ)。既存利用者は既定 `Classic` のまま挙動不変。
- 影響範囲: MAUI facade / 両 OS の Bridge 層と binding csproj / MAUI サンプル / net10.0 ユニットテスト。
- リスク: `setStyle` は Store を通らない初の Bridge API (Native 側も style は Store 外なので対称。maui/ADR-0002 との関係は実装時に注記)。

## 級: M

公開 API の追加だが値の伝搬のみで新視覚契約なし。同型の伝搬系フェーズ phase-9 (add-accessory-visibility-toggle) の M 級実績に倣う。

domain: maui
roadmap: maui-support/phase-11-modern-style
