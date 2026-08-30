# レビュー結果 - add-cell-types-basic (§21 / Decision 9)

**レビュー日時**: 2026年06月02日
**レビュワー**: sdd-reviewer
**変更提案ID**: add-cell-types-basic
**スコープ**: §21（Decision 9）Android 改修のみ（git 作業ツリーの未コミット変更のうち `android/ks-settingsview-ui/` 配下）

---

## サマリー

§21（Decision 9）の Android 改修は、設計（design.md Decision 9-1〜9-6）と tasks.md の各タスクを高い忠実度で実装しており、品質は良好。特にちらつき修正（9-1）の payload 部分 bind 機構は設計意図どおりに動作し、外部 `submitList` 更新の反映・二重発火の防止・フルリバインド回避がすべて整合的に成立している。`KsSimpleCheckView` はオリジナル `SimpleCheck.cs` の `OnDraw` を忠実に移植している。

- ビルド: 成功（compile / `lintDebug` PASS）
- ユニットテスト: `:ks-settingsview-ui:testDebugUnitTest` **全 PASS**（BasicCellsTest 32 件含む全スイート failures=0 / skipped=0、`--rerun-tasks` で再現確認）
- `openspec validate add-cell-types-basic --strict`: PASS
- compose モジュールの flaky テスト 1 件: §21 と**無関係**であることを検証済み（後述）

Critical はゼロ。実装者からの申し送り 3 点のうち #2（flaky）と #1（design 変更禁止対応）は妥当。#3（accentColor 着色）のみ設計意図との乖離が残り、ユーザー判断が望ましい。

**判定: NEEDS_DISCUSSION**

理由: 機能・テスト・ビルドはすべて健全で Critical/Major はない。ただし Decision 9-3 / 9-5 が要求する「`accentColor` / `Theme.cellAccentColor` での着色」が、コア型（`Theme` に `cellAccentColor` なし、`RadioCell`/`SimpleCheckCell` に `accentColor` フィールドなし）の制約により**設計記述どおりには実装できておらず**、title 色流用という代替で着地している。これは「実装が誤っている」のではなく「設計記述とコア API の整合をどう取るか」という設計判断であり、design/spec 変更禁止ルール下ではレビュアー単独で決められない。ユーザー確認のうえで (a) 現状の title 色流用を許容として明文化、または (b) 後続変更提案で `cellAccentColor` / `accentColor` を追加、のいずれかを決める必要がある。

---

## 検証ログ（テスト実行可否）

- `cd android && ./gradlew :ks-settingsview-ui:testDebugUnitTest --rerun-tasks`
  → BUILD SUCCESSFUL。BasicCellsTest=32 / ApplyDiffTest=15 / KsCellRegistryTest=10 ほか全スイート failures=0, errors=0, skipped=0。
- `./gradlew :ks-settingsview-ui:lintDebug` → BUILD SUCCESSFUL。
- `openspec validate add-cell-types-basic --strict` → valid。
- flaky 検証: `ks-settingsview-compose` モジュールは本作業ツリーで**変更ゼロ**（`git diff --stat -- android/ks-settingsview-compose/` が空）。`KsSettingsViewComposeTest > DSL 方式で外部 state を 2 回連続更新...` を `git stash`（クリーン HEAD = 142e6ad）で 4 回実行したところ FAIL→PASS→FAIL→PASS→PASS と**非決定的**に挙動した。§21 改修は `ks-settingsview-ui` に限定されるため、当該 flaky は本改修と無関係という申し送り #2 の判断は**妥当**。テスト失敗の見逃しには該当しない。

---

## 指摘事項

#### 🔵 Suggestion: Radio/SimpleCheck のチェック色が design 9-3/9-5 の `accentColor`/`Theme.cellAccentColor` 着色と乖離

**該当箇所**: `RadioCellViewHolder.kt:47-49` / `SimpleCheckCellViewHolder.kt:51`

**問題点**:
design.md 9-3 は「色は `accentColor` / `Theme.cellAccentColor` で着色する」、9-5 系も accent 着色を前提としている。しかし実装は次の制約に直面している。
- コア `Theme`（`ks-settingsview-core/.../Theme.kt`）に `cellAccentColor` フィールドが存在しない（`selectedColor` はある）。
- `RadioCell` / `SimpleCheckCell` には `accentColor` フィールドが**そもそも無い**（`SwitchCell`/`CheckboxCell` のみ保有）。

このため両 ViewHolder は `checkView.color = cell.style.titleColor?.toColorInt() ?: effective.titleColor` と**実効タイトル色を流用**している。視覚的には機能するが、設計が想定したアクセント着色とは異なる。さらに `RadioCellViewHolder.kt:47` のコメント「accentColor 指定があればその色」は、`RadioCell` に `accentColor` が無いため**事実と異なるミスリードなコメント**になっている（実際は style.titleColor → titleColor のみ）。

**推奨修正**:
spec/design の変更禁止ルール下では本レビューで断定せず、ユーザー判断を仰ぐ。選択肢:
1. 現状の「title 色流用」を許容と決定し、後続で design 9-3/9-5 の文言を「`cellAccentColor` 未導入のため当面は実効 title 色を使用」と補足（design 変更可能になった時点で）。
2. 後続変更提案で `Theme.cellAccentColor` および `RadioCell.accentColor` / `SimpleCheckCell.accentColor` を追加し、本来の着色に揃える。

いずれにせよ、少なくとも `RadioCellViewHolder.kt:47` の「accentColor 指定があれば」コメントは現コードの挙動（style.titleColor フォールバック）に合わせて即時修正することを推奨（コメントとコードの不一致）。

---

#### 🔵 Suggestion: 申し送り #1（design 9-4 への配置決定追記）の扱いは妥当だが追跡導線が弱い

**該当箇所**: tasks.md 21.3.3 / `SimpleCheckCellViewHolder.kt:14-16`（KDoc）

**問題点**:
tasks.md 21.3.3 は「オリジナルと照合して配置を決定し **design.md 9-4 に追記**」を指示。実装者は「openspec/specs・changes ドキュメントの書き換え禁止」ルールに従い design.md 本文には反映せず、配置決定（オリジナル `SimpleCheckCellView.cs` 準拠で accessory 右側 30×30dp、従来の左側 ✓ から変更）を ViewHolder の KDoc に記載した。

レビュアー所見: **この扱いは妥当**。配置決定そのものはオリジナル `SimpleCheckCellView.cs`（`AccessoryStack.AddView(_checkView, 30x30)` = 右側配置）と一致しており、技術的に正しい。design.md を編集しなかった判断もレビュー禁止事項（仕様ドキュメント書き換え）と整合する。ただし tasks.md 21.3.3 が「design.md に追記」と明記したまま `[x]` 完了になっているため、タスク記述と実際の成果物（KDoc 記載）に齟齬がある。

**推奨修正**:
design.md 本文への追記が必要かはユーザー判断。当面は (a) tasks.md 21.3.3 の文言を「配置決定は ViewHolder KDoc に記載（design 変更禁止のため）」と注記する、(b) もしくはアーカイブ前に design.md 9-4 の「配置（accessory 側）」確定文言をオーナーが追記する、のいずれか。SimpleCheckCell の data class KDoc（`SimpleCheckCell.kt:9`）が依然「左側に小さなチェックを表示」のままで、右側配置に変えた実態と矛盾している点は併せて更新を推奨。

---

#### 🔵 Suggestion: SwitchCell ウィジェット直接操作の挙動が「従来どおり」とは厳密には異なる

**該当箇所**: `SwitchCellViewHolder.kt:140-142`（`isClickable = false` / `isFocusable = false`）

**問題点**:
9-6 の「スイッチウィジェット直接操作も従来どおり機能する」を、スイッチを `isClickable=false` にし、スイッチ上のタップを container にバブリングさせて `toggle()` する方式で実現している。結果として「スイッチをタップするとトグルする」は成立するが、`MaterialSwitch` 本来の**サムをドラッグしてスライドする操作**は無効化される（クリック扱いになる）。機能的・テスト的には問題なく、二重発火も発生しない（スイッチがクリックを消費しないため container 一本に集約され、`OnCheckedChangeListener` 経由で 1 回のみ発火）。設計の主眼（セル全体タップ + 二重発火防止）は満たしている。

**推奨修正**:
必須ではない。ドラッグ操作も維持したい場合はスイッチを `clickable` のままにし、container クリック時にスイッチ領域内タップを除外する制御が必要になるが、二重発火リスクとのトレードオフがある。現状の割り切りは Android 設定アプリの一般的挙動とも整合し許容範囲。実機確認タスク（21.7.6）で UX を最終判断するのが妥当。

---

#### 🔵 Suggestion: build.gradle のテーマ要件コメントが Decision 8 の Material3 必須と不整合

**該当箇所**: `android/ks-settingsview-ui/build.gradle.kts`（material 依存追加コメント）

**問題点**:
追加された依存コメントが「利用者アプリは Theme.MaterialComponents.* または Theme.AppCompat.* 派生テーマを指定する必要がある」と記載しているが、Decision 8 および本改修のテスト（`ContextThemeWrapper(..., Theme_Material3_Light_NoActionBar)`）・MEMORY（Android テーマ要件）に従えば `MaterialSwitch` は `?attr/materialSwitchStyle`（Theme.Material3.* 系のみ定義）を要求するため、実態は **Theme.Material3.* 必須**。コメントが古いテーマ要件（Decision 7 期）のまま。

**推奨修正**:
コメントを「利用者アプリは Theme.Material3.* 派生テーマが必須（MaterialSwitch の materialSwitchStyle 要求）」に修正。`docs/android-ui.md` のテーマ要件記述（Material3 必須）と表現を揃える。

---

## 良好な点（特筆）

- **9-1 payload 機構の整合性**: `equals`/`hashCode` から内部状態を除外 → `areContentsTheSame==true` → `getChangePayload` が内部状態差分のみ payload 化 → `onBindViewHolder(payloads)` が `bindStateOnly` で局所更新、という流れが破綻なく成立。`getItemId` も内部状態を除外した `hashCode` を用いるため、トグル時に item id が変わらず remove+insert アニメ（＝ちらつきの別要因）も誘発しない。設計の「外部 `submitList` 反映」「二重発火しない」要件をテスト（`bindStateOnly は onValueChanged を発火させずに表示を更新する` / `payload 経由の bindStateOnly で外部更新が反映される`）で実証している。
- **`KsSimpleCheckView`**: オリジナル `SimpleCheck.cs` の座標（22/52→38/68, 36/66→74/28）、`StrokeWidth=ToPixels(2)`、`AntiAlias`、`SetWillNotDraw(false)`、`Selected` 時のみ描画を 1:1 で移植。`isChecked` setter で値変化時のみ `invalidate()` する点も適切。
- **`ic_navigate_next.xml`**: 18×26dp / `#FFCACACA` でオリジナル準拠。`CommandCellViewHolder` の `AppCompatImageView` 置換と `hideArrow` visibility 制御も維持。
- **`applyCellBackground`**: `RippleDrawable(selectedColor, ColorDrawable(bg), null)` でオリジナル `CellBaseView.cs` の ripple を再現。`onDrawOver` 罫線（Decision 8）との描画順序分離も KDoc で説明済み。
- **テスト品質**: スタブ・スキップ・言い訳コメントによる実質スキップは無し。境界（payload null 返却、状態同一時の無更新）・通知発火/非発火・再利用クリアを網羅。

---

## アクションプラン（優先度順）

1. （任意・即時可）`RadioCellViewHolder.kt:47` の「accentColor 指定があれば」コメントを実コード挙動（`style.titleColor` フォールバック）に合わせて修正。`SimpleCheckCell.kt:9` の「左側に小さなチェック」KDoc を右側配置の実態に合わせて修正。`build.gradle.kts` のテーマ要件コメントを Material3 必須に修正。
2. （要ユーザー判断 / NEEDS_DISCUSSION 本体）Decision 9-3/9-5 の accent 着色意図に対し、(a) title 色流用を許容と明文化するか (b) 後続提案で `Theme.cellAccentColor` / `RadioCell.accentColor` / `SimpleCheckCell.accentColor` を導入するかを決定。
3. （要ユーザー判断）tasks.md 21.3.3「design.md 9-4 に追記」の扱い（KDoc 記載で完了とみなすか、アーカイブ前にオーナーが design へ反映するか）を確定。
4. （アーカイブ前の残作業・本レビュー対象外）21.5.3 / 21.5.4 / 21.7.2〜21.7.6 の Pixel 6a 実機目視検証（Ripple 重畳順序・selectedColor 反映・ちらつき非発生・セル全体タップ）は未チェックのまま。これらは実機必須タスクであり、コードレビューでは合否判定不能。実機検証完了をアーカイブ条件とすること。

---

## 判定結果

**ステータス: NEEDS_DISCUSSION**

- Critical / Major 指摘なし。ビルド・全ユニットテスト PASS・`openspec --strict` PASS。
- compose flaky テストは §21 と無関係であることを検証済みでテスト失敗の見逃しに該当しない。
- 設計記述（9-3/9-5 の accent 着色）とコア API の乖離、および tasks.md 21.3.3 の design 追記指示の扱いについて、spec/design 変更禁止ルール下ではレビュアー単独で結論を出せないため、ユーザー確認を推奨する。
- 上記設計判断が解消（または現状許容と明文化）されれば、実装そのものは APPROVED 相当の品質。
