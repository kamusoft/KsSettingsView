# verification: settingsview-migration-defects

修正後の実行時確認 (cross/runtime-behavior-verification.md) の記録。修正前の証跡は exploration.md 論点 2 の実測表を参照。

## 論点 1: 暗黙 Style でのクラッシュ (MAUI)

- 手段: MauiHost (`maui/tests/KsSettingsView.MauiHost/App.cs`) に暗黙 Style (`HeaderTextColor` = マゼンタ、`CellBackgroundColor` = `#FFF6CC`、`ListStyle` = `Modern`) を一時的に追加し、iOS Simulator (iPhone 17 Pro / iOS 26.0) で起動。一時変更は確認後に復元済み (差分なし)
- 結果: 落ちずに設定画面が開き、Cell が淡黄・「バインド Section」見出しがマゼンタ・Section が Modern の角丸カードで描かれた。「落ちないが Style の色が効かない」側も解消
- 証跡: `evidence/maui-implicit-style-ios-after.png`
- 修正前の再現はユニットテスト側の A/B で代替 (`ImplicitStyleTests` 7 件が修正前コードで全件失敗)

## 論点 2: 通常遷移でのスクロール位置 (Android)

- 手段: MAUI Sample の Header / Footer デモページに「子ページへ Push」ボタンを一時的に追加し (リポジトリの Sample には存在しない。確認後に削除済み)、Pixel 6a 実機で最下部までスクロール → 子ページを Push → 戻る
- 結果: 位置が完全に保たれる。修正の復元処理だけを外したビルドでは同じ操作で先頭に戻る (A/B 成立)
- 証跡: `evidence/android-maui-sample-normal-nav-01-after-scrolled-bottom.png` (Push 前) / `-02-after-child-page-pushed.png` / `-03-after-back-position-kept.png` (戻った後) / `-04-probe-restore-removed-reset-to-top.png` (復元処理を外した対照)。-01 と -03 はバイト同一で、位置が保たれた結果として同じ画面になっている
- 修正前と同じ導線 (ColorAnalyzer の通常遷移) でなく MAUI Sample を使った点は deviation.md に記録
