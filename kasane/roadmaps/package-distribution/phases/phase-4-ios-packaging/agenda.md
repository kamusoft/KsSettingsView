# phase-4-ios-packaging

SwiftPM 専用の配信リポジトリを作り、`ios/` (Package.swift / Sources / Tests) のスナップショットを umbrella product `KsSettingsView` 1 本で配布できる形にする (cross/ADR-0018、2026-08-21 改訂)。monorepo のルートに Package.swift は置かない。

## 論点

(すべて解消済み — 決定事項へ移動)

## 決定事項

- products は umbrella `KsSettingsView` 1 本のみとし、既存 3 product (`KsSettingsViewCore` / `KsSettingsViewUI` / `KsSettingsViewSwiftUI`) は `ios/Package.swift` から削除する。target 構成・module 名は変更しない。`samples/ios` のリンク設定は 3 productRef → umbrella 1 本へ差し替える (Local Swift Package 参照 `../../ios` は変更不要)。cross/ADR-0018 の決定文どおり (2026-09-01)
- スナップショットの中身はホワイトリスト方式 (列挙した以外は構造的に含めない) で次の 5 点: `ios/Package.swift` (umbrella 1 本化反映) / `ios/Sources/` / `ios/Tests/` (testTarget path 参照のため必須) / monorepo ルート `LICENSE` のコピー / 誘導 README。`binding/` `DerivedData/` `.build/` `.swiftpm/` 等は列挙外として自然に除外される (2026-09-01)
- スナップショット生成は `scripts/spm-snapshot/` にファイル配置専用スクリプト + 誘導 README テンプレートを置く。スクリプトの責務は「配信リポジトリ作業コピーの `.git` 以外を空にしてホワイトリスト 5 点を配置する」まで (冪等)。commit / tag / push は呼び出し側 — phase-4 は手動 (初回 push と https 解決確認まで)、phase-8 は release workflow (tag 最後の制御は cross/ADR-0020 に従い workflow 側が持つ) (2026-09-01)
- tag 表記は接頭辞なしの `X.Y.Z`。dispatch 入力 = tag = 各レジストリへ注入する version が同一文字列で流れ、変換を持たない。monorepo・配信リポジトリで同じ値、KsDialogs も同表記で揃える (逆流は KsDialogs phase-11)。cross/ADR-0020 に追記済み (2026-09-01)
- 配信リポジトリへの CI 書き込みは書き込み許可付き deploy key 方式。秘密鍵は monorepo のリポジトリ単位 Actions secrets (例: `SPM_DEPLOY_KEY`) に置く (organization secrets は KsDialogs との鍵共有になり 1 鍵 1 リポジトリの最小権限が崩れるため不採用)。鍵の作成・登録は phase-8 で実施 — phase-4 の初回 push は手動のため CI 用の鍵は不要 (2026-09-01)

### 配信リポジトリ `KsSettingsView-SPM` の初期設定 (2026-09-01)

- 最初から public で作成 (中身はホワイトリスト 5 点のみで機密混入の余地がなく、https 実リモート解決確認に public が必須)
- default branch は `main`。Issues / Wiki / Projects / Discussions は無効化。PR は monorepo と同じ collaborators only 設定 (cross/ADR-0024 と同じ機構)
- README は誘導のみ (ソース・Issue 窓口・インストール手順は monorepo へのリンク)
- workflow・branch protection は一切置かない (release CI が直 push する)
- GitHub Release は作らない — tag のみ。Release は ADR-0020 どおり monorepo 側にだけ作る
- description と Website リンクは monorepo へ向ける

### binding への影響 (2026-09-01)

- 「無影響の見込み」は実査で覆った: `ios/binding/KsSettingsViewBridge.xcodeproj` は product `KsSettingsViewCore` / `KsSettingsViewUI` を productRef でリンクしており、product 削除でビルドが壊れる。`samples/ios` と同じく umbrella `KsSettingsView` 1 本への差し替えを phase-4 の作業範囲に含める
- 受け入れ条件: binding xcodeproj のビルド成功と xcframework 生成確認 (umbrella 経由で SwiftUI target もリンク対象に入るが、Bridge は SwiftUI の記号を参照しないためリンカが落とすことを実証する)
- MAUI 側 (`KsSettingsView.Maui.csproj` → `KsSettingsView.Binding.iOS.csproj` の ProjectReference 連鎖) は `Package.swift` の product を参照しておらず無影響と確認済み

## TODO

- [x] 論点の解消 (2026-09-01 全 7 決定)
- [x] ksn-propose で変更提案を起こす (add-spm-distribution、2026-09-01)
- [x] https 実リモート解決確認 (2026-09-01 完了、tag `0.1.0-alpha.1` で確認後削除): 一時消費者プロジェクトから `https://github.com/kamusoft/KsSettingsView-SPM` を tag 指定で解決する。確認用 tag は prerelease 表記 (`X.Y.Z-alpha.N` 等) — 正式 tag は publish 全成功後にのみ生まれる (cross/ADR-0020) ため
- [x] 蒸留時の concepts 追随 (2026-09-01 完了): repository-boundaries.md (配信リポジトリの位置づけ、ルートに Package.swift を置かないこと)、public-identifiers.md の SwiftPM product 行 (3 本 → umbrella 1 本) と Package URL・配信リポジトリ名
- [x] **phase-9 からの申し送り** (2026-08-30、2026-09-01 完了): 配信リポジトリ `KsSettingsView-SPM` を作成し、`kasane/handbook/cross/public-identifiers.md` へ配信リポジトリ名を追記する。ルート README (`README.md` / `README_ja.md`) と `skills/{en,ja}/kssettingsview-ios/SKILL.md` は既にこの名前を暫定値として書いている (cross/ADR-0018)

## 実装結果 (2026-09-01 反映)

- change: [changes/archive/2026-09-01-add-spm-distribution](../../../../changes/archive/2026-09-01-add-spm-distribution/proposal.md)。verify VALID / review APPROVED。https 実リモート解決 (`0.1.0-alpha.1` の exact 指定・3 module の公開型参照ビルド成功) まで確認し、検証用 tag は削除済み。配信リポジトリは「commit はあるが tag がない」意図した状態で、初回 tag は phase-8 が打つ
- 見込みとの差: binding の staticlib archive では umbrella 経由の `KsSettingsViewSwiftUI` オブジェクトが dead-strip されず xcframework に混入する (シンボル 642 件)。**見送り判断**: オーナー合意で受容し、除外が必要になったら将来の change で検討する ([deviation.md](../../../../changes/archive/2026-09-01-add-spm-distribution/deviation.md))
- 付随: product 一本化で Xcode 生成 scheme が `KsSettingsView` 1 本になり、`verify-ios.yml`・handbook (test-execution / swift6-language-mode-check) の scheme 名を同 change 内で追随済み。同期スクリプトの安全弁テストは CI lint job に常設
- 申し送り → phase-8: deploy key の作成・secrets 登録・release workflow への同期スクリプト組み込み・初回 tag ([phase-8 agenda の TODO](../phase-8-release-workflow/agenda.md) に追記済み)
- 申し送り → KsDialogs: 同型展開 (`KsDialogs-SPM`) は KsDialogs 側 phase-11 の責務 (本ロードマップの非ゴールどおり、追加の受け皿なし)
