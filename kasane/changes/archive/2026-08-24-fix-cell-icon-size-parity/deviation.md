# Deviation: fix-cell-icon-size-parity

- ButtonCell の `titleAlignment`: spec は主行の幅配分のみを規定し alignment の見え方に沈黙 → ADR-0026 の新配分で title 領域がコンテンツ幅になるため、CENTER / END を指定しても配る余白が消え見た目に出なくなる。**対象は `valueText` (または残り幅を占める行内 trailing) を持つ ButtonCell に限る** — 実装の分岐は `fillsRow = valueText == null && !views.hasFillingInlineTrailing` であり、icon のみ / hintText のみの ButtonCell では title が主行の全幅を使うため alignment は従来どおり視覚に出る。aux をまったく持たないボタンスタイルも全幅を明示化して中央揃えを維持。オーナー合意により受け入れ。理由: iOS の通常レイアウトは元から title がコンテンツ幅で同じ挙動であり、Android が iOS に揃った形で本 change の parity 目的に合致する (2026-08-23)

- [付随修正] `ButtonCellViewHolder.kt` のボタンスタイル分岐: `applyCellBaseLayout` を通らず旧既定 (`title = 0dp + weight 1`) に暗黙依存していたため、既定配分の入れ替えで中央揃えが壊れた。`applyTitleWidthMode(views, fillsRow = true)` を明示して修正。理由: ADR-0026 が維持すると明記している挙動であり、本 change の変更が直接開けた穴 (process L-005)、(2026-08-23)

- [付随修正] `buildCellBaseViews` の icon 初期値リテラル `24` を `Theme.DEFAULT_CELL_ICON_SIZE_DP_VALUE` 参照へ置換、および旧配分を語る周辺コメント (`ButtonCellViewHolder` KDoc / `EntryCellViewHolder.create`) の是正。理由: 本 change で SoT が `EffectiveStyle` 側へ移り、旧配分の記述が現行契約と矛盾するため (2026-08-23)

- [付随修正] iOS `CellBaseLayout.swift` に `normalizedIconImage(_:)` を追加し、`iconImageView.image` へ渡す画像の `alignmentRectInsets` を 0 にした (insets が 0 の画像は同一インスタンスを返す early return 付き)。tasks 2.1 に明記はないが、SF Symbols は字形ごとに非 0 の `alignmentRectInsets` を持ち (`bell` は top -0.667 / bottom -0.333)、Auto Layout は整列矩形に対して制約を解くため、これがないと Requirement「枠の幅と高さは画像の intrinsic size に関わらず解決済み icon size に等しい」を満たせない (size 44 指定でも frame が 44x43 になる)。理由: spec 充足に必要な実装手段 (2026-08-23)

- [付随修正] iOS `prepareForReuse` と icon 非表示時に `layer.cornerRadius = 0` を追加。理由: 旧 radius がリサイクル先の行に残るのを防ぐ、数行で閉じる不備 (process L-005)、(2026-08-23)

- [付随修正] `.gitignore` に `!kasane/changes/**/verification/*.log` を追加。理由: 8 行目の `*.log` により tasks 2.8 が要求する証跡 `ui/verification/ios-test-constraints.log` がコミット対象外になっていた。tasks が指定したファイル名を保ったまま証跡を追跡対象にするため、change 配下の verification 証跡を除外の例外にした (2026-08-23)

## 蒸留への申し送り (追加分)

- `concepts/cross/conventions/comment-policy.md` の禁止類型の列挙を、lint の検出範囲に合わせて追記する (現状は lint が規約本文より広い): (1) **タスク通番** (`タスク 2.4` / `タスク 5` / `task 3`) — 既存の「Phase / Round / Decision / 論点」の列挙に並べる、(2) **変更アーティファクト配下のパス参照の接頭辞なし相対形と非 `.md` 成果物** (`ui/verification/foo.png` `changes/…` 等) — 現行の列挙は `openspec/` `kasane/` 接頭辞付きと `.md` 文書を想定した書き方になっている

- `concepts/core/cells/basic-cells.md` の `ButtonCell.titleAlignment` 記述と `concepts/core/styling/cell-row-layout.md` の title alignment 記述を、「行内 trailing があるとき title 領域はコンテンツ幅になるため alignment は視覚に出ない」と明確化する (現行文言「残った title 領域内で alignment される」は誤りではないが、利用者が期待を持ちうる)

## 合意済みの見送り (レビュー・verify へ申し送り)

- サンプルの Bluetooth 行のアイコン字形が iOS (`antenna.radiowaves.left.and.right`) と Android (`ic_bluetooth.xml`) で異なる件: オーナー判断により**意図した差分として見送り** (起票もしない)。サンプルは各 OS の標準素材を使う方針。icon 枠の寸法・角丸・幅配分の契約には影響しない (2026-08-23)
- `ui/mock/approved.png` の Wi-Fi valueText (`eoGW-276ccc8-5`) と現行サンプル (`demoAP-0a1b2c-5`) の文言差: モック撮影後の文言変更で両 OS 一致しており、レイアウト契約に影響しないため対処不要 (2026-08-23)
- iOS で Dynamic Type がアプリ起動中に追随しない件 (`ios/Sources/KsSettingsViewUI/Theme.swift` の `defaultCellTitleFont` 等が `static let UIFont.preferredFont(forTextStyle:)` で型初期化時に 1 度だけ解決される): 本 change 以前からの挙動で、修正には Cell 側の trait 変更ハンドリングが必要。オーナー判断により**見送り** (起票もしない)。本 change の icon 枠・角丸・幅配分の契約には影響しない (2026-08-23)

- [オーナー指示による同梱] `scripts/comment_policy_rules.py` に comment-policy の検出パターンを追加 (**最終的にタスク通番 1 型のみ**。下記のとおり履歴記述とパス参照は削除に至った): 本 change のレビューで、lint が「禁止 0 件」と報告した状態で実際には規約違反が 3 件残っていたことが判明した (相方レビューが規約本文から検出)。規約は既に禁止しているのに検査が拾えていない穴であり、機械検査できる規約は文章ルールより lint に置く強制力ルーティングの第一候補にあたる。オーナー指示により本 change に同梱する。**本 change のデルタスペックとは無関係の改善**であり、icon 枠・角丸・幅配分の契約には影響しない (2026-08-23)
- コメント内の変更提案識別子の裸参照が 26 箇所残っている件 (`purify-core-extract-style-to-ui-layer` 等。`comment-policy.md:27` が禁止類型として明記しているが検査パターンが存在しない「第 4 の穴」): オーナー判断により**見送り** (箇所の書き換えも検出パターンの追加も行わない、起票もしない)。今回追加した 3 型の検出で本 change の目的は達しており、26 箇所は個別判断を要するまとまった作業になるため (2026-08-23)
- comment-policy lint の残課題 (review-005 の Suggestion 3 件。**本 change では未対応**、蒸留への申し送り): (1) **`references` への固定は対症療法** — 根本は `_URL_RE` がスキームなしのホストパス (`developer.android.com/...` 形式) を除去しないことで、単数形 `reference/old-concepts/` (実在) が無検出、`github.com/foo/bar/blob/main/roadmap.md` が誤ブロックになる。review-004 の推奨文にあった「実ディレクトリ名は複数形のみ / 検出力は落ちない」は事実ではないため、この前提を蒸留に持ち越さないこと。(2) 新規追加した 2 パターン (`(?:制約|条件|指定|属性)が外れた` / `可能に(?:なった|なり)`) だけ否定先読みを欠き、`可能になった場合に備えて` 等が advisory 発火する (兄弟の `ように…` 版は除外済み)。(3) 回帰ケースを持たない advisory パターンが 3 件残る。いずれも実測 0 件・advisory 止まりまたは回避手段ありで現時点の実害はなく、オーナー方針により本 change では対応しない (2026-08-23)

- [オーナー指示] comment-policy lint の**履歴記述 (advisory) 検出を機構ごと削除**: 当初は履歴記述も検出対象に加えたが、「旧実装では」「〜に移った」のような書き手の作業を語る形と「値が変わったとき」「window から外れた時点で」のような実行時の状態説明は語彙が重なり、素朴なパターンでは 44〜45 件の偽陽性が出た。それを助詞・名詞の要求で絞り込む必要がある時点で機械判定できていないため、オーナー判断により今回の追加分だけでなく**既存の 2 パターン (`旧(?:実装\|…)` / `全面刷新\|…\|だった`) も含めて全削除**した (既存分も条件節を 2 件誤検出していた)。`ADVISORY_PATTERNS` / `_ADVISORY` / `scan_text` の advisory 分岐 / `--advisory` オプション / 集計・表示 / selftest ケースをすべて削除し、`scan_text` の戻り値は種別を畳んで `(行番号, 説明, 該当行)` にした (hook も追随)。この類型はレビューで見る (2026-08-23)

- [オーナー指示] comment-policy lint の**変更アーティファクト配下のパス参照の検出を削除**: change 配下のパス (`ui/verification/foo.png`) とスキームなしのホストパス (`developer.android.com/reference/...`、`github.com/foo/bar/blob/main/roadmap.md`) は文字列として同型で、パターンで区別しようとすると例外を足し続けることになる (実際 `references?` の `?` ひとつで Android 公式ドキュメントが hook exit 2 になっていた)。副作用が拾える違反を上回るとのオーナー判断により、追加したパス参照パターンと文書名の追加分 (`exploration|deviation|agenda|history|roadmap`) を削除し、文書名は既存の `proposal|design|tasks|brief|spec` に戻した。**結果として、この付帯作業の発端だったアーカイブ配下 PNG 参照は再び検出されない** (今回のコメント自体は削除済みのため現状の実害なし)。既存の `(?:openspec|kasane)/` パターンにも同じ性質があるが既存分のため未変更 (2026-08-23)

- 蒸留への申し送り (lint 関連): 上記により lint の検出範囲は規約本文より狭い状態に戻った。`comment-policy.md` への追記はタスク通番のみが対象で、パス参照の接頭辞なし相対形については規約本文の記述を変えない。lint の残課題として記録していた `_URL_RE` がスキームなしホストパスを除去しない件は、パス参照検出の削除により当面の実害が消えたため優先度は下がる (根本課題としては残る)

- [オーナー指示] comment-policy lint の**既存パターン `(?:^|[^\w/])(?:openspec|kasane)/` (アーカイブ文書のパス参照) も削除**: 追加分のパス参照検出と同じ性質で、`github.com/kamusoft/kasane/blob/main/README.md` のような公開リポジトリ URL を誤ブロックする。削除前に全走査でこのパターン単独の検出が **0 件**であること (他パターンで代替できること。`kasane/changes/foo/spec.md` 形式は `(?:proposal|design|tasks|brief|spec)\.md` が拾う) を確認したうえで外し、公開 URL が通ることを selftest ケース「許容: リポジトリ名を含む公開 URL」として固定した (2026-08-23)
