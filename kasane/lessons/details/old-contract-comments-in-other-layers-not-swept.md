# 経緯: 契約変更後に旧契約を語るコメント・doc を層をまたいで掃く (impl L-006)

pain 3 例で 2026-09-05 に昇格。いずれも「観察可能な契約を変えたあと、旧契約を記述する既存の説明文 (コメント・KDoc・doc コメント・テスト説明) を、変更した箇所の外側で掃き残した」型。3 例ともレビューが検出しており、実装者自身は完了と判断していた。

- 2026-08-09 harden-update-accessory-unknown-id: 未知 sectionID の `updateAccessory` を Store で no-op にする契約変更 (core/ADR-0020) で、実装対象外の MAUI 層 (`IKsSettingsGateway.cs` / `KsSettingsController.cs` / `RemovedElementNotificationTests.cs`) に「この操作だけは no-op 契約の対象外」という正反対の記述が残った。review-001 が Major として検出、`契約の対象外` / `素通し` の再走査で修正完了を確認 (review-002)。
- 2026-08-25 adjust-section-spacing: ライブ調整で確定した余白・Switch 色の値変更後、周辺のテスト説明・KDoc・区切りコメントの旧仕様記述 (「AiForms 準拠で 0」「オフ = colorSurfaceContainerHighest/colorOutline」「オン thumb = colorOnPrimary」) が review-001/002/003 の 3 周にわたり順次検出された (1 回で網羅的に掃けていない)。特徴語 grep で最終的に全消化。
- 2026-09-05 ios-effectivestyle-visibility: `EffectiveStyle` を public → internal に降格した際、公開型 `Theme` の doc コメント 3 箇所と `// MARK:` 行が internal 化後の型を利用者向けに案内したまま残存。実装者は型名で `ios/Sources/` を grep して該当行を見ていたが「内部機構の説明」と判断して掃かず、公開 protocol `KsCellRenderer` の 1 箇所だけを直した。review-001 が Major として検出、review-002 で解消確認。

観測の共通構造: 契約の変更点は実装者の頭の中で「変えた箇所」に局在するが、旧契約の説明はそれを参照するあらゆる層 (別 platform の facade・テストの説明文・公開型の doc) に散らばっている。コンパイルもテストも散文を検査しないため、掃き残しは人が特徴語で探す以外に検出手段がない。3 例目は grep まではしていた点で前 2 例と異なり、取りこぼしの原因は検索ではなく判定にあった — 「その行が内部機構の説明か、利用者向けの案内か」を公開メンバーの doc かどうかで判定せずに残した。ルール文の後段 (残す判断の条件) はこの例から来ている。

なお可視性の降格 (public → internal) は挙動を変えないため「契約変更」と意識されにくいが、利用者から見える名前が消える点で観察可能な契約の変更であり、旧契約 (その型が公開されている前提の説明) の掃き出しが同じく必要になる。
