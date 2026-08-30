# phase-7-drag-sort

AiForms の UseDragSort 相当の D&D 並べ替えを、AiForms 互換ではなく Native 起点で再設計して MAUI まで通す。

## 論点

- Native の並べ替え UI と契約 (iOS: UICollectionView reorder / Android: ItemTouchHelper 等) の設計
- 並べ替え確定時の Store への反映経路 (moveCell / moveSection との接続、Diff 発行タイミング)
- 並べ替え可否の宣言形状 (Section 単位 / Cell 単位のフラグ設計)
- 並べ替え結果の通知契約 (maui/ADR-0003 の単一 delegate/listener 集約との整合)
- MAUI API の形と ItemsSource 連動 (テンプレ生成 Cell の並べ替えをソースコレクションへ書き戻すか)

## 決定事項

(議論で確定したらここに移動)

## TODO

- [ ] 論点の解消
- [ ] ksn-propose で変更提案を起こす
