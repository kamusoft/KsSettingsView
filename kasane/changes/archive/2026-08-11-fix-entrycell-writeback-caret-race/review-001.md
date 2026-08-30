# レビュー結果: fix-entrycell-writeback-caret-race (001 回目)

**日付**: 2026-08-11
**判定**: APPROVED
**対象**: 未コミットの作業ツリー変更 (tasks.md グループ 1〜3)。グループ 4 (実機検証) は未実施でレビュー対象外
**指摘件数**: Critical 0 / Major 0 / Minor 2 / Suggestion 2

## サマリー

デルタスペックの 5 Requirement・12 Scenario はすべて実装され、対応するテストが存在する。同一性判定を `cell.id` に限定した点、再同期時に `TextWatcher` を外して逆流を止めた点、`isEnabled = false` がフォーカス喪失を誘発することを見越して focus リスナーを常設化した点は、spec-review で採用された相方指摘 (Critical / Major 4 件) をいずれも構造的に塞いでいる。テストは 2188 件・失敗 0、ミューテーション実測で核心アサーションの回帰検出力も確認済み。

実装レベルの欠陥は検出しなかった。指摘は、本変更が新たに作る公開挙動契約を蒸留時に明文化すべき点 (Minor-1) と、規約上まだ「完了」と判定できない点 (Minor-2) が主である。

## 検証したこと

- **ビルドとテスト**: `cd android && ./gradlew test --rerun-tasks` を実行。8 テストタスク (4 モジュール × debug/release) すべてが本実行で再生成され、`build/test-results/test*UnitTest/TEST-*.xml` 集計で **tests=2188 / failures=0 / errors=0 / skipped=0**。新規 `EntryCellWriteBackGuardTest` は debug / release とも 12 件・失敗 0。
  - 注: 実行の最後に `Gradle build daemon has been stopped: stop command received` で BUILD FAILED となったが、これは並行していたレビュー側の daemon 操作が原因の環境事象であり、全テストタスクの結果 XML は生成済み・失敗 0 を確認した。
- **ミューテーション実測** (lessons/code-review L-001)。実装へ一時変更を入れ、テストが実際に落ちることを確認した。実施後は backup との `shasum` 一致で原状復帰を確認済み (`71ae3c5e…`)。
  - probe 1: `applyTextToEditor` の watcher 退避を無効化 → 「フォーカス喪失で保留されていたプログラム的更新が反映され通知は発火しない」「フォーカス喪失直前の入力は静穏化後の表示と通知の双方に残る」の 2 件が FAILED。表示アサーションは通過し通知アサーションだけが落ちたため、「再同期は `onTextChanged` を発火させてはならない」の検出力が証明された。
  - probe 2: 同一性判定を `boundCellId == cell.id` から `boundText == cell.text` (内容ベース) へ差し替え → 5 件が FAILED (「別 id への再バインドは text が同じでも新しい Cell として反映される」「同一 id で text だけが違う再バインドは…上書きしない」「往復より速い連続入力でも全文字が入力順どおり残る」を含む)。同一性の判別テスト 3 種が実際に判別を測っていることを確認した。
  - restore 後に再実行して 12 件・失敗 0 に復帰することを確認済み。
- **規約適合**: `python3 scripts/comment-policy-lint.py` → 禁止 0 件 (検査対象 571 ファイル)。追加コメントは自己完結しており、外部参照は `android/ADR-0001` `android/ADR-0002` `android/ADR-0003` の許容形式のみ。デルタスペック構文キーワード (SHALL 等) の混入もない。
- **足場の凍結**: `proposal.md` / `specs/` は未変更。change 配下の変更は `tasks.md` のチェック更新と、タスク 3.1 が対象とする `repro-burst-loop.sh` のみ。
- **tasks.md の正確性**: 1.1〜1.4 / 2.1〜2.7 / 3.1 のチェックはすべて実体を伴う。虚偽チェックなし。2.7 (既存テスト回帰) は上記の全件実行で裏取り済み。グループ 4 が未チェックであることも実態と一致。
- **spec 逸脱**: `deviation.md` は存在せず、無断の仕様逸脱も検出しなかった。
- **シェルスクリプト**: `zsh -n` で構文検査 OK。`exec > >(tee -a "$LOG")` によるログ欠落 (プロセス置換の flush 漏れ) を疑い等価な最小スクリプトで実測したが、判定行を含む全行がログに残ることを確認したため指摘しない。

## 指摘事項

### [🟡 Minor] 「書き戻しをしない利用構成」で blur 時に入力が表示から消える — 新しい公開契約として明文化が必要

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCellViewHolder.kt:286-290` (`resyncTextOnFocusLost`)

**問題点**: 再同期先は「最後に bind された `cell.text`」であり、これは spec の Requirement「フォーカス喪失時の text 再同期と収束」が SHALL として要求するとおりの実装である。ただし spec の収束保証は「未完了の往復がすべて配信される」ことを前提にしており、**書き戻しがそもそも発生しない利用構成では前提が成立しない**。

具体例: `onTextChanged` だけを購読して `cell.text` を更新しない使い方 (入力を検索条件としてのみ使う欄など)。`boundText` は初期値のまま更新されないため、フォーカス喪失時に入力欄が初期値へ戻り、以後 bind も届かないので収束しない。変更前は blur 時に何も起きなかったため、この構成に限れば挙動の後退にあたる。

`kasane/concepts/core/cells/input-cells.md` は「callback を受けて新しい Cell 値を供給する」を値+callback 経路の契約としており、また `samples/android` の「ニックネーム (callback)」は実際に書き戻している。したがって現行の実装・サンプルが壊れているわけではなく、**契約が暗黙だったものが本変更で必須化される**という位置づけになる。

**推奨修正**: 実装変更は不要。ADR 起票時に負の帰結として、また `concepts/core/cells/input-cells.md` (または `android/api/android-native-host.md`) の EntryCell 契約として次を明記すること。

- フォーカス喪失時、入力欄は最後に bind された `cell.text` へ再同期する
- したがって利用側は `onTextChanged` を受けて `cell.text` を更新する必要がある。更新しない構成ではフォーカス喪失時に表示が最後の bind 値へ戻る

proposal.md の「リスク」節が挙げているのはフォーカス中の外部更新の見え方だけで、この構成には触れていない。蒸留で拾い落とさないよう申し送りが要る。

### [🟡 Minor] 実機検証 (グループ 4) 未実施のため、規約上まだ「完了」と判定できない

**該当箇所**: `kasane/changes/fix-entrycell-writeback-caret-race/tasks.md:38-52`

**問題点**: 本変更は IME・フォーカス・フレーム間タイミングが絡むため、`kasane/concepts/cross/conventions/runtime-behavior-verification.md` の対象そのものである。同規約は「修正前の実環境再現 → 同一手順での解消確認 → 証跡を change 配下へ保存」までを完了条件とし、「テスト環境の green は実機の動作を保証しない」と明記している。グループ 1〜3 の範囲は妥当だが、change 全体としては未完了である。

あわせて、`repro-burst-loop.sh` が観測するのは **最終 text と View bounds だけ**である。Scenario「高速連続入力の完全性」が要求するキャレット非移動、および Scenario「日本語 IME 変換中の内容更新エコー」が要求する未確定文字列の維持は、このスクリプトでは検証できない (spec-review の Major「キャレット未検証でも合格できる」の残余部分)。タスク 4.3 / 4.4 は目視確認である旨と観測項目を、証跡側に明示的に残すことを推奨する。

**推奨修正**: 実装側の対応は不要。グループ 4 の実施と `evidence.md` の作成をもって完了判定とすること。スクリプトの合格 (`result=0`) だけをもって Scenario 全体の充足と読み替えないこと。

### [🔵 Suggestion] `catch (_: Throwable)` がテスト環境都合の防御として本番コードに残っている

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCellViewHolder.kt:271-275`

**問題点**: `setSelection` の失敗を `Throwable` 全捕捉で握り潰しており、コメントも「テスト環境 (Robolectric) で setSelection が失敗するケース」を理由に挙げている。移設された既存コードであり本変更が持ち込んだものではないが、新規メソッドとして切り出された結果 diff 範囲に入っている。新規テストは Robolectric 上でキャレット位置を実際にアサートできており (`h.caret` が 11 を観測)、この防御が現在も必要かは疑わしい。

**推奨修正**: 任意。削除して Robolectric で通ることを確認するか、想定例外 (`IndexOutOfBoundsException` 等) へ捕捉範囲を絞る。本番の異常を隠す全捕捉は既存債務として残すよりは縮めたい。

### [🔵 Suggestion] `repro-burst-loop.sh` の `result=` 行の型が混在している

**該当箇所**: `kasane/changes/fix-entrycell-writeback-caret-race/repro-burst-loop.sh:33,83`

**問題点**: 正常終了時は `result=0` / `result=1` と数値を、前提エラー時は `result=ERROR` と文字列を出力する。ログを機械集計する用途 (証跡としての保存が本タスクの目的) では読み手が両形式を扱う必要がある。

**推奨修正**: 任意。`result=OK` / `result=NG` / `result=ERROR` に揃えると、終了コードとログ行の役割が分離されて読みやすい。

## 確認して問題なしと判断した観点

- **同一性判定**: `boundCellId` を `cell.id` のみで比較。`EntryCell` は `data class` で `equals` が `text` を含むため `==` / 参照比較は罠になるが、実装・コメントとも回避理由を明示している。`reset()` で `boundCellId` / `boundText` を破棄しており、リサイクル後の持ち越しもない。
- **focus リスナーの常設化**: `init` で一度だけ装着し `bind` / `reset` で付け外ししない構造は、`editText.isEnabled = false` の代入が `View.setFlags` 経由で即座に `clearFocus()` を誘発する事実に対して正しい。bind 途中でリスナーを差し替える方式なら「無効化による編集終了」を取りこぼす。旧実装が無効セルで `onFocusChangeListener = null` にしていた挙動は `if (v.isEnabled)` ガードで等価に保たれている。
- **`isEnabled = false` 経路の再同期タイミング**: `bind` 内で再同期が走る時点で `currentWatcher` は既に null (L132-133 で解除済み、L238 で再装着) のため、この経路でも通知は逆流しない。テスト「フォーカス中の無効化は編集を終了させ直前の入力を静穏化後に残す」が実経路で押さえている。
- **プロパティ反映の優先順位**: `inputType` の同値ガード (android/ADR-0001)・`maxLength` の LengthFilter 上限比較・`hint` の差分判定はいずれも維持されている。`setTextColor` / `setGravity` / `setEnabled` の無条件代入は AOSP 側で同値早期 return するため IME 再起動を誘発しない。
- **収束の担保**: `SettingsRootStore.replaceCell` / `replaceCells` に同値による配信スキップはなく、通知した値は必ず Diff として配信される。テストの `awaitDifferCommit` はタイムアウトで `fail` するため、待機条件が成立しないまま黙って通過することもない。
- **リーク・スレッド**: 追加した保持状態は `String?` 2 本のみで View 参照を持たず、focus リスナーも引数 `v` を使うためリーク要因にならない。アクセスはすべてメインスレッド。
- **テストと Scenario の対応**: 12 Scenario すべてに対応テストが存在する。IME は composing span の範囲 (`BaseInputConnection.getComposingSpanStart/End`) までアサートしており、`setText` が span を落とす性質を利用した実効的な検証になっている。言い訳コメントによる実質スキップや、境界値・異常系の欠落は見当たらない。

## アクションプラン

1. (蒸留時・必須) Minor-1 を ADR の Consequences と concepts の EntryCell 契約へ反映する — 「フォーカス喪失時の再同期先は最後に bind された `cell.text`。利用側は `onTextChanged` を受けて `cell.text` を更新する必要がある」
2. (完了判定・必須) Minor-2 — グループ 4 の実機検証を実施し証跡を残す。スクリプトが観測しないキャレット・composing は目視確認として手順と結果を明記する
3. (任意) Suggestion 2 件。本変更で対応してもよいし、別途でもよい
