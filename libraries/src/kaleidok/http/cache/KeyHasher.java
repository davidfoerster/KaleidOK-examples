package kaleidok.http.cache;

import java.security.DigestException;
import java.security.MessageDigest;

import static kaleidok.util.Math.divCeil;


class KeyHasher
{
  private static final int
    MAX_SYMBOL_COUNT = 64,
    BITS_PER_SYMBOL = 5,
    SYMBOL_COUNT = 1 << BITS_PER_SYMBOL,
    MAX_BYTE_COUNT = MAX_SYMBOL_COUNT * BITS_PER_SYMBOL / Byte.SIZE;


  private final MessageDigest digester;

  private final int digestLen;

  private final byte[] buf;

  private final char[] key;


  public KeyHasher( MessageDigest digester )
  {
    this.digester = digester;
    digestLen = Math.min(digester.getDigestLength(), MAX_BYTE_COUNT);
    buf = new byte[Math.max(digester.getDigestLength(), 1 << 10)];
    key = new char[divCeil(digestLen * Byte.SIZE, BITS_PER_SYMBOL)];
  }


  public String toInternalKey( CharSequence externalKey )
  {
    digestExternalKey(externalKey);
    return digestToKey();
  }


  private void digestExternalKey( CharSequence s )
  {
    final MessageDigest digester = this.digester;
    final byte[] buf = this.buf;
    final int len = s.length(),
      chunklen = buf.length / (Character.SIZE / Byte.SIZE);

    for (int chunkOffset = 0; chunkOffset < len; chunkOffset += chunklen) {
      final int chunkEnd = Math.min(chunkOffset + chunklen, len);
      for (int i = chunkOffset; i < chunkEnd; i++) {
        final char c = s.charAt(i);
        buf[i * 2] = (byte) c;
        buf[i * 2 + 1] = (byte) (c >>> Byte.SIZE);
      }
      digester.update(buf, 0, chunkEnd - chunkOffset);
    }

    try {
      digester.digest(buf, 0, buf.length);
    } catch (DigestException ex) {
      throw new AssertionError(ex);
    }
  }


  private String digestToKey()
  {
    final byte[] buf = this.buf;
    final char[] key = this.key;
    final int digestLen = this.digestLen;

    long bits = 0;
    int bitCount = 0, bufIdx = 0, keyIdx = 0;
    while (bufIdx < digestLen) {
      for (; bufIdx < digestLen && bitCount <= (Long.SIZE - Byte.SIZE);
        bitCount += Byte.SIZE, bufIdx++)
      {
        bits |= (long) (buf[bufIdx] & 0xff) << bitCount;
      }
      for (; bitCount >= BITS_PER_SYMBOL;
        bitCount -= BITS_PER_SYMBOL, bits >>>= BITS_PER_SYMBOL, keyIdx++)
      {
        key[keyIdx] =
          Character.forDigit((int) bits & (SYMBOL_COUNT - 1), SYMBOL_COUNT);
      }
    }
    if (bitCount > 0)
      key[keyIdx++] = Character.forDigit((int) bits, SYMBOL_COUNT);

    assert keyIdx == key.length;
    return new String(key);
  }
}
