# Proposal: investigate-maui-icon-lease-sharing

## Why

MAUI facade の icon 実体化 (maui/ADR-0015) では、複数の `KsImageLease` が同一の platform 画像インスタンスを包み得る。探索調査 (exploration.md「調査結果」、dotnet/maui 10.0.70 実ソース) で、破棄の意味論が OS で真逆であることが確定した:

- **Android**: 破棄 = Glide 参照カウントのデクリメント。リース1つ = 参照1つで、片方の破棄は他方に無害。破棄はむしろ必須 (スキップは即リーク)
- **iOS**: 破棄 = `UIImage.Dispose()` の一発破壊。参照カウントなし。`UIImage.FromBundle` フォールバック (asset catalog 画像等) でのみ同一 UIImage が複数リースに入り得て、片方の破棄が他方の表示を壊す

守るべき穴は iOS の1経路のみ。ただし FromBundle が同一 managed peer を返す点は実ソースからの推定で未実測のため、防御実装の前に probe で実証する。

## What Changes

1. **probe 実測 (ゲート)**: iOS 環境で `UIImage.FromBundle` 経由の2回解決が同一インスタンスを返すことを実証する (`ReferenceEquals` と native handle の双方を記録し、共有表のキー確定の根拠も得る)。**再現しなければ 2〜3 は実施せず、デルタスペックを撤回して probe-only の調査として閉じる** (非再現の記録要件と残余リスク受容は tasks 1.2)
2. **iOS の画像単位の共有防御**: 同一 platform 画像を包む複数リースが存在する場合、最後のリースの破棄まで実際の後片付けを遅延する参照カウント機構を追加する。所有は**プロセス全域の static** (解決口の世代交代・SettingsView 間の共有もカバー、UI スレッド契約下)。本体は platform 非依存の純ロジックとして `Internals` に置き (maui/ADR-0009 のテスト戦略に乗せる)、iOS の `KsImageResolver` だけが配線する。Android の破棄経路は現行維持 (変更しない)。複数 disposer の所有規則: 各解決結果の後片付け口は共有表が保持し、カウント 0 で全てを各1回実行する (iOS の後片付け口は全て同一 UIImage への `Dispose()` であり、同一 managed peer への多重 `Dispose()` は初回以降 no-op のため、実画像への効果は1回)
3. **同一画像への再解決時の即時解放**: `StoreIcon` を変更し、再解決が表示中と同一の画像インスタンスを返した場合は旧リースを退役キューに積まずその場で解放する (表示内容が変わらないため配信は従来どおり不要。各リースの後片付け口は独立しており、共有時は 2 の機構が防御する)
4. **テスト**: 共有 registry と複数 disposer を明示注入・観測できる足場を整え (resolver インスタンス差し替えを含む)、共有シナリオ (同一 Cell の再解決・世代交代跨ぎ・Cell 間共有・控えられない解決結果の即時破棄) で「片方の破棄が他方の表示を壊さない」ことを固定する
5. **付随確認**: 完了済み `fix-maui-icon-lease-disposal-ordering` の順序保証が Android でも成立することを、明示した成立条件 (tasks 4.1) に照らして確認し、結果を exploration.md に記録する (ローカルソース検証で `MauiCustomTarget.post()` はメインスレッド上なら同期実行と確認済み — 成立見込み)

影響する能力: maui-cells (IconSource の実体化と反映)

## Non-Goals

- **facade 全体の所有権モデル見直し** — 探索で却下済み (穴が1経路に絞られたため過剰装備)
- **Android GIF の同一 GifDrawable 複数添付問題** — refcount 上は安全で、アニメーション状態の共有は Glide 側の既知の性質。現時点で GIF icon の利用実績・要望がなく、独立に探索すべき別課題
- **review-002 の誤り所見 (iOS file/font の破棄 Action null) のアーカイブ訂正** — アーカイブは歴史資料として凍結。訂正情報は本 change の exploration.md が正となり、蒸留時に concepts / ADR へ反映する

## Impact

- 破壊的変更なし (公開 API 変更なし、facade 内部のみ)
- リスク: 参照カウント機構は検証可能な不変条件 (各リース破棄の冪等性・underflow なし・カウント 0 での全後片付け実行とエントリ除去・例外時のカウント整合) を単体テストで固定する。二重解放 (表示破壊) 方向には倒さない
- probe 不成立の場合はコード変更ゼロで終了 (デルタスペック撤回、残余リスク受容を記録)

## 級: M

作業面が複数 (probe / 防御機構 / Fake 拡張 / 付随確認) で probe 結果による分岐を持つが、公開 API 変更なし・可逆性高。

domain: maui
