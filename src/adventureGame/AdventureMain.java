package adventureGame;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/* Text adventure game by MH
 * Version 2.0 
 * Please see "version info.txt" for what works and what doesn't
 * "Language.txt" lists all of the commands that work so far and explains what is supposed to happen with each.
 */


public class AdventureMain {

	static int INVSIZE = 8; //size of inventory	
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
		System.out.print("Please type your firstname: (press enter for \"Zena\") ");
		String name = getCommand();
		if (name.equals("qwerty")) name = "Zena";
		player = new Player(name);		

		startingMessage();

		if (player.name.contentEquals("Gandalf")) {
			System.out.println("\n............\n"
					+ "You have entered 'wizard' mode: free batteries!"
					+ "\n............\n");
			inventory.add("batteries");
		}

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
			
			//pressing enter without typing anything should not run playerCheckup (and print that you are hungry).
			if (command.equals("qwerty")) {
				System.out.println("?");
				continue;
			}
			
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

			//check if player won the game.
			if (itemMap.get("bell").isActivated() && itemMap.get("emerald").isActivated()) {
				playing = false;
				winMessage();
			}
		}

		System.out.println("\n **** Thanks for playing. ****");
		//		System.exit(0);
	}


	String getCommand() {
		Scanner sc = new Scanner(System.in);		
		String text = sc.nextLine();
		if (text.length() == 0) text = "qwerty"; //default command for pressing "Enter"
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
		text = text.replaceAll("place", "put");
		//NOTE: for now, push and pull can be mapped to move.
		text = text.replaceAll("push", "move");
		text = text.replaceAll("pull", "move");
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

		//Special case for "rock".  Note, all rocks are called "rock".
		//There are [rock] items in a couple of rooms. There is also a special rock called ["rock"].
		//If you're in that room, this translates rock --> "rock".
		if (word2.equals("rock") && allRooms.get(currentRoom).items.contains("\"rock\"")) word2 = "\"rock\"";
		if (word3.equals("rock") && allRooms.get(currentRoom).items.contains("\"rock\"")) word3 = "\"rock\"";

		/***** MAIN PROCESSING *****/
		//any command that uses up a turn and food, must run player.update();
		switch(word1) {

		// ****  one word commands  **** //
		case "quit": case "exit":
			System.out.print("Do you really want to quit the game? ");
			char ans = getCommand().toUpperCase().charAt(0);
			if (ans == 'Y') {
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
			//player.update();
			break;
		case "search":
			search();
			break;
		case "swim":
		case "wash":
		case "bathe":
			swim();
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
			climb(word2);
			break;
		case "read":
			readItem(word2);
			break;
		case "lookat":
		case "examine":
			lookAtObject(word2);
			break;
		case "rip":
		case "tear":
			if (word2.equals("paper")) {
				System.out.println("The paper is unusually tough. Cutting it might be better.");
			}
			break;
		case "cut":
			cutItem(word2);
			break;
		case "pickup":
			takeItem(word2);
			break;		
		case "take":
			//take B, take B from A
			if (word3.equals("from")) {
				takeItem(word2, word4);
			} else {
				takeItem(word2);
			}
			break;
		case "drop":
			dropItem(word2);
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
		case "put":  
			//put A in B  (why would anyone do this?) "put hammer in chest"
			if (word3.equals("in")) {
				putItem(word2, word4);
				break;
			}
			
			//This does not work EXCEPT for these two special commands
			if (text.startsWith("put emerald in bell")) activateItem("bell");
			//special lake command
			else if (currentRoom.equals("black_lake") && text.startsWith("put hand in lake")) activateItem("lake");
			else System.out.println ("huh?");
			break;
		case "reach":
			if (currentRoom.equals("black_lake") && text.startsWith("reach in lake")) activateItem("lake");
			break;
		case "smash":
		case "break":
		case "hit":
			//"break rock with hammer"  or "break rock" while you have a hammer
//			if (text.contains ("rock")) {	
			if(text.contains("with hammer") || inventory.contains("hammer")) {
				activateItem("hammer");
			}
			
			else System.out.println("I'm not sure how to do that.");
			break;
			
		case "throw":
			throwItem(word2);
			break;
		//Is there anything else to use?
		case "use":
			if (text.startsWith("use hammer to") && text.contains("rock")) {
				activateItem("hammer");
				break;
			}
			System.out.println("Oops. I don't understand how to use that.");
			//activate(word2);
			break;
		case "close":
			if (word2.equals("")) {
				System.out.println("close what?");				
			} else {
				closeObject(word2);
			}			
			break;			
		
			//SPECIAL COMMANDS
			//get this working for open paper and open package
		case "open":
			if (word2.equals("")) {
				System.out.println("open what?");				
			} else {
				openStuff(word2, word3, word4);
			}			
			break;			
		case "turn":
			/*	turn on flashlight.  turn off flashlight
			turn flashlight on, turn flashlight off		*/
			if (word2.equals("flashlight") || word3.equals("flashlight")) {
				flashlight(word2, word3);
				break;
			}
			else System.out.println("Sorry, I don't understand what you want to turn.");
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


		//run methods for moving ... e.g. climbing the tree and falling
		//this method must put the player in the correct room and also run player.udpate()
		if (newRoom.substring(0, 2).equals("r_")) {			
			runMethod(newRoom);
			return;
		}

		player.update();
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
			System.out.println("CRITICAL ERROR: Location \""+ currentRoom + "\" does not exist.");
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
	/*
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
	 */
	boolean itemPresent(String itemName) {
		if ((inventory.contains(itemName))) return true;		
		Room r = allRooms.get(currentRoom);
		if (r.items.contains(itemName)) return true;
		return false;
	}

	void lookAtObject(String itemName){
		//is item in inventory
		if ((inventory.contains(itemName))) {

			Item it = itemMap.get(itemName);

			if (it.isActivated()) System.out.println(it.descrActive);
			else System.out.println(it.descrLook);
			//player.update();
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
			//player.update();
			return;
		}
		//TODO: containers - all open containers must be searched 
		System.out.println("That object does not exist (here).");	
	}

	void readItem(String itemname) {
		if (itemname.equals("")) {
			System.out.println("Read what?");
			return;
		}
		if (!itemPresent(itemname)) {
			System.out.println("There is no '" + itemname + "' in this location, nor in your inventory.");
			return;			
		}

		Item z = null;
		if ((inventory.contains(itemname))) z = itemMap.get(itemname);	
		Room r = allRooms.get(currentRoom);
		if (r.items.contains(itemname)) z = itemMap.get(itemname);

		if (z.descrRead.length() > 0)
			System.out.println("The " + itemname + " says: " + z.descrRead);
		else
			System.out.println("There is no writing on the " + itemname +".");
		player.update();
	}

	
	void cutItem(String itemname) {
		if (itemname.equals("")) {
			System.out.println("Cut what?");
			return;
		}
		if (!itemPresent(itemname)) {
			System.out.println("There is no '" + itemname + "' in this location, nor in your inventory.");
			return;			
		}

		Item it1 = null;
		if ((inventory.contains(itemname))) it1 = itemMap.get(itemname);	
		Room r = allRooms.get(currentRoom);
		if (r.items.contains(itemname)) it1 = itemMap.get(itemname);
		
		/* The following code is only for cutting the paper repeatedly with the knife */
		if (!inventory.contains("knife")) {
			System.out.println("You have nothing to cut with. (Snort)");
			return;
		}
		
		if(! itemname.equals("paper")) {
			System.out.println("Cutting that is not useful.");
			return;
		}
		
		String s1 = it1.descrRead;
		int len = s1.length(); 
		if (len < 20) {
			System.out.println("The paper is too small to cut further");
			return;
		}
		
		String s2 = s1.substring(len/2, len-1);
		s1 = s1.substring(0,len/2);
		it1.descrRead = s1;
		
		if (len >= 80) {
			inventory.add("yellowpaper");		
			System.out.println("You cut the paper. Half of it turns yellow.");
		} else if (len >= 40) {
			inventory.add("pinkpaper");		
			System.out.println("You cut the paper. Half of it turns pink.");
		} else if (len >= 20){
			inventory.add("bluepaper");		
			System.out.println("You cut the paper. Half of it turns blue.");
		}
		
		player.update();

	}
	
	//Note: The method that actually does the eating is in the player class.
	void eatItem(String itemname) {
		if (itemname.equals("")){
			System.out.println("eat what?");
			return;
		}
		//is item in current room? eat that item first.
		Room r = allRooms.get(currentRoom);
		if (r.items.contains(itemname)) {
			Item z = itemMap.get(itemname);
			if (! player.eat( itemname, z)) return;	
			if (!z.regenFood ) r.items.remove(itemname);
			player.update();
			return;				
		}
		//is item in inventory:
		for (String s : inventory) {
			if (s.equals(itemname)) {
				if (! player.eat(itemname, itemMap.get(itemname))) return;				
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

	void throwItem(String item) {
		if (! item.equals("knife")) {
			System.out.println("From a very early age, "
					+ "the only thing that you have been proficient at throwing is very sharp knives.");
			return;
		}
		if (!itemPresent("knife")) {
			System.out.println("You don't have a knife handy");
			return;
		}
		if (! currentRoom.equals("tunnel2")) {
			System.out.println("You throw the knife "
					+ "and hear a satisfying thunk as it embeds itself in a nearby piece of wood.");
			if (inventory.contains("knife")) {
				inventory.remove(item);
				allRooms.get(currentRoom).items.add(item);
			}
			player.update();
			return;
		}
		
		//You have the knife and you are in the room with the goblin
		System.out.println("You throw the deadly knife at the goblin."
				+ "\nQuick as a flash, the goblin snatches it in mid air and thrusts it into his belt."
				+ "\n\"You found my favourite knife\" he croaks.");
		System.out.println("Then he looks upwards significantly, and vanishes into the rock.");
		System.out.println("In gratitude, there's a bag of gold left behind");
		Room r = allRooms.get(currentRoom);
		r.items.remove("goblin");
		r.items.remove("knife");
		inventory.remove("knife");
		r.items.add("gold");
		r.setExits("tunnel1", "tunnel3","", "", "secret_room","");
		player.update();
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
				Item it = itemMap.get("lake");
				it.activatedMethod = ""; //prevent "reach into lake" from duplicating the key
			} else {
				System.out.println("All you see is inviting black water.");
			}
			player.update();
			return;
		}
		System.out.println("Being fairly observant all you need to do is to \"look at __\". "
				+ "\nSearching really doesn't help you at all, "
				+ "... except possibly at the lowest point in the game.");
	}

	void climb(String word2) {
		System.out.println("** TIP: you can use Up and Down instead of \"climb\". **");
		
		if (currentRoom.equals("cave1")) {
			System.out.println("You carefully climb the steep rockface");
			moveToRoom('u');	
			return;
		}
		if (currentRoom.equals("chimney")) {
			System.out.println("TIP: you can use Up and Down instead of \"climb\".");
			System.out.println("You climb DOWN the crack ...");
			moveToRoom('d');	
			return;
		}
		if (word2.equals("")) {
			System.out.println("What do you want to climb?");
			return;
		}
		if (word2.equals("tree")) {
			if (currentRoom.equals("forest1")) {
				System.out.println("You start climbing ...");
				moveToRoom('u');
			} else {
				System.out.println("There is no climbable tree here.");					
			}
			return;
		}
		
		System.out.println("You can't climb that.");				
	}
	
	void pray() {
		System.out.println("In utter desperation you pray to the divinity.\n "
				+ "This takes a lot of effort to get it to actually work.\n ... ");
		thsleep(600);
		System.out.println("\nAre you sure you want to do this? You might get injured.");
		System.out.println("(The only real reason for praying is if you're lost in the gloomy forest maze.)");
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
		s+="                              ???        \n";
		s+="                               |         \n";
		s+="          to forest <<--------[6]********\n";
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
		s+="    down from tree (otherside)             \n\n";

		System.out.println(s);
	}

	void startingMessage() {
		currentRoom = "clearing";
		String startingText = "\n\nWelcome " + player.name + ". You wake up in a forest clearing.\n "
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

	void swim() {
		if (currentRoom.equals("black_lake")) {
			System.out.println("You swim in the lake and feel refreshed.");
			player.heal(5);
			player.update();
		} else {
			System.out.println("There's nowhere to swim here.");
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
				+ " as well as vertically by typing in the appropriate word (e.g. north, up or just n and u)."
				+ "\nOther common adventure game commands work here too: "
				+ "\nlook, inventory, move, take (and 'take all', and 'take A from B'), drop, eat, drink, pray, cut, ...");
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
			takeItem(eachItem);
		}
		System.out.println("Everything in this location has been added to your backpack.");
		player.update();
	}

	void takeItem(String itemName) {
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
		
		//special case to change the descrLook text:
		if (itemName.equals("knife")) {
			itemMap.get(itemName).descrRoom = "A sharp knife lies in the dirt.";
		}
		player.update();
	}

	//TAKE A FROM B
	void takeItem(String itemName, String container) {
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
			it2.itemContained = "";	
			System.out.println("You add the " + itemName + " to your backpack.");
			player.update();

		} else {
			System.out.printf("There is no %s is in the %s.%n", itemName, container);
		}				
	}
	
	//PUT A INTO B
	void putItem(String itemName, String container) {
		if (! itemPresent(itemName)) {
			System.out.println("You don't have " + itemName + " in your inventory (and it's not in the current location either.");
			return;
		}
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
		
		//The container needs to be empty - as it can only contain only one thing
		if (! it2.itemContained.equals("")) {
			System.out.printf("The %s needs to be empty.%n", container);
			return;
		}
		
		it2.itemContained = itemName;
		inventory.remove(itemName);
		System.out.println("You put the " + itemName + " in the " + container + ".");
		player.update();
	}

	void dropItem(String item) {
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
			if (! it.openRequires.isBlank()) {
				System.out.println("If you close the *" + itemName + "* "
						+ "you will need a " + it.openRequires + " to open it again.");
				System.out.print("Proceed? (Y/N)" );
				char ans = getCommand().toUpperCase().charAt(0);
				if (ans != 'Y') {
					System.out.println("okay then");
					return;
				}	
			}
			System.out.println("You close the " + itemName);
			it.isOpen = false;
			player.update();
		} else {
			System.out.println("The " + itemName + " is already closed.");
		}
	}

	void flashlight(String word2, String word3) {
		boolean turnOn = false;
		if (word2.equals("on") || word3.equals("on")) turnOn = true;

		if (! itemPresent("flashlight")) {
			System.out.println("You don't seem to have the flashlight handy.");
			return;
		}
		if (! itemPresent("batteries")) {
			System.out.println("The flashlight requires batteries");
			return;
		}
		if (turnOn) {
			itemMap.get("flashlight").setActivate(true);
			lookAtObject("flashlight");
		}
		else {
			itemMap.get("flashlight").setActivate(false);
		}
		//player.update();  <-- this is in setActivate()
	}

	//What objects get activated? (by opening?)
	//rock: hit rock with hammer or open rock with hammer, or smash rock with hammer
	//lake (in black_lake room)
	//hammer
	//bell
	void activateItem(String itemName) {
		//exit if it is not in the room and not in the inventory
		if (! itemPresent(itemName)) {
			System.out.println("You don't have " + itemName + " in your inventory (and it's not in the current location either.");
			return;
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

	
	void openStuff(String word2, String w3, String w4)
	{
		//we know that w2 is not empty

		//open A, open A with B			
		if (w3.equals("with") && w4.length() > 0) {
			//openObject(word22, w4);
			openObject(word2); //yeah, we don't really need word4.
		}	
		else {
			openObject(word2);
			//else activate(word2);
		}
	}

	void openObject(String itemName) {

		if (! itemPresent(itemName)) {
			System.out.println("That object does not exist (here).");
			return;
		}

		Item it = itemMap.get(itemName);		
		if (!it.isContainer) {
			System.out.println("You cannot open that item.");
			return;
		}

		//special case for package containing lembas
		if (inventory.contains(itemName) && itemName.equals("package")) {
			inventory.remove(itemName);
			//remove the item from itemMap? No. Maybe we'll need the package again one day.
			inventory.add("lembas");
			System.out.println("It contains lembas!");
			player.update();
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
			System.out.println("The bell doesn't work in this location.");
			return;			
		}
		if (! itemMap.get("bell").isActivated()) {
			System.out.println("The bell will not work without an emerald clapper.");
			return;
		}

		player.update();
		System.out.println("The emerald strikes the shimmering bell ... something transcendent happens ...");
		itemMap.get("emerald").setActivate(true); //this triggers the end of the game.
	}
	
	void winMessage() {
		System.out.println("You ring the bell and a beautiful liquid sound fills the cave.\n"
				+ "Everything shimmers and you find yourself back home"
				+ " and with the emerald bell still in your hand.\n");
		if (inventory.contains("batteries")) 
			System.out.println("The nuclear battery is worth millions. Maybe Elon Musk will buy it.");
		if (inventory.contains("crystal")) 
			System.out.println("You still have the strange glowing crystal. "
			+ "What will you do, keep it hidden? sell it to DARPA? ?");
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
		player.update();
	}
	
	void fallFromCliff(){
		System.out.println("What a dumb idea to climb higher. "
				+ "\nYou slip and fall down to the ground, your hands are now bloody and you broke a nail!");
		player.injury(5);
		currentRoom = "cave1";
		lookAtRoom(false);
		player.update();
	}

	void getFromLake() {
		System.out.println("You feel around in the dark water and pull up a small metal object.");
		System.out.println("It's a key!");
		Item it = itemMap.get("key");
		it.descrRoom = "A shimmering key";
		it = itemMap.get("lake");
		it.activatedMethod = ""; //do not ever show this message twice.
		//		allRooms.get("black_lake").items.add("key");
		//		takeObject("key");
	}

	//This uses the "move" command to reveal the chest. 
	void moveLever() {
		System.out.println("The ground trembles violently. You hear a deep grinding noise."
				+ "\nYou hear falling rocks; your shoulder gets a nasty gash; dust is choking you."
				+ "\nThe room shakes and something massive falls from the ceiling");
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

	//this allows an exit to the treasury once the door is moved.
	void moveTreasureDoor() {
		Room r = allRooms.get("tunnel3");
		r.setExits("tunnel2", "treasury","", "", "","");
		r.items.remove("door");
		//currentRoom = "treasury";
		//lookAtRoom(true);
	}

	void moveLeaves() {
		System.out.println("[cough][cough] These leaves are too dusty. A drink would help.");
		player.injury(10); //for choking dust
		player.isThirsty = true;	 
	}

	void useHammer() {
		Room r = allRooms.get(currentRoom);
		if (r.items.contains("\"rock\"")) {
			System.out.println("The rock breaks open revealing a shimmering silver bell.");
			r.items.remove("\"rock\""); //	remove "rock"
			r.items.add("bell");			
		} else {
			System.out.println("You start smashing things, but nothing special happens.");
			System.out.println("But you did get rid of some of your pent up aggression.");
		}
	}

	//room method
	void fetidCave() {
		System.out.println("There is a really bad smell. Something died in there. Do you really want to go in?");
		char ans = getCommand().toUpperCase().charAt(0);
		if (ans != 'Y') {
			System.out.println("okay then");
			return;
		}	
		currentRoom = "cave3";
		System.out.println("Danger: The miasma has poisoned you.");
		player.isPoisoned = true;
		lookAtRoom(false);
		player.update();
	}

}
