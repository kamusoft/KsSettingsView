# deviation

合意済みスコープ・指示との差分を 1 行ずつ記録する。

- Simulator の録画は iOS 26.1 ではなく **iOS 26.5** の iPhone 17 で撮った (比較元の archive は 26.1)。26.1 の Simulator には画面操作に使う agent が入っておらず、入れるには環境への追加が要るため。今回の録画は修正後の退行確認 (内容の後着と高さの変動が無いこと) であり、修正前のフレームとの画素比較はしていないので、OS 版の差は判定に影響しない
- 実機・Simulator の A/B ログの取得と probe のために、`maui/KsSettingsView.Maui/Platforms/iOS/KsAccessoryHostView.cs` へ一時的な観測ログを 2 度入れて 2 度とも外した (`shasum` が元と一致することを確認済み)。`ios/Sources/KsSettingsViewBridge/KsBridgeCellContentView.swift` へは、追加したテストが退行を検出することを確かめるため一時的に旧実装を戻して 1 度実行し、同じく元へ戻した (`shasum` 一致)。いずれも git 操作は使っていない (L-009)
- `maui/` 側 (wrapper の幅の解決と計測の控えの破棄) には自動テストを足していない。facade のテストプロジェクトは素の `net10.0` であり、`Platforms/iOS/` のコードを参照できる載り場所が無いため。代わりに実機のログで A/B を取り、内容の変化への追従 (展開・折りたたみ) まで確認した
