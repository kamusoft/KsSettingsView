using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using KsSettingsView.Tests.Fakes;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsSettingsView.Tests;

/// <summary>
/// IconSource が platform の画像へ解決され、解決できた時点で内容更新として表示へ反映されることを
/// 確認する。解決そのものは fake の解決口で模擬する。
/// </summary>
[TestFixture]
public class IconSourceTests
{
    /// <summary>接続前に設定した画像は、接続後に解決されて輸送内容へ載る。</summary>
    [Test]
    public void IconSetBeforeConnectIsResolvedOnConnect()
    {
        ImageSource source = ImageSource.FromFile("icon.png");
        LabelCell cell = new() { IconSource = source };
        SettingsView view = new() { Root = { new Section { Cells = { cell } } } };

        GatewayScope scope = GatewayScope.Connect(view).Reset();
        Assert.That(scope.Gateway.IconOf(cell), Is.Null);

        object icon = new();
        scope.Images.Complete(source, icon);
        scope.Flush();

        Assert.That(scope.Gateway.IconOf(cell), Is.SameAs(icon));
        Assert.That(scope.Single<GatewayCall.ReplaceCell>().NewCell, Is.SameAs(cell));
    }

    /// <summary>解決が終わるまでは icon なしのまま、内容更新も起きない。</summary>
    [Test]
    public void PendingResolutionPublishesNothing()
    {
        LabelCell cell = new() { IconSource = ImageSource.FromFile("icon.png") };
        SettingsView view = new() { Root = { new Section { Cells = { cell } } } };

        GatewayScope scope = GatewayScope.Connect(view).Reset();
        scope.Flush();

        Assert.That(scope.Images.Pending, Has.Count.EqualTo(1));
        Assert.That(scope.Gateway.IconOf(cell), Is.Null);
        Assert.That(scope.All<GatewayCall.ReplaceCell>(), Is.Empty);
    }

    /// <summary>画像の差し替えは解決し直され、新しい画像が載る。</summary>
    [Test]
    public void ChangingIconResolvesAgain()
    {
        ImageSource first = ImageSource.FromFile("first.png");
        LabelCell cell = new() { IconSource = first };
        SettingsView view = new() { Root = { new Section { Cells = { cell } } } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        object firstIcon = new();
        scope.Images.Complete(first, firstIcon);
        scope.Flush();

        ImageSource second = ImageSource.FromFile("second.png");
        cell.IconSource = second;
        object secondIcon = new();
        scope.Images.Complete(second, secondIcon);
        scope.Flush();

        Assert.That(scope.Gateway.IconOf(cell), Is.SameAs(secondIcon));
    }

    /// <summary>画像を外すと icon なしになり、内容更新として配信される。</summary>
    [Test]
    public void ClearingIconRemovesIt()
    {
        ImageSource source = ImageSource.FromFile("icon.png");
        LabelCell cell = new() { IconSource = source };
        SettingsView view = new() { Root = { new Section { Cells = { cell } } } };
        GatewayScope scope = GatewayScope.Connect(view);

        scope.Images.Complete(source, new object());
        scope.Flush();
        scope.Reset();

        cell.IconSource = null;
        scope.Flush();

        Assert.That(scope.Gateway.IconOf(cell), Is.Null);
        Assert.That(scope.Single<GatewayCall.ReplaceCell>().NewCell, Is.SameAs(cell));
    }

    /// <summary>解決が追い抜かれた場合、最後に設定した画像だけが表示に残る。</summary>
    [Test]
    public void LatestIconSourceWinsRegardlessOfCompletionOrder()
    {
        ImageSource first = ImageSource.FromFile("first.png");
        LabelCell cell = new() { IconSource = first };
        SettingsView view = new() { Root = { new Section { Cells = { cell } } } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        ImageSource second = ImageSource.FromFile("second.png");
        cell.IconSource = second;

        object secondIcon = new();
        scope.Images.Complete(second, secondIcon);
        scope.Images.Complete(first, new object());
        scope.Flush();

        Assert.That(scope.Gateway.IconOf(cell), Is.SameAs(secondIcon));
    }

    /// <summary>解決に失敗した画像は icon なしとして確定する。</summary>
    [Test]
    public void FailedResolutionFallsBackToNoIcon()
    {
        ImageSource source = ImageSource.FromFile("broken.png");
        LabelCell cell = new() { IconSource = source };
        SettingsView view = new() { Root = { new Section { Cells = { cell } } } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        scope.Images.Complete(source, null);
        scope.Flush();

        Assert.That(scope.Gateway.IconOf(cell), Is.Null);
        Assert.That(scope.All<GatewayCall.ReplaceCell>(), Is.Empty);
    }

    /// <summary>Native Host の解放をまたいだ解決の結果は採用されない。</summary>
    [Test]
    public void ResolutionFromReleasedHostIsDiscarded()
    {
        ImageSource source = ImageSource.FromFile("icon.png");
        LabelCell cell = new() { IconSource = source };
        SettingsView view = new() { Root = { new Section { Cells = { cell } } } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        view.ReleaseHost();
        scope.Images.Complete(source, new object());
        scope.Flush();

        Assert.That(scope.Gateway.IconOf(cell), Is.Null);
    }

    /// <summary>再接続では現在の画像が新しい解決口で解決し直される。</summary>
    [Test]
    public void ReconnectResolvesCurrentIconAgain()
    {
        ImageSource source = ImageSource.FromFile("icon.png");
        LabelCell cell = new() { IconSource = source };
        SettingsView view = new() { Root = { new Section { Cells = { cell } } } };
        GatewayScope scope = GatewayScope.Connect(view);

        view.ReleaseHost();
        FakeImageResolver.PendingRequest stale = scope.Images.Pending.Single();
        scope.Reconnect();
        scope.Reset();

        object icon = new();
        scope.Images.Complete(source, icon);

        // 解放前に始まっていた解決が後から完了しても、新しい解決の結果を上書きしない。
        scope.Images.Complete(stale, new object());
        scope.Flush();

        Assert.That(scope.Gateway.IconOf(cell), Is.SameAs(icon));
        Assert.That(scope.Single<GatewayCall.ReplaceCell>().NewCell, Is.SameAs(cell));
    }

    /// <summary>除去済みの Cell に対する解決の完了は捨てられる。</summary>
    [Test]
    public void ResolutionForRemovedCellIsDiscarded()
    {
        ImageSource source = ImageSource.FromFile("icon.png");
        LabelCell cell = new() { IconSource = source };
        Section section = new() { Cells = { cell } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        section.Cells.Remove(cell);
        scope.Images.Complete(source, new object());
        scope.Flush();

        Assert.That(scope.Gateway.IconOf(cell), Is.Null);
        Assert.That(scope.All<GatewayCall.ReplaceCell>(), Is.Empty);
    }

    /// <summary>
    /// Cell を外して入れ直しても、外す前に始まっていた解決が後の解決を上書きしない。
    /// </summary>
    /// <remarks>
    /// 外す前の解決を複数残しておくのは、要求の識別が Cell ごとに数え直されると入れ直した後の
    /// 要求と番号がぶつかるため。ぶつかる番号がどれかに依らず検出できるよう、残した解決を
    /// すべて完了させて最後の指定が残ることを見る。
    /// </remarks>
    [Test]
    public void ResolutionsStartedBeforeReRegistrationDoNotWin()
    {
        ImageSource initial = ImageSource.FromFile("initial.png");
        LabelCell cell = new() { IconSource = initial };
        Section section = new() { Cells = { cell } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        foreach (string name in new[] { "stale-1.png", "stale-2.png", "stale-3.png" })
        {
            cell.IconSource = ImageSource.FromFile(name);
        }

        FakeImageResolver.PendingRequest[] stale = [.. scope.Images.Pending];
        Assert.That(stale, Is.Not.Empty);

        section.Cells.Remove(cell);
        cell.IconSource = ImageSource.FromFile("readded.png");
        section.Cells.Add(cell);

        cell.IconSource = ImageSource.FromFile("next.png");
        ImageSource latest = ImageSource.FromFile("latest.png");
        cell.IconSource = latest;

        object latestIcon = new();
        scope.Images.Complete(latest, latestIcon);
        foreach (FakeImageResolver.PendingRequest request in stale)
        {
            scope.Images.Complete(request, new object());
        }

        scope.Flush();

        Assert.That(scope.Gateway.IconOf(cell), Is.SameAs(latestIcon));
    }

    /// <summary>画像を差し替えると、前の解決結果の後片付けが行われる。</summary>
    [Test]
    public void ReplacingIconDisposesPreviousResult()
    {
        ImageSource first = ImageSource.FromFile("first.png");
        LabelCell cell = new() { IconSource = first };
        SettingsView view = new() { Root = { new Section { Cells = { cell } } } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        FakeImageResolver.DisposeProbe firstProbe = scope.Images.CompleteTracked(first, new object());
        scope.Flush();

        ImageSource second = ImageSource.FromFile("second.png");
        cell.IconSource = second;
        object secondIcon = new();
        FakeImageResolver.DisposeProbe secondProbe = scope.Images.CompleteTracked(second, secondIcon);
        scope.Flush();

        Assert.That(firstProbe.IsDisposed, Is.True);
        Assert.That(secondProbe.IsDisposed, Is.False);
        Assert.That(scope.Gateway.IconOf(cell), Is.SameAs(secondIcon));
    }

    /// <summary>画像を外すと、控えていた解決結果の後片付けが行われる。</summary>
    [Test]
    public void ClearingIconDisposesResolvedResult()
    {
        ImageSource source = ImageSource.FromFile("icon.png");
        LabelCell cell = new() { IconSource = source };
        SettingsView view = new() { Root = { new Section { Cells = { cell } } } };
        GatewayScope scope = GatewayScope.Connect(view);

        FakeImageResolver.DisposeProbe probe = scope.Images.CompleteTracked(source, new object());
        scope.Flush();

        cell.IconSource = null;
        scope.Flush();

        Assert.That(probe.IsDisposed, Is.True);
    }

    /// <summary>Cell を外すと、控えていた解決結果の後片付けが行われる。</summary>
    [Test]
    public void RemovingCellDisposesResolvedResult()
    {
        ImageSource source = ImageSource.FromFile("icon.png");
        LabelCell cell = new() { IconSource = source };
        Section section = new() { Cells = { cell } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);

        FakeImageResolver.DisposeProbe probe = scope.Images.CompleteTracked(source, new object());
        scope.Flush();

        section.Cells.Remove(cell);

        Assert.That(probe.IsDisposed, Is.True);
    }

    /// <summary>追い抜かれた解決の結果は採用されず、後片付けが行われる。</summary>
    [Test]
    public void StaleResolutionResultIsDisposed()
    {
        ImageSource first = ImageSource.FromFile("first.png");
        LabelCell cell = new() { IconSource = first };
        SettingsView view = new() { Root = { new Section { Cells = { cell } } } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        ImageSource second = ImageSource.FromFile("second.png");
        cell.IconSource = second;

        object secondIcon = new();
        FakeImageResolver.DisposeProbe currentProbe = scope.Images.CompleteTracked(second, secondIcon);
        FakeImageResolver.DisposeProbe staleProbe = scope.Images.CompleteTracked(first, new object());
        scope.Flush();

        Assert.That(staleProbe.IsDisposed, Is.True);
        Assert.That(currentProbe.IsDisposed, Is.False);
        Assert.That(scope.Gateway.IconOf(cell), Is.SameAs(secondIcon));
    }

    /// <summary>Native Host の解放をまたいだ解決の結果は後片付けが行われる。</summary>
    [Test]
    public void ResolutionFromReleasedHostIsDisposed()
    {
        ImageSource source = ImageSource.FromFile("icon.png");
        LabelCell cell = new() { IconSource = source };
        SettingsView view = new() { Root = { new Section { Cells = { cell } } } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        view.ReleaseHost();
        FakeImageResolver.DisposeProbe probe = scope.Images.CompleteTracked(source, new object());
        scope.Flush();

        Assert.That(probe.IsDisposed, Is.True);
        Assert.That(scope.Gateway.IconOf(cell), Is.Null);
    }

    /// <summary>画像の差し替えの後片付けは、差し替えを native へ配信した後に行われる。</summary>
    [Test]
    public void ReplacedIconLeaseIsDisposedAfterNativeUpdate()
    {
        ImageSource first = ImageSource.FromFile("first.png");
        LabelCell cell = new() { IconSource = first };
        SettingsView view = new() { Root = { new Section { Cells = { cell } } } };
        GatewayScope scope = GatewayScope.Connect(view);

        FakeImageResolver.DisposeProbe probe = scope.Images.CompleteTracked(first, new object());
        scope.Flush();
        scope.Reset();

        List<GatewayCall> callsAtDispose = [];
        probe.OnDispose = () => callsAtDispose = [.. scope.Calls];

        cell.IconSource = ImageSource.FromFile("second.png");
        scope.Images.CompleteTracked(cell.IconSource, new object());
        scope.Flush();

        Assert.That(probe.IsDisposed, Is.True);
        Assert.That(callsAtDispose.OfType<GatewayCall.ReplaceCell>(), Is.Not.Empty);
    }

    /// <summary>Cell 除去の後片付けは、除去を native へ配信した後に行われる。</summary>
    [Test]
    public void RemovedCellLeaseIsDisposedAfterNativeRemoval()
    {
        ImageSource source = ImageSource.FromFile("icon.png");
        LabelCell cell = new() { IconSource = source };
        Section section = new() { Cells = { cell } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);

        FakeImageResolver.DisposeProbe probe = scope.Images.CompleteTracked(source, new object());
        scope.Flush();
        scope.Reset();

        List<GatewayCall> callsAtDispose = [];
        probe.OnDispose = () => callsAtDispose = [.. scope.Calls];

        section.Cells.Remove(cell);

        Assert.That(probe.IsDisposed, Is.True);
        Assert.That(callsAtDispose.OfType<GatewayCall.RemoveCell>(), Is.Not.Empty);
    }

    /// <summary>Section の Cells 差し替えの後片付けは、差し替えを native へ配信した後に行われる。</summary>
    [Test]
    public void ReplacedSectionCellsLeaseIsDisposedAfterNativeUpdate()
    {
        ImageSource source = ImageSource.FromFile("icon.png");
        LabelCell cell = new() { IconSource = source };
        Section section = new() { Cells = { cell } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);

        FakeImageResolver.DisposeProbe probe = scope.Images.CompleteTracked(source, new object());
        scope.Flush();
        scope.Reset();

        List<GatewayCall> callsAtDispose = [];
        probe.OnDispose = () => callsAtDispose = [.. scope.Calls];

        section.Cells = new ObservableCollection<CellBase>();

        Assert.That(probe.IsDisposed, Is.True);
        Assert.That(callsAtDispose.OfType<GatewayCall.ReplaceSection>(), Is.Not.Empty);
    }

    /// <summary>設定ツリー作り直しの後片付けは、作り直しを native へ配信した後に行われる。</summary>
    [Test]
    public void RebuiltRootLeasesAreDisposedAfterNativeRebuild()
    {
        ImageSource source = ImageSource.FromFile("icon.png");
        LabelCell cell = new() { IconSource = source };
        SettingsView view = new() { Root = { new Section { Cells = { cell } } } };
        GatewayScope scope = GatewayScope.Connect(view);

        FakeImageResolver.DisposeProbe probe = scope.Images.CompleteTracked(source, new object());
        scope.Flush();
        scope.Reset();

        List<GatewayCall> callsAtDispose = [];
        probe.OnDispose = () => callsAtDispose = [.. scope.Calls];

        view.Root = new ObservableCollection<Section>();

        Assert.That(probe.IsDisposed, Is.True);
        Assert.That(callsAtDispose.OfType<GatewayCall.SetRoot>(), Is.Not.Empty);
    }

    /// <summary>除去済みの Cell に対する解決の結果は後片付けが行われる。</summary>
    [Test]
    public void ResolutionForRemovedCellIsDisposed()
    {
        ImageSource source = ImageSource.FromFile("icon.png");
        LabelCell cell = new() { IconSource = source };
        Section section = new() { Cells = { cell } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        section.Cells.Remove(cell);
        FakeImageResolver.DisposeProbe probe = scope.Images.CompleteTracked(source, new object());
        scope.Flush();

        Assert.That(probe.IsDisposed, Is.True);
    }
}
