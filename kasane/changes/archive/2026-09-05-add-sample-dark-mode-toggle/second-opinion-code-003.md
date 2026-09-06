# セカンドオピニオン: add-sample-dark-mode-toggle (code-003)
**相方**: codex / **label**: so-code-add-sample-dark-mode-toggle-003 / **日付**: 2026-09-05 / **対象**: 作業ツリーの未コミット変更 (HEAD cedaf04 との差分。サイクル 3 の再レビュー)
---
# レビュー結果: add-sample-dark-mode-toggle（003 回目）

**日付**: 2026-09-05  
**判定**: **NEEDS_DISCUSSION**  
**件数**: Critical 0 / Major 1 / Minor 2 / Suggestion 0

## サマリー

前回指摘された実装上の問題はすべて解消されています。`Window` 再利用、Android の同値選択 no-op、iOS テストの非 nil 確認はいずれも妥当で、新たなコード上の blocking 問題は見つかりませんでした。

一方、承認済みモックと実装の未合意差分が残り、UI 承認ゲートの扱いをオーナー判断なしに確定できないため、判定は `NEEDS_DISCUSSION` です。

## 前回指摘の対応

- 相方 Major の MAUI `Window` 累積問題は対応済みです。`App` インスタンスが同じ `Window` を保持し、`CreateWindow` から再利用しています。MAUI 10.0.70 の既定 `CreateWindow` が既存 `Windows[0]` を返す方式とも整合します。[Application.cs](https://github.com/dotnet/maui/blob/10.0.70/src/Controls/src/Core/Application/Application.cs)、[Window.cs](https://github.com/dotnet/maui/blob/10.0.70/src/Controls/src/Core/Window/Window.cs)
- `ui/brief.md` 冒頭には deviation 1 の注記があります。
- Android は `appearance != currentAppearance` の場合だけ保存・再生成します。
- iOS の負の検証には `XCTAssertNotNil` が追加されています。
- `maui-android-input-cells-system-device-toggle-x3.png` と brief の記録から、3 往復後もページ・入力状態・テーマ追随が維持されたことを確認しました。

## 指摘事項

### [🟠 Major] 承認済みモックとの差分が、合意済みでも未合意でもない扱いになっている

**該当箇所**: `kasane/changes/add-sample-dark-mode-toggle/ui/brief.md:103,107,113`

**問題点**:  
brief は「合意済み妥協なし」「未合意の乖離なし」としながら、承認済み `ui/mock/approved.png` にだけ存在する「無効なボタン」を事後的に照合対象外としています。さらにモックの ButtonCell は「登録」ですが、実装は3面とも「ログアウト」です。

`ui/mock/approved.png` は実装期間中の見た目の正であり、この除外は `deviation.md` の合意済み2項目にも含まれていません。したがって、現在の記録だけではオーナーがモックとの差分を正式に受け入れたのか判断できず、UI照合完了を確定できません。

**推奨修正**:  
実装へ架空の行を追加するのではなく、オーナーが次のどちらかを裁定してください。

1. モックの適用範囲を配色・外観UIに限定し、この差分を正式な合意済み差分として記録する。
2. 現行3面の構成に合わせたモックを改めて承認し、`approved.png` を再確定する。

凍結済み足場の扱いを含むため、レビュー側から直接書き換えを指示できる問題ではありません。

### [🟡 Minor] 修正前画像が最終視覚照合用ディレクトリに残っている

**該当箇所**: `kasane/changes/add-sample-dark-mode-toggle/ui/brief.md:70`  
**対象ファイル**: `ui/verification/maui-ios-calendar-dark-range-before.png`

**問題点**:  
`ui/verification/` は最終周の実装画像だけを置く場所です。この画像はカレンダーシート修正前の A/B 証跡であり、動作証跡としては `evidence/` が適切です。

**推奨修正**:  
修正前画像を `evidence/` 側で管理し、参照先を調整してください。`ui/verification/` には修正後と非回帰確認の最終画像だけを残します。

### [🟡 Minor] 後続 change の探索記録が現在の検証結果に追随していない

**該当箇所**: `kasane/changes/fix-default-colors-dark-appearance/exploration.md:12`

**問題点**:  
「MAUI 面の実体は未確認」とありますが、本変更の brief では MAUI iOS / Android の双方で同じ既定色問題を確認済みです（`ui/brief.md:69,82`）。同じ未コミット差分内で事実関係が矛盾しています。

**推奨修正**:  
MAUI 両実行面で再現確認済みであることと、対応する証跡を記録してください。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md`
- `kasane/handbook/cross/test-execution.md`
- `kasane/handbook/cross/runtime-behavior-verification.md`
- `kasane/handbook/cross/sample-parity.md`
- `kasane/handbook/ios/swift6-language-mode-check.md`
- `kasane/decisions/cross/0016-sample-cross-platform-parity.md`
- `kasane/decisions/android/0020-bundled-theme-always-wrap-host-independent.md`
- `kasane/lessons/code-review.md`
- ksn-core の UI artifact・deviation 規律
- SwiftUI / Kotlin / Compose / C# / MAUI の実装レビュー観点

指定どおりテスト・ビルドは再実行せず、提示された全テスト1009件成功、Swift 6 error 0、各サンプルビルド成功・警告0、lint 0件を前提にした静的レビューです。

## 突き合わせ結果 (ホスト review-003.md との照合、2026-09-05)

| 指摘 | 出典 | 採否 | 根拠 |
|---|---|---|---|
| 承認モックとの差分 (「無効なボタン」行・ButtonCell「登録」) が合意済みとも未合意とも記録されていない | 相方のみ (Major → NEEDS_DISCUSSION) | **採用** → オーナー裁定で解決 | モックは見た目の正であり、事後の照合対象外注記だけでは承認ゲートが閉じない。オーナー裁定 (案 1: モックの規範範囲を配色と外観 UI に限定、行構成・文言の差分は合意済み差分) を deviation.md 3 項目目と brief.md「合意済み妥協」に記録。実装は現行構成のまま |
| 修正前画像が `ui/verification/` に残っている | 相方のみ (Minor) | **採用** | ksn-core references/ui-artifacts.md (verification/ は最終周のみ、動作証跡は evidence/)。`evidence/maui-ios-calendar-dark-range-before.png` へ移動し brief.md の参照を更新 |
| 簡易起票スタブの「MAUI 面の実体は未確認」が brief.md の確認結果と矛盾 | 相方のみ (Minor) | **採用** | 事実関係の整合。スタブを MAUI 両面確認済み + 証跡参照に更新 |
| Android Native の再起動維持・端末外観追随の画像なし / `SampleTheme.cs` の「従来どおり」(comment-policy) / 提示物の外観引き継ぎを concepts へ | ホストのみ (Minor 2 / Suggestion 1) | 確定 | 画像 2 枚を追加撮影、コメントを現在形に修正。concepts は蒸留 (ksn-distill) への申し送り |

未解決 (両者矛盾): なし。採用 3 / 降格 0。
