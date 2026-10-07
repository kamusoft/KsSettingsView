# レビュー結果: android-compose-scroll-restore-with-multiple-hosts (002 回目)

**日付**: 2026-10-07
**判定**: APPROVED

## サマリー

review-001.md の指摘 4 件への対応を確かめた。指摘 2 と 4 はテストの修正で解消し、指摘 1 と 3 は未確認の点として deviation.md に内容どおり記録されている。ライブラリ本体とサンプルに前回の確認以降の変更は無く、Android の全件テストは 3188 件 / 失敗 0 件で通る。残る指摘は無い。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always)
- `kasane/handbook/cross/test-execution.md` (テストを実行するとき・結果を報告するとき)
- `kasane/handbook/cross/runtime-behavior-verification.md` (スクロール制御の位置の控えと戻しを変更して完了を判定するとき)
- `kasane/lessons/code-review.md` (L-001)・`kasane/lessons/process.md` (L-009)

ロードしたスキル: ksn-review / kotlin-impl-skill / jetpack-compose-impl-skill / csharp-impl-skill / maui-skill

## 確認した観点

**前回の指摘への対応**

| 指摘 | 対応 | 確認の結果 |
|---|---|---|
| 1 (Minor) テストの補助の根拠 | 対応せず、deviation.md に未確認として記録 | 記録は指摘の内容を正しく表している (下記) |
| 2 (Minor) 「自身が根」の判定を外しても落ちない | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ScrollRecreationTest.kt:205` にテストを追加 | 解消。改変で落ちることを実測した (下記) |
| 3 (Minor) Android の端末での Activity の作り直し | 対応せず、deviation.md に未確認として記録 | 記録は指摘の内容を正しく表している (下記) |
| 4 (Suggestion) 使っていない観測 | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/compose/ComposeScrollRecreationTest.kt` の見張りのテストを整理 | 妥当 (下記) |

- 指摘 2: 追加のテストは `listOf(HostItself, null, null)` の構成で、自身が根の View Host が位置へ戻り、直置きの 2 つは復元が積まれず内容の先頭を表示することを確かめる。範囲の根を親からたどる形へ改変すると (前回の改変 C。リポジトリの外の写しで実施)、このテスト 1 件だけが落ちる (32 件中 1 件。自身が根の View Host の復元を待つ箇所で `processed=0` のまま待機切れ)。改変なしでは通る。直置きの 2 つの「何も起きない」の確認は、負の検証用と名前で分かる既存の `drainMainLooperForUnchangedCheck` を通しており、cross/ADR-0027 の形に合う
- 指摘 4: `ancestorCount` と `AncestorObservation` を削り、控えるのは外れる時点の境目の View と、外れた後に保存を求められた回数だけになった。「外れた後に保存が求められる」ことは前提のアサーションとして残り、外れる回数は `listOf(holder)` との比較で 1 回と確かめている。使われない観測は残っていない
- 指摘 1・3 の記録 (deviation.md の「決定事項 論点 2『端末での確かめ』で未確認のまま残した点」の行): (a) は、補助の名前・端末で確かめたのは最終の見え方であること・レイアウト要求が残らないことは直接観測していないことを書いており、指摘 1 と一致する。(b) は、端末で 2 つの画面が重なった状態からの Activity の作り直しを修正後のビルドで観測していないこと、テストで確かめた形を書いており、指摘 3 と一致する。どちらもリリース後に起票元のアプリで確かめる項目に加えるとしている

**意図しない変更の有無**

- `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt` は、前回の確認時に控えた内容と SHA-1 が一致する (更新時刻だけが新しい)
- 前回の確認時点より後に内容が変わったソースは、テスト 2 ファイル (`ScrollRecreationTest.kt`・`ComposeScrollRecreationTest.kt`) だけ。`samples/` の 2 ファイルと `DateCalendarRecreationTest.kt`・`kssettingsview-bridge` は変わっていない
- exploration.md の更新時刻は前回の確認より前のままで、書き換えられていない

**実行した検証**

- Android 全件: `./gradlew test --rerun-tasks` (JDK 21) → BUILD SUCCESSFUL。`kssettingsview` 1401 件 × 2 variant、`kssettingsview-bridge` 193 件 × 2 variant、合計 3188 tests / 0 failures / 0 errors (前回から 1 件 × 2 variant の増)。既知の間欠の待機切れは出なかった
- `samples/` とライブラリ本体は前回から変わっていないため、サンプルのコンパイルと端末の証跡との対応は前回の確認を引き継ぐ

**設計品質・コメント**

- 追加・変更したテストのコメントは現在形で、作業文書への参照は無い

## 指摘事項

なし

## 所見 (指摘ではない)

- deviation.md に未確認として残った 2 点 (補助の根拠、端末での Activity の作り直し) と、起票元の表の 3 つめの場面 (タブ) は、リリース後に起票元のアプリで確かめる項目として残る
- コードとテストのコメントが参照する android/ADR-0023 は proposed のまま (前回と同じ)

## アクションプラン

なし (蒸留へ進められる)
