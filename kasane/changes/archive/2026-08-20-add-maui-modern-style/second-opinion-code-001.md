# セカンドオピニオン: add-maui-modern-style (code-001)
**相方**: codex / **日付**: 2026-08-20 / **対象**: HEAD (f3e9f43) に対する作業ツリーの未コミット変更一式
---
# レビュー結果: add-maui-modern-style

**判定**: **CHANGES_REQUESTED**

提示されたビルド・テスト結果は成功として扱い、静的レビューのみ実施しました。主経路は仕様に沿っていますが、Android で仕様上許容される負値・非有限値が描画時正規化へ到達する前に例外となる問題があります。

指摘件数: Critical 0 / Major 1 / Minor 2 / Suggestion 0

## 指摘事項

### [🟠 Major] Android Bridge が不正 margin を Theme 構築前に拒否する

**該当箇所**: [android/ks-settingsview-bridge/.../KsBridgeTheme.kt:191](android/ks-settingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeTheme.kt:191)

**問題点**: `resolveSectionMargin()` が Compose 標準の `PaddingValues(...)` を使用していますが、このコンストラクタは負の成分と `NaN` を拒否します。この制約はテスト用 [RawPaddingValues.kt:10](android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/RawPaddingValues.kt:10) 自身にも明記されています。

そのため、次の契約経路は成立しません。

```text
MAUI の負値・NaN SectionMargin
→ KsBridgeTheme.resolveSectionMargin()
→ PaddingValues の事前条件違反
→ SectionBoxMetrics の描画時 0 正規化へ到達しない
```

これは「Theme 構築時には拒否せず、描画時のみ正規化する」という Android delta spec、および facade の「範囲外・非有限値でも例外を投げない」契約に反します。現在のテストは facade では fake gateway、UI 層では `RawPaddingValues` を使っており、問題の Bridge 境界を通らないため検出できていません。

**推奨修正**: Bridge 内で未検証値をそのまま保持できる `PaddingValues` 実装を使用し、負値・NaN・±Infinity を `SectionBoxMetrics` まで輸送してください。Bridge テストにも、負値・非有限 margin を `setTheme()` へ渡して例外なく描画時に 0 へ正規化される統合ケースを追加してください。

### [🟡 Minor] 視覚照合の完了チェックと証跡の組数が一致しない

**該当箇所**: [kasane/changes/add-maui-modern-style/tasks.md:36](kasane/changes/add-maui-modern-style/tasks.md:36)

**問題点**: タスクは `OS × style × preset` の全組照合を要求しています。2 OS × 2 styles × 3 presets なので12組ですが、証跡は10組で、両 OS の `Classic × Bordered` がありません。

この組は「Classic では border 指定を無視する」ことを直接確認するケースなので、`Classic × Standard` だけでは代替できません。

**推奨修正**: iOS/Android の `Classic × Bordered` について、MAUI/native の比較画像4枚を追加して全12組を揃えてください。

### [🟡 Minor] 「対応表を作る」タスクの成果物が確認できない

**該当箇所**: [kasane/changes/add-maui-modern-style/tasks.md:31](kasane/changes/add-maui-modern-style/tasks.md:31)

**問題点**: 文言・Section構成・preset内容の対応表を作成するタスクが完了扱いですが、対象差分と変更ディレクトリ内に対応表が存在しません。実装内容自体のパリティは確認できましたが、チェック済みタスクの成果物としては追跡できません。

**推奨修正**: 対応表を変更アーティファクト内へ保存するか、成果物を残さない準備作業という意図ならタスク記述と完了状態を整合させてください。

## アクションプラン

1. Android Bridge で未検証 margin を保持できる輸送型へ変更し、Bridge を通る回帰テストを追加する。
2. `Classic × Bordered` の視覚証跡を両 OS で追加する。
3. sample-parity 対応表の成果物とタスク状態を整合させる。

**最終判定: CHANGES_REQUESTED**

## 突き合わせ結果 (ksn-orchestrator, 2026-08-20)

ホスト側: review-001.md (CHANGES_REQUESTED / Critical 1・Major 1・Minor 1)

| 指摘 | 出典 | 採否 | 確定重要度 |
|---|---|---|---|
| Android Bridge `resolveSectionMargin()` の `PaddingValues(...)` が負値・NaN を構築時拒否し素通し契約が破綻 | 双方一致 (ホスト Critical / 相方 Major) | 確定 | Critical (ホスト側の高い方) |
| Bridge 層に負値・非有限の回帰テストが無く Critical が全テストを通過 | 双方一致 (ホスト Major / 相方は Major 内の推奨修正) | 確定 | Major |
| 視覚照合が 12 組中 10 組 (`Classic × Bordered` 両 OS 分欠け) | 相方のみ | 採用 (task 5.1 の全組要求 + この組のみが Classic の border 無視を直接検証) | Minor |
| sample-parity 対応表が change 配下に成果物として無い | 相方のみ | 採用 (task 4.1 の成果物追跡。修正コスト小) | Minor |
| `SectionDecorationDemoViewModel.Styles` 未使用 | ホストのみ | 確定 | Minor |

未解決 (矛盾) なし。採用指摘はホスト側指摘と同格として修正サイクル 1 周目で処理する。
