# レビュー結果: fix-replace-section-header-refresh (001 回目)

**日付**: 2026-08-05
**判定**: APPROVED

## サマリー

合意済みスコープ (A案: `applyFullSnapshot` で旧/新 visible projection の header/footer 差分を検出して `reloadSections` に積む) を正しく実装しており、`replaceSection` / `.full` 両経路の text header 取りこぼしが解消している。既存の `updateSectionAccessoryAndReload` が同じ accessory 変化に対して既に `reloadSections` を使っているため、経路間の一貫性も取れている。Simulator 全件 411 件 pass、ミューテーションプローブで追加テストの回帰検出力も確認した。Critical / Major なし。残る指摘は「合意スコープ内で意図的に残した制約」を記録に残してほしいという文書側の要求のみで、実装の修正は求めない。

## 検証した内容

- **ビルド / テスト**: `cd ios && xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.5'` → **Executed 411 tests, with 0 failures** / `** TEST SUCCEEDED **` (verification.md の記録と一致)
- **ミューテーションプローブ** (lessons/code-review L-001): `applyReplaceSection` の `forceReloadSectionIDs` を空集合へ潰したビルドで `SectionAccessoryRenderingTests` を実行 → **14 件中 `test_replaceSectionのviewヘッダ差し替えが表示中のsupplementaryに反映される` の 1 件だけが失敗**。前提アサート (初期表示の view header 描画) と他 13 件は通過。追加テストがトートロジーではなく、`forceReloadSectionIDs` 分岐が load-bearing であることの実測証明。プローブ後は backup から復元し shasum 一致 (`eb8428440c32bc02df32b3afde82c7e3321784e0`) を確認済み
- **コメント規約 lint**: `python3 scripts/comment-policy-lint.py` を触った 2 ファイルへ実行 → 禁止 64 件でベースラインから増減なし。新規追加コメントに禁止参照・禁止記述類型は無し
- **既存 ADR / concepts との整合**: `core/architecture/display-state-synchronization.md`・`core/core-model/structural-changes.md` の契約 (値等価と構造 identity の分離、`replaceSection` = Section 全体置換) に反しない。`SectionAccessory` の「view ケースは中身を等価判定に含めない」契約 (`ios/Sources/KsSettingsViewCore/SectionAccessory.swift`) を変更せず、呼び出し元の意図で補う設計になっている点は妥当
- **足場凍結**: `exploration.md` の更新は探索完了 (方針確定・決定事項の追記) の記録であり、実装中の spec 書き換えを示す証拠は無い。deviation.md 無しと矛盾しない

## 指摘事項

### [🟡 Minor] `.full` 経路の view accessory 取りこぼしが残るが、どのアーティファクトにも記録されていない

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1168-1183`

**問題点**: `accessoryChanged` は `SectionAccessory` の `==` に依存するため、`.view` 同士は常に等価と判定される。`forceReloadSectionIDs` を渡すのは `applyReplaceSection` だけなので、**`.full` (= `SettingsRootStore.replaceAll`) で同一 Section ID のまま `.view` header の中身を差し替えた場合は、修正後も supplementary が古いまま残る**。これは本 change が直した症状とまったく同じクラスの不具合であり、`SettingsRootStore` を直接使う UIKit 利用者に到達可能な公開経路である (SwiftUI DSL 経路は `DSLDiffCalculator` が `updateAccessory` を発行するため影響しない。Bridge 経路は `KsBridgeSection` が text header しか輸送しないため影響しない、と確認した)。

合意スコープ (「view 形式 accessory は等価比較不能のため replaceSection 経由では強制 reload」) の範囲内であり実装の逸脱ではない。ただし `exploration.md` の「決定事項」節にこの限定は書かれておらず、`deviation.md` も無いため、**この既知の制約がどこにも残らないまま蒸留に進むと情報が失われる**。

**推奨修正**: コード修正は不要。蒸留前に、この制約 (「`.view` accessory の中身変化は `replaceSection` 経由でのみ表示へ反映される。`.full` では反映されない」) を記録に残す先を orchestrator / オーナーで確定してほしい。`SectionAccessory` の等価契約に紐づく公開挙動なので、`concepts/core/architecture/display-state-synchronization.md` または `core-model/structural-changes.md` への追記が収まりとして自然に見える。

### [🟡 Minor] view accessory を持つ Section への `replaceSection` は内容不変でも Section 内全 Cell を reload する

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1367-1380`

**問題点**: `containsViewAccessory` は旧/新の header・footer に `.view` が 1 つでも含まれれば true になるため、**header も cells も一切変わっていない `replaceSection` でも対象 Section が無条件に `reloadSections` される**。`reloadSections` は supplementary だけでなく Section 内の全 Cell を作り直すため、当該 Section 内で編集中の Cell (EntryCell 等) があれば first responder と IME 状態が失われ、ちらつきも発生する。

これは exploration.md が B案を却下した理由 (「header 不変でも section 内全 cell が reload されちらつく」) と同じ副作用を、view accessory を持つ Section に限って局所的に再導入したことになる。等価比較が不能である以上この選択自体は妥当で、合意スコープにも沿っているが、**A案採用時の「header 不変ケースに副作用が出ない」という前提が view accessory では成立しない**点は公開挙動の観測可能な変化である。

**推奨修正**: コード修正は不要 (等価判定を強化しない限り回避不能)。上の指摘と合わせて、この副作用を記録に残してほしい。`applyReplaceSection` のコメントは「reload を強制する」までは書いているが、「その結果 Section 内の全 Cell が作り直される」という代償までは書いていないので、コメントに 1 文足すだけでもファイル単独での理解可能性は上がる。

### [🔵 Suggestion] 「header 不変なら reload しない」ことを固定する回帰テストが無い

**該当箇所**: `ios/Tests/KsSettingsViewUITests/SectionAccessoryRenderingTests.swift:376` 以降

**問題点**: 追加された 3 件は「変化したときに反映される」側だけを固定している。A案が B案より優れる根拠である「**header 不変なら reload しない (= 無駄な Cell 再構成・ちらつきが起きない)**」は、どのテストにも固定されていない。将来 `accessoryChanged` の条件を緩めて無条件 reload にしても、全テストが green のまま通過してしまう。

**推奨修正**: `.full` を header 同一の root で適用し、適用前後で `cv.cellForItem(at:)` が返す Cell インスタンスが同一 (`===`) であることを検証するテストを 1 件足す。reload が走れば別インスタンスになるため、トートロジーにならない検出力を持つ。

### [🔵 Suggestion] テストヘルパ `pump` の固定 50ms 待ちは時間依存

**該当箇所**: `ios/Tests/KsSettingsViewUITests/SectionAccessoryRenderingTests.swift:396-402`

**問題点**: `RunLoop.current.run(until: Date().addingTimeInterval(seconds))` で 50ms 固定待ちしており、`dataSource.apply(animatingDifferences: true)` の完了を実時間に賭けている。手元では 2 回の全件実行とも安定して通ったが、負荷の高い CI では marginal になり得る。

**推奨修正**: 固定待ちではなく、条件成立までの短い run loop 反復 + デッドライン (例: 最大 2 秒) のポーリングにすると、速い環境では速く終わり遅い環境でも落ちなくなる。

### [🔵 Suggestion] `applyFullSnapshot` の doc comment に abstract が無い

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1127-1129`

**問題点**: doc comment が `- Parameter forceReloadSectionIDs:` から始まっており、関数自体の要約行が無い。`root` / `animated` の説明も無いため、Quick Help では引数 1 つだけが説明された不揃いな表示になる。

**推奨修正**: 1 行の要約 (「model 全体から visible projection と snapshot を作り直す」相当) を先頭に足す。

## アクションプラン

1. (蒸留前・文書) Minor 2 件の内容 — `.full` 経路では `.view` accessory の中身変化が反映されないこと、`replaceSection` は view accessory を含む Section を内容不変でも reload すること — を記録に残す先を確定する (concepts への追記が第一候補)。実装変更は不要
2. (任意・低優先) Suggestion 1 の「header 不変なら reload しない」回帰テストを追加する
3. (任意・低優先) Suggestion 2〜3 のテストヘルパのポーリング化と doc comment の整形

上記はいずれも本 change のマージを妨げない。

---

## 追記: 指摘対応後の再確認 (2026-08-05)

対応分 3 ファイルの差分のみを確認した。**判定は APPROVED のまま維持する。**

### 対応の確認結果

| 指摘 | 対応 | 判定 |
|---|---|---|
| Minor 1 / 2 (記録) | `exploration.md` に「実装フェーズの追加判断・既知の制限 (2026-08-05、review-001 Minor 対応で記録)」節を追加。`.full` 経路の `.view` 取りこぼし (`updateAccessory` 経由が正) と、view accessory を持つ Section の `replaceSection` が内容不変でも全 Cell を reload することを、判断理由込みで明記 | 解消 |
| Suggestion 1 (回帰テスト) | `test_fullDiffでheader不変ならCellは再構成されない` を追加 | 解消 |
| Suggestion 3 (doc comment) | `applyFullSnapshot` に要約行と `root` / `animated` の Parameters 説明を追加 | 解消 |
| Suggestion 2 (`pump` のポーリング化) | 既存 `ContentUpdateBatchTests.pump` と同型を保つため見送り、変えるなら両方まとめて別 change とする判断 | 妥当。既存テストヘルパとの一貫性を優先する判断に異議なし |

### 再検証

- **Simulator 全件**: `Executed 412 tests, with 0 failures` / `** TEST SUCCEEDED **` (追加 1 件が反映された件数)
- **ミューテーションプローブ (追加テストの検出力測定)**: `applyFullSnapshot` の reload 条件を `if true || ...` に潰して全 Section を無条件 reload させたビルドで `SectionAccessoryRenderingTests` を実行 → **15 件中 `test_fullDiffでheader不変ならCellは再構成されない` の 1 件だけが失敗**。前提アサート (初期 Cell の取得) と他 14 件は通過。新規テストがトートロジーではなく、A案が B案より優れる根拠 (「header 不変なら reload しない」) を実際に固定していることの実測証明。プローブ後は backup から復元し shasum 一致 (`a25870d53d83ac9af95e1abf5c951e20a56630d0`) を確認済み
- **コメント規約 lint**: 触った 2 ファイルで禁止 64 件、ベースラインから増減なし。追加された doc comment・テストコメントに禁止参照および禁止記述類型は無し

### 再確認で新たに気づいた点

#### [🔵 Suggestion] 既知の制限の記録先は `deviation.md` の方が蒸留で拾われやすい

**該当箇所**: `kasane/changes/fix-replace-section-header-refresh/exploration.md` の追加節

**問題点**: 記録の内容自体は過不足なく、`決定事項` 節を書き換えず日付と出典を明示した append になっているため、足場の逆流修正には当たらないと判断する。ただし ksn-distill は実装乖離メモを `deviation.md` から拾う建て付けであり、探索アーティファクト内の追加節は蒸留時に見落とされる余地がある。

**推奨修正**: 今回は orchestrator が蒸留時に concepts 反映を判断すると明言しているため対応不要。今後同種の「実装フェーズで判明した既知の制限」は `deviation.md` に置く方が機構に乗りやすい。

判定は APPROVED を維持する。残るのは上記 Suggestion のみで、いずれも本 change のマージを妨げない。
