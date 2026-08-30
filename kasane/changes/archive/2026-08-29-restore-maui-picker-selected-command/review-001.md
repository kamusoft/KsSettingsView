# レビュー結果: restore-maui-picker-selected-command (001 回目)

**日付**: 2026-08-28
**判定**: APPROVED

## サマリー

`SelectedCommand` は OneWay・既定 null の公開 API として追加され、Native の選択確定通知による値・TwoWay バインド先の更新後に、選択モードに対応する項目を引数として1回実行される。直接 setter・同一値の再確定・`CanExecute=false`・未知 Cell ID の各境界も仕様どおりで、Critical / Major / Minor / Suggestion の指摘はない。

MAUI facade の全テストは 513 tests / 0 failures / 0 skipped、追加対象は 8 tests / 0 failures で成功した。

## 指摘事項

なし。

## 確認した観点

- 公開 API の型・既定値・BindingMode と XML ドキュメント
- 単一選択・複数選択の書き戻し、相互導出、Command 実行順
- 同一値再確定時の不要な再書き戻し抑止と完了通知の維持
- `CanExecute` を確認しない移植元互換、直接 setter・未知 Cell ID・確定通知なしでの非発火
- Native / Bridge 境界を変更せず facade 内に閉じた実装であること
- tasks.md の完了チェックと実装・テストの対応

## アクションプラン

なし。
