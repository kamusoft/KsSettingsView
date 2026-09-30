# 検証結果: add-scroll-control (002 回目)

**日付**: 2026-09-30
**判定**: VALID

**対象**: verify-001.md で ❌ とした 2 件の解消を確かめる再検証。❌ の原因はどちらも tasks.md 6.3 の未了 (❌-1) だった。対応表は verify-001.md を引き継ぎ、この回で状態が変わった行だけを更新して載せる。ほかの行 (Scenario 97 件のうち 95 件) は verify-001.md のとおりで変わらない。

## サマリー

tasks.md 6.3 にチェックが付き、tasks.md の全 40 件が完了した。追加された `evidence/` の画像 10 枚を開き、各画像が 6.3 の観測点と一致することを確かめた。

- iOS の「Section へ」: 修正後のビルドで撮り直され、Native と MAUI iOS の両方で到着と一方向の着地を確認できた
- Android の着地の連続フレーム 3 か所: 3 か所とも一方向に進んで止まっていた

verify-001 の ❌ 2 件は解消した。新しい ❌ は無い。コード (ライブラリ・テスト・Bridge・facade) は verify-001 の時点から変わっていない。以上から VALID と判定した。

## 事前条件の再検査

| 検査 | 結果 | 根拠 |
|---|---|---|
| 逆流 (足場の書き換え) | ✅ なし | `proposal.md`・`design.md`・`specs/` に HEAD からの差分は無い。`tasks.md` の差分は 40 行で、すべて `[ ]` → `[x]` だけ (`--word-diff` で確認)。6.1〜6.4 はすべて `[x]` |
| tasks.md の未完了 | ✅ なし | 全 40 件にチェックが付いている (verify-001 の ❌-1 が解消) |
| tasks.md の虚偽チェック | ✅ なし | 6.3 は、下記「追加の証跡の照合」で証跡の実物を確かめた |
| verify-001 以降のコードの変更 | ✅ 変更なし | 作業ツリーの変更ファイル (`ios/`・`android/`・`maui/`・`samples/`) のうち、verify-001.md より後に更新されたのは `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/ScrollControlDemoScreen.kt` の 1 本だけだった。全文を読み、verify-001 で読んだ内容と一致することを目視で照合した。上向きの連続フレームの撮影に使った一時的な操作 (画像の見出しに「一時ボタン」とある) は、ファイルに残っていない (「一時」の文字列も無い)。更新時刻だけが変わった形で、文言・構成・命令は verify-001 で突き合わせたものと同じである |
| テストの全件成功 | ✅ | ライブラリ・テストのコードが変わっていないので、verify-001 の実行結果を引き継ぐ: Android 3146 tests / 0 failures (本検証で実行)、MAUI 624 / 0 (本検証で実行)、iOS 1184 / 0 (ホスト側の報告)、iOS のテストのビルド成功 (本検証で確認) |
| 未記録乖離 | ✅ なし | この回の差分は tasks.md のチェックと `evidence/` の追加だけで、新しい乖離は無い |

## 追加の証跡の照合

画像はすべて開き、観測点と一致するかを見た。連続フレームは、フレームごとの縦の移動量の推定 (Δ) の符号と、行の並びの進み方で判定した。

| 観測点 | 証跡 (`evidence/`) | 観測した内容 | 結果 |
|---|---|---|---|
| iOS Native「Section へ」の到着 (修正後のビルド) | `ios-native-fixed-4-section-to-start.png` | 「Section 4」の見出しがナビゲーションバーの直下 (表示範囲の上端) にある | ✅ |
| iOS Native「Section へ」の着地 (修正後のビルド) | `ios-native-frames-section-to-start-fixed.png` | 先頭から下向き。Δ はすべて 0 か負で、累計 −3784 で止まり、以後 0 が続く。途中で逆向きに戻るフレームは無い。最終フレームは到着の静止画と同じ位置 | ✅ |
| MAUI iOS「Section へ」の到着 (修正後のビルド) | `maui-ios-fixed-4-section-to-start.png` | 「Section 4」の見出しがナビゲーションバーの直下にある | ✅ |
| MAUI iOS「Section へ」の着地 (修正後のビルド) | `maui-ios-frames-section-to-start-fixed.png` | 先頭から下向き。Δ はすべて 0 か負で、累計 −3718 で止まる。大きいタイトルが縮む区間でも逆向きに戻らない | ✅ |
| MAUI Android「末尾へ」の到着 | `maui-android-fixed-2-scroll-to-end.png` | 最下部に Root Footer「ここが内容の末尾です（Root Footer）」が見える (内容の末尾) | ✅ |
| MAUI Android「末尾へ」の着地 | `maui-android-frames-scroll-to-end-fixed.png` | 行の繰り返しで移動量の推定が折り返すため、画像内で数値は省かれている。行の並び (項目 1-x → 5-x → 先頭へ戻る → Root Footer) が単調に下へ進み、Root Footer が見えた位置で止まっていることを目視で確かめた。上へ戻るコマは無い | ✅ |
| MAUI Android「Cell を中央へ」の到着 | `maui-android-fixed-5-cell-to-center.png` | 「項目 3-3」が表示範囲の中央にある | ✅ |
| MAUI Android「Cell を中央へ」の着地 (下向き) | `maui-android-frames-cell-center-down-fixed.png` | Δ はすべて 0 か負で、減速しながら累計 −2310 で止まる | ✅ |
| Android Native「Cell を中央へ」の着地 (上向き、末尾から) | `android-native-frames-cell-center-up-fixed.png` | Δ はすべて 0 か正 (上向き) で、減速しながら累計 +1807 で止まる | ✅ |
| Android Native「Cell を中央へ」の到着 (末尾から) | `android-native-fixed-7-cell-to-center-from-end.png` | 「項目 3-3」が表示範囲の中央にある | ✅ |

## 対応表 (更新した行)

verify-001.md の「samples-ios / samples-android / samples-maui」表のうち、状態が変わった行と注記を更新した行を載せる。

| Requirement / Scenario | 実機観測 (`evidence/`) | verify-001 | verify-002 |
|---|---|---|---|
| samples-ios / 各操作で対象の位置へ移る | 修正後のビルドで 4 操作がそろった: 末尾へ `ios-native-fixed-2-scroll-to-end.png`、Section へ `ios-native-fixed-4-section-to-start.png` (着地は `ios-native-frames-section-to-start-fixed.png`)、Cell を中央へ `ios-native-fixed-5-cell-to-center.png`・`ios-native-fixed-7-cell-to-center-from-end.png`、先頭へ `ios-native-fixed-3-scroll-to-start-noanim.png` | ❌ | ✅ |
| samples-maui / 各操作で対象の位置へ移る | Android: `maui-android-fixed-2-scroll-to-end.png`・`maui-android-4-section-to-start.png`・`maui-android-fixed-5-cell-to-center.png`・`maui-android-3-scroll-to-start-noanim.png`。iOS (修正後のビルド): `maui-ios-fixed-2-scroll-to-end.png`・`maui-ios-fixed-4-section-to-start.png`・`maui-ios-fixed-5-cell-to-center.png`・`maui-ios-fixed-3-scroll-to-start-noanim.png` | ❌ | ✅ |
| samples-android / 各操作で対象の位置へ移る | verify-001 の証跡に加え、上向きの中央合わせの到着と着地 `android-native-fixed-7-cell-to-center-from-end.png`・`android-native-frames-cell-center-up-fixed.png` | ✅ (連続フレームの未了は ❌-1 に含めていた) | ✅ |
| settings-view-android-ui / 一方向の着地 / 中央・末尾合わせのアニメーションが行き過ぎて戻らない | verify-001 の証跡に加え、MAUI Android の下向き `maui-android-frames-cell-center-down-fixed.png`・`maui-android-frames-scroll-to-end-fixed.png`、Native の上向き `android-native-frames-cell-center-up-fixed.png` | ⚠️ (deviation 記録済み) | ⚠️ (deviation 記録済み。変わらず) |
| settings-view-ios-ui / 一方向の着地 / 中央合わせのアニメーションが行き過ぎて戻らない | verify-001 の証跡に加え、修正後のビルドの Section の下向き `ios-native-frames-section-to-start-fixed.png`・`maui-ios-frames-section-to-start-fixed.png` | ⚠️ (deviation 記録済み) | ⚠️ (deviation 記録済み。変わらず) |

集計: Scenario 97 件のうち、✅ または ⚠️ (deviation 記録済み) が 97 件で、❌ は 0 件。verify-001.md の注 1〜3 はそのまま引き継ぐ。どれも一致と判定した上での注記である。

## verify-001 の ❌ の解消状況

| # | 内容 | 状況 | 根拠 |
|---|---|---|---|
| ❌-1 (a) | Android の着地の連続フレーム 3 か所 | 解消 | MAUI Android の「末尾へ」下向き・「Cell を中央へ」下向き、Android Native の「Cell を中央へ」上向きの連続フレームと到着の静止画を照合した。3 か所とも一方向で止まっている |
| ❌-1 (b) | iOS の「Section へ」の修正後のビルドでの撮り直し | 解消 | Native と MAUI iOS の両方で、修正後のビルドの到着の静止画と連続フレームを照合した。見出しが上端に来て、一方向で止まっている |
| (a)・(b) に伴う Scenario の ❌ 2 件 | samples-ios「各操作で対象の位置へ移る」、samples-maui「各操作で対象の位置へ移る」 | 解消 | 上の対応表の更新行 |
| tasks.md 6.3 の未完了 | — | 解消 | 6.3 に `[x]`。tasks.md の差分はチェックの更新だけ |

## 申し送り (判定に含めない)

- 長命層 (decisions / handbook / concepts) への反映は、proposal.md と deviation.md にある「蒸留時に反映」の行による蒸留時の申し送りで、不一致としていない (verify-001 と同じ)
- iOS のテスト実行と Swift 6 言語モードの確認は、制約によりホスト側の報告に依っている (verify-001 と同じ)
