package kaleidok.processing.image;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;


public final class ImageIO
{
  private ImageIO() { }


  public static final int
    BMP_FILEHEADER_SIZE = 14,
    BMP_INFOHEADER_SIZE = 40,
    BMP_OVERALLHEADER_SIZE = BMP_FILEHEADER_SIZE + BMP_INFOHEADER_SIZE;


  private static void checkBounds( int width, int height, int[] pixels,
    int offset )
  {
    if (width <= 0 || height <= 0)
      throw new ArrayIndexOutOfBoundsException(Math.min(width, height));
    if (offset < 0)
      throw new ArrayIndexOutOfBoundsException(offset);

    long len = (long) width * height;
    if (len > pixels.length - offset) // conveniently also checks for int overflow
    {
      throw new ArrayIndexOutOfBoundsException(
        "Array index out of range: " + (len + offset));
    }
  }


  public static void saveBmp32( ByteBuffer buf,
    int width, int height, int[] pixels, int offset )
  {
    checkBounds(width, height, pixels, offset);
    saveBmp32_unchecked(buf, width, height, pixels, offset);
  }


  private static void saveBmp32_unchecked( ByteBuffer buf,
    int width, int height, int[] pixels, int offset )
  {
    long
      len = (long) width * height,
      imageSize = len * Integer.BYTES,
      fileSize = imageSize + BMP_OVERALLHEADER_SIZE;
    if (fileSize > buf.remaining())
      throw new BufferOverflowException();

    ByteOrder oldByteOrder = buf.order();
    buf.order(ByteOrder.LITTLE_ENDIAN)

    // file header
      .put((byte) 'B').put((byte) 'M')
      .putInt((int) fileSize).putInt(0).putInt(BMP_OVERALLHEADER_SIZE)

    // info header
      .putInt(BMP_INFOHEADER_SIZE).putInt(width).putInt(-height)
      .putShort((short) 1).putShort((short) Integer.SIZE)
      .putInt(0).putInt((int) imageSize)
      .putInt(0).putInt(0).putInt(0).putInt(0)

    // image data
      .asIntBuffer().put(pixels, offset, (int) len);

    buf.order(oldByteOrder);
  }


  public static MappedByteBuffer saveBmp32( FileChannel fc, boolean close,
    int width, int height, int[] pixels, int offset ) throws IOException
  {
    if (fc == null)
      throw new NullPointerException(FileChannel.class.getSimpleName());
    checkBounds(width, height, pixels, offset);

    long fileSize =
      (long) width * height * Integer.BYTES + BMP_OVERALLHEADER_SIZE;
    MappedByteBuffer map;
    try {
      map = fc.map(FileChannel.MapMode.READ_WRITE, fc.position(), fileSize);
    } finally {
      if (close)
        fc.close();
    }
    saveBmp32_unchecked(map, width, height, pixels, offset);
    return map;
  }


  public static MappedByteBuffer saveBmp32( Path path,
    int width, int height, int[] pixels, int offset )
    throws IOException, UnsupportedOperationException
  {
    try (FileChannel fc = FileChannel.open(path, MMAP_SAVE_OPTIONS)) {
      return saveBmp32(fc, true, width, height, pixels, offset);
    }
  }


  private static final OpenOption[] MMAP_SAVE_OPTIONS = {
      StandardOpenOption.READ,
      StandardOpenOption.WRITE,
      StandardOpenOption.CREATE,
      StandardOpenOption.TRUNCATE_EXISTING
    };
}
