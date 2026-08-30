# レビュー結果: investigate-maui-icon-lease-sharing (001 回目)

**日付**: 2026-08-25
**判定**: CHANGES_REQUESTED

## サマリー

デルタスペックの 6 Scenario と Requirement 本文の SettingsView 間保護はいずれも実装とテストで押さえられており、ミューテーション probe でテストの回帰検出力も確認できた (後述「確認した観点」)。iOS 配線の実測証跡も存在し、提出コードと対応している。一方で、共有表が預かる後片付けの口が**再解決のたびに増え続け、プロセス全域の static に無制限に溜まる**経路を実測で確認した (Major)。デルタスペックに違反はしないが、本変更が新たに持ち込んだリソース保持であり、既存テストでは一切観測されない。

指摘は Critical 0 件 / Major 1 件 / Minor 1 件 / Suggestion 3 件。

## ビルド・テスト実行結果

| 対象 | 結果 |
|---|---|
| `maui/KsSettingsView.Maui.Tests` (net10.0) | **462 件成功 / 0 失敗 / 0 スキップ** |
| `maui/KsSettingsView.Maui` net10.0-ios | **0 エラー / 0 警告** |
| `maui/KsSettingsView.Maui` net10.0-android | **0 エラー / 0 警告** |
| `python3 scripts/comment-policy-lint.py` | 禁止 0 件 (検査対象 677 ファイル) |
| `python3 scripts/local-path-lint.py` / `identity-lint.py` | 検出なし |

コンテキストパッケージが客観的事実として提示した数値 (462 件 / 警告 0) をレビュー側で再実行して一致を確認した。

## 指摘事項

### [🟠 Major] 共有画像の後片付けの口が再解決のたびに蓄積し、解放されないまま増え続ける

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsSharedImageRegistry.cs:57-61` (`Acquire` の `entry.Disposers.Add`) / 同 `:75-111` (`Release`)

**問題点**:

`Acquire` は呼ばれるたびに `entry.Disposers` へ後片付けの口を追加するが、`Release` はカウントを減らすだけで、**手放されたリースに対応する口をリストから取り除かない**。取り除かれるのはカウントが 0 になってエントリごと消えるときだけである。

ところが iOS で共有が起きる画像 (asset catalog / 拡張子なし名 — probe が同一インスタンスと実証した種別) では、Host 再接続のたびに同じ `UIImage` が返り、`StoreIcon` が旧リースを即時解放するためカウントは 1 に戻る。**カウントは 1 のままなのに `Disposers` だけが 1 回の再接続につき 1 件ずつ伸び続ける**。エントリが消えるのはその画像を使う Cell が全て居なくなったときだけなので、設定画面を開き直すたびに `IImageSourceServiceResult` が static に溜まる。

実測 (レビュー側で一時 probe テストを作成し、計測後に撤去):

```
再接続 50 回後: probes=51 aliveLeases=1 disposedBefore=0 → Cell 除去後 disposedAfter=51
```

生きているリースは 1 本なのに、51 個の後片付け口 (と、それが握る参照) がプロセス全域の static に保持されたままだった。1 件あたりの大きさは小さいが上限がなく、既存テストは `TrackedImageCount` (画像の数) しか見ていないため、この増加は 1 件も観測されない。

デルタスペック本文には違反しない (「保持されていた**全ての**後片付け口」が 0 で走ればよい) が、本変更前は結果オブジェクトはリースと 1:1 で解放されており、蓄積は存在しなかった。新規に持ち込んだ保持である。

**推奨修正**: 次のいずれか。仕様文言 (「保持されていた全ての後片付け口」) は保持する集合を減らす方向の実装を禁じていないため、デルタスペックの改訂なしで対処できる。

1. `Acquire` の時点で、その画像が既に別の口によって生かされている場合、新しい結果を `Disposers` に積まずその場で手放す設計にする (iOS では冗長な `result` の後片付けは「既に預かっている口」と同じ `UIImage.Dispose()` に帰着するため、代表 1 件で足りる)。ただしこれは「同一画像に対する後片付けは等価」という platform 前提を `KsSharedImageRegistry` に持ち込むので、その前提を doc コメントで明示すること
2. 口を `Handle` 側に持たせ、手放された口はエントリから外して即座に (共有中でも安全な形で) 始末できるかを検討する
3. 上記いずれも取れないと判断する場合は、蓄積が想定内であることと上限の根拠をオーナーへ提示して合意する (この場合は deviation ではなく設計判断の記録が要る)

いずれの対処でも、「同一画像への再解決を N 回繰り返しても保持される口が増えない」ことを固定するテストを `SharedImageRegistryTests` へ追加してほしい (現状この不変条件を守るテストが 1 本もない)。

### [🟡 Minor] 後片付けの例外伝播が、退役キューの残りのリースを取りこぼす

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsSharedImageRegistry.cs:107-110` / `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs` の `DisposeRetired`

**問題点**: `Release` は集約した失敗を `AggregateException` で呼び出し元へ投げる (tasks 2.1 が求めた「例外の伝播方針」の選択であり、それ自体は妥当)。しかし `DisposeRetired` は退役キューを先に空にしてから `foreach` で破棄するため、途中で例外が抜けると**残りのリースは二度と破棄されない** (キューからは既に消えている)。1 リースの破棄が N 個の口をまとめて実行するようになったぶん、この経路が発火する面は本変更で広がっている。

同様に、`StoreIcon` の即時解放と iOS `KsImageResolver.ResolveAsync` の `completed(lease)` 経路も `AggregateException` を素通しするため、`_ = ResolveAsync(...)` の未観測例外になり得る (`completed` が呼ばれず icon が反映されないまま止まる)。

例外を投げる後片付けは異常系であり本変更前から同じ形ではあるが、伝播を「方針」として明示的に選んだ以上、受け側の耐性も一緒に決めておくのが筋だと考える。

**推奨修正**: `DisposeRetired` のループを 1 件ごとに保護し (失敗を集めて最後にまとめて投げる)、少なくとも「1 件の失敗で他のリースを取りこぼさない」ことをテストで固定する。数行で閉じるため本サイクル内での対処を推奨する。

### [🔵 Suggestion] `Release` の防御分岐が到達不能で、コメントが実在しない状況を説明している

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsSharedImageRegistry.cs:77-81`

**問題点**: `Handle` は `_released` で冪等化されており、かつ必ず自分を発行した表を参照する。エントリが消えるのはカウント 0 のとき (= その画像の全 `Handle` が解放済み) だけなので、「エントリが無いのに `Release` が呼ばれる」状態は構成できない。コメントの「表の外から破棄された場合に限られる」は、この型の設計では起こり得ない状況を説明しており、ファイル単独で読んだときに読み手を誤らせる (comment-policy の「そのファイルだけを読んでいる人にとって意味が通る」に照らして)。

**推奨修正**: 防御分岐自体は残してよい。コメントを「到達しない想定の防御。万一到達してもカウントを動かさない」といった、実際の役割を述べる文へ書き換える。

### [🔵 Suggestion] 2 つの証跡で実行環境の表記が食い違っている

**該当箇所**: `exploration.md` の「probe 実測: iOS の同一 UIImage 共有」(iOS 26.0.1 / `maui/spike/app/KsBindingSpikeApp`) と `evidence/ios-wiring-before-after.txt` (iOS 26.5 / `maui/tests/KsSettingsView.MauiHost`)

**問題点**: 同日の 2 つの実測が別の Simulator ランタイム・別ハーネスで行われている (どちらのランタイムも手元に実在することは確認済みで、記録として矛盾はしない)。ただし evidence 側が「同一インスタンスであることは tasks 1.1 の probe で実証済み」と参照しているため、読み手には「同じ環境で連続して測った」ように読める。

なお evidence の baseline 列 (配線なしで handle が `0x0` になる) は、その環境でも共有が実際に起きていたことを自力で示しているため、結論の妥当性には影響しない。両ハーネスの一時改造が撤去済みであることは `git status` で確認した (`maui/spike/` `maui/tests/` に差分なし)。

**推奨修正**: evidence の見出しに「1.1 とは別ランタイム (26.5) での計測。共有の発生は baseline 列が同環境で示している」旨を 1 行足すと、後から読む人が環境を取り違えない。

### [🔵 Suggestion] iOS 配線が `KsSharedImageRegistry.Shared` を使うことは自動テストで固定されていない

**該当箇所**: `maui/KsSettingsView.Maui/Platforms/iOS/KsImageResolver.cs:47`

**問題点**: `IconSharingTests` は `FakeImageResolver.CompleteShared` 経由で共有表を通すため、**iOS の実配線を外しても net10.0 テストは全件通る**。maui/ADR-0009 の TFM 構成による既知の限界であり、tasks 2.2 も「probe ハーネス上で確認」と定めていたとおり `evidence/ios-wiring-before-after.txt` が代替になっている (レビュー側もこれを判定条件として実在と内容を確認した)。

**推奨修正**: 対処不要。将来 platform テストの導線が入ったときの候補として記録に残す。

## 確認した観点 (指摘に至らなかったもの)

- **`StoreIcon` の条件分岐の等価性**: 新旧を全 4 組合せ (previous/lease の null 有無 × 画像同一性) で突き合わせ、共有ケース以外の挙動 (退役キュー投入・`MarkContentDirty` の発火) が変わっていないことを確認した
- **テストの回帰検出力** (lessons code-review L-001 のミューテーション probe):
  - `StoreIcon` の共有ケース分岐を変更前の形へ戻す → **3 件失敗**
  - `KsSharedImageRegistry.Release` の遅延判定を無効化する → **14 件失敗**
  - いずれも計測後に原状復帰し、`shasum` 一致 (`cec83bb8…`) と `git status` で確認済み
- **足場アーティファクトの逆流なし**: `proposal.md` と `specs/maui-cells/spec.md` に差分なし。`exploration.md` は追記のみ (削除行 0)、`tasks.md` は `[ ]`→`[x]` のみで本文改変なし。どちらも tasks 1.1 / 1.2 / 4.1 が明示的に求めた記録
- **Android 経路の不変性**: Android の `KsImageResolver` は無変更で、共有表を通らない。Android の後片付けは Glide 参照カウントの減算であり、`StoreIcon` の即時解放でも新リースが参照を保つため表示は壊れない
- **static のテスト汚染なし**: テストは `KsSharedImageRegistry.Shared` を使わずインスタンスを注入している (`GatewayScope.ImageRegistry`)
- **コメント規約**: 新規コメントに変更 ID・タスク通番・delta spec キーワード (`SHALL` 等)・履歴記述の混入なし。lint も 0 件

## アクションプラン

1. **[Major]** `KsSharedImageRegistry` の後片付け口の蓄積を止める (または蓄積が許容である根拠をオーナーへ提示する)。合わせて「再解決 N 回で保持数が増えない」不変条件のテストを追加する
2. **[Minor]** `DisposeRetired` を 1 件ごとに保護し、後片付けの失敗が残りのリースを取りこぼさないことをテストで固定する
3. **[Suggestion]** `Release` の防御分岐のコメントを実状に合わせる / evidence に計測環境の但し書きを 1 行足す
