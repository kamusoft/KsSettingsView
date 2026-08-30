# レビュー結果: fix-entrycell-ime-composition (002 回目)

**日付**: 2026-08-01
**判定**: APPROVED (再確認により更新。初回判定は CHANGES_REQUESTED)

> 初回判定は CHANGES_REQUESTED。Minor 2 件の修正を受けた再確認で APPROVED に更新した。経緯は末尾の「再確認 (2026-08-01)」を参照。以下の初回指摘は証跡としてそのまま残す。

## サマリー

改訂スコープ (`supportsChangeAnimations = false` + `PAYLOAD_CONTENT` 付き `notifyItemChanged` + 誤り前提コメントの修正) は正しく実装されている。真因の説明は AOSP の `SimpleItemAnimator.canReuseUpdatedViewHolder` / `DefaultItemAnimator.canReuseUpdatedViewHolder(holder, payloads)` の挙動と整合し、2 つの機構が独立に ViewHolder 再利用を保証する「二重担保」も成立している。実機証跡 4 枚も主張どおりの内容で、TwoWay 経路・callback 経路の両方で未確定文字列と変換候補が維持されていることを確認した。Android 全ユニットテストは 520 件 pass (core 74 / ui 370 / compose 76、`--rerun-tasks` で強制再実行して確認)。

修正を求めるのはコメント 2 点のみ。実装ロジック・テスト設計への Critical / Major 指摘はない。1 点目は本 change のスコープ項目「誤り前提コメントの修正」の取りこぼしで、同一ファイル内に旧前提の記述が残っている。2 点目は新たに触れた 2 ファイルの comment-policy 違反で、review-001 で適用したのと同じ基準による。

## 指摘事項

### [🟡 Minor / 優先度: 高] 誤り前提コメントが同一ファイル内に残っている

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsListAdapter.kt:233-234`

**問題点**: `CellListItemDiffCallback` の KDoc に「同一 id の内容更新は `KsSettingsView.applyDiff` の `ReplaceCell` ハンドリングが `notifyItemChanged(position)` による部分更新で反映する」が残っている。本 change は「payload なしの `notifyItemChanged(position)` では ViewHolder が差し替えられる」ことを不具合の根と特定し、スコープ項目として誤り前提コメントの修正を挙げているが、修正されたのは `submitContentUpdate` の KDoc (`:48-64`) だけで、同じ機構を説明するこの箇所は旧記述のまま。読者はこのファイル内で矛盾した 2 つの説明に出会う。

不具合の起点が「コメントに書かれた誤った設計前提」だった経緯を踏まえると、同種の記述を残すのは再発経路をそのまま残すことに等しい。

**推奨修正**: `notifyItemChanged(position, PAYLOAD_CONTENT)` (payload 必須である旨を含む) に更新するか、API 名を書かず「`submitContentUpdate` による部分更新経路で反映する」と参照先を関数名に寄せる。なお同ファイル `:265` の `areContentsTheSame` 内コメント (「ReplaceCell → notifyItemChanged の部分更新経路で反映する」) は payload の有無に踏み込んでいないため、現状のままでも誤りではない。

### [🟡 Minor / 優先度: 中] 新たに触れた 2 ファイルに comment-policy 違反が残っている

**該当箇所**:

- `KsSettingsView.kt`: 32, 48-49, 158, 180, 184-185, 433, 439, 668
- `KsSettingsListAdapter.kt`: 15, 17, 20, 28-29, 144, 156, 230, 236-237

**問題点**: `concepts/cross/conventions/comment-policy.md` の禁止参照・禁止記述類型に該当する記述が両ファイルに残っている。内訳は以下の 3 型。

1. **アーカイブ文書のパス参照**: `仕様: openspec/changes/.../spec.md` (`KsSettingsView.kt:48-49`、`KsSettingsListAdapter.kt:28-29` / `236-237`)
2. **変更提案識別子の裸参照**: `purify-core-extract-style-to-ui-layer` (`KsSettingsView.kt:32` / `158`)、`add-cell-types-basic` (`:180`)、`add-cell-types-input` (`:184`)、`add-visibility-flags-section-and-cell` (`:433` / `:668`)、`refactor-display-state-sync` (`KsSettingsListAdapter.kt:17` / `230`)
3. **デルタスペック構文キーワード・議論通番・裸の Requirement/Scenario 参照**: `仕様 MUST:` (`KsSettingsView.kt:185`)、`〜が MUST。` (`:439`)、`（Decision 2）` (`KsSettingsListAdapter.kt:15`)、`Phase 15.3:` (`:144`)、`"ID 衝突回避" Scenario` (`:20`)、`"未登録 Cell の扱い" Scenario` (`:156`)

同規約は適用契機に「既存コメントに触れる実装をするとき・コードレビューのとき」を挙げており、両ファイルとも本 change の diff に含まれる。直前の change でオーナーが「触れたファイルの部分だけ直す」と裁定しており、本 change の review-001 でも同じ基準で `EntryCellViewHolder.kt` / `InputCellsTest.kt` を掃除済みなので、基準を揃える。新規追加された `ContentUpdatePayloadTest.kt` および今回追加・変更されたコメント (`KsSettingsView.kt:63-69`、`KsSettingsListAdapter.kt:48-64` / `181-188`) には違反がないことを確認済み。

**推奨修正**: comment-policy の 3 類型に従って書き換える。1 は定型句型なので該当行を削除。2 は理由一体型で、対応する ADR がないため「その場で読んで分かる設計説明」に書き直す (例: `# purify-core-extract-style-to-ui-layer` の見出しは節の内容を表す見出し語に置換)。3 は規範性を自然な日本語に置換 (`仕様 MUST: オプトアウト可能` → `オプトアウト可能とする`、`判定できることが MUST` → `判定する必要がある`) し、通番・Scenario 名は削除する。ボリュームは 2 ファイル・約 16 箇所で、いずれも 1〜2 行の機械的な書き換え。

### [🔵 Suggestion] Root Header/Footer の内容更新は payload なしのまま

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/RootHeaderFooterAdapter.kt:48`

**問題点**: accessory 更新時に `notifyItemChanged(0)` を payload なしで発行している。Root Header/Footer は `SettingsAccessory.View` で利用者の任意 View (EditText を含み得る) をホストできるため、EntryCell と同じ「ViewHolder 差し替えで IME 接続が切れる」条件を満たす。現状は `supportsChangeAnimations = false` に守られており不具合は出ないが、本 change が掲げる二重担保のうち片方しか効いていない。

**推奨修正**: 本 change では対応不要 (当該ファイルは diff に含まれず、修正が触れていないファイルへ広がるため)。payload 方針を Adapter 間で揃える小変更として別途起こすのが妥当。

### [🔵 Suggestion] 「ViewHolder が再利用される」という因果を直接押さえるテストがない

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ContentUpdatePayloadTest.kt`

**問題点**: 4 テストは「payload が付いている」「フラグが false」という**手段**を検証しており、本当に守りたい性質「更新通知後も同一 ViewHolder が再利用される」は AOSP の挙動に関する前提として文章でしか担保されていない。将来 RecyclerView の実装が変わった場合、テストは緑のまま前提だけが崩れる。

**推奨修正**: 任意。`DefaultItemAnimator().canReuseUpdatedViewHolder(holder, listOf(PAYLOAD_CONTENT))` が true を返すこと、および `supportsChangeAnimations = false` の Animator が payload なしでも true を返すことを 2 行で確認するテストを足すと、AOSP 側の前提が直接固定される。

### [🔵 Suggestion] payload 定数の置き場所が 2 クラスに分かれている

**該当箇所**: `KsSettingsListAdapter.kt:189` (`PAYLOAD_CONTENT`)、`KsSettingsView.kt:661` (`PAYLOAD_THEME`)

**問題点**: 同一 RecyclerView に対する payload キーが別クラスの companion に分かれて定義されている (`"ks-content"` / `"ks-theme"`)。用途が近く命名規則も揃っているため、将来 3 つ目を足すときに置き場所が揺れる。

**推奨修正**: 任意。本 change では現状のままでよい。整理するなら payload キーを 1 箇所に集約する。

## 確認した観点 (指摘に至らなかったもの)

- **真因の説明の妥当性**: `SimpleItemAnimator.canReuseUpdatedViewHolder(holder)` は `!supportsChangeAnimations || holder.isInvalid()` を返し、`DefaultItemAnimator` の payload 版は `payloads.isEmpty()` でなければ true を返す。したがって (1) フラグ無効化と (2) payload 付与はそれぞれ単独で ViewHolder 再利用を成立させる。コメントの「二重に担保」は正確。
- **`supportsChangeAnimations = false` の副作用範囲**: このフラグは change アニメーションのみを制御し、add / remove / move の各アニメーションには影響しない。適用対象は `KsSettingsView` が内部生成する唯一の RecyclerView (`KsSettingsView.kt:57`) で、ライブラリ内に他の RecyclerView 生成箇所はない。Compose Bridge は `KsSettingsViewLayout` という別名 import で同じ `ui.KsSettingsView` を `AndroidView` に埋め込んでいるため (`KsSettingsViewComposable.kt:13` / `150`)、View / Compose 双方の経路が同じ設定を受ける。全 Cell 型で更新時のクロスフェードがなくなる (即時反映になる) が、これは exploration の改訂決定事項どおりで、ちらつき解消側にも働く。
- **3 引数 `onBindViewHolder` 未実装の前提**: `KsSettingsListAdapter` / `RootHeaderFooterAdapter` / 基底の `ListAdapter` のいずれにも 3 引数版のオーバーライドが存在しないことを確認した。よって payload 付き通知は RecyclerView 既定動作で 2 引数版へ委譲され、内容は完全に反映される。コメントとテストの主張は正しい。
- **`PAYLOAD_THEME` との干渉**: 双方とも payload の**中身は参照されない** (`canReuseUpdatedViewHolder` を true にするためだけの非空マーカー)。Theme 更新経路 (`KsSettingsView.kt:516-537`) は従来どおりフル bind に落ちるため、内容更新経路の変更による干渉はない。むしろ内容更新が Theme 更新と同じ流儀に揃った。
- **テストの実効性**: payload を外せばテスト 1 / 2 が、`supportsChangeAnimations = false` を外せばテスト 4 が失敗する。テスト 3 は将来 3 引数版が部分 bind 実装で追加された場合に内容反映の欠落を検出する。いずれも「消したら赤くなる」テストになっている。
- **`awaitDifferCommit` の待ち合わせ**: `AsyncListDiffer` のバックグラウンド差分計算を idle + ポーリングで待つ実装。タイムアウト時は素通りして後続の assert が失敗する設計なので、無限待ちにも偽陽性にもならない。実測でも 4 テスト合計 0.065 秒で完了しており、実質的な待ちは発生していない。
- **実機証跡の内容**: 4 枚とも「入力 Cell 5 種デモ」の同一画面。`after-1char` は名前欄が未確定 (ハイライト+下線) で候補バーに「赤/ある/あ/ぁ」、`after-2char` は「あか」で未確定継続・候補「赤/垢/アカ/あか/赤い」、`after-callback` はニックネーム (callback) 欄で「かあ」が未確定・候補表示、`before` は「Tanaあ」が確定済みで候補バーなし。TwoWay 経路と callback 経路の双方が押さえられており、主張と整合する。ファイル更新時刻は `before` が最後 (16:16、他は 16:14-16:15) で、修正適用後に旧挙動を再現して A/B 比較した順序と読める。
- **差分ガード (review-001 の対象) の維持**: `EntryCellViewHolder` の 3 ガードは変更されていない。ViewHolder が再利用されるようになった以降は `setInputType` の無条件 `restartInput` が実際に効くため、維持の判断は妥当。review-001 の結論を覆す必要はない。
- **テスト実行**: `./gradlew :ks-settingsview-ui:testDebugUnitTest --rerun-tasks` で強制再実行し ui 370 件 pass (新規 4 件を含む)。core 74 / compose 76 も pass。failures 0 / errors 0 / skipped 0。既存テストへの回帰はない。

## アクションプラン

1. `KsSettingsListAdapter.kt:233-234` の旧前提コメントを payload 付き経路の記述に更新する (Minor 1)
2. `KsSettingsView.kt` / `KsSettingsListAdapter.kt` の comment-policy 違反 (計約 16 箇所) を 3 類型に従って書き換える (Minor 2)
3. Root Header/Footer の payload 付与・因果を押さえるテスト・payload 定数の集約は本 change では見送ってよい (Suggestion 3 件)

---

## 再確認 (2026-08-01)

**判定**: APPROVED

Minor 2 件の解消と、「コメント以外のコード行が変わっていないこと」を確認した。Suggestion 3 件は指示により見送りで、いずれも実害がないため判定に影響しない。

### 実行コードの不変性 — 確認済み

`HEAD` との差分から**コメント (行コメント・ブロックコメント・KDoc) を機械的に除去**し、空白正規化したうえで比較した結果、本 change 全体の実行コード差分は以下 5 行のみだった。今回のコメント修正によって増えた実行コード行はゼロ。

- `KsSettingsView.kt`: `import androidx.recyclerview.widget.SimpleItemAnimator` / `(itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false`
- `KsSettingsListAdapter.kt`: `notifyItemChanged(position)` → `notifyItemChanged(position, PAYLOAD_CONTENT)` / `const val PAYLOAD_CONTENT: String = "ks-content"`

`EntryCellViewHolder.kt` と `InputCellsTest.kt` の実行コードは review-001 再確認時点から変化していない (差分ガード 3 箇所とテスト 5 件のまま)。

### Minor 1 (誤り前提コメントの残存) — 解消

`CellListItemDiffCallback` の KDoc は「同一 id の内容更新は [KsSettingsListAdapter.submitContentUpdate] による部分更新経路で反映する」に書き換えられた。推奨した 2 案のうち関数名参照案の採用で、API 名の重複記述がなくなり、payload の要否は `submitContentUpdate` の KDoc に一本化された。KDoc リンク形式のため grep でも到達でき、同一ファイル内の記述の矛盾は解消している。

同ファイル `:265` (`areContentsTheSame` 内) と `KsSettingsView.kt:427` にも `notifyItemChanged` に言及する記述が残るが、いずれも payload の有無に踏み込まず経路名 (`submitContentUpdate`) を併記しているため、初回指摘のとおり誤りではない。

### Minor 2 (comment-policy 違反 16 箇所) — 解消

指摘した全 16 箇所が規約の 3 類型に沿って処理されていることを、diff の目視と検出パターンの grep で確認した。

- **定型句型 (削除)**: `仕様: openspec/...` 3 ブロック、`Requirement` / `Scenario` の裸参照
- **理由一体型 (自己完結する説明へ)**: `# purify-core-extract-style-to-ui-layer` → `# Theme の扱い`、`add-cell-types-basic「基本 Cell の登録 API」Requirement` → 「基本 Cell 7 種を自動登録する。」、`（add-visibility-flags-section-and-cell）` の削除など。ADR 化されていない判断ばかりなので `<domain>/ADR-NNNN` へ置換しなかった判断も規約どおり (該当 ADR がなければ自己完結説明へ、が規約の指示)
- **構文キーワード・通番 (自然な日本語へ)**: `仕様 MUST: オプトアウト可能` → 「自動登録はオプトアウト可能とし」、`判定できることが MUST` → 「判定する必要がある」、`（Decision 2）` 削除、`Phase 15.3:` 削除

副次的に `本提案で〜整理した` という履歴記述型の記述も現在形 (「Theme は `SettingsRoot` には含まれず〜」) に直っており、書き換え後の文はいずれも単独で意味が通る。ダングリングした文末や見出しの欠落もない。

検出パターン (`openspec/` / `Phase [0-9]` / `Decision [0-9]` / `MUST` / `SHOULD` / `spec.md` / `Requirement` / `Scenario` / 既知の変更提案識別子ほか) による残存スキャンは、本 change で触れた 5 ファイルすべてで 0 件。

| ファイル | 残存違反 |
|---|---|
| `KsSettingsView.kt` | 0 |
| `KsSettingsListAdapter.kt` | 0 |
| `EntryCellViewHolder.kt` | 0 |
| `InputCellsTest.kt` | 0 |
| `ContentUpdatePayloadTest.kt` | 0 |

### テスト実行 (再確認時)

`./gradlew :ks-settingsview-{ui,core,compose}:testDebugUnitTest --rerun-tasks` で強制再実行:

- ks-settingsview-core: tests=74 failures=0 errors=0 skipped=0
- ks-settingsview-ui: tests=370 failures=0 errors=0 skipped=0
- ks-settingsview-compose: tests=76 failures=0 errors=0 skipped=0

全 520 件 pass。コメントのみの変更のため件数・内訳は初回確認時と同一。

### 残る任意事項

Suggestion 3 件 (Root Header/Footer への payload 付与 / `canReuseUpdatedViewHolder` の因果を直接押さえるテスト / payload 定数の集約) は未対応。1 件目は本 change で触れていない `RootHeaderFooterAdapter.kt` に及ぶため別 change が妥当。残る 2 件も本 change の完了を妨げない。実機証跡は review-002 初回で内容を確認済みで、追加の確認は不要。
