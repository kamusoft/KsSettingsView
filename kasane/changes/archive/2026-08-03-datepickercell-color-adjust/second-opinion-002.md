# セカンドオピニオン: datepickercell-color-adjust (code-review、review-001 対応)
**相方**: codex / **日付**: 2026-08-02 / **対象**: claude/datepickercell-color-adjust 作業ツリーの全変更 (HEAD 22a20fd 比)
**注**: second-opinion-001.md は提案フェーズ (spec-review) の証跡。本ファイルは実装レビュー並走分。

---

# レビュー結果: datepickercell-color-adjust

**日付**: 2026-08-02  
**判定**: **CHANGES_REQUESTED**

## サマリー

提示された 1,442 件のテスト成功・lint 結果を確認済み証拠として扱いました。承認モックと 7 枚の検証画像は概ね一致していますが、状態判定の堅牢性と回帰テストに Major が 2 件あります。

## 指摘事項

### 🟠 Major: 描画色を選択状態の判定に利用している

**該当箇所**: [DatePickerColorizer.kt:383](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerColorizer.kt:383)

**問題点**: 選択・今日・通常の判定を `MaterialShapeDrawable` の「塗りや枠に可視色があるか」で行っています。これは状態ではなく、ホスト側の Material Calendar style が生成した描画結果です。

そのため、例えば以下で誤判定します。

- ホストが通常日の fill を不透明色にカスタマイズすると、通常日も選択日として扱われる。
- `DatePickerCell.accentColor = Color.Transparent` の場合、最初の走査で選択状態を認識して fill を透明アクセントへ置換した後、次の pre-draw では fill が不可視となり通常状態へ戻る。
- 通常スタイルに可視 stroke がある場合、通常日を「今日」と誤認する。

既定 Material style のスクリーンショットでは現れませんが、「任意のアクセント色」と、プラットフォーム既定色に依存しない部位対応を要求する仕様に反します。

**推奨修正**: 色とは独立した状態を保持してください。`isEnabled`・`isSelected` と、Material が再設定した Drawable の identity を用いたロールキャッシュなどで状態を判定し、自分で適用した色を次回の判定入力にしない構造にします。透明アクセント、通常日の可視 fill/stroke を含む回帰テストも追加してください。

### 🟠 Major: 実際のダイアログ統合と内部構造契約をテストできていない

**該当箇所**:

- [DatePickerColorizerTest.kt:31](android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerColorizerTest.kt:31)
- [DatePickerMaterialContractTest.kt:47](android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerMaterialContractTest.kt:47)
- [tasks.md:26](kasane/changes/datepickercell-color-adjust/tasks.md:26)

**問題点**: `DatePickerColorizerTest` は合成 View に `colorize()` を直接呼ぶため、次の主要経路を通りません。

- `attach()` と Fragment tag の照合
- `FragmentLifecycleCallbacks`
- pre-draw hook の登録・解除
- window 背景の差し替え
- 実際の MaterialDatePicker におけるモード切替・月移動

また、契約テストが検証するのはヘッダ、操作ボタン、入力欄だけです。実装が依存する月移動ボタン、`month_grid`、年選択 RecyclerView と子 View の親子関係などは未検証です。ID が残ったまま型や階層だけ変わると、合成テストも契約テストも成功したまま着色が外れます。したがって tasks 4.3・4.4 の完了チェックは現状では過大です。

**推奨修正**: `FragmentActivity` 上で実際の `MaterialDatePicker` を表示し、ライフサイクル経由で背景・代表部位・再選択・モード切替を確認する統合テストを追加してください。併せて `MaterialIds` が列挙する全必須部位について、型だけでなく親子関係も契約テストに含めてください。

### 🟡 Minor: UI brief に禁止されている実装寸法が追記されている

**該当箇所**: [ui/brief.md:80](kasane/changes/datepickercell-color-adjust/ui/brief.md:80)

**問題点**: `ui-artifacts.md` は brief に px 値・具体レイアウトを書かない規約ですが、実測 px、28/100dp、120dp などの実装寸法が追加されています。brief の部位対応表や照合結果は適切ですが、実装調査記録が混在しています。

**推奨修正**: brief にはデザイン判断・部位対応・視覚照合結果を残し、実測値と内部構造の調査結果は探索記録、テスト、コードコメントへ移してください。

## アクションプラン

1. カレンダー項目の状態判定を描画色から分離する。
2. 実 MaterialDatePicker を使った統合テストと、全内部依存の契約テストを追加する。
3. UI brief から実装寸法の調査記録を分離する。

**件数**: Critical 0 / Major 2 / Minor 1 / Suggestion 0  
**結果ファイル**: ご指定に従い作成していません。

## 突き合わせ結果 (ksn-orchestrator、2026-08-02)

ホスト側 review-001.md (CHANGES_REQUESTED / Minor 4・Suggestion 3) との突き合わせ。

| # | 相方の指摘 | 採否 | 根拠 |
|---|---|---|---|
| C1 | Major: 描画色を選択状態の判定に利用 (DatePickerColorizer.kt:383) | **採用** (Major) | 相方のみ + 根拠強。オーケストレーターがコードで裏取り: `applyCalendarItem` は fill/stroke の可視性で状態を分類し、自分が適用した accentCsl が次回 pre-draw の判定入力になる。透明アクセント (alpha 0) で選択→通常へ転落、ホスト側カスタム calendar style で通常日を選択/今日と誤認するシナリオが成立する。ホスト側レビューの見逃しとして扱う |
| C2 | Major: 実ダイアログ統合と内部構造契約の未検証 (tasks 4.3/4.4 の過大チェック) | **採用** (Major) | 相方のみ + 根拠強。テスト実態を裏取り: DatePickerColorizerTest は合成 View 階層へ colorize() 直呼びで、attach()/FragmentLifecycleCallbacks/pre-draw 登録解除/window 背景の経路が未検証。契約テストの実 inflate 検証はヘッダ・ボタン・入力欄のみ。ホスト側の非ブロッキング指摘 (ヘッダ検証の手組みレイアウト依存・背景ロールの自動テスト無し) とも部分的に同種で、相互補強とみなす。verify-001 の VALID (Scenario対応表) と矛盾はしない — 単体レベルの対応は存在し、統合経路の欠落が論点 |
| C3 | Minor: brief.md への実装寸法の混在 | **採用** (Minor) | ui-artifacts.md の「書かないもの: px 値・生カラー値・具体レイアウト」に合致する規約裏付けあり (好み・スタイルの域ではない)。実測値・内部構造の調査記録は brief から実装側 (コードコメント / 変更内ノート) へ移す |

- 双方一致 (完全一致) の指摘: なし。C2 とホスト側 Minor 2 件が部分重複
- 降格・未解決: なし
- ホスト側のみの指摘 (H1 ハードキャスト / H2 冪等性回帰テスト欠落ほか) は review-001.md のとおり同格で修正サイクルへ
