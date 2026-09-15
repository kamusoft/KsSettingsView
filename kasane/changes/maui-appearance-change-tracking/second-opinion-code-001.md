# セカンドオピニオン: maui-appearance-change-tracking (code-001)
**相方**: codex / **label**: so-code-maui-appearance-change-tracking / **日付**: 2026-09-15 / **対象**: 付随修正 samples/ios/KsSettingsViewSample (SampleAppearanceWindowStyle.swift 新規 / SampleAppearance.swift / ContentView.swift / project.pbxproj)
---
# レビュー結果: maui-appearance-change-tracking

**日付**: 2026-09-15  
**判定**: **APPROVED**

## サマリー

対象の付随修正に、修正を要求する問題は見つかりませんでした。`UIWindow.overrideUserInterfaceStyle` へ適用先を移した実装は、明示外観・「システム」への復帰・表示中の端末外観変更を適切に扱っています。

`deviation.md:1` の差し戻しは合意済みとして扱い、tasks 1〜6 の未実装は指摘対象から除外しました。

## 照合した規約

- `cross/comment-policy.md`
- `cross/runtime-behavior-verification.md`
- `cross/sample-parity.md`
- `cross/test-execution.md`
- `cross/local-development-setup.md`
- `cross/ADR-0016`
- `kasane/lessons/code-review.md`
- `swift-ui-impl-skill`

`ios/swift6-language-mode-check.md` は `ios/Sources/` を変更していないため適用外です。

## 指摘事項

なし。

- Critical: 0
- Major: 0
- Minor: 0
- Suggestion: 0

## 確認結果

- `samples/ios/KsSettingsViewSample/SampleAppearanceWindowStyle.swift:15` の modifier は現在の window だけへ作用し、複数 window にも各 `ContentView` 単位で適用されます。
- `:32` と `:55` により、選択変更と window 接続の両方で反映されます。
- `:62` の同値ガードにより、外観変更による再評価ループを避けています。
- `.system → .unspecified` の対応は、明示外観解除後も端末外観への追随を回復する意図と一致します。
- 表示文言・画面構成・デモデータは変更されておらず、Sample パリティを維持しています。
- Xcode project には新規ファイルが参照・グループ・Sources build phaseへ一貫して登録されています。
- A/B 証跡では、修正前の chrome 不追随と、修正後の dark → light → dark の往復追随を確認できました。
- 提示された検証結果（Sample BUILD SUCCEEDED、ライブラリ 1027件成功・0 failures、各 lint 0件）を前提に判定しました。依頼どおり、こちらではビルド・テストを再実行していません。

結果ファイルは、指定どおり作成していません。



## 突き合わせ結果 (2026-09-15、ホスト側 review-001.md との照合)

- **相方のみ**: 指摘なし (APPROVED)。
- **ホストのみ・採用**: Major「付随修正が症状に効いた証拠が無い」— ホストが修正後ビルドと変更前ビルドを同一 Simulator で実測し、画面遷移を挟む手順 (明示「ダーク」→ デモ画面 → 戻る → 「システム」→ デモ画面 → 端末ダーク) では修正後も chrome が追随しないことを観測 (3 回中 2 回)。実測に基づく根拠が強く、相方の静的レビューでは到達できない種類の指摘 (process L-002: 相方 APPROVED は証明ではない)。修正サイクルへ。
- **ホストのみ・採用 (Minor / Suggestion)**: 再現手順が成果物に無い / deviation の箇所列挙に ContentView.swift・project.pbxproj が無い (L-009) / Sample の外観適用が MAUI と同形になった点を再探索の前提として残す。修正サイクルで併せて対処。
- **矛盾 (要再提示)**: なし。判定の差は実測の有無によるもので、論点の対立ではない。
- 集計: 確定 0 / 採用 4 (ホスト由来) / 降格 0 / 未解決 0。
