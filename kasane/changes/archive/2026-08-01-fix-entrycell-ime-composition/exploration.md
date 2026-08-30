# Exploration: fix-entrycell-ime-composition

## 課題 / 動機

Android の EntryCell で日本語 IME 入力すると、「あ」と1文字入れた時点で変換候補が出ずに即時確定される (composing 状態が維持されない)。iOS・AiForms では発生しない。サンプル「入力 Cell 5 種デモ」の TwoWay 経路・callback 経路の両方で再現。

原因 (調査で特定):

1. IME で1文字入力 → `TextWatcher.afterTextChanged` → `onTextChanged` → Compose 状態更新
2. DSL 再構築 → 内容更新判定 → `KsSettingsListAdapter.submitContentUpdate` → payload なし `notifyItemChanged(position)`
3. `EntryCellViewHolder.bind()` がフル再実行され、`editText.inputType = ...` (EntryCellViewHolder.kt:85) が**値が同じでも毎回無条件に代入**される
4. `TextView.setInputType()` は内部で `InputMethodManager.restartInput()` を呼ぶため、フォーカス中の EditText の composing 状態が毎キーストロークで強制確定される

`setText` には差分ガードが既にある (bind 内、同値ならスキップ) が、`inputType` / `gravity` / `filters` / `highlightColor` 等には差分ガードがない。この中で composing 破壊の直接原因は `inputType`。

- Compose Bridge (`KsSettingsViewComposable`) は AndroidView で View 実装をラップしているだけなので、View / Compose どちらの経路でも同一の `EntryCellViewHolder` を通り再現する
- iOS が無事な理由: `UITextField` の trait 再設定は marked text に対して `restartInput` ほど破壊的でない

## 検討した選択肢 (却下案と理由を含む)

| 案 | 内容 | 評価 |
|---|---|---|
| A: IME 破壊系プロパティに差分ガード | `inputType` (念のため `filters` 等も) を「値が変わったときだけ」設定する | **採用**。即解消・リスク最小・S級 |
| B: payload 付き部分バインドへ構造改善 | `notifyItemChanged(pos, payload)` + 部分 bind を導入し、キーストローク由来の更新でフルリバインドしない | 却下 (今回は見送り)。根本的で将来の同種バグも防ぐが M 級で波及が大きい。必要になったら別変更として起こす |
| C: 自己 echo 時のリバインド抑止 | 入力元セルへの notifyItemChanged をスキップ | 却下。判定ロジックが汎用 Adapter に漏れて脆く、フォーカス外更新を取りこぼす恐れ |

## 決定事項

- A 案を採用 (ユーザー確定)。`EntryCellViewHolder.bind()` で IME を破壊するプロパティ (少なくとも `inputType`) に差分ガードを入れ、同値再バインドで `restartInput` が発生しないようにする
- ADR は起票しない (覆すコスト低・境界を越えない・将来を制約しない)

## ADR 候補

なし (未起票のまま終了してよい)

## 未決の論点

- 差分ガードの対象範囲: `inputType` は必須。`filters` / `hint` / `gravity` 等をどこまで含めるかは実装時判断 (IME 再接続を誘発し得るものを優先)
- テスト方針: Robolectric では IME composing の実挙動を再現しにくいため、「同値で bind() を再実行しても inputType の setter が呼ばれない (= restartInput 相当が走らない)」ことを検証する形が現実的

## UI 素材 (ui/references/ の一覧と注釈)

なし。再現スクリーンショット (「入力 Cell 5 種デモ」で名前欄に「ああ」が確定済みの状態) はチャット添付で提供されたが、見た目の議論ではないため保存対象外とした。

## 変更級の推奨: S (理由)

触る能力は EntryCell の Android 実装 1 箇所、公開 API 変更なし、可逆性高、UI の見た目変更なし。デルタスペック不要でそのまま実装 (Plan + テスト) に進める規模。

## 関連ファイル

- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCellViewHolder.kt` (bind: 85 行付近の inputType 代入)
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsListAdapter.kt` (submitContentUpdate: 74-81)
- `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/InputCellsDemoScreen.kt` (再現サンプル)

---

## 追記 (2026-08-01): A 案実装後にオーナー実機確認で未解消 → 並走調査で真因を再特定

A 案 (差分ガード) 実装・レビュー APPROVED 後、オーナーの実機確認で症状が全く解消していないことが判明。ksn-dual-research (codex + ホスト側 ksn-researcher の独立並走) で再調査した結果、両者一致で真因を特定した。

### 真因 (当初仮説の誤りの内容)

payload なし `notifyItemChanged(position)` + 既定 `DefaultItemAnimator` の組み合わせでは、`canReuseUpdatedViewHolder` が false を返し **ViewHolder が再利用されず新規生成・クロスフェードされる** (androidx `DefaultItemAnimator.java:669-673` / `SimpleItemAnimator.java:84-86`)。つまり1打鍵ごとに EditText インスタンスごと差し替わり、InputConnection が作り直されて composing が確定する。`bind()` 内の setter 差分ガードは「ガードした EditText 自体が捨てられる」ため原理的に効かない。

- `KsSettingsListAdapter.kt:52-53` の「notifyItemChanged は ViewHolder を破棄せず同一 ViewHolder に bind する」というコメントは既定 ItemAnimator 下では誤り (設計前提の誤りが不具合の根)
- ホスト側調査が Pixel 6a + Gboard で実機実測 (アニメーション 20 倍速 + 連続スクショ輝度解析) により「変更行だけがクロスフェード = ViewHolder 差し替え」を直接裏付け
- AiForms が無事な理由: テキスト変更で Adapter へ notify を返さない一方向設計のため (Web 調査で Xamarin.Forms 版ソースを確認)
- 同コードベースの Theme 更新は既に payload 付き (`PAYLOAD_THEME`) で正しく、内容更新経路だけが payload なしだった

### 改訂した決定事項

- **旧 A 案 (差分ガード) 単独では不十分**。ただし inputType ガードは「同一 ViewHolder 再利用になった後の必須防御」(AOSP で `setInputType` は無条件 `restartInput`) として維持する
- **旧 B 案の軽量形 + アニメーション抑止を採用**:
  1. RecyclerView 生成時に `supportsChangeAnimations = false` (ViewHolder 差し替えとちらつきを止める)
  2. `submitContentUpdate` を `notifyItemChanged(position, PAYLOAD_CONTENT)` に変更 (`PAYLOAD_THEME` の前例に倣う。payload 非空なら DefaultItemAnimator は同一 ViewHolder を再利用する)
  3. `KsSettingsListAdapter` の誤った前提コメントを修正
- 検証は Robolectric に加えて**実機 (Gboard 日本語) で変換候補表示と未確定維持を確認してから完了とする** (lessons/inbox: runtime-fix-reported-without-device-verification)

### 当初調査の敗因 (教訓済み)

コード読解のみで「setInputType → restartInput」を真因と断定し、実機での再現・確認をせずに完了報告した。調査報告自体に「実機未確認」と明記されていたのに修正サイクルへ実機検証を組み込まなかった。
