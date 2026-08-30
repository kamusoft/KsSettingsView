# 目視確認の記録: add-maui-accessory-views

スクリーンショットは `screenshots/` 配下。実行環境は iOS Simulator (iPhone 17 Pro / iOS 26.0) と
Android Emulator (`emulator-5556` / 720x1280)。

## サンプル 7 項目 (tasks 7.2)

対象: `samples/maui/KsSettingsView.Sample.Maui` の「Header / Footer への View 配置デモ」。
画面上の丸数字 ① 〜 ⑦ が確認項目の番号に対応する。

| 項目 | 確認内容 | iOS | Android |
|---|---|---|---|
| 一覧 | 「MAUI 固有」区分にデモ項目があり、遷移先タイトルと文言が一致する | `sample-ios-00-menu.png` | `sample-android-00-menu.png` |
| (1) | RootHeaderView / RootFooterView が list の先頭と末尾に表示される | `sample-ios-01-top.png` (先頭) / `sample-ios-04-bottom.png` (末尾) | `sample-android-01-top.png` (先頭) / `sample-android-07-reconnect-bottom.png` (末尾) |
| (2) | Section の HeaderView / FooterView が表示され、Header View のバインド値が View を置き直さずに更新される | `sample-ios-01-top.png` (初期値) → `sample-ios-02-bind-and-text-fallback.png` (更新 1 回目) | `sample-android-01-top.png` → `sample-android-02-bind-and-text-fallback.png` |
| (3) | text と View の併存時は View 優先。View を外すと Header Text が現れ、戻すと再び View になる | `sample-ios-01-top.png` (View 表示) → `sample-ios-02-bind-and-text-fallback.png` (text へフォールバック) → `sample-ios-03-swap.png` (View へ復帰) | `sample-android-01-top.png` → `sample-android-02-bind-and-text-fallback.png` → `sample-android-03-swap.png` |
| (4) | 別インスタンスへの差し替えで色と文言が変わる | `sample-ios-03-swap.png` | `sample-android-03-swap.png` |
| (5) | 内容を 1 行 ⇔ 3 行に変えると領域の高さが追従する | `sample-ios-04-bottom.png` (1 行) → `sample-ios-05-grow.png` (3 行) | `sample-android-03-swap.png` (1 行) → `sample-android-04-grow.png` (3 行) |
| (6) | HeaderHeight = 44 の領域は 2 行目以降がはみ出して表示されない | `sample-ios-04-bottom.png` | `sample-android-05-bottom.png` |
| (7) | 同一ページインスタンスのまま Handler 切断 → 再接続で view accessory が復元し、離脱中の差し替えが反映される | `sample-ios-06-reconnect-top.png` / `sample-ios-07-reconnect-bottom.png` | `sample-android-06-reconnect-top.png` / `sample-android-07-reconnect-bottom.png` |

項目 (7) の読み方:

- Root Header View の文言が「離脱中に差し替え 1 回目」に変わっている = 切断中の変更が再接続後に反映された
- ⑦ の Header View が「1 回目の再接続後／離脱中の Handler: 切断」= pop で Handler が実際に切れていた
  (ページ側が pop 直後に `Settings.Handler is null` を読んで表示している)
- 「このページ」の値が「1 個目のページインスタンス」のまま = 新しい Page を作り直していない

## E2E (tasks 6.3)

対象: `maui/tests/KsSettingsView.MauiHost`。Root / Section × Header / Footer の 4 対象を配置し、
画面下のボタンから内容変化・固定高さ・Section 差し替え・インスタンス差し替え・View の取り外しを起こす。
入口画面の「離脱中に Root Header View を差し替え」で切断中の変更を作る。

| 確認内容 | iOS | Android |
|---|---|---|
| 4 対象すべてに View が表示され、同時指定の text は表示されない | `e2e-ios-01-initial.png` | `e2e-android-01-initial.png` |
| 内容変化の live 反映とサイズ追従 (1 行 → 3 行) | `e2e-ios-02-grow.png` | `e2e-android-02-grow.png` |
| HeaderHeight = 44 の固定高さと切り詰め。`ReplaceSection` を経ても view accessory が落ちない | `e2e-ios-03-headerheight-fixed.png` | `e2e-android-03-headerheight-fixed.png` |
| Section の可視性を落として戻しても Header / Footer の View が復元する | `e2e-ios-04-section-visibility-restored.png` | `e2e-android-04a-section-hidden.png` / `e2e-android-04b-section-restored.png` |
| 別インスタンスへの差し替え | `e2e-ios-05-swap.png` | `e2e-android-05-swap.png` |
| View を null にすると text へフォールバックする | `e2e-ios-06-text-fallback.png` | `e2e-android-06-text-fallback.png` |
| 同一 SettingsView インスタンスの切断 → 再接続で全 view accessory が復元し、離脱中の Root Header View 差し替えが反映される | `e2e-ios-07-reconnect.png` | `e2e-android-07-reconnect.png` |

不具合は検出されなかった。

## E2E (tasks 6.3) の再実行記録 — 2026-08-12

facade (`maui/KsSettingsView.Maui/`) の修正後に同じホストで再実行した。スクリーンショットは
`screenshots/rerun/` 配下。実行環境は iOS Simulator (iPhone 17 Pro / iOS 26.0) と
Android Emulator (`emulator-5556` / 720x1280)。

| 確認項目 | iOS | Android |
|---|---|---|
| 4 対象すべてに View が表示され、同時指定の text は表示されない | `e2e-ios-01-initial.png` | `e2e-android-01-initial.png` |
| バインド値変更の live 反映と自動高さ追従 (1 行 → 3 行) | `e2e-ios-02-grow.png` | `e2e-android-02-grow.png` |
| HeaderHeight 44 で固定高さと切り詰め。`ReplaceSection` を経ても view accessory が落ちない | `e2e-ios-03-headerheight-fixed.png` | `e2e-android-03-headerheight-fixed.png` |
| HeaderHeight を自動へ戻すと 3 行の高さが復帰する | `e2e-ios-03b-headerheight-auto-restored.png` | `e2e-android-03b-headerheight-auto-restored.png` |
| Section の可視性 off → on で Header / Footer の View が復元する | `e2e-ios-04a-section-hidden.png` / `e2e-ios-04b-section-restored.png` | `e2e-android-04a-section-hidden.png` / `e2e-android-04b-section-restored.png` |
| Header View を別インスタンスへ差し替える | `e2e-ios-05-swap.png` | `e2e-android-05-swap.png` |
| Header View を取り外すと text へフォールバックする | `e2e-ios-06-text-fallback.png` | `e2e-android-06-text-fallback.png` |
| 取り外した元インスタンスを再設定すると同じ View が内容ごと復帰する | `e2e-ios-06b-same-instance-restored.png` | `e2e-android-06b-same-instance-restored.png` |
| 同一 SettingsView インスタンスの切断 → 再接続で全 view accessory が復元し、離脱中の Root Header View 差し替えが反映される | `e2e-ios-07-reconnect.png` | `e2e-android-07-reconnect.png` |

全項目が両 OS で期待どおりに表示され、前回の記録との表示差分はなかった。実行中の iOS のコンソールログと
Android の logcat にアプリ由来の例外・警告は出ていない。

ビルド環境のメモ:

- iOS は `DEVELOPER_DIR=/Applications/Xcode-26.1.1.app/Contents/Developer` を指定してビルドする
  (Microsoft.iOS 26.1 SDK は Xcode 26.1 を要求し、既定の Xcode 26.5 ではビルドが止まる)
- Android は `dotnet build` の増分判定で `_BuildApkFastDev` が省略され、facade の変更が APK にも端末側の
  アセンブリにも反映されないことがある。`bin/.../*-Signed.apk` と `obj/.../android/bin/*.apk` を消してから
  `-t:Install` を実行して作り直す
