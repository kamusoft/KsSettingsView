# レビュー結果: ios-picker-selection-parity (002 回目)

**日付**: 2026-08-02
**判定**: APPROVED

## サマリー

review-001 の Major 2 件 (comment-policy 違反・ナビバー appearance の上書き範囲)、Minor 1 件 (brief.md の生カラー値)、Suggestion 1 件 (seam 命名) はいずれも解消されている。ナビバー appearance は「現在有効な appearance を基点に複製し、タイトル文字色だけを差し替える」形へ書き換えられ、scroll edge 状態は透過構成を明示的に再現するようになった。濃色テーマでの上端表示スクリーンショット (`ui/verification/navbar-dark-theme-check.png`) が追加され、ナビバー背景が候補リストの実効セル背景色と揃うことが証跡として残っている。

テストは 502 件全数成功 (`xcodebuild test` / iPhone 17 Pro / iOS 26.5、failures 0 / skipped 0)。修正による退行は認められない。残る指摘は優先度の低い Minor 1 件と Suggestion 2 件のみのため APPROVED とする。

## 前回指摘の解消状況

| review-001 の指摘 | 状態 | 根拠 |
|---|---|---|
| 🟠 新規コメントが comment-policy の禁止参照に該当 (9 箇所) | **解消** | 変更 3 ファイルに対する `openspec/` `kasane/changes` `Phase/Decision/Round` `MUST/SHALL` `Requirement` `tasks N` の grep が全て無ヒット。ソースヘッダは現在形の仕様説明へ、テストの MARK は参照なしの見出しへ置換済み |
| 🟠 ナビバー appearance の上書き範囲がタイトル色を超えている | **解消** | `applyNavigationBarTitleAppearance` が `navigationItem` → バーの順に現在有効な appearance を基点として複製し、タイトル文字色のみ差し替える形に変更。scroll edge は未設定時に `configureWithTransparentBackground()` を基点として iOS 15 以降の既定 (上端で透過) を再現。`:145-146` のコメントも実装と整合した。濃色テーマの上端表示を撮影して背景が list と揃うことを確認済み |
| 🟡 brief.md の追記に生カラー値 | **解消** | 「Sample アプリの MAUI 互換 Theme (明色系。`cellAccentColor` / `cellTitleColor` / `separatorColor` / `selectedColor` を指定したもの)」という参照表現へ置換 |
| 🔵 テスト seam の可視性・命名が不統一 | **解消** | `initialScrollTargetRow` を `private` へ落とし、`_initialScrollTargetRow` を既存の `_effectiveStyle` / `_resolvedAccentColor` と同じ「配線検証用」節へ配置 |
| 🔵 行スタイル適用の軽微な冗長 | **一部解消** | `cell.tintColor = resolvedAccentColor` は削除済み。`selectedBackgroundView` の毎回生成は残存 (下記 Suggestion) |

## 指摘事項

### [🟡 Minor] ナビバー appearance の基点取得タイミングが UIAppearance プロキシ適用より早い可能性

**該当箇所**: `ios/Sources/KsSettingsViewUI/PickerListViewController.swift:119, 143-175`

**問題点**:
`applyNavigationBarTitleAppearance()` は `viewDidLoad` から呼ばれ、`navigationController?.navigationBar.standardAppearance` を基点にする。UIKit の `UIAppearance` プロキシ (`UINavigationBar.appearance().standardAppearance = ...` 等) はビューが window に入った時点で適用されるため、`viewDidLoad` 時点のバーはまだ既定構成のままである可能性が高い。その場合、ここで作った appearance を `navigationItem` 側 (バーより優先度が高い) へ載せることで、利用者アプリがプロキシ経由で施したナビバーのカスタマイズがこの選択面だけ落ちる。

コメント `:145-146` の「利用者アプリがナビゲーションバーへ施したカスタマイズはそのまま残る」は、バーのインスタンスへ直接設定された場合には正しいが、プロキシ経由の場合には成り立たない恐れがある (未検証)。

なお `UINavigationBarAppearance(barAppearance:)` による複製自体は問題ない — SDK ヘッダが "copying all relevant properties from the given appearance object" と明記しており、同一クラス間なら `titleTextAttributes` / `buttonAppearance` を含めて引き継がれる。

**推奨修正**: 次のいずれか (デルタスペックはナビバー背景・ホスト側カスタマイズを規定していないため、いずれも必須ではない)。

- 基点の取得と適用を `viewWillAppear` へ移す (モーダル提示時、この時点ではバーが遷移コンテナ = window 配下にあるためプロキシ適用後の値を読める)。`hasPerformedInitialScroll` と同様のフラグで 1 度だけ適用する
- 現行タイミングを維持するなら、コメントを「バーのインスタンスへ直接設定された構成を引き継ぐ」程度に狭めて、実装が保証する範囲だけを書く

**対応結果 (レビュー後に確認)**: **解消**。後者 (コメントを実装が保証する範囲へ狭める) が採られ、`:145-146` は「呼び出し時点で navigationItem / バーに設定済みの appearance を複製してタイトル文字色だけを差し替えるため、背景・フォントサイズの構成は基点の appearance から引き継がれる。」に書き換えられた。「利用者アプリのカスタマイズはそのまま残る」という未検証の主張が落ち、コメントは実装が実際に保証する範囲 (呼び出し時点で設定済みの appearance を基点にする) だけを述べる形になっている。scroll edge が未設定のときに透過構成を基点とする例外は `:166-167` のインラインコメントが別途説明しており、記述の整合は取れている。挙動変更は無く (コメントのみの差分)、テスト結果への影響はない。`viewWillAppear` への移動を見送った判断も、デルタスペックがナビバー背景・ホスト側カスタマイズを規定していない以上妥当。

### [🔵 Suggestion] mock 照合用スクリーンショット 3 点が修正前ビルドで撮影されている

**該当箇所**: `kasane/changes/ios-picker-selection-parity/ui/verification/` (`single-select-sheet.png` / `multi-select-sheet.png` / `many-items-initial-scroll.png` はいずれも 18:48-18:49、`navbar-dark-theme-check.png` は 19:16)

**問題点**: ナビバー appearance の書き換えは scroll edge 状態の背景 (既定背景 → 透過) を実際に変える修正であり、承認済み mock との照合に使う 3 点は修正前の描画である。`ui/brief.md` は「この appearance の作り方を変えても描画が変わらないことを画素比較で確認済み」と記しているが、その比較結果自体は証跡として残っていない。明色テーマでは差が視認できないため実害は小さいものの、照合証跡と提出コードの対応が切れている。

**推奨修正**: 3 点を現行ビルドで撮り直して差し替える (撮影手順は確立済みで低コスト)。撮り直さない場合は、比較に用いた方法と結果を brief.md に具体的に残す。

### [🔵 Suggestion] `selectedBackgroundView` を行の生成・再利用のたびに新規確保している

**該当箇所**: `ios/Sources/KsSettingsViewUI/PickerListViewController.swift:216-235` (`tableView(_:cellForRowAt:)`)

**問題点**: review-001 から継続。実効ハイライト色を確実に効かせるために専用ビューを差し替える判断自体は妥当で、コメントもその理由を説明できている。ただし再利用セルに対しても毎回 `UIView` を確保しており、既存の `selectedBackgroundView` があれば背景色だけ更新すれば足りる。

**推奨修正**: 実害は小さいので任意。整理するなら `cell.selectedBackgroundView?.backgroundColor = ...`、無ければ新規生成という形にできる。

## 確認した観点 (指摘なし)

- **ビルド / テスト**: `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.5'` → **TEST SUCCEEDED**。xcresult サマリで passed 502 / failed 0 / skipped 0 / expectedFailures 0
- **修正による退行**: ナビバー適用は `standardAppearance` / `compactAppearance` / `scrollEdgeAppearance` の 3 状態すべてを埋めており、compact 未設定時は標準構成と同じ結果を明示的に置いている。`compactScrollEdgeAppearance` は未設定でも scroll edge / compact のいずれかへフォールバックし、どちらもタイトル色適用済みのため取りこぼしはない。バーが無い状態 (テストの直接生成経路) では既定構成へフォールバックし、クラッシュ・nil 参照の経路はない
- **スタイル適用の退行**: `tableView.tintColor` の一元化後もチェックマークの着色は行へ継承され、accent 3 段解決のテスト 3 本が全段を検証している。`cell.tintColor` 削除による色落ちは起きていない
- **Scenario 網羅**: デルタスペック 5 Requirement の全 Scenario に対応するテストが `PickerSelectionScreenTests.swift` に存在 (スタイル継承 6・ナビバー 2・タイトル 2・アクセシビリティ 2・初期スクロール 5) + 配線 seam 1・キャンセル経路 2
- **契約の厳密さ**: 「有効 index の抽出はスクロール先計算のみ、選択集合は正規化しない」が `initialScrollTargetRow:193-201` と確定 callback のテスト (`{1, 5}` 保持) の双方で守られている
- **足場の凍結**: 実装中に書き換えられたのは tasks.md のチェックと ui/brief.md の照合結果追記のみ。proposal / specs は未変更
- **コメント規約**: 変更 3 ファイルの新規コメントに禁止参照・禁止類型なし。各コメントはファイル単独で意味が通る
- **初期スクロールの再入**: `hasPerformedInitialScroll` によりレイアウト再走で再スクロールしない。`scrollToRow` が誘発する再レイアウトでも同フラグが先に立つため無限再帰しない

## アクションプラン

1. (Minor・任意) ナビバー appearance の基点取得を `viewWillAppear` へ移すか、コメントを実装が保証する範囲へ狭める
2. (Suggestion・任意) mock 照合用スクリーンショット 3 点を現行ビルドで撮り直す
3. (Suggestion・任意) `selectedBackgroundView` の毎回生成を色更新のみに整理する

いずれも APPROVED の判定を妨げない。

## オーケストレーターへの申し送り

`ui/brief.md` の照合結果に「**オーナーの最終承認は未取得** (実装ワーカーからの提示待ち)」の記載が残っている。ksn-ui の視覚照合はオーナー承認をもって完了となるため、`ui/verification/` の画像をオーナーへ提示し承認日を brief.md へ記録する必要がある (レビュー判定とは別ゲート)。上記 Suggestion 2 (撮り直し) を採るなら、提示前に済ませるのが自然。
