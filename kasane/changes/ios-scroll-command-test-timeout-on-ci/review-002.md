# レビュー結果: ios-scroll-command-test-timeout-on-ci (002 回目)

**日付**: 2026-09-30
**判定**: APPROVED

## サマリー

前回 (review-001.md) の Minor 指摘への修正を確かめた。DSL の割り込みテストで、命令を出した直後 (割り込みブロックの外) に `markIssued(first)` を足している。これで割り込みが回らない状態でも、報告に命令からの経過時間と「命令の直後の main.async=未実行」が出る。割り込みが回れば内側の `markIssued(second)` で上書きされるので、通常の報告は前回と変わらない。追加の `main.async` はフラグを立てるだけで、受け口の引き直し → 割り込み → Host の遅延 という前提の順序は崩れていない。新たな指摘はない。

## 照合した規約

- cross/comment-policy.md (always)
- cross/test-execution.md (テストを実行するとき・テスト結果を報告するとき)
- 今回の対象はテスト 1 関数の周辺だけのため、前回照合した ADR (cross/ADR-0027・core/ADR-0037) の判断は変わらない

ロードしたスキル: ksn-review (コンテキストパッケージの解決結果どおり、追加の code-review スキルなし)

## 確認したこと

**対象の差分**
- `ios/Tests/KsSettingsViewSwiftUITests/DSLScrollControlTests.swift:511-521`: `first.scrollToEnd(animated: false)` の直後にコメント 1 行と `markIssued(first)` を追加。割り込みブロック内の `self.markIssued(second)` は前回のまま残る

**指摘が解消されたか**
- 解消されている。割り込みブロックが実行されない場合 (main キューが止まる候補) でも `lastIssuedCommand` には `first` の記録が残る。このため報告は「命令の記録なし」ではなく、次の内容になる
  - `受け口: 引き直し=...`: `first` がまだ受け口につながっているため
  - `命令から N 秒 命令の直後の main.async=未実行`
- 記録の漏れと main キューの停止を、報告の見え方で区別できるようになった

**記録の上書きの順序**
- `markIssued(first)` はテスト本体で同期的に記録する。割り込みブロックはその後で main キューから実行されるので、`markIssued(second)` が必ず後から上書きする。逆の順序になる経路はない
- 待機が待っているのは割り込みの中で出した位置の戻し (処理済み 1 件) なので、ブロックが回った後は `second` の記録が正しい対象になる。ブロックが回らなければ位置の戻しも出ていないので、最後に出した命令である `first` からの経過で読むのが正しい。どちらの状態でも、報告の意味は「最後に出した命令」で一貫している
- `receiverState` は記録したハンドルの `currentReceiver` を読む。ブロックが回る前でも後でも受け口 (`DSLScrollCommandResolver`) を指すので、どちらの記録でも受け口の観測値が出る

**追加の `main.async` がテストの前提を崩さないか**
- 実装の送り先を読んで、main キューに積まれる順を確かめた
  - `DSLScrollCommandResolver.receive` (`ios/Sources/KsSettingsViewSwiftUI/DSLScrollCommandResolver.swift:69-76`) は、命令を受けた時点で引き直しを `main.async` で積む
  - Host の `enqueueScrollEntry` (`ios/Sources/KsSettingsViewUI/KsSettingsViewController+ScrollControl.swift:130-141`) は、引き直しの中で Host へ渡った時点で遅延を `main.async` で積む
- この結果、main キューは「引き直し → 記録のフラグ (今回の追加) → 割り込みブロック」の順に並ぶ。Host の遅延は引き直しの実行中に積まれるので、割り込みブロックより後になる。割り込みが Host の遅延より前に入るという前提は保たれている
- 間に入る記録のフラグは `IssuedCommand.mainQueueRan` を立てるだけで、Host・受け口・UIKit の状態に触れない。前提アサーション `handedToHost == 1` の観測時点も変わらない
- ブロック内の `markIssued(second)` のフラグは Host の遅延より後に回る。これは前回から変わっていない

**コメント (comment-policy)**
- 追加コメント「割り込みの処理が回らないまま待機が deadline を超えても、命令を出した時点の記録が残るようにする。」は、ファイル単体で意味が通る。直前の行の「割り込んで差し替える」と対になって読める
- 作業文書・レビュー・通番への参照はなく、時間軸の記述や仕様用のキーワードもない
- `scripts/comment-policy-lint.py --advisory` で、このファイルからの検出はない

**ビルドとテスト** (専用シミュレータ ksn-scroll-probe を boot して 1 回だけ実行し、終了後に shutdown した。他のシミュレータには触れていない)
- `xcodebuild test -scheme KsSettingsView -only-testing:KsSettingsViewSwiftUITests/DSLScrollControlTests` で TEST SUCCEEDED (10 tests / 0 failures)
- 対象テスト `test_Hostへ渡った後の未実行の命令もハンドルの差し替えで捨てられ位置の復元は残る` は passed
- このファイルからのコンパイル警告はない
- 追加した記録は失敗時の報告だけに現れる。報告の中身をミューテーションで確かめる作業は前回済ませており (review-001.md)、今回の差分は記録の置き場所を 1 箇所足しただけで報告の組み立ては変えていない。このため、今回はミューテーションを再度実行していない (反復実行を避けるというオーナーの指示にも沿う)

**前回の Suggestion** (命令と待機の間に別の処理を挟むテストでは、「命令の直後の main.async」が判定材料にならない件)
- 対応を見送ったことはコンテキストパッケージで受け取っている。再指摘はしない

## 指摘事項

なし

## アクションプラン

なし (このまま完了でよい)
