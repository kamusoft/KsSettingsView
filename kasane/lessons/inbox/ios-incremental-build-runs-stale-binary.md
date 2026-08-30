---
scope: impl
kind: pain
severity: normal
count: 2
first-seen: 2026-08-12
last-seen: 2026-08-19
evidence:
  - add-maui-custom-cell (iOS bridge の Swift を変更後、samples の通常 `dotnet build -f net10.0-ios` では xcframework は再生成されるのにアプリ実行ファイルが再リンクされず、修正担当ワーカーの「修正前後とも欠陥再現せず」の実測が全て stale binary 上の観測だった。`-t:Rebuild` で強制リンクして初めて反映され、堅牢化変更が実は行高さ追従を壊す回帰 (SHALL 違反) を持っていたことが後続の再検証で発覚)
  - add-accessory-visibility-toggle (MAUI Android サンプルの目視検証で、Debug 構成が Fast Deployment のため `adb install` した apk 単体では新デモを含まない古い実行内容が表示され、アンインストール後は `No assemblies found … Fast Deployment` で SIGABRT。`dotnet build -f net10.0-android -t:Install` で配備し直して初めて最新コードが載った。検証ワーカーが誤所見を出す直前に気づき自力回復)
---

## ルール文

MAUI サンプルを Simulator / 実機で検証するときは、実行前に**最新コードが実際に配備されたことを確認**する。stale な実行内容上の「再現しない」「直った」「表示されない」は判断材料にならない。
- iOS: native (Swift) 変更後、通常の `dotnet build` は xcframework の変化をアプリの増分リンク判定が拾わないことがある。`obj/.../nativelibraries/<App>` の mtime とサイズを確認し、疑わしければ `-t:Rebuild` を挟む
- Android: Debug 構成は Fast Deployment でアセンブリを apk に埋めないため、`adb install` した apk 単体では古い内容が走る (または `No assemblies found` で SIGABRT)。配備は `dotnet build -f net10.0-android -t:Install` (または `EmbedAssembliesIntoApk=true`) で行う

## 経緯

- 2026-08-12 add-maui-custom-cell: iOS スクロール欠落の修正検証が stale binary 上で行われ、「修正前でも再現せず」という無効な観測から堅牢化維持の裁定が一度下った。再検証ワーカーがリンク成果物の mtime 比較で罠を発見し、強制リビルド後に回帰 (行高さ追従の破壊) と欠陥の残存の両方が確定した。
