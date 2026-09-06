# レビュー結果: add-sample-dark-mode-toggle (003 回目)

**日付**: 2026-09-05
**判定**: APPROVED

## サマリー

前サイクルで確定した指摘 4 件 (相方 Major 1 / 相方 Minor 1 / ホスト Suggestion 2) はすべて対応済みで、対応の中身をコード・証跡の双方で確認した。とくに `App` の `Window` 保持への切り替えは、指摘が要求した「複数回往復の実測」まで証跡が付いている。

新規に見た範囲 (`Window` 再利用の寿命、Android の同値ガードの再読込経路、iOS 提示経路の網羅性、3 面の色値・文言・日付範囲の一致) に Critical / Major は無い。ビルド・テスト・lint は本レビューで全件再実行し、iOS 1009 tests / 0 failures、Swift 6 言語モード error 0、サンプル 4 ビルドすべて成功 (新規警告 0)、標準 lint 3 本とも 0 件を確認した。

残るのは Minor 2 件 (いずれも低優先度) と Suggestion 1 件で、判定を妨げない。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/comment-policy.md` | **常時** (rule)。新規・改訂コメントを持つ全ファイル |
| `kasane/handbook/cross/sample-parity.md` | `samples/**` のデモ画面・文言・デモデータの変更 |
| `kasane/handbook/cross/test-execution.md` | テストの実行と結果報告 (iOS 全件 / Android Sample ビルド / MAUI 両 TFM) |
| `kasane/handbook/cross/runtime-behavior-verification.md` | 外観解決・提示コンテナ・Activity 再生成という実行時挙動の完了判定 |
| `kasane/handbook/ios/swift6-language-mode-check.md` | `ios/Sources/**` を触る変更の完了判定 |
| `kasane/decisions/android/0020-bundled-theme-always-wrap-host-independent.md` (android/ADR-0020) | Android サンプルの Manifest テーマ・Activity 基底クラス |
| `kasane/decisions/cross/0016-sample-cross-platform-parity.md` (cross/ADR-0016) | 3 面の文言・構成・パラメータ一致 |
| `kasane/lessons/code-review.md` (L-001) / `kasane/lessons/process.md` (L-001〜L-008) | レビュー観点 |
| `kotlin-impl-skill` (config `domain-skills.android.code-review`) | Android 実装のレビュー観点 |

適用外と判定した文書: `cross/public-identifiers.md` (build.gradle.kts / Package.swift / csproj のいずれにも触れていない)、`cross/aiforms-origin-reference.md` (未移植機能の実装ではない)、`cross/user-skill-api-listing.md` (`skills/` 無変更)、`cross/local-development-setup.md` / `cross/release-procedure.md` (guide、本作業の対象外)、`maui/integration-host-verification.md` / `maui/performance-verification.md` (`maui/` の binding / facade 層は無変更、性能評価もしていない)。

節ごとに照合した結果:

- **android/ADR-0020**: `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/MainActivity.kt:47` は `ComponentActivity` のまま。`samples/android/app/src/main/res/values/themes.xml` / `values-night/themes.xml` の `Theme.KsSettingsViewSample` はどちらも `@android:style/Theme.Material*.NoActionBar` を親に取り、AppCompat / MaterialComponents の XML テーマを持ち込んでいない。MAUI 側 `samples/maui/KsSettingsView.Sample.Maui/Platforms/Android/MainActivity.cs:23` は `Maui.SplashTheme` のまま
- **sample-parity**: 「外観」見出し・3 項目の文言・並び順・読み上げ文言はいずれも 3 面とも定義 1 箇所 (`SampleAppearance.swift` / `SampleAppearance.kt` / `SampleAppearance.cs`) に閉じ、画面側にリテラルが残っていない (grep で確認)。dark プリセット 9 色定数は 3 面とも同一 RGBA (`#1B1915` / `#2A2620` / `#4A3F28` / `#E0B040` / `#9A948A` / `#7A756C` / `#E6E1D6` / `#B8B2A6` / `#9A948A`)。予約日の範囲 2026/06/01–06/20 と初期値 2026/06/01 も 3 面一致。`sectionDecorationDemo(dark:)` が触る色ロール (下地・header / footer 背景) と触らない色ロール (箱・separator・文字色) の切り分けも 3 面同型
- **comment-policy**: 新規・改訂コメントに作業文書パス・変更 ID・ローカル通番・仮称・行番号参照の混入なし。ADR 参照はすべて `cross/ADR-0016` / `android/ADR-0020` の ID 形式。`SampleAppearance.*` / `SampleAppearanceStore.*` の相互参照はリポジトリ内ソースファイル名への参照 (許容) で、参照先の実在も確認した。時間軸を含む記述は 1 箇所だけ検出 (下記 Minor 1)。lint も 0 件 (検査対象 771 ファイル) だが、検出範囲は規約より狭いため本文からも判定した
- **swift6-language-mode-check**: `SWIFT_VERSION=6` を渡したビルドで `-swift-version 6` が実際に swiftc へ渡ったことを確認し **BUILD SUCCEEDED / error 0 件**。`ios/Package.swift` は `git status` で差分 0 (一時設定を残していない)
- **runtime-behavior-verification / process L-003**: 本サイクルで挙動が変わった経路 (MAUI Android の Window 再利用) は、実測手順・結果・非回帰まで `ui/brief.md:92-97` に記録され、最終状態の画像 `ui/verification/maui-android-input-cells-system-device-toggle-x3.png` がある

## 前回指摘の対応状況

| # | 出典 / 重要度 | 指摘 | 判定 | 根拠 |
|---|---|---|---|---|
| 1 | second-opinion-code-002 [Major] (突き合わせで確定) | Activity 再生成のたびに古い `Window` が同じ `NavigationPage` を購読し続ける | **対応済み** | `samples/maui/KsSettingsView.Sample.Maui/App.cs:20,26-27` が `Window? _window` を持ち `CreateWindow` は `_window ??= new Window(CreateNavigation())` を返す。`NavigationPage` の static 保持は消えている (`s_navigation` は grep 0 件)。実測は `ui/brief.md:92-97` — 入力状態を持たせたまま端末外観を 5 回切り替え (3 往復) し、ページ・入力状態・プリセットが最後まで保たれ、プロセス同一・例外なしを確認。証跡 `ui/verification/maui-android-input-cells-system-device-toggle-x3.png`。非回帰 (「ダーク」明示選択・強制終了後の再起動維持) も同じ節に記録 |
| 2 | second-opinion-code-002 [Minor] | brief.md 冒頭の Theme 未設定画面の記述が deviation 1 項目目と矛盾 | **対応済み** | `ui/brief.md:7` の当該行末に注記が入り、「実装フェーズで 3 面とも固定ライト値で追随しないと判明し、本 change では合意済み例外とした (本体側は `fix-default-colors-dark-appearance`)」と deviation を指している |
| 3 | review-002 🔵 Suggestion 1 | Android は同じ外観を選び直しても Activity を作り直す | **対応済み** | `MainActivity.kt:100-110` が `remember { SampleAppearanceStore.load(context) }` を名前付きで保持し、`appearance != currentAppearance` のときだけ `save` + `recreate()`。同値タップは保存も再生成も通らない。`remember` はルート離脱で破棄され戻り時に再読込するため、値が古いまま残る経路もない |
| 4 | review-002 🔵 Suggestion 2 | `App` のナビゲーション保持のコメントと実際の寿命がずれる | **対応済み (実装ごと差し替え)** | `App.cs:11-19` のコメントが、①再利用の理由 (MAUI の `Window` は載っているページの Handler 変更イベントを購読し、破棄時に解除しない)、②MAUI 既定の `CreateWindow` と同形であること、③引き継ぎが外観切り替え以外の再生成経路にも及ぶこと、の 3 点を明記している。前回指摘した「外観切り替えのときだけ効くと読める」余地は消えた |
| 5 | review-002 🔵 Suggestion 3 | iOS の負の検証が `presentationController` の nil でも通る | **対応済み** | `ios/Tests/KsSettingsViewUITests/PresentationAppearanceTests.swift:126,161` に `XCTAssertNotNil(...)` を追加。`:106` (window 未接続ケース) は前回の指摘どおり対象外のまま。9 テストとも pass |

## 実行した客観確認

| 検査 | 結果 |
|---|---|
| iOS 全テスト (`cd ios && xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,name=iPhone 17 Pro'`) | **TEST SUCCEEDED**。バンドル集計 166 + 88 + 94 + 7 + 654 = **1009 tests / 0 failures**。`PresentationAppearanceTests` は 9 件すべて pass (テスト名で個別確認) |
| iOS Swift 6 言語モード (`xcodebuild build ... SWIFT_VERSION=6`) | **BUILD SUCCEEDED / error 0 件**。swiftc 起動行に `-swift-version 6` が渡っていることを確認。`ios/Package.swift` は差分 0 |
| iOS Sample (`samples/ios` で `xcodebuild build -scheme KsSettingsViewSample`) | **BUILD SUCCEEDED / warning 0 件** (新規 `SampleAppearance.swift` の pbxproj 登録も含めて成立) |
| Android Sample (`samples/android` で `./gradlew :app:assembleDebug --offline --rerun-tasks`) | **BUILD SUCCESSFUL** (59 tasks executed = キャッシュ再利用なしで全件コンパイル)。警告 2 件はいずれも本 change が触っていない `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsWheelView.kt` の既存 deprecation |
| MAUI Sample `net10.0-android` | ビルド成功 / 警告 0 / エラー 0 |
| MAUI Sample `net10.0-ios` | ビルド成功 / 警告 0 / エラー 0 |
| 標準 lint (`local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py`) | いずれも 0 件 (comment-policy の検査対象 771 ファイル) |

## 指摘事項

### [🟡 Minor] Android Native だけ「再起動後の維持」と「システム選択中の端末外観追随」の記録が無い

**該当箇所**: `ui/brief.md:36-39` (照合結果 (Android Native) の観測点一覧)、`ui/verification/`

**問題点**:
他 3 実行面の節にはいずれも「外観の切り替え: 再起動後も『ダーク』を維持 (`*-menu-dark-relaunch.png`)、システム選択中に端末の外観を変えると追随 (`*-menu-system-device-dark.png`)」の観測行と対応画像がある (`ui/brief.md:52`、`:63`、`:79`)。Android Native の節にだけこの行が無く、`android-menu-dark-relaunch.png` / `android-menu-system-device-dark.png` に相当する画像も無い。

該当するのは samples-android の Scenario「選択が再起動後も維持される」と「システム選択中に端末の外観が変わると追随する」で、tasks 5.1 のチェックは 4 実行面で確認したと宣言しているが、change 配下に残っている記録は tasks.md のチェックだけになっている (process L-003 の「証跡を change 配下に残す」)。Android は 4 面で唯一、切り替え機構が非標準 (Activity の Configuration 上書き + `recreate()`) で spike タスク (3.0) まで置いた面であり、記録が薄いのがこの面であるのは据わりが悪い。

低優先度と判断した理由: 保存値の読み出し経路 (`SampleAppearanceStore.load` → `attachBaseContext` → `applyOverrideConfiguration`) は「ダーク」選択時の `recreate()` そのものが通す経路であり、`android-menu-dark.png` がその成立を既に示している。再起動で追加されるのは SharedPreferences ファイルがプロセスをまたいで残るという platform の契約だけで、コード側に固有の分岐は無い。「システム」追随側も `nightModeOverride` が `null` を返す (= 上書きなし) 形で、`AndroidManifest.xml` に `configChanges` を宣言していないため OS 標準の再生成に委ねている。

**推奨修正**: 次のどちらかで閉じる。(a) 該当 2 枚を撮って `ui/verification/` に置き、Android Native の節に他 3 面と同じ観測行を 1 行足す。(b) 撮影しない場合は、Android Native の節に「この 2 Scenario は tasks 5.1 の操作確認で担保し、画像は残していない」と証跡範囲を明記する (process L-003 (3) の「証跡範囲を明記する」)。

### [🟡 Minor] MAUI の doc コメントに時間軸を含む記述が 1 箇所ある

**該当箇所**: `samples/maui/KsSettingsView.Sample.Maui/SampleTheme.cs:146`

**問題点**:
`/// light 側は従来どおり未指定のまま残す。` の「従来どおり」は、このファイルだけを読んでいる人には何と比べているかが分からない。comment-policy の「禁止する記述類型 — 進捗ログ・履歴記述 (時間軸を含む記述)。現在の仕様を現在形で書く」に当たる。

同じ内容を説明する iOS `samples/ios/KsSettingsViewSample/SampleTheme.swift:156-157` と Android `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/SampleTheme.kt:199-200` は「色ロールの構成は light と同じで、description と valueText の色だけを追加で明示する」と現在形で書いており、MAUI だけが過去を参照している。`comment-policy-lint.py` は機械判定できない類型のため 0 件のまま通っている。

**推奨修正**: 「light 側は未指定のまま残す。」に直す (3 文字削るだけで意味は変わらない)。

### [🔵 Suggestion] ライブラリに増えた利用者可視の保証が、どの長命層にも記述されていない

**該当箇所**: `ios/Sources/KsSettingsViewUI/PresentationAppearance.swift:17-47`

**問題点**:
本 change で iOS 本体に「Cell から提示するモーダルは、提示元と window の実効外観が食い違うとき提示元の外観を引き継ぐ」という挙動が入った。これは deviation.md 2 項目目でオーナー裁定済みの合意差分であり、違反ではない。一方でこの保証は利用者から観測できる挙動 (ホストが root view controller 側に `overrideUserInterfaceStyle` を掛ける構成で、シート地色が提示元に揃う) でありながら、デルタスペックにも `kasane/concepts/core/styling/style-resolution.md` にも対応する記述が無い。実装範囲そのものは正しく、`ios/Sources` 内の `present(` 呼び出しは `PickerCellView.swift:109` と `DatePickerCellView.swift:118` の 2 箇所だけで、どちらも引き継ぎを通っていることを grep で確認済み (deviation が宣言する「同型の提示を持つ箇所すべて」を満たす)。

**推奨修正**: 蒸留 (ksn-distill) で concepts へ 1 段落として拾えるよう、指摘として残す。レビューでは何も直さなくてよい。

## 確認して問題がなかった観点

- **`Window` 再利用の寿命**: `App` は MAUI の DI でプロセスに 1 つなので、インスタンスフィールド `_window` は前サイクルの static と同じ寿命を持ち、かつ App 単位に閉じた。`CreateWindow` の実装は MAUI 既定 (`Windows` に既存があればそれを返す) と同形で、`Windows` から外れた状態で呼ばれても `_window` を返すため既定より頑健になっている
- **`App` コンストラクタでの外観適用**: `App()` は基底の `Application` コンストラクタが `Application.Current` を立てた後に走るため `SampleAppearanceStore.Apply` が効く。`Apply` 側にも `Application.Current is { }` のガードがあり、取れない経路で例外にならない
- **`SampleThemeFollower` の二重購読**: Activity 再生成でページが Unloaded を受けないまま Loaded を受けても、`_subscribedTo` の nil ガードで購読は 1 本のまま。`OnPageLoaded` 冒頭の `_apply` はガードの外にあるため、再生成後の再適用は必ず走る
- **iOS の外観引き継ぎの副作用**: `UITraitCollection(userInterfaceStyle:)` は他の trait を unspecified のまま残すため、`overrideTraitCollection` に与えてもサイズクラス等は親から継承される。`styleToInherit` が提示元 == window のとき `nil` を返す設計により、通常の iOS Native (SwiftUI の `preferredColorScheme` が window に効く) では上書きが付かず、`ios-picker-dark.png` / `ios-calendar-dark-range.png` の非回帰と整合する
- **inputView 経由のピッカーが対象外である根拠**: `TimePickerCellView` / `NumberPickerCellView` は `present(` を持たず inputView で出すため提示コンテナを経由しない。`KeyWindowResolver.topPresentedViewController()` の呼び出しも 2 箇所だけで、引き継ぎ漏れの経路が無い
- **Android の同値ガードと再読込**: `remember { load(context) }` は MENU_ROUTE の composition に閉じ、デモ画面へ遷移して戻ると再評価される。書き手はこの画面だけで、書き込みの直後に `recreate()` が入るため、表示中の値と保存値がずれる窓が無い。`save` は `apply()` だが同一プロセス内の in-memory 反映は同期なので、直後の `attachBaseContext` からの `load` は新しい値を読む
- **Android chrome の解決源**: `attachBaseContext` の上書きは Activity の Resources に効き、`values-night/` の同名 style・Compose の `isSystemInDarkTheme()`・ライブラリ UI がすべて同じ Configuration から解決される。「システム」は `null` を返して上書きしないため端末追随が保たれる
- **MAUI Android の再生成条件**: `OnConfigurationChanged` は night ビットの変化だけを見るため、向き・画面サイズなど他の宣言済み config 変更では再生成しない。`OnCreate` が `_nightMode` を取り直すので再帰的な再生成にもならない
- **light 側の不変性**: `SampleTheme.mauiLight` (iOS `:138` / Android `:180`) と MAUI `Apply(view, false)` は、旧定義と同じ色ロール・同じ値を返す。MAUI が light 時に `CellValueTextColor` / `CellDescriptionColor` へ渡す `null` は未指定と等価
- **足場の凍結**: `proposal.md` (10:26) / `specs/*/spec.md` (10:22–10:26) / `ui/mock/approved.png` (10:22) は実装着手以降に更新されていない。更新されているのは記録側の `tasks.md` (12:23) / `deviation.md` (11:53) / `ui/brief.md` (13:59) と証跡だけ
- **tasks.md の虚偽チェック**: 5.5 が宣言する 4 ビルドと lint 0 件は本レビューで全件再実行して成立を確認した。3.0 の spike も、`values-night/` リソース・Compose・ライブラリ UI が同じ Configuration から解決される形で実装に反映されている
- **pbxproj**: 追加した `A1000001000000000000A019` / `A1000002000000000000A019` は既存 ID と衝突せず、buildFile / fileRef / group / Sources の 4 箇所すべてに揃っている。iOS Sample のビルド成功がこれを裏づける

## アクションプラン

1. **[Minor]** Android Native の 2 Scenario について、証跡画像を足すか証跡範囲を明記する (`ui/brief.md` の照合結果 (Android Native))
2. **[Minor]** `samples/maui/KsSettingsView.Sample.Maui/SampleTheme.cs:146` の「従来どおり」を削る
3. **[Suggestion]** 蒸留で `PresentationAppearance` の保証を concepts に拾う (レビューでの修正は不要)

1 と 2 はどちらも数行で閉じる。いずれも APPROVED を妨げないため、着手せずそのまま蒸留へ進んでもよい。
