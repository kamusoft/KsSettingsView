---
scope: code-review
kind: pain
severity: normal
count: 2
first-seen: 2026-09-05
last-seen: 2026-09-05
evidence:
  - add-sample-dark-mode-toggle (review-001 の Minor「`Configuration()` の既定コンストラクタは `setToDefaults()` を呼び fontScale = 1 で埋めるため、明示選択時に端末のフォントスケールが 1.0 へ戻る」を、実装ワーカーが Emulator 実測と AOSP の実装で反証。引数なしの `Configuration()` は `unset()` を呼ぶ差分用コンストラクタで fontScale = 0、`updateFrom` は `fontScale > 0` のときだけ取り込む。review-002 で指摘を撤回。推奨修正 `fontScale = 0f` も既定値の再代入で no-op だった)
  - fix-default-colors-dark-appearance (review-001 が「行の Context は `ContextThemeWrapper` で `getResources()` をラップ元へ委譲するため構成変更後も現在の夜間モードを返す」を根拠に、外観変更後の Context 再利用を「確認して問題が無かった観点」に置いた。前半 [Configuration は最新] は正しいが、`ContextThemeWrapper` のテーマ属性は生成時に組み立てられ固定される点を確認しておらず、相方 second-opinion-code-001 が実経路プローブ [`colorSurface` が旧値のまま] で Major として反証。指摘を書く側だけでなく「問題なし」と判定する側でも同じ型で起きる)
---

## ルール文

レビューで platform / framework API の内部挙動 (コンストラクタの初期化内容・既定値・差分の取り込み条件・呼び出し順序・ラッパが何を委譲し何を固定するか) を根拠に Minor 以上の指摘を書くとき、または懸念を「問題なし」と判定して確認済み観点に置くときは、その API の実装ソースまたは公式リファレンスの該当箇所を読んでから書き、指摘文にその参照 (ソース行または doc の節) を併記する。参照を示せない内部挙動の主張は指摘にせず、「〜であれば問題になる。実測または実装の確認を推奨」の形で Suggestion に留める。事後判定: 指摘文中の API 内部挙動の主張には、それを確認した参照が 1 つ以上添えられている。

## 経緯

- 2026-09-05 add-sample-dark-mode-toggle: ホスト側 review-001 が `android.content.res.Configuration` の 2 つの初期化 (`Configuration()` = `unset()` / `setToDefaults()` = 明示呼び出し) を取り違え、記憶ベースで「端末のフォントスケールが潰れる」と Minor を出した。実装ワーカーが Emulator 実測 (フォント最大でも変化なし) と AOSP の実装 + javadoc ("Construct an invalid Configuration ... only suitable for constructing a Configuration delta") で反証し、review-002 が撤回。修正サイクル 1 周の工数を的外れの検証に使った。実装側が impl/L-005 (レビュー推奨修正を仮説として扱う) を守ったため実装への混入は防げたが、レビュー側の起点で防ぐルールが無かった。
- 2026-09-05 fix-default-colors-dark-appearance: 逆向きの 2 例目。framework API の内部挙動 (`ContextThemeWrapper` が resources を委譲する) を記憶で根拠にし、懸念を「問題なし」側へ倒した。API の実装 (テーマは生成時に `Resources.Theme` として組み立てられる) を読んでいれば、委譲されるのは Configuration であってテーマ属性ではないと分かった。ルール文を「指摘を書くとき」だけでなく「問題なしと判定するとき」も対象にする形へ広げた。
