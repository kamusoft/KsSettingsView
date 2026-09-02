# セカンドオピニオン: add-maui-nuget-distribution (code-002)
**相方**: codex / **label**: so-code-add-maui-nuget-distribution (turn 2、code-001 の続き) / **日付**: 2026-09-02 / **対象**: 修正サイクル 1 周目後の作業ツリー (コミット 55bd972 からの全差分)
---
## 再レビュー結果

### 1. 前回の Major

1. **EntryCell の未記録の名前衝突: 解消**

   - `kasane/changes/add-maui-nuget-distribution/deviation.md:11` に `EntryCell` の衝突が追記され、`SwitchCell` と同じオーナー判断を適用すること、型名・名前空間を維持すること、完全修飾または using alias を案内することが明記されています。
   - `kasane/changes/add-maui-nuget-distribution/evidence/consumer-verification.txt:302-328` では、両型が iOS / Android の両 TFM で CS0104 になることを記録しています。
   - 同ファイル `:330-344` では、完全修飾と using alias による回避が両 TFM で成功しています。
   - deviation 記録済みの合意済み差分となったため、仕様違反としては扱いません。

2. **package README の MauiProgram 例: 解消**

   - `README.md:137-158` と `README_ja.md:137-158` に、以下を含む自己完結した `MauiProgram` のコード例があります。
     - `using KsSettingsView;`
     - `using Microsoft.Maui.Hosting;`
     - `.UseMauiApp<App>()`
     - `.AddKsSettingsView()`
     - `builder.Build()`
   - 英語版・日本語版の C# コードブロックは同一です。
   - `kasane/changes/add-maui-nuget-distribution/evidence/consumer-verification.txt:251-299` には、再 pack 後の nupkg から XAML と C# ブロックを無編集で抽出し、Android / iOS の消費者ビルドが成功した記録があります。これは `spec.md:107-111` の「例を変更せずにビルドが成功する」を満たします。

### 2. 対応による新規問題

**なし。**

Critical / Major / Minor / Suggestion に該当する新規指摘はありません。`git diff --check 55bd972` も問題なしでした。

既知事項の XA4301、NU1507、Android binding の xml doc、アイコン帰属は依頼どおり判定を阻害する指摘には含めていません。

### 3. 最終判定

**APPROVED**

適用規約は `kasane/handbook/cross/comment-policy.md`、`test-execution.md`、`public-identifiers.md`、`kasane/handbook/maui/integration-host-verification.md` です。今回は指定どおり静的レビューのみで、ビルド・テスト結果は提示された結果と evidence の整合性を確認しました。


## 突き合わせ結果 (ksn-orchestrator、2026-09-02)

相方は前回の Major 2 件を解消と判定し新規指摘なし (APPROVED)。ホスト側 review-002 と突き合わせて収束判定する。
