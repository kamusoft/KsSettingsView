# セカンドオピニオン: add-question-form-and-english-screenshots (code-001)

**相方**: codex / **label**: so-code-add-question-form-and-english-screenshots / **日付**: 2026-08-30 / **対象**: `HEAD` (ade7326) からの作業ツリー差分 (未追跡ファイル含む)

---

# レビュー結果: add-question-form-and-english-screenshots

**判定**: CHANGES_REQUESTED  
**件数**: Critical 0 / Major 2 / Minor 1 / Suggestion 0  
**結果ファイル**: ユーザー指定に従い作成せず、テキストで返却

## サマリー

Issue Form・CONTRIBUTING・docs-refresh の変更はデルタスペックと整合しています。一方、スクリーンショットの目視検査では、4.8・4.11 のチェック済み状態と画像実体が一致していません。

### [🟠 Major] Classic 画像が Modern と同じスクロール位置ではない

**該当箇所**: `tasks.md:41`、`ui/brief.md:113`  
**画像実体**: `assets/ios-classic.png`、`assets/android-classic.png`

**問題点**: Modern 画像にはステータスバー、画面タイトル、style 切替、`Decoration preset` が写っていますが、Classic 画像は style 切替から始まり、画面タイトルと preset 行が画面外です。「同一スクロール位置」「platform と style 以外の差を持たない」という Requirement を満たしません。

差し替え前の日本語版にも同じずれがありましたが、今回のデルタスペックで明示的に禁止されたため、既存踏襲では免責されません。承認候補と `assets/` は全バイト一致しており、配置ミスでもありません。

**推奨修正**: style 切替後に明示的に最上部へ戻し、Classic 2 枚を再撮影してください。4 枚を並べ、上端の構成要素が一致することを再確認してください。

### [🟠 Major] iOS 画像の最下部 footer が途中で切れている

**該当箇所**: `tasks.md:44`、`ui/brief.md:98`  
**画像実体**: `assets/ios-modern.png`、`assets/ios-classic.png`

**問題点**: 最下部の `No border by default...` footer が画面下端で途中までしか表示されず、最後まで読めません。`ui/brief.md:98` はこれを「文字列の切れではない」としていますが、デルタスペックの「切れがなく最後まで読める」という契約を brief 側の解釈で緩和することはできません。`deviation.md` も存在しません。

**推奨修正**: footer を完全に表示するか、文字列が中途半端に写り込まない構図で再撮影してください。現条件で両立できない場合は、実装側だけで処理せずオーナー判断を仰ぎ、合意された乖離として記録する必要があります。

### [🟡 Minor] 撮影した最終候補の置き場が `ui/references/` になっている

**該当箇所**: `ui/brief.md:44`、`ui/brief.md:104`

**問題点**: ksn-core では `ui/references/` は議論時に渡された参考画像、実装中に撮影した最終周の画像は `ui/verification/` と役割分離されています。今回の4枚は実装時に撮影・承認された画像です。

**推奨修正**: 候補4枚を `ui/verification/` へ移し、brief と tasks の参照先を更新してください。

## 確認済みの正常項目

- Issue Form 4本（`config.yml` を含む）はすべて YAML として解析可能
- 質問フォームの必須項目・ラベル・英日投稿案内は仕様どおり
- CONTRIBUTING 英日は同一粒度
- README 英日は同一の画像パスを参照し、MAUI の説明も存在
- 4画像は英語表示で、目視できる端末固有情報なし
- `ui/references/` と `assets/` の各対応画像は全バイト一致
- `samples/` とルート README に未意図の差分なし
- `git diff --check`、local-path lint、identity lint は成功
- PNG metadata に個人・端末識別情報は確認されず

---

## 突き合わせ結果 (2026-08-30)

ホスト側レビュー ([review-001.md](review-001.md)、判定 APPROVED / Suggestion 2 件) と本セカンドオピニオン (判定 CHANGES_REQUESTED / Major 2・Minor 1) を突き合わせた。**確定した修正対象は 0 件**。

| # | 相方の指摘 | 採否 | 根拠 |
|---|---|---|---|
| Major 1 | Classic 画像が Modern と同じスクロール位置ではない | **却下 (事実誤認)** | 下記 |
| Major 2 | iOS 画像の最下部 footer が途中で切れている | **降格** | 下記 |
| Minor 1 | 撮影候補の置き場が `ui/references/` になっている | **降格** | 下記 |

### Major 1 — 却下 (事実誤認)

相方は「`ios-classic.png` にはステータスバー・画面タイトル・装飾プリセット行が写っておらず、`android-classic.png` には画面タイトルと装飾プリセット行が写っていない」と主張した。両者の指摘が矛盾したため ksn-second-opinion の規律に従い根拠の再提示を 1 回求めた (ホスト側の主張は伝えず、4 枚それぞれについて上端から写っている要素の列挙を依頼) が、相方は同じ読み取り結果を再提示した。

これは実画像と一致しない。Modern と Classic の上端 640px (iOS) / 560px (Android) を並べて比較したところ、両 platform とも上端から順に **ステータスバー (9:41) → 画面タイトル `Section decoration (style switch)` → Classic / Modern 切替セグメント → `Decoration preset` `Default`** が同一位置に並び、差は切替セグメントの選択状態だけだった。ホスト側レビューの独立検査も同じ結論に達している。

再現手順 (`assets/` の 4 枚から比較画像を作る):

```python
from PIL import Image
for name, a, b, h in [("ios", "assets/ios-modern.png", "assets/ios-classic.png", 640),
                      ("android", "assets/android-modern.png", "assets/android-classic.png", 560)]:
    ia, ib = Image.open(a), Image.open(b)
    ca, cb = ia.crop((0, 0, ia.width, h)), ib.crop((0, 0, ib.width, h))
    out = Image.new("RGB", (ca.width + cb.width + 24, h), (255, 0, 255))
    out.paste(ca, (0, 0)); out.paste(cb, (ca.width + 24, 0))
    out.save(f"top-compare-{name}.png")
```

相方は read-only sandbox で PNG を開いたと述べているが、画像内容の読み取りに失敗したと判断する。**画像を判定材料とする指摘については、相方の読み取り結果を単独の根拠にしない**。

### Major 2 — 降格 (事実は正しいが spec 解釈として過大)

iOS 2 枚の最下部で 4 つ目 Section の footer が画面下端に達して途切れているのは事実。ただしこれは viewport 端のクリップであり、レイアウト起因の文字切れ (ellipsis・重なり・不自然な折り返し) ではない。

Requirement「スクリーンショットの提示」は「表示文字列は切れ・重なり・不自然な折り返しがない」と同時に「4 枚は同一のデモ画面・同一のスクロール位置で撮り、platform と style 以外の差を持たない」を課しており、[ui/brief.md](ui/brief.md) の撮影条件表がそのスクロール位置を最上部と固定している。画面より縦に長いコンテンツを最上部で撮る限り下端でのクリップは不可避で、前者を viewport 端のクリップまで含めて読むと、この画面のスクリーンショットはいかなる位置でも撮れなくなる。

差し替え前の日本語版 (`git show HEAD:assets/ios-classic.png`) も同じ位置・同じ量で途切れており、英語化による劣化ではない。オーナーへの承認提示時にこの点を明示したうえで承認を得ている (brief.md「承認モック」)。

### Minor 1 — 降格 (足場が明示的に指定した置き場)

ksn-core `references/ui-artifacts.md` の一般則では、実装時に撮った画像の置き場は `ui/verification/` (視覚照合ループの最終周) であり、`ui/references/` は議論中に渡された参考画像の置き場である。相方の指摘はこの一般則としては正しい。

ただし本変更には承認済みモックが存在せず、[ui/brief.md](ui/brief.md) 冒頭が「HTML モックは生成せず、実機で撮影した候補から採用を選ぶ形で承認ゲートを置く」と定めている。照合すべき「見た目の正」が無い以上、`ui/verification/` の定義 (実装がその正と照合された証跡) に当てはまる対象がない。brief の「リファレンス注釈」節と [tasks.md](tasks.md) 2.2 はいずれも `ui/references/` を候補の置き場として明示指定しており、実装側がこれに従うのは足場の指示どおりの動き。移動すると足場の記述と実態が食い違い、逆流修正の禁止から deviation 記録が必要になる。同じ形は phase-9 (`changes/archive/2026-08-30-consolidate-readmes-and-contribution/ui/`) でも採られ、蒸留・アーカイブまで通っている。

### ホスト側 Suggestion 2 件の扱い

`question.yml` の textarea に `placeholder` が無い件と、CONTRIBUTING 3 段落で「いつこのテンプレートを使うか」の案内の有無が揃っていない件は、相方から対応する指摘がなかった (相方も「質問フォームの必須項目・ラベル・英日投稿案内は仕様どおり」「CONTRIBUTING 英日は同一粒度」と正常判定)。いずれもホスト側自身が「既存 2 本と揃っているため tasks 1.2 の観点では現状で揃っており、spec 違反ではない」と任意対応に位置づけているため、修正サイクルは回さない。
