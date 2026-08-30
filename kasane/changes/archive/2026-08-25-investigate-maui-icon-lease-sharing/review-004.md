# レビュー結果: investigate-maui-icon-lease-sharing (004 回目)

**日付**: 2026-08-25
**判定**: APPROVED

## サマリー

review-003 / second-opinion-code-003 の全 8 指摘に対応が入り、いずれも解消を確認した。とりわけ核心だった「照合キーが MAUI と違う」(Major 1) は `Path.GetFileNameWithoutExtension` を純ロジック側へ移して MAUI 10.0.70 の `ImageSourceExtensions.GetPlatformImage(IFileImageSource)` と完全に同形になり、`File.Exists` 短絡の撤去 (Major 2) によって分類は「実体の同一性だけを見る」形へ収束した。**分岐を推し量る実装をやめたこと**で、誤分類が表示破壊側へ倒れる経路が構造的に消えている — キャッシュがその名前でその実体を持っていないと確認できたときにだけ後片付け口を付ける形なので、判定の材料が欠けた場合も分類が失敗した場合も安全側 (口を付けない) に倒れる。

証跡も埋まった。`evidence/ios-wiring-before-after.txt` の第 3 節が 4 つの asset 種別について「分類結果 / 後片付け口の有無 / 全 Cell 除去時に handle が 0 になるか」を実測しており、拡張子付き asset catalog (`probe_asset.png`) が拡張子なし (`probe_asset`) と**同一 handle** であること、修正前ミューテーションではそこに後片付け口が付いて実際に破壊されること、修正後はその経路が消えることまで実物で押さえている。review-003 Major 3 が求めた内容をそのまま満たす。

残りは優先度の低い Suggestion 3 件のみで、いずれも本 change でのブロック要因にはしない。

## 指摘ごとの解消状況

| # | 指摘 (review-003 / 相方) | 重要度 | 状況 | 確認内容 |
|---|---|---|---|---|
| 1 | 照合キーが MAUI と違う名前 | Major | ✅ 解消 | `maui/KsSettingsView.Maui/Internals/KsFileImageOwnership.cs:70` で `Path.GetFileNameWithoutExtension`。delegate (`KsImageResolver.cs:96`) は生の `imageNamed:` のみを担う分担で、MAUI 10.0.70 の実装と同形。ミューテーション再実測: 生名へ戻すと **6 件失敗** (前回は 0 件で素通りしていた欠陥) |
| 2 | `File.Exists` 短絡が「破棄する側」を無検証で確定 | Major | ✅ 解消 | 引数ごと撤去。`ClassificationNeverSkipsTheCacheLookup` (`FileImageOwnershipTests.cs:116`) が「照合を飛ばさない」を固定。ミューテーション: 短絡 (拡張子付きは照合を飛ばす) を復活させると **7 件失敗**。復号不能ケースの誤評価撤回も deviation の [設計判断] 項に記録済み |
| 3 | iOS 配線の検出力ゼロ / facade 所有分岐の実行時証跡なし | Major | ✅ 解消 | `evidence/ios-wiring-before-after.txt` 第 3 節。4 種別 (asset catalog 拡張子なし / 拡張子付き / bundle 直下 png / MauiImage 生成 png) の分類結果と後片付けの実行、および修正前ミューテーションでの表示破壊再現と修正後の消滅を実測。facade 所有 (#3 #4) は除去時に handle が 0 へ落ちることまで確認されており、「非共有画像は直ちに後片付け」に実測が付いた |
| 4 | doc コメントが「新たな読み込みを起こさない」と断言 | Minor | ✅ 解消 | `KsFileImageOwnership.cs:45-50` が「解決がキャッシュ経由なら追加の読み込みにならない / ファイルから起こした画像なら新規の読み込みと常駐を起こし得る」と実態どおりに書き分けている |
| 5 | 単体テストが誤った契約 (生名) を固定 | Minor | ✅ 解消 | `TheCacheIsQueriedWithTheStrippedName` が `logo.png` / `images/logo.png` / `Resources/Images/logo.svg` → `logo` をパラメータ化で固定。拡張子付き・ディレクトリ付きのキャッシュ所有検出も個別テストで押さえている |
| 6 | 即時解放の前提が未記載 | Minor | ✅ 解消 | `KsSettingsController.cs:1602-1606` の remarks に「共有され得るのはキャッシュ所有の画像だけ」「分類を誤ればこの解放が表示を壊す側に働く」と明記。`KsFileImageOwnership.cs:34-38` からも同じ前提を書いている |
| 7 | `CleanupFor` 例外時の result 取りこぼし | Suggestion | ✅ 解消 | `KsImageResolver.cs:92-104` で try 化し、失敗時は口を付けない側 (null) へ倒す。安全側の統一という点で当初の推奨より良い |
| 8 | tasks 2.1 / 3.2 の読み替え | Suggestion | ✅ 解消 | deviation.md に [読み替え] 項を追加 (足場は無変更) |

## 新規指摘

### [🔵 Suggestion] 短絡撤去の代償が記録のみで、量が測られていない

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsFileImageOwnership.cs:45-50` / `kasane/changes/investigate-maui-icon-lease-sharing/deviation.md` の [設計判断] 項

**問題点**: 短絡が無くなったことで、facade 所有の画像 (MauiImage / MauiAsset の png など) を解決するたびに `imageNamed:` の引き直しが走る。bundle 直下に同名資産がある典型的な配置ではこの引き直しが名前付き画像キャッシュへ 1 件常駐を増やすため、icon の異なる名前の数だけ実体が二重に載る。UIKit 管理で purge されるため上限は付くが、実測はされていない (トレードオフの受け入れは deviation に記録済みなので合意違反ではない)。

**推奨対応**: 対応は任意。第 3 節の計測ハーネスを再度組むことがあれば、引き直し由来のキャッシュ常駐が実際にどの程度かを 1 行足しておくと、後から「この代償は許容範囲だったか」を再評価できる。

### [🔵 Suggestion] iOS 配線そのものは依然 CI から守られていない

**該当箇所**: `maui/KsSettingsView.Maui/Platforms/iOS/KsImageResolver.cs:99` (`return cacheOwned ? null : result;`)

**問題点**: ミューテーション再実測で、この 1 行を無効化して本 change 以前の欠陥へ戻しても **失敗テストは 0 件**のまま (net10.0 のテストは `Platforms/iOS` を含まないため)。分類の中身 (`KsFileImageOwnership`) は 6〜13 件の検出力を持つようになったが、それを配線している最後の 1 行は実行時証跡でしか守られておらず、証跡ハーネスは計測後に撤去されている。tasks 2.2 が「platform 自動テストの導入可否は実装フェーズで判断」としている以上、本 change での対応は不要と判断する。

**推奨対応**: 本 change では対応不要。将来 iOS の platform テスト基盤を持つ機会があれば、この配線を最初の対象に含めるとよい。

### [🔵 Suggestion] deviation の初出項に、撤回済みの機構説明が残っている

**該当箇所**: `kasane/changes/investigate-maui-icon-lease-sharing/deviation.md` 1 項目め

**問題点**: 初出項は「cache 所有 (MAUI `FileImageSourceService` の FromBundle フォールバック分岐 = `File.Exists` false) を分類し」「分類の自己検証: `File.Exists` false 分岐では…」と、撤回済みの判定方法で機構を説明している。[設計判断] 項が「起草時は…想定していた → 変更」と明示的に上書きしているので記録としては追えるが、蒸留で初出項だけを読むと誤った機構が concepts / ADR へ流れ得る。

**推奨対応**: [設計判断] 項の冒頭に「初出項の判定方法の記述はこの項で置き換わる」の一言を足すか、蒸留時に初出項を読まない旨を明記する。

## 確認した観点 (指摘に至らなかったもの)

- **ビルドとテスト**: `net10.0-ios` / `net10.0-android` ビルド成功 (警告 0)、テスト 465 件成功 / 0 失敗
- **ミューテーション再実測** (lessons code-review L-001。使用した一時変更は backup と shasum 一致で原状復帰を確認済み):

  | 変異 | 失敗テスト |
  |---|---|
  | 照合キーを生のファイル名へ戻す (Major 1 の欠陥を再現) | 6 件 ✅ |
  | 分類を常に facade 所有へ倒す | 13 件 ✅ |
  | 短絡 (拡張子付きは照合を飛ばす) を復活させる | 7 件 ✅ |
  | iOS 配線の分類を無効化する | 0 件 (上の Suggestion) |

- **短絡撤去による回帰**: 「非共有画像は直ちに後片付け」は `IconSharingTests.cs:223` `:278` に加えて evidence 第 3 節 #3 #4 (除去時に handle が 0 へ) で実測されており維持。facade 所有側が誤ってキャッシュ所有と判定される方向は、`ReferenceEquals` が別実体に対して真になり得ないため構造上起こらない
- **判定不能入力の扱い**: `null` / `""` / `".png"` / `"images/"` はいずれも「キャッシュ所有 = 破棄しない」へ倒れる (`FileImageOwnershipTests.cs:141`)。`Path.GetFileNameWithoutExtension` が空を返す入力を別扱いにしている点も込みで安全側に揃っている
- **キャッシュ purge との競合**: 「MAUI の解決と分類の引き直しの間にキャッシュが purge され、別実体が返って誤分類する」経路を検討したが、両呼び出しは同一の同期継続の中にあり、メモリ警告 (main runloop 配送) が割り込む余地がないため実害なしと判断した
- **例外時の安全側**: `CleanupFor` の catch は口を付けない側へ倒すため、分類の失敗が表示破壊にはならない (代わりにその画像は誰も破棄しない — 記録済みの失敗方針と一致)
- **Android 非影響**: 分類は iOS の `KsImageResolver` に閉じており、Android 側 resolver は無変更
- **registry 撤去の残骸**: `maui/` 配下のソース・テストに `KsSharedImageRegistry` / `SharedImageRegistryTests` への参照なし
- **足場の逆流なし**: `proposal.md` / `specs/maui-cells/spec.md` は無変更。`tasks.md` はチェックボックスのみ、`exploration.md` は tasks 1.1 / 1.2 / 4.1 が指示した記録の追記のみ
- **lint**: comment-policy / local-path / identity いずれも 0 件

## アクションプラン

ブロックする指摘なし。Suggestion 3 件はいずれも任意で、蒸留時に deviation の [設計判断] 項を正として扱えば足りる。
