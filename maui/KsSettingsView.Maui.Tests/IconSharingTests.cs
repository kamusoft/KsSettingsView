using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using KsSettingsView.Tests.Fakes;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsSettingsView.Tests;

/// <summary>
/// 複数のリースが同一の platform 画像インスタンスを包んだとき、片方の破棄がもう片方の表示を
/// 壊さないことを確認する。共有は解決口の世代交代と SettingsView の境界をまたいでも成立する。
/// </summary>
/// <remarks>
/// 共有が起きるのは platform のキャッシュが所有する画像で、解決口はそう分類した結果に
/// 後片付けの口を付けない (<see cref="FakeImageResolver.CompleteCacheOwned"/> がその形を再現する)。
/// facade が所有する画像は共有されず、従来どおり破棄で後片付けが走る
/// (<see cref="FakeImageResolver.CompleteTracked"/>)。
/// </remarks>
[TestFixture]
public class IconSharingTests
{
    /// <summary>同一 Cell の再解決が同じ画像を返しても icon は生き、配信も起きない。</summary>
    [Test]
    public void ReresolvingSameCellToSameImageKeepsIconAlive()
    {
        ImageSource source = ImageSource.FromFile("icon.png");
        LabelCell cell = new() { IconSource = source };
        SettingsView view = new() { Root = { new Section { Cells = { cell } } } };
        GatewayScope scope = GatewayScope.Connect(view);

        object icon = new();
        scope.Images.CompleteCacheOwned(source, icon);
        scope.Flush();
        scope.Reset();

        scope.Reconnect();
        scope.Images.CompleteCacheOwned(source, icon);
        scope.Flush();

        Assert.Multiple(() =>
        {
            Assert.That(scope.Gateway.IconOf(cell), Is.SameAs(icon));
            Assert.That(scope.All<GatewayCall.ReplaceCell>(), Is.Empty);
        });
    }

    /// <summary>同一画像への再解決では、旧リースが退役キューに滞留せずその場で解放される。</summary>
    /// <remarks>
    /// 表示内容が変わらないため配信も flush も起きず、退役キューに積むと解放の時機が失われる。
    /// 解放そのものを観測したいので、ここでは後片付けの口を持つリース (facade 所有の形) を使う。
    /// 画像の同一性で決まる分岐であり、所有の分類とは独立している。
    /// </remarks>
    [Test]
    public void ReresolvingSameCellToSameImageReleasesPreviousLeaseImmediately()
    {
        ImageSource source = ImageSource.FromFile("icon.png");
        LabelCell cell = new() { IconSource = source };
        SettingsView view = new() { Root = { new Section { Cells = { cell } } } };
        GatewayScope scope = GatewayScope.Connect(view);

        object icon = new();
        FakeImageResolver.DisposeProbe previous = scope.Images.CompleteTracked(source, icon);
        scope.Flush();

        scope.Reconnect();
        scope.Images.CompleteTracked(source, icon);

        Assert.Multiple(() =>
        {
            // 退役キューを流す flush を経ずに解放されている。
            Assert.That(previous.DisposeCount, Is.EqualTo(1));
            Assert.That(scope.Gateway.IconOf(cell), Is.SameAs(icon));
        });
    }

    /// <summary>解決口が作り直されても、同じ画像を返す限り icon は生きている。</summary>
    [Test]
    public void SharingAcrossResolverGenerationsKeepsIconAlive()
    {
        ImageSource source = ImageSource.FromFile("icon.png");
        LabelCell cell = new() { IconSource = source };
        SettingsView view = new() { Root = { new Section { Cells = { cell } } } };
        GatewayScope scope = GatewayScope.Connect(view);

        object icon = new();
        FakeImageResolver oldResolver = scope.Images;
        oldResolver.CompleteCacheOwned(source, icon);
        scope.Flush();

        view.ReleaseHost();
        scope.Reconnect(renewImages: true);
        Assert.That(scope.Images, Is.Not.SameAs(oldResolver));

        scope.Images.CompleteCacheOwned(source, icon);
        scope.Flush();

        Assert.That(scope.Gateway.IconOf(cell), Is.SameAs(icon));
    }

    /// <summary>同一画像を持つ 2 つの Cell の片方を外しても、残る Cell の icon は生きている。</summary>
    [Test]
    public void RemovingOneOfTwoCellsSharingImageKeepsTheOtherAlive()
    {
        ImageSource first = ImageSource.FromFile("first.png");
        ImageSource second = ImageSource.FromFile("second.png");
        LabelCell firstCell = new() { IconSource = first };
        LabelCell secondCell = new() { IconSource = second };
        Section section = new() { Cells = { firstCell, secondCell } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);

        object icon = new();
        scope.Images.CompleteCacheOwned(first, icon);
        scope.Images.CompleteCacheOwned(second, icon);
        scope.Flush();

        section.Cells.Remove(firstCell);

        Assert.That(scope.Gateway.IconOf(secondCell), Is.SameAs(icon));
    }

    /// <summary>別の SettingsView が同じ画像を表示していても、片方を畳んで壊れない。</summary>
    [Test]
    public void SharingAcrossSettingsViewsKeepsIconAlive()
    {
        ImageSource first = ImageSource.FromFile("first.png");
        ImageSource second = ImageSource.FromFile("second.png");
        LabelCell firstCell = new() { IconSource = first };
        LabelCell secondCell = new() { IconSource = second };
        Section firstSection = new() { Cells = { firstCell } };
        SettingsView firstView = new() { Root = { firstSection } };
        SettingsView secondView = new() { Root = { new Section { Cells = { secondCell } } } };

        GatewayScope firstScope = GatewayScope.Connect(firstView);
        GatewayScope secondScope = GatewayScope.Connect(secondView);

        object icon = new();
        firstScope.Images.CompleteCacheOwned(first, icon);
        secondScope.Images.CompleteCacheOwned(second, icon);
        firstScope.Flush();
        secondScope.Flush();

        firstSection.Cells.Remove(firstCell);

        Assert.That(secondScope.Gateway.IconOf(secondCell), Is.SameAs(icon));
    }

    /// <summary>共有していた最後の Cell が外れても、キャッシュ所有の画像には何も起こさない。</summary>
    [Test]
    public void RemovingTheLastSharingCellLeavesTheCacheOwnedImageUntouched()
    {
        ImageSource first = ImageSource.FromFile("first.png");
        ImageSource second = ImageSource.FromFile("second.png");
        LabelCell firstCell = new() { IconSource = first };
        LabelCell secondCell = new() { IconSource = second };
        Section section = new() { Cells = { firstCell, secondCell } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);

        object icon = new();
        scope.Images.CompleteCacheOwned(first, icon);
        scope.Images.CompleteCacheOwned(second, icon);
        scope.Flush();

        section.Cells.Remove(firstCell);
        section.Cells.Remove(secondCell);

        Assert.Multiple(() =>
        {
            Assert.That(scope.Gateway.IconOf(firstCell), Is.Null);
            Assert.That(scope.Gateway.IconOf(secondCell), Is.Null);
        });
    }

    /// <summary>控えられない解決結果がキャッシュ所有の画像を包んでも、表示中の icon は壊れない。</summary>
    /// <remarks>
    /// 追い抜かれた解決・旧世代の解決・登録解除済み Cell への解決の 3 経路とも、
    /// その場で破棄される。破棄が画像に触れないことがここでの要点になる。
    /// </remarks>
    [Test]
    public void UnretainedResolutionsSharingImageDoNotBreakTheDisplayedIcon()
    {
        ImageSource first = ImageSource.FromFile("first.png");
        LabelCell cell = new() { IconSource = first };
        Section section = new() { Cells = { cell } };
        LabelCell doomedCell = new() { IconSource = ImageSource.FromFile("doomed.png") };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);

        object icon = new();

        // 追い抜かれた解決: 先に始まっていた first の解決が、後から設定した second の後に届く。
        ImageSource second = ImageSource.FromFile("second.png");
        cell.IconSource = second;
        scope.Images.CompleteCacheOwned(second, icon);
        scope.Images.CompleteCacheOwned(first, icon);
        scope.Flush();
        Assert.That(scope.Gateway.IconOf(cell), Is.SameAs(icon));

        // 旧世代の解決: 前の世代の口が抱えたままの依頼が、口を作り直した後に結果を届ける。
        ImageSource third = ImageSource.FromFile("third.png");
        cell.IconSource = third;
        FakeImageResolver oldResolver = scope.Images;
        view.ReleaseHost();
        scope.Reconnect(renewImages: true);
        scope.Images.CompleteCacheOwned(third, icon);
        oldResolver.CompleteCacheOwned(third, icon);
        scope.Flush();
        Assert.That(scope.Gateway.IconOf(cell), Is.SameAs(icon));

        // 登録解除済み Cell への解決: 外れた Cell 宛の結果が後から届く。
        section.Cells.Add(doomedCell);
        section.Cells.Remove(doomedCell);
        scope.Images.CompleteCacheOwned(doomedCell.IconSource!, icon);
        scope.Flush();
        Assert.That(scope.Gateway.IconOf(cell), Is.SameAs(icon));
    }

    /// <summary>facade が所有する画像の後片付けは、Cell を外した時点で直ちに走る。</summary>
    [Test]
    public void OwnedImageIsCleanedUpOnItsOwnRemoval()
    {
        ImageSource source = ImageSource.FromFile("icon.png");
        LabelCell cell = new() { IconSource = source };
        Section section = new() { Cells = { cell } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);

        FakeImageResolver.DisposeProbe probe = scope.Images.CompleteTracked(source, new object());
        scope.Flush();

        section.Cells.Remove(cell);

        Assert.That(probe.DisposeCount, Is.EqualTo(1));
    }

    /// <summary>1 件の後片付けが失敗しても、退役キューの残りは破棄され取りこぼされない。</summary>
    [Test]
    public void FailingCleanupDoesNotStrandTheRemainingRetiredLeases()
    {
        ImageSource first = ImageSource.FromFile("first.png");
        ImageSource second = ImageSource.FromFile("second.png");
        ImageSource third = ImageSource.FromFile("third.png");
        LabelCell firstCell = new() { IconSource = first };
        LabelCell secondCell = new() { IconSource = second };
        LabelCell thirdCell = new() { IconSource = third };
        SettingsView view = new()
        {
            Root = { new Section { Cells = { firstCell, secondCell, thirdCell } } },
        };
        GatewayScope scope = GatewayScope.Connect(view);

        FakeImageResolver.DisposeProbe firstProbe = scope.Images.CompleteTracked(first, new object());
        FakeImageResolver.DisposeProbe secondProbe = scope.Images.CompleteTracked(second, new object());
        FakeImageResolver.DisposeProbe thirdProbe = scope.Images.CompleteTracked(third, new object());
        scope.Flush();

        // 破棄順に関わらず「途中で抜けたら残りが取りこぼされる」ことを見るため、2 件を失敗させる。
        firstProbe.OnDispose = () => throw new InvalidOperationException("first cleanup failed");
        secondProbe.OnDispose = () => throw new InvalidOperationException("second cleanup failed");

        AggregateException? thrown = Assert.Throws<AggregateException>(
            () => view.Root = new ObservableCollection<Section>());

        Assert.Multiple(() =>
        {
            Assert.That(thrown!.InnerExceptions, Has.Count.EqualTo(2));
            Assert.That(firstProbe.DisposeCount, Is.EqualTo(1));
            Assert.That(secondProbe.DisposeCount, Is.EqualTo(1));
            Assert.That(thirdProbe.DisposeCount, Is.EqualTo(1));
        });
    }

    /// <summary>facade が所有する画像の後片付けは、外した Cell の除去を native へ配信した後に走る。</summary>
    [Test]
    public void OwnedImageCleanupHappensAfterNativeDelivery()
    {
        ImageSource source = ImageSource.FromFile("icon.png");
        LabelCell cell = new() { IconSource = source };
        Section section = new() { Cells = { cell } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);

        FakeImageResolver.DisposeProbe probe = scope.Images.CompleteTracked(source, new object());
        scope.Flush();
        scope.Reset();

        List<GatewayCall> callsAtCleanup = [];
        probe.OnDispose = () => callsAtCleanup = [.. scope.Calls];

        section.Cells.Remove(cell);

        Assert.Multiple(() =>
        {
            Assert.That(probe.DisposeCount, Is.EqualTo(1));
            Assert.That(callsAtCleanup.OfType<GatewayCall.RemoveCell>(), Is.Not.Empty);
        });
    }
}
