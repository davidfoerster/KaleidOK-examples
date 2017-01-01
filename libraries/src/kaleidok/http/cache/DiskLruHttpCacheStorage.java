package kaleidok.http.cache;

import com.jakewharton.disklrucache.DiskLruCache;
import kaleidok.io.platform.PlatformPaths;
import org.apache.commons.collections4.map.LRUMap;
import org.apache.http.client.cache.HttpCacheEntry;
import org.apache.http.client.cache.HttpCacheStorage;
import org.apache.http.client.cache.HttpCacheUpdateCallback;
import org.apache.http.client.cache.HttpCacheUpdateException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Map;


@SuppressWarnings("unused")
public class DiskLruHttpCacheStorage implements HttpCacheStorage, Closeable
{
  /**
   * The current collision resolution method simply replaces existing colliding
   * cache entries, which is probably fine in a key space of 2^(5*64).
   *
   * A smarter method might be to use some kind of open addressing
   * like linear probing. Separate chaining is possible through DiskLruCache's
   * additional entry values, but a bit difficult to implement. Both methods
   * have the drawback of defeating the LRU mechanism, as there is currently no
   * unaccounted access to cache entries.
   */

  private final DiskLruCache diskCache;

  /**
   * Caches mapping from external to internal keys.
   * <p>
   * TODO: Use <a href="https://github.com/ben-manes/caffeine">Caffeine</a>
   * instead of {@link LRUMap}, when switching to JDK 1.8.
   */
  private final Map<String, String> keyMap =
    Collections.synchronizedMap(new LRUMap<String, String>(500));

  private static final ThreadLocal<KeyHasher> keyHasher =
    ThreadLocal.withInitial(() -> {
        try {
          return new KeyHasher(MessageDigest.getInstance("SHA-384"));
        } catch (NoSuchAlgorithmException ex) {
          throw new InternalError(ex);
        }
      });


  public DiskLruHttpCacheStorage( File directory, int appVersion,
    long maxSize )
    throws IOException
  {
    diskCache = DiskLruCache.open(directory, appVersion, 1, maxSize);
  }

  public DiskLruHttpCacheStorage( String appName, int appVersion,
    long maxSize )
    throws IOException
  {
    this(
      PlatformPaths.getCacheDir(appName).toFile(),
      appVersion, maxSize);
  }


  @Override
  public void putEntry( String key, HttpCacheEntry entry ) throws IOException
  {
    if (entry == null)
      throw new NullPointerException("entry");

    DiskLruCache.Editor editor = diskCache.edit(toInternalKey(key));
    if (editor != null)
    {
      try
      {
        serialize(key, entry, editor.newOutputStream(0));
        editor.commit();
      }
      finally
      {
        editor.abortUnlessCommitted();
      }
    }
  }


  @Override
  public HttpCacheEntry getEntry( String key ) throws IOException
  {
    try (DiskLruCache.Snapshot snapshot = diskCache.get(toInternalKey(key))) {
      return (snapshot != null)  ?
        deserialize(key, snapshot.getInputStream(0)) :
        null;
    }
  }


  @Override
  public void removeEntry( String key ) throws IOException
  {
    diskCache.remove(toInternalKey(key, true));
  }


  @Override
  public void updateEntry( String key, HttpCacheUpdateCallback callback )
    throws IOException, HttpCacheUpdateException
  {
    if (callback == null)
      throw new NullPointerException("callback");

    String internalKey = toInternalKey(key);
    DiskLruCache.Editor editor = diskCache.edit(internalKey);
    if (editor == null)
      throw new HttpCacheUpdateException("Entry is being edited already");

    boolean closed = false;
    try {
      InputStream is = editor.newInputStream(0);
      HttpCacheEntry entry;
      if (is != null) {
        entry = deserialize(key, is);
        is.close();
      } else {
        entry = null;
      }

      entry = callback.update(entry);
      if (entry != null) {
        serialize(key, entry, editor.newOutputStream(0));
        editor.commit();
        closed = true;
      } else {
        editor.abort();
        closed = true;
        diskCache.remove(internalKey);
      }
    } finally {
      if (!closed)
        editor.abort();
    }
  }


  protected String toInternalKey( String externalKey )
  {
    return toInternalKey(externalKey, false);
  }

  protected String toInternalKey( String externalKey, boolean remove )
  {
    String internalKey = remove ?
      keyMap.remove(externalKey) :
      keyMap.get(externalKey);
    if (internalKey == null) {
      internalKey = keyHasher.get().toInternalKey(externalKey);
      if (!remove)
        keyMap.put(externalKey, internalKey);
    }
    return internalKey;
  }


  protected static void serialize( String externalKey, HttpCacheEntry entry,
    OutputStream os )
    throws IOException
  {
    if (!(os instanceof BufferedOutputStream))
      os = new BufferedOutputStream(os);
    try (ObjectOutputStream oos = new ObjectOutputStream(os)) {
      oos.writeObject(externalKey);
      oos.writeObject(entry);
    }
  }


  protected static HttpCacheEntry deserialize( String externalKey,
    InputStream is )
    throws IOException
  {
    if (!(is instanceof BufferedInputStream))
      is = new BufferedInputStream(is);
    try (ObjectInputStream ois = new ObjectInputStream(is)) {
      String storedKey = (String) ois.readObject();
      return (externalKey.equals(storedKey)) ?
        (HttpCacheEntry) ois.readObject() :
        null;
    } catch (ClassNotFoundException ex) {
      throw new AssertionError(ex);
    }
  }


  public long getMaxSize()
  {
    return diskCache.getMaxSize();
  }

  public void setMaxSize( long maxSize )
  {
    diskCache.setMaxSize(maxSize);
  }


  public long size()
  {
    return diskCache.size();
  }


  public boolean isClosed()
  {
    return diskCache.isClosed();
  }


  public void flush() throws IOException
  {
    diskCache.flush();
  }


  @Override
  public void close() throws IOException
  {
    diskCache.close();
    keyMap.clear();
  }


  public void delete() throws IOException
  {
    diskCache.delete();
    keyMap.clear();
  }
}
