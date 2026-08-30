# 一致検証: investigate-maui-icon-lease-sharing (003 回目)

**日付**: 2026-08-25
**判定**: INVALID

読み替えの前提: deviation.md の先頭項により、デルタスペックの「参照カウントで後片付けを遅延する」前提は「解決時の所有権分類 (キャッシュ所有の画像には後片付け口を付けない)」として読む。以下の対応表はこの読み替え後の意味で突き合わせている。

## Requirement 本文の対応

| 要求 | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| いずれかの解決結果の破棄で、表示中の他の解決結果の icon 表示を壊さない (SHALL NOT) | `maui/KsSettingsView.Maui/Internals/KsFileImageOwnership.cs:40-62` / 配線 `maui/KsSettingsView.Maui/Platforms/iOS/KsImageResolver.cs:85-99` | `maui/KsSettingsView.Maui.Tests/FileImageOwnershipTests.cs` / `maui/KsSettingsView.Maui.Tests/IconSharingTests.cs:104` `:152` / `evidence/ios-wiring-before-after.txt` | ❌ 乖離 (下記 ❌-1) |
| 保護は解決口の世代交代をまたいで働く (SHALL) | 分類は解決ごとに完結し状態を持たない (`KsImageResolver.cs:85`) | `IconSharingTests.cs:80` (`Reconnect(renewImages: true)`) | ✅ 一致 |
| 保護は異なる SettingsView の間でも働く (SHALL) | 同上 (controller 間に共有状態が無い) | `IconSharingTests.cs:126` | ✅ 一致 |
| 同一画像を包むリースが残っている間、後片付けは 0 回 | キャッシュ所有と分類した結果には口を付けない (`KsImageResolver.cs:98`) | `IconSharingTests.cs:104` / `evidence/ios-wiring-before-after.txt` (片方除去後も handle 維持) | ✅ 一致 (❌-1 の入力クラスを除く) |
| 最後のリース破棄で保持していた全ての後片付け口を各 1 回実行し、以後保持を残さない | 該当なし — キャッシュ所有画像には口を保持しない (registry 撤去で保持機構自体が無い) | `IconSharingTests.cs:152` / `evidence` 再実測 (最後の Cell 除去後も handle 維持) | ⚠️ deviation 記録済み (読み替え) |
| 各リースの破棄は冪等で、多重破棄が多重実行・underflow を起こさない | `maui/KsSettingsView.Maui/Internals/KsImageLease.cs` (`_disposed` ガード、本 change では無変更) | 直接固定するテストは無し (旧 `SharedImageRegistryTests` の撤去で消滅) | ⚠️ 実装で担保・専用テストなし |
| 共有されていない画像の後片付けは、そのリースの破棄時に直ちに実行される | `KsImageResolver.cs:87-98` (file 以外と facade 所有 file は `result` をそのまま渡す) | `IconSharingTests.cs:223` `:278` (Fake 経由) | ⚠️ 配線の検出力ゼロ (下記 ⚠️-2) |
| 同一画像を受け取った再解決では差し替え配信を行わず、旧リースをその場で解放する (SHALL) | `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1630-1634` | `IconSharingTests.cs:26` (配信なし) / `:56` (即時解放) | ✅ 一致 |

## Scenario の対応

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 同一 Cell の再解決で新旧リースが同一画像を包む | `KsSettingsController.cs:1630-1634` | `IconSharingTests.cs:26` `:56` | ✅ 一致 |
| 解決口の世代交代をまたいだ共有 | `KsImageResolver.cs:85-99` (状態を持たない分類) | `IconSharingTests.cs:80` | ✅ 一致 |
| 2 つの Cell が同一画像を包む | `KsFileImageOwnership.cs:40-62` / `KsImageResolver.cs:98` | `IconSharingTests.cs:104` / 配信後に後片付けが走ることは `:278` | ✅ 一致 (❌-1 の入力クラスを除く) |
| 控えられない解決結果の即時破棄でも共有画像は守られる | 同上 (口を持たないリースの破棄は no-op) | `IconSharingTests.cs:183` (追い抜かれた解決 / 旧世代 / 登録解除済み Cell の 3 経路) | ✅ 一致 |
| 最後のリースの破棄で後片付けが実行される | 該当なし (キャッシュ所有には口を保持しない) / facade 所有は `KsImageResolver.cs:98` | `IconSharingTests.cs:152` `:223` / `evidence` 再実測 | ⚠️ deviation 記録済み (読み替え) |
| 非共有画像は従来どおり直ちに後片付けされる | `KsImageResolver.cs:87-98` | `IconSharingTests.cs:223` `:278` | ⚠️ 配線の検出力ゼロ (⚠️-2) |

## ❌ / ⚠️ の内訳

### ❌-1 キャッシュ所有の画像が facade 所有と分類され得る (未記録の乖離)

参照している Microsoft.Maui.Core 10.0.70 の iOS 実装は、file 画像のフォールバックで `Path.GetFileNameWithoutExtension(file)` を使って `imageNamed:` を引く。一方 `KsImageResolver.cs:96` は `IFileImageSource.File` を素のまま `UIImage.FromBundle` へ渡すため、拡張子付き・ディレクトリ付きの指定 (例: asset catalog の imageset `logo` を `ImageSource.FromFile("logo.png")` で指定) では照合が空振りし、キャッシュ所有の共有画像に後片付け口が付く。この状態で `StoreIcon` の即時解放が走ると、表示中の共有 UIImage が破棄される — Requirement 本文の SHALL NOT と「リースが残っている間 後片付け 0 回」に反する入力クラスが残っている。

また `KsFileImageOwnership.cs:56-59` の実ファイル短絡は自己検証を経ずに「破棄する側」を確定するため、MAUI が復号失敗でフォールバックした場合 (10.0.70 の `FileImageSourceService` は復号失敗を例外にせずフォールバックする) にも同じ向きの誤分類が起こる。

deviation.md には「誤分類は破棄しない側にだけ倒す」と記録されているが、上記 2 経路はいずれも逆向きに倒れるため、**記録済みの乖離では説明できない**。

**見立て**: 実装を直すべき。引き直しの名前を MAUI の解決に合わせ、短絡が安全側にしか倒さない形にする (詳細は `review-003.md` の Major 1 / Major 2)。復号失敗経路の残余だけは、修正後も残るなら deviation への記録で合意する余地がある。

### ⚠️-2 iOS 配線が自動テストからも実行時証跡からも守られていない

`KsImageResolver.CleanupFor` の分類を無効化する変異 (本 change 以前の欠陥へ戻す) を入れてもテストは 458 件全成功のままで、失敗は 0 件だった。実行時証跡 `evidence/ios-wiring-before-after.txt` はキャッシュ所有の枝のみを押さえており、facade 所有と分類された画像が実際に後片付けされることは実環境で確認されていない。「非共有画像は従来どおり直ちに後片付けされる」の実装対応は存在するが、その配線が効いていることの担保は無い。

**見立て**: 実装 (テスト・証跡) を足すべき。分類結果を asset 種別ごとに Simulator で観測して `evidence/` に追記するのが最小の埋め方。

### ⚠️-3 リース破棄の冪等性を固定するテストが無い

`KsImageLease` の `_disposed` ガードで担保されているが、旧 `SharedImageRegistryTests` の撤去により直接固定するテストが残っていない。`KsImageLease` 自体は本 change の変更対象外のため乖離とはしないが、Requirement が明示する不変条件が無防備になっている点は記録しておく。

**見立て**: 数行のテスト追加で閉じる (lessons process L-005 の範囲)。

## 追加検査

- **tasks.md**: 全 8 タスクがチェック済み。2.1 / 3.2 は「参照カウント機構」とその不変条件を要求する文言のままだが、deviation.md の設計変更記録により所有権分類 (`KsFileImageOwnership` + `FileImageOwnershipTests`) が代替物として実装されている → **虚偽チェックとはしない** (⚠️ 読み替え。蒸留時の誤読を避けるため deviation への一行追記を推奨)。2.2 の「配線が実際に効いていることの確認」はキャッシュ所有の枝のみ (⚠️-2)
- **逆流検査**: `proposal.md` と `specs/maui-cells/spec.md` は無変更。`tasks.md` の差分はチェックボックスのみ。`exploration.md` の追記は tasks 1.1 / 1.2 / 4.1 が指示した記録であり足場の書き換えではない → **逆流なし**
- **未記録乖離**: ❌-1 (上記)
- **付随修正**: `DisposeRetiredViews` / `DisposeRetired` の例外集約は deviation.md の `[付随修正]` として記録済み。対応テストは `IconSharingTests.cs:241` と `maui/KsSettingsView.Maui.Tests/CustomCellContentTests.cs:392` にある。`GatewayScope.Reconnect(renewImages)` と `FakeImageResolver.CompleteCacheOwned` は tasks 3.1 のテスト足場整備に対応 → 未記録の同梱なし
- **テスト実行**: `maui/KsSettingsView.Maui.Tests` (net10.0) 458 件成功 / 0 失敗。`maui/KsSettingsView.Maui` の `net10.0-ios` ビルド成功 (警告 0)
- **UI 変更**: 本 change に `ui/` アーティファクトは無い (対象外)

## 判定

❌ が 1 件 (未記録の乖離) あるため **INVALID**。⚠️-2 / ⚠️-3 は Requirement の担保が薄い箇所として併記する。
