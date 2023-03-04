import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.io.*;

public class ParallelTreap<T extends Comparable<T>> {
	
	static final int THREAD_COUNT = 5;
	static Random rng = new Random();
	private ArrayDeque<Operation<T>> queue = new ArrayDeque<>();
	
	static class Operation<T> {
		int op;
		T valueParam;
		Node<T> nodeParam;
		public Operation(int a, T b, Node<T> c) {
			op = a;
			valueParam = b;
			nodeParam = c;
		}
	}
	
	public void add(Operation<T> p) {
		synchronized(queue) {
			queue.add(p);
		}
	}
	
	public Operation<T> poll() {
		synchronized(queue) {
			return queue.poll();
		}
	}
		
	static class Node<T> {
		T value;
		int priority;
		int size = 1;
		Node<T> parent = null;
		MCSLock threadQueue = new MCSLock();
		Node<T> left = null, right = null;
		
		public Node(T v) {
			this.value = v;
			this.priority = rng.nextInt();
		}
		
		public Node(T v, Node<T> parent) {
			this.value = v;
			this.priority = rng.nextInt();
			this.parent = parent;
		}
		
		public static int size(Node root) {
			return root == null ? 0 : root.size;
		}
		
		public static void compute_size(Node root) {
			if (root == null)
				return;
			root.size = size(root.left) + size(root.right) + 1;
		}
	}
	
	static class MCSLock {
		AtomicReference<QNode> tail;
		ThreadLocal<QNode> myNode;
		public MCSLock() {
			tail = new AtomicReference<QNode>(null);
			myNode = new ThreadLocal<QNode>() {
				protected QNode initialValue() {
					return new QNode();
				}
			};
		}

		static class QNode {
			AtomicBoolean locked = new AtomicBoolean();
			QNode next = null;
		}
	
		public void lock() {
			QNode qnode = myNode.get();
			QNode pred = tail.getAndSet(qnode);
			if (pred != null) {
				qnode.locked.set(true);
				pred.next = qnode;
				// wait until predecessor gives up the lock
				while (qnode.locked.get()) {}
			}
		}
		public void unlock() {
			QNode qnode = myNode.get();
			if (qnode.next == null) {
				if (tail.compareAndSet(qnode, null))
					return;
				// wait until predecessor fills in its next field
				while (qnode.next == null) {}
			}
			qnode.next.locked.set(false);
			qnode.next = null;
		}
	}
	
	public Node<T> treapRoot;
	AtomicInteger threadsInUse = new AtomicInteger(0);
	int cnt = 0;
	public synchronized Thread assignThread(T value) {
		while (cnt >= THREAD_COUNT) {
		
		}
		// threadsInUse.getAndIncrement();
		cnt++;
		return new Thread(new Runnable() {
			public void run() {
				try {
					treapRoot = insert(treapRoot, value);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
			}
		});
	}	
	
	public synchronized void treapInsert(T value) throws InterruptedException {
		Thread thread = assignThread(value);
		thread.start();
		
		thread.join();
		//Thread.sleep(20);
	    //System.out.println("PRINTING " + value);
		//inorder(this.treapRoot);
//		threadsInUse.getAndDecrement();
		cnt--;
	}
	
	public boolean contains(Node<T> root, T value) {
		if (root == null)
			return false;
		else if (root.value.compareTo(value) == 0) {
			return true;
		}
		else if (root.value.compareTo(value) < 0) {
			return contains(root.right, value);
		}
		else {
			return contains(root.left, value);
		}
	}
	
	public Node<T> leftRotation(Node<T> root) {
		if (root.right == null)
			return root;
		Node<T> right = root.right;
		Node<T> rightLeft = root.right.left;
		right.left = root;
		root.right = rightLeft;
		
		if (rightLeft != null)
			rightLeft.parent = root;
		right.parent = root.parent;
		root.parent = right;
		
		return right;
	}
 
 
	public Node<T> rightRotation(Node<T> root) {
		if (root.left == null)
			return root;
		Node<T> left = root.left;
		Node<T> leftRight = root.left.right;
		left.right = root;
		root.left = leftRight;
		
		if (leftRight != null)
			leftRight.parent = root;
		left.parent = root.parent;
		root.parent = left;
		
		return left;
	}
	
	
	public Node<T> insert(Node<T> root, T value) throws InterruptedException {
		Node<T> temp = root;
		
		if (temp == null) {
			return createNode(value);
		}
		
		ArrayDeque<Node<T>> stackOfPath = new ArrayDeque<>();
		ArrayDeque<Integer> stackOfLeftOrRightChild = new ArrayDeque<>();
		
		while (temp != null) {
			stackOfPath.push(temp);
			if (value.compareTo(temp.value) < 0) {
				temp = temp.left;
				stackOfLeftOrRightChild.push(0);
			}
			else if (value.compareTo(temp.value) > 0){
				temp = temp.right;
				stackOfLeftOrRightChild.push(1);
			}
			else {
				//System.out.println(value + " " + temp.value + " " + "returned?");
				return root;
			}
		}
				
		int leftOrRightChild = 0;
		Node<T> parent = null;		
		Node<T> current = createNode(value);
		//System.out.println(current.value + " STARTING PROCESS");
		while (stackOfPath.size() > 0) {
			parent = stackOfPath.pop();
			current.parent = parent;
						
			current.threadQueue.lock();
			Node<T> tempParent = current.parent;
			tempParent.threadQueue.lock();
			//System.out.println(tempParent.value);
			
			leftOrRightChild = stackOfLeftOrRightChild.pop();
			if (leftOrRightChild == 0) {
				parent.left = current;
				Node<T> tempChild = current.right;
				if (tempChild != null) tempChild.threadQueue.lock();
				if (parent.left != null && parent.left.priority > parent.priority) {
					parent = leftRotation(parent);
				}
				if (tempChild != null) tempChild.threadQueue.unlock();
			}
			else {
				parent.right = current;
				Node<T> tempChild = current.left;
				if (tempChild != null) tempChild.threadQueue.lock();
				if (parent.right != null && parent.right.priority > parent.priority) {
					parent = rightRotation(parent);
				}
				if (tempChild != null) tempChild.threadQueue.unlock();
			}
			current.threadQueue.unlock();
			tempParent.threadQueue.unlock();
			
			current = parent;
			
		}
		//System.out.println(current.value + " ENDING PROCESS");
		//Thread.sleep(30);
		return root = current;
	}
	
//	public Node<T> insert(Node<T> root, T value) {
//		if (root == null) {
//			return createNode(value);
//		}
//		if (value.compareTo(root.value) < 0) {
//			root.left = insert(root.left, value);
//			if (root.left != null && root.left.priority > root.priority) {
//				root = rightRotation(root);
//			}
//		}
//		else if (value.compareTo(root.value) > 0){
//			root.right = insert(root.right, value);
//			if (root.right != null && root.right.priority > root.priority) {
//				root = leftRotation(root);
//			}
//		}
//		else {
//			;
//		}
//		return root;
//	}	

	public void inorder(Node<T> root) {
		inorderHelper(root);
		System.out.println();
	}
	
	public void inorderHelper(Node<T> root) {
		if (root == null)
			return;
		inorderHelper(root.left);
		System.out.print(root.value + " ");
		inorderHelper(root.right);
	}
	
	public int height(Node<T> root) {
		if (root == null) return 0;
		return Math.max(height(root.left), height(root.right)) + 1;
	}
	
	public Node<T> createNode(T value) {
		return new Node(value);
	}
	
	public static void main(String[] args) throws InterruptedException {
//		Node<Integer> root = null;
		long start = System.currentTimeMillis();
		ParallelTreap<Integer> t = new ParallelTreap<>();
		
		t.treapRoot = t.insert(t.treapRoot, 18);
     	t.treapRoot = t.insert(t.treapRoot, 25);
		t.treapRoot = t.insert(t.treapRoot, 23);
		t.treapRoot = t.insert(t.treapRoot, 13);
		
		
//		t.treapInsert(0);
//		t.treapInsert(1);
//		for (int i = 0; i < 10; i++)
//			t.treapInsert(i);
//		
		for (int i = 0; i < 5000; i++) {
			//t.treapInsert(rng.nextInt());
			 t.treapInsert(i);
		}
		System.out.println(t.height(t.treapRoot));
		t.inorder(t.treapRoot);
		//while (t.threadsInUse.get() > 0);
		//t.inorder(t.treapRoot);

		long end = System.currentTimeMillis();
		System.out.println((end - start));
//		t.inorder(root);
//		
//		for (int i = 30; i >= 20; i--)
//			root = t.insert(root, i);
//		
//		t.inorder(root);
//		
//		ParallelTreap<Integer> testSpeed = new ParallelTreap<>();
//		for (int i = 0; i <= 1e7; i++) {
//			root = testSpeed.insert(root, i);
//		}
		// testSpeed.inorder(root);
	}
}
