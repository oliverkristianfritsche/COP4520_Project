import java.util.*;
import java.io.*;

public class ParallelTreap<T extends Comparable<T>> {
	
	static final int THREAD_COUNT = 5;
	static Random rng = new Random();
	private ArrayDeque<Operation<T>> queue = new ArrayDeque<>();
	private Thread[] threads;
	
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
		
		Node<T> left = null, right = null;
		
		public Node(T v) {
			this.value = v;
			this.priority = rng.nextInt();
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
	
	public ParallelTreap() {
		threads = new Thread[THREAD_COUNT];
		
		for (int i = 0; i < threads.length; i++) {
			final int id = i;
			threads[i] = new Thread(new Runnable() {
				public void run() {
					while (true) {
						Operation<T> operation = poll();
						if (operation == null) {
							;
						}
						else if (operation.op == 0) {
							insert(operation.nodeParam, operation.valueParam);
						}
					}
				}
			});
		}
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
	
	
	public Node<T> insert(Node<T> root, T value) {
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
			else if (value.compareTo(root.value) > 0){
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
			
			/*
			 LockQueue<> waitingOnMe			 
			 waitingOnMe.lock();
			 
			 // do your magic
			 
			 waitingOnMe.unlock()	
			 */
			
			leftOrRightChild = stackOfLeftOrRightChild.pop();
			if (leftOrRightChild == 0) {
				parent.left = current;
				if (parent.left != null && parent.left.priority > parent.priority) {
					parent = leftRotation(parent);
				}
			}
			else {
				parent.right = current;
				if (parent.right != null && parent.right.priority > parent.priority) {
					parent = rightRotation(parent);
				}
			}
			current = parent;
		}
	
		return root = parent;
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
	
	public Node<T> createNode(T value) {
		return new Node(value);
	}
	
	public static void main(String[] args) {
		Node<Integer> root = null;
		
//		ParallelTreap<Integer> t = new ParallelTreap<>();
//		
//		for (int i = 0; i < 10; i++)
//			root = t.insert(root, i);
//		
//		t.inorder(root);
//		
//		for (int i = 30; i >= 20; i--)
//			root = t.insert(root, i);
//		
//		t.inorder(root);
//		
		ParallelTreap<Integer> testSpeed = new ParallelTreap<>();
		for (int i = 0; i <= 1e7; i++) {
			root = testSpeed.insert(root, i);
		}
		// testSpeed.inorder(root);
	}
}
