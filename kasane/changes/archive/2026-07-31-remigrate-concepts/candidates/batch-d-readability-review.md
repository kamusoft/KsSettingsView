# Batch D 初見可読性レビュー

## 判定

**要修正**。12件の責務分担と相互関係は概ね一貫しているが、初見利用者が文書だけを根拠に用語の層を区別し、拡張手順と禁止事項を第三者へ誤解なく説明するには、下記の必須事項が残る。必須事項の解消後は PASS と判断できる。

## レビュー条件

- 指定された統合ドラフト12件の本文だけを読んだ。
- 最終配置を `kasane/concepts/architecture/`、`kasane/concepts/styling/`、`kasane/concepts/conventions/` と仮定した。
- リンクは最終配置からの相対パスとして評価した。リンク先本文および実ファイルの存在は、このレビューの閲覧範囲外のため検証していない。

## 必須

### M1. `Cell` が指す層を明示する

対象: `architecture/display-state-synchronization.md`、`architecture/cell-renderer-registry.md`、`styling/style-resolution.md`

`style-resolution` は Core の `Cell` 抽象へ style 型を要求しないとする一方、`display-state-synchronization` は `CellStyle` を Cell の内容として扱い、`cell-renderer-registry` は具象 Cell 型を model と呼ぶ。さらに Native 側にも `UICollectionViewCell` / `CellViewHolder` があるため、初見では次の3者を区別できない。

- Core が保持する Cell 抽象
- UI 層の `CellStyle` などを持つ具象 Cell model
- platform の再利用可能な Native cell / ViewHolder

少なくとも関係する文書の最初に、この3層の関係と、`SettingsRoot` が実際に何を保持するかを同じ語彙で定義する必要がある。以降は `Cell`、`Cell model`、`Native cell` を使い分ける。これがないと「Core は style を持たない」と「CellStyle は Cell 内容である」が矛盾しているように読める。

### M2. DSL の二種類の構築 API を解決可能にする

対象: `architecture/declarative-ui-bridge.md`

禁止事項の「DSL 初期値用 builder」と「再評価される DSL scope」は、本文中に定義も公開 API 名もなく、12件内の関連文書からも解決できない。両者を同じ API として説明してはいけない理由も示されていないため、利用者は正しい使い分けを再現できない。

各 platform について、次のいずれかが必要である。

- 実際の API 名、評価タイミング、状態の所有者を対比する短い表と最小例を追加する。
- この文書ではその禁止事項を扱わず、正しい API を説明する具体的な見出しへ直接リンクする。

また、「宣言評価中」と「Representable / `AndroidView.update` の反映境界」の違いを、一回の state 変更が `resolved tree → Store 操作 → Host` へ流れる例で示すと誤用を防げる。

### M3. Renderer Registry の拡張契約を再現可能にする

対象: `architecture/cell-renderer-registry.md`

文書の主目的は利用者定義 Cell の追加だが、現在は「表示前に登録する」という説明だけで、初見利用者が登録を再現できない。特に次が未解決である。

- iOS の共有 Registry と注入 Registry で、標準 Cell を準備する手順がどう違うか
- Android の factory 登録に必要な key / `viewType` と `CELL_VIEW_TYPE_MIN` の使い方
- 「別 Cell 型の登録識別子」の正体と、衝突を避ける単位
- 再 bind / 再利用時に listener、購読、埋め込み View をどの時点で解除するか

各 platform の最小登録例を一つずつ置くか、これらを満たす platform 文書の具体的な見出しへリンクする必要がある。未登録 fallback は通常経路ではないという禁止だけでは、安全な通常経路を説明できない。

## 推奨

### R1. Batch D の入口と読む順序を示す

`native-host-boundary`、`store-and-update-streams`、`display-state-synchronization`、`declarative-ui-bridge` は相互リンクされており整合しているが、どれを入口にするかは明示されていない。次のような短い読み順があると、同じ説明の反復を責務の違いとして理解しやすい。

1. Native Host の全体境界
2. Store の状態と通知
3. 4種類の表示同期
4. 宣言 UI Bridge と tree identity
5. Renderer Registry

styling 4件は `style-resolution`、repository / identifiers は `repository-boundaries` を入口として示せる。

### R2. 初出の専門語を定義する

次の語は意味を推測できるものの、初見利用者が同じ意味で他者へ説明できるほどは解決されていない。

- `preflight`、`commit`、snapshot、rebind
- process-wide singleton、placeholder、strict mode
- SSoT、GAV、composite build、step-in
- canvas、hairline、supplementary、Decoration

初出で日本語の短い言い換えを添えるか、各文書の用語表へ追加するのが望ましい。`display-state-synchronization` の既存用語表は良い型になっている。

### R3. identity の安全規則に対照例を加える

対象: `architecture/declarative-tree-identity.md`

動的 collection、明示 ID、位置 fallback の選択表は分かりやすい。一方、重要な禁止である key と明示 ID の併用、および「異なる型の同じ文字列表現を同一視しない」は具体例がない。並べ替え前後で key が維持される例、`1` と `"1"` のような typed hint の例、誤った併用例を各一つ加えると、ADR と実装の優先順位差へ依存しない規則を説明しやすい。

### R4. Android の handler なし ripple の意味を限定する

対象: `styling/cell-visual-states.md`

「handler を持たない LabelCell も clickable」という記述は、callback が発火する、操作可能である、アクセシビリティ上も action を持つ、のいずれにも読める。これは視覚 feedback だけなのか、入力 semantics も含む現行契約なのかを明示した方がよい。iOS との差を残す説明自体は有用である。

### R5. 行高の未指定値と固定高の結果を例示する

対象: `styling/cell-row-layout.md`

正の Cell 高さ、正の Theme row height、platform 最低値という順序と、`hasUnevenRows` の true / false は明瞭である。0または負値を「未指定」とみなすのか、固定高で複数行内容が収まらない場合に clipping / wrapping のどちらになるのかを補うと、禁止事項まで再現しやすい。

### R6. Theme の具体的な property 対応を補う

対象: `styling/list-appearance.md`、`styling/style-resolution.md`

canvas と Cell 背景の区別は明瞭である。一方、Classic separator、Modern Section 背景、Header / Footer、選択色について「Theme から解決」とだけある箇所は、対応 property 名を列挙すると推論を減らせる。iOS Footer の固定 gray は例外規則なので、適用範囲と Theme で上書き可能かも明記した方がよい。

### R7. 標準 Cell の件数より登録集合を主語にする

対象: `architecture/cell-renderer-registry.md`

「基本 Cell 7種と入力 Cell 5種」は変化しやすく、件数だけでは対象を確認できない。種別一覧へのリンクを基本 Cell と入力 Cell の双方に置き、本文では「標準登録対象の Cell 集合」を主語にすると陳腐化しにくい。

### R8. Maven の規範と現状の優先関係を一文で固定する

対象: `conventions/public-identifiers.md`

accepted ADR の `jp.kamusoft` と現行 Gradle `group` の不一致、開発用 GAV が公開契約ではない点は正確に切り分けられている。ただし、「accepted ADR が将来の公開座標を既に決めており実装が未追従」なのか、「公開導入時に再判断する論点」なのかは読み手に委ねられている。どちらが規範かを一文で明示すると、将来の変更担当者が逆方向へ解消するのを防げる。

## PASS と評価した点

- Host、Store、Bridge、Renderer の責務境界は文書間で一貫している。
- 構造、同一 ID の内容、可視性、Theme の4経路は、Store・Host・Bridge の各文書で矛盾なく対応している。
- 完全な model と visible projection、値等価と構造 identity の分離は、用語表と禁止事項まで含めて説明できている。
- Root Header / Footer と Section Header / Footer の所有境界は architecture / styling 間で一致している。
- Cell 固有値、CellStyle、Theme、platform default、disabled overlay の優先関係は `style-resolution` と `cell-visual-states` で整合している。
- Classic / Modern の切替、Theme 更新、内容更新のいずれも Section / Cell identity を変えないという原則が一貫している。
- monorepo と platform 別 build root、Sample と配布物、開発用 GAV と公開座標の区別は明確である。
- 最終配置を基準にすると、同一カテゴリ、`../architecture`、`../styling`、`../platforms`、`../core-model`、`../cells`、`../../decisions` の相対リンク構造は整合している。

## 再レビュー

### 最終判定

**要軽微修正（必須なし、推奨1件）**。前回の必須 M1〜M3 はすべて解消し、新たな必須事項はない。推奨 R1・R3〜R8 も解消している。R2 の専門語定義だけが一部残っているため、現時点では無条件の PASS ではなく、下記の軽微な修正後に PASS と判断する。

### 前回指摘の確認

| 指摘 | 判定 | 確認内容 |
|---|---|---|
| M1 Cell が指す層 | 解消 | `native-host-boundary` に3層の定義と `SettingsRoot` の保持関係が追加され、`display-state-synchronization`、`cell-renderer-registry`、`style-resolution` から同じ定義を参照している。 |
| M2 DSL の二種類の API | 解消 | platform ごとの初期値構築 API と再評価 DSL の表、評価タイミング、`resolved tree → Store 操作 → Host` の流れが追加された。 |
| M3 Registry の拡張契約 | 解消 | iOS / Android の最小登録例、独立 Registry の標準登録、Android の key / `viewType` / strict mode、再利用時の解放地点が明記された。 |
| R1 読む順序 | 解消 | `native-host-boundary` の関連節に architecture、styling、repository / conventions の入口と順序が追加された。 |
| R2 専門語 | **一部未解消** | snapshot、reconfigure / rebind、singleton、代替行、strict mode、GAV、composite build、SSoT、canvas、hairline、補助 layout は解決した。一方、下記2語が残る。 |
| R3 identity の例 | 解消 | 並べ替え、key と明示 ID の禁止、整数 `1` と文字列 `"1"` の対照例が追加された。 |
| R4 handler なし ripple | 解消 | 視覚 feedback の実装状態であり、callback / 利用者向け action を意味しないと限定された。 |
| R5 行高 | 解消 | 未指定・非正値・minimum・固定高で内容が収まらない場合の利用者責務が追加された。 |
| R6 Theme property | 解消 | separator、Header / Footer、選択色、Modern 背景の property 対応と iOS Footer の上書き可否が追加された。 |
| R7 標準 Cell 集合 | 解消 | 標準登録集合を主語にし、基本 Cell / 入力 Cell の双方へリンクした。 |
| R8 Maven の規範 | 解消 | accepted ADR が公開 `groupId` の規範であり、現行 Gradle `group` は未追従の drift であると明記された。 |

### 残る推奨

#### RR1. `preflight` と `Decoration` を初出で言い換える

対象: `architecture/display-state-synchronization.md`

- `DSL の preflight` は「部分更新を適用する前に、前後の可視性差を検査する処理」など、役割を一度定義する。
- Theme 更新表の Android `Decoration` は、`RecyclerView` の行間・Section 装飾など、ここで何を指すかを日本語で補う。

どちらも文脈から大意は推測でき、責務契約を誤らせるほどではないため必須ではない。ただし前回 R2 の「初出の専門語を解決可能にする」という観点では未解消である。

### 新規指摘とリンク

- 新たな必須事項はない。
- RR1 以外の新たな推奨事項はない。
- 最終配置を基準に、追加された `native-host-boundary.md#cell-の3層`、platform 文書の `#cell-renderer-registry`、既存のカテゴリ間・ADR 相対リンクは構造上整合している。リンク先本文と実在性は今回も閲覧範囲外のため検証していない。

## 最終再レビュー

### 最終判定: PASS

前回残っていた RR1 は解消した。

- `preflight` は「事前差分確認」へ置き換えられ、可視性差を検出して full 更新へ切り替える処理だと前後の文だけで理解できる。
- Android の `Decoration` は更新経路では「Section 装飾」、禁止事項では「Section の背景・角丸装飾」へ具体化され、Root Header / Footer を対象外にする規則も誤解なく説明できる。

同じ12文書の範囲で新たな必須・推奨事項はない。前回までの M1〜M3、R1〜R8、RR1 はすべて解消しており、初見利用者が責務境界、更新経路、identity、拡張手順、style 規則、repository / 配布規約を他者へ説明できる状態になった。最終配置を基準とする相対リンク評価にも変更はない。
