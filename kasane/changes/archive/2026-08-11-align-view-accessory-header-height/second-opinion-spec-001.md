# セカンドオピニオン: align-view-accessory-header-height (spec-001)
**相方**: codex / **日付**: 2026-08-11 / **対象**: 提案一式 (proposal.md / specs/settings-view-android-ui/spec.md / tasks.md)
---
# レビュー結果: align-view-accessory-header-height

**日付**: 2026-08-11  
**判定**: **NEEDS_DISCUSSION**

## サマリー

基本方針は現行 iOS 実装と整合していますが、Android の `headerHeight` 動的更新経路が提案から漏れています。単純に再 bind を追加すると `AndroidView` の内部状態を失う可能性があり、仕様判断が必要です。

指摘件数: Critical 0 / Major 2 / Minor 2 / Suggestion 0

## 指摘事項

### [🟠 Major] View accessory の動的な高さ変更が仕様・タスクから漏れている

**該当箇所**: [spec.md:7](kasane/changes/align-view-accessory-header-height/specs/settings-view-android-ui/spec.md:7)、[tasks.md:5](kasane/changes/align-view-accessory-header-height/tasks.md:5)

**問題点**:  
Scenario は初期表示と ViewHolder 再利用だけを扱い、同一 View accessory の `headerHeight` が `ReplaceSection` / `Full` で変更された場合を定めていません。

現行 `CellListItemDiffCallback` は View accessory の高さ差を明示的に無視しています（[KsSettingsListAdapter.kt:354](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsListAdapter.kt:354)）。その挙動はテストでも固定されています（[ListAdapterDiffTest.kt:141](android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ListAdapterDiffTest.kt:141)、[FullUpdateContentSyncTest.kt:271](android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/FullUpdateContentSyncTest.kt:271)）。

したがって、現在の tasks どおり `bind()` と adapter の引数伝搬だけを変更しても、表示済み Header の高さは更新されません。一方、単純に DiffCallback を不等価へ変えると、`KsAnyView.AndroidView` は再 bind 時に子 View を factory から作り直すため（[SectionAccessoryViewHolders.kt:336](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SectionAccessoryViewHolders.kt:336)）、高さだけの変更で入力状態などを失う可能性があります。

**推奨修正**:

- `WRAP_CONTENT → 固定値`、`固定値 → WRAP_CONTENT`、`固定値 A → B` の動的変更 Scenario を追加する。
- 高さだけの変更で hosted View を再生成するか、同一 View インスタンスを維持するかを契約として裁定する。
- 状態維持を選ぶ場合は、高さ専用 payload または内容を再 bind しない高さ更新経路を tasks に追加する。
- 上記の現行テストを新契約へ置き換えるタスクと、関連コメントの更新を明記する。
- proposal の「変更点は bind 時の1箇所」というリスク評価（[proposal.md:26](kasane/changes/align-view-accessory-header-height/proposal.md:26)）も修正する。

### [🟠 Major] 視覚的な clipping の受け入れ基準と UI アーティファクトが不足している

**該当箇所**: [spec.md:9](kasane/changes/align-view-accessory-header-height/specs/settings-view-android-ui/spec.md:9)、[tasks.md:11](kasane/changes/align-view-accessory-header-height/tasks.md:11)、[proposal.md:28](kasane/changes/align-view-accessory-header-height/proposal.md:28)

**問題点**:  
「内容が固定高さより大きい場合は clip」が公開される観察可能な結果ですが、tasks は固定高さのテストとしか記述しておらず、`layoutParams.height` の設定だけで Scenario を満たしたことにできてしまいます。子 View が実際に境界外へ描画されないことは、それだけでは検証できません。

また、本変更は M 級の UI 挙動変更ですが、変更配下に必須の `ui/` アーティファクトがありません。

**推奨修正**:

- 固定高さより大きな AndroidView/Compose 内容を使い、親境界と clipping 条件を検証するテストを明記する。
- Robolectric で実描画を保証できない場合は、実環境スクリーンショットによる視覚照合を検証タスクへ追加する。
- `ui/brief.md`、承認対象、実装後の `ui/verification/` を用意し、自動高さ・固定高さ・oversized content の状態を記録する。

### [🟡 Minor] Section 値が Theme 値へ勝つことを Scenario が識別できない

**該当箇所**: [spec.md:7](kasane/changes/align-view-accessory-header-height/specs/settings-view-android-ui/spec.md:7)、[spec.md:9](kasane/changes/align-view-accessory-header-height/specs/settings-view-android-ui/spec.md:9)

**問題点**:  
優先順位は Requirement に書かれていますが、View accessory の Scenario は「Section のみ正値」と「Section=-1、Theme 正値」に分かれています。Section と Theme の両方が正値の場合に Theme を誤って優先する実装でも、現在の全 Scenario を通過できます。

**推奨修正**:  
View accessory について `Section.headerHeight` と `Theme.headerHeight` の両方を異なる正値にし、Section 値が採用される Scenario とテストを追加してください。iOS の対称テストにも同条件を含めると契約を両端で固定できます。

### [🟡 Minor] proposal の domain が変更範囲と一致していない

**該当箇所**: [proposal.md:32](kasane/changes/align-view-accessory-header-height/proposal.md:32)、[rules.md:16](kasane/concepts/rules.md:16)

**問題点**:  
変更は Android 実装と iOS テストの複数ドメインに触れますが、`domain: core` になっています。プロジェクト規約では複数ドメインを変更する proposal は `cross` です。現状ではプラットフォーム固有スキルの解決にも影響します。

**推奨修正**:  
proposal の domain を `cross` に変更してください。蒸留される高さ契約や ADR 自体の配置先は、内容に従って `core` のままで問題ありません。

## アクションプラン

1. 高さだけの動的変更時に hosted View の状態を維持するか裁定する。
2. 動的変更 Scenario、DiffCallback/payload 対応、既存の反対契約テスト更新を追加する。
3. clipping の検証方法と `ui/` アーティファクトを追加する。
4. Section/Theme 同時指定の優先順位 Scenarioを追加する。
5. proposal の domain を `cross` に修正する。

制約に従い、ビルド・テスト実行およびファイル書き込みは行っていません。

## 突き合わせ結果 (2026-08-11)

ホスト側自己レビュー (2周・指摘なしで通過) と突き合わせ。相方指摘は4件すべて「相方のみ」:

- **Major-1 (動的高さ変更の漏れ)**: **採用** — 根拠強 (DiffCallback `isSameHeaderHeight` が View accessory の高さ差を意図的に無視 + 固定テスト2本をコードで確認)。契約裁定はオーナー判断で「payload 方式 (hosted view 維持・高さのみ更新)」を採用。spec に Requirement + Scenario 4本、tasks に 1.4/1.5/2.4 を追加、proposal のリスク評価を修正
- **Major-2 (clip 検証 + ui/ 不足)**: **採用** — clip 視覚検証タスク (3.3) を追加。ui/ は前例 (fix-cell-accessory-vertical-fill 等) が規約運用を裏付け、brief + mock 1案 (mock-variants: 2 への例外理由を brief に明記、オーナー承認) で追加
- **Minor-1 (Section > Theme 優先の Scenario 不足)**: **採用** — Scenario とテスト言及を追加
- **Minor-2 (domain: cross)**: **採用** — rules.md「複数ドメインに触る proposal は cross」を確認、修正。ADR の配置先は蒸留時に内容で判定 (core 想定) のまま

採用 4 / 降格 0 / 未解決 0。判定 NEEDS_DISCUSSION の論点 (動的高さの契約) はオーナー裁定で解消済み。
