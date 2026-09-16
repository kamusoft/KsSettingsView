# Proposal: maui-cell-appthemebinding-logical-child

## Why

MAUI facade の `Section` / `Cell` は SettingsView の element ツリーに繋がっておらず (`Parent` が null)、Cell の色プロパティに書いた `AppThemeBinding` / `DynamicResource` は初期解決こそ届くが外観変更・Resources 差し替えで再評価されない。core/ADR-0031 はその帰結として「Cell の色は `RequestedThemeChanged` を購読して再代入する」を利用者の手段と定めており、`SettingsView` の Theme プロパティ (`AppThemeBinding` が効く) と Cell のプロパティで手段が割れる非対称が残っていた。

maui-appearance-change-tracking の spike ②-b (2026-09-15) で、Section / Cell を論理子に繋ぐだけで `AppThemeBinding` が再評価され native の描画まで届くことが iOS / Android の両方で成立した。facade 内では accessory View と `CustomCell.Content` が既に論理子 (maui/ADR-0016) であり、Section / Cell だけが非論理子という非対称も同時に解消できる。

## What Changes

- **Section / Cell を SettingsView の論理子にする** (SettingsView → Section、Section → Cell)。論理所有は accessory View と同じ「所属の寿命」で、Handler (Host) の有無に依らず、所属が始まる時点で繋ぎ、終わる時点 (Root からの除去・Section からの除去・ItemsSource 再生成・Root 差し替え) で外す。継承 BindingContext の配布と同じ器 (`KsBindingContextBinder` を所有の器に育てる) が 1 手順として行う
- **Section / Cell の全 BindableProperty で `AppThemeBinding` / `DynamicResource` が再評価される**ことを契約にし、テストで固定する
- **他所に所有された Section / Cell の追加は追加時に例外にする** (別の Section・別の SettingsView・外れた Section が所有したままの場合とも。Host の有無に依らず)。同じコレクション内の二重追加は現行どおり変換時に例外。accessory View / `CustomCell.Content` の検査にも「期待する所有者以外の facade 所有者に所有された View」を加える。例外の型と「native へ構造・snapshot を配信する前に全件検査し部分更新を残さない」契約は現行のまま
- **利用者向けの手段を `AppThemeBinding` / `DynamicResource` に一本化**し、購読 + 再代入を記述から外す: concepts (`maui-styling.md` の手段表・`maui-facade.md` の制約・`maui-rendering-lifecycle.md` / `view-materialization.md` の論理所有)、Sample (SettingsView 用の共有 Style (Setter に `AppThemeBinding`) を Sample のリソースに置いて 7 ページが参照し、ButtonCell の `TitleColor` は XAML の `AppThemeBinding` に。`SampleThemeFollower` と `SampleTheme.Apply` / `MauiTitleText` を削除。オーナー裁定 2026-09-15: Cell 分だけ置換して補助クラスを Theme 用に残す案は、購読が Sample に残り 3 面一致の実例として弱いため却下)、検証ホスト MauiHost の既定シナリオ (購読 + 再代入 → `AppThemeBinding` の再評価)
- ADR: cross/ADR-0032 (proposed、core/ADR-0031 Decision 2 の MAUI 節を amends) を起票済み。昇格は蒸留時

影響する能力: `maui-core` (facade の所有モデル・binding の解決契約)、`samples-maui` (外観追随の書き方)。

## Non-Goals

- **Skill (`skills/{en,ja}/kssettingsview-maui/references/styling.md`、`kssettingsview-aiforms-migration/references/api-mapping.md`) の追随** — skills は concepts からの派生物で、更新は蒸留後に docs-refresh 経由に限る (AGENTS.md の規約)
- **`Section` / `CellBase` を `VisualElement` / `NavigableElement` にすること** — 暗黙 Style が当たるようになり Window ごとの外観解決にも乗る別の設計判断。本 change は素の `Element` のまま (ADR-0032 の前提)
- **マルチウィンドウで窓ごとに外観が異なる構成の支援** — `AppThemeBinding` が `VisualElement` でない対象では `Application.Current` の外観に落ちる MAUI 本体の挙動。上の Non-Goal と同じ設計判断に属する
- **`x:Reference` の解決** — namescope 経由で論理子 (`Parent` チェーン) とは別機構。concepts の記述は現行維持
- **CSS StyleSheet が Section / Cell に当たることの契約化** — `.css` 利用時の副作用 (実害は薄い見立て) は ADR-0032 の Consequences に留め、実測・契約化は要望が出たときに別途
- **iOS の外観変化の本体観測 (maui-appearance-change-tracking の ①)** — 親 change の領分。本 change は同梱しない取り決め (親 change の tasks 5.1)

## Impact

- 見た目の変更なし (Sample は実装前後で同じ描画)。`ui/` は作らない
- 公開 API の追加・変更なし。購読 + 再代入は動作し続け、効かなかった XAML が効くようになる方向の挙動拡張
- 破壊的変更 1 件: 他所に所有された Section / Cell の追加が**追加時**に `InvalidOperationException` になる (現行は同じ SettingsView 内なら変換時、別 SettingsView をまたぐ場合は黙って通る)。同じ Section / Cell / View を複数の SettingsView で共有する配置は誤用であり、concepts の契約「複数箇所へ置くことは禁止」に実装を合わせる。beta 中は受容する (core/ADR-0031 Decision 3)。次の prerelease のリリースノートに記載
- リスク: 外す側 4 経路の対称化が漏れると `Parent` が古い所有者を指し続け、多重配置の誤判定や古い親の Resources 参照を生む。デルタスペックの所有解除 Scenario と leak テストで固定する
- リスク: MAUI 本体の `Element.Parent` が弱参照であることに依存する (spike で LeakTests 緑)。実装の最初の一手で既存 LeakTests 2 件を論理子化込みで確認し、意図をテスト名 / コメントに明記する
- 前提: settingsview-migration-defects (`maui-facade.md` の `DynamicResource` 記述訂正) が先に merge される。本 change は同じ段落をさらに書き換える

## 級: L

MAUI facade 1 能力・公開 API 不変だが、所有モデルを Section / Cell へ広げる設計変更で accepted の core/ADR-0031 を amends し、契約として出すと戻しにくい (覆すコストが高い)。オーナー確定 2026-09-15。

domain: maui

ADR の置き場について: 起票済みの cross/ADR-0032 は core/ADR-0031 を amends するため、ドメインをまたぐ改訂の規則 (ksn-core references/domain-axis.md) で cross に置いている。change 自体が触るコード・テスト・Sample と concepts の行き先はすべて maui であり、`domain:` は maui とする (cross にすると蒸留時の concepts の行き先が誤る)。
