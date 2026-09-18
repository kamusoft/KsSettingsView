# レビュー結果: ios-cell-highlight-delayed-on-touchdown (004 回目)

**日付**: 2026-09-18
**判定**: APPROVED

## サマリー

このレビューは、オーナー承認のもと **前回 (003) の Major 1 件の修正だけ**を対象に絞っている (上限サイクル超過のため)。結論として、Major は実測で閉じている — `isDisappearing = false` は `tapHandler` の呼び出しより前へ移り、追加された回帰テストは順序を戻すミューテーションで**そのテストだけが落ちる**ことを確認した (他 7 件は通過)。テスト一式は 1062 tests / 0 failures で、`summary.md` の申告と一致する。

残るのは記録側の不整合 1 件のみ (`evidence.md` の自動テスト節。件数が 1061 のまま・「6 点」と書きつつ箇条書きが 7 点・前回の推奨修正 3 の追記が未反映)。挙動には関係せず、コードの再検証を要しない**文面の訂正**であるため、CHANGES_REQUESTED とはせず APPROVED としたうえで蒸留前の必須訂正としてアクションプランに置く。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 追加コメント (didSelectItemAt の合図初期化の理由・新規テストの意図) を本文の許容参照 / 禁止類型と照合。lint は禁止 0 件
- `kasane/handbook/cross/test-execution.md` (テストを実行するとき・結果を報告するとき) — Simulator 全件実行、バンドル集計行の合算で件数確認、負の検証と収束待ちの書き分けを照合
- `kasane/handbook/ios/swift6-language-mode-check.md` (`ios/Sources/` を触る変更の完了判定) — 差分が 1 文の移動とコメント追記のみのため再ビルドはせず、下記「確認した観点」に判断根拠を記す
- `kasane/handbook/cross/runtime-behavior-verification.md` (実行時挙動が絡む不具合の完了判定) — 今回の対象経路 (UIKit 直接ホストの同期 push) は実機証跡ではなく自動テストで担保する形になっており、その旨の記録が要る (下記 B-1)
- `kasane/lessons/code-review.md` (重点観点 L-001) — 「修正が指摘を閉じたか」の判定にミューテーション実測を用い、原状復帰を shasum 一致で確認した

---

## 指摘事項

## A. 見た目・挙動が変わる指摘

**なし。**

## B. 見た目・挙動が変わらない指摘

### [🟡 Minor] `evidence.md` の自動テスト節が実測と合っていない

**該当箇所**: `evidence.md` (自動テストの節)

**問題点**:

今回の追記で担保点が 1 つ増えたが、周辺の記述が追随していない。3 点ある。

| 記述 | 実測・他アーティファクトとの関係 |
|---|---|
| `全件を Simulator 実行で確認した (1061 tests / 0 failures)` | 今回の実測は **1062 tests / 0 failures**。`summary.md` の「1062 tests / 0 failures」とも食い違う |
| `テストは次の 6 点を担保する` | 直後の箇条書きは **7 点**ある (同じ文書内で閉じた不整合) |
| push 遷移の確認行 (`CommandCell から次の画面へ push 遷移する`) | 前回の推奨修正 3 (確認した host が SwiftUI であることを書き添える) が未反映。UIKit 直接ホストは自動テストのみで担保しており、実機証跡の射程を読み手が取り違える |

`evidence.md` は変更と共にアーカイブされる証跡であり、後から実測値を取り直せない。テスト結果の報告に実行件数を併記する規約 (`cross/test-execution.md`) の趣旨からも、値が古いまま残るのは避けたい。

**推奨修正**:

1. 件数を `1062 tests / 0 failures` に直す
2. 「6 点」を「7 点」に直す (または箇条書きを 6 点へまとめ直す)
3. push 遷移の確認行に、確認した host が SwiftUI であることを書き添える。あわせて UIKit 直接ホストは自動テストで担保している旨が自動テスト節にある形にする (現在の追記でその役割は果たせている)

---

## 所見 (今回は指摘にしないもの)

範囲外の新規指摘は Critical に限る取り決めのため、以下は材料として残す。

- **Swift 5 モードのビルドに main actor 分離の警告が残る**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:2363` と `:2459` で `main actor-isolated static property 'sectionTextGap' can not be referenced from a nonisolated context; this is an error in the Swift 6 language mode` が出る。両行は HEAD と一字一句同じ既存行 (本変更で 64 行ぶん位置がずれただけ) で、本変更が持ち込んだものではない。Swift 6 言語モードの一時ビルドは前回 error 0 で確認済みであり (モードによって分離推論が異なる)、矛盾ではないが、恒久切替時に見る対象として残る
- **新規テストの `@Sendable` クロージャが `var` を捕捉している**: `ios/Tests/KsSettingsViewUITests/CellTouchFeedbackTimingTests.swift:261-266` は `var controllerRef` を `@Sendable` クロージャから参照する形で、controller と Cell の循環を解いている。現行の toolchain / 言語モードではビルドが通っている。Swift 6 言語モードへ恒久切替する際に露出する可能性があるが、`ios/handbook` の Swift 6 確認はテストターゲットを明示的に範囲外としており、今回の判定材料にはしない
- 前回 (003) の「所見」4 件 (エッジスワイプの完了待ちの登録漏れ・`viewWillAppear` の coordinator 無し経路の Task 非対称・`resetSelectedColor` がフェードを打ち切らない・長命層への反映) は今回も状態が変わっていない。次にこの領域へ触るときの材料として引き続き有効

---

## 確認した観点 (問題なしと判断したもの)

### (a) 前回 Major の閉じ確認

- **修正の形**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:2543-2554` で `isDisappearing = false` が `tapHandler` の呼び出しより**前**にある。付随して cell 取得と handler 取得が 1 つの `if let` に統合されており、handler 不在時に猶予 Task を張る挙動 (従来どおり) も変わっていない
- **ミューテーション実測** (L-001):

| 条件 | `test_tapHandlerの中で同期的に遷移が始まっても選択が残る` | 他 7 件 |
|---|---|---|
| 現状 (`isDisappearing = false` が handler の前) | **passed** | passed |
| 順序を戻す (handler の後へ移動) | **failed** | passed (7 件とも) |

  追加テストは当該順序だけを検出しており、トートロジーではない。ミューテーションに使った一時変更は backup からの書き戻しで原状復帰し、**shasum 一致** (`38276fdc…`) と `git status` が着手時と完全一致することを確認した。git はすべて読み取り操作のみ
- **テストの書き方**: 解除されないことの確認に `waitForNegativeVerification` を使っており、呼び出し名から負の検証と判別できる (`cross/test-execution.md` の「例外は負の検証だけ」に適合)。秒数そのものは固定していない
- **コメント**: `:2545-2547` の追記は「UIKit から直接使う場合、handler の中で同期的に push されて viewWillDisappear が届くため」と自己完結しており、review 番号・作業文書への参照を含まない。テスト側 `:259-260` も同様。lint は禁止 0 件 (要確認は `KsSettingsViewController.swift:1947` の 1 件のみで、HEAD に既存の差分外の行)

### (b) 順序変更による退行の有無

- **SwiftUI ホスト経路**: `NavigationStack` は path の変更を次の更新で反映するため `viewWillDisappear` は handler の戻り後に届く。合図の初期化がその前に移っても、届いた `true` を上書きする経路は生まれない。既存の `test_タップで遷移が始まった場合は選択が残る` (didSelect の後に `viewWillDisappear`) と `test_遷移が始まらないタップでは選択が解除される` が両方通っており、非同期順序の両分岐は担保されている
- **pageSheet の Picker 系**: 提示元に `viewWillDisappear` が届かない前提は変わらないため、猶予後に解除される挙動 (オーナー確定の A-2) も変わらない
- **戻り際の経路**: `viewWillAppear` 側は今回の差分の対象外で、`pendingDeselectTask` の取り合いも前回確認から変化していない
- **テスト一式**: `xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` のバンドル集計行の合算で **170 + 88 + 98 + 7 + 699 = 1062 tests / 0 failures** (`** TEST SUCCEEDED **`)。前回 (1061) から UITests が +1 で、増分は今回の追加テストと一致する。ビルドは差分なしの増分ビルドで、ミューテーション時の再コンパイルでも新規警告は出ていない
- **Swift 6 言語モード**: 差分は既存メソッド内の 1 文の位置移動とコメント追記のみで、型・分離・シグネチャのいずれも変えていない。前回の一時設定ビルド (error 0) の結論を覆す変更ではないと判断し、再実行はしていない

### (c) `summary.md` との一致

- 「遷移の合図 (`isDisappearing`) はタップハンドラ呼び出しの前にリセットし、ハンドラ内で同期的に push が始まる UIKit 直接ホストでも合図が消えない」— コードと一致
- 「`CellTouchFeedbackTimingTests.swift` (新規、8 件)」— 実行結果も 8 件で一致
- 「iOS テスト一式: 1062 tests / 0 failures」— 実測と一致 (`evidence.md` 側だけが 1061 のまま。上記 B-1)
- ADR `kasane/decisions/ios/0005-cell-press-feedback-library-controlled-timing.md` は `proposed` のため、これを根拠にした指摘は出していない

## アクションプラン

1. B-1 の 3 点 (件数 1062 / 担保点の数 / push 遷移の確認 host) を `evidence.md` で直す。**蒸留前の必須訂正**だが、挙動に影響しない記録の訂正であり再レビューは要しない
2. 「所見」の各件は対応不要。次にこの領域へ触るときの材料として残す
