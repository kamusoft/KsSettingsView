# レビュー結果: add-sample-dark-mode-toggle (001 回目)

**日付**: 2026-09-05
**判定**: CHANGES_REQUESTED

## サマリー

3 面の外観切替・dark プリセット・カレンダー範囲デモは、承認モックの色ロール対応表どおりの RGBA で 3 面に同一定義され、文言・並び順も一致していて、実装の骨格は素直で読みやすい。iOS 本体の `PresentationAppearance` も提示経路 2 箇所を漏れなく通り、`ios/Sources/` 内の `present(` 呼び出しは他に存在しないことを確認した。ビルド・テスト (iOS 1009 件 / 0 失敗)・Swift 6 言語モード error 0・標準 lint 0 件はいずれも再実行して確認済み。

一方、本体を触った 2 経路のうち **PickerCell の選択面には実機・Simulator の証跡がまったく無い**。ライブラリ利用者の目に見える描画が変わる修正であり、process/L-003 (利用者可視の変更は実機証跡の実在と提出コードとの対応を判定条件にする) に照らして Major とした。他は Minor / Suggestion。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/comment-policy.md` | 常時 (rule) |
| `kasane/handbook/cross/sample-parity.md` | `samples/` のデモ画面・文言・デモデータの追加・変更 |
| `kasane/handbook/cross/test-execution.md` | テストの実行と結果報告 |
| `kasane/handbook/cross/runtime-behavior-verification.md` | 実行時挙動 (外観解決・提示コンテナ) が絡む修正の完了判定 |
| `kasane/handbook/ios/swift6-language-mode-check.md` | `ios/Sources/` を触る変更の完了判定 |
| `kasane/decisions/android/0020-bundled-theme-always-wrap-host-independent.md` (android/ADR-0020) | Android サンプルの Manifest テーマ・Activity 基底クラス |
| `kasane/decisions/cross/0016-sample-cross-platform-parity.md` (cross/ADR-0016) | 3 面の文言・構成一致 |
| `kasane/lessons/code-review.md` (L-001) / `kasane/lessons/process.md` (L-002 / L-003 / L-005 / L-006 / L-007) | レビュー観点 |
| `kotlin-impl-skill` (config `domain-skills.android.code-review`) | Android 実装のレビュー観点 |

規約適合として確認した点:

- **android/ADR-0020**: `MainActivity` は `ComponentActivity` のまま。`AndroidManifest.xml:23` の `@style/Theme.KsSettingsViewSample` は `values/` `values-night/` とも `@android:style/Theme.Material*.NoActionBar` を親に取り、AppCompat / MaterialComponents の XML テーマを持ち込んでいない。「素の framework テーマでも動く」検証装置の条件は保たれている
- **swift6-language-mode-check**: `SWIFT_VERSION=6` 付きビルド (ログに `-swift-version 6` が渡ったことと 126 件の SwiftCompile を確認) で **error 0 件**。`ios/Package.swift` は無変更
- **comment-policy**: 新規コメントに作業文書パス・変更 ID・ローカル通番・`SHALL` 等の混入なし。ADR 参照は `cross/ADR-0016` / `android/ADR-0020` の ID 形式で書かれている。lint も 0 件 (ただし lint の検出範囲は規約より狭いため、本文からも読んで判定した)
- **sample-parity**: 見出し「外観」と 3 項目の文言・並び順は 3 面で一字一句一致し、いずれも 1 箇所の定義 (`SampleAppearance` / `SampleAppearances`) を参照している。dark プリセットは platform 固有の semantic color を使わず同一 RGBA を 3 面に置いている

## 指摘事項

### [🟠 Major] 本体を触った提示経路 2 箇所のうち、PickerCell 選択面の実機証跡が存在しない

**該当箇所**: `ios/Sources/KsSettingsViewUI/PickerCellView.swift:98-110` / `ui/brief.md` の「照合結果 (MAUI iOS)」

**問題点**:
deviation.md 2 項目目 (オーナー裁定で Non-Goal を撤回し本体を修正した項目) の対象は「iOS 本体の提示経路 (シート・ポップオーバー等、同型の提示を持つ箇所すべて)」であり、実装は `PickerCellView` と `DatePickerCellView` の 2 箇所に入っている。しかし `ui/verification/` にある A/B 証跡 (`maui-ios-calendar-dark-range-before.png` → `maui-ios-calendar-dark-range.png`) と非回帰証跡 (`maui-ios-calendar-system-dark-range.png` / `ios-calendar-dark-range.png`) は**すべて DatePickerCell のカレンダーシートのもの**で、PickerCell の選択面は 4 実行面のどの画像にも写っていない (`maui-ios-input-cells-dark.png` は一覧のみで選択面を開いていない)。

PickerCell 選択面は `UINavigationController` を pageSheet として提示する経路であり、修正前は MAUI ダーク時に画面全体がライトで描かれ、修正後はダークになる — カレンダーシートより見た目の変化が大きい。にもかかわらず担保はユニットテスト (`PresentationAppearanceTests.swift` の `test_選択面は…`) だけで、これが観測しているのは `overrideUserInterfaceStyle` / `overrideTraitCollection` という内部値であって、利用者が見る画面ではない。`kasane/lessons/process.md` L-003 は「(3) 承認済み照合の後に視覚へ影響する修正を入れたら影響スクショを再撮影するか証跡範囲を明記する / (4) レビューは証跡の実在と提出コードとの対応を判定条件にし、実機確認を Suggestion へ格下げしない」と定めており、そのまま該当する。`kasane/handbook/cross/runtime-behavior-verification.md` の「修正前後の A/B と証跡の change 配下保存」も同じ要求をしている。

**推奨修正**:
次の 2 枚を撮って `ui/verification/` に追加し、`ui/brief.md` の「照合結果 (MAUI iOS)」「照合結果 (iOS Native)」へ 1 行ずつ記録する。

1. MAUI iOS で「ダーク」選択中に入力 Cell 5 種デモの PickerCell (「テーマ」または「通知種別」) の選択面を開いた状態 — 提示物と地色が両方ダークであること
2. iOS Native で同じ選択面 — 修正前と同じ描画のままであること (非回帰)

撮り直しが難しい場合の代替は、`ui/brief.md` に「PickerCell 選択面は本 change では実機未確認 (ユニットテストのみ)」と証跡範囲を明記したうえでオーナー判断を仰ぐこと。証跡なしで「同型の提示を持つ箇所すべてで解消した」と読める現状の記述だけを残すのは避けたい。

### [🟡 Minor] Android の Configuration 上書きが端末のフォントスケールを 1.0 に戻す

**該当箇所**: `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/SampleAppearanceStore.kt:48-57`

**問題点**:
`Configuration()` の既定コンストラクタは `setToDefaults()` を呼び、`fontScale = 1` を含む既定値で埋める。この Configuration を `applyOverrideConfiguration` に渡すと、`ResourcesManager` 側で `baseConfig.updateFrom(override)` が走り、`updateFrom` は `delta.fontScale > 0` のときそのまま取り込むため、**端末側でフォントサイズを大きくしている利用者が「ライト」「ダーク」を選ぶと、サンプルの文字サイズが 1.0 倍へ戻る**。「システム」は `null` を返して上書きしないので、この症状は明示選択時だけ出る。

コード中のコメントは `uiMode` の type / night が別マスクで取り込まれることを正しく説明しているが、`fontScale` が同じ `updateFrom` で無条件に取り込まれる点は考慮に入っていない。Emulator の既定 (fontScale 1.0) では見えないため、証跡画像にも現れない。

**推奨修正**:
`fontScale` を「未指定」にして `updateFrom` の取り込み条件から外す (1 行)。

```kotlin
// updateFrom は fontScale > 0 のとき無条件に取り込むため、未指定 (0) にして端末の設定を残す。
return Configuration().apply {
    fontScale = 0f
    uiMode = night
}
```

反映後は Emulator の「表示サイズとテキスト」でフォントを最大にし、「ダーク」を選んでも文字サイズが変わらないことを 1 度確認するのが確実。

### [🟡 Minor] Android 側 `SampleAppearance` の KDoc が指す iOS の対応ファイルが誤っている

**該当箇所**: `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/SampleScreen.kt:60`

**問題点**:
`enum class SampleAppearance` の KDoc に「対応する iOS 側定義: samples/ios/KsSettingsViewSample/SampleScreen.swift」とあるが、iOS の `SampleAppearance` は `samples/ios/KsSettingsViewSample/SampleAppearance.swift` にあり、`SampleScreen.swift` には存在しない (同ファイル冒頭の `SampleScreen` 自体の対応先としては正しいので、enum ごとコピーされた際に追随し損ねたと読める)。逆方向の `SampleAppearance.swift` と `SampleAppearance.cs` は正しい相手を指しているため、片方向だけ迷子になっている。comment-policy はソースファイル名への参照を許容参照としているが、それは「grep で到達でき、消えれば壊れに気づける」ことが前提で、誤った参照先はその前提を満たさない。

**推奨修正**: 参照先を `samples/ios/KsSettingsViewSample/SampleAppearance.swift` に直す。あわせて MAUI の `SampleAppearance.cs` も対応先として併記すると 3 面が相互に辿れる (iOS 側 `SampleAppearance.swift` は Android のみを挙げているので、こちらも同様)。

### [🟡 Minor] MAUI の追随役が購読を解除できないまま生き残る経路がある

**該当箇所**: `samples/maui/KsSettingsView.Sample.Maui/SampleThemeFollower.cs:60-69`

**問題点**:
`OnPageUnloaded` は `if (!_subscribed || Application.Current is not { } application) return;` で早期に戻るが、`Application.Current` が取れなかった場合に `_subscribed` を `true` のまま残す。以後 `OnPageLoaded` は `if (_subscribed …) return;` で再購読をスキップするため、**購読は Application 側に残ったまま、解除する機会が二度と来ない**。`Application` はプロセス寿命を持ち、ハンドラ経由で follower → `_apply` クロージャ → ページと `Settings` を掴み続けるので、ページが解放されない。

シャットダウン中など `Application.Current` が null になる場面は限られるため実害の確率は低いが、`_subscribed` フラグと実際の購読状態が食い違い得る構造そのものが読み手を誤らせる。

**推奨修正**: 解除は購読対象の参照を自分で持つ形にし、フラグと実体を一致させる。

```csharp
private Application? _subscribedTo;   // 購読先。解除は必ずこの参照に対して行う

private void OnPageLoaded(object? sender, EventArgs e)
{
    _apply(SampleTheme.IsDark);
    if (_subscribedTo is not null || Application.Current is not { } application) return;
    application.RequestedThemeChanged += OnRequestedThemeChanged;
    _subscribedTo = application;
}

private void OnPageUnloaded(object? sender, EventArgs e)
{
    if (_subscribedTo is not { } application) return;
    application.RequestedThemeChanged -= OnRequestedThemeChanged;
    _subscribedTo = null;
}
```

### [🔵 Suggestion] 読み上げ文言「選択中」だけが 3 面の画面側にハードコードされている

**該当箇所**: `samples/ios/KsSettingsViewSample/ContentView.swift:35` / `samples/android/.../MenuScreen.kt:70` / `samples/maui/KsSettingsView.Sample.Maui/MenuPage.cs:78`

**問題点**:
本 change は「文言を画面側に手書きすると表記ゆれが再発するため、定義はここ 1 箇所に閉じる」と 3 面すべての `SampleAppearance` 系ファイルに明記しているのに、読み上げ用の「選択中」だけがその外側 (各画面) に直書きされている。今は 3 面とも同じ文字列だが、この change が自分で立てた方針から外れた 1 語であり、次に触る人が片面だけ変える余地が残る。

**推奨修正**: `SampleAppearance.selectedAccessibilityLabel` / `SELECTED_LABEL` / `SelectedLabel` として各 platform の定義側へ移し、画面はそれを参照する。

### [🔵 Suggestion] Android だけ `SampleAppearance` が専用ファイルを持たない

**該当箇所**: `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/SampleScreen.kt:54-82`

**問題点**:
iOS は `SampleAppearance.swift`、MAUI は `SampleAppearance.cs` を新設しているのに、Android だけ `SampleScreen.kt` の末尾に同居させている。同居しているせいで前掲の Minor (対応ファイルの誤参照) も起きている。3 面の構造が揃っていると、次の追随作業で「どこを見ればよいか」が platform をまたいで同じになる。

**推奨修正**: `SampleAppearance.kt` へ切り出す (同一 package なので参照側の変更は不要)。

### [🔵 Suggestion] inputView 経由の選択面 (TimePicker / NumberPicker / DatePicker wheels) が MAUI ダークで同じ症状を出さないかが未確認

**該当箇所**: `ios/Sources/KsSettingsViewUI/TimePickerCellView.swift:42` / `NumberPickerCellView.swift:47` / `DatePickerCellView.swift:53`

**問題点**:
deviation.md 2 項目目が挙げる原因は「MAUI の `UserAppTheme` が window ではなく root VC に外観上書きを掛けるため、window 直下に置かれる提示コンテナが端末の外観のまま残る」こと。同じ「window 直下の別コンテナ」に載る UI として、`embeddedField.inputView` / `inputAccessoryView` に載せている 3 種のピッカー (アラーム = TimePickerCell、サイズ = NumberPickerCell、誕生日 = DatePickerCell wheels) がある。これらはキーボードウィンドウ側に置かれるため、MAUI ダークで同じくライトのまま描かれる可能性がある。

ただし `UIPresentationController` とは提示機構が異なり、UIKit がキーボードウィンドウの trait をどう解決するかは実測しないと確定しない。`ui/verification/maui-ios-input-cells-dark.png` は一覧のみで、いずれのピッカーも開いていないため、現時点では**症状の有無そのものが未確認**。したがってここでは「本体に不具合がある」とは主張しない。

**推奨修正**: Major の撮り直しをするなら、そのついでに MAUI iOS ダークで「アラーム」を開いた 1 枚を撮り、(a) ダークで描かれるなら `ui/brief.md` に「inputView 経路も確認済み」と 1 行残す、(b) ライトで描かれるなら deviation.md 2 項目目の「同型の提示を持つ箇所すべて」の範囲に入るかをオーナーへ確認する — のどちらかに倒す。数分で範囲の曖昧さが消える。

### [🔵 Suggestion] Android のルートメニューが composition 中に毎回 SharedPreferences を読む

**該当箇所**: `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/MainActivity.kt:99`

**問題点**:
`appearance = SampleAppearanceStore.load(context)` を composable の呼び出し引数に直接置いているため、ルートメニューが recompose されるたびに `getSharedPreferences` + `getString` が走る。初回はディスク I/O を伴い、以後もキャッシュ参照とはいえ Compose の状態ではない値を composition の途中で読む形になっている (値が変わるのは `recreate()` 後だけなので描画は正しい)。

**推奨修正**: `remember { SampleAppearanceStore.load(context) }` にするか、Activity 側で一度読んで渡す。挙動は変わらず、composition が副作用を持たない形に揃う。

## 確認して問題がなかった観点

- **light 側の不変性**: iOS の旧 `mauiTitleText` (#CC9900) は `mauiHeaderText` と同値で、`mauiTitleText(dark: false)` が同じ色を返す。MAUI の `CellValueTextColor` / `CellDescriptionColor` へ light 時に渡す `null` は `BindableProperty` の既定 `default(Color)` と同値 (`maui/KsSettingsView.Maui/SettingsView.cs:328-333,360-365`) で、未指定と等価。3 面とも light の実効色は変わらない
- **dark プリセットの色値**: 10 色ロールすべてが承認モックの対応表と 3 面で一致 (verify-001.md の検算表)
- **`PresentationAppearance` の判定条件**: `source == window` のときに `nil` を返して上書きを付けない設計は、通常ホスト (iOS Native / SwiftUI の `preferredColorScheme`) で挙動を変えないことを意味し、非回帰の観点で妥当。`UITraitCollection(userInterfaceStyle:)` を `overrideTraitCollection` に渡す形は他の trait (size class 等) を潰さない
- **提示順序の非回帰**: `PickerCellView` は presenter 解決より前に VC を組み立てるようになったが、旧コードでも `makeListViewController()` は presenter 解決より前に呼ばれていたため副作用の順序は変わらない。`DatePickerCellView` は多重提示ガードと presenter 解決の順序が元のまま
- **テスト seam の安全性**: `_makeCalendarSheetControllerForTesting()` は `currentCalendarController` を書き換えないため、テストが多重提示ガードの状態を汚さない
- **Android の Activity 再生成経路**: `AndroidManifest.xml` に `configChanges` の指定がないため、「システム」選択中の端末夜間モード変更でも Activity が再生成され `attachBaseContext` からやり直される。MAUI 側も `Platforms/Android/MainActivity.cs` から `ConfigChanges.UiMode` を外して同じ形に揃えてある
- **Compose の LazyColumn key 衝突**: 外観行のキー (`System` / `Light` / `Dark`) とデモ行のキー (`store` / `dsl` / …) は重複しない
- **pbxproj**: 追加した `A1000001000000000000A019` / `A1000002000000000000A019` は既存 ID と衝突せず、buildFile 2 箇所・fileRef 3 箇所という既存の出現パターンに揃っている
- **ビルド / テスト / lint**: iOS 1009 tests / 0 failures、Swift 6 言語モード error 0、Android Sample `assembleDebug` 成功、MAUI Sample `net9.0-ios` ビルド成功、標準 lint 3 本すべて 0 件 (いずれも本レビューで実行)

## アクションプラン

1. **[Major]** PickerCell 選択面の証跡を 2 枚 (MAUI iOS ダーク / iOS Native 非回帰) 追加し、`ui/brief.md` に記録する。撮らない場合は証跡範囲の限定を brief.md に明記してオーナー判断へ上げる
2. **[Minor]** `SampleAppearanceStore.nightModeOverride` に `fontScale = 0f` を足し、フォントスケールが上書きで潰れないようにする
3. **[Minor]** `SampleScreen.kt:60` の iOS 対応ファイル参照を `SampleAppearance.swift` に直す
4. **[Minor]** `SampleThemeFollower` の購読フラグを購読先参照に置き換える
5. **[Suggestion]** 1 と同じ機会に inputView 経路 (アラーム) を 1 枚撮り、deviation 2 項目目の範囲の曖昧さを消す
6. **[Suggestion]** 「選択中」の文言を 3 面の定義側へ移す / `SampleAppearance.kt` を切り出す / `remember` で SharedPreferences 読み出しを 1 回にする
