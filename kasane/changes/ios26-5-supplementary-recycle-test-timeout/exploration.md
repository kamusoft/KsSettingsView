# Exploration: ios26-5-supplementary-recycle-test-timeout

## 課題 / 動機

- **事象**: iOS のテスト 3 アサーションが「画面外へ送った header supplementary が回収される」系の待機で 3.0 秒 deadline を超過して落ちた。`KsSettingsViewBridgeTests.KsBridgeAccessoryViewTests.test_リサイクルを挟んだ再表示が失敗しない` (2 アサーション) と `KsSettingsViewUITests.AccessoryViewDetachDiagnosticTests.test_退役した旧viewは明示的に剥がすまでview階層に残る`
- **発見の文脈**: change `ios-swiftui-representable-safe-area` の実装 (2026-09-19) で、spike 用に作成した iOS 26.5 の iPhone 17 Pro Simulator (個体) で全テストを実行した際に検出。同日の iOS 26.1 (iPhone 17 Pro) では全件 0 失敗。両テストターゲットは `KsSettingsViewSwiftUI` に依存しておらず、当該 change とは無関係と切り分け済み

### 発見時の実測 (2026-09-19)

| 項目 | 値 |
|---|---|
| Bridge テストの待機 | 経過 3.011 秒 / deadline 3.000 秒、実測: 先頭 header が差し込んだ `ProbeView` を載せたまま返り続けた |
| UI 診断テストの待機 | 経過 3.006 秒 / deadline 3.000 秒、実測: header supplementary 表示中 = true |
| Bridge バンドル全体の所要時間 | 13.127 秒 (全件) / 10.452 秒 (バンドル絞り込み)。いずれも 2 failures |
| 同日の iOS 26.1 での Bridge バンドル | 6.794 秒 / 7.482 秒、0 failures |

### 再調査の実測 (2026-09-26、iOS 26.5)

| 実行 | 結果 |
|---|---|
| 対象 2 クラスの絞り込み実行 (iPhone 17 Pro 標準個体) | Bridge 14 / UI 6 件、0 failures |
| 対象 2 クラスの絞り込み実行 (発見時と同じ spike 個体) | Bridge 14 / UI 6 件、0 failures |
| 全件実行 (iPhone 17 Pro 標準個体) | 1116 件 / 0 failures (Bridge 173 / Core 88 / SwiftUI 112 / TestSupport 7 / UI 736)。Bridge バンドルは 7.193 秒 |

2 個体の Simulator 設定 (言語・ロケール・文字サイズ) に差は見当たらなかった。

### 見立て (修正後)

起票時の見立て「iOS 26.5 の UIKit が可視矩形外の supplementary を保持し続ける挙動差」は取り下げる。発見時の実行機は Bridge バンドルが通常の約 2 倍遅く、今日の 26.5 では同じ個体でも再現しない。

`fix-ios-cell-recycle-test-flaky` (2026-09-07) と同じ型と読む。画面外へ出た部品を UIKit がいつ手放すかは UIKit の保持方針と実行機の速さで決まり、テストはその返り値 (`supplementaryView(forElementKind:at:)` が nil を返すこと) を「画面外へ出た」ことの代理に待っていた。handbook `cross/test-execution.md` の「何を待機条件に選ぶか」(framework が決める値を待機条件の代理にしない) にそのまま該当する。

26.5 と遅い実行機の組み合わせで出やすいのか (ランタイム差も効いているのか) は切り分けきれていない。前例の CI (Bridge バンドル 34.6 秒の遅い実行機) では supplementary 側の待機は通っていた。どちらであっても直し方は変わらない。

### 同型の箇所の横断確認

`ios/Tests` で「消える」型の待機条件 (`until:` が `== nil` / 否定 / `isEmpty` / `contains`) を洗った。画面外への回収を UIKit の返り値で待っているのは失敗した 2 箇所だけ:

- `ios/Tests/KsSettingsViewBridgeTests/KsBridgeAccessoryViewTests.swift` の `test_リサイクルを挟んだ再表示が失敗しない` (待機と、直後の「前提: 先頭 header が画面外へ出ていない」アサーション)
- `ios/Tests/KsSettingsViewUITests/AccessoryViewDetachDiagnosticTests.swift` の `test_退役した旧viewは明示的に剥がすまでview階層に残る` (`isHeaderSupplementaryVisible` による待機)

同ファイル内の「section 0 header の view accessory 解除」の待機は同じ `headerAccessoryView == nil` の形だが、表示中の header をライブラリが再構成するのを待つものであり、代理にはあたらない。

## 検討した選択肢 (却下案と理由を含む)

論点1: 画面外へ送った header の待機条件を何に置き換えるか

| 案 | 実行機の速さ・環境で揺れないか | 使い回し後の再設定を必ず通るか | handbook 規約との整合 | 判定 |
|---|---|---|---|---|
| A header の位置が表示範囲と交わらないことを待つ | 揺れない | 通らない回がある (遅い実行機で UIKit が抱えたままなら同じ実体の再表示で済む) | 規約の直し方そのもの | **採用** |
| B 使い回しの発生を待つ (差し込んだ view が旧 header から外れた印) | 使い回しの時期は UIKit が決めるため同じ誤失敗が残る | 必ず通る | 規約に反する | 却下 |
| C 待機の deadline (3.0 秒) を延ばす | 発見時は 3 秒間ずっと成立しておらず、延ばしても保証がない | 現状と同じ | 前例でも却下済み | 却下 |

A の弱み (使い回しの経路を通らない回がある) は、通常の速さでは UIKit が header を手放している (2026-09-26 の実行で回収待ちが成立している) ため、経路の検証は実際には継続すると判断した。

**2026-09-26 再決定 (独立レビュー review-001 の実測による)**: 上の判断の根拠は、旧来の待機が RunLoop を約 40 周 (約 0.46 秒) 回していた副作用だった。位置判定の待機はレイアウト 1 回で成立するため、通常の速さでも header は一度も回収されず (10/10)、戻したときはスクロール前と同じ実体の再表示になる。再バインドを壊すミューテーションに対する検出力は 0 だった (旧来の待機では 3/3 で検出)。論点1 を次の案で決め直した。

| 案 | 環境で揺れないか | 再設定の経路を必ず通るか | handbook 規約との整合 | 判定 |
|---|---|---|---|---|
| D 画面外にある間にテスト自身が header の作り直しを起こし、戻したときにスクロール前と別の実体へ同じ view が載ることを待つ | 揺れない (時期をテストが決める) | 通る | 適合 | **採用** |
| B′ 位置判定の後に回収待ちの段を残す | 回収の時期は UIKit 次第で発見時の誤失敗が残る | 通る | 反する (却下済みの B に近い) | 却下 |
| A′ 経路の検証を諦め、テスト名とコメントを実態に合わせる | 揺れない | 通らない (Bridge 経路では画面外の間に view が外れる信号も出ないことを実測済みで、兄弟テストも代わりにならない) | 適合 | 却下 (再設定経路の回帰を検出するテストがなくなる) |

論点2: 修正の前後をどう確かめるか

| 案 | 修正前の失敗との比較 | 他作業への影響 | 判定 |
|---|---|---|---|
| A 通常の全件実行 + 待機が即座に成立することの確認 | しない (新しい条件はテストが設定したスクロール位置と header の位置だけで決まり、UIKit の保持に依らないことを構造で担保する) | なし | **採用** |
| B CPU 負荷をかけて修正前の失敗を再現し、同条件で修正後の通過を確かめる | 再現する保証がない (発見時は 26.5 + 遅い状態の組み合わせ) | 並行セッションの Simulator を巻き込む恐れ | 却下 |

## 決定事項

- 画面外の判定に `supplementaryView(forElementKind:at:) == nil` を使わず、先頭 header の位置 (レイアウト上の frame) が表示範囲 (`collectionView.bounds`) と交わらないことで判定する (論点1 = A)
- Bridge テストの「前提: 先頭 header が画面外へ出ていない」アサーションも同じ位置判定に揃える
- UI 診断テストの画面外判定 (`isHeaderSupplementaryVisible` の否定) も位置判定へ置き換える。先頭へ戻したときの「再表示される」待機は表示の出現を待つものであり、変えない
- **(2026-09-26 再決定で追加)** 位置で画面外を確かめたあと、画面外にある間にテスト自身の操作で先頭 header の作り直しを起こす。先頭へ戻したときは「スクロール前と別の supplementary 実体に同じ view が載る」ことを再設定の遷移証拠として待つ。Bridge テストと UI 診断テストの両方に適用する (UI 診断テストの「再表示される」待機は、この遷移証拠に置き換える)
- **(同上)** 作り直しの操作が、スクロールによる使い回しと同じ製品コードの再設定経路を通ることを、再設定を壊すミューテーションで両テストが落ちることで実測する
- **(同上)** 理由コメントは観測した事実に合わせる (待機直後の header は hidden = false で保持されていた)
- 失敗時の観測値に、先頭 header の frame / hidden、表示範囲を出す (前例と同じく、次に落ちたときに切り分けられるようにする)
- 待機の deadline は既定の 3.0 秒のまま変えない
- 検証は通常の全件実行 (Simulator、実行件数併記) と、対象テストの待機が即座に成立することの確認で行う (論点2 = A)
- 変更対象はテスト 2 ファイルに限り、製品コードには触れない
- 論点3 (iOS 26.5 を CI の検証対象に含めるか) は追加作業なしで解消: CI (`.github/workflows/verify-ios.yml`) は利用可能な最新ランタイムの iPhone を自動選択するため、ランナーに 26.5 が載れば自動で対象になる

## ADR 候補 (作成済み: なし / 未起票: なし)

論点1・2 の決定は既存の handbook 規約をテストへ当てはめたもので、ADR の選別基準 (覆すコスト高 / 境界を越える / 将来を制約) に該当しない。

## 未決の論点

- なし (論点1〜3 は決定済み)

## UI 素材 (ui/references/ の一覧と注釈)

なし (テストのみの変更)

## 変更級の推奨: S (オーナー確定)

理由: iOS のテスト 2 ファイルの待機条件に閉じ、製品コード・公開 API・UI・データスキーマ・既存 ADR の決定に触れない。判定の書き換えだけで可逆。起票時の「ライブラリ側の参照保持なら M」の分岐は、発見時の実測 (UIKit が header を返し続けた) により消えた。

## 蒸留時の方針

`kasane/handbook/cross/test-execution.md` の「何を待機条件に選ぶか」節は、代理の例として `UICollectionView.cellForItem(at:)` だけを挙げている。supplementary (`supplementaryView(forElementKind:at:)`) でも同じ誤りが再発したことを受け、例を「行や header / footer が画面外へ出たことを UIKit の返り値で判定する」形へ一般化する追記を検討する。
