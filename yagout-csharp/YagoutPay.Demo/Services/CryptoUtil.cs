using System.Security.Cryptography;
using System.Text;

namespace YagoutPay.Demo.Services;

public static class CryptoUtil
{
    private static readonly byte[] IV = Encoding.ASCII.GetBytes("0123456789abcdef");

    public static string EncryptBase64(string plain, string keyBase64)
    {
        var key = Convert.FromBase64String(RemoveWhitespace(keyBase64));
        using var aes = Aes.Create();
        aes.Mode = CipherMode.CBC;
        aes.Padding = PaddingMode.PKCS7;
        aes.Key = key;
        aes.IV = IV;
        using var enc = aes.CreateEncryptor();
        var bytes = Encoding.UTF8.GetBytes(plain);
        var cipher = enc.TransformFinalBlock(bytes, 0, bytes.Length);
        return Convert.ToBase64String(cipher);
    }

    public static string DecryptBase64(string cipherBase64, string keyBase64)
    {
        var key = Convert.FromBase64String(RemoveWhitespace(keyBase64));
        var bytes = Convert.FromBase64String(cipherBase64);
        using var aes = Aes.Create();
        aes.Mode = CipherMode.CBC;
        aes.Padding = PaddingMode.PKCS7;
        aes.Key = key;
        aes.IV = IV;
        using var dec = aes.CreateDecryptor();
        var plain = dec.TransformFinalBlock(bytes, 0, bytes.Length);
        return Encoding.UTF8.GetString(plain);
    }

    public static string Sha256Hex(string input)
    {
        using var sha = SHA256.Create();
        var hash = sha.ComputeHash(Encoding.UTF8.GetBytes(input));
        var sb = new StringBuilder(hash.Length * 2);
        foreach (var b in hash) sb.Append(b.ToString("x2"));
        return sb.ToString();
    }

    private static string RemoveWhitespace(string s) => new string(s.Where(c => !char.IsWhiteSpace(c)).ToArray());
}
