# レビュー結果: fix-cell-icon-size-parity (002 回目)

**日付**: 2026-08-23
**判定**: APPROVED

## サマリー

review-001 と second-opinion-code-001 の指摘 7 件はすべて解消されている。特に回帰検出力の穴 (`invalidateOutline()` 除去がテストで検出できない) は、`IconFrameOutlineProvider.radiusPx` の `val` 化とインスタンス差し替え方式によって**実際に閉じたことをミューテーション 3 本で実測**した (設定・変更・解除の 3 経路すべてでテストが落ちる)。両 platform のテストもレビュー側で再実行し、Android 2582 件 / iOS 581 件が全件パス、iOS の制約衝突 0 件を再現した。

新たな退行は見つからなかった。iOS の `showIcon(size:)` / `hideIcon()` への分割は制約の activate/deactivate・`isHidden`・`prepareForReuse` の順序と対応関係を保っており、`deviation.md` の ButtonCell 記述も実装の分岐と正確に一致する。

指摘は Minor 1 件・Suggestion 1 件。Minor は今回のコメント修正で**新しく持ち込まれた**事実誤り (テスト KDoc の主張が実装の分岐と矛盾する) で、実測で確定した。いずれもコード契約には影響しない。

---

## 前回指摘の解消状況

| # | 出典 | 指摘 | 状態 | 確認方法 |
|---|---|---|---|---|
| 1 | ホスト Minor | `tasks.md` のチェック全未消化 | ✅ 解消 | 全 23 項目 `[x]`。虚偽チェックがないことを verify-001 の対応表で全件突き合わせ |
| 2 | ホスト Minor | `brief.md:39` の証跡ファイル名が実体と不一致 | ✅ 解消 | `android-overflow-long-value.png` に修正済み。brief が引用する 9 証跡すべての実在を確認 |
| 3 | ホスト Suggestion | `invalidateOutline()` 除去をテストが検出できない | ✅ 解消 | ミューテーション 3 本で実測 (下記「検出力の実測」) |
| 4 | ホスト Suggestion | `setIconVisible(_:size:)` の既定値 0 が誤用を許す | ✅ 解消 | `showIcon(size:)` / `hideIcon()` の 2 入口へ分割。`size` 省略はコンパイル不能になった |
| 5 | 相方 Minor | `deviation.md` の ButtonCell 対象範囲が実装より広い | ✅ 解消 | 実装の分岐と一致することを実測で確認 (下記「deviation の記述と実装の一致」) |
| 6 | 相方 Minor | KDoc にアーカイブ配下の PNG 参照が残る | ✅ 解消 | 該当の一文を削除。あわせて変更提案内通番 2 件・履歴記述 1 件も是正済み。comment-policy lint 全走査で禁止 0 件、untracked 3 ファイルも `scan_text` 直接適用で 0 件 |
| 7 | 相方 Suggestion | 制約ログに原出力・提出ツリーの識別子がない | ✅ 解消 | 検証対象ソース 6 件の SHA-256 を再計算し**全件一致**。原出力の SHA-256・検索コマンド・空振り確認 (`Test Suite` 160 件) も併記されている |

### 検出力の実測 (code-review L-001)

`applyIconFrame` に一時的なミューテーションを入れ、`CellIconFrameTest` が落ちることを確認した。使用後は backup との `shasum` 一致 (`d830687bfa3f51e8231e8ae284b9fc45c4d0f52c3eaab8935ae04e4d932c5165`) で原状復帰を確認済み。

| 経路 | ミューテーション | 結果 |
|---|---|---|
| 変更 | `if (current == null \|\| current.radiusPx != radiusPx)` → `if (current == null)` (= 前回の欠陥と同型: radius が変わっても provider を差し替えない) | **落ちた** — `CellIconFrameTest.kt:352` 「新しい radius で clip し直す」/ 11 tests, 1 failed |
| 解除 | else 節の `iconView.clipToOutline = false` を削除 | **落ちた** — `CellIconFrameTest.kt:360` 「角丸なしで再 bind すると clip が解除される」/ 11 tests, 1 failed |
| 設定 | `IconFrameOutlineProvider(radiusPx)` → `IconFrameOutlineProvider(0f)` | **落ちた** — `:273` `:291` `:327` `:348` の 4 件 |

前回は「provider を同一インスタンスのまま書き換える」設計だったため、テストヘルパが `view.outlineProvider.getOutline(...)` で provider を直接叩く限り `invalidateOutline()` の有無を観測できなかった。`radiusPx` が `val` になり radius 変更が**必ず**インスタンス差し替えを伴う形になったことで、provider を直接叩く観測がそのまま検出になっている。実機側も `View.setOutlineProvider` が内部で outline を再構築するため、明示的な `invalidateOutline()` を落としたことによる実挙動の欠落はない (コメント `CellBaseLayout.kt:551-552` の説明どおり)。

### deviation の記述と実装の一致

`deviation.md` の新しい記述「対象は `valueText` (または残り幅を占める行内 trailing) を持つ ButtonCell に限る」は、実装の分岐 `fillsRow = valueText == null && !views.hasFillingInlineTrailing` (`CellBaseLayout.kt:466-469`) と一致する。icon のみ / hintText のみの ButtonCell で alignment が視覚に出ることを、一時テストで実測して確認した (使用後 `trash` で削除済み):

```
PROBE icon-only: titleWidth=248 natural=91 contentRow=248
PROBE hint-only: titleWidth=288 natural=91 contentRow=288
```

いずれも title 領域がテキスト自然幅を大きく上回り、gravity が配る余白がある = CENTER / END が視覚に出る。deviation の限定は正しい。

---

## 指摘事項

### [🟡 Minor] テスト KDoc の「唯一の構成」が実装の分岐と矛盾する (今回の修正で新しく入った記述)

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/CellRowWidthAllocationTest.kt:674-675`

**問題点**: 今回のコメント整理で追加された次の一文が、実装の分岐と矛盾している。

> 行内 trailing が無い行では title が主行の全幅を取る（core/ADR-0026）ため、ここが `titleAlignment` の余白を生む唯一の構成になる。

実装の分岐は `fillsRow = valueText == null && !views.hasFillingInlineTrailing` であり、**aux なしのボタンスタイルは「唯一」ではない**。`hasAux` が真でも `valueText` が無い ButtonCell (icon のみ / hintText のみ) は通常レイアウトを通り、`applyCellBaseLayout` が `fillsRow = true` を選ぶため title は主行の全幅を取り、`titleAlignment` の余白が生まれる。上記の実測 (icon のみ: title 248px に対しテキスト自然幅 91px、hintText のみ: 288px 対 91px) がそれを示す。

この記述は、相方が `deviation.md` について指摘した「対象範囲が実装より広い」のと同じ誤りが、修正の過程でソースコメント側に入ってしまったもの。`deviation.md` は正しく狭められた一方、この KDoc は逆方向 (aux なしだけが特別) に振れている。comment-policy の「そのファイルだけを読んでいる人にとって意味が通ること」は満たすが、**内容が現行契約と一致していない**ため、後からこのテストを読む人が「icon 付き ButtonCell では alignment が効かない」と誤解する。

**推奨修正**: 「唯一」を落とし、この構成が代表例であることに書き換える。例:

> 行内 trailing が無い行では title が主行の全幅を取る（core/ADR-0026）ため、`titleAlignment` が配る余白が生まれる。ここではその全幅構成のうち、本体行が Cell 全体へ広がるボタンスタイルを測る。

### [🔵 Suggestion] 視覚証跡の対象範囲が brief に書かれていない (process L-003 (3))

**該当箇所**: `kasane/changes/fix-cell-icon-size-parity/ui/brief.md`「視覚照合の結果」

**問題点**: `ui/verification/` の PNG 8 点は review-001 前の実装で撮影されており、その後に Android の outline provider 方式と iOS の icon 表示 API の分割が入っている。`ios-test-constraints.log` は修正反映後に再実行され対象ソースの SHA-256 まで併記された一方、PNG 側には撮影時点と提出コードの対応が書かれていない。

**レビュー側の判定**: **再撮影は不要**。今回の修正はいずれも描画結果を変えない:

- Android: 解決済み radius が同じなら outline の `setRoundRect(0, 0, width, height, radiusPx)` の結果は同一。`View.setOutlineProvider` は代入時に outline を再構築するため、明示的な `invalidateOutline()` の有無で clip 形状は変わらない
- iOS: `showIcon(size:)` / `hideIcon()` は `setIconVisible(_:size:)` と同じ 3 操作 (constant 更新・制約 activate/deactivate・`isHidden`) を同じ組み合わせで行っており、呼び出し 3 箇所の実引数も同じ。再実行した 581 件のテストが実寸・制約状態・`prepareForReuse` 後の復帰まで観測して通っている
- 残る変更はテストコードとコメントのみ

**推奨修正**: brief の「視覚照合の結果」に、証跡が対応する実装範囲を 1 行足す (「PNG 証跡は 2026-08-23 の実装で撮影。以後の修正は outline provider の持ち方と icon 表示 API の入口分割のみで描画結果を変えないため再撮影していない」等)。アーカイブ後に第三者が証跡と提出コードの対応を追えるようにするための記述で、コードの修正は不要。

---

## 確認した観点 (指摘なし)

- **ビルド・テスト (レビュー側で再実行)**: Android `./gradlew test --rerun-tasks` → BUILD SUCCESSFUL (4m32s)、JUnit XML 集計で tests 2582 / failures 0 / errors 0 / skipped 0。iOS `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17'` → `** TEST SUCCEEDED **`、`Executed 581 tests, with 0 failures`、原出力に `Unable to simultaneously satisfy constraints` 0 件
- **iOS の表示 API 分割に順序の変化がないか**: `showIcon(size:)` は constant → activate → `isHidden = false`、`hideIcon()` は `isHidden = true` → deactivate。どちらも `UIStackView` が非表示 arranged subview に張る required の寸法 0 制約と required のサイズ制約が同時に立つ瞬間があるが、2 文の間にレイアウトパスが挟まらない同期実行なので制約解決は起きない。実際 581 件の実行で衝突 0 件。`prepareForReuse` (`KsListCellBase.swift:352-360`) は `image = nil` → `cornerRadius = 0` → `hideIcon()` の順で、リサイクル先に前回の radius も枠も残らない。`iconImageView` は宣言時点で `isHidden = true`、サイズ制約は `installBaseLayout` で**非有効のまま**生成されるため、init 直後の状態と `hideIcon()` 後の状態が一致している
- **表示制御の経路の一本化**: `ios/Sources/` 側で `iconImageView.isHidden` を直接触る箇所は `showIcon` / `hideIcon` の中だけ。`updateIconSize` / `setIconVisible` の残存参照は 0 件。制約は `internal private(set)` で、外部からの `isActive` 書き換えができない
- **Android の outline 経路の網羅**: radius > 0 で provider 未設定 (`as?` が null) / 同一 radius (差し替えなし) / 異なる radius (差し替え) / radius 0 への解除 (`ViewOutlineProvider.BACKGROUND` へ戻し + `clipToOutline = false`) の 4 経路を読み合わせ、いずれも前回の状態が残らないことを確認。`IconFrameOutlineProvider` は View ごと・radius 変更時のみの生成で、bind ごとの生成にはなっていない
- **プロジェクト固有規約 (process L-002)**: `python3 scripts/comment-policy-lint.py --summary` → 「合計: 0 ファイル / 禁止 0 件 (検査対象 670 ファイル)」。untracked の新規テスト 3 ファイルは `scripts/comment_policy_rules.py` の `scan_text` を直接適用して 0 件。アーカイブ配下 PNG 参照・変更提案内通番はすべて消えており、残るのは `core/ADR-0025` / `core/ADR-0026` / `android/ADR-0002` の許容形式のみ
- **修正後コメントの内容妥当性**: `CellRowWidthAllocationTest.kt` の 3 箇所 (クラス KDoc・`// MARK:` 2 件) と `ButtonCellViewHolder.kt` / `EntryCellViewHolder.kt` / `CellBaseLayout.kt` の書き換えを実装と突き合わせ、上記 Minor 1 件を除いて現行契約と一致。「本体行の入れ子化により〜移った」の履歴記述は現在形の構造説明 (「ConstraintSet の対象は、title を内包する行コンテナの `contentRow` である」) へ置き換わっている
- **足場の凍結**: `specs/` `proposal.md` `exploration.md` に作業ツリーの変更なし (逆流なし)。今回変更された足場は `tasks.md` (チェック消化) と `ui/brief.md` (証跡ファイル名の是正) のみで、いずれもレビュー指摘の採用に対応する
- **deviation の同梱条件**: 記録済み付随修正 5 件は前回から増減なし。`.gitignore` の 1 行は `git check-ignore -v` で否定パターンが効いていることを確認し、`git status -uall` で `ios-test-constraints.log` が untracked (= コミット対象) になることも確認した
- **証跡ログの同定可能性**: `ios-test-constraints.log` が併記する 6 件の SHA-256 を現在の作業ツリーに対して再計算し全件一致。相方 Suggestion の対応が形だけでなく実際に機能している
- **合意済み事項**: `deviation.md` の ButtonCell の titleAlignment・付随修正 5 件・見送り 3 件はオーナー合意済みとして指摘対象から除外した

---

## アクションプラン

1. (Minor) `CellRowWidthAllocationTest.kt:674-675` の「唯一の構成」を、全幅構成の代表例である旨に書き換える
2. (Suggestion) `ui/brief.md`「視覚照合の結果」に PNG 証跡の対象範囲を 1 行追記する

1 は蒸留・アーカイブ前の処理を推奨する (アーカイブ後はソースコメントだけが残り、誤解が固定される)。2 は任意。いずれもコード契約・テストの変更を伴わないため、判定は APPROVED のまま。
