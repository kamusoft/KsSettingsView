# レビュー結果: fix-android-accessory-header-refresh (001 回目)

**日付**: 2026-08-05
**判定**: CHANGES_REQUESTED

## サマリー

実装本体 (DiffCallback の Section H/F 内容比較・`setRootDirect` の構造/内容分離) はデルタスペックの Requirement と ADR-0012 (補正後) に正しく対応しており、設計・コメント・命名の品質も高い。ビルドとテストは全件 green (1964 件 / 失敗 0 / エラー 0、`./gradlew test --rerun-tasks` で再実行確認済み)。

一方でテストの回帰検出力に穴がある。修正前コード (HEAD) に対して新規テストを実測したところ、**デルタスペックの Scenario に 1:1 対応する positive テスト 2 件が修正前コードでも green** だった。この 2 件は「実装が壊れても落ちない」ため、Scenario の担保になっていない。加えて待機ヘルパの設計上、負のアサーションが黙って空振りし得る構造になっている (実測で 1 件が毎回 5 秒空転していることも確認)。

## 検証した内容 (客観的事実)

| 項目 | 結果 |
|---|---|
| `cd android && ./gradlew test --rerun-tasks` | BUILD SUCCESSFUL / **1964 tests / 0 failures / 0 errors / 0 skipped** (XML 集計) |
| `python3 scripts/comment-policy-lint.py` | 変更 4 ファイルはいずれも違反 0 件 (`ListAdapterDiffTest.kt` の既存 `openspec/...` 参照も解消済み) |
| 足場凍結 | `specs/.../spec.md` mtime 11:19:35 に対しコード編集は 11:28 以降。実装中の spec 書き換えなし |
| deviation.md 記録済みの乖離 | 「旧∩新 かつ値が変化した Cell のみへ通知」— 合意済み差分として指摘対象外 |
| ミューテーション実測 (lessons L-001) | scratchpad の独立コピーに本体 2 ファイルの HEAD 版を配置して新規テストを実行。**18 件中 9 件 fail** = 実装の効果は概ね検出できている。リポジトリは無改変 (`git status` で確認済み) |

---

## 指摘事項

### [🟠 Major] Scenario 対応テスト 2 件が修正前コードでも green — 回帰検出力ゼロ

**該当箇所**:
- `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/FullUpdateContentSyncTest.kt:152-180` (`replaceSection による header text 変更が表示へ反映される`)
- 同 `:362-381` (`Full diff で同一 id の Cell 内容変更が表示へ反映される`)

**問題点**: 本体 2 ファイルを HEAD (修正前) に戻して同テストを実行した実測結果:

```
PASS  replaceSection による header text 変更が表示へ反映される          (0.02s)
PASS  Full diff で同一 id の Cell 内容変更が表示へ反映される            (5.20s)
FAIL  updateAccessory による header text 変更が…                        ← 検出力あり
FAIL  replaceSection で同一 id の Cell 内容変更が…                      ← 検出力あり
（18 件中 9 件 FAIL / うち PASS 9 件の大半は negative テストで正しく PASS）
```

原因は 2 つある。

1. ヘルパ `bindSectionText` / `bindCellTitle` (`:64-82`) は **新規 ViewHolder を作って `onBindViewHolder` を直接呼ぶ**。`adapter.currentList` は修正前でも `submitList` によって新しい平坦リストへ差し替わるため、「新しい ViewHolder に bind すると新しい値が出る」ことは通知の有無と無関係に常に成り立つ。したがってこの 2 件のアサーションは本修正に対してトートロジーになっている。
2. `Full diff …` の待機 `awaitDifferCommit { recorder.changed.isNotEmpty() }` は修正前コードでは条件が成立しないが、`awaitDifferCommit` はタイムアウトしても失敗しないため 5.20s 空転した後そのまま先へ進み、上記のトートロジーなアサーションで PASS する。

対応する Scenario は「replaceSection による header text 変更が表示へ反映される」「Full diff で同一 id の Cell 内容変更が表示へ反映される」の 2 つで、いずれも現状テストでは担保されていない。

**推奨修正**: 兄弟テスト (`:339-360` の `replaceSection で同一 id の Cell 内容変更…`) と同様に、`NotificationRecorder` による payload 付き変更通知のアサーションを 2 件へ追加する。`ChangeRecord(position, 1, KsSettingsListAdapter.PAYLOAD_CONTENT)` の照合が実質的な検出力を持つ唯一のアサーションであるため、positive Scenario のテストには必ず含める。修正後に同じミューテーション実測 (HEAD の本体で新テストを走らせる) をやり直し、両件が FAIL することを確認すること。

---

### [🟡 Minor] `awaitDifferCommit` がタイムアウトで失敗しない

**該当箇所**: `FullUpdateContentSyncTest.kt:53-61`

**問題点**: 条件が満たされないまま `timeoutMillis` を過ぎても、ヘルパは `idle()` を 1 回呼んで**黙って戻る**。失敗パスが存在しない。このため「待った後に何かが起きていないことを確かめる」型のアサーションは、待機が空振りしただけでも通過する。Major の 2 件目 (`Full diff …`) はまさにこの経路で PASS していた。同型の潜在的な空振りは `内容が変わらない Cell へは内容通知を発行しない` (`:576-596`) にもある — 待機条件 `adapter.currentList !== committedBefore` が成立しなくても `assertEquals(emptyList(), recorder.changed)` は通る。

**推奨修正**: タイムアウト時に `fail("差分コミットが $timeoutMillis ms 以内に完了しなかった")` で落とす。1 箇所の修正で本ファイル全体の負のアサーションが「コミット完了後に評価されたこと」を保証できる。

---

### [🟡 Minor] 待機条件の値が誤っており、テスト 1 件が毎回 5 秒空転している

**該当箇所**: `FullUpdateContentSyncTest.kt:281` (`awaitDifferCommit { adapter.itemCount == 5 }`)

**問題点**: 初期状態の平坦リストは s1 (header + c1 + footer) + s2 (header + c2) = **5 件**であり、`InsertCell` 適用後は **6 件**になる。条件 `itemCount == 5` は初回 `idle()` 直後にはすでに 6 になっているため成立せず、毎回タイムアウトまで回る。修正後コードでの実測所要時間 (テスト XML) がこれを裏づける:

```
5.01s  内容が同一の Section H_F へは変更通知を発行しない   ← 他の全 20 件は 0.00〜0.01s
```

結果としてテストは通っているが (5 秒の空転中にコミットが完了するため)、意図した同期点で待っていない。同じ理由で、将来この条件が「本当に待ちたいこと」を守らない。

**推奨修正**: `adapter.itemCount == 6` に直す (または `recorder.inserted.isNotEmpty()` のように事象ベースの条件にする)。上記 Minor のタイムアウト失敗化を入れると、この誤りは即座に検出される。

---

### [🟡 Minor] `SectionHeader.headerHeight` の変化が内容比較から漏れている

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsListAdapter.kt:293-296` / `337-348`

**問題点**: `CellListItem.SectionHeader` は `accessory` に加えて `headerHeight` を保持し、`SectionTextAccessoryViewHolder.bind` はこれを `itemView.layoutParams.height` へ反映する。しかし `areContentsTheSame` は `isSameAccessoryContent(accessory, accessory)` しか見ないため、**header text が同じまま `Section.headerHeight` だけを変えた `replaceSection` / Full 更新は再 bind されず、固定高さが反映されない**。`Section.headerHeight` は公開 API であり、これを変える手段は full 更新経路しかないため実際に到達可能な穴である。

デルタスペックは Requirement を「accessory が非 null から非 null へ変わる更新」に限定しており、さらに「accessory 内容が変わらない場合は変更通知を発行しない」と定めているため、**本実装は spec 準拠**であり、修正前も同じ挙動だった (新規に作り込まれた穴ではない)。ただし本変更は「Section H/F の内容差を DiffUtil で拾う」箇所そのものを書き換えており、読み手が「Section H/F の内容差は網羅された」と誤解しやすい状態になっている。

**推奨修正**: いずれか。(a) `SectionHeader` の比較に `headerHeight` を含める — `Double` の値比較でありリスナー等価不安定性の問題は生じないため、`内容が同一なら通知しない` Scenario とも矛盾しない。(b) スコープ外とするなら、`isSameAccessoryContent` の呼び出し側 KDoc に「`headerHeight` は比較対象に含まない」を明記し、follow-up として別 change に切る。

---

### [🔵 Suggestion] `contentChangedCellIds` の KDoc の断定が deviation.md の但し書きと食い違う

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:955-956`

**問題点**: 「各 Cell の `equals` は… 値等価であり、**内容が変わっていれば必ず不等価になる**」と断定しているが、`deviation.md` が記録するとおり `CustomCell` は `builder` / `onTap` を等価性から除外する (core/ADR-0014) ため、`builder` だけを差し替えた場合は等価と判定され通知対象にならない。既存の利用者契約どおりの挙動ではあるが、コメントの断定はそれを覆い隠す。

**推奨修正**: 「関数値 (`builder` / `onTap`) は等価性に参加しないため、表示に効く値は `content` 側に含める必要がある (core/ADR-0014)」といった但し書きを添える。`kasane/changes/` 配下は蒸留後にアーカイブされるため、この前提はコード側コメントに残す価値がある。

---

### [🔵 Suggestion] accessory の型切替は実環境での目視確認を推奨

**該当箇所**: `FullUpdateContentSyncTest.kt:183-215` (`accessory の型の切替が表示へ反映される`)

**問題点**: 本テストは通知の発行と `getItemViewType` の切替、および新規 ViewHolder への bind 結果までを確認しているが、`RecyclerView` の実レイアウトパスは通していない。payload 付き変更通知 + stable IDs + view type 変化という組み合わせで ViewHolder が実際に交換されるかは `ItemAnimator` の実挙動に依存し、`cross/conventions/test-execution.md` が「Robolectric で検証したつもりになる範囲」として名指しする領域に近い。

なお `cross/conventions/runtime-behavior-verification.md` の適用対象 (ユニットテストで症状自体を再現できない不具合) には本件全体としては当たらない — ミューテーション実測のとおり症状 (通知が出ない) は 9 件のテストで再現できている。したがって完了判定を止めるものではないが、型切替だけは Sample アプリでの目視確認を 1 回入れておくとリスクが閉じる。

**推奨修正**: `samples/android` で Section header を Text → View へ切り替える操作を 1 回実行し、行が新しい内容になることを確認する。証跡を残せるなら `kasane/changes/fix-android-accessory-header-refresh/` 配下に置く。

---

## アクションプラン

1. **[Major]** `replaceSection による header text 変更が表示へ反映される` / `Full diff で同一 id の Cell 内容変更が表示へ反映される` に payload 付き通知のアサーションを追加し、HEAD 版本体に対するミューテーション実測で両件が FAIL することを確認する
2. **[Minor]** `awaitDifferCommit` をタイムアウト時に `fail()` させる
3. **[Minor]** `:281` の待機条件を `adapter.itemCount == 6` (または事象ベース) に修正する
4. **[Minor]** `SectionHeader.headerHeight` を内容比較に含めるか、除外を KDoc に明記して follow-up 化するかを決める
5. **[Suggestion]** `contentChangedCellIds` の KDoc に `CustomCell` の関数値除外の但し書きを添える
6. **[Suggestion]** accessory 型切替を Sample アプリで 1 回目視確認する

1〜3 はいずれもテストコードの局所修正で、実装本体の変更を伴わない。
