# Batch B 統合案 初見可読性レビュー

## 判定

**要修正（PASS ではない）**

2文書だけを初めて読む利用者として確認した。どちらも、対象 API の目的、Store と Host / SwiftUI の責務境界、主な保証と禁止、代表的な利用経路は概ね説明できる。一方で、文書の位置づけ、文書間リンク、SwiftUI の最小利用例、identity の利用契約には、初見読者が誤解するか実作業で止まる問題がある。

レビュー対象は次の2ドラフトに限定した。コード、テスト、旧 spec、抽出 candidate、integration report は参照していない。

- `batch-b/platforms/ios-native-host.md`
- `batch-b/platforms/ios-swiftui.md`

## 必須

### 両文書 — 冒頭の位置づけ

両文書の冒頭にある「利用者向けの正本」は、コードとテストが現仕様の SSoT である Kasane の層構造と衝突し、初見読者に「この文書が現仕様の一次情報である」と誤解させる。「公開 API の利用契約と責務境界を整理した reference」など、L2 concept の役割が分かる表現へ変更する。

### 両文書 — 冒頭および「関連」の相対リンク

文書間の役割を示すリンクが、`platforms/` 配置から見て一段余計または一段不足している。現状では入口で案内された読者が関連文書へ移動できず、2文書の役割分担も辿れない。実配置基準で次の形に直す。

- `ios-native-host.md` から `ios-swiftui.md` は `ios-swiftui.md`
- `ios-swiftui.md` から `ios-native-host.md` は `ios-native-host.md`
- `platforms/` から `core-model/` は `../core-model/...`
- `platforms/` から `cells/` は `../cells/...`

冒頭と「関連」にある同種のリンクをすべて同じ基準で修正する。

### `ios-swiftui.md` — 「二つの利用方式」の利用例

公開 API の reference として、最初の利用例がそのまま最小例として使える形になっていない。

- DSL 例は、直後に `SwiftUI.Section` との名前衝突を説明しているのに、衝突し得る `Section("通知")` を入口例で使っている。入口例は `KsSection` または `ksSection(...)` に統一する。
- 必要な `import` を示すか、既存の共通 import を前提とすることを明記する。
- Store 例の `root`、`theme`、`newCell`、`sectionID` は未定義で、単独では利用手順を再現できない。最小の値を定義した動く例へするか、「既存の Root を保持している場合」の断片であることを明記して、完全な最小例へリンクする。

これにより、初見読者が DSL / Store の選択理由だけでなく、最初の一歩も誤解なく説明・再現できるようにする。

### `ios-swiftui.md` — 「宣言ツリーの identity」

「accepted ADR と現行 iOS 実装に drift があるため、解消までは一方だけを identity の正として使う」は、背景は伝わるが利用契約が曖昧である。「同じ要素では DSL 専用 `ForEach` の key と `sectionID(_:)` / `cellID(_:)` を併用しない。どちらか一方だけを指定する」のように、現在安全な操作を直接書く。また、この制約を同文書の「してはいけないこと」にも追加する。

ADR と実装の不一致そのものを残す場合は、読者が追跡できる具体的な ADR アンカーも添える。「accepted ADR」という一般名だけでは、どの決定との不一致か再確認できない。

## 推奨

### `ios-native-host.md` — 「目的」「model と表示の同期」

本文中で定義されてはいるが、`Host`、`model`、`visible projection`、`Native snapshot` の関係は初見では一度戻って読み直す必要がある。短い「用語」節、または「所有者 / 保持する状態 / 担当する更新」の3列程度の表にまとめると、Store と Controller の境界を他者へ説明しやすい。

特に `model` は一般語に見えるため、「hidden 要素を含む Store の現在状態」を指すのか、Controller が保持する写像を指すのかを一貫した表記で固定する。

### `ios-native-host.md` — 「SettingsRootStore」

「存在しない Section / Cell を対象にした通常の remove、move、replace、insert」は、insert が何の不存在を指すか読み取りにくい。たとえば「対象 ID が見つからない remove / move / replace、および挿入先 Section が見つからない insert」のように、操作と不存在対象を対応させる。

### `ios-swiftui.md` — 「二つの利用方式」

DSL と Store の選択基準は文章で説明できている。冒頭に「所有者」「向くケース」「更新方法」の比較表を置くと、初見読者が選択を即答しやすい。特に「DSL は内部 Store」「Store 方式は利用者所有 Store」という所有権の差を表に出すとよい。

### `ios-swiftui.md` — 「二つの利用方式」の補助 API

`makeController()` と `applyUpdate(to:coordinator:)` は「Store backing 用」とだけ説明され、誰がどの場面で呼ぶ API か分からない。一般利用者が直接使う必要がないなら高度な利用者向けとして分離し、必要なら最小の用途を1文で示す。一般利用者が使う公開入口なら、呼出順序と責務を説明する。

### `ios-swiftui.md` — 「宣言ツリーの identity」

identity は本書で最も誤用コストが高い契約だが、静的例しかない。DSL 専用 `ForEach` の `Identifiable` 版または `id:` 版を使った、挿入・削除・並べ替えに耐える短い例を1つ追加する。その例では、同じ要素へ `.cellID(_:)` を重ねないことも見せる。

### `ios-swiftui.md` — 用語

`resolved tree`、`identity hint`、`位置 fallback`、`Store backing`、`DSL backing`、`reconfigure`、`full 更新` は相互に関係するが、初出箇所に定義が分散している。短い「用語」節で、公開型名ではない概念語を日本語の説明と対応付ける。英日を混在させる場合も、同じ対象を別の言い回しに変えない。

## 問題なし

### `ios-native-host.md`

- 「目的」で、`SettingsRootStore` が状態と通知、`KsSettingsViewController` が visible projection・snapshot・描画を担うと示しており、中心的な責務境界は明確である。
- 「公開入口」は主要型と入口を具体名で接地しており、メソッドの網羅的シグネチャ集にはなっていない。
- 「model と表示の同期」は変更種別と iOS 側の反映を表で対応させ、hidden 要素を削除しない理由と index の基準も説明できている。
- 「Cell Renderer Registry」は標準 Registry と独立 Registry の違い、独自 Renderer の登録、未登録時の結果まで一続きに説明できている。
- 「保証すること」と「してはいけないこと」は分離され、保持、購読、identity、hidden、Theme の禁止境界が具体的である。
- 「利用例」は Store の生成、Controller への接続、表示後の部分更新という代表的な流れを示している。

### `ios-swiftui.md`

- 「目的」で、SwiftUI は状態と event の橋渡しを担い、Native list や renderer を再実装しないと明記しており、Native Host 文書との役割分担は明確である。
- 「二つの利用方式」は DSL が内部 Store、Store 方式が外部 Store を使い、最終的に同じ Host / Diff 経路へ収束することを説明できている。
- 「DSL の構築 API」「Cell と Section の modifier」は Root / Section / Cell の各適用範囲と、独自 Cell が対応するための protocol アンカーを示している。
- 「Theme の伝播」は DSL / Store の違い、modifier 未指定時の保持、Theme と style / accessory の別経路を区別できている。
- 「保証すること」と「してはいけないこと」は、body の副作用、identity、可視性、Theme、`.disabled(_:)` の誤用を具体的に分けている。

## 総括

必須項目を直せば、初見読者は「UIKit では Store と Native Host を直接使い、SwiftUI では DSL または同じ Store を Bridge 経由で使う。どちらも Native Host の描画・差分経路へ収束する」と説明できる構成になる。用語表と動的 identity の短い例まで加えると、読み返しと誤用の両方をさらに減らせる。

## 再レビュー（2026-07-19）

**PASS**

修正後の `batch-b/platforms/ios-native-host.md` と `batch-b/platforms/ios-swiftui.md` だけを再読した。コード、テスト、旧 spec、抽出 candidate、integration report は参照していない。

前回の必須事項はすべて解消している。

- 冒頭は L2 reference の役割を示す表現となり、現仕様の SSoT と誤認させない。
- 同一カテゴリ間、`core-model/`、`cells/`、ADR-0008 へのリンクは実配置基準で辿れる。
- SwiftUI の DSL / Store 例は import と必要な値を備え、入口例では `ksSection` / `KsSection` を使って名前衝突も避けている。
- identity は「同じ要素で `ForEach` key と明示 ID を併用しない」という安全な操作へ言い換えられ、「してはいけないこと」にも反映されている。drift の出所は ADR-0008 へ接地している。

前回の推奨事項も、Native Host の状態・所有者表、DSL / Store の方式比較表、補助 API の対象用途、動的 `ForEach` の例、用語表として反映されている。初見読者は、両文書だけから次を誤解なく説明できる。

- UIKit では利用者が `SettingsRootStore` を更新し、`KsSettingsViewController` が visible projection、Native snapshot、Cell 描画へ接続する。
- SwiftUI では画面規模と更新主体に応じて DSL / Store 方式を選び、どちらも同じ Native Host と Store / Diff 経路へ収束する。
- Theme、構造 Diff、identity、可視性の更新境界と、各方式でしてはいけない操作。
- 標準的な構築、表示後の部分更新、動的コレクション、独自 Cell 登録の代表的な利用法。

残存する必須・推奨の可読性問題はない。
