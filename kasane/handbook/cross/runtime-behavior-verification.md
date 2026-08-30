---
kind: rule
applies-when:
  always: false
  tasks: [実行時挙動の不具合調査, 不具合修正の完了判定]
title: 実行時挙動の検証規約
description: 実行時挙動 (IME・フォーカス・アニメーション・タイミング) が絡む不具合修正を「完了」と判定する条件 — 実環境での再現確立と修正後の同一手順による解消確認、および iOS 基本 Cell Sample の目視確認項目
timestamp: 2026-08-29
---

# 実行時挙動の検証規約

この文書は、実行時挙動が絡む不具合修正の**完了判定**を定める。読むと、どの種類の不具合でユニットテストだけの検証が「完了」にならないか、代わりに何を確認するかが分かる。

## 規約

実行時挙動 — IME・フォーカス・アニメーション・スレッドやフレームのタイミング・OS サービスとの連携 — が絡む不具合の修正は、次の 3 点を満たすまで完了と報告しない:

1. **修正前に実環境で症状を再現する。** 実環境とは実機・エミュレータ/Simulator と実物の依存先 (実 IME・実キーボード操作等) の組み合わせを指す。再現できた操作手順が、そのまま解消確認の手順になる
2. **修正後に同一手順で解消を確認する。** 可能なら修正前後の A/B (修正を外したビルドで症状が出る / 入れたビルドで出ない) まで取ると、修正が原因に効いた証明になる
3. **証跡 (スクリーンショット・ログ等) を change 配下に残す。** レビューと蒸留が「解消した」の主張を検証できる形にする

原因分析の段階でも同じ規律を適用する: **コード読解だけで原因仮説を「真因」と断定しない**。仮説は実環境の観測 (再現・計測・A/B) で裏取りしてから修正に進む。

## なぜ

ユニットテストは「実装した通りに動く」ことしか守れない。原因仮説そのものが誤っていれば、仮説に沿ったテストが全て green でも不具合は 1 ミリも直らない。実行時挙動は Robolectric や macOS 上のテスト環境では再現しない要素 (実 IME の composing、ItemAnimator の実挙動、フレーム間タイミング等) を含むため、テスト環境の green は実機の動作を保証しない。

## 適用範囲

- **対象**: 症状が実行時にしか現れない不具合。目安は「ユニットテストでその症状自体を再現できるか」— できないなら本規約の対象
- **対象外**: 純ロジックの不具合 (ユニットテストで症状を再現できるもの)。この場合は失敗するテストの作成が再現手順を兼ね、テストの green が解消確認になる

ユニットテストが不要になる規約ではない。実環境の再現確認に**加えて**、修正の意図を固定する回帰テストは通常どおり書く。

## iOS Basic Cell Sample の目視確認

`samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift` を Simulator または実機で起動し、Sample Theme と基本 Cell の統合状態を次の観測点で確認する。これは色値一覧を正典化するものではなく、Theme の値は `samples/ios/KsSettingsViewSample/SampleTheme.swift`、画面の文言と構成は `SampleScreen.swift` および画面実装が正である。

| 観測点 | 確認する結果 |
|---|---|
| Sticky Footer | RadioCell Section の footer `You can select either TypeA or TypeB.` が画面下端へ固定されず、content とともにスクロールアウトする |
| canvas と Cell 背景 | Section 間と Header / Footer 領域にベージュ系の canvas 背景が描かれ、白い Cell 背景とは別の二層として見える |
| Header / Footer の空領域 | Header text や Footer text がない Section（CommandCell / LabelCell / SwitchCell / CheckboxCell / SimpleCheckCell / ButtonCell の Footer など）に不要な余白が生じない |
| separator inset | Section 先頭・末尾の境界線は全幅、Section 内の線は icon ありで 52pt、icon なしで 16pt の leading inset を持つ |
| icon | CommandCell の `Tanaka Taro` に `person.crop.circle`、LabelCell の `Storage` に `externaldrive` の SF Symbols が描画される |
| 順序と文言 | CommandCell → LabelCell → SwitchCell → CheckboxCell → RadioCell → SimpleCheckCell → ButtonCell の順で、title / description / valueText と RadioCell footer が Android Sample と一致する |

この確認を不具合修正の完了証跡に使う場合は、上の一般規約どおり修正前後で同じ操作と観測点を使う。

## 出典

fix-entrycell-ime-composition (2026-08-01): Android EntryCell の日本語 IME 即時確定不具合で、コード読解のみから原因を断定して修正し、ユニットテスト全 green・独立レビュー APPROVED まで通したが、オーナー実機確認で症状が全く変わっていないことが判明した (真因は別箇所)。再修正では修正前ビルドで症状を再現 → 修正後ビルドで解消をスクリーンショット証跡付きで確認してから完了報告し、この規約の原型となった。

## 関連

- [test-execution.md](test-execution.md) — 「実行件数の確認までが検証」— テスト実行そのものに潜む同系の落とし穴
- [local-development-setup.md](local-development-setup.md) — Sample を開いて実行するまでの環境設定と手順
