# UI Brief: fix-default-colors-dark-appearance

## 画面と状態

- **Theme を渡さない設定 list** (状態: ライト / ダーク): 3 platform の Native Host と MAUI facade が、Theme 未指定 (または一部だけ上書き) のときに描く既定の見た目。ライトは現行のまま変えない。ダークはライブラリ所有の dark セットで描く。見た目の正は承認モックの「ダーク (新規)」パネルと色ロール対応表
- **Classic / Modern の両 style** (状態: ライト / ダーク): 既定色は style に依らず同じセットから解決する。Modern の箱は Cell 背景と list 下地の対比で見える (箱の枠線の既定は透明のまま)
- **選択面 (PickerCell のシート / DatePickerCell のカレンダー)** (状態: ライト / ダーク): Android は開いた時点の解決済み Theme の値で地色・罫線・文字を描く。表示中の外観切替では描き直さず、閉じて開き直したときから新しい外観 (表示中の外観切替の対象外)。iOS は提示元の外観を引き継ぐ既存契約のまま
- **表示中の外観切替** (Android で Activity が再生成されない構成 / iOS の trait 変更): 未指定の色だけが新しい外観のセットへ描き直される。明示指定した色は変わらない
- loading / empty / error 状態なし (静的な描画契約のため)

## リファレンス注釈

- `references/ios-visibility-dark.png` / `references/ios-section-decoration-dark.png` / `references/android-visibility-dark.png` / `references/android-section-decoration-dark.png` / `references/maui-ios-visibility-dark.png` / `references/maui-android-visibility-dark.png`: 起票元 change (add-sample-dark-mode-toggle) の照合で撮った**現状の症状** (白地に淡色文字)。採用する要素はなし。本 change 後の照合で「同じ画面が判読できる」ことを比べる基準として使う
- 参考にする OS 側の見た目: iOS の設定アプリのダーク配色 (canvas 黒・Cell 濃灰・separator・二次文字の灰色)。画像は持たず、モックの dark 値の出所として色ロール対応表に記す

## デザイントークン参照

- Theme の色ロール (backgroundColor / cellBackgroundColor / separatorColor / selectedColor / cellAccentColor / disabledTextColor / headerTextColor / footerTextColor / headerBackgroundColor / footerBackgroundColor / cellTitleColor / cellDescriptionColor) の意味と解決順は [スタイルの所有と実効値解決](../../../concepts/core/styling/style-resolution.md)
- ライブラリ既定色を中立に保つ方針 (Sample の AiForms 互換色は製品契約ではない) は同文書「Sample の AiForms 互換色」
- concepts/ に dark 用のトークン定義はまだ無い。dark セットの生値は承認モックの色ロール対応表が持ち、蒸留で concepts へ昇格させる (exploration.md 論点 8)
- 3 platform の既定値の platform 差の許容範囲は [Sample のプラットフォーム間一致](../../../handbook/cross/sample-parity.md) の「本体既定値の platform 差」

## 承認モック

mock/plan-a.html を採用 (approved.png)。2026-09-05 オーナー承認。案A = iOS の dark パレットに忠実 (canvas 黒 / Cell 背景 濃灰 / Header・Footer 背景は canvas と同色 / accent は dark systemBlue)。却下: 案B (ライトの色同士の関係を保つ: canvas = Cell 背景、accent はライトと同値) — OS 標準の設定画面との整合と Modern の箱の視認性で案A を優先。dark セットの生値の正は approved.png 内 (plan-a.html 下部) の色ロール対応表。approved.png はモックの描画のみで個人要素は含まない (確認済み)。

候補だったもの: `mock/plan-a.html` (iOS の dark パレットに忠実: canvas と Cell 背景を分け、accent は dark systemBlue) / `mock/plan-b.html` (ライトの色同士の関係を保つ: canvas = Cell 背景、Header / Footer 背景だけ僅かに明るい、accent はライトと同値)。両案とも light 側は現行値で同一。

## 現行との照合 (lessons spec-review L-003)

- light 列の各値は現行ソースと照合済み (2026-09-05): iOS `Theme.swift` の `defaultBackgroundColor` / `defaultSeparatorColor` / `defaultSelectedColor` / `defaultAccentColor` / `defaultHeaderBackgroundColor` / `defaultHeaderTextColor` / `defaultFooterTextColor` / `defaultDisabledTextColor` と `init` の `cellBackgroundColor` 既定、Android `Theme.kt` の `DEFAULT_SEPARATOR_COLOR` / `DEFAULT_SELECTED_COLOR` / `DEFAULT_ACCENT_COLOR` / `DEFAULT_HEADER_BACKGROUND_COLOR` / `DEFAULT_HEADER_TEXT_COLOR` / `DEFAULT_FOOTER_TEXT_COLOR` / `DEFAULT_BACKGROUND_COLOR` / `DEFAULT_DISABLED_TEXT_COLOR` / `DEFAULT_CELL_TITLE_COLOR` / `DEFAULT_CELL_DESCRIPTION_COLOR` / `DEFAULT_BUTTON_TITLE_COLOR` と `cellBackgroundColor` 既定。approved.png の light 列はこれらの現行値と一致する (iOS の Header / Footer 文字は Android と僅差の現状の platform 差のまま。モックの表に注記)
- モックの行構成・文言は説明用の架空データで、Sample の画面を写したものではない (規範は配色のみ。行構成は非規範)

## 照合記録

### 最終状態の一覧 (2026-09-05 時点)

この節より下は初回照合 → 再照合 → 再実証の順に追記してある。**最終的な判定はこの表が正**で、下の各節に残る初回時点の記述は経緯として読む。

| 実行面 | ダークの既定色 (approved.png dark 対応表) | ライトの非回帰 (A/B) | 表示中の外観追随 (同一 View) | 選択面を閉じて開き直す |
|---|---|---|---|---|
| iOS Native | 一致 (Cell description / ButtonCell title は `.secondaryLabel` / `.systemBlue` の合意済み差分) | 画素一致 | trait 変更で描き直し (7.8 / 7.9 のテストで検証) | 提示元の外観を引き継ぐ既存契約のまま (対象外) |
| Android Native | 一致 (ButtonCell title の乖離は再照合で解消) | title / valueText の #1D1B20 → #000000 のみ (承認済みの変化) | **一致** (下記「再実証」) | **一致** (下記「再実証」) |
| MAUI iOS | 一致 (同上の合意済み差分) | 画素一致 | 初回 spike で確認済み (Activity 再生成の概念なし) | iOS の既存契約のまま (対象外) |
| MAUI Android | 一致 | 画素一致 | **一致** (下記「再実証」。初回記述は誤りで訂正済み) | Android Native と同じ本体経路 (下記) |

「表示中の外観追随」と「選択面の開き直し」の証跡は下の「再実証 (2026-09-05)」節にある。それ以外の判定の証跡は「一致した点 (ダーク)」「ライトの非回帰 (A/B)」「再照合 (2026-09-05、tasks 8.2)」の各節。

### 初回照合 (2026-09-05、tasks 6.2 / 8.1〜8.5)

`verification/` の各画像と `mock/approved.png` の色ロール対応表を照合した。判定は画素サンプリングの hex 比較で行い、行構成・文言 (モックの非規範部分) は比較していない。

### 撮影条件

`verification/baseline-light/README.md` と同一 (iPhone 17 Pro Simulator / iOS 26.0、Android Emulator `ksn_custcell_api35` / Android 15、いずれも Debug、ルートメニューから 1 回タップで遷移した直後・スクロールなし)。ダークは Sample のルートメニュー「外観」で「ダーク」を選び、OS 側の設定には触れていない (MAUI Android の表示中切替のみ、外観「システム」で端末の夜間モードを切り替えた)。

MAUI の 2 面は、native 実装後の xcframework / AAR を反映したビルドを入れ直してから撮影した。iOS 側は 1 回目の `dotnet build` で app bundle が再リンクされず旧 native のままだったため、Sample の iOS 向け `obj` / `bin` を捨てて再ビルドし、実行ファイルが新しい native を含むことを確認してから install した。

### 一致した点 (ダーク)

| ロール | 期待 (approved.png dark) | iOS Native | Android Native | MAUI iOS | MAUI Android |
|---|---|---|---|---|---|
| list 下地 | #000000 | 一致 | 一致 | 一致 | 一致 |
| Cell 背景 | #1C1C1E | 一致 | 一致 | 一致 | 一致 |
| separator | #38383A | 一致 | 一致 | 一致 | 一致 |
| Header / Footer 背景 | #000000 | 一致 | 一致 | 一致 | 一致 |
| Header / Footer 文字 | #8E8E93 | 一致 | 一致 | 一致 | 一致 |
| Cell title | #FFFFFF (iOS は `.label`) | 一致 | 一致 | 一致 | 一致 |
| Cell description | #8E8E93 (iOS は `.secondaryLabel`) | `.secondaryLabel` 合成値 | 一致 | `.secondaryLabel` 合成値 | 一致 |
| accent | #0A84FF | 一致 | 一致 | 一致 | 一致 |
| ButtonCell title | #0A84FF (iOS は `.systemBlue`) | `.systemBlue` (合意済み差分) | **不一致** (下記) | `.systemBlue` | 一致 |

Section 装飾デモは Sample のプリセット Theme を渡す画面のため、照合対象はプリセットが指定していない色 (Cell 背景・separator・Header / Footer 文字) に限る。プリセットが指定する下地と accent は対象外として扱った。

`references/` の症状画像 (白地に淡色文字) と同じ画面が、4 実行面とも判読できる状態で描かれていることを確認した。Android の選択面 (PickerCell のシート) とカレンダーの選択面も、開いた時点の解決済み値で暗い地色・罫線・文字が描かれ判読できる。MAUI Android は外観「システム」で画面を表示したまま端末の夜間モードを切り替え、同じ画面・同じスクロール位置のまま dark セットへ描き替わることを確認した (live-before / live-after)。**訂正 (2026-09-05、後続の再実証による)**: この初回照合の時点では Sample の `MainActivity.OnConfigurationChanged()` が `Recreate()` を呼んでおり、live-before / live-after が示すのは「切替後に画面がダークで再構築された」ことまでで、同一 View への追随ではなかった。同一 View での追随は下の「再実証 (2026-09-05)」で別途取り直しており、そちらが正。

### 乖離 (未修正・本体側の判断待ち)

- **Android Native の ButtonCell title 既定が外観セットの値にならない**: title 未指定の ButtonCell が dark で #FFFFFF、light で #000000 (どちらも Cell title の既定値) に描かれ、期待の #0A84FF / #007AFF にならない。ButtonCell の 4 段解決の 3 段目 (`Theme.cellTitleColor`) が、Compose 入口の既定 Theme (`KsSettingsViewDefaults.theme()`) では常に値を持つため最終段へ到達しない。View 経路 (bridge → MAUI) は `Theme` の色が未指定のままなので正しく #0A84FF になり、iOS も `.systemBlue` で正しい。ライトでは本 change 前 (青) からの見た目の後退でもある。証跡は `verification/android-native-unify-common-fields-dark-scrolled.png` (白) と `verification/maui-android-unify-common-fields-dark-scrolled.png` (#0A84FF)

### ライトの非回帰 (A/B)

`verification/baseline-light/` と同条件で撮り直し、ステータスバー (上端 100px / iOS は 160px) を除いて画素比較した。

- iOS Native 5 面・MAUI iOS 3 面: 画素一致 (差分 0、Section 装飾デモのみ最大チャネル差 1 のアンチエイリアス差が数十画素)
- Android Native 5 面・MAUI Android 3 面: レイアウトは画素一致。差分は Cell title / valueText の既定色が #1D1B20 (同梱テーマの `textColorPrimary`) から #000000 (ライブラリの light 既定) へ変わった分と、そのアンチエイリアスのにじみだけで、他の色・位置に差はない。これは「title の既定はホストや同梱テーマの属性から動的に解決しない」という規範どおりの変化であり、回帰ではない。撮り直した画像は `verification/*-light-after.png`

### 個人要素の確認

`verification/` に置いた 30 枚をすべて開いて確認した。写っているのはステータスバーの時刻・Wi-Fi / 電波 / 電池のアイコンと、デモデータ (架空の Wi-Fi 名 `demoAP-0a1b2c-5`、架空の氏名・メール・電話番号) のみで、端末名・アカウント・連絡先・位置情報・通知は写っていない。iOS のステータスバー左上に出る直前アプリ表示は開発用アプリ名であり、A/B はステータスバーを除いて行っている。

### 再照合 (2026-09-05、tasks 8.2)

上の「乖離」に挙げた Android Native の ButtonCell title を本体側で直した後 (裁定 B': `KsSettingsViewDefaults.lightTheme()` / `darkTheme()` が Cell title / description を埋めない)、Android Native と MAUI Android を撮り直して再照合した。**乖離は解消し、他のロールに変化はない。**

撮影条件は上の「撮影条件」と同じ (Android Emulator `ksn_custcell_api35` / Android 15、Debug)。ダークは Sample の「外観」で「ダーク」、ライト A/B は「外観」を「システム」に戻してアプリを起動し直した状態 (端末の夜間モード off) で撮った。

#### ButtonCell title (乖離の解消)

| 外観 | 期待 | 実測 | 判定 |
|---|---|---|---|
| dark | #0A84FF | #0A84FF (Cell 背景 #1C1C1E 上) | 一致 |
| light | #007AFF (本 change 前と同じ) | #007AFF (Cell 背景 #FFFFFF 上) | 一致 |

「共通フィールド統合デモ」を末尾までスクロールした状態の `verification/android-native-unify-common-fields-dark-scrolled.png` / `verification/android-native-unify-common-fields-light-after-scrolled.png` の「ログアウト」(title を指定しない ButtonCell) の文字を画素サンプリングして測った。同じ画面の「登録」(title を明示指定した ButtonCell) と「送信」(valueText) は指定どおりのままで変わっていない。

#### 他の色ロール (ダーク・Android Native)

「共通フィールド統合デモ」の画素サンプリングで、期待値どおりであることを再確認した。

| ロール | 期待 | 実測 |
|---|---|---|
| list 下地 / Header・Footer 背景 | #000000 | #000000 |
| Cell 背景 | #1C1C1E | #1C1C1E |
| separator | #38383A | #38383A |
| Header / Footer 文字 | #8E8E93 | #8E8E93 |
| Cell title | #FFFFFF | #FFFFFF |
| Cell description | #8E8E93 | #8E8E93 |
| accent | #0A84FF | #0A84FF |

5 面の撮り直し画像を前回の同名画像と画素比較したところ、差はナビゲーションバー帯 (画面下端 66px、OS のシステムバー) の地色だけで、Cell 領域は画素一致だった。Android の選択面 (PickerCell のシート) とカレンダーの選択面も撮り直し、暗い地色・罫線・文字で判読できることを再確認した (カレンダーは同じくナビゲーションバー帯以外が画素一致)。

#### ライトの非回帰 (A/B) の再実施

`verification/baseline-light/` の Android Native 5 面と、ステータスバー (上端 100px) を除いて画素比較した。

- 差分は前回同様、Cell title / valueText の #1D1B20 → #000000 とそのアンチエイリアスのにじみだけ (最大チャネル差 32 = 0x1D)。位置・レイアウト・他の色に差はない。これはオーナー承認済みの利用者可視の変化 (deviation.md「ライトの既定は本 change 前と同じ」)
- 差分画素数: Store 11,178 / DSL 19,860 / 共通フィールド統合 19,924 / isVisible 46,239 / Section 装飾 24,235
- ライトの A/B は**アプリを起動し直してから**撮る必要がある。表示中に「外観」を切り替えただけだと、Sample 自身のシステムバー地色が切り替え前の値のまま残り (#FEF7FF → #FAFAFA)、ライブラリと無関係な差分が出る。ステータスバー / ナビゲーションバー帯を除けば結果は同じ

#### MAUI Android の非影響確認

native の AAR を反映して MAUI Sample を組み直し、「共通フィールド統合デモ」をダークで撮り直した。前回の同名画像とステータスバーを除いて**画素一致 (差分 0)**。ButtonCell の「ログアウト」も #0A84FF のままで、View 経路 (bridge → MAUI) は本修正の影響を受けていない。

#### 撮り直したファイル

`verification/` 配下 (いずれも旧ファイルを `trash` して置き換え):

- `android-native-store-dark.png` / `android-native-dsl-dark.png` / `android-native-unify-common-fields-dark.png` / `android-native-visibility-dark.png` / `android-native-section-decoration-dark.png`
- `android-native-unify-common-fields-dark-scrolled.png` (**この画像は乖離の証跡から修正後の状態に入れ替わった**。上の「乖離」節が指す「白い ログアウト」はこの画像には写っていない)
- `android-native-picker-sheet-dark.png` / `android-native-datepicker-calendar-dark.png`
- `android-native-store-light-after.png` / `android-native-dsl-light-after.png` / `android-native-unify-common-fields-light-after.png` / `android-native-visibility-light-after.png` / `android-native-section-decoration-light-after.png`
- `maui-android-unify-common-fields-dark.png`

新規追加: `android-native-unify-common-fields-light-after-scrolled.png` (ライトの ButtonCell が #007AFF に戻っていることの証跡)

iOS Native / MAUI iOS / MAUI Android の他の面は本修正の影響範囲外 (bridge は Theme を Unspecified で運ぶ、iOS は無変更) のため撮り直していない。

#### 個人要素の確認

撮り直した 15 枚をすべて開いて確認した。写っているのはステータスバーの時刻・Wi-Fi / 電波 / 電池のアイコンと、デモデータ (架空の氏名 `Tanaka Taro` / `佐藤 花子`、架空のメール `tanaka.taro@example.com`、架空の電話番号 `090-0000-0000`) のみで、端末名・アカウント・連絡先・位置情報・通知は写っていない。

## 再実証 (2026-09-05)

独立レビュー (`../second-opinion-code-001.md` の [Major] 2 件) を受けて取り直した 2 件。撮影条件は上の「撮影条件」と同じ (Android Emulator `ksn_custcell_api35` / Android 15、Debug)。

### 1. MAUI Android の「表示中の追随」(同一 View であることの実証)

初回照合の記述は、Sample の `MainActivity.OnConfigurationChanged()` が `Recreate()` を呼ぶ構成のままで撮ったため「同一 View への追随」を証明していなかった。`Recreate()` を一時的にコメントアウトしたビルドで撮り直し、**同一 Activity・同一 View のまま追随することを確認した**。手順と識別子の突き合わせは `../tasks.md` 0.1 の「0.1 の再実施」節が正で、証跡は `../evidence/maui-android-norecreate-*` の 4 枚と `../evidence/maui-android-norecreate-instance-identity.txt`。一時改変は戻し、改変前のビルドを Emulator に入れ直した。

| 面 | ロール | ライト (切替前) | ダーク (切替後・同一 View) |
|---|---|---|---|
| 共通フィールド統合デモ (色未指定) | list 下地 | #FFFFFF | #000000 |
| 同上 | Cell 背景 | #FFFFFF | #1C1C1E |
| isVisible デモ (`AppThemeBinding` 一時付与) | Cell 背景 (明示) | #FFD9E6 | #102A54 |
| 同上 | Section Header 文字 (明示) | #C2185B | #7FD4FF |
| 同上 | list 下地 (未指定) | #FFFFFF | #000000 |

### 2. Android Native の選択面を閉じて開き直す

`KsThemedContext` の修正 (外観が変わったら同梱テーマ付き Context を作り直す) が実経路で効くことの証跡。**期待**: 外観変更後に選択面を開き直すと、明示色だけでなくテーマ属性由来の要素 (シートのドラッグハンドル・ダイアログの地色と罫線・ボタン文字) も新しい外観になる。**実測**: 期待どおり。

手順 (View が生き残る経路を作るための一時改変を含む):

1. Android Native Sample の `app/src/main/AndroidManifest.xml` の `MainActivity` に `android:configChanges="uiMode"` を**一時的に**付ける。Sample の「外観」切替も端末の夜間モード変更も、この宣言が無いと Activity が再生成され、選択面の Context が作り直されてしまうため、開き直しの経路そのものを踏めない
2. 外観「システム」・端末はライトの状態で「入力 Cell 5 種デモ」を開き、PickerCell (単一選択) のシートを開いて Cancel で閉じる
3. `adb shell cmd uimode night yes` で端末を夜間へ。list は同一 View のまま dark へ描き替わる (`KsSettingsView{a54b071}` が切替の前後で一致)
4. 同じ行をタップしてシートを開き直す。DatePickerCell (カレンダー) も 2〜4 と同じ手順で確認する
5. 一時改変を戻し、改変前のビルドを 3 台に入れ直して端末の夜間モードを off に戻す

| 選択面 | ライトで開いたとき | 外観変更後に開き直したとき |
|---|---|---|
| PickerCell のシート | 白い地色・灰のドラッグハンドル・黒い題字 | 暗い地色・明るいドラッグハンドル・白い題字。Cancel / チェックの accent は指定どおり |
| DatePickerCell のカレンダー | 明るい surface・濃い日付文字・薄い罫線 | 暗い surface・明るい日付文字・暗い側の罫線。選択日のマーカーと今日 / Cancel / OK の accent は指定どおり |

証跡: `verification/android-native-picker-sheet-reopen-light-before.png` / `verification/android-native-picker-sheet-reopen-dark-after.png` / `verification/android-native-datepicker-calendar-reopen-light-before.png` / `verification/android-native-datepicker-calendar-reopen-dark-after.png`。あわせて、開き直しの前提である「同一 View のまま list が追随する」ことの証跡として `verification/android-native-input-cells-live-light-before.png` / `verification/android-native-input-cells-live-dark-after.png` を置いた。

**取っていない証跡**: 修正前ビルド (`KsThemedContext` の早期 return が残った状態) で古い外観のまま出ることの A/B は取っていない。本体を一時的に戻すことになるため、本作業の担当範囲 (本体の変更禁止) から外れる。必要なら別途指示を受けて取る。

### 個人要素の確認

本節で追加した 10 枚 (`evidence/` 4 枚・`verification/` 6 枚) をすべて開いて確認した。写っているのはステータスバーの時刻・Wi-Fi / 電波 / 電池のアイコンと、デモデータ (架空の氏名 `Tanaka Taro`、架空のメール `tanaka.taro@example.com`、架空の電話番号 `090-0000-0000`) のみで、端末名・アカウント・連絡先・位置情報・通知は写っていない。`evidence/maui-android-norecreate-instance-identity.txt` は `dumpsys` 出力から対象アプリの 4 行だけを抜粋したもので、`scripts/log-sanitize.py` を通して置換対象なしを確認した (他アプリ・ランチャの行は採っていない)。

## 再撮影 (2026-09-06、review-002 Major の修正後)

上の「再実証」2. で撮った Android Native の live 切替の証跡を、行の ViewHolder がテーマ属性を bind 時に現在の外観から引き直すようにした後の状態で撮り直した。手順・撮影条件は「再実証」2. の 1〜5 と同じ (Android Native Sample の manifest に `android:configChanges="uiMode"` を一時付与 → Sample の外観「システム」・端末ライトで対象画面を開く → `adb shell cmd uimode night yes` → 同じ View のまま撮影 → 一時改変を戻して入れ直し、端末の夜間モードを off)。撮影は Emulator (`ksn_custcell_api35` / Android 15、Debug) の 1 台のみを対象にした。

**期待の置き方**: テーマ属性由来の値には承認モックの対応表に載る規範値が無い (「現在の外観の同梱テーマが解決する値」が契約)。そこで、**同じ外観で新規に生成した ViewHolder が示す値**を期待値とし、live 切替後の値がそれと一致することを判定に使った。期待値は各面をダークのまま起動し直して同じ位置を採った (Activity 再生成を経るので ViewHolder は新規生成)。

### 1. 入力 Cell 5 種デモ (EntryCell の placeholder)

「ニックネーム (callback)」行の placeholder「callback 経路で更新」(placeholder 色をどの段でも指定していない行) の文字色。

| 状態 | 期待 | 実測 | 判定 |
|---|---|---|---|
| ライト (切替前) | 同梱テーマ light の hint 色 | 文字 #A9A8AA / 地 #FFFFFF | 判読できる |
| ダーク (live 切替後・同一 View) | ダークで新規生成したときの hint 色 = 文字 #9B9599 | 文字 #9B9599 / 地 #2A2620 | 一致・判読できる |

修正前の同じ位置は文字 #252220 / 地 #2A2620 (review-002 の実測) で、切替前のライト値が残っていた。修正後は明るいグレーになり、地とのコントラスト比は約 5.5:1 で読める。同じ画面の「表示名」行 (placeholder 色を Cell 個別に指定) はライト・ダークとも指定色のままで変わらない。

### 2. 共通フィールド統合デモ (SwitchCell のオフ状態と既定色)

Theme を渡さない画面のため、Cell 背景・list 下地・title はライブラリ既定色が出る。SwitchCell「Wi-Fi のみ同期」(オフ) の track / thumb はテーマ属性由来。

| ロール | ライト (切替前) | ダーク (live 切替後・同一 View) | ダークで新規生成したときの値 | 判定 |
|---|---|---|---|---|
| オフ track | #E2E4E7 | #33373C | #33373C | 一致 |
| オフ thumb | #6B6F74 | #83888D | #83888D | 一致 |
| list 下地 | #FFFFFF | #000000 | #000000 | 一致 (承認モック dark 対応表と同値) |
| Cell 背景 | #FFFFFF | #1C1C1E | #1C1C1E | 一致 (同上) |
| Cell title | #000000 | #FFFFFF | #FFFFFF | 一致 (同上) |

ライトとダークで track / thumb の明度が反転しており、切替前の値が残っていない。

### 撮り直した / 追加したファイル

`verification/` 配下:

- `android-native-input-cells-live-light-before.png` / `android-native-input-cells-live-dark-after.png` (旧ファイルを `trash` して置き換え)
- `android-native-unify-common-fields-live-light-before.png` / `android-native-unify-common-fields-live-dark-after.png` (**新規**。SwitchCell のオフ状態と既定色の live 追随には従来 live の証跡が無かったため追加)

期待値の採取に使ったダーク新規生成時の画像は判定の中間物なので残していない (実測値は上の表に記録)。選択面の開き直し (`*-reopen-*`) と、初回照合・再照合で撮った他の面は本修正の影響外のため撮り直していない。

### 個人要素の確認

本節で置き換え・追加した 4 枚をすべて開いて確認した。写っているのはステータスバーの時刻・Wi-Fi / 電波 / 電池のアイコンと、デモデータ (架空の氏名 `Tanaka Taro`、架空のメール `tanaka.taro@example.com`、架空の電話番号 `090-0000-0000`) のみで、端末名・アカウント・連絡先・位置情報・通知は写っていない。

## 付随修正の証跡 (2026-09-06、Sample アイコン tint)

Android Native Sample の単色アイコン (Material Symbols 由来の vector drawable 14 本) の `android:tint` を固定値から外観別のカラーリソースに切り替えた付随修正の A/B 証跡。ダークで暗い Cell 背景の上にアイコンが埋もれていた症状 (前回照合での指摘) の解消を確認する。**ライトは値が変わらないため見た目は変化しない**ことも同時に確認した。

対象は「共通フィールド統合デモ」(Theme を渡さない画面。Cell 背景・list 下地はライブラリ既定色が出る) のアイコン付き 8 行 — 通知 (ベル) / Wi-Fi のみ同期 (Wi-Fi) / 規約に同意 (書類) / ライト (太陽) / ダーク (月) / 自動 (輝度自動) / 通知 1 (メール) / 通知 2 (カレンダー)。

手順: 修正込みでビルドした Debug APK を Emulator (`ksn_custcell_api35` / Android 15) の 1 台だけに入れ、Sample の外観「システム」のまま、端末の夜間モードを `off` → `on` と切り替えて同じ画面を撮影した (Sample は外観変更で画面を作り直すため、通常の切替でよい)。実機 2 台には入れていない。撮影後、端末の夜間モードは `off` に戻した。

| 外観 | アイコン tint の期待 | アイコン tint の実測 (8 行すべて) | Cell 背景 | 判定 |
|---|---|---|---|---|
| ライト | #666666 (修正前と同値) | #666666 | #FFFFFF | 変化なし |
| ダーク | ライブラリ dark 既定の description 文字色と同じ明るいグレー (#8E8E93) | #8E8E93 | #1C1C1E | 一致・判読できる |

実測は保存した画像の各アイコンの外接矩形から最頻色を採ったもので、8 行とも同一の値だった。ダークのアイコン (#8E8E93) と Cell 背景 (#1C1C1E) のコントラスト比は約 5.2:1 で、修正前 (#666666 と #1C1C1E で約 1.8:1) の埋もれは解消している。

証跡: `verification/android-native-unify-common-fields-icon-light.png` / `verification/android-native-unify-common-fields-icon-dark.png`。同じ画面の既存の照合画像 (`android-native-unify-common-fields-*.png`) は既定色そのものの証跡であり、本節の 2 枚はアイコン tint の A/B を見るためのものなので別ファイルとして置いた。

### 個人要素の確認

本節で追加した 2 枚を開いて確認した。写っているのはステータスバーの時刻・Wi-Fi / 電波 / 電池のアイコンと Sample のデモデータのみで、端末名・アカウント・連絡先・位置情報・通知は写っていない。ローカル絶対パスの写り込みも無い。
