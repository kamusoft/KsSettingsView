using System;
using System.Collections.Generic;
using KsSettingsView.Internals;
using NUnit.Framework;

namespace KsSettingsView.Tests;

/// <summary>
/// ファイル指定から解決した画像を facade が後片付けしてよいか、の分類を確認する。
/// </summary>
/// <remarks>
/// platform の名前付き画像キャッシュへの問い合わせは差し替えて、引く名前と分岐だけを見る。
/// 引く名前は画像解決サービスがキャッシュを引くときと同じ形 (ディレクトリと拡張子を落とした名前)
/// でなければならず、ここがずれると照合が必ず空振りして表示破壊側の誤分類になる。
/// </remarks>
[TestFixture]
public class FileImageOwnershipTests
{
    /// <summary>キャッシュは、ディレクトリと拡張子を落とした名前で引く。</summary>
    /// <param name="fileName">解決に使ったファイル指定</param>
    [TestCase("logo", "logo")]
    [TestCase("logo.png", "logo")]
    [TestCase("images/logo.png", "logo")]
    [TestCase("images/logo", "logo")]
    [TestCase("Resources/Images/logo.svg", "logo")]
    public void TheCacheIsQueriedWithTheStrippedName(string fileName, string expected)
    {
        List<string> lookups = [];

        KsFileImageOwnership.IsOwnedByPlatformCache(
            fileName,
            new object(),
            name =>
            {
                lookups.Add(name);
                return null;
            });

        Assert.That(lookups, Is.EqualTo(new[] { expected }));
    }

    /// <summary>キャッシュが同じインスタンスを返す画像はキャッシュ所有。</summary>
    [Test]
    public void ImageReturnedByTheCacheIsOwnedByTheCache()
    {
        object image = new();

        bool owned = KsFileImageOwnership.IsOwnedByPlatformCache("asset_name", image, _ => image);

        Assert.That(owned, Is.True);
    }

    /// <summary>拡張子付きで指定しても、キャッシュ所有の画像はキャッシュ所有と判定される。</summary>
    /// <remarks>
    /// asset catalog の資産名に拡張子は付かない。生の指定のままキャッシュを引くと一致せず、
    /// 共有されている画像に後片付けの口を付けてしまう。
    /// </remarks>
    [Test]
    public void CacheOwnedImageIsDetectedEvenWhenTheFileNameCarriesAnExtension()
    {
        object image = new();

        bool owned = KsFileImageOwnership.IsOwnedByPlatformCache(
            "logo.png",
            image,
            name => name == "logo" ? image : null);

        Assert.That(owned, Is.True);
    }

    /// <summary>ディレクトリ付きで指定しても、キャッシュ所有の画像はキャッシュ所有と判定される。</summary>
    [Test]
    public void CacheOwnedImageIsDetectedEvenWhenTheFileNameCarriesADirectory()
    {
        object image = new();

        bool owned = KsFileImageOwnership.IsOwnedByPlatformCache(
            "images/logo.png",
            image,
            name => name == "logo" ? image : null);

        Assert.That(owned, Is.True);
    }

    /// <summary>キャッシュが別のインスタンスを返すなら facade 所有。</summary>
    /// <remarks>ファイルから起こした画像は、同じ名前の資産があっても別実体になる。</remarks>
    [Test]
    public void ImageNotMatchingTheCacheIsNotOwnedByTheCache()
    {
        bool owned = KsFileImageOwnership.IsOwnedByPlatformCache(
            "logo.png",
            new object(),
            _ => new object());

        Assert.That(owned, Is.False);
    }

    /// <summary>キャッシュがその名前を知らない場合は facade 所有。</summary>
    [Test]
    public void ImageWithNoCacheEntryIsNotOwnedByTheCache()
    {
        bool owned = KsFileImageOwnership.IsOwnedByPlatformCache(
            "logo.png",
            new object(),
            _ => null);

        Assert.That(owned, Is.False);
    }

    /// <summary>実ファイルがある名前でも、照合を飛ばさずキャッシュへ引き直す。</summary>
    /// <remarks>
    /// 解決がどちらの経路を通ったかは外から見えない。経路を推し量って照合を飛ばすと、
    /// 推し量りが外れたときに表示破壊側へ倒れる。判定は常に実体の同一性で行う。
    /// </remarks>
    [Test]
    public void ClassificationNeverSkipsTheCacheLookup()
    {
        List<string> lookups = [];

        KsFileImageOwnership.IsOwnedByPlatformCache(
            "packaged.png",
            new object(),
            name =>
            {
                lookups.Add(name);
                return null;
            });

        Assert.That(lookups, Is.Not.Empty);
    }

    /// <summary>由来を確かめられない指定は、後片付けしない側へ倒す。</summary>
    /// <param name="fileName">引き直せる名前を取り出せないファイル指定</param>
    /// <remarks>
    /// 取りこぼした結果は回収されるだけだが、誤って後片付けすると表示中の画像が壊れる。
    /// </remarks>
    [TestCase(null)]
    [TestCase("")]
    [TestCase(".png")]
    [TestCase("images/")]
    public void UndecidableFileNameFallsBackToNotDisposing(string? fileName)
    {
        bool owned = KsFileImageOwnership.IsOwnedByPlatformCache(
            fileName,
            new object(),
            _ => throw new InvalidOperationException("キャッシュを引いてはいけない"));

        Assert.That(owned, Is.True);
    }
}
