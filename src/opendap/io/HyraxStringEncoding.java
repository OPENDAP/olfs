package opendap.io;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This is a thread safe wrapper of the Hyrax wide String encoding.
 * The default Charset used is java.nio.charset.StandardCharsets.UTF_8.
 * This can be changed at run time by utilizing the setCharset() method.
 *
 */
public class HyraxStringEncoding {

    private static ReentrantReadWriteLock _rwLock;
    static {
        _rwLock = new ReentrantReadWriteLock(true);
    }

    private static Charset _currentCharSet;

    static {
        _currentCharSet = StandardCharsets.UTF_8;
    }

    public static Charset getCharset(){
        Lock lock = _rwLock.readLock();
        try {
            lock.lock();
            return Charset.forName(_currentCharSet.name());
        }
        finally {
            lock.unlock();
        }
    }
    public static Charset setCharset(Charset newCharset){
        Lock lock = _rwLock.writeLock();
        try {
            lock.lock();
            Charset oldCS = _currentCharSet;
            _currentCharSet = newCharset;
            return oldCS;
        }
        finally {
            lock.unlock();
        }
    }
}
