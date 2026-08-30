# セカンドオピニオン: investigate-maui-icon-lease-sharing (code-003)
**相方**: codex / **label**: so-code2-investigate-maui-icon-lease-sharing / **日付**: 2026-08-25 / **対象**: D 案 (所有権分類方式) 実装後の未コミット working tree 変更 (KsFileImageOwnership.cs 新規、KsSharedImageRegistry 廃止、iOS KsImageResolver.cs / テスト群の再編)
---
# レビュー結果: investigate-maui-icon-lease-sharing

**日付**: 2026-08-25  
**判定**: **CHANGES_REQUESTED**

## サマリー

合意済みの所有権分類方式は妥当ですが、MAUI 本体と異なるキーで名前付き画像キャッシュを照合しており、特定のファイル指定でキャッシュ所有画像を facade 所有と誤分類します。これにより、今回防ぐべき `UIImage.Dispose()` による共有画像破壊が再発します。

## 指摘事項

### [🟠 Major] MAUI の fallback と異なる画像名で所有権を判定している

**該当箇所**: `maui/KsSettingsView.Maui/Platforms/iOS/KsImageResolver.cs:96`、`maui/KsSettingsView.Maui/Internals/KsFileImageOwnership.cs:61`

**問題点**: Microsoft.Maui 10.0.70 の `FileImageSourceService` は主経路に失敗すると、`Path.GetFileNameWithoutExtension(file)` を `UIImage.FromBundle` に渡します。一方、対象実装は元の `fileName` をそのまま渡しています。

例えば `images/shared.png` の主経路が失敗し、MAUI が asset catalog の `shared` を返した場合、実装側は `UIImage.FromBundle("images/shared.png")` と比較します。この結果が null または別インスタンスになると cache 所有を検出できず、`result` をリースへ残します。そのリースの破棄で共有 UIImage が無効化され、今回の不具合が再現します。

現在のテストも、キャッシュ検索へ渡す名前が変換されるケースを扱っていません。

**推奨修正**: MAUI の fallback と同じ `Path.GetFileNameWithoutExtension(fileName)` をキャッシュ照合キーに使用してください。併せて `images/shared.png` のようなディレクトリ・拡張子付き指定から `shared` を検索し、同一画像なら cache 所有と判定する回帰テストを追加してください。

## 確認結果

- Critical: 0
- Major: 1
- Minor: 0
- Suggestion: 0
- `proposal.md` とデルタスペックは `HEAD` から変更されておらず、足場凍結を維持
- `deviation.md` の所有権分類方式は合意済み差分として評価
- ホスト側の458件成功・両platformビルド成功を前提として採用
- ファイル変更およびテスト再実行は未実施

**最終判定: CHANGES_REQUESTED**

## 突き合わせ結果 (2026-08-25, ksn-orchestrator)

ホスト側: review-003.md (CHANGES_REQUESTED / Major 3・Minor 3・Suggestion 2)、verify-003.md (INVALID)

| # | 指摘 | 出典 | 採否 | 重要度 |
|---|---|---|---|---|
| 1 | キャッシュ引き直しが MAUI と違う名前 (`Path.GetFileNameWithoutExtension` を通さない生の fileName) で行われ、拡張子付き/ディレクトリ付き指定の cache 所有画像を facade 所有と誤分類し表示破壊が再発する | **双方一致** (相方は 10.0.70 実ソース、ホストは参照アセンブリの逆コンパイルで独立確証。オーケストレーターもローカル dotnet/maui ソース `ImageSourceExtensions.cs:66` で照合済み) | **確定** | Major |
| 2 | `File.Exists` 短絡が自己検証を飛ばして「破棄する側」を無検証で確定し、「誤分類は破棄しない側にだけ倒れる」不変条件が不成立 (復号不能フォールバック等で表示破壊側に倒れ得る) | ホストのみ | **採用** | Major |
| 3 | iOS 配線に検出力ゼロ (分類無効化ミューテーションで失敗 0 件)・facade 所有分岐の実行時証跡なし | ホストのみ (実測) | **採用** | Major |
| 4-6 | doc コメントの過大な断言 / 誤った契約を固定する単体テスト / 即時解放の前提未記載 | ホストのみ | **採用** | Minor |
| 7-8 | `CleanupFor` 例外時の result 取りこぼし / tasks 2.1・3.2 の読み替え 1 行 | ホストのみ | **採用** (対応は実装判断) | Suggestion |

降格・未解決なし。指摘 1 は前回 (code-001 指摘 4) の教訓に従い、採否確定前にオーケストレーターが事実主張を実ソースで検証した。
