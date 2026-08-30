# セカンドオピニオン: fix-picker-dialog-recreation (001 = 提案の spec-review)
**相方**: codex / **日付**: 2026-08-03 / **対象**: 提案一式 (proposal / specs / tasks)
---
## 判定

**NEEDS_DISCUSSION**

Critical 1件、Major 4件、Minor 3件です。特に Fragment の所有者識別がないまま実装すると、別 `KsSettingsView` の Cell へ値を通知し得ます。実装開始前に仕様修正が必要です。

静的レビューのみ実施し、ビルド・テスト・ファイル変更は行っていません。

## 指摘事項

### Critical — 複数 KsSettingsView で別 Cell へ誤発火し得る

**該当箇所**: [proposal.md:28](kasane/changes/fix-picker-dialog-recreation/proposal.md:28)、[spec.md「対応付け不能時の dismiss フォールバック」:52](kasane/changes/fix-picker-dialog-recreation/specs/settings-view-android-ui/spec.md:52)、[tasks.md:17](kasane/changes/fix-picker-dialog-recreation/tasks.md:17)

**問題点**: Fragment tag が Cell 種別と `cell.id` しか持たず、所有する `KsSettingsView` を識別できません。各 View が同じ FragmentManager 全体を走査するため、複数 View の root に同種・同一 ID の Cell があれば、同じ Fragment に複数の OK リスナーが登録され、確定時に両 Cell が更新され得ます。

これは「誤った Cell への発火は起きない」という明示的保証に反します。Compose DSL の ID は固定名前空間と構造から決定されるため、同型の2画面では実際に同じ ID が生成され得ます（[DeclarativeDSLIdentity.kt:51](android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DeclarativeDSLIdentity.kt:51)）。

**推奨修正**: Fragment tag または saved state に、Activity 再生成をまたいで安定する `KsSettingsView` 所有者キーを含めてください。代替として FragmentManager 単位で全 View を一括照合し、候補が一意でない場合は dismiss する仕様でも構いません。少なくとも以下を Scenario 化してください。

- 同じ ID を持つ2つの `KsSettingsView`
- 所有 View の Cell のみが1回通知される
- 所有者を一意に決められない場合は、どの Cell にも通知しない

### Major — `.r<n>` サフィックスと任意の cell.id を一意に解析できない

**該当箇所**: [spec.md「再生成後のピッカーダイアログの完全復元」:13](kasane/changes/fix-picker-dialog-recreation/specs/settings-view-android-ui/spec.md:13)、[tasks.md:5](kasane/changes/fix-picker-dialog-recreation/tasks.md:5)、[ADR-0011:25](kasane/decisions/android/0011-picker-dialog-rotation-restore-container-driven.md:25)

**問題点**: spec は「ID の文字列内容によらず成立」と保証していますが、task は末尾の `.r<n>` を剥がして ID を復元する設計です。世代なしの `id = "foo.r1"` と、`id = "foo"` の世代1は同じ表現になり、区別できません。

ドットを含む例だけではこの衝突を検出できません。

**推奨修正**: 長さプレフィックス、可逆エンコード、または別 Bundle フィールドなど、ID と世代を曖昧なく分離できる形式に変更してください。`foo.r1`、空文字、Unicode、複数世代をテスト対象に追加してください。

### Major — one-shot 走査時に Fragment の View が未生成だと復元を取り逃す

**該当箇所**: [proposal.md:11](kasane/changes/fix-picker-dialog-recreation/proposal.md:11)、[spec.md「復元走査の駆動条件」:68](kasane/changes/fix-picker-dialog-recreation/specs/settings-view-android-ui/spec.md:68)、[tasks.md:10](kasane/changes/fix-picker-dialog-recreation/tasks.md:10)

**問題点**: `KsSettingsView` の attach と root 反映が完了しても、復元 Fragment がまだ `view == null` の可能性があります。そこで「生成済み View への即時適用」だけを試して one-shot を消費すると、その後の `onFragmentViewCreated` を取り逃します。

反対に View 生成後なら、既存の `attach()` だけでは既に通過した callback を拾えません。また現在の Colorizer では View 階層の着色と window 背景の着色が別経路です（[TimePickerColorizer.kt:114](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerColorizer.kt:114)、[DatePickerColorizer.kt:129](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerColorizer.kt:129)）。「生成済み View」だけの即時処理では `backgroundColor` の復元を漏らす危険があります。

**推奨修正**: 「先に lifecycle hook を登録し、現在 View があれば即時適用も行う」という状態非依存の契約にしてください。即時適用には window 背景、View 階層、pre-draw 再適用、破棄時の解除を明記してください。View 未生成／生成済みの両方を Scenario とテストに追加すべきです。

### Major — 「対応する Cell」の適格条件が未定義

**該当箇所**: [spec.md「再生成後のピッカーダイアログの完全復元」:7](kasane/changes/fix-picker-dialog-recreation/specs/settings-view-android-ui/spec.md:7)、[spec.md「対応付け不能時の dismiss フォールバック」:52](kasane/changes/fix-picker-dialog-recreation/specs/settings-view-android-ui/spec.md:52)

**問題点**: 「同一 id の同種 Cell」だけでは次が決まりません。

- 同一 ID の `DatePickerCell` が `Spinner` に変更されている
- Cell が `isEnabled = false` または `isVisible = false`
- Cell を含む Section が非表示
- `onValueChanged = null`
- 同一 ID の Cell が root 内に複数ある
- `minDate` / `maxDate` などが回転前後で変わっている

特に callback は nullable なので、現記述の「`onValueChanged` が1回発火する」は有効な Cell 状態に対して満たせない場合があります。

**推奨修正**: 完全復元対象の適格条件を定義してください。たとえば「同一 ID・同一 picker variant・visible・enabled・候補が一意」などです。Cell 内容が変化した場合に、復元ダイアログの古い制約を維持するのか、dismiss／再構築するのかも決定し、それぞれ Scenario を追加してください。callback が非 null の場合だけ通知することも明記が必要です。

### Major — 検証計画がプロジェクトの実行時挙動検証規約を満たさない

**該当箇所**: [tasks.md「テスト」:20](kasane/changes/fix-picker-dialog-recreation/tasks.md:20)、[tasks.md「実機確認」:29](kasane/changes/fix-picker-dialog-recreation/tasks.md:29)、[runtime-behavior-verification.md:15](kasane/concepts/cross/conventions/runtime-behavior-verification.md:15)

**問題点**: tasks は修正後の目視確認しか要求していません。プロジェクト規約は、実行時不具合について以下を必須としています。

- 修正前の実環境での再現
- 修正後に同一手順で解消確認
- change 配下への証跡保存

また、以下の Scenario はテスト対応が不足しています。

- 作り直し世代を経た実ダイアログの Activity 再生成
- tag が `.r<n>` で終わる ID
- Fragment View 生成前／生成後の両タイミング
- 複数 `KsSettingsView`
- proposal が対応可能と述べるプロセス再生成

Task 1.2 の tag 単体テストだけでは「作り直し世代のダイアログが完全復元される」ことを判定できません。

**推奨修正**: 修正前後の同一操作、証跡保存先、上記統合ケースを tasks に追加してください。プロセス再生成を保証しないなら、[proposal.md:27](kasane/changes/fix-picker-dialog-recreation/proposal.md:27) の主張を削除または非保証事項へ移してください。

### Minor — 「既定ランダム ID では常に dismiss」は過剰な断定

**該当箇所**: [proposal.md:25](kasane/changes/fix-picker-dialog-recreation/proposal.md:25)、[ADR-0011:37](kasane/decisions/android/0011-picker-dialog-rotation-restore-container-driven.md:37)

**問題点**: ID は Cell 構築時に一度生成されます。同じ `SettingsRoot`／Store が再生成後も保持されていれば、明示 ID でなくても一致します。spec の Scenario は「アプリが Cell を再構築した」と限定しており、こちらが正確です。

**推奨修正**: 「Cell を再構築してランダム ID が変わる経路では dismiss」と修正し、安定性の条件を「明示指定か否か」ではなく「再生成前後で同じ ID か」に統一してください。

### Minor — 復元する「今日」操作の契約が履歴アーティファクト依存

**該当箇所**: [spec.md:11](kasane/changes/fix-picker-dialog-recreation/specs/settings-view-android-ui/spec.md:11)

**問題点**: 「datepickercell-today-shortcut のデルタスペックと同一」としていますが、archive 内のデルタ spec は現仕様の SSoT ではありません。また現在の Scenario は、範囲外時の no-op、代替表示状態、アクセシビリティ、操作自体では callback を発火しないことを直接固定していません。

**推奨修正**: 本変更で復元後にも維持すべき観察可能な契約を自己完結的に記載するか、最低限それらを Scenario とテスト対応に列挙してください。

### Minor — UI アーティファクト省略が Kasane の M 級 UI 変更規約と不整合

**該当箇所**: [proposal.md「UI アーティファクト」:30](kasane/changes/fix-picker-dialog-recreation/proposal.md:30)

**問題点**: 新規デザインではありませんが、復元後の配色と操作提示を受け入れ基準に含む UI 挙動変更です。Kasane の M 級 UI 変更では `ui/` が必要です。また、実行時検証規約が要求する証跡の置き場も現在ありません。

**推奨修正**: 新規 mock 不要という判断は維持して構いませんが、`ui/brief.md` に「通常表示を視覚基準とする」ことと mock 免除理由を記録し、修正前後および復元後のスクリーンショットを `ui/verification/` に残すタスクを追加してください。

## 優先アクション

1. Fragment の所有者識別と、複数 View 時の曖昧性処理を決定する。
2. tag を曖昧なく符号化できる形式へ変更する。
3. Fragment View 生成前後の両方を扱う復元契約を追加する。
4. Cell 適格条件と root 内容変更時の扱いを決める。
5. 実環境での修正前後検証・証跡を含むテスト計画へ改訂する。


## 突き合わせ結果 (2026-08-03)

ホスト側は自己レビュー2周 (指摘1件: 複数 KsSettingsView 構成の制約を検出し「dismiss への安全側劣化」として明文化済み) との突き合わせ。

| # | 指摘 | 採否 | 根拠・対応 |
|---|---|---|---|
| Critical | 複数 KsSettingsView で別 Cell へ誤発火 | **採用 (ユーザー裁定へ)** | ホスト側は「dismiss 劣化」までしか見ておらず、**同一 id が双方の root にあると両方が復元して二重発火する**経路 (DSL の構造由来 id は同型2画面で衝突し得る) は見逃し。spec の MUST NOT 保証に反するため設計判断をユーザーへ提示 |
| Major-1 | `.r<n>` サフィックス剥がしの曖昧性 (`"foo.r1"` 問題) | **採用** | 誤対応 → 誤発火に至り得る実害。tasks 1.1/1.2 を曖昧性のない符号化 (世代を id より前) + 追加テストケースに修正 |
| Major-2 | one-shot 走査時の Fragment View 未生成 / window 背景の適用漏れ | **採用 (一部降格)** | 状態非依存の再 attach 契約 + window 背景・pre-draw・解除の明記を tasks 2.1/2.2 に反映。「View 未生成」タイミング自体は attach 後の走査では起きにくく Scenario 化は見送り (テスト不能な投機的ケース) |
| Major-3 | 「対応する Cell」の適格条件未定義 | **採用** | spec R1 に適格条件 (同一 id・同型・一意) と非影響条件 (isEnabled / null callback / 構成値変化 = 表示時の構成を維持) を定義。R2 に uiStyle 変更・重複 id の dismiss を追加 |
| Major-4 | 検証計画が runtime-behavior-verification.md 規約を満たさない | **採用** | concept の実在を確認。tasks グループ5 を修正前再現 → 修正後解消 → 証跡 `ui/verification/` 保存に改訂。統合ケース (4.7) 追加。proposal のプロセス再生成の主張は非保証へ降格 |
| Minor-1 | 「既定ランダム id は常に dismiss」は過剰断定 | **採用** | proposal / (ADR) の条件を「再生成前後で同じ id か」に統一 |
| Minor-2 | 「今日」契約が archive のデルタスペック依存 | **採用** | spec R1-3 に観察可能な契約 (今日へ移動・非発火・範囲外 no-op・a11y 公開) を自己完結で記載 |
| Minor-3 | ui/ 省略が規約と不整合 | **採用** | ui/brief.md を作成 (mock 免除理由・視覚基準・証跡置き場)。mock 不要の判断自体は維持 (相方も同意) |

採用 7 / 一部降格 1 / 降格 0 / 未解決 1 (Critical — ユーザー裁定待ち)。ADR-0011 への修正 (tag 符号化・id 安定性条件・Critical の解決策) は Critical の裁定後にまとめて反映する。

### 追記 (2026-08-03): Critical の裁定

オーナー裁定により案 a (単独インスタンス時のみ完全復元 + claim による重複処理防止) を採用。spec (複数インスタンス時の規則 + Scenario)・tasks (3.3 / 4.8)・proposal (既知の制約)・ADR-0011 (Decision 4 追加、代替案 b/c の却下理由収録) に反映済み。未解決 0 件。
