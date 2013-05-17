package fitting.multithread;
import java.util.LinkedList;
import java.util.Vector;

public class ThreadPool {
	private int nThreads = 0;
	private LinkedList<Runnable> tasks = new LinkedList<Runnable>();
	private Vector<ThreadTask> workers = new Vector<ThreadTask>();
	int countIdle = 0;

	public ThreadPool(int size) {
		this.nThreads = size;
		int idx = 0;
		for (int i = 0; i < size; i++) {
			ThreadTask thread = new ThreadTask(this, idx++);
			workers.add(thread);
			thread.start();
		}
	}

	public void addTask(Runnable task) {
		synchronized (tasks) {
			tasks.addLast(task);
			tasks.notify();
		}
	}

	public synchronized void increaseIdle() {
		countIdle++;
	}
	
	public synchronized void decreaseIdle() {
		countIdle--;
	}
	
	public boolean isEmpty() {
		return countIdle == nThreads;
	}

	public void terminateAll() {
		for (ThreadTask t : workers) {
			t.finish();
			t.interrupt();
		}
		synchronized (tasks) {
			tasks.notifyAll();
		}
	}

	public Runnable getNext() {
		Runnable returnVal = null;
		synchronized (tasks) {
			while (tasks.isEmpty()) {
				try {
					tasks.wait();
				} catch (InterruptedException ex) {
					//System.err.println("Interrupted");
					return null;
				}
			}
			if (tasks.isEmpty()) return null;
			returnVal = tasks.removeFirst();
		}
		return returnVal;
	}
}
