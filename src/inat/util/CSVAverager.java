package inat.util;


import java.io.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;


/**
 * This class is not currently used.
 * It takes a series of .csv files (with the same headings)
 * and produces a new .csv with the averages of the given data series
 */
//Get a series of csv files and output one csv file containing the averages of the columns of all input files
public class CSVAverager {
	private File[] csvFiles = null;
	private Vector<Vector<double[]>> data = null; //the arrays of double here are of the same size of the array of headers (i.e., one of the double[] contains one line of one csv file)
	
	public CSVAverager(String dirName) {
		this((new File(dirName)).listFiles());
	}
	
	public CSVAverager(File[] csvFiles) {
		this.csvFiles = csvFiles;
		data = new Vector<Vector<double[]>>(this.csvFiles.length);
		data.setSize(this.csvFiles.length);
	}
	
	public void average(String outputFileName, boolean computeAlsoStdDev) throws Exception {
		File outputFile = new File(outputFileName);
		Vector<Vector<String>> files = new Vector<Vector<String>>();
		String[] lines = new String[csvFiles.length];
		files.setSize(csvFiles.length);
		int[] fileStatus = new int[csvFiles.length];
		final int PAUSE = 0, //non devo leggere la prossima riga: ho ancora quella vecchia da finire
				  CONTINUE = 1, //devo leggere la prossima riga
				  EOF = 2; //il file è già finito: non devo leggere proprio niente
		BufferedReader reader = null;
		String firstLine = null; //the first line: we copy it on our output, as it should be the same for every input file
		double[] averageRow = null,
				 stdDevRow = null;
		String[] columnHeaders = null;
		for (int i=0;i<csvFiles.length;i++) {
			reader = new BufferedReader(new FileReader(csvFiles[i]));
			if (i == 0) {
				firstLine = reader.readLine();
				StringTokenizer tok = new StringTokenizer(firstLine, ",");
				averageRow = new double[tok.countTokens()]; //this is the value of the current row which will be output on the file
				if (computeAlsoStdDev) stdDevRow = new double[averageRow.length];
				columnHeaders = new String[averageRow.length];
				int idx = 0;
				while (tok.hasMoreTokens()) {
					columnHeaders[idx++] = tok.nextToken();
				}
			} else {
				reader.readLine();
			}
			files.set(i, new Vector<String>());
			String line = null;
			while (true) {
				line = reader.readLine();
				if (line == null || line.length() < 1) break;
				files.elementAt(i).add(line);
			}
			reader.close();
			fileStatus[i] = CONTINUE;
		}
		
		BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));
		//out.write(firstLine);
		out.write(columnHeaders[0] + ",");
		for (int i=1;i<columnHeaders.length-1;i++) {
			out.write(columnHeaders[i] + ",");
			if (computeAlsoStdDev) out.write(columnHeaders[i] + "_StdDev,");
		}
		out.write(columnHeaders[columnHeaders.length-1]);
		if (computeAlsoStdDev) out.write("," + columnHeaders[columnHeaders.length-1] + "_StdDev");
		out.newLine();
		DecimalFormat formatter = new DecimalFormat("#.####", new DecimalFormatSymbols(Locale.US));
        
		double[][] currentRow = new double[csvFiles.length][averageRow.length]; //this is the values of the current row of the matrix on the different files
		boolean allFinished = false;
		int countValid = 0;
		while (!allFinished) { //scorriamo le righe finché non restano più file che hanno ancora righe
			allFinished = true;
			countValid = 0;
			averageRow[0] = Double.NaN;
			for (int i=1;i<averageRow.length;i++) {
				averageRow[i] = 0;
				if (computeAlsoStdDev) stdDevRow[i] = 0;
			}
			for (int i=0;i<csvFiles.length;i++) {
				if (fileStatus[i] == EOF) continue;
				if (files.elementAt(i).size() == 0 || files.elementAt(i).elementAt(0) == null || files.elementAt(i).elementAt(0).length() < 1) {
					fileStatus[i] = EOF;
					continue;
				}
				if (fileStatus[i] == CONTINUE) {
					lines[i] = files.elementAt(i).remove(0);
				}
				allFinished = false;
				StringTokenizer tok = new StringTokenizer(lines[i], ",");
				currentRow[i][0] = Double.parseDouble(tok.nextToken());
				if (Double.isNaN(averageRow[0]) || currentRow[i][0] < averageRow[0]) {
					averageRow[0] = currentRow[i][0];
					if (computeAlsoStdDev) stdDevRow[0] = currentRow[i][0];
				}
				for (int j=1;j<currentRow[i].length;j++) {
					currentRow[i][j] = Double.parseDouble(tok.nextToken());
				}
			}
			if (allFinished) break;
			for (int i=0;i<csvFiles.length;i++) {
				if (currentRow[i][0] == averageRow[0] && fileStatus[i] != EOF) {
					fileStatus[i] = CONTINUE;
					for (int j=1;j<currentRow[i].length;j++) {
						averageRow[j] += currentRow[i][j];
						if (computeAlsoStdDev) stdDevRow[j] += currentRow[i][j] * currentRow[i][j];
					}
					countValid++;
				} else {
					fileStatus[i] = PAUSE;
				}
			}
			
			out.write(averageRow[0] + ",");
			for (int i=1;i<averageRow.length-1;i++) {
				double currAverage = averageRow[i] / countValid,
					   currStdDev = 0;
				if (computeAlsoStdDev) currStdDev = Math.sqrt((stdDevRow[i] - 2 * currAverage * averageRow[i] + countValid * currAverage * currAverage) / countValid);
				out.write("" + formatter.format(currAverage) + ",");
				if (computeAlsoStdDev) {
					if (currStdDev > 1e-8) out.write("" + formatter.format(currStdDev) + ",");
					else out.write(",");
				}
			}
			double lastAverage = averageRow[averageRow.length-1] / countValid,
				   lastStdDev = 0;
			if (computeAlsoStdDev) lastStdDev = Math.sqrt((stdDevRow[averageRow.length-1] - 2 * lastAverage * averageRow[averageRow.length-1] + countValid * lastAverage * lastAverage) / countValid);
			out.write("" + formatter.format(lastAverage));
			if (computeAlsoStdDev) {
				if (lastStdDev > 1e-8) out.write("," + lastStdDev);
				else out.write(",");
			}
			out.newLine();
		}
		out.close();
	}
	
	//Old version: tries to do everything on the fly, but it keeps too many files open! (e.g., when asking for an average on 1000 runs, it stops because it cannot open any more files)
	/*public void average1(String outputFileName) throws Exception {
		File outputFile = new File(outputFileName);
		String[] lines = new String[csvFiles.length];
		int[] fileStatus = new int[csvFiles.length];
		final int PAUSE = 0, //non devo leggere la prossima riga: ho ancora quella vecchia da finire
				  CONTINUE = 1, //devo leggere la prossima riga
				  EOF = 2; //il file è già finito: non devo leggere proprio niente
		BufferedReader[] readers = new BufferedReader[csvFiles.length];
		String firstLine = null; //the first line: we copy it on our output, as it should be the same for every input file
		double[] averageRow = null;
		for (int i=0;i<csvFiles.length;i++) {
			readers[i] = new BufferedReader(new FileReader(csvFiles[i]));
			if (i == 0) {
				firstLine = readers[i].readLine();
				averageRow = new double[new StringTokenizer(firstLine, ",").countTokens()]; //this is the value of the current row which will be output on the file
			} else {
				readers[i].readLine();
			}
			fileStatus[i] = CONTINUE;
		}
		
		BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));
		out.write(firstLine);
		out.newLine();
		DecimalFormat formatter = new DecimalFormat("#.####", new DecimalFormatSymbols(Locale.US));
        
		double[][] currentRow = new double[csvFiles.length][averageRow.length]; //this is the values of the current row of the matrix on the different files
		boolean allFinished = false;
		int countValid = 0;
		while (!allFinished) { //scorriamo le righe finché non restano più file che hanno ancora righe
			allFinished = true;
			countValid = 0;
			averageRow[0] = Double.NaN;
			for (int i=1;i<averageRow.length;i++) {
				averageRow[i] = 0;
			}
			for (int i=0;i<csvFiles.length;i++) {
				if (fileStatus[i] == EOF) continue;
				if (fileStatus[i] == CONTINUE) {
					lines[i] = readers[i].readLine();
				}
				if (lines[i] == null || lines[i].length() < 2) {
					fileStatus[i] = EOF;
					continue;
				}
				allFinished = false;
				StringTokenizer tok = new StringTokenizer(lines[i], ",");
				currentRow[i][0] = Double.parseDouble(tok.nextToken());
				if (Double.isNaN(averageRow[0]) || currentRow[i][0] < averageRow[0]) {
					averageRow[0] = currentRow[i][0];
				}
				for (int j=1;j<currentRow[i].length;j++) {
					currentRow[i][j] = Double.parseDouble(tok.nextToken());
				}
			}
			if (allFinished) break;
			for (int i=0;i<csvFiles.length;i++) {
				if (currentRow[i][0] == averageRow[0] && fileStatus[i] != EOF) {
					fileStatus[i] = CONTINUE;
					for (int j=1;j<currentRow[i].length;j++) {
						averageRow[j] += currentRow[i][j];
					}
					countValid++;
				} else {
					fileStatus[i] = PAUSE;
				}
			}
			
			out.write(averageRow[0] + ",");
			for (int i=1;i<averageRow.length-1;i++) {
				out.write("" + formatter.format(averageRow[i] / countValid) + ",");
			}
			out.write("" + formatter.format(averageRow[averageRow.length-1] / countValid));
			out.newLine();
		}
		out.close();
	}*/
	
	public static void main(String[] args) throws Exception {
		if (args.length >= 2) {
			CSVAverager av = new CSVAverager(args[0]);
			av.average(args[1], false);
			System.out.println("Ok, written file " + args[1]);
		} else {
			System.out.println("Expected 2 parameters:");
			System.out.println(" (1) directory with only .csv files inside it"); //you can explain me why in the 9 hells the File.listFiles method does *not* accept a FileFilter argument, even if it is in the documentation. Is it a problem of my JDK??
			System.out.println(" (2) output .csv files with all the averages");
		}
	}
}
