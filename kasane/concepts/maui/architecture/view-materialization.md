---
type: concept
title: MauiView の native 実体化機構 (materializer seam と platform lease)
description: VisualElement を platform view へ実体化する facade 内の共有基盤 — seam 契約・自己計測 wrapper・論理所有と lease の寿命分離・退役順序・native への埋め込みの継ぎ目
tags: [maui, materialization, handler, lifecycle]
timestamp: 2026-08-22
---

# MauiView の native 実体化機構 (materializer seam と platform lease)

この文書を読むと、facade が任意の MauiView (`VisualElement`) を native の platform view (UIView / Android View) に実体化して表示へ届ける共有基盤の契約と寿命規律が分かる。この機構に載る用途は2つ — accessory View (Root / Section の Header・Footer) と `CustomCell.Content` (行の内容) で、後者は accessory で確立した機構を拡張する形で載っている。決定の経緯は maui/ADR-0016〜0018 (accessory) と maui/ADR-0020 (cell content)。利用者から見える公開契約は [表示への反映と Host の寿命](../api/maui-rendering-lifecycle.md) と [Cell の MAUI 表現](../api/maui-cells.md) (CustomCell)、実体の輸送は [MAUI Native Bridge の interop 境界](../api/native-bridge.md)、Store と Host の一般契約は [Store の状態と更新通知](../../core/architecture/store-and-update-streams.md) を参照。

本文中の用語: **seam** = platform 依存を型で切り出した差し替え点 (maui/ADR-0009 の gateway seam と同じ考え方)。platform ごとに実装を差し替え、platform を持たない TFM (ターゲットフレームワーク) である素の net10.0 では fake を注入してテストする。**Host** = native 側の SettingsView 実体 (iOS は ViewController / Android は View)。MAUI の Handler (facade と platform を繋ぐ MAUI 標準機構) の接続で生成され、切断で解放される。**変換経路** = facade の Section / Cell ツリーを Bridge の写し (DTO) へ変換して native の Store へ配信する経路で、View を置くプロパティの配置・解除もここを通って native へ届く。**輸送** = interop 境界を越えて platform 実体そのものを native へ渡すこと。

## 三層の責務

| 層 | 実体 | 責務 |
|---|---|---|
| Handler | `SettingsViewHandler` | Handler 接続時に per-TFM の materializer seam (`IKsViewMaterializer`) を注入する。`IMauiContext` は seam 内に閉じ込め、controller には見せない |
| controller | `KsSettingsController` | 実体化のタイミングと寿命を所有する。置かれた View と lease を持つ所有表を用途ごとに2つ持ち、accessory は slot (`KsAccessorySlot`、Root / Section × header / footer の配置座標) を鍵に、cell content は CustomCell を鍵に引く。破棄待ちの wrapper を native への配信が済むまで溜める退役キューと、同一 View の多重配置を弾く検査表は両用途で共通 |
| gateway | `IKsSettingsGateway` / per-TFM 実装の `KsBridgeGateway` ([native-bridge.md の責務境界表](../api/native-bridge.md) 参照) | 実体を型なし参照 (`object?`) のまま受け取り、platform 型へキャストして Bridge へ輸送する |

fake seam と fake gateway を注入すれば net10.0 単体でこの機構の変換ロジックをテストできる (maui/ADR-0009 のテスト戦略)。

## seam 契約

`IKsViewLease Materialize(View view, Action measureInvalidated)` — lease は platform 実体と破棄手順を一体で持ち、破棄は lease に対して行う (実体を直接触らない)。`measureInvalidated` は実体側の計測無効化を呼び出し元へ通知するコールバックで、後述のサイズ変化伝播の起点になる。

**呼び出し側の前提条件**: `Materialize` を呼ぶ時点で View の論理所有 (`Parent` と継承 `BindingContext`) は確定済みであること。Handler は必ず BindingContext の定まった View に対して作られる (MAUI 公式骨格の順序)。

### 産物は自己計測 wrapper

産物は bare な platform view ではなく**自己計測 wrapper** — iOS は MAUI 本体の iOS 側基底クラス `MauiView` + `ICrossPlatformLayout` の自前サブクラス、Android は MAUI 本体の `ItemContentView` と同じ役割を果たす自前の `ViewGroup` サブクラス (いずれも `KsAccessoryHostView`。名前は accessory 由来だが、cell content もこの同じ型を使う — 用途共通の基盤として流用しており、用途ごとの分岐は持たない)。計測 (`IView.Measure`) / arrange / `MeasureInvalidated` の native への中継 / Handler の破棄を wrapper が自蔵する。

iOS の wrapper は `IntrinsicContentSize` を override し、`MeasureInvalidated` で `InvalidateIntrinsicContentSize()` を呼ばなければならない — iOS の accessory 自動高さは Auto Layout 経由でしか決まらず、`SizeThatFits` の override だけでは領域が潰れる (実測。maui/ADR-0016)。

## 論理所有と platform lease の寿命分離

| | 論理所有 | platform lease |
|---|---|---|
| 実体 | logical tree 接続 + 継承 BindingContext (共通処理は `KsAccessoryViewOwnership`) | wrapper + Handler |
| 寿命 | **View を置くプロパティの寿命** — 配置時に確定し、解除・差し替え・所有者の削除で解放する | **Host 世代の寿命** — Handler 切断 (= Host 解放) で破棄し、再接続時に新しい MauiContext で再実体化して明示経路で再発行する |
| Handler との関係 | Handler の有無に依らず維持されるため、XAML 構築時や Host 解放中も BindingContext の継承と変更伝播が働く | 復元の正は facade が所有する VisualElement であり、platform 実体は世代ごとの派生物 |

設定ツリーに未参加の所有者 (XAML 構築中の Section 等) の受け皿経路 (設定ツリーに参加するまで配置を預かる経路) は、**他所に所有されている View を引き取らない** — 既存配置を黙って奪う代わりに、所有者が変換経路へ参加した時点で多重配置例外になり、既存配置は無傷で残る。

### cell content の検査時点

cell content も同じ契約に乗る (論理所有は `KsAccessoryViewOwnership` — こちらも名前は accessory 由来だが両用途で共用する — が担い、多重配置は accessory と同じ検査表で判定する)。ただし検査の**時点**が異なり、`CustomCell.Content` は BindableProperty の `validateValue` で**値が確定する前に**問い合わせる (`IKsCellContentGuard`)。値が確定してからでは、それまでの内容の論理所有が先に解かれており、後から多重配置を見つけても元へ戻せない — `BindableObject` は変更通知中の再 set を後回しにするため、値を書き戻す形のロールバックも当てにできない。検査を通った後の論理所有の確定は、変更通知を受けた変換経路が行う。

検査の失敗は `validateValue` の false 返却ではなく `InvalidOperationException` の送出で表す。false を返すと BindableProperty 側が `ArgumentException` に変換してしまい、多重配置が公開契約どおりの例外型で観測できなくなる。

## cell content の世代トークン

native の CustomCell は「content 値 + その値から行の中身を組み立てる builder」の組で表され、content 値の値等価で再バインド (builder の再実行) の要否を決める。ところが MAUI の View は binding で生きたまま中身が変わるため、View 自身を content 値にできない (値比較の経路へ view の変更を流さない — maui/ADR-0018 と同じ規律)。そこで controller が単調増加の**世代トークン**を振り、native の content にはトークンだけを載せる (`CustomCell.ContentToken`。写しに載るのもトークンだけで、View 実体は gateway が輸送の直前に、Cell を鍵に controller の所有表から引き当てる)。

トークンは**参照が入れ替わるたびに必ず変わる**。設定・差し替え・null 化・null のままの Host 再接続・Host 世代の作り直しのいずれでも振り直す。振り直しを飛ばすと native から見て「内容は変わっていない」ことになり、退役済みの platform view を指したまま表示が固まる。

逆に**同一トークンの間は埋め込み platform view のインスタンスが安定する**。保証しているのはインスタンスの安定であって再バインドの抑止ではない — native 側の再バインドは style / showArrow / isEnabled / isVisible の変更や再配信でも起きるが、そのたびに定数返しの builder が同じ実体を返すため、破棄も再実体化も Handler 切断も起きない。

## 退役順序

旧実体の退役は全経路 (差し替え / null 化 / 所有者の削除・置換 / Root 再構築 / Host 切断) で「**Store 更新 → native への配信 → 旧 wrapper 破棄**」の順序を守る — native が旧実体を子 view として保持している間に破棄する窓を作らない。破棄手順は購読解除 → platform view を superview から外す (iOS は退役したセルが旧 view を掴んだまま自動 detach しない) → `DisconnectHandlers`。

同一 View を包み直す際は、**先に**その View を掴む退役待ち実体を破棄する — Handler は VisualElement と 1:1 のため、後から破棄すると再実体化済みの新しい Handler ごと切断される。

### Host 解放時の cell content

Host 解放 (`ReleaseHost`) の cell content は、**内容なしの世代を振って配信し直してから** lease を破棄する。Host が解放されても Cell 自身は Store に残り続けるため、送り直しを省くと Store が退役済みの実体を指したままになり、Host 再接続時に死んだ platform view を触り得る。

### 複数 Cell の配信は 1 バッチ

**複数 Cell の内容を同時に配信するときは 1 バッチにまとめる** (`KsSettingsController.DeliverCellContents` → gateway の `ReplaceCells`)。1 件ずつ単発の `ReplaceCell` を連ねると、Android では先行する更新の反映通知が後続の更新に追い越されて破棄され、表示が古い世代 — 内容の View を持たない世代 — のまま止まる (Host 再接続で複数行をまとめて送り直す経路で実測)。対象が 1 件のときだけ単発経路をそのまま使う。

### icon の lease も同じ順序

**icon の `KsImageLease` も同じ退役キューで同じ順序に従う** — 解決済み platform 画像 (`UIImage` / `Drawable`) を包む lease は wrapper とは別の機構だが、置換・Cell 除去・Root 再構築・接続解除のいずれでも「native への配信 → 破棄」を守る (maui/ADR-0015)。lease の破棄は画像そのものではなく画像解決サービスが登録した後片付けを走らせるため、先に破棄すると表示中の画像の裏付けが失われる (Android の bitmap 解放が最悪ケース)。

## native への埋め込みの継ぎ目 (cell content)

輸送された platform view を native の宣言 UI へ埋め込む部分は、両 OS とも interop 固有の吸収層を必要とした。accessory (領域を native Host が直接抱える) には無い問題で、いずれも**行の再利用**が原因になっている。

### iOS — 行ごとの入れ物

representable (SwiftUI へ UIKit view を橋渡しする `UIViewRepresentable` — `KsBridgeCellContentView`。同名の型が Android 側にもあるが per-platform の別実装) は輸送された共有 view を直接返さず、**行ごとに作る入れ物** (`KsBridgeCellContentHostView`) を返し、view はその子として取り付ける。以下、「入れ物」= この行ごとの host view、「表に出ている」= window に載っていて自分にも祖先にも隠されたものがない状態を指す。SwiftUI の後片付けは遅れて走るため、共有 view を直接返す形だと、前の行の後片付けが後から走ったときに表示中の行から内容が外れて空行になる (実機で再現・確定)。内容の実体は1つきりで入れ物の間を移動するため、「どの入れ物が抱えるか」の規則を入れ物側に一箇所で持つ:

- 行の内容として作られたばかりの入れ物 (`makeUIView`) は無条件に引き取る
- それ以外の機会では、内容がどこにも付いていないか、抱えている相手が表に出ておらず、かつ自分が表に出ているときだけ引き取る — **表に出ている入れ物からは決して奪わない**ため、2つの入れ物が取り合って振動しない
- 引き取りを確かめる機会は配置任せにしない。内容を外された合図 (`willRemoveSubview`) と表示への出入り (`didMoveToWindow`) で配置を予約し、`didMoveToWindow` では生存中の入れ物の弱参照一覧をたどって**同じ内容を待つ入れ物にも確かめ直させる** (抱え主が表から外れたことは、待っている側には何も届かないため)

### Android — key(token) と行タップの返し

埋め込み (`KsBridgeCellContentView.Content`) は `AndroidView` を `key(token)` で包む。`AndroidView` の factory は同じ呼び出し位置につき一度しか呼ばれず、トークンを `key` へ与えないとトークンが変わっても古い view が行に残る (実測)。さらに、この埋め込みは自分の占める領域全体をポインタ入力の受け口として登録するため、**タッチが消費されたかどうかではなく、当たったかどうか**で行の click listener を遮る — 内容の上のタップが行タップとして発火しない。継ぎ目の外側 (`AndroidView` の modifier) に `detectTapGestures` を置いて行タップを返す:

- 内容の中の要素がタッチを引き取ったときはポインタの変化が消費済みになって検出が始まらないため、内容の操作と行タップは二重発火しない (行が無効なときも、埋め込みを内包する native 側の行が先にポインタを消費するため始動しない)
- 検出ノードはタップ購読の有無によらず常設し、通知先だけを差し替える。購読の有無で modifier の構成が変わると埋め込みが作り直され、内容の view が付け替わってしまう

この遮りは Compose の interop そのものの性質であり、行タップを返す仕掛けは bridge の MAUI 経路にしか置かれていない。**native の利用者が CustomCell の content に `AndroidView` を直接置くと、同じ理由で行タップ (`CustomCellViewHolder` の click listener) が発火しない** — native UI 層は未対応で、既知の制約として扱う。

### Android に iOS のような行ごとの入れ物が無い理由 (構造根拠・検証済み)

Compose の interop は factory が返した view を framework 自身が行ごとに作る ViewGroup (`AndroidViewHolder`) の子として抱えるため、行ごとの入れ物に相当する構造は interop 側に既に存在する。そのうえで、iOS の空行レースの成立条件だった「後片付け経路に子側 detach (現在の親がどこであれ実体を剥がす操作。UIKit の `removeFromSuperview` 相当) があり、それが新しい行の引き取りより後に遅延実行され得る」が Android には成立しない。

Compose の破棄チェーンはすべて親側・自己スコープ — `layoutNode.onDetach` は `removeAllViewsInLayout()` (自分の子だけを外す)、node 破棄の `onRelease` は view 操作を一切しない (実体は退役した holder の子のまま残り、次に表示する行の factory が引き取る)、holder の除去 (`removeAndroidView`) も holder 自身を外すだけ。別の行へ移った実体には構造的に届かない (compose-ui 1.7.5 ソースで確認)。子側の操作は「これから表示する行」方向にしか存在しない — 埋め込み factory の detach→返却と、wrapper (`KsAccessoryHostView`) 構築時の `RemoveFromParent()`。ViewGroup の単一親不変条件 (親を持つ子への `addView` は例外) により親替えは必ず現在の親の `removeView` を経由するため、退役側が後から表示中の行へ手を出す経路自体が無い。

reuse 経路 (`AndroidViewHolder.onReuse` には子側の `addView` があり、他の行に奪われた実体へ実行されると例外になり得る) は、埋め込みが `onReset` を渡していない → 非 reusable (`ComposeNode`) のため通らない。根拠は compose-runtime の再利用分岐 — Composer は onReset を持たないノードを再利用対象から外し、reuse の機会でも**強制置換 (破棄して作り直し)** する。native CustomCell が `ReusableContent` で中身をリサイクルするようになった ([android/ADR-0015](../../../decisions/android/0015-customcell-pool-aware-composition-disposal.md)) 後もこの前提は変わらない。**`AndroidView` へ `onReset` を与える変更はこの前提を壊す**ので、加える場合はこの節を確かめ直すこと。

deactivate 経路 (`AndroidViewHolder.onDeactivate` = `removeAllViewsInLayout`) は android/ADR-0015 以降、プール投入時の非活性化で埋め込みに対して**新たに走るようになった**。これも親側・自己スコープの操作で別の行の実体には届かない。deactivate を明示的に通す Bridge 専用回帰テスト (`KsBridgeCustomCellDeactivateTest`) と実機高速フリック再検証 (2026-08-16、エミュ 583 + Pixel 6a 267 ジェスチャで空行・例外 0) で裏取り済み。

### 実証と再検証の条件

エミュレータ実証 (2026-08-13、API 35 / compose-ui 1.7.5): CustomCellDemo 全域の高速フリック往復 2 セッション (計約 600 フリック・約 100 検査点、iOS 再現手順と同じ高速フリック+遅いドラッグの組を含む) で空行 0 件・埋め込み起因の例外 0 件 (iOS は修正前およそ 10 スワイプ中 2 件)。この根拠は compose-ui の実装詳細に依存するため、Compose BOM のメジャー更新時は破棄チェーン (`onDetach` / `onRelease`) に子側 detach が増えていないかを確かめ直すこと。

## サイズ変化の伝播

用途によって届け先が違う。**accessory** は領域の高さを native Host が抱えるため、wrapper の `MeasureInvalidated` を facade で集約し、`invalidateAccessoryMeasurement` で native の高さ再計算へ届ける (maui/ADR-0018)。**cell content** は wrapper 自身の計測無効化だけで両 OS の行高さが追従する — 行高さは native CustomCell の self-sizing で決まるため、native 側に cell 版の再計測 API を足す必要がなかった (追従しない場合の対照を取ったうえで実測確認済み。maui/ADR-0020)。cell content の materialize に渡す `measureInvalidated` が何もしないのはこのためである。

## 用途をまたぐ再利用の規律

用途固有のポリシー (計測キャッシュ・幅補正等) は共有部の上に載せ、wrapper 本体に用途分岐を持ち込まない (maui/ADR-0016 — 原典 AiForms.Maui.SettingsView ([原典参照の規約](../../../handbook/cross/aiforms-origin-reference.md)) が両用途をコピー分岐で持ち保守負債化している構造を反面教師とする)。

更新セマンティクスは両用途で「参照が正・内容は live」(maui/ADR-0018・0020): 別インスタンスへの差し替えのときだけ native へ送り直し、内容の変化そのものは binding に任せる (前掲の世代トークンは、この規律を cell content で成立させるための仕掛け)。

### テストの規律

テストの fake materializer は **Handler 1:1 の共有** (接続中は再利用・切断済みなら作り直し) を模擬すること — 模擬しない fake は退役順序の誤りを検出できない。

退役順序のテストは「破棄されたか」ではなく「**破棄の瞬間に native への配信が済んでいたか**」を観測する — 破棄の口に観測用の後片付けを差し込み、その中で gateway 呼び出しの記録を写し取って対象の配信が含まれることを assert する。到達順だけを見るテストは順序が逆でも緑になる。icon 側は特にこの空振りが起きやすい: 後片付けの実体が経路依存で、file / resource 経路では何もしないため、順序を誤ってもサンプルと大半のテストは無症状のまま通る。

## 関連

- [表示への反映と Host の寿命](../api/maui-rendering-lifecycle.md) — この機構の上に載る利用者向け公開面 (accessory View と `CustomCell.Content` の更新規律・lifecycle)
- [Cell の MAUI 表現](../api/maui-cells.md) — CustomCell の公開契約
- [MAUI facade の公開契約](../api/maui-facade.md) — facade の入口
- [MAUI Native Bridge の interop 境界](../api/native-bridge.md) — 実体の輸送と gateway の位置づけ
- [Store の状態と更新通知](../../core/architecture/store-and-update-streams.md) — 一過性通知 (`invalidateAccessoryMeasurement`) の位置づけ
- [CustomCell](../../core/cells/custom-cell.md) — content と builder による行の共通契約 (native 側の再バインド規則)
- 決定の経緯: maui/ADR-0016 (三層構造と wrapper・寿命)、maui/ADR-0017 (インスタンス輸送と detach)、maui/ADR-0018 (accessory の更新セマンティクスと再計算口)、maui/ADR-0020 (cell content の live view と世代トークン)
