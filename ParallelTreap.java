import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.io.FileWriter;
import java.io.IOException;

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

	public Node<T> sequential_insert(Node<T> root,T value) {
		Node<T> newNode = new Node<>(value);
		Node<T> parent = null;
		Node<T> current = treapRoot;

		while (current != null) {
			parent = current;
			if (value.compareTo(current.value) < 0) {
				current = current.left;
			}
			else if (value.compareTo(current.value) > 0){
				current = current.right;
			}
			else {
				return root;
			}
		}

		newNode.parent = parent;
		if (parent == null) {
			root = newNode;
		}
		else if (value.compareTo(parent.value) < 0) {
			parent.left = newNode;
		}
		else {
			parent.right = newNode;
		}

		while (newNode != root && newNode.parent.priority > newNode.priority) {
			if (newNode == newNode.parent.left) {
				newNode = rightRotation(newNode.parent);
			}
			else {
				newNode = leftRotation(newNode.parent);
			}
		}

		
		
		return treapRoot;
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

	public String evalute(int nThreads,int nElements,String method,int nRuns, T[] vals) throws InterruptedException {
		String text = "";
		long startTime = 0;
		long endTime = 0;
		long totalTime = 0;
		long averageTime = 0;
		int height = 0;
		int maxHeight = 0;
		int minHeight = 0;
		int averageHeight = 0;
		int totalHeight = 0;
		int success = 0;
		Thread[] threads = new Thread[nThreads];

		for(int i =0; i < nRuns; i++){
			this.treapRoot = new Node(-1, Integer.MAX_VALUE);
			
			if(method.equals("insert")){
				try{
					startTime = System.nanoTime();
					
					for (int index = 0; index < threads.length; index++) {
							final int id = index;
							threads[index] = new Thread(new Runnable() {
								public void run() {
									for (Integer j = id; j < nElements; j += threads.length)
										try {
											treapRoot = insert(treapRoot, vals[j]);
										} catch (Exception g) {
											g.printStackTrace();
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
					endTime = System.nanoTime();
					totalTime += endTime - startTime;
					height = this.height(this.treapRoot);
					totalHeight += height;
					if(height > maxHeight){
						maxHeight = height;
					}
					if(height < minHeight){
						minHeight = height;
					}
					success++;
					text += totalTime + " " + height + "\n";
				}
				catch(Exception e){
					text += "-1 -1 -1 -1\n";
				}
			
			}
			else if(method.equals("delete")){
				//todo
			}
			else if(method.equals("contains")){
				//todo
			}
			else{
				System.out.println("Invalid method or implementation");
			}
		}
		averageTime = totalTime / success;
		averageHeight = totalHeight / success;
		return text;
	}

	public static void main(String[] args) throws InterruptedException {
		boolean runEvaluation = true;
		
		if (runEvaluation) {
			ParallelTreap<Integer> t;
			int[] nThreads = {1,2,4,6,8};
			int[] nElements = {100,1000,10000,100000, 1000000};
			String[] methods = {"insert"};//, "delete", "contains"};
			String[] implementations = {"parallel"};//, "sequential"};
			int nRuns = 10;
			FileWriter writer= null;
			String text;
			String filename;
			Integer[] vals;

			for (int i = 0; i < nThreads.length; i++) {
				for (int j = 0; j < nElements.length; j++) {
					for (int k = 0; k < methods.length; k++) {
				
						t= new ParallelTreap<Integer>();
						vals = new Integer[nElements[j]];
						for (int m = 0; m < nElements[j]; m++) {
							vals[m] = m;
						} 
						filename = "./evaluation/data/"+nThreads[i] + "_" + nElements[j] + "_" + methods[k] + "_" + "parallel.txt";
						try {
							writer = new FileWriter(filename);
							text = t.evalute(nThreads[i], nElements[j], methods[k],nRuns, vals);
							writer.write(text);
						} catch (IOException e) {
							System.err.println("Error writing to "+filename+": " + e.getMessage());
						} finally {
							try {
								if (writer != null) {
									writer.close();
								}
							} catch (IOException e) {
								System.err.println("Error closing "+filename+": " + e.getMessage());
							}
						}
							
						
					}
				}
			}
		}
		else {
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
}
