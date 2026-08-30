# エミュレータ実機確認: fix-android-accessory-header-refresh

- 日付: 2026-08-05 / 実施: ksn-orchestrator (ユーザー指示によるエミュレータ検証 + review-001 Suggestion 6)
- 環境: Android Emulator Pixel_6 (emulator-5554) / samples/android (composite build でワークツリーの本体ソースを参照)
- 手段: StoreDemoScreen に一時検証ボタンを追加 (検証後に revert 済み。証跡はスクリーンショットのみ残す)

| # | 操作 | 期待 (修正後) | 結果 | 証跡 |
|---|---|---|---|---|
| 1 | 初期表示 | header「PoC Section」 | ✅ | verify-device-01-initial.png |
| 2 | `updateAccessory(SectionHeader, 同一 sectionID, 別 text)` | header が「Header 更新 4」へ即時反映 (修正前は Adapter 通知 0 件で不変) | ✅ | verify-device-02-updateaccessory-header.png |
| 3 | `replaceSection` (同一 id・header text のみ変更) | header が「RS 更新 5」へ即時反映 | ✅ | verify-device-03-replacesection-header.png |
| 4 | `updateAccessory` で Text → View 型切替 | header 行が View accessory (赤字 VIEW HEADER) へ差し替わる (review-001 Suggestion 6 の目視対象) | ✅ | verify-device-04-type-switch-view.png |

いずれもクラッシュ・ちらつき・行の残骸なし。Cell 行 (Sample Row 1〜3) は影響を受けず表示維持。
