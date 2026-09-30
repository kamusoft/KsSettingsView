---
id: 0029
title: MAUI のスクロール命令は、インターフェース型のハンドルを SettingsView が作って ViewModel へ渡して提供する
status: accepted
date: 2026-09-29
---

## Context

スクロール制御は、Store を経由せず Host につなぐ命令ハンドルで提供する (core/ADR-0037)。MAUI の facade (`SettingsView`) は BindableProperty だけを持ち、Handler へ命令を委譲する前例も無い。MAUI 標準の CollectionView / ScrollView は、部品のメソッド (`ScrollTo` / `ScrollToAsync`) で命令する流儀である。この流儀では、ViewModel から直接命令できない。

オーナーは、ViewModel が UI に直接依存せず、テスト容易性も確保できる形を求めた。先例として、KsSettingsView の利用者である ColorAnalyzer の `ICameraController` がある (`../ColorAnalyzer/ColorAnalyzer/Controls/CameraView/CameraView.cs`)。これはインターフェース型の BindableProperty を既定 OneWayToSource で持ち、View が作った実体を ViewModel へ渡す形である。

facade は gateway 経由でのみ Native に触れ (ADR-0009)、Host は Bridge が所有する (ADR-0005)。

前提: ViewModel が BindingContext を通じて SettingsView と結び付く MVVM の構成で使われること。

## Decision

MAUI のスクロール命令は、SettingsView にメソッドを生やさず、インターフェース型の BindableProperty で命令ハンドルを公開する。実体は SettingsView が作り、プロパティの既定の向きを View → ViewModel (OneWayToSource) にして ViewModel へ渡す。ViewModel はインターフェースだけを知り、実装クラスにも UI の部品にも依存しない。

命令は gateway → Bridge → Host につないだ Native のハンドルへ届く。Bridge が Native のハンドルを持ち、Host を作るたびにつなぎ直す。

含まないもの: 対象の指し方、Host が無い間の命令の扱いと準備完了の合図、型とプロパティの名前 (設計。出典の agenda の決定事項)。

## Alternatives Considered

- SettingsView にメソッドを生やす (MAUI 標準部品の流儀): ViewModel から命令するには、code-behind かイベントの中継が要るため却下。
- メソッドとハンドルの両方を用意する: 入口が 2 つになり、同時に使ったときの優先の決まりが要るため却下。
- ハンドルの型を実装クラスにする: ViewModel が UI の実装に依存し、テストで fake を差し込みにくくなるため却下 (オーナー判断)。
- ViewModel 側で実体を作って SettingsView へ渡す (KsCollectionView の ViewModel 所有と同じ向き): 実体を作る場所で実装クラスを知る必要があり (DI の登録か直接の生成)、自作の実装がプロパティに渡されたときの決まりも要るため却下。
- Handler から Host を直接操作する: gateway による Bridge 呼び出しの切り離し (ADR-0009) を破るため却下。

## Consequences

- 正: ViewModel はインターフェースだけで命令でき、テストでは fake で命令を確かめられる。DI の登録も要らない。
- 正: ColorAnalyzer の `ICameraController` と同じ書き方になる。
- 正: SettingsView がつなぐのは常に自分で作った実体なので、自作の実装が渡される組み合わせが起きない。
- 負: ハンドルは SettingsView ごとに 1 つで、1 つのハンドルを複数の画面で使い回せない。
- 負: SettingsView に結び付くまで ViewModel 側のハンドルは null であり、ViewModel は null を扱う必要がある。
- 負: MAUI 標準部品 (CollectionView.ScrollTo) の流儀とは書き方が違う。
- 負: Store 操作と 1:1 に対応しない Bridge の公開 API が増える。

## Revisit When

- 前提 (Context) が崩れたとき

---
出典: kasane/roadmaps/maui-support/phases/phase-8-scroll-control/history.md (2026-09-29: MAUI での命令の出し方 / MAUI の命令ハンドルの実体を誰が作るか / MAUI の Bridge までの経路と Host が無い間の命令) / ../ColorAnalyzer/ColorAnalyzer/Controls/CameraView/CameraView.cs
関連: core/ADR-0037 (Store を経由せず Host につなぐ命令ハンドル)
