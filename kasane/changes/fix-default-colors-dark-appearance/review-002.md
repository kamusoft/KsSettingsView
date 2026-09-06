# レビュー結果: fix-default-colors-dark-appearance (002 回目)

**日付**: 2026-09-05
**判定**: CHANGES_REQUESTED

## サマリー

1 周目の指摘は、ホスト review-001 の 6 件・相方 second-opinion-code-001 の採用 3 件のうち **8 件が解消**した。`KsThemedContext` の夜間モード照合による作り直し、`init` の解決 → 装飾組み立ての順序入れ替え、公開 KDoc からの ADR ID 除去、factory 前提の契約明記はいずれも実装とテストの両方で閉じており、実経路テスト 5 本 (行をタップして選択面を提示し、外観を切り替え、閉じて開き直すまで通す) は相方 Major の指摘した検出力の穴をふさいでいる。MAUI Android の再実証も `Recreate()` を一時無効化したビルドで取り直され、インスタンス識別子の前後一致という反証可能な形で記録されている。

一方で、相方 Major の根本原因である「行の Context は生成時のテーマを保持する」は**選択面・ダイアログ経路にしか適用されていない**。行そのものが読むテーマ属性 (`EntryCell` の placeholder 色、`SwitchCell` のオフ状態色) は ViewHolder 生成時の古い `KsThemedContext` から解決され続けるため、Activity を再生成しないホストで外観を切り替えると placeholder がほぼ判読不能になる。この症状は本 change 自身の証跡画像 `ui/verification/android-native-input-cells-live-dark-after.png` に写っている (画素実測: 文字 #252220 / 地 #2A2620)。あわせて、相方 Minor の「テストコメントの履歴記述」が iOS 側で 2 箇所残っている。前者を Major として CHANGES_REQUESTED とする。

### 実行した検証

- Android: `ANDROID_HOME=<SDK> ./gradlew test --rerun-tasks` (`android/`) — 216 の結果 XML を集計して **2806 tests / 0 failures** (debug + release、`124 actionable tasks: 124 executed` で全件再実行を確認)
- iOS: `xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,id=<iPhone 17 Pro>'` — バンドル集計行の合算で **1021 tests / 0 failures** (Bridge 166 / Core 88 / SwiftUI 94 / TestSupport 7 / UI 666)、`** TEST SUCCEEDED **`。iOS は本周でコメント変更のみだが、全件を自分で回して確認した
- lint: `scripts/comment-policy-lint.py` 禁止 0 件 (778 ファイル)、`scripts/local-path-lint.py` 0 件、`scripts/identity-lint.py` 0 件
- 足場凍結: `specs/` / `proposal.md` / `design.md` / `exploration.md` / `ui/mock/` に diff なし
- 証跡の実在と対応: `ui/verification/` 38 件 + `baseline-light/`、`evidence/` 9 件を実在確認。`android-native-picker-sheet-reopen-dark-after.png` は brief.md の手順 (入力 Cell 5 種デモの PickerCell、`InputCellsDemoScreen.kt` の「テーマを選択」シート) と一致し、暗い地色・明るいドラッグハンドル・白い題字という記述どおりの状態を写している。`evidence/maui-android-norecreate-instance-identity.txt` は個体特定値を含まず、identityHashCode の前後一致という主張どおりの内容

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| cross/comment-policy.md | 常時 (always) |
| cross/test-execution.md | テスト実行・テスト結果の報告 |
| cross/sample-parity.md | `samples/android` の `SampleTheme` を触る |
| cross/runtime-behavior-verification.md | 外観の実行時切替の完了判定 |
| ios/swift6-language-mode-check.md | `ios/Sources/` を触る変更の完了判定 |
| core/ADR-0030 (proposed) / core/ADR-0009 / android/ADR-0020 / cross/ADR-0016 | 本 change の設計根拠と隣接決定 |
| concepts/core/styling/style-resolution.md | 実効値解決の現行記述 |
| lessons/code-review.md (L-001) / lessons/process.md (L-002・L-003・L-005・L-006) | 昇格済みルール |

core / android のドメイン handbook は存在しない。`cross/public-identifiers.md` / `release-procedure.md` / `local-development-setup.md` / `user-skill-api-listing.md` / `aiforms-origin-reference.md` は担当範囲に当たらないため本文まで読んでいない。

`cross/runtime-behavior-verification.md` について: 相方 Major の `KsThemedContext` 修正は「修正前ビルドでの症状再現 A/B を取っていない」と brief.md に明記されているが、同規約の適用範囲は「ユニットテストでその症状自体を再現できない不具合」であり、本件は Robolectric の実経路テスト (`選択面を開き直すとテーマ属性も夜間側で解決される` が `colorSurface` の実解決値を観測する) で症状を再現・固定できているため対象外と判定した。規約違反としては扱わない。

## 1 周目指摘の解消状況

| 出所 | 指摘 | 判定 | 根拠 |
|---|---|---|---|
| review-001 [Minor] | `init` が未解決 Theme で ItemDecoration を組み立てる | **解消** | `KsSettingsView.kt:269-273` で解決を装飾組み立ての前へ移動。回帰テスト `applyDiff だけで内容を入れる View の装飾も解決済みの色を持つ` が夜間で separator / Cell 背景 / list 下地の 3 色を固定 |
| review-001 [Minor] | 公開 KDoc の ADR ID | **解消** | `Theme.kt` / `KsSettingsViewDefaults.kt` の公開 KDoc から除去。残る ADR 参照は internal (`KsThemePalette.kt:11`・`EffectiveStyle.kt`) と Swift のファイル冒頭ブロックコメント (`ios/Sources/KsSettingsViewUI/Theme.swift:7`) のみで、いずれも「公開メンバーの doc コメント」に当たらず規約適合 |
| review-001 [Minor] | factory を外観と食い違って渡した場合の契約 | **解消** | `KsSettingsViewDefaults.kt` の KDoc に前提と回避策を明記し、`夜間に lightTheme を渡すとタイトルだけ夜間の既定になる` が実効値 (Cell 背景は light / title と description は dark) を固定 |
| review-001 [Suggestion] | `ButtonCellViewHolder` の外観判定 2 回引き | **解消** | `ButtonCellViewHolder.kt:89-100` で 1 回に統一し両引数へ渡す |
| review-001 [Suggestion] | 開いたままの選択面が再配色されない側の未アサート | **解消** | `開いたままの選択面は外観が切り替わっても再配色されない` がシートのタイトル文字色と面の色を切替前後で比較し、空振り防止の逆アサートまで付いている |
| review-001 [Suggestion] | 同ファイル内の既存 ADR 参照 | **解消** | `Theme.kt` / `CellStyle.kt` / `Theme.swift` の公開 doc コメントから除去 |
| second-opinion [Major] | 外観変更後も古い `KsThemedContext` が再利用される | **部分解消** | `KsThemedContext.kt:34` の `createdNightMode` と `:105` の照合で、選択面・カレンダーの選択面の経路は解消 (実経路テスト 3 本で実証)。**行の ViewHolder が読むテーマ属性は未解消** — 下の [🟠 Major] を参照 |
| second-opinion [Major] | MAUI Android の「Activity 再生成なし」の根拠矛盾 | **解消** | tasks.md 0.1 に「0.1 の再実施」節を追加し、初回記述を取り消し線と訂正文で明示。`Recreate()` を一時無効化したビルドで再取得し、Activity / KsSettingsView / RecyclerView の identityHashCode 一致 (`evidence/maui-android-norecreate-instance-identity.txt`) と未指定色・明示色の両方の追随 (画像 4 枚) を記録。brief.md 側も初回記述に訂正を差し込んでいる |
| second-opinion [Minor] | 公開 KDoc の内部識別子とテストコメントの履歴記述 | **部分解消** | Kotlin 側 (`KsSettingsViewDefaultsTest.kt` / `ThemeTest.kt`) は解消。**iOS 側に 2 箇所残存** — 下の [🟡 Minor] を参照 |
| second-opinion [Suggestion] | brief.md の初回不一致と最終状態の矛盾 | **解消** | 「最終状態の一覧 (2026-09-05 時点)」の表を照合記録の冒頭に置き、「この表が正・下の各節は経緯として読む」と明示。初回照合節にも訂正文を差し込み済み |

## 指摘事項

### [🟠 Major] 行の ViewHolder が生成時 Context のテーマ属性を保持し、外観の live 切替で追随しない — placeholder が判読不能になる

**該当箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCellViewHolder.kt:80`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SwitchCellViewHolder.kt:161-170`

**問題点**:

相方 Major の根本原因は「`ContextThemeWrapper` は生成時のテーマを組み立てて保持する」ことにある。今回の修正はこれを `ksThemedContext()` の入口 (`KsThemedContext.kt:105`) で解決したため、**そこを通る選択面とカレンダーの選択面だけ**が新しい外観のテーマ属性を得るようになった。行そのものは `ksThemedContext()` を通り直さないため、同じ問題が残っている。

- `EntryCellViewHolder.kt:80` — `private val hostHintTextColors: ColorStateList? = editText.hintTextColors` は **ViewHolder 生成時**に、その時点の `KsThemedContext` が解決した `android:textColorHint` を捕まえる。placeholder 色がどの段にも指定されていないとき (`EffectiveStyle.effectivePlaceholderColor` が `Unspecified` を返す経路) はこの値が復元される
- `SwitchCellViewHolder.kt:161-170` — `MaterialColors.getColor(switchView, colorSurfaceContainerHighest / colorOutline, ...)` は `switchView.context`（= 生成時の `KsThemedContext`）のテーマから解決する。コメントは「ダークテーマでの明度の反転にそのまま追従する」と書いているが、Activity を再生成しないホストではこの前提が崩れる

同梱テーマは `Theme.Material3.DayNight.NoActionBar` (`android/kssettingsview/src/main/res/values/themes.xml:14`) なので、外観ごとに `textColorHint` / `colorSurfaceContainerHighest` / `colorOutline` は別の値へ解決される。`applyThemeInternal` は意図的に payload 付き `notifyItemRangeChanged` で「同一 ViewHolder への再 bind」に留めるため、ViewHolder は作り直されず、上記 2 つは古い外観の値のまま残る。

**実害は本 change 自身の証跡に写っている**。`ui/verification/android-native-input-cells-live-dark-after.png` (`configChanges="uiMode"` を一時付与して同一 View のまま夜間へ切り替えた画面) の「ニックネーム (callback)」行の placeholder 「callback 経路で更新」を画素サンプリングすると、文字 `#252220` / 地 `#2A2620` でコントラスト比はほぼ 1:1 になっている。同じ画面のライト側 (`...-live-light-before.png`) では文字 `#A9A8AA` / 地 `#FFFFFF` で読める。ダークの正しい姿は DayNight の dark `textColorHint` (明るいグレー) であり、切替前のライト値が残ったための症状である。

settings-view-android-ui「夜間モード変更時の未指定色の再解決」は「表示中の**行**・list 下地・Section 装飾・Header / Footer に再適用する (SHALL)」と定める。placeholder は「未指定色の外観解決」で platform 既定へ委ねる契約だが、その platform 既定は**現在の外観の**ホストテーマ hint 色であるべきで、切替前の外観の値ではない。本 change が新設した live 切替経路で、本 change が解消しようとした「暗い地に暗い文字で読めない」症状がそのまま残っている。

**推奨修正**:

いずれか 1 つで閉じる。

- (推奨・局所) bind 時にテーマ属性を **`views.root.context.ksThemedContext()`** から解決し直す。修正済みの `ksThemedContext()` は行の古いラッパを受け取ると現在の外観のラッパを作り直して返すため、`hostHintTextColors` を `val` から「bind 時に現在のラッパから引き直す」形へ変え、`SwitchCellViewHolder` の `MaterialColors.getColor` の第 1 引数も同じラッパ由来の Context 経由にすれば、行も追随する
- (代替) 外観が変わったときに限り `recyclerView.recycledViewPool.clear()` + 全件通知で ViewHolder を作り直す。`表示中に夜間モードへ切り替わると未指定色が追随し identity は保たれる` が固定しているのは Section / Cell の id であって ViewHolder の同一性ではないため、この案でも既存テストは崩れない。ただし `applyThemeInternal` の doc コメントが避けている「全件再生成」に戻るため、ちらつきとのトレードオフになる

回帰テストは、夜間へ切り替えた後の `EditText.hintTextColors.defaultColor` が切替前と異なり、新しく生成した同梱テーマ付き Context の `textColorHint` と一致することを 1 本固定すれば足りる (既存の `ThemeAppearanceResolutionTest` の `switchToNight` ヘルパがそのまま使える)。

### [🟡 Minor] 新規に書いたテストコメントに履歴記述が残っている (comment-policy 違反)

**該当箇所**: `ios/Tests/KsSettingsViewUITests/ThemeDefaultColorAppearanceTests.swift:61`、`ios/Tests/KsSettingsViewUITests/SectionAccessoryRenderingTests.swift:293`

**問題点**:

handbook cross/comment-policy.md「禁止する記述類型」は進捗ログ・履歴記述 (時間軸を含む記述) を禁じ、「禁止する参照」は変更識別子の裸参照を禁じている。相方 Minor で指摘された型のうち、Kotlin 側 (`KsSettingsViewDefaultsTest.kt` / `ThemeTest.kt`) は解消したが、iOS 側に新規追記として次が残っている。

- `ThemeDefaultColorAppearanceTests.swift:61` — `// MARK: - ライト外観の既定色は本 change 前と同じ固定値`。「本 change」は変更識別子への裸参照であり、かつ「〜前と同じ」は履歴記述。同 `:63` のテスト名 `test_既定色のライト解決値は従来の固定値と一致する` も「従来」で同じ時間軸を語っている (規約はコメントを対象とするが、名前も同じ読み手を混乱させる)
- `SectionAccessoryRenderingTests.swift:293` — `// ライトでは従来どおりのグレー（≒ #6D6D72）、ダークでは dark セットのグレー（#8E8E93）に` の「従来どおり」

`comment-policy-lint.py` は「本 change」「従来」を禁止語として検出しないため (禁止 0 件)、規約本文からのレビュー判定に当たる範囲である。

**推奨修正**: 現在成立すべき条件として書き直す。`:61` は「ライト外観の既定色は固定値」、`:63` は `test_既定色のライト解決値が固定値と一致する`、`SectionAccessoryRenderingTests.swift:293` は「ライトはグレー（≒ #6D6D72）、ダークは dark セットのグレー（#8E8E93）に解決される」で意味は保たれる。

### [🔵 Suggestion] 触れたテストファイルに残る裸の変更識別子

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ThemeTest.kt:30`

**問題点**: 本 diff が直上の行 (`:28`) を書き換えたブロックの中に、`// rowHeight = -1, hasUnevenRows = true（refine-cell-layout-after-unify-review で ...）` という変更識別子の裸参照が残っている。comment-policy「禁止する参照」に当たる既存債務で、lessons process L-005 の「本 change で触れたファイル内、数行で閉じる」範囲。`BasicCellsTest.kt` にも同種が 4 箇所あるが、そちらは本 diff が触れた行から離れているため対象外と判断した。

**推奨修正**: 識別子を落とし、「Auto 高さ + 下限保証を既定とする」という現在形の説明だけ残す。判断が要るなら本 change では見送り、記録だけ残す。

## 確認して問題が無かった観点

- **仕様充足**: settings-view-android-ui / settings-view-ios-ui / maui-bridge / samples-{android,ios,maui} の Requirement と Scenario に、テストまたは視覚証跡が対応している。`ThemeAppearanceResolutionTest` は 22 本まで増え、初期解決・ButtonCell の独立性・夜間切替と attach 時照合・構造更新をまたぐ追随・選択面の 3 経路 (開き直し / 開いたまま / テーマ属性) ・`Unspecified` の非到達・枠線透明を網羅する
- **tasks.md の虚偽チェック**: 9.1 (蒸留への申し送り) 以外すべて実装・テスト・証跡と対応する。0.1 は初回記述を取り消し線で無効化したうえで再実施節を正としており、チェックの根拠が現行 Sample コードと矛盾しない形に直っている
- **deviation**: 7 件の乖離と 5 件の `[付随修正]` に増減なし。いずれも本 change が触れたファイル内・数行規模で、テストで担保されている
- **解決点の単一化**: `EffectiveStyle.from` / `ItemDecoration` / Header・Footer ViewHolder / `PickerSheetStyle` / `resolveDatePickerDialogColors` / `RecyclerView` 背景がすべて解決済み `internalTheme` を読む。14 箇所の `EffectiveStyle.from` 呼び出しは全て `(theme, style, darkTheme)` の新シグネチャで、外観の出所は `isKsDarkAppearance()` に統一されている
- **構造更新経路**: `applyRootDirect(root, userTheme)` により、Full diff・Section 置換・可視性変更・Store 再同期が `themeBacking` を書き戻さない。`setRootDirect` は利用者 Theme を伴う 2 経路 (bind / Store 再購読) だけが使う
- **`KsThemedContext` の作り直し**: `themeBaseContext()` は `KsThemedContext` を `ContextWrapper` として素通りしてラップ元 (Activity) まで降りるため、古いラッパから作り直しても入れ子にならない。キャッシュは夜間モードごとに 1 エントリで置き換わり、`WeakReference` のままなので保持は増えない
- **bridge**: `KsBridgeTheme.resolve()` / `KsBridgeCellStyle.resolve()` の `color(argb) = KsBridgeColor.color(argb) ?: Color.Unspecified` は wire 形式 (項目・型・null の意味) を変えない。色以外は従来どおり `base` の既定で埋める
- **iOS**: `SectionBoxDecorationView` は `appliedBorderColor` を保持し iOS 17+ / 16 の両経路で `layer.borderColor` を再解決する。`apply(layoutAttributes)` の early return 側でも `.clear` へ揃えてから再適用しており、再利用時の色残りがない
- **テストの手抜き**: 新規テストは実描画値 (シート内 `design_bottom_sheet` の `backgroundTintList`、`TextView.currentTextColor`、`colorSurface` の実解決値) を観測点に取り、いずれも「空振りしていないこと」の逆アサート付き。実経路テストは行を `performClick()` して `ShadowDialog.getLatestDialog()` を取るため、`PickerSheetStyle` を直接組み立てるだけの以前の形より検出力が上がっている
- **sample-parity**: `SampleTheme.sectionDecorationDemo` の引数型変更のみで、文言・画面構成・デモデータ・色定数は不変
- **証跡の実在と対応 (lessons L-003)**: `ui/verification/` の再撮影分・再実証分と `evidence/` の再 spike 分がいずれも実在し、brief.md / tasks.md の記述と対応する。個人要素の確認は初回・再照合・再実証の 3 回とも記録されている。「修正前ビルドの A/B は取っていない」旨も明記されており、証跡の範囲が読み手に分かる

## アクションプラン

1. 行の ViewHolder が読むテーマ属性 (`EntryCellViewHolder.kt:80` の hint 色、`SwitchCellViewHolder.kt:161-170` のオフ状態色) を bind 時に現在の外観のラッパから解決し直し、夜間へ切り替えた後の `hintTextColors` を固定する回帰テストを 1 本足す。修正後、`android-native-input-cells-live-dark-after.png` に相当する面を撮り直して placeholder が判読できることを証跡に残す
2. `ThemeDefaultColorAppearanceTests.swift:61` / `:63` と `SectionAccessoryRenderingTests.swift:293` の履歴記述を現在形へ書き直す
3. (任意) `ThemeTest.kt:30` の裸の変更識別子を現在形の説明へ整理する
