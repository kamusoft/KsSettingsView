# Exploration: maui-android-root-explicit-size-ignored

## 課題 / 動機

起票元: ../KsDialogs/kasane/changes/archive/2026-09-27-fix-maui-android-dialog-content-size/

**MAUI の View を Android のネイティブの親へそのまま渡すと、ルートの WidthRequest / HeightRequest が効かない**

### 何が起きたか

KsDialogs の MAUI Android で、ダイアログの中身のルート (`ContentView`) に付けた `WidthRequest=160` / `HeightRequest=100` が効かず、中の Label の大きさ (約 26×19) まで縮んで表示された。iOS では指定どおりの大きさで出ていた。

原因は、MAUI の View を platform view 化した結果 (`ToPlatform()`) を、Android のネイティブの親 (中身の LayoutParams で測る ViewGroup) にそのまま渡していたこと。MAUI 本体 (dotnet/maui) では、ルート自身の明示サイズは「MAUI の親が測るとき」にしか効かない。handler の `GetDesiredSizeFromHandler` が、明示サイズを EXACTLY の測定条件に変えて渡すからである。ネイティブの親に直接測られた場合の振る舞いは次のとおり。

| ルートの種類 | 振る舞い |
|---|---|
| `ContentView` / `Border` 系 | 中身の大きさ + 余白を返す (`LayoutExtensions.MeasureContent`)。自分の明示サイズは返らないので縮む |
| `Grid` / `StackLayout` 系 | 自分の明示サイズを返すため縮まない |
| どの種類でも | ルート自身の `Arrange` を呼ぶ親がいないため、ルートの `Width` / `Height` が -1 のままで、`SizeChanged` も届かない |
| `MinimumWidthRequest` / `MinimumHeightRequest` の指定 | Android の最小サイズへ写されるので効く |

実測は KsDialogs の実配置テストホスト (Android エミュレータ API 35 / 31、iOS Simulator) で行った。結果の行は source の `evidence/placement-host-runs.txt` にある。

### 相手に関係する理由

KsSettingsView の CustomCell も、任意の MAUI View を Android のネイティブの list に載せる作りと理解している。セルの中身をネイティブの親へ `ToPlatform()` の結果のまま渡しているなら、同じ症状が出る可能性がある。具体的には、ルートが `ContentView` / `Border` の CustomCell で、ルートの `HeightRequest` / `WidthRequest` が効かないこと。こちらは KsSettingsView のコードを読んでいないので、同じ作りかどうかは未確認。

### 提案する対応

判断は KsSettingsView 側に委ねる。確かめるなら、CustomCell のルートを `ContentView` にして `HeightRequest` を中身より大きく指定し、Android で指定どおりの高さになるかを見る。

同じ症状が出た場合、KsDialogs では Android 側にも iOS と同じ役割の包み (ViewGroup) を置いて直した。包みの `OnMeasure` で MAUI の `IView.Measure` を呼び、`OnLayout` で `IView.Arrange` を呼ぶ形である。MAUI 本体でも CollectionView のセル (`ItemContentView`) が同じやり方を使っている。実装は `maui/KsDialogs.Maui/Platforms/Android/PlatformDialogContent.cs` の `DialogContentView`、経緯と却下した案は source の `exploration.md` にある。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

## 未決の論点

- 未探索 (簡易起票)
