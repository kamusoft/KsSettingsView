# 検証記録: fix-replace-section-header-refresh

- 検証日: 2026-08-05
- 検証環境: iPhone 17 シミュレータ / iOS 26.5 (23F73)、`xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.5'`

## 症状の再現 (修正前)

表示中の supplementary view を直接検証するテスト 3 件を `SectionAccessoryRenderingTests` に追加し、
修正を含まないビルド (git stash で修正のみ退避した A/B) で実行した:

```
Executed 14 tests, with 3 failures (0 unexpected)
failed: test_replaceSectionのtextヘッダ変更が表示中のsupplementaryに反映される
failed: test_fullDiffのtextヘッダ変更が表示中のsupplementaryに反映される
failed: test_replaceSectionのviewヘッダ差し替えが表示中のsupplementaryに反映される
```

- 失敗内容: `replaceSection` / `.full` 適用後も表示中 header の UILabel が旧テキストのまま
  (`XCTAssertEqual failed: ("Optional("ヘッダA")") is not equal to ("Optional("ヘッダB")")` 相当)
- 前提アサート (初期表示の header 描画) は成功しており、更新の取りこぼしだけが失敗している

## 解消の確認 (修正後)

同一手順・同一環境で:

- 追加 3 テストを含む `SectionAccessoryRenderingTests` 全 14 件 pass
- パッケージ全件: **Executed 411 tests, with 0 failures** / `** TEST SUCCEEDED **`

## 付記

- 追加テストは window ホスト + run loop 送り (`hostControllerInWindow` / `pump`) で実描画を確定させ、
  provider 呼び出しではなく `cv.supplementaryView(forElementKind:at:)` が返す表示中の実物 view を検証する
  (provider 経由の検証では常に新規生成 view が返り、本症状を検出できない)
- コメント規約 lint: 触った 2 ファイルの禁止件数はベースライン 64 件から増加なし
  (64 件は既存負債で、別 change `cleanup-comment-lint-debt` の対象)
