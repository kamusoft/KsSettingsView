# phase-10-template-virtualization

ItemsSource / ItemTemplate の仮想化 (リサイクル対応)。データ量が多いと MAUI View (CellBase) オブジェクトが大量生成されるオリジナル AiForms 由来の問題を解消する。

## 論点

- 大量 ItemsSource 時の CellBase 生成を遅延・リサイクルする方式 (可視範囲のみ実体化する仕組みの設計)
- Native 側リサイクル (RecyclerView / UICollectionView) との協調 — CellBase を介さず native cell へ直接バインドする案の成立性
- cellId ↔ CellBase 対応表・Diff 変換経路 (phase-2 決定の二層方式) との整合
- BindingContext 再割当の意味論とエコー抑止 (同値チェック) への影響
- (phase-5 からの申し送り 2026-08-12) CustomCell の `ContentTemplate` プロパティと content 値駆動の template 再実体化 — phase-5 は「参照が正・内容は live + 世代トークン」(maui/ADR-0020) を採り template 前提の設計を本フェーズとセットで再考すると先送りした。CustomCell は行数分の live View が常存する (仮想化なし) ため、大量行の扱いも本フェーズの射程。行リサイクルで View を載せ替える場合は iOS の行ごとの入れ物 + 引き取り規則・Android の key(token) 埋め込み (concepts/maui/architecture/view-materialization.md) との整合が前提

## 決定事項

(議論で確定したらここに移動)

## TODO

- [ ] 論点の解消
- [ ] ksn-propose で変更提案を起こす
