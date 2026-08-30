# レビュー結果: fix-ios-root-accessory-theme-refresh (002 回目)

**日付**: 2026-08-24
**判定**: APPROVED

## サマリー

review-001 の 🟡 Minor 2 件 (本 change が無効化した既存コメントの残置) が、いずれもコメントの差し替えだけで解消されていることを確認した。新しい記述は現行実装と一致しており、review-001 の Suggestion 3 件の判定にも影響しない。lint も両ファイルで 0 件、iOS 588 件 / Android 全モジュールとも再実行してグリーン。Critical / Major はなく、残るのは任意対応の Suggestion のみのため APPROVED とする。

## 差分の範囲確認

review-001 時点のスナップショットと突き合わせ、コード側の変更が申告どおり 2 コメントに限られることを確認した。

- `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift` — review-001 時点のファイルとの差分は `:1487-1491` の 1 ハンクのみ (コメント 3 行 → 2 行)。実行される文は 1 つも変わっていない
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt` — 差分は `resyncFromStore` の KDoc (`:344-348`) のみ。他のハンクは 1 行ぶん位置がずれただけで内容は同一
- 他の 3 ファイル (`KsSettingsListAdapter.kt` / `RootHeaderFooterAdapter.kt` / `KsSettingsViewControllerTests.swift`) とテスト 4 本・証跡は無変更

## (a) 新コメントと現行実装の一致

### 指摘 1 (iOS `applyFullSnapshot` 冒頭) — 解消

新: 「旧 visible projection を控える。後段の「header / footer が変化した Section の reload 指定」で新 projection との比較対象に使う (hidden Section は projection から除外済み)」

- 実装一致: `oldVisible` は `:1519` で `oldByID` に畳まれ、`:1521-1528` で新 projection の各 Section と `header` / `footer` を比較して `reloadIDs` を組み立てている。コメントの説明どおり
- 矛盾解消: 「`headerMode` / `footerMode` を再評価するためレイアウトを作り直す必要がある」という記述が消え、5 行下の `:1496`「layout 自体は作り直さない」との矛盾はなくなった。削除済みの `layoutModesDiffer` を想起させる記述も残っていない
- 括弧内の「hidden Section は projection から除外済み」も `computeVisibleSections` の挙動と一致

### 指摘 2 (Android `resyncFromStore` KDoc) — 解消

新: 「…取り込むのは root だけにして、Theme は現在値をそのまま持ち越す。なお `setRootDirect` は渡された Theme が現在値と異なるときに自ら再 bind 通知を発行するため、`bind` のように Theme を引数で渡す経路でも、続く collect の同値スキップとは無関係に表示へ反映される」

- 実装一致 (前半): `attachStoreCollection` は `store.theme` (StateFlow) を collect して `theme` setter へ流す (`:389-397`)。collect を張り直せば現在値が改めて届き、`themeBacking` と異なるときだけ `applyThemeInternal` が走る。「ここで持ち越すだけで足りる」理由として正しい
- 実装一致 (後半): `setRootDirect` は `internalTheme != theme` を見て `notifyThemeChangedToAdapters()` を発行する (`:500` / `:507-515`)。`bind` は `store.theme.value` を引数で渡す経路 (`:302` / `:312`) であり、新記述のとおり同値スキップとは独立に表示へ届く。この振る舞いは `RootAccessoryThemeRefreshTest` の bind 系 3 件が担保している
- 逆戻り誘発の解消: 「引数で Theme を渡すと再 bind 通知が出ない」という、`bind` の実装と矛盾する旧警告は消えた

## (b) Suggestion 3 件への影響

いずれも影響なし。指摘対象のコードは今回まったく触れられていない。

1. `applyTheme` での Root accessory 二重再適用 — `KsSettingsViewController.swift:413-416` / `:1112-1116` は無変更。指摘は有効なまま (任意対応)
2. Theme 通知と内容通知が重なるケースの回帰テスト欠落 — `KsSettingsListAdapter.kt:207-208` / `RootHeaderFooterAdapter.kt:115-116` は無変更。レビュー側の一時プローブで現時点の挙動が正しいことは実測済みで、残るのはテストによる固定のみ (任意対応)
3. 蒸留時に concepts へ「text / View 形式 H/F / Cell」の三分割を明示 — 実装変更を伴わない申し送り。そのまま有効

## (c) lint / テスト

- `python3 scripts/comment-policy-lint.py` — リポジトリ全体で禁止 0 件 (673 ファイル)。修正した 2 ファイルを明示指定した実行でも 0 件。差し替え後の記述に変更提案 ID・議論通番・アーカイブ文書パス・デルタスペック構文キーワードの混入はなく、時間軸の記述 (「旧〜だった」「〜で変更した」) も含まれていない
- `python3 scripts/local-path-lint.py` / `identity-lint.py` — exit 0
- iOS `xcodebuild test -scheme KsSettingsView-Package` — 588 tests / 0 failures
- Android `./gradlew test` — BUILD SUCCESSFUL (13 タスク再実行)

## 指摘事項

### [🔵 Suggestion] 新しい iOS コメントは `oldVisible` の 2 つ目の用途に触れていない

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1487-1488`

**問題点**: `oldVisible` は reload 指定 (`:1519-1528`) だけでなく、同一 ID のまま内容が変わった Cell の洗い出し (`:1536-1540` の `FullSnapshotContentTargets.compute`) でも比較対象として使われる。新コメントは前者だけを名指ししているため、`:1536` の呼び出しを読んだ人が「コメントに書かれていない用途がある」と感じる余地が残る。誤りではなく、両方の用途はそれぞれの呼び出し地点 (`:1516-1518` / `:1533-1535`) で説明されているため実害は小さい。

**推奨修正 (任意)**: 「後段で新 projection と突き合わせる判定 (supplementary の reload 指定と Cell 内容の再適用) の比較対象に使う」のように、用途を列挙するか総称にする。

## アクションプラン

判定を満たすために必要な対応はない。以下はすべて任意。

1. (Suggestion) `applyTheme` の Root accessory 二重再適用を省くか、冪等である旨をコメントに残す (review-001 より継続)
2. (Suggestion) Theme 通知と内容通知が重なるケースの回帰テストを両 Adapter について 1 件ずつ足す (review-001 より継続)
3. (Suggestion) 蒸留時、concepts への追随で「text / View 形式 H/F / Cell」の三分割を明示する (review-001 より継続)
4. (Suggestion) iOS `applyFullSnapshot` 冒頭のコメントで `oldVisible` の用途を列挙する
