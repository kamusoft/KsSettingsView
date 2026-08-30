# Exploration: fix-root-accessory-payload-notify

## 課題 / 動機

`RootHeaderFooterAdapter.view` の setter (RootHeaderFooterAdapter.kt:46-49) が、内容更新時に **payload なし** `notifyItemChanged(0)` を発行している。

fix-entrycell-ime-composition で確定した通り、payload なし notifyItemChanged は既定 `DefaultItemAnimator` 下で ViewHolder を再利用せず新規生成・クロスフェードする (`DefaultItemAnimator.canReuseUpdatedViewHolder` は payload 空で false)。Root Header / Footer は `RootAccessory.View` として任意の View (EditText 等の入力 View を含み得る) を載せられるため、内容更新のたびに View が差し替わると IME composing・フォーカス・スクロール状態が破壊され得る。

現状は fix-entrycell-ime-composition で入れた `KsSettingsView` の `supportsChangeAnimations = false` に守られており実害は出ないが、Cell 側 (`KsSettingsListAdapter.submitContentUpdate` = `PAYLOAD_CONTENT` 付き) と防御の厚みが揃っておらず、ItemAnimator 設定への依存が一方にだけ残っている。

出典: fix-entrycell-ime-composition の review-002.md Suggestion 1。

## 検討した選択肢 (却下案と理由を含む)

| 案 | 内容 | 評価 |
|---|---|---|
| A: payload 付き通知に揃える | `notifyItemChanged(0, <PAYLOAD_CONTENT>)` へ変更 (Cell 側の流儀に統一) | **採用方向**。二重担保が Header/Footer にも及ぶ |
| B: 現状維持 (supportsChangeAnimations=false に依存) | 変更なし | 却下。利用者やテーマが itemAnimator を差し替えた場合に単層防御が消える |

実装時の小論点: payload 定数を `KsSettingsListAdapter.PAYLOAD_CONTENT` の参照にするか、`RootHeaderFooterAdapter` 独自定数にするか (review-002 Suggestion 3「payload 定数の置き場所の集約」と合わせて判断してよい)。

## 決定事項

- 公開前トリアージ (2026-08-21): **公開後でも可**。現状は `supportsChangeAnimations = false` で実害がなく、防御の二重化が目的。数行なので手が空いたときに
- 簡易 change として scaffold のみ作成 (オーナー指示 2026-08-01)。実装は未着手
- S 級のため proposal / デルタスペックは作らない (ksn-propose 規約)。実装時は直接実装 + テスト

## ADR 候補

なし (fix-entrycell-ime-composition の蒸留で「内容更新は payload 付き通知を正とする」が concepts / ADR 化されるなら、本変更はその追随)

## 未決の論点

- payload 定数の共有方法 (上記の小論点)
- テスト形式: `AdapterDataObserver` の payload 検証 (ContentUpdatePayloadTest.kt の流儀を踏襲) で足りる見込み

## UI 素材

なし (見た目の変更なし)

## 変更級の推奨: S (理由)

1 ファイル数行・公開 API 変更なし・可逆・UI 変更なし。fix-entrycell-ime-composition で確立済みのパターンの横展開。

## 関連ファイル

- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/RootHeaderFooterAdapter.kt` (view setter: 46-49)
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsListAdapter.kt` (PAYLOAD_CONTENT の前例)
- `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ContentUpdatePayloadTest.kt` (テストの流儀)
