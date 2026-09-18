# Design: android-accessory-view-late-insert-animation

## Context

MAUI facade は、配置された MAUI View (Section の header / footer、Root の header / footer、CustomCell の内容) の native 実体化と配信を、Host が view 階層へ取り付けられた後 (`Loaded`) にまとめて行っている。現行の順序は次のとおり。

| 順 | 処理 | View の状態 |
|---|---|---|
| 1 | Handler が Host を作る直前に facade を接続する。初回はここで設定ツリー全体を Store へ配信する | 実体化の口がまだ無く、View は 1 つも実体化されていない。配信データに View は載らない |
| 2 | 実体化の口を差し込む | 差し込むだけで、既存の配置は実体化しない |
| 3 | Host を作る。Host は Store の現在状態から表示を復元する | View なしの表示で復元される |
| 4 | `Loaded` を受けて、Root → 全 Section → 全 CustomCell の順に実体化して配信し直す | ここで初めて View が届く |

4 が 3 より遅いのは、Root の header / footer が「Host 単位のプロパティ」で Store の現在状態に含まれず (core/ADR-0005)、Android の Host が取り付け前に Store 購読を張らないため、取り付け前に渡すと失われるからである (core/ADR-0019、maui/ADR-0016)。この 1 つの制約のために、Store の現在状態である Section の header / footer と CustomCell の内容まで一律に遅らされている。

確認済みの事実:

- 実体化の前提 (`MauiContext`) は 1 の時点で揃っている。自己計測 wrapper は platform view の親も Host も要求しない
- 配信データへ View を詰める箇所は、配置が持つ実体を引き当てるだけで自分では実体化しない。1 より前に実体化してあれば、Section の header / footer と CustomCell の内容は初回の配信データに載る
- Root の header / footer は配信データに載る場所が無く、必ず単発の更新口 (Store の `updateAccessory`) を通る。Store は Root 対象を状態として持たず通知だけを流し、通知は再生されない
- Handler の切断 → 再接続 (ページ再表示・タブ切替) では facade は接続済みのため 1 の全体配信は走らない。Host の解放時に Section の View は text (無ければ解除) へ書き戻され、実体は退役している
- iOS の Host は生成時から Store を購読しているが、view load 前に届いた更新は全体差し替え以外を捨てる。Root 対象も捨てられる。MAUI 経由で順序に依存しないのは、Handler が Host の view を先に読んで view load が済むためである
- Android の Host は取り付け中だけ Store を購読する (`lifecycleScope`)。取り外し中に届いた Root 対象の更新も同じく失われる

## Goals / Non-Goals

**Goals**

- MAUI で配置した View が、遷移直後の最初のフレームから正しい位置・高さで表示される (初回接続・再接続の両方)
- 両 OS の Host が、購読できていない間に Store の更新口から渡された Root の header / footer を失わない (core/ADR-0033)
- View の寿命の契約 (論理所有と platform lease の分離・Handler 1:1・退役順序) を保つ

**Non-Goals** は proposal.md のとおり (iOS の CustomCell 高さ症状・実行時の挿入アニメーション抑止・枠だけの行)。

## Decisions

### Decision 1: MAUI facade は View の実体化と Store への配信を Host 生成の前に済ませる

**採用案:** 実体化の口の差し込みを接続より前へ移し、Store の現在状態になるもの (Section の header / footer・CustomCell の内容) は Host を作る前に実体化して Store へ届ける。Host は生成時に Store の現在状態から復元するので、最初の表示に View が含まれる。

| 場面 | 手順 |
|---|---|
| 初回接続 | 口を差し込む → 設定ツリー全体の配信の中で、配信データを組む前に全配置を実体化する → Host を作る |
| 再接続 (接続済み) | 口を差し込む → 全 Section の slot を実体化して更新口で Store へ届ける → 全 CustomCell の内容を実体化して 1 バッチで届ける → Host を作る |
| 接続後の配置・差し替え | 現行どおり、その場で実体化して配信する |

- 実体化は controller が行い、gateway が配信データを組む途中では行わない (Handler 注入 seam・controller 所有・gateway 輸送の三層を保つ。maui/ADR-0016)
- 論理所有の確定 → 旧実体の先行破棄 → 実体化 → 配信、の順序は現行のまま保つ
- 再接続時の配信は Host が存在しない間に行われるため、Store の状態更新だけが起き、表示への反映通知は発生しない。Section の slot を 1 件ずつ届けても、反映通知の追い越し (CustomCell で実測された問題) は起きない

**理由:** Host の生成が Store からの復元で始まる以上、復元より前に Store へ届けるのが、最初のフレームに間に合わせる最短の経路になる。取り付けを待つ理由は Root だけにあり (Decision 2 と 3 で解消)、Section と CustomCell には元々無い。

**代替案:**
- **A: `Loaded` 待ちは残し、初回接続の全体配信にだけ View を載せる** — 再接続では全体配信が走らないため、ページ再表示・タブ切替で後着が残る。経路も 2 本になる
- **B: 配信データを組む箇所で、未実体化の配置をその場で実体化する** — gateway の輸送層で Handler 生成という重い副作用が走り、三層構造 (maui/ADR-0016) を崩す

### Decision 2: Root の header / footer は Host 生成の直後、取り付けを待たずに適用する

**採用案:** Handler は Host を作った直後 (platform view を返す前) に Root の header / footer を実体化して更新口で届ける。`Loaded` では親子関係の成立確定 (iOS の ViewController 包含) だけを行う。

**理由:** Root は Store の現在状態に含まれないため、Host が存在しない間に届けても受け手がいない。Host 生成後・取り付け前が、最初のフレームに間に合う最も早い時点になる。この時点の更新を Host が失わないことは Decision 3 と 4 が保証する。

**代替案:**
- **A: Root だけ `Loaded` 後のまま残す** — Root の header は先頭行で、後着すると全行がずれる。探索で却下済み (core/ADR-0033 の却下案)
- **B: Bridge が Root の最新値を控え、Host 生成時に Host のプロパティへ直接渡す** — MAUI 利用者しか救わず、Native 直接利用者に罠が残る。探索で却下済み (core/ADR-0033 の却下案)

### Decision 3: Android の Host は Store に結び付いている間、Root 対象の更新を同期の受け口で受け取る

**採用案:** Store は Root の header / footer を対象とする更新を、結び付いている Host へ**同期に**直接知らせる受け口を持つ (モジュール内部の口)。Host は `bind` の中で受け口を登録し、`unbind` と別 Store への `bind` で外す。受け口は知らせを受けた場で Host のプロパティへ値を控える。控えた値は Host のプロパティなので、取り付け時の表示にそのまま現れる。

- 受け口へ知らせる内容は、通知と同じ更新の値 (`SettingsRootDiff` の accessory 更新) そのものとし、Host は既存の適用処理 (`applyDiff`) と同じ反映を行う。更新の型と「1 操作につき 1 回適用する」境界 (core/ADR-0006) は変えず、Root 対象の配送路だけを分ける。公開されている `applyDiff` へ accessory 更新を直接渡す使い方は従来どおり有効とする
- 登録は Host ごとに 1 件とする。同じ Store へ複数の Host が結び付いている場合は、すべての Host へ知らせる。同じ Host が同じ Store へ `bind` し直しても登録は増えない。ある Host の `unbind`・回収は、他の Host の登録に影響しない
- Store の更新口は、現行どおりどのスレッドから呼んでもよい (オーナー確定 2026-09-17)。受け口は渡された値をスレッド安全に控えるだけを必ず行い、表示への反映はメインスレッドで行う: 呼び出しがメインスレッドならその場で反映し、それ以外ならメインスレッドへ送る。取り付け時は、控えたまま未反映の値を先に反映する
- ある Host での反映が失敗しても、記録に残したうえで、他の Host へ知らせることと Store の通知の発行は続ける (失敗を更新口の呼び出し元へ伝えない)
- Root 対象の反映はこの受け口だけが行う。取り付け中の通知の購読 (`diffs`) は、受け取った更新が Root 対象なら適用処理へ渡さずに読み飛ばす。反映経路が 1 本なので、取り付け状態による切り替えも二重適用の調停も要らない
- Store が持つのは受け口の登録だけで、Root の header / footer の値は持たない。登録は Host を弱参照し、参照が切れた登録は次に知らせる時点で取り除く
- Store の通知 (`diffs`) への Root 対象の発行は現行のまま残す (core/ADR-0020 の「Root 系は無条件に通知を発行する」を変えない)
- 構造の更新・内容更新・Theme は現行どおり取り付け中だけ購読し、取り付け時に Store の現在状態から復元する (core/ADR-0019 のまま)

**理由:** Store の通知は再生されず、容量に上限があり、送信の失敗を無視する。Section や Cell は取りこぼしても Store の現在状態から復元できるが、Root は Store に値が無いため復元できない。通知に乗せるかぎり「`bind` から戻った直後に渡された値」と「容量を超える連続更新の最後の値」を保証できない。MAUI facade は Host を作った直後に同じ呼び出しの中で Root を渡すため (Decision 2)、前者は主経路そのものにあたる。同期の受け口なら、`bind` から戻った時点で有効であり、最後に渡された値が必ず残る。

**代替案:**
- **A: Store が Root の最新値を状態の外の別フィールドに控え、Host が取り付け時に読む** — core/ADR-0005 が却下した「Root Header・Footer を Store のプロパティにする案 (Store の責務肥大)」にあたる。却下理由は現在も有効
- **B: Store の更新通知を再生可能にする (replay)** — Root 以外の構造更新まで再生され、取り付け時の現在状態からの復元と二重適用になる
- **C: Host が `bind` の時点から通知を購読する (取り付け状態に関係なく Root 対象だけ拾う)** — 購読の開始が `bind` から戻る時点に間に合う保証が無く、容量を超える連続更新で最後の値を落とし得る。取り付け中の購読との二重適用の調停も要る
- **E: Root 対象の更新口をメインスレッド限定の契約にして、受け口がその場で反映する** — 仕組みは単純になるが、公開 API に新しい制約が増え、メインスレッド以外から呼んでいた Native 利用コードを壊す。不具合修正に伴って利用者の契約を狭めることになるため採らない
- **D: Host が `bind` の時点から全更新を購読する** — C と同じ欠点に加え、表示の無い間に差分適用を走らせる意味が無い (構造の更新は現在状態からの復元で既に収束する。core/ADR-0019)

### Decision 4: iOS の Host は view load 前に届いた Root 対象の更新を控え、view load 時の構築に使う

**採用案:** view load 前に届いた更新のうち Root の header / footer を対象とするものは、捨てずに Host のプロパティへ控える。控える際に view load を誘発しない (レイアウトの作り直しや可視領域の再描画は view load 済みのときだけ行う)。view load 時の構築は控えた値を使う。

**理由:** iOS は購読が生成時から張られているので、足りないのは「view load 前に届いた Root 対象を捨てない」ことだけである。

**代替案:**
- **A: Bridge の Host 生成で view load を強制する** — Bridge 経由しか救わず、Native 直接利用で view load 前に更新口を使う場合が残る

### Decision 5: 現行の順序を固定している回帰テストは、新しい保証を固定する形に改める

**採用案:**
- MAUI: 「取り付け前は内容が配信されない」「取り付け後に Root を適用する」を固定しているテストを、「Host 生成の前に Store へ届いている」「Root は Host 生成直後に適用される」へ改める。テスト足場の「取り付け」操作は、親子関係の確定だけを表すものへ役割を変える
- iOS: 「Root の header / footer は復元対象外で、所有者の再適用により表示される」を、「view load 前に更新口から渡した Root の header / footer が view load 後に表示される」へ改める
- Android: 取り付け順序の回帰テストに、Root の header / footer の項目を新設する (取り付け前・取り外し中・`bind` 直後・容量を超える連続更新・別 Store への再 bind・適用回数・Host の非延命)

**理由:** これらのテストは覆す対象の契約そのものを固定しているため、書き換えが契約変更の可視化点になる。

**代替案:** なし (テストを残したまま実装を変えることはできない)

## Risks / Trade-offs

- **iOS の初回計測**: 自己計測 wrapper は幅が未確定の間、無限幅で測る。実体化が早まることで、幅確定前に測られる機会が増え得る。幅の確定後に測り直す経路は既にあるため表示は収束する見込みだが、最初のフレームで高さがぶれるかは実機でしか分からない。iOS 実機で Android と同じ項目を確認し、view accessory に修正前には無かった高さの変動が出た場合は本 change の完了を止めてオーナーへ諮る。CustomCell の行の高さだけが後から変わる既存の現象は `kasane/changes/ios-customcell-initial-height-grow-animation` の対象であり、本 change では「Content が後着していないこと」を合否とする (本 change では計測方式を変えない)
- **Android の受け口の寿命**: `unbind` されずに手放された Host について、登録が次に Root 対象を知らせる時点まで Store に残る。登録は Host を弱参照するため、Host 本体は延命されない。これを回帰テストで固定する
- **取り付け前の Handler 生成**: Host 生成前に MAUI View の Handler を作ることになる。現行でも再接続時 (取り付け済みで接続) は同じ時点で作っており、初回接続でも `MauiContext` は揃っている
- **テストの書き換え範囲**: MAUI のテスト足場の「取り付け」を呼ぶテストが広範囲にあり、期待の前後関係が変わるものが出る

## Migration Plan

公開 API の変更は無く、利用者の移行は不要。取り付け後に Root の header / footer を適用している既存の利用コードはそのまま動く。

実装順は Host 側 (Decision 3・4) を先に行う。MAUI 側 (Decision 2) は Host の保証を前提にするため、先に入れると Android で Root が表示されなくなる。

## Open Questions

- 実行時に複数の accessory を同じ操作で差し替えたとき、Android で反映通知の追い越しが起きるか。初回と再接続では Decision 1 により起きないが、実行時の連続差し替えは現行のまま単発配信である。回帰テストで確認し、再現した場合は本 change 内で 1 バッチ化する (判断が要る規模ならオーナーに諮る)

## ADR 候補

- Decision 3・4 — core/ADR-0033 (proposed) に反映済み。取り付け前の保持を「Store から Host への同期の受け口」で実現し Store に値を持たせないことは、core/ADR-0005 の責務分離を将来にわたって保つ判断にあたる (境界を越える / 将来を制約)
- Decision 1・2 — maui/ADR-0027 (proposed、amends maui/ADR-0016) として起票済み。maui/ADR-0016 の「Handler 再接続時は Host 取り付け後に再実体化して再発行する」を置き換える (境界を越える: Handler・controller・Bridge 契約の順序)
- Decision 5 — 該当なし (テストの追随)
