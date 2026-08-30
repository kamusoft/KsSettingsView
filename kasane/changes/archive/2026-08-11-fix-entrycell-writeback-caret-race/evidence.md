# 実機検証記録: fix-entrycell-writeback-caret-race

tasks.md グループ 4 (実機検証) の証跡。Robolectric の緑は実 IME 挙動を保証しないため、
Pixel 6a 実機で修正前後の A/B を取得した。

## 検証環境

| 項目 | 値 |
|---|---|
| 端末 | Pixel 6a (serial <android-device-serial>) / Android 16 |
| 日付 | 2026-08-11 |
| IME | Gboard (com.google.android.inputmethod.latin) |
| IME 言語 | バースト試験: **English (US) QWERTY** / IME 試験: **日本語 QWERTY** |
| 対象アプリ | MAUI サンプル (`jp.kamusoft.kssettingsview.samples.maui`) / native サンプル (`jp.kamusoft.kssettingsview.samples.android`) |
| 画面 | 入力 Cell 5 種デモ |
| 対象 Cell | **メール欄** (`tanaka.taro@example.com`、双方向バインディング経路) |

### 対象 Cell に名前欄ではなくメール欄を使った理由

両サンプルの名前欄には `maxLength = 20` が設定されている
(native: `InputCellsDemoScreen.kt:102` / MAUI: `InputCellsDemoPage.xaml:27`)。
`repro-burst-loop.sh` は試行間でフィールドを初期化しないため、名前欄では 2 試行で 20 文字に
飽和し、以降の全試行が「注入しても値が伸びない」= FAIL として計上される。これは
**maxLength による切り詰めであってレース由来の破損ではない**。実際に名前欄で測った初回の
試行では修正前・修正後の双方が 20 文字ちょうどで停止しており、判別に使えなかった。
上限のないメール欄に切り替えて計測している。

### フォーカス確立の手順 (サンプルごとに異なる)

- **native**: 入力欄をタップ → `KEYCODE_MOVE_END` でキャレットを末尾へ
- **MAUI**: 入力欄の**右端** (x=1030) をタップしてキャレットを末尾へ。MAUI では
  `KEYCODE_MOVE_END` がリストの縦スクロールを誘発して対象欄が可視領域から外れるため使えない
  (本変更のスコープ外の MAUI ホスト側の挙動。試験手順として回避した)

いずれもフォーカス確立後は**タップを挟まず** `input text abcde` のみを注入する
(exploration の交絡なし条件 v5 と同じ)。

## 4.1 / 4.2 バースト入力試験 — 修正前後の A/B

同一端末・同一 IME・同一 Cell・同一手順で、実装のみを入れ替えて計測した。
修正前は `git checkout` で HEAD (修正なし) に戻してリビルド・再インストールし、
計測後に修正版へ復元している。

| 対象 | 実装 | pass | fail | skip | 有効試行 | 判定 | ログ |
|---|---|---:|---:|---:|---:|---|---|
| native サンプル | **修正前** | 8 | **10** | 2 | 18 | NG (56% 破損) | [ablation-native-before-fix.txt](evidence/ablation-native-before-fix.txt) |
| native サンプル | 修正後 | 17 | **0** | 3 | 17 | **OK** | [burst-native.txt](evidence/burst-native.txt) |
| MAUI サンプル | **修正前** | 11 | **7** | 2 | 18 | NG (39% 破損) | [ablation-maui-before-fix.txt](evidence/ablation-maui-before-fix.txt) |
| MAUI サンプル | 修正後 | 18 | **0** | 2 | 18 | **OK** | [burst-maui.txt](evidence/burst-maui.txt) |
| MAUI サンプル | 修正後 (復元後の再確認) | 18 | **0** | 2 | 18 | **OK** | [burst-maui-confirm.txt](evidence/burst-maui-confirm.txt) |

合格条件 (有効試行 15 以上・FAIL 0) を 4.1 / 4.2 とも満たす。

修正前の FAIL は exploration と同じ 2 つの壊れ方を示した:

- **欠落**: `abcde` 注入に対し `abc` / `ab` しか入らない
- **並び替え**: `abe` / `abde` (native iter 12-13、MAUI iter 14)

修正後は両サンプルとも破損 0。修正前の破損率 (39〜56%) に対して 17〜18 連続 pass が
偶然起きる確率は 10^-4 未満であり、この A/B はハーネスの検出力を伴った有意な差である。

### 補足: ハーネスの検出力の確認過程

名前欄で行った予備計測では、修正前に iter 1-3 で実際の文字欠落
(`Tanaka Taro` → `+abc` → `+abcd` → `+ab`) を観測した後、20 文字飽和により停止した。
この飽和は当初「入力不能化」と誤読しかけたが、修正前後とも**ちょうど 20 文字**で止まる
ことから maxLength 由来と特定し、上限のないメール欄での再計測に切り替えている。

## 4.3 バースト入力後の入力継続

修正前の exploration では、MAUI でバースト後に IME desync が起きフォーカスも IME 接続も
生きたまま入力が一切通らなくなる二次被害が観測されていた (v5)。

| 対象 | 手順 | 結果 |
|---|---|---|
| native | 20 回のバースト直後に `ZZ` を注入 | **OK** — 末尾に `ZZ` が追記された |
| MAUI | 20 回のバースト直後に `ZZ` を注入 | **OK** — 末尾に `ZZ` が追記され、画面上の「最後のイベント」ラベルにも `...abcdeZZ` まで到達 |

MAUI では表示だけでなく書き戻し経路 (ViewModel まで) にも最終値が正しく届いていることを
「最後のイベント」ラベルで確認した。

## 4.4 日本語 IME 変換中の内容更新エコー

IME を日本語 QWERTY に切り替え、MAUI サンプルの名前欄 (空にした状態) で確認した。

| 手順 | 入力欄の状態 | 書き戻しエコー (最後のイベント) |
|---|---|---|
| ローマ字 `aiueo` を注入 | 未確定 `あいうえお` (下線+ハイライト表示・変換候補バー生存) | `名前 → あいうえお` |
| ローマ字 `kanji` を注入 | 未確定 `かんじ` | `名前 → かんじ` |
| 変換キーを押下 | 未確定 `感じ` (**変換操作が成立**) | — |
| Enter で確定 | `感じ` | `名前 → 感じ` |

**結果: OK。** 打鍵ごとに書き戻しエコー (同一 Cell の再バインド) が届いている状態でも、
未確定文字列は確定・破棄されず、変換操作 (かんじ → 感じ) が最後まで通った。
視覚的な証跡は [evidence-02-ime-composing-preserved.png](evidence/evidence-02-ime-composing-preserved.png)
— 名前欄の `あいうえお` に composing の下線とハイライトが乗り、変換候補
(あいうえお / あいうえお順 / アイウエオ) が生きていることが確認できる。

## 4.5 通常操作の回帰

MAUI サンプルで確認 (fix-maui-entrycell-focus-loss の再発がないことを含む)。

| 項目 | 手順 | 結果 |
|---|---|---|
| 低速入力 | 英語 IME で `H` `e` `l` `l` `o` を 1 秒間隔で注入 | **OK** — `Hello` |
| 日本語 IME の変換確定 | 4.4 の `かんじ` → `感じ` 確定 | **OK** |
| BackSpace 連続削除 | 高速に 3 連続 DEL | **OK** — `感じ` が空になり placeholder `山田 太郎` を表示 |
| フォーカス移動 | 名前 → 電話 → 名前 | **OK** — フォーカスが追随し、名前欄の値 `Hello` は blur 再同期で巻き戻らず保持 |
| 移動直後の入力継続 | 名前欄へ戻った直後に `XY` 注入 | **OK** — `HelloXY` |

フォーカス移動でフォーカスが失われる事象 (fix-maui-entrycell-focus-loss) の再発はない。
バースト 20 回中もフォーカスは一度も外れていない (外れていれば以降の注入が別の欄に入るが、
全試行で対象欄の値のみが伸びている)。

## 本検証で確認できていないこと

- **キャレット位置の直接観測**: `repro-burst-loop.sh` は最終 text と要素 bounds しか
  観測せず、キャレット位置そのものは測っていない。ただし全試行で注入文字列が
  **末尾に**追記されていることは、キャレットが末尾から動いていないことの間接証拠になる
  (キャレットが移動していれば注入位置がずれて `before + abcde` の一致が崩れる)。
  キャレット非移動の直接検証は Robolectric テスト側が担っている
- **iOS の同型レース**: 本変更の Non-Goals。未検証
- **Gboard 以外の IME**: 未検証
