# Proposal: add-accessory-visibility-toggle

## Why

Section の Header / Footer を内容を保持したまま隠す手段が無い。現行契約は「表示しない」を accessory の中身の不在 (nil / 空 text) で表現しており、一時的な非表示は accessory の退避・復元を利用者に強いる。原典 AiForms の `FooterVisible` 相当を Native 起点で再設計して MAUI まで通す (roadmap maui-support のゴール項目)。設計判断はフェーズ議論でオーナー承認済み — agenda 決定 (論点1〜4、kasane/roadmaps/maui-support/phases/phase-9-accessory-visibility/agenda.md) と [core/ADR-0023](../../decisions/core/0023-accessory-visibility-and-composition.md) (proposed、accepted への昇格は蒸留時)。

## What Changes

- **settings-view-ios-ui**: `Section` に `isHeaderVisible` / `isFooterVisible` (既定 `true`、値等価性に参加 — 手動 `==` へ追加)。表示判定を「トグル && 内容あり」の AND 合成へ (`supplementaryModes` / `makeHeaderBoundaryItem` / `shouldShowFooter` の3箇所)。対称化: header 不在時の `Section.headerHeight` / `Theme.headerHeight` による supplementary 生成を廃止 (高さ解決は存在判定の後。逆契約を固定していた既存テストは反転)。「内容の不在 = nil または空 text」を header / footer で統一。`Section` 手動再構築箇所でのトグル保持。SwiftUI 宣言 DSL (`ksSection` 等) のトグル引数と DSL 差分検出への織り込み + Store/DSL 対称テスト (core/ADR-0018)
- **settings-view-android-ui**: `Section` (data class) に同フィールド追加。`flatten` に AND 判定を織り込み。対称化: 空文字 text accessory (header / footer) を非表示へ (iOS へ揃える)。Compose 宣言 DSL (`DSLScope.Section`) のトグル引数と DSL 差分検出への織り込み + Store/DSL 対称テスト
- **maui-bridge**: `KsBridgeSection` (iOS / Android) に `isHeaderVisible` / `isFooterVisible` フィールド追加、`makeSection` で core `Section` へ伝搬。iOS Binding の `ApiDefinition.cs` へ 2 プロパティ追加 (managed API 生成)。専用 bridge 操作は追加しない (replaceSection 相乗り)
- **maui-core**: facade `Section` に BindableProperty `IsHeaderVisible` / `IsFooterVisible` (既定 `true`)。PropertyChanged は `IsVisible` / `HeaderHeight` と同じ ReplaceSection バッチ分岐へ相乗り
- **samples**: 3 platform の既存 Visibility デモ画面へ Header / Footer トグルのデモを追加 (sample-parity 準拠・同一文言・同一構成)

## Non-Goals

- Root Header / Footer の表示トグル (対象は Section のみ。Root は設定・解除で足りる)
- 専用の Diff / Store / Bridge 操作 (replaceSection 相乗りで足りる。ヘビーな用途が出たら後付けは可逆 — 論点2)
- 「内容が無いが領域だけ出す」表現 (view accessory + 高さ指定で代替 — ADR-0023 の却下案)
- `headerHeight` 系の高さ契約の変更 (core/ADR-0021 のまま。本 change は存在判定との適用順序のみ規定)

## Impact

- 公開 API は追加のみ。**非空の accessory を持つ既存コードでは、トグル未指定 (既定 `true`) の表示挙動を維持する** — 挙動不変の主張はこの範囲に限る
- **公開挙動の変更 (対称化、ADR-0023 の帰結。breaking として明示)**:
  1. Android: 空文字 text accessory (header / footer) が非表示になる
  2. iOS: header 不在 (nil / 空 text) + `Section.headerHeight` 正値で supplementary が生成されなくなる (逆契約を固定していた既存テストを反転)
  3. iOS: header nil + `Theme.headerHeight` 正値で supplementary が生成されなくなる
  - 対称化のための挙動変更は core/ADR-0021 の前例に倣う。text なしの spacer 用途は view accessory + 高さ指定で代替する
- `Section` の値等価性にフィールドが参加するため、iOS の手動 `==` / `hash` の更新漏れに注意。iOS は `Section` を手動再構築する箇所が多く、トグルの保持漏れ (暗黙の `true` 戻り) が主要リスク — 保持要件と回帰テストで固定する
- 宣言 DSL (SwiftUI / Compose) の構築 API と差分検出への織り込みが必要 (Store/DSL 対称性は core/ADR-0018 の対称テスト義務)
- リスク中: 判定の織り込みと各層の配管は先例経路 (`isVisible` / `headerHeight` と同型) だが、上記の再構築保持と DSL 差分検出が注意点

## 級: M

公開 API の小変更 (bool 2個の配管) で、設計判断は ADR-0023 に集約済み。複数 capability に触れるが全層が同一概念の薄い伝搬であり、L 相当の design.md は ADR-0023 の重複になるため M と判定。spec-review (second-opinion-spec-001) で宣言 DSL 経路が加わった後も、追加分は同じ bool の配管で新規の設計判断を伴わないため M 維持 (オーナー再確認済み 2026-08-19)。ui/ は作成しない — トグルは既存要素の表示・非表示でデザインすべき固有の見た目が無いため (オーナー承認済み、先行 MAUI 系 change と同型)。

domain: cross
roadmap: maui-support/phase-9-accessory-visibility
