# Exploration: ios-cell-highlight-delayed-on-touchdown

## 課題 / 動機

iOS で Cell を素早くタップすると、押下時の背景色 (`Theme.selectedColor` によるタッチフィードバック) が指を置いた瞬間 (TouchDown) に出ない。色が出る前に指が離れて選択・遷移が始まるため、利用者には「遷移してから色が変わっている」ように見える。長押し (0.5 秒程度) なら色は出る。

依存側リポジトリ KsAppKMP のリファレンスアプリ (SwiftUI の `KsSettingsView { }` で 7 画面を描画、採用版 0.1.0-beta.4) で、オーナーが実機 (iPhone 15) で確認した。Command Cell / Radio Cell とも同じ症状。経緯は `../KsAppKMP/kasane/changes/kssettingsview-adoption/` の review-004.md と ui/brief.md の照合記録にある。

依存側ではライブラリが collection view を公開していないため、アプリから設定を変えて回避できない。Android は同じ `Theme.selectedColor` の指定でタップ直後にリップルが出るので、OS 間で体感差になっている。

### 分かっている事実 (起点側で裏取り済み)

- **押下時の背景そのものは仕様どおり描かれている**: シミュレータ iOS 26.4 で押下を保持したまま連続撮影し、行の画素が `selectedColor` を Cell 背景に重ねた計算値と一致した。色の値ではなくタイミングの問題
- **押下時に塗る経路**: `ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift` の `installSelectedColorHandler` (`configurationUpdateHandler` で `isHighlighted || isSelected` のとき背景を差し替える)。登録元は `ios/Sources/KsSettingsViewUI/KsListCellBase.swift` と `ios/Sources/KsSettingsViewUI/CustomCellView.swift`
- **遅延タッチの設定は一切していない**: tag 0.1.0-beta.4 と develop の現時点とも、`ios/` 配下に `delaysContentTouches` / `canCancelContentTouches` を設定している箇所が無い (git grep で 0 件)。`UIScrollView` の既定は `delaysContentTouches = true` で、スクロールかタップかの判定のあいだ (約 0.15 秒) Cell へ押下状態が伝わらない
- collection view は `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift` で生成している (`UICollectionView(frame:collectionViewLayout:)`)。SwiftUI 版は同じ controller を包むので、ここで設定すれば両方式に効く見込み

### 探索で分かった仕組み (2026-09-18)

症状に効いている層は 2 つある。

| 層 | 場所 | 何が起きるか |
|---|---|---|
| ① 押下が Cell に届くまでの遅れ | `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift` の `loadView` (collection view 生成部。背景色・`keyboardDismissMode` だけ設定し、タッチ遅延は既定のまま) | `delaysContentTouches` 既定 true のため、速いタップでは指を離した瞬間に「押下→選択」がまとめて Cell へ届く。押下色が出るのが TouchUp と同時になる |
| ② 選択の解除タイミング | 同ファイル `collectionView(_:didSelectItemAt:)` の `defer` で即座に `deselectItem(animated: true)` | TouchUp と同時に `tapHandler` (遷移) が走り、同時に押下色のフェードアウトが始まる。① と重なって「遷移してから色が変わった」ように見える |

Apple 標準の設定アプリは ② について「戻ってきたときに解除する」流儀 (選択が push 中も残る)。① を直しても ② が今のままなら「色が一瞬しか出ない」体感が残る可能性がある。

既存の決定・記述との関係: iOS の ADR (0001〜0003) にタッチ処理の決定は無い。`concepts/core/styling/cell-visual-states.md` は「押下・選択で `Theme.selectedColor`」と観察可能な挙動だけを書き、タイミングは規定していない (衝突なし)。relations の KsDialogs (sibling) は overlap が `ios/Sources` に掛からない。

## 検討した選択肢 (却下案と理由を含む)

ゴールの置き方 (2026-09-18 に提示):

- **(a) 指を置いた瞬間に押下色が出る** (Android のリップルと同じ体感。① のみ直す)
- **(b) (a) に加えて遷移中も押下色が残る** (Apple 設定アプリの流儀。② の解除タイミングも変える。選択状態を accessory で表す既存方針とのすり合わせが要る)
- **(c) まず ① だけ実物で試し、(b) にするかはその結果で決める** → 採用

## 決定事項

- 2026-09-18: ゴールは (c)。まず ① (`delaysContentTouches = false`) を Sample アプリに当てて実物で確かめ、② を変えるかは結果を見て判断する
- 進め方: 正解を見ながら追い込む調整型なので ksn-live (ライブ調整モード) で進める
- ① にはほぼセットで `touchesShouldCancel(in:)` を true に上書きする `UICollectionView` のサブクラスが要る (UIKit 既定では Switch / TextField 等の UIControl 上から始めたドラッグでスクロールが始まらなくなるため)

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

- 作成済み: ios/ADR-0005 (proposed、2026-09-18。ライブ調整の確定時にユーザーが起票を選択)

## 未決の論点

- 仮説 (未検証): ① で TouchDown 直後に押下色が出るようになるか。ksn-live で実物確認する
- ② の解除タイミングを変えるか ((b) にするか)。① の結果を見て判断する
- 副作用の確認: スクロールを始めたときにハイライトが一瞬出てしまわないか (標準の Settings アプリとの見え方の比較を含む)
- Cell 内のコントロール (Switch / Entry など) との兼ね合い: 遅延を切ったとき、コントロール上から始めたドラッグでスクロールできるか。`touchesShouldCancel(in:)` の既定 (`UIControl` 上ではキャンセルしない) を上書きする必要があるか
- CustomCell (利用者が任意の View を置く Cell) への影響: 置かれた View のタッチ処理が変わらないか
- 自動テストで押下タイミングを検証できるか (collection view の属性の assert に留めるか、実機目視を確認手段にするか)

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: S / ios

根拠: 触るのは controller の collection view 設定 1 箇所 + `UICollectionView` サブクラス 1 つ (`touchesShouldCancel`) で、公開 API と他 platform に波及しない。可逆。UI の見た目に関わるが、確認手段は実機・シミュレータでの体感になるため ksn-live で進める。② まで踏み込む場合も同じ change に含める (隣接課題)。

(起票時の暫定の根拠: 仮説どおりなら iOS 系統の collection view 設定 1 箇所 (+ 必要なら `touchesShouldCancel` の上書き) で完結し、公開 API と他 platform に波及しない。副作用の確認で公開オプション化や挙動の取り決めが要ると分かれば見直す。確認は実機での体感が主になるため、ksn-live での調整も候補。)
