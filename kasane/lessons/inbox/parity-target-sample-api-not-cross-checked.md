---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-08-10
last-seen: 2026-08-10
evidence:
  - add-maui-basic-input-cells (samples-maui spec が「iOS/Android サンプルとの完全一致」を SHALL で要求する一方、一致対象の native サンプルが使う `Section(headerHeight:)` が maui-cells / maui-bridge の公開面 spec に含まれておらず、サンプル実装ワーカーが facade の欠落として停止。`valueText` も基本 Cell 側の解釈が狭く、同じ突き合わせ漏れから再現不能箇所が出た。オーナー判断で headerHeight は deviation 記録の上スコープ追加。さらに実装後の code-review (review-001 Major-2) が3件目として `EntryCell` の値変更 callback (`onTextChanged`) の欠落を検出 — 「ニックネーム (callback)」デモの対比構造が MAUI で成立せず、こちらは deviation 記録で対処。同一 change 内で3例)
---

## ルール文

parity 要求 (「既存 platform のサンプル/画面と完全一致」) を SHALL で立てる spec を書く・レビューするときは、一致対象の実装ソース (native サンプルのコード) が使用している API を列挙し、同 change の公開面 spec (facade / Bridge) がそのすべてを供給できるかを突き合わせる。parity は「一致しろ」という要求だけでは完結せず、一致に必要な API が同時に揃うことが前提条件になる — 欠けていると実装終盤 (サンプル移植) で初めて欠落が顕在化し、スコープ判断の差し戻しが発生する。

## 経緯

- 2026-08-10 add-maui-basic-input-cells: samples-maui spec は4デモページの完全一致を SHALL で要求。しかし一致対象の iOS/Android サンプルが使う `Section(headerHeight: 60)` は maui-cells (facade 公開面) にも maui-bridge (輸送) にも規定がなく、Bridge 実装ワーカー2名はともに「spec 対象外」として正しく未実装、サンプル実装ワーカーが移植不能として停止した。また基本 Cell の `valueText` (native 共通行フィールド) も、facade ワーカーが「AiForms に無く spec 個別列挙にも無い」と狭く解釈して5種に未公開のままとなり、共通フィールドデモの再現不能箇所が出た (こちらは「native の対応 Cell と同じ状態フィールド (SHALL)」の読み直しで spec 内解決)。提案フェーズの spec-review (ホスト + 相方2回) はいずれも parity 対象サンプルのソースと公開面の突き合わせを行っていなかった。
