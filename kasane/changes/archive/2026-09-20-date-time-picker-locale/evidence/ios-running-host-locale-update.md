# iOS 実行中 Host の Locale 更新確認

## 環境

- iPhone 17 Pro Simulator / iOS 26.5
- Sample bundle: `jp.kamusoft.kssettingsview.samples.ios`
- 2026-09-20 にレビュー修正後の source から再 build / install

## OS 経路の確認結果

Sample を `-AppleLanguages (en) -AppleLocale en_US` で起動したプロセスは PID 67605 だった。
このプロセスを終了せずに `-AppleLanguages (ja) -AppleLocale ja_JP` を付けて再度 launch しても PID は
67605 のままで、表示中 Cell は `10:15 PM` のまま変化しなかった。Simulator の launch argument による
アプリ言語変更は、実行中プロセスへ Locale 更新を配送する手段にはならない。

プロセスを terminate して日本語引数で起動し直すと PID 95126 になり、同じ Cell は `10:15 午後` に
変化した。既存の `ios-time-en.png` / `ios-time-ja.png` と `ios-date-en.png` / `ios-date-ja.png` は、この
OS Locale での最終描画を担う。

## 実行中通知経路との役割分担

OS 起動引数では同一プロセスの通知到達を再現できないため、実行中 Host に
`NSLocale.currentLocaleDidChangeNotification` が届いた後の production handler は Simulator unit test で補完する。
テストでは View ごとの Locale resolver seam だけを英語から日本語へ切り替え、通知自体は production と同じ
`NotificationCenter` 経路で post する。Time / Date Wheels / Date Calendar の picker Locale、Cell の自動値、
明示 `valueText`、`is24Hour`、未確定選択の保持、Done 後の通知値を観測する。

この分担により、OS Locale での UIKit 実描画は静止画、同一 Host への通知後の状態遷移は production 通知経路を
通る自動テストが担う。
