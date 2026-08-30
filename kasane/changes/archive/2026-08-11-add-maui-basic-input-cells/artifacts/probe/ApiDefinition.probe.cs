// ---------------------------------------------------------------------------
// probe 専用の binding 定義 (add-maui-basic-input-cells / tasks 1.1〜1.5)。
// 検証が済んだら Swift 側の KsBridgeProbe.swift ごと削除する。
// ---------------------------------------------------------------------------

/// <summary>probe 1.1: `@objc` protocol を C# 側で実装するための binding。</summary>
[Protocol]
[Model]
[BaseType(typeof(NSObject))]
interface KsBridgeProbeDelegate
{
    /// <summary>cellID のみの通知。</summary>
    [Abstract]
    [Export("probeTapped:")]
    void ProbeTapped(string cellID);

    /// <summary>scalar 引数を伴う通知。</summary>
    [Abstract]
    [Export("probeSwitchChanged:isOn:")]
    void ProbeSwitchChanged(string cellID, bool isOn);

    /// <summary>配列引数を伴う通知。</summary>
    [Abstract]
    [Export("probeIndicesChanged:indices:")]
    void ProbeIndicesChanged(string cellID, NSNumber[] indices);

    /// <summary>文字列引数を伴う通知。</summary>
    [Abstract]
    [Export("probeTimeChanged:time:")]
    void ProbeTimeChanged(string cellID, string time);
}

/// <summary>probe 1.3〜1.5: 共通基底 DTO。</summary>
[BaseType(typeof(NSObject))]
[DisableDefaultCtor]
interface KsBridgeProbeCell
{
    /// <summary>基底の指定イニシャライザ。</summary>
    [Export("initWithTitle:")]
    NativeHandle Constructor(string title);

    /// <summary>基底が採番する Cell ID。</summary>
    [Export("cellID")]
    string CellID { get; }

    /// <summary>共通フィールド。</summary>
    [Export("title")]
    string Title { get; set; }

    /// <summary>platform 画像。</summary>
    [NullAllowed]
    [Export("icon", ArgumentSemantic.Strong)]
    UIImage Icon { get; set; }

    /// <summary>nullable scalar (boxed)。</summary>
    [NullAllowed]
    [Export("iconSize", ArgumentSemantic.Strong)]
    NSNumber IconSize { get; set; }

    /// <summary>nullable scalar (enum の序数)。</summary>
    [NullAllowed]
    [Export("uiStyle", ArgumentSemantic.Strong)]
    NSNumber UiStyle { get; set; }
}

/// <summary>probe 1.4: 基底の派生 A。基底の init を再宣言して継承させる。</summary>
[BaseType(typeof(KsBridgeProbeCell))]
[DisableDefaultCtor]
interface KsBridgeProbeLabelCell
{
    /// <summary>基底から継承した指定イニシャライザ。</summary>
    [Export("initWithTitle:")]
    NativeHandle Constructor(string title);
}

/// <summary>probe 1.4: 基底の派生 B (固有 init を持つ)。</summary>
[BaseType(typeof(KsBridgeProbeCell))]
[DisableDefaultCtor]
interface KsBridgeProbeSwitchCell
{
    /// <summary>派生固有の指定イニシャライザ。</summary>
    [Export("initWithTitle:isOn:")]
    NativeHandle Constructor(string title, bool isOn);

    /// <summary>派生固有フィールド。</summary>
    [Export("isOn")]
    bool IsOn { get; set; }
}

/// <summary>probe の入口。</summary>
[BaseType(typeof(NSObject))]
interface KsBridgeProbe
{
    /// <summary>delegate の弱参照 (ObjC 慣例)。</summary>
    [NullAllowed]
    [Export("delegate", ArgumentSemantic.Weak)]
    NSObject WeakDelegate { get; set; }

    /// <summary>型付き delegate。</summary>
    [Wrap("WeakDelegate")]
    [NullAllowed]
    KsBridgeProbeDelegate Delegate { get; set; }

    /// <summary>基底型の配列で異種 DTO を受け渡す。</summary>
    [Export("cells", ArgumentSemantic.Copy)]
    KsBridgeProbeCell[] Cells { get; set; }

    /// <summary>基底型の引数で異種 DTO を受け取る。</summary>
    [Export("addCell:")]
    string AddCell(KsBridgeProbeCell cell);

    /// <summary>Swift 側で判別・読み出しした結果を返す。</summary>
    [Export("describeCells")]
    string DescribeCells();

    /// <summary>delegate へ 4 種の通知を発火する。</summary>
    [Export("fireAll")]
    void FireAll();

    /// <summary>delegate が生きているか。</summary>
    [Export("hasDelegate")]
    bool HasDelegate { get; }
}
