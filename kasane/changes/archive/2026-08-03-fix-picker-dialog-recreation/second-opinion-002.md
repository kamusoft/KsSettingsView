# セカンドオピニオン: fix-picker-dialog-recreation (code-review 1 回目)

**相方**: codex (器: ksn-reviewer) / **日付**: 2026-08-03 / **モード**: code-review
**対象**: 未コミット作業ツリー差分 (`android/ks-settingsview-ui/` の変更5ファイル + 新規6ファイル)
**対応するホスト側レビュー**: `review-001.md`

> 採番について: `second-opinion-001.md` は提案フェーズの spec-review モードで使用済みのため、
> code-review モードの1回目は 002 とした (ホスト側 review-001.md に対応する)。

**相方に渡した入力**: デルタスペック / proposal / tasks / ui/brief / ui/verification、関連 ADR (android/0006・0008・0010・0011)、
実装 diff の範囲 (変更・新規ファイル一覧)、客観的事実 (テスト 1722 件 green・ベースライン 1648 件・実環境検証済み)、deviation.md 不在。
**渡していない入力**: ホスト側レビューの内容、実装者の経緯報告、重点観点の誘導。

---

# レビュー結果: fix-picker-dialog-recreation

**判定: APPROVED**

Critical 0 / Major 0 / Minor 2 / Suggestion 0

デルタスペック、関連 ADR、実装差分、新規37テスト、検証記録を照合しました。主要経路には仕様を阻害する問題は見当たりません。テスト結果はご提示の 1722件成功を前提とし、こちらでは実行していません。

## 指摘事項

### [🟡 Minor] detach により one-shot 走査が未実行のまま消費される

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:642`

**問題点**: `isRestoreScanScheduled` は `post` 前に `true` になりますが、コールバック実行時に View が一時的に detach されていると、657行目で走査せず終了します。その後 reattach されてもラッチが `true` のため再予約されず、復元ダイアログが未処理のまま残る可能性があります。

**推奨修正**: 「予約中」と「完了済み」を別状態にし、コールバック開始時に予約中を解除して、実際に走査した場合のみ完了済みにしてください。あわせて「予約後に detach → コールバック消化 → reattach」のテストを追加すると安全です。

### [🟡 Minor] `findFragmentManager` の KDoc が宣言から分離している

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerCellViewHolder.kt:165`

**問題点**: `findFragmentManager` 用の KDoc と、新設された `resolveTimePickerDialogColors` の KDoc が連続しています。前者が対応する宣言から離れ、`findFragmentManager` 自体は説明なしになっています。

**推奨修正**: 165〜175行の KDoc を `Context.findFragmentManager()` の直前へ移動してください。

## アクションプラン

1. one-shot 走査の予約／完了状態を分離する。
2. detach・reattach 境界テストを追加する。
3. 分離した KDoc を正しい宣言の直前へ移動する。

---

`KSN_COUNTERPART_META`: counterpart=codex / session_id=<session-id> /
label=so-review-fix-picker-dialog-recreation / turns=1 /
response_file=`~/.kasane/counterpart-bridge/responses/so-review-fix-picker-dialog-recreation-1.md`

---

## 突き合わせ結果

ホスト側 `review-001.md` (判定 CHANGES_REQUESTED / 🟠1・🟡2・🔵3) と本ファイル (判定 APPROVED / 🟡2) の突き合わせ。

| # | 指摘 | ホスト | 相方 | 採否 | 根拠 |
|---|---|---|---|---|---|
| 1 | 復元走査が「復元された Fragment」と「表示中の生きた Fragment」を区別していない | 🟠 Major | — | **確定** | ホスト単独指摘だが、該当箇所・到達経路2本・実害 (別画面 Cell への値書き込み) が具体的で根拠強。android/ADR-0011 が position ベース tag を却下した failure mode の再来 |
| 2 | one-shot ラッチが「予約」時点で立ち、detach を挟むと二度と走査しない | 🟡 Minor | 🟡 Minor | **確定** | **双方一致**。独立した2文脈・別モデルが同一箇所 (`scheduleRestoreScanIfReady` / `runRestoreScan` の早期 return) を指摘。オーケストレーターもコードで実在を確認済み |
| 3 | `findFragmentManager` の KDoc が宣言から分離し宙に浮いている | 🟡 Minor | 🟡 Minor | **確定** | **双方一致**。`TimePickerCellViewHolder.kt:165-186` に KDoc ブロックが2連続していることをオーケストレーターもコードで確認済み |
| 4 | `PickerRestoreRegistry` の登録解除を固定するテストがない | 🔵 Suggestion | — | **採用** | 抜けると「detach 後も複数インスタンス判定が 2 のまま → 単独構成なのに一律 dismiss」の退行が無検出になる。テスト追加のみで安価 |
| 5 | 横向き「通常表示」の参照ショットが証跡に無い | 🔵 Suggestion | — | **降格** | spec が要求する検証点 (配色・「今日」ボタン) は既に証跡が揃っている。レビュー自身も「今後同種の検証を行うときは」という将来向け助言として提示。追加撮影のコストに見合わない |
| 6 | `DatePickerColorizer.kt:488` の既存コメントが規約違反 | 🔵 Suggestion | — | **降格** | 本変更が追加したものではなく、レビュー自身がスコープ外と明記 |

### 件数

**確定 3 / 採用 1 / 降格 2 / 未解決 0**

相方のみで採用に至った指摘 (ホスト側の見逃し) は **0 件**。今回の相方の寄与は「ホスト側 Minor 2件の独立再現による確度の底上げ」であり、新規の掘り当てはなかった。

### 入力の非対称について (記録)

ホスト側 ksn-reviewer には spec 由来の難所 (tag 符号化の曖昧性 / 駆動条件のタイミング / 複数インスタンス時の競合 / 適格条件の解釈 / リーク) を重点観点として渡したが、**相方には渡していない** (ksn-second-opinion が「特に X を見てほしい」の誘導を相方への入力に含めることを禁じているため)。
結果として相方のほうが誘導が少ない条件でのレビューとなった。相方が Major を掘り当てなかったことについて、この非対称が影響した可能性は排除できない。

### スコープ判断 (オーケストレーター裁定)

#1 の Major について、レビューは「スコープ外なら deviation 記録」の選択肢も提示していたが、**修正する**と裁定した。
`runRestoreScan()` は本変更で新設されたものであり、変更前にはこの誤書き込み経路自体が存在しない。すなわち本変更が新たに持ち込む退行であって、既知の限界として記録すべき性質のものではない。

