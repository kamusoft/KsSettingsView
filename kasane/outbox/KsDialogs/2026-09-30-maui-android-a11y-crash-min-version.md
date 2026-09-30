---
from: KsSettingsView
to: KsDialogs
kind: change
date: 2026-09-30
source: kasane/changes/archive/2026-09-30-add-scroll-control/
---

# MAUI の Android でアクセシビリティ経由のクラッシュが出たため、MAUI の最低版を 10.0.71 に上げた

## 何が起きたか

MAUI の Android Sample に uiautomator の dump (アクセシビリティ経由の問い合わせ) を掛けると、アプリが落ちた。TalkBack を有効にして Cell の並ぶ画面を開いたときも同じだった。例外は次のとおり。

```
System.MissingMethodException: Method not found: void AndroidX.Core.View.Accessibility.AccessibilityNodeInfoCompat.set_Checked(bool)
```

呼び出し元は MAUI の `MauiAccessibilityDelegateCompat.OnInitializeAccessibilityNodeInfo` だった。

## 原因

版の組み合わせで起きる。

- こちらの binding の依存 (Compose 1.11.4・AppCompat 1.7.1.4・ConstraintLayout 2.2.1.6・RecyclerView 1.4.0.6 ほか) が、`Xamarin.AndroidX.Core` を 1.19.0.1 へ引き上げる。MAUI 10.0.70 自身の依存だけなら 1.16.0.3 で済む
- `Xamarin.AndroidX.Core` 1.17 以降の binding では、`AccessibilityNodeInfoCompat.Checked` が `bool` から `int` に変わった
- `Microsoft.Maui.dll` 10.0.70 は `set_Checked(bool)` を呼ぶ。10.0.71 以降はこの呼び出しを持たない
- MAUI の Android 向け assembly が参照する AndroidX / MAUI のメンバーを、解決後の assembly の定義と照合した。10.0.70 は欠落 1 件 (この `set_Checked`)、10.0.71 は 0 件だった

binding を依存に持つ NuGet 利用者のアプリにも同じ組み合わせが届くので、ライブラリ側の依存の宣言で直した。

## こちらで直したこと

- `maui/Directory.Packages.props` の `Microsoft.Maui.Controls` を 10.0.70 から 10.0.71 に上げた。facade の依存の下限も上がるので、利用者のアプリに必要な MAUI の最低版は 10.0.71 になる (10.0.70 に固定したアプリは restore が NU1605 で止まる)
- Sample の `MauiVersion` と、消費者検証アプリ `verification/maui/VerificationApp.csproj` も 10.0.71 にした (下限を上げると 10.0.70 のままでは restore が止まるため)
- `Microsoft.Maui.Core` だけを 10.0.71 に宣言して Controls 10.0.70 を許す案もあったが、未検証の版の混在になるため採らなかった

## 相手に関係する理由

そちらは MAUI 10.0.20 で、Android の binding が同じ系統の AndroidX (Compose など) に依存している。解決後の `Xamarin.AndroidX.Core` が 1.17 以降になっていれば、同じクラッシュが TalkBack の利用者のアプリで起きうる。確かめる手順:

1. MAUI の Android Sample をフルビルド (bin/obj を捨てて) して Emulator に入れ、`adb -s <シリアル> shell uiautomator dump` を掛けて、アプリが落ちるか見る
2. 落ちるなら、restore 後の `obj/project.assets.json` で `Xamarin.AndroidX.Core` の解決版を確かめる

あわせて、iOS Sample の Cell の通知の閉包 (`@Sendable`) から SwiftUI の `@State` を書き換えるところは、`MainActor.assumeIsolated { ... }` で包む形に全画面をそろえた (Swift 6 の concurrency 警告が 58 件から 0 件)。通知の閉包がメインスレッドから呼ばれることは、本体の公開 doc に明記した。同じ書き方の Sample があれば参考になると思う。
