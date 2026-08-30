# セカンドオピニオン: add-maui-accessory-views (code-004)
**相方**: codex / **日付**: 2026-08-12 / **対象**: cycle 4 差分 (ReassignIfFree ガード + テスト強化)
---

## 判定: APPROVED

**指摘件数**: Critical 0 / Major 0 / Minor 0 / Suggestion 0

### 1. 前回 Major — 解消

- [KsAccessoryViewOwnership.cs:68](maui/KsSettingsView.Maui/Internals/KsAccessoryViewOwnership.cs:68) の `ReassignIfFree` は、別所有者を持つ `newView` に対して論理親・BindingContextを変更せず、既存配置を奪いません。
- その場合も `oldView` の解除は行うため、差し替え元が古い論理子として残る問題もありません。
- [Section.cs:52](maui/KsSettingsView.Maui/Section.cs:52) と [Section.cs:64](maui/KsSettingsView.Maui/Section.cs:64) の両fallbackが新メソッドを使用しています。
- Section登録時は従来どおりcontrollerの重複検査が所有確定より先に実行されるため、既存配置を保ったまま例外になります。
- [AccessoryViewTests.cs:313](maui/KsSettingsView.Maui.Tests/AccessoryViewTests.cs:313) 以降で、直接追加・ItemsSource/DataTemplate・解除後の正規再利用をそれぞれ固定できています。

### 2. cycle 4による新規問題

見つかりませんでした。外部所有Viewの保護、旧Viewの後始末、解放済みViewの再利用が矛盾なく分離されています。

提示されたMAUI 342件／0 failuresを受け入れた静的レビューです。テスト実行、ファイル書き込み、git操作は行っていません。

---

## 突き合わせ結果 (ksn-orchestrator、2026-08-12)

ホスト側 review-004.md (APPROVED / Suggestion 1) と相方 (APPROVED / 指摘 0) — **双方 APPROVED で収束**。オーナー裁定の Major はガード条件が deviation.md 5件目と厳密一致することを双方が確認し、ホスト側は双方向ミューテーション (ガード撤去 / 過剰ガード) で検出力も実証。

ホスト Suggestion 1 (remarks の例外タイミングの文言精度 — Host 未接続で設定ツリーに入った場合は例外が Host 接続時までずれる) は orchestrator が直接修正 (KsAccessoryViewOwnership.cs 2箇所 / Section.cs 1箇所の文言精密化。lint 0 件・net10.0 ビルド成功)。確認は後続の ksn-verifier (独立文脈) のパッケージに含める。
