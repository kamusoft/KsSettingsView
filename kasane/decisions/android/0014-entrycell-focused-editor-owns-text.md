---
id: 0014
title: フォーカス中の EntryCell 入力欄は値の SSoT — 内容更新の text 反映はフォーカス喪失まで遅延する
status: accepted
date: 2026-08-11
---

## Context

EntryCell への高速連続入力で文字の欠落・並び替え・キャレット移動が発生し、壊れた値が書き戻しでアプリ状態まで確定する不具合を Pixel 6a 実機で確認した (MAUI 経路 7/18 破損、native Compose/Store 経路 4/10。陰性対照の素の EditText は 8/8 正常で、KsSettingsView スタック固有)。

原因は書き戻しラウンドトリップの構造的レース: 打鍵 → `onTextChanged` → 呼び出し側の Store コミット (`replaceCell`) → 次フレームの bind、という往復の「配信スナップショット確定 → bind 適用」の窓に次の打鍵が挟まると、bind が古い値で `setText` + 末尾 `setSelection` を行い、確定済みの打鍵を巻き戻す。さらに `setText` が IME の composing を破壊し、フォーカスと IME 接続が生きたまま入力を受け付けなくなる二次被害 (desync) も観測した。

[ADR-0001](0001-content-update-preserves-viewholder.md) の ViewHolder 維持保護は正しく機能しており (EditText インスタンスは同一のまま)、本件は別経路の問題である。また既存のエコー抑止 (bind 側の同値 setText ガード) は「EditText が既に次の文字へ進んでいる」ケースを止められない — 同値でないからこそ setText が走る、という構造。

## Decision

`EntryCellViewHolder.bind` の text 反映契約を次のとおりとする:

- **同一 Cell (同じ `cell.id`) への再バインドで入力欄 (EditText) がフォーカス中の間は、text とキャレット位置を差し替えない** — 編集中の値の正 (SSoT) は入力欄自身であり、書き戻し経路から遅れて返る値 (エコー含む) で上書きしない。
- **フォーカス喪失時**に、最後にバインドされた `cell.text` と入力欄が食い違っていれば再同期する。再同期の setText は TextWatcher を発火させない (再同期値を書き戻し経路へ逆流させない)。
- text 以外のプロパティ反映 (表示系は即時 / 入力系は同値ガード付き) と、**別 Cell (異なる `cell.id`) への再バインド・非フォーカス時の text 反映は従来どおり**。同一性判定は `cell.id` のみで行う (EntryCell は data class で equals が text を含むため、equals / 参照比較は使わない)。

ui 層 1 箇所の修正で、MAUI facade・native Compose DSL・Store 直接利用のすべての経路に効く。

## Alternatives Considered

- **世代トークン方式** (打鍵通知に単調増加の通番を付けて往復させ、自分が送った通番より古いスナップショットは反映しない): エコーと外部変更を厳密に区別できるが、bridge DTO・core の輸送契約に手が入り iOS も巻き込む。ui 層 1 箇所で全経路に効く本案で足りるため却下。
- **C# Controller 側でエコー配信を抑止** (ユーザーコードが値を変えなかった場合に ReplaceCell 配信自体を止める): MAUI の窓は塞がるが、native DSL / Store 経路のレース (実測 4/10) が残るため単独では不十分で不採用。
- **キャレット相対位置の保存・復元**: setText 前後でキャレットを補正しても、巻き戻しによる文字欠落自体は解けない対症療法のため却下。
- **入力元への通知抑止 (一方向化)**: ADR-0001 が却下済み。本決定は通知経路 (`onTextChanged` → 書き戻し) を維持し、**反映側だけをフォーカス期間ガードする**点で異なる。

## Consequences

- 正: 書き戻しの往復より速い連続入力でも欠落・並び替え・キャレット移動が起きない (実機 A/B: native 10/18 破損 → 0/17、MAUI 7/18 → 0/18)。
- 正: レースの setText による IME composing 破壊が消え、バースト後の入力不能化 (desync) も解消。日本語 IME の変換操作が書き戻しエコー下でも成立する。
- 負: フォーカス中のプログラム的な text 更新 (入力値の正規化を即時反映する利用パターン等) は入力欄へ即時反映されず、フォーカス喪失時に反映される。編集中でない Cell は従来どおり即時。
- 負: `onTextChanged` を受けて `cell.text` を更新しない利用構成では、フォーカス喪失時に入力欄が最後にバインドされた text へ戻る。値 + callback 経路の利用側は callback を受けて `cell.text` を更新する必要がある — これまで暗黙だった契約が本決定で必須化された。
- 補足: ADR-0001 (内容更新は ViewHolder を維持する) を**補完**する。ADR-0001 の Consequence「高速入力の取りこぼし解消」は ViewHolder 再生成由来の取りこぼしに限られ、書き戻しレース由来の欠落は本決定が塞ぐ。
- 補足: iOS の同型レース (UITextField 経路) は未検証。構造確認と実機試験の上、確認され次第 ios ドメインの変更として扱う。

出典: fix-entrycell-writeback-caret-race (exploration.md の実測記録と原因分析 / proposal.md「ADR 候補への申し送り」/ review-001 Minor-1 / evidence.md の実機 A/B)
