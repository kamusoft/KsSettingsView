# レビュー結果: align-view-accessory-header-height (001 回目)

**日付**: 2026-08-11
**判定**: NEEDS_DISCUSSION

## サマリー

デルタスペックの 2 Requirement / 11 Scenario はすべて実装とテストで満たされており、ビルド・テストは Android (1107 件 × debug/release = 2214 件, 0 failures) / iOS (457 件, 0 failures) ともに green。高さ解決の共通化 (`applySectionHeaderHeight`) と、内容の再バインドを伴わない高さ専用 payload 経路の設計はいずれも妥当で、payload 経路にはミューテーション実測による回帰検出力も確認できた。

一方で、**固定高さが accessory の内容高さより大きい場合に、hosted view が固定高さいっぱいまで伸びない**点が Android 側に残っており、iOS (contentView へ 4 辺 pin) と非対称のままになっている。本 change の目的が OS 対称化であること、および `ui/verification/` のスクリーンショットにその差が実際に写っていることから、実装判断ではなく契約判断が要る論点として NEEDS_DISCUSSION とする。デルタスペック自体は「Header 領域の高さ」しか規定していないため、これは spec 違反ではなく spec の未規定範囲である。

## 指摘事項

### [🟠 Major] 固定高さ時に hosted view が領域を埋めず、iOS と非対称が残る

**該当箇所**:
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SectionAccessoryViewHolders.kt:394-401` (`bindKsAnyView` が子を `FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)` で addView)
- 対比: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1871-1879` (hosted view を contentView の leading/trailing/**top/bottom** へ required priority で pin)

**問題点**:

本 change によって Android の view accessory の Header 領域は固定高さになったが、その中に載る hosted view は `WRAP_CONTENT` のまま top 揃えで配置される (FrameLayout の既定 child gravity は `TOP|START`)。したがって「固定高さ > 内容高さ」のとき、領域下部に hosted view が及ばない帯が残る。

iOS は hosted view を contentView の上下端へ required priority の制約で pin しているため、同じ条件で hosted view は固定高さいっぱいに引き伸ばされる。つまり「固定高さが view accessory にも効く」ことは対称化されたが、**その領域内での hosted view の占有範囲は非対称のまま**になっている。背景色や枠線を持つカスタム view を固定高さ Header に置いた利用者には、iOS では 48dp 全面が塗られ Android では内容分しか塗られない、という見た目の差として現れる。

実証 (`ui/verification/android-header-height-states.png`, 720×1280 / density 2.0 = 48dp は 96px):

| 状態 | 領域 | 内訳 |
|---|---|---|
| A 自動 | y 0-157 → 区切り線 158 | accessory が全域を占有 |
| B 固定 48dp・内容が収まる | y 398-493 (96px = 48dp) → 区切り線 494 | accessory は **64px のみ**、下 32px は未描画 |
| C 固定 48dp・内容がはみ出す | y 734-829 (96px = 48dp) → 区切り線 830 | accessory が全域を占有 (clip 成立) |

行の高さ自体はいずれも仕様どおり (B・C とも 96px = 48dp) であり、デルタスペックの Scenario は満たしている。問題は領域内の占有のみ。

なお `ui/brief.md` の「視覚照合結果」は「approved.png と一致・合意済み妥協は 0 件」と記録しているが、承認モックの状態 B は Header 領域が固定高さいっぱいに塗られた矩形として描かれており、上記の 32px の帯はモックに存在しない。brief の照合記録は現状この差を捉えられていない。

**推奨修正** (いずれも契約判断が必要なため、決定はオーナーに委ねる):

- 案1: 固定高さが解決されたときは hosted view を `MATCH_PARENT` (または `layout_gravity = FILL`) で載せ、iOS と同じ「領域いっぱい」に揃える。デルタスペックへ「固定高さ時、hosted view は領域全体を占める」旨の Scenario を追加する必要があるため、spec 追記を伴う
- 案2: 現状 (内容なりの高さで top 揃え) を意図した挙動として確定し、デルタスペックまたは concepts に「固定高さは領域の高さのみを決め、hosted view の配置には関与しない」と明記したうえで、`ui/brief.md` の照合記録を「合意済み妥協 1 件」に改める
- どちらを採るにせよ、`ui/brief.md` の「合意済み妥協は 0 件」は現状の実装と整合しないため、記録の更新が要る

### [🟡 Minor] `PAYLOAD_CONTENT` の KDoc が「3 引数版は未実装」と述べており、同一ファイルの実装と矛盾する

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsListAdapter.kt:240-241`

```
     * `SimpleItemAnimator.canReuseUpdatedViewHolder` が true を返して同一 ViewHolder への
     * 再 bind が保証される。3 引数版 `onBindViewHolder` は実装していないため、
     * RecyclerView の既定動作で 2 引数版へ委譲されフル bind となり内容は完全に反映される。
```

**問題点**: 本変更で 3 引数版 `onBindViewHolder` (同ファイル 204-216 行) が実装された。`PAYLOAD_CONTENT` の挙動 (フル bind へ落ちる) 自体は変わらないが、その理由が「未実装だから既定動作で委譲される」から「実装した振り分けが `super` へ委譲するから」へ変わっており、KDoc は事実と食い違う。同種の記述を持つ `ContentUpdatePayloadTest.kt` のコメントは更新済みで、本番側だけが取り残されている。この記述を信じた読み手が 3 引数版の存在を見落とすと、payload 追加時に振り分けを通さない実装をしかねない。

なお `KsSettingsView.kt:640-642` の `applyThemeInternal` の KDoc (「payload を渡しても各 ViewHolder の `onBindViewHolder(holder, position)` が呼ばれる」) も同じ前提に立つ記述で、現在は 3 引数版が `PAYLOAD_THEME` を `super` へ落とすことで成立している。こちらは文言としては誤りではないが、併せて見直すと整合が取れる。

**推奨修正**: 「3 引数版 `onBindViewHolder` は本 payload を振り分け対象外として `super` へ委譲し、2 引数版のフル bind で内容が完全に反映される」といった、現在の実装を現在形で述べる記述へ書き換える。

### [🔵 Suggestion] `applySectionHeaderHeight` の `internal` 可視性

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SectionAccessoryViewHolders.kt:311`

**問題点**: 呼び出し元は同一ファイル内の 2 箇所のみで、テストからも直接参照されていない (テストは ViewHolder / Adapter 経由で検証している)。同ファイル内の他のヘルパ (`createSectionTextView` / `createAccessoryContainer`) は `private` で揃っている。

**推奨修正**: `private` へ落とす。ファイル外での再利用が見込まれるなら現状維持でよいが、その場合は `bindKsAnyView` と同様に「モジュール内共用ヘルパ」であることが分かる位置づけにしたい。

### [🔵 Suggestion] `layoutParams == null` 分岐が到達不能かつ RecyclerView 子として不適切な型を作る

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SectionAccessoryViewHolders.kt:328-334`

**問題点**: 2 つの呼び出し元 (`createSectionTextView` / `createAccessoryContainer` 生成の itemView) はいずれも生成時点で `layoutParams` を設定済みのため、`lp == null` 分岐は実際には到達しない。仮に到達した場合、RecyclerView に attach 済みの子へ素の `ViewGroup.LayoutParams` を代入することになり、`RecyclerView.LayoutParams` が持つ情報を落とす経路になる。

**推奨修正**: 分岐を削除して `view.layoutParams` を非 null 前提で扱うか、残すならフォールバックの必要性 (どの経路で null になり得るか) をコメントで自己完結させる。

### [🔵 Suggestion] `ui/verification/` の rebound スクリーンショットが states と byte 同一

**該当箇所**: `kasane/changes/align-view-accessory-header-height/ui/verification/android-header-height-rebound.png`

**問題点**: `android-header-height-states.png` と SHA-256 が完全一致する (`e21904cb…`)。再表示後も表示が変わらなければ同一バイト列になるのは自然だが、証跡としては「再取得したもの」と「複製したもの」を区別できない。

**推奨修正**: 再表示の証跡としては、スクロール位置や時刻表示が異なる別フレームを添えるか、`ui/brief.md` に「再表示後の画面は states と同一表示のためバイト一致」と明記して意図を残す。なお「混在しても入れ替わらない」Scenario 自体はユニットテスト (`ViewAccessoryHeaderHeightTest` の ViewHolder 再利用テスト) と `-scrolled.png` で押さえられているため、証跡の欠落ではない。

## 確認した観点 (指摘に至らなかったもの)

- **payload 振り分けの正しさ**: `payloads.all { it == PAYLOAD_HEADER_HEIGHT }` は、`PAYLOAD_THEME` (`applyThemeInternal` の一括通知) や `PAYLOAD_CONTENT` が混在・蓄積した場合に必ずフル bind へ落ちる。payload 文字列の衝突もない (`ks-theme` / `ks-content` / `ks-header-height`)
- **ViewHolder 再利用時の高さ引きずり**: `bind()` が常に `applySectionHeaderHeight` を通り、未指定時に `WRAP_CONTENT` へ戻す。Footer 経路は `isHeader = false` で常に自動高さ
- **回帰検出力の実測** (lessons/code-review L-001): `getChangePayload` の `heightOnly` を強制 false にするミューテーションを入れたところ、高さ payload 関連 4 件のみが FAILED し、静的な高さ解決テストは PASSED のまま。高さ専用経路のテストが「view インスタンス維持」を実際に検出していることを確認 (実施後 `shasum` 一致で原状復帰済み)
- **コメント規約**: `python3 scripts/comment-policy-lint.py --summary` = 禁止 0 件 (572 ファイル)。新規コメントに change-id / Phase / レビュー通番・デルタスペック構文キーワードの混入なし
- **足場の逆流**: `proposal.md` / `specs/` / `exploration.md` は無変更。`tasks.md` はチェックの反転のみ、`ui/brief.md` は末尾への視覚照合結果の追記のみで、既存記述の書き換えなし
- **iOS プロダクションコード**: 無変更 (差分はテストのみ) で契約どおり
- **本レビューの独立性**: 作業中に `second-opinion-code-001.md` が作業ツリーに現れたが、独立文脈を保つため読んでいない

## アクションプラン

1. **[要判断]** Major の hosted view 占有範囲について、案1 (iOS と同じく領域いっぱいへ伸ばす + spec に Scenario 追記) か案2 (現状を意図挙動として確定 + spec/concepts へ明記) をオーナーが決める。いずれの場合も `ui/brief.md` の「合意済み妥協は 0 件」を実態に合わせて更新する
2. Minor: `KsSettingsListAdapter.kt:240-241` の KDoc を現在の実装に合わせて書き換える (併せて `KsSettingsView.kt` の `applyThemeInternal` KDoc も整合確認)
3. Suggestion 3 件 (可視性・到達不能分岐・検証スクリーンショットの記録) は 1・2 の対応時にまとめて処理するか、見送りを明示する
