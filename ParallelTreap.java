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

		public String toString() {
			return this.value + " " + (this.left == null ? "null" : this.left.value) + " " + (this.right == null ? "null" : this.right.value) + " " + (this.parent == null ? "null" : this.parent.value);
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
	
	public Node<T> DleftRotation(Node<T> root) {
		Node<T> right = root.right;
		Node<T> rightLeft = root.right.left;
		right.left = root;
		root.right = rightLeft;
		
		if(root.value.compareTo(root.parent.value) < 0){
			root.parent.left = right;
		}else if(root.value.compareTo(root.parent.value) > 0){
			root.parent.right = right;
		}
		right.parent = root.parent;
		root.parent = right;
		if(rightLeft != null)
			rightLeft.parent = root;
					
		return right;
	}
     
	public Node<T> DrightRotation(Node<T> root) {
		Node<T> left = root.left;
		Node<T> leftRight = root.left.right;
		left.right = root;
		root.left = leftRight;
		if(root.value.compareTo(root.parent.value) < 0){
			root.parent.left = left;
		}else if(root.value.compareTo(root.parent.value) > 0){
			root.parent.right = left;
		}
		left.parent = root.parent;
		root.parent = left;
		if(leftRight != null)
			leftRight.parent = root;
	
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
					parent = DrightRotation(parent);
				}	
			}
			else {
				parent.right = current;
				if (parent.right != null && parent.right.priority > parent.priority) {
					parent = DleftRotation(parent);
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

	public int nChildren(Node<T> root) {
		int count = 0;
		if (root.left != null)
			count++;
		if (root.right != null)
			count++;
		return count;
	}

	public synchronized Node<T> delete(Node<T> root, T toDelete) throws InterruptedException {
		Node<T> current = root;
		Node<T> oldParent = null;
		while (current != null && !current.value.equals(toDelete)) {
			if ((current.left != null && current.left.value.equals(toDelete)) || 
				(current.right != null && current.right.value.equals(toDelete))){
				if (toDelete.compareTo(current.value) < 0) {
					current = current.left;
				} else {
					current = current.right;
				}
				break;
			} else {
				if (toDelete.compareTo(current.value) < 0) {
					current = current.left;
				} else {
					current = current.right;
				}
			}
		}
		
		if (current == null) {
			return root;
		}
		oldParent = current.parent;
		oldParent.lock.lock();
				
		while (current.left != null && current.right != null) {
		
			if (current.left.priority < current.right.priority) {
				DleftRotation(current);
			} else {
				DrightRotation(current);
			}
		
		}
		Node<T> parent = current.parent;
		Node<T> child;
		if (current.left != null) {
			child = current.left;
		} else {
			child = current.right;
			
		}
		
		if (parent.left == current) {
			parent.left = child;
			if(child != null)
				child.parent = parent;
		} else {
			parent.right = child;
			if(child != null)
				child.parent = parent;
		}
		
		oldParent.lock.unlock();
		return root;
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

	public String evalute(int nThreads,int nElements,String method,int nRuns, T[] vals,T[] vals2) throws InterruptedException {
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
					text += "-1 -1\n";
				}
			
			}
			else if(method.equals("delete")){
				
				try{
					for(int j = 0; j < nElements; j++){
						treapRoot = insert(treapRoot, vals[j]);
					}
					startTime = System.nanoTime();
					for (int index = 0; index < threads.length; index++) {
							final int id = index;
							threads[index] = new Thread(new Runnable() {
								public void run() {
									for (Integer j = id; j < nElements; j += threads.length)
										try {
											treapRoot = delete(treapRoot, vals[j]);											
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
					text += "-1 -1\n";
				}
			}
			else if(method.equals("mixed")){
				try{
					for(int j = 0; j < nElements/2; j++){
						treapRoot = insert(treapRoot, vals[j]);
					}
					startTime = System.nanoTime();
					for (int index = 0; index < threads.length; index++) {
							final int id = index;
							threads[index] = new Thread(new Runnable() {
								public void run() {
									for (Integer j = id; j < nElements; j += threads.length)
										try {
											treapRoot = delete(treapRoot, vals[j]);
											treapRoot = insert(treapRoot,vals2[j]);											
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
					text += "-1 -1\n";
				}
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
			int[] nThreads = {4};
			int[] nElements = {10000};
			String[] methods = {"mixed","insert"};//, "delete", "contains"};
			String[] implementations = {"parallel"};//, "sequential"};
			int nRuns = 5;
			FileWriter writer= null;
			String text;
			String filename;
			Integer[] vals;
			Integer[] vals2;

			for (int i = 0; i < nThreads.length; i++) {
				for (int j = 0; j < nElements.length; j++) {
					for (int k = 0; k < methods.length; k++) {
				
						t= new ParallelTreap<Integer>();
						vals = new Integer[nElements[j]];
						vals2 = new Integer[nElements[j]];
						for (int m = 0; m < nElements[j]; m++) {
							vals[m] = m;
							vals2[m] = m+nElements[j];
						} 
						filename = "./evaluation/data/"+nThreads[i] + "_" + nElements[j] + "_" + methods[k] + "_" + "parallel.txt";
						try {
							writer = new FileWriter(filename);
							text = t.evalute(nThreads[i], nElements[j], methods[k],nRuns, vals,vals2);
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
		}
	}
}
