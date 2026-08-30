# Exploration: fix-entrycell-writeback-caret-race

## 課題 / 動機

EntryCell への高速連続入力で、文字の欠落・並び替え・キャレット位置ずれが発生する。初出は
fix-maui-entrycell-focus-loss の実機検証中に 1 回だけ観測された `Tanaka|` + a,b,c,d,e →
`Tanakabcdea` (期待 `Tanakaabcde`)。本探索で構造的レースであることをコードで確認し、実機で
高再現率の再現に成功した。**壊れた値は書き戻しで ViewModel (アプリ状態) まで確定する**ため、
表示だけの問題ではない。

## 実測記録 (2026-08-11, Pixel 6a / bluejay 実機, adb `input text` バースト注入)

判定はいずれも「実行後の値 == 実行前の値 + 注入文字列」。低速入力 (打鍵間隔 1 秒以上) では
全アプリで破損 0。

### MAUI サンプル (jp.kamusoft.kssettingsview.samples.maui, fix-maui-entrycell-focus-loss 適用済みビルド)

| 試行 | 条件 | 結果 |
|---|---|---|
| v1: 名前欄 + タップあり + Gboard (英/日混在) | 交絡あり (参考値) | 10/10 破損 (`Tanaka Tarabco` = d,e 欠落+o 転置 等) |
| v3: 電話欄 (数字キーパッド、composing なし) + タップあり | 交絡小 | 8/15 破損 (欠落 `1234`・`12`、**並び替え `1212`**) |
| v5: 名前欄 + **タップなし** (フォーカス保持・キャレット末尾維持) + English qwerty | 交絡なし | 2/2 破損 (`abcde`→`abcd`、e 欠落) 後、**IME desync で入力完全不能化** |

- v5 の入力不能化: フォーカスフラグ `.F` も `mInputShown=true` も維持されたまま、以後の
  `input text` が一切反映されなくなる (名前欄に spell-check 下線が残存)。レースの setText が
  Gboard の composing 状態を破壊した二次被害とみられる。別フィールドへの再フォーカスで回復。
- 破損値はサンプルの「最後のイベント」ラベルにも出る = `EntryCellTextChanged` → ViewModel
  まで到達して確定している。

### Android native サンプル (同一手順の対照)

| 試行 | 条件 | 結果 |
|---|---|---|
| v3: 電話欄 + タップあり | 座標ノイズ混入 (タップがメール欄に逸れた分を含む) | 15 中 2 破損 + 3 無効 |
| v5: メール欄 + タップなし + English qwerty | 交絡なし | 10 有効中 4 破損 (`abc` 止まり ×3、**並び替え `abe`** ×1) |

### 陰性対照 (素の EditText = 設定アプリ検索欄、同一注入)

- **8/8 全て正常**。adb 注入・Gboard は文字を落とさない。破損は KsSettingsView スタック固有。

## 原因 (構造、コードで確認済み)

書き戻しラウンドトリップの「スナップショット確定 → bind 適用」の間に次の打鍵が挟まると、
bind が古い値で EditText を巻き戻す:

1. 打鍵 → `EntryCellViewHolder.bind` 内の TextWatcher (同期) → MAUI では
   `KsSettingsController.EntryCellTextChanged` → `cell.ValueText` 書き込み →
   `ScheduleFlush` が **dispatcher post (非同期)** で Flush を予約
2. 次のループで `Flush` → `KsBridgeGateway.ReplaceCell(cellId, ToDto(cell))` —
   **この時点のスナップショットが確定** → store.replaceCell → `notifyItemChanged(payload)` →
   **次フレームのレイアウトで bind 実行**
3. bind は `editText.text?.toString() != cell.text` のとき `setText(cell.text)` +
   `setSelection(cell.text.length)` (EntryCellViewHolder.kt:95-104)。スナップショット確定〜
   bind 実行の窓 (1 フレーム前後) に打鍵が挟まると EditText の方が先に進んでいるため、
   **古い値への巻き戻し + キャレットの旧末尾への移動**が起きる。以降の打鍵が旧位置に挿入され
   欠落・並び替えになる。あわせて setText が IME の composing を破壊し、desync すると入力
   不能化に至る (MAUI v5 で観測)。

native (Compose DSL / Store 経路) も「recomposition (非同期) → store.replaceCell →
次フレーム bind」という同型のラウンドトリップを持つため同じレースが存在する。MAUI は
dispatcher post + bridge 変換のぶん窓が広く、再現率が高い (8/15 vs 2/15)。

エコー抑止の現行実装 (maui/ADR-0012: 書き戻し入口の同値チェック、bind 側の差分判定) は
「native が既に次の文字へ進んでいる」ケースを止められない — 同値でないからこそ setText が
走る、という構造。

## 検討した選択肢 (未決 — ユーザーと議論して確定する)

- **案A: bind 側ガード「フォーカス中の EntryCell は EditText が値の SSoT」** —
  `editText.isFocused` の間は内容更新による setText をスキップし、フォーカス喪失時に最新の
  cell.text へ再同期する。ui 層 1 箇所の修正で MAUI / native DSL / Store 利用者すべてに効く。
  IME composing を壊さなくなるため入力不能化も同時に解消する見込み。トレードオフ: 編集中の
  プログラム的な値変更 (ユーザーコードが ValueText を変換して書き戻す使い方) の反映が
  フォーカス喪失まで遅れる — 挙動契約の変更であり ADR 級。
- **案B: 世代トークン方式** — 打鍵通知に単調増加の通番を付けて往復させ、bind 時に「自分が
  送った通番より古いスナップショットなら setText しない」。エコーと外部変更を厳密に区別できる
  が、bridge DTO・core の輸送契約に手が入る (境界を越える変更、iOS も巻き込む)。
- **案C: C# Controller 側でエコー配信を抑止** — interaction 由来の書き戻しでユーザーコードが
  値を変えなかった場合、native への ReplaceCell 配信自体を抑止する。MAUI の窓は塞がるが
  native DSL 経路のレース (実測 4/10) が残るため単独では不十分。案A の補強としてはあり得る。
- **案D: キャレット相対位置の保存・復元 (対症療法)** — setText 前後でキャレットを補正する。
  巻き戻しによる文字欠落自体は解けないため第一候補にしない。

## 決定事項

- (未決 — 修正方針の確定は次ターン以降)

## ADR 候補

- 未起票。案A 採用なら「フォーカス中の EntryCell 入力欄は値の SSoT — 内容更新の反映は
  フォーカス喪失まで遅延する」が ADR 級 (挙動契約・全 platform に波及・覆すコスト高)。
  maui/ADR-0012 (書き戻しは必須コミット) との関係整理も必要。

## 未決の論点

1. 案A〜D の選定 (推奨: 案A、必要なら案C を併用)
2. iOS に同型レースがあるか未検証 (UITextField 経路の書き戻しラウンドトリップ構造の確認と
   実機バースト試験が必要)
3. native (Compose DSL / Store) 側も同一変更で直すか、platform ごとに change を分けるか
4. IME desync による入力不能化が案A で本当に消えるかの実機確認 (修正後の完了条件に含める)

## UI 素材

- なし (挙動のみの修正)。`evidence-01-maui-first-repro.png` は再現時の画面記録。
  `repro-burst-loop.sh` はタップなしバースト注入の再現スクリプト (修正後の検証に再利用可)。

## 変更級の推奨: M (理由)

- ui 層の bind 挙動契約の変更 + ADR 起票 + MAUI / native 両サンプルでの実機検証が必要。
  触るファイルは少ないが、挙動契約が全 platform (iOS 対称性の確認込み) を跨ぐため S では
  収まらない。案B へ進む場合は輸送契約変更で L 寄りの M。

## 実測ログ出典

- 本探索セッション (2026-08-11)。再現手順: `repro-burst-loop.sh` (前提: 対象欄フォーカス済み・
  English qwerty・キャレット末尾。タップなしで `input text abcde` を反復し相対期待値で判定)。
