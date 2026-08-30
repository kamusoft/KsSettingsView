# セカンドオピニオン: timepickercell-color-adjust (002 — 対応するホスト側レビュー: review-001.md)
**相方**: codex / **日付**: 2026-08-02 / **対象**: 実装 diff (HEAD d410495 に対する作業ツリー) — code-review モード
(注: second-opinion-001.md は提案フェーズの spec-review 証跡のため、本ファイルは 002 を採番)
---
# レビュー結果: timepickercell-color-adjust

**日付**: 2026-08-02  
**判定**: **CHANGES_REQUESTED**

## サマリー

主要な色ロール、アクセント解決順、モード切替後の再適用は実装・テスト・実機証跡で確認できます。一方、Activity 再生成時に配色が失われる問題と、半透明アクセントで可読色判定が逆転する問題があります。

指摘件数: Critical 0 / Major 3 / Minor 0 / Suggestion 0

## 指摘事項

### [🟠 Major] Activity 再生成後に配色フックが復元されない

**該当箇所**: [TimePickerCellViewHolder.kt:108](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerCellViewHolder.kt:108)、[TimePickerColorizer.kt:117](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerColorizer.kt:117)

**問題点**: 色情報と `FragmentLifecycleCallbacks` は、その場で生成された `TimePickerColorizer` のメモリ上にしか保持されません。ダイアログ表示中に画面回転などで Activity が再生成されると、`MaterialTimePicker` 自体は saved state から復元されますが、Colorizer は復元されません。その結果、再生成された View には Material 既定配色が戻り、pre-draw による維持処理も失われます。

合成 View の単体テストと現在の5枚の証跡はいずれも Activity 再生成を通っていないため、この問題を検出できません。

**推奨修正**: 色ロールと対象識別子を Fragment の arguments/saved state に保存し、復元された `MaterialTimePicker` にも再着色と pre-draw hook を設定できるライフサイクル経路を設けてください。Activity recreation 後も背景・文字盤・入力モードの配色が維持されるテストも追加してください。

### [🟠 Major] 半透明アクセントではコントラスト判定が実描画色と一致しない

**該当箇所**: [TimePickerColors.kt:31](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerColors.kt:31)、[TimePickerColors.kt:103](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerColors.kt:103)、[TimePickerColorRolesTest.kt:44](android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerColorRolesTest.kt:44)

**問題点**: `contrastingBlackOrWhite` はアルファを無視し、`blend` も `top` 色自身のアルファを無視しています。公開 API の `Color` に不透明色限定の契約はなく、spec も任意のアクセント色を対象としています。

例えば白背景に `0x40000000` のアクセントを指定すると、実描画面は明るいグレーになりますが、現在の判定は RGB 部分の黒だけを見て白文字を選びます。実際には黒文字の方が大幅に高コントラストです。現在の「アルファは判定に影響しない」テストは、この誤動作を仕様として固定しています。

**推奨修正**: アクセントをダイアログ背景へアルファ合成した実効面色に対して黒／白を比較してください。派生面の合成でも `top` の固有アルファとロールの透過率を掛け合わせ、正しい source-over 合成を行ってください。半透明の黒・白・有彩色を使った回帰テストも必要です。

### [🟠 Major] オーナー承認状態の記録が依頼前提および同一文書内で矛盾している

**該当箇所**: [brief.md:79](kasane/changes/timepickercell-color-adjust/ui/brief.md:79)、[brief.md:81](kasane/changes/timepickercell-color-adjust/ui/brief.md:81)、[brief.md:89](kasane/changes/timepickercell-color-adjust/ui/brief.md:89)

**問題点**: レビュー中に `brief.md` へ「オーナー確認・承認済み」という行が追加されました。しかし依頼時の明示前提は「合意が必要な妥協点はオーナー確認待ち」であり、直後の見出しも「要オーナー確認」「合意が必要な妥協点」のままです。承認証跡として相互に両立しません。

**推奨修正**: 実際にオーナー確認待ちであれば、承認済みの記述を削除してください。確認が完了した場合のみ、その事実に合わせて見出し・各妥協点の状態を一貫して更新してください。

## アクションプラン

1. Activity/Fragment 再生成後の配色復元経路とテストを追加する。
2. 半透明色を正しく合成してコントラストを判定し、誤った既存テストを修正する。
3. `brief.md` の承認状態を事実に合わせて統一する。
4. 修正後に既存621件、lint、回転を含む視覚確認を再実施する。

指定どおりレビュー結果ファイルへの書き込みは行っていません。

## 突き合わせ結果 (ホスト側判定、2026-08-02)

| # | 指摘 | 出典 | 採否 |
|---|---|---|---|
| 1 | コメント規約違反 10 箇所 (Major) | ホストのみ | **確定** — 機械的書き換えで修正 |
| 2 | 回転 (Activity 再生成) 後の無着色 | **双方一致** (ホスト Minor-1 / 相方 Major-1) | **確定・重要度は高い方 (Major)**。ただし既存構造由来 (positive listener も同時に失われる、DatePicker 同型) のため、本 change で最小修正するか別変更に倒すかは**オーナー判断へ** |
| 3 | 半透明アクセントでコントラスト判定が実描画と逆転 (相方 Major-2) | 相方のみ + 根拠強 (具体的失敗例あり) | **採用** — 背景への alpha 合成後の実効色で黒/白判定するよう修正 + 回帰テスト。既存の「アルファ非依存」テストは仕様を誤って固定していたため書き換え |
| 4 | brief.md の承認状態が文書内で矛盾 (相方 Major-3) | 相方のみ | **採用 (文書修正)** — オーナー承認はレビュー開始後にチャットで実際に得られており承認自体は事実。見出しを承認済み状態に統一 |
| 5 | キーボード入力モードの単体テスト不足 (ホスト Minor-2) | ホストのみ | **採用** — TextInputLayout/EditText の色マッピングテスト追加 |
| 6 | pre-draw 冪等再適用のテスト不足 (ホスト Minor-3) | ホストのみ | **採用** — 2回目呼び出しの冪等性・遅延生成 View 追随のテスト追加 |
| 7 | 針の layerType ガード (ホスト Suggestion) | ホストのみ | **降格 (対応不要)** — ライブラリ更新時の追随確認項目として蒸留時に ADR へ含める判断 |

- 矛盾する指摘: なし。降格: #7 のみ。未解決: #2 のスコープ判断 (オーナーへ)

### 追記: 指摘 #2 (回転問題) のオーナー決定 (2026-08-02)

**別変更に切り出す**ことで確定。理由: 回転時にはダイアログの配色だけでなく `addOnPositiveButtonClickListener` も失われる (値確定が効かない・DatePickerCell も同型の既存構造問題) ため、着色だけの救済は中途半端になる。リスナー復元と着色復元を一体で設計する独立変更として起こす。本 change のデルタスペックは構成変更を扱っておらず spec 違反ではないため、deviation 記録は不要。

## 相方の修正確認 (turn 2, 2026-08-02)

修正サイクル 1 後の再確認を同一セッションで実施。**総合判定: APPROVED** (raw: ~/.kasane/counterpart-bridge/responses/so-code-timepickercell-color-adjust-2.md)

- 指摘 #3 半透明アクセント: **解消** (accentSurface による実効色判定と回帰テストを確認。白背景 + 0x40000000 → 実効 #BFBFBF → 黒文字)
- 指摘 #4 brief 承認状態: **解消** (軽微な文言不一致の指摘のみ → その場で修正済み)
- 指摘 #2 回転問題: 本変更の指摘としては解消。独立変更 fix-picker-dialog-recreation の起票スタブを失わず追跡することが条件
- 補足 (対応不要の認識): `blend` は背景不透明の前提 — KDoc の限定は将来の整理余地として申し送り
