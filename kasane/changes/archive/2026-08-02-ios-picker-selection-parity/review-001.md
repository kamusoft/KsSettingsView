# レビュー結果: ios-picker-selection-parity (001 回目)

**日付**: 2026-08-02
**判定**: CHANGES_REQUESTED

## サマリー

デルタスペックの5 Requirement (スタイル継承 / ナビバー適用 / タイトル解決 / アクセシビリティ状態 / 初期スクロール) は全 Scenario が実装され、対応する単体テストも網羅されている。`presentPickerModal` を `makeListViewController` に切り出して配線の検証 seam を通した設計、`initialScrollTargetRow` を純関数的に切り出して端部クランプに依存しないテストを可能にした点、範囲外 index を正規化せず callback へ保持する契約の遵守は良好で、テストは 359 件全数成功 (`xcodebuild test` / iPhone 17 Pro)。

一方で、(1) 新規コメント 9 箇所が `concepts/cross/conventions/comment-policy.md` の禁止参照 (変更提案パス・Requirement 裸参照・tasks 通番) に該当し、(2) ナビゲーションバーの appearance 適用がタイトル色以外 (背景・scroll edge・ホストアプリの appearance カスタマイズ) まで上書きしており、コード内コメントの主張「背景はシステム既定を維持」と実装が一致しない。いずれも実装側で解消可能なため CHANGES_REQUESTED とする。

## 指摘事項

### [🟠 Major] 新規コメントが comment-policy の禁止参照に該当する

**該当箇所**:
- `ios/Sources/KsSettingsViewUI/PickerListViewController.swift:10-13`
- `ios/Tests/KsSettingsViewUITests/PickerSelectionScreenTests.swift:7, 81, 157, 181, 206, 219, 248`

**問題点**:
`kasane/concepts/cross/conventions/comment-policy.md` は、コメント中の「変更提案 (`kasane/changes/`) のパス参照」「拡張子なしのデルタスペック Requirement 裸参照」「変更提案内の通番」を明示的に禁止している (適用範囲にテストコードを含む)。該当箇所は以下の3類型すべてに当たる。

- ソース側ヘッダ: `// 仕様: kasane/changes/ios-picker-selection-parity/specs/settings-view-ios-host/spec.md` + `"PickerCell 選択面のスタイル継承" ... Requirements。` — アーカイブ後に参照先が死ぬパス参照 + Requirement 裸参照
- テスト側ヘッダ (`:7`): 同じパス参照
- テスト側 MARK コメント (`:81, 181, 206, 219, 248`): `（Requirement: ○○）` の裸参照
- テスト側 MARK コメント (`:157`): `（tasks 3.6）` — 変更提案内の通番参照

なお同ファイル `:7-9` の `openspec/...` 参照は本 change 以前からの既存行であり、本指摘の対象は新規追加行に限る (既存行の整理は任意)。

**推奨修正**:
policy の「書き換え時の判断基準」1 (定型句型) に従い、参照句を削除して残る説明が自己完結するよう整形する。テストの MARK は `// MARK: - スタイル継承` `// MARK: - 配線の検証 seam` のように参照を落とすだけでよい。ソースヘッダは「選択面は呼び出し元 Cell / Theme の実効スタイルを継承し、選択中の項目を中央付近に表示した状態で開く」のような現在形の仕様説明へ置き換える。

### [🟠 Major] ナビゲーションバー appearance の上書き範囲がタイトル色を超えている

**該当箇所**: `ios/Sources/KsSettingsViewUI/PickerListViewController.swift:147-157`

**問題点**:
`applyNavigationBarTitleAppearance` は `UINavigationBarAppearance()` を**新規生成**し `configureWithDefaultBackground()` を掛けたうえで、`standardAppearance` / `compactAppearance` / `scrollEdgeAppearance` の3つすべてに代入している。これは「タイトル文字色だけを差し替える」実装ではなく、appearance 全体の置き換えである。結果として2つの副作用がある。

1. **scroll edge 状態の背景が変わる**: iOS 15 以降、`scrollEdgeAppearance` が未設定のときのナビバーはスクロール上端で透過背景となり、下地 (ここでは `effective.cellBackgroundColor` を適用した tableView) の色が透ける。`configureWithDefaultBackground()` を代入すると上端でもシステムのマテリアル背景が敷かれるため、テーマ色を敷いたリストとナビバーの色が揃わなくなる。本 change の目的 (選択面のテーマ追従) に対して逆向きの副作用であり、`ui/brief.md` の「未確認の状態」に書かれた懸念 (濃色テーマで list 背景とナビバー背景の明度が揃わない) は、実装がこの代入をしていることに起因する。検証スクリーンショットの Theme が明色 (`cellBackgroundColor` が白系) のため視覚照合では顕在化していない。
2. **ホストアプリのナビバーカスタマイズを無効化する**: 本ライブラリは利用者アプリに組み込まれる。利用者が `UINavigationBar.appearance()` 等でブランド配色を設定していても、`navigationItem` 側の appearance が優先されるため、この選択面だけがその設定を落とす。

さらに `:148` のコメント「背景・フォントサイズはシステム既定を維持するため、appearance は既定構成から派生させる」は、少なくとも scroll edge については実装の挙動と一致していない (コメントが読み手を誤導する)。

デルタスペックはナビバーの背景を規定していないため、「利用者から見える挙動が spec の沈黙領域で変わっている」状態であり、意図的な変更なら合意と記録が必要になる。

**推奨修正**: 次のいずれか。

- (a) 既存 appearance から派生させ、タイトル色のみを差し替える。例: `navigationController?.navigationBar.standardAppearance.copy()` を基点にし、`scrollEdgeAppearance` は元が nil のときは透過構成 (`configureWithTransparentBackground()`) を基点にする — 背景の見え方を現状のシステム既定に一致させたうえでタイトル色だけを載せる
- (b) (a) が困難なら、濃色テーマ (`cellBackgroundColor` を白以外にした Theme) で上端表示のスクリーンショットを撮って差の有無を確認し、現行実装を維持する判断ならオーナー合意を取って `deviation.md` へ記録する

いずれの場合も `:148` のコメントを実装と一致する内容へ直すこと。

### [🟡 Minor] brief.md の追記に生カラー値が含まれる

**該当箇所**: `kasane/changes/ios-picker-selection-parity/ui/brief.md`「照合結果」節 (撮影環境の記述)

**問題点**: ksn-core の ui/ 規約は brief.md に「px 値・生カラー値・具体レイアウト」を書かないと定めている (それらは mock が持つ)。追記された `accent #FFBF00 / タイトル #555555 / separator #E6DAB9 / selected #50FFBF00` は生カラー値そのもの。

**推奨修正**: 「Sample アプリの MAUI 互換 Theme (明色系)」のように、どのテーマ設定で撮ったかが分かる参照表現へ置き換える。

### [🔵 Suggestion] テスト seam の可視性・命名が不統一

**該当箇所**: `ios/Sources/KsSettingsViewUI/PickerListViewController.swift:167`

**問題点**: 同ファイルの他のテスト向け公開 (`_currentSingle` / `_simulateDone` / `_effectiveStyle` / `_resolvedAccentColor`) は `_` プレフィックスで「テスト用フック」と識別できるようにしているが、`initialScrollTargetRow` だけ通常の `internal` プロパティとして露出しており、production から呼ばれる本来のロジックなのかテスト seam なのかが読みで区別できない。

**推奨修正**: `private` に落として `internal var _initialScrollTargetRow: Int? { initialScrollTargetRow }` を「テスト用フック」節へ置くか、既存の seam と同じ節へ移して意図をコメントで明示する。

### [🔵 Suggestion] 行スタイル適用の軽微な冗長

**該当箇所**: `ios/Sources/KsSettingsViewUI/PickerListViewController.swift:196, 199-201` (`tableView(_:cellForRowAt:)`)

**問題点**: (1) `cell.tintColor = resolvedAccentColor` は `viewDidLoad` で `tableView.tintColor` を設定済みのため、tint の継承により実質冗長。(2) `selectedBackgroundView` 用の `UIView` を行の生成/再利用のたびに新規確保している (再利用セルでも毎回差し替わる)。

**推奨修正**: 実害は小さいので任意。整理するなら (1) は削除、(2) は既存の `selectedBackgroundView` があれば背景色だけ更新する形にできる。

## 確認した観点 (指摘なし)

- **ビルド / テスト**: `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` → **TEST SUCCEEDED** (359 tests / 0 failures)
- **Scenario 網羅**: デルタスペック5 Requirement の全 Scenario に対応するテストが存在 (スタイル継承6・ナビバー2・タイトル2・アクセシビリティ2・初期スクロール5)。tasks.md のチェックに虚偽なし (3.6 の配線 seam・3.7 のキャンセル経路・3.8 の既存テスト退行確認もいずれも実体がある)
- **契約の厳密さ**: 「有効 index の抽出はスクロール先計算のみ、選択集合は正規化しない」が `initialScrollTargetRow` の実装とテスト (`確定 callback に {1, 5} が保持される`) の両方で守られている。accent の3段解決は VC 側に集約され、ナビバーは `resolvedAccentColor` を共有していて Theme を再参照していない (spec の要求どおり)
- **足場の凍結**: 実装中に書き換えられたのは tasks.md のチェックと ui/brief.md の照合結果追記のみで、いずれも規約上の想定運用。proposal / specs は未変更
- **エッジケース**: 空 items・範囲外 index・未選択・複数選択の上限到達・キャンセル経路を確認。初期スクロールは `hasPerformedInitialScroll` により再入 (単一選択時の `reloadData()` 起点の再レイアウト含む) が抑止されている
- **アクセシビリティ**: `applyCheckState` にチェック表示と `.selected` trait を集約したため、セル再利用時の状態残留が構造的に起きない

## アクションプラン

1. (Major) comment-policy 違反 9 箇所を定型句型の書き換えで解消する
2. (Major) ナビバー appearance を既存 appearance からの派生に変更する。現行維持を選ぶ場合は濃色テーマでの確認 + オーナー合意 + deviation.md 記録、いずれの場合も `:148` のコメントを実装と一致させる
3. (Minor) brief.md の生カラー値を参照表現へ置き換える
4. (Suggestion) テスト seam の命名統一・行スタイル適用の冗長整理 (任意)

## オーケストレーターへの申し送り

`ui/brief.md` の照合結果に「**オーナーの最終承認は未取得** (実装ワーカーからの提示待ち)」と記録されている。ksn-ui の視覚照合はオーナー承認をもって完了となるため、上記修正後に verification/ の3点をオーナーへ提示し、承認日を brief.md へ記録する必要がある (レビュー判定とは別ゲート)。
