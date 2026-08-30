---
id: 0008
title: MAUI 公開 API は AiForms 互換命名を基本とし、現行コア契約に無い機能は互換提供せず Native から再設計する
status: accepted
date: 2026-08-07
---

## Context

`KsSettingsView.Maui` の BindableObject 階層 (SettingsView / Section / CellBase) は原典 AiForms.Maui.SettingsView を参考にするが、完全 API 互換は保証しない方針である。公開面の突き合わせ棚卸しの結果:

- Theme / CellStyle / 共通 Cell 属性に相当する公開プロパティは、camelCase 化のみで現行コア契約とほぼ 1:1 に対応する
- 現行仕様に存在しない機能 (ドラッグソート、スクロール制御 API、Android 専用の表示切替、非同期画像ロード等) が一定数ある
- 変更通知の設計思想が異なる (AiForms は INotifyCollectionChanged / INotifyPropertyChanged の集約、現行は値 + 明示 Diff)

## Decision

- 現行コア契約に対応概念がある公開面は **AiForms の命名を踏襲**して公開する。ただし AiForms の命名自体が非対称な箇所は対称性を優先して改める — Section の header テキストは `Title` ではなく **`HeaderText`** とし、`FooterText` との対称対で公開する (Native の `Section.header` / `footer` (SectionAccessory) は元々対称で「title」という語を持たないため、Native 側の変更は不要)
- Font 系は MAUI 慣例に従い **FontFamily / FontSize / FontAttributes の分割公開**とし、facade が native の統合 font 記述子へ合成する ([ADR-0004](0004-maui-idiomatic-types-for-styling.md) の具体化)
- `Tapped` を CellBase の共通イベントにしない。現行契約の「LabelCell は操作 control を持たない」と衝突するため、タップ通知は対応概念を持つ Cell (CommandCell / ButtonCell の onTap 等) にのみ個別に公開する
- `ItemsSource` / `ItemTemplate` (SettingsView 直下の Section 生成・Section 配下の Cell 生成) は MAUI 層の機能として踏襲する。テンプレートから生成した Section / Cell は通常の構造操作として Diff 変換経路へ流し、**Native 側にテンプレート概念を持ち込まない**
- コンテナ形状 (SettingsView.Root と Section 直下への Cell 配置による XAML の書き味) は AiForms 同形を踏襲する
- 現行仕様に無い AiForms 機能は**互換 API として提供しない**。同種の機能が必要な場合は AiForms API の模倣ではなく **Native 側から再設計して提供する** (例: ドラッグソート、スクロール制御、Section header/footer の表示トグル)

## Alternatives Considered

- **完全 AiForms 互換 (全公開面を踏襲)**: 現行仕様に無い機能まで互換を負うと、Native に流れない MAUI 層だけの挙動や Native との二重実装が生じ、「現行コア契約が正」の前提が崩れるため却下
- **互換を捨てた新規 API 設計**: 命名がほぼ 1:1 で対応する以上、独自命名は移行性と学習容易性を損なうだけで得るものがないため却下

## Consequences

- 正: AiForms からの移行者は A 分類の公開面をほぼそのまま使える。現行契約との対応も 1:1 で追いやすい
- 正: Native にテンプレート概念や XAML 都合の機能が流れず、コア契約が汚れない
- 負: 完全互換ではないため、AiForms 利用コードの移植には書き換えが必要な箇所が残る (存在しない機能・イベント設計の違い)
- 負: 分割 font → 統合 font の合成規則を facade が持つことになり、未指定時の既定値解決に設計が必要
- 負: 互換提供しない機能を Native から再設計する場合、その都度コア契約側の拡張が必要になる

---
出典: kasane/roadmaps/maui-support/phases/phase-2-maui-core/artifacts/2026-08-06-aiforms-surface-inventory.md / kasane/roadmaps/maui-support/phases/phase-2-maui-core/history.md (2026-08-07: AiForms 互換 BindableObject 階層の踏襲範囲)
