# Exploration: settingsview-migration-defects

## 課題 / 動機

`../ColorAnalyzer` を `AiForms.Maui.SettingsView` から `KsSettingsView.Maui` 0.1.0-beta.2 へ移行する作業 (`../ColorAnalyzer/kasane/changes/migrate-settingsview-to-kssettingsview`) の中で見つかった、KsSettingsView 側の不具合 3 件。実機 (iPhone 11 / iOS 18.7.8、Pixel 6a / Android 16) とシミュレータで確認した。

いずれも移行先の画面が表示できない・挙動が退行する形で現れるため、AiForms からの移行者が最初に踏む可能性が高い。

なお、同じ移行作業で見つかった「テーマ切替で Cell の色が追従しない」件は、既存の `maui-appthemebinding-coverage` に同じ見立てが起票済みだったため、そちらへ裏付けを追記した (`DynamicResource` でも `AppThemeBinding` と同じ挙動になる、という観測)。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## 未決の論点

- 未探索 (簡易起票)

### 1. 暗黙 Style を当てると `SettingsView` のコンストラクタで NullReferenceException

`x:Key` を持たない `Style TargetType="ks:SettingsView"` (暗黙 Style) をアプリのリソース辞書に置くと、`SettingsView` を含むページを開いた時点で落ちる。**暗黙 Style は MAUI の正常な使い方であり、利用側で避けるべきものではない。**

```
System.NullReferenceException
   at KsSettingsView.SettingsView.ApplyTheme()
   at KsSettingsView.SettingsView.<>c.<.cctor>b__217_25(BindableObject, Object, Object)
   at Microsoft.Maui.Controls.Setter.Apply(BindableObject, SetterSpecificity)
   at Microsoft.Maui.Controls.Style.ApplyCore(...)
   at Microsoft.Maui.Controls.MergedStyle.set_ImplicitStyle(IStyle)
   at Microsoft.Maui.Controls.MergedStyle.RegisterImplicitStyles()
   at Microsoft.Maui.Controls.MergedStyle..ctor(Type, BindableObject)
   at Microsoft.Maui.Controls.StyleableElement..ctor()
   at KsSettingsView.SettingsView..ctor()
```

機序: `SettingsView..ctor()` → 基底の `StyleableElement..ctor()` → `MergedStyle..ctor()` → `RegisterImplicitStyles()` で暗黙 Style の Setter が適用され、`ApplyTheme()` が走る。**この時点で `SettingsView` 自身のコンストラクタ本体はまだ実行されておらず**、`_controller` が null。

該当箇所:

- `maui/KsSettingsView.Maui/SettingsView.cs:489-496` — `_controller` はコンストラクタ**本体**で `new KsSettingsController(this)` される
- 同 `:987` — `private void ApplyTheme() => _controller.SetTheme(CreateThemeSnapshot());` が `_controller` を**無条件参照**
- 同 `:149`〜`:213` — 多数の `propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme()`
- 同 `:78` / `:95` — これらの callback は `_controller?` と null 条件演算子で**守られている**

つまり **`ApplyTheme` 系だけが守られていない**。`:78` / `:95` と同じ形にするのが素直な直し方に見えるが、null のときに Theme の適用を単に捨ててよいか (後で再適用されるか) は未確認。

回避策: Style に `x:Key` を付けて `Style="{StaticResource ...}"` で明示適用する。キー付き Style は構築完了後に適用されるため `MergedStyle..ctor` の経路を通らない。ColorAnalyzer 側では暫定的にこれを入れている。

### 2. Android でスクロール位置が Handler 再接続をまたいで失われる

設定リストをスクロールした状態で別ページへ遷移して戻ると、**リストが先頭に戻る**。iOS では起きない。

**Sample で再現済み** — 「Header / Footer への View 配置デモ」の「離れて戻る（Handler 切断 → 再接続）」(`samples/maui/KsSettingsView.Sample.Maui/Pages/AccessoryViewsDemoPage.xaml.cs:82-100`) を最下部までスクロールした状態で押すと、スクロールが先頭付近へリセットされる。この導線は `PopAsync` → `PushAsync(this)` で**同一ページインスタンスのまま Handler の切断→再接続だけ**を起こすため、ページ再生成ではなく **Handler 再接続が引き金**であることが分かる。

該当箇所:

- `maui/KsSettingsView.Maui/Handlers/SettingsViewHandler.cs:96-106` — `DisconnectHandler` が `view.ReleaseHost()` を呼ぶ
- `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:284-293` — `ReleaseHost()` が `_gateway.ReleaseHost()` へ
- `android/kssettingsview-bridge/src/main/kotlin/.../KsSettingsBridge.kt:129-134` — `releaseHost()` が `view.unbind()` して `hostView = null`
- 同 `:107-115` — `makeHostView()` は `hostView` が無ければ `KsSettingsView(context)` を**新規生成**し、Store から表示を復元

Host は世代ごとに作り直され、復元されるのは **Store が持つもの (設定ツリーと Theme) だけ**。スクロール位置は Store にない。

`android/kssettingsview/src/main/kotlin/.../KsSettingsView.kt:947-970` の `onSaveInstanceState` も見たが、`SavedState` が保持するのは `calendarCellId` と `calendarDisplayState` の 2 つだけ (`:1125-1148`)。Kotlin の main ソース全体を `scrollToPosition` / `firstVisibleItem` 等で走査したが、リスト本体のスクロール位置を保存・復元する実装は存在しない。

**直し方の前例がある**: `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:305-310` は、見た目スタイルについて「Native 側でも見た目スタイルは Store の外にあるため、Native Host を作り直すと失われる。現在値をここで控え、接続時に配信することで Host の世代をまたいで保つ (maui/ADR-0023)」という同型の対処を既に実装済み。**スクロール位置がこの「控えて再配信する」対象に入っていないだけ**。

未確認: **なぜ Android だけ Host が破棄されるのか**。`DisconnectHandler` → `ReleaseHost` の経路は iOS/Android 共通コードなので、両方で捨てられているなら iOS でも失われるはず。MAUI 本体のソースは未読で、platform 差 (Android の NavigationPage が fragment の view を破棄する等) の推測に留まる。

関連: `ScrollToTop` / `ScrollToBottom` は「スクロール制御はまだ公開していない」= 後続フェーズ予定のため、**失われた位置を利用側から復元する API も現状ない**。

### 3. Skill の Cell への DynamicResource 記述が不正確

`skills/{en,ja}/kssettingsview-maui/references/cells.md:3` と `skills/{en,ja}/kssettingsview-aiforms-migration/references/api-mapping.md:45` が、Section / Cell について「`{Binding}` は効くが `x:Reference` と `{DynamicResource}` は解決されない」と書いている。

実測では **初期解決は届く**ため「解決されない」は不正確。正しくは「**一度きりの解決は届くが、リソース辞書の差し替え通知は Cell まで伝播しない**」。

この記述は AiForms からの移行者が Cell の色指定をどう書くかを直接左右する。実際 ColorAnalyzer の移行では、この記述を根拠に「Cell 個別の `AccentColor` を画面全体の `CellAccentColor` へ集約する」という不要な設計変更が入りかけた (オーナーの指摘で撤回)。

実測の詳細と裏付けは `maui-appthemebinding-coverage` の未決の論点 (2026-09-11 追記) に記録した。

### 補足: AiForms 移行で写像先が無かったもの

不具合ではないが、移行対応表の充実に関わる観測として記録する。

- `HeaderPadding` / `FooterPadding` の**左右方向に代替がない**。上下は `SectionMargin` で作れるが、`SectionMargin` の左右成分は `Classic` では無視されるため、見出しの左インデントは `HeaderView` の自作でしか変えられない。AiForms の `HeaderPadding="14,0,0,6"` に対し KsSettingsView の既定は 16.7pt で、2pt の差が残る
- `HeaderHeight` と `SectionMargin` が**加算される**点が移行時につまずきやすい。AiForms の `HeaderHeight="42"` は「余白込みの確保領域」だったため、そのまま持ち込むと Section 間が広くなりすぎる。対応表に「`HeaderHeight` は帯そのものの高さであり、`SectionMargin` とは別に加算される」と明記されていると移行者が助かる

## ADR 候補 (作成済み: なし / 未起票: なし)

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: 未判定

3 件とも独立しており、それぞれ別に直せる。1 は `?.` 1 箇所で済む可能性があり、2 は ADR-0023 と同型の実装が要る。3 は文書修正。
