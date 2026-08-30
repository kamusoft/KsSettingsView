# Batch C 初見可読性レビュー

## 判定

**要修正（必須 2 件、推奨 5 件）**

指定された次の2文書だけを、関連コードや既存仕様を知らない Android 利用者として読んだ。

- `platforms/android-native-host.md`
- `platforms/android-compose.md`

両文書を読めば、Native Host が Store と Android View の接続境界であり、Compose はその上に DSL / Store の二方式を提供することを他者へ説明できる。主要な前提、禁止事項、代表例も概ね再現できる。ただし、公開 Theme 入口の関係と Compose modifier の非対応時の契約には、利用者が異なる解釈をし得る箇所がある。

## 必須

### 1. Native Host の二つの Theme 入口の関係が不明

対象: `android-native-host.md` の「SettingsRootStore」「KsSettingsView」「スタイルと視覚状態」

`SettingsRootStore(initialTheme)` / `store.applyTheme(...)` と `KsSettingsView.theme` の二つが公開 Theme 入口として登場するが、`bind(store)` 後にどちらが正になるかが書かれていない。「`theme` は View を直接扱う場合の Theme 入口」の「直接扱う場合」も、root を Host へ渡す公開手段が本文では `bind(store)` しか示されず、具体的な利用形態を特定できない。

初見利用者は、たとえば次を判断できない。

- `view.theme = ...` の後に `bind(store)` すると Store の Theme で上書きされるのか
- bind 済み View の `theme` を変更してよいのか、`store.applyTheme` だけを使うべきなのか
- Store を使わず `view.theme` を使う場合、root をどの公開 API から渡すのか

Store 方式では Store の Theme を唯一の正とする、または二入口の優先順位と利用条件を明記する必要がある。該当する代表コードも1例あると誤用を防げる。

### 2. Compose modifier の非対応時契約を再現できない

対象: `android-compose.md` の「DSL の構築 API」

「非準拠型に対応 modifier を呼んだ場合は元の値を返す」の「元の値」が、元の `Cell`、元の `CellHandle`、変更前の属性値のどれを指すか判別できない。また、表では `CellHandle / Cell` を一括しているため、同名 modifier の戻り値やチェーン継続可否も読み取れない。

これは利用者定義 Cell に対する公開 API の動作契約なので、少なくとも次を明記する必要がある。

- `Cell` 版と `CellHandle` 版それぞれの receiver と戻り値
- 非準拠時に no-op になる対象と、呼び出しチェーンがそのまま継続できるか
- `cellID` には `DSLReidentifiableCell` が必要だが、style / icon と同様に非準拠時 no-op なのか、それとも別の失敗方法なのか

組み込み Cell と利用者定義 Cell を各1つ示す短い modifier 例があれば、API の利用方式を誤解なく再現できる。

## 推奨

### 1. Native の Root / Section Accessory に最小例を加える

`rootHeader` / `rootFooter`、`RootAccessory`、`SectionAccessory`、`KsAnyView.Compose` / `.AndroidView` の関係は文章では理解できるが、文字列と任意 View の実際の構築・代入構文は再現できない。代表例に Root H/F の設定か、`KsAnyView` のどちらか一方を加えるとよい。「Root H/F」は最初の使用時に Root Header / Footer の略記だと明記するとさらに読みやすい。

### 2. Native 内部語を利用者向けの語へ寄せる

「状態 commit」「list commit」「subtype」は定義がなく、公開 API 利用者には意味を解決しにくい。「一回の状態更新」「`submitList` の反映完了」「行種別」のように言い換えるか、用語表を設けるとよい。`model`、`visible projection`、`Host` は冒頭の表で解決できており問題ない。

### 3. Compose の二つの DSL scope の型・パッケージを例で明確にする

`settingsRoot { section { cell(...) } }` と `KsSettingsView { Section { ... } }` が別 scope であることは明記されているが、同じ文書内で大文字・小文字 API と Native / Compose の Cell 型が並ぶため、初見では import 元を取り違えやすい。Store 方式の例に必要な import を付けるか、各 API の完全修飾パッケージを一度示すとよい。

### 4. identity の代表例に明示 ID の例も加える

動的 `forEach` の正例は十分に分かりやすい。一方、`.sectionID(...)` / `.cellID(...)` は「引数値そのものを最終 ID にしない」という意外性のある契約なので、静的要素での正しい使用例を1つ示すと、key との併用禁止を含めて再現しやすい。

### 5. 「drift」「Compose Native 型」を平易にする

ADR と実装の「drift」は、この文脈では「食い違い」と書けば知識なしで理解できる。「Compose Native 型」も Android の Native View 型と混同し得るため、「Jetpack Compose 側の型」のように表現するとよい。`identity hint`、`resolved tree`、`位置 fallback`、`内容更新`、`full 更新` は用語表で解決できている。

## 問題なし

### 1. 対象を他者へ説明できるか

問題なし。Native 文書は Store、完全な model、visible projection、RecyclerView 描画の責務を分けている。Compose 文書は DSL / Store の選択基準と、どちらも同じ Native Host へ収束することを説明している。二文書の役割分担も相互リンクで明確である。

### 2. 抽象語・参照・新造語

上記推奨事項を除けば問題なし。特に Compose の identity 関連語は文書末の用語表で回収され、Native の model / visible projection / Host は冒頭で定義されている。hidden 要素、stable item ID、Material3 Theme の前提も、禁止事項まで含めて意味を追える。

### 3. 公開 API の利用方式・前提・禁止事項・代表例

必須2件を除けば問題なし。Native は Store の部分操作、hidden を含む index、Registry、Material3 XML Theme、bind の基本例を示している。Compose は DSL / Store の選択、Recomposition をまたぐ Store 保持、動的 identity、Theme 更新、`disabled` が no-op であることを、正例と禁止事項の双方で示している。

### 4. 最終配置基準のリンク

問題なし。両文書を `kasane/concepts/platforms/` に配置した場合、リンクは次のように解決される。

- `android-native-host.md` / `android-compose.md`: 同じ `platforms/` 配下
- `../core-model/...`: `kasane/concepts/core-model/` 配下
- `../cells/...`: `kasane/concepts/cells/` 配下
- `../../decisions/0008-stable-declarative-tree-identity.md`: `kasane/decisions/` 配下
- `android-native-host.md#material3-theme-の前提`: Native 文書内の該当見出し

候補配置時点ではなく、指定どおり最終配置 `kasane/concepts/platforms/` を基準に判定した。

## 再レビュー

### 判定

**PASS（残存する必須・推奨事項なし）**

更新後の同じ2文書だけを初見利用者の立場で再読した。前回の必須2件と推奨5件はすべて解消されており、新たな問題もない。

### 必須事項の確認

- Store 方式では `store.theme` が唯一の正であること、`bind(store)` が先行する `view.theme` を上書きすること、bind 後の直接変更も次の通知または再 bind で上書きされることが明記された。Store を使わない高度な `view.applyDiff(...)` 方式の入口と、同じ View での併用禁止も示され、Theme の優先関係を誤解なく再現できる。
- `Cell` extension と `CellHandle` extension について、receiver、戻り値、非対応時の no-op、chain 継続可否が表で区別された。`cellID` は別契約として、hint は記録される一方、非準拠 Cell は例外にせず元の `Cell.id` を維持することも明記され、利用者定義 Cell の挙動を再現できる。

### 推奨事項の確認

- Native の例に `RootAccessory.Text` と `store.applyTheme` が加わり、Root Header / Footer の略記も解消された。
- 「状態 commit」「list commit」「subtype」は「一回の状態更新」「`submitList` の反映完了」「行種別」へ置き換えられた。
- Store 初期値用 builder の import と完全修飾名が示され、二つの DSL scope を区別できる。
- 静的要素の `.sectionID(...)` / `.cellID(...)` 例と、値が最終 ID そのものではなく hint である説明が追加された。
- 「drift」は「食い違い」、「Compose Native 型」は「Jetpack Compose 側の型」へ置き換えられた。

### 問題なし

- 対象の説明可能性: Native Host、Store、Compose の DSL / Store 方式の役割と接続関係を他者へ説明できる。
- 用語と参照: 初見で解決できない抽象語、宙に浮いた参照、意味の取れない新造語は残っていない。
- 公開 API: 利用方式、Material3 XML Theme などの前提、identity や hidden index などの禁止事項、Native / Compose 双方の代表例を誤解なく追える。
- リンク: 最終配置 `kasane/concepts/platforms/` 基準で、同階層の Android 文書、`../core-model/`、`../cells/`、`../../decisions/`、Native 文書内の Material3 見出しへのリンクはいずれも正しい。
