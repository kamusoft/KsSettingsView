# Deviation: fix-ios-tapnotifyingrenderer-actor-isolation

実装フェーズで生じた、デルタスペック / tasks の記述と実装の差分を記録する。

- tasks 3.2 (`disconnectStore()` 側の保守注意書きの更新): tasks では注意書きが `disconnectStore()` 側にある前提だったが、実際には「購読を増やすときは両方に追記すること」の記述は `deinit` ブロック内のコメントにのみ存在した。tasks 3.1 の deinit 削除で当該コメントごと解消したため、`disconnectStore()` 自体は無変更 (doc コメントに deinit への言及は元から無い)。代わりに実態と矛盾する購読プロパティの doc コメント 3 件 (「deinit で cancel する」→ 解放時に自動解除) を更新した。理由: 注意書きの実在箇所が tasks の想定と異なっていたため (2026-09-01)
