# UI Brief: relax-android-host-prerequisites

## 画面と状態

新設・置換される選択面は2つ。いずれもモーダル提示で、loading / empty 状態は持たない (候補は常に確定的に生成される)。

1. **時刻選択シート (TimeSelectionSheet)** — TimePickerCell の行タップで下から提示
   - 状態: 24時間制 (時 0–23 / 分 0–59 の2系列) / 12時間制 (時 1–12 / 分 / 午前・午後の3系列)。`format` の `a` 有無で決まる
   - 構成: ドラッグハンドル → ヘッダー (取消 / タイトル / 確定 pill) → ホイール行。既存の NumberSelectionSheet / DateSelectionSheet と同系の器
2. **カレンダー選択ダイアログ (Compose Material3 DatePicker)** — DatePickerCell (uiStyle Material) の行タップで中央に提示
   - 状態: カレンダー表示 / テキスト入力 (DisplayMode 切替)、todayText 指定時のみ「今日」操作が現れる
   - 構成: ヘッダー (タイトル + 選択日 + モード切替) → カレンダーグリッド → 操作行

## リファレンス注釈

- ui/references/ への持ち込み画像はなし。視覚の正は既存実装の意匠 (シート: ドラッグハンドル 対称ヘッダー・ホイールの選択帯とフェード / ダイアログ: Material3 DatePicker 標準) と本 mock
- 4色ロール (背景・強調・通常文字・アクセント上文字) は既存契約 (concepts/core/cells/date-picker-selection-surface.md) を踏襲

## デザイントークン参照

- 配色はライブラリ Theme の実効値解決に従う (accent 3段解決・cellBackgroundColor・separatorColor 等 — concepts/core/styling/style-resolution.md)。mock は既定 Theme 値で描いている
- ホイールの寸法・帯・フェード・スナップ静止は既存 KsWheelView の意匠と同一 (concepts/core/cells/number-picker-selection-surface.md)

## 承認モック (2026-08-27 オーナー承認)

- **時刻選択シート**: mock/timepicker-sheet-a.html を採用 (approved-timepicker.png) — 帯横断・DateSelectionSheet 完全同型 (系列ラベルなし・等幅)。案B (コロン区切り・中央寄せ) は不採用
- **カレンダーダイアログ**: mock/datepicker-dialog-a.html を採用 (approved-datepicker.png) — 「今日」は操作行の左端 (todayText 指定時のみ)。現行 Material ダイアログと同じ操作位置。案B (中央 chip) は不採用
- **mock の適用範囲の注記** (相方レビュー spec-001 反映): mock が確定するのはカレンダー表示状態の構成と「今日」配置のみ。テキスト入力・年選択・範囲外 disabled・dark の各状態は Compose M3 標準構成 + design.md の色ロール対応表に従い、実装時の視覚検証 (tasks 4.5) で照合する

> 注: 以下の照合記録のうち material3 1.3.1 期に取得した datepicker 系証跡ファイルは、Compose BOM 引き上げ後の再照合 (後述の m3-140 節) で同一状態の `datepicker-m3-140-*` に置換済み。本文中の旧ファイル名は取得当時の記録として残している。

## 照合結果 — 時刻選択シート (2026-08-27)

- 証跡: `ui/verification/timepicker-24h-ja.png` / `timepicker-12h-ja.png` / `timepicker-12h-en.png` (samples/android を API 35 エミュレータで実行して撮影)
- `mock/approved-timepicker.png` と突き合わせ、**構造・トークン・意図の3点で一致**。視覚検証ループは1周で収束 (乖離ゼロ)
  - 構造: ドラッグハンドル → ヘッダー (取消 / タイトル / 確定 pill) → 行を横断する1本の選択帯 → 等幅の系列 (24h=2連 / 12h=3連)、系列ラベルなし
  - トークン: 既存シート (`SheetChrome` / `KsWheelView` / `PickerSheetStyle`) の部品と値をそのまま踏襲。視覚パラメータの新設なし。証跡の accent がモックと違う色なのは、サンプルアプリの `Theme` 実効値 (amber) と mock の既定 Theme (青) の差であり、解決経路は同一 (accent 3段解決)
  - 意図: 選択中行のみ accent + 太字、中央からの距離でフェード・縮小。12h の午前/午後は端末 Locale 由来 (ja=午前/午後、en=AM/PM を証跡で確認)

### mock との描画差 (合意済み妥協ではなく mock 側のフィラー)

- mock 12h の時系列は選択中「2」の上に「1」「12」を描いているが、実装は spec の「時 1–12」に従い巡回しないため「1」の上は空になる。mock の周辺行は分系列に存在しない「32」を描いているのと同じ性質の**構成確認用フィラー**であり、挙動の正は spec 側

### トークン候補 (mock/既存トークンに無く、実装で決めた値)

- **分候補の2桁ゼロ詰め表示** (`05` / `30`): mock は 28–32 しか描いておらず1桁分の表記が確定していない。時は非ゼロ詰め・分はゼロ詰めとした (時計表記の一般慣行、および iOS 側 `UIDatePicker` の分表記に揃う)。桁幅が揃うことで帯の中で数字が踊らない

## 照合結果 — カレンダー選択ダイアログ (2026-08-27)

- 証跡 (Pixel 6a / Android 16 実機で samples/android を実行して撮影。`ui/verification/`):
  - カレンダー表示: `datepicker-calendar-light.png` / `datepicker-calendar-outofrange-dark.png`
  - テキスト入力: `datepicker-textinput-light.png` / `datepicker-textinput-focused-light.png` / `datepicker-textinput-dark.png`
  - 年選択: `datepicker-yearselect-light.png` / `datepicker-yearselect-dark.png`
  - 範囲外 disabled: `datepicker-outofrange-light.png` / `datepicker-outofrange-nightmode.png` / `datepicker-calendar-outofrange-dark.png`
- `mock/approved-datepicker.png` と突き合わせ、**構造・トークン・意図の3点で一致**。視覚検証ループは2周で収束
  - 構造: ヘッダー (タイトル + 選択日 + モード切替アイコン) → 年月ナビ → 曜日行 → 日グリッド → 操作行。「今日」は操作行の左端 (todayText 指定時のみ)、右側にキャンセル / OK
  - トークン: design の色ロール対応表どおり。背景 = ダイアログ面 (カレンダー / 年選択 / テキスト入力の各面)、強調 = 選択日の塗り・年選択の選択状態・入力欄の枠とキャレット・操作行の文字、通常文字 = ヘッダ / 曜日 / 日付 / 年月 / 入力ラベル (範囲外はアルファ減)、アクセント上文字 = 選択日と選択年の数字 (実色に対する黒/白の自動選択が light/dark 双方で正しい側を選ぶことを証跡で確認)。証跡の accent が mock と違う色なのは、サンプルアプリの `Theme` 実効値 (light=amber / dark 検証用=青) と mock の既定 Theme (青) の差であり、解決経路は同一
  - 意図: 選択日だけが塗りで立ち、範囲外は同じ文字色のアルファ減で「読めるが操作できない」表現に留まる
- 端末の夜間モードだけを切り替えた証跡 (`datepicker-outofrange-nightmode.png`) は light の証跡と同一の見た目になる。配色はライブラリ `Theme` の実効値で決まり、ホストや端末の明暗に追随しない (隔離契約どおり)

### 視覚検証ループで潰した乖離

1. **1周目**: ダイアログの幅が内容合わせのままだと window 側の制限で縮み、日グリッドの最終列 (土曜) とヘッダの選択日が切れた → window 幅を明示 (面 360dp + 影の余白、画面が狭い端末では画面幅の 96% を上限)
2. **1周目**: テキスト入力欄の文字選択ハンドルが Material3 既定の紫のまま浮いた → 配色体系の土台自体を色ロールから導出し、対応表が届かない細部 (ハンドル・リップル・区切り線) も色ロールの近傍に収まるようにした

## 照合結果 — カレンダー選択ダイアログ / ランドスケープ (2026-08-27)

グループ5の回転検証で、ランドスケープでは選択面の高さが画面高を超え、カレンダー最終週と操作行 (今日 / キャンセル / OK) が画面外に落ちて操作できない状態だった (`evidence/datepicker-rotation-05-after-landscape.png`)。曜日ヘッダ行が二重に見える描画崩れも同時に観察された。

- 修正: 面の高さを可視領域 (画面高 − 影の余白×2) で頭打ちにし、収まらない分はカレンダー部を縦スクロールで送る。操作行は面の下端に固定して常に画面内に残す。ポートレートのように収まる高さでは `weight(fill = false)` により従来どおり内容の自然な高さで並ぶ
- 証跡 (Pixel 6a / Android 16 実機。`ui/verification/`):
  - `datepicker-landscape-fixed-opened.png` — 提示直後。面全体が画面内に収まり、操作行が見えている
  - `datepicker-landscape-fixed-scrolled.png` — カレンダー部を縦スクロールして月末まで到達。操作行は固定で残る
  - `datepicker-landscape-fixed-monthnav.png` — 月送り (6月 → 7月) が成立
  - `datepicker-landscape-fixed-yearselect.png` — 年選択面も画面内に収まる
  - `datepicker-landscape-fixed-selected.png` / `datepicker-landscape-fixed-confirmed.png` — 日付選択 → 確定で行の値が 2026/07/15 へ反映
- **曜日ヘッダの二重描画は高さ制約の導入で解消**した。縦方向にクリップされた状態での再描画に由来しており、面が可視領域に収まる構成では再現しない (上記証跡のいずれでも曜日行は1行のまま)
- **ポートレートの見た目は不変**: `datepicker-portrait-after-height-cap.png` を既存証跡 `datepicker-calendar-light.png` と突き合わせ、面の寸法・位置・余白・空き行まで同一
- 視覚パラメータの新設なし (既存の影の余白をそのまま高さ計算に使う)

### トークン候補 (mock/既存トークンに無く、実装で決めた値)

- **ヘッダの選択日の骨格 `yMMMEd`** (曜日の略称つき): Material3 の既定は曜日なし。mock が曜日を含む表記で描かれており、現行のカレンダー選択面の表記とも揃うため骨格を明示した。表記の並びと区切りは Locale から解決される
- **ダイアログの面の寸法**: 最大幅 360dp (Material3 のカレンダーの実寸)・角丸 28dp・影 6dp・影の余白 12dp・画面幅に対する上限 96%

## 再照合 — Compose BOM 2025.11.01 (material3 1.4.0) (2026-08-27)

初回の照合証跡は material3 1.3.1 時点で取得したもの。BOM を 2025.11.01 (compose 1.11.4 / material3 1.4.0) へ上げたため、tasks 4.5 の対象状態を 1.4.0 で撮り直して再照合した。

- 対象: カレンダー表示 / テキスト入力 / 年選択 / 範囲外 disabled の各状態 × light・dark (+ 端末夜間モード)
- 撮影条件: Pixel 6a / Android 16 実機、samples/android を BOM 2025.11.01 でビルド。dark と範囲外は初回と同じく検証用の一時パラメータ (dark Theme: 面 #121212 / accent #8AB3F9 / 文字 #EDEDED、範囲 2026/06/10–2026/06/20、初期値 2026/06/01 = 範囲外) を samples に一時的に当てて撮影し、撮影後にサンプルは元に戻している (リポジトリには残していない)
- 新証跡 (`ui/verification/`。旧証跡は上書きせず併存):
  - `datepicker-m3-140-calendar-light.png` / `datepicker-m3-140-yearselect-light.png`
  - `datepicker-m3-140-textinput-light.png` / `datepicker-m3-140-textinput-light-keyboard-hidden.png`
  - `datepicker-m3-140-outofrange-light.png` / `datepicker-m3-140-outofrange-nightmode.png`
  - `datepicker-m3-140-calendar-outofrange-dark.png` / `datepicker-m3-140-textinput-dark.png` / `datepicker-m3-140-yearselect-dark.png`

**判定: 一致。** `mock/approved-datepicker.png` の構成と「今日」配置、および design.md Decision 4 の色ロール対応表と、1.4.0 でも突き合わせて崩れ・色ずれ・スタイル変化なし。

- 構造・トークン・意図の3点は初回照合の記述がそのまま成立する (ヘッダ / 年月ナビ / 曜日行 / 日グリッド / 操作行、「今日」は操作行の左端)
- 旧証跡との差分は機械的にも確認した。ダイアログ面の矩形 (x80–1000, y500–1950) で旧新を画素比較すると、有意差 (チャンネル差 >12) はカレンダー light 48px / 年選択 light 48px / 範囲外 light 16px / カレンダー dark 22px / 年選択 dark 22px (いずれも 133 万画素中) で、位置は面の縁のアンチエイリアス (背後のリストのスクロール位置が撮影時に違うため) のみ。**描画そのものの変化はゼロ**
- 端末の夜間モードだけを切り替えた証跡 (`datepicker-m3-140-outofrange-nightmode.png`) は light と同一の見た目。1.4.0 でも隔離契約 (ホスト・端末の明暗に追随しない) は保たれている

### 1.3.1 → 1.4.0 で変わった点 (1件・色ロールとは無関係)

- **テキスト入力モードが自動でフォーカスされる**: 1.3.1 ではモード切替アイコンを押した直後の入力欄は非フォーカス (枠・ラベルが灰、キーボードなし) で、欄をタップして初めてフォーカスされた。1.4.0 では切替と同時に入力欄がフォーカスを取り、枠とラベルが accent 色になりキーボードが出る (`datepicker-m3-140-textinput-light.png` / `-dark.png`)。M3 既定の挙動変化であり、色ロールの写像は変わっていない — 1.3.1 のフォーカス時証跡 (`datepicker-textinput-focused-light.png`) と 1.4.0 の切替直後を比べると、有意差はキャレット/選択ハンドルの有無 (x310–540, y808–992 に集中) だけで、枠・ラベル・文字・操作行の色は同一
- 上記以外 (カレンダー・年選択・範囲外 disabled・dark・夜間モード) は変化なし
- 本再照合の対象は tasks 4.5 の状態一式。ランドスケープ (tasks 4.6) の証跡は再取得していない
- 証跡の整理: `ui/verification/` は最終周の集合だけを残す。1.4.0 で撮り直した状態の 1.3.1 時点の画像 (カレンダー / 年選択 / テキスト入力 / 範囲外 の light・dark・夜間モード 9 枚。上の比較記述が名前で挙げているものを含む) は削除済みで、比較の結論は本節の記述が持つ。ランドスケープ (`datepicker-landscape-fixed-*.png`)・ポートレート再確認 (`datepicker-portrait-after-height-cap.png`)・時刻シート (`timepicker-*.png`) は 1.4.0 で撮り直していない状態の唯一の証跡のため残している
- 個人情報の写り込みなし (目視確認。写るのはステータスバーの時刻・通信/電池アイコン・アプリ由来の通知アイコンのみで、アカウント名・実データは画面外)
