# UI Brief: ios-swiftui-representable-safe-area

## 画面と状態

- **`NavigationStack` の中身として全画面で置いた `KsSettingsView`** (状態: 先頭表示 / 末尾までスクロール): 既定の全面配置。一覧の下地が画面の上端から下端まで途切れず、bar に覆われる領域の inset は UIKit の自動調整で付く。iOS 26 では大タイトルがスクロールで畳まれる
- **同じ画面に `.respectsSafeArea()` を付けた状態** (状態: 先頭表示 / 末尾までスクロール): 本変更前の既定と同じ。一覧はセーフエリアの内側に収まり、bar の後ろへ回り込まない
- **EntryCell 編集中** (状態: キーボード表示): ラッパがキーボード領域の外側に縮み、編集中の Cell がキーボードに隠れない (本変更前と同じ)
- loading / empty / error 状態なし (配置の契約のため)

## リファレンス注釈

いずれも KsAppKMP のリファレンスアプリ (iOS 26.4 Simulator、デモデータ) の `HomeView` を写したもの。行構成・文言・アイコンは KsAppKMP のもので非規範。**見るのは一覧の下地と bar との関係だけ**。

- `references/ios26-spike-safearea-ignore-all-top.png` — 利用側で `.ignoresSafeArea()` を付けた状態の先頭表示。**目指す見た目**: 大タイトルが一覧の先頭に置かれ、下端では末尾 Cell がタブバーの後ろへ回り込み、タブバーが一覧の下地の上に浮く
- `references/ios26-spike-safearea-ignore-all-bottom.png` — 同じ状態で末尾までスクロール。**目指す見た目**: 大タイトルがインラインに畳まれ、末尾 Cell がタブバーの上で止まり、一覧の下地が画面下端まで続く
- `references/ios26-spike-safearea-baseline-top.png` — 修飾なし (本変更前の既定) の先頭表示。**直したい見た目**: 一覧がセーフエリアの内側に縮み、下端で一覧の下地が切れてタブバーの上下に別色の帯が出る
- `references/ios26-spike-safearea-baseline-bottom.png` — 同じ状態で末尾までスクロール。**直したい見た目**: 大タイトルが畳まれずに流れて消え、上端に空白が残る

## デザイントークン参照

- 色・余白は変えない。一覧の下地・Cell 背景は Theme の色ロール ([concepts/core/styling](../../../concepts/core/index.md)) の現行値のまま
- bar に覆われる領域の inset の量は OS (UIKit の automatic contentInset 調整) が決める。ライブラリは値を持たない

## 承認モック

HTML モックは作らない (オーナー確定 2026-09-19)。目指す見た目は `references/ios26-spike-safearea-ignore-all-{top,bottom}.png` で確定しており、これを承認済みの正として扱う。視覚照合は Sample (`samples/ios`) の各画面で撮った実装スクリーンショットを、上記 2 枚と「一覧の下地と bar の関係」の観点で突き合わせる (行構成・文言は Sample のものであり照合対象外)。

現行との照合 (lessons spec-review L-003): references は KsAppKMP の画面であり、Sample の文言・行構成・初期値を描いたものではない。Sample 側の文言・構成は本変更で変えない (specs/samples-ios)。

### 照合結果 (2026-09-19)

`verification/` の各画像を `references/ios26-spike-safearea-ignore-all-{top,bottom}.png` と「一覧の下地と bar の関係」の観点で照合した。撮影は iOS 26.5 Simulator (iPhone 17 Pro) と iOS 18.6 Simulator、Sample のデモデータ。

| 画像 | 照合した観点と結果 |
|---|---|
| `ios26-customcell-top.png` | 大タイトルが一覧の下地 (canvas) の上に置かれ、下地が画面上端まで続く。下端でも下地が画面下端まで途切れない → ignore-all-top と同じ関係 |
| `ios26-customcell-scrolled-inline-title.png` | スクロールで大タイトルがインラインタイトルへ畳まれ、内容がバーの下へ潜る → ignore-all-bottom と同じ関係 |
| `ios26-customcell-bottom.png` | 末尾 Cell と Section footer がホームインジケータ領域に隠れず見え、下地は画面下端まで続く |
| `ios26-store-top.png` / `ios26-store-bottom.png` | Store 方式でも同じ。末尾までスクロールすると末尾 Cell と footer が隠れずに見え、別色の帯が出ない |
| `ios18-customcell-top.png` / `ios18-customcell-bottom.png` / `ios18-dsl-top.png` / `ios18-store-top.png` | iOS 18.6 でも帯が出ず、先頭 / 末尾 Cell の inset が正しい |
| `ios26-inputcells-keyboard.png` | 下端付近の EntryCell をタップした状態。編集中の Cell がキーボードに隠れない (keyboard 領域を無視しない裏付け) |
| `ios26-customcell-respectssafearea-top.png` | Sample へ一時的に `.respectsSafeArea()` を付けた状態。一覧がセーフエリアの内側へ縮み、下端に別色の帯が出る (本変更前の配置) → 既定との A/B が成立。撮影後に一時変更は戻した |

内側適用の同等性 (tasks 0.1 の spike): DSL 方式デモ画面と基本 Cell 7 種デモ画面で、利用側に `.ignoresSafeArea(.container, edges: .bottom)` を書いた状態と、ラッパ内側で `.ignoresSafeArea(.container)` を掛けて利用側の記述を外した状態を同一操作で撮り、ステータスバー (時計表示) を除く全画素が一致することを確認した (差分 0 / 2,969,172 画素、両画面とも)。

合意済み妥協: 0 件。

トークン候補: なし (色・余白を足していない)。

### 証跡の範囲についての注記

- Scenario「Store 方式と DSL 方式で配置が同じ」について、iOS 26 では同構成 (どちらも `NavigationStack` 直下に単独で置く) のペアを撮っていない。両方式は `KsSettingsView.body` の 1 箇所で同じ修飾を受ける構造的同一性があり、同構成ペアは iOS 18.6 の `ios18-dsl-top.png` / `ios18-store-top.png` で一致を確認した (review-001 Minor 2 への対応、2026-09-19)
- このリポジトリの Sample にはタブバーが無いため、タブバーを伴う構成の確認は範囲外 (proposal の Impact どおり、SPM 更新後に依存側で確認する)。本証跡はホームインジケータ領域・ナビゲーションバーに対する回り込みで取っている
- 大タイトルの畳み込みは、`KsSettingsView` を `NavigationStack` の直下に単独で置く CustomCell デモ画面で確認した。Store 方式デモ / DSL 方式デモ / 入力 Cell 5 種デモは `VStack` の中に操作用のボタンや直近イベント表示と並べて置く構成のため、いずれの配置でも大タイトルはスクロールビューの先頭に乗らない (Sample の構成に由来するもので、ラッパの配置とは独立)
- Store 方式デモは初期 3 行では画面に収まりきるため、証跡は画面上の「項目追加」を 14 回押して一覧を画面より長くしてから撮っている (Sample のコードは変更していない)
