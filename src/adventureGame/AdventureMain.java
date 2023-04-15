package adventureGame;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/* Text adventure game by Michael Harwood
 * Version 1.8 
 * Please see "version info.txt" for what works and what doesn't
 * "Language.txt" lists all of the commands that work so far and explains what is supposed to happen with each.
 */


public class AdventureMain {

	static int INVSIZE = 10; //size of inventory	
	//instance variables
	HashMap<String,Room> allRooms = new HashMap<String,Room>();
	HashMap<String, Item> itemMap = new HashMap<String,Item>(); //list of all item objects
	ArrayList<String> inventory = new ArrayList<String>();
	String currentRoom;
	Player player;

	//This is used for a two stage warning system when you enter a dark room.
	//First there's a warning. If you do not get light somehow on the next turn, then you're dead.
	enum Dark{OKAY, WARNING, DEAD};
	Dark darkWarning = Dark.OKAY;

	public static void main(String[]args){ new AdventureMain();	}

	AdventureMain() {

		boolean playing = true;
		
		Room.setupRooms(allRooms);
		Item.setUpItems(itemMap, allRooms);
		
		String command = "";
		System.out.print("Please type your firstname: (press enter for \"Billy\") ");
		String name = getCommand();
		if (name.equals("qwerty")) name = "Billy";
		player = new Player(name); //make a new player with given name		

		startingMessage();

		/*  for ANSI screen controls -- which don't work in Windows.
		char escCode = 0x1B;
		int row = 10; int column = 10;
		System.out.print(escCode + "[2J"); //clear screen
		System.out.print(String.format("%c[%d;%df",escCode,row,column));
		 */

		lookAtRoom(true);

		/** MAIN GAME LOOP **/

		while (playing) {
			command = getCommand();

			playing = parseCommand(command);

			if (darkWarning == Dark.DEAD) {
				System.out.println("You died in the dark, and possible alone.");
				playing = false;
				continue;
			}

			if (! player.checkup() ) {
				playing = false;
				continue;
			}
			
			//TODO check if player won the game.
		}
	}


	String getCommand() {
		Scanner sc = new Scanner(System.in);		
		String text = sc.nextLine();
		if (text.length() == 0) text = "qwerty"; //default command
		return text;
	}

	String removeArticles(String str)  {
		str = " " + str + " ";          // add leading and trailing space
		while (str.indexOf(" a ") != -1) {
			str = str.replace(" a ", " ");
		}
		while (str.indexOf(" an ") != -1) {
			str = str.replace(" an ", " ");
		}
		while (str.indexOf(" the ") != -1) {
			str = str.replace(" the ", " ");
		}
		return str;
	}

	/***** PREPROCESSING *****/ 
	String preProcess(String text) {
		text = text.replaceAll(" into ", " in ");
		text = text.replaceAll(" rocks", " rock");
		text = text.replaceAll("pick up", "pickup");
		text = text.replaceAll("look at", "lookat");
		text = text.replaceAll("climb up", "climbup");
		
		//No. we have to allow "turn on flashlight" as well as "turn flashlight on"
		//text = text.replaceAll("turn on", "turnon");
		//text = text.replaceAll("turn off", "turnoff");

		//pre-parsing: What about "go to"		
		text = text.replaceAll("[^a-z0-9 ]", "");
		text = this.removeArticles(text).trim();
		return text;
	}
	

	boolean parseCommand(String text) {

		text = text.toLowerCase().trim();
		text = preProcess(text);
		text = Item.itemSynonyms(text); 

		//no words entered:
		if (text.length()==0 || text.equals("qwerty")) return true;

		String words[] = text.split(" ");

		String word1,word2,word3,word4;
		word2 = word3 = word4 = "";
		word1 = words[0];
		if (words.length > 1) word2 = words[1];
		if (words.length > 2) word3 = words[2]; 
		if (words.length > 3) word4 = words[3]; 

		//special case for "rock2".  Note, all rocks are called "rock".
		//TODO: explain, what does this do?
		if (word2.equals("rock") && allRooms.get(currentRoom).items.contains("\"rock\"")) word2 = "\"rock\"";
		if (word3.equals("rock") && allRooms.get(currentRoom).items.contains("\"rock\"")) word3 = "\"rock\"";

		/***** MAIN PROCESSING *****/
		//any command that uses up a turn and food, must run player.update();
		switch(word1) {

		// ****  one word commands  **** //
		case "quit": case "exit":
			System.out.print("Do you really want to quit the game? ");
			String ans = getCommand().toUpperCase();
			if (ans.equals("YES") || ans.equals("Y")) {
				System.out.print("Thanks for playing. Bye.");
				return false;
			}			
		case "n": case "s": case "w": case "e": case "u": case "d":
		case "north": case "south": case "west": case "east": case "up": case "down":
			moveToRoom(word1.charAt(0));
			break;
		case "i": case "inventory":
			showInventory();
			break;
		case "sleep":
			sleep();			
			break;	
		case "look":
			lookAtRoom(true);
			player.update();
			break;
		case "search":
			search();
			break;
		case "please":
			if (! word2.equals("help")) return true;
		case "pray":
			pray();
			break;
		case "help":
			printHelp();
			break;

		// *** two word commands ***
		case "climbup":
		case "climb":
			if (word2.equals("")) {
				System.out.println("What do you want to climb?");
				break;
			}
			if (word2.equals("tree")) {
				if (currentRoom.equals("forest1")) {
					System.out.println("You start climbing ...");
					moveToRoom('u');
				} else {
					System.out.println("There is no climbable tree here.");					
				}
			} else {
				System.out.println("You can't climb that.");				
			}
			break;
		case "read":
			readObject(word2);
			break;
		case "lookat":
		case "examine":
			lookAtObject(word2);
			break;

		case "pickup":
			takeObject(word2);
			break;		
		case "take":
			//take B, take B from A
			if (word3.equals("from")) {
				takeObject(word2, word4);
			} else {
				takeObject(word2);
			}
			break;
		case "drop":
			dropObject(word2);
			break;
		case "eat":
			eatItem(word2);
			break;	
		case "drink":
			drink(word2);
			break;
		case "move": //move an item. These are things you can't pick up.
			moveItem(word2);
			break;
		//FIXME
		case "put":  
			//TODO: put A in B  (why would anyone do this?) "put hammer in chest"
			//TODO: add player.update()
			//This does not work EXCEPT for these two special commands
			if (text.startsWith("put emerald in bell")) activate("bell");
			//special lake command
			else if (currentRoom.equals("black_lake") && text.startsWith("put hand in lake")) activate("lake");
			else System.out.println ("huh?");
			break;
		case "reach":
			if (currentRoom.equals("black_lake") && text.startsWith("reach in lake")) activate("lake");
			break;
			//hit rock with hammer
		case "smash":
		case "break":
		case "hit":
			if (text.contains (" rock with hammer")) activate("hammer");
			else System.out.println("Sorry, I don't understand that command");
			break;
			//use hammer to break rock
		case "use":
			if (text.startsWith("use hammer to") && text.contains("rock")) {
				activate("hammer");
				break;
			}
			System.out.println("Sorry, I don't understand that command");
			//activate(word2);
			break;

			//SPECIAL COMMANDS
			//get this working for open paper and open package, maybe also open door
		case "open":
			if (word2=="") {
				System.out.println("open what?");				
			} else {
				openStuff(word2, word3, word4);
			}			
			break;			
		
			/*	turn on flashlight.  turn off flashlight
			turn flashlight on, turn flashlight off		*/
		case "turn":
			//TODO: update this with activate flashlight() method
			
			//TODO if word2 or word3 = flashlight, then check if it is in your inventory or on the ground
			if (word3.equals("flashlight")) {
				if (word2.equals("on")) { 
					itemMap.get("flashlight").setActivate(true);
					lookAtObject("flashlight");
				}
				if (word2.equals("off")) itemMap.get("flashlight").setActivate(false);
			}
			else if (word2.equals("flashlight")) {
				if (word3.equals("on")) { 
					itemMap.get("flashlight").setActivate(true); 
					lookAtObject("flashlight");
				}
				if (word3.equals("off")) itemMap.get("flashlight").setActivate(false);
			}
			else System.out.println("Sorry, I don't understand what you want to do.");
			break;
		case "ring":
			ringBell(word2);
			break;
		default: 
			System.out.println("Sorry, I don't understand that command");
		}
		return true;
	}			

	//This will crash if you move to a room that does not exist in the hashmap.
	void moveToRoom(char dir) {
		String newRoom = allRooms.get(currentRoom).getExit(dir);

		if (newRoom.length()==0) {
			System.out.println("You can't go that way");
			return;
		}

		player.update();
		 
		//run methods for moving ... e.g. climbing the tree and falling
		if (newRoom.substring(0, 2).equals("r_")) {			
			runMethod(newRoom);
			return;
		}

		currentRoom = newRoom;		
		lookAtRoom(false);		
	}

	/* lookAtRoom:
	 * function: this displays the title and description of the room.
	 * 		If you have already been in this room, it won't display the description
	 * 		It also lists all of the items in the room.
	 * 		It prints a warning about dark rooms & death.
	 * parameters: if look is true, then it will display the description.
	 */
	void lookAtRoom(boolean look) {
		Room rm = allRooms.get(currentRoom);
		if (rm == null) { 
			System.out.println("ERROR: room \""+ currentRoom + "\" does not exist.");
			return;
		}		

		if (rm.getIsDark()) {
			if (inventory.contains("flashlight") && itemMap.get("flashlight").isActivated()) {
				//continue
			} else {
				if (darkWarning == Dark.WARNING) {
					//you are moving into a second dark room
					System.out.println("\n== ??? ==");
					System.out.println("It is pitch black in here. "		
							+ "You really have fallen into a pit and died. (I tried to warn you.)");
					darkWarning = Dark.DEAD;
					return;
				}
				System.out.println("\n== ??? ==");
				System.out.println("It is pitch black in here. "		
						+ "You will probably fall into a pit and die.");
				darkWarning=Dark.WARNING;
				return;
			}
		}

		darkWarning=Dark.OKAY; //you are no longer in a dark room.
		System.out.println("\n== " + rm.getTitle() + " ==");
		if (!rm.hasVisited() || look) {
			System.out.println("" + rm.getDesc());		
			for (String s : rm.items){
				//make sure you don't print out blank lines (e.g. in treasury);
				if (itemMap.get(s).descrRoom.trim() != "")				
					System.out.println(itemMap.get(s).descrRoom);
			}		
			rm.visit();
		}	
	}

	//FIXME: this method returns the item in the room or inventory.
	//It's only used once. NOT that useful - since you would still have to remove the item.
	//Better to make it a boolean to check if it is present or not.
	Item itemPresent(String itemName) {
		if ((inventory.contains(itemName))) {
			return itemMap.get(itemName);
		}
		Room r = allRooms.get(currentRoom);
		if (r.items.contains(itemName)) {
			return itemMap.get(itemName);
		}
		return null;
	}

	void lookAtObject(String itemName){
		//is item in inventory
		if ((inventory.contains(itemName))) {
			Item it = itemMap.get(itemName);

			if (it.isActivated()) System.out.println(it.descrActive);
			else System.out.println(it.descrLook);
			player.update();
			return;
		}
		//is item in current room?
		Room r = allRooms.get(currentRoom);
		if (r.items.contains(itemName)) {
			Item q = itemMap.get(itemName);
			if (q.isActivated()) System.out.println(q.descrActive);
			else if(q.isOpen) {
				System.out.print("The " + itemName + " is open ");
				if (q.itemContained.equals("")) System.out.println("and it is empty.");	
				else System.out.println("and it contains a " + q.itemContained);							
			}
			else System.out.println(q.descrLook);	
			player.update();
			return;
		}
		System.out.println("That object does not exist (here).");	
	}

	void readObject(String itemname) {
		if (itemname == "") {
			System.out.println("Read what?");
			return;
		}
		Item z = itemPresent(itemname);
		if (z == null) {
			System.out.println("There is no '" + itemname + "' in this location, nor in your inventory.");
			return;			
		}
		if (z.descrRead.length() > 0)
			System.out.println("The " + itemname + " says: " + z.descrRead);
		else
			System.out.println("There is no writing on the " + itemname +".");
		player.update();
	}

	//Note: this method actually does the eating in the player class.
	void eatItem(String itemname) {
		if (itemname.equals("")){
			System.out.println("eat what?");
			return;
		}
		//is item in current room? eat that item first.
		Room r = allRooms.get(currentRoom);
		if (r.items.contains(itemname)) {			
			if (! player.eat(itemMap.get(itemname))) return;				
			r.items.remove(itemname);
			player.update();
			return;				
		}
		//is item in inventory:
		for (String s : inventory) {
			if (s.equals(itemname)) {
				if (! player.eat(itemMap.get(itemname))) return;				
				inventory.remove(itemname);
				player.update();
				return;
			}
		}
		System.out.println("There is no " + itemname + " here.");
	}
	
	void drink(String liquid) {
		if (currentRoom.contentEquals("black_lake")) {
			if (player.isThirsty) {
				//if so, you get your health back.
				player.heal(10);
				System.out.println("That drink was refreshing and helped so much!");
				player.isThirsty = false;
				player.update();
				return;
			}
			//otherwise you just enjoy the drink an nothing happens.
			System.out.println("The water is very cold. ");
			if (! inventory.contains("key")) {
				System.out.println("You get a glimpse of something shiny in the dark water.");
				player.update();
			}
		}
		else {
			System.out.println("How would you drink that?");
		}
	}

	void search() {
		if (currentRoom.contentEquals("black_lake")) {
			Room r = allRooms.get(currentRoom);			
			//find key
			if (r.items.contains("key")) {			
				r.items.remove("key");
				System.out.println("You notice a shiny piece of metal in the water\n"
						+ "You pick it up and add it to your inventory.");
				inventory.add("key");
				player.update();
			} else {
				System.out.println("All you see is black water.");
			}
			return;
		}
		System.out.println("Being fairly observant all you need to do is to \"look at __\". "
				+ "\nSearching really doesn't help you at all, "
				+ "... except possibly at the lowest point in the game.");
	}
	
	void pray() {
		System.out.println("In utter desperation you pray to the divinity.\n "
				+ "This takes a lot of effort to get it to actually work.\n ... ");
		thsleep(600);
		System.out.println("\nAre you sure you want to do this? You might get injured.");
		char ans = getCommand().toUpperCase().charAt(0);
		if (ans != 'Y') {
			System.out.println("okay then");
			return;
		}
		player.injury(10);
		player.update();
		System.out.println("You see a vision of a map! It looks the north maze.");
		thsleep(600);
		String s = "\n\n";
		s+="                               &         \n";
		s+="             forest <<--------[6]********\n";
		s+="                               |        *\n";
		s+="                               |        *\n";
		s+="    @@@@@@@@@@@@@@@[4]*********|*********\n";
		s+="    @               $          |         \n";
		s+="    @               $          |         \n";
		s+="    @               $          |         \n";
		s+="   [5]########      $          |         \n";
		s+="    .        #      $          |         \n";
		s+="    .        #      $          |         \n";
		s+="    .    /<<[3]&    $          |         \n";
		s+="    .    !   |      $          |         \n";
		s+="    .    !   |      $          |         \n";
		s+="    .    !   +------$----------+         \n";
		s+="    .    !          $                    \n";
		s+="    .    \\  &       $                    \n";
		s+="    .......[2]$$$$$$$                    \n";
		s+="            ^                            \n";
		s+="            ^                            \n";
		s+="     down tree (otherside)               \n";
		
		System.out.println(s);
	}
	
	void startingMessage() {
		currentRoom = "clearing";
		String startingText = "\n\n" + player.name + ". You wake up in a forest clearing.\n "
				+ "The birds are sinning and the sky is shining.\n"
				+ "... This feels like a *very* special clearing. "
				+ "You wonder if you're in another dimension or timeline.";
		
		System.out.println(startingText);
	}

	void sleep() {
		if (currentRoom.equals("clearing")) {
			if (player.getHealth() < 75) {
				System.out.println("You have a much needed nap");
				player.heal(7);
				player.turns += 5;
				player.hunger(2);
			} else {
				System.out.println("You're too wide awake to sleep");
			}
		} else {
			System.out.println("It doesn't feel safe to sleep here.");			
		}
	}
	
	//Thread.sleep
	void thsleep(int n) {
		try {Thread.sleep(n);
		} catch (InterruptedException e) {}
	}

	void printHelp() {
		System.out.println("\n*******************************************************************************");
		System.out.println("You're in a strange land and have to complete a quest/puzzle to get home again.\n"
				+ "Try simple commands to do things. You can move in the cardinal directions\n"
				+ " as well as vertically by typing in the appropriate word."
				+ "\nOther common adventure game commands work here too: look, inventory, move, take, drop ...");
		System.out.println("There's a way to get more help if you're stuck, and there are 3 clues.");
		System.out.println("*******************************************************************************");
	}

	void takeAll() {
		Room rm = allRooms.get(currentRoom);
		int index=0;
		while(rm.items.size() > 0 && index < rm.items.size()) {
			if (inventory.size() > INVSIZE) {
				System.out.println("Your inventory is full. Drop something first.");
				return;
			}
			String eachItem = rm.items.get(index);
			//handle items that are in room that cannot be picked up (or you'll get infinite loops)
			if (!itemMap.get(eachItem).isCarryable) {
				index++;
				continue;
			}
			takeObject(eachItem);
		}
		System.out.println("Everything in the room has been added to your backpack.");
		player.update();
	}

	void takeObject(String itemName) {
		if (inventory.size() > INVSIZE) {
			System.out.println("Your inventory is full. Drop something first.");
			return;
		}

		//handle "take all"
		if (itemName.equals("all")) {
			takeAll();
			return;
		}

		//see if item is in current room.
		Room r = allRooms.get(currentRoom);
		if (! r.items.contains(itemName)) {		
			System.out.println("There is no " + itemName + " here.");
			return;
		}
		//see if item is carryable
		if (! itemMap.get(itemName).isCarryable) {
			System.out.println("You can't take that!");
			return;
		}
		//move item from room to inventory
		r.items.remove(itemName);
		inventory.add(itemName);
		System.out.println("You add the " + itemName + " to your backpack.");
		player.update();
	}

	//TAKE A FROM B
	void takeObject(String itemName, String container) {
		Room r = allRooms.get(currentRoom);
		if (! r.items.contains(container)) {		
			System.out.println("There is no " + container + " here.");
			return;
		}

		Item it2 = itemMap.get(container);
		if (! it2.isOpen) {
			System.out.println("Sorry, the " + container + " is not open.");
			return;
		}
		if (it2.itemContained.equals("")) {
			System.out.printf("The %s is empty.%n", container);
			return;
		}
		if (it2.itemContained.equals(itemName)) {
			//itemList.get(container).itemContained = "";
			if (inventory.size() > INVSIZE) {
				System.out.println("Your inventory is full. Drop something first.");
				return;
			}
			inventory.add(itemName);
			it2.itemContained = "";	//TODO does this remove it from the item??
			System.out.println("You add the " + itemName + " to your backpack.");
			player.update();

		} else {
			System.out.printf("There is no %s is in the %s.%n", itemName, container);
		}				
	}

	void dropObject(String item) {
		if (item.equals("all")) {
			while(inventory.size() > 0) {
				String nextItem = inventory.get(0);
				inventory.remove(nextItem);
				allRooms.get(currentRoom).items.add(nextItem);				
			}
			System.out.println("You drop everything!");
			player.update();
			return;			
		}

		if (inventory.contains(item)) {
			inventory.remove(item);
			allRooms.get(currentRoom).items.add(item);
			System.out.println("You drop the " + item);
			player.update();
		} else {
			System.out.println("You do not have " + item + " in your backpack.");
		}
	}

	void showInventory() {
		//show health, hunger, status, inventory
		System.out.printf("\n****************************** TURNS: %2d **************************************\n",player.turns);
		System.out.printf("Stats for %s: \tHealth=%s\t\tFood=%s\t\t%n" ,
				player.name, player.getHealth(), player.getFoodPoints());
		String s = "";
		if (player.isThirsty) s+="~~ You are thirsty. ~~\t\t";
		if (player.isPoisoned) s+="%% You are poisoned! %%\t";
		if (s.length() > 0) System.out.println(s);
		System.out.println();
		for (int i=0; i < inventory.size(); i++) {
			System.out.println((char)(i+97) + ") " + inventory.get(i));
		}
		if (inventory.size() == 0) System.out.println("You have nothing in your backpack");
		System.out.println("*******************************************************************************");
	}


	/* This method will move an item
	 * It must be in the current room and not be carryable.
	 * Items can reveal an object when they are moved. This only happens once.
	 * An item can also have a method that is called when it is moved.
	 */
	void moveItem(String itemName){
		//is item in current room?
		Room r = allRooms.get(currentRoom);
		if (r.items.contains(itemName)) {
			Item it = itemMap.get(itemName);
			if (!it.isCarryable) {	//can only move non-carryable things				
				if (! it.revealItem.isEmpty())	{ //reveal object
					System.out.println(it.moveDesc);
					//check if new revealed object is already in room:
					if (! allRooms.get(currentRoom).items.contains(it.revealItem))
						allRooms.get(currentRoom).items.add(it.revealItem);
					//stop object from being moved again (revealing new objects).		

					itemMap.get(itemName).revealItem = ""; //this should update the item in the list.
				} else {
					System.out.printf("You move the %s.%n", itemName);					
				}
				if (it.moveMethod.startsWith("m_")) runMethod(it.moveMethod);
			}
			else System.out.println("You can't move that object.");	
			player.update();
			return;
		}
		System.out.println("That object does not exist (here).");	
	}

	void closeObject(String itemName) {
		Room r = allRooms.get(currentRoom);
		if (!r.items.contains(itemName)) {
			System.out.println("That object does not exist (here).");
			return;
		}
		Item it = itemMap.get(itemName);
		if (!it.isContainer) {
			System.out.println("You cannot close that item.");
			return;
		}
		if (it.isOpen) {
			System.out.println("You close the " + itemName);
			it.isOpen = false;
			player.update();
		} else {
			System.out.println("The " + itemName + " is already closed.");
		}
	}

	//TODO: what objects get activated? (by opening?)
	//rock: hit rock with hammer or open rock with hammer, or smash rock with hammer
	//flashlight: activated in switch statement
	//lake (in black_lake room)
	//hammer
	//bell
	void activate(String itemName) {
		Room r = allRooms.get(currentRoom);
		//exit if it is not in the room and not in the inventory
		if (! r.items.contains(itemName)) {
			if (!(inventory.contains(itemName))) {
				System.out.println("You don't have " + itemName + " in your inventory.");
				return;
			}
		}
		Item it = itemMap.get(itemName);
		if (it.activatedMethod.equals("") && it.descrActive.equals(""))	{
			System.out.println("You don't know how to use this.");
			return;
		}
		it.setActivate(true);	
		if (it.activatedMethod.startsWith("a_")) {
			runMethod(it.activatedMethod);
		}
		System.out.println(it.descrActive);
		player.update();
	}

	//TODO fix this method.
	void openStuff(String w2, String w3, String w4)
	{
		//we know that w2 is not empty.

		//open A, open A with B			
		if (w3.equals("with") && w4.length() > 0) {
			//openObject(w2, w4);
			openObject(w2);
		}	
		else {
			openObject(w2);
			//else activate(word2);
		}
	}

	void openObject(String itemName) {
		//TODO: FIXME This means that you can't open things in your inventory

		Room r = allRooms.get(currentRoom);
		if (!r.items.contains(itemName)) {
			System.out.println("That object does not exist (here).");
			return;
		}
		Item it = itemMap.get(itemName);		
		if (!it.isContainer) {
			System.out.println("You cannot open that item.");
			return;
		}
		if (it.isOpen) {
			System.out.print("The " + itemName + " is already open ");
			if (it.itemContained.equals("")) {
				System.out.println("and it is empty.");	
			} else {
				System.out.println("and it contains a " + it.itemContained);
			}
			return;
		}
		String tool = it.openRequires;
		if (tool.equals("")) {
			it.isOpen = true;
			System.out.printf("You open the %s and it contains a %s.%n", itemName, it.itemContained);
		} else {
			if (inventory.contains(tool)) {
				System.out.printf("You open the %s with the %s.%n", itemName, tool);
				System.out.printf("It contains a %s.%n", it.itemContained);	
				it.isOpen = true;
			} else {
				System.out.printf("You need a %s to open this.%n", tool);
			}
		}		
	}

	/*void openObject(String itemName, String tool) {		
		openObject(itemName);
	}*/


	void ringBell(String itemName) {

		if (! itemName.equals("bell")) {
			System.out.println("You can't ring that.");
			return;			
		}
		if (! inventory.contains("bell")) {
			System.out.println("You don't have a bell to ring.");
			return;
		}
		if (! currentRoom.equals("cave2")) {
			System.out.println("The bell doesn't work in this room/location.");
			return;			
		}
		if (! itemMap.get("bell").isActivated()) {
			System.out.println("The bell will not work without an emerald clapper.");
			return;
		}

		System.out.println("You ring the bell and a beautiful liquid sound fills the cave.\n"
				+ "Everything shimmers and you find yourself back home - with the emerald bell still in your hand.\n"
				+ "\n\n **** Thanks for playing. ****");
		System.exit(0);
	}

	/************** Run special methods via reflection ****************/
	
	/**************************************************
	 * This will run the methods stored in variables
	 * for any part of the program.
	 * r_  = room method
	 * m_  = move item method
	 * a_  = activate method
	 ************************************************/
	//Ideally these methods should be in the Room or Item classes (and static), but the manipulate too many global variables.
	
	void runMethod(String methName) {		
		methName = methName.substring(2); //strip off the first two letters
		//Class<AdventureMain> clazz = AdventureMain.class;

		try {
//			Method method = clazz.getDeclaredMethod(methName, String.class);

			Method m = AdventureMain.class.getDeclaredMethod(methName);
			m.invoke(this); //method takes no parameters and we don't care about a return value
//			AdventureMain.class.getDeclaredMethod(methName).invoke(null);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	void fallFromTree(){
		System.out.println("You fall from the tree and break a leg");
		player.injury(15);
		currentRoom = "forest1";
		lookAtRoom(false);
	}

	void getFromLake() {
		//FIXME: stop the possibility of taking the same key multiple times.
		System.out.println("You feel around in the dark water and pull up a small metal object.");
		System.out.println("It's a key! The key has been added to your backpack.");
		allRooms.get("black_lake").items.add("key");
		takeObject("key");
	}

	void moveLever() {
		System.out.println("The ground trembles. You hear a deep grinding noise."
				+ "\nThe room shakes and part of it opens."
				+ "\nYou hear falling rocks. Dust is choking you.");
		player.injury(15);
		//make lever immoveable.
		Item z = new Item("The lever looks totally worn out.");	
		z.descrRoom = "A black metal lever on the wall has a plaque under it.";
		z.isCarryable = false;
		z.moveDesc = "Nothing happens when you move the lever now.";		
		itemMap.put("lever", z);		

		//change exits. Maybe open up a new room too?		
		Room r = new Room("[Former] Entrance to cave",
				"This was where you entered the cave. The eastern exit is totally blocked off with rock from the earthquake."
						+ "\nThe cave continues to the west");
		r.setExits("", "","cave2", "", "","");
		allRooms.put("cave1",r);
	}

	void moveLeaves() {
		System.out.println("[cough][cough] These leaves are too dusty. A drink would help.");
		player.injury(10); //for choking dust
		player.isThirsty = true;
		//Why??
		//if (roomList.get(currentRoom).items.contains("hammer")) player.injury(10);	 
	}

	void useHammer() {
		Room r = allRooms.get(currentRoom);
		if (r.items.contains("\"rock\"")) {
			System.out.println("The rock breaks open revealing a golden bell.");
			allRooms.get(currentRoom).items.remove("\"rock\""); //	remove "rock"
			allRooms.get(currentRoom).items.add("bell");			
		} else {
			System.out.println("You start smashing things, but nothing special happens.");
		}
	}
	
	void flashlight() {
		System.out.println("fixme: flashlight");
		System.exit(0);
	}
}
