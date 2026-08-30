---
type: concept
title: DatePickerCell の選択面
description: DatePickerCell の行タップで開く日付選択 UI のプラットフォーム共通契約 (確定と破棄・min/max・todayText) と、uiStyle ごとの器の違い・Android 固有の配色/ホイール/回転復元契約
tags: [cells, date-picker, selection-surface, styling]
timestamp: 2026-08-28
---

# DatePickerCell の選択面

この文書は、`DatePickerCell` の行タップで開く日付選択 UI (以下「選択面」) が iOS / Android で共通に守る挙動契約と、`uiStyle` ごとに異なる器 (提示コンテナ) の構成を説明する。想定読者はライブラリの実装者と、挙動契約を知りたい利用者の両方である。読むと、確定と破棄の意味論、`minDate` / `maxDate` の効き方、「今日」ジャンプ (`todayText`) の適用面、Android のカレンダーダイアログ配色・Spinner 3連ホイール・回転をまたぐ表示継続の契約が分かる。`DatePickerCell` のモデル — `date` / `minDate` / `maxDate` / `format`、選択面のタイトル `pickerTitle`、確定通知の `onValueChanged`、強調色 `accentColor`、Android の `Spinner` 選択面ヘッダー固有の `androidButtonColor` — は [入力 Cell](input-cells.md)、スタイル解決の一般規則は [スタイルの所有と実効値解決](../styling/style-resolution.md) を先に読むと分かりやすい。数値選択の選択面は別契約 — [NumberPickerCell の選択面](number-picker-selection-surface.md) を参照。

## 目的

選択面は「日付を選び、確定して初めて反映される」体験を両プラットフォームで揃えつつ、器そのものは `uiStyle` と各 OS の慣習に合わせるための境界である。挙動契約 (確定と破棄・範囲制限・callback のタイミング) は全形式で同一に保ち、器と操作語彙はプラットフォーム差・形式差として明示的に管理する。

## 提示の器 (uiStyle × プラットフォーム)

`DatePickerUIStyle` の case はプラットフォームごとに異なり、同一視してはならない ([入力 Cell](input-cells.md)):

| プラットフォーム × uiStyle | 器 | 実装の入口 |
|---|---|---|
| iOS `.wheels` | 埋め込み `UIDatePicker` を `inputView` 経由でキーボード位置にスライドアップ表示。ツールバー (Cancel / タイトル / Today / Done) 付き | `DatePickerCellView` + `EmbeddedPickerToolbar` |
| iOS `.calendar` | カレンダー形式のシート | `DatePickerCalendarSheetController` |
| Android `Material` | `ComponentDialog` に ComposeView を載せ、Compose Material3 `DatePicker` (カレンダー表示とテキスト入力の2モード、相互切替可) + 操作行 (キャンセル / (todayText 指定時) 今日 / 確定) で構成するダイアログ ([android/ADR-0019](../../../decisions/android/0019-datepickercell-calendar-compose-datepicker.md))。ホスト Activity の型 (`ComponentActivity` を含む) と XML テーマに前提はない | `DatePickerCellViewHolder` + `DateCalendarDialog` |
| Android `Spinner` | ボトムシート + 年/月/日の3連スナップ式ホイール ([android/ADR-0009](../../../decisions/android/0009-datepicker-spinner-bottom-sheet-triple-wheel.md))。器の意匠 (ドラッグハンドル + キャンセル/タイトル/確定ヘッダー) は `NumberSelectionSheet` と同系 | `DateSelectionSheet` + `KsWheelView` 3連 |

Android のホイール `KsWheelView` は internal の内部部品であり、公開契約は Cell model と選択面の挙動だけである ([NumberPickerCell の選択面](number-picker-selection-surface.md) と共有)。

## 共通の挙動契約

- 提示: `isEnabled` な DatePickerCell の行タップで開く。`isEnabled = false` はタップ無効
- タイトル: `pickerTitle` があればそれ、なければ `title` で解決する
- 範囲制限: `minDate` / `maxDate` が選択可能な日付の範囲を定める
- 範囲外の初期値 (Android): 開いた時点の `date` が範囲外なら、最も近い範囲端へ丸めて提示する (`Material` / `Spinner` 共通)。`minDate > maxDate` のような提示できない範囲指定では選択面を提示せず警告ログを残す (両形式共通。`Spinner` はさらに空範囲・過大範囲も防御する — 後述)
- 確定のみ反映: 確定操作 (Android の OK / iOS の Done) で、その時点の選択日から作った値を引数に `onValueChanged` を1回発火して閉じる。非確定の閉じ方 (キャンセル・外側タップ・Back・下スワイプ等、器が提供するすべての経路) では発火せず、変更は破棄される
- iOS の確定値は選んだ年月日に**元の `cell.date` の時刻成分を保持**して合成する (`Date` 型のため)。Android は `LocalDate` をそのまま渡す

## 今日へのジャンプ (todayText)

`DatePickerCell.todayText: String?` (既定 `null` / `nil`) は「今日」ジャンプ操作のオプトインで、iOS / Android 同名・同意味論:

- 非 null かつ非空文字のときだけ操作を提示する
- 実行すると選択中がデバイスの現在日付 (端末タイムゾーンの今日) へ移動する。**この操作自体は `onValueChanged` を発火しない** — 反映は確定操作のみ
- 今日が `minDate`..`maxDate` の範囲外なら、実行しても選択状態を変更しない (安全弁。範囲比較は日単位で、min/max 当日は有効)
- 適用される形式: **全形式** — iOS は `.wheels` (ツールバー) と `.calendar` (シート内ボタン)、Android は `Spinner` (ホイール下の chip) と `Material` (ダイアログ下部の操作行のボタン)

Android `Material` 固有の契約 (カレンダーは「表示月」を持つため):

- 実行すると選択日に加えて**表示月も今日へ移動**し、ヘッダの選択日表示・確定操作の有効状態も追随する
- 年選択グリッドやテキスト入力の表示中でも成立する — 実行するとカレンダー (日グリッド) の表示に戻って今日へ移動する
- 操作の連続実行は1回と同じ (冪等)
- 実現は picker state (選択日と表示月) への状態操作で、View 走査もリフレクションも使わない。ジャンプ自体は `onValueChanged` を発火しない ([android/ADR-0019](../../../decisions/android/0019-datepickercell-calendar-compose-datepicker.md))

## Android カレンダーダイアログの配色

Android の `Material` 形式は、4つの色ロールを Compose M3 `DatePicker` の公開の配色 API (`DatePickerColors`) と操作行へ写像して描画する ([android/ADR-0019](../../../decisions/android/0019-datepickercell-calendar-compose-datepicker.md))。カレンダー表示・テキスト入力の両モードが対象で、色ロールは4つ:

- 背景 = `Theme.backgroundColor` — ダイアログ surface (カレンダー・年選択・テキスト入力の各面)
- 強調 = `DatePickerCell.accentColor` → `CellStyle.accentColor` → `Theme.cellAccentColor` の3段解決 — 選択日の塗り・今日の枠・年選択の選択状態・入力欄の枠とキャレット・操作行 (キャンセル / 今日 / 確定) の文字
- 通常文字 = 実効タイトル文字色 (`CellStyle.titleColor` → `Theme.cellTitleColor` → プラットフォーム既定) — ヘッダ・曜日・日付数字・年月表示・入力ラベル。範囲外 disabled は同色のアルファ減
- アクセント上の文字 = アクセント色は不透明とは限らないため、背景色に重ねた後の実際の色を基準に、黒と白のうちコントラスト比が高い方を自動選択する — 選択日の数字・年選択の選択中文字

配色対応表に現れない細部 (文字選択ハンドル等) も、土台の Compose `ColorScheme` 自体を色ロールから導出することで色ロールの近傍に寄せる — Compose M3 既定色 (紫系) がダイアログ内で浮かない。既定に任せた部位もホスト非依存 (同梱テーマ経由で解決) であり、ホストテーマからの視覚隔離 ([android/ADR-0020](../../../decisions/android/0020-bundled-theme-always-wrap-host-independent.md)) を破らない。

iOS の `accentColor` は埋め込み picker の `tintColor` と入力ツールバーへ適用され、ダイアログ全体の配色契約は Android 固有である ([入力 Cell](input-cells.md))。

## Android カレンダーダイアログの回転復元

カレンダーダイアログは、構成変更 (回転等) をまたいで次を保証する ([android/ADR-0021](../../../decisions/android/0021-calendar-dialog-restore-via-view-instance-state.md)):

- **構成変更を in-place で処理するホスト** (Activity 再生成が起きない構成。例: 本ライブラリの .NET MAUI facade が載るテンプレート既定のホスト): ダイアログは開いたまま生存し、選択状態を維持する
- **Activity 再生成が起きるホスト**: `KsSettingsView` (設定画面を表示する Android の Host View — [Android Native Host](../../android/api/android-native-host.md)) の View インスタンス状態に保存した状態 (対象 `cell.id`・選択日・表示月・表示モード) から再提示する。再提示は、保存時と同一 `cell.id` の DatePickerCell (uiStyle `Material`) が復元後の root に存在する場合に限る。不成立なら再提示せず、いかなる Cell へも値を書き込まない
- **成立条件**: View 階層の状態保存は ID を持つ View にしか働かないため、`KsSettingsView` は ID 未設定のときライブラリ既定 ID を自前付与する (ホストの明示 ID は尊重)。ライブラリ既定 ID のインスタンスが同一階層に複数ある構成では保存先が衝突するため復元しない — ホストが個別 ID を与えれば成立する。再生成の前後で `cell.id` が一致すること (Cell への明示 id 指定、または Compose 宣言 DSL が識別のために導出する安定 ID — [Android Compose Bridge](../../android/api/android-compose.md)) も対応付けの前提である
- 復元後の選択面でも配色・今日ジャンプ・確定/破棄の契約はすべて有効である
- バックグラウンド遷移 (Home キー・他アプリ起動) では閉じない。閉じるのはホスト破棄への追随と detach の経路である
- Android のシート系選択面 (`Spinner` の3連ホイールや [TimePickerCell の選択面](time-picker-selection-surface.md) 等のボトムシート) は復元対象外で、回転で閉じる (無発火)

## Android Spinner の選択面 (3連ホイール)

年・月・日の各ホイール (以下「系列」) からなる、`Material` にない Spinner 固有の契約:

- 年候補: `minDate` の年 (未指定 1900) から `maxDate` の年 (未指定 2100) まで。境界年では月候補・日候補も範囲内に収まる選択肢だけに制限される
- 初期選択: 開いた時点の `date`。範囲外なら最も近い範囲端へ丸めて提示する
- 日候補の追随: 年・月の変更で日候補件数が実日数 (閏年含む) に追随し、末日超過は末日へ、範囲外は範囲内最近傍へ丸める (1/31 → 2月 → 2/28。iOS `UIDatePicker` の標準挙動と揃えた形)
- 不正・過大な範囲への防御: `minDate > maxDate`、既定値 (1900 / 2100) を当てた結果として範囲が空になる構成 (例: `maxDate` のみ 1850 年を指定すると既定の下限 1900 年と逆転する)、年候補件数が提示上限 1,000,000 件を超える指定 (件数は桁あふれを避けるため 64bit 整数で算出する) では、選択面を提示せず警告ログを残す
- 候補表示: 端末 Locale の日付表記慣行から導出する (自前の翻訳文字列は同梱しない。日本語なら「2026年 / 8月 / 2日」)。系列の並びは Locale によらず年→月→日で固定
- 操作ラベル: OS の公開文字列リソース (`android.R.string.ok` / `android.R.string.cancel`) — [NumberPickerCell の選択面](number-picker-selection-surface.md) と同じ方針
- スナップ静止 (ホイールが候補位置で止まって初めてその候補が選択中になること) の意味論・候補領域の下スワイプが dismiss にならないこと・アクセシビリティ (系列ごとの選択中公開と前後候補アクション) も NumberPicker の選択面と同じ契約
- ヘッダーの確定・キャンセル操作の色は `DatePickerCell.androidButtonColor` が指定されていればそれを最優先し、未指定なら選択中候補の強調と同じ accent の3段解決 (`DatePickerCell.accentColor` → `CellStyle.accentColor` → `Theme.cellAccentColor`) に従う。`androidButtonColor` はこの `Spinner` ヘッダー専用で、`Material` のカレンダーダイアログには適用されない (操作行は常に accent の3段解決)

## 保証すること

- 確定 callback は確定操作の1回だけ発火し、非確定 dismiss はどの経路でも発火しない — これが崩れると、利用者アプリの状態が「開いて閉じただけ」で書き換わる
- 選択面が提示する日付は常に `minDate`..`maxDate` の範囲内にある (初期表示・年月変更後・今日ジャンプ後のいずれでも) — 範囲外の値が確定されると利用者側のバリデーションを素通りする
- `todayText` の提示条件 (非 null かつ非空文字) と範囲外セーフガードは iOS / Android で同一である

## してはいけないこと

- `DatePickerUIStyle` の case (iOS `.wheels` / `.calendar`、Android `Material` / `Spinner`) を同一と仮定してはならない — case の名前は platform 間で一対一に対応しない (ホイール型は iOS `.wheels` ⇄ Android `Spinner` と名前がずれ、`Material` は器の見た目を表す名前ですらない)
- `KsWheelView` や `DateSelectionSheet`・`DateCalendarDialog` を公開 API として利用者に案内しない — internal の内部部品である
- Compose のバージョンを上げたまま Material 形式のカレンダーを無検証で信頼しない — Compose Material3 `DatePicker` は experimental API であり (android/ADR-0019 の負の帰結)、版更新時はシグネチャと描画の追随確認が必要である (版整合の規律は [Android のビルドツールチェーン](../../android/architecture/build-toolchain.md))

## 用語

- **選択面**: 入力 Cell の行タップで開くモーダルな選択 UI ([PickerCell の選択面](picker-selection-surface.md) と共通の語)
- **器**: 選択面を提示するコンテナ。挙動契約と切り離してプラットフォーム差・形式差を管理する単位
- **系列**: Android Spinner の3連ホイールを構成する年・月・日の各ホイール
- **色ロール**: ダイアログ内の部位を役割 (背景・強調・通常文字・アクセント上文字) で束ね、テーマ色を割り当てる単位

## 関連

- [入力 Cell](input-cells.md) — `DatePickerCell` のモデルとプラットフォーム差の一覧
- [NumberPickerCell の選択面](number-picker-selection-surface.md) — ボトムシート + ホイールの器・スナップ静止・アクセシビリティ契約の共有元
- [TimePickerCell の選択面](time-picker-selection-surface.md) — 同系の時刻選択シート
- [スタイルの所有と実効値解決](../styling/style-resolution.md) — 実効値解決の一般規則
- [Android Native Host](../../android/api/android-native-host.md) — Host の利用契約と回転復元の成立条件
- [android/ADR-0009](../../../decisions/android/0009-datepicker-spinner-bottom-sheet-triple-wheel.md) — Spinner の器をボトムシート + 3連ホイールにした決定
- [android/ADR-0019](../../../decisions/android/0019-datepickercell-calendar-compose-datepicker.md) — カレンダー型を Compose Material3 DatePicker のダイアログ表示に統一した決定
- [android/ADR-0021](../../../decisions/android/0021-calendar-dialog-restore-via-view-instance-state.md) — 回転復元を View インスタンス状態で自前化した決定
