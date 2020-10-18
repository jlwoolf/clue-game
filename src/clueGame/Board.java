package clueGame;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class Board {
	private BoardCell[][] grid;
	private int numRows;
	private int numColumns;
	
	private String layoutConfigFile;
	private String setupConfigFile;
	private Map<Character, Room> roomMap;
	
	private Set<BoardCell> targets;
	private Set<BoardCell> visited;
	
	//Singleton Design Pattern implementation
	private static Board theInstance = new Board();
	private Board() {
		super();
	}
	public static Board getInstance() {
		return theInstance;
	}
	public void initialize() {
		try {
			loadSetupConfig();
		} catch (FileNotFoundException | BadConfigFormatException e) {
			e.printStackTrace();
		}
		
		try {
			loadLayoutConfig();
		} catch (FileNotFoundException | BadConfigFormatException e) {
			e.printStackTrace();
		}
		
		calcAdj();
	}
	
	//initialization methods
	public void setConfigFiles(String layoutConfigFile, String setupConfigFile) {
		this.layoutConfigFile = "data/" + layoutConfigFile;
		this.setupConfigFile = "data/" + setupConfigFile;
	}

	//load setup config and check for errors
	public void loadSetupConfig() throws FileNotFoundException, BadConfigFormatException{
		Scanner fileReader = new Scanner(new File(setupConfigFile));
		roomMap = new HashMap<Character, Room>();
		//parse through file and add each room to roomMap
		while(fileReader.hasNext()) {
			String line = fileReader.nextLine();
			if(line.contains("//"))
					continue;
			
			//make sure only two types of spaces exist, Card and Other
			//throws error if otherwise
			String[] lineContents = line.split(", ");
			if(!lineContents[0].equals("Room") && !lineContents[0].equals("Space"))
				throw new BadConfigFormatException();
			
			String roomName = lineContents[1];
			Character roomChar = lineContents[2].charAt(0);
			
			Room room = new Room(roomName);
			roomMap.put(roomChar, room);
		}
		fileReader.close();
	}
	
	//load layout config and check for errors
	@SuppressWarnings("resource")
	public void loadLayoutConfig() throws FileNotFoundException, BadConfigFormatException {
		//get number of rows and columns to initialze the grid
		File file = new File(layoutConfigFile);
		Scanner fileReader = new Scanner(file);
		setRowsColumns(fileReader);
		grid = new BoardCell[numRows][numColumns];
		fileReader.close();
		
		//re-run through file line by line
		fileReader = new Scanner(file);
		for(int i = 0; i < numRows; i++) {
			//get line contents and split into each cell
			String line = fileReader.nextLine();
			String[] lineContents = line.split(",");
			for(int j = 0; j < numColumns; j++) {
				//verify that room is a valid room from setup file
				if(!roomMap.containsKey(lineContents[j].charAt(0))) {
					throw new BadConfigFormatException();
				} else {
					//update grid cell with proper infromation from file
					grid[i][j] = new BoardCell(i, j, lineContents[j].charAt(0));
					if(lineContents[j].contains("*")) {
						grid[i][j].setRoomCenter(true);
						roomMap.get(grid[i][j].getInitial()).setCenterCell(grid[i][j]);
					}
					if(lineContents[j].contains("#")) {
						grid[i][j].setLabel(true);
						roomMap.get(lineContents[j].charAt(0)).setLabelCell(grid[i][j]);
					}
					if(lineContents[j].contains("^")) {
						grid[i][j].setDoorway(DoorDirection.UP);
					} else if(lineContents[j].contains("v")) {
						grid[i][j].setDoorway(DoorDirection.DOWN);
					} else if(lineContents[j].contains("<")) {
						grid[i][j].setDoorway(DoorDirection.LEFT);
					} else if(lineContents[j].contains(">")) {
						grid[i][j].setDoorway(DoorDirection.RIGHT);
					}
					
					try {
						if(roomMap.containsKey(lineContents[j].charAt(1))) {
						grid[i][j].setSecretPassage(lineContents[j].charAt(1));
						}
					} catch (StringIndexOutOfBoundsException e) {
						continue;
					}
				}
			}
		}
		
	}
	//calculate row and column counts
	private void setRowsColumns(Scanner fileReader) throws BadConfigFormatException {
		int rows = 0;
		int cols = 0;
		numColumns = 0;
		while(fileReader.hasNextLine()) {
			String line = fileReader.nextLine();
			String[] lineContents = line.split(",");
			cols = lineContents.length;
			if(numColumns == 0)
				numColumns = cols;
			else if(numColumns != cols)
				throw new BadConfigFormatException(numColumns, cols);
			
			rows++;
		}
		numRows = rows;
	}
	//getters for rows and columns
	public int getNumRows() {
		return numRows;
	}
	public int getNumColumns() {
		return numColumns;
	}
	
	//getters for room of cell or character
	public Room getRoom(char roomChar) {
		return roomMap.get(roomChar);
	}
	public Room getRoom(BoardCell cell) {
		return roomMap.get(cell.getInitial());
	}
	
	//methods for getting target spaces
	public void calcTargets(BoardCell startCell, int pathLength) {
		visited = new HashSet<>();
		visited.add(startCell);
		targets = recursiveTargets(startCell, pathLength);
	}
	public Set<BoardCell> getTargets() {
		return targets;
	}	
	
	private Set<BoardCell> recursiveTargets(BoardCell startCell, int pathLength) {
		Set<BoardCell> targetSet = new HashSet<>();

		for(BoardCell adjCell : startCell.getAdjList()) {
			if(visited.contains(adjCell) || adjCell.isOccupied()) {
				continue;
			} 
			visited.add(adjCell);
			
			if(pathLength == 1 || adjCell.isRoom()) {
				targetSet.add(adjCell);
			} else {
				targetSet.addAll(recursiveTargets(adjCell, pathLength-1));
			}
			visited.remove(adjCell);
		}

		return targetSet;
	}
	private void calcAdj() {
		for(int i = 0; i < numRows; i++) {
			for(int j = 0; j < numColumns; j++) {
				if(i != 0)
					grid[i][j].addAdj(this.getCell(i-1,j));
				if(i != numRows-1)
					grid[i][j].addAdj(this.getCell(i+1,j));
				if(j != 0)
					grid[i][j].addAdj(this.getCell(i,j-1));
				if(j != numColumns-1)
					grid[i][j].addAdj(this.getCell(i,j+1));
			}
		}
	}
	
	//getter for cell in grid
	public BoardCell getCell(int i, int j) {
		return grid[i][j];
	}
}