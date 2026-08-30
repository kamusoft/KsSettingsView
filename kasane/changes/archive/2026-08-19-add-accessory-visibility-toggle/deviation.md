# Deviation: add-accessory-visibility-toggle

- SwiftUI `SectionModifiers.copyWith` (`.sectionHeader(_:)` / `.sectionFooter(_:)`): spec ではトグル (`isHeaderVisible` / `isFooterVisible`) の保持のみ → オーナー合意により `isVisible` の保持も追加 (再構築時に `isVisible` を落とす既存コピー漏れバグのついで解消。固定テスト `SectionModifiersTests.test_モディファイアはid以外の状態フィールドを保持する` で固定)。理由: 同一箇所の同種コピー漏れであり、トグルのみ保持すると不整合が残るため (2026-08-19)
- tasks 6.2「対称化3件をスクリーンショットで記録」: spec/tasks は samples での記録を想定 → オーナー合意により、現行サンプルに再現手段が無いためサンプルの一時改変 (撮影後に完全 revert、SHA1 一致確認済み) で `evidence/symmetry-*` を取得。理由: 恒久デモの追加はスコープ拡大であり、証跡取得のみが目的のため (2026-08-19)
