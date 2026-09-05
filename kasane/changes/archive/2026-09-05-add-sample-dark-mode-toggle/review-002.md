# レビュー結果: add-sample-dark-mode-toggle (002 回目)

**日付**: 2026-09-05
**判定**: APPROVED

## サマリー

review-001.md / second-opinion-code-001.md の指摘 10 件はすべて決着している — 対応済み 9 件、反証を認めるもの 1 件 (Android の `Configuration()` と fontScale)。前回 blocking だった 2 件 (PickerCell 選択面の実機証跡なし / MAUI Android で表示中ページが追随しない) は、いずれも証跡付きで解消したことを画像と実装の双方で確認した。

追加で見た範囲 (MAUI Android の Activity 再生成経路、`App` のナビゲーション保持、Android の `SampleAppearance` 切り出し、3 面の文言・色値の一致) に Critical / Major は無い。ビルド・テスト・lint は本レビューで再実行して確認した (iOS 1009 tests / 0 failures、Swift 6 言語モード error 0、サンプル 3 面 + MAUI 両 TFM のビルド成功・警告 0、標準 lint 0 件)。残るのは Suggestion 3 件で、いずれも判定に影響しない。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/comment-policy.md` | 常時 (rule) |
| `kasane/handbook/cross/sample-parity.md` | `samples/**` のデモ画面・文言・デモデータの変更 |
| `kasane/handbook/cross/test-execution.md` | テストの実行と結果報告 |
| `kasane/handbook/cross/runtime-behavior-verification.md` | 実行時挙動 (外観解決・提示コンテナ・Activity 再生成) が絡む修正の完了判定 |
| `kasane/handbook/ios/swift6-language-mode-check.md` | `ios/Sources/**` を触る変更の完了判定 |
| `kasane/decisions/android/0020-bundled-theme-always-wrap-host-independent.md` (android/ADR-0020) | Android サンプルの Manifest テーマ・Activity 基底クラス |
| `kasane/decisions/cross/0016-sample-cross-platform-parity.md` (cross/ADR-0016) | 3 面の文言・構成一致 |
| `kasane/lessons/code-review.md` (L-001) / `kasane/lessons/process.md` (L-001〜L-007) | レビュー観点 |
| `kotlin-impl-skill` (config `domain-skills.android.code-review`) | Android 実装のレビュー観点 |

規約適合として節ごとに照合した点:

- **android/ADR-0020**: `samples/android/.../MainActivity.kt:47` は `ComponentActivity` のまま。`AndroidManifest.xml:23` の `@style/Theme.KsSettingsViewSample` は `res/values/themes.xml` / `res/values-night/themes.xml` とも `@android:style/Theme.Material*.NoActionBar` を親に取り、AppCompat / MaterialComponents の XML テーマを持ち込んでいない。MAUI 側 `Platforms/Android/MainActivity.cs:23` は `Maui.SplashTheme` のままで ADR の前提 (ライブラリが自前テーマをかぶせる) を崩していない
- **sample-parity**: 見出し「外観」と 3 項目 (システム / ライト / ダーク)・並び順・読み上げ文言「選択中」は 3 面とも 1 箇所の定義 (`SampleAppearance.swift` / `SampleAppearance.kt` / `SampleAppearance.cs`) を参照し一字一句一致。dark プリセットは platform 固有の semantic color を使わず同一 RGBA を 3 面へ写している。予約日の範囲 2026/06/01〜06/20 と初期値も 3 面一致。ルートメニューの chrome (チェック印の色・divider) の差は「platform の見た目そのもの」として許容範囲内
- **comment-policy**: 新規・改訂コメントに作業文書パス・変更 ID・ローカル通番・仮称の混入なし。ADR 参照は `cross/ADR-0016` / `android/ADR-0020` の ID 形式。`SampleAppearance.*` 相互参照はリポジトリ内ソースファイルへの参照 (許容) で、参照先の実在も確認した。履歴記述 (「〜から移植」「旧実装」等) は無い。lint も 0 件 (検査 771 ファイル。検出範囲は規約より狭いため本文からも判定した)
- **swift6-language-mode-check**: `SWIFT_VERSION=6` を渡したビルドで `-swift-version 6` が実際に渡ったことと 126 件の SwiftCompile を確認し **error 0 件**。`ios/Package.swift` は無変更 (`git status` で差分 0)
- **runtime-behavior-verification / process L-003**: 本サイクルで挙動が変わった 2 経路 (iOS の提示外観引き継ぎ / MAUI Android の uiMode 受け取り) とも、修正後の実機・Simulator 画像が `ui/verification/` にあり、`ui/brief.md` に観測点が記録されている

## 前回指摘の対応状況

| # | 出典 / 重要度 | 指摘 | 判定 | 根拠 |
|---|---|---|---|---|
| 1 | review-001 🟠 Major | 本体を触った提示経路 2 箇所のうち PickerCell 選択面の実機証跡が無い | **対応済み** | `ui/verification/maui-ios-picker-dark.png` (MAUI ダークで提示物・地色ともダーク) と `ios-picker-dark.png` (iOS Native 非回帰。同じ描画) を確認。`ui/brief.md` の「照合結果 (MAUI iOS)」「照合結果 (iOS Native)」にも 1 行ずつ記録済み |
| 2 | second-opinion-code-001 [Major] | MAUI Android で `ConfigChanges.UiMode` を外したため、デモページ表示中の端末外観変更が Activity 再生成になり「表示中のページも追随する (SHALL)」を満たせない | **対応済み** | `Platforms/Android/MainActivity.cs:26-28` で `ConfigChanges.UiMode` を復元し、`OnConfigurationChanged` で night ビットの変化だけを検出して `Recreate()`。`App.cs:14,22-23` が同じ `NavigationPage` を作り直した Window へ載せ直す。証跡 `maui-android-basic-cells-system-device-dark.png` で、同じ「基本 Cell 7 種デモ」に留まったまま dark プリセットへ切り替わり「最後にタップ: Tanaka Taro」も保たれることを確認。非回帰 (ライト / ダーク選択 / 再起動維持) も brief に画素一致で記録済み |
| 3 | review-001 🟡 Minor | `Configuration()` が fontScale を 1.0 に戻す | **反証を認める (指摘を撤回)** | 下記「撤回した指摘」参照 |
| 4 | review-001 🟡 Minor | Android 側 `SampleAppearance` の KDoc が指す iOS 対応ファイルが誤り | **対応済み** | `SampleAppearance.kt:9-10` が `SampleAppearance.swift` と `SampleAppearance.cs` を正しく指す。`SampleScreen.kt` に `SampleAppearance` の記述は残っていない (grep 0 件)。iOS `SampleAppearance.swift:9-11` と MAUI `SampleAppearance.cs:27-28` も 3 面相互に辿れる形に揃っている |
| 5 | review-001 🟡 Minor | `SampleThemeFollower` の購読フラグと実体が食い違い得る | **対応済み** | `SampleThemeFollower.cs:23,52-70` が `_subscribedTo` (購読先 `Application` の参照) を持ち、解除はその参照に対して行う形になった。`Application.Current` が取れない経路でフラグだけ残る穴は塞がっている |
| 6 | second-opinion-code-001 [Minor] | brief.md に Emulator の実シリアルが残る | **対応済み** | `ui/brief.md` は 2 箇所とも `<android-serial>`。`identity-lint.py` も 0 件 |
| 7 | review-001 🔵 Suggestion | 「選択中」の読み上げ文言が 3 面の画面側にハードコード | **対応済み** | `SampleAppearance.selectedAccessibilityLabel` / `SampleAppearance.SELECTED_LABEL` / `SampleAppearances.SelectedLabel` へ集約され、`ContentView.swift:34` / `MenuScreen.kt:69` / `MenuPage.cs:78` が参照している |
| 8 | review-001 🔵 Suggestion | Android だけ `SampleAppearance` が専用ファイルを持たない | **対応済み** | `samples/android/.../SampleAppearance.kt` を新設 |
| 9 | review-001 🔵 Suggestion | inputView 経由のピッカーが MAUI ダークで同症状を出さないかが未確認 | **対応済み (症状なしを実測)** | `maui-ios-time-picker-dark.png` で TimePickerCell「アラーム」のホイール・アクセサリバー・地色がいずれもダークで描かれることを確認。deviation 2 項目目の範囲を広げる必要はない |
| 10 | review-001 🔵 Suggestion | ルートメニューが composition ごとに SharedPreferences を読む | **対応済み** | `MainActivity.kt:101` が `remember { SampleAppearanceStore.load(context) }` になった |

## 撤回した指摘 (review-001 🟡 Minor の fontScale)

**結論: 実装側の反証が正しく、review-001 の指摘は誤りだったので撤回する。コード変更は不要。**

review-001 は「`Configuration()` の既定コンストラクタは `setToDefaults()` を呼び、`fontScale = 1` を含む既定値で埋める」を前提にしていたが、これは 2 つのコンストラクタを取り違えている。`android.content.res.Configuration` の引数なしコンストラクタは `unset()` を呼び、`fontScale = 0` を含む「未設定」状態を作る (javadoc も "Construct an invalid Configuration. This state is only suitable for constructing a Configuration delta ... In order to create a valid standalone Configuration, you must call `setToDefaults`" と、差分専用であることを明示している)。`fontScale = 1` になるのは `setToDefaults()` を明示的に呼んだ場合だけで、`SampleAppearanceStore.nightModeOverride` は呼んでいない。

取り込み側も同じ結論になる。`Configuration.updateFrom(delta)` は `delta.fontScale > 0` のときだけ `fontScale` を取り込むため、0 のまま渡した差分は端末側の値を上書きしない (`densityDpi` / `screenLayout` / locale list なども同様に「未設定」ガードを持つ)。したがって review-001 の推奨修正 `fontScale = 0f` は既定値の再代入であり no-op で、入れても入れなくても挙動は同じ。実装側が Emulator 実測で反証したのと、AOSP の実装・javadoc の記述は一致している。

`SampleAppearanceStore.kt:44-47` の KDoc は改訂後の記述 (「引数なしの `Configuration()` は全フィールドを『未設定』で作る差分用のコンストラクタで、`Configuration.updateFrom` は未設定のフィールドを取り込まない」) が上記の機構と正確に対応しており、内容に誤りは無い。

## 実行した客観確認

| 検査 | 結果 |
|---|---|
| iOS 全テスト (`cd ios && xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,name=iPhone 17 Pro'`) | **TEST SUCCEEDED**。バンドル集計 166 + 88 + 94 + 7 + 654 = **1009 tests / 0 failures**。`PresentationAppearanceTests` 8 件も全 pass |
| iOS Swift 6 言語モード (`xcodebuild build ... SWIFT_VERSION=6`) | **error 0 件** (`-swift-version 6` の伝播と 126 SwiftCompile を確認、BUILD SUCCEEDED)。`ios/Package.swift` 無変更 |
| Android Sample (`samples/android` で `./gradlew :app:assembleDebug --offline`) | BUILD SUCCESSFUL (59 tasks up-to-date = 現行ソースでコンパイル済み) |
| MAUI Sample `net10.0-android` | ビルド成功 / 警告 0 / エラー 0 |
| MAUI Sample `net10.0-ios` | ビルド成功 / 警告 0 / エラー 0 |
| 標準 lint (`local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py`) | いずれも 0 件 (comment-policy は検査対象 771 ファイル) |

## 指摘事項

### [🔵 Suggestion] Android は同じ外観を選び直しても Activity を作り直す

**該当箇所**: `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/MainActivity.kt:102-106`

**問題点**:
`onSelectAppearance` は現在の選択と同じ項目をタップしたときも `save` → `activity?.recreate()` を通る。実効外観は変わらないのに Activity が作り直されるため、画面が一度消えて描き直される。iOS (`ContentView.swift:26` は `@AppStorage` への同値代入で再描画も起きない) と MAUI (`MenuPage.cs:123-126` は `UserAppTheme` を同値で入れ直すだけ) は同じ操作が実質 no-op なので、3 面で同じ操作の見え方が揃っていない。

**推奨修正**: 記憶した現在値を名前付きで持ち、変わったときだけ保存と再生成を行う。

```kotlin
val current = remember { SampleAppearanceStore.load(context) }
MenuScreen(
    appearance = current,
    onSelectAppearance = { appearance ->
        if (appearance != current) {
            SampleAppearanceStore.save(context, appearance)
            activity?.recreate()
        }
    },
    ...
)
```

### [🔵 Suggestion] `App` のナビゲーション保持が「Activity 再生成をまたぐ」以外の経路にも効く (未実測の所見)

**該当箇所**: `samples/maui/KsSettingsView.Sample.Maui/App.cs:11-14,22-23`

**問題点**:
`s_navigation` は `static` で、`CreateWindow` は 2 回目以降も同じ `NavigationPage` を返す。コメントは「アプリの寿命の間 1 つだけ作る」「Android は外観の切り替えで Activity を作り直し」と再生成経路を説明しているが、実際の寿命はプロセスであり、**外観切り替え以外の理由で Activity だけが作り直される経路** (バックで root Activity を終了した後にプロセスが生きたままランチャから再起動する、システムがメモリ都合で Activity を破棄した後に戻る、など) でも同じ再利用が起きる。その場合アプリはルートメニューではなく直前に開いていたページで再開する。

状態が保たれること自体は望ましい挙動にも読めるため「不具合」とは主張しない (この経路は実測していない)。ただしコメントの説明範囲と実際の寿命がずれており、次に触る人が「外観切り替えのときだけ効く」と読む余地がある。

**推奨修正**: どちらかで揃える。(a) コメントに「保持はプロセス寿命であり、Activity が作り直される経路すべてでナビゲーションが引き継がれる」と実体を書き足す、(b) 再利用を外観切り替え起点の再生成に限定する。(a) なら 1 行で閉じる。

### [🔵 Suggestion] 負の検証が `presentationController` の nil でも通る形になっている

**該当箇所**: `ios/Tests/KsSettingsViewUITests/PresentationAppearanceTests.swift:125,158,106`

**問題点**:
`XCTAssertNil(sheet.presentationController?.overrideTraitCollection)` は、`presentationController` 自体が nil の場合も無条件に通る。同ファイルの正の検証 (`:116-118` / `:148-151`) が非 nil を保証しているので現状は検出力が失われていないが、将来 VC の組み立てが変わって `presentationController` が作られなくなると、負の検証 3 本が黙って空振りに変わる。

**推奨修正**: 負の検証の前に `XCTAssertNotNil(sheet.presentationController)` を 1 行足す (`:106` の window 未接続ケースは presentationController が生成される前提ではないため対象外としてよい)。

## 確認して問題がなかった観点

- **MAUI Android の再生成経路**: `MainActivity.cs:41-56` は night ビットの変化だけを見て `Recreate()` するため、向きや画面サイズなど他の宣言済み config 変更では再生成しない。再生成後の `OnCreate` が `_nightMode` を新しい値で取り直すので再帰的な再生成にもならない
- **`SampleThemeFollower` の寿命**: `Attach` が返す追随役はページのイベント購読から根を持つため保持不要という説明どおり。購読は Loaded で張り Unloaded で外れ、`_subscribedTo` の nil ガードで二重購読も起きない。Activity 再生成でページが Unloaded されないまま Loaded を受けても、`_apply` は走り購読は 1 本のまま
- **light 側の不変性**: `SampleTheme.maui(dark: false)` / `SampleTheme.Apply(view, false)` / `SampleTheme.maui(false)` の 3 面とも、旧定義と同じ色ロール・同じ値を返す。MAUI が light 時に `CellValueTextColor` / `CellDescriptionColor` へ渡す `null` は未指定と等価
- **dark プリセットの色値**: 10 色ロールすべてが承認モックの対応表および 3 面の定義と一致 (verify-002.md の検算表)
- **`SampleAppearance` の 3 面同型**: 表示文言・並び順・初期値・見出し・読み上げ文言のすべてが定義側 1 箇所に閉じ、画面側にリテラルが残っていない
- **Android の Configuration 上書き**: `attachBaseContext` で `super` の直後に `applyOverrideConfiguration` を呼ぶ形は Resources 生成前という前提を満たす。「システム」は `null` を返して上書きしないため端末追随が保たれる
- **Compose 側の実効外観**: `isSystemInDarkTheme()` は Activity の Configuration を読むため、上書きと端末夜間モードの両方が反映される。`MaterialTheme` の light / dark 分岐と `SampleTheme.maui(dark)` の分岐が同じ源から解決される
- **Compose の LazyColumn key 衝突**: 外観行のキー (`System` / `Light` / `Dark`) とデモ行のキー (`SampleScreen.route`) は重複しない
- **MAUI の行モデル**: `MenuRow` は外観行と画面行のどちらかだけを持つ 2 コンストラクタで、`OnSelectionChanged` の分岐が漏れなく対応する。`MenuGroup` の見出しは `SampleAppearances.SectionTitle` と `SampleScreen.Groups` の名前をそのまま使い、二重管理になっていない
- **pbxproj**: 追加した `A1000001000000000000A019` / `A1000002000000000000A019` は既存 ID と衝突せず、buildFile 1 / fileRef 1 / group 1 / Sources 1 の既存パターンに揃っている
- **足場の凍結**: `proposal.md` (10:26) / `specs/*/spec.md` (10:22–10:26) / `ui/mock/approved.png` (10:22) は実装着手以降 (12:03 以降) に更新されていない

## アクションプラン

1. **[Suggestion]** Android の外観行タップに同値ガードを入れる (`MainActivity.kt:102-106`)
2. **[Suggestion]** `App.cs` のナビゲーション保持のコメントに実際の寿命を書き足す (または再利用を再生成経路に限定する)
3. **[Suggestion]** `PresentationAppearanceTests` の負の検証に `XCTAssertNotNil(...presentationController)` を添える

いずれも APPROVED を妨げない。着手しない場合もそのまま蒸留へ進んでよい。
