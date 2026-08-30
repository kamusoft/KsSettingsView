# セカンドオピニオン: implement-modern-style (code-001)
**相方**: codex (ksn-reviewer 編成) / **日付**: 2026-08-20 / **対象**: 未コミット working tree 全変更 (HEAD 81bf2c4 比) + 未追跡新規ファイル (ios/Sources, ios/Tests, android/ks-settingsview-ui, android/ks-settingsview-compose, samples/ios, samples/android)
---

# レビュー結果: implement-modern-style

**判定**: APPROVED
**件数**: Critical 0 / Major 0 / Minor 1 / Suggestion 1

## サマリー

未追跡ファイルを含む指定範囲の全変更を静的レビューしました。仕様を阻害する Critical / Major は見つかりません。deviation.md の合意済み差分と既知の申し送りは指摘から除外しています。

提示済みのテスト結果（iOS 876件、Android 2490件、lint 禁止0件）を検証結果として採用し、再実行はしていません。

## 指摘事項

### [🟡 Minor] Section が0件でも iOS に sectionMargin が残る

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:733`、`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:747`

**問題点**: `applyListEdgeMargin()` と `rootAccessoryContentInsets()` は、可視 Section の有無にかかわらず Modern の既定余白（top 22ptなど）を適用します。全 Section が削除・非表示の状態でも、list 端または Root Header / Footer の内側に「どの Section にも属さない余白」が残ります。Android は実在する Section 行にだけ offset を付けるため、空状態で挙動が非対称になります。

**推奨修正**: 可視 Section が0件なら edge margin と Root accessory 用 insetを `.zero` にしてください。また、`visibleSections` が空／非空へ遷移する構造・可視性更新経路でも再計算されるよう処理を集約し、空状態と全非表示状態の回帰テストを追加してください。

### [🔵 Suggestion] Android の identity テストが内容しか検証していない

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ModernSectionDecorationTest.kt:600`

**問題点**: `titlesBefore` と `committedTexts()` の比較を「Section / Cell の identity は変わらない」としていますが、同じ文字列を持つ別 ID や再生成された項目でも通過します。

**推奨修正**: Theme 変更前後の `sectionId` と `cell.id` の列を比較してください。ViewHolder 維持まで保証したい場合は、可視 ViewHolder の参照同一性も別途検証すると明確です。

## アクションプラン

1. 空の可視 projection では iOS の sectionMargin を適用しないよう修正する。
2. 空状態／全 Section 非表示状態のテストを追加する。
3. Android の identity アサーションを ID 比較へ強化する。

--- KSN_COUNTERPART_META counterpart=codex session_id=<session-id> label=so-code-implement-modern-style turns=1 ---

## 突き合わせ結果 (2026-08-20、ホスト review-001 との照合)

- 双方一致: なし (指摘の重複なし)
- **採用** (相方のみ・根拠強): [Minor] 可視 Section 0 件でも iOS に sectionMargin が残る (該当箇所特定 + Android との非対称という実害シナリオあり)。ホスト側 Major/Minor と同格で修正サイクルに含める
- **採用 (Suggestion 扱い・任意)**: Android identity テストの ID 比較への強化 (根拠は具体的だがテスト強度の改善提案の域)
- 降格: なし / 未解決 (矛盾): なし
- 判定の差 (相方 APPROVED vs ホスト CHANGES_REQUESTED) は矛盾ではなく検出力の差: ホスト側 Major (挿入/削除後の clip 破綻) は実行時プローブによる動的検証で捕捉されたもので、静的レビューの相方には構造的に見えない (lessons/process.md L-002 の想定どおり)。総合判定はホスト側 CHANGES_REQUESTED を採る
