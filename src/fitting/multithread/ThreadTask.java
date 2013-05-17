package fitting.multithread;
public class ThreadTask extends Thread {
	private ThreadPool pool;
	private boolean finished = false;
	private int myIdx = 0;

	public ThreadTask(ThreadPool thePool, int idx) {
		pool = thePool;
		myIdx = idx;
	}

	public void run() {
		while (!finished) {
			// blocks until job
			pool.increaseIdle();
			Runnable job = pool.getNext();
			if (finished || job == null) return;
			try {
				pool.decreaseIdle();
				job.run(); //run, non start: lo voglio eseguire in questo thread
			} catch (Exception e) {
				// Ignore exceptions thrown from jobs
				System.err.println("Job exception: " + e);
				e.printStackTrace();
			}
		}
	}

	public void finish() {
		this.finished = true;
	}

	public int getIdx() {
		return myIdx;
	}
}
