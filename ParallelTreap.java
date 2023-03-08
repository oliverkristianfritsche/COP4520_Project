import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class ParallelTreap<T extends Comparable<T>> {
	
	static final int THREAD_COUNT = 8;
	static final int SIZE = 10000;
	static Random rng = new Random();
			
	static class Node<T> {
		ReentrantLock lock = new ReentrantLock();
		T value;
		int priority;
		Node<T> parent = null;
		Node<T> left = null, right = null;
		
		public Node(T v) {
			this.value = v;
			this.priority = rng.nextInt() - 1;
		}
		
		public Node(T v, Node<T> parent) {
			this.value = v;
			this.priority = rng.nextInt();
			this.parent = parent;
		}
		
		public Node(T v, int priority) {
			this.value = v;
			this.priority = priority;
		}
	}
		
	public Node<T> treapRoot = new Node(-1, Integer.MAX_VALUE);
	
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
		Node<T> right = root.right;
		Node<T> rightLeft = root.right.left;
		right.left = root;
		root.right = rightLeft;
		return right;
	}
     
    public Node<T> rightRotation(Node<T> root) {
		Node<T> left = root.left;
		Node<T> leftRight = root.left.right;
		left.right = root;
		root.left = leftRight;
		return left;
	}
	
	public Node<T> insert(Node<T> root, T value) throws InterruptedException {
		Node<T> temp = root;

		if (temp == null) {
			return createNode(value);
		}
		
		ArrayDeque<Node<T>> stackOfPath = new ArrayDeque<>();
		ArrayDeque<Integer> stackOfLeftOrRightChild = new ArrayDeque<>();
		
		Node<T> current = createNode(value);
		Node<T> firstSubtreeToLock = null;
		
		current.lock.lock();
		
		while (temp != null) {
			stackOfPath.push(temp);
			if (firstSubtreeToLock == null)
				firstSubtreeToLock = temp;
			else if (current.priority < temp.priority) {
				firstSubtreeToLock = temp;
			}
			
			temp.lock.lock();
			
			if (value.compareTo(temp.value) < 0) {
				temp = temp.left;
				stackOfLeftOrRightChild.push(0);
			}
			else if (value.compareTo(temp.value) > 0){
				temp = temp.right;
				stackOfLeftOrRightChild.push(1);
			}
			else {
				return root;
			}
		}
		
		temp = root;
		while (temp != firstSubtreeToLock) {
			temp.lock.unlock();
			if (value.compareTo(temp.value) < 0) {
				temp = temp.left;
			}
			else if (value.compareTo(temp.value) > 0){
				temp = temp.right;
			}
			else {
				break;
			}
		}
		
		ArrayDeque<Node<T>> remainPathUnlock = new ArrayDeque<>();
		remainPathUnlock.add(current);
		temp = firstSubtreeToLock;
		while (temp != null) {
			remainPathUnlock.push(temp);
			if (value.compareTo(temp.value) < 0) {
				temp = temp.left;
			}
			else if (value.compareTo(temp.value) > 0){
				temp = temp.right;
			}
			else {
				break;
			}
		}
					
		int leftOrRightChild = 0;
		Node<T> parent = null;		
		
		
		boolean flag = false;
		while (stackOfPath.size() > 0) {
			parent = stackOfPath.pop();
			current.parent = parent;
			if (current == firstSubtreeToLock) {
				flag = true;
			}
			if (flag) {
				current = parent;
				continue;
			}
			leftOrRightChild = stackOfLeftOrRightChild.pop();
			if (leftOrRightChild == 0) {
				parent.left = current;
				if (parent.left != null && parent.left.priority > parent.priority) {
					parent = rightRotation(parent);
				}	
			}
			else {
				parent.right = current;
				if (parent.right != null && parent.right.priority > parent.priority) {
					parent = leftRotation(parent);
				}
			}
			current = parent;
		}
		
		while (remainPathUnlock.size() > 0) {
			Node<T> val = remainPathUnlock.pop();
			val.lock.unlock();
		}
		
		return root = current;
	}
	
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
		long start = System.currentTimeMillis();
		ParallelTreap<Integer> t = new ParallelTreap<>();
				
		Thread[] threads = new Thread[THREAD_COUNT];
				
		for (int index = 0; index < threads.length; index++) {
			final int id = index;
			threads[index] = new Thread(new Runnable() {
				public void run() {
					for (Integer i = id; i < SIZE; i += threads.length)
						try {
							t.treapRoot = t.insert(t.treapRoot, i);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
				}
			});;
		}
			
		for (int index = 0; index < threads.length; index++) {
			threads[index].start();
		}
		
		for (int index = 0; index < threads.length; index++) {
			threads[index].join();
		}
				
		System.out.println("Height: " + t.height(t.treapRoot));
		
		long end = System.currentTimeMillis();
		System.out.println((end - start));
		// t.inorder(t.treapRoot);
	}
}
