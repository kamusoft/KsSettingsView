# Tasks: cleanup-comment-lint-debt

級: S (提案アーティファクト・デルタスペックなし。独立レビューは実施)
決定事項の正: [exploration.md](exploration.md)

## 全タスク共通の規律

コメント規約は `kasane/concepts/cross/conventions/comment-policy.md` が正。作業前に必ず本文を読むこと。

### 書き換えの方針 (exploration.md 論点2 の決定 α)

規約の「書き換え時の判断基準 (3 類型)」に従う。そのうえで、対応 ADR を探す努力量を以下に限定する。

1. **ADR 参照 (`<domain>/ADR-NNNN`) への置換は、次の両方を満たす場合のみ**
   - コメントの説明だけでは設計理由が語れない (理由一体型)
   - `kasane/decisions/<domain>/index.md` の**タイトルレベルで明らかに一致する** ADR がある
   - ADR 本文の読み込み・全件突合はしない。「たぶんこれだろう」で参照を付けない
2. **上記に当てはまらないものは、現在形の自己完結説明に書き直す**
3. **履歴参照** (`設計（履歴）: openspec/.../archive/...` 等) は規約の類型3として削除する。失われる情報が惜しい場合のみ、現在形の仕様説明に翻案する
4. **新規 ADR は起票しない**
5. **判断に迷った箇所は勝手に決めず、報告に箇条書きで上げる** (オーナー確認に回す)
6. **lint が検出しない規約違反も、同じブロックを触るついでの範囲では併せて直す** (オーナー承認済み、2026-08-06)
   - 規約の正は `comment-policy.md` であり lint はその近似にすぎない。片方だけ直すと不自然な状態が残る
   - 対象例: 裸の change-id 参照、履歴記述 (「旧来は〜」「〜に切り替えた」)、裸のドキュメント参照、タスク通番
   - **ファイル全体の総点検まではしない**。あくまで書き換え対象ブロックの周辺に限る
   - コメント内容に事実誤認を見つけた場合は現状のコードに合わせて訂正してよい (機能コードは変えない)。訂正した箇所は報告に明記する

### 実行環境の申し送り

- Android のテストはこの worktree に `android/local.properties` が無い (gitignore 対象) ため、素の `./gradlew test` は SDK location not found で失敗する。`ANDROID_HOME=~/Library/Developer/Xamarin/android-sdk-macosx` を渡して実行すること
- iOS のテストは **`swift test` では検証にならない** (`concepts/cross/conventions/test-execution.md`)。`KsSettingsViewUITests` は macOS 上の `swift test` で**1件も実行されない**。必ず Simulator で実行し、**実行件数を報告に併記する**こと:

  ```
  cd ios && xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17 Pro'
  ```

  件数は出力末尾の `Executed N tests, with M failures` で確認する (2026-08-01 実測で全件 338 件)

### 絶対制約

- **機能コードを1文字も変えない**。差分はコメント行のみ
- `comment-policy:allow` マーカーによる除外で件数を落とさない (誤検知が確実な場合のみ。使ったら報告に理由を書く)
- spec・足場アーティファクトの書き換え禁止、git 操作禁止

### 各グループの完了条件

- `python3 scripts/comment-policy-lint.py <対象パス>` の**禁止件数 0**
- `git diff -- <対象パス>` の差分が**コメント行のみ**であること (自分で確認して報告に明記)
- 判断に迷った箇所のリスト (無ければ「なし」と明記)

---

## タスクグループ

小規模な領域を先に処理し、書き換え方針の実地確認をしてから大規模領域へ進む順序とする。

### G1: android/ks-settingsview-core (67 件 / 22 ファイル)

- [x] `android/ks-settingsview-core` 配下の禁止 67 件を 0 にする
- ドメイン: android
- 対象パス: `android/ks-settingsview-core`

### G2: android/ks-settingsview-compose (32 件 / 15 ファイル)

- [x] `android/ks-settingsview-compose` 配下の禁止 32 件を 0 にする
- ドメイン: android
- 対象パス: `android/ks-settingsview-compose`

### G3: android/ks-settingsview-ui (282 件 / 70 ファイル)

最大の領域。ワーカーのコンテキスト予算のため main / test に分割する。

#### G3a: android/ks-settingsview-ui/src/main (184 件 / 47 ファイル)

- [x] `android/ks-settingsview-ui/src/main` 配下の禁止 184 件を 0 にする
- ドメイン: android
- 対象パス: `android/ks-settingsview-ui/src/main`
- 集中箇所: `CellBaseLayout.kt` (14) / `SectionAccessoryViewHolders.kt` (13) / `EntryCell.kt` (9) / `EffectiveStyle.kt` (9) / `ClassicSectionDecoration.kt` (8)

#### G3b: android/ks-settingsview-ui/src/test (98 件 / 23 ファイル)

- [x] `android/ks-settingsview-ui/src/test` 配下の禁止 98 件を 0 にする
- ドメイン: android
- 対象パス: `android/ks-settingsview-ui/src/test`
- 集中箇所: `BasicCellsTest.kt` (22) / `UnifyCellCommonFieldsTest.kt` (14) / `CellRowWidthAllocationTest.kt` (14)

### G4: ios/Sources (277 件 / 54 ファイル)

ワーカーのコンテキスト予算のためターゲット別に分割する。

#### G4a: ios/Sources/KsSettingsViewCore + KsSettingsViewSwiftUI (88 件 / 21 ファイル)

- [x] `ios/Sources/KsSettingsViewCore` (59 件 / 12 ファイル) と `ios/Sources/KsSettingsViewSwiftUI` (29 件 / 9 ファイル) の禁止を 0 にする
- ドメイン: ios
- 対象パス: `ios/Sources/KsSettingsViewCore`, `ios/Sources/KsSettingsViewSwiftUI`
- 集中箇所: `Section.swift` (9)

#### G4b: ios/Sources/KsSettingsViewUI (189 件 / 33 ファイル)

- [x] `ios/Sources/KsSettingsViewUI` 配下の禁止 189 件を 0 にする
- ドメイン: ios
- 対象パス: `ios/Sources/KsSettingsViewUI`
- `KsSettingsViewController.swift` が単独 54 件。ヘッダの `// 仕様: openspec/...` 定型句と文中の `〜Requirement` 参照が主。次いで `PickerCell.swift` (9) / `EntryCell.swift` (8) / `RadioCell.swift` (7)

### G5: ios/Tests (59 件 / 28 ファイル)

- [x] `ios/Tests` 配下の禁止 59 件を 0 にする
- ドメイン: ios
- 対象パス: `ios/Tests`

### G6: samples/ + リポジトリ直下 (48 件 / 26 ファイル)

- [x] `samples/` と `android/settings.gradle.kts` 等の残りの禁止 48 件を 0 にする
- ドメイン: ios / android 両方 (samples/ios と samples/android)
- 対象パス: `samples`, `android/settings.gradle.kts`
- Kotlin/Swift だけでなく `build.gradle.kts` / `AndroidManifest.xml` / `strings.xml` / drawable XML のコメントも対象

---

## 全体の完了条件 (全グループ完了後にオーケストレーターが確認)

- [x] `python3 scripts/comment-policy-lint.py --summary` の**全体禁止件数 0** (765 → 0、検査対象 401 ファイル)
- [x] 機能コードの差分が 0 (全 diff がコメント行のみ。レビュアーが言語別コメント除去後の正規化比較で 225 ファイル全件一致を機械的に証明)
- [x] iOS ビルド・テストが通る (Simulator 実行で 624 tests / 0 failures)
- [x] Android ビルド・テストが通る (`--rerun-tasks` で 1986 tests / 0 failures / 0 errors)
- [x] 判断に迷った箇所のリストをオーナーに提示 (各サイクルで実施。合意結果は deviation.md へ)
- [ ] ~~独立レビュー (ksn-reviewer) で APPROVED~~ → **未達成**。レビュー 3 周 (上限) を実施し、review-003 の判定は CHANGES_REQUESTED。**オーナー判断により Major 2 件のみ修正して完了**とした (deviation.md「レビューサイクルの打ち切り」参照)
