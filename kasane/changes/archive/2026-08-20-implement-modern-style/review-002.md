# レビュー結果: implement-modern-style (002 回目)

**日付**: 2026-08-20
**判定**: CHANGES_REQUESTED

## サマリー

前回 (review-001) の Major 1 件・Minor 2 件と、相方レビューから採用した Minor 1 件は**いずれも解消**を確認した。箱 clip は Cell インスタンスのキャッシュ依存から外れ、`willDisplay` と構造更新後の再解決の二重化で構造変更に追従する。clip 形状は「Cell 単体の寸法」ではなく「箱の実 geometry を Cell 座標系へ写す」方式へ改められ、角丸 clamp も `SectionBoxMetrics.clampedCornerRadius` 1 か所へ集約された。回帰テストは実際の `cell.layer.mask` を観測しており代理値ではない。A/B 証跡 (`ios-modern-insert-delete-2-after-delete-before-fix.png` / `-after-delete.png`) も実在し、提出コードの挙動 (修正前は淡黄の Cell 背景が角丸と下端ボーダーを塗りつぶし、修正後は箱形状へ収まる) と対応している。

一方で、採用 Minor (可視 Section 0 件時の余白) の修正手段として導入された `refreshSectionUnitPresentation()` が、**Root Header / Footer の accessory を全 Diff で作り直す**副作用を持ち込んでいる。一時プローブで実測したところ、Root accessory と無関係な `replaceCell` 1 件で `KsAnyView.uiKit` の factory が再実行された。これは本 change 以前には無かった挙動で、concepts が明示的に避けるべきとしている類型 (View accessory の factory 再生成による内部状態喪失) に該当するため Major とする。

## 確認した観点

- **ビルド / テスト** (concepts/cross/conventions/test-execution.md に従い件数まで確認):
  - iOS: `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17'` → Bridge 148 / Core 88 / SwiftUI 91 / UI 556 = **883 tests, 0 failures** (`** TEST SUCCEEDED **`)
  - Android: `./gradlew test --rerun-tasks` → BUILD SUCCESSFUL、test-results XML 集計で **2490 tests / 0 failures / 0 errors**
  - サンプル: `samples/ios` `xcodebuild build` → `** BUILD SUCCEEDED **`、`samples/android` `./gradlew assembleDebug` → BUILD SUCCESSFUL
- **未追跡ファイル**: `git status --porcelain` で列挙し、未追跡の新規 5 ファイル (`SectionBoxAttributes.swift` / `SectionBoxCellClip.swift` / `SectionBoxDecorationView.swift` / `SectionBoxLayout.swift` / `SectionBoxMetrics.swift`) と新規テスト `SectionBoxDecorationTests.swift` を含めてレビューした
- **コメント規約**: `python3 scripts/comment-policy-lint.py` → 検査 639 ファイル / 禁止 0 件。新規 iOS ファイルの参照は `ios/ADR-0003` のみで、change-id・レビュー通番の裸参照なし
- **足場**: `specs/` `proposal.md` `design.md` `exploration.md` は未変更。更新は `tasks.md` のチェックと `ui/brief.md` の照合メモ・追補のみ
- **deviation.md**: 記録済み 6 件は合意済み差分として指摘対象から除外した
- **前回 Suggestion 2 件** (Android の 1 フレーム 2 回走査 / 蒸留時の Android spec SHALL NOT の扱い) は今回の対象外として合意済みのため、未対応でも指摘していない
- **Android 側**: `android/` の実装ファイルは review-001 時点から変更なし (最終更新 12:30 対 review-001 13:25) を確認。今回の修正は iOS に閉じている
- **視覚証跡** (lessons/process.md L-003): `ui/verification/` に構造変更の A/B を含む 4 枚が追加され、`ui/brief.md` に撮影条件 (iPhone 17 Simulator / ボーダー 3pt / 中央 Cell に淡黄背景) と各枚の意味が明記されている。実見して提出コードの clip 契約と一致することを確認した

## 前回指摘の解消状況

| 前回指摘 | 判定 | 根拠 |
|---|---|---|
| [Major] 挿入 / 削除後に隣接 Cell の箱 clip が更新されない | **解消** | `KsSettingsViewController.swift:2334` (`willDisplay` で再解決)、`:1103` `refreshVisibleSectionBoxClips()`、`:1383` / `:1568` から `refreshSectionUnitPresentation()` 経由で構造更新後に再解決。回帰テスト 2 本 (`SectionBoxDecorationTests.swift:684` / `:700`) は実際の `cell.layer.mask` のパスを観測しており、代理値ではない |
| [Minor] 角丸 clamp が実描画経路と別実装 | **解消** | `SectionBoxDecorationView.swift:41` が `SectionBoxMetrics.clampedCornerRadius(_:for:)` を使用。テストの観測点も decoration view の `layer.cornerRadius` へ移った (`SectionBoxDecorationTests.swift:193`) |
| [Minor] `sectionCornerRadius` が Cell 高さを超えると Cell 背景がはみ出す | **解消** | `SectionBoxCellClip` が箱の実 geometry (`boxFrame` / `cellFrame`) から `boxTopOffset` / `boxBottomOffset` を持ち、半径を箱の寸法で clamp する方式へ変更 (`SectionBoxCellClip.swift:62-93`)。テスト `SectionBoxDecorationTests.swift:777` が「半径 60 / 行高 48 / 箱 192」で弧の外側の点を検査している |
| [Minor・相方採用] 可視 Section 0 件でも sectionMargin が残る | **解消 (副作用あり → 下記 Major)** | `sectionUnitMargin()` (`KsSettingsViewController.swift:741`) が `visibleSections.isEmpty` で `.zero` を返す。テスト 3 本 (`:417` / `:434` / `:451`) |

既存 Scenario との整合も確認した。clip の box-geometry 方式化は「可視 Cell が0件の Section は箱を生成しない」(`sectionBoxFrame` が nil → `.none`)、「viewport より長い Section」(中間 Cell は offset が半径以上で `.none`) と矛盾しない。余白の 0 件ガードは**可視 Section 数**に対する条件であり、「可視 Cell が0件の Section にも `sectionMargin` は適用される」という Scenario (テスト `:622` が固定) とは別条件のため衝突しない。

## 指摘事項

### [🟠 Major] Root Header / Footer の accessory が全 Diff で factory から作り直される

**該当箇所**:
- `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1115-1124` (`refreshSectionUnitPresentation()` が `rootHeader != nil` / `rootFooter != nil` なら無条件に `refreshRootSupplementary` を呼ぶ)
- 同 `:1383` (`applyDiff` の全 case の後)、`:1568` (`applyFullSnapshot` の apply completion)、`:406` (`applyTheme`)
- 同 `:2197-2251` (`applyAccessoryToListCell` は既存 subview を除去し、`.uiKit` は `factory()` を再実行、`.swiftUI` は `UIHostingConfiguration` を作り直す)

**問題点**:
`refreshRootSupplementary` は HEAD (81bf2c4) では `rootHeader` / `rootFooter` の `didSet` からのみ呼ばれていた。本変更で `refreshSectionUnitPresentation()` の一部となり、**Root accessory と無関係な Diff でも毎回**呼ばれるようになっている。`applyAccessoryToListCell` は内容を作り直す実装なので、`.view` 形式の Root accessory は Diff のたびに factory から再生成される。

一時プローブで実測した (プローブは実行後に削除済み。`ios/Tests/KsSettingsViewUITests/` に一時ファイルを置き、実行後に trash):

```
PROBE baseline factoryCallCount=1
PROBE afterDiff factoryCallCount=2 (delta=1)   // .replaceCell（Root accessory と無関係）1 件の後
```

`replaceCell` は Switch や Entry の値更新で日常的に流れる経路であり、そのたびに Root Header の View が作り直される。影響は:

- **内部状態の喪失**: `KsAnyView.uiKit { MyView() }` のように factory が毎回新しい View を返す実装では、編集中テキスト・スクロール位置・アニメーション状態が失われる。factory がインスタンスを capture している実装でも `removeFromSuperview()` → 再 addSubview されるため first responder は失われる
- **既存の設計方針との矛盾**: `kasane/concepts/core/architecture/display-state-synchronization.md:70` は「View accessory で高さ差を内容差とすると `KsAnyView` の View が factory から作り直されて内部状態を失う」として、この類型を避ける判断を明記している。同 `:64` も同種のコスト (first responder 喪失) を注意点として挙げている
- **無駄な処理**: SwiftUI backing では Diff のたびに `UIHostingConfiguration` を作り直す

Root accessory の再構成が必要なのは、**`rootAccessoryContentInsets(isFooter:)` の解決値が実際に変わったとき** (可視 Section が 0 件 ↔ 非 0 件へ遷移した、または Theme の `sectionMargin` が変わった) だけであり、他の Diff では不要である。

**推奨修正**:
最後に適用した Root accessory の inset (または `sectionUnitMargin()` の結果) を保持し、値が変わったときだけ `refreshRootSupplementary` を呼ぶ。あわせて「Root accessory の View が構造 Diff で作り直されない」ことを固定する回帰テストを 1 本追加する (上記プローブがそのまま雛形になる: `.uiKit` factory の呼び出し回数が `replaceCell` の前後で増えないこと)。

`refreshVisibleSectionBoxClips()` 側は Cell の mask を差し替えるだけで identity を壊さないため、この分離で問題ない。

### [🔵 Suggestion] `SectionBoxMetrics.clampedCornerRadius(for:)` (インスタンス版) の呼び出し元がテストだけになった

**該当箇所**: `ios/Sources/KsSettingsViewUI/SectionBoxMetrics.swift:82-84`

**問題点**: clamp の一本化で本番経路は static 版 (`clampedCornerRadius(_:for:)`) のみを使うようになり、インスタンス版は `SectionBoxDecorationTests.swift:188-189` からしか呼ばれていない。static 版へ委譲するだけの薄いラッパなので実害はないが、Sources 側に本番未使用の API が残っている。

**推奨修正**: 削除してテストを static 版へ寄せるか、残すなら本番側でも使う。急がない。

### [🔵 Suggestion] 可視 Section 0 件の回帰テストが「Section が 1 つも無い」ケースだけを覆っている

**該当箇所**: `ios/Tests/KsSettingsViewUITests/SectionBoxDecorationTests.swift:417` / `:434` / `:451`

**問題点**: 相方レビューの推奨は「空状態と**全 Section 非表示**状態の回帰テスト」だった。現行テストは `SettingsRoot(sections: [])` と `removeSection` の 2 経路で、`Section.isVisible == false` により可視 Section が 0 件になる経路 (`replaceSection` → `applyFullSnapshot`) は覆っていない。ガード自体は `visibleSections` 共通なので実装上の穴ではないが、`applyFullSnapshot` の completion 経由という**別の再計算タイミング**を通る点は未固定。

**推奨修正**: 全 Section を `isVisible = false` にする `replaceSection` (または `.full`) で `contentInset` が 0 になることを見るテストを 1 本足す。任意。

## アクションプラン

1. **[Major]** `refreshSectionUnitPresentation()` から Root accessory の再構成を切り離し、解決済み inset が変化したときだけ `refreshRootSupplementary` を呼ぶ。「構造 Diff で Root accessory の View が作り直されない」回帰テストを追加する
2. **[Suggestion]** `SectionBoxMetrics.clampedCornerRadius(for:)` の扱いを決める (削除 or 本番利用)
3. **[Suggestion]** 全 Section 非表示経路の余白 0 テストを追加する

1 は Root accessory の表示位置を変えない修正のため、視覚証跡の再撮影は不要と判断する (寸法の解決値そのものは変えず、再適用の頻度だけを絞る修正である限りにおいて)。もし解決タイミングの変更で Root accessory と Section の間隔が動く実装になる場合は、lessons/process.md L-003 に従い該当スクリーンショットを撮り直すこと。
