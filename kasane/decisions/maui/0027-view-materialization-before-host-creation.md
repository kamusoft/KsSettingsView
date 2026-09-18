---
id: 0027
title: 配置された View の実体化と配信は Host の生成前に済ませ、Root だけ生成直後に適用する
status: accepted
date: 2026-09-17
amends: 0016
---

## Context

配置された MAUI View (Section の header / footer・Root の header / footer・CustomCell の内容) の実体化と配信は、Host が view 階層へ取り付けられた後 (`Loaded`) にまとめて行っていた (maui/ADR-0016)。取り付けを待つ理由は Root の header / footer だけにあった — Store の現在状態に含まれず、Android の Host が取り付け前の更新を失うためである。この 1 つの制約のために Section の header / footer と CustomCell の内容まで遅れ、Android では View だけの header / footer が遷移後に行の挿入として現れ、後続の行がスライドしていた。

Host は生成時に Store の現在状態から表示を復元する。実体化の前提 (`MauiContext`) は Host を作る前に揃っており、自己計測 wrapper は platform view の親も Host も要求しない。Handler の切断 → 再接続では設定ツリー全体の配信は走らず、Section の View は解放時に text へ書き戻されている。表示中の Root を作り直す経路では同じ View が残り得るため、その View の旧実体は配信が済むまで後片付け待ちになる。

前提: 両 OS の Host が、購読できていない間に渡された Root の header / footer を失わない (core/ADR-0033)。View の寿命の契約 — 論理所有と platform lease の分離・Handler 1:1・「Store 更新 → native への配信 → 旧 wrapper 破棄」の退役順序 (maui/ADR-0016) — は維持する。

## Decision

maui/ADR-0016 の決定のうち「Handler 再接続時は Host 取り付け後に再実体化し、Root + Section の全 view accessory を再発行する」を本決定で置き換える。他の決定 (三層構造・自己計測 wrapper・論理所有と platform lease の寿命分離・退役順序・Handler 切断時の破棄と書き戻し) は維持する。

- Store の現在状態になるもの (Section の header / footer・CustomCell の内容) は、Host を作る前に実体化して Store へ届ける。初回接続では設定ツリー全体の配信データに載せ、再接続では更新口 (CustomCell の内容は 1 バッチ) で届ける
- Root の header / footer は、Host を作った直後・platform view を返す前に、取り付けを待たずに適用する
- 実体化は controller が行い、gateway が配信データを組む途中では行わない。実体化の口は接続より前に差し込み、切断時に手放す
- 設定ツリー全体の事前実体化は、後片付け待ちの実体を持つ View を対象から外し、その分は従来どおり配信の後に実体化して送る — 先に包み直すと native が参照する旧 wrapper を配信前に破棄することになり、退役順序に反する
- 取り付けの通知 (`Loaded`) で行うのは親子関係の成立確定だけとする
- Host の生成・Root の header / footer の適用・親子関係の登録は 1 つの失敗単位とし、途中で失敗したら親子関係を解き、Host を世代ごと解放してから失敗を伝える。失敗した世代の実体や口を次の生成へ持ち越さない

## Alternatives Considered

- **取り付け後の適用は残し、初回接続の全体配信にだけ View を載せる**: 却下。再接続では全体配信が走らないため、ページ再表示・タブ切替で後着が残る。経路も 2 本になる
- **配信データを組む箇所で未実体化の配置をその場で実体化する**: 却下。輸送層で Handler 生成という重い副作用が走り、三層構造を崩す
- **Root だけ取り付け後の適用のまま残す**: 却下。Root の header は先頭行で、後着すると全行がずれる
- **後片付け待ちの実体を持つ View も事前に包み直す**: 却下。退役順序 (maui/ADR-0016) を破る。表示中の Root の作り直しは新しい保証 (初回表示・再接続) の対象ではなく、その位置だけ配信の後に送っても目標を損なわない

## Consequences

- 正: 配置した View が初回接続・再接続のどちらでも最初の表示に含まれる
- 正: 再接続時の Section の配信は Host が存在しない間に行われ、Store の状態更新だけが起きる。反映通知の追い越しが起きる余地が無い
- 正: 配信経路が Section・Root・CustomCell で 1 本になる
- 負: core/ADR-0033 の Host 保証に依存する。保証の無い Host と組み合わせると Android で Root の header / footer が表示されない
- 負: 自己計測 wrapper が幅の確定前に計測される機会が増え得る (iOS は幅未確定の間、無限幅で測る)
- 負: 表示中の Root の作り直しで同じ View が残る位置に限り、実体化が配信の後になる
- 負: 実体化が Host 生成の前に走るため、接続と Host 生成の失敗時に破棄すべき実体が増え、後片付けの網羅性が facade の責務になる

## Revisit When

- core/ADR-0033 の Host 保証が取り下げられたとき
- Host の生成が Store の現在状態からの復元で始まらなくなったとき

---
出典: kasane/changes/archive/2026-09-18-android-accessory-view-late-insert-animation/design.md (Decision 1 / Decision 2) / 同 exploration.md (論点4) / 同 deviation.md (後片付け待ちの実体を持つ View の除外・Host 生成の失敗単位・実体化の口の解放)
関連: core/ADR-0033 (前提となる Host 保証) / maui/ADR-0016 (維持する寿命分離と退役順序)
