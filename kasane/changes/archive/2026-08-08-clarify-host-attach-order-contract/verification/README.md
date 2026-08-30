# 検証ホスト E2E の証跡

検証ホスト (`maui/tests/`) を「Host 生成 → 操作 → 取り付け」の自然な順序に戻したうえで、
起動直後の表示を撮影した記録。期待される表示は `maui/README.md` の一覧と同じ。

実行環境:

- iOS: iPhone 17 Simulator (iOS 26.1) / `DEVELOPER_DIR=/Applications/Xcode-26.1.1.app/Contents/Developer`
- Android: 実機 Pixel (adb serial `0B261JEC216142`)

| ファイル | 条件 | 結果 |
| --- | --- | --- |
| `01-ios-before-fix-natural-order.png` | iOS Host の view load 時の Store pull を外したビルド | Section header が「通知」のまま (accessory 更新が消える)、ストレージの `replaceSection` が未反映 (「同期」行と footer が無い) |
| `02-ios-after-fix-natural-order.png` | 現行ビルド | 期待どおり。事前操作後の Section / Cell 列・theme・Root header / footer がすべて表示される |
| `03-ios-root-accessory-without-view-load.png` | 現行ビルド。window 表示だけを行い view の読み込みを確定させずに Root accessory を適用した場合 | Section / Cell 列と theme は復元されるが、Root header / footer は表示されない |
| `04-android-after-fix-natural-order.png` | 現行ビルド | 期待どおり。iOS と同じ表示になる |

01 で残っている更新のうち Cell 内容の反映は、`replaceCells` の経路が内容更新の前に Store の
現在状態を取り込み直すことによる副次的なもので、Store pull による復元とは別の経路である。

03 は、Root header / footer が復元の対象外であり、所有者が **view の構築後に** 適用する必要が
あることを示す。取り付け (window の表示) だけでは view はまだ構築されておらず、この時点の
Root accessory 更新は反映されない。
