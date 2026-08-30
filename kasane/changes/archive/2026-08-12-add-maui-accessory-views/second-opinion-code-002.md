# セカンドオピニオン: add-maui-accessory-views (code-002)
**相方**: codex / **日付**: 2026-08-12 / **対象**: 修正サイクル後の working tree (前回 Major 2件の解消確認 + 修正範囲の新規レビュー)
---

# 再レビュー結果

**判定: APPROVED**  
**内訳: Critical 0 / Major 0 / Minor 1 / Suggestion 1**

前回の Major 2件はいずれも解消しています。修正範囲に新たな実行時不具合は見つかりませんでした。

## 前回指摘の判定

### 1. Root再構築時のHandler切断 — 解消

[KsSettingsController.cs:476](maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:476) で、同一Viewを再実体化する前に `DisposeRetiredViewsOf()` が旧leaseを破棄するようになりました。

これにより処理順序が次のようになっています。

1. 旧native表示を更新から外す
2. 同一Viewの旧leaseを破棄して旧Handlerを切断
3. 新しいHandler／platform実体を生成
4. 一括退役処理では新しいleaseを破棄しない

[FakeViewMaterializer.cs:61](maui/KsSettingsView.Maui.Tests/Fakes/FakeViewMaterializer.cs:61) もViewとHandlerの1対1性および切断後の再生成を模擬しており、[AccessoryViewTests.cs:470](maui/KsSettingsView.Maui.Tests/AccessoryViewTests.cs:470) が以前の不具合を直接検出できる形になっています。Host再接続経路も同様に確認されています。

### 2. Handler未接続中の論理ツリー・BindingContext未接続 — 解消

論理所有権がplatform実体化から分離されています。

- Root accessoryは[SettingsView.cs:798](maui/KsSettingsView.Maui/SettingsView.cs:798)で即座にSettingsViewへ接続
- Section accessoryは[Section.cs:40](maui/KsSettingsView.Maui/Section.cs:40)で即座にSectionへ接続
- 共通処理は[KsAccessoryViewOwnership.cs:25](maui/KsSettingsView.Maui/Internals/KsAccessoryViewOwnership.cs:25)
- Host解放時はplatform leaseだけを破棄し、論理子は維持

Handler未生成時の初期継承、Host解放中のBindingContext変更、論理ツリー維持、プロパティ解除時の論理子削除が[AccessoryViewTests.cs:505](maui/KsSettingsView.Maui.Tests/AccessoryViewTests.cs:505)以降でそれぞれ検証されています。

## 新規指摘

### [🟡 Minor] 論理所有権の寿命変更がdeviation.mdに未記録

**該当箇所**: [design.md:20](kasane/changes/add-maui-accessory-views/design.md:20)、[design.md:65](kasane/changes/add-maui-accessory-views/design.md:65)、[tasks.md:21](kasane/changes/add-maui-accessory-views/tasks.md:21)、[deviation.md:1](kasane/changes/add-maui-accessory-views/deviation.md:1)

**問題点**: design/tasksは`AddLogicalChild`と`RemoveLogicalChild`をwrapper生成・破棄の一部として規定しています。一方、修正後は論理所有権をBindablePropertyの寿命、platform leaseをHostの寿命として明確に分離しています。実装側の判断が正しく今回のMajorを解消していますが、設計からの重要な変更が既存2件のdeviationに含まれていません。

**推奨修正**: `deviation.md`へ、論理所有権をplatform leaseから分離したこと、その理由がHandler未接続中およびHost解放中もBindingContext継承を維持するためであることを追記してください。コード修正は不要です。

### [🔵 Suggestion] LeakTestsのコメントが更新後のfake実装と一致しない

**該当箇所**: [LeakTests.cs:124](maui/KsSettingsView.Maui.Tests/LeakTests.cs:124)

**問題点**: コメントはHandler切断がfake seamの検証範囲外と説明していますが、更新後の[FakeViewMaterializer.cs:103](maui/KsSettingsView.Maui.Tests/Fakes/FakeViewMaterializer.cs:103)はlease破棄時のHandler切断を明示的に模擬しています。実際にAccessoryViewTestsでも接続状態を検証しています。

**推奨修正**: 「このリークテストはplatform実体の回収だけを対象とし、Handler接続状態はAccessoryViewTestsで検証する」という説明に更新してください。

提示されたMAUI 328件・Android 2280件の成功結果を前提とし、指示どおりこちらではビルド・テスト・git操作・ファイル書き込みは行っていません。

---

## 突き合わせ結果 (ksn-orchestrator、2026-08-12)

ホスト側 review-002.md (Major 1 / Minor 1) との突き合わせ。相方は APPROVED、ホストは CHANGES_REQUESTED — 判定は割れたが指摘対象が異なるだけで矛盾はなし (ホスト Major はプローブ実測による動的検証由来で、静的レビューの相方には見えない型。lessons L-002 の想定どおりホスト側責務で検出)。

| 指摘 | 採否 | 処理 |
|---|---|---|
| 相方 Minor: 論理所有分離が deviation.md 未記録 | **採用** | orchestrator が deviation.md へ追記済み (2026-08-12 付エントリ) |
| 相方 Suggestion: LeakTests コメントと fake 実装の不一致 | **採用** | cycle 3 の修正対象に含めた |
| ホスト Major / Minor (Root/Section 非対称 2 件) | ホスト側指摘としてそのまま cycle 3 へ | — |
