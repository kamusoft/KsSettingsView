# レビュー結果: settingsview-migration-defects (002 回目)

**日付**: 2026-09-15
**判定**: APPROVED

## サマリー

review-001 の Major 2 件・Minor 2 件・Suggestion 2 件すべてに対応が入り、いずれも指摘の意図どおりに解消していた。とくに Major 2 への `verification.md` は、暗黙 Style の内容 (マゼンタの見出し・`#FFF6CC` の Cell 背景・Modern の角丸カード) が証跡画像の見た目と一致しており、証跡と提出コードの対応が第三者にも追えるようになっている。

残るのは concepts の 1 文の句点欠落 (Minor) と、追加テストの守備範囲についての所見 (Suggestion) のみで、いずれも優先度は低い。句点だけコミット前に直してほしい。

## 照合した規約

review-001 と同一 (`kasane/handbook/cross/comment-policy.md` (always) / `test-execution.md` / `runtime-behavior-verification.md`、ksn-core `references/evidence.md`・`references/paths.md`、`maui/ADR-0022`・`ADR-0023`、`kasane/lessons/code-review.md` L-001、`kasane/lessons/process.md` L-003・L-005・L-009)。今回の差分は concepts / roadmaps / change 成果物とテスト 1 件の追加が中心で、新たに当たる handbook 文書は無い。

## ビルドとテスト

| 対象 | コマンド | 結果 |
|---|---|---|
| MAUI | `dotnet test maui/KsSettingsView.Maui.Tests/` | 成功 528 / 失敗 0 / スキップ 0 (前回 527 → 追加テスト 1 件で +1) |
| Android | `./gradlew test` | BUILD SUCCESSFUL。テスト結果 XML 集計: `kssettingsview` 1256 件 × 2 variant、`kssettingsview-bridge` 173 件 × 2 variant、いずれも失敗 0・スキップ 0 |

lint:

| lint | 結果 |
|---|---|
| `comment-policy-lint.py` | 禁止 0 件 |
| `local-path-lint.py` | 0 件 |
| `identity-lint.py` | 0 件 |
| `doc-structure-lint.py` | 本 change が触るファイルの**違反 0 件** (前回の `phase-8-scroll-control/agenda.md:10` は解消)。`android/api/android-native-host.md` は「散文 11925 字」の構造の注意 (違反ではない) に載るが、これは変更前から 11783 字で閾値超過だった既存事項 |

注記: `./gradlew test` を 2 つ同時に走らせた最初の試行で `:kssettingsview-bridge:testReleaseUnitTest` が FAILED になったが、同じタスクを単独で回すと BUILD SUCCESSFUL。並走させたこちらの操作が原因で、成果物の欠陥ではない (記録のために残す)。

### 検出力の実測 (code-review L-001)

今回追加された `ImplicitStyleWithSamePlacedViewInTwoSlotsThrows` に 2 種のミューテーションを当てた (いずれも shasum 一致で原状復帰済み)。

| ミューテーション | 結果 |
|---|---|
| `maui/KsSettingsView.Maui/SettingsView.cs` を修正前の内容へ戻す | **失敗** — 期待した `InvalidOperationException` ではなく `NullReferenceException` が出る |
| `RootHeaderView` / `RootFooterView` の validateValue だけを旧形 (`_controller?.`) へ戻す | **通過** (8 件全通過) |

前者が落ちるので、このテストは「暗黙 Style 経由の多重配置が公開契約どおりの例外型で観測できる」という利用者から見える契約を守っている。後者については下の Suggestion 1 を参照。

## 前回指摘への対応確認

| 前回の指摘 | 対応 | 判定 |
|---|---|---|
| Major 1: Android concepts が「スクロール位置は復元対象外」のまま | `android-native-host.md` の本文 (旧 :56) と「保証すること」(旧 :164) を実態へ更新、timestamp を 2026-09-15 へ | 解消 (文面に Minor 1 件) |
| Major 2: 修正後証跡が無参照 | `verification.md` を新設し、論点 1 / 2 それぞれの手段・結果・証跡ファイルを change 相対で参照。導線差し替えを `deviation.md` へ追記 | 解消 |
| Minor 1: 検査厳格化に回帰テストが無い | `ImplicitStyleTests` へ 1 件追加 | おおむね解消 (Suggestion 1) |
| Minor 2: phase-8 agenda が doc-structure lint の新規違反 | 小節へ切り出し | 解消 (lint 違反 0 件を実測) |
| Suggestion 1: KDoc の断定が広い | `restorePendingScrollPosition` の KDoc へ前提 (同一 `concatAdapter` を内容ごと戻すこと・`LinearLayoutManager` は item 数 0 のレイアウトで保留状態を捨てること) を追記 | 解消 |
| Suggestion 2: -01 / -03 のバイト同一 | `verification.md` に「位置が保たれた結果として同じ画面」と明記 | 解消 |

### Major 1 の内容確認

本文の書き換えは「detach 直前のアンカー (先頭可視行と行内オフセット) を控え、再 attach 時に同じ View 内で復元する。View 自体が作り直される経路 (Host の再生成・Activity 再生成) では復元しない」で、実装 (`savePendingScrollPosition` / `restorePendingScrollPosition` と `onSaveInstanceState` に含めない判断) と一致する。復元する範囲と**しない**範囲の両方を書いている点がよい。

### Major 2 の内容確認

`verification.md` の論点 1 は、当てた暗黙 Style の中身 (`HeaderTextColor` = マゼンタ / `CellBackgroundColor` = `#FFF6CC` / `ListStyle` = `Modern`) を明記している。証跡 `evidence/maui-implicit-style-ios-after.png` を実際に開いて照合したところ、Cell 背景が淡黄・「バインド Section」のテキスト見出しがマゼンタ・Section が角丸カード (Modern) で描かれており、記述と画像が一致する。これで「落ちない」だけでなく「Style の指定が表示へ届いた」側まで証跡から読み取れる — 前回の Major 2 の核心が解消している。

一時変更の置き場 (`maui/tests/KsSettingsView.MauiHost/App.cs`) が実在し、作業ツリーに差分が残っていないことも確認した。論点 2 も導線・A/B・バイト同一の理由がそろっている。

## 指摘事項

### [🟡 Minor] concepts の「保証すること」で 2 文が句点なしに連結している

**該当箇所**: `kasane/concepts/android/api/android-native-host.md:164`

**問題点**: 現在の本文はこうなっている。

> …再 attach 時の Store 現在状態の取り込み直しにより失われない スクロール位置も同じ View 内の付け外しでは保たれる (View の作り直しをまたぐ保持は対象外)。

「失われない」の後の句点が落ちていて、独立した 2 文が空白で連結している。内容は正しいので誤読の実害は小さいが、concepts は読み物であり「保証すること」は箇条書きの短い断定が並ぶ節なので目に付く。

**推奨修正**: 「…により失われない。スクロール位置も…」と句点を補う。

## 所見 (対応不要)

### [🔵 Suggestion 1] 追加テストは検査の「値確定前」という性質までは固定していない

**該当箇所**: `maui/KsSettingsView.Maui.Tests/ImplicitStyleTests.cs:139-157`、`deviation.md`

上のミューテーション表のとおり、validateValue だけを旧形 (`_controller?.`) へ戻してもこのテストは通過する。`SetAccessoryView` が `KsAccessoryViewOwnership.Reassign` より先に `EnsureAccessoryViewIsNotPlaced` を呼ぶため、propertyChanged 経由でも同じ `InvalidOperationException` が同じタイミングで出るためである。

つまり固定できているのは「暗黙 Style + 多重配置 → `InvalidOperationException` (NRE ではない)」までで、deviation が記録した「検査が構築より前にも走る = 値が確定する前に弾く」(`maui/ADR-0022` の決定 1) そのものではない。ただしこの差は暗黙 Style の経路からは観測しにくい — 検査が走らなかった場合に残る差は「公開値が確定してしまう」ことだが、コンストラクタが例外で抜けるためそのインスタンスに触れず、テストから見分けられない。

利用者から見える契約 (例外型) は守られているので追加対応は求めない。deviation の記述を「公開契約の例外型はテストで固定、値確定前という性質はコードの構造 (`Controller` が常に非 null) で担保」と 1 段はっきりさせておくと、将来 `?.` へ戻そうとした人が判断を誤らずに済む。

### [🔵 Suggestion 2] verification.md の論点 1 に修正前の実環境再現へのポインタがない

**該当箇所**: `verification.md` の「論点 1」節

冒頭で「修正前の証跡は exploration.md **論点 2** の実測表を参照」と断っているため、論点 1 の修正前再現 (ColorAnalyzer を実機で開いたときのクラッシュ、`exploration.md` 論点 1 の例外スタック) への参照が無い。`cross/runtime-behavior-verification.md` の 1 点目 (修正前に実環境で症状を再現) 自体は exploration の実機観測で満たされているので、記録が繋がっていないだけ。論点 1 節に「修正前の再現は exploration.md 論点 1 の例外スタック (ColorAnalyzer 実機)」と 1 行足すと、蒸留時に前後がそろって読める。

## 確認して問題が無かった観点

- **前回 APPROVED 相当と判断した範囲の再確認**: 実装コード (`SettingsView.cs` / `KsSettingsView.kt`) の差分は前回から実質変わっていない (Android は KDoc への前提追記 4 行のみ)。遅延生成の再入・二重生成が無いこと、暗黙 Style 由来の Theme / ListStyle / Root accessory が初回 Connect と `ApplyHostViews` で配信されること、Android のアンカー寿命と元の目的 (空表示回避) が保たれていることの結論は変わらない
- **KDoc 追記の正しさ**: 「`LinearLayoutManager` は保留状態を消費するレイアウトで item 数が 0 だとそれを捨てる」は前回こちらが指摘した内容と一致し、続く「空の adapter を当ててから復元する形にはできない」も実装の順序 (adapter 復帰 → アンカー復元 → pending Store の取り込み直し) と整合する
- **comment-policy**: 追記した KDoc・テストの doc コメントに作業文書パス・change 識別子・論点番号・デルタスペック構文キーワードの混入は無い。追加テストは非公開クラス配下で公開 doc コメントの規約に触れない
- **パスの書き方**: `verification.md` / `deviation.md` / 更新後の agenda はいずれも change 相対またはリポジトリ相対で、ローカル絶対パスは無い (local-path-lint 0 件)。agenda が証跡を参照する箇所に「archive 後は媒体が削除されるので exploration.md の実測表を参照」と添えてあるのは、`distill.archive-media: delete` を踏まえた良い配慮
- **足場の書き換え**: `exploration.md` の差分は前回から変化なし (探索フェーズの追記のみ)

## アクションプラン

1. (Minor) `kasane/concepts/android/api/android-native-host.md:164` の「失われない」の後に句点を補う
2. (任意) Suggestion 1 / 2 の 1 行追記。いずれも蒸留時の読みやすさのためで、実装・テストの変更は不要
