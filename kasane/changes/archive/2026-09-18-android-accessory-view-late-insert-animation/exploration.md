# Exploration: android-accessory-view-late-insert-animation

## 課題 / 動機

(2026-09-17、`../ColorAnalyzer` の change `migrate-remaining-pages-to-kssettingsview` より越境の簡易起票。出典: `../ColorAnalyzer/kasane/changes/migrate-remaining-pages-to-kssettingsview/session.md`)

**症状** (Android 実機 Pixel 6a、KsSettingsView.Maui 0.1.0-beta.4 系、毎回再現):
`Section.FooterView` に MAUI View (Label) を置き、`FooterText` を持たない Section があるページへ遷移すると、遷移直後はフッターが表示されず下の Section が詰まった位置に出る。数百 ms 後にフッターが現れ、下の行がスライドダウンする。AiForms.Maui.SettingsView には無かった挙動。同じページの `CustomCell` は最初のフレームから正しく描画される。

**依頼元の調査で読んだ根拠** (起票時に実物で行の実在を確認し、行番号を実物に合わせた):

- 初回配信に footer が載らない: `maui/KsSettingsView.Maui/SettingsView.cs:984-989` で `Controller.Connect` (→ `SetRoot` 送信) が先、`AttachViews` が後。View の実体化は `maui/KsSettingsView.Maui/Handlers/SettingsViewHandler.cs:80-124` の `Loaded` → `OnHostAttached` → `ApplyHostViews` まで遅れる (`maui/KsSettingsView.Maui/Platforms/Android/KsBridgeGateway.cs:200-213` のコメント「実体化されていない位置は view なしになり…実体化の後に明示の更新で送り直される」)
- `FooterText` 未設定で FooterView だけの Section は、その時点で footer 行そのものが存在しない: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:1338-1366` の `flatten` が `shouldShowFooter` (`:1395-1397`、`hasAccessoryContent` で null / 空 text を内容なしとする) を満たさない footer 行を出さない
- Loaded 後の accessory 更新で footer 行が**挿入**として現れる: 同 `KsSettingsView.kt:981-1004` の `updateSectionAccessoryAndSubmit` → `mainListAdapter.submitList` (`KsSettingsListAdapter.kt:14` のとおり `AsyncListDiffer` による差分適用)
- 本体は change アニメーションだけを無効化している: 同 `KsSettingsView.kt:102` の `supportsChangeAnimations = false`。add / move は既定の `DefaultItemAnimator` が生きたまま
- accessory の再計測はブリッジ越しでディスパッチャ 1 tick 遅れる: `maui/KsSettingsView.Maui/Platforms/Android/KsAccessoryHostView.cs:43,74-78` の `MeasureInvalidated` → `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:691-700` (`_measureDirtySlots` に積んで `ScheduleFlush`) → `:1714` (`_dispatcher.Dispatch(Flush)`) → `:1726` の `Flush`。`CustomCell` の content は同 `:892` のとおり再計測コールバックが空 (`static () => { }`) で、この往復が無い
- 利用側で避ける公開設定は無い: `Section` に `FooterHeight` は無く (`maui/KsSettingsView.Maui/Section.cs:119,253` は `HeaderHeight` のみ)、ItemAnimator を差し替える公開 API も無い

**利用側の状況**: ColorAnalyzer は**本体の修正を先に行う方針** (オーナー決定 2026-09-17)。利用側では回避策 (`FooterText` への置き換え) をあえて入れず、`../ColorAnalyzer/ColorAnalyzer/Views/Others/AnalysisSettingsPage.xaml` の FooterView 2 箇所を修正確認のサンプルとして残している。同アプリのフィードバック画面 (FooterView に横スクロールの画像リスト) も移行予定で、同じ症状が出る見込み。

## 探索で確認した事実 (2026-09-17、コードで検証)

| # | 事実 | 確度 |
|---|---|---|
| 1 | 初回配信 (SetRoot) に MAUI View の Header / Footer は載らない。View の実体化が `Loaded` → `OnHostAttached` → `ApplyHostViews` まで遅れる (`maui/KsSettingsView.Maui/SettingsView.cs:981-986`、`maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:329-355`) | 確定 |
| 2 | 待ちの正体はディスパッチャの tick ではなく `VirtualView.Loaded` イベント。体感の数百 ms の主因はここ | 確定 / 主因は推定 |
| 3 | 再計測の 1 tick 遅れ (`InvalidateAccessoryMeasurement`) は hosted view の `requestLayout()` だけで、行の再挿入・再 submit は起こさない。従属的 | 確定 |
| 4 | CustomCell の content も同じく `ApplyHostViews` まで未実体化。Cell 行は内容に依らず必ず存在するため後着は内容更新になり、change アニメーション無効 (android/ADR-0001) で目立たない | 確定 |
| 5 | `Section.HeaderView` も同じ症状 (`shouldShowHeader` は footer と対称) | 確定 |
| 6 | RootHeader / RootFooter も RecyclerView の行 (`ConcatAdapter` の 3 段)。`RootHeaderFooterAdapter` が null→非 null で `notifyItemInserted(0)` を直接呼ぶ | 確定 |
| 7 | iOS も初回配信に view accessory は載らず、後着は `reloadSections` + `apply(animatingDifferences: true)`。`performWithoutAnimation` は不使用。実機での見え方は未検証 | 確定 / 見え方は不明 |
| 8 | `Loaded` まで待つ理由として文書化されているのは root accessory だけ (maui/ADR-0016、`kasane/concepts/maui/api/native-bridge.md` の「Host の生成と解放」、core/ADR-0019)。Section accessory は Store 状態で、attach 前配信でも購読時に復元される | 確定 |
| 9 | 実体化の前提 (`MauiContext`) は `CreateHost` 時点で揃う。wrapper は platform view の親も Host 世代も要求しない。ただし `Connect` と `AttachViews` の順序入れ替えだけでは足りない (`AttachViews` は `_views` を格納するだけ) | 確定 |
| 10 | Android で root accessory が attach 前に失われるのは、Bridge が Store の更新通知経由で流すため (`android/kssettingsview-bridge/.../KsSettingsBridge.kt:367-378`)。Host の `rootHeader` / `rootFooter` プロパティへの直接セットは購読と無関係に効く | 確定 |
| 11 | add / move / remove アニメーションに依存する機能・テストは見当たらない (D&D 並べ替え未実装。itemAnimator に触れるテストは `ContentUpdatePayloadTest.kt` の `supportsChangeAnimations == false` の 1 本) | 確定 |
| 12 | Native 単体では症状は起きない (設定ツリーに `SectionAccessory.View` を入れて初回に渡せる。root は Host プロパティへ直接セット)。例外は `store.updateAccessory` で attach 前に root accessory を渡す経路で、Android のみ取りこぼす (core/ADR-0019 で呼び出し側責務と明文化済み)。Compose / SwiftUI ラッパーは root accessory に触れていない | 確定 |

## 検討した選択肢 (却下案と理由を含む)

### 論点1: 直ったと言える状態

- **A: 最初のフレームから出す (採用)** — 体感の主因が `Loaded` 待ちであり、アニメーションだけ消しても「遅れて現れて下がずれる」が残るため
- B: 遅れは許容し挿入アニメーションだけ消す — 却下。根本原因が残り、瞬間的な位置ずれが残る
- C: 行の枠だけ初回配信に載せる — 却下。高さが不明で結局ずれる。core/ADR-0023 (内容の無い Header / Footer は行を作らない・三値は却下) と衝突し、Section モデル + 両 OS + Bridge を触る重さに見合わない

### 論点2: 画面全体の Header / Footer (RootHeader / RootFooter)

- **X: 画面全体も最初のフレームから出す (採用)** — RootHeader は先頭行で、後着すると全行がずれ最も目立つ。Section / Root で MAUI 側の経路を分けずに済み、View 配置の `Loaded` 待ち機構を無くせる見込み
- Y: 後から届くまま挿入アニメーションだけ消す — 却下。全行の瞬間移動が残り、MAUI 側の経路が 2 本のまま
- Z: 今回は触らない — 却下。同じ領域の課題を別 change に逃がさない方針

### 論点3: 取りこぼさない手当てを入れる層

- **H: Host 側で直す (採用)** — Native 直接利用者の罠 (事実 12 の例外経路) も消え、iOS と挙動がそろう。core/ADR-0019 の却下理由 (「Android は既に安全なのに保証しないと書くと実態と乖離する」) とも、罠を契約で残さず直す方向で整合する
- G: Bridge 側で Host プロパティへ直接渡す — 却下。変更は薄い層で小さく済むが、MAUI 利用者しか救わず Android Native にだけ罠が残る

### 論点4: CustomCell の content も初回配信に間に合わせるか

- **一緒に前倒しする (採用)** — accessory と content は同じ 1 回 (`ApplyHostViews`) で後から届けており、片方だけ前倒しすると MAUI 側の経路が 2 本に分かれる。content も「空の行 → 後着で高さが変わる」ずれを起こしている。前倒しすれば `ios-customcell-initial-height-grow-animation` の「後着」仮説が消え、原因の切り分けになる
- Header / Footer だけ前倒しする — 却下。範囲は小さいが経路が 2 本になり、`Loaded` 待ちの機構が残る

## 決定事項

- ゴールは「遷移直後の最初のフレームから、MAUI View の Header / Footer が正しい位置に出ている」(論点1 = A)
- 対象は `Section.FooterView` / `Section.HeaderView` / RootHeader / RootFooter のすべて (論点2 = X)。iOS も同じ facade 経路なので同時に確認する
- Android の「attach 前に届いた root accessory を取りこぼさない」手当ては Host 側に入れる (論点3 = H)。`store.updateAccessory` を attach 前に呼ぶ Native 直接利用者も救い、iOS の「順序に依存しない」へ対称化する。core/ADR-0019 の「所有者が attach 後に適用する責務」と Bridge 契約の「Android は attach 後に再適用」は制約が緩む方向の改訂になる (既存利用者は壊れない)
- CustomCell の content も Header / Footer と同じ経路で初回配信に間に合わせる (論点4)。View 配置についての `Loaded` 待ちは丸ごと無くす方向。content の世代トークン (maui/ADR-0020) と破棄順序 (maui/ADR-0016) との整合は提案段階の設計で詰める

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

- 作成済み: core/ADR-0033 (proposed) — Root の header / footer は取り付け前に渡されても Host が失わない (core/ADR-0019 を amends)

## 未決の論点

- Host 側の手当ての設計: 「root accessory は Store の現在状態に含めない」(core/ADR-0005 の責務分離、core/ADR-0019 の復元対象外) を崩さずに、attach 前に届いた未反映分をどう拾うか
- MAUI 側で初回配信前に実体化を通す道の設計 (maui/ADR-0016 の論理所有と platform lease の寿命分離・Handler 1:1 制約との整合)。View 配置について `Loaded` 待ちを完全に無くせるか
- iOS の後着時の見え方 (実機未検証)。初回配信に載れば後着自体が無くなる見込みだが、確認は要る
- Section accessory は 1 slot ずつ単発で `UpdateAccessoryView` を呼んでおり、Cell で問題になった「単発を連ねると Android で反映通知が追い越される」と同型のリスクが残る。FooterView が複数ある画面は未検証
- 表示済み画面へ実行時に View の Header / Footer を足したときの挿入アニメーションは現状維持でよいか (正当な実行時変化として扱う想定)
- (解消 2026-09-17) iOS の Native 直接利用でも、view load 前に Store の更新口から渡した Root の header / footer は失われると確定 (`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift` の `applyDiff` が view load 前は `.full` 以外を捨て、`resyncFromStore` は Root accessory を対象外とする。Bridge の `makeHostViewController` は view load を強制しない)。iOS にも手当てを入れる
- Handler の切断 → 再接続 (タブ切替・ページ再表示) は初回配信 (`RebuildRoot`) をやり直さないため、前倒しを初回接続だけに入れると再接続時の後着が残る。再接続時にも間に合わせる設計が要る
- Host が取り付け前の分を保持する方法: Store に state 外の別フィールドで持たせる案は core/ADR-0005 の却下済み案 (Store の責務肥大) に当たる。Host が bind 時点から Root 対象の更新だけ購読する案は ADR と衝突しない。design で比較する

## 関連する change

`kasane/changes/ios-customcell-initial-height-grow-animation` (iOS で CustomCell の高さが遷移直後に後から増える)。別 change のまま進めるが、次の 3 点でつながっている。

- 共有部: どちらも MAUI View を自己計測 wrapper で実体化し、`Loaded` 後に `ApplyHostViews` で後から届ける同じ仕組みの上にある (事実 4)
- 競合する原因仮説: あちらの起票メモは「幅未確定の間は無限幅で測る」を原因候補にしているが、「content が `Loaded` 後に遅れて届き、内容差し替えが `animatingDifferences: true` で適用される」でも同じ見え方を説明できる。どちらが主因かは未検証。content も初回配信に間に合わせれば後者は消えるので、この change の後にあちらの再現確認をすると切り分けになる
- この change が iOS に持ち込み得る退行: 実体化を前倒しすると、wrapper の最初の計測が幅 0 (無限幅) で行われる機会が増える。利用側の観察では現状 iOS の FooterView に遅延・高さのブレは出ていないので、折り返す Label を持つ FooterView で iOS 実機の退行確認が要る

## 訂正

- 探索中に挙げた「android/ADR-0001 の残課題の記述が古い」は誤り。同 ADR の footer に 2026-09-05 の現行照合があり、残課題の解消 (`fix-root-accessory-payload-notify`) と Alternatives の「現存」の解消が記録済み。accepted の本文は不変で、事後の事実は footer に書く規約どおりの状態なので、修正は不要

## UI 素材 (ui/references/ の一覧と注釈)

なし (見た目の変更ではなく表示タイミングの修正)

## 変更級の推奨: S / M / L (理由)

L (オーナー確定 2026-09-17)

- 触る能力: MAUI facade (実体化と配信の順序)・Android の Host (取り付け前の Root header / footer の保持)・iOS (保証の確認と、必要なら手当て) の 3 つにまたがる
- 能力間でそろえる設計判断がある: 両 OS の Host 保証 (core/ADR-0033、core/ADR-0019 を amends) と、それを前提にした MAUI 側の `Loaded` 待ち撤去
- 公開 API の追加は無い見込みだが、Host の観察可能な保証が変わる
- 既存の決定 (maui/ADR-0016 の寿命分離と破棄順序・maui/ADR-0020 の世代トークン・core/ADR-0005 の責務分離) との整合設計が要る
- UI の見た目は変えない (表示タイミングの修正) ので ui/ は作らない。実機確認は Android (Pixel 6a) と iOS 実機の両方で要る
