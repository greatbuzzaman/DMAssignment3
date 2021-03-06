import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Vector;

import Jama.Matrix;

/*Akshay Data Mining HW3 on Markov Clustering*/

public class MarkovClustering {

	int e = 2; // Power parameter
	int r = 2; // Inflation parameter

	public String filePath;
	
	double tolerance = (double) 1.0E-20;

	ArrayList<Integer> attNodes = new ArrayList<Integer>();//stores att nodes
	ArrayList<String> physicsNodes = new ArrayList<String>();//stores physics nodes
	ArrayList<Integer> yeastNodes = new ArrayList<Integer>();//stores yeast nodes

	double[][] attNodeMatrix;
	double[][] physicsNodeMatrix;
	double[][] yeastNodeMatrix;

	String attFilePath = System.getProperty("user.dir") + "/src/Data/attweb_net.txt";
	String physicsFilePath = System.getProperty("user.dir") + "/src/Data/physics_collaboration_net.txt";
	String yeastFilePath = System.getProperty("user.dir") + "/src/Data/yeast_undirected_metabolic.txt";
	
	String attFilePathtest = System.getProperty("user.dir") + "/src/Data/attweb_nettest.txt";
	String physicsFilePathtest = System.getProperty("user.dir") + "/src/Data/physics_collaboration_nettest.txt";
	String yeastFilePathtest = System.getProperty("user.dir") + "/src/Data/yeast_undirected_metabolictest.txt";
	
	String attCLUFilePath = System.getProperty("user.dir") + "/src/Data/attweb_netCLU.clu";
	String physicsCLUFilePathtest = System.getProperty("user.dir") + "/src/Data/physics_collaboration_netCLU.clu";
	String yeastCLUFilePathtest = System.getProperty("user.dir") + "/src/Data/yeast_undirected_metabolicCLU.clu";
	
	ArrayList<Vector> clusterList; 

	public static void main(String args[]){
		/*test tes = new test();
		tes.norm();*/
		MarkovClustering mc = new MarkovClustering();
		mc.operations();
		Matrix mat = new Matrix(mc.attNodeMatrix);
	}

	public void operations(){

		//Read all the files and create the mapping arraylists
		readFiles(attFilePath);
		readFiles(physicsFilePath);
		readFiles(yeastFilePath);
		
		//Initialize all the matrices by reading the file and the corresponding mapping matrix.
		attNodeMatrix = createMatrix(attNodes, attFilePath);
		physicsNodeMatrix = createMatrix(physicsNodes, physicsFilePath);
		yeastNodeMatrix = createMatrix(yeastNodes, yeastFilePath);
		
		//Initialize the counts
		int attCount=0;
		int physicsCount = 0;
		int yeastCount = 0;

		//printMatrix(physicsNodeMatrix);
		while(attCount<18){
		//while(!(convergence(attNodeMatrix, 0.97, 0.02) && attCount!=0)){
			//attNodeMatrix = addSelfLoops(attNodeMatrix);
			attNodeMatrix = NormalizedMatrix(attNodeMatrix);
			attNodeMatrix = ExpansionMatrix(attNodeMatrix);
			attNodeMatrix = InflationMatrix(attNodeMatrix);
			attNodeMatrix = pruneMatrix(attNodeMatrix);
			attCount++;
		}
		findClusters(attNodeMatrix);
		System.out.println("attCount" + attCount + " " + clusterList.size());
		writeCLUFile(attCLUFilePath, clusterList);
		
		double[][] temp = new double[physicsNodeMatrix.length][physicsNodeMatrix.length];
		
		//while(!withinTolerance(temp, physicsNodeMatrix)){
		while(physicsCount<21){
			//physicsNodeMatrix = addSelfLoops(physicsNodeMatrix);
			temp = physicsNodeMatrix;
			physicsNodeMatrix = NormalizedMatrix(physicsNodeMatrix);	
			physicsNodeMatrix = ExpansionMatrix(physicsNodeMatrix);
			physicsNodeMatrix = InflationMatrix(physicsNodeMatrix);
			physicsNodeMatrix = pruneMatrix(physicsNodeMatrix);
			physicsCount++;
		}
		findClusters(physicsNodeMatrix);
		System.out.println("physicsCount" + physicsCount + " " + clusterList.size());
		
		writeCLUFile(physicsCLUFilePathtest, clusterList);
		
		//mc.printMatrix(mc.attNodeMatrix.getArray());
		while(yeastCount<35){
		//while(!(convergence(yeastNodeMatrix, 0.97, 0.02) && yeastCount!=0)){
			//yeastNodeMatrix = addSelfLoops(yeastNodeMatrix);
			yeastNodeMatrix = NormalizedMatrix(yeastNodeMatrix);
			yeastNodeMatrix = ExpansionMatrix(yeastNodeMatrix);
			yeastNodeMatrix = InflationMatrix(yeastNodeMatrix);
			yeastNodeMatrix = pruneMatrix(yeastNodeMatrix);
			yeastCount++;
		}
		findClusters(yeastNodeMatrix);
		System.out.println("yeastCount" + yeastCount + " " + clusterList.size());
		writeCLUFile(yeastCLUFilePathtest, clusterList);
		
		System.out.println("Done");
		
		/*for(int i=0;i<clusterList.size();i++){
			Iterator<Integer> iter = clusterList.get(i).iterator();
			while(iter.hasNext())
				System.out.print(iter.next() + " ");
			System.out.println();
		}*/
		writeFile(attFilePathtest,attNodeMatrix);
		writeFile(physicsFilePathtest,physicsNodeMatrix);
		writeFile(yeastFilePathtest, yeastNodeMatrix);
	}
	
	public void writeFile(String filePath, double[][] inputArr){
		try {
			int dimensions = inputArr.length;
			File file = new File(filePath);
			if(!file.exists())
				file.createNewFile();
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			for(int i=0;i<dimensions;i++){
				for(int j=0;j<dimensions;j++){
					bw.write(inputArr[i][j] + "\t");
				}
				bw.newLine();
			}
			bw.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void writeCLUFile(String filePath, ArrayList<Vector> inputArr){
		try {
			
			int dimensions = 0;
			if(filePath.contains("attweb")){
				dimensions = attNodes.size();
			}
			if(filePath.contains("physics")){
				dimensions = physicsNodes.size();
			}
			if(filePath.contains("yeast")){
				dimensions = yeastNodes.size();
			}
			
			File file = new File(filePath);
			if(!file.exists())
				file.createNewFile();
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			int count = 0; //counter for node / line you're writing data for.
			System.out.println(dimensions);
			bw.write("*Vertices " + new Integer(dimensions).toString());
			while(count<dimensions){
				for(int i=0;i<inputArr.size();i++){
					Vector vect = inputArr.get(i);
					for(int j=0;j<vect.size();j++){
						if((int)vect.get(j) == count){
							//System.out.println(i);
							bw.newLine();
							bw.write(new Integer(i).toString());
							count++;
						}
					}
				}
			}
			bw.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//Take input from the user. 
	public void getInfofrmUser(){
		Scanner userInput = new Scanner(System.in);
		System.out.println("*********** IMPLEMENTATION OF Markov(MCL) ALGORITHM **********");
		//fileName="D:/UB 2012-13/Java files/MCLalgo/src/data/yeast_undirected_metabolic.txt";
		filePath = System.getProperty("user.dir") + "/src/Data/attweb_net.txt";
		System.out.println("Enter the file path"); // to read file path
		InputStreamReader input = new InputStreamReader(System.in);
		BufferedReader reader = new BufferedReader(input);
	}
	public void readFiles(String filePath){

		FileReader fr;
		try {
			String line;
			fr = new FileReader(filePath);
			BufferedReader br = new BufferedReader(fr);

			while((line = br.readLine())!=null){
				String[] elements = line.split("\\s+"); // Extracting numbers in each line. Nodes per edge in our case.
				for(int i=0;i<elements.length;i++){
					if(filePath.contains("attweb")){
						if(searchIntElement(attNodes, Integer.parseInt(elements[i]))==99999){
							attNodes.add(Integer.parseInt(elements[i]));// add unique nodes to the array list.
						}
					}

					if(filePath.contains("physics")){
						if(searchStringElement(physicsNodes, elements[i])==99999){
							physicsNodes.add(elements[i]);// add unique nodes to the array list.
						}
					}

					if(filePath.contains("yeast")){
						if(searchIntElement(yeastNodes, Integer.parseInt(elements[i]))==99999){
							yeastNodes.add(Integer.parseInt(elements[i]));// add unique nodes to the array list.
						}
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*Returns the index of the element you were looking for. 
	Returns 99999 otherwise considering the arralist has less than that many elements.*/
	public int searchIntElement(ArrayList<Integer> array, int num){
		int retElement = 99999; 
		for(int i=0;i<array.size();i++){
			if(array.get(i) == num){
				retElement = i;
			}
		}
		return retElement;
	}
	
	public boolean searchVectorElement(ArrayList<Vector> array, Vector vect){
		boolean flag = false;
		if(array!=null && array.size() > 0){
		for(int i=0;i<array.size();i++){
			Vector vect1 = array.get(i);
			if(vect1.size() == vect.size()){
				flag = true;
				for(int j=0;j<vect1.size();j++){
					Collections.sort(vect);
					Collections.sort(vect1);
					if(vect1.get(j)!=vect.get(j)){
						flag = false;
						break;
					}
				}
			}
		}
		}else{
			flag = false;
		}
		return flag;
	}
	
	public int searchStringElement(ArrayList<String> array, String str){
		int retElement = 99999; 
		for(int i=0;i<array.size();i++){
			if(array.get(i).equals(str)){
				retElement = i;
			}
		}
		return retElement;
	}

	//Print the contents of the array list. 
	public void printArrayList(ArrayList arrList){
		System.out.println(arrList.size());
		for(int i=0;i<arrList.size();i++){
			System.out.println("Index i = " + i + " -- " + arrList.get(i));
		}
	}

	//read nodes from the file and 
	public double[][] createMatrix(ArrayList array, String filePath){
		double[][] matrix = new double[array.size()][array.size()];
		FileReader fr;
		try {
			String line;
			fr = new FileReader(filePath);
			BufferedReader br = new BufferedReader(fr);

			while((line = br.readLine())!=null){
				String[] elements = line.split("\\s+"); // Extracting numbers in each line. Nodes per edge in our case.

				if(filePath.contains("attweb")){
					int m = searchIntElement(attNodes, Integer.parseInt(elements[0])); 
					int n = searchIntElement(attNodes, Integer.parseInt(elements[1]));
					if(m!=99999 && n!=99999){
						matrix[m][n] = 1;//There's an edge so set that index intersection element as 1
						matrix[n][m] = 1;//There's an edge so set that index intersection element as 1
					}
				}

				if(filePath.contains("physics")){
					int m = searchStringElement(physicsNodes, elements[0]); 
					int n = searchStringElement(physicsNodes, elements[1]);
					if(m!=99999 && n!=99999){
						matrix[m][n] = 1;//There's an edge so set that index intersection element as 1
						matrix[n][m] = 1;//There's an edge so set that index intersection element as 1
					}
				}

				if(filePath.contains("yeast")){
					int m = searchIntElement(yeastNodes, Integer.parseInt(elements[0])); 
					int n = searchIntElement(yeastNodes, Integer.parseInt(elements[1]));
					if(m!=99999 && n!=99999){
						matrix[m][n] = 1;//There's an edge so set that index intersection element as 1
						matrix[n][m] = 1;//There's an edge so set that index intersection element as 1
					}
				}
			}
		}catch(FileNotFoundException fne){
			System.out.println("File not found");
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return matrix;
	}

	//Make the diagonal elements = 1
	public double[][] addSelfLoops(double[][] mat){
		for(int i=0;i<mat.length;i++){
			mat[i][i] = 1;
		}
		return mat;
	}

	//Multiplying the matrix with itself - Expansion
	public  double[][] ExpansionMatrix(double[][] inputMatrix){
		int dimensions = inputMatrix.length;
		
		double Val=0.0;
		double[][] expansionMatrix=new double[dimensions][dimensions];
		for(int i=0;i<dimensions;i++)
		{
			for(int j=0;j<dimensions;j++)
			{
				double sum=0.0;
				for(int k=0;k<dimensions;k++)
				{
					sum+= inputMatrix[i][k]*inputMatrix[k][j];
				}
				expansionMatrix[i][j] = sum;
				//Val=(double)sum;
				//expansionMatrix[i][j]=(double)Math.round(Val*1000)/1000; //Scrapping decimal places
			}
		}
		return expansionMatrix;
	}

	//Squaring each element - Inflating
	public double[][] InflationMatrix(double[][] inputMatrix){
		int dimensions = inputMatrix.length;
		double sqVal=0.0;
		double[][] inflationMatrix=new double[dimensions][dimensions];
		for(int i=0;i<dimensions;i++){
			for(int j=0;j<dimensions;j++){
				double sqsum=0.0;
				sqsum+= inputMatrix[i][j]*inputMatrix[i][j];
				inflationMatrix[i][j] = sqsum;
				//sqVal=(double)sqsum;
				//inflationMatrix[i][j]=(double)Math.round(sqVal*1000)/1000; //scrapping decimal places
			}
		}
		return inflationMatrix;
	}

	//Print a 2D matrix
	void printMatrix(double[][] array){
		for(int i=0;i<array.length;i++){
			for(int j=0;j<array[i].length;j++){
				System.out.print(array[i][j] + "\t");
			}
			System.out.println();
		}
	}

	//Dividing each element by the sum of the elements in it's column - Normalizing
	public double[][] NormalizedMatrix(double[][] randMatrix){
		double[] sums = new double[randMatrix.length];
		
		for(int i =0;i<randMatrix.length;i++){
			for(int j =0; j<randMatrix[i].length;j++){
				sums[i] = sums[i] + randMatrix[j][i]; // Sum of values in a column
			}
			//System.out.println(sums[i]);
		}
		
		for(int i=0;i<randMatrix.length;i++){   
            for(int j=0;j<randMatrix[i].length;j++)  {
            	randMatrix[i][j] = randMatrix[i][j]/sums[j];
            }
		}
		return randMatrix;
	}

	//Replace values less than min as 0 and those greater than max as 1 - Pruning
	public double[][] pruneMatrix(double[][] inputArr){
		for(int i =0;i<inputArr.length;i++){
			for(int j =0; j<inputArr[i].length;j++){
				if(inputArr[i][j] < tolerance)
					inputArr[i][j] = 0;
			}
		}
		return inputArr;
	}

	//Verify if onvergence is achieved by checking if there is any value in the matrix that lies between min and max.
	/*public boolean convergence(double[][] inputArr, double max, double min){
		for(int i =0;i<inputArr.length;i++){
			for(int j =0; j<inputArr[i].length;j++){
				if(inputArr[i][j] > min && inputArr[i][j] < max){
					//System.out.println(inputArr[i][j] + " ** " + i + " ** " +j);
					return false;
				}
			}
		}
		return true;
	}*/
	public void findClusters(double[][] inputArr){
		clusterList = new ArrayList<>();
		int  count = 0;
		for(int i =0;i<inputArr.length;i++){
			if(inputArr[i][i]>0){
				count++;
				Vector vect = new Vector();
				for(int j =0; j<inputArr[i].length;j++){
					if(inputArr[i][j] > 0)
						vect.add(j); // add all the nodes in the row to the cluster.
				}
				if(!searchVectorElement(clusterList, vect))
					clusterList.add(vect); //add it to the list only if not found above.
			}
		}
		System.out.println("** " + count);
	}
	
	public boolean withinTolerance(double[][] oldMat, double[][] newMat){
		int dimensions = oldMat.length;
		int equalElements=0;
		equalElements=0;
		boolean flag = false;
		int count = 0;
		for(int i=0;i<dimensions;i++){
			for(int j=0;j<dimensions;j++){
				count++;//count total elements in the matrix for finding the ratio later on
				if(oldMat[i][j] == newMat[i][j]){
					equalElements++;
				}
			}
		}          
		if((equalElements/count) <0.05){
			flag = true;
		}
		return flag;
	}
}