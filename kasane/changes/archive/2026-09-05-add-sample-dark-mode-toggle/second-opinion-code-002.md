# セカンドオピニオン: add-sample-dark-mode-toggle (code-002)
**相方**: codex / **label**: so-code-add-sample-dark-mode-toggle-002 / **日付**: 2026-09-05 / **対象**: 作業ツリーの未コミット変更 (HEAD cedaf04 との差分。サイクル 2 の再レビュー)
---
# 独立コードレビュー — サイクル 2

**指摘件数**: Critical 0 / Major 1 / Minor 1 / Suggestion 0

## 指摘事項

### [Major] Activity 再生成のたびに古い `Window` が `NavigationPage` を購読し続ける

**該当箇所**: `samples/maui/KsSettingsView.Maui.Sample/App.cs:14,20-23`、`samples/maui/KsSettingsView.Maui.Sample/Platforms/Android/MainActivity.cs:41-52`

**問題点**:

システム外観変更時に `Recreate()` が呼ばれ、その都度 `CreateWindow()` は新しい MAUI `Window` を作ります。一方、同じ static `NavigationPage` が各 `Window` に再利用されています。

MAUI の `Window` は設定された `Page` の `HandlerChanged` / `HandlerChanging` を購読しますが、ネイティブ Window の破棄時には `Page` を解除しません。したがって、外観変更を繰り返すと、破棄済みの古い `Window` が同じ `NavigationPage` から参照され続けます。古い Window の保持、イベント購読の累積、後続の Handler 変更時に古い Window が反応する危険があります。[MAUI 10.0.70 の Window 実装](https://github.com/dotnet/maui/blob/10.0.70/src/Controls/src/Core/Window/Window.cs)

今回のスクリーンショットは最初の再生成と状態保持を実証していますが、システム外観を繰り返し変更した場合のライフサイクル健全性までは担保しません。MAUI は、再生成されたネイティブ Window に同じクロスプラットフォーム `Window` を再接続するライフサイクルを想定しています。[MAUI app lifecycle](https://learn.microsoft.com/en-us/dotnet/maui/fundamentals/app-lifecycle?view=net-maui-10.0)

**推奨修正**:

`NavigationPage` だけでなく `Window` 自体を `App` のインスタンスフィールドで保持し、再生成時に再利用してください。

```csharp
private Window? window;

protected override Window CreateWindow(IActivationState? activationState)
{
    return window ??= new Window(CreateNavigationPage());
}
```

修正後、デモページで入力状態を持たせたまま、システム外観を light → dark → light → dark と複数回変更し、ページ・入力状態・外観が維持されることを確認してください。

### [Minor] UI ブリーフの Theme 未設定画面に関する記述が合意済み差分と矛盾している

**該当箇所**: `kasane/changes/add-sample-dark-mode-toggle/ui/brief.md:7`

**問題点**:

ここでは Theme 未設定画面について「ライブラリ既定値が外観に自動追従する」と定義されていますが、同じ文書の後半および `deviation.md` 項目 1 では、背景・セル・セパレーター等の既定色は固定され、dark 外観には追従しないことが合意済みです。

合意済み差分そのものは問題ありませんが、画面・状態の概要が実際の仕様と食い違っています。

**推奨修正**:

該当文を、Theme 未設定画面はライブラリ既定値をそのまま表示し、外観非追従は `deviation.md` 項目 1 の合意済み例外である、と明記する内容へ更新してください。

## 前回指摘の確認

前回の指摘は、提示された実装と証跡上すべて対応済みと判断しました。

- MAUI Android の `ConfigChanges.UiMode` と `OnConfigurationChanged()` → `Recreate()` が復元されている
- デモページ滞在中の外観変更、ページ維持、入力状態保持を証跡で確認できる
- MAUI iOS／iOS PickerCell の選択面を dark 表示で確認できる
- MAUI iOS の `inputView` 時刻ピッカーを dark 表示で確認できる
- Android の `Configuration()` と `fontScale` に関する KDoc が差分 Configuration の意味を明確化している
- `SampleThemeFollower` は購読元 `Application` を保持し、同じインスタンスから購読解除している
- Android 選択表示文字列の集約、ファイル分割、設定記憶が反映されている
- 実機識別子はプレースホルダー化されている

`deviation.md` の2項目は合意済み差分として扱い、違反には数えていません。

指定どおり静的レビューのみ実施し、ビルド・テスト・lint は再実行していません。提示された実行結果と4実行面の証跡画像を前提に判定しています。

## 判定

**CHANGES_REQUESTED**

## 突き合わせ結果 (ホスト review-002.md との照合、2026-09-05)

| 指摘 | 出典 | 採否 | 根拠 |
|---|---|---|---|
| Activity 再生成のたびに古い `Window` が同じ `NavigationPage` を購読し続ける (`App.cs` の static 保持) | 相方 Major / ホスト Suggestion 2 (同じ箇所: `s_navigation` の寿命と再利用範囲) | **確定 (Major)** | 双方一致 (相方が高い方を主張)。MAUI の `Window` は `Page` の Handler 系イベントを購読し、ネイティブ Window 破棄で解除しないという機構の根拠が具体的。修正サイクル 2 で `Window` 自体の保持 (または MAUI の activation state による既存 Window 再利用) に切り替え、外観を複数回往復してページ・入力状態・外観の維持を実測する |
| brief.md 冒頭「ライブラリ既定色がそのまま外観に追随する」が deviation 1 項目目と矛盾 | 相方のみ (Minor) | **採用** (Minor) | 文書内の整合。オーケストレーターが注記を追記 |
| 同じ外観を選び直しても `recreate()` する (Android) / iOS テストの負の検証が nil でも通る | ホストのみ (Suggestion) | 確定 (Suggestion、修正サイクルで対処) | 数行で閉じる |

未解決 (両者矛盾): なし。確定 1 / 採用 1 / 降格 0。
