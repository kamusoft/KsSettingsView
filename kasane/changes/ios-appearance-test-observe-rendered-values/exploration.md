# Exploration: ios-appearance-test-observe-rendered-values

## 課題 / 動機

起票時の見立て: iOS の外観切替テストが、行の背景を「保持している dynamic `UIColor` を検証側で `resolvedColor(with:)` した値」で見ている。この観測は UIKit が実際に背景を貼り直したかを示さず、描画が古いままでも緑になる (検出力が無い)。

maui-appearance-change-tracking (archive 2026-09-17) の調査で見つかり、同 change の tasks 3.1〜3.7 として計画されたが、不具合の前提が切り分けゲートで反証されて change ごと探索へ差し戻されたため未着手のまま残った。他の残スコープ (Cell の `AppThemeBinding`・Header / Footer 背景の Theme 適用・`AppThemeBinding` の回帰資産) は別 change が引き取り済みで、この項目だけ受け皿が無かった (2026-09-17 の蒸留でオーナー裁定により簡易起票)。

探索 (2026-09-26) の結論: 「検出力が無い」は過大だった。今の観測は自前のコード変更で起きうる壊れ方をほぼ捕まえており、取りこぼすのは起きうる場所の少ない 2 種類だけ。観測の置き換えは行わず、縮小した範囲で閉じる (決定事項)。

### 分かっている事実 (起票時)

- 該当箇所: `ios/Tests/KsSettingsViewUITests/ThemeDarkAppearanceRenderingTests.swift` の `appliedCellBackgroundColor` (`backgroundConfiguration` の色を `resolvedColor(with: cell.traitCollection)` で解決) と、`CellStyleAppearanceTests.swift` の同型の観測
- 同じテストファイルの list 下地は、描画画像の画素で観測済み
- 表示中の外観切替の追随そのものは、iOS Native 単体・MauiHost iOS (iOS 26.0 / 18.6) とも実測で成立している (archive 側 tasks.md の 0.1〜0.3 の切り分け結果)。直すのは製品の挙動ではなくテストの観測点
- 元の計画と Scenario は `kasane/changes/archive/2026-09-17-maui-appearance-change-tracking/` の design.md Decision 3、tasks.md グループ 3、`specs/settings-view-ios-ui/spec.md`

### 探索で分かった事実 (2026-09-26)

- `ThemeDarkAppearanceRenderingTests.swift` 冒頭コメントの「Cell の背景は `UIBackgroundConfiguration` が描く領域で、`CALayer.render(in:)` による画像化には現れないため画素では観測しない」は、e66ee88 (fix-default-colors-dark-appearance) で最初から書かれた一文で、画素観測を試した記録は archive に無い。`drawHierarchy` を試した記録も無い (`CustomCellTests.swift` の `drawHierarchy` のコメントは SwiftUI の hosted content についての別件)。写るかどうかは未確認のまま
- 製品は行背景を外観変化で再適用していない。描画のたびに dynamic `UIColor` を背景設定に入れ (`ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift` の `applyRenderedBackgroundColor`。標準 Cell と CustomCell 共通)、外観切替後の解決は UIKit に任せる (core/ADR-0030 の iOS の実現方法)。trait 変化を購読しているのは `KsCheckBoxView` と `SectionBoxDecorationView` の CGColor だけ。したがって起票時の論点「背景の再適用を一時的に止める改変で赤を確かめる」は、止める対象が無い
- 今の観測 (保持色を行自身の trait で解決) は、UIKit が描画時に行う解決と同じ計算をなぞる。自前のコード変更で起きうる壊れ方 (色を早期に固定値へ解決する・行に外観が伝わらない・違う色を入れる) は今の観測でも赤になる。取りこぼすのは次の 2 種類だけ
  - ① 行背景の上に別の不透明な色が塗られて隠れる: 行では起きうる場所が少ない。行の中身は背景を塗らず、CustomCell は `UIHostingConfiguration` で背景を行の背景設定に任せる (`ios/Sources/KsSettingsViewUI/CustomCellView.swift`)。416477d の tint は Header / Footer の View accessory の話で行ではない
  - ② 保持色は正しいのに UIKit が描き直さない: OS 側の不具合で、自前の変更から起きる余地が小さい
- 元の Decision 3 の根拠「既存テストは不具合を検出できなかった」は、その不具合 (MAUI iOS で行背景が白のまま) が切り分けで再現しなかったため成り立たない。疑われたのは MAUI iOS の経路で、iOS 単体のテストはそもそもそこを通らない
- 旧 3.7 (text Header / Footer 背景) は scroll-indicator-visible-not-applied (archive 2026-09-17) が実装・テスト済み (`ios/Tests/KsSettingsViewUITests/AccessoryBackgroundColorTests.swift`)。差は観測の形 (保持値の解決) だけ
- 旧 3.3 / 3.4 / 3.6 は未被覆
  - 3.3 (CustomCell の外観切替): 背景適用の経路は標準 Cell と同じだが、view のクラス (`CustomCellView`) と状態更新の処理が別
  - 3.4 (行背景の固定色): 固定の `UIColor` をそのまま入れるだけで、壊れる余地が小さい
  - 3.6 (スクロール途中の往復で可視行・offset を保つ): 元 Decision 1 (外観変化で再適用する処理) を守るための Scenario で、その処理は作られていない

## 検討した選択肢 (却下案と理由を含む)

- **A: 縮小して閉じる (採用)** — 観測の置き換えはやめ、CustomCell の外観切替テスト 1 本と、テスト冒頭コメントの書き直しだけを入れる。小さな手間で CustomCell の欠落が埋まり、見送った理由がコードの隣に残る
- **B: 何もせず破棄** — 却下。未検証の一文が残り、見送った理由も残らないため、同じ課題が再起票されうる
- **C: 当初どおり進める (観測を描画値へ置き換え + 旧 3.3 / 3.4 / 3.6)** — 却下。得られるのは ①② への保険だが、① は行で起きうる場所が少なく、② は OS 側の不具合。画素に写るかの当たり付けが要り、写らなければ UIKit 内部の view 構造に依存する観測となって iOS 更新のたびに壊れやすい
  - C の中での観測手段の比較 (結論に至る前に C ごと却下): 画素 (`layer.render(in:)`) と画素 (`drawHierarchy`) は ①② を捕まえ公開 API で済むが実現性が未確認。背景 view の layer の色は ② だけを捕まえ、UIKit 内部構造に依存する

## 決定事項

- 行背景の観測は今の形 (保持色を行自身の trait で解決) を維持し、描画値 (画素・layer の色) への置き換えは行わない (2026-09-26 オーナー裁定、A 案)
- 範囲:
  1. `ThemeDarkAppearanceRenderingTests.swift` の冒頭コメントから未検証の主張 (画素に現れない) を除き、「なぜ保持色の観測で足りるか (UIKit の解決と同じ計算をなぞる)」と「何を取りこぼすか (上の ①②)」を書く。`CellStyleAppearanceTests.swift` と `AccessoryBackgroundColorTests.swift` は未検証の主張を持たないため書き直しは必須ではない
  2. CustomCell を含む root をライトで表示したままダークへ切り替え、CustomCell の行の背景が dark 既定 (`#1C1C1E`) になり、行の identity が保たれるテストを 1 本足す。観測は既存と同じ (`CustomCellView` は `UICollectionViewListCell` なので `appliedCellBackgroundColor` がそのまま使える)。置き場は外観切替のハーネスを持つ `ThemeDarkAppearanceRenderingTests.swift`。足したテストが、CustomCell の行背景を固定の light 値にする一時改変で赤になることを確かめる (改変は戻す)
- 旧 3.4 / 3.6 のテスト追加、旧 3.7 の観測の置き換えは行わない

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

- なし。観測の流儀はテストだけで覆せ、platform 境界・公開 API に触れない (選別基準に該当しない)

## 未決の論点

- なし。論点1 (観測手段) と論点3 (検出力の確認方法) は置き換えをやめたため不要になり、論点2 (範囲) は決定事項のとおり

## UI 素材 (ui/references/ の一覧と注釈)

- なし

## 変更級の推奨: S (ios)

iOS のテストコード (テスト 1 本の追加とコメントの書き直し) だけを触り、製品コード・公開 API・他 platform に波及しない。可逆で局所的。
