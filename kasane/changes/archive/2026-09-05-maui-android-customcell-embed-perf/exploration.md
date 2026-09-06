# Exploration: maui-android-customcell-embed-perf

## 課題 / 動機

customcell-android-maui-perf の調査 (2026-08-28、MAUI Android CustomCell のスクロールカクつき) で見つかった構造課題 2 件。当該 change では「Release ビルドでは支配項でない (native 同等以上の実測)」ため見送りとなったが、課題自体は実在する:

1. **MAUI 埋め込みが Compose の再利用最適化から外れている** — native CustomCell は android/ADR-0015 で `ReusableContent` によるノード再利用が効くが、MAUI 埋め込み (`AndroidView`、android/kssettingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeCellContentView.kt) は行がリサイクルされるたびにノード破棄→再生成→factory 再実行 (wrapper view の detach→attach) が走る
2. **wrapper に measure キャッシュが無い** — `KsAccessoryHostView.OnMeasure` (maui/KsSettingsView.Maui/Platforms/Android/KsAccessoryHostView.cs) は制約の同一判定も結果キャッシュもなく、毎回 MAUI 側の全ツリー計測 (`IView.Measure`、Label のテキスト計測含む) を呼ぶ。1 と組み合わさり、スクロール中の毎バインドでフル再計測になる

## 検討した選択肢 (却下案と理由を含む)

### 2026-09-05 の裏取りで前提が変わった点

- **課題 1 の原因は「非 reusable 判定」ではなく `key(token)`** — 埋め込みは世代トークンで `key(token)` に包まれている。Compose の `Composer.start()` (runtime 1.7.5 ソース) は reusing 中でも「グループキー + objectKey が一致する既存グループ」しか引き当てないため、Cell ごとに異なるトークンで包まれた `AndroidView` は `onReset` の有無に関係なく常に新規挿入になる。したがって **`onReset` を渡すだけの案は効かない** (却下)。
- さらに現設計は factory が「その Cell の輸送 View」を返すため、仮に再利用が成立すると factory が再実行されず前の Cell の View が出たままになる。reusable 化は「危険」以前に「不正」。効かせるには **factory が行ごとの空の入れ物を返し `update` で輸送 View を付け替える形** (iOS の `KsBridgeCellContentHostView` 方式に相当) への作り替えが必要で、view-materialization.md「Android に iOS のような行ごとの入れ物が無い理由」節の再検証・書き直しを伴う設計変更 (M〜L 級)。
- **課題 2 は前提どおり実在** — dotnet/maui main の `VisualElement.Measure` / `IView.Measure` に制約キーのキャッシュは無い (旧 `_measureCache` は obsolete な `GetSizeRequest` 専用)。wrapper 側で「制約 → 結果」を持ち `MeasureInvalidated` で捨てるキャッシュは maui/ADR-0016 が「共有部の上に載せる用途固有ポリシー (計測キャッシュ・幅補正等)」として想定済みの形。ただし効果は未計測。

### 進め方の選択肢 (2026-09-05)

| 案 | 内容 | 評価 |
|---|---|---|
| A | Release で「効くか」を先に数える (1 bind あたりの `IView.Measure` 回数、行数 / content の重さを振った gfxinfo) | **採用**。B/C/D を根拠で選べる。実機セッション 1 回 |
| B | 計測せず measure キャッシュだけ S 級で入れる | 保留。効果の裏付けなしに ADR-0016 の「本家の計測契約に追随する負債」だけ増える |
| C | 行ごとの入れ物方式へ作り替える | 保留。M〜L 級、入れ物の所有規則を Android にも持ち込む |
| D | 閉じる (知見だけ concepts へ) | 保留。A の結果次第 |

### A の計測結果 (2026-09-05、evidence/measurements-2026-09-05.md)

Pixel 4a / 6a 実機、Release、現行 content (行あたり View 7 個) と重い content (View 15 個) の 4 構成 + Pixel 4a の atrace:

- **課題 2 (measure キャッシュ) は効かない — 却下**。measure は bind 1 回あたり 1.5〜3.2 回、1 回の所要は中央値 0.1〜0.2ms、最悪構成でも 1 フレーム平均 0.3ms (予算の 2%)。wrapper の再実体化は 0 件
- **カクつきの正体は行リサイクル時の「輸送 View の再親付け」から派生する下流コスト**。重い content の Pixel 4a で Janky 14.5% / p90 73ms に転じたが、bind のあるフレーム (と直後のフレーム) の中央値 36ms に対し、それ以外は 1ms。内訳は inset の再配布 (`dispatchApplyInsets`、`addView` に伴い毎回ウィンドウ全体へ走る、約 9〜12ms)・描画の記録 (約 15ms)・埋め込み View の Android 側 layout (`Compose:onPositionedCallbacks` 内の MAUI Arrange、約 10ms)・再 composition (約 4ms)
- 閾値の目安: Pixel 6a 級では View 15 個でも Janky 1% (p90 19ms) で予算内。Pixel 4a 級 (2020 年ミドル) では View 7 個で p90 25ms (境界)、View 15 個で明確にカクつく
- **課題 1 の「行ごとの入れ物」案 (C) も主因を取り除かない**: 入れ物を再利用しても輸送 View は行ごとに入れ物の間を移動する (再親付けは同数)。減るのは Compose ノード再生成分 (再 composition の一部) だけ

### 計測後の選択肢

| 案 | 内容 | 評価 |
|---|---|---|
| B | measure キャッシュ | **却下** (計測で効果なし) |
| C | 行ごとの入れ物方式 | 主因 (再親付け) を残す。効果は再 composition の一部のみ。保留 |
| E (新) | **MAUI content を持つ行をリサイクルしない** — Cell と行 (ViewHolder) を固定し輸送 View を付けたままにする。再親付け・inset 再配布・再 layout・ノード再生成がすべて消える。MAUI 側は content View を全件生かしているため、増えるメモリは行の器 (ComposeView) 分のみ | 唯一主因に効く。native の行リサイクル契約 (android/ADR-0015 の前提、RecyclerView の viewType / isRecyclable) と bridge からの指定経路に触れる M 級以上。効果は未実証 (試作要) |
| F | 既知の制約として閾値を concepts に記録して閉じる | 現行 content は両端末で予算内。ローエンド × 重い content の組は「Cell content を軽くする」利用者側の指針で吸収 |

## 決定事項

- 2026-09-05: 進め方は A (計測先行)。B/C/D は計測結果で判断する
- 2026-09-05: B (measure キャッシュ) は計測により却下
- 2026-09-05: **F を採用 — コード変更は行わず、知見を concepts に残して change を閉じる** (オーナー判断)。E (MAUI content 行のリサイクル停止) は実利用でローエンド端末の報告が出たときの再開方向として本メモに残す

## ADR 候補 (作成済み: なし / 未起票: なし)

- 現時点でなし。C を採る場合は「Android の埋め込みに行ごとの入れ物を持つ」が ADR 級 (view-materialization の構造根拠を覆す)

## ksn-concept への申し送り (F の実施内容)

concepts に残す知見 (置き場所の候補は括弧内):

1. MAUI 埋め込みの再利用は `key(token)` により `onReset` の有無に関係なく成立しない。factory が Cell ごとの輸送 View を返す設計では reusable 化は不正 (前の Cell の View が出る) — view-materialization.md「Android — key(token) と行タップの返し」節の補強、および「reuse 経路」段落の根拠更新
2. 行リサイクル時の主コストは輸送 View の再親付けから派生する inset 再配布・描画記録・Android 側 layout・再 composition で、wrapper の measure は 1 フレーム 0.3ms 以下 (計測キャッシュは効かない) — view-materialization.md の Android 節、または性能検証の規約 (concepts/maui の performance-verification) に閾値の目安 (Pixel 6a 級: View 15 個/行で予算内、Pixel 4a 級: 7 個で境界・15 個でカクつき) とあわせて
3. view-materialization.md の「compose-ui 1.7.5 ソースで確認」は、bridge が実際に解決する 1.9.5 との差を明記する (再検証条件の更新。1.9.5 の破棄チェーンは未確認)
4. E (リサイクル停止) を将来の再開方向として、この change の証跡パスとともに残す

証跡: evidence/measurements-2026-09-05.md と evidence/scripts/ (change を閉じる際は、前例 archive/2026-08-28-customcell-android-maui-perf/evidence と同様に参照可能な場所へ移してから concepts から指す)

## 未決の論点

- E を再開する場合: bridge から native へ「この行はリサイクル不可」を伝える経路 (CustomCell の属性か bridge 専用の viewType か)、ADR-0015 の pool-aware 前提 (プール投入時の deactivate が子 View を外す) との整合、試作後の同手順 (evidence/scripts) での再計測
- **concept の前提要確認**: view-materialization.md は「compose-ui 1.7.5 ソースで確認」と記すが、bridge が実際に解決するのは compose-ui 1.9.5 (BOM 2025.11.01、`gradlew :kssettingsview-bridge:dependencies` で確認)。1.9.5 の `AndroidViewHolder` (onDetach / onRelease / onDeactivate / onReuse) に子側 detach が増えていないかは未確認 (手元のキャッシュに 1.9.5 の ui sources 無し)。deactivate 回帰テスト (`KsBridgeCustomCellDeactivateTest`) は 1.9.5 で通っている

## UI 素材 (ui/references/ の一覧と注釈)

- なし

## 変更級の推奨: 変更なし (F) — ksn-concept へハンドオフ

- コード変更なし。知見の concepts 追記は ksn-concept で行う (上記申し送り)
- E を再開する場合の暫定: M 級以上 (native ui の行リサイクル契約 + bridge + view-materialization の Android 節)
