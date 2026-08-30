# レビュー結果: fix-android-accessory-header-refresh (002 回目)

**日付**: 2026-08-05
**判定**: APPROVED

## サマリー

review-001 のアクションプラン 1〜5 はいずれも意図どおり修正されており、独立に再実測して効果を確認した。Major として指摘した「Scenario 対応テスト 2 件が修正前コードでも green」は解消し、**修正前コードに対して 18 件中 11 件が FAIL** (前回は 9 件) — 指摘した 2 件が両方 FAIL 側へ移った。5 秒空転していたテストも 0.01s に戻り、全 21 件が正常な所要時間で走っている。新たな Critical / Major / Minor は検出していない。

アクションプラン 6 (型切替の実環境目視) は未実施だが、これは review-001 でも 🔵 Suggestion であり、判定を保留する理由にはしない。

## 検証した内容 (客観的事実)

| 項目 | 結果 |
|---|---|
| `cd android && ./gradlew test --rerun-tasks` | BUILD SUCCESSFUL / **1964 tests / 0 failures / 0 errors / 0 skipped** |
| `python3 scripts/comment-policy-lint.py` | 変更 4 ファイルはいずれも違反 0 件 (追記された KDoc・行コメントを含めて) |
| 逆流検査 | `specs/.../spec.md` (11:19:35) / `proposal.md` (11:19:47) はいずれも初回実装開始前の mtime のまま。今回の修正ラウンド (12:15〜12:16) で足場の書き換えなし |
| 旧契約テストの残骸 | `areContents に反映されない` の grep 結果 0 件 |
| ミューテーション再実測 | 下記のとおり |

### ミューテーション再実測 (独立環境)

前回のプローブ環境 (`scratchpad/probe`) は 12:19 に第三者によって上書きされていたため、独立性を担保するために**ゼロから再構築** (`scratchpad/probe2`) した。本体 2 ファイルを `git show HEAD:` から取り直し、**shasum 一致で HEAD 版であることを確認**したうえで、修正後のテストファイル (`contentChangedCellIds` 直接テスト 3 件は HEAD ではコンパイル不能のため除去) を実行した。

修正前コード (HEAD) に対する結果 — **18 件中 11 件 FAIL** (前回 9 件):

```
FAIL   5.02s replaceSection による header text 変更が表示へ反映される        ← 前回 PASS (0.02s)
FAIL   5.02s Full diff で同一 id の Cell 内容変更が表示へ反映される          ← 前回 PASS (5.20s)
FAIL   5.02s updateAccessory による header text 変更が…
FAIL   5.02s updateAccessory による footer text 変更が…
FAIL   5.04s View accessory の差し替えが…
FAIL   5.03s accessory の型の切替が表示へ反映される
FAIL   5.02s replaceSection で同一 id の Cell 内容変更が…
FAIL   5.03s root の再設定でも同一 id の Cell 内容変更が反映される
FAIL   5.02s 削除された Cell へは内容通知を発行しない
FAIL   5.02s 新規に挿入される Cell へは内容通知を重ねない
FAIL   7.24s hidden から表示へ復帰する Cell へは内容通知を重ねない
PASS   0.15s header の追加が行の挿入として反映される
PASS   0.03s footer の解除が行の削除として反映される
PASS   0.01s 内容が変わらない Cell へは内容通知を発行しない
PASS   0.02s 初回の root 反映では内容変更通知を発行しない
PASS   0.02s 空 root への full 更新で表示が空になる
PASS   0.02s 内容が同一の Section H_F へは変更通知を発行しない   ← 前回 5.03s → 待機条件の修正が効いている
PASS   0.03s Section header footer だけを持つ root への full 更新が反映される
```

残る PASS 7 件はいずれも「修正前後で挙動が変わらないことを固定する」性格のテスト (構造通知のみで成立する挿入・削除、および negative Scenario) であり、HEAD で PASS するのが正しい。回帰検出力の欠落ではない。

修正後コード (本番) では全 21 件が 0.00〜0.01s で完了し、review-001 Minor で指摘した 5 秒空転は解消している。

## 個別確認

### アクションプラン 1 (Major): payload 付き通知アサーションの追加 — 妥当

`FullUpdateContentSyncTest.kt:184-189` / `:389-394` に `ChangeRecord(0, 1, PAYLOAD_CONTENT)` の照合が追加され、「新規 ViewHolder への bind は通知の有無と無関係に currentList の新しい値を返す」という理由がコメントで明示されている。再実測で両件が FAIL 側へ移ったことを確認済み。

### アクションプラン 2 (Minor): `awaitDifferCommit` のタイムアウト失敗化 — 妥当

`FullUpdateContentSyncTest.kt:54-64`。ループを抜けた後に `fail(...)` を置く形で、待機条件が成立しないまま先へ進む経路が閉じている。ファイル内 19 箇所の待機すべてに一律で効く。

### アクションプラン 3 (Minor): 待機条件 `itemCount == 6` — 妥当

`FullUpdateContentSyncTest.kt:290`。初期 5 件 + `InsertCell` 1 件 = 6 で正しい。修正前後の所要時間 (5.01s → 0.01s) が実効を裏づける。

### アクションプラン 4 (Minor): headerHeight は (b) 案 = KDoc 明記 + follow-up 起票 — 妥当

`KsSettingsListAdapter.kt:293-295` に「比較対象は accessory の内容のみで、`SectionHeader.headerHeight` は含まない。そのため header text が同一のまま headerHeight だけを変える full 更新は再 bind されない」と明記されている。コメント単独で穴の所在と条件が読み取れる。follow-up は `kasane/changes/fix-android-header-height-refresh/exploration.md` に起票済みで、課題・修正方向の候補・級の推奨 (S) まで揃っている。本 change のデルタスペックは Requirement を accessory 内容に限定しているため、スコープ外として送るのは spec とも整合する。

### アクションプラン 5 (Suggestion): `contentChangedCellIds` の KDoc — 妥当

`KsSettingsView.kt:955-958`。「関数値 (`CustomCell` の `builder` / `onTap` 等) は等価性に参加しない (core/ADR-0014) ため、表示に効く値は `content` 側に含めるのが利用者契約 — 関数値だけを差し替えた full 更新はここでは変化として検出されない」と、断定が撤回され例外が自己完結して読める形になっている。`core/ADR-0014` はコメント規約の許容参照形式。

## 指摘事項

### [🔵 Suggestion] 復活した 2 件の検出力は現状 `fail()` 経由で成立している

**該当箇所**: `FullUpdateContentSyncTest.kt:180` / `:387` (`awaitDifferCommit { recorder.changed.isNotEmpty() }`)

**内容**: 再実測で両件が FAIL するのは、追加した `ChangeRecord` の照合に到達したためではなく、その手前の `awaitDifferCommit` がタイムアウトして `fail()` するためである (所要 5.02s がその証拠)。検出力としては十分に成立しており、追加アサーションは「待機が通ってしまった場合の最後の砦」として二重の担保になっている。修正は不要だが、将来 `awaitDifferCommit` の実装を触るときはこの 2 件の検出力がそこに依存している点を意識されたい。

### [🔵 Suggestion] アクションプラン 6 (型切替の実環境目視) は未消化

**内容**: エミュレータ検証フェーズで実施予定と申し送りを受けている。payload 付き変更通知 + stable IDs + view type 変化の組み合わせで `RecyclerView` が実際に ViewHolder を交換するかは Robolectric の範囲外 (`cross/conventions/test-execution.md`)。蒸留前に 1 回消化しておくと、`cross/conventions/runtime-behavior-verification.md` の趣旨にも沿う。

## アクションプラン

なし (ブロッキングな指摘なし)。エミュレータ検証フェーズでアクションプラン 6 を消化すること。
