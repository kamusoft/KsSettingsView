# 検証結果: datepickercell-color-adjust (001 回目)

**日付**: 2026-08-02
**判定**: VALID

デルタスペック `specs/cell-types-input/spec.md` の全 Requirement / Scenario と実装・テストの対応を突き合わせた。
❌ (未記録の欠落・乖離) は 0 件。虚偽チェックなし、足場の逆流なし、テストは全件成功。

パスはすべてリポジトリルート (`android/ks-settingsview-ui/src/...` は `main/kotlin/jp/kamusoft/kssettingsview/ui/` /
`test/kotlin/jp/kamusoft/kssettingsview/ui/` を省略表記) 基準。

## 対応表

### Requirement: DatePickerCell の日付選択ダイアログはテーマ配色を反映する (Android)

色ロールの割当 (Requirement 本文の 4 つの箇条書き):

| 色ロール | 実装 | テスト | 状態 |
|---|---|---|---|
| 背景 ← `Theme.backgroundColor` | `DatePickerColorizer.kt:175-184` (window 背景の `MaterialShapeDrawable.fillColor` 差し替え / `InsetDrawable` unwrap は `:458-465`)、色束は `DatePickerCellViewHolder.kt:85-89` | `DatePickerColorizerTest.kt:403-418` (色束の解決)。window への適用自体の自動テストは無く `ui/verification/01-calendar.png` が証跡 | ✅ 一致 |
| 強調 ← 解決済みアクセント | 選択日/選択年 `DatePickerColorizer.kt:383-400`、今日/今年の枠 同 `:393-396`、OK/キャンセル `:85-88, 263-264`、入力欄の枠・キャレット `:100-103, 429-436` | `DatePickerColorizerTest.kt:76-90` (OK/キャンセル)、`:124-140` (枠・キャレット)、`:171-181` (選択日)、`:184-195` (今日)、`:221-235` (年グリッド) | ✅ 一致 |
| 通常文字 ← 実効タイトル文字色 | 汎用 TextView 分岐 `DatePickerColorizer.kt:281`、ヘッダタイトル `:304-310`、選択日テキスト `:255-257`、モード切替アイコン `:259-261`、年月/月送り `:415-419`、入力文字 `:439-442` | `DatePickerColorizerTest.kt:66-73`、`:93-102`、`:105-121`、`:143-152`、`:198-207` | ✅ 一致 |
| アクセント上文字 ← 実効面とのコントラストで白黒自動 | `PickerDialogColors.onAccent` (`PickerDialogColors.kt` の派生色)、適用は `DatePickerColorizer.kt:71, 390` | `PickerDialogColorRolesTest.kt:14-75` (黒白自動選択・境界・半透明の実効面判定)、適用は `DatePickerColorizerTest.kt:179-181, 230-231` | ✅ 一致 |

適用範囲の限定 (「表示セッション内。Activity/構成の再生成をまたぐ復元後は対象外」) は、フックが Fragment 破棄で
自己解除される実装 (`DatePickerColorizer.kt:145-155`) と一致する。復元後の再着色は実装されておらず、これは
Requirement 本文および proposal Non-Goals のとおり。

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| テーマ色の反映 | `DatePickerColorizer.kt:233-283` (全走査 + 部位判定)、attach は `DatePickerCellViewHolder.kt:128-132` | `DatePickerColorizerTest.kt:66-235` (代表部位 9 件)、`ui/verification/01,04,05` | ✅ 一致 |
| 入力モード切替後も配色が維持される | pre-draw フックによる再走査 `DatePickerColorizer.kt:196-205, 233-243` | `DatePickerColorizerTest.kt:259-273` (後から現れた入力欄が次の走査で着色される)、`ui/verification/05-text-input.png` | ✅ 一致 |
| カレンダー操作後も配色が維持される | 同上 (月移動・年選択で再生成される View)、年セル判定 `:369-373` | `DatePickerColorizerTest.kt:221-235` (年グリッド)。月移動そのものの自動テストは無く `ui/verification/03-calendar-month-moved.png` が証跡 | ✅ 一致 |
| 日付を選び直しても配色が維持される | `DatePickerColorizer.kt:383-400` (ライブラリの塗り戻しから状態を読み直して再適用) | `DatePickerColorizerTest.kt:237-257` (旧選択日/新選択日の入れ替え)、`:184-218` (今日・無効日)、`ui/verification/02-calendar-reselected.png` | ✅ 一致 |
| アクセント上の文字の可読性 | `PickerDialogColors.onAccent` / `ColorRoles` | `PickerDialogColorRolesTest.kt:14-75` (半透明アクセントを背景へ合成した実効面での判定を含む) | ✅ 一致 |

### Requirement: DatePickerCell のアクセント色は Cell 固有値を先頭に解決される (Android)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Cell 固有値の優先 | `DatePickerCellViewHolder.kt:81-89` (`cell.accentColor ?: effective.accentColor`) | `DatePickerColorizerTest.kt:362-376` | ✅ 一致 |
| CellStyle 値へのフォールバック | 同上 (`EffectiveStyle` の既存解決) | `DatePickerColorizerTest.kt:378-388` | ✅ 一致 |
| Theme 値へのフォールバック | 同上 | `DatePickerColorizerTest.kt:390-400` | ✅ 一致 |

### Requirement: 日付選択ダイアログのヘッダはタイトルと選択日の両方が読める (Android)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 日本語タイトルと日本語日付の同時表示 | ベースライン復元 `DatePickerColorizer.kt:304-310, 335-338`、選択日の縮小 `:322-341, 344-355` | `DatePickerColorizerTest.kt:277-291` (重なり + 自領域内)、`:294-309` (寸法復元)、`:311-320` (0.8 倍)、`:322-337` (幅に収める追加縮小)、`DatePickerMaterialContractTest.kt:65-88` (寸法と前提の固定)、`ui/verification/01, 07` | ✅ 一致 |
| 日付選択後もヘッダが崩れない | `DatePickerColorizer.kt:285-291` (選択日テキストを動的側で再調整) | `DatePickerColorizerTest.kt:339-355` | ✅ 一致 |

保証構成 (縦・横、端末既定フォント倍率) のうち横向きは `ui/verification/07-calendar-landscape.png` で確認。
2 行折り返しとなるが重なり・クリップは無い (brief 末尾に記録あり)。

## 追加検査

- **tasks.md の虚偽チェック**: 16 タスクすべて `[x]`。1.1〜1.3 のスパイク結果は `ui/brief.md`「実装中の発見」に、
  2.1 は `TimePickerColors` → `PickerDialogColors` のリネーム (`git status` の R 判定) に、3.x / 4.x は上表に、
  5.1 は `ui/verification/` の 7 枚に、5.2 は lint 実行 (`PrivateResource` の抑制は
  `DatePickerColorizer.kt:59` / `TimePickerColorizer.kt:46` の 2 ヘルパのみ、lint 指摘 0 件) に対応。**虚偽なし**
- **逆流検査**: `git diff HEAD` の範囲で `proposal.md` / `specs/cell-types-input/spec.md` に変更なし。
  `tasks.md` はチェックボックスのみ、`ui/brief.md` は末尾への追記のみ (既存記述の書き換え・削除なし)。**逆流なし**
- **未記録乖離**: ❌ が無いため該当なし。部位対応表のうち「ヘッダ下の区切り線」は実装されていないが、
  material 1.12.0 のダイアログ表示では当該 View が常に GONE で着色対象の実体が無いことが `ui/brief.md`
  「部位対応表への補足」に記録済み → ⚠️ 記録済みの差分として扱う (deviation.md ではなく brief.md での記録)。
  「タッチ時のリップル」「エラー表示」「scrim」は対応表で対象外と定義されたとおり未着色 (`:277-278` で明示的に除外)
- **UI 変更の記録**: `ui/brief.md:71-78` に承認モック (variant-a、2026-08-02 オーナー承認、A 案確定) の記録あり。
  実装中の方針変更 (CJK 大フォント仮説の棄却 → 寸法リソースからのベースライン復元) と、モックとの構造差
  (ヘッダ内の縦余白) が「照合結果」に記録済み
- **テスト実行**: `./gradlew :ks-settingsview-ui:testDebugUnitTest --rerun` を実行。33 クラス / failures 0 / errors 0。
  本変更の 3 クラスは `DatePickerColorizerTest` 23 件、`DatePickerMaterialContractTest` 4 件、
  `PickerDialogColorRolesTest` 22 件、いずれも成功
- **lint**: `./gradlew :ks-settingsview-ui:lintDebug` 成功。指摘 17 件はすべて既存ファイル
  (build.gradle.kts / EntryCellViewHolder.kt / KsSimpleCheckView.kt / KsWheelView.kt / SheetChrome.kt) で、
  本変更の追加・変更ファイルに対する指摘は 0 件

## 判定理由

全 Requirement / Scenario が「✅ 一致」。虚偽チェック・逆流・未記録乖離・テスト失敗のいずれも無いため **VALID**。

なお、以下は一致検証としては ✅ だが証跡が自動テストではなく実機スクリーンショットのみである点を付記する
(品質評価は review-001.md 側で扱う):

- 背景ロールの window への適用 (`DatePickerColorizer.kt:175-184`)
- 月移動後の再着色 (Scenario「カレンダー操作後も配色が維持される」の月移動部分)
