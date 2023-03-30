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

	public int nChildren(Node<T> root) {
		int count = 0;
		if (root.left != null)
			count++;
		if (root.right != null)
			count++;
		return count;
	}

	public void lockSubtree(Node<T> root, boolean toLock) throws InterruptedException {
		if (root == null) {
			return;
		}
		if (toLock) {
			if (root.lock.tryLock()) {
				lockSubtree(root.left, true);
				lockSubtree(root.right, true);
			} else {
				root.lock.lockInterruptibly();  // acquire the lock, potentially blocking the thread
				lockSubtree(root.left, true);   // now that we have the lock, lock the subtrees
				lockSubtree(root.right, true);
			}
		} else {
			root.lock.unlock();               // unlock the current node
			lockSubtree(root.left, false);    // unlock the subtrees
			lockSubtree(root.right, false);
		}
	}

	// public Node<T> delete(Node<T> root, T toDelete) throws InterruptedException {
		
	// 	Node<T> temp = root;

	// 	if (temp == null) {
	// 		System.out.println('n');
	// 		return temp;
	// 	}
		
	// 	while(temp != null){
	// 		temp.lock.lock();
	// 		if(toDelete.compareTo(temp.value)==0){
	// 			break;
	// 		}
	// 		if(toDelete.compareTo(temp.value) > 0){
	// 			// temp.lock.unlock();
	// 			temp = temp.right;
	// 		}
	// 		else{
	// 			// temp.lock.unlock();
	// 			temp = temp.left;
	// 		}
	// 	}
	// 	if (temp == null) {
	// 		System.out.println('n');
	// 		return root;
	// 	}
		
	
	// 	//lock all self and subtrees
	// 	//lockSubtree(temp,true);//(node, 0:unlock 1:lock)
		
	// 	while(nChildren(temp) > 1){
	// 		if(temp.right.priority > temp.left.priority){
	// 			leftRotation(temp);
	// 			// temp.parent.lock.unlock();
	// 			//lockSubtree(temp.parent.right,false);
	// 		}
	// 		else{
	// 			rightRotation(temp);
	// 			//temp.parent.lock.unlock();
	// 			// lockSubtree(temp.parent.left,false);
	// 		}
	// 	}
	// 	System.out.println(temp.value + "  " + toDelete);
	// 	if(temp.parent.right == temp){
	// 		if (temp.right != null){
	// 			temp.parent.right = temp.right;
	// 			// temp.parent.lock.unlock();
	// 		}
	// 		else{
	// 			temp.parent.right = temp.left;
	// 			// temp.parent.lock.unlock();
	// 		}
	// 	}
	// 	else{
	// 		if (temp.right != null){
	// 			temp.parent.left = temp.right;
	// 			// temp.parent.lock.unlock();
	// 		}
	// 		else{
	// 			temp.parent.left = temp.left;
	// 			// temp.parent.lock.unlock();
	// 		}
	// 	}
		
	// 	return root;
	// }
	
	// public Node<T> delete(Node<T> root, T toDelete) throws InterruptedException {
	// 	Node<T> parent = null;
	// 	Node<T> current = root;
	
	// 	// Find the node to delete
		
	// 	while (current != null && !current.value.equals(toDelete)) {
	// 		parent = current;
	// 		if (toDelete.compareTo(current.value) < 0) {
				
	// 			current = current.left;
	// 		} else {
			
	// 			current = current.right;
	// 		}
	// 	}
	
	// 	// Node not found
	// 	if (current == null) {
	// 		return root;
	// 	}
	// 	lockSubtree(current,true);
	// 	// If the node has two children, swap it with the node that has the smallest priority in its right subtree
	// 	while (current.left != null && current.right != null) {
	// 		if (current.left.priority < current.right.priority) {
	// 			current = rightRotation(current);
	// 			current.parent.lock.unlock();
	// 			lockSubtree(current.parent.left,false);
	// 		} else {
	// 			current = leftRotation(current);
	// 			current.parent.lock.unlock();
	// 			lockSubtree(current.parent.right,false);
	// 		}
	// 		if (parent == null) {
	// 			root = current;
	// 		} else if (parent.left == current.right) {
	// 			parent.left = current;
	// 		} else {
	// 			parent.right = current;
	// 		}
	// 		// lockSubtree(parent,false);
	// 		parent = current;
	// 	}
	
	// 	// Delete the node
	// 	Node<T> child;
	// 	if (current.left != null) {
	// 		child = current.left;
	// 	} else {
	// 		child = current.right;
	// 	}
	// 	if (parent == null) {
	// 		root = child;
	// 	} else if (parent.left == current) {
	// 		parent.left = child;
	// 	} else {
	// 		parent.right = child;
	// 	}
	
	// 	return root;
	// }
	
	public Node<T> delete(Node<T> root, T toDelete) throws InterruptedException {
		Node<T> parent = null;
		Node<T> current = root;
		
		// Find the node to delete
		
		while (current != null && !current.value.equals(toDelete)) {
			parent = current;
			if (toDelete.compareTo(current.value) < 0) {
				current = current.left;
			} else {
				current = current.right;
			}
		}
		
		// Node not found
		if (current == null) {
			return root;
		}
		
		lockSubtree(current, true);
		
		// Delete the node
		Node<T> child;
		if (current.left != null) {
			child = current.left;
		} else {
			child = current.right;
		}
		if (parent == null) {
			root = child;
		} else if (parent.left == current) {
			parent.left = child;
		} else {
			parent.right = child;
		}
		
		lockSubtree(current, false);
		
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
			int[] nThreads = {1,2,6,8};
			int[] nElements = {10,1000,10000,100000};
			String[] methods = {"delete"};//, "delete", "contains"};
			String[] implementations = {"parallel"};//, "sequential"};
			int nRuns = 3;
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
