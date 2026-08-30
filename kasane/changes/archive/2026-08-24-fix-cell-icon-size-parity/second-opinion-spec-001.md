# セカンドオピニオン: fix-cell-icon-size-parity (spec-001)
**相方**: codex (器: ksn-reviewer) / **日付**: 2026-08-22 / **対象**: 提案一式 (proposal / specs / tasks / ui/brief) — 自己レビュー 2 周完了時点
---
# レビュー結果: fix-cell-icon-size-parity

**日付**: 2026-08-22

## サマリー

現状のまま実装へ進むべきではありません。特に iOS の制約優先度案は要求する正方形枠を保証できず、数値の有効範囲や狭幅時の縮退規則にも未決の設計判断があります。

指摘件数は Critical 0 / Major 6 / Minor 3 / Suggestion 1 です。指定どおりファイル更新・ビルド・テストは行っていません。

## 指摘事項

### [🟠 Major] iOS の制約優先度案では固定サイズを保証できない

**該当箇所**: [tasks.md:13](kasane/changes/fix-cell-icon-size-parity/tasks.md:13)、[KsListCellBase.swift:165](ios/Sources/KsSettingsViewUI/KsListCellBase.swift:165)、[KsListCellBase.swift:168](ios/Sources/KsSettingsViewUI/KsListCellBase.swift:168)、[iOS spec.md:14](kasane/changes/fix-cell-icon-size-parity/specs/settings-view-ios-ui/spec.md:14)

**問題点**: 提案はサイズ制約を 999、horizontal hugging / CCR を 750 にしますが、vertical compression resistance は現状の `.required` のままです。したがって intrinsic height が枠より大きい画像では、999 の高さ制約が 1000 の vertical CCR に負け、tasks 2.3 の「大きい UIImage でも幅・高さが解決済みサイズ」という受け入れ条件を満たせません。また狭幅時は `stackV`・title・accessory の horizontal CCR が `.required` のため、icon の幅制約 999 が先に破られます。

**推奨修正**: 次のどちらを契約として選ぶか決めてください。

- icon 表示中は幅・高さを required にし、非表示時だけ競合する制約を deactivate する。
- 圧縮時には icon が縮むことを仕様化し、固定サイズ SHALL をその条件に合わせて弱める。

前者なら縦横両方向の大きい画像・非表示遷移・狭幅レイアウトをテストに追加してください。

### [🟠 Major] 「同じ Theme なら全行同じ列幅」と CellStyle 優先が矛盾する

**該当箇所**: [iOS spec.md:7](kasane/changes/fix-cell-icon-size-parity/specs/settings-view-ios-ui/spec.md:7)、[iOS spec.md:19](kasane/changes/fix-cell-icon-size-parity/specs/settings-view-ios-ui/spec.md:19)

**問題点**: Requirement は同じ Theme 下の全 icon 付き Cell が同じ列幅・title 開始位置を持つとしていますが、直後に Cell ごとの `CellStyle.iconSize` が Theme より優先されると規定しています。同じ Theme でも異なる CellStyle を持つ正当な入力では両方を満たせません。

**推奨修正**: 「解決済み icon size が同じ Cell 間」へ条件を限定し、SF Symbols の Scenario にも「各 Cell の `CellStyle.iconSize` は同一または未指定」を明記してください。

### [🟠 Major] 「未指定利用者は変化なし」という影響評価が事実と異なる

**該当箇所**: [proposal.md:35](kasane/changes/fix-cell-icon-size-parity/proposal.md:35)、[proposal.md:8](kasane/changes/fix-cell-icon-size-parity/proposal.md:8)、[proposal.md:17](kasane/changes/fix-cell-icon-size-parity/proposal.md:17)

**問題点**: iOS では `cellIconSize` 未指定でも既定の 24pt 制約が現在 intrinsic size に負けています。修正後は SF Symbols や 24pt より大きい UIImage が 24pt 枠へ収まるため、未指定利用者にも明確な見た目変更があります。「既定値の生値が変わらない」と「既定利用者の描画が変わらない」が混同されています。

**推奨修正**: Impact を「既定値は不変だが、iOS では未指定でも intrinsic size が既定枠と異なる icon の描画・title 開始位置が変わる」に訂正してください。

### [🟠 Major] icon size / radius の入力値域が未定義

**該当箇所**: [Android spec.md:7](kasane/changes/fix-cell-icon-size-parity/specs/settings-view-android-ui/spec.md:7)、[Android spec.md:41](kasane/changes/fix-cell-icon-size-parity/specs/settings-view-android-ui/spec.md:41)、[iOS spec.md:7](kasane/changes/fix-cell-icon-size-parity/specs/settings-view-ios-ui/spec.md:7)

**問題点**: 公開 API は負値・0・NaN・±∞を拒否していません。Android で負の dp を LayoutParams に直接変換すると `MATCH_PARENT` / `WRAP_CONTENT` の予約値と衝突し得ます。iOS では負の寸法が不正な Auto Layout 制約になります。ADR-0025 の「clamp しない」は非正方形画像と radius の関係についてであり、非有限値や負の icon size の意味は決めていません。

**推奨修正**: 少なくとも以下を両 OS 共通契約として決め、Scenario を追加してください。

- icon size は有限かつ正である必要があるか。
- 無効値を既定へ戻す、0へ正規化する、例外にする、のどれか。
- radius の負値・非有限値・size の半分を超える値をどう扱うか。

### [🟠 Major] デルタスペックと UI brief が Kasane の UI アーティファクト境界に反する

**該当箇所**: [Android spec.md:7](kasane/changes/fix-cell-icon-size-parity/specs/settings-view-android-ui/spec.md:7)、[iOS spec.md:7](kasane/changes/fix-cell-icon-size-parity/specs/settings-view-ios-ui/spec.md:7)、[ui/brief.md:14](kasane/changes/fix-cell-icon-size-parity/ui/brief.md:14)

**問題点**: デルタスペックには正方形配置、aspect fit、title の開始位置、clip 形状など具体的なレイアウト記述が大量に含まれています。これは `ksn-core/references/delta-spec.md` の UI lint が禁止する「レイアウト配置」に該当します。また brief にも具体レイアウトが入り、mock と brief の責務が混在しています。

**推奨修正**: デルタスペックには Theme / CellStyle の変更が表示中の行へ反映される、といった観察可能な状態遷移を残してください。正方形枠・aspect fit・clip の見た目は ADR-0025、承認 mock、実装テストへ分離し、brief はそれらへの参照と照合対象の説明に限定してください。

### [🟠 Major] proposal の domain がルーティング規約と一致しない

**該当箇所**: [proposal.md:43](kasane/changes/fix-cell-icon-size-parity/proposal.md:43)、[concepts/rules.md:16](kasane/concepts/rules.md:16)

**問題点**: proposal は Android と iOS の実装能力を同時に変更するため、規約上 `domain: cross` です。`domain: core` のままだとオーケストレーション時に Android / iOS の `domain-skills` が結合されず、実装・レビューのスキル解決も誤ります。共通契約の蒸留先が core であることと、proposal の作業ドメインは別です。

**推奨修正**: `domain: cross` へ変更してください。蒸留時に ADR / concepts を core へ置く判断は維持できます。

### [🟡 Minor] Android の aspect fit と radius 再適用がテストで固定されない

**該当箇所**: [tasks.md:7](kasane/changes/fix-cell-icon-size-parity/tasks.md:7)、[tasks.md:8](kasane/changes/fix-cell-icon-size-parity/tasks.md:8)、[tasks.md:9](kasane/changes/fix-cell-icon-size-parity/tasks.md:9)

**問題点**: 非正方形 drawable のテストは LayoutParams と visibility しか検証せず、「画像が枠内に aspect fit で収まる」を判定できません。また Theme 変更テストは size だけで、同じ ViewHolder に対する radius の正値→0、正値→別値の遷移を検証しません。後者は stale な `clipToOutline` / outline provider を見逃します。

**推奨修正**: `scaleType == FIT_CENTER` と描画対象が枠を越えないことを検証し、同一 ViewHolder で radius の設定・更新・解除を行う再 bind テストを追加してください。

### [🟡 Minor] iOS の制約警告確認が再現可能な受け入れ判定になっていない

**該当箇所**: [tasks.md:15](kasane/changes/fix-cell-icon-size-parity/tasks.md:15)、[iOS spec.md:7](kasane/changes/fix-cell-icon-size-parity/specs/settings-view-ios-ui/spec.md:7)

**問題点**: Requirement は「制約の不整合を報告しない」と SHALL で規定していますが、tasks はコンソールを目視し「自動アサート不要」としています。ログの保存先もなく、レビューや CI で再判定できません。同じ task の Theme 更新も `KsSettingsViewController.applyTheme(_:)` を通すのか、Cell を直接再 render するのか曖昧です。

**推奨修正**: hidden 時の制約 active 状態・優先度・最終寸法を自動テストし、ログ確認を残すなら change 配下へ保存してください。Theme 更新テストは controller の `applyTheme(_:)` を明記してください。

### [🟡 Minor] iOS の優先度コメントに ADR-0025 を引用する指示が不適切

**該当箇所**: [tasks.md:26](kasane/changes/fix-cell-icon-size-parity/tasks.md:26)、[ADR-0025:18](kasane/decisions/core/0025-cell-icon-radius-applies-to-square-frame.md:18)

**問題点**: ADR-0025 は角丸の適用先と Android outline を決めていますが、iOS の 999 / 1000 の優先度設計は扱っていません。iOS の優先度コメントを ADR-0025 参照にすると、根拠のない引用になります。

**推奨修正**: Android outline のみ ADR-0025 を参照し、iOS のコメントは「UIStackView の required な非表示制約を優先する」など、そのファイルだけで理解できる理由を記述してください。

### [🔵 Suggestion] iOS の主不具合を表す承認ビジュアルがない

**該当箇所**: [ui/brief.md:24](kasane/changes/fix-cell-icon-size-parity/ui/brief.md:24)、[tasks.md:21](kasane/changes/fix-cell-icon-size-parity/tasks.md:21)

**問題点**: `approved.png` は正方形バッジを使う Section 装飾デモであり、iOS の主不具合である intrinsic 幅の異なる SF Symbols を表示していません。修正後の共通フィールド画面は before/after の自己比較だけになり、承認済みの見た目との照合になりません。

**推奨修正**: 共通フィールドデモについて、title 開始位置の基準線が分かる承認画像または注釈付き reference を追加してください。

## アクションプラン

1. iOS の固定サイズと hidden／圧縮時の優先順位を決定する。
2. 無効な icon size / radius の共通契約を決定する。
3. Impact、iOS Requirement の矛盾、`domain:` を修正する。
4. UI lint に従って spec / brief / mock の責務を再配置する。
5. Android の aspect fit・radius 遷移、iOS の controller rebind・hidden 制約を自動テスト可能な形へ具体化する。

**判定: NEEDS_DISCUSSION**

## 突き合わせ結果 (2026-08-22、ホスト側自己レビュー 2 周との照合)

| # | 指摘 | 採否 | 根拠・反映先 |
|---|---|---|---|
| Major-1 | iOS 優先度案 (999/750) は垂直 CCR と狭幅時の title CCR に負ける | **採用** (ホスト側の見逃し) | `KsListCellBase.swift:168` で垂直 CCR `.required` を確認。相方案 1 (表示中 `.required` + 非表示時 deactivate) を採用。狭幅時の配分は別 change に逃がさず本 change で iOS を既存契約 (concepts cell-row-layout / android ADR-0002) へ揃える (オーナー指示)。→ iOS spec Requirement 1・3、proposal What Changes 2・3、tasks 2.1 / 2.3 / 2.7 |
| Major-2 | 「同じ Theme なら全行同じ列幅」と CellStyle 優先の矛盾 | **採用** | 「解決済み icon size が同じ Cell 間」へ限定、Scenario に CellStyle 同一/未指定を明記 |
| Major-3 | 「未指定利用者は変化なし」は iOS で誤り | **採用** | proposal Impact を訂正 (iOS 未指定利用者にも描画変化あり) |
| Major-4 | icon size / radius の値域未定義 (Android 負 dp が LayoutParams 予約値と衝突) | **採用** | 既存の `> 0` パターンに揃え「size は正の有限値、radius は 0 以上の有限値、無効値は未指定として次の段へ」を両 OS 共通契約に。半辺超えは clamp しない (ADR-0025 と同じ姿勢)。→ 両 spec に Scenario 追加、tasks 1.1 / 1.3 / 2.2 |
| Major-5 | spec にレイアウト記述が多く UI lint 違反 | **降格 (部分反映)** | `cellIconSize` の契約自体が寸法であり「幅高さ = 解決値」は観測可能な状態として残す。実装寄りの「aspect fit」は「枠を超えない」へ言い換え。見た目の正は approved.png / ADR-0025 に置く方針は維持 |
| Major-6 | `domain: core` は規約違反 | **採用** | `concepts/rules.md`「複数ドメインに触る proposal は cross」に従い `domain: cross` へ。先例 fix-dsl-header-height-diff の core は先例側の誤り |
| Minor-1 | Android の aspect fit・radius 遷移がテストで固定されない | **採用** | tasks 1.2 (scaleType 明示・clip 解除) / 1.6 (正→別→0 の再 bind)、Android spec に再 bind Scenario 追加 |
| Minor-2 | iOS の制約警告確認が再現不能、Theme 更新経路が曖昧 | **採用** | 非表示時の `isActive == false` を自動テスト (tasks 2.5)、ログは `ui/verification/ios-test-constraints.log` に保存 (2.8)、Theme 更新は `applyTheme(_:)` を Scenario に明記 |
| Minor-3 | iOS の優先度コメントに ADR-0025 を引くのは不適切 | **採用** | tasks 4.1: Android outline のみ ADR-0025 参照、iOS は自己完結の理由を書く |
| Suggestion-1 | SF Symbols ケースの承認画像がない | **降格 (部分反映)** | 新規モックは作らない。tasks 3.2 で修正前後 + 基準線注釈の比較画像を証跡として作る |

集計: 採用 8 / 降格 2 / 未解決 0。降格 2 件もいずれも部分反映済み。

### 追記 (2026-08-22、Major-1 の最終処置)

Major-1 で本 change に取り込んだ幅配分について、移植元 AiForms を確認した結果「iOS を Android の配分へ揃える」前提が覆った (オリジナルの時点で両 OS が逆)。オーナー裁定で **Android を iOS の配分 (title を守り valueText を省略) へ揃える** (core/ADR-0026)。iOS の優先度は無変更となり、Android の既定配分入れ替え + 既存テストの反転が本 change に入る。採否 (採用) は変わらず、反映先が iOS → Android へ移った。
