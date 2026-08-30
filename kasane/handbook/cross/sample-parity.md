---
kind: rule
applies-when:
  always: false
  paths: ["samples/**"]
  tasks: [Sample のデモ画面・文言の追加・変更]
title: Sample のプラットフォーム間一致
description: Sample アプリは全 platform で同一の文言・画面構成で実装し、プラットフォーム間検証の装置として機能させる規約
timestamp: 2026-08-29
---

# Sample のプラットフォーム間一致

この文書は、`samples/` 配下の Sample アプリをプラットフォーム間でどこまで一致させるかの規約を定める。読むと、Sample に画面や文言を追加・変更するときに何を揃えなければならないか、どんな差異なら許されるかが分かる。[リポジトリとビルドの責務境界](../../concepts/cross/architecture/repository-boundaries.md) を先に読むと、Sample が何を保証する装置かが分かりやすい。

## 目的

KsSettingsView はプラットフォーム間で仕様・動作を統一することを製品目的とする。Sample はその統一が実際に成立しているかを実行・目視で確認する検証装置を兼ねる。Sample 自体がプラットフォーム間でばらばらだと、画面上の差異が「本体の仕様差 (= バグ)」なのか「Sample の書き方の差」なのか判別できず、検証装置として機能しない。そこで Sample は**全 platform で一字一句同じ文言・同じ画面構成**で実装する ([cross/ADR-0016](../../decisions/cross/0016-sample-cross-platform-parity.md))。

## 保証すること

一致の単位は「対応するデモ画面」。画面同士の対応は画面タイトル (= ルートメニューの文言) で取る。すべての platform で以下を揃える。揃っていないと、目視検証が platform 間比較として成立しない (対象は `samples/ios` / `samples/android` / `samples/maui` の3つ。現時点でパリティ対象のデモ画面は全 platform に揃っている — 最後まで残っていた CustomCellDemo も MAUI 側が追随済みで、MAUI にしかない挙動は別画面「CustomCell の MAUI 固有デモ」へ分けて下記の例外に置いた):

- **画面の集合**: デモ画面とルートメニュー項目は全 platform に同一の構成で存在する
- **表示文言の完全一致**: 画面タイトル・メニュー項目・Section の header / footer・Cell の表示フィールド (title / description / hintText 等 — [基本 Cell](../../concepts/core/cells/basic-cells.md) 参照)・デモデータ (初期値・選択肢の文言) は一字一句一致させる。大文字小文字の違いや「〜デモ」の有無のような表記ゆれも不一致とみなす
- **画面構成の一致**: Section 数・Cell 数・並び順と、各 Cell に渡すパラメータ (例: 選択肢の数、min / max / step / unit、初期値、初期選択、maxSelectedNumber、`accentColor` 等の色) を一致させる。**色に platform 固有の semantic color (iOS の `UIColor.systemXxx` など) を使わない** — 実値が platform 間でずれるため、同一の RGBA を Sample 共通の定義 (`SampleTheme`) に置いて両 platform から参照する。dark mode 追随のような platform らしさより一致を優先する
- **メニューと画面タイトルの一致**: ルートメニューのリンク文言と遷移先画面自身のタイトルは同一文字列にする (別々に定義される二重管理が表記ゆれの主要因)

模範例: isVisible デモ (`samples/ios/KsSettingsViewSample/VisibilityDemoView.swift` と `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/VisibilityDemoScreen.kt`) は全文言・構成が完全に一致している。

## 許容される差異

- OS 標準のナビゲーション chrome や既定フォント・描画差など、platform の見た目そのもの (例: iOS は `NavigationStack`、Android は `TopAppBar` を持つ共通ラッパーで画面を包む)。これらはむしろ「同じ宣言内容が platform らしく描画される」ことの確認対象
- 本体公開 API の platform 命名差に由来する、画面文言に出ないコード上の差 (引数名・型名・リソース指定方法)
- Sample が明示的に渡していないパラメータの、**本体既定値の platform 差** (例: `titleColor` 未指定の ButtonCell が iOS では青、Android では Material 3 の紫で描画される)。Sample 側で色を明示すると「既定色のデモ」という意図が壊れるため解消できない。規約違反としては扱わないが、本体の統一課題として deviation.md に記録して追跡する
- **実装順序による一時的な片側先行**。change が platform 別に分かれる場合など、先行 platform だけにデモ画面・文言変更が存在する期間は許容する。本規約が要求する一致は**収束状態**であり、常時同時である必要はない。ただし恒久化させないこと — 未追随 platform への追随を tasks / 後続 change として残し、追跡できる状態を保つ (追跡が切れた片側限定の差異は、この規約の違反になる)

## 例外: デモ対象の公開 API が存在しない platform

デモ画面の一致対象は、**デモ対象の公開 API がその platform に存在する場合に限る**。存在しない platform には当該画面を作らない (作れない)。例: Store 直接操作デモ・宣言 DSL デモは native 2 platform (iOS / Android) のみの画面である — MAUI は Store / DSL を公開しておらず (facade 契約が Binding assembly 型の直接使用を禁じている)、デモ化は公開契約との自己矛盾になる。逆方向 (MAUI にしか対応概念がない `ItemsSource` / `DataTemplateSelector` 等のデモ) も同様に MAUI のみの画面としてよい。この例外に該当する画面は片側先行の追跡対象に数えない。

この例外は、確認項目の一部に他 platform の対応概念が存在する場合にも適用を拡張する — **デモの主対象がその platform 固有の公開 API と意味論**である場合は、ページ全体を platform 固有画面としてよい (オーナー裁定 2026-08-11)。適用例: MAUI の `AccessoryViewsDemoPage` は表示・高さ挙動に native の対応概念があるが、主対象は VisualElement 埋め込み・text/View 併存の優先解決・Handler lifecycle 復元という facade 固有の意味論のため、MAUI のみの画面とする。`CustomCellMauiSpecificDemoPage` も同様で、CustomCell そのもののデモ (`CustomCellDemoPage`) は native と一致させたうえで、`Content` の差し替えと null 遷移・`ItemTemplate` 生成行の独立性・再接続の復元といった facade 固有の意味論だけをこちらへ分けている。`MauiSpecificCellFeaturesDemoPage` (「MAUI 固有 Cell 機能デモ」) は Cell 1 種に閉じない facade 固有契約 (バインド可能な選択完了 command、選択要素列そのものの TwoWay) を集める Cell 横断の器で、以後の facade 固有機能のデモもここへ同居させられる。platform 固有画面はルートメニュー上で「MAUI 固有」のような区分に置き、パリティ対象のデモ画面と明確に区別する。

## 例外: platform 固有の技術検証画面

iOS の `MinimalDiffableDemoView` (KsSettingsView を経由しない生 UIKit の動作検証) のような platform 固有の技術検証画面は、一致の対象外として許容する。ただし:

- ルートメニュー上でデモ画面と明確に区別する (「検証」など、ライブラリのデモとは別物だと分かる表記にする)
- 他 platform への移植義務は負わないが、デモ画面の集合には数えない

## してはいけないこと

- 片側だけの文言・構成・デモデータの変更を**追跡なしで放置しない**。同一 change 内で全 platform を揃えるのが原則だが、実装順序の都合で片側が先行する場合は、追随の予定を残す (進行中の変更ディレクトリ `kasane/changes/<change-id>/` の tasks.md / deviation.md — 実装が仕様・計画から逸れた点を記録するメモ — や後続 change)。追跡が残っている一時的な不一致は違反ではない
- platform ごとに独自の「改善」(Section の統合、プレビュー UI の追加、選択肢の増減) をしない。改善は全 platform 一斉に行う
- 本体公開 API の platform 差で一致が不可能な箇所を黙認しない。一致できない理由を同じく deviation.md に記録し、本体側の統一課題として扱う
- この規約を製品契約と混同しない。[リポジトリとビルドの責務境界](../../concepts/cross/architecture/repository-boundaries.md) の通り、Sample の表示文字列やデモデータは利用者向けの製品契約ではない (変更しても breaking change ではない)。本規約は「platform 間で互いに揃える」内部の検証規約であり、両者は別軸で両立する

## 関連

- [リポジトリとビルドの責務境界](../../concepts/cross/architecture/repository-boundaries.md) — Sample が利用者側から本体を参照する境界と、製品契約にしない範囲
- [cross/ADR-0016](../../decisions/cross/0016-sample-cross-platform-parity.md) — Sample をプラットフォーム間検証装置と位置づける決定
