# phase-4-ios-packaging

SwiftPM 専用の配信リポジトリを作り、`ios/` (Package.swift / Sources / Tests) のスナップショットを umbrella product `KsSettingsView` 1 本で配布できる形にする (cross/ADR-0018、2026-08-21 改訂)。monorepo のルートに Package.swift は置かない。

## 論点

- 配信リポジトリの名前 (`KsSettingsView-swift` 等、cross/ADR-0002 の命名との整合) と初期設定 (public、Issues / PR / Wiki 無効、README は monorepo への誘導のみ、CI 以外は書かない)
- スナップショットの中身 (`Package.swift` / `Sources/` / `Tests/` / LICENSE / 誘導 README) と、`ios/binding/` `ios/DerivedData` `ios/build` を含めない除外規則
- スナップショット生成の実装 (スクリプトか workflow か、monorepo の `scripts/` に置くか) と phase-8 との分担: phase-4 で手動実行による初回 push と解決確認まで、release workflow への組み込みは phase-8
- 配信リポジトリへ CI が書き込む権限 (deploy key か fine-grained PAT か) と secret の置き場 (phase-8 の secrets 設計と連動)
- products の最終形: `KsSettingsView` 1 本 (3 target 束ね)。既存 3 product を残すか消すか (ADR-0018 は 1 本)。`samples/ios` のリンク設定 (3 product → 1 product) の変更 (Local Swift Package 参照 `../../ios` は変更不要)
- `ios/binding/KsSettingsViewBridge.xcodeproj` と MAUI binding csproj (`KsBridgeSwiftRoot`) への影響確認 (Sources の位置は変わらないので無影響の見込み)
- tag 表記 (`X.Y.Z` か `vX.Y.Z` か) の KsDialogs との統一 (phase-8 の dispatch 入力形式とも連動)。配信リポジトリと monorepo で同じ値
- 配信リポジトリの https 実リモート解決確認 (KsDialogs PoC は file:// 止まり)
- 蒸留時の concepts 追随: repository-boundaries.md (配信リポジトリの位置づけ、ルートに Package.swift を置かないこと)、public-identifiers.md の SwiftPM product 行と Package URL

## 決定事項

(議論で確定したらここに移動)

## TODO

- [ ] 論点の解消
- [ ] ksn-propose で変更提案を起こす
- [ ] **phase-9 からの申し送り** (2026-08-30): 配信リポジトリ `KsSettingsView-SPM` を作成し、`kasane/handbook/cross/public-identifiers.md` へ配信リポジトリ名を追記する。ルート README (`README.md` / `README_ja.md`) と `skills/{en,ja}/kssettingsview-ios/SKILL.md` は既にこの名前を暫定値として書いている (cross/ADR-0018)
