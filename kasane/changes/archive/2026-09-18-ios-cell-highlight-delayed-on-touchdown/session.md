# Live Session: ios-cell-highlight-delayed-on-touchdown
対象: iOS で Cell を速くタップしたとき、指を置いた瞬間に押下色 (Theme.selectedColor) が出るようにする。まず collection view のタッチ遅延 (delaysContentTouches) を切る ① を Sample アプリに当てて実物で確かめ、遷移中に押下色を残すか (②) は結果で決める
開始: 2026-09-18
出典: exploration.md (決定事項・未決の論点)

## 試行ログ (append-only)
- 試行 1 (①): `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:419` の collection view 生成を `UICollectionView` → 新規 `ImmediateTouchCollectionView` (internal、`touchesShouldCancel(in:)` 常に true) に置き換え、直後で `delaysContentTouches` 既定 true → false。Simulator iPhone 17 (iOS 26.5) のサンプルで確認 → (1) 速いタップで押下色が即出る: OK (2) Switch の上からの縦ドラッグ: リストが動くこともあればスイッチが反応することもあり、スイッチ自体の反応も鈍い: 要調整 (3) スクロール開始時のチラつき: 気にならない → 継続 (2 を次の試行で調整)
- 試行 2 (Switch の扱い): `ios/Sources/KsSettingsViewUI/ImmediateTouchCollectionView.swift:26` `touchesShouldCancel(in:)` 常に true → view 自身と superview 連鎖に UISwitch / UISlider があれば false (スライド系コントロール上のタッチはリストがキャンセルしない)、それ以外は true。`delaysContentTouches = false` は維持。Simulator に反映後、実機 iPhone 11 (pixie4, iOS 18.7.8) へも配備 (署名はコマンドライン渡し、project.pbxproj は無変更)。iPhone 15 (pixie5, iOS 26.6.1) へも配備完了 (Team への端末登録を伴う)。実機 2 台で確認 → iPhone 11: (1) スイッチの反応は問題なし (2) スイッチ上からの縦ドラッグはスクロールしない (スイッチ操作が優先) — 許容するか次の試行で調整するかはユーザー判断待ち。iPhone 15: 同じ結果。加えて押下色の出方が速すぎてスクロール開始時に一瞬反応するのが気になる (Simulator では気にならなかったが実機では目立つ) → 継続 (試行 3 で押下色の立ち上がりを調整)
- 試行 3 (押下色の立ち上がり): `ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift:94` 押下色の適用 即時代入 → `UIView.animate` (定数 `selectedColorFadeInDuration` = 0.12 秒、easeIn、beginFromCurrentState、allowUserInteraction)。同 :105 非押下への復帰は進行中アニメーションを打ち切って即時。3 台 (Simulator / iPhone 11 / iPhone 15) に配備。実機評価: ダメ — 押下色が早く消えすぎて一瞬で終わる (立ち上がりを緩めても解除 (②) が即時のままなので、フェードインの途中で消え始める) → 却下 (単独では不採用。試行 4 で ② を変えた後にフェードインを残すかを再評価)
- ユーザーの指摘 (2026-09-18): 「遷移してから色が変わる」体感の本命は ② (didSelectItemAt で即座に deselect している解除タイミング) かもしれない。サンプルには CommandCell からのページ遷移が無いので、確認用に遷移を仕込む必要がある → 試行 4 で ② を扱う
- 試行 4 (② 解除タイミング): `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:2513` `didSelectItemAt` の `defer` 即時 deselect → tapHandler 後に猶予 (定数 `selectionHoldDuration` = 0.1 秒) を置き、その間に `viewWillDisappear` が来なければ解除。同 :455-476 `viewWillAppear` / `viewWillDisappear` を新規 override し、戻り際は `transitionCoordinator` に合わせて解除 (無ければ即時)。解除対象は `indexPathsForSelectedItems` 全件。サンプル `samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift` の「プロフィール」CommandCell に確認用の push 遷移を追加 (deviation.md に記録)。試行 3 のフェードインは残したまま。3 台に配備、実機評価: (1) 遷移中は残るが戻ったときには既に消えている = 解除が早すぎる (2) Radio / Checkbox のタップはチラつく = フェードイン・フェードアウトが欲しい (3) スクロール開始では反応しない方がよい → 継続 (試行 5 で 3 点を調整)
- 試行 5 (立ち上がり遅延・フェードアウト・戻り後の解除): `ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift:119-183` 押下色の処理を `updateSelectedColor` / `fadeInSelectedColor` / `restoreNormalColor(fadesOut:)` に分割 — 押下は遅延予約 (キャンセルで破棄、isSelected なら待たず塗る)、解除は選択経由ならフェードアウト・キャンセルなら即時。定数 3 本を同 :187-192 に集約 (`selectedColorHighlightDelay` 0.1 / `selectedColorFadeInDuration` 0.12 / `selectedColorFadeOutDuration` 0.25 秒)。`KsCellViewState` に予約・表示状態のフィールドを追加し `resetSelectedColor` を `KsListCellBase.swift:355` / `CustomCellView.swift:121` の `prepareForReuse` から呼ぶ。`KsSettingsViewController.swift:460-465` 戻り際の解除を `viewWillAppear` + coordinator → `viewDidAppear` で `deselectAllItems(animated: true)` (`selectionHoldDuration` 0.1 は据え置き)。3 台に配備、実機評価: (1) 戻ってきてから消えるまでが遅い → もう少し早く (2) Radio の Type B の Cell だけ速いタップで押下色が付かない (長押しなら付く) → 要調査 (3) スクロール開始時に反応しなくなった: OK → 継続 (試行 6 で 1・2 を調整)
- 試行 6: (A) `KsSettingsViewController.swift:460-472` 戻り際の解除を `viewDidAppear` → `viewWillAppear` + `transitionCoordinator.animate(alongsideTransition:)` 内 (pop 中にフェードアウト。coordinator 無しは即 deselect)。(B) Radio TypeB 不発の原因: タップで値が変わる Cell は直後に再構成され、`CellBaseLayout.swift` の render が `backgroundConfiguration` に平常色を入れて押下色を消していた (TypeA は選択済みで再構成が起きず症状が出ない)。修正: `KsCellViewSupport.swift:71-82` に `applyRenderedBackgroundColor` を新設 (押下色の表示中は平常色で塗り替えない)、`CellBaseLayout.swift:117-127` / `CustomCellView.swift:53-61` の render の背景適用を `setRenderState` の後へ移してこの関数経由に置換。Simulator で TypeB の選択・表示が正常なことをワーカーが確認。3 台に配備、実機評価: (2) Radio TypeA/TypeB・Checkbox の速いタップ: OK (3) 立ち上がりとスクロール無反応: OK (1) 戻り後の消え方: iPhone 11 (iOS 18) は完璧、iPhone 15 (iOS 26) は少し早い → もう一瞬遅らせる → 継続 (試行 7)
- 試行 7 (戻り時の解除を一瞬遅らせる): `KsSettingsViewController.swift:462-476` coordinator の completion で `isCancelled` を確認し、通過時のみ `Task.sleep(returnDeselectDelay)` 後に解除。定数 `returnDeselectDelay` = 0.08 秒 (同 :487)。ただし解除開始が「遷移完了後 + 0.08 秒」になっており指示 (遷移中 + 0.08 秒) と違う (試行 5 より遅くなる) → ユーザー確認前に指揮側が差し戻し (試行 7b)
- 試行 7b: `KsSettingsViewController.swift:462-483` `viewWillAppear` で `Task.sleep(returnDeselectDelay = 0.08 秒)` を開始し、経過時に coordinator が非インタラクティブなら遷移中のまま `deselectAllItems(animated: true)`、インタラクティブ (エッジスワイプ) なら completion で `isCancelled` が false のときだけ解除。3 台に配備、実機評価: iPhone 11 / iPhone 15 とも (1) 戻るボタンでの消え方 (2) エッジスワイプ、どちらも完璧 → 採用 (完了指示: 2026-09-18)

## 決定事項
- 2026-09-18: 見た目・挙動を試行 7b の状態で確定 (ユーザーの完了宣言「1, 2 どちらも完璧」)
- 2026-09-18: 押下色の立ち上がり (遅延 0.1 秒 + フェードイン 0.12 秒) とタップ後のフェードアウト (0.25 秒)、値が変わる Cell の再構成で押下色を消さない扱いは試行 6 の形で OK
- 2026-09-18: スクロール開始時に押下色が反応しない挙動は試行 5 の形 (立ち上がり遅延 0.1 秒 + キャンセル時は即時復帰) で OK
- 2026-09-18: スイッチ (スライド系コントロール) の上から始まったタッチはスイッチ優先で、縦ドラッグでもリストはスクロールしない (試行 2 の形で確定)。縦横の向き判定でスクロールに振る調整 (試行 3 候補) は行わない — ユーザー判断: 「スイッチのスクロール制御は別にがんばらなくてよい」

## レビュー (review-001.md、2026-09-18)
- 判定 CHANGES_REQUESTED (Minor 3 / Suggestion 3)。挙動不変の B-1〜B-3 はワーカーで修正中。挙動が変わりうる A-1 (フェードイン進行中のキャンセルが即時復帰にならない: `backgroundView?.layer.removeAllAnimations()` が no-op) / A-2 (pageSheet の present では viewWillDisappear が呼ばれず「present は選択を残す」が成立しない) / A-3 (EntryCell の文字選択ドラッグの実機未確認) はオーナー判断待ち

- A-1: オーナー判断「気にならないので何もしない」→ 挙動据え置き、summary / ADR-0005 の記述を実態に合わせて修正 (2026-09-18)

- A-2: オーナー判断 (a) 記述を実態に合わせる → summary / ADR-0005 を修正、コード変更なし (2026-09-18)

- B-1〜B-3 適用済み (テスト 2 件追加 / `delaysContentTouches` の設定を `ImmediateTouchCollectionView` の init へ / 解除 Task を `pendingDeselectTask` 1 本で保持)。`ios/ADR-0005` のコードコメントを 3 箇所に付与。テスト 1061 件 / 0 failures (2026-09-18)

- A-3: 実機で EntryCell の文字選択ドラッグを確認、問題なし (evidence.md に追記、2026-09-18)

- A-1 の掃除: no-op の `removeAllAnimations` 2 行を削除しコメントを実態に修正。テスト 1061 件 / 0 failures。2 周目の独立レビュー (review-002) を起動 (2026-09-18)

- review-002: CHANGES_REQUESTED (Minor 2 / Suggestion 3、挙動が変わる指摘なし)。遅延中キャンセルのテストの検出力ゼロ (layoutIfNeeded 不足) / evidence.md の件数が古い / テスト冒頭の列挙 / コメントの push・present 表現 / viewWillAppear 側の解除 Task の保持 — 全件ワーカーで修正中、3 周目で確認予定 (2026-09-18)

- review-002 の 5 件を修正 (テストの検出力はミューテーションで実証、原状復帰 shasum 一致)。テスト 1061 件 / 0 failures。3 周目 (上限) の独立レビュー review-003 を起動 (2026-09-18)

- review-003: CHANGES_REQUESTED (Major 1)。review-002 の 5 件は閉じた。新規 Major: `didSelectItemAt` が `isDisappearing = false` を handler の後に置くため、UIKit 直接ホストの同期 push では遷移の合図が上書きされ選択が残らない (SwiftUI ホストは成立)。上限 3 周に達したためオーナーに相談 → (a) 修正 + この 1 点に絞った 4 周目の独立確認、で承認。ワーカーで修正中 (2026-09-18)

- review-003 Major 修正済み (`isDisappearing = false` を handler の前へ、回帰テスト 1 件、evidence.md 追記)。テスト 1062 件 / 0 failures。4 周目 (この 1 点に絞った確認) review-004 を起動 (2026-09-18)

- review-004: APPROVED (Minor 1 = evidence.md の記録訂正のみ。指揮側で訂正済み)。テスト 1062 件 / 0 failures。change の完成 (2026-09-18)

## エスカレーション・スコープ外の発見
- review-003 / 004 の所見: 新規テストの `@Sendable` クロージャが var を捕捉 (テストターゲットは Swift 6 確認の範囲外、恒久切替時に露出しうる)。既存行の main actor 分離警告 (`sectionTextGap`) は本変更由来ではない
