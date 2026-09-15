# Exploration: settingsview-migration-defects

## 課題 / 動機

`../ColorAnalyzer` を `AiForms.Maui.SettingsView` から `KsSettingsView.Maui` 0.1.0-beta.2 へ移行する作業 (`../ColorAnalyzer/kasane/changes/migrate-settingsview-to-kssettingsview`) の中で見つかった、KsSettingsView 側の不具合 3 件。実機 (iPhone 11 / iOS 18.7.8、Pixel 6a / Android 16) とシミュレータで確認した。

いずれも移行先の画面が表示できない・挙動が退行する形で現れるため、AiForms からの移行者が最初に踏む可能性が高い。

なお、同じ移行作業で見つかった「テーマ切替で Cell の色が追従しない」件は、既存の `maui-appthemebinding-coverage` に同じ見立てが起票済みだったため、そちらへ裏付けを追記した (`DynamicResource` でも `AppThemeBinding` と同じ挙動になる、という観測)。

## 検討した選択肢 (却下案と理由を含む)

- 論点 1 の直し方: (a) callback の null 安全化 + コンストラクタ末尾の一括同期 / (b) controller・binder の遅延生成。探索の推しは (b)。詳細は「未決の論点」の論点 1
- 論点 2 の直し場所: MAUI facade 側で位置を控えて再配信する案は、Native に位置を渡す窓口が無く phase-8 の設計判断と重なるため本 change では採らない。Android Native の付け外しに閉じた修正を主案とする
- 論点 3 の直し場所: skills を直接直す案は AGENTS.md の規約 (skills の追従は docs-refresh 経由のみ) に反するため却下。concepts を直して docs-refresh に繋ぐ

## 決定事項

- 2026-09-15 変更級 S で直接実装に進む (ユーザー確定)。提案書は作らず、tasks は実装フェーズで持つ
- 論点 2 は「通常遷移で Android だけ先頭に戻る」経路 (Android Native の window 付け外し) だけを本 change で直す。Host 世代をまたぐスクロール位置の保持は phase-8-scroll-control の agenda に論点として積んだ
- 論点 3 は concepts (`maui/api/maui-facade.md`) の記述を直し、skills への反映は蒸留後の docs-refresh に委ねる
- `maui-rendering-lifecycle.md` の「ページ離脱で Host は解放される」の乖離は本 change の concepts 更新に含める (drift として別扱いにしない)
- 実装の完了判定は `cross/runtime-behavior-verification.md` に従い、`evidence/` の修正前証跡と同じ導線で修正後を確認する (Android 実機の通常遷移、および暗黙 Style を当てた `SettingsView` の起動)

## 未決の論点

- 探索済み (2026-09-15、実測あり)。3 論点の番号は起票時のまま

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

**探索での追加観測 (2026-09-15)**:

- 守られていない callback は Theme 系だけではない。`Root` (`SettingsView.cs:36`)、`RootHeaderView` / `RootFooterView` の propertyChanged (`:83` / `:100`。validateValue 側だけが `?.` で守られている)、`ListStyle` (`:141`) も `_controller` を無条件参照している。暗黙 Style で `ListStyle="Modern"` を当てるのは自然な書き方なので、Theme だけ塞いでも別の入口が残る
- `?.` で握りつぶすだけでは足りない。controller は既定の Theme 写し (`KsSettingsController.cs:118` の `new()`) を持って生まれ、Handler 接続時 (`:203`) にその写しを配信する。暗黙 Style で先に色が入っても、後で別の色が変わらない限り `ApplyTheme()` は走らないため、「落ちないが Style の色が効かない」画面になる。`ApplyTheme()` の呼び出し元はプロパティ callback と `BackgroundColor` 変更 (`:933`) だけで、コンストラクタ本体からは呼ばれていない
- 直し方の候補: (a) callback を null 安全にし、コンストラクタ本体の末尾で現在値を一括同期する (Theme・ListStyle・Root accessory View)。(b) controller と binder を「最初に触られた時点で作る」遅延生成にし、基底コンストラクタ中の通知も普通に受ける。推しは (b) — 同期漏れが構造的に消える。ただし `_sectionBinder` / `_sectionContextBinder` も同じ順序問題を持つので、遅延生成の対象は 3 つまとめて考える

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

**探索での実測と裏取り (2026-09-15)**: 証跡は `evidence/` (ファイル名が導線と前後を表す)

| 導線 | OS / 端末 | 結果 |
|---|---|---|
| 通常遷移: ColorAnalyzer (0.1.0-beta.2) の「その他」で最下部までスクロール → 「ライセンス情報」を Push → 戻る | Android / Pixel 6a 実機 | **先頭に戻る** (`android-coloranalyzer-normal-nav-*`) |
| Sample 導線: Header / Footer デモ最下部で「離れて戻る」(Pop → 同一インスタンスを Push) | Android / Pixel 6a 実機 | 先頭付近に戻る (`android-sample-reconnect-*`) |
| Sample 導線: 同上 | iOS / iPhone 17 Pro Simulator (iOS 26.0) | **先頭に戻る** (`ios-sample-reconnect-*`)。Sample の ⑦ の表示が「離脱中の Handler: 切断」を示しており、Host が作り直されている |

MAUI 本体のソース読み (scout、dotnet/maui main と release/10.0.1xx):

- **Push で背後に回るページの Handler は Android でも iOS でも切られない**。Android は Fragment を作り直すが、同じ Activity なら Handler と platform view をそのまま新 Fragment へ付け替える (`src/Core/src/Platform/Android/MauiContextExtensions.cs` の 4 引数 `ToPlatform`、`StackNavigationManager` のコメント「Fragments are always destroyed if they aren't visible / The Handler/PlatformView associated with the visible IView remain intact」)。`NavigationViewFragment` に `OnDestroyView` での切断はない
- Handler が切られるのは **スタックから消えるページ自身** で両 OS 共通 (`src/Controls/src/Core/Page/Page.cs` `SendNavigatedFrom` の Pop 分岐 → `DisconnectHandlers()`)。Sample の「離れて戻る」はこの経路
- Android 固有で teardown する経路は Shell / TabbedPage / FlyoutPage の Fragment 系にあるが、`ShellFragmentContainer.OnDestroyView` の切断は main ブランチのみで net9 / net10 の出荷ブランチには無い。ColorAnalyzer は NavigationPage + タブ構成で、通常遷移は Push 経路
- 一方 Android Native の設定リスト (`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:327-338`) は `onDetachedFromWindow` で `recyclerView.adapter = null` にし、`onAttachedToWindow` (`:280-292`) で同じ `concatAdapter` を戻す。`RecyclerView.setAdapter` は同一インスタンスでも全 ViewHolder を作り直す動作で、LayoutManager のアンカーが消えて先頭から再レイアウトされる。コメントに「**スクロール位置は復元対象に含まない**」と明記済み (ViewPager2 / Compose `AndroidView` の付け外しを想定した意図的な仕様)。iOS の Host (`UITableView`) は window から外れても `contentOffset` を保つ
- iOS の `releaseHost` (`ios/Sources/KsSettingsViewBridge/KsSettingsBridge.swift:102-106`) は Android と同型で Host を捨てる。よって「Host 破棄の有無」では左右差を説明できない

結論: 症状は 2 つの経路に分かれる。

1. **通常遷移 (Push して戻る)**: Host は両 OS で生き続ける。Android だけ先頭に戻るのは **Android Native の window 付け外しがスクロール位置を捨てる**ため。これが ColorAnalyzer で見た実症状で、**直す場所は Android Native 側** (adapter を切る直前に `layoutManager.onSaveInstanceState()` を控え、付け直した後に `onRestoreInstanceState` する)。MAUI に限らず ViewPager2 / Compose `AndroidView` の Native 利用者にも効く
2. **Host 世代をまたぐ経路 (Pop されたページ自身の再 Push、Activity 再生成等)**: 両 OS で先頭に戻る。位置を保つには facade 側で控えて再配信する仕組み (maui/ADR-0023 の見た目スタイルと同型) と、Native にスクロール位置を渡す窓口 (現状無い) が要る。これは phase-8-scroll-control の「命令系 API の層配置」と同じ設計判断を含むため、**本 change では扱わず phase-8 の論点に積む**

concepts の乖離候補: `kasane/concepts/maui/api/maui-rendering-lifecycle.md:50` は「ページ離脱 (Handler 切断) で Host は解放される」と書くが、Push で背後に回る通常の離脱では Handler は切られず Host も生きている (両 OS)。切られるのは Pop で消えるページ自身。記述を実態に合わせる必要がある (本 change の concepts 更新に含めるか、drift として別扱いかは未決)

### 3. Skill の Cell への DynamicResource 記述が不正確

`skills/{en,ja}/kssettingsview-maui/references/cells.md:3` と `skills/{en,ja}/kssettingsview-aiforms-migration/references/api-mapping.md:45` が、Section / Cell について「`{Binding}` は効くが `x:Reference` と `{DynamicResource}` は解決されない」と書いている。

実測では **初期解決は届く**ため「解決されない」は不正確。正しくは「**一度きりの解決は届くが、リソース辞書の差し替え通知は Cell まで伝播しない**」。

この記述は AiForms からの移行者が Cell の色指定をどう書くかを直接左右する。実際 ColorAnalyzer の移行では、この記述を根拠に「Cell 個別の `AccentColor` を画面全体の `CellAccentColor` へ集約する」という不要な設計変更が入りかけた (オーナーの指摘で撤回)。

実測の詳細と裏付けは `maui-appthemebinding-coverage` の未決の論点 (2026-09-11 追記) に記録した。

**探索での追加観測 (2026-09-15)**: 同じ記述の正本は concepts にある (`kasane/concepts/maui/api/maui-facade.md:90`「`x:Reference` と `DynamicResource` は届かない」)。skills は concepts からの派生物で、追従は docs-refresh 経由に限る (AGENTS.md)。よって本 change で直すのは concepts の記述で、skills (cells.md / api-mapping.md) と補足の移行対応表 (HeaderPadding 左右・HeaderHeight の加算) は蒸留後に docs-refresh を依頼して反映する。

### 補足: AiForms 移行で写像先が無かったもの

不具合ではないが、移行対応表の充実に関わる観測として記録する。

- `HeaderPadding` / `FooterPadding` の**左右方向に代替がない**。上下は `SectionMargin` で作れるが、`SectionMargin` の左右成分は `Classic` では無視されるため、見出しの左インデントは `HeaderView` の自作でしか変えられない。AiForms の `HeaderPadding="14,0,0,6"` に対し KsSettingsView の既定は 16.7pt で、2pt の差が残る
- `HeaderHeight` と `SectionMargin` が**加算される**点が移行時につまずきやすい。AiForms の `HeaderHeight="42"` は「余白込みの確保領域」だったため、そのまま持ち込むと Section 間が広くなりすぎる。対応表に「`HeaderHeight` は帯そのものの高さであり、`SectionMargin` とは別に加算される」と明記されていると移行者が助かる

## ADR 候補 (作成済み: なし / 未起票: なし)

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: S (2026-09-15 ユーザー確定)

判定材料 (2026-09-15):

- 触る能力: MAUI facade のコンストラクタ順序 (論点 1)、Android Native の window 付け外し (論点 2 の経路 1)、concepts の記述 2 箇所 (論点 3 と lifecycle の乖離) — 独立した 3 箇所だが、いずれも既存契約の中での修正で公開 API は変えない
- 可逆性: いずれも局所的で戻せる。Android Native の LayoutManager 状態の控えは既存の `onDetachedFromWindow` / `onAttachedToWindow` の対に閉じる
- UI: 見た目の変更なし。実行時挙動の不具合なので `cross/runtime-behavior-verification.md` の完了判定 (修正前後の同一手順・証跡) が要る。修正前の証跡は本探索で取得済み
- 1 変更に収めない部分: 論点 2 の経路 2 (Host 世代をまたぐスクロール位置の保持) は phase-8-scroll-control へ

迷ったら 1 段上の規律で M も候補。M にする理由があるとすれば「Android Native + MAUI facade + concepts の 3 領域を 1 change で触り、それぞれに回帰テストと実機確認が要る」点。
