# レビュー結果 - refine-basic-cells-sample-layout

**レビュー日時**: 2026年06月04日
**レビュワー**: sdd-reviewer
**変更提案ID**: refine-basic-cells-sample-layout

---

## サマリー

### 評価概要

本変更提案は、`KsImage` の sealed 化（iOS / Android 両プラットフォーム破壊変更）、Core への `Section.headerHeight` 追加、iOS の Sticky Footer デグレ修正・viewBackgroundColor セクション間反映・Section H/F 余白制御・罫線インセット規則、Android の Switch Thumb/Track 色分離・Checkbox 右端整列・KsImage アイコン解決、両プラットフォーム Sample の Cell タイプ別構成への再編という、多岐にわたる修正を 1 つの change にまとめたものである。

実装内容を網羅的に検証した結果、以下の通り評価する：

1. **仕様遵守**: 6 つの delta spec（cell-types-basic / settings-view-core / settings-view-ios-ui / settings-view-android-ui / samples-ios / samples-android）すべての Requirement / Scenario が実装に反映されている。
2. **破壊変更対応**: `KsImage` 旧 3 フィールド形式（`name` / `url` / `systemName`）の利用箇所は本リポジトリ内（Sample / テスト含む）で完全に置換されている。grep による `KsImage(systemName:`, `KsImage(name:`, `KsImage(url:` の検索で残存ヒットなし。
3. **iOS / Android 整合性**: Section 順序（CommandCell → LabelCell → SwitchCell → CheckboxCell → RadioCell → SimpleCheckCell → ButtonCell）、各 Cell の title / description / valueText / footer のテキストが両 Sample で一字一句一致している。
4. **テスト**:
   - iOS (swift test): 154 tests passed
   - Android (`:ks-settingsview-core:test :ks-settingsview-ui:test :ks-settingsview-compose:test`): BUILD SUCCESSFUL
   - 仕様で要求された KsImage の派生別解決テスト、SwitchCell Thumb/Track テスト、Checkbox 24dp テスト、headerHeight テスト、footer 空文字列 supplementary 非生成テストはすべて実装済み。
5. **プロジェクトルール遵守**:
   - Android Sample の Theme は `Theme.Material3.DayNight.NoActionBar`（MaterialSwitch の `materialSwitchStyle` 要件を満たす）
   - iOS の罫線描画は `UIListSeparatorConfiguration` の宣言的 API でカスタマイズ（onDrawOver は iOS では該当せず）
   - Android UI 修正は既存の Cell ライフサイクルおよび二層分離ポリシーに反していない
6. **openspec validate**: `Change 'refine-basic-cells-sample-layout' is valid`（strict）

軽微な改善余地はあるものの、Critical / Major の問題はなく、仕様駆動開発のサイクルとして適切に閉じている。実機目視確認（13.x）はコード変更ではなくユーザー手動確認に集約されている点も明示されており、design.md / tasks.md でカバーされている範囲外への逸脱はない。

### 判定

**APPROVED**

---

## 指摘事項

### 🔵 Suggestion-1: iOS の罫線インセット規則テストが自動化されていない

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:411-471` (`separatorConfiguration(for:base:)` / `titleLeadingPosition(for:)`)

**問題点**:
tasks.md の 5.3 / 5.4 は実機目視確認に集約されており、`titleLeadingPosition(for:)` の純粋関数的ロジック（icon 有無による 16pt / 52pt の分岐）も `separatorConfiguration(for:base:)` の Cell 位置判定（isFirst / isLast / 中間）も自動テストが無い。これらは UIKit のレンダリングを経由しない純粋ロジックであり、副作用も少ないため、ユニットテスト化が比較的容易である。

**推奨修正**:
`titleLeadingPosition(for cell: any KsCell) -> CGFloat` を `internal` に昇格させ、`LabelCell(icon: nil)` → 16、`LabelCell(icon: .systemName("..."))` → 52 を返すことを直接検証する 2〜3 ケースのテストを追加すると良い。`separatorConfiguration(for:base:)` も同様に Cell 配列を与えて isFirst / isLast / 中間で `topSeparatorInsets.leading` と `bottomSeparatorInsets.leading` を直接検証可能。

ただし、本 change の Scope 内では実機目視確認で代替する旨が明記されており、判定上の障害にはならない。

---

### 🔵 Suggestion-2: Android Checkbox の右端整列を実機目視で確認する設計

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CheckboxCellViewHolder.kt:112-137`

**問題点**:
tasks.md 8.3 / 8.4 で、4 種アクセサリ（Switch / Checkbox / Radio / SimpleCheck）の右端整列は実機目視に集約されているが、Instrumented Test や `LayoutParams` 値の比較で `marginEnd` を含むレイアウト整合性をある程度自動検証可能だった可能性がある。現在の `CheckboxCellViewHolder で 24dp 明示サイズが適用される` テストは、24dp サイズだけを検証しており、accessoryHolder 内の他アクセサリとの相対位置までは検証していない。

**推奨修正**:
将来的に「4 種アクセサリの右端 X 座標が ±1px 以内で一致する」ことをロボトリック等で計測するテストを追加すると、リグレッション耐性が向上する。本 change の Scope では実機目視で代替されており、判定上の障害にはならない。

---

### 🔵 Suggestion-3: `KsImage.SystemName` が Android で `View.GONE` フォールバックされた際の開発者通知

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/LabelCellViewHolder.kt:297-303`

**問題点**:
design.md Decision 2 で「`SystemName` 受け取り時にエラー throw を行わない」と決定されており、その方針自体は妥当（クロスプラットフォーム DSL での煩雑なハンドリングを避ける）。しかし、`isLoggable` レベル（VERBOSE / DEBUG）程度の `Log.d` を出力すれば、開発時の「アイコンが出ない」事象のデバッグが容易になる可能性がある。spec では「エラーログや throw を行ってはならない (MUST NOT)」と明記されているため、現状の実装は仕様準拠だが、デバッグビルド限定でのログ出力は検討余地あり。

**推奨修正**:
本 change の spec では明確に「エラーログを行ってはならない」とされているため、修正不要。ただし将来 change での議論余地として記録しておく価値はある。

---

### 🟡 Minor-1: iOS の sectionProvider クロージャ内で `root.sections` をスナップショットとして参照する経路に注意

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:317-358`

**問題点**:
`makeLayout(for:)` 内で `let sectionsSnapshot = root.sections` を捕捉し、sectionProvider クロージャから参照しているが、`makeHeaderBoundaryItem` / `shouldShowFooter` の呼び出しでは `self?` 経由で `modelSection` を渡している。一方、`modelSection` の参照は `sectionsSnapshot[sectionIdx]` から取得しているため、スナップショット時点と現在の root が乖離する可能性がある（例: Diff 適用後の最初の sectionProvider 評価）。

なお、`applyFullSnapshot(root:animated:)` で mode 変化時のみ `rebuildLayout` を呼び出すため、sectionsSnapshot は updated root を捕捉した新しいクロージャから始まる。Diff 操作（insertSection / removeSection 等）後の sectionProvider 評価は古い `sectionsSnapshot` を参照することになるが、これは header/footer mode が変わらない限り変更されない設計のため、実際の表示への影響は限定的と判断される。

**推奨修正**:
現状実装で問題は顕在化しないが、コード読解時の混乱を避けるため、コメントで「`sectionsSnapshot` は makeLayout 呼び出し時点のスナップショット。Diff 適用後は applyFullSnapshot が mode 変化を検知し rebuildLayout を呼ぶことで再生成される」旨を追記すると良い。

優先度: 低（修正は次回の change で対応可能）

---

### 🟡 Minor-2: iOS `LabelCellView.applyLabelCellContents` のアイコン nil 時 / 不正名時の挙動

**該当箇所**: `ios/Sources/KsSettingsViewUI/LabelCellView.swift:90-103`

**問題点**:
`if let icon = cell.icon` の中で `content.image = UIImage(systemName: name)` を実行しているが、`UIImage(systemName:)` が `nil` を返した場合、`content.image = nil` が代入される。これは `UIListContentConfiguration` 上では「画像領域を確保しない」挙動になるはずだが、`content.image = nil` を明示しないと前回設定の image が残る可能性がある（再利用時）。

`LabelCellView.prepareForReuse` で `contentConfiguration = nil` をクリアしているため、現実装でも再利用時の残存は防げているが、`else { content.image = nil }` を明示しておくと安全性が増す。

**推奨修正**:
```swift
if let icon = cell.icon {
    switch icon {
    case .systemName(let name):
        content.image = UIImage(systemName: name)  // nil なら自動的に画像非表示
    case .uiImage(let image):
        content.image = image
    }
} else {
    content.image = nil  // 明示クリア（防御的）
}
```

優先度: 低（既存の prepareForReuse で実害は無い）

---

## アクションプラン

優先度順に対応すべき項目：

1. **追加対応不要（APPROVED）** — Critical / Major 指摘はなく、本 change を承認する。
2. **将来 change での検討**:
   - Suggestion-1: 罫線インセット規則の純粋関数のユニットテスト化
   - Suggestion-2: Android 4 種アクセサリの右端整列の Instrumented Test 化
   - Minor-1: sectionProvider のスナップショット意味論のコメント追記
   - Minor-2: `content.image = nil` の明示クリア

これらは次回の change で実機目視確認の項目を自動化に置き換えるタイミングで対応すると良い。

---

## 判定結果

**ステータス**: `APPROVED`

### 判定根拠

- ✅ openspec の仕様（proposal.md / design.md / tasks.md / 6 つの delta spec）と実装が一致している
- ✅ tasks.md のうち、コード変更を伴う項目はすべて完了している（未チェックの 5.3 / 5.4 / 7.4 / 8.3 / 10.4 / 11.5 / 13.1 / 13.2 は実機目視確認に集約されており、design.md / tasks.md でその旨が明記されている）
- ✅ 破壊変更（`KsImage` sealed 化）に伴う旧 API 呼び出し箇所は完全に移行されている
- ✅ iOS / Android Sample のセクション順序およびテキストが一字一句一致している
- ✅ プロジェクトルール（Android Theme.Material3.*、iOS の宣言的 separator 設定、Cell ライフサイクル）に違反していない
- ✅ 必要なテストがすべて実装され、PASS している（iOS 154 / Android BUILD SUCCESSFUL）
- ✅ `openspec validate refine-basic-cells-sample-layout --strict` が valid を返している
- ✅ Critical / Major 指摘は存在しない

### 次のステップ

次のフェーズ（`sdd-validator` による検証）に進める。

実機目視確認（tasks.md 13.1 / 13.2）はユーザー手動確認の項目であり、コード変更フェーズの完了判定とは独立して扱う。
