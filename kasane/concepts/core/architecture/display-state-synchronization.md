---
type: concept
title: 表示状態同期
description: 構造・同一 ID の内容・可視性・Theme を異なる更新経路へ分ける共通原則
tags: [architecture, state, diff, visibility]
timestamp: 2026-08-24
---

この文書は、設定ツリーの変更を Native 表示へ同期する4つの経路を説明する。読むと、値等価と構造 identity を分ける理由、同じ ID の内容更新、visible projection、Theme の扱いが分かる。

ここで Cell model は、Core の Cell 抽象へ準拠し、UI 層が定義する具象の値を指す。`CellStyle` は Core 抽象の要件ではないが、それを持つ具象 Cell model では内容比較の対象になる。platform の再利用行は Native cell / ViewHolder と呼ぶ。3層の関係は [Native Host の責務境界](native-host-boundary.md#cell-の3層) を参照する。

## 同期経路

モデルの値等価と、表示構造を追跡する identity は別の契約である。値等価は通常の値比較とテストに使い、構造判定には Section / Cell ID だけを使う。

| 変化 | 共通の扱い | iOS | Android |
|---|---|---|---|
| 構造 | 追加・削除・移動・ID 変更 | 表示 ID 列を持つ snapshot を更新 | `submitList` へ新しい平坦 list を渡す |
| 内容 | 同じ ID の Cell model 値変更 | item を保ったまま reconfigure | `submitList` の反映完了後に ViewHolder へ最新 model を再適用 |
| 可視性 | hidden を含む model から projection を再構築 | full snapshot | full list |
| Theme | 構造から独立した表示状態 | canvas・表示中 Cell と text 形式 Header / Footer を再評価 (後述の「Theme 更新の適用範囲」) | 行・背景・Section 装飾と text 形式 Header / Footer を再評価 (同左) |

`Cell` の `Equatable` / `equals` は内容を含む通常の値等価として維持する。構造同期だけが内容を無視して ID を比較する。

## Theme 更新の適用範囲 (text / View / Cell)

Theme 変更が表示中の要素へ届く範囲は、両 platform とも次の単一ルールで揃える:

- **Cell** — 表示中の行へ Theme を再適用する (rebind / reconfigure)。
- **text 形式の Header / Footer (Root / Section とも)** — 表示中のまま色・フォントを in-place で再適用する。行 identity・supplementary の再構成は伴わない。
- **View 形式の Header / Footer (`KsAnyView.AndroidView` / `.Compose`、iOS の view accessory)** — Theme 通知の再 bind 対象にしない。再 bind は factory の再実行になり hosted view の内部状態 (入力・フォーカス等) を失わせるためで、Theme を反映したい View accessory は利用者が自分の View を更新する。

full 更新の共通出口 (Android の `setRootDirect`) が theme を取り込むときも、内部フィールドへの代入だけで済ませず、Cell・Root accessory・Section supplementary の各 adapter へ上記ルールどおりの Theme 変更通知を発行する — 通知を欠くと「Store 再接続経由の Theme 変更だけ表示に反映されない」という経路依存の非対称が生じる。

## model と visible projection

model は hidden Section / Cell を含む完全な `SettingsRoot`、visible projection は表示対象だけを元の順序と ID のまま取り出した派生状態である。hidden 対象への更新は model に保持され、再表示時に更新済みの内容が現れる。

可視性の変化を通常の内容更新へ押し込むと、表示行の追加・削除を見落とす。そのため DSL の事前差分確認と Host の防御処理は、可視性差を検出したら full 更新へ切り替える。

## full 更新のコストモデル

両 platform の Native 表示は差分適用基盤の上にある (Android は平坦 list を `submitList` へ渡す DiffUtil、iOS は Diffable Data Source の snapshot)。このため full 更新は「現在の model 全体から作り直す」と言っても、実際に行われるのは旧・新の visible projection の全行を ID で照合し直すこと (全行照合) であり、画面の再描画は差分のある行に限られる:

- Android は新しい平坦 list 全体を `submitList` し、DiffUtil は行の追加・削除・移動 (構造差) を計算する。CellRow は DiffUtil で内容比較しない建付けのため、同一 ID で残る Cell の内容差は共通出口 `setRootDirect` が発行する payload 付き内容通知が補う (後述の「内容更新」節、`android/ADR-0012`)。Section header / footer 行だけは DiffUtil の内容比較で payload rebind へ落ちる
- iOS は新しい snapshot を apply する。snapshot は ID のみで構築されるためセルの内容変化それ自体は snapshot 差分に現れず、同一 ID で残る Cell の内容差は `applyFullSnapshot` が snapshot 適用時の `reconfigureItems` で一括再適用して補う (後述の「内容更新」節)。header / footer が変わった Section だけが `reloadSections` で supplementary を再構成し、固定高さは apply 完了後の `invalidateLayout()` が再評価する (後述の「Section header の固定高さ」節)

部分更新 API (`replaceCell` / `replaceCells` / `updateAccessory`) との違いは再描画コストではなく、全行照合の計算コストと、「何が変わったか」を呼び出し側が特定済みか (特定済みなら照合が要らず、副作用も局所に留まる) にある。変化点を特定できる更新は部分更新 API を使い、変化点を特定できない・可視性が絡む更新だけを full 更新へ流す。

`replaceSection` は API の型として Section 全体の置換であり、header / footer / 固定高さ / 可視性 (`isVisible`) / cells の任意の組み合わせの変化を内包し得る。このため両 platform とも細粒度の差分抽出を試みず、**full 経路で処理する** — 利用者 API `replaceSection` は `SettingsRootDiff` の `ReplaceSection` として Host の diff 適用へ渡り ([SettingsRootDiff による構造変更](../core-model/structural-changes.md))、Android では full 更新の共通出口 `setRootDirect` に、iOS では `applyFullSnapshot` (full snapshot 適用の実体) に合流する。iOS は `.view` が絡む Section に限り追加の強制 reload を行う (後述の「Section accessory の内容更新」節)。局所 API に見えるが実行コストは full 更新と同等以上で、「Section 単位だから安い」という期待で選ぶ API ではない。

## 内容更新

同じ ID の `replaceCell` は同一行の内容再構成を意味する。ID が変わる差し替えは remove + insert などの構造変更で表す。

Android の `replaceCells` は RadioCell など連動する複数 Cell を一回の状態更新へまとめ、一回の `submitList` 完了後に対象行を再 bind する。iOS は Diffable Data Source の item identity を維持して各対象を reconfigure する。ただし同一 ID のまま具象型が変わった Cell (例: `LabelCell → SwitchCell`) は Renderer も変わるため reconfigure では反映できず、iOS の部分更新経路 (`replaceCell` 単発・`replaceCells` バッチ) は full 経路と同じ具象型比較で当該 Cell だけを `reloadItems` の cell 交換へ振り分ける。型変化を reconfigure に流すと UIKit が reuse identifier 不一致の例外でクラッシュする — この検出は full 経路 (後述) と部分更新経路の両方が持つ。

full 更新でも同一 ID の Cell の内容変化は取りこぼさない。Android の full 更新の共通出口 `setRootDirect` は、構造を DiffUtil で反映した後、旧・新 visible projection の双方に存在し値が変わった Cell へ payload 付き内容通知を一括発行して完結する (`android/ADR-0012`)。新規挿入・削除・hidden の Cell へ内容通知は重ねず、内容通知の対象が空でも構造の反映は必ず実行される。

iOS の full 更新も同じ規律で補う。`applyFullSnapshot` は旧・新 visible projection の双方に存在し値が変わった同一 ID の Cell を純粋 helper `FullSnapshotContentTargets` で選び、snapshot 適用時に `reconfigureItems` で内容を再適用する (行 identity は維持される)。同一 ID のまま具象型が変わった Cell は reconfigure では Native cell を維持できないため `reloadItems` の cell 交換で反映し、`reloadSections` で再構成される Section の Cell は reload 側が内容ごと再構成するため対象から除外する。Android と同様、新規挿入・削除・hidden へ内容再適用は重ねず、対象が空でも構造の反映は必ず実行される。

## Section accessory の内容更新

section identity が同じまま Section header / footer の accessory が**非 null から非 null へ**変わる更新 (`updateAccessory`・同一 ID の `replaceSection`・full 更新) は、構造変更ではなく同一行 / supplementary view の内容更新として表示へ反映される。null ↔ 非 null の変化は行の挿入・削除 (構造変更) として扱う。

- Android は `CellListItemDiffCallback` が SectionHeader / SectionFooter に限り `areContentsTheSame` を内容比較にし、payload 付き rebind へ落とす (`android/ADR-0012`)。CellRow の常時 true (内容更新は明示通知で行う建付け) は維持される
- iOS は `applyFullSnapshot` が旧・新 visible projection の header / footer 差分を検出し、当該 Section を `reloadSections` で再構成する

View 形式 accessory の等価判定は view の中身を比較できない。このため:

- Android は `KsAnyView` の参照比較で「別インスタンスへの差し替え」を内容変更として検出する
- iOS の `SectionAccessory` の等価判定は `.view` 同士を常に等価と扱うため、`.full` 更新では `.view` の中身差し替えが表示へ反映されない。`.view` の中身更新は `updateAccessory` 経由が正
- iOS の `replaceSection` は `.view` が絡む Section を強制 reload するため差し替えが反映されるが、その代償として内容が実質不変でも Section 内の全 Cell が再構成される (編集中 Cell の first responder が失われ得る)

## Section header の固定高さ

`Section.headerHeight` も accessory と同じく、section identity が変わらない更新では再 bind または layout の再評価を経てはじめて表示へ反映される。

- Android は `CellListItemDiffCallback` が固定高さの差も内容差として扱い、accessory 内容と同じ payload 付き rebind へ落とす。比較対象は Text accessory の header に限る — 高さを表示へ反映するのは Text accessory だけであり、View accessory で高さ差を内容差とすると `KsAnyView` の View が factory から作り直されて内部状態を失う
- iOS は固定高さを layout の supplementary item サイズとして解決し、visible projection 更新後の `invalidateLayout()` で追従する

宣言的 DSL (Compose / SwiftUI) 経由では、両 platform の `DSLDiffCalculator` が同一 Section ID の固定高さの差を可視性と同型の preflight で検出し、full 更新のみを発行して上記の反映経路へ載せる。固定高さが不変なら preflight は発火せず、通常の差分経路のままになる。

## 保証すること

- 内容を含む値等価を構造 identity の判定に使わない。
- 同じ ID の Cell 内容更新で行 identity を維持する。
- 同一 section identity の accessory 内容変化 (非 null → 非 null) を、どの更新経路でも表示へ反映する。
- 可視性の変化では完全な model から visible projection を再構築する。
- Theme 更新で Section / Cell の ID と構造を変更しない。
- Theme 変更を text 形式の Root / Section Header / Footer へ表示中でも反映する。View 形式の Header / Footer は Theme 通知で再 bind しない (内部状態の保持を優先する)。
- hidden 対象への操作を model に保持する。

## してはいけないこと

- Cell の title、選択値、CellStyle を snapshot / stable item ID に含めない。
- ID が変わる差し替えを同一行の内容更新として扱わない。
- 可視性変更と通常の内容更新を同じ経路へ流さない。
- Theme を構造・内容・可視性のいずれかへ擬装しない。
- `replaceSection` を軽量な部分更新として選ばない (実行コストは full 更新と同等以上)。

## 用語

| 用語 | 意味 |
|---|---|
| model | hidden 要素を含む設定ツリーの完全な状態 |
| visible projection | model から表示対象だけを取り出した派生状態 |
| snapshot | iOS Diffable Data Source が現在の Section / Cell ID 列を保持する表示構造 |
| reconfigure / rebind | 同じ ID の Native 行を破棄せず、最新 Cell model と Theme を再適用すること |
| full 更新 | 現在の model 全体から visible projection を作り直し、Native 表示構造と全行照合して差分だけを再描画する更新 |

## 関連

- [Store の状態と更新通知](store-and-update-streams.md)
- [宣言ツリーの安定 identity](declarative-tree-identity.md)
- [Native Host の責務境界](native-host-boundary.md)
- [SettingsRootDiff による構造変更](../core-model/structural-changes.md)
