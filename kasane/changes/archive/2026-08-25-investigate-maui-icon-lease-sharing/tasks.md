# Tasks: investigate-maui-icon-lease-sharing

## 1. probe 実測 (ゲート)

- [x] 1.1 iOS で `UIImage.FromBundle` 経由の 2 回解決が同一インスタンスを返すかを probe で実測する。`ReferenceEquals` (managed 参照) と native handle の**双方**を記録し、共有表のキーをどちらにすべきかの根拠も得る。対象環境 (iOS バージョン・Simulator/実機)・asset 種別 (asset catalog / bundle 直下 png / 拡張子なしファイル名)・反復数を exploration.md に記録する
- [x] 1.2 ゲート判定: **再現しなければ 2〜3 は実施せず、デルタスペック (specs/maui-cells) を撤回して probe-only の調査として閉じる**。その場合、非再現の記録 (環境・asset 種別・反復数) と残余リスク (未検証環境で共有が起きる可能性) の受容をオーナー確認のうえ exploration.md に明記する (4 は probe 結果に関わらず実施)

## 2. iOS の画像単位共有防御 (probe 再現時のみ)

- [x] 2.1 プロセス全域で共有する画像単位参照カウント機構 (static、UI スレッド契約下) を platform 非依存の純ロジックとして `maui/KsSettingsView.Maui/Internals/` に追加する。キーは probe の結果 (1.1) に基づき managed 参照または native handle で確定する。不変条件: 各リースの破棄は冪等 / カウント underflow を起こさない / カウント 0 で保持していた全後片付け口を各 1 回実行しエントリを除去する / 後片付け口の例外時もカウント整合を壊さない (例外の伝播方針をテストで固定) (→ Requirement: 共有 platform 画像の後片付け安全性)
- [x] 2.2 iOS の `KsImageResolver` に配線する (解決結果の handle を参照カウント経由に包む)。Android の `KsImageResolver` は変更しない。配線が実際に効いていることは probe ハーネス上で確認する (platform 自動テストの導入可否は実装フェーズで判断) (→ Scenario: 非共有画像は従来どおり直ちに後片付けされる)
- [x] 2.3 `KsSettingsController.StoreIcon` を変更し、再解決が表示中と同一の画像インスタンスを返した場合は旧リースを退役キューに積まずその場で解放する (配信は従来どおり行わない) (→ Scenario: 同一 Cell の再解決で新旧リースが同一画像を包む)

## 3. テスト (probe 再現時のみ)

- [x] 3.1 テスト足場の整備: 共有 registry と複数 disposer を明示的に注入・観測できる形にする。`FakeImageResolver.CompleteTracked` は既に同一 `object` を複数依頼へ渡せるため、追加すべきは「registry を通したリース生成」の経路と、`GatewayScope.Reconnect()` で **resolver インスタンスを差し替えられる**経路
- [x] 3.2 参照カウント機構の単体テスト (net10.0) を作成する。不変条件 (冪等・underflow なし・カウント 0 で全後片付け実行とエントリ除去・例外時の整合) を固定する (→ Scenario: 最後のリースの破棄で後片付けが実行される / 非共有画像は従来どおり直ちに後片付けされる)
- [x] 3.3 controller レベルの共有シナリオテストを作成する: 同一 Cell の再解決 (即時解放) / 解決口の世代交代をまたいだ共有 (2 つの resolver インスタンス) / 2 つの Cell の共有 / 異なる SettingsView (別 controller) 間の共有 / 控えられない解決結果 (追い抜かれた解決・旧世代の解決・登録解除済み Cell への解決) の即時破棄 (→ Scenario: 上記の各 Scenario、および Requirement 本文の SettingsView 間保護)

## 4. 付随確認 (probe 結果に関わらず実施)

- [x] 4.1 完了済み `fix-maui-icon-lease-disposal-ordering` の順序保証が Android でも成立することの確認。成立条件: 「facade のリース破棄要求が native 配信呼び出しの完了後にのみ起きる (既存の順序固定テスト 4 本) こと」+「`MauiCustomTarget` の後片付けは要求時点より後ろへ送られることはあっても先行しないこと」。ローカルソース検証 (exploration.md「ローカルソースによる再検証」) で `MauiCustomTarget.post()` はメインスレッド上なら同期実行と判明済みのため、残る確認は「facade のリース破棄が UI スレッド以外から走る経路が無い」ことのコード確認のみ。**不成立と判明した場合は実装を止めてオーナーへ提示する** (本 change への修正同梱か別 change 起票かの判断)。結論と根拠を exploration.md に記録する
