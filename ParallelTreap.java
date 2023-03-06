import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class ParallelTreap<T extends Comparable<T>> {
	
	static final int THREAD_COUNT = 2;
	static final int SLEEP_TIME = 5;
	static Random rng = new Random();
			
	static class Node<T> {
		ReentrantLock lock = new ReentrantLock();
		T value;
		int priority;
		int size = 1;
		Node<T> parent = null;
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
		
	public Node<T> treapRoot;
	
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
		
		if (rightLeft != null) {
			synchronized(rightLeft) {
				right.left = root;
				root.right = rightLeft;
				
				rightLeft.parent = root;
				right.parent = root.parent;
				root.parent = right;
			}
		}
		else {
			right.left = root;
			root.right = null;
			right.parent = root.parent;
			root.parent = right;
		}
				
		return right;
	}
 
 
	public Node<T> rightRotation(Node<T> root) {
		Node<T> left = root.left;
		Node<T> leftRight = root.left.right;
				
		if (leftRight != null) {
			synchronized(leftRight) {
				left.right = root;
				root.left = leftRight;
				
				leftRight.parent = root;
				left.parent = root.parent;
				root.parent = left;
			}
		}
		else {
			left.right = root;
			root.left = null;
			left.parent = root.parent;
			root.parent = left;
		}
		
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
				return root;
			}
		}
					
		int leftOrRightChild = 0;
		Node<T> parent = null;		
		Node<T> current = createNode(value);
		
		while (stackOfPath.size() > 0) {
			parent = stackOfPath.pop();
			current.parent = parent;
						
			synchronized(current) {
				Node<T> tempParent = current.parent;
				synchronized(tempParent) {
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
				}
			}			
			current = parent;
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
					for (Integer i = id; i < 10000; i += threads.length)
						try {
							//System.out.println("THREAD WITH ID: " + id + " inserting " + i);
							t.treapRoot = t.insert(t.treapRoot, i);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
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
//		t.inorder(t.treapRoot);
	}
}
