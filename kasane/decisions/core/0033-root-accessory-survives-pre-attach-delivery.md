---
id: 0033
title: Root の header / footer は Host が購読できていない間に渡されても失わない
status: accepted
date: 2026-09-17
amends: 0019
---

## Context

MAUI の `Section.FooterView` / `Section.HeaderView` / Root の header・footer に任意 View を置いたページへ遷移すると、Android では最初のフレームに View が無く、遅れて行が挿入されて後続の行がスライドしていた。原因は次の連鎖にある。

MAUI facade は View の native 実体化と配信を、初回の設定ツリー配信より後 (MAUI View の `Loaded`) まで遅らせていた。遅らせる理由として記録されているのは Root の header / footer だけである。Android の Host は取り付け前に Store 購読を張らず、Root 対象の更新通知は届かないまま捨てられる。Root の header / footer は Store の現在状態に含まれない (core/ADR-0005) ため、取り付け時の復元 (core/ADR-0019) でも戻らない。core/ADR-0019 はこれを「所有者が view load / attach 後に適用する責務」とした。

- Section の header / footer は Store の現在状態なので、取り付け前に渡しても復元される。それでも Root と同じ経路で一律に遅らされていた
- text を持たず View だけの header / footer は、View が届くまで「内容なし」として行が作られない (core/ADR-0023)。届いた時点で行の挿入になり、既定の挿入アニメーションが付く
- Android の Host には Root の header / footer を直接受け取るプロパティがあり、これは購読と無関係に効く。取りこぼすのは Store の更新口を通した場合だけである
- iOS の Host は生成時から購読を張るが、view load 前に届いた更新は全体差し替え以外を捨てる。Root 対象も捨てられる。MAUI 経由で順序に依存しないのは、Handler が Host の view を先に読んで view load を済ませているためにすぎない
- Native を直接使う場合、View の header / footer は最初の設定ツリーに入れて渡せるため症状は出ない。ただし Store の更新口で取り付け前 (iOS は view load 前) に Root の header / footer を渡すと黙って失われる
- Store の通知は再生されず、容量に上限があり、送信の失敗を無視する。Section や Cell は取りこぼしても現在状態から復元できるが、Root は Store に値が無いため復元できない

前提: Root の header / footer を Store の現在状態に含めない責務分離 (core/ADR-0005) は維持する。Store の更新口を呼べるスレッドの契約 (どのスレッドから呼んでもよい) は変えない。

## Decision

「**Root の header / footer は、Host が購読できていない間に Store の更新口から渡されても失われず、Host の次の表示に反映される**」を両 OS の Host 保証に加える。

core/ADR-0019 の決定のうち「Root の header / footer は所有者 (呼び出し側) が view load / attach 後に適用する責務とする」を本決定で置き換える。他の決定 (取り付け時に Store 現在状態から復元する・復元対象の範囲・theme の扱い) は維持する。Host を作り直したときに再適用するのは引き続き所有者の責務である (Root は Store に無いため)。

### 両 OS 共通

- 保証は Host 側で実現し、Bridge だけで吸収しない。Store には Root の header / footer の値を持たせない
- 構造・内容・Theme の更新は従来どおり取り付け中だけ購読し、取り付け時に Store の現在状態から復元する。Store の通知への Root 対象の発行は現行のまま残す (core/ADR-0020 の「Root 系は無条件に通知を発行する」を変えない)
- 内容の無い header / footer に行を作らない判定 (core/ADR-0023) は変えない

### Android の Host

- Store が Root 対象の更新を結び付いている Host へ**同期に**直接知らせる受け口をモジュール内部に持つ。Host は `bind` の中で登録し、`unbind` と別 Store への `bind` で外し、知らされた値を自分のプロパティに控える
- 受け口の知らせには配送元の Store が付き、Host は現在結び付いている Store からの更新だけを受理する — ある Store の配送の途中で別 Store への `bind` / `unbind` が挟まっても、以前の Store の値を取り込まない
- Root 対象を反映するのはこの受け口だけとし、取り付け中の通知 (`diffs`) の購読は Root 対象を読み飛ばす。反映経路が 1 本なので、取り付け状態による切り替えも二重適用の調停も要らない
- 受け口へ知らせるのは通知と同じ更新の値であり、更新の型と「1 操作につき 1 回適用する」境界 (core/ADR-0006) は変えない。分けるのは Root 対象の配送路だけである。登録は Host ごとに 1 件とし、同じ Store に結び付いた Host すべてへ知らせる
- Store が持つのは受け口の登録 (弱参照) だけで、値は持たない。参照が切れた登録は次に知らせる時点で取り除く。受け口は Host の公開 API に露出させない
- 受け口は値をスレッド安全に控え、表示への反映はメインスレッドで行う (メインスレッドからの呼び出しはその場で、それ以外はメインスレッドへ送る。取り付け時は未反映の値を先に反映する)。ある Host での反映の失敗は記録に残し、他の Host への知らせと通知の発行を続け、更新口の呼び出し元へは伝えない

### iOS の Host

- view load 前に届いた Root 対象の更新 (`nil` による解除を含む) を捨てずに自分のプロパティへ控え、view load 時の構築に使う。控える際に view load を誘発しない

### MAUI facade

- View を置いた header / footer を最初のフレームから表示することを目標とし、Section・Root の区別なく初回の配信に間に合わせる (facade 側の順序は maui/ADR-0027)

## Alternatives Considered

- **Bridge 側で Host のプロパティへ直接渡す**: 却下。変更は薄い層で小さく済むが、救われるのは MAUI 利用者だけで、Android の Native 直接利用者には取りこぼしの罠が残る。core/ADR-0019 が「Bridge / Host 直接利用者にも罠が残る」契約を却下した判断とも合わない
- **Store が Root の最新値を状態の外の別フィールドに控え、Host が取り付け時に読む**: 却下。core/ADR-0005 が却下した「Root Header・Footer を Store のプロパティにする案 (Store の責務肥大)」にあたり、その却下理由は現在も有効である
- **Host が bind の時点から通知を購読して Root 対象を拾う**: 却下。購読の開始が bind から戻る時点に間に合う保証が無く、容量を超える連続更新で最後の値を落とし得る。取り付け中の購読との二重適用の調停も要る。Root は Store に値が無いため、取りこぼすと復元できない
- **Root 対象の更新口をメインスレッド限定の契約にして、受け口がその場で反映する**: 却下。仕組みは単純になるが、公開 API に新しい制約が増え、メインスレッド以外から呼んでいた利用コードを壊す。不具合修正に伴って利用者の契約を狭めない
- **Store の更新通知を再生可能にする**: 却下。Root 以外の構造更新まで再生され、取り付け時の現在状態からの復元 (core/ADR-0019) と二重適用になる
- **iOS は Bridge の Host 生成で view load を強制する**: 却下。Bridge 経由しか救わず、Native 直接利用で view load 前に更新口を使う場合が残る
- **遅れて届くのは許容し、挿入アニメーションだけを消す**: 却下。体感の主因は配信が `Loaded` まで遅れることで、アニメーションを消しても View が遅れて現れ後続の行が瞬間的にずれる見え方が残る。Root の header は先頭行のため全行がずれ、最も目立つ
- **View が届く前から行の枠だけを初回配信に載せる**: 却下。届く前は高さが分からず結局ずれる。「内容が無いが領域だけ出す」は core/ADR-0023 が三値化とともに却下しており、Section モデル・両 OS・Bridge を貫通する重さに見合わない
- **Root の header / footer は対象外とし Section だけ直す**: 却下。同じ領域の同じ症状であり、MAUI facade の配信経路が Section 用と Root 用の 2 本に分かれたまま残る

## Consequences

- 正: MAUI facade が Root の header / footer のために `Loaded` を待つ理由が無くなり、View の配信経路を Section・Root で 1 本にできる
- 正: Android の Native 直接利用者が取り付け前に Store の更新口を使っても Root の header / footer を失わない。取り付け順序に関する保証が両 OS でそろう
- 正: 既存の利用者は壊れない (取り付け後に適用する従来の使い方はそのまま有効)
- 負: Android の Store に、通知とは別の Host 向けの受け口が増える。Root 対象だけが構造の更新と別の経路で反映される
- 負: MAUI facade が Root を取り付け前に渡すようになるため、この保証を持たない Android の Host と組み合わせると Root の header / footer が表示されない (facade と native の世代がずれた配備で現れる)
- 負: 表示済みの画面へ実行時に header / footer を足したときの挿入アニメーションは残る (実行時の内容変化として扱う)

## Revisit When

- Root の header / footer を Store の現在状態に含める方向へ core/ADR-0005 の責務分離を見直すとき (本決定の Host 側の保持は不要になる)

---
出典: kasane/changes/archive/2026-09-18-android-accessory-view-late-insert-animation/exploration.md (探索で確認した事実 / 検討した選択肢 / 決定事項) / 同 design.md (Decision 3 / Decision 4) / 同 deviation.md (配送元の Store による受理判定・受け口を公開 API に露出させない裁定)
関連: maui/ADR-0027 (本決定の Host 保証を前提に、facade が View の実体化と配信を Host 生成の前へ移す)
