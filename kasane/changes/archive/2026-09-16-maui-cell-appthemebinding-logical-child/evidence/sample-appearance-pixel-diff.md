# MAUI Sample: 実装前後の描画の画素比較

Sample の 7 デモ画面を light / dark の両外観で撮影し、実装前 (購読 + 再代入) と実装後 (共有 Style + AppThemeBinding) の
スクリーンショットを画素単位で比較した結果。比較対象から外したのは端末側の chrome だけ (iOS: 上端 150px のステータスバー、
Android: 上端 90px のステータスバーと下端 130px のナビゲーションバー。時計の表示が実行のたびに変わるため)。
diff_px は RGB 差の合計が 8 を超えた画素数、max は 1 チャンネルあたりの最大差。

| platform | 外観 | 画面 | diff_px | max |
|---|---|---|---|---|
| iOS | light | 基本 Cell 7 種デモ (上端) | 0 | 0 |
| iOS | light | 基本 Cell 7 種デモ (末尾 / ButtonCell) | 0 | 0 |
| iOS | light | 入力 Cell 5 種デモ | 0 | 0 |
| iOS | light | CustomCell デモ | 0 | 0 |
| iOS | light | Section 装飾デモ | 0 | 0 |
| iOS | light | Header / Footer への View 配置デモ | 0 | 0 |
| iOS | light | CustomCell の MAUI 固有デモ | 0 | 0 |
| iOS | light | MAUI 固有 Cell 機能デモ | 0 | 0 |
| iOS | dark | 基本 Cell 7 種デモ (上端) | 0 | 0 |
| iOS | dark | 基本 Cell 7 種デモ (末尾 / ButtonCell) | 0 | 0 |
| iOS | dark | 入力 Cell 5 種デモ | 0 | 0 |
| iOS | dark | CustomCell デモ | 0 | 0 |
| iOS | dark | Section 装飾デモ | 0 | 0 |
| iOS | dark | Header / Footer への View 配置デモ | 0 | 0 |
| iOS | dark | CustomCell の MAUI 固有デモ | 0 | 0 |
| iOS | dark | MAUI 固有 Cell 機能デモ | 0 | 0 |
| Android | light | 基本 Cell 7 種デモ (上端) | 0 | 0 |
| Android | light | 基本 Cell 7 種デモ (末尾 / ButtonCell) | 0 | 0 |
| Android | light | 入力 Cell 5 種デモ | 0 | 0 |
| Android | light | CustomCell デモ | 0 | 0 |
| Android | light | Section 装飾デモ | 0 | 0 |
| Android | light | Header / Footer への View 配置デモ | 0 | 0 |
| Android | light | CustomCell の MAUI 固有デモ | 0 | 0 |
| Android | light | MAUI 固有 Cell 機能デモ | 0 | 0 |
| Android | dark | 基本 Cell 7 種デモ (上端) | 0 | 0 |
| Android | dark | 基本 Cell 7 種デモ (末尾 / ButtonCell) | 0 | 0 |
| Android | dark | 入力 Cell 5 種デモ | 0 | 0 |
| Android | dark | CustomCell デモ | 0 | 0 |
| Android | dark | Section 装飾デモ | 0 | 0 |
| Android | dark | Header / Footer への View 配置デモ | 0 | 0 |
| Android | dark | CustomCell の MAUI 固有デモ | 0 | 0 |
| Android | dark | MAUI 固有 Cell 機能デモ | 0 | 0 |

全 32 組が完全一致 (diff_px = 0)。撮影環境: iOS Simulator iPhone 17 Pro (iOS 26.4)、Android Emulator Pixel 6 (API 36)。
外観は Sample のルートメニューの外観選択 (ライト / ダーク) で固定した。
