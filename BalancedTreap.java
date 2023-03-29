import java.util.*;
import java.io.*;

public class BalancedTreap<T extends Comparable<T>> {
	static Random rng = new Random();
	
	static class Node<T> {
		T value;
		int priority;
		int size = 1;
		
		Node<T> left = null, right = null,parent=null;
		
		public Node(T v) {
			this.value = v;
			this.priority = rng.nextInt();
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
		
		public static int size(Node root) {
			return root == null ? 0 : root.size;
		}
		
		public static void compute_size(Node root) {
			if (root == null)
				return;
			root.size = size(root.left) + size(root.right) + 1;
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

	public int height(Node<T> root) {
		if (root == null) return 0;
		return Math.max(height(root.left), height(root.right)) + 1;
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
	
		return root = parent;
	}	
    
//	public Node<T> insertRecursive(Node<T> root, T value) {
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

	public String evalute(int nElements,String method,int nRuns, T[] vals) throws InterruptedException {
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
		Node<T> root;

		for(int i =0; i < nRuns; i++){
			root = new Node(-1, Integer.MAX_VALUE);
			
			if(method.equals("insert")){
				try{
					startTime = System.nanoTime();
					
					for(int j = 0; j < nElements; j++){
						root = this.insert(root, vals[j]);
					}
			
					endTime = System.nanoTime();
					totalTime += endTime - startTime;
					height = this.height(root);
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
				
			}
			else if(method.equals("contains")){
				
			}
			else{
				System.out.println("Invalid method or implementation");
			}
		}
		averageTime = totalTime / success;
		averageHeight = totalHeight / success;
		return text;
	}
	
	public static void main(String[] args) {
		
		boolean runEvaluation = true;
		
		if (runEvaluation) {
			BalancedTreap<Integer> t;
			int[] nElements = {100,1000,10000,100000, 1000000};
			String[] methods = {"insert"};//, "delete", "contains"};
			int nRuns = 10;
			FileWriter writer= null;
			String text;
			String filename;
			Integer[] vals;

			
			for (int j = 0; j < nElements.length; j++) {
				for (int k = 0; k < methods.length; k++) {
					
						t= new BalancedTreap<Integer>();
						vals = new Integer[nElements[j]];
						for (int m = 0; m < nElements[j]; m++) {
							vals[m] = m;
						} 
						filename = "./evaluation/data/"+nElements[j] + "_" + methods[k] + "_" +"parallel.txt";
						try {
							writer = new FileWriter(filename);
							text = t.evalute(nElements[j], methods[k],nRuns, vals);
							writer.write(text);
						} catch (Exception e) {
							System.err.println("Error writing to "+filename+": " + e.getMessage());
						} finally {
							try {
								if (writer != null) {
									writer.close();
								}
							} catch (Exception e) {
								System.err.println("Error closing "+filename+": " + e.getMessage());
							}
						}
						
					}
				}
			}
			
		
		else {
			Node<Integer> root = null;
			
			BalancedTreap<Integer> t = new BalancedTreap<>();
			
			for (int i = 0; i < 10; i++) {
				root = t.insert(root, i);
			}
			
			t.inorder(root);
			
			for (int i = 30; i >= 20; i--)
				root = t.insert(root, i);
			
			t.inorder(root);
			
			BalancedTreap<Integer> testSpeed = new BalancedTreap<>();
			for (int i = 0; i <= 1e7; i++) {
				testSpeed.insert(root, i);
			}
		}
	}
}
