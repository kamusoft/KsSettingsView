# 実装ノート: datepickercell-color-adjust

実装中の調査で確定した、material-components 1.12.0 の内部挙動と実測値の記録。

**位置づけ**: デザイン判断・部位対応・視覚照合結果は `ui/brief.md` が持つ。ここは
「なぜその補正になったか」を支える実測と機序であり、ライブラリ更新時の追随作業で読む資料。
挙動の正はデルタスペック、見た目の正は `ui/mock/approved.png`。

---

## 1. ヘッダ重なりの真因 (実機スパイク: Pixel 4a / API 33 相当・density 2.75・fontScale 1.0)

`mtrl_picker_header_title_and_selection` 配下の 2 TextView を実機で計測した結果:

| View | top | paddingTop | baseline | 期待 (XML 指定) |
|---|---|---|---|---|
| `mtrl_picker_title_text` | 0 | 0 | 46px | 28dp = 77px |
| `mtrl_picker_header_selection_text` | 0 | 0 | 103px | 100dp = 275px |

`firstBaselineToTopHeight` が**まったく効いていない**。原因は、この 2 つの XML が使う
`app:firstBaselineToTopHeight` / `app:autoSizeTextType` / `app:lineHeight` が
**AppCompat 名前空間の属性**であるのに対し、`MaterialDatePicker` のダイアログは AppCompat の
View インフレータを持たない Context で inflate されるため (計測時に生成クラスが
`AppCompatTextView` ではなく素の `TextView` / `Button` であることを確認)、これらの属性が
解釈されずに落ちること。結果、両テキストが同一 FrameLayout の上端に重なる。

「日本語グリフが大きいから食い込む」ではなく、**縦位置指定そのものが失われている**のが真因
(当初仮説の棄却)。

この前提は `DatePickerMaterialContractTest`
(`ヘッダの寸法指定は素の TextView に解釈されない前提のままである`) が固定している。
ライブラリ側が解釈されるように変えたら補正は不要になるため、そこで気付ける。

## 2. ヘッダ補正のパラメータ

- **ベースライン**: ライブラリが意図した寸法リソース
  (`mtrl_calendar_title_baseline_to_top` = 28dp / `mtrl_calendar_selection_text_baseline_to_top` = 100dp。
  横向きは 24dp / 64dp) を実行時に解決して設定し直す。縦横で値が異なるため定数化しない
- **選択日テキストの縮小率**: Material 既定 (32sp) の **0.8 倍** (承認モックの 26px 相当)。
  ヘッダ高さ (120dp) は変更しない
- **幅に収めるための追加縮小**: `autoSizeTextType` も同じ理由で失われており幅方向の自動縮小が効かない。
  表示幅に収まらない場合は既定の **0.5 倍**を下限に 1px 刻みで追加縮小する
  (Requirement「クリップされずに読める」を満たすため)

## 3. 減光した派生色の比率

`PickerDialogColors` に追加した 3 種 (いずれもモック確定値):

| 派生色 | 導出 | 使う部位 |
|---|---|---|
| `disabledText` | 通常文字を背景へ 38% で重ねる | 無効日・非フォーカス時の入力欄枠 (モックの `--text-dim`) |
| `disabledAccent` | アクセントを背景へ 38% で重ねる | OK の無効状態 (material の disabled alpha と一致) |
| `subduedText` | 通常文字を背景へ 70% で重ねる | helper / placeholder (モックの helper 不透明度) |

## 4. 日付セル / 年セルの状態判定

### ライブラリ側の実装 (material 1.12.0)

- `CalendarItemStyle.styleItem()` は呼ばれるたびに `MaterialShapeDrawable` を**新規生成**して
  `View.setBackground` する。つまり **Drawable のインスタンスが同じ間はロールも変わっていない**
- `MonthAdapter.updateSelectedState()` は `dayTextView.setSelected(selected)` /
  `setEnabled(valid)` を立ててから `styleItem()` を呼ぶ
- `YearGridAdapter.onBindViewHolder()` は View 状態を一切立てず、`CalendarItemStyle` の差
  (`selectedYear` / `todayYear` / `year`) だけで役割を表す

### `isSelected` が使えない理由 (2026-08-02)

`MonthAdapter` が立てた `setSelected(true)` は、直後に `AbsListView.setupChild()` が
「選択位置以外の子は非選択」として `setSelected(false)` で塗り潰すため、レイアウト後に残らない。
実 `MaterialDatePicker` を表示して日グリッドの全セルを調べたところ、選択日を含めて
`isSelected == false` だった (階層に同時に存在する `month_grid` すべてで 0 件)。

この観測は `DatePickerDialogIntegrationTest`
(`実ダイアログの日付セルには isSelected が残らない`) が固定している。material 側が状態を保つように
変われば失敗し、そのとき状態判定を View 状態ベースへ戻せる (下記「既知の限界」も同時に消える)。

したがって、選択 / 今日 / 通常を区別できる手掛かりは
**「ライブラリがその View に与えた背景 Drawable の塗り・枠」だけ**である。

### 採用した設計

`DatePickerColorizer.resolveCalendarItemRole` は次の順で決める:

1. `isEnabled == false` → 無効 (走査はこの状態を書き換えないため毎回読んでよい)
2. 背景 Drawable のインスタンスに対して**確定済みのロールがあればそれを再利用**
   (`WeakHashMap<MaterialShapeDrawable, Role>`)
3. 未確定なら、ライブラリが与えたばかりの塗り・枠の可視性で決める
   (塗り → 選択 / 枠 → 今日 / どちらも無し → 通常)

2 が本質。判定は Drawable 1 個につき 1 回だけなので、走査が塗り替えた後の色を読み直すことがない。
これが無いと「透明なアクセント色を適用 → 次のフレームで塗りが不可視 → 非選択へ転落 → また選択」
という自己参照の振動が起きる (回帰テスト: `透明なアクセント色でも選択日/選択年は選択ロールを保つ`)。
ライブラリが塗り戻すと新しい Drawable インスタンスになるため、キャッシュは自動的に無効化され、
弱参照キーなので解放も漏れない。

### 既知の限界

**この節が本件の詳細の一本化先**。`DatePickerColorizer.resolveCalendarItemRole` の KDoc と
`ui/brief.md`「実装で確定した部位の扱い」は要点だけを述べ、ここを参照する。

ホストが `materialCalendarDay` 等を上書きして**通常項目にも可視の塗り・枠**を与えた構成では、
その項目が選択 / 今日と同じロールで描かれる (塗りならアクセント塗り + アクセント上文字、
枠なら枠だけが強調色)。ライブラリが役割ごとのスタイルを外へ公開しておらず、描画以外に区別できる
手掛かりが無いため (`isSelected` が使えないことは前節のとおり)。

配色がテーマへ寄る方向の誤りであり、フレーム間で振動することはない
(回帰テスト: `通常日に可視の塗りを持つカスタム style でも配色は振動しない` /
`通常日に可視の枠を持つカスタム style は枠だけが強調色になる`)。

受容の経緯は `deviation.md` の 1 件目 (既定 Material 構成では発生せず、ADR-0008 の走査方式内に
描画非依存の代替が無いため、合意済みの制約として受容)。

## 5. テスト経路の分担

| テスト | 通す経路 | 守るもの |
|---|---|---|
| `DatePickerColorizerTest` | 合成 View 階層へ `colorize()` 直呼び | 色マッピング・ヘッダ補正の数値・状態判定・冪等性 (入力値を作り込みたいケース) |
| `DatePickerMaterialContractTest` | 実レイアウトの inflate | 走査が依存する ID・View 型・親子関係が material 側に存在すること |
| `DatePickerDialogIntegrationTest` | `FragmentActivity` 上の実 `MaterialDatePicker` | `attach()` → `FragmentLifecycleCallbacks` → pre-draw → window 背景の実経路と、実 View 階層での着色 |

`MaterialDatePicker` はダイアログ表示 (非フルスクリーン) で `mtrl_calendar_horizontal` を使い、
月送りは `RecyclerView` の水平ページング。`SmoothCalendarLayoutManager` が前後の月も先読みで
配置するため、階層には `month_grid` が同時に複数存在する (統合テストはその全部を対象にする)。
