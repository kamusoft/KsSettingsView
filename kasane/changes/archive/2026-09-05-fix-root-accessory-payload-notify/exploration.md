# Exploration: fix-root-accessory-payload-notify

## 課題 / 動機

`RootHeaderFooterAdapter.view` の setter (RootHeaderFooterAdapter.kt:46-49) が、内容更新時に **payload なし** `notifyItemChanged(0)` を発行している。

android/ADR-0001「内容更新は payload 付き通知と change アニメーション無効で同一 ViewHolder を維持する」で確定した通り、payload なし notifyItemChanged は既定 `DefaultItemAnimator` 下で ViewHolder を再利用せず新規生成・クロスフェードする (`DefaultItemAnimator.canReuseUpdatedViewHolder` は payload 空で false)。

現状は `KsSettingsView` の `supportsChangeAnimations = false` に守られており実害は出ないが、Cell 側 (`KsSettingsListAdapter.submitContentUpdate` = `PAYLOAD_CONTENT` 付き) と防御の厚みが揃っておらず、ItemAnimator 設定への依存が一方にだけ残っている。利用者やテーマが itemAnimator を差し替えた場合に Root H/F だけ単層防御になる。

出典: fix-entrycell-ime-composition の review-002.md Suggestion 1。

### 動機の補正 (2026-09-05 探索でコード裏取り)

payload 付き通知で守れるのは **ViewHolder の再利用 (新規生成・クロスフェードの回避)** であり、Root H/F の View 形式の **利用者 View の作り直しは防げない**。`RootAnyViewAccessoryViewHolder.bind` → `bindKsAnyView` は `KsAnyView.AndroidView` のとき常に `removeAllViews` して factory から作り直すため、非 null → 非 null の再代入では payload の有無に関わらず View が再生成される (Compose 形式は既存 ComposeView を再利用して内容だけ差し替えるので payload の恩恵がある。Text 形式は TextView が保たれる)。

したがって本 change の効果は「ADR-0001 の二重担保を Header/Footer にも及ぼす」であって「入力状態 (IME・フォーカス) を守る」ではない。同一 View 再代入時の作り直しは別論点 (未決の論点 ②)。

## 検討した選択肢 (却下案と理由を含む)

| 案 | 内容 | 評価 |
|---|---|---|
| A: payload 付き通知に揃える | `notifyItemChanged(0, <内容 payload>)` へ変更 (Cell 側の流儀に統一) | **採用**。二重担保が Header/Footer にも及ぶ |
| B: 現状維持 (supportsChangeAnimations=false に依存) | 変更なし | 却下。利用者やテーマが itemAnimator を差し替えた場合に単層防御が消える |

payload 定数の置き場所 (review-002 Suggestion 3・ADR-0012 Consequences で「本 change で集約の重みが増す」と明記):

| 案 | 内容 | 評価 |
|---|---|---|
| ① 集約する | `PAYLOAD_CONTENT` / `PAYLOAD_HEADER_HEIGHT` を `KsSettingsView` の companion に `PAYLOAD_THEME` と並べる (internal のまま。公開 API は増やさない) | **採用** (ユーザー確定 2026-09-05)。参照元は main 2 ファイル + test 3 ファイル、S 級のまま |
| ② 既存定数を参照するだけ | `KsSettingsListAdapter.PAYLOAD_CONTENT` を Root 側から参照 | 却下。同一 RecyclerView の payload キーが 3 箇所目から参照され、置き場所の揺れが固定化する |

## 決定事項

- 公開前トリアージ (2026-08-21): **公開後でも可**。現状は `supportsChangeAnimations = false` で実害がなく、防御の二重化が目的
- 簡易 change として scaffold のみ作成 (オーナー指示 2026-08-01)。実装は未着手
- S 級のため proposal / デルタスペックは作らない (ksn-propose 規約)。実装時は直接実装 + テスト
- スコープ確定 (2026-09-05 ユーザー): **payload 付き通知 + payload 定数の集約**。同一 View 再代入の同値ガードは含めない (未決の論点 ②)
- 定数の可視性は internal を維持する。`PAYLOAD_THEME` だけが public であり、内容 payload を public にする理由はない

## ADR 候補

なし。android/ADR-0001 (accepted) の追随であり、定数の置き場所はコード配置の判断で ADR の選別基準 (覆すコスト高 / 境界を越える / 将来を制約) に当たらない。

## 未決の論点

- ② 同一 `RootAccessory.View` インスタンスを `rootHeader` / `rootFooter` に再代入したとき、setter に同値ガードがなく factory から View が作り直される (`RootAccessory.View.equals` はクラス一致のみなので等価比較は使えず、参照比較 `===` が候補)。DSL 経路 (`applyUpdateAccessory` → `extractRootAccessory`) で同じインスタンスが再代入され得るかを調べてから、別 change として扱うか判断する
- テスト形式: `AdapterDataObserver` の 3 引数版 `onItemRangeChanged` で payload を検証する (ContentUpdatePayloadTest.kt の `ChangeRecordingObserver` の流儀)。既存 RootHeaderFooterAdapterTest の `notifyItemChanged_0` テストは 2 引数版で件数しか見ていないため、payload 検証を足す

## UI 素材

なし (見た目の変更なし)

## 変更級の推奨: S (理由)

main 2 ファイル + test 3 ファイルの数行・公開 API 変更なし・可逆・UI 変更なし。android/ADR-0001 で確立済みのパターンの横展開と定数の移動。

## 関連ファイル

- `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/RootHeaderFooterAdapter.kt` (view setter: 46-49、クラス KDoc の `notifyItemChanged(0)` 記述も更新)
- `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsListAdapter.kt` (`PAYLOAD_CONTENT` / `PAYLOAD_HEADER_HEIGHT` の現在の置き場所、DiffCallback からの参照)
- `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt` (`PAYLOAD_THEME` の隣が集約先)
- `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SectionAccessoryViewHolders.kt` (`RootAnyViewAccessoryViewHolder` / `bindKsAnyView`。動機補正の根拠、変更対象ではない)
- テスト: `RootHeaderFooterAdapterTest.kt` (payload 検証を追加)、`ContentUpdatePayloadTest.kt` / `ListAdapterDiffTest.kt` / `FullUpdateContentSyncTest.kt` (定数の参照先を追随)
- 関連 ADR: android/ADR-0001 (二重担保)、android/ADR-0012 (定数集約の重み)
