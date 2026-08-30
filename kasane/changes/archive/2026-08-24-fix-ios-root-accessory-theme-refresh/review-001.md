# レビュー結果: fix-ios-root-accessory-theme-refresh (001 回目)

**日付**: 2026-08-24
**判定**: CHANGES_REQUESTED

## サマリー

合意済みスコープ (A)〜(F) はいずれも実装され、iOS 588 件 / Android 全モジュール (`--rerun-tasks` で強制再実行) ともグリーン。ミューテーションプローブで「text 形式の追従」「View 形式の非再構成」の双方向に回帰検出力があることを実測で確認し、視覚証跡 (iOS / Android の A/B) も実在して提出コードの挙動と一致していた。機能上の欠陥は見つかっていない。

一方で、本 change が壊した / 無効化した既存コメントが 2 か所そのまま残っている。どちらも本 change が触れたファイル内で数行に収まり (lessons process L-005)、片方は「この change が直したはずの落とし穴」を今も現役の危険として説明しているため、将来の逆戻りを誘発する。この 2 点の是正を条件に CHANGES_REQUESTED とする。

## 検証したこと

- **ビルド / テスト**: iOS `xcodebuild test -scheme KsSettingsView-Package` → 588 tests / 0 failures。Android `./gradlew test --rerun-tasks` → BUILD SUCCESSFUL (初回の `./gradlew test` は全タスク UP-TO-DATE だったため、キャッシュを外して再実行した)
- **ミューテーションプローブ (lessons code-review L-001)**:
  - `applyTheme` の `refreshRootAccessoryTextAppearance()` / `refreshSectionAccessoryTextAppearance()` をコメントアウト → text 系 11 件のみが失敗、View 系 4 件は通過 (追従アサーションに検出力あり)
  - text 形式限定ガードを外して View 形式にも再適用 → View 系 4 件 + 混在 1 件のみが失敗、text 系は通過 (「View 形式を対象外にする」ガードにも検出力あり)
  - 使用した一時変更は backup との `shasum` 一致で原状復帰を確認済み
- **視覚証跡 (lessons process L-003)**: `evidence/all-accessory-theme-{before,after}-*.png` は Root Header + Section Header 3 件が赤 22pt へ、Section Footer が橙へ追従し、View 形式の Root Footer だけ不変。`evidence/android-view-accessory-theme-after-{unfixed,fixed}.png` は EditText の入力内容が unfixed で消え fixed で残る。いずれも撮影メモの記述と画像が一致し、提出コードの分岐と対応している
- **Android の payload 合流 (新設スキップ経路の安全網)**: 一時プローブテストで、Theme 変更と View 形式 accessory の内容差し替えが同一 `setRootDirect` で届く場合を Root / Section 双方について実測 → いずれも中身が正しく作り直された (プローブは削除済み)
- **lint**: `comment-policy-lint.py` 0 件、`local-path-lint.py` / `identity-lint.py` は追跡外の `deviation.md` / `evidence/*.txt` を明示指定した実行でも exit 0
- **足場**: `exploration.md` の変更は探索フェーズでの論点確定 (未検討 → 決定事項) であり、実装中のスコープ書き換えではない。スコープ拡張 (E)(F) と付随修正は `deviation.md` 側に記録されていて規律どおり
- **削除された死経路**: `supplementaryModes` / `makeListConfig` / `layoutModesDiffer` への参照は、現役のソース・concepts・docs のいずれにも残っていない (アーカイブ文書のみ)。`headerTopPadding = 0` / `backgroundColor = .clear` は `makeLayout` 側 (`KsSettingsViewController.swift:527-530`) に現存し、削除されたテストは死んだ複製を見ていただけなので実質のカバレッジ欠落はない

## 指摘事項

### [🟡 Minor] `resyncFromStore` の KDoc が、本 change で解消した落とし穴を今も現役の危険として説明している

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:344-349`

**問題点**: 「むしろ Theme を `setRootDirect` の引数で先に入れてしまうと `themeBacking` だけが新しくなり、続く collect の同値スキップに阻まれて `applyThemeInternal` が走らない — すなわち各 ViewHolder への再 bind 通知が発行されず、既に bind 済みの Cell が古い Theme の配色のまま残る」という説明は、本 change の付随修正 (`setRootDirect` 内の `if (themeChanged) notifyThemeChangedToAdapters()`、同ファイル `:508-516`) によって成立しなくなっている。実際、同じファイルの `bind`(`:302` / `:312`) はこの KDoc が警告している「`setRootDirect` の引数に `store.theme.value` を入れる」呼び出しそのものであり、それが正しく動くことは新規テスト `RootAccessoryThemeRefreshTest` の bind 系 3 件が担保している。

この矛盾を放置すると、将来の保守者が「`bind` の方が KDoc の禁止事項を踏んでいる」と読んで `bind` 側を巻き戻す、あるいは同値スキップ回避の重複ワークアラウンドを足す、という逆戻りを誘発する。`resyncFromStore` が `internalTheme` を渡す判断自体は妥当なので、変えるべきは理由の記述だけ。

**推奨修正**: 「むしろ〜残る。」の一文を落とし、`theme` が `StateFlow` であり collect 再開時に現在値が改めて流れてくること (= ここで持ち越すだけで足りること) を理由として残す。触れるなら、`setRootDirect` 側が Theme 差分を検出して通知を出すようになった現在形の説明に合わせる。

### [🟡 Minor] 削除した `layoutModesDiffer` の存在理由を語るコメントが `applyFullSnapshot` に残り、直後の記述と矛盾している

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1490-1492`

**問題点**: 「header / footer の有無が visible projection で変化した場合は、`headerMode` / `footerMode` を再評価するためレイアウトを作り直す必要がある」と書かれているが、`makeLayout` は `headerMode` / `footerMode` を `.supplementary` 固定にしており (`:520-529`)、この「再評価」を担っていた `layoutModesDiffer` は本 change で削除された。5 行下の `:1497` は「layout 自体は作り直さない (`setCollectionViewLayout` を呼ばない)」と正反対のことを述べており、同じ関数内で読者が二つの矛盾する説明に当たる。

なお `oldVisible` / `newVisible` の算出自体は後続 (`:1520` 付近の `oldByID`、`:1538` 付近の引数) で現役なので、削除対象はコメントの前半だけ。

この記述は `cleanup-comment-lint-debt` の review-003 で 🟡 Minor として観測され、「実装側の実態 (`layoutModesDiffer` が未使用であること) にまで踏み込まないと最終形が決まらない」として後続 change へ送られた債務にあたる。実態を確定させた本 change が閉じるべき場所で、修正は 2〜3 行に収まる。

**推奨修正**: 前半を、`old` / `new` の visible projection をここで確定させる目的 (Section 単位の差分計算と余白・箱 clip の再評価に使う) の説明へ書き換える。`headerMode` / `footerMode` の再評価には触れない。

### [🔵 Suggestion] `applyTheme` で `sectionMargin` が変わったとき、text 形式 Root accessory が二重に再適用される

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:413-416`

**問題点**: `refreshSectionUnitPresentation()` は `refreshRootAccessoriesIfMarginChanged()` を呼び (`:1112-1116`)、余白の解決値が変わっていれば Root Header / Footer を `refreshRootSupplementary` で作り直す。その直後の `refreshRootAccessoryTextAppearance()` は text 形式について同じ `refreshRootSupplementary` をもう一度呼ぶため、`Theme.sectionMargin` を変える Theme 適用では UILabel の生成・破棄が 2 回走る。結果は冪等で不具合ではないが、無駄な処理であり、読み手には「なぜ 2 回呼ぶのか」が分からない。

**推奨修正**: `refreshRootAccessoriesIfMarginChanged` が実際に再構成した場合は text の再適用を省く、あるいは冪等で重複し得ることを `refreshRootAccessoryTextAppearance` の doc コメントに一文添える (処理を変えない場合)。

### [🔵 Suggestion] Theme 通知と内容通知が同一描画機会に重なる場合を守るテストがない

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsListAdapter.kt:207-208` / `RootHeaderFooterAdapter.kt:115-116`

**問題点**: 新設したスキップ経路は「Theme と内容が同じ描画機会に重なった場合は payload に両方が載るため、この分岐には入らない」という前提の上に立っている。この前提が崩れると、View 形式 accessory の内容差し替えが黙って落ちる (利用者から見えるのは古い中身のまま) という無音の欠陥になる。前提自体は RecyclerView の payload 蓄積挙動に依存する外部仕様であり、テストで固定されていない。

レビュー側の一時プローブで、Root / Section とも「Theme 変更 + View 形式 accessory の内容差し替え」を同一 `setRootDirect` に重ねた場合に中身が正しく作り直されることは実測済み (現時点で欠陥はない)。既存の `SectionAccessoryThemeRefreshTest` の内容差し替えテストは Theme を据え置いており、この合流ケースは通っていない。

**推奨修正**: `SectionAccessoryThemeRefreshTest` / `RootAccessoryThemeRefreshTest` に「Theme 変更と View 形式 accessory の内容差し替えを同時に適用したとき、中身が差し替わる」ケースを 1 件ずつ足す。

### [🔵 Suggestion] `setRootDirect` の Theme 通知は Cell も全件フル bind する — View 形式 Cell の内部状態の扱いを言語化しておきたい

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:508-516`

**問題点**: 本 change で「View 形式 accessory は内部状態を守るため Theme 通知で作り直さない」という規律が Root / Section の H/F に入った一方、`KsAnyView` を抱える Cell (`CustomCell`) は従来どおり `PAYLOAD_THEME` でフル bind される。`applyThemeInternal` 経路では以前からそうだったので回帰ではなく、iOS の `snapshot.reconfigureItems` とも対称だが、`setRootDirect` が通知を出すようになったぶん発火点は増えている。

**推奨修正**: 実装変更は不要。蒸留時に `concepts/core/architecture/display-state-synchronization.md` へ Theme 追従を書き足す際、「text は追従・H/F の View 形式は状態保持のため非追従・Cell は再構成」の三分割を明示しておくと、次に同種の判断をする人が迷わない。

## アクションプラン

1. (Minor) `KsSettingsView.kt:344-349` の `resyncFromStore` KDoc から、解消済みの落とし穴の記述を外す
2. (Minor) `KsSettingsViewController.swift:1490-1492` のコメント前半を、`headerMode` / `footerMode` の再評価に触れない現在形の説明へ書き換える
3. (Suggestion) `applyTheme` の Root accessory 二重再適用を省くか、冪等である旨をコメントに残す
4. (Suggestion) Theme 通知と内容通知が重なるケースの回帰テストを両 Adapter について 1 件ずつ足す
5. (Suggestion) 蒸留時、concepts への追随で「text / View 形式 H/F / Cell」の三分割を明示する
