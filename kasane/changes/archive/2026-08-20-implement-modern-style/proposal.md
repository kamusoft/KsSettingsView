# Proposal: implement-modern-style

## Why

`KsSettingsViewStyle.Modern` は現在骨組みだけの状態にある。iOS は list appearance を `.insetGrouped` へ切り替えるのみで余白・角丸を UIKit 任せにしており制御手段がなく、Android は `ModernSectionDecoration` の寸法 (縦 12dp / 左右 16dp / 角丸 12dp) が private 定数のハードコードで、セクション内の separator も描いていない。iOS 設定画面風の「ボックス型セクション」を完成形として提供し、その視覚属性を利用者が制御できるようにする。

制御対象は Section Margin (上下左右の余白)・Section Border Radius (角丸半径)・Section Border Width (ボーダー幅)・Section Border Color (ボーダー色) の4属性。iOS の実現方式は `.insetGrouped` を廃した自前 Section 装飾とする (ios/ADR-0003 accepted — 4属性の制御可能性を OS 外観自動追従より優先)。

## What Changes

- **core (styling 契約)**: Theme に4属性 (`sectionMargin` / `sectionCornerRadius` / `sectionBorderWidth` / `sectionBorderColor`) を追加。型は platform native、未指定時の既定は style ごとの platform 既定。`sectionMargin` は Classic にも上下成分のみ適用 (左右は無視、既定 0 で現行外観不変)。Section Header / Footer は箱の外側 (両 OS 共通)。Modern の separator は「箱の上下端なし・中間は箱の内側左端から 16pt/16dp・icon 非依存」
- **settings-view-ios-ui**: Modern を `.insetGrouped` から compositional layout 上の自前装飾 (section contentInsets + 角丸・ボーダーの decoration 描画) へ置換。`itemSeparatorHandler` に style 分岐を追加。Theme 4属性を公開
- **settings-view-android-ui**: `ModernSectionDecoration` のハードコード寸法を Theme 4属性から解決。箱から Section Header / Footer 行を除外。セクション内の中間 separator 描画 (`onDrawOver`) とボーダー描画を追加。`ClassicSectionDecoration` に上下 margin 適用を追加
- **samples-ios / samples-android**: Classic / Modern を切り替えて4属性を確認できるデモの追加または既存デモの拡張
- テスト: 上記の契約 (既定値解決・style 切替・separator 規則・Header/Footer 除外) の検証を追加

## Non-Goals

- MAUI への伝搬 (style 公開 API・BindableProperty・Bridge) — maui-support ロードマップ phase-11-modern-style で後続対応
- Section 単位の4属性上書き (将来拡張の余地のみ残す。今回は Theme の全体一括設定だけ)
- Header / Footer の箱の内外を選ぶトグル (要件外。必要なら別変更)
- 本物の iOS 設定画面風の icon 連動 separator inset (Classic の「icon 非依存」文法との一貫性を優先)
- 既定値の platform 間統一 (既定は platform native を維持 — list-appearance.md の意図を継続)

## Impact

- **公開 API**: Theme への4属性追加 (iOS / Android)。すべて既定値付きの追加のため、既存利用コードはソース互換
- **視覚挙動の変更 (Modern 利用者に影響)**: iOS は `.insetGrouped` 廃止により OS 描画から自前描画へ変わる (見た目の微差が生じ得る)。Android は Section Header / Footer が箱の外に出る (現行は箱の中)。Modern はサンプルでも未使用の骨組み段階のため、実利用への影響は限定的と判断
- **Classic への影響**: `sectionMargin` 既定 0 のため現行外観は不変
- **リスク**: iOS の layout 置換に伴う glitch (既存コメントにも `setCollectionViewLayout` 同期差し替えの注意あり)。Modern 時の Cell 自前背景と角丸の干渉。decoration への Theme 値受け渡し方式。→ design.md で扱う

## 級: L

複数能力横断 (styling 2概念 + iOS / Android) + iOS 描画機構の置換 (ADR-0003) + 公開 API 追加 + UI あり。

domain: cross
