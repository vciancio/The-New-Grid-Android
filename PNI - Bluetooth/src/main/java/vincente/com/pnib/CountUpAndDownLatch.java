package vincente.com.pnib;

import java.util.concurrent.CountDownLatch;

/**
 * Created by vincente on 4/19/16
 */
public class CountUpAndDownLatch {
    private CountDownLatch latch;
    private final Object lock = new Object();

    public CountUpAndDownLatch(int count) {
        this.latch = new CountDownLatch(count);
    }

    public void countDownOrWaitIfZero() throws InterruptedException {
        synchronized(lock) {
            while(latch.getCount() == 0) {
                lock.wait();
            }
            latch.countDown();
            lock.notifyAll();
        }
    }

    public void countDown() {
        synchronized (lock){
            latch.countDown();
            lock.notifyAll();
        }
    }

    public void waitUntil(int i) throws InterruptedException {
        synchronized(lock){
            while(latch.getCount() != i){
                lock.wait();
            }
        }
    }

    public void waitUntil(int i, long timeout) throws InterruptedException {
        synchronized (lock){
            if(latch.getCount() > i){
                lock.wait(timeout);
            }
        }
    }

    public void waitUntilZero() throws InterruptedException {
        waitUntil(0);
    }

    public void countUp() { //should probably check for Integer.MAX_VALUE
        synchronized(lock) {
            latch = new CountDownLatch((int) latch.getCount() + 1);
            lock.notifyAll();
        }
    }

    public int getCount() {
        synchronized(lock) {
            return (int) latch.getCount();
        }
    }
}
