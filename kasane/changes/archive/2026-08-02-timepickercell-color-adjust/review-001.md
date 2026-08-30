# レビュー結果: timepickercell-color-adjust (001 回目)

**日付**: 2026-08-02
**判定**: CHANGES_REQUESTED

## サマリー

デルタスペックの 2 Requirement / 7 Scenario はいずれも実装で満たされており、実機証跡 5 枚を実際に確認した結果、承認モックの色ロール配置 (背景 / 強調 / 通常文字 / アクセント上文字 / 中間面) が再現されていることを画素値レベルで確認できた (中間面 `#E3E1DA` ≒ 導出値 `#E4E1D9`、通常文字 `#555555` = `cellTitleColor` 完全一致)。設計も ADR-0006 の方針どおりヘルパ 1 クラスに閉じており、静的適用 / 動的適用の分離・CSL による状態駆動・material 内部実装への依存の隔離は妥当。

一方、新規に書かれたコメントが `concepts/cross/conventions/comment-policy.md` の禁止参照 (変更アーティファクトのパス参照・Scenario 名の裸参照・承認モックへの裸参照) に 5 ファイル 10 箇所で該当する。これは直近の別 change (fix-cell-accessory-vertical-fill) で Major 指摘済みの型であり、アーカイブ後に参照先が死ぬ性質のため修正を求める。加えてテスト範囲にキーボード入力モードと pre-draw 冪等再適用の穴がある。

### 実行した客観確認

| 項目 | 結果 |
|---|---|
| `cd android && ./gradlew test` | tests=621 failures=0 errors=0 (debug) / 621・0・0 (release)。内訳 core 74 / compose 76 / ui 471 |
| `./gradlew :ks-settingsview-ui:testDebugUnitTest`（新規テスト込みで実行） | BUILD SUCCESSFUL |
| `./gradlew :ks-settingsview-ui:lintDebug` | BUILD SUCCESSFUL (`PrivateResource` 指摘なし) |
| 足場凍結 | `proposal.md` / `specs/` は無変更。`tasks.md` はチェックボックスのみ 21 箇所、`ui/brief.md` は追記のみ 54 行 — 逆流修正なし |
| deviation.md | 不在 (合意済み乖離なし) |
| 実機証跡 | `verification/` 5 枚を実閲覧し、画素値をサンプリングして色ロールの適用を確認 |

## デルタスペック一致検証 (ksn-verify 兼務)

### Requirement: TimePickerCell の時刻選択ダイアログはテーマ配色を反映する (Android)

| Scenario | 実装 | 単体テスト | 実機証跡 |
|---|---|---|---|
| テーマ色の反映 | `TimePickerColorizer.colorize` / `applyStaticRole` / `applyToWindowBackground` (`TimePickerColorizer.kt:213,243,159`) | `TimePickerColorizerTest` の色マッピング 7 件 (ヘッダ文字 / OK・キャンセル / モード切替 / チップ / 文字盤 / 針 / AM・PM) | `keyboard-24h.png` `clock-24h-hour.png` — 背景 `#F1EFE7`・強調ゴールド・通常文字 `#555555` を確認 |
| 12時間フォーマットでの反映 | `applyPeriodToggleButton` + `checkedStateList` (`TimePickerColorizer.kt:356,365`) | `AM PM トグルは checked 状態で強調ロール` | `clock-12h-am.png` `keyboard-12h-pm.png` — 選択セルが強調色 + 黒文字 (onAccent)、非選択セルはダイアログ背景 (白ではないことを画素で確認) |
| 入力モード切替後も配色が維持される | `installPreDrawHook` + `styledViews` による遅延生成 View への追随 (`TimePickerColorizer.kt:177,114`) | **なし** (Minor-2) | `keyboard-12h-pm.png` (時計→キーボード切替後) |
| 時刻選択の操作後も配色が維持される | `applyDynamicRole` / `applyClockFaceNumber` (`TimePickerColorizer.kt:231,297`) | `文字盤の数字は shader を落として単色描画にする` (部分。再適用の冪等性は未検証) | `clock-24h-minute.png` (時→分遷移後) |
| アクセント上の文字の可読性 | `ColorRoles.contrastingBlackOrWhite` (`TimePickerColors.kt:90`) | `TimePickerColorRolesTest` 5 件 (代表色・純白/純黒・境界近傍 117/118・アルファ非依存) | ノブ上の数字が黒 (`clock-*.png`) |

### Requirement: TimePickerCell のアクセント色は Cell 固有値を先頭に解決される (Android)

| Scenario | 実装 | 単体テスト |
|---|---|---|
| Cell 固有値の優先 | `TimePickerCellViewHolder.resolveDialogColors` の `cell.accentColor?.toArgb() ?: effective.accentColor` (`TimePickerCellViewHolder.kt:79`) | `アクセント色は Cell 固有値を優先する` |
| 未指定時のフォールバック | 同上 (`EffectiveStyle.effectiveAccentColor` = `cellStyle.accentColor` → `theme.cellAccentColor` を再利用) | `〜CellStyle へフォールバックする` / `〜Theme へフォールバックする` |

一致判定: **VALID** (全 Scenario に実装が対応。テスト対応の穴 2 件は下記 Minor で指摘)。

## 指摘事項

### [🟠 Major] 新規コメントがソースコメント規約の禁止参照に該当する

**該当箇所**:
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerCellViewHolder.kt:70-71`
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerColors.kt:12-14, 28, 53, 59`
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerColorizer.kt:44-45, 240, 375`
- `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerColorRolesTest.kt:9-10`
- `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerColorizerTest.kt:29-30`

**問題点**: `kasane/concepts/cross/conventions/comment-policy.md` は、コメント中の外部参照を `<domain>/ADR-NNNN` / URL / RFC 等の 3 種に限定し、次を明示的に禁止している。

- 変更アーティファクトのパス参照 — `仕様: kasane/changes/timepickercell-color-adjust/specs/cell-types-input/spec.md`
- Scenario / Requirement 名の裸参照 — `"アクセント上の文字の可読性" Scenario` / `〜 Requirement。`
- 拡張子なし・パスなしのアーカイブ文書への裸参照 — `同 spec の` / `ui/brief.md の部位対応表を正とする` / `見た目の正は承認モック` / `ui/mock/variant-a-background-color.html の --surface-derived`

適用範囲にテストコードも含まれる (規約「適用範囲と運用」)。これらの参照先は蒸留・アーカイブ後に到達不能になり、`TimePickerColors.kt` の透過率定数のように「値の根拠」を参照に委ねている箇所は、根拠がコード側から追えなくなる。同型の違反は直近の change で Major 指摘・複数サイクルの修正コストになっている。

なお `TimePickerCellViewHolder.kt:22-23` の `openspec/changes/...` 参照は本 diff 以前から存在する既存コメントであり、本指摘の対象外 (触れているファイル内ではあるので、ついでに整えるかはオーケストレーター判断)。

**推奨修正**:
- `仕様: <パス>` の定型句は削除する (規約の「定型句型」)。設計根拠を残したい箇所は `android/ADR-0006` 参照に置換する — ADR-0006 は方式・状態キー・shader クリアの根拠を持っており、ほぼそのまま受け皿になる
- モック / brief への参照は自己完結した説明へ書き直す。例: `TimePickerColors.kt:53` は「見た目の正は承認モック (…の `--surface-derived`)」ではなく「背景に黒/白を重ねる比率。この値はデザイン確定値であり、変更すると中間面全体の明度が動く」のように、値の意味と変更時の影響で説明する
- `TimePickerColorizer.kt:240` の「部位とロールの対応は `ui/brief.md` の部位対応表を正とする」は、直後の `when` 節そのものが対応表であるため、「表に無い部位 (scrim / リップル / エラー表示) は既定のまま残す」だけを残せば意味が通る
- `TimePickerColorizer.kt:47` の裸の `ADR-0006` は、規約の許容形式 `android/ADR-0006` に揃える (同ファイル 32・42 行では既にドメイン付きで書かれており不統一)

### [🟡 Minor] 構成変更で復元されたダイアログが無着色になる

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerCellViewHolder.kt:105-109` / `TimePickerColorizer.kt:147-150`

**問題点**: `TimePickerColorizer.attach` はセルのクリック時にだけ `FragmentManager` へ登録される。画面回転などで Activity が再生成されると、`MaterialTimePicker` は新しい `FragmentManager` から復元されるが、そこには着色フックが登録されていないため、表示され続けているダイアログが Material 既定配色 (紫) に戻る。デルタスペックの Scenario は構成変更を扱っていないため spec 違反ではないが、「表示中は配色が維持される」というこの変更の目的からは外れる。

なお同じダイアログは復元後に `addOnPositiveButtonClickListener` も失う (`DatePickerCellViewHolder.kt:98` も同型) ため、**この制約は本変更が持ち込んだものではなく既存の構造に由来する**。着色だけを個別に救済すると値確定が効かないままになるので、両方をまとめて扱う方が筋がよい。

**推奨修正**: 本サイクルで直すなら `bind` 時に `fm.findFragmentByTag(tag)` で復元済みダイアログの有無を確認して再 attach する形が最小。ただしリスナー復元と併せた設計判断になるため、別変更に倒す判断も妥当 — その場合はオーナーへ「回転時にダイアログの配色と値確定が失われる (既存構造由来)」として上げ、判断を記録する。

### [🟡 Minor] キーボード入力モードの着色に単体テストが無い

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerColorizer.kt:327-342` (`applyTimeTextInput` / `applyTimeEditText`)

**問題点**: tasks 4.3 の「色マッピングの単体テスト」で、`TextInputLayout` (枠 CSL・`boxBackgroundColor`・`cursorColor`) と `EditText` (文字色・`highlightColor`) だけが検証範囲から漏れている。ここは ADR-0006 の机上確定が実機で覆った箇所 (枠の駆動源が `state_selected` ではなくフォーカス、キャレットは `TextInputLayout` 管轄) であり、`boxStrokeCsl` の状態順や `cursorColor` の当て先を将来誰かが「素直に」書き換えると、実機を見るまで気付けない形で退行する。既存テストは Material3 テーマ下で `Chip` / `MaterialButton` を生成できているため、`TextInputLayout` も同じ枠組みで検証可能 (Robolectric 制約の壁ではない)。

**推奨修正**: `TimePickerColorizerTest` に `TextInputLayout` + `TextInputEditText` の合成階層を足し、`boxStrokeColorStateList` の `state_focused` = accent / 既定 = TRANSPARENT、`boxBackgroundColor` = `accentTint`、`cursorColor` = accent、`EditText.currentTextColor` = text を検証する。

### [🟡 Minor] pre-draw 冪等再適用の分岐に単体テストが無い

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerColorizer.kt:213-235` (`colorize` の静的 1 回 / 動的毎回の分離、`styledViews`)

**問題点**: 「入力モード切替後も配色が維持される」「時刻選択の操作後も配色が維持される」の 2 Scenario を支えているのはこの分離だが、テストはいずれも `colorize` の初回呼び出ししか通っていない。実機証跡でスクリーンショットとしては担保されているものの、「静的適用は 2 回目に走らない (毎フレーム再描画を誘発しない)」「後から追加された View は次の走査で着色される」という、実機計測で得た設計上の要点が回帰テストで固定されていない。

**推奨修正**: 同じ合成階層に対し (a) `colorize` を 2 回呼び、文字盤数字の shader を 1 回目と 2 回目の間で再設定して 2 回目で再度 null になること、(b) 1 回目の後に子 View を追加し、2 回目の走査で着色されること、の 2 点を検証する。可能なら `TextInputLayout` をスパイして `setBoxStrokeColorStateList` が 2 回目に呼ばれないことまで押さえると、意図 (静的適用の 1 回性) が直接固定される。

### [🔵 Suggestion] 針の着色は既にハードウェアレイヤ化されている場合に適用されない

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerColorizer.kt:285-289`

**問題点**: `if (view.layerType != View.LAYER_TYPE_HARDWARE)` のガードにより、対象 View が既にハードウェアレイヤ (アニメーションの `withLayer` 等) になっている場合は ColorFilter 付き Paint が設定されず、かつ `styledViews` により再試行もされない。material 1.12.0 の `ClockHandView` は自前でレイヤ種別を変えないため現状は成立しており (実機証跡でも針・ノブ・中心ドットが着色済み)、ライブラリ更新時の追随確認項目という位置づけ。

**推奨修正**: 現状のままでよい。修正するなら、ガードを「既に自前の Paint を設定済みか」の判定に変える (フラグを持つ) 方が意図に忠実。

## オーナー判断待ちとして扱った項目 (指摘ではない)

`ui/brief.md` に記録済みの以下は、合意待ちの記録として違反判定に含めていない。レビュー観点からの補足だけ添える。

- **モックの `--text: #CC9900` と実装の `#555555`**: 実機証跡の画素値は `(85,85,85)` = `cellTitleColor` 完全一致で、デルタスペックの通常文字ロール定義どおり。モック側の hex がトークン注釈と食い違っている (`#CC9900` は `mauiTitleText` / `mauiHeaderText` の値) のが実態で、実装側の判断は spec に忠実。オーナー確認は「モックの hex を直すか、通常文字ロールの解決順を変えるか」の二択になる
- **AM/PM トグルの枠線が Material 既定のグレー**: 実機証跡でも枠線が残る。非選択セルの塗りはコメントどおりダイアログ背景が透けており (画素上、白は存在しない)、白浮きは起きていない
- **モード切替アイコンの形状差 / 選択数字のスナップ切替 / リップル・scrim・エラー表示の既定維持**: いずれも部位対応表・ADR-0006 の Consequences と整合
- **透過率 5.5% / 16% の定数化**: `TimePickerColors` の companion に集約されており場所は妥当。トークン昇格の要否は蒸留時の判断

## アクションプラン

1. **Major**: 5 ファイル 10 箇所の禁止参照コメントを書き換える (`android/ADR-0006` への置換 + 自己完結説明化)。裸の `ADR-0006` もドメイン付きへ統一
2. **Minor-2 / Minor-3**: `TimePickerColorizerTest` に (a) `TextInputLayout` / `EditText` の色マッピング、(b) `colorize` 2 回目の冪等性と遅延生成 View への追随 を追加する
3. **Minor-1**: 構成変更時の再 attach を本サイクルで入れるか、リスナー復元と併せて別変更に倒すかをオーケストレーター/オーナーで判断し、結論を記録する
4. **Suggestion**: 対応不要 (ライブラリ更新時の追随確認項目として ADR-0006 の追随リストに含める価値はある)
