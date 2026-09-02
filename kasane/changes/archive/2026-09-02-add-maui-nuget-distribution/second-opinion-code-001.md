# セカンドオピニオン: add-maui-nuget-distribution (code-001)
**相方**: codex / **label**: so-code-add-maui-nuget-distribution / **日付**: 2026-09-02 / **対象**: コミット 55bd972 から作業ツリーまでの全差分 (abf6e0a の名前空間改名を含む)
---
# レビュー結果: add-maui-nuget-distribution

**判定**: `NEEDS_DISCUSSION`  
**件数**: Critical 0 / Major 2 / Minor 0 / Suggestion 0

## サマリー

pack・CPM・`buildTransitive` ガードと機械的な名前空間改名は、合意済み deviation を考慮すれば概ね仕様どおりです。ただし公開 API の未記録の名前衝突と、package README の消費者検証不足が残っています。

## 指摘事項

### [🟠 Major] `EntryCell` にも未記録の名前衝突がある

**該当箇所**: `maui/KsSettingsView.Maui/EntryCell.cs:14`

**問題点**: 名前空間改名により `KsSettingsView.EntryCell` は既存の `Microsoft.Maui.Controls.EntryCell` と衝突します。.NET MAUI 10 には [`Microsoft.Maui.Controls.EntryCell`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.maui.controls.entrycell?view=net-maui-10.0) が存在するため、MAUI の暗黙 using と `using KsSettingsView;` を併用すると、既知の [`SwitchCell`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.maui.controls.switchcell?view=net-maui-10.0) と同様に CS0104 になります。

`kasane/changes/add-maui-nuget-distribution/deviation.md:10` は `SwitchCell` だけを合意済みとしており、`EntryCell` は記録されていません。消費者検証も `kasane/changes/add-maui-nuget-distribution/evidence/consumer-verification.txt:77` で `EntryCell` を対象にしていません。このまま docs-refresh へ渡すと、注意書きから `EntryCell` が漏れます。

**推奨修正**: オーナー判断として次のいずれかを確定してください。

- 名前空間・型名を維持するなら、`EntryCell` も合意済み差分へ追加し、README／skills の注意書きを `SwitchCell` と `EntryCell` の両方にする。
- consumer probe に両型を追加し、完全修飾名または using alias が必要になることを検証・証跡化する。
- 衝突を受け入れない場合は、型名または名前空間の再設計判断が必要です。

### [🟠 Major] package README の `MauiProgram` 例はそのまま使用できない

**該当箇所**: `README.md:137`、`README_ja.md:137`

**問題点**: デルタスペックは `kasane/changes/add-maui-nuget-distribution/specs/maui-nuget-distribution/spec.md:99` と同ファイル `:109` で、改名後の `using` と `MauiProgram` 登録を含む例を、そのまま写してビルドできることを要求しています。

しかし README にあるのは XAML と「`.AddKsSettingsView()` を呼ぶ」という文章だけで、必要な `using KsSettingsView;` と登録コードがありません。実際、リポジトリ内の正しい利用例は `samples/maui/KsSettingsView.Sample.Maui/MauiProgram.cs:1` にその using を持っています。

また、`kasane/changes/add-maui-nuget-distribution/evidence/consumer-verification.txt:127` は登録したとだけ記録しており、README に存在しない using をどう追加したかを示していません。「例を変更せずビルド成功」という同ファイル `:134` の結論を裏付けていません。したがって `tasks.md:34` と `tasks.md:40` の完了チェックは現状では成立しません。

**推奨修正**: 両 README に、少なくとも次を含む自己完結した `MauiProgram` コード例を翻訳ロックステップで追加してください。

```csharp
using KsSettingsView;

// ...
builder
    .UseMauiApp<App>()
    .AddKsSettingsView();
```

その後、nupkg 内の README だけを入力として新規 consumer にコピーし、未記載の using・alias・完全修飾を追加せず両 OS でビルドできることを再検証してください。

## 既知事項の評価

- XA4301 4件: 今回の違反には数えていませんが、公開前には解消または利用者向け既知事項として扱う価値があります。
- NU1507: 証跡上は作業機の複数ソース構成に依存し、解決版の変化ではないため、現実装の正しさには影響しません。
- Android binding の XML doc: runtime・restore への影響はなく、パッケージ内容の整理上の問題に留まります。
- README の画像絶対 URL 化は、NuGet.org が相対画像を表示しないという[公式要件](https://learn.microsoft.com/en-us/nuget/nuget-org/package-readme-on-nuget-org)に適合しています。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md`
- `kasane/handbook/cross/test-execution.md`
- `kasane/handbook/cross/public-identifiers.md`
- `kasane/handbook/maui/integration-host-verification.md`
- `kasane/lessons/code-review.md`
- `csharp-impl-skill`
- `maui-skill`
- `maui-native-binding-skill`

指定どおりビルド・テストは再実行せず、提示結果、evidence、生成済み nuspec、基準コミットからの差分を静的に照合しました。ファイルへの書き込みは行っていません。


## 突き合わせ結果 (ksn-orchestrator、2026-09-02)

ホスト側 review-001.md (CHANGES_REQUESTED: Minor 2 / Suggestion 5) との突き合わせ。

| 相方の指摘 | 採否 | 根拠 |
|---|---|---|
| [Major] `EntryCell` にも未記録の名前衝突 | **採用** (相方のみ + 根拠強) | facade の公開型を列挙して確認: MAUI Controls と同名なのは `EntryCell` / `SwitchCell` の 2 件 (`TextCell` / `ImageCell` / `ViewCell` に相当する型は無い)。deviation.md の SwitchCell 合意を EntryCell へ拡張し、消費者検証で両型の CS0104 を実測する。ホスト側の見逃しとして lessons に捕捉 |
| [Major] package README の `MauiProgram` 例がそのまま使えない | **採用** (相方のみ + 根拠強) | Scenario「README の例による消費者ビルド」は「MAUI 最小例 (XAML と `MauiProgram` の登録)」を要求するが、README.md:137 は散文のみで `using KsSettingsView;` と登録コードが無い。証跡 (consumer-verification.txt:127) も登録コードの出所を示していない。両 README に自己完結した `MauiProgram` 例を足し、nupkg 内 README だけを入力に再検証する |

ホスト側のみの指摘 (相方は言及なし): README 相対リンク 7 箇所 (Minor 高) / アイコン帰属表示 (Minor) / Suggestion 5 件 — ホスト判定のまま処理する。両者の矛盾: なし。
