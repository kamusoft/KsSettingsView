# レビュー結果: implement-modern-style (001 回目)

**日付**: 2026-08-20
**判定**: CHANGES_REQUESTED

## サマリー

デルタスペック 4 能力の Requirement / Scenario はおおむね忠実に実装されており、ビルド・テストは iOS 549 件 / Android 2490 件 (`--rerun-tasks` で再実行) ともに 0 failure、サンプル 2 本のビルドも成功、視覚証跡 (`ui/verification/` 15 枚) も実在し提出コードと対応していることを確認した。テストは Robolectric 実表示経路・実 layout 経路の観測点を取っており、代理値でごまかしていない点も良い。

一方で iOS 側に、承認済み合成契約 (箱の角丸 clip) が**構造変更後に破綻する**欠陥を 1 件検出した (実測で再現確認済み)。Android は同じ契約を毎描画時に解決するため影響を受けず、iOS 側だけがキャッシュ方式による片側欠陥になっている。これは Major として修正を求める。あわせて iOS の clamp 実装の重複 (テストの回帰検出力が実描画経路に届いていない) を Minor で挙げる。

## 確認した観点

- ビルド / テスト: `xcodebuild test -scheme KsSettingsView-Package` → `Executed 549 tests, with 0 failures` / `./gradlew test --rerun-tasks` → 2490 tests / 0 failures (test-results XML 集計)。`samples/ios` `xcodebuild build` → BUILD SUCCEEDED、`samples/android` `./gradlew assembleDebug` → 成功
- コメント規約: `python3 scripts/comment-policy-lint.py` = 禁止 0 件。未追跡ファイルを含めた明示パス走査 (303 ファイル) でも禁止 0 件 / 要確認 2 件 (いずれも本変更の diff 外の既存行)
- 足場: `specs/` `proposal.md` `design.md` は未変更 (更新は `tasks.md` のチェックと `ui/brief.md` の照合メモのみ)。tasks.md の [x] はすべて対応する成果物が存在する
- deviation.md 記録済みの 5 件 (iOS margin の複合方式 / Root H/F 内側配置 / Android H/F 行の水平 inset / ボーダーの onDrawOver / 角丸 clip の被覆方式 / iOS の zIndex 回避) は合意済み差分として指摘対象から除外した
- サンプル一致 (`concepts/cross/conventions/sample-parity.md`): 画面タイトル「Section 装飾デモ（style 切替）」・4 Section の構成・全文言・プリセット名/値・バッジ地色 RGBA が iOS / Android で一致。spike 画面は `verifications` に分離され「検証」表記を持つ
- 視覚証跡 (lessons/process.md L-003): `ui/verification/` に iOS 5 枚・Android 6 枚・モック横並び 4 枚が実在。`compare-mock-vs-ios-modern-standard.png` と `android-modern-bordered.png` を実見し、提出コードのデモ画面 (Section 構成・文言・プリセット) と一致することを確認した

## 指摘事項

### [🟠 Major] iOS: Cell の挿入 / 削除後に隣接 Cell の箱 clip が更新されず、箱の角丸が破綻する

**該当箇所**:
- `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1073` 付近 (`applySectionBoxClip(to:at:)` の呼び出しが `cellProvider` 内だけ)
- `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1443` 付近 (`applyFullSnapshot` は内容が変わった item しか reconfigure しない)
- `ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift` (`KsCellViewState.sectionBoxClip` は Cell インスタンスに紐づくキャッシュ)

**問題点**:
`SectionBoxCellClip` は Cell の dequeue / reconfigure 時にしか解決されず、Cell インスタンスの associated object に保持される。`SettingsRootDiff` で Section の先頭 / 末尾が変わっても、隣接する既存 Cell は reconfigure 対象にならないため clip が古いまま残る。

一時プローブで実測 (プローブは実行後に削除済み):

```
// 末尾に Cell を挿入した後
PROBE old(item1)=SectionBoxCellClip(cornerRadius: 26.0, borderWidth: 0.0, roundsTop: false, roundsBottom: true)
      new(item2)=SectionBoxCellClip(cornerRadius: 26.0, borderWidth: 0.0, roundsTop: false, roundsBottom: true)
// 末尾 Cell を削除した後 (item1 が新しい末尾)
PROBE afterRemove(item1)=SectionBoxCellClip(cornerRadius: 0.0, borderWidth: 0.0, roundsTop: false, roundsBottom: false)  // = .none
```

- **削除ケース**: 新しい末尾 Cell の clip が `.none` になり mask が外れる。Cell は `backgroundConfiguration` で行全面を不透明に塗るため、箱の角丸の下端が Cell の四角い背景で塗りつぶされ、**下地 (`Theme.backgroundColor`) が見えるべき角が消えて箱の下端が角ばって見える**。既定 Theme (radius 26pt) でも下地と `cellBackgroundColor` に対比があれば明確に視認できる
- **挿入ケース**: 末尾でなくなった Cell が `roundsBottom: true` を保持し、箱の途中に角丸 clip が残る。`sectionBorderWidth > 0` や `CellStyle.backgroundColor` 指定時に見える

これは spec「箱と Cell 背景の合成」の SHALL (「Section 先頭 / 末尾 Cell の背景…は箱の角丸形状で clip され、角の外へはみ出さない」) が、Scenario「構造変更後も箱が Cell 範囲に追従する」の直後に破れる状態であり、deviation.md にも記録がない。Cell を画面外へスクロールアウトさせて戻すと再 dequeue で直るため、断続的に現れる。

Android の `ModernSectionDecoration` は毎描画時に現在のリストから箱端を解決するため同じ欠陥を持たず、iOS 片側の挙動差になっている。

**推奨修正**: clip の解決タイミングをキャッシュ依存から外す。いずれかで足りる。
- (a) `collectionView(_:willDisplay:forItemAt:)` でも `applySectionBoxClip(to:at:)` を呼び、表示のたびに現在の visible projection から解決し直す
- (b) snapshot 適用時に「可視 Cell 数が変化した Section」の先頭 / 末尾 item を `reconfigureItems` に加える
テストは「Section 末尾 Cell を削除した後、新しい末尾 Cell の clip が `roundsBottom == true` になる」「末尾に挿入した後、旧末尾 Cell の clip が `roundsBottom == false` になる」の 2 本を追加すれば検出力を固定できる (上記プローブがそのまま雛形になる)。

### [🟡 Minor] iOS: 角丸 clamp が実描画経路と別実装になっており、clamp のテストが本番経路を守っていない

**該当箇所**:
- `ios/Sources/KsSettingsViewUI/SectionBoxMetrics.swift:74` (`clampedCornerRadius(for:)`)
- `ios/Sources/KsSettingsViewUI/SectionBoxDecorationView.swift` の `apply(_:)` (`layer.cornerRadius = min(attributes.cornerRadius, min(bounds.width, bounds.height) / 2)`)
- `ios/Tests/KsSettingsViewUITests/SectionBoxDecorationTests.swift:186` (`test_角丸半径は箱の短辺の半分へclampされる`)

**問題点**:
`clampedCornerRadius(for:)` は `Sources/` から一度も呼ばれていない (grep で参照はテスト 2 箇所のみ)。実際の clamp は `SectionBoxDecorationView.apply(_:)` にインラインで重複実装されている。結果として clamp を検証するテストは**本番の描画経路を検証していない**: `apply(_:)` 側の clamp を消してもこのテストは通り続ける。Android 側 (`SectionBoxMetrics.clampedCornerRadius`) は本番から呼ばれており、iOS だけが乖離している。

**推奨修正**: `SectionBoxDecorationView.apply(_:)` が `SectionBoxMetrics.clampedCornerRadius(for:)` を使うようにして実装を 1 本化する (attributes 経由で metrics を渡すか、同等の static ヘルパへ委譲する)。一本化できないなら、テストの観測点を decoration view の `layer.cornerRadius` に移す。

### [🟡 Minor] iOS: `sectionCornerRadius` が Cell 高さを超えると Cell 背景が箱の角丸からはみ出す

**該当箇所**: `ios/Sources/KsSettingsViewUI/SectionBoxCellClip.swift` の `maskPath(in:)`

**問題点**:
Cell 側の clip 半径は `min(cornerRadius - borderWidth, min(rect.width / 2, verticalLimit))` で、先頭 / 末尾 Cell の `verticalLimit` は**その Cell 1 行の高さ**。一方、箱側の clamp は**箱全体の短辺の半分**。両者の上限が異なるため、`sectionCornerRadius > 先頭 Cell の高さ` かつ `箱の高さ > 2 × cornerRadius` の条件で Cell の clip 半径だけが小さくなり、Cell の背景が箱の角丸の外側へはみ出す (例: 行高 50pt × 4 行 = 箱 200pt に radius 60 → 箱 60 / Cell 50)。spec「箱と Cell 背景の合成」の「角の外へはみ出さない」に反する。

既定値 (radius 26 / 行高 44 以上) では発生しないため実害は限定的だが、`sectionCornerRadius` は公開 API で任意値を受け付ける。

**推奨修正**: Cell 側の clip 半径の上限を、Cell 単体の高さではなく箱の実寸から解決した clamp 済み半径 (= 箱と同じ値) に揃える。`SectionBoxCellClip.resolve` へ箱の高さ (または clamp 済み半径) を渡す形が素直。

### [🔵 Suggestion] Android: 1 フレームで child 全走査を 2 回行っている

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/ModernSectionDecoration.kt:90` (`onDraw`) と `:116` (`onDrawOver`)

**問題点**:
`onDraw` / `onDrawOver` がそれぞれ `resolveMetrics(parent)` + `collectSectionBoxes(parent, metrics)` を実行し、可視 child 全件の走査と Map / Set の生成をフレームあたり 2 回行う。正しさに問題はないが、可視行数の多い長いリストでスクロール中の割り当てが倍になる。

**推奨修正**: 同一フレーム内で `onDraw` の結果を保持して `onDrawOver` で再利用する (RecyclerView は 1 フレームで `onDraw` → children → `onDrawOver` の順に必ず呼ぶ)。ただし `onDraw` が呼ばれない経路がないことを確認したうえで行うこと。急ぐ性質ではないため任意。

### [🔵 Suggestion] 蒸留時の反映内容 (Android の Header / Footer 水平 inset)

`specs/settings-view-android-ui/spec.md` の「Section Header / Footer 行は…水平方向の inset 対象にもしない (SHALL NOT)」は deviation.md でオーナー判断により覆されており、実装は H/F 行にも `sectionMargin` の水平成分を適用している (iOS と一致)。蒸留で concepts へ反映する際は、spec 文言ではなく deviation 後の契約 (H/F 行にも水平 margin を適用し、箱は H/F を覆わない) を正として書くこと。

## アクションプラン

1. **[Major]** iOS の箱 clip 解決をキャッシュ依存から外し、構造変更後も先頭 / 末尾 Cell の clip が正しくなるようにする。回帰テスト 2 本 (挿入後 / 削除後) を追加する
2. **[Minor]** iOS の角丸 clamp を `SectionBoxMetrics.clampedCornerRadius(for:)` へ一本化し、テストの観測点を本番経路に合わせる
3. **[Minor]** Cell 側 clip 半径の上限を箱の clamp 済み半径に揃える (大きな `sectionCornerRadius` 指定時のはみ出し解消)
4. **[Suggestion]** Android の 1 フレーム 2 回走査の解消 (任意)
5. **[Suggestion]** 蒸留時に Android spec の SHALL NOT ではなく deviation 後の契約を concepts へ書く

1 の修正後は、視覚に影響する変更のため lessons/process.md L-003 に従い、影響範囲のスクリーンショット (Modern で Cell を挿入 / 削除した前後) を `ui/verification/` へ追加するか、既存証跡の有効範囲を brief.md に明記すること。
