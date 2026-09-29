---
id: 0030
title: Host の作り直しをまたぐスクロール位置は、Bridge が控えて次の Host で戻す
status: proposed
date: 2026-09-29
---

## Context

MAUI では、Pop したページと同じインスタンスをもう一度 Push したときや、Android の Activity が作り直されたときに、Handler の切断と再接続で Host が作り直される (ADR-0007)。このとき両 OS でスクロール位置が先頭に戻ることを実測した (kasane/changes/archive/2026-09-17-settingsview-migration-defects/exploration.md の実測表)。上に別のページを Push するだけの通常遷移では Host は作り直されず、Android での付け外しの問題は同じ change で Native 側を直して解決済みである。

スクロール位置は Store の状態ではなく (core/ADR-0037)、Store の現在状態から表示を復元する経路 (core/ADR-0019) には乗らない。Store の外の表示状態を Host の世代をまたいで保つ先例はスタイル切替 (ADR-0023) で、Host の世代をまたいで生きる Bridge が値を持ち、Host の生成時に当て直す。gateway は Host の解放をまたいで作り直されないため、facade が再接続時に値を再送する形では成立しない。

KsCollectionView (スクロール制御の形の踏襲元) には、Host の作り直しをまたいで位置を保つ先例は無い。

前提: MAUI の SettingsView と Bridge が、Handler の切断と再接続をまたいで生き続けること (ADR-0007)。

## Decision

Host の作り直しをまたいで、スクロール位置を保つ。Bridge は Host を手放すときに表示位置を控え、次の Host で表示が整ったらその位置へ戻す。位置は先頭に見えている要素の ID と、そこからのずれで控える。

復元は、準備完了の合図 (ADR-0029 のハンドルが命令を受け付けられるようになったことを ViewModel に知らせる合図) の中で出した命令より先に処理する。合図は命令が効く時点で出し、復元が画面に反映し終わるのは待たない。ViewModel が合図の中で出した命令は、復元の後に実行される。

含まないもの: Native に足す「位置を控える・戻す」窓口の形と公開の範囲、控えた要素が離れている間に消えていたときの戻り先、MAUI を使わない Android の View Host が Activity の作り直しで位置を戻す仕組み (設計。出典の agenda の決定事項)。

## Alternatives Considered

- 復元が画面に反映し終わってから準備完了の合図を出す: ViewModel は位置を読めないため、反映完了を待つ利得が無い。命令の順序は待ち行列で保証でき、Native から MAUI まで復元完了の通知経路を足す費用だけが残るため却下 (オーナー判断)。
- 保たない (Host の作り直しを新しい画面とみなし、先頭から表示する): 利用者から見れば同じページに戻っただけなのに先頭へ飛び、不具合に見えるため却下。
- 利用者に任せる (ハンドルに位置を読む命令を足し、利用者が控えて準備完了の合図で戻す): KsCollectionView に無い語彙が増え、利用者に控えて戻すコードを書かせることになるため却下。
- facade が位置を控えて再接続時に再送する: gateway は Host の解放をまたいで作り直されず、再送の契機が無いため却下 (ADR-0023 と同じ理由)。

## Consequences

- 正: 同じページに戻ったときや Activity が作り直されたときも元の位置に戻り、通常遷移での位置の保持と体験がそろう。
- 正: 位置を要素の ID で控えるため、離れている間に項目が増減しても同じ要素の場所へ戻れる。
- 正: 合図の中で出した命令が復元より後に処理されるので、ViewModel の命令が最後に勝つ。復元の完了を知らせる経路を別に持たずに済む。
- 負: Bridge が Store の外の表示状態をもう 1 つ持つことになり、スタイル切替と合わせて Host の生成時に当て直す値が増える。
- 負: Native に「位置を控える・戻す」窓口が要り、両 OS の Host と Bridge の公開面が増える。

## Revisit When

- 前提 (Context) が崩れたとき

---
出典: kasane/roadmaps/maui-support/phases/phase-8-scroll-control/history.md (2026-09-29: Host の作り直しをまたぐスクロール位置) / kasane/changes/archive/2026-09-17-settingsview-migration-defects/exploration.md (実測表)
関連: ADR-0023 (Bridge が Host の外で値を持つ先例) / core/ADR-0037 (Store を経由しない命令ハンドル) / ADR-0029 (MAUI の命令ハンドルと準備完了の合図)
