---
id: 0033
title: Root の header / footer は Host が購読できていない間に渡されても失わない
status: proposed
date: 2026-09-17
amends: 0019
---

## Context

MAUI の `Section.FooterView` / `Section.HeaderView` / Root の header・footer に任意 View を置いたページへ遷移すると、Android では最初のフレームに View が無く、遅れて行が挿入されて後続の行がスライドする。原因は次の連鎖にある。

MAUI facade は View の native 実体化と配信を、初回の設定ツリー配信より後 (MAUI View の `Loaded`) まで遅らせている。遅らせる理由として記録されているのは Root の header / footer だけである。Android の Host は取り付け前に Store 購読を張らず、Root 対象の更新通知は届かないまま捨てられる。Root の header / footer は Store の現在状態に含まれない (core/ADR-0005) ため、取り付け時の復元 (core/ADR-0019) でも戻らない。core/ADR-0019 はこれを「所有者が view load / attach 後に適用する責務」とした。

- Section の header / footer は Store の現在状態なので、取り付け前に渡しても復元される。それでも Root と同じ経路で一律に遅らされている
- text を持たず View だけの header / footer は、View が届くまで「内容なし」として行が作られない (core/ADR-0023)。届いた時点で行の挿入になり、既定の挿入アニメーションが付く
- Android の Host には Root の header / footer を直接受け取るプロパティがあり、これは購読と無関係に効く。取りこぼすのは Store の更新口を通した場合だけである
- Native を直接使う場合、View の header / footer は最初の設定ツリーに入れて渡せるため症状は出ない。ただし Store の更新口で取り付け前に Root の header / footer を渡すと、Android だけ黙って失われる

前提: Root の header / footer を Store の現在状態に含めない責務分離 (core/ADR-0005) は維持する。

## Decision

「**Root の header / footer は、Host の取り付け前に Store の更新口から渡されても失われず、取り付け後の最初の表示に反映される**」を両 OS の Host 保証に加える。

core/ADR-0019 の決定のうち「Root の header / footer は所有者 (呼び出し側) が view load / attach 後に適用する責務とする」を本決定で置き換える。他の決定 (取り付け時に Store 現在状態から復元する・復元対象の範囲・theme の扱い) は維持する。

- 保証は Host 側で実現し、Bridge だけで吸収しない。Store には Root の header / footer を持たせない
- Android は、Store が Root 対象の更新を結び付いている Host へ同期に直接知らせる受け口を持つ。Host は bind から unbind までの間これを登録し、知らされた値を自分のプロパティに控える
- Android で Root 対象を反映するのはこの受け口だけとし、取り付け中の通知の購読は Root 対象を反映しない。Store が持つのは受け口の登録 (Host の弱参照) だけで、値は持たない
- 受け口へ知らせるのは通知と同じ更新の値であり、更新の型と「1 操作につき 1 回適用する」境界 (core/ADR-0006) は変えない。分けるのは Root 対象の配送路だけである。登録は Host ごとに 1 件とし、同じ Store に結び付いた Host すべてへ知らせる
- Store の更新口を呼べるスレッドの契約は変えない。受け口は値をスレッド安全に控え、表示への反映はメインスレッドで行う。ある Host での反映の失敗は更新口の呼び出し元へ伝えない
- 構造・内容・Theme の更新は従来どおり取り付け中だけ購読し、取り付け時に Store の現在状態から復元する
- iOS の Host は view load 前に届いた Root 対象の更新を捨てずに自分のプロパティへ控え、view load 時の構築に使う。控える際に view load を誘発しない
- MAUI facade は、View を置いた header / footer を最初のフレームから表示することを目標とし、Section・Root の区別なく初回の配信に間に合わせる
- 内容の無い header / footer に行を作らない判定 (core/ADR-0023) は変えない

## Alternatives Considered

- **Bridge 側で Host のプロパティへ直接渡す**: 却下。変更は薄い層で小さく済むが、救われるのは MAUI 利用者だけで、Android の Native 直接利用者には取りこぼしの罠が残る。core/ADR-0019 が「Bridge / Host 直接利用者にも罠が残る」契約を却下した判断とも合わない
- **Store が Root の最新値を状態の外の別フィールドに控え、Host が取り付け時に読む**: 却下。core/ADR-0005 が却下した「Root Header・Footer を Store のプロパティにする案 (Store の責務肥大)」にあたり、その却下理由は現在も有効である
- **Host が bind の時点から通知を購読して Root 対象を拾う**: 却下。Store の通知は再生されず、容量に上限があり、送信の失敗を無視する。購読の開始が bind から戻る時点に間に合う保証が無く、容量を超える連続更新で最後の値を落とし得る。Root は Store に値が無いため、取りこぼすと復元できない
- **Root 対象の更新口をメインスレッド限定の契約にする**: 却下。仕組みは単純になるが、公開 API に新しい制約が増え、メインスレッド以外から呼んでいた利用コードを壊す
- **Store の更新通知を再生可能にする**: 却下。Root 以外の構造更新まで再生され、取り付け時の現在状態からの復元 (core/ADR-0019) と二重適用になる
- **遅れて届くのは許容し、挿入アニメーションだけを消す**: 却下。体感の主因は配信が `Loaded` まで遅れることで、アニメーションを消しても View が遅れて現れ後続の行が瞬間的にずれる見え方が残る。Root の header は先頭行のため全行がずれ、最も目立つ
- **View が届く前から行の枠だけを初回配信に載せる**: 却下。届く前は高さが分からず結局ずれる。「内容が無いが領域だけ出す」は core/ADR-0023 が三値化とともに却下しており、Section モデル・両 OS・Bridge を貫通する重さに見合わない
- **Root の header / footer は対象外とし Section だけ直す**: 却下。同じ領域の同じ症状であり、MAUI facade の配信経路が Section 用と Root 用の 2 本に分かれたまま残る

## Consequences

- 正: MAUI facade が Root の header / footer のために `Loaded` を待つ理由が無くなり、View の配信経路を Section・Root で 1 本にできる
- 正: Android の Native 直接利用者が取り付け前に Store の更新口を使っても Root の header / footer を失わない。取り付け順序に関する保証が両 OS でそろう
- 正: 既存の利用者は壊れない (取り付け後に適用する従来の使い方はそのまま有効)
- 負: Android の Store に、通知とは別の Host 向けの受け口が増える。Root 対象だけが構造の更新と別の経路で反映される
- 負: 表示済みの画面へ実行時に header / footer を足したときの挿入アニメーションは残る (実行時の内容変化として扱う)

## Revisit When

- Root の header / footer を Store の現在状態に含める方向へ core/ADR-0005 の責務分離を見直すとき (本決定の Host 側の保持は不要になる)

---
出典: kasane/changes/android-accessory-view-late-insert-animation/exploration.md (探索で確認した事実 / 検討した選択肢 / 決定事項) / kasane/changes/android-accessory-view-late-insert-animation/design.md (Decision 3 / Decision 4)
