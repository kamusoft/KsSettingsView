# Proposal: android-accessory-view-late-insert-animation

## Why

MAUI で Header / Footer に任意 View を置いたページへ遷移すると、Android では遷移直後に View が表示されず、数百 ms 後に行が挿入されて後続の行がスライドダウンする (`../ColorAnalyzer` の AiForms からの移行で見つかった。AiForms には無かった挙動)。Section の `HeaderView` / `FooterView`、Root の header / footer のすべてで起き、Root の header は先頭行のため全行がずれる。

原因は、MAUI facade が View の native 実体化と配信を、初回の設定ツリー配信より後 (MAUI View の `Loaded`) まで遅らせていることにある。text を持たず View だけの header / footer は View が届くまで行が作られない (core/ADR-0023) ため、届いた時点で行の挿入になる。CustomCell の内容も同じ仕組みで遅れて届いており、行は最初からあるが、内容が入った時点で行の高さが変わる。

遅らせる理由として記録されているのは Root の header / footer だけである。Android の Host は取り付け前に Store 購読を張らないため、Store の更新口から渡された Root の header / footer は届かないまま失われ、Store の現在状態にも含まれないので取り付け時の復元でも戻らない。core/ADR-0019 はこれを呼び出し側の責務としたが、この制約が MAUI facade に `Loaded` 待ちを強い、今回の症状を生んでいる。Native を直接使う場合にも、取り付け前に Store の更新口を使うと Android だけ黙って失われる罠として残っている。

## What Changes

- **MAUI: View を最初のフレームから表示する** — Section の `HeaderView` / `FooterView`、Root の header / footer、CustomCell の内容に置いた MAUI View を、`Loaded` を待たずに初回の設定ツリー配信へ間に合わせる。View 配置についての `Loaded` 待ちは無くし、配信経路を Section・Root・CustomCell で 1 本にする。公開 API は変えない
- **Android の Host: 取り付け前の Root の header / footer を失わない** — Host の取り付け前に Store の更新口から渡された Root の header / footer を、取り付け後の最初の表示に反映する。Root の header / footer を Store の現在状態に含めない責務分離 (core/ADR-0005) は保つ。手当ては Host 側に入れ、Bridge だけで吸収しない (core/ADR-0033)
- **iOS の Host: 同じ保証にそろえる** — iOS の Host も view load 前に Store の更新口から渡された Root の header / footer を失う (MAUI 経由で順序に依存しないのは、Handler が Host の view を先に読むことで view load が済むためにすぎない)。Native 直接利用でも失われないよう、Android と同じ保証に合わせる。現行の「Root の header / footer は復元対象外で所有者の再適用により表示される」を固定している回帰テストは新しい保証へ改める
- **変えないもの**: 内容の無い header / footer に行を作らない判定 (core/ADR-0023)、表示済みの画面へ実行時に header / footer を足したときの挿入アニメーション (実行時の内容変化として扱う)、View の寿命と退役順序の契約 (maui/ADR-0016)、CustomCell の埋め込み view の安定性 (maui/ADR-0020)
- 影響能力: maui-core (view accessory の実体化と配信の順序) / maui-cells (CustomCell の Content) / android-host (Host の保証) / ios-host (Host の保証)。Bridge の契約のうち「Android では Root の header / footer の再適用を取り付け後に行う」は不要になるが、Bridge の挙動は変わらないためデルタスペックは置かず、記述の追随は蒸留で行う

## Non-Goals

- **iOS で CustomCell の高さが遷移直後に後から増える症状の解消** (`kasane/changes/ios-customcell-initial-height-grow-animation`) — 原因が「内容の後着」か「幅が決まる前の無限幅での計測」か未確定で、後者なら自己計測 wrapper の計測方式という別の設計判断になるため別 change のままにする。本 change で内容の後着が無くなった後に、あちらで再現確認をして切り分ける
- **実行時に header / footer を足したときの挿入アニメーションの抑止** — 正当な実行時変化であり、抑止するなら両 OS の Host と Bridge に新しい口を足す別の設計判断になるため
- **View が届く前から行の枠だけを出すこと** — core/ADR-0023 が却下済みの「内容が無いが領域だけ出す」にあたるため (core/ADR-0033 の却下案)
- **`skills/` と README の追従** — docs-refresh スキルの責務で、ユーザーの明示依頼により別途行う

## Impact

- **利用者可視の変更**: MAUI の画面で、View を置いた header / footer と CustomCell の内容が遷移直後から正しい位置・高さで表示される。Android で顕著だった遅延挿入とスライドが無くなる。iOS も同じ経路を通るため初回の表示タイミングが変わる
- **公開 API**: 型・シグネチャの変更なし。Host の観察可能な保証が 1 つ増える (取り付け前に渡した Root の header / footer が失われない)。取り付け後に適用する従来の使い方はそのまま有効で、既存の利用者は壊れない
- **既存の決定との関係**: core/ADR-0019 の決定のうち「Root の header / footer は所有者が view load / attach 後に適用する責務」だけを core/ADR-0033 (proposed、amends 0019) で置き換える。maui/ADR-0016 が記録する「Android は attach 前の Root 対象の更新が失われるため attach 後に適用する」は前提が無くなる (決定そのものの改訂が要るかは design で判定する)
- **リスク**: (1) 実体化の前倒しが View の寿命管理 (論理所有と platform lease の分離・Handler 1:1・退役順序) に触れる。Handler の切断 → 再接続 (ページ再表示・タブ切替) をまたぐ挙動を回帰テストで固定する。(2) iOS で自己計測 wrapper の最初の計測が幅未確定のまま行われる機会が増え、初回の高さのブレを持ち込み得る。折り返す Label を持つ FooterView で iOS 実機の退行確認を行う。(3) Android の Host の購読・復元まわりに手が入る
- **`ui/` (brief・モック) は作らない**: 見た目として新たに決める要素が無く、表示タイミングの修正だけのため。確認は実機証跡 (Android Pixel 6a・iOS 実機、遷移直後のフレーム) で行う
- 長命層: `kasane/concepts/maui/architecture/view-materialization.md` (実体化のタイミング)・`kasane/concepts/maui/api/native-bridge.md` (Root の header / footer の再適用順序) ほかの記述が変わる (蒸留で追随)

## 級: L

MAUI facade・Android の Host・iOS の Host の 3 能力にまたがり、両 OS の Host 保証 (core/ADR-0033) とそれを前提にした MAUI 側の `Loaded` 待ち撤去という能力間でそろえる設計判断を含むため (オーナー確定 2026-09-17)。

domain: cross
