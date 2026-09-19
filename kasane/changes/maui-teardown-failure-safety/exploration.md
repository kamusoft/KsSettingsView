# Exploration: maui-teardown-failure-safety

## 課題 / 動機

(2026-09-18、change `android-accessory-view-late-insert-animation` のレビューで収束しなかった指摘群より簡易起票。出典: `kasane/changes/android-accessory-view-late-insert-animation/` の `second-opinion-code-003.md`〜`005.md`・`review-004.md`・`review-005.md`・`deviation.md`)

MAUI facade の「切断」と「画面生成の失敗時の巻き戻し」で、後片付けの途中にさらに例外が出たときの安全性が、場所ごとにばらばらになっている。上記 change では、レビュー指摘に 1 件ずつ応えるたびに 1 段深い異常系が次の指摘として現れ、4 周連続で収束しなかった。個別の穴ふさぎではなく、後片付け全体の方針として設計し直す。

**上記 change で済んだこと** (前提として読む):
- 接続の失敗・Native Host の生成完了までの失敗では、Host の世代ごと手放す (未登録の配置の実体も破棄する)
- `KsSettingsController.ReleaseHost` / `Disconnect` は、後片付けの単位を独立に試みてから例外を集約する
- 書き戻し・内容なし配信に失敗した実体は、native Host の解放が済むまで破棄しない

**残っている指摘** (未検証。レビューの指摘をそのまま写したもので、起票時点の実物確認はしていない):
- 通常切断の `SettingsViewHandler.DisconnectHandler` が逐次実行のままで、親子関係の解消が例外を投げると `ReleaseHost` と基底の切断へ進まない。`ReleaseHost` が集約例外を返した場合も基底の切断が飛ぶ (巻き戻し側 `RollbackHost` は独立に試みる形になっており、非対称)
- native Host の解放にも失敗した場合に、native が参照しているかもしれない実体を安全に再試行できる場所が無い
- `KsSettingsController` の同一ファイル内に、`TryCleanUp` と同じ趣旨の手書きの try / 集約が複数残っている。`DisposeRetiredViews` だけ集約例外を平坦化していない
- (2026-09-18 追記、出典: 同 change の `review-006.md` の申し送り) `ReleaseCellContentViews` の配信成功後の破棄ループの全件試行がテストで固定されていない (素の `Dispose()` へ戻しても全件通る)
- (同上) `ReleaseCellContentViews` / `ReleaseAccessoryViews` のリースを外すループの途中で例外が出ると、取り出し済みの実体が延期先にも破棄ループにも届かず取りこぼされる

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

## 未決の論点

- **未探索 (簡易起票)** — 上記はレビュー指摘の写しで、現状のコードとテストでの再確認・議論はしていない。探索を始めるときは実物を先に読む
- 後片付けの失敗を利用者へどう伝えるか (集約例外を投げる / 記録だけして握る)。切断は MAUI のライフサイクルから呼ばれるため、例外を投げること自体の是非が論点になり得る
- 現実に `Dispose` や native への配信が例外を投げる頻度 (ホスト側レビューは「現実性が低い」と評価)。どこまでの多重障害を保証対象にするかの線引き
- maui/ADR-0016 (寿命と退役順序) への書き足しが要るか
