# セカンドオピニオン: ios-swiftui-representable-safe-area (code-001)
**相方**: codex / **label**: so-code-ios-swiftui-representable-safe-area / **日付**: 2026-09-19 / **対象**: 作業ツリー未コミット差分 (ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift、ios/Tests/KsSettingsViewSwiftUITests/RootSafeAreaModifierTests.swift、samples/ios の 5 画面) と change アーティファクト一式
---
**判定: APPROVED**

Critical 0 / Major 0 / Minor 0 / Suggestion 1。仕様逸脱や完了を妨げる問題は確認できませんでした。記録済み deviation は合理的で、Sample の文言・構成にも変更はありません。

## 指摘事項

### [🔵 Suggestion] 実際の safe-area modifier 適用経路を自動テストで保護する

**該当箇所**: `ios/Tests/KsSettingsViewSwiftUITests/RootSafeAreaModifierTests.swift:6`

**問題点**: 新規テストは `_respectsSafeArea` の保持・copy 契約を十分検証していますが、実際に `body` が `.ignoresSafeArea(.container, edges:)` を適用する `ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift:97` は検証対象外です。この行を削除しても全新規テストは成功します。現実装の正しさは保存済みスクリーンショットで確認できるため承認を妨げませんが、将来の回帰は自動検出できません。

**推奨修正**: 既存の `UIHostingController` を使うテスト基盤を流用し、既定値と `.respectsSafeArea()` で `KsSettingsViewController` の配置領域が切り替わることを検証するホストレベルテストを追加してください。レイアウトテストが不安定になる場合は、現状の実環境証跡を維持する運用でも構いません。

## 確認結果

- Store / DSL の両方式に同一の safe-area modifier が適用されています。
- `.respectsSafeArea(_:)` は既定引数、`false`、copy semantics、既存 Root modifier との連鎖を満たしています。
- `.container` のみを無視しており、keyboard 領域は対象外です。
- Sample 差分は5画面からの回避策削除だけで、文言・Section・Cell 構成は不変です。
- 技術検証2画面に回避策を残す deviation は、実装経路と整合しています。
- iOS 18 / 26、Store / DSL、末尾表示、キーボード、opt-out の証跡を確認しました。
- `git diff --check` に問題はありません。
- テストとビルドは依頼条件に従い再実行せず、提示された1095件成功・Swift 6 error 0件を採用しました。

照合した規約: `cross/comment-policy.md`、`cross/sample-parity.md`、`cross/runtime-behavior-verification.md`、`ios/swift6-language-mode-check.md`。


## 突き合わせ結果 (2026-09-19)

| 指摘 | 相方 | ホスト (review-001) | 採否 |
|---|---|---|---|
| 既定の全面配置の適用経路 (`body` の `.ignoresSafeArea`) に自動回帰テストが無い | Suggestion | Minor | **確定** (重要度はホスト側の Minor)。`UIWindow` + `UIHostingController` の配置実測テストを 1 件追加する |
| Store / DSL 同配置 Scenario の証跡が spec の GIVEN と噛み合わない | — | Minor | ホストのみ。brief の注記で閉じる |
| opt-out A/B が iOS 26 の 1 画面のみ / DSL デモの Root Footer 青帯 / 逆流検査の機械的不能 | — | Suggestion | ホストのみ。修正サイクルは回さない (Root Footer の色は本変更と因果なし、蒸留時の申し送りに留める) |

採用 1 / 確定 1 / 降格 0 / 未解決 0。判定は双方 APPROVED で一致。
