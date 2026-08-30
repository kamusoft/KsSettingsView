# レビュー結果: fix-android-header-height-refresh (002 回目)

**日付**: 2026-08-05
**判定**: APPROVED

**スコープ**: review-001 の Minor 1 件 (コメントの Text accessory 限定条件の欠落) に対する修正の解消可否確認に限定した追認レビュー。実装全体の再レビューは 001 で完了しており、本回では行っていない。

## サマリー

review-001 の Minor は **解消**。指摘した 2 箇所 (`KsSettingsListAdapter.kt:257` のクラス KDoc、`:293-294` のインラインコメント) はいずれも「Text accessory の Header に限る」条件を明示する文言に置き換わっており、クラス KDoc だけを読んだ保守者が「View accessory の Header も高さ変更で rebind される」と誤読する余地はなくなった。差分はコメント 2 箇所のみで、`areContentsTheSame` / `isSameHeaderHeight` の式は 001 時点から一切変わっていない。

新たな規約違反・事実誤りの混入もなし。Critical / Major / Minor いずれもなく、追加の指摘は無い。

## 解消確認

### 1. Minor「クラス KDoc とインラインコメントが Text accessory 限定の条件を落としている」→ 解消

| 箇所 | 修正後の文言 | 判定 |
|---|---|---|
| `KsSettingsListAdapter.kt:257` (クラス KDoc) | 「Section H/F は **accessory の内容を比較**する（Text accessory の Header は固定高さも含む）」 | 解消。括弧内が「Text accessory の Header」に限定され、View accessory の Header が固定高さ差で rebind されるという読みは成立しない |
| `KsSettingsListAdapter.kt:293-294` (`areContentsTheSame` 内) | 「Text accessory の Header は表示高さも再 bind でしか反映されないため、固定高さの差も内容差として扱う（View accessory は [isSameHeaderHeight] のとおり対象外）」 | 解消。限定条件に加えて View accessory が対象外である結論と参照先を明示している |

契約の要約 (クラス KDoc) → 判定箇所 (インライン) → 実装理由 (`isSameHeaderHeight` の KDoc `:353-363`) の3層が、いずれも「Text accessory 限定」で一貫した。

### 2. 文言の事実性 (実装との突き合わせ)

- 「固定高さを表示へ反映するのは Text accessory だけ」— `KsSettingsListAdapter.onBindViewHolder:159-168` で `headerHeight` を渡すのは `SectionAccessory.Text` 分岐のみ。`SectionAccessory.View` 分岐は `SectionAnyViewAccessoryViewHolder.bind(accessory)` で高さを受け取らない。記述どおり
- 「表示高さも再 bind でしか反映されない」— 高さを適用するのは `SectionAccessoryViewHolders.kt:79-97` (`SectionTextAccessoryViewHolder.bind` 内) だけで、他に `layoutParams.height` を更新する経路はない (`grep headerHeight` で main ソース全件確認)。記述どおり
- 「（View accessory は [isSameHeaderHeight] のとおり対象外）」— 参照先 `:368` の `if (oldItem.accessory !is SectionAccessory.Text) return true` と一致

### 3. 新たな規約違反の有無

- **`concepts/cross/conventions/comment-policy.md`**: 追加文言に禁止参照 (変更提案識別子・Phase/Decision 通番・アーカイブ文書パス・拡張子なし裸参照)、禁止類型 (履歴記述・過去仕様・`MUST` 等のデルタスペック構文キーワード) はいずれも含まれない。`[isSameHeaderHeight]` は同一ファイル内のコード識別子への参照であり、規約 15 行目「リポジトリ内のコード識別子への参照は外部参照ではなく自由に書いてよい」に該当する
- **機械検査**: `python3 scripts/comment-policy-lint.py android/ks-settingsview-ui/.../KsSettingsListAdapter.kt` → 禁止 0 件 (本レビューで再実行し確認)
- **自己完結性** (ksn-review 汎用観点「コメントが単独で理解できるか」): 両箇所とも外部文書 ID に依存せず、同一ファイル内の識別子だけで完結している

### 4. ビルド

- `./gradlew :ks-settingsview-ui:compileDebugKotlin` → **BUILD SUCCESSFUL** (全タスク UP-TO-DATE = 現在のソース内容でコンパイル済み)。KDoc ブロックの閉じ忘れ等の構文破壊がないことを確認
- テスト全件 (2002 件 pass) は review-001 で確認済み。本回の差分はコメント 2 箇所のみで、`areContentsTheSame` / `isSameHeaderHeight` の式・シグネチャ・テストコードいずれも 001 時点から変更がないため、全件再実行は行っていない (実装側で `--rerun-tasks` 実行済みの報告あり)

## 指摘事項

なし。

review-001 の Suggestion 2 件 (`display-state-synchronization.md:44` への追随、Text 限定判断の `fix-ios-view-header-height-override` への引き継ぎ) は蒸留フェーズ向けであり、本回でも状態は変わらず未着手のまま。実装レビューの範囲では対応不要。

## アクションプラン

1. 実装側の対応は完了。マージ可能
2. (蒸留フェーズ) review-001 の Suggestion 2 件を ksn-distill で処理する
