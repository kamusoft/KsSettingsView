# Design: relax-android-host-prerequisites

## Context

Android ライブラリの利用前提2つ (Theme.Material3.* XML テーマ / FragmentActivity ホスト) を撤廃する。方式は探索で決定済み: TimePickerCell は時分ホイールのボトムシートへ全ホスト統一 (android/ADR-0018)、DatePickerCell のカレンダー型は Compose Material3 DatePicker のダイアログ表示へ統一 (android/ADR-0019)、テーマは同梱 Material3 派生テーマの常時ラップ (android/ADR-0020)。本書はその実現方式の設計判断を確定する。

現状の要点 (調査済み):

- Material attr への依存は「MaterialSwitch / MaterialCheckBox のコンストラクタ (非 M3 テーマで初期化例外)」「MaterialColors の fallback 付き参照4箇所」「BottomSheetDialog のテーマ解決」に集約。ライブラリ res/ に themes.xml は無い
- Context 注入点は「ViewHolder 生成の `parent.context`」「シート/ダイアログの `views.root.context`」の2系統
- ホストテーマの色を意図的に反映しているのは ButtonCell タイトル色の第4段 (`androidx.appcompat.R.attr.colorPrimary` → 失敗時固定色) **のみ**。Cell accent の既定は `Theme.cellAccentColor` の固定既定値でありホストテーマに追従していない
- `KsSettingsView` に View インスタンス状態の保存・復元は未実装 (回転復元は FragmentManager の saved state に全面依存)
- 12/24h は `TimePickerCell.format` に `a` (大文字小文字問わず) を含むか否かで決定 (端末設定は見ない・未文書化の実装挙動)
- 既存ボトムシート3種は「ドラッグハンドル → 取消/タイトル/確定ヘッダー → 内容」の共通意匠で、確定ボタンのみが反映経路。`KsWheelView` は候補数と表示文字列関数だけを受ける汎用部品で時・分に流用可能

## Goals / Non-Goals

proposal.md の What Changes / Non-Goals に従う。本書は方式の設計判断のみを扱う。

## Decisions

### Decision 1: 同梱テーマは Material3 派生のフルテーマとし、共有ヘルパで常時ラップする

**採用案:** `res/values/themes.xml` に internal テーマ (parent: `Theme.Material3.DayNight.NoActionBar`) を同梱する。internal な Context 変換ヘルパを1つ新設し、ViewHolder 生成 (`parent.context`) とシート/ダイアログ (`views.root.context`) の2系統の入口で常に `ContextThemeWrapper` を適用する。`BottomSheetDialog` のテーマ解決 (`bottomSheetDialogTheme`) は M3 親テーマが提供するため個別定義しない。

ラップの所有境界とキャッシュ寿命 (相方レビュー spec-001 反映):

- **ラップ対象はライブラリ所有の UI だけ**: 標準 Cell の行・chrome・選択面。**利用者所有コンテンツ (CustomCell の content factory・`KsAnyView` 経由の利用者 View) には従来どおりホストの Context を渡す** — 利用者 UI のテーマをライブラリが書き換えない
- **ラップ Context は Activity より長く保持しない**: 保持はラップ元 Context のライフサイクルに従属する範囲のみ (View 階層・シートと同寿命)。プロセス・シングルトンでのキャッシュはしない (Activity leak 防止)。Activity 再生成で自然に作り直され、in-place の `uiMode` 変更 (ホストが configChanges で処理する構成) では次の View 生成時に再導出される

**理由:** attr の完全性を Material3 親テーマに任せることで、ウィジェットが要求する attr の列挙保守が不要になる (ADR-0020 の決定)。DayNight 派生により、M3 attr を明度の土台として使う箇所 (SwitchCell のオフ色導出) がライト/ダークの正しい側の既定値になる。

**代替案:**
- **A: 必要 attr のみの最小 ThemeOverlay** — 列挙の完全性を保守し続ける負担と漏れ=クラッシュ残存。ADR-0020 で却下済み
- **B: Light 固定の親テーマ** — ダークモードで M3 attr の明度土台が反転せず、accent 導出色の明度関係が崩れるため却下

### Decision 2: ホストテーマからの色引き継ぎは行わない (ButtonCell の colorPrimary 追従を廃止)

**採用案:** ライブラリ UI が参照する色は同梱テーマとライブラリの styling API (`Theme` / `CellStyle`) で完結させる。現行で唯一ホストテーマ色を反映していた ButtonCell タイトル色の第4段 (`colorPrimary` の動的解決) は**廃止**し、固定の既定色 (現行のフォールバック値と同一) に統一する。Cell accent の既定は現行どおり `Theme.cellAccentColor` (固定既定値) で変更しない。

**理由:** ホスト依存の色経路を1つだけ残すと「ホストテーマから隔離される」という新契約に例外が生まれ、ラップ前 Context を引き回す実装コストも増える。廃止すれば契約が完全に統一され、実装も安い (オーナー決定 2026-08-27。ADR-0020 に訂正注記あり)。

**代替案:**
- **A: ButtonCell の追従をラップ前 Context 解決で維持** — 利用者可視の変化はゼロだが、隔離契約に例外が残り、ラップ前 Context の引き回しが必要。統一性とコストを優先して却下
- **B: accent 既定をホスト colorPrimary 追従へ拡大** — アイデアとしては有望だが、既定の見た目が変わる挙動変更の追加で本変更の目的外。将来の変更候補として見送り (proposal の Non-Goals に記録)

### Decision 3: 時刻の選択面は既存シート同系の新設シートとし、12/24h は format 文字列で決める

**採用案:** `TimeSelectionSheet` を新設する (器・ヘッダー意匠・確定/破棄の操作規約・スナップ静止・アクセシビリティは NumberPickerCell / DatePickerCell (Spinner) の選択面契約と同一)。系列構成は、24時間制で「時 0–23 / 分 0–59」の2連、12時間制で「時 1–12 / 分 0–59 / 午前・午後」の3連。12/24h の判定は **format 文字列の引用符 (`'`) 外に AM/PM パターン文字 `a` (小文字) を含むか**で行う — `DateTimeFormatter.ofPattern` のパターン文法に整合させる (相方レビュー spec-001 反映: 現行実装の `contains("a", ignoreCase)` は引用リテラル内の `a` や大文字 `A` (milli-of-day) を誤検出するバグであり引き継がない)。午前/午後のラベルは端末 Locale の表記から導出する (自前の翻訳文字列は同梱しない)。初期選択は開いた時点の `cell.time`。ホイールは `KsWheelView` を再利用し、時刻用の系列管理は新クラスとする (DateSelectionSheet の年月日同期ロジックは共用しない)。

**理由:** 既存シート3種と器・操作規約を完全に揃え、利用者の学習コストと実装の分岐を増やさない。判定を DateTimeFormatter のパターン文法に整合させることで、行の valueText 表示 (format 由来) と選択面の時制が一致する。

**代替案:**
- **A: 端末の 24 時間設定 (`DateFormat.is24HourFormat`) で決める** — 行の表示 format と選択面の時制が食い違い得る (format="HH:mm" なのに 12h ホイール等)。現行判定からの挙動変更でもあるため却下
- **B: AM/PM を時ホイールの候補に混ぜて2連に収める** — 「ホイールが候補位置で止まって初めて選択中になる」スナップ静止の意味論で午前/午後切替が時選択と絡み合い、操作が曖昧になるため却下

### Decision 4: カレンダーの器は ComponentDialog + ComposeView とし、操作行は自前で構成する

**採用案:** `androidx.activity.ComponentDialog` に ComposeView を載せ、コンテンツを「Compose Material3 `DatePicker` (カレンダー/テキスト入力の DisplayMode 切替つき) + 操作行 (取消 / (todayText 指定時) 今日 / 確定)」で構成する。`minDate` / `maxDate` は `SelectableDates` + 年範囲で反映。今日ジャンプは picker state の選択日と表示月を今日へ設定する状態操作で実現し (onValueChanged 非発火・範囲外セーフガード・冪等・非カレンダー表示中でもカレンダーへ戻して成立)、確定操作のみが `onValueChanged(LocalDate)` を1回発火する。

状態変換の契約 (相方レビュー spec-001 反映。いずれも既存契約との整合を優先):

- **範囲外の初期値**: 開いた時点の `cell.date` が範囲外なら**最も近い範囲端へ丸めて提示** (Spinner 選択面の既存契約と同一)
- **年範囲**: `minDate` / `maxDate` の未指定側は 1900 / 2100 (Spinner の既定と同一)
- **epoch 変換**: `LocalDate` ⇔ epoch millis は **UTC 基準の日単位往復** (現行 Material 経路の `toEpochMilliUtc` と同一)。端末タイムゾーンを使うのは「今日」の算出だけ — タイムゾーンによって確定日が前後にずれないこと

配色は既存の4色ロール契約を `DatePickerColors` と操作行へ次の対応で写像する (相方レビュー spec-001 反映):

| 色ロール | 適用先 |
|---|---|
| 背景 (`Theme.backgroundColor`) | ダイアログ surface (カレンダー・年選択・テキスト入力の各面) |
| 強調 (accent 3段解決) | 選択日の塗り・今日の枠・年選択の選択状態・入力欄の枠とキャレット・操作行 (確定/取消/今日) の文字 |
| 通常文字 (実効タイトル文字色) | ヘッダ・曜日・日付数字・年月表示・入力ラベル。範囲外 disabled は同色のアルファ減 |
| アクセント上の文字 | 選択日の数字・年選択の選択中文字 (背景合成後の実色に対する黒/白コントラスト自動選択 — 既存契約と同一) |

上記以外の細部は Compose M3 既定に任せる (既定に任せた部位はホスト非依存 — 同梱テーマ経由で解決されるため隔離契約は破らない)。

**理由:** DialogFragment 依存なしで ComponentActivity で成立し (ADR-0019)、配色と今日ジャンプが View 走査ハックなしの正面 API で実現できる。器を自前にするのは、material3 の `DatePickerDialog` composable を使うと ComponentDialog の中にさらに Compose のダイアログ窓が開いて構成が二重化し、今日ボタンの配置自由度も失われるため。

**代替案:**
- **A: material3 `DatePickerDialog` composable をそのまま使う** — ダイアログの二重化と操作行のカスタム制約。却下
- **B: `AppCompatDialog` を器にする** — appcompat 1.7 の ViewTree owner 自動設定に依存できるが、ComponentDialog も同等の owner 供給を自前で持ち依存が軽い。スパイクで問題が出た場合の切替先として保持 (可逆な実装詳細)

### Decision 5: 回転復元は KsSettingsView の View インスタンス状態で自前化する

**採用案:** `KsSettingsView` に `onSaveInstanceState` / `onRestoreInstanceState` を実装し、表示中カレンダーダイアログの状態 (対象 cell.id・選択日・表示月・表示モード) を保存する。復元は attach 後、保存された cell.id と同一 id の DatePickerCell (uiStyle Material) が復元後の root に存在する場合に限り、保存状態で再表示する。不成立なら再表示せず、値も書き込まない。シート系 (Picker / Number / Date Spinner / 新 TimeSelectionSheet) は従来どおり復元対象外。

ホスト形態別の保証と成立条件 (相方レビュー spec-001 反映):

- **構成変更を in-place で処理するホスト** (MAUI テンプレート既定の `ConfigurationChanges` 等): Activity 再生成が起きないため復元機構は関与しない。**ダイアログは開いたまま生存する**のが期待挙動 (検証もこの形で行う)
- **Activity 再生成が起きるホスト**: View 階層の状態保存への参加が成立条件。素の View 直置きではホストが `KsSettingsView` に安定した View id を与えること (Android 標準の View 状態保存の条件)。現行コードは View に id を設定していない (`KsSettingsView.kt` / Bridge / Compose ラッパのいずれも) ため、**ライブラリ所有の統合層 (Compose ラッパ・Bridge) では id の付与または状態中継を実装範囲に含める** — 具体方式はスパイク (tasks 1.2) で確定する
- **スパイクで状態保存が成立しないと判明したホスト形態**: 「回転で閉じる (無発火)」を**正式な縮退契約**とする (シート系と同じ挙動。値の誤書き込みは起こさない)。縮退が確定したホスト形態は deviation ではなく spec 側の注記として明文化する

**理由:** FragmentManager の saved state に代わる標準の View 状態機構で、ADR-0011 が確立した設計原則 (cell.id での対応付け・不成立時は復元しない・別 Cell へ書き込まない) をそのまま踏襲できる。復元後に配色・今日ジャンプが機能しない現行の既知問題 (concepts 記載) も、再表示方式では構造的に解消される。

**代替案:**
- **A: ダイアログ内の rememberSaveable のみに頼る** — ダイアログは Activity と共に破棄され、再表示の主体が存在しないため単独では成立しない。却下
- **B: プロセス内シングルトンで表示状態を保持** — プロセス再生成で消え、View の寿命と絡んで所有関係が曖昧になる。却下

### Decision 6: Fragment 依存機構は非推奨期間を挟まず削除する

**採用案:** `findFragmentManager()`・`PickerRestoreRegistry`・`TimePickerColorizer`・`DatePickerColorizer`・`DatePickerTodayShortcut`・`MaterialDatePickerPresenter`・`PickerDialogTag` を削除する。`androidx.fragment:fragment-ktx` の直接依存 (ui の build.gradle.kts と binding csproj の明示参照) も撤去する (appcompat の推移依存としては残る)。旧機構のテスト (Colorizer / TodayShortcut / DialogRecreation 系) は削除し、新実装のテストで置き換える。

**理由:** すべて internal で公開 API に現れず (公開契約は Cell モデルと選択面挙動 — concepts 明記)、非推奨サイクルを設ける対象がない。

**代替案:**
- **A: @Deprecated で残す** — internal 実装に非推奨期間は無意味で、死蔵コードとテスト資産の維持コストだけが残る。却下

## Risks / Trade-offs

- **Compose Material3 DatePicker は experimental API**: compose 版更新でシグネチャが動き得る。版更新時の追随確認を binding csproj のコメント規律 (Material 1.12 ピンと同型) として明文化する
- **ComposeView-in-ComponentDialog は机上確定のみ**: ViewTree owner の供給と回転時の挙動は一次情報 (リリースノート・API 仕様) からの確定で、実測していない。tasks 冒頭のスパイクで先に潰す (lessons process L-004)
- **View インスタンス状態の成立条件がホスト形態で異なり得る**: View 直置き / Compose `AndroidView` / MAUI の各ホストで saved state 経路が機能するか未確認。スパイクの確認範囲に含める。機能しないホスト形態では「回転で閉じる (無発火)」への縮退とし、値の誤書き込みは起こさない
- **視覚差**: 時計ダイヤル廃止 (ADR-0018 記録済み) とカレンダー細部差 (ADR-0019 記録済み)。mock 承認と実機証跡 (lessons process L-003) で管理
- **テスト資産の入れ替え**: Colorizer 系の大規模テストを廃棄し新規テストで置換するため、移行期の回帰検出力が一時的に下がる。デルタスペックの Scenario を新テストの指標として先に固定する

## Migration Plan

1. スパイク (ComposeView-in-Dialog + View 状態保存の3ホスト形態確認) — 前提が覆ったらエスカレーション
2. テーマ同梱と常時ラップ (全 Cell が非 M3 テーマで動く状態を先に作る)
3. TimeSelectionSheet 置換 (MaterialTimePicker 撤去)
4. Compose カレンダーダイアログ置換 (MaterialDatePicker 撤去)
5. 回転復元の自前化 → Fragment 依存機構の一括削除
6. サンプル復帰 (MAUI SplashTheme / android サンプルの ComponentActivity 化) と binding csproj の整理
7. skills/ への追従は本 change の実装完了後、docs-refresh で別途 (Non-Goal)

## Open Questions

- View 状態保存が Compose `AndroidView` / MAUI ホストで機能するか (スパイクで確定。不成立なら該当ホストは回転で閉じる縮退)

## ADR 候補

- **Decision 5** (回転復元の View インスタンス状態方式): ADR-0011 (superseded) の後継機構で、KsSettingsView の状態保存という能力境界を新設し将来の復元系機能を制約する。実装確定後に distill で起票
- 他の Decision は ADR-0018/0019/0020 (起票済み) の実装詳細、または選択面契約としてコード+テスト+concepts 追随が担うため候補にしない
