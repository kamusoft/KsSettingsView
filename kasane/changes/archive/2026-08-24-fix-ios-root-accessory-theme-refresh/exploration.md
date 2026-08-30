# Exploration: fix-ios-root-accessory-theme-refresh

2026-08-21 の公開前トリアージで `cleanup-ios-supplementary-modes-dead-path` を吸収した。どちらも `KsSettingsViewController.swift` の header / footer supplementary 経路で、同じレビュー文脈で閉じる。

2026-08-24 の探索で全論点の方針を確定した (下記「決定事項」)。

## 課題 / 動機

### (1) `applyTheme(_:)` が Root accessory のテキスト色・フォントを更新しない

iOS の `applyTheme(_:)` (`KsSettingsViewController.swift:405`) が Root Header / Footer accessory のテキスト色・フォントを更新しない。Root accessory の再構成は「Section 単位余白 (`sectionMargin` 解決値) が変わったとき」だけ通る経路 (`refreshRootAccessoriesIfMarginChanged`、implement-modern-style で追加) と、`rootHeader` / `rootFooter` の didSet だけで、**余白を変えない Theme 変更 (例: `headerTextColor` だけの変更) では表示中の Root accessory に反映されない**。

これは HEAD 81bf2c4 時点からの既存挙動であり implement-modern-style の回帰ではない (同 change の review-003 で実測確認済み)。ただし Cell / Section Header 側は Theme 変更で色が追従するため、Root accessory だけ非対称。

出典: implement-modern-style の review-003 (Suggestion、既存挙動として判定外)。

### (2) 実行時経路から外れたレイアウト設定系コード (旧 cleanup-ios-supplementary-modes-dead-path)

同じファイルに、実行時経路から外れた supplementary 関連コードが残っている (add-accessory-visibility-toggle の review-001 Suggestion で観測):

- `supplementaryModes` / `makeListConfig` は実行時経路から外れている — `makeLayout` は headerMode / footerMode を `.supplementary` 固定にし、実表示の間引きは sectionProvider 側 (`shouldShowHeader` / `shouldShowFooter`) で行われる。`makeListConfig` はテストからのみ呼ばれる (2026-08-24 実測: 呼び出しはテスト4件のみ、`supplementaryModes` はテスト8件 + 死経路2つのみ、`layoutModesDiffer` は呼び出し元ゼロ)
- これらに対するテストは利用者可視の回帰検出力を持たない (実表示を観測するテストは別途存在するためカバレッジ欠落はない)

### (3) Android 側の拾い物 (2026-08-24 の対称性調査で発見、本 change に同梱)

- Android は「text 形式のみ Theme 追従、View 形式は非追従」を既に実現している (`RootHeaderFooterAdapter` + `applyThemeInternal` の `PAYLOAD_THEME` 通知) が、**これを守る回帰テストが存在しない**
- `setRootDirect` 経路 (`KsSettingsView.kt:490-500` 付近) は adapter へ theme 代入のみで notify を発行しない (実害未検証)

### 公開前に扱う理由

(1) の方針決定は、利用者向け Skills (package-distribution phase-12) が Theme 追従の挙動を書く前に要る。(2) は公開後でも構わないが、同じファイル・同じ経路を触るので一緒に閉じる。

## 検討した選択肢 (却下案と理由を含む)

### (1) → (a) を採用

- **(a) text 形式の Root accessory のみ Theme 変更時に既存インスタンスへ属性再適用 (factory 再生成なし)** — 採用。追従に必要な部品 (`refreshRootSupplementary`) は既存で、`applyTheme` からの配線が欠けているだけ。text 形式には守るべき内部状態がなく、display-state-synchronization の「View accessory を factory から作り直さない」制約に抵触しない。View 形式はテキストを描かないため追従対象が存在せず、「text 形式だけ追従」で利用者可視の非対称は全部解消する。Android の既存挙動 (text のみ追従) とも揃う
- (b) 現状を仕様として concepts に明文化し Skills へ注記 — 却下。「Root だけ追従しない」非対称を利用者ドキュメントで背負い続けることになり、修正コストが小さい (a) に対して利点がない

実装上の要点: `refreshRootSupplementary` は無条件に `applyAccessoryToListCell` へ流すため、View 形式のときに呼ぶと factory 再実行になる。「text 形式のときだけ呼ぶ」ガードが必要。

### (2) → (a) を採用

- **(a) 死経路の削除 (テストごと)** — 採用。実表示の間引きは sectionProvider 側で完結しており二重管理になっている。`shouldShowHeader` / `shouldShowFooter` 自体は実経路が使うため残す (削除対象は mode への写像層のみ)
- (b) 実経路への接続 — 却下。`makeLayout` 自身のコメントが「listConfig を可視性で変えると layout の作り直しが必要になり、`setCollectionViewLayout` の同期差し替えが glitch を誘発する」と接続しない理由を明記しており、固定 `.supplementary` 設計を壊す方向になる

### (3) → 両方同梱を採用

- Android の Theme 追従回帰テスト追加: 採用 (隣接課題は同じ change で直す)
- `setRootDirect` の notify 欠落: 実装時にまず検証し、実害があれば修正、実害なしなら観察結果の記録のみ (条件付き同梱)

## 決定事項

- 公開前トリアージ (2026-08-21): **初回リリース前に対応**。`cleanup-ios-supplementary-modes-dead-path` を吸収、旧ディレクトリは破棄
- 2026-08-24 探索:
  - (1) は **(a) text 形式のみ Theme 追従** で確定
  - (2) は **(a) 削除 (テストごと)** で確定
  - (3) Android の回帰テスト追加と `setRootDirect` 検証を本 change に同梱 (検証の結果実害なしなら修正なし)
  - Android 対称性は ksn-scout 調査で確認済み: Android は text 形式のみ追従で、iOS の (a) 修正は Android と同じ形に揃う (自信度: 高、読み取り調査のみ)

## ADR 候補

- なし。(1) は (a) 採用のため ADR 不要 (局所修正・公開 API 変更なし)。(2) も削除のため不要
- 蒸留時の concepts 追随: `display-state-synchronization.md` の Theme 行「表示済み Header / Footer の即時再評価は保証しない」に、text 形式 Root accessory の追従を反映する一行追記が要る見込み

## 未決の論点

- なし (全論点確定済み)

## UI 素材

なし (色変更前後の視覚確認のみ)

## 変更級の推奨: S (理由)

- (1) は text 形式への属性再適用に限る局所修正で公開 API 変更なし。設計の曖昧さは解消済み
- (2) は内部整理のみで利用者可視挙動に変化なし
- (3) はテスト追加 + 条件付き小修正で、級を上げる要素ではない
- 両 platform に触るが、いずれも既存経路の配線・削除・テストで可逆性が高い
- 再判定トリガー: `setRootDirect` の検証で構造的な修正が必要と判明した場合

## 関連ファイル

- `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift` (`applyTheme` / `refreshRootAccessoriesIfMarginChanged` / `refreshRootSupplementary` / `applyAccessoryToListCell` / `supplementaryModes` / `makeListConfig` / `layoutModesDiffer` / `makeLayout`)
- `ios/Tests/KsSettingsViewUITests/KsSettingsViewControllerTests.swift` (死経路のテスト削除対象)
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/RootHeaderFooterAdapter.kt`
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt` (`applyThemeInternal` / `setRootDirect`)
- `kasane/concepts/core/architecture/display-state-synchronization.md` (既存の制約記述、蒸留時に追随)
- 出典: `kasane/changes/archive/2026-08-20-implement-modern-style/review-003.md` (Suggestion)、`kasane/changes/archive/2026-08-19-add-accessory-visibility-toggle/review-001.md` (Suggestion)
