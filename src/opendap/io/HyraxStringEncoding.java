package opendap.io;

import java.nio.charset.Charset;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by ndp on 10/20/15.
 */
public class HyraxStringEncoding {

    private static ReentrantReadWriteLock _rwLock;
    static {
        _rwLock = new ReentrantReadWriteLock(true);
    }

    private static Charset _currentCharSet;

    static {
        _currentCharSet = Charset.forName("UTF-8");
    }

    public static Charset getCharset(){
        Lock lock = _rwLock.readLock();
        try {
            lock.lock();
            return _currentCharSet;
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
