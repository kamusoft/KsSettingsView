# UI Brief: add-sample-dark-mode-toggle

## 画面と状態

- **ルートメニュー (3 面)** (状態: ライト / ダーク): 先頭に「外観」の項目群 (システム / ライト / ダーク) を置き、選択中の項目を識別できる形にする。既存の「デモ」(MAUI は「MAUI 固有」、iOS は「検証」も) の群はそのまま後続
- **Theme を渡すデモ画面** (基本 Cell 7 種 / 入力 Cell 5 種 / CustomCell / Section 装飾) (状態: light プリセット / dark プリセット): 実効外観がダークのとき dark プリセットで描画される。見た目の正は承認モックの「基本 Cell 7 種デモ — dark プリセット」
- **Theme を渡さないデモ画面** (Store / DSL / 共通フィールド統合 / isVisible) (状態: ライト / ダーク): ライブラリ既定色がそのまま外観に追随する。見た目の正はライブラリ側 (モックは持たない)。**注記 (2026-09-05、deviation.md 1 項目目)**: 実装フェーズで 3 面ともライブラリ既定の背景・Cell 背景・separator が固定ライト値で外観に追随しないと判明し、本 change では追随しないことを合意済み例外とした (本体側は `fix-default-colors-dark-appearance` で扱う)
- **Android サンプル chrome** (状態: ライト / ダーク): 実効外観に追随する。見た目は framework テーマと Material3 の標準 light / dark に任せる (モックは持たない)
- **MAUI サンプル chrome**: ナビゲーションバーは固定色のまま (両外観で判読できる)。ページ下地は MAUI の標準 light / dark に任せる (モックは持たない)
- **予約日の選択面** (状態: 通常のみ): 2026/06 の月表示で、21 日以降が範囲外 disabled として見える (各 platform のカレンダーは前後の月の日を描画しないため、同じ月内で範囲を切る)。「今日」は端末の今日が範囲外なら無変更
- loading / empty / error 状態なし (静的なデモ画面のため)

## リファレンス注釈

- references/ 画像なし (探索・提案中に画像の提示なし)
- light 側の見た目の正は現行実装 (`samples/ios/KsSettingsViewSample/SampleTheme.swift` の `maui`) そのまま。本 change で light 側は変えない

## デザイントークン参照

- Theme の色ロール (backgroundColor / cellBackgroundColor / separatorColor / selectedColor / cellAccentColor / headerTextColor / footerTextColor / disabledTextColor / cellTitleColor) の意味と解決順は [スタイルの所有と実効値解決](../../../concepts/core/styling/style-resolution.md)
- Sample の AiForms 互換色が製品契約ではないことは同文書「Sample の AiForms 互換色」と [Sample のプラットフォーム間一致](../../../handbook/cross/sample-parity.md)
- concepts/ にダーク用のトークン定義はない。dark プリセットの生値は承認モック (色ロール対応表) が持ち、実装は 3 面の `SampleTheme` に同一 RGBA で写す。dark 側は description / valueText の色ロール (`cellDescriptionColor` / `cellValueTextColor`) も持つ — Android の description 既定色は固定値で夜間に追随しないため

## 承認モック

mock/plan-a.html を採用 (approved.png)。2026-09-05 オーナー承認。案A = 暖色ダーク (light の色相を保って明度反転。既定色ダークと見分けがつくことを優先)。却下: 案B (中立ダーク) — iOS のライブラリ既定色ダークと見た目が近く、Theme の dark 値が効いているかをスクショで区別しにくい。

dark プリセットの色値の正は approved.png 内 (plan-a.html 下部) の色ロール対応表。approved.png はモックの描画のみで個人要素は含まない (確認済み)。

承認後の改訂 (2026-09-05、second-opinion-spec-001 反映): 予約日の範囲を 06/01〜06/20 に変更しカレンダー図から月外日を除去 (各 platform とも描画しないため)、対応表に description / valueText の色ロールを追加、light の valueText 表示色を実効値 (title へフォールバック) に修正。approved.png は改訂後を再取得済み。改訂版を 2026-09-05 オーナー再承認 (approved.png = 改訂版)。

## 照合結果 (Android Native)

2026-09-05、Emulator (API 35 / 1080x2340) で撮影し approved.png と照合した。撮影は 1 周で収束し、乖離修正の追加ラウンドは発生していない。画像は verification/ の `android-*.png` (個人要素なし。表示はデモデータのみ)。

- 構造: ルートメニュー先頭の「外観」項目群 (システム / ライト / ダーク、選択中にチェック) と後続の「デモ」群 (`android-menu-light.png` / `android-menu-dark.png`)、基本 Cell 7 種デモの Section 構成はモックと一致する
- ルートメニュー: ライト / ダークの外観で `android-menu-light.png` / `android-menu-dark.png`。入力 Cell 5 種デモの dark プリセットは `android-input-cells-dark.png`
- トークン: dark プリセットの 10 色ロールは対応表の RGBA をそのまま `SampleTheme` に写した (canvas #1B1915 / cell #2A2620 / separator #4A3F28 / accent #FFBF00 / header・ButtonCell title #E0B040 / footer #9A948A / disabled #7A756C / title #E6E1D6 / valueText #B8B2A6 / description #9A948A)
- 状態: light プリセットは現行のまま変化なし (`android-basic-cells-light.png`)。dark プリセットは `android-basic-cells-dark-1.png` / `-2.png`
- 予約日の選択面: 2026/06 表示で 21 日以降が無効、「今日」は選択を変えない (`android-calendar-dark-range.png`)。ダーク選択時は選択面もダーク配色
- 外観の切り替え: 「ダーク」選択後に強制終了して再起動しても「ダーク」を維持 (`android-menu-dark-relaunch.png`)、「システム」選択中に端末を夜間モードにするとルートメニュー表示中でも追随する (`android-menu-system-device-dark.png`)。2026-09-05 追加撮影、Emulator (<android-serial> / 1080x2340)。撮影前に前面が Android Native サンプルであることを確認済み

合意済み差分 (deviation.md 1 項目目、2026-09-05 オーナー裁定): Theme を渡さない画面のダーク描画。`android-visibility-dark.png` / `android-section-decoration-dark.png` のとおり、Android のライブラリ既定色は背景・separator・description が固定のライト値で、title / valueText だけが夜間の `textColorPrimary` (白系) に解決されるため、白地に白文字になる。モックはこの領域の見た目を持たず、本体側の追随は別 change `fix-default-colors-dark-appearance` で扱う (Theme 明示画面と chrome のダーク描画までを本 change の達成範囲とする)。

## 照合結果 (iOS Native)

2026-09-05、Simulator (iPhone 17 Pro / iOS 26.5) で撮影し approved.png と照合した。撮影は 2 周で収束した (1 周目の乖離: ルートメニューの外観項目名が既定 Button の accent 色で描かれ、モックの「項目名は行の既定色・選択中の印だけ accent」と食い違った。`buttonStyle(.plain)` + 印だけ `.tint` にして解消)。画像は verification/ の `ios-*.png` (個人要素なし。表示はデモデータのみ)。

- 構造: ルートメニュー先頭の「外観」項目群 (システム / ライト / ダーク、選択中にチェック) と後続の「デモ」「検証」群 (`ios-menu-light.png` / `ios-menu-dark.png`)、基本 Cell 7 種デモの Section 構成はモックと一致する
- ルートメニュー: ライト / ダークの外観で `ios-menu-light.png` / `ios-menu-dark.png`
- トークン: dark プリセットの 10 色ロールは対応表の RGBA をそのまま `SampleTheme` に写した (canvas #1B1915 / cell #2A2620 / separator #4A3F28 / accent #FFBF00 / header・ButtonCell title #E0B040 / footer #9A948A / disabled #7A756C / title #E6E1D6 / valueText #B8B2A6 / description #9A948A)。Android 側と同一値
- 状態: light プリセットは現行のまま変化なし (`ios-basic-cells-light.png`)。dark プリセットは `ios-basic-cells-dark-1.png` / `-2.png` と `ios-input-cells-dark.png`
- 予約日の選択面: 2026/06 表示で 21 日以降が無効、「今日」は選択も月表示も変えない (`ios-calendar-dark-range.png`)
- PickerCell の選択面: 「ダーク」選択時の入力 Cell 5 種デモの PickerCell (単一選択)「テーマ」の選択面は MAUI iOS と同じダーク描画で、提示外観の引き継ぎを入れる前と変わらない (`ios-picker-dark.png`)
- 外観の切り替え: 再起動後も「ダーク」を維持 (`ios-menu-dark-relaunch.png`)、「システム」選択中に端末の外観をダークへ変えるとアプリも追随 (`ios-menu-system-device-dark.png`)

合意済み差分 (deviation.md 1 項目目、2026-09-05 オーナー裁定): Theme を渡さない画面のダーク描画。`ios-visibility-dark.png` / `ios-section-decoration-dark.png` のとおり、iOS のライブラリ既定色も Android と同じく背景・Cell 背景・separator・header / footer 文字が固定のライト値で、title / description だけが `UIColor.label` / `.secondaryLabel` として夜間に解決されるため、白地に白文字になる。モックはこの領域の見た目を持たず、扱いは Android と同じ (deviation.md 1 項目目)。

## 照合結果 (MAUI iOS)

2026-09-05、Simulator (iPhone 17 Pro / iOS 26.5) で撮影し approved.png と照合した。撮影は 1 周で収束した。画像は verification/ の `maui-ios-*.png` (個人要素なし。表示はデモデータのみ)。

- 構造: ルートメニュー先頭の「外観」項目群 (システム / ライト / ダーク、選択中にチェック) と後続の「デモ」「MAUI 固有」群 (`maui-ios-menu-light.png` / `maui-ios-menu-dark.png`)、基本 Cell 7 種デモの Section 構成はモックと一致する
- ルートメニュー: ライト / ダークの外観で `maui-ios-menu-light.png` / `maui-ios-menu-dark.png`
- トークン: dark プリセットの 10 色ロールは対応表の RGBA をそのまま `SampleTheme` に写した。iOS Native / Android Native と同一値であることを定数名と値の対応表で突き合わせ済み
- 状態: light プリセットは現行のまま変化なし (`maui-ios-basic-cells-light.png`)。dark プリセットは `maui-ios-basic-cells-dark-1.png` / `-2.png` と `maui-ios-input-cells-dark.png`
- 外観の切り替え: 再起動後も「ダーク」を維持 (`maui-ios-menu-dark-relaunch.png`)、「システム」選択中に端末の外観をダークへ変えてアプリへ戻ると追随する (`maui-ios-menu-system-device-dark.png`。表示したままでは切り替わらず、再開時に反映される)
- 予約日の選択面: 2026/06 表示で 21 日以降が無効、「今日」は選択も月表示も変えない (`maui-ios-calendar-dark-range.png`)
- PickerCell の選択面: 「ダーク」選択時に入力 Cell 5 種デモの PickerCell (単一選択)「テーマ」の選択面を開くと、提示物 (選択肢の一覧) と地色の両方がダークで描かれる (`maui-ios-picker-dark.png`)。カレンダーシートと並ぶもう一方の提示経路の証跡
- inputView 経由のピッカー: 「ダーク」選択時に TimePickerCell「アラーム」を開くと、ホイール・アクセサリバー・地色ともダークで描かれる (`maui-ios-time-picker-dark.png`)。提示コンテナで起きていた地色がライトのまま残る症状は、この経路では出ていない

合意済み差分と解消済みの乖離 (2026-09-05):
- 合意済み差分 (deviation.md 1 項目目): Theme を渡さない画面のダーク描画。`maui-ios-visibility-dark.png` / `maui-ios-section-decoration-dark.png` のとおり、iOS Native / Android Native と同じ症状 (背景・Cell 背景・separator が固定ライト値のまま、title だけ淡色) が MAUI でも出る
- 解消済み (deviation.md 2 項目目、オーナー裁定で iOS 本体を修正): 「ダーク」選択時のみ予約日の選択面のシート地色がライトのまま描かれていた件 (修正前の A 側は `evidence/maui-ios-calendar-dark-range-before.png`。verification/ には最終画像だけを置く)。原因は MAUI の `UserAppTheme` が外観上書きを window ではなく root VC に掛けるため、window 直下に置かれる提示コンテナ (`UIPresentationController`) が端末の外観のままになること。iOS 本体 (`ios/Sources/KsSettingsViewUI/PresentationAppearance.swift`) で提示元と window の外観が食い違うときに提示物へ提示元の外観を与える形で解消し、修正後の `maui-ios-calendar-dark-range.png` でシートがダークで描かれることを確認 (非回帰: `maui-ios-calendar-system-dark-range.png`、iOS Native `ios-calendar-dark-range.png`)

## 照合結果 (MAUI Android)

2026-09-05、Emulator (<android-serial> / 1080x2340) で撮影し approved.png と照合した。撮影は 1 周で収束した。画像は verification/ の `maui-android-*.png` (個人要素なし。表示はデモデータのみ)。

- 構造: ルートメニューの項目群と基本 Cell 7 種デモの Section 構成はモックと一致する
- トークン: dark プリセットの 10 色ロールは MAUI iOS と同一定義 (同じ `SampleTheme`) を使う
- 状態: light プリセットは現行のまま変化なし (`maui-android-basic-cells-light.png`)。dark プリセットは `maui-android-basic-cells-dark-1.png` / `-2.png` と `maui-android-input-cells-dark.png`
- 外観の切り替え: 再起動後も「ダーク」を維持 (`maui-android-menu-dark-relaunch.png`)、「システム」選択中に端末を夜間モードにするとルートメニュー表示中でも追随する (`maui-android-menu-system-device-dark.png`)、デモページ表示中の追随は下記「表示中のページの追随」のとおり (`maui-android-basic-cells-system-device-dark.png`)
- 予約日の選択面: 2026/06 表示で 21 日以降が無効、「今日」は選択を変えない。ダーク選択時は選択面もダーク配色 (`maui-android-calendar-dark-range.png`)

合意済み差分 (deviation.md 1 項目目、2026-09-05 オーナー裁定): Theme を渡さない画面のダーク描画。`maui-android-visibility-dark.png` / `maui-android-section-decoration-dark.png` のとおり、他 3 面と同じ症状が出る。

表示中のページの追随 (2026-09-05 追加検証、Emulator (<android-serial> / 1080x2340)):

- 実測した症状: 「システム」選択中に基本 Cell 7 種デモを表示したまま端末を夜間モードにすると、Activity が再生成されてルートメニューへ戻り、ページの状態が失われた。さらに再生成だけを経た状態では MAUI の `Application.RequestedTheme` がライトのまま据え置かれるため、その後デモページを開き直しても light プリセットで描かれた (SettingsView の配色が実効外観に追随しない)
- 機構: MAUI がテーマ状態を更新するのは Activity が uiMode の変更を受け取ったときだけで、受け取らずに再生成させると更新されない。一方、受け取るだけにすると解決済みのリソースを持つ既存 View が残り、ページ下地と既定色で描く文字 (ルートメニューの項目名など) がライトのまま残る
- 直した形: `MainActivity` が uiMode を受け取って MAUI のテーマ更新と `RequestedThemeChanged` を通したうえで、自分で Activity を作り直す。`App` は同じウィンドウを作り直しの前後で使い回すため、表示中のページと入力状態は作り直しをまたいで保たれる
- 結果 (`maui-android-basic-cells-system-device-dark.png`): 同じ「基本 Cell 7 種デモ」に留まったまま dark プリセットへ切り替わり、行タップの状態 (「最後にタップ: Tanaka Taro」) も保たれる。入力 Cell 5 種デモでも、EntryCell に入れた文字と「最後のイベント」の表示が保たれたまま dark へ切り替わることを確認した。Activity 自体は作り直される (ページ下地と既定色の文字を新しい外観で描き直すため) が、ナビゲーションと入力状態は失われない
- 非回帰: ルートメニューのライト / 「ダーク」選択時 / 「ダーク」のまま再起動 / 基本 Cell 7 種デモの dark は、承認済みの `maui-android-menu-light.png` / `maui-android-menu-dark.png` / `maui-android-menu-dark-relaunch.png` / `maui-android-basic-cells-dark-1.png` と画素単位で一致する (ステータスバーの時刻表示部を除いて比較)

外観の複数回往復 (2026-09-05 追加検証、Emulator (<android-serial> / 1080x2340)):

- 手順: 「システム」選択中に入力 Cell 5 種デモを開き、EntryCell「名前」を編集して入力状態 (編集後の値と「最後のイベント」の表示) を持たせたうえで、端末の夜間モードを light → dark → light → dark → light → dark と 5 回切り替えた (3 往復)。切り替えのたびに前面が本アプリであることを確認した
- 結果 (`maui-android-input-cells-system-device-toggle-x3.png` は最終の dark 状態): 5 回とも同じ「入力 Cell 5 種デモ」に留まり、dark / light プリセットが毎回入れ替わり、入力状態は最後まで保たれた。アプリのプロセスは一連の切り替えを通して同一で、例外・クラッシュも記録されていない
- 機構: `App` はウィンドウを 1 つだけ作り、Activity の作り直しのたびに同じものを返す。MAUI の Window は載っているページの Handler 変更イベントを購読し、その解除はページが差し替わったときにしか行わないため、作り直しのたびに新しい Window へ同じページを載せ直すと、破棄済みの Window が購読を持ったまま積み上がる
- 非回帰: 端末が夜間モードでない状態での「ダーク」の明示選択 (chrome とルートメニューがダーク、「ダーク」に選択印) と、その状態での強制終了後の再起動時の維持を再確認した (`maui-android-menu-dark.png` / `maui-android-menu-dark-relaunch.png` と同じ表示)

## トークン候補

- dark プリセットの 10 色ロール (上記 RGBA)。concepts にダーク用トークンはまだ無く、3 面の `SampleTheme` に直接置いている (iOS は `SampleTheme.mauiDark*`、MAUI は `SampleTheme.MauiDark*` の定数群として同じ値を持つ)

## 合意済み妥協

- プラットフォーム制約による見た目の妥協はなし
- 承認モックの規範範囲の限定 (2026-09-05 オーナー裁定、deviation.md 3 項目目): approved.png の規範は配色 (色ロール対応表) とルートメニューの外観 UI に限る。基本 Cell 7 種デモの行構成・文言 (モックの「無効なボタン」行、ButtonCell「登録」) は現行の 3 面 (行なし、「ログアウト」) を正とし、モックとの差分は合意済み差分

## 未合意の乖離 (判断待ち)

- なし (MAUI iOS のシート地色は iOS 本体の修正で解消、Theme を渡さない画面は deviation.md で合意済み)

## モックとの既知の差分 (合意済み、上の「合意済み妥協」参照)

- approved.png の基本 Cell 7 種デモには「無効なボタン」の行と ButtonCell「登録」が描かれているが、この行は現行の 3 面 (iOS / Android / MAUI) のいずれにも存在しない (モック作成時の創作。デモの構成変更は本 change のスコープ外で、3 面は現行構成のまま一致している)。4 実行面の照合はこの行を除いて判定した (2026-09-05)
