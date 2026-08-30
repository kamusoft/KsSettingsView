# 一致検証: investigate-maui-icon-lease-sharing (004 回目)

**日付**: 2026-08-25
**判定**: VALID

読み替えの前提 (deviation.md の合意済み差分):

- 1 項目め — 「参照カウントで後片付けを遅延する」前提を「解決時の所有権分類 (キャッシュ所有の画像には後片付け口を付けない)」として読む
- [設計判断] 項 — 分類方法は「MAUI の分岐のミラー」ではなく「照合キーを揃えたキャッシュ引き直しと `ReferenceEquals` による実体同一性の確認のみ」。`File.Exists` 短絡は撤去、代償 (同名資産があるとキャッシュ常駐が 1 件増え得る) は受け入れ
- [読み替え] 項 — tasks 2.1 / 3.2 は所有権分類 (`KsFileImageOwnership`) と `FileImageOwnershipTests` で満たしたものとして読む

## Requirement 本文の対応

| 要求 | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| いずれかの解決結果の破棄で、表示中の他の解決結果の icon 表示を壊さない (SHALL NOT) | `maui/KsSettingsView.Maui/Internals/KsFileImageOwnership.cs:57-78` (照合キー `:70`、実体同一性 `:77`) / 配線 `maui/KsSettingsView.Maui/Platforms/iOS/KsImageResolver.cs:85-105` | `maui/KsSettingsView.Maui.Tests/FileImageOwnershipTests.cs:26` `:59` `:73` `:116` / `maui/KsSettingsView.Maui.Tests/IconSharingTests.cs:104` `:152` / `evidence/ios-wiring-before-after.txt` 第 3 節 (#1 #2 が同一 handle・修正前ミューテーションでの破壊再現と修正後の消滅) | ✅ 一致 (verify-003 ❌-1 解消) |
| 保護は解決口の世代交代をまたいで働く (SHALL) | 分類は解決ごとに完結し状態を持たない (`KsImageResolver.cs:85`) | `IconSharingTests.cs:80` (`Reconnect(renewImages: true)`) | ✅ 一致 |
| 保護は異なる SettingsView の間でも働く (SHALL) | 同上 (controller 間に共有状態が無い) | `IconSharingTests.cs:126` | ✅ 一致 |
| 同一画像を包むリースが残っている間、後片付けは 0 回 | キャッシュ所有と分類した結果には口を付けない (`KsImageResolver.cs:99`) | `IconSharingTests.cs:104` / `evidence` 第 2 節・第 3 節 #1 #2 | ✅ 一致 |
| 最後のリース破棄で保持していた全ての後片付け口を各 1 回実行し、以後保持を残さない | 該当なし — キャッシュ所有画像には口を保持しない (保持機構そのものが無い) | `IconSharingTests.cs:152` / `evidence` 第 2 節 (最後の Cell 除去後も handle 維持) | ⚠️ deviation 記録済み (読み替え) |
| 各リースの破棄は冪等で、多重破棄が多重実行・underflow を起こさない | `maui/KsSettingsView.Maui/Internals/KsImageLease.cs` の `_disposed` ガード (本 change では無変更) | 専用テストなし (注記 1) | ✅ 実装で担保 |
| 共有されていない画像の後片付けは、そのリースの破棄時に直ちに実行される | `KsImageResolver.cs:87-99` (file 以外と facade 所有 file は `result` をそのまま渡す) | `IconSharingTests.cs:223` `:278` / `evidence` 第 3 節 #3 #4 (除去時に handle が 0 へ) | ✅ 一致 (verify-003 ⚠️-2 解消) |
| 同一画像を受け取った再解決では差し替え配信を行わず、旧リースをその場で解放する (SHALL) | `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1633-1637` | `IconSharingTests.cs:26` (配信なし) / `:56` (即時解放) | ✅ 一致 |

## Scenario の対応

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 同一 Cell の再解決で新旧リースが同一画像を包む | `KsSettingsController.cs:1633-1637` | `IconSharingTests.cs:26` `:56` | ✅ 一致 |
| 解決口の世代交代をまたいだ共有 | `KsImageResolver.cs:85-105` (状態を持たない分類) | `IconSharingTests.cs:80` | ✅ 一致 |
| 2 つの Cell が同一画像を包む | `KsFileImageOwnership.cs:57-78` / `KsImageResolver.cs:99` | `IconSharingTests.cs:104` / 配信後に後片付けが走ることは `:278` | ✅ 一致 |
| 控えられない解決結果の即時破棄でも共有画像は守られる | 同上 (口を持たないリースの破棄は no-op) | `IconSharingTests.cs:183` (追い抜かれた解決 / 旧世代 / 登録解除済み Cell の 3 経路) | ✅ 一致 |
| 最後のリースの破棄で後片付けが実行される | 該当なし (キャッシュ所有には口を保持しない) / facade 所有は `KsImageResolver.cs:99` | `IconSharingTests.cs:152` `:223` / `evidence` 第 2 節・第 3 節 | ⚠️ deviation 記録済み (読み替え) |
| 非共有画像は従来どおり直ちに後片付けされる | `KsImageResolver.cs:87-99` | `IconSharingTests.cs:223` `:278` / `evidence` 第 3 節 #3 #4 | ✅ 一致 |

## verify-003 の未解消項目の追跡

| verify-003 | 状況 |
|---|---|
| ❌-1 キャッシュ所有の画像が facade 所有と分類され得る | **解消**。照合キーを `Path.GetFileNameWithoutExtension` へ揃え (MAUI 10.0.70 の `ImageSourceExtensions.GetPlatformImage(IFileImageSource)` と同形)、`File.Exists` 短絡を撤去。Simulator 実測で誤分類経路の消滅を確認 (`evidence` 第 3 節)。ミューテーション再実測でも照合キー戻しが 6 件・短絡復活が 7 件のテスト失敗として捕まる (前回は 0 件で素通り) |
| ⚠️-2 iOS 配線が自動テストからも実行時証跡からも守られていない | **証跡側は解消**。4 asset 種別の分類結果と後片付けの実行が実測された。自動テスト側は依然として配線 1 行の変異で 0 件失敗のままだが、platform 自動テストの導入可否は tasks 2.2 が実装フェーズの判断に委ねており、判断は「導入しない」で成立している (review-004 に Suggestion として記録) |
| ⚠️-3 リース破棄の冪等性を固定するテストが無い | **変わらず** (注記 1)。実装 (`KsImageLease` の `_disposed`) は本 change の変更対象外であり、対応する Scenario も無いため一致検証としては ✅ 扱い |

## 追加検査

- **tasks.md**: 全 8 タスクがチェック済み。2.1 / 3.2 の文言と実装の対応は deviation の [読み替え] 項で明示された → 虚偽チェックなし。2.2 の「配線が実際に効いていることの確認」は `evidence` 第 3 節で 4 asset 種別ぶん満たされた
- **逆流検査**: `proposal.md` と `specs/maui-cells/spec.md` は `HEAD` から無変更。`tasks.md` の差分はチェックボックスのみ。`exploration.md` の追記は tasks 1.1 / 1.2 / 4.1 が指示した記録
- **未記録乖離**: なし
- **付随修正**: `DisposeRetiredViews` / `DisposeRetired` の例外集約は deviation の `[付随修正]` として記録済み (テストは `IconSharingTests.cs:241` / `maui/KsSettingsView.Maui.Tests/CustomCellContentTests.cs:392`)。テスト足場の追加 (`GatewayScope.Reconnect(renewImages)` / `FakeImageResolver.CompleteCacheOwned`) は tasks 3.1 に対応。Scenario に対応しない未記録の同梱なし
- **テスト実行**: `maui/KsSettingsView.Maui.Tests` (net10.0) **465 件成功 / 0 失敗**。`maui/KsSettingsView.Maui` の `net10.0-ios` / `net10.0-android` ビルド成功 (警告 0)
- **UI 変更**: 本 change に `ui/` アーティファクトは無い (対象外)

### 注記 1

`KsImageLease` の破棄冪等性を直接固定するテストは、旧 `SharedImageRegistryTests` の撤去以降存在しない。Requirement 本文が挙げる不変条件ではあるが、対応する Scenario が無く、実装も本 change で触れていない既存コードのため一致検証上の欠落とはしない。テストで固定したい場合は数行で足せる。

## 判定

❌ なし。⚠️ 2 件はいずれも deviation.md 記録済みの読み替えに帰着するため **VALID**。
