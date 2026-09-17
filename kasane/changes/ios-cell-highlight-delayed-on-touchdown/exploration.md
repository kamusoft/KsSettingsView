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

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

## 未決の論点

- 未探索 (簡易起票)
- 仮説 (未検証): `KsSettingsViewController` の collection view で `delaysContentTouches = false` にすれば、TouchDown で即座にハイライトが出る。実機で症状が消えるかをまず確かめる
- 副作用の確認: スクロールを始めたときにハイライトが一瞬出てしまわないか (標準の Settings アプリとの見え方の比較を含む)
- Cell 内のコントロール (Switch / Entry など) との兼ね合い: 遅延を切ったとき、コントロール上から始めたドラッグでスクロールできるか。`touchesShouldCancel(in:)` の既定 (`UIControl` 上ではキャンセルしない) を上書きする必要があるか
- CustomCell (利用者が任意の View を置く Cell) への影響: 置かれた View のタッチ処理が変わらないか
- 自動テストで押下タイミングを検証できるか (collection view の属性の assert に留めるか、実機目視を確認手段にするか)

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: 未判定 (暫定 S / ios)

暫定の根拠: 仮説どおりなら iOS 系統の collection view 設定 1 箇所 (+ 必要なら `touchesShouldCancel` の上書き) で完結し、公開 API と他 platform に波及しない。副作用の確認で公開オプション化や挙動の取り決めが要ると分かれば見直す。確認は実機での体感が主になるため、ksn-live での調整も候補。
